package Zeze.Transaction.Collections;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Transaction;

public abstract class PMap<K, V> extends Collection implements Map<K, V>, Iterable<Map.Entry<K, V>> {
	public org.pcollections.PMap<K, V> _map = org.pcollections.Empty.map();

	@Override
	public final V get(Object key) {
		return getMap().get(key);
	}

	@Override
	public abstract V put(K key, V value);

	@Override
	public abstract void putAll(Map<? extends K, ? extends V> m);

	@Override
	public abstract V remove(Object key);
	public abstract boolean remove(Map.Entry<K, V> item);

	@Override
	public abstract void clear();

	public final void copyTo(Map.Entry<K, V>[] array, int arrayIndex) {
		int index = arrayIndex;
		for (var e : getMap().entrySet()) {
			array[index++] = e;
		}
	}

	protected final org.pcollections.PMap<K, V> getMap() {
		if (isManaged()) {
			var txn = Transaction.getCurrent();
			if (txn == null) {
				return _map;
			}
			txn.VerifyRecordAccessed(this, true);
			var log = txn.GetLog(getParent().getObjectId() + getVariableId());
			if (log == null)
				return _map;
			@SuppressWarnings("unchecked")
			var mapLog = (LogMap1<K, V>)log;
			return mapLog.getValue();
		}
		return _map;
	}

	@Override
	public final int size() {
		return getMap().size();
	}

	@Override
	public final boolean containsValue(Object v) {
		return getMap().containsValue(v);
	}

	@Override
	public final boolean containsKey(Object key) {
		return getMap().containsKey(key);
	}

	@Override
	public boolean isEmpty() {
		return getMap().isEmpty();
	}

	@Override
	public Set<K> keySet() {
		return new AbstractSet<>() {
			@Override
			public Iterator<K> iterator() {
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
	public java.util.Collection<V> values() {
		return new AbstractCollection<>() {
			@Override
			public Iterator<V> iterator() {
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
	public Set<Map.Entry<K, V>> entrySet() {
		return new AbstractSet<>() {
			@Override
			public Iterator<Entry<K, V>> iterator() {
				return new Iterator<>() {
					private final Iterator<Map.Entry<K, V>> it = getMap().entrySet().iterator();
					Map.Entry<K, V> next;

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
						PMap.this.remove(next.getKey());
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
	public Iterator<Map.Entry<K, V>> iterator() {
		return entrySet().iterator();
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		ByteBuffer.BuildString(sb, getMap());
		return sb.toString();
	}
}
