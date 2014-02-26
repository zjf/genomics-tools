client-java
==============

###Getting started

This java client allows users to call the <a href="https://developers.google.com/genomics">Google Genomics API</a> through the command line.

* To use, first build the client using <a href="http://maven.apache.org/download.cgi">Apache Maven</a>:

```
cd genomics-tools/client-java
mvn package
cd target
```

* Then, follow the <a href="https://developers.google.com/genomics/v1beta/quickstart">quickstart instructions</a> to generate a valid client_secrets.json file.

* Move the client_secrets.json file into the target directory and start the authentication process:
```
java -jar google-api-services-genomics-v1-rev20130925-1.18.0-rc-SNAPSHOT.jar auth
```

* Once authenticated, you can then perform API queries like fetching readsets or reads:
```
java -jar google-api-services-genomics-v1-rev20130925-1.18.0-rc-SNAPSHOT.jar listreadsets --dataset_ids <dataset_id> --fields "readsets(id,name)"
```
```
java -jar google-api-services-genomics-v1-rev20130925-1.18.0-rc-SNAPSHOT.jar listreads --readset_ids <readset_id> --sequence_name 1 --sequence_start 10000 --sequence_end 10000
```

###Code layout

Most of the Java code is a generated client library. This includes everything under com/google/api/services. There are only 2 non-generated files:

* **CommandLine.java** defines all of the possible command line arguments using the
<a href="http://args4j.kohsuke.org/index.html">args4j library</a>.

* **GenomicsSample.java** provides the bulk of the logic. In its main method, the user's request is dispatched to
either make a call to the Genomics API or to authenticate the user. Most of the code deals with OAuth.

