#!/usr/bin/env bash

if [ "$1" = 'production' ] && [ "$TARGET_VERSION" = "$TRAVIS_TAG"]; then
    echo ----------------------------------------------------------
    echo -------- DEPLOYING PRODUCTION - $TARGET_VERSION-----------
    echo ----------------------------------------------------------
else
    echo ----------------------------------------------------------
    echo ----------- DEPLOYING SNAPSHOP - $POM_VERSION ------------
    echo ----------------------------------------------------------
fi

echo Deploying version...
mvn deploy -Dmaven.test.skip=true -P release --settings settings.xml
