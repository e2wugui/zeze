package Zeze.Transaction;

import java.io.Serial;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.LongFunction;
import java.util.function.Supplier;

import Zeze.Application;
import Zeze.Onz.Onz;
import Zeze.Onz.OnzProcedure;
import Zeze.Services.GlobalCacheManagerConst;
import Zeze.Transaction.Collections.Collection;
import Zeze.Util.Random;
import Zeze.Util.ZezeCounter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

/**
 * Zeze 事务核心：管理保存点、修改日志与记录访问，配合记录锁驱动 redo 重试，直至最终提交或回滚。
 */
public final class Transaction {
	private static final Logger logger = LogManager.getLogger(Transaction.class);

	// Abort 日志按秒限频并累计次数，避免环境异常时刷屏。
	private static final LongAdder abortWarnCount = new LongAdder();
	private static volatile long lastAbortWarnTime;
	// redo 日志同样限频，避免高冲突负载下日志影响吞吐。
	private static final LongAdder redoInfoCount = new LongAdder();
	private static volatile long lastRedoInfoTime;
	private static final ThreadLocal<@Nullable Transaction> threadLocal = new ThreadLocal<>();
	private static final Object NULL_VALUE = new Object(); // resolveOnce 已解析为 null 的哨兵

	public static Transaction create(Locks locks) {
		var t = threadLocal.get();
		if (t == null)
			threadLocal.set(t = new Transaction());
		t.locks = locks;
		t.created = true;
		return t;
	}

	public static void destroy() {
		var t = threadLocal.get();
		if (t != null)
			t.reuseTransaction();
	}

	public static @Nullable Transaction getCurrent() {
		var t = threadLocal.get();
		return t != null && t.created ? t : null;
	}

	public static @Nullable Transaction getCurrentVerifyRead(Bean bean) {
		// 读操作不做验证。
		return getCurrent();
	}

	public static Transaction getCurrentVerifyWrite(Bean bean) {
		var t = getCurrent();
		if (t == null)
			throw new IllegalStateException("not in transaction");
		t.verifyRecordForWrite(bean);
		return t;
	}

	private final ArrayList<Lockey> holdLocks = new ArrayList<>();
	private final ArrayList<Procedure> procedureStack = new ArrayList<>();
	private @Nullable ArrayList<Runnable> logActions;
	private final ArrayList<Savepoint> savepoints = new ArrayList<>();
	private final ArrayList<Savepoint.Action> actions = new ArrayList<>();
	// 暂存最近重试轮的回滚回调，供终局失败时执行。
	private @Nullable ArrayList<Savepoint.Action> redoRollbackActions;
	private @Nullable Throwable lastRoundException;
	private final TreeMap<TableKey, RecordAccessed> accessedRecords = new TreeMap<>();
	private @Nullable Locks locks;
	private TransactionState state = TransactionState.Running;
	private boolean created;
	private boolean alwaysReleaseLockWhenRedo;
	private final ArrayList<Bean> redoBeans = new ArrayList<>();
	private @Nullable HashMap<ResolveKey, Object> onceResolved;
	private @Nullable OnzProcedure onzProcedure;
	final Profiler profiler = new Profiler();
	private final AtomicLong totalTransaction = new AtomicLong();

	private Transaction() {
	}

	public ArrayList<Procedure> getProcedureStack() {
		return procedureStack;
	}

	public void addLogAction(Runnable action) {
		if (null == logActions)
			logActions = new ArrayList<>();
		logActions.add(action);
	}

	TreeMap<TableKey, RecordAccessed> getAccessedRecords() {
		return accessedRecords;
	}

	public boolean isRunning() {
		return state == TransactionState.Running;
	}

	public boolean isCompleted() {
		return state == TransactionState.Completed;
	}

	public @Nullable Procedure getTopProcedure() {
		var stackSize = procedureStack.size();
		return stackSize > 0 ? procedureStack.get(stackSize - 1) : null;
	}

	/**
	 * 最近一次 perform 中最后一个异常重试轮的原始异常，仅供诊断。
	 * 后续无异常的返回码重试不清除此值；下一次 perform 开始时清零。
	 */
	public @Nullable Throwable getLastRoundException() {
		return lastRoundException;
	}

