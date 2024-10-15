#!/bin/sh

rebuild_rpi_ws281x () {
  echo "Rebuilding rpi_ws281x"
  
  git pull
  
  echo "AArch64"
  scons V=yes TOOLCHAIN=aarch64-linux-gnu CFLAGS="-march=armv8-a"
  sudo mv libws2811.a /usr/lib/aarch64-linux-gnu/.
  
  echo "ARMv7"
  scons V=yes TOOLCHAIN=arm-linux-gnueabihf CFLAGS="-march=armv7"
  sudo mv libws2811.a /usr/lib/arm-linux-gnueabihf/.
  
  echo "ARMv6"
  old_path=${PATH}
  PATH=${PI_CC_TARGET_DIR}/bin:${PATH} && scons V=yes TOOLCHAIN=arm-linux-gnueabihf CFLAGS="-march=armv6 -mfloat-abi=hard"
  sudo mv libws2811.a ${PI_CC_TARGET_DIR}/arm-linux-gnueabihf/lib/.
  PATH=${old_path}
}

entry_dir=$PWD

# Check if rpi_ws281x has updated
cd ${HOME}/rpi_ws281x
git fetch
if [ ! $(git rev-parse HEAD) = $(git rev-parse @{u}) ]; then
  rebuild_rpi_ws281x
else
  echo "rpi_ws281x is up to date"
fi

cd ${entry_dir}/src/main/native
LIB_DIR=../../../lib
LIB_NAME=libws281xj.so

# AArch64
echo "Compiling for AArch64"
make clean && make CROSS_PREFIX=aarch64-linux-gnu- CFLAGS="-march=armv8-a" RPI_WS281X_DIR=/home/develop/rpi_ws281x
if [ $? -eq 0 ]; then
  TARGET=${LIB_DIR}/linux-aarch64 && mkdir -p ${TARGET} && mv ${LIB_NAME} ${TARGET}/.
  make clean
fi
echo

# ARMv7
echo "Compiling for ARMv7"
make clean && make CROSS_PREFIX=arm-linux-gnueabihf- CFLAGS="-mfpu=vfp -mfloat-abi=hard -march=armv7" RPI_WS281X_DIR=/home/develop/rpi_ws281x
if [ $? -eq 0 ]; then
  TARGET=${LIB_DIR}/linux-armv7 && mkdir -p ${TARGET} && mv ${LIB_NAME} ${TARGET}/.
  make clean
fi
echo

# Finally build ARMv6 to be extra sure that PATH has no reference to the Pi ARMv6 cross compiler
echo "Compiling for ARMv6"
OLD_PATH=${PATH}
PATH=${PI_CC_TARGET_DIR}/bin:${PATH} && make clean && make CROSS_PREFIX=arm-linux-gnueabihf- CFLAGS="-mfpu=vfp -mfloat-abi=hard -march=armv6" RPI_WS281X_DIR=/home/develop/rpi_ws281x
if [ $? -eq 0 ]; then
  TARGET=${LIB_DIR}/linux-armv6 && mkdir -p ${TARGET} && mv ${LIB_NAME} ${TARGET}/.
  make clean
fi
PATH=${OLD_PATH}

cd ../../..
