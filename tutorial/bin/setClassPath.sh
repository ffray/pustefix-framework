#!/bin/sh

if [ "$1" = "" ]; then
    echo "Need start of project base dir as first parameter";
    exit -1;
fi

JARS="`ls -1 $1/lib/*.jar | paste -s -d:`:";

echo -n $JARS$1/build:$1/res
