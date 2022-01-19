#pragma once

#include "luaheader.h"
#include <string>
#include <stdexcept>

namespace Zeze
{

class LuaHelper
{
public:
	lua_State* L;

	LuaHelper(lua_State* from)
	{
		L = from;
	}

	LuaHelper()
	{
		L = NULL;
	}

	~LuaHelper()
	{
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

	void PushBuffer(const char* buffer, size_t length)
	{
		lua_pushlstring(L, buffer, length);
	}

	void PushString(const char* s, size_t length)
	{
		PushBuffer(s, length);
	}

	void PushString(const char* s)
	{
		PushString(s, strlen(s));
	}

	void PushString(const std::string& s)
	{
		PushString(s.data(), s.size());
	}

	void PushLightUserData(void* data)
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

	void * ToUserData(int index)
	{
		return lua_touserdata(L, index);
	}

	template <typename T>
	T ToObject(int index)
	{
		if (IsNil(index) || !IsLightUserData(index))
			return NULL;

		return (T)ToUserData(index);
	}

	enum class LuaStatus
	{
		OK = 0,
		Yield = 1,
		ErrRun = 2,
		ErrSyntax = 3,
		ErrMem = 4,
		ErrErr = 5
	};

	LuaStatus LoadBuffer(const char* buffer, size_t length)
	{
		return (LuaStatus)luaL_loadbufferx(L, buffer, length, NULL, NULL);
	}

	LuaStatus PCall(int arguments, int results, int errorFunctionIndex)
	{
		return (LuaStatus)lua_pcallk(L, arguments, results, errorFunctionIndex, 0, 0);
	}

	bool DoString(const char* code, size_t length)
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

	enum class LuaType
	{
		None = -1,
		Nil = 0,
		Boolean = 1,
		LightUserData = 2,
		Number = 3,
		String = 4,
		Table = 5,
		Function = 6,
		UserData = 7,
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

	bool IsLightUserData(int index)
	{
		return Type(index) == LuaType::LightUserData;
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
		return lua_next(L, index) != 0;
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

	LuaType GetGlobal(const char * name)
	{
		return (LuaType)lua_getglobal(L, name);
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
};

} // namespace Zeze