	void reuseTransaction() {
		// 手工 create/begin 后直接 destroy 时也需释放锁。
		if (!holdLocks.isEmpty()) {
			logger.error("reuseTransaction with {} held lock(s), releasing defensively", holdLocks.size());
			holdLocks.forEach(Lockey::exitLock);
			holdLocks.clear();
		}
		procedureStack.clear();
		if (null != logActions)
			logActions.clear();
		savepoints.clear();
		actions.clear();
		if (redoRollbackActions != null)
			redoRollbackActions.clear();
		accessedRecords.clear();
		locks = null;
		state = TransactionState.Running;
		created = false;
		alwaysReleaseLockWhenRedo = false;
		redoBeans.clear();
		if (null != onceResolved)
			onceResolved.clear();
		onzProcedure = null;
		profiler.reset();
	}

	void reuseTransactionForRedo(CheckResult checkResult) {
		if (checkResult == CheckResult.RedoAndReleaseLock) {
			holdLocks.forEach(Lockey::exitLock);
			holdLocks.clear();
		}
		if (null != logActions)
			logActions.clear();
		// 重试会重新注册回调，先保留本轮回调以便重试耗尽时回滚。
		if (redoRollbackActions == null)
			redoRollbackActions = new ArrayList<>();
		else
			redoRollbackActions.clear();
		redoRollbackActions.addAll(actions); // rollback() 已汇入事务级的本轮回调
		for (var sp : savepoints)
			sp.mergeRollbackActions(redoRollbackActions); // Begin/End 不配对时残留层中的回调
		savepoints.clear();
		actions.clear();
		accessedRecords.clear();
		state = TransactionState.Running;
		redoBeans.clear();
		// onceResolved 跨 redo 复用，仅在 perform 入口或事务对象回收时清除。
	}

	public void begin() {
		var saveSize = savepoints.size();
		savepoints.add(saveSize > 0 ? savepoints.get(saveSize - 1).beginSavepoint() : new Savepoint());
	}

	public void commit() {
		int saveSize = savepoints.size();
		if (saveSize < 1)
			throw new IllegalStateException("commit: savepoints is empty. begin/commit not paired.");
		if (saveSize > 1)
			savepoints.get(saveSize - 2).mergeCommitFrom(savepoints.removeLast());
		// 最外层提交由 perform 处理。
	}

	public void rollback() {
		int lastIndex = savepoints.size() - 1;
		if (lastIndex < 0)
			throw new IllegalStateException("rollback: savepoints is empty. begin/rollback not paired.");
		Savepoint last = savepoints.remove(lastIndex);
		if (lastIndex > 0)
			savepoints.get(lastIndex - 1).mergeRollbackFrom(last);
		else
			last.mergeRollbackActions(actions);
	}

	public TransactionLog logGetOrAdd(long logKey, Supplier<? extends TransactionLog> logFactory) {
		var log = getLog(logKey);
		if (log == null)
			putLog(log = logFactory.get());
		return log;
	}

	public TransactionLog logGetOrAdd(Collection source) {
		long key = source.changeLogKey();
		var existing = getLog(key);
		if (existing != null)
			return existing;
		var root = source.rootInfo;
		assert root != null;
		var sourceRecord = root.record();
		assert sourceRecord != null;
		var table = sourceRecord.getTable();
		// Onz coordinates commit/flush of whole records; it does not consume collection deltas.
		boolean capture = table.getZeze().getHistory() != null
			|| table.getChangeListenerMap().hasListener();
		var created = source.createLog(capture ? ChangeCaptureMode.DELTA : ChangeCaptureMode.NONE);
		putLog(created);
		return created;
	}

	public @Nullable TransactionLog getLog(long key) {
		verifyRunningOrCompleted();
		// 无保存点时仍允许读取。
		var saveSize = savepoints.size();
		return saveSize > 0 ? savepoints.get(saveSize - 1).getLog(key) : null;
	}

	public void putLog(TransactionLog log) {
		verifyRunning();
		savepoints.getLast().putLog(log);
	}

	private void triggerRedoActions() {
		profiler.onRedo();
		redoBeans.forEach(Bean::resetRootInfo);
	}

	/**
	 * 一次 perform（含全部 redo）中，同一 (owner, key) 只解析一次，结果包括 null 都会缓存。
	 * 用于消费 Rpc 上下文等一次性外部资源，避免重试时重复消费。
	 * owner 按对象身份比较，重试须传同一实例；key 须在该 owner 内唯一标识资源。
	 */
	public <T> @Nullable T resolveOnce(Object owner, long key, LongFunction<@Nullable T> resolver) {
		var cache = onceResolved;
		if (cache == null)
			onceResolved = cache = new HashMap<>();
		var cacheKey = new ResolveKey(owner, key);
		var cached = cache.get(cacheKey);
		if (cached != null) {
			//noinspection unchecked
			return cached != NULL_VALUE ? (T)cached : null;
		}
		var value = resolver.apply(key);
		cache.put(cacheKey, value != null ? value : NULL_VALUE);
		return value;
	}

