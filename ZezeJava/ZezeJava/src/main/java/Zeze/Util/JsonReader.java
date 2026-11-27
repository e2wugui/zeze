package Zeze.Util;

import java.util.Objects;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import static Zeze.Util.Json.*;

/**
 * 流式 JSON / JSON5 读取解析器：直接操作 UTF-8 字节缓冲，供 {@link Json} 反射解析与手工调用。
 * 支持 JSON5 扩展：无引号标识符键、单尾随逗号、单引号与跨行续写字符串、十六进制与前导/尾随小数点数值、
 * {@code Infinity}/{@code NaN}、显式 {@code +} 号及单行/多行注释。
 */
public final class JsonReader {
	private static final Double NEGATIVE_INFINITY = Double.NEGATIVE_INFINITY;
	private static final Double POSITIVE_INFINITY = Double.POSITIVE_INFINITY;

	// 默认嵌套深度上限（1024），远大于 JsonWriter 默认上限 16 且避免深嵌套递归栈溢出。
	public static final int DEFAULT_MAX_DEPTH = 1024;

	private static final byte[] ESCAPE = { // @formatter:off
		//   0    1    2    3    4    5    6    7    8    9    A    B    C    D    E    F
			' ', '!', '"', '#', '$', '%', '&','\'', '(', ')', '*', '+', ',', '-', '.', '/', // 0x2x
			'\0','1', '2', '3', '4', '5', '6', '7', '8', '9', ':', ';', '<', '=', '>', '?', // 0x3x
			'@', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', // 0x4x
			'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '[','\\', ']', '^', '_', // 0x5x
			'`', 'a','\b', 'c', 'd', 'e','\f', 'g', 'h', 'i', 'j', 'k', 'l', 'm','\n', 'o', // 0x6x
			'p', 'q','\r', 's','\t', 'u',0x0b, 'w', 'x', 'y', 'z', '{', '|', '}', '~', '?', // 0x7x
	}; //@formatter:on

	private static final double[] EXP = { // 1e0...1e308: 309 * 8 bytes = 2472 bytes
		1e0, 1e1, 1e2, 1e3, 1e4, 1e5, 1e6, 1e7, 1e8, 1e9, 1e10, 1e11, 1e12, 1e13, 1e14, 1e15, 1e16, 1e17, 1e18,
		1e19, 1e20, 1e21, 1e22, 1e23, 1e24, 1e25, 1e26, 1e27, 1e28, 1e29, 1e30, 1e31, 1e32, 1e33, 1e34, 1e35, 1e36,
		1e37, 1e38, 1e39, 1e40, 1e41, 1e42, 1e43, 1e44, 1e45, 1e46, 1e47, 1e48, 1e49, 1e50, 1e51, 1e52, 1e53, 1e54,
		1e55, 1e56, 1e57, 1e58, 1e59, 1e60, 1e61, 1e62, 1e63, 1e64, 1e65, 1e66, 1e67, 1e68, 1e69, 1e70, 1e71, 1e72,
		1e73, 1e74, 1e75, 1e76, 1e77, 1e78, 1e79, 1e80, 1e81, 1e82, 1e83, 1e84, 1e85, 1e86, 1e87, 1e88, 1e89, 1e90,
		1e91, 1e92, 1e93, 1e94, 1e95, 1e96, 1e97, 1e98, 1e99, 1e100, 1e101, 1e102, 1e103, 1e104, 1e105, 1e106,
		1e107, 1e108, 1e109, 1e110, 1e111, 1e112, 1e113, 1e114, 1e115, 1e116, 1e117, 1e118, 1e119, 1e120, 1e121,
		1e122, 1e123, 1e124, 1e125, 1e126, 1e127, 1e128, 1e129, 1e130, 1e131, 1e132, 1e133, 1e134, 1e135, 1e136,
		1e137, 1e138, 1e139, 1e140, 1e141, 1e142, 1e143, 1e144, 1e145, 1e146, 1e147, 1e148, 1e149, 1e150, 1e151,
		1e152, 1e153, 1e154, 1e155, 1e156, 1e157, 1e158, 1e159, 1e160, 1e161, 1e162, 1e163, 1e164, 1e165, 1e166,
		1e167, 1e168, 1e169, 1e170, 1e171, 1e172, 1e173, 1e174, 1e175, 1e176, 1e177, 1e178, 1e179, 1e180, 1e181,
		1e182, 1e183, 1e184, 1e185, 1e186, 1e187, 1e188, 1e189, 1e190, 1e191, 1e192, 1e193, 1e194, 1e195, 1e196,
		1e197, 1e198, 1e199, 1e200, 1e201, 1e202, 1e203, 1e204, 1e205, 1e206, 1e207, 1e208, 1e209, 1e210, 1e211,
		1e212, 1e213, 1e214, 1e215, 1e216, 1e217, 1e218, 1e219, 1e220, 1e221, 1e222, 1e223, 1e224, 1e225, 1e226,
		1e227, 1e228, 1e229, 1e230, 1e231, 1e232, 1e233, 1e234, 1e235, 1e236, 1e237, 1e238, 1e239, 1e240, 1e241,
		1e242, 1e243, 1e244, 1e245, 1e246, 1e247, 1e248, 1e249, 1e250, 1e251, 1e252, 1e253, 1e254, 1e255, 1e256,
		1e257, 1e258, 1e259, 1e260, 1e261, 1e262, 1e263, 1e264, 1e265, 1e266, 1e267, 1e268, 1e269, 1e270, 1e271,
		1e272, 1e273, 1e274, 1e275, 1e276, 1e277, 1e278, 1e279, 1e280, 1e281, 1e282, 1e283, 1e284, 1e285, 1e286,
		1e287, 1e288, 1e289, 1e290, 1e291, 1e292, 1e293, 1e294, 1e295, 1e296, 1e297, 1e298, 1e299, 1e300, 1e301,
		1e302, 1e303, 1e304, 1e305, 1e306, 1e307, 1e308
	};

	private static final ThreadLocal<JsonReader> localReaders = ensureNotNull(
		ThreadLocal.withInitial(JsonReader::new));
	private static @Nullable String @Nullable [] poolStrs;
	private static int poolSize = 1024;

	public static JsonReader local() {
		return ensureNotNull(localReaders.get());
	}

	public static void removeLocal() {
		localReaders.remove();
	}

	public static void resetStringPool(int size) {
		poolSize = 1 << (32 - Integer.numberOfLeadingZeros(size - 1));
		poolStrs = null;
	}

	public static void clearStringPool() {
		@Nullable String[] ss = poolStrs;
		if (ss != null)
			Arrays.fill(ss, null);
	}

	static String intern(byte[] buf, int pos, int end) {
		int len = end - pos;
		@Nullable String[] ss = poolStrs;
		if (ss == null)
			poolStrs = ss = new String[poolSize];
		int idx = getKeyHash(buf, pos, end) & (ss.length - 1);
		String s = ss[idx];
		if (s != null) { // LATIN1（compact strings），直接比对String内部byte[]
			byte[] b = (byte[])unsafe.getObject(s, STRING_VALUE_OFFSET);
			if (b.length == len && Arrays.equals(b, 0, len, buf, pos, end))
				return s;
		}
		ss[idx] = s = newByteString(buf, pos, end);
		return s;
	}

	private byte @Nullable [] buf; // only support utf-8 encoding
	private int pos;
	private char @Nullable [] tmp; // for parseString & parseStringNoQuot
	private int depth; // current container nesting depth
	private int maxDepth = DEFAULT_MAX_DEPTH;

	public JsonReader() {
	}

	public JsonReader(byte[] b) {
		buf = b;
	}

	public JsonReader(byte[] b, int p) {
		buf = b;
		pos = p;
	}

	public JsonReader reset() {
		buf = null;
		pos = 0;
		tmp = null;
		depth = 0;
		return this;
	}

	public byte @Nullable [] buf() {
		return buf;
	}

	public JsonReader buf(String s) {
		buf = s.getBytes(StandardCharsets.UTF_8);
		pos = 0;
		depth = 0;
		return this;
	}

	public JsonReader buf(byte[] b) {
		buf = b;
		pos = 0;
		depth = 0;
		return this;
	}

	public JsonReader buf(byte[] b, int p) {
		buf = b;
		pos = p;
		depth = 0;
		return this;
	}

	public int pos() {
		return pos;
	}

	public JsonReader pos(int p) {
		pos = p;
		return this;
	}

	public JsonReader pos(Pos p) {
		pos = p.pos;
		return this;
	}

	public int getMaxDepth() {
		return maxDepth;
	}

	public JsonReader setMaxDepth(int depth) {
		this.maxDepth = depth;
		return this;
	}

	public char @Nullable [] tmp() {
		return tmp;
	}

	public JsonReader tmp(char[] t) {
		tmp = t;
		return this;
	}

	public JsonReader tmp(int size) {
		if (tmp == null || tmp.length < size)
			tmp = new char[size];
		return this;
	}

	public JsonReader skip(int n) {
		pos += n;
		return this;
	}

	public JsonReader trySkipBom() {
		int p = pos;
		//noinspection DataFlowIssue
		if (p + 2 < buf.length && buf[p] == (byte)0xef && buf[p + 1] == (byte)0xbb && buf[p + 2] == (byte)0xbf)
			pos = p + 3;
		return this;
	}

	public boolean end() {
		//noinspection DataFlowIssue
		return end(buf.length);
	}

	public boolean end(int len) {
		for (int b; pos < len; pos++)
			//noinspection DataFlowIssue
			if ((b = buf[pos] & 0xff) > ' ') {
				if (b != '/')
					return false;
				if (pos + 1 >= len)
					return true; // 尾部孤立 '/' 按未终结行注释至 EOF 处理
				if (buf[++pos] == '*') {
					for (pos++; pos < len; pos++)
						if (buf[pos] == '*' && pos + 1 < len && buf[pos + 1] == '/') {
							pos++; // 停在 '/' 上，由外层 pos++ 越过
							break;
						}
				} else
					for (; pos < len; pos++) {
						int c = buf[pos] & 0xff;
						if (c == '\n' || c == '\r')
							break;
						if (isUnicodeLineSeparator(pos, len)) {
							pos += 2; // 外层循环越过最后一个 UTF-8 字节
							break;
						}
					}
			}
		return true;
	}

	public int next() {
		for (int b; ; pos++) {
			//noinspection DataFlowIssue
			if (pos >= buf.length)
				return 0; // 输入耗尽返回 NUL 哨兵（实字节 0x00 已被视为空白跳过，不会碰撞）
			if ((b = buf[pos] & 0xff) > ' ') {
				if (b != '/')
					return b;
				skipComment();
			}
		}
	}

