package Benchmark;

import java.lang.management.ManagementFactory;
import java.util.HashMap;

import harness.Bench;
import kotlinx.collections.immutable.ExtensionsKt;
import kotlinx.collections.immutable.PersistentMap;
import org.junit.jupiter.api.Test;
import org.openjdk.jol.info.GraphLayout;
import org.pcollections.Empty;
import org.pcollections.PMap;

/**
 * pcollections 替代评估基准：pcollections(HashTreePMap) vs Vavr(HAMT) vs kotlinx(CHAMP)。
 *
 * 模拟 Zeze 事务写模式（LogMap1.put：每次写产生一个新版本，旧版本不驻留，即无多版本并存），
 * 对比：
 * <ul>
 *   <li>写吞吐与每次写分配字节（GC 压力）：build / overwrite(覆盖已有key) / addNew(新增key) / remove；</li>
 *   <li>读吞吐：get；</li>
 *   <li>稳态内存：JOL 深度大小（含 Integer 装箱对象，各库等量，公平）；</li>
 *   <li>no-op 引用恒等语义：Zeze 依赖 newMap != oldMap 检测无变更（如 LogMap1.putAll、PSet1.add），</li>
 * </ul>
 * 附带 java.util.HashMap 作可变原地写的下界参考（无版本化语义，不可比写场景，仅供参考）。
 *
 * 运行：gradlew :ZezeJavaTest:bench --tests "*BenchPersistentCollectionsCompare"
 */
@Bench
public class BenchPersistentCollectionsCompare {
	public static final int N = 100_000; // 基础 map 大小（条目数）
	public static final int OPS = 10_000; // 每个写场景的操作数
	public static final int GET_OPS = 1_000_000;

	/** 防止死码消除的结果汇聚点。 */
	public static volatile long sink;

	private interface Lib {
		String name();

		Object empty();

		Object put(Object map, int key, int value); // 版本化写：返回新版本

		Object remove(Object map, int key); // 版本化删除：返回新版本

		int get(Object map, int key); // 命中返回值，未命中返回 Integer.MIN_VALUE

		int size(Object map);

		/** 场景隔离用副本：持久化库 O(1) 返回自身（版本不可变），HashMap 真实拷贝。 */
		Object copy(Object map);
	}

	@SuppressWarnings("unchecked")
	private static org.pcollections.PMap<Integer, Integer> pc(Object map) {
		return (PMap<Integer, Integer>)map;
	}

	private static final Lib PCOLLECTIONS = new Lib() {
		@Override
		public String name() {
			return "pcollections";
		}

		@Override
		public Object empty() {
			return Empty.map();
		}

		@Override
		public Object put(Object map, int key, int value) {
			return pc(map).plus(key, value);
		}

		@Override
		public Object remove(Object map, int key) {
			return pc(map).minus(key);
		}

		@Override
		public int get(Object map, int key) {
			var v = pc(map).get(key);
			return v != null ? v : Integer.MIN_VALUE;
		}

		@Override
		public int size(Object map) {
			return pc(map).size();
		}

		@Override
		public Object copy(Object map) {
			return map;
		}
	};

	@SuppressWarnings("unchecked")
	private static io.vavr.collection.HashMap<Integer, Integer> vavr(Object map) {
		return (io.vavr.collection.HashMap<Integer, Integer>)map;
	}

	private static final Lib VAVR = new Lib() {
		@Override
		public String name() {
			return "Vavr";
		}

		@Override
		public Object empty() {
			return io.vavr.collection.HashMap.empty();
		}

		@Override
		public Object put(Object map, int key, int value) {
			return vavr(map).put(key, value);
		}

		@Override
		public Object remove(Object map, int key) {
			return vavr(map).remove(key);
		}

		@Override
		public int get(Object map, int key) {
			var v = vavr(map).get(key).getOrNull();
			return v != null ? v : Integer.MIN_VALUE;
		}

		@Override
		public int size(Object map) {
			return vavr(map).size();
		}

		@Override
		public Object copy(Object map) {
			return map;
		}
	};

	@SuppressWarnings("unchecked")
	private static PersistentMap<Integer, Integer> kx(Object map) {
		return (PersistentMap<Integer, Integer>)map;
	}

	private static final Lib KOTLINX = new Lib() {
		@Override
		public String name() {
			return "kotlinx";
		}

		@Override
		public Object empty() {
			PersistentMap<Integer, Integer> empty = ExtensionsKt.persistentHashMapOf();
			return empty;
		}

		@Override
		public Object put(Object map, int key, int value) {
			return kx(map).put(key, value);
		}

		@Override
		public Object remove(Object map, int key) {
			return kx(map).remove(key);
		}

		@Override
		public int get(Object map, int key) {
			var v = kx(map).get(key);
			return v != null ? v : Integer.MIN_VALUE;
		}

		@Override
		public int size(Object map) {
			return kx(map).size();
		}

		@Override
		public Object copy(Object map) {
			return map;
		}
	};

	/** 原地写、无版本化语义，仅作吞吐/内存下界参考。 */
	private static final Lib HASHMAP = new Lib() {
		@SuppressWarnings("unchecked")
		private HashMap<Integer, Integer> m(Object map) {
			return (HashMap<Integer, Integer>)map;
		}

		@Override
		public String name() {
			return "HashMap参考";
		}

		@Override
		public Object empty() {
			return new HashMap<Integer, Integer>();
		}

		@Override
		public Object put(Object map, int key, int value) {
			m(map).put(key, value);
			return map; // 原地修改
		}

		@Override
		public Object remove(Object map, int key) {
			m(map).remove(key);
			return map;
		}

		@Override
		public int get(Object map, int key) {
			var v = m(map).get(key);
			return v != null ? v : Integer.MIN_VALUE;
		}

		@Override
		public int size(Object map) {
			return m(map).size();
		}

		@Override
		public Object copy(Object map) {
			return new HashMap<>(m(map));
		}
	};

