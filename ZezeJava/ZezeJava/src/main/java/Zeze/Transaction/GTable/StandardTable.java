package Zeze.Transaction.GTable;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ForwardingMapEntry;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.locks.ReentrantLock;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@GwtCompatible
class StandardTable<R, C, V> extends AbstractTable<R, C, V> implements Serializable {

	ReentrantLock lock = new ReentrantLock();
	Map<R, Map<C, V>> backingMap;
	Supplier<? extends Map<C, V>> factory;
	private transient @MonotonicNonNull Set<C> columnKeySet;
	private transient @MonotonicNonNull Map<R, Map<C, V>> rowMap;
	private transient StandardTable<R, C, V>.@MonotonicNonNull ColumnMap columnMap;
	private static final long serialVersionUID = 0L;

	/*
	StandardTable(Map<R, Map<C, V>> backingMap, Supplier<? extends Map<C, V>> factory) {
		this.backingMap = backingMap;
		this.factory = factory;
	}
	*/

	public boolean contains(@Nullable Object rowKey, @Nullable Object columnKey) {
		return rowKey != null && columnKey != null && super.contains(rowKey, columnKey);
	}

	public boolean containsColumn(@Nullable Object columnKey) {
		if (columnKey == null) {
			return false;
		} else {
			Iterator var2 = this.backingMap.values().iterator();

			Map map;
			do {
				if (!var2.hasNext()) {
					return false;
				}

				map = (Map)var2.next();
			} while(!Maps2.safeContainsKey(map, columnKey));

			return true;
		}
	}

	public boolean containsRow(@Nullable Object rowKey) {
		return rowKey != null && Maps2.safeContainsKey(this.backingMap, rowKey);
	}

	public boolean containsValue(@Nullable Object value) {
		return value != null && super.containsValue(value);
	}

	public V get(@Nullable Object rowKey, @Nullable Object columnKey) {
		return rowKey != null && columnKey != null ? super.get(rowKey, columnKey) : null;
	}

	public boolean isEmpty() {
		return this.backingMap.isEmpty();
	}

	public int size() {
		int size = 0;

		Map map;
		for(Iterator var2 = this.backingMap.values().iterator(); var2.hasNext(); size += map.size()) {
			map = (Map)var2.next();
		}

		return size;
	}

	public void clear() {
		this.backingMap.clear();
	}

	private Map<C, V> getOrCreate(R rowKey) {
		Map<C, V> map = (Map)this.backingMap.get(rowKey);
		if (map == null) {
			map = (Map)this.factory.get();
			this.backingMap.put(rowKey, map);
		}

		return map;
	}

	@CanIgnoreReturnValue
	public V put(R rowKey, C columnKey, V value) {
		Preconditions.checkNotNull(rowKey);
		Preconditions.checkNotNull(columnKey);
		Preconditions.checkNotNull(value);
		return this.getOrCreate(rowKey).put(columnKey, value);
	}

	@CanIgnoreReturnValue
	public V remove(@Nullable Object rowKey, @Nullable Object columnKey) {
		if (rowKey != null && columnKey != null) {
			Map<C, V> map = (Map)Maps2.safeGet(this.backingMap, rowKey);
			if (map == null) {
				return null;
			} else {
				V value = map.remove(columnKey);
				if (map.isEmpty()) {
					this.backingMap.remove(rowKey);
				}

				return value;
			}
		} else {
			return null;
		}
	}

	@CanIgnoreReturnValue
	private Map<R, V> removeColumn(Object column) {
		Map<R, V> output = new LinkedHashMap();
		Iterator<Map.Entry<R, Map<C, V>>> iterator = this.backingMap.entrySet().iterator();

		while(iterator.hasNext()) {
			Map.Entry<R, Map<C, V>> entry = (Map.Entry)iterator.next();
			V value = entry.getValue().remove(column);
			if (value != null) {
				output.put(entry.getKey(), value);
				if (((Map)entry.getValue()).isEmpty()) {
					iterator.remove();
				}
			}
		}

		return output;
	}

	private boolean containsMapping(Object rowKey, Object columnKey, Object value) {
		return value != null && value.equals(this.get(rowKey, columnKey));
	}

	private boolean removeMapping(Object rowKey, Object columnKey, Object value) {
		if (this.containsMapping(rowKey, columnKey, value)) {
			this.remove(rowKey, columnKey);
			return true;
		} else {
			return false;
		}
	}

