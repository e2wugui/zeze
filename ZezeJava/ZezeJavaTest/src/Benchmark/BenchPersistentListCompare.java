package Benchmark;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import harness.Bench;
import kotlinx.collections.immutable.ExtensionsKt;
import kotlinx.collections.immutable.PersistentList;
import org.junit.jupiter.api.Test;
import org.openjdk.jol.info.GraphLayout;
import org.pcollections.Empty;
import org.pcollections.PVector;

/**
 * pcollections 替代评估基准（List 篇）：pcollections(TreePVector 指尖树) vs Vavr(Vector 位图trie)
 * vs kotlinx(PersistentList/PersistentVector)。
 *
 * 模拟 Zeze 事务写模式（LogList1：每次写产生一个新版本，旧版本不驻留），场景对齐其 OpLog 路径：
 * <ul>
 *   <li>build：尾部逐条追加（decode/装载路径，对应 add 不断 append）；</li>
 *   <li>append：尾部追加（LogList1.add(item) → plus(item)）；</li>
 *   <li>insertMiddle：中部插入（LogList1.add(index, item) → plus(index, item)）；</li>
 *   <li>set：按下标改写（LogList1.set → with(index, item)）；</li>
 *   <li>removeMiddle：按下标中部删除（LogList1.remove(index) → minus(index)，一般情形，
 *       Zeze 是先 indexOf 再 remove，故省略 indexOf 扫描只测 remove 本身）；</li>
 *   <li>get：按下标随机读；iterate：整表遍历（ns/op 为每元素）；</li>
 *   <li>footprint：JOL 深度大小（含 Integer 装箱，各库等量）；</li>
 *   <li>no-op 引用恒等：LogList1.addAll 依赖 plusAll(空集合)返回原实例来检测无变更。</li>
 * </ul>
 * 附带 java.util.ArrayList 作可变原地写的下界参考（无版本化语义，仅供参考）。
 *
 * 运行：gradlew :ZezeJavaTest:bench --tests "*BenchPersistentListCompare"
 */
@Bench
public class BenchPersistentListCompare {
	public static final int N = 100_000; // 基础 list 大小（元素数）
	public static final int OPS = 10_000; // 每个写场景的操作数
	public static final int GET_OPS = 1_000_000;
	public static final int ITER_PASSES = 20; // 遍历场景的整表趟数

	/** 防止死码消除的结果汇聚点。 */
	public static volatile long sink;

	private interface Lib {
		String name();

		Object empty();

		Object append(Object list, int value); // 尾部追加，返回新版本

		Object insert(Object list, int index, int value); // 任意位置插入，返回新版本

		Object set(Object list, int index, int value); // 按下标改写，返回新版本

		Object removeAt(Object list, int index); // 按下标删除，返回新版本

		int get(Object list, int index);

		int size(Object list);

		/** 场景隔离用副本：持久化库 O(1) 返回自身（版本不可变），ArrayList 真实拷贝。 */
		Object copy(Object list);

		/** no-op 检查：追加空集合（LogList1.addAll 依赖返回原实例）。 */
		Object addAllEmpty(Object list);
	}

	@SuppressWarnings("unchecked")
	private static PVector<Integer> pv(Object list) {
		return (PVector<Integer>)list;
	}

	private static final Lib PCOLLECTIONS = new Lib() {
		@Override
		public String name() {
			return "pcollections";
		}

		@Override
		public Object empty() {
			return Empty.vector();
		}

		@Override
		public Object append(Object list, int value) {
			return pv(list).plus(value);
		}

		@Override
		public Object insert(Object list, int index, int value) {
			return pv(list).plus(index, value);
		}

		@Override
		public Object set(Object list, int index, int value) {
			return pv(list).with(index, value);
		}

		@Override
		public Object removeAt(Object list, int index) {
			return pv(list).minus(index);
		}

		@Override
		public int get(Object list, int index) {
			return pv(list).get(index);
		}

		@Override
		public int size(Object list) {
			return pv(list).size();
		}

		@Override
		public Object copy(Object list) {
			return list;
		}

		@Override
		public Object addAllEmpty(Object list) {
			return pv(list).plusAll(List.of());
		}
	};

