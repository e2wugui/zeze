
using System;
using System.Collections.Generic;
using System.Text;
using System.Text.Json;

namespace Game
{
    public sealed partial class App
    {
        public Dictionary<int, Zezex.Provider.BModule> StaticBinds { get; } = new Dictionary<int, Zezex.Provider.BModule>();
        public ProviderModuleBinds ProviderModuleBinds { get; private set; }

        public Zeze.IModule ReplaceModuleInstance(Zeze.IModule module)
        {
            return Game.ModuleRedirect.Instance.ReplaceModuleInstance(module);
        }

        public Config Config { get; private set; }
        public Load Load { get; } = new Load();
        public Zeze.Services.ServiceManager.Agent ServiceManagerAgent { get; private set; }
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
            int AutoKeyLocalId = -1;
            for (int i = 0; i < args.Length; ++i)
            {
                switch (args[i])
                {
                    case "-AutoKeyLocalId":
                        AutoKeyLocalId = int.Parse(args[++i]);
                        break;
                }
            }

            LoadConfig();
            var config = global::Zeze.Config.Load();
            if (AutoKeyLocalId != -1)
                config.AutoKeyLocalId = AutoKeyLocalId; // replace from args
            Create(config);

            ProviderModuleBinds = ProviderModuleBinds.Load();
            ProviderModuleBinds.BuildStaticBinds(Modules, Zeze.Config.AutoKeyLocalId, StaticBinds);

            StartModules(); // 启动模块，装载配置什么的。
            Zeze.Start(); // 启动数据库
            StartService(); // 启动网络
            ServiceManagerAgent = new Zeze.Services.ServiceManager.Agent(config,
                (agent) =>
                {
                    foreach (var staticBind in StaticBinds)
                    {
                        agent.RegisterService($"{GameServerServiceNamePrefix}{staticBind.Key}",
                            config.AutoKeyLocalId.ToString());
                    }
                    agent.SubscribeService(GameLinkdServiceName, global::Zeze.Services.ServiceManager.SubscribeInfo.SubscribeTypeSimple);
                },
                (subscribeState) =>
                {
                    Server.ApplyLinksChanged(subscribeState.ServiceInfos);
                });
            Load.StartTimerTask();
        }

        public void Stop()
        {
            StopService(); // 关闭网络
            Zeze.Stop(); // 关闭数据库
            StopModules(); // 关闭模块,，卸载配置什么的。
            Destroy();
        }
    }
}
