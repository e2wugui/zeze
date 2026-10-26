package Zeze.Transaction.Logs;

import java.lang.invoke.VarHandle;

import Zeze.Serialize.Vector3;

import Zeze.Transaction.Bean;
import Zeze.Transaction.TransactionLog;

/** Local field write; the inherited value is also its complete change payload. */
public class LogVector3 extends LogVector3Value implements TransactionLog {
	private final Bean belong;
	private final VarHandle vh;

	public LogVector3(Bean belong, int varId, VarHandle vh, Vector3 value) {
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
