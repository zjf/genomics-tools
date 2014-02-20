"""
Copyright 2014 Google Inc. All rights reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Example Genomics Map Reduce
"""

import datetime
import jinja2
import logging
import re
import urllib
import webapp2

from google.appengine.ext import blobstore
from google.appengine.ext import db

from google.appengine.ext.webapp import blobstore_handlers

from google.appengine.api import files
from google.appengine.api import taskqueue
from google.appengine.api import users

from mapreduce import base_handler
from mapreduce import mapreduce_pipeline
from mapreduce import operation as op
from mapreduce import shuffler


class FileMetadata(db.Model):
  """A helper class that will hold metadata for the user's blobs.

  Specifially, we want to keep track of who uploaded it, where they uploaded it
  from (right now they can only upload from their computer, but in the future
  urlfetch would be nice to add), and links to the results of their MR jobs. To
  enable our querying to scan over our input data, we store keys in the form
  'user/date/blob_key', where 'user' is the given user's e-mail address, 'date'
  is the date and time that they uploaded the item on, and 'blob_key'
  indicates the location in the Blobstore that the item can be found at. '/'
  is not the actual separator between these values - we use '..' since it is
  an illegal set of characters for an e-mail address to contain.
  """

  __SEP = ".."
  __NEXT = "./"

  owner = db.UserProperty()
  filename = db.StringProperty()
  uploadedOn = db.DateTimeProperty()
  source = db.StringProperty()
  blobkey = db.StringProperty()
  wordcount_link = db.StringProperty()
  index_link = db.StringProperty()
  phrases_link = db.StringProperty()

  @staticmethod
  def getFirstKeyForUser(username):
    """Helper function that returns the first possible key a user could own.

    This is useful for table scanning, in conjunction with getLastKeyForUser.

    Args:
      username: The given user's e-mail address.
    Returns:
      The internal key representing the earliest possible key that a user could
      own (although the value of this key is not able to be used for actual
      user data).
    """

    return db.Key.from_path("FileMetadata", username + FileMetadata.__SEP)

  @staticmethod
  def getLastKeyForUser(username):
    """Helper function that returns the last possible key a user could own.

    This is useful for table scanning, in conjunction with getFirstKeyForUser.

    Args:
      username: The given user's e-mail address.
    Returns:
      The internal key representing the last possible key that a user could
      own (although the value of this key is not able to be used for actual
      user data).
    """

    return db.Key.from_path("FileMetadata", username + FileMetadata.__NEXT)

  @staticmethod
  def getKeyName(username, date, blob_key):
    """Returns the internal key for a particular item in the database.

    Our items are stored with keys of the form 'user/date/blob_key' ('/' is
    not the real separator, but __SEP is).

    Args:
      username: The given user's e-mail address.
      date: A datetime object representing the date and time that an input
        file was uploaded to this app.
      blob_key: The blob key corresponding to the location of the input file
        in the Blobstore.
    Returns:
      The internal key for the item specified by (username, date, blob_key).
    """

    sep = FileMetadata.__SEP
    return str(username + sep + str(date) + sep + blob_key)


