package Zeze.Util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import Zeze.Net.Binary;
import org.apache.logging.log4j.message.ParameterizedMessageFactory;

public final class Str {
	public static final ParameterizedMessageFactory Formatter = new ParameterizedMessageFactory();
	public static final int INDENT_MAX = 64;
	private static final String[] INDENTS = new String[INDENT_MAX];
	private static final String[] EMPTY = new String[0];

	private Str() {
	}

	static {
		for (int i = 0; i < INDENT_MAX; i++)
			INDENTS[i] = " ".repeat(i);
	}

	public static String format(String f, Object... params) {
		return Formatter.newMessage(f, params).getFormattedMessage();
	}

	public static String indent(int n) {
		if (n <= 0)
			return "";
		if (n >= INDENT_MAX)
			n = INDENT_MAX - 1;
		return INDENTS[n];
	}

	public static String[] trim(String[] strs) {
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

	public static String stacktrace(Throwable ex) {
		try (var out = new ByteArrayOutputStream();
			 var ps = new PrintStream(out, false, StandardCharsets.UTF_8)) {
			ex.printStackTrace(ps);
			return out.toString(StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static String fromBinary(Binary b) {
		return new String(b.bytesUnsafe(), b.getOffset(), b.size(), StandardCharsets.UTF_8);
	}

	public static int parseIntSize(String s) {
		if ((s = s.trim()).equalsIgnoreCase("max"))
			return Integer.MAX_VALUE;
		long v = parseLongSize(s);
		if (v > Integer.MAX_VALUE)
			throw new NumberFormatException("int overflow for '" + s + "'");
		return (int)v;
	}

	public static long parseLongSize(String s) {
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
		try {
			num = (Number)JsonReader.local().buf(buf).parseNumber();
			if (num instanceof Long || num instanceof Integer)
				return Math.multiplyExact(num.longValue(), scale);
		} catch (Exception e) {
			throw new IllegalStateException("long overflow for '" + s + "'", e);
		}
		double v = num.doubleValue() * scale;
		if (!Double.isFinite(v))
			throw new IllegalStateException("double overflow for '" + s + "'");
		if (v < 0 || v > Long.MAX_VALUE)
			throw new IllegalStateException("long overflow for '" + s + "'");
		return (long)v;
	}
}
