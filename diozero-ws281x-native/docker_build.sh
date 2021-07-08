#!/bin/sh

LIB_NAME=ws281xj
JAVA_PROJECT=../diozero-ws281x-java

docker run --rm -w /${LIB_NAME} -v "$(pwd):/${LIB_NAME}" diozero/diozero-ws281x-cc sh -c ./cc_build.sh

if [ $? -eq 0 ]; then
  cp -R lib/* ${JAVA_PROJECT}/src/main/resources/lib/.
fi
