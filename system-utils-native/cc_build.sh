#!/bin/sh

cd src/main/c
LIB_DIR=../../../lib

# x86_64
make clean && make ARCH=x86-64
if [ $? -eq 0 ]; then
  TARGET=${LIB_DIR}/linux-x86_64 && mkdir -p ${TARGET} && mv libdiozero-system-utils.so ${TARGET}/.
fi

# aarch64
make clean && make CROSS_PREFIX=aarch64-linux-gnu- ARCH=armv8-a
if [ $? -eq 0 ]; then
  TARGET=${LIB_DIR}/linux-aarch64 && mkdir -p ${TARGET} && mv libdiozero-system-utils.so ${TARGET}/.
fi

# armv7
make clean && make CROSS_PREFIX=arm-linux-gnueabihf- ARCH=armv7 CC_CFLAGS="-mfpu=vfp -mfloat-abi=hard"
if [ $? -eq 0 ]; then
  TARGET=${LIB_DIR}/linux-armv7 && mkdir -p ${TARGET} && mv libdiozero-system-utils.so ${TARGET}/.
fi

# Finally build armv6 to be extra sure that PATH has no reference to the Pi armv6 cross compiler
OLD_PATH=${PATH}
PATH=${PI_GCC_TARGET_DIR}/bin:${PATH} && make clean && make CROSS_PREFIX=arm-linux-gnueabihf- ARCH=armv6 CC_CFLAGS="-mfpu=vfp -mfloat-abi=hard"
if [ $? -eq 0 ]; then
  TARGET=${LIB_DIR}/linux-armv6 && mkdir -p ${TARGET} && mv libdiozero-system-utils.so ${TARGET}/.
fi
PATH=${OLD_PATH}

cd ../../..
