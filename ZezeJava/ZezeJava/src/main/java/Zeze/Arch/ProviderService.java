package Zeze.Arch;

import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Builtin.Provider.AnnounceProviderInfo;
import Zeze.Builtin.Provider.BAnnounceProviderInfo;
import Zeze.Builtin.Provider.Bind;
import Zeze.Builtin.Provider.Subscribe;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Services.ServiceManager.BServiceInfos;
import Zeze.Util.OutObject;
import Zeze.Util.TaskCompletionSource;

public class ProviderService extends Zeze.Services.HandshakeClient {
	// private static final Logger logger = LogManager.getLogger(ProviderService.class);

	protected ProviderApp providerApp;
	private final ConcurrentHashMap<String, Connector> links = new ConcurrentHashMap<>();
	private volatile Connector[] linkConnectors = new Connector[0];
	private final AtomicInteger linkRandomIndex = new AtomicInteger();

	// 用来同步等待Provider的静态绑定完成。
	public final TaskCompletionSource<Boolean> providerStaticBindCompleted = new TaskCompletionSource<>();
	public final TaskCompletionSource<Boolean> providerDynamicSubscribeCompleted = new TaskCompletionSource<>();

	public ProviderService(String name, Zeze.Application zeze) {
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
		return serviceInfo.getPassiveIp() + ":" + serviceInfo.getPassivePort();
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
		super.start();
	}

	public void apply(BServiceInfos serviceInfos) {
		var current = new HashSet<String>();
		for (var link : serviceInfos.getServiceInfoListSortedByIdentity()) {
			var linkName = getLinkName(link);
			var connector = links.computeIfAbsent(linkName, __ -> {
				var outC = new OutObject<Connector>();
				if (getConfig().tryGetOrAddConnector(link.getPassiveIp(), link.getPassivePort(), true, outC)) {
					try {
						outC.value.start();
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
				return outC.value;
			});
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

	public AsyncSocket randomLink() {
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

		var announce = new AnnounceProviderInfo(new BAnnounceProviderInfo(providerApp.serverServiceNamePrefix,
				String.valueOf(getZeze().getConfig().getServerId()), providerApp.directIp, providerApp.directPort));
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
