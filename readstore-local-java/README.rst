readstore-local-java
====================

Getting started  
---------------

To use, first build the code using `Apache Maven <http://maven.apache.org/download.cgi>`_::

  cd readstore-local-java
  mvn package

Once built, use the jar file to start a local server::

  java -cp target/readstore-local-java-v1beta-jar-with-dependencies.jar com.google.cloud.genomics.localrepo.Server --dataset=testdata:testdata

There are two command line flags available:

``--port=<portnum>``:
  Sets the port that the server listens on for incoming connections. If
  unspecified, the default is 5000.

``--dataset=<id>:<directory>``:
  This flag can occur zero or more times. With each instance of this flag, you
  supply a dataset ID and a path to a directory on your local filesystem. When
  the server is started, each directory is recursively traversed, and all ``.bam``
  files with a corresponding sibbling ``.bai`` file are included into the data
  with the given dataset ID. For example, if you have the following directory
  layout::

    my_directory/
      my_subdirectory/
        foo.bam
        foo.bam.bai
      another_subdirectory/
        bar.bam
      baz.bam
      baz.bam.bai

  and you passed the flag ``--dataset=my_data:/path/to/my_directory``, then
  ``my_directory/my_subdirectory/foo.bam`` and ``my_directory/baz.bam`` would be
  included into a dataset with ID ``my_data``, but
  ``my_directory/another_subdirectory/bar.bam`` would be excluded, due to not
  having its sibbling ``.bai`` file.  

Go to ``http://localhost:<portnum>/datasets`` to see your data.  

Code layout
-----------

Under active development - coming soon!
