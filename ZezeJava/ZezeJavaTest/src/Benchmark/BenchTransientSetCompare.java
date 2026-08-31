package Benchmark;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashSet;

import clojure.lang.IEditableCollection;
import clojure.lang.IPersistentSet;
import clojure.lang.ITransientSet;
import clojure.lang.PersistentHashSet;
import harness.Bench;
import kotlinx.collections.immutable.ExtensionsKt;
import kotlinx.collections.immutable.PersistentSet;
import org.junit.jupiter.api.Test;
import org.pcollections.Empty;
import org.pcollections.PSet;

/**
 * 可变快照（transient/builder）评估基准（set 篇）：Clojure(PersistentHashSet transient)
 * vs kotlinx(PersistentHashSet builder/CHAMP) vs pcollections(Empty.set()→MapPSet，无快照，
 * 仅持久化操作对照)。
 *
 * 与 map 篇（BenchTransientSnapshotCompare）同口径，回答：
 * <ul>
 *   <li>开始/提交可变快照本身的成本（必须测量项）：begin（asTransient/builder）、
 *       beginCommit（空提交）、beginCommit1（begin+1写+提交，最小脏周期）；</li>
 *   <li>快照上的批量操作 vs pcollections：snapAdd(新元素)/snapReAdd(已存在元素，no-op 路径)/
 *       snapRemove/snapContains（周期 = begin + OPS 次操作 + commit，ns/op 按操作数折算）
 *       对照 pcollections 的 addNew/reAdd/remove/contains 与 addAll（plusAll）。</li>
 * </ul>
 * 附带 Clojure/kotlinx 自身的持久化操作与 java.util.HashSet 可变原地写下界参考，
 * 用于归因"快照收益"来自哪个环节。
 *
 * 互操作要点（javap/行为实测确认）：
 * <ul>
 *   <li>Clojure：持久化加为 cons（IPersistentCollection 返回），transient 加为 conj
 *       （ITransientCollection 返回），删为 disjoin；transient 读用 contains/get；
 *       无原生批量 add API。</li>
 *   <li>kotlinx：PersistentSet.builder() 存在，Builder 为 MutableSet 语义（add/remove 原地）。</li>
 *   <li>pcollections：Empty.set() 实际类型为 MapPSet（基于 HashTreePMap）。</li>
 * </ul>
 *
 * 运行：gradlew :ZezeJavaTest:bench --tests "*BenchTransientSetCompare"
 */
@Bench
public class BenchTransientSetCompare {
	public static final int N = 100_000; // 基础 set 大小（元素数）
	public static final int OPS = 10_000; // 每个快照周期的操作数
	public static final int CYCLES = 10; // 快照场景测量周期数
	public static final int WARM_CYCLES = 2; // 快照场景预热周期数
	public static final int GET_OPS = 1_000_000;
	public static final int BEGIN_ITERS = 200_000; // begin/commit 场景迭代数
	public static final int BULK_ITERS = 20; // addAll 场景迭代数（每次 plusAll OPS 条）
	public static final int BULK_WARM = 5;

	/** 防止死码消除的结果汇聚点。 */
	public static volatile long sink;

	private interface Lib {
		String name();

		boolean snapshot(); // 是否支持可变快照（transient/builder）

		boolean bulkAdd(); // 是否有原生持久化批量加（plusAll/addAll）

		Object empty();

		Object buildBase(int count); // 构建基础 set（兼作整体预热）

		Object add(Object set, int value); // 持久化加（新元素或已存在元素）：返回新版本

		Object remove(Object set, int value); // 持久化删：返回新版本

		int contains(Object set, int value); // 命中返回 1，未命中返回 0

		int size(Object set);

		/** 场景隔离用副本：持久化库 O(1) 返回自身（版本不可变），HashSet 真实拷贝。 */
		Object copy(Object set);

		Object plusAll(Object set, ArrayList<Integer> values); // 持久化批量加

		// ---- 可变快照（仅 snapshot()==true 时可用）----

		Object begin(Object set); // 开始可变快照

		Object snapAdd(Object snap, int value); // 快照上加（原地，返回快照引用）

		Object snapRemove(Object snap, int value); // 快照上删（原地）

		int snapContains(Object snap, int value); // 命中返回 1，未命中返回 0

		int snapSize(Object snap);

		Object commit(Object snap); // 提交快照，返回持久化版本
	}

	private static IPersistentSet cljs(Object set) {
		return (IPersistentSet)set;
	}

