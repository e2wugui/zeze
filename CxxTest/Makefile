
CC = g++

TARGET = zezecxxtest

SRCS = \
main.cpp \
TestByteBuffer.cpp \
demo/M6/ModuleM6.cpp \
demo/M6/M7/ModuleM7.cpp \
demo/Module1/ModuleModule1.cpp \
demo/Module1/Module11/ModuleModule11.cpp \
Gen/demo/Bean1.cpp \
Gen/demo/M6/AbstractModule.cpp \
Gen/demo/M6/B67.cpp \
Gen/demo/M6/M7/AbstractModule.cpp \
Gen/demo/M6/M7/B67.cpp \
Gen/demo/Module1/AbstractModule.cpp \
Gen/demo/Module1/BAutoValue.cpp \
Gen/demo/Module1/BFood.cpp \
Gen/demo/Module1/BItem.cpp \
Gen/demo/Module1/BKuafu.cpp \
Gen/demo/Module1/BKuafuResult.cpp \
Gen/demo/Module1/BOnlyData.cpp \
Gen/demo/Module1/BRemoved2.cpp \
Gen/demo/Module1/BSimple.cpp \
Gen/demo/Module1/BTestSchemas.cpp \
Gen/demo/Module1/BValue.cpp \
Gen/demo/Module1/Module11/AbstractModule.cpp \
Gen/demo/Module2/BValue.cpp \
Gen/TaskTest/TaskExt/AbstractModule.cpp \
Gen/TaskTest/TaskExt/BMyTask01.cpp \
Gen/TaskTest/TaskExt/BTConditionExploreWorld.cpp \
Gen/TaskTest/TaskExt/BTConditionExploreWorldEvent.cpp \
TaskTest/TaskExt/ModuleTaskExt.cpp \
../cxx/zezecxx.a

OBJS = $(SRCS:%.cpp=%.o)

CXXFLAGS = -std=c++11 -march=native -mno-bmi2 -Wall -Wno-unknown-pragmas -Wunused-function -Wunused-variable -O2 -I . -I ../..

all: $(TARGET)

clean:
	find . -name "*.o" -type f -delete
	rm $(TARGET)

$(TARGET): $(OBJS)
	$(CC) $(CXXFLAGS) -o $@ $^
