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
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Log;
import Zeze.Transaction.Transaction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.pcollections.Empty;

public abstract class PSortedMap<K extends Comparable<K>, V> extends Collection
		implements NavigableMap<K, V>, Iterable<Map.Entry<K, V>> {
	@NotNull org.pcollections.PSortedMap<K, V> map = Empty.sortedMap();

	public final @NotNull org.pcollections.PSortedMap<K, V> getMap() {
		if (isManaged()) {
			var txn = Transaction.getCurrentVerifyRead(this);
			if (txn == null)
				return map;
			//noinspection DataFlowIssue
			Log log = txn.getLog(parent().objectId() + variableId());
			if (log == null)
				return map;
			@SuppressWarnings("unchecked")
			var mapLog = (LogSortedMap1<K, V>)log;
			return mapLog.getValue();
		}
		return map;
	}

	@Override
	public abstract void encode(@NotNull ByteBuffer bb);

	@Override
	public abstract void decode(@NotNull IByteBuffer bb);

	public abstract ByteBuffer encodeKey(K key);

	public abstract K decodeKey(@NotNull ByteBuffer bb);

	@Override
	public @NotNull Iterator<Entry<K, V>> iterator() {
		return entrySet().iterator();
	}

	@Override
	public Entry<K, V> lowerEntry(K key) {
		return getMap().lowerEntry(key);
	}

	@Override
	public K lowerKey(K key) {
		return getMap().lowerKey(key);
	}

	@Override
	public Entry<K, V> floorEntry(K key) {
		return getMap().floorEntry(key);
	}

	@Override
	public K floorKey(K key) {
		return getMap().floorKey(key);
	}

	@Override
	public Entry<K, V> ceilingEntry(K key) {
		return getMap().ceilingEntry(key);
	}

	@Override
	public K ceilingKey(K key) {
		return getMap().ceilingKey(key);
	}

	@Override
	public Entry<K, V> higherEntry(K key) {
		return getMap().higherEntry(key);
	}

	@Override
	public K higherKey(K key) {
		return getMap().higherKey(key);
	}

	@Override
	public Entry<K, V> firstEntry() {
		return getMap().firstEntry();
	}

	@Override
	public Entry<K, V> lastEntry() {
		return getMap().lastEntry();
	}

	@Override
	public Entry<K, V> pollFirstEntry() {
		var first = getMap().firstEntry();
		if (first != null)
			remove(first.getKey());
		return first;
	}

	@Override
	public Entry<K, V> pollLastEntry() {
		var last = getMap().lastEntry();
		if (last != null)
			remove(last.getKey());
		return last;
	}

	@Override
	public NavigableMap<K, V> descendingMap() {
		return getMap().descendingMap();
	}

	@Override
	public NavigableSet<K> navigableKeySet() {
		return getMap().navigableKeySet();
	}

	@Override
	public NavigableSet<K> descendingKeySet() {
		return getMap().descendingKeySet();
	}

	@Override
	public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
		return getMap().subMap(fromKey, fromInclusive, toKey, toInclusive);
	}

	@Override
	public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
		return getMap().headMap(toKey, inclusive);
	}

	@Override
	public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
		return getMap().tailMap(fromKey, inclusive);
	}

	@Override
	public Comparator<? super K> comparator() {
		return getMap().comparator();
	}

	@Override
	public @NotNull SortedMap<K, V> subMap(K fromKey, K toKey) {
		return getMap().subMap(fromKey, toKey);
	}

	@Override
	public @NotNull SortedMap<K, V> headMap(K toKey) {
		return getMap().headMap(toKey);
	}

	@Override
	public @NotNull SortedMap<K, V> tailMap(K fromKey) {
		return getMap().tailMap(fromKey);
	}

	@Override
	public K firstKey() {
		return getMap().firstKey();
	}

	@Override
	public K lastKey() {
		return getMap().lastKey();
	}

	@Override
	public int size() {
		return getMap().size();
	}

	@Override
	public boolean isEmpty() {
		return getMap().isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return getMap().containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return getMap().containsValue(value);
	}

	@Override
	public V get(Object key) {
		return getMap().get(key);
	}

	@Override
	public abstract @Nullable V put(K key, V value);

	@Override
	public abstract V remove(Object key);

	public abstract boolean remove(@NotNull Map.Entry<K, V> item);

	@Override
	public abstract void putAll(@NotNull Map<? extends K, ? extends V> m);

	@Override
	public abstract void clear();

	@Override
	public @NotNull Set<K> keySet() {
		return new AbstractSet<>() {
			@Override
			public @NotNull Iterator<K> iterator() {
				return new Iterator<>() {
					private final Iterator<Entry<K, V>> it = entrySet().iterator();

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
						it.remove();
					}
				};
			}

			@Override
			public int size() {
				return getMap().size();
			}
		};
	}

	@Override
	public @NotNull java.util.Collection<V> values() {
		return new AbstractCollection<>() {
			@Override
			public @NotNull Iterator<V> iterator() {
				return new Iterator<>() {
					private final Iterator<Entry<K, V>> it = entrySet().iterator();

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
						it.remove();
					}
				};
			}

			@Override
			public int size() {
				return getMap().size();
			}
		};
	}

	@Override
	public @NotNull Set<Entry<K, V>> entrySet() {
		return new AbstractSet<>() {
			@Override
			public @NotNull Iterator<Entry<K, V>> iterator() {
				return new Iterator<>() {
					private final Iterator<Map.Entry<K, V>> it = getMap().entrySet().iterator();
					private Map.Entry<K, V> next;

					@Override
					public boolean hasNext() {
						return it.hasNext();
					}

					@Override
					public Entry<K, V> next() {
						return next = it.next();
					}

					@Override
					public void remove() {
						PSortedMap.this.remove(next.getKey());
					}
				};
			}

			@Override
			public int size() {
				return getMap().size();
			}
		};
	}

	@Override
	public int hashCode() {
		return getMap().hashCode();
	}

	@Override
	public boolean equals(@Nullable Object o) {
		return o instanceof PSortedMap && getMap().equals(((PSortedMap<?, ?>)o).getMap());
	}

	@Override
	public @NotNull String toString() {
		var sb = new StringBuilder();
		ByteBuffer.BuildString(sb, getMap());
		return sb.toString();
	}
}