	private static ITransientSet cljsT(Object snap) {
		return (ITransientSet)snap;
	}

	private static final Lib CLOJURE = new Lib() {
		@Override
		public String name() {
			return "Clojure";
		}

		@Override
		public boolean snapshot() {
			return true; // PersistentHashSet transient
		}

		@Override
		public boolean bulkAdd() {
			return false; // 无原生批量 API
		}

		@Override
		public Object empty() {
			return PersistentHashSet.EMPTY;
		}

		@Override
		public Object buildBase(int count) {
			IPersistentSet s = PersistentHashSet.EMPTY;
			for (int i = 0; i < count; i++)
				s = (IPersistentSet)s.cons(i);
			return s;
		}

		@Override
		public Object add(Object set, int value) {
			return cljs(set).cons(value);
		}

		@Override
		public Object remove(Object set, int value) {
			return cljs(set).disjoin(value);
		}

		@Override
		public int contains(Object set, int value) {
			return cljs(set).contains(value) ? 1 : 0;
		}

		@Override
		public int size(Object set) {
			return cljs(set).count();
		}

		@Override
		public Object copy(Object set) {
			return set; // 版本不可变
		}

		@Override
		public Object plusAll(Object set, ArrayList<Integer> values) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object begin(Object set) {
			return (ITransientSet)((IEditableCollection)cljs(set)).asTransient();
		}

		@Override
		public Object snapAdd(Object snap, int value) {
			return cljsT(snap).conj(value);
		}

		@Override
		public Object snapRemove(Object snap, int value) {
			return cljsT(snap).disjoin(value);
		}

		@Override
		public int snapContains(Object snap, int value) {
			return cljsT(snap).contains(value) ? 1 : 0;
		}

		@Override
		public int snapSize(Object snap) {
			return cljsT(snap).count();
		}

		@Override
		public Object commit(Object snap) {
			return cljsT(snap).persistent();
		}
	};

	@SuppressWarnings("unchecked")
	private static PersistentSet<Integer> kxs(Object set) {
		return (PersistentSet<Integer>)set;
	}

	@SuppressWarnings("unchecked")
	private static PersistentSet.Builder<Integer> kxsB(Object snap) {
		return (PersistentSet.Builder<Integer>)snap;
	}

	private static final Lib KOTLINX = new Lib() {
		@Override
		public String name() {
			return "kotlinx";
		}

		@Override
		public boolean snapshot() {
			return true; // PersistentHashSet builder
		}

		@Override
		public boolean bulkAdd() {
			return true; // addAll
		}

		@Override
		public Object empty() {
			return ExtensionsKt.<Integer>persistentHashSetOf();
		}

		@Override
		public Object buildBase(int count) {
			PersistentSet<Integer> s = ExtensionsKt.persistentHashSetOf();
			for (int i = 0; i < count; i++)
				s = s.add(i);
			return s;
		}

		@Override
		public Object add(Object set, int value) {
			return kxs(set).add(value);
		}

		@Override
		public Object remove(Object set, int value) {
			return kxs(set).remove(value);
		}

		@Override
		public int contains(Object set, int value) {
			return kxs(set).contains(value) ? 1 : 0;
		}

		@Override
		public int size(Object set) {
			return kxs(set).size();
		}

		@Override
		public Object copy(Object set) {
			return set; // 版本不可变
		}

		@Override
		public Object plusAll(Object set, ArrayList<Integer> values) {
			return kxs(set).addAll(values);
		}

		@Override
		public Object begin(Object set) {
			return kxs(set).builder();
		}

		@Override
		public Object snapAdd(Object snap, int value) {
			kxsB(snap).add(value); // MutableSet.add 返回 boolean，忽略
			return snap;
		}

		@Override
		public Object snapRemove(Object snap, int value) {
			kxsB(snap).remove(value); // MutableSet.remove 返回 boolean，忽略
			return snap;
		}

		@Override
		public int snapContains(Object snap, int value) {
			return kxsB(snap).contains(value) ? 1 : 0;
		}

		@Override
		public int snapSize(Object snap) {
			return kxsB(snap).size();
		}

		@Override
		public Object commit(Object snap) {
			return kxsB(snap).build();
		}
	};

	@SuppressWarnings("unchecked")
	private static PSet<Integer> pcs(Object set) {
		return (PSet<Integer>)set;
	}

