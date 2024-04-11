package Zezex;

import java.nio.file.Files;
import java.nio.file.Paths;
import Zeze.Arch.LinkdApp;
import Zeze.Arch.LinkdProvider;
import Zeze.Arch.LoadConfig;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Util.JsonReader;
import Zeze.Util.PersistentAtomicLong;

public final class App extends Zeze.AppBase {
	public static final App Instance = new App();

	public static App getInstance() {
		return Instance;
	}

	public LinkdProvider LinkdProvider;

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
		AsyncSocket.setSessionIdGenFunc(PersistentAtomicLong.getOrAdd(LinkdApp.getName())::next);
		startService(); // 启动网络. after setSessionIdGenFunc
		LinkdApp.registerService(null);
	}

	public void Stop() throws Exception {
		stopService(); // 关闭网络
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

    @Override
    public void createModules() throws Exception {
        lock();
        try {
            Zeze.initialize(this);
            var _modules_ = createRedirectModules(new Class[] {
                Zezex.Linkd.ModuleLinkd.class,
            });
            if (_modules_ == null)
                return;

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
            if (Zezex_Linkd != null)
                Zezex_Linkd.Stop(this);
        } finally {
            unlock();
        }
    }

    public void stopBeforeModules() throws Exception {
        lock();
        try {
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
