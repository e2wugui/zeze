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

	@SuppressWarnings("unchecked")
	public static <V> @NotNull Meta1<V> getList1Meta(@NotNull Class<V> valueClass) {
		return (Meta1<V>)list1Metas.computeIfAbsent(valueClass, vc -> {
			// Bean值不支持（FND7-09，PSet1/PMap1判例同族）：1系容器按值拷贝记账，不挂接
			// rootInfo（对比PList2.add的initRootInfoWithRedo），装入的bean永不受管——原位
			// 修改走非受管直写分支，不产生日志，提交后静默丢失。显式失败优于静默丢数据。
			if (Bean.class.isAssignableFrom(vc)) {
				throw new IllegalArgumentException(
						"List1Meta does not support Bean value type (in-place modifications never managed, silently lost): "
								+ vc.getName());
			}
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
			// Bean元素不支持（FND5-44同族复审）：Bean是值语义equals但身份hashCode（可变bean
			// 不覆写hashCode防哈希漂移），哈希容器对bean元素静默漏命中——去重/remove/removeAll
			// 失真。显式失败优于静默错；框架无PSet2，bean集合属设计不支持（LogList2用IdentityHashSet
			// +身份比较是既有约定，见LogList2）。
			if (Bean.class.isAssignableFrom(vc)) {
				throw new IllegalArgumentException(
						"Set1Meta does not support Bean value type (equals-without-hashCode misbehaves in hash set): "
								+ vc.getName());
			}
			return new Meta1<>("LogSet1:", set1HeadHash, (Class<V>)vc);
		});
	}

	public static <V> @NotNull Meta1<V> createDynamicListMeta(@NotNull ToLongFunction<Bean> get,
															  @NotNull LongFunction<Bean> create) {
		return new Meta1<>(get, create);
	}
}
