package zezeboot;

import Zeze.Config;

@SuppressWarnings("RedundantThrows")
public class App extends Zeze.AppBase {
	public static final App instance = new App();

	public void start(String[] args) throws Exception {
		createZeze(Config.load("zeze_client.xml"));
		createService();
		createModules();
		Zeze.start();
		startModules();
		startService();
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
    public Zeze.Application Zeze;

    public zezeboot.LinkClient LinkClient;

    public Zeze.Builtin.LinkdBase.ModuleLinkdBase Zeze_Builtin_LinkdBase;
    public zezeboot.link.ModuleLink zezeboot_link;
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

            Zeze = new Zeze.Application("client", config);
        } finally {
            unlock();
        }
    }

    @Override
    public void createService() {
        lock();
        try {
            LinkClient = new zezeboot.LinkClient(Zeze);
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
                Zeze.Builtin.LinkdBase.ModuleLinkdBase.class,
                zezeboot.link.ModuleLink.class,
                zezeboot.login.ModuleLogin.class,
            });
            if (_modules_ == null)
                return;

            Zeze_Builtin_LinkdBase = (Zeze.Builtin.LinkdBase.ModuleLinkdBase)_modules_[0];
            Zeze_Builtin_LinkdBase.Initialize(this);
            if (modules.put(Zeze_Builtin_LinkdBase.getFullName(), Zeze_Builtin_LinkdBase) != null)
                throw new IllegalStateException("duplicate module name: Zeze_Builtin_LinkdBase");

            zezeboot_link = (zezeboot.link.ModuleLink)_modules_[1];
            zezeboot_link.Initialize(this);
            if (modules.put(zezeboot_link.getFullName(), zezeboot_link) != null)
                throw new IllegalStateException("duplicate module name: zezeboot_link");

            zezeboot_login = (zezeboot.login.ModuleLogin)_modules_[2];
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
            zezeboot_link = null;
            Zeze_Builtin_LinkdBase = null;
            modules.clear();
        } finally {
            unlock();
        }
    }

    public void destroyServices() {
        lock();
        try {
            LinkClient = null;
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
            Zeze_Builtin_LinkdBase.Start(this);
            zezeboot_link.Start(this);
            zezeboot_login.Start(this);
        } finally {
            unlock();
        }
    }

    @Override
    public void startLastModules() throws Exception {
        lock();
        try {
            Zeze_Builtin_LinkdBase.StartLast();
            zezeboot_link.StartLast();
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
            if (zezeboot_link != null)
                zezeboot_link.Stop(this);
            if (Zeze_Builtin_LinkdBase != null)
                Zeze_Builtin_LinkdBase.Stop(this);
        } finally {
            unlock();
        }
    }

    public void stopBeforeModules() throws Exception {
        lock();
        try {
            if (zezeboot_login != null)
                zezeboot_login.StopBefore();
            if (zezeboot_link != null)
                zezeboot_link.StopBefore();
            if (Zeze_Builtin_LinkdBase != null)
                Zeze_Builtin_LinkdBase.StopBefore();
        } finally {
            unlock();
        }
    }

    public void startService() throws Exception {
        lock();
        try {
            LinkClient.start();
        } finally {
            unlock();
        }
    }

    public void stopService() throws Exception {
        lock();
        try {
            if (LinkClient != null)
                LinkClient.stop();
        } finally {
            unlock();
        }
    }
    // ZEZE_FILE_CHUNK }}} GEN APP @formatter:on
}
