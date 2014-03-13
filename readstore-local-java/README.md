readstore-local-java
==============

###Getting started
* To use, first build the code using <a href="http://maven.apache.org/download.cgi">Apache Maven</a>:

```
cd repository-local-java
mvn package
```

* Once built, use the jar file to start a local server:
```
java -cp target/repository-local-java-v1beta-jar-with-dependencies.jar:lib/sam-1.109.jar:lib/picard-1.109.jar com.google.cloud.genomics.localrepo.Server --dataset=testdata:testdata
```

* Go to http://localhost:5000/datasets to see your data.

* To run over your own BAM data, the dataset flag can be modified to point to
any local file. The flag uses the format --dataset=<id>:<filepath>, where id can
be any string. The filepath should point to a BAM file. Note that you must also
have a BAM index file in the same directory, otherwise the code will not be able
to generate readsets.


###Code layout

Coming soon