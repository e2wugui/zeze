package Zeze.Transaction.Collections;

import org.jspecify.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;

import Zeze.Transaction.Bean;

/**
 * LogSortedMap2 家族（PSortedMap2）元数据：Bean 值受管，支持原位修改，键有序。
 */
public final class SortedMap2Meta<K, V> extends Meta2<K, V> {
	// 线上typeId原料（hashLog输入），逐字保留，勿改。
	static final String HEAD = "Zeze.Transaction.Collections.LogSortedMap2<";
	private static final long headHash = Bean.hash64(HEAD);
	private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<Class<?>, SortedMap2Meta<?, ?>>> metas
		= new ConcurrentHashMap<>();

	SortedMap2Meta(Class<K> keyClass, Class<V> valueClass) {
		super("LogSortedMap2:", headHash, keyClass, valueClass);
	}

	SortedMap2Meta(Class<K> keyClass, Class<V> valueClass, Supplier<V> valueCtor) {
		super("LogSortedMap2:", headHash, keyClass, valueClass, valueCtor);
	}

	SortedMap2Meta(Class<K> keyClass, ToLongFunction<@Nullable Bean> get,
				   LongFunction<@Nullable Bean> create) {
		super("LogSortedMap2:", headHash, keyClass, get, create);
	}

	public static <K, V extends Bean> SortedMap2Meta<K, V> get(Class<K> keyClass,
															   Class<V> valueClass) {
		var map = metas.computeIfAbsent(keyClass, kc -> {
			checkNonBeanKey("PSortedMap2 (LogSortedMap2)", kc);
			return new ConcurrentHashMap<>();
		});
		var r = map.get(valueClass);
		if (r != null) {
			//noinspection unchecked
			return (SortedMap2Meta<K, V>)r;
		}
		//noinspection unchecked
		return (SortedMap2Meta<K, V>)map.computeIfAbsent(valueClass,
			vc -> new SortedMap2Meta<>(keyClass, (Class<V>)vc));
	}

	public static <K, V extends Bean> SortedMap2Meta<K, V> create(Class<K> keyClass,
																  Class<V> valueClass,
																  Supplier<V> valueCtor) {
		checkNonBeanKey("PSortedMap2 (LogSortedMap2)", keyClass);
		return new SortedMap2Meta<>(keyClass, valueClass, valueCtor);
	}

	public static <K, V extends Bean> SortedMap2Meta<K, V> createDynamic(Class<K> keyClass,
																		 ToLongFunction<@Nullable Bean> get,
																		 LongFunction<@Nullable Bean> create) {
		checkNonBeanKey("PSortedMap2 (LogSortedMap2)", keyClass);
		return new SortedMap2Meta<>(keyClass, get, create);
	}
}
