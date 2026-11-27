package Zeze.Arch;

import java.util.Objects;

import java.net.ServerSocket;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Application;
import Zeze.Builtin.Provider.BModule;
import Zeze.Builtin.ProviderDirect.AnnounceProviderInfo;
import Zeze.Builtin.ProviderDirect.ModuleRedirect;
import Zeze.Builtin.ProviderDirect.ModuleRedirectAllResult;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolHandle;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.HandshakeBoth;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Util.Action0;
import Zeze.Util.ConcurrentHashSet;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.OutObject;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import Zeze.Util.TaskSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/**
 * Provider之间直连网络管理服务。
 */
public class ProviderDirectService extends HandshakeBoth {
	private static final Logger logger = LogManager.getLogger(ProviderDirectService.class);

	protected @Nullable ProviderApp providerApp;

	protected final ProviderApp requireProviderApp() {
		return Objects.requireNonNull(providerApp, "provider is not attached to an application");
	}
	public final ConcurrentHashMap<String, ProviderSession> providerByLoadName = new ConcurrentHashMap<>();
	public final LongConcurrentHashMap<ProviderSession> providerByServerId = new LongConcurrentHashMap<>();
	private final LongConcurrentHashMap<ConcurrentHashSet<Action0>> serverReadyEvents = new LongConcurrentHashMap<>();

	public ProviderDirectService(String name, Application zeze) {
		super(name, zeze);
	}

	public void removeServer(BServiceInfo pm) {
		lock();
		try {
			//noinspection DataFlowIssue
			var ss = requireProviderApp().zeze.getServiceManager().getSubscribeStates().get(pm.getServiceName());
			if (null != ss) {
				var connName = pm.getPassiveIp() + "_" + pm.getPassivePort();
				var conn = getConfig().findConnector(connName);
				if (conn != null) {
					conn.stop();
					// 按所有权条件删除，避免同 serverId 换地址重启时误删新会话的路由状态。
					var ps = providerByLoadName.get(connName);
					if (ps != null) {
						providerByLoadName.remove(connName, ps);
						providerByServerId.remove(Integer.parseInt(pm.getServiceIdentity()), ps);
						var oldSessionId = ps.getSessionId();
						ss.getLocalStates().computeIfPresent(pm.getServiceIdentity(), (k, v) ->
								v instanceof ProviderModuleState pms && pms.sessionId == oldSessionId ? null : v);
					}
					getConfig().removeConnector(conn);
				}
			}
		} finally {
			unlock();
		}
	}

	public void addServer(BServiceInfo pm) {
		lock();
		try {
			var connName = pm.getPassiveIp() + "_" + pm.getPassivePort();
			var ps = providerByLoadName.get(connName);
			if (ps != null) {
				var prefix = Objects.requireNonNull(requireProviderApp().serverServiceNamePrefix);
				var mid = Integer.parseInt(pm.getServiceName().substring(prefix.length()));
				// 锁序：PDS 锁 -> providerApp 锁，仅锁内读取 modules，setReady 放锁外。
				BModule.Data m;
				requireProviderApp().lock();
				try {
					m = requireProviderApp().modules.get(mid);
				} finally {
					requireProviderApp().unlock();
				}
				if (m != null)
					setReady(pm.getServiceName(), pm, ps, mid, m);
				else
					logger.error("addServer: not found module: {}", pm.getServiceName());
				return;
			}
			if (pm.getServiceIdentity().startsWith("@")) // from linkd
				return;
			var serverId = Integer.parseInt(pm.getServiceIdentity());
			//noinspection DataFlowIssue
			if (serverId < getZeze().getConfig().getServerId())
				return;
			if (serverId == getZeze().getConfig().getServerId()) {
				setRelativeServiceReady(newSession(serverId), requireProviderApp().directIp, requireProviderApp().directPort);
				return;
			}
			var out = new OutObject<Connector>();
			if (getConfig().tryGetOrAddConnector(pm.getPassiveIp(), pm.getPassivePort(), true, out)) {
				// 新建的Connector。开始连接。
				var template = newSession(serverId);
				template.timeCounter.close(); // Connector只保存稳定身份模板，不持有活跃连接的周期计数。
				//noinspection DataFlowIssue
				out.value.userState = template;
				out.value.start();
			}
		} finally {
			unlock();
		}
	}

