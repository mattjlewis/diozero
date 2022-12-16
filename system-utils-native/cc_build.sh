#!/bin/sh

cd src/main/c
LIB_DIR=../../../lib
LIB_NAME=libdiozero-system-utils.so

# x86_64
echo "Compiling for x86-64"
make clean && make CFLAGS="-march=x86-64"
if [ $? -eq 0 ]; then
  TARGET=${LIB_DIR}/linux-x86_64 && mkdir -p ${TARGET} && mv ${LIB_NAME} ${TARGET}/.
  make clean
fi
echo

# AArch64
echo "Compiling for AArch64"
make clean && make CROSS_PREFIX=aarch64-linux-gnu- CFLAGS="-march=armv8-a"
if [ $? -eq 0 ]; then
  TARGET=${LIB_DIR}/linux-aarch64 && mkdir -p ${TARGET} && mv ${LIB_NAME} ${TARGET}/.
  make clean
fi
echo

# ARMv7
echo "Compiling for ARMv7"
make clean && make CROSS_PREFIX=arm-linux-gnueabihf- CFLAGS="-mfpu=vfp -mfloat-abi=hard -march=armv7"
if [ $? -eq 0 ]; then
  TARGET=${LIB_DIR}/linux-armv7 && mkdir -p ${TARGET} && mv ${LIB_NAME} ${TARGET}/.
  make clean
fi
echo

# Finally build ARMv6 to be extra sure that PATH has no reference to the Pi ARMv6 cross compiler
echo "Compiling for ARMv6"
OLD_PATH=${PATH}
PATH=${PI_GCC_TARGET_DIR}/bin:${PATH} && make clean && make CROSS_PREFIX=arm-linux-gnueabihf- CFLAGS="-mfpu=vfp -mfloat-abi=hard -march=armv6"
if [ $? -eq 0 ]; then
  TARGET=${LIB_DIR}/linux-armv6 && mkdir -p ${TARGET} && mv ${LIB_NAME} ${TARGET}/.
  make clean
fi
PATH=${OLD_PATH}

cd ../../..
