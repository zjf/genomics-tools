protobufs
==============

The <a href="https://developers.google.com/genomics">Google Genomics API</a>
uses <a href="https://developers.google.com/protocol-buffers/">protobuf</a>
files to represent genomics data. The client libraries, documentation, and json
output are all auto generated from these .proto files.

* **read.proto** represents a group of bases and their metadata.
* **readset.proto** represents a collection of reads along with information
    from the BAM file(s) the reads were imported from.
* **common.proto** holds messages that will be common across many genomics objects.
    Currently it only contains a basic key value pair.
* **bam.proto** contains all messages that map 1-1 with the BAM format.
    The hope is that all important BAM information will be pulled into top level
    readset fields. Until this is done, each readset contains the exact header
    sections from the BAM file it was imported from.

The format provided by these files is also documented in the
<a href="https://www.googleapis.com/discovery/v1/apis/genomics/v1beta/rest">API definition file</a>.

Note also that all of the Google Genomics APIs can return protobufs instead of json by adding a
alt=proto url parameter to a request.
