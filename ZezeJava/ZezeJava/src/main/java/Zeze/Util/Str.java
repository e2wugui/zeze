package Zeze.Util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Map;
import java.util.Objects;
import Zeze.Net.Binary;
import org.apache.logging.log4j.message.ParameterizedMessageFactory;
import org.jspecify.annotations.Nullable;

/** 字符串工具集：格式化、缩进、字符集查找、大小/版本号解析与 {var} 模板展开。 */
public final class Str {
	public static final ParameterizedMessageFactory Formatter = new ParameterizedMessageFactory();
	public static final int INDENT_MAX = 64;
	private static final String[] INDENTS = new String[INDENT_MAX];
	private static final String[] EMPTY = new String[0];

	private Str() {
	}

	public static Charset lookupCharset(String csn) throws UnsupportedEncodingException {
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

	public static String format(String f, @Nullable Object... params) {
		return Formatter.newMessage(f, params).getFormattedMessage();
	}

	public static String indent(int n) {
		if (n <= 0)
			return "";
		if (n >= INDENT_MAX)
			n = INDENT_MAX - 1;
		return INDENTS[n];
	}

	public static String [] trim(@Nullable String @Nullable [] strs) {
		if (strs == null)
			return EMPTY;
		int n = 0;
		for (int i = 0; i < strs.length; i++) {
			var str = strs[i];
			if (str != null && !(strs[i] = str.trim()).isEmpty())
				n++;
		}
		if (n == strs.length)
			return (String[])strs;
		if (n == 0)
			return EMPTY;
		String[] newStrs = new String[n];
		for (int i = 0, j = 0; i < strs.length; i++) {
			var str = strs[i];
			if (str != null && !str.isEmpty())
				newStrs[j++] = str;
		}
		return newStrs;
	}

	public static String stacktrace(Throwable ex) {
		try (var out = new ByteArrayOutputStream();
			 var ps = new PrintStream(out, false, StandardCharsets.UTF_8)) {
			ex.printStackTrace(ps);
			return out.toString(StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw Task.forceThrow(e);
		}
	}

	public static String fromBinary(Binary b) {
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
		char[] buf = new char[s.length()];
		int pos = 0;
		long scale = 1;
		loop:
		for (int i = 0, n = s.length(); i < n; i++) {
			char c = s.charAt(i);
			switch (c) {
			//@formatter:off
			case '0': case '1': case '2': case '3': case '4':
			case '5': case '6': case '7': case '8': case '9': case '.':
					buf[pos++] = c;
				break;
			//@formatter:on
			case 'K':
			case 'k':
				scale = 1 << 10;
				checkUnitTail(s, i);
				break loop;
			case 'M':
			case 'm':
				scale = 1 << 20;
				checkUnitTail(s, i);
				break loop;
			case 'G':
			case 'g':
				scale = 1 << 30;
				checkUnitTail(s, i);
				break loop;
			case 'T':
			case 't':
				scale = 1L << 40;
				checkUnitTail(s, i);
				break loop;
			case 'P':
			case 'p':
				scale = 1L << 50;
				checkUnitTail(s, i);
				break loop;
			case 'E':
			case 'e':
				scale = 1L << 60;
				checkUnitTail(s, i);
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
		BigDecimal value;
		try {
			// 精确缩放后再截断，避免 double 舍入误差或溢出饱和
			value = new BigDecimal(buf, 0, pos).multiply(BigDecimal.valueOf(scale));
		} catch (NumberFormatException e) {
			throw new NumberFormatException("invalid number '" + s + "'");
		}
		if (value.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) > 0)
			throw new IllegalStateException("long overflow for '" + s + "'");
		return value.longValue();
	}

	// 单位字符后仅允许间隔符，非法尾字符 fail-fast
	private static void checkUnitTail(String s, int unitIndex) {
		for (int i = unitIndex + 1, n = s.length(); i < n; i++) {
			char c = s.charAt(i);
			switch (c) {
			//@formatter:off
			case '\t': case ' ': case '_': case ',': case '\'':
			//@formatter:on
				continue; // 允许用的间隔符
			default:
				throw new NumberFormatException("invalid char '" + c + "' after unit in '" + s
						+ "' (unit is a single letter, size ends at it: '" + s.substring(0, unitIndex + 1) + "')");
			}
		}
	}

	public static long parseVersion(String version) {
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
					throw new NumberFormatException(version); // 仅支持 4 段版本号，超出 fail-fast
				v += (long)t << s;
				t = 0;
				s -= 16;
			} else if (!Character.isSpaceChar(c))
				throw new NumberFormatException(version);
		}
		return v + ((long)t << s);
	}

	public static String toVersionStr(long version) {
		return String.format("%d.%d.%d.%d",
				version >>> 48, (version >> 32) & 0xffff, (version >> 16) & 0xffff, version & 0xffff);
	}

	public static String format(String str, Map<String, Object> params) {
		var sb = new StringBuilder();
		String varName;
		var fromIndex = new OutInt(0);
		while ((varName = parseVar(sb, str, fromIndex)) != null) {
			var p = params.get(varName);
			if (p == null)
				throw new IllegalArgumentException("var name not found. " + varName);

			// 字面文本直接拼接不经 Formatter（避免 '%' 触发格式异常），浮点按 %f 保持既有输出格式
			if (p instanceof Float || p instanceof Double)
				sb.append(String.format("%f", p));
			else
				sb.append(p);
		}
		return sb.toString();
	}

	private static @Nullable String parseVar(StringBuilder sb, String str,
											 OutInt fromIndex) {
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
