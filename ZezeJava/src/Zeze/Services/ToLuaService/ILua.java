package Zeze.Services.ToLuaService;

import Zeze.*;
import Zeze.Services.*;
import Zeze.Net.*;
import Zeze.Serialize.*;
import java.util.*;

public interface ILua {
	public boolean DoString(String str);
	public void GetField(int index, String name);
	public void PushNil();
	public boolean IsTable(int index);
	public boolean Next(int index);
	public long ToInteger(int index);
	public void Pop(int n);
	public LuaType GetGlobal(String name);
	public void PushObject(Object obj);
	public void PushInteger(long l);
	public void Call(int arguments, int results);
	public void PushString(String str);
	public void SetTable(int index);
	public void GetTable(int index);
	public boolean IsNil(int index);
	public double ToNumber(int index);
	public String toString(int index);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public byte[] ToBuffer(int index);
	public byte[] ToBuffer(int index);
	public boolean ToBoolean(int index);
	public void CreateTable(int elements, int records);
	public void PushBoolean(boolean v);
	public void PushNumber(double number);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public void PushBuffer(byte[] buffer);
	public void PushBuffer(byte[] buffer);
	public <T> T ToObject(int index);
//C# TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
//#if USE_KERA_LUA
	public void Register(String name, KeraLua.LuaFunction func);
//#endif // end USE_KERA_LUA
}