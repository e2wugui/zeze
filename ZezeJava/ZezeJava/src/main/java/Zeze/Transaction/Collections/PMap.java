package Zeze.Transaction.Collections;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Log;
import Zeze.Transaction.Transaction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.pcollections.Empty;

/** 事务 Map 基类：基于 pcollections 持久化映射的 Map 视图，托管下的修改经 LogMap 记账。 */
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
			public @NotNull Iterator<K> iterator() {
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
			public @NotNull Iterator<V> iterator() {
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
			public @NotNull Iterator<Entry<K, V>> iterator() {
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
						if (next == null)
							throw new IllegalStateException("iterator remove() before next()");
						// 身份fail-fast（对齐PList判例）：next()返回的条目在迭代期间被外部结构性
						// 修改（同键移除/重put成不同值）时，按键删除会删错条目或静默no-op，
						// 不相等（含键已不存在）即响亮失败。
						var key = next.getKey();
						if (getMap().get(key) != next.getValue())
							throw new ConcurrentModificationException("structural modification during iteration");
						PMap.this.remove(key);
						next = null;
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
		// 按Map接口内容比较（对称性，理由同PList）：getMap()的equals按Map契约。
		return o instanceof java.util.Map && getMap().equals(o);
	}

	@Override
	public @NotNull String toString() {
		var sb = new StringBuilder();
		ByteBuffer.BuildString(sb, getMap());
		return sb.toString();
	}
}
