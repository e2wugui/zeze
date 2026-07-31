---
title: "Rank 排行榜"
description: "Zeze.Game.Rank 高性能分布式排行榜——并发分区、一致性哈希路由与多路归并聚合"
category: reference
order: 52
---

> 本文档描述 `Zeze.Game.Rank` / `AbstractRank` 的并发分区原理、`@RedirectHash` 路由、多路归并聚合查询与即时排行榜用法，供排行榜业务开发检索参考。

## 模块定位

`Zeze.Game.Rank` 是高性能**分布式排行榜**，通过 **`ConcurrentLevel` 并发分区**与 **`RedirectHash` 一致性路由**实现多服务器并行更新，使用**多路归并**算法聚合查询结果。

| 核心特性 | 说明 |
|----------|------|
| **并发分区** | `ConcurrentLevel` 决定并发度（如 128），角色 hash 分散到分组 |
| **一致性路由** | `@RedirectHash` 按 hash 路由到一台 Server，同 hash 串行 |
| **多路归并** | 全局查询时整合所有分组，归并排序取 TopN |
| **即时更新** | 角色数值变化马上更新，适合即时排行榜 |
| **时间维度** | 支持总榜/日榜/周榜/季榜/年榜/自定义 |

---

## 并发分区原理

### 问题：单点瓶颈

全局单点排行榜无法提供足够并发：所有更新请求排队互斥，在线角色多、更新频繁时成为瓶颈。

### 解法：分组存储

| 策略 | 说明 |
|------|------|
| 分组存储 | 角色 hash 分散到多组，每组独立排名并保存足够数量 |
| 全局汇总 | 需要全局排名时，整合所有分组 |
| 并发提升 | 并发量随 `ConcurrentLevel` 线性增长（如 128 组 → 并发增 128 倍） |

### ConcurrentLevel（并发度）

```java
// 决定最大并发度，【非常重要】
public final int getConcurrentLevel(int rankType) {
    var f = funcConcurrentLevel;
    return f != null ? f.applyAsInt(rankType) : 128; // 默认 128
}

rank.setFuncConcurrentLevel(rankType -> 128);
```

| 要点 | 说明 |
|------|------|
| 决定最大并发度 | 由 `ConcurrentLevelSource` 在 `@RedirectHash` 中给出 |
| 留有余地 | 一般设足够大（默认 128） |
| **不可随意改** | 改变分组参数会导致**分组数据全部失效**，需清除重建 |
| 分组数 = 最大服务器数 | 分组数即为最大可扩展服务器数 |

---

## 存储结构

排行榜由一张表 `trank` 存储，key 为 `BConcurrentKey`，value 为 `BRankList`。

### BConcurrentKey（分区键）

| 字段 | 说明 |
|------|------|
| `RankType` | 排行榜类型 |
| `ConcurrentId` | `= hash % ConcurrentLevel`，即分组编号 |
| `TimeType` | 时间维度 |
| `Year` | 年份 |
| `Offset` | 时间偏移（按 TimeType 含义不同） |

### TimeType 时间维度

| 常量 | 值 | 含义 |
|------|----|------|
| `TimeTypeTotal` | 0 | 总榜（Year=0，Offset=0） |
| `TimeTypeDay` | 1 | 每日榜（Offset=一年中的第几天） |
| `TimeTypeWeek` | 2 | 每周榜（Offset=周数） |
| `TimeTypeSeason` | 3 | 每季榜（Offset=季节，中文季节） |
| `TimeTypeYear` | 4 | 每年榜（Offset=0） |
| `TimeTypeCustomize` | 5 | 自定义（Offset=自定义 Id，Year=0） |

### 创建 RankKey

```java
// 总榜
BConcurrentKey key = Rank.newRankKey(rankType, BConcurrentKey.TimeTypeTotal);

// 日榜（自动取当前时间）
BConcurrentKey key = Rank.newRankKey(rankType, BConcurrentKey.TimeTypeDay);

// 自定义 Id 榜（如帮派榜）
BConcurrentKey key = Rank.newRankKey(rankType, guildId);
```

---

## 创建与启动

