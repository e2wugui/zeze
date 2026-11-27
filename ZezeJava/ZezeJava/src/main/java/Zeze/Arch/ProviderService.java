package Zeze.Arch;

import java.util.Objects;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Application;
import Zeze.Builtin.Provider.AnnounceProviderInfo;
import Zeze.Builtin.Provider.Bind;
import Zeze.Builtin.Provider.Dispatch;
import Zeze.Builtin.Provider.SetDisableChoice;
import Zeze.Builtin.Provider.Subscribe;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolHandle;
import Zeze.Net.Rpc;
import Zeze.Services.HandshakeClient;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Util.OutObject;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import Zeze.Util.TaskSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/**
 * Provider 连接 Linkd 的客户端服务：维护到各 Linkd 的连接，握手时宣告自身信息并完成模块 Bind/Subscribe。
 */
public class ProviderService extends HandshakeClient {
	private static final Logger logger = LogManager.getLogger(ProviderService.class);

	protected @Nullable ProviderApp providerApp;

	protected final ProviderApp requireProviderApp() {
		return Objects.requireNonNull(providerApp, "provider is not attached to an application");
	}
	private final ConcurrentHashMap<String, Connector> links = new ConcurrentHashMap<>();
	private volatile Connector [] linkConnectors = new Connector[0];
	private final AtomicInteger linkRandomIndex = new AtomicInteger();

	// 用来同步等待Provider的静态绑定完成。
	public final TaskCompletionSource<Boolean> providerStaticBindCompleted = new TaskCompletionSource<>();
	public final TaskCompletionSource<Boolean> providerDynamicSubscribeCompleted = new TaskCompletionSource<>();

	public ProviderService(String name, Application zeze) {
		super(name, zeze);
	}

	/**
	 * 不使用 RemoteEndPoint 是怕有些系统返回 ipv6 有些 ipv4，造成不一致。
	 * 这里要求 linkName 在所有 provider 中都一样。
	 * 使用 Connector 配置得到名字，只要保证配置一样。
	 */
	public static String getLinkName(AsyncSocket sender) {
		//noinspection DataFlowIssue
		return sender.getConnector().getName();
	}

	public static String getLinkName(BServiceInfo serviceInfo) {
		return serviceInfo.getPassiveIp() + "_" + serviceInfo.getPassivePort();
	}

	public void kick(String linkName, long linkSid, int code, String desc) {
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
		refreshLinkConnectors();
		super.start();
	}

	public boolean applyPut(BServiceInfo link) {
		var linkName = getLinkName(link);
		var isNew = new OutObject<>(false);
		links.computeIfAbsent(linkName, __ -> {
			var outC = new OutObject<Connector>();
			if (getConfig().tryGetOrAddConnector(link.getPassiveIp(), link.getPassivePort(), true, outC)) {
				try {
					//noinspection DataFlowIssue
					outC.value.start();
					isNew.value = true;
				} catch (Exception e) {
					throw Task.forceThrow(e);
				}
			}
			return outC.value;
		});
		return Objects.requireNonNull(isNew.value);
	}

	public boolean applyRemove(BServiceInfo link) {
		var linkName = getLinkName(link);
		var removed = links.remove(linkName);
		if (removed != null) {
			getConfig().removeConnector(removed);
			removed.stop();
			return true;
		}
		return false;
	}

	public void refreshLinkConnectors() {
		linkConnectors = links.values().toArray(new Connector[0]);
	}

	public static class LinkSession {
		public final String name;
		public final long sessionId;

		public LinkSession(AsyncSocket so) {
			name = getLinkName(so);
			sessionId = so.getSessionId();
		}
	}

	public ConcurrentHashMap<String, Connector> getLinks() {
		return links;
	}

	private volatile boolean initDisableChoice = false;

	public void initDisableChoiceFromLinks(boolean value) {
		lock();
		try {
			initDisableChoice = value;
		} finally {
			unlock();
		}
	}

	boolean isInitDisableChoice() {
		return initDisableChoice;
	}

