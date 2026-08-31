package Benchmark;

import java.lang.management.ManagementFactory;
import java.util.HashMap;

import clojure.lang.IEditableCollection;
import clojure.lang.IPersistentMap;
import clojure.lang.ITransientAssociative2;
import clojure.lang.ITransientMap;
import clojure.lang.PersistentHashMap;
import harness.Bench;
import kotlinx.collections.immutable.ExtensionsKt;
import kotlinx.collections.immutable.PersistentMap;
import org.junit.jupiter.api.Test;
import org.pcollections.Empty;
import org.pcollections.PMap;

/**
 * 可变快照（transient/builder）评估基准：Clojure(PersistentHashMap transient) vs
 * kotlinx(PersistentHashMap builder/CHAMP) vs pcollections(HashTreePMap，无快照，仅持久化操作对照)。
 *
 * 背景：Vavr 与 pcollections 均无 toTransient/builder 式可变快照 API（此前已实测确认）。
 * 本基准回答两个问题：
 * <ul>
 *   <li>开始/提交可变快照本身的成本（必须测量项）：begin（asTransient/builder）、
 *       beginCommit（空提交）、beginCommit1（begin+1写+提交，最小脏周期）；</li>
 *   <li>快照上的批量操作 vs pcollections：snapOverwrite/snapAdd/snapRemove/snapGet
 *       （周期 = begin + OPS 次操作 + commit，ns/op 按操作数折算）对照 pcollections 的
 *       overwrite/addNew/remove/get 与 bulkAdd（plusAll，pcollections 最接近快照批量的原生路径）。</li>
 * </ul>
 * 附带 Clojure/kotlinx 自身的持久化单笔写与读（overwrite/addNew/remove/get），用于归因
 * "快照收益"来自哪个环节；java.util.HashMap 作可变原地写下界参考（无版本化语义）。
 * Vavr 无快照（已实测），此篇不再引入。
 *
 * 互操作要点（javap 实测确认）：
 * <ul>
 *   <li>Clojure：asTransient 声明在 IEditableCollection；ATransientMap 为包私有且未实现
 *       ILookup，快照读须走 ITransientAssociative2.entryAt；transient 提交（persistent()）
 *       后即失效，每周期必须重新 begin；IPersistentMap.assoc/without 为版本化写。</li>
 *   <li>kotlinx：builder() 返回 PersistentMap.Builder（MutableMap 语义，put 返回旧值需忽略），
 *       build() 提交；空 map 即 PersistentHashMap，builder 可用（行为验证过）。</li>
 * </ul>
 *
 * 运行：gradlew :ZezeJavaTest:bench --tests "*BenchTransientSnapshotCompare"
 */
@Bench
public class BenchTransientSnapshotCompare {
	public static final int N = 100_000; // 基础 map 大小（条目数）
	public static final int OPS = 10_000; // 每个快照周期的操作数
	public static final int CYCLES = 10; // 快照场景测量周期数
	public static final int WARM_CYCLES = 2; // 快照场景预热周期数
	public static final int GET_OPS = 1_000_000;
	public static final int BEGIN_ITERS = 200_000; // begin/commit 场景迭代数
	public static final int BULK_ITERS = 20; // bulkAdd 场景迭代数（每次 plusAll OPS 条）
	public static final int BULK_WARM = 5;

	/** 防止死码消除的结果汇聚点。 */
	public static volatile long sink;

	private interface Lib {
		String name();

		boolean snapshot(); // 是否支持可变快照（transient/builder）

		boolean bulkPlus(); // 是否有原生持久化批量写（plusAll/putAll）

		Object empty();

		Object buildBase(int count); // 构建基础 map（兼作整体预热）

		Object put(Object map, int key, int value); // 持久化版本化写：返回新版本

		Object remove(Object map, int key); // 持久化版本化删除：返回新版本

		Object plusAll(Object map, HashMap<Integer, Integer> entries); // 持久化批量写

		int get(Object map, int key); // 命中返回值，未命中返回 Integer.MIN_VALUE

		int size(Object map);

		/** 场景隔离用副本：持久化库 O(1) 返回自身（版本不可变），HashMap 真实拷贝。 */
		Object copy(Object map);

		// ---- 可变快照（仅 snapshot()==true 时可用）----

		Object begin(Object map); // 开始可变快照

		Object snapPut(Object snap, int key, int value); // 快照上写（原地，返回快照引用）

		Object snapRemove(Object snap, int key); // 快照上删（原地，返回快照引用）

		int snapGet(Object snap, int key);

		int snapSize(Object snap);

		Object commit(Object snap); // 提交快照，返回持久化版本
	}

	@SuppressWarnings("unchecked")
	private static IPersistentMap clj(Object map) {
		return (IPersistentMap)map;
	}

	private static ITransientMap cljT(Object snap) {
		return (ITransientMap)snap;
	}

	private static final Lib CLOJURE = new Lib() {
		@Override
		public String name() {
			return "Clojure";
		}

		@Override
		public boolean snapshot() {
			return true; // PersistentHashMap transient
		}

		@Override
		public boolean bulkPlus() {
			return false; // 无原生批量 API（create(Map) 仅能从零构建）
		}

		@Override
		public Object empty() {
			return PersistentHashMap.EMPTY;
		}

		@Override
		public Object buildBase(int count) {
			IPersistentMap map = PersistentHashMap.EMPTY;
			for (int i = 0; i < count; i++)
				map = map.assoc(i, i);
			return map;
		}

		@Override
		public Object put(Object map, int key, int value) {
			return clj(map).assoc(key, value);
		}

		@Override
		public Object remove(Object map, int key) {
			return clj(map).without(key);
		}

		@Override
		public Object plusAll(Object map, HashMap<Integer, Integer> entries) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int get(Object map, int key) {
			var v = clj(map).valAt(key);
			return v != null ? (Integer)v : Integer.MIN_VALUE;
		}

		@Override
		public int size(Object map) {
			return clj(map).count();
		}

		@Override
		public Object copy(Object map) {
			return map; // 版本不可变
		}

		@Override
		public Object begin(Object map) {
			return ((IEditableCollection)clj(map)).asTransient();
		}

		@Override
		public Object snapPut(Object snap, int key, int value) {
			return cljT(snap).assoc(key, value);
		}

		@Override
		public Object snapRemove(Object snap, int key) {
			return cljT(snap).without(key);
		}

		@Override
		public int snapGet(Object snap, int key) {
			var e = ((ITransientAssociative2)snap).entryAt(key);
			return e != null ? (Integer)e.getValue() : Integer.MIN_VALUE;
		}

		@Override
		public int snapSize(Object snap) {
			return cljT(snap).count();
		}

		@Override
		public Object commit(Object snap) {
			return cljT(snap).persistent();
		}
	};

	@SuppressWarnings("unchecked")
	private static PersistentMap<Integer, Integer> kx(Object map) {
		return (PersistentMap<Integer, Integer>)map;
	}

	@SuppressWarnings("unchecked")
	private static PersistentMap.Builder<Integer, Integer> kxB(Object snap) {
		return (PersistentMap.Builder<Integer, Integer>)snap;
	}

	private static final Lib KOTLINX = new Lib() {
		@Override
		public String name() {
			return "kotlinx";
		}

		@Override
		public boolean snapshot() {
			return true; // PersistentHashMap builder
		}

		@Override
		public boolean bulkPlus() {
			return true; // putAll
		}

		@Override
		public Object empty() {
			return ExtensionsKt.<Integer, Integer>persistentHashMapOf();
		}

		@Override
		public Object buildBase(int count) {
			PersistentMap<Integer, Integer> map = ExtensionsKt.persistentHashMapOf();
			for (int i = 0; i < count; i++)
				map = map.put(i, i);
			return map;
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
		public Object plusAll(Object map, HashMap<Integer, Integer> entries) {
			return kx(map).putAll(entries);
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
			return map; // 版本不可变
		}

		@Override
		public Object begin(Object map) {
			return kx(map).builder();
		}

		@Override
		public Object snapPut(Object snap, int key, int value) {
			kxB(snap).put(key, value); // MutableMap.put 返回旧值，忽略
			return snap;
		}

		@Override
		public Object snapRemove(Object snap, int key) {
			kxB(snap).remove(key); // MutableMap.remove 返回旧值，忽略
			return snap;
		}

		@Override
		public int snapGet(Object snap, int key) {
			var v = kxB(snap).get(key);
			return v != null ? v : Integer.MIN_VALUE;
		}

		@Override
		public int snapSize(Object snap) {
			return kxB(snap).size();
		}

		@Override
		public Object commit(Object snap) {
			return kxB(snap).build();
		}
	};

	@SuppressWarnings("unchecked")
	private static PMap<Integer, Integer> pc(Object map) {
		return (PMap<Integer, Integer>)map;
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
		public boolean bulkPlus() {
			return true; // plusAll
		}

		@Override
		public Object empty() {
			return Empty.<Integer, Integer>map(); // HashTreePMap
		}

		@Override
		public Object buildBase(int count) {
			var map = Empty.<Integer, Integer>map();
			for (int i = 0; i < count; i++)
				map = pc(map).plus(i, i);
			return map;
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
		public Object plusAll(Object map, HashMap<Integer, Integer> entries) {
			return pc(map).plusAll(entries);
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
			return map; // 版本不可变
		}

		@Override
		public Object begin(Object map) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object snapPut(Object snap, int key, int value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object snapRemove(Object snap, int key) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int snapGet(Object snap, int key) {
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
		public boolean snapshot() {
			return false;
		}

		@Override
		public boolean bulkPlus() {
			return true; // putAll
		}

		@Override
		public Object empty() {
			return new HashMap<Integer, Integer>();
		}

		@Override
		public Object buildBase(int count) {
			var m = new HashMap<Integer, Integer>(count * 2);
			for (int i = 0; i < count; i++)
				m.put(i, i);
			return m;
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
		public Object plusAll(Object map, HashMap<Integer, Integer> entries) {
			m(map).putAll(entries);
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
			return new HashMap<>(m(map)); // 场景隔离需真实拷贝
		}

		@Override
		public Object begin(Object map) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object snapPut(Object snap, int key, int value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object snapRemove(Object snap, int key) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int snapGet(Object snap, int key) {
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
		System.out.println("Clojure=1.12.0(transient)  kotlinx=0.3.7(builder/CHAMP)  "
				+ "pcollections=4.0.2(HashTreePMap,无快照)  HashMap=参考(原地写)\n");

		for (var lib : new Lib[] { CLOJURE, KOTLINX, PCOLLECTIONS, HASHMAP }) {
			var base = lib.buildBase(N);
			if (lib.size(base) != N)
				throw new AssertionError(lib.name() + " buildBase size=" + lib.size(base));
			var localSink = 0;

			// overwrite：持久化版本化覆盖已有 key
			var map = lib.copy(base);
			for (int i = 0; i < OPS; i++) // 预热
				map = lib.put(map, (i * 31 + 7) % N, i);
			map = lib.copy(base);
			long a0 = allocated();
			long t0 = System.nanoTime();
			for (int i = 0; i < OPS; i++)
				map = lib.put(map, (i * 31 + 7) % N, i);
			report(lib, "overwrite", OPS, System.nanoTime() - t0, allocated() - a0);
			if (lib.size(map) != N)
				throw new AssertionError(lib.name() + " overwrite size=" + lib.size(map));

			// addNew：持久化版本化新增 key
			map = lib.copy(base);
			for (int i = 0; i < OPS; i++) // 预热
				map = lib.put(map, N + i, i);
			map = lib.copy(base);
			a0 = allocated();
			t0 = System.nanoTime();
			for (int i = 0; i < OPS; i++)
				map = lib.put(map, N + i, i);
			report(lib, "addNew", OPS, System.nanoTime() - t0, allocated() - a0);
			if (lib.size(map) != N + OPS)
				throw new AssertionError(lib.name() + " addNew size=" + lib.size(map));

			// remove：持久化版本化前缀连续删除
			map = lib.copy(base);
			for (int i = 0; i < OPS; i++) // 预热
				map = lib.remove(map, i);
			map = lib.copy(base);
			a0 = allocated();
			t0 = System.nanoTime();
			for (int i = 0; i < OPS; i++)
				map = lib.remove(map, i);
			report(lib, "remove", OPS, System.nanoTime() - t0, allocated() - a0);
			if (lib.size(map) != N - OPS)
				throw new AssertionError(lib.name() + " remove size=" + lib.size(map));

			// get：持久化读吞吐
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

			// bulkAdd：持久化批量写（plusAll/putAll，pcollections 最接近快照批量的原生路径）
			if (lib.bulkPlus()) {
				var bulk = new HashMap<Integer, Integer>();
				for (int i = 0; i < OPS; i++)
					bulk.put(N + i, i);
				for (int w = 0; w < BULK_WARM; w++) { // 预热
					var r = lib.plusAll(lib.copy(base), bulk); // HashMap 原地写：需副本隔离
					if (lib.size(r) != N + OPS)
						throw new AssertionError(lib.name() + " bulkAdd warm size=" + lib.size(r));
				}
				a0 = allocated();
				t0 = System.nanoTime();
				for (int it = 0; it < BULK_ITERS; it++)
					localSink += lib.size(lib.plusAll(lib.copy(base), bulk));
				report(lib, "bulkAdd", BULK_ITERS * OPS, System.nanoTime() - t0, allocated() - a0);
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

				// beginCommit1：begin + 1 次覆盖 + 提交（最小脏周期，含 1 次写路径的提交成本）
				for (int i = 0; i < BEGIN_ITERS / 4; i++) { // 预热
					var snap = lib.begin(base);
					snap = lib.snapPut(snap, i % N, i);
					localSink += lib.size(lib.commit(snap));
				}
				localSink = 0;
				a0 = allocated();
				t0 = System.nanoTime();
				for (int i = 0; i < BEGIN_ITERS; i++) {
					var snap = lib.begin(base);
					snap = lib.snapPut(snap, i % N, i);
					localSink += lib.size(lib.commit(snap));
				}
				report(lib, "beginCommit1", BEGIN_ITERS, System.nanoTime() - t0, allocated() - a0);
				sink += localSink;
				localSink = 0;

				// snapOverwrite：周期 = begin + OPS 次覆盖 + commit
				for (int w = 0; w < WARM_CYCLES; w++) { // 预热
					var snap = lib.begin(base);
					for (int i = 0; i < OPS; i++)
						snap = lib.snapPut(snap, (i * 31 + 7) % N, i);
					var committed = lib.commit(snap);
					if (lib.size(committed) != N)
						throw new AssertionError(lib.name() + " snapOverwrite warm size=" + lib.size(committed));
				}
				a0 = allocated();
				t0 = System.nanoTime();
				var last = base;
				for (int c = 0; c < CYCLES; c++) {
					var snap = lib.begin(base);
					for (int i = 0; i < OPS; i++)
						snap = lib.snapPut(snap, (i * 31 + 7) % N, i);
					last = lib.commit(snap);
					localSink += lib.size(last);
				}
				report(lib, "snapOverwrite", CYCLES * OPS, System.nanoTime() - t0, allocated() - a0);
				if (lib.size(last) != N)
					throw new AssertionError(lib.name() + " snapOverwrite size=" + lib.size(last));

				// snapAdd：周期 = begin + OPS 次新增 + commit
				for (int w = 0; w < WARM_CYCLES; w++) { // 预热
					var snap = lib.begin(base);
					for (int i = 0; i < OPS; i++)
						snap = lib.snapPut(snap, N + i, i);
					var committed = lib.commit(snap);
					if (lib.size(committed) != N + OPS)
						throw new AssertionError(lib.name() + " snapAdd warm size=" + lib.size(committed));
				}
				a0 = allocated();
				t0 = System.nanoTime();
				last = base;
				for (int c = 0; c < CYCLES; c++) {
					var snap = lib.begin(base);
					for (int i = 0; i < OPS; i++)
						snap = lib.snapPut(snap, N + i, i);
					last = lib.commit(snap);
					localSink += lib.size(last);
				}
				report(lib, "snapAdd", CYCLES * OPS, System.nanoTime() - t0, allocated() - a0);
				if (lib.size(last) != N + OPS)
					throw new AssertionError(lib.name() + " snapAdd size=" + lib.size(last));

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
