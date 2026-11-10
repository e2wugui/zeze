package Zeze.Transaction.Collections;

import org.jspecify.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.DynamicBean;
import Zeze.Transaction.DynamicField;
import Zeze.Util.Reflect;

/** PMap2 及 GTable 中以受管 Bean 为值的 Map 日志元数据。 */
public final class Map2Meta<K, V extends Bean> extends BeanValueMapMeta<K, V> {
	// 线上日志类型号的哈希前缀，须保持原样。
	private static final long headHash = Bean.hash64("Zeze.Transaction.Collections.LogMap2<");
	private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<Class<?>, Map2Meta<?, ?>>> metas
		= new ConcurrentHashMap<>();

	public final int valueEncodeType;
	private final @Nullable Consumer<? super V> valueValidator;

	private Map2Meta(Class<K> keyClass, Class<V> valueClass) {
		this(keyClass, valueClass, Reflect.getDefaultConstructor(valueClass));
	}

	private Map2Meta(Class<K> keyClass, Class<V> valueClass, Supplier<V> valueCtor) {
		this(keyClass, valueClass, Reflect.supplierMH.bindTo(valueCtor));
	}

	private Map2Meta(Class<K> keyClass, Class<V> valueClass, MethodHandle valueFactory) {
		super("LogMap2:", headHash, keyClass, valueClass, valueFactory);
		valueEncodeType = IByteBuffer.BEAN;
		valueValidator = null;
	}

	private Map2Meta(long familyHeadHash, String namePrefix, Class<K> keyClass, Class<V> valueClass,
					 String valueIdentity, Supplier<V> valueCtor, @Nullable DynamicField field,
					 @Nullable Consumer<? super V> valueValidator) {
		super(Bean.hashLog(familyHeadHash, Reflect.getStableName(keyClass), valueIdentity),
				namePrefix + keyClass.getName() + ',' + valueIdentity, keyClass, valueClass,
				Reflect.supplierMH.bindTo(valueCtor), field);
		valueEncodeType = IByteBuffer.BEAN;
		this.valueValidator = valueValidator;
	}

	private Map2Meta(Class<K> keyClass, DynamicField field) {
		super("LogMap2:", headHash, keyClass, field);
		valueEncodeType = IByteBuffer.DYNAMIC;
		valueValidator = null;
	}

	void checkValue(V value) {
		DynamicField.checkValueOwner(dynamicField, value);
		if (valueValidator != null)
			valueValidator.accept(value);
	}

	public static <K> Map2Meta<K, DynamicBean> createDynamic(Class<K> keyClass, DynamicField field) {
		checkNonBeanKey("LogMap2:", keyClass);
		var meta = new Map2Meta<K, DynamicBean>(keyClass, field);
		field.bindLog(meta.logTypeId, (varId, bb) -> LogMap2.decode(varId, meta, bb));
		return meta;
	}

	/** 按键和值的类型获取共享元数据，值用默认构造器创建。 */
	public static <K, V extends Bean> Map2Meta<K, V> get(Class<K> keyClass,
														 Class<V> valueClass) {
		var map = metas.computeIfAbsent(keyClass, kc -> {
			checkNonBeanKey("PMap2/GTable2 (LogMap2)", kc);
			return new ConcurrentHashMap<>();
		});
		var meta = map.get(valueClass);
		if (meta == null)
			meta = map.computeIfAbsent(valueClass, ignored -> new Map2Meta<>(keyClass, valueClass));
		//noinspection unchecked
		return (Map2Meta<K, V>)meta;
	}

	/**
	 * 用自定义工厂创建独立元数据，由调用方注册。
	 * 工厂产物须与默认构造等价。同一键值类型共用日志类型号，解码使用先注册的工厂。
	 */
	public static <K, V extends Bean> Map2Meta<K, V> create(Class<K> keyClass,
															Class<V> valueClass,
															Supplier<V> valueCtor) {
		checkNonBeanKey("PMap2/GTable2 (LogMap2)", keyClass);
		return new Map2Meta<>(keyClass, valueClass, valueCtor);
	}

	/**
	 * 创建 GTable 外层元数据，由调用方缓存。
	 * 日志类型号由 GTable 家族、行键类型和 {@code valueIdentity} 中的完整列值类型决定。
	 */
	public static <K, V extends Bean> Map2Meta<K, V> createWithFamily(
		String familyHead, String namePrefix,
		Class<K> keyClass, Class<V> valueClass,
		String valueIdentity, Supplier<V> valueCtor) {
		return createWithFamily(familyHead, namePrefix, keyClass, valueClass, valueIdentity, valueCtor, null);
	}

	public static <K, V extends Bean> Map2Meta<K, V> createWithFamily(
		String familyHead, String namePrefix, Class<K> keyClass, Class<V> valueClass,
		String valueIdentity, Supplier<V> valueCtor, @Nullable DynamicField field) {
		return createWithFamily(familyHead, namePrefix, keyClass, valueClass, valueIdentity, valueCtor, field, null);
	}

	/** GTable 的外层 Map 在挂接前通过校验器核对行的内层 Meta。 */
	public static <K, V extends Bean> Map2Meta<K, V> createWithFamily(
		String familyHead, String namePrefix, Class<K> keyClass, Class<V> valueClass,
		String valueIdentity, Supplier<V> valueCtor, @Nullable DynamicField field,
		@Nullable Consumer<? super V> valueValidator) {
		checkNonBeanKey("GTable outer (" + namePrefix + ")", keyClass);
		var meta = new Map2Meta<>(Bean.hash64(familyHead), namePrefix, keyClass, valueClass,
			valueIdentity, valueCtor, field, valueValidator);
		if (field != null)
			field.bindLog(meta.logTypeId, (varId, bb) -> LogMap2.decode(varId, meta, bb));
		return meta;
	}
}
