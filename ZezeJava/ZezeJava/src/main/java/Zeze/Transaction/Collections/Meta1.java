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
	public final BiConsumer<ByteBuffer, V> valueEncoder; // 只用于非Bean类型
	public final Function<IByteBuffer, V> valueDecoder; // 只用于非Bean类型
	public final MethodHandle valueFactory; // 只用于Bean类型

	private Meta1(long headHash, @NotNull Class<V> valueClass) {
		logTypeId = Bean.hashLog(headHash, valueClass);
		var valueCodecFuncs = SerializeHelper.createCodec(valueClass);
		valueEncoder = valueCodecFuncs.encoder;
		valueDecoder = valueCodecFuncs.decoder;
		valueFactory = Bean.class.isAssignableFrom(valueClass) ? Reflect.getDefaultConstructor(valueClass) : null;
	}

	private Meta1(@NotNull ToLongFunction<Bean> get, @NotNull LongFunction<Bean> create) {
		logTypeId = dynamicBeanTypeId;
		valueEncoder = null;
		valueDecoder = null;
		valueFactory = SerializeHelper.createDynamicFactory(get, create);
	}

	@SuppressWarnings("unchecked")
	public static <V extends Bean> @NotNull Meta1<V> getLogOneMeta(@NotNull Class<V> beanClass) {
		return (Meta1<V>)logOneMetas.computeIfAbsent(beanClass, vc -> new Meta1<>(logOneHeadHash, (Class<V>)vc));
	}

	@SuppressWarnings("unchecked")
	public static <V extends Serializable> @NotNull Meta1<V> getBeanMeta(@NotNull Class<V> beanClass) {
		return (Meta1<V>)beanMetas.computeIfAbsent(beanClass, vc -> new Meta1<>(beanHeadHash, (Class<V>)vc));
	}

	@SuppressWarnings("unchecked")
	static <V> @NotNull Meta1<V> getList1Meta(@NotNull Class<V> valueClass) {
		return (Meta1<V>)list1Metas.computeIfAbsent(valueClass, vc -> new Meta1<>(list1HeadHash, (Class<V>)vc));
	}

	@SuppressWarnings("unchecked")
	public static <V extends Bean> @NotNull Meta1<V> getList2Meta(@NotNull Class<V> valueClass) {
		return (Meta1<V>)list2Metas.computeIfAbsent(valueClass, vc -> new Meta1<>(list2HeadHash, (Class<V>)vc));
	}

	@SuppressWarnings("unchecked")
	static <V> @NotNull Meta1<V> getSet1Meta(@NotNull Class<V> valueClass) {
		return (Meta1<V>)set1Metas.computeIfAbsent(valueClass, vc -> new Meta1<>(set1HeadHash, (Class<V>)vc));
	}

	static <V> @NotNull Meta1<V> createDynamicListMeta(@NotNull ToLongFunction<Bean> get, @NotNull LongFunction<Bean> create) {
		return new Meta1<>(get, create);
	}
}