	public static <T> @Nullable T resolveOnceOrApply(Object owner, long key, LongFunction<@Nullable T> resolver) {
		var txn = getCurrent();
		if (null != txn)
			return txn.resolveOnce(owner, key, resolver);
		return resolver.apply(key);
	}

	private record ResolveKey(Object owner, long key) {
		@Override
		public boolean equals(@Nullable Object other) {
			return other instanceof ResolveKey(Object owner1, long key1) && owner == owner1 && key == key1;
		}

		@Override
		public int hashCode() {
			return 31 * System.identityHashCode(owner) + Long.hashCode(key);
		}
	}

	static void whileRedo(Bean b) {
		// 仅 redo 时重置 RootInfo，最终回滚仍保留 Bean 所有权；复用需重建实例或 copy()。
		var current = getCurrent();
		if (current != null)
			current.redoBeans.add(b);
	}

	public static void whileCommit(Runnable action) {
		var current = getCurrent();
		if (current == null)
			throw new IllegalStateException("no current transaction for whileCommit");
		current.runWhileCommit(action);
	}

	public static void whileRollback(Runnable action) {
		var current = getCurrent();
		if (current == null)
			throw new IllegalStateException("no current transaction for whileRollback");
		current.runWhileRollback(action);
	}

	public void runWhileCommit(Runnable action) {
		if (state == TransactionState.Completed) {
			actions.add(new Savepoint.Action(Savepoint.ActionType.COMMIT, action));
			return;
		}
		verifyRunning();
		savepoints.getLast().addCommitAction(action);
	}

	public void runWhileRollback(Runnable action) {
		if (state == TransactionState.Completed) {
			actions.add(new Savepoint.Action(Savepoint.ActionType.ROLLBACK, action));
			return;
		}
		verifyRunning();
		savepoints.getLast().addRollbackAction(action);
	}

	void setAlwaysReleaseLockWhenRedo(int tableId) {
		alwaysReleaseLockWhenRedo = true;
		if (!holdLocks.isEmpty())
			throwRedo(tableId, "Redo: AlwaysReleaseLock");
	}

