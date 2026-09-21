package Zeze.Transaction.Collections;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
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

// 家族封闭：sealed+包私有构造器，子类各自硬编码家族头串（线上typeId原料，逐字保留）。
// 容器层（P*）构造器只收对应子类型，日志层（Log*）与JSON等家族无关消费方收本基类；
// 勿再引入收基类的容器构造器——跨家族meta借道对家typeId注册，复制端followerApply强转CCE。
public sealed abstract class Meta2<K, V> permits Map1Meta, Map2Meta, SortedMap1Meta, SortedMap2Meta {
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
	// 实例的真实 key/value 类型，供 variables() 等元数据推导。
	public final @NotNull Class<?> keyClass;
	public final @NotNull Class<?> valueClass;

	Meta2(@NotNull String headStr, long headHash, @NotNull Class<K> keyClass, @NotNull Class<V> valueClass,
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

	Meta2(@NotNull String headStr, long headHash, @NotNull Class<K> keyClass, @NotNull Class<V> valueClass,
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

	Meta2(@NotNull String headStr, long headHash, @NotNull Class<K> keyClass, @NotNull Class<V> valueClass) {
		this(headStr, headHash, keyClass, valueClass,
				Bean.class.isAssignableFrom(valueClass) ? Reflect.getDefaultConstructor(valueClass) : null);
	}

	Meta2(@NotNull String headStr, long headHash, @NotNull Class<K> keyClass, @NotNull ToLongFunction<Bean> get,
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

	// Bean key：值语义equals配身份hashCode，日志簿记, HashMap/HashSet静默漏命中，可致主从分歧。
	// check在computeIfAbsent内按miss执行：被拒类型永不入缓存，每次调用皆重抛；不变量是
	// "被拒类型永不在缓存中"——任何新的缓存插入路径必须经由本check，不允许绕过直插。
	static void checkNonBeanKey(@NotNull String family, @NotNull Class<?> keyClass) {
		if (Bean.class.isAssignableFrom(keyClass))
			throw new IllegalArgumentException(
					family + " does not support Bean key type (equals-without-hashCode misbehaves in hash map), use BeanKey: "
							+ keyClass.getName());
	}

	// Bean值（仅1系）：1系按值拷贝记账、不挂接rootInfo，装入的bean永不受管，原位修改静默丢失。
	// 2系（LogMap2/LogSortedMap2）Bean值受管合法，不拦。执行位置同checkNonBeanKey的说明。
	static void checkNonBeanValue1(@NotNull String family, @NotNull String advice, @NotNull Class<?> valueClass) {
		if (Bean.class.isAssignableFrom(valueClass))
			throw new IllegalArgumentException(
					family + " does not support Bean value type (in-place modifications never managed, silently lost), "
							+ advice + ": " + valueClass.getName());
	}

	// Java Class → schema 类型名，与生成器（Gen/Types/Variable.GetTypeFullName）的输出对齐：
	// 内建类型用 schema 关键字，bean/枚举用全名（生成约定 schema 全名 == Java 类名）。
	// 供运行时从 Meta2.keyClass/valueClass 推导 variables() 等类型元数据。
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
