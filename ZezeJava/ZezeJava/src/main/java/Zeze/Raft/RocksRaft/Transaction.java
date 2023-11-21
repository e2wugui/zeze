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
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Action0;
import Zeze.Util.ThrowAgainException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Transaction {
	private static final Logger logger = LogManager.getLogger(Transaction.class);
	private static final ThreadLocal<Transaction> threadLocal = new ThreadLocal<>();

	public static Transaction create() {
		var t = threadLocal.get();
		if (t == null)
			threadLocal.set(t = new Transaction());
		return t;
	}

	public static void destroy() {
		threadLocal.remove();
	}

	public static Transaction getCurrent() {
		return threadLocal.get();
	}

	public static final class RecordAccessed extends Bean {
		private final Record<?> origin;
		private final long timestamp;
		private boolean dirty;
		private LogBeanKey<Bean> putLog;

		public RecordAccessed(Record<?> origin) {
			this.origin = origin;
			timestamp = origin.getTimestamp();
		}

		public Record<?> getOrigin() {
			return origin;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public boolean getDirty() {
			return dirty;
		}

		public void setDirty(boolean value) {
			dirty = value;
		}

		public LogBeanKey<Bean> getPutLog() {
			return putLog;
		}

		public Bean newestValue() {
			if (putLog != null)
				return putLog.value;
			return origin.getValue();
		}

		@Override
		public Bean copy() {
			throw new UnsupportedOperationException();
		}

		public void put(Transaction current, Bean value) {
			current.putLog(putLog = new LogBeanKey<>(Bean.class, this, 0, value));
		}

		public void remove(Transaction current) {
			put(current, null);
		}

		@Override
		protected void initChildrenRootInfo(Record.RootInfo root) {
		}

		@Override
		public void encode(ByteBuffer bb) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void decode(IByteBuffer bb) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void followerApply(Log log) {
			throw new UnsupportedOperationException(); // Follower 不会到达这里。
		}

		@Override
		public void leaderApplyNoRecursive(Log log) {
			// 在处理完 Log 以后，专门处理 PutLog 。see _final_commit_ & Record.LeaderApply
		}
	}

	private final TreeMap<TableKey, RecordAccessed> accessedRecords = new TreeMap<>();
	private final ArrayList<Savepoint> savepoints = new ArrayList<>();
	private final HashSet<PessimismLock> pessimismLocks = new HashSet<>();
	private Changes changes;
	private List<Action0> lastRollbackActions;

	public Changes getChanges() {
		return changes;
	}

	public <T extends PessimismLock> T addPessimismLock(T pLock) {
		if (pessimismLocks.add(pLock))
			pLock.lock();
		return pLock;
	}

	public Log getLog(long logKey) {
		var saveSize = savepoints.size();
		return saveSize > 0 ? savepoints.get(saveSize - 1).getLog(logKey) : null;
	}

	public void putLog(Log log) {
		savepoints.get(savepoints.size() - 1).putLog(log);
	}

	public Log logGetOrAdd(long logKey, Supplier<Log> logFactory) {
		var log = getLog(logKey);
		if (log == null)
			putLog(log = logFactory.get());
		return log;
	}

	public void addRecordAccessed(Record.RootInfo root, RecordAccessed r) {
		r.initRootInfo(root, null);
		accessedRecords.put(root.getTableKey(), r);
	}

	public RecordAccessed getRecordAccessed(TableKey key) {
		return accessedRecords.get(key);
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
		last.rollback();
		if (lastIndex > 0)
			savepoints.get(lastIndex - 1).mergeRollbackFrom(last); // 嵌套事务，把日志合并到上一层。
		else
			lastRollbackActions = last.getRollbackActions(); // 最后一个Savepoint Rollback的时候需要保存一下，用来触发回调。ugly。
	}

	public long perform(Procedure procedure) throws Exception {
		try {
			var rc = procedure.call();
			if (_lock_and_check_(TransactionLevel.Serializable)) {
				if (rc == 0) {
					_final_commit_(procedure);
				} else {
					procedure.setAutoResponseResultCode(rc);
					_final_rollback_(procedure);
				}
				return rc;
			}
			procedure.setAutoResponseResultCode(rc);
			_final_rollback_(procedure); // 乐观锁，这里应该redo
			return rc;
		} catch (ThrowAgainException e) {
			procedure.setAutoResponseResultCode(Zeze.Transaction.Procedure.Exception);
			_final_rollback_(procedure);
			throw e;
		} catch (RaftRetryException e) {
			procedure.setAutoResponseResultCode(Zeze.Transaction.Procedure.RaftRetry);
			logger.info("RocksRaft Retry", e);
			_final_rollback_(procedure);
			return Zeze.Transaction.Procedure.RaftRetry;
		} catch (Throwable e) { // // rollback. 必须捕捉所有异常。logger.error, rethrow AssertionError
			procedure.setAutoResponseResultCode(Zeze.Transaction.Procedure.Exception);
			logger.error("RocksRaft Call Exception", e);
			if (e instanceof AssertionError) {
				_final_rollback_(procedure);
				throw e;
			}
			if (_lock_and_check_(TransactionLevel.Serializable)) {
				_final_rollback_(procedure);
				return Zeze.Transaction.Procedure.Exception;
			}
			_final_rollback_(procedure); // 乐观锁，这里应该redo
			return Zeze.Transaction.Procedure.Exception;
		} finally {
			for (var pLock : pessimismLocks)
				pLock.unlock();
			pessimismLocks.clear();
		}
	}

	public void leaderApply(Changes changes) {
		var it = savepoints.get(savepoints.size() - 1).logIterator();
		if (it != null) {
			while (it.moveToNext()) {
				var log = it.value();
				if (log.getBelong() != null)
					log.getBelong().leaderApplyNoRecursive(log);
			}
		}
		var rs = new ArrayList<Record<?>>();
		for (var ar : accessedRecords.values()) {
			if (ar.dirty) {
				ar.origin.leaderApply(ar);
				rs.add(ar.origin);
			}
		}
		changes.getRocks().flush(rs, changes);
	}

	public void runWhileCommit(Action0 action) {
		savepoints.get(savepoints.size() - 1).addCommitAction(action);
	}

	public void runWhileRollback(Action0 action) {
		savepoints.get(savepoints.size() - 1).addRollbackAction(action);
	}

	private boolean _lock_and_check_(@SuppressWarnings("SameParameterValue") TransactionLevel level) {
		boolean allRead = true;
		var saveSize = savepoints.size();
		if (saveSize > 0) {
			var it = savepoints.get(saveSize - 1).logIterator();
			if (it != null) {
				while (it.moveToNext()) {
					var log = it.value();
					// 特殊日志。不是 bean 的修改日志，当然也不会修改 Record。
					// 现在不会有这种情况，保留给未来扩展需要。
					if (log.getBelong() == null)
						continue;

					TableKey tkey = log.getBelong().tableKey();
					var record = accessedRecords.get(tkey);
					if (record != null) {
						record.setDirty(true);
						allRead = false;
					} else
						logger.error("impossible! record not found."); // 只有测试代码会把非 Managed 的 Bean 的日志加进来。
				}
			}
		}
		//noinspection IfStatementWithIdenticalBranches
		if (allRead && level == TransactionLevel.AllowDirtyWhenAllRead)
			return true; // 使用一个新的enum表示一下？
		return true;
	}

	private void _final_commit_(Procedure procedure) {
		// Collect Changes
		Savepoint sp = savepoints.get(savepoints.size() - 1);
		changes = new Changes(procedure.getRocks(), this, procedure.uniqueRequest);
		var it = sp.logIterator();
		if (it != null) {
			while (it.moveToNext()) {
				var log = it.value();
				// 这里都是修改操作的日志，没有Owner的日志是特殊测试目的加入的，简单忽略即可。
				if (log.getBelong() == null)
					continue;

				// 当changes.Collect在日志往上一级传递时调用，
				// 第一个参数Owner为null，表示bean属于record，到达root了。
				changes.collect(log.getBelong(), log);
			}
		}

		for (var ar : accessedRecords.values()) {
			if (ar.dirty)
				changes.collectRecord(ar);
		}

		if (!changes.getRecords().isEmpty()) { // has changes
			procedure.getRocks().updateAtomicLongs(changes.getAtomicLongs());
			var resultBean = null != procedure.uniqueRequest ? procedure.uniqueRequest.getResultBean() : null;
			procedure.getRocks().getRaft().appendLog(changes, resultBean);
		}

		_trigger_commit_actions_(procedure, sp);

		Protocol<?> autoResponse = procedure.autoResponse;
		if (autoResponse != null)
			autoResponse.SendResult();
	}

	private static void _trigger_commit_actions_(Procedure procedure, Savepoint last) {
		var commitActions = last.getCommitActions();
		if (commitActions != null) {
			for (var action : commitActions) {
				try {
					action.run();
				} catch (Throwable ex) { // run handle. 必须捕捉所有异常。logger.error
					logger.error("Commit Procedure {} Action {}", procedure, action.getClass().getName(), ex);
				}
			}
			commitActions.clear();
		}
	}

	private void _final_rollback_(Procedure procedure) {
		var rollbackActions = lastRollbackActions;
		if (rollbackActions != null) {
			for (var action : rollbackActions) {
				try {
					action.run();
				} catch (Throwable ex) { // run handle. 必须捕捉所有异常。logger.error
					logger.error("Commit Procedure {} Action {}", procedure, action.getClass().getName(), ex);
				}
			}
			lastRollbackActions = null;
		}
		Protocol<?> autoResponse = procedure.autoResponse;
		if (autoResponse != null)
			autoResponse.SendResult();
	}
}
