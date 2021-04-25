#!/bin/sh

make clean && make ARCH=x86-64
OUTPUT_DIR=x86-64
mkdir -p ${OUTPUT_DIR}
cp libdiozero-system-utils.so ${OUTPUT_DIR}/libdiozero-system-utils.so
