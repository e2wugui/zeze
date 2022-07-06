package Zeze.Transaction;

import java.util.ArrayList;
import Zeze.Util.LongHashMap;

public final class Savepoint {
	private LongHashMap<Log> Logs;
	// private final LongHashMap<Log> Newly = new LongHashMap<>(); // 当前Savepoint新加的，用来实现Rollback，先不实现。
	private ArrayList<Action> actions;

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
		// Newly.put(log.LogKey, log);
	}

	public Savepoint BeginSavepoint() {
		var sp = new Savepoint();
		if (Logs != null) {
			sp.Logs = new LongHashMap<>(Logs);
			sp.Logs.foreachUpdate((__, v) -> v.BeginSavepoint());
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

	public void MergeFrom(Savepoint next, boolean isCommit) {
		if (isCommit) {
			if (next.Logs != null)
				next.Logs.foreachValue(log -> log.EndSavepoint(this));
			if (next.actions != null)
				getActionsForAdd().addAll(next.actions);
		} else if (next.actions != null) {
			for (Action action : next.actions) {
				if (action.actionType == ActionType.NESTED_ROLLBACK) {
					getActionsForAdd().add(action);
				} else if (action.actionType == ActionType.ROLLBACK) {
					action.actionType = ActionType.NESTED_ROLLBACK;
					getActionsForAdd().add(action);
				}
			}
		}
	}

	public void MergeActions(ArrayList<Action> transactionActions, boolean isCommit) {
		if (actions != null) {
			for (Action action : actions) {
				if (action.actionType == ActionType.NESTED_ROLLBACK ||
						action.actionType == ActionType.ROLLBACK && !isCommit ||
						action.actionType == ActionType.COMMIT && isCommit) {
					transactionActions.add(action);
				}
			}
		}
	}

	public void Commit() {
		if (Logs != null)
			Logs.foreachValue(Log::Commit);
	}
}