class IndexHandler(webapp2.RequestHandler):
  """The main page that users will interact with, which presents users with
  the ability to upload new data or run MapReduce jobs on their existing data.
  """

  template_env = jinja2.Environment(loader=jinja2.FileSystemLoader("templates"),
                                    autoescape=True)

  def get(self):
    user = users.get_current_user()
    username = user.nickname()

    # taken from the python-client app. Maybe refactor to common place ??? -jlucena
    targets = [
      {'name': "chr1", 'sequenceLength': 249250621},
      {'name': "chr2", 'sequenceLength': 243199373},
      {'name': "chr3", 'sequenceLength': 198022430},
      {'name': "chr4", 'sequenceLength': 191154276},
      {'name': "chr5", 'sequenceLength': 180915260},
      {'name': "chr6", 'sequenceLength': 171115067},
      {'name': "chr7", 'sequenceLength': 159138663},
      {'name': "chr8", 'sequenceLength': 146364022},
      {'name': "chr9", 'sequenceLength': 141213431},
      {'name': "chr10", 'sequenceLength': 135534747},
      {'name': "chr11", 'sequenceLength': 135006516},
      {'name': "chr12", 'sequenceLength': 133851895},
      {'name': "chr13", 'sequenceLength': 115169878},
      {'name': "chr14", 'sequenceLength': 107349540},
      {'name': "chr15", 'sequenceLength': 102531392},
      {'name': "chr16", 'sequenceLength': 90354753},
      {'name': "chr17", 'sequenceLength': 81195210},
      {'name': "chr18", 'sequenceLength': 78077248},
      {'name': "chr19", 'sequenceLength': 59128983},
      {'name': "chr20", 'sequenceLength': 63025520},
      {'name': "chr21", 'sequenceLength': 48129895},
      {'name': "chr22", 'sequenceLength': 51304566},
      {'name': "chrX", 'sequenceLength': 155270560},
      {'name': "chrY", 'sequenceLength': 59373566},
    ]

    self.response.out.write(self.template_env.get_template("index.html").render(
        {"username": username,
         "targets": targets}))

  def post(self):
    readset_id = self.request.get("readset_id")
    seqname = self.request.get("seqname")
    seqstart = self.request.get("seqstart")
    seqend = self.request.get("seqend")

    if self.request.get("submit_read"):
        self.response.out.write("yo yo: %s %s %s %s" % (readset_id, seqname, seqstart, seqend))
        return


    pipeline = PhrasesPipeline(readset_id, seqname)

    pipeline.start()
    self.redirect(pipeline.base_path + "/status?root=" + pipeline.pipeline_id)


def split_into_sentences(s):
  """Split text into list of sentences."""
  s = re.sub(r"\s+", " ", s)
  s = re.sub(r"[\\.\\?\\!]", "\n", s)
  return s.split("\n")


def split_into_words(s):
  """Split a sentence into list of words."""
  s = re.sub(r"\W+", " ", s)
  s = re.sub(r"[_0-9]+", " ", s)
  return s.split()


def word_count_map(data):
  """Word count map function."""
  (entry, text_fn) = data
  text = text_fn()

  logging.debug("Got %s", entry.filename)
  for s in split_into_sentences(text):
    for w in split_into_words(s.lower()):
      yield (w, "")


def word_count_reduce(key, values):
  """Word count reduce function."""
  yield "%s: %d\n" % (key, len(values))


def index_map(data):
  """Index demo map function."""
  (entry, text_fn) = data
  text = text_fn()

  logging.debug("Got %s", entry.filename)
  for s in split_into_sentences(text):
    for w in split_into_words(s.lower()):
      yield (w, entry.filename)


def index_reduce(key, values):
  """Index demo reduce function."""
  yield "%s: %s\n" % (key, list(set(values)))


PHRASE_LENGTH = 4


def phrases_map(data):
  """Phrases demo map function."""
  (entry, text_fn) = data
  text = text_fn()
  filename = entry.filename

  logging.debug("Got %s", filename)
  for s in split_into_sentences(text):
    words = split_into_words(s.lower())
    if len(words) < PHRASE_LENGTH:
      yield (":".join(words), filename)
      continue
    for i in range(0, len(words) - PHRASE_LENGTH):
      yield (":".join(words[i:i+PHRASE_LENGTH]), filename)


def phrases_reduce(key, values):
  """Phrases demo reduce function."""
  if len(values) < 10:
    return
  counts = {}
  for filename in values:
    counts[filename] = counts.get(filename, 0) + 1

  words = re.sub(r":", " ", key)
  threshold = len(values) / 2
  for filename, count in counts.items():
    if count > threshold:
      yield "%s:%s\n" % (words, filename)

