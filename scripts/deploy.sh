#!/usr/bin/env bash

echo "Inspecting version..."
# Inspect twice - first to download all the dependencies, then really get the version
mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version> /dev/null
export POM_VERSION=`mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | grep "^[0-9]" | grep SNAPSHOT$` 
export TARGET_VERSION=${POM_VERSION/-SNAPSHOT/}

echo Starting deploying...
echo Mode: $1
echo Travis Tag: "$TRAVIS_TAG"
echo Target Version: $TARGET_VERSION


if [ "$1" = "production" ] && [ "$TARGET_VERSION" = "$TRAVIS_TAG"]; then
    echo ----------------------------------------------------------
    echo -------- DEPLOYING PRODUCTION - $TARGET_VERSION ----------
    echo ----------------------------------------------------------
    echo Changing pom.xml version...
    mvn versions:set -DnewVersion=$TARGET_VERSION
else
    echo ----------------------------------------------------------
    echo ----------- DEPLOYING SNAPSHOP - $POM_VERSION ------------
    echo ----------------------------------------------------------
fi

mvn deploy -B -Dmaven.test.skip=true -P release --settings settings.xml
