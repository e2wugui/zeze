---
title: "参考索引"
description: "Zeze 详细参考文档总索引，按主题分组"
category: reference
order: 0
---

# 参考索引

> 这里有 Zeze 全部主题的 API 级参考文档。写代码时查具体接口、配置、属性表——按主题找入口。如果想系统学习，请先读 [Manual 指南](../manual/01-the-pain.md)。

## 核心

定义数据、写事务、读写存储的基石。

| 文档 | 说明 |
|---|---|
| [solution.xml 参考](./solution-xml.md) | 完整 XML 语法：solution/module/bean/variable/table/rpc/protocol/project/service 全部标签与属性 |
| [Bean 数据模型](./bean.md) | 类型系统、variable id 规则、托管状态、DynamicBean、版本兼容 |
| [事务系统](./transaction.md) | Procedure、TransactionLevel、嵌套存储过程、whileCommit、返回值、@DispatchMode |
| [Table 存储接口](./table.md) | CRUD、selectDirty、walk 遍历、TableCache、本地 RocksDB 缓存 |
| [序列化协议](./serialize.md) | 二进制 TLV 编码、4-bit 类型、Tag、varint、跨语言一致 |
| [ChangeListener](./listener.md) | Table 级数据变更监听，同步数据给客户端 |
| [事务中操作外部系统](./third-party-interactions.md) | whileCommit/幂等/事务队列，安全调度外部副作用 |
| [配置参考](./configuration.md) | 完整 XML 配置：zeze/DatabaseConf/TableConf/ServiceConf |

## 架构

分布式服务的组成与协作。

| 文档 | 说明 |
|---|---|
| [Provider-Linkd 架构](./arch-provider-linkd.md) | Linkd/Provider/ServiceManager/ProviderDirect 角色与交互全貌 |
| [Online 在线管理](./arch-online.md) | 登录登出、可靠消息、Transmit 跨服转发、Broadcast |
| [Redirect 跨服调用](./arch-redirect.md) | @RedirectHash / @RedirectAll / @RedirectToServer 三种模式 |
| [网络层](./arch-net.md) | Service/Protocol/Rpc、连接管理、握手加密、WebSocket |
| [Session 与 UserState](./arch-session.md) | 会话信息在连接/协议/事务间的传递 |
| [GlobalCacheManager](./arch-global-cache-manager.md) | 缓存同步协议：Modify/Share/Invalid 权限模型 |
| [ServiceManager](./arch-service-manager.md) | 服务注册与发现 |
| [全球同服](./arch-one-world.md) | 单点模块的并发分组、ConcurrentLevel、大量共享模块优化 |

## 数据库

存储后端的选择与配置。

| 文档 | 说明 |
|---|---|
| [数据库抽象层总览](./db-overview.md) | Database/Storage、DatabaseType 枚举、选择决策表 |
| [关系型（MySQL/PostgreSQL）](./db-relational.md) | JDBC、KV 模式 vs 关系映射、Druid 连接池 |
| [NoSQL（MongoDB/Redis/TiKV）](./db-nosql.md) | 各 NoSQL 配置与选型 |
| [RocksDB](./db-rocksdb.md) | 嵌入式引擎、单机限制、本地缓存层 |
| [Dbh2](./db-dbh2.md) | Zeze 自研分布式数据库，分桶与 Raft |

## 内置组件

开箱即用的框架组件。

| 文档 | 说明 |
|---|---|
| [Timer 定时器](./svc-timer.md) | Auto/Named/Online/Offline 四类调度、Cron 表达式 |
| [AutoKey](./svc-autokey.md) | 分布式唯一自增 ID |
| [DelayRemove](./svc-delay-remove.md) | 延迟删除，配合并发遍历 |
| [Raft](./svc-raft.md) | 共识实现，GCM/ServiceManager 的高可用基础 |

## 持久化集合

基于 Table 的高层数据结构。

| 文档 | 说明 |
|---|---|
| [集合总览](./coll-overview.md) | 统一初始化、BeanFactory、与 Table 的关系 |
| [Queue](./coll-queue.md) | FIFO 队列 / LIFO 栈、CsQueue 跨服务器 |
| [LinkedMap](./coll-linked-map.md) | 有序双向链表映射，背包/排行榜 |
| [CHashMap](./coll-chashmap.md) | 一致性哈希并发 Map |
| [DepartmentTree](./coll-department-tree.md) | 部门树，组织架构/权限 |
| [BoolList](./coll-bool-list.md) | 位图列表，标记位 |
| [DAG](./coll-dag.md) | 有向无环图，依赖管理 |

## 游戏模块

游戏服务端开箱即用系统。

| 文档 | 说明 |
|---|---|
| [游戏模块总览](./game-overview.md) | ProviderWithOnline 启动入口 |
| [Bag 背包](./game-bag.md) | 物品增删移、自动堆叠拆分 |
| [Rank 排行榜](./game-rank.md) | 并发分区、RedirectHash、多路归并 |
| [LoginQueue 登录队列](./game-login-queue.md) | 登录排队与过载保护 |
| [Task 任务系统](./game-task.md) | 基于持久化队列的任务模型 |

## 客户端接入

跨语言客户端库。

| 文档 | 说明 |
|---|---|
| [C# / Unity](./client-csharp.md) | solution.xml 生成 C# 代码、Unity 接入 |
| [C++](./client-cpp.md) | zezecxx 静态库、ByteBuffer、Net、Protocol |
| [TypeScript](./client-typescript.md) | Zeze/zeze.ts 序列化、浏览器/Node.js 接入 |

## 进阶

运行期与运维话题。

| 文档 | 说明 |
|---|---|
| [热更新](./advanced-hot-reload.md) | ClassReloader/HotModule/HotService，不停服更新 |
| [线程模型](./advanced-threads.md) | 线程池分类、Checkpoint 线程、调度 |
| [性能调优](./advanced-performance.md) | 缓存命中率、Checkpoint 策略、benchmark |
| [Prometheus 监控](./advanced-metrics.md) | 指标暴露、Dashboard |
| [消息队列](./advanced-mq.md) | 内置 MQ、RocketMQ、RedoQueue 跨系统重试 |

---

← 返回 [文档首页](../index.md) · 想系统学习去 [Manual 指南](../manual/01-the-pain.md)
