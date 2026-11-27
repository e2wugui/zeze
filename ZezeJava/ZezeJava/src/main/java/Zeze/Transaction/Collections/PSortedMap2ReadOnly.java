package Zeze.Transaction.Collections;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collections;
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

	public @Nullable VReadOnly get(K key) {
		//noinspection unchecked
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

	// NavigableMap 只读视图：单条 Entry 经 ReadOnlyEntry 包装禁写；子视图由 pcollections 保证只读（泛型不变需强转），
	// 键/键集直接透传，不暴露破坏性的 pollFirstEntry/pollLastEntry。

	public @Nullable Entry<K, VReadOnly> lowerEntry(K key) {
		return wrap(map.lowerEntry(key));
	}

	public @Nullable K lowerKey(K key) {
		return map.lowerKey(key);
	}

	public @Nullable Entry<K, VReadOnly> floorEntry(K key) {
		return wrap(map.floorEntry(key));
	}

	public @Nullable K floorKey(K key) {
		return map.floorKey(key);
	}

	public @Nullable Entry<K, VReadOnly> ceilingEntry(K key) {
		return wrap(map.ceilingEntry(key));
	}

	public @Nullable K ceilingKey(K key) {
		return map.ceilingKey(key);
	}

	public @Nullable Entry<K, VReadOnly> higherEntry(K key) {
		return wrap(map.higherEntry(key));
	}

	public @Nullable K higherKey(K key) {
		return map.higherKey(key);
	}

	public @Nullable Entry<K, VReadOnly> firstEntry() {
		return wrap(map.firstEntry());
	}

	public @Nullable Entry<K, VReadOnly> lastEntry() {
		return wrap(map.lastEntry());
	}

	public NavigableMap<K, VReadOnly> descendingMap() {
		//noinspection unchecked
		return (NavigableMap<K, VReadOnly>)map.descendingMap();
	}

	public NavigableSet<K> navigableKeySet() {
		return map.navigableKeySet();
	}

	public NavigableSet<K> descendingKeySet() {
		return map.descendingKeySet();
	}

	public NavigableMap<K, VReadOnly> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
		//noinspection unchecked
		return (NavigableMap<K, VReadOnly>)map.subMap(fromKey, fromInclusive, toKey, toInclusive);
	}

	public NavigableMap<K, VReadOnly> headMap(K toKey, boolean inclusive) {
		//noinspection unchecked
		return (NavigableMap<K, VReadOnly>)map.headMap(toKey, inclusive);
	}

	public NavigableMap<K, VReadOnly> tailMap(K fromKey, boolean inclusive) {
		//noinspection unchecked
		return (NavigableMap<K, VReadOnly>)map.tailMap(fromKey, inclusive);
	}

	public @Nullable Comparator<? super K> comparator() {
		return map.comparator();
	}

	public SortedMap<K, VReadOnly> subMap(K fromKey, K toKey) {
		//noinspection unchecked
		return (SortedMap<K, VReadOnly>)map.subMap(fromKey, toKey);
	}

	public SortedMap<K, VReadOnly> headMap(K toKey) {
		//noinspection unchecked
		return (SortedMap<K, VReadOnly>)map.headMap(toKey);
	}

	public SortedMap<K, VReadOnly> tailMap(K fromKey) {
		//noinspection unchecked
		return (SortedMap<K, VReadOnly>)map.tailMap(fromKey);
	}

	public K firstKey() {
		return map.firstKey();
	}

	public K lastKey() {
		return map.lastKey();
	}

	public Set<K> keySet() {
		return Collections.unmodifiableSet(map.keySet());
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
			public boolean contains(Object item) {
				return map.entrySet().contains(item);
			}

			@Override
			public Iterator<Map.Entry<K, VReadOnly>> iterator() {
				return new Iterator<>() {
					// 包装 entrySet 迭代器以禁止 remove 并暴露 ReadOnlyEntry。
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

		@Override
		public VReadOnly getValue() {
			//noinspection unchecked
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
