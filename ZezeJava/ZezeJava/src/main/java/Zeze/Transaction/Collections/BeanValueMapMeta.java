package Zeze.Transaction.Collections;

import java.lang.invoke.MethodHandle;
import java.util.function.Supplier;

import Zeze.Transaction.Bean;
import Zeze.Transaction.DynamicBean;
import Zeze.Transaction.DynamicField;
import Zeze.Util.Reflect;
import org.jspecify.annotations.Nullable;

/** {@link Map2Meta} 和 {@link SortedMap2Meta} 共用的 Bean 值元数据。 */
sealed abstract class BeanValueMapMeta<K, V extends Bean> extends Meta2<K, V> permits Map2Meta, SortedMap2Meta {
	final MethodHandle valueFactory;
	final @Nullable DynamicField dynamicField;

	BeanValueMapMeta(String headStr, long headHash, Class<K> keyClass, Class<V> valueClass, MethodHandle valueFactory) {
		this(Bean.hashLog(headHash, keyClass, valueClass), headStr + keyClass.getName() + ',' + valueClass.getName(),
			keyClass, valueClass, valueFactory, null);
	}

	BeanValueMapMeta(String headStr, long headHash, Class<K> keyClass, DynamicField field) {
		this(Bean.hashLog(headHash, keyClass, DynamicBean.class), headStr + keyClass.getName() + ",DynamicBean",
			keyClass, DynamicBean.class, dynamicFactory(field), field);
	}

	BeanValueMapMeta(int logTypeId, String name, Class<K> keyClass, Class<?> valueClass, MethodHandle valueFactory,
					 @Nullable DynamicField field) {
		super(logTypeId, name, keyClass, valueClass);
		this.valueFactory = valueFactory;
		dynamicField = field;
	}

	private static MethodHandle dynamicFactory(DynamicField field) {
		Supplier<DynamicBean> factory = () -> new DynamicBean(field);
		return Reflect.supplierMH.bindTo(factory);
	}
}
