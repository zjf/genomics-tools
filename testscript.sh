#/usr/bin/env/sh

set -o nounset
set -o errexit

# Build Go client and run tests

export GOPATH=~
readonly TARGET_PATH=$GOPATH/src/github.com/GoogleCloudPlatform/genomics-tools
mkdir -p $TARGET_PATH
cp -r * $TARGET_PATH

pushd $TARGET_PATH

# go fmt check
readonly GO_FMT_COUNT=`go fmt ./... | wc -l`
if [ $GO_FMT_COUNT -ne 0 ]
then
    echo $GO_FMT_COUNT files need to be reformatted with go fmt.
fi

# Build and test
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
