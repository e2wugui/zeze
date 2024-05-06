package Zeze.Services;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Config;
import Zeze.Net.Acceptor;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolHandle;
import Zeze.Net.Rpc;
import Zeze.Net.RpcTimeoutException;
import Zeze.Net.Service;
import Zeze.Raft.RaftConfig;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.GlobalCacheManager.Acquire;
import Zeze.Services.GlobalCacheManager.BGlobalKeyState;
import Zeze.Services.GlobalCacheManager.Cleanup;
import Zeze.Services.GlobalCacheManager.KeepAlive;
import Zeze.Services.GlobalCacheManager.Login;
import Zeze.Services.GlobalCacheManager.NormalClose;
import Zeze.Services.GlobalCacheManager.ReLogin;
import Zeze.Services.GlobalCacheManager.Reduce;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.IdentityHashSet;
import Zeze.Util.KV;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.OutInt;
import Zeze.Util.OutLong;
import Zeze.Util.OutObject;
import Zeze.Util.PerfCounter;
import Zeze.Util.Task;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

public final class GlobalCacheManagerServer extends ReentrantLock implements GlobalCacheManagerConst {
	static {
		var level = Level.toLevel(System.getProperty("logLevel"), Level.INFO);
		((LoggerContext)LogManager.getContext(false)).getConfiguration().getRootLogger().setLevel(level);
	}

	private static final boolean ENABLE_PERF = true;
	private static final Logger logger = LogManager.getLogger(GlobalCacheManagerServer.class);
	private static final boolean isDebugEnabled = logger.isDebugEnabled();
	private static final GlobalCacheManagerServer instance = new GlobalCacheManagerServer();

	public static GlobalCacheManagerServer getInstance() {
		return instance;
	}

	private ServerService server;
	private AsyncSocket serverSocket;
	private ConcurrentHashMap<Binary, CacheState> global;
	private final AtomicLong serialIdGenerator = new AtomicLong();
	/*
	 * 会话。
	 * key是 LogicServer.Id，现在的实现就是Zeze.Config.ServerId。
	 * 在连接建立后收到的Login Or ReLogin 中设置。
	 * 每个会话记住分配给自己的GlobalTableKey，用来在正常退出的时候释放。
	 * 每个会话还需要记录该会话的Socket.SessionId。在连接重新建立时更新。
	 * 总是GetOrAdd，不删除。按现在的cache-sync设计，
	 * ServerId 是及其有限的。不会一直增长。
	 * 简化实现。
	 */
	private LongConcurrentHashMap<CacheHolder> sessions;
	private final GCMConfig gcmConfig = new GCMConfig();
	private AchillesHeelConfig achillesHeelConfig;
	private GlobalCacheManagerPerf perf;

	public static final class GCMConfig implements Config.ICustomize {
		// 设置了这么大，开始使用后，大概会占用700M的内存，作为全局服务器，先这么大吧。
		// 尽量不重新调整ConcurrentHashMap。
		int initialCapacity = 10_000_000;

		int maxNetPing = 3_000;
		int serverProcessTime = 14_000;
		int serverReleaseTimeout = 60_000;

		@Override
		public String getName() {
			return "GlobalCacheManager";
		}

		@Override
		public void parse(Element self) {
			var attr = self.getAttribute("InitialCapacity");
			if (!attr.isBlank())
				initialCapacity = Math.max(Integer.parseInt(attr), 31);

			attr = self.getAttribute("MaxNetPing");
			if (!attr.isEmpty())
				maxNetPing = Integer.parseInt(attr);
			attr = self.getAttribute("ServerProcessTime");
			if (!attr.isEmpty())
				serverProcessTime = Integer.parseInt(attr);
			attr = self.getAttribute("ServerReleaseTimeout");
			if (!attr.isEmpty())
				serverReleaseTimeout = Integer.parseInt(attr);
		}
	}

	// 外面主动提供装载配置，需要在Load之前把这个实例注册进去。
	public GlobalCacheManagerServer.GCMConfig getGcmConfig() {
		return gcmConfig;
	}

	private GlobalCacheManagerServer() {
	}

	public void start(@Nullable InetAddress ipaddress, int port) {
		start(ipaddress, port, null);
	}

