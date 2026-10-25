package Zeze.Transaction;

import java.util.Objects;

import Zeze.Transaction.Collections.LogBean;

/** DynamicBean 的写侧事务日志；完整消费载荷由父类保存。 */
public class LogDynamic extends LogDynamicValue implements TransactionLog {
	private final Bean belong;

	public LogDynamic(Bean belong, int varId, Bean self) {
		super(varId, self);
		this.belong = Objects.requireNonNull(belong);
	}

	@Override
	public final Bean getBelong() {
		return belong;
	}

	@Override
	public final Category category() {
		return Category.eHistory;
	}

	/**
	 * Savepoint存储键，必须与DynamicBean.getBean/getTypeId/setBean的查找键一致：
	 * 集合元素形态（self.parent为Collection）下belong(集合)+varId在同组元素间碰撞，
	 * 用self的dynamicLogKey（见该方法注释的号段论证）。
	 */
	@Override
	public long getLogKey() {
		var self = getThis();
		if (self instanceof DynamicBean dynamic)
			return dynamic.dynamicLogKey();
		return TransactionLog.super.getLogKey();
	}

	@Override
	public TransactionLog beginSavepoint() {
		var self = getThis();
		assert self != null;
		var dup = new LogDynamic(getBelong(), getVariableId(), self);
		dup.specialTypeId = specialTypeId;
		dup.value = value;
		return dup;
	}

	@Override
	public void endSavepoint(Savepoint currentSp) {
		// 结束保存点，直接覆盖到当前的日志里面即可。
		currentSp.putLog(this);
	}

	@Override
	public boolean addChild(Bean recent, Log vlog) {
		if (logBean == null) {
			var child = (LogBean)vlog;
			if (child.getThis() != ((DynamicBean)recent).bean)
				return true;
			logBean = child;
			return true;
		}
		return false;
	}

	@Override
	public void commit() {
		if (value != null) {
			var self = (DynamicBean)Objects.requireNonNull(getThis());
			self.bean = value;
			self.typeId = specialTypeId;
		}
	}

	void setValue(long typeId, Bean bean) {
		specialTypeId = typeId;
		value = bean;
	}

	@Override
	public void collectChanges(Changes changes) {
		if (belong.isManaged())
			changes.collect(belong, this);
	}
}