	public int skipNext() {
		for (int b; ; ) {
			//noinspection DataFlowIssue
			if (++pos >= buf.length)
				return 0; // 输入耗尽返回 NUL 哨兵
			if ((b = buf[pos] & 0xff) > ' ') {
				if (b != '/')
					return b;
				skipComment();
			}
		}
	}

	public int skipVar(int e) { // ']' or '}'
		for (int b, c; ; ) {
			//noinspection DataFlowIssue
			if (pos >= buf.length)
				return e; // 输入耗尽按遇到闭合定界符宽容收尾
			if ((b = buf[pos]) == ',' || b == '\n')
				for (; ; ) {
					if (++pos >= buf.length)
						return e;
					if (((((b = buf[pos]) & 0xff) - ' ' - 1) ^ (',' - ' ' - 1)) > 0) { // (b & 0xff) > ' ' && b != ','
						if (b != '/')
							return b;
						skipComment();
					}
				}
			if (b == e)
				return b;
			pos++;
			if (b < '"')
				continue;
			if (b == '"' || b == '\'') {
				while (pos < buf.length && (c = buf[pos++]) != b)
					if (c == '\\')
						pos++; // 反斜杠在末字节时 pos 越界一位，由循环条件拦截
			} else if ((b | 0x20) == '{') { // '['(0x5B) | 0x20 == '{'(0x7B)
				// 引号须按原始字节判定；[/{ 与 ]/} 仅作嵌套层级配对
				for (int level = 0; pos < buf.length && ((b = buf[pos++]) != '}' && b != ']' || --level >= 0); ) {
					if (b == '"' || b == '\'') {
						while (pos < buf.length && (c = buf[pos++]) != b)
							if (c == '\\')
								pos++;
					} else if (b == '{' || b == '[')
						level++;
					else if (b == '/') {
						pos--;
						skipComment();
						pos++; // 越过块注释结束 '/'
					}
				}
			} else if (b == '/') {
				pos--;
				skipComment();
				pos++; // skipComment 停在块注释结束 '/' 上，外层循环不自增 pos，须手动越过
			}
		}
	}

	private void skipComment() {
		//noinspection DataFlowIssue
		if (pos + 1 >= buf.length)
			return;
		if (buf[++pos] == '*') {
			for (pos++; ; ) {
				if (pos >= buf.length)
					return;
				if (buf[pos++] == '*' && pos < buf.length && buf[pos] == '/')
					return; // pos 停在关闭 '/' 上（调用方负责越过）
			}
		}
		// 行注释结束于 LF/CR/CRLF/U+2028/U+2029 或 EOF，pos 停在行终止符末字节
		for (; pos < buf.length; pos++) {
			int b = buf[pos] & 0xff;
			if (b == '\n' || b == '\r')
				return;
			if (isUnicodeLineSeparator(pos, buf.length)) {
				pos += 2;
				return;
			}
		}
	}

	// U+2028/U+2029 的 UTF-8 编码（E2 80 A8/A9）
	private boolean isUnicodeLineSeparator(int p, int limit) {
		//noinspection DataFlowIssue
		return p + 2 < limit && (buf[p] & 0xff) == 0xE2
			&& (buf[p + 1] & 0xff) == 0x80
			&& (((buf[p + 2] & 0xff) == 0xA8) || ((buf[p + 2] & 0xff) == 0xA9));
	}

	// 尾数为零时溢出指数仍为零，避免溢出分支将 0e999 误判为饱和值/Infinity
	private static boolean isZeroMantissa(long i, double d) {
		return i == 0 && d == 0.0;
	}

	public int skipColon() {
		int b = next();
		return b == ':' ? skipNext() : b;
	}

	public void skipQuot(int e) {
		final byte[] buffer = Objects.requireNonNull(buf, "JSON input is not set");
		for (int b; pos < buffer.length && (b = buffer[pos++]) != e; )
			if (b == '\\')
				pos++;
		if (pos > buffer.length)
			pos = buffer.length; // 末字节为 '\' 时防越界
	}

	public @Nullable Object parse() throws ReflectiveOperationException {
		return parse(null, next());
	}

	@SuppressWarnings("unchecked")
	@Nullable Object parse(@Nullable Object obj, int b) throws ReflectiveOperationException {
		//noinspection EnhancedSwitchMigration
		switch (b) { //@formatter:off
		case '{': return parseMap0(obj instanceof Map ? (Map<String, @Nullable Object>) obj : null);
		case '[': return parseArray0(obj instanceof Collection ? (Collection<@Nullable Object>) obj : null);
		case '"': case '\'': return parseString(b, false);
		case '0': case '1': case '2': case '3': case '4': case '5': case '6':
		case '7': case '8': case '9': case '-': case '+': case '.':
		case 'I': case 'i': case 'N': return parseNumber();
		case 'n': return null; // 写侧恒写小写 null 与大写 NaN/Infinity，首字符即可区分
		case 'f': case 'F': return false;
		case 't': case 'T': return true;
		} //@formatter:on
		return null;
	}

	public @Nullable Collection<@Nullable Object> parseArray(@Nullable Collection<@Nullable Object> c) throws ReflectiveOperationException {
		return next() == '[' ? parseArray0(c) : c;
	}

	Collection<@Nullable Object> parseArray0(@Nullable Collection<@Nullable Object> c) throws ReflectiveOperationException {
		if (depth >= maxDepth)
			throw new IllegalStateException("json nesting depth exceeds " + maxDepth);
		depth++;
		try {
			if (c == null)
				c = new ArrayList<>();
			for (int b = skipNext(); b != ']'; b = skipVar(']'))
				c.add(parse(null, b));
			pos++;
			return c;
		} finally {
			depth--;
		}
	}

	/** 按元素类型解析数组字段；元素 null 按类型默认值处理，非 '[' 起始返回 null。 */
	private @Nullable Object parseArrayField(Class<?> componentType, int b) throws ReflectiveOperationException {
		if (b != '[')
			return null;
		if (depth >= maxDepth)
			throw new IllegalStateException("json nesting depth exceeds " + maxDepth);
		depth++;
		try {
			var list = new ArrayList<>();
			for (int c = skipNext(); c != ']'; c = skipVar(']'))
				list.add(parseArrayElement(componentType, c));
			pos++;
			var array = java.lang.reflect.Array.newInstance(componentType, list.size());
			for (int i = 0; i < list.size(); i++)
				java.lang.reflect.Array.set(array, i, list.get(i));
			return array;
		} finally {
			depth--;
		}
	}

	private @Nullable Object parseArrayElement(Class<?> ct, int b) throws ReflectiveOperationException {
		if (ct == boolean.class)
			return b == 't';
		if (ct == int.class)
			return b == 'n' ? 0 : parseInt();
		if (ct == long.class)
			return b == 'n' ? 0L : parseLong();
		if (ct == double.class)
			return b == 'n' ? 0.0 : parseDouble();
		if (ct == float.class)
			return b == 'n' ? 0f : (float)parseDouble();
		if (ct == byte.class)
			return b == 'n' ? (byte)0 : (byte)parseInt();
		if (ct == short.class)
			return b == 'n' ? (short)0 : (short)parseInt();
		if (ct == char.class)
			return b == 'n' ? (char)0 : (char)parseInt();
		if (ct == String.class)
			return b == 'n' ? null : parseString(false);
		// 包装类型数组组件：保留 null，数值转换与原始类型一致
		if (ct == Integer.class)
			return b == 'n' ? null : parseInt();
		if (ct == Long.class)
			return b == 'n' ? null : parseLong();
		if (ct == Double.class)
			return b == 'n' ? null : parseDouble();
		if (ct == Float.class)
			return b == 'n' ? null : (float)parseDouble();
		if (ct == Byte.class)
			return b == 'n' ? null : (byte)parseInt();
		if (ct == Short.class)
			return b == 'n' ? null : (short)parseInt();
		if (ct == Character.class)
			return b == 'n' ? null : (char)parseInt();
		if (ct == Boolean.class)
			return b == 'n' ? null : b == 't';
		return parse(null, b); // Object等：走通用值解析
	}

	public <T> @Nullable Collection<@Nullable T> parseArray(@Nullable Collection<@Nullable T> c, Class<T> elemClass)
		throws ReflectiveOperationException {
		return parseArray(Json.instance, c, elemClass);
	}

	public <T> @Nullable Collection<@Nullable T> parseArray(Json json, @Nullable Collection<@Nullable T> c,
															Class<T> elemClass) throws ReflectiveOperationException {
		if (next() != '[')
			return c;
		if (depth >= maxDepth)
			throw new IllegalStateException("json nesting depth exceeds " + maxDepth);
		depth++;
		try {
			if (c == null)
				c = new ArrayList<>();
			ClassMeta<T> classMeta = json.getClassMeta(elemClass);
			Parser<T> parser = classMeta.parser;
			for (int b = skipNext(); b != ']'; b = skipVar(']'))
				c.add(b == 'n' ? null : parser != null
					? parser.parse(this, classMeta, null, null, null) : parse(json, null, classMeta));
			pos++;
			return c;
		} finally {
			depth--;
		}
	}

	public @Nullable Map<String, @Nullable Object> parseMap(@Nullable Map<String, @Nullable Object> m) throws ReflectiveOperationException {
		return next() == '{' ? parseMap0(m) : m;
	}

	Map<String, @Nullable Object> parseMap0(@Nullable Map<String, @Nullable Object> m) throws ReflectiveOperationException {
		if (depth >= maxDepth)
			throw new IllegalStateException("json nesting depth exceeds " + maxDepth);
		depth++;
		try {
			if (m == null)
				m = new HashMap<>();
			for (int b = skipNext(); b != '}'; b = skipVar('}')) {
				String k = parseStringKey(this, b);
				m.put(k, parse(null, skipColon()));
			}
			pos++;
			return m;
		} finally {
			depth--;
		}
	}

	public <T> @Nullable T parse(@Nullable Class<T> klass) throws ReflectiveOperationException {
		return klass != null ? parse((T)null, Json.instance.getClassMeta(klass)) : null;
	}

	public <T> @Nullable T parse(Json json, @Nullable Class<T> klass) throws ReflectiveOperationException {
		return klass != null ? parse(json, null, json.getClassMeta(klass)) : null;
	}

	public <T> @Nullable T parse(@Nullable ClassMeta<T> classMeta) throws ReflectiveOperationException {
		return parse((T)null, classMeta);
	}

	// 供自定义容器 parser 递归解析 value 时计入嵌套深度限制
	@Nullable Object parseNested(@Nullable ClassMeta<?> classMeta) throws ReflectiveOperationException {
		if (depth >= maxDepth)
			throw new IllegalStateException("json nesting depth exceeds " + maxDepth);
		depth++;
		try {
			return parse(classMeta);
		} finally {
			depth--;
		}
	}

