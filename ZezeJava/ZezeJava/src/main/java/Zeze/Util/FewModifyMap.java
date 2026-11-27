package Zeze.Util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/**
 * 写少读多的 Map：写侧 HashMap 加锁修改，读侧走不可变快照（写后首次读重建），允许 null key/value。
 */
public class FewModifyMap<K extends @Nullable Object, V extends @Nullable Object> implements Map<K, V>, Cloneable {
	private transient volatile @Nullable Map<K, V> read;
	private final HashMap<K, V> write;
	private transient final ReentrantLock writeLock = new ReentrantLock();

	public FewModifyMap() {
		write = new HashMap<>();
	}

	public FewModifyMap(int initialCapacity) {
		write = new HashMap<>(initialCapacity);
	}

	public FewModifyMap(int initialCapacity, float loadFactor) {
		write = new HashMap<>(initialCapacity, loadFactor);
	}

	public FewModifyMap(Map<? extends K, ? extends V> m) {
		write = new HashMap<>(m);
	}

	private Map<K, V> prepareRead() {
		var r = read;
		if (r == null) {
			writeLock.lock();
			try {
				if ((r = read) == null) {
					// 用 HashMap 拷贝而非 Map.copyOf，以支持 null key/value 并保持只读快照语义。
					read = r = Collections.unmodifiableMap(new HashMap<>(write));
				}
			} finally {
				writeLock.unlock();
			}
		}
		return r;
	}

	/** 只读快照：不受本Map后续写入影响；key/value对象仍共享。 */
	public Map<K, V> snapshot() {
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
	public boolean containsKey(@Nullable Object key) {
		return prepareRead().containsKey(key);
	}

	@Override
	public boolean containsValue(@Nullable Object value) {
		return prepareRead().containsValue(value);
	}

	@Override
	public @Nullable V get(@Nullable Object key) {
		return prepareRead().get(key);
	}

	@Override
	public V getOrDefault(@Nullable Object key, V defaultValue) {
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
	public @Nullable V remove(@Nullable Object key) {
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
	public boolean remove(@Nullable Object key, @Nullable Object value) {
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

		var snapshot = new LinkedHashMap<K, V>(m);
		writeLock.lock();
		try {
			write.putAll(snapshot);
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

	@SuppressWarnings("MethodDoesntCallSuperMethod")
	@Override
	public FewModifyMap<K, V> clone() throws CloneNotSupportedException {
		if (getClass() == FewModifyMap.class)
			return new FewModifyMap<>(prepareRead());
		throw new CloneNotSupportedException();
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
