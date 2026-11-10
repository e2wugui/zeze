package Zeze.Transaction.Collections;

import java.lang.invoke.MethodHandle;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Transaction.Bean;

/** CollOne 的单 Bean 日志元数据。 */
public final class LogOneMeta<V> extends Meta1<V> {
	// 线上日志类型号的哈希前缀，须保持原样。
	private static final long headHash = Bean.hash64("Zeze.Transaction.Collections.LogOne<");
	private static final ConcurrentHashMap<Class<?>, LogOneMeta<?>> metas = new ConcurrentHashMap<>();

	final MethodHandle valueFactory;

	private LogOneMeta(Class<V> valueClass) {
		super(Bean.hashLog(headHash, valueClass), "LogOne:" + valueClass.getName());
		valueFactory = publicValueFactory(valueClass);
	}

	public static <V extends Bean> LogOneMeta<V> get(Class<V> beanClass) {
		//noinspection unchecked
		return (LogOneMeta<V>)metas.computeIfAbsent(beanClass, LogOneMeta::new);
	}
}
