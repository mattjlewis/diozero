RM = rm -f

CC           = $(CROSS_PREFIX)gcc
AR           = $(CROSS_PREFIX)ar
SIZE         = $(CROSS_PREFIX)size
STRIP        = $(CROSS_PREFIX)strip
SHLIB        = $(CC) -shared
STRIPLIB     = $(STRIP) --strip-unneeded

# To check CFLAGS: arm-linux-gnueabihf-gcc -Q --help=target
# Alternative for 64 bit: aarch64-linux-gnu-gcc
#CFLAGS = -O3 -Wall -fPIC -march=armv6+fp -mfpu=vfp -mfloat-abi=hard
CFLAGS = -O3 -Wall -fPIC -march=$(ARCH) $(CC_CFLAGS)
# BeagleBone Black:
#CFLAGS = -O3 -Wall -fPIC -march=armv7-a+fp -mfpu=vfpv3-d16 -mfloat-abi=hard -mhard-float -mabi=aapcs-linux

INCLUDES = -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux

LFLAGS = $(CC_LFLAGS)

LIBS = -Wl,-Bstatic -li2c -Wl,-Bdynamic

SO_FILE_EXT = so

SRCS = com_diozero_internal_provider_builtin_gpio_NativeGpioDevice.c \
	com_diozero_internal_provider_builtin_i2c_NativeI2C.c \
	com_diozero_internal_provider_builtin_serial_NativeSerialDevice.c \
	com_diozero_internal_provider_builtin_spi_NativeSpiDevice.c \
	com_diozero_util_EpollNative.c \
	com_diozero_util_MmapBufferNative.c \
	com_diozero_util_PollNative.c \
	com_diozero_util_SleepUtil.c \
	com_diozero_util_Util.c

OBJS = $(SRCS:.c=.o)

LIB_TARGET = libdiozero-system-utils.$(SO_FILE_EXT)

all: $(LIB_TARGET)

.c.o:
	$(CC) $(CFLAGS) $(INCLUDES) -c $< -o $@

$(LIB_TARGET): $(OBJS)
	$(SHLIB) -o $(LIB_TARGET) $(OBJS) $(LFLAGS) $(LIBS)
	$(STRIPLIB) $(LIB_TARGET)

clean:
	$(RM) $(OBJS) $(LIB_TARGET) *~

depend: $(SRCS)
	makedepend $(INCLUDES) $^
