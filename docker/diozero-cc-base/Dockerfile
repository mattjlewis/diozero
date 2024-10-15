FROM debian:buster

ARG DEBIAN_FRONTEND=noninteractive

# Update packages
RUN apt-get update && apt-get -y upgrade
# Install required tools and compilers
RUN apt-get install -y sudo git wget build-essential gawk texinfo bison file \
    gcc g++ gcc-8 g++-8 make cmake autoconf automake \
    bzip2 python python3 rsync libtool libtool-bin i2c-tools libi2c-dev
# Generic cross-compilation toolchain
RUN apt-get -y install build-essential lib32z1 \
  gcc-aarch64-linux-gnu gcc-arm-linux-gnueabihf binutils-aarch64-linux-gnu binutils-arm-linux-gnueabihf \
  crossbuild-essential-arm64 crossbuild-essential-armhf zlib1g-dev
# Cleanup
RUN apt-get clean autoclean && apt-get autoremove -y

# Enable x86
RUN dpkg --add-architecture i386
RUN apt-get update && apt-get -y install libstdc++6:i386 libgcc1:i386 zlib1g:i386

# Install JDK 11
RUN apt-get update && apt-get -y install openjdk-11-jdk-headless
ENV JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64

ENV PI_CC_TARGET_DIR=/opt/pi_cross_compiler

# Create the folder in which we’ll put the Pi cross compiler
RUN mkdir -p ${PI_CC_TARGET_DIR}

# Install the Raspberry Pi Linux headers
RUN git clone --depth=1 https://github.com/raspberrypi/linux
# Target architecture
ARG TARGET=arm-linux-gnueabihf
RUN cd linux && make ARCH=arm KERNEL=kernel7 INSTALL_HDR_PATH=${PI_CC_TARGET_DIR}/${TARGET} headers_install
# Cleanup
RUN rm -rf ./linux

# Environment variables
ENV RUN_AS_USER=develop
ENV CC_HOME_DIR=/home/${RUN_AS_USER}
# Working directory for building GCC
ENV BUILD_WORKING_DIR=${CC_HOME_DIR}/build

# Add a user so that we don't run as root
RUN useradd -m ${RUN_AS_USER} && echo "${RUN_AS_USER}:${RUN_AS_USER}" | chpasswd && adduser ${RUN_AS_USER} sudo
RUN echo "${RUN_AS_USER} ALL=(ALL) NOPASSWD: ALL" > /etc/sudoers.d/01_${RUN_AS_USER}-nopasswd

# Switch to the non-root user
USER ${RUN_AS_USER}

# Working directory
RUN mkdir -p ${BUILD_WORKING_DIR}
WORKDIR ${BUILD_WORKING_DIR}
