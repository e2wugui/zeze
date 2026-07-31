---
title: "选配数据库"
description: "理解 Zeze 的数据库抽象层，按场景选对存储后端"
category: manual
order: 6
---

读完这篇，你将能根据项目所处的阶段（开发、单机部署、生产运维、超大规模）为 Zeze 选出最合适的数据库后端，并知道怎么通过配置在多个数据库之间灵活分配表。

## 为什么数据库可以是"透明"的

很多框架把业务逻辑和某种特定的数据库死死绑在一起——比如一上来就让你写 SQL，代码里到处是 `SELECT`、`INSERT`。一旦哪天想换个存储，几乎要重写一遍。

Zeze 不这么干。它的做法是：**在底层存储和上层事务之间，放一层统一的 KV（键值）抽象**。你写业务代码时操作的是 Bean 和事务，并不关心数据最终落到了内存里、MySQL 里还是 TiKV 里。需要换数据库时，**只改 XML 配置，不动一行业务代码**，同一套逻辑就能跑在另一种存储上。

这就是"数据库透明"的核心：**一套代码，多种后端，按需切换**。

## Database 抽象类：统一接口的入口

这一层抽象的根基是 `Database` 抽象类，它对外暴露的核心能力很简洁：

- **`openTable(name, id)`**：打开一张表。
- **`beginTransaction()`**：获取一个数据库事务。
- **`Operates` 接口**：直接操作后台数据库，包括实例的 `setInUse` / `clearInUse` 管理、以及版本化数据的读写。

而 `Database.Table` 又有两类实现，对应两种数据组织方式：

| 实现类型 | 说明 | 适用范围 |
|---|---|---|
| **AbstractKVTable** | KV 模式，key 和 value 都是 `ByteBuffer` | **所有数据库**都支持 |
| **关系表映射** | 把 Bean 的字段映射为表的列 | 仅 **MySQL / PostgreSQL** 支持 |

也就是说，KV 模式是"最大公约数"，无论你选哪种后端都能用；而关系映射是给 MySQL/PostgreSQL 的"加强包"，能让你享受索引、关系查询的好处。

在这两者之间起桥梁作用的是 **Storage 层**：它读着表的配置，决定某张表该走 KV 模式还是关系映射模式，把 `TableX` 和底层的 `Database.Table` 衔接起来。

## 数据库类型一览

`DatabaseType` 枚举列出了 Zeze 支持的所有后端：

| DbType | 说明 |
|---|---|
| **Memory** | 纯内存，不持久化。适合单元测试和开发调试 |
| **MySql** | 基于 JDBC，同时支持 KV 和关系映射 |
| **PostgreSQL** | 基于 JDBC，同时支持 KV 和关系映射 |
| **SqlServer** | 基于 JDBC，**需自备 mssql 驱动** |
| **RocksDB** | 嵌入式 KV，性能极高，**不支持 GlobalCacheManager（仅单机模式）** |
| **MongoDB** | 文档存储 |
| **Redis** | KV 存储 |
| **Tikv** | 分布式事务 KV，支持 `distTxn` |
| **Dbh2** | Zeze 自研的分布式数据库 |

## 怎么选：一张决策表

直接对着场景选，是最省心的方式：

| 场景 | 推荐 | 理由 |
|---|---|---|
| 单元测试 / 开发调试 | **Memory** | 零配置，启动最快，跑完即弃 |
| 单机高性能 | **RocksDB** | 嵌入式、无网络开销，读写极快 |
| 生产环境需要 SQL 运维 | **MySQL / PostgreSQL** | 成熟稳定，运维生态完善，支持关系映射 |
| 需要跨表关系查询 | **MySQL / PostgreSQL 关系映射** | Bean 字段映射为表列，可建索引 |
| 高吞吐热点数据 | **Redis** | 内存级读写延迟 |
| 超大规模分布式 | **TiKV** | 分布式事务，能水平扩展 |
| 多数据中心同步 | **Dbh2** | 内置分片与复制 |

一个务实的成长路径是：**开发期用 Memory，单机上线换 RocksDB，规模扩大或需要 SQL 运维时迁到 MySQL/PostgreSQL，走向真正的大规模分布式时再上 TiKV**。因为切换只改配置，这条路走得平滑。

## 配置：用 XML 把一切串起来

数据库的选择最终落在 XML 配置上。一个 `DatabaseConf` 大致长这样：

```xml
<DatabaseConf
    Name=""
    DatabaseType="MySql"
    DatabaseUrl="jdbc:mysql://127.0.0.1:3306/zeze"
    DatabaseName="zeze"
    DisableOperates="false"
    distTxn="false" />
```

几个要点：

- **`Name` 为空字符串代表默认数据库**，没有特别指定的表都会落到它上面。
- **可以同时配置多个 `DatabaseConf`**，给每个起个不同的 `Name`。
- 然后用 `<TableConf>` 把具体的表分配到指定的数据库实例：

