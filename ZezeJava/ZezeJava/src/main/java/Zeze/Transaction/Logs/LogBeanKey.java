package Zeze.Transaction.Logs;

import java.lang.invoke.VarHandle;

import Zeze.Serialize.Encodable;
import Zeze.Transaction.Collections.BeanKeyMeta;

import Zeze.Transaction.Bean;
import Zeze.Transaction.TransactionLog;

/** Local field write; the inherited value is also its complete change payload. */
public class LogBeanKey<T extends Encodable> extends LogBeanKeyValue<T> implements TransactionLog {
	private final Bean belong;
	private final VarHandle vh;

	public LogBeanKey(Bean belong, int varId, VarHandle vh, BeanKeyMeta<T> meta, T value) {
		super(varId, meta, value);
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
