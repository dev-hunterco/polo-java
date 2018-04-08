#!/usr/bin/env bash

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
