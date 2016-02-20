
RM = rm -f
CC = gcc

CFLAGS = -Wall

INCLUDES = -I/home/pi/rpi_ws281x \
					-I$(JAVA_HOME)/include \
					-I$(JAVA_HOME)/include/linux

LFLAGS = -L/home/pi/rpi_ws281x

LIBS = -lws2811

SRCS = com_diozero_ws281xj_WS281x.c

OBJS = $(SRCS:.c=.o)

LIB_TARGET = libws281xj.so
MAIN = ws281xj

all: $(LIB_TARGET) $(MAIN)

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

