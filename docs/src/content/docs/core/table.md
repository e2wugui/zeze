---
title: "Table 存储接口"
sidebar:
  order: 4
---

**Table** 是 Zeze 中的核心数据存储单元，语义上等价于一个类型安全的 `Map<K, V>`。每张表由一个 **key**（实现 `Comparable`）和一个 **value**（继承自 `Bean`）组成，通过 solution.xml 中的 `<table>` 声明并由代码生成器自动产生 `TableX<K, V>` 子类（→ [bean](./bean.md), [solution-xml](./solution-xml.md)）。

## 核心 CRUD API

以下方法均须在事务内调用（→ [transaction](./transaction.md)），否则会抛出异常。

### get

```java
// 返回 value，记录不存在时返回 null
@Nullable V get(@NotNull K key)
```

`get` 是最基础的读取操作。首次访问某条记录时，框架会从 **Storage** 层加载到内存缓存，并提升全局缓存状态至 `StateShare`。

### getOrAdd

```java
// 记录不存在则创建并返回新值
@NotNull V getOrAdd(@NotNull K key)

// 通过 OutObject<Boolean> 判断是否新建
@NotNull V getOrAdd(@NotNull K key, @Nullable OutObject<Boolean> isAdd)
```

当 `isAdd.value == true` 时表示本次创建了新记录。典型用法：

```java
var isAdd = new OutObject<Boolean>();
BPlayer player = tablePlayer.getOrAdd(roleId, isAdd);
if (isAdd.value) {
    // 首次创建，初始化默认值
    player.setLevel(1);
    player.setName("newbie");
}
```

### put / insert / tryAdd

```java
// 直接写入（覆盖或新增）
void put(@NotNull K key, @NotNull V value)

// 仅当 key 不存在时写入，已存在则抛 IllegalArgumentException
void insert(@NotNull K key, @NotNull V value)

// 仅当 key 不存在时写入，返回是否成功
boolean tryAdd(@NotNull K key, @NotNull V value)
```

`put` 不管记录是否已存在，直接覆盖。`insert` 和 `tryAdd` 提供了"仅新增"语义。

### remove

```java
void remove(@NotNull K key)
```

将指定 key 对应的记录标记为删除。记录在事务提交后才会真正生效，后续由 **Checkpoint** 刷写到数据库。

### contains

```java
boolean contains(@NotNull K key)
```

等价于 `get(key) != null`，用于判断记录是否存在。

## 事务外读取

部分场景需要在事务外读取数据，框架提供了两种方式：

### selectCopy

```java
// 返回记录的深拷贝，事务内外均可使用
@Nullable V selectCopy(@NotNull K key)
```

- 事务内调用：若该事务已访问过此 key，返回最新值的拷贝；否则从后台加载并拷贝，但不加入事务的 RecordAccessed。
- 事务外调用：从缓存或数据库加载后返回拷贝。
- 得到的对象不应用于修改，建议搭配 `ReadOnly` 接口使用。

### selectDirty

```java
// 从本地缓存快速读取，默认 3 秒有效期
@Nullable V selectDirty(@NotNull K key)
// 自定义缓存有效期（毫秒），0 表示总是从数据库取最新值
@Nullable V selectDirty(@NotNull K key, int cacheTTL)
```

`selectDirty` 不经过 **GlobalCacheManager** 权限协商，速度快但一致性较弱，适合用于 `whileCommit` 回调、日志统计等可容忍短暂不一致的场景。

## 内存缓存与 LRU 机制

### TableCache

**TableCache** 是每张表的内存缓存层，内部维护一个 `ConcurrentHashMap<K, Record1<K, V>>` 作为主数据存储，并使用分段式 LRU 策略管理缓存淘汰。

核心参数：

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `MAX_NODE_COUNT` | 8640 | LRU 节点队列最大长度，超过时触发 shrink |
| `SHRINK_NODE_COUNT` | 8000 | shrink 后的目标节点数 |
| `CacheInitialCapacity` | 31 | 缓存初始容量 |
| `RealCacheCapacity` | -1（不限） | 缓存容量上限，-1 表示不限制 |

LRU 淘汰策略：框架定期创建新的 **热点段**（`ConcurrentHashMap`），新记录总是插入当前热点段。后台定时任务检查总节点数，超过 `MAX_NODE_COUNT` 时，将最老的段合并到当前头部段并丢弃多余段。当缓存记录总数超过 `RealCacheCapacity` 时，从最老的段开始逐条尝试回收非脏、非新鲜的记录。

### 本地 RocksDB 缓存

每张表在本地都维护一个 **RocksDB 缓存表**（`localRocksCacheTable`），记录从远程数据库加载后会同步写入本地 RocksDB。当内存中的记录被 LRU 淘汰后，下次访问可从本地 RocksDB 读取，避免远程数据库访问。

## Storage 与数据库同步

### Storage

**Storage** 是 Table 与底层数据库之间的桥梁，在 `open()` 时创建。对于内存表（`isMemory() == true`），Storage 为 `null`。

```java
public final class Storage<K extends Comparable<K>, V extends Bean> {
    Storage(TableX<K, V> table, Database database, String tableName)
    Table getTable()
    Database.Table getDatabaseTable()
}
```

### 脏数据标记与刷写流程

每条记录（**Record** / **Record1**）内部维护一个 `dirty` 标志。数据修改并提交后，记录被标记为脏。刷写流程由 **Checkpoint** 驱动，支持两种模式：

