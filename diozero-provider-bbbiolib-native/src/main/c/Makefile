RM = rm -f
CC = gcc

CFLAGS = -Wall -fPIC
INCLUDES = -I/home/debian/BBBIOlib/BBBio_lib \
	-I$(JAVA_HOME)/include \
	-I$(JAVA_HOME)/include/linux
LFLAGS = 

LIBS = -lBBBio

SRCS = com_diozero_internal_provider_bbbiolib_BbbIoLibNative.c

OBJS = $(SRCS:.c=.o)

LIB_TARGET = libdiozero_bbbiolib.so

all: $(LIB_TARGET)

$(LIB_TARGET): $(OBJS)
	@echo [LINK] $(LIB_TARGET)
	@$(CC) -shared -o $(LIB_TARGET) $(OBJS) $(LFLAGS) $(LIBS)

.c.o:
	@echo [COMPILE] $<
	@$(CC) $(CFLAGS) $(INCLUDES) -c $< -o $@

clean:
	$(RM) $(OBJS) $(LIB_TARGET) *~

depend: $(SRCS)
	makedepend $(INCLUDES) $^
