package Zeze.Util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 写少读多的Map：写侧HashMap加锁修改，读侧走不可变快照（写后首次读重建）。
 * null契约与HashMap一致：允许null key与null value，快照可区分null value与absent
 * （FND7-39起快照为HashMap拷贝，不再因null建不成）。需拒绝null key请用
 * {@link FewModifySortedMap}（TreeMap契约，key不允许null、需可比较）。
 */
public class FewModifyMap<K, V> implements Map<K, V>, Cloneable {
	private transient volatile @Nullable Map<K, V> read;
	private final @NotNull HashMap<K, V> write;
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

	public FewModifyMap(@NotNull Map<? extends K, ? extends V> m) {
		write = new HashMap<>(m);
	}

	private @NotNull Map<K, V> prepareRead() {
		var r = read;
		if (r == null) {
			writeLock.lock();
			try {
				if ((r = read) == null)
					// HashMap 拷贝允许 null value（FND7-39，对齐 FewModifySortedMap 的 TreeMap 快照）：
					// Map.copyOf 遇 null value/key 抛 NPE 且快照建不成（read 恒 null），此后任意读
					// 方法重复抛 NPE，读侧永久瘫痪。unmodifiable 包装保持快照只读契约不弱化。
					//noinspection Java9CollectionFactory
					read = r = Collections.unmodifiableMap(new HashMap<>(write));
			} finally {
				writeLock.unlock();
			}
		}
		return r;
	}

	public @NotNull Map<K, V> snapshot() {
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
	public @NotNull FewModifyMap<K, V> clone() throws CloneNotSupportedException {
		if (getClass() == FewModifyMap.class)
			return new FewModifyMap<>(prepareRead());
		throw new CloneNotSupportedException();
	}

	@Override
	public @NotNull String toString() {
		return prepareRead().toString();
	}
}
