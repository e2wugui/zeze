---
title: "Zeze 文档"
description: "基于缓存一致性的分布式事务应用框架"
category: root
order: 0
---

# Zeze 文档

> **Zeze** 是一个基于**缓存一致性**的分布式事务应用框架。它用一个统一的方案同时解决了服务端开发中最折磨人的三个问题：**数据中途失败导致的不一致**、**多线程并发与死锁**、**内存数据与数据库的同步**。

你只需要**定义数据结构、编写业务逻辑**，事务管理、并发控制和持久化同步全部由框架自动完成——写起来就像单进程单线程程序一样简单，跑起来却是安全的、可并发的、可分布式的。

---

## 为什么值得读下去

如果你写过服务端，下面三个场景多半都遇到过：

1. **一个操作要改好几份数据，改到一半抛异常了**——已经改完的部分怎么办？手写回滚？模块一嵌套就维护不动了。
2. **高并发下线程间共享数据**——加锁吧，竞态、死锁、优先级反转接踵而至；不加吧，数据被改坏。
3. **内存里的数据什么时候写库**——写早了浪费 I/O，写晚了怕丢；同时用好几种数据库，同步逻辑成倍增加。

Zeze 用**内存事务 + 乐观锁 + 一致性缓存**把这三件事一次性解决：异常自动回滚、原理上不可能死锁、内存与数据库自动同步。详见 [Zeze 解决的三大痛点](./manual/01-the-pain.md)。

---

## 文档怎么读

这套文档分三层，按你的目标选入口：

### 🚀 Quickstart（快速上手）—— 30 分钟跑起来

第一次接触 Zeze，想最快看到能跑的东西。从 [Zeze 是什么](./quickstart/01-what-is-zeze.md) 开始。

- [01 · Zeze 是什么](./quickstart/01-what-is-zeze.md) — 一句话和三个痛点
- [02 · 环境搭建](./quickstart/02-install.md) — JDK、代码生成器、IDE
- [03 · 第一个应用](./quickstart/03-first-app.md) — XML 定义 → 生成 → 写逻辑
- [04 · 接下来读什么](./quickstart/04-next-steps.md) — 学习路径指引

### 📖 Manual（指南）—— 懂原理、会设计

想真正理解 Zeze 为什么这样设计、怎么用好它。这是阅读主体，建议顺序读。

- [01 · Zeze 解决的三大痛点](./manual/01-the-pain.md) — 核心动机，开篇必读
- [02 · Zeze 如何工作](./manual/02-how-zeze-works.md) — 心智模型与 CPU 缓存类比
- [03 · 定义数据](./manual/03-defining-data.md) — Bean、Table、solution.xml
- [04 · 编写业务逻辑](./manual/04-writing-logic.md) — 存储过程、事务回滚、副作用
- [05 · 走向分布式](./manual/05-going-distributed.md) — Provider-Linkd、全球同服
- [06 · 选配数据库](./manual/06-choosing-database.md) — Memory/RocksDB/MySQL/TiKV
- [07 · 不懂多线程也能写并发程序](./manual/07-multithreading-without-fear.md)
- [08 · 上线清单](./manual/08-production-checklist.md) — 配置、容量、过载、停服

### 📚 Reference（详细参考）—— 随查随用

写代码时查具体 API、配置项、属性表。按主题分组，见 [参考索引](./reference/index.md)，涵盖：

- **核心**：[solution.xml](./reference/solution-xml.md) · [Bean](./reference/bean.md) · [事务](./reference/transaction.md) · [Table](./reference/table.md) · [序列化](./reference/serialize.md) · [配置](./reference/configuration.md)
- **架构**：[Provider-Linkd](./reference/arch-provider-linkd.md) · [Online](./reference/arch-online.md) · [Redirect](./reference/arch-redirect.md) · [网络层](./reference/arch-net.md) · [GlobalCacheManager](./reference/arch-global-cache-manager.md) · [服务发现](./reference/arch-service-manager.md)
- **数据库**：[概览](./reference/db-overview.md) · [关系型](./reference/db-relational.md) · [NoSQL](./reference/db-nosql.md) · [RocksDB](./reference/db-rocksdb.md) · [Dbh2](./reference/db-dbh2.md)
- **内置组件**：[Timer](./reference/svc-timer.md) · [AutoKey](./reference/svc-autokey.md) · [DelayRemove](./reference/svc-delay-remove.md) · [Raft](./reference/svc-raft.md)
- **持久化集合**：[概览](./reference/coll-overview.md) · [Queue](./reference/coll-queue.md) · [LinkedMap](./reference/coll-linked-map.md) · [CHashMap](./reference/coll-chashmap.md) · [DepartmentTree](./reference/coll-department-tree.md) · [BoolList](./reference/coll-bool-list.md) · [DAG](./reference/coll-dag.md)
- **游戏模块**：[总览](./reference/game-overview.md) · [背包](./reference/game-bag.md) · [排行榜](./reference/game-rank.md) · [登录队列](./reference/game-login-queue.md) · [任务](./reference/game-task.md)
- **客户端接入**：[C#/Unity](./reference/client-csharp.md) · [C++](./reference/client-cpp.md) · [TypeScript](./reference/client-typescript.md)
- **进阶**：[热更新](./reference/advanced-hot-reload.md) · [线程模型](./reference/advanced-threads.md) · [性能调优](./reference/advanced-performance.md) · [监控](./reference/advanced-metrics.md) · [消息队列](./reference/advanced-mq.md)

---

## 一分钟速览

| 你关心的问题 | Zeze 的答案 | 去哪看 |
|---|---|---|
| 改数据改一半出错 | 内存事务，异常自动整体回滚 | [痛点篇](./manual/01-the-pain.md) · [事务参考](./reference/transaction.md) |
| 多线程死锁 | 乐观锁，执行不加锁、提交才校验，原理上无死锁 | [工作原理](./manual/02-how-zeze-works.md) |
| 数据何时落库 | Checkpoint 自动批量同步，无需写 SQL | [选配数据库](./manual/06-choosing-database.md) |
| 能用哪些数据库 | Memory / RocksDB / MySQL / PostgreSQL / MongoDB / Redis / TiKV / SqlServer / Dbh2 | [数据库概览](./reference/db-overview.md) |
| 怎么水平扩展 | Provider-Linkd 架构 + 服务发现 + 缓存同步 | [走向分布式](./manual/05-going-distributed.md) |
| 多语言客户端 | Java / C# / C++ / TypeScript / Lua / Python | [客户端接入](./reference/client-csharp.md) |

---

## 项目信息

- **主语言**：Java（编译目标 **Java 21**，需 JDK 21 及以上）
- **Maven 坐标**：`com.zezeno:zeze-java`
- **包名约定**：Java 包用 `Zeze.*`（大写 Z），不是 `com.zeze.*`
- **初始项目模板**：[zezeboot](https://gitee.com/dwing/zezeboot)

> 本文档是对 Zeze 框架的全新整理，目标是流畅易读、完整齐备。原始文档保留在框架仓库中，本文档不覆盖、不修改原文，所有内容独立存放在 `zezedoc/`。
