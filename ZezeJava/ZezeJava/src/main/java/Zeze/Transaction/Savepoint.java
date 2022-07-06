package Zeze.Transaction;

import java.util.ArrayList;
import java.util.List;
import Zeze.Util.LongHashMap;

public final class Savepoint {
	private final LongHashMap<Log> Logs = new LongHashMap<>();
	// private final HashMap<Long, Log> Newly = new HashMap<>(); // 当前Savepoint新加的，用来实现Rollback，先不实现。

	public LongHashMap<Log> getLogs() {
		return Logs;
	}

	public enum ActionType {
		COMMIT,
		ROLLBACK,
		NESTED_ROLLBACK
	}

	public static class Action {
		public ActionType actionType;
		public final Runnable action;

		public Action(ActionType actionType, Runnable action) {
			this.actionType = actionType;
			this.action = action;
		}
	}

	private final ArrayList<Action> actions = new ArrayList<>();

	public void PutLog(Log log) {
		Logs.put(log.getLogKey(), log);
		// Newly.put(log.LogKey, log);
	}

	public Log GetLog(long logKey) {
		return Logs.get(logKey);
	}

	public Savepoint Duplicate() {
		Savepoint sp = new Savepoint();
		Logs.foreachValue((log) -> sp.Logs.put(log.getLogKey(), log.BeginSavepoint()));
		return sp;
	}

	public void MergeFrom(Savepoint other, boolean isCommit) {
		if (isCommit) {
			other.Logs.foreachValue((log) -> log.EndSavepoint(this));
			actions.addAll(other.actions);
		} else {

			for (Action action : other.actions) {
				if (action.actionType == ActionType.NESTED_ROLLBACK) {
					actions.add(action);
				} else if (action.actionType == ActionType.ROLLBACK) {
					action.actionType = ActionType.NESTED_ROLLBACK;
					actions.add(action);
				}
			}
		}
	}

	public void MergeActions(List<Action> transactionActions, boolean isCommit) {
		for (Action action : actions) {
			if (action.actionType == ActionType.NESTED_ROLLBACK) {
				transactionActions.add(action);
			} else if (action.actionType == ActionType.ROLLBACK && !isCommit ||
					action.actionType == ActionType.COMMIT && isCommit) {
				transactionActions.add(action);
			}
		}
	}

	public void addCommitAction(Runnable action) {
		actions.add(new Action(ActionType.COMMIT, action));
	}

	public void addRollbackAction(Runnable action) {
		actions.add(new Action(ActionType.ROLLBACK, action));
	}

	@SuppressWarnings("unused")
	private void clearActions() {
		actions.clear();
	}

	public void Commit() {
		Logs.foreachValue(Log::Commit);
	}

	public void Rollback() {
	}
}
