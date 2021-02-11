#!/bin/sh

PATH=/home/vagrant/rpi-tools/arm-bcm2708/gcc-linaro-arm-linux-gnueabihf-raspbian-x64/bin:$PATH
make clean && make CROSS_COMPILE=arm-linux-gnueabihf- ARCH=armv8-a CC_CFLAGS="-mfpu=vfp -mfloat-abi=hard"
cp libdiozero-system-utils.so aarch64/libdiozero-system-utils.so
