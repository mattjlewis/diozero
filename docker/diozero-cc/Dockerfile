FROM diozero/diozero-cc-base

# Upgrade packages
RUN apt-get update && apt-get -y upgrade && apt-get -y clean autoclean autoremove

# Original instructions: https://solarianprogrammer.com/2018/05/06/building-gcc-cross-compiler-raspberry-pi/

# Environment variables
ENV PI_GCC_TARGET_DIR=/opt/cross-pi-gcc
ENV RUN_AS_USER=develop
ARG HOME_DIR=/home/${RUN_AS_USER}

# These must match the versions on the Raspberry Pi
ARG GCC_VERSION=gcc-8.3.0
ARG GLIBC_VERSION=glibc-2.28
ARG BINUTILS_VERSION=binutils-2.31.1
# Working directory for building GCC
ARG BUILD_WORKING_DIR=${HOME_DIR}/build
# Number of jobs value to pass the make -j command
ARG MAKE_JOBS=8

# Use GCC 8 as the default compiler otherwise there will be compilation errors
RUN update-alternatives --install /usr/bin/gcc gcc /usr/bin/gcc-8 999 && \
    update-alternatives --install /usr/bin/g++ g++ /usr/bin/g++-8 999 && \
    update-alternatives --install /usr/bin/cc cc /usr/bin/gcc-8 999 && \
    update-alternatives --install /usr/bin/c++ c++ /usr/bin/g++-8 999

# Create the folder in which we’ll put the Pi cross compiler
RUN mkdir -p ${PI_GCC_TARGET_DIR}

# Add a user so that we don't have to run as root
RUN useradd -m ${RUN_AS_USER} && echo "${RUN_AS_USER}:${RUN_AS_USER}" | chpasswd && adduser ${RUN_AS_USER} sudo
RUN echo "${RUN_AS_USER} ALL=(ALL) NOPASSWD: ALL" > /etc/sudoers.d/01_${RUN_AS_USER}-nopasswd

# Switch to the run as user
USER ${RUN_AS_USER}

# Working directory
WORKDIR ${BUILD_WORKING_DIR}

# Save the current PATH variable value, ensure that the cross compiler is first in the PATH
ARG OLD_PATH=${PATH}
ENV PATH=${PI_GCC_TARGET_DIR}/bin:$PATH

# Download and extract GCC
RUN wget https://ftp.gnu.org/gnu/gcc/${GCC_VERSION}/${GCC_VERSION}.tar.gz && \
    tar xf ${GCC_VERSION}.tar.gz && \
    rm ${GCC_VERSION}.tar.gz
# Download and extract LibC
RUN wget https://ftp.gnu.org/gnu/libc/${GLIBC_VERSION}.tar.bz2 && \
    tar xjf ${GLIBC_VERSION}.tar.bz2 && \
    rm ${GLIBC_VERSION}.tar.bz2
# Download and extract BinUtils
RUN wget https://ftp.gnu.org/gnu/binutils/${BINUTILS_VERSION}.tar.bz2 && \
    tar xjf ${BINUTILS_VERSION}.tar.bz2 && \
    rm ${BINUTILS_VERSION}.tar.bz2
# Download the GCC prerequisites
RUN cd ${GCC_VERSION} && contrib/download_prerequisites && rm *.tar.*

# Download and install the Raspberry Pi Linux headers
WORKDIR ${BUILD_WORKING_DIR}
RUN git clone --depth=1 https://github.com/raspberrypi/linux
WORKDIR ${BUILD_WORKING_DIR}/linux
ENV KERNEL=kernel7
RUN sudo make ARCH=arm INSTALL_HDR_PATH=${PI_GCC_TARGET_DIR}/arm-linux-gnueabihf headers_install

# Build BinUtils
WORKDIR ${BUILD_WORKING_DIR}/build-binutils
RUN ../${BINUTILS_VERSION}/configure \
    --prefix=${PI_GCC_TARGET_DIR} \
    --with-arch=armv6 --with-fpu=vfp --with-float=hard \
    --target=arm-linux-gnueabihf \
    --disable-multilib