	/**
	 * 执行最外层 Procedure，处理重试、提交与回滚。
	 */
	public long perform(Procedure procedure) {
		if (null != onceResolved)
			onceResolved.clear();
		if (redoRollbackActions != null)
			redoRollbackActions.clear();
		lastRoundException = null;
		try {
			var checkpoint = procedure.getZeze().getCheckpoint();
			if (checkpoint == null || procedure.getZeze().isFatalStopping())
				return Procedure.Closed;
			for (int tryCount = 0; tryCount < 256; ++tryCount) {
				// Redo 保留锁重试；需要释放锁的冲突必须返回 RedoAndReleaseLock。
				for (; tryCount < 256; ++tryCount) {
					// 每轮重试都检查停机，避免继续执行业务后才被提交闸门拒绝。
					if (procedure.getZeze().getCheckpoint() == null || procedure.getZeze().isFatalStopping()) {
						if (redoRollbackActions != null)
							actions.addAll(redoRollbackActions);
						finalRollback(procedure);
						return Procedure.Closed;
					}
					CheckResult checkResult = CheckResult.Redo;
					try {
						var result = procedure.call();
						switch (state) {
						case Running:
							var saveSize = savepoints.size();
							if ((result == Procedure.Success && saveSize != 1)
								|| (result != Procedure.Success && saveSize > 0)) {
								logger.error("perform({}): savepoints.size != 1", procedure);
								finalRollback(procedure);
								return Procedure.ErrorSavepoint;
							}
							// 单独处理锁检查异常，避免被外层 catch 误判为保存点配对错误。
							CheckResult lockResult;
							try {
								lockResult = lockAndCheck(procedure);
							} catch (Throwable lcEx) {
								if (state != TransactionState.Running)
									throw lcEx; // 由外层按 Abort/Redo 状态处理。
								logger.error("perform({}) lockAndCheck exception. run count:{}", procedure, tryCount, lcEx);
								finalRollback(procedure);
								return Procedure.Exception;
							}
							checkResult = lockResult;
							if (checkResult == CheckResult.Success) {
								if (result == Procedure.Success) {
									OnzProcedure flushMode = null;
									if (onzProcedure != null) {
										onzProcedure.sendReadyAndWait();
										if (onzProcedure.getFlushMode() != Onz.eFlushAsync)
											flushMode = onzProcedure;
									}
									var history = procedure.getZeze().getHistory();
									var changes = Changes.createIfNeeded(this, history != null);
									try {
										finalCommit(procedure, flushMode, changes, history);
									} catch (RejectWhileStopping | IHistory.CommitRejectedException e) {
										if (e instanceof IHistory.CommitRejectedException)
											logger.error("history prepare rejected: {}", procedure.getActionName(), e);
										// 提交被拒绝时回滚并返回 Closed。若 Onz 参与方已收到 Commit 决策，
										// 需记录回滚分歧并留哨兵，供迟到的 redo Commit 确认。
										if (onzProcedure != null && !(onzProcedure instanceof Zeze.Onz.OnzSaga)) {
											logger.error("onz participant({}) tid={}: coordinator commit decision"
													+ " delivered and persisted, but local transaction rolled back"
													+ " while stopping -- cross-cluster divergence, manual check required",
												procedure.getActionName(), onzProcedure.getOnzTid(), e);
											onzProcedure.markRolledBackAfterReady();
										}
										finalRollback(procedure);
										return Procedure.Closed;
									} catch (AssertionError ae) {
										// 提交已完成；回调断言失败直接向上传播。
										throw ae;
									} catch (Throwable ex) {
										logger.fatal("finalCommit exception:", ex);
										// 先释放记录锁，避免最终保存被本事务阻塞。
										holdLocks.forEach(Lockey::exitLock);
										holdLocks.clear();

										Application.haltAfterCheckpoint(procedure.getZeze(), 543543);
										return 0;
									}
									return Procedure.Success;
								}
								finalRollback(procedure);
								return result;
							}
							break;

						case Abort:
							logger.warn("perform({}): Abort", procedure);
							finalRollback(procedure);
							return Procedure.AbortException;

						case Redo:
							break;

						case RedoAndReleaseLock:
							checkResult = CheckResult.RedoAndReleaseLock;
							break;
						}
						redoInfoCount.increment();
						var redoNowMs = System.currentTimeMillis();
						if (redoNowMs - lastRedoInfoTime >= 1000) {
							lastRedoInfoTime = redoNowMs;
							logger.info("perform({}): {}, count={} (rate-limited 1/s)",
								procedure, checkResult, redoInfoCount.sumThenReset());
						}
						triggerRedoActions();
					} catch (Throwable e) {
						// 业务异常已由 Procedure.call 处理；此处接收重做、断言和内部错误。
						lastRoundException = e;
						switch (state) {
						case Running:
							logger.error("perform({}) exception. run count:{}", procedure, tryCount, e);
							if (!savepoints.isEmpty()) {
								logger.error("perform({}) exception. !savepoints.isEmpty", procedure, e);
								finalRollback(procedure);
								return Procedure.ErrorSavepoint;
							}
							if (e instanceof AssertionError) {
								finalRollback(procedure);
								throw (AssertionError)e;
							}
							// 锁检查失败按内部错误退出，保留业务异常的诊断。
							try {
								checkResult = lockAndCheck(procedure);
							} catch (Throwable lcEx) {
								logger.error("perform({}) lockAndCheck exception (in exception path). run count:{}",
									procedure, tryCount, lcEx);
								finalRollback(procedure);
								return Procedure.Exception;
							}
							if (checkResult == CheckResult.Success) {
								finalRollback(procedure);
								return Procedure.Exception;
							}
							break;

						case Abort:
							abortWarnCount.increment();
							var nowMs = System.currentTimeMillis();
							if (nowMs - lastAbortWarnTime >= 1000) {
								lastAbortWarnTime = nowMs;
								logger.warn("perform({}): Abort, count={} (rate-limited 1/s)",
									procedure, abortWarnCount.sumThenReset(), e);
							}
							finalRollback(procedure);
							return Procedure.AbortException;

						case Redo:
							logger.trace("perform({}): Redo", procedure, e);
							checkResult = CheckResult.Redo;
							break;

						case RedoAndReleaseLock:
							logger.trace("perform({}): RedoAndReleaseLock", procedure, e);
							checkResult = CheckResult.RedoAndReleaseLock;
							break;

						default: // case Completed:
							if (e instanceof AssertionError)
								throw (AssertionError)e;
							logger.error("perform({}) {} exception. run count:{}", procedure, state, tryCount, e);
						}
						triggerRedoActions();
					}

					// 正常与异常重试共用此处升级；终局路径由外层 finally 与 Procedure 清理。
					if (alwaysReleaseLockWhenRedo && checkResult == CheckResult.Redo)
						checkResult = CheckResult.RedoAndReleaseLock;
					reuseTransactionForRedo(checkResult);

					if (checkResult == CheckResult.RedoAndReleaseLock) {
						procedure.procedureCounter().redoAndReleaseLock();
						break;
					}
					procedure.procedureCounter().redo();
				}
				try {
					Thread.sleep(Random.getInstance().nextInt(80) + 20);
				} catch (InterruptedException e) {
					// 中断取消重试，回滚最近一轮回调并恢复中断标志。
					logger.error("perform({}): interrupted, cancel retry", procedure);
					Thread.currentThread().interrupt();
					if (redoRollbackActions != null)
						actions.addAll(redoRollbackActions);
					finalRollback(procedure);
					return Procedure.Exception;
				}
			}
			if (lastRoundException != null)
				logger.error("perform({}): too many try, last exception:", procedure, lastRoundException);
			else
				logger.error("perform({}): too many try", procedure);
			// 最后一轮回调已被 reuseTransactionForRedo 清空，用暂存的最近一轮回调终局回滚。
			if (redoRollbackActions != null)
				actions.addAll(redoRollbackActions);
			finalRollback(procedure);
			return Procedure.TooManyTry;
		} finally {
			state = TransactionState.Completed;
			holdLocks.forEach(Lockey::exitLock);
			holdLocks.clear();
			var totalCount = totalTransaction.incrementAndGet();
			var config = procedure.getZeze().getConfig().getCheckpointTransactionPeriod();
			if (config > 0 && (totalCount % config == 0)) {
				try {
					procedure.getZeze().checkpointRunThread();
				} catch (Throwable e) {
					// 周期 checkpoint 触发失败不能覆盖已提交事务的返回值。
					logger.error("perform({}): checkpointRunThread fail", procedure, e);
				}
			}
		}
	}

