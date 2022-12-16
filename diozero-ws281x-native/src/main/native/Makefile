RM           := rm -f
CC           := $(CROSS_PREFIX)gcc
AR           := $(CROSS_PREFIX)ar
SIZE         := $(CROSS_PREFIX)size
STRIP        := $(CROSS_PREFIX)strip
SHLIB        := $(CC) -shared
STRIP_FLAGS  :=

LIB_NAME     := ws281xj

SRCS = com_diozero_ws281xj_rpiws281x_WS281x.c

LDLIBS := -lws2811
RPI_WS281X_DIR ?= $(HOME)/rpi_ws281x

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
    OS_JAVA_INC := linux
    LIB_EXT     := so
    STRIP_FLAGS += --strip-unneeded
endif
ifeq ($(uname_S), Darwin)
    OS_JAVA_INC := darwin
    LIB_EXT     := dylib
    STRIP_FLAGS += -x
endif

override CFLAGS := -O3 -Wall -fPIC $(CFLAGS)

INCLUDES := -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/$(OS_JAVA_INC) -I$(RPI_WS281X_DIR)

LIB_PREFIX ?= lib
LIB_TARGET := $(LIB_PREFIX)$(LIB_NAME).$(LIB_EXT)
OBJS       := $(SRCS:.c=.o)
#LDFLAGS    := -L$(RPI_WS281X_DIR)

all: $(LIB_TARGET)

.c.o:
	@echo [COMPILE] $<
	@$(CC) $(CFLAGS) $(INCLUDES) -c $< -o $@

$(LIB_TARGET): $(OBJS)
	@echo [LINK] $(LIB_TARGET)
	@$(SHLIB) -o $(LIB_TARGET) $(OBJS) $(LDFLAGS) $(LDLIBS)
	@$(STRIP) $(STRIP_FLAGS) $(LIB_TARGET)

clean:
	$(RM) $(OBJS) $(LIB_TARGET) $(MAIN) *~

depend: $(SRCS)
	makedepend $(INCLUDES) $^
