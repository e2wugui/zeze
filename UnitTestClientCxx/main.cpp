
#include "LuaState.h"

int main(int argc, char* argv[])
{
	printf("hello 1\n");

	Lua lua;

	lua.DoString("print(\"hello world\")");

	return 0;
}
