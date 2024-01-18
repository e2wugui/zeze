package Zeze.Util;

import java.nio.charset.StandardCharsets;
import org.jetbrains.annotations.NotNull;

public final class BitConverter {
	public static int num2Hex(int n) {
		return n + '0' + (((9 - n) >> 31) & ('A' - '9' - 1)); // 无分支,比查表快
	}

	public static @NotNull String toHexString(byte @NotNull [] bytes) {
		return toHexString(bytes, 0, bytes.length);
	}

	public static @NotNull String toHexString(byte @NotNull [] bytes, int offset, int len) {
		if (len <= 0)
			return "";
		var str = new byte[len * 2];
		for (int i = 0, j = 0; i < len; i++) {
			int b = bytes[offset + i];
			str[j++] = (byte)num2Hex((b >> 4) & 0xf);
			str[j++] = (byte)num2Hex(b & 0xf);
		}
		return new String(str, StandardCharsets.ISO_8859_1);
	}

	public static @NotNull String toString(byte @NotNull [] bytes, int offset, int len) {
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

	public static @NotNull String toString(byte @NotNull [] bytes) {
		return toString(bytes, 0, bytes.length);
	}

	public static @NotNull String toStringWithLimit(byte @NotNull [] bytes, int offset, int len, int limit) {
		if (limit < 0)
			throw new IllegalArgumentException("limit=" + limit);
		if (len <= limit)
			return toString(bytes, offset, len);
		var sb = new StringBuilder(limit * 3 + 15);
		for (int i = 0; i < limit; i++) {
			int b = bytes[offset + i];
			if (i > 0)
				sb.append('-');
			sb.append((char)num2Hex((b >> 4) & 0xf));
			sb.append((char)num2Hex(b & 0xf));
		}
		sb.append("...[+").append(len - limit).append(']');
		return sb.toString();
	}

	public static @NotNull String toStringWithLimit(byte @NotNull [] bytes, int limit) {
		return toStringWithLimit(bytes, 0, bytes.length, limit);
	}

	public static @NotNull String toStringWithLimit(byte @NotNull [] bytes, int offset, int len,
													int limit1, int limit2) {
		int limit = limit1 + limit2;
		if ((limit1 | limit2 | limit) < 0)
			throw new IllegalArgumentException("limit=" + limit1 + '+' + limit2);
		if (len <= limit)
			return toString(bytes, offset, len);
		var sb = new StringBuilder(limit * 3 + 18);
		if (limit1 > 0) {
			for (int i = 0; i < limit1; i++) {
				int b = bytes[offset + i];
				if (i > 0)
					sb.append('-');
				sb.append((char)num2Hex((b >> 4) & 0xf));
				sb.append((char)num2Hex(b & 0xf));
			}
			sb.append("...");
		}
		sb.append('[').append('+').append(len - limit).append(']');
		if (limit2 > 0) {
			sb.append("...");
			offset += len - limit2;
			for (int i = 0; i < limit2; i++) {
				int b = bytes[offset + i];
				if (i > 0)
					sb.append('-');
				sb.append((char)num2Hex((b >> 4) & 0xf));
				sb.append((char)num2Hex(b & 0xf));
			}
		}
		return sb.toString();
	}

	public static @NotNull String toStringWithLimit(byte @NotNull [] bytes, int limit1, int limit2) {
		return toStringWithLimit(bytes, 0, bytes.length, limit1, limit2);
	}

	// 十六进制字符串转成二进制数组. 大小写的A~F都支持,忽略其它字符
	public static void toBytes(@NotNull String hex, byte @NotNull [] bytes, int offset) {
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
	public static byte @NotNull [] toBytes(@NotNull String hex) {
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
