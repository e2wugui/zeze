package Zeze.Util;

public final class MathEx {
	public static int unsignedMod(int hash, int div) {
		if (div <= 0)
			throw new RuntimeException("div <= 0");
		return Integer.remainderUnsigned(hash, div);
	}

	private MathEx() {
	}
}
