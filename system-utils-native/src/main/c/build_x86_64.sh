#!/bin/sh

make clean && make ARCH=x86-64
OUTPUT_DIR=lib/linux-x86_64 && mkdir -p ${OUTPUT_DIR} && mv libdiozero-system-utils.so ${OUTPUT_DIR}/libdiozero-system-utils.so