	public void setDisableChoiceFromLinks(boolean value) {
		// ProviderApp 构造的时候初始化，相当于final了。
		// 这样写是为了用户不用在构造ProviderService的时候传参数。
		requireProviderApp().lock();
		try {
			if (!requireProviderApp().isOnlineReady())
				throw new RuntimeException("online not ready.");
			requireProviderApp().setUserDisableChoice(value);
			for (var link : links.values())
				sendDisableChoiceToLink(link, value);
		} finally {
			requireProviderApp().unlock();
		}
	}

	protected void trySetLinkChoice() {
		for (var link : links.values())
			trySetLinkChoice(link);
	}

	private void trySetLinkChoice(Connector link) {
		if (requireProviderApp().isOnlineReady())
			sendDisableChoiceToLink(link, requireProviderApp().isUserDisableChoice());
	}

	private static void sendDisableChoiceToLink(Connector link, boolean value) {
		var r = new SetDisableChoice();
		r.Argument.setDisableChoice(value);
		r.sendCallbackAlways(link.getSocket(), (p) -> {
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
	public LinkSession newSession(AsyncSocket so) {
		return new LinkSession(so);
	}

	@Override
	public void OnHandshakeDone(AsyncSocket so) throws Exception {
		super.OnHandshakeDone(so);
		so.setUserState(newSession(so));

		var announce = new AnnounceProviderInfo();
		announce.Argument.setServiceNamePrefix(Objects.requireNonNull(requireProviderApp().serverServiceNamePrefix));
		//noinspection DataFlowIssue
		announce.Argument.setServiceIdentity(String.valueOf(getZeze().getConfig().getServerId()));
		announce.Argument.setProviderDirectIp(Objects.requireNonNull(requireProviderApp().directIp));
		announce.Argument.setProviderDirectPort(requireProviderApp().directPort);
		announce.Argument.setAppVersion(requireProviderApp().zeze.getConfig().getAppVersion());
		announce.Argument.setDisableChoice(!requireProviderApp().isOnlineReady() || requireProviderApp().isUserDisableChoice());

		announce.Send(so);

		// static binds
		var bind = new Bind();
		requireProviderApp().lock();
		try {
			requireProviderApp().staticBinds.foreach(bind.Argument.getModules()::put);
		} finally {
			requireProviderApp().unlock();
		}
		var linkName = getLinkName(so);
		bind.Send(so, rpc -> {
			// 仅在 Bind 成功时置位启动完成信号。
			if (checkLinkdHandshakeResult(rpc, so, linkName, "Bind"))
				providerStaticBindCompleted.setResult(true);
			return 0;
		});
		var sub = new Subscribe();
		requireProviderApp().lock();
		try {
			requireProviderApp().dynamicModules.foreach(sub.Argument.getModules()::put);
		} finally {
			requireProviderApp().unlock();
		}
		sub.Send(so, rpc -> {
			if (checkLinkdHandshakeResult(rpc, so, linkName, "Subscribe"))
				providerDynamicSubscribeCompleted.setResult(true);
			return 0;
		});

		var c = so.getConnector();
		if (c != null)
			trySetLinkChoice(c);
	}

	// 检查 Bind/Subscribe 应答：超时或失败时关闭连接以触发重连重试，成功返回 true。
	static boolean checkLinkdHandshakeResult(Rpc<?, ?> rpc, AsyncSocket so,
	                                         String linkName, String what) {
		if (rpc.isTimeout() || rpc.getResultCode() != 0) {
			logger.error("linkd {} fail. link={}, isTimeout={}, code={}",
					what, linkName, rpc.isTimeout(), rpc.getResultCode());
			so.close(new IOException("ProviderService." + what + " fail: isTimeout=" + rpc.isTimeout()
					+ ", code=" + rpc.getResultCode()));
			return false;
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void dispatchProtocol(Protocol<?> p, ProtocolFactoryHandle<?> factoryHandle)
			throws Exception {
		if (p instanceof Dispatch d) {
			//noinspection DataFlowIssue
			TaskSpec.ofFunc(() -> ((ProtocolHandle<Protocol<?>>)factoryHandle.Handle).handle(p), p,
							(__, code) -> p.trySendResultCode(code))
					.dispatchMode(factoryHandle.Mode)
					.executeOneByOne(d.Argument.getAccount());
		} else
			super.dispatchProtocol(p, factoryHandle);
	}
}
