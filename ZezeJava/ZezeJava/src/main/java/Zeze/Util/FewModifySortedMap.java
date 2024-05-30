package Zeze.Util;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FewModifySortedMap<K, V> implements SortedMap<K, V>, Cloneable {
	private transient volatile @Nullable SortedMap<K, V> read;
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

	private @NotNull SortedMap<K, V> prepareRead() {
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

	public @NotNull SortedMap<K, V> snapshot() {
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

	@Override
	public @NotNull SortedMap<K, V> subMap(K fromKey, K toKey) {
		return prepareRead().subMap(fromKey, toKey);
	}

	@Override
	public @NotNull SortedMap<K, V> headMap(K toKey) {
		return prepareRead().headMap(toKey);
	}

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

	@Override
	public @NotNull Set<K> keySet() {
		return prepareRead().keySet();
	}

	@Override
	public @NotNull Collection<V> values() {
		return prepareRead().values();
	}

	@Override
	public @NotNull Set<Entry<K, V>> entrySet() {
		return prepareRead().entrySet();
	}

	// 这个方法暴露了可写的操作，不安全。
	public @NotNull NavigableMap<K, V> descendingMap() {
		var tree = (TreeMap<K, V>)prepareRead();
		return tree.descendingMap();
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
