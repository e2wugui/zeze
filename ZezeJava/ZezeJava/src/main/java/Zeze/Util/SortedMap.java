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

	private final ArrayList<Entry<K, V>> elements;

	public SortedMap(int initCapacity) {
		elements = new ArrayList<>(initCapacity);
	}

	public Entry<K, V> lowerBound(K key) {
		var index = lowerBoundIndex(key);
		if (index >= 0)
			return get(index);
		return null;
	}

	public Entry<K, V> upperBound(K key) {
		var index = upperBoundIndex(key);
		if (index >= 0)
			return get(index);
		return null;
	}

	public int lowerBoundIndex(K key) {
		/*
template<class ForwardIt, class T>
ForwardIt lower_bound(ForwardIt first, ForwardIt last, const T& value)
{
    ForwardIt it;
    typename std::iterator_traits<ForwardIt>::difference_type count, step;
    count = std::distance(first, last);

    while (count > 0)
    {
        it = first;
        step = count / 2;
        std::advance(it, step);

        if (*it < value)
        {
            first = ++it;
            count -= step + 1;
        }
        else
            count = step;
    }

    return first;
}
		*/
		return 0;
	}

	public int upperBoundIndex(K key) {
		/*
template<class ForwardIt, class T>
ForwardIt upper_bound(ForwardIt first, ForwardIt last, const T& value)
{
    ForwardIt it;
    typename std::iterator_traits<ForwardIt>::difference_type count, step;
    count = std::distance(first, last);

    while (count > 0)
    {
        it = first;
        step = count / 2;
        std::advance(it, step);

        if (!(value < *it))
        {
            first = ++it;
            count -= step + 1;
        }
        else
            count = step;
    }

    return first;
}
		*/
		return 0;
	}

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
			return get(index);
		return null;
	}

	public int put(K key, V value) {
		// todo 随便写写先
		var index = lowerBoundIndex(key);
		if (index >= 0) {
			elements.add(index, Entry.create(key, value));
			return index;
		}
		elements.add(Entry.create(key, value));
		return elements.size() - 1;
	}

	public Entry<K, V> remove(K key) {
		var index = findIndex(key);
		if (index >= 0)
			return remove(index);
		return null;
	}

	public Entry<K, V> get(int index) {
		return elements.get(index);
	}

	public Entry<K, V> remove(int index) {
		return elements.remove(index);
	}
}
