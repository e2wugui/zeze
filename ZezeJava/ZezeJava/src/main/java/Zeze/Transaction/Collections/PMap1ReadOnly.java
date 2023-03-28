package Zeze.Transaction.Collections;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import Zeze.Serialize.ByteBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PMap1ReadOnly<K, V> implements Iterable<Map.Entry<K, V>> {
	private final @NotNull PMap1<K, V> map;

	public PMap1ReadOnly(@NotNull PMap1<K, V> map) {
		this.map = map;
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	public int size() {
		return map.size();
	}

	public @Nullable V getReadOnly(@NotNull K key) {
		return map.get(key);
	}

	public boolean containsValue(@NotNull V v) {
		return map.containsValue(v);
	}

	public boolean containsKey(@NotNull K key) {
		return map.containsKey(key);
	}

	public void copyTo(Map.Entry<K, V> @NotNull [] array, int arrayIndex) {
		map.copyTo(array, arrayIndex);
	}

	public @NotNull Set<K> keySet() {
		return new AbstractSet<>() {
			@Override
			public Iterator<K> iterator() {
				return new Iterator<>() {
					private final Iterator<Map.Entry<K, V>> it = entrySet().iterator();

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

	public @NotNull java.util.Collection<V> values() {
		return new AbstractCollection<>() {
			@Override
			public Iterator<V> iterator() {
				return new Iterator<>() {
					private final Iterator<Map.Entry<K, V>> it = entrySet().iterator();

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

	public @NotNull Set<Map.Entry<K, V>> entrySet() {
		return new AbstractSet<>() {
			@Override
			public Iterator<Map.Entry<K, V>> iterator() {
				return new Iterator<>() {
					private final Iterator<Map.Entry<K, V>> it = map.entrySet().iterator();

					@Override
					public boolean hasNext() {
						return it.hasNext();
					}

					@Override
					public Map.Entry<K, V> next() {
						return it.next();
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
	public @NotNull Iterator<Map.Entry<K, V>> iterator() {
		return entrySet().iterator();
	}

	public @NotNull PMap1<K, V> copy() {
		return map.copy();
	}

	public void encode(@NotNull ByteBuffer bb) {
		map.encode(bb);
	}

	@Override
	public int hashCode() {
		return map.hashCode();
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		return obj instanceof PMap1ReadOnly && map.equals(((PMap1ReadOnly<?, ?>)obj).map);
	}

	@Override
	public @NotNull String toString() {
		return map.toString();
	}
}