	private void triggerActions(Procedure procedure) {
		//noinspection ForLoopReplaceableByForEach
		for (var i = 0; i < actions.size(); ++i) { // size可能会在循环中增加
			var action = actions.get(i);
			try {
				action.action.run();
			} catch (AssertionError e) {
				throw e;
			} catch (Throwable e) {
				String typeStr = switch (action.actionType) {
					case COMMIT -> "commit";
					case NESTED_ROLLBACK -> "nestedRollback";
					default -> "rollback";
				};
				logger.error("{} Procedure={} Action={} exception:",
					typeStr, procedure.getActionName(), action.action.getClass().getName(), e);
			}
		}
	}

	private void triggerCommitActions(Procedure procedure) {
		triggerActions(procedure);
	}

	private void triggerRollbackActions(Procedure procedure) {
		triggerActions(procedure);
	}

	/** Onz binding is independent of History/listener delta capture. */
	public void setOnzProcedure(@Nullable OnzProcedure onzProcedure) {
		this.onzProcedure = onzProcedure;
	}

	/** History 与监听器共用的变更采集，必须在 PutLog.commit 填充 committedPutLog 后执行。 */
	void collectChanges(Changes changes) {
		var lastSp = savepoints.getLast();
		var it = lastSp.logIterator();
		if (it != null) {
			while (it.moveToNext())
				changes.collect(it.value());
		}
		for (var ar : accessedRecords.values()) {
			if (ar.dirty)
				changes.collectRecord(ar);
		}
		changes.finishCollect();
	}

