package Zeze.Transaction.Collections;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Transaction.Bean;
import org.jetbrains.annotations.NotNull;

/**
 * LogOne 家族（CollOne 单Bean变量）元数据。
 */
public final class LogOneMeta<V> extends Meta1<V> {
	// 线上typeId原料（hashLog输入），逐字保留，勿改。
	static final String HEAD = "Zeze.Transaction.Collections.LogOne<";
	private static final long headHash = Bean.hash64(HEAD);
	private static final ConcurrentHashMap<Class<?>, LogOneMeta<?>> metas = new ConcurrentHashMap<>();

	LogOneMeta(@NotNull Class<V> valueClass) {
		super("LogOne:", headHash, valueClass);
	}

	@SuppressWarnings("unchecked")
	public static <V extends Bean> @NotNull LogOneMeta<V> get(@NotNull Class<V> beanClass) {
		return (LogOneMeta<V>)metas.computeIfAbsent(beanClass, vc -> new LogOneMeta<>((Class<V>)vc));
	}
}
