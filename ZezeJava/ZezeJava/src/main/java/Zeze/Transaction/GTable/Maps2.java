package Zeze.Transaction.GTable;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import javax.annotation.CheckForNull;
import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.collect.UnmodifiableListIterator;
import com.google.errorprone.annotations.concurrent.LazyInit;
import com.google.j2objc.annotations.Weak;
import org.checkerframework.checker.nullness.qual.Nullable;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Predicates.compose;

public class Maps2 {
	static boolean safeRemove(Collection<?> collection, @Nullable Object object) {
		Preconditions.checkNotNull(collection);

		try {
			return collection.remove(object);
		} catch (NullPointerException | ClassCastException var3) {
			return false;
		}
	}

	@CheckForNull
	static <V extends @Nullable Object> V safeRemove(Map<?, V> map, @CheckForNull Object key) {
		checkNotNull(map);
		try {
			return map.remove(key);
		} catch (ClassCastException | NullPointerException e) {
			return null;
		}
	}


	static void clear(Iterator<?> iterator) {
		checkNotNull(iterator);
		while (iterator.hasNext()) {
			iterator.next();
			iterator.remove();
		}
	}

	static boolean equalsImpl(Table<?, ?, ?> table, @CheckForNull Object obj) {
		if (obj == table) {
			return true;
		} else if (obj instanceof Table) {
			Table<?, ?, ?> that = (Table<?, ?, ?>) obj;
			return table.cellSet().equals(that.cellSet());
		} else {
			return false;
		}
	}

	static boolean safeContainsKey(Map<?, ?> map, Object key) {
		Preconditions.checkNotNull(map);

		try {
			return map.containsKey(key);
		} catch (NullPointerException | ClassCastException var3) {
			return false;
		}
	}

	static <V> V safeGet(Map<?, V> map, @Nullable Object key) {
		Preconditions.checkNotNull(map);

		try {
			return map.get(key);
		} catch (NullPointerException | ClassCastException var3) {
			return null;
		}
	}

	static <K extends @Nullable Object, V extends @Nullable Object>
	Iterator<Map.Entry<K, V>> asMapEntryIterator(Set<K> set, final Function<? super K, V> function) {
		return new TransformedIterator<K, Map.Entry<K, V>>(set.iterator()) {
			@Override
			Map.Entry<K, V> transform(final K key) {
				return Maps.immutableEntry(key, function.apply(key));
			}
		};
	}

	static boolean safeContains(Collection<?> collection, @CheckForNull Object object) {
		checkNotNull(collection);
		try {
			return collection.contains(object);
		} catch (ClassCastException | NullPointerException e) {
			return false;
		}
	}

	static <T extends @Nullable Object> UnmodifiableIterator<T> emptyIterator() {
		return emptyListIterator();
	}

	@SuppressWarnings("unchecked")
	static <T extends @Nullable Object> UnmodifiableListIterator<T> emptyListIterator() {
		return (UnmodifiableListIterator<T>) ArrayItr.EMPTY;
	}

	private static final class ArrayItr<T extends @Nullable Object>
			extends AbstractIndexedListIterator<T> {
		static final UnmodifiableListIterator<Object> EMPTY = new ArrayItr<>(new Object[0], 0, 0, 0);

		private final T[] array;
		private final int offset;

		ArrayItr(T[] array, int offset, int length, int index) {
			super(length, index);
			this.array = array;
			this.offset = offset;
		}

		@Override
		protected T get(int index) {
			return array[offset + index];
		}
	}

	@GwtCompatible
	abstract static class ViewCachingAbstractMap<
			K extends @Nullable Object, V extends @Nullable Object>
			extends AbstractMap<K, V> {
		/**
		 * Creates the entry set to be returned by {@link #entrySet()}. This method is invoked at most
		 * once on a given map, at the time when {@code entrySet} is first called.
		 */
		abstract Set<Entry<K, V>> createEntrySet();

		@LazyInit
		@CheckForNull private transient Set<Entry<K, V>> entrySet;

		@Override
		public Set<Entry<K, V>> entrySet() {
			Set<Entry<K, V>> result = entrySet;
			return (result == null) ? entrySet = createEntrySet() : result;
		}

		@LazyInit @CheckForNull private transient Set<K> keySet;

		@Override
		public Set<K> keySet() {
			Set<K> result = keySet;
			return (result == null) ? keySet = createKeySet() : result;
		}

		Set<K> createKeySet() {
			return new KeySet<>(this);
		}

		@LazyInit @CheckForNull private transient Collection<V> values;

		@Override
		public Collection<V> values() {
			Collection<V> result = values;
			return (result == null) ? values = createValues() : result;
		}

		Collection<V> createValues() {
			return new Values<>(this);
		}
	}

