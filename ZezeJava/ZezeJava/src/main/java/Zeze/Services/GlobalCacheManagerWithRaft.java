package Zeze.Services;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;
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
import Zeze.Transaction.DispatchMode;
import Zeze.Util.IdentityHashSet;
import Zeze.Util.KV;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.OutLong;
import Zeze.Util.OutObject;
import Zeze.Util.PerfCounter;
import Zeze.Util.Task;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

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
	/*
	 * 会话。
	 * key是 LogicServer.Id，现在的实现就是Zeze.Config.ServerId。
	 * 在连接建立后收到的Login Or ReLogin 中设置。
	 * 每个会话还需要记录该会话的Socket.SessionId。在连接重新建立时更新。
	 * 总是GetOrAdd，不删除。按现在的cache-sync设计，
	 * ServerId 是及其有限的。不会一直增长。
	 * 简化实现。
	 */
	private final LongConcurrentHashMap<CacheHolder> sessions = new LongConcurrentHashMap<>();

	private final GlobalCacheManagerServer.GCMConfig gcmConfig = new GlobalCacheManagerServer.GCMConfig();
	private final AchillesHeelConfig achillesHeelConfig;
	private final GlobalCacheManagerPerf perf;
	private final AtomicLong serialId = new AtomicLong();

	// 外面主动提供装载配置，需要在Load之前把这个实例注册进去。
	public GlobalCacheManagerServer.GCMConfig getGcmConfig() {
		return gcmConfig;
	}

	public GlobalCacheManagerWithRaft(String raftName) throws Exception {
		this(raftName, null, null, false);
	}

	public GlobalCacheManagerWithRaft(String raftName, RaftConfig raftConf) throws Exception {
		this(raftName, raftConf, null, false);
	}

	public GlobalCacheManagerWithRaft(String raftName, RaftConfig raftConf, Config config) throws Exception {
		this(raftName, raftConf, config, false);
	}

	public GlobalCacheManagerWithRaft(String raftName, RaftConfig raftConf, Config config,
									  boolean RocksDbWriteOptionSync) throws Exception {
		if (config == null)
			config = Config.load();
		config.parseCustomize(this.gcmConfig);
		rocks = new Rocks(raftName, RocksMode.Pessimism, raftConf, config, RocksDbWriteOptionSync);

		RegisterRocksTables(rocks);
		RegisterProtocols(rocks.getRaft().getServer());

		var globalTemplate = rocks.<Binary, BCacheState>getTableTemplate("Global");
		globalTemplate.setLruTryRemoveCallback(this::globalLruTryRemove);
		globalStates = globalTemplate.openTable(0);
		serverAcquiredTemplate = rocks.getTableTemplate("Session");

		if (ENABLE_PERF)
			perf = new GlobalCacheManagerPerf(raftName, serialId); // Rocks.AtomicLong(GlobalSerialIdAtomicLongIndex));
		PerfCounter.instance.tryStartScheduledLog();

		rocks.getRaft().getServer().start();

		// Global的守护不需要独立线程。当出现异常问题不能工作时，没有释放锁是不会造成致命问题的。
		achillesHeelConfig = new AchillesHeelConfig(this.gcmConfig.maxNetPing, this.gcmConfig.serverProcessTime, this.gcmConfig.serverReleaseTimeout);
		Task.schedule(5000, 5000, this::achillesHeelDaemon);
	}

	private void achillesHeelDaemon() {
		var now = System.currentTimeMillis();
		var raft = rocks.getRaft();
		if (raft != null && raft.isLeader()) {
			sessions.forEach(session -> {
				if (now - session.getActiveTime() > achillesHeelConfig.globalDaemonTimeout && !session.debugMode) {
					session.lock();
					try {
						session.kick();
						var Acquired = serverAcquiredTemplate.openTable(session.serverId);
						try {
							var releaseCount = new OutLong();
							Acquired.walkKey(key -> {
								// 在循环中删除。这样虽然效率低些，但是能处理更多情况。
								if (rocks.getRaft().isLeader()) {
//									logger.info("AchillesHeelDaemon.Release table={} key={} session={}",
//											Acquired.getName(), key, session);
									release(session, key);
									++releaseCount.value;
									return true;
								}
								return false;
							});
							session.setActiveTime(System.currentTimeMillis());
							if (releaseCount.value > 0)
								logger.info("AchillesHeelDaemon.Release session={} count={}", session, releaseCount.value);
						} catch (Throwable e) { // print stack trace.
							logger.error("AchillesHeelDaemon.Release {} exception", session, e);
						} finally {
							// server一直没有恢复，这个减少一点Release。
							// 完善的做法是session已经全部release以后，删除掉。
							// 但是删除session并发上复杂点。先这样了。
							session.setActiveTime(System.currentTimeMillis());
						}
					} finally {
						session.unlock();
					}
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

	@Override
	protected long ProcessAcquireRequest(Acquire rpc) throws Exception {
		var acquireState = rpc.Argument.getState();
		if (ENABLE_PERF)
			perf.onAcquireBegin(rpc, acquireState);
		var sender = (CacheHolder)rpc.getSender().getUserState();
		if (sender != null)
			sender.setActiveTime(System.currentTimeMillis());
		rpc.Result.setGlobalKey(rpc.Argument.getGlobalKey());
		rpc.Result.setState(acquireState); // default success

		long result;
		if (sender == null) {
			rpc.Result.setState(StateInvalid);
			// 没有登录重做。登录是Agent自动流程的一部分，应该稍后重试。
			rpc.SendResultCode(Zeze.Transaction.Procedure.RaftRetry);
			result = 0;
		} else {
			var proc = new Procedure(rocks, () -> {
				switch (acquireState) {
				case StateInvalid: // release
					rpc.Result.setState(release(sender, rpc.Argument.getGlobalKey(), true));
					rpc.setResultCode(0);
					return 0;
				case StateShare:
					return acquireShare(rpc);
				case StateModify:
					return acquireModify(rpc);
				default:
					rpc.Result.setState(StateInvalid);
					rpc.setResultCode(0);
					return AcquireErrorState;
				}
			});
			proc.autoResponse = rpc; // 启用自动发送rpc结果，但不做唯一检查。
			result = proc.call();
		}
		if (ENABLE_PERF)
			perf.onAcquireEnd(rpc, acquireState);
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
				globalStates.getLruCache().remove(key);
				return true;
			}
			return false;
		} finally {
			lockey.unlock();
		}
	}

	private long acquireShare(Acquire rpc) throws InterruptedException {
		CacheHolder sender = (CacheHolder)rpc.getSender().getUserState();
		var globalTableKey = rpc.Argument.getGlobalKey();
		var fresh = rpc.getResultCode();
		rpc.setResultCode(0);

		while (true) {
			var lockey = Transaction.getCurrent().addPessimismLock(locks.get(globalTableKey));

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

					if (cs.getModify() == sender.serverId) {
						if (isDebugEnabled)
							logger.debug("1 {} {} {}", sender, StateShare, cs);
						rpc.Result.setState(StateInvalid);
						return AcquireShareDeadLockFound; // 事务数据没有改变，回滚
					}
					break;
				case StateModify:
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
			//Rocks.AtomicLongIncrementAndGet(GlobalSerialIdAtomicLongIndex);
			serialId.getAndIncrement();
			var SenderAcquired = serverAcquiredTemplate.openTable(sender.serverId);
			var reduceTid = new OutLong();
			if (cs.getModify() != -1) {
				if (cs.getModify() == sender.serverId) {
					// 已经是Modify又申请，可能是sender异常关闭，
					// 又重启连上。更新一下。应该是不需要的。
					SenderAcquired.put(globalTableKey, newAcquiredState(StateModify));
					cs.setAcquireStatePending(StateInvalid);
					if (isDebugEnabled)
						logger.debug("4 {} {} {}", sender, StateShare, cs);
					rpc.Result.setState(StateModify);
					rpc.setResultCode(AcquireShareAlreadyIsModify);
					return 0; // 可以忽略的错误，数据有改变，需要提交事务。
				}

				var reduceResultState = new OutObject<>(StateReduceNetError); // 默认网络错误。
				if (CacheHolder.reduce(sessions, cs.getModify(), globalTableKey, fresh, r -> {
					if (ENABLE_PERF)
						perf.onReduceEnd(r);
					if (r.isTimeout()) {
						reduceResultState.value = StateReduceRpcTimeout;
					} else {
						reduceResultState.value = r.Result.getState();
						reduceTid.value = r.Result.getReduceTid();
					}
					lockey.enter();
					try {
						lockey.pulseAll();
					} finally {
						lockey.exit();
					}
					return 0;
				})) {
					if (isDebugEnabled)
						logger.debug("5 {} {} {}", sender, StateShare, cs);
					lockey.await();
				}

				var ModifyAcquired = serverAcquiredTemplate.openTable(cs.getModify());
				switch (reduceResultState.value) {
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
					// logger.error("XXX fresh {} {} {}", sender, acquireState, cs);
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
					// logger.error("XXX 8 state={} {} {} {}", reduceResultState.Value, sender, acquireState, cs);
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
				rpc.Result.setReduceTid(reduceTid.value);
				return 0; // 成功也会自动发送结果.
			}

			SenderAcquired.put(globalTableKey, newAcquiredState(StateShare));
			cs.getShare().add(sender.serverId);
			cs.setAcquireStatePending(StateInvalid);
			if (isDebugEnabled)
				logger.debug("7 {} {} {}", sender, StateShare, cs);
			lockey.pulseAll();
			rpc.Result.setReduceTid(reduceTid.value);
			return 0; // 成功也会自动发送结果.
		}
	}

	private long acquireModify(Acquire rpc) throws InterruptedException {
		CacheHolder sender = (CacheHolder)rpc.getSender().getUserState();
		var globalTableKey = rpc.Argument.getGlobalKey();
		var fresh = rpc.getResultCode();
		rpc.setResultCode(0);

		while (true) {
			var lockey = Transaction.getCurrent().addPessimismLock(locks.get(globalTableKey));

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
					if (cs.getModify() == sender.serverId) {
						if (isDebugEnabled)
							logger.debug("1 {} {} {}", sender, StateModify, cs);
						rpc.Result.setState(StateInvalid);
						return AcquireModifyDeadLockFound; // 事务数据没有改变，回滚
					}
					break;
				case StateModify:
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
			//Rocks.AtomicLongIncrementAndGet(GlobalSerialIdAtomicLongIndex);
			serialId.getAndIncrement();
			var SenderAcquired = serverAcquiredTemplate.openTable(sender.serverId);
			var reduceTid = new OutLong();
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
				if (CacheHolder.reduce(sessions, cs.getModify(), globalTableKey, fresh, r -> {
					if (ENABLE_PERF)
						perf.onReduceEnd(r);
					if (r.isTimeout()) {
						reduceResultState.value = StateReduceRpcTimeout;
					} else {
						reduceResultState.value = r.Result.getState();
						reduceTid.value = r.Result.getReduceTid();
					}
					lockey.enter();
					try {
						lockey.pulseAll();
					} finally {
						lockey.exit();
					}
					return 0;
				})) {
					if (isDebugEnabled)
						logger.debug("5 {} {} {}", sender, StateModify, cs);
					lockey.await();
				}

				var ModifyAcquired = serverAcquiredTemplate.openTable(cs.getModify());
				switch (reduceResultState.value) {
				case StateInvalid:
					ModifyAcquired.remove(globalTableKey);
					break; // reduce success

				case StateReduceErrorFreshAcquire:
					cs.setAcquireStatePending(StateInvalid);
					if (ENABLE_PERF)
						perf.onOthers("XXX Fresh " + StateModify);
					// logger.error("XXX fresh {} {} {} {}", sender, acquireState, cs);
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
					// logger.error("XXX 9 {} {} {} {}", sender, acquireState, cs, reduceResultState.Value);
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
				rpc.Result.setReduceTid(reduceTid.value);
				return 0;
			}

			ArrayList<KV<CacheHolder, Reduce>> reducePending = new ArrayList<>();
			IdentityHashSet<CacheHolder> reduceSucceed = new IdentityHashSet<>();
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
			// 2. sender是share, 而且reducePending是空的
			var errorFreshAcquire = new OutObject<>(Boolean.FALSE);
			if (cs.getShare().size() != 0 && (!senderIsShare || !reducePending.isEmpty())) {
				Task.executeUnsafe(() -> {
					// 一个个等待是否成功。WaitAll 碰到错误不知道怎么处理的，
					// 应该也会等待所有任务结束（包括错误）。
					var freshAcquire = false;
					for (var kv : reducePending) {
						CacheHolder session = kv.getKey();
						Reduce reduce = kv.getValue();
						try {
							//noinspection DataFlowIssue
							reduce.getFuture().await();
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
						lockey.pulseAll();
					} finally {
						lockey.exit();
					}
				}, "GlobalCacheManagerWithRaft.AcquireModify.WaitReduce", DispatchMode.Normal);
				if (isDebugEnabled)
					logger.debug("7 {} {} {}", sender, StateModify, cs);
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
				// 失败了，要把原来是share的sender恢复。先这样吧。
				if (senderIsShare)
					cs.getShare().add(sender.serverId);

				cs.setAcquireStatePending(StateInvalid);
				if (ENABLE_PERF)
					perf.onOthers("XXX 10 " + StateModify + ' ' + errorFreshAcquire.value);
				// logger.error("XXX 10 {} {} {}", sender, acquireState, cs);
				rpc.Result.setState(StateInvalid);
				lockey.pulseAll();
				rpc.setResultCode(errorFreshAcquire.value
						? StateReduceErrorFreshAcquire  // 这个错误码导致Server-RedoAndReleaseLock
						: AcquireModifyFailed); // 这个错误码导致Server事务失败。
				rpc.Result.setReduceTid(reduceTid.value);
				return 0; // 可能存在部分reduce成功，需要提交事务。
			}

			SenderAcquired.put(globalTableKey, newAcquiredState(StateModify));
			cs.setModify(sender.serverId);
			cs.setAcquireStatePending(StateInvalid);
			if (isDebugEnabled)
				logger.debug("8 {} {} {}", sender, StateModify, cs);
			lockey.pulseAll();
			rpc.Result.setReduceTid(reduceTid.value);
			return 0; // 成功也会自动发送结果.
		}
	}

	private void release(CacheHolder sender, Binary gkey) throws Exception {
		rocks.newProcedure(() -> {
			release(sender, gkey, false);
			return 0L;
		}).call();
	}

	private int release(CacheHolder sender, Binary gkey, boolean noWait) throws InterruptedException {
		while (true) {
			var lockey = Transaction.getCurrent().addPessimismLock(locks.get(gkey));

			BCacheState cs = globalStates.getOrAdd(gkey);
			if (cs.getAcquireStatePending() == StateRemoved)
				continue; // 这个是不可能的，因为有Release请求进来意味着肯定有拥有者(share or modify)，此时不可能进入StateRemoved。

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
			cs.setAcquireStatePending(StateRemoving);

			if (cs.getModify() == sender.serverId)
				cs.setModify(-1);
			cs.getShare().remove(sender.serverId); // always try remove
			serverAcquiredTemplate.openTable(sender.serverId).remove(gkey);

			if (cs.getModify() == -1 && cs.getShare().size() == 0) {
				// 1. 安全的从global中删除，没有并发问题。
				cs.setAcquireStatePending(StateRemoved);
				globalStates.remove(gkey);
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
		if (!session.tryBindSocket(rpc.getSender(), rpc.Argument.getGlobalCacheManagerHashIndex())) {
			rpc.SendResultCode(LoginBindSocketFail);
			return 0;
		}
		session.setActiveTime(System.currentTimeMillis());
		session.setDebugMode(rpc.Argument.isDebugMode());
		// new login, 比如逻辑服务器重启。release old acquired.
		var SenderAcquired = serverAcquiredTemplate.openTable(session.serverId);
		SenderAcquired.walkKey(key -> {
			release(session, key);
			return true; // continue walk
		});

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
		if (!session.tryBindSocket(rpc.getSender(), rpc.Argument.getGlobalCacheManagerHashIndex())) {
			rpc.SendResultCode(ReLoginBindSocketFail);
			return 0;
		}
		session.setActiveTime(System.currentTimeMillis());
		session.setDebugMode(rpc.Argument.isDebugMode());
		rpc.SendResultCode(0);
		logger.info("ReLogin {} {}.", rocks.getRaft().getName(), rpc.getSender());
		return 0;
	}

	@Override
	protected long ProcessNormalCloseRequest(NormalClose rpc) throws Exception {
		Object userState = rpc.getSender().getUserState();
		if (!(userState instanceof CacheHolder)) {
			rpc.SendResultCode(AcquireNotLogin);
			return 0; // not login
		}
		CacheHolder session = (CacheHolder)userState;
		if (!session.tryUnBindSocket(rpc.getSender())) {
			rpc.SendResultCode(NormalCloseUnbindFail);
			return 0;
		}
		var SenderAcquired = serverAcquiredTemplate.openTable(session.serverId);
		SenderAcquired.walkKey(key -> {
			release(session, key);
			return true; // continue walk
		});
		rpc.SendResultCode(0);
		logger.info("NormalClose {} {}", rocks.getRaft().getName(), rpc.getSender());
		return 0;
	}

	@Override
	protected long ProcessCleanupRequest(Cleanup rpc) {
		if (achillesHeelConfig != null) // disable cleanup.
			return 0;

		// 安全性以后加强。
		if (!rpc.Argument.getSecureKey().equals("Ok! verify secure.")) {
			rpc.SendResultCode(CleanupErrorSecureKey);
			return 0;
		}

		var session = sessions.computeIfAbsent(rpc.Argument.getServerId(), serverId -> new CacheHolder(this, (int)serverId));
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

		// 还有更多的防止出错的手段吗？

		// XXX verify danger
		Task.schedule(5 * 60 * 1000, () -> { // delay 5 mins
			var SenderAcquired = serverAcquiredTemplate.openTable(session.serverId);
			SenderAcquired.walkKey(key -> {
				release(session, key);
				return true; // continue release;
			});
			rpc.SendResultCode(0);
		});

		return 0;
	}

	@Override
	protected long ProcessKeepAliveRequest(KeepAlive rpc) {
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
			rocks.close();
		} catch (Exception e) {
			Task.forceThrow(e);
		}
	}

	private static final class CacheHolder extends ReentrantLock {
		final GlobalCacheManagerWithRaft globalRaft;
		final int serverId;
		private long sessionId;
		private int globalCacheManagerHashIndex;
		private volatile long activeTime = System.currentTimeMillis();
		private volatile long lastErrorTime;
		private boolean debugMode;

		// not under lock
		void kick() {
			var raft = globalRaft.getRocks().getRaft();
			if (raft != null) {
				var peer = raft.getServer().GetSocket(sessionId);
				if (peer != null) {
					peer.setUserState(null); // 来自这个Agent的所有请求都会失败。
					peer.close(kickException); // 关闭连接，强制Agent重新登录。
				}
			}
			sessionId = 0; // 清除网络状态。
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
			lock();
			try {
				if (newSocket.getUserState() != null && newSocket.getUserState() != this)
					return false; // 允许重复login|relogin，但不允许切换ServerId。

				var socket = globalRaft.getRocks().getRaft().getServer().GetSocket(sessionId);
				if (socket == null || socket == newSocket) {
					// old socket not exist or has lost.
					sessionId = newSocket.getSessionId();
					newSocket.setUserState(this);
					this.globalCacheManagerHashIndex = globalCacheManagerHashIndex;
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

				var socket = globalRaft.getRocks().getRaft().getServer().GetSocket(sessionId);
				if (socket != null && socket != oldSocket)
					return false; // not same socket

				sessionId = 0;
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
				logger.error("Reduce invalid serverId={}", serverId);
				return false;
			}
			return session.reduce(gkey, fresh, response);
		}

		static KV<CacheHolder, Reduce> reduceWaitLater(LongConcurrentHashMap<CacheHolder> sessions, int serverId,
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
		Reduce reduceWaitLater(Binary gkey, long fresh) {
			Reduce reduce = null;
			try {
				if (System.currentTimeMillis() - lastErrorTime < globalRaft.achillesHeelConfig.globalForbidPeriod)
					return null;
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
