package Zeze.Transaction.Collections;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;
import Zeze.Transaction.Bean;
import org.jetbrains.annotations.NotNull;

/**
 * LogMap2 家族（PMap2/GTable2）元数据：Bean 值受管，支持原位修改。
 */
public final class Map2Meta<K, V> extends Meta2<K, V> {
	// 线上typeId原料（hashLog输入），逐字保留，勿改。
	static final String HEAD = "Zeze.Transaction.Collections.LogMap2<";
	private static final long headHash = Bean.hash64(HEAD);
	private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<Class<?>, Map2Meta<?, ?>>> metas
			= new ConcurrentHashMap<>();

	Map2Meta(@NotNull Class<K> keyClass, @NotNull Class<V> valueClass) {
		super("LogMap2:", headHash, keyClass, valueClass);
	}

	Map2Meta(@NotNull Class<K> keyClass, @NotNull Class<V> valueClass, @NotNull Supplier<V> valueCtor) {
		super("LogMap2:", headHash, keyClass, valueClass, valueCtor);
	}

	Map2Meta(@NotNull Class<K> keyClass, @NotNull ToLongFunction<Bean> get, @NotNull LongFunction<Bean> create) {
		super("LogMap2:", headHash, keyClass, get, create);
	}

	// coll-01根治：GTable外层专用。家族头/name前缀由调用方提供（GTable1/GTable2各自常量，
	// 与PMap2的LogMap2命名空间分流），valueIdentity为完整列/值身份串。
	Map2Meta(@NotNull String familyHead, long familyHeadHash, @NotNull String namePrefix,
			 @NotNull Class<K> keyClass, @NotNull Class<V> valueClass,
			 @NotNull String valueIdentity, @NotNull Supplier<V> valueCtor) {
		super(namePrefix, familyHeadHash, keyClass, valueClass, valueIdentity, valueCtor);
	}

	/**
	 * 获取（或首次创建并缓存）共享的 Map2 元数据。
	 * 契约：{@link #create} 的自定义 valueCtor 与本方法的默认构造对同一
	 * (keyClass, valueClass) 生成相同 logTypeId（typeId 由 head+两类名散列，与ctor无关），
	 * Log.register 按 typeId 先到先得——ctor 产物若与默认构造产物不等价，后注册方的差异
	 * 在日志反序列化（Log.create 按 typeId 查表）中永远不生效。
	 */
	@SuppressWarnings("unchecked")
	public static <K, V extends Bean> @NotNull Map2Meta<K, V> get(@NotNull Class<K> keyClass,
	                                                              @NotNull Class<V> valueClass) {
		var map = metas.computeIfAbsent(keyClass, kc -> {
			checkNonBeanKey("PMap2/GTable2 (LogMap2)", kc);
			return new ConcurrentHashMap<>();
		});
		var r = map.get(valueClass);
		if (r != null)
			return (Map2Meta<K, V>)r;
		return (Map2Meta<K, V>)map.computeIfAbsent(valueClass,
				vc -> new Map2Meta<>(keyClass, (Class<V>)vc));
	}

	/**
	 * 用自定义 valueCtor 创建 Map2 元数据（不进共享缓存，调用方自行注册）。
	 * 契约：valueCtor 产物必须与 valueClass 默认构造产物（new V()）等价——本方法与
	 * {@link #get} 对同一 (keyClass, valueClass) 算出相同 logTypeId，注册进
	 * Log 工厂表后 typeId 先到先得，两者不等价时后注册方的 ctor 被静默忽略，日志
	 * 反序列化拿到错误构造的实例（如 History 增量回放数据损坏）。
	 */
	public static <K, V extends Bean> @NotNull Map2Meta<K, V> create(@NotNull Class<K> keyClass,
	                                                                  @NotNull Class<V> valueClass,
	                                                                  @NotNull Supplier<V> valueCtor) {
		checkNonBeanKey("PMap2/GTable2 (LogMap2)", keyClass);
		return new Map2Meta<>(keyClass, valueClass, valueCtor);
	}

	public static <K, V extends Bean> @NotNull Map2Meta<K, V> createDynamic(@NotNull Class<K> keyClass,
	                                                                         @NotNull ToLongFunction<Bean> get,
	                                                                         @NotNull LongFunction<Bean> create) {
		checkNonBeanKey("PMap2/GTable2 (LogMap2)", keyClass);
		return new Map2Meta<>(keyClass, get, create);
	}

	/**
	 * GTable外层meta构造（coll-01根治）：valueClass是擦除的共享类（BeanMap1/BeanMap2.class），
	 * 真实列/值类型活在闭包——logTypeId/name由valueIdentity完整身份参与，同keyClass不同
	 * 列/值类型的GTable不再共享typeId（Log.register先到先得+解码端按typeId全局查表）。
	 * 不进共享缓存，调用方按(row,col,val)自行缓存。wire兼容由调用方裁定（History无生产
	 * 启用时可直接切换）。
	 */
	public static <K, V extends Bean> @NotNull Map2Meta<K, V> createWithFamily(
			@NotNull String familyHead, @NotNull String namePrefix,
			@NotNull Class<K> keyClass, @NotNull Class<V> valueClass,
			@NotNull String valueIdentity, @NotNull Supplier<V> valueCtor) {
		checkNonBeanKey("GTable outer (" + namePrefix + ")", keyClass);
		return new Map2Meta<>(familyHead, Bean.hash64(familyHead), namePrefix, keyClass, valueClass, valueIdentity, valueCtor);
	}
}
