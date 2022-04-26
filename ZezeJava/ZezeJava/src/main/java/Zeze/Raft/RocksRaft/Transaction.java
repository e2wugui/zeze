package Zeze.Raft.RocksRaft;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Supplier;
import Zeze.Net.Protocol;
import Zeze.Raft.RaftRetryException;
import Zeze.Raft.RocksRaft.Log1.LogBeanKey;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Action0;
import Zeze.Util.ThrowAgainException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Transaction {
	private static final Logger logger = LogManager.getLogger(Transaction.class);
	private static final ThreadLocal<Transaction> threadLocal = new ThreadLocal<>();

	public static Transaction Create() {
		var t = threadLocal.get();
		if (t == null)
			threadLocal.set(t = new Transaction());
		return t;
	}

	public static void Destroy() {
		threadLocal.set(null);
	}

	public static Transaction getCurrent() {
		return threadLocal.get();
	}

	public static final class RecordAccessed extends Bean {
		private final Record<?> Origin;
		private final long Timestamp;
		private boolean Dirty;
		private LogBeanKey<Bean> PutLog;

		public RecordAccessed(Record<?> origin) {
			Origin = origin;
			Timestamp = origin.getTimestamp();
		}

		public Record<?> getOrigin() {
			return Origin;
		}

		public long getTimestamp() {
			return Timestamp;
		}

		public boolean getDirty() {
			return Dirty;
		}

		public void setDirty(boolean value) {
			Dirty = value;
		}

		public LogBeanKey<Bean> getPutLog() {
			return PutLog;
		}

		public Bean NewestValue() {
			if (PutLog != null)
				return PutLog.Value;
			return Origin.getValue();
		}

		@Override
		public Bean CopyBean() {
			throw new UnsupportedOperationException();
		}

		public void Put(Transaction current, Bean value) {
			current.PutLog(PutLog = new LogBeanKey<>(Bean.class, this, 0, value));
		}

		public void Remove(Transaction current) {
			Put(current, null);
		}

		@Override
		protected void InitChildrenRootInfo(Record.RootInfo root) {
		}

		@Override
		public void Encode(ByteBuffer bb) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void Decode(ByteBuffer bb) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void FollowerApply(Log log) {
			throw new UnsupportedOperationException(); // Follower 不会到达这里。
		}

		@Override
		public void LeaderApplyNoRecursive(Log log) {
			// 在处理完 Log 以后，专门处理 PutLog 。see _final_commit_ & Record.LeaderApply
		}
	}

	private final TreeMap<TableKey, RecordAccessed> AccessedRecords = new TreeMap<>();
	private final ArrayList<Savepoint> Savepoints = new ArrayList<>();
	private final HashSet<PessimismLock> PessimismLocks = new HashSet<>();
	private Changes Changes;

	public Changes getChanges() {
		return Changes;
	}

	public <T extends PessimismLock> T AddPessimismLock(T pLock) {
		if (PessimismLocks.add(pLock))
			pLock.lock();
		return pLock;
	}

	public Log GetLog(long logKey) {
		return Savepoints.isEmpty() ? null : Savepoints.get(Savepoints.size() - 1).GetLog(logKey);
	}

	public void PutLog(Log log) {
		Savepoints.get(Savepoints.size() - 1).PutLog(log);
	}

	public Log LogGetOrAdd(long logKey, Supplier<Log> logFactory) {
		var log = GetLog(logKey);
		if (log == null)
			PutLog(log = logFactory.get());
		return log;
	}

	public void AddRecordAccessed(Record.RootInfo root, RecordAccessed r) {
		r.InitRootInfo(root, null);
		AccessedRecords.put(root.getTableKey(), r);
	}

	public RecordAccessed GetRecordAccessed(TableKey key) {
		return AccessedRecords.get(key);
	}

	public void Begin() {
		Savepoints.add(Savepoints.isEmpty() ? new Savepoint() : Savepoints.get(Savepoints.size() - 1).BeginSavepoint());
	}

	public void Commit() {
		int lastIndex = Savepoints.size() - 1;
		if (lastIndex > 0)
			Savepoints.get(lastIndex - 1).MergeFrom(Savepoints.remove(lastIndex), true); // 嵌套事务，把日志合并到上一层。
		// else // 最外层存储过程提交在 Perform 中处理
	}

	private List<Action0> LastRollbackActions;

	public void Rollback() {
		int lastIndex = Savepoints.size() - 1;
		Savepoint last = Savepoints.remove(lastIndex);
		last.Rollback();

		if (lastIndex > 0)
			Savepoints.get(lastIndex - 1).MergeFrom(last, false);
		else
			LastRollbackActions = last.RollbackActions; // 最后一个Savepoint Rollback的时候需要保存一下，用来触发回调。ugly。
	}

	public long Perform(Procedure procedure) throws Throwable {
		try {
			procedure.ResultCode = procedure.Call();
			if (_lock_and_check_(TransactionLevel.Serializable)) {
				if (procedure.ResultCode == 0)
					_final_commit_(procedure);
				else
					_final_rollback_(procedure);
				return procedure.ResultCode;
			}
			_final_rollback_(procedure); // 乐观锁，这里应该redo
			return procedure.ResultCode;
		} catch (ThrowAgainException e) {
			procedure.ResultCode = Zeze.Transaction.Procedure.Exception;
			_final_rollback_(procedure);
			throw e;
		} catch (RaftRetryException e) {
			procedure.ResultCode = Zeze.Transaction.Procedure.RaftRetry;
			logger.debug("RocksRaft Retry", e);
			_final_rollback_(procedure);
			return procedure.ResultCode;
		} catch (Throwable e) {
			procedure.ResultCode = Zeze.Transaction.Procedure.Exception;
			logger.error("RocksRaft Call Exception", e);
			if (e instanceof AssertionError) {
				_final_rollback_(procedure);
				throw e;
			}
			if (_lock_and_check_(TransactionLevel.Serializable)) {
				_final_rollback_(procedure);
				return procedure.ResultCode;
			}
			_final_rollback_(procedure); // 乐观锁，这里应该redo
			return procedure.ResultCode;
		} finally {
			for (var pLock : PessimismLocks)
				pLock.unlock();
			PessimismLocks.clear();
		}
	}

	public void LeaderApply(Changes changes) {
		Savepoint sp = Savepoints.get(Savepoints.size() - 1);
		for (var it = sp.getLogs().iterator(); it.moveToNext(); ) {
			var log = it.value();
			if (log.getBelong() != null)
				log.getBelong().LeaderApplyNoRecursive(log);
		}
		var rs = new ArrayList<Record<?>>();
		for (var ar : AccessedRecords.values()) {
			if (ar.Dirty) {
				ar.Origin.LeaderApply(ar);
				rs.add(ar.Origin);
			}
		}
		changes.getRocks().Flush(rs, changes);
	}

	public void RunWhileCommit(Action0 action) {
		Savepoints.get(Savepoints.size() - 1).CommitActions.add(action);
	}

	public void RunWhileRollback(Action0 action) {
		Savepoints.get(Savepoints.size() - 1).RollbackActions.add(action);
	}

	private boolean _lock_and_check_(@SuppressWarnings("SameParameterValue") TransactionLevel level) {
		boolean allRead = true;
		if (!Savepoints.isEmpty()) {
			for (var it = Savepoints.get(Savepoints.size() - 1).getLogs().iterator(); it.moveToNext(); ) {
				var log = it.value();
				// 特殊日志。不是 bean 的修改日志，当然也不会修改 Record。
				// 现在不会有这种情况，保留给未来扩展需要。
				if (log.getBelong() == null)
					continue;

				TableKey tkey = log.getBelong().getTableKey();
				var record = AccessedRecords.get(tkey);
				if (record != null) {
					record.setDirty(true);
					allRead = false;
				} else
					logger.fatal("impossible! record not found."); // 只有测试代码会把非 Managed 的 Bean 的日志加进来。
			}
		}
		//noinspection IfStatementWithIdenticalBranches
		if (allRead && level == TransactionLevel.AllowDirtyWhenAllRead)
			return true; // 使用一个新的enum表示一下？
		return true;
	}

	private void _final_commit_(Procedure procedure) {
		// Collect Changes
		Savepoint sp = Savepoints.get(Savepoints.size() - 1);
		Changes = new Changes(procedure.getRocks(), this, procedure.UniqueRequest);
		for (var it = sp.getLogs().iterator(); it.moveToNext(); ) {
			var log = it.value();
			// 这里都是修改操作的日志，没有Owner的日志是特殊测试目的加入的，简单忽略即可。
			if (log.getBelong() == null)
				continue;

			// 当changes.Collect在日志往上一级传递时调用，
			// 第一个参数Owner为null，表示bean属于record，到达root了。
			Changes.Collect(log.getBelong(), log);
		}

		for (var ar : AccessedRecords.values()) {
			if (ar.Dirty)
				Changes.CollectRecord(ar);
		}

		if (!Changes.getRecords().isEmpty()) { // has changes
			procedure.getRocks().UpdateAtomicLongs(Changes.getAtomicLongs());
			var resultBean = null != procedure.UniqueRequest ? procedure.UniqueRequest.getResultBean() : null;
			procedure.getRocks().getRaft().AppendLog(Changes, resultBean);
		}

		_trigger_commit_actions_(procedure, sp);

		Protocol<?> autoResponse = procedure.AutoResponse;
		if (autoResponse != null)
			autoResponse.SendResultCode(procedure.ResultCode);
	}

	private void _trigger_commit_actions_(Procedure procedure, Savepoint last) {
		for (var action : last.CommitActions) {
			try {
				action.run();
			} catch (Throwable ex) {
				logger.error(() -> "Commit Procedure " + procedure + " Action " + action.getClass().getName(), ex);
			}
		}
		last.CommitActions.clear();
	}

	private void _final_rollback_(Procedure procedure) {
		if (LastRollbackActions != null) {
			for (var action : LastRollbackActions) {
				try {
					action.run();
				} catch (Throwable ex) {
					logger.error(() -> "Commit Procedure " + procedure + " Action " + action.getClass().getName(), ex);
				}
			}
			LastRollbackActions = null;
		}
		Protocol<?> autoResponse = procedure.AutoResponse;
		if (autoResponse != null)
			autoResponse.SendResultCode(procedure.ResultCode);
	}
}
