package Zeze.Util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OutLong {
	public long value;

	public OutLong() {
	}

	public OutLong(long value) {
		this.value = value;
	}

	@Override
	public int hashCode() {
		return Long.hashCode(value);
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		return obj instanceof OutLong && value == ((OutLong)obj).value;
	}

	@Override
	public @NotNull String toString() {
		return String.valueOf(value);
	}
}
