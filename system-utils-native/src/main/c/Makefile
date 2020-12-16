RM = rm -f
CC = $(CROSS_COMPILE)gcc

# To check CFLAGS: arm-linux-gnueabihf-gcc -Q --help=target
# Alternative for 64 bit: aarch64-linux-gnu-gcc
#CFLAGS = -Wall -fPIC -O3 -march=armv6+fp -mfpu=vfp -mfloat-abi=hard
CFLAGS = -Wall -fPIC -O3 -march=$(ARCH) $(CC_CFLAGS)
# BeagleBone Black:
#CFLAGS = -Wall -fPIC -O3 -march=armv7-a+fp -mfpu=vfpv3-d16 -mfloat-abi=hard -mhard-float -mabi=aapcs-linux

INCLUDES = -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux -I$(JAVA_HOME)/include/darwin
LFLAGS = -L/home/vagrant/libs/$(CROSS_COMPILE)lib
SO_FILE_EXT = so

LIBS = -Wl,-Bstatic -li2c -Wl,-Bdynamic

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

$(LIB_TARGET): $(OBJS)
	@echo [LINK] $(LIB_TARGET)
	@$(CC) -shared -o $(LIB_TARGET) $(OBJS) $(LFLAGS) $(LIBS)

$(MAIN): $(OBJS)
	@echo [LINK] $(MAIN)
	$(CC) -o $(MAIN) $(OBJS) $(LFLAGS) $(LIBS)

.c.o:
	@echo [COMPILE] $(CFLAGS) $<
	@$(CC) $(CFLAGS) $(INCLUDES) -c $< -o $@

clean:
	$(RM) $(OBJS) $(LIB_TARGET) *~

depend: $(SRCS)
	makedepend $(INCLUDES) $^
