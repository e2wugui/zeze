package Zeze.Transaction.Collections;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;

import Zeze.Serialize.ByteBuffer;
import org.jspecify.annotations.Nullable;

import java.util.Map.Entry;

/** PSortedMap1 的只读视图：仅暴露查询与遍历，修改入口抛 UnsupportedOperationException。 */
public class PSortedMap1ReadOnly<K extends Comparable<K>, V> implements Iterable<Map.Entry<K, V>> {
	private final PSortedMap1<K, V> map;

	public PSortedMap1ReadOnly(PSortedMap1<K, V> map) {
		this.map = map;
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	public int size() {
		return map.size();
	}

	public @Nullable V get(K key) {
		return map.get(key);
	}

	public boolean containsValue(V v) {
		return map.containsValue(v);
	}

	public boolean containsKey(K key) {
		return map.containsKey(key);
	}

	public void copyTo(Map.Entry<K, V>[] array, int arrayIndex) {
		for (var e : entrySet())
			array[arrayIndex++] = e;
	}

	// ---- NavigableMap readonly view methods ----
	// 全部委托给底层 PSortedMap1，最终走到 org.pcollections.PSortedMap，
	// pcollections 返回的 NavigableMap / NavigableSet 视图本身就是只读的，无需再包一层。
	// 注意：pollFirstEntry / pollLastEntry 是破坏性方法，不暴露。

	public @Nullable Entry<K, V> lowerEntry(K key) {
		return map.lowerEntry(key);
	}

	public @Nullable K lowerKey(K key) {
		return map.lowerKey(key);
	}

	public @Nullable Entry<K, V> floorEntry(K key) {
		return map.floorEntry(key);
	}

	public @Nullable K floorKey(K key) {
		return map.floorKey(key);
	}

	public @Nullable Entry<K, V> ceilingEntry(K key) {
		return map.ceilingEntry(key);
	}

	public @Nullable K ceilingKey(K key) {
		return map.ceilingKey(key);
	}

	public @Nullable Entry<K, V> higherEntry(K key) {
		return map.higherEntry(key);
	}

	public @Nullable K higherKey(K key) {
		return map.higherKey(key);
	}

	public @Nullable Entry<K, V> firstEntry() {
		return map.firstEntry();
	}

	public @Nullable Entry<K, V> lastEntry() {
		return map.lastEntry();
	}

	public NavigableMap<K, V> descendingMap() {
		return map.descendingMap();
	}

	public NavigableSet<K> navigableKeySet() {
		return map.navigableKeySet();
	}

	public NavigableSet<K> descendingKeySet() {
		return map.descendingKeySet();
	}

	public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
		return map.subMap(fromKey, fromInclusive, toKey, toInclusive);
	}

	public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
		return map.headMap(toKey, inclusive);
	}

	public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
		return map.tailMap(fromKey, inclusive);
	}

	public @Nullable Comparator<? super K> comparator() {
		return map.comparator();
	}

	public SortedMap<K, V> subMap(K fromKey, K toKey) {
		return map.subMap(fromKey, toKey);
	}

	public SortedMap<K, V> headMap(K toKey) {
		return map.headMap(toKey);
	}

	public SortedMap<K, V> tailMap(K fromKey) {
		return map.tailMap(fromKey);
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
					private final Iterator<Map.Entry<K, V>> it = entrySet().iterator();

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

	public java.util.Collection<V> values() {
		return new AbstractCollection<>() {
			@Override
			public Iterator<V> iterator() {
				return new Iterator<>() {
					private final Iterator<Map.Entry<K, V>> it = entrySet().iterator();

					@Override
					public boolean hasNext() {
						return it.hasNext();
					}

					@Override
					public V next() {
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

	public Set<Map.Entry<K, V>> entrySet() {
		return new AbstractSet<>() {
			@Override
			public Iterator<Map.Entry<K, V>> iterator() {
				return new Iterator<>() {
					// PSortedMap.entrySet() 返回的迭代器 remove() 是可变的，这里包一层禁掉。
					// next() 返回的 Entry 来自 pcollections，本身就是 immutable 的。
					private final Iterator<Map.Entry<K, V>> it = map.entrySet().iterator();

					@Override
					public boolean hasNext() {
						return it.hasNext();
					}

					@Override
					public Map.Entry<K, V> next() {
						return it.next();
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
	public Iterator<Map.Entry<K, V>> iterator() {
		return entrySet().iterator();
	}

	public PSortedMap1<K, V> copy() {
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
		return obj instanceof PSortedMap1ReadOnly<?, ?> o && map.equals(o.map);
	}

	@Override
	public String toString() {
		return map.toString();
	}
}