	public void start(@Nullable InetAddress ipaddress, int port, @Nullable Config config) {
		lock();
		try {
			if (server != null)
				return;

			if (ENABLE_PERF)
				perf = new GlobalCacheManagerPerf("", serialIdGenerator);
			PerfCounter.instance.tryStartScheduledLog();

			if (config == null)
				config = Config.load();
			config.parseCustomize(this.gcmConfig);

			sessions = new LongConcurrentHashMap<>(4096);
			global = new ConcurrentHashMap<>(this.gcmConfig.initialCapacity);

			server = new ServerService(config);

			server.AddFactoryHandle(Acquire.TypeId_, new Service.ProtocolFactoryHandle<>(
					Acquire::new, this::processAcquireRequest, TransactionLevel.None, DispatchMode.Normal));
			server.AddFactoryHandle(Reduce.TypeId_, new Service.ProtocolFactoryHandle<>(
					Reduce::new, null, TransactionLevel.None, DispatchMode.Critical));
			server.AddFactoryHandle(Login.TypeId_, new Service.ProtocolFactoryHandle<>(
					Login::new, this::processLogin, TransactionLevel.None, DispatchMode.Critical));
			server.AddFactoryHandle(ReLogin.TypeId_, new Service.ProtocolFactoryHandle<>(
					ReLogin::new, this::processReLogin, TransactionLevel.None, DispatchMode.Critical));
			server.AddFactoryHandle(NormalClose.TypeId_, new Service.ProtocolFactoryHandle<>(
					NormalClose::new, this::processNormalClose, TransactionLevel.None, DispatchMode.Critical));
			// 临时注册到这里，安全起见应该起一个新的Service，并且仅绑定到 localhost。
			server.AddFactoryHandle(Cleanup.TypeId_, new Service.ProtocolFactoryHandle<>(
					Cleanup::new, this::processCleanup, TransactionLevel.None, DispatchMode.Direct));
			server.AddFactoryHandle(KeepAlive.TypeId_, new Service.ProtocolFactoryHandle<>(
					KeepAlive::new, GlobalCacheManagerServer::processKeepAliveRequest, TransactionLevel.None, DispatchMode.Direct));

			serverSocket = server.newServerSocket(ipaddress, port,
					new Acceptor(port, ipaddress != null ? ipaddress.getHostAddress() : null));

			// Global的守护不需要独立线程。当出现异常问题不能工作时，没有释放锁是不会造成致命问题的。
			achillesHeelConfig = new AchillesHeelConfig(this.gcmConfig.maxNetPing,
					this.gcmConfig.serverProcessTime, this.gcmConfig.serverReleaseTimeout);
			Task.schedule(5000, 5000, this::achillesHeelDaemon);
		} finally {
			unlock();
		}
	}

	private void achillesHeelDaemon() {
		var now = System.currentTimeMillis();

		sessions.forEach(session -> {
			if (now - session.getActiveTime() > achillesHeelConfig.globalDaemonTimeout && !session.debugMode) {
				session.lock();
				try {
					session.kick();
					if (!session.acquired.isEmpty()) {
						var releaseCount = 0L;
						for (var k : session.acquired.keySet()) {
							// ConcurrentDictionary 可以在循环中删除。这样虽然效率低些，但是能处理更多情况。
							try {
								release(session, k, false);
								++releaseCount;
							} catch (InterruptedException ex) {
								logger.error("", ex);
							}
						}
						session.setActiveTime(System.currentTimeMillis());
						if (releaseCount > 0)
							logger.info("AchillesHeelDaemon.Release session={} count={}", session, releaseCount);
					}
				} finally {
					session.unlock();
				}
			}
		});
	}

	public void stop() throws Exception {
		lock();
		try {
			if (server == null)
				return;
			serverSocket.close();
			serverSocket = null;
			server.stop();
			server = null;
		} finally {
			unlock();
		}
	}

	/**
	 * 报告错误的时候带上相关信息（包括GlobalCacheManager和LogicServer等等）
	 * 手动Cleanup时，连接正确的服务器执行。
	 */
	private long processCleanup(Cleanup rpc) {
		logger.info("ProcessCleanup: {} RequestId={} {}", rpc.getSender(), rpc.getSessionId(), rpc.Argument);
		if (achillesHeelConfig != null) // disable cleanup.
			return 0;

		// 安全性以后加强。
		if (!rpc.Argument.secureKey.equals("Ok! verify secure.")) {
			logger.warn("ProcessCleanup: {} RequestId={} result={}", rpc.getSender(), rpc.getSessionId(), CleanupErrorSecureKey);
			rpc.SendResultCode(CleanupErrorSecureKey);
			return 0;
		}

		var session = sessions.computeIfAbsent(rpc.Argument.serverId, __ -> new CacheHolder());
		if (session.globalCacheManagerHashIndex != rpc.Argument.globalCacheManagerHashIndex) {
			// 多点验证
			logger.warn("ProcessCleanup: {} RequestId={} result={}", rpc.getSender(), rpc.getSessionId(), CleanupErrorGlobalCacheManagerHashIndex);
			rpc.SendResultCode(CleanupErrorGlobalCacheManagerHashIndex);
			return 0;
		}

		if (server.GetSocket(session.sessionId) != null) {
			// 连接存在，禁止cleanup。
			logger.warn("ProcessCleanup: {} RequestId={} result={}", rpc.getSender(), rpc.getSessionId(), CleanupErrorHasConnection);
			rpc.SendResultCode(CleanupErrorHasConnection);
			return 0;
		}

		// 还有更多的防止出错的手段吗？

		// XXX verify danger
		Task.schedule(5 * 60 * 1000, () -> { // delay 5 mins
			for (var k : session.acquired.keySet()) {
				// ConcurrentDictionary 可以在循环中删除。这样虽然效率低些，但是能处理更多情况。
				release(session, k, false);
			}
			rpc.SendResultCode(0);
		});

		return 0;
	}

