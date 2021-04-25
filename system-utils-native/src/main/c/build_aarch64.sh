#!/bin/sh

#PATH=/home/vagrant/rpi-tools/arm-bcm2708/gcc-linaro-arm-linux-gnueabihf-raspbian-x64/bin:$PATH
#make clean && make CROSS_PREFIX=arm-linux-gnueabihf- ARCH=armv8-a
make clean && make CROSS_PREFIX=aarch64-linux-gnu- ARCH=armv8-a
OUTPUT_DIR=aarch64
mkdir -p ${OUTPUT_DIR}
cp libdiozero-system-utils.so ${OUTPUT_DIR}/libdiozero-system-utils.so
