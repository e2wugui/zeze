package Zeze.Raft.RocksRaft;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.HashMap;
import java.util.function.Supplier;
import Zeze.Net.Protocol;
import Zeze.Raft.RaftLog;
import Zeze.Raft.RaftRetryException;
import Zeze.Raft.RocksRaft.Log1.LogBeanKey;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Util.Action0;
import Zeze.Util.ThrowAgainException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/**
 * RocksRaft 事务：线程绑定的事务上下文——savepoint 日志栈、访问记录集与悲观锁，
 * 驱动存储过程的提交/回滚与 leader 侧 apply。
 */
public final class Transaction {
	private static final Logger logger = LogManager.getLogger(Transaction.class);
	private static final ThreadLocal<@Nullable Transaction> threadLocal = new ThreadLocal<>();

	public static Transaction create() {
		var t = threadLocal.get();
		if (t == null)
			threadLocal.set(t = new Transaction());
		return t;
	}

	public static void destroy() {
		threadLocal.remove();
	}

	public static @Nullable Transaction getCurrent() {
		return threadLocal.get();
	}

	public static final class RecordAccessed extends Bean {
		private final Record<?> origin;
		private final long timestamp;
		private boolean dirty;
		// put/remove 意图（LogBeanKey）仅存于所属事务的 savepoint 日志栈，嵌套回滚时自动回落。
		private final Transaction owner;