RUN make -j${MAKE_JOBS}
RUN sudo make install

RUN sudo apt-get -y install zlib1g-dev

# Build the first part of GCC
WORKDIR ${BUILD_WORKING_DIR}/build-gcc
# Removed: --enable-bootstrap
RUN ../${GCC_VERSION}/configure \
    --prefix=${PI_GCC_TARGET_DIR} \
    --build=x86_64-linux-gnu --host=x86_64-linux-gnu --target=arm-linux-gnueabihf \
    --enable-languages=c,c++ \
    --with-arch=armv6 --with-fpu=vfp --with-float=hard \
    --disable-multilib \
    --program-prefix=arm-linux-gnueabihf- \
    --with-gcc-major-version-only \
    --enable-shared --enable-linker-build-id \
    --without-included-gettext --enable-threads=posix \
    --enable-nls --enable-clocale=gnu --enable-libstdcxx-debug \
    --enable-libstdcxx-time=yes --with-default-libstdcxx-abi=new --enable-gnu-unique-object \
    --enable-plugin --enable-default-pie --with-system-zlib --with-target-system-zlib \
    --disable-libitm --disable-libquadmath --disable-libquadmath-support --disable-sjlj-exceptions \
    --disable-werror --enable-checking=release \
    --libdir=${PI_GCC_TARGET_DIR}/arm-linux-gnueabihf/lib
RUN make -j${MAKE_JOBS} all-gcc
RUN sudo make install-gcc

# Build GLIBC
WORKDIR ${BUILD_WORKING_DIR}/build-glibc
RUN ../${GLIBC_VERSION}/configure \
    --prefix=${PI_GCC_TARGET_DIR}/arm-linux-gnueabihf \
    --build=x86_64-linux-gnu --host=arm-linux-gnueabihf --target=arm-linux-gnueabihf \
    --with-arch=armv6 --with-fpu=vfp --with-float=hard \
    --with-headers=${PI_GCC_TARGET_DIR}/arm-linux-gnueabihf/include \
    --disable-multilib libc_cv_forced_unwind=yes
RUN sudo make install-bootstrap-headers=yes install-headers
# Bit of a cludge - fix file permissions from the above command
RUN sudo chown -R ${RUN_AS_USER}:users .
RUN make -j${MAKE_JOBS} csu/subdir_lib
RUN sudo install csu/crt1.o csu/crti.o csu/crtn.o ${PI_GCC_TARGET_DIR}/arm-linux-gnueabihf/lib
RUN sudo arm-linux-gnueabihf-gcc -nostdlib -nostartfiles -shared -x c /dev/null \
    -o ${PI_GCC_TARGET_DIR}/arm-linux-gnueabihf/lib/libc.so
RUN sudo touch ${PI_GCC_TARGET_DIR}/arm-linux-gnueabihf/include/gnu/stubs.h

# Continue building GCC
WORKDIR ${BUILD_WORKING_DIR}/build-gcc
RUN make -j${MAKE_JOBS} all-target-libgcc
RUN sudo make install-target-libgcc

# Finish building GLIBC
WORKDIR ${BUILD_WORKING_DIR}/build-glibc
RUN make -j${MAKE_JOBS}
RUN sudo make install

# Finish building GCC
WORKDIR ${BUILD_WORKING_DIR}/build-gcc
RUN make -j${MAKE_JOBS}
RUN sudo make install

# Restore the old path value
ENV PATH=${OLD_PATH}