	private long processLogin(Login rpc) throws Exception {
		logger.info("ProcessLogin: {} RequestId={} {}", rpc.getSender(), rpc.getSessionId(), rpc.Argument);
		var session = sessions.computeIfAbsent(rpc.Argument.serverId, __ -> new CacheHolder());
		if (!session.tryBindSocket(rpc.getSender(), rpc.Argument.globalCacheManagerHashIndex, true)) {
			rpc.SendResultCode(LoginBindSocketFail);
			return 0;
		}
		// new login, 比如逻辑服务器重启。release old acquired.
		for (var k : session.acquired.keySet()) {
			// ConcurrentDictionary 可以在循环中删除。这样虽然效率低些，但是能处理更多情况。
			release(session, k, false);
		}
		session.setActiveTime(System.currentTimeMillis());
		session.setDebugMode(rpc.Argument.debugMode);
		rpc.Result.maxNetPing = gcmConfig.maxNetPing;
		rpc.Result.serverProcessTime = gcmConfig.serverProcessTime;
		rpc.Result.serverReleaseTimeout = gcmConfig.serverReleaseTimeout;
		rpc.SendResultCode(0);
		return 0;
	}

	private long processReLogin(ReLogin rpc) {
		logger.info("ProcessReLogin: {} RequestId={} {}", rpc.getSender(), rpc.getSessionId(), rpc.Argument);
		var session = sessions.computeIfAbsent(rpc.Argument.serverId, __ -> new CacheHolder());
		if (!session.tryBindSocket(rpc.getSender(), rpc.Argument.globalCacheManagerHashIndex, false)) {
			rpc.SendResultCode(ReLoginBindSocketFail);
			return 0;
		}
		session.setActiveTime(System.currentTimeMillis());
		session.setDebugMode(rpc.Argument.debugMode);
		rpc.SendResultCode(0);
		return 0;
	}

	private long processNormalClose(NormalClose rpc) throws Exception {
		logger.info("ProcessNormalClose: {} RequestId={}", rpc.getSender(), rpc.getSessionId());
		var session = (CacheHolder)rpc.getSender().getUserState();
		if (session == null) {
			logger.warn("ProcessNormalClose: {} RequestId={} result={}", rpc.getSender(), rpc.getSessionId(), AcquireNotLogin);
			rpc.SendResultCode(AcquireNotLogin);
			return 0; // not login
		}
		if (!session.tryUnBindSocket(rpc.getSender())) {
			logger.warn("ProcessNormalClose: {} RequestId={} result={}", rpc.getSender(), rpc.getSessionId(), NormalCloseUnbindFail);
			rpc.SendResultCode(NormalCloseUnbindFail);
			return 0;
		}
		for (var k : session.acquired.keySet()) {
			// ConcurrentDictionary 可以在循环中删除。这样虽然效率低些，但是能处理更多情况。
			release(session, k, false);
		}
		rpc.SendResultCode(0);
		logger.info("After NormalClose global.Count={}", global.size());
		return 0;
	}

	private static long processKeepAliveRequest(KeepAlive rpc) {
		if (rpc.getSender().getUserState() == null) {
			rpc.SendResultCode(AcquireNotLogin);
			return 0;
		}
		var sender = (CacheHolder)rpc.getSender().getUserState();
		sender.setActiveTime(System.currentTimeMillis());
		rpc.SendResult();
		return 0;
	}

	private long processAcquireRequest(Acquire rpc) {
		var acquireState = rpc.Argument.state;
		if (ENABLE_PERF)
			perf.onAcquireBegin(rpc, acquireState);
		rpc.Result.globalKey = rpc.Argument.globalKey;
		rpc.Result.state = acquireState; // default success

		long result = 0;
		if (rpc.getSender().getUserState() == null) {
			rpc.Result.state = StateInvalid;
			rpc.SendResultCode(AcquireNotLogin);
		} else {
			try {
				var sender = (CacheHolder)rpc.getSender().getUserState();
				sender.setActiveTime(System.currentTimeMillis());
				switch (acquireState) {
				case StateInvalid: // release
					rpc.Result.state = release(sender, rpc.Argument.globalKey, true); //await 方法内有等待
					rpc.SendResultCode(0);
					break;
				case StateShare:
					result = acquireShare(rpc); //await 方法内有等待
					break;
				case StateModify:
					result = acquireModify(rpc); //await 方法内有等待
					break;
				default:
					rpc.Result.state = StateInvalid;
					rpc.SendResultCode(AcquireErrorState);
					break;
				}
			} catch (Throwable ex) { // rpc response
				logger.error("ProcessAcquireRequest", ex);
				rpc.Result.state = StateInvalid;
				rpc.SendResultCode(AcquireException);
			}
		}
		if (ENABLE_PERF)
			perf.onAcquireEnd(rpc, acquireState);
		return result;
	}

