package Zeze.Transaction.Collections;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import Zeze.Serialize.Encodable;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.SerializeHelper;
import Zeze.Transaction.Bean;
import Zeze.Transaction.BeanKey;
import Zeze.Util.Reflect;

/** BeanKey 字段的日志元数据。 */
public final class BeanKeyMeta<V> extends Meta1<V> {
	// 沿用 LogBeanKey 的历史哈希前缀，以保持日志类型号。
	private static final long headHash = Bean.hash64("Zeze.Transaction.Log<");
	private static final ConcurrentHashMap<Class<?>, BeanKeyMeta<?>> metas = new ConcurrentHashMap<>();

	public final Function<IByteBuffer, V> valueDecoder;

	private BeanKeyMeta(Class<V> valueClass) {
		super(Bean.hashLog(headHash, valueClass), "LogBeanKey:" + valueClass.getName());
		valueDecoder = BeanKey.class.isAssignableFrom(valueClass)
				? SerializeHelper.createDecodeFunc(valueClass)
				: SerializeHelper.createCodec(valueClass, Reflect.getDefaultConstructor(valueClass)).decoder;
	}

	public static <V extends Encodable> BeanKeyMeta<V> get(Class<V> beanClass) {
		//noinspection unchecked
		return (BeanKeyMeta<V>)metas.computeIfAbsent(beanClass, BeanKeyMeta::new);
	}
}
