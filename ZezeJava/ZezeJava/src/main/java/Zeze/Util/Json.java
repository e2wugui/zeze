package Zeze.Util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.BeanKey;
import Zeze.Transaction.Collections.CollOne;
import Zeze.Transaction.Collections.PList2;
import Zeze.Transaction.Collections.PMap2;
import Zeze.Transaction.Data;
import Zeze.Transaction.DynamicBean;
import Zeze.Transaction.EmptyBean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sun.misc.Unsafe;

// Compile with JDK11+; Run with JDK8+ (JDK9+ is recommended); Android is NOT supported
public final class Json implements Cloneable {
	static final int TYPE_BOOLEAN = 1; // boolean, Boolean
	static final int TYPE_BYTE = 2; // byte, Byte
	static final int TYPE_SHORT = 3; // short, Short
	static final int TYPE_CHAR = 4; // char, Character
	static final int TYPE_INT = 5; // int, Integer
	static final int TYPE_LONG = 6; // long, Long
	static final int TYPE_FLOAT = 7; // float, Float
	static final int TYPE_DOUBLE = 8; // double, Double
	static final int TYPE_STRING = 9; // String
	static final int TYPE_OBJECT = 10; // Object(null,Boolean,Integer,Long,Double,String,ArrayList<?>,HashMap<?,?>)
	static final int TYPE_POS = 11; // Pos(pos)
	static final int TYPE_CUSTOM = 12; // user custom type
	static final int TYPE_WRAP_FLAG = 0x10; // wrap<1~8>
	static final int TYPE_LIST_FLAG = 0x20; // Collection<1~12> (parser only needs clear() & add(v))
	static final int TYPE_MAP_FLAG = 0x30; // Map<String, 1~12> (parser only needs clear() & put(k,v))

	public interface KeyReader {
		@NotNull Object parse(@NotNull JsonReader jr, int b) throws ReflectiveOperationException;
	}

	public interface Creator<T> {
		T create() throws ReflectiveOperationException;
	}

	public static final class FieldMeta {
		public final int hash; // for FieldMetaMap
		public final int type; // defined above
		public final int offset; // for unsafe access
		public final @NotNull Class<?> klass; // TYPE_CUSTOM:fieldClass; TYPE_LIST_FLAG/TYPE_MAP_FLAG:subValueClass
		transient @Nullable ClassMeta<?> classMeta; // from klass, lazy assigned
		transient @Nullable FieldMeta next; // for FieldMetaMap
		public final byte[] name; // field name
		public final @Nullable Creator<?> ctor; // for TYPE_LIST_FLAG/TYPE_MAP_FLAG
		public final @Nullable KeyReader keyParser; // for TYPE_MAP_FLAG
		public final @NotNull Field field;
		public final @Nullable Type[] paramTypes;

		public FieldMeta(int type, int offset, @NotNull String name, @NotNull Class<?> klass, @Nullable Creator<?> ctor,
						 @Nullable KeyReader keyReader, @NotNull Field field) {
			this.name = name.getBytes(StandardCharsets.UTF_8);
			this.hash = getKeyHash(this.name, 0, this.name.length);
			this.type = type;
			this.offset = offset;
			this.klass = klass;
			this.ctor = ctor;
			this.keyParser = keyReader;
			this.field = field;
			Type geneType = field.getGenericType();
			paramTypes = geneType instanceof ParameterizedType
					? ((ParameterizedType)geneType).getActualTypeArguments() : null;
		}

		public @NotNull String getName() {
			return new String(name, StandardCharsets.UTF_8);
		}
	}

	public interface Parser<T> {
		@Nullable T parse(@NotNull JsonReader reader, @NotNull ClassMeta<T> classMeta, @Nullable FieldMeta fieldMeta,
						  @Nullable T obj, @Nullable Object parent) throws ReflectiveOperationException;

		@SuppressWarnings("unchecked")
		default @Nullable T parse0(@NotNull JsonReader reader, @NotNull ClassMeta<?> classMeta,
								   @Nullable FieldMeta fieldMeta, @Nullable Object obj,
								   @Nullable Object parent) throws ReflectiveOperationException {
			return parse(reader, (ClassMeta<T>)classMeta, fieldMeta, (T)obj, parent);
		}
	}

	public interface Writer<T> {
		void write(@NotNull JsonWriter writer, @NotNull ClassMeta<T> classMeta, @Nullable T obj);

		// ensure +1
		@SuppressWarnings("unchecked")
		default void write0(@NotNull JsonWriter writer, @NotNull ClassMeta<?> classMeta, @Nullable Object obj) {
			write(writer, (ClassMeta<T>)classMeta, (T)obj);
		}
	}

	public static final class Pos {
		public int pos;

		public Pos() {
		}

		public Pos(int p) {
			pos = p;
		}
	}

	public static final class ClassMeta<T> {
		private static final @NotNull HashMap<Class<?>, Integer> typeMap = new HashMap<>(32);
		private static final @NotNull HashMap<Type, KeyReader> keyReaderMap = new HashMap<>(16);

		final @NotNull Json json;
		final @NotNull Class<T> klass;
		final @NotNull Creator<T> ctor;
		private final FieldMeta[] valueTable;
		final FieldMeta[] fieldMetas;
		transient @Nullable Parser<T> parser; // user custom parser
		transient @Nullable Writer<T> writer; // user custom writer

