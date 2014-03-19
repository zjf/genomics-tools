#/usr/bin/env/sh
set -e
java_components="benchmarker-java client-java readstore-local-java"

for java_component in $java_components
do
    echo "Testing component: $java_component"
    cd $java_component
    mvn install -DskipTests=true
    mvn test
    cd ..
done