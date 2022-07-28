package Zeze.Arch;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Provider.BModule;
import Zeze.Builtin.ProviderDirect.AnnounceProviderInfo;
import Zeze.Builtin.ProviderDirect.ModuleRedirect;
import Zeze.Builtin.ProviderDirect.ModuleRedirectAllResult;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolHandle;
import Zeze.Services.ServiceManager.Agent;
import Zeze.Services.ServiceManager.ServiceInfo;
import Zeze.Services.ServiceManager.ServiceInfos;
import Zeze.Services.ServiceManager.SubscribeInfo;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.OutObject;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Provider之间直连网络管理服务。
 */
public class ProviderDirectService extends Zeze.Services.HandshakeBoth {
	private static final Logger logger = LogManager.getLogger(ProviderDirectService.class);

	public ProviderApp ProviderApp;
	public final ConcurrentHashMap<String, ProviderSession> ProviderByLoadName = new ConcurrentHashMap<>();
	public final LongConcurrentHashMap<ProviderSession> ProviderByServerId = new LongConcurrentHashMap<>();

	public ProviderDirectService(String name, Zeze.Application zeze) throws Throwable {
		super(name, zeze);
	}

	public synchronized void RemoveServer(Agent.SubscribeState ss, ServiceInfo pm) {
		var connName = pm.getPassiveIp() + ":" + pm.getPassivePort();
		var conn = getConfig().FindConnector(connName);
		if (null != conn) {
			conn.Stop();
			ProviderByLoadName.remove(connName);
			ProviderByServerId.remove(Long.parseLong(pm.getServiceIdentity()));
			ss.SetServiceIdentityReadyState(pm.getServiceIdentity(), null);
			getConfig().RemoveConnector(conn);
		}
	}

	public synchronized void AddServer(Agent.SubscribeState ss, ServiceInfo pm) {
		var connName = pm.getPassiveIp() + ":" + pm.getPassivePort();
		var ps = ProviderByLoadName.get(connName);
		if (null != ps) {
			// connection has ready.
			var mid = Integer.parseInt(pm.getServiceName().split("#")[1]);
			var m = ProviderApp.Modules.get(mid);
			SetReady(ss, pm, ps, mid, m);
			return;
		}
		var serverId = Integer.parseInt(pm.getServiceIdentity());
		if (serverId < getZeze().getConfig().getServerId())
			return;
		if (serverId == getZeze().getConfig().getServerId()) {
			var localPs = new ProviderSession();
			localPs.ServerId = serverId;
			SetRelativeServiceReady(localPs, ProviderApp.DirectIp, ProviderApp.DirectPort);
			return;
		}
		var out = new OutObject<Connector>();
		if (getConfig().TryGetOrAddConnector(pm.getPassiveIp(), pm.getPassivePort(), true, out)) {
			// 新建的Connector。开始连接。
			var peerPs = new ProviderSession();
			peerPs.ServerId = serverId;
			out.Value.UserState = peerPs;
			out.Value.Start();
		}
	}

	public void TryConnectAndSetReady(Agent.SubscribeState ss, ServiceInfos infos) throws Throwable {
		var current = new HashMap<String, ServiceInfo>();
		for (var pm : infos.getServiceInfoListSortedByIdentity()) {
			AddServer(ss, pm);
			current.put(pm.getPassiveIp() + ":" + pm.getPassivePort(), pm);
		}
		getConfig().ForEachConnector((c) -> current.remove(c.getName()));
		for (var pm : current.values()) {
			RemoveServer(ss, pm);
		}
	}

	@Override
	public void OnSocketAccept(AsyncSocket sender) {
		if (sender.getConnector() == null) {
			// 被动连接等待对方报告信息时再处理。
			// passive connection continue process in ProviderDirect.ProcessAnnounceProviderInfoRequest.
			var ps = new ProviderSession();
			ps.SessionId = sender.getSessionId();
			sender.setUserState(ps); // acceptor
		}
		super.OnSocketAccept(sender);
	}

	@Override
	public void OnHandshakeDone(AsyncSocket socket) throws Throwable {
		// call base
		super.OnHandshakeDone(socket);

		var c = socket.getConnector();
		if (c != null) {
			// 主动连接。
			var ps = (ProviderSession)socket.getUserState();
			ps.SessionId = socket.getSessionId();
			SetRelativeServiceReady(ps, c.getHostNameOrAddress(), c.getPort());

			var r = new AnnounceProviderInfo();
			r.Argument.setIp(ProviderApp.DirectIp);
			r.Argument.setPort(ProviderApp.DirectPort);
			r.Argument.setServerId(ProviderApp.Zeze.getConfig().getServerId());
			r.Send(socket, (_r) -> 0L); // skip result
		}
	}

