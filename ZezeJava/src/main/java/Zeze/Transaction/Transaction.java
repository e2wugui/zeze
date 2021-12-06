package Zeze.Transaction;

import Zeze.Services.*;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Transaction {
	private static final Logger logger = LogManager.getLogger(Transaction.class);

	private final static ThreadLocal<Transaction> threadLocal = new ThreadLocal<>();

	public static Transaction getCurrent() {
		var t = threadLocal.get();
		if (null == t)
			return null;
		return t.Created ? t : null;
	}

	// 嵌套存储过程栈。
	private final ArrayList<Procedure> ProcedureStack = new ArrayList<> ();
	public ArrayList<Procedure> getProcedureStack() {
		return ProcedureStack;
	}

	public Procedure getTopProcedure() {
		return getProcedureStack().isEmpty() ? null : getProcedureStack().get(getProcedureStack().size() - 1);
	}

	private boolean Created = true;

	private void ReuseTransaction()
	{
		this.Created = false;

		this.AccessedRecords.clear();
		this.CommitActions.clear();
		//this.holdLocks.Clear(); // 执行完肯定清理了。
		this.State = TransactionState.Running;
		this.ProcedureStack.clear();
		this.RollbackActions.clear();
		this.Savepoints.clear();
	}

	private Locks Locks;

	public static Transaction Create(Locks locks) {
		var t = threadLocal.get();
		if (null == t) {
			t = new Transaction();
			t.Locks = locks;
			threadLocal.set(t);
			return t;
		}
		t.Locks = locks;
		t.Created = true;
		return t;
	}

	public static void Destroy() {
		threadLocal.get().ReuseTransaction();
	}

	public void Begin() {
		Savepoint sp = !Savepoints.isEmpty() ? Savepoints.get(Savepoints.size() - 1).Duplicate() : new Savepoint();
		Savepoints.add(sp);
	}

	public void Commit() {
		if (Savepoints.size() > 1) {
			// 嵌套事务，把日志合并到上一层。
			int lastIndex = Savepoints.size() - 1;
			Savepoint last = Savepoints.get(lastIndex);
			Savepoints.remove(lastIndex);
			Savepoints.get(Savepoints.size() - 1).Merge(last);
		}
		// else
		    // 最外层存储过程提交在 Perform 中处理
	}

	public void Rollback() {
		int lastIndex = Savepoints.size() - 1;
		Savepoint last = Savepoints.get(lastIndex);
		Savepoints.remove(lastIndex);
		last.Rollback();
	}

	public Log GetLog(long key) {
		VerifyRunningOrCompleted();
		// 允许没有 savepoint 时返回 null. 就是说允许在保存点不存在时进行读取操作。
		return !Savepoints.isEmpty() ? Savepoints.get(Savepoints.size() - 1).GetLog(key) : null;
	}

	public void PutLog(Log log) {
		VerifyRunning();
		Savepoints.get(Savepoints.size() - 1).PutLog(log);
	}

	public ChangeNote GetOrAddChangeNote(long key, Zeze.Util.Factory<ChangeNote> factory) {
		// 必须存在 Savepoint. 可能是为了修改。
		return Savepoints.get(Savepoints.size() - 1).GetOrAddChangeNote(key, factory);
	}

	/*
	public void PutChangeNote(long key, ChangeNote note)
	{
	    savepoints[~1].PutChangeNote(key, note);
	}
	*/

	private final ArrayList<Runnable> CommitActions = new ArrayList<>();
	private final ArrayList<Runnable> RollbackActions = new ArrayList<>();

	public void RunWhileCommit(Runnable action) {
		VerifyRunning();
		CommitActions.add(action);
	}

	public void RunWhileRollback(Runnable action) {
		VerifyRunning();
		RollbackActions.add(action);
	}

	TableKey LastTableKeyOfRedoAndRelease;
	long LastGlobalSerialIdOfRedoAndRelease;
	private boolean AlwaysReleaseLockWhenRedo = false;
	void SetAlwaysReleaseLockWhenRedo() {
		AlwaysReleaseLockWhenRedo = true;
		if (holdLocks.size() > 0)
			this.ThrowRedo();
	}

	/**
	 Procedure 第一层入口，总的处理流程，包括重做和所有错误处理。
	 
	 @param procedure first procedure
	*/
	public long Perform(Procedure procedure) throws Throwable {
		try {
			for (int tryCount = 0; tryCount < 256; ++tryCount) { // 最多尝试次数
				// 默认在锁内重复尝试，除非CheckResult.RedoAndReleaseLock，否则由于CheckResult.Redo保持锁会导致死锁。
				procedure.getZeze().getCheckpoint().EnterFlushReadLock();
				try {
					for (; tryCount < 256; ++tryCount) { // 最多尝试次数
						CheckResult checkResult = CheckResult.Redo; // 用来决定是否释放锁，除非 _lock_and_check_ 明确返回需要释放锁，否则都不释放。
						try {
							var result = procedure.Call();
							switch (State) {
								case Running:
									if ((result == Procedure.Success && Savepoints.size() != 1) || (result != Procedure.Success && !Savepoints.isEmpty())) {
										// 这个错误不应该重做
										logger.fatal("Transaction.Perform:{}. savepoints.Count != 1.", procedure);
										_final_rollback_(procedure);
										return Procedure.ErrorSavepoint;
									}
									checkResult = _lock_and_check_(procedure.getTransactionLevel());
									if (checkResult == CheckResult.Success) {
										if (result == Procedure.Success) {
											_final_commit_(procedure);
											// 正常一次成功的不统计，用来观察redo多不多。
											// 失败在 Procedure.cs 中的统计。
											if (tryCount > 0) {
												ProcedureStatistics.getInstance().GetOrAdd("Zeze.Transaction.TryCount").GetOrAdd(tryCount).incrementAndGet();
											}
											return Procedure.Success;
										}
										_final_rollback_(procedure);
										return result;
									}
									break; // retry

								case Abort:
									logger.debug("Transaction.Perform: Abort");
									_final_rollback_(procedure);
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
						}
						catch (Throwable e) {
							// Procedure.Call 里面已经处理了异常。只有 unit test 或者重做或者内部错误会到达这里。
							// 在 unit test 下，异常日志会被记录两次。
							switch (State) {
								case Running:
									logger.error("Transaction.Perform:{} exception. run count:{}", procedure, tryCount, e);
									if (!Savepoints.isEmpty()) {
										// 这个错误不应该重做
										logger.fatal("Transaction.Perform:{}. exception. savepoints.Count != 0.", procedure, e);
										_final_rollback_(procedure);
										return Procedure.ErrorSavepoint;
									}
									// 对于 unit test 的异常特殊处理，与unit test框架能搭配工作
									if (e instanceof AssertionError) {
										_final_rollback_(procedure);
										throw e;
									}
									checkResult = _lock_and_check_(TransactionLevel.Serializable);
									if (checkResult == CheckResult.Success) {
										_final_rollback_(procedure);
										return Procedure.Excption;
									}
									// retry
									break;

								case Abort:
									logger.debug("Transaction.Perform: Abort");
									_final_rollback_(procedure);
									return Procedure.AbortException;

								case Redo:
									checkResult = CheckResult.Redo;
									break;

								case RedoAndReleaseLock:
									checkResult = CheckResult.RedoAndReleaseLock;
									break;
							}
							// retry
						}
						finally {
							if (checkResult == CheckResult.RedoAndReleaseLock) {
								for (var holdLock : holdLocks) {
									holdLock.ExitLock();
								}
								holdLocks.clear();
							}
							// retry 可能保持已有的锁，清除记录和保存点。
							AccessedRecords.clear();
							Savepoints.clear();
							CommitActions.clear();
							RollbackActions.clear();

							State = TransactionState.Running; // prepare to retry
						}

						if (checkResult == CheckResult.RedoAndReleaseLock) {
							//logger.Debug("CheckResult.RedoAndReleaseLock break {0}", procedure);
							break;
						}
					}
				}
				finally {
					procedure.getZeze().getCheckpoint().ExitFlushReadLock();
				}
				//logger.Debug("Checkpoint.WaitRun {0}", procedure);
				procedure.getZeze().__TryWaitFlushWhenReduce(LastTableKeyOfRedoAndRelease, LastGlobalSerialIdOfRedoAndRelease);
			}
			logger.error("Transaction.Perform:{}. too many try.", procedure);
			_final_rollback_(procedure);
			return Procedure.TooManyTry;
		}
		finally {
			for (var holdLock : holdLocks) {
				holdLock.ExitLock();
			}
			holdLocks.clear();
		}
	}

	private void _notify_listener_(ChangeCollector cc) {
		try {
			Savepoint sp = Savepoints.get(Savepoints.size() - 1);
			for (Log log : sp.getLogs().values()) {
				if (log.getBean() == null) {
					continue; // 特殊日志没有Bean。
				}

				// 写成回调是为了优化，仅在需要的时候才创建path。
				cc.CollectChanged(log.getBean().getTableKey(),
					() -> {
						var pn = new ChangePathAndNote();
						pn.path = new ArrayList<>();
						pn.note = null;
						pn.path.add(Zeze.Util.KV.Create(log.getBean(), log.getVariableId()));
						log.getBean().BuildChangeListenerPath(pn.path);
						return pn;
					});
			}
			for (ChangeNote cn : sp.getChangeNotes().values()) {
				if (cn.getBean() == null) {
					continue;
				}

				// 写成回调是为了优化，仅在需要的时候才创建path。
				cc.CollectChanged(cn.getBean().getTableKey(),
						() -> {
							var pn = new ChangePathAndNote();
							pn.path = new ArrayList<>();
							pn.note = cn;
							pn.path.add(Zeze.Util.KV.Create(cn.getBean().getParent(), cn.getBean().getVariableId()));
							cn.getBean().getParent().BuildChangeListenerPath(pn.path);
							return pn;
				});
			}

			Savepoints.clear();
			//accessedRecords.Clear(); // 事务内访问过的记录保留，这样在Listener中可以读取。

			cc.Notify();
		}
		catch (Throwable ex) {
			logger.error("ChangeListener Collect And Notify", ex);
		}
	}

	private void _trigger_commit_actions_(Procedure procedure) {
		for (var action : CommitActions) {
			try {
				action.run();
			}
			catch (Throwable e) {
				logger.error("Commit Procedure {} Action {}", procedure, action.getClass().getName(), e);
			}
		}
		CommitActions.clear();
	}

	private void _final_commit_(Procedure procedure) {
		// 下面不允许失败了，因为最终提交失败，数据可能不一致，而且没法恢复。
		// 可以在最终提交里可以实现每事务checkpoint。
		ChangeCollector cc = new ChangeCollector();

		RelativeRecordSet.TryUpdateAndCheckpoint(this, procedure, () -> {
				try {
					Savepoints.get(Savepoints.size() - 1).Commit();
					for (var e : getAccessedRecords().entrySet()) {
						if (e.getValue().Dirty) {
							e.getValue().OriginRecord.Commit(e.getValue());
							cc.BuildCollect(procedure.getZeze(), e.getKey(), e.getValue()); // 首先对脏记录创建Table,Record相关Collector。
						}
					}
				}
				catch (Throwable e) {
					logger.error("Transaction._final_commit_ {}", procedure, e);
					Runtime.getRuntime().halt(54321);
				}
		});

		// 禁止在listener回调中访问表格的操作。除了回调参数中给定的记录可以访问。
		// 不再支持在回调中再次执行事务。
		// 在Notify之前设置的。
		State = TransactionState.Completed;
		_notify_listener_(cc);
		_trigger_commit_actions_(procedure);
	}

	private void _final_rollback_(Procedure procedure) {
		State = TransactionState.Completed;
		for (var action : RollbackActions) {
			try {
				action.run();
			}
			catch (Throwable e) {
				logger.error("Rollback Procedure {0} Action {1} {2}", procedure, action.getClass().getName(), e);
			}
		}
		RollbackActions.clear();
	}

	private final ArrayList<Lockey> holdLocks = new ArrayList<>(); // 读写锁的话需要一个包装类，用来记录当前维持的是哪个锁。

	private final TreeMap<TableKey, RecordAccessed> AccessedRecords = new TreeMap<> ();
	public TreeMap<TableKey, RecordAccessed> getAccessedRecords() {
		return AccessedRecords;
	}
	private final ArrayList<Savepoint> Savepoints = new ArrayList<>();

	/** 
	 只能添加一次。
	 @param r record accessed
	*/
	void AddRecordAccessed(Record.RootInfo root, RecordAccessed r) {
		VerifyRunning();
		r.InitRootInfo(root, null);
		getAccessedRecords().put(root.getTableKey(), r);
	}

	public RecordAccessed GetRecordAccessed(TableKey key) {
		// 允许读取事务内访问过的记录。
		VerifyRunningOrCompleted();
		return getAccessedRecords().get(key);
	}


	public void VerifyRecordAccessed(Bean bean) {
		VerifyRecordAccessed(bean, false);
	}

	public void VerifyRecordAccessed(Bean bean, boolean IsRead) {
		//if (IsRead)// && App.Config.AllowReadWhenRecoredNotAccessed)
		//    return;
		if (bean.RootInfo.getRecord().getState() == GlobalCacheManagerServer.StateRemoved) {
			throw new RuntimeException("VerifyRecordAccessed: Record Has Bean Removed From Cache. " + bean.getTableKey());
		}
		var ra = GetRecordAccessed(bean.getTableKey());
		if (ra == null) {
			throw new RuntimeException("VerifyRecordAccessed: Record Not Control Under Current Transastion. " + bean.getTableKey());
		}
		if (bean.RootInfo.getRecord() != ra.OriginRecord) {
			throw new RuntimeException("VerifyRecordAccessed: Record Reloaded. " + bean.getTableKey());
		}
		// 事务结束后可能会触发Listener，此时Commit已经完成，Timestamp已经改变，
		// 这种情况下不做RedoCheck，当然Listener的访问数据是只读的。
		if (ra.OriginRecord.getTable().getZeze().getConfig().getFastRedoWhenConfict()
				&& State != TransactionState.Completed
				&& ra.OriginRecord.getTimestamp() != ra.Timestamp) {
			ThrowRedo();
		}
	}

	private enum CheckResult {
		Success,
		Redo,
		RedoAndReleaseLock
	}

	private CheckResult _check_(boolean writeLock, RecordAccessed e) throws Throwable {
		e.OriginRecord.EnterFairLock();
		try {
			if (writeLock) {
				switch (e.OriginRecord.getState()) {
					case GlobalCacheManagerServer.StateRemoved:
						// fall down
					case GlobalCacheManagerServer.StateInvalid:
						LastTableKeyOfRedoAndRelease = e.getTableKey();
						LastGlobalSerialIdOfRedoAndRelease = e.OriginRecord.LastErrorGlobalSerialId;
						return CheckResult.RedoAndReleaseLock; // 写锁发现Invalid，可能有Reduce请求。

					case GlobalCacheManagerServer.StateModify:
						return e.Timestamp != e.OriginRecord.getTimestamp() ? CheckResult.Redo : CheckResult.Success;

					case GlobalCacheManagerServer.StateShare:
						// 这里可能死锁：另一个先获得提升的请求要求本机Recude，但是本机Checkpoint无法进行下去，被当前事务挡住了。
						// 通过 GlobalCacheManager 检查死锁，返回失败;需要重做并释放锁。
						var acquire = e.OriginRecord.Acquire(GlobalCacheManagerServer.StateModify);
						if (acquire.Result.State != GlobalCacheManagerServer.StateModify) {
							logger.debug("Acquire Faild. Maybe DeadLock Found {}", e.OriginRecord);
							e.OriginRecord.setState(GlobalCacheManagerServer.StateInvalid); // 这里保留StateShare更好吗？
							LastTableKeyOfRedoAndRelease = e.getTableKey();
							e.OriginRecord.LastErrorGlobalSerialId = acquire.Result.GlobalSerialId; // save
							LastGlobalSerialIdOfRedoAndRelease = acquire.Result.GlobalSerialId;
							return CheckResult.RedoAndReleaseLock;
						}
						e.OriginRecord.setState(GlobalCacheManagerServer.StateModify);
						return e.Timestamp != e.OriginRecord.getTimestamp() ? CheckResult.Redo : CheckResult.Success;
				}
				return e.Timestamp != e.OriginRecord.getTimestamp() ? CheckResult.Redo : CheckResult.Success; // imposible
			}
			else {
				if (e.OriginRecord.getState() == GlobalCacheManagerServer.StateInvalid
						|| e.OriginRecord.getState() == GlobalCacheManagerServer.StateRemoved) {
					LastTableKeyOfRedoAndRelease = e.getTableKey();
					LastGlobalSerialIdOfRedoAndRelease = e.OriginRecord.LastErrorGlobalSerialId;
					return CheckResult.RedoAndReleaseLock; // 发现Invalid，可能有Reduce请求或者被Cache清理，此时保险起见释放锁。
				}
				return e.Timestamp != e.OriginRecord.getTimestamp() ? CheckResult.Redo : CheckResult.Success;
			}
		} finally {
			e.OriginRecord.ExitFairLock();
		}
	}

	private CheckResult _lock_and_check_(Map.Entry<TableKey, RecordAccessed> e) throws Throwable {
		Lockey lockey = Locks.Get(e.getKey());
		boolean writeLock = e.getValue().Dirty;
		lockey.EnterLock(writeLock);
		holdLocks.add(lockey);
		return _check_(writeLock, e.getValue());
	}

	private CheckResult _lock_and_check_(TransactionLevel level) throws Throwable {
		boolean allRead = true;
		if (!Savepoints.isEmpty()) {
			// 全部 Rollback 时 Count 为 0；最后提交时 Count 必须为 1；
			// 其他情况属于Begin,Commit,Rollback不匹配。外面检查。
			for (var log : Savepoints.get(Savepoints.size() - 1).getLogs().values()) {
				// 特殊日志。不是 bean 的修改日志，当然也不会修改 Record。
				// 现在不会有这种情况，保留给未来扩展需要。
				if (log.getBean() == null) {
					continue;
				}

				TableKey tkey = log.getBean().getTableKey();
				var record = AccessedRecords.get(tkey);
				if (null != record) {
					record.Dirty = true;
					allRead = false;
				} else {
					// 只有测试代码会把非 Managed 的 Bean 的日志加进来。
					logger.fatal("impossible! record not found.");
				}
			}
		}

		if (allRead && level == TransactionLevel.AllowDirtyWhenAllRead)
			return CheckResult.Success; // 使用一个新的enum表示一下？

		boolean conflict = false; // 冲突了，也继续加锁，为重做做准备！！！
		if (holdLocks.isEmpty()) {
			for (var e : getAccessedRecords().entrySet()) {
				switch (_lock_and_check_(e)) {
					case Success:
						break;
					case Redo:
						conflict = true;
						break; // continue lock
					case RedoAndReleaseLock:
						return CheckResult.RedoAndReleaseLock;
				}
			}
			return conflict ? CheckResult.Redo : CheckResult.Success;
		}

		int index = 0;
		int n = holdLocks.size();
		final var ite = getAccessedRecords().entrySet().iterator();
		var e = ite.hasNext() ? ite.next() : null;
		while (null != e) {
			// 如果 holdLocks 全部被对比完毕，直接锁定它
			if (index >= n) {
				switch (_lock_and_check_(e)) {
					case Success:
						break;
					case Redo:
						conflict = true;
						break; // continue lock
					case RedoAndReleaseLock:
						return CheckResult.RedoAndReleaseLock;
				}
				e = ite.hasNext() ? ite.next() : null;
				continue;
			}

			Lockey curLock = holdLocks.get(index);
			int c = curLock.getTableKey().compareTo(e.getKey());

			// holdlocks a  b  ...
			// needlocks a  b  ...
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
				// else 已经持有读锁，不可能被修改也不可能降级(reduce)，所以不做检测了。                    
				// 已经锁定了，跳过当前锁，比较下一个。
				++index;
				e = ite.hasNext() ? ite.next() : null;
				continue;
			}
			// holdlocks a  b  ...
			// needlocks a  c  ...
			if (c < 0) {
				// 释放掉 比当前锁序小的锁，因为当前事务中不再需要这些锁
				int unlockEndIndex = index;
				for (; unlockEndIndex < n && holdLocks.get(unlockEndIndex).getTableKey().compareTo(e.getKey()) < 0; ++unlockEndIndex) {
					var toUnlockLocker = holdLocks.get(unlockEndIndex);
					toUnlockLocker.ExitLock();
				}
				holdLocks.subList(index, unlockEndIndex).clear();
				n = holdLocks.size();
				// 重新从当前 e 继续锁。
				continue;
			}

			// holdlocks a  c  ...
			// needlocks a  b  ...
			// 为了不违背锁序，释放从当前锁开始的所有锁
			n = _unlock_start_(index, n);
			// 重新从当前 e 继续锁。
		}
		return conflict ? CheckResult.Redo : CheckResult.Success;
	}

	private int _unlock_start_(int index, int nLast) {
		for (int i = index; i < nLast; ++i) {
			var toUnlockLocker = holdLocks.get(i);
			toUnlockLocker.ExitLock();
		}
		holdLocks.subList(index, nLast).clear();
		return holdLocks.size();
	}

	private TransactionState State = TransactionState.Running;

	public TransactionState getState() {
		return State;
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
		ProcedureStatistics.getInstance().GetOrAdd(getTopProcedure().getActionName()).GetOrAdd(Procedure.RedoAndRelease).incrementAndGet();
		GoBackZeze.Throw(msg, cause);
	}

	public void ThrowRedo() {
		if (State != TransactionState.Running)
			throw new IllegalStateException("RedoAndReleaseLock: State Is Not Running.");
		State = TransactionState.Redo;
		GoBackZeze.Throw("Redo", null);
	}

	public void VerifyRunning() {
		switch (State) {
			case Running:
				return;
			default:
				throw new IllegalStateException("State Is Not Running");
		}
	}

	public void VerifyRunningOrCompleted() {
		switch (State) {
			case Running: case Completed:
				return;
			default:
				throw new IllegalStateException("State Is Not RunningOrCompleted");
		}
	}
}