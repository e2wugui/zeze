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
  NO : Additional white space characters are allowed: \t,\n,\v(\x0b),\f(\x0c),\r, (\x20),\xa0,\u2028,\u2029,\uFEFF
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
			1e+0, 1e+1, 1e+2, 1e+3, 1e+4, 1e+5, 1e+6, 1e+7, 1e+8, 1e+9, 1e+10, 1e+11, 1e+12, 1e+13, 1e+14, 1e+15, 1e+16,
			1e+17, 1e+18, 1e+19, 1e+20, 1e+21, 1e+22, 1e+23, 1e+24, 1e+25, 1e+26, 1e+27, 1e+28, 1e+29, 1e+30, 1e+31,
			1e+32, 1e+33, 1e+34, 1e+35, 1e+36, 1e+37, 1e+38, 1e+39, 1e+40, 1e+41, 1e+42, 1e+43, 1e+44, 1e+45, 1e+46,
			1e+47, 1e+48, 1e+49, 1e+50, 1e+51, 1e+52, 1e+53, 1e+54, 1e+55, 1e+56, 1e+57, 1e+58, 1e+59, 1e+60, 1e+61,
			1e+62, 1e+63, 1e+64, 1e+65, 1e+66, 1e+67, 1e+68, 1e+69, 1e+70, 1e+71, 1e+72, 1e+73, 1e+74, 1e+75, 1e+76,
			1e+77, 1e+78, 1e+79, 1e+80, 1e+81, 1e+82, 1e+83, 1e+84, 1e+85, 1e+86, 1e+87, 1e+88, 1e+89, 1e+90, 1e+91,
			1e+92, 1e+93, 1e+94, 1e+95, 1e+96, 1e+97, 1e+98, 1e+99, 1e+100, 1e+101, 1e+102, 1e+103, 1e+104, 1e+105,
			1e+106, 1e+107, 1e+108, 1e+109, 1e+110, 1e+111, 1e+112, 1e+113, 1e+114, 1e+115, 1e+116, 1e+117, 1e+118,
			1e+119, 1e+120, 1e+121, 1e+122, 1e+123, 1e+124, 1e+125, 1e+126, 1e+127, 1e+128, 1e+129, 1e+130, 1e+131,
			1e+132, 1e+133, 1e+134, 1e+135, 1e+136, 1e+137, 1e+138, 1e+139, 1e+140, 1e+141, 1e+142, 1e+143, 1e+144,
			1e+145, 1e+146, 1e+147, 1e+148, 1e+149, 1e+150, 1e+151, 1e+152, 1e+153, 1e+154, 1e+155, 1e+156, 1e+157,
			1e+158, 1e+159, 1e+160, 1e+161, 1e+162, 1e+163, 1e+164, 1e+165, 1e+166, 1e+167, 1e+168, 1e+169, 1e+170,
			1e+171, 1e+172, 1e+173, 1e+174, 1e+175, 1e+176, 1e+177, 1e+178, 1e+179, 1e+180, 1e+181, 1e+182, 1e+183,
			1e+184, 1e+185, 1e+186, 1e+187, 1e+188, 1e+189, 1e+190, 1e+191, 1e+192, 1e+193, 1e+194, 1e+195, 1e+196,
			1e+197, 1e+198, 1e+199, 1e+200, 1e+201, 1e+202, 1e+203, 1e+204, 1e+205, 1e+206, 1e+207, 1e+208, 1e+209,
			1e+210, 1e+211, 1e+212, 1e+213, 1e+214, 1e+215, 1e+216, 1e+217, 1e+218, 1e+219, 1e+220, 1e+221, 1e+222,
			1e+223, 1e+224, 1e+225, 1e+226, 1e+227, 1e+228, 1e+229, 1e+230, 1e+231, 1e+232, 1e+233, 1e+234, 1e+235,
			1e+236, 1e+237, 1e+238, 1e+239, 1e+240, 1e+241, 1e+242, 1e+243, 1e+244, 1e+245, 1e+246, 1e+247, 1e+248,
			1e+249, 1e+250, 1e+251, 1e+252, 1e+253, 1e+254, 1e+255, 1e+256, 1e+257, 1e+258, 1e+259, 1e+260, 1e+261,
			1e+262, 1e+263, 1e+264, 1e+265, 1e+266, 1e+267, 1e+268, 1e+269, 1e+270, 1e+271, 1e+272, 1e+273, 1e+274,
			1e+275, 1e+276, 1e+277, 1e+278, 1e+279, 1e+280, 1e+281, 1e+282, 1e+283, 1e+284, 1e+285, 1e+286, 1e+287,
			1e+288, 1e+289, 1e+290, 1e+291, 1e+292, 1e+293, 1e+294, 1e+295, 1e+296, 1e+297, 1e+298, 1e+299, 1e+300,
			1e+301, 1e+302, 1e+303, 1e+304, 1e+305, 1e+306, 1e+307, 1e+308};

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

	static @NotNull String intern(byte[] buf, int pos, int end) throws ReflectiveOperationException {
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
			if ((b = buf[pos]) == ',')
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
	@Nullable
	Object parse(@Nullable Object obj, int b) throws ReflectiveOperationException {
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

	@NotNull
	Collection<Object> parseArray0(@Nullable Collection<Object> c) throws ReflectiveOperationException {
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

	@NotNull
	Map<String, Object> parseMap0(@Nullable Map<String, Object> m) throws ReflectiveOperationException {
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
							if (newSubObj != null && !fm.klass.isAssignableFrom(newSubObj.getClass()))
								throw new InstantiationException("incompatible type(" + newSubObj.getClass()
										+ ") for field: " + fm.getName() + " in " + classMeta.klass.getName());
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
						if (ClassMeta.isAbstract(subClassMeta.klass))
							throw new InstantiationException(
									"abstract field: " + fm.getName() + " in " + classMeta.klass.getName());
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
							if (parser == null)
								throw new InstantiationException("abstract Collection field: " + fm.getName() + " in "
										+ classMeta.klass.getName());
							Object c2 = parser.parse0(this, cm, fm, null, obj);
							if (c2 != null && !fm.klass.isAssignableFrom(c2.getClass()))
								throw new InstantiationException(
										"incompatible type(" + c2.getClass() + ") for Collection field: " + fm.getName()
												+ " in " + classMeta.klass.getName());
							unsafe.putObject(obj, offset, c2);
							break;
						}
					} else
						c.clear();
					b = skipNext();
					switch (type & 0xf) {
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
							if (ClassMeta.isAbstract(subClassMeta.klass))
								throw new InstantiationException(
										"abstract element class: " + fm.getName() + " in " + classMeta.klass.getName());
							for (; b != ']'; b = skipVar(']'))
								c.add(parse0(subClassMeta.ctor.create(), subClassMeta));
						}
						break;
					}
					pos++;
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
							if (parser == null)
								throw new InstantiationException(
										"abstract Map field: " + fm.getName() + " in " + classMeta.klass.getName());
							Object m2 = parser.parse0(this, cm, fm, null, obj);
							if (m2 != null && !fm.klass.isAssignableFrom(m2.getClass()))
								throw new InstantiationException("incompatible type(" + m2.getClass()
										+ ") for Map field: " + fm.getName() + " in " + classMeta.klass.getName());
							unsafe.putObject(obj, offset, m2);
							break;
						}
					} else
						m.clear();
					b = skipNext();
					KeyReader keyParser = ensureNotNull(fm.keyParser);
					switch (type & 0xf) {
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
							if (ClassMeta.isAbstract(subClassMeta.klass))
								throw new InstantiationException(
										"abstract value class: " + fm.getName() + " in " + classMeta.klass.getName());
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
			}
		}
		pos++;
		return obj;
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

	public static @NotNull String parseStringKey(@NotNull JsonReader jr, int b) throws ReflectiveOperationException {
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

	public @Nullable String parseString() throws ReflectiveOperationException {
		return parseString(false);
	}

	public @Nullable String parseString(boolean intern) throws ReflectiveOperationException {
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

	public @NotNull String parseStringNoQuot() throws ReflectiveOperationException {
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
				b = buffer[++p];
				if (useDouble == 0) {
					useDouble = 1;
					for (; (c = (b - '0') & 0xff) < 10; b = buffer[++p]) {
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
				for (; (c = (b - '0') & 0xff) < 10; b = buffer[++p]) {
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
		}
		pos = p;
		if (expMinus)
			exp = -exp;
		if (useDouble > 0) {
			//noinspection ConstantConditions
			if (useDouble == 1)
				d = i;
			exp += expFrac;
			d = exp < -308 ? strtod(strtod(d, -308), exp + 308) : strtod(d, exp);
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
				b = buffer[++p];
				if (useDouble == 0) {
					useDouble = 1;
					for (; (c = (b - '0') & 0xff) < 10; b = buffer[++p]) {
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
				for (; (c = (b - '0') & 0xff) < 10; b = buffer[++p]) {
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
		}
		pos = p;
		if (expMinus)
			exp = -exp;
		if (useDouble > 0) {
			//noinspection ConstantConditions
			if (useDouble == 1)
				d = i;
			exp += expFrac;
			d = exp < -308 ? strtod(strtod(d, -308), exp + 308) : strtod(d, exp);
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
				b = buffer[++p];
				if (useDouble == 0) {
					useDouble = 1;
					for (; (c = (b - '0') & 0xff) < 10; b = buffer[++p]) {
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
				for (; (c = (b - '0') & 0xff) < 10; b = buffer[++p]) {
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
		}
		pos = p;
		if (expMinus)
			exp = -exp;
		if (useDouble > 0) {
			//noinspection ConstantConditions
			if (useDouble == 1)
				d = i;
			exp += expFrac;
			d = exp < -308 ? strtod(strtod(d, -308), exp + 308) : strtod(d, exp);
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
				b = buffer[++p];
				if (useDouble == 0) {
					useDouble = 1;
					for (; (c = (b - '0') & 0xff) < 10; b = buffer[++p]) {
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
				for (; (c = (b - '0') & 0xff) < 10; b = buffer[++p]) {
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
		}
		pos = p;
		if (expMinus)
			exp = -exp;
		if (useDouble > 0) {
			//noinspection ConstantConditions
			if (useDouble == 1)
				d = i;
			exp += expFrac;
			d = exp < -308 ? strtod(strtod(d, -308), exp + 308) : strtod(d, exp);
			return minus ? -d : d;
		}
		if (minus)
			i = -i;
		final int j = (int)i;
		return j == i ? (Object)j : i;
	}
}
