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

* Go to http://localhost:8080/datasets to see your data


###Code layout

Coming soon