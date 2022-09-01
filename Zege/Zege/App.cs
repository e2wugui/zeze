
using Zeze.Net;
using Zeze;

namespace Zege
{
    public sealed partial class App
    {
        public Connector Connector;

        public void Start(string ip, int port)
        {
            // 客户端使用默认配置启动，不用配置文件。
            CreateZeze(new Config());

            CreateService();

            if (null != ip && ip.Length > 0 && port != 0)
            {
                // 指定ip,port连接服务器。
                ClientService.Config.TryGetOrAddConnector(ip, port, false, out Connector);
            }
            else
            {
                // 从配置里面找到第一个Connnector。
                ClientService.Config.ForEachConnector((c) => { Connector = c; return false; });
            }
            if (null == Connector)
                throw new Exception("miss Connector!");

            CreateModules();
            Zeze.StartAsync().Wait(); // 启动数据库
            StartModules(); // 启动模块，装载配置什么的。
            StartService(); // 启动网络
        }

        public void Stop()
        {
            StopService(); // 关闭网络
            StopModules(); // 关闭模块，卸载配置什么的。
            Zeze.Stop(); // 关闭数据库
            DestroyModules();
            DestroyService();
            DestroyZeze();
        }
    }
}
