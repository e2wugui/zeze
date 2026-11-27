package Zeze.Util;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongFunction;
import org.jspecify.annotations.Nullable;
import sun.misc.Unsafe;

/**
 * 原始 {@code long} 键高并发哈希表（移植自 JDK ConcurrentHashMap，避免装箱开销；不支持 null 值）。
 */
public final class LongConcurrentHashMap<V> implements LongMap<V> {
	/** 最大表容量（1<<30，高 2 位保留作控制位）。 */
	private static final int MAXIMUM_CAPACITY = 1 << 30;

	/** 默认初始容量（须为 2 的幂）。 */
	private static final int DEFAULT_CAPACITY = 16;

	/** 扩容迁移时每个线程每次认领的最小桶数步长。 */
	private static final int MIN_TRANSFER_STRIDE = 16;

	/** sizeCtl 中用于记录扩容代际戳的位数。 */
	private static final int RESIZE_STAMP_BITS = 16;

	/** 可并发协助扩容的最大线程数。 */
	private static final int MAX_RESIZERS = (1 << (32 - RESIZE_STAMP_BITS)) - 1;

	/** sizeCtl 中扩容代际戳的位偏移。 */
	private static final int RESIZE_STAMP_SHIFT = 32 - RESIZE_STAMP_BITS;

	/** 可用 CPU 核心数，用于扩容步长等界限。 */
	private static final int NCPU = Runtime.getRuntime().availableProcessors();

	/** 哈希桶数组（首次插入时懒初始化，长度恒为 2 的幂）。 */
	private volatile @Nullable Node<V> @Nullable [] table;

	/** 扩容目标表，仅在扩容期间非空。 */
	private volatile @Nullable Node<V> @Nullable [] nextTable;

	/** 基础计数（无竞争时直接 CAS 更新，竞争时分摊至 counterCells）。 */
	@SuppressWarnings("unused")
	private volatile long baseCount;

	/**
	 * 初始化与扩容控制字：负值表示初始化(-1)或扩容中；表未建时保存初始容量；建成后保存下次扩容阈值。
	 */
	private volatile int sizeCtl;

	/** 扩容迁移时的下一个待分配桶索引上界。 */
	private volatile int transferIndex;

	/** 扩容或创建 CounterCells 时的 CAS 自旋锁。 */
	private volatile int cellsBusy;

	/** 分段计数槽数组，非空时长度为 2 的幂。 */
	private volatile @Nullable CounterCell @Nullable [] counterCells;

	/** 键值节点；ForwardingNode 等特殊子类的 val 为 null，常规节点 val 恒非空。 */
	private static class Node<V> {
		private final long key;
		private volatile @Nullable V val;
		private volatile @Nullable Node<V> next;

		private Node(long key, @Nullable V val, @Nullable Node<V> next) {
			this.key = key;
			this.val = val;
			this.next = next;
		}
	}

	/** 扩容迁移期间放在已迁移桶头部的转发节点。 */
	private static final class ForwardingNode<V> extends Node<V> {
		final @Nullable Node<V>[] nextTable;

		ForwardingNode(@Nullable Node<V>[] tab) {
			super(0, null, null);
			nextTable = tab;
		}

		@SuppressWarnings("ConstantConditions")
	@Nullable Node<V> find(int h, long k) {
			// 循环推进，避免连续转发导致深层递归
			for (@Nullable Node<V>[] tab = nextTable; ; ) {
				Node<V> e;
				int n;
				if (tab == null || (n = tab.length) == 0 || (e = tabAt(tab, h & (n - 1))) == null)
					return null;
				for (; ; ) {
					if (e.val == null) { // MOVED
						tab = ((ForwardingNode<V>)e).nextTable;
						break;
					}
					if (e.key == k)
						return e;
					if ((e = e.next) == null)
						return null;
				}
			}
		}
	}

	/** 折叠 64 位 key 的高低 32 位并做高位扩散，避免仅高 32 位不同的键聚集在同一桶。 */
	private static int spread(long key) {
		int h = Long.hashCode(key);
		return (h ^ (h >>> 16)) & 0x7fffffff;
	}

