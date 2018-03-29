#!/usr/bin/env bash
TARGET_VERSION=`mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version |grep -Ev '(^\[|Download\w+:)'`
TARGET_VERSION=${TARGET_VERSION/-SNAPSHOT/}

echo Maven Base Version: $TARGET_VERSION
echo Current Tag: $TRAVIS_TAG

if [ "$1" = 'production' ] && [ "$TARGET_VERSION" = "$TRAVIS_TAG"]; then
    echo ----------------------------------------------------------
    echo ------------------ DEPLOYING PRODUCTION ------------------
    echo ----------------------------------------------------------
else
    echo ----------------------------------------------------------
    echo ------------------ DEPLOYING SNAPSHOP --------------------
    echo ----------------------------------------------------------
fi

echo Building version...
# mvn deploy -P release --settings settings.xml
