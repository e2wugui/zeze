package Zeze.Services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.Future;
import Zeze.Builtin.ServiceManagerWithRaft.*;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolHandle;
import Zeze.Raft.Raft;
import Zeze.Raft.RaftConfig;
import Zeze.Raft.RocksRaft.Procedure;
import Zeze.Raft.RocksRaft.Rocks;
import Zeze.Raft.RocksRaft.RocksMode;
import Zeze.Raft.RocksRaft.Table;
import Zeze.Raft.Server;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Services.ServiceManager.BServiceInfosVersion;
import Zeze.Services.ServiceManager.BSubscribeInfo;
import Zeze.Transaction.DispatchMode;
import Zeze.Util.Action0;
import Zeze.Util.FuncLong;
import Zeze.Util.KV;
import Zeze.Util.PerfCounter;
import Zeze.Util.Random;
import Zeze.Util.Task;
import Zeze.Util.TaskOneByOneByKey;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

public final class ServiceManagerWithRaft extends AbstractServiceManagerWithRaft implements AutoCloseable {
	static {
		var level = Level.toLevel(System.getProperty("logLevel"), Level.INFO);
		((LoggerContext)LogManager.getContext(false)).getConfiguration().getRootLogger().setLevel(level);
	}

	private static final Logger logger = LogManager.getLogger(ServiceManagerWithRaft.class);
	private final Rocks rocks;
	private final Table<String, BAutoKey> tableAutoKey;
	private final Table<String, BSession> tableSession;
	private final Table<String, BLoadObservers> tableLoadObservers;
	private final Table<String, BServerState> tableServerState;
	private final HashMap<Integer, Future<?>> offlineNotifyFutures = new HashMap<>();

	// 需要从配置文件中读取，把这个引用加入：Zeze.Config.AddCustomize
	private final ServiceManagerServer.Conf conf = new ServiceManagerServer.Conf();

	public ServiceManagerWithRaft(String raftName, RaftConfig raftConf) throws Exception {
		this(raftName, raftConf, Config.load(), false);
	}

	public ServiceManagerWithRaft(String raftName, RaftConfig raftConf, Config config,
								  boolean RocksDbWriteOptionSync) throws Exception {
		PerfCounter.instance.tryStartScheduledLog();

		if (null == config)
			config = Config.load();
		config.parseCustomize(conf);

		rocks = new Rocks(raftName, RocksMode.Pessimism, raftConf, config, RocksDbWriteOptionSync,
				SMServer::new, new TaskOneByOneByKey());

		RegisterRocksTables(rocks);
		RegisterProtocols(rocks.getRaft().getServer());
		rocks.getRaft().getServer().start();

		tableAutoKey = rocks.<String, BAutoKey>getTableTemplate("tAutoKey").openTable();
		tableSession = rocks.<String, BSession>getTableTemplate("tSession").openTable();
		tableLoadObservers = rocks.<String, BLoadObservers>getTableTemplate("tLoadObservers").openTable();
		tableServerState = rocks.<String, BServerState>getTableTemplate("tServerState").openTable();
	}

	@Override
	public void close() {
		rocks.close();
	}

	/**
	 * 所有Raft网络层收到的请求和Rpc的结果，全部加锁，直接运行。
	 * 这样整个程序就单线程化了。
	 */
	public class SMServer extends Server {
		public SMServer(Raft raft, String name, Config config) {
			super(raft, name, config);
		}

		@Override
		public <P extends Protocol<?>> void dispatchRaftRpcResponse(P rpc, ProtocolHandle<P> responseHandle,
																	ProtocolFactoryHandle<?> factoryHandle) {
			lock();
			try {
				if (logger.isDebugEnabled())
					logger.debug("dispatchRaftRpcResponse: {}{}", rpc.getClass().getName(), rpc);
				var procedure = rocks.newProcedure(() -> responseHandle.handle(rpc));
				Task.call(procedure::call, rpc);
			} finally {
				unlock();
			}
		}

