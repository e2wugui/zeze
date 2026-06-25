package Demo;

import Zeze.Arch.Gen.GenModule;
import Zeze.Arch.ProviderModuleBinds;
import Zeze.Builtin.Provider.BKick;
import Zeze.Config;
import Zeze.Game.ProviderDirectWithTransmit;
import Zeze.Game.ProviderWithOnline;
import Zeze.Arch.ProviderApp;
import Zeze.Arch.LoadConfig;
import Zeze.Net.AsyncSocket;
import Zeze.Util.JsonReader;
import Zeze.Util.PersistentAtomicLong;

import java.nio.file.Files;
import java.nio.file.Paths;

public class App extends Zeze.AppBase {
	public static App Instance = new App();

    public ProviderWithOnline Provider;
    public ProviderApp ProviderApp;
    public ProviderDirectWithTransmit ProviderDirect;

    public static App getInstance() {
		return Instance;
	}

    private static LoadConfig LoadConfig() {
        try {
            byte[] bytes = Files.readAllBytes(Paths.get("linkd.json"));
            return new JsonReader().buf(bytes).parse(LoadConfig.class);
            // return new ObjectMapper().readValue(bytes, LoadConfig.class);
        } catch (Exception e) {
            // e.printStackTrace();
        }
        return new LoadConfig();
    }

	public void Start(String[] args) throws Exception {
        int serverId = -1;
        int providerDirectPort = -1;
        for (int i = 0; i < args.length; ++i) {
            switch (args[i]) {
                case "-ServerId":
                    serverId = Integer.parseInt(args[++i]);
                    break;
                case "-GenFileSrcRoot":
                    GenModule.instance.genFileSrcRoot = args[++i];
                    break;
                case "-ProviderDirectPort":
                    providerDirectPort = Integer.parseInt(args[++i]);
                    break;
            }
        }
        Start(serverId, providerDirectPort);
    }

    public void Start() throws Exception {
        Start(-1, -1);
    }

    public void Start(int serverId, int providerDirectPort) throws Exception {
        var config = Config.load("server.xml");
        if (serverId != -1) {
            config.setServerId(serverId); // replace from args
        }

        if (providerDirectPort != -1) {
            final int port = providerDirectPort;
            config.getServiceConfMap().get("ServerDirect").forEachAcceptor((a) -> a.setPort(port));
        }

		createZeze();
		createService();

        Provider = new ProviderWithOnline();
        Provider.setControlKick(BKick.eControlReportClient);

        ProviderDirect = new ProviderDirectWithTransmit();
        ProviderApp = new ProviderApp(Zeze, Provider, Server,
                "Game.Server.Module#",
                ProviderDirect, ServerDirect, "Game.Linkd", LoadConfig());
        Provider.create(this);

		createModules();

        if (GenModule.instance.genFileSrcRoot != null) {
            System.out.println("---------------");
            System.out.println("New Source File Has Generate. Re-Compile Need.");
            System.exit(0);
        }

        Zeze.getTimer().initializeOnlineTimer(ProviderApp);

		Zeze.start(); // 启动数据库
		startModules(); // 启动模块，装载配置什么的。

        Provider.start();

        PersistentAtomicLong socketSessionIdGen = PersistentAtomicLong.getOrAdd("Game.Server." + config.getServerId());
        AsyncSocket.setSessionIdGenFunc(socketSessionIdGen::next);
		startService(); // 启动网络

        // 服务准备好以后才注册和订阅。
        ProviderApp.startLast(ProviderModuleBinds.load(), modules);
	}

	public void Stop() throws Exception {
		stopService(); // 关闭网络
		stopModules(); // 关闭模块，卸载配置什么的。
		Zeze.stop(); // 关闭数据库
		destroyModules();
		destroyServices();
		destroyZeze();
	}

	// ZEZE_FILE_CHUNK {{{ GEN APP @formatter:off
    public Zeze.Application Zeze;

    public Demo.Server Server;
    public Demo.ServerDirect ServerDirect;

    public Demo.Login.ModuleLogin Demo_Login;
    public Demo.Fight.ModuleFight Demo_Fight;
    public Demo.MyWorld.ModuleMyWorld Demo_MyWorld;

