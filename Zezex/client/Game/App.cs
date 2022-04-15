
namespace Game
{
    public sealed partial class App
    {
        public override Zeze.IModule ReplaceModuleInstance(Zeze.IModule module)
        {
            return module;
        }

        public void Start()
        {
            CreateZeze();
            CreateService();
            CreateModules();
            StartModules(); // 启动模块，装载配置什么的。
            Zeze.StartAsync().Wait(); // 启动数据库
            StartModules();
            StartService(); // 启动网络
        }

        public void Stop()
        {
            StopService(); // 关闭网络等等。
            Zeze.Stop(); // 关闭数据库
            StopModules(); // 关闭模块,，卸载配置什么的。
            DestroyModules();
            DestroyService();
            DestroyZeze();
        }
    }
}
