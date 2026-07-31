---
title: "Queue 持久队列"
description: "Zeze.Collections.Queue 持久化 FIFO 队列与 LIFO 栈的初始化、操作与内部结构"
category: reference
order: 41
---

> 本文档说明 `Zeze.Collections.Queue<V extends Bean>` 持久化队列的初始化、FIFO/LIFO 双模式操作、内部链表结构及与 LinkedMap 的差异。

## 模块定位

`Zeze.Collections.Queue<V extends Bean>` 是持久化队列，支持 **FIFO（队列）** 与 **LIFO（栈）** 两种模式。所有操作在事务中执行，自动持久化。

| 核心特性 | 说明 |
|----------|------|
| **双模式** | 同一实例可按 Queue（FIFO）或 Stack（LIFO）使用 |
| **持久化** | 基于内部 Table，自动同步到数据库 |
| **事务安全** | 乐观锁，操作原子可重入 |
| **热更新** | 值类型支持热重载 |
| **节点分块** | 大数据量分多节点存储，避免单记录过大 |
| **时间戳** | 每个元素自动记录添加时间 |

## 快速开始

```java
// 1. 注册 Module
var queueModule = new Queue.Module(zeze);

// 2. 打开具名实例（默认每节点 30 个元素）
Queue<Task> taskQueue = queueModule.open("taskQueue", Task.class);

// 3. 自定义节点容量
Queue<Task> bigQueue = queueModule.open("bigQueue", Task.class, 100);
```

## Module 方法

| 方法 | 说明 |
|------|------|
| `open(name, class)` | 打开具名队列，默认节点容量 |
| `open(name, class, nodeSize)` | 打开具名队列，指定每节点元素数 |
| `openCsQueue(name, class)` | 打开**跨服务器**队列，默认节点容量 |
| `openCsQueue(name, class, nodeSize)` | 打开跨服务器队列，指定节点容量 |

> **CsQueue（跨服务器队列）** 支持多服务器协同消费，适用于分布式任务分发场景。

## 基本操作

### FIFO 模式（队列）

| 操作 | 说明 |
|------|------|
| `add(value)` | 加入队尾 |
| `poll()` | 取出并删除队首元素，空返回 `null` |
| `peek()` | 查看队首元素（不删除），空返回 `null` |

```java
queue.add(task1);      // 队尾入队
queue.add(task2);
Task head = queue.poll();  // 取队首 task1，删除
Task next = queue.peek();  // 看 task2，不删
```

### LIFO 模式（栈）

| 操作 | 说明 |
|------|------|
| `push(value)` | 压入栈顶 |
| `pop()` | 弹出栈顶元素，空返回 `null` |

```java
queue.push(task1);    // 栈顶
queue.push(task2);
Task top = queue.pop();   // 弹 task2
```

> `pop` 内部即 `poll`，二者可混合使用：`add`/`poll` 与 `push`/`pop` 在同一实例上可交替调用。

### 通用操作

| 操作 | 说明 |
|------|------|
| `size()` | 元素总数 |
| `isEmpty()` | 是否为空 |
| `pollNode()` | 取出一个节点（含多个值） |
| `peekNode()` | 查看一个节点（不删除） |
| `walk(func)` | 只读遍历 |
| `clear()` | 清空 |

### walk 遍历

`walk(TableWalkHandle<BQueueNodeKey, V> func)` 以**只读快照**方式遍历，内部使用 `selectDirty` 读取，**不持锁**，适合统计、转储等场景。

```java
queue.walk((nodeKey, value) -> {
    // 处理每个元素
    return true; // 返回 false 提前终止
});
```

## 内部数据结构

Queue 内部由两层 Bean 构成，分别存储队列元信息与节点数据。

| 结构 | 字段 | 说明 |
|------|------|------|
| **BQueue**（队列根） | `headNodeKey` | 首节点 Key |
| | `tailNodeKey` | 尾节点 Key |
| | `lastNodeId` | 最后分配的节点 ID |
| | `count` | 元素总数 |
| **BQueueNode**（节点） | `nextNodeKey` | 单向链表下一节点 |
| | `values[]` | 本节点存储的多个值 |
| **BQueueNodeValue**（节点值） | `timestamp` | 元素添加时间 |
| | `value` | `DynamicBean` 包装的实际值 |

### 存储表

| 表 | Key | Value |
|----|-----|-------|
| `_tQueues` | `name` | `BQueue`（队列根） |
| `_tQueueNodes` | `name + nodeId` | `BQueueNode`（节点） |

### 链表结构

```
HeadNode ──▶ Node1 ──▶ Node2 ──▶ ... ──▶ TailNode
              │          │                   │
           values[]    values[]           values[]
          (≤nodeSize) (≤nodeSize)        (≤nodeSize)
```

每个节点可存放多个 value（由 `nodeSize` 决定），节点间以单向链表串联。

## Queue vs LinkedMap

| 维度 | Queue | LinkedMap |
|------|-------|-----------|
| **访问方式** | 只能访问队首 / 队尾 | 可通过 key 随机访问 |
| **顺序模型** | FIFO 或 LIFO | 插入顺序，元素可在链表中移动 |
| **删除** | 只能删队首 | 可删除任意元素 |
| **典型场景** | 消息队列、任务栈 | 背包、排行榜 |

详见 [./coll-linked-map.md](./coll-linked-map.md)。

## 注意事项

| 注意点 | 说明 |
|--------|------|
| **必须在 Procedure 中操作** | 所有读写必须在事务内 |
| **名称不能含 `@`** | `@` 为内部保留分隔符 |
| **值必须继承 Bean** | 并经 `BeanFactory` 注册 |
| **nodeSize 仅影响新节点** | 已存在的旧节点容量不变 |
| **可混合 FIFO/LIFO** | `add`/`poll` 与 `push`/`pop` 可交替使用 |
| **自动时间戳** | 每个元素自动记录添加时间，无需业务自行维护 |

## 相关文档

- 持久化集合总览：[./coll-overview.md](./coll-overview.md)
- LinkedMap 有序映射：[./coll-linked-map.md](./coll-linked-map.md)
- Table 抽象层：[./table.md](./table.md)
