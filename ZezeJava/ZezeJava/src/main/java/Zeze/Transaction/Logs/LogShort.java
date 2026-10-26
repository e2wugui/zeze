package Zeze.Transaction.Logs;

import java.lang.invoke.VarHandle;

import Zeze.Transaction.Bean;
import Zeze.Transaction.TransactionLog;

/** Local field write; the inherited value is also its complete change payload. */
public class LogShort extends LogShortValue implements TransactionLog {
	private final Bean belong;
	private final VarHandle vh;

	public LogShort(Bean belong, int varId, VarHandle vh, short value) {
		super(varId, value);
		this.belong = belong;
		this.vh = vh;
	}

	@Override
	public final Bean getBelong() {
		return belong;
	}

	@Override
	public void commit() {
		vh.set(belong, value);
	}
}