```java
// 创建（通过 GenModule 创建支持 Redirect 的实例）
Rank rank = Rank.create(app);

// 启动：向 ServiceManager 注册服务
rank.Start(serviceNamePrefix, providerDirectIp, providerDirectPort);
```

---

## 更新排行榜

`updateRank` 使用 `@RedirectHash` 按 hash 路由到目标 Server：

```java
@RedirectHash(ConcurrentLevelSource = "getConcurrentLevel(keyHint.getRankType())")
public RedirectFuture<Long> updateRank(int hash, BConcurrentKey keyHint, long roleId, Bean value);
```

| 参数 | 说明 |
|------|------|
| `hash` | 角色 hash，框架据此选 Server 与分组 |
| `keyHint` | 排行榜分区键（`BConcurrentKey`） |
| `roleId` | 角色 id |
| `value` | 排序值 Bean（如 `BValueLong`） |

### 调用方式

```java
// 角色战力变化，立即更新
var key = Rank.newRankKey(rankType, BConcurrentKey.TimeTypeTotal);
var future = rank.updateRank(roleIdHash, key, roleId, new BValueLong(combatPower));
```

### 更新逻辑

| 步骤 | 行为 |
|------|------|
| 1. 路由分组 | `ConcurrentId = hash % ConcurrentLevel` 定位分组 |
| 2. 先删旧值 | 删除该 roleId 的旧记录（若有） |
| 3. 插入新值 | 按比较器插入有序位置，超过 `ComputeCount` 截断 |
| 4. 自动注册 | 调用 `beanFactory.register(value)` 注册值类型 |

> `oneByOne=true`（默认）：同一 hash 的请求排队串行，避免并发冲突。

---

## 查询排行榜

### getRankTotal —— 带缓存的全局查询

```java
// 查询全局榜（带缓存，默认缓存 5 分钟）
RankTotal total = rank.getRankTotal(keyHint);
BRankList list = total.getTableValue();

// 查询角色名次（返回 -1 表示未上榜）
long position = rank.getRankPosition(keyHint, roleId);
```

| 方法 | 说明 |
|------|------|
| `getRankTotal(keyHint)` | 带缓存查询，缓存过期重新构建 |
| `getRankTotal(keyHint, countNeed)` | 指定需要的数量 |
| `getRankPosition(keyHint, roleId)` | 查角色名次，未上榜返回 `-1` |
| `getRankPositionWithGuess(...)` | 未上榜时按分数估算名次（仅 `BValueLong`） |

### getRankDirect —— 直接合并（不走缓存）

```java
// 直接查数据库并合并所有分组，不使用缓存
BRankList list = rank.getRankDirect(keyHint);
```

### 多路归并算法

全局查询时，遍历所有 `ConcurrentId`（0 ~ ConcurrentLevel-1），取出各分组 `BRankList`，归并排序后取 TopN：

```
分组0 ──┐
分组1 ──┼──▶ 多路归并（merge）──▶ TopN 全局榜
 ...    │
分组N ──┘
```

| 步骤 | 说明 |
|------|------|
| 取各分组 | 遍历 `ConcurrentLevel` 个分组，`_trank.getOrAdd(concurrentKey)` |
| 两两归并 | `merge(left, right)` 按比较器合并有序列表 |
| 截断 | 中间结果超过 `ComputeCount` 即删除尾部，减少计算量 |

---

## 参数配置

| 配置方法 | 默认值 | 说明 |
|----------|--------|------|
| `setFuncRankSize(fn)` | `100` | 每个排行榜保留的数量 |
| `setFuncConcurrentLevel(fn)` | `128` | 最大并发度（**不可随意改**） |
| `setFuncRankCacheTimeout(fn)` | `5 * 60 * 1000`（5 分钟） | 全局榜缓存超时 |
| `setComputeFactor(factor)` | `2.5` | 中间数据倍数（最小 2） |
| `setCompactor(comparator)` | `LongOnlyCompactor` | 排序比较器 |

### ComputeCount（中间数据数量）

