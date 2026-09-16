package Zeze.Util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
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
		buf[pos] = ' ';
		Number num;
		var jr = JsonReader.local();
		if (jr.buf() != null) // 重入保护：外层解析进行中时改用独立实例，避免内层 buf()/reset() 破坏外层状态（同 Json 静态入口）
			jr = new JsonReader();
		try {
			num = (Number)jr.buf(buf).parseNumber();
			if (num instanceof Long || num instanceof Integer)
				return Math.multiplyExact(num.longValue(), scale);
		} catch (NumberFormatException e) {
			// FND6-05：空数字串（空值/纯单位配置）按数字格式错误直通，与本方法非法字符路径
			// 的异常类型一致；不伪装成溢出。
			throw new NumberFormatException("invalid number '" + s + "'");
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

	// FND7-53：单位字符之后仅允许间隔符到串尾，否则"1e3"按1E、"10Mx"按10M被静默接受
	// ——与本方法对单位前非法字符的fail-fast语义自相矛盾，畸形配置无告警进入内存分配。
	private static void checkUnitTail(@NotNull String s, int unitIndex) {
		for (int i = unitIndex + 1, n = s.length(); i < n; i++) {
			char c = s.charAt(i);
			switch (c) {
			//@formatter:off
			case '\t': case ' ': case '_': case ',': case '\'':
			//@formatter:on
				continue; // 允许用的间隔符
			default:
				// R3-U2（B）：用户既有部署配置的双字母单位习惯（redis的100mb、SI的64KB）从静默
				// 按单字符接受变为启动fail-fast——消息自带等效合法写法（截到单位字符为止），
				// 迁移零思考成本。
				throw new NumberFormatException("invalid char '" + c + "' after unit in '" + s
						+ "' (unit is a single letter, size ends at it: '" + s.substring(0, unitIndex + 1) + "')");
			}
		}
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
					// FND6-05口径的fail-fast（FND6-06）：4段是既定格式契约，第5个'.'起原静默break
					// 丢弃——"1.2.3.4.5"与"1.2.3.4"解析相等，第5段差异被忽略，配置错误无告警。
					throw new NumberFormatException(version);
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
		var sb = new StringBuilder();
		String varName;
		var fromIndex = new OutInt(0);
		while ((varName = parseVar(sb, str, fromIndex)) != null) {
			var p = params.get(varName);
			if (p == null)
				throw new IllegalArgumentException("var name not found. " + varName);

			// 模板契约是"普通文本+{var}占位"：字面文本不经任何格式符解释，'%'是普通字符
			// （FND4-12：原先把字面段喂给Formatter，一个%即抛UnknownFormatConversion或
			// %n/%s等合法符静默注入/吞参错位）。仅参数值按原Formatter类型语义渲染保持
			// 既有输出不变：浮点%f（定点6位小数），其余（%b/%c/%d/%s）与String.valueOf一致。
			if (p instanceof Float || p instanceof Double)
				sb.append(String.format("%f", (Number)p));
			else
				sb.append(p);
		}
		return sb.toString();
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