	public <T> @Nullable T parse(@Nullable T obj) throws ReflectiveOperationException {
		return parse(obj, (ClassMeta<T>)null);
	}

	public <T> @Nullable T parse(@Nullable T obj, @Nullable Class<? super T> klass)
		throws ReflectiveOperationException {
		return parse(obj, klass != null ? Json.instance.getClassMeta(klass) : null);
	}

	public <T> @Nullable T parse(Json json, @Nullable T obj, @Nullable Class<? super T> klass)
		throws ReflectiveOperationException {
		return parse(json, obj, klass != null ? json.getClassMeta(klass) : null);
	}

	public <T> @Nullable T parse(@Nullable T obj, @Nullable ClassMeta<? super T> classMeta)
		throws ReflectiveOperationException {
		return parse(Json.instance, obj, classMeta);
	}

	public <T> @Nullable T parse(Json json, @Nullable T obj) throws ReflectiveOperationException {
		return parse(json, obj, (ClassMeta<T>)null);
	}

	@SuppressWarnings("unchecked")
	public <T> @Nullable T parse(Json json, @Nullable T obj, @Nullable ClassMeta<? super T> classMeta)
		throws ReflectiveOperationException {
		if (classMeta == null) {
			if (obj == null)
				return null;
			classMeta = json.getClassMeta((Class<T>)obj.getClass());
		} else {
			if (next() == 'n')
				return null;
			Parser<? super T> parser = classMeta.parser;
			if (parser != null)
				return (T)parser.parse0(this, classMeta, null, obj, null);
			if (classMeta.klass == Object.class)
				return (T)parse(obj, next());
			KeyReader kr = ClassMeta.getKeyReader(classMeta.klass);
			if (kr != null) {
				int b = next();
				// b==0 为输入耗尽哨兵，直接回退 null，避免传入 KeyReader 越界读
				return (T)(b == 0 ? null : kr.parse(this, b));
			}
		}
		if (next() == 'n')
			return null;
		Parser<? super T> parser = classMeta.parser;
		if (parser != null)
			return (T)parser.parse0(this, classMeta, null, obj, null);
		if (obj == null) {
			if (ClassMeta.isAbstract(classMeta.klass))
				throw new InstantiationException("abstract class: " + classMeta.klass.getName());
			obj = (T)classMeta.ctor.create();
		}
		// 顶层 Collection/Map 直接走数组/Map 解析，与写侧对称
		if (obj instanceof Collection<?>)
			return (T)parseArray((Collection<@Nullable Object>)obj);
		if (obj instanceof Map<?, ?>)
			return (T)parseMap((Map<String, @Nullable Object>)obj);
		return parse0(obj, classMeta);
	}

	public <T> T parse0(T obj, ClassMeta<?> classMeta) throws ReflectiveOperationException {
		if (next() != '{')
			return obj;
		if (depth >= maxDepth)
			throw new IllegalStateException("json nesting depth exceeds " + maxDepth);
		depth++;
		try {
			for (int b = skipNext(); b != '}'; b = skipVar('}')) {
				boolean quot = b == '"' || b == '\'';
				int keyBegin = pos + (quot ? 1 : 0);
				FieldMeta fm = classMeta.get(quot ? parseKeyHash(b) : parseKeyHashNoQuot(b));
				// 32 位哈希命中后按解码字节比对字段名，防哈希碰撞
				if (fm == null || !keyEquals(keyBegin, quot ? pos - 1 : pos, fm.name))
					continue;
				b = skipColon();
				long offset = fm.offset;
				int type = fm.type;
				switch (type) {
				case TYPE_BOOLEAN:
					unsafe.putBoolean(obj, offset, b == 't');
					break;
				case TYPE_BYTE:
					unsafe.putByte(obj, offset, (byte)parseInt());
					break;
				case TYPE_SHORT:
					unsafe.putShort(obj, offset, (short)parseInt());
					break;
				case TYPE_CHAR:
					unsafe.putChar(obj, offset, (char)parseInt());
					break;
				case TYPE_INT:
					unsafe.putInt(obj, offset, parseInt());
					break;
				case TYPE_LONG:
					unsafe.putLong(obj, offset, parseLong());
					break;
				case TYPE_FLOAT:
					unsafe.putFloat(obj, offset, (float)parseDouble());
					break;
				case TYPE_DOUBLE:
					unsafe.putDouble(obj, offset, parseDouble());
					break;
				case TYPE_STRING:
					unsafe.putObject(obj, offset, b == 'n' ? null : parseString(false));
					break;
				case TYPE_OBJECT: {
					if (b == 'n') {
						unsafe.putObject(obj, offset, null);
						break;
					}
					// 与强类型容器字段一致，复用对象重复解析前先清空旧容器内容
					Object oldObj = unsafe.getObject(obj, offset);
					if (oldObj instanceof Collection<?>)
						((Collection<?>)oldObj).clear();
					else if (oldObj instanceof Map<?, ?>)
						((Map<?, ?>)oldObj).clear();
					unsafe.putObject(obj, offset, parse(oldObj, b));
					break;
				}
				case TYPE_POS:
					Pos p = (Pos)unsafe.getObject(obj, offset);
					if (p == null)
						unsafe.putObject(obj, offset, p = new Pos());
					p.pos = pos;
					break;
				case TYPE_ARRAY_FLAG + TYPE_BOOLEAN:
				case TYPE_ARRAY_FLAG + TYPE_BYTE:
				case TYPE_ARRAY_FLAG + TYPE_SHORT:
				case TYPE_ARRAY_FLAG + TYPE_CHAR:
				case TYPE_ARRAY_FLAG + TYPE_INT:
				case TYPE_ARRAY_FLAG + TYPE_LONG:
				case TYPE_ARRAY_FLAG + TYPE_FLOAT:
				case TYPE_ARRAY_FLAG + TYPE_DOUBLE:
				case TYPE_ARRAY_FLAG + TYPE_STRING:
				case TYPE_ARRAY_FLAG + TYPE_OBJECT:
					if (b == 'n')
						unsafe.putObject(obj, offset, null);
					else
						unsafe.putObject(obj, offset, parseArrayField(fm.klass, b));
					break;
				case TYPE_CUSTOM:
					if (b == 'n') {
						unsafe.putObject(obj, offset, null);
						break;
					}
					Object subObj = unsafe.getObject(obj, offset);
					if (subObj != null) {
						Class<?> subClass = subObj.getClass();
						ClassMeta<?> subClassMeta;
						if (subClass == fm.klass) {
							subClassMeta = fm.classMeta;
							if (subClassMeta == null)
								fm.classMeta = subClassMeta = classMeta.json.getClassMeta(subClass);
						} else
							subClassMeta = classMeta.json.getClassMeta(subClass);
						Parser<?> parser = subClassMeta.parser;
						if (parser != null) {
							Object newSubObj = parser.parse0(this, subClassMeta, fm, subObj, obj);
							if (newSubObj != subObj) {
								if (newSubObj != null && !fm.klass.isAssignableFrom(newSubObj.getClass())) {
									throw new InstantiationException("incompatible type(" + newSubObj.getClass()
										+ ") for field: " + fm.getName() + " in " + classMeta.klass.getName());
								}
								unsafe.putObject(obj, offset, newSubObj);
							}
						} else
							parse0(subObj, subClassMeta);
					} else {
						ClassMeta<?> subClassMeta = fm.classMeta;
						if (subClassMeta == null)
							fm.classMeta = subClassMeta = classMeta.json.getClassMeta(fm.klass);
						Parser<?> parser = subClassMeta.parser;
						if (parser != null)
							subObj = parser.parse0(this, subClassMeta, fm, null, obj);
						else {
							if (ClassMeta.isAbstract(subClassMeta.klass)) {
								throw new InstantiationException(
									"abstract field: " + fm.getName() + " in " + classMeta.klass.getName());
							}
							subObj = parse0(subClassMeta.ctor.create(), subClassMeta);
						}
						unsafe.putObject(obj, offset, subObj);
					}
					break;
				case TYPE_WRAP_FLAG + TYPE_BOOLEAN:
					unsafe.putObject(obj, offset, b == 'n' ? null : b == 't');
					break;
				case TYPE_WRAP_FLAG + TYPE_BYTE:
					unsafe.putObject(obj, offset, b == 'n' ? null : (byte)parseInt());
					break;
				case TYPE_WRAP_FLAG + TYPE_SHORT:
					unsafe.putObject(obj, offset, b == 'n' ? null : (short)parseInt());
					break;
				case TYPE_WRAP_FLAG + TYPE_CHAR:
					unsafe.putObject(obj, offset, b == 'n' ? null : (char)parseInt());
					break;
				case TYPE_WRAP_FLAG + TYPE_INT:
					unsafe.putObject(obj, offset, b == 'n' ? null : parseInt());
					break;
				case TYPE_WRAP_FLAG + TYPE_LONG:
					unsafe.putObject(obj, offset, b == 'n' ? null : parseLong());
					break;
				case TYPE_WRAP_FLAG + TYPE_FLOAT:
					unsafe.putObject(obj, offset, b == 'n' ? null : (float)parseDouble());
					break;
				case TYPE_WRAP_FLAG + TYPE_DOUBLE:
					unsafe.putObject(obj, offset, b == 'n' ? null : parseDouble());
					break;
				default:
					int flag = type & 0xf0;
					if (flag == TYPE_LIST_FLAG) {
						if (b != '[') {
							unsafe.putObject(obj, offset, null);
							break;
						}
						@SuppressWarnings("unchecked")
						Collection<@Nullable Object> c = (Collection<@Nullable Object>)unsafe.getObject(obj, offset);
						if (c == null) {
							Creator<?> ctor = fm.ctor;
							if (ctor != null) {
								@SuppressWarnings("unchecked")
								Collection<@Nullable Object> c2 = ensureNotNull((Collection<@Nullable Object>)ctor.create());
								unsafe.putObject(obj, offset, c = c2);
							} else {
								ClassMeta<?> cm = classMeta.json.getClassMeta(fm.klass);
								Parser<?> parser = cm.parser;
								if (parser == null) {
									throw new InstantiationException("abstract Collection field: " + fm.getName() + " in "
										+ classMeta.klass.getName());
								}
								Object c2 = parser.parse0(this, cm, fm, null, obj);
								if (c2 != null && !fm.klass.isAssignableFrom(c2.getClass())) {
									throw new InstantiationException(
										"incompatible type(" + c2.getClass() + ") for Collection field: " + fm.getName()
											+ " in " + classMeta.klass.getName());
								}
								unsafe.putObject(obj, offset, c2);
								break;
							}
						} else
							c.clear();
						parseList0(c, classMeta, fm);
					} else if (flag == TYPE_MAP_FLAG) {
						if (b != '{') {
							unsafe.putObject(obj, offset, null);
							break;
						}
						@SuppressWarnings("unchecked")
						Map<Object, @Nullable Object> m = (Map<Object, @Nullable Object>)unsafe.getObject(obj, offset);
						if (m == null) {
							Creator<?> ctor = fm.ctor;
							if (ctor != null) {
								@SuppressWarnings("unchecked")
								Map<Object, @Nullable Object> m2 = ensureNotNull((Map<Object, @Nullable Object>)ctor.create());
								unsafe.putObject(obj, offset, m = m2);
							} else {
								ClassMeta<?> cm = classMeta.json.getClassMeta(fm.klass);
								Parser<?> parser = cm.parser;
								if (parser == null) {
									throw new InstantiationException(
										"abstract Map field: " + fm.getName() + " in " + classMeta.klass.getName());
								}
								Object m2 = parser.parse0(this, cm, fm, null, obj);
								if (m2 != null && !fm.klass.isAssignableFrom(m2.getClass())) {
									throw new InstantiationException("incompatible type(" + m2.getClass()
										+ ") for Map field: " + fm.getName() + " in " + classMeta.klass.getName());
								}
								unsafe.putObject(obj, offset, m2);
								break;
							}
						} else
							m.clear();
						parseMap0(m, classMeta, fm);
					}
				}
			}
			pos++;
			return obj;
		} finally {
			depth--;
		}
	}