		public RecordAccessed(Transaction owner, Record<?> origin) {
			this.owner = owner;
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

		@SuppressWarnings("unchecked")
		public @Nullable LogBeanKey<Bean> getPutLog() {
			// 查询当前 savepoint 中 key 为 belong(this).objectId() + variableId(0) 的 put/remove 日志。
			var log = owner.getLog(objectId());
			return log instanceof LogBeanKey<?> putLog ? (LogBeanKey<Bean>)putLog : null;
		}

		public @Nullable Bean newestValue() {
			var putLog = getPutLog();
			if (putLog != null)
				return putLog.value;
			return origin.getValue();
		}

		@Override
		public Bean copy() {
			throw new UnsupportedOperationException();
		}

		public void put(Transaction current, @Nullable Bean value) {
			current.putLog(new LogBeanKey<>(Bean.class, this, 0, value));
		}

		public void remove(Transaction current) {
			put(current, null);
		}

		@Override
		protected void initChildrenRootInfo(Record.@Nullable RootInfo root) {
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

	// 与缓存及Changes使用相同的键身份；compareTo等价不代表物理键相同。
	private final HashMap<TableKey, RecordAccessed> accessedRecords = new HashMap<>();
	private final ArrayList<Savepoint> savepoints = new ArrayList<>();
	private final HashSet<PessimismLock> pessimismLocks = new HashSet<>();
	private @Nullable Rocks rocks;
	private @Nullable Changes changes;
	private @Nullable List<Action0> lastRollbackActions;

	void bindRocks(@Nullable Rocks instance) {
		if (instance == null)
			return;
		if (rocks != null && rocks != instance)
			throw new IllegalStateException("RocksRaft transaction cannot span multiple Rocks instances");
		rocks = instance;
	}

	boolean ownsRocks(Rocks instance) {
		return rocks == instance;
	}

	public @Nullable Changes getChanges() {
		return changes;
	}

	public <T extends PessimismLock> T addPessimismLock(T pLock) {
		if (pessimismLocks.add(pLock))
			pLock.lock();
		return pLock;
	}

	public @Nullable Log getLog(long logKey) {
		var saveSize = savepoints.size();
		return saveSize > 0 ? savepoints.get(saveSize - 1).getLog(logKey) : null;
	}

	/** 获取当前顶层 savepoint；栈空时抛带调用点定位的 {@link IllegalStateException}。 */
	private Savepoint lastSavepoint(String where) {
		if (savepoints.isEmpty())
			throw new IllegalStateException("RocksRaft Transaction savepoints empty at " + where
					+ " (unbalanced manual savepoint begin/rollback?)");
		return savepoints.getLast();
	}

	public void putLog(Log log) {
		lastSavepoint("putLog").putLog(log);
	}

	public Log logGetOrAdd(long logKey, Supplier<Log> logFactory) {
		var log = getLog(logKey);
		if (log == null)
			putLog(log = logFactory.get());
		return log;
	}

	public void addRecordAccessed(Record.RootInfo root, RecordAccessed r) {
		r.initRootInfo(root, null);
		accessedRecords.put(root.tableKey(), r);
	}

	public @Nullable RecordAccessed getRecordAccessed(TableKey key) {
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
			lastRollbackActions = last.getRollbackActions(); // 最后一个Savepoint Rollback的时候需要保存一下，用来触发回调。
	}

	public long perform(Procedure procedure) throws Exception {
		try {
			var rc = procedure.call();
			if (rc == 0) {
				markDirtyRecords();
				_final_commit_(procedure);
			} else {
				procedure.setAutoResponseResultCode(rc);
				_final_rollback_(procedure);
			}
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
		} catch (Throwable e) {
			procedure.setAutoResponseResultCode(Zeze.Transaction.Procedure.Exception);
			logger.error("RocksRaft Call Exception", e);
			_final_rollback_(procedure);
			if (e instanceof AssertionError)
				throw e;
			return Zeze.Transaction.Procedure.Exception;
		} finally {
			for (var pLock : pessimismLocks)
				pLock.unlock();
			pessimismLocks.clear();
			// 释放本事务访问记录的在用保护；已转入 pendingFlush 补偿的记录由补偿登记继续持有在用计数。
			for (var ar : accessedRecords.values())
				ar.getOrigin().endAccess();
		}
	}

	public void leaderApply(Changes changes, RaftLog holder) {
		var rocks = Objects.requireNonNull(changes.getRocks());
		try {
			leaderApplyInternal(changes, holder, rocks);
		} catch (Rocks.FlushException e) {
			// flush 失败由 pendingFlush 补偿重试，放行给 apply 重试。
			throw e;
		} catch (Throwable e) {
			// 非 flush 异常表示状态机分歧，对齐 followerApply 直接 fatalKill，防止同条目反复重抛卡死 lastApplied。
			//noinspection DataFlowIssue
			logger.fatal("{} leaderApply divergence, fatalKill. logIndex={} term={}",
					rocks.getRaft().getName(), holder.getIndex(), holder.getTerm(), e);
			rocks.getRaft().fatalKill();
		}
	}

	private void leaderApplyInternal(Changes changes, RaftLog holder, Rocks rocks) {
		var index = holder.getIndex();
		var pending = rocks.takePendingFlush(index, holder.getTerm());
		if (pending != null) {
			// 上次内存已应用但 flush 失败，仅重试 flush；不可重跑日志迭代（业务超时回滚后 savepoints 可能已清空）。
			try {
				rocks.flush(pending, changes, false, holder);
			} catch (Rocks.FlushException e) {
				// 重试再失败时转移补偿持有的在用计数（addReference=false），避免计数归零间隙被驱逐。
				rocks.putPendingFlush(index, holder.getTerm(), pending, false);
				throw e;
			}
			// 释放补偿登记持有的在用保护。
			for (var r : pending)
				r.endAccess();
			return;
		}
		var it = lastSavepoint("leaderApply").logIterator();
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
		try {
			rocks.flush(rs, changes, false, holder);
		} catch (Rocks.FlushException e) {
			// 落盘失败时登记已应用记录，后续重试仅 flush，避免重放造成双重应用。
			rocks.putPendingFlush(index, holder.getTerm(), rs);
			throw e;
		}
	}

	public void runWhileCommit(Action0 action) {
		lastSavepoint("runWhileCommit").addCommitAction(action);
	}

	public void runWhileRollback(Action0 action) {
		lastSavepoint("runWhileRollback").addRollbackAction(action);
	}

	/** 根据最外层 savepoint 的修改日志标记脏记录（RocksRaft 仅支持悲观模式，无乐观版本校验）。 */
	private void markDirtyRecords() {
		var saveSize = savepoints.size();
		if (saveSize > 0) {
			var it = savepoints.get(saveSize - 1).logIterator();
			if (it != null) {
				while (it.moveToNext()) {
					var log = it.value();
					// 无 belong 的特殊日志不关联 Record，直接跳过。
					if (log.getBelong() == null)
						continue;

					TableKey tkey = log.getBelong().tableKey();
					var record = accessedRecords.get(tkey);
					if (record != null) {
						record.setDirty(true);
					} else
						logger.error("impossible! record not found."); // 只有测试代码会把非 Managed 的 Bean 的日志加进来。
				}
			}
		}
	}

	private void _final_commit_(Procedure procedure) {
		Savepoint sp = lastSavepoint("_final_commit_");
		changes = new Changes(procedure.getRocks(), this, procedure.uniqueRequest);
		var it = sp.logIterator();
		if (it != null) {
			while (it.moveToNext()) {
				var log = it.value();
				// 没有 belong 的日志是特殊测试用途，直接忽略。
				if (log.getBelong() == null)
					continue;

				// 向上归集日志树，belong 为 null 表示已到达 record 根节点。
				changes.collect(Objects.requireNonNull(log.getBelong()), log);
			}
		}

		for (var ar : accessedRecords.values()) {
			if (ar.dirty)
				changes.collectRecord(ar);
		}

		if (!changes.getRecords().isEmpty()) {
			//noinspection DataFlowIssue
			procedure.getRocks().updateAtomicLongs(changes.getAtomicLongs());
			var resultBean = null != procedure.uniqueRequest ? procedure.uniqueRequest.getResultBean() : null;
			//noinspection DataFlowIssue
			procedure.getRocks().getRaft().appendLog(changes, resultBean, procedure.getExpectedTerm());
		}

		_trigger_commit_actions_(procedure, sp);

		Protocol<?> autoResponse = procedure.autoResponse;
		if (autoResponse != null) {
			try {
				autoResponse.SendResult();
			} catch (Throwable ex) {
				// appendLog 成功后应答发送失败仅记日志，不可上抛触发 _final_rollback_ 破坏提交/回滚互斥契约。
				logger.error("send auto response after commit fail", ex);
			}
		}
	}

	private static void _trigger_commit_actions_(Procedure procedure, Savepoint last) {
		var commitActions = last.getCommitActions();
		if (commitActions != null) {
			for (var action : commitActions) {
				try {
					action.run();
				} catch (Throwable ex) {
					logger.error("Commit Procedure {} Action {}", procedure, action.getClass().getName(), ex);
				}
			}
			commitActions.clear();
		}
	}

	private void _final_rollback_(Procedure procedure) {
		var rollbackActions = lastRollbackActions;
		if (rollbackActions == null) {
			// rc == 0 但 _final_commit_ 失败时不经过 rollback()，直接从最外层 savepoint 取出回滚动作执行。
			var saveSize = savepoints.size();
			if (saveSize > 0)
				rollbackActions = savepoints.get(saveSize - 1).getRollbackActions();
		}
		if (rollbackActions != null) {
			for (var action : rollbackActions) {
				try {
					action.run();
				} catch (Throwable ex) {
					logger.error("Commit Procedure {} Action {}", procedure, action.getClass().getName(), ex);
				}
			}
			lastRollbackActions = null;
		}
		Protocol<?> autoResponse = procedure.autoResponse;
		if (autoResponse != null) {
			try {
				autoResponse.SendResult();
			} catch (Throwable ex) {
				// 回滚已完成，应答发送失败不得把错误码返回路径改写成异常上抛。
				logger.error("send auto response after rollback fail", ex);
			}
		}
	}
}
