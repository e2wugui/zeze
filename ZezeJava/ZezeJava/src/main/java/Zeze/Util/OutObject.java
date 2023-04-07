package Zeze.Util;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OutObject<T> {
	public T value;

	public OutObject() {
	}

	public OutObject(@Nullable T value) {
		this.value = value;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(value);
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		return obj instanceof OutObject && Objects.equals(value, ((OutObject<?>)obj).value);
	}

	@Override
	public @NotNull String toString() {
		return String.valueOf(value);
	}
}
