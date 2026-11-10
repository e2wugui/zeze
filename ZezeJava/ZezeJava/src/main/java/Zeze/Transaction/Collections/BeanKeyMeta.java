package Zeze.Transaction.Collections;

import java.lang.invoke.MethodHandle;
import java.util.concurrent.ConcurrentHashMap;

import Zeze.Serialize.Serializable;
import Zeze.Transaction.Bean;

/** BeanKey 字段的日志元数据。 */
public final class BeanKeyMeta<V> extends Meta1<V> {
	// 沿用 LogBeanKey 的历史哈希前缀，以保持日志类型号。
	private static final long headHash = Bean.hash64("Zeze.Transaction.Log<");
	private static final ConcurrentHashMap<Class<?>, BeanKeyMeta<?>> metas = new ConcurrentHashMap<>();

	public final MethodHandle valueFactory;

	private BeanKeyMeta(Class<V> valueClass) {
		super(Bean.hashLog(headHash, valueClass), "LogBeanKey:" + valueClass.getName());
		valueFactory = publicValueFactory(valueClass);
	}

	public static <V extends Serializable> BeanKeyMeta<V> get(Class<V> beanClass) {
		//noinspection unchecked
		return (BeanKeyMeta<V>)metas.computeIfAbsent(beanClass, BeanKeyMeta::new);
	}
}
