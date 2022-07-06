package Zeze.Transaction;

import java.util.ArrayList;
import Zeze.Util.LongHashMap;

public final class Savepoint {
	private LongHashMap<Log> Logs;
	// private final LongHashMap<Log> Newly = new LongHashMap<>(); // 当前Savepoint新加的，用来实现Rollback，先不实现。
	private ArrayList<Action> actions;

	public LongHashMap<Log>.Iterator logIterator() {
		var logs = Logs;
		return logs != null ? logs.iterator() : null;
	}

	public Log GetLog(long logKey) {
		var logs = Logs;
		return logs != null ? logs.get(logKey) : null;
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
		var logs = Logs;
		if (logs != null) {
			var newLogs = new LongHashMap<>(logs);
			newLogs.foreachUpdate((__, v) -> v.BeginSavepoint());
			sp.Logs = newLogs;
		}
		return sp;
	}

	public enum ActionType {
		COMMIT,
		ROLLBACK,
		NESTED_ROLLBACK
	}

	public static final class Action {
		public ActionType actionType;
		public final Runnable action;

		public Action(ActionType actionType, Runnable action) {
			this.actionType = actionType;
			this.action = action;
		}
	}

	private ArrayList<Action> getActionsForAdd() {
		var a = actions;
		if (a == null)
			actions = a = new ArrayList<>();
		return a;
	}

	public void addCommitAction(Runnable action) {
		getActionsForAdd().add(new Action(ActionType.COMMIT, action));
	}

	public void addRollbackAction(Runnable action) {
		getActionsForAdd().add(new Action(ActionType.ROLLBACK, action));
	}

	public void MergeCommitFrom(Savepoint next) {
		var nextLogs = next.Logs;
		if (nextLogs != null)
			nextLogs.foreachValue(log -> log.EndSavepoint(this));
		var nextActions = next.actions;
		if (nextActions != null)
			getActionsForAdd().addAll(nextActions);
	}

	public void MergeRollbackFrom(Savepoint next) {
		var nextActions = next.actions;
		if (nextActions != null) {
			for (Action action : nextActions) {
				if (action.actionType == ActionType.ROLLBACK)
					action.actionType = ActionType.NESTED_ROLLBACK;
				else if (action.actionType != ActionType.NESTED_ROLLBACK)
					continue;
				getActionsForAdd().add(action);
			}
		}
	}

	public void MergeCommitActions(ArrayList<Action> transactionActions) {
		var a = actions;
		if (a != null) {
			for (Action action : a) {
				if (action.actionType == ActionType.COMMIT || action.actionType == ActionType.NESTED_ROLLBACK)
					transactionActions.add(action);
			}
		}
	}

	public void MergeRollbackActions(ArrayList<Action> transactionActions) {
		var a = actions;
		if (a != null) {
			for (Action action : a) {
				if (action.actionType == ActionType.ROLLBACK || action.actionType == ActionType.NESTED_ROLLBACK)
					transactionActions.add(action);
			}
		}
	}

	public void Commit() {
		var logs = Logs;
		if (logs != null)
			logs.foreachValue(Log::Commit);
	}
}
