package Zeze.Util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.Map;
import java.util.Objects;
import Zeze.Net.Binary;
import org.apache.logging.log4j.message.ParameterizedMessageFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Str {
	public static final ParameterizedMessageFactory Formatter = new ParameterizedMessageFactory();
	public static final int INDENT_MAX = 64;
	private static final String[] INDENTS = new String[INDENT_MAX];
	private static final String[] EMPTY = new String[0];

	private Str() {
	}

	public static @NotNull Charset lookupCharset(@NotNull String csn) throws UnsupportedEncodingException {
		Objects.requireNonNull(csn);
		try {
			return Charset.forName(csn);
		} catch (UnsupportedCharsetException | IllegalCharsetNameException x) {
			throw new UnsupportedEncodingException(csn);
		}
	}

	static {
		for (int i = 0; i < INDENT_MAX; i++)
			INDENTS[i] = " ".repeat(i);
	}

	public static @NotNull String format(@NotNull String f, @Nullable Object... params) {
		return Formatter.newMessage(f, params).getFormattedMessage();
	}

	public static @NotNull String indent(int n) {
		if (n <= 0)
			return "";
		if (n >= INDENT_MAX)
			n = INDENT_MAX - 1;
		return INDENTS[n];
	}

	public static String @NotNull [] trim(String @Nullable [] strs) {
		if (strs == null)
			return EMPTY;
		int n = 0;
		for (int i = 0; i < strs.length; i++)
			if (strs[i] != null && !(strs[i] = strs[i].trim()).isEmpty())
				n++;
		if (n == strs.length)
			return strs;
		if (n == 0)
			return EMPTY;
		String[] newStrs = new String[n];
		for (int i = 0, j = 0; i < strs.length; i++)
			if (strs[i] != null && !strs[i].isEmpty())
				newStrs[j++] = strs[i];
		return newStrs;
	}

	public static @NotNull String stacktrace(@NotNull Throwable ex) {
		try (var out = new ByteArrayOutputStream();
			 var ps = new PrintStream(out, false, StandardCharsets.UTF_8)) {
			ex.printStackTrace(ps);
			return out.toString(StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw Task.forceThrow(e);
		}
	}

	public static @NotNull String fromBinary(@NotNull Binary b) {
		return new String(b.bytesUnsafe(), b.getOffset(), b.size(), StandardCharsets.UTF_8);
	}

	public static int parseIntSize(@Nullable String s) {
		return parseIntSize(s, -1);
	}

	public static int parseIntSize(@Nullable String s, int defSize) {
		if (s == null)
			return defSize;
		if ((s = s.trim()).equalsIgnoreCase("max"))
			return Integer.MAX_VALUE;
		long v = parseLongSize(s, defSize);
		if (v > Integer.MAX_VALUE)
			throw new NumberFormatException("int overflow for '" + s + "'");
		return (int)v;
	}

	public static long parseLongSize(@Nullable String s) {
		return parseLongSize(s, -1);
	}

	public static long parseLongSize(@Nullable String s, long defSize) {
		if (s == null)
			return defSize;
		if ((s = s.trim()).equalsIgnoreCase("max"))
			return Long.MAX_VALUE;
		byte[] buf = new byte[s.length() + 1];
		int pos = 0;
		long scale = 1;
		loop:
		for (int i = 0, n = s.length(); i < n; i++) {
			char c = s.charAt(i);
			switch (c) {
			//@formatter:off
			case '0': case '1': case '2': case '3': case '4':
			case '5': case '6': case '7': case '8': case '9': case '.':
				buf[pos++] = (byte)c;
				break;
			//@formatter:on
			case 'K':
			case 'k':
				scale = 1 << 10;
				break loop;
			case 'M':
			case 'm':
				scale = 1 << 20;
				break loop;
			case 'G':
			case 'g':
				scale = 1 << 30;
				break loop;
			case 'T':
			case 't':
				scale = 1L << 40;
				break loop;
			case 'P':
			case 'p':
				scale = 1L << 50;
				break loop;
			case 'E':
			case 'e':
				scale = 1L << 60;
				break loop;
			case '\t':
			case ' ':
			case '_':
			case ',':
			case '\'':
				continue; // 允许用的间隔符
			default:
				throw new NumberFormatException("invalid char '" + c + "' in '" + s + "'");
			}
		}
		buf[pos] = ' ';
		Number num;
		var jr = JsonReader.local();
		try {
			num = (Number)jr.buf(buf).parseNumber();
			if (num instanceof Long || num instanceof Integer)
				return Math.multiplyExact(num.longValue(), scale);
		} catch (Exception e) {
			throw new IllegalStateException("long overflow for '" + s + "'", e);
		} finally {
			jr.reset();
		}
		double v = num.doubleValue() * scale;
		if (!Double.isFinite(v))
			throw new IllegalStateException("double overflow for '" + s + "'");
		if (v < 0 || v > Long.MAX_VALUE)
			throw new IllegalStateException("long overflow for '" + s + "'");
		return (long)v;
	}

	public static long parseVersion(@NotNull String version) {
		long v = 0;
		int t = 0, s = 48;
		for (int i = 0, n = version.length(); i < n; i++) {
			int c = version.charAt(i);
			if (c >= '0' && c <= '9') {
				t = t * 10 + c - '0';
				if (t > 0xffff)
					throw new NumberFormatException(version);
			} else if (c == '.') {
				if (s == 0)
					break;
				v += (long)t << s;
				t = 0;
				s -= 16;
			} else if (!Character.isSpaceChar(c))
				throw new NumberFormatException(version);
		}
		return v + ((long)t << s);
	}

	public static @NotNull String toVersionStr(long version) {
		return String.format("%d.%d.%d.%d",
				version >>> 48, (version >> 32) & 0xffff, (version >> 16) & 0xffff, version & 0xffff);
	}

	public static @NotNull String format(@NotNull String str, @NotNull Map<String, Object> params) {
		var formatSb = new StringBuilder();
		var paramsList = new ArrayList<>();
		String varName;
		var fromIndex = new OutInt(0);
		while ((varName = parseVar(formatSb, str, fromIndex)) != null) {
			var p = params.get(varName);
			if (p == null)
				throw new IllegalArgumentException("var name not found. " + varName);

			// 支持格式化更多类型
			if (p instanceof Boolean) {
				formatSb.append("%b");
				paramsList.add(p);
			} else if (p instanceof Character) {
				formatSb.append("%c");
				paramsList.add(p);
			} else if (p instanceof Byte || p instanceof Short
					|| p instanceof Integer || p instanceof Long
					|| p instanceof BigInteger) {
				formatSb.append("%d");
				paramsList.add(p);
			} else if (p instanceof Float || p instanceof Double) {
				formatSb.append("%f");
				paramsList.add(p);
			} else if (p instanceof Date) {
				formatSb.append("%t");
				paramsList.add(p);
			} else if (p instanceof String) {
				formatSb.append("%s");
				paramsList.add(p);
			} else {
				formatSb.append("%s");
				paramsList.add(p);
			}
		}
		var newFormat = formatSb.toString();
		//System.out.println(newFormat);
		//return newFormat.formatted(paramsList.toArray());
		return new Formatter().format(newFormat, paramsList.toArray()).toString();
	}

	private static @Nullable String parseVar(@NotNull StringBuilder sb, @NotNull String str,
											 @NotNull OutInt fromIndex) {
		var quoteLeft = str.indexOf('{', fromIndex.value);
		if (quoteLeft < 0) {
			sb.append(str, fromIndex.value, str.length());
			return null;
		}
		var quoteRight = str.indexOf('}', quoteLeft + 1);
		if (quoteRight < 0) {
			sb.append(str, fromIndex.value, str.length());
			return null;
		}
		sb.append(str, fromIndex.value, quoteLeft);
		fromIndex.value = quoteRight + 1;
		return str.substring(quoteLeft + 1, quoteRight);
	}
}
