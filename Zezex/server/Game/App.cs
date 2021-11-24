
using System;
using System.Collections.Generic;
using System.Text;
using System.Text.Json;
using Zeze.Util;
using Zeze.Net;

namespace Game
{
    public sealed partial class App
    {
        public Dictionary<int, Zezex.Provider.BModule> StaticBinds { get; } = new Dictionary<int, Zezex.Provider.BModule>();
        public Dictionary<int, Zezex.Provider.BModule> DynamicModules { get; } = new Dictionary<int,Zezex.Provider.BModule>();

        public Zezex.ProviderModuleBinds ProviderModuleBinds { get; private set; }

        public override Zeze.IModule ReplaceModuleInstance(Zeze.IModule module)
        {
            return Zezex.ModuleRedirect.Instance.ReplaceModuleInstance(module);
        }

        public Config Config { get; private set; }
        public Load Load { get; } = new Load();

        public const string ServerServiceNamePrefix = "Game.Server.Module#";
        public const string LinkdServiceName = "Game.Linkd";

        private void LoadConfig()
        {
            try
            {
                string json = Encoding.UTF8.GetString(System.IO.File.ReadAllBytes("Game.json"));
                Config = JsonSerializer.Deserialize<Config>(json);
            }
            catch (Exception)
            {
                //MessageBox.Show(ex.ToString());
            }
            if (null == Config)
                Config = new Config();
        }

        private PersistentAtomicLong AsyncSocketSessionIdGen;

        public void Start(string[] args)
        {
            int ServerId = -1;
            for (int i = 0; i < args.Length; ++i)
            {
                switch (args[i])
                {
                    case "-ServerId":
                        ServerId = int.Parse(args[++i]);
                        break;
                }
            }

            LoadConfig();
            var config = global::Zeze.Config.Load();
            if (ServerId != -1)
                config.ServerId = ServerId; // replace from args
            Create(config);

            ProviderModuleBinds = Zezex.ProviderModuleBinds.Load();
            ProviderModuleBinds.BuildStaticBinds(Modules, Zeze.Config.ServerId, StaticBinds);
            ProviderModuleBinds.BuildDynamicBinds(Modules, Zeze.Config.ServerId, DynamicModules);

            Zeze.ServiceManagerAgent.OnChanged = (subscribeState) =>
            {
                Server.ApplyLinksChanged(subscribeState.ServiceInfos);
            };

            Zeze.Start(); // 启动数据库
            StartModules(); // 启动模块，装载配置什么的。

            AsyncSocketSessionIdGen = PersistentAtomicLong.GetOrAdd("Server." + config.ServerId);
            AsyncSocket.SessionIdGenFunc = AsyncSocketSessionIdGen.Next;

            StartService(); // 启动网络
            Load.StartTimerTask();

            // 服务准备好以后才注册和订阅。
            foreach (var staticBind in StaticBinds)
            {
                Zeze.ServiceManagerAgent.RegisterService($"{ServerServiceNamePrefix}{staticBind.Key}",
                    config.ServerId.ToString());
            }
            Zeze.ServiceManagerAgent.SubscribeService(LinkdServiceName,
                global::Zeze.Services.ServiceManager.SubscribeInfo.SubscribeTypeSimple);
        }

        public void Stop()
        {
            StopService(); // 关闭网络
            StopModules(); // 关闭模块,，卸载配置什么的。
            Zeze.Stop(); // 关闭数据库
            Destroy();
        }
    }
}