	static boolean removeAllImpl(Set<?> set, Collection<?> collection) {
		checkNotNull(collection); // for GWT
		if (collection instanceof Multiset) {
			collection = ((Multiset<?>) collection).elementSet();
		}
		/*
		 * AbstractSet.removeAll(List) has quadratic behavior if the list size
		 * is just more than the set's size.  We augment the test by
		 * assuming that sets have fast contains() performance, and other
		 * collections don't.  See
		 * http://code.google.com/p/guava-libraries/issues/detail?id=1013
		 */
		if (collection instanceof Set && collection.size() > set.size()) {
			return Iterators.removeAll(set.iterator(), collection);
		} else {
			return removeAllImpl(set, collection.iterator());
		}
	}

	static boolean removeAllImpl(Set<?> set, Iterator<?> iterator) {
		boolean changed = false;
		while (iterator.hasNext()) {
			changed |= set.remove(iterator.next());
		}
		return changed;
	}

	@SuppressWarnings("unchecked")
	static <T extends @Nullable Object> Iterator<T> emptyModifiableIterator() {
		return (Iterator<T>) EmptyModifiableIterator.INSTANCE;
	}

	static <V extends @Nullable Object> Predicate<Map.Entry<?, V>> valuePredicateOnEntries(
			Predicate<? super V> valuePredicate) {
		return compose(valuePredicate, Maps2.<V>valueFunction());
	}
	@SuppressWarnings("unchecked")
	static <V extends @Nullable Object> Function<Map.Entry<?, V>, V> valueFunction() {
		return (Function) Maps2.EntryFunction.VALUE;
	}
	private enum EmptyModifiableIterator implements Iterator<Object> {
		INSTANCE;

		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public Object next() {
			throw new NoSuchElementException();
		}

		@Override
		public void remove() {
			checkRemove(false);
		}
	}
	static void checkRemove(boolean canRemove) {
		checkState(canRemove, "no calls to next() since the last call to remove()");
	}
	abstract static class ImprovedAbstractSet<E extends @Nullable Object> extends AbstractSet<E> {
		@Override
		public boolean removeAll(Collection<?> c) {
			return removeAllImpl(this, c);
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			return super.retainAll(checkNotNull(c)); // GWT compatibility
		}
	}

	static <K extends @Nullable Object, V extends @Nullable Object> Iterator<K> keyIterator(
			Iterator<Map.Entry<K, V>> entryIterator) {
		return new TransformedIterator<Map.Entry<K, V>, K>(entryIterator) {
			@Override
			K transform(Map.Entry<K, V> entry) {
				return entry.getKey();
			}
		};
	}

	static class KeySet<K extends @Nullable Object, V extends @Nullable Object>
			extends ImprovedAbstractSet<K> {
		@Weak final Map<K, V> map;

		KeySet(Map<K, V> map) {
			this.map = checkNotNull(map);
		}

		Map<K, V> map() {
			return map;
		}

		@Override
		public Iterator<K> iterator() {
			return keyIterator(map().entrySet().iterator());
		}

		@Override
		public void forEach(Consumer<? super K> action) {
			checkNotNull(action);
			// avoids entry allocation for those maps that allocate entries on iteration
			map.forEach((k, v) -> action.accept(k));
		}

		@Override
		public int size() {
			return map().size();
		}

		@Override
		public boolean isEmpty() {
			return map().isEmpty();
		}

		@Override
		public boolean contains(@CheckForNull Object o) {
			return map().containsKey(o);
		}

		@Override
		public boolean remove(@CheckForNull Object o) {
			if (contains(o)) {
				map().remove(o);
				return true;
			}
			return false;
		}

		@Override
		public void clear() {
			map().clear();
		}
	}

