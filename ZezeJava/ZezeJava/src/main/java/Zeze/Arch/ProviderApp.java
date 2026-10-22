package Zeze.Arch;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Application;
import Zeze.Builtin.Provider.BLoad;
import Zeze.Builtin.Provider.BModule;
import Zeze.Game.ProviderWithOnline;
import Zeze.IModule;
import Zeze.Services.ServiceManager.BEditService;
import Zeze.Util.IntHashMap;
import org.jspecify.annotations.Nullable;

/**
 * 记录实现一个Provider需要的对象，
 * 设置相关对象之间的引用，
 * 初始化。
 */
public class ProviderApp extends ReentrantLock {
	public final Application zeze;

	public final @Nullable ProviderImplement providerImplement;
	public final ProviderService providerService;
	public final @Nullable String serverServiceNamePrefix;

	public final @Nullable ProviderDirect providerDirect;
	public final @Nullable ProviderDirectService providerDirectService;

	public final @Nullable String linkdServiceName;

	// 这两个参数可以自动设置，但不够可靠，生产环境最好手动设置。
	public final @Nullable String directIp;
	public int directPort;

	public final @Nullable ProviderDistribute distribute;

	// 三个map均为非线程安全IntHashMap：所有写点（startLast、ProviderService.addHotModule）
	// 与全部读点（ProviderService.OnHandshakeDone、ProviderDirectService.addServer/
	// setRelativeServiceReady、ProviderImplement.registerModulesAndSubscribeLinkd）统一在
	// providerApp锁（this）下进行，防止热更线程写与linkd重连握手线程读并发时resize撕裂。
	public final IntHashMap<BModule.Data> staticBinds = new IntHashMap<>();
	public final IntHashMap<BModule.Data> dynamicModules = new IntHashMap<>();
	public final IntHashMap<BModule.Data> modules = new IntHashMap<>();
	public final HashMap<String, IModule> builtinModules = new HashMap<>();
	private volatile boolean startLast;
	private volatile boolean isOnlineReady = false;
	private volatile boolean isUserDisableChoice = true;

	public boolean isOnlineReady() {
		return isOnlineReady;
	}

	public boolean isUserDisableChoice() {
		return isUserDisableChoice;
	}

	public boolean isStartLast() {
		return startLast;
	}

	public ProviderApp(Application zeze,
					   ProviderImplement server,
					   ProviderService toLinkdService,
					   String providerModulePrefixNameOnServiceManager,
					   ProviderDirect direct,
					   ProviderDirectService toOtherProviderService,
					   String linkdNameOnServiceManager,
					   LoadConfig loadConfig) {
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

		//noinspection DataFlowIssue
		this.zeze.getServiceManager().setOnSetServerLoad((serverLoad) -> {
			var ps = providerDirectService.providerByLoadName.get(serverLoad.getName());
			if (ps != null) {
				var load = new BLoad();
				load.decode(serverLoad.param.Wrap());
				ps.load = load;
			}
		});

		this.distribute = new ProviderDistribute(zeze, loadConfig, toOtherProviderService,
				zeze.getConfig().getAppVersion());
		//noinspection DataFlowIssue
		this.zeze.getServiceManager().setOnChanged(this::applyOnChanged);
		this.providerDirect.RegisterProtocols(providerDirectService);
	}

	void applyOnChanged(BEditService edit) {
		var refresh = false;
		for (var r : edit.getRemove()) {
			if (r.getServiceName().equals(linkdServiceName))
				refresh |= providerService.applyRemove(r);
			else if (r.getServiceName().startsWith(Objects.requireNonNull(serverServiceNamePrefix))) {
				//noinspection DataFlowIssue
				providerDirectService.removeServer(r);
				//noinspection DataFlowIssue
				distribute.removeServer(r);
			}
		}
		for (var p : edit.getAdd()) {
			if (p.getServiceName().equals(linkdServiceName))
				refresh |= providerService.applyPut(p);
			else if (p.getServiceName().startsWith(Objects.requireNonNull(serverServiceNamePrefix))) {
				//noinspection DataFlowIssue
				providerDirectService.addServer(p);
				//noinspection DataFlowIssue
				distribute.addServer(p);
			}
		}
		if (refresh)
			providerService.refreshLinkConnectors();
	}

	/**
	 * 这是为了发布打包的时候，用来构建模块配置，所有的变量都不需要使用。
	 */
	public ProviderApp(Application zeze) {
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

	public void buildProviderModuleBinds(ProviderModuleBinds binds,
										 Map<String, IModule> modules) {
		for (var builtin : builtinModules.values())
			modules.put(builtin.getFullName(), builtin);

		binds.buildStaticBinds(modules, zeze.getConfig().getServerId(), staticBinds);
		binds.buildDynamicBinds(modules, zeze.getConfig().getServerId(), dynamicModules);

		this.modules.putAll(staticBinds);
		this.modules.putAll(dynamicModules);

		// 上面的 buildStaticBinds & buildDynamicBinds 不包含所有模块，下面需要补充。
		// 用于ProviderDirect。
		var defaultModuleConfig = new BModule.Data(
				BModule.ChoiceTypeDefault,
				false);

		for (var module : modules.values()) {
			if (!this.modules.containsKey(module.getId())) { // 补充其它模块的信息
				var m = binds.getModules().get(module.getFullName());
				this.modules.put(module.getId(), m != null
						? new BModule.Data(m.getChoiceType(), m.isDynamic())
						: defaultModuleConfig);
			}
		}
	}

	void setUserDisableChoice(boolean userDisableChoice) {
		isUserDisableChoice = userDisableChoice;
	}

	public void startLast(ProviderModuleBinds binds, Map<String, IModule> modules) throws Exception {
		lock();
		try {
			// 本方法恰好执行一次。ProviderApp没有stop、实例不可重启（对齐Application不可复用契约），
			// 重入属编程错误，快速失败——否则二次调用会把模块绑定/订阅/timer/safeBatch全部重跑一遍。
			if (startLast)
				throw new IllegalStateException("ProviderApp.startLast() already called.");
			buildProviderModuleBinds(binds, modules);
			//noinspection DataFlowIssue
			providerImplement.registerModulesAndSubscribeLinkd();
			startLast = true;
			//noinspection DataFlowIssue
			zeze.getTimer().start();
			//noinspection DataFlowIssue
			zeze.getSafeBatch().start();
			//noinspection DataFlowIssue
			zeze.getAppBase().startLastModules();

			if (providerImplement instanceof ProviderWithOnline)
				//noinspection DataFlowIssue
				(((ProviderWithOnline)providerImplement).getOnline()).startAfter();
			// 这个应该在Online.Start里面设置更合理。
			// 但是Online有多个版本，而且跨包设置需要方法，就这里直接设置了。
			// 是启动流程的一部分。
			isOnlineReady = true;

			// 开启link，默认开启。
			setUserDisableChoice(false);
			providerService.trySetLinkChoice();

			startLoginQueueAgent();
		} finally {
			unlock();
		}
	}

	// LoginQueueAgent启动：创建收口在LoadBase.ensureLoginQueueAgent——
	// load没有且配置启用才建，预置/已建复用；不覆盖（覆盖即泄漏+同身份重复注册）。
	private void startLoginQueueAgent() throws Exception {
		//noinspection DataFlowIssue
		var load = providerImplement.getLoad();
		if (load == null)
			return;
		var agent = load.ensureLoginQueueAgent();
		if (agent != null)
			agent.start();
	}
}