	public void parseList0(Collection<@Nullable Object> c, ClassMeta<?> classMeta, FieldMeta fm)
		throws ReflectiveOperationException {
		if (depth >= maxDepth)
			throw new IllegalStateException("json nesting depth exceeds " + maxDepth);
		depth++;
		try {
			int b = skipNext();
			switch (fm.type & 0xf) {
			case TYPE_BOOLEAN:
				for (; b != ']'; b = skipVar(']'))
					c.add(b == 'n' ? null : b == 't');
				break;
			case TYPE_BYTE:
				for (; b != ']'; b = skipVar(']'))
					c.add(b == 'n' ? null : (byte)parseInt());
				break;
			case TYPE_SHORT:
				for (; b != ']'; b = skipVar(']'))
					c.add(b == 'n' ? null : (short)parseInt());
				break;
			case TYPE_CHAR:
				for (; b != ']'; b = skipVar(']'))
					c.add(b == 'n' ? null : (char)parseInt());
				break;
			case TYPE_INT:
				for (; b != ']'; b = skipVar(']'))
					c.add(b == 'n' ? null : parseInt());
				break;
			case TYPE_LONG:
				for (; b != ']'; b = skipVar(']'))
					c.add(b == 'n' ? null : parseLong());
				break;
			case TYPE_FLOAT:
				for (; b != ']'; b = skipVar(']'))
					c.add(b == 'n' ? null : (float)parseDouble());
				break;
			case TYPE_DOUBLE:
				for (; b != ']'; b = skipVar(']'))
					c.add(b == 'n' ? null : parseDouble());
				break;
			case TYPE_STRING:
				for (; b != ']'; b = skipVar(']'))
					c.add(b == 'n' ? null : parseString(false));
				break;
			case TYPE_OBJECT:
				for (; b != ']'; b = skipVar(']'))
					c.add(parse(null, b));
				break;
			case TYPE_POS:
				for (; b != ']'; b = skipVar(']'))
					c.add(new Pos(pos));
				break;
			case TYPE_CUSTOM:
				ClassMeta<?> subClassMeta = fm.classMeta;
				if (subClassMeta == null)
					fm.classMeta = subClassMeta = classMeta.json.getClassMeta(fm.klass);
				Parser<?> parser = subClassMeta.parser;
				if (parser != null) {
					for (; b != ']'; b = skipVar(']'))
						c.add(b == 'n' ? null : parser.parse0(this, subClassMeta, fm, null, c));
				} else {
					if (ClassMeta.isAbstract(subClassMeta.klass)) {
						throw new InstantiationException(
							"abstract element class: " + fm.getName() + " in " + classMeta.klass.getName());
					}
					for (; b != ']'; b = skipVar(']'))
						c.add(b == 'n' ? null : parse0(subClassMeta.ctor.create(), subClassMeta));
				}
				break;
			}
			pos++;
		} finally {
			depth--;
		}
	}

	public void parseMap0(Map<Object, @Nullable Object> m, ClassMeta<?> classMeta, FieldMeta fm)
		throws ReflectiveOperationException {
		if (depth >= maxDepth)
			throw new IllegalStateException("json nesting depth exceeds " + maxDepth);
		depth++;
		try {
			int b = skipNext();
			KeyReader keyParser = ensureNotNull(fm.keyParser);
			switch (fm.type & 0xf) {
			case TYPE_BOOLEAN:
				for (; b != '}'; b = skipVar('}')) {
					Object k = keyParser.parse(this, b);
					b = skipColon();
					m.put(k, b == 'n' ? null : b == 't');
				}
				break;
			case TYPE_BYTE:
				for (; b != '}'; b = skipVar('}')) {
					Object k = keyParser.parse(this, b);
					m.put(k, skipColon() == 'n' ? null : (byte)parseInt());
				}
				break;
			case TYPE_SHORT:
				for (; b != '}'; b = skipVar('}')) {
					Object k = keyParser.parse(this, b);
					m.put(k, skipColon() == 'n' ? null : (short)parseInt());
				}
				break;
			case TYPE_CHAR:
				for (; b != '}'; b = skipVar('}')) {
					Object k = keyParser.parse(this, b);
					m.put(k, skipColon() == 'n' ? null : (char)parseInt());
				}
				break;
			case TYPE_INT:
				for (; b != '}'; b = skipVar('}')) {
					Object k = keyParser.parse(this, b);
					m.put(k, skipColon() == 'n' ? null : parseInt());
				}
				break;
			case TYPE_LONG:
				for (; b != '}'; b = skipVar('}')) {
					Object k = keyParser.parse(this, b);
					m.put(k, skipColon() == 'n' ? null : parseLong());
				}
				break;
			case TYPE_FLOAT:
				for (; b != '}'; b = skipVar('}')) {
					Object k = keyParser.parse(this, b);
					m.put(k, skipColon() == 'n' ? null : (float)parseDouble());
				}
				break;
			case TYPE_DOUBLE:
				for (; b != '}'; b = skipVar('}')) {
					Object k = keyParser.parse(this, b);
					m.put(k, skipColon() == 'n' ? null : parseDouble());
				}
				break;
			case TYPE_STRING:
				for (; b != '}'; b = skipVar('}')) {
					Object k = keyParser.parse(this, b);
					m.put(k, skipColon() == 'n' ? null : parseString(false));
				}
				break;
			case TYPE_OBJECT:
				for (; b != '}'; b = skipVar('}')) {
					Object k = keyParser.parse(this, b);
					m.put(k, parse(null, skipColon()));
				}
				break;
			case TYPE_POS:
				for (; b != '}'; b = skipVar('}')) {
					Object k = keyParser.parse(this, b);
					skipColon();
					m.put(k, new Pos(pos));
				}
				break;
			case TYPE_CUSTOM:
				ClassMeta<?> subClassMeta = fm.classMeta;
				if (subClassMeta == null)
					fm.classMeta = subClassMeta = classMeta.json.getClassMeta(fm.klass);
				Parser<?> parser = subClassMeta.parser;
				if (parser != null) {
					for (; b != '}'; b = skipVar('}')) {
						Object k = keyParser.parse(this, b);
						m.put(k, skipColon() == 'n' ? null : parser.parse0(this, subClassMeta, fm, null, m));
					}
				} else {
					if (ClassMeta.isAbstract(subClassMeta.klass)) {
						throw new InstantiationException(
							"abstract value class: " + fm.getName() + " in " + classMeta.klass.getName());
					}
					for (; b != '}'; b = skipVar('}')) {
						Object k = keyParser.parse(this, b);
						m.put(k, skipColon() == 'n' ? null : parse0(subClassMeta.ctor.create(), subClassMeta));
					}
				}
				break;
			}
			pos++;
		} finally {
			depth--;
		}
	}

	int parseKeyHash(int e) {
		final byte[] buffer = Objects.requireNonNull(buf, "JSON input is not set");
		pos++; // 跳过开引号
		if (pos >= buffer.length)
			return 0;
		int b = buffer[pos++];
		if (b == e)
			return 0;
		final int m = keyHashMultiplier;
		int h = 0;
		for (; ; ) {
			if (b == '\\') { // 解码转义并按解码后的 UTF-8 字节算哈希，以匹配字段名 getKeyHash()
				if (pos >= buffer.length)
					return h;
				int c = buffer[pos++];
				if (c == 'u') {
					if (pos + 4 > buffer.length)
						return h;
					c = parseHex4(buffer, pos);
					pos += 4;
					if (c >= 0xd800 && c < 0xdc00 && pos + 6 <= buffer.length && buffer[pos] == '\\' && buffer[pos + 1] == 'u') {
						int c2 = parseHex4(buffer, pos + 2);
						if ((c2 & 0xfc00) == 0xdc00) { // UTF-16 代理对 -> 4 字节 UTF-8
							pos += 6;
							c = (c << 10) + c2 + (0x10000 - (0xd800 << 10) - 0xdc00);
							h = h * m + (byte)(0xf0 + (c >> 18));
							h = h * m + (byte)(0x80 + ((c >> 12) & 0x3f));
							h = h * m + (byte)(0x80 + ((c >> 6) & 0x3f));
							h = h * m + (byte)(0x80 + (c & 0x3f));
							c = -1;
						}
					}
					if (c >= 0) {
						if (c >= 0x800) { // 3 字节 UTF-8（含孤立代理）
							h = h * m + (byte)(0xe0 + (c >> 12));
							h = h * m + (byte)(0x80 + ((c >> 6) & 0x3f));
							h = h * m + (byte)(0x80 + (c & 0x3f));
						} else if (c >= 0x80) { // 2 字节 UTF-8
							h = h * m + (byte)(0xc0 + (c >> 6));
							h = h * m + (byte)(0x80 + (c & 0x3f));
						} else
							h = h * m + (byte)c;
					}
				} else {
					c = c >= 0x20 ? ESCAPE[c - 0x20] : (c & 0xff);
					if (c >= 0x80) { // U+0080..U+00FF -> 2 字节 UTF-8
						h = h * m + (byte)(0xc0 + (c >> 6));
						h = h * m + (byte)(0x80 + (c & 0x3f));
					} else
						h = h * m + (byte)c;
				}
			} else
				h = h * m + b;
			if (pos >= buffer.length)
				return h;
			if ((b = buffer[pos++]) == e)
				return h;
		}
	}

