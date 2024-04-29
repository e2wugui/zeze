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

public class FewModifySortedMap<K, V> implements SortedMap<K, V>, java.io.Serializable {
	private transient volatile SortedMap<K, V> read;
	private final TreeMap<K, V> write;
	private final ReentrantLock writeLock = new ReentrantLock();

	public FewModifySortedMap() {
		write = new TreeMap<>();
	}

	protected SortedMap<K, V> prepareRead() {
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
	public V get(Object key) {
		return prepareRead().get(key);
	}

	@Override
	public V put(K key, V value) {
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
	public V putIfAbsent(K key, V value) {
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
	public V replace(K key, V value) {
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
	public V remove(Object key) {
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
	public void putAll(Map<? extends K, ? extends V> m) {
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
	public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
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
	public Comparator<? super K> comparator() {
		return prepareRead().comparator();
	}

	@NotNull
	@Override
	public SortedMap<K, V> subMap(K fromKey, K toKey) {
		return prepareRead().subMap(fromKey, toKey);
	}

	@NotNull
	@Override
	public SortedMap<K, V> headMap(K toKey) {
		return prepareRead().headMap(toKey);
	}

	@NotNull
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

	// 这个方法暴露了可写的操作，不安全。
	public NavigableMap<K, V> descendingMap() {
		var tree = (TreeMap<K, V>)prepareRead();
		return tree.descendingMap();
	}

	@Override
	public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
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
	public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
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
	public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
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
	public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
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
	public FewModifySortedMap<K, V> clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	@Override
	public String toString() {
		return prepareRead().toString();
	}
}
