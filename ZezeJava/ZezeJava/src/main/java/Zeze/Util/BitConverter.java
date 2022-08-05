package Zeze.Util;

import java.nio.charset.StandardCharsets;

public class BitConverter {
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
		return new String(str, StandardCharsets.UTF_8);
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
}
