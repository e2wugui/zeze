package Zeze.Util;

public class BitConverter {
	public static int num2Hex(int n) {
		return n + '0' + (((9 - n) >> 31) & ('A' - '9' - 1)); // 无分支,比查表快
	}

	public static String toString(byte[] bytes, int offset, int len) {
		var sb = new StringBuilder();
		for (int i = 0; i < len; i++) {
			int b = bytes[i + offset];
			if (i > 0)
				sb.append('-');
			sb.append((char)num2Hex((b >> 4) & 0xf));
			sb.append((char)num2Hex(b & 0xf));
		}
		return sb.toString();
	}

	public static String toString(byte[] bytes) {
		return toString(bytes, 0, bytes.length);
	}

	public static String toStringWithLimit(byte[] bytes, int offset, int len, int limit) {
		var sb = new StringBuilder();
		for (int i = 0, n = Math.min(len, limit); i < n; i++) {
			int b = bytes[i + offset];
			if (i > 0)
				sb.append('-');
			sb.append((char)num2Hex((b >> 4) & 0xf));
			sb.append((char)num2Hex(b & 0xf));
		}
		if (len > limit)
			sb.append("...[+").append(len - limit).append(']');
		return sb.toString();
	}

	public static String toStringWithLimit(byte[] bytes, int limit) {
		return toStringWithLimit(bytes, 0, bytes.length, limit);
	}

	public static void main(String[] args) {
		var bytes = new byte[256];
		for (int i = 0; i < bytes.length; ++i)
			bytes[i] = (byte)i;
		System.out.println(BitConverter.toStringWithLimit(bytes, 16));
	}
}