	@SuppressWarnings("unchecked")
	private static io.vavr.collection.Vector<Integer> vv(Object list) {
		return (io.vavr.collection.Vector<Integer>)list;
	}

	private static final Lib VAVR = new Lib() {
		@Override
		public String name() {
			return "Vavr";
		}

		@Override
		public Object empty() {
			return io.vavr.collection.Vector.empty();
		}

		@Override
		public Object append(Object list, int value) {
			return vv(list).append(value);
		}

		@Override
		public Object insert(Object list, int index, int value) {
			return vv(list).insert(index, value);
		}

		@Override
		public Object set(Object list, int index, int value) {
			return vv(list).update(index, value);
		}

		@Override
		public Object removeAt(Object list, int index) {
			return vv(list).removeAt(index);
		}

		@Override
		public int get(Object list, int index) {
			return vv(list).get(index);
		}

		@Override
		public int size(Object list) {
			return vv(list).size();
		}

		@Override
		public Object copy(Object list) {
			return list;
		}

		@Override
		public Object addAllEmpty(Object list) {
			return vv(list).appendAll(List.of());
		}
	};

	@SuppressWarnings("unchecked")
	private static PersistentList<Integer> kl(Object list) {
		return (PersistentList<Integer>)list;
	}

	private static final Lib KOTLINX = new Lib() {
		@Override
		public String name() {
			return "kotlinx";
		}

		@Override
		public Object empty() {
			PersistentList<Integer> empty = ExtensionsKt.persistentListOf();
			return empty;
		}

		@Override
		public Object append(Object list, int value) {
			return kl(list).add(value);
		}

		@Override
		public Object insert(Object list, int index, int value) {
			return kl(list).add(index, value);
		}

		@Override
		public Object set(Object list, int index, int value) {
			return kl(list).set(index, value);
		}

		@Override
		public Object removeAt(Object list, int index) {
			return kl(list).removeAt(index);
		}

		@Override
		public int get(Object list, int index) {
			return kl(list).get(index);
		}

		@Override
		public int size(Object list) {
			return kl(list).size();
		}

		@Override
		public Object copy(Object list) {
			return list;
		}

		@Override
		public Object addAllEmpty(Object list) {
			return kl(list).addAll(List.of());
		}
	};

