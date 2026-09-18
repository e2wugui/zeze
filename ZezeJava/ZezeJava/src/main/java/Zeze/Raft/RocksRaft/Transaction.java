package Zeze.Raft.RocksRaft;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;
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
		// put/remove意图(LogBeanKey)的唯一事实源是所属事务的savepoint日志栈：
		// 嵌套回滚随savepoint丢弃，读取自动回落到外层已提交意图或origin值（FND4-24）。
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
		public LogBeanKey<Bean> getPutLog() {
			// 从当前（最外层未弹出的）savepoint查put意图：嵌套回滚后查不到即正确回落。
			// 查询键=LogBeanKey的belong(this).objectId()+variableId(0)（Log.getLogKey）。
			var log = owner.getLog(objectId());
			return log instanceof LogBeanKey<?> putLog ? (LogBeanKey<Bean>)putLog : null;
		}

		public Bean newestValue() {
			var putLog = getPutLog();
			if (putLog != null)
				return putLog.value;
			return origin.getValue();
		}

		@Override
		public Bean copy() {
			throw new UnsupportedOperationException();
		}

		public void put(Transaction current, Bean value) {
			current.putLog(new LogBeanKey<>(Bean.class, this, 0, value));
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

	/** 当前savepoint（FND5-15）：savepoint的begin/commit/rollback均为public，业务在
	 * process体内手动rollback多于begin后，putLog/leaderApply/_final_commit_等在空栈上
	 * getLast()抛NoSuchElementException——该异常不属于FlushException/RocksDBException，
	 * 不受followerApply的fatalKill兜底也不被tryApply捕获，沿tryCommit上抛到复制应答
	 * 线程，lastApplied楔死且无统一终止。防御性收口：空栈抛带上下文的
	 * IllegalStateException，业务误用从“apply线程未受控异常”变为带定位的明确失败；
	 * 正常配对路径（Procedure.call严格配对）零变化。 */
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
			if (_lock_and_check_(/*TransactionLevel.Serializable*/)) {
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
			if (_lock_and_check_(/*TransactionLevel.Serializable*/)) {
				_final_rollback_(procedure);
				return Zeze.Transaction.Procedure.Exception;
			}
			_final_rollback_(procedure); // 乐观锁，这里应该redo
			return Zeze.Transaction.Procedure.Exception;
		} finally {
			for (var pLock : pessimismLocks)
				pLock.unlock();
			pessimismLocks.clear();
				// 【FND7-14】释放本事务访问记录的在用保护（驱逐允许）。正常路径leaderApply在
				// appendLog等待期间（perform内）已完成应用与flush，此时释放安全；appendLog超时
				// 未决的条目若已进入flush补偿（flush失败），由putPendingFlush登记继续持有在用，
				// 直到重试成功或过期丢弃，迟到flush窗口内记录不可被驱逐。
				for (var ar : accessedRecords.values())
					ar.getOrigin().endAccess();
		}
	}

	public void leaderApply(Changes changes, RaftLog holder) {
		var rocks = changes.getRocks();
		try {
			leaderApplyInternal(changes, holder, rocks);
		} catch (Rocks.FlushException e) {
			// flush失败有补偿重试通道（pendingFlush，FND-R2-4），不是结构性分歧，放行给apply重试。
			throw e;
		} catch (Throwable e) {
			// 【FND7-15】对齐Rocks.followerApply的"宁死不糊"：leaderApply链路抛出非Flush
			// 异常（生成leaderApplyNoRecursive的ClassCast/NPE、lastSavepoint的
			// IllegalStateException等）时，后台apply线程的uncaughtHandler仅记日志，
			// applyFuture在finally置null后同条目反复重入重抛——lastApplied永久楔死且
			// 无fatalKill，leader持续复制提交并对外提供停在楔死点的过期读（静默落后），
			// 换主后需要InstallSnapshot追赶。与follower路径对称：fatalKill把静默分歧
			// 变成显性crash。FlushException的pendingFlush重试语义不变。
			logger.fatal("{} leaderApply divergence, fatalKill. logIndex={} term={}",
					rocks.getRaft().getName(), holder.getIndex(), holder.getTerm(), e);
			rocks.getRaft().fatalKill();
		}
	}

	private void leaderApplyInternal(Changes changes, RaftLog holder, Rocks rocks) {
		var index = holder.getIndex();
		var pending = rocks.takePendingFlush(index, holder.getTerm());
		if (pending != null) {
			// 上次leaderApply已完成内存变更但flush失败（FND-R2-4）：内存已是最终状态，
			// 只重试flush。不能重跑下面的日志迭代：业务线程超时回滚后savepoints可能已清空。
			try {
				rocks.flush(pending, changes);
			} catch (Rocks.FlushException e) {
				// 【FND8-39】重试再失败的重登记用转移语义（addReference=false）：补偿持有的
				// 计数随消费原样移入新登记，在用保护横跨补偿生命周期不断档；原"先endAccess
				// 再登记"的归零间隙内LRU驱逐+同key脏重载旧基线，增量日志应用在旧值上即分歧。
				rocks.putPendingFlush(index, holder.getTerm(), pending, false);
				throw e;
			}
			// 【FND7-14】重试flush成功：释放补偿登记持有的在用保护（业务计数若未随
			// perform释放，由其finally释放）。
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
			rocks.flush(rs, changes);
		} catch (Rocks.FlushException e) {
			// 内存已变更但落盘失败：记录已应用的记录集合，等下次apply重试时只flush，
			// 避免重试经readLog解码走增量followerApply造成双重应用（FND-R2-4）。
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

	@SuppressWarnings("SameReturnValue")
	private boolean _lock_and_check_(/*TransactionLevel level*/) {
//		boolean allRead = true;
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
//						allRead = false;
					} else
						logger.error("impossible! record not found."); // 只有测试代码会把非 Managed 的 Bean 的日志加进来。
				}
			}
		}
