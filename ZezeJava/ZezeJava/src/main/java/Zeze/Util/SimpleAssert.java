package Zeze.Util;

import java.util.List;

public final class SimpleAssert {
	public static void isTrue(boolean c) {
		if (!c)
			throw new ThrowAgainException();
	}

	public static void isNull(Object o) {
		if (o != null)
			throw new ThrowAgainException(o + " != null");
	}

	public static void areEqual(Object expected, Object current) {
		if (!expected.equals(current))
			throw new ThrowAgainException(expected + " != " + current);
	}

	public static <T> boolean areSequenceEqual(List<T> a, List<T> b) {
		int size = a.size();
		if (size != b.size())
			return false;
		for (int i = 0; i < size; ++i) {
			if (!a.get(i).equals(b.get(i)))
				return false;
		}
		return true;
	}

	private SimpleAssert() {
	}
}
