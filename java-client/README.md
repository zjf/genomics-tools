java-client
==============

cd genomics-tools/java-client
mvn package
cd target
java -jar google-api-services-genomics-v1-rev20130925-1.18.0-rc-SNAPSHOT.jar listreads --client_id <client_id> --project_id 0 --sequence_name 1 --sequence_start 10000 --sequence_end 10000
