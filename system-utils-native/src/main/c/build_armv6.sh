#!/bin/sh

PATH=${HOME}/rpi-tools/arm-bcm2708/gcc-linaro-arm-linux-gnueabihf-raspbian/bin:$PATH
CROSS_PREFIX=arm-linux-gnueabihf-
make clean && make CROSS_PREFIX=${CROSS_PREFIX} ARCH=armv6 CC_CFLAGS="-mfpu=vfp -mfloat-abi=hard" CC_LFLAGS="-L${HOME}/libs/${CROSS_PREFIX}lib"
OUTPUT_DIR=linux-armv6 && mkdir -p ${OUTPUT_DIR} && mv libdiozero-system-utils.so ${OUTPUT_DIR}/libdiozero-system-utils.so
