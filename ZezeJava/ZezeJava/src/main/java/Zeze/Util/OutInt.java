package Zeze.Util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// int 输出参数包装（模拟 C# out 参数）
public class OutInt {
	public int value;

	public OutInt() {
	}

	public OutInt(int value) {
		this.value = value;
	}

	@Override
	public int hashCode() {
		return Integer.hashCode(value);
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		return obj instanceof OutInt && value == ((OutInt)obj).value;
	}

	@Override
	public @NotNull String toString() {
		return String.valueOf(value);
	}
}
