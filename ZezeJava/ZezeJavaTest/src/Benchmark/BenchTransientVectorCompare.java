package Benchmark;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;

import clojure.lang.IEditableCollection;
import clojure.lang.IPersistentVector;
import clojure.lang.ITransientVector;
import clojure.lang.PersistentVector;
import harness.Bench;
import kotlinx.collections.immutable.ExtensionsKt;
import kotlinx.collections.immutable.PersistentList;
import org.junit.jupiter.api.Test;
import org.pcollections.Empty;
import org.pcollections.PVector;

/**
 * 可变快照（transient/builder）评估基准（vector/list 篇）：Clojure(PersistentVector transient)
 * vs kotlinx(PersistentList builder) vs pcollections(TreePVector，无快照，仅持久化操作对照)，
 * 附带 ArrayList 可变原地写下界参考。
 *
 * 与 map 篇（BenchTransientSnapshotCompare）同口径，回答：
 * <ul>
 *   <li>开始/提交可变快照本身的成本（必须测量项）：begin（asTransient/builder）、
 *       beginCommit（空提交）、beginCommit1（begin+1写+提交，最小脏周期）；</li>
 *   <li>快照上的批量操作 vs pcollections：snapSet/snapAppend/snapPopTail/snapGet
 *       （周期 = begin + OPS 次操作 + commit，ns/op 按操作数折算）对照 pcollections 的
 *       setOverwrite/append/popTail/get 与 appendAll（plusAll，pcollections 最接近快照批量的原生路径）；</li>
 *   <li>中间插入/删除：insertMid/removeMid 及快照版 snapInsertMid/snapRemoveMid ——
 *       Clojure PersistentVector 无此 API（仅尾追加/去尾，中间操作需 O(n) 重建），跳过；
 *       ArrayList 参考为 O(n) 搬移的原地写。</li>
 * </ul>
 * 附带 Clojure/kotlinx 自身的持久化操作与 ArrayList 参考（原地写、无版本化语义，下界），
 * 用于归因"快照收益"来自哪个环节。
 *
 * 互操作要点（javap/行为实测确认）：
 * <ul>
 *   <li>Clojure：持久化追加以 cons（IPersistentCollection）返回，transient 以 conj
 *       （ITransientCollection）返回，均须按新引用接回；ITransientVector 支持
 *       assocN/pop/nth/count；pop 为 O(1) 去尾；无原生批量 append API，也无中间插入/删除。</li>
 *   <li>kotlinx：PersistentList 持久化接口有 add(int,E)/removeAt(int)（中间操作可用）；
 *       builder 为 MutableList 语义（add/set/remove 原地，返回旧值需忽略）。</li>
 *   <li>pcollections：Empty.vector() 即 TreePVector；plus(int,E) 插入、minus(int) 索引删。</li>
 * </ul>
 *
 * 运行：gradlew :ZezeJavaTest:bench --tests "*BenchTransientVectorCompare"
 */
@Bench
public class BenchTransientVectorCompare {
	public static final int N = 100_000; // 基础 vector 大小（元素数）
	public static final int OPS = 10_000; // 每个快照周期的操作数
	public static final int CYCLES = 10; // 快照场景测量周期数
	public static final int WARM_CYCLES = 2; // 快照场景预热周期数
	public static final int GET_OPS = 1_000_000;
	public static final int BEGIN_ITERS = 200_000; // begin/commit 场景迭代数
	public static final int BULK_ITERS = 20; // appendAll 场景迭代数（每次 plusAll OPS 条）
	public static final int BULK_WARM = 5;

	/** 防止死码消除的结果汇聚点。 */
	public static volatile long sink;

	private interface Lib {
		String name();

		boolean snapshot(); // 是否支持可变快照（transient/builder）

		boolean bulkAppend(); // 是否有原生持久化批量尾追加（plusAll/addAll）

		boolean midOps(); // 是否支持中间插入/删除（Clojure vector 无此 API）

		Object empty();

		Object buildBase(int count); // 构建基础 vector（兼作整体预热）

