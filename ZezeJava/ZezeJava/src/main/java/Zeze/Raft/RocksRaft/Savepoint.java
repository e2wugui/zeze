package Zeze.Raft.RocksRaft;

import java.util.ArrayList;
import org.jspecify.annotations.Nullable;
import Zeze.Util.Action0;
import Zeze.Util.LongHashMap;

/**
 * 事务的 savepoint：本层修改日志集与提交/回滚动作，支持嵌套事务的合并提交/回滚。
 */
public final class Savepoint {
	private @Nullable LongHashMap<Log> logs;
	private @Nullable ArrayList<Action0> commitActions;
	private @Nullable ArrayList<Action0> rollbackActions;

	@Nullable ArrayList<Action0> getCommitActions() {
		return commitActions;
	}

	@Nullable ArrayList<Action0> getRollbackActions() {
		return rollbackActions;
	}

	public LongHashMap<Log>.@Nullable Iterator logIterator() {
		var logs = this.logs;
		return logs != null ? logs.iterator() : null;
	}

	public @Nullable Log getLog(long logKey) {
		var logs = this.logs;
		return logs != null ? logs.get(logKey) : null;
	}

	public void putLog(Log log) {
		var logs = this.logs;
		if (logs == null)
			this.logs = logs = new LongHashMap<>();
		logs.put(log.getLogKey(), log);
	}

	public Savepoint beginSavepoint() {
		var sp = new Savepoint();
		var logs = this.logs;
		if (logs != null) {
			var newLogs = new LongHashMap<>(logs);
			//noinspection DataFlowIssue
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
	 * 撤销当前 savepoint：仅丢弃本层日志并触发回滚回调，不还原内存 bean 的原位修改（Log.Rollback 未实现）。
	 */
	public void rollback() {
	}
}
