package Zezex;

import java.util.*;

// auto-generated



public final class App {
	private static App Instance = new App();
	public static App getInstance() {
		return Instance;
	}

	private Zeze.Application Zeze;
	public Zeze.Application getZeze() {
		return Zeze;
	}
	public void setZeze(Zeze.Application value) {
		Zeze = value;
	}

	private HashMap<String, Zeze.IModule> Modules = new HashMap < String, getZeze().IModule> ();
	public HashMap<String, Zeze.IModule> getModules() {
		return Modules;
	}

	private Zezex.Linkd.ModuleLinkd Zezex_Linkd;
	public Zezex.Linkd.ModuleLinkd getZezexLinkd() {
		return Zezex_Linkd;
	}
	public void setZezexLinkd(Zezex.Linkd.ModuleLinkd value) {
		Zezex_Linkd = value;
	}

	private Zezex.Provider.ModuleProvider Zezex_Provider;
	public Zezex.Provider.ModuleProvider getZezexProvider() {
		return Zezex_Provider;
	}
	public void setZezexProvider(Zezex.Provider.ModuleProvider value) {
		Zezex_Provider = value;
	}

	private Zezex.LinkdService LinkdService;
	public Zezex.LinkdService getLinkdService() {
		return LinkdService;
	}
	public void setLinkdService(Zezex.LinkdService value) {
		LinkdService = value;
	}

	private Zezex.ProviderService ProviderService;
	public Zezex.ProviderService getProviderService() {
		return ProviderService;
	}
	public void setProviderService(Zezex.ProviderService value) {
		ProviderService = value;
	}


	public void Create() {
		Create(null);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public void Create(Zeze.Config config = null)
	public void Create(Zeze.Config config) {
		synchronized (this) {
			if (null != getZeze()) {
				return;
			}

			setZeze(new Zeze.Application("Zezex", config));

			setLinkdService(new Zezex.LinkdService(getZeze()));
			setProviderService(new Zezex.ProviderService(getZeze()));

			setZezexLinkd(new Zezex.Linkd.ModuleLinkd(this));
			setZezexLinkd((Zezex.Linkd.ModuleLinkd)ReplaceModuleInstance(getZezexLinkd()));
			getModules().put(getZezexLinkd().getName(), getZezexLinkd());
			setZezexProvider(new Zezex.Provider.ModuleProvider(this));
			setZezexProvider((Zezex.Provider.ModuleProvider)ReplaceModuleInstance(getZezexProvider()));
			getModules().put(getZezexProvider().getName(), getZezexProvider());

			getZeze().Schemas = new Zezex.Schemas();
		}
	}

	public void Destroy() {
		synchronized (this) {
			setZezexLinkd(null);
			setZezexProvider(null);
			getModules().clear();
			setLinkdService(null);
			setProviderService(null);
			setZeze(null);
		}
	}

	public void StartModules() {
		synchronized (this) {
			getZezexLinkd().Start(this);
			getZezexProvider().Start(this);

		}
	}

	public void StopModules() {
		synchronized (this) {
			getZezexLinkd().Stop(this);
			getZezexProvider().Stop(this);
		}
	}

	public void StartService() {
		synchronized (this) {
			getLinkdService().Start();
			getProviderService().Start();
		}
	}

	public void StopService() {
		synchronized (this) {
			getLinkdService().Stop();
			getProviderService().Stop();
		}
	}


	private static final NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

	public Zeze.IModule ReplaceModuleInstance(Zeze.IModule module) {
		return module;
	}


	private Config Config;
	public Config getConfig() {
		return Config;
	}
	private void setConfig(Config value) {
		Config = value;
	}
	private Zeze.Services.ServiceManager.Agent ServiceManagerAgent;
	public Zeze.Services.ServiceManager.Agent getServiceManagerAgent() {
		return ServiceManagerAgent;
	}
	private void setServiceManagerAgent(Zeze.Services.ServiceManager.Agent value) {
		ServiceManagerAgent = value;
	}
	public static final String ServerServiceNamePrefix = "Game.Server.Module#";
	public static final String LinkdServiceName = "Game.Linkd";

	private void LoadConfig() {
		try {
			String json = Encoding.UTF8.GetString(System.IO.File.ReadAllBytes("linkd.json"));
			setConfig(JsonSerializer.<Config>Deserialize(json));
		}
		catch (RuntimeException e) {
			//MessageBox.Show(ex.ToString());
		}
		if (null == getConfig()) {
			setConfig(new Config());
		}
	}

	private String ProviderServicePassiveIp;
	public String getProviderServicePassiveIp() {
		return ProviderServicePassiveIp;
	}
	private void setProviderServicePassiveIp(String value) {
		ProviderServicePassiveIp = value;
	}
	private int ProviderServicePasivePort;
	public int getProviderServicePasivePort() {
		return ProviderServicePasivePort;
	}
	private void setProviderServicePasivePort(int value) {
		ProviderServicePasivePort = value;
	}

	public void Start() {
		LoadConfig();
		Create();
		StartModules(); // 启动模块，装载配置什么的。
		getZeze().Start(); // 启动数据库
		StartService(); // 启动网络

//C# TO JAVA CONVERTER TODO TASK: Java has no equivalent to C# deconstruction declarations:
		var(ip, port) = ProviderService.GetOnePassiveAddress();
		setProviderServicePassiveIp(ip);
		setProviderServicePasivePort(port);

		setServiceManagerAgent(new Zeze.Services.ServiceManager.Agent(getZeze().Config));
		getServiceManagerAgent().RegisterService(LinkdServiceName, String.format("%1$s:%2$s", getProviderServicePassiveIp(), getProviderServicePasivePort()), getProviderServicePassiveIp(), getProviderServicePasivePort(), null);
	}

	public void Stop() {
		StopService(); // 关闭网络
		getZeze().Stop(); // 关闭数据库
		StopModules(); // 关闭模块,，卸载配置什么的。
		Destroy();
	}
}