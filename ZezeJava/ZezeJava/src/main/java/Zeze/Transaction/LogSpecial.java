package Zeze.Transaction;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class LogSpecial<TBean extends Bean, TValue> extends Log {
	private final @Nullable TValue value;

	protected LogSpecial(@NotNull Bean bean, int varId, @Nullable TValue value) {
		super(bean, varId);
		this.value = value;
	}

	@Override
	public @NotNull Category category() {
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
	public @NotNull String toString() {
		return String.valueOf(value);
	}
}
