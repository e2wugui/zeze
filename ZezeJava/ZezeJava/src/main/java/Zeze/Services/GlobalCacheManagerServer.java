package Zeze.Services;

import java.util.Objects;
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
import Zeze.Util.DaemonTimer;
import Zeze.Util.Id128;
import Zeze.Util.IdentityHashSet;
import Zeze.Util.KVNotNull;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.OutInt;
import Zeze.Util.OutObject;
import Zeze.Util.Task;
import Zeze.Util.TaskSpec;
import Zeze.Util.ZezeCounter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Element;
import static Zeze.Util.Args.requireInt;
import static Zeze.Util.Args.requireValue;

/**
 * 同步版全局缓存管理器服务器（单例）：管理cache-sync的全局锁所有权协商（Acquire/Reduce/Release），
 * 每个GlobalTableKey一把锁，处理线程在锁与Condition上同步等待。
 */
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

	// stop 仅停止对象而不置 null，避免在飞协议或晚到守护解引用 NPE；open 作为活/死闸门
	private volatile @Nullable ServerService server;
	private @Nullable AsyncSocket serverSocket;
	private @Nullable ConcurrentHashMap<Binary, CacheState> global;
	private final AtomicLong serialIdGenerator = new AtomicLong();
	// 会话表：key 为 ServerId，Login/ReLogin 时按需创建且不删除（ServerId 数量有限）
	private @Nullable LongConcurrentHashMap<CacheHolder> sessions;
	private final GCMConfig gcmConfig = new GCMConfig();
	private @Nullable AchillesHeelConfig achillesHeelConfig;
	private final DaemonTimer achillesHeelDaemonTimer =
			new DaemonTimer("GlobalCacheManager.AchillesHeelDaemon", 5000, this::achillesHeelDaemon);
	// open 为受理新绑定的闸门；stopping 仅在实例锁内维护，防止 stop 两段之间 start() 重入
	private volatile boolean open;
	private boolean stopping;
	private @Nullable GlobalCacheManagerPerf perf;

	private ServerService getServer() {
		var server = this.server;
		assert server != null;
		return server;
	}

	private ConcurrentHashMap<Binary, CacheState> getGlobal() {
		var global = this.global;
		assert global != null;
		return global;
	}

	private LongConcurrentHashMap<CacheHolder> getSessions() {
		var sessions = this.sessions;
		assert sessions != null;
		return sessions;
	}

	private AchillesHeelConfig getAchillesHeelConfig() {
		var config = achillesHeelConfig;
		assert config != null;
		return config;
	}

	private GlobalCacheManagerPerf getPerf() {
		var perf = this.perf;
		assert perf != null;
		return perf;
	}


	public static final class GCMConfig implements Config.ICustomize {
		// 设置这么大，开始使用后大概会占用700M内存；作为全局服务器足够，
		// 且尽量不重新调整ConcurrentHashMap。
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
	public GCMConfig getGcmConfig() {
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
			if (open || stopping)
				return; // 已在运行，或stop拆除在飞

			ZezeCounter.tryInit();

			if (config == null)
				config = Config.load();
			config.parseCustomize(this.gcmConfig);
			// 所有请求依赖的配置须先于监听发布；Cleanup 也以非空配置作为禁用条件。
			achillesHeelConfig = new AchillesHeelConfig(gcmConfig.maxNetPing,
					gcmConfig.serverProcessTime, gcmConfig.serverReleaseTimeout);

			sessions = new LongConcurrentHashMap<>(4096);
			global = new ConcurrentHashMap<>(this.gcmConfig.initialCapacity);

			server = new ServerService(config);

			getServer().AddFactoryHandle(Acquire.TypeId_, new Service.ProtocolFactoryHandle<>(
					Acquire::new, this::processAcquireRequest, TransactionLevel.None, DispatchMode.Normal));
			getServer().AddFactoryHandle(Reduce.TypeId_, new Service.ProtocolFactoryHandle<>(
					Reduce::new, null, TransactionLevel.None, DispatchMode.Critical));
			getServer().AddFactoryHandle(Login.TypeId_, new Service.ProtocolFactoryHandle<>(
					Login::new, this::processLogin, TransactionLevel.None, DispatchMode.Critical));
			getServer().AddFactoryHandle(ReLogin.TypeId_, new Service.ProtocolFactoryHandle<>(
					ReLogin::new, this::processReLogin, TransactionLevel.None, DispatchMode.Critical));
			getServer().AddFactoryHandle(NormalClose.TypeId_, new Service.ProtocolFactoryHandle<>(
					NormalClose::new, this::processNormalClose, TransactionLevel.None, DispatchMode.Critical));
			// 临时注册到这里，安全起见应该起一个新的Service，并且仅绑定到 localhost。
			getServer().AddFactoryHandle(Cleanup.TypeId_, new Service.ProtocolFactoryHandle<>(
					Cleanup::new, this::processCleanup, TransactionLevel.None, DispatchMode.Direct));
			getServer().AddFactoryHandle(KeepAlive.TypeId_, new Service.ProtocolFactoryHandle<>(
					KeepAlive::new, GlobalCacheManagerServer::processKeepAliveRequest, TransactionLevel.None, DispatchMode.Direct));

			// 在开放端口监听前打开闸门，避免启动瞬间到达的 Login 被误拒；后续失败经 stop() 复位
			open = true;
			try {
				// perf 须在 open 之后创建，确保启动失败走 stop() 时能正确关闭其周期任务
				if (ENABLE_PERF)
					perf = new GlobalCacheManagerPerf("", serialIdGenerator);
				serverSocket = getServer().newServerSocket(ipaddress, port,
						new Acceptor(port, ipaddress != null ? ipaddress.getHostAddress() : null));

				achillesHeelDaemonTimer.start();
			} catch (RuntimeException e) {
				// 失败时经 stop() 拆除已建资源并复位 open，清理异常不掩盖原始异常
				try {
					stop();
				} catch (Throwable ex) {
					logger.error("start failed and stop cleanup exception: ", ex);
				}
				throw e;
			}
		} finally {
			unlock();
		}
	}

	private void achillesHeelDaemon() {
		var now = System.currentTimeMillis();

		getSessions().forEach(session -> {
			session.lock();
			try {
				// 必须在锁内复查超时，防止踢掉刚在检查与加锁之间完成重连的新连接
				if (now - session.getActiveTime() > getAchillesHeelConfig().globalDaemonTimeout && !session.debugMode) {
					session.kick();
					if (!session.acquired.isEmpty()) {
						var releaseCount = 0L;
						for (var k : session.acquired.keySet()) {
							try {
								release(session, k, false);
								++releaseCount;
							} catch (InterruptedException ex) {
								Thread.currentThread().interrupt();
								logger.warn("achillesHeelDaemon interrupted, abort remaining release. session={} key={}", session, k, ex);
								return; // 本轮被中断，退出由下轮继续
							}
						}
						session.setActiveTime(System.currentTimeMillis());
						if (releaseCount > 0)
							logger.info("AchillesHeelDaemon.Release session={} count={}", session, releaseCount);
					}
				}
			} finally {
				session.unlock();
			}
		});
	}

	public void stop() throws Exception {
		lock();
		try {
			if (!open)
				return; // 未启动或已在停机中
			open = false; // 先关闸门拒绝新绑定
			stopping = true; // 阻止停机两段之间 start() 重入
		} finally {
			unlock();
		}
		// 锁外先停守护定时器，避免持实例锁等待持 session 锁的扫描体
		achillesHeelDaemonTimer.stop();
		lock();
		try {
			// server 与 perf 保留引用不置 null，避免晚到扫描或并发派发解引用 NPE
			if (serverSocket != null) {
				serverSocket.close();
				serverSocket = null;
			}
			getServer().stop();
			if (perf != null)
				getPerf().close();
		} finally {
			stopping = false;
			unlock();
		}
	}

	/**
	 * 报告错误的时候带上相关信息（包括GlobalCacheManager和LogicServer等等）
	 * 手动Cleanup时，连接正确的服务器执行。
	 */
	private long processCleanup(Cleanup rpc) {
		logger.info("ProcessCleanup: {} RequestId={} {}", rpc.getSender(), rpc.getSessionId(), rpc.Argument);
		if (achillesHeelConfig != null) { // disable cleanup.
			logger.warn("ProcessCleanup: {} RequestId={} result={}", rpc.getSender(), rpc.getSessionId(), CleanupErrorDisabled);
			rpc.SendResultCode(CleanupErrorDisabled);
			return 0;
		}

		// 安全性以后加强。
		//noinspection DataFlowIssue
		if (!rpc.Argument.secureKey.equals("Ok! verify secure.")) {
			logger.warn("ProcessCleanup: {} RequestId={} result={}", rpc.getSender(), rpc.getSessionId(), CleanupErrorSecureKey);
			rpc.SendResultCode(CleanupErrorSecureKey);
			return 0;
		}

		var session = getSessions().computeIfAbsent(rpc.Argument.serverId, __ -> new CacheHolder());
		ArrayList<Binary> releaseKeys;
		long fireGeneration;
		//noinspection DataFlowIssue
		session.lock();
		try {
		if (session.globalCacheManagerHashIndex != rpc.Argument.globalCacheManagerHashIndex) {
			// 多点验证
			logger.warn("ProcessCleanup: {} RequestId={} result={}", rpc.getSender(), rpc.getSessionId(), CleanupErrorGlobalCacheManagerHashIndex);
			rpc.SendResultCode(CleanupErrorGlobalCacheManagerHashIndex);
			return 0;
		}

		if (getServer().GetSocket(session.sessionId) != null) {
			// 连接存在，禁止cleanup。
			logger.warn("ProcessCleanup: {} RequestId={} result={}", rpc.getSender(), rpc.getSessionId(), CleanupErrorHasConnection);
			rpc.SendResultCode(CleanupErrorHasConnection);
			return 0;
		}
			releaseKeys = new ArrayList<>(session.acquired.keySet());
			fireGeneration = session.generation;
		} finally {
			session.unlock();
		}

		// 还有更多的防止出错的手段吗？

		// XXX verify danger
		TaskSpec.ofAction(() -> { // delay 5 mins
			for (var k : releaseKeys) {
				// ConcurrentDictionary 可以在循环中删除。这样虽然效率低些，但是能处理更多情况。
				release(session, k, false, fireGeneration);
			}
			rpc.SendResultCode(0);
		}).schedule(5 * 60 * 1000);

		return 0;
	}

	// 客户端契约：须收到 Login 成功应答后才发送 Acquire/Release，否则预发请求可能被旧权限回收误伤
	private long processLogin(Login rpc) throws Exception {
		logger.info("ProcessLogin: {} RequestId={} {}", rpc.getSender(), rpc.getSessionId(), rpc.Argument);
		var session = getSessions().computeIfAbsent(rpc.Argument.serverId, __ -> new CacheHolder());
		ArrayList<Binary> releaseKeys;
		long fireGeneration;
		//noinspection DataFlowIssue
		session.lock();
		try {
			if (!session.tryBindSocket(Objects.requireNonNull(rpc.getSender()), rpc.Argument.globalCacheManagerHashIndex, true)) {
				rpc.SendResultCode(LoginBindSocketFail);
				return 0;
			}
			releaseKeys = new ArrayList<>(session.acquired.keySet());
			fireGeneration = session.generation;
			session.setActiveTime(System.currentTimeMillis());
			session.setDebugMode(rpc.Argument.debugMode);
		} finally {
			session.unlock();
		}
		// 新登录（如逻辑服重启）：仅释放绑定时刻快照到的旧权限
		for (var k : releaseKeys) {
			release(session, k, false, fireGeneration);
		}
		rpc.Result.maxNetPing = gcmConfig.maxNetPing;
		rpc.Result.serverProcessTime = gcmConfig.serverProcessTime;
		rpc.Result.serverReleaseTimeout = gcmConfig.serverReleaseTimeout;
		rpc.SendResultCode(0);
		return 0;
	}

	private long processReLogin(ReLogin rpc) {
		logger.info("ProcessReLogin: {} RequestId={} {}", rpc.getSender(), rpc.getSessionId(), rpc.Argument);
		var session = getSessions().computeIfAbsent(rpc.Argument.serverId, __ -> new CacheHolder());
		//noinspection DataFlowIssue
		if (!session.tryBindSocket(Objects.requireNonNull(rpc.getSender()), rpc.Argument.globalCacheManagerHashIndex, false)) {
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
		//noinspection DataFlowIssue
		var session = (CacheHolder)rpc.getSender().getUserState();
		if (session == null) {
			logger.warn("ProcessNormalClose: {} RequestId={} result={}", rpc.getSender(), rpc.getSessionId(), AcquireNotLogin);
			rpc.SendResultCode(AcquireNotLogin);
			return 0; // not login
		}
		// 解绑前在锁内快照待释放集合与世代号，避免解绑后同 serverId 新连接写入的新权限被误回收
		ArrayList<Binary> releaseKeys;
		long fireGeneration;
		session.lock();
		try {
			releaseKeys = new ArrayList<>(session.acquired.keySet());
			fireGeneration = session.generation;
			if (!session.tryUnBindSocket(rpc.getSender())) {
			logger.warn("ProcessNormalClose: {} RequestId={} result={}", rpc.getSender(), rpc.getSessionId(), NormalCloseUnbindFail);
			rpc.SendResultCode(NormalCloseUnbindFail);
			return 0;
			}
		} finally {
			session.unlock();
		}
		// 整个释放循环共用解绑时刻的世代号，防止重绑后后续 key 绕过世代守卫误杀新所有权
		for (var k : releaseKeys) {
			release(session, k, false, fireGeneration);
		}
		rpc.SendResultCode(0);
		logger.info("After NormalClose global.Count={}", getGlobal().size());
		return 0;
	}

	private static long processKeepAliveRequest(KeepAlive rpc) {
		//noinspection DataFlowIssue
		var userState = rpc.getSender().getUserState(); // 单次读取：检查与转型之间状态可能被并发清除
		if (userState == null) {
			rpc.SendResultCode(AcquireNotLogin);
			return 0;
		}
		var sender = (CacheHolder)userState;
		sender.setActiveTime(System.currentTimeMillis());
		rpc.SendResult();
		return 0;
	}

	private long processAcquireRequest(Acquire rpc) {
		var acquireState = rpc.Argument.state;
		if (ENABLE_PERF)
			getPerf().onAcquireBegin(rpc, acquireState);
		rpc.Result.globalKey = rpc.Argument.globalKey;
		rpc.Result.state = acquireState; // default success

		//noinspection DataFlowIssue
		var userState = rpc.getSender().getUserState(); // 单次读取：检查与转型之间状态可能被并发清除
		if (userState == null) {
			rpc.Result.state = StateInvalid;
			rpc.SendResultCode(AcquireNotLogin);
		} else {
			try {
				var sender = (CacheHolder)userState;
				sender.setActiveTime(System.currentTimeMillis());
				switch (acquireState) {
				case StateInvalid: // release
						long fireGeneration;
						sender.lock();
						try {
							fireGeneration = rpc.getSender().getUserState() == sender
									&& sender.sessionId == rpc.getSender().getSessionId() ? sender.generation : -1;
						} finally {
							sender.unlock();
						}
						if (fireGeneration == -1) {
							rpc.Result.state = StateInvalid;
							rpc.SendResultCode(AcquireNotLogin);
						} else {
							rpc.Result.state = release(sender, Objects.requireNonNull(rpc.Argument.globalKey), true, fireGeneration);
							rpc.SendResultCode(0);
						}
					break;
				case StateShare:
					acquireShare(rpc, sender); //await 方法内有等待
					break;
				case StateModify:
					acquireModify(rpc, sender); //await 方法内有等待
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
			getPerf().onAcquireEnd(rpc, acquireState);
		return 0;
	}

	private int release(CacheHolder sender, Binary _gKey, boolean noWait) throws InterruptedException {
		// daemon（kick后锁内串行）与acquire的StateInvalid释放（rpc作用域）按各自进入时刻判定
		return release(sender, _gKey, noWait, sender.generation);
	}

	private int release(CacheHolder sender, Binary _gKey, boolean noWait, long fireGeneration) throws InterruptedException {
		// 一次操作固定所属运行域；stop/start会替换实例global，旧等待者不能重试到新map。
		var operationGlobal = getGlobal();
		while (true) {
			CacheState cs = operationGlobal.computeIfAbsent(_gKey, CacheState::new);
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
				if (sender.generation != fireGeneration) {
					// 世代已更替，跳过旧世代释放并按实际持有状态应答
					return cs.getSenderCacheState(sender);
				}
				cs.acquireStatePending = StateRemoving;

				if (cs.modify == sender)
					cs.modify = null;
				cs.share.remove(sender); // always try remove
				sender.acquired.remove(gKey);

				if (cs.modify == null && cs.share.isEmpty()) {
					// 安全的从global中删除，没有并发问题。
					cs.acquireStatePending = StateRemoved;
					operationGlobal.remove(gKey, cs);
				} else
					cs.acquireStatePending = StateInvalid;
				cs.signalAll(); //notify
				return StateInvalid;
			} finally {
				cs.unlock();
			}
		}
	}

	private void acquireShare(Acquire rpc, CacheHolder sender) throws InterruptedException {
		var operationGlobal = getGlobal();
		while (true) {
			CacheState cs = operationGlobal.computeIfAbsent(Objects.requireNonNull(rpc.Argument.globalKey), CacheState::new);
			// 异常逃逸时由 finally 兜底复位占住的申请位，防止该 key 永久冻结
			var ownsPending = false;
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
							return;
						}
						break;
					case StateModify:
						if (cs.modify == Objects.requireNonNull(sender) || cs.share.contains(sender)) {
							if (isDebugEnabled)
								logger.debug("2 {} {} {}", sender, StateShare, cs);
							rpc.Result.state = StateInvalid;
							rpc.SendResultCode(AcquireShareDeadLockFound);
							return;
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
				ownsPending = true;
				serialIdGenerator.getAndIncrement();

				var gKey = cs.globalKey;
				var reduceTid = new OutObject<>(Id128.Zero);
				if (cs.modify != null) {
					if (cs.modify == sender) {
						// 已经是Modify又申请，可能是sender异常关闭，
						// 又重启连上。更新一下。应该是不需要的。
						sender.acquired.put(gKey, StateModify);
						cs.acquireStatePending = StateInvalid;
						cs.signalAll(); // 归还申请位必须唤醒等待者
						if (isDebugEnabled)
							logger.debug("4 {} {} {}", sender, StateShare, cs);
						rpc.Result.state = StateModify;
						rpc.SendResultCode(AcquireShareAlreadyIsModify);
						return;
					}

					var reduceResultState = new OutInt(StateReduceNetError); // 默认网络错误。
					var reduceDone = new boolean[]{false}; // 完成标志（回调锁内置位后再pulse）
					if (cs.modify.reduce(gKey, rpc.getResultCode(), r -> { //await 方法内有等待
						if (ENABLE_PERF)
							getPerf().onReduceEnd(r);
						if (r.isTimeout()) {
							reduceResultState.value = StateReduceRpcTimeout;
						} else if (!GlobalCacheManagerConst.isReduceResultCodeSuccess(r.getResultCode())) {
							// 错误应答的默认StateInvalid不能证明降级；明确的幂等确认码仍允许使用结果。
							reduceResultState.value = StateReduceNetError;
						} else {
							reduceResultState.value = r.Result.state;
							reduceTid.value = r.Result.reducedTid;
						}
						cs.lock();
						try {
							reduceDone[0] = true;
							cs.signalAll(); //notify
						} finally {
							cs.unlock();
						}
						return 0;
					})) {
						if (isDebugEnabled)
							logger.debug("5 {} {} {}", sender, StateShare, cs);
						// 循环复查完成谓词，防止 Condition 伪唤醒提前走失败分支
						while (!reduceDone[0])
							cs.await();
					}
					var oldModify = cs.modify;
					switch (reduceResultState.value) {
					case StateShare:
						cs.modify = null;
						oldModify.acquired.put(gKey, StateShare);
						cs.share.add(oldModify); // 降级成功。
						break;

					case StateInvalid:
						// 降到了 Invalid，此时就不需要加入 Share 了。
						cs.modify = null;
						oldModify.acquired.remove(gKey);
						break;

					case StateReduceErrorFreshAcquire:
						cs.acquireStatePending = StateInvalid;
						cs.signalAll(); //notify
						if (ENABLE_PERF)
							getPerf().onOthers("XXX Fresh " + StateShare);
						rpc.Result.state = StateInvalid;
						rpc.SendResultCode(StateReduceErrorFreshAcquire);
						return;

					default:
						// 包含协议返回错误的值的情况。
						// case StateReduceRpcTimeout: // 11
						// case StateReduceException: // 12
						// case StateReduceNetError: // 13
						cs.acquireStatePending = StateInvalid;
						cs.signalAll(); //notify
						if (ENABLE_PERF)
							getPerf().onOthers("XXX 8 " + StateShare + " " + reduceResultState.value);
						rpc.Result.state = StateInvalid;
						rpc.SendResultCode(AcquireShareFailed);
						return;
					}

					sender.acquired.put(gKey, StateShare);
					cs.share.add(sender);
					cs.acquireStatePending = StateInvalid;
					cs.signalAll(); //notify
					if (isDebugEnabled)
						logger.debug("6 {} {} {}", sender, StateShare, cs);
					rpc.Result.reducedTid = reduceTid.value;
					rpc.SendResultCode(0);
					return;
				}

				//noinspection DataFlowIssue
				sender.acquired.put(gKey, StateShare);
				cs.share.add(sender);
				cs.acquireStatePending = StateInvalid;
				cs.signalAll(); //notify
				if (isDebugEnabled)
					logger.debug("7 {} {} {}", sender, StateShare, cs);
				rpc.Result.reducedTid = reduceTid.value;
				rpc.SendResultCode(0);
				return;
			} finally {
				// 异常逃逸时复位本次占住的申请位并唤醒等待者；正常路径已自行复位为StateInvalid
				if (ownsPending && (cs.acquireStatePending == StateShare || cs.acquireStatePending == StateModify)) {
					cs.acquireStatePending = StateInvalid;
					cs.signalAll(); //notify
				}
				cs.unlock();
			}
		}
	}

	private void acquireModify(Acquire rpc, CacheHolder sender) throws InterruptedException {
		var operationGlobal = getGlobal();
		while (true) {
			CacheState cs = operationGlobal.computeIfAbsent(Objects.requireNonNull(rpc.Argument.globalKey), CacheState::new);
			// 申请位占住后异常逃逸不复位会冻结key（理由同acquireShare）；finally兜底复位。
			var ownsPending = false;
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
							return;
						}
						break;
					case StateModify:
						if (cs.modify == Objects.requireNonNull(sender) || cs.share.contains(sender)) {
							if (isDebugEnabled)
								logger.debug("2 {} {} {}", sender, StateModify, cs);
							rpc.Result.state = StateInvalid;
							rpc.SendResultCode(AcquireModifyDeadLockFound);
							return;
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
				ownsPending = true;
				serialIdGenerator.getAndIncrement();

				var gKey = cs.globalKey;
				var reduceTid = new OutObject<>(Id128.Zero);
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
						return;
					}

					var reduceResultState = new OutInt(StateReduceNetError); // 默认网络错误。
					var reduceDone = new boolean[]{false}; // 完成标志（回调锁内置位后再pulse）
					if (cs.modify.reduce(gKey, rpc.getResultCode(), r -> { //await 方法内有等待
						if (ENABLE_PERF)
							getPerf().onReduceEnd(r);
						if (r.isTimeout()) {
							reduceResultState.value = StateReduceRpcTimeout;
						} else if (!GlobalCacheManagerConst.isReduceResultCodeSuccess(r.getResultCode())) {
							// 错误应答的默认StateInvalid不能证明降级；明确的幂等确认码仍允许使用结果。
							reduceResultState.value = StateReduceNetError;
						} else {
							reduceResultState.value = r.Result.state;
							reduceTid.value = r.Result.reducedTid;
						}
						cs.lock();
						try {
							reduceDone[0] = true;
							cs.signalAll(); //notify
						} finally {
							cs.unlock();
						}
						return 0;
					})) {
						if (isDebugEnabled)
							logger.debug("5 {} {} {}", sender, StateModify, cs);
						// 同Share侧——谓词循环防伪唤醒直走失败分支。
						while (!reduceDone[0])
							cs.await(); //await 等通知
					}

					var oldModify = cs.modify;
					switch (reduceResultState.value) {
					case StateInvalid:
						cs.modify = null;
						oldModify.acquired.remove(gKey);
						break; // reduce success

					case StateReduceErrorFreshAcquire:
						cs.acquireStatePending = StateInvalid;
						cs.signalAll(); //notify
						if (ENABLE_PERF)
							getPerf().onOthers("XXX Fresh " + StateModify);
						rpc.Result.state = StateInvalid;
						rpc.SendResultCode(StateReduceErrorFreshAcquire);
						return;

					default:
						// case StateReduceRpcTimeout: // 11
						// case StateReduceException: // 12
						// case StateReduceNetError: // 13
						cs.acquireStatePending = StateInvalid;
						cs.signalAll(); //notify
						if (ENABLE_PERF)
							getPerf().onOthers("XXX 9 " + StateModify + " " + reduceResultState.value);
						rpc.Result.state = StateInvalid;
						rpc.SendResultCode(AcquireModifyFailed);
						return;
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
					return;
				}

				ArrayList<KVNotNull<CacheHolder, Reduce>> reducePending = new ArrayList<>();
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
					reducePending.add(KVNotNull.create(c, reduce));
				}

				// 两种情况不需要发reduce
				// 1. share是空的, 可以直接升为Modify
				// 2. sender是share, 而且reducePending的size是0
				var errorFreshAcquire = new OutObject<>(Boolean.FALSE);
				var waitReduceDone = new boolean[]{false}; // runNow完成标志（锁内置位）
				if (!cs.share.isEmpty() && (!senderIsShare || !reducePending.isEmpty())) {
					TaskSpec.ofAction(() -> {
						// 一个个等待是否成功。WaitAll 碰到错误不知道怎么处理的，
						// 应该也会等待所有任务结束（包括错误）。
						var freshAcquire = false;
						for (var reduce : reducePending) {
							try {
								//noinspection DataFlowIssue
								reduce.getValue().getFuture().await(); //await 等RPC回复
									if (reduce.getValue().isTimeout()
											|| !GlobalCacheManagerConst.isReduceResultCodeSuccess(reduce.getValue().getResultCode())) {
										reduce.getKey().setError();
										continue;
									}
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
							} finally {
								if (ENABLE_PERF) {
									var r = reduce.getValue();
									var future = r.getFuture();
									if (r.isTimeout() || future != null && future.isDone() && !future.isCompletedExceptionally())
										getPerf().onReduceEnd(r);
									else
										getPerf().onReduceCancel(r);
								}
							}
						}
						errorFreshAcquire.value = freshAcquire;
						cs.lock();
						try {
							waitReduceDone[0] = true;
							cs.signalAll(); //notify
						} finally {
							cs.unlock();
						}
					}).name("GlobalCacheManager.AcquireModify.WaitReduce").runNow();
					if (isDebugEnabled)
						logger.debug("7 {} {} {}", sender, StateModify, cs);
					// 循环复查完成谓词，防止 Condition 伪唤醒提前读取并发填充中的 reduceSucceed
					while (!waitReduceDone[0])
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
					//noinspection DataFlowIssue
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
					// 失败了，要把原来是share的sender恢复。
					if (senderIsShare)
						cs.share.add(sender);
					cs.acquireStatePending = StateInvalid;
					cs.signalAll(); //notify
					if (ENABLE_PERF)
						getPerf().onOthers("XXX 10 " + StateModify + ' ' + errorFreshAcquire.value);
					rpc.Result.state = StateInvalid;
					//noinspection DataFlowIssue
					if (errorFreshAcquire.value)
						rpc.SendResultCode(StateReduceErrorFreshAcquire); // 这个错误不看做失败，允许发送方继续尝试。
					else
						rpc.SendResultCode(AcquireModifyFailed);
				}
				return;
			} finally {
				// 异常逃逸时复位本次占住的申请位并唤醒等待者；正常路径已自行复位为StateInvalid
				if (ownsPending && (cs.acquireStatePending == StateShare || cs.acquireStatePending == StateModify)) {
					cs.acquireStatePending = StateInvalid;
					cs.signalAll(); //notify
				}
				cs.unlock();
			}
		}
	}

	private static final class CacheState extends ReentrantLock {
		final Binary globalKey; // 这里的引用同global map的key,用于给CacheHolder里的map相同的key引用
		final IdentityHashSet<CacheHolder> share = new IdentityHashSet<>();
		@Nullable CacheHolder modify;
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
		private volatile boolean debugMode;
		// 世代号：kick 与重绑均在 session 锁内递增；release 进入时捕获、移除前复查，不匹配则跳过以防误杀新连接所有权
		private volatile long generation;

		// 与绑定共用session锁，代次递增不会丢失。
		void kick() {
			lock();
			try {
				var peer = instance.getServer().GetSocket(sessionId);
				if (null != peer) {
					peer.setUserState(null); // 来自这个Agent的所有请求都会失败。
					peer.close(kickException); // 关闭连接，强制Agent重新登录。
				}
				sessionId = 0; // 清除网络状态。
				//noinspection NonAtomicOperationOnVolatileField
				++generation; // 推进世代，使在飞的旧 release 失效
			} finally {
				unlock();
			}
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
				// ReLogin 要求同一 serverId 已完成过 Login 绑定，用来检测 GCM 重启。
				if (!login && !logined)
					return false;
				if (newSocket.getUserState() != null) {
					logger.warn("TryBindSocket: already bound! newSocket.getUserState() != null, SessionId={}", newSocket.getSessionId());
					return false; // 不允许再次绑定。Login Or ReLogin 只能发一次。
				}

				if (!instance.open)
					return false;
				var socket = instance.getServer().GetSocket(sessionId);
				if (socket == null) {
					// 旧连接已不存在或已断开
					sessionId = newSocket.getSessionId();
					newSocket.setUserState(this);
					if (login)
						logined = true;
					globalCacheManagerHashIndex = _GlobalCacheManagerHashIndex;
					// 锁内刷新活跃时间，避免守护线程按旧超时踢掉刚登录的新连接
					activeTime = System.currentTimeMillis();
					//noinspection NonAtomicOperationOnVolatileField
					++generation; // 重绑推进新世代，使之前在飞的 release 失效
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

				var current = instance.getServer().GetSocket(sessionId);
				if (current != null && current != oldSocket)
					return false; // not same socket

				sessionId = 0;
				oldSocket.setUserState(null); // 已解绑连接的迟到请求不能使用后来重绑的Holder代次
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
				if (System.currentTimeMillis() - lastErrorTime < instance.getAchillesHeelConfig().globalForbidPeriod)
					return false;
				AsyncSocket peer = instance.getServer().GetSocket(sessionId);
				if (peer != null) {
					var reduce = new Reduce(gkey, StateInvalid);
					reduce.setResultCode(fresh);
					if (ENABLE_PERF)
						instance.getPerf().onReduceBegin(reduce);
					if (reduce.Send(peer, response, instance.getAchillesHeelConfig().reduceTimeout)) //await 等RPC回复
						return true;
					if (ENABLE_PERF)
						instance.getPerf().onReduceCancel(reduce);
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
			if (now - lastErrorTime > instance.getAchillesHeelConfig().globalForbidPeriod)
				lastErrorTime = now;
		}

		/**
		 * 返回null表示发生了网络错误，或者应用服务器已经关闭。
		 */
		@Nullable Reduce reduceWaitLater(Binary gkey, long fresh) {
			Reduce reduce = null;
			try {
				if (System.currentTimeMillis() - lastErrorTime < instance.getAchillesHeelConfig().globalForbidPeriod)
					return null;
				AsyncSocket peer = instance.getServer().GetSocket(sessionId);
				if (peer != null) {
					reduce = new Reduce(gkey, StateInvalid);
					reduce.setResultCode(fresh);
					if (ENABLE_PERF)
						instance.getPerf().onReduceBegin(reduce);
					reduce.SendForWait(peer, instance.getAchillesHeelConfig().reduceTimeout);
					return reduce;
				}
				logger.warn("Send Reduce failed. SessionId={}, gkey={}", sessionId, gkey);
			} catch (Throwable ex) { // 这里的异常只应该是网络发送异常。
				if (ENABLE_PERF && reduce != null)
					instance.getPerf().onReduceCancel(reduce);
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
			// so.UserState 在 Login/ReLogin 的时候初始化。
			super.OnSocketAccept(so);
		}

		@Override
		public void OnSocketClose(AsyncSocket so, @Nullable Throwable e) throws Exception {
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
			} catch (Throwable e) {
				logger.error("dispatchRpcResponse exception:", e);
			}
		}
	}

	public static void main(String [] args) throws Exception {
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
				ip = requireValue(args, ++i, "-ip");
				break;
			case "-port":
				port = requireInt(args, ++i, "-port");
				break;
			case "-raft":
				raftName = requireValue(args, ++i, "-raft");
				break;
			case "-raftConf":
				raftConf = requireValue(args, ++i, "-raftConf");
				break;
			case "-threads":
				i++;
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
