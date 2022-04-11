package Zeze.Arch;

import java.util.HashMap;
import Zeze.Beans.Provider.BLoad;
import Zeze.Beans.Provider.BModule;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.IntHashMap;

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

	public String LinkdServiceName;

	// 现在内部可以自动设置两个参数，但有点不够可靠，生产环境最好手动设置。
	public String DirectIp;
	public int DirectPort;

	public ProviderDistribute Distribute;

	public ProviderApp(Zeze.Application zeze,
					   ProviderImplement server,
					   ProviderService toLinkdService,
					   String providerModulePrefixNameOnServiceManager,
					   ProviderDirect direct,
					   ProviderDirectService toOtherProviderService,
					   String linkdNameOnServiceManager,
					   LoadConfig loadConfig
					   ) {
		this.Zeze = zeze;
		this.Zeze.Redirect = new RedirectBase(this);

		this.ProviderImplement = server;
		this.ProviderImplement.ProviderApp = this;
		this.ProviderService = toLinkdService;
		this.ProviderService.ProviderApp = this;
		this.ServerServiceNamePrefix = providerModulePrefixNameOnServiceManager;

		this.ProviderDirect = direct;
		this.ProviderDirect.ProviderApp = this;
		this.ProviderDirectService = toOtherProviderService;
		this.ProviderDirectService.ProviderApp = this;

		var kv = ProviderDirectService.GetOnePassiveAddress();
		this.DirectIp = kv.getKey();
		this.DirectPort = kv.getValue();

		this.LinkdServiceName = linkdNameOnServiceManager;

		this.ProviderImplement.RegisterProtocols(ProviderService);

		this.Zeze.getServiceManagerAgent().setOnChanged(
				(subscribeState) -> ProviderImplement.ApplyServiceInfos(subscribeState.getServiceInfos()));
		this.Zeze.getServiceManagerAgent().setOnPrepare(
				(subscribeState) -> ProviderImplement.ApplyPrepareServiceInfos(subscribeState));

		this.Zeze.getServiceManagerAgent().setOnSetServerLoad((serverLoad) -> {
			var ps = this.ProviderDirectService.ProviderSessions.get(serverLoad.getName());
			if (null != ps) {
				var bb = ByteBuffer.Wrap(serverLoad.Param);
				var load = new BLoad();
				load.Decode(bb);
				ps.Load = load;
			}
		});
		this.Distribute = new ProviderDistribute();
		this.Distribute.LoadConfig = loadConfig;
		this.Distribute.Zeze = this.Zeze;
		this.Distribute.ProviderService = ProviderService;

		this.ProviderDirect.RegisterProtocols(ProviderDirectService);
	}

	public final IntHashMap<BModule> StaticBinds = new IntHashMap<>();
	public final IntHashMap<BModule> DynamicModules = new IntHashMap<>();
	public final IntHashMap<BModule> Modules = new IntHashMap<>();

	public void initialize(ProviderModuleBinds binds, HashMap<String, Zeze.IModule> modules) {
		binds.BuildStaticBinds(modules, Zeze.getConfig().getServerId(), StaticBinds);
		binds.BuildDynamicBinds(modules, Zeze.getConfig().getServerId(), DynamicModules);
		Modules.putAll(StaticBinds);
		Modules.putAll(DynamicModules);
	}

	public void StartLast() throws Throwable {
		ProviderImplement.RegisterModulesAndSubscribeLinkd();
	}
}
