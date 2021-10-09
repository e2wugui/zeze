package Zeze.Services;

import Zeze.Net.*;
import Zeze.Serialize.*;
import Zeze.*;
import java.util.*;

///#define USE_KERA_LUA


/** 
 在lua线程中调用，一般实现：
 0 创建lua线程，
 1 创建 ToLua 实例。
 2 调用具体 Service.InitializeLua 初始化
 3 调用lua.main进入lua代码
 4 在lua.main中回调每个 Service.InitializeLua 中注册的方法回调
 * lua 热更的话需要建议重新创建 ToLua ，并且重新初始化（InitializeLua）。重用 ToLua 的话，需要调用一次 ToLua.LoadMeta();
*/

public interface FromLua {
	public String getName();
	public Net.Service getService();

	public Zeze.Services.ToLuaService.ToLua getToLua();
}