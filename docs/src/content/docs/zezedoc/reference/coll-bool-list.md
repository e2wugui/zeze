---
title: "BoolList 位图列表"
description: "Zeze.Collections.BoolList 位图列表的初始化与适用场景"
category: reference
order: 45
---

> 本文档说明 `Zeze.Collections.BoolList` 位图列表的定位、初始化与适用场景。

## 模块定位

`Zeze.Collections.BoolList` 是持久化的**位图列表**，用位（bit）表示布尔标记，适用于标记位、开关集合等需要高效位操作的场景。

| 特性 | 说明 |
|------|------|
| **位图存储** | 以 bit 为单位，存储紧凑 |
| **高效位操作** | 提供位的设置、清除、查询 |
| **事务安全** | 基于内部 Table，享受乐观锁与自动持久化 |

## 适用场景

- 标记位集合（功能开关、状态标记）
- 大量布尔状态的紧凑存储
- 成员资格标记、成就点亮等位图语义数据

## 初始化

```java
// 1. 注册 Module 到 Application
var boolListModule = new BoolList.Module(zeze);

// 2. 打用具名实例：单参数，仅传 name（不能为空、不能包含 @）
BoolList flags = boolListModule.open("featureFlags");
```

> `open` 是**单参数**方法，只接收 name，不需要额外参数。name 为空或含 `@` 会抛 `IllegalArgumentException`。

## 提供的位操作 API

BoolList 提供位级别的设置、清除、查询能力：

| 方法 | 说明 |
|------|------|
| `set(int index)` | 将第 `index` 位置 1 |
| `clear(int index)` | 将第 `index` 位置 0 |
| `get(int index)` | 查询第 `index` 位是否为 1 |
| `clearAll()` | 清空所有位 |

## 共同约束

| 约束 | 说明 |
|------|------|
| **必须在 Procedure 中操作** | 所有读写必须在事务内 |
| **名称不能含 `@`** | `@` 为内部保留分隔符 |

## 相关文档

- 持久化集合总览：[./coll-overview.md](./coll-overview.md)
- Table 抽象层：[./table.md](./table.md)
