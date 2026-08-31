package Benchmark;

import java.lang.management.ManagementFactory;
import java.util.Random;
import java.util.TreeMap;

import harness.Bench;
import org.junit.jupiter.api.Test;
import org.openjdk.jol.info.GraphLayout;
import org.pcollections.Empty;
import org.pcollections.PSortedMap;

/**
 * pcollections 替代评估基准（SortedMap 篇）：pcollections(TreePMap 红黑树) vs Vavr(TreeMap 红黑树)。
 *
 * kotlinx 无持久化有序 Map 实现（persistentHashMapOf 为无序 CHAMP），跳过。
 * 为 SortedMap 替换决策（当前结论：仅 SortedMap 保留 pcollections）提供数据。
 *
 * 模拟 Zeze 事务写模式（每次写产生一个新版本，旧版本不驻留），场景：
 * <ul>
 *   <li>build：顺序 key 0..N-1 逐条插入（decode/装载路径，与 map 基准可对照）；</li>
 *   <li>buildRandom：固定种子 Fisher-Yates 洗牌后的 key 序列逐条插入（树结构的典型对抗序）；</li>
 *   <li>overwrite：覆盖已有 key；addNew：新增 key；remove：前缀连续删除；</li>
 *   <li>get：随机命中读；firstLast：firstKey+lastKey（有序 map 特征操作 min/max）；</li>
 *   <li>iterate：按 key 升序整表遍历（ns/op 为每元素）；</li>
 *   <li>footprint：JOL 深度大小（含 Integer 装箱，各库等量）；</li>
 *   <li>no-op 引用恒等：Zeze 依赖 newMap != oldMap 检测无变更。</li>
 * </ul>
 * 附带 java.util.TreeMap 作可变原地写的下界参考（无版本化语义，仅供参考）。
 *
 * 运行：gradlew :ZezeJavaTest:bench --tests "*BenchPersistentSortedMapCompare"
 */
@Bench
public class BenchPersistentSortedMapCompare {
	public static final int N = 100_000; // 基础 map 大小（条目数）
	public static final int OPS = 10_000; // 每个写场景的操作数
	public static final int GET_OPS = 1_000_000;
	public static final int ITER_PASSES = 20; // 遍历场景的整表趟数

	/** 洗牌序列固定种子（预热/测量共用同一序列）。 */
	public static final long SHUFFLE_SEED = 20260831;

	/** 固定种子 Fisher-Yates 洗牌的 0..N-1 序列（树结构的典型对抗序）。 */
	private static final int[] SHUFFLED_KEYS = shuffledKeys(N, SHUFFLE_SEED);

	/** 防止死码消除的结果汇聚点。 */
	public static volatile long sink;

	private interface Lib {
		String name();

		Object empty();

		Object put(Object map, int key, int value); // 版本化写：返回新版本

		Object remove(Object map, int key); // 版本化删除：返回新版本

		int get(Object map, int key); // 命中返回值，未命中返回 Integer.MIN_VALUE

		int size(Object map);

		/** 场景隔离用副本：持久化库 O(1) 返回自身（版本不可变），TreeMap 真实拷贝。 */
		Object copy(Object map);

		int firstKey(Object map); // 最小 key

		int lastKey(Object map); // 最大 key

		/** 按 key 升序的 key 集合。 */
		Iterable<Integer> keys(Object map);
	}

	@SuppressWarnings("unchecked")
	private static PSortedMap<Integer, Integer> pc(Object map) {
		return (PSortedMap<Integer, Integer>)map;
	}

