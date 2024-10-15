FROM diozero/diozero-cc-base

# Original instructions: https://solarianprogrammer.com/2018/05/06/building-gcc-cross-compiler-raspberry-pi/

# These should match the versions on the Raspberry Pi
# Now 2.31 on Pi Bullseye image as of Aug 2022
ARG GLIBC_VERSION=glibc-2.29
# Now 2.35.2 on Pi Bullseye image as of Aug 2022
ARG BINUTILS_VERSION=binutils-2.31.1
# Now 10.2.1 on Pi Bullseye image as of Aug 2022
ARG GCC_VERSION=gcc-8.3.0
# Target architecture to compile for
ARG TARGET=arm-linux-gnueabihf
# Number of jobs value to pass the make -j command
ARG MAKE_JOBS=4

# Use GCC 8 as the default compiler otherwise there will be compilation errors
RUN sudo update-alternatives --install /usr/bin/gcc gcc /usr/bin/gcc-8 999 && \
    sudo update-alternatives --install /usr/bin/g++ g++ /usr/bin/g++-8 999 && \
    sudo update-alternatives --install /usr/bin/cc cc /usr/bin/gcc-8 999 && \
    sudo update-alternatives --install /usr/bin/c++ c++ /usr/bin/g++-8 999

# Save the current PATH variable value, ensure that the cross compiler is first in the PATH
ARG OLD_PATH=${PATH}
ENV PATH=${PI_CC_TARGET_DIR}/bin:$PATH

# Download and extract LibC
RUN wget -q -O - https://ftp.gnu.org/gnu/libc/${GLIBC_VERSION}.tar.bz2 | tar xj
# Download and extract Binutils
RUN wget -q -O - https://ftp.gnu.org/gnu/binutils/${BINUTILS_VERSION}.tar.bz2 | tar xj
# Download and extract GCC
RUN wget -q -O - https://ftp.gnu.org/gnu/gcc/${GCC_VERSION}/${GCC_VERSION}.tar.gz | tar xz
# Download the GCC prerequisites
RUN cd ${GCC_VERSION} && contrib/download_prerequisites && rm *.tar.*

# Build Binutils
RUN mkdir -p ${BUILD_WORKING_DIR}/build-binutils
WORKDIR ${BUILD_WORKING_DIR}/build-binutils
RUN ../${BINUTILS_VERSION}/configure \
    --prefix=${PI_CC_TARGET_DIR} \
    --with-arch=armv6 --with-fpu=vfp --with-float=hard \
    --target=${TARGET} \
    --disable-multilib
RUN make -j${MAKE_JOBS}
RUN sudo make install

# Build the first part of GCC
RUN mkdir -p ${BUILD_WORKING_DIR}/build-gcc
WORKDIR ${BUILD_WORKING_DIR}/build-gcc
# Fixing limits.h: https://stackoverflow.com/questions/58199020/locally-built-gcc-cross-compiler-reports-mb-len-max-wrong-with-d-fortify-source
#WORKDIR ${GCC_VERSION}
#RUN cat gcc/limitx.h gcc/glimits.h gcc/limity.h > \
#    `dirname $(${TARGET}-gcc -print-libgcc-file-name)`/include-fixed/limits.h
# Removed: --enable-bootstrap
RUN ../${GCC_VERSION}/configure \
    --prefix=${PI_CC_TARGET_DIR} \
    --build=x86_64-linux-gnu --host=x86_64-linux-gnu --target=${TARGET} \
    --enable-languages=c,c++ \
    --with-arch=armv6 --with-fpu=vfp --with-float=hard \
    --disable-multilib \
    --with-headers=${PI_CC_TARGET_DIR}/${TARGET}/include/linux \
    --program-prefix=${TARGET}- \
    --with-gcc-major-version-only \
    --enable-shared --enable-linker-build-id \
    --without-included-gettext --enable-threads=posix \
    --enable-nls --enable-clocale=gnu --enable-libstdcxx-debug \
    --enable-libstdcxx-time=yes --with-default-libstdcxx-abi=new --enable-gnu-unique-object \
    --enable-plugin --enable-default-pie --with-system-zlib --with-target-system-zlib \
    --disable-libitm --disable-libquadmath --disable-libquadmath-support --disable-sjlj-exceptions \
    --disable-werror --enable-checking=release \
    --libdir=${PI_CC_TARGET_DIR}/${TARGET}/lib
RUN make -j${MAKE_JOBS} all-gcc
RUN sudo make install-gcc
# https://stackoverflow.com/questions/44419593/gcc-4-9-4-cross-compiler-build-limits-h-issue
RUN sudo rm -rf ${PI_CC_TARGET_DIR}/${TARGET}/sys-include

# Build GLIBC
RUN mkdir -p ${BUILD_WORKING_DIR}/build-glibc
WORKDIR ${BUILD_WORKING_DIR}/build-glibc
RUN ../${GLIBC_VERSION}/configure \
    --prefix=${PI_CC_TARGET_DIR}/${TARGET} \
    --build=x86_64-linux-gnu --host=${TARGET} --target=${TARGET} \
    --with-arch=armv6 --with-fpu=vfp --with-float=hard \
    --with-headers=${PI_CC_TARGET_DIR}/${TARGET}/include \
    --disable-multilib libc_cv_forced_unwind=yes
RUN sudo make install-bootstrap-headers=yes install-headers
# Cludge - fix file permissions from the above command
RUN sudo chown -R ${RUN_AS_USER}:users .
RUN make -j${MAKE_JOBS} csu/subdir_lib
RUN sudo install csu/crt1.o csu/crti.o csu/crtn.o ${PI_CC_TARGET_DIR}/${TARGET}/lib
RUN sudo ${TARGET}-gcc -nostdlib -nostartfiles -shared -x c /dev/null \
    -o ${PI_CC_TARGET_DIR}/${TARGET}/lib/libc.so
RUN sudo touch ${PI_CC_TARGET_DIR}/${TARGET}/include/gnu/stubs.h

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

# FIXME Fix the limits.h header file. Why is the cross-compiler one missing some info?
# https://stackoverflow.com/questions/44419593/gcc-4-9-4-cross-compiler-build-limits-h-issue
RUN sudo cp /usr/lib/gcc-cross/arm-linux-gnueabihf/8/include-fixed/limits.h ${PI_CC_TARGET_DIR}/arm-linux-gnueabihf/lib/gcc/arm-linux-gnueabihf/8/include-fixed/limits.h

# Cleanup
WORKDIR ${CC_HOME_DIR}
RUN rm -rf ${BUILD_WORKING_DIR}/build-binutils
RUN rm -rf ${BUILD_WORKING_DIR}/${BINUTILS_VERSION}
RUN rm -rf ${BUILD_WORKING_DIR}/build-glibc
RUN rm -rf ${BUILD_WORKING_DIR}/${GLIBC_VERSION}
RUN rm -rf ${BUILD_WORKING_DIR}/build-gcc
RUN rm -rf ${BUILD_WORKING_DIR}/${GCC_VERSION}

# Restore the old path value
ENV PATH=${OLD_PATH}