	private void finalCommit(Procedure proc, @Nullable OnzProcedure flushMode,
							 @Nullable Changes changes, @Nullable IHistory history) throws Exception {
		// 最终提交的数据应用阶段不可回滚，失败须终止进程。
		proc.getZeze().getProcedureLockWatcher().doWatch(proc, accessedRecords);
		var lastSp = savepoints.getLast();
		RelativeRecordSet.tryUpdateAndCheckpoint(this, proc, () -> {
			try {
				lastSp.mergeCommitActions(actions);
				lastSp.commit();
				for (var v : accessedRecords.values()) {
					v.atomicTupleRecord.record().setNotFresh();
					if (v.dirty) {
						v.atomicTupleRecord.record().commit(v);
						var newValue = v.atomicTupleRecord.record().getSoftValue();
						if (newValue != null) {
							// 删除后再次插入时，version 从 0 开始。
							var oldValue = v.atomicTupleRecord.strongRef();
							var oldVersion = oldValue != null ? oldValue.version() : 0;
							newValue.version(oldVersion + 1);
						}
					}
				}
			} catch (Throwable e) {
				logger.fatal("finalCommit({}) exception:", proc, e);
				LogManager.shutdown();
				Runtime.getRuntime().halt(54321);
			}
		}, flushMode, changes, history);

		// 监听器仅可读取回调记录，不能访问其他表或再次启动事务。
		state = TransactionState.Completed;

		try {
			if (null != logActions) {
				// 日志动作失败不能阻止监听通知和提交回调。
				for (var act : logActions) {
					try {
						act.run();
					} catch (Throwable e) {
						logger.error("finalCommit({}) logAction exception:", proc, e);
					}
				}
			}
			if (changes != null)
				changes.notifyListener();
			triggerCommitActions(proc);
		} catch (AssertionError ex) {
			throw ex;
		} catch (Throwable ex) {
			logger.error("finalCommit({}) exception:", proc, ex);
		}
	}

	private void finalRollback(Procedure procedure) {
		for (var ra : accessedRecords.values())
			ra.atomicTupleRecord.record().setNotFresh();
		// 保存点未配对时，也需执行残留层中的回滚回调。
		for (var sp : savepoints)
			sp.mergeRollbackActions(actions);
		savepoints.clear(); // 回滚回调读取时应看到原值。
		state = TransactionState.Completed;
		try {
			if (null != logActions) {
				// 日志动作失败不能阻止后续日志和回滚回调。
				for (var act : logActions) {
					try {
						act.run();
					} catch (Throwable e) {
						logger.error("finalRollback({}) logAction exception:", procedure, e);
					}
				}
			}
			triggerRollbackActions(procedure);
		} catch (AssertionError ex) {
			throw ex;
		} catch (Throwable ex) {
			logger.error("finalRollback({}) exception:", procedure, ex);
		}
	}

	/**
	 * 只能添加一次。
	 */
	void addRecordAccessed(Record.RootInfo root, RecordAccessed ra,
						   @SuppressWarnings("unused") boolean removeWhileRollback) {
		verifyRunning();
		ra.initRootInfo(root, null);
		accessedRecords.put(root.tableKey(), ra);
	}

	public @Nullable RecordAccessed getRecordAccessed(TableKey key) {
		verifyRunningOrCompleted();
		return accessedRecords.get(key);
	}

	public void verifyRecordForWrite(Bean bean) {
		var ri = bean.rootInfo;
		//noinspection DataFlowIssue
		if (ri.record().getState() == GlobalCacheManagerConst.StateRemoved)
			throwRedo(ri.tableKey().getId(), "Redo: StateRemoved: " + bean.tableKey());
		//noinspection DataFlowIssue
		var ra = getRecordAccessed(bean.tableKey());
		if (ra == null)
			throw new IllegalStateException("VerifyRecordAccessed: Record Not Control Under Current Transaction: " + bean.tableKey());
		var atr = ra.atomicTupleRecord;
		var r = atr.record();
		if (ri.record() != r)
			throw new IllegalStateException("VerifyRecordAccessed: Record Reloaded: " + bean.tableKey());
		// 提交后监听器只读，时间戳已更新，无需重做检查。
		var t = r.getTable();
		if (t.getZeze().getConfig().getFastRedoWhenConflict()
			&& state != TransactionState.Completed
			&& r.getTimestamp() != atr.timestamp())
			throwRedo(ri.tableKey().getId(), "Redo: FastRedoWhenConflict(" + t.getName() + ')');
	}

	private enum CheckResult {
		Success,
		Redo,
		RedoAndReleaseLock
	}