	@SuppressWarnings("MethodMayBeStatic")
	public ProviderSession newSession(int serverId) {
		var session = new ProviderSession();
		session.serverId = serverId;
		return session;
	}

	@SuppressWarnings("MethodMayBeStatic")
	public ProviderSession newSession(AsyncSocket so) {
		var session = new ProviderSession();
		session.sessionId = so.getSessionId();
		return session;
	}

	@Override
	public void OnSocketConnected(AsyncSocket so) {
		if (so.getConnector() != null) {
			// 为每条连接新建会话，Connector 模板不充当可变会话。
			var template = (ProviderSession)so.getUserState();
			//noinspection DataFlowIssue
			var session = newSession(template.getServerId());
			session.sessionId = so.getSessionId();
			so.setUserState(session);
			// close 可能先于替换发生，此处补清理避免新会话残留。
			if (so.isClosed()) {
				session.timeCounter.close();
				return;
			}
		}
		super.OnSocketConnected(so);
	}

	@Override
	public void OnSocketAccept(AsyncSocket so) {
		if (so.getConnector() == null) {
			// 被动连接等待对方发送 AnnounceProviderInfo 后再设置就绪状态。
			so.setUserState(newSession(so)); // acceptor
		}
		super.OnSocketAccept(so);
	}

	@Override
	public void OnHandshakeDone(AsyncSocket so) throws Exception {
		super.OnHandshakeDone(so);

		var c = so.getConnector();
		if (c != null) {
			// 主动连接。
			var ps = (ProviderSession)so.getUserState();
			setRelativeServiceReady(Objects.requireNonNull(ps), c.getHostNameOrAddress(), c.getPort());

			var r = new AnnounceProviderInfo();
			r.Argument.setIp(Objects.requireNonNull(requireProviderApp().directIp));
			r.Argument.setPort(requireProviderApp().directPort);
			r.Argument.setServerId(requireProviderApp().zeze.getConfig().getServerId());
			r.Send(so, (_r) -> 0L); // skip result
		}
	}

	public void waitDirectServerReady(int serverId) {
		waitDirectServerReady(serverId, 3000);
	}

	public void waitDirectServerReady(int serverId, long timeout) {
		var future = new TaskCompletionSource<Long>();
		Action0 callback = () -> future.setResult(0L);
		try {
			waitDirectServerReady(serverId, callback);
			future.await(timeout);
		} finally {
			//noinspection DataFlowIssue
			serverReadyEvents.computeIfAbsent(serverId, __ -> new ConcurrentHashSet<>()).remove(callback);
		}
	}

	public void waitDirectServerReady(int serverId, Action0 callback) {
		lock();
		try {
			var ps = providerByServerId.get(serverId);
			var socket = ps != null ? GetSocket(ps.getSessionId()) : null;
			//noinspection DataFlowIssue
			boolean local = ps != null && ps.getSessionId() == 0
				&& serverId == getZeze().getConfig().getServerId();
			if (!local && (socket == null || socket.isClosed())) {
				//noinspection DataFlowIssue
				serverReadyEvents.computeIfAbsent(serverId, __ -> new ConcurrentHashSet<>()).add(callback);
				return;
			}
		} finally {
			unlock();
		}
		try {
			callback.run(); // 锁外回调，避免死锁风险。
		} catch (Exception e) {
			throw Task.forceThrow(e);
		}
	}

	// 由于sm的服务信息是碎片传递给订阅者的，所以本质上得到的快照在启动的时候几乎总是不完整的，
	// 为了得到真正所有的服务器信息，只有sleep。因此不提供waitAllDirectServerReady。

