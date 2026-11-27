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

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * 二维稀疏映射表接口，将有序键对（行键、列键）映射到单个值。
 * <p>所有返回集合或 {@link Map} 的方法均返回底层表的实时视图，视图修改与表内容双向联动。
 *
 * @param <R> 行键类型
 * @param <C> 列键类型
 * @param <V> 值类型
 */
public interface Table<R, C, V> {
  // 查询

  /** 返回是否包含指定行键和列键的映射。 */
  boolean contains(
      @Nullable Object rowKey,
      @Nullable Object columnKey);

  /** 返回是否包含指定行键的映射。 */
  boolean containsRow(@Nullable Object rowKey);

  /** 返回是否包含指定列键的映射。 */
  boolean containsColumn(@Nullable Object columnKey);

  /** 返回是否包含指定值的映射。 */
  boolean containsValue(@Nullable Object value);

  /** 返回指定行键和列键对应的值，不存在时返回 {@code null}。 */
  @Nullable
  V get(
      @Nullable Object rowKey,
      @Nullable Object columnKey);

  /** 返回表是否为空。 */
  boolean isEmpty();

  /** 返回表中单元格映射的总数。 */
  int size();

  /** 比较两表是否相等（等价于 {@link #cellSet()} 集合相等）。 */
  @Override
  boolean equals(@Nullable Object obj);

  /**
   * 返回此表的哈希码。
   * <p>注意：{@link GTable2} 中 Bean 值按引用身份计算哈希，内容相等的表可能哈希码不同，勿作为哈希容器键使用。
   */
  @Override
  int hashCode();

  // 修改

  /** 清空表中的所有映射。 */
  void clear();

  /** 将指定值关联到给定的行键与列键，返回旧值（若先前不存在则返回 {@code null}）。 */
  @Nullable
  V put(R rowKey, C columnKey, V value);

  /** 将指定表的所有单元格映射复制到本表。 */
  void putAll(Table<? extends R, ? extends C, ? extends V> table);

  /** 移除指定行键与列键对应的映射，返回旧值（不存在则返回 {@code null}）。 */
  @Nullable
  V remove(
      @Nullable Object rowKey,
      @Nullable Object columnKey);

  // 视图

  /** 返回指定行键下「列键 -> 值」的实时视图；若行不存在则返回空视图。 */
  Map<C, V> row(R rowKey);

  /** 返回指定列键下「行键 -> 值」的实时视图；若列不存在则返回空视图。 */
  Map<R, V> column(C columnKey);

  /** 返回所有单元格（行键、列键、值）三元组的实时视图，不支持 {@code add}/{@code addAll}。 */
  Set<Cell<R, C, V>> cellSet();

  /** 返回所有非空行键的实时视图。 */
  Set<R> rowKeySet();

  /** 返回所有非空列键的实时视图。 */
  Set<C> columnKeySet();

  /** 返回所有值的实时视图（可能包含重复值）。 */
  Collection<V> values();

  /**
   * 返回「行键 -> 行映射」的实时视图。外层视图不支持 {@code put}/{@code putAll}/{@code setValue}，
   * 但 {@code rowMap().get(r)} 返回的行映射与 {@link #row} 行为一致。
   */
  Map<R, Map<C, V>> rowMap();

  /**
   * 返回「列键 -> 列映射」的实时视图。外层视图不支持 {@code put}/{@code putAll}/{@code setValue}，
   * 但 {@code columnMap().get(c)} 返回的列映射与 {@link #column} 行为一致。
   */
  Map<C, Map<R, V>> columnMap();

  /** 表示表中单个映射的（行键、列键、值）三元组。 */
  interface Cell<R, C, V> {
    /** 返回单元格的行键。 */
    R getRowKey();

    /** 返回单元格的列键。 */
    C getColumnKey();

    /** 返回单元格的值。 */
    V getValue();

    /** 当且仅当两单元格的行键、列键和值均相等时返回 {@code true}。 */
    @Override
    boolean equals(@Nullable Object obj);

    /** 返回单元格的哈希码（由行键、列键和值共同计算）。 */
    @Override
    int hashCode();
  }
}