	private int release(CacheHolder sender, Binary _gKey, boolean noWait) throws InterruptedException {
		while (true) {
			CacheState cs = global.computeIfAbsent(_gKey, CacheState::new);
			cs.lock();
			try { //await 等锁
				if (cs.acquireStatePending == StateRemoved) {
					// 这个是不可能的，因为有Release请求进来意味着肯定有拥有者(share or modify)，此时不可能进入StateRemoved。
					continue;
				}

				var gKey = cs.globalKey; // release 应该不需要引用到同一个key，统一写成这样了。
				while (cs.acquireStatePending != StateInvalid && cs.acquireStatePending != StateRemoved) {
					switch (cs.acquireStatePending) {
					case StateShare:
					case StateModify:
						if (isDebugEnabled)
							logger.debug("Release 0 {} {} {}", sender, gKey, cs);
						if (noWait)
							return cs.getSenderCacheState(sender);
						break;
					case StateRemoving:
						// release 不会导致死锁，等待即可。
						break;
					}
					cs.await(); //await 等通知
				}
				if (cs.acquireStatePending == StateRemoved)
					continue;
				cs.acquireStatePending = StateRemoving;

				if (cs.modify == sender)
					cs.modify = null;
				cs.share.remove(sender); // always try remove
				sender.acquired.remove(gKey);

				if (cs.modify == null && cs.share.isEmpty()) {
					// 安全的从global中删除，没有并发问题。
					cs.acquireStatePending = StateRemoved;
					global.remove(gKey);
				} else
					cs.acquireStatePending = StateInvalid;
				cs.signalAll(); //notify
				return StateInvalid;
				//notify
			} finally {
				cs.unlock();
			}
		}
	}

	private int acquireShare(Acquire rpc) throws InterruptedException {
		CacheHolder sender = (CacheHolder)rpc.getSender().getUserState();
		while (true) {
			CacheState cs = global.computeIfAbsent(rpc.Argument.globalKey, CacheState::new);
			cs.lock();
			try { //await 等锁
				if (cs.acquireStatePending == StateRemoved)
					continue;

				if (cs.modify != null && !cs.share.isEmpty())
					throw new IllegalStateException("CacheState state error");

				while (cs.acquireStatePending != StateInvalid && cs.acquireStatePending != StateRemoved) {
					switch (cs.acquireStatePending) {
					case StateShare:
						if (cs.modify == null)
							throw new IllegalStateException("CacheState state error");
						if (cs.modify == sender) {
							if (isDebugEnabled)
								logger.debug("1 {} {} {}", sender, StateShare, cs);
							rpc.Result.state = StateInvalid;
							rpc.SendResultCode(AcquireShareDeadLockFound);
							return 0;
						}
						break;
					case StateModify:
						if (cs.modify == sender || cs.share.contains(sender)) {
							if (isDebugEnabled)
								logger.debug("2 {} {} {}", sender, StateShare, cs);
							rpc.Result.state = StateInvalid;
							rpc.SendResultCode(AcquireShareDeadLockFound);
							return 0;
						}
						break;
					case StateRemoving:
						break;
					}
					if (isDebugEnabled)
						logger.debug("3 {} {} {}", sender, StateShare, cs);
					cs.await(); //await 等通知
					if (cs.modify != null && !cs.share.isEmpty())
						throw new IllegalStateException("CacheState state error");
				}
				if (cs.acquireStatePending == StateRemoved)
					continue; // concurrent release

				cs.acquireStatePending = StateShare;
				serialIdGenerator.getAndIncrement();

				var gKey = cs.globalKey;
				var reduceTid = new OutLong();
				if (cs.modify != null) {
					if (cs.modify == sender) {
						// 已经是Modify又申请，可能是sender异常关闭，
						// 又重启连上。更新一下。应该是不需要的。
						sender.acquired.put(gKey, StateModify);
						cs.acquireStatePending = StateInvalid;
						if (isDebugEnabled)
							logger.debug("4 {} {} {}", sender, StateShare, cs);
						rpc.Result.state = StateModify;
						rpc.SendResultCode(AcquireShareAlreadyIsModify);
						return 0;
					}

					var reduceResultState = new OutInt(StateReduceNetError); // 默认网络错误。
					if (cs.modify.reduce(gKey, rpc.getResultCode(), r -> { //await 方法内有等待
						if (ENABLE_PERF)
							perf.onReduceEnd(r);
						if (r.isTimeout()) {
							reduceResultState.value = StateReduceRpcTimeout;
						}else {
							reduceResultState.value = r.Result.state;
							reduceTid.value = r.Result.reducedTid;
						}
						cs.lock();
						try {
							cs.signalAll(); //notify
						} finally {
							cs.unlock();
						}
						return 0;
					})) {
						if (isDebugEnabled)
							logger.debug("5 {} {} {}", sender, StateShare, cs);
						cs.await();
					}
					switch (reduceResultState.value) {
					case StateShare:
						cs.modify.acquired.put(gKey, StateShare);
						cs.share.add(cs.modify); // 降级成功。
						break;

					case StateInvalid:
						// 降到了 Invalid，此时就不需要加入 Share 了。
						cs.modify.acquired.remove(gKey);
						break;

					case StateReduceErrorFreshAcquire:
						cs.acquireStatePending = StateInvalid;
						cs.signalAll(); //notify
						if (ENABLE_PERF)
							perf.onOthers("XXX Fresh " + StateShare);
						// logger.error("XXX Fresh {} {} {}", sender, StateShare, cs);
						rpc.Result.state = StateInvalid;
						rpc.SendResultCode(StateReduceErrorFreshAcquire);
						return 0;

					default:
						// 包含协议返回错误的值的情况。
						// case StateReduceRpcTimeout: // 11
						// case StateReduceException: // 12
						// case StateReduceNetError: // 13
						cs.acquireStatePending = StateInvalid;
						cs.signalAll(); //notify
						if (ENABLE_PERF)
							perf.onOthers("XXX 8 " + StateShare + " " + reduceResultState.value);
						// logger.error("XXX 8 {} {} {} {}", sender, StateShare, cs, reduceResultState.Value);
						rpc.Result.state = StateInvalid;
						rpc.SendResultCode(AcquireShareFailed);
						return 0;
					}

					sender.acquired.put(gKey, StateShare);
					cs.modify = null;
					cs.share.add(sender);
					cs.acquireStatePending = StateInvalid;
					cs.signalAll(); //notify
					if (isDebugEnabled)
						logger.debug("6 {} {} {}", sender, StateShare, cs);
					rpc.Result.reducedTid = reduceTid.value;
					rpc.SendResultCode(0);
					return 0;
				}

				sender.acquired.put(gKey, StateShare);
				cs.share.add(sender);
				cs.acquireStatePending = StateInvalid;
				cs.signalAll(); //notify
				if (isDebugEnabled)
					logger.debug("7 {} {} {}", sender, StateShare, cs);
				rpc.Result.reducedTid = reduceTid.value;
				rpc.SendResultCode(0);
				return 0;
			} finally {
				cs.unlock();
			}
		}
	}