	private static final Lib PCOLLECTIONS = new Lib() {
		@Override
		public String name() {
			return "pcollections";
		}

		@Override
		public boolean snapshot() {
			return false; // 无可变快照 API（此前已实测确认）
		}

		@Override
		public boolean bulkAdd() {
			return true; // plusAll
		}

		@Override
		public Object empty() {
			return Empty.<Integer>set(); // MapPSet（基于 HashTreePMap）
		}

		@Override
		public Object buildBase(int count) {
			var s = Empty.<Integer>set();
			for (int i = 0; i < count; i++)
				s = pcs(s).plus(i);
			return s;
		}

		@Override
		public Object add(Object set, int value) {
			return pcs(set).plus(value);
		}

		@Override
		public Object remove(Object set, int value) {
			return pcs(set).minus(value);
		}

		@Override
		public int contains(Object set, int value) {
			return pcs(set).contains(value) ? 1 : 0;
		}

		@Override
		public int size(Object set) {
			return pcs(set).size();
		}

		@Override
		public Object copy(Object set) {
			return set; // 版本不可变
		}

		@Override
		public Object plusAll(Object set, ArrayList<Integer> values) {
			return pcs(set).plusAll(values);
		}

		@Override
		public Object begin(Object set) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object snapAdd(Object snap, int value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object snapRemove(Object snap, int value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int snapContains(Object snap, int value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int snapSize(Object snap) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object commit(Object snap) {
			throw new UnsupportedOperationException();
		}
	};

	/** 原地写、无版本化语义，仅作吞吐/内存下界参考。 */
	private static final Lib HASHSET = new Lib() {
		@SuppressWarnings("unchecked")
		private HashSet<Integer> hs(Object set) {
			return (HashSet<Integer>)set;
		}

		@Override
		public String name() {
			return "HashSet参考";
		}

		@Override
		public boolean snapshot() {
			return false;
		}

		@Override
		public boolean bulkAdd() {
			return true; // addAll
		}

		@Override
		public Object empty() {
			return new HashSet<Integer>();
		}

		@Override
		public Object buildBase(int count) {
			var s = new HashSet<Integer>(count * 2);
			for (int i = 0; i < count; i++)
				s.add(i);
			return s;
		}

		@Override
		public Object add(Object set, int value) {
			hs(set).add(value);
			return set; // 原地修改
		}

		@Override
		public Object remove(Object set, int value) {
			hs(set).remove(value);
			return set;
		}

		@Override
		public int contains(Object set, int value) {
			return hs(set).contains(value) ? 1 :  0;
		}

		@Override
		public int size(Object set) {
			return hs(set).size();
		}

		@Override
		public Object copy(Object set) {
			return new HashSet<>(hs(set)); // 场景隔离需真实拷贝
		}

		@Override
		public Object plusAll(Object set, ArrayList<Integer> values) {
			hs(set).addAll(values);
			return set;
		}

		@Override
		public Object begin(Object set) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object snapAdd(Object snap, int value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object snapRemove(Object snap, int value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int snapContains(Object snap, int value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int snapSize(Object snap) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object commit(Object snap) {
			throw new UnsupportedOperationException();
		}
	};

	private static final com.sun.management.ThreadMXBean THREAD_ALLOC =
			(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();

	private static long allocated() {
		return THREAD_ALLOC.getThreadAllocatedBytes(Thread.currentThread().getId());
	}

	private void report(Lib lib, String scenario, int ops, long nanos, long bytes) {
		System.out.printf("%-12s %-14s %,12d ops  %,10.1f ns/op  %,10.1f B/op%n",
				lib.name(), scenario, ops, (double)nanos / ops, (double)bytes / ops);
	}

	@Test
	public void compare() {
		System.out.printf("JVM=%s maxHeap=%,dMB N=%,d OPS=%,d CYCLES=%d BEGIN_ITERS=%,d%n",
				System.getProperty("java.vm.version"), Runtime.getRuntime().maxMemory() >> 20,
				N, OPS, CYCLES, BEGIN_ITERS);
		System.out.println("Clojure=1.12.0(PersistentHashSet transient)  kotlinx=0.3.7(PersistentHashSet builder)  "
				+ "pcollections=4.0.2(MapPSet,无快照)  HashSet=参考(原地写)\n");

		for (var lib : new Lib[] { CLOJURE, KOTLINX, PCOLLECTIONS, HASHSET }) {
			var base = lib.buildBase(N);
			if (lib.size(base) != N)
				throw new AssertionError(lib.name() + " buildBase size=" + lib.size(base));
			var localSink = 0;

			// addNew：持久化加新元素
			var set = lib.copy(base);
			for (int i = 0; i < OPS; i++) // 预热
				set = lib.add(set, N + i);
			set = lib.copy(base);
			long a0 = allocated();
			long t0 = System.nanoTime();
			for (int i = 0; i < OPS; i++)
				set = lib.add(set, N + i);
			report(lib, "addNew", OPS, System.nanoTime() - t0, allocated() - a0);
			if (lib.size(set) != N + OPS)
				throw new AssertionError(lib.name() + " addNew size=" + lib.size(set));

			// reAdd：持久化加已存在元素（no-op 路径）
			set = lib.copy(base);
			for (int i = 0; i < OPS; i++) // 预热
				set = lib.add(set, (i * 31 + 7) % N);
			set = lib.copy(base);
			a0 = allocated();
			t0 = System.nanoTime();
			for (int i = 0; i < OPS; i++)
				set = lib.add(set, (i * 31 + 7) % N);
			report(lib, "reAdd", OPS, System.nanoTime() - t0, allocated() - a0);
			if (lib.size(set) != N)
				throw new AssertionError(lib.name() + " reAdd size=" + lib.size(set));

			// remove：持久化前缀连续删除
			set = lib.copy(base);
			for (int i = 0; i < OPS; i++) // 预热
				set = lib.remove(set, i);
			set = lib.copy(base);
			a0 = allocated();
			t0 = System.nanoTime();
			for (int i = 0; i < OPS; i++)
				set = lib.remove(set, i);
			report(lib, "remove", OPS, System.nanoTime() - t0, allocated() - a0);
			if (lib.size(set) != N - OPS)
				throw new AssertionError(lib.name() + " remove size=" + lib.size(set));

			// contains：持久化随机命中读
			for (int i = 0; i < GET_OPS / 10; i++) // 预热
				localSink += lib.contains(base, (i * 31 + 7) % N);
			localSink = 0;
			a0 = allocated();
			t0 = System.nanoTime();
			for (int i = 0; i < GET_OPS; i++)
				localSink += lib.contains(base, (i * 31 + 7) % N);
			report(lib, "contains", GET_OPS, System.nanoTime() - t0, allocated() - a0);
			sink += localSink;
			localSink = 0;

			// addAll：持久化批量加（plusAll/addAll）
			if (lib.bulkAdd()) {
				var bulk = new ArrayList<Integer>();
				for (int i = 0; i < OPS; i++)
					bulk.add(N + i);
				for (int w = 0; w < BULK_WARM; w++) { // 预热
					var r = lib.plusAll(lib.copy(base), bulk); // HashSet 原地写：需副本隔离
					if (lib.size(r) != N + OPS)
						throw new AssertionError(lib.name() + " addAll warm size=" + lib.size(r));
				}
				a0 = allocated();
				t0 = System.nanoTime();
				for (int it = 0; it < BULK_ITERS; it++)
					localSink += lib.size(lib.plusAll(lib.copy(base), bulk));
				report(lib, "addAll", BULK_ITERS * OPS, System.nanoTime() - t0, allocated() - a0);
				sink += localSink;
				localSink = 0;
			}

			// ---- 可变快照场景 ----
			if (lib.snapshot()) {
				// begin：仅开始快照（不提交即丢弃）
				for (int i = 0; i < BEGIN_ITERS / 4; i++) // 预热
					localSink += lib.snapSize(lib.begin(base));
				localSink = 0;
				a0 = allocated();
				t0 = System.nanoTime();
				for (int i = 0; i < BEGIN_ITERS; i++)
					localSink += lib.snapSize(lib.begin(base));
				report(lib, "begin", BEGIN_ITERS, System.nanoTime() - t0, allocated() - a0);
				sink += localSink;
				localSink = 0;

				// beginCommit：begin + 空提交（提交可变快照的最小成本）
				for (int i = 0; i < BEGIN_ITERS / 4; i++) // 预热
					localSink += lib.size(lib.commit(lib.begin(base)));
				localSink = 0;
				a0 = allocated();
				t0 = System.nanoTime();
				for (int i = 0; i < BEGIN_ITERS; i++)
					localSink += lib.size(lib.commit(lib.begin(base)));
				report(lib, "beginCommit", BEGIN_ITERS, System.nanoTime() - t0, allocated() - a0);
				sink += localSink;
				localSink = 0;

				// beginCommit1：begin + 1 次加新元素 + 提交（最小脏周期）
				for (int i = 0; i < BEGIN_ITERS / 4; i++) { // 预热
					var snap = lib.begin(base);
					snap = lib.snapAdd(snap, N + i);
					localSink += lib.size(lib.commit(snap));
				}
				localSink = 0;
				a0 = allocated();
				t0 = System.nanoTime();
				for (int i = 0; i < BEGIN_ITERS; i++) {
					var snap = lib.begin(base);
					snap = lib.snapAdd(snap, N + i);
					localSink += lib.size(lib.commit(snap));
				}
				report(lib, "beginCommit1", BEGIN_ITERS, System.nanoTime() - t0, allocated() - a0);
				sink += localSink;
				localSink = 0;

				// snapAdd：周期 = begin + OPS 次加新元素 + commit
				for (int w = 0; w < WARM_CYCLES; w++) { // 预热
					var snap = lib.begin(base);
					for (int i = 0; i < OPS; i++)
						snap = lib.snapAdd(snap, N + i);
					var committed = lib.commit(snap);
					if (lib.size(committed) != N + OPS)
						throw new AssertionError(lib.name() + " snapAdd warm size=" + lib.size(committed));
				}
				a0 = allocated();
				t0 = System.nanoTime();
				var last = base;
				for (int c = 0; c < CYCLES; c++) {
					var snap = lib.begin(base);
					for (int i = 0; i < OPS; i++)
						snap = lib.snapAdd(snap, N + i);
					last = lib.commit(snap);
					localSink += lib.size(last);
				}
				report(lib, "snapAdd", CYCLES * OPS, System.nanoTime() - t0, allocated() - a0);
				if (lib.size(last) != N + OPS)
					throw new AssertionError(lib.name() + " snapAdd size=" + lib.size(last));

				// snapReAdd：周期 = begin + OPS 次加已存在元素（no-op 路径）+ commit
				for (int w = 0; w < WARM_CYCLES; w++) { // 预热
					var snap = lib.begin(base);
					for (int i = 0; i < OPS; i++)
						snap = lib.snapAdd(snap, (i * 31 + 7) % N);
					var committed = lib.commit(snap);
					if (lib.size(committed) != N)
						throw new AssertionError(lib.name() + " snapReAdd warm size=" + lib.size(committed));
				}
				a0 = allocated();
				t0 = System.nanoTime();
				last = base;
				for (int c = 0; c < CYCLES; c++) {
					var snap = lib.begin(base);
					for (int i = 0; i < OPS; i++)
						snap = lib.snapAdd(snap, (i * 31 + 7) % N);
					last = lib.commit(snap);
					localSink += lib.size(last);
				}
				report(lib, "snapReAdd", CYCLES * OPS, System.nanoTime() - t0, allocated() - a0);
				if (lib.size(last) != N)
					throw new AssertionError(lib.name() + " snapReAdd size=" + lib.size(last));

				// snapRemove：周期 = begin + OPS 次前缀删除 + commit
				for (int w = 0; w < WARM_CYCLES; w++) { // 预热
					var snap = lib.begin(base);
					for (int i = 0; i < OPS; i++)
						snap = lib.snapRemove(snap, i);
					var committed = lib.commit(snap);
					if (lib.size(committed) != N - OPS)
						throw new AssertionError(lib.name() + " snapRemove warm size=" + lib.size(committed));
				}
				a0 = allocated();
				t0 = System.nanoTime();
				last = base;
				for (int c = 0; c < CYCLES; c++) {
					var snap = lib.begin(base);
					for (int i = 0; i < OPS; i++)
						snap = lib.snapRemove(snap, i);
					last = lib.commit(snap);
					localSink += lib.size(last);
				}
				report(lib, "snapRemove", CYCLES * OPS, System.nanoTime() - t0, allocated() - a0);
				if (lib.size(last) != N - OPS)
					throw new AssertionError(lib.name() + " snapRemove size=" + lib.size(last));

				// snapContains：单快照 + 随机命中读（提交不计入测量）
				var snap = lib.begin(base);
				for (int i = 0; i < GET_OPS / 10; i++) // 预热
					localSink += lib.snapContains(snap, (i * 31 + 7) % N);
				localSink = 0;
				a0 = allocated();
				t0 = System.nanoTime();
				for (int i = 0; i < GET_OPS; i++)
					localSink += lib.snapContains(snap, (i * 31 + 7) % N);
				report(lib, "snapContains", GET_OPS, System.nanoTime() - t0, allocated() - a0);
				sink += localSink;
				localSink = 0;
				sink += lib.size(lib.commit(snap)); // 收尾提交（不计时）
			}
			sink += localSink;
			System.out.println();
		}
	}
}
