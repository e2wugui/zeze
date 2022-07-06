package Zeze.Raft.RocksRaft;

import java.util.ArrayList;
import Zeze.Util.Action0;
import Zeze.Util.LongHashMap;

public final class Savepoint {
	private LongHashMap<Log> Logs;
	// private final LongHashMap<Log> Newly = new LongHashMap<Log>(); // 当前Savepoint新加的，用来实现Rollback，先不实现。
	private ArrayList<Action0> CommitActions;
	private ArrayList<Action0> RollbackActions;

	ArrayList<Action0> getCommitActions() {
		return CommitActions;
	}

	ArrayList<Action0> getRollbackActions() {
		return RollbackActions;
	}

	public LongHashMap<Log>.Iterator logIterator() {
		return Logs != null ? Logs.iterator() : null;
	}

	public Log GetLog(long logKey) {
		return Logs != null ? Logs.get(logKey) : null;
	}

	public void PutLog(Log log) {
		var logs = Logs;
		if (logs == null)
			Logs = logs = new LongHashMap<>();
		logs.put(log.getLogKey(), log);
		// Newly.put(log.getLogKey(), log);
	}

	public Savepoint BeginSavepoint() {
		var sp = new Savepoint();
		if (Logs != null) {
			sp.Logs = new LongHashMap<>(Logs);
			sp.Logs.foreachUpdate((__, v) -> v.BeginSavepoint());
		}
		return sp;
	}

	private ArrayList<Action0> getCommitActionsForAdd() {
		var actions = CommitActions;
		if (actions == null)
			CommitActions = actions = new ArrayList<>();
		return actions;
	}

	private ArrayList<Action0> getRollbackActionsForAdd() {
		var actions = RollbackActions;
		if (actions == null)
			RollbackActions = actions = new ArrayList<>();
		return actions;
	}

	public void addCommitAction(Action0 action) {
		getCommitActionsForAdd().add(action);
	}

	public void addRollbackAction(Action0 action) {
		getRollbackActionsForAdd().add(action);
	}

	public void MergeFrom(Savepoint next, boolean isCommit) {
		if (isCommit) {
			if (next.Logs != null)
				next.Logs.foreachValue(log -> log.EndSavepoint(this));
			if (next.CommitActions != null)
				getCommitActionsForAdd().addAll(next.CommitActions);
		} else if (next.RollbackActions != null)
			getCommitActionsForAdd().addAll(next.RollbackActions);
		if (next.RollbackActions != null)
			getRollbackActionsForAdd().addAll(next.RollbackActions);
	}

	public void Rollback() {
		// 现在没有实现 Log.Rollback。不需要再做什么，保留接口，以后实现Rollback时再处理。
		// Newly.foreachValue(log -> log.Rollback());
	}
}
