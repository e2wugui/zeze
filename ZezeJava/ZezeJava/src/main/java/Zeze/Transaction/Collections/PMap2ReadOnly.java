package Zeze.Transaction.Collections;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import org.jspecify.annotations.Nullable;

/** PMap2 的只读视图：值以只读类型暴露，修改入口抛 UnsupportedOperationException。 */
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
	public @Nullable VReadOnly get(K key) {
		return (VReadOnly)map.get(key);
	}

	public boolean containsValue(VReadOnly v) {
		//noinspection SuspiciousMethodCalls
		return map.containsValue(v);
	}

	public boolean containsKey(K key) {
		return map.containsKey(key);
	}

	public void copyTo(Map.Entry<K, VReadOnly> [] array, int arrayIndex) {
		for (var e : entrySet())
			array[arrayIndex++] = e;
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

	private static class ReadOnlyEntry<K, V, VReadOnly> implements Map.Entry<K, VReadOnly> {
		private final Map.Entry<K, V> e;

		ReadOnlyEntry(Map.Entry<K, V> e) {
			this.e = e;
		}

		@Override
		public K getKey() {
			return e.getKey();
		}

		@SuppressWarnings("unchecked")
		@Override
		public VReadOnly getValue() {
			return (VReadOnly)e.getValue();
		}

		@Override
		public VReadOnly setValue(VReadOnly value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int hashCode() {
			return e.hashCode();
		}

		@Override
		public boolean equals(@Nullable Object o) {
			if (this == o)
				return true;
			if (!(o instanceof Map.Entry<?, ?> that))
				return false;
			return e.getKey().equals(that.getKey()) && e.getValue().equals(that.getValue());
		}
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

					@Override
					public Map.Entry<K, VReadOnly> next() {
						return new ReadOnlyEntry<>(it.next());
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
	public boolean equals(@Nullable Object obj) {
		return obj instanceof PMap2ReadOnly && map.equals(((PMap2ReadOnly<?, ?, ?>)obj).map);
	}

	@Override
	public String toString() {
		return map.toString();
	}
}
