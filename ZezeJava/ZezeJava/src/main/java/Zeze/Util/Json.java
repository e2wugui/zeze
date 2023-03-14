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
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Collections.CollOne;
import Zeze.Transaction.Collections.PList2;
import Zeze.Transaction.Collections.PMap2;
import Zeze.Transaction.DynamicBean;
import Zeze.Transaction.EmptyBean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sun.misc.Unsafe;

// Compile with JDK11+; Run with JDK8+ (JDK9+ is recommended); Android is NOT supported
public final class Json {
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

	interface KeyReader {
		@NotNull
		Object parse(@NotNull JsonReader jr, int b) throws ReflectiveOperationException;
	}

	interface Creator<T> {
		T create() throws ReflectiveOperationException;
	}

	static final class FieldMeta {
		final int hash; // for FieldMetaMap
		final int type; // defined above
		final int offset; // for unsafe access
		final @NotNull Class<?> klass; // TYPE_CUSTOM:fieldClass; TYPE_LIST_FLAG/TYPE_MAP_FLAG:subValueClass
		transient @Nullable ClassMeta<?> classMeta; // from klass, lazy assigned
		transient @Nullable FieldMeta next; // for FieldMetaMap
		final byte[] name; // field name
		final @Nullable Creator<?> ctor; // for TYPE_LIST_FLAG/TYPE_MAP_FLAG
		final @Nullable KeyReader keyParser; // for TYPE_MAP_FLAG

		FieldMeta(int type, int offset, @NotNull String name, @NotNull Class<?> klass, @Nullable Creator<?> ctor,
				  @Nullable KeyReader keyReader) {
			this.name = name.getBytes(StandardCharsets.UTF_8);
			this.hash = getKeyHash(this.name, 0, this.name.length);
			this.type = type;
			this.offset = offset;
			this.klass = klass;
			this.ctor = ctor;
			this.keyParser = keyReader;
		}

		@NotNull
		String getName() {
			return new String(name, StandardCharsets.UTF_8);
		}
	}

	public interface Parser<T> {
		@Nullable
		T parse(@NotNull JsonReader reader, @NotNull ClassMeta<T> classMeta, @Nullable T obj, @Nullable Object parent)
				throws ReflectiveOperationException;

		@SuppressWarnings("unchecked")
		default @Nullable T parse0(@NotNull JsonReader reader, @NotNull ClassMeta<?> classMeta, @Nullable Object obj,
								   @Nullable Object parent) throws ReflectiveOperationException {
			return parse(reader, (ClassMeta<T>)classMeta, (T)obj, parent);
		}
	}

	public interface Writer<T> {
		void write(@NotNull JsonWriter writer, @NotNull ClassMeta<T> classMeta, @Nullable T obj);

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

		static boolean isAbstract(@NotNull Class<?> klass) {
			return (klass.getModifiers() & (Modifier.INTERFACE | Modifier.ABSTRACT)) != 0;
		}

