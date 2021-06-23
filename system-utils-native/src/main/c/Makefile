RM           := rm -f
CC           := $(CROSS_PREFIX)gcc
AR           := $(CROSS_PREFIX)ar
SIZE         := $(CROSS_PREFIX)size
STRIP        := $(CROSS_PREFIX)strip
SHLIB        := $(CC) -shared
STRIP_FLAGS  :=

LIB_NAME     := diozero-system-utils

SRCS := com_diozero_internal_provider_builtin_serial_NativeSerialDevice.c \
        com_diozero_util_FileNative.c \
        com_diozero_util_MmapBufferNative.c \
        com_diozero_util_PollNative.c \
        com_diozero_util_SleepUtil.c \
        com_diozero_util_Util.c

LDLIBS :=

ifeq ($(OS),Windows_NT)
    uname_S := Windows
else
    uname_S := $(shell uname -s)
endif

ifeq ($(uname_S), Windows)
    OS_JAVA_INC := win32
    LIB_EXT     := dll
endif
ifeq ($(uname_S), Linux)
    SRCS        += com_diozero_internal_provider_builtin_gpio_NativeGpioDevice.c \
	                 com_diozero_internal_provider_builtin_i2c_NativeI2C.c \
	                 com_diozero_internal_provider_builtin_spi_NativeSpiDevice.c \
	                 com_diozero_util_EpollNative.c
    OS_JAVA_INC := linux
    LIB_EXT     := so
    LDLIBS      += -Wl,-Bstatic -li2c -Wl,-Bdynamic
    STRIP_FLAGS += --strip-unneeded
endif
ifeq ($(uname_S), Darwin)
    OS_JAVA_INC := darwin
    LIB_EXT     := dylib
    STRIP_FLAGS += -x
endif

# To check CFLAGS: arm-linux-gnueabihf-gcc -Q --help=target
# Equivalent for AArch64: aarch64-linux-gnu-gcc -Q --help=target
# ARMv6 CFLAGS:
#CFLAGS = -O3 -Wall -fPIC -march=armv6+fp -mfpu=vfp -mfloat-abi=hard
# BeagleBone Black/Green ARMv7 CFLAGS:
#CFLAGS = -O3 -Wall -fPIC -march=armv7-a+fp -mfpu=vfpv3-d16 -mfloat-abi=hard -mhard-float -mabi=aapcs-linux
override CFLAGS := -O3 -Wall -fPIC $(CFLAGS)

INCLUDES   := -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/$(OS_JAVA_INC)
LIB_PREFIX ?= lib
LIB_TARGET := $(LIB_PREFIX)$(LIB_NAME).$(LIB_EXT)
OBJS       := $(SRCS:.c=.o)

all: $(LIB_TARGET)

.c.o:
	@echo [COMPILE] $<
	@$(CC) $(CFLAGS) $(INCLUDES) -c $< -o $@

$(LIB_TARGET): $(OBJS)
	@echo [LINK] $(LIB_TARGET)
	@$(SHLIB) -o $(LIB_TARGET) $(OBJS) $(LDFLAGS) $(LDLIBS)
	@$(STRIP) $(STRIP_FLAGS) $(LIB_TARGET)

clean:
	$(RM) $(OBJS) $(LIB_TARGET) *~

depend: $(SRCS)
	makedepend $(INCLUDES) $^
