using System;
using System.Threading;

namespace UnitTestClient
{
    public class Program
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public static void Main(string[] args)
        {
            demo.App.Instance.Start();
            KeraLua.Lua lua = new KeraLua.Lua();
            try
            {
                // 网络建立好，handshake 以后的事件会保存下来，等待lua调用ZezeUpdate才会触发。所以可以先连接。
                demo.App.Instance.Client.Connect("127.0.0.1", 9999);
                if (lua.DoString("package.path = package.path .. ';../../../LuaSrc/?.lua;../../../LuaGen/?.lua'"))
                    throw new Exception("package.path");
                lua.DoString("return package.path");
                Console.WriteLine(lua.ToString(-1));
                demo.App.Instance.Client.InitializeLua(new Zeze.Services.ToLuaService.ToLua(lua));
                if (lua.DoString("require 'main'"))
                    throw new Exception("run main.lua error");
            }
            catch (Exception ex)
            {
                Console.WriteLine(ex);
                Console.WriteLine(lua.ToString(-1));
            }
            finally
            {
                demo.App.Instance.Stop();
            }
        }
    }
}
