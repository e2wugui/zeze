
using System;
using System.Net.NetworkInformation;
using System.Text;
using System.Text.Json;
using Zeze.Util;
using Zeze.Net;
using Zeze.Arch;

namespace Zezex
{
    public sealed partial class App
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public override T ReplaceModuleInstance<T>(T module)
        {
            return module;
        }


        private PersistentAtomicLong AsyncSocketSessionIdGen;
        public LinkdProvider LinkdProvider { get; set; }
        public LinkdApp LinkdApp { get; set; }

        public string ProviderServicePassiveIp { get; private set; }
        public int ProviderServicePasivePort { get; private set; }


        public void Start(string[] args)
        {

            int linkPort = -1;
            int providerPort = -1;
            for (int i = 0; i < args.Length; ++i)
            {
                switch (args[i])
                {
                    case "-LinkPort":
                        linkPort = int.Parse(args[++i]);
                        break;
                    case "-ProviderPort":
                        providerPort = int.Parse(args[++i]);
                        break;
                }
            }
            Start(linkPort, providerPort);
        }

        public void Start(int linkPort, int providerPort)
        {
            // Create
            var config = global::Zeze.Config.Load("linkd.xml");
            if (linkPort != -1)
            {
                if (config.ServiceConfMap.TryGetValue("LinkdService", out var service))
                    service.ForEachAcceptor((a) => a.Port = linkPort);
            }
            if (linkPort != -1)
            {
                if (config.ServiceConfMap.TryGetValue("ProviderService", out var service))
                    service.ForEachAcceptor((a) => a.Port = linkPort);
            }
            CreateZeze(config);
            CreateService();
            LinkdProvider = new LinkdProvider();
            LinkdApp = new LinkdApp("Game.Linkd", Zeze, LinkdProvider, ProviderService, LinkdService, LoadConfig.Load("load.json"));
            CreateModules();
            StartModules(); // 启动模块，装载配置什么的。
            Zeze.StartAsync().Wait(); // 启动数据库
            AsyncSocketSessionIdGen = PersistentAtomicLong.GetOrAdd(LinkdApp.GetName());
            AsyncSocket.SessionIdGenFunc = AsyncSocketSessionIdGen.Next;
            StartService(); // 启动网络
            LinkdApp.RegisterService(null).Wait();
        }

        public void Stop()
        {
            StopService(); // 关闭网络
            Zeze.Stop(); // 关闭数据库
            StopModules(); // 关闭模块,，卸载配置什么的。
            DestroyModules();
            DestroyService();
            DestroyZeze();
        }
    }
}
