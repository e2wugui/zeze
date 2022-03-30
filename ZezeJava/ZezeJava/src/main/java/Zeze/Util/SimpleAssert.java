package Zeze.Util;

public final class SimpleAssert {
	public static void IsTrue(boolean c) {
		if (!c)
			throw new ThrowAgainException();
	}

	public static void IsNull(Object o) {
		if (o != null)
			throw new ThrowAgainException(o + " != null");
	}

	public static void AreEqual(Object except, Object current) {
		if (!except.equals(current))
			throw new ThrowAgainException(except + " != " + current);
	}

	private SimpleAssert() {
	}
}