	@SuppressWarnings("UnnecessaryUnicodeEscape")
	int parseKeyHashNoQuot(int b) {
		if (b == ':')
			return 0;
		b = (byte)b; // skipNext() 返回无符号字节，字段元数据按有符号 UTF-8 字节算哈希
		final int m = keyHashMultiplier;
		int h = 0;
		for (; ; ) {
			if (b == '\\') {
				//noinspection DataFlowIssue
				if (pos + 1 >= buf.length)
					return h;
				int c = buf[++pos];
				if (c == 'u') {
					if (pos + 4 >= buf.length)
						return h;
					c = parseHex4(buf, pos + 1);
					pos += 5; // 跳过 'u' + 4 位 hex，pos 停在下一未读字节
					if (c >= 0xd800 && c < 0xdc00 && pos + 6 < buf.length && buf[pos] == '\\' && buf[pos + 1] == 'u') {
						int c2 = parseHex4(buf, pos + 2);
						if ((c2 & 0xfc00) == 0xdc00) { // UTF-16 代理对 -> 4 字节 UTF-8
							pos += 6;
							c = (c << 10) + c2 + (0x10000 - (0xd800 << 10) - 0xdc00);
							h = h * m + (byte)(0xf0 + (c >> 18));
							h = h * m + (byte)(0x80 + ((c >> 12) & 0x3f));
							h = h * m + (byte)(0x80 + ((c >> 6) & 0x3f));
							h = h * m + (byte)(0x80 + (c & 0x3f));
							c = -1;
						}
					}
					if (c >= 0) {
						if (c >= 0x800) { // 3 字节 UTF-8（含孤立代理）
							h = h * m + (byte)(0xe0 + (c >> 12));
							h = h * m + (byte)(0x80 + ((c >> 6) & 0x3f));
							h = h * m + (byte)(0x80 + (c & 0x3f));
						} else if (c >= 0x80) { // 2 字节 UTF-8
							h = h * m + (byte)(0xc0 + (c >> 6));
							h = h * m + (byte)(0x80 + (c & 0x3f));
						} else
							h = h * m + (byte)c;
					}
				} else {
					c = c >= 0x20 ? ESCAPE[c - 0x20] : (c & 0xff);
					if (c >= 0x80) { // U+0080..U+00FF -> 2 字节 UTF-8
						h = h * m + (byte)(0xc0 + (c >> 6));
						h = h * m + (byte)(0x80 + (c & 0x3f));
					} else
						h = h * m + (byte)c;
					pos++; // 前进到下一未读字节，与 'u' 路径保持一致
				}
				// 转义解码后 pos 已在下一未读字节，直接读取而不走循环尾部的 ++pos
				if (pos >= buf.length)
					return h;
				b = buf[pos];
				if (((((b & 0xff) - ' ' - 1) ^ (':' - ' ' - 1)) <= 0) || b == '/')
					return h;
				continue;
			} else
				h = h * m + b;
			//noinspection DataFlowIssue
			if (pos + 1 >= buf.length)
				return h;
			if (((((b = buf[++pos]) & 0xff) - ' ' - 1) ^ (':' - ' ' - 1)) <= 0) // (b & 0xff) <= ' ' || b == ':'
				return h;
			if (b == '/')
				return h;
		}
	}

	/** 哈希命中后的键内容逐字节校验（解码语义与 parseKeyHash 一致，无转义走快路径）。 */
	boolean keyEquals(int begin, int end, byte[] name) {
		final byte[] buffer = Objects.requireNonNull(buf, "JSON input is not set");
		int p, b, c;
		for (p = begin; p < end; p++)
			if (buffer[p] == '\\')
				break;
		if (p == end)
			return end - begin == name.length && Arrays.equals(buffer, begin, end, name, 0, name.length);
		final int len = name.length;
		int n = 0;
		for (p = begin; p < end; ) {
			b = buffer[p++];
			if (b != '\\') {
				if (n >= len || name[n++] != (byte)b)
					return false;
				continue;
			}
			if (p >= end)
				return false;
			c = buffer[p++];
			if (c == 'u') {
				if (end - p < 4)
					return false;
				c = parseHex4(buffer, p);
				p += 4;
				if (c >= 0xd800 && c < 0xdc00 && end - p >= 6
						&& buffer[p] == '\\' && buffer[p + 1] == 'u') {
					int c2 = parseHex4(buffer, p + 2);
					if ((c2 & 0xfc00) == 0xdc00) {
						p += 6;
						c = (c << 10) + c2 + (0x10000 - (0xd800 << 10) - 0xdc00);
						if (n + 4 > len
							|| name[n] != (byte)(0xf0 + (c >> 18))
							|| name[n + 1] != (byte)(0x80 + ((c >> 12) & 0x3f))
							|| name[n + 2] != (byte)(0x80 + ((c >> 6) & 0x3f))
							|| name[n + 3] != (byte)(0x80 + (c & 0x3f)))
							return false;
						n += 4;
						continue;
					}
				}
				if (c >= 0x800) {
					if (n + 3 > len
						|| name[n] != (byte)(0xe0 + (c >> 12))
						|| name[n + 1] != (byte)(0x80 + ((c >> 6) & 0x3f))
						|| name[n + 2] != (byte)(0x80 + (c & 0x3f)))
						return false;
					n += 3;
				} else if (c >= 0x80) {
					if (n + 2 > len
						|| name[n] != (byte)(0xc0 + (c >> 6))
						|| name[n + 1] != (byte)(0x80 + (c & 0x3f)))
						return false;
					n += 2;
				} else if (n >= len || name[n++] != (byte)c)
					return false;
			} else {
				c = c >= 0x20 ? ESCAPE[c - 0x20] : (c & 0xff);
				if (c >= 0x80) {
					if (n + 2 > len
						|| name[n] != (byte)(0xc0 + (c >> 6))
						|| name[n + 1] != (byte)(0x80 + (c & 0x3f)))
						return false;
					n += 2;
				} else if (n >= len || name[n++] != (byte)c)
					return false;
			}
		}
		return n == len;
	}

	public static String parseStringKey(JsonReader jr, int b) {
		return b == '"' || b == '\'' ? jr.parseString(b, true) : jr.parseStringNoQuot();
	}

	public static Boolean parseBooleanKey(JsonReader jr, int b) {
		final byte[] buffer = Objects.requireNonNull(jr.buf, "JSON input is not set");
		boolean v;
		if (b == '"' || b == '\'') {
			if (++jr.pos >= buffer.length)
				return false;
			v = buffer[jr.pos] == 't';
			jr.skipQuot(b);
		} else {
			// 同时用于键与 typed 值：停在冒号、容器分隔符或注释开头，交给外层消费
			v = buffer[jr.pos] == 't';
			// noinspection StatementWithEmptyBody
			for (int c; jr.pos < buffer.length && (c = buffer[jr.pos] & 0xff) > ' '
				&& c != ':' && c != ',' && c != ']' && c != '}' && c != '/'; jr.pos++) {
			}
		}
		return v;
	}

	public static Byte parseByteKey(JsonReader jr, int b) {
		int v;
		if (b == '"' || b == '\'') {
			jr.pos++;
			v = jr.parseInt();
			jr.skipQuot(b);
		} else
			v = jr.parseInt();
		return (byte)v;
	}

	public static Short parseShortKey(JsonReader jr, int b) {
		int v;
		if (b == '"' || b == '\'') {
			jr.pos++;
			v = jr.parseInt();
			jr.skipQuot(b);
		} else
			v = jr.parseInt();
		return (short)v;
	}

	public static Character parseCharKey(JsonReader jr, int b) {
		int v;
		if (b == '"' || b == '\'') {
			jr.pos++;
			v = jr.parseInt();
			jr.skipQuot(b);
		} else
			v = jr.parseInt();
		return (char)v;
	}

	public static Integer parseIntegerKey(JsonReader jr, int b) {
		int v;
		if (b == '"' || b == '\'') {
			jr.pos++;
			v = jr.parseInt();
			jr.skipQuot(b);
		} else
			v = jr.parseInt();
		return v;
	}

	public static Long parseLongKey(JsonReader jr, int b) {
		long v;
		if (b == '"' || b == '\'') {
			jr.pos++;
			v = jr.parseLong();
			jr.skipQuot(b);
		} else
			v = jr.parseLong();
		return v;
	}

	public static Float parseFloatKey(JsonReader jr, int b) {
		double v;
		if (b == '"' || b == '\'') {
			jr.pos++;
			v = jr.parseDouble();
			jr.skipQuot(b);
		} else
			v = jr.parseDouble();
		return (float)v;
	}

	public static Double parseDoubleKey(JsonReader jr, int b) {
		double v;
		if (b == '"' || b == '\'') {
			jr.pos++;
			v = jr.parseDouble();
			jr.skipQuot(b);
		} else
			v = jr.parseDouble();
		return v;
	}

	private static int parseHex(int b) {
		return (b & 0xf) + (b >> 6) * 9; // 0~9:0x3X  A~F:0x4X  a~f:0x6X
	}

	private static int parseHex4(byte[] buffer, int p) {
		return (parseHex(buffer[p]) << 12) + (parseHex(buffer[p + 1]) << 8)
			+ (parseHex(buffer[p + 2]) << 4) + parseHex(buffer[p + 3]);
	}

