package Zeze.Services;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Arch.RedirectFuture;
import Zeze.Config;
import Zeze.Net.Acceptor;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolHandle;
import Zeze.Net.Rpc;
import Zeze.Net.Selectors;
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
import Zeze.Util.AbstractAsyncLock;
import Zeze.Util.Action0;
import Zeze.Util.Action1;
import Zeze.Util.AsyncLock;
import Zeze.Util.DaemonTimer;
import Zeze.Util.Id128;
import Zeze.Util.IdentityHashSet;
import Zeze.Util.InlineAsyncLock;
import Zeze.Util.KVNotNull;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.OutObject;
import Zeze.Util.Task;
import Zeze.Util.TaskSpec;
import Zeze.Util.ThreadFactoryWithName;
import Zeze.Util.ZezeCounter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.jspecify.annotations.Nullable;
import static Zeze.Util.Args.requireInt;
import static Zeze.Util.Args.requireValue;

/**
 * 异步版全局缓存管理器服务器：管理cache-sync的全局锁所有权协商（Acquire/Reduce/Release），
 * 协议处理几乎无阻塞、直接跑在IO线程上，每个GlobalTableKey一把AsyncLock串行化。
 */
public final class GlobalCacheManagerAsyncServer extends ReentrantLock implements GlobalCacheManagerConst {
	static {
		var level = Level.toLevel(System.getProperty("logLevel"), Level.INFO);
		((LoggerContext)LogManager.getContext(false)).getConfiguration().getRootLogger().setLevel(level);
	}

	private static final boolean ENABLE_PERF = true;
	private static final Logger logger = LogManager.getLogger(GlobalCacheManagerAsyncServer.class);
	private static final boolean isDebugEnabled = logger.isDebugEnabled();
	// -tryNextSync 启动参数置位，main里start前设置、之后只读；创建CacheState时选择锁实现。
	private static volatile boolean useSyncLock;

	// stop 仅停止对象而不置 null，避免在飞协议或晚到守护解引用 NPE；open 作为活/死闸门
	private volatile @Nullable ServerService server;
	private @Nullable AsyncSocket serverSocket;
	private @Nullable ConcurrentHashMap<Binary, CacheState> global;
	// 每次 start 捕获共享池；启动方负责在 GCM 在途业务结束后关闭池，重建池时应重启 GCM
	private @Nullable Executor lockExecutor;
	private final AtomicLong serialIdGenerator = new AtomicLong();
	// 会话表：key 为 ServerId，Login/ReLogin 时按需创建且不删除（ServerId 数量有限）
	private @Nullable LongConcurrentHashMap<CacheHolder> sessions;
	private final GlobalCacheManagerServer.GCMConfig gcmConfig = new GlobalCacheManagerServer.GCMConfig();
	private @Nullable AchillesHeelConfig achillesHeelConfig;
	private final DaemonTimer achillesHeelDaemonTimer =
			new DaemonTimer("GlobalCacheManagerAsync.AchillesHeelDaemon", 5000, this::achillesHeelDaemon);
	// open 为受理新绑定的闸门；stopping 仅在实例锁内维护，防止 stop 两段之间 start() 重入
	private volatile boolean open;
	private boolean stopping;
	private @Nullable GlobalCacheManagerPerf perf;

	private ServerService getServer() {
		var s = server;
		assert s != null : "server has not been initialized";
		return s;
	}

	private ConcurrentHashMap<Binary, CacheState> getGlobal() {
		var g = global;
		assert g != null : "global has not been initialized";
		return g;
	}

	private LongConcurrentHashMap<CacheHolder> getSessions() {
		var s = sessions;
		assert s != null : "sessions have not been initialized";
		return s;
	}

	private AchillesHeelConfig getAchillesHeelConfig() {
		var config = achillesHeelConfig;
		assert config != null : "achillesHeelConfig has not been initialized";
		return config;
	}

	private GlobalCacheManagerPerf getPerf() {
		var p = perf;
		assert p != null : "perf has not been initialized";
		return p;
	}


	// 每个实例都是独立的服务器（无共享静态状态），可同 JVM 启动多个监听不同端口。
	public GlobalCacheManagerAsyncServer() {
	}

