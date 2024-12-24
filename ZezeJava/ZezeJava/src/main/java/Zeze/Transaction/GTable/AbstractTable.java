package Zeze.Transaction.GTable;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@GwtCompatible
abstract class AbstractTable<R, C, V> implements com.google.common.collect.Table<R, C, V> {
	private transient @MonotonicNonNull Set<Table.Cell<R, C, V>> cellSet;
	private transient @MonotonicNonNull Collection<V> values;

	AbstractTable() {
	}

	public boolean containsRow(@Nullable Object rowKey) {
		return Maps2.safeContainsKey(this.rowMap(), rowKey);
	}

	public boolean containsColumn(@Nullable Object columnKey) {
		return Maps2.safeContainsKey(this.columnMap(), columnKey);
	}

	public Set<R> rowKeySet() {
		return this.rowMap().keySet();
	}

	public Set<C> columnKeySet() {
		return this.columnMap().keySet();
	}

	public boolean containsValue(@Nullable Object value) {
		Iterator var2 = this.rowMap().values().iterator();

		Map row;
		do {
			if (!var2.hasNext()) {
				return false;
			}

			row = (Map)var2.next();
		} while(!row.containsValue(value));

		return true;
	}

	public boolean contains(@Nullable Object rowKey, @Nullable Object columnKey) {
		Map<C, V> row = (Map)Maps2.safeGet(this.rowMap(), rowKey);
		return row != null && Maps2.safeContainsKey(row, columnKey);
	}

	public V get(@Nullable Object rowKey, @Nullable Object columnKey) {
		Map<C, V> row = (Map)Maps2.safeGet(this.rowMap(), rowKey);
		return row == null ? null : Maps2.safeGet(row, columnKey);
	}

	public boolean isEmpty() {
		return this.size() == 0;
	}

	public void clear() {
		Maps2.clear(this.cellSet().iterator());
	}

	@CanIgnoreReturnValue
	public V remove(@Nullable Object rowKey, @Nullable Object columnKey) {
		Map<C, V> row = (Map)Maps2.safeGet(this.rowMap(), rowKey);
		return row == null ? null : Maps2.safeRemove(row, columnKey);
	}

	@CanIgnoreReturnValue
	public V put(R rowKey, C columnKey, V value) {
		return this.row(rowKey).put(columnKey, value);
	}

	public void putAll(Table<? extends R, ? extends C, ? extends V> table) {
		Iterator var2 = table.cellSet().iterator();

		while(var2.hasNext()) {
			Table.Cell<? extends R, ? extends C, ? extends V> cell = (Table.Cell)var2.next();
			this.put(cell.getRowKey(), cell.getColumnKey(), cell.getValue());
		}

	}

	public Set<Table.Cell<R, C, V>> cellSet() {
		Set<Table.Cell<R, C, V>> result = this.cellSet;
		return result == null ? (this.cellSet = this.createCellSet()) : result;
	}

	Set<Table.Cell<R, C, V>> createCellSet() {
		return new AbstractTable.CellSet();
	}

	abstract Iterator<Table.Cell<R, C, V>> cellIterator();

	abstract Spliterator<Table.Cell<R, C, V>> cellSpliterator();

	public Collection<V> values() {
		Collection<V> result = this.values;
		return result == null ? (this.values = this.createValues()) : result;
	}

	Collection<V> createValues() {
		return new AbstractTable.Values();
	}

	Iterator<V> valuesIterator() {
		return new TransformedIterator<Cell<R, C, V>, V>(this.cellSet().iterator()) {
			V transform(Table.Cell<R, C, V> cell) {
				return cell.getValue();
			}
		};
	}

	Spliterator<V> valuesSpliterator() {
		return CollectSpliterators2.map(this.cellSpliterator(), Table.Cell::getValue);
	}

	public boolean equals(@Nullable Object obj) {
		return Maps2.equalsImpl(this, obj);
	}

	public int hashCode() {
		return this.cellSet().hashCode();
	}

	public String toString() {
		return this.rowMap().toString();
	}

	class Values extends AbstractCollection<V> {
		Values() {
		}

		public Iterator<V> iterator() {
			return AbstractTable.this.valuesIterator();
		}

		public Spliterator<V> spliterator() {
			return AbstractTable.this.valuesSpliterator();
		}

		public boolean contains(Object o) {
			return AbstractTable.this.containsValue(o);
		}

		public void clear() {
			AbstractTable.this.clear();
		}

		public int size() {
			return AbstractTable.this.size();
		}
	}

	class CellSet extends AbstractSet<Table.Cell<R, C, V>> {
		CellSet() {
		}

		public boolean contains(Object o) {
			if (!(o instanceof Table.Cell)) {
				return false;
			} else {
				Table.Cell<?, ?, ?> cell = (Table.Cell)o;
				Map<C, V> row = (Map)Maps2.safeGet(AbstractTable.this.rowMap(), cell.getRowKey());
				return row != null && Maps2.safeContains(row.entrySet(), Maps.immutableEntry(cell.getColumnKey(), cell.getValue()));
			}
		}

		public boolean remove(@Nullable Object o) {
			if (!(o instanceof Table.Cell)) {
				return false;
			} else {
				Table.Cell<?, ?, ?> cell = (Table.Cell)o;
				Map<C, V> row = (Map)Maps2.safeGet(AbstractTable.this.rowMap(), cell.getRowKey());
				return row != null && Maps2.safeRemove(row.entrySet(), Maps.immutableEntry(cell.getColumnKey(), cell.getValue()));
			}
		}

		public void clear() {
			AbstractTable.this.clear();
		}

		public Iterator<Table.Cell<R, C, V>> iterator() {
			return AbstractTable.this.cellIterator();
		}

		public Spliterator<Table.Cell<R, C, V>> spliterator() {
			return AbstractTable.this.cellSpliterator();
		}

		public int size() {
			return AbstractTable.this.size();
		}
	}
}
