package Zeze.Transaction;

import java.util.ArrayList;
import java.util.List;
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

	public static Transaction Create(Locks locks) {
		var t = threadLocal.get();
		if (t == null)
			threadLocal.set(t = new Transaction());
		t.Locks = locks;
		t.Created = true;
		return t;
	}

	public static void Destroy() {
		var t = threadLocal.get();
		if (t != null)
			t.ReuseTransaction();
	}

	public static Transaction getCurrent() {
		var t = threadLocal.get();
		return t != null && t.Created ? t : null;
	}

	private final ArrayList<Lockey> holdLocks = new ArrayList<>(); // 读写锁的话需要一个包装类，用来记录当前维持的是哪个锁。
	private final ArrayList<Procedure> ProcedureStack = new ArrayList<>(); // 嵌套存储过程栈。
	private final ArrayList<Savepoint> Savepoints = new ArrayList<>();
	private final ArrayList<Savepoint.Action> Actions = new ArrayList<>();
	private final TreeMap<TableKey, RecordAccessed> AccessedRecords = new TreeMap<>();
	private Locks Locks;
	private TransactionState State = TransactionState.Running;
	private boolean Created;
	private boolean AlwaysReleaseLockWhenRedo;

	private Transaction() {
	}

	public ArrayList<Procedure> getProcedureStack() {
		return ProcedureStack;
	}

	TreeMap<TableKey, RecordAccessed> getAccessedRecords() {
		return AccessedRecords;
	}

	public Procedure getTopProcedure() {
		var stackSize = ProcedureStack.size();
		return stackSize > 0 ? ProcedureStack.get(stackSize - 1) : null;
	}

	private void ReuseTransaction() {
		// holdLocks.Clear(); // 执行完肯定清理了。
		ProcedureStack.clear();
		Savepoints.clear();
		Actions.clear();
		RedoActions.clear();
		AccessedRecords.clear();
		Locks = null;
		State = TransactionState.Running;
		Created = false;
		AlwaysReleaseLockWhenRedo = false;
	}

	public void Begin() {
		var saveSize = Savepoints.size();
		Savepoints.add(saveSize > 0 ? Savepoints.get(saveSize - 1).BeginSavepoint() : new Savepoint());
	}

	public void Commit() {
		int saveSize = Savepoints.size();
		if (saveSize > 1)
			Savepoints.get(saveSize - 2).MergeCommitFrom(Savepoints.remove(saveSize - 1)); // 嵌套事务，把日志合并到上一层。
		// else // 最外层存储过程提交在 Perform 中处理
	}

	public void Rollback() {
		int lastIndex = Savepoints.size() - 1;
		Savepoint last = Savepoints.remove(lastIndex);
		// last.Rollback();
		if (lastIndex > 0)
			Savepoints.get(lastIndex - 1).MergeRollbackFrom(last); // 嵌套事务，把日志合并到上一层。
		else
			last.MergeRollbackActions(Actions);
	}

	public Log LogGetOrAdd(long logKey, Supplier<Log> logFactory) {
		var log = GetLog(logKey);
		if (log == null)
			PutLog(log = logFactory.get());
		return log;
	}

	public Log GetLog(long key) {
		VerifyRunningOrCompleted();
		// 允许没有 savepoint 时返回 null. 就是说允许在保存点不存在时进行读取操作。
		var saveSize = Savepoints.size();
		return saveSize > 0 ? Savepoints.get(saveSize - 1).GetLog(key) : null;
	}

	public void PutLog(Log log) {
		VerifyRunning();
		Savepoints.get(Savepoints.size() - 1).PutLog(log);
	}

	private final List<Runnable> RedoActions = new ArrayList<>();

	private void TriggerRedoActions() {
		for (var a : RedoActions)
			a.run(); // redo action 的回调不处理异常。向外面抛出并中断事务。
	}

	static void whileRedo(Runnable action) {
		getCurrent().RedoActions.add(action);
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
		VerifyRunning();
		Savepoints.get(Savepoints.size() - 1).addCommitAction(action);
	}

	public void runWhileRollback(Runnable action) {
		VerifyRunning();
		Savepoints.get(Savepoints.size() - 1).addRollbackAction(action);
	}

	void SetAlwaysReleaseLockWhenRedo() {
		AlwaysReleaseLockWhenRedo = true;
		if (!holdLocks.isEmpty())
			ThrowRedo();
	}

	/**
	 * Procedure 第一层入口，总的处理流程，包括重做和所有错误处理。
	 *
	 * @param procedure first procedure
	 */
	public long Perform(Procedure procedure) {
		try {
			var checkpoint = procedure.getZeze().getCheckpoint();
			if (checkpoint == null)
				return Procedure.Closed;
			for (int tryCount = 0; tryCount < 256; ++tryCount) { // 最多尝试次数
				// 默认在锁内重复尝试，除非CheckResult.RedoAndReleaseLock，否则由于CheckResult.Redo保持锁会导致死锁。
				checkpoint.EnterFlushReadLock();
				try {
					for (; tryCount < 256; ++tryCount) { // 最多尝试次数
						CheckResult checkResult = CheckResult.Redo; // 用来决定是否释放锁，除非 _lock_and_check_ 明确返回需要释放锁，否则都不释放。
						try {
							var result = procedure.Call();
							switch (State) {
							case Running:
								var saveSize = Savepoints.size();
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
											if (Macro.EnableStatistics) {
												ProcedureStatistics.getInstance().GetOrAdd("Zeze.Transaction.TryCount").GetOrAdd(tryCount).increment();
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
							if (AlwaysReleaseLockWhenRedo && checkResult == CheckResult.Redo)
								checkResult = CheckResult.RedoAndReleaseLock;
						} catch (Throwable e) {
							// Procedure.Call 里面已经处理了异常。只有 unit test 或者重做或者内部错误会到达这里。
							// 在 unit test 下，异常日志会被记录两次。
							switch (State) {
							case Running:
								logger.error("Transaction.Perform:{} exception. run count:{}", procedure, tryCount, e);
								if (!Savepoints.isEmpty()) {
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
							// retry
						} finally {
							if (checkResult == CheckResult.RedoAndReleaseLock) {
								holdLocks.forEach(Lockey::ExitLock);
								holdLocks.clear();
							}
							// retry 可能保持已有的锁，清除记录和保存点。
							AccessedRecords.clear();
							Savepoints.clear();
							Actions.clear();
							TriggerRedoActions();
							RedoActions.clear();

							State = TransactionState.Running; // prepare to retry
						}

						if (checkResult == CheckResult.RedoAndReleaseLock) {
							// logger.debug("CheckResult.RedoAndReleaseLock break {}", procedure);
							break;
						}
					}
				} finally {
					checkpoint.ExitFlushReadLock();
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
			holdLocks.forEach(Lockey::ExitLock);
			holdLocks.clear();
		}
	}

	private void triggerActions(Procedure procedure) {
		for (var action : Actions) {
			try {
				action.action.run();
			} catch (AssertionError e) {
				throw e;
			} catch (Throwable e) {
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
		var lastSp = Savepoints.get(Savepoints.size() - 1);
		RelativeRecordSet.TryUpdateAndCheckpoint(this, procedure, () -> {
			try {
				lastSp.MergeCommitActions(Actions);
				lastSp.Commit();
				for (var v : AccessedRecords.values()) {
					v.AtomicTupleRecord.Record.setNotFresh();
					if (v.Dirty) {
						v.AtomicTupleRecord.Record.Commit(v);
					}
				}
			} catch (Throwable e) {
				logger.fatal("Transaction.finalCommit {}", procedure, e);
				LogManager.shutdown();
				Runtime.getRuntime().halt(54321);
			}
		});

		// 禁止在listener回调中访问表格的操作。除了回调参数中给定的记录可以访问。
		// 不再支持在回调中再次执行事务。
		// 在Notify之前设置的。
		State = TransactionState.Completed;

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
						cc.Collect(logBelong, log);
					}
				}
			}

			for (var ar : AccessedRecords.values()) {
				if (ar.Dirty)
					cc.CollectRecord(ar);
			}
			cc.NotifyListener();
		} catch (Throwable ex) {
			logger.error("", ex);
		}

		_trigger_commit_actions_(procedure);
	}

	private void finalRollback(Procedure procedure) {
		finalRollback(procedure, false);
	}

	private void finalRollback(Procedure procedure, boolean executeRollbackAction) {
		for (var ra : AccessedRecords.values()) {
			ra.AtomicTupleRecord.Record.setNotFresh();
		}
		Savepoints.clear(); // 这里可以安全的清除日志，这样如果 rollback_action 需要读取数据，将读到原始的。
		State = TransactionState.Completed;
		if (executeRollbackAction) {
			_trigger_rollback_actions_(procedure);
		}
	}

	/**
	 * 只能添加一次。
	 *
	 * @param r record accessed
	 */
	void AddRecordAccessed(Record.RootInfo root, RecordAccessed r) {
		VerifyRunning();
		r.InitRootInfo(root, null);
		AccessedRecords.put(root.getTableKey(), r);
	}

	public RecordAccessed GetRecordAccessed(TableKey key) {
		// 允许读取事务内访问过的记录。
		VerifyRunningOrCompleted();
		return AccessedRecords.get(key);
	}

	public void VerifyRecordAccessed(Bean bean) {
		VerifyRecordAccessed(bean, false);
	}

	public void VerifyRecordAccessed(Bean bean, @SuppressWarnings("unused") boolean IsRead) {
		if (IsRead)
			return; // allow read

		if (bean.RootInfo.getRecord().getState() == GlobalCacheManagerConst.StateRemoved) {
			ThrowRedo(); // 这个错误需要redo。不是逻辑错误。
		}
		var ra = GetRecordAccessed(bean.getTableKey());
		if (ra == null) {
			throw new IllegalStateException("VerifyRecordAccessed: Record Not Control Under Current Transaction. " + bean.getTableKey());
		}
		if (bean.RootInfo.getRecord() != ra.AtomicTupleRecord.Record) {
			throw new IllegalStateException("VerifyRecordAccessed: Record Reloaded. " + bean.getTableKey());
		}
		// 事务结束后可能会触发Listener，此时Commit已经完成，Timestamp已经改变，
		// 这种情况下不做RedoCheck，当然Listener的访问数据是只读的。
		if (ra.AtomicTupleRecord.Record.getTable().getZeze().getConfig().getFastRedoWhenConflict()
				&& State != TransactionState.Completed
				&& ra.AtomicTupleRecord.Record.getTimestamp() != ra.AtomicTupleRecord.Timestamp) {
			ThrowRedo();
		}
	}

	private enum CheckResult {
		Success,
		Redo,
		RedoAndReleaseLock
	}

	private static CheckResult _check_(boolean writeLock, RecordAccessed e) {
		e.AtomicTupleRecord.Record.EnterFairLock();
		try {
			if (writeLock) {
				switch (e.AtomicTupleRecord.Record.getState()) {
				case GlobalCacheManagerConst.StateRemoved:
					// 被从cache中清除，不持有该记录的Global锁，简单重做即可。
					return CheckResult.Redo;

				case GlobalCacheManagerConst.StateInvalid:
					return CheckResult.RedoAndReleaseLock; // 写锁发现Invalid，可能有Reduce请求。

				case GlobalCacheManagerConst.StateModify:
					return e.AtomicTupleRecord.Timestamp != e.AtomicTupleRecord.Record.getTimestamp() ? CheckResult.Redo : CheckResult.Success;

				case GlobalCacheManagerConst.StateShare:
					// 这里可能死锁：另一个先获得提升的请求要求本机Reduce，但是本机Checkpoint无法进行下去，被当前事务挡住了。
					// 通过 GlobalCacheManager 检查死锁，返回失败;需要重做并释放锁。
					var acquire = e.AtomicTupleRecord.Record.Acquire(GlobalCacheManagerConst.StateModify,
							e.AtomicTupleRecord.Record.isFresh(), false);
					if (acquire.ResultState != GlobalCacheManagerConst.StateModify) {
						e.AtomicTupleRecord.Record.setNotFresh(); // 抢失败不再新鲜。
						logger.debug("Acquire Failed. Maybe DeadLock Found {}", e.AtomicTupleRecord);
						e.AtomicTupleRecord.Record.setState(GlobalCacheManagerConst.StateInvalid); // 这里保留StateShare更好吗？
						return CheckResult.RedoAndReleaseLock;
					}
					e.AtomicTupleRecord.Record.setState(GlobalCacheManagerConst.StateModify);
					return e.AtomicTupleRecord.Timestamp != e.AtomicTupleRecord.Record.getTimestamp() ? CheckResult.Redo : CheckResult.Success;
				}
				return e.AtomicTupleRecord.Timestamp != e.AtomicTupleRecord.Record.getTimestamp() ? CheckResult.Redo : CheckResult.Success; // impossible
			}
			switch (e.AtomicTupleRecord.Record.getState()) {
			case GlobalCacheManagerConst.StateRemoved:
				// 被从cache中清除，不持有该记录的Global锁，简单重做即可。
				return CheckResult.Redo;

			case GlobalCacheManagerConst.StateInvalid:
				return CheckResult.RedoAndReleaseLock; // 发现Invalid，可能有Reduce请求或者被Cache清理，此时保险起见释放锁。
			}
			return e.AtomicTupleRecord.Timestamp != e.AtomicTupleRecord.Record.getTimestamp() ? CheckResult.Redo : CheckResult.Success;
		} finally {
			e.AtomicTupleRecord.Record.ExitFairLock();
		}
	}

	private CheckResult lockAndCheck(Map.Entry<TableKey, RecordAccessed> e) {
		Lockey lockey = Locks.Get(e.getKey());
		boolean writeLock = e.getValue().Dirty;
		lockey.EnterLock(writeLock);
		holdLocks.add(lockey);
		return _check_(writeLock, e.getValue());
	}

	private CheckResult lockAndCheck(TransactionLevel level) {
		boolean allRead = true;
		var saveSize = Savepoints.size();
		if (saveSize > 0) {
			// 全部 Rollback 时 Count 为 0；最后提交时 Count 必须为 1；
			// 其他情况属于Begin,Commit,Rollback不匹配。外面检查。
			var it = Savepoints.get(saveSize - 1).logIterator();
			if (it != null) {
				while (it.moveToNext()) {
					// 特殊日志。不是 bean 的修改日志，当然也不会修改 Record。
					// 现在不会有这种情况，保留给未来扩展需要。
					Log log = it.value();
					if (log.getBean() == null)
						continue;

					TableKey tkey = log.getBean().getTableKey();
					var record = AccessedRecords.get(tkey);
					if (record != null) {
						record.Dirty = true;
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
			for (var e : AccessedRecords.entrySet()) {
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
		final var ite = AccessedRecords.entrySet().iterator();
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
				if (e.getValue().Dirty && !curLock.isWriteLockHeld()) {
					// 必须先全部释放，再升级当前记录锁，再锁后面的记录。
					// 直接 unlockRead，lockWrite会死锁。
					n = _unlock_start_(index, n);
					// 从当前index之后都是新加锁，并且index和n都不会再发生变化。
					// 重新从当前 e 继续锁。
					continue;
				}
				// BUG 即使锁内。Record.Global.State 可能没有提升到需要水平。需要重新_check_。
				var r = _check_(e.getValue().Dirty, e.getValue());
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
					holdLocks.get(unlockEndIndex).ExitLock();
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
			holdLocks.get(i).ExitLock();
		holdLocks.subList(index, nLast).clear();
		return holdLocks.size();
	}

	public void ThrowAbort(String msg, Throwable cause) {
		if (State != TransactionState.Running)
			throw new IllegalStateException("Abort: State Is Not Running.");
		State = TransactionState.Abort;
		GoBackZeze.Throw(msg, cause);
	}

	public void ThrowRedoAndReleaseLock(String msg, Throwable cause) {
		if (State != TransactionState.Running)
			throw new IllegalStateException("RedoAndReleaseLock: State Is Not Running.");
		State = TransactionState.RedoAndReleaseLock;
		if (Macro.EnableStatistics) {
			//noinspection ConstantConditions
			ProcedureStatistics.getInstance().GetOrAdd(getTopProcedure().getActionName()).GetOrAdd(Procedure.RedoAndRelease).increment();
		}
		GoBackZeze.Throw(msg, cause);
	}

	public void ThrowRedo() {
		if (State != TransactionState.Running)
			throw new IllegalStateException("RedoAndReleaseLock: State Is Not Running.");
		State = TransactionState.Redo;
		GoBackZeze.Throw("Redo", null);
	}

	public void VerifyRunning() {
		if (State != TransactionState.Running)
			throw new IllegalStateException("State Is Not Running");
	}

	public void VerifyRunningOrCompleted() {
		if (State != TransactionState.Running && State != TransactionState.Completed)
			throw new IllegalStateException("State Is Not RunningOrCompleted");
	}
}
