RM = rm -f
CC = gcc

CFLAGS = -Wall -fPIC
INCLUDES = -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux
LFLAGS = 
SO_FILE_EXT = "so"

LIBS = 

SRCS = com_diozero_internal_provider_i2c_NativeI2CDevice.c com_diozero_internal_provider_spi_NativeSpiDevice.c com_diozero_util_EpollNative.c com_diozero_util_PollNative.c com_diozero_util_SleepUtil.c com_diozero_util.c $(EXTRA_SRC)

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
	@echo [COMPILE] $<
	@$(CC) $(CFLAGS) $(INCLUDES) -c $< -o $@

clean:
	$(RM) $(OBJS) $(LIB_TARGET) *~

depend: $(SRCS)
	makedepend $(INCLUDES) $^
