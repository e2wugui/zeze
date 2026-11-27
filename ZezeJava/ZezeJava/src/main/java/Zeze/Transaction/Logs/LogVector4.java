package Zeze.Transaction.Logs;

import java.lang.invoke.VarHandle;

import Zeze.Serialize.Vector4;

import Zeze.Transaction.Bean;
import Zeze.Transaction.TransactionLog;

/** Vector4 字段本地写入事务日志，提交时将继承的新值写回所属 Bean。 */
public class LogVector4 extends LogVector4Value implements TransactionLog {
	private final Bean belong;
	private final VarHandle vh;

	public LogVector4(Bean belong, int varId, VarHandle vh, Vector4 value) {
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
