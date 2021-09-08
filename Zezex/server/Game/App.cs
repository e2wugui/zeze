
using System;
using System.Collections.Generic;
using System.Text;
using System.Text.Json;

namespace Game
{
    public sealed partial class App
    {
        public Dictionary<int, Zezex.Provider.BModule> StaticBinds { get; } = new Dictionary<int, Zezex.Provider.BModule>();
        public Zezex.ProviderModuleBinds ProviderModuleBinds { get; private set; }

        public Zeze.IModule ReplaceModuleInstance(Zeze.IModule module)
        {
            return Game.ModuleRedirect.Instance.ReplaceModuleInstance(module);
        }

        public Config Config { get; private set; }
        public Load Load { get; } = new Load();

        public const string GameServerServiceNamePrefix = "Game.Server.Module#";
        public const string GameLinkdServiceName = "Game.Linkd";

        private void LoadConfig()
        {
            try
            {
                string json = Encoding.UTF8.GetString(System.IO.File.ReadAllBytes("game.json"));
                Config = JsonSerializer.Deserialize<Config>(json);
            }
            catch (Exception)
            {
                //MessageBox.Show(ex.ToString());
            }
            if (null == Config)
                Config = new Config();
        }


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

            // 这里有点问题，ServiceManager 增加了 AutoKey 功能，这个可能被广泛使用，需要先初始化。
            // 但是连接成功后，服务就被注册到 ServiceManager 中，而此时本服务还没启动完成。
            // 可能会造成服务临时性不可用。XXX 以后再考虑解决这个问题。
            Zeze.ServiceManagerAgent = new Zeze.Services.ServiceManager.Agent(config,
                (agent) =>
                {
                    foreach (var staticBind in StaticBinds)
                    {
                        agent.RegisterService($"{GameServerServiceNamePrefix}{staticBind.Key}",
                            config.ServerId.ToString());
                    }
                    agent.SubscribeService(GameLinkdServiceName, global::Zeze.Services.ServiceManager.SubscribeInfo.SubscribeTypeSimple);
                },
                (subscribeState) =>
                {
                    Server.ApplyLinksChanged(subscribeState.ServiceInfos);
                });

            Zeze.Start(); // 启动数据库
            StartModules(); // 启动模块，装载配置什么的。
            StartService(); // 启动网络
            Load.StartTimerTask();
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
