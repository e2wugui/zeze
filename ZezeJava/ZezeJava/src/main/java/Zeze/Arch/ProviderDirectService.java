package Zeze.Arch;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Net.Protocol;
import Zeze.Transaction.TransactionLevel;
import Zeze.Beans.ProviderDirect.*;
import Zeze.Util.KV;
import Zeze.Util.OutObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Provider之间直连网络管理服务。
 */
public class ProviderDirectService extends Zeze.Services.HandshakeBoth {
	private static final Logger logger = LogManager.getLogger(ProviderDirectService.class);
	public ProviderApp ProviderApp;

	public ProviderDirectService(String name, Zeze.Application zeze) throws Throwable {
		super(name, zeze);
	}

	public void Apply(Zeze.Services.ServiceManager.ServiceInfos infos) {
		for (var pm : infos.getServiceInfoListSortedByIdentity()) {
			var serverId = Integer.valueOf(pm.getServiceIdentity());
			if (serverId <= getZeze().getConfig().getServerId())
				continue;
			var out = new OutObject<Connector>();
			if (getConfig().TryGetOrAddConnector(pm.getPassiveIp(), pm.getPassivePort(), true, out)) {
				// 新建的Connector。开始连接。
				out.Value.Start();
			}
		}
	}

	@Override
	public void OnHandshakeDone(AsyncSocket socket) throws Throwable {
		super.OnHandshakeDone(socket);
		var c = socket.getConnector();
		if (c != null) {
			// 主动连接。
			updateServiceInfos(c.getHostNameOrAddress(), c.getPort(), socket.getSessionId());
			var r = new AnnounceProviderInfo();
			r.Argument.setIp(ProviderApp.ProviderDirectPassiveIp);
			r.Argument.setPort(ProviderApp.ProviderDirectPassivePort);
			r.Send(socket, (_r) -> 0L); // skip result
		}
		// 被动连接等待对方报告信息时再处理。
	}

	void updateServiceInfos(String ip, int port, long sessionId) {
		// 需要把所有符合当前连接目标的Provider相关的服务信息都更新到当前连接的状态。
		for (var ss : getZeze().getServiceManagerAgent().getSubscribeStates().values()) {
			if (ss.getServiceName().startsWith(ProviderApp.ServerServiceNamePrefix)) {
				var mid = Integer.valueOf(ss.getServiceName().split("#")[1]);
				var m = ProviderApp.Modules.get(mid);
				for (var server : ss.getServiceInfos().getServiceInfoListSortedByIdentity()) {
					// 符合当前连接目标。
					if (server.getPassiveIp().equals(ip) && server.getPassivePort() == port) {
						ss.SetServiceIdentityReadyState(server.getServiceIdentity(),
								new ProviderModuleState(sessionId, mid, m.getChoiceType(), m.getConfigType()));
					}
				}
			}
		}
	}

	@Override
	public <P extends Protocol<?>> void DispatchProtocol(P p, ProtocolFactoryHandle<P> factoryHandle) throws Throwable {
		// 防止Client不进入加密，直接发送用户协议。
		if (!IsHandshakeProtocol(p.getTypeId())) {
			p.getSender().VerifySecurity();
		}

		if (p.getTypeId() == ModuleRedirect.TypeId_) {
			if (null != factoryHandle.Handle) {
				var moduleRedirect = (ModuleRedirect)p;
				if (null != getZeze()) {
					if (factoryHandle.Level != TransactionLevel.None) {
						getZeze().getTaskOneByOneByKey().Execute(
								moduleRedirect.Argument.getHashCode(),
								() -> Zeze.Util.Task.Call(getZeze().NewProcedure(() -> factoryHandle.Handle.handle(p),
												p.getClass().getName(), factoryHandle.Level, p.getUserState()),
										p, Protocol::SendResultCode));
					} else {
						getZeze().getTaskOneByOneByKey().Execute(moduleRedirect.Argument.getHashCode(),
								() -> Zeze.Util.Task.Call(() -> factoryHandle.Handle.handle(p), p,
										Protocol::SendResultCode));
					}
				}
			} else
				logger.warn("Protocol Handle Not Found: {}", p);
			return;
		}

		super.DispatchProtocol(p, factoryHandle);
	}
}
