---
title: "LinkedMap 有序映射"
description: "Zeze.Collections.LinkedMap 基于记录的有序双向链表映射的初始化、访问模型与 DelayRemove 机制"
category: reference
order: 42
---

> 本文档说明 `Zeze.Collections.LinkedMap` 基于记录的有序双向链表映射的初始化、访问模型、遍历语义与 DelayRemove 延迟删除机制。

## 模块定位

`LinkedMap` 是**基于记录的有序双向链表映射**。它用记录存储 Node，每个 Node 记录前驱/后继 Node 的 Key，每个 Node 内存放一定数量的 Item，因此可以承载非常大的 Item 量。

| 特性 | 说明 |
|------|------|
| **有序双向链表** | 元素按插入顺序排列，可在链表中移动 |
| **按记录分块** | 每个 Node 存一组 Item，支持海量数据 |
| **可随机访问** | 通过 key 直接定位元素 |
| **FIFO / LIFO** | 支持按插入顺序从头/尾访问 |

### 访问方式

| 方式 | 说明 |
|------|------|
| **按 key 随机访问** | 通过业务 key 直接定位 Item |
| **按插入顺序遍历** | FIFO（从头）或 LIFO（从尾） |
| **可移动** | 元素可在链表中重新排序 |

## 快速开始

```java
// 1. 注册 Module
var linkedMapModule = new LinkedMap.Module(zeze);

// 2. 打用具名实例，指定值类型
LinkedMap<Item> myMap = linkedMapModule.open("myMap", Item.class);

// DepartmentTree 复用同一个 linkedMapModule
var departmentTreeModule = new DepartmentTree.Module(zeze, linkedMapModule);
```

## 遍历与 DelayRemove

由于数据分散在多个记录（Node）中，**遍历不能在一个事务里访问所有记录**，而须挨个处理：取当前 Node 的下一个，处理完再取下下一个。

这种"边遍历边删除"会引发一个并发问题：

> 当下一个记录刚被删除时，当前 Node 的后继指针仍指向那个已被删的记录。立即删除当前 Node、再获取下一个时就会得到 `null`，导致链表断裂、元素丢失。

**DelayRemove（延迟删除）** 机制解决此问题：删除一个 Node 时，**先更新前驱/后继索引**，经过**一定时间后才真正物理删除**记录，保证遍历者仍能顺着旧索引走到链表末端。

| 阶段 | 行为 |
|------|------|
| 逻辑删除 | 先更新前后 Node 的索引，把目标 Node 从链表摘除 |
| 延迟物理删除 | 一定时间后才真正删除该 Node 记录 |
| 效果 | 遍历者可安全沿旧索引走完链表，不丢元素 |

DelayRemove 的完整说明见 [./svc-delay-remove.md](./svc-delay-remove.md)。

## 与 Queue 的差异

| 维度 | LinkedMap | Queue |
|------|-----------|-------|
| **随机访问** | 可按 key 访问 | 只能队首/队尾 |
| **顺序** | 插入顺序，可移动 | FIFO / LIFO |
| **删除** | 可删任意元素 | 只能删队首 |
| **典型场景** | 背包、排行榜、有序列表 | 消息队列、任务栈 |

详见 [./coll-queue.md](./coll-queue.md)。

## 共同约束

| 约束 | 说明 |
|------|------|
| **必须在 Procedure 中操作** | 所有读写必须在事务内 |
| **名称不能含 `@`** | `@` 为内部保留分隔符 |
| **值必须继承 Bean** | 并经 `BeanFactory` 注册 |

## 相关文档

- 持久化集合总览：[./coll-overview.md](./coll-overview.md)
- Queue 持久队列：[./coll-queue.md](./coll-queue.md)
- DelayRemove 延迟删除：[./svc-delay-remove.md](./svc-delay-remove.md)
- DepartmentTree 部门树：[./coll-department-tree.md](./coll-department-tree.md)