    @Override
    public Zeze.Application getZeze() {
        return Zeze;
    }

    public void createZeze() throws Exception {
        createZeze(null);
    }

    @Override
    public void createZeze(Zeze.Config config) throws Exception {
        lock();
        try {
            if (Zeze != null)
                throw new IllegalStateException("Zeze Has Created!");

            Zeze = new Zeze.Application("server", config);
        } finally {
            unlock();
        }
    }

    @Override
    public void createService() {
        lock();
        try {
            Server = new Demo.Server(Zeze);
            ServerDirect = new Demo.ServerDirect(Zeze);
        } finally {
            unlock();
        }
    }

    @Override
    public void createModules() throws Exception {
        lock();
        try {
            Zeze.initialize(this);
            var _modules_ = createRedirectModules(new Class[] {
                Demo.Login.ModuleLogin.class,
                Demo.Fight.ModuleFight.class,
                Demo.MyWorld.ModuleMyWorld.class,
            });
            if (_modules_ == null)
                return;

            Demo_Login = (Demo.Login.ModuleLogin)_modules_[0];
            Demo_Login.Initialize(this);
            if (modules.put(Demo_Login.getFullName(), Demo_Login) != null)
                throw new IllegalStateException("duplicate module name: Demo_Login");

            Demo_Fight = (Demo.Fight.ModuleFight)_modules_[1];
            Demo_Fight.Initialize(this);
            if (modules.put(Demo_Fight.getFullName(), Demo_Fight) != null)
                throw new IllegalStateException("duplicate module name: Demo_Fight");

            Demo_MyWorld = (Demo.MyWorld.ModuleMyWorld)_modules_[2];
            Demo_MyWorld.Initialize(this);
            if (modules.put(Demo_MyWorld.getFullName(), Demo_MyWorld) != null)
                throw new IllegalStateException("duplicate module name: Demo_MyWorld");

            Zeze.setSchemas(new Demo.Schemas());
        } finally {
            unlock();
        }
    }

    public void destroyModules() throws Exception {
        lock();
        try {
            Demo_MyWorld = null;
            Demo_Fight = null;
            Demo_Login = null;
            modules.clear();
        } finally {
            unlock();
        }
    }

    public void destroyServices() {
        lock();
        try {
            Server = null;
            ServerDirect = null;
        } finally {
            unlock();
        }
    }

    public void destroyZeze() {
        lock();
        try {
            Zeze = null;
        } finally {
            unlock();
        }
    }

    public void startModules() throws Exception {
        lock();
        try {
            Demo_Login.Start(this);
            Demo_Fight.Start(this);
            Demo_MyWorld.Start(this);
        } finally {
            unlock();
        }
    }

    @Override
    public void startLastModules() throws Exception {
        lock();
        try {
            Demo_Login.StartLast();
            Demo_Fight.StartLast();
            Demo_MyWorld.StartLast();
        } finally {
            unlock();
        }
    }

    public void stopModules() throws Exception {
        lock();
        try {
            if (Demo_MyWorld != null)
                Demo_MyWorld.Stop(this);
            if (Demo_Fight != null)
                Demo_Fight.Stop(this);
            if (Demo_Login != null)
                Demo_Login.Stop(this);
        } finally {
            unlock();
        }
    }

    public void stopBeforeModules() throws Exception {
        lock();
        try {
            if (Demo_MyWorld != null)
                Demo_MyWorld.StopBefore();
            if (Demo_Fight != null)
                Demo_Fight.StopBefore();
            if (Demo_Login != null)
                Demo_Login.StopBefore();
        } finally {
            unlock();
        }
    }

    public void startService() throws Exception {
        lock();
        try {
            Server.start();
            ServerDirect.start();
        } finally {
            unlock();
        }
    }

    public void stopService() throws Exception {
        lock();
        try {
            if (Server != null)
                Server.stop();
            if (ServerDirect != null)
                ServerDirect.stop();
        } finally {
            unlock();
        }
    }
    // ZEZE_FILE_CHUNK }}} GEN APP @formatter:on
}
