package Zeze.Util;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

/**
 * 写少读多的有序 Map：写侧 TreeMap 加锁修改，读侧走不可变快照（写后首次读重建）。
 * key 需可比较且 key/value 按非空约束使用；视图方法均返回只读快照视图，写操作抛 {@link UnsupportedOperationException}。
 */
public class FewModifySortedMap<K extends Comparable<? super K>, V> implements NavigableMap<K, V> {
	private transient volatile @Nullable NavigableMap<K, V> read;
	private final TreeMap<K, V> write;
	private transient final ReentrantLock writeLock = new ReentrantLock();

	public FewModifySortedMap() {
		write = new TreeMap<>();
	}

	public FewModifySortedMap(@Nullable Comparator<? super K> comparator) {
		write = new TreeMap<>(comparator);
	}

	public FewModifySortedMap(Map<? extends K, ? extends V> m) {
		write = new TreeMap<>(m);
	}

	private NavigableMap<K, V> prepareRead() {
		var r = read;
		if (r == null) {
			writeLock.lock();
			try {
				if ((r = read) == null) {
					// 快照须继承 write 的比较器并做只读包装，避免通过视图修改共享快照。
					var tree = new TreeMap<K, V>(write.comparator());
					tree.putAll(write);
					r = Collections.unmodifiableNavigableMap(tree);
					read = r;
				}
			} finally {
				writeLock.unlock();
			}
		}
		return r;
	}

	/** 只读快照：不受本 Map 后续写入影响；key/value 对象仍共享。 */
	public NavigableMap<K, V> snapshot() {
		return prepareRead();
	}

	@Override
	public int size() {
		return prepareRead().size();
	}

	@Override
	public boolean isEmpty() {
		return prepareRead().isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return prepareRead().containsKey(key);
	}

	@Override
	public boolean containsValue(@Nullable Object value) {
		return prepareRead().containsValue(value);
	}

	@Override
	public @Nullable V get(Object key) {
		return prepareRead().get(key);
	}

	@Override
	public V getOrDefault(Object key, V defaultValue) {
		return prepareRead().getOrDefault(key, defaultValue);
	}

