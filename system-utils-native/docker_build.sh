#!/bin/sh

LIB_NAME=system-utils-native
JAVA_PROJECT=../diozero-core

docker run --rm -w /${LIB_NAME} -v "$(pwd):/${LIB_NAME}" diozero/diozero-cc sh -c ./cc_build.sh

if [ $? -eq 0 ]; then
  cp -R lib/* ${JAVA_PROJECT}/src/main/resources/lib/.
fi
