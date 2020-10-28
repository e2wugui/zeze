
#include "../cxx/LuaState.h"
#include <iostream>

int main(int argc, char* argv[])
{
	printf("hello 1\n");

	lua_State * L = luaL_newstate();
	luaL_openlibs(L);
	try
	{
		Zeze::LuaHelper lua(L);
		lua.DoString("print(\"hello world\")");
	}
	catch (std::exception & ex)
	{
		std::cout << ex.what() << std::endl;
	}
	lua_close(L);
	return 0;
}