		static {
			typeMap.put(boolean.class, TYPE_BOOLEAN);
			typeMap.put(byte.class, TYPE_BYTE);
			typeMap.put(short.class, TYPE_SHORT);
			typeMap.put(char.class, TYPE_CHAR);
			typeMap.put(int.class, TYPE_INT);
			typeMap.put(long.class, TYPE_LONG);
			typeMap.put(float.class, TYPE_FLOAT);
			typeMap.put(double.class, TYPE_DOUBLE);
			typeMap.put(String.class, TYPE_STRING);
			typeMap.put(Object.class, TYPE_OBJECT);
			typeMap.put(Pos.class, TYPE_POS);
			typeMap.put(Boolean.class, TYPE_WRAP_FLAG + TYPE_BOOLEAN);
			typeMap.put(Byte.class, TYPE_WRAP_FLAG + TYPE_BYTE);
			typeMap.put(Short.class, TYPE_WRAP_FLAG + TYPE_SHORT);
			typeMap.put(Character.class, TYPE_WRAP_FLAG + TYPE_CHAR);
			typeMap.put(Integer.class, TYPE_WRAP_FLAG + TYPE_INT);
			typeMap.put(Long.class, TYPE_WRAP_FLAG + TYPE_LONG);
			typeMap.put(Float.class, TYPE_WRAP_FLAG + TYPE_FLOAT);
			typeMap.put(Double.class, TYPE_WRAP_FLAG + TYPE_DOUBLE);
			keyReaderMap.put(Boolean.class, JsonReader::parseBooleanKey);
			keyReaderMap.put(Byte.class, JsonReader::parseByteKey);
			keyReaderMap.put(Short.class, JsonReader::parseShortKey);
			keyReaderMap.put(Character.class, JsonReader::parseCharKey);
			keyReaderMap.put(Integer.class, JsonReader::parseIntegerKey);
			keyReaderMap.put(Long.class, JsonReader::parseLongKey);
			keyReaderMap.put(Float.class, JsonReader::parseFloatKey);
			keyReaderMap.put(Double.class, JsonReader::parseDoubleKey);
			keyReaderMap.put(String.class, JsonReader::parseStringKey);
			keyReaderMap.put(Object.class, JsonReader::parseStringKey);
		}

		static boolean isInKeyReaderMap(Class<?> klass) {
			return keyReaderMap.containsKey(klass);
		}

		public static KeyReader getKeyReader(Class<?> klass) {
			return keyReaderMap.get(klass);
		}

		static boolean isAbstract(@NotNull Class<?> klass) {
			return (klass.getModifiers() & (Modifier.INTERFACE | Modifier.ABSTRACT)) != 0;
		}

		@SuppressWarnings("unchecked")
		public static <T> @NotNull Creator<T> getDefCtor(@NotNull Class<T> klass) {
			for (Constructor<?> c : klass.getDeclaredConstructors()) {
				if (c.getParameterCount() == 0) {
					setAccessible(c);
					return () -> (T)c.newInstance((Object[])null);
				}
			}
			return () -> (T)unsafe.allocateInstance(klass);
		}

		private static @Nullable Class<?> getCollectionSubClass(Type geneType) { // X<T>, X extends Y<T>, X implements Y<T>
			if (geneType instanceof ParameterizedType) {
				//noinspection PatternVariableCanBeUsed
				ParameterizedType paraType = (ParameterizedType)geneType;
				Class<?> rawClass = (Class<?>)paraType.getRawType();
				if (Collection.class.isAssignableFrom(rawClass)) {
					Type type = paraType.getActualTypeArguments()[0];
					if (type instanceof Class)
						return (Class<?>)type;
				}
			}
			if (geneType instanceof Class) {
				//noinspection PatternVariableCanBeUsed
				Class<?> klass = (Class<?>)geneType;
				for (Type subType : klass.getGenericInterfaces()) {
					Class<?> subClass = getCollectionSubClass(subType);
					if (subClass != null)
						return subClass;
				}
				return getCollectionSubClass(klass.getGenericSuperclass());
			}
			return null;
		}

		private static Class<?> getRawClass(Type type) {
			if (type instanceof Class)
				return (Class<?>)type;
			if (type instanceof ParameterizedType)
				return (Class<?>)((ParameterizedType)type).getRawType();
			if (type instanceof TypeVariable) {
				Type[] bounds = ((TypeVariable<?>)type).getBounds();
				if (bounds.length > 0)
					return getRawClass(bounds[0]);
			}
			return Object.class;
		}

		private static Type[] getMapSubClasses(Type geneType) { // X<K,V>, X extends Y<K,V>, X implements Y<K,V>
			if (geneType instanceof ParameterizedType) {
				//noinspection PatternVariableCanBeUsed
				ParameterizedType paraType = (ParameterizedType)geneType;
				if (Map.class.isAssignableFrom((Class<?>)paraType.getRawType())) {
					Type[] subTypes = paraType.getActualTypeArguments();
					if (subTypes.length == 2) {
						subTypes[0] = getRawClass(subTypes[0]);
						subTypes[1] = getRawClass(subTypes[1]);
						return subTypes;
					}
				}
			}
			if (geneType instanceof Class) {
				//noinspection PatternVariableCanBeUsed
				Class<?> klass = (Class<?>)geneType;
				for (Type subType : klass.getGenericInterfaces()) {
					Type[] subTypes = getMapSubClasses(subType);
					if (subTypes != null)
						return subTypes;
				}
				return getMapSubClasses(klass.getGenericSuperclass());
			}
			return null;
		}

