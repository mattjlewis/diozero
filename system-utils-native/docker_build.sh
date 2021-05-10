#!/bin/sh

docker run --rm -w /system-utils-native \
  -v "$(pwd):/system-utils-native" diozero/diozero-cc sh -c ./cc_build.sh

if [ $? -eq 0 ]; then
  cp -R lib/* ../diozero-core/src/main/resources/lib/.
fi
