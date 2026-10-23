package Zeze.Transaction.Logs;

import java.lang.invoke.VarHandle;

import Zeze.Serialize.Vector2Int;

import Zeze.Transaction.Bean;
import Zeze.Transaction.Changes;
import Zeze.Transaction.TransactionLog;

/** Local field write; the inherited value is also its complete change payload. */
public class LogVector2Int extends LogVector2IntValue implements TransactionLog {
	private final Bean belong;
	private final VarHandle vh;

	public LogVector2Int(Bean belong, int varId, VarHandle vh, Vector2Int value) {
		super(varId, value);
		this.belong = belong;
		this.vh = vh;
	}

	@Override
	public final Bean getBelong() {
		return belong;
	}

	@Override
	public Category category() {
		return Category.eHistory;
	}

	@Override
	public void commit() {
		vh.set(belong, value);
	}

	@Override
	public void collectChanges(Changes changes) {
		if (belong.isManaged())
			changes.collect(belong, this);
	}
}
