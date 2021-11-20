package Infinite;

import Zeze.Config;

public class App {
    demo.App app;
    Config config;

    public App(int serverId) {
        config = Config.Load("zeze.xml");
        config.setServerId(serverId);
        app = new demo.App();
    }

    public void Start() throws Throwable {
        app.Start(config);
    }

    public void Stop() throws Throwable {
        app.Stop();
    }

    public void Run(Runnable task) {

    }
}
