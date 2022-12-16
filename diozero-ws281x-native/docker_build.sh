#!/bin/sh

LIB_NAME=ws281xj
JAVA_PROJECT=../diozero-ws281x-java

#podman run --rm -v "$(pwd):/${LIB_NAME}" -w ${LIB_NAME} diozero/diozero-ws281x-cc sh -c ./cc_build.sh
#podman run --rm -v "$(pwd):/home/develop/src_dir" -w /home/develop/src_dir diozero/diozero-ws281x-cc sh -c ./cc_build.sh
podman run --rm -v "$(pwd):/${LIB_NAME}" -w /${LIB_NAME} --uidmap 1000:0:1 --uidmap 0:1:1000 --gidmap 1000:0:1 --gidmap 0:1:1000 diozero/diozero-ws281x-cc sh -c ./cc_build.sh

if [ $? -eq 0 ]; then
  cp -R lib/* ${JAVA_PROJECT}/src/main/resources/lib/.
fi