		@Override
		public void dispatchRaftRequest(Protocol<?> p, FuncLong func, String name, Action0 cancel,
										DispatchMode mode) {
			lock();
			try {
				if (logger.isDebugEnabled()) {
					var netSession = (Session)p.getSender().getUserState();
					var ssName = null != netSession ? netSession.name : "";
					logger.debug("dispatchRaftRequest: {}@{}{}", p.getClass().getName(), ssName, p);
				}
				var procedure = new Procedure(rocks, func);
				Task.call(procedure::call, p, Protocol::SendResultCode);
			} finally {
				unlock();
			}
		}

		@Override
		public void OnSocketClose(AsyncSocket so, Throwable e) throws Exception {
			var netSession = (Session)so.getUserState();
			if (null != netSession) {
				if (logger.isDebugEnabled())
					logger.info("OnSocketClose: {}", netSession.name);
				lock();
				try {
					var procedure = rocks.newProcedure(() -> {
						netSession.onClose();
						return 0;
					});
					procedure.call();
				} finally {
					unlock();
				}
			}
			super.OnSocketClose(so, e);
		}
	}

	/*
	private static BSubscribeInfo fromRocks(BSubscribeInfoRocks rocks) {
		return new BSubscribeInfo(rocks.getServiceName(), rocks.getVersion());
	}
	*/

	public class Session {
		private final String name;
		private final long sessionId;
		private final Future<?> keepAliveTimerTask;
		public static final long eOfflineNotifyDelay = 600 * 1000;

		public Session(String name, long sessionId) {
			this.name = name;
			this.sessionId = sessionId;

			if (conf.keepAlivePeriod > 0) {
				keepAliveTimerTask = Task.scheduleUnsafe(
						Random.getInstance().nextInt(conf.keepAlivePeriod),
						conf.keepAlivePeriod,
						() -> {
							AsyncSocket s = null;
							try {
								s = rocks.getRaft().getServer().GetSocket(sessionId);
								var r = new KeepAlive();
								r.SendAndWaitCheckResultCode(s);
							} catch (Throwable ex) { // logger.error
								if (s != null)
									s.close(ex);
								else
									logger.error("ServiceManager.KeepAlive", ex);
							}
						});
			} else
				keepAliveTimerTask = null;
		}

		public void onClose() {
			if (keepAliveTimerTask != null)
				keepAliveTimerTask.cancel(false);

			var session = tableSession.get(name);
			if (null != session) {
				for (var info : session.getSubscribes().values())
					unSubscribeNow(name, info.getServiceName());

				var notifies = new HashMap<AsyncSocket, Edit>();
				for (var unReg : session.getRegisters().values()) {
					var state = tableServerState.get(unReg.getServiceName());
					if (state != null) {
						var versions = state.getServiceInfosVersion().get(unReg.getVersion());
						if (null != versions) {
							var exist = versions.getServiceInfos().get(unReg.getServiceIdentity());
							versions.getServiceInfos().remove(unReg.getServiceIdentity());
							if (exist != null && exist.getSessionName().equals(name)) {
								// 有可能当前连接没有注销，新的注册已经AddOrUpdate，此时忽略当前连接的注销。
								removeAndCollectNotify(state, fromRocks(exist), notifies);
							}
						}
					}
				}
				ServiceManagerWithRaft.sendNotifies(notifies);

				// offline notify，开启一个线程执行，避免互等造成麻烦。
				// 这个操作不能cancel，即使Server重新起来了，通知也会进行下去。
				var serverId = session.getOfflineRegisterServerId();
				if (serverId >= 0) {
					if (!offlineNotifyFutures.containsKey(serverId))
						offlineNotifyFutures.put(serverId, Task.scheduleUnsafe(eOfflineNotifyDelay,
								() -> offlineNotify(session, true)));
				} else {
					Task.run(() -> offlineNotify(session, false), "offlineNotifyImmediately");
				}
			}
			tableSession.remove(name);
		}