	public Set<Table.Cell<R, C, V>> cellSet() {
		return super.cellSet();
	}

	Iterator<Table.Cell<R, C, V>> cellIterator() {
		return new StandardTable.CellIterator();
	}

	Spliterator<Table.Cell<R, C, V>> cellSpliterator() {
		return CollectSpliterators2.flatMap(this.backingMap.entrySet().spliterator(), (rowEntry) -> {
			return CollectSpliterators2.map((rowEntry.getValue()).entrySet().spliterator(), (columnEntry) -> {
				return Tables.immutableCell(rowEntry.getKey(), columnEntry.getKey(), columnEntry.getValue());
			});
		}, 65, (long)this.size());
	}

	public Map<C, V> row(R rowKey) {
		return new StandardTable.Row(rowKey);
	}

	public Map<R, V> column(C columnKey) {
		return new StandardTable.Column(columnKey);
	}

	public Set<R> rowKeySet() {
		return this.rowMap().keySet();
	}

	public Set<C> columnKeySet() {
		lock.lock();
		try {
			Set<C> result = this.columnKeySet;
			return result == null ? (this.columnKeySet = new StandardTable.ColumnKeySet()) : result;
		} finally {
			lock.unlock();
		}
	}

	Iterator<C> createColumnKeyIterator() {
		return new StandardTable.ColumnKeyIterator();
	}

	public Collection<V> values() {
		return super.values();
	}

	public Map<R, Map<C, V>> rowMap() {
		lock.lock();
		try {
			Map<R, Map<C, V>> result = this.rowMap;
			return result == null ? (this.rowMap = this.createRowMap()) : result;
		} finally {
			lock.unlock();
		}
	}

	Map<R, Map<C, V>> createRowMap() {
		return new StandardTable.RowMap();
	}

	public Map<C, Map<R, V>> columnMap() {
		lock.lock();
		try {
			StandardTable<R, C, V>.ColumnMap result = this.columnMap;
			return result == null ? (this.columnMap = new StandardTable.ColumnMap()) : result;
		} finally {
			lock.unlock();
		}
	}

	private class ColumnMap extends Maps2.ViewCachingAbstractMap<C, Map<R, V>> {
		private ColumnMap() {
		}

		public Map<R, V> get(Object key) {
			return StandardTable.this.containsColumn(key) ? StandardTable.this.column((C)key) : null;
		}

		public boolean containsKey(Object key) {
			return StandardTable.this.containsColumn(key);
		}

		public Map<R, V> remove(Object key) {
			return StandardTable.this.containsColumn(key) ? StandardTable.this.removeColumn(key) : null;
		}

		public Set<Map.Entry<C, Map<R, V>>> createEntrySet() {
			return new StandardTable.ColumnMap.ColumnMapEntrySet();
		}

		public Set<C> keySet() {
			return StandardTable.this.columnKeySet();
		}

		Collection<Map<R, V>> createValues() {
			return new StandardTable.ColumnMap.ColumnMapValues();
		}

		private class ColumnMapValues extends Maps2.Values<C, Map<R, V>> {
			ColumnMapValues() {
				super(StandardTable.ColumnMap.this);
			}

			public boolean remove(Object obj) {
				Iterator var2 = StandardTable.ColumnMap.this.entrySet().iterator();

				Map.Entry entry;
				do {
					if (!var2.hasNext()) {
						return false;
					}

					entry = (Map.Entry)var2.next();
				} while(!((Map)entry.getValue()).equals(obj));

				StandardTable.this.removeColumn(entry.getKey());
				return true;
			}

			public boolean removeAll(Collection<?> c) {
				Preconditions.checkNotNull(c);
				boolean changed = false;
				Iterator var3 = Lists.newArrayList(StandardTable.this.columnKeySet().iterator()).iterator();

				while(var3.hasNext()) {
					C columnKey = (C)var3.next();
					if (c.contains(StandardTable.this.column(columnKey))) {
						StandardTable.this.removeColumn(columnKey);
						changed = true;
					}
				}

				return changed;
			}

			public boolean retainAll(Collection<?> c) {
				Preconditions.checkNotNull(c);
				boolean changed = false;
				Iterator var3 = Lists.newArrayList(StandardTable.this.columnKeySet().iterator()).iterator();

				while(var3.hasNext()) {
					C columnKey = (C)var3.next();
					if (!c.contains(StandardTable.this.column(columnKey))) {
						StandardTable.this.removeColumn(columnKey);
						changed = true;
					}
				}

				return changed;
			}
		}