	/** 原地写、无版本化语义，仅作吞吐/内存下界参考。 */
	private static final Lib ARRAYLIST = new Lib() {
		@Override
		public String name() {
			return "ArrayList参考";
		}

		@SuppressWarnings("unchecked")
		private ArrayList<Integer> m(Object list) {
			return (ArrayList<Integer>)list;
		}

		@Override
		public Object empty() {
			return new ArrayList<Integer>();
		}

		@Override
		public Object append(Object list, int value) {
			m(list).add(value);
			return list; // 原地修改
		}

		@Override
		public Object insert(Object list, int index, int value) {
			m(list).add(index, value);
			return list;
		}

		@Override
		public Object set(Object list, int index, int value) {
			m(list).set(index, value);
			return list;
		}

		@Override
		public Object removeAt(Object list, int index) {
			m(list).remove(index);
			return list;
		}

		@Override
		public int get(Object list, int index) {
			return m(list).get(index);
		}

		@Override
		public int size(Object list) {
			return m(list).size();
		}

		@Override
		public Object copy(Object list) {
			return new ArrayList<>(m(list));
		}

		@Override
		public Object addAllEmpty(Object list) {
			return list; // 原地无操作
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

	/** 构建基础 list（也充当整体预热）。 */
	private Object build(Lib lib, int count) {
		var list = lib.empty();
		for (int i = 0; i < count; i++)
			list = lib.append(list, i);
		return list;
	}

	@SuppressWarnings("unchecked")
	private static Iterable<Integer> iterable(Object list) {
		return (Iterable<Integer>)list;
	}

	@Test
	public void compare() {
		System.out.printf("JVM=%s maxHeap=%,dMB N=%,d OPS=%,d ITER_PASSES=%d%n%n",
				System.getProperty("java.vm.version"), Runtime.getRuntime().maxMemory() >> 20, N, OPS, ITER_PASSES);

		for (var lib : new Lib[] { PCOLLECTIONS, VAVR, KOTLINX, ARRAYLIST }) {
			// 预热：构建一次全量 base（JIT 编译 append 路径）
			var base = build(lib, N);
			if (lib.size(base) != N)
				throw new AssertionError(lib.name() + " build size=" + lib.size(base));

			// build：从空开始逐条尾部追加，预热一次后测量第二次
			var warm = build(lib, N);
			if (lib.size(warm) != N)
				throw new AssertionError(lib.name() + " warm build size=" + lib.size(warm));
			var list = lib.empty();
			long a0 = allocated();
			long t0 = System.nanoTime();
			for (int i = 0; i < N; i++)
				list = lib.append(list, i);
			report(lib, "build", N, System.nanoTime() - t0, allocated() - a0);

			// append：尾部追加（LogList1.add 路径）
			list = lib.copy(base);
			for (int i = 0; i < OPS; i++) // 预热
				list = lib.append(list, i);
			list = lib.copy(base);
			a0 = allocated();
			t0 = System.nanoTime();
			for (int i = 0; i < OPS; i++)
				list = lib.append(list, i);
			report(lib, "append", OPS, System.nanoTime() - t0, allocated() - a0);
			if (lib.size(list) != N + OPS)
				throw new AssertionError(lib.name() + " append size=" + lib.size(list));

			// insertMiddle：中部插入（index 递增场景下取当前中点）
			list = lib.copy(base);
			for (int i = 0; i < OPS; i++) // 预热
				list = lib.insert(list, lib.size(list) / 2, i);
			list = lib.copy(base);
			a0 = allocated();
			t0 = System.nanoTime();
			for (int i = 0; i < OPS; i++)
				list = lib.insert(list, lib.size(list) / 2, i);
			report(lib, "insertMiddle", OPS, System.nanoTime() - t0, allocated() - a0);
			if (lib.size(list) != N + OPS)
				throw new AssertionError(lib.name() + " insertMiddle size=" + lib.size(list));

			// set：按下标改写（LogList1.set 路径）
			list = lib.copy(base);
			for (int i = 0; i < OPS; i++) // 预热
				list = lib.set(list, (i * 31 + 7) % N, i);
			list = lib.copy(base);
			a0 = allocated();
			t0 = System.nanoTime();
			for (int i = 0; i < OPS; i++)
				list = lib.set(list, (i * 31 + 7) % N, i);
			report(lib, "set", OPS, System.nanoTime() - t0, allocated() - a0);
			if (lib.size(list) != N)
				throw new AssertionError(lib.name() + " set size=" + lib.size(list));

			// removeMiddle：按下标中部删除
			list = lib.copy(base);
			for (int i = 0; i < OPS; i++) // 预热
				list = lib.removeAt(list, lib.size(list) / 2);
			list = lib.copy(base);
			a0 = allocated();
			t0 = System.nanoTime();
			for (int i = 0; i < OPS; i++)
				list = lib.removeAt(list, lib.size(list) / 2);
			report(lib, "removeMiddle", OPS, System.nanoTime() - t0, allocated() - a0);
			if (lib.size(list) != N - OPS)
				throw new AssertionError(lib.name() + " removeMiddle size=" + lib.size(list));

			// get：按下标读
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

			// iterate：整表遍历（ns/op 为每元素耗时）
			localSink = 0;
			for (var v : iterable(base)) // 预热
				localSink += v;
			localSink = 0;
			int elementVisits = N * ITER_PASSES;
			a0 = allocated();
			t0 = System.nanoTime();
			for (int p = 0; p < ITER_PASSES; p++)
				for (var v : iterable(base))
					localSink += v;
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
	 * no-op 引用恒等：Zeze 的 LogList1.addAll 依赖 plusAll(空集合)返回原实例廉价检测无变化；
	 * set(同值) 不被 Zeze 依赖，仅作观察。
	 */
	private void noOpIdentity() {
		System.out.println("no-op 引用恒等（==原实例）:");
		for (var lib : new Lib[] { PCOLLECTIONS, VAVR, KOTLINX }) {
			var base = build(lib, 1_000);
			var addEmpty = lib.addAllEmpty(base) == base;
			var setSame = lib.set(base, 5, 5) == base; // index=5 已存在且值相同
			System.out.printf("%-12s plusAll(空)==this: %-5s  set(同值)==this: %-5s%n",
					lib.name(), addEmpty, setSame);
		}
	}
}
