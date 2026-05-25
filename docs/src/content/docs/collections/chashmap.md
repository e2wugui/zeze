---
title: "CHashMap 一致性哈希"
sidebar:
  order: 5
---

`CHashMap` 是基于**一致性哈希**的并发 Map 实现。它将 key 哈希到多个 [LinkedMap](./linked-map) 桶中，通过降低单桶冲突来提升并发性能。所有操作在事务中执行，数据自动持久化。

## 包路径

```
Zeze.Collections.CHashMap<V extends Bean>
```

## 原理

CHashMap 内部维护一个 `LinkedMap` 数组作为桶（bucket）。写入时根据 key 的哈希值取模选择桶，不同 key 分布到不同桶中，从而减少事务锁冲突，提升并发吞吐量。`size` 的维护在事务提交后异步更新，避免锁住所有桶。

## 快速开始

```java
Zeze.Application zeze = new Zeze.Application(config);
LinkedMap.Module lmModule = new LinkedMap.Module(zeze);

// 打开 CHashMap，默认并发级别 128，节点大小 30
CHashMap<PlayerData> playerMap = lmModule.openConcurrent("players", PlayerData.class);

// 指定节点大小
CHashMap<PlayerData> playerMap = lmModule.openConcurrent("players", PlayerData.class, 50);
```

## API 参考

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `get(String key)` | `V / null` | 根据 key 获取值 |
| `getOrAdd(String key)` | `V` | 获取或创建值 |
| `put(String key, V value)` | `V / null` | 插入或更新 |
| `remove(String key)` | `V / null` | 删除并返回 |
| `size()` | `long` | 元素总数（近似值，异步更新） |
| `isEmpty()` | `boolean` | 是否为空 |
| `getName()` | `String` | 获取名称 |

## 使用示例

```java
CHashMap<PlayerData> playerMap = lmModule.openConcurrent("players", PlayerData.class);

zeze.newProcedure(() -> {
    // 插入
    PlayerData data = new PlayerData();
    data.name = "Alice";
    data.level = 10;
    playerMap.put("player_001", data);

    // 获取
    PlayerData p = playerMap.get("player_001");

    // 获取或创建
    PlayerData p2 = playerMap.getOrAdd("player_002");

    // 删除
    playerMap.remove("player_001");

    return 0;
}, "chashmap_ops").call();
```

## 内部实现

CHashMap 不直接操作 Table，而是基于 [LinkedMap](./linked-map) 构建。每个桶是一个 `LinkedMap` 实例，名称格式为 `"{name}@{bucketIndex}"`。`size` 数组在事务提交后通过 `Transaction.whileCommit` 回调更新。

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `concurrencyLevel` | 128/256 | 桶数量，决定并发度 |
| `nodeSize` | 30 | 每个 LinkedMap 节点的元素上限 |

## 注意事项

1. **通过 LinkedMap.Module 打开** -- CHashMap 使用 `LinkedMap.Module.openConcurrent()` 创建，没有独立的 Module
2. **名称限制** -- 名称不能包含 `@` 字符
3. **size 近似** -- `size()` 返回的是异步更新的近似值，在事务提交后生效
4. **并发级别固定** -- 桶数量在创建时确定，不可动态调整
5. **不支持遍历** -- CHashMap 没有提供全量遍历方法
