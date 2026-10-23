package Zeze.Transaction.Logs;

import java.lang.invoke.VarHandle;

import Zeze.Net.Binary;

import Zeze.Transaction.Bean;
import Zeze.Transaction.Changes;
import Zeze.Transaction.TransactionLog;

/** Local field write; the inherited value is also its complete change payload. */
public class LogBinary extends LogBinaryValue implements TransactionLog {
	private final Bean belong;
	private final VarHandle vh;

	public LogBinary(Bean belong, int varId, VarHandle vh, Binary value) {
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
