package Zeze.Transaction.Collections;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Transaction.Bean;
import org.jetbrains.annotations.NotNull;

/**
 * LogMap1 家族（PMap1/GTable1 内层）元数据：非 Bean 值，按值拷贝记账。
 */
public final class Map1Meta<K, V> extends Meta2<K, V> {
	// 线上typeId原料（hashLog输入），逐字保留，勿改。
	static final String HEAD = "Zeze.Transaction.Collections.LogMap1<";
	private static final long headHash = Bean.hash64(HEAD);
	private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<Class<?>, Map1Meta<?, ?>>> metas
			= new ConcurrentHashMap<>();

	Map1Meta(@NotNull Class<K> keyClass, @NotNull Class<V> valueClass) {
		super("LogMap1:", headHash, keyClass, valueClass);
	}

	@SuppressWarnings("unchecked")
	public static <K, V> @NotNull Map1Meta<K, V> get(@NotNull Class<K> keyClass, @NotNull Class<V> valueClass) {
		var map = metas.computeIfAbsent(keyClass, kc -> {
			checkNonBeanKey("PMap1/GTable1 (LogMap1)", kc);
			return new ConcurrentHashMap<>();
		});
		var r = map.get(valueClass);
		if (r != null)
			return (Map1Meta<K, V>)r;
		return (Map1Meta<K, V>)map.computeIfAbsent(valueClass, vc -> {
			checkNonBeanValue1("PMap1/GTable1 (LogMap1)", "use PMap2/GTable2", vc);
			return new Map1Meta<>(keyClass, (Class<V>)vc);
		});
	}
}
