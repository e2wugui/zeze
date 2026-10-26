package Zeze.Transaction.Collections;

import java.util.concurrent.ConcurrentHashMap;

import Zeze.Transaction.Bean;

/**
 * LogSortedMap1 家族（PSortedMap1）元数据：非 Bean 值，按值拷贝记账，键有序。
 */
public final class SortedMap1Meta<K, V> extends Meta2<K, V> {
	// 线上typeId原料（hashLog输入），逐字保留，勿改。
	static final String HEAD = "Zeze.Transaction.Collections.LogSortedMap1<";
	private static final long headHash = Bean.hash64(HEAD);
	private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<Class<?>, SortedMap1Meta<?, ?>>> metas
		= new ConcurrentHashMap<>();

	SortedMap1Meta(Class<K> keyClass, Class<V> valueClass) {
		super("LogSortedMap1:", headHash, keyClass, valueClass);
	}

	public static <K, V> SortedMap1Meta<K, V> get(Class<K> keyClass,
												  Class<V> valueClass) {
		var map = metas.computeIfAbsent(keyClass, kc -> {
			checkNonBeanKey("PSortedMap1 (LogSortedMap1)", kc);
			return new ConcurrentHashMap<>();
		});
		var r = map.get(valueClass);
		if (r != null) {
			//noinspection unchecked
			return (SortedMap1Meta<K, V>)r;
		}
		//noinspection unchecked
		return (SortedMap1Meta<K, V>)map.computeIfAbsent(valueClass, vc -> {
			checkNonBeanValue1("PSortedMap1 (LogSortedMap1)", "use PSortedMap2", vc);
			//noinspection unchecked
			return new SortedMap1Meta<>(keyClass, (Class<V>)vc);
		});
	}
}