	private int acquireModify(Acquire rpc) throws InterruptedException {
		CacheHolder sender = (CacheHolder)rpc.getSender().getUserState();
		while (true) {
			CacheState cs = global.computeIfAbsent(rpc.Argument.globalKey, CacheState::new);
			cs.lock();
			try { //await 等锁
				if (cs.acquireStatePending == StateRemoved)
					continue;

				if (cs.modify != null && !cs.share.isEmpty())
					throw new IllegalStateException("CacheState state error");

				while (cs.acquireStatePending != StateInvalid && cs.acquireStatePending != StateRemoved) {
					switch (cs.acquireStatePending) {
					case StateShare:
						if (cs.modify == null)
							throw new IllegalStateException("CacheState state error");

						if (cs.modify == sender) {
							if (isDebugEnabled)
								logger.debug("1 {} {} {}", sender, StateModify, cs);
							rpc.Result.state = StateInvalid;
							rpc.SendResultCode(AcquireModifyDeadLockFound);
							return 0;
						}
						break;
					case StateModify:
						if (cs.modify == sender || cs.share.contains(sender)) {
							if (isDebugEnabled)
								logger.debug("2 {} {} {}", sender, StateModify, cs);
							rpc.Result.state = StateInvalid;
							rpc.SendResultCode(AcquireModifyDeadLockFound);
							return 0;
						}
						break;
					case StateRemoving:
						break;
					}
					if (isDebugEnabled)
						logger.debug("3 {} {} {}", sender, StateModify, cs);
					cs.await(); //await 等通知
					if (cs.modify != null && !cs.share.isEmpty())
						throw new IllegalStateException("CacheState state error");
				}
				if (cs.acquireStatePending == StateRemoved)
					continue; // concurrent release

				cs.acquireStatePending = StateModify;
				serialIdGenerator.getAndIncrement();

				var gKey = cs.globalKey;
				var reduceTid = new OutLong();
				if (cs.modify != null) {
					if (cs.modify == sender) {
						if (isDebugEnabled)
							logger.debug("4 {} {} {}", sender, StateModify, cs);
						// 已经是Modify又申请，可能是sender异常关闭，又重启连上。
						// 更新一下。应该是不需要的。
						sender.acquired.put(gKey, StateModify);
						cs.acquireStatePending = StateInvalid;
						cs.signalAll(); //notify
						rpc.SendResultCode(AcquireModifyAlreadyIsModify);
						return 0;
					}

					var reduceResultState = new OutInt(StateReduceNetError); // 默认网络错误。
					if (cs.modify.reduce(gKey, rpc.getResultCode(), r -> { //await 方法内有等待
						if (ENABLE_PERF)
							perf.onReduceEnd(r);
						if (r.isTimeout()) {
							reduceResultState.value = StateReduceRpcTimeout;
						}else {
							reduceResultState.value = r.Result.state;
							reduceTid.value = r.Result.reducedTid;
						}
						cs.lock();
						try {
							cs.signalAll(); //notify
						} finally {
							cs.unlock();
						}
						return 0;
					})) {
						if (isDebugEnabled)
							logger.debug("5 {} {} {}", sender, StateModify, cs);
						cs.await(); //await 等通知
					}

					switch (reduceResultState.value) {
					case StateInvalid:
						cs.modify.acquired.remove(gKey);
						break; // reduce success

					case StateReduceErrorFreshAcquire:
						cs.acquireStatePending = StateInvalid;
						cs.signalAll(); //notify
						if (ENABLE_PERF)
							perf.onOthers("XXX Fresh " + StateModify);
						// logger.error("XXX Fresh {} {} {}", sender, StateModify, cs);
						rpc.Result.state = StateInvalid;
						rpc.SendResultCode(StateReduceErrorFreshAcquire);
						return 0;

					default:
						// case StateReduceRpcTimeout: // 11
						// case StateReduceException: // 12
						// case StateReduceNetError: // 13
						cs.acquireStatePending = StateInvalid;
						cs.signalAll(); //notify
						if (ENABLE_PERF)
							perf.onOthers("XXX 9 " + StateModify + " " + reduceResultState.value);
						// logger.error("XXX 9 {} {} {} {}", sender, StateModify, cs, reduceResultState.Value);
						rpc.Result.state = StateInvalid;
						rpc.SendResultCode(AcquireModifyFailed);
						return 0;
					}

					sender.acquired.put(gKey, StateModify);
					cs.modify = sender;
					cs.share.remove(sender);
					cs.acquireStatePending = StateInvalid;
					cs.signalAll(); //notify
					if (isDebugEnabled)
						logger.debug("6 {} {} {}", sender, StateModify, cs);
					rpc.Result.reducedTid = reduceTid.value;
					rpc.SendResultCode(0);
					return 0;
				}

				ArrayList<KV<CacheHolder, Reduce>> reducePending = new ArrayList<>();
				IdentityHashSet<CacheHolder> reduceSucceed = new IdentityHashSet<>();
				boolean senderIsShare = false;
				// 先把降级请求全部发送给出去。
				for (var it = cs.share.iterator(); it.moveToNext(); ) {
					var c = it.value();
					if (c == sender) {
						// 申请者不需要降级，直接加入成功。
						senderIsShare = true;
						reduceSucceed.add(sender);
						continue;
					}
					Reduce reduce = c.reduceWaitLater(gKey, rpc.getResultCode());
					if (reduce == null) {
						// 网络错误不再认为成功。整个降级失败，要中断降级。
						// 已经发出去的降级请求要等待并处理结果。后面处理。
						break;
					}
					reducePending.add(KV.create(c, reduce));
				}

				// 两种情况不需要发reduce
				// 1. share是空的, 可以直接升为Modify
				// 2. sender是share, 而且reducePending的size是0
				var errorFreshAcquire = new OutObject<>(Boolean.FALSE);
				if (!cs.share.isEmpty() && (!senderIsShare || !reducePending.isEmpty())) {
					Task.executeUnsafe(() -> {
						// 一个个等待是否成功。WaitAll 碰到错误不知道怎么处理的，
						// 应该也会等待所有任务结束（包括错误）。
						var freshAcquire = false;
						for (var reduce : reducePending) {
							try {
								//noinspection DataFlowIssue
								reduce.getValue().getFuture().await(); //await 等RPC回复
								switch (reduce.getValue().Result.state) {
								case StateInvalid:
									reduceSucceed.add(reduce.getKey());
									break;
								case StateReduceErrorFreshAcquire:
									// 这个错误不进入Forbid状态。
									freshAcquire = true;
									break;
								default:
									reduce.getKey().setError();
									logger.error("Reduce result state={}", reduce.getValue().Result.state);
									break;
								}
							} catch (Throwable ex) { // exception to result
								reduce.getKey().setError();
								// 等待失败不再看作成功。
								if (Task.getRootCause(ex) instanceof RpcTimeoutException) {
									logger.warn("Reduce Timeout {} AcquireState={} CacheState={} arg={}",
											sender, StateModify, cs, reduce.getValue().Argument);
								} else {
									logger.error("Reduce {} AcquireState={} CacheState={} arg={}",
											sender, StateModify, cs, reduce.getValue().Argument, ex);
								}
							}
						}
						errorFreshAcquire.value = freshAcquire;
						cs.lock();
						try {
							// 需要唤醒等待任务结束的，但没法指定，只能全部唤醒。
							cs.signalAll(); //notify
						} finally {
							cs.unlock();
						}
					}, "GlobalCacheManager.AcquireModify.WaitReduce", DispatchMode.Normal);
					if (isDebugEnabled)
						logger.debug("7 {} {} {}", sender, StateModify, cs);
					cs.await(); //await 等通知
				}

				// 移除成功的。
				for (var it = reduceSucceed.iterator(); it.moveToNext(); ) {
					var succeed = it.value();
					if (succeed != sender) {
						// sender 不移除：
						// 1. 如果申请成功，后面会更新到Modify状态。
						// 2. 如果申请不成功，恢复 cs.Share，保持 Acquired 不变。
						succeed.acquired.remove(gKey);
					}
					cs.share.remove(succeed);
				}
				// 如果前面降级发生中断(break)，这里就不会为0。
				if (cs.share.isEmpty()) {
					sender.acquired.put(gKey, StateModify);
					cs.modify = sender;
					cs.acquireStatePending = StateInvalid;
					cs.signalAll(); //notify
					if (isDebugEnabled)
						logger.debug("8 {} {} {}", sender, StateModify, cs);
					rpc.Result.reducedTid = reduceTid.value;
					rpc.SendResultCode(0);
				} else {
					// senderIsShare 在失败的时候，Acquired 没有变化，不需要更新。
					// 失败了，要把原来是share的sender恢复。先这样吧。
					if (senderIsShare)
						cs.share.add(sender);
					cs.acquireStatePending = StateInvalid;
					cs.signalAll(); //notify
					if (ENABLE_PERF)
						perf.onOthers("XXX 10 " + StateModify + ' ' + errorFreshAcquire.value);
					// logger.error("XXX 10 {} {} {}", sender, StateModify, cs);
					rpc.Result.state = StateInvalid;
					if (errorFreshAcquire.value)
						rpc.SendResultCode(StateReduceErrorFreshAcquire); // 这个错误不看做失败，允许发送方继续尝试。
					else
						rpc.SendResultCode(AcquireModifyFailed);
				}
				// 很好，网络失败不再看成成功，发现除了加break，
				// 其他处理已经能包容这个改动，都不用动。
				return 0;
			} finally {
				cs.unlock();
			}
		}
	}

