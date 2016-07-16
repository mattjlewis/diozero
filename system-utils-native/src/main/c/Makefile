RM = rm -f
CC = gcc

CFLAGS = -Wall -fPIC

INCLUDES = -I/home/pi/rpi_ws281x -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux

LFLAGS = 

LIBS = 

SRCS = com_diozero_util_EpollNative.c com_diozero_util_PollNative.c com_diozero_util_SleepUtil.c com_diozero_util.c

OBJS = $(SRCS:.c=.o)

LIB_TARGET = libdiozero-system-utils.so

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
	$(RM) $(OBJS) $(TARGET) *~

depend: $(SRCS)
	makedepend $(INCLUDES) $^

