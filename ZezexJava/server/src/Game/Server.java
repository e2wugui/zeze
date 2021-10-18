package Game;

import Zeze.Net.*;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import java.util.concurrent.ConcurrentHashMap;
import java.util.*;

// auto-generated


public final class Server extends ServerBase {
	private static final Logger logger = LogManager.getLogger(Server.class);

	/** 
	 不使用 RemoteEndPoint 是怕有些系统返回 ipv6 有些 ipv4，造成不一致。
	 这里要求 linkName 在所有 provider 中都一样。
	 使用 Connector 配置得到名字，只要保证配置一样。
	 
	 @param sender
	 @return 
	*/
	public String GetLinkName(AsyncSocket sender) {
		return sender.getConnector().getName();
	}

	public String GetLinkName(Zeze.Services.ServiceManager.ServiceInfo serviceInfo) {
		return serviceInfo.getPassiveIp() + ":" + serviceInfo.getPassivePort();
	}

	@Override
	public void Start() {
		// copy Config.Connector to Links
		getConfig().ForEachConnector((c) -> getLinks().putIfAbsent(c.getName(), c));
		super.Start();
	}

	public void ApplyLinksChanged(Zeze.Services.ServiceManager.ServiceInfos serviceInfos) {
		HashSet<String> current = new HashSet<String>();
		for (var link : serviceInfos.getServiceInfoListSortedByIdentity()) {
			var linkName = GetLinkName(link);
			current.add(getLinks().computeIfAbsent(linkName, (key) -> {
				var outc = new Zeze.Util.OutObject<Connector>();
					if (getConfig().TryGetOrAddConnector(link.getPassiveIp(), link.getPassivePort(), true, outc)) {
						outc.Value.Start();
					}
					return outc.Value;
			}).getName());
		}
		// 删除多余的连接器。
		for (var linkName : getLinks().keySet()) {
			if (current.contains(linkName)) {
				continue;
			}
			var removed = getLinks().remove(linkName);
			if (null != removed) {
				getConfig().RemoveConnector(removed);
				removed.Stop();
			}
		}
	}

	public static class LinkSession {
		private String Name;
		public final String getName() {
			return Name;
		}
		private long SessionId;
		public final long getSessionId() {
			return SessionId;
		}

		// 在和linkd连接建立完成以后，由linkd发送通告协议时保存。
		private int LinkId;
		public final int getLinkId() {
			return LinkId;
		}
		private void setLinkId(int value) {
			LinkId = value;
		}
		private long ProviderSessionId;
		public final long getProviderSessionId() {
			return ProviderSessionId;
		}
		private void setProviderSessionId(long value) {
			ProviderSessionId = value;
		}

		public LinkSession(String name, long sid) {
			Name = name;
			SessionId = sid;
		}

		public final void Setup(int linkId, long providerSessionId) {
			setLinkId(linkId);
			setProviderSessionId(providerSessionId);
		}
	}

	private ConcurrentHashMap<String, Connector> Links = new ConcurrentHashMap<String, Connector> ();
	public ConcurrentHashMap<String, Connector> getLinks() {
		return Links;
	}

	// 用来同步等待Provider的静态绑定完成。
	public Zeze.Util.ManualResetEvent ProviderStaticBindCompleted = new Zeze.Util.ManualResetEvent(false);

	@Override
	public void OnHandshakeDone(AsyncSocket sender) {
		super.OnHandshakeDone(sender);
		var linkName = GetLinkName(sender);
		sender.setUserState(new LinkSession(linkName, sender.getSessionId()));

		var announce = new Zezex.Provider.AnnounceProviderInfo();
		announce.Argument.setServiceNamePrefix(App.ServerServiceNamePrefix);
		announce.Argument.setServiceIndentity(String.valueOf(getZeze().getConfig().getServerId()));
		announce.Send(sender);

		// static binds
		var rpc = new Zezex.Provider.Bind();
		rpc.Argument.getModules().putAll(Game.App.getInstance().getStaticBinds());
		rpc.Send(sender, (protocol) -> {
				ProviderStaticBindCompleted.Set();
				return 0;
		});
	}

	@Override
	public void DispatchProtocol(Protocol p, ProtocolFactoryHandle factoryHandle) {
		// 防止Client不进入加密，直接发送用户协议。
		if (false == IsHandshakeProtocol(p.getTypeId())) {
			p.Sender.VerifySecurity();
		}

		if (p.getTypeId() == Zezex.Provider.ModuleRedirect.TypeId_) {
			if (null != factoryHandle.Handle) {
				var modureRecirect = p instanceof Zezex.Provider.ModuleRedirect ? (Zezex.Provider.ModuleRedirect)p : null;
				if (null != getZeze() && false == factoryHandle.NoProcedure) {
					getZeze().getTaskOneByOneByKey().Execute(
							modureRecirect.Argument.getHashCode(),
							() -> Zeze.Util.Task.Call(getZeze().NewProcedure(() -> factoryHandle.Handle.handle(p),
									p.getClass().getName(), p.UserState),
									p, (p2, code) -> p2.SendResultCode(code)));
				}
				else {
					getZeze().getTaskOneByOneByKey().Execute(modureRecirect.Argument.getHashCode(),
							() -> Zeze.Util.Task.Call(() -> factoryHandle.Handle.handle(p), p,
									(p2, code) -> p2.SendResultCode(code)));
				}
			}
			else {
				logger.log(getSocketOptions().getSocketLogLevel(), "Protocol Handle Not Found. {}", p);
			}
			return;
		}

		super.DispatchProtocol(p, factoryHandle);
	}

	public void ReportLoad(int online, int proposeMaxOnline, int onlineNew) {
		var report = new Zezex.Provider.ReportLoad();

		report.Argument.setOnline(online);
		report.Argument.setProposeMaxOnline(proposeMaxOnline);
		report.Argument.setOnlineNew(onlineNew);

		for (var link : getLinks().values()) {
			if (link.isHandshakeDone()) {
				link.getSocket().Send(report);
			}
		}
	}


	public Server(Zeze.Application zeze) {
		super(zeze);
	}

}