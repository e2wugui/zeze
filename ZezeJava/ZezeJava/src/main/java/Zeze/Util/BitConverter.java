package Zeze.Util;

public class BitConverter {
	private static final char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

	public static String toString(byte[] bytes, int offset, int len) {
		var sb = new StringBuilder();
		for (int i = 0; i < len; i++) {
			int b = bytes[i + offset];
			if (i > 0)
				sb.append('-');
			sb.append(hexDigits[(b >> 4) & 0xf]);
			sb.append(hexDigits[b & 0xf]);
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
			sb.append(hexDigits[(b >> 4) & 0xf]);
			sb.append(hexDigits[b & 0xf]);
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