	/** 返回不小于期望容量的最小 2 的幂。 */
	private static int tableSizeFor(int n) {
		n--;
		n |= n >>> 1;
		n |= n >>> 2;
		n |= n >>> 4;
		n |= n >>> 8;
		n |= n >>> 16;
		return n < 0 ? 1 : (n >= MAXIMUM_CAPACITY ? MAXIMUM_CAPACITY : n + 1);
	}

	// 表元素与扩容目标表的 volatile 访问方法；调用方须在局部变量上判空和检查长度。

	@SuppressWarnings("unchecked")
	private static <V> @Nullable Node<V> tabAt(@Nullable Node<V>[] tab, int i) {
		return (Node<V>)U.getObjectVolatile(tab, ((long)i << ASHIFT) + ABASE);
	}

	private static <V> boolean casTabAt(@Nullable Node<V>[] tab, int i, Node<V> v) {
		return U.compareAndSwapObject(tab, ((long)i << ASHIFT) + ABASE, null, v);
	}

	private static <V> void setTabAt(@Nullable Node<V>[] tab, int i, @Nullable Node<V> v) {
		U.putObjectVolatile(tab, ((long)i << ASHIFT) + ABASE, v);
	}

	/** 返回容量为 n 的表扩容代际戳（左移 RESIZE_STAMP_SHIFT 后必为负）。 */
	private static int resizeStamp(int n) {
		return Integer.numberOfLeadingZeros(n) | (1 << (RESIZE_STAMP_BITS - 1));
	}

	/** 创建默认初始容量（16）的空表。 */
	public LongConcurrentHashMap() {
	}

	/** 按指定初始容量创建空表。 */
	public LongConcurrentHashMap(int initialCapacity) {
		if (initialCapacity < 0)
			throw new IllegalArgumentException();
		sizeCtl = ((initialCapacity >= (MAXIMUM_CAPACITY >>> 1)) ? MAXIMUM_CAPACITY : tableSizeFor(initialCapacity + (initialCapacity >>> 1) + 1));
	}

	/** 按初始容量和负载因子创建空表。 */
	public LongConcurrentHashMap(int initialCapacity, float loadFactor) {
		this(initialCapacity, loadFactor, 1);
	}

