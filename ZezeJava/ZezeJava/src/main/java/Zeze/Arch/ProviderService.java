package Zeze.Arch;

import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Application;
import Zeze.Builtin.Provider.AnnounceProviderInfo;
import Zeze.Builtin.Provider.BModule;
import Zeze.Builtin.Provider.Bind;
import Zeze.Builtin.Provider.Dispatch;
import Zeze.Builtin.Provider.SetDisableChoice;
import Zeze.Builtin.Provider.Subscribe;
import Zeze.IModule;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Net.Protocol;
import Zeze.Services.HandshakeClient;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Services.ServiceManager.BServiceInfos;
import Zeze.Util.OutObject;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProviderService extends HandshakeClient {
	private static final Logger logger = LogManager.getLogger(ProviderService.class);

	protected ProviderApp providerApp;
	private final ConcurrentHashMap<String, Connector> links = new ConcurrentHashMap<>();
	private volatile @NotNull Connector[] linkConnectors = new Connector[0];
	private final AtomicInteger linkRandomIndex = new AtomicInteger();

	// 用来同步等待Provider的静态绑定完成。
	public final TaskCompletionSource<Boolean> providerStaticBindCompleted = new TaskCompletionSource<>();
	public final TaskCompletionSource<Boolean> providerDynamicSubscribeCompleted = new TaskCompletionSource<>();

	public ProviderService(@NotNull String name, @NotNull Application zeze) {
		super(name, zeze);
	}

	/**
	 * 不使用 RemoteEndPoint 是怕有些系统返回 ipv6 有些 ipv4，造成不一致。
	 * 这里要求 linkName 在所有 provider 中都一样。
	 * 使用 Connector 配置得到名字，只要保证配置一样。
	 */
	public static @NotNull String getLinkName(@NotNull AsyncSocket sender) {
		//noinspection DataFlowIssue
		return sender.getConnector().getName();
	}

	public static @NotNull String getLinkName(@NotNull BServiceInfo serviceInfo) {
		return serviceInfo.getPassiveIp() + "_" + serviceInfo.getPassivePort();
	}

	public void kick(@NotNull String linkName, long linkSid, int code, @NotNull String desc) {
		if (linkSid == 0)
			return;

		var link = links.get(linkName);
		if (link != null)
			ProviderImplement.sendKick(link.TryGetReadySocket(), linkSid, code, desc);
	}

	@Override
	public void start() throws Exception {
		// copy Config.Connector to Links
		getConfig().forEachConnector(c -> links.putIfAbsent(c.getName(), c));
		super.start();
	}

	public @Nullable Connector apply(@NotNull BServiceInfo link) {
		var linkName = getLinkName(link);
		return links.computeIfAbsent(linkName, __ -> {
			var outC = new OutObject<Connector>();
			if (getConfig().tryGetOrAddConnector(link.getPassiveIp(), link.getPassivePort(), true, outC)) {
				try {
					outC.value.start();
				} catch (Exception e) {
					Task.forceThrow(e);
				}
			}
			return outC.value;
		});
	}

	public void apply(@NotNull BServiceInfos serviceInfos) {
		var current = new HashSet<String>();
		for (var link : serviceInfos.getServiceInfoListSortedByIdentity()) {
			var connector = apply(link);
			if (connector != null)
				current.add(connector.getName());
		}
		// 删除多余的连接器。
		for (var linkName : links.keySet()) {
			if (current.contains(linkName))
				continue;
			var removed = links.remove(linkName);
			if (removed != null) {
				getConfig().removeConnector(removed);
				removed.stop();
			}
		}
		linkConnectors = links.values().toArray(new Connector[links.size()]);
	}

	public static class LinkSession {
		public final @NotNull String name;
		public final long sessionId;

		public LinkSession(@NotNull AsyncSocket so) {
			name = getLinkName(so);
			sessionId = so.getSessionId();
		}
	}

	public @NotNull ConcurrentHashMap<String, Connector> getLinks() {
		return links;
	}

	private volatile boolean disableChoice = false;

	public synchronized void initDisableChoiceFromLinks(boolean value) {
		disableChoice = value;
	}

	public void setDisableChoiceFromLinks(boolean value) {
		// ProviderApp 构造的时候初始化，相当于final了。
		// 这样写是为了用户不用在构造ProviderService的时候传参数。
		//noinspection SynchronizeOnNonFinalField
		synchronized (providerApp) {
			if (!providerApp.isOnlineReady())
				throw new RuntimeException("online not ready.");
			providerApp.setUserDisableChoice(value);
			for (var link : links.values())
				sendDisableChoiceToLink(link, value);
		}
	}

	void trySetLinkChoice() {
		for (var link : links.values())
			trySetLinkChoice(link);
	}

	private void trySetLinkChoice(Connector link) {
		if (providerApp.isOnlineReady())
			sendDisableChoiceToLink(link, providerApp.isUserDisableChoice());
	}

	private static void sendDisableChoiceToLink(Connector link, boolean value) {
		var r = new SetDisableChoice();
		r.Argument.setDisableChoice(value);
		r.Send(link.getSocket(), (p) -> {
			if (r.isTimeout() || r.getResultCode() != 0)
				logger.error("setDisableChoice fail. {}", link.getName());
			return 0;
		});
	}

	public @Nullable AsyncSocket randomLink() {
		var volatileTmp = linkConnectors;
		if (volatileTmp.length == 0)
			return null;

		var index = linkRandomIndex.getAndIncrement();
		var connector = volatileTmp[Integer.remainderUnsigned(index, volatileTmp.length)];
		// 如果只选择已经连上的Link，当所有的连接都没准备好时，仍然需要GetReadySocket，
		// 所以简单处理成总是等待连接完成。
		return connector.GetReadySocket();
	}

	@SuppressWarnings("MethodMayBeStatic")
	public @NotNull LinkSession newSession(@NotNull AsyncSocket so) {
		return new LinkSession(so);
	}

	@Override
	public void OnHandshakeDone(@NotNull AsyncSocket so) throws Exception {
		super.OnHandshakeDone(so);
		so.setUserState(newSession(so));

		var announce = new AnnounceProviderInfo();
		announce.Argument.setServiceNamePrefix(providerApp.serverServiceNamePrefix);
		announce.Argument.setServiceIdentity(String.valueOf(getZeze().getConfig().getServerId()));
		announce.Argument.setProviderDirectIp(providerApp.directIp);
		announce.Argument.setProviderDirectPort(providerApp.directPort);
		announce.Argument.setAppVersion(providerApp.zeze.getConfig().getAppVersion());
		announce.Argument.setDisableChoice(disableChoice);

		announce.Send(so);

		// static binds
		var bind = new Bind();
		providerApp.staticBinds.foreach(bind.Argument.getModules()::put);
		bind.Send(so, rpc -> {
			providerStaticBindCompleted.setResult(true);
			return 0;
		});
		var sub = new Subscribe();
		providerApp.dynamicModules.foreach(sub.Argument.getModules()::put);
		sub.Send(so, rpc -> {
			providerDynamicSubscribeCompleted.setResult(true);
			return 0;
		});

		trySetLinkChoice(so.getConnector());
	}

	// 热更新增模块。
	// 1. 热更才会调用；
	// 2. 只有新增模块才会调用；
	public void addHotModule(IModule module, BModule.Data config) {
		{
			// 全局数据更新
			providerApp.zeze.getAppBase().addModule(module);
			providerApp.modules.put(module.getId(), config);
		}
		{
			// 注册订阅服务。
			var sm = providerApp.zeze.getServiceManager();
			var identity = String.valueOf(providerApp.zeze.getConfig().getServerId());
			sm.registerService(
					providerApp.serverServiceNamePrefix + module.getId(), identity,
					providerApp.directIp, providerApp.directPort);
			sm.subscribeService(providerApp.serverServiceNamePrefix + module.getId(), config.getSubscribeType());
		}

		// 并通知所有links。
		if (config.getConfigType() != BModule.Data.ConfigTypeDynamic) {
			providerApp.staticBinds.put(module.getId(), config);
			var bind = new Bind();
			bind.Argument.getModules().put(module.getId(), config);
			for (var link : links.values())
				bind.Send(link.TryGetReadySocket());
		} else {
			providerApp.dynamicModules.put(module.getId(), config);
			var sub = new Subscribe();
			sub.Argument.getModules().put(module.getId(), config);
			for (var link : links.values())
				sub.Send(link.TryGetReadySocket());
		}
	}

	@Override
	public void dispatchProtocol(@NotNull Protocol<?> p, @NotNull ProtocolFactoryHandle<?> factoryHandle) throws Exception {
		if (p instanceof Dispatch) {
			//noinspection RedundantCast,DataFlowIssue
			getZeze().getTaskOneByOneByKey().Execute(((Dispatch)p).Argument.getAccount(),
					() -> Task.call(() -> factoryHandle.Handle.handleProtocol(p), p, Protocol::trySendResultCode),
					factoryHandle.Mode);
		} else
			super.dispatchProtocol(p, factoryHandle);
	}

	/*
	public void reportLoad(int online, int proposeMaxOnline, int onlineNew) {
		var report = new ReportLoad();

		report.Argument.setOnline(online);
		report.Argument.setProposeMaxOnline(proposeMaxOnline);
		report.Argument.setOnlineNew(onlineNew);

		for (var link : Links.values()) {
			if (link.isHandshakeDone()) {
				link.getSocket().Send(report);
			}
		}
	}
	*/
}
