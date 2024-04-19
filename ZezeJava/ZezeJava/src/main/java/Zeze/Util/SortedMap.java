package Zeze.Util;

import java.util.ArrayList;
import java.util.Arrays;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 以有序Entry(K+V)数组为存储结构的Map
 * 除只读方法外不支持并发
 */
public class SortedMap<K extends Comparable<K>, V> {
	public static class Entry<K, V> {
		private final @NotNull K key;
		private final @NotNull V value;
		private final int index; // 批量加入时的数组索引,参与解决冲突

		private Entry(@NotNull K key, @NotNull V value, int index) {
			this.key = key;
			this.value = value;
			this.index = index;
		}

		public @NotNull K getKey() {
			return key;
		}

		public @NotNull V getValue() {
			return value;
		}

		@Override
		public @NotNull String toString() {
			return "(" + key + ',' + value + ',' + index + ')';
		}
	}

	/**
	 * 处理加入时出现key冲突的选择方法
	 */
	public interface Selector<K extends Comparable<K>, V> {
		/**
		 * 方法实现时要考虑几个因素:
		 * <li>传递性: 通常判定是否要替换都要做成对old和new的某种计算结果的数值做大小比较,大小天然满足传递性
		 * <li>公平性: 只比较kv本身会导致结果有强烈的偏向性,所以引入索引参与hash计算可以基本保证公平性
		 * <li>如果新旧K-V-index三者完全一致,那么是否替换都不应该对map有任何影响
		 * <li>如果完全不关心稳定性和公平性,可以只返回false或true
		 *
		 * @param oldK     旧(已存在的)key
		 * @param oldV     旧(已存在的)value
		 * @param oldIndex 旧(已存在的)索引. 来自批量加入时的数组索引
		 * @param newK     新(尝试加入的)key
		 * @param newV     新(尝试加入的)value
		 * @param newIndex 新(尝试加入的)索引. 来自批量加入时的数组索引
		 * @return 是否用新的替换旧的
		 */
		boolean select(@NotNull K oldK, @NotNull V oldV, int oldIndex, @NotNull K newK, @NotNull V newV, int newIndex);
	}

	private @NotNull ArrayList<Entry<K, V>> elements = new ArrayList<>();
	private final @Nullable Selector<K, V> selector;

	public SortedMap() {
		this(null);
	}

	public SortedMap(@Nullable Selector<K, V> selector) {
		this.selector = selector;
	}

	public int size() {
		return elements.size();
	}

	public @Nullable Entry<K, V> first() {
		var es = elements;
		return es.isEmpty() ? null : es.get(0);
	}

	public @Nullable Entry<K, V> lowerBound(@NotNull K key) {
		var es = elements;
		var index = lowerBoundIndex(key);
		return index < es.size() ? es.get(index) : null;
	}

	public @Nullable Entry<K, V> upperBound(@NotNull K key) {
		var es = elements;
		var index = upperBoundIndex(key);
		return index < es.size() ? es.get(index) : null;
	}

	/**
	 * std::lower_bound 定义。返回指定key为上限的索引。即在>=key范围内找最小的key的索引
	 *
	 * @return index. 不存在时返回size
	 */
	public int lowerBoundIndex(@NotNull K key) {
		var es = elements;
		int b = 0, e = es.size();
		while (b < e) {
			int m = (b + e) >> 1;
			int c = es.get(m).key.compareTo(key);
			if (c < 0)
				b = m + 1;
			else if (c > 0)
				e = m;
			else
				return m;
		}
		return b;
	}

	/**
	 * std::upper_bound 定义。返回指定key为下限的索引。即在>key范围内找最小的key的索引
	 *
	 * @return index. 不存在时返回size
	 */
	public int upperBoundIndex(@NotNull K key) {
		var es = elements;
		int b = 0, e = es.size();
		while (b < e) {
			int m = (b + e) >> 1;
			int c = es.get(m).key.compareTo(key);
			if (c < 0)
				b = m + 1;
			else if (c > 0)
				e = m;
			else
				return m + 1;
		}
		return b;
	}

	/**
	 * 二分法查找key
	 *
	 * @return index. 不存在时返回-1
	 */
	public int findIndex(@NotNull K key) {
		var es = elements;
		int b = 0, e = es.size();
		while (b < e) {
			int m = (b + e) >> 1;
			int c = es.get(m).key.compareTo(key);
			if (c < 0)
				b = m + 1;
			else if (c > 0)
				e = m;
			if (c == 0)
				return m;
		}
		return -1;
	}

	public @Nullable Entry<K, V> get(@NotNull K key) {
		var index = findIndex(key);
		return index >= 0 ? elements.get(index) : null;
	}

	public int add(@NotNull K key, @NotNull V value) {
		var es = elements;
		var index = lowerBoundIndex(key);
		if (index < es.size() && es.get(index).key.compareTo(key) == 0)
			return -1; // duplicate
		es.add(index, new Entry<>(key, value, 0));
		return index;
	}

	public void addAll(@NotNull K @NotNull [] keys, @NotNull V value) {
		Arrays.sort(keys);
		addSortedAll(keys, value);
	}

	public void addSortedAll(@NotNull K @NotNull [] keys, @NotNull V value) {
		int jn = keys.length;
		if (jn > 0) {
			var es = elements;
			int in = es.size();
			var newElements = new ArrayList<Entry<K, V>>(in + jn);
			int i = 0, j = 0;
			if (in > 0) {
				var ie = es.get(0);
				for (K ik = ie.key, jk = keys[0]; ; ) {
					int c = ik.compareTo(jk);
					if (c < 0) {
						newElements.add(ie);
						if (++i >= in)
							break;
						ie = es.get(i);
						ik = ie.key;
					} else if (c > 0) {
						newElements.add(new Entry<>(jk, value, j));
						if (++j >= jn)
							break;
						jk = keys[j];
					} else {
						if (selector != null && selector.select(ik, ie.value, ie.index, jk, value, j))
							newElements.add(new Entry<>(jk, value, j));
						else
							newElements.add(ie);
						++j;
						if (++i >= in || j >= jn)
							break;
						ie = es.get(i);
						ik = ie.key;
						jk = keys[j];
					}
				}
			}
			if (i < in)
				newElements.addAll(es.subList(i, in));
			else {
				for (; j < jn; j++)
					newElements.add(new Entry<>(keys[j], value, j));
			}
			elements = newElements;
		}
	}

	public @Nullable Entry<K, V> remove(@NotNull K key) {
		var index = findIndex(key);
		return index >= 0 ? elements.remove(index) : null;
	}

	public @Nullable Entry<K, V> remove(@NotNull K key, @NotNull V value) {
		var es = elements;
		var index = findIndex(key);
		return index >= 0 && es.get(index).value.equals(value) ? es.remove(index) : null;
	}

	public @NotNull Entry<K, V> getAt(int index) {
		return elements.get(index);
	}

	public @NotNull Entry<K, V> removeAt(int index) {
		return elements.remove(index);
	}

	@Override
	public @NotNull String toString() {
		return elements.toString();
	}
}
