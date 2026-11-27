package Zeze.Services;

import java.util.Objects;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Builtin.GlobalCacheManagerWithRaft.Acquire;
import Zeze.Builtin.GlobalCacheManagerWithRaft.BAcquiredState;
import Zeze.Builtin.GlobalCacheManagerWithRaft.BCacheState;
import Zeze.Builtin.GlobalCacheManagerWithRaft.BReduceParam;
import Zeze.Builtin.GlobalCacheManagerWithRaft.Cleanup;
import Zeze.Builtin.GlobalCacheManagerWithRaft.KeepAlive;
import Zeze.Builtin.GlobalCacheManagerWithRaft.Login;
import Zeze.Builtin.GlobalCacheManagerWithRaft.NormalClose;
import Zeze.Builtin.GlobalCacheManagerWithRaft.ReLogin;
import Zeze.Builtin.GlobalCacheManagerWithRaft.Reduce;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.ProtocolHandle;
import Zeze.Net.Rpc;
import Zeze.Net.RpcTimeoutException;
import Zeze.Raft.RaftConfig;
import Zeze.Raft.RocksRaft.Procedure;
import Zeze.Raft.RocksRaft.Record;
import Zeze.Raft.RocksRaft.Rocks;
import Zeze.Raft.RocksRaft.RocksMode;
import Zeze.Raft.RocksRaft.Table;
import Zeze.Raft.RocksRaft.TableTemplate;
import Zeze.Raft.RocksRaft.Transaction;
import Zeze.Util.DaemonTimer;
import Zeze.Util.Id128;
import Zeze.Util.IdentityHashSet;
import Zeze.Util.KV;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.OutLong;
import Zeze.Util.OutObject;
import Zeze.Util.Task;
import Zeze.Util.TaskSpec;
import Zeze.Util.ZezeCounter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.jspecify.annotations.Nullable;

/**
 * Raft版全局缓存管理器服务器：全局锁所有权状态（Global表）与各Server已获取记录
 * （Session#serverId表）经rocks-raft复制持久化，申请位等瞬态由pessimism锁与回滚动作维护。
 */