	// under lock
	private void notifyServerReady(int serverId) {
		var watchers = serverReadyEvents.computeIfAbsent(serverId, __ -> new ConcurrentHashSet<>());
		//noinspection DataFlowIssue
		for (var w : watchers) {
			try {
				w.run();
			} catch (Exception ex) {
				logger.error("", ex);
			}
		}
		watchers.clear();
	}

	void setRelativeServiceReady(ProviderSession ps, @Nullable String ip, int port) {
		lock();
		try {
			if (ps.directClosed)
				return;
			ps.serverLoadIp = ip;
			ps.serverLoadPort = port;
			// 本机的连接可能设置多次。此时使用已经存在的，忽略后面的。
			var old = providerByLoadName.putIfAbsent(ps.getServerLoadName(), ps);
			if (old != null) {
				if (old == ps)
					return;
				// 同 loadName 接管：先更新映射再异步关闭旧连接。
				//noinspection DataFlowIssue
				if (ps.getServerId() != getZeze().getConfig().getServerId())
					logger.warn("setRelativeServiceReady: supersede old session {} with new {} for {}",
							old, ps, ps.getServerLoadName());
				providerByLoadName.put(ps.getServerLoadName(), ps);
				// 终结旧会话 TimeCounter（含无连接的本机合成会话），并异步关闭旧连接。
				old.timeCounter.close();
				var oldSocket = old.getSessionId() != 0 ? GetSocket(old.getSessionId()) : null;
				if (oldSocket != null)
					TaskSpec.ofAction(() -> oldSocket.close(new java.io.IOException(
							"superseded by new direct connection, loadName=" + ps.getServerLoadName()))).runNow();
			}
			providerByServerId.put(ps.getServerId(), ps);

			// 需要把所有符合当前连接目标的Provider相关的服务信息都更新到当前连接的状态。
			//noinspection DataFlowIssue
			for (var ss : getZeze().getServiceManager().getSubscribeStates().values()) {
				var prefix = Objects.requireNonNull(requireProviderApp().serverServiceNamePrefix);
				if (ss.getServiceName().startsWith(prefix)) {
					// direct 会话不携带 appVersion，统一按本机主版本查询。
					var infos = ss.getServiceInfos(getZeze().getConfig().getAppMainVersion());
					if (infos == null)
						continue;
					var mid = Integer.parseInt(ss.getServiceName().substring(prefix.length()));
					BModule.Data m;
					requireProviderApp().lock();
					try {
						m = requireProviderApp().modules.get(mid);
					} finally {
						requireProviderApp().unlock();
					}
					if (m == null) {
						logger.error("setRelativeServiceReady: not found module: {}", ss.getServiceName());
						continue;
					}
					for (var server : infos.getSortedIdentities()) {
						// 符合当前连接目标。每个Identity标识的服务的(ip,port)必须不一样。
						if (server.getPassiveIp().equals(ip) && server.getPassivePort() == port) {
							setReady(ss.getServiceName(), server, ps, mid, m);
						}
					}
				}
			}
			// 最后才通知成功。
			notifyServerReady(ps.getServerId());
		} finally {
			unlock();
		}
	}

	private void setReady(String serviceName, BServiceInfo server, ProviderSession ps,
						  int mid, BModule.Data m) {
		//noinspection DataFlowIssue
		var ss = requireProviderApp().zeze.getServiceManager().getSubscribeStates().get(serviceName);
		if (null != ss) {
			var pms = new ProviderModuleState(ps.getSessionId(), mid, m.getChoiceType(), m.isDynamic());
			ps.getOrAddServiceReadyState(serviceName).put(server.getServiceIdentity(), pms);
			ss.setIdentityLocalState(server.getServiceIdentity(), pms);
		}
	}

