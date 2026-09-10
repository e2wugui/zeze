package Zeze.Transaction.Collections;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.SerializeHelper;
import Zeze.Transaction.Bean;
import Zeze.Transaction.DynamicBean;
import Zeze.Util.Reflect;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;

public final class Meta2<K, V> {
	private static final long map1HeadHash = Bean.hash64("Zeze.Transaction.Collections.LogMap1<");
	private static final long map2HeadHash = Bean.hash64("Zeze.Transaction.Collections.LogMap2<");
	private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<Class<?>, Meta2<?, ?>>> map1Metas = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<Class<?>, Meta2<?, ?>>> map2Metas = new ConcurrentHashMap<>();
	private static final long sortedMap1HeadHash = Bean.hash64("Zeze.Transaction.Collections.LogSortedMap1<");
	private static final long sortedMap2HeadHash = Bean.hash64("Zeze.Transaction.Collections.LogSortedMap2<");
	private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<Class<?>, Meta2<?, ?>>> sortedMap1Metas = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<Class<?>, Meta2<?, ?>>> sortedMap2Metas = new ConcurrentHashMap<>();

	public final int logTypeId;
	public final int keyEncodeType;
	public final @NotNull BiConsumer<ByteBuffer, K> keyEncoder;
	public final @NotNull Function<IByteBuffer, K> keyDecoder;
	public final @NotNull SerializeHelper.ObjectIntFunction<IByteBuffer, K> keyDecoderWithType;
	public final int valueEncodeType;
	public final BiConsumer<ByteBuffer, V> valueEncoder; // 只用于非Bean类型
	public final Function<IByteBuffer, V> valueDecoder; // 只用于非Bean类型
	public final SerializeHelper.ObjectIntFunction<IByteBuffer, V> valueDecoderWithType; // 只用于非Bean类型
	public final MethodHandle valueFactory; // 只用于Bean类型
	public final @NotNull String name; // 主要用于分析查错
	// 实例的真实 key/value 类型，供 variables() 等元数据推导（FND3-07）。
	public final @NotNull Class<?> keyClass;
	public final @NotNull Class<?> valueClass;

	private Meta2(@NotNull String headStr, long headHash, @NotNull Class<K> keyClass, @NotNull Class<V> valueClass,
	              MethodHandle valueFactory) {
		logTypeId = Bean.hashLog(headHash, keyClass, valueClass);
		this.keyClass = keyClass;
		this.valueClass = valueClass;
		var keyCodecFuncs = SerializeHelper.createCodec(keyClass);
		keyEncodeType = keyCodecFuncs.encodeType;
		keyEncoder = keyCodecFuncs.encoder;
		keyDecoder = keyCodecFuncs.decoder;
		keyDecoderWithType = keyCodecFuncs.decoderWithType;
		// 非Bean类型没有valueFactory，其codec已预注册，使用无ctor的重载。
		var valueCodecFuncs = valueFactory != null
				? SerializeHelper.createCodec(valueClass, valueFactory)
				: SerializeHelper.createCodec(valueClass);
		valueEncodeType = valueCodecFuncs.encodeType;
		valueEncoder = valueCodecFuncs.encoder;
		valueDecoder = valueCodecFuncs.decoder;
		valueDecoderWithType = valueCodecFuncs.decoderWithType;
		this.valueFactory = valueFactory;
		name = headStr + keyClass.getName() + ',' + valueClass.getName();
	}

	private Meta2(@NotNull String headStr, long headHash, @NotNull Class<K> keyClass, @NotNull Class<V> valueClass,
	              @NotNull Supplier<V> ctor) {
		this(headStr, headHash, keyClass, valueClass, toMethodHandle(ctor));
	}

	private static MethodHandle toMethodHandle(@NotNull Supplier<?> ctor) {
		try {
			return Reflect.lookup.findVirtual(Supplier.class, "get", MethodType.methodType(Object.class)).bindTo(ctor);
		} catch (ReflectiveOperationException e) {
			throw Task.forceThrow(e);
		}
	}

	private Meta2(@NotNull String headStr, long headHash, @NotNull Class<K> keyClass, @NotNull Class<V> valueClass) {
		this(headStr, headHash, keyClass, valueClass,
				Bean.class.isAssignableFrom(valueClass) ? Reflect.getDefaultConstructor(valueClass) : null);
	}

	private Meta2(@NotNull String headStr, long headHash, @NotNull Class<K> keyClass, @NotNull ToLongFunction<Bean> get,
	              @NotNull LongFunction<Bean> create) {
		logTypeId = Bean.hashLog(headHash, keyClass, DynamicBean.class);
		this.keyClass = keyClass;
		this.valueClass = DynamicBean.class;
		var keyCodecFuncs = SerializeHelper.createCodec(keyClass);
		keyEncodeType = keyCodecFuncs.encodeType;
		keyEncoder = keyCodecFuncs.encoder;
		keyDecoder = keyCodecFuncs.decoder;
		keyDecoderWithType = keyCodecFuncs.decoderWithType;
		valueEncodeType = IByteBuffer.DYNAMIC;
		valueEncoder = null;
		valueDecoder = null;
		valueDecoderWithType = null;
		valueFactory = SerializeHelper.createDynamicFactory(get, create);
		name = headStr + keyClass.getName() + ",DynamicBean";
	}

	@SuppressWarnings("unchecked")
	public static <K, V> @NotNull Meta2<K, V> getMap1Meta(@NotNull Class<K> keyClass, @NotNull Class<V> valueClass) {
		var map = map1Metas.computeIfAbsent(keyClass, __ -> new ConcurrentHashMap<>());
		var r = map.get(valueClass);
		if (r != null)
			return (Meta2<K, V>)r;
		return (Meta2<K, V>)map.computeIfAbsent(valueClass,
				vc -> new Meta2<>("LogMap1:", map1HeadHash, keyClass, (Class<V>)vc));
	}

	@SuppressWarnings("unchecked")
	public static <K, V extends Bean> @NotNull Meta2<K, V> getMap2Meta(@NotNull Class<K> keyClass,
	                                                                   @NotNull Class<V> valueClass) {
		var map = map2Metas.computeIfAbsent(keyClass, __ -> new ConcurrentHashMap<>());
		var r = map.get(valueClass);
		if (r != null)
			return (Meta2<K, V>)r;
		return (Meta2<K, V>)map.computeIfAbsent(valueClass,
				vc -> new Meta2<>("LogMap2:", map2HeadHash, keyClass, (Class<V>)vc));
	}

	public static <K, V extends Bean> @NotNull Meta2<K, V> createMap2Meta(@NotNull Class<K> keyClass,
	                                                                      @NotNull Class<V> valueClass,
	                                                                      @NotNull Supplier<V> valueCtor) {
		return new Meta2<>("LogMap2:", map2HeadHash, keyClass, valueClass, valueCtor);
	}

	public static <K, V extends Bean> @NotNull Meta2<K, V> createDynamicMapMeta(@NotNull Class<K> keyClass,
	                                                                            @NotNull ToLongFunction<Bean> get,
	                                                                            @NotNull LongFunction<Bean> create) {
		return new Meta2<>("LogMap2:", map2HeadHash, keyClass, get, create);
	}

	@SuppressWarnings("unchecked")
	public static <K, V> @NotNull Meta2<K, V> getSortedMap1Meta(@NotNull Class<K> keyClass, @NotNull Class<V> valueClass) {
		var map = sortedMap1Metas.computeIfAbsent(keyClass, __ -> new ConcurrentHashMap<>());
		var r = map.get(valueClass);
		if (r != null)
			return (Meta2<K, V>)r;
		return (Meta2<K, V>)map.computeIfAbsent(valueClass,
				vc -> new Meta2<>("LogSortedMap1:", sortedMap1HeadHash, keyClass, (Class<V>)vc));
	}

	@SuppressWarnings("unchecked")
	public static <K, V extends Bean> @NotNull Meta2<K, V> getSortedMap2Meta(@NotNull Class<K> keyClass,
	                                                                         @NotNull Class<V> valueClass) {
		var map = sortedMap2Metas.computeIfAbsent(keyClass, __ -> new ConcurrentHashMap<>());
		var r = map.get(valueClass);
		if (r != null)
			return (Meta2<K, V>)r;
		return (Meta2<K, V>)map.computeIfAbsent(valueClass,
				vc -> new Meta2<>("LogSortedMap2:", sortedMap2HeadHash, keyClass, (Class<V>)vc));
	}

	public static <K, V extends Bean> @NotNull Meta2<K, V> createSortedMap2Meta(@NotNull Class<K> keyClass,
	                                                                            @NotNull Class<V> valueClass,
	                                                                            @NotNull Supplier<V> valueCtor) {
		return new Meta2<>("LogSortedMap2:", sortedMap2HeadHash, keyClass, valueClass, valueCtor);
	}

	public static <K, V extends Bean> @NotNull Meta2<K, V> createDynamicSortedMapMeta(@NotNull Class<K> keyClass,
	                                                                                  @NotNull ToLongFunction<Bean> get,
	                                                                                  @NotNull LongFunction<Bean> create) {
		return new Meta2<>("LogSortedMap2:", sortedMap2HeadHash, keyClass, get, create);
	}

	// Java Class → schema 类型名，与生成器（Gen/Types/Variable.GetTypeFullName）的输出对齐：
	// 内建类型用 schema 关键字，bean/枚举用全名（生成约定 schema 全名 == Java 类名）。
	// 供运行时从 Meta2.keyClass/valueClass 推导 variables() 等类型元数据（FND3-07）。
	public static @NotNull String schemaTypeName(@NotNull Class<?> cls) {
		if (cls == Boolean.class || cls == boolean.class)
			return "bool";
		if (cls == Byte.class || cls == byte.class)
			return "byte";
		if (cls == Short.class || cls == short.class)
			return "short";
		if (cls == Integer.class || cls == int.class)
			return "int";
		if (cls == Long.class || cls == long.class)
			return "long";
		if (cls == Float.class || cls == float.class)
			return "float";
		if (cls == Double.class || cls == double.class)
			return "double";
		if (cls == String.class)
			return "string";
		if (cls == Zeze.Net.Binary.class)
			return "binary";
		if (cls == java.math.BigDecimal.class)
			return "decimal";
		if (cls == DynamicBean.class)
			return "dynamic";
		if (cls == Zeze.Serialize.Vector2.class)
			return "vector2";
		if (cls == Zeze.Serialize.Vector2Int.class)
			return "vector2int";
		if (cls == Zeze.Serialize.Vector3.class)
			return "vector3";
		if (cls == Zeze.Serialize.Vector3Int.class)
			return "vector3int";
		if (cls == Zeze.Serialize.Vector4.class)
			return "vector4";
		if (cls == Zeze.Serialize.Quaternion.class)
			return "quaternion";
		return cls.getName();
	}
}
