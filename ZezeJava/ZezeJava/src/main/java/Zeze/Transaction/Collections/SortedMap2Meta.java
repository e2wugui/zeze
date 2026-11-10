package Zeze.Transaction.Collections;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import Zeze.Transaction.Bean;
import Zeze.Transaction.DynamicBean;
import Zeze.Transaction.DynamicField;
import Zeze.Util.Reflect;

/** PSortedMap2 的日志元数据，键有序，支持受管 Bean 的原位修改。 */
public final class SortedMap2Meta<K, V extends Bean> extends BeanValueMapMeta<K, V> {
	// 线上日志类型号的哈希前缀，须保持原样。
	private static final long headHash = Bean.hash64("Zeze.Transaction.Collections.LogSortedMap2<");
	private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<Class<?>, SortedMap2Meta<?, ?>>> metas
		= new ConcurrentHashMap<>();

	private SortedMap2Meta(Class<K> keyClass, Class<V> valueClass) {
		super("LogSortedMap2:", headHash, keyClass, valueClass, Reflect.getDefaultConstructor(valueClass));
	}

	private SortedMap2Meta(Class<K> keyClass, Class<V> valueClass, Supplier<V> valueCtor) {
		super("LogSortedMap2:", headHash, keyClass, valueClass, Reflect.supplierMH.bindTo(valueCtor));
	}

	private SortedMap2Meta(Class<K> keyClass, DynamicField field) {
		super("LogSortedMap2:", headHash, keyClass, field);
	}

	public static <K extends Comparable<K>> SortedMap2Meta<K, DynamicBean> createDynamic(Class<K> keyClass, DynamicField field) {
		checkNonBeanKey("LogSortedMap2:", keyClass);
		var meta = new SortedMap2Meta<K, DynamicBean>(keyClass, field);
		field.bindLog(meta.logTypeId, (varId, bb) -> LogSortedMap2.decode(varId, meta, bb));
		return meta;
	}

	public static <K, V extends Bean> SortedMap2Meta<K, V> get(Class<K> keyClass,
															   Class<V> valueClass) {
		var map = metas.computeIfAbsent(keyClass, kc -> {
			checkNonBeanKey("PSortedMap2 (LogSortedMap2)", kc);
			return new ConcurrentHashMap<>();
		});
		var meta = map.get(valueClass);
		if (meta == null)
			meta = map.computeIfAbsent(valueClass, ignored -> new SortedMap2Meta<>(keyClass, valueClass));
		//noinspection unchecked
		return (SortedMap2Meta<K, V>)meta;
	}

	public static <K, V extends Bean> SortedMap2Meta<K, V> create(Class<K> keyClass,
																  Class<V> valueClass,
																  Supplier<V> valueCtor) {
		checkNonBeanKey("PSortedMap2 (LogSortedMap2)", keyClass);
		return new SortedMap2Meta<>(keyClass, valueClass, valueCtor);
	}
}
