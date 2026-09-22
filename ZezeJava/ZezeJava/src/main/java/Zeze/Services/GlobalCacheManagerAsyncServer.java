package Zeze.Services;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
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
import Zeze.Util.Action0;
import Zeze.Util.Action1;
import Zeze.Util.AsyncLock;
import Zeze.Util.Id128;
import Zeze.Util.IdentityHashSet;
import Zeze.Util.KV;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class GlobalCacheManagerAsyncServer extends ReentrantLock implements GlobalCacheManagerConst {
	static {
		var level = Level.toLevel(System.getProperty("logLevel"), Level.INFO);
		((LoggerContext)LogManager.getContext(false)).getConfiguration().getRootLogger().setLevel(level);
	}

	private static final boolean ENABLE_PERF = true;
	private static final @NotNull Logger logger = LogManager.getLogger(GlobalCacheManagerAsyncServer.class);
	private static final boolean isDebugEnabled = logger.isDebugEnabled();
	// -tryNextSync 启动参数置位，main里start前设置、之后只读；CacheState构造锁时读取。
	// 不再用系统属性(其生效依赖AsyncLock类惰性加载时机，设置晚了静默失效)。
	private static volatile boolean useSyncLock;

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
	private final GlobalCacheManagerServer.GCMConfig gcmConfig = new GlobalCacheManagerServer.GCMConfig();
	private AchillesHeelConfig achillesHeelConfig;
	private Future<?> achillesHeelTimer;
	// FND10 svc-01 对齐同步版FND7-18三件套：tick只派发不内联执行（running标志），stop关门+等待在飞扫描。
	private final AtomicBoolean achillesHeelRunning = new AtomicBoolean();
	private volatile boolean achillesHeelShutdown;
	private GlobalCacheManagerPerf perf;

	// 每个实例都是独立的服务器（无共享静态状态），可同 JVM 启动多个监听不同端口。
	public GlobalCacheManagerAsyncServer() {
	}

	// 外面主动提供装载配置，需要在Load之前把这个实例注册进去。
	public @NotNull GlobalCacheManagerServer.GCMConfig getGcmConfig() {
		return gcmConfig;
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
			ZezeCounter.tryInit();

			if (config == null)
				config = Config.load();
			config.parseCustomize(gcmConfig);

			sessions = new LongConcurrentHashMap<>(4096);
			global = new ConcurrentHashMap<>(gcmConfig.initialCapacity);

			server = new ServerService(config);

			server.AddFactoryHandle(Acquire.TypeId_, new Service.ProtocolFactoryHandle<>(
					Acquire::new, this::processAcquireRequest, TransactionLevel.None, DispatchMode.Direct));
			server.AddFactoryHandle(Reduce.TypeId_, new Service.ProtocolFactoryHandle<>(
					Reduce::new, null, TransactionLevel.None, DispatchMode.Direct));
			server.AddFactoryHandle(Login.TypeId_, new Service.ProtocolFactoryHandle<>(
					Login::new, this::processLogin, TransactionLevel.None, DispatchMode.Direct));
			server.AddFactoryHandle(ReLogin.TypeId_, new Service.ProtocolFactoryHandle<>(
					ReLogin::new, this::processReLogin, TransactionLevel.None, DispatchMode.Direct));
			server.AddFactoryHandle(NormalClose.TypeId_, new Service.ProtocolFactoryHandle<>(
					NormalClose::new, this::processNormalClose, TransactionLevel.None, DispatchMode.Direct));
			// 临时注册到这里，安全起见应该起一个新的Service，并且仅绑定到 localhost。
			server.AddFactoryHandle(Cleanup.TypeId_, new Service.ProtocolFactoryHandle<>(
					Cleanup::new, this::processCleanup, TransactionLevel.None, DispatchMode.Direct));
			server.AddFactoryHandle(KeepAlive.TypeId_, new Service.ProtocolFactoryHandle<>(
					KeepAlive::new, GlobalCacheManagerAsyncServer::processKeepAliveRequest,
					TransactionLevel.None, DispatchMode.Direct));

			serverSocket = server.newServerSocket(ipaddress, port,
					new Acceptor(port, ipaddress != null ? ipaddress.getHostAddress() : null));

			// Global的守护不需要独立线程。当出现异常问题不能工作时，没有释放锁是不会造成致命问题的。
			achillesHeelConfig = new AchillesHeelConfig(gcmConfig.maxNetPing, gcmConfig.serverProcessTime,
					gcmConfig.serverReleaseTimeout);
			achillesHeelTimer = TaskSpec.ofAction(this::scheduleAchillesHeelDaemon).schedulePeriodNow(5000, 5000);
			achillesHeelShutdown = false; // FND7-18（自同步版移植）：stop后restart支持
		} finally {
			unlock();
		}
	}

	/*
	 * FND7-18（自同步版移植）：派发决策持实例锁与stop()第一段互斥——cancel(false)挡不住已启动的tick，
	 * tick在锁内复查关门标志，保证stop置位后不再产生新扫描；锁内仅CAS+入队，不执行扫描。
	 */
	private void scheduleAchillesHeelDaemon() {
		lock();
		try {
			if (achillesHeelShutdown)
				return; // stop()已关门：不再派发新扫描
			if (!achillesHeelRunning.compareAndSet(false, true))
				return; // 上一轮扫描仍在执行，跳过本轮
			var submitted = false;
			try {
				TaskSpec.ofAction(() -> {
					try {
						achillesHeelDaemon();
					} finally {
						achillesHeelRunning.set(false); // 扫描结束才放行下一轮
					}
				}).name("GlobalCacheManagerAsync.AchillesHeelDaemon").runNow();
				submitted = true;
			} finally {
				if (!submitted)
					achillesHeelRunning.set(false); // 派发失败（池未初始化等）：复位标志，避免守护永久停摆
			}
		} finally {
			unlock();
		}
	}

	/*
	 * FND7-18（自同步版移植）：等待在飞守护扫描结束。等待预算覆盖最坏一轮；等不满仅告警继续
	 * 停机（kick的判空已保证此时无NPE，最多一轮检查提前中止）。
	 */
	private void awaitAchillesHeelIdle() {
		var deadline = System.currentTimeMillis() + Task.defaultTimeout + 5_000;
		while (achillesHeelRunning.get()) {
			if (System.currentTimeMillis() >= deadline) {
				logger.warn("AchillesHeelDaemon still running, skip waiting before stop");
				return;
			}
			try {
				//noinspection BusyWait
				Thread.sleep(10);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}
	}

	private void achillesHeelDaemon() {
		var now = System.currentTimeMillis();

		sessions.forEach(session -> {
			session.lock();
			try {
				// 超时检查必须在锁内复查（理由同同步版FND-S1-6）：检查在锁外时，同serverId新
				// incarnation恰在"检查→加锁"窗口内Login（bind持锁刷新activeTime），daemon会
				// kick新连接并回收其新获取的权限。
				if (now - session.getActiveTime() > achillesHeelConfig.globalDaemonTimeout && !session.debugMode) {
					session.kick();
					if (!session.acquired.isEmpty()) {
						var releaseCount = 0L;
						var allReleaseFuture = new CountDownFuture();
						for (var k : session.acquired.keySet()) {
							// ConcurrentDictionary 可以在循环中删除。这样虽然效率低些，但是能处理更多情况。
							releaseAsync(session, k, allReleaseFuture.createOne());
							++releaseCount;
						}
						session.setActiveTime(System.currentTimeMillis());
						if (releaseCount > 0)
							logger.info("AchillesHeelDaemon.Release session={} count={}", session, releaseCount);
						// skip allReleaseFuture result
					}
				}
			} finally {
				session.unlock();
			}
		});
	}

	public void stop() throws Exception {
		Future<?> timer;
		lock();
		try {
			if (server == null)
				return;
			// 先停使用者再拆被使用者（对齐Raft版顺序，FND4-53）：daemon经CacheHolder.kick访问
			// owner.server.GetSocket，原顺序先置server=null后cancel定时器——停机窗口内daemon
			// 踩到null NPE（持session锁的该轮forEach中止，剩余session不再检查）。
			achillesHeelShutdown = true; // FND7-18（自同步版移植）：锁内置位后cancel，tick锁内复查保证不再产生新扫描
			timer = achillesHeelTimer;
			achillesHeelTimer = null;
		} finally {
			unlock();
		}
		// cancel必须在实例锁外调用（同步版复审R2同因）：tick任务体在TimerFuture锁内执行并会取
		// 实例锁，持实例锁cancel与在飞tick构成ABBA死锁。锁外cancel不破坏关门语义（标志已锁内置位）。
		if (timer != null)
			timer.cancel(false);
		// FND7-18：cancel(false)只阻止后续触发，不join正在执行的扫描——不等待就在飞扫描的
		// kick踩到已置null的server。不持锁等待：扫描体不拿实例锁。
		awaitAchillesHeelIdle();
		lock();
		try {
			if (server == null)
				return; // 并发stop已完成拆除
			serverSocket.close();
			serverSocket = null;
			server.stop();
			server = null;
			if (perf != null) // 不置null：极端情况下并发的协议派发还会引用perf对象
				perf.close();
		} finally {
			unlock();
		}
	}

	/**
	 * 报告错误的时候带上相关信息（包括GlobalCacheManager和LogicServer等等）
	 * 手动Cleanup时，连接正确的服务器执行。
	 */
	private long processCleanup(@NotNull Cleanup rpc) {
		logger.info("ProcessCleanup: {} RequestId={} {}", rpc.getSender(), rpc.getSessionId(), rpc.Argument);
		if (achillesHeelConfig != null) { // disable cleanup.
			logger.warn("ProcessCleanup: {} RequestId={} result={}",
					rpc.getSender(), rpc.getSessionId(), CleanupErrorDisabled);
			rpc.SendResultCode(CleanupErrorDisabled);
			return 0;
		}

		// 安全性以后加强。
		if (!rpc.Argument.secureKey.equals("Ok! verify secure.")) {
			logger.warn("ProcessCleanup: {} RequestId={} result={}",
					rpc.getSender(), rpc.getSessionId(), CleanupErrorSecureKey);
			rpc.SendResultCode(CleanupErrorSecureKey);
			return 0;
		}

		var session = sessions.computeIfAbsent(rpc.Argument.serverId, __ -> new CacheHolder(this));
		if (session.globalCacheManagerHashIndex != rpc.Argument.globalCacheManagerHashIndex) {
			// 多点验证
			logger.warn("ProcessCleanup: {} RequestId={} result={}",
					rpc.getSender(), rpc.getSessionId(), CleanupErrorGlobalCacheManagerHashIndex);
			rpc.SendResultCode(CleanupErrorGlobalCacheManagerHashIndex);
			return 0;
		}

		if (server.GetSocket(session.sessionId) != null) {
			// 连接存在，禁止cleanup。
			logger.warn("ProcessCleanup: {} RequestId={} result={}",
					rpc.getSender(), rpc.getSessionId(), CleanupErrorHasConnection);
			rpc.SendResultCode(CleanupErrorHasConnection);
			return 0;
		}

		// 还有更多的防止出错的手段吗？

		// XXX verify danger
		TaskSpec.ofAction(() -> { // delay 5 mins
			var allReleaseFuture = new CountDownFuture();
			for (var k : session.acquired.keySet()) {
				// ConcurrentDictionary 可以在循环中删除。这样虽然效率低些，但是能处理更多情况。
				releaseAsync(session, k, allReleaseFuture.createOne());
			}
			allReleaseFuture.then(__ -> rpc.SendResultCode(0));
		}).schedule(5 * 60 * 1000);

		return 0;
	}

	private long processLogin(@NotNull Login rpc) {
		logger.info("ProcessLogin: {} RequestId={} {}", rpc.getSender(), rpc.getSessionId(), rpc.Argument);
		var session = sessions.computeIfAbsent(rpc.Argument.serverId, __ -> new CacheHolder(this));
		if (!session.tryBindSocket(rpc.getSender(), rpc.Argument.globalCacheManagerHashIndex, true)) {
			rpc.SendResultCode(LoginBindSocketFail);
			return 0;
		}
		session.setActiveTime(System.currentTimeMillis());
		session.setDebugMode(rpc.Argument.debugMode);
		// new login, 比如逻辑服务器重启。release old acquired.
		// 先快照再逐个释放（理由同processNormalClose）：只回收绑定时刻已存在的旧权限，
		// 防止迭代期间新到达的Acquire被本循环错误回收。
		var releaseKeys = new ArrayList<>(session.acquired.keySet());
		var allReleaseFuture = new CountDownFuture();
		for (var k : releaseKeys) {
			// ConcurrentDictionary 可以在循环中删除。这样虽然效率低些，但是能处理更多情况。
			releaseAsync(session, k, allReleaseFuture.createOne());
		}
		rpc.Result.maxNetPing = gcmConfig.maxNetPing;
		rpc.Result.serverProcessTime = gcmConfig.serverProcessTime;
		rpc.Result.serverReleaseTimeout = gcmConfig.serverReleaseTimeout;
		allReleaseFuture.then(__ -> rpc.SendResultCode(0));
		return 0;
	}

	private long processReLogin(@NotNull ReLogin rpc) {
		logger.info("ProcessReLogin: {} RequestId={} {}", rpc.getSender(), rpc.getSessionId(), rpc.Argument);
		var session = sessions.computeIfAbsent(rpc.Argument.serverId, __ -> new CacheHolder(this));
		if (!session.tryBindSocket(rpc.getSender(), rpc.Argument.globalCacheManagerHashIndex, false)) {
			rpc.SendResultCode(ReLoginBindSocketFail);
			return 0;
		}
		session.setActiveTime(System.currentTimeMillis());
		session.setDebugMode(rpc.Argument.debugMode);
		rpc.SendResultCode(0);
		return 0;
	}

	private long processNormalClose(@NotNull NormalClose rpc) {
		logger.info("ProcessNormalClose: {} RequestId={}", rpc.getSender(), rpc.getSessionId());
		var session = (CacheHolder)rpc.getSender().getUserState();
		if (session == null) {
			logger.warn("ProcessNormalClose: {} RequestId={} result={}",
					rpc.getSender(), rpc.getSessionId(), AcquireNotLogin);
			rpc.SendResultCode(AcquireNotLogin);
			return 0; // not login
		}
		/*
		 * 释放集合必须在解绑之前快照（理由见同步版processNormalClose）：解绑后同serverId新进程可
		 * Login并Acquire新权限写入同一张acquired，本释放循环不能回收它们；旧连接未解绑时新进程
		 * 无法绑定，故快照内不可能出现新incarnation的权限。异步版迭代本身快，但releaseAsync是
		 * 异步完成的，弱一致迭代同样可能看到迭代期间新加入的key。
		 */
		var releaseKeys = new ArrayList<>(session.acquired.keySet());
		if (!session.tryUnBindSocket(rpc.getSender())) {
			logger.warn("ProcessNormalClose: {} RequestId={} result={}",
					rpc.getSender(), rpc.getSessionId(), NormalCloseUnbindFail);
			rpc.SendResultCode(NormalCloseUnbindFail);
			return 0;
		}
		var allReleaseFuture = new CountDownFuture();
		for (var k : releaseKeys) {
			// ConcurrentDictionary 可以在循环中删除。这样虽然效率低些，但是能处理更多情况。
			releaseAsync(session, k, allReleaseFuture.createOne());
		}
		allReleaseFuture.then(__ -> {
			rpc.SendResultCode(0);
			logger.info("After NormalClose global.Count={}", global.size());
		});
		return 0;
	}

	private static long processKeepAliveRequest(@NotNull KeepAlive rpc) {
		if (rpc.getSender().getUserState() == null) {
			rpc.SendResultCode(AcquireNotLogin);
			return 0;
		}
		var sender = (CacheHolder)rpc.getSender().getUserState();
		sender.setActiveTime(System.currentTimeMillis());
		rpc.SendResult();
		return 0;
	}

	private long processAcquireRequest(@NotNull Acquire rpc) {
		var acquireState = rpc.Argument.state;
		if (ENABLE_PERF)
			perf.onAcquireBegin(rpc, acquireState);
		rpc.Result.globalKey = rpc.Argument.globalKey;
		rpc.Result.state = acquireState; // default success

		if (rpc.getSender().getUserState() == null) {
			rpc.Result.state = StateInvalid;
			rpc.SendResultCode(AcquireNotLogin);
		} else {
			try {
				var sender = (CacheHolder)rpc.getSender().getUserState();
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
			perf.onAcquireEnd(rpc, acquireState);
		return 0;
	}

	public static final class CountDownFuture extends RedirectFuture<Object> {
		private static final @NotNull VarHandle vhCounter;

		static {
			try {
				vhCounter = MethodHandles.lookup().findVarHandle(CountDownFuture.class, "counter", int.class);
			} catch (ReflectiveOperationException e) {
				throw new ExceptionInInitializerError(e);
			}
		}

		private volatile @SuppressWarnings("unused") int counter;

		public @NotNull CountDownFuture createOne() {
			vhCounter.getAndAdd(this, 1);
			return this;
		}

		public void finishOne() {
			if ((int)vhCounter.getAndAdd(this, -1) == 0)
				setResult(null);
		}

		@Override
		public @NotNull RedirectFuture<Object> then(@NotNull Action1<@Nullable Object> onResult) {
			finishOne();
			return super.then(onResult);
		}
	}

	private void releaseAsync(@NotNull CacheHolder sender, @NotNull Binary _gKey, @NotNull CountDownFuture future) {
		var cs = global.computeIfAbsent(_gKey, CacheState::new);
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
					releaseAsync(sender, gKey, future); // retry
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
					releaseAsync(sender, gKey, future); // retry
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
					global.remove(gKey);
				} else
					cs.acquireStatePending = StateInvalid;
				cs.lock.notifyAllWait();
				future.finishOne();
			} catch (Throwable ex) { // AsyncLock.enter会捕获吞掉异常；异常路径必须落实finishOne，
				// 否则CountDownFuture永不完成，processLogin/processNormalClose的应答永不发出（客户端超时重试风暴）。
				logger.error("ReleaseAsync", ex);
				future.finishOne();
			} finally {
				// 异常逃逸时复位本次占住的StateRemoving并唤醒等待者；正常路径已自行复位（StateRemoved或StateInvalid）。
				if (ownsRemoving && cs.acquireStatePending == StateRemoving) {
					cs.acquireStatePending = StateInvalid;
					cs.lock.notifyAllWait();
				}
			}
		});
	}

	private void releaseAsync(@NotNull Acquire rpc) {
		// sender入口一次捕获（理由同acquireShareAsync）：kick后延续阶段重读getUserState()为null，
		// 异常发生在StateRemoving置位之后会把该状态留在cs上，等待者永不唤醒（key冻结）。
		var sender = (CacheHolder)rpc.getSender().getUserState();
		if (sender == null) { // 与processAcquireRequest的检查之间有kick竞争窗口
			rpc.Result.state = StateInvalid;
			rpc.SendResultCode(AcquireNotLogin);
			if (ENABLE_PERF)
				perf.onAcquireEnd(rpc, StateInvalid);
			return;
		}
		var cs = global.computeIfAbsent(rpc.Argument.globalKey, CacheState::new);
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
					releaseAsync(rpc); // retry
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
							perf.onAcquireEnd(rpc, StateInvalid);
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
					releaseAsync(rpc); // retry
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
					global.remove(gKey);
				} else
					cs.acquireStatePending = StateInvalid;
				cs.lock.notifyAllWait();
				rpc.Result.state = StateInvalid;
				rpc.SendResultCode(0);
				if (ENABLE_PERF)
					perf.onAcquireEnd(rpc, StateInvalid);
			} catch (Throwable ex) { // AsyncLock.enter会捕获吞掉异常；异常路径也必须应答（对齐acquire*Async）。
				logger.error("ReleaseAsync", ex);
				rpc.Result.state = StateInvalid;
				rpc.SendResultCode(AcquireException);
				if (ENABLE_PERF)
					perf.onAcquireEnd(rpc, StateInvalid);
			} finally {
				// 异常逃逸时复位本次占住的StateRemoving并唤醒等待者；正常路径已自行复位（StateRemoved或StateInvalid）。
				if (ownsRemoving && cs.acquireStatePending == StateRemoving) {
					cs.acquireStatePending = StateInvalid;
					cs.lock.notifyAllWait();
				}
			}
		});
	}

	private void acquireShareAsync(@NotNull Acquire rpc) {
		// sender入口一次捕获（对齐同步版acquireShare/acquireModify先例）：回调每次重入都重读
		// getUserState()，会话被daemon kick（置null）后延续阶段得到null，NPE发生在申请位置位之后
		// 且无复位——pending永久泄漏，该key上所有后续acquire/release进入永不唤醒的等待（key冻结）。
		var sender = (CacheHolder)rpc.getSender().getUserState();
		if (sender == null) { // 与processAcquireRequest的检查之间有kick竞争窗口
			rpc.Result.state = StateInvalid;
			rpc.SendResultCode(AcquireNotLogin);
			if (ENABLE_PERF)
				perf.onAcquireEnd(rpc, StateShare);
			return;
		}
		var cs = global.computeIfAbsent(rpc.Argument.globalKey, CacheState::new);
		var state = new Object() {
			int stage;
			int reduceResultState;
			Id128 reduceTid;
		};
		cs.lock.enter(() -> {
			// stage==2的延续持有此前阶段设置的申请位；本次进入新设置的也会置位
			var ownsPending = state.stage == 2;
			try {
				if (state.stage == 0) {
					if (cs.acquireStatePending == StateRemoved) {
						cs.lock.leave();
						acquireShareAsync(rpc); // retry
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
									perf.onAcquireEnd(rpc, StateShare);
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
									perf.onAcquireEnd(rpc, StateShare);
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
						acquireShareAsync(rpc); // retry
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
							cs.lock.notifyAllWait(); // 归还申请位必须唤醒等待者（FND4-52，对齐acquireModifyAsync同分支）
							if (isDebugEnabled)
								logger.debug("4 {} {} {}", sender, StateShare, cs);
							rpc.Result.state = StateModify;
							rpc.SendResultCode(AcquireShareAlreadyIsModify);
							if (ENABLE_PERF)
								perf.onAcquireEnd(rpc, StateShare);
							return;
						}

						state.reduceResultState = StateReduceNetError; // 默认网络错误。。
						if (cs.modify.reduceWaitLater(gKey, rpc.getResultCode(), r -> {
							if (ENABLE_PERF)
								perf.onReduceEnd(r);
							if (r.isTimeout()) {
								logger.warn("acquireShare: reduce timeout. so={}, time={}, arg={}",
										r.getSender(), r.getTimeout(), r.Argument);
								state.reduceResultState = StateReduceRpcTimeout;
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
							perf.onOthers("XXX Fresh " + StateShare);
						rpc.Result.state = StateInvalid;
						rpc.SendResultCode(StateReduceErrorFreshAcquire);
						if (ENABLE_PERF)
							perf.onAcquireEnd(rpc, StateShare);
						return;

					default:
						// 包含协议返回错误的值的情况。
						// case StateReduceRpcTimeout: // 11
						// case StateReduceException: // 12
						// case StateReduceNetError: // 13
						cs.acquireStatePending = StateInvalid;
						cs.lock.notifyAllWait();
						if (ENABLE_PERF)
							perf.onOthers("XXX 8 " + StateShare + " " + state.reduceResultState);
						// logger.error("XXX 8 {} {} {} {}", sender, StateShare, cs, state.reduceResultState);
						rpc.Result.state = StateInvalid;
						rpc.SendResultCode(AcquireShareFailed);
						if (ENABLE_PERF)
							perf.onAcquireEnd(rpc, StateShare);
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
						perf.onAcquireEnd(rpc, StateShare);
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
					perf.onAcquireEnd(rpc, StateShare);
			} catch (Throwable ex) { // AsyncLock.enter会捕获吞掉异常；异常路径也必须应答
				logger.error("AcquireShareAsync", ex);
				rpc.Result.state = StateInvalid;
				rpc.SendResultCode(AcquireException);
				if (ENABLE_PERF)
					perf.onAcquireEnd(rpc, StateShare);
			} finally {
				// 异常逃逸时复位本次占住的申请位并唤醒等待者；正常路径已自行复位（StateInvalid）
				if (ownsPending && (cs.acquireStatePending == StateShare || cs.acquireStatePending == StateModify)) {
					cs.acquireStatePending = StateInvalid;
					cs.lock.notifyAllWait();
				}
			}
		});
	}

	private void acquireModifyAsync(@NotNull Acquire rpc) {
		// sender入口一次捕获（理由同acquireShareAsync）：kick后延续阶段重读getUserState()为null，
		// NPE发生在申请位置位之后且无复位——pending永久泄漏，key冻结。
		var sender = (CacheHolder)rpc.getSender().getUserState();
		if (sender == null) { // 与processAcquireRequest的检查之间有kick竞争窗口
			rpc.Result.state = StateInvalid;
			rpc.SendResultCode(AcquireNotLogin);
			if (ENABLE_PERF)
				perf.onAcquireEnd(rpc, StateModify);
			return;
		}
		var cs = global.computeIfAbsent(rpc.Argument.globalKey, CacheState::new);
		var state = new Object() {
			int stage;
			int reduceResultState;
			Id128 reduceTid;
		};
		cs.lock.enter(() -> {
			// stage==2的延续持有此前阶段设置的申请位；本次进入新设置的也会置位
			var ownsPending = state.stage == 2;
			try {
				if (state.stage == 0) {
					if (cs.acquireStatePending == StateRemoved) {
						cs.lock.leave();
						acquireModifyAsync(rpc); // retry
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
									perf.onAcquireEnd(rpc, StateModify);
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
									perf.onAcquireEnd(rpc, StateModify);
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
						acquireModifyAsync(rpc); // retry
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
								perf.onAcquireEnd(rpc, StateModify);
							return;
						}

						state.reduceResultState = StateReduceNetError; // 默认网络错误。
						if (cs.modify.reduceWaitLater(gKey, rpc.getResultCode(), r -> {
							if (ENABLE_PERF)
								perf.onReduceEnd(r);
							if (r.isTimeout()) {
								logger.warn("acquireModify: reduce timeout. so={}, time={}, arg={}",
										r.getSender(), r.getTimeout(), r.Argument);
								state.reduceResultState = StateReduceRpcTimeout;
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
							perf.onOthers("XXX Fresh " + StateModify);
						rpc.Result.state = StateInvalid;
						rpc.SendResultCode(StateReduceErrorFreshAcquire);
						if (ENABLE_PERF)
							perf.onAcquireEnd(rpc, StateModify);
						return;

					default:
						// case StateReduceRpcTimeout: // 11
						// case StateReduceException: // 12
						// case StateReduceNetError: // 13
						cs.acquireStatePending = StateInvalid;
						cs.lock.notifyAllWait();
						if (ENABLE_PERF)
							perf.onOthers("XXX 9 " + StateModify + " " + state.reduceResultState);
						// logger.error("XXX 9 {} {} {} {}", sender, StateModify, cs, state.reduceResultState);
						rpc.Result.state = StateInvalid;
						rpc.SendResultCode(AcquireModifyFailed);
						if (ENABLE_PERF)
							perf.onAcquireEnd(rpc, StateModify);
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
						perf.onAcquireEnd(rpc, StateModify);
					return;
				}

				var reducePending = new ArrayList<KV<CacheHolder, Reduce>>();
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
							perf.onReduceEnd(r);
						// cs.lock.enter(() -> {
						// 	cs.Share.remove(c);
						allReduceFuture.finishOne();
						// });
						return 0;
					});
					if (reduce == null) {
						// 网络错误不再认为成功。整个降级失败，要中断降级。
						// 已经发出去的降级请求要等待并处理结果。后面处理。
						allReduceFuture.finishOne();
						break;
					}
					reducePending.add(KV.create(c, reduce));
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
						// 失败了，要把原来是share的sender恢复。先这样吧。
						if (senderIsShare)
							cs.share.add(sender);
						cs.acquireStatePending = StateInvalid;
						cs.lock.notifyAllWait();
						if (ENABLE_PERF)
							perf.onOthers("XXX 10 " + StateModify + ' ' + errorFreshAcquire.value);
						// logger.error("XXX 10 {} {} {}", sender, StateModify, cs);
						rpc.Result.state = StateInvalid;
						if (errorFreshAcquire.value)
							rpc.SendResultCode(StateReduceErrorFreshAcquire); // 这个错误不看做失败，允许发送方继续尝试。
						else
							rpc.SendResultCode(AcquireModifyFailed);
					}
					if (ENABLE_PERF)
						perf.onAcquireEnd(rpc, StateModify);
					// 很好，网络失败不再看成成功，发现除了加break，
					// 其他处理已经能包容这个改动，都不用动。
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
					perf.onAcquireEnd(rpc, StateModify);
			} finally {
				// 异常逃逸时复位本次占住的申请位并唤醒等待者；正常路径已自行复位（StateInvalid）
				if (ownsPending && (cs.acquireStatePending == StateShare || cs.acquireStatePending == StateModify)) {
					cs.acquireStatePending = StateInvalid;
					cs.lock.notifyAllWait();
				}
			}
		});
	}

	private static final class CacheState {
		final @NotNull Binary globalKey; // 这里的引用同global map的key,用于给CacheHolder里的map相同的key引用
		final IdentityHashSet<CacheHolder> share = new IdentityHashSet<>();
		final AsyncLock lock = new AsyncLock(useSyncLock);
		CacheHolder modify;
		int acquireStatePending = StateInvalid;

		public CacheState(@NotNull Binary gKey) {
			globalKey = gKey;
		}

		int getSenderCacheState(@NotNull CacheHolder sender) {
			if (modify == sender)
				return StateModify;
			if (share.contains(sender))
				return StateShare;
			return StateInvalid;
		}

		@Override
		public @NotNull String toString() {
			StringBuilder sb = new StringBuilder();
			ByteBuffer.BuildString(sb, share);
			return String.format("(P%d M%s S%s)", acquireStatePending, modify, sb);
		}
	}

	private static final class CacheHolder extends ReentrantLock {
		// 必须持有owner实例：本类此前硬编码引用单例（instance.server/achillesHeelConfig/perf），
		// "可同JVM启动多实例"名不副实——单例未启动时自建实例Login即NPE
		// （tryBindSocket的instance.server为null）；单例同时启动时则静默串用单例的
		// server/config/perf，跨实例状态错乱。
		private final GlobalCacheManagerAsyncServer owner;
		final ConcurrentHashMap<Binary, Integer> acquired = new ConcurrentHashMap<>();

		CacheHolder(GlobalCacheManagerAsyncServer owner) {
			this.owner = owner;
		}
		long sessionId;
		int globalCacheManagerHashIndex;
		private volatile long activeTime = System.currentTimeMillis();
		private volatile long lastErrorTime;
		private boolean logined = false; // 改成State，也能表示已经kick过，下一次不再kick？
		private volatile boolean debugMode;

		long getActiveTime() {
			return activeTime;
		}

		void setActiveTime(long value) {
			activeTime = value;
		}

		void setDebugMode(boolean debugMode) {
			this.debugMode = debugMode;
		}

		// not under lock
		void kick() {
			// FND7-18（自同步版移植）：stop()等待在飞扫描有超时预算，超预算继续拆依赖后晚到的扫描
			// 会读到null的server——缓存引用判空跳过kick，不NPE中断本轮forEach的其余会话检查。
			var srv = owner.server;
			var peer = srv != null ? srv.GetSocket(sessionId) : null;
			if (null != peer) {
				peer.setUserState(null); // 来自这个Agent的所有请求都会失败。
				peer.close(kickException); // 关闭连接，强制Agent重新登录。
			}
			sessionId = 0; // 清除网络状态。
		}

		boolean tryBindSocket(@NotNull AsyncSocket newSocket, int globalCacheManagerHashIndex, boolean login) {
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
					logger.warn("TryBindSocket: already bound! newSocket.getUserState() != null, SessionId={}",
							newSocket.getSessionId());
					return false; // 不允许再次绑定。Login Or ReLogin 只能发一次。
				}

				// S2-F1（自同步版移植）：对齐kick()防护——stop()拆依赖窗口内server已被置null，
				// 裸解引用NPE会中断在飞会话的绑定链；判空直接失败。
				var srv = owner.server;
				if (srv == null)
					return false;
				var socket = srv.GetSocket(sessionId);
				if (socket == null) {
					// old socket not exist or has lost.
					sessionId = newSocket.getSessionId();
					newSocket.setUserState(this);
					this.globalCacheManagerHashIndex = globalCacheManagerHashIndex;
					// 绑定即刷新活跃时刻（持锁）：achillesHeelDaemon的锁内复查据此识别
					// 新incarnation，避免旧超时判定kick掉刚Login的新连接。
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

		boolean tryUnBindSocket(@NotNull AsyncSocket oldSocket) {
			lock();
			try {
				// 这里检查比较严格，但是这些检查应该都不会出现。

				if (oldSocket.getUserState() != this)
					return false; // not bind to this

				// S2-F1（自同步版移植）：对齐kick()防护——close后server为null，裸引用NPE中断解绑。
				var srv = owner.server;
				if (srv == null)
					return false;
				var current = srv.GetSocket(sessionId);
				if (current != null && current != oldSocket)
					return false; // not same socket

				sessionId = 0;
				return true;
			} finally {
				unlock();
			}
		}

		@Override
		public @NotNull String toString() {
			return String.valueOf(sessionId);
		}

		void setError() {
			long now = System.currentTimeMillis();
			if (now - lastErrorTime > owner.achillesHeelConfig.globalForbidPeriod)
				lastErrorTime = now;
		}

		/**
		 * 返回null表示发生了网络错误，或者应用服务器已经关闭。
		 */
		@Nullable Reduce reduceWaitLater(@NotNull Binary gkey, long fresh,
										 @NotNull ProtocolHandle<Rpc<BGlobalKeyState, BGlobalKeyState>> handle) {
			try {
				if (System.currentTimeMillis() - lastErrorTime < owner.achillesHeelConfig.globalForbidPeriod)
					return null;
				AsyncSocket peer = owner.server.GetSocket(sessionId);
				if (peer != null) {
					var reduce = new Reduce(gkey, StateInvalid);
					reduce.setResultCode(fresh);
					if (ENABLE_PERF)
						owner.perf.onReduceBegin(reduce);
					if (reduce.Send(peer, handle, owner.achillesHeelConfig.reduceTimeout))
						return reduce;
					if (ENABLE_PERF)
						owner.perf.onReduceCancel(reduce);
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
		public void OnSocketAccept(@NotNull AsyncSocket so) throws Exception {
			logger.info("OnSocketAccept {}", so);
			// so.UserState = new CacheHolder(so.SessionId); // Login ReLogin 的时候初始化。
			super.OnSocketAccept(so);
		}

		@Override
		public void OnSocketClose(@NotNull AsyncSocket so, @Nullable Throwable e) throws Exception {
			logger.info("OnSocketClose {}", so);
			var session = (CacheHolder)so.getUserState();
			if (session != null)
				session.tryUnBindSocket(so); // unbind when login
			super.OnSocketClose(so, e);
		}

		@Override
		public void dispatchProtocol(long typeId, @NotNull ByteBuffer bb,
									 @NotNull ProtocolFactoryHandle<?> factoryHandle,
									 @Nullable AsyncSocket so) throws Exception {
			var p = decodeProtocol(typeId, bb, factoryHandle, so);
			p.handle(this, factoryHandle); // 所有协议处理几乎无阻塞,可放心直接跑在IO线程上
		}

		@Override
		public <P extends Protocol<?>> void dispatchRpcResponse(@NotNull P rpc,
																@NotNull ProtocolHandle<P> responseHandle,
																@NotNull ProtocolFactoryHandle<?> factoryHandle) {
			// 在新的decode-dispatch流程中，上面的dispatchProtocol直接执行操作，实际上包含了rpc.handle，
			// 这个函数不会被触发了。先保留在这里。
			try {
				responseHandle.handle(rpc);
			} catch (Throwable e) { // logger.error
				logger.error("dispatchRpcResponse exception:", e);
			}
		}
	}

	// 开关缺值时args[++i]抛无上下文的AIOOBE；这里给出明确的参数错误（SM1-F4同型判例）。
	private static String requireValue(String[] args, int index, String name) {
		if (index >= args.length)
			throw new IllegalArgumentException("argument '" + name + "' requires a value");
		return args[index];
	}

	public static void main(@NotNull String @NotNull [] args) throws Exception {
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
					port = Integer.parseInt(requireValue(args, ++i, "-port"));
					break;
				case "-threads":
					threadCount = Integer.parseInt(requireValue(args, ++i, "-threads"));
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
				Executors.newSingleThreadScheduledExecutor(
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