		ClassMeta(final @NotNull Json json, final @NotNull Class<T> klass) {
			this.json = json;
			this.klass = klass;
			ctor = getDefCtor(klass);
			int size = 0;
			for (Class<?> c = klass; c != null; c = c.getSuperclass())
				for (Field field : getDeclaredFields(c))
					if ((field.getModifiers() & (Modifier.STATIC | Modifier.TRANSIENT)) == 0
							&& !field.getName().startsWith("this$"))
						size++;
			valueTable = new FieldMeta[1 << (32 - Integer.numberOfLeadingZeros(size * 2 - 1))];
			fieldMetas = new FieldMeta[size];
			ArrayList<Class<? super T>> classes = new ArrayList<>(2);
			for (Class<? super T> c = klass; c != null; c = c.getSuperclass())
				classes.add(c);
			for (int i = classes.size() - 1, j = 0; i >= 0; i--) {
				Class<? super T> c = classes.get(i);
				for (Field field : getDeclaredFields(c)) {
					if ((field.getModifiers() & (Modifier.STATIC | Modifier.TRANSIENT)) != 0)
						continue;
					final String fieldName = field.getName();
					if (fieldName.startsWith("this$")) // closure field
						continue;
					Class<?> fieldClass = ensureNotNull(field.getType());
					Creator<?> fieldCtor = null;
					KeyReader keyReader = null;
					Integer v = typeMap.get(fieldClass);
					int type;
					if (v != null)
						type = v;
					else if (Collection.class.isAssignableFrom(fieldClass)) { // Collection<?>
						if (!isAbstract(fieldClass))
							fieldCtor = getDefCtor(fieldClass);
						else if (fieldClass.isAssignableFrom(ArrayList.class)) // AbstractList,AbstractCollection,List,Collection
							fieldCtor = getDefCtor(ArrayList.class);
						else if (fieldClass.isAssignableFrom(HashSet.class)) // AbstractSet,Set
							fieldCtor = getDefCtor(HashSet.class);
						else if (fieldClass.isAssignableFrom(ArrayDeque.class)) // Deque
							fieldCtor = getDefCtor(ArrayDeque.class);
						else if (fieldClass.isAssignableFrom(TreeSet.class)) // NavigableSet
							fieldCtor = getDefCtor(TreeSet.class);
						else if (fieldClass.isAssignableFrom(LinkedList.class)) // AbstractSequentialList
							fieldCtor = getDefCtor(LinkedList.class);
						else if (fieldClass.isAssignableFrom(PriorityQueue.class)) // AbstractQueue
							fieldCtor = getDefCtor(PriorityQueue.class);
						Class<?> subClass = getCollectionSubClass(field.getGenericType());
						if (subClass != null) {
							v = typeMap.get(fieldClass = subClass);
							type = TYPE_LIST_FLAG + (v != null ? v & 0xf : TYPE_CUSTOM);
						} else
							type = TYPE_LIST_FLAG + TYPE_OBJECT;
					} else if (Map.class.isAssignableFrom(fieldClass)) { // Map<?,?>
						if (!isAbstract(fieldClass))
							fieldCtor = getDefCtor(fieldClass);
						else if (fieldClass.isAssignableFrom(HashMap.class)) // AbstractMap,Map
							fieldCtor = getDefCtor(HashMap.class);
						else if (fieldClass.isAssignableFrom(TreeMap.class)) // NavigableMap,SortedMap
							fieldCtor = getDefCtor(TreeMap.class);
						Type[] subTypes = getMapSubClasses(field.getGenericType());
						if (subTypes != null) {
							v = typeMap.get(fieldClass = ensureNotNull((Class<?>)subTypes[1]));
							type = TYPE_MAP_FLAG + (v != null ? v & 0xf : TYPE_CUSTOM);
							keyReader = keyReaderMap.get(subTypes[0]);
							if (keyReader == null) {
								Class<?> keyClass = (Class<?>)subTypes[0];
								if (isAbstract(keyClass)) {
									throw new IllegalStateException("unsupported abstract key class for field: "
											+ fieldName + " in " + klass.getName());
								}
								Creator<?> keyCtor = getDefCtor(keyClass);
								keyReader = (jr, b) -> {
									String keyStr = JsonReader.parseStringKey(jr, b);
									return ensureNotNull(new JsonReader().buf(keyStr).parse(json, keyCtor.create()));
								};
							}
						} else {
							type = TYPE_MAP_FLAG + TYPE_OBJECT;
							keyReader = JsonReader::parseStringKey;
						}
					} else
						type = TYPE_CUSTOM;
					long offset = objectFieldOffset(field);
					if (offset != (int)offset) {
						throw new IllegalStateException("unexpected offset(" + offset + ") from field: "
								+ fieldName + " in " + klass.getName());
					}
					final BiFunction<Class<?>, Field, String> fieldNameFilter = json.fieldNameFilter;
					final String fn = fieldNameFilter != null ? fieldNameFilter.apply(c, field) : fieldName;
					put(j++, new FieldMeta(type, (int)offset, fn != null ? fn : fieldName, fieldClass, fieldCtor,
							keyReader, field));
				}
			}
		}

		public static int getType(Class<?> klass) {
			Integer type = typeMap.get(klass);
			return type != null ? type : TYPE_CUSTOM;
		}

		public @NotNull Creator<T> getCtor() {
			return ctor;
		}

		public @Nullable Parser<T> getParser() {
			return parser;
		}

		public void setParser(@Nullable Parser<T> p) {
			parser = p;
		}

		@SuppressWarnings("unchecked")
		@Deprecated // unsafe
		public void setParserUnsafe(@Nullable Parser<?> p) { // DANGEROUS! only for special purpose
			parser = (Parser<T>)p;
		}

		public @Nullable Writer<T> getWriter() {
			return writer;
		}

		public void setWriter(@Nullable Writer<T> w) {
			writer = w;
		}

		@Nullable FieldMeta get(int hash) {
			for (FieldMeta fm = valueTable[hash & (valueTable.length - 1)]; fm != null; fm = fm.next)
				if (fm.hash == hash)
					return fm;
			return null;
		}

		void put(int idx, @NotNull FieldMeta fieldMeta) {
			fieldMeta.next = null;
			fieldMetas[idx] = fieldMeta;
			int hash = fieldMeta.hash;
			int i = hash & (valueTable.length - 1);
			FieldMeta fm = valueTable[i];
			if (fm == null) { // fast path
				valueTable[i] = fieldMeta;
				return;
			}
			for (; ; ) {
				if (fm.hash == hash) { // bad luck! try to call setKeyHashMultiplier with another prime number
					throw new IllegalStateException("conflicted field names: " + fieldMeta.getName() + " & "
							+ fm.getName() + " in " + fieldMeta.klass.getName());
				}
				FieldMeta next = fm.next;
				if (next == null) {
					fm.next = fieldMeta;
					return;
				}
				fm = next;
			}
		}
	}

	static final @NotNull Unsafe unsafe;
	private static final @NotNull MethodHandle getDeclaredFields0MH;
	static final @NotNull MethodHandle stringCtorMH;
	private static final long OVERRIDE_OFFSET;
	static final long STRING_VALUE_OFFSET, STRING_CODE_OFFSET;
	static final boolean BYTE_STRING;
	static final int keyHashMultiplier = 0x100_0193; // 1677_7619 can be changed to another prime number
	public static final int javaVersion;
	public static final Json instance = new Json();

