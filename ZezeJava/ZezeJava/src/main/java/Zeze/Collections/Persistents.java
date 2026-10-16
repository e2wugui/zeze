package Zeze.Collections;

import org.pcollections.Empty;
import org.pcollections.PMap;
import org.pcollections.PSet;
import org.pcollections.PVector;

/**
 * 持久化容器后端工厂（pcollections 接口形态不变，内核可切换）。
 *
 * <p>后端经系统属性 {@code zeze.collections.backend} 在 JVM 启动时选定一次（非法值快速失败，
 * 防止 bench 拼错静默跑错档）：
 * <ul>
 *   <li>{@code ktt}（默认，2026-10-16 裁定）：kotlinx 内核 + 批量 op 走 builder（变更跟踪保恒等：
 *       全部 no-op 时返回原实例）。三后端对照 GameSim 驻留 -27%、批量写 p50 -64%、冷装载 -45%、
 *       CPU 无回归（BENCHMARK.md"持久化容器后端对照"节）；</li>
 *   <li>{@code kt}：kotlinx 内核纯持久化单 op 路径，批量 op 为单 op 循环（no-op 引用恒等由单 op
 *       语义传导）；</li>
 *   <li>{@code pcollections}：回退杠杆（逃生门）——原样返回 pcollections 实现
 *       （TreePVector/HashPMap/MapPSet），行为与历史版本完全一致，零包装开销。</li>
 * </ul>
 *
 * <p>内核 kotlinx.collections.immutable 0.5.2（2026-10-16 升级裁定，生产不开 -ea 时与 0.3.7
 * 持平或略优；其热路径 assert 仅在 -ea 车道付出 ~2× 容器热路径代价，详见 BENCHMARK.md
 * "持久化容器后端对照"节；适配器用旧名 API——0.5.x 的新名 putting 系委托旧名，跨版兼容）。
 *
 * <p>事务集合（Zeze.Transaction.Collections）的写路径依赖"无变更操作返回同一实例"（{@code !=}
 * 判定）检测无变化，三个后端都必须保持该契约（回归测试钉住）。PSortedMap 族不经此工厂，
 * 永久保留 pcollections TreePMap。
 */
public final class Persistents {
	/** 当前后端名（系统属性原样留档，便于诊断输出对账）。 */
	public static final String BACKEND = backend();

	/** 是否使用 kotlinx 内核（kt 与 ktt 共用适配类）。 */
	static final boolean USE_KT = !"pcollections".equals(BACKEND);

	/**
	 * 批量 op 是否走 builder（ktt 档启动时定型）。volatile 非 final：契约测试在单 JVM 内
	 * 翻档覆盖循环/builder 双路径用（bench 各后端独立进程，不依赖运行时翻转）。
	 */
	public static volatile boolean BULK = "ktt".equals(BACKEND);

	private Persistents() {
	}

	private static String backend() {
		var v = System.getProperty("zeze.collections.backend", "ktt");
		if (!v.equals("pcollections") && !v.equals("kt") && !v.equals("ktt"))
			throw new IllegalArgumentException("zeze.collections.backend: " + v + " (合法值: pcollections|kt|ktt)");
		return v;
	}

	public static <E> PVector<E> vector() {
		return USE_KT ? KtVector.empty() : Empty.vector();
	}

	public static <K, V> PMap<K, V> map() {
		return USE_KT ? KtMap.empty() : Empty.map();
	}

	public static <E> PSet<E> set() {
		return USE_KT ? KtSet.empty() : Empty.set();
	}
}
