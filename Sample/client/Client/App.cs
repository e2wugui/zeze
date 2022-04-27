
namespace Client
{
    public sealed partial class App
    {
        public void Start(string ip, int port)
        {
            var conf = global::Zeze.Config.Load("client.xml");
            CreateZeze(conf);
            CreateService();
            ClientService.Config.TryGetOrAddConnector(ip, port, true, out _);
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
