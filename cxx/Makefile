
CC = gcc
AR = ar
RANLIB = ranlib

TARGET = zezecxx.a

SRCS = ByteBuffer.cpp codec.cpp dh.cpp Net.cpp octets.cpp Protocol.cpp rfc2118.cpp security.cpp utils.cpp Vector.cpp
SRCSC = BI.c

OBJS = $(SRCS:%.cpp=%.o)
OBJSC = $(SRCS:%.c=%.o)

CXXFLAGS = -std=c++11 -march=native -mno-bmi2 -Wall -Wno-unknown-pragmas -O2
ARFLAGS = rcs

all: $(TARGET)

clean:
	rm -rf *.o
	rm $(TARGET)

$(TARGET): $(OBJS) $(OBJSC)
	$(AR) $(ARFLAGS) $@ $^
	$(RANLIB) $@
