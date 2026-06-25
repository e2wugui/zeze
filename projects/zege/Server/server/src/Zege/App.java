package Zege;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import Zeze.Arch.LoadConfig;
import Zeze.Arch.ProviderApp;
import Zeze.Arch.ProviderDirect;
import Zeze.Arch.ProviderModuleBinds;
import Zeze.Arch.ProviderWithOnline;
import Zeze.Collections.DepartmentTree;
import Zeze.Collections.LinkedMap;
import Zeze.Component.DbWeb;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Util.Cert;
import Zeze.Util.JsonReader;
import Zeze.Util.PersistentAtomicLong;

public class App extends Zeze.AppBase {
	public static final App Instance = new App();

	public static App getInstance() {
		return Instance;
	}

	public ProviderApp ProviderApp;
	public ProviderDirect ProviderDirect;
	public ProviderWithOnline Provider;
	public LinkedMap.Module LinkedMaps;
	public DepartmentTree.Module DepartmentTrees;
	public Zeze.Netty.HttpServer HttpServer;
    private Zeze.Component.DbWeb DbWeb;

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

	public KeyStore CaKeyStore;

	private void createFakeCa() throws IOException, GeneralSecurityException {
		var file = "ZegeCa.pkcs12";
		var passwd = "123";
		if (Files.exists(Path.of(file))) {
			CaKeyStore = Cert.loadKeyStore(new FileInputStream(file), passwd);
		} else {
			var rsa = Cert.generateRsaKeyPair();
			var cert = Zege.User.ModuleUser.generateRsaCert("ZegeCa", rsa.getPublic(), "ZegeCa", rsa.getPrivate(), 365 * 100);
			CaKeyStore = KeyStore.getInstance("pkcs12");
			CaKeyStore.load(null, null);
			CaKeyStore.setKeyEntry("ZegeCa", rsa.getPrivate(), passwd.toCharArray(), new Certificate[]{cert});
			CaKeyStore.store(new FileOutputStream(file), passwd.toCharArray());
		}
	}

	public ZegeConfig ZegeConfig = new ZegeConfig();

	public void Start(String conf) throws Exception {
		var config = Config.load("server.xml");
		config.parseCustomize(ZegeConfig);

		createZeze(config);
		createService();

		HttpServer = new HttpServer(Zeze);
        DbWeb = new DbWeb();
		DbWeb.Initialize(this);
		DbWeb.RegisterHttpServlet(HttpServer);

		Provider = new ProviderWithOnline();
		ProviderDirect = new ProviderDirect();
		ProviderApp = new ProviderApp(Zeze, Provider, Server,
				"Zege.Server.Module#",
				ProviderDirect, ServerDirect, "Zege.Linkd", LoadConfig());
		Provider.create(this);
		LinkedMaps = new LinkedMap.Module(Zeze);
		DepartmentTrees = new DepartmentTree.Module(Zeze, LinkedMaps);

		createModules();
		Zeze.start(); // 启动数据库
		startModules(); // 启动模块，装载配置什么的。
		Provider.start();
		HttpServer.start(new Netty(1), 11001); //TODO: 从配置里读线程数和端口

		createFakeCa();

		PersistentAtomicLong socketSessionIdGen = PersistentAtomicLong.getOrAdd("Zege.Server." + Zeze.getConfig().getServerId());
		AsyncSocket.setSessionIdGenFunc(socketSessionIdGen::next);
		startService(); // 启动网络
		ProviderApp.startLast(ProviderModuleBinds.load(), modules);
	}

	public void Stop() throws Exception {
		if (Provider != null)
			Provider.stop();
		//HttpServer.close();
		stopService(); // 关闭网络
		stopModules(); // 关闭模块，卸载配置什么的。
		if (Zeze != null)
			Zeze.stop(); // 关闭数据库
		destroyModules();
		destroyServices();
		destroyZeze();
	}

	// ZEZE_FILE_CHUNK {{{ GEN APP @formatter:off
    public Zeze.Application Zeze;

    public Zege.Server Server;
    public Zege.ServerDirect ServerDirect;

    public Zege.User.ModuleUser Zege_User;
    public Zege.Friend.ModuleFriend Zege_Friend;
    public Zege.Message.ModuleMessage Zege_Message;
    public Zege.Notify.ModuleNotify Zege_Notify;

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
            Server = new Zege.Server(Zeze);
            ServerDirect = new Zege.ServerDirect(Zeze);
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
                Zege.User.ModuleUser.class,
                Zege.Friend.ModuleFriend.class,
                Zege.Message.ModuleMessage.class,
                Zege.Notify.ModuleNotify.class,
            });
            if (_modules_ == null)
                return;

            Zege_User = (Zege.User.ModuleUser)_modules_[0];
            Zege_User.Initialize(this);
            if (modules.put(Zege_User.getFullName(), Zege_User) != null)
                throw new IllegalStateException("duplicate module name: Zege_User");

            Zege_Friend = (Zege.Friend.ModuleFriend)_modules_[1];
            Zege_Friend.Initialize(this);
            if (modules.put(Zege_Friend.getFullName(), Zege_Friend) != null)
                throw new IllegalStateException("duplicate module name: Zege_Friend");

            Zege_Message = (Zege.Message.ModuleMessage)_modules_[2];
            Zege_Message.Initialize(this);
            if (modules.put(Zege_Message.getFullName(), Zege_Message) != null)
                throw new IllegalStateException("duplicate module name: Zege_Message");

            Zege_Notify = (Zege.Notify.ModuleNotify)_modules_[3];
            Zege_Notify.Initialize(this);
            if (modules.put(Zege_Notify.getFullName(), Zege_Notify) != null)
                throw new IllegalStateException("duplicate module name: Zege_Notify");

            Zeze.setSchemas(new Zege.Schemas());
        } finally {
            unlock();
        }
    }

    public void destroyModules() throws Exception {
        lock();
        try {
            Zege_Notify = null;
            Zege_Message = null;
            Zege_Friend = null;
            Zege_User = null;
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
            Zege_User.Start(this);
            Zege_Friend.Start(this);
            Zege_Message.Start(this);
            Zege_Notify.Start(this);
        } finally {
            unlock();
        }
    }

    @Override
    public void startLastModules() throws Exception {
        lock();
        try {
            Zege_User.StartLast();
            Zege_Friend.StartLast();
            Zege_Message.StartLast();
            Zege_Notify.StartLast();
        } finally {
            unlock();
        }
    }

    public void stopModules() throws Exception {
        lock();
        try {
            if (Zege_Notify != null)
                Zege_Notify.Stop(this);
            if (Zege_Message != null)
                Zege_Message.Stop(this);
            if (Zege_Friend != null)
                Zege_Friend.Stop(this);
            if (Zege_User != null)
                Zege_User.Stop(this);
        } finally {
            unlock();
        }
    }

    public void stopBeforeModules() throws Exception {
        lock();
        try {
            if (Zege_Notify != null)
                Zege_Notify.StopBefore();
            if (Zege_Message != null)
                Zege_Message.StopBefore();
            if (Zege_Friend != null)
                Zege_Friend.StopBefore();
            if (Zege_User != null)
                Zege_User.StopBefore();
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
