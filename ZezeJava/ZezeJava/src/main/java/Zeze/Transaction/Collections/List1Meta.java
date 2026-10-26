package Zeze.Transaction.Collections;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Transaction.Bean;

/**
 * LogList1 家族（PList1）元数据：非 Bean 值，按值拷贝记账。
 */
public final class List1Meta<V> extends Meta1<V> {
	// 线上typeId原料（hashLog输入），逐字保留，勿改。
	static final String HEAD = "Zeze.Transaction.Collections.LogList1<";
	private static final long headHash = Bean.hash64(HEAD);
	private static final ConcurrentHashMap<Class<?>, List1Meta<?>> metas = new ConcurrentHashMap<>();

	List1Meta(Class<V> valueClass) {
		super("LogList1:", headHash, valueClass);
	}

	public static <V> List1Meta<V> get(Class<V> valueClass) {
		//noinspection unchecked
		return (List1Meta<V>)metas.computeIfAbsent(valueClass, vc -> {
			checkNonBeanValue("PList1 (LogList1)",
					"(in-place modifications never managed, silently lost), use PList2", vc);
			//noinspection unchecked
			return new List1Meta<>((Class<V>)vc);
		});
	}
}
