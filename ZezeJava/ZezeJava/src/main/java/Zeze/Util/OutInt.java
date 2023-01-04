package Zeze.Util;

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
	public boolean equals(Object obj) {
		return obj instanceof OutInt && value == ((OutInt)obj).value;
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}
}
