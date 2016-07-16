
RM = rm -f
CC = gcc

CFLAGS = -Wall -fPIC

INCLUDES = -I$(JAVA_HOME)/include \
	-I$(JAVA_HOME)/include/linux

LFLAGS =

LIBS = 

SRCS = com_diozero_internal_provider_jpi_MmapBufferNative.c

OBJS = $(SRCS:.c=.o)

LIB_TARGET = libjpi.so

all: $(LIB_TARGET)

$(LIB_TARGET): $(OBJS)
	@echo [LINK] $(LIB_TARGET)
	@$(CC) -shared -o $(LIB_TARGET) $(OBJS) $(LFLAGS) $(LIBS)

.c.o:
	@echo [COMPILE] $<
	@$(CC) $(CFLAGS) $(INCLUDES) -c $< -o $@

clean:
	$(RM) $(OBJS) $(TARGET) *~

depend: $(SRCS)
	makedepend $(INCLUDES) $^


