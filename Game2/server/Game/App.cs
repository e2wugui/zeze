
using System.Collections.Generic;

namespace Game
{
    public sealed partial class App
    {
        public Zeze.Util.Scheduler Scheduler { get; } = new Zeze.Util.Scheduler();
        public Dictionary<int, int> StaticBinds { get; } = new Dictionary<int, int>();
        public ProviderModuleBinds ProviderModuleBinds { get; private set; }

        public Zeze.IModule ReplaceModuleInstance(Zeze.IModule module)
        {
            return Game.ModuleRedirect.Instance.ReplaceModuleInstance(module);
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

            var config = global::Zeze.Config.Load();
            if (AutoKeyLocalId != -1)
                config.AutoKeyLocalId = AutoKeyLocalId; // replace from args
            Create(config);

            ProviderModuleBinds = ProviderModuleBinds.Load();
            ProviderModuleBinds.BuildStaticBinds(Modules, Zeze.Config.AutoKeyLocalId, StaticBinds);
            StartModules(); // 启动模块，装载配置什么的。
            Zeze.Start(); // 启动数据库
            StartService(); // 启动网络
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
