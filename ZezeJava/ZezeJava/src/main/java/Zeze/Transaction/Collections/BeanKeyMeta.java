package Zeze.Transaction.Collections;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Bean;
import org.jetbrains.annotations.NotNull;

/**
 * LogBeanKey 家族（BeanKey 值）元数据。
 */
public final class BeanKeyMeta<V> extends Meta1<V> {
	// 线上typeId原料（hashLog输入）。与LogBeanKey类名不同源（历史头串），勿"修正"——
	// 改动即typeId漂移，存量数据不可解。
	static final String HEAD = "Zeze.Transaction.Log<";
	private static final long headHash = Bean.hash64(HEAD);
	private static final ConcurrentHashMap<Class<?>, BeanKeyMeta<?>> metas = new ConcurrentHashMap<>();

	BeanKeyMeta(@NotNull Class<V> valueClass) {
		super("LogBeanKey:", headHash, valueClass);
	}

	@SuppressWarnings("unchecked")
	public static <V extends Serializable> @NotNull BeanKeyMeta<V> get(@NotNull Class<V> beanClass) {
		return (BeanKeyMeta<V>)metas.computeIfAbsent(beanClass, vc -> new BeanKeyMeta<>((Class<V>)vc));
	}
}
