package Zeze.Transaction;

import java.util.ArrayList;
import Zeze.Util.LongHashMap;

public final class Savepoint {
	private LongHashMap<Log> logs; // key:objectId+varId
	// private final LongHashMap<Log> Newly = new LongHashMap<>(); // 当前Savepoint新加的，用来实现Rollback，先不实现。
	private ArrayList<Action> actions;

	LongHashMap<Log>.Iterator logIterator() {
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
		// Newly.put(log.getLogKey(), log);
	}

	Savepoint beginSavepoint() {
		var sp = new Savepoint();
		var logs = this.logs;
		if (logs != null) {
			var newLogs = new LongHashMap<>(logs);
			newLogs.foreachUpdate((__, v) -> v.beginSavepoint());
			sp.logs = newLogs;
		}
		return sp;
	}

	enum ActionType {
		COMMIT,
		ROLLBACK,
		NESTED_ROLLBACK
	}

	static final class Action {
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

	void addCommitAction(Runnable action) {
		getActionsForAdd().add(new Action(ActionType.COMMIT, action));
	}

	void addRollbackAction(Runnable action) {
		getActionsForAdd().add(new Action(ActionType.ROLLBACK, action));
	}

	void mergeCommitFrom(Savepoint next) {
		var nextLogs = next.logs;
		if (nextLogs != null)
			nextLogs.foreachValue(log -> log.endSavepoint(this));
		var nextActions = next.actions;
		if (nextActions != null)
			getActionsForAdd().addAll(nextActions);
	}

	void mergeRollbackFrom(Savepoint next) {
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

	void mergeCommitActions(ArrayList<Action> transactionActions) {
		var a = actions;
		if (a != null) {
			for (Action action : a) {
				if (action.actionType == ActionType.COMMIT || action.actionType == ActionType.NESTED_ROLLBACK)
					transactionActions.add(action);
			}
		}
	}

	void mergeRollbackActions(ArrayList<Action> transactionActions) {
		var a = actions;
		if (a != null) {
			for (Action action : a) {
				if (action.actionType == ActionType.ROLLBACK || action.actionType == ActionType.NESTED_ROLLBACK)
					transactionActions.add(action);
			}
		}
	}

	void commit() {
		var logs = this.logs;
		if (logs != null)
			logs.foreachValue(Log::commit);
	}
}
