package Zeze.Transaction.Logs;

import java.lang.invoke.VarHandle;

import Zeze.Serialize.Vector2Int;

import Zeze.Transaction.Bean;
import Zeze.Transaction.TransactionLog;

/** Vector2Int 字段本地写入事务日志，提交时将继承的新值写回所属 Bean。 */
public class LogVector2Int extends LogVector2IntValue implements TransactionLog {
	private final Bean belong;
	private final VarHandle vh;

	public LogVector2Int(Bean belong, int varId, VarHandle vh, Vector2Int value) {
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
