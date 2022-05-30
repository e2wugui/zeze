package Zezex;

import java.nio.file.Files;
import java.nio.file.Paths;
import Zeze.Net.AsyncSocket;
import Zeze.Util.PersistentAtomicLong;
import com.fasterxml.jackson.databind.ObjectMapper;
import Zeze.Arch.*;
import Zeze.Config;

public final class App extends Zeze.AppBase {
	public static final App Instance = new App();

	public static App getInstance() {
		return Instance;
	}

	public LinkdProvider LinkdProvider;

	private LoadConfig LoadConfig() {
		try {
			byte[] bytes = Files.readAllBytes(Paths.get("linkd.json"));
			return new ObjectMapper().readValue(bytes, LoadConfig.class);
		} catch (Exception e) {
			// e.printStackTrace();
		}
		return new LoadConfig();
	}

	public LinkdApp LinkdApp;

	public void Start(String[] args) throws Throwable {
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
		Start(linkPort, providerPort);
	}

	public void Start(int linkPort, int providerPort) throws Throwable {
		// Create
		var config = Config.Load("linkd.xml");
		if (linkPort != -1) {
			config.getServiceConfMap().get("LinkdService").ForEachAcceptor((a) -> a.setPort(linkPort));
		}
		if (linkPort != -1) {
			config.getServiceConfMap().get("ProviderService").ForEachAcceptor((a) -> a.setPort(providerPort));
		}
		CreateZeze(config);
		CreateService();
		LinkdProvider = new LinkdProvider();
		LinkdApp = new LinkdApp("Game.Linkd", Zeze, LinkdProvider, ProviderService, LinkdService, LoadConfig());
		CreateModules();
		// Start
		Zeze.Start(); // 启动数据库
		StartModules(); // 启动模块，装载配置什么的。
		AsyncSocket.setSessionIdGenFunc(PersistentAtomicLong.getOrAdd(LinkdApp.GetName())::next);
		StartService(); // 启动网络. after setSessionIdGenFunc
		LinkdApp.RegisterService(null);
	}

	public void Stop() throws Throwable {
		StopService(); // 关闭网络
		if (Zeze != null)
			Zeze.Stop(); // 关闭数据库
		StopModules(); // 关闭模块，卸载配置什么的。
		DestroyModules();
		DestroyServices();
		DestroyZeze();
	}

	// ZEZE_FILE_CHUNK {{{ GEN APP @formatter:off
    public Zeze.Application Zeze;
    public final java.util.HashMap<String, Zeze.IModule> Modules = new java.util.HashMap<>();

    public Zezex.LinkdService LinkdService;
    public Zezex.ProviderService ProviderService;

    public Zezex.Linkd.ModuleLinkd Zezex_Linkd;

    @Override
    public Zeze.Application getZeze() {
        return Zeze;
    }

    public void CreateZeze() throws Throwable {
        CreateZeze(null);
    }

    public synchronized void CreateZeze(Zeze.Config config) throws Throwable {
        if (Zeze != null)
            throw new RuntimeException("Zeze Has Created!");

        Zeze = new Zeze.Application("Zezex", config);
    }

    public synchronized void CreateService() throws Throwable {

        LinkdService = new Zezex.LinkdService(Zeze);
        ProviderService = new Zezex.ProviderService(Zeze);
    }
    public synchronized void CreateModules() {
        Zezex_Linkd = ReplaceModuleInstance(new Zezex.Linkd.ModuleLinkd(this));
        Zezex_Linkd.Initialize(this);
        if (Modules.put(Zezex_Linkd.getFullName(), Zezex_Linkd) != null)
            throw new RuntimeException("duplicate module name: Zezex_Linkd");

        Zeze.setSchemas(new Zezex.Schemas());
    }

    public synchronized void DestroyModules() {
        Zezex_Linkd = null;
        Modules.clear();
    }

    public synchronized void DestroyServices() {
        LinkdService = null;
        ProviderService = null;
    }

    public synchronized void DestroyZeze() {
        Zeze = null;
    }

    public synchronized void StartModules() throws Throwable {
        Zezex_Linkd.Start(this);
    }

    public synchronized void StopModules() throws Throwable {
        if (Zezex_Linkd != null)
            Zezex_Linkd.Stop(this);
    }

    public synchronized void StartService() throws Throwable {
        LinkdService.Start();
        ProviderService.Start();
    }

    public synchronized void StopService() throws Throwable {
        if (LinkdService != null)
            LinkdService.Stop();
        if (ProviderService != null)
            ProviderService.Stop();
    }
    // ZEZE_FILE_CHUNK }}} GEN APP @formatter:on
}