//		if (allRead && level == TransactionLevel.AllowDirtyWhenAllRead)
//			return true; // 使用一个新的enum表示一下？
		return true;
	}

	private void _final_commit_(Procedure procedure) {
		// Collect Changes
		Savepoint sp = lastSavepoint("_final_commit_");
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
		if (autoResponse != null) {
			try {
				autoResponse.SendResult();
			} catch (Throwable ex) {
				// 决策点（appendLog成功）之后的应答发送失败只降级应答质量（FND4-32）：
				// 传播出去会让perform的catch在已提交事务上补跑_final_rollback_，
				// 提交/回滚互斥的回调契约被破坏（提交动作已执行又叠回滚动作）。
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
				} catch (Throwable ex) { // run handle. 必须捕捉所有异常。logger.error
					logger.error("Commit Procedure {} Action {}", procedure, action.getClass().getName(), ex);
				}
			}
			commitActions.clear();
		}
	}

	private void _final_rollback_(Procedure procedure) {
		var rollbackActions = lastRollbackActions;
		if (rollbackActions == null) {
			// 过程成功返回（rc==0）但 _final_commit_ 失败（raft appendLog 抛 RaftRetry 等）
			// 或乐观检查失败的路径不经过 rollback()，lastRollbackActions 不会被赋值；
			// 此时最外层savepoint仍完整保留（_final_commit_ 不弹出），直接取其回滚动作执行，
			// 维持"未提交则已注册的回滚动作必执行"。
			var saveSize = savepoints.size();
			if (saveSize > 0)
				rollbackActions = savepoints.get(saveSize - 1).getRollbackActions();
		}
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
		if (autoResponse != null) {
			try {
				autoResponse.SendResult();
			} catch (Throwable ex) {
				// 回滚已完成，应答发送失败不得把错误码返回路径改写成异常上抛（FND4-32同源）。
				logger.error("send auto response after rollback fail", ex);
			}
		}
	}
}