	private static final class CacheState extends ReentrantLock {
		final Binary globalKey; // 这里的引用同global map的key,用于给CacheHolder里的map相同的key引用
		final IdentityHashSet<CacheHolder> share = new IdentityHashSet<>();
		CacheHolder modify;
		int acquireStatePending = StateInvalid;
		private final Condition thisCond = newCondition();

		public CacheState(Binary gKey) {
			globalKey = gKey;
		}

		public void await() throws InterruptedException {
			thisCond.await();
		}

		public void signalAll() {
			thisCond.signalAll();
		}

		int getSenderCacheState(CacheHolder sender) {
			if (modify == sender)
				return StateModify;
			if (share.contains(sender))
				return StateShare;
			return StateInvalid;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			ByteBuffer.BuildString(sb, share);
			return String.format("P%d M%s S%s", acquireStatePending, modify, sb);
		}
	}

	private static final class CacheHolder extends ReentrantLock {
		final ConcurrentHashMap<Binary, Integer> acquired = new ConcurrentHashMap<>();
		long sessionId;
		int globalCacheManagerHashIndex;
		private volatile long activeTime = System.currentTimeMillis();
		private volatile long lastErrorTime;
		private boolean logined = false;
		private boolean debugMode;

		// not under lock
		void kick() {
			var peer = instance.server.GetSocket(sessionId);
			if (null != peer) {
				peer.setUserState(null); // 来自这个Agent的所有请求都会失败。
				peer.close(kickException); // 关闭连接，强制Agent重新登录。
			}
			sessionId = 0; // 清除网络状态。
		}

