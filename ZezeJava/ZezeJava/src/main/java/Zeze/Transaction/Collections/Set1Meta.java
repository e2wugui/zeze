package Zeze.Transaction.Collections;

import java.util.concurrent.ConcurrentHashMap;

import Zeze.Serialize.SerializeHelper;
import Zeze.Transaction.Bean;

/** PSet1 的日志元数据，元素须为非 Bean 类型。 */
public final class Set1Meta<V> extends Meta1<V> {
	// 线上日志类型号的哈希前缀，须保持原样。
	private static final long headHash = Bean.hash64("Zeze.Transaction.Collections.LogSet1<");
	private static final ConcurrentHashMap<Class<?>, Set1Meta<?>> metas = new ConcurrentHashMap<>();

	final SerializeHelper.CodecFuncs<V> valueCodec;

	private Set1Meta(Class<V> valueClass) {
		super(Bean.hashLog(headHash, valueClass), "LogSet1:" + valueClass.getName());
		valueCodec = SerializeHelper.createCodec(valueClass);
	}

	public static <V> Set1Meta<V> get(Class<V> valueClass) {
		//noinspection unchecked
		return (Set1Meta<V>)metas.computeIfAbsent(valueClass, vc -> {
			checkNonBeanValue("PSet1 (LogSet1)",
				"(equals-without-hashCode misbehaves in hash set; Bean set unsupported by design)", vc);
			return new Set1Meta<>(vc);
		});
	}
}
