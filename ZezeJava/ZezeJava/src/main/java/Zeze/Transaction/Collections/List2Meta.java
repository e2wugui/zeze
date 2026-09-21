package Zeze.Transaction.Collections;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongFunction;
import java.util.function.ToLongFunction;
import Zeze.Transaction.Bean;
import org.jetbrains.annotations.NotNull;

/**
 * LogList2 家族（PList2）元数据：Bean 值受管（IdentityHashSet+身份比较是既有约定，见LogList2）。
 */
public final class List2Meta<V> extends Meta1<V> {
	// 线上typeId原料（hashLog输入），逐字保留，勿改。
	static final String HEAD = "Zeze.Transaction.Collections.LogList2<";
	private static final long headHash = Bean.hash64(HEAD);
	private static final ConcurrentHashMap<Class<?>, List2Meta<?>> metas = new ConcurrentHashMap<>();

	List2Meta(@NotNull Class<V> valueClass) {
		super("LogList2:", headHash, valueClass);
	}

	List2Meta(@NotNull ToLongFunction<Bean> get, @NotNull LongFunction<Bean> create) {
		super(get, create);
	}

	@SuppressWarnings("unchecked")
	public static <V extends Bean> @NotNull List2Meta<V> get(@NotNull Class<V> valueClass) {
		return (List2Meta<V>)metas.computeIfAbsent(valueClass, vc -> new List2Meta<>((Class<V>)vc));
	}

	public static <V extends Bean> @NotNull List2Meta<V> createDynamic(@NotNull ToLongFunction<Bean> get,
																	   @NotNull LongFunction<Bean> create) {
		return new List2Meta<>(get, create);
	}
}