		class ColumnMapEntrySet extends StandardTable<R, C, V>.TableSet<Map.Entry<C, Map<R, V>>> {
			ColumnMapEntrySet() {
				super();
			}

			public Iterator<Map.Entry<C, Map<R, V>>> iterator() {
				return Maps2.asMapEntryIterator(StandardTable.this.columnKeySet(), new Function<C, Map<R, V>>() {
					public Map<R, V> apply(C columnKey) {
						return StandardTable.this.column(columnKey);
					}
				});
			}

			public int size() {
				return StandardTable.this.columnKeySet().size();
			}

			public boolean contains(Object obj) {
				if (obj instanceof Map.Entry) {
					Map.Entry<?, ?> entry = (Map.Entry)obj;
					if (StandardTable.this.containsColumn(entry.getKey())) {
						C columnKey = (C)entry.getKey();
						return StandardTable.ColumnMap.this.get(columnKey).equals(entry.getValue());
					}
				}

				return false;
			}

			public boolean remove(Object obj) {
				if (this.contains(obj)) {
					Map.Entry<?, ?> entry = (Map.Entry)obj;
					StandardTable.this.removeColumn(entry.getKey());
					return true;
				} else {
					return false;
				}
			}

			public boolean removeAll(Collection<?> c) {
				Preconditions.checkNotNull(c);
				return Maps2.removeAllImpl(this, c.iterator());
			}

			public boolean retainAll(Collection<?> c) {
				Preconditions.checkNotNull(c);
				boolean changed = false;
				Iterator var3 = Lists.newArrayList(StandardTable.this.columnKeySet().iterator()).iterator();

				while(var3.hasNext()) {
					var columnKey = var3.next();
					if (!c.contains(Maps.immutableEntry(columnKey, StandardTable.this.column((C)columnKey)))) {
						StandardTable.this.removeColumn(columnKey);
						changed = true;
					}
				}

				return changed;
			}
		}
	}

	class RowMap extends Maps2.ViewCachingAbstractMap<R, Map<C, V>> {
		RowMap() {
		}

		public boolean containsKey(Object key) {
			return StandardTable.this.containsRow(key);
		}

		public Map<C, V> get(Object key) {
			return StandardTable.this.containsRow(key) ? StandardTable.this.row((R)key) : null;
		}

		public Map<C, V> remove(Object key) {
			return key == null ? null : (Map)StandardTable.this.backingMap.remove(key);
		}

		protected Set<Map.Entry<R, Map<C, V>>> createEntrySet() {
			return new StandardTable.RowMap.EntrySet();
		}

		class EntrySet extends StandardTable<R, C, V>.TableSet<Map.Entry<R, Map<C, V>>> {
			EntrySet() {
				super();
			}

			public Iterator<Map.Entry<R, Map<C, V>>> iterator() {
				return Maps2.asMapEntryIterator(StandardTable.this.backingMap.keySet(), new Function<R, Map<C, V>>() {
					public Map<C, V> apply(R rowKey) {
						return StandardTable.this.row(rowKey);
					}
				});
			}

			public int size() {
				return StandardTable.this.backingMap.size();
			}

			public boolean contains(Object obj) {
				if (!(obj instanceof Map.Entry)) {
					return false;
				} else {
					Map.Entry<?, ?> entry = (Map.Entry)obj;
					return entry.getKey() != null && entry.getValue() instanceof Map && Maps2.safeContains(StandardTable.this.backingMap.entrySet(), entry);
				}
			}

			public boolean remove(Object obj) {
				if (!(obj instanceof Map.Entry)) {
					return false;
				} else {
					Map.Entry<?, ?> entry = (Map.Entry)obj;
					return entry.getKey() != null && entry.getValue() instanceof Map && StandardTable.this.backingMap.entrySet().remove(entry);
				}
			}
		}
	}

	private class ColumnKeyIterator extends AbstractIterator<C> {
		final Map<C, V> seen;
		final Iterator<Map<C, V>> mapIterator;
		Iterator<Map.Entry<C, V>> entryIterator;

		private ColumnKeyIterator() {
			this.seen = (Map)StandardTable.this.factory.get();
			this.mapIterator = StandardTable.this.backingMap.values().iterator();
			this.entryIterator = Maps2.emptyIterator();
		}

