---
title: "CHashMap 并发 Map"
description: "Zeze.Collections.CHashMap 基于取模分桶的并发 Map 的初始化与适用场景"
category: reference
order: 43
---

> 本文档说明 `Zeze.Collections.CHashMap` 并发 Map 的定位、初始化与适用场景。

## 模块定位

`Zeze.Collections.CHashMap` 是**按 key 哈希值取模分桶**实现的并发 Map，把数据分散到多个底层 `LinkedMap`（桶）上以降低锁冲突，适用于**高并发随机读写**场景。

| 特性 | 说明 |
|------|------|
| **取模分桶** | `hash(key) % concurrencyLevel` 选桶（非一致性哈希环） |
| **高并发随机读写** | 面向大量 key 的并发 put/get 优化，不同桶可并行 |
| **依赖 LinkedMap.Module** | **没有独立 Module**，由 `LinkedMap.Module.openConcurrent(...)` 创建 |
| **事务安全** | 基于内部 Table，享受乐观锁与自动持久化 |

> ⚠️ **注意**：CHashMap **不是一致性哈希环**，桶数量（`concurrencyLevel`）在创建时固定，扩缩容需要重建整个 CHashMap。如果需要一致性哈希语义，请另寻方案。

## 适用场景

- 高并发下对大量 key 的随机读写（热点分散到不同桶）
- 需要按 key 哈希定位、低冲突的键值存储

## 初始化

CHashMap **没有独立的 Module**，必须先注册 `LinkedMap.Module`，再通过它的 `openConcurrent(...)` 创建：

```java
// 1. 先注册 LinkedMap Module
var linkedMapModule = new LinkedMap.Module(zeze);

// 2. 通过 LinkedMap.Module.openConcurrent 打开一个 CHashMap（名称不能包含 @）
CHashMap<Value> shardMap = linkedMapModule.openConcurrent("shardMap", Value.class);
//   默认 concurrencyLevel=128, nodeSize=30
// 另有重载：openConcurrent(name, valueClass, nodeSize)（默认 concurrencyLevel=256）
```

> 值类型必须继承 `Zeze.Transaction.Bean` 并经 `BeanFactory` 注册。

## 共同约束

| 约束 | 说明 |
|------|------|
| **必须在 Procedure 中操作** | 所有读写必须在事务内 |
| **名称不能含 `@`** | `@` 为内部保留分隔符（CHashMap 内部用 `name + "@" + 桶号` 拼出各桶的 LinkedMap 名） |
| **值必须继承 Bean** | 并经 `BeanFactory` 注册 |
| **支持值类型热重载** | 通过 `HotBeanFactory` 接口 |

## 相关文档

- 持久化集合总览：[./coll-overview.md](./coll-overview.md)
- LinkedMap 有序映射：[./coll-linked-map.md](./coll-linked-map.md)
- Table 抽象层：[./table.md](./table.md)


## 共同约束

| 约束 | 说明 |
|------|------|
| **必须在 Procedure 中操作** | 所有读写必须在事务内 |
| **名称不能含 `@`** | `@` 为内部保留分隔符 |
| **值必须继承 Bean** | 并经 `BeanFactory` 注册 |
| **支持值类型热重载** | 通过 `HotBeanFactory` 接口 |

## 相关文档

- 持久化集合总览：[./coll-overview.md](./coll-overview.md)
- Table 抽象层：[./table.md](./table.md)
