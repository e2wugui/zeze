package Zeze.Util;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static Zeze.Util.Json.*;

/*
JSON5 additional features:
 YES : Object keys may be an ECMAScript 5.1 IdentifierName.
 YES : Objects may have a single trailing comma.
 YES : Arrays may have a single trailing comma.
 YES : Strings may be single quoted.
 YES : Strings may span multiple lines by escaping new line characters.
 YES : Strings may include character escapes.
 YES : Numbers may be hexadecimal. 0xdecaf,-0XC0FFEE
 YES : Numbers may have a leading or trailing decimal point.
 YES : Numbers may be IEEE 754 "Infinity", "-Infinity", and "NaN".
 YES : Numbers may begin with an explicit plus sign.
 YES : Single and multi-line comments are allowed.
PART : Additional white space characters are allowed: \t,\n,\v(\x0b),\f(\x0c),\r, (\x20),\xa0,\u2028,\u2029,\uFEFF
*/
public final class JsonReader {
	private static final @NotNull Double NEGATIVE_INFINITY = Double.NEGATIVE_INFINITY;
	private static final @NotNull Double POSITIVE_INFINITY = Double.POSITIVE_INFINITY;

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

	private static final @NotNull ThreadLocal<JsonReader> localReaders = ensureNotNull(
			ThreadLocal.withInitial(JsonReader::new));
	private static String[] poolStrs;
	private static int poolSize = 1024;

	public static @NotNull JsonReader local() {
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
		String[] ss = poolStrs;
		if (ss != null)
			Arrays.fill(ss, null);
	}

	static @NotNull String intern(byte[] buf, int pos, int end) {
		int len = end - pos;
		String[] ss = poolStrs;
		if (ss == null)
			poolStrs = ss = new String[poolSize];
		int idx = getKeyHash(buf, pos, end) & (ss.length - 1);
		String s = ss[idx];
		if (s != null) {
			if (BYTE_STRING) { // JDK9+
				byte[] b = (byte[])unsafe.getObject(s, STRING_VALUE_OFFSET);
				if (b.length == len && Arrays.equals(b, 0, len, buf, pos, end))
					return s;
			} else { // for JDK8-
				int n = s.length();
				if (n == len) {
					for (int i = 0; ; i++) {
						if (i == n)
							return s;
						if (s.charAt(i) != (buf[pos + i] & 0xff))
							break;
					}
				}
			}
		}
		ss[idx] = s = newByteString(buf, pos, end);
		return s;
	}

	private byte[] buf; // only support utf-8 encoding
	private int pos;
	private char[] tmp; // for parseString & parseStringNoQuot

	public JsonReader() {
	}

	public JsonReader(byte[] b) {
		buf = b;
	}

	public JsonReader(byte[] b, int p) {
		buf = b;
		pos = p;
	}

	public @NotNull JsonReader reset() {
		buf = null;
		pos = 0;
		tmp = null;
		return this;
	}

	public byte[] buf() {
		return buf;
	}

	public @NotNull JsonReader buf(@NotNull String s) {
		buf = s.getBytes(StandardCharsets.UTF_8);
		pos = 0;
		return this;
	}

	public @NotNull JsonReader buf(byte[] b) {
		buf = b;
		pos = 0;
		return this;
	}

	public @NotNull JsonReader buf(byte[] b, int p) {
		buf = b;
		pos = p;
		return this;
	}

	public int pos() {
		return pos;
	}

	public @NotNull JsonReader pos(int p) {
		pos = p;
		return this;
	}

	public @NotNull JsonReader pos(@NotNull Pos p) {
		pos = p.pos;
		return this;
	}

	public char[] tmp() {
		return tmp;
	}

	public @NotNull JsonReader tmp(char[] t) {
		tmp = t;
		return this;
	}

	public @NotNull JsonReader tmp(int size) {
		if (tmp == null || tmp.length < size)
			tmp = new char[size];
		return this;
	}

