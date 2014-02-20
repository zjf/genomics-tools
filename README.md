genomics-tools
==============

The projects in this repository are focused around the <a href="https://developers.google.com/genomics">Google Genomics API</a>.

* **java-client** provides a command line interface for API queries, and demonstrates how a more complex Java
client might be written.

* **python-client** provides an example web interface that depends on API queries, and demonstrates how a more complex
Python client might be written. It uses
<a href="https://developers.google.com/appengine/docs/python/gettingstartedpython27/introduction">Google App Engine</a>
to deploy.

* **readstore-local** implements the Genomics API locally, reading its data from a local
<a href="http://samtools.sourceforge.net/SAMv1.pdf">BAM file</a>.

* **python-mapreduce**