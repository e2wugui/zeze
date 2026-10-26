package Zeze.Transaction.Collections;

import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Transaction.Savepoint;
import Zeze.Transaction.TransactionLog;

/** CollOne 的写侧事务日志；完整消费载荷由父类保存。 */
public class LogOne<V extends Bean> extends LogOneValue<V> implements TransactionLog {
	private final Bean belong;

	public LogOne(Bean belong, int varId, Bean self, V value) {
		super(varId, resolveMeta(self, value), self);
		this.belong = belong;
		this.value = value;
	}

	private static <V extends Bean> LogOneMeta<V> resolveMeta(Bean self, V value) {
		// 声明类优先：装子类实例须按声明类建typeId，与读端注册对称。
		if (self instanceof CollOne<?> collOne)
			//noinspection unchecked
			return (LogOneMeta<V>)collOne.meta;
		//noinspection unchecked
		return LogOneMeta.get((Class<V>)value.getClass());
	}

	@Override
	public final Bean getBelong() {
		return belong;
	}

	public void setValue(V value) {
		this.value = value;
	}

	@Override
	public TransactionLog beginSavepoint() {
		var self = getThis();
		assert self != null;
		assert value != null;
		return new LogOne<>(getBelong(), getVariableId(), self, value);
	}

	@Override
	public void endSavepoint(Savepoint currentSp) {
		// 结束保存点，直接覆盖到当前的日志里面即可。
		currentSp.putLog(this);
	}

	@Override
	public boolean addChild(Bean recent, Log vlog) {
		if (logBean == null) {
			logBean = (LogBean)vlog;
			return true;
		}
		return false;
	}

	@Override
	public void commit() {
		if (value != null)
			//noinspection DataFlowIssue,unchecked
			((CollOne<V>)getThis()).value = value;
	}
}
