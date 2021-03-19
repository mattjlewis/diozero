#!/bin/sh

PATH=/home/vagrant/rpi-tools/arm-bcm2708/gcc-linaro-arm-linux-gnueabihf-raspbian-x64/bin:$PATH
#make clean && make CROSS_COMPILE=arm-linux-gnueabihf- ARCH=armv8-a
make clean && make CROSS_COMPILE=aarch64-linux-gnu- ARCH=armv8-a
cp libdiozero-system-utils.so aarch64/libdiozero-system-utils.so