		protected C computeNext() {
			while(true) {
				if (this.entryIterator.hasNext()) {
					Map.Entry<C, V> entry = (Map.Entry)this.entryIterator.next();
					if (!this.seen.containsKey(entry.getKey())) {
						this.seen.put(entry.getKey(), entry.getValue());
						return entry.getKey();
					}
				} else {
					if (!this.mapIterator.hasNext()) {
						return this.endOfData();
					}

					this.entryIterator = ((Map)this.mapIterator.next()).entrySet().iterator();
				}
			}
		}
	}

	private class ColumnKeySet extends StandardTable<R, C, V>.TableSet<C> {
		private ColumnKeySet() {
			super();
		}

		public Iterator<C> iterator() {
			return StandardTable.this.createColumnKeyIterator();
		}

		public int size() {
			return Iterators.size(this.iterator());
		}

		public boolean remove(Object obj) {
			if (obj == null) {
				return false;
			} else {
				boolean changed = false;
				Iterator<Map<C, V>> iterator = StandardTable.this.backingMap.values().iterator();

				while(iterator.hasNext()) {
					Map<C, V> map = (Map)iterator.next();
					if (map.keySet().remove(obj)) {
						changed = true;
						if (map.isEmpty()) {
							iterator.remove();
						}
					}
				}

				return changed;
			}
		}

		public boolean removeAll(Collection<?> c) {
			Preconditions.checkNotNull(c);
			boolean changed = false;
			Iterator<Map<C, V>> iterator = StandardTable.this.backingMap.values().iterator();

			while(iterator.hasNext()) {
				Map<C, V> map = (Map)iterator.next();
				if (Iterators.removeAll(map.keySet().iterator(), c)) {
					changed = true;
					if (map.isEmpty()) {
						iterator.remove();
					}
				}
			}

			return changed;
		}

		public boolean retainAll(Collection<?> c) {
			Preconditions.checkNotNull(c);
			boolean changed = false;
			Iterator<Map<C, V>> iterator = StandardTable.this.backingMap.values().iterator();

			while(iterator.hasNext()) {
				Map<C, V> map = (Map)iterator.next();
				if (map.keySet().retainAll(c)) {
					changed = true;
					if (map.isEmpty()) {
						iterator.remove();
					}
				}
			}

			return changed;
		}

		public boolean contains(Object obj) {
			return StandardTable.this.containsColumn(obj);
		}
	}

	private class Column extends Maps2.ViewCachingAbstractMap<R, V> {
		final C columnKey;

		Column(C columnKey) {
			this.columnKey = Preconditions.checkNotNull(columnKey);
		}

		public V put(R key, V value) {
			return StandardTable.this.put(key, this.columnKey, value);
		}

		public V get(Object key) {
			return StandardTable.this.get(key, this.columnKey);
		}

		public boolean containsKey(Object key) {
			return StandardTable.this.contains(key, this.columnKey);
		}

		public V remove(Object key) {
			return StandardTable.this.remove(key, this.columnKey);
		}

		@CanIgnoreReturnValue
		boolean removeFromColumnIf(Predicate<? super Map.Entry<R, V>> predicate) {
			boolean changed = false;
			Iterator<Map.Entry<R, Map<C, V>>> iterator = StandardTable.this.backingMap.entrySet().iterator();

			while(iterator.hasNext()) {
				Map.Entry<R, Map<C, V>> entry = (Map.Entry)iterator.next();
				Map<C, V> map = (Map)entry.getValue();
				V value = map.get(this.columnKey);
				if (value != null && predicate.apply(Maps.immutableEntry(entry.getKey(), value))) {
					map.remove(this.columnKey);
					changed = true;
					if (map.isEmpty()) {
						iterator.remove();
					}
				}
			}

			return changed;
		}

		Set<Map.Entry<R, V>> createEntrySet() {
			return new StandardTable.Column.EntrySet();
		}

		Set<R> createKeySet() {
			return new StandardTable.Column.KeySet();
		}

		Collection<V> createValues() {
			return new StandardTable.Column.Values();
		}

		private class Values extends Maps2.Values<R, V> {
			Values() {
				super(StandardTable.Column.this);
			}

			public boolean remove(Object obj) {
				return obj != null && StandardTable.Column.this.removeFromColumnIf(Maps2.valuePredicateOnEntries(Predicates.equalTo(obj)));
			}

			public boolean removeAll(Collection<?> c) {
				return StandardTable.Column.this.removeFromColumnIf(Maps2.valuePredicateOnEntries(Predicates.in(c)));
			}