		private void tryNotifyOffline(OfflineNotify notify, BSession session, BOfflineNotifyRocks notifyId, HashSet<Session> skips) {
			var selected = randomFor(session, notifyId.getNotifyId(), skips);
			if (selected == null)
				return; // 没有找到可用的通知对象，放弃通知。

			notify.Send(selected.getValue(), (This) -> {
				logger.info("OfflineNotify: serverId={} notifyId={} selectSessionId={} resultCode={}",
						session.getOfflineRegisterServerId(), notifyId, selected.getKey().sessionId, notify.getResultCode());
				if (notify.getResultCode() != 0)
					tryNotifyOffline(notify, session, notifyId, skips);
				return 0;
			});

			// 保存这一次通知失败session，下一次尝试选择的时候忽略。
			skips.add(selected.getKey());
		}

		private void offlineNotify(BSession session, boolean delay) {
			var serverId = session.getOfflineRegisterServerId();
			if (delay && null == offlineNotifyFutures.remove(serverId))
				return; // 此serverId的新连接已经连上或者通知已经执行。

			logger.info("OfflineNotify: serverId={} notifyIds={} begin",
					serverId, session.getOfflineRegisterNotifies());
			for (var notifyId : session.getOfflineRegisterNotifies().values()) {
				var skips = new HashSet<Session>();

				var notify = new OfflineNotify();
				var netNotifyId = notify.Argument;
				netNotifyId.serverId = notifyId.getServerId();
				netNotifyId.notifyId = notifyId.getNotifyId();
				netNotifyId.notifySerialId = notifyId.getNotifySerialId();
				netNotifyId.notifyContext = notifyId.getNotifyContext();

				tryNotifyOffline(notify, session, notifyId, skips);
			}
			logger.info("OfflineNotify: serverId={} end", session.getOfflineRegisterServerId());
		}

		// 从注册了这个notifyId的其他session中随机选择一个。【实际实现是从连接里面按顺序挑选的】
		private KV<Session, AsyncSocket> randomFor(BSession session, String notifyId, HashSet<Session> skips) {
			var sessions = new ArrayList<KV<Session, AsyncSocket>>();
			try {
				rocks.getRaft().getServer().foreach(socket -> {
					var netSession = (Session)socket.getUserState();
					if (netSession != null && netSession != this && !skips.contains(netSession)) {
						if (session.getOfflineRegisterNotifies().containsKey(notifyId))
							sessions.add(KV.create(netSession, socket));
					}
				});
			} catch (Exception e) {
				Task.forceThrow(e);
			}
			if (sessions.isEmpty())
				return null;
			return sessions.get(Random.getInstance().nextInt(sessions.size()));
		}
	}

