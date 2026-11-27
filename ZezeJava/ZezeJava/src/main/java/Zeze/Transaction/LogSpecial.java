package Zeze.Transaction;

import org.jspecify.annotations.Nullable;

/** 仅用于事务本地提交动作的特殊日志基类（如 RecordAccessed.PutLog），不参与序列化传输。 */
public abstract class LogSpecial<TBean extends Bean, TValue> implements TransactionLog {
	private final @Nullable TValue value;
	private final Bean belong;
	private final int variableId;

	@Override
	public final Bean getBelong() {
		return belong;
	}

	@Override
	public final int getVariableId() {
		return variableId;
	}

	protected LogSpecial(Bean bean, int varId, @Nullable TValue value) {
		belong = bean;
		variableId = varId;
		this.value = value;
	}

	public final @Nullable TValue getValue() {
		return value;
	}

	@SuppressWarnings("unchecked")
	public final TBean getBeanTyped() {
		return (TBean)getBelong();
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}
}
