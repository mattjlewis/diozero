FROM diozero/diozero-cc

ARG HOME_DIR=/home/${RUN_AS_USER}

RUN sudo apt-get update && sudo apt-get -y install scons

#RUN sudo apt-get -y install python3-distutils
RUN git clone https://github.com/jgarff/rpi_ws281x.git --depth 1
WORKDIR ${HOME_DIR}/rpi_ws281x

# aarch64
RUN scons V=yes TOOLCHAIN=aarch64-linux-gnu CFLAGS="-march=armv8-a"
#RUN make clean && make CROSS_PREFIX=aarch64-linux-gnu- CFLAGS="-march=armv8-a" PREFIX=/usr \
#		&& sudo make install PREFIX=/usr
RUN sudo mv libws2811.a /usr/lib/aarch64-linux-gnu/.

# armv7
RUN scons V=yes TOOLCHAIN=arm-linux-gnueabihf CFLAGS="-march=armv7"
#RUN make clean && make CROSS_PREFIX=arm-linux-gnueabihf- CFLAGS="-march=armv7" PREFIX=/usr \
#		&& sudo make install PREFIX=/usr
RUN sudo mv libws2811.a /usr/lib/arm-linux-gnueabihf/.

# armv6 - building this last out of paranoia to ensure PATH isn't modified
RUN PATH=${PI_GCC_TARGET_DIR}/bin:${PATH} && scons V=yes TOOLCHAIN=arm-linux-gnueabihf CFLAGS="-march=armv6 -mfloat-abi=hard"
#RUN PATH=${PI_GCC_TARGET_DIR}/bin:${PATH} \
#		&& make clean && make CROSS_PREFIX=arm-linux-gnueabihf- \
#			CFLAGS="-march=armv6 -mfpu=vfp -mfloat-abi=hard" PREFIX=${PI_GCC_TARGET_DIR} \
#		&& sudo make install DESTDIR=${PI_GCC_TARGET_DIR}/arm-linux-gnueabihf
RUN sudo mv libws2811.a ${PI_GCC_TARGET_DIR}/arm-linux-gnueabihf/lib/.
