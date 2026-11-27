package Zeze.Util;

import java.lang.ref.WeakReference;
import org.jspecify.annotations.Nullable;

/** 弱引用元素 Set（非线程安全）：条目随元素被 GC 回收自动清除。 */
public final class WeakHashSet<K> {
	/** 默认初始容量（必须为 2 的幂）。 */
	private static final int DEFAULT_INITIAL_CAPACITY = 1 << 8;

	/** 最大容量（必须为不超过 1<<30 的 2 的幂）。 */
	private static final int MAXIMUM_CAPACITY = 1 << 30;

	/** 默认负载因子。 */
	private static final float DEFAULT_LOAD_FACTOR = 0.75f;

	/** 哈希桶表，长度始终为 2 的幂。 */
	@SuppressWarnings("rawtypes")
	private @Nullable Entry[] table;

	private int size;
	/** 下次扩容阈值（capacity * loadFactor）。 */
	private int threshold;
	private final float loadFactor;

	/** 按指定初始容量和负载因子构造空的 WeakHashSet。 */
	public WeakHashSet(int initialCapacity, float loadFactor) {
		if (initialCapacity < 0)
			throw new IllegalArgumentException("Illegal Initial Capacity: " + initialCapacity);
		if (initialCapacity > MAXIMUM_CAPACITY)
			initialCapacity = MAXIMUM_CAPACITY;

		if (loadFactor <= 0 || Float.isNaN(loadFactor))
			throw new IllegalArgumentException("Illegal Load factor: " + loadFactor);
		int capacity = 1;
		while (capacity < initialCapacity)
			capacity <<= 1;
		table = new Entry[capacity];
		this.loadFactor = loadFactor;
		threshold = (int)(capacity * loadFactor);
	}

	/** 按指定初始容量和默认负载因子（0.75）构造空的 WeakHashSet。 */
	public WeakHashSet(int initialCapacity) {
		this(initialCapacity, DEFAULT_LOAD_FACTOR);
	}

	/** 按默认初始容量（256）和负载因子（0.75）构造空的 WeakHashSet。 */
	public WeakHashSet() {
		this.loadFactor = DEFAULT_LOAD_FACTOR;
		threshold = DEFAULT_INITIAL_CAPACITY;
		table = new Entry[DEFAULT_INITIAL_CAPACITY];
	}

	/** 对 hashCode 做二次扰动，降低 2 的幂次桶表下的低位碰撞。 */
	private static int hash(int h) {
		h ^= (h >>> 20) ^ (h >>> 12);
		return h ^ (h >>> 7) ^ (h >>> 4);
	}

	private static int indexFor(int h, int length) {
		return h & (length - 1);
	}

	/** 清理表内已被 GC 回收的过期条目。 */
	@SuppressWarnings("unchecked")
	private void expungeStaleEntries() {
		int n = table.length;
		for (int i = 0; i < n; ++i) {
			Entry<K> e = table[i];
			if (e == null)
				continue;
			Entry<K> prev = null;
			while (e != null) {
				K k = e.get();
				Entry<K> next = e.next;
				if (k == null) {
					if (prev == null)
						table[i] = next;
					else
						prev.next = next;
					e.next = null; // 协助 GC
					--size;
				} else {
					prev = e;
				}
				e = next;
			}
		}
	}

	/** 返回当前条目数快照（可能包含尚未清理的已回收弱引用条目）。 */
	public int size() {
		return size;
	}

	/** 查找并返回集合中已存的等值元素（非线程安全），不存在则返回 null。 */
	@SuppressWarnings({"unchecked", "ConstantConditions"})
	public @Nullable K get(final K k) {
		if (k == null)
			throw new NullPointerException();
		int h = hash(k.hashCode());
		int i = indexFor(h, table.length);
		Entry<K> prev = null;
		Entry<K> e = table[i];
		while (e != null) {
			K _k = e.get();
			Entry<K> next = e.next;
			if (_k == null) {
				--size;
				e.next = null; // 协助 GC
				if (prev == null)
					table[i] = next;
				else
					prev.next = next;
			} else {
				if (h == e.hash && k.equals(_k))
					return _k;
				prev = e;
			}
			e = next;
		}
		return null;
	}

	/**
	 * 若集合中尚无等值元素则加入并返回之；若已存在则不修改集合并返回旧元素。
	 *
	 * @return 新加入的元素，或已存在的等值旧元素
	 */
	@SuppressWarnings({"unchecked", "ConstantConditions"})
	public K add(final K k) {
		if (k == null)
			throw new NullPointerException();
		int h = hash(k.hashCode());
		int i = indexFor(h, table.length);
		Entry<K> prev = null;
		Entry<K> e = table[i];
		while (e != null) {
			K _k = e.get();
			Entry<K> next = e.next;
			if (_k == null) {
				--size;
				e.next = null; // 协助 GC
				if (prev == null)
					table[i] = next;
				else
					prev.next = next;
			} else {
				if (h == e.hash && k.equals(_k))
					return _k;
				prev = e;
			}
			e = next;
		}

		table[i] = new Entry<K>(k, h, table[i]);
		if (++size >= threshold) {
			expungeStaleEntries();
			if (size >= threshold)
				resize();
		}
		return k;
	}

	/** 扩容并重哈希（达到 MAXIMUM_CAPACITY 后不再扩容，将阈值设为 Integer.MAX_VALUE）。 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	private void resize() {
		int n = table.length;
		if (n == MAXIMUM_CAPACITY) {
			threshold = Integer.MAX_VALUE;
			return;
		}
		int n2 = n + n;
		@Nullable Entry[] dest = new Entry[n2];
		for (Entry<K> entry : table) {
			Entry<K> e = entry;
			while (e != null) {
				Entry<K> next = e.next;
				K key = e.get();
				if (key == null) {
					e.next = null; // 协助 GC
					--size;
				} else {
					int i = indexFor(e.hash, n2);
					e.next = dest[i];
					dest[i] = e;
				}
				e = next;
			}
		}
		table = dest;
		threshold = (int)(n2 * loadFactor);
	}

	/** 以 WeakReference 主引用作为键的哈希链节点。 */
	private static final class Entry<K> extends WeakReference<K> {
		private final int hash;
		private @Nullable Entry<K> next;

		Entry(K key, int hash, @Nullable Entry<K> next) {
			super(key);
			this.hash = hash;
			this.next = next;
		}
	}
}
