package Zeze.Util;

import java.lang.ref.WeakReference;

public final class WeakHashSet<K> {

	/**
	 * The default initial capacity -- MUST be a power of two.
	 */
	private static final int DEFAULT_INITIAL_CAPACITY = 1 << 8;

	/**
	 * The maximum capacity, used if a higher value is implicitly specified by
	 * either of the constructors with arguments. MUST be a power of two <= 1<<30.
	 */
	private static final int MAXIMUM_CAPACITY = 1 << 30;

	/**
	 * The load fast used when none specified in constructor.
	 */
	private static final float DEFAULT_LOAD_FACTOR = 0.75f;

	/**
	 * The table, resized as necessary. Length MUST Always be a power of two.
	 */
	@SuppressWarnings("rawtypes")
	private Entry[] table;

	/**
	 * The number of key-value mappings contained in this weak hash map.
	 */
	private int size;

	/**
	 * The next size value at which to resize (capacity * load factor).
	 */
	private int threshold;

	/**
	 * The load factor for the hash table.
	 */
	private final float loadFactor;

	/**
	 * Constructs a new, empty <tt>WeakHashSet</tt> with the given initial capacity
	 * and the given load factor.
	 *
	 * @param initialCapacity The initial capacity of the <tt>WeakHashSet</tt>
	 * @param loadFactor      The load factor of the <tt>WeakHashSet</tt>
	 * @throws IllegalArgumentException if the initial capacity is negative, or if
	 *                                  the load factor is nonpositive.
	 */
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
		threshold = (int) (capacity * loadFactor);
	}

	/**
	 * Constructs a new, empty <tt>WeakHashSet</tt> with the given initial capacity
	 * and the default load factor (0.75).
	 *
	 * @param initialCapacity The initial capacity of the <tt>WeakHashSet</tt>
	 * @throws IllegalArgumentException if the initial capacity is negative
	 */
	public WeakHashSet(int initialCapacity) {
		this(initialCapacity, DEFAULT_LOAD_FACTOR);
	}

	/**
	 * Constructs a new, empty <tt>WeakHashSet</tt> with the default initial
	 * capacity (16) and load factor (0.75).
	 */
	public WeakHashSet() {
		this.loadFactor = DEFAULT_LOAD_FACTOR;
		threshold = DEFAULT_INITIAL_CAPACITY;
		table = new Entry[DEFAULT_INITIAL_CAPACITY];
	}

	/**
	 * Applies a supplemental hash function to a given hashCode, which defends
	 * against poor quality hash functions. This is critical because HashMap uses
	 * power-of-two length hash tables, that otherwise encounter collisions for
	 * hashCodes that do not differ in lower bits. Note: Null keys always map to
	 * hash 0, thus index 0.
	 */
	private static int hash(int h) {
		// This function ensures that hashCodes that differ only by
		// constant multiples at each bit position have a bounded
		// number of collisions (approximately 8 at default load factor).
		h ^= (h >>> 20) ^ (h >>> 12);
		return h ^ (h >>> 7) ^ (h >>> 4);
	}

	/**
	 * Returns index for hash code h.
	 */
	private static int indexFor(int h, int length) {
		return h & (length - 1);
	}

	/**
	 * Expunges stale entries from the table.
	 */
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
					e.next = null; // Help GC
					--size;
				} else {
					prev = e;
				}
				e = next;
			}
		}
	}

	/**
	 * Returns the number of key-value mappings in this map. This result is a
	 * snapshot, and may not reflect unprocessed entries that will be removed before
	 * next attempted access because they are no longer referenced.
	 */
	public int size() {
		return size;
	}

	/**
	 * Returns the key stored in the set NOT THREAD SAFE
	 *
	 * @see #add(Object)
	 */
	@SuppressWarnings("unchecked")
	public K get(final K k) {
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
				e.next = null; // Help GC
				// remove e
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
	 * Adds the specified element to this set if it is not already present. More
	 * formally, adds the specified element <tt>e</tt> to this set if this set
	 * contains no element <tt>e2</tt> such that
	 * <tt>(e==null&nbsp;?&nbsp;e2==null&nbsp;:&nbsp;e.equals(e2))</tt>. If this set
	 * already contains the element, the call leaves the set unchanged and returns
	 * the old value.
	 *
	 * @return the added element or the old element if an equal one exists
	 */
	@SuppressWarnings("unchecked")
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
				e.next = null; // Help GC
				// remove e
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

	/**
	 * Rehashes the contents of this set into a new array with a larger capacity.
	 * This method is called automatically when the number of keys in this set
	 * reaches its threshold.
	 *
	 * If current capacity is MAXIMUM_CAPACITY, this method does not resize the set,
	 * but sets threshold to Integer.MAX_VALUE. This has the effect of preventing
	 * future calls.
	 *
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void resize() {
		int n = table.length;
		if (n == MAXIMUM_CAPACITY) {
			threshold = Integer.MAX_VALUE;
			return;
		}
		int n2 = n + n;
		Entry[] dest = new Entry[n2];
		/** Transfers all entries from table to dest tables */
		for (Entry<K> entry : table) {
			Entry<K> e = entry;
			while (e != null) {
				Entry<K> next = e.next;
				K key = e.get();
				if (key == null) {
					e.next = null; // Help GC
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
		threshold = (int) (n2 * loadFactor);
	}

	/**
	 * The entries in this hash set extend WeakReference using its main ref field as
	 * the key
	 */
	private final static class Entry<K> extends WeakReference<K> {
		private final int hash;
		private Entry<K> next;

		/**
		 * Creates new entry.
		 */
		Entry(K key, int hash, Entry<K> next) {
			super(key);
			this.hash = hash;
			this.next = next;
		}

	}

}