	static <K extends @Nullable Object, V extends @Nullable Object> Iterator<V> valueIterator(
			Iterator<Map.Entry<K, V>> entryIterator) {
		return new TransformedIterator<Map.Entry<K, V>, V>(entryIterator) {
			@Override
			V transform(Map.Entry<K, V> entry) {
				return entry.getValue();
			}
		};
	}

	static class Values<K extends @Nullable Object, V extends @Nullable Object>
			extends AbstractCollection<V> {
		@Weak final Map<K, V> map;

		Values(Map<K, V> map) {
			this.map = checkNotNull(map);
		}

		final Map<K, V> map() {
			return map;
		}

		@Override
		public Iterator<V> iterator() {
			return valueIterator(map().entrySet().iterator());
		}

		@Override
		public void forEach(Consumer<? super V> action) {
			checkNotNull(action);
			// avoids allocation of entries for those maps that generate fresh entries on iteration
			map.forEach((k, v) -> action.accept(v));
		}

		@Override
		public boolean remove(@CheckForNull Object o) {
			try {
				return super.remove(o);
			} catch (UnsupportedOperationException e) {
				for (Map.Entry<K, V> entry : map().entrySet()) {
					if (Objects.equal(o, entry.getValue())) {
						map().remove(entry.getKey());
						return true;
					}
				}
				return false;
			}
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			try {
				return super.removeAll(checkNotNull(c));
			} catch (UnsupportedOperationException e) {
				Set<K> toRemove = Sets.newHashSet();
				for (Map.Entry<K, V> entry : map().entrySet()) {
					if (c.contains(entry.getValue())) {
						toRemove.add(entry.getKey());
					}
				}
				return map().keySet().removeAll(toRemove);
			}
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			try {
				return super.retainAll(checkNotNull(c));
			} catch (UnsupportedOperationException e) {
				Set<K> toRetain = Sets.newHashSet();
				for (Map.Entry<K, V> entry : map().entrySet()) {
					if (c.contains(entry.getValue())) {
						toRetain.add(entry.getKey());
					}
				}
				return map().keySet().retainAll(toRetain);
			}
		}

		@Override
		public int size() {
			return map().size();
		}

		@Override
		public boolean isEmpty() {
			return map().isEmpty();
		}

		@Override
		public boolean contains(@CheckForNull Object o) {
			return map().containsValue(o);
		}

		@Override
		public void clear() {
			map().clear();
		}
	}

	abstract static class EntrySet<K extends @Nullable Object, V extends @Nullable Object>
			extends ImprovedAbstractSet<Map.Entry<K, V>> {
		abstract Map<K, V> map();

		@Override
		public int size() {
			return map().size();
		}

		@Override
		public void clear() {
			map().clear();
		}

		@Override
		public boolean contains(@CheckForNull Object o) {
			if (o instanceof Map.Entry) {
				Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
				Object key = entry.getKey();
				V value = Maps2.safeGet(map(), key);
				return Objects.equal(value, entry.getValue()) && (value != null || map().containsKey(key));
			}
			return false;
		}

		@Override
		public boolean isEmpty() {
			return map().isEmpty();
		}

		@Override
		public boolean remove(@CheckForNull Object o) {
			/*
			 * `o instanceof Entry` is guaranteed by `contains`, but we check it here to satisfy our
			 * nullness checker.
			 */
			if (contains(o) && o instanceof Map.Entry) {
				Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
				return map().keySet().remove(entry.getKey());
			}
			return false;
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			try {
				return super.removeAll(checkNotNull(c));
			} catch (UnsupportedOperationException e) {
				// if the iterators don't support remove
				return removeAllImpl(this, c.iterator());
			}
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			try {
				return super.retainAll(checkNotNull(c));
			} catch (UnsupportedOperationException e) {
				// if the iterators don't support remove
				Set<@Nullable Object> keys = Sets.newHashSetWithExpectedSize(c.size());
				for (Object o : c) {
					/*
					 * `o instanceof Entry` is guaranteed by `contains`, but we check it here to satisfy our
					 * nullness checker.
					 */
					if (contains(o) && o instanceof Map.Entry) {
						Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
						keys.add(entry.getKey());
					}
				}
				return map().keySet().retainAll(keys);
			}
		}
	}

