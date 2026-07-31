---
title: "DelayRemove 延迟删除"
description: "Zeze 延迟删除机制：先更新索引、延迟真删的轻量垃圾回收"
category: reference
order: 32
---

> 本文档说明 Zeze 的 DelayRemove 延迟删除机制：被删除的记录先加入延迟队列，过一段时间才真正删除，主要用于解决基于记录的队列（如 LinkedMap）在并发遍历与删除时的索引一致性问题。

## 概述

DelayRemove 把删除操作分成两步：先把记录加入**延迟队列**，**过一段时间才真正删除**。它是一个通用的延迟垃圾回收机制，LinkedMap 的「记录锁外并发遍历和删除」问题是它解决的典型场景之一，但并非只为 LinkedMap 设计。

| 特性 | 说明 |
|------|------|
| 行为 | 删除的记录先加入延迟队列，延迟一段后才真删 |
| 初始化 | ✅ **需要 `start()` 启动**（后台清扫任务） |
| 解决问题 | 并发遍历与删除时的索引一致性 |

## 用法

`DelayRemove.remove` 是**实例方法**（非静态方法），通常通过 `Table.delayRemove` 间接使用：

```java
// 实例方法（DelayRemove 单例，非静态调用）
delayRemove.remove(table, key);

// 通过 Table（推荐，内部转调上面的实例方法）
table.delayRemove(key);
```

| API | 说明 |
|-----|------|
| `DelayRemove.remove(TableX<K,?> table, K key)` | **实例方法**，把记录加入延迟队列 |
| `Table.delayRemove(key)` | Table 实例方法，等价调用 |

## 设计动机：LinkedMap 并发遍历问题

`Zeze.Collections.LinkedMap` 用记录存储 Node：每个 Node 记录前、后 Node 的 Key，每个 Node 存一定量 Item（可存很大 Item 量）。

问题：遍历 LinkedMap 时，**不能在一个事务内访问所有记录**，须挨个处理。当下一个记录刚被删时：

```
当前 Node.next → 指向已删除的记录
              → 获取下一个得 null
              → 不能继续遍历
```

DelayRemove 的解法：**先更新前后记录的索引**，更新后遍历只遍历新记录，并发遍历能继续；一定时间后才真正删除。

```
删除 Node X：
  1. 先更新 X.prev.next = X.next 和 X.next.prev = X.prev  （索引更新）
  2. 此时并发遍历只走新索引，不再访问 X                     （遍历可继续）
  3. 延迟一段时间后才真正删除 X                              （垃圾回收）
```

这相当于一个简单的垃圾回收机制：保证遍历过程中正在使用的数据不会被立即回收。

## 配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `DelayRemoveHourStart` | `3` | 每天执行真删的起始小时 |
| `DelayRemoveHourEnd` | `7` | 每天执行真删的结束小时 |
| `DelayRemoveDays` | `7` | 保留天数 |

```xml
<!-- 每天凌晨 3 点到 7 点之间执行真删，记录最多保留 7 天 -->
<DelayRemoveHourStart>3</DelayRemoveHourStart>
<DelayRemoveHourEnd>7</DelayRemoveHourEnd>
<DelayRemoveDays>7</DelayRemoveDays>
```

## 相关文档

- LinkedMap：[./coll-linked-map.md](./coll-linked-map.md)
- 配置参考：[./configuration.md](./configuration.md)
