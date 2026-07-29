package Zeze.Transaction.Collections;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public class PMap2ReadOnly<K, V extends Bean, VReadOnly> implements Iterable<Map.Entry<K, VReadOnly>> {
	private final @NotNull PMap2<K, V> map;

	public PMap2ReadOnly(@NotNull PMap2<K, V> map) {
		this.map = map;
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	public int size() {
		return map.size();
	}

	@SuppressWarnings("unchecked")
	public @Nullable VReadOnly get(@NotNull K key) {
		return (VReadOnly)map.get(key);
	}

	public boolean containsValue(@NotNull VReadOnly v) {
		//noinspection SuspiciousMethodCalls
		return map.containsValue(v);
	}

	public boolean containsKey(@NotNull K key) {
		return map.containsKey(key);
	}

	public void copyTo(Map.Entry<K, VReadOnly> @NotNull [] array, int arrayIndex) {
		for (var e : entrySet())
			array[arrayIndex++] = e;
	}

	public @NotNull Set<K> keySet() {
		return new AbstractSet<>() {
			@Override
			public @NonNull Iterator<K> iterator() {
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

	public @NotNull java.util.Collection<VReadOnly> values() {
		return new AbstractCollection<>() {
			@Override
			public @NonNull Iterator<VReadOnly> iterator() {
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

		ReadOnlyEntry(Map.Entry<K, V> e) { this.e = e; }

		@Override public K getKey() { return e.getKey(); }

		@SuppressWarnings("unchecked")
		@Override public VReadOnly getValue() { return (VReadOnly)e.getValue(); }

		@Override public VReadOnly setValue(VReadOnly value) {
			throw new UnsupportedOperationException();
		}

		@Override public int hashCode() { return e.hashCode(); }
		@Override public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof Map.Entry<?, ?> that)) return false;
			return e.getKey().equals(that.getKey()) && e.getValue().equals(that.getValue());
		}
	}

	public @NotNull Set<Map.Entry<K, VReadOnly>> entrySet() {
		return new AbstractSet<>() {
			@Override
			public @NonNull Iterator<Map.Entry<K, VReadOnly>> iterator() {
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
	public @NotNull Iterator<Map.Entry<K, VReadOnly>> iterator() {
		return entrySet().iterator();
	}

	public @NotNull PMap2<K, V> copy() {
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
		return obj instanceof PMap2ReadOnly && map.equals(((PMap2ReadOnly<?, ?, ?>)obj).map);
	}

	@Override
	public @NotNull String toString() {
		return map.toString();
	}
}