		long getActiveTime() {
			return activeTime;
		}

		void setActiveTime(long value) {
			activeTime = value;
		}

		void setDebugMode(boolean debugMode) {
			this.debugMode = debugMode;
		}

		boolean tryBindSocket(AsyncSocket newSocket, int _GlobalCacheManagerHashIndex, boolean login) {
			lock();
			try {
				if (login) {
					// login 相当于重置，允许再次Login。
					logined = true;
				} else {
					// relogin 必须login之后才允许ReLogin。这个用来检测Global宕机并重启。
					if (!logined)
						return false;
				}
				if (newSocket.getUserState() != null) {
					logger.warn("TryBindSocket: already bound! newSocket.getUserState() != null, SessionId={}", newSocket.getSessionId());
					return false; // 不允许再次绑定。Login Or ReLogin 只能发一次。
				}

				var socket = instance.server.GetSocket(sessionId);
				if (socket == null) {
					// old socket not exist or has lost.
					sessionId = newSocket.getSessionId();
					newSocket.setUserState(this);
					globalCacheManagerHashIndex = _GlobalCacheManagerHashIndex;
					return true;
				}
				// 每个AutoKeyLocalId只允许一个实例，已经存在了以后，旧的实例上有状态，阻止新的实例登录成功。
				logger.warn("TryBindSocket: already bound! GetSocket(SessionId={}) != null", newSocket.getSessionId());
				return false;
			} finally {
				unlock();
			}
		}

		boolean tryUnBindSocket(AsyncSocket oldSocket) {
			lock();
			try {
				// 这里检查比较严格，但是这些检查应该都不会出现。

				if (oldSocket.getUserState() != this)
					return false; // not bind to this

				var current = instance.server.GetSocket(sessionId);
				if (current != null && current != oldSocket)
					return false; // not same socket

				sessionId = 0;
				return true;
			} finally {
				unlock();
			}
		}

		@Override
		public String toString() {
			return String.valueOf(sessionId);
		}

		boolean reduce(Binary gkey, long fresh, ProtocolHandle<Rpc<BGlobalKeyState, BGlobalKeyState>> response) {
			try {
				if (System.currentTimeMillis() - lastErrorTime < instance.achillesHeelConfig.globalForbidPeriod)
					return false;
				AsyncSocket peer = instance.server.GetSocket(sessionId);
				if (peer != null) {
					var reduce = new Reduce(gkey, StateInvalid);
					reduce.setResultCode(fresh);
					if (ENABLE_PERF)
						instance.perf.onReduceBegin(reduce);
					if (reduce.Send(peer, response, instance.achillesHeelConfig.reduceTimeout)) //await 等RPC回复
						return true;
					if (ENABLE_PERF)
						instance.perf.onReduceCancel(reduce);
				}
				logger.warn("Send Reduce failed. SessionId={}, peer={}, gkey={}", sessionId, peer, gkey);
			} catch (Exception ex) {
				// 这里的异常只应该是网络发送异常。
				logger.error("Reduce Exception {}", gkey, ex);
			}
			setError();
			return false;
		}

