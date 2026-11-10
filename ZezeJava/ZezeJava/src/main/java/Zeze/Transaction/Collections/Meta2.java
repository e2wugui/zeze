package Zeze.Transaction.Collections;

import java.util.function.BiConsumer;
import java.util.function.Function;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.SerializeHelper;
import Zeze.Transaction.Bean;
import Zeze.Transaction.DynamicBean;

/**
 * 键值日志的公共元数据，保存日志身份、键值类型和键编解码器。
 * <ul>
 *   <li>{@link Map1Meta} 和 {@link SortedMap1Meta} 提供非 Bean 值的编解码。</li>
 *   <li>{@link Map2Meta} 和 {@link SortedMap2Meta} 通过 {@link BeanValueMapMeta} 共用 Bean 工厂和动态字段绑定。</li>
 *   <li>GTable 外层的值是行 Bean，因此使用 {@link Map2Meta}。行内按单元格的值类型选 Map1 或 Map2。</li>
 * </ul>
 */
public sealed abstract class Meta2<K, V> permits Map1Meta, SortedMap1Meta, BeanValueMapMeta {
	public final int logTypeId;
	public final String name;
	public final Class<?> keyClass;
	public final Class<?> valueClass;
	public final int keyEncodeType;
	public final BiConsumer<ByteBuffer, K> keyEncoder;
	public final Function<IByteBuffer, K> keyDecoder;
	public final SerializeHelper.ObjectIntFunction<IByteBuffer, K> keyDecoderWithType;

	Meta2(String headStr, long headHash, Class<K> keyClass, Class<V> valueClass) {
		this(Bean.hashLog(headHash, keyClass, valueClass),
			headStr + keyClass.getName() + ',' + valueClass.getName(), keyClass, valueClass);
	}

	Meta2(int logTypeId, String name, Class<K> keyClass, Class<?> valueClass) {
		this.logTypeId = logTypeId;
		this.name = name;
		this.keyClass = keyClass;
		this.valueClass = valueClass;
		var keyCodec = SerializeHelper.createCodec(keyClass);
		keyEncodeType = keyCodec.encodeType;
		keyEncoder = keyCodec.encoder;
		keyDecoder = keyCodec.decoder;
		keyDecoderWithType = keyCodec.decoderWithType;
	}

	// Bean 的 equals 与 hashCode 语义不一致，会破坏日志的键查找。须在写入缓存前拒绝。
	static void checkNonBeanKey(String family, Class<?> keyClass) {
		if (Bean.class.isAssignableFrom(keyClass))
			throw new IllegalArgumentException(
				family + " does not support Bean key type (equals-without-hashCode misbehaves in hash map), use BeanKey: "
					+ keyClass.getName());
	}

	// 1 系不跟踪 Bean 的原位修改，须在写入缓存前拒绝。
	static void checkNonBeanValue1(String family, String advice, Class<?> valueClass) {
		if (Bean.class.isAssignableFrom(valueClass))
			throw new IllegalArgumentException(
				family + " does not support Bean value type (in-place modifications never managed, silently lost), "
					+ advice + ": " + valueClass.getName());
	}

	// 为 variables() 推导 schema 类型名，命名与生成器的 Variable.GetTypeFullName 一致。
	public static String schemaTypeName(Class<?> cls) {
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
