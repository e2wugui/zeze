---
title: "Table 存储接口"
description: "Zeze Table 的 CRUD、事务外读取、缓存、Storage 与遍历接口速查"
category: reference
order: 4
---

本文是 Zeze **Table** 的完整 API 参考——覆盖 CRUD、事务外读取、TableCache、本地 RocksDB 缓存、Storage、Record 生命周期和遍历接口，供写代码时随查随用。数据模型见 [Bean 数据模型](./bean.md)，XML 声明见 [solution.xml 参考](./solution-xml.md)，事务要求见 [事务系统](./transaction.md)。

## 什么是 Table

Table 语义等价于一个**类型安全的 `Map<K, V>`**：

| 约束 | 说明 |
|------|------|
| key | 基本类型或 beankey，必须实现 `Comparable` |
| value | 必须继承 Bean |

在 `solution.xml` 里用 `<table>` 声明后，代码生成器产出 `TableXxx<K, V>` 子类。

```xml
<table name="tPlayer" key="long" value="Player"/>
```

```java
TableTPlayer table = module.getTableTPlayer();
Player player = table.getOrAdd(roleId);
```

> **重要：所有 CRUD 操作都必须在事务内调用。** Table 的事务特性依赖框架对每次访问的记录，脱离事务无法保证一致性。见 [事务系统](./transaction.md)。

---

## CRUD（事务内）

### 读取

| 方法 | 说明 |
|------|------|
| `get(key)` | 取值。不存在返回 `null`。首次访问从 Storage 加载，提升为 `StateShare` |
| `getOrAdd(key)` | 取值，不存在则创建 |
| `getOrAdd(key, OutObject<Boolean> isAdd)` | 同上，`isAdd` 输出是否新建 |
| `contains(key)` | 判断是否存在，等价 `get(key) != null` |

```java
OutObject<Boolean> isAdd = new OutObject<>();
Player player = table.getOrAdd(roleId, isAdd);
if (isAdd.value) {
    // 新建的玩家
    player.setName("newbie");
}
```

### 写入

| 方法 | 说明 |
|------|------|
| `put(key, value)` | 写入（覆盖） |
| `insert(key, value)` | 插入，**已存在抛 `IllegalArgumentException`** |
| `tryAdd(key, value)` | 尝试添加，返回是否成功（不抛异常） |

### 删除

| 方法 | 说明 |
|------|------|
| `remove(key)` | 标记删除，**提交后生效**，Checkpoint 时刷写 |

> `remove` 只是标记删除：当前事务内仍可见，提交后才真正生效。

```java
table.remove(roleId);   // 提交后该记录被删除
```

---

## 事务外读取

不在事务里又想读数据时，提供两种方式：

### selectCopy

返回数据的**深拷贝**，事务内外均可：

| 行为 | 说明 |
|------|------|
| 事务内访问过 | 返回最新值的拷贝 |
| 未访问过 | 后台加载后拷贝 |
| 不加入 RecordAccessed | 不影响事务冲突检测 |
| 不应修改 | 只读用途，搭 ReadOnly 接口更安全 |

### selectDirty

从本地缓存快速读取：

| 方法 | 说明 |
|------|------|
| `selectDirty(key)` | 默认 3 秒有效期 |
| `selectDirty(key, cacheTTL)` | 自定义 TTL；`0` 总是从数据库取 |

| 特性 | 说明 |
|------|------|
| 不经 GCM 协商 | 一致性较弱 |
| 适合 | `whileCommit` 预读、统计聚合等能容忍轻微陈旧的场景 |

```java
// 事务外快速读取（可能轻微陈旧）
PlayerData data = table.selectDirty(roleId);
```

---

## TableCache

每张表都有一个内存缓存 `TableCache`，结构如下：

| 组成 | 说明 |
|------|------|
| 主存储 | `ConcurrentHashMap` |
| 分段 LRU | 多个 LRU 段轮换，控制总量 |

### 关键参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `MAX_NODE_COUNT` | 8640 | 单段最大节点数，超过触发合并 |
| `SHRINK_NODE_COUNT` | 8000 | 合并后收缩到的大小 |
| `CacheInitialCapacity` | 31 | 缓存初始容量 |
| `RealCacheCapacity` | -1 | 不限（-1） |

### 工作机制

| 阶段 | 说明 |
|------|------|
| 创建热点段 | 定期创建新的 LRU 段，新记录插入当前段 |
| 合并 | 后台检查总节点数超过 `MAX_NODE_COUNT`，合并老段 |
| 回收 | 超过 `RealCacheCapacity` 时，从最老段逐条回收**非脏、非新鲜**记录 |

---

## 本地 RocksDB 缓存

每张表在本地维护一个 **localRocksCacheTable**，value 用 `SoftReference` 持有。

### 设计动机

| 要点 | 说明 |
|----|------|
| 让 `cache.capacity` 只影响 Record1 条目数 | **与 value 本身大小无关** |
| 默认容量 | `20000 × 5.0 = 100000`，基本不用调 |
| GC 回收后 | 从本地 RocksDB 恢复，避免回源数据库 |

### 第一原则：与后端数据库保持一致