public class GlobalCacheManagerWithRaft
		extends AbstractGlobalCacheManagerWithRaft implements Closeable, GlobalCacheManagerConst {
	static {
		var level = Level.toLevel(System.getProperty("logLevel"), Level.INFO);
		((LoggerContext)LogManager.getContext(false)).getConfiguration().getRootLogger().setLevel(level);
	}

	private static final boolean ENABLE_PERF = true;
	private static final Logger logger = LogManager.getLogger(GlobalCacheManagerWithRaft.class);
	private static final boolean isDebugEnabled = logger.isDebugEnabled();
	public static final int GlobalSerialIdAtomicLongIndex = 0;

	private final Rocks rocks;
	private final GlobalLocks locks = new GlobalLocks();
	/**
	 * 全局记录分配状态。
	 */
	private final Table<Binary, BCacheState> globalStates;
	/**
	 * 每个服务器已分配记录。
	 * 这是个Table模板，使用的时候根据ServerId打开真正的存储表。
	 */
	private final TableTemplate<Binary, BAcquiredState> serverAcquiredTemplate;
	// 会话表：key 为 ServerId，Login/ReLogin 时按需创建且不删除（ServerId 数量有限）
	private final LongConcurrentHashMap<CacheHolder> sessions = new LongConcurrentHashMap<>();

	private final GlobalCacheManagerServer.GCMConfig gcmConfig = new GlobalCacheManagerServer.GCMConfig();
	private final AchillesHeelConfig achillesHeelConfig;
	private final DaemonTimer achillesHeelDaemonTimer;
	// 停机标记：daemon 在模块锁内首查，置位后立即退出不再触碰 rocks
	private volatile boolean closed;
	private final GlobalCacheManagerPerf perf;
	private final AtomicLong serialId = new AtomicLong();

	record Leadership(long term, long readyNanos) {
		boolean reconnectWindowElapsed(long currentTerm, long nowNanos, long timeoutMillis) {
			return term == currentTerm && nowNanos - readyNanos > TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
		}
	}

	private volatile @Nullable Leadership leadership;

	// Session 表名前缀（TableTemplate.openTable 生成 "Session#<serverId>"）
	private static final String SessionTableNamePrefix = "Session#";
	private volatile boolean warnedStorageNull; // storage==null 仅告警一次，避免每轮刷屏

	// 外面主动提供装载配置，需要在Load之前把这个实例注册进去。
	public GlobalCacheManagerServer.GCMConfig getGcmConfig() {
		return gcmConfig;
	}

	public GlobalCacheManagerWithRaft(String raftName) throws Exception {
		this(raftName, null, null);
	}

	public GlobalCacheManagerWithRaft(String raftName, @Nullable RaftConfig raftConf) throws Exception {
		this(raftName, raftConf, null);
	}

	public GlobalCacheManagerWithRaft(String raftName, @Nullable RaftConfig raftConf, @Nullable Config config) throws Exception {
		if (config == null)
			config = Config.load();
		config.parseCustomize(this.gcmConfig);
		achillesHeelConfig = new AchillesHeelConfig(gcmConfig.maxNetPing,
				gcmConfig.serverProcessTime, gcmConfig.serverReleaseTimeout);
		rocks = new Rocks(raftName, RocksMode.Pessimism, raftConf, config);

		RegisterRocksTables(rocks);
		//noinspection DataFlowIssue
		RegisterProtocols(rocks.getRaft().getServer());

		var globalTemplate = rocks.<Binary, BCacheState>getTableTemplate("Global");
		//noinspection DataFlowIssue
		globalTemplate.setLruTryRemoveCallback(this::globalLruTryRemove);
		globalStates = globalTemplate.openTable(0);
		serverAcquiredTemplate = Objects.requireNonNull(rocks.getTableTemplate("Session"));

		// 须在 server.start() 前注册：就任主节点时即刻从 storage 重建内存 sessions 视图（回调运行在 Raft apply 路径，必须快且不抛异常）
		var raft = Objects.requireNonNull(rocks.getRaft());
		raft.setOnLeaderReady(() -> {
			leadership = new Leadership(raft.getLogSequence().getTerm(), System.nanoTime());
			try {
				rebuildSessionsFromStorage();
			} catch (Throwable e) {
				logger.error("RebuildSessionsFromStorage(onLeaderReady) exception", e);
			}
		});

		ZezeCounter.tryInit();

		// 请求处理会引用 perf 与安全期限配置，须全部就绪后才能开放监听。
		if (ENABLE_PERF)
			perf = new GlobalCacheManagerPerf(raftName, serialId); // Rocks.AtomicLong(GlobalSerialIdAtomicLongIndex));

		// Global的守护不需要独立线程。当出现异常问题不能工作时，没有释放锁是不会造成致命问题的。
		achillesHeelDaemonTimer = new DaemonTimer("GlobalCacheManagerWithRaft.AchillesHeelDaemon", 5000,
				this::achillesHeelDaemon);
		try {
			rocks.getRaft().getServer().start();
			achillesHeelDaemonTimer.start();
		} catch (Throwable e) {
			// 构造失败后调用方拿不到实例，必须在这里回收提前就绪的周期任务及监听资源。
			try {
				close();
			} catch (Throwable cleanupError) {
				e.addSuppressed(cleanupError);
			}
			throw e;
		}
	}

	private void achillesHeelDaemon() {
		lock();
		try {
			// closed 检查与后续 rocks 访问均在模块锁内，与 close() 锁屏障互斥，避免关库后访问原生句柄
			if (closed)
				return;
			achillesHeelDaemonLocked(System.currentTimeMillis());
		} finally {
			unlock();
		}
	}

	private void achillesHeelDaemonLocked(long now) {
		var raft = rocks.getRaft();
		var leader = raft != null && raft.isLeader();
		if (leader) {
			var fireTerm = raft.getLogSequence().getTerm();
			// Leader 期间每轮幂等对账重建会话视图，异常独立兜底以免跳过超时检查
			try {
				rebuildSessionsFromStorage();
			} catch (Throwable e) {
				logger.error("RebuildSessionsFromStorage exception", e);
			}
			var currentLeadership = leadership;
			// 新任期先给客户端一个完整的重连窗口，避免旧 activeTime 导致误回收仍存活会话的权限
			if (currentLeadership == null || !currentLeadership.reconnectWindowElapsed(fireTerm,
					System.nanoTime(), achillesHeelConfig.globalDaemonTimeout))
				return;
			sessions.forEach(session -> {
				session.lock();
				try {
					// 必须在锁内复查超时，防止踢掉刚在检查与加锁之间完成重连的新连接
					if (now - session.getActiveTime() > achillesHeelConfig.globalDaemonTimeout && !session.debugMode
							&& raft.isLeader() && raft.getLogSequence().getTerm() == fireTerm) {
						session.kick();
						var fireGeneration = session.generation;
						var Acquired = serverAcquiredTemplate.openTable(session.serverId);
						try {
							var releaseCount = new OutLong();
							Acquired.walkKey(key -> {
								// 在循环中删除。这样虽然效率低些，但是能处理更多情况。
								if (raft.isLeader() && raft.getLogSequence().getTerm() == fireTerm) {
									var rc = release(session, key, fireGeneration, fireTerm);
									if (rc != 0) {
										logger.error("AchillesHeelDaemon.Release session={} rc={}", session, rc);
										return false;
									}
									++releaseCount.value;
									return true;
								}
								return false;
							});
							session.setActiveTime(System.currentTimeMillis());
							if (releaseCount.value > 0)
								logger.info("AchillesHeelDaemon.Release session={} count={}", session, releaseCount.value);
						} catch (Throwable e) {
							logger.error("AchillesHeelDaemon.Release {} exception", session, e);
						} finally {
							// server一直没有恢复时，刷新activeTime降低Release频率。
							// 更完善的做法是session全部release后删除之，但删除session的并发较复杂。
							session.setActiveTime(System.currentTimeMillis());
						}
					}
				} finally {
					session.unlock();
				}
			});
		}
	}

	public Rocks getRocks() {
		return rocks;
	}

	private static BAcquiredState newAcquiredState(int state) {
		BAcquiredState acquiredState = new BAcquiredState();
		acquiredState.setState(state);
		return acquiredState;
	}

	// 从 storage 已存在的 Session#<serverId> 列族表重建内存 sessions 视图（不修改已有 Holder），
	// 使新 Leader 能触达并超时回收切主前已失联 serverId 的锁权限
	private void rebuildSessionsFromStorage() {
		var storage = rocks.getStorage();
		if (storage == null) {
			if (!warnedStorageNull) {
				warnedStorageNull = true;
				logger.warn("RebuildSessionsFromStorage storage == null, skip rebuild");
			}
			return;
		}
		int rebuilt = 0;
		for (var name : storage.getTableMap().keySet()) {
			if (!name.startsWith(SessionTableNamePrefix))
				continue;
			long serverId;
			try {
				serverId = Long.parseLong(name.substring(SessionTableNamePrefix.length()));
			} catch (NumberFormatException e) {
				continue;
			}
			if (serverId < 0 || serverId > Integer.MAX_VALUE)
				continue;
			if (sessions.putIfAbsent(serverId, new CacheHolder(this, (int)serverId)) == null)
				++rebuilt;
		}
		if (rebuilt > 0)
			logger.info("LeaderSessionsRebuilt count={}", rebuilt);
	}

	@Override
	protected long ProcessAcquireRequest(Acquire rpc) throws Exception {
		var acquireState = rpc.Argument.getState();
		if (ENABLE_PERF)
			perf.onAcquireBegin(rpc, acquireState);
		//noinspection DataFlowIssue
		var sender = (CacheHolder)rpc.getSender().getUserState();
		if (sender != null)
			sender.setActiveTime(System.currentTimeMillis());
		rpc.Result.setGlobalKey(rpc.Argument.getGlobalKey());
		rpc.Result.setState(acquireState); // default success

		long result;
		try {
			if (sender == null) {
				rpc.Result.setState(StateInvalid);
				// 没有登录重做。登录是Agent自动流程的一部分，应该稍后重试。
				rpc.SendResultCode(Zeze.Transaction.Procedure.RaftRetry);
				result = 0;
			} else {
				long fireGeneration;
				sender.lock();
				try {
					fireGeneration = rpc.getSender().getUserState() == sender
							&& sender.sessionId == rpc.getSender().getSessionId() ? sender.generation : -1;
				} finally {
					sender.unlock();
				}
				if (acquireState == StateInvalid && fireGeneration == -1) {
					rpc.Result.setState(StateInvalid);
					rpc.SendResultCode(Zeze.Transaction.Procedure.RaftRetry);
					return 0;
				}
				var raft = rocks.getRaft();
				var fireTerm = raft != null ? raft.getLogSequence().getTerm() : -1;
				var proc = new Procedure(rocks, () -> {
					switch (acquireState) {
					case StateInvalid: // release
							rpc.Result.setState(release(sender, rpc.Argument.getGlobalKey(), true, fireGeneration, fireTerm));
						rpc.setResultCode(0);
						return 0;
					case StateShare:
						return acquireShare(rpc, sender);
					case StateModify:
						return acquireModify(rpc, sender);
					default:
						rpc.Result.setState(StateInvalid);
						rpc.setResultCode(0);
						return AcquireErrorState;
					}
				});
					if (acquireState == StateInvalid)
						proc.setExpectedTerm(fireTerm);
				proc.autoResponse = rpc; // 启用自动发送rpc结果，但不做唯一检查。
				result = proc.call();
			}
		} finally {
			// 始终收尾性能统计，避免 proc.call() 抛异常导致 acquires 映射泄漏
			if (ENABLE_PERF)
				perf.onAcquireEnd(rpc, acquireState);
		}
		return result; // has handle all error.
	}

	private boolean globalLruTryRemove(Binary key, Record<Binary> r) {
		var lockey = locks.get(key);
		if (!lockey.tryLock())
			return false;
		try {
			// 这里不需要设置成StateRemoved。
			// StateRemoved状态表示记录被删除了，而不是被从Cache中清除。
			// AcquireStatePending是瞬时数据（不会被持久化）。
			// 记录从Cache中清除后，可以再次从RocksDb中装载。
			var cs = (BCacheState)r.getValue(); // null when record removed
			if (cs == null || cs.getAcquireStatePending() == StateInvalid) {
				//noinspection DataFlowIssue
				globalStates.getLruCache().remove(key);
				return true;
			}
			return false;
		} finally {
			lockey.unlock();
		}
	}

	private long acquireShare(Acquire rpc, CacheHolder sender) throws InterruptedException {
		var globalTableKey = rpc.Argument.getGlobalKey();
		var fresh = rpc.getResultCode();
		rpc.setResultCode(0);

		while (true) {
			var lockey = Objects.requireNonNull(Transaction.getCurrent()).addPessimismLock(locks.get(globalTableKey));

			BCacheState cs = globalStates.getOrAdd(globalTableKey);
			if (cs.getAcquireStatePending() == StateRemoved)
				continue;

			if (cs.getModify() != -1 && cs.getShare().size() != 0)
				throw new IllegalStateException("CacheState state error");

			while (cs.getAcquireStatePending() != StateInvalid && cs.getAcquireStatePending() != StateRemoved) {
				switch (cs.getAcquireStatePending()) {
				case StateShare:
					if (cs.getModify() == -1)
						throw new IllegalStateException("CacheState state error");

					//noinspection DataFlowIssue
					if (cs.getModify() == sender.serverId) {
						if (isDebugEnabled)
							logger.debug("1 {} {} {}", sender, StateShare, cs);
						rpc.Result.setState(StateInvalid);
						return AcquireShareDeadLockFound; // 事务数据没有改变，回滚
					}
					break;
				case StateModify:
					//noinspection DataFlowIssue
					if (cs.getModify() == sender.serverId || cs.getShare().Contains(sender.serverId)) {
						if (isDebugEnabled)
							logger.debug("2 {} {} {}", sender, StateShare, cs);
						rpc.Result.setState(StateInvalid);
						return AcquireShareDeadLockFound; // 事务数据没有改变，回滚
					}
					break;
				case StateRemoving:
					break;
				}
				if (isDebugEnabled)
					logger.debug("3 {} {} {}", sender, StateShare, cs);
				lockey.await();
				if (cs.getModify() != -1 && cs.getShare().size() != 0)
					throw new IllegalStateException("CacheState state error");
			}

			if (cs.getAcquireStatePending() == StateRemoved)
				continue; // concurrent release.

			cs.setAcquireStatePending(StateShare);
			// AcquireStatePending 为瞬态字段无事务日志，注册回滚动作复位并在释放悲观锁前唤醒等待者
			Transaction.getCurrent().runWhileRollback(() -> {
				if (cs.getAcquireStatePending() == StateShare) {
					cs.setAcquireStatePending(StateInvalid);
					lockey.pulseAll();
				}
			});
			serialId.getAndIncrement();
			//noinspection DataFlowIssue
			var SenderAcquired = serverAcquiredTemplate.openTable(sender.serverId);
			var reduceTid = new OutObject<>(Id128.Zero);
			if (cs.getModify() != -1) {
				if (cs.getModify() == sender.serverId) {
					// 已经是Modify又申请，可能是sender异常关闭，
					// 又重启连上。更新一下。应该是不需要的。
					SenderAcquired.put(globalTableKey, newAcquiredState(StateModify));
					cs.setAcquireStatePending(StateInvalid);
					lockey.pulseAll(); // 归还申请位必须唤醒等待者
					if (isDebugEnabled)
						logger.debug("4 {} {} {}", sender, StateShare, cs);
					rpc.Result.setState(StateModify);
					rpc.setResultCode(AcquireShareAlreadyIsModify);
					return 0; // 可以忽略的错误，数据有改变，需要提交事务。
				}

				var reduceResultState = new OutObject<>(StateReduceNetError); // 默认网络错误。
				var reduceDone = new boolean[]{false}; // 完成标志（回调锁内置位后再pulse）
				if (CacheHolder.reduce(sessions, cs.getModify(), globalTableKey, fresh, r -> {
					if (ENABLE_PERF)
						perf.onReduceEnd(r);
					if (r.isTimeout()) {
						reduceResultState.value = StateReduceRpcTimeout;
					} else if (!GlobalCacheManagerConst.isReduceResultCodeSuccess(r.getResultCode())) {
						// 错误应答的默认StateInvalid不能证明降级；明确的幂等确认码仍允许使用结果。
						reduceResultState.value = StateReduceNetError;
					} else {
						reduceResultState.value = r.Result.getState();
						reduceTid.value = r.Result.getReduceTid();
					}
					lockey.enter();
					try {
						reduceDone[0] = true;
						lockey.pulseAll();
					} finally {
						lockey.exit();
					}
					return 0;
				})) {
					if (isDebugEnabled)
						logger.debug("5 {} {} {}", sender, StateShare, cs);
					// 循环复查完成谓词，防止 Condition 伪唤醒提前走失败分支
					while (!reduceDone[0])
						lockey.await();
				}

				var ModifyAcquired = serverAcquiredTemplate.openTable(cs.getModify());
				switch (Objects.requireNonNull(reduceResultState.value)) {
				case StateShare:
					ModifyAcquired.put(globalTableKey, newAcquiredState(StateShare));
					cs.getShare().add(cs.getModify()); // 降级成功。
					break;

				case StateInvalid:
					// 降到了 Invalid，此时就不需要加入 Share 了。
					ModifyAcquired.remove(globalTableKey);
					break;

				case StateReduceErrorFreshAcquire:
					cs.setAcquireStatePending(StateInvalid);
					if (ENABLE_PERF)
						perf.onOthers("XXX Fresh " + StateShare);
					rpc.Result.setState(StateInvalid);
					lockey.pulseAll(); //notify
					return StateReduceErrorFreshAcquire; // 事务数据没有改变，回滚

				default:
					// 包含协议返回错误的值的情况。
					// case StateReduceRpcTimeout: // 11
					// case StateReduceException: // 12
					// case StateReduceNetError: // 13
					cs.setAcquireStatePending(StateInvalid);
					if (ENABLE_PERF)
						perf.onOthers("XXX 8 " + StateShare + " " + reduceResultState.value);
					rpc.Result.setState(StateInvalid);
					lockey.pulseAll();
					return AcquireShareFailed; // 事务数据没有改变，回滚
				}

				SenderAcquired.put(globalTableKey, newAcquiredState(StateShare));
				cs.setModify(-1);
				cs.getShare().add(sender.serverId);
				cs.setAcquireStatePending(StateInvalid);
				if (isDebugEnabled)
					logger.debug("6 {} {} {}", sender, StateShare, cs);
				lockey.pulseAll();
				rpc.Result.setReduceTid(Objects.requireNonNull(reduceTid.value));
				return 0; // 成功也会自动发送结果.
			}

			SenderAcquired.put(globalTableKey, newAcquiredState(StateShare));
			cs.getShare().add(sender.serverId);
			cs.setAcquireStatePending(StateInvalid);
			if (isDebugEnabled)
				logger.debug("7 {} {} {}", sender, StateShare, cs);
			lockey.pulseAll();
			rpc.Result.setReduceTid(Objects.requireNonNull(reduceTid.value));
			return 0; // 成功也会自动发送结果.
		}
	}

	private long acquireModify(Acquire rpc, CacheHolder sender) throws InterruptedException {
		var globalTableKey = rpc.Argument.getGlobalKey();
		var fresh = rpc.getResultCode();
		rpc.setResultCode(0);

		while (true) {
			var lockey = Objects.requireNonNull(Transaction.getCurrent()).addPessimismLock(locks.get(globalTableKey));

			BCacheState cs = globalStates.getOrAdd(globalTableKey);
			if (cs.getAcquireStatePending() == StateRemoved)
				continue;

			if (cs.getModify() != -1 && cs.getShare().size() != 0)
				throw new IllegalStateException("CacheState state error");

			while (cs.getAcquireStatePending() != StateInvalid && cs.getAcquireStatePending() != StateRemoved) {
				switch (cs.getAcquireStatePending()) {
				case StateShare:
					if (cs.getModify() == -1) {
						logger.error("cs state must be modify");
						throw new IllegalStateException("CacheState state error");
					}
					//noinspection DataFlowIssue
					if (cs.getModify() == sender.serverId) {
						if (isDebugEnabled)
							logger.debug("1 {} {} {}", sender, StateModify, cs);
						rpc.Result.setState(StateInvalid);
						return AcquireModifyDeadLockFound; // 事务数据没有改变，回滚
					}
					break;
				case StateModify:
					//noinspection DataFlowIssue
					if (cs.getModify() == sender.serverId || cs.getShare().Contains(sender.serverId)) {
						if (isDebugEnabled)
							logger.debug("2 {} {} {}", sender, StateModify, cs);
						rpc.Result.setState(StateInvalid);
						return AcquireModifyDeadLockFound; // 事务数据没有改变，回滚
					}
					break;
				case StateRemoving:
					break;
				}
				if (isDebugEnabled)
					logger.debug("3 {} {} {}", sender, StateModify, cs);
				lockey.await();
				if (cs.getModify() != -1 && cs.getShare().size() != 0)
					throw new IllegalStateException("CacheState state error");
			}
			if (cs.getAcquireStatePending() == StateRemoved)
				continue; // concurrent release

			cs.setAcquireStatePending(StateModify);
			// AcquireStatePending 为瞬态字段无事务日志，注册回滚动作复位并唤醒等待者
			Transaction.getCurrent().runWhileRollback(() -> {
				if (cs.getAcquireStatePending() == StateModify) {
					cs.setAcquireStatePending(StateInvalid);
					lockey.pulseAll();
				}
			});
			serialId.getAndIncrement();
			//noinspection DataFlowIssue
			var SenderAcquired = serverAcquiredTemplate.openTable(sender.serverId);
			var reduceTid = new OutObject<>(Id128.Zero);
			if (cs.getModify() != -1) {
				if (cs.getModify() == sender.serverId) {
					// 已经是Modify又申请，可能是sender异常关闭，又重启连上。
					// 更新一下。应该是不需要的。
					SenderAcquired.put(globalTableKey, newAcquiredState(StateModify));
					cs.setAcquireStatePending(StateInvalid);
					if (isDebugEnabled)
						logger.debug("4 {} {} {}", sender, StateModify, cs);
					lockey.pulseAll();
					rpc.setResultCode(AcquireModifyAlreadyIsModify);
					return 0; // 可以忽略的错误，数据有改变，需要提交事务。
				}

				var reduceResultState = new OutObject<>(StateReduceNetError); // 默认网络错误。
				var reduceDone = new boolean[]{false}; // 完成标志（回调锁内置位后再pulse）
				if (CacheHolder.reduce(sessions, cs.getModify(), globalTableKey, fresh, r -> {
					if (ENABLE_PERF)
						perf.onReduceEnd(r);
					if (r.isTimeout()) {
						reduceResultState.value = StateReduceRpcTimeout;
					} else if (!GlobalCacheManagerConst.isReduceResultCodeSuccess(r.getResultCode())) {
						// 错误应答的默认StateInvalid不能证明降级；明确的幂等确认码仍允许使用结果。
						reduceResultState.value = StateReduceNetError;
					} else {
						reduceResultState.value = r.Result.getState();
						reduceTid.value = r.Result.getReduceTid();
					}
					lockey.enter();
					try {
						reduceDone[0] = true;
						lockey.pulseAll();
					} finally {
						lockey.exit();
					}
					return 0;
				})) {
					if (isDebugEnabled)
						logger.debug("5 {} {} {}", sender, StateModify, cs);
					// 同Share侧——谓词循环防伪唤醒直走失败分支。
					while (!reduceDone[0])
						lockey.await();
				}

				var ModifyAcquired = serverAcquiredTemplate.openTable(cs.getModify());
				switch (Objects.requireNonNull(reduceResultState.value)) {
				case StateInvalid:
					ModifyAcquired.remove(globalTableKey);
					break; // reduce success

				case StateReduceErrorFreshAcquire:
					cs.setAcquireStatePending(StateInvalid);
					if (ENABLE_PERF)
						perf.onOthers("XXX Fresh " + StateModify);
					rpc.Result.setState(StateInvalid);
					lockey.pulseAll(); //notify
					return StateReduceErrorFreshAcquire; // 事务数据没有改变，回滚

				default:
					// case StateReduceRpcTimeout: // 11
					// case StateReduceException: // 12
					// case StateReduceNetError: // 13
					cs.setAcquireStatePending(StateInvalid);
					if (ENABLE_PERF)
						perf.onOthers("XXX 9 " + StateModify + " " + reduceResultState.value);
					rpc.Result.setState(StateInvalid);
					lockey.pulseAll();
					return AcquireModifyFailed; // 事务数据没有改变，回滚
				}

				cs.setModify(sender.serverId);
				cs.getShare().remove(sender.serverId);
				SenderAcquired.put(globalTableKey, newAcquiredState(StateModify));
				cs.setAcquireStatePending(StateInvalid);
				lockey.pulseAll();

				if (isDebugEnabled)
					logger.debug("6 {} {} {}", sender, StateModify, cs);
				rpc.Result.setReduceTid(Objects.requireNonNull(reduceTid.value));
				return 0;
			}

			ArrayList<KV<CacheHolder, Reduce>> reducePending = new ArrayList<>();
			IdentityHashSet<CacheHolder> reduceSucceed = new IdentityHashSet<>();
			var waitReduceDone = new boolean[]{false}; // runNow完成标志（锁内置位）
			boolean senderIsShare = false;
			// 先把降级请求全部发送给出去。
			for (var c : cs.getShare()) {
				if (c == sender.serverId) {
					senderIsShare = true;
					reduceSucceed.add(sender);
					continue;
				}
				var kv = CacheHolder.reduceWaitLater(sessions, c, globalTableKey, fresh);
				if (kv == null) {
					// 网络错误不再认为成功。整个降级失败，要中断降级。
					// 已经发出去的降级请求要等待并处理结果。后面处理。
					break;
				}
				reducePending.add(kv);
			}
			// 两种情况不需要发reduce
			// 1. share是空的, 可以直接升为Modify
			// 2. sender是share, 而且reducePending的size是0
			var errorFreshAcquire = new OutObject<>(Boolean.FALSE);
			if (cs.getShare().size() != 0 && (!senderIsShare || !reducePending.isEmpty())) {
				TaskSpec.ofAction(() -> {
					// 一个个等待是否成功。WaitAll 碰到错误不知道怎么处理的，
					// 应该也会等待所有任务结束（包括错误）。
					var freshAcquire = false;
					for (var kv : reducePending) {
						CacheHolder session = kv.getKey();
						Reduce reduce = kv.getValue();
						try {
							//noinspection DataFlowIssue
							reduce.getFuture().await();
								if (reduce.isTimeout()
										|| !GlobalCacheManagerConst.isReduceResultCodeSuccess(reduce.getResultCode())) {
									session.setError();
									if (ENABLE_PERF)
										perf.onReduceEnd(reduce);
									continue;
								}
							switch (reduce.Result.getState()) {
							case StateInvalid:
								reduceSucceed.add(session);
								break;

							case StateReduceErrorFreshAcquire:
								// 这个错误不进入Forbid状态。
								freshAcquire = true;
								break;

							default:
								session.setError();
								logger.error("Reduce {}=>{} AcquireState={} CacheState={} res={}",
										sender, session, StateModify, cs, reduce.Result);
								break;
							}
							if (ENABLE_PERF)
								perf.onReduceEnd(reduce);
						} catch (Throwable ex) { // exception to result.
							if (ENABLE_PERF) {
								if (reduce.isTimeout())
									perf.onReduceEnd(reduce);
								else
									perf.onReduceCancel(reduce);
							}
							session.setError();
							// 等待失败不再看作成功。
							if (Task.getRootCause(ex) instanceof RpcTimeoutException) {
								logger.warn("Reduce Timeout {}=>{} AcquireState={} CacheState={} arg={}",
										sender, session, StateModify, cs, reduce.Argument);
							} else {
								logger.error("Reduce {}=>{} AcquireState={} CacheState={} arg={}",
										sender, session, StateModify, cs, reduce.Argument, ex);
							}
						}
					}
					lockey.enter();
					try {
						errorFreshAcquire.value = freshAcquire;
						waitReduceDone[0] = true;
						lockey.pulseAll();
					} finally {
						lockey.exit();
					}
				}).name("GlobalCacheManagerWithRaft.AcquireModify.WaitReduce").runNow();
				if (isDebugEnabled)
					logger.debug("7 {} {} {}", sender, StateModify, cs);
				// 循环复查完成谓词，防止 Condition 伪唤醒提前读取并发填充中的 reduceSucceed
				while (!waitReduceDone[0])
					lockey.await();
			}

			// 移除成功的。
			for (var it = reduceSucceed.iterator(); it.moveToNext(); ) {
				CacheHolder succeed = it.value();
				if (succeed.serverId != sender.serverId) {
					// sender 不移除：
					// 1. 如果申请成功，后面会更新到Modify状态。
					// 2. 如果申请不成功，恢复 cs.Share，保持 Acquired 不变。
					var KeyAcquired = serverAcquiredTemplate.openTable(succeed.serverId);
					KeyAcquired.remove(globalTableKey);
				}
				cs.getShare().remove(succeed.serverId);
			}

			// 如果前面降级发生中断(break)，这里就不会为0。
			if (cs.getShare().size() != 0) {
				// senderIsShare 在失败的时候，Acquired 没有变化，不需要更新。
				// 失败了，要把原来是share的sender恢复。
				if (senderIsShare)
					cs.getShare().add(sender.serverId);

				cs.setAcquireStatePending(StateInvalid);
				if (ENABLE_PERF)
					perf.onOthers("XXX 10 " + StateModify + ' ' + errorFreshAcquire.value);
				rpc.Result.setState(StateInvalid);
				lockey.pulseAll();
				rpc.setResultCode(Objects.requireNonNull(errorFreshAcquire.value)
						? StateReduceErrorFreshAcquire  // 这个错误码导致Server-RedoAndReleaseLock
						: AcquireModifyFailed); // 这个错误码导致Server事务失败。
				rpc.Result.setReduceTid(Objects.requireNonNull(reduceTid.value));
				return 0; // 可能存在部分reduce成功，需要提交事务。
			}

			SenderAcquired.put(globalTableKey, newAcquiredState(StateModify));
			cs.setModify(sender.serverId);
			cs.setAcquireStatePending(StateInvalid);
			if (isDebugEnabled)
				logger.debug("8 {} {} {}", sender, StateModify, cs);
			lockey.pulseAll();
			rpc.Result.setReduceTid(Objects.requireNonNull(reduceTid.value));
			return 0; // 成功也会自动发送结果.
		}
	}

	private long release(CacheHolder sender, Binary gkey) throws Exception {
		var raft = rocks.getRaft();
		return release(sender, gkey, sender.generation, raft != null ? raft.getLogSequence().getTerm() : -1);
	}

	private long release(CacheHolder sender, Binary gkey, long generation, long term) throws Exception {
		var procedure = rocks.newProcedure(() -> {
			release(sender, gkey, false, generation, term);
			return 0L;
		});
		// 任期约束在实际append的raft锁内检查，不让旧释放跨任期提交。
		procedure.setExpectedTerm(term);
		return procedure.call();
	}

	private long releaseKeys(CacheHolder sender, Iterable<Binary> keys, long generation, long term) throws Exception {
		for (var key : keys) {
			var rc = release(sender, key, generation, term);
			if (rc != 0)
				return rc;
		}
		return 0;
	}

	private int release(CacheHolder sender, Binary gkey, boolean noWait) throws InterruptedException {
		var raft = rocks.getRaft();
		return release(sender, gkey, noWait, sender.generation,
				raft != null ? raft.getLogSequence().getTerm() : -1);
	}

	private int release(CacheHolder sender, Binary gkey, boolean noWait, long generation, long term)
			throws InterruptedException {
		while (true) {
			var lockey = Objects.requireNonNull(Transaction.getCurrent()).addPessimismLock(locks.get(gkey));

			BCacheState cs = globalStates.getOrAdd(gkey);
			if (cs.getAcquireStatePending() == StateRemoved)
				continue;

			while (cs.getAcquireStatePending() != StateInvalid && cs.getAcquireStatePending() != StateRemoved) {
				switch (cs.getAcquireStatePending()) {
				case StateShare:
				case StateModify:
					if (isDebugEnabled)
						logger.debug("Release 0 {} {} {}", sender, gkey, cs);
					if (noWait)
						return getSenderCacheState(cs, sender);
					break;
				case StateRemoving:
					// release 不会导致死锁，等待即可。
					break;
				}
				lockey.await();
			}
			if (cs.getAcquireStatePending() == StateRemoved)
				continue;
			var raft = rocks.getRaft();
			// 代次守护本进程绑定，term 守护跨任期逃逸；悲观锁保持至提交前，防止新 Acquire 插入
			if (sender.generation != generation || raft == null || !raft.isLeader()
					|| raft.getLogSequence().getTerm() != term)
				return getSenderCacheState(cs, sender);
			cs.setAcquireStatePending(StateRemoving);
			// StateRemoving 为瞬态字段无事务日志，回滚时复位并唤醒等待者
			Transaction.getCurrent().runWhileRollback(() -> {
				if (cs.getAcquireStatePending() == StateRemoving) {
					cs.setAcquireStatePending(StateInvalid);
					lockey.pulseAll();
				}
			});

			if (cs.getModify() == sender.serverId)
				cs.setModify(-1);
			cs.getShare().remove(sender.serverId); // always try remove
			serverAcquiredTemplate.openTable(sender.serverId).remove(gkey);

			if (cs.getModify() == -1 && cs.getShare().size() == 0) {
				// 安全的从global中删除，没有并发问题。
				cs.setAcquireStatePending(StateRemoved);
				globalStates.remove(gkey);
				// remove 走 Raft 提交，失败回滚时复位瞬态 StateRemoved，防止后续访问忙自旋或无法逐出缓存
				Transaction.getCurrent().runWhileRollback(() -> {
					if (cs.getAcquireStatePending() == StateRemoved) {
						cs.setAcquireStatePending(StateInvalid);
						lockey.pulseAll();
					}
				});
			} else
				cs.setAcquireStatePending(StateInvalid);
			lockey.pulseAll();
			return StateInvalid;
		}
	}

	private static int getSenderCacheState(BCacheState cs, CacheHolder sender) {
		if (cs.getModify() == sender.serverId)
			return StateModify;
		if (cs.getShare().Contains(sender.serverId))
			return StateShare;
		return StateInvalid;
	}

	@Override
	protected long ProcessLoginRequest(Login rpc) throws Exception {
		var session = sessions.computeIfAbsent(rpc.Argument.getServerId(), serverId -> new CacheHolder(this, (int)serverId));
		var releaseKeys = new ArrayList<Binary>();
		long generation;
		long term;
		//noinspection DataFlowIssue
		session.lock();
		try {
				if (!session.tryBindSocket(Objects.requireNonNull(rpc.getSender()), rpc.Argument.getGlobalCacheManagerHashIndex(), true)) {
				rpc.SendResultCode(LoginBindSocketFail);
				return 0;
			}
			session.setActiveTime(System.currentTimeMillis());
			session.setDebugMode(rpc.Argument.isDebugMode());
			generation = session.generation;
			//noinspection DataFlowIssue
			term = rocks.getRaft().getLogSequence().getTerm();
			serverAcquiredTemplate.openTable(session.serverId).walkKey(key -> {
				releaseKeys.add(key);
				return true;
			});
		} finally {
			session.unlock();
		}
		// 新登录（如逻辑服重启）：先快照再逐个释放，防止迭代期间新到达的 Acquire 被误回收
		var releaseResult = releaseKeys(session, releaseKeys, generation, term);
		if (releaseResult != 0) {
			rpc.SendResultCode(releaseResult);
			return 0;
		}

		rpc.Result.setMaxNetPing(gcmConfig.maxNetPing);
		rpc.Result.setServerProcessTime(gcmConfig.serverProcessTime);
		rpc.Result.setServerReleaseTimeout(gcmConfig.serverReleaseTimeout);

		rpc.SendResultCode(0);
		logger.info("Login {} {}.", rocks.getRaft().getName(), rpc.getSender());
		return 0;
	}

	@Override
	protected long ProcessReLoginRequest(ReLogin rpc) {
		var session = sessions.computeIfAbsent(rpc.Argument.getServerId(), serverId -> new CacheHolder(this, (int)serverId));
		//noinspection DataFlowIssue
		if (!session.tryBindSocket(Objects.requireNonNull(rpc.getSender()), rpc.Argument.getGlobalCacheManagerHashIndex())) {
			rpc.SendResultCode(ReLoginBindSocketFail);
			return 0;
		}
		session.setActiveTime(System.currentTimeMillis());
		session.setDebugMode(rpc.Argument.isDebugMode());
		rpc.SendResultCode(0);
		//noinspection DataFlowIssue
		logger.info("ReLogin {} {}.", rocks.getRaft().getName(), rpc.getSender());
		return 0;
	}

	@Override
	protected long ProcessNormalCloseRequest(NormalClose rpc) throws Exception {
		//noinspection DataFlowIssue
		Object userState = rpc.getSender().getUserState();
		if (!(userState instanceof CacheHolder session)) {
			rpc.SendResultCode(AcquireNotLogin);
			return 0; // not login
		}
		// 解绑前在锁内快照待释放集合与世代/任期，避免解绑后同 serverId 新连接写入的新权限被误回收
		var releaseKeys = new ArrayList<Binary>();
		long generation;
		long term;
		session.lock();
		try {
			var raft = rocks.getRaft();
			if (raft == null) {
				rpc.SendResultCode(NormalCloseUnbindFail);
				return 0;
			}
			generation = session.generation;
			term = raft.getLogSequence().getTerm();
			serverAcquiredTemplate.openTable(session.serverId).walkKey(key -> {
				releaseKeys.add(key);
				return true;
			});
			if (!session.tryUnBindSocket(rpc.getSender())) {
				rpc.SendResultCode(NormalCloseUnbindFail);
				return 0;
			}
		} finally {
			session.unlock();
		}
		rpc.SendResultCode(releaseKeys(session, releaseKeys, generation, term));
		logger.info("NormalClose {} {}", rocks.getRaft().getName(), rpc.getSender());
		return 0;
	}

	@Override
	@SuppressWarnings("ConstantConditions") // 保留对最终初始化配置的防御检查。
	protected long ProcessCleanupRequest(Cleanup rpc) throws Exception {
		if (achillesHeelConfig != null) { // disable cleanup.
			rpc.SendResultCode(CleanupErrorDisabled);
			return 0;
		}

		// 安全性以后加强。
		if (!rpc.Argument.getSecureKey().equals("Ok! verify secure.")) {
			rpc.SendResultCode(CleanupErrorSecureKey);
			return 0;
		}

		var session = sessions.computeIfAbsent(rpc.Argument.getServerId(), serverId -> new CacheHolder(this, (int)serverId));
		var releaseKeys = new ArrayList<Binary>();
		long generation;
		long term;
		session.lock();
		try {
		if (session.globalCacheManagerHashIndex != rpc.Argument.getGlobalCacheManagerHashIndex()) {
			// 多点验证
			rpc.SendResultCode(CleanupErrorGlobalCacheManagerHashIndex);
			return 0;
		}

		if (rocks.getRaft().getServer().GetSocket(session.sessionId) != null) {
			// 连接存在，禁止cleanup。
			rpc.SendResultCode(CleanupErrorHasConnection);
			return 0;
		}
			generation = session.generation;
			term = rocks.getRaft().getLogSequence().getTerm();
			serverAcquiredTemplate.openTable(session.serverId).walkKey(key -> {
				releaseKeys.add(key);
				return true;
			});
		} finally {
			session.unlock();
		}

		// 还有更多的防止出错的手段吗？

		// XXX verify danger
		TaskSpec.ofAction(() -> { // delay 5 mins
			rpc.SendResultCode(releaseKeys(session, releaseKeys, generation, term));
		}).schedule(5 * 60 * 1000);

		return 0;
	}

	@Override
	protected long ProcessKeepAliveRequest(KeepAlive rpc) {
		//noinspection DataFlowIssue
		var sender = (CacheHolder)rpc.getSender().getUserState();
		if (null == sender) {
			rpc.SendResultCode(AcquireNotLogin);
			return 0;
		}
		sender.setActiveTime(System.currentTimeMillis());
		rpc.SendResultCode(Zeze.Transaction.Procedure.Success);
		return 0;
	}

	@Override
	public void close() {
		try {
			// 先置 closed 并停守护，再过模块锁屏障确保在飞扫描已退出，避免关库后并发访问 RocksDB 原生句柄
			closed = true;
			achillesHeelDaemonTimer.stop();
			lock();
			unlock();
			perf.close();
			rocks.close();
		} catch (Exception e) {
			throw Task.forceThrow(e);
		}
	}

	private static final class CacheHolder extends ReentrantLock {
		private static final AtomicLongFieldUpdater<CacheHolder> generationUpdater =
				AtomicLongFieldUpdater.newUpdater(CacheHolder.class, "generation");
		final GlobalCacheManagerWithRaft globalRaft;
		final int serverId;
		private long sessionId;
		private volatile long generation;
		private int globalCacheManagerHashIndex;
		private volatile long activeTime = System.currentTimeMillis();
		private volatile long lastErrorTime;
		private volatile boolean debugMode;

		// 与绑定共用session锁，代次递增不会丢失。
		void kick() {
			lock();
			try {
				generationUpdater.incrementAndGet(this);
				var raft = globalRaft.getRocks().getRaft();
				if (raft != null) {
					var peer = raft.getServer().GetSocket(sessionId);
					if (peer != null) {
						peer.setUserState(null); // 来自这个Agent的所有请求都会失败。
						peer.close(kickException); // 关闭连接，强制Agent重新登录。
					}
				}
				sessionId = 0; // 清除网络状态。
			} finally {
				unlock();
			}
		}

		CacheHolder(GlobalCacheManagerWithRaft globalRaft, int serverId) {
			this.globalRaft = globalRaft;
			this.serverId = serverId;
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

		boolean tryBindSocket(AsyncSocket newSocket, int globalCacheManagerHashIndex) {
			return tryBindSocket(newSocket, globalCacheManagerHashIndex, false);
		}

		private boolean tryBindSocket(AsyncSocket newSocket, int globalCacheManagerHashIndex, boolean newLogin) {
			lock();
			try {
				if (newSocket.getUserState() != null && newSocket.getUserState() != this)
					return false; // 允许重复login|relogin，但不允许切换ServerId。

				var raft = globalRaft.getRocks().getRaft();
				if (raft == null)
					return false;
				var socket = raft.getServer().GetSocket(sessionId);
				if (socket == null || socket == newSocket) {
					// 同一 socket 重复 Login 开启新清理代次；ReLogin 仅恢复连接不清理权限
					if (newLogin || sessionId != newSocket.getSessionId())
						generationUpdater.incrementAndGet(this);
					sessionId = newSocket.getSessionId();
					newSocket.setUserState(this);
					this.globalCacheManagerHashIndex = globalCacheManagerHashIndex;
					// 锁内刷新活跃时间，避免守护线程按旧超时踢掉刚登录的新连接
					activeTime = System.currentTimeMillis();
					return true;
				}
				// 每个ServerId只允许一个实例，已经存在了以后，旧的实例上有状态，阻止新的实例登录成功。
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

				var raft = globalRaft.getRocks().getRaft();
				if (raft == null)
					return false;
				var socket = raft.getServer().GetSocket(sessionId);
				if (socket != null && socket != oldSocket)
					return false; // not same socket

				sessionId = 0;
				oldSocket.setUserState(null); // 已解绑连接的迟到请求不能使用后来重绑的Holder代次
				return true;
			} finally {
				unlock();
			}
		}

		void setError() {
			long now = System.currentTimeMillis();
			if (now - lastErrorTime > globalRaft.achillesHeelConfig.globalForbidPeriod)
				lastErrorTime = now;
		}

		static boolean reduce(LongConcurrentHashMap<CacheHolder> sessions, int serverId, Binary gkey, long fresh,
							  ProtocolHandle<Rpc<BReduceParam, BReduceParam>> response) {
			var session = sessions.get(serverId);
			if (session == null) {
				logger.error("ReduceMissingHolder serverId={}", serverId);
				return false;
			}
			return session.reduce(gkey, fresh, response);
		}

		static @Nullable KV<CacheHolder, Reduce> reduceWaitLater(LongConcurrentHashMap<CacheHolder> sessions, int serverId,
																 Binary gkey, long fresh) {
			CacheHolder session = sessions.get(serverId);
			if (session == null)
				return null;
			Reduce reduce = session.reduceWaitLater(gkey, fresh);
			if (reduce == null)
				return null;
			return KV.create(session, reduce);
		}

		boolean reduce(Binary gkey, long fresh, ProtocolHandle<Rpc<BReduceParam, BReduceParam>> response) {
			Reduce reduce = null;
			try {
				if (System.currentTimeMillis() - lastErrorTime < globalRaft.achillesHeelConfig.globalForbidPeriod)
					return false;
				//noinspection DataFlowIssue
				AsyncSocket peer = globalRaft.getRocks().getRaft().getServer().GetSocket(sessionId);
				if (peer != null) {
					reduce = new Reduce();
					reduce.setResultCode(fresh);
					reduce.Argument.setGlobalKey(gkey);
					reduce.Argument.setState(StateInvalid);
					if (ENABLE_PERF)
						globalRaft.perf.onReduceBegin(reduce);
					if (reduce.Send(peer, response, globalRaft.achillesHeelConfig.reduceTimeout))
						return true;
					if (ENABLE_PERF)
						globalRaft.perf.onReduceCancel(reduce);
					logger.warn("Reduce send failed: {} peer={}, gkey={}", this, peer, gkey);
				} else
					logger.warn("Reduce invalid: {} gkey={}", this, gkey);
			} catch (Exception ex) {
				if (ENABLE_PERF && reduce != null)
					globalRaft.perf.onReduceCancel(reduce);
				// 这里的异常只应该是网络发送异常。
				logger.error("Reduce Exception: {} gkey={}", this, gkey, ex);
			}
			setError();
			return false;
		}

		/**
		 * 返回null表示发生了网络错误，或者应用服务器已经关闭。
		 */
		@Nullable Reduce reduceWaitLater(Binary gkey, long fresh) {
			Reduce reduce = null;
			try {
				if (System.currentTimeMillis() - lastErrorTime < globalRaft.achillesHeelConfig.globalForbidPeriod)
					return null;
				//noinspection DataFlowIssue
				AsyncSocket peer = globalRaft.getRocks().getRaft().getServer().GetSocket(sessionId);
				if (peer != null) {
					reduce = new Reduce();
					reduce.setResultCode(fresh);
					reduce.Argument.setGlobalKey(gkey);
					reduce.Argument.setState(StateInvalid);
					if (ENABLE_PERF)
						globalRaft.perf.onReduceBegin(reduce);
					reduce.SendForWait(peer, globalRaft.achillesHeelConfig.reduceTimeout);
					return reduce;
				}
				logger.warn("ReduceWaitLater invalid: {} gkey={}", this, gkey);
			} catch (Exception ex) {
				if (ENABLE_PERF && reduce != null)
					globalRaft.perf.onReduceCancel(reduce);
				// 这里的异常只应该是网络发送异常。
				logger.error("ReduceWaitLater Exception: {} gkey={}", this, gkey, ex);
			}
			setError();
			return null;
		}

		@Override
		public String toString() {
			return sessionId + "@" + serverId;
		}
	}
}
