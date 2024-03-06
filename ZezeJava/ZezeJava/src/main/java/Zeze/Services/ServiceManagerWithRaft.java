package Zeze.Services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.concurrent.Future;
import Zeze.Builtin.ServiceManagerWithRaft.*;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolHandle;
import Zeze.Raft.Raft;
import Zeze.Raft.RaftConfig;
import Zeze.Raft.RocksRaft.CollMap2;
import Zeze.Raft.RocksRaft.Procedure;
import Zeze.Raft.RocksRaft.Rocks;
import Zeze.Raft.RocksRaft.RocksMode;
import Zeze.Raft.RocksRaft.Table;
import Zeze.Raft.Server;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Services.ServiceManager.BServiceInfos;
import Zeze.Services.ServiceManager.BSubscribeInfo;
import Zeze.Transaction.DispatchMode;
import Zeze.Util.Action0;
import Zeze.Util.FuncLong;
import Zeze.Util.KV;
import Zeze.Util.PerfCounter;
import Zeze.Util.Random;
import Zeze.Util.Task;
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
	private final HashMap<String, Future<?>> notifyTimeoutTasks = new HashMap<>();

	private Future<?> startNotifyDelayTask;
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

		rocks = new Rocks(raftName, RocksMode.Pessimism, raftConf, config, RocksDbWriteOptionSync, SMServer::new);
		RegisterRocksTables(rocks);
		RegisterProtocols(rocks.getRaft().getServer());
		rocks.getRaft().getServer().start();

		tableAutoKey = rocks.<String, BAutoKey>getTableTemplate("tAutoKey").openTable();
		tableSession = rocks.<String, BSession>getTableTemplate("tSession").openTable();
		tableLoadObservers = rocks.<String, BLoadObservers>getTableTemplate("tLoadObservers").openTable();
		tableServerState = rocks.<String, BServerState>getTableTemplate("tServerState").openTable();

		if (this.conf.startNotifyDelay > 0) {
			startNotifyDelayTask = Task.scheduleUnsafe(this.conf.startNotifyDelay, () -> {
				startNotifyDelayTask = null;
				tableServerState.walk((__, state) -> {
					rocks.newProcedure(() -> {
						startReadyCommitNotify(state, true);
						return 0;
					}).call();
					return true;
				});
			});
		}
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
		public synchronized <P extends Protocol<?>> void dispatchRaftRpcResponse(P rpc, ProtocolHandle<P> responseHandle,
																				 ProtocolFactoryHandle<?> factoryHandle) {
			if (logger.isDebugEnabled())
				logger.debug("dispatchRaftRpcResponse: " + rpc.getClass().getName() + rpc);
			var procedure = rocks.newProcedure(() -> responseHandle.handle(rpc));
			Task.call(procedure::call, rpc);
		}

		@Override
		public synchronized void dispatchRaftRequest(Protocol<?> p, FuncLong func, String name, Action0 cancel,
													 DispatchMode mode) {
			if (logger.isDebugEnabled()) {
				var netSession = (Session)p.getSender().getUserState();
				var ssName = null != netSession ? netSession.name : "";
				logger.debug("dispatchRaftRequest: " + p.getClass().getName() + "@" + ssName + p);
			}
			var procedure = new Procedure(rocks, func);
			Task.call(procedure::call, p, Protocol::SendResultCode);
		}

		@Override
		public void OnSocketClose(AsyncSocket so, Throwable e) throws Exception {
			var netSession = (Session)so.getUserState();
			if (null != netSession) {
				if (logger.isDebugEnabled())
					logger.info("OnSocketClose: " + netSession.name);
				synchronized (this) {
					var procedure = rocks.newProcedure(() -> {
						netSession.onClose();
						return 0;
					});
					procedure.call();
				}
			}
			super.OnSocketClose(so, e);
		}
	}

	private static BSubscribeInfo fromRocks(BSubscribeInfoRocks rocks) {
		var r = new BSubscribeInfo();
		r.setServiceName(rocks.getServiceName());
		r.setSubscribeType(rocks.getSubscribeType());
		return r;
	}

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
					unSubscribeNow(name, fromRocks(info));

				var changed = new HashMap<String, BServerState>(session.getRegisters().size());
				for (var info : session.getRegisters()) {
					var state = unRegisterNow(name, fromRocks(info.getValue()));
					if (state != null)
						changed.putIfAbsent(state.getServiceName(), state);
				}
				for (var state : changed.values())
					startReadyCommitNotify(state);

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
				sessionName);
	}

	private static BServiceInfoKeyRocks toRocksKey(BServiceInfo serverInfo) {
		return new BServiceInfoKeyRocks(serverInfo.getServiceName(), serverInfo.getServiceIdentity());
	}

	private static BSubscribeInfoRocks toRocks(BSubscribeInfo si) {
		return new BSubscribeInfoRocks(si.getServiceName(), si.getSubscribeType());
	}

	@Override
	protected long ProcessRegisterRequest(Register r) {
		var netSession = (Session)r.getSender().getUserState();
		var session = tableSession.get(netSession.name);
		// 允许重复登录，断线重连Agent不好原子实现重发。
		session.getRegisters().put(toRocksKey(r.Argument), toRocks(r.Argument, netSession.name));
		var state = tableServerState.getOrAdd(r.Argument.getServiceName());
		if (!state.getServiceName().equals(r.Argument.getServiceName()))
			state.setServiceName(r.Argument.getServiceName());
		// AddOrUpdate，否则重连重新注册很难恢复到正确的状态。
		state.getServiceInfos().put(r.Argument.getServiceIdentity(), toRocks(r.Argument, netSession.name));
		startReadyCommitNotify(state);
		notifySimpleOnRegister(state, r.Argument);
		r.SendResultCode(Register.Success);
		return 0;
	}

	private void notifySimpleOnRegister(BServerState state, BServiceInfo info) {
		if (state.getSimple().size() == 0)
			return;
		var sb = new StringBuilder();
		for (var sessionName : state.getSimple().keys()) {
			var session = tableSession.get(sessionName);
			if (null != session && new Register(info).Send(rocks.getRaft().getServer().GetSocket(session.getSessionId()))) {
				sb.append(sessionName).append(',');
				continue;
			}
			logger.warn("NotifySimpleOnRegister {} failed: serverId({}) => sessionId({})",
					info.getServiceName(), info.getServiceIdentity(), sessionName);
		}
		var n = sb.length();
		if (n > 0) {
			sb.setLength(n - 1);
			logger.info("NotifySimpleOnRegister {} serverId({}) => sessionIds({})",
					info.getServiceName(), info.getServiceIdentity(), sb);
		}
	}

	@Override
	protected long ProcessSubscribeRequest(Subscribe r) {
		logger.info("{}: Subscribe {} type={}",
				r.getSender(), r.Argument.getServiceName(), r.Argument.getSubscribeType());
		var netSession = (Session)r.getSender().getUserState();
		var session = tableSession.get(netSession.name);
		session.getSubscribes().put(r.Argument.getServiceName(), toRocks(r.Argument));
		var state = tableServerState.getOrAdd(r.Argument.getServiceName());
		if (!state.getServiceName().equals(r.Argument.getServiceName()))
			state.setServiceName(r.Argument.getServiceName());
		return subscribeAndSend(state, r, netSession.name);
	}

	@Override
	protected long ProcessUnRegisterRequest(UnRegister r) {
		logger.info("{}: UnRegister {} serverId={}",
				r.getSender(), r.Argument.getServiceName(), r.Argument.getServiceIdentity());
		var netSession = (Session)r.getSender().getUserState();
		unRegisterNow(netSession.name, r.Argument); // ignore UnRegisterNow failed
		var session = tableSession.get(netSession.name);
		session.getRegisters().remove(toRocksKey(r.Argument)); // ignore remove failed
		// 注销不存在也返回成功，否则Agent处理比较麻烦。
		r.SendResultCode(Zeze.Services.ServiceManager.UnRegister.Success);
		return 0;
	}

	public BServerState unRegisterNow(String ssName, BServiceInfo info) {
		var state = tableServerState.get(info.getServiceName());
		if (state != null) {
			var exist = state.getServiceInfos().get(info.getServiceIdentity());
			state.getServiceInfos().remove(info.getServiceIdentity());
			if (exist != null && exist.getSessionName().equals(ssName)) {
				// 有可能当前连接没有注销，新的注册已经AddOrUpdate，此时忽略当前连接的注销。
				notifySimpleOnUnRegister(state, fromRocks(exist));
				return state;
			}
		}
		return null;
	}

	private static BServiceInfo fromRocks(BServiceInfoRocks rocks) {
		return new BServiceInfo(rocks.getServiceName(), rocks.getServiceIdentity(),
				rocks.getPassiveIp(), rocks.getPassivePort(), rocks.getExtraInfo());
	}

	public void notifySimpleOnUnRegister(BServerState state, BServiceInfo info) {
		if (state.getSimple().size() == 0)
			return;

		var sb = new StringBuilder();
		for (var sessionName : state.getSimple().keys()) {
			var session = tableSession.get(sessionName);
			if (null != session && new UnRegister(info).Send(rocks.getRaft().getServer().GetSocket(session.getSessionId()))) {
				sb.append(sessionName).append(',');
				continue;
			}
			logger.warn("NotifySimpleOnUnRegister {} failed: serverId({}) => sessionId({})",
					info.getServiceName(), info.getServiceIdentity(), sessionName);
		}
		var n = sb.length();
		if (n > 0) {
			sb.setLength(n - 1);
			logger.info("NotifySimpleOnUnRegister {} serverId({}) => sessionIds({})",
					info.getServiceName(), info.getServiceIdentity(), sb);
		}
	}

	@Override
	protected long ProcessUnSubscribeRequest(UnSubscribe r) {
		logger.info("{}: UnSubscribe {} type={}",
				r.getSender(), r.Argument.getServiceName(), r.Argument.getSubscribeType());
		var netSession = (Session)r.getSender().getUserState();
		var session = tableSession.get(netSession.name);
		var sub = session.getSubscribes().get(r.Argument.getServiceName());
		session.getSubscribes().remove(r.Argument.getServiceName());
		if (sub != null) {
			if (r.Argument.getSubscribeType() == sub.getSubscribeType()) {
				var changed = unSubscribeNow(netSession.name, r.Argument);
				if (changed != null) {
					r.setResultCode(UnSubscribe.Success);
					r.SendResult();
					tryCommit(changed);
					return 0;
				}
			}
		}
		// 取消订阅不能存在返回成功。否则Agent比较麻烦。
		//r.ResultCode = UnSubscribe.NotExist;
		//r.SendResult();
		//return Procedure.LogicError;
		r.setResultCode(Zeze.Services.ServiceManager.UnRegister.Success);
		r.SendResult();
		return 0;
	}

	public BServerState unSubscribeNow(String sessionName, BSubscribeInfo info) {
		var state = tableServerState.get(info.getServiceName());
		if (state != null) {
			CollMap2<String, BSubscribeStateRocks> subState;
			switch (info.getSubscribeType()) {
			case BSubscribeInfo.SubscribeTypeSimple:
				subState = state.getSimple();
				break;
			case BSubscribeInfo.SubscribeTypeReadyCommit:
				subState = state.getReadyCommit();
				break;
			default:
				return null;
			}
			var removed = subState.get(sessionName);
			subState.remove(sessionName);
			if (removed != null)
				return state;
		}
		return null;
	}

	@Override
	protected long ProcessUpdateRequest(Update r) {
		logger.info("{}: Update {} serverId={} ip={} port={}", r.getSender(), r.Argument.getServiceName(),
				r.Argument.getServiceIdentity(), r.Argument.getPassiveIp(), r.Argument.getPassivePort());
		var netSession = (Session)r.getSender().getUserState();
		var session = tableSession.get(netSession.name);
		if (!session.getRegisters().containsKey(toRocksKey(r.Argument)))
			return Update.ServiceNotRegister;
		var state = tableServerState.get(r.Argument.getServiceName());
		if (state == null)
			return Update.ServerStateError;
		var rc = updateAndNotify(state, r.Argument);
		if (rc != 0)
			return rc;
		r.SendResult();
		return 0;
	}

	public int updateAndNotify(BServerState state, BServiceInfo info) {
		var current = state.getServiceInfos().get(info.getServiceIdentity());
		if (current == null)
			return Update.ServiceIdentityNotExist;

		current.setPassiveIp(info.getPassiveIp());
		current.setPassivePort(info.getPassivePort());
		current.setExtraInfo(info.getExtraInfo());

		// 简单广播。
		var sb = new StringBuilder();
		for (var sessionName : state.getSimple().keys()) {
			var session = tableSession.get(sessionName);
			if (null != session && new Update(fromRocks(current)).Send(rocks.getRaft().getServer().GetSocket(session.getSessionId()))) {
				sb.append(sessionName).append(',');
				continue;
			}

			logger.warn("UpdateAndNotify {} failed: serverId({}) => sessionId({})",
					info.getServiceName(), info.getServiceIdentity(), sessionName);
		}
		var n = sb.length();
		if (n > 0)
			sb.setCharAt(n - 1, ';');
		else
			sb.append(';');

		for (var sessionName : state.getReadyCommit().keys()) {
			var session = tableSession.get(sessionName);
			if (new Update(fromRocks(current)).Send(rocks.getRaft().getServer().GetSocket(session.getSessionId()))) {
				sb.append(sessionName).append(',');
				continue;
			}
			logger.warn("UpdateAndNotify {} failed: serverId({}) => sessionId({})",
					info.getServiceName(), info.getServiceIdentity(), sessionName);
		}
		if (sb.length() > 1)
			logger.info("UpdateAndNotify {} serverId({}) => sessionIds({})",
					info.getServiceName(), info.getServiceIdentity(), sb);
		return 0;
	}

	private void startReadyCommitNotify(BServerState state) {
		startReadyCommitNotify(state, false);
	}

	private static BServiceInfos newSortedBServiceInfos(BServerState state) {
		var result = new BServiceInfos();
		result.serviceName = state.getServiceName();
		result.serialId = state.getSerialId();
		var sortedMap = new TreeMap<BServiceInfoKeyRocks, BServiceInfoRocks>();
		for (var info : state.getServiceInfos())
			sortedMap.put(new BServiceInfoKeyRocks(state.getServiceName(), info.getKey()), info.getValue());
		for (var sortedInfo : sortedMap.values())
			result.getServiceInfoListSortedByIdentity().add(fromRocks(sortedInfo));
		return result;
	}

	private void startReadyCommitNotify(BServerState state, boolean notifySimple) {
		if (startNotifyDelayTask != null)
			return;

		state.setSerialId(state.getSerialId() + 1);
		var notify = new NotifyServiceList(newSortedBServiceInfos(state));

		var sb = new StringBuilder();

		if (notifySimple) {
			for (var it : state.getSimple()) {
				var session = tableSession.get(it.getKey());
				if (null != session) {
					var s = rocks.getRaft().getServer().GetSocket(session.getSessionId());
					if (s != null && notify.Send(s))
						sb.append(s.getSessionId()).append(',');
				}
			}
		}

		var n = sb.length();
		if (n > 0)
			sb.setCharAt(n - 1, ';');
		else
			sb.append(';');

		for (var it : state.getReadyCommit()) {
			it.getValue().setReady(false);
			var session = tableSession.get(it.getKey());
			if (null != session) {
				var s = rocks.getRaft().getServer().GetSocket(session.getSessionId());
				if (s != null && notify.Send(s))
					sb.append(s.getSessionId()).append(',');
			}
		}
		if (sb.length() > 1)
			AsyncSocket.logger.info("SEND[{}]: NotifyServiceList: {}", sb, notify.Argument);

		if (state.getReadyCommit().size() > 0) {
			var notifyTimeoutTask = notifyTimeoutTasks.get(state.getServiceName());
			// 只有两段公告模式需要回应处理。
			if (notifyTimeoutTask != null)
				notifyTimeoutTask.cancel(false);
			notifyTimeoutTasks.put(state.getServiceName(), Task.scheduleUnsafe(conf.retryNotifyDelayWhenNotAllReady,
					() -> {
						// NotifyTimeoutTask 会在下面两种情况下被修改：
						// 1. 在 Notify.ReadyCommit 完成以后会被清空。
						// 2. 启动了新的 Notify。
						// restart
						rocks.newProcedure(() -> {
							startReadyCommitNotify(state);
							return 0;
						}).call();
					}));
		}
	}

	@Override
	protected long ProcessReadyServiceListRequest(ReadyServiceList r) {
		var netSession = (Session)r.getSender().getUserState();
		var state = tableServerState.getOrAdd(r.Argument.serviceName);
		if (!state.getServiceName().equals(r.Argument.serviceName))
			state.setServiceName(r.Argument.serviceName);
		setReady(state, r, netSession.name);
		r.SendResult();
		return 0;
	}

	private void setReady(BServerState state, ReadyServiceList r, String sessionName) {
		if (r.Argument.serialId != state.getSerialId()) {
			logger.debug("Ready Skip: SerialId Not Equal. {} Now={}", r.Argument.serialId, state.getSerialId());
			return;
		}
		// logger.debug("Ready:{} Now={}", p.Argument.SerialId, SerialId);
		var subscribeState = state.getReadyCommit().get(sessionName);
		if (subscribeState != null) {
			subscribeState.setReady(true);
			tryCommit(state);
		}
	}

	private void tryCommit(BServerState state) {
		var notifyTimeoutTask = notifyTimeoutTasks.get(state.getServiceName());
		if (notifyTimeoutTask == null)
			return; // no pending notify

		for (var it : state.getReadyCommit()) {
			if (!it.getValue().isReady())
				return;
		}

		logger.debug("Ready Broadcast.");
		var commit = new CommitServiceList();
		commit.Argument.serviceName = state.getServiceName();
		commit.Argument.serialId = state.getSerialId();
		for (var it : state.getReadyCommit()) {
			var session = tableSession.get(it.getKey());
			if (null != session)
				commit.Send(rocks.getRaft().getServer().GetSocket(session.getSessionId()));
		}
		notifyTimeoutTask.cancel(false);
		notifyTimeoutTasks.remove(state.getServiceName());
	}

	private long subscribeAndSend(BServerState state, Subscribe r, String ssName) {
		// 外面会话的 TryAdd 加入成功，下面TryAdd肯定也成功。
		switch (r.Argument.getSubscribeType()) {
		case BSubscribeInfo.SubscribeTypeSimple:
			state.getSimple().put(ssName, new BSubscribeStateRocks());
			if (startNotifyDelayTask == null)
				new SubscribeFirstCommit(newSortedBServiceInfos(state)).Send(r.getSender());
			break;

		case BSubscribeInfo.SubscribeTypeReadyCommit:
			state.getReadyCommit().put(ssName, new BSubscribeStateRocks());
			startReadyCommitNotify(state);
			break;

		default:
			r.SendResultCode(Subscribe.UnknownSubscribeType);
			return Zeze.Transaction.Procedure.LogicError;
		}

		var netSession = (Session)r.getSender().getUserState();
		for (var info : state.getServiceInfos().values())
			addLoadObserver(info.getPassiveIp(), info.getPassivePort(), netSession.name);

		r.SendResultCode(Zeze.Services.ServiceManager.Subscribe.Success);
		return 0;
	}
}