	/** 按初始容量、负载因子和预估并发更新线程数创建空表。 */
	public LongConcurrentHashMap(int initialCapacity, float loadFactor, int concurrencyLevel) {
		if (loadFactor <= 0.0f || initialCapacity < 0 || concurrencyLevel <= 0)
			throw new IllegalArgumentException();
		if (initialCapacity < concurrencyLevel)
			initialCapacity = concurrencyLevel;
		long size = (long)(1.0 + initialCapacity / loadFactor);
		sizeCtl = (size >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : tableSizeFor((int)size);
	}

	@Override
	public int size() {
		long n = sumCount();
		return ((n < 0L) ? 0 : (n > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int)n);
	}

	@Override
	public boolean isEmpty() {
		return sumCount() <= 0L; // 忽略瞬态负值
	}

	@Override
	public @Nullable V get(long key) {
		@Nullable Node<V>[] tab;
		Node<V> e, p;
		int n;
		int h = spread(key);
		if ((tab = table) != null && (n = tab.length) > 0 && (e = tabAt(tab, h & (n - 1))) != null) {
			if (e.val != null) {
				do {
					if (e.key == key)
						return e.val;
				}
				while ((e = e.next) != null);
			} else // MOVED
				return (p = ((ForwardingNode<V>)e).find(h, key)) != null ? p.val : null;
		}
		return null;
	}

	@Override
	public boolean containsKey(long key) {
		return get(key) != null;
	}

	@Override
	@SuppressWarnings("ConstantConditions")
	public boolean containsValue(V value) {
		if (value == null)
			throw new NullPointerException();
		@Nullable Node<V>[] t;
		if ((t = table) != null) {
			Traverser<V> it = new Traverser<>(t, t.length);
			for (Node<V> p; (p = it.advance()) != null; ) {
				V v = p.val;
				//noinspection PointlessNullCheck
				if (v == value || (v != null && value.equals(v)))
					return true;
			}
		}
		return false;
	}

	@Override
	public @Nullable V put(long key, V value) {
		return putVal(key, value, false);
	}

	/** put 与 putIfAbsent 的公共实现。 */
	@SuppressWarnings("ConstantConditions")
	private @Nullable V putVal(long key, V value, boolean onlyIfAbsent) {
		if (value == null)
			throw new NullPointerException();
		int hash = spread(key);
		int binCount = 0;
		for (@Nullable Node<V>[] tab = table; ; ) {
			Node<V> f;
			int n, i;
			if (tab == null || (n = tab.length) == 0)
				tab = initTable();
			else if ((f = tabAt(tab, i = hash & (n - 1))) == null) {
				if (casTabAt(tab, i, new Node<>(key, value, null)))
					break; // 空桶无锁 CAS 插入
			} else if (f.val == null) // MOVED
				tab = helpTransfer(tab, f);
			else {
				V oldVal = null;
				synchronized (f) {
					if (tabAt(tab, i) == f) {
						binCount = 1;
						for (Node<V> e = f; ; ++binCount) {
							if (e.key == key) {
								oldVal = e.val;
								if (!onlyIfAbsent)
									e.val = value;
								break;
							}
							Node<V> pred = e;
							if ((e = e.next) == null) {
								pred.next = new Node<>(key, value, null);
								break;
							}
						}
					}
				}
				if (binCount != 0) {
					if (oldVal != null)
						return oldVal;
					break;
				}
			}
		}
		addCount(1L, binCount);
		return null;
	}

	// 注意这个computeIfAbsent并不等同于ConcurrentHashMap里的,mappingFunction有小概率无效执行,不适用于有非托管资源分配
	@Override
	public @Nullable V computeIfAbsent(long key, LongFunction<? extends @Nullable V> mappingFunction) {
		V value = get(key);
		if (value != null)
			return value;
		value = mappingFunction.apply(key);
		if (value == null)
			return get(key);
		V oldValue = putIfAbsent(key, value);
		return oldValue != null ? oldValue : value;
	}

	@Override
	public @Nullable V remove(long key) {
		return replaceNode(key, null, null);
	}

	/** remove/replace 公共实现：若 cv 非空则匹配后替换，新值为 null 则删除节点。 */
	private @Nullable V replaceNode(long key, @Nullable V value, @Nullable Object cv) {
		int hash = spread(key);
		for (@Nullable Node<V>[] tab = table; ; ) {
			Node<V> f;
			int n, i;
			if (tab == null || (n = tab.length) == 0 || (f = tabAt(tab, i = hash & (n - 1))) == null)
				break;
			if (f.val == null) // MOVED
				tab = helpTransfer(tab, f);
			else {
				V oldVal = null;
				boolean validated = false;
				synchronized (f) {
					if (tabAt(tab, i) == f) {
						validated = true;
						for (Node<V> e = f, pred = null; ; ) {
							if (e.key == key) {
								V ev = e.val;
								//noinspection PointlessNullCheck
								if (cv == null || cv == ev || (ev != null && cv.equals(ev))) {
									oldVal = ev;
									if (value != null)
										e.val = value;
									else if (pred != null)
										pred.next = e.next;
									else
										setTabAt(tab, i, e.next);
								}
								break;
							}
							pred = e;
							if ((e = e.next) == null)
								break;
						}
					}
				}
				if (validated) {
					if (oldVal != null) {
						if (value == null)
							addCount(-1L, -1);
						return oldVal;
					}
					break;
				}
			}
		}
		return null;
	}

	@Override
	public void clear() {
		long delta = 0L; // 累计删除数（负值）
		int i = 0;
		@Nullable Node<V>[] tab = table;
		while (tab != null && i < tab.length) {
			Node<V> f = tabAt(tab, i);
			if (f == null)
				++i;
			else if (f.val == null) { // MOVED
				tab = helpTransfer(tab, f);
				i = 0;
			} else {
				synchronized (f) {
					if (tabAt(tab, i) == f) {
						do {
							--delta;
							f = f.next;
						}
						while (f != null);
						setTabAt(tab, i++, null);
					}
				}
			}
		}
		if (delta != 0L)
			addCount(delta, -1);
	}

	@Override
	public String toString() {
		@Nullable Node<V>[] t;
		int f = (t = table) == null ? 0 : t.length;
		Traverser<V> it = new Traverser<>(t, f);
		StringBuilder sb = new StringBuilder();
		sb.append('{');
		Node<V> p;
		if ((p = it.advance()) != null) {
			for (; ; ) {
				long k = p.key;
				V v = p.val;
				sb.append(k);
				sb.append('=');
				sb.append(v == this ? "(this Map)" : v);
				if ((p = it.advance()) == null)
					break;
				sb.append(", ");
			}
		}
		return sb.append('}').toString();
	}

	@Override
	@SuppressWarnings("ConstantConditions")
	public @Nullable V putIfAbsent(long key, V value) {
		return putVal(key, value, true);
	}

	@Override
	@SuppressWarnings("ConstantConditions")
	public boolean remove(long key, Object value) {
		return value != null && replaceNode(key, null, value) != null;
	}

	@Override
	@SuppressWarnings("ConstantConditions")
	public boolean replace(long key, V oldValue, V newValue) {
		if (oldValue == null || newValue == null)
			throw new NullPointerException();
		return replaceNode(key, newValue, oldValue) != null;
	}

	@Override
	@SuppressWarnings("ConstantConditions")
	public @Nullable V replace(long key, V value) {
		if (value == null)
			throw new NullPointerException();
		return replaceNode(key, value, null);
	}

	@Override
	public V getOrDefault(long key, V defaultValue) {
		V v;
		return (v = get(key)) == null ? defaultValue : v;
	}

	/** 返回映射条数的估算值（支持超过 Integer.MAX_VALUE）。 */
	public long mappingCount() {
		return Math.max(sumCount(), 0L); // 忽略瞬态负值
	}

	/** 按 sizeCtl 中记录的容量懒初始化哈希桶数组。 */
	private @Nullable Node<V>[] initTable() {
		@Nullable Node<V>[] tab;
		int sc;
		while ((tab = table) == null || tab.length == 0) {
			if ((sc = sizeCtl) < 0)
				Thread.yield();
			else if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
				try {
					if ((tab = table) == null || tab.length == 0) {
						int n = (sc > 0) ? sc : DEFAULT_CAPACITY;
						@SuppressWarnings("unchecked")
						@Nullable Node<V>[] nt = (@Nullable Node<V>[])new Node<?>[n];
						table = tab = nt;
						sc = n - (n >>> 2);
					}
				} finally {
					sizeCtl = sc;
				}
				break;
			}
		}
		return tab;
	}