	public @NotNull JsonReader skip(int n) {
		pos += n;
		return this;
	}

	public @NotNull JsonReader trySkipBom() {
		int p = pos;
		if (p + 2 < buf.length && buf[p] == (byte)0xef && buf[p + 1] == (byte)0xbb && buf[p + 2] == (byte)0xbf)
			pos = p + 3;
		return this;
	}

	public boolean end() {
		return end(buf.length);
	}

	public boolean end(int len) {
		for (int b; pos < len; pos++)
			if ((b = buf[pos] & 0xff) > ' ') {
				if (b != '/') // check comment
					return false;
				if ((b = buf[++pos]) == '*') {
					for (pos++; ; )
						if (buf[pos++] == '*' && buf[pos] == '/')
							break;
				} else
					while (b != '\n' && ++pos < len)
						b = buf[pos];
			}
		return true;
	}

	public int next() {
		for (int b; ; pos++) {
			if ((b = buf[pos] & 0xff) > ' ') {
				if (b != '/') // check comment
					return b;
				skipComment();
			}
		}
	}

	public int skipNext() {
		for (int b; ; ) {
			if ((b = buf[++pos] & 0xff) > ' ') {
				if (b != '/') // check comment
					return b;
				skipComment();
			}
		}
	}

	public int skipVar(int e) { // ']' or '}'
		for (int b, c; ; ) {
			if ((b = buf[pos]) == ',' || b == '\n')
				for (; ; )
					if (((((b = buf[++pos]) & 0xff) - ' ' - 1) ^ (',' - ' ' - 1)) > 0) { // (b & 0xff) > ' ' && b != ','
						if (b != '/') // check comment
							return b;
						skipComment();
					}
			if (b == e)
				return b;
			pos++;
			if (b < '"') // fast path
				continue;
			if (b == '"' || b == '\'') {
				while ((c = buf[pos++]) != b)
					if (c == '\\')
						pos++;
			} else if ((b | 0x20) == '{') { // [:0x5B | 0x20 = {:0x7B
				for (int level = 0; (b = buf[pos++] | 0x20) != '}' || --level >= 0; ) { // ]:0x5D | 0x20 = }:0x7D
					if (b == '"' || b == '\'') { // '"' = 0x22; '\'' = 0x27
						while ((c = buf[pos++]) != b)
							if (c == '\\')
								pos++;
					} else if (b == '{') // [:0x5B | 0x20 = {:0x7B
						level++;
					else if (b == '/') { // skip comment
						pos--;
						skipComment();
					}
				}
			} else if (b == '/') { // skip comment
				pos--;
				skipComment();
			}
		}
	}

	private void skipComment() {
		int b;
		if ((b = buf[++pos]) == '*') {
			for (pos++; ; )
				if (buf[pos++] == '*' && buf[pos] == '/')
					break;
		} else
			while (b != '\n')
				b = buf[++pos];
	}

	public int skipColon() {
		int b = next();
		return b == ':' ? skipNext() : b;
	}

	public void skipQuot(int e) {
		for (int b; (b = buf[pos++]) != e; )
			if (b == '\\')
				pos++;
	}

	public @Nullable Object parse() throws ReflectiveOperationException {
		return parse(null, next());
	}

	@SuppressWarnings("unchecked")
	@Nullable Object parse(@Nullable Object obj, int b) throws ReflectiveOperationException {
		//noinspection EnhancedSwitchMigration
		switch (b) { //@formatter:off
		case '{': return parseMap0(obj instanceof Map ? (Map<String, Object>) obj : null);
		case '[': return parseArray0(obj instanceof Collection ? (Collection<Object>) obj : null);
		case '"': case '\'': return parseString(false);
		case '0': case '1': case '2': case '3': case '4': case '5': case '6':
		case '7': case '8': case '9': case '-': case '+': case '.':
		case 'I': case 'i': case 'N': case 'n': return parseNumber();
		case 'f': case 'F': return false;
		case 't': case 'T': return true;
		} //@formatter:on
		return null;
	}

