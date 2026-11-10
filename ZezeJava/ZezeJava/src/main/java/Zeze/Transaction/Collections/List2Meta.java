package Zeze.Transaction.Collections;

import org.jspecify.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import Zeze.Transaction.Bean;
import Zeze.Transaction.DynamicField;
import Zeze.Transaction.DynamicBean;
import Zeze.Util.Reflect;

/** PList2 的日志元数据，提供受管 Bean 元素的工厂。 */
public final class List2Meta<V> extends Meta1<V> {
	// 线上日志类型号的哈希前缀，须保持原样。
	private static final long headHash = Bean.hash64("Zeze.Transaction.Collections.LogList2<");
	private static final ConcurrentHashMap<Class<?>, List2Meta<?>> metas = new ConcurrentHashMap<>();

	private static final int dynamicBeanTypeId = Bean.hash32("Zeze.Transaction.Collections.LogList2<Zeze.Transaction.DynamicBean>");
	final MethodHandle valueFactory;
	final @Nullable DynamicField dynamicField;

	private List2Meta(Class<V> valueClass) {
		super(Bean.hashLog(headHash, valueClass), "LogList2:" + valueClass.getName());
		valueFactory = publicValueFactory(valueClass);
		dynamicField = null;
	}

	private List2Meta(DynamicField field) {
		super(dynamicBeanTypeId, "LogList2:DynamicBean");
		Supplier<DynamicBean> factory = () -> new DynamicBean(field);
		valueFactory = Reflect.supplierMH.bindTo(factory);
		dynamicField = field;
	}

	public static List2Meta<DynamicBean> createDynamic(DynamicField field) {
		var meta = new List2Meta<DynamicBean>(field);
		field.bindLog(meta.logTypeId, (varId, bb) -> LogList2.decode(varId, meta, bb));
		return meta;
	}

	public static <V extends Bean> List2Meta<V> get(Class<V> valueClass) {
		//noinspection unchecked
		return (List2Meta<V>)metas.computeIfAbsent(valueClass, List2Meta::new);
	}
}
