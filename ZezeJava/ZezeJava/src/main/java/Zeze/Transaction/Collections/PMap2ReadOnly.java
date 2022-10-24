package Zeze.Transaction.Collections;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;

public class PMap2ReadOnly<K, V extends Bean, VReadOnly> implements Iterable<Map.Entry<K, VReadOnly>> {
	private final PMap2<K, V> map;

	public PMap2ReadOnly(PMap2<K, V> map) {
		this.map = map;
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	public int size() {
		return map.size();
	}

	@SuppressWarnings("unchecked")
	public VReadOnly getReadOnly(Object key) {
		//noinspection SuspiciousMethodCalls
		return (VReadOnly)map.get(key);
	}

	public boolean containsValue(Object v) {
		//noinspection SuspiciousMethodCalls
		return map.containsValue(v);
	}

	public boolean containsKey(Object key) {
		//noinspection SuspiciousMethodCalls
		return map.containsKey(key);
	}

	@SuppressWarnings("unchecked")
	public void copyTo(Map.Entry<K, VReadOnly>[] array, int arrayIndex) {
		int index = arrayIndex;
		for (var e : map.entrySet())
			array[index++] = (Map.Entry<K, VReadOnly>)e;
	}

	public Set<K> keySet() {
		return new AbstractSet<>() {
			@Override
			public Iterator<K> iterator() {
				return new Iterator<>() {
					private final Iterator<Map.Entry<K, VReadOnly>> it = entrySet().iterator();

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
						throw new UnsupportedOperationException();
					}
				};
			}

			@Override
			public int size() {
				return map.size();
			}
		};
	}

	public java.util.Collection<VReadOnly> values() {
		return new AbstractCollection<>() {
			@Override
			public Iterator<VReadOnly> iterator() {
				return new Iterator<>() {
					private final Iterator<Map.Entry<K, VReadOnly>> it = entrySet().iterator();

					@Override
					public boolean hasNext() {
						return it.hasNext();
					}

					@Override
					public VReadOnly next() {
						return it.next().getValue();
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}

			@Override
			public int size() {
				return map.size();
			}
		};
	}

	public Set<Map.Entry<K, VReadOnly>> entrySet() {
		return new AbstractSet<>() {
			@Override
			public Iterator<Map.Entry<K, VReadOnly>> iterator() {
				return new Iterator<>() {
					private final Iterator<Map.Entry<K, V>> it = map.entrySet().iterator();

					@Override
					public boolean hasNext() {
						return it.hasNext();
					}

					@SuppressWarnings("unchecked")
					@Override
					public Map.Entry<K, VReadOnly> next() {
						return (Map.Entry<K, VReadOnly>)it.next();
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}

			@Override
			public int size() {
				return map.size();
			}
		};
	}

	@Override
	public Iterator<Map.Entry<K, VReadOnly>> iterator() {
		return entrySet().iterator();
	}

	public PMap2<K, V> copy() {
		return map.copy();
	}

	public void encode(ByteBuffer bb) {
		map.encode(bb);
	}

	@Override
	public int hashCode() {
		return map.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof PMap2ReadOnly && map.equals(((PMap2ReadOnly<?, ?, ?>)obj).map);
	}

	@Override
	public String toString() {
		return map.toString();
	}
}