	public @Nullable Collection<Object> parseArray(@Nullable Collection<Object> c) throws ReflectiveOperationException {
		return next() == '[' ? parseArray0(c) : c;
	}

	@NotNull Collection<Object> parseArray0(@Nullable Collection<Object> c) throws ReflectiveOperationException {
		if (c == null)
			c = new ArrayList<>();
		for (int b = skipNext(); b != ']'; b = skipVar(']'))
			c.add(parse(null, b));
		pos++;
		return c;
	}

	public <T> @Nullable Collection<T> parseArray(@Nullable Collection<T> c, @NotNull Class<T> elemClass)
			throws ReflectiveOperationException {
		return parseArray(Json.instance, c, elemClass);
	}

	public <T> @Nullable Collection<T> parseArray(@NotNull Json json, @Nullable Collection<T> c,
												  @NotNull Class<T> elemClass) throws ReflectiveOperationException {
		if (next() != '[')
			return c;
		if (c == null)
			c = new ArrayList<>();
		ClassMeta<T> classMeta = json.getClassMeta(elemClass);
		Parser<T> parser = classMeta.parser;
		if (parser != null) {
			for (int b = skipNext(); b != ']'; b = skipVar(']'))
				c.add(parser.parse(this, classMeta, null, null, null));
		} else {
			if (ClassMeta.isAbstract(elemClass))
				throw new InstantiationException("abstract element class: " + elemClass.getName());
			for (int b = skipNext(); b != ']'; b = skipVar(']'))
				c.add(parse0(classMeta.ctor.create(), classMeta));
		}
		pos++;
		return c;
	}

	public @Nullable Map<String, Object> parseMap(@Nullable Map<String, Object> m) throws ReflectiveOperationException {
		return next() == '{' ? parseMap0(m) : m;
	}

	@NotNull Map<String, Object> parseMap0(@Nullable Map<String, Object> m) throws ReflectiveOperationException {
		if (m == null)
			m = new HashMap<>();
		for (int b = skipNext(); b != '}'; b = skipVar('}')) {
			String k = parseStringKey(this, b);
			m.put(k, parse(null, skipColon()));
		}
		pos++;
		return m;
	}

	public <T> @Nullable T parse(@Nullable Class<T> klass) throws ReflectiveOperationException {
		return klass != null ? parse((T)null, Json.instance.getClassMeta(klass)) : null;
	}

	public <T> @Nullable T parse(@NotNull Json json, @Nullable Class<T> klass) throws ReflectiveOperationException {
		return klass != null ? parse(json, null, json.getClassMeta(klass)) : null;
	}

	public <T> @Nullable T parse(@Nullable ClassMeta<T> classMeta) throws ReflectiveOperationException {
		return parse((T)null, classMeta);
	}

	public <T> @Nullable T parse(@Nullable T obj) throws ReflectiveOperationException {
		return parse(obj, (ClassMeta<T>)null);
	}

	public <T> @Nullable T parse(@Nullable T obj, @Nullable Class<? super T> klass)
			throws ReflectiveOperationException {
		return parse(obj, klass != null ? Json.instance.getClassMeta(klass) : null);
	}

	public <T> @Nullable T parse(@NotNull Json json, @Nullable T obj, @Nullable Class<? super T> klass)
			throws ReflectiveOperationException {
		return parse(json, obj, klass != null ? json.getClassMeta(klass) : null);
	}

	public <T> @Nullable T parse(@Nullable T obj, @Nullable ClassMeta<? super T> classMeta)
			throws ReflectiveOperationException {
		return parse(Json.instance, obj, classMeta);
	}