# Compile libi2c for various CPU architectures - diozero statically links against this library
ARG I2C_VERSION=4.1
WORKDIR ${BUILD_WORKING_DIR}
RUN wget http://deb.debian.org/debian/pool/main/i/i2c-tools/i2c-tools_${I2C_VERSION}.orig.tar.xz
RUN tar Jxf i2c-tools_${I2C_VERSION}.orig.tar.xz
RUN rm i2c-tools_${I2C_VERSION}.orig.tar.xz
WORKDIR ${BUILD_WORKING_DIR}/i2c-tools-${I2C_VERSION}
# Note no need to build for x86-64 - that is provided by the i2ctools package itself
# aarch64
RUN make clean && make CC=aarch64-linux-gnu-gcc AR=aarch64-linux-gnu-ar STRIP=aarch64-linux-gnu-strip ARCH=armv8-a PREFIX=/usr
RUN sudo cp lib/libi2c.a /usr/lib/aarch64-linux-gnu/.
# armv7
RUN make clean && make CC=arm-linux-gnueabihf-gcc AR=arm-linux-gnueabihf-ar STRIP=arm-linux-gnueabihf-strip ARCH=armv7 CFLAGS="-mfpu=vfp -mfloat-abi=hard" PREFIX=/usr
RUN sudo cp lib/libi2c.a /usr/lib/arm-linux-gnueabihf/.
# armv6 - building this last out of paranoia to ensure PATH isn't modified
RUN PATH=${PI_GCC_TARGET_DIR}/bin:${PATH} && make clean && make CC=arm-linux-gnueabihf-gcc AR=arm-linux-gnueabihf-ar STRIP=arm-linux-gnueabihf-strip ARCH=armv6 CFLAGS="-mfpu=vfp -mfloat-abi=hard" PREFIX=${PI_GCC_TARGET_DIR}
RUN PATH=${PI_GCC_TARGET_DIR}/bin:${PATH} && sudo make install PREFIX=${PI_GCC_TARGET_DIR}/arm-linux-gnueabihf

# Cleanup
WORKDIR ${HOME_DIR}
RUN sudo rm -rf ${BUILD_WORKING_DIR}

WORKDIR ${HOME}

# Finally add some other convenience packages
RUN sudo apt update && sudo apt-get -y install libsdl2-dev

ARG DEBIAN_MIRROR=http://ftp.uk.debian.org/debian
ARG RASPBIAN_MIRROR=http://raspbian.raspberrypi.org/raspbian
ARG LIB_SDL2=libsdl2-dev_2.0.9+dfsg1-1
ARG LIB_SDL2_PATH=pool/main/libs/libsdl2

# AArch64
ARG LIB_SDL2_ARM64_FILE=${LIB_SDL2}_arm64.deb
ARG LIB_SDL2_ARM64_URL=${DEBIAN_MIRROR}/${LIB_SDL2_PATH}/${LIB_SDL2_ARM64_FILE}
RUN wget ${LIB_SDL2_ARM64_URL}
RUN ar -p ${LIB_SDL2_ARM64_FILE} data.tar.xz | sudo tar Jxf - --strip-components=2 -C /usr/aarch64-linux-gnu
RUN rm ${LIB_SDL2_ARM64_FILE}

# ARMv7
ARG LIB_SDL2_ARMV7_FILE=${LIB_SDL2}_armhf.deb
ARG LIB_SDL2_ARMV7_URL=${DEBIAN_MIRROR}/${LIB_SDL2_PATH}/${LIB_SDL2_ARMV7_FILE}
RUN wget ${LIB_SDL2_ARMV7_URL}
RUN ar -p ${LIB_SDL2_ARMV7_FILE} data.tar.xz | sudo tar Jxf - --strip-components=2 -C /usr/arm-linux-gnueabihf
RUN rm ${LIB_SDL2_ARMV7_FILE}

# Raspbian ARMv6
ARG LIB_SDL2_ARMV6_FILE=${LIB_SDL2}_armhf.deb
ARG LIB_SDL2_ARMV6_URL=${RASPBIAN_MIRROR}/${LIB_SDL2_PATH}/${LIB_SDL2_ARMV6_FILE}
RUN wget ${LIB_SDL2_ARMV6_URL}
RUN ar -p ${LIB_SDL2_ARMV6_FILE} data.tar.xz | sudo tar Jxf - --strip-components=2 -C ${PI_GCC_TARGET_DIR}/arm-linux-gnueabihf
RUN sudo mv ${PI_GCC_TARGET_DIR}/arm-linux-gnueabihf/lib/arm-linux-gnueabihf/* ${PI_GCC_TARGET_DIR}/arm-linux-gnueabihf/lib
RUN rm ${LIB_SDL2_ARMV6_FILE}
