package Zeze.Util;

public class MathEx {
	public static int unsignedMod(int hash, int div) {
		if (div < 0)
			throw new RuntimeException("div < 0");
		return (int)(Integer.toUnsignedLong(hash) % div);
	}
}