	public <T> @Nullable T parse(@NotNull Json json, @Nullable T obj) throws ReflectiveOperationException {
		return parse(json, obj, (ClassMeta<T>)null);
	}

	@SuppressWarnings("unchecked")
	public <T> @Nullable T parse(@NotNull Json json, @Nullable T obj, @Nullable ClassMeta<? super T> classMeta)
			throws ReflectiveOperationException {
		if (classMeta == null) {
			if (obj == null)
				return null;
			classMeta = json.getClassMeta((Class<T>)obj.getClass());
		} else {
			KeyReader kr = ClassMeta.getKeyReader(classMeta.klass);
			if (kr != null)
				return (T)kr.parse(this, next());
		}
		Parser<? super T> parser = classMeta.parser;
		if (parser != null)
			return (T)parser.parse0(this, classMeta, null, obj, null);
		if (obj != null)
			return parse0(obj, classMeta);
		if (ClassMeta.isAbstract(classMeta.klass))
			throw new InstantiationException("abstract class: " + classMeta.klass.getName());
		return parse0((T)classMeta.ctor.create(), classMeta);
	}

	public <T> @NotNull T parse0(@NotNull T obj, @NotNull ClassMeta<?> classMeta) throws ReflectiveOperationException {
		if (next() != '{')
			return obj;
		for (int b = skipNext(); b != '}'; b = skipVar('}')) {
			FieldMeta fm = classMeta.get(b == '"' || b == '\'' ? parseKeyHash(b) : parseKeyHashNoQuot(b));
			if (fm == null)
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
				unsafe.putObject(obj, offset, parseString(false));
				break;
			case TYPE_OBJECT:
				unsafe.putObject(obj, offset, parse(unsafe.getObject(obj, offset), b));
				break;
			case TYPE_POS:
				Pos p = (Pos)unsafe.getObject(obj, offset);
				if (p == null)
					unsafe.putObject(obj, offset, p = new Pos());
				p.pos = pos;
				break;
			case TYPE_CUSTOM:
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
					Collection<Object> c = (Collection<Object>)unsafe.getObject(obj, offset);
					if (c == null) {
						Creator<?> ctor = fm.ctor;
						if (ctor != null) {
							@SuppressWarnings("unchecked")
							Collection<Object> c2 = ensureNotNull((Collection<Object>)ctor.create());
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
					Map<Object, Object> m = (Map<Object, Object>)unsafe.getObject(obj, offset);
					if (m == null) {
						Creator<?> ctor = fm.ctor;
						if (ctor != null) {
							@SuppressWarnings("unchecked")
							Map<Object, Object> m2 = ensureNotNull((Map<Object, Object>)ctor.create());
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
	}

	public void parseList0(@NotNull Collection<Object> c, @NotNull ClassMeta<?> classMeta, @NotNull FieldMeta fm)
			throws ReflectiveOperationException {
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
				c.add(parseString(false));
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
					c.add(parser.parse0(this, subClassMeta, fm, null, c));
			} else {
				if (ClassMeta.isAbstract(subClassMeta.klass)) {
					throw new InstantiationException(
							"abstract element class: " + fm.getName() + " in " + classMeta.klass.getName());
				}
				for (; b != ']'; b = skipVar(']'))
					c.add(parse0(subClassMeta.ctor.create(), subClassMeta));
			}
			break;
		}
		pos++;
	}

	public void parseMap0(@NotNull Map<Object, Object> m, @NotNull ClassMeta<?> classMeta, @NotNull FieldMeta fm)
			throws ReflectiveOperationException {
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
					skipColon();
					m.put(k, parser.parse0(this, subClassMeta, fm, null, m));
				}
			} else {
				if (ClassMeta.isAbstract(subClassMeta.klass)) {
					throw new InstantiationException(
							"abstract value class: " + fm.getName() + " in " + classMeta.klass.getName());
				}
				for (; b != '}'; b = skipVar('}')) {
					Object k = keyParser.parse(this, b);
					skipColon();
					m.put(k, parse0(subClassMeta.ctor.create(), subClassMeta));
				}
			}
			break;
		}
		pos++;
	}