	@Override
	public void OnSocketClose(AsyncSocket socket, @Nullable Throwable ex) throws Exception {
		var ps = (ProviderSession)socket.getUserState();
		// 与 setRelativeServiceReady/addServer 互斥，防止已关闭会话被迟到的握手写回。
		lock();
		try {
			if (ps != null) {
				ps.directClosed = true;
				for (var service : ps.ServiceReadyStates.entrySet()) {
					//noinspection DataFlowIssue
					var subs = getZeze().getServiceManager().getSubscribeStates().get(service.getKey());
					if (subs == null)
						continue; // 服务已取消订阅
					for (var stateEntry : service.getValue().entrySet()) {
						// 仅移除仍属于当前会话的 localState，避免误删重连新会话。
						subs.getLocalStates().remove(stateEntry.getKey(), stateEntry.getValue());
					}
				}
				// 条件删除，避免迟到的关闭回调误删新会话注册。
				providerByLoadName.remove(ps.getServerLoadName(), ps);
				providerByServerId.remove(ps.getServerId(), ps);
				// 取消 TimeCounter 周期任务，避免会话更替泄漏。
				ps.timeCounter.close();
			}
		} finally {
			unlock();
			super.OnSocketClose(socket, ex);
		}
	}

	@Override
	public void dispatchProtocol(long typeId, ByteBuffer bb, ProtocolFactoryHandle<?> factoryHandle,
								 @Nullable AsyncSocket so) throws Exception {
		var p = decodeProtocol(typeId, bb, factoryHandle, so);
		p.dispatch(this, factoryHandle);
	}

	@Override
	public void dispatchProtocol(Protocol<?> p, ProtocolFactoryHandle<?> factoryHandle)
			throws Exception {
		if (p.getTypeId() == ModuleRedirect.TypeId_) {
			var r = (ModuleRedirect)p;
			// 总是不启用存储过程，内部处理redirect时根据Redirect.Handle配置决定是否在存储过程中执行。
			if (r.Argument.isNoOneByOne()) {
				TaskSpec.ofFunc(() -> p.handle(this, factoryHandle), p, (__, code) -> p.trySendResultCode(code))
						.name(r.Argument.getMethodFullName()).dispatchMode(factoryHandle.Mode).runNow();
			} else {
				TaskSpec.ofFunc(() -> p.handle(this, factoryHandle), p, (__, code) -> p.trySendResultCode(code))
						.name(r.Argument.getMethodFullName())
						.dispatchMode(factoryHandle.Mode)
						.executeOneByOne(r.Argument.getKey());
			}
			return;
		}
		if (p.getTypeId() == ModuleRedirectAllResult.TypeId_) {
			var r = (ModuleRedirectAllResult)p;
			// 总是不启用存储过程，内部处理redirect时根据Redirect.Handle配置决定是否在存储过程中执行。
			TaskSpec.ofFunc(() -> p.handle(this, factoryHandle), p, (__, code) -> p.trySendResultCode(code))
					.name(r.Argument.getMethodFullName()).dispatchMode(factoryHandle.Mode).runNow();
			return;
		}
		// 所有的Direct都不启用存储过程。
		TaskSpec.ofFunc(() -> p.handle(this, factoryHandle), p, (__, code) -> p.trySendResultCode(code))
				.dispatchMode(factoryHandle.Mode).runNow();
	}

	@Override
	public <P extends Protocol<?>> void dispatchRpcResponse(P rpc, ProtocolHandle<P> responseHandle,
															ProtocolFactoryHandle<?> factoryHandle) {
		if (rpc.getTypeId() == ModuleRedirect.TypeId_) {
			var r = (ModuleRedirect)rpc;
			// 总是不启用存储过程，内部处理redirect时根据Redirect.Handle配置决定是否在存储过程中执行。
			if (r.Argument.isNoOneByOne())
				TaskSpec.ofFunc(() -> responseHandle.handle(rpc), rpc)
						.dispatchMode(factoryHandle.Mode).runNow();
			else {
				TaskSpec.ofFunc(() -> responseHandle.handle(rpc), rpc)
						.dispatchMode(factoryHandle.Mode)
						.executeOneByOne(r.Argument.getKey());
			}
			return;
		}

		// no procedure.
		TaskSpec.ofFunc(() -> responseHandle.handle(rpc), rpc).dispatchMode(factoryHandle.Mode).runNow();
	}

	@Override
	public void onServerSocketBind(ServerSocket ss) {
		requireProviderApp().directPort = ss.getLocalPort();
	}
}