	// 外面主动提供装载配置，需要在Load之前把这个实例注册进去。
	public GlobalCacheManagerServer.GCMConfig getGcmConfig() {
		return gcmConfig;
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
			config.parseCustomize(gcmConfig);
			// 所有请求依赖的配置须先于监听发布；Cleanup 也以非空配置作为禁用条件。
			achillesHeelConfig = new AchillesHeelConfig(gcmConfig.maxNetPing,
					gcmConfig.serverProcessTime, gcmConfig.serverReleaseTimeout);
			lockExecutor = useSyncLock ? null : Task.getThreadPool();

			sessions = new LongConcurrentHashMap<>(4096);
			global = new ConcurrentHashMap<>(gcmConfig.initialCapacity);

			server = new ServerService(config);

			getServer().AddFactoryHandle(Acquire.TypeId_, new Service.ProtocolFactoryHandle<>(
					Acquire::new, this::processAcquireRequest, TransactionLevel.None, DispatchMode.Direct));
			getServer().AddFactoryHandle(Reduce.TypeId_, new Service.ProtocolFactoryHandle<>(
					Reduce::new, null, TransactionLevel.None, DispatchMode.Direct));
			getServer().AddFactoryHandle(Login.TypeId_, new Service.ProtocolFactoryHandle<>(
					Login::new, this::processLogin, TransactionLevel.None, DispatchMode.Direct));
			getServer().AddFactoryHandle(ReLogin.TypeId_, new Service.ProtocolFactoryHandle<>(
					ReLogin::new, this::processReLogin, TransactionLevel.None, DispatchMode.Direct));
			getServer().AddFactoryHandle(NormalClose.TypeId_, new Service.ProtocolFactoryHandle<>(
					NormalClose::new, this::processNormalClose, TransactionLevel.None, DispatchMode.Direct));
			// 临时注册到这里，安全起见应该起一个新的Service，并且仅绑定到 localhost。
			getServer().AddFactoryHandle(Cleanup.TypeId_, new Service.ProtocolFactoryHandle<>(
					Cleanup::new, this::processCleanup, TransactionLevel.None, DispatchMode.Direct));
			getServer().AddFactoryHandle(KeepAlive.TypeId_, new Service.ProtocolFactoryHandle<>(
					KeepAlive::new, GlobalCacheManagerAsyncServer::processKeepAliveRequest,
					TransactionLevel.None, DispatchMode.Direct));

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
						var allReleaseFuture = new CountDownFuture();
						for (var k : session.acquired.keySet()) {
							releaseAsync(session, k, allReleaseFuture.createOne());
							++releaseCount;
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
			logger.warn("ProcessCleanup: {} RequestId={} result={}",
					rpc.getSender(), rpc.getSessionId(), CleanupErrorDisabled);
			rpc.SendResultCode(CleanupErrorDisabled);
			return 0;
		}

		// 安全性以后加强。
		//noinspection DataFlowIssue
		if (!rpc.Argument.secureKey.equals("Ok! verify secure.")) {
			logger.warn("ProcessCleanup: {} RequestId={} result={}",
					rpc.getSender(), rpc.getSessionId(), CleanupErrorSecureKey);
			rpc.SendResultCode(CleanupErrorSecureKey);
			return 0;
		}

		var session = getSessions().computeIfAbsent(rpc.Argument.serverId, __ -> new CacheHolder(this));
		ArrayList<Binary> releaseKeys;
		long fireGeneration;
		//noinspection DataFlowIssue
		session.lock();
		try {
		if (session.globalCacheManagerHashIndex != rpc.Argument.globalCacheManagerHashIndex) {
			// 多点验证
			logger.warn("ProcessCleanup: {} RequestId={} result={}",
					rpc.getSender(), rpc.getSessionId(), CleanupErrorGlobalCacheManagerHashIndex);
			rpc.SendResultCode(CleanupErrorGlobalCacheManagerHashIndex);
			return 0;
		}

		if (getServer().GetSocket(session.sessionId) != null) {
			// 连接存在，禁止cleanup。
			logger.warn("ProcessCleanup: {} RequestId={} result={}",
					rpc.getSender(), rpc.getSessionId(), CleanupErrorHasConnection);
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
			var allReleaseFuture = new CountDownFuture();
			for (var k : releaseKeys) {
				// ConcurrentDictionary 可以在循环中删除。这样虽然效率低些，但是能处理更多情况。
				releaseAsync(session, k, allReleaseFuture.createOne(), fireGeneration);
			}
			allReleaseFuture.then(__ -> rpc.SendResultCode(0));
		}).schedule(5 * 60 * 1000);

		return 0;
	}

	private long processLogin(Login rpc) {
		logger.info("ProcessLogin: {} RequestId={} {}", rpc.getSender(), rpc.getSessionId(), rpc.Argument);
		var session = getSessions().computeIfAbsent(rpc.Argument.serverId, __ -> new CacheHolder(this));
		ArrayList<Binary> releaseKeys;
		long fireGeneration;
		//noinspection DataFlowIssue
		session.lock();
		try {
			//noinspection DataFlowIssue
			if (!session.tryBindSocket(rpc.getSender(), rpc.Argument.globalCacheManagerHashIndex, true)) {
				rpc.SendResultCode(LoginBindSocketFail);
				return 0;
			}
			session.setActiveTime(System.currentTimeMillis());
			session.setDebugMode(rpc.Argument.debugMode);
			releaseKeys = new ArrayList<>(session.acquired.keySet());
			fireGeneration = session.generation;
		} finally {
			session.unlock();
		}
		// 新登录（如逻辑服重启）：仅释放绑定时刻快照到的旧权限，避免误回收后续新 Acquire
		var allReleaseFuture = new CountDownFuture();
		for (var k : releaseKeys) {
			releaseAsync(session, k, allReleaseFuture.createOne(), fireGeneration);
		}
		rpc.Result.maxNetPing = gcmConfig.maxNetPing;
		rpc.Result.serverProcessTime = gcmConfig.serverProcessTime;
		rpc.Result.serverReleaseTimeout = gcmConfig.serverReleaseTimeout;
		allReleaseFuture.then(__ -> rpc.SendResultCode(0));
		return 0;
	}

	private long processReLogin(ReLogin rpc) {
		logger.info("ProcessReLogin: {} RequestId={} {}", rpc.getSender(), rpc.getSessionId(), rpc.Argument);
		var session = getSessions().computeIfAbsent(rpc.Argument.serverId, __ -> new CacheHolder(this));
		//noinspection DataFlowIssue
		if (!session.tryBindSocket(rpc.getSender(), rpc.Argument.globalCacheManagerHashIndex, false)) {
			rpc.SendResultCode(ReLoginBindSocketFail);
			return 0;
		}
		session.setActiveTime(System.currentTimeMillis());
		session.setDebugMode(rpc.Argument.debugMode);
		rpc.SendResultCode(0);
		return 0;
	}

