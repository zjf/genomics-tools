#/usr/bin/env/sh

set -o nounset
set -o errexit

# Build Go client and run tests

export GOPATH=~
readonly TARGET_PATH=$GOPATH/src/github.com/GoogleCloudPlatform/genomics-tools
mkdir -p $TARGET_PATH
cp -r * $TARGET_PATH

pushd $TARGET_PATH

go get ./...
go test ./...

popd

# Build Java client and run tests
java_components="benchmarker-java client-java readstore-local-java"

for java_component in $java_components
do
    echo "Testing component: $java_component"
    cd $java_component
    mvn install -DskipTests=true
    mvn test
    cd ..
done
