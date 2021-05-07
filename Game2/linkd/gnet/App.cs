
using System;
using System.Net.NetworkInformation;
using System.Text;
using System.Text.Json;

namespace gnet
{
    public sealed partial class App
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public Zeze.IModule ReplaceModuleInstance(Zeze.IModule module)
        {
            return module;
        }


        public Config Config { get; private set; }
        public Zeze.Services.ServiceManager.Agent ServiceManagerAgent { get; private set; }
        public const string GameServerServiceNamePrefix = "Game.Server.Module#";
        public const string GameLinkdServiceName = "Game.Linkd";

        private void LoadConfig()
        {
            try
            {
                string json = Encoding.UTF8.GetString(System.IO.File.ReadAllBytes("linkd.json"));
                Config = JsonSerializer.Deserialize<Config>(json);
            }
            catch (Exception)
            {
                //MessageBox.Show(ex.ToString());
            }
            if (null == Config)
                Config = new Config();
        }

        public string ProviderServicePassiveIp { get; private set; }
        public int ProviderServicePasivePort { get; private set; }

        private string GetOneIpAddress()
        {
            foreach (NetworkInterface neti in NetworkInterface.GetAllNetworkInterfaces())
            {
                if (neti.NetworkInterfaceType == NetworkInterfaceType.Loopback)
                    continue;

                IPInterfaceProperties property = neti.GetIPProperties();
                foreach (UnicastIPAddressInformation ip in property.UnicastAddresses)
                {
                    switch (ip.Address.AddressFamily)
                    {
                        case System.Net.Sockets.AddressFamily.InterNetworkV6:
                        case System.Net.Sockets.AddressFamily.InterNetwork:
                            return ip.Address.ToString();
                    }
                }
            }
            return null;
        }

        public void Start()
        {
            LoadConfig();
            Create();
            StartModules(); // 启动模块，装载配置什么的。
            Zeze.Start(); // 启动数据库
            StartService(); // 启动网络

            ProviderService.Config.ForEachAcceptor((a) =>
            {
                if (string.IsNullOrEmpty(ProviderServicePassiveIp))
                {
                    ProviderServicePassiveIp = a.Ip;
                    ProviderServicePasivePort = a.Port;
                }
            });

            if (ProviderServicePasivePort == 0)
                throw new Exception("ProviderService.Acceptor: No Config.");

            if (string.IsNullOrEmpty(ProviderServicePassiveIp))
            {
                ProviderServicePassiveIp = GetOneIpAddress();
            }
            if (string.IsNullOrEmpty(ProviderServicePassiveIp))
            {
                // 实在找不到ip地址，就设置成loopback。
                logger.Warn("ProviderServicePassiveIp No Config. set to 127.0.0.1");
                ProviderServicePassiveIp = "127.0.0.1";
            }

            ServiceManagerAgent = new Zeze.Services.ServiceManager.Agent(Zeze.Config,
            (agent) =>
            {
                agent.RegisterService(GameLinkdServiceName,
                    $"{ProviderServicePassiveIp}:{ProviderServicePasivePort}",
                    ProviderServicePassiveIp, ProviderServicePasivePort);
            },
            (subscribeState) =>
            {
                // 不需要做任何操作，直接使用得到的服务列表。
            });
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