	@SuppressWarnings("UnnecessaryLocalVariable")
	int parseKeyHash(int e) {
		pos++; // skip the first '"' or '\''
		int b = buf[pos++];
		if (b == e)
			return 0;
		if (b == '\\')
			b = buf[pos++];
		for (int h = b, m = keyHashMultiplier; ; h = h * m + b) {
			if ((b = buf[pos++]) == e)
				return h;
			if (b == '\\')
				b = buf[pos++];
		}
	}

	int parseKeyHashNoQuot(int b) {
		if (b == ':')
			return 0;
		if (b == '\\')
			b = buf[++pos];
		for ( //noinspection UnnecessaryLocalVariable
				int h = b, m = keyHashMultiplier; ; h = h * m + b) {
			if (((((b = buf[++pos]) & 0xff) - ' ' - 1) ^ (':' - ' ' - 1)) <= 0) // (b & 0xff) <= ' ' || b == ':'
				return h;
			if (b == '/') // check comment
				return h;
			if (b == '\\')
				b = buf[++pos];
		}
	}

	public static @NotNull String parseStringKey(@NotNull JsonReader jr, int b) {
		String key = b == '"' || b == '\'' ? jr.parseString(true) : jr.parseStringNoQuot();
		return key != null ? key : "";
	}

	public static @NotNull Boolean parseBooleanKey(@NotNull JsonReader jr, int b) {
		boolean v;
		if (b == '"' || b == '\'') {
			v = jr.buf[++jr.pos] == 't';
			jr.skipQuot(b);
		} else
			v = jr.buf[++jr.pos] == 't';
		return v;
	}

	public static @NotNull Byte parseByteKey(@NotNull JsonReader jr, int b) {
		int v;
		if (b == '"' || b == '\'') {
			jr.pos++;
			v = jr.parseInt();
			jr.skipQuot(b);
		} else
			v = jr.parseInt();
		return (byte)v;
	}

	public static @NotNull Short parseShortKey(@NotNull JsonReader jr, int b) {
		int v;
		if (b == '"' || b == '\'') {
			jr.pos++;
			v = jr.parseInt();
			jr.skipQuot(b);
		} else
			v = jr.parseInt();
		return (short)v;
	}

	public static @NotNull Character parseCharKey(@NotNull JsonReader jr, int b) {
		int v;
		if (b == '"' || b == '\'') {
			jr.pos++;
			v = jr.parseInt();
			jr.skipQuot(b);
		} else
			v = jr.parseInt();
		return (char)v;
	}

	public static @NotNull Integer parseIntegerKey(@NotNull JsonReader jr, int b) {
		int v;
		if (b == '"' || b == '\'') {
			jr.pos++;
			v = jr.parseInt();
			jr.skipQuot(b);
		} else
			v = jr.parseInt();
		return v;
	}

	public static @NotNull Long parseLongKey(@NotNull JsonReader jr, int b) {
		long v;
		if (b == '"' || b == '\'') {
			jr.pos++;
			v = jr.parseLong();
			jr.skipQuot(b);
		} else
			v = jr.parseLong();
		return v;
	}

	public static @NotNull Float parseFloatKey(@NotNull JsonReader jr, int b) {
		double v;
		if (b == '"' || b == '\'') {
			jr.pos++;
			v = jr.parseDouble();
			jr.skipQuot(b);
		} else
			v = jr.parseDouble();
		return (float)v;
	}

