---
title: "DAG 有向无环图"
description: "Zeze.Collections.DAG 有向无环图的初始化与适用场景"
category: reference
order: 46
---

> 本文档说明 `Zeze.Collections.DAG` 有向无环图的定位、初始化与适用场景。

## 模块定位

`Zeze.Collections.DAG` 是持久化的**有向无环图（Directed Acyclic Graph）**，适用于依赖管理、任务编排等需要表达节点间先后依赖关系的场景。

| 特性 | 说明 |
|------|------|
| **节点 / 边** | 可添加节点（带值）和有向依赖边 |
| **有效性检查** | `checkValid()` 校验当前结构是否有效 |
| **事务安全** | 基于内部 Table，享受乐观锁与自动持久化 |

> ⚠️ **重要限制**：当前实现的**环检测尚未生效**（内部 `isValid()` 恒返回 `true`，留有 TODO），`addEdge` **不会自动拒绝成环**。请不要依赖框架自动防环，业务侧需自行保证不构成环。

## 适用场景

- 依赖管理（任务的前置依赖、模块构建依赖）
- 任务编排（工作流、流水线）
- 技能/科技树等有向无环层级关系（需自行保证无环）

## 初始化

```java
// 1. 注册 Module 到 Application
var dagModule = new DAG.Module(zeze);

// 2. 打用具名实例（名称不能包含 @）
DAG workflow = dagModule.open("buildPipeline", Node.class);
```

> 值类型必须继承 `Zeze.Transaction.Bean` 并经 `BeanFactory` 注册。

## 提供的 API

DAG 目前只提供节点与边的添加和基本查询能力（无删除、无拓扑排序）：

| 方法 | 说明 |
|------|------|
| `addNode(long id, V value)` | 添加节点（带值） |
| `addEdge(long from, long to)` | 添加有向依赖边 `from → to` |
| `checkValid()` | 校验图是否有效（当前实现恒通过，见上方限制说明） |
| `isEmpty()` | 图中是否无节点 |

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
