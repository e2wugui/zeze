package Zeze.Transaction.Logs;

import java.lang.invoke.VarHandle;

import Zeze.Transaction.Bean;
import Zeze.Transaction.TransactionLog;

/** String 字段本地写入事务日志，提交时将继承的新值写回所属 Bean。 */
public class LogString extends LogStringValue implements TransactionLog {
	private final Bean belong;
	private final VarHandle vh;

	public LogString(Bean belong, int varId, VarHandle vh, Object value) {
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
		vh.set(belong, stringValue());
	}
}
