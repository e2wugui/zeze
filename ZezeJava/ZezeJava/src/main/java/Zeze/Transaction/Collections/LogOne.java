package Zeze.Transaction.Collections;

import java.util.Objects;

import Zeze.Transaction.Bean;
import Zeze.Transaction.Changes;
import Zeze.Transaction.Log;
import Zeze.Transaction.Savepoint;
import Zeze.Transaction.TransactionLog;

/** CollOne 的写侧事务日志；完整消费载荷由父类保存。 */
@SuppressWarnings("unchecked")
public class LogOne<V extends Bean> extends LogOneValue<V> implements TransactionLog {
	private final Bean belong;

	public LogOne(Bean belong, int varId, Bean self, V value) {
		super(varId, resolveMeta(self, value), self);
		this.belong = Objects.requireNonNull(belong);
		this.value = value;
	}

	private static <V extends Bean> LogOneMeta<V> resolveMeta(Bean self, V value) {
		// 声明类优先：装子类实例须按声明类建typeId，与读端注册对称。
		if (self instanceof CollOne<?> collOne)
			return (LogOneMeta<V>)collOne.meta;
		return LogOneMeta.get((Class<V>)value.getClass());
	}

	@Override
	public final Bean getBelong() {
		return belong;
	}

	@Override
	public final Category category() {
		return Category.eHistory;
	}

	public void setValue(V value) {
		this.value = value;
	}

	@Override
	public TransactionLog beginSavepoint() {
		var self = getThis();
		assert self != null;
		return new LogOne<>(getBelong(), getVariableId(), self, Objects.requireNonNull(value));
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
			//noinspection DataFlowIssue
			((CollOne<V>)getThis()).value = value;
	}

	@Override
	public void collectChanges(Changes changes) {
		if (belong.isManaged())
			changes.collect(belong, this);
	}
}