	public byte @Nullable [] parseByteString() {
		final byte[] buffer = Objects.requireNonNull(buf, "JSON input is not set");
		int p = pos, b, e = buffer[p];
		if (e != '"' && e != '\'')
			return null;
		final int begin = ++p;
		for (; ; p++) {
			if (p >= buffer.length) { // 截断于闭引号前：EOF即终结，宽容收尾
				pos = p;
				return Arrays.copyOfRange(buffer, begin, p);
			}
			if ((b = buffer[p]) == e) {
				pos = p + 1; // lucky! finished the fast path
				return Arrays.copyOfRange(buffer, begin, p);
			}
			if (b == '\\')
				break; // jump to the slow path below
		}
		int len = p - begin, n = len;
		// 长度测定段与解码段逐case镜像（截断/残缺转义一律按字面量收尾），保证两段计数一致。
		while (p < buffer.length && (b = buffer[p++]) != e) {
			if (b != '\\') {
				len++;
				continue;
			}
			if (p >= buffer.length || buffer[p++] != 'u') { // '\'末字节或简写转义：2字节入1字节出
				len++;
				continue;
			}
			if (buffer.length - p < 4) { // 残缺\\u：'\''与'u'按字面量，余下hex由后续迭代按普通字节计
				len += 2;
				continue;
			}
			b = parseHex4(buffer, p);
			p += 4;
			if (b >= 0x800) {
				if ((b & 0xfc00) == 0xd800 && p + 5 < buffer.length && buffer[p] == '\\' && buffer[p + 1] == 'u'
					&& (parseHex4(buffer, p + 2) & 0xfc00) == 0xdc00) {
					p += 6;
					len += 4;
				} else
					len += 3;
			} else if (b >= 0x80)
				len += 2;
			else
				len++;
		}
		byte[] t = new byte[len];
		System.arraycopy(buffer, begin, t, 0, n);
		for (p = begin + n; ; ) {
			if (p >= buffer.length) { // 截断：pos钉len
				pos = p;
				return t;
			}
			if ((b = buffer[p++]) == e) {
				pos = p;
				return t;
			}
			if (b != '\\') {
				t[n++] = (byte)b;
				continue;
			}
			if (p >= buffer.length || (b = buffer[p++]) != 'u') { // '\'末字节或简写转义
				t[n++] = b >= 0x20 ? ESCAPE[b - 0x20] : (byte)b;
				continue;
			}
			if (buffer.length - p < 4) { // 残缺\\u：字面量（与长度段一致）
				t[n++] = '\\';
				t[n++] = 'u';
				continue;
			}
			b = parseHex4(buffer, p);
			p += 4;
			if (b >= 0x800) {
				int c2;
				if ((b & 0xfc00) == 0xd800 && p + 5 < buffer.length && buffer[p] == '\\' && buffer[p + 1] == 'u'
					&& ((c2 = parseHex4(buffer, p + 2)) & 0xfc00) == 0xdc00) {
					p += 6;
					b = (b << 10) + c2 + (0x10000 - (0xd800 << 10) - 0xdc00);
					t[n++] = (byte)(0xf0 + (b >> 18)); // 1111 0xxx  10xx xxxx  10xx xxxx  10xx xxxx
					t[n++] = (byte)(0x80 + ((b >> 12) & 0x3f));
				} else
					t[n++] = (byte)(0xe0 + (b >> 12)); // 1110 xxxx  10xx xxxx  10xx xxxx
				t[n++] = (byte)(0x80 + ((b >> 6) & 0x3f));
				t[n++] = (byte)(0x80 + (b & 0x3f));
			} else if (b >= 0x80) {
				t[n++] = (byte)(0xc0 + (b >> 6)); // 110x xxxx  10xx xxxx
				t[n++] = (byte)(0x80 + (b & 0x3f));
			} else
				t[n++] = (byte)b; // 0xxx xxxx
		}
	}

	public String parseString() {
		return parseString(false);
	}

	public String parseString(boolean intern) {
		final byte[] buffer = Objects.requireNonNull(buf, "JSON input is not set");
		int p = pos, b = buffer[p];
		if (b != '"' && b != '\'') {
			final int begin = p;
			for (; ; b = buffer[++p]) {
				if (b == ',' || b == '\n' || (b | 0x20) == '}') { // ]:0x5D | 0x20 = }:0x7D
					pos = p;
					if (begin < p && buffer[p - 1] == '\r')
						p--;
					return intern ? intern(buffer, begin, p) : newByteString(buffer, begin, p);
				}
				if (b < 0) // 多字节 UTF-8 首字节转慢路径解码
					return parseStringUnquotedSlow(begin);
				if (p + 1 >= buffer.length) {
					pos = buffer.length;
					var end = pos;
					if (begin < end && buffer[end - 1] == '\r')
						end--;
					return intern ? intern(buffer, begin, end) : newByteString(buffer, begin, end);
				}
			}
		}
		return parseString(b, intern);
	}

	// 无引号值慢路径：反斜杠按字面量处理，整段纯 UTF-8 一次解码
	private String parseStringUnquotedSlow(int begin) {
		final byte[] buffer = Objects.requireNonNull(buf, "JSON input is not set");
		int b, p;
		for (p = begin; p < buffer.length; p++) {
			b = buffer[p];
			if (b == ',' || b == '\n' || (b | 0x20) == '}')
				break;
		}
		pos = p;
		int end = p;
		if (end > begin && buffer[end - 1] == '\r')
			end--;
		char[] t = tmp;
		if (t == null || t.length < end - begin)
			tmp = t = new char[end - begin];
		return new String(t, 0, decodeUtf8(t, 0, begin, end));
	}

	public String parseString(int e, boolean intern) {
		final byte[] buffer = Objects.requireNonNull(buf, "JSON input is not set");
		int p = pos, b;
		final int begin = ++p;
		for (; ; p++) {
			if (p >= buffer.length) {
				pos = p;
				return intern ? intern(buffer, begin, p) : newByteString(buffer, begin, p);
			}
			if ((b = buffer[p]) == e) {
				pos = p + 1;
				return intern ? intern(buffer, begin, p) : newByteString(buffer, begin, p);
			}
			if ((b ^ '\\') < 1) // '\\' 或多字节 UTF-8 字符
				break;
		}
		// 慢路径扫描终止符：跨越转义序列，残缺 \\u 不越过终止符或反斜杠
		int end;
		for (end = p; end < buffer.length && (b = buffer[end]) != e; end++) {
			if (b == '\\') {
				if (end + 1 >= buffer.length)
					break;
				end++;
				if (buffer[end] == 'u') {
					for (int j = 0; j < 4 && end + 1 < buffer.length
						&& buffer[end + 1] != e && buffer[end + 1] != '\\'; j++)
						end++;
				}
			}
		}
		pos = end < buffer.length ? end + 1 : end;
		return decodeStringSlow(begin, end);
	}

	public String parseStringNoQuot() {
		final byte[] buffer = Objects.requireNonNull(buf, "JSON input is not set");
		int p = pos, b;
		final int begin = p;
		for (; ; p++) {
			if (p >= buffer.length || (((((b = buffer[p]) & 0xff) - ' ' - 1) ^ (':' - ' ' - 1)) <= 0) || b == '/')
				return intern(buffer, begin, pos = p);
			if ((b ^ '\\') < 1) // '\\' 或多字节 UTF-8 字符
				break;
		}
		int end;
		for (end = p; end < buffer.length && ((((b = buffer[end]) & 0xff) - ' ' - 1) ^ (':' - ' ' - 1)) > 0 && b != '/'; end++) {
			if (b == '\\') {
				if (end + 1 >= buffer.length)
					break;
				end++;
				if (buffer[end] == 'u') {
					for (int j = 0; j < 4 && end + 1 < buffer.length
						&& ((((buffer[end + 1] & 0xff) - ' ' - 1) ^ (':' - ' ' - 1)) > 0)
						&& buffer[end + 1] != '\\'; j++)
						end++;
				}
			}
		}
		pos = end; // 定界符或注释开头 '/' 留给外层消费
		return decodeStringSlow(begin, end);
	}

	// 含转义字符串的慢路径解码：[begin, end) 按纯 UTF-8 分段解码，单独处理 '\' 转义（残缺 \\u 按字面量收尾）。
	private String decodeStringSlow(int begin, int end) {
		final byte[] buffer = Objects.requireNonNull(buf, "JSON input is not set");
		char[] t = tmp;
		if (t == null || t.length < end - begin)
			tmp = t = new char[end - begin];
		int n = 0, b;
		for (int p = begin; p < end; ) {
			int seg = p;
			//noinspection StatementWithEmptyBody
			for (; seg < end && buffer[seg] != '\\'; seg++)
				;
			n = decodeUtf8(t, n, p, seg);
			if ((p = seg) < end) { // buffer[p] == '\\'
				if (++p >= end)
					break;
				if (buffer[p] == 'u') {
					if (end - p < 5) {
						// 残缺 \\u 按字面量输出
						t[n++] = '\\';
						t[n++] = 'u';
						p++;
					} else {
						t[n++] = (char)parseHex4(buffer, ++p);
						p += 4;
					}
				} else {
					b = buffer[p++];
					// JSON5 行续写：反斜杠后跟 LF/CR/CRLF 或 U+2028/U+2029 不产生字符
					if (b == '\n' || b == '\r') {
						if (b == '\r' && p < end && buffer[p] == '\n')
							p++; // CRLF
					} else if ((b & 0xff) == 0xE2 && p + 1 < end && (buffer[p] & 0xff) == 0x80
						&& (((buffer[p + 1] & 0xff) == 0xA8) || ((buffer[p + 1] & 0xff) == 0xA9))) {
						p += 2; // U+2028/U+2029
					} else {
						// 无符号掩码避免 >=0x80 符号扩展为 U+FFxx
						t[n++] = (char)(b >= 0x20 ? ESCAPE[b - 0x20] : b & 0xff);
					}
				}
			}
		}
		return new String(t, 0, n);
	}

	/** 解码 [begin, end) 的纯 UTF-8 段写入 t[n..]，非法字节按单字节 LATIN1 容忍，返回写入后下标。 */
	private int decodeUtf8(char[] t, int n, int begin, int end) {
		final byte[] buffer = Objects.requireNonNull(buf, "JSON input is not set");
		int p = begin;
		while (p < end) {
			int b = buffer[p] & 0xff;
			if (b < 0x80) {
				t[n++] = (char)b;
				p++;
				continue;
			}
			int continuations = b >= 0xc2 && b <= 0xdf ? 1
					: b >= 0xe0 && b <= 0xef ? 2 : b >= 0xf0 && b <= 0xf4 ? 3 : 0;
			if (continuations != 0 && end - p > continuations) {
				int codePoint = b & (continuations == 1 ? 0x1f : continuations == 2 ? 0xf : 7);
				for (int j = 1; j <= continuations; j++) {
					int c = buffer[p + j] & 0xff;
					if ((c & 0xc0) != 0x80) {
						codePoint = -1;
						break;
					}
					codePoint = (codePoint << 6) | (c & 0x3f);
				}
				int minimum = continuations == 1 ? 0x80 : continuations == 2 ? 0x800 : 0x10000;
				if (codePoint >= minimum && codePoint <= 0x10ffff && (codePoint < 0xd800 || codePoint > 0xdfff)) {
					if (codePoint < 0x10000)
						t[n++] = (char)codePoint;
					else {
						t[n++] = Character.highSurrogate(codePoint);
						t[n++] = Character.lowSurrogate(codePoint);
					}
					p += continuations + 1;
					continue;
				}
			}
			// 非法序列仅消费当前字节，保留后续字节
			t[n++] = (char)b;
			p++;
		}
		return n;
	}

