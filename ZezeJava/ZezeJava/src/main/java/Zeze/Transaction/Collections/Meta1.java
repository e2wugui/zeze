package Zeze.Transaction.Collections;

import java.lang.invoke.MethodHandle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.ToLongFunction;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Serialize.SerializeHelper;
import Zeze.Transaction.Bean;
import Zeze.Transaction.BeanKey;
import Zeze.Util.Reflect;
import org.jetbrains.annotations.NotNull;

public final class Meta1<V> {
	private static final long beanHeadHash = Bean.hash64("Zeze.Transaction.Log<");
	private static final long logOneHeadHash = Bean.hash64("Zeze.Transaction.Collections.LogOne<");
	private static final long list1HeadHash = Bean.hash64("Zeze.Transaction.Collections.LogList1<");
	private static final long list2HeadHash = Bean.hash64("Zeze.Transaction.Collections.LogList2<");
	private static final long set1HeadHash = Bean.hash64("Zeze.Transaction.Collections.LogSet1<");
	private static final int dynamicBeanTypeId = Bean.hash32("Zeze.Transaction.Collections.LogList2<Zeze.Transaction.DynamicBean>");
	private static final ConcurrentHashMap<Class<?>, Meta1<?>> beanMetas = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<Class<?>, Meta1<?>> logOneMetas = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<Class<?>, Meta1<?>> list1Metas = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<Class<?>, Meta1<?>> list2Metas = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<Class<?>, Meta1<?>> set1Metas = new ConcurrentHashMap<>();

	public final int logTypeId;
	public final int valueEncodeType;
	public final BiConsumer<ByteBuffer, V> valueEncoder; // 只用于非Bean类型
	public final Function<IByteBuffer, V> valueDecoder; // 只用于非Bean类型
	public final SerializeHelper.ObjectIntFunction<IByteBuffer, V> valueDecoderWithType; // 只用于非Bean类型
	public final MethodHandle valueFactory; // 只用于Bean类型
	public final @NotNull String name; // 主要用于分析查错

	private Meta1(@NotNull String headStr, long headHash, @NotNull Class<V> valueClass) {
		logTypeId = Bean.hashLog(headHash, valueClass);
		var valueCodecFuncs = SerializeHelper.createCodec(valueClass);
		valueEncodeType = valueCodecFuncs.encodeType;
		valueEncoder = valueCodecFuncs.encoder;
		valueDecoder = valueCodecFuncs.decoder;
		valueDecoderWithType = valueCodecFuncs.decoderWithType;
		valueFactory = BeanKey.class.isAssignableFrom(valueClass) || Bean.class.isAssignableFrom(valueClass)
				? Reflect.getDefaultConstructor(valueClass) : null;
		name = headStr + valueClass.getName();
	}

	private Meta1(@NotNull ToLongFunction<Bean> get, @NotNull LongFunction<Bean> create) {
		logTypeId = dynamicBeanTypeId;
		valueEncodeType = IByteBuffer.DYNAMIC;
		valueEncoder = null;
		valueDecoder = null;
		valueDecoderWithType = null;
		valueFactory = SerializeHelper.createDynamicFactory(get, create);
		name = "LogList2:DynamicBean";
	}

	@SuppressWarnings("unchecked")
	public static <V extends Bean> @NotNull Meta1<V> getLogOneMeta(@NotNull Class<V> beanClass) {
		return (Meta1<V>)logOneMetas.computeIfAbsent(beanClass,
				vc -> new Meta1<>("LogOne:", logOneHeadHash, (Class<V>)vc));
	}

	@SuppressWarnings("unchecked")
	public static <V extends Serializable> @NotNull Meta1<V> getBeanMeta(@NotNull Class<V> beanClass) {
		return (Meta1<V>)beanMetas.computeIfAbsent(beanClass,
				vc -> new Meta1<>("LogBeanKey:", beanHeadHash, (Class<V>)vc));
	}

	// 1系容器不支持Bean值，工厂层是唯一拦截点（R2-T backlog④收口；原容器构造器层check经复核
	// 逐路径冗余已删，判例注释并归于此）：List1按值拷贝记账、不挂接rootInfo，装入的bean永不受管，
	// 原位修改静默丢失；Set1是哈希语义——Bean值语义equals配身份hashCode（可变bean
	// 不覆写hashCode防哈希漂移），去重/remove/removeAll失真，且框架设计上无PSet2，bean集合属
	// 设计不支持；LogList2用IdentityHashSet+身份比较是既有约定，见LogList2）。
	// check在computeIfAbsent内按miss执行：被拒类型永不入缓存，每次调用皆重抛；不变量是
	// "被拒类型永不在缓存中"——任何新的缓存插入路径必须经由本check，不允许绕过直插。
	private static void checkNonBeanValue(@NotNull String family, @NotNull String reason, @NotNull Class<?> valueClass) {
		if (Bean.class.isAssignableFrom(valueClass))
			throw new IllegalArgumentException(
					family + " does not support Bean value type " + reason + ": " + valueClass.getName());
	}

	@SuppressWarnings("unchecked")
	public static <V> @NotNull Meta1<V> getList1Meta(@NotNull Class<V> valueClass) {
		return (Meta1<V>)list1Metas.computeIfAbsent(valueClass, vc -> {
			checkNonBeanValue("PList1 (LogList1)",
					"(in-place modifications never managed, silently lost), use PList2", vc);
			return new Meta1<>("LogList1:", list1HeadHash, (Class<V>)vc);
		});
	}

	@SuppressWarnings("unchecked")
	public static <V extends Bean> @NotNull Meta1<V> getList2Meta(@NotNull Class<V> valueClass) {
		return (Meta1<V>)list2Metas.computeIfAbsent(valueClass,
				vc -> new Meta1<>("LogList2:", list2HeadHash, (Class<V>)vc));
	}

	@SuppressWarnings("unchecked")
	public static <V> @NotNull Meta1<V> getSet1Meta(@NotNull Class<V> valueClass) {
		return (Meta1<V>)set1Metas.computeIfAbsent(valueClass, vc -> {
			checkNonBeanValue("PSet1 (LogSet1)",
					"(equals-without-hashCode misbehaves in hash set; Bean set unsupported by design)", vc);
			return new Meta1<>("LogSet1:", set1HeadHash, (Class<V>)vc);
		});
	}

	public static <V> @NotNull Meta1<V> createDynamicListMeta(@NotNull ToLongFunction<Bean> get,
															  @NotNull LongFunction<Bean> create) {
		return new Meta1<>(get, create);
	}
}
