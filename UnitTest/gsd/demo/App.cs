
namespace demo
{
    public sealed partial class App
    {
        public void Start(Zeze.Config config = null)
        {
            Create(config);
            StartModules(); // 启动模块，装载配置什么的。
            Zeze.Start(); // 启动数据库
            StartService(); // 启动网络等等。
        }

        public void Stop()
        {
            StopService(); // 关闭网络等等。
            Zeze.Stop(); // 关闭数据库
            StopModules(); // 关闭模块,，卸载配置什么的。
            Destroy();
        }
    }
}
