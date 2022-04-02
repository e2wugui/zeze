package Zezex;

import java.nio.file.Files;
import java.nio.file.Paths;
import Zeze.Net.AsyncSocket;
import Zeze.Util.PersistentAtomicLong;
import Zeze.Util.Str;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class App extends Zeze.AppBase {
	public static App Instance = new App();

	public static App getInstance() {
		return Instance;
	}

	public static final String LinkdServiceName = "Game.Linkd";

	private LinkConfig LinkConfig;
	private Zeze.Services.ServiceManager.Agent ServiceManagerAgent;

	public LinkConfig getLinkConfig() {
		return LinkConfig;
	}

	public Zeze.Services.ServiceManager.Agent getServiceManagerAgent() {
		return ServiceManagerAgent;
	}

	private void LoadConfig() {
		try {
			byte[] bytes = Files.readAllBytes(Paths.get("linkd.json"));
			LinkConfig = new ObjectMapper().readValue(bytes, LinkConfig.class);
		} catch (Exception e) {
			// e.printStackTrace();
		}
		if (LinkConfig == null)
			LinkConfig = new LinkConfig();
	}

	public void Start() throws Throwable {
		LoadConfig();
		CreateZeze();
		CreateService();
		CreateModules();
		StartModules(); // 启动模块，装载配置什么的。
		Zeze.Start(); // 启动数据库

		var ipp = LinkdProviderService.GetOnePassiveAddress();
		String ProviderServicePassiveIp = ipp.getKey();
		int ProviderServicePassivePort = ipp.getValue();

		var linkName = Str.format("{}:{}", ProviderServicePassiveIp, ProviderServicePassivePort);
		AsyncSocket.setSessionIdGenFunc(PersistentAtomicLong.getOrAdd("Game.Linkd." + linkName)::next);

		StartService(); // 启动网络. after setSessionIdGenFunc

		ServiceManagerAgent = new Zeze.Services.ServiceManager.Agent(Zeze);
		getServiceManagerAgent().RegisterService(LinkdServiceName,
				linkName, ProviderServicePassiveIp, ProviderServicePassivePort, null);
	}

	public void Stop() throws Throwable {
		StopService(); // 关闭网络
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
    public LinkdProviderService LinkdProviderService;

    public Zezex.Linkd.ModuleLinkd Zezex_Linkd;
    public Zezex.Provider.ModuleProvider Zezex_Provider;

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
        LinkdProviderService = new LinkdProviderService(Zeze);
    }
    public synchronized void CreateModules() throws Throwable {
        Zezex_Linkd = new Zezex.Linkd.ModuleLinkd(this);
        Zezex_Linkd.Initialize(this);
        Zezex_Linkd = (Zezex.Linkd.ModuleLinkd)ReplaceModuleInstance(Zezex_Linkd);
        if (Modules.put(Zezex_Linkd.getFullName(), Zezex_Linkd) != null)
            throw new RuntimeException("duplicate module name: Zezex_Linkd");

        Zezex_Provider = new Zezex.Provider.ModuleProvider(this);
        Zezex_Provider.Initialize(this);
        Zezex_Provider = (Zezex.Provider.ModuleProvider)ReplaceModuleInstance(Zezex_Provider);
        if (Modules.put(Zezex_Provider.getFullName(), Zezex_Provider) != null)
            throw new RuntimeException("duplicate module name: Zezex_Provider");

        Zeze.setSchemas(new Zezex.Schemas());
    }

    public synchronized void DestroyModules() {
        Zezex_Provider = null;
        Zezex_Linkd = null;
        Modules.clear();
    }

    public synchronized void DestroyServices() {
        LinkdService = null;
        LinkdProviderService = null;
    }

    public synchronized void DestroyZeze() {
        Zeze = null;
    }

    public synchronized void StartModules() throws Throwable {
        Zezex_Linkd.Start(this);
        Zezex_Provider.Start(this);
    }

    public synchronized void StopModules() throws Throwable {
        if (Zezex_Provider != null)
            Zezex_Provider.Stop(this);
        if (Zezex_Linkd != null)
            Zezex_Linkd.Stop(this);
    }

    public synchronized void StartService() throws Throwable {
        LinkdService.Start();
        LinkdProviderService.Start();
    }

    public synchronized void StopService() throws Throwable {
        if (LinkdService != null)
            LinkdService.Stop();
        if (LinkdProviderService != null)
            LinkdProviderService.Stop();
    }
    // ZEZE_FILE_CHUNK }}} GEN APP @formatter:on
}
