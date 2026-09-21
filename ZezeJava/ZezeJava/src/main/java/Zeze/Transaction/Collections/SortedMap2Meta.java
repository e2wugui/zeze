package Zeze.Transaction.Collections;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;
import Zeze.Transaction.Bean;
import org.jetbrains.annotations.NotNull;

/**
 * LogSortedMap2 家族（PSortedMap2）元数据：Bean 值受管，支持原位修改，键有序。
 */
public final class SortedMap2Meta<K, V> extends Meta2<K, V> {
	// 线上typeId原料（hashLog输入），逐字保留，勿改。
	static final String HEAD = "Zeze.Transaction.Collections.LogSortedMap2<";
	private static final long headHash = Bean.hash64(HEAD);
	private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<Class<?>, SortedMap2Meta<?, ?>>> metas
			= new ConcurrentHashMap<>();

	SortedMap2Meta(@NotNull Class<K> keyClass, @NotNull Class<V> valueClass) {
		super("LogSortedMap2:", headHash, keyClass, valueClass);
	}

	SortedMap2Meta(@NotNull Class<K> keyClass, @NotNull Class<V> valueClass, @NotNull Supplier<V> valueCtor) {
		super("LogSortedMap2:", headHash, keyClass, valueClass, valueCtor);
	}

	SortedMap2Meta(@NotNull Class<K> keyClass, @NotNull ToLongFunction<Bean> get,
	               @NotNull LongFunction<Bean> create) {
		super("LogSortedMap2:", headHash, keyClass, get, create);
	}

	@SuppressWarnings("unchecked")
	public static <K, V extends Bean> @NotNull SortedMap2Meta<K, V> get(@NotNull Class<K> keyClass,
	                                                                     @NotNull Class<V> valueClass) {
		var map = metas.computeIfAbsent(keyClass, kc -> {
			checkNonBeanKey("PSortedMap2 (LogSortedMap2)", kc);
			return new ConcurrentHashMap<>();
		});
		var r = map.get(valueClass);
		if (r != null)
			return (SortedMap2Meta<K, V>)r;
		return (SortedMap2Meta<K, V>)map.computeIfAbsent(valueClass,
				vc -> new SortedMap2Meta<>(keyClass, (Class<V>)vc));
	}

	public static <K, V extends Bean> @NotNull SortedMap2Meta<K, V> create(@NotNull Class<K> keyClass,
	                                                                        @NotNull Class<V> valueClass,
	                                                                        @NotNull Supplier<V> valueCtor) {
		checkNonBeanKey("PSortedMap2 (LogSortedMap2)", keyClass);
		return new SortedMap2Meta<>(keyClass, valueClass, valueCtor);
	}

	public static <K, V extends Bean> @NotNull SortedMap2Meta<K, V> createDynamic(@NotNull Class<K> keyClass,
	                                                                               @NotNull ToLongFunction<Bean> get,
	                                                                               @NotNull LongFunction<Bean> create) {
		checkNonBeanKey("PSortedMap2 (LogSortedMap2)", keyClass);
		return new SortedMap2Meta<>(keyClass, get, create);
	}
}
