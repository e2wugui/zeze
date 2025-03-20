package Zeze.Util;

public class MathEx {
	public static int unsignedMod(int hash, int div) {
		return (int)(Integer.toUnsignedLong(hash) % Integer.toUnsignedLong(div));
	}
}
