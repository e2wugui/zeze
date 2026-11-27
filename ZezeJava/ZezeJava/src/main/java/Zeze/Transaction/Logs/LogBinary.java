package Zeze.Transaction.Logs;

import java.lang.invoke.VarHandle;

import Zeze.Net.Binary;

import Zeze.Transaction.Bean;
import Zeze.Transaction.TransactionLog;

/** Binary 字段本地写入事务日志，提交时将继承的新值写回所属 Bean。 */
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
	public void commit() {
		vh.set(belong, value);
	}
}
