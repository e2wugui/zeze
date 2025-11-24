
CC = gcc
AR = ar
RANLIB = ranlib

TARGET = zezecxx.a

SRCS = Net.cpp
SRCSC = BI.c

OBJS = $(SRCS:%.cpp=%.o)
OBJSC = $(SRCS:%.c=%.o)

CXXFLAGS = -std=c++11 -Wall -O2
ARFLAGS = rcs

#OBJS := $(addsuffix .o,$(basename ${SRCS}))`

all: $(TARGET)

$(TARGET): $(OBJS) $(OBJSC)
	$(AR) $(ARFLAGS) $@ $^
	$(RANLIB) $@