		Object append(Object vec, int value); // 持久化尾追加：返回新版本

		Object set(Object vec, int index, int value); // 持久化覆盖已有位置：返回新版本

		Object popTail(Object vec); // 持久化去尾：返回新版本

		Object insert(Object vec, int index, int value); // 持久化中间插入：返回新版本

		Object removeAt(Object vec, int index); // 持久化中间删除：返回新版本

		int get(Object vec, int index); // 越界/未命中返回 Integer.MIN_VALUE

		int size(Object vec);

		/** 场景隔离用副本：持久化库 O(1) 返回自身（版本不可变），ArrayList 真实拷贝。 */
		Object copy(Object vec);

		Object plusAll(Object vec, ArrayList<Integer> values); // 持久化批量尾追加

		// ---- 可变快照（仅 snapshot()==true 时可用）----

		Object begin(Object vec); // 开始可变快照

		Object snapAppend(Object snap, int value); // 快照上尾追加（原地，返回快照引用）

		Object snapSet(Object snap, int index, int value); // 快照上覆盖已有位置（原地）

		Object snapPopTail(Object snap); // 快照上去尾（原地）

		Object snapInsert(Object snap, int index, int value); // 快照上中间插入（原地）

		Object snapRemoveAt(Object snap, int index); // 快照上中间删除（原地）

		int snapGet(Object snap, int index);

		int snapSize(Object snap);

		Object commit(Object snap); // 提交快照，返回持久化版本
	}

	private static IPersistentVector cljv(Object vec) {
		return (IPersistentVector)vec;
	}

	private static ITransientVector cljvT(Object snap) {
		return (ITransientVector)snap;
	}

