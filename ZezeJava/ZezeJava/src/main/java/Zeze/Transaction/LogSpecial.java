package Zeze.Transaction;

import org.jspecify.annotations.Nullable;

/** Local-only value used by commit actions, such as RecordAccessed.PutLog. */
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

	@Override
	public Category category() {
		return Category.eSpecial;
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

	@Override
	public final void collectChanges(Changes changes) {
	}
}