	private static final Lib PCOLLECTIONS = new Lib() {
		@Override
		public String name() {
			return "pcollections";
		}

		@Override
		public Object empty() {
			return Empty.<Integer, Integer>sortedMap(); // TreePMap（红黑树，自然序）
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

		@Override
		public int firstKey(Object map) {
			return pc(map).firstKey();
		}

		@Override
		public int lastKey(Object map) {
			return pc(map).lastKey();
		}

		@Override
		public Iterable<Integer> keys(Object map) {
			return pc(map).keySet();
		}
	};

	@SuppressWarnings("unchecked")
	private static io.vavr.collection.TreeMap<Integer, Integer> vavr(Object map) {
		return (io.vavr.collection.TreeMap<Integer, Integer>)map;
	}

	private static final Lib VAVR = new Lib() {
		@Override
		public String name() {
			return "Vavr";
		}

		@Override
		public Object empty() {
			return io.vavr.collection.TreeMap.<Integer, Integer>empty(); // 红黑树，自然序
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

		@Override
		public int firstKey(Object map) {
			return vavr(map).head()._1;
		}

		@Override
		public int lastKey(Object map) {
			return vavr(map).last()._1;
		}

		@Override
		public Iterable<Integer> keys(Object map) {
			return vavr(map).keySet();
		}
	};

	/** 原地写、无版本化语义，仅作吞吐/内存下界参考。 */
	private static final Lib TREEMAP = new Lib() {
		@SuppressWarnings("unchecked")
		private TreeMap<Integer, Integer> m(Object map) {
			return (TreeMap<Integer, Integer>)map;
		}

		@Override
		public String name() {
			return "TreeMap参考";
		}

		@Override
		public Object empty() {
			return new TreeMap<Integer, Integer>();
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
			return new TreeMap<>(m(map));
		}

		@Override
		public int firstKey(Object map) {
			return m(map).firstKey();
		}

		@Override
		public int lastKey(Object map) {
			return m(map).lastKey();
		}

		@Override
		public Iterable<Integer> keys(Object map) {
			return m(map).keySet();
		}
	};

	private static final com.sun.management.ThreadMXBean THREAD_ALLOC =
			(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();

	private static long allocated() {
		return THREAD_ALLOC.getThreadAllocatedBytes(Thread.currentThread().getId());
	}

	private void report(Lib lib, String scenario, int ops, long nanos, long bytes) {
		System.out.printf("%-12s %-13s %,12d ops  %,10.1f ns/op  %,10.1f B/op%n",
				lib.name(), scenario, ops, (double)nanos / ops, (double)bytes / ops);
	}

	/** 固定种子 Fisher-Yates 洗牌的 0..n-1 序列。 */
	private static int[] shuffledKeys(int n, long seed) {
		var keys = new int[n];
		for (var i = 0; i < n; i++)
			keys[i] = i;
		var rnd = new Random(seed);
		for (var i = n - 1; i > 0; i--) {
			var j = rnd.nextInt(i + 1);
			var t = keys[i];
			keys[i] = keys[j];
			keys[j] = t;
		}
		return keys;
	}

	/** 构建基础 map：顺序 key 0..count-1（也充当整体预热）。 */
	private Object build(Lib lib, int count) {
		var map = lib.empty();
		for (int i = 0; i < count; i++)
			map = lib.put(map, i, i);
		return map;
	}

	/** 以洗牌序构建基础 map（buildRandom 场景的构建路径）。 */
	private Object buildRandom(Lib lib) {
		var map = lib.empty();
		for (int i = 0; i < N; i++)
			map = lib.put(map, SHUFFLED_KEYS[i], i);
		return map;
	}

	@Test
	public void compare() {
		System.out.printf("JVM=%s maxHeap=%,dMB N=%,d OPS=%,d ITER_PASSES=%d SHUFFLE_SEED=%d%n",
				System.getProperty("java.vm.version"), Runtime.getRuntime().maxMemory() >> 20,
				N, OPS, ITER_PASSES, SHUFFLE_SEED);
		System.out.println("kotlinx: 跳过 —— 无持久化有序 Map 实现（persistentHashMapOf 为无序 CHAMP）\n");

		for (var lib : new Lib[] { PCOLLECTIONS, VAVR, TREEMAP }) {
			// 预热：构建一次全量 base（JIT 编译 put 路径）
			var base = build(lib, N);
			if (lib.size(base) != N)
				throw new AssertionError(lib.name() + " build size=" + lib.size(base));

			// build：从空开始顺序 key 逐条 put，预热一次后测量第二次
			var warm = build(lib, N);
			if (lib.size(warm) != N)
				throw new AssertionError(lib.name() + " warm build size=" + lib.size(warm));
			var map = lib.empty();
			long a0 = allocated();
			long t0 = System.nanoTime();
			for (int i = 0; i < N; i++)
				map = lib.put(map, i, i);
			report(lib, "build", N, System.nanoTime() - t0, allocated() - a0);
			if (lib.size(map) != N)
				throw new AssertionError(lib.name() + " build size=" + lib.size(map));

			// buildRandom：洗牌序逐条插入（树结构的典型对抗序，预热/测量共用同一序列）
			var warmRandom = buildRandom(lib);
			if (lib.size(warmRandom) != N)
				throw new AssertionError(lib.name() + " warm buildRandom size=" + lib.size(warmRandom));
			map = lib.empty();
			a0 = allocated();
			t0 = System.nanoTime();
			for (int i = 0; i < N; i++)
				map = lib.put(map, SHUFFLED_KEYS[i], i);
			report(lib, "buildRandom", N, System.nanoTime() - t0, allocated() - a0);
			if (lib.size(map) != N)
				throw new AssertionError(lib.name() + " buildRandom size=" + lib.size(map));

			// overwrite：覆盖已有 key
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

			// remove：删除已有 key（前缀连续删除）
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

			// firstLast：firstKey+lastKey 循环（有序 map 特征操作 min/max）
			localSink = 0;
			for (int i = 0; i < GET_OPS / 10; i++) // 预热
				localSink += lib.firstKey(base) + lib.lastKey(base);
			localSink = 0;
			a0 = allocated();
			t0 = System.nanoTime();
			for (int i = 0; i < GET_OPS; i++)
				localSink += lib.firstKey(base) + lib.lastKey(base);
			report(lib, "firstLast", GET_OPS, System.nanoTime() - t0, allocated() - a0);
			sink += localSink;

			// iterate：按 key 升序整表遍历（ns/op 为每元素耗时）
			localSink = 0;
			for (var k : lib.keys(base)) // 预热
				localSink += k;
			localSink = 0;
			int elementVisits = N * ITER_PASSES;
			a0 = allocated();
			t0 = System.nanoTime();
			for (int p = 0; p < ITER_PASSES; p++)
				for (var k : lib.keys(base))
					localSink += k;
			report(lib, "iterate", elementVisits, System.nanoTime() - t0, allocated() - a0);
			sink += localSink;

			// 稳态内存：JOL 深度大小（含 Integer 装箱，各库等量）
			var bytes = GraphLayout.parseInstance(base).totalSize();
			System.out.printf("%-12s %-13s %,15d bytes  %,8.1f B/entry (含Integer装箱)%n%n",
					lib.name(), "footprint", bytes, (double)bytes / N);
		}

		noOpIdentity();
	}

	/**
	 * no-op 引用恒等：Zeze 依赖 "无变更操作返回原实例"（如 LogSortedMap1.putAll）廉价检测无变化。
	 * pcollections 有此行为；其余库观察确认。
	 */
	private void noOpIdentity() {
		System.out.println("no-op 引用恒等（==原实例）:");
		for (var lib : new Lib[] { PCOLLECTIONS, VAVR }) {
			var base = build(lib, 1_000);
			var putSameValue = lib.put(base, 5, 5) == base; // key=5 已存在且值相同
			var removeAbsent = lib.remove(base, 999_999) == base; // 不存在的 key
			System.out.printf("%-12s put(相同值)==this: %-5s  remove(不存在)==this: %-5s%n",
					lib.name(), putSameValue, removeAbsent);
		}
	}
}