		void setError() {
			long now = System.currentTimeMillis();
			if (now - lastErrorTime > instance.achillesHeelConfig.globalForbidPeriod)
				lastErrorTime = now;
		}

		/**
		 * 返回null表示发生了网络错误，或者应用服务器已经关闭。
		 */
		Reduce reduceWaitLater(Binary gkey, long fresh) {
			try {
				if (System.currentTimeMillis() - lastErrorTime < instance.achillesHeelConfig.globalForbidPeriod)
					return null;
				AsyncSocket peer = instance.server.GetSocket(sessionId);
				if (peer != null) {
					var reduce = new Reduce(gkey, StateInvalid);
					reduce.setResultCode(fresh);
					if (ENABLE_PERF)
						instance.perf.onReduceBegin(reduce);
					reduce.SendForWait(peer, instance.achillesHeelConfig.reduceTimeout);
					if (ENABLE_PERF) {
						//noinspection DataFlowIssue
						if (reduce.getFuture().isCompletedExceptionally() && !reduce.isTimeout())
							instance.perf.onReduceCancel(reduce);
						else
							instance.perf.onReduceEnd(reduce);
					}
					return reduce;
				}
				logger.warn("Send Reduce failed. SessionId={}, gkey={}", sessionId, gkey);
			} catch (Throwable ex) { // 这里的异常只应该是网络发送异常。
				logger.error("ReduceWaitLater Exception {}", gkey, ex);
			}
			setError();
			return null;
		}
	}

	private static final class ServerService extends Service {
		ServerService(Config config) {
			super("GlobalCacheManager", config);
		}

		@Override
		public void OnSocketAccept(AsyncSocket so) throws Exception {
			logger.info("OnSocketAccept {}", so);
			// so.UserState = new CacheHolder(so.SessionId); // Login ReLogin 的时候初始化。
			super.OnSocketAccept(so);
		}

		@Override
		public void OnSocketClose(AsyncSocket so, Throwable e) throws Exception {
			logger.info("OnSocketClose {}", so);
			var session = (CacheHolder)so.getUserState();
			if (session != null)
				session.tryUnBindSocket(so); // unbind when login
			super.OnSocketClose(so, e);
		}

		@Override
		public <P extends Protocol<?>> void dispatchRpcResponse(P rpc, ProtocolHandle<P> responseHandle,
																ProtocolFactoryHandle<?> factoryHandle) {
			try {
				responseHandle.handle(rpc);
			} catch (Throwable e) { // logger.error
				logger.error("dispatchRpcResponse exception:", e);
			}
		}
	}

	public static void main(String[] args) throws Exception {
		Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
			//noinspection CallToPrintStackTrace
			e.printStackTrace();
			logger.error("uncaught exception in {}:", t, e);
		});

		String ip = null;
		int port = 5002;
		String raftName = null;
		String raftConf = "global.raft.xml";

		Task.tryInitThreadPool();

		for (int i = 0; i < args.length; ++i) {
			switch (args[i]) {
			case "-ip":
				ip = args[++i];
				break;
			case "-port":
				port = Integer.parseInt(args[++i]);
				break;
			case "-raft":
				raftName = args[++i];
				break;
			case "-raftConf":
				raftConf = args[++i];
				break;
			case "-threads":
				i++;
				// ThreadPool.SetMinThreads(int.Parse(args[i]), completionPortThreads);
				break;
			default:
				throw new IllegalArgumentException("unknown argument: " + args[i]);
			}
		}

		if (raftName == null || raftName.isEmpty()) {
			logger.info("Start {}:{}", ip != null ? ip : "any", port);
			InetAddress address = (ip != null && !ip.isBlank()) ? InetAddress.getByName(ip) : null;
			instance.start(address, port);
			synchronized (Thread.currentThread()) {
				Thread.currentThread().wait();
			}
		} else if (raftName.equals("RunAllNodes")) {
			logger.info("Start Raft=RunAllNodes");
			//noinspection unused
			try (var GlobalRaft1 = new GlobalCacheManagerWithRaft("127.0.0.1_5556", RaftConfig.load(raftConf));
				 var GlobalRaft2 = new GlobalCacheManagerWithRaft("127.0.0.1_5557", RaftConfig.load(raftConf));
				 var GlobalRaft3 = new GlobalCacheManagerWithRaft("127.0.0.1_5558", RaftConfig.load(raftConf))) {
				synchronized (Thread.currentThread()) {
					Thread.currentThread().wait();
				}
			}
		} else {
			logger.info("Start Raft={},{}", raftName, raftConf);
			//noinspection unused
			try (var GlobalRaft = new GlobalCacheManagerWithRaft(raftName, RaftConfig.load(raftConf))) {
				synchronized (Thread.currentThread()) {
					Thread.currentThread().wait();
				}
			}
		}
	}
}
