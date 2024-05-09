package Zeze.Util;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;

public class IdentityHashSet<T> implements Cloneable {
	private int size;
	private T[] keyTable;
	private final float loadFactor;
	private int threshold;
	private int shift;

	public IdentityHashSet() {
		this(2, 0.8f);
	}

	public IdentityHashSet(int cap) {
		this(cap, 0.8f);
	}

	@SuppressWarnings("unchecked")
	public IdentityHashSet(int cap, float loadFactor) {
		if (loadFactor <= 0 || loadFactor >= 1)
			throw new IllegalArgumentException("invalid loadFactor: " + loadFactor);
		this.loadFactor = loadFactor;
		int tableSize = tableSize(Math.max(cap, 0));
		threshold = (int)((float)tableSize * loadFactor);
		shift = Long.numberOfLeadingZeros(tableSize - 1);
		keyTable = (T[])new Object[tableSize];
	}

	public IdentityHashSet(Collection<? extends T> c) {
		this(c.size());
		addAll(c);
	}

	public IdentityHashSet(IdentityHashSet<? extends T> set) {
		size = set.size;
		keyTable = set.keyTable.clone();
		loadFactor = set.loadFactor;
		threshold = set.threshold;
		shift = set.shift;
	}

	private int tableSize(int cap) {
		cap = Math.min(Math.max((int)Math.ceil((float)cap / loadFactor), 2), 0x40000000);
		return 1 << 32 - Integer.numberOfLeadingZeros(cap - 1);
	}

	private int hash(Object key) {
		return (int)(System.identityHashCode(key) * 0x9E3779B97F4A7C15L >>> shift);
	}

	public T[] getKeyTable() {
		return keyTable;
	}

	public float getLoadFactor() {
		return loadFactor;
	}

	public int capacity() {
		return keyTable.length;
	}

	public int size() {
		return size;
	}

	public boolean isEmpty() {
		return size == 0;
	}

	public boolean contains(Object key) {
		T[] kt = keyTable;
		int m = kt.length - 1;
		int i = hash(key);
		for (T k; (k = kt[i]) != key; i = (i + 1) & m)
			if (k == null)
				return false;
		return true;
	}

	public boolean containsAll(Collection<?> c) {
		for (Object e : c)
			if (e != null && !contains(e))
				return false;
		return true;
	}

	public boolean containsAll(IdentityHashSet<?> set) {
		for (Object e : set.keyTable)
			if (e != null && !contains(e))
				return false;
		return true;
	}

	public boolean add(T key) {
		T[] kt = keyTable;
		for (int i = hash(key), m = kt.length - 1; ; i = (i + 1) & m) {
			T k = kt[i];
			if (k == null) {
				kt[i] = key;
				if (++size >= threshold)
					resize(kt.length << 1);
				return true;
			}
			if (k == key)
				return false;
		}
	}

	public void addAll(Collection<? extends T> c) {
		for (T k : c)
			if (k != null)
				add(k);
	}

	public void addAll(IdentityHashSet<? extends T> set) {
		for (T k : set.keyTable)
			if (k != null)
				add(k);
	}

	public boolean remove(Object key) {
		T[] kt = keyTable;
		int m = kt.length - 1;
		int i = hash(key);
		for (T k; (k = kt[i]) != key; i = (i + 1) & m)
			if (k == null)
				return false;
		T k;
		for (int j = (i + 1) & m; (k = kt[j]) != null; j = (j + 1) & m) {
			int h = hash(k);
			if (((j - h) & m) > ((i - h) & m)) {
				kt[i] = k;
				i = j;
			}
		}
		kt[i] = null;
		size--;
		return true;
	}

	public boolean removeAll(Collection<?> c) {
		boolean r = false;
		for (Object k : c)
			if (k != null)
				r |= remove(k);
		return r;
	}

	public boolean removeAll(IdentityHashSet<?> set) {
		boolean r = false;
		for (Object k : set.keyTable)
			if (k != null)
				r |= remove(k);
		return r;
	}

	public void clear() {
		if (size == 0)
			return;
		size = 0;
		Arrays.fill(keyTable, null);
	}

	public void clear(int maxCap) {
		int tableSize = tableSize(Math.max(maxCap, 0));
		if (tableSize >= keyTable.length) {
			clear();
			return;
		}
		size = 0;
		resize(tableSize);
	}

	public void shrink(int maxCap) {
		int tableSize = tableSize(Math.max(maxCap, size));
		if (tableSize < keyTable.length)
			resize(tableSize);
	}

	public void ensureCapacity(int cap) {
		int tableSize = tableSize(Math.max(cap, 0));
		if (tableSize > keyTable.length)
			resize(tableSize);
	}

	private void resize(int newSize) {
		int m = newSize - 1;
		threshold = (int)(newSize * loadFactor);
		shift = Long.numberOfLeadingZeros(m);
		@SuppressWarnings("unchecked")
		T[] kt = (T[])new Object[newSize];
		if (size != 0) {
			for (T k : keyTable) {
				if (k != null) {
					for (int i = hash(k); ; i = (i + 1) & m) {
						if (kt[i] == null) {
							kt[i] = k;
							break;
						}
					}
				}
			}
		}
		keyTable = kt;
	}

	public Object[] toArray() {
		Object[] a = new Object[size];
		int i = 0;
		for (T k : keyTable)
			if (k != null)
				a[i++] = k;
		return a;
	}

	@SuppressWarnings("unchecked")
	public <A> A[] toArray(A[] a) {
		int size = this.size;
		if (a.length < size)
			a = (A[])Array.newInstance(a.getClass().getComponentType(), size);
		int i = 0;
		for (T k : keyTable)
			if (k != null)
				a[i++] = (A)k;
		return a;
	}

	public void foreach(Consumer<T> consumer) {
		for (T k : keyTable)
			if (k != null)
				consumer.accept(k);
	}

	public interface SetPredicate<T> {
		boolean test(IdentityHashSet<T> set, T key);
	}

	public boolean foreachTest(SetPredicate<T> tester) {
		for (T k : keyTable)
			if (k != null && !tester.test(this, k))
				return false;
		return true;
	}

	public final class Iterator {
		private int idx = -1;

		public boolean moveToNext() {
			T[] kt = keyTable;
			for (int lastIdx = kt.length - 1; idx < lastIdx; )
				if (kt[++idx] != null)
					return true;
			return false;
		}

		public T value() {
			return keyTable[idx];
		}
	}

	public Iterator iterator() {
		return new Iterator();
	}

	@Override
	public IdentityHashSet<T> clone() throws CloneNotSupportedException {
		@SuppressWarnings("unchecked")
		IdentityHashSet<T> set = (IdentityHashSet<T>)super.clone();
		set.keyTable = keyTable.clone();
		return set;
	}

	@Override
	public String toString() {
		if (size == 0)
			return "{}";
		StringBuilder sb = new StringBuilder(32).append('{');
		T[] kt = keyTable;
		T k;
		int i = 0, n = Math.min(kt.length, 20);
		while (i < n) {
			if ((k = kt[i++]) != null) {
				sb.append(k);
				break;
			}
		}
		for (; i < n; i++)
			if ((k = kt[i]) != null)
				sb.append(',').append(k);
		if (n != kt.length)
			sb.append(",...");
		return sb.append('}').toString();
	}
}
