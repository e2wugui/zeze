package Zeze.Transaction.Collections;

import java.lang.invoke.MethodHandle;
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

// 家族封闭：sealed+包私有构造器，子类各自硬编码家族头串（线上typeId原料，逐字保留）；
// 容器层（P*）构造器只收对应子类型，日志层（Log*）等家族无关消费方收本基类。
// 勿再引入收基类的容器构造器——跨家族meta会使读端解出别家Log，followerApply强转CCE。
public sealed abstract class Meta1<V> permits BeanKeyMeta, List1Meta, List2Meta, LogOneMeta, Set1Meta {
	// LogList2<DynamicBean> 的固定线上typeId（仅dynamic构造器使用）。
	static final int dynamicBeanTypeId = Bean.hash32("Zeze.Transaction.Collections.LogList2<Zeze.Transaction.DynamicBean>");

	public final int logTypeId;
	public final int valueEncodeType;
	public final BiConsumer<ByteBuffer, V> valueEncoder; // 只用于非Bean类型
	public final Function<IByteBuffer, V> valueDecoder; // 只用于非Bean类型
	public final SerializeHelper.ObjectIntFunction<IByteBuffer, V> valueDecoderWithType; // 只用于非Bean类型
	public final MethodHandle valueFactory; // 只用于Bean类型
	public final @NotNull String name; // 主要用于分析查错

	Meta1(@NotNull String headStr, long headHash, @NotNull Class<V> valueClass) {
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

	Meta1(@NotNull ToLongFunction<Bean> get, @NotNull LongFunction<Bean> create) {
		logTypeId = dynamicBeanTypeId;
		valueEncodeType = IByteBuffer.DYNAMIC;
		valueEncoder = null;
		valueDecoder = null;
		valueDecoderWithType = null;
		valueFactory = SerializeHelper.createDynamicFactory(get, create);
		name = "LogList2:DynamicBean";
	}

	// 1系容器不支持Bean值，工厂层是唯一拦截点：List1按值拷贝记账、不挂接rootInfo，装入的bean
	// 永不受管，原位修改静默丢失；Set1是哈希语义——Bean值语义equals配身份hashCode（可变bean
	// 不覆写hashCode防哈希漂移），去重/remove/removeAll失真，且框架设计上无PSet2。
	// check在computeIfAbsent内按miss执行：不变量是"被拒类型永不在缓存中"——任何新的
	// 缓存插入路径必须经由本check，不允许绕过直插。
	static void checkNonBeanValue(@NotNull String family, @NotNull String reason, @NotNull Class<?> valueClass) {
		if (Bean.class.isAssignableFrom(valueClass))
			throw new IllegalArgumentException(
					family + " does not support Bean value type " + reason + ": " + valueClass.getName());
	}
}
