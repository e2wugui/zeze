package Zeze.Collections;

import org.pcollections.Empty;
import org.pcollections.PMap;
import org.pcollections.PSet;
import org.pcollections.PVector;

/**
 * 持久化不可变容器后端工厂（统一对外暴露 pcollections 接口）。
 * <p>通过系统属性 {@code zeze.collections.backend} 在启动时选定实现：
 * <ul>
 *   <li>{@code myktt}（默认）：仓库内自移植不可变容器内核，批量操作走 builder；</li>
 *   <li>{@code ktt}：kotlinx 不可变容器内核，批量操作走 builder；</li>
 *   <li>{@code kt}：kotlinx 内核，批量操作退化为逐条更新；</li>
 *   <li>{@code pcollections}：原生 pcollections 实现。</li>
 * </ul>
 * 事务集合依赖“无变更操作返回原实例”的引用恒等契约，各后端均须保持一致。
 * PSortedMap 族不经此工厂，直接使用 pcollections TreePMap。
 */
public final class Persistents {
	/** 当前选定的后端名称。 */
	public static final String BACKEND = backend();

	static final boolean USE_KT = "kt".equals(BACKEND) || "ktt".equals(BACKEND);
	static final boolean USE_MYKT = "myktt".equals(BACKEND);

	/** 批量操作是否使用 builder（volatile 以便测试切换）。 */
	public static volatile boolean BULK = "ktt".equals(BACKEND) || USE_MYKT;

	private Persistents() {
	}

	private static String backend() {
		var v = System.getProperty("zeze.collections.backend", "myktt");
		if (!v.equals("pcollections") && !v.equals("kt") && !v.equals("ktt") && !v.equals("myktt"))
			throw new IllegalArgumentException("zeze.collections.backend: " + v + " (合法值: pcollections|kt|ktt|myktt)");
		return v;
	}

	public static <E> PVector<E> vector() {
		return USE_MYKT ? MyKtVector.<E>empty() : USE_KT ? KtVector.<E>empty() : Empty.vector();
	}

	public static <K, V> PMap<K, V> map() {
		return USE_MYKT ? MyKtMap.<K, V>empty() : USE_KT ? KtMap.<K, V>empty() : Empty.map();
	}

	public static <E> PSet<E> set() {
		return USE_MYKT ? MyKtSet.<E>empty() : USE_KT ? KtSet.<E>empty() : Empty.set();
	}
}