	private static CheckResult _check_(boolean writeLock,
									   RecordAccessed e) {
		e.atomicTupleRecord.record().enterFairLock();
		try {
			if (writeLock) {
				switch (e.atomicTupleRecord.record().getState()) {
				case GlobalCacheManagerConst.StateRemoved:
					// 被从cache中清除，不持有该记录的Global锁，简单重做即可。
					return CheckResult.Redo;

				case GlobalCacheManagerConst.StateInvalid:
					return CheckResult.RedoAndReleaseLock; // 写锁发现Invalid，可能有Reduce请求。

				case GlobalCacheManagerConst.StateModify:
					return e.atomicTupleRecord.timestamp() != e.atomicTupleRecord.record().getTimestamp()
						? CheckResult.Redo : CheckResult.Success;

				case GlobalCacheManagerConst.StateShare:
					// 并发提升可能等待本机 Reduce；GCM 检出死锁后需释放本地锁再重试。
					var acquire = e.atomicTupleRecord.record().acquire(GlobalCacheManagerConst.StateModify,
						e.atomicTupleRecord.record().isFresh(), false);
					//noinspection DataFlowIssue
					if (!acquire.isSuccess() || acquire.resultState() != GlobalCacheManagerConst.StateModify) {
						e.atomicTupleRecord.record().setNotFresh(); // 抢失败不再新鲜。
						logger.debug("Acquire Failed. Maybe DeadLock Found: record={}, time={}, resultCode={}",
							e.atomicTupleRecord.record(), e.atomicTupleRecord.timestamp(), acquire.resultCode());
						e.atomicTupleRecord.record().setState(GlobalCacheManagerConst.StateInvalid);
						return CheckResult.RedoAndReleaseLock;
					}
					e.atomicTupleRecord.record().setState(GlobalCacheManagerConst.StateModify);
					return e.atomicTupleRecord.timestamp() != e.atomicTupleRecord.record().getTimestamp()
						? CheckResult.Redo : CheckResult.Success;
				}
				return e.atomicTupleRecord.timestamp() != e.atomicTupleRecord.record().getTimestamp()
					? CheckResult.Redo : CheckResult.Success;
			}

			return switch (e.atomicTupleRecord.record().getState()) {
				case GlobalCacheManagerConst.StateRemoved ->
					// 被从cache中清除，不持有该记录的Global锁，简单重做即可。
					CheckResult.Redo;
				case GlobalCacheManagerConst.StateInvalid -> CheckResult.RedoAndReleaseLock; // Invalid 可能伴随 Reduce 或缓存清理，需释放锁。
				default -> e.atomicTupleRecord.timestamp() != e.atomicTupleRecord.record().getTimestamp()
					? CheckResult.Redo : CheckResult.Success;
			};
		} finally {
			e.atomicTupleRecord.record().exitFairLock();
		}
	}

	Lockey getLockey(TableKey key) {
		var l = locks;
		if (l == null)
			throw new IllegalStateException("transaction has no locks (not in perform)");
		return l.get(key);
	}

	private CheckResult lockAndCheck(Map.Entry<TableKey, RecordAccessed> e) {
		Lockey lockey = getLockey(e.getKey());
		boolean writeLock = e.getValue().dirty;
		lockey.enterLock(writeLock);
		holdLocks.add(lockey);
		return _check_(writeLock, e.getValue());
	}

