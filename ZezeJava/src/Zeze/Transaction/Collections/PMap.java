package Zeze.Transaction.Collections;

import Zeze.Transaction.*;
import java.util.*;
import java.util.stream.Collectors;

import org.pcollections.Empty;

public abstract class PMap<K, V> extends PCollection {
	private final LogFactory<org.pcollections.PMap<K, V>> _logFactory;
	protected org.pcollections.PMap<K, V> map;

	public PMap(long logKey, LogFactory<org.pcollections.PMap<K, V>> logFactory) {
		super(logKey);
		this._logFactory = logFactory;
		map = Empty.map();
	}

	public final Log NewLog(org.pcollections.PMap<K, V> value) {
		return _logFactory.create(value);
	}

	public abstract class LogV extends Log {
		public org.pcollections.PMap<K, V> Value;
		protected LogV(Bean bean, org.pcollections.PMap<K, V> value) {
			super(bean);
			Value = value;
		}

		protected final void Commit(PMap<K, V> variable) {
			variable.map = Value;
		}
	}

	protected final org.pcollections.PMap<K, V> getData() {
		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			if (txn == null) {
				return map;
			}
			txn.VerifyRecordAccessed(this, true);
			var log = txn.GetLog(LogKey);
			@SuppressWarnings("unchecked")
			var oldv = null != log ? ((LogV)log).Value : map;
			return oldv;
		}
		else {
			return map;
		}
	}

	public final int size() {
		return getData().size();
	}

	@Override
	public String toString() {
		return getData().entrySet().stream().map(e -> e.getKey().toString() + ":" + e.getValue().toString()).collect(Collectors.joining(",", "{", "}"));
	}

	public final boolean isReadOnly() {
		return false;
	}

	public abstract V put(K key, V value);
	public abstract void putAll(Map<? extends K, ? extends V> m);
	public abstract void clear();
	public abstract boolean remove(K key);
	public abstract boolean remove(Map.Entry<K, V> item);

	public final void copyTo(Map.Entry<K, V>[] array, int arrayIndex) {
		int index = arrayIndex;
		for (var e : getData().entrySet()) {
			array[index++] = e;
		}
	}

	public V get(K key) {
		return getData().get(key);
	}

	public final boolean containsValue(V v) {
		return getData().containsValue(v);
	}

	public final boolean containsKey(Object key) {
		return getData().containsKey(key);
	}

    public Set<K> keySet() {
        return getData().keySet();
    }

    public Collection<V> values() {
        return getData().values();
    }

    public Set<Map.Entry<K, V>> entrySet() {
        return getData().entrySet();
    }
}