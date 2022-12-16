#!/bin/sh

LIB_NAME=system-utils-native
JAVA_PROJECT=../diozero-core

podman run --rm -w /${LIB_NAME} -v "$(pwd):/${LIB_NAME}" --uidmap 1000:0:1 --uidmap 0:1:1000 --gidmap 1000:0:1 --gidmap 0:1:1000 diozero/diozero-cc sh -c ./cc_build.sh

if [ $? -eq 0 ]; then
  cp -R lib/* ${JAVA_PROJECT}/src/main/resources/lib/.
fi
