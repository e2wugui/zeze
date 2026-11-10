package Zeze.Transaction.Collections;

import java.util.concurrent.ConcurrentHashMap;

import Zeze.Serialize.SerializeHelper;
import Zeze.Transaction.Bean;

/** PMap1 和 GTable1 行内 Map 的日志元数据，按值记录变更。 */
public final class Map1Meta<K, V> extends Meta2<K, V> {
	// 线上日志类型号的哈希前缀，须保持原样。
	private static final long headHash = Bean.hash64("Zeze.Transaction.Collections.LogMap1<");

	private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<Class<?>, Map1Meta<?, ?>>> metas
		= new ConcurrentHashMap<>();

	public final SerializeHelper.CodecFuncs<V> valueCodec;

	private Map1Meta(Class<K> keyClass, Class<V> valueClass) {
		super("LogMap1:", headHash, keyClass, valueClass);
		valueCodec = SerializeHelper.createCodec(valueClass);
	}

	public static <K, V> Map1Meta<K, V> get(Class<K> keyClass, Class<V> valueClass) {
		var map = metas.computeIfAbsent(keyClass, kc -> {
			checkNonBeanKey("PMap1/GTable1 (LogMap1)", kc);
			return new ConcurrentHashMap<>();
		});
		var meta = map.get(valueClass);
		if (meta == null) {
			meta = map.computeIfAbsent(valueClass, ignored -> {
				checkNonBeanValue1("PMap1/GTable1 (LogMap1)", "use PMap2/GTable2", valueClass);
				return new Map1Meta<>(keyClass, valueClass);
			});
		}
		//noinspection unchecked
		return (Map1Meta<K, V>)meta;
	}
}
