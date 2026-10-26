package Zeze.Transaction.Logs;

import java.lang.invoke.VarHandle;

import Zeze.Serialize.Vector3Int;

import Zeze.Transaction.Bean;
import Zeze.Transaction.TransactionLog;

/** Local field write; the inherited value is also its complete change payload. */
public class LogVector3Int extends LogVector3IntValue implements TransactionLog {
	private final Bean belong;
	private final VarHandle vh;

	public LogVector3Int(Bean belong, int varId, VarHandle vh, Vector3Int value) {
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
