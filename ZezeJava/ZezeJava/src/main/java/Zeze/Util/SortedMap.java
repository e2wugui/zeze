package Zeze.Util;

import java.util.ArrayList;

/**
 * 这根本不是Map。
 * @param <K>
 * @param <V>
 */
public class SortedMap<K extends Comparable<K>, V> {
	public static class Entry<K extends Comparable<K>, V> implements Comparable<Entry<K, V>> {
		K key;
		V value;

		public Entry(K key, V value) {
			this.key = key;
			this.value = value;
		}

		public K getKey() {
			return key;
		}

		public V getValue() {
			return value;
		}

		public static <K extends Comparable<K>, V> Entry<K, V> create(K key, V value) {
			return new Entry<>(key, value);
		}

		@Override
		public int compareTo(SortedMap.Entry<K, V> o) {
			return key.compareTo(o.key);
		}

		@Override
		public String toString() {
			return "(" + key + "," + value + ")";
		}
	}

	private final ArrayList<Entry<K, V>> elements = new ArrayList<>();

	public Entry<K, V> first() {
		return elements.isEmpty() ? null : elements.get(0);
	}

	public Entry<K, V> lowerBound(K key) {
		var index = lowerBoundIndex(key);
		if (index >= 0 && index < elements.size())
			return getAt(index);
		return null;
	}

	public Entry<K, V> upperBound(K key) {
		var index = upperBoundIndex(key);
		if (index >= 0 && index < elements.size())
			return getAt(index);
		return null;
	}

	/**
	 * std::lower_bound 定义。返回指定key为上限的索引。
	 * @param key key
	 * @return index locate，不存在时返回lastIndex+1。
	 */
	public int lowerBoundIndex(K key) {
		var first = 0;
		var count = elements.size();
		while (count > 0) {
			var it = first;
			var step = count / 2;
			it += step;
			if (getAt(it).key.compareTo(key) < 0) {
				first = it + 1;
				count -= step + 1;
			} else
				count = step;
		}
		return first;
	}

	/**
	 * std::upper_bound 定义。返回指定key为下限的索引。
	 * @param key key
	 * @return index locate，不存在时返回lastIndex+1
	 */
	public int upperBoundIndex(K key) {
		var first = 0;
		var count = elements.size();
		while (count > 0) {
			var it = first;
			var step = count / 2;
			it += step;
			if (getAt(it).key.compareTo(key) <= 0) {
				first = it + 1;
				count -= step + 1;
			} else
				count = step;
		}
		return first;
	}

	/**
	 * 二分法查找key
	 * @param key key
	 * @return index found. -1 means not found.
	 */
	public int findIndex(K key) {
		int low = 0;
		int high = elements.size() - 1;
		while (low <= high) {
			int middle = (low + high) / 2;
			int c = key.compareTo(elements.get(middle).key);
			if (c == 0)
				return middle;
			if(c > 0)
				low = middle + 1;
			else
				high = middle - 1;
		}
		return -1;
	}

	public Entry<K, V> get(K key) {
		var index = findIndex(key);
		if (index >= 0)
			return getAt(index);
		return null;
	}

	public int add(K key, V value) {
		// todo 随便写写先
		var index = lowerBoundIndex(key);
		if (index >= 0) {
			if (index < elements.size() && getAt(index).key.compareTo(key) == 0)
				return -1; // duplicate.
			elements.add(index, Entry.create(key, value));
			return index;
		}
		throw new RuntimeException("internal error");
	}

	public Entry<K, V> remove(K key) {
		var index = findIndex(key);
		if (index >= 0)
			return removeAt(index);
		return null;
	}

	public Entry<K, V> getAt(int index) {
		return elements.get(index);
	}

	public Entry<K, V> removeAt(int index) {
		return elements.remove(index);
	}

	@Override
	public String toString() {
		return elements.toString();
	}
}
