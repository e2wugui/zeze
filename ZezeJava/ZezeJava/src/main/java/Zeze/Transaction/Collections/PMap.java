package Zeze.Transaction.Collections;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Log;
import Zeze.Transaction.Transaction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.pcollections.Empty;

public abstract class PMap<K, V> extends Collection implements Map<K, V>, Iterable<Map.Entry<K, V>> {
	@NotNull org.pcollections.PMap<K, V> map = Empty.map();

	@Override
	public final @Nullable V get(@NotNull Object key) {
		return getMap().get(key);
	}

	@Override
	public abstract @Nullable V put(@NotNull K key, @NotNull V value);

	@Override
	public abstract void putAll(@NotNull Map<? extends K, ? extends V> m);

	@Override
	public abstract @Nullable V remove(@NotNull Object key);

	public abstract boolean remove(@NotNull Map.Entry<K, V> item);

	@Override
	public abstract void clear();

	public final void copyTo(Map.Entry<K, V> @NotNull [] array, int arrayIndex) {
		for (var e : getMap().entrySet())
			array[arrayIndex++] = e;
	}

	public final @NotNull org.pcollections.PMap<K, V> getMap() {
		if (isManaged()) {
			var txn = Transaction.getCurrentVerifyRead(this);
			if (txn == null)
				return map;
			//noinspection DataFlowIssue
			Log log = txn.getLog(parent().objectId() + variableId());
			if (log == null)
				return map;
			@SuppressWarnings("unchecked")
			var mapLog = (LogMap1<K, V>)log;
			return mapLog.getValue();
		}
		return map;
	}

	@Override
	public final int size() {
		return getMap().size();
	}

	@Override
	public final boolean containsValue(@NotNull Object v) {
		return getMap().containsValue(v);
	}

	@Override
	public final boolean containsKey(@NotNull Object key) {
		return getMap().containsKey(key);
	}

	@Override
	public boolean isEmpty() {
		return getMap().isEmpty();
	}

	@Override
	public @NotNull Set<K> keySet() {
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
	public @NotNull java.util.Collection<V> values() {
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
	public @NotNull Set<Map.Entry<K, V>> entrySet() {
		return new AbstractSet<>() {
			@Override
			public Iterator<Entry<K, V>> iterator() {
				return new Iterator<>() {
					private final Iterator<Map.Entry<K, V>> it = getMap().entrySet().iterator();
					private Map.Entry<K, V> next;

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
	public @NotNull Iterator<Map.Entry<K, V>> iterator() {
		return entrySet().iterator();
	}

	@Override
	public int hashCode() {
		return getMap().hashCode();
	}

	@Override
	public boolean equals(@Nullable Object o) {
		return o instanceof PMap && getMap().equals(((PMap<?, ?>)o).getMap());
	}

	@Override
	public @NotNull String toString() {
		var sb = new StringBuilder();
		ByteBuffer.BuildString(sb, getMap());
		return sb.toString();
	}
}
