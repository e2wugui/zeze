package Zezex;

import java.nio.file.Files;
import java.nio.file.Paths;
import Zeze.Arch.LinkdApp;
import Zeze.Arch.LinkdProvider;
import Zeze.Arch.LoadConfig;
import Zeze.Config;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Services.ReloadClassServer;
import Zeze.Services.RunClassServer;
import Zeze.Util.JsonReader;
import Zeze.Util.TaskSpec;

public final class App extends Zeze.AppBase {
	public static final App Instance = new App();

	public static App getInstance() {
		return Instance;
	}

	public LinkdProvider LinkdProvider;
	private final Netty netty = new Netty();
	private HttpServer httpServer;

	@Override
	public HttpServer getHttpServer() {
		return httpServer;
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

	public LinkdApp LinkdApp;

	public void Start(String[] args) throws Exception {
		int linkPort = -1;
		int providerPort = -1;
		for (int i = 0; i < args.length; ++i) {
			switch (args[i]) {
			case "-LinkPort":
				linkPort = Integer.parseInt(args[++i]);
				break;
			case "-ProviderPort":
				providerPort = Integer.parseInt(args[++i]);
				break;
			}
		}
		Start(-1, linkPort, providerPort);
	}

	public void Start(int serverId, int linkPort, int providerPort) throws Exception {
		// Create
		var config = Config.load("linkd.xml");
		if (serverId != -1)
			config.setServerId(serverId);
		if (linkPort != -1) {
			config.getServiceConfMap().get("LinkdService").forEachAcceptor((a) -> a.setPort(linkPort));
		}
		if (linkPort != -1) {
			config.getServiceConfMap().get("ProviderService").forEachAcceptor((a) -> a.setPort(providerPort));
		}
		createZeze(config);
		createService();
		LinkdProvider = new LinkdProvider();
		LinkdApp = new LinkdApp("Game.Linkd", Zeze, LinkdProvider, ProviderService, LinkdService, LoadConfig());
		createModules();
		// Start
		Zeze.start(); // 启动数据库
		startModules(); // 启动模块，装载配置什么的。
		// FND7-19/R3：不再全局安装PersistentAtomicLong发号——多App同JVM（linkd+Game.Server
		// 拓扑）值域重叠必撞号。Service实例级随机63位基址发号已保证跨JVM/跨App唯一。
		httpServer = new HttpServer(Zeze);
		// 【安全警示】/reloadClass与/runClass是无鉴权的任意字节码注入/执行端点（见两类
		// 的类注释），下方挂载后随httpServer监听linkPort+10000且未指定host（bind所有
		// 网卡）。生产环境请改绑回环/内网管理面（HttpServer.start指定host）或直接移除
		// 这两行挂载，绝不可暴露公网。当前保持默认绑定行为不变。
		ReloadClassServer reloadClassServer = new ReloadClassServer(this, "/reloadClass", "upload", "filename");
		reloadClassServer.start();
		RunClassServer runClassServer = new RunClassServer(this, "/runClass", "clazz", "filename");
		httpServer.start(netty, linkPort + 10000);
		startService(); // 启动网络. after setSessionIdGenFunc
		LinkdApp.registerService(null);

		TaskSpec.ofAction(HotReloadTest::print).schedulePeriod(2000, 2000);
	}

	public void Stop() throws Exception {
		stopService(); // 关闭网络
		httpServer.close();
		if (Zeze != null)
			Zeze.stop(); // 关闭数据库
		stopModules(); // 关闭模块，卸载配置什么的。
		destroyModules();
		destroyServices();
		destroyZeze();
	}

	// ZEZE_FILE_CHUNK {{{ GEN APP @formatter:off
    public Zeze.Application Zeze;

    public Zezex.LinkdService LinkdService;
    public Zezex.ProviderService ProviderService;

    public Zezex.Linkd.ModuleLinkd Zezex_Linkd;

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

            Zeze = new Zeze.Application("linkd", config);
        } finally {
            unlock();
        }
    }

    @Override
    public void createService() {
        lock();
        try {
            LinkdService = new Zezex.LinkdService(Zeze);
            ProviderService = new Zezex.ProviderService(Zeze);
        } finally {
            unlock();
        }
    }

    public static Class<?>[] redirectModuleClasses() {
        return new Class[] {
            Zezex.Linkd.ModuleLinkd.class,
        };
    }

    @Override
    public void createModules() throws Exception {
        lock();
        try {
            Zeze.initialize(this);
            var _modules_ = createRedirectModules(redirectModuleClasses());

            Zezex_Linkd = (Zezex.Linkd.ModuleLinkd)_modules_[0];
            Zezex_Linkd.Initialize(this);
            if (modules.put(Zezex_Linkd.getFullName(), Zezex_Linkd) != null)
                throw new IllegalStateException("duplicate module name: Zezex_Linkd");

            Zeze.setSchemas(new Zezex.Schemas());
        } finally {
            unlock();
        }
    }

    public void destroyModules() throws Exception {
        lock();
        try {
            Zezex_Linkd = null;
            modules.clear();
        } finally {
            unlock();
        }
    }

    public void destroyServices() {
        lock();
        try {
            LinkdService = null;
            ProviderService = null;
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
            Zezex_Linkd.Start(this);
        } finally {
            unlock();
        }
    }

    @Override
    public void startLastModules() throws Exception {
        lock();
        try {
            Zezex_Linkd.StartLast();
        } finally {
            unlock();
        }
    }

    public void stopModules() throws Exception {
        lock();
        try {
            if (Zeze == null)
                return;
            if (Zezex_Linkd != null)
                Zezex_Linkd.Stop(this);
        } finally {
            unlock();
        }
    }

    public void stopBeforeModules() throws Exception {
        lock();
        try {
            if (Zeze == null)
                return;
            if (Zezex_Linkd != null)
                Zezex_Linkd.StopBefore();
        } finally {
            unlock();
        }
    }

    public void startService() throws Exception {
        lock();
        try {
            LinkdService.start();
            ProviderService.start();
        } finally {
            unlock();
        }
    }

    public void stopService() throws Exception {
        lock();
        try {
            if (LinkdService != null)
                LinkdService.stop();
            if (ProviderService != null)
                ProviderService.stop();
        } finally {
            unlock();
        }
    }
    // ZEZE_FILE_CHUNK }}} GEN APP @formatter:on
}
