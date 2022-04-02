package Zeze.Arch;

import java.util.HashMap;
import Zeze.Beans.Provider.BModule;

/**
 * 记录实现一个Provider需要的对象，
 * 设置相关对象之间的引用，
 * 初始化。
 */
public class ProviderApp {
	public Zeze.Application Zeze;

	public ProviderImplement ProviderImplement;
	public ProviderService ProviderService;
	public String ServerServiceNamePrefix;

	public ProviderDirect ProviderDirect;
	public ProviderDirectService ProviderDirectService;
	public String ProviderDirectPassiveIp;
	public int ProviderDirectPassivePort;

	public String LinkdServiceName;

	public ProviderApp(Zeze.Application zeze,
					   ProviderImplement server,
					   ProviderService toLinkdService,
					   String providerModulePrefixNameOnServiceManager,
					   ProviderDirect direct,
					   ProviderDirectService toOtherProviderService,
					   String ProviderDirectPassiveIp,
					   int ProviderDirectPassivePort,
					   String linkdNameOnServiceManager
					   ) {
		this.Zeze = zeze;

		this.ProviderImplement = server;
		this.ProviderImplement.ProviderApp = this;
		this.ProviderService = toLinkdService;
		this.ProviderService.ProviderApp = this;
		this.ServerServiceNamePrefix = providerModulePrefixNameOnServiceManager;

		this.ProviderDirect = direct;
		this.ProviderDirect.ProviderApp = this;
		this.ProviderDirectService = toOtherProviderService;
		this.ProviderDirectService.ProviderApp = this;

		this.ProviderDirectPassiveIp = ProviderDirectPassiveIp;
		this.ProviderDirectPassivePort = ProviderDirectPassivePort;
		this.LinkdServiceName = linkdNameOnServiceManager;

		this.ProviderImplement.RegisterProtocols(ProviderService);
		this.Zeze.getServiceManagerAgent().setOnChanged(
				(subscribeState) -> ProviderImplement.ApplyServiceInfos(subscribeState.getServiceInfos()));

		this.ProviderDirect.RegisterProtocols(ProviderDirectService);
	}

	public final HashMap<Integer, BModule> StaticBinds = new HashMap<>();
	public final HashMap<Integer, BModule> DynamicModules = new HashMap<>();
	public java.util.HashMap<Integer, BModule> Modules;

	public void initialize(ProviderModuleBinds binds, java.util.HashMap<String, Zeze.IModule> modules) {
		binds.BuildStaticBinds(modules, Zeze.getConfig().getServerId(), StaticBinds);
		binds.BuildDynamicBinds(modules, Zeze.getConfig().getServerId(), DynamicModules);
		Modules.putAll(StaticBinds);
		Modules.putAll(DynamicModules);
	}

	public void StartLast() throws Throwable {
		ProviderImplement.RegisterModulesAndSubscribeLinkd();
	}
}
