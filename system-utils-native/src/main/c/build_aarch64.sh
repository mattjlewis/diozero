#!/bin/sh

make clean && make CROSS_PREFIX=aarch64-linux-gnu- CFLAGS="-march=armv8-a"
OUTPUT_DIR=lib/linux-aarch64 && mkdir -p ${OUTPUT_DIR} && mv libdiozero-system-utils.so ${OUTPUT_DIR}/libdiozero-system-utils.so