	private static final class ThreadLocalRandom {
		private static final class Probe {
			int probe;
		}

		private static final AtomicInteger probeGenerator = new AtomicInteger();
		private static final ThreadLocal<@Nullable Probe> tlProb = new ThreadLocal<>();

		static void localInit() {
			int p = probeGenerator.addAndGet(0x9e3779b9);
			Probe probe = new Probe();
			probe.probe = (p == 0 ? 1 : p); // skip 0
			tlProb.set(probe);
		}

		static int getProbe() {
			Probe probe = tlProb.get();
			return probe != null ? probe.probe : 0;
		}

		static int advanceProbe(int probe) {
			probe ^= probe << 13; // xorshift
			probe ^= probe >>> 17;
			probe ^= probe << 5;
			//noinspection DataFlowIssue
			tlProb.get().probe = probe;
			return probe;
		}
	}

	/**
	 * 更新元素计数并在需要时发起或协助扩容迁移。
	 *
	 * @param x     计数增量
	 * @param check &lt;0 不检查扩容，&lt;=1 仅无竞争时检查
	 */
	private void addCount(long x, int check) {
		@Nullable CounterCell[] as;
		long b, s;
		if ((as = counterCells) != null || !U.compareAndSwapLong(this, BASECOUNT, b = baseCount, s = b + x)) {
			CounterCell a;
			long v;
			int m;
			boolean uncontended = true;
			if (as == null || (m = as.length - 1) < 0 || (a = as[ThreadLocalRandom.getProbe() & m]) == null ||
					!(uncontended = U.compareAndSwapLong(a, CELLVALUE, v = a.value, v + x))) {
				fullAddCount(x, uncontended);
				return;
			}
			if (check <= 1)
				return;
			s = sumCount();
		}
		if (check >= 0) {
			@Nullable Node<V>[] tab, nt;
			int n, sc;
			while (s >= (sc = sizeCtl) && (tab = table) != null && (n = tab.length) < MAXIMUM_CAPACITY) {
				int rs = resizeStamp(n);
				int resizeBase = rs << RESIZE_STAMP_SHIFT;
				if (sc < 0) {
					//noinspection ConstantConditions
					// sizeCtl 高位为代际、低位为参与者：边界必须比较完整编码。
					if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == resizeBase + 1
							|| sc == resizeBase + MAX_RESIZERS || (nt = nextTable) == null || transferIndex <= 0)
						break;
					if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1))
						transfer(tab, nt);
				} else if (U.compareAndSwapInt(this, SIZECTL, sc, resizeBase + 2))
					transfer(tab, null);
				s = sumCount();
			}
		}
	}

	/** 若正在扩容则协助迁移桶节点。 */
	@SuppressWarnings("ConstantConditions")
	private @Nullable Node<V> @Nullable [] helpTransfer(@Nullable Node<V> @Nullable [] tab, Node<V> f) {
		@Nullable Node<V>[] nextTab;
		int sc;
		if (tab != null && f.val == null && (nextTab = ((ForwardingNode<V>)f).nextTable) != null) { // MOVED
			int rs = resizeStamp(tab.length);
			int resizeBase = rs << RESIZE_STAMP_SHIFT;
			while (nextTab == nextTable && table == tab && (sc = sizeCtl) < 0) {
				//noinspection ConstantConditions
				if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == resizeBase + 1
						|| sc == resizeBase + MAX_RESIZERS || transferIndex <= 0)
					break;
				if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1)) {
					transfer(tab, nextTab);
					break;
				}
			}
			return nextTab;
		}
		return table;
	}

	/** 将旧表各桶节点迁移或拷贝到扩容后的新表。 */
	private void transfer(@Nullable Node<V>[] tab, @Nullable Node<V> @Nullable [] nextTab) {
		int n = tab.length, stride;
		if ((stride = (NCPU > 1) ? (n >>> 3) / NCPU : n) < MIN_TRANSFER_STRIDE)
			stride = MIN_TRANSFER_STRIDE;
		if (nextTab == null) {
			try {
				@SuppressWarnings("unchecked")
				@Nullable Node<V>[] nt = (@Nullable Node<V>[])new Node<?>[n << 1];
				nextTab = nt;
			} catch (Throwable ex) { // OOME 兜底
				sizeCtl = Integer.MAX_VALUE;
				return;
			}
			nextTable = nextTab;
			transferIndex = n;
		}
		int nextn = nextTab.length;
		ForwardingNode<V> fwd = new ForwardingNode<>(nextTab);
		boolean advance = true;
		boolean finishing = false; // 提交 nextTab 前做一次全表复查
		for (int i = 0, bound = 0; ; ) {
			Node<V> f;
			while (advance) {
				int nextIndex, nextBound;
				if (--i >= bound || finishing)
					advance = false;
				else if ((nextIndex = transferIndex) <= 0) {
					i = -1;
					advance = false;
				} else if (U.compareAndSwapInt(this, TRANSFERINDEX, nextIndex, nextBound = (nextIndex > stride ? nextIndex - stride : 0))) {
					bound = nextBound;
					i = nextIndex - 1;
					advance = false;
				}
			}
			if (i < 0 || i >= n || i + n >= nextn) {
				int sc;
				if (finishing) {
					nextTable = null;
					table = nextTab;
					sizeCtl = (n << 1) - (n >>> 1);
					return;
				}
				if (U.compareAndSwapInt(this, SIZECTL, sc = sizeCtl, sc - 1)) {
					if ((sc - 2) != resizeStamp(n) << RESIZE_STAMP_SHIFT)
						return;
					finishing = advance = true;
					i = n; // 提交前复查全表
				}
			} else if ((f = tabAt(tab, i)) == null)
				advance = casTabAt(tab, i, fwd);
			else if (f.val == null) // MOVED
				advance = true;
			else {
				synchronized (f) {
					if (tabAt(tab, i) == f) {
						Node<V> ln, hn;
						int runBit = spread(f.key) & n;
						Node<V> lastRun = f;
						for (Node<V> p = f.next; p != null; p = p.next) {
							int b = spread(p.key) & n;
							if (b != runBit) {
								runBit = b;
								lastRun = p;
							}
						}
						if (runBit == 0) {
							ln = lastRun;
							hn = null;
						} else {
							hn = lastRun;
							ln = null;
						}
						for (Node<V> p = f; p != lastRun; p = p.next) {
							// lastRun 是此链上的非空节点；p 在抵达 lastRun 前不会为 null。
						//noinspection DataFlowIssue
						long pk = p.key;
							V pv = p.val;
							if ((spread(p.key) & n) == 0)
								ln = new Node<>(pk, pv, ln);
							else
								hn = new Node<>(pk, pv, hn);
						}
						setTabAt(nextTab, i, ln);
						setTabAt(nextTab, i + n, hn);
						setTabAt(tab, i, fwd);
						advance = true;
					}
				}
			}
		}
	}

	/** 缓存行填充的分段计数单元（参考 Striped64）。 */
	// @sun.misc.Contended
	private static final class CounterCell {
		@SuppressWarnings("unused")
		private volatile long p0, p1, p2, p3, p4, p5, p6;
		@SuppressWarnings({"FieldMayBeFinal", "CanBeFinal"})
		private volatile long value;
		@SuppressWarnings("unused")
		private volatile long q0, q1, q2, q3, q4, q5, q6;

		private CounterCell(long x) {
			value = x;
		}
	}

	private long sumCount() {
		@Nullable CounterCell[] as = counterCells;
		long sum = baseCount;
		if (as != null) {
			for (CounterCell a : as)
				if (a != null)
					sum += a.value;
		}
		return sum;
	}

	// See LongAdder version for explanation
	private void fullAddCount(long x, boolean wasUncontended) {
		int h;
		if ((h = ThreadLocalRandom.getProbe()) == 0) {
			ThreadLocalRandom.localInit(); // force initialization
			h = ThreadLocalRandom.getProbe();
			wasUncontended = true;
		}
		boolean collide = false; // True if last slot nonempty
		for (; ; ) {
			@Nullable CounterCell[] as;
			CounterCell a;
			int n;
			long v;
			if ((as = counterCells) != null && (n = as.length) > 0) {
				if ((a = as[h & (n - 1)]) == null) {
					if (cellsBusy == 0) { // Try to attach new Cell
						CounterCell r = new CounterCell(x); // Optimistic create
						if (cellsBusy == 0 && U.compareAndSwapInt(this, CELLSBUSY, 0, 1)) {
							boolean created = false;
							try { // Recheck under lock
								@Nullable CounterCell[] rs;
								int m, j;
								if ((rs = counterCells) != null && (m = rs.length) > 0 && rs[j = h & (m - 1)] == null) {
									rs[j] = r;
									created = true;
								}
							} finally {
								cellsBusy = 0;
							}
							if (created)
								break;
							continue; // Slot is now non-empty
						}
					}
					collide = false;
				} else if (!wasUncontended) // CAS already known to fail
					wasUncontended = true; // Continue after rehash
				else if (U.compareAndSwapLong(a, CELLVALUE, v = a.value, v + x))
					break;
				else if (counterCells != as || n >= NCPU)
					collide = false; // At max size or stale
				else if (!collide)
					collide = true;
				else if (cellsBusy == 0 && U.compareAndSwapInt(this, CELLSBUSY, 0, 1)) {
					try {
						if (counterCells == as) // Expand table unless stale
							counterCells = Arrays.copyOf(as, n << 1, CounterCell[].class);
					} finally {
						cellsBusy = 0;
					}
					collide = false;
					continue; // Retry with expanded table
				}
				h = ThreadLocalRandom.advanceProbe(h);
			} else if (cellsBusy == 0 && counterCells == as && U.compareAndSwapInt(this, CELLSBUSY, 0, 1)) {
				boolean init = false;
				try { // Initialize table
					if (counterCells == as) {
						@Nullable CounterCell[] rs = new CounterCell[2];
						rs[h & 1] = new CounterCell(x);
						counterCells = rs;
						init = true;
					}
				} finally {
					cellsBusy = 0;
				}
				if (init)
					break;
			} else if (U.compareAndSwapLong(this, BASECOUNT, v = baseCount, v + x))
				break; // Fall back on using base
		}
	}

	@Override
	public LongIterator keyIterator() {
		@Nullable Node<V>[] t;
		int f = (t = table) == null ? 0 : t.length;
		return new KeyIterator<>(t, f);
	}

	@Override
	public Iterator<V> iterator() {
		@Nullable Node<V>[] t;
		int f = (t = table) == null ? 0 : t.length;
		return new ValueIterator<>(t, f);
	}

	@Override
	public MapIterator<V> entryIterator() {
		@Nullable Node<V>[] t;
		int f = (t = table) == null ? 0 : t.length;
		return new EntryIterator<>(t, f);
	}

	/** 遍历遇到转发节点时保存的旧表遍历上下文栈节点。 */
	private static final class TableStack<V> {
		private int length;
		private int index;
		private @Nullable Node<V> @Nullable [] tab;
		private @Nullable TableStack<V> next;
	}

	/** 弱一致遍历器基类：支持遍历期间并发扩容（通过 TableStack 回溯转发前的表区间）。 */
	private static class Traverser<V> {
		private @Nullable Node<V> @Nullable [] tab; // 当前遍历表（扩容时切换）
		protected @Nullable Node<V> next; // 下一个待返回节点
		private @Nullable TableStack<V> stack, spare; // 遇到 ForwardingNode 时压栈/复用
		private int index; // 下一个访问的桶下标
		private int baseIndex; // 初始表当前桶下标
		private final int baseSize; // 初始表容量

		private Traverser(@Nullable Node<V> @Nullable [] tab, int size) {
			this.tab = tab;
			baseSize = size;
		}

		/** 前进到下一个有效节点，遍历结束返回 null。 */
		protected final @Nullable Node<V> advance() {
			Node<V> e = next;
			if (e != null)
				e = e.next;
			for (; ; ) {
				@Nullable Node<V>[] t;
				int i, n;
				if (e != null)
					return next = e;
				if (baseIndex >= baseSize || (t = tab) == null || (n = t.length) <= (i = index) || i < 0)
					return next = null;
				if ((e = tabAt(t, i)) != null && e.val == null) { // MOVED
					tab = ((ForwardingNode<V>)e).nextTable;
					e = null;
					pushState(t, i, n);
					continue;
				}
				if (stack != null)
					recoverState(n);
				else if ((index = i + baseSize) >= n)
					index = ++baseIndex;
			}
		}

		/** 遇到转发节点时压栈保存当前表遍历状态。 */
		private void pushState(@Nullable Node<V>[] t, int i, int n) {
			TableStack<V> s = spare;
			if (s != null)
				spare = s.next;
			else
				s = new TableStack<>();
			s.tab = t;
			s.length = n;
			s.index = i;
			s.next = stack;
			stack = s;
		}

		/** 当前转发区间遍历完毕后弹栈恢复外层表状态。 */
		private void recoverState(int n) {
			TableStack<V> s;
			int len;
			while ((s = stack) != null && (index += (len = s.length)) >= n) {
				n = len;
				index = s.index;
				tab = s.tab;
				s.tab = null;
				TableStack<V> next2 = s.next;
				s.next = spare;
				stack = next2;
				spare = s;
			}
			if (s == null && (index += baseSize) >= n)
				index = ++baseIndex;
		}
	}

	/** Key/Value/Entry 迭代器基类。 */
	private abstract static class BaseIterator<V> extends Traverser<V> {
		private BaseIterator(@Nullable Node<V> @Nullable [] tab, int size) {
			super(tab, size);
			advance();
		}

		public final boolean hasNext() {
			return next != null;
		}

		@SuppressWarnings("static-method")
		@Deprecated // unsupported
		public final void remove() {
			throw new UnsupportedOperationException();
		}
	}

	private static final class KeyIterator<V> extends BaseIterator<V> implements LongIterator {
		private KeyIterator(@Nullable Node<V> @Nullable [] tab, int size) {
			super(tab, size);
		}

		@Override
		public long next() {
			Node<V> p;
			if ((p = next) == null)
				throw new NoSuchElementException();
			long k = p.key;
			advance();
			return k;
		}
	}

	private static final class ValueIterator<V> extends BaseIterator<V> implements Iterator<V> {
		private ValueIterator(@Nullable Node<V> @Nullable [] tab, int size) {
			super(tab, size);
		}

		@Override
		public V next() {
			Node<V> p;
			if ((p = next) == null)
				throw new NoSuchElementException();
			V v = p.val;
			assert v != null; // Traverser 不返回 val 为 null 的 ForwardingNode。
			advance();
			return v;
		}
	}

	private static final class EntryIterator<V> extends BaseIterator<V> implements MapIterator<V> {
		private @Nullable Node<V> node;

		EntryIterator(@Nullable Node<V> @Nullable [] tab, int size) {
			super(tab, size);
		}

		@Override
		public boolean moveToNext() {
			if ((node = next) == null)
				return false;
			advance();
			return true;
		}

		@Override
		public long key() {
			//noinspection DataFlowIssue
			return node.key;
		}

		@Override
		public V value() {
			var node = this.node;
			assert node != null;
			var value = node.val;
			assert value != null; // 当前条目已经过 Traverser 筛选。
			return value;
		}
	}

	// Unsafe 字段偏移量
	private static final Unsafe U;
	private static final long SIZECTL;
	private static final long TRANSFERINDEX;
	private static final long BASECOUNT;
	private static final long CELLSBUSY;
	private static final long CELLVALUE;
	private static final long ABASE;
	private static final int ASHIFT;

	private static Unsafe getUnsafe() throws IllegalAccessException {
		Class<Unsafe> k = Unsafe.class;
		for (Field f : k.getDeclaredFields()) {
			f.setAccessible(true);
			Object x = f.get(null);
			if (k.isInstance(x))
				return k.cast(x);
		}
		throw new NoSuchFieldError("the Unsafe");
	}

	@SuppressWarnings("deprecation")
	private static long objectFieldOffset(Field field) {
		return U.objectFieldOffset(field);
	}

	static {
		try {
			U = getUnsafe();
			Class<?> k = LongConcurrentHashMap.class;
			SIZECTL = objectFieldOffset(k.getDeclaredField("sizeCtl"));
			TRANSFERINDEX = objectFieldOffset(k.getDeclaredField("transferIndex"));
			BASECOUNT = objectFieldOffset(k.getDeclaredField("baseCount"));
			CELLSBUSY = objectFieldOffset(k.getDeclaredField("cellsBusy"));
			Class<?> ck = CounterCell.class;
			CELLVALUE = objectFieldOffset(ck.getDeclaredField("value"));
			Class<?> ak = Node[].class;
			ABASE = U.arrayBaseOffset(ak);
			int scale = U.arrayIndexScale(ak);
			if ((scale & (scale - 1)) != 0)
				throw new Error("data type scale not a power of two");
			ASHIFT = 31 - Integer.numberOfLeadingZeros(scale);
		} catch (Exception e) {
			throw new ExceptionInInitializerError(e);
		}
	}
}
