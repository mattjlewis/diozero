FROM diozero-cc-gcc

# Compile libi2c for various CPU architectures - diozero statically links against this library
ARG I2C_VERSION=4.1

WORKDIR ${BUILD_WORKING_DIR}
RUN wget -q -O - http://deb.debian.org/debian/pool/main/i/i2c-tools/i2c-tools_${I2C_VERSION}.orig.tar.xz | tar xJ
WORKDIR ${BUILD_WORKING_DIR}/i2c-tools-${I2C_VERSION}

# Note no need to build for x86-64 - that is provided by the i2ctools package itself
# aarch64
RUN make clean && make CC=aarch64-linux-gnu-gcc AR=aarch64-linux-gnu-ar STRIP=aarch64-linux-gnu-strip ARCH=armv8-a PREFIX=/usr
RUN sudo cp lib/libi2c.a /usr/lib/aarch64-linux-gnu/.

# armv7
RUN make clean && make CC=arm-linux-gnueabihf-gcc AR=arm-linux-gnueabihf-ar STRIP=arm-linux-gnueabihf-strip ARCH=armv7 CFLAGS="-mfpu=vfp -mfloat-abi=hard" PREFIX=/usr
RUN sudo cp lib/libi2c.a /usr/lib/arm-linux-gnueabihf/.

# armv6 - building this last out of paranoia to ensure PATH isn't modified
RUN PATH=${PI_CC_TARGET_DIR}/bin:${PATH} && make clean && make CC=arm-linux-gnueabihf-gcc AR=arm-linux-gnueabihf-ar STRIP=arm-linux-gnueabihf-strip ARCH=armv6 CFLAGS="-mfpu=vfp -mfloat-abi=hard" PREFIX=${PI_CC_TARGET_DIR}
RUN PATH=${PI_CC_TARGET_DIR}/bin:${PATH} && sudo make install PREFIX=${PI_CC_TARGET_DIR}/arm-linux-gnueabihf

# Cleanup
WORKDIR ${CC_HOME_DIR}
RUN sudo rm -rf ${BUILD_WORKING_DIR}
