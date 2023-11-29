package Zeze.Transaction;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class Log1<TBean extends Bean, TValue> extends Log {
	private final @Nullable TValue value;

	protected Log1(@NotNull Bean bean, int varId, @Nullable TValue value) {
		setBean(bean);
		setVariableId(varId);
		this.value = value;
	}

	public final @Nullable TValue getValue() {
		return value;
	}

	@SuppressWarnings("unchecked")
	public final TBean getBeanTyped() {
		return (TBean)getBean();
	}

	@Override
	public @NotNull String toString() {
		return String.valueOf(value);
	}
}
