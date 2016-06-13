
RM = rm -f
CC = gcc

CFLAGS = -Wall -fPIC

INCLUDES = -I$(JAVA_HOME)/include \
	-I$(JAVA_HOME)/include/linux

LFLAGS =

LIBS = 

SRCS = GpioMMapTest.c

OBJS = $(SRCS:.c=.o)

LIB_TARGET = libgpiommaptest.so

MAIN_TARGET = gpiommaptest

all: $(LIB_TARGET) $(MAIN_TARGET)

$(LIB_TARGET): $(OBJS)
	@echo [LINK] $(LIB_TARGET)
	@$(CC) -shared -o $(LIB_TARGET) $(OBJS) $(LFLAGS) $(LIBS)

$(MAIN_TARGET): $(OBJS)
	@echo [BUILD] $(MAIN_TARGET)
	@$(CC) -o $(MAIN_TARGET) $(OBJS) $(LFLAGS) $(LIBS)

.c.o:
	@echo [COMPILE] $<
	@$(CC) $(CFLAGS) $(INCLUDES) -c $< -o $@

clean:
	$(RM) $(OBJS) $(TARGET) *~

depend: $(SRCS)
	makedepend $(INCLUDES) $^


