client-java
===========

Getting started
---------------

This Java client allows users to call the `Google Genomics API`_ through the
command line.

* To use, first build the client using `Apache Maven`_::

    cd genomics-tools/client-java
    mvn package

* Then, follow the `authentication instructions`_ to generate a valid
  ``client_secrets.json`` file.

* Move the ``client_secrets.json`` file into the target directory and start the
  authentication process::

    java -jar target/genomics-tools-client-java-v1beta.jar auth

* Once authenticated, you can then perform API queries like fetching readsets or
  reads::

    java -jar target/genomics-tools-client-java-v1beta.jar searchreadsets --dataset_ids <dataset_id> --fields "readsets(id,name)"

    java -jar target/genomics-tools-client-java-v1beta.jar searchreads --readset_ids <readset_id> --sequence_name 1 --sequence_start 10000 --sequence_end 10000

.. _Google Genomics API: https://developers.google.com/genomics
.. _Apache Maven: http://maven.apache.org/download.cgi
.. _authentication instructions: https://developers.google.com/genomics#authenticate

Code layout
-----------

Most of the Java code is a generated client library. This includes everything under
``com/google/api/services``. There are only 2 non-generated files:

CommandLine.java:
    defines all of the possible command line arguments using the `args4j library
    <http://args4j.kohsuke.org/index.html>`_.

GenomicsSample.java:
    provides the bulk of the logic. In its ``main`` method, the user's request is
    dispatched to either make a call to the Genomics API or to authenticate the
    user. Most of the code deals with OAuth.

