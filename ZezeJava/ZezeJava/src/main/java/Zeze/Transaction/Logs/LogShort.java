package Zeze.Transaction.Logs;

import java.lang.invoke.VarHandle;

import Zeze.Transaction.Bean;
import Zeze.Transaction.TransactionLog;

/** short 字段本地写入事务日志，提交时将继承的新值写回所属 Bean。 */
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
