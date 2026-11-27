package Zeze.Transaction.Logs;

import java.lang.invoke.VarHandle;

import Zeze.Serialize.Quaternion;

import Zeze.Transaction.Bean;
import Zeze.Transaction.TransactionLog;

/** Quaternion 字段本地写入事务日志，提交时将继承的新值写回所属 Bean。 */
public class LogQuaternion extends LogQuaternionValue implements TransactionLog {
	private final Bean belong;
	private final VarHandle vh;

	public LogQuaternion(Bean belong, int varId, VarHandle vh, Quaternion value) {
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
