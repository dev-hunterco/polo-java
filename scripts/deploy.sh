#!/usr/bin/env bash

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
