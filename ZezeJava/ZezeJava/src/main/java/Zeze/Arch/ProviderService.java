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

	protected ProviderApp ProviderApp;
	private final ConcurrentHashMap<String, Connector> Links = new ConcurrentHashMap<>();
	private volatile Connector[] LinkConnectors = new Connector[0];
	private final AtomicInteger LinkRandomIndex = new AtomicInteger();

	// 用来同步等待Provider的静态绑定完成。
	public final TaskCompletionSource<Boolean> ProviderStaticBindCompleted = new TaskCompletionSource<>();
	public final TaskCompletionSource<Boolean> ProviderDynamicSubscribeCompleted = new TaskCompletionSource<>();

	public ProviderService(String name, Zeze.Application zeze) throws Throwable {
		super(name, zeze);
	}

	/**
	 * 不使用 RemoteEndPoint 是怕有些系统返回 ipv6 有些 ipv4，造成不一致。
	 * 这里要求 linkName 在所有 provider 中都一样。
	 * 使用 Connector 配置得到名字，只要保证配置一样。
	 */
	public static String GetLinkName(AsyncSocket sender) {
		return sender.getConnector().getName();
	}

	public static String GetLinkName(BServiceInfo serviceInfo) {
		return serviceInfo.getPassiveIp() + ":" + serviceInfo.getPassivePort();
	}

	public void kick(String linkName, long linkSid, int code, String desc) {
		if (linkSid == 0)
			return;

		var link = Links.get(linkName);
		if (link != null)
			ProviderImplement.SendKick(link.TryGetReadySocket(), linkSid, code, desc);
	}

	@Override
	public void Start() throws Throwable {
		// copy Config.Connector to Links
		getConfig().ForEachConnector(c -> Links.putIfAbsent(c.getName(), c));
		super.Start();
	}

	public void Apply(BServiceInfos serviceInfos) {
		var current = new HashSet<String>();
		for (var link : serviceInfos.getServiceInfoListSortedByIdentity()) {
			var linkName = GetLinkName(link);
			var connector = Links.computeIfAbsent(linkName, __ -> {
				var outC = new OutObject<Connector>();
				if (getConfig().tryGetOrAddConnector(link.getPassiveIp(), link.getPassivePort(), true, outC)) {
					try {
						outC.value.Start();
					} catch (Throwable e) {
						throw new RuntimeException(e);
					}
				}
				return outC.value;
			});
			if (connector != null)
				current.add(connector.getName());
		}
		// 删除多余的连接器。
		for (var linkName : Links.keySet()) {
			if (current.contains(linkName))
				continue;
			var removed = Links.remove(linkName);
			if (removed != null) {
				getConfig().removeConnector(removed);
				removed.Stop();
			}
		}
		LinkConnectors = Links.values().toArray(new Connector[Links.size()]);
	}

	public static class LinkSession {
		public final String Name;
		public final long SessionId;

		public LinkSession(String name, long sid) {
			Name = name;
			SessionId = sid;
		}
	}

	public ConcurrentHashMap<String, Connector> getLinks() {
		return Links;
	}

	public AsyncSocket RandomLink() {
		var volatileTmp = LinkConnectors;
		if (volatileTmp.length == 0)
			return null;

		var index = LinkRandomIndex.getAndIncrement();
		var connector = volatileTmp[Integer.remainderUnsigned(index, volatileTmp.length)];
		// 如果只选择已经连上的Link，当所有的连接都没准备好时，仍然需要GetReadySocket，
		// 所以简单处理成总是等待连接完成。
		return connector.GetReadySocket();
	}

	@Override
	public void OnHandshakeDone(AsyncSocket sender) throws Throwable {
		super.OnHandshakeDone(sender);
		sender.setUserState(new LinkSession(GetLinkName(sender), sender.getSessionId()));

		var announce = new AnnounceProviderInfo(new BAnnounceProviderInfo(ProviderApp.ServerServiceNamePrefix,
				String.valueOf(getZeze().getConfig().getServerId()), ProviderApp.DirectIp, ProviderApp.DirectPort));
		announce.Send(sender);

		// static binds
		var bind = new Bind();
		ProviderApp.StaticBinds.foreach(bind.Argument.getModules()::put);
		bind.Send(sender, rpc -> {
			ProviderStaticBindCompleted.setResult(true);
			return 0;
		});
		var sub = new Subscribe();
		ProviderApp.DynamicModules.foreach(sub.Argument.getModules()::put);
		sub.Send(sender, rpc -> {
			ProviderDynamicSubscribeCompleted.setResult(true);
			return 0;
		});
	}

	/*
	public void ReportLoad(int online, int proposeMaxOnline, int onlineNew) {
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
