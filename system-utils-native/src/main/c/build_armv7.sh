#!/bin/sh

make clean && make CROSS_PREFIX=arm-linux-gnueabihf- ARCH=armv7
OUTPUT_DIR=lib/linux-armv7 && mkdir -p ${OUTPUT_DIR} && mv libdiozero-system-utils.so ${OUTPUT_DIR}/libdiozero-system-utils.so
