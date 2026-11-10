package Zeze.Transaction.Collections;

import java.lang.invoke.MethodHandle;

import Zeze.Transaction.Bean;
import Zeze.Util.Reflect;
import Zeze.Util.Task;

/**
 * 单值类型日志的公共元数据。各家族定义自己的线上类型号。
 * <ul>
 *   <li>{@link List1Meta} 和 {@link Set1Meta} 提供非 Bean 元素的编解码。</li>
 *   <li>{@link List2Meta} 提供受管 Bean 元素的工厂，动态元素按字段绑定。</li>
 *   <li>{@link LogOneMeta} 和 {@link BeanKeyMeta} 分别提供单个 Bean 和 BeanKey 的解码工厂。</li>
 * </ul>
 */
public sealed abstract class Meta1<V> permits BeanKeyMeta, List1Meta, List2Meta, LogOneMeta, Set1Meta {
	public final int logTypeId;
	public final String name;

	Meta1(int logTypeId, String name) {
		this.logTypeId = logTypeId;
		this.name = name;
	}

	// 这些家族的反射工厂要求 public 无参构造器。
	static MethodHandle publicValueFactory(Class<?> valueClass) {
		try {
			var ctor = valueClass.getConstructor();
			ctor.setAccessible(true);
			return Reflect.lookup.unreflectConstructor(ctor);
		} catch (ReflectiveOperationException e) {
			throw Task.forceThrow(e);
		}
	}

	// 写入缓存前拒绝 Bean 值，拒绝原因由各家族提供。
	static void checkNonBeanValue(String family, String reason, Class<?> valueClass) {
		if (Bean.class.isAssignableFrom(valueClass))
			throw new IllegalArgumentException(
				family + " does not support Bean value type " + reason + ": " + valueClass.getName());
	}
}
