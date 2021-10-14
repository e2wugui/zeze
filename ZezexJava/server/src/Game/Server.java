package Game;

import Zeze.Net.*;
import java.util.*;

// auto-generated


public final class Server extends Zeze.Services.HandshakeClient {
	private static final NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

	/** 
	 不使用 RemoteEndPoint 是怕有些系统返回 ipv6 有些 ipv4，造成不一致。
	 这里要求 linkName 在所有 provider 中都一样。
	 使用 Connector 配置得到名字，只要保证配置一样。
	 
	 @param sender
	 @return 
	*/
	public String GetLinkName(AsyncSocket sender) {
		return sender.Connector.Name;
	}

	public String GetLinkName(Zeze.Services.ServiceManager.ServiceInfo serviceInfo) {
		return serviceInfo.PassiveIp + ":" + serviceInfo.PassivePort;
	}

	@Override
	public void Start() {
		// copy Config.Connector to Links
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		getConfig().ForEachConnector((c) -> getLinks().TryAdd(c.Name, c));
		super.Start();
	}

	public void ApplyLinksChanged(Zeze.Services.ServiceManager.ServiceInfos serviceInfos) {
		HashSet<String> current = new HashSet<String>();
		for (var link : serviceInfos.ServiceInfoListSortedByIdentity) {
			var linkName = GetLinkName(link);
			current.add(getLinks().putIfAbsent(linkName, (key) -> {
					Zeze.Net.Connector c;
					tangible.OutObject<Connector> tempOut_c = new tangible.OutObject < getZeze().Net.Connector>();
					if (getConfig().TryGetOrAddConnector(link.PassiveIp, link.PassivePort, true, tempOut_c)) {
						c = tempOut_c.outArgValue;
						c.Start();
					}
					else {
						c = tempOut_c.outArgValue;
					}
					return c;
			}).Name);
		}
		// 删除多余的连接器。
		for (var linkName : getLinks().keySet()) {
			if (current.contains(linkName)) {
				continue;
			}
			TValue removed;
			tangible.OutObject<Connector> tempOut_removed = new tangible.OutObject<Connector>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
			if (getLinks().TryRemove(linkName, tempOut_removed)) {
			removed = tempOut_removed.outArgValue;
				getConfig().RemoveConnector(removed);
				removed.Stop();
			}
		else {
			removed = tempOut_removed.outArgValue;
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

	private java.util.concurrent.ConcurrentHashMap<String, Connector> Links = new java.util.concurrent.ConcurrentHashMap<String, Connector> ();
	public java.util.concurrent.ConcurrentHashMap<String, Connector> getLinks() {
		return Links;
	}

	// 用来同步等待Provider的静态绑定完成。
	public System.Threading.ManualResetEvent ProviderStaticBindCompleted = new System.Threading.ManualResetEvent(false);

	@Override
	public void OnHandshakeDone(AsyncSocket sender) {
		super.OnHandshakeDone(sender);
		var linkName = GetLinkName(sender);
		sender.UserState = new LinkSession(linkName, sender.SessionId);

		var announce = new Zezex.Provider.AnnounceProviderInfo();
		announce.getArgument().ServiceNamePrefix = App.ServerServiceNamePrefix;
		announce.getArgument().ServiceIndentity = getZeze().Config.ServerId.toString();
		announce.Send(sender);

		// static binds
		var rpc = new Zezex.Provider.Bind();
		rpc.getArgument().getModules().AddRange(Game.App.getInstance().getStaticBinds());
		rpc.Send(sender, (protocol) -> {
				ProviderStaticBindCompleted.Set();
				return 0;
		});
	}

	@Override
	public void DispatchProtocol(Protocol p, ProtocolFactoryHandle factoryHandle) {
		// 防止Client不进入加密，直接发送用户协议。
		if (false == IsHandshakeProtocol(p.TypeId)) {
			p.Sender.VerifySecurity();
		}

		if (p.TypeId == Zezex.Provider.ModuleRedirect.TypeId_) {
			if (null != factoryHandle.Handle) {
				var modureRecirect = p instanceof Zezex.Provider.ModuleRedirect ? (Zezex.Provider.ModuleRedirect)p : null;
				if (null != getZeze() && false == factoryHandle.NoProcedure) {
					getZeze().TaskOneByOneByKey.Execute(modureRecirect.getArgument().getHashCode(), () -> Zeze.Util.Task.Call(getZeze().NewProcedure(() -> factoryHandle.Handle(p), p.getClass().getName(), p.UserState), p, (p, code) -> p.SendResultCode(code)));
				}
				else {
					getZeze().TaskOneByOneByKey.Execute(modureRecirect.getArgument().getHashCode(), () -> Zeze.Util.Task.Call(() -> factoryHandle.Handle(p), p, (p, code) -> p.SendResultCode(code)));
				}
			}
			else {
				logger.Log(getSocketOptions().SocketLogLevel, "Protocol Handle Not Found. {0}", p);
			}
			return;
		}

		super.DispatchProtocol(p, factoryHandle);
	}

	public void ReportLoad(int online, int proposeMaxOnline, int onlineNew) {
		var report = new Zezex.Provider.ReportLoad();

		report.getArgument().Online = online;
		report.getArgument().ProposeMaxOnline = proposeMaxOnline;
		report.getArgument().OnlineNew = onlineNew;

		for (var link : getLinks().values()) {
			if (link.IsHandshakeDone) {
				link.Socket.Send(report);
			}
		}
	}


	public Server(Zeze.Application zeze) {
		super("Server", zeze);
	}

}