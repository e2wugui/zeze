---
title: "数据库抽象层总览"
description: "Zeze 数据库抽象层的核心接口、DatabaseType 枚举、Storage 桥梁与选型决策表"
category: reference
order: 20
---

> 本文档总览 Zeze 数据库抽象层的整体结构：底层存储统一封装为 KV 接口，上层事务不关心数据持久化到哪里，通过 XML 配置即可让同一套业务代码运行在内存、关系型、NoSQL 或分布式 KV 之上。本文是其余数据库参考文档的入口与索引。

## 设计理念：KV 统一，存储透明

Zeze 在「底层存储」和「上层事务」之间放了一层统一的 **KV（键值）抽象**。业务代码只操作 Bean 与事务，不感知数据最终落到内存、MySQL 还是 TiKV。切换数据库时**只改 XML 配置，不动业务代码**。

```
┌──────────────────────────────────────────┐
│        业务逻辑（Bean + 事务）            │  不关心持久化到哪
├──────────────────────────────────────────┤
│   Storage 层（TableX ↔ Database.Table）   │  桥梁：选 KV / 关系映射
├──────────────────────────────────────────┤
│   Database 抽象（openTable / Txn）        │  统一接口
├──────────────────────────────────────────┤
 Memory │ MySql │ PostgreSQL │ SqlServer │ RocksDb │ MongoDB │ Redis │ Tikv │ Dbh2
```

## Database 抽象类

`Database` 是一切存储后端的根基。核心 API：

| API | 说明 |
|-----|------|
| `openTable(name, id)` | 打开一张表，返回 `Database.Table` |
| `beginTransaction()` | 获取一个 `Database.Transaction` |
| `Database.Transaction` | 支持 `commit()` / `rollback()` |
| `Operates` 接口 | 直接操作后台库：`setInUse` / `clearInUse`（实例管理）、`saveDataWithSameVersion` / `getDataWithVersion`（版本化读写） |

### Database.Table 两类实现

| 实现类型 | key / value | 适用范围 | 说明 |
|----------|-------------|----------|------|
| **AbstractKVTable** | 均为 `ByteBuffer` | **所有数据库** | KV 模式，最通用 |
| **关系表映射** | Bean 字段映射为表列，key/value 为 `SQLStatement` | **仅 MySQL / PostgreSQL** | 支持按字段建索引与 SQL 查询 |

分页遍历通过 `walk` / `walkDesc` / `walkKey` 实现，遍历结果会与缓存合并。

## DatabaseType 枚举

下表是 `Config.DbType` 枚举的全部 10 个值及其工厂接线情况：

| 枚举值 | 实现类 | 工厂接线 | 特性 |
|--------|--------|----------|------|
| `Memory` | `DatabaseMemory` | ✅ | 纯内存，不持久化，测试与开发用 |
| `MySql` | `DatabaseMySql` | ✅ | JDBC，支持 KV + 关系映射 |
| `PostgreSQL` | `DatabasePostgreSQL` | ✅ | JDBC，支持 KV + 关系映射 |
| `SqlServer` | `DatabaseSqlServer` | ✅ | JDBC；**需自备 mssql 驱动**（框架 `build.gradle` 未含该依赖） |
| `RocksDb` | `DatabaseRocksDb` | ✅ | 嵌入式，无网络，**不支持 GlobalCacheManager**，单机 |
| `MongoDB` | `DatabaseMongoDb` | ✅ | 文档存储 |
| `Redis` | `DatabaseRedis` | ✅ | KV 内存级读写 |
| `Tikv` | `DatabaseTikv` | ✅ | 分布式事务，支持 `distTxn` |
| `DynamoDb` | `DatabaseDynamoDb` | ❌ **未接线** | 实现类存在，但工厂 switch 无此 case，配置即抛 `UnsupportedOperationException` |
| `Dbh2` | `Zeze.Dbh2.Database` | ✅ | 自研分布式数据库 |

> ⚠️ **DynamoDB 当前不可用**：虽有枚举值和实现类，但 `Config.createDatabase` 工厂未为其接线，配置后会抛异常，不能作为生产后端。**FoundationDB 不在枚举中**——`build.gradle` 虽有 `fdb-java` 依赖，但全仓库无引用、`DbType` 无此项，无法配置。