	@Override
	protected long ProcessLoginRequest(Login r) {
		var session = tableSession.getOrAdd(r.Argument.getSessionName());
		r.getSender().setUserState(new Session(r.Argument.getSessionName(), r.getSender().getSessionId()));
		session.setSessionId(r.getSender().getSessionId());
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessAllocateIdRequest(AllocateId r) {
		r.Result.setName(r.Argument.getName());
		var autoKey = tableAutoKey.getOrAdd(r.Argument.getName());

		r.Result.setStartId(autoKey.getCurrent());

		// 随便修正一下分配数量。
		var count = r.Argument.getCount();
		if (count < 256)
			count = 256;
		else if (count > 10000)
			count = 10000;

		long current = autoKey.getCurrent() + count;
		autoKey.setCurrent(current);
		r.Result.setCount(count);

		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessOfflineRegisterRequest(OfflineRegister r) {
		logger.info("{}: OfflineRegister serverId={} notifyId={}",
				r.getSender(), r.Argument.serverId, r.Argument.notifyId);
		var netSession = (Session)r.getSender().getUserState();
		var session = tableSession.get(netSession.name);
		// 允许重复注册：简化server注册逻辑。
		session.setOfflineRegisterServerId(r.Argument.serverId);

		var bOfflineNotifyRocks = new BOfflineNotifyRocks();
		bOfflineNotifyRocks.setServerId(r.Argument.serverId);
		bOfflineNotifyRocks.setNotifyContext(r.Argument.notifyContext);
		bOfflineNotifyRocks.setNotifyId(r.Argument.notifyId);
		bOfflineNotifyRocks.setNotifySerialId(r.Argument.notifySerialId);
		session.getOfflineRegisterNotifies().put(r.Argument.notifyId, bOfflineNotifyRocks);

		var future = offlineNotifyFutures.remove(r.Argument.serverId);
		if (null != future)
			future.cancel(true);

		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessNormalCloseRequest(NormalClose r) {
		var netSession = (Session)r.getSender().getUserState();
		var session = tableSession.get(netSession.name);
		// 正常不关闭不执行异常下线通知。
		session.getOfflineRegisterNotifies().clear();
		r.SendResult();
		return 0;
	}

	private void addLoadObserver(String ip, int port, String sessionName) {
		if (!ip.isEmpty() && port != 0) {
			var loadObservers = tableLoadObservers.getOrAdd(ip + "_" + port);
			loadObservers.getObservers().add(sessionName);
		}
	}

	@Override
	protected long ProcessSetServerLoadRequest(SetServerLoad r) {
		var loadObservers = tableLoadObservers.getOrAdd(r.Argument.ip + "_" + r.Argument.port);
		var observers = loadObservers.getObservers();

		var set = new SetServerLoad();
		set.Argument = r.Argument;

		ArrayList<String> removed = null;
		for (var observer : observers) {
			try {
				var session = tableSession.get(observer);
				if (null != session && set.Send(rocks.getRaft().getServer().GetSocket(session.getSessionId())))
					continue;
			} catch (Throwable ignored) { // ignored
			}
			if (removed == null)
				removed = new ArrayList<>();
			removed.add(observer);
		}
		if (removed != null) {
			for (var remove : removed)
				observers.remove(remove);
		}
		r.SendResult();
		return 0;
	}

	private static BServiceInfoRocks toRocks(BServiceInfo serverInfo, String sessionName) {
		return new BServiceInfoRocks(serverInfo.getServiceName(), serverInfo.getServiceIdentity(),
				serverInfo.getPassiveIp(), serverInfo.getPassivePort(), serverInfo.getExtraInfo(),
				sessionName, serverInfo.getVersion());
	}

	private static BServiceInfoKeyRocks toRocksKey(BServiceInfo serverInfo) {
		return new BServiceInfoKeyRocks(serverInfo.getServiceName(), serverInfo.getServiceIdentity());
	}

	private static BSubscribeInfoRocks toRocks(BSubscribeInfo si) {
		return new BSubscribeInfoRocks(si.getServiceName(), si.getVersion());
	}

	private static void sendNotifies(HashMap<AsyncSocket, Edit> notifies) {
		// todo 增加一些发送错误的日志。
		for (var e : notifies.entrySet()) {
			e.getValue().Send(e.getKey());
		}
	}

	@Override
	protected long ProcessEditRequest(Edit r) {
		var netSession = (Session)r.getSender().getUserState();
		var notifies = new HashMap<AsyncSocket, Edit>();

		// step 1: remove
		for (var unReg : r.Argument.getRemove()) {
			var state = tableServerState.get(unReg.getServiceName());
			if (state != null) {
				var versions = state.getServiceInfosVersion().get(unReg.getVersion());
				if (null != versions) {
					var exist = versions.getServiceInfos().get(unReg.getServiceIdentity());
					versions.getServiceInfos().remove(unReg.getServiceIdentity());
					if (exist != null && exist.getSessionName().equals(netSession.name)) {
						// 有可能当前连接没有注销，新的注册已经AddOrUpdate，此时忽略当前连接的注销。
						removeAndCollectNotify(state, fromRocks(exist), notifies);
					}
				}
			}
			var session = tableSession.get(netSession.name);
			session.getRegisters().remove(toRocksKey(unReg)); // ignore remove failed
		}

		// step 2: add
		for (var reg : r.Argument.getAdd()) {
			var session = tableSession.get(netSession.name);
			// 允许重复登录，断线重连Agent不好原子实现重发。
			session.getRegisters().put(toRocksKey(reg), toRocks(reg, netSession.name));
			var state = tableServerState.getOrAdd(reg.getServiceName());
			if (!state.getServiceName().equals(reg.getServiceName()))
				state.setServiceName(reg.getServiceName());
			var versions = state.getServiceInfosVersion().get(reg.getVersion());
			if (null == versions)
				state.getServiceInfosVersion().put(reg.getVersion(), versions = new BServiceInfosVersionRocks());
			// AddOrUpdate，否则重连重新注册很难恢复到正确的状态。
			versions.getServiceInfos().put(reg.getServiceIdentity(), toRocks(reg, netSession.name));
			addAndCollectNotify(state, reg, notifies);
		}

		sendNotifies(notifies);
		r.SendResult();
		return 0;
	}

	private void addAndCollectNotify(BServerState state, BServiceInfo info, HashMap<AsyncSocket, Edit> notifies) {
		for (var e : state.getSimple().entrySet()) {
			var subVersion = e.getValue().getVersion();
			if (subVersion == 0 || subVersion == info.getVersion()) {
				var sessionName = e.getKey();
				var session = tableSession.get(sessionName);
				if (null == session)
					continue;
				var peer = rocks.getRaft().getServer().GetSocket(session.getSessionId());
				if (null == peer)
					continue;

				var notify = notifies.computeIfAbsent(peer, __ -> new Edit());
				notify.Argument.getAdd().add(info);
			}
		}
	}

	@Override
	protected long ProcessSubscribeRequest(Subscribe r) {
		logger.info("{}: Subscribe {}", r.getSender(), r.Argument);
		var netSession = (Session)r.getSender().getUserState();
		var session = tableSession.get(netSession.name);
		for (var info : r.Argument.subs) {
			session.getSubscribes().put(info.getServiceName(), toRocks(info));
			var state = tableServerState.getOrAdd(info.getServiceName());
			if (!state.getServiceName().equals(info.getServiceName()))
				state.setServiceName(info.getServiceName());
			subscribeAndCollect(state, r, info, netSession.name);
		}
		r.SendResult();
		return 0;
	}

	private static BServiceInfo fromRocks(BServiceInfoRocks rocks) {
		return new BServiceInfo(rocks.getServiceName(), rocks.getServiceIdentity(),
				rocks.getVersion(),
				rocks.getPassiveIp(), rocks.getPassivePort(), rocks.getExtraInfo());
	}

	public void removeAndCollectNotify(BServerState state, BServiceInfo info, HashMap<AsyncSocket, Edit> notifies) {
		for (var e : state.getSimple().entrySet()) {
			var subVersion = e.getValue().getVersion();
			if (subVersion == 0 || subVersion == info.getVersion()) {
				var sessionName = e.getKey();
				var session = tableSession.get(sessionName);
				if (null == session)
					continue;
				var peer = rocks.getRaft().getServer().GetSocket(session.getSessionId());
				if (null == peer)
					continue;

				var notify = notifies.computeIfAbsent(peer, __ -> new Edit());
				notify.Argument.getRemove().add(info);
			}
		}
	}

	@Override
	protected long ProcessUnSubscribeRequest(UnSubscribe r) {
		logger.info("{}: UnSubscribe {}", r.getSender(), r.Argument);
		var netSession = (Session)r.getSender().getUserState();
		var session = tableSession.get(netSession.name);
		for (var serviceName : r.Argument.serviceNames) {
			var sub = session.getSubscribes().get(serviceName);
			session.getSubscribes().remove(serviceName);
			if (sub != null) {
				unSubscribeNow(netSession.name, serviceName);
			}
		}
		r.SendResult();
		return 0;
	}

	public BServerState unSubscribeNow(String sessionName, String serviceName) {
		var state = tableServerState.get(serviceName);
		if (state != null) {
			var removed = state.getSimple().get(sessionName);
			state.getSimple().remove(sessionName);
			if (removed != null)
				return state;
		}
		return null;
	}

	private void subscribeAndCollect(BServerState state, Subscribe r, BSubscribeInfo subInfo, String ssName) {
		// 外面会话的 TryAdd 加入成功，下面TryAdd肯定也成功。
		state.getSimple().put(ssName, toRocks(subInfo));
		r.Result.map.put(state.getServiceName(), new BServiceInfosVersion(subInfo.getVersion(), state));

		var netSession = (Session)r.getSender().getUserState();
		for (var versions : state.getServiceInfosVersion().values())
			for (var info : versions.getServiceInfos().values())
				addLoadObserver(info.getPassiveIp(), info.getPassivePort(), netSession.name);
	}
}