	private final @NotNull ConcurrentHashMap<Class<?>, ClassMeta<?>> classMetas = new ConcurrentHashMap<>();
	public BiFunction<Class<?>, Field, String> fieldNameFilter;

	@SuppressWarnings("MethodDoesntCallSuperMethod")
	@Override
	public @NotNull Json clone() {
		Json json = new Json();
		json.classMetas.putAll(classMetas);
		json.fieldNameFilter = fieldNameFilter;
		return json;
	}

	static {
		try {
			Field theUnsafeField = Unsafe.class.getDeclaredField("theUnsafe");
			theUnsafeField.setAccessible(true);
			unsafe = ensureNotNull((Unsafe)theUnsafeField.get(null));
			for (long i = 8; ; i++) {
				if (unsafe.getBoolean(theUnsafeField, i)) {
					theUnsafeField.setAccessible(false);
					if (!unsafe.getBoolean(theUnsafeField, i)) {
						OVERRIDE_OFFSET = i;
						break;
					}
					theUnsafeField.setAccessible(true);
				}
				if (i == 32) // should be enough
					throw new UnsupportedOperationException(System.getProperty("java.version"));
			}
			MethodHandles.Lookup lookup = MethodHandles.lookup();
			getDeclaredFields0MH = ensureNotNull(lookup.unreflect(setAccessible(
					Class.class.getDeclaredMethod("getDeclaredFields0", boolean.class))));
			Field valueField = getDeclaredField(String.class, "value");
			STRING_VALUE_OFFSET = objectFieldOffset(Objects.requireNonNull(valueField));
			BYTE_STRING = valueField.getType() == byte[].class;
			STRING_CODE_OFFSET = BYTE_STRING ?
					objectFieldOffset(Objects.requireNonNull(getDeclaredField(String.class, "coder"))) : 0;
			//noinspection JavaReflectionMemberAccess
			stringCtorMH = ensureNotNull(lookup.unreflectConstructor(setAccessible(BYTE_STRING
					? String.class.getDeclaredConstructor(byte[].class, byte.class)
					: String.class.getDeclaredConstructor(char[].class, boolean.class))));
			javaVersion = (int)Float.parseFloat(System.getProperty("java.specification.version"));
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	public static @NotNull Unsafe getUnsafe() {
		return unsafe;
	}

	@SuppressWarnings("deprecation")
	public static long objectFieldOffset(Field field) {
		return unsafe.objectFieldOffset(field);
	}

	static Field[] getDeclaredFields(Class<?> klass) {
		try {
			return (Field[])getDeclaredFields0MH.invokeExact(klass, false);
		} catch (RuntimeException | Error e) {
			throw e;
		} catch (Throwable e) { // MethodHandle.invoke
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("SameParameterValue")
	static @Nullable Field getDeclaredField(Class<?> klass, String fieldName) {
		for (Field field : getDeclaredFields(klass))
			if (field.getName().equals(fieldName))
				return field;
		return null;
	}

	public static <T extends AccessibleObject> @NotNull T setAccessible(@NotNull T ao) {
		unsafe.putBoolean(ao, OVERRIDE_OFFSET, true);
		return ao;
	}

	public static <T> @NotNull T ensureNotNull(@Nullable T obj) {
		assert obj != null;
		return obj;
	}

//	public static void setKeyHashMultiplier(int multiplier) { // must be set before any other access
//		keyHashMultiplier = multiplier;
//	}

	public static int getKeyHash(byte[] buf, int pos, int end) {
		if (pos >= end)
			return 0;
		//noinspection UnnecessaryLocalVariable
		int h = buf[pos], m = keyHashMultiplier;
		while (++pos < end)
			h = h * m + buf[pos];
		return h;
	}

	static @NotNull String newByteString(byte[] buf, int pos, int end) {
		if (!BYTE_STRING) // for JDK8-
			return new String(buf, pos, end - pos, StandardCharsets.ISO_8859_1);
		try {
			return (String)stringCtorMH.invokeExact(Arrays.copyOfRange(buf, pos, end), (byte)0); // for JDK9+
		} catch (Throwable e) { // MethodHandle.invoke
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public <T> @NotNull ClassMeta<T> getClassMeta(@NotNull Class<T> klass) {
		ClassMeta<?> cm = classMetas.get(klass);
		if (cm == null)
			cm = classMetas.computeIfAbsent(klass, c -> new ClassMeta<>(this, c));
		return (ClassMeta<T>)cm;
	}

	public void clearClassMetas() {
		classMetas.clear();
	}

	public static <T> @Nullable T parse(@NotNull String jsonStr, @Nullable Class<T> klass) {
		JsonReader jr = JsonReader.local();
		try {
			return jr.buf(jsonStr).parse(klass);
		} catch (ReflectiveOperationException e) {
			throw Task.forceThrow(e);
		} finally {
			jr.reset();
		}
	}

	public static <T> @Nullable T parse(byte @NotNull [] jsonStr, @Nullable Class<T> klass) {
		JsonReader jr = JsonReader.local();
		try {
			return jr.buf(jsonStr).parse(klass);
		} catch (ReflectiveOperationException e) {
			throw Task.forceThrow(e);
		} finally {
			jr.reset();
		}
	}

	public static <T> @Nullable T parse(byte @NotNull [] jsonStr, int pos, @Nullable Class<T> klass) {
		JsonReader jr = JsonReader.local();
		try {
			return jr.buf(jsonStr, pos).parse(klass);
		} catch (ReflectiveOperationException e) {
			throw Task.forceThrow(e);
		} finally {
			jr.reset();
		}
	}

	public static <T> @Nullable T parse(@NotNull String jsonStr, @Nullable T obj) {
		JsonReader jr = JsonReader.local();
		try {
			return jr.buf(jsonStr).parse(obj);
		} catch (ReflectiveOperationException e) {
			throw Task.forceThrow(e);
		} finally {
			jr.reset();
		}
	}

	public static <T> @Nullable T parse(byte @NotNull [] jsonStr, @Nullable T obj) {
		JsonReader jr = JsonReader.local();
		try {
			return jr.buf(jsonStr).parse(obj);
		} catch (ReflectiveOperationException e) {
			throw Task.forceThrow(e);
		} finally {
			jr.reset();
		}
	}

	public static <T> @Nullable T parse(byte @NotNull [] jsonStr, int pos, @Nullable T obj) {
		JsonReader jr = JsonReader.local();
		try {
			return jr.buf(jsonStr, pos).parse(obj);
		} catch (ReflectiveOperationException e) {
			throw Task.forceThrow(e);
		} finally {
			jr.reset();
		}
	}

	public static @NotNull String toCompactString(@Nullable Object obj) {
		JsonWriter jw = JsonWriter.local();
		try {
			return jw.clear().setFlagsAndDepthLimit(0, 16).write(obj).toString();
		} finally {
			jw.clear();
		}
	}

	public static byte @NotNull [] toCompactBytes(@Nullable Object obj) {
		JsonWriter jw = JsonWriter.local();
		try {
			return jw.clear().setFlagsAndDepthLimit(0, 16).write(obj).toBytes();
		} finally {
			jw.clear();
		}
	}

	static {
		Json json = instance;

		json.fieldNameFilter = (klass, field) -> {
			final String fn = field.getName();
			if (fn.charAt(0) == '_' &&
					(Data.class.isAssignableFrom(klass) // data
							|| Bean.class.isAssignableFrom(klass) // bean
							|| BeanKey.class.isAssignableFrom(klass) // beankey
							|| Zeze.Raft.RocksRaft.Bean.class.isAssignableFrom(klass))) // RocksRaft bean
				return fn.substring(1); // 特殊规则: 忽略字段前的下划线前缀
			return fn;
		};

		json.getClassMeta(ByteBuffer.class).setParser((reader, classMeta, fieldMeta, obj, parent) -> {
			final byte[] data = reader.parseByteString();
			if (obj == null)
				return ByteBuffer.Wrap(data != null ? data : ByteBuffer.Empty);
			obj.wraps(data != null ? data : ByteBuffer.Empty);
			return obj;
		});
		json.getClassMeta(ByteBuffer.class).setWriter((writer, classMeta, obj) -> {
			if (obj == null)
				writer.write(json, null);
			else {
				// String s = obj.toString();
				// writer.ensure(s.length() + 3);
				// writer.write(s, false);
				writer.ensure(obj.size() * 6 + 3);
				writer.write(obj.Bytes, obj.ReadIndex, obj.size(), false);
			}
		});

		json.getClassMeta(Binary.class).setParser((reader, classMeta, fieldMeta, obj, parent) -> {
			byte[] s = reader.parseByteString();
			return s != null ? new Binary(s) : Binary.Empty;
		});
		json.getClassMeta(Binary.class).setWriter((writer, classMeta, obj) -> {
			if (obj == null)
				writer.write(json, null);
			else {
				// String s = obj.toString();
				// writer.ensure(s.length() + 3);
				// writer.write(s, false);
				writer.ensure(obj.size() * 6 + 3);
				writer.write(obj.bytesUnsafe(), obj.getOffset(), obj.size(), false);
			}
		});

		json.getClassMeta(byte[].class).setParser((reader, classMeta, fieldMeta, obj, parent) -> reader.parseByteString());
		json.getClassMeta(byte[].class).setWriter((writer, classMeta, obj) -> {
			if (obj == null)
				writer.write(json, null);
			else {
				writer.ensure(obj.length * 6 + 3);
				writer.write(obj, false);
			}
		});

		json.getClassMeta(DynamicBean.class).setParser((reader, classMeta, fieldMeta, obj, parent) -> {
			if (obj == null) {
				if (parent instanceof PList2)
					obj = (DynamicBean)((PList2<?>)parent).createValue();
				else if (parent instanceof PMap2)
					obj = (DynamicBean)((PMap2<?, ?>)parent).createValue();
				if (obj == null)
					return null;
			}
			obj.reset();
			int p = reader.pos();
			reader.parse0(obj, classMeta);
			Bean bean = obj.getCreateBean().apply(obj.getTypeId());
			obj.setBean(bean != null ? bean : new EmptyBean());
			reader.pos(p).parse0(obj, classMeta);
			return obj;
		});

		json.getClassMeta(CollOne.class).setParser((reader, classMeta, fieldMeta, obj, parent) -> {
			if (obj == null)
				throw new UnsupportedOperationException();
			reader.parse(json, obj.getValue());
			return obj;
		});
		json.getClassMeta(CollOne.class).setWriter((writer, classMeta, obj) -> {
			if (obj == null)
				throw new UnsupportedOperationException();
			writer.write(json, obj.getValue());
		});

		json.getClassMeta(IntList.class).setParser((reader, classMeta, fieldMeta, obj, parent) -> {
			if (reader.next() != '[')
				return obj;
			if (obj == null)
				obj = new IntList();
			else
				obj.clear();
			for (int b = reader.skipNext(); b != ']'; b = reader.skipVar(']'))
				obj.add(reader.parseInt());
			reader.skip(1);
			return obj;
		});
		json.getClassMeta(IntList.class).setWriter((writer, classMeta, obj) -> {
			if (obj == null)
				writer.write(json, null);
			else {
				boolean comma = false;
				writer.ensure(3);
				writer.writeByteUnsafe((byte)'[');
				if ((writer.getFlags() & JsonWriter.FLAG_PRETTY_FORMAT_AND_WRAP_ELEMENT)
						== JsonWriter.FLAG_PRETTY_FORMAT_AND_WRAP_ELEMENT) {
					writer.incTab();
					for (int i = 0, n = obj.size(); i < n; i++) {
						if (comma)
							writer.writeByteUnsafe((byte)',');
						writer.writeNewLineTabs();
						writer.ensure(13);
						writer.write(obj.get(i));
						comma = true;
					}
					writer.decTab();
					if (comma)
						writer.writeNewLineTabs();
				} else {
					for (int i = 0, n = obj.size(); i < n; i++) {
						if (comma)
							writer.writeByteUnsafe((byte)',');
						writer.ensure(13);
						writer.write(obj.get(i));
						comma = true;
					}
				}
				writer.writeByteUnsafe((byte)']');
			}
		});

		json.getClassMeta(LongList.class).setParser((reader, classMeta, fieldMeta, obj, parent) -> {
			if (reader.next() != '[')
				return obj;
			if (obj == null)
				obj = new LongList();
			else
				obj.clear();
			for (int b = reader.skipNext(); b != ']'; b = reader.skipVar(']'))
				obj.add(reader.parseLong());
			reader.skip(1);
			return obj;
		});
		json.getClassMeta(LongList.class).setWriter((writer, classMeta, obj) -> {
			if (obj == null)
				writer.write(json, null);
			else {
				boolean comma = false;
				writer.ensure(3);
				writer.writeByteUnsafe((byte)'[');
				if ((writer.getFlags() & JsonWriter.FLAG_PRETTY_FORMAT_AND_WRAP_ELEMENT)
						== JsonWriter.FLAG_PRETTY_FORMAT_AND_WRAP_ELEMENT) {
					writer.incTab();
					for (int i = 0, n = obj.size(); i < n; i++) {
						if (comma)
							writer.writeByteUnsafe((byte)',');
						writer.writeNewLineTabs();
						writer.ensure(22);
						writer.write(obj.get(i));
						comma = true;
					}
					writer.decTab();
					if (comma)
						writer.writeNewLineTabs();
				} else {
					for (int i = 0, n = obj.size(); i < n; i++) {
						if (comma)
							writer.writeByteUnsafe((byte)',');
						writer.ensure(22);
						writer.write(obj.get(i));
						comma = true;
					}
				}
				writer.writeByteUnsafe((byte)']');
			}
		});

		json.getClassMeta(FloatList.class).setParser((reader, classMeta, fieldMeta, obj, parent) -> {
			if (reader.next() != '[')
				return obj;
			if (obj == null)
				obj = new FloatList();
			else
				obj.clear();
			for (int b = reader.skipNext(); b != ']'; b = reader.skipVar(']'))
				obj.add((float)reader.parseDouble());
			reader.skip(1);
			return obj;
		});
		json.getClassMeta(FloatList.class).setWriter((writer, classMeta, obj) -> {
			if (obj == null)
				writer.write(json, null);
			else {
				boolean comma = false;
				writer.ensure(3);
				writer.writeByteUnsafe((byte)'[');
				if ((writer.getFlags() & JsonWriter.FLAG_PRETTY_FORMAT_AND_WRAP_ELEMENT)
						== JsonWriter.FLAG_PRETTY_FORMAT_AND_WRAP_ELEMENT) {
					writer.incTab();
					for (int i = 0, n = obj.size(); i < n; i++) {
						if (comma)
							writer.writeByteUnsafe((byte)',');
						writer.writeNewLineTabs();
						writer.ensure(27);
						writer.write(obj.get(i));
						comma = true;
					}
					writer.decTab();
					if (comma)
						writer.writeNewLineTabs();
				} else {
					for (int i = 0, n = obj.size(); i < n; i++) {
						if (comma)
							writer.writeByteUnsafe((byte)',');
						writer.ensure(27);
						writer.write(obj.get(i));
						comma = true;
					}
				}
				writer.writeByteUnsafe((byte)']');
			}
		});

		json.getClassMeta(Vector3List.class).setParser((reader, classMeta, fieldMeta, obj, parent) -> {
			if (reader.next() != '[')
				return obj;
			if (obj == null)
				obj = new Vector3List();
			else
				obj.clear();
			for (int b = reader.skipNext(); b != ']'; b = reader.skipVar(']'))
				obj.add((float)reader.parseDouble());
			reader.skip(1);
			return obj;
		});
		json.getClassMeta(Vector3List.class).setWriter((writer, classMeta, obj) -> {
			if (obj == null)
				writer.write(json, null);
			else {
				boolean comma = false;
				writer.ensure(3);
				writer.writeByteUnsafe((byte)'[');
				if ((writer.getFlags() & JsonWriter.FLAG_PRETTY_FORMAT_AND_WRAP_ELEMENT)
						== JsonWriter.FLAG_PRETTY_FORMAT_AND_WRAP_ELEMENT) {
					writer.incTab();
					for (int i = 0, n = obj.size(); i < n; i++) {
						if (comma)
							writer.writeByteUnsafe((byte)',');
						writer.writeNewLineTabs();
						writer.ensure(27);
						writer.write(obj.get(i));
						comma = true;
					}
					writer.decTab();
					if (comma)
						writer.writeNewLineTabs();
				} else {
					for (int i = 0, n = obj.size(); i < n; i++) {
						if (comma)
							writer.writeByteUnsafe((byte)',');
						writer.ensure(27);
						writer.write(obj.get(i));
						comma = true;
					}
				}
				writer.writeByteUnsafe((byte)']');
			}
		});

		json.getClassMeta(Vector3IntList.class).setParser((reader, classMeta, fieldMeta, obj, parent) -> {
			if (reader.next() != '[')
				return obj;
			if (obj == null)
				obj = new Vector3IntList();
			else
				obj.clear();
			for (int b = reader.skipNext(); b != ']'; b = reader.skipVar(']'))
				obj.add(reader.parseInt());
			reader.skip(1);
			return obj;
		});
		json.getClassMeta(Vector3IntList.class).setWriter((writer, classMeta, obj) -> {
			if (obj == null)
				writer.write(json, null);
			else {
				boolean comma = false;
				writer.ensure(3);
				writer.writeByteUnsafe((byte)'[');
				if ((writer.getFlags() & JsonWriter.FLAG_PRETTY_FORMAT_AND_WRAP_ELEMENT)
						== JsonWriter.FLAG_PRETTY_FORMAT_AND_WRAP_ELEMENT) {
					writer.incTab();
					for (int i = 0, n = obj.size(); i < n; i++) {
						if (comma)
							writer.writeByteUnsafe((byte)',');
						writer.writeNewLineTabs();
						writer.ensure(13);
						writer.write(obj.get(i));
						comma = true;
					}
					writer.decTab();
					if (comma)
						writer.writeNewLineTabs();
				} else {
					for (int i = 0, n = obj.size(); i < n; i++) {
						if (comma)
							writer.writeByteUnsafe((byte)',');
						writer.ensure(13);
						writer.write(obj.get(i));
						comma = true;
					}
				}
				writer.writeByteUnsafe((byte)']');
			}
		});

		json.getClassMeta(IntHashSet.class).setParser((reader, classMeta, fieldMeta, obj, parent) -> {
			if (reader.next() != '[')
				return obj;
			if (obj == null)
				obj = new IntHashSet();
			else
				obj.clear();
			for (int b = reader.skipNext(); b != ']'; b = reader.skipVar(']'))
				obj.add(reader.parseInt());
			reader.skip(1);
			return obj;
		});
		json.getClassMeta(IntHashSet.class).setWriter((writer, classMeta, obj) -> {
			if (obj == null)
				writer.write(json, null);
			else {
				boolean comma = false;
				writer.ensure(3);
				writer.writeByteUnsafe((byte)'[');
				if ((writer.getFlags() & JsonWriter.FLAG_PRETTY_FORMAT_AND_WRAP_ELEMENT)
						== JsonWriter.FLAG_PRETTY_FORMAT_AND_WRAP_ELEMENT) {
					writer.incTab();
					for (IntHashSet.Iterator it = obj.iterator(); it.moveToNext(); ) {
						if (comma)
							writer.writeByteUnsafe((byte)',');
						writer.writeNewLineTabs();
						writer.ensure(13);
						writer.write(it.value());
						comma = true;
					}
					writer.decTab();
					if (comma)
						writer.writeNewLineTabs();
				} else {
					for (IntHashSet.Iterator it = obj.iterator(); it.moveToNext(); ) {
						if (comma)
							writer.writeByteUnsafe((byte)',');
						writer.ensure(13);
						writer.write(it.value());
						comma = true;
					}
				}
				writer.writeByteUnsafe((byte)']');
			}
		});

		json.getClassMeta(LongHashSet.class).setParser((reader, classMeta, fieldMeta, obj, parent) -> {
			if (reader.next() != '[')
				return obj;
			if (obj == null)
				obj = new LongHashSet();
			else
				obj.clear();
			for (int b = reader.skipNext(); b != ']'; b = reader.skipVar(']'))
				obj.add(reader.parseLong());
			reader.skip(1);
			return obj;
		});
		json.getClassMeta(LongHashSet.class).setWriter((writer, classMeta, obj) -> {
			if (obj == null)
				writer.write(json, null);
			else {
				boolean comma = false;
				writer.ensure(3);
				writer.writeByteUnsafe((byte)'[');
				if ((writer.getFlags() & JsonWriter.FLAG_PRETTY_FORMAT_AND_WRAP_ELEMENT)
						== JsonWriter.FLAG_PRETTY_FORMAT_AND_WRAP_ELEMENT) {
					writer.incTab();
					for (LongHashSet.Iterator it = obj.iterator(); it.moveToNext(); ) {
						if (comma)
							writer.writeByteUnsafe((byte)',');
						writer.writeNewLineTabs();
						writer.ensure(22);
						writer.write(it.value());
						comma = true;
					}
					writer.decTab();
					if (comma)
						writer.writeNewLineTabs();
				} else {
					for (LongHashSet.Iterator it = obj.iterator(); it.moveToNext(); ) {
						if (comma)
							writer.writeByteUnsafe((byte)',');
						writer.ensure(22);
						writer.write(it.value());
						comma = true;
					}
				}
				writer.writeByteUnsafe((byte)']');
			}
		});

		json.getClassMeta(IntHashMap.class).setParser((reader, classMeta, fieldMeta, obj, parent) -> {
			if (reader.next() != '{')
				return obj;
			if (obj == null)
				obj = new IntHashMap<>();
			else
				obj.clear();
			Class<?> valueClass = fieldMeta != null ? ((Class<?>)fieldMeta.paramTypes[0]) : null;
			ClassMeta<?> valueMeta = valueClass != null ? instance.getClassMeta(valueClass) : null;
			for (int b = reader.skipNext(); b != '}'; b = reader.skipVar('}')) {
				int k = JsonReader.parseIntegerKey(reader, b);
				reader.skipColon();
				@SuppressWarnings({"unchecked", "unused"})
				Object __ = obj.put(k, reader.parse(valueMeta));
			}
			reader.skip(1);
			return obj;
		});
		json.getClassMeta(IntHashMap.class).setWriter((writer, classMeta, obj) -> {
			if (obj == null)
				writer.write(json, null);
			else {
				boolean comma = false;
				boolean noQuote = writer.isNoQuoteKey();
				writer.ensure(1);
				writer.writeByteUnsafe((byte)'{');
				if (writer.isPrettyFormat()) {
					for (IntHashMap<?>.Iterator it = obj.iterator(); it.moveToNext(); ) {
						Object v = it.value();
						if (v == null && !writer.isWriteNull())
							continue;
						if (comma)
							writer.writeByteUnsafe((byte)',');
						else {
							writer.incTab();
							comma = true;
						}
						writer.writeNewLineTabs();
						int k = it.key();
						if (noQuote) {
							writer.ensure(13);
							writer.write(k);
						} else {
							writer.ensure(15);
							writer.writeByteUnsafe((byte)'"');
							writer.write(k);
							writer.writeByteUnsafe((byte)'"');
						}
						writer.writeByteUnsafe((byte)':');
						writer.writeByteUnsafe((byte)' ');
						writer.write(v);
					}
					if (comma) {
						writer.decTab();
						writer.writeNewLineTabs();
					} else
						writer.ensure(2);
				} else {
					for (IntHashMap<?>.Iterator it = obj.iterator(); it.moveToNext(); ) {
						Object v = it.value();
						if (v == null && !writer.isWriteNull())
							continue;
						if (comma)
							writer.writeByteUnsafe((byte)',');
						int k = it.key();
						if (noQuote) {
							writer.ensure(12);
							writer.write(k);
						} else {
							writer.ensure(14);
							writer.writeByteUnsafe((byte)'"');
							writer.write(k);
							writer.writeByteUnsafe((byte)'"');
						}
						writer.writeByteUnsafe((byte)':');
						writer.write(v);
						comma = true;
					}
					writer.ensure(2);
				}
				writer.writeByteUnsafe((byte)'}');
			}
		});

		json.getClassMeta(LongHashMap.class).setParser((reader, classMeta, fieldMeta, obj, parent) -> {
			if (reader.next() != '{')
				return obj;
			if (obj == null)
				obj = new LongHashMap<>();
			else
				obj.clear();
			Class<?> valueClass = fieldMeta != null ? ((Class<?>)fieldMeta.paramTypes[0]) : null;
			ClassMeta<?> valueMeta = valueClass != null ? instance.getClassMeta(valueClass) : null;
			for (int b = reader.skipNext(); b != '}'; b = reader.skipVar('}')) {
				long k = JsonReader.parseLongKey(reader, b);
				reader.skipColon();
				@SuppressWarnings({"unchecked", "unused"})
				Object __ = obj.put(k, reader.parse(valueMeta));
			}
			reader.skip(1);
			return obj;
		});
		json.getClassMeta(LongHashMap.class).setWriter((writer, classMeta, obj) -> {
			if (obj == null)
				writer.write(json, null);
			else {
				boolean comma = false;
				boolean noQuote = writer.isNoQuoteKey();
				writer.ensure(1);
				writer.writeByteUnsafe((byte)'{');
				if (writer.isPrettyFormat()) {
					for (LongHashMap<?>.Iterator it = obj.iterator(); it.moveToNext(); ) {
						Object v = it.value();
						if (v == null && !writer.isWriteNull())
							continue;
						if (comma)
							writer.writeByteUnsafe((byte)',');
						else {
							writer.incTab();
							comma = true;
						}
						writer.writeNewLineTabs();
						long k = it.key();
						if (noQuote) {
							writer.ensure(13);
							writer.write(k);
						} else {
							writer.ensure(15);
							writer.writeByteUnsafe((byte)'"');
							writer.write(k);
							writer.writeByteUnsafe((byte)'"');
						}
						writer.writeByteUnsafe((byte)':');
						writer.writeByteUnsafe((byte)' ');
						writer.write(v);
					}
					if (comma) {
						writer.decTab();
						writer.writeNewLineTabs();
					} else
						writer.ensure(2);
				} else {
					for (LongHashMap<?>.Iterator it = obj.iterator(); it.moveToNext(); ) {
						Object v = it.value();
						if (v == null && !writer.isWriteNull())
							continue;
						if (comma)
							writer.writeByteUnsafe((byte)',');
						long k = it.key();
						if (noQuote) {
							writer.ensure(12);
							writer.write(k);
						} else {
							writer.ensure(14);
							writer.writeByteUnsafe((byte)'"');
							writer.write(k);
							writer.writeByteUnsafe((byte)'"');
						}
						writer.writeByteUnsafe((byte)':');
						writer.write(v);
						comma = true;
					}
					writer.ensure(2);
				}
				writer.writeByteUnsafe((byte)'}');
			}
		});

		json.getClassMeta(LongConcurrentHashMap.class).setParser((reader, classMeta, fieldMeta, obj, parent) -> {
			if (reader.next() != '{')
				return obj;
			if (obj == null)
				obj = new LongConcurrentHashMap<>();
			else
				obj.clear();
			Class<?> valueClass = fieldMeta != null ? ((Class<?>)fieldMeta.paramTypes[0]) : null;
			ClassMeta<?> valueMeta = valueClass != null ? instance.getClassMeta(valueClass) : null;
			for (int b = reader.skipNext(); b != '}'; b = reader.skipVar('}')) {
				long k = JsonReader.parseLongKey(reader, b);
				reader.skipColon();
				@SuppressWarnings({"unchecked", "unused"})
				Object __ = obj.put(k, reader.parse(valueMeta));
			}
			reader.skip(1);
			return obj;
		});
		json.getClassMeta(LongConcurrentHashMap.class).setWriter((writer, classMeta, obj) -> {
			if (obj == null)
				writer.write(json, null);
			else {
				boolean comma = false;
				boolean noQuote = writer.isNoQuoteKey();
				writer.ensure(1);
				writer.writeByteUnsafe((byte)'{');
				if (writer.isPrettyFormat()) {
					for (LongMap.MapIterator<?> it = obj.entryIterator(); it.moveToNext(); ) {
						Object v = it.value();
						if (v == null && !writer.isWriteNull())
							continue;
						if (comma)
							writer.writeByteUnsafe((byte)',');
						else {
							writer.incTab();
							comma = true;
						}
						writer.writeNewLineTabs();
						long k = it.key();
						if (noQuote) {
							writer.ensure(13);
							writer.write(k);
						} else {
							writer.ensure(15);
							writer.writeByteUnsafe((byte)'"');
							writer.write(k);
							writer.writeByteUnsafe((byte)'"');
						}
						writer.writeByteUnsafe((byte)':');
						writer.writeByteUnsafe((byte)' ');
						writer.write(v);
					}
					if (comma) {
						writer.decTab();
						writer.writeNewLineTabs();
					} else
						writer.ensure(2);
				} else {
					for (LongMap.MapIterator<?> it = obj.entryIterator(); it.moveToNext(); ) {
						Object v = it.value();
						if (v == null && !writer.isWriteNull())
							continue;
						if (comma)
							writer.writeByteUnsafe((byte)',');
						long k = it.key();
						if (noQuote) {
							writer.ensure(12);
							writer.write(k);
						} else {
							writer.ensure(14);
							writer.writeByteUnsafe((byte)'"');
							writer.write(k);
							writer.writeByteUnsafe((byte)'"');
						}
						writer.writeByteUnsafe((byte)':');
						writer.write(v);
						comma = true;
					}
					writer.ensure(2);
				}
				writer.writeByteUnsafe((byte)'}');
			}
		});
	}
}
