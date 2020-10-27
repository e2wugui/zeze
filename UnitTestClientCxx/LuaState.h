#pragma once

#include "luaheader.h"
#include <string>
#include <stdexcept>

class Lua
{
	lua_State* L;
public:
	Lua()
	{
		L = luaL_newstate();
		if (NULL == L)
			throw std::exception("luaL_newstate return NULL");
		luaL_openlibs(L);
	}

	void PushBoolean(bool b)
	{
		lua_pushboolean(L, b);
	}

	void PushInteger(long long n)
	{
		lua_pushinteger(L, n);
	}

	void PushNumber(double n)
	{
		lua_pushnumber(L, n);
	}

	void PushBuffer(const char* buffer, int length)
	{
		lua_pushlstring(L, buffer, length);
	}

	void PushString(const char* s, int length)
	{
		PushBuffer(s, length);
	}

	void PushLightUserData(void * data)
	{
		lua_pushlightuserdata(L, data);
	}

	void PushNil()
	{
		lua_pushnil(L);
	}

	template <typename T>
	void PushObject(T obj)
	{
		if (obj == NULL)
		{
			PushNil();
			return;
		}

		PushLightUserData(obj);
	}

	//
	// ժҪ:
	//     Lua Load/Call status return
	enum class LuaStatus
	{
		//
		// ժҪ:
		//     success
		OK = 0,
		//
		// ժҪ:
		//     Yield
		Yield = 1,
		//
		// ժҪ:
		//     a runtime error.
		ErrRun = 2,
		//
		// ժҪ:
		//     syntax error during precompilation
		ErrSyntax = 3,
		//
		// ժҪ:
		//     memory allocation error. For such errors, Lua does not call the message handler.
		ErrMem = 4,
		//
		// ժҪ:
		//     error while running the message handler.
		ErrErr = 5
	};

	LuaStatus LoadBuffer(const char * buffer, int length)
	{
		return (LuaStatus)luaL_loadbufferx(L, buffer, length, NULL, NULL);
	}

	LuaStatus PCall(int arguments, int results, int errorFunctionIndex)
	{
		return (LuaStatus)lua_pcallk(L, arguments, results, errorFunctionIndex, 0, 0);
	}

	bool DoString(const char* code, int length)
	{
		return LoadBuffer(code, length) != LuaStatus::OK || PCall(0, -1, 0) != LuaStatus::OK;
	}

	bool DoString(const char* code)
	{
		return DoString(code, strlen(code));
	}

	void Call(int arguments, int results)
	{
		lua_callk(L, arguments, results, 0, 0);
	}

	//
	// ժҪ:
	//     Lua types
	enum class LuaType
	{
		None = -1,
		//
		// ժҪ:
		//     LUA_TNIL
		Nil = 0,
		//
		// ժҪ:
		//     LUA_TBOOLEAN
		Boolean = 1,
		//
		// ժҪ:
		//     LUA_TLIGHTUSERDATA
		LightUserData = 2,
		//
		// ժҪ:
		//     LUA_TNUMBER
		Number = 3,
		//
		// ժҪ:
		//     LUA_TSTRING
		String = 4,
		//
		// ժҪ:
		//     LUA_TTABLE
		Table = 5,
		//
		// ժҪ:
		//     LUA_TFUNCTION
		Function = 6,
		//
		// ժҪ:
		//     LUA_TUSERDATA
		UserData = 7,
		//
		// ժҪ:
		//     LUA_TTHREAD
		Thread = 8
	};

	LuaType Type(int index)
	{
		return (LuaType)lua_type(L, index);
	}

	bool IsTable(int index)
	{
		return Type(index) == LuaType::Table;
	}

	bool IsNil(int index)
	{
		return Type(index) == LuaType::Nil;
	}

	LuaType GetField(int index, const char * key)
	{
		return (LuaType)lua_getfield(L, index, key);
	}

	LuaType GetTable(int index)
	{
		return (LuaType)lua_gettable(L, index);
	}

	bool Next(int index)
	{
		lua_next(L, index) != 0;
	}

	void SetTable(int index)
	{
		lua_settable(L, index);
	}

	void CreateTable(int elements, int records)
	{
		lua_createtable(L, elements, records);
	}

	bool ToBoolean(int index)
	{
		return lua_toboolean(L, index) != 0;
	}

	long long ToInteger(int index)
	{
		int isnum = 0;
		return lua_tointegerx(L, index, &isnum);
	}

	double ToNumber(int index)
	{
		int isNum;
		return lua_tonumberx(L, index, &isNum);
	}
	
	std::string ToBuffer(int index)
	{
		size_t len;
		const char * buff;
		buff = lua_tolstring(L, index, &len);
		if (buff == NULL || len == 0)
			return std::string();
		return std::string(buff, len);
	}

	std::string ToString(int index)
	{
		return ToBuffer(index);
	}

	void Pop(int n)
	{
		lua_settop(L, -n - 1);
	}

	void SetGlobal(const char * name)
	{
		lua_setglobal(L, name);
	}

	void PushCClosure(lua_CFunction function, int n)
	{
		lua_pushcclosure(L, function, n);
	}

	void Register(const char * name, lua_CFunction function)
	{
		PushCClosure(function, 0);
		SetGlobal(name);
	}


	~Lua()
	{
		lua_close(L);
	}
};
