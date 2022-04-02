package Zezex;

import java.nio.file.Files;
import java.nio.file.Paths;
import Zeze.Net.AsyncSocket;
import Zeze.Util.PersistentAtomicLong;
import Zeze.Util.Str;
import com.fasterxml.jackson.databind.ObjectMapper;
import Zeze.Arch.*;

public final class App extends Zeze.AppBase {
	public static App Instance = new App();

	public static App getInstance() {
		return Instance;
	}

	public ProviderLinkd ProviderLinkd;

	private LinkdConfig LoadConfig() {
		try {
			byte[] bytes = Files.readAllBytes(Paths.get("linkd.json"));
			return new ObjectMapper().readValue(bytes, LinkdConfig.class);
		} catch (Exception e) {
			// e.printStackTrace();
		}
		return new LinkdConfig();
	}

	public void Start() throws Throwable {
		LoadConfig();
		CreateZeze();
		ProviderLinkd = new ProviderLinkd(Zeze, LoadConfig(), ProviderService, LinkdService);
		CreateService();
		CreateModules();
		StartModules(); // 启动模块，装载配置什么的。
		Zeze.Start(); // 启动数据库

		var ipp = ProviderService.GetOnePassiveAddress();
		String psip = ipp.getKey();
		int psport = ipp.getValue();

		var linkName = Str.format("{}:{}", psip, psport);
		AsyncSocket.setSessionIdGenFunc(PersistentAtomicLong.getOrAdd("Game.Linkd." + linkName)::next);

		StartService(); // 启动网络. after setSessionIdGenFunc

		ProviderLinkd.RegisterService("Game.Linkd", linkName, psip, psport, null);
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
    public Zezex.ProviderService ProviderService;

    public Zezex.Linkd.ModuleLinkd Zezex_Linkd;

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
        Zezex_Linkd = new Zezex.Linkd.ModuleLinkd(this);
        Zezex_Linkd.Initialize(this);
        Zezex_Linkd = (Zezex.Linkd.ModuleLinkd)ReplaceModuleInstance(Zezex_Linkd);
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
