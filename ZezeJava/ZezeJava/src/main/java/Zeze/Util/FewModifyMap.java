package Zeze.Util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("CloneableClassWithoutClone")
public class FewModifyMap<K, V> implements Map<K, V>, Cloneable, Serializable {
	private volatile Map<K, V> read;
	private final Map<K, V> write = new HashMap<>();

	private Map<K, V> prepareRead() {
		var r = read;
		if (r == null) {
			synchronized (write) {
				if ((r = read) == null)
					read = r = new HashMap<>(write);
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
		synchronized (write) {
			var prev = write.put(key, value);
			read = null;
			return prev;
		}
	}

	@Override
	public V remove(Object key) {
		synchronized (write) {
			var prev = write.remove(key);
			read = null;
			return prev;
		}
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		if (m.isEmpty())
			return;
		synchronized (write) {
			write.putAll(m);
			read = null;
		}
	}

	@Override
	public void clear() {
		synchronized (write) {
			if (write.isEmpty())
				return;
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
