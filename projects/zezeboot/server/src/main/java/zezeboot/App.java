package zezeboot;

import Zeze.Arch.LoadConfig;
import Zeze.Arch.ProviderApp;
import Zeze.Arch.ProviderModuleBinds;
import Zeze.Arch.ProviderOnly;
import Zeze.Component.DbWeb;
import Zeze.Config;
import Zeze.Game.ProviderDirectWithTransmit;
import Zeze.Net.AsyncSocket;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Util.PersistentAtomicLong;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SuppressWarnings("RedundantThrows")
public class App extends Zeze.AppBase {
	private static final Logger logger = LogManager.getLogger(App.class);
	public static final App instance = new App();

	private ProviderApp providerApp;
	private ProviderModuleBinds providerModuleBinds;
	private Netty netty;
	private Zeze.Netty.HttpServer httpServer;
	private DbWeb dbWeb;

	public ProviderApp getProviderApp() {
		return providerApp;
	}

	public ProviderModuleBinds getProviderModuleBinds() {
		return providerModuleBinds;
	}

	public DbWeb getDbWeb() {
		return dbWeb;
	}

	public void start(String[] args) throws Exception {
		var config = Config.load("zeze_server.xml");
		createZeze(config);
		createService();
		var provider = new ProviderOnly();
		providerApp = new ProviderApp(Zeze, provider, ProviderClient, "ZezeBoot.server.module#",
				new ProviderDirectWithTransmit(), DirectService, "ZezeBoot.link", new LoadConfig());
		provider.create(this);
		providerModuleBinds = ProviderModuleBinds.load();
		createModules();
		Zeze.getTimer().initializeOnlineTimer(providerApp);
		Zeze.start();
		startModules();
		provider.start();
		AsyncSocket.setSessionIdGenFunc(PersistentAtomicLong.getOrAdd("ZezeBoot.server." + config.getServerId())::next);
		startService();
		providerApp.startLast(providerModuleBinds, modules);
		Zeze.getTimer().start();

		netty = new Netty(1);
		httpServer = new HttpServer(Zeze);
		dbWeb = new DbWeb();
		dbWeb.Initialize(this);
		dbWeb.RegisterHttpServlet(httpServer);
		httpServer.start(netty, 8080);

		logger.info("server started!");
	}

	public void stop() throws Exception {
		if (httpServer != null) {
			httpServer.close();
			httpServer = null;
		}
		if (netty != null) {
			netty.close();
			netty = null;
		}
		stopService();
		stopModules();
		if (Zeze != null)
			Zeze.stop();
		destroyModules();
		destroyServices();
		destroyZeze();
	}

	// ZEZE_FILE_CHUNK {{{ GEN APP @formatter:off
    public Zeze.Application Zeze;

    public zezeboot.DirectService DirectService;
    public zezeboot.ProviderClient ProviderClient;

    public zezeboot.login.ModuleLogin zezeboot_login;

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
            DirectService = new zezeboot.DirectService(Zeze);
            ProviderClient = new zezeboot.ProviderClient(Zeze);
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
                zezeboot.login.ModuleLogin.class,
            });
            if (_modules_ == null)
                return;

            zezeboot_login = (zezeboot.login.ModuleLogin)_modules_[0];
            zezeboot_login.Initialize(this);
            if (modules.put(zezeboot_login.getFullName(), zezeboot_login) != null)
                throw new IllegalStateException("duplicate module name: zezeboot_login");

            Zeze.setSchemas(new zezeboot.Schemas());
        } finally {
            unlock();
        }
    }

    public void destroyModules() throws Exception {
        lock();
        try {
            zezeboot_login = null;
            modules.clear();
        } finally {
            unlock();
        }
    }

    public void destroyServices() {
        lock();
        try {
            DirectService = null;
            ProviderClient = null;
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
            zezeboot_login.Start(this);
        } finally {
            unlock();
        }
    }

    @Override
    public void startLastModules() throws Exception {
        lock();
        try {
            zezeboot_login.StartLast();
        } finally {
            unlock();
        }
    }

    public void stopModules() throws Exception {
        lock();
        try {
            if (zezeboot_login != null)
                zezeboot_login.Stop(this);
        } finally {
            unlock();
        }
    }

    public void stopBeforeModules() throws Exception {
        lock();
        try {
            if (zezeboot_login != null)
                zezeboot_login.StopBefore();
        } finally {
            unlock();
        }
    }

    public void startService() throws Exception {
        lock();
        try {
            DirectService.start();
            ProviderClient.start();
        } finally {
            unlock();
        }
    }

    public void stopService() throws Exception {
        lock();
        try {
            if (DirectService != null)
                DirectService.stop();
            if (ProviderClient != null)
                ProviderClient.stop();
        } finally {
            unlock();
        }
    }
    // ZEZE_FILE_CHUNK }}} GEN APP @formatter:on
}