## Storage 层：TableX 与 Database.Table 的桥梁

`Storage` 是连接 TableX 与 `Database.Table` 的桥梁。在 `open()` 时根据配置决定走 KV 模式还是关系映射模式：

```
open() 时：
  if (table.isRelationalMapping() && database instanceof DatabaseRelationalMapping)
      openRelationalTable(table)   // 关系映射模式
  else
      openTable(...)               // KV 模式
```

Storage 管理表的生命周期，根据配置选择 KV 或关系映射。

## XML 配置

### 单数据库配置

```xml
<DatabaseConf Name="default"
              DatabaseType="Memory"
              DatabaseUrl=""
              DatabaseName="">
</DatabaseConf>
```

属性说明：

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `Name` | — | 数据库逻辑名，`TableConf.DatabaseName` 通过它引用 |
| `DatabaseType` | `Memory` | 数据库类型，见上方枚举 |
| `DatabaseUrl` | — | JDBC URL 或文件路径 |
| `DatabaseName` | `zeze_mongodb` | 命名库；`TableConf.DatabaseName` 引用它分配表归属 |
| `DisableOperates` | — | 禁用 `Operates` 接口 |
| `DistTxn` | — | 仅 TiKV，启用分布式事务 |

> ⚠️ **默认库名是 `zeze_mongodb`（适用于所有数据库类型）**，不是空串，也不仅限 MongoDB。这是 `DatabaseConf` 字段级的默认值（`Config.java:843`），配置为空时也回退为 `zeze_mongodb`。如果你的库不叫这个名字，记得显式配置。

### JDBC 连接池（Druid）

JDBC 数据库支持 Druid 连接池，详见 [./db-relational.md](./db-relational.md)。

### 多数据库混合

定义多个 `DatabaseConf`，通过 `TableConf` 的 `Name` 与 `DatabaseName` 分配表归属：

```xml
<!-- 多个数据库 -->
<DatabaseConf Name="default" DatabaseType="MySql" DatabaseUrl="jdbc:mysql://..."/>
<DatabaseConf Name="hotdata" DatabaseType="Redis" DatabaseUrl="redis://localhost:6379"/>

<!-- 把指定表分配到 hotdata 库 -->
<TableConf Name="demo_Module1_RankTable" DatabaseName="hotdata"/>
```

未指定 `DatabaseName` 的表使用默认库。

## 数据库选择决策表

| 场景 | 推荐 | 理由 |
|------|------|------|
| 单元测试 | `Memory` | 纯内存、不持久化、零依赖、速度快 |
| 单机高性能 | `RocksDb` | 嵌入式，无网络开销，读写延迟极低 |
| 生产 SQL 运维 | `MySql` / `PostgreSQL` | 团队熟悉，工具链成熟 |
| SQL Server 生态 | `SqlServer` | 需自备 mssql JDBC 驱动 |
| 跨表关系查询 | 关系映射模式 | Bean 字段映射表列，支持 SQL |
| 高吞吐热点 | `Redis` | 内存级 KV 读写 |
| 超大规模分布式 | `Tikv` | 水平扩展，支持分布式事务 |
| 多数据中心 | `Dbh2` | 自研分布式，内置分桶与复制 |

## 注意事项

| 限制 | 说明 |
|------|------|
| RocksDB 多实例 | 嵌入式存储无法多实例共享，**不支持** `GlobalCacheManager`；配置 `GlobalCacheManagerHostNameOrAddress` 时使用 RocksDB 会抛异常 |
| key 长度限制 | `eMaxKeyLength`：PostgreSQL `2712`、MySQL `3072`、MongoDB `1024` |
| 关系映射范围 | 仅 **MySQL + PostgreSQL** 支持关系映射模式 |

## 相关文档

- Table 参考：[./table.md](./table.md)
- 配置参考：[./configuration.md](./configuration.md)
- 关系型数据库：[./db-relational.md](./db-relational.md)
- NoSQL：[./db-nosql.md](./db-nosql.md)
- RocksDB：[./db-rocksdb.md](./db-rocksdb.md)
- Dbh2：[./db-dbh2.md](./db-dbh2.md)
- 选型指南：[../manual/06-choosing-database.md](../manual/06-choosing-database.md)
