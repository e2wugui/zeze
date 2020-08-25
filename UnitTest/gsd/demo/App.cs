
namespace demo
{
    public sealed partial class App
    {
        // 在这里定义你的全局变量吧
        public void Start()
        {
            // 这个方法需要自己调用，在这里可以调整顺序。
            StartModules(); // 启动模块，装载配置什么的。
            Zeze.Start(); // 启动数据库
            // 启动网络等等。
        }

        public void Stop()
        {
            // 这个方法需要自己调用，在这里可以调整顺序。
            // 关闭网络等等。
            Zeze.Stop(); // 关闭数据库
            StopModules(); // 关闭模块,，卸载配置什么的。
        }
    }
}
