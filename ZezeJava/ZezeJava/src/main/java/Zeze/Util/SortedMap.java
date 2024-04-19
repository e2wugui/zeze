package Zeze.Util;

import java.util.ArrayList;
import java.util.Arrays;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 以有序Entry(K+V)数组为存储结构的Map
 * 主要用于读多写少的场合, 除只读方法外不支持并发
 *
 * @param <K> 要求实现equals和compareTo
 * @param <V> 要求实现equals和compareTo
 */
public class SortedMap<K extends Comparable<K>, V extends Comparable<V>> {
	public static class Entry<K extends Comparable<K>, V extends Comparable<V>> implements Comparable<Entry<K, V>> {
		private final @NotNull K key;
		private final @NotNull V value;
		private final long hash; // 主要用于公平地比较
		private @Nullable Entry<K, V> next; // 相同key的下个节点

		private Entry(@NotNull K key, @NotNull V value, long hash) {
			this.key = key;
			this.value = value;
			this.hash = hash;
		}

		public @NotNull K getKey() {
			return key;
		}

		public @NotNull V getValue() {
			return value;
		}

		/**
		 * @return 0:e加入到非头节点; 1:e加入到头节点; -1:已加过过相同节点
		 */
		private int addEntry(@NotNull Entry<K, V> e) {
			assert key.equals(e.key);
			int c = compareTo(e);
			if (c > 0) {
				e.next = this;
				return 1;
			}
			if (c < 0) {
				for (var s = this; ; ) {
					var n = s.next;
					if (n == null) {
						s.next = e;
						break;
					}
					c = n.compareTo(e);
					if (c > 0) {
						e.next = n;
						s.next = e;
						break;
					}
					if (c == 0)
						return -1;
					s = n;
				}
			}
			return 0;
		}

		/**
		 * @return 被移除的节点. null表示没找到指定节点
		 */
		private @Nullable Entry<K, V> removeEntry(@NotNull V value, long hash) {
			if (this.hash == hash && this.value.equals(value))
				return this;
			for (var s = this; ; ) {
				var n = s.next;
				if (n == null)
					return null;
				if (n.hash == hash && n.value.equals(value)) {
					s.next = n.next;
					return n;
				}
				s = n;
			}
		}

		@Override
		public int compareTo(@NotNull Entry<K, V> o) {
			int c = Long.compare(hash, o.hash);
			return c != 0 ? c : value.compareTo(o.value);
		}

		@Override
		public @NotNull String toString() {
			var sb = new StringBuilder(32);
			sb.append('(').append(key).append(':').append(value).append(';').append(hash);
			for (var s = next; s != null; s = s.next)
				sb.append(")-(").append(s.key).append(':').append(s.value).append(';').append(s.hash);
			return sb.append(')').toString();
		}
	}

	/**
	 * 对一个节点做hash,用于加入时出现key冲突的排序依据(越小越优先,相等时再比较value)
	 * 如果不用hash,只比较kv本身会导致结果有强烈的偏向性,所以引入索引参与hash计算可以基本保证公平性
	 */
	public interface HashFunc<K extends Comparable<K>, V extends Comparable<V>> {
		/**
		 * 相同(equals)的value和index必须得到确定的hash值,尽量能在64位全范围分散开
		 */
		long hash(@NotNull K key, @NotNull V value, int index);
	}

	private @NotNull ArrayList<Entry<K, V>> elements = new ArrayList<>();
	private final @NotNull HashFunc<K, V> hashFunc;
	private int size;

	public SortedMap() {
		this(null);
	}

	public SortedMap(@Nullable HashFunc<K, V> hashFunc) {
		this.hashFunc = hashFunc != null ? hashFunc : (k, v, i) -> ((long)v.hashCode() << 32) + i;
	}

	public boolean isEmpty() {
		return size == 0;
	}

	public int size() {
		return size;
	}

	public int keySize() {
		return elements.size();
	}

	public @NotNull Entry<K, V> getAt(int index) {
		return elements.get(index);
	}

	public @Nullable Entry<K, V> first() {
		var es = elements;
		return es.isEmpty() ? null : es.get(0);
	}

	public @Nullable Entry<K, V> lowerBound(@NotNull K key) {
		var es = elements;
		int i = lowerBoundIndex(key);
		return i < es.size() ? es.get(i) : null;
	}

