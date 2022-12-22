package Zeze.Util;

import java.nio.charset.StandardCharsets;

public final class BitConverter {
	public static int num2Hex(int n) {
		return n + '0' + (((9 - n) >> 31) & ('A' - '9' - 1)); // 无分支,比查表快
	}

	public static String toString(byte[] bytes, int offset, int len) {
		if (len <= 0)
			return "";
		var str = new byte[len * 3 - 1];
		for (int i = 0, j = 0; i < len; i++) {
			if (i > 0)
				str[j++] = '-';
			int b = bytes[offset + i];
			str[j++] = (byte)num2Hex((b >> 4) & 0xf);
			str[j++] = (byte)num2Hex(b & 0xf);
		}
		return new String(str, StandardCharsets.ISO_8859_1);
	}

	public static String toString(byte[] bytes) {
		return toString(bytes, 0, bytes.length);
	}

	public static String toStringWithLimit(byte[] bytes, int offset, int len, int limit) {
		if (limit < 0)
			limit = 0;
		if (len <= limit)
			return toString(bytes, offset, len);
		var sb = new StringBuilder(limit * 3 + 16);
		for (int i = 0; i < limit; i++) {
			int b = bytes[i + offset];
			if (i > 0)
				sb.append('-');
			sb.append((char)num2Hex((b >> 4) & 0xf));
			sb.append((char)num2Hex(b & 0xf));
		}
		sb.append("...[+").append(len - limit).append(']');
		return sb.toString();
	}

	public static String toStringWithLimit(byte[] bytes, int limit) {
		return toStringWithLimit(bytes, 0, bytes.length, limit);
	}

	// 十六进制字符串转成二进制数组. 大小写的A~F都支持,忽略其它字符
	public static void toBytes(String hex, byte[] bytes, int offset) {
		final long MASK = ~0x007E_0000_007E_03FFL; // 0~9;A~F;a~f
		for (int i = 0, v = 1, s = hex.length(); i < s; i++) {
			int c = hex.charAt(i) - '0';
			if ((c & ~63 | (int)(MASK >> c) & 1) == 0) {
				v = (v << 4) + (c & 0xf) + ((c >> 4) & 1) * 9;
				if (v > 0xff) {
					bytes[offset++] = (byte)v;
					v = 1;
				}
			}
		}
	}

	// 十六进制字符串转成二进制数组. 大小写的A~F都支持,忽略其它字符
	public static byte[] toBytes(String hex) {
		final long MASK = 0x007E_0000_007E_03FFL; // 0~9;A~F;a~f
		int n = 0;
		for (int i = 0, s = hex.length(); i < s; i++) {
			int c = hex.charAt(i) - '0';
			if ((c & ~63) == 0)
				n += (int)(MASK >> c) & 1;
		}
		byte[] b = new byte[n >> 1];
		toBytes(hex, b, 0);
		return b;
	}

	private BitConverter() {
	}
}
