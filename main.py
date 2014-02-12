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

This file serves two main purposes
- it serves up the main html page
- and it provides a simple set of apis to the javascript
"""

import os, json, webapp2, jinja2

from oauth2client import appengine
from google.appengine.api import users
from google.appengine.api import urlfetch


# Increase timeout to the maximum for all requests
urlfetch.set_default_fetch_deadline(60)

JINJA_ENVIRONMENT = jinja2.Environment(
    loader=jinja2.FileSystemLoader(os.path.dirname(__file__)),
    autoescape=True,
    extensions=['jinja2.ext.autoescape'])

client_secrets = os.path.join(os.path.dirname(__file__), 'client_secrets.json')

decorator = appengine.oauth2decorator_from_clientsecrets(
    client_secrets,
    scope=[
      'https://www.googleapis.com/auth/genomics',
      'https://www.googleapis.com/auth/devstorage.read_write'
    ])

class ApiException(Exception):
  pass

class BaseRequestHandler(webapp2.RequestHandler):
  def handle_exception(self, exception, debug_mode):
    if isinstance(exception, ApiException):
      # ApiExceptions are expected, and will return nice error messages to the client
      self.response.write(exception.message)
      self.response.set_status(400)
    else:
      # All other exceptions are unexpected and should crash properly
      return webapp2.RequestHandler.handle_exception(self, exception, debug_mode)

  def get_content(self, path, method='POST', body=None):
    http = decorator.http()
    response, content = http.request(
      uri="https://www.googleapis.com/genomics/v1beta/%s" % path,
      method=method, body=json.dumps(body) if body else None,
      headers={'Content-Type': 'application/json; charset=UTF-8'})

    # TODO: Nice errors for other non-200s
    if response.status == 404:
      raise ApiException('Api not found')
    elif response.status == 400:
      raise ApiException('Api request malformed')

    content = json.loads(content)
    if 'error' in content:
      raise ApiException(content['error']['message'])

    self.response.write(json.dumps(content))

class ReadsetImportHandler(BaseRequestHandler):

  @decorator.oauth_aware
  def post(self):
    dataset_id = self.request.get('datasetId')
    source_uri = self.request.get('sourceUri')
    
    body = {
      'datasetId' : dataset_id,
      'sourceUris': [source_uri],
     }
    self.get_content("readsets/import", body=body)

class GetJobHandler(BaseRequestHandler):

  @decorator.oauth_aware
  def get(self):
    job_id = self.request.get('jobId')
    self.get_content("jobs/%s" % job_id, method='GET')

class ReadsetSearchHandler(BaseRequestHandler):

  @decorator.oauth_aware
  def get(self):
    readset_id = self.request.get('readsetId')
    if not readset_id:
      # TODO: Do real searching here
      content = [
        {'name': 'DG', 'id': 'CJ_ppJ-WCxD-2oXg667IhDM='},
      ]
      self.response.write("%s" % json.dumps(content))
      return

    # Single readset response
    targets = [
      {'name': "1", 'targetLength': 249250621},
      {'name': "2", 'targetLength': 243199373},
      {'name': "3", 'targetLength': 198022430},
      {'name': "4", 'targetLength': 191154276},
      {'name': "5", 'targetLength': 180915260},
      {'name': "6", 'targetLength': 171115067},
      {'name': "7", 'targetLength': 159138663},
      {'name': "8", 'targetLength': 146364022},
      {'name': "9", 'targetLength': 141213431},
      {'name': "10", 'targetLength': 135534747},
      {'name': "11", 'targetLength': 135006516},
      {'name': "12", 'targetLength': 133851895},
      {'name': "13", 'targetLength': 115169878},
      {'name': "14", 'targetLength': 107349540},
      {'name': "15", 'targetLength': 102531392},
      {'name': "16", 'targetLength': 90354753},
      {'name': "17", 'targetLength': 81195210},
      {'name': "18", 'targetLength': 78077248},
      {'name': "19", 'targetLength': 59128983},
      {'name': "20", 'targetLength': 63025520},
      {'name': "21", 'targetLength': 48129895},
      {'name': "22", 'targetLength': 51304566},
      {'name': "X", 'targetLength': 155270560},
      {'name': "Y", 'targetLength': 59373566},
    ]
    self.response.write("%s" % json.dumps(targets))

class ReadSearchHandler(BaseRequestHandler):

  @decorator.oauth_aware
  def get(self):
    readset_ids = [id for id in self.request.get('readsetIds').split(',')]

    body = {
      'readsetIds': readset_ids,
      'sequenceName': 'chr' + self.request.get('target'), # TODO: Need to not hardcode this
      'sequenceStart': max(0, int(self.request.get('targetStart'))),
      'sequenceEnd': int(self.request.get('targetEnd')),
      'pageToken': self.request.get('pageToken'),
     }
    self.get_content("reads/search", body=body)

class MainHandler(webapp2.RequestHandler):

  @decorator.oauth_aware
  def get(self):
    if decorator.has_credentials():
      template = JINJA_ENVIRONMENT.get_template('main.html')
      self.response.write(template.render({
        'username': users.User().nickname(),
        'logout_url': users.create_logout_url('/')
      }))
    else:
      template = JINJA_ENVIRONMENT.get_template('grantaccess.html')
      self.response.write(template.render({
        'url': decorator.authorize_url()
      }))

app = webapp2.WSGIApplication(
    [
     ('/', MainHandler),
     ('/api/jobs', GetJobHandler),
     ('/api/reads', ReadSearchHandler),
     ('/api/readsets', ReadsetSearchHandler),
     ('/api/readsets/import', ReadsetImportHandler),
     (decorator.callback_path, decorator.callback_handler()),
    ],
    debug=True)
