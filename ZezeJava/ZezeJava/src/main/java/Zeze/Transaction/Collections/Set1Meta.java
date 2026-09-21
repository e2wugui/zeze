package Zeze.Transaction.Collections;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Transaction.Bean;
import org.jetbrains.annotations.NotNull;

/**
 * LogSet1 家族（PSet1）元数据：框架设计上无PSet2，Bean集合属设计不支持。
 */
public final class Set1Meta<V> extends Meta1<V> {
	// 线上typeId原料（hashLog输入），逐字保留，勿改。
	static final String HEAD = "Zeze.Transaction.Collections.LogSet1<";
	private static final long headHash = Bean.hash64(HEAD);
	private static final ConcurrentHashMap<Class<?>, Set1Meta<?>> metas = new ConcurrentHashMap<>();

	Set1Meta(@NotNull Class<V> valueClass) {
		super("LogSet1:", headHash, valueClass);
	}

	@SuppressWarnings("unchecked")
	public static <V> @NotNull Set1Meta<V> get(@NotNull Class<V> valueClass) {
		return (Set1Meta<V>)metas.computeIfAbsent(valueClass, vc -> {
			checkNonBeanValue("PSet1 (LogSet1)",
					"(equals-without-hashCode misbehaves in hash set; Bean set unsupported by design)", vc);
			return new Set1Meta<>((Class<V>)vc);
		});
	}
}
