package Zeze.Util;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.LongConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LongHashSet implements Cloneable {
	private int size;
	private long @NotNull [] keyTable;
	private final float loadFactor;
	private int threshold;
	private byte shift;
	private boolean hasZeroKey;

	public LongHashSet() {
		this(2, 0.8f);
	}

	public LongHashSet(int cap) {
		this(cap, 0.8f);
	}

	public LongHashSet(int cap, float loadFactor) {
		if (loadFactor <= 0 || loadFactor >= 1)
			throw new IllegalArgumentException("invalid loadFactor: " + loadFactor);
		this.loadFactor = loadFactor;
		int tableSize = tableSize(Math.max(cap, 0));
		threshold = (int)((float)tableSize * loadFactor);
		shift = (byte)Long.numberOfLeadingZeros(tableSize - 1);
		keyTable = new long[tableSize];
	}

	public LongHashSet(@NotNull LongHashSet set) {
		size = set.size;
		keyTable = set.keyTable.clone();
		loadFactor = set.loadFactor;
		threshold = set.threshold;
		shift = set.shift;
		hasZeroKey = set.hasZeroKey;
	}

	private int tableSize(int cap) {
		cap = Math.min(Math.max((int)Math.ceil((float)cap / loadFactor), 2), 0x40000000);
		return 1 << 32 - Integer.numberOfLeadingZeros(cap - 1);
	}

	private int hash(long key) {
		return (int)(key * 0x9E3779B97F4A7C15L >>> shift);
	}

	public int size() {
		return size;
	}

	public long @NotNull [] getKeyTable() {
		return keyTable;
	}

	public float getLoadFactor() {
		return loadFactor;
	}

	public boolean hasZeroKey() {
		return hasZeroKey;
	}

	public int capacity() {
		return keyTable.length;
	}

	public boolean isEmpty() {
		return size == 0;
	}

	public boolean contains(long key) {
		if (key == 0)
			return hasZeroKey;
		long[] kt = keyTable;
		int m = kt.length - 1;
		int i = hash(key);
		for (long k; (k = kt[i]) != key; i = (i + 1) & m)
			if (k == 0)
				return false;
		return true;
	}

	public boolean add(long key) {
		if (key == 0) {
			if (hasZeroKey)
				return false;
			hasZeroKey = true;
			size++;
			return true;
		}
		long[] kt = keyTable;
		for (int i = hash(key), m = kt.length - 1; ; i = (i + 1) & m) {
			long k = kt[i];
			if (k == 0) {
				kt[i] = key;
				if (++size >= threshold)
					resize(kt.length << 1);
				return true;
			}
			if (k == key)
				return false;
		}
	}

	public void addAll(@NotNull LongHashSet set) {
		if (set.hasZeroKey)
			hasZeroKey = true;
		for (long k : set.keyTable)
			if (k != 0)
				add(k);
	}

	public void addAll(@NotNull Collection<Long> set) {
		for (long v : set)
			add(v);
	}

	public void addAllTo(@NotNull Collection<Long> set) {
		foreach(set::add);
	}

	public boolean remove(long key) {
		if (key == 0) {
			if (!hasZeroKey)
				return false;
			hasZeroKey = false;
			size--;
			return true;
		}
		long[] kt = keyTable;
		int m = kt.length - 1;
		int i = hash(key);
		for (long k; (k = kt[i]) != key; i = (i + 1) & m)
			if (k == 0)
				return false;
		for (int j = (i + 1) & m; (key = kt[j]) != 0; j = (j + 1) & m) {
			int h = hash(key);
			if (((j - h) & m) > ((i - h) & m)) {
				kt[i] = key;
				i = j;
			}
		}
		kt[i] = 0;
		size--;
		return true;
	}

	public void clear() {
		if (size == 0)
			return;
		size = 0;
		hasZeroKey = false;
		Arrays.fill(keyTable, 0);
	}

	public void clear(int maxCap) {
		int tableSize = tableSize(Math.max(maxCap, 0));
		if (tableSize >= keyTable.length) {
			clear();
			return;
		}
		size = 0;
		hasZeroKey = false;
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
		threshold = (int)(newSize * loadFactor);
		int m = newSize - 1;
		shift = (byte)Long.numberOfLeadingZeros(m);
		long[] kt = new long[newSize];
		if (size != 0) {
			for (long k : keyTable) {
				if (k != 0) {
					for (int i = hash(k); ; i = (i + 1) & m) {
						if (kt[i] == 0) {
							kt[i] = k;
							break;
						}
					}
				}
			}
		}
		keyTable = kt;
	}

	public void foreach(@NotNull LongConsumer consumer) {
		if (hasZeroKey)
			consumer.accept(0);
		for (long k : keyTable)
			if (k != 0)
				consumer.accept(k);
	}

	public interface LongSetPredicate {
		boolean test(@NotNull LongHashSet set, long key);
	}

	public boolean foreachTest(@NotNull LongSetPredicate tester) {
		if (hasZeroKey && !tester.test(this, 0))
			return false;
		for (long k : keyTable)
			if (k != 0 && !tester.test(this, k))
				return false;
		return true;
	}

	public final class Iterator {
		private int idx = -2;

		public boolean moveToNext() {
			if (idx == -2) {
				idx = -1;
				if (hasZeroKey)
					return true;
			}
			long[] kt = keyTable;
			for (int lastIdx = kt.length - 1; idx < lastIdx; )
				if (kt[++idx] != 0)
					return true;
			return false;
		}

		public long value() {
			return idx >= 0 ? keyTable[idx] : 0;
		}
	}

	public @NotNull Iterator iterator() {
		return new Iterator();
	}

	@Override
	public @NotNull LongHashSet clone() throws CloneNotSupportedException {
		LongHashSet set = (LongHashSet)super.clone();
		set.keyTable = keyTable.clone();
		return set;
	}

	@Override
	public int hashCode() {
		int h = 0;
		for (long k : keyTable)
			if (k != 0)
				h += Long.hashCode(k);
		return hasZeroKey ? ~h : h;
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (o == this)
			return true;
		if (!(o instanceof LongHashSet))
			return false;
		LongHashSet ls = (LongHashSet)o;
		if (size != ls.size || hasZeroKey != ls.hasZeroKey)
			return false;
		for (long k : keyTable)
			if (k != 0 && !ls.contains(k))
				return false;
		return true;
	}

	@Override
	public @NotNull String toString() {
		if (size == 0)
			return "{}";
		StringBuilder sb = new StringBuilder(32).append('{');
		long[] kt = keyTable;
		int i = 0, n = Math.min(kt.length, 20);
		long k;
		if (hasZeroKey)
			sb.append('0');
		else {
			while (i < n) {
				if ((k = kt[i++]) != 0) {
					sb.append(k);
					break;
				}
			}
		}
		for (; i < n; i++)
			if ((k = kt[i]) != 0)
				sb.append(',').append(k);
		if (n != kt.length)
			sb.append(",...");
		return sb.append('}').toString();
	}
}
