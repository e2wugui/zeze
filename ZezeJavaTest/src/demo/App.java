package demo;

public class App extends AppBase {
    public static App Instance = new App();
    public static App getInstance() {
        return Instance;
    }

    public void Start() {
        Create();
        Zeze.Start(); // 启动数据库
        StartModules(); // 启动模块，装载配置什么的。
        StartService(); // 启动网络
    }

    public void Stop() {
        StopService(); // 关闭网络
        StopModules(); // 关闭模块,，卸载配置什么的。
        Zeze.Stop(); // 关闭数据库
        Destroy();
    }
}
