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
import javax.faces.component.html.HtmlPanelGrid;
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

	public KeyStore FakeCa;

	private void createFakeCa() throws IOException, GeneralSecurityException {
		var file = "ZegeFakeCa.pkcs12";
		var passwd = "123";
		if (Files.exists(Path.of(file))) {
			FakeCa = Cert.loadKeyStore(new FileInputStream(file), passwd);
		} else {
			var rsa = Cert.generateRsaKeyPair();
			var cert = Cert.generate("ZegeFakeCa", rsa.getPublic(), "ZegeFakeCa", rsa.getPrivate(), 100000);
			FakeCa = KeyStore.getInstance("pkcs12");
			FakeCa.load(null, null);
			FakeCa.setKeyEntry("ZegeFakeCa", rsa.getPrivate(), passwd.toCharArray(), new Certificate[]{cert});
			FakeCa.store(new FileOutputStream(file), passwd.toCharArray());
		}
	}

	public ZegeConfig ZegeConfig = new ZegeConfig();

	public void Start(String conf) throws Exception {
		var config = new Config().addCustomize(ZegeConfig);
		config.loadAndParse(conf);

		createZeze(config);
		createService();

		HttpServer = new HttpServer(Zeze, null, 600);

		var dbWeb = new DbWeb();
		dbWeb.Initialize(this);
		dbWeb.RegisterHttpServlet(HttpServer);

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
		HttpServer.start(new Netty(1), 80); //TODO: 从配置里读线程数和端口

		createFakeCa();

		PersistentAtomicLong socketSessionIdGen = PersistentAtomicLong.getOrAdd("Zege.Server." + Zeze.getConfig().getServerId());
		AsyncSocket.setSessionIdGenFunc(socketSessionIdGen::next);
		startService(); // 启动网络
		ProviderApp.startLast(ProviderModuleBinds.load(), modules);
	}

	public void Stop() throws Exception {
		if (Provider != null)
			Provider.stop();
		HttpServer.close();
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
    public final java.util.HashMap<String, Zeze.IModule> modules = new java.util.HashMap<>();

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

    public synchronized void createZeze(Zeze.Config config) throws Exception {
        if (Zeze != null)
            throw new RuntimeException("Zeze Has Created!");

        Zeze = new Zeze.Application("server", config);
    }

    public synchronized void createService() {
        Server = new Zege.Server(Zeze);
        ServerDirect = new Zege.ServerDirect(Zeze);
    }

    public synchronized void createModules() {
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
            throw new RuntimeException("duplicate module name: Zege_User");

        Zege_Friend = (Zege.Friend.ModuleFriend)_modules_[1];
        Zege_Friend.Initialize(this);
        if (modules.put(Zege_Friend.getFullName(), Zege_Friend) != null)
            throw new RuntimeException("duplicate module name: Zege_Friend");

        Zege_Message = (Zege.Message.ModuleMessage)_modules_[2];
        Zege_Message.Initialize(this);
        if (modules.put(Zege_Message.getFullName(), Zege_Message) != null)
            throw new RuntimeException("duplicate module name: Zege_Message");

        Zege_Notify = (Zege.Notify.ModuleNotify)_modules_[3];
        Zege_Notify.Initialize(this);
        if (modules.put(Zege_Notify.getFullName(), Zege_Notify) != null)
            throw new RuntimeException("duplicate module name: Zege_Notify");

        Zeze.setSchemas(new Zege.Schemas());
    }

    public synchronized void destroyModules() {
        Zege_Notify = null;
        Zege_Message = null;
        Zege_Friend = null;
        Zege_User = null;
        modules.clear();
    }

    public synchronized void destroyServices() {
        Server = null;
        ServerDirect = null;
    }

    public synchronized void destroyZeze() {
        Zeze = null;
    }

    public synchronized void startModules() throws Exception {
        Zege_User.Start(this);
        Zege_Friend.Start(this);
        Zege_Message.Start(this);
        Zege_Notify.Start(this);
    }

    public synchronized void stopModules() throws Exception {
        if (Zege_Notify != null)
            Zege_Notify.Stop(this);
        if (Zege_Message != null)
            Zege_Message.Stop(this);
        if (Zege_Friend != null)
            Zege_Friend.Stop(this);
        if (Zege_User != null)
            Zege_User.Stop(this);
    }

    public synchronized void startService() throws Exception {
        Server.start();
        ServerDirect.start();
    }

    public synchronized void stopService() throws Exception {
        if (Server != null)
            Server.stop();
        if (ServerDirect != null)
            ServerDirect.stop();
    }
    // ZEZE_FILE_CHUNK }}} GEN APP @formatter:on
}