	abstract static class IteratorBasedAbstractMap<
			K extends @Nullable Object, V extends @Nullable Object>
			extends AbstractMap<K, V> {
		@Override
		public abstract int size();

		abstract Iterator<Entry<K, V>> entryIterator();

		Spliterator<Entry<K, V>> entrySpliterator() {
			return Spliterators.spliterator(
					entryIterator(), size(), Spliterator.SIZED | Spliterator.DISTINCT);
		}

		@Override
		public Set<Entry<K, V>> entrySet() {
			return new Maps2.EntrySet<K, V>() {
				@Override
				Map<K, V> map() {
					return Maps2.IteratorBasedAbstractMap.this;
				}

				@Override
				public Iterator<Entry<K, V>> iterator() {
					return entryIterator();
				}

				@Override
				public Spliterator<Entry<K, V>> spliterator() {
					return entrySpliterator();
				}

				@Override
				public void forEach(Consumer<? super Entry<K, V>> action) {
					forEachEntry(action);
				}
			};
		}

		void forEachEntry(Consumer<? super Entry<K, V>> action) {
			entryIterator().forEachRemaining(action);
		}

		@Override
		public void clear() {
			Maps2.clear(entryIterator());
		}
	}

	@SuppressWarnings("unchecked")
	static <K extends @Nullable Object> Function<Map.Entry<K, ?>, K> keyFunction() {
		return (Function) EntryFunction.KEY;
	}
	private enum EntryFunction implements Function<Map.Entry<?, ?>, @Nullable Object> {
		KEY {
			@Override
			@CheckForNull
			public Object apply(Map.Entry<?, ?> entry) {
				return entry.getKey();
			}
		},
		VALUE {
			@Override
			@CheckForNull
			public Object apply(Map.Entry<?, ?> entry) {
				return entry.getValue();
			}
		};
	}
	static <K extends @Nullable Object> Predicate<Map.Entry<K, ?>> keyPredicateOnEntries(
			Predicate<? super K> keyPredicate) {
		return compose(keyPredicate, Maps2.<K>keyFunction());
	}

	abstract static class AbstractCell<
			R extends @Nullable Object, C extends @Nullable Object, V extends @Nullable Object>
			implements Table.Cell<R, C, V> {
		// needed for serialization
		AbstractCell() {}

		@Override
		public boolean equals(@CheckForNull Object obj) {
			if (obj == this) {
				return true;
			}
			if (obj instanceof com.google.common.collect.Table.Cell) {
				com.google.common.collect.Table.Cell<?, ?, ?> other = (com.google.common.collect.Table.Cell<?, ?, ?>) obj;
				return Objects.equal(getRowKey(), other.getRowKey())
						&& Objects.equal(getColumnKey(), other.getColumnKey())
						&& Objects.equal(getValue(), other.getValue());
			}
			return false;
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(getRowKey(), getColumnKey(), getValue());
		}

		@Override
		public String toString() {
			return "(" + getRowKey() + "," + getColumnKey() + ")=" + getValue();
		}
	}

	/**
	 * Returns an immutable cell with the specified row key, column key, and value.
	 *
	 * <p>The returned cell is serializable.
	 *
	 * @param rowKey the row key to be associated with the returned cell
	 * @param columnKey the column key to be associated with the returned cell
	 * @param value the value to be associated with the returned cell
	 */
	public static <R extends @Nullable Object, C extends @Nullable Object, V extends @Nullable Object>
	Table.Cell<R, C, V> immutableCell(
			R rowKey,
			C columnKey,
			V value) {
		return new ImmutableCell<>(rowKey, columnKey, value);
	}

	static final class ImmutableCell<
			R extends @Nullable Object, C extends @Nullable Object, V extends @Nullable Object>
			extends AbstractCell<R, C, V> implements Serializable {
		private final R rowKey;
		private final C columnKey;
		private final V value;

		ImmutableCell(
				R rowKey,
				C columnKey,
				V value) {
			this.rowKey = rowKey;
			this.columnKey = columnKey;
			this.value = value;
		}

		@Override
		public R getRowKey() {
			return rowKey;
		}

		@Override
		public C getColumnKey() {
			return columnKey;
		}

		@Override
		public V getValue() {
			return value;
		}

		private static final long serialVersionUID = 0;
	}
}
