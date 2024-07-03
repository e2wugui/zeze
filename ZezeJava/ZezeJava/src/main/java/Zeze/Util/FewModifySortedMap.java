package Zeze.Util;

import java.util.Collection;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FewModifySortedMap<K, V> implements NavigableMap<K, V>, Cloneable {
	private transient volatile @Nullable NavigableMap<K, V> read;
	private final @NotNull TreeMap<K, V> write;
	private transient final ReentrantLock writeLock = new ReentrantLock();

	public FewModifySortedMap() {
		write = new TreeMap<>();
	}

	public FewModifySortedMap(@Nullable Comparator<? super K> comparator) {
		write = new TreeMap<>(comparator);
	}

	public FewModifySortedMap(@NotNull Map<? extends K, ? extends V> m) {
		write = new TreeMap<>(m);
	}

	private @NotNull NavigableMap<K, V> prepareRead() {
		var r = read;
		if (r == null) {
			writeLock.lock();
			try {
				if ((r = read) == null) {
					r = new TreeMap<>();
					r.putAll(write);
					read = r;
				}
			} finally {
				writeLock.unlock();
			}
		}
		return r;
	}

	// 必须只读,不允许写,虽然不会抛异常
	public @NotNull NavigableMap<K, V> snapshot() {
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
	public boolean containsValue(Object value) {
		return prepareRead().containsValue(value);
	}

	@Override
	public @Nullable V get(Object key) {
		return prepareRead().get(key);
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
	public boolean remove(Object key, Object value) {
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
	public void putAll(@NotNull Map<? extends K, ? extends V> m) {
		if (m.isEmpty())
			return;
		writeLock.lock();
		try {
			write.putAll(m);
			read = null;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public void replaceAll(@NotNull BiFunction<? super K, ? super V, ? extends V> function) {
		writeLock.lock();
		try {
			if (write.isEmpty())
				return;
			write.replaceAll(function);
			read = null;
		} finally {
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

	// 必须只读,不允许写,虽然不会抛异常
	@Override
	public @NotNull SortedMap<K, V> subMap(K fromKey, K toKey) {
		return prepareRead().subMap(fromKey, toKey);
	}

	// 必须只读,不允许写,虽然不会抛异常
	@Override
	public @NotNull SortedMap<K, V> headMap(K toKey) {
		return prepareRead().headMap(toKey);
	}

	// 必须只读,不允许写,虽然不会抛异常
	@Override
	public @NotNull SortedMap<K, V> tailMap(K fromKey) {
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

	// 必须只读,不允许写,虽然不会抛异常
	@Override
	public @NotNull Set<K> keySet() {
		return prepareRead().keySet();
	}

	// 必须只读,不允许写,虽然不会抛异常
	@Override
	public @NotNull Collection<V> values() {
		return prepareRead().values();
	}

	// 必须只读,不允许写,虽然不会抛异常
	@Override
	public @NotNull Set<Entry<K, V>> entrySet() {
		return prepareRead().entrySet();
	}

	@Override
	public Entry<K, V> lowerEntry(K key) {
		return prepareRead().lowerEntry(key);
	}

	@Override
	public K lowerKey(K key) {
		return prepareRead().lowerKey(key);
	}

	@Override
	public Entry<K, V> floorEntry(K key) {
		return prepareRead().floorEntry(key);
	}

	@Override
	public K floorKey(K key) {
		return prepareRead().floorKey(key);
	}

	@Override
	public Entry<K, V> ceilingEntry(K key) {
		return prepareRead().ceilingEntry(key);
	}

	@Override
	public K ceilingKey(K key) {
		return prepareRead().ceilingKey(key);
	}

	@Override
	public Entry<K, V> higherEntry(K key) {
		return prepareRead().higherEntry(key);
	}

	@Override
	public K higherKey(K key) {
		return prepareRead().higherKey(key);
	}

	@Override
	public Entry<K, V> firstEntry() {
		return prepareRead().firstEntry();
	}

	@Override
	public Entry<K, V> lastEntry() {
		return prepareRead().lastEntry();
	}

	@Override
	public Entry<K, V> pollFirstEntry() {
		return prepareRead().pollFirstEntry();
	}

	@Override
	public Entry<K, V> pollLastEntry() {
		return prepareRead().pollLastEntry();
	}

	// 必须只读,不允许写,虽然不会抛异常
	@Override
	public @NotNull NavigableMap<K, V> descendingMap() {
		return prepareRead().descendingMap();
	}

	// 必须只读,不允许写,虽然不会抛异常
	@Override
	public NavigableSet<K> navigableKeySet() {
		return prepareRead().navigableKeySet();
	}

	// 必须只读,不允许写,虽然不会抛异常
	@Override
	public NavigableSet<K> descendingKeySet() {
		return prepareRead().descendingKeySet();
	}

	// 必须只读,不允许写,虽然不会抛异常
	@Override
	public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
		return prepareRead().subMap(fromKey, fromInclusive, toKey, toInclusive);
	}

	// 必须只读,不允许写,虽然不会抛异常
	@Override
	public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
		return prepareRead().headMap(toKey, inclusive);
	}

	// 必须只读,不允许写,虽然不会抛异常
	@Override
	public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
		return prepareRead().tailMap(fromKey, inclusive);
	}

	@Override
	public @Nullable V compute(K key, @NotNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
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
	public @Nullable V computeIfAbsent(K key, @NotNull Function<? super K, ? extends V> mappingFunction) {
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
	public @Nullable V computeIfPresent(K key, @NotNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
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
	public @Nullable V merge(K key, V value, @NotNull BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
		writeLock.lock();
		try {
			var v = write.merge(key, value, remappingFunction);
			read = null;
			return v;
		} finally {
			writeLock.unlock();
		}
	}

	@SuppressWarnings("MethodDoesntCallSuperMethod")
	@Override
	public @NotNull FewModifySortedMap<K, V> clone() throws CloneNotSupportedException {
		if (getClass() == FewModifySortedMap.class)
			return new FewModifySortedMap<>(prepareRead());
		throw new CloneNotSupportedException();
	}

	@Override
	public @NotNull String toString() {
		return prepareRead().toString();
	}
}
