RM = rm -f
CC = gcc

CFLAGS = -Wall

INCLUDES = -I/home/pi/rpi_ws281x \
	-I$(JAVA_HOME)/include \
	-I$(JAVA_HOME)/include/linux

LFLAGS = -L/home/pi/rpi_ws281x

LIBS = -lws2811

SRCS = com_diozero_ws281xj_rpiws281x_WS281x.c

OBJS = $(SRCS:.c=.o)

LIB_TARGET = libws281xj.so

all: $(LIB_TARGET)

$(LIB_TARGET): $(OBJS)
	@echo [LINK] $(LIB_TARGET)
	@$(CC) -shared -o $(LIB_TARGET) $(OBJS) $(LFLAGS) $(LIBS)

.c.o:
	@echo [COMPILE] $<
	@$(CC) $(CFLAGS) $(INCLUDES) -c $< -o $@

clean:
	$(RM) $(OBJS) $(LIB_TARGET) $(MAIN) *~

depend: $(SRCS)
	makedepend $(INCLUDES) $^