| 操作 | 行为 |
|------|------|
| 写入 | 同一次 flush 调用中**同时写远程和本地** |
| 加载 | 远程加载后**回写本地**；远程没有时**本地也删** |
| 删除 | 同步删本地 |

### 分布式失效

当其他实例修改了某条记录，当前实例的缓存降级为 `Invalid`，下次访问时从远程重新加载，覆盖本地 RocksDB 后恢复。本地与远程的不变式始终成立。

---

## Storage

Storage 是 Table 与数据库之间的桥梁：

| 方法 / 属性 | 说明 |
|-------------|------|
| `open()` | 打开时创建内存表 |
| `isMemory()` | 是否纯内存表（返回 `boolean`）；判定内存表的标准是其 `getStorage()` 为 `null` |
| 脏标记与刷写 | 提交后标记脏记录 |

### 刷写模式

| 模式 | 说明 |
|------|------|
| `Immediately` | 提交后**立即写**数据库，并清除脏标记 |
| `Table`（按表批量 Checkpoint） | 遍历脏记录 → `encode0` 序列化 → `flush` 写库 → `cleanup` 清理 |

---

## Record 生命周期

```
Load  →  Access  →  Modify  →  Flush  →  Cleanup
```

| 状态 | 说明 |
|------|------|
| Load | 首次访问从数据库加载 |
| Access | 进入缓存，状态在 `Modify` / `Share` / `Invalid` 间通过 GCM 协商 |
| Modify | 被当前事务修改；提交时设 dirty、递增 timestamp |
| Flush | Checkpoint 时刷写到数据库 |
| Cleanup | 清理回收 |

---

## 遍历（事务外）

所有遍历方法均在事务外调用，回调返回 `false` 可提前中断。

### walk 系列（数据库 + 缓存合并）

| 方法 | 说明 |
|------|------|
| `walk(callback)` | 正序遍历，数据库 + 缓存合并 |
| `walkDesc(callback)` | 倒序遍历 |
| `walkKey(callback)` | 只遍历 key |
| `walk(exclusiveStartKey, proposeLimit, callback)` | 分页遍历 |

### walkDatabase 系列（直接遍历数据库）

| 方法 | 说明 |
|------|------|
| `walkDatabase(callback)` | 直接遍历数据库，**不经过缓存** |
| `walkDatabaseRaw(callback)` | 原始遍历 |

### walkMemory 系列（遍历内存缓存）

| 方法 | 说明 |
|------|------|
| `walkMemory(callback)` | 遍历内存缓存（StateShare / Modify 状态） |
| `walkCacheKey(callback)` | 遍历缓存中的 key |

```java
// 全表遍历（数据库 + 缓存合并，最完整）
table.walk((key, value) -> {
    System.out.println(key + ": " + value);
    return true;   // 返回 false 中断
});

// 分页遍历
table.walk(startKey, 100, (key, value) -> {
    process(value);
    return true;
});
```

---

## 函数式接口

| 接口 | 签名 | 说明 |
|------|------|------|
| `TableWalkHandle<K,V>` | `boolean handle(K key, V value)` | 遍历回调，返回 `false` 中断 |
| `TableWalkKey<K>` | `void handle(K key)` | 只遍历 key 的回调 |

---

## 只读接口：TableReadOnly

用于把表的只读视图暴露给其他模块，避免误写：

| 方法 | 说明 |
|------|------|
| `getReadOnly(key)` | 只读 get |
| `contains(key)` | 判断存在 |
| `selectCopy(key)` | 深拷贝 |
| `walk(...)` | 只读遍历 |

```java
// 只读视图通过生成代码获取（按表名拼出 getTableXxxReadOnly），而非 table.asReadOnly()
TableReadOnly<Long, Player, PlayerReadOnly> readOnly = getTabletPlayerReadOnly();
PlayerReadOnly view = readOnly.getReadOnly(roleId);
```

## 动态表：TableDynamic

运行时动态创建的表，复用母表的编解码逻辑：

| 方法 | 说明 |
|------|------|
| `zeze.openDynamicTable(...)` | 注册动态表 |
| `dropTable(name)` | 删除动态表 |

---

## solution.xml 声明

```xml
<!-- key 必须简单类型或 beankey（实现 Comparable），value 必须是 bean -->
<table name="tPlayer"  key="long"     value="Player"/>
<table name="tRoleServer" key="RoleServerKey" value="RoleServer"/>

<!-- 可选属性 -->
<table name="tMail"    key="long"     value="Mail"    suffix="_@ServerId"/>
<table name="tCounter" key="long"     value="Counter" memory="true"/>
<table name="tAuto"    key="long"     value="Auto"    autokey="true"/>
```

| 可选属性 | 说明 |
|----------|------|
| `suffix` | 表名后缀模板（`@AppMainVersion` / `@ServerId`） |
| `memory` | 纯内存表 |
| `autokey` | 自动键，取值 `"true"`（仅 long key）或 `"random"`（仅 binary key）；**没有 `autoIncrement` 属性** |

## 相关文档

- [Bean 数据模型](./bean.md) — Table value 的结构与版本兼容
- [solution.xml 参考](./solution-xml.md) — `<table>` 完整属性
- [事务系统](./transaction.md) — CRUD 必须在事务内
- [数据库概览](./db-overview.md) — Storage 与不同数据库后端
