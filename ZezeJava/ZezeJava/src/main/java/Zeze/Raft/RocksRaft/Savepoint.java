package Zeze.Raft.RocksRaft;

import java.util.ArrayList;
import Zeze.Util.Action0;
import Zeze.Util.LongHashMap;

public final class Savepoint {
	private LongHashMap<Log> logs;
	// private final LongHashMap<Log> newly = new LongHashMap<Log>(); // 当前Savepoint新加的，用来实现Rollback，先不实现。
	private ArrayList<Action0> commitActions;
	private ArrayList<Action0> rollbackActions;

	ArrayList<Action0> getCommitActions() {
		return commitActions;
	}

	ArrayList<Action0> getRollbackActions() {
		return rollbackActions;
	}

	public LongHashMap<Log>.Iterator logIterator() {
		var logs = this.logs;
		return logs != null ? logs.iterator() : null;
	}

	public Log getLog(long logKey) {
		var logs = this.logs;
		return logs != null ? logs.get(logKey) : null;
	}

	public void putLog(Log log) {
		var logs = this.logs;
		if (logs == null)
			this.logs = logs = new LongHashMap<>();
		logs.put(log.getLogKey(), log);
		// newly.put(log.getLogKey(), log);
	}

	public Savepoint beginSavepoint() {
		var sp = new Savepoint();
		var logs = this.logs;
		if (logs != null) {
			var newLogs = new LongHashMap<>(logs);
			newLogs.foreachUpdate((__, v) -> v.beginSavepoint());
			sp.logs = newLogs;
		}
		return sp;
	}

	private ArrayList<Action0> getCommitActionsForAdd() {
		var actions = commitActions;
		if (actions == null)
			commitActions = actions = new ArrayList<>();
		return actions;
	}

	private ArrayList<Action0> getRollbackActionsForAdd() {
		var actions = rollbackActions;
		if (actions == null)
			rollbackActions = actions = new ArrayList<>();
		return actions;
	}

	public void addCommitAction(Action0 action) {
		getCommitActionsForAdd().add(action);
	}

	public void addRollbackAction(Action0 action) {
		getRollbackActionsForAdd().add(action);
	}

	public void mergeCommitFrom(Savepoint next) {
		var nextLogs = next.logs;
		if (nextLogs != null)
			nextLogs.foreachValue(log -> log.endSavepoint(this));
		var nextCommitActions = next.commitActions;
		if (nextCommitActions != null)
			getCommitActionsForAdd().addAll(nextCommitActions);
		var nextRollbackActions = next.rollbackActions;
		if (nextRollbackActions != null)
			getRollbackActionsForAdd().addAll(nextRollbackActions);
	}

	public void mergeRollbackFrom(Savepoint next) {
		var nextRollbackActions = next.rollbackActions;
		if (nextRollbackActions != null) {
			getCommitActionsForAdd().addAll(nextRollbackActions);
			getRollbackActionsForAdd().addAll(nextRollbackActions);
		}
	}

	/**
	 * 撤销当前 savepoint：【契约】apply 期内存态不可事务性回退。当前实现只丢弃日志栈
	 * （putLog 收集的修改日志随 savepoint 弹出而不可见）并触发回滚回调，过程体内对
	 * 缓存 Record 的 bean 的原位修改不会被还原——Log.Rollback 未实现（见方法体内的
	 * 保留注释）。事务失败（rc!=0、乐观检查失败、RaftRetry 失去领导权等）后，这些
	 * 修改残留在记录缓存中（污染 bean）：本节点后续访问该记录的读者可能读到未提交
	 * 的修改，直到该记录被驱逐出缓存并从 storage 重载为止（驱逐路径见
	 * {@link Table#tryRemoveRecord} 与 {@link Record#evictPolluted}；截断场景的
	 * 污染驱逐见 {@code Rocks.takePendingFlush} 的 term 不匹配分支）。
	 * 依赖"回滚后读不到过程内修改"的代码在 RocksRaft 下不成立。
	 */
	public void rollback() {
		// 现在没有实现 Log.Rollback。不需要再做什么，保留接口，以后实现Rollback时再处理。
		// newly.foreachValue(log -> log.Rollback());
	}
}
