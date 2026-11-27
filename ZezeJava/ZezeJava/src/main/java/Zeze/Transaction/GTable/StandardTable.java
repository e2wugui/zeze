/*
 * Copyright (C) 2008 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package Zeze.Transaction.GTable;


import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/**
 * 基于「行键 -> (列键 -> 值)」两层 {@link Map} 的 {@link Table} 实现，不支持 {@code null} 键或值。
 * <p>{@link #column}、{@link #columnKeySet()} 与 {@link #columnMap()} 的迭代器不支持 {@code remove()}。
 * 底层采用弱一致快照迭代且非线程安全，不实现 {@code Serializable}。
 */
class StandardTable<R, C, V> extends AbstractTable<R, C, V> {
  // 两个字段由 GTable1/GTable2 的所有构造器完成注入。
  @SuppressWarnings("NotNullFieldNotInitialized")
  Map<R, Map<C, V>> backingMap;
  @SuppressWarnings("NotNullFieldNotInitialized")
  Supplier<? extends Map<C, V>> factory;

  // 查询

  @Override
  public boolean contains(@Nullable Object rowKey, @Nullable Object columnKey) {
    return rowKey != null && columnKey != null && super.contains(rowKey, columnKey);
  }

  @Override
  public boolean containsColumn(@Nullable Object columnKey) {
    if (columnKey == null) {
      return false;
    }
    for (Map<C, V> map : backingMap.values()) {
      if (Utils.safeContainsKey(map, columnKey)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean containsRow(@Nullable Object rowKey) {
    return rowKey != null && Utils.safeContainsKey(backingMap, rowKey);
  }

  @Override
  public boolean containsValue(@Nullable Object value) {
    return value != null && super.containsValue(value);
  }

  @Override
  @Nullable
  public V get(@Nullable Object rowKey, @Nullable Object columnKey) {
    if (rowKey == null || columnKey == null) {
      return null;
    }
    // 单格读取直接查两层 Map，避免临时 Row 视图与外层重复查询。
    Map<C, V> row = Utils.safeGet(backingMap, rowKey);
    return row == null ? null : Utils.safeGet(row, columnKey);
  }

  @Override
  public boolean isEmpty() {
    return backingMap.isEmpty();
  }

  @Override
  public int size() {
    int size = 0;
    for (Map<C, V> map : backingMap.values()) {
      size += map.size();
    }
    return size;
  }

  // 修改

  @Override
  public void clear() {
    backingMap.clear();
  }

  @Override
  @Nullable
  public V put(R rowKey, C columnKey, V value) {
    Utils.checkNotNull(rowKey);
    Utils.checkNotNull(columnKey);
    Utils.checkNotNull(value);
    // 新行先写入内层再挂入外层，避免内层写入抛异常后在外层残留空行。
    Map<C, V> map = backingMap.get(rowKey);
    if (map == null) {
      map = factory.get();
      V old = map.put(columnKey, value);
      backingMap.put(rowKey, map);
      return old;
    }
    return map.put(columnKey, value);
  }

  @Override
  @Nullable
  public V remove(@Nullable Object rowKey, @Nullable Object columnKey) {
    if ((rowKey == null) || (columnKey == null)) {
      return null;
    }
    Map<C, V> map = Utils.safeGet(backingMap, rowKey);
    if (map == null) {
      return null;
    }
    //noinspection SuspiciousMethodCalls
    V value = map.remove(columnKey);
    if (map.isEmpty()) {
      //noinspection SuspiciousMethodCalls
      backingMap.remove(rowKey);
    }
    return value;
  }

  private Map<R, V> removeColumn(@Nullable Object column) {
    Map<R, V> output = new LinkedHashMap<>();
    Iterator<Entry<R, Map<C, V>>> iterator = backingMap.entrySet().iterator();
    while (iterator.hasNext()) {
      Entry<R, Map<C, V>> entry = iterator.next();
	  //noinspection SuspiciousMethodCalls
	  V value = entry.getValue().remove(column);
      if (value != null) {
        output.put(entry.getKey(), value);
        if (entry.getValue().isEmpty()) {
          iterator.remove();
        }
      }
    }
    return output;
  }

  private boolean containsMapping(
      @Nullable Object rowKey, @Nullable Object columnKey, @Nullable Object value) {
    return value != null && value.equals(get(rowKey, columnKey));
  }

  /** 若存在匹配的（行键、列键、值）映射则移除并返回 {@code true}。 */
  private boolean removeMapping(
      @Nullable Object rowKey, @Nullable Object columnKey, @Nullable Object value) {
    if (containsMapping(rowKey, columnKey, value)) {
      remove(rowKey, columnKey);
      return true;
    }
    return false;
  }

  // 视图

  /** 以底层表是否为空作为 {@code isEmpty()}、以清空整表作为 {@code clear()} 的视图集合基类。 */
  private abstract class TableSet<T> extends Utils.ImprovedAbstractSet<T> {
    @Override
    public boolean isEmpty() {
      return backingMap.isEmpty();
    }

    @Override
    public void clear() {
      backingMap.clear();
    }
  }

  /**
   * {@inheritDoc}
   * <p>按行顺序遍历单元格，每个返回的 {@link Cell} 为获取时刻的不可变快照。
   */
  @Override
  public Set<Cell<R, C, V>> cellSet() {
    return super.cellSet();
  }

  @Override
  Iterator<Cell<R, C, V>> cellIterator() {
    return new CellIterator();
  }

  private class CellIterator implements Iterator<Cell<R, C, V>> {
    final Iterator<Entry<R, Map<C, V>>> rowIterator = backingMap.entrySet().iterator();
    @Nullable Entry<R, Map<C, V>> rowEntry;
    Iterator<Entry<C, V>> columnIterator = Utils.emptyModifiableIterator();

    @Override
    public boolean hasNext() {
      return rowIterator.hasNext() || columnIterator.hasNext();
    }

    @Override
    public Cell<R, C, V> next() {
      if (!columnIterator.hasNext()) {
        rowEntry = rowIterator.next();
        columnIterator = rowEntry.getValue().entrySet().iterator();
      }
      Utils.requireNonNull(rowEntry);
      Entry<C, V> columnEntry = columnIterator.next();
      return Utils.immutableCell(rowEntry.getKey(), columnEntry.getKey(), columnEntry.getValue());
    }

    @Override
    public void remove() {
      columnIterator.remove();
      if (Utils.requireNonNull(rowEntry).getValue().isEmpty()) {
        rowIterator.remove();
        rowEntry = null;
      }
    }
  }

  @Override
  Spliterator<Cell<R, C, V>> cellSpliterator() {
    return CollectSpliterators2.flatMap(
        backingMap.entrySet().spliterator(),
        (Entry<R, Map<C, V>> rowEntry) ->
            CollectSpliterators2.map(
                rowEntry.getValue().entrySet().spliterator(),
                (Entry<C, V> columnEntry) ->
                    Utils.immutableCell(rowEntry.getKey(), columnEntry.getKey(), columnEntry.getValue())),
        Spliterator.DISTINCT | Spliterator.SIZED,
        size());
  }

  @Override
  public Map<C, V> row(R rowKey) {
    return new Row(rowKey);
  }

  class Row extends Utils.IteratorBasedAbstractMap<C, V> {
    final R rowKey;

    Row(R rowKey) {
      this.rowKey = Utils.checkNotNull(rowKey);
    }

    @Nullable Map<C, V> backingRowMap;

    final void updateBackingRowMapField() {
      // 行被移除再重建后可能指向新实例，每次访问前刷新引用。
      backingRowMap = computeBackingRowMap();
    }

    @Nullable
    Map<C, V> computeBackingRowMap() {
      return backingMap.get(rowKey);
    }

    // 每次移除元素后调用，确保外层不保留空行。
    void maintainEmptyInvariant() {
      updateBackingRowMapField();
      if (backingRowMap != null && backingRowMap.isEmpty()) {
        backingMap.remove(rowKey);
        backingRowMap = null;
      }
    }

    @Override
    public boolean containsKey(@Nullable Object key) {
      updateBackingRowMapField();
      return (key != null && backingRowMap != null) && Utils.safeContainsKey(backingRowMap, key);
    }

    @Override
    @Nullable
    public V get(@Nullable Object key) {
      updateBackingRowMapField();
      return (key != null && backingRowMap != null) ? Utils.safeGet(backingRowMap, key) : null;
    }

    @Override
    @Nullable
    public V put(C key, V value) {
      Utils.checkNotNull(value);
      if (backingRowMap != null && !backingRowMap.isEmpty()) {
        // 若行已重建则重新绑定，若仍不存在则拒绝写入过期视图。
        Map<C, V> currentRowMap = computeBackingRowMap();
        if (currentRowMap == null)
          throw new IllegalStateException("stale row view: row " + rowKey + " has been removed from table");
        backingRowMap = currentRowMap;
        return backingRowMap.put(key, value);
      }
      return StandardTable.this.put(rowKey, key, value);
    }

    @Override
    @Nullable
    public V remove(@Nullable Object key) {
      updateBackingRowMapField();
      if (backingRowMap == null) {
        return null;
      }
      V result = Utils.safeRemove(backingRowMap, key);
      maintainEmptyInvariant();
      return result;
    }

    @Override
    public void clear() {
      updateBackingRowMapField();
      if (backingRowMap != null) {
        backingRowMap.clear();
      }
      maintainEmptyInvariant();
    }

    @Override
    public int size() {
      updateBackingRowMapField();
      return (backingRowMap == null) ? 0 : backingRowMap.size();
    }

    @Override
    Iterator<Entry<C, V>> entryIterator() {
      updateBackingRowMapField();
      if (backingRowMap == null) {
        return Utils.emptyModifiableIterator();
      }
      final Iterator<Entry<C, V>> iterator = backingRowMap.entrySet().iterator();
      return new Iterator<>() {
        private @Nullable Entry<C, V> lastReturned;

        @Override
        public boolean hasNext() {
          return iterator.hasNext();
        }

        @Override
        public Entry<C, V> next() {
          return lastReturned = wrapEntry(iterator.next());
        }

        @Override
        public void remove() {
          if (lastReturned == null) {
            throw new IllegalStateException("iterator remove() before next()");
          }
          updateBackingRowMapField();
          if (backingRowMap == null || backingRowMap.get(lastReturned.getKey()) != lastReturned.getValue()) {
            throw new ConcurrentModificationException("structural modification during iteration");
          }
          backingRowMap.remove(lastReturned.getKey());
          lastReturned = null;
          maintainEmptyInvariant();
        }
      };
    }

    @Override
    Spliterator<Entry<C, V>> entrySpliterator() {
      updateBackingRowMapField();
      if (backingRowMap == null) {
        return Spliterators.emptySpliterator();
      }
      return CollectSpliterators2.map(backingRowMap.entrySet().spliterator(), this::wrapEntry);
    }

    Entry<C, V> wrapEntry(final Entry<C, V> entry) {
      return new ForwardingMapEntry<>() {
        // 缓存当前值，因为底层不可变 entry 不会随 setValue 更新。
        private V value = entry.getValue();

        @Override
        protected Entry<C, V> delegate() {
          return entry;
        }

        @Override
        public V getValue() {
          return value;
        }

        @Override
        public V setValue(@Nullable V value) {
          // 底层 entry 不可变，通过 Row.put 执行事务化更新。
          var old = Row.this.put(entry.getKey(), Utils.checkNotNull(value));
          this.value = Utils.checkNotNull(value);
          return Utils.uncheckedCastNullableTToT(old);
        }

        @Override
        public int hashCode() {
          return standardHashCode();
        }

        @SuppressWarnings("EqualsDoesNotCheckParameterClass")
        @Override
        public boolean equals(@Nullable Object object) {
          return standardEquals(object);
        }
      };
    }
  }

  /**
   * {@inheritDoc}
   * <p>返回视图的迭代器不支持 {@code remove()}。
   */
  @Override
  public Map<R, V> column(C columnKey) {
    return new Column(columnKey);
  }

  private class Column extends Utils.ViewCachingAbstractMap<R, V> {
    final C columnKey;

    Column(C columnKey) {
      this.columnKey = Utils.checkNotNull(columnKey);
    }

    @Override
    @Nullable
    public V put(R key, V value) {
      return StandardTable.this.put(key, columnKey, value);
    }

    @Override
    @Nullable
    public V get(@Nullable Object key) {
      return StandardTable.this.get(key, columnKey);
    }

    @Override
    public boolean containsKey(@Nullable Object key) {
      return StandardTable.this.contains(key, columnKey);
    }

    @Override
    @Nullable
    public V remove(@Nullable Object key) {
      return StandardTable.this.remove(key, columnKey);
    }

    /** 移除列视图中满足条件的全部映射。 */
    boolean removeFromColumnIf(Predicate<? super Entry<R, V>> predicate) {
      boolean changed = false;
      Iterator<Entry<R, Map<C, V>>> iterator = backingMap.entrySet().iterator();
      while (iterator.hasNext()) {
        Entry<R, Map<C, V>> entry = iterator.next();
        Map<C, V> map = entry.getValue();
        V value = map.get(columnKey);
        if (value != null && predicate.apply(Utils.immutableEntry(entry.getKey(), value))) {
          map.remove(columnKey);
          changed = true;
          if (map.isEmpty()) {
            iterator.remove();
          }
        }
      }
      return changed;
    }

    @Override
    Set<Entry<R, V>> createEntrySet() {
      return new EntrySet();
    }

    private class EntrySet extends Utils.ImprovedAbstractSet<Entry<R, V>> {
      @Override
      public Iterator<Entry<R, V>> iterator() {
        return new EntrySetIterator();
      }

      @Override
      public int size() {
        int size = 0;
        for (Map<C, V> map : backingMap.values()) {
          if (map.containsKey(columnKey)) {
            size++;
          }
        }
        return size;
      }

      @Override
      public boolean isEmpty() {
        return !containsColumn(columnKey);
      }

      @Override
      public void clear() {
        removeFromColumnIf(Utils.alwaysTrue());
      }

      @Override
      public boolean contains(@Nullable Object o) {
        if (o instanceof Entry<?, ?> entry) {
			return containsMapping(entry.getKey(), columnKey, entry.getValue());
        }
        return false;
      }

      @Override
      public boolean remove(@Nullable Object obj) {
        if (obj instanceof Entry<?, ?> entry) {
			return removeMapping(entry.getKey(), columnKey, entry.getValue());
        }
        return false;
      }

      @Override
      public boolean removeAll(Collection<?> c) {
        if (c instanceof Set<?> && c.size() > backingMap.size()) {
          return removeFromColumnIf(Utils.in(c));
        }
        // 参数也可能是同一张表的视图。先快照，避免删除时使参数迭代器失效。
        return Utils.removeAllImpl(this, new ArrayList<>(c).iterator());
      }

      @Override
      public boolean retainAll(Collection<?> c) {
        return removeFromColumnIf(Utils.not(Utils.in(c)));
      }
    }

    private class EntrySetIterator extends AbstractIterator<Entry<R, V>> {
      final Iterator<Entry<R, Map<C, V>>> iterator = backingMap.entrySet().iterator();

      @Override
      @Nullable
      protected Entry<R, V> computeNext() {
        while (iterator.hasNext()) {
          final Entry<R, Map<C, V>> entry = iterator.next();
          if (entry.getValue().containsKey(columnKey)) {
            class EntryImpl extends AbstractMapEntry<R, V> {
              @Override
              public R getKey() {
                return entry.getKey();
              }

              @Override
              public V getValue() {
                return entry.getValue().get(columnKey);
              }

              @Override
              public V setValue(@Nullable V value) {
                return Utils.uncheckedCastNullableTToT(entry.getValue().put(columnKey, Utils.checkNotNull(value)));
              }
            }
            return new EntryImpl();
          }
        }
        return endOfData();
      }
    }

    @Override
    Set<R> createKeySet() {
      return new KeySet();
    }

    private class KeySet extends Utils.KeySet<R, V> {
      KeySet() {
        super(Column.this);
      }

      @Override
      public boolean contains(@Nullable Object obj) {
        return StandardTable.this.contains(obj, columnKey);
      }

      @Override
      public boolean remove(@Nullable Object obj) {
        return StandardTable.this.remove(obj, columnKey) != null;
      }

      @Override
      public boolean removeAll(Collection<?> c) {
        if (c instanceof Set<?> && c.size() > backingMap.size()) {
          return removeFromColumnIf(Utils.keyPredicateOnEntries(Utils.in(c)));
        }
        return Utils.removeAllImpl(this, new ArrayList<>(c).iterator());
      }

      @Override
      public boolean retainAll(final Collection<?> c) {
        return removeFromColumnIf(Utils.keyPredicateOnEntries(Utils.not(Utils.in(c))));
      }
    }

    @Override
    Collection<V> createValues() {
      return new Values();
    }

    private class Values extends Utils.Values<R, V> {
      Values() {
        super(Column.this);
      }

      @Override
      public boolean removeAll(final Collection<?> c) {
        return removeFromColumnIf(Utils.valuePredicateOnEntries(Utils.in(c)));
      }

      @Override
      public boolean retainAll(final Collection<?> c) {
        return removeFromColumnIf(Utils.valuePredicateOnEntries(Utils.not(Utils.in(c))));
      }
    }
  }

  @Override
  public Set<R> rowKeySet() {
    return rowMap().keySet();
  }

  @Nullable private transient Set<C> columnKeySet;

  /**
   * {@inheritDoc}
   * <p>按行顺序去重遍历列键，迭代器不支持 {@code remove()}。
   */
  @Override
  public Set<C> columnKeySet() {
    Set<C> result = columnKeySet;
    return (result == null) ? columnKeySet = new ColumnKeySet() : result;
  }

  private class ColumnKeySet extends TableSet<C> {
    @Override
    public Iterator<C> iterator() {
      return createColumnKeyIterator();
    }

    @Override
    public int size() {
      return Utils.size(iterator());
    }

    @Override
    public boolean remove(@Nullable Object obj) {
      if (obj == null) {
        return false;
      }
      boolean changed = false;
      Iterator<Map<C, V>> iterator = backingMap.values().iterator();
      while (iterator.hasNext()) {
        Map<C, V> map = iterator.next();
        if (map.remove(obj) != null) {
          changed = true;
          if (map.isEmpty()) {
            iterator.remove();
          }
        }
      }
      return changed;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
      Utils.checkNotNull(c);
      boolean changed = false;
      Iterator<Map<C, V>> iterator = backingMap.values().iterator();
      while (iterator.hasNext()) {
        Map<C, V> map = iterator.next();
        // 避免 c 含 null 时直接调用 map.keySet().removeAll(c) 触发 NPE。
        if (Utils.removeAll(map.keySet().iterator(), c)) {
          changed = true;
          if (map.isEmpty()) {
            iterator.remove();
          }
        }
      }
      return changed;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
      Utils.checkNotNull(c);
      boolean changed = false;
      Iterator<Map<C, V>> iterator = backingMap.values().iterator();
      while (iterator.hasNext()) {
        Map<C, V> map = iterator.next();
        if (map.keySet().retainAll(c)) {
          changed = true;
          if (map.isEmpty()) {
            iterator.remove();
          }
        }
      }
      return changed;
    }

    @Override
    public boolean contains(@Nullable Object obj) {
      return containsColumn(obj);
    }
  }

  /** 创建去重遍历所有列键的迭代器。 */
  Iterator<C> createColumnKeyIterator() {
    return new ColumnKeyIterator();
  }

  private class ColumnKeyIterator extends AbstractIterator<C> {
    // 使用 HashSet 仅记录列键去重，避免通过 factory.get() 创建行容器并误触受管 Bean 状态。
    final Set<C> seen = new java.util.HashSet<>();
    final Iterator<Map<C, V>> mapIterator = backingMap.values().iterator();
    Iterator<Entry<C, V>> entryIterator = Utils.emptyIterator();

    @Override
    @Nullable
    protected C computeNext() {
      while (true) {
        if (entryIterator.hasNext()) {
          Entry<C, V> entry = entryIterator.next();
          if (!seen.contains(entry.getKey())) {
            seen.add(entry.getKey());
            return entry.getKey();
          }
        } else if (mapIterator.hasNext()) {
          entryIterator = mapIterator.next().entrySet().iterator();
        } else {
          return endOfData();
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   * <p>按行顺序遍历全部值。
   */
  @Override
  public Collection<V> values() {
    return super.values();
  }

  @Nullable private transient Map<R, Map<C, V>> rowMap;

  @Override
  public Map<R, Map<C, V>> rowMap() {
    Map<R, Map<C, V>> result = rowMap;
    return (result == null) ? rowMap = createRowMap() : result;
  }

  Map<R, Map<C, V>> createRowMap() {
    return new RowMap();
  }

  class RowMap extends Utils.ViewCachingAbstractMap<R, Map<C, V>> {
    @Override
    public boolean containsKey(@Nullable Object key) {
      return containsRow(key);
    }

    @SuppressWarnings("unchecked")
    @Override
    @Nullable
    public Map<C, V> get(@Nullable Object key) {
      return containsRow(key) ? row((R) Utils.requireNonNull(key)) : null;
    }

    @Override
    @Nullable
    public Map<C, V> remove(@Nullable Object key) {
      return (key == null) ? null : backingMap.remove(key);
    }

    @Override
    protected Set<Entry<R, Map<C, V>>> createEntrySet() {
      return new EntrySet();
    }

    private final class EntrySet extends TableSet<Entry<R, Map<C, V>>> {
      @Override
      public Iterator<Entry<R, Map<C, V>>> iterator() {
        return Utils.asMapEntryIterator(backingMap.keySet(), StandardTable.this::row);
      }

      @Override
      public int size() {
        return backingMap.size();
      }

      @Override
      public boolean contains(@Nullable Object obj) {
        if (obj instanceof Entry<?, ?> entry) {
			return entry.getKey() != null
              && entry.getValue() instanceof Map
              && Utils.safeContains(backingMap.entrySet(), entry);
        }
        return false;
      }

      @Override
      public boolean remove(@Nullable Object obj) {
        if (obj instanceof Entry<?, ?> entry) {
			return entry.getKey() != null
              && entry.getValue() instanceof Map
              && backingMap.entrySet().remove(entry);
        }
        return false;
      }
    }
  }

  @Nullable private transient ColumnMap columnMap;

  @Override
  public Map<C, Map<R, V>> columnMap() {
    ColumnMap result = columnMap;
    return (result == null) ? columnMap = new ColumnMap() : result;
  }

  private class ColumnMap extends Utils.ViewCachingAbstractMap<C, Map<R, V>> {
    @SuppressWarnings("unchecked")
    @Override
    @Nullable
    public Map<R, V> get(@Nullable Object key) {
      return containsColumn(key) ? column((C) Utils.requireNonNull(key)) : null;
    }

    @Override
    public boolean containsKey(@Nullable Object key) {
      return containsColumn(key);
    }

    @Override
    @Nullable
    public Map<R, V> remove(@Nullable Object key) {
      return containsColumn(key) ? removeColumn(key) : null;
    }

    @Override
    public Set<Entry<C, Map<R, V>>> createEntrySet() {
      return new ColumnMapEntrySet();
    }

    @Override
    public Set<C> keySet() {
      return columnKeySet();
    }

    @Override
    Collection<Map<R, V>> createValues() {
      return new ColumnMapValues();
    }

    private final class ColumnMapEntrySet extends TableSet<Entry<C, Map<R, V>>> {
      @Override
      public Iterator<Entry<C, Map<R, V>>> iterator() {
        return Utils.asMapEntryIterator(columnKeySet(), StandardTable.this::column);
      }

      @Override
      public int size() {
        return columnKeySet().size();
      }

      @Override
      public boolean contains(@Nullable Object obj) {
        if (obj instanceof Entry<?, ?> entry) {
			if (containsColumn(entry.getKey())) {
            return Utils.requireNonNull(get(entry.getKey())).equals(entry.getValue());
          }
        }
        return false;
      }

      @Override
      public boolean remove(@Nullable Object obj) {
        if (contains(obj) && obj instanceof Entry<?, ?> entry) {
			removeColumn(entry.getKey());
          return true;
        }
        return false;
      }

      @Override
      public boolean removeAll(Collection<?> c) {
        // 不走父类的 removeAll，避免在部分情况下触发不支持的 columnKeySet().iterator().remove()。
        Utils.checkNotNull(c);
        return Utils.removeAllImpl(this, c.iterator());
      }

      @Override
      public boolean retainAll(Collection<?> c) {
        Utils.checkNotNull(c);
        boolean changed = false;
        for (C columnKey : Utils.newArrayList(columnKeySet().iterator())) {
          if (!c.contains(Utils.immutableEntry(columnKey, column(columnKey)))) {
            removeColumn(columnKey);
            changed = true;
          }
        }
        return changed;
      }
    }

    private class ColumnMapValues extends Utils.Values<C, Map<R, V>> {
      ColumnMapValues() {
        super(ColumnMap.this);
      }

      @Override
      public boolean remove(@Nullable Object obj) {
        for (Entry<C, Map<R, V>> entry : ColumnMap.this.entrySet()) {
          if (entry.getValue().equals(obj)) {
            removeColumn(entry.getKey());
            return true;
          }
        }
        return false;
      }

      @Override
      public boolean removeAll(Collection<?> c) {
        Utils.checkNotNull(c);
        boolean changed = false;
        for (C columnKey : Utils.newArrayList(columnKeySet().iterator())) {
          if (c.contains(column(columnKey))) {
            removeColumn(columnKey);
            changed = true;
          }
        }
        return changed;
      }

      @Override
      public boolean retainAll(Collection<?> c) {
        Utils.checkNotNull(c);
        boolean changed = false;
        for (C columnKey : Utils.newArrayList(columnKeySet().iterator())) {
          if (!c.contains(column(columnKey))) {
            removeColumn(columnKey);
            changed = true;
          }
        }
        return changed;
      }
    }
  }
}