	private static final com.sun.management.ThreadMXBean THREAD_ALLOC =
			(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();

	private static long allocated() {
		return THREAD_ALLOC.getThreadAllocatedBytes(Thread.currentThread().getId());
	}

	private void report(Lib lib, String scenario, int ops, long nanos, long bytes) {
		System.out.printf("%-12s %-10s %,12d ops  %,10.1f ns/op  %,10.1f B/op%n",
				lib.name(), scenario, ops, (double)nanos / ops, (double)bytes / ops);
	}

	/** 构建基础 map（也充当整体预热）。 */
	private Object build(Lib lib, int count) {
		var map = lib.empty();
		for (int i = 0; i < count; i++)
			map = lib.put(map, i, i);
		return map;
	}

	@Test
	public void compare() {
		System.out.printf("JVM=%s maxHeap=%,dMB N=%,d OPS=%,d%n%n",
				System.getProperty("java.vm.version"), Runtime.getRuntime().maxMemory() >> 20, N, OPS);

		for (var lib : new Lib[] { PCOLLECTIONS, VAVR, KOTLINX, HASHMAP }) {
			// 预热：构建一次全量 base（JIT 编译 put 路径）
			var base = build(lib, N);
			if (lib.size(base) != N)
				throw new AssertionError(lib.name() + " build size=" + lib.size(base));

			// build：从空开始逐条 put（decode/装载路径），预热一次后测量第二次
			var warm = build(lib, N);
			if (lib.size(warm) != N)
				throw new AssertionError(lib.name() + " warm build size=" + lib.size(warm));
			var map = lib.empty();
			long a0 = allocated();
			long t0 = System.nanoTime();
			for (int i = 0; i < N; i++)
				map = lib.put(map, i, i);
			report(lib, "build", N, System.nanoTime() - t0, allocated() - a0);

			// overwrite：覆盖已有 key（LogMap1.put 典型路径）
			map = lib.copy(base);
			for (int i = 0; i < OPS; i++) // 预热
				map = lib.put(map, (i * 31 + 7) % N, i);
			map = lib.copy(base);
			a0 = allocated();
			t0 = System.nanoTime();
			for (int i = 0; i < OPS; i++)
				map = lib.put(map, (i * 31 + 7) % N, i);
			report(lib, "overwrite", OPS, System.nanoTime() - t0, allocated() - a0);
			if (lib.size(map) != N)
				throw new AssertionError(lib.name() + " overwrite size=" + lib.size(map));

			// addNew：新增 key
			map = lib.copy(base);
			for (int i = 0; i < OPS; i++)
				map = lib.put(map, N + i, i);
			map = lib.copy(base);
			a0 = allocated();
			t0 = System.nanoTime();
			for (int i = 0; i < OPS; i++)
				map = lib.put(map, N + i, i);
			report(lib, "addNew", OPS, System.nanoTime() - t0, allocated() - a0);
			if (lib.size(map) != N + OPS)
				throw new AssertionError(lib.name() + " addNew size=" + lib.size(map));

			// remove：删除已有 key
			map = lib.copy(base);
			for (int i = 0; i < OPS; i++)
				map = lib.remove(map, i);
			map = lib.copy(base);
			a0 = allocated();
			t0 = System.nanoTime();
			for (int i = 0; i < OPS; i++)
				map = lib.remove(map, i);
			report(lib, "remove", OPS, System.nanoTime() - t0, allocated() - a0);
			if (lib.size(map) != N - OPS)
				throw new AssertionError(lib.name() + " remove size=" + lib.size(map));

			// get：读吞吐
			var localSink = 0;
			for (int i = 0; i < GET_OPS / 10; i++) // 预热
				localSink += lib.get(base, (i * 31 + 7) % N);
			localSink = 0;
			a0 = allocated();
			t0 = System.nanoTime();
			for (int i = 0; i < GET_OPS; i++)
				localSink += lib.get(base, (i * 31 + 7) % N);
			report(lib, "get", GET_OPS, System.nanoTime() - t0, allocated() - a0);
			sink += localSink;

			// 稳态内存：JOL 深度大小（含 Integer 装箱，各库等量）
			var bytes = GraphLayout.parseInstance(base).totalSize();
			System.out.printf("%-12s %-10s %,15d bytes  %,8.1f B/entry (含Integer装箱)%n%n",
					lib.name(), "footprint", bytes, (double)bytes / N);
		}

		noOpIdentity();
	}

	/**
	 * no-op 引用恒等：Zeze 的 LogMap1.putAll / LogList1.addAll / PSet1.add 依赖
	 * "无变更操作返回原实例"来廉价检测无变化。pcollections 有此行为；其余库观察确认。
	 */
	private void noOpIdentity() {
		System.out.println("no-op 引用恒等（==原实例）:");
		for (var lib : new Lib[] { PCOLLECTIONS, VAVR, KOTLINX }) {
			var base = build(lib, 1_000);
			var putSameValue = lib.put(base, 5, 5) == base; // key=5 已存在且值相同
			var removeAbsent = lib.remove(base, 999_999) == base; // 不存在的 key
			System.out.printf("%-12s put(相同值)==this: %-5s  remove(不存在)==this: %-5s%n",
					lib.name(), putSameValue, removeAbsent);
		}
	}
}
