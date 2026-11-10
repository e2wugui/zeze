package Zeze.Transaction.Collections;

import java.util.concurrent.ConcurrentHashMap;

import Zeze.Serialize.SerializeHelper;
import Zeze.Transaction.Bean;

/** PSortedMap1 的日志元数据，键有序，按值记录变更。 */
public final class SortedMap1Meta<K, V> extends Meta2<K, V> {
	// 线上日志类型号的哈希前缀，须保持原样。
	private static final long headHash = Bean.hash64("Zeze.Transaction.Collections.LogSortedMap1<");
	private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<Class<?>, SortedMap1Meta<?, ?>>> metas
		= new ConcurrentHashMap<>();

	final SerializeHelper.CodecFuncs<V> valueCodec;

	private SortedMap1Meta(Class<K> keyClass, Class<V> valueClass) {
		super("LogSortedMap1:", headHash, keyClass, valueClass);
		valueCodec = SerializeHelper.createCodec(valueClass);
	}

	public static <K, V> SortedMap1Meta<K, V> get(Class<K> keyClass,
												  Class<V> valueClass) {
		var map = metas.computeIfAbsent(keyClass, kc -> {
			checkNonBeanKey("PSortedMap1 (LogSortedMap1)", kc);
			return new ConcurrentHashMap<>();
		});
		var meta = map.get(valueClass);
		if (meta == null) {
			meta = map.computeIfAbsent(valueClass, ignored -> {
				checkNonBeanValue1("PSortedMap1 (LogSortedMap1)", "use PSortedMap2", valueClass);
				return new SortedMap1Meta<>(keyClass, valueClass);
			});
		}
		//noinspection unchecked
		return (SortedMap1Meta<K, V>)meta;
	}
}