		@SuppressWarnings("unchecked")
		private static <T> @NotNull Creator<T> getDefCtor(@NotNull Class<T> klass) {
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

		private static Type[] getMapSubClasses(Type geneType) { // X<K,V>, X extends Y<K,V>, X implements Y<K,V>
			if (geneType instanceof ParameterizedType) {
				//noinspection PatternVariableCanBeUsed
				ParameterizedType paraType = (ParameterizedType)geneType;
				if (Map.class.isAssignableFrom((Class<?>)paraType.getRawType())) {
					Type[] subTypes = paraType.getActualTypeArguments();
					if (subTypes.length == 2 && subTypes[0] instanceof Class && subTypes[1] instanceof Class)
						return subTypes;
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
					if ((field.getModifiers() & (Modifier.STATIC | Modifier.TRANSIENT)) == 0)
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
								if (isAbstract(keyClass))
									throw new IllegalStateException("unsupported abstract key class for field: "
											+ fieldName + " in " + klass.getName());
								Creator<?> keyCtor = getDefCtor(keyClass);
								keyReader = (jr, b) -> {
									String keyStr = JsonReader.parseStringKey(jr, b);
									return ensureNotNull(new JsonReader().buf(keyStr).parse(keyCtor.create()));
								};
							}
						} else {
							type = TYPE_MAP_FLAG + TYPE_OBJECT;
							keyReader = JsonReader::parseStringKey;
						}
					} else
						type = TYPE_CUSTOM;
					long offset = unsafe.objectFieldOffset(field);
					if (offset != (int)offset)
						throw new IllegalStateException("unexpected offset(" + offset + ") from field: "
								+ fieldName + " in " + klass.getName());
					final var fieldNameFilter = json.fieldNameFilter;
					final String fn = fieldNameFilter != null ? fieldNameFilter.apply(c, field) : fieldName;
					put(j++, new FieldMeta(type, (int)offset, fn != null ? fn : fieldName, fieldClass, fieldCtor,
							keyReader));
				}
			}
		}

		static int getType(Class<?> klass) {
			Integer type = typeMap.get(klass);
			return type != null ? type : TYPE_CUSTOM;
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

		@Nullable
		FieldMeta get(int hash) {
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
				if (fm.hash == hash) // bad luck! try to call setKeyHashMultiplier with another prime number
					throw new IllegalStateException("conflicted field names: " + fieldMeta.getName() + " & "
							+ fm.getName() + " in " + fieldMeta.klass.getName());
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
	private static final long OVERRIDE_OFFSET;
	static final long STRING_VALUE_OFFSET;
	static final boolean BYTE_STRING;
	static final int keyHashMultiplier = 0x100_0193; // 1677_7619 can be changed to another prime number
	public static final Json instance = new Json();

	private final @NotNull ConcurrentHashMap<Class<?>, ClassMeta<?>> classMetas = new ConcurrentHashMap<>();
	public BiFunction<Class<?>, Field, String> fieldNameFilter;

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
			getDeclaredFields0MH = ensureNotNull(MethodHandles.lookup().unreflect(setAccessible(
					Class.class.getDeclaredMethod("getDeclaredFields0", boolean.class))));
			Field valueField = getDeclaredField(String.class, "value");
			STRING_VALUE_OFFSET = unsafe.objectFieldOffset(Objects.requireNonNull(valueField));
			BYTE_STRING = valueField.getType() == byte[].class;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	public static @NotNull Unsafe getUnsafe() {
		return unsafe;
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

	static @NotNull String newByteString(byte[] buf, int pos, int end) throws ReflectiveOperationException {
		if (!BYTE_STRING) // for JDK8-
			return new String(buf, pos, end - pos, StandardCharsets.ISO_8859_1);
		@SuppressWarnings("null")
		@NotNull
		String str = (String)unsafe.allocateInstance(String.class);
		unsafe.putObject(str, STRING_VALUE_OFFSET, Arrays.copyOfRange(buf, pos, end)); // for JDK9+
		return str;
	}

	@SuppressWarnings("unchecked")
	public <T> @NotNull ClassMeta<T> getClassMeta(@NotNull Class<T> klass) {
		var cm = classMetas.get(klass);
		if (cm == null)
			cm = classMetas.computeIfAbsent(klass, c -> new ClassMeta<>(this, c));
		return (ClassMeta<T>)cm;
	}

	public void clearClassMetas() {
		classMetas.clear();
	}

	static {
		var json = instance;

		json.fieldNameFilter = (klass, field) -> {
			final String fn = field.getName();
			if (fn.charAt(0) == '_' &&
					(Bean.class.isAssignableFrom(klass) // bean
							|| Zeze.Raft.RocksRaft.Bean.class.isAssignableFrom(klass) // RocksRaft bean
							|| (Serializable.class.isAssignableFrom(klass)
							&& Comparable.class.isAssignableFrom(klass)))) // beankey
				return fn.substring(1); // 特殊规则: 忽略字段前的下划线前缀
			return fn;
		};

		json.getClassMeta(ByteBuffer.class).setParser((reader, classMeta, obj, parent) -> {
			final byte[] data = reader.parseByteString();
			if (obj == null)
				return ByteBuffer.Wrap(data);
			obj.wraps(data != null ? data : ByteBuffer.Empty);
			return obj;
		});
		json.getClassMeta(ByteBuffer.class).setWriter((writer, classMeta, obj) -> {
			if (obj == null)
				writer.write(null);
			else {
				var s = obj.toString();
				writer.ensure(s.length() + 2);
				writer.write(s, false);
				// writer.ensure(obj.size() * 6 + 2);
				// writer.write(obj.Bytes, obj.ReadIndex, obj.Size(), false);
			}
		});

		json.getClassMeta(Binary.class).setParser((reader, classMeta, obj, parent) ->
				new Binary(reader.parseByteString()));
		json.getClassMeta(Binary.class).setWriter((writer, classMeta, obj) -> {
			if (obj == null)
				writer.write(null);
			else {
				var s = obj.toString();
				writer.ensure(s.length() + 2);
				writer.write(s, false);
				// writer.ensure(obj.size() * 6 + 2);
				// writer.write(obj.bytesUnsafe(), obj.getOffset(), obj.size(), false);
			}
		});

		json.getClassMeta(DynamicBean.class).setParser((reader, classMeta, obj, parent) -> {
			if (obj == null) {
				if (parent instanceof PList2)
					obj = (DynamicBean)((PList2<?>)parent).createValue();
				else if (parent instanceof PMap2)
					obj = (DynamicBean)((PMap2<?, ?>)parent).createValue();
			}
			if (obj != null) {
				int p = reader.pos();
				reader.parse0(obj, classMeta);
				Bean bean = obj.getCreateBean().apply(obj.getTypeId());
				obj.setBean(bean != null ? bean : new EmptyBean());
				reader.pos(p).parse0(obj, classMeta);
			}
			return obj;
		});

		json.getClassMeta(CollOne.class).setParser((reader, classMeta, obj, parent) -> {
			if (obj == null)
				throw new UnsupportedOperationException();
			reader.parse(obj.getValue());
			return obj;
		});
		json.getClassMeta(CollOne.class).setWriter((writer, classMeta, obj) -> {
			if (obj == null)
				throw new UnsupportedOperationException();
			writer.write(obj.getValue());
		});
	}
}
