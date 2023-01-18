package Zeze.Transaction;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;
import Zeze.Services.GlobalCacheManagerConst;
import Zeze.Util.Macro;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Transaction {
	private static final Logger logger = LogManager.getLogger(Transaction.class);
	private static final ThreadLocal<Transaction> threadLocal = new ThreadLocal<>();

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

	public static Transaction getCurrent() {
		var t = threadLocal.get();
		return t != null && t.created ? t : null;
	}

	public static Transaction getCurrentVerifyRead(Bean bean) {
		return getCurrent();
	}

	public static Transaction getCurrentVerifyWrite(Bean bean) {
		var t = getCurrent();
		if (t != null)
			t.verifyRecordForWrite(bean);
		return t;
	}

	private final ArrayList<Lockey> holdLocks = new ArrayList<>(); // 读写锁的话需要一个包装类，用来记录当前维持的是哪个锁。
	private final ArrayList<Procedure> procedureStack = new ArrayList<>(); // 嵌套存储过程栈。
	private final ArrayList<Savepoint> savepoints = new ArrayList<>();
	private final ArrayList<Savepoint.Action> actions = new ArrayList<>();
	private final TreeMap<TableKey, RecordAccessed> accessedRecords = new TreeMap<>();
	private Locks locks;
	private TransactionState state = TransactionState.Running;
	private boolean created;
	private boolean alwaysReleaseLockWhenRedo;
	private final ArrayList<Bean> redoBeans = new ArrayList<>();

	private Transaction() {
	}

	public ArrayList<Procedure> getProcedureStack() {
		return procedureStack;
	}

	TreeMap<TableKey, RecordAccessed> getAccessedRecords() {
		return accessedRecords;
	}

	public boolean isRunning() {
		return state == TransactionState.Running;
	}

	public Procedure getTopProcedure() {
		var stackSize = procedureStack.size();
		return stackSize > 0 ? procedureStack.get(stackSize - 1) : null;
	}

	private void reuseTransaction() {
		// holdLocks.Clear(); // 执行完肯定清理了。
		procedureStack.clear();
		savepoints.clear();
		actions.clear();
		redoBeans.clear();
		accessedRecords.clear();
		locks = null;
		state = TransactionState.Running;
		created = false;
		alwaysReleaseLockWhenRedo = false;
	}

	public void begin() {
		var saveSize = savepoints.size();
		savepoints.add(saveSize > 0 ? savepoints.get(saveSize - 1).beginSavepoint() : new Savepoint());
	}

	public void commit() {
		int saveSize = savepoints.size();
		if (saveSize > 1)
			savepoints.get(saveSize - 2).mergeCommitFrom(savepoints.remove(saveSize - 1)); // 嵌套事务，把日志合并到上一层。
		// else // 最外层存储过程提交在 Perform 中处理
	}

	public void rollback() {
		int lastIndex = savepoints.size() - 1;
		Savepoint last = savepoints.remove(lastIndex);
		// last.Rollback();
		if (lastIndex > 0)
			savepoints.get(lastIndex - 1).mergeRollbackFrom(last); // 嵌套事务，把日志合并到上一层。
		else
			last.mergeRollbackActions(actions);
	}

	public Log logGetOrAdd(long logKey, Supplier<Log> logFactory) {
		var log = getLog(logKey);
		if (log == null)
			putLog(log = logFactory.get());
		return log;
	}

	public Log getLog(long key) {
		verifyRunningOrCompleted();
		// 允许没有 savepoint 时返回 null. 就是说允许在保存点不存在时进行读取操作。
		var saveSize = savepoints.size();
		return saveSize > 0 ? savepoints.get(saveSize - 1).getLog(key) : null;
	}

	public void putLog(Log log) {
		verifyRunning();
		savepoints.get(savepoints.size() - 1).putLog(log);
	}

	private void triggerRedoActions() {
		redoBeans.forEach(Bean::resetRootInfo);
	}

	static void whileRedo(Bean b) {
		// 这个目前仅用来重置Bean.RootInfo。
		// 而RootInfo的设置可能在事务外使用，此时忽略action的执行。
		var current = getCurrent();
		if (null != current) {
			current.redoBeans.add(b);
		}
	}

	public static void whileCommit(Runnable action) {
		//noinspection ConstantConditions
		getCurrent().runWhileCommit(action);
	}

	public static void whileRollback(Runnable action) {
		//noinspection ConstantConditions
		getCurrent().runWhileRollback(action);
	}

	public void runWhileCommit(Runnable action) {
		verifyRunning();
		savepoints.get(savepoints.size() - 1).addCommitAction(action);
	}

	public void runWhileRollback(Runnable action) {
		verifyRunning();
		savepoints.get(savepoints.size() - 1).addRollbackAction(action);
	}

	void setAlwaysReleaseLockWhenRedo() {
		alwaysReleaseLockWhenRedo = true;
		if (!holdLocks.isEmpty())
			throwRedo();
	}

	/**
	 * Procedure 第一层入口，总的处理流程，包括重做和所有错误处理。
	 *
	 * @param procedure first procedure
	 */
	public long perform(Procedure procedure) {
		try {
			var checkpoint = procedure.getZeze().getCheckpoint();
			if (checkpoint == null)
				return Procedure.Closed;
			for (int tryCount = 0; tryCount < 256; ++tryCount) { // 最多尝试次数
				// 默认在锁内重复尝试，除非CheckResult.RedoAndReleaseLock，否则由于CheckResult.Redo保持锁会导致死锁。
				checkpoint.enterFlushReadLock();
				try {
					for (; tryCount < 256; ++tryCount) { // 最多尝试次数
						CheckResult checkResult = CheckResult.Redo; // 用来决定是否释放锁，除非 _lock_and_check_ 明确返回需要释放锁，否则都不释放。
						try {
							var result = procedure.call();
							switch (state) {
							case Running:
								var saveSize = savepoints.size();
								if ((result == Procedure.Success && saveSize != 1)
										|| (result != Procedure.Success && saveSize > 0)) {
									// 这个错误不应该重做
									logger.fatal("Transaction.Perform:{}. savepoints.Count != 1.", procedure);
									finalRollback(procedure);
									return Procedure.ErrorSavepoint;
								}
								checkResult = lockAndCheck(procedure.getTransactionLevel());
								if (checkResult == CheckResult.Success) {
									if (result == Procedure.Success) {
										finalCommit(procedure);
										// 正常一次成功的不统计，用来观察redo多不多。
										// 失败在 Procedure.cs 中的统计。
										if (tryCount > 0) {
											if (Macro.enableStatistics) {
												ProcedureStatistics.getInstance().getOrAdd("Zeze.Transaction.TryCount").getOrAdd(tryCount).increment();
											}
										}
										return Procedure.Success;
									}
									finalRollback(procedure, true);
									return result;
								}
								break; // retry

							case Abort:
								logger.warn("Transaction.Perform: Abort");
								finalRollback(procedure);
								return Procedure.AbortException;

							case Redo:
								//checkResult = CheckResult.Redo;
								break; // retry

							case RedoAndReleaseLock:
								checkResult = CheckResult.RedoAndReleaseLock;
								break; // retry
							}
							// retry clear in finally
							if (alwaysReleaseLockWhenRedo && checkResult == CheckResult.Redo)
								checkResult = CheckResult.RedoAndReleaseLock;
							triggerRedoActions();
						} catch (Throwable e) { // logger.error, logger.warn, rethrow AssertionError, ignored
							// Procedure.Call 里面已经处理了异常。只有 unit test 或者重做或者内部错误会到达这里。
							// 在 unit test 下，异常日志会被记录两次。
							switch (state) {
							case Running:
								logger.error("Transaction.Perform:{} exception. run count:{}", procedure, tryCount, e);
								if (!savepoints.isEmpty()) {
									// 这个错误不应该重做
									logger.fatal("Transaction.Perform:{}. exception. savepoints.Count != 0.", procedure, e);
									finalRollback(procedure);
									return Procedure.ErrorSavepoint;
								}
								// 对于 unit test 的异常特殊处理，与unit test框架能搭配工作
								if (e instanceof AssertionError) {
									finalRollback(procedure);
									throw (AssertionError)e;
								}
								checkResult = lockAndCheck(procedure.getTransactionLevel());
								if (checkResult == CheckResult.Success) {
									finalRollback(procedure, true);
									return Procedure.Exception;
								}
								// retry
								break;

							case Abort:
								if (!"GlobalAgent.Acquire Failed".equals(e.getMessage()) &&
										!"GlobalAgent In FastErrorPeriod".equals(e.getMessage()))
									logger.warn("Transaction.Perform: Abort", e);
								finalRollback(procedure);
								return Procedure.AbortException;

							case Redo:
								checkResult = CheckResult.Redo;
								break;

							case RedoAndReleaseLock:
								checkResult = CheckResult.RedoAndReleaseLock;
								break;

							default: // case Completed:
								if (e instanceof AssertionError)
									throw (AssertionError)e;
							}
							triggerRedoActions();
							// retry
						} finally {
							if (checkResult == CheckResult.RedoAndReleaseLock) {
								holdLocks.forEach(Lockey::exitLock);
								holdLocks.clear();
							}
							// retry 可能保持已有的锁，清除记录和保存点。
							accessedRecords.clear();
							savepoints.clear();
							actions.clear();
							redoBeans.clear();

							state = TransactionState.Running; // prepare to retry
						}

						if (checkResult == CheckResult.RedoAndReleaseLock) {
							// logger.debug("CheckResult.RedoAndReleaseLock break {}", procedure);
							break;
						}
					}
				} finally {
					checkpoint.exitFlushReadLock();
				}
				//logger.Debug("Checkpoint.WaitRun {0}", procedure);
				// 实现Fresh队列以后删除Sleep。
				try {
					Thread.sleep(Zeze.Util.Random.getInstance().nextInt(80) + 20);
				} catch (InterruptedException e) {
					logger.error("", e);
				}
			}
			logger.error("Transaction.Perform:{}. too many try.", procedure);
			finalRollback(procedure);
			return Procedure.TooManyTry;
		} finally {
			holdLocks.forEach(Lockey::exitLock);
			holdLocks.clear();
		}
	}

	private void triggerActions(Procedure procedure) {
		for (var action : actions) {
			try {
				action.action.run();
			} catch (AssertionError e) {
				throw e;
			} catch (Throwable e) { // logger.error
				String typeStr;
				if (action.actionType == Savepoint.ActionType.COMMIT) {
					typeStr = "commit";
				} else if (action.actionType == Savepoint.ActionType.NESTED_ROLLBACK) {
					typeStr = "nestedRollback";
				} else {
					typeStr = "rollback";
				}
				logger.error("{} Procedure {} Action {}", typeStr, procedure.getActionName(), action.getClass().getName(), e);
			}
		}
	}

	private void _trigger_commit_actions_(Procedure procedure) {
		triggerActions(procedure);
	}

	private void _trigger_rollback_actions_(Procedure procedure) {
		triggerActions(procedure);
	}

	private void finalCommit(Procedure procedure) {
		// 下面不允许失败了，因为最终提交失败，数据可能不一致，而且没法恢复。
		// 可以在最终提交里可以实现每事务checkpoint。
		var lastSp = savepoints.get(savepoints.size() - 1);
		RelativeRecordSet.tryUpdateAndCheckpoint(this, procedure, () -> {
			try {
				lastSp.mergeCommitActions(actions);
				lastSp.commit();
				for (var v : accessedRecords.values()) {
					v.atomicTupleRecord.record.setNotFresh();
					if (v.dirty) {
						v.atomicTupleRecord.record.commit(v);
						var newValue = v.atomicTupleRecord.record.getSoftValue();
						if (null != newValue) {
							// 如果newValue为null，表示记录被删除，以后再次PutValue，version从0重新开始。
							var oldValue = v.atomicTupleRecord.strongRef;
							var oldVersion = null != oldValue ? oldValue.version() : 0;
							newValue.version(oldVersion + 1);
						}
					}
				}
			} catch (Throwable e) { // halt
				logger.fatal("Transaction.finalCommit {}", procedure, e);
				LogManager.shutdown();
				Runtime.getRuntime().halt(54321);
			}
		});

		// 禁止在listener回调中访问表格的操作。除了回调参数中给定的记录可以访问。
		// 不再支持在回调中再次执行事务。
		// 在Notify之前设置的。
		state = TransactionState.Completed;

		// collect logs and notify listeners
		try {
			var cc = new Changes(this);
			var it = lastSp.logIterator();
			if (it != null) {
				while (it.moveToNext()) {
					var log = it.value();
					var logBelong = log.getBelong();
					// 这里都是修改操作的日志，没有Owner的日志是特殊测试目的加入的，简单忽略即可。
					if (logBelong != null && logBelong.isManaged()) {
						// 第一个参数Owner为null，表示bean属于record，到达root了。
						cc.collect(logBelong, log);
					}
				}
			}

			for (var ar : accessedRecords.values()) {
				if (ar.dirty)
					cc.collectRecord(ar);
			}
			cc.notifyListener();
		} catch (Throwable ex) { // logger.error
			logger.error("", ex);
		}

		_trigger_commit_actions_(procedure);
	}

	private void finalRollback(Procedure procedure) {
		finalRollback(procedure, false);
	}

	private void finalRollback(Procedure procedure, boolean executeRollbackAction) {
		for (var ra : accessedRecords.values()) {
			ra.atomicTupleRecord.record.setNotFresh();
		}
		savepoints.clear(); // 这里可以安全的清除日志，这样如果 rollback_action 需要读取数据，将读到原始的。
		state = TransactionState.Completed;
		if (executeRollbackAction) {
			_trigger_rollback_actions_(procedure);
		}
	}

	/**
	 * 只能添加一次。
	 *
	 * @param r record accessed
	 */
	void addRecordAccessed(Record.RootInfo root, RecordAccessed r) {
		verifyRunning();
		r.initRootInfo(root, null);
		accessedRecords.put(root.getTableKey(), r);
	}

	public RecordAccessed getRecordAccessed(TableKey key) {
		// 允许读取事务内访问过的记录。
		verifyRunningOrCompleted();
		return accessedRecords.get(key);
	}

	public void verifyRecordForWrite(Bean bean) {
		if (bean.rootInfo.getRecord().getState() == GlobalCacheManagerConst.StateRemoved) {
			throwRedo(); // 这个错误需要redo。不是逻辑错误。
		}
		var ra = getRecordAccessed(bean.tableKey());
		if (ra == null) {
			throw new IllegalStateException("VerifyRecordAccessed: Record Not Control Under Current Transaction. " + bean.tableKey());
		}
		if (bean.rootInfo.getRecord() != ra.atomicTupleRecord.record) {
			throw new IllegalStateException("VerifyRecordAccessed: Record Reloaded. " + bean.tableKey());
		}
		// 事务结束后可能会触发Listener，此时Commit已经完成，Timestamp已经改变，
		// 这种情况下不做RedoCheck，当然Listener的访问数据是只读的。
		if (ra.atomicTupleRecord.record.getTable().getZeze().getConfig().getFastRedoWhenConflict()
				&& state != TransactionState.Completed
				&& ra.atomicTupleRecord.record.getTimestamp() != ra.atomicTupleRecord.timestamp) {
			throwRedo();
		}
	}

	private enum CheckResult {
		Success,
		Redo,
		RedoAndReleaseLock
	}

	private static CheckResult _check_(boolean writeLock, RecordAccessed e) {
		e.atomicTupleRecord.record.enterFairLock();
		try {
			if (writeLock) {
				switch (e.atomicTupleRecord.record.getState()) {
				case GlobalCacheManagerConst.StateRemoved:
					// 被从cache中清除，不持有该记录的Global锁，简单重做即可。
					return CheckResult.Redo;

				case GlobalCacheManagerConst.StateInvalid:
					return CheckResult.RedoAndReleaseLock; // 写锁发现Invalid，可能有Reduce请求。

				case GlobalCacheManagerConst.StateModify:
					return e.atomicTupleRecord.timestamp != e.atomicTupleRecord.record.getTimestamp() ? CheckResult.Redo : CheckResult.Success;

				case GlobalCacheManagerConst.StateShare:
					// 这里可能死锁：另一个先获得提升的请求要求本机Reduce，但是本机Checkpoint无法进行下去，被当前事务挡住了。
					// 通过 GlobalCacheManager 检查死锁，返回失败;需要重做并释放锁。
					var acquire = e.atomicTupleRecord.record.acquire(GlobalCacheManagerConst.StateModify,
							e.atomicTupleRecord.record.isFresh(), false);
					if (acquire.resultState != GlobalCacheManagerConst.StateModify) {
						e.atomicTupleRecord.record.setNotFresh(); // 抢失败不再新鲜。
						logger.debug("Acquire Failed. Maybe DeadLock Found {}", e.atomicTupleRecord);
						e.atomicTupleRecord.record.setState(GlobalCacheManagerConst.StateInvalid); // 这里保留StateShare更好吗？
						return CheckResult.RedoAndReleaseLock;
					}
					e.atomicTupleRecord.record.setState(GlobalCacheManagerConst.StateModify);
					return e.atomicTupleRecord.timestamp != e.atomicTupleRecord.record.getTimestamp() ? CheckResult.Redo : CheckResult.Success;
				}
				return e.atomicTupleRecord.timestamp != e.atomicTupleRecord.record.getTimestamp() ? CheckResult.Redo : CheckResult.Success; // impossible
			}
			switch (e.atomicTupleRecord.record.getState()) {
			case GlobalCacheManagerConst.StateRemoved:
				// 被从cache中清除，不持有该记录的Global锁，简单重做即可。
				return CheckResult.Redo;

			case GlobalCacheManagerConst.StateInvalid:
				return CheckResult.RedoAndReleaseLock; // 发现Invalid，可能有Reduce请求或者被Cache清理，此时保险起见释放锁。
			}
			return e.atomicTupleRecord.timestamp != e.atomicTupleRecord.record.getTimestamp() ? CheckResult.Redo : CheckResult.Success;
		} finally {
			e.atomicTupleRecord.record.exitFairLock();
		}
	}

	private CheckResult lockAndCheck(Map.Entry<TableKey, RecordAccessed> e) {
		Lockey lockey = locks.get(e.getKey());
		boolean writeLock = e.getValue().dirty;
		lockey.enterLock(writeLock);
		holdLocks.add(lockey);
		return _check_(writeLock, e.getValue());
	}

	private CheckResult lockAndCheck(TransactionLevel level) {
		boolean allRead = true;
		var saveSize = savepoints.size();
		if (saveSize > 0) {
			// 全部 Rollback 时 Count 为 0；最后提交时 Count 必须为 1；
			// 其他情况属于Begin,Commit,Rollback不匹配。外面检查。
			var it = savepoints.get(saveSize - 1).logIterator();
			if (it != null) {
				while (it.moveToNext()) {
					// 特殊日志。不是 bean 的修改日志，当然也不会修改 Record。
					// 现在不会有这种情况，保留给未来扩展需要。
					Log log = it.value();
					if (log.getBean() == null)
						continue;

					TableKey tkey = log.getBean().tableKey();
					var record = accessedRecords.get(tkey);
					if (record != null) {
						record.dirty = true;
						allRead = false;
					} else {
						// 只有测试代码会把非 Managed 的 Bean 的日志加进来。
						logger.fatal("impossible! record not found.");
					}
				}
			}
		}

		if (allRead && level == TransactionLevel.AllowDirtyWhenAllRead)
			return CheckResult.Success; // 使用一个新的enum表示一下？

		boolean conflict = false; // 冲突了，也继续加锁，为重做做准备！！！
		if (holdLocks.isEmpty()) {
			for (var e : accessedRecords.entrySet()) {
				var r = lockAndCheck(e);
				switch (r) {
				case Success:
					break;
				case Redo:
					conflict = true;
					break; // continue lock
				default:
					return r;
				}
			}
			return conflict ? CheckResult.Redo : CheckResult.Success;
		}

		int index = 0;
		int n = holdLocks.size();
		final var ite = accessedRecords.entrySet().iterator();
		var e = ite.hasNext() ? ite.next() : null;
		while (null != e) {
			// 如果 holdLocks 全部被对比完毕，直接锁定它
			if (index >= n) {
				var r = lockAndCheck(e);
				switch (r) {
				case Success:
					break;
				case Redo:
					conflict = true;
					break; // continue lock
				default:
					return r;
				}
				e = ite.hasNext() ? ite.next() : null;
				continue;
			}

			Lockey curLock = holdLocks.get(index);
			int c = curLock.getTableKey().compareTo(e.getKey());

			// holdLocks a  b  ...
			// needLocks a  b  ...
			if (c == 0) {
				// 这里可能发生读写锁提升
				if (e.getValue().dirty && !curLock.isWriteLockHeld()) {
					// 必须先全部释放，再升级当前记录锁，再锁后面的记录。
					// 直接 unlockRead，lockWrite会死锁。
					n = _unlock_start_(index, n);
					// 从当前index之后都是新加锁，并且index和n都不会再发生变化。
					// 重新从当前 e 继续锁。
					continue;
				}
				// BUG 即使锁内。Record.Global.State 可能没有提升到需要水平。需要重新_check_。
				var r = _check_(e.getValue().dirty, e.getValue());
				switch (r) {
				case Success:
					// 已经锁内，所以肯定不会冲突，多数情况是这个。
					break;
				case Redo:
					// Impossible!
					conflict = true;
					break; // continue lock
				default:
					// _check_可能需要到Global提升状态，这里可能发生GLOBAL-DEAD-LOCK。
					return r;
				}
				++index;
				e = ite.hasNext() ? ite.next() : null;
				continue;
			}
			// holdLocks a  b  ...
			// needLocks a  c  ...
			if (c < 0) {
				// 释放掉 比当前锁序小的锁，因为当前事务中不再需要这些锁
				int unlockEndIndex = index;
				for (; unlockEndIndex < n && holdLocks.get(unlockEndIndex).getTableKey().compareTo(e.getKey()) < 0; ++unlockEndIndex)
					holdLocks.get(unlockEndIndex).exitLock();
				holdLocks.subList(index, unlockEndIndex).clear();
				n = holdLocks.size();
				// 重新从当前 e 继续锁。
				continue;
			}

			// holdLocks a  c  ...
			// needLocks a  b  ...
			// 为了不违背锁序，释放从当前锁开始的所有锁
			n = _unlock_start_(index, n);
			// 重新从当前 e 继续锁。
		}
		return conflict ? CheckResult.Redo : CheckResult.Success;
	}

	private int _unlock_start_(int index, int nLast) {
		for (int i = index; i < nLast; ++i)
			holdLocks.get(i).exitLock();
		holdLocks.subList(index, nLast).clear();
		return holdLocks.size();
	}

	public void throwAbort(String msg, Throwable cause) {
		if (state != TransactionState.Running)
			throw new IllegalStateException("Abort: State Is Not Running: " + state);
		state = TransactionState.Abort;
		GoBackZeze.Throw(msg, cause);
	}

	public void throwRedoAndReleaseLock(String msg, Throwable cause) {
		if (state != TransactionState.Running)
			throw new IllegalStateException("RedoAndReleaseLock: State Is Not Running: " + state);
		state = TransactionState.RedoAndReleaseLock;
		if (Macro.enableStatistics) {
			//noinspection ConstantConditions
			ProcedureStatistics.getInstance().getOrAdd(getTopProcedure().getActionName()).getOrAdd(Procedure.RedoAndRelease).increment();
		}
		GoBackZeze.Throw(msg, cause);
	}

	public void throwRedo() {
		if (state != TransactionState.Running)
			throw new IllegalStateException("RedoAndReleaseLock: State Is Not Running: " + state);
		state = TransactionState.Redo;
		GoBackZeze.Throw("Redo", null);
	}

	public void verifyRunning() {
		if (state != TransactionState.Running)
			throw new IllegalStateException("State Is Not Running: " + state);
	}

	public void verifyRunningOrCompleted() {
		if (state != TransactionState.Running && state != TransactionState.Completed)
			throw new IllegalStateException("State Is Not RunningOrCompleted: " + state);
	}
}