	static double strtod(double d, int e) {
		return e >= 0
			? (e > 308 ? (d == 0.0 ? d : d * Double.POSITIVE_INFINITY) : d * EXP[e])
			: (e < -308 ? 0 : d / EXP[-e]);
	}

	// 有效数字 >=16 位或 |expTotal|>15 时快路径可能双舍入，回退 Double.parseDouble 保证正确舍入；
	// 无需或无法回退时返回 null（[from, to) 含符号，返回值已带符号）。
	private static @Nullable Double parseDoubleRounded(byte[] buffer, int from, int to, int expTotal) {
		int digits = 0;
		boolean mantissa = true; // 指数部分不计有效数字
		for (int i = from; i < to; i++) {
			byte b = buffer[i];
			if (b == 'e' || b == 'E')
				mantissa = false;
			else if (mantissa && b >= '0' && b <= '9' && (digits > 0 || b > '0'))
				digits++; // 跳过前导零
		}
		if (digits < 16 && expTotal >= -15 && expTotal <= 15)
			return null;
		try {
			return Double.parseDouble(new String(buffer, from, to - from, StandardCharsets.ISO_8859_1));
		} catch (NumberFormatException ignored) {
			return null;
		}
	}

	public int parseInt() {
		final byte[] buffer = Objects.requireNonNull(buf, "JSON input is not set");
		final int startPos = pos;
		double d = 0;
		int p = pos, i = 0, n = 0, expFrac = 0, exp = 0, useDouble = 0, b, c;
		boolean minus = false, expMinus = false;

		try {
			b = buffer[p];
			if (b == '-') {
				minus = true;
				b = buffer[++p];
			} else if (b == '+')
				b = buffer[++p];
			c = b | 0x20;
			if (c == 'i' || c == 'n') { // Infinity/NaN，按 (int)double 饱和语义取值
				do
					b = ++p < buffer.length ? buffer[p] : 0;
				while ((((b | 0x20) - 'a') & 0xff) < 26); // 防止词尾恰在缓冲区末尾时越界
				pos = p;
				return c == 'n' ? 0 : minus ? Integer.MIN_VALUE : Integer.MAX_VALUE;
			}
			if (b == '0') {
				b = buffer[++p];
				if ((b | 0x20) == 'x') { // JSON5 0x 十六进制（溢出转 double 饱和强转）
					boolean useD = false;
					for (; ; ) {
						b = ++p < buffer.length ? buffer[p] : 0;
						if ((c = (b - '0') & 0xff) < 10) {
							if (useD)
								d = d * 16 + c;
							else if (i >= 0x0800_0000) { // 0x0800_0000*16+15恰溢出int
								useD = true;
								d = (double)i * 16 + c;
							} else
								i = i * 16 + c;
						} else if ((c = ((b | 0x20) - 'a') & 0xff) < 6) {
							if (useD)
								d = d * 16 + c + 10;
							else if (i >= 0x0800_0000) {
								useD = true;
								d = (double)i * 16 + c + 10;
							} else
								i = i * 16 + c + 10;
						} else
							break;
					}
					pos = p;
					if (useD)
						return (int)(minus ? -d : d);
					return minus ? -i : i;
				}
			} else if ((i = (b - '0') & 0xff) < 10) {
				while ((c = ((b = buffer[++p]) - '0') & 0xff) < 10) {
					if (i >= 0xCCC_CCCC) { // 0xCCC_CCCC * 10 = 0x7FFF_FFF8
						d = i;
						useDouble = 2;
						do
							d = d * 10 + c;
						while ((c = ((b = buffer[++p]) - '0') & 0xff) < 10);
						break;
					}
					i = i * 10 + c;
					n++;
				}
			} else
				i = 0;

			if (b == '.') {
				c = ((b = buffer[++p]) - '0') & 0xff;
				if (useDouble == 0) {
					useDouble = 1;
					for (; c < 10; c = ((b = buffer[++p]) - '0') & 0xff) {
						if (i >= 0xCCC_CCCC)
							break;
						i = i * 10 + c;
						expFrac--;
						if (i != 0)
							n++;
					}
					d = i;
					useDouble = 2;
				}
				for (; c < 10; c = ((b = buffer[++p]) - '0') & 0xff) {
					if (n < 17) {
						d = d * 10 + c;
						expFrac--;
						if (d > 0)
							n++;
					}
				}
			}

			if ((b | 0x20) == 'e') { // E:0x45 | 0x20 = e:0x65
				if (useDouble == 0) {
					d = i;
					useDouble = 2;
				}
				if ((b = buffer[++p]) == '+')
					b = buffer[++p];
				if (b == '-') {
					expMinus = true;
					b = buffer[++p];
				} else if (b == '+')
					b = buffer[++p];
				if ((b = (b - '0') & 0xff) < 10) {
					exp = b;
					final int maxExp = expMinus ? (expFrac + 0x7FFF_FFF7) / 10 : 308 - expFrac;
					while ((b = (buffer[++p] - '0') & 0xff) < 10) {
						if ((exp = exp * 10 + b) > maxExp) {
							do
								b = ++p < buffer.length ? buffer[p] : 0;
							while (((b - '0') & 0xff) < 10);
							if (!expMinus) {
								pos = p;
								if (isZeroMantissa(i, d))
									return 0;
								return minus ? Integer.MIN_VALUE : Integer.MAX_VALUE;
							}
							break; // p 指向首个非数字字符
						}
					}
				}
			}
		} catch (IndexOutOfBoundsException ignored) {
			if (useDouble == 1)
				d = i;
		}
		pos = p;
		if (p == startPos)
			throw new NumberFormatException("empty number");
		if (expMinus)
			exp = -exp;
		if (useDouble > 0) {
			var rounded = parseDoubleRounded(buffer, startPos, p, exp + expFrac);
			if (rounded != null)
				return (int)(double)rounded;
			exp += expFrac;
			d = exp < -308 ? strtod(d / 1e308, exp + 308) : strtod(d, exp);
			i = (int)(minus ? -d : d);
		} else if (minus)
			i = -i;
		return i;
	}

	public long parseLong() {
		final byte[] buffer = Objects.requireNonNull(buf, "JSON input is not set");
		final int startPos = pos;
		double d = 0;
		long i = 0;
		int p = pos, n = 0, expFrac = 0, exp = 0, useDouble = 0, b, c;
		boolean minus = false, expMinus = false;

		try {
			b = buffer[p];
			if (b == '-') {
				minus = true;
				b = buffer[++p];
			} else if (b == '+')
				b = buffer[++p];
			c = b | 0x20;
			if (c == 'i' || c == 'n') { // Infinity/NaN，按 (long)double 饱和语义取值
				do
					b = ++p < buffer.length ? buffer[p] : 0;
				while ((((b | 0x20) - 'a') & 0xff) < 26);
				pos = p;
				return c == 'n' ? 0L : minus ? Long.MIN_VALUE : Long.MAX_VALUE;
			}
			if (b == '0') {
				b = buffer[++p];
				if ((b | 0x20) == 'x') { // JSON5 0x 十六进制（溢出转 double 饱和强转）
					boolean useD = false;
					for (; ; ) {
						b = ++p < buffer.length ? buffer[p] : 0;
						if ((c = (b - '0') & 0xff) < 10) {
							if (useD)
								d = d * 16 + c;
							else if (i >= 0x0800_0000_0000_0000L) { // 0x0800..*16+15 恰溢出 long
								useD = true;
								d = (double)i * 16 + c;
							} else
								i = i * 16 + c;
						} else if ((c = ((b | 0x20) - 'a') & 0xff) < 6) {
							if (useD)
								d = d * 16 + c + 10;
							else if (i >= 0x0800_0000_0000_0000L) {
								useD = true;
								d = (double)i * 16 + c + 10;
							} else
								i = i * 16 + c + 10;
						} else
							break;
					}
					pos = p;
					if (useD)
						return (long)(minus ? -d : d);
					return minus ? -i : i;
				}
			} else if ((i = (b - '0') & 0xff) < 10) {
				while ((c = ((b = buffer[++p]) - '0') & 0xff) < 10) {
					if (i >= 0xCCC_CCCC_CCCC_CCCCL && (i > 0xCCC_CCCC_CCCC_CCCCL || c > 7)) {
						d = i; // 0xCCC_CCCC_CCCC_CCCC * 10 = 0x7FFF_FFFF_FFFF_FFF8
						useDouble = 2;
						do
							d = d * 10 + c;
						while ((c = ((b = buffer[++p]) - '0') & 0xff) < 10);
						break;
					}
					i = i * 10 + c;
					n++;
				}
			} else
				i = 0;

			if (b == '.') {
				c = ((b = buffer[++p]) - '0') & 0xff;
				if (useDouble == 0) {
					useDouble = 1;
					for (; c < 10; c = ((b = buffer[++p]) - '0') & 0xff) {
						if (i > 0x1F_FFFF_FFFF_FFFFL) // 2^53 - 1 for fast path
							break;
						i = i * 10 + c;
						expFrac--;
						if (i != 0)
							n++;
					}
					d = i;
					useDouble = 2;
				}
				for (; c < 10; c = ((b = buffer[++p]) - '0') & 0xff) {
					if (n < 17) {
						d = d * 10 + c;
						expFrac--;
						if (d > 0)
							n++;
					}
				}
			}

			if ((b | 0x20) == 'e') { // E:0x45 | 0x20 = e:0x65
				if (useDouble == 0) {
					d = i;
					useDouble = 2;
				}
				if ((b = buffer[++p]) == '+')
					b = buffer[++p];
				if (b == '-') {
					expMinus = true;
					b = buffer[++p];
				} else if (b == '+')
					b = buffer[++p];
				if ((b = (b - '0') & 0xff) < 10) {
					exp = b;
					final int maxExp = expMinus ? (expFrac + 0x7FFF_FFF7) / 10 : 308 - expFrac;
					while ((b = (buffer[++p] - '0') & 0xff) < 10) {
						if ((exp = exp * 10 + b) > maxExp) {
							do
								b = ++p < buffer.length ? buffer[p] : 0;
							while (((b - '0') & 0xff) < 10);
							if (!expMinus) {
								pos = p;
								if (isZeroMantissa(i, d))
									return 0L;
								return minus ? Long.MIN_VALUE : Long.MAX_VALUE;
							}
							break;
						}
					}
				}
			}
		} catch (IndexOutOfBoundsException ignored) {
			if (useDouble == 1)
				d = i;
		}
		pos = p;
		if (p == startPos)
			throw new NumberFormatException("empty number");
		if (expMinus)
			exp = -exp;
		if (useDouble > 0) {
			var rounded = parseDoubleRounded(buffer, startPos, p, exp + expFrac);
			if (rounded != null)
				return (long)(double)rounded;
			exp += expFrac;
			d = exp < -308 ? strtod(d / 1e308, exp + 308) : strtod(d, exp);
			i = (long)(minus ? -d : d);
		} else if (minus)
			i = -i;
		return i;
	}

