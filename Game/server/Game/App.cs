
namespace Game
{
    public sealed partial class App
    {
        public Zeze.IModule ReplaceModuleInstance(Zeze.IModule module)
        {
            return module;
        }

        public void Start()
        {
            StartModules(); // 启动模块，装载配置什么的。
            Zeze.Start(); // 启动数据库
            // 启动网络等等。
        }

        public void Stop()
        {
            // 关闭网络等等。
            Zeze.Stop(); // 关闭数据库
            StopModules(); // 关闭模块,，卸载配置什么的。
        }
    }
}
