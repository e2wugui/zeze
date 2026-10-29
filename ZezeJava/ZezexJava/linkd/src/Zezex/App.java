package Zezex;

import java.util.Objects;
import org.jspecify.annotations.Nullable;
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
import static Zeze.Util.Args.requireInt;

public final class App extends Zeze.AppBase {
	public static final App Instance = new App();

	public static App getInstance() {
		return Instance;
	}

	public @Nullable LinkdProvider LinkdProvider;
	private final Netty netty = new Netty();
	private @Nullable HttpServer httpServer;

	@Override
	public HttpServer getHttpServer() {
		return Objects.requireNonNull(httpServer);
	}

	private static @Nullable LoadConfig LoadConfig() {
		try {
			byte[] bytes = Files.readAllBytes(Paths.get("linkd.json"));
			return new JsonReader().buf(bytes).parse(LoadConfig.class);
			// return new ObjectMapper().readValue(bytes, LoadConfig.class);
		} catch (Exception e) {
			// e.printStackTrace();
		}
		return new LoadConfig();
	}

	public @Nullable LinkdApp LinkdApp;

	public void Start(String[] args) throws Exception {
		int linkPort = -1;
		int providerPort = -1;
		for (int i = 0; i < args.length; ++i) {
			switch (args[i]) {
			case "-LinkPort":
				linkPort = requireInt(args, ++i, "-LinkPort");
				break;
			case "-ProviderPort":
				providerPort = requireInt(args, ++i, "-ProviderPort");
				break;
			}
		}
		Start(-1, linkPort, providerPort);
	}

	public void Start(int serverId, int linkPort, int providerPort) throws Exception {
		Start(Config.load("linkd.xml"), serverId, linkPort, providerPort);
	}

	/** 使用调用方提供的配置启动，serverId 与监听端口仍可由参数覆盖。 */
	public void Start(Config config, int serverId, int linkPort, int providerPort) throws Exception {
		// Create
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
		LinkdApp = new LinkdApp("Game.Linkd", getZeze(), LinkdProvider, Objects.requireNonNull(ProviderService), Objects.requireNonNull(LinkdService), Objects.requireNonNull(LoadConfig()));
		createModules();
		// Start
		getZeze().start(); // 启动数据库
		startModules(); // 启动模块，装载配置什么的。
		// FND7-19/R3：不再全局安装PersistentAtomicLong发号——多App同JVM（linkd+Game.Server
		// 拓扑）值域重叠必撞号。Service实例级随机63位基址发号已保证跨JVM/跨App唯一。
		httpServer = new HttpServer(getZeze());
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
		Objects.requireNonNull(httpServer).close();
		if (Zeze != null)
			getZeze().stop(); // 关闭数据库
		stopModules(); // 关闭模块，卸载配置什么的。
		destroyModules();
		destroyServices();
		destroyZeze();
	}

	// ZEZE_FILE_CHUNK {{{ GEN APP @formatter:off
    public Zeze.@org.jspecify.annotations.Nullable Application Zeze;

    public Zezex.@org.jspecify.annotations.Nullable LinkdService LinkdService;
    public Zezex.@org.jspecify.annotations.Nullable ProviderService ProviderService;

    public Zezex.Linkd.@org.jspecify.annotations.Nullable ModuleLinkd Zezex_Linkd;

    @Override
    public Zeze.Application getZeze() {
        return java.util.Objects.requireNonNull(Zeze, "Zeze has not been created");
    }

    public void createZeze() throws Exception {
        createZeze(null);
    }

    @Override
    public void createZeze(Zeze.@org.jspecify.annotations.Nullable Config config) throws Exception {
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
            LinkdService = new Zezex.LinkdService(getZeze());
            ProviderService = new Zezex.ProviderService(getZeze());
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
            getZeze().initialize(this);
            var _modules_ = createRedirectModules(redirectModuleClasses());

            Zezex_Linkd = (Zezex.Linkd.ModuleLinkd)_modules_[0];
            Zezex_Linkd.Initialize(this);
            if (modules.put(Zezex_Linkd.getFullName(), Zezex_Linkd) != null)
                throw new IllegalStateException("duplicate module name: Zezex_Linkd");

            getZeze().setSchemas(new Zezex.Schemas());
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
            java.util.Objects.requireNonNull(Zezex_Linkd).Start(this);
        } finally {
            unlock();
        }
    }

    @Override
    public void startLastModules() throws Exception {
        lock();
        try {
            java.util.Objects.requireNonNull(Zezex_Linkd).StartLast();
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
            java.util.Objects.requireNonNull(LinkdService).start();
            java.util.Objects.requireNonNull(ProviderService).start();
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
