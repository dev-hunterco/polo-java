#!/usr/bin/env bash
echo "Preparing Docker..."
docker pull localstack/localstack
docker run -d -e SERVICES='sqs,sns' -p 127.0.0.1:4575:4575 -p 127.0.0.1:4576:4576 localstack/localstack
docker ps -a
sleep 5

echo "Installing Certificate..."
openssl aes-256-cbc -K $encrypted_e29ac0d0251a_key -iv $encrypted_e29ac0d0251a_iv -in codesigning.asc.enc -out codesigning.asc -d
gpg --fast-import codesigning.asc

echo "Inspecting version..."
# Inspect twice - first to download all the dependencies, then really get the version
mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version> /dev/null
export POM_VERSION=`mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | grep "^[0-9]" | grep SNAPSHOT$` 
export TARGET_VERSION=${POM_VERSION/-SNAPSHOT/}
echo "Maven Base Version: $TARGET_VERSION"
echo "Current Tag: $TRAVIS_TAG"
