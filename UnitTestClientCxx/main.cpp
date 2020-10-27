
#include "../cxx/LuaState.h"

int main(int argc, char* argv[])
{
	printf("hello 1\n");

	Zeze::LuaState lua;

	lua.DoString("print(\"hello world\")");

	return 0;
}
