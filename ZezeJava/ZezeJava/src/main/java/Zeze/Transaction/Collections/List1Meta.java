package Zeze.Transaction.Collections;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Serialize.SerializeHelper;
import Zeze.Transaction.Bean;

/** PList1 的日志元数据，按值记录非 Bean 元素的变更。 */
public final class List1Meta<V> extends Meta1<V> {
	// 线上日志类型号的哈希前缀，须保持原样。
	private static final long headHash = Bean.hash64("Zeze.Transaction.Collections.LogList1<");
	private static final ConcurrentHashMap<Class<?>, List1Meta<?>> metas = new ConcurrentHashMap<>();

	final SerializeHelper.CodecFuncs<V> valueCodec;

	private List1Meta(Class<V> valueClass) {
		super(Bean.hashLog(headHash, valueClass), "LogList1:" + valueClass.getName());
		valueCodec = SerializeHelper.createCodec(valueClass);
	}

	public static <V> List1Meta<V> get(Class<V> valueClass) {
		//noinspection unchecked
		return (List1Meta<V>)metas.computeIfAbsent(valueClass, vc -> {
			checkNonBeanValue("PList1 (LogList1)",
					"(in-place modifications never managed, silently lost), use PList2", vc);
			return new List1Meta<>(vc);
		});
	}
}
