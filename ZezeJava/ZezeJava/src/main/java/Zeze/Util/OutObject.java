package Zeze.Util;

import java.util.Objects;

public class OutObject<T> {
	public T value;

	public OutObject() {
	}

	public OutObject(T value) {
		this.value = value;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(value);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof OutObject && Objects.equals(value, ((OutObject<?>)obj).value);
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}
}
