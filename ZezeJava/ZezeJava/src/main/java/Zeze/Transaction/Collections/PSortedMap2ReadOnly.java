package Zeze.Transaction.Collections;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import org.jspecify.annotations.Nullable;
import java.util.Map.Entry;

/** PSortedMap2 的只读视图：值以只读类型暴露，修改入口抛 UnsupportedOperationException。 */
public class PSortedMap2ReadOnly<K extends Comparable<K>, V extends Bean, VReadOnly>
		implements Iterable<Map.Entry<K, VReadOnly>> {
	private final PSortedMap2<K, V> map;

	public PSortedMap2ReadOnly(PSortedMap2<K, V> map) {
		this.map = map;
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	public int size() {
		return map.size();
	}

	@SuppressWarnings("unchecked")
	public @Nullable VReadOnly get(K key) {
		return (VReadOnly)map.get(key);
	}

	public boolean containsValue(VReadOnly v) {
		//noinspection SuspiciousMethodCalls
		return map.containsValue(v);
	}

	public boolean containsKey(K key) {
		return map.containsKey(key);
	}

	public void copyTo(Map.Entry<K, VReadOnly> [] array, int arrayIndex) {
		for (var e : entrySet())
			array[arrayIndex++] = e;
	}

	// ---- NavigableMap readonly view methods ----
	// - 返回单个 Entry 的方法：用 ReadOnlyEntry 包装，把 V 强转 VReadOnly 局限在 getValue() 里，
	//   并把 setValue() 堵掉，编译期就禁止破坏只读契约。
	// - 返回 NavigableMap/SortedMap 的方法（subMap/headMap/tailMap/descendingMap）：
	//   底层 pcollections 的子视图本身就是 unmodifiable 的，运行时任何 mutator 都会抛
	//   UnsupportedOperationException；但泛型是不变的，没法把 NavigableMap<K,V> 安全转成
	//   NavigableMap<K,VReadOnly>，只能借助 unchecked cast。安全性靠 pcollections 保证。
	// - 返回 K 或 NavigableSet<K> 的方法：直接透传，无类型问题。
	// - pollFirstEntry/pollLastEntry 是破坏性方法，不暴露。

	public @Nullable Entry<K, VReadOnly> lowerEntry(K key) {
		return wrap(map.lowerEntry(key));
	}

	public K lowerKey(K key) {
		return map.lowerKey(key);
	}

	public @Nullable Entry<K, VReadOnly> floorEntry(K key) {
		return wrap(map.floorEntry(key));
	}

	public K floorKey(K key) {
		return map.floorKey(key);
	}

	public @Nullable Entry<K, VReadOnly> ceilingEntry(K key) {
		return wrap(map.ceilingEntry(key));
	}

	public K ceilingKey(K key) {
		return map.ceilingKey(key);
	}

	public @Nullable Entry<K, VReadOnly> higherEntry(K key) {
		return wrap(map.higherEntry(key));
	}

	public K higherKey(K key) {
		return map.higherKey(key);
	}

	public @Nullable Entry<K, VReadOnly> firstEntry() {
		return wrap(map.firstEntry());
	}

	public @Nullable Entry<K, VReadOnly> lastEntry() {
		return wrap(map.lastEntry());
	}

	@SuppressWarnings("unchecked")
	public NavigableMap<K, VReadOnly> descendingMap() {
		return (NavigableMap<K, VReadOnly>)map.descendingMap();
	}

	public NavigableSet<K> navigableKeySet() {
		return map.navigableKeySet();
	}

	public NavigableSet<K> descendingKeySet() {
		return map.descendingKeySet();
	}

	@SuppressWarnings("unchecked")
	public NavigableMap<K, VReadOnly> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
		return (NavigableMap<K, VReadOnly>)map.subMap(fromKey, fromInclusive, toKey, toInclusive);
	}

	@SuppressWarnings("unchecked")
	public NavigableMap<K, VReadOnly> headMap(K toKey, boolean inclusive) {
		return (NavigableMap<K, VReadOnly>)map.headMap(toKey, inclusive);
	}

	@SuppressWarnings("unchecked")
	public NavigableMap<K, VReadOnly> tailMap(K fromKey, boolean inclusive) {
		return (NavigableMap<K, VReadOnly>)map.tailMap(fromKey, inclusive);
	}

	public @Nullable Comparator<? super K> comparator() {
		return map.comparator();
	}

	@SuppressWarnings("unchecked")
	public SortedMap<K, VReadOnly> subMap(K fromKey, K toKey) {
		return (SortedMap<K, VReadOnly>)map.subMap(fromKey, toKey);
	}

	@SuppressWarnings("unchecked")
	public SortedMap<K, VReadOnly> headMap(K toKey) {
		return (SortedMap<K, VReadOnly>)map.headMap(toKey);
	}

	@SuppressWarnings("unchecked")
	public SortedMap<K, VReadOnly> tailMap(K fromKey) {
		return (SortedMap<K, VReadOnly>)map.tailMap(fromKey);
	}

	public K firstKey() {
		return map.firstKey();
	}

	public K lastKey() {
		return map.lastKey();
	}

	public Set<K> keySet() {
		return new AbstractSet<>() {
			@Override
			public Iterator<K> iterator() {
				return new Iterator<>() {
					private final Iterator<Map.Entry<K, VReadOnly>> it = entrySet().iterator();

					@Override
					public boolean hasNext() {
						return it.hasNext();
					}

					@Override
					public K next() {
						return it.next().getKey();
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}

			@Override
			public int size() {
				return map.size();
			}
		};
	}

	public java.util.Collection<VReadOnly> values() {
		return new AbstractCollection<>() {
			@Override
			public Iterator<VReadOnly> iterator() {
				return new Iterator<>() {
					private final Iterator<Map.Entry<K, VReadOnly>> it = entrySet().iterator();

					@Override
					public boolean hasNext() {
						return it.hasNext();
					}

					@Override
					public VReadOnly next() {
						return it.next().getValue();
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}

			@Override
			public int size() {
				return map.size();
			}
		};
	}

	public Set<Map.Entry<K, VReadOnly>> entrySet() {
		return new AbstractSet<>() {
			@Override
			public Iterator<Map.Entry<K, VReadOnly>> iterator() {
				return new Iterator<>() {
					// PSortedMap.entrySet() 的 iterator.remove() 是可变的，这里包一层禁掉；
					// 同时用 ReadOnlyEntry 把每个 Entry<K,V> 转成 Entry<K,VReadOnly>。
					private final Iterator<Map.Entry<K, V>> it = map.entrySet().iterator();

					@Override
					public boolean hasNext() {
						return it.hasNext();
					}

					@Override
					public Map.Entry<K, VReadOnly> next() {
						return new ReadOnlyEntry<>(it.next());
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}

			@Override
			public int size() {
				return map.size();
			}
		};
	}

	@Override
	public Iterator<Map.Entry<K, VReadOnly>> iterator() {
		return entrySet().iterator();
	}

	public PSortedMap2<K, V> copy() {
		return map.copy();
	}

	public void encode(ByteBuffer bb) {
		map.encode(bb);
	}

	@Override
	public int hashCode() {
		return map.hashCode();
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		return obj instanceof PSortedMap2ReadOnly && map.equals(((PSortedMap2ReadOnly<?, ?, ?>)obj).map);
	}

	@Override
	public String toString() {
		return map.toString();
	}

	// ---- helpers ----

	private static <K, V, VReadOnly> @Nullable Entry<K, VReadOnly> wrap(@Nullable Entry<K, V> e) {
		return e == null ? null : new ReadOnlyEntry<>(e);
	}

	private static final class ReadOnlyEntry<K, V, VReadOnly> implements Map.Entry<K, VReadOnly> {
		private final Map.Entry<K, V> e;

		ReadOnlyEntry(Map.Entry<K, V> e) {
			this.e = e;
		}

		@Override
		public K getKey() {
			return e.getKey();
		}

		@SuppressWarnings("unchecked")
		@Override
		public VReadOnly getValue() {
			return (VReadOnly)e.getValue();
		}

		@Override
		public VReadOnly setValue(VReadOnly value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int hashCode() {
			return e.hashCode();
		}

		@Override
		public boolean equals(@Nullable Object o) {
			if (this == o)
				return true;
			if (!(o instanceof Map.Entry<?, ?> that))
				return false;
			return Objects.equals(e.getKey(), that.getKey())
					&& Objects.equals(e.getValue(), that.getValue());
		}

		@Override
		public String toString() {
			return e.toString();
		}
	}
}