			public boolean retainAll(Collection<?> c) {
				return StandardTable.Column.this.removeFromColumnIf(Maps2.valuePredicateOnEntries(Predicates.not(Predicates.in(c))));
			}
		}

		private class KeySet extends Maps2.KeySet<R, V> {
			KeySet() {
				super(StandardTable.Column.this);
			}

			public boolean contains(Object obj) {
				return StandardTable.this.contains(obj, StandardTable.Column.this.columnKey);
			}

			public boolean remove(Object obj) {
				return StandardTable.this.remove(obj, StandardTable.Column.this.columnKey) != null;
			}

			public boolean retainAll(Collection<?> c) {
				return StandardTable.Column.this.removeFromColumnIf(Maps2.keyPredicateOnEntries(Predicates.not(Predicates.in(c))));
			}
		}

		private class EntrySetIterator extends AbstractIterator<Map.Entry<R, V>> {
			final Iterator<Map.Entry<R, Map<C, V>>> iterator;

			private EntrySetIterator() {
				this.iterator = StandardTable.this.backingMap.entrySet().iterator();
			}

			protected Map.Entry<R, V> computeNext() {
				while(true) {
					if (this.iterator.hasNext()) {
						final Map.Entry<R, Map<C, V>> entry = (Map.Entry)this.iterator.next();
						if (!((Map)entry.getValue()).containsKey(StandardTable.Column.this.columnKey)) {
							continue;
						}

						class EntryImpl extends AbstractMapEntry<R, V> {
							EntryImpl() {
							}

							public R getKey() {
								return entry.getKey();
							}

							public V getValue() {
								return (entry.getValue()).get(StandardTable.Column.this.columnKey);
							}

							public V setValue(V value) {
								return (entry.getValue()).put(StandardTable.Column.this.columnKey, Preconditions.checkNotNull(value));
							}
						}

						return new EntryImpl();
					}

					return (Map.Entry)this.endOfData();
				}
			}
		}

		private class EntrySet extends Maps2.ImprovedAbstractSet<Map.Entry<R, V>> {
			private EntrySet() {
			}

			public Iterator<Map.Entry<R, V>> iterator() {
				return StandardTable.Column.this.new EntrySetIterator();
			}

			public int size() {
				int size = 0;
				Iterator var2 = StandardTable.this.backingMap.values().iterator();

				while(var2.hasNext()) {
					Map<C, V> map = (Map)var2.next();
					if (map.containsKey(StandardTable.Column.this.columnKey)) {
						++size;
					}
				}

				return size;
			}

			public boolean isEmpty() {
				return !StandardTable.this.containsColumn(StandardTable.Column.this.columnKey);
			}

			public void clear() {
				StandardTable.Column.this.removeFromColumnIf(Predicates.alwaysTrue());
			}

			public boolean contains(Object o) {
				if (o instanceof Map.Entry) {
					Map.Entry<?, ?> entry = (Map.Entry)o;
					return StandardTable.this.containsMapping(entry.getKey(), StandardTable.Column.this.columnKey, entry.getValue());
				} else {
					return false;
				}
			}

			public boolean remove(Object obj) {
				if (obj instanceof Map.Entry) {
					Map.Entry<?, ?> entry = (Map.Entry)obj;
					return StandardTable.this.removeMapping(entry.getKey(), StandardTable.Column.this.columnKey, entry.getValue());
				} else {
					return false;
				}
			}

			public boolean retainAll(Collection<?> c) {
				return StandardTable.Column.this.removeFromColumnIf(Predicates.not(Predicates.in(c)));
			}
		}
	}

	class Row extends Maps2.IteratorBasedAbstractMap<C, V> {
		final R rowKey;
		@Nullable Map<C, V> backingRowMap;

		Row(R rowKey) {
			this.rowKey = Preconditions.checkNotNull(rowKey);
		}

		Map<C, V> backingRowMap() {
			return this.backingRowMap != null && (!this.backingRowMap.isEmpty() || !StandardTable.this.backingMap.containsKey(this.rowKey)) ? this.backingRowMap : (this.backingRowMap = this.computeBackingRowMap());
		}

		Map<C, V> computeBackingRowMap() {
			return (Map)StandardTable.this.backingMap.get(this.rowKey);
		}

