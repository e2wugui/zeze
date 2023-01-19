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
		long r = parseLongSize(s);
		int i = (int)r;
		if (i != r)
			throw new NumberFormatException("int overflow for '" + s + "'");
		return i;
	}

	public static long parseLongSize(String s) {
		long r = 0;
		for (int i = 0, n = s.length(); i < n; i++) {
			int c = s.charAt(i);
			switch (c) {
			//@formatter:off
			case '0': case '1': case '2': case '3': case '4':
			case '5': case '6': case '7': case '8': case '9':
				r = Math.addExact(Math.multiplyExact(r, 10), c - '0');
				break;
			//@formatter:on
			case 'K':
			case 'k':
				return Math.multiplyExact(r, 1 << 10);
			case 'M':
			case 'm':
				return Math.multiplyExact(r, 1 << 20);
			case 'G':
			case 'g':
				return Math.multiplyExact(r, 1 << 30);
			case 'T':
			case 't':
				return Math.multiplyExact(r, 1L << 40);
			case 'P':
			case 'p':
				return Math.multiplyExact(r, 1L << 50);
			case 'E':
			case 'e':
				return Math.multiplyExact(r, 1L << 60);
			case '\t':
			case ' ':
			case '_':
			case ',':
			case '\'':
				continue; // 允许用的间隔符
			default:
				throw new NumberFormatException("invalid format for '" + s + "'");
			}
		}
		return r;
	}
}
