package Zezex;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import jason.JasonReader;

//ZEZE_FILE_CHUNK {{{ IMPORT GEN

//ZEZE_FILE_CHUNK }}} IMPORT GEN

public final class App extends Zeze.AppBase {
	public static App Instance = new App();
	public static App getInstance() {
		return Instance;
	}

	public void Start() {
		LoadConfig();
		Create();
		StartModules(); // 启动模块，装载配置什么的。
		Zeze.Start(); // 启动数据库
		StartService(); // 启动网络

		var ipp = ProviderService.GetOnePassiveAddress();
		setProviderServicePassiveIp(ipp.getKey());
		setProviderServicePasivePort(ipp.getValue());

		setServiceManagerAgent(new Zeze.Services.ServiceManager.Agent(Zeze.getConfig()));
		getServiceManagerAgent().RegisterService(LinkdServiceName, String.format("%1$s:%2$s", getProviderServicePassiveIp(), getProviderServicePasivePort()), getProviderServicePassiveIp(), getProviderServicePasivePort(), null);
	}

	public void Stop() {
		StopService(); // 关闭网络
		Zeze.Stop(); // 关闭数据库
		StopModules(); // 关闭模块,，卸载配置什么的。
		Destroy();
	}

	private Zezex.Config Config;
	public Zezex.Config getConfig() {
		return Config;
	}
	private void setConfig(Zezex.Config value) {
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
            byte [] bytes = Files.readAllBytes(Paths.get("linkd.json"));
            setConfig(new JasonReader().buf(bytes).parse(Zezex.Config.class));
		}
		catch (Exception e) {
			//MessageBox.Show(ex.ToString());
		}
		if (null == getConfig()) {
			setConfig(new Zezex.Config());
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

    // ZEZE_FILE_CHUNK {{{ GEN APP
    public Zeze.Application Zeze;
    public HashMap<String, Zeze.IModule> Modules = new HashMap<>();

    public Zezex.Linkd.ModuleLinkd Zezex_Linkd;

    public Zezex.Provider.ModuleProvider Zezex_Provider;

    public Zezex.LinkdService LinkdService;

    public Zezex.ProviderService ProviderService;

    public void Create() {
        Create(null);
    }

    public void Create(Zeze.Config config) {
        synchronized (this) {
            if (null != Zeze)
                return;

            Zeze = new Zeze.Application("Zezex", config);

            LinkdService = new Zezex.LinkdService(Zeze);
            ProviderService = new Zezex.ProviderService(Zeze);

            Zezex_Linkd = new Zezex.Linkd.ModuleLinkd(this);
            Zezex_Linkd = (Zezex.Linkd.ModuleLinkd)ReplaceModuleInstance(Zezex_Linkd);
            if (null != Modules.put(Zezex_Linkd.getName(), Zezex_Linkd)) {
                throw new RuntimeException("duplicate module name: Zezex_Linkd");
            }
            Zezex_Provider = new Zezex.Provider.ModuleProvider(this);
            Zezex_Provider = (Zezex.Provider.ModuleProvider)ReplaceModuleInstance(Zezex_Provider);
            if (null != Modules.put(Zezex_Provider.getName(), Zezex_Provider)) {
                throw new RuntimeException("duplicate module name: Zezex_Provider");
            }

            Zeze.setSchemas(new Zezex.Schemas());
        }
    }

    public void Destroy() {
        synchronized(this) {
            Zezex_Linkd = null;
            Zezex_Provider = null;
            Modules.clear();
            LinkdService = null;
            ProviderService = null;
            Zeze = null;
        }
    }

    public void StartModules() {
        synchronized(this) {
            Zezex_Linkd.Start(this);
            Zezex_Provider.Start(this);

        }
    }

    public void StopModules() {
        synchronized(this) {
            Zezex_Linkd.Stop(this);
            Zezex_Provider.Stop(this);
        }
    }

    public void StartService() {
        synchronized(this) {
            LinkdService.Start();
            ProviderService.Start();
        }
    }

    public void StopService() {
        synchronized(this) {
            LinkdService.Stop();
            ProviderService.Stop();
        }
    }
    // ZEZE_FILE_CHUNK }}} GEN APP
}