	synchronized void SetRelativeServiceReady(ProviderSession ps, String ip, int port) {
		ps.ServerLoadIp = ip;
		ps.ServerLoadPort = port;
		// 本机的连接可能设置多次。此时使用已经存在的，忽略后面的。
		if (null != ProviderByLoadName.putIfAbsent(ps.getServerLoadName(), ps))
			return;
		ProviderByServerId.put(ps.getServerId(), ps);

		// 需要把所有符合当前连接目标的Provider相关的服务信息都更新到当前连接的状态。
		for (var ss : getZeze().getServiceManagerAgent().getSubscribeStates().values()) {
			if (ss.getServiceName().startsWith(ProviderApp.ServerServiceNamePrefix)) {
				var infos = ss.getSubscribeType() == SubscribeInfo.SubscribeTypeSimple
						? ss.getServiceInfos() : ss.getServiceInfosPending();
				if (null == infos)
					continue;
				var mid = Integer.parseInt(ss.getServiceName().split("#")[1]);
				var m = ProviderApp.Modules.get(mid);
				for (var server : infos.getServiceInfoListSortedByIdentity()) {
					// 符合当前连接目标。每个Identity标识的服务的(ip,port)必须不一样。
					if (server.getPassiveIp().equals(ip) && server.getPassivePort() == port) {
						SetReady(ss, server, ps, mid, m);
					}
				}
			}
		}
	}

	private void SetReady(Agent.SubscribeState ss, ServiceInfo server, ProviderSession ps, int mid, BModule m) {
		var pms = new ProviderModuleState(ps.getSessionId(), mid, m.getChoiceType(), m.getConfigType());
		ps.GetOrAddServiceReadyState(ss.getServiceName()).put(server.getServiceIdentity(), pms);
		ss.SetServiceIdentityReadyState(server.getServiceIdentity(), pms);
	}

	@Override
	public void OnSocketClose(AsyncSocket socket, Throwable ex) throws Throwable {
		var ps = (ProviderSession)socket.getUserState();
		if (ps != null) {
			for (var service : ps.ServiceReadyStates.entrySet()) {
				var subs = getZeze().getServiceManagerAgent().getSubscribeStates().get(service.getKey());
				for (var identity : service.getValue().keySet()) {
					subs.SetServiceIdentityReadyState(identity, null);
				}
			}
			ProviderByLoadName.remove(ps.getServerLoadName());
			ProviderByServerId.remove(ps.getServerId());
		}
		super.OnSocketClose(socket, ex);
	}

	@Override
	public <P extends Protocol<?>> void DispatchProtocol(P p, ProtocolFactoryHandle<P> factoryHandle) {
		if (p.getTypeId() == ModuleRedirect.TypeId_) {
			if (null != factoryHandle.Handle) {
				var r = (ModuleRedirect)p;
				// 总是不启用存储过程，内部处理redirect时根据Redirect.Handle配置决定是否在存储过程中执行。
				getZeze().getTaskOneByOneByKey().Execute(r.Argument.getHashCode(), () -> Zeze.Util.Task.Call(
						() -> factoryHandle.Handle.handle(p), p, Protocol::trySendResultCode, r.Argument.getMethodFullName()),
						factoryHandle.Mode);
			} else
				logger.warn("Protocol Handle Not Found: {}", p);
			return;
		}
		if (p.getTypeId() == ModuleRedirectAllResult.TypeId_) {
			if (null != factoryHandle.Handle) {
				var r = (ModuleRedirectAllResult)p;
				// 总是不启用存储过程，内部处理redirect时根据Redirect.Handle配置决定是否在存储过程中执行。
				Zeze.Util.Task.run(() -> factoryHandle.Handle.handle(p), p, Protocol::trySendResultCode,
						r.Argument.getMethodFullName(), factoryHandle.Mode);
			} else
				logger.warn("Protocol Handle Not Found: {}", p);
			return;
		}
		// 所有的Direct都不启用存储过程。
		Zeze.Util.Task.run(() -> factoryHandle.Handle.handle(p), p, Protocol::trySendResultCode, factoryHandle.Mode);
		//super.DispatchProtocol(p, factoryHandle);
	}

	@Override
	public <P extends Protocol<?>> void DispatchRpcResponse(
			P rpc, ProtocolHandle<P> responseHandle, ProtocolFactoryHandle<?> factoryHandle) {

		if (rpc.getTypeId() == ModuleRedirect.TypeId_) {
			var redirect = (ModuleRedirect)rpc;
			// 总是不启用存储过程，内部处理redirect时根据Redirect.Handle配置决定是否在存储过程中执行。
			getZeze().getTaskOneByOneByKey().Execute(redirect.Argument.getHashCode(),
					() -> Zeze.Util.Task.Call(() -> responseHandle.handle(rpc), rpc), factoryHandle.Mode);
			return;
		}

		// no procedure.
		Task.run(() -> Task.Call(() -> responseHandle.handle(rpc), rpc),
				"ProviderDirectService.DispatchRpcResponse", factoryHandle.Mode);
		//super.DispatchRpcResponse(rpc, responseHandle, factoryHandle);
	}
}