	public @Nullable Entry<K, V> upperBound(@NotNull K key) {
		var es = elements;
		int i = upperBoundIndex(key);
		return i < es.size() ? es.get(i) : null;
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
		int i = findIndex(key);
		return i >= 0 ? elements.get(i) : null;
	}

	@Override
	public @NotNull String toString() {
		return elements.toString();
	}

	// 以上是只读方法; 以下是修改方法

	public void clear() {
		elements = new ArrayList<>();
		size = 0;
	}

	public void add(@NotNull K key, @NotNull V value, int index) {
		var es = elements;
		int i = lowerBoundIndex(key);
		Entry<K, V> oldE, newE = new Entry<>(key, value, hashFunc.hash(key, value, index));
		if (i == es.size())
			es.add(newE);
		else if (!(oldE = es.get(i)).key.equals(key))
			es.add(i, newE);
		else {
			int r = oldE.addEntry(newE);
			if (r < 0)
				return;
			if (r > 0)
				es.set(i, newE);
		}
		size++;
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
				K lastK = null;
				Entry<K, V> lastE = null;
				var ie = es.get(0);
				for (K ik = ie.key, jk = keys[0]; ; ) {
					int c = ik.compareTo(jk);
					if (c < 0) {
						newElements.add(ie);
						if (++i >= in)
							break;
						ie = es.get(i);
						ik = ie.key;
					} else {
						var e = new Entry<>(jk, value, hashFunc.hash(jk, value, j));
						if (c > 0) {
							if (jk.equals(lastK)) {
								int r = lastE.addEntry(e);
								if (r > 0)
									newElements.set(newElements.size() - 1, e);
								else if (r < 0)
									size--;
							} else {
								lastK = jk;
								newElements.add(lastE = e);
							}
						} else {
							int r = ie.addEntry(e);
							if (r > 0)
								es.set(i, ie = e);
							else if (r < 0)
								size--;
						}
						if (++j >= jn)
							break;
						jk = keys[j];
					}
				}
			}
			if (i < in)
				newElements.addAll(es.subList(i, in));
			else {
				K lastK = null;
				Entry<K, V> lastE = null;
				for (; j < jn; j++) {
					K jk = keys[j];
					var e = new Entry<>(jk, value, hashFunc.hash(jk, value, j));
					if (jk.equals(lastK)) {
						int r = lastE.addEntry(e);
						if (r > 0)
							newElements.set(newElements.size() - 1, e);
						else if (r < 0)
							size--;
					} else {
						lastK = jk;
						newElements.add(lastE = e);
					}
				}
			}
			elements = newElements;
			size += jn;
		}
	}

	public @Nullable Entry<K, V> remove(@NotNull K key, @NotNull V value, int index) {
		var es = elements;
		int i = findIndex(key);
		if (i < 0)
			return null;
		var e = es.get(i);
		var oldE = e.removeEntry(value, hashFunc.hash(key, value, index));
		if (oldE != null) {
			if (oldE == e) {
				var n = e.next;
				if (n != null)
					es.set(i, n);
				else
					es.remove(i);
			}
			oldE.next = null;
			size--;
		}
		return oldE;
	}

	public void removeAll(@NotNull K @NotNull [] keys, @NotNull V value) {
		Arrays.sort(keys);
		removeSortedAll(keys, value);
	}

	public void removeSortedAll(@NotNull K @NotNull [] keys, @NotNull V value) {
		int jn = keys.length;
		if (jn > 0) {
			var es = elements;
			int in = es.size();
			if (in > 0) {
				int i = 0, j = 0;
				var newElements = new ArrayList<Entry<K, V>>(in);
				var ie = es.get(0);
				for (K ik = ie.key, jk = keys[0]; ; ) {
					int c = ik.compareTo(jk);
					if (c < 0) {
						newElements.add(ie);
						if (++i >= in)
							break;
						ie = es.get(i);
						ik = ie.key;
					} else {
						if (c == 0) {
							var oldE = ie.removeEntry(value, hashFunc.hash(jk, value, j));
							if (oldE != null) {
								size--;
								if (oldE == ie) {
									var n = ie.next;
									if (n != null)
										es.set(i, ie = n);
									else {
										if (++i >= in)
											break;
										ie = es.get(i);
										ik = ie.key;
									}
								}
							}
						}
						if (++j >= jn)
							break;
						jk = keys[j];
					}
				}
				if (i < in)
					newElements.addAll(es.subList(i, in));
				elements = newElements;
			}
		}
	}
}
