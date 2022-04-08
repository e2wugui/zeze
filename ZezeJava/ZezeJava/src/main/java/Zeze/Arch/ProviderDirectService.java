package Zeze.Arch;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Beans.ProviderDirect.AnnounceProviderInfo;
import Zeze.Beans.ProviderDirect.ModuleRedirect;
import Zeze.Beans.ProviderDirect.ModuleRedirectAllResult;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolHandle;
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

	public void TryConnectTo(Zeze.Services.ServiceManager.ServiceInfos infos) {
		for (var pm : infos.getServiceInfoListSortedByIdentity()) {
			var serverId = Integer.parseInt(pm.getServiceIdentity());
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
		var ps = new ProviderSession(socket.getSessionId());
		socket.setUserState(ps);
		var c = socket.getConnector();
		if (c != null) {
			// 主动连接。
			SetRelativeServiceReady(ps, c.getHostNameOrAddress(), c.getPort());
			var r = new AnnounceProviderInfo();
			r.Argument.setIp(ProviderApp.ProviderDirectPassiveIp);
			r.Argument.setPort(ProviderApp.ProviderDirectPassivePort);
			r.Send(socket, (_r) -> 0L); // skip result
		}
		// 被动连接等待对方报告信息时再处理。
		// call base
		super.OnHandshakeDone(socket);
	}

	public ConcurrentHashMap<String, ProviderSession> ProviderSessions = new ConcurrentHashMap<>();

	void SetRelativeServiceReady(ProviderSession ps, String ip, int port) {
		ps.ServerLoadIp = ip;
		ps.ServerLoadPort = port;
		ProviderSessions.put(ps.getServerLoadName(), ps);

		// 需要把所有符合当前连接目标的Provider相关的服务信息都更新到当前连接的状态。
		for (var ss : getZeze().getServiceManagerAgent().getSubscribeStates().values()) {
			if (ss.getServiceName().startsWith(ProviderApp.ServerServiceNamePrefix)) {
				var mid = Integer.parseInt(ss.getServiceName().split("#")[1]);
				var m = ProviderApp.Modules.get(mid);
				for (var server : ss.getServiceInfos().getServiceInfoListSortedByIdentity()) {
					// 符合当前连接目标。每个Identity标识的服务的(ip,port)必须不一样。
					if (server.getPassiveIp().equals(ip) && server.getPassivePort() == port) {
						var pms = new ProviderModuleState(ps.getSessionId(), mid, m.getChoiceType(), m.getConfigType());
						ps.GetOrAddServiceReadyState(ss.getServiceName()).put(server.getServiceIdentity(), pms);
						ss.SetServiceIdentityReadyState(server.getServiceIdentity(), pms);
					}
				}
			}
		}
	}

	@Override
	public void OnSocketClose(AsyncSocket socket, Throwable ex) throws Throwable {
		var ps = (ProviderSession)socket.getUserState();
		if (null != ps) {
			for (var service : ps.ServiceReadyStates.entrySet()) {
				var subs = getZeze().getServiceManagerAgent().getSubscribeStates().get(service.getKey());
				for (var identity : service.getValue().keySet()) {
					subs.SetServiceIdentityReadyState(identity, null);
				}
			}
		}
		ProviderSessions.remove(ps.getServerLoadName());
		super.OnSocketClose(socket, ex);
	}

	@Override
	public <P extends Protocol<?>> void DispatchProtocol(P p, ProtocolFactoryHandle<P> factoryHandle) throws Throwable {
		// 防止Client不进入加密，直接发送用户协议。
		if (!IsHandshakeProtocol(p.getTypeId())) {
			p.getSender().VerifySecurity();
		}

		if (p.getTypeId() == ModuleRedirect.TypeId_) {
			if (null != factoryHandle.Handle) {
				var redirect = (ModuleRedirect)p;
				// 总是不启用存储过程，内部处理redirect时根据Redirect.Handle配置决定是否在存储过程中执行。
				getZeze().getTaskOneByOneByKey().Execute(redirect.Argument.getHashCode(),
						() -> Zeze.Util.Task.Call(() -> factoryHandle.Handle.handle(p), p, Protocol::SendResultCode));
			} else
				logger.warn("Protocol Handle Not Found: {}", p);
			return;
		}
		if (p.getTypeId() == ModuleRedirectAllResult.TypeId_) {
			if (null != factoryHandle.Handle) {
				var r = (ModuleRedirectAllResult)p;
				// 总是不启用存储过程，内部处理redirect时根据Redirect.Handle配置决定是否在存储过程中执行。
				Zeze.Util.Task.Call(() -> factoryHandle.Handle.handle(p), p, Protocol::SendResultCode, r.Argument.getMethodFullName());
			} else
				logger.warn("Protocol Handle Not Found: {}", p);
			return;
		}

		super.DispatchProtocol(p, factoryHandle);
	}

	@Override
	public <P extends Protocol<?>> void DispatchRpcResponse(
			P rpc, ProtocolHandle<P> responseHandle, ProtocolFactoryHandle<?> factoryHandle) throws Throwable {

		if (rpc.getTypeId() == ModuleRedirect.TypeId_) {
			var redirect = (ModuleRedirect)rpc;
			// 总是不启用存储过程，内部处理redirect时根据Redirect.Handle配置决定是否在存储过程中执行。
			getZeze().getTaskOneByOneByKey().Execute(redirect.Argument.getHashCode(),
					() -> Zeze.Util.Task.Call(() -> responseHandle.handle(rpc), rpc));
			return;
		}

		super.DispatchRpcResponse(rpc, responseHandle, factoryHandle);
	}
}
