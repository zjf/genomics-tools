protobufs
=========

The `Google Genomics API <https://developers.google.com/genomics>`_ uses `protobuf
<https://developers.google.com/protocol-buffers/>`_ files to represent genomics
data. The client libraries, documentation, and JSON output are all auto generated
from these ``.proto`` files.

read.proto:
    represents a group of bases and their metadata.
readset.proto:
    represents a collection of reads along with information from the BAM file(s)
    the reads were imported from.
common.proto:
    holds messages that will be common across many genomics objects. Currently it
    only contains a basic key value pair.
bam.proto:
    contains all messages that map 1-1 with the BAM format. The hope is that all
    important BAM information will be pulled into top level readset fields. Until
    this is done, each readset contains the exact header sections from the BAM file
    it was imported from.
service.proto:
    defines the REST service used to expose these objects.  

The format provided by these files is also documented in the `API definition file
<https://www.googleapis.com/discovery/v1/apis/genomics/v1beta/rest>`_.

Note also that all of the Google Genomics APIs can return protobufs instead of JSON
by adding a ``alt=proto`` URL parameter to a request.
