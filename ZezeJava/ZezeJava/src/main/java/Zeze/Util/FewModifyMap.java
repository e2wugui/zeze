package Zeze.Util;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FewModifyMap<K, V> implements Map<K,V>, Cloneable, Serializable {
	private volatile Map<K, V> read;
	private Map<K, V> write = new HashMap<>();

	private Map<K, V> prepareRead() {
		if (null != read)
			return read;

		synchronized (write) {
			read = new HashMap<>();
			read.putAll(write);
			return read;
		}
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
		synchronized (write) {
			var r = write.put(key, value);
			read = null;
			return r;
		}
	}

	@Override
	public V remove(Object key) {
		synchronized (write) {
			var r = write.remove(key);
			read = null;
			return r;
		}
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		synchronized (write) {
			write.putAll(m);
			read = null;
		}
	}

	@Override
	public void clear() {
		synchronized (write) {
			write.clear();
			read = null;
		}
	}

	@Override
	public Set<K> keySet() {
		return Collections.unmodifiableSet(prepareRead().keySet());
	}

	@Override
	public Collection<V> values() {
		return Collections.unmodifiableCollection(prepareRead().values());
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return Collections.unmodifiableSet(prepareRead().entrySet());
	}
}