		void maintainEmptyInvariant() {
			if (this.backingRowMap() != null && this.backingRowMap.isEmpty()) {
				StandardTable.this.backingMap.remove(this.rowKey);
				this.backingRowMap = null;
			}

		}

		public boolean containsKey(Object key) {
			Map<C, V> backingRowMap = this.backingRowMap();
			return key != null && backingRowMap != null && Maps2.safeContainsKey(backingRowMap, key);
		}

		public V get(Object key) {
			Map<C, V> backingRowMap = this.backingRowMap();
			return key != null && backingRowMap != null ? Maps2.safeGet(backingRowMap, key) : null;
		}

		public V put(C key, V value) {
			Preconditions.checkNotNull(key);
			Preconditions.checkNotNull(value);
			return this.backingRowMap != null && !this.backingRowMap.isEmpty() ? this.backingRowMap.put(key, value) : StandardTable.this.put(this.rowKey, key, value);
		}

		public V remove(Object key) {
			Map<C, V> backingRowMap = this.backingRowMap();
			if (backingRowMap == null) {
				return null;
			} else {
				V result = Maps2.safeRemove(backingRowMap, key);
				this.maintainEmptyInvariant();
				return result;
			}
		}

		public void clear() {
			Map<C, V> backingRowMap = this.backingRowMap();
			if (backingRowMap != null) {
				backingRowMap.clear();
			}

			this.maintainEmptyInvariant();
		}

		public int size() {
			Map<C, V> map = this.backingRowMap();
			return map == null ? 0 : map.size();
		}

		Iterator<Map.Entry<C, V>> entryIterator() {
			Map<C, V> map = this.backingRowMap();
			if (map == null) {
				return Maps2.emptyModifiableIterator();
			} else {
				final Iterator<Map.Entry<C, V>> iterator = map.entrySet().iterator();
				return new Iterator<Map.Entry<C, V>>() {
					public boolean hasNext() {
						return iterator.hasNext();
					}

					public Map.Entry<C, V> next() {
						return StandardTable.Row.this.wrapEntry((Map.Entry)iterator.next());
					}

					public void remove() {
						iterator.remove();
						StandardTable.Row.this.maintainEmptyInvariant();
					}
				};
			}
		}

		Spliterator<Map.Entry<C, V>> entrySpliterator() {
			Map<C, V> map = this.backingRowMap();
			return map == null ? Spliterators.emptySpliterator() : CollectSpliterators2.map(map.entrySet().spliterator(), this::wrapEntry);
		}

		Map.Entry<C, V> wrapEntry(final Map.Entry<C, V> entry) {
			return new ForwardingMapEntry<C, V>() {
				protected Map.Entry<C, V> delegate() {
					return entry;
				}

				public V setValue(V value) {
					return super.setValue(Preconditions.checkNotNull(value));
				}

				public boolean equals(Object object) {
					return this.standardEquals(object);
				}
			};
		}
	}

	private class CellIterator implements Iterator<Table.Cell<R, C, V>> {
		final Iterator<Map.Entry<R, Map<C, V>>> rowIterator;
		Map.@Nullable Entry<R, Map<C, V>> rowEntry;
		Iterator<Map.Entry<C, V>> columnIterator;

		private CellIterator() {
			this.rowIterator = StandardTable.this.backingMap.entrySet().iterator();
			this.columnIterator = Maps2.emptyModifiableIterator();
		}

		public boolean hasNext() {
			return this.rowIterator.hasNext() || this.columnIterator.hasNext();
		}

		public Table.Cell<R, C, V> next() {
			if (!this.columnIterator.hasNext()) {
				this.rowEntry = (Map.Entry)this.rowIterator.next();
				this.columnIterator = ((Map)this.rowEntry.getValue()).entrySet().iterator();
			}

			Map.Entry<C, V> columnEntry = (Map.Entry)this.columnIterator.next();
			return Tables.immutableCell(this.rowEntry.getKey(), columnEntry.getKey(), columnEntry.getValue());
		}

		public void remove() {
			this.columnIterator.remove();
			if (((Map)this.rowEntry.getValue()).isEmpty()) {
				this.rowIterator.remove();
				this.rowEntry = null;
			}

		}
	}

	private abstract class TableSet<T> extends Maps2.ImprovedAbstractSet<T> {
		private TableSet() {
		}

		public boolean isEmpty() {
			return StandardTable.this.backingMap.isEmpty();
		}

		public void clear() {
			StandardTable.this.backingMap.clear();
		}
	}
}
