---
title: "排行榜"
sidebar:
  order: 2
---

Rank 模块（`Zeze.Game.Rank`）为游戏提供高性能的分布式排行榜系统。它通过 **ConcurrentLevel** 并发分区实现多服务器并行更新，使用多路归并算法聚合查询结果，支持多种时间维度和自定义排行榜。

## 核心概念

### ConcurrentLevel -- 并发分区

**ConcurrentLevel** 是排行榜最关键的配置参数（默认 128）。排行榜数据按照 `hash(roleId) % ConcurrentLevel` 分散到多个分区中存储和更新。分区数量决定了最大并发度，修改此参数会导致旧数据全部失效并需要重建。

```java
rank.setFuncConcurrentLevel(rankType -> 256);
```

### RankSize 和 ComputeCount

- **RankSize**（默认 100）：最终排行榜展示的条目数量。
- **ComputeCount**（默认 `RankSize * computeFactor`，factor 默认 2.5）：中间计算数据保留的条目数量，大于 RankSize 以保证合并后结果的准确性。

### BConcurrentKey -- 排行榜键

每个排行榜实例由 **BConcurrentKey** 唯一标识，包含以下字段：

| 字段 | 含义 |
|------|------|
| `rankType` | 排行榜类型标识 |
| `concurrentIndex` | 并发分区索引（内部计算） |
| `timeType` | 时间维度 |
| `year` | 年份 |
| `offset` | 时间偏移（天/周/季）或自定义 ID |

### 时间维度

Rank 内置了以下时间类型（`BConcurrentKey.TimeType*`）：

- **Total**：全量排行榜，不区分时间。
- **Day**：日排行，按年内第几天分。
- **Week**：周排行，按年内第几周分。
- **Season**：季排行，按春夏秋冬分。
- **Year**：年排行，按年份分。
- **Customize**：自定义排行榜，使用 `customizeId` 标识。

```java
// 创建日排行榜 key
BConcurrentKey key = Rank.newRankKey(rankType, BConcurrentKey.TimeTypeDay);

// 创建自定义排行榜 key
BConcurrentKey key = Rank.newRankKey(rankType, customId);
```

## API 详解

### 创建和初始化

```java
Rank rank = Rank.create(app);
rank.setFuncRankSize(rankType -> 200);
rank.setFuncConcurrentLevel(rankType -> 256);
rank.setFuncRankCacheTimeout(rankType -> 10 * 60 * 1000); // 10分钟缓存
```

### updateRank -- 更新排行

```java
@RedirectHash(ConcurrentLevelSource = "getConcurrentLevel(keyHint.getRankType())")
public RedirectFuture<Long> updateRank(int hash, BConcurrentKey keyHint, long roleId, Bean value);
```

`updateRank` 使用 **@RedirectHash** 注解将请求路由到正确的并发分区。执行流程：

1. 先移除该 `roleId` 在当前分区的旧记录。
2. 使用 `compactor` 比较器在有序列表中找到合适位置插入。
3. 如果列表超过 `ComputeCount`，移除末尾多余记录。

默认比较器 `LongOnlyCompactor` 按 `BValueLong` 的值降序排列。可以通过 `setCompactor` 自定义排序逻辑。

### removeRank -- 移除排行

```java
public RedirectFuture<Long> removeRank(int hash, BConcurrentKey keyHint, long roleId);
```

从指定分区中移除某个角色的排行记录。

### getRankTotal -- 查询排行榜

```java
RankTotal total = rank.getRankTotal(keyHint);       // 返回 RankSize 条
RankTotal total = rank.getRankTotal(keyHint, 500);   // 返回 500 条
```

查询流程：

1. 检查缓存是否有效（`rankCacheTimeout`，默认 5 分钟）。
2. 缓存失效则调用 `getRankDirect` 重新构建。
3. `getRankDirect` 遍历所有并发分区，使用多路归并算法合并排序。

### getRankPosition -- 查询排名位置

```java
long position = rank.getRankPosition(keyHint, roleId);  // 精确排名
long position = rank.getRankPositionWithGuess(keyHint, roleId, score, totalUser);  // 估算排名
```

`getRankPosition` 返回精确排名（在榜内时）或 -1（未入榜）。`getRankPositionWithGuess` 在未入榜时根据分数和总人数估算大致排名。

### deleteRank -- 删除排行榜

```java
rank.deleteRank(keyHint);  // 删除所有并发分区的数据
```

### mergeRank -- 合并排行榜

```java
rank.mergeRank(keyHintFrom, keyHintTo);  // 将 from 合并到 to
```

直接合并两个排行榜的所有分区数据。合并后保留 `ComputeCount` 条记录。

## 注册自定义 Bean 类型

排行榜的值类型通过 **BeanFactory** 管理。自定义值类型需要在启动时注册：

```java
Rank.register(MyCustomRankValue.class);
```

注册的 Bean 类型会持久化其类名，系统在反序列化时通过 `beanFactory` 自动还原。

## 分布式原理

Rank 模块利用 Zeze 的 **Redirect** 机制实现分布式：

- `updateRank` 和 `removeRank` 使用 `@RedirectHash` 根据 `roleId` 的哈希值将请求路由到目标服务器。
- 查询操作（`getRankTotal`）在任意服务器上执行，直接读取本地数据库并合并所有分区数据。
- 排行榜数据通过 Zeze 的缓存一致性协议在集群间同步。

## 事务和性能

- 更新操作在 Zeze 事务内执行，乐观锁保证并发安全。
- 每个并发分区是一个独立的记录，不同分区的更新互不冲突。
- 查询结果有缓存机制，避免频繁的全量合并计算。
- 适当增大 `ComputeFactor` 可以提高合并后的结果精度，但会占用更多存储空间。