- **CheckpointMode.Immediately** — 事务提交后立即将变更写入数据库，不使用脏标记。
- **CheckpointMode.Table** — 按表批量刷写。Checkpoint 时遍历脏记录，依次执行 `encode0()`（序列化快照）和 `flush()`（写入数据库），最后 `cleanup()` 清理快照状态。

### Record 生命周期

一条记录从访问到持久化，经历以下阶段：

1. **Load** — 通过 `TableCache.getOrAdd()` 创建 `Record1`，首次访问时从 Storage 或本地 RocksDB 加载 value。
2. **Access** — 事务通过 `get`/`getOrAdd` 获取记录，框架通过全局缓存管理器协商权限（`StateInvalid` → `StateShare` → `StateModify`）。
3. **Modify** — 事务修改 Bean 字段，`Procedure` 提交时调用 `Record.commit()` 设置 `dirty = true` 并更新 `timestamp`。
4. **Flush** — Checkpoint 调用 `Record.encode0()` 序列化快照，再调用 `Record.flush()` 写入数据库。
5. **Cleanup** — 写入完成后调用 `Record.cleanup()` 清除快照引用和脏标记。

## 遍历 API

Table 提供三类遍历方式，均须在事务外调用：

### walk / walkDesc — 遍历数据库（含缓存合并）

```java
// 遍历全表，返回处理的记录数
long walk(@NotNull TableWalkHandle<K, V> callback) throws Exception
long walkDesc(@NotNull TableWalkHandle<K, V> callback) throws Exception

// 仅遍历 key
long walkKey(@NotNull TableWalkKey<K> callback) throws Exception

// 分页遍历，exclusiveStartKey 为起始 key（不含），返回下一个 key
@Nullable K walk(@Nullable K exclusiveStartKey, int proposeLimit,
                 @NotNull TableWalkHandle<K, V> callback) throws Exception
```

`walk` 从后台数据库遍历，同时与内存缓存合并，能看到最新的已提交数据。注意：新增但未 Checkpoint 的记录可能看不到。每个记录回调时加读锁，回调完成立即释放。

回调接口定义：

```java
@FunctionalInterface
public interface TableWalkHandle<K, V> {
    boolean handle(@NotNull K key, @NotNull V value) throws Exception;
    // 返回 false 可中断遍历
}

@FunctionalInterface
public interface TableWalkKey<K> {
    boolean handle(@NotNull K key) throws Exception;
}
```

### walkDatabase / walkDatabaseRaw — 直接遍历数据库

```java
// 类型化遍历，看不到本地缓存数据
long walkDatabase(@NotNull TableWalkHandle<K, V> callback) throws Exception

// 原始字节遍历（仅 KV 表支持）
long walkDatabaseRaw(@NotNull TableWalkHandleRaw callback) throws Exception
```

`walkDatabase` 直接从后台数据库读取，**不经过本地缓存**，适合批量导出或后台分析。`walkDatabaseRaw` 以原始 `byte[]` 形式返回，效率更高但不做反序列化。

### walkMemory — 遍历内存缓存

```java
// 遍历当前缓存中的记录，返回处理的记录数
long walkMemory(@NotNull TableWalkHandle<K, V> callback) throws Exception

// 仅遍历缓存中的 key
long walkCacheKey(@NotNull TableWalkKey<K> callback) throws Exception
```

`walkMemory` 只遍历内存缓存中状态为 `StateShare` 或 `StateModify` 的记录。对于内存表，如果配置了容量限制，会从本地 RocksDB 缓存遍历。

## TableReadOnly 与 TableDynamic

### TableReadOnly

**TableReadOnly** 是一个只读接口，模块可将其暴露给其他模块用于只读访问：

```java
public interface TableReadOnly<K, V, VReadOnly> {
    @Nullable VReadOnly getReadOnly(@NotNull K key);
    boolean contains(@NotNull K key);
    @Nullable V selectCopy(@NotNull K key);
    // ... 以及所有 walk 方法的只读版本
}
```

生成代码会为每张表同时生成 `TableReadOnly` 实现，value 的只读视图通过 `VReadOnly` 泛型参数提供。

### TableDynamic

**TableDynamic** 允许运行时动态创建表，复用已有表的 key/value 编解码逻辑：

```java
// 基于母表创建动态表
var dynamicTable = new TableDynamic<>(zeze, "dynamic_table", templateTable);
```

动态表通过 `zeze.openDynamicTable()` 注册，使用与母表相同的序列化方案，但拥有独立的存储空间。可通过 `dropTable()` 删除。

## 在 solution.xml 中定义 Table

在 solution.xml 的 `<module>` 内通过 `<table>` 标签声明：

```xml
<module name="demo" id="1">
    <bean name="BPlayer">
        <variable id="1" name="Level" type="int"/>
        <variable id="2" name="Name" type="string"/>
    </bean>
    <table name="tPlayer" key="long" value="BPlayer"/>
</module>
```

**key 类型约束**：key 必须是简单类型（`int`、`long`、`string` 等）或 `<beankey>` 定义的复合键类型，且必须实现 `Comparable`。

**value 类型约束**：value 必须是 `<bean>` 定义的类型，继承自 `Zeze.Transaction.Bean`。

可选属性：

| 属性 | 说明 |
|------|------|
| `suffix` | 表名后缀，支持 `@ServerId`、`@AppMainVersion` 等变量替换，用于数据隔离 |
| `autoIncrement` | 是否使用自增 key |

代码生成器会根据定义自动生成 `TableX<K, V>` 的具体子类，开发者通过模块中自动注入的表实例进行操作。
