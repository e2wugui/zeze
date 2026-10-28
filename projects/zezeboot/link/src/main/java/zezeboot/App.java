package zezeboot;

import Zeze.Arch.LinkdApp;
import Zeze.Arch.LinkdProvider;
import Zeze.Arch.LoadConfig;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Util.PersistentAtomicLong;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SuppressWarnings("RedundantThrows")
public class App extends Zeze.AppBase {
	private static final Logger logger = LogManager.getLogger(App.class);
	public static final App instance = new App();

	private LinkdApp linkApp;

	public LinkdApp getLinkApp() {
		return linkApp;
	}

	public void start(String[] args) throws Exception {
		createZeze(Config.load("zeze_link.xml"));
		createService();
		linkApp = new LinkdApp("ZezeBoot.link", Zeze, new LinkdProvider(), ProviderService, LinkService,
				new LoadConfig());
		AsyncSocket.setSessionIdGenFunc(PersistentAtomicLong.getOrAdd(linkApp.getName())::next);
		createModules();
		Zeze.start();
		startModules();
		startService();
		linkApp.registerService(null);
		logger.info("link started!");
	}

	public void stop() throws Exception {
		stopService();
		stopModules();
		if (Zeze != null)
			Zeze.stop();
		destroyModules();
		destroyServices();
		destroyZeze();
	}

	// ZEZE_FILE_CHUNK {{{ GEN APP @formatter:off
    public Zeze.@org.jspecify.annotations.Nullable Application Zeze;

    public zezeboot.@org.jspecify.annotations.Nullable LinkService LinkService;
    public zezeboot.@org.jspecify.annotations.Nullable ProviderService ProviderService;

    public zezeboot.link.@org.jspecify.annotations.Nullable ModuleLink zezeboot_link;

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

            Zeze = new Zeze.Application("link", config);
        } finally {
            unlock();
        }
    }

    @Override
    public void createService() {
        lock();
        try {
            LinkService = new zezeboot.LinkService(getZeze());
            ProviderService = new zezeboot.ProviderService(getZeze());
        } finally {
            unlock();
        }
    }

    public static Class<?>[] redirectModuleClasses() {
        return new Class[] {
            zezeboot.link.ModuleLink.class,
        };
    }

    @Override
    public void createModules() throws Exception {
        lock();
        try {
            getZeze().initialize(this);
            var _modules_ = createRedirectModules(redirectModuleClasses());

            zezeboot_link = (zezeboot.link.ModuleLink)_modules_[0];
            zezeboot_link.Initialize(this);
            if (modules.put(zezeboot_link.getFullName(), zezeboot_link) != null)
                throw new IllegalStateException("duplicate module name: zezeboot_link");

            getZeze().setSchemas(new zezeboot.Schemas());
        } finally {
            unlock();
        }
    }

    public void destroyModules() throws Exception {
        lock();
        try {
            zezeboot_link = null;
            modules.clear();
        } finally {
            unlock();
        }
    }

    public void destroyServices() {
        lock();
        try {
            LinkService = null;
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
            java.util.Objects.requireNonNull(zezeboot_link).Start(this);
        } finally {
            unlock();
        }
    }

    @Override
    public void startLastModules() throws Exception {
        lock();
        try {
            java.util.Objects.requireNonNull(zezeboot_link).StartLast();
        } finally {
            unlock();
        }
    }

    public void stopModules() throws Exception {
        lock();
        try {
            if (Zeze == null)
                return;
            if (zezeboot_link != null)
                zezeboot_link.Stop(this);
        } finally {
            unlock();
        }
    }

    public void stopBeforeModules() throws Exception {
        lock();
        try {
            if (Zeze == null)
                return;
            if (zezeboot_link != null)
                zezeboot_link.StopBefore();
        } finally {
            unlock();
        }
    }

    public void startService() throws Exception {
        lock();
        try {
            java.util.Objects.requireNonNull(LinkService).start();
            java.util.Objects.requireNonNull(ProviderService).start();
        } finally {
            unlock();
        }
    }

    public void stopService() throws Exception {
        lock();
        try {
            if (LinkService != null)
                LinkService.stop();
            if (ProviderService != null)
                ProviderService.stop();
        } finally {
            unlock();
        }
    }
    // ZEZE_FILE_CHUNK }}} GEN APP @formatter:on
}
