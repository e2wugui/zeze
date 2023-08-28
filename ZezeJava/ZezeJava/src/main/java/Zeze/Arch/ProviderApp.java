package Zeze.Arch;

import java.util.HashMap;
import java.util.Map;
import Zeze.Application;
import Zeze.Builtin.Provider.BLoad;
import Zeze.Builtin.Provider.BModule;
import Zeze.IModule;
import Zeze.Net.Service;
import Zeze.Services.ServiceManager.BSubscribeInfo;
import Zeze.Util.IntHashMap;
import org.jetbrains.annotations.NotNull;

/**
 * 记录实现一个Provider需要的对象，
 * 设置相关对象之间的引用，
 * 初始化。
 */
public class ProviderApp {
	public final @NotNull Application zeze;

	public final @NotNull ProviderImplement providerImplement;
	public final @NotNull ProviderService providerService;
	public final @NotNull String serverServiceNamePrefix;

	public final @NotNull ProviderDirect providerDirect;
	public final @NotNull ProviderDirectService providerDirectService;

	public final @NotNull String linkdServiceName;

	// 现在内部可以自动设置两个参数，但有点不够可靠，生产环境最好手动设置。
	public final @NotNull String directIp;
	public int directPort;

	public final @NotNull ProviderDistribute distribute;

	public final IntHashMap<BModule.Data> staticBinds = new IntHashMap<>();
	public final IntHashMap<BModule.Data> dynamicModules = new IntHashMap<>();
	public final IntHashMap<BModule.Data> modules = new IntHashMap<>();
	public final HashMap<String, IModule> builtinModules = new HashMap<>();

	public ProviderApp(@NotNull Application zeze,
					   @NotNull ProviderImplement server,
					   @NotNull ProviderService toLinkdService,
					   @NotNull String providerModulePrefixNameOnServiceManager,
					   @NotNull ProviderDirect direct,
					   @NotNull ProviderDirectService toOtherProviderService,
					   @NotNull String linkdNameOnServiceManager,
					   @NotNull LoadConfig loadConfig) {
		this.zeze = zeze;
		this.zeze.setProviderApp(this);
		this.zeze.redirect = new RedirectBase(this);

		this.providerImplement = server;
		this.providerImplement.providerApp = this;
		this.providerService = toLinkdService;
		this.providerService.providerApp = this;
		this.serverServiceNamePrefix = providerModulePrefixNameOnServiceManager;

		this.providerDirect = direct;
		this.providerDirect.providerApp = this;
		this.providerDirectService = toOtherProviderService;
		this.providerDirectService.providerApp = this;

		var kv = providerDirectService.getOnePassiveAddress();
		this.directIp = kv.getKey();
		this.directPort = kv.getValue();

		this.linkdServiceName = linkdNameOnServiceManager;

		this.providerImplement.RegisterProtocols(providerService);

		this.zeze.getServiceManager().setOnSetServerLoad((serverLoad) -> {
			var ps = providerDirectService.providerByLoadName.get(serverLoad.getName());
			if (ps != null) {
				var load = new BLoad();
				load.decode(serverLoad.param.Wrap());
				ps.load = load;
			}
		});

		this.distribute = new ProviderDistribute(zeze, loadConfig, toOtherProviderService);

		this.zeze.getServiceManager().setOnChanged((ss) -> {
			providerImplement.applyOnChanged(ss);
			distribute.applyServers(ss);
		});
		this.zeze.getServiceManager().setOnPrepare(providerImplement::applyOnPrepare);
		this.zeze.getServiceManager().setOnUpdate((ss, si) -> {
			distribute.addServer(ss, si);
			providerDirectService.addServer(ss, si);
			providerImplement.addServer(ss, si);
		});
		this.zeze.getServiceManager().setOnRemoved((ss, si) -> {
			distribute.removeServer(ss, si);
			providerDirectService.removeServer(ss, si);
		});

		this.providerDirect.RegisterProtocols(providerDirectService);
	}

	/**
	 * 这是为了发布打包的时候，用来构建模块配置，所有的变量都不需要使用。
	 */
	public ProviderApp(Application zeze) throws Exception {
		this.zeze = zeze;
		this.zeze.setProviderApp(this);
		this.zeze.redirect = new RedirectBase(this);
		this.providerImplement = null;
		this.providerService = new ProviderService("fakeProviderService", this.zeze);
		this.serverServiceNamePrefix = null;
		this.providerDirect = null;
		this.providerDirectService = null;
		this.linkdServiceName = null;
		this.directIp = null;
		this.distribute = null;
	}

	public void buildProviderModuleBinds(@NotNull ProviderModuleBinds binds,
										 @NotNull Map<String, IModule> modules) {
		for (var builtin : builtinModules.values())
			modules.put(builtin.getFullName(), builtin);

		binds.buildStaticBinds(modules, zeze.getConfig().getServerId(), staticBinds);
		binds.buildDynamicBinds(modules, zeze.getConfig().getServerId(), dynamicModules);

		this.modules.putAll(staticBinds);
		this.modules.putAll(dynamicModules);

		// todo 难道上面的 buildStaticBinds & buildDynamicBinds 会没有处理全。导致这里需要补充一下？
		var defaultModuleConfig = new BModule.Data(
				BModule.ChoiceTypeDefault,
				BModule.ConfigTypeDefault,
				BSubscribeInfo.SubscribeTypeSimple);

		for (var module : modules.values()) {
			if (!this.modules.containsKey(module.getId())) { // 补充其它模块的信息
				var m = binds.getModules().get(module.getFullName());
				this.modules.put(module.getId(), m != null
						? new BModule.Data(m.getChoiceType(), m.getConfigType(), m.getSubscribeType())
						: defaultModuleConfig);
			}
		}
	}

	public void startLast(@NotNull ProviderModuleBinds binds, @NotNull Map<String, IModule> modules) {
		buildProviderModuleBinds(binds, modules);
		providerImplement.registerModulesAndSubscribeLinkd();
	}
}