	public static @NotNull Double parseDoubleKey(@NotNull JsonReader jr, int b) {
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

	public byte[] parseByteString() {
		final byte[] buffer = buf;
		int p = pos, b, e = buffer[p];
		if (e != '"' && e != '\'')
			return null;
		final int begin = ++p;
		for (; ; p++) {
			if ((b = buffer[p]) == e) {
				pos = p + 1; // lucky! finished the fast path
				return Arrays.copyOfRange(buffer, begin, p);
			}
			if (b == '\\')
				break; // jump to the slow path below
		}
		int len = p - begin, n = len;
		while ((b = buffer[p++]) != e) {
			if (b == '\\' && buffer[p++] == 'u') {
				b = parseHex4(buffer, p);
				p += 4;
				if (b >= 0x800) {
					if ((b & 0xfc00) == 0xd800 && buffer[p] == '\\' && buffer[p + 1] == 'u'
							&& (parseHex4(buffer, p + 2) & 0xfc00) == 0xdc00) {
						p += 6;
						len += 4;
					} else
						len += 3;
				} else if (b >= 0x80)
					len += 2;
				else
					len++;
			} else
				len++;
		}
		byte[] t = new byte[len];
		System.arraycopy(buffer, begin, t, 0, n);
		for (p = begin + n; ; ) {
			if ((b = buffer[p++]) == e) {
				pos = p;
				return t;
			}
			if (b == '\\') {
				if ((b = buffer[p++]) == 'u') {
					b = parseHex4(buffer, p);
					p += 4;
					if (b >= 0x800) {
						if ((b & 0xfc00) == 0xd800 && buffer[p] == '\\' && buffer[p + 1] == 'u'
								&& ((len = parseHex4(buffer, p + 2)) & 0xfc00) == 0xdc00) {
							p += 6;
							b = (b << 10) + len + (0x10000 - (0xd800 << 10) - 0xdc00);
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
				} else
					t[n++] = b >= 0x20 ? ESCAPE[b - 0x20] : (byte)b;
			} else
				t[n++] = (byte)b;
		}
	}

	public @Nullable String parseString() {
		return parseString(false);
	}

	public @Nullable String parseString(boolean intern) {
		final byte[] buffer = buf;
		int p = pos, b, e = buffer[p];
		if (e != '"' && e != '\'')
			return null;
		final int begin = ++p;
		for (; ; p++) {
			if ((b = buffer[p]) == e) {
				pos = p + 1; // lucky! finished the fast path
				return intern ? intern(buffer, begin, p) : newByteString(buffer, begin, p);
			}
			if ((b ^ '\\') < 1) // '\\' or multibyte char
				break; // jump to the slow path below
		}
		int len = p - begin, n = len, c, d, f;
		for (; (b = buffer[p++]) != e; len++)
			if (b == '\\' && buffer[p++] == 'u')
				p += 4;
		char[] t = tmp;
		if (t == null || t.length < len)
			tmp = t = new char[len];
		p = begin;
		for (int i = 0; i < n; )
			t[i++] = (char)(buffer[p++] & 0xff);
		for (; ; ) {
			if ((b = buffer[p++]) == e) {
				pos = p;
				return new String(t, 0, n);
			}
			if (b == '\\') {
				if ((b = buffer[p++]) == 'u') {
					t[n++] = (char)parseHex4(buffer, p);
					p += 4;
				} else
					t[n++] = (char)(b >= 0x20 ? ESCAPE[b - 0x20] : b & 0xff);
			} else if (b >= 0)
				t[n++] = (char)b; // 0xxx xxxx
			else if (b >= -0x20) {
				if (b >= -0x10) {
					if ((c = buffer[p]) < -0x40 && (d = buffer[p + 1]) < -0x40 && (f = buffer[p + 2]) < -0x40) {
						b = (b << 18) + (c << 12) + (d << 6) + f + ((0x10 << 18) + (0x80 << 12) + (0x80 << 6) + 0x80 - 0x10000);
						t[n++] = (char)(0xd800 + ((b >> 10) & 0x3ff)); // 1111 0xxx  10xx xxxx  10xx xxxx  10xx xxxx
						t[n++] = (char)(0xdc00 + (b & 0x3ff));
						p += 3;
					} else
						t[n++] = (char)(b & 0xff); // ignore malformed utf-8
				} else if ((c = buffer[p]) < -0x40 && (d = buffer[p + 1]) < -0x40) {
					t[n++] = (char)((b << 12) + (c << 6) + d + ((0x20 << 12) + (0x80 << 6) + 0x80)); // 1110 xxxx  10xx xxxx  10xx xxxx
					p += 2;
				} else
					t[n++] = (char)(b & 0xff); // ignore malformed utf-8
			} else if ((c = buffer[p]) < -0x40) {
				p++;
				t[n++] = (char)((b << 6) + c + ((0x40 << 6) + 0x80)); // 110x xxxx  10xx xxxx
			} else
				t[n++] = (char)(b & 0xff); // ignore malformed utf-8
		}
	}

	public @NotNull String parseStringNoQuot() {
		final byte[] buffer = buf;
		int p = pos, b;
		final int begin = p;
		for (; ; p++) {
			if (((((b = buffer[p]) & 0xff) - ' ' - 1) ^ (':' - ' ' - 1)) <= 0) // (b & 0xff) <= ' ' || b == ':'
				return intern(buffer, begin, pos = p); // lucky! finished the fast path
			if ((b ^ '\\') < 1) // '\\' or multibyte char
				break; // jump to the slow path below
		}
		int len = p - begin, n = len, c, d, e;
		for (; ((((b = buffer[p++]) & 0xff) - ' ' - 1) ^ (':' - ' ' - 1)) > 0; len++) // (b & 0xff) > ' ' && b != ':'
			if (b == '\\' && buffer[p++] == 'u')
				p += 4;
		char[] t = tmp;
		if (t == null || t.length < len)
			tmp = t = new char[len];
		p = begin;
		for (int i = 0; i < n; )
			t[i++] = (char)(buffer[p++] & 0xff);
		for (; ; ) {
			if ((((b = buffer[p++] & 0xff) - ' ' - 1) ^ (':' - ' ' - 1)) <= 0) { // (b & 0xff) <= ' ' || b == ':'
				pos = p;
				return new String(t, 0, n);
			}
			if (b == '\\') {
				if ((b = buffer[p++]) == 'u') {
					t[n++] = (char)parseHex4(buffer, p);
					p += 4;
				} else
					t[n++] = (char)(b >= 0x20 ? ESCAPE[b - 0x20] : b);
			} else if (b < 0x80)
				t[n++] = (char)b; // 0xxx xxxx
			else if (b > 0xdf) {
				if (b > 0xef) {
					if ((c = buffer[p]) < -0x40 && (d = buffer[p + 1]) < -0x40 && (e = buffer[p + 2]) < -0x40) {
						b = (b << 18) + (c << 12) + (d << 6) + e + ((-0xf0 << 18) + (0x80 << 12) + (0x80 << 6) + 0x80 - 0x10000);
						t[n++] = (char)(0xd800 + ((b >> 10) & 0x3ff)); // 1111 0xxx  10xx xxxx  10xx xxxx  10xx xxxx
						t[n++] = (char)(0xdc00 + (b & 0x3ff));
						p += 3;
					} else
						t[n++] = (char)(b & 0xff); // ignore malformed utf-8
				} else if ((c = buffer[p]) < -0x40 && (d = buffer[p + 1]) < -0x40) {
					t[n++] = (char)((b << 12) + (c << 6) + d + ((-0xe0 << 12) + (0x80 << 6) + 0x80)); // 1110 xxxx  10xx xxxx  10xx xxxx
					p += 2;
				} else
					t[n++] = (char)b; // ignore malformed utf-8
			} else if ((c = buffer[p]) < -0x40) {
				p++;
				t[n++] = (char)((b << 6) + c + ((-0xc0 << 6) + 0x80)); // 110x xxxx  10xx xxxx
			} else
				t[n++] = (char)b; // ignore malformed utf-8
		}
	}

	static double strtod(double d, int e) {
		return e >= 0 ? d * EXP[e] : (e < -308 ? 0 : d / EXP[-e]);
	}

	public int parseInt() {
		final byte[] buffer = buf;
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
			if (b == '0')
				b = buffer[++p];
			else if ((i = (b - '0') & 0xff) < 10) {
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
							if (!expMinus)
								return minus ? Integer.MIN_VALUE : Integer.MAX_VALUE;
							do
								b = buffer[++p];
							while (((b - '0') & 0xff) < 10);
						}
					}
				}
			}
		} catch (IndexOutOfBoundsException ignored) {
			if (useDouble == 1)
				d = i;
		}
		pos = p;
		if (expMinus)
			exp = -exp;
		if (useDouble > 0) {
			exp += expFrac;
			d = exp < -308 ? strtod(d / 1e308, exp + 308) : strtod(d, exp);
			i = (int)(minus ? -d : d);
		} else if (minus)
			i = -i;
		return i;
	}

	public long parseLong() {
		final byte[] buffer = buf;
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
			if (b == '0')
				b = buffer[++p];
			else if ((i = (b - '0') & 0xff) < 10) {
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
							if (!expMinus)
								return minus ? Long.MIN_VALUE : Long.MAX_VALUE;
							do
								b = buffer[++p];
							while (((b - '0') & 0xff) < 10);
						}
					}
				}
			}
		} catch (IndexOutOfBoundsException ignored) {
			if (useDouble == 1)
				d = i;
		}
		pos = p;
		if (expMinus)
			exp = -exp;
		if (useDouble > 0) {
			exp += expFrac;
			d = exp < -308 ? strtod(d / 1e308, exp + 308) : strtod(d, exp);
			i = (long)(minus ? -d : d);
		} else if (minus)
			i = -i;
		return i;
	}

	public double parseDouble() {
		final byte[] buffer = buf;
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
			if (b == '0')
				b = buffer[++p];
			else if ((i = (b - '0') & 0xff) < 10) {
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
							if (!expMinus)
								return minus ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
							do
								b = buffer[++p];
							while (((b - '0') & 0xff) < 10);
						}
					}
				}
			}
		} catch (IndexOutOfBoundsException ignored) {
			if (useDouble == 1)
				d = i;
		}
		pos = p;
		if (expMinus)
			exp = -exp;
		if (useDouble > 0) {
			exp += expFrac;
			d = exp < -308 ? strtod(d / 1e308, exp + 308) : strtod(d, exp);
			if (minus)
				d = -d;
		} else
			d = minus ? -i : i;
		return d;
	}

	public @NotNull Object parseNumber() {
		final byte[] buffer = buf;
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
				if (c == 'i' || c == 'n') { // Infinity NaN
					do
						b = buffer[++p];
					while ((((b | 0x20) - 'a') & 0xff) < 26);
					return c == 'n' ? Double.NaN : minus ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
				}
			}
			if (b == '0') {
				b = buffer[++p];
				if ((b | 0x20) == 'x') { // 0x
					for (; ; ) {
						b = buffer[++p];
						if ((c = (b - '0') & 0xff) < 10)
							i = i * 16 + c;
						else if ((c = ((b | 0x20) - 'a') & 0xff) < 6)
							i = i * 16 + c + 10;
						else
							break;
					}
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
							if (!expMinus)
								return minus ? NEGATIVE_INFINITY : POSITIVE_INFINITY;
							do
								b = buffer[++p];
							while (((b - '0') & 0xff) < 10);
						}
					}
				}
			}
		} catch (IndexOutOfBoundsException ignored) {
			if (useDouble == 1)
				d = i;
		}
		pos = p;
		if (expMinus)
			exp = -exp;
		if (useDouble > 0) {
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