	public double parseDouble() {
		final byte[] buffer = Objects.requireNonNull(buf, "JSON input is not set");
		final int startPos = pos;
		double d = 0;
		long i = 0;
		int p = pos, n = 0, expFrac = 0, exp = 0, useDouble = 0, b, c;
		boolean minus = false, expMinus = false;

		try {
			b = buffer[p];
			if (b == '-') {
				minus = true;
				b = buffer[++p];
			} else if (b == '+')
				b = buffer[++p];
			c = b | 0x20;
			if (c == 'i' || c == 'n') { // Infinity / NaN
				do
					b = ++p < buffer.length ? buffer[p] : 0;
				while ((((b | 0x20) - 'a') & 0xff) < 26);
				pos = p;
				return c == 'n' ? Double.NaN : minus ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
			}
			if (b == '0') {
				b = buffer[++p];
				if ((b | 0x20) == 'x') { // JSON5 0x 十六进制（超 long 按 double 累计）
					boolean useD = false;
					for (; ; ) {
						b = ++p < buffer.length ? buffer[p] : 0;
						if ((c = (b - '0') & 0xff) < 10) {
							if (useD)
								d = d * 16 + c;
							else if (i >= 0x0800_0000_0000_0000L) {
								useD = true;
								d = (double)i * 16 + c;
							} else
								i = i * 16 + c;
						} else if ((c = ((b | 0x20) - 'a') & 0xff) < 6) {
							if (useD)
								d = d * 16 + c + 10;
							else if (i >= 0x0800_0000_0000_0000L) {
								useD = true;
								d = (double)i * 16 + c + 10;
							} else
								i = i * 16 + c + 10;
						} else
							break;
					}
					pos = p;
					return useD ? (minus ? -d : d) : minus ? -(double)i : (double)i;
				}
			} else if ((i = (b - '0') & 0xff) < 10) {
				while ((c = ((b = buffer[++p]) - '0') & 0xff) < 10) {
					if (i >= 0xCCC_CCCC_CCCC_CCCCL && (i > 0xCCC_CCCC_CCCC_CCCCL || c > 7)) {
						d = i; // 0xCCC_CCCC_CCCC_CCCC * 10 = 0x7FFF_FFFF_FFFF_FFF8
						useDouble = 2;
						do
							d = d * 10 + c;
						while ((c = ((b = buffer[++p]) - '0') & 0xff) < 10);
						break;
					}
					i = i * 10 + c;
					n++;
				}
			} else
				i = 0;

			if (b == '.') {
				c = ((b = buffer[++p]) - '0') & 0xff;
				if (useDouble == 0) {
					useDouble = 1;
					for (; c < 10; c = ((b = buffer[++p]) - '0') & 0xff) {
						if (i > 0x1F_FFFF_FFFF_FFFFL) // 2^53 - 1 for fast path
							break;
						i = i * 10 + c;
						expFrac--;
						if (i != 0)
							n++;
					}
					d = i;
					useDouble = 2;
				}
				for (; c < 10; c = ((b = buffer[++p]) - '0') & 0xff) {
					if (n < 17) {
						d = d * 10 + c;
						expFrac--;
						if (d > 0)
							n++;
					}
				}
			}

			if ((b | 0x20) == 'e') { // E:0x45 | 0x20 = e:0x65
				if (useDouble == 0) {
					d = i;
					useDouble = 2;
				}
				if ((b = buffer[++p]) == '+')
					b = buffer[++p];
				if (b == '-') {
					expMinus = true;
					b = buffer[++p];
				} else if (b == '+')
					b = buffer[++p];
				if ((b = (b - '0') & 0xff) < 10) {
					exp = b;
					final int maxExp = expMinus ? (expFrac + 0x7FFF_FFF7) / 10 : 308 - expFrac;
					while ((b = (buffer[++p] - '0') & 0xff) < 10) {
						if ((exp = exp * 10 + b) > maxExp) {
							do
								b = ++p < buffer.length ? buffer[p] : 0;
							while (((b - '0') & 0xff) < 10);
							if (!expMinus) {
								pos = p;
								if (isZeroMantissa(i, d))
									return minus ? -0.0 : 0.0;
								return minus ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
							}
							break;
						}
					}
				}
			}
		} catch (IndexOutOfBoundsException ignored) {
			if (useDouble == 1)
				d = i;
		}
		pos = p;
		if (p == startPos)
			throw new NumberFormatException("empty number");
		if (expMinus)
			exp = -exp;
		if (useDouble > 0) {
			var rounded = parseDoubleRounded(buffer, startPos, p, exp + expFrac);
			if (rounded != null)
				return rounded;
			exp += expFrac;
			d = exp < -308 ? strtod(d / 1e308, exp + 308) : strtod(d, exp);
			if (minus)
				d = -d;
		} else
			// 先转 double 再取负，保留 -0.0 符号
			d = minus ? -(double)i : (double)i;
		return d;
	}

	public Object parseNumber() {
		final byte[] buffer = Objects.requireNonNull(buf, "JSON input is not set");
		final int startPos = pos;
		double d = 0;
		long i = 0;
		int p = pos, n = 0, expFrac = 0, exp = 0, useDouble = 0, b, c;
		boolean minus = false, expMinus = false;

		try {
			b = buffer[p];
			if (((b - '0') & 0xff) >= 10) {
				if (b == '-') {
					minus = true;
					b = buffer[++p];
				} else if (b == '+')
					b = buffer[++p];
				c = b | 0x20;
				if (c == 'i' || c == 'n') { // Infinity / NaN
					do
						b = ++p < buffer.length ? buffer[p] : 0;
					while ((((b | 0x20) - 'a') & 0xff) < 26);
					pos = p;
					return c == 'n' ? Double.NaN : minus ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
				}
			}
			if (b == '0') {
				b = buffer[++p];
				if ((b | 0x20) == 'x') { // JSON5 0x 十六进制（按范围返回 Integer/Long/Double）
					boolean useD = false;
					for (; ; ) {
						b = ++p < buffer.length ? buffer[p] : 0;
						if ((c = (b - '0') & 0xff) < 10) {
							if (useD)
								d = d * 16 + c;
							else if (i >= 0x0800_0000_0000_0000L) {
								useD = true;
								d = (double)i * 16 + c;
							} else
								i = i * 16 + c;
						} else if ((c = ((b | 0x20) - 'a') & 0xff) < 6) {
							if (useD)
								d = d * 16 + c + 10;
							else if (i >= 0x0800_0000_0000_0000L) {
								useD = true;
								d = (double)i * 16 + c + 10;
							} else
								i = i * 16 + c + 10;
						} else
							break;
					}
					pos = p;
					if (useD)
						return minus ? -d : d;
					if (minus)
						i = -i;
					final int j = (int)i;
					return j == i ? (Object)j : i;
				}
			} else if ((i = (b - '0') & 0xff) < 10) {
				while ((c = ((b = buffer[++p]) - '0') & 0xff) < 10) {
					if (i >= 0xCCC_CCCC_CCCC_CCCCL && (i > 0xCCC_CCCC_CCCC_CCCCL || c > 7)) {
						if (minus && i == 0xCCC_CCCC_CCCC_CCCCL && c == 8) {
							i = Long.MIN_VALUE;
							n++;
							c = ((b = buffer[++p]) - '0') & 0xff;
							if (c >= 10 && b != '.' && (b | 0x20) != 'e')
								break;
							d = 9223372036854775808.0; // -(double)Long.MIN_VALUE，避免后续 '.'/'e' 路径对负 d 二次取反
							useDouble = 2;
							while (c < 10) {
								d = d * 10 + c;
								c = ((b = buffer[++p]) - '0') & 0xff;
							}
							break;
						}
						d = i; // 0xCCC_CCCC_CCCC_CCCC * 10 = 0x7FFF_FFFF_FFFF_FFF8
						useDouble = 2;
						do
							d = d * 10 + c;
						while ((c = ((b = buffer[++p]) - '0') & 0xff) < 10);
						break;
					}
					i = i * 10 + c;
					n++;
				}
			} else
				i = 0;

			if (b == '.') {
				c = ((b = buffer[++p]) - '0') & 0xff;
				if (useDouble == 0) {
					useDouble = 1;
					for (; c < 10; c = ((b = buffer[++p]) - '0') & 0xff) {
						if (i > 0x1F_FFFF_FFFF_FFFFL) // 2^53 - 1 for fast path
							break;
						i = i * 10 + c;
						expFrac--;
						if (i != 0)
							n++;
					}
					d = i;
					useDouble = 2;
				}
				for (; c < 10; c = ((b = buffer[++p]) - '0') & 0xff) {
					if (n < 17) {
						d = d * 10 + c;
						expFrac--;
						if (d > 0)
							n++;
					}
				}
			}

			if ((b | 0x20) == 'e') { // E:0x45 | 0x20 = e:0x65
				if (useDouble == 0) {
					d = i;
					useDouble = 2;
				}
				if ((b = buffer[++p]) == '+')
					b = buffer[++p];
				if (b == '-') {
					expMinus = true;
					b = buffer[++p];
				} else if (b == '+')
					b = buffer[++p];
				if ((b = (b - '0') & 0xff) < 10) {
					exp = b;
					final int maxExp = expMinus ? (expFrac + 0x7FFF_FFF7) / 10 : 308 - expFrac;
					while ((b = (buffer[++p] - '0') & 0xff) < 10) {
						if ((exp = exp * 10 + b) > maxExp) {
							do
								b = ++p < buffer.length ? buffer[p] : 0;
							while (((b - '0') & 0xff) < 10);
							if (!expMinus) {
								pos = p;
								if (isZeroMantissa(i, d))
									return minus ? -0.0 : 0.0;
								return minus ? NEGATIVE_INFINITY : POSITIVE_INFINITY;
							}
							break;
						}
					}
				}
			}
		} catch (IndexOutOfBoundsException ignored) {
			if (useDouble == 1)
				d = i;
		}
		pos = p;
		if (p == startPos)
			throw new NumberFormatException("empty number");
		if (expMinus)
			exp = -exp;
		if (useDouble > 0) {
			var rounded = parseDoubleRounded(buffer, startPos, p, exp + expFrac);
			if (rounded != null)
				return rounded;
			exp += expFrac;
			d = exp < -308 ? strtod(d / 1e308, exp + 308) : strtod(d, exp);
			return minus ? -d : d;
		}
		if (minus)
			i = -i;
		final int j = (int)i;
		return j == i ? (Object)j : i;
	}
}