	private long processNormalClose(NormalClose rpc) {
		logger.info("ProcessNormalClose: {} RequestId={}", rpc.getSender(), rpc.getSessionId());
		//noinspection DataFlowIssue
		var session = (CacheHolder)rpc.getSender().getUserState();
		if (session == null) {
			logger.warn("ProcessNormalClose: {} RequestId={} result={}",
					rpc.getSender(), rpc.getSessionId(), AcquireNotLogin);
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
			logger.warn("ProcessNormalClose: {} RequestId={} result={}",
					rpc.getSender(), rpc.getSessionId(), NormalCloseUnbindFail);
			rpc.SendResultCode(NormalCloseUnbindFail);
			return 0;
			}
		} finally {
			session.unlock();
		}
		var allReleaseFuture = new CountDownFuture();
		for (var k : releaseKeys) {
			releaseAsync(session, k, allReleaseFuture.createOne(), fireGeneration);
		}
		allReleaseFuture.then(__ -> {
			rpc.SendResultCode(0);
			logger.info("After NormalClose global.Count={}", getGlobal().size());
		});
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
					releaseAsync(rpc);
					return 0;
				case StateShare:
					acquireShareAsync(rpc);
					return 0;
				case StateModify:
					acquireModifyAsync(rpc);
					return 0;
				default:
					rpc.Result.state = StateInvalid;
					rpc.SendResultCode(AcquireErrorState);
					break;
				}
			} catch (Throwable ex) { // rpc response.
				logger.error("ProcessAcquireRequest", ex);
				rpc.Result.state = StateInvalid;
				rpc.SendResultCode(AcquireException);
			}
		}
		if (ENABLE_PERF)
			getPerf().onAcquireEnd(rpc, acquireState);
		return 0;
	}

	public static final class CountDownFuture extends RedirectFuture<@Nullable Object> {
		private static final VarHandle vhCounter;

		static {
			try {
				vhCounter = MethodHandles.lookup().findVarHandle(CountDownFuture.class, "counter", int.class);
			} catch (ReflectiveOperationException e) {
				throw new ExceptionInInitializerError(e);
			}
		}

		private volatile @SuppressWarnings("unused") int counter;

		public CountDownFuture createOne() {
			vhCounter.getAndAdd(this, 1);
			return this;
		}

		public void finishOne() {
			if ((int)vhCounter.getAndAdd(this, -1) == 0)
				setResult(null);
		}

		@Override
		public RedirectFuture<@Nullable Object> then(Action1<@Nullable Object> onResult) {
			finishOne();
			return super.then(onResult);
		}
	}

	private void releaseAsync(CacheHolder sender, Binary _gKey, CountDownFuture future) {
		releaseAsync(sender, _gKey, future, sender.generation);
	}

	private void releaseAsync(CacheHolder sender, Binary _gKey,
			CountDownFuture future, long fireGeneration) {
		releaseAsync(sender, _gKey, future, fireGeneration, getGlobal());
	}

	// 固定操作所属map，含StateRemoved重试；stop/start后旧回调只收尾旧运行域。
	private void releaseAsync(CacheHolder sender, Binary _gKey, CountDownFuture future,
			long fireGeneration, ConcurrentHashMap<Binary, CacheState> operationGlobal) {
		var cs = operationGlobal.computeIfAbsent(_gKey, this::newCacheState);
		var state = new Object() {
			int stage;
		};
		cs.lock.enter(() -> {
			// release置位StateRemoving后不再挂起，异常逃逸只可能发生在本段内；finally兜底复位（理由同acquire*Async）。
			var ownsRemoving = false;
			try {
				var gKey = cs.globalKey;
				if (state.stage == 1) {
					if (cs.modify != null && !cs.share.isEmpty())
						throw new IllegalStateException("CacheState state error");
				} else if (cs.acquireStatePending == StateRemoved && state.stage == 0) {
					// 这个是不可能的，因为有Release请求进来意味着肯定有拥有者(share or modify)，此时不可能进入StateRemoved。
					cs.lock.leave();
					releaseAsync(sender, gKey, future, fireGeneration, operationGlobal);
					return;
				}

				if (cs.acquireStatePending != StateInvalid && cs.acquireStatePending != StateRemoved) {
					switch (cs.acquireStatePending) {
					case StateShare:
					case StateModify:
						if (isDebugEnabled)
							logger.debug("Release 0 {} {} {}", sender, gKey, cs);
						break;
					case StateRemoving:
						// release 不会导致死锁，等待即可。
						break;
					}
					state.stage = 1;
					cs.lock.leaveAndWaitNotify();
					return;
				}
					if (cs.acquireStatePending == StateRemoved) {
						cs.lock.leave();
						releaseAsync(sender, gKey, future, fireGeneration, operationGlobal);
						return;
					}
					if (sender.generation != fireGeneration) {
						// 世代已更替，跳过旧世代释放以防误杀新连接所有权；仍须完成 future
						future.finishOne();
						return;
					}
					cs.acquireStatePending = StateRemoving;
					ownsRemoving = true;

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
				cs.lock.notifyAllWait();
				future.finishOne();
			} catch (Throwable ex) { // 异常路径也必须 finishOne，避免 Login/NormalClose 应答永久挂起
				logger.error("ReleaseAsync", ex);
				future.finishOne();
			} finally {
				// 异常逃逸时复位本次占住的 StateRemoving 并唤醒等待者
				if (ownsRemoving && cs.acquireStatePending == StateRemoving) {
					cs.acquireStatePending = StateInvalid;
					cs.lock.notifyAllWait();
				}
			}
		});
	}

	private void releaseAsync(Acquire rpc) {
		// 入口一次捕获 sender，避免延续阶段因 kick 清空 userState 触发 NPE
		//noinspection DataFlowIssue
		var sender = (CacheHolder)rpc.getSender().getUserState();
		if (sender == null) { // 与processAcquireRequest的检查之间有kick竞争窗口
			rpc.Result.state = StateInvalid;
			rpc.SendResultCode(AcquireNotLogin);
			if (ENABLE_PERF)
				getPerf().onAcquireEnd(rpc, StateInvalid);
			return;
		}
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
			if (ENABLE_PERF)
				getPerf().onAcquireEnd(rpc, StateInvalid);
			return;
		}
		releaseAsync(rpc, sender, fireGeneration);
	}

	private void releaseAsync(Acquire rpc, CacheHolder sender, long fireGeneration) {
		releaseAsync(rpc, sender, fireGeneration, getGlobal());
	}

	private void releaseAsync(Acquire rpc, CacheHolder sender, long fireGeneration,
			ConcurrentHashMap<Binary, CacheState> operationGlobal) {
		//noinspection DataFlowIssue
		var cs = operationGlobal.computeIfAbsent(rpc.Argument.globalKey, this::newCacheState);
		var state = new Object() {
			int stage;
		};
		cs.lock.enter(() -> {
			// release置位StateRemoving后不再挂起，异常逃逸只可能发生在本段内；finally兜底复位（理由同acquire*Async）。
			var ownsRemoving = false;
			try {
				if (state.stage == 1) {
					if (cs.modify != null && !cs.share.isEmpty())
						throw new IllegalStateException("CacheState state error");
				} else if (cs.acquireStatePending == StateRemoved && state.stage == 0) {
					// 这个是不可能的，因为有Release请求进来意味着肯定有拥有者(share or modify)，此时不可能进入StateRemoved。
					cs.lock.leave();
					releaseAsync(rpc, sender, fireGeneration, operationGlobal);
					return;
				}

				var gKey = cs.globalKey;
				if (cs.acquireStatePending != StateInvalid && cs.acquireStatePending != StateRemoved) {
					switch (cs.acquireStatePending) {
					case StateShare:
					case StateModify:
						if (isDebugEnabled)
							logger.debug("Release 1 {} {} {}", sender, gKey, cs);
						rpc.Result.state = cs.getSenderCacheState(sender);
						rpc.SendResultCode(0);
						if (ENABLE_PERF)
							getPerf().onAcquireEnd(rpc, StateInvalid);
						return;
					case StateRemoving:
						// release 不会导致死锁，等待即可。
						break;
					}
					state.stage = 1;
					cs.lock.leaveAndWaitNotify();
					return;
				}
				if (cs.acquireStatePending == StateRemoved) {
					cs.lock.leave();
					releaseAsync(rpc, sender, fireGeneration, operationGlobal);
					return;
				}
				if (sender.generation != fireGeneration) {
					// 世代已更替，跳过旧世代释放并按当前状态应答
					rpc.Result.state = cs.getSenderCacheState(sender);
					rpc.SendResultCode(0);
					if (ENABLE_PERF)
						getPerf().onAcquireEnd(rpc, StateInvalid);
					return;
				}

				cs.acquireStatePending = StateRemoving;
				ownsRemoving = true;

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
				cs.lock.notifyAllWait();
				rpc.Result.state = StateInvalid;
				rpc.SendResultCode(0);
				if (ENABLE_PERF)
					getPerf().onAcquireEnd(rpc, StateInvalid);
			} catch (Throwable ex) { // AsyncLock.enter 会吞掉异常，异常路径须自行应答
				logger.error("ReleaseAsync", ex);
				rpc.Result.state = StateInvalid;
				rpc.SendResultCode(AcquireException);
				if (ENABLE_PERF)
					getPerf().onAcquireEnd(rpc, StateInvalid);
			} finally {
				// 异常逃逸时复位本次占住的 StateRemoving 并唤醒等待者
				if (ownsRemoving && cs.acquireStatePending == StateRemoving) {
					cs.acquireStatePending = StateInvalid;
					cs.lock.notifyAllWait();
				}
			}
		});
	}

	private void acquireShareAsync(Acquire rpc) {
		acquireShareAsync(rpc, getGlobal());
	}

	private void acquireShareAsync(Acquire rpc, ConcurrentHashMap<Binary, CacheState> operationGlobal) {
		// 入口一次捕获 sender，避免延续阶段因 kick 清空 userState 触发 NPE
		//noinspection DataFlowIssue
		var sender = (CacheHolder)rpc.getSender().getUserState();
		if (sender == null) { // 与processAcquireRequest的检查之间有kick竞争窗口
			rpc.Result.state = StateInvalid;
			rpc.SendResultCode(AcquireNotLogin);
			if (ENABLE_PERF)
				getPerf().onAcquireEnd(rpc, StateShare);
			return;
		}
		//noinspection DataFlowIssue
		var cs = operationGlobal.computeIfAbsent(rpc.Argument.globalKey, this::newCacheState);
		var state = new Object() {
			int stage;
			int reduceResultState;
			@Nullable Id128 reduceTid;
		};
		cs.lock.enter(() -> {
			// stage==2的延续持有此前阶段设置的申请位；本次进入新设置的也会置位
			var ownsPending = state.stage == 2;
			try {
				if (state.stage == 0) {
					if (cs.acquireStatePending == StateRemoved) {
						cs.lock.leave();
						acquireShareAsync(rpc, operationGlobal);
						return;
					}
				}
				if (state.stage <= 1 && cs.modify != null && !cs.share.isEmpty())
					throw new IllegalStateException("CacheState state error");

				if (state.stage <= 1) {
					if (cs.acquireStatePending != StateInvalid && cs.acquireStatePending != StateRemoved) {
						switch (cs.acquireStatePending) {
						case StateShare:
							if (cs.modify == null)
								throw new IllegalStateException("CacheState state error");
							if (cs.modify == sender) {
								if (isDebugEnabled)
									logger.debug("1 {} {} {}", sender, StateShare, cs);
								rpc.Result.state = StateInvalid;
								rpc.SendResultCode(AcquireShareDeadLockFound);
								if (ENABLE_PERF)
									getPerf().onAcquireEnd(rpc, StateShare);
								return;
							}
							break;
						case StateModify:
							if (cs.modify == sender || cs.share.contains(sender)) {
								if (isDebugEnabled)
									logger.debug("2 {} {} {}", sender, StateShare, cs);
								rpc.Result.state = StateInvalid;
								rpc.SendResultCode(AcquireShareDeadLockFound);
								if (ENABLE_PERF)
									getPerf().onAcquireEnd(rpc, StateShare);
								return;
							}
							break;
						case StateRemoving:
							break;
						}
						if (isDebugEnabled)
							logger.debug("3 {} {} {}", sender, StateShare, cs);
						state.stage = 1;
						cs.lock.leaveAndWaitNotify();
						return;
					}
					if (cs.acquireStatePending == StateRemoved) {
						cs.lock.leave();
						acquireShareAsync(rpc, operationGlobal);
						return; // concurrent release
					}

					cs.acquireStatePending = StateShare;
					ownsPending = true;
					serialIdGenerator.getAndIncrement();
				}

				var gKey = cs.globalKey;
				if (cs.modify != null || state.stage == 2) {
					if (state.stage != 2) {
						if (cs.modify == sender) {
							// 已经是Modify又申请，可能是sender异常关闭，
							// 又重启连上。更新一下。应该是不需要的。
							sender.acquired.put(gKey, StateModify);
							cs.acquireStatePending = StateInvalid;
							cs.lock.notifyAllWait(); // 归还申请位必须唤醒等待者
							if (isDebugEnabled)
								logger.debug("4 {} {} {}", sender, StateShare, cs);
							rpc.Result.state = StateModify;
							rpc.SendResultCode(AcquireShareAlreadyIsModify);
							if (ENABLE_PERF)
								getPerf().onAcquireEnd(rpc, StateShare);
							return;
						}

						state.reduceResultState = StateReduceNetError; // 默认网络错误。
						if (cs.modify.reduceWaitLater(gKey, rpc.getResultCode(), r -> {
							if (ENABLE_PERF)
								getPerf().onReduceEnd(r);
							if (r.isTimeout()) {
								logger.warn("acquireShare: reduce timeout. so={}, time={}, arg={}",
										r.getSender(), r.getTimeout(), r.Argument);
								state.reduceResultState = StateReduceRpcTimeout;
							} else if (!GlobalCacheManagerConst.isReduceResultCodeSuccess(r.getResultCode())) {
								// 错误应答的默认StateInvalid不能证明降级；明确的幂等确认码仍允许使用结果。
								state.reduceResultState = StateReduceNetError;
							} else {
								state.reduceResultState = r.Result.state;
								state.reduceTid = r.Result.reducedTid;
							}
							cs.lock.enter(cs.lock::notifyAllWait);
							return 0;
						}) != null) {
							if (isDebugEnabled)
								logger.debug("5 {} {} {}", sender, StateShare, cs);
							state.stage = 2;
							ownsPending = false; // 挂起不是退出：回调稍后重入（stage==2重新持有ownsPending），finally不能复位申请位
							cs.lock.leaveAndWaitNotify();
							return;
						}
					}

					switch (state.reduceResultState) {
					case StateShare:
						assert cs.modify != null;
						cs.modify.acquired.put(gKey, StateShare);
						cs.share.add(cs.modify); // 降级成功。
						break;

					case StateInvalid:
						// 降到了 Invalid，此时就不需要加入 Share 了。
						assert cs.modify != null;
						cs.modify.acquired.remove(gKey);
						break;

					case StateReduceErrorFreshAcquire:
						cs.acquireStatePending = StateInvalid;
						cs.lock.notifyAllWait();
						if (ENABLE_PERF)
							getPerf().onOthers("XXX Fresh " + StateShare);
						rpc.Result.state = StateInvalid;
						rpc.SendResultCode(StateReduceErrorFreshAcquire);
						if (ENABLE_PERF)
							getPerf().onAcquireEnd(rpc, StateShare);
						return;

					default:
						// 包含协议返回错误的值的情况。
						// case StateReduceRpcTimeout: // 11
						// case StateReduceException: // 12
						// case StateReduceNetError: // 13
						cs.acquireStatePending = StateInvalid;
						cs.lock.notifyAllWait();
						if (ENABLE_PERF)
							getPerf().onOthers("XXX 8 " + StateShare + " " + state.reduceResultState);
						rpc.Result.state = StateInvalid;
						rpc.SendResultCode(AcquireShareFailed);
						if (ENABLE_PERF)
							getPerf().onAcquireEnd(rpc, StateShare);
						return;
					}

					sender.acquired.put(gKey, StateShare);
					cs.modify = null;
					cs.share.add(sender);
					cs.acquireStatePending = StateInvalid;
					if (isDebugEnabled)
						logger.debug("6 {} {} {}", sender, StateShare, cs);
					cs.lock.notifyAllWait();
					rpc.Result.reducedTid = state.reduceTid;
					rpc.SendResultCode(0);
					if (ENABLE_PERF)
						getPerf().onAcquireEnd(rpc, StateShare);
					return;
				}

				sender.acquired.put(gKey, StateShare);
				cs.share.add(sender);
				cs.acquireStatePending = StateInvalid;
				if (isDebugEnabled)
					logger.debug("7 {} {} {}", sender, StateShare, cs);
				cs.lock.notifyAllWait();
				rpc.Result.reducedTid = state.reduceTid;
				rpc.SendResultCode(0);
				if (ENABLE_PERF)
					getPerf().onAcquireEnd(rpc, StateShare);
			} catch (Throwable ex) { // AsyncLock.enter会捕获吞掉异常；异常路径也必须应答
				logger.error("AcquireShareAsync", ex);
				rpc.Result.state = StateInvalid;
				rpc.SendResultCode(AcquireException);
				if (ENABLE_PERF)
					getPerf().onAcquireEnd(rpc, StateShare);
			} finally {
				// 异常逃逸时复位本次占住的申请位并唤醒等待者；正常路径已自行复位（StateInvalid）
				if (ownsPending && (cs.acquireStatePending == StateShare || cs.acquireStatePending == StateModify)) {
					cs.acquireStatePending = StateInvalid;
					cs.lock.notifyAllWait();
				}
			}
		});
	}

	private void acquireModifyAsync(Acquire rpc) {
		acquireModifyAsync(rpc, getGlobal());
	}

	private void acquireModifyAsync(Acquire rpc, ConcurrentHashMap<Binary, CacheState> operationGlobal) {
		// 入口一次捕获 sender，避免延续阶段因 kick 清空 userState 触发 NPE
		//noinspection DataFlowIssue
		var sender = (CacheHolder)rpc.getSender().getUserState();
		if (sender == null) { // 与processAcquireRequest的检查之间有kick竞争窗口
			rpc.Result.state = StateInvalid;
			rpc.SendResultCode(AcquireNotLogin);
			if (ENABLE_PERF)
				getPerf().onAcquireEnd(rpc, StateModify);
			return;
		}
		//noinspection DataFlowIssue
		var cs = operationGlobal.computeIfAbsent(rpc.Argument.globalKey, this::newCacheState);
		var state = new Object() {
			int stage;
			int reduceResultState;
			@Nullable Id128 reduceTid;
		};
		cs.lock.enter(() -> {
			// stage==2的延续持有此前阶段设置的申请位；本次进入新设置的也会置位
			var ownsPending = state.stage == 2;
			try {
				if (state.stage == 0) {
					if (cs.acquireStatePending == StateRemoved) {
						cs.lock.leave();
						acquireModifyAsync(rpc, operationGlobal);
						return;
					}
				}
				if (state.stage <= 1) {
					if (cs.modify != null && !cs.share.isEmpty())
						throw new IllegalStateException("CacheState state error");
				}

				if (state.stage <= 1) {
					if (cs.acquireStatePending != StateInvalid && cs.acquireStatePending != StateRemoved) {
						switch (cs.acquireStatePending) {
						case StateShare:
							if (cs.modify == null)
								throw new IllegalStateException("CacheState state error");

							if (cs.modify == sender) {
								if (isDebugEnabled)
									logger.debug("1 {} {} {}", sender, StateModify, cs);
								rpc.Result.state = StateInvalid;
								rpc.SendResultCode(AcquireModifyDeadLockFound);
								if (ENABLE_PERF)
									getPerf().onAcquireEnd(rpc, StateModify);
								return;
							}
							break;
						case StateModify:
							if (cs.modify == sender || cs.share.contains(sender)) {
								if (isDebugEnabled)
									logger.debug("2 {} {} {}", sender, StateModify, cs);
								rpc.Result.state = StateInvalid;
								rpc.SendResultCode(AcquireModifyDeadLockFound);
								if (ENABLE_PERF)
									getPerf().onAcquireEnd(rpc, StateModify);
								return;
							}
							break;
						case StateRemoving:
							break;
						}
						if (isDebugEnabled)
							logger.debug("3 {} {} {}", sender, StateModify, cs);
						state.stage = 1;
						cs.lock.leaveAndWaitNotify();
						return;
					}
					if (cs.acquireStatePending == StateRemoved) {
						cs.lock.leave();
						acquireModifyAsync(rpc, operationGlobal);
						return; // concurrent release
					}

					cs.acquireStatePending = StateModify;
					ownsPending = true;
					serialIdGenerator.getAndIncrement();
				}

				var gKey = cs.globalKey;
				if (cs.modify != null || state.stage == 2) {
					if (state.stage != 2) {
						if (cs.modify == sender) {
							if (isDebugEnabled)
								logger.debug("4 {} {} {}", sender, StateModify, cs);
							// 已经是Modify又申请，可能是sender异常关闭，又重启连上。
							// 更新一下。应该是不需要的。
							sender.acquired.put(gKey, StateModify);
							cs.acquireStatePending = StateInvalid;
							cs.lock.notifyAllWait();
							rpc.SendResultCode(AcquireModifyAlreadyIsModify);
							if (ENABLE_PERF)
								getPerf().onAcquireEnd(rpc, StateModify);
							return;
						}

						state.reduceResultState = StateReduceNetError; // 默认网络错误。
						if (cs.modify.reduceWaitLater(gKey, rpc.getResultCode(), r -> {
							if (ENABLE_PERF)
								getPerf().onReduceEnd(r);
							if (r.isTimeout()) {
								logger.warn("acquireModify: reduce timeout. so={}, time={}, arg={}",
										r.getSender(), r.getTimeout(), r.Argument);
								state.reduceResultState = StateReduceRpcTimeout;
							} else if (!GlobalCacheManagerConst.isReduceResultCodeSuccess(r.getResultCode())) {
								// 错误应答的默认StateInvalid不能证明降级；明确的幂等确认码仍允许使用结果。
								state.reduceResultState = StateReduceNetError;
							} else {
								state.reduceResultState = r.Result.state;
								state.reduceTid = r.Result.reducedTid;
							}
							cs.lock.enter(cs.lock::notifyAllWait);
							return 0;
						}) != null) {
							if (isDebugEnabled)
								logger.debug("5 {} {} {}", sender, StateModify, cs);
							state.stage = 2;
							ownsPending = false; // 挂起不是退出：回调稍后重入（stage==2重新持有ownsPending），finally不能复位申请位
							cs.lock.leaveAndWaitNotify();
							return;
						}
					}

					switch (state.reduceResultState) {
					case StateInvalid:
						assert cs.modify != null;
						cs.modify.acquired.remove(gKey);
						break; // reduce success

					case StateReduceErrorFreshAcquire:
						cs.acquireStatePending = StateInvalid;
						cs.lock.notifyAllWait();
						if (ENABLE_PERF)
							getPerf().onOthers("XXX Fresh " + StateModify);
						rpc.Result.state = StateInvalid;
						rpc.SendResultCode(StateReduceErrorFreshAcquire);
						if (ENABLE_PERF)
							getPerf().onAcquireEnd(rpc, StateModify);
						return;

					default:
						// case StateReduceRpcTimeout: // 11
						// case StateReduceException: // 12
						// case StateReduceNetError: // 13
						cs.acquireStatePending = StateInvalid;
						cs.lock.notifyAllWait();
						if (ENABLE_PERF)
							getPerf().onOthers("XXX 9 " + StateModify + " " + state.reduceResultState);
						rpc.Result.state = StateInvalid;
						rpc.SendResultCode(AcquireModifyFailed);
						if (ENABLE_PERF)
							getPerf().onAcquireEnd(rpc, StateModify);
						return;
					}

					sender.acquired.put(gKey, StateModify);
					cs.modify = sender;
					cs.share.remove(sender);
					cs.acquireStatePending = StateInvalid;
					if (isDebugEnabled)
						logger.debug("6 {} {} {}", sender, StateModify, cs);
					cs.lock.notifyAllWait();
					rpc.Result.reducedTid = state.reduceTid;
					rpc.SendResultCode(0);
					if (ENABLE_PERF)
						getPerf().onAcquireEnd(rpc, StateModify);
					return;
				}

				var reducePending = new ArrayList<KVNotNull<CacheHolder, Reduce>>();
				var reduceSucceed = new IdentityHashSet<CacheHolder>();
				var allReduceFuture = new CountDownFuture();
				var senderIsShareTmp = false;
				// 先把降级请求全部发送给出去。
				for (var it = cs.share.iterator(); it.moveToNext(); ) {
					CacheHolder c = it.value();
					if (c == sender) {
						// 申请者不需要降级，直接加入成功。
						senderIsShareTmp = true;
						reduceSucceed.add(sender);
						continue;
					}
					allReduceFuture.createOne();
					Reduce reduce = c.reduceWaitLater(gKey, rpc.getResultCode(), r -> {
						if (ENABLE_PERF)
							getPerf().onReduceEnd(r);
						allReduceFuture.finishOne();
						return 0;
					});
					if (reduce == null) {
						// 网络错误不再认为成功。整个降级失败，要中断降级。
						// 已经发出去的降级请求要等待并处理结果。后面处理。
						allReduceFuture.finishOne();
						break;
					}
					reducePending.add(KVNotNull.create(c, reduce));
				}
				boolean senderIsShare = senderIsShareTmp;

				var errorFreshAcquire = new OutObject<>(Boolean.FALSE);
				Action0 lastStage = () -> {
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
						if (isDebugEnabled)
							logger.debug("8 {} {} {}", sender, StateModify, cs);
						cs.lock.notifyAllWait();
						rpc.Result.reducedTid = state.reduceTid;
						rpc.SendResultCode(0);
					} else {
						// senderIsShare 在失败的时候，Acquired 没有变化，不需要更新。
						// 失败了，要把原来是share的sender恢复。
						if (senderIsShare)
							cs.share.add(sender);
						cs.acquireStatePending = StateInvalid;
						cs.lock.notifyAllWait();
						if (ENABLE_PERF)
							getPerf().onOthers("XXX 10 " + StateModify + ' ' + errorFreshAcquire.value);
						rpc.Result.state = StateInvalid;
						//noinspection DataFlowIssue
						if (errorFreshAcquire.value)
							rpc.SendResultCode(StateReduceErrorFreshAcquire); // 这个错误不看做失败，允许发送方继续尝试。
						else
							rpc.SendResultCode(AcquireModifyFailed);
					}
					if (ENABLE_PERF)
						getPerf().onAcquireEnd(rpc, StateModify);
				};

			// 两种情况不需要发reduce
			// 1. share是空的, 可以直接升为Modify
			// 2. sender是share, 而且reducePending的size是0
			if (!cs.share.isEmpty() && (!senderIsShare || !reducePending.isEmpty())) {
				if (isDebugEnabled)
					logger.debug("7 {} {} {}", sender, StateModify, cs);
				ownsPending = false; // 注册then后回调即退出（等reduce期间申请位仍需持有），后续由cs.lock.enter(lastStage)重入，finally不能复位
				allReduceFuture.then(__ -> {
						// 一个个等待是否成功。WaitAll 碰到错误不知道怎么处理的，
						// 应该也会等待所有任务结束（包括错误）。
						var freshAcquire = false;
						for (var e : reducePending) {
							var cacheHolder = e.getKey();
							var reduce = e.getValue();
							if (reduce.isTimeout()) { // 等待失败不再看作成功。
								cacheHolder.setError();
								logger.warn("reduce timeout {} AcquireState={} CacheState={} arg={}",
										rpc.getSender(), StateModify, cs, reduce.Argument);
							} else if (!GlobalCacheManagerConst.isReduceResultCodeSuccess(reduce.getResultCode())) {
								// 错误应答不能证明降级成功，保留该持有者。
								cacheHolder.setError();
							} else {
								switch (reduce.Result.state) {
								case StateInvalid:
									reduceSucceed.add(cacheHolder);
									break;
								case StateReduceErrorFreshAcquire:
									// 这个错误不进入Forbid状态。
									freshAcquire = true;
									break;
								default:
									cacheHolder.setError();
									logger.error("Reduce result state={}", reduce.Result.state);
									break;
								}
							}
						}
						errorFreshAcquire.value = freshAcquire;
						cs.lock.enter(lastStage);
					});
				} else
					lastStage.run();
			} catch (Throwable ex) { // AsyncLock.enter会捕获吞掉异常；异常路径也必须应答
				logger.error("AcquireModifyAsync", ex);
				rpc.Result.state = StateInvalid;
				rpc.SendResultCode(AcquireException);
				if (ENABLE_PERF)
					getPerf().onAcquireEnd(rpc, StateModify);
			} finally {
				// 异常逃逸时复位本次占住的申请位并唤醒等待者；正常路径已自行复位（StateInvalid）
				if (ownsPending && (cs.acquireStatePending == StateShare || cs.acquireStatePending == StateModify)) {
					cs.acquireStatePending = StateInvalid;
					cs.lock.notifyAllWait();
				}
			}
		});
	}

	private CacheState newCacheState(Binary gKey) {
		//noinspection DataFlowIssue
		return new CacheState(gKey, useSyncLock ? new InlineAsyncLock()
				: new AsyncLock(lockExecutor));
	}

	private static final class CacheState {
		final Binary globalKey; // 这里的引用同global map的key,用于给CacheHolder里的map相同的key引用
		final IdentityHashSet<CacheHolder> share = new IdentityHashSet<>();
		final AbstractAsyncLock lock;
		@Nullable CacheHolder modify;
		int acquireStatePending = StateInvalid;

		public CacheState(Binary gKey, AbstractAsyncLock lock) {
			globalKey = gKey;
			this.lock = lock;
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
			return String.format("(P%d M%s S%s)", acquireStatePending, modify, sb);
		}
	}

	private static final class CacheHolder extends ReentrantLock {
		private static final AtomicLongFieldUpdater<CacheHolder> generationUpdater =
				AtomicLongFieldUpdater.newUpdater(CacheHolder.class, "generation");
		private final GlobalCacheManagerAsyncServer owner; // 归属服务器实例，支持同 JVM 多实例隔离
		final ConcurrentHashMap<Binary, Integer> acquired = new ConcurrentHashMap<>();

		CacheHolder(GlobalCacheManagerAsyncServer owner) {
			this.owner = owner;
		}
		long sessionId;
		int globalCacheManagerHashIndex;
		private volatile long activeTime = System.currentTimeMillis();
		private volatile long lastErrorTime;
		private boolean isLoggedIn = false;
		private volatile boolean debugMode;
		// 世代号：kick 与重绑均在 session 锁内递增；releaseAsync 发射时捕获、移除前复查，不匹配则跳过以防误杀新连接所有权
		private volatile long generation;

		long getActiveTime() {
			return activeTime;
		}

		void setActiveTime(long value) {
			activeTime = value;
		}

		void setDebugMode(boolean debugMode) {
			this.debugMode = debugMode;
		}

		// 与绑定共用session锁，代次递增不会丢失。
		void kick() {
			lock();
			try {
				generationUpdater.incrementAndGet(this); // 推进世代，使在飞的旧 release 失效
				var peer = owner.getServer().GetSocket(sessionId);
				if (null != peer) {
					peer.setUserState(null); // 来自这个Agent的所有请求都会失败。
					peer.close(kickException); // 关闭连接，强制Agent重新登录。
				}
				sessionId = 0; // 清除网络状态。
			} finally {
				unlock();
			}
		}

		boolean tryBindSocket(AsyncSocket newSocket, int globalCacheManagerHashIndex, boolean login) {
			lock();
			try {
				// ReLogin 要求同一 serverId 已完成过 Login 绑定，用来检测 GCM 重启。
				if (!login && !isLoggedIn)
					return false;
				if (newSocket.getUserState() != null) {
					logger.warn("TryBindSocket: already bound! newSocket.getUserState() != null, SessionId={}",
							newSocket.getSessionId());
					return false; // 不允许再次绑定。Login Or ReLogin 只能发一次。
				}

				if (!owner.open)
					return false;
				var socket = owner.getServer().GetSocket(sessionId);
				if (socket == null) {
					// 旧连接已不存在或已断开
					generationUpdater.incrementAndGet(this); // 重绑推进新世代，使之前在飞的 release 失效
					sessionId = newSocket.getSessionId();
					newSocket.setUserState(this);
					if (login)
						isLoggedIn = true;
					this.globalCacheManagerHashIndex = globalCacheManagerHashIndex;
					// 锁内刷新活跃时间，避免守护线程按旧超时踢掉刚登录的新连接
					activeTime = System.currentTimeMillis();
					return true;
				}
				// 每个ServerId只允许一个实例，已经存在了以后，旧的实例上有状态，阻止新的实例登录成功。
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

				var current = owner.getServer().GetSocket(sessionId);
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

		void setError() {
			long now = System.currentTimeMillis();
			if (now - lastErrorTime > owner.getAchillesHeelConfig().globalForbidPeriod)
				lastErrorTime = now;
		}

		/**
		 * 返回null表示发生了网络错误，或者应用服务器已经关闭。
		 */
		@Nullable Reduce reduceWaitLater(Binary gkey, long fresh,
										 ProtocolHandle<Rpc<BGlobalKeyState, BGlobalKeyState>> handle) {
			try {
				if (System.currentTimeMillis() - lastErrorTime < owner.getAchillesHeelConfig().globalForbidPeriod)
					return null;
				AsyncSocket peer = owner.getServer().GetSocket(sessionId);
				if (peer != null) {
					var reduce = new Reduce(gkey, StateInvalid);
					reduce.setResultCode(fresh);
					if (ENABLE_PERF)
						owner.getPerf().onReduceBegin(reduce);
					if (reduce.Send(peer, handle, owner.getAchillesHeelConfig().reduceTimeout))
						return reduce;
					if (ENABLE_PERF)
						owner.getPerf().onReduceCancel(reduce);
				}
				logger.warn("Send Reduce failed. SessionId={}, peer={}, gkey={}", sessionId, peer, gkey);
			} catch (Throwable ex) { // 这里的异常只应该是网络发送异常。
				logger.error("ReduceWaitLater Exception {}", gkey, ex);
			}
			setError();
			return null;
		}
	}

	private static final class ServerService extends Service {
		ServerService(@Nullable Config config) {
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
		public void dispatchProtocol(long typeId, ByteBuffer bb,
									 ProtocolFactoryHandle<?> factoryHandle,
									 @Nullable AsyncSocket so) throws Exception {
			var p = decodeProtocol(typeId, bb, factoryHandle, so);
			p.handle(this, factoryHandle); // 所有协议处理几乎无阻塞,可放心直接跑在IO线程上
		}

		@Override
		public <P extends Protocol<?>> void dispatchRpcResponse(P rpc,
																ProtocolHandle<P> responseHandle,
																ProtocolFactoryHandle<?> factoryHandle) {
			// 在新的decode-dispatch流程中，上面的dispatchProtocol直接执行操作，实际上包含了rpc.handle，
			// 这个函数不会被触发，保留在这里。
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
		int threadCount = 0;
		String raftName = null;
		String raftConf = "global.raft.xml";

		for (int i = 0; i < args.length; ++i) {
			switch (args[i]) {
			case "-ip":
				ip = requireValue(args, ++i, "-ip");
				break;
			case "-port":
				port = requireInt(args, ++i, "-port");
				break;
			case "-threads":
				threadCount = requireInt(args, ++i, "-threads");
				break;
			case "-raft":
				raftName = requireValue(args, ++i, "-raft");
				break;
			case "-raftConf":
				raftConf = requireValue(args, ++i, "-raftConf");
				break;
			case "-tryNextSync":
				useSyncLock = true;
				break;
			default:
				throw new IllegalArgumentException("unknown argument: " + args[i]);
			}
		}

		int cpuCount = Runtime.getRuntime().availableProcessors();
		if (threadCount < 1)
			threadCount = cpuCount;
		Task.initThreadPool(Task.newFixedThreadPool(threadCount, "ZezeTaskPool"),
				Executors.newScheduledThreadPool(1,
						new ThreadFactoryWithName("ZezeScheduledPool", Thread.NORM_PRIORITY + 2)));
		if (Selectors.getInstance().getCount() < cpuCount)
			Selectors.getInstance().add(cpuCount - Selectors.getInstance().getCount());

		if (raftName == null || raftName.isEmpty()) {
			logger.info("Start {}:{}", ip != null ? ip : "any", port);
			InetAddress address = (ip != null && !ip.isBlank()) ? InetAddress.getByName(ip) : null;
			new GlobalCacheManagerAsyncServer().start(address, port);
			synchronized (Thread.currentThread()) {
				Thread.currentThread().wait();
			}
		} else if (raftName.equals("RunAllNodes")) {
			logger.info("Start Raft=RunAllNodes");
			//noinspection unused
			try (var GlobalRaft1 = new GlobalCacheManagerWithRaft("127.0.0.1:5556", RaftConfig.load(raftConf));
				 var GlobalRaft2 = new GlobalCacheManagerWithRaft("127.0.0.1:5557", RaftConfig.load(raftConf));
				 var GlobalRaft3 = new GlobalCacheManagerWithRaft("127.0.0.1:5558", RaftConfig.load(raftConf))) {
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
