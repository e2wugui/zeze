package Zeze.Util;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

// 对象输出参数包装（模拟 C# out 参数）
public class OutObject<T extends @Nullable Object> {
	public @Nullable T value;

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
	public String toString() {
		return String.valueOf(value);
	}
}