	private CheckResult lockAndCheck(Procedure procedure) {
		var level = procedure.getTransactionLevel();
		boolean allRead = true;
		var saveSize = savepoints.size();
		if (saveSize > 0) {
			var it = savepoints.get(saveSize - 1).logIterator();
			if (it != null) {
				while (it.moveToNext()) {
					var log = it.value();
					var belong = log.getBelong();
					if (belong == null)
						continue;

					var tkey = belong.tableKey();
					if (tkey == null) {
						// 非受管 Bean 没有 tableKey。
						logger.error("impossible! log bean({}): {}", belong.getClass().getName(), belong);
						continue;
					}
					var record = accessedRecords.get(tkey);
					if (record != null) {
						record.dirty = true;
						allRead = false;
					} else
						logger.error("impossible! record not found.");
				}
			}
		}

		if (allRead && level == TransactionLevel.AllowDirtyWhenAllRead)
			return CheckResult.Success;

		boolean conflict = false; // 冲突后仍继续加锁，供重试复用。
		if (holdLocks.isEmpty()) {
			for (var e : accessedRecords.entrySet()) {
				var r = lockAndCheck(e);
				switch (r) {
				case Success:
					break;
				case Redo:
					conflict = true;
					ZezeCounter.instance.tableCounter(e.getKey().getId(), ZezeCounter.TableMetric.REDO).increment();
					break;
				default:
					return r;
				}
			}
			return conflict ? CheckResult.Redo : CheckResult.Success;
		}

		int index = 0;
		int n = holdLocks.size();
		final var ite = accessedRecords.entrySet().iterator();
		for (var e = ite.hasNext() ? ite.next() : null; e != null; ) {
			if (index >= n) {
				var r = lockAndCheck(e);
				switch (r) {
				case Success:
					break;
				case Redo:
					conflict = true;
					ZezeCounter.instance.tableCounter(e.getKey().getId(), ZezeCounter.TableMetric.REDO).increment();
					break;
				default:
					return r;
				}
				e = ite.hasNext() ? ite.next() : null;
				continue;
			}

			Lockey curLock = holdLocks.get(index);
			int c = curLock.getTableKey().compareTo(e.getKey());

			if (c == 0) {
				if (e.getValue().dirty && !curLock.isWriteLockHeld()) {
					// 升级读锁前释放当前及后续锁，保持锁序并避免读写锁升级死锁。
					n = _unlock_start_(index, n);
					continue;
				}
				// 本地锁未变时仍需检查 Global 状态。
				var r = _check_(e.getValue().dirty, e.getValue());
				switch (r) {
				case Success:
					break;
				case Redo:
					conflict = true;
					ZezeCounter.instance.tableCounter(e.getKey().getId(), ZezeCounter.TableMetric.REDO).increment();
					break;
				default:
					// _check_可能需要到Global提升状态，这里可能发生GLOBAL-DEAD-LOCK。
					return r;
				}
				index++;
				e = ite.hasNext() ? ite.next() : null;
				continue;
			}
			if (c < 0) {
				// 释放本轮不再需要的较小键锁。
				int unlockEndIndex = index;
				while (unlockEndIndex < n && holdLocks.get(unlockEndIndex).getTableKey().compareTo(e.getKey()) < 0)
					holdLocks.get(unlockEndIndex++).exitLock();
				holdLocks.subList(index, unlockEndIndex).clear();
				n = holdLocks.size();
				continue;
			}

			// 新增较小键的锁前，先释放当前及后续锁以维持锁序。
			n = _unlock_start_(index, n);
		}
		return conflict ? CheckResult.Redo : CheckResult.Success;
	}

	private int _unlock_start_(int index, int nLast) {
		for (int i = index; i < nLast; ++i)
			holdLocks.get(i).exitLock();
		holdLocks.subList(index, nLast).clear();
		return holdLocks.size();
	}

	@Contract("_, _ -> fail")
	public void throwAbort(String msg, @Nullable Throwable cause) {
		if (state != TransactionState.Running)
			throw new IllegalStateException("Abort: State Is Not Running: " + state + ", msg: " + msg, cause);
		state = TransactionState.Abort;
		throw cause != null ? new GoBackZeze(msg, cause) : new GoBackZeze(msg);
	}

	@Contract("_, _, _ -> fail")
	public void throwRedoAndReleaseLock(int tableId, String msg, @Nullable Throwable cause) {
		ZezeCounter.instance.tableCounter(tableId, ZezeCounter.TableMetric.REDO).increment();
		if (state != TransactionState.Running)
			throw new IllegalStateException("RedoAndReleaseLock: State Is Not Running: " + state + ", msg: " + msg, cause);
		state = TransactionState.RedoAndReleaseLock;
		throw cause != null ? new GoBackZeze(msg, cause) : new GoBackZeze(msg);
	}

	@Contract("_, _ -> fail")
	public void throwRedo(int tableId, String msg) {
		ZezeCounter.instance.tableCounter(tableId, ZezeCounter.TableMetric.REDO).increment();
		if (state != TransactionState.Running)
			throw new IllegalStateException("Redo: State Is Not Running: " + state);
		state = TransactionState.Redo;
		throw new GoBackZeze(msg);
	}

	public void verifyRunning() {
		if (state != TransactionState.Running)
			throw new IllegalStateException("State Is Not Running: " + state);
	}

	public void verifyRunningOrCompleted() {
		if (state != TransactionState.Running && state != TransactionState.Completed)
			throw new IllegalStateException("State Is Not Running or Completed: " + state);
	}

	/**
	 * 提交闸门关闭时由 RelativeRecordSet 抛出，perform 转为回滚并返回 Procedure.Closed。
	 */
	static class RejectWhileStopping extends RuntimeException {
		@Serial
		private static final long serialVersionUID = 0L;

		RejectWhileStopping(String msg) {
			super(msg);
		}
	}

	Savepoint lastSavepoint() {
		return savepoints.getLast();
	}

}