	@Override
	public @Nullable V put(K key, V value) {
		writeLock.lock();
		try {
			var prev = write.put(key, value);
			read = null;
			return prev;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public @Nullable V putIfAbsent(K key, V value) {
		writeLock.lock();
		try {
			var prev = write.putIfAbsent(key, value);
			read = null;
			return prev;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public @Nullable V replace(K key, V value) {
		writeLock.lock();
		try {
			var prev = write.replace(key, value);
			read = null;
			return prev;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public boolean replace(K key, V oldValue, V newValue) {
		writeLock.lock();
		try {
			if (!write.replace(key, oldValue, newValue))
				return false;
			read = null;
			return true;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public @Nullable V remove(Object key) {
		writeLock.lock();
		try {
			var prev = write.remove(key);
			read = null;
			return prev;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public boolean remove(Object key, @Nullable Object value) {
		writeLock.lock();
		try {
			if (!write.remove(key, value))
				return false;
			read = null;
			return true;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		if (m.isEmpty())
			return;
		// 保留源迭代中的每个条目，键是否相同由目标 TreeMap 的比较器判断。
		var snapshot = new ArrayList<Entry<K, V>>(m.size());
		for (var entry : m.entrySet())
			snapshot.add(new AbstractMap.SimpleImmutableEntry<>(entry));
		writeLock.lock();
		try {
			for (var entry : snapshot)
				write.put(entry.getKey(), entry.getValue());
		} finally {
			read = null;
			writeLock.unlock();
		}
	}

	@Override
	public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
		writeLock.lock();
		try {
			if (write.isEmpty())
				return;
			write.replaceAll(function);
		} finally {
			read = null;
			writeLock.unlock();
		}
	}

	@Override
	public void clear() {
		writeLock.lock();
		try {
			if (write.isEmpty())
				return;
			write.clear();
			read = null;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public @Nullable Comparator<? super K> comparator() {
		return prepareRead().comparator();
	}

	@Override
	public SortedMap<K, V> subMap(K fromKey, K toKey) {
		return prepareRead().subMap(fromKey, toKey);
	}

	@Override
	public SortedMap<K, V> headMap(K toKey) {
		return prepareRead().headMap(toKey);
	}

	@Override
	public SortedMap<K, V> tailMap(K fromKey) {
		return prepareRead().tailMap(fromKey);
	}

	@Override
	public K firstKey() {
		return prepareRead().firstKey();
	}

	@Override
	public K lastKey() {
		return prepareRead().lastKey();
	}

	@Override
	public Set<K> keySet() {
		return prepareRead().keySet();
	}

	@Override
	public Collection<V> values() {
		return prepareRead().values();
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return prepareRead().entrySet();
	}

	@Override
	public @Nullable Entry<K, V> lowerEntry(K key) {
		return prepareRead().lowerEntry(key);
	}

	@Override
	public @Nullable K lowerKey(K key) {
		return prepareRead().lowerKey(key);
	}

	@Override
	public @Nullable Entry<K, V> floorEntry(K key) {
		return prepareRead().floorEntry(key);
	}

	@Override
	public @Nullable K floorKey(K key) {
		return prepareRead().floorKey(key);
	}

	@Override
	public @Nullable Entry<K, V> ceilingEntry(K key) {
		return prepareRead().ceilingEntry(key);
	}

	@Override
	public @Nullable K ceilingKey(K key) {
		return prepareRead().ceilingKey(key);
	}

	@Override
	public @Nullable Entry<K, V> higherEntry(K key) {
		return prepareRead().higherEntry(key);
	}

	@Override
	public @Nullable K higherKey(K key) {
		return prepareRead().higherKey(key);
	}

	@Override
	public @Nullable Entry<K, V> firstEntry() {
		return prepareRead().firstEntry();
	}

	@Override
	public @Nullable Entry<K, V> lastEntry() {
		return prepareRead().lastEntry();
	}

	// poll 为写操作，在 writeLock 内修改 write 并失效读快照。
	@Override
	public @Nullable Entry<K, V> pollFirstEntry() {
		writeLock.lock();
		try {
			var e = write.pollFirstEntry();
			read = null;
			return e;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public @Nullable Entry<K, V> pollLastEntry() {
		writeLock.lock();
		try {
			var e = write.pollLastEntry();
			read = null;
			return e;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public NavigableMap<K, V> descendingMap() {
		return prepareRead().descendingMap();
	}

	@Override
	public NavigableSet<K> navigableKeySet() {
		return prepareRead().navigableKeySet();
	}

	@Override
	public NavigableSet<K> descendingKeySet() {
		return prepareRead().descendingKeySet();
	}

	@Override
	public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
		return prepareRead().subMap(fromKey, fromInclusive, toKey, toInclusive);
	}

	@Override
	public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
		return prepareRead().headMap(toKey, inclusive);
	}

	@Override
	public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
		return prepareRead().tailMap(fromKey, inclusive);
	}

	@Override
	public @Nullable V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
		writeLock.lock();
		try {
			var v = write.compute(key, remappingFunction);
			read = null;
			return v;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public @Nullable V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
		writeLock.lock();
		try {
			var v = write.computeIfAbsent(key, mappingFunction);
			read = null;
			return v;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public @Nullable V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
		writeLock.lock();
		try {
			var v = write.computeIfPresent(key, remappingFunction);
			read = null;
			return v;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public @Nullable V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
		writeLock.lock();
		try {
			var v = write.merge(key, value, remappingFunction);
			read = null;
			return v;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public String toString() {
		return prepareRead().toString();
	}

	/** 基于调用时刻快照比较内容；hashCode 随内容变化，勿直接作 Map/Set 键（需要稳定键请用 {@link #snapshot()}）。 */
	@Override
	public boolean equals(@Nullable Object obj) {
		return this == obj || obj instanceof Map && prepareRead().equals(obj);
	}

	@Override
	public int hashCode() {
		return prepareRead().hashCode();
	}
}