```java
// 中间分组保存的数量 = RankSize * ComputeFactor（最少 2 倍）
public final int getComputeCount(int rankType) {
    float factor = Math.max(computeFactor, 2);
    return (int)(getRankSize(rankType) * factor);
}
```

> 每个分组保存 `ComputeCount`（比 `RankSize` 多）条记录，保证归并后能凑出足够的全局 TopN。

### 比较器

默认 `LongOnlyCompactor` 仅支持 `BValueLong`：

```java
// 自定义比较器（支持复杂排序值 Bean）
rank.setCompactor((o1, o2) -> {
    // 返回正数表示 o1 排在 o2 之后（降序榜）
});
```

---

## 管理操作

### 删除角色榜记录

```java
// 从排行榜中删除某角色
var future = rank.removeRank(roleIdHash, keyHint, roleId);
```

### 删除整个排行榜

```java
// 清除某排行榜的所有分组数据
rank.deleteRank(keyHint);
```

`deleteRank` 遍历所有 `ConcurrentId`，逐个 `_trank.remove`。

### 合并排行榜

```java
// 把一个时间榜合并到另一个（如日榜合并到周榜）
rank.mergeRank(keyHintFrom, keyHintTo);
```

| 约束 | 说明 |
|------|------|
| 类型须一致 | `RankType` / `TimeType` / `Year` 必须相同 |
| 同榜跳过 | `Offset` 相同则直接返回 |
| 合并后截断 | 超过 `ComputeCount` 删除尾部 |

---

## 单角色更新：@RedirectHash

`updateRank` / `removeRank` 使用 `@RedirectHash`，按角色 hash 路由到**一台** Server 执行：

```java
@RedirectHash(ConcurrentLevelSource = "getConcurrentLevel(keyHint.getRankType())")
public RedirectFuture<Long> updateRank(int hash, BConcurrentKey keyHint, long roleId, Bean value)
// 返回 RedirectFuture<Long>
```

| 注解 | 用途 |
|------|------|
| `@RedirectHash` | 更新/删除单角色，按 hash 路由到一台 Server |

### 全局查询：本地遍历归并（非 @RedirectAll）

> ⚠️ **注意**：Rank 的全局查询**没有使用 `@RedirectAll`**。它走的是 `getRankDirect` —— 在**当前这一台服务器本地**遍历其共享库范围内的排行榜，然后归并结果。也就是说，所谓「全局榜」是基于单机数据 + 归并的，而不是广播所有分组做 MapReduce。如果你的榜数据分散在多台 Server，需要自行在业务层聚合多台的 `getRankDirect` 结果。

> Redirect 机制详见 [Redirect 跨服调用](./arch-redirect.md)。

---

## 适用场景

| 场景 | 是否适用 | 说明 |
|------|----------|------|
| **实时排行榜** | ✅ 适用 | 角色数值变化马上更新；查询侧 `getRankTotal` 默认有约 5 分钟缓存，并非严格即时 |
| 战力榜 / 等级榜 | ✅ 适用 | `BValueLong` 即可 |
| 帮派榜 | ✅ 适用 | 用 `TimeTypeCustomize` + 帮派 Id |
| 日/周/赛季榜 | ✅ 适用 | TimeType 自动按时间分区 |
| 历史快照榜 | ⚠️ 可用 `mergeRank` | 把时间榜合并归档 |

---

## 注意事项

| 注意点 | 说明 |
|--------|------|
| **ConcurrentLevel 不可随意改** | 改变会导致分组数据全部失效，需清除重建 |
| **值类型需注册** | `updateRank` 时自动注册，自定义比较器需类型匹配 |
| **估算名次仅支持 Long** | `getRankPositionWithGuess` 仅 `BValueLong` |
| **缓存一致性** | `getRankTotal` 带缓存，过期重建；需实时用 `getRankDirect` |
| **服务器数 ≤ 分组数** | 初期服务器数一般小于分组数，每台处理多分组 |

---

## 相关文档

- [游戏模块总览](./game-overview.md) — Game 模块整体架构
- [Redirect 跨服调用](./arch-redirect.md) — `@RedirectHash` / `@RedirectAll` 注解详解
- [全球同服](./arch-one-world.md) — 分组拆分策略与负载分配
