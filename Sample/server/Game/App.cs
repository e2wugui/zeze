
using System;
using System.Collections.Generic;
using System.Text;
using System.Text.Json;
using Zeze.Util;
using Zeze.Net;
using Zeze.Game;

namespace Game
{
    public sealed partial class App
    {
        public override Zeze.IModule ReplaceModuleInstance(Zeze.IModule module)
        {
            return Zeze.Redirect.ReplaceModuleInstance(this, module);
        }

        public ProviderImplementWithOnline ProviderImplementWithOnline { get; set; }
        public ProviderDirectWithTransmit ProviderDirectWithTransmit { get; set; }
        public Zeze.Arch.ProviderApp ProviderApp { get; set; }

        private PersistentAtomicLong AsyncSocketSessionIdGen;

        public void Start(string[] args)
        {
            string GenRedirect = null;
            //GenRedirect = "C:\\Users\\10501\\Desktop\\code\\zeze\\Sample\\server";
            int ServerId = -1;
            int ProviderDirectPort = -1;
            for (int i = 0; i < args.Length; ++i)
            {
                switch (args[i])
                {
                    case "-ServerId":
                        ServerId = int.Parse(args[++i]);
                        break;
                    case "-ProviderDirectPort":
                        ProviderDirectPort = int.Parse(args[++i]);
                        break;
                    case "-GenRedirect":
                        GenRedirect = args[++i];
                        break;
                }
            }
            Start(ServerId, ProviderDirectPort, GenRedirect);
        }

        public void Start(int ServerId, int ProviderDirectPort, string GenRedirect = null)
        {
            var config = global::Zeze.Config.Load("serverd.xml");
            if (ServerId != -1)
            {
                config.ServerId = ServerId; // replace from args
            }
            if (ProviderDirectPort != -1)
            {
                if (config.ServiceConfMap.TryGetValue("ServerDirect", out var direct))
                    direct.ForEachAcceptor((a) => a.Port = ProviderDirectPort);
            }
            CreateZeze(config);
            CreateService();

            ProviderImplementWithOnline = new ProviderImplementWithOnline();
            ProviderDirectWithTransmit = new ProviderDirectWithTransmit();
            ProviderApp = new Zeze.Arch.ProviderApp(Zeze, ProviderImplementWithOnline, Server, "Game.Server.Module#",
                ProviderDirectWithTransmit, ServerDirect, "Game.Linkd", global::Zeze.Arch.LoadConfig.Load("load.json"));
            ProviderImplementWithOnline.Online = new(ProviderApp);

            global::Zeze.Arch.Gen.GenModule.Instance.GenRedirect = GenRedirect;
            CreateModules();
            if (global::Zeze.Arch.Gen.GenModule.Instance.GenRedirect != null)
                throw new Exception("ModuleRedirect HasNewGen. Please Rebuild Now.");

            ProviderApp.initialize(global::Zeze.Arch.ProviderModuleBinds.Load(), Modules); // need Modules

            Zeze.StartAsync().Wait(); // 启动数据库
            StartModules(); // 启动模块，装载配置什么的。

            AsyncSocketSessionIdGen = PersistentAtomicLong.GetOrAdd("Server." + config.ServerId);
            AsyncSocket.SessionIdGenFunc = AsyncSocketSessionIdGen.Next;

            StartService(); // 启动网络
            ProviderImplementWithOnline.Online.Start();

            // 服务准备好以后才注册和订阅。
            _ = ProviderApp.StartLast();
        }

        public void Stop()
        {
            StopService(); // 关闭网络
            StopModules(); // 关闭模块,，卸载配置什么的。
            Zeze.Stop(); // 关闭数据库
            DestroyModules();
            DestroyService();
            DestroyZeze();
        }
    }
}