```xml
<!-- 把角色表放到主库 -->
<TableConf Name="Role" DatabaseName="zeze" />

<!-- 把日志表单独放到另一个库，分摊压力 -->
<TableConf Name="GameLog" DatabaseName="zeze_log" />
```

这就是**多库混用**：你可以把热点表和高频写入的表拆到不同的数据库实例上，各取所长。比如把核心业务放 MySQL、把排行榜这种高吞吐热点放 Redis，组合使用。

## 一个绕不开的限制：RocksDB 只能单机

RocksDB 是嵌入式数据库，数据直接写在进程所在机器的磁盘上，没有网络开销，所以**单机性能极高**。但这也带来了一个硬限制：

> ⚠️ **RocksDB 不支持多实例**。它无法配合 GlobalCacheManager 做多服务器之间的缓存同步——如果你强行配置，框架会**直接抛异常**。

换句话说，一旦你的服务要走向多 Provider 的分布式部署（参见上一篇 [走向分布式](./05-going-distributed.md) 里讲到的 GCM 缓存一致性），就必须换掉 RocksDB，改用支持分布式的后端（如 MySQL、TiKV 等）。RocksDB 的定位很明确：**单机时代的最佳选择**。

另外有几个 key 长度限制值得留意，避免踩坑：`Database.eMaxKeyLength = 2712`（受 PostgreSQL 限制），MySQL 是 3072，MongoDB 是 1024。关系映射模式则只有 MySQL / PostgreSQL 支持。

## TableConf：把缓存调到刚刚好

如果说 `DatabaseConf` 决定"数据存哪儿"，那 `TableConf` 决定的是"这张表怎么被缓存和使用"。它有几个关键属性：

| 属性 | 作用 |
|---|---|
| **`CacheCapacity`** | 缓存容量（记录数）。**最重要的配置**。建议设成预期在线人数 |
| **`CacheFactor`** | 放大因子，默认 `5.0`。借助 `SoftReference`，实际可缓存的记录数能远超 `CacheCapacity` |
| **`CheckpointWhenCommit`** | 事务提交时是否立即把该表持久化。默认 `false`，对充值货币等关键数据建议设为 `true` |
| **`DatabaseName`** | 指定这张表归属哪个数据库实例 |
| **`DatabaseOldName` / `DatabaseOldMode`** | 旧库配置，用于数据迁移：新库查不到时自动从旧库读取并导入 |

### CacheCapacity 怎么配

`CacheCapacity` 是你最能"感受到性能"的一个旋钮。它的实际效果是：

> **实际缓存记录数 ≈ `CacheCapacity` × `CacheFactor`**

原理是 Zeze 用 `SoftReference`（软引用）来持有缓存对象。JVM 内存充裕时，缓存可以远超 `CacheCapacity` 的设定值（最多到乘以 `CacheFactor` 的量级）；只有当内存紧张时，才会按需回收软引用，把缓存压回到 `CacheCapacity` 附近。

所以一条简单的经验法则是：**把 `CacheCapacity` 设成你预期的在线人数**。在线的人数据基本都能命中缓存，离线的人数据在内存紧张时被自然淘汰，下次访问再从数据库加载。配合默认的 `CacheFactor=5.0`，缓冲空间相当宽裕。

### 关键数据要"落袋为安"

大多数表采用延迟写（提交事务时不立刻刷盘，由检查点机制统一处理），性能更好。但对于**绝对不能丢的数据**——比如玩家的充值货币——可以把 `CheckpointWhenCommit` 设为 `true`，让每次事务提交都立刻持久化这张表。代价是多一点 I/O，换来的是断电也不丢数据的安心。

### 平滑迁移：新旧库并存

当你需要换库或重构表结构时，`DatabaseOldName` / `DatabaseOldMode` 能让迁移变得平滑：框架在读取时，如果新库查不到，会自动回退到旧库读取并导入到新库。这样你就能**边跑边迁**，而不用停服做一次性大搬家。

## 小结

把这篇收个尾，其实就三句话：

- **数据库透明**：统一 KV 抽象 + XML 配置切换，让你在 Memory、关系型、NoSQL、分布式 KV 之间自由选择，业务代码纹丝不动。
- **按场景选型**：开发用 Memory、单机上 RocksDB、生产运维上 MySQL/PostgreSQL、超大规模上 TiKV，热点上 Redis，路径清晰。
- **精调缓存**：用 `CacheCapacity` 配合 `CacheFactor` 把缓存调到刚好的水位，用 `CheckpointWhenCommit` 守护关键数据，用新旧库配置做平滑迁移。

选好了数据库，下一步就该理解在它之上、Zeze 是如何让你不用操心多线程也能写出正确的并发程序的——请继续阅读 [不懂多线程也能写并发程序](./07-multithreading-without-fear.md)。需要查阅配置项与数据库抽象的完整细节时，参考 [数据库总览](../reference/db-overview.md) 和 [配置参考](../reference/configuration.md)。
