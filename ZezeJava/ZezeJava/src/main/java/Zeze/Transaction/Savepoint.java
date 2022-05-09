package Zeze.Transaction;

import java.util.ArrayList;
import java.util.List;
import Zeze.Util.LongHashMap;

public final class Savepoint {
	// private static final Logger logger = LogManager.getLogger(Savepoint.class);
	private final LongHashMap<Log> Logs = new LongHashMap<>();
	// private readonly Dictionary<long, Log> Newly = new Dictionary<>(); // 当前Savepoint新加的，用来实现Rollback，先不实现。

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
		//newly[log.LogKey] = log;
	}

	public Log GetLog(long logKey) {
		return Logs.get(logKey);
	}

	public Savepoint Duplicate() {
		Savepoint sp = new Savepoint();
		sp.Logs.putAll(Logs);
		return sp;
	}

	public void MergeFrom(Savepoint other, boolean isCommit) {
		if (isCommit) {
			Logs.putAll(other.Logs);
			actions.addAll(other.actions);
		} else{

			for (Action action : other.actions) {
				if (action.actionType == ActionType.NESTED_ROLLBACK) {
					actions.add(action);
				} else if (action.actionType == ActionType.ROLLBACK){
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
					action.actionType == ActionType.COMMIT && isCommit){
				transactionActions.add(action);
			}
		}
	}

	public void addCommitAction(Runnable action) {
		this.actions.add(new Action(ActionType.COMMIT, action));
	}

	public void addRollbackAction(Runnable action) {
		this.actions.add(new Action(ActionType.ROLLBACK, action));
	}

	@SuppressWarnings("unused")
	private void clearActions() {
		this.actions.clear();
	}

	public void Commit() {
		Logs.foreachValue(Log::Commit);
	}

	public void Rollback() {
	}
}
