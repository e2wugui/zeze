package Zeze.Services.ToLuaService;

import Zeze.*;
import Zeze.Services.*;
import Zeze.Net.*;
import Zeze.Serialize.*;
import java.util.*;

//C# TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
//#if USE_KERA_LUA
public class Kera implements ILua {
	private KeraLua.Lua Lua;
	public Kera(KeraLua.Lua Lua) {
		this.Lua = Lua;
	}
	public final void Call(int arguments, int results) {
		Lua.Call(arguments, results);
	}
	public final void CreateTable(int elements, int records) {
		Lua.CreateTable(elements, records);
	}
	public final boolean DoString(String str) {
		return Lua.DoString(str);
	}
	public final void GetField(int index, String name) {
		Lua.GetField(index, name);
	}
	public final LuaType GetGlobal(String name) {
		return LuaType.forValue(Lua.GetGlobal(name));
	}
	public final void GetTable(int index) {
		Lua.GetTable(index);
	}
	public final boolean IsNil(int index) {
		return Lua.IsNil(index);
	}
	public final boolean IsTable(int index) {
		return Lua.IsTable(index);
	}
	public final boolean Next(int index) {
		return Lua.Next(index);
	}
	public final void Pop(int n) {
		Lua.Pop(n);
	}
	public final void PushBoolean(boolean v) {
		Lua.PushBoolean(v);
	}
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: void ILua.PushBuffer(byte[] buffer)
	public final void PushBuffer(byte[] buffer) {
		Lua.PushBuffer(buffer);
	}
	public final void PushInteger(long l) {
		Lua.PushInteger(l);
	}
	public final void PushNil() {
		Lua.PushNil();
	}
	public final void PushNumber(double number) {
		Lua.PushNumber(number);
	}
	public final void PushObject(Object obj) {
		Lua.PushObject(obj);
	}
	public final void PushString(String str) {
		Lua.PushString(str);
	}
	public final void SetTable(int index) {
		Lua.SetTable(index);
	}
	public final boolean ToBoolean(int index) {
		return Lua.ToBoolean(index);
	}
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: byte[] ILua.ToBuffer(int index)
	public final byte[] ToBuffer(int index) {
		return Lua.ToBuffer(index);
	}
	public final long ToInteger(int index) {
		return Lua.ToInteger(index);
	}
	public final double ToNumber(int index) {
		return Lua.ToNumber(index);
	}
	public final String toString(int index) {
		return Lua.toString(index);
	}
	public final <T> T ToObject(int index) {
		return Lua.<T>ToObject(index, false);
	}
	public final void Register(String name, KeraLua.LuaFunction func) {
		Lua.Register(name, func);
	}
}