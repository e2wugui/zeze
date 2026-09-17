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

	// 工厂层Bean拦截（R2-T backlog④收口）：容器类构造器已拦Bean key/value（判例FND6-41/
	// FND7-05/FND7-09），但"直建meta再走PMap1(meta)/PMap2(meta)/BeanMap(meta)构造器"可绕过。
	// 工厂是公开meta的唯一构建入口，在此拦截即封死全部绕行路径（含GTable的Bean行/列）。
	// Bean key：值语义equals配身份hashCode，日志簿记HashMap/HashSet静默漏命中，可致主从分歧。
	private static <K> void checkNonBeanKey(@NotNull String family, @NotNull Class<K> keyClass) {
		if (Bean.class.isAssignableFrom(keyClass)) {
			throw new IllegalArgumentException(
					family + " does not support Bean key type (equals-without-hashCode misbehaves in hash map): "
							+ keyClass.getName());
		}
	}

	@SuppressWarnings("unchecked")
	public static <K, V> @NotNull Meta2<K, V> getMap1Meta(@NotNull Class<K> keyClass, @NotNull Class<V> valueClass) {
		var map = map1Metas.computeIfAbsent(keyClass, kc -> {
			// Bean key不支持（FND6-41，PSet1判例姊妹）：Bean是值语义equals但身份hashCode（可变bean
			// 不覆写hashCode防哈希漂移），哈希容器对bean键静默漏命中——put/get/remove/contains失真。
			// 显式失败优于静默错。
			if (Bean.class.isAssignableFrom(kc)) {
				throw new IllegalArgumentException(
						"Map1Meta does not support Bean key type (equals-without-hashCode misbehaves in hash map): "
								+ kc.getName());
			}
			return new ConcurrentHashMap<>();
		});
		var r = map.get(valueClass);
		if (r != null)
			return (Meta2<K, V>)r;
		return (Meta2<K, V>)map.computeIfAbsent(valueClass, vc -> {
			// Bean值不支持（FND7-09，PList1判例同族）：1系容器按值拷贝记账，不挂接rootInfo
			// （对比PMap2.put的initRootInfoWithRedo），装入的bean永不受管——原位修改不产生
			// 日志，提交后静默丢失。显式失败优于静默丢数据。
			if (Bean.class.isAssignableFrom(vc)) {
				throw new IllegalArgumentException(
						"Map1Meta does not support Bean value type (in-place modifications never managed, silently lost): "
								+ vc.getName());
			}
			return new Meta2<>("LogMap1:", map1HeadHash, keyClass, (Class<V>)vc);
		});
	}

	/**
	 * 获取（或首次创建并缓存）共享的 Map2 元数据。
	 * 契约：{@link #createMap2Meta} 的自定义 valueCtor 与本方法的默认构造对同一
	 * (keyClass, valueClass) 生成相同 logTypeId（typeId 由 head+两类名散列，与ctor无关），
	 * Log.register 按 typeId 先到先得——ctor 产物若与默认构造产物不等价，后注册方的差异
	 * 在日志反序列化（Log.create 按 typeId 查表）中永远不生效。
	 */
	@SuppressWarnings("unchecked")
	public static <K, V extends Bean> @NotNull Meta2<K, V> getMap2Meta(@NotNull Class<K> keyClass,
																	   @NotNull Class<V> valueClass) {
		var map = map2Metas.computeIfAbsent(keyClass, kc -> {
			// Bean key不支持（FND6-41，PSet1判例姊妹）：Bean是值语义equals但身份hashCode（可变bean
			// 不覆写hashCode防哈希漂移），哈希容器对bean键静默漏命中——put/get/remove/contains失真。
			// 显式失败优于静默错。
			if (Bean.class.isAssignableFrom(kc)) {
				throw new IllegalArgumentException(
						"Map2Meta does not support Bean key type (equals-without-hashCode misbehaves in hash map): "
								+ kc.getName());
			}
			return new ConcurrentHashMap<>();
		});
		var r = map.get(valueClass);
		if (r != null)
			return (Meta2<K, V>)r;
		return (Meta2<K, V>)map.computeIfAbsent(valueClass,
				vc -> new Meta2<>("LogMap2:", map2HeadHash, keyClass, (Class<V>)vc));
	}

	/**
	 * 用自定义 valueCtor 创建 Map2 元数据（不进共享缓存，调用方自行注册）。
	 * 契约：valueCtor 产物必须与 valueClass 默认构造产物（new V()）等价——本方法与
	 * {@link #getMap2Meta} 对同一 (keyClass, valueClass) 算出相同 logTypeId，注册进
	 * Log 工厂表后 typeId 先到先得，两者不等价时后注册方的 ctor 被静默忽略，日志
	 * 反序列化拿到错误构造的实例（如 History 增量回放数据损坏）。
	 */
	public static <K, V extends Bean> @NotNull Meta2<K, V> createMap2Meta(@NotNull Class<K> keyClass,
																		  @NotNull Class<V> valueClass,
																		  @NotNull Supplier<V> valueCtor) {
		checkNonBeanKey("LogMap2", keyClass);
		return new Meta2<>("LogMap2:", map2HeadHash, keyClass, valueClass, valueCtor);
	}

	public static <K, V extends Bean> @NotNull Meta2<K, V> createDynamicMapMeta(@NotNull Class<K> keyClass,
																				@NotNull ToLongFunction<Bean> get,
																				@NotNull LongFunction<Bean> create) {
		checkNonBeanKey("LogMap2", keyClass);
		return new Meta2<>("LogMap2:", map2HeadHash, keyClass, get, create);
	}

	@SuppressWarnings("unchecked")
	public static <K, V> @NotNull Meta2<K, V> getSortedMap1Meta(@NotNull Class<K> keyClass,
																@NotNull Class<V> valueClass) {
		var map = sortedMap1Metas.computeIfAbsent(keyClass, kc -> {
			// Bean key不支持（FND7-05，PMap1/PSet1判例姊妹）：排序map本体TreePMap按compareTo定序没问题，
			// 但日志簿记LogSortedMap1.replaced/removed是HashMap/HashSet——Bean值语义equals配身份
			// hashCode，等值bean落不同桶静默漏命中：mergeChangeNote漏合并，encode按身份哈希决定的
			// 迭代序写出重复条目，follower解码plusAll的终值依赖迭代序，可致静默主从分歧。显式失败优于静默错。
			if (Bean.class.isAssignableFrom(kc)) {
				throw new IllegalArgumentException(
						"SortedMap1Meta does not support Bean key type (equals-without-hashCode misbehaves in hash map): "
								+ kc.getName());
			}
			return new ConcurrentHashMap<>();
		});
		var r = map.get(valueClass);
		if (r != null)
			return (Meta2<K, V>)r;
		return (Meta2<K, V>)map.computeIfAbsent(valueClass, vc -> {
			// Bean值不支持（FND7-09姊妹缺口，PList1/PMap1判例同族）：排序map同为1系按值拷贝记账，
			// put不挂接rootInfo（对比PSortedMap2.put），装入的bean永不受管——原位修改不产生日志，
			// 提交后静默丢失。显式失败优于静默丢数据。
			if (Bean.class.isAssignableFrom(vc)) {
				throw new IllegalArgumentException(
						"SortedMap1Meta does not support Bean value type (in-place modifications never managed, silently lost): "
								+ vc.getName());
			}
			return new Meta2<>("LogSortedMap1:", sortedMap1HeadHash, keyClass, (Class<V>)vc);
		});
	}

	@SuppressWarnings("unchecked")
	public static <K, V extends Bean> @NotNull Meta2<K, V> getSortedMap2Meta(@NotNull Class<K> keyClass,
																			 @NotNull Class<V> valueClass) {
		var map = sortedMap2Metas.computeIfAbsent(keyClass, kc -> {
			// Bean key不支持（FND7-05，PMap2/PSet1判例姊妹）：排序map本体TreePMap按compareTo定序没问题，
			// 但日志簿记LogSortedMap1.replaced/removed是HashMap/HashSet——Bean值语义equals配身份
			// hashCode，等值bean落不同桶静默漏命中：mergeChangeNote漏合并，encode按身份哈希决定的
			// 迭代序写出重复条目，follower解码plusAll的终值依赖迭代序，可致静默主从分歧。显式失败优于静默错。
			if (Bean.class.isAssignableFrom(kc)) {
				throw new IllegalArgumentException(
						"SortedMap2Meta does not support Bean key type (equals-without-hashCode misbehaves in hash map): "
								+ kc.getName());
			}
			return new ConcurrentHashMap<>();
		});
		var r = map.get(valueClass);
		if (r != null)
			return (Meta2<K, V>)r;
		return (Meta2<K, V>)map.computeIfAbsent(valueClass,
				vc -> new Meta2<>("LogSortedMap2:", sortedMap2HeadHash, keyClass, (Class<V>)vc));
	}

	public static <K, V extends Bean> @NotNull Meta2<K, V> createSortedMap2Meta(@NotNull Class<K> keyClass,
																				@NotNull Class<V> valueClass,
																				@NotNull Supplier<V> valueCtor) {
		checkNonBeanKey("LogSortedMap2", keyClass);
		return new Meta2<>("LogSortedMap2:", sortedMap2HeadHash, keyClass, valueClass, valueCtor);
	}

	public static <K, V extends Bean> @NotNull Meta2<K, V> createDynamicSortedMapMeta(@NotNull Class<K> keyClass,
																					  @NotNull ToLongFunction<Bean> get,
																					  @NotNull LongFunction<Bean> create) {
		checkNonBeanKey("LogSortedMap2", keyClass);
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