class WordCountPipeline(base_handler.PipelineBase):
  """A pipeline to run Word count demo.

  Args:
    blobkey: blobkey to process as string. Should be a zip archive with
      text files inside.
  """

  def run(self, filekey, blobkey):
    logging.debug("filename is %s" % filekey)
    output = yield mapreduce_pipeline.MapreducePipeline(
        "word_count",
        "main.word_count_map",
        "main.word_count_reduce",
        "mapreduce.input_readers.BlobstoreZipInputReader",
        "mapreduce.output_writers.BlobstoreOutputWriter",
        mapper_params={
            "blob_key": blobkey,
        },
        reducer_params={
            "mime_type": "text/plain",
        },
        shards=16)
    yield StoreOutput("WordCount", filekey, output)


class IndexPipeline(base_handler.PipelineBase):
  """A pipeline to run Index demo.

  Args:
    blobkey: blobkey to process as string. Should be a zip archive with
      text files inside.
  """


  def run(self, filekey, blobkey):
    output = yield mapreduce_pipeline.MapreducePipeline(
        "index",
        "main.index_map",
        "main.index_reduce",
        "mapreduce.input_readers.BlobstoreZipInputReader",
        "mapreduce.output_writers.BlobstoreOutputWriter",
        mapper_params={
            "blob_key": blobkey,
        },
        reducer_params={
            "mime_type": "text/plain",
        },
        shards=16)
    yield StoreOutput("Index", filekey, output)


class PhrasesPipeline(base_handler.PipelineBase):
  """A pipeline to run Phrases demo.

  Args:
    blobkey: blobkey to process as string. Should be a zip archive with
      text files inside.
  """

  def run(self, filekey, blobkey):
    output = yield mapreduce_pipeline.MapreducePipeline(
        "phrases",
        "main.phrases_map",
        "main.phrases_reduce",
        "mapreduce.input_readers.BlobstoreZipInputReader",
        "mapreduce.output_writers.BlobstoreOutputWriter",
        mapper_params={
            "blob_key": blobkey,
        },
        reducer_params={
            "mime_type": "text/plain",
        },
        shards=16)
    yield StoreOutput("Phrases", filekey, output)


class StoreOutput(base_handler.PipelineBase):
  """A pipeline to store the result of the MapReduce job in the database.

  Args:
    mr_type: the type of mapreduce job run (e.g., WordCount, Index)
    encoded_key: the DB key corresponding to the metadata of this job
    output: the blobstore location where the output of the job is stored
  """

  def run(self, mr_type, encoded_key, output):
    logging.debug("output is %s" % str(output))
    key = db.Key(encoded=encoded_key)
    m = FileMetadata.get(key)

    if mr_type == "WordCount":
      m.wordcount_link = output[0]
    elif mr_type == "Index":
      m.index_link = output[0]
    elif mr_type == "Phrases":
      m.phrases_link = output[0]

    m.put()

class UploadHandler(blobstore_handlers.BlobstoreUploadHandler):
  """Handler to upload data to blobstore."""

  def post(self):
    source = "uploaded by user"
    upload_files = self.get_uploads("file")
    blob_key = upload_files[0].key()
    name = self.request.get("name")

    user = users.get_current_user()

    username = user.nickname()
    date = datetime.datetime.now()
    str_blob_key = str(blob_key)
    key = FileMetadata.getKeyName(username, date, str_blob_key)

    m = FileMetadata(key_name = key)
    m.owner = user
    m.filename = name
    m.uploadedOn = date
    m.source = source
    m.blobkey = str_blob_key
    m.put()

    self.redirect("/")


class DownloadHandler(blobstore_handlers.BlobstoreDownloadHandler):
  """Handler to download blob by blobkey."""

  def get(self, key):
    key = str(urllib.unquote(key)).strip()
    logging.debug("key is %s" % key)
    blob_info = blobstore.BlobInfo.get(key)
    self.send_blob(blob_info)


app = webapp2.WSGIApplication(
    [
        ('/', IndexHandler),
        ('/upload', UploadHandler),
        (r'/blobstore/(.*)', DownloadHandler),
    ],
    debug=True)
