package Zeze.Raft.RocksRaft;

import java.util.ArrayList;
import java.util.List;
import Zeze.Util.Action0;
import Zeze.Util.LongHashMap;

public final class Savepoint {
	private final LongHashMap<Log> Logs = new LongHashMap<>();
	// private final LongHashMap<Log> Newly = new LongHashMap<Log>(); // 当前Savepoint新加的，用来实现Rollback，先不实现。
	final List<Action0> CommitActions = new ArrayList<>();
	final List<Action0> RollbackActions = new ArrayList<>();

	public LongHashMap<Log> getLogs() {
		return Logs;
	}

	public void PutLog(Log log) {
		Logs.put(log.getLogKey(), log);
		// newly[log.LogKey] = log;
	}

	public Log GetLog(long logKey) {
		return Logs.get(logKey);
	}

	public Savepoint BeginSavepoint() {
		var sp = new Savepoint();
		for (var it = Logs.iterator(); it.moveToNext(); )
			sp.Logs.put(it.key(), it.value().BeginSavepoint());
		return sp;
	}

	public void MergeFrom(Savepoint next, boolean isCommit) {
		if (isCommit) {
			for (var it = next.Logs.iterator(); it.moveToNext(); )
				it.value().EndSavepoint(this);
			CommitActions.addAll(next.CommitActions);
		} else
			CommitActions.addAll(next.RollbackActions);
		RollbackActions.addAll(next.RollbackActions);
	}

	public void Rollback() {
		// 现在没有实现 Log.Rollback。不需要再做什么，保留接口，以后实现Rollback时再处理。
		// for (var e : newly)
		//     e.Value.Rollback();
	}
}