	private static final Lib CLOJURE = new Lib() {
		@Override
		public String name() {
			return "Clojure";
		}

		@Override
		public boolean snapshot() {
			return true; // PersistentVector transient
		}

		@Override
		public boolean bulkAppend() {
			return false; // 无原生批量 API（create(Iterable) 仅能从零构建）
		}

		@Override
		public boolean midOps() {
			return false; // PersistentVector 无中间插入/删除 API（需 O(n) 重建）
		}

		@Override
		public Object empty() {
			return PersistentVector.EMPTY;
		}

		@Override
		public Object buildBase(int count) {
			IPersistentVector v = PersistentVector.EMPTY;
			for (int i = 0; i < count; i++)
				v = (IPersistentVector)v.cons(i);
			return v;
		}

		@Override
		public Object append(Object vec, int value) {
			return cljv(vec).cons(value);
		}

		@Override
		public Object set(Object vec, int index, int value) {
			return cljv(vec).assocN(index, value);
		}

		@Override
		public Object popTail(Object vec) {
			return cljv(vec).pop(); // O(1) 去尾
		}

		@Override
		public Object insert(Object vec, int index, int value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object removeAt(Object vec, int index) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int get(Object vec, int index) {
			return (Integer)cljv(vec).nth(index);
		}

		@Override
		public int size(Object vec) {
			return cljv(vec).count();
		}

		@Override
		public Object copy(Object vec) {
			return vec; // 版本不可变
		}

		@Override
		public Object plusAll(Object vec, ArrayList<Integer> values) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object begin(Object vec) {
			return (ITransientVector)((IEditableCollection)cljv(vec)).asTransient();
		}

		@Override
		public Object snapAppend(Object snap, int value) {
			return cljvT(snap).conj(value);
		}

		@Override
		public Object snapSet(Object snap, int index, int value) {
			return cljvT(snap).assocN(index, value);
		}

		@Override
		public Object snapPopTail(Object snap) {
			return cljvT(snap).pop();
		}

		@Override
		public Object snapInsert(Object snap, int index, int value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object snapRemoveAt(Object snap, int index) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int snapGet(Object snap, int index) {
			return (Integer)cljvT(snap).nth(index);
		}

		@Override
		public int snapSize(Object snap) {
			return cljvT(snap).count();
		}

		@Override
		public Object commit(Object snap) {
			return cljvT(snap).persistent();
		}
	};

	@SuppressWarnings("unchecked")
	private static PersistentList<Integer> kxl(Object vec) {
		return (PersistentList<Integer>)vec;
	}

	@SuppressWarnings("unchecked")
	private static PersistentList.Builder<Integer> kxlB(Object snap) {
		return (PersistentList.Builder<Integer>)snap;
	}

	private static final Lib KOTLINX = new Lib() {
		@Override
		public String name() {
			return "kotlinx";
		}

		@Override
		public boolean snapshot() {
			return true; // PersistentList builder
		}

		@Override
		public boolean bulkAppend() {
			return true; // addAll
		}

		@Override
		public boolean midOps() {
			return true; // add(int,E)/removeAt(int)
		}

		@Override
		public Object empty() {
			return ExtensionsKt.<Integer>persistentListOf();
		}

		@Override
		public Object buildBase(int count) {
			PersistentList<Integer> l = ExtensionsKt.persistentListOf();
			for (int i = 0; i < count; i++)
				l = l.add(i);
			return l;
		}

		@Override
		public Object append(Object vec, int value) {
			return kxl(vec).add(value);
		}

		@Override
		public Object set(Object vec, int index, int value) {
			return kxl(vec).set(index, value);
		}

		@Override
		public Object popTail(Object vec) {
			return kxl(vec).removeAt(kxl(vec).size() - 1);
		}

		@Override
		public Object insert(Object vec, int index, int value) {
			return kxl(vec).add(index, value);
		}

		@Override
		public Object removeAt(Object vec, int index) {
			return kxl(vec).removeAt(index);
		}

		@Override
		public int get(Object vec, int index) {
			var v = kxl(vec).get(index);
			return v != null ? v : Integer.MIN_VALUE;
		}

		@Override
		public int size(Object vec) {
			return kxl(vec).size();
		}

		@Override
		public Object copy(Object vec) {
			return vec; // 版本不可变
		}

		@Override
		public Object plusAll(Object vec, ArrayList<Integer> values) {
			return kxl(vec).addAll(values);
		}

		@Override
		public Object begin(Object vec) {
			return kxl(vec).builder();
		}

		@Override
		public Object snapAppend(Object snap, int value) {
			kxlB(snap).add(value); // MutableList.add 返回 boolean，忽略
			return snap;
		}

		@Override
		public Object snapSet(Object snap, int index, int value) {
			kxlB(snap).set(index, value); // MutableList.set 返回旧值，忽略
			return snap;
		}

		@Override
		public Object snapPopTail(Object snap) {
			kxlB(snap).remove(kxlB(snap).size() - 1); // java.util.List.remove(int)，返回旧值忽略
			return snap;
		}

		@Override
		public Object snapInsert(Object snap, int index, int value) {
			kxlB(snap).add(index, value); // MutableList.add(int,E) 原地
			return snap;
		}

		@Override
		public Object snapRemoveAt(Object snap, int index) {
			kxlB(snap).remove(index); // java.util.List.remove(int)，返回旧值忽略
			return snap;
		}

		@Override
		public int snapGet(Object snap, int index) {
			var v = kxlB(snap).get(index);
			return v != null ? v : Integer.MIN_VALUE;
		}

		@Override
		public int snapSize(Object snap) {
			return kxlB(snap).size();
		}

		@Override
		public Object commit(Object snap) {
			return kxlB(snap).build();
		}
	};

	@SuppressWarnings("unchecked")
	private static PVector<Integer> pcv(Object vec) {
		return (PVector<Integer>)vec;
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
		public boolean bulkAppend() {
			return true; // plusAll
		}

		@Override
		public boolean midOps() {
			return true; // plus(int,E)/minus(int)
		}

		@Override
		public Object empty() {
			return Empty.<Integer>vector(); // TreePVector
		}

		@Override
		public Object buildBase(int count) {
			var v = Empty.<Integer>vector();
			for (int i = 0; i < count; i++)
				v = pcv(v).plus(i);
			return v;
		}

		@Override
		public Object append(Object vec, int value) {
			return pcv(vec).plus(value);
		}

		@Override
		public Object set(Object vec, int index, int value) {
			return pcv(vec).with(index, value);
		}

		@Override
		public Object popTail(Object vec) {
			return pcv(vec).minus(pcv(vec).size() - 1); // O(log n) 索引删
		}

		@Override
		public Object insert(Object vec, int index, int value) {
			return pcv(vec).plus(index, value); // 中间插入
		}

		@Override
		public Object removeAt(Object vec, int index) {
			return pcv(vec).minus(index); // 中间索引删
		}

		@Override
		public int get(Object vec, int index) {
			var v = pcv(vec).get(index);
			return v != null ? v : Integer.MIN_VALUE;
		}

		@Override
		public int size(Object vec) {
			return pcv(vec).size();
		}

		@Override
		public Object copy(Object vec) {
			return vec; // 版本不可变
		}

		@Override
		public Object plusAll(Object vec, ArrayList<Integer> values) {
			return pcv(vec).plusAll(values);
		}

		@Override
		public Object begin(Object vec) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object snapAppend(Object snap, int value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object snapSet(Object snap, int index, int value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object snapPopTail(Object snap) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object snapInsert(Object snap, int index, int value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object snapRemoveAt(Object snap, int index) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int snapGet(Object snap, int index) {
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
	private static final Lib ARRAYLIST = new Lib() {
		@SuppressWarnings("unchecked")
		private ArrayList<Integer> al(Object vec) {
			return (ArrayList<Integer>)vec;
		}

		@Override
		public String name() {
			return "ArrayList参考";
		}

		@Override
		public boolean snapshot() {
			return false;
		}

		@Override
		public boolean bulkAppend() {
			return true; // addAll
		}

		@Override
		public boolean midOps() {
			return true; // add(int,E)/remove(int)
		}

		@Override
		public Object empty() {
			return new ArrayList<Integer>();
		}

		@Override
		public Object buildBase(int count) {
			var l = new ArrayList<Integer>(count);
			for (int i = 0; i < count; i++)
				l.add(i);
			return l;
		}

		@Override
		public Object append(Object vec, int value) {
			al(vec).add(value);
			return vec; // 原地修改
		}

		@Override
		public Object set(Object vec, int index, int value) {
			al(vec).set(index, value);
			return vec;
		}

		@Override
		public Object popTail(Object vec) {
			al(vec).remove(al(vec).size() - 1);
			return vec;
		}

		@Override
		public Object insert(Object vec, int index, int value) {
			al(vec).add(index, value); // O(n) 搬移
			return vec;
		}

		@Override
		public Object removeAt(Object vec, int index) {
			al(vec).remove(index); // O(n) 搬移
			return vec;
		}

		@Override
		public int get(Object vec, int index) {
			return al(vec).get(index);
		}

		@Override
		public int size(Object vec) {
			return al(vec).size();
		}

		@Override
		public Object copy(Object vec) {
			return new ArrayList<>(al(vec)); // 场景隔离需真实拷贝
		}

		@Override
		public Object plusAll(Object vec, ArrayList<Integer> values) {
			al(vec).addAll(values);
			return vec;
		}

		@Override
		public Object begin(Object vec) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object snapAppend(Object snap, int value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object snapSet(Object snap, int index, int value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object snapPopTail(Object snap) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object snapInsert(Object snap, int index, int value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object snapRemoveAt(Object snap, int index) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int snapGet(Object snap, int index) {
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
		System.out.println("Clojure=1.12.0(PersistentVector transient)  kotlinx=0.3.7(PersistentList builder)  "
				+ "pcollections=4.0.2(TreePVector,无快照)  ArrayList=参考(原地写)");
		System.out.println("insertMid/removeMid：Clojure PersistentVector 无此 API 跳过；ArrayList 参考 O(n) 搬移\n");

		for (var lib : new Lib[] { CLOJURE, KOTLINX, PCOLLECTIONS, ARRAYLIST }) {
			var base = lib.buildBase(N);
			if (lib.size(base) != N)
				throw new AssertionError(lib.name() + " buildBase size=" + lib.size(base));
			var localSink = 0;

			// setOverwrite：持久化覆盖已有位置
			var vec = lib.copy(base);
			for (int i = 0; i < OPS; i++) // 预热
				vec = lib.set(vec, (i * 31 + 7) % N, i);
			vec = lib.copy(base);
			long a0 = allocated();
			long t0 = System.nanoTime();
			for (int i = 0; i < OPS; i++)
				vec = lib.set(vec, (i * 31 + 7) % N, i);
			report(lib, "setOverwrite", OPS, System.nanoTime() - t0, allocated() - a0);
			if (lib.size(vec) != N)
				throw new AssertionError(lib.name() + " setOverwrite size=" + lib.size(vec));

			// append：持久化尾追加新元素
			vec = lib.copy(base);
			for (int i = 0; i < OPS; i++) // 预热
				vec = lib.append(vec, N + i);
			vec = lib.copy(base);
			a0 = allocated();
			t0 = System.nanoTime();
			for (int i = 0; i < OPS; i++)
				vec = lib.append(vec, N + i);
			report(lib, "append", OPS, System.nanoTime() - t0, allocated() - a0);
			if (lib.size(vec) != N + OPS)
				throw new AssertionError(lib.name() + " append size=" + lib.size(vec));

			// popTail：持久化去尾
			vec = lib.copy(base);
			for (int i = 0; i < OPS; i++) // 预热
				vec = lib.popTail(vec);
			vec = lib.copy(base);
			a0 = allocated();
			t0 = System.nanoTime();
			for (int i = 0; i < OPS; i++)
				vec = lib.popTail(vec);
			report(lib, "popTail", OPS, System.nanoTime() - t0, allocated() - a0);
			if (lib.size(vec) != N - OPS)
				throw new AssertionError(lib.name() + " popTail size=" + lib.size(vec));

			// insertMid：持久化中间插入（插到当前中点；Clojure 无此 API 跳过）
			if (lib.midOps()) {
				vec = lib.copy(base);
				for (int i = 0; i < OPS; i++) // 预热
					vec = lib.insert(vec, (N + i) / 2, N + i);
				if (lib.size(vec) != N + OPS)
					throw new AssertionError(lib.name() + " insertMid warm size=" + lib.size(vec));
				vec = lib.copy(base);
				a0 = allocated();
				t0 = System.nanoTime();
				for (int i = 0; i < OPS; i++)
					vec = lib.insert(vec, (N + i) / 2, N + i);
				report(lib, "insertMid", OPS, System.nanoTime() - t0, allocated() - a0);
				if (lib.size(vec) != N + OPS)
					throw new AssertionError(lib.name() + " insertMid size=" + lib.size(vec));

				// removeMid：持久化中间删除（删当前中点）
				vec = lib.copy(base);
				for (int i = 0; i < OPS; i++) // 预热
					vec = lib.removeAt(vec, (N - i) / 2);
				if (lib.size(vec) != N - OPS)
					throw new AssertionError(lib.name() + " removeMid warm size=" + lib.size(vec));
				vec = lib.copy(base);
				a0 = allocated();
				t0 = System.nanoTime();
				for (int i = 0; i < OPS; i++)
					vec = lib.removeAt(vec, (N - i) / 2);
				report(lib, "removeMid", OPS, System.nanoTime() - t0, allocated() - a0);
				if (lib.size(vec) != N - OPS)
					throw new AssertionError(lib.name() + " removeMid size=" + lib.size(vec));
			}

			// get：持久化随机命中读
			for (int i = 0; i < GET_OPS / 10; i++) // 预热
				localSink += lib.get(base, (i * 31 + 7) % N);
			localSink = 0;
			a0 = allocated();
			t0 = System.nanoTime();
			for (int i = 0; i < GET_OPS; i++)
				localSink += lib.get(base, (i * 31 + 7) % N);
			report(lib, "get", GET_OPS, System.nanoTime() - t0, allocated() - a0);
			sink += localSink;
			localSink = 0;

			// appendAll：持久化批量尾追加（plusAll/addAll）
			if (lib.bulkAppend()) {
				var bulk = new ArrayList<Integer>();
				for (int i = 0; i < OPS; i++)
					bulk.add(N + i);
				for (int w = 0; w < BULK_WARM; w++) { // 预热
					var r = lib.plusAll(lib.copy(base), bulk); // ArrayList 原地写：需副本隔离
					if (lib.size(r) != N + OPS)
						throw new AssertionError(lib.name() + " appendAll warm size=" + lib.size(r));
				}
				a0 = allocated();
				t0 = System.nanoTime();
				for (int it = 0; it < BULK_ITERS; it++)
					localSink += lib.size(lib.plusAll(lib.copy(base), bulk));
				report(lib, "appendAll", BULK_ITERS * OPS, System.nanoTime() - t0, allocated() - a0);
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

				// beginCommit1：begin + 1 次覆盖 + 提交（最小脏周期）
				for (int i = 0; i < BEGIN_ITERS / 4; i++) { // 预热
					var snap = lib.begin(base);
					snap = lib.snapSet(snap, i % N, i);
					localSink += lib.size(lib.commit(snap));
				}
				localSink = 0;
				a0 = allocated();
				t0 = System.nanoTime();
				for (int i = 0; i < BEGIN_ITERS; i++) {
					var snap = lib.begin(base);
					snap = lib.snapSet(snap, i % N, i);
					localSink += lib.size(lib.commit(snap));
				}
				report(lib, "beginCommit1", BEGIN_ITERS, System.nanoTime() - t0, allocated() - a0);
				sink += localSink;
				localSink = 0;

				// snapSet：周期 = begin + OPS 次覆盖 + commit
				for (int w = 0; w < WARM_CYCLES; w++) { // 预热
					var snap = lib.begin(base);
					for (int i = 0; i < OPS; i++)
						snap = lib.snapSet(snap, (i * 31 + 7) % N, i);
					var committed = lib.commit(snap);
					if (lib.size(committed) != N)
						throw new AssertionError(lib.name() + " snapSet warm size=" + lib.size(committed));
				}
				a0 = allocated();
				t0 = System.nanoTime();
				var last = base;
				for (int c = 0; c < CYCLES; c++) {
					var snap = lib.begin(base);
					for (int i = 0; i < OPS; i++)
						snap = lib.snapSet(snap, (i * 31 + 7) % N, i);
					last = lib.commit(snap);
					localSink += lib.size(last);
				}
				report(lib, "snapSet", CYCLES * OPS, System.nanoTime() - t0, allocated() - a0);
				if (lib.size(last) != N)
					throw new AssertionError(lib.name() + " snapSet size=" + lib.size(last));

				// snapAppend：周期 = begin + OPS 次尾追加 + commit
				for (int w = 0; w < WARM_CYCLES; w++) { // 预热
					var snap = lib.begin(base);
					for (int i = 0; i < OPS; i++)
						snap = lib.snapAppend(snap, N + i);
					var committed = lib.commit(snap);
					if (lib.size(committed) != N + OPS)
						throw new AssertionError(lib.name() + " snapAppend warm size=" + lib.size(committed));
				}
				a0 = allocated();
				t0 = System.nanoTime();
				last = base;
				for (int c = 0; c < CYCLES; c++) {
					var snap = lib.begin(base);
					for (int i = 0; i < OPS; i++)
						snap = lib.snapAppend(snap, N + i);
					last = lib.commit(snap);
					localSink += lib.size(last);
				}
				report(lib, "snapAppend", CYCLES * OPS, System.nanoTime() - t0, allocated() - a0);
				if (lib.size(last) != N + OPS)
					throw new AssertionError(lib.name() + " snapAppend size=" + lib.size(last));

				// snapPopTail：周期 = begin + OPS 次去尾 + commit
				for (int w = 0; w < WARM_CYCLES; w++) { // 预热
					var snap = lib.begin(base);
					for (int i = 0; i < OPS; i++)
						snap = lib.snapPopTail(snap);
					var committed = lib.commit(snap);
					if (lib.size(committed) != N - OPS)
						throw new AssertionError(lib.name() + " snapPopTail warm size=" + lib.size(committed));
				}
				a0 = allocated();
				t0 = System.nanoTime();
				last = base;
				for (int c = 0; c < CYCLES; c++) {
					var snap = lib.begin(base);
					for (int i = 0; i < OPS; i++)
						snap = lib.snapPopTail(snap);
					last = lib.commit(snap);
					localSink += lib.size(last);
				}
				report(lib, "snapPopTail", CYCLES * OPS, System.nanoTime() - t0, allocated() - a0);
				if (lib.size(last) != N - OPS)
					throw new AssertionError(lib.name() + " snapPopTail size=" + lib.size(last));

				// snapInsertMid/snapRemoveMid：周期 = begin + OPS 次中间插入/删除 + commit
				// （仅 kotlinx：Clojure vector 无中间操作 API）
				if (lib.midOps()) {
					for (int w = 0; w < WARM_CYCLES; w++) { // 预热
						var snap = lib.begin(base);
						for (int i = 0; i < OPS; i++)
							snap = lib.snapInsert(snap, (N + i) / 2, N + i);
						var committed = lib.commit(snap);
						if (lib.size(committed) != N + OPS)
							throw new AssertionError(lib.name() + " snapInsertMid warm size=" + lib.size(committed));
					}
					a0 = allocated();
					t0 = System.nanoTime();
					last = base;
					for (int c = 0; c < CYCLES; c++) {
						var snap = lib.begin(base);
						for (int i = 0; i < OPS; i++)
							snap = lib.snapInsert(snap, (N + i) / 2, N + i);
						last = lib.commit(snap);
						localSink += lib.size(last);
					}
					report(lib, "snapInsertMid", CYCLES * OPS, System.nanoTime() - t0, allocated() - a0);
					if (lib.size(last) != N + OPS)
						throw new AssertionError(lib.name() + " snapInsertMid size=" + lib.size(last));

					for (int w = 0; w < WARM_CYCLES; w++) { // 预热
						var snap = lib.begin(base);
						for (int i = 0; i < OPS; i++)
							snap = lib.snapRemoveAt(snap, (N - i) / 2);
						var committed = lib.commit(snap);
						if (lib.size(committed) != N - OPS)
							throw new AssertionError(lib.name() + " snapRemoveMid warm size=" + lib.size(committed));
					}
					a0 = allocated();
					t0 = System.nanoTime();
					last = base;
					for (int c = 0; c < CYCLES; c++) {
						var snap = lib.begin(base);
						for (int i = 0; i < OPS; i++)
							snap = lib.snapRemoveAt(snap, (N - i) / 2);
						last = lib.commit(snap);
						localSink += lib.size(last);
					}
					report(lib, "snapRemoveMid", CYCLES * OPS, System.nanoTime() - t0, allocated() - a0);
					if (lib.size(last) != N - OPS)
						throw new AssertionError(lib.name() + " snapRemoveMid size=" + lib.size(last));
				}

				// snapGet：单快照 + 随机命中读（提交不计入测量）
				var snap = lib.begin(base);
				for (int i = 0; i < GET_OPS / 10; i++) // 预热
					localSink += lib.snapGet(snap, (i * 31 + 7) % N);
				localSink = 0;
				a0 = allocated();
				t0 = System.nanoTime();
				for (int i = 0; i < GET_OPS; i++)
					localSink += lib.snapGet(snap, (i * 31 + 7) % N);
				report(lib, "snapGet", GET_OPS, System.nanoTime() - t0, allocated() - a0);
				sink += localSink;
				localSink = 0;
				sink += lib.size(lib.commit(snap)); // 收尾提交（不计时）
			}
			sink += localSink;
			System.out.println();
		}
	}
}
