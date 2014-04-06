==============
genomics-tools
==============
---------------
|Build Status|_
---------------

.. |Build Status| image:: https://travis-ci.org/GoogleCloudPlatform/genomics-tools.png?branch=master
.. _Build Status: https://travis-ci.org/GoogleCloudPlatform/genomics-tools

The projects in this repository are focused around the `Google Genomics API
<https://developers.google.com/genomics>`_:

client-go:
    shows how easy it can be to call into the API with the Go programming
    language.
client-java:
    provides a command line interface for API queries, and demonstrates how a
    more complex Java client might be written.
client-python:
    provides an example web interface that depends on API queries, and
    demonstrates how a more complex Python client might be written. It uses
    `Google App Engine`_ to deploy.
client-r:
    provides a simple R script that transforms API query results into
    GAlignments.
mapreduce-python:
    uses the `MapReduce Python`_ feature of Google App Engine to do complex calculations over API data.
protobufs:
    includes the protocol buffers used by the API. All client libraries, documentation, and output are auto generated from these files.
readstore-local-java:
    implements the Genomics API locally, reading its data from a local
    `BAM file`_.

.. _Google App Engine: https://developers.google.com/appengine/docs/python/gettingstartedpython27/introduction
.. _MapReduce Python: https://developers.google.com/appengine/docs/python/dataprocessing/
.. _BAM file: http://samtools.sourceforge.net/SAMv1.pdf

Contributing changes
--------------------

See `CONTRIB.rst <CONTRIB.rst>`__.

Licensing
---------

See `LICENSE <LICENSE>`__.
