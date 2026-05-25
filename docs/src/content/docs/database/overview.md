---
title: "数据库抽象层总览"
sidebar:
  order: 1
---

Zeze 的数据库抽象层将底层存储引擎统一封装为 Key-Value 接口，使得上层事务代码无需关心数据最终持久化到哪里。通过简单的 XML 配置切换，同一套业务逻辑可以运行在内存、关系型数据库、NoSQL 或分布式 KV 存储之上。

## 核心抽象

### Database 抽象类

`Zeze.Transaction.Database` 是所有数据库实现的基类，定义了以下核心能力：

- **Table 管理**：通过 `openTable(name, id)` 打开数据库表，每张表在物理存储中对应一个独立的存储单元。
- **事务支持**：通过 `beginTransaction()` 获取数据库事务（`Database.Transaction`），支持 `commit` 和 `rollback`。
- **Operates 接口**：直接操作后台数据库的存储过程，用于实例管理（`setInUse`/`clearInUse`）和版本化数据读写（`saveDataWithSameVersion`/`getDataWithVersion`）。

### Database.Table 内部接口

`Database.Table` 是框架内部与存储交互的接口，提供两类实现：

| 实现方式 | 说明 |
|---|---|
| **AbstractKVTable** | KV 模式，key 和 value 均为 `ByteBuffer` 二进制数据。适用于所有数据库类型。 |
| **关系表映射** | 将 Bean 的字段映射为数据库表列，key 和 value 为 `SQLStatement`。目前 MySQL 和 PostgreSQL 支持。 |

`Database.Table` 还提供 `walk`/`walkDesc`/`walkKey` 等遍历接口，支持分页遍历和缓存合并读取。

## DatabaseType 枚举及选择指南

Zeze 通过 `Config.DbType` 枚举支持以下数据库类型：

| DbType | 实现类 | 说明 |
|---|---|---|
| `Memory` | `DatabaseMemory` | 纯内存存储，数据不持久化。适合单元测试和开发调试。 |
| `MySql` | `DatabaseMySql` | 基于 JDBC 的 MySQL 实现。支持 KV 模式和关系映射模式。 |
| `PostgreSQL` | `DatabasePostgreSQL` | 基于 JDBC 的 PostgreSQL 实现。支持 KV 模式和关系映射模式。 |
| `SqlServer` | `DatabaseSqlServer` | 基于 JDBC 的 SQL Server 实现。 |
| `RocksDB` | `DatabaseRocksDb` | 嵌入式 KV 存储，性能极高。**不支持 GlobalCacheManager（单机模式）**。 |
| `MongoDB` | `DatabaseMongoDb` | MongoDB 文档存储。 |
| `Redis` | `DatabaseRedis` | Redis KV 存储。 |
| `Tikv` | `DatabaseTikv` | TiKV 分布式事务 KV 存储，支持 `distTxn` 分布式事务。 |
| `DynamoDB` | `DatabaseDynamoDb` | AWS DynamoDB。 |
| `Dbh2` | `Zeze.Dbh2.Database` | Zeze 自研的分布式数据库抽象层。 |

## Storage 层

`Zeze.Transaction.Storage` 是 `TableX`（逻辑表）与 `Database.Table`（物理表）之间的桥梁。每个 `TableX` 在打开时会创建一个 `Storage` 实例：

```java
public Storage(TableX<K, V> table, Database database, String tableName) {
    this.table = table;
    if (table.isRelationalMapping() && database instanceof DatabaseRelationalMapping) {
        // 关系映射模式：打开关系表
        databaseTable = ((DatabaseRelationalMapping)database).openRelationalTable(tableName);
    } else {
        // KV 模式：打开普通 KV 表
        databaseTable = database.openTable(tableName, table.getId());
    }
}
```

Storage 层负责：
- 根据表配置选择 KV 模式或关系映射模式
- 管理 Database.Table 的生命周期

## XML 配置语法

数据库通过 `<DatabaseConf>` 节点配置，位于 `<zeze>` 根节点下：

```xml
<zeze>
  <!-- 默认数据库（名称为空字符串） -->
  <DatabaseConf DatabaseType="MySql"
    DatabaseUrl="jdbc:mysql://localhost:3306/zeze?user=root&amp;password=123456&amp;useSSL=false&amp;serverTimezone=UTC" />

  <!-- 命名数据库 -->
  <DatabaseConf Name="rocksdb"
    DatabaseType="RocksDB"
    DatabaseUrl="/data/zeze/rocksdb" />

  <DatabaseConf Name="mongo"
    DatabaseType="MongoDB"
    DatabaseUrl="mongodb://localhost:27017"
    DatabaseName="zeze_mongodb" />
</zeze>
```

### DatabaseConf 属性说明

| 属性 | 必填 | 说明 |
|---|---|---|
| `Name` | 否 | 数据库实例名称，默认为空字符串。通过 `TableConf.DatabaseName` 引用。 |
| `DatabaseType` | 否 | 数据库类型枚举值，默认 `Memory`。 |
| `DatabaseUrl` | 否 | 数据库连接地址或文件路径。MySQL/PostgreSQL 为 JDBC URL。 |
| `DatabaseName` | 否 | 数据库名称，仅 MongoDB 需要，默认 `zeze_mongodb`。 |
| `DisableOperates` | 否 | 设为 `true` 禁用 Operates（实例管理等），默认 `false`。 |
| `distTxn` | 否 | 启用分布式事务，仅 TiKV 支持，默认 `false`。 |

JDBC 类型（MySql、PostgreSQL、SqlServer）还支持 Druid 连接池属性（如 `DriverClassName`、`UserName`、`Password`、`MaxActive` 等），详见[关系型数据库](/database/relational)章节。

## 多数据库混合配置

Zeze 支持同一应用使用多种数据库。通过 `TableConf.DatabaseName` 将表分配到不同的数据库实例：

```xml
<zeze>
  <!-- 定义多个数据库 -->
  <DatabaseConf DatabaseType="MySql"
    DatabaseUrl="jdbc:mysql://db-server:3306/zeze?user=root&amp;password=pwd" />

  <DatabaseConf Name="hotdata"
    DatabaseType="Redis"
    DatabaseUrl="redis://localhost:6379" />

  <DatabaseConf Name="archive"
    DatabaseType="PostgreSQL"
    DatabaseUrl="jdbc:postgresql://pg-server:5432/archive?user=postgres&amp;password=pwd" />

  <!-- 默认表配置：使用默认数据库（MySQL） -->
  <TableConf CacheCapacity="20000" />

  <!-- 指定表使用特定数据库 -->
  <TableConf Name="demo_Module1_RankTable" DatabaseName="hotdata" CacheCapacity="50000" />
  <TableConf Name="demo_Module1_LogTable" DatabaseName="archive" CacheCapacity="100000" />
</zeze>
```

未指定 `DatabaseName` 的表使用名称为空的默认数据库。

## 数据库选择决策表

| 场景 | 推荐数据库 | 理由 |
|---|---|---|
| 单元测试 / 开发调试 | Memory | 零配置，启动最快，数据自动重置 |
| 单机高性能服务 | RocksDB | 嵌入式引擎，无网络开销，读写延迟极低 |
| 生产环境、需要 SQL 运维能力 | MySQL / PostgreSQL | 成熟稳定，支持关系映射和直接 SQL 查询 |
| 需要跨表关系查询 | MySQL / PostgreSQL（关系映射模式） | Bean 字段直接映射为表列，支持 SQL 索引 |
| 高吞吐热点数据 | Redis | 内存级读写性能 |
| 超大规模分布式 | TiKV | 分布式事务支持，水平扩展 |
| AWS 云原生部署 | DynamoDB | 全托管，按需付费 |
| 多数据中心同步 | Dbh2 | Zeze 自研，内置分片和复制 |

### 注意事项

1. **RocksDB 不支持多实例**：由于 RocksDB 是嵌入式存储，无法配合 `GlobalCacheManager` 进行多服务器缓存同步。配置了 `GlobalCacheManagerHostNameOrAddress` 时使用 RocksDB 会抛出异常。
2. **Key 长度限制**：数据库对 key 长度有限制，常量 `Database.eMaxKeyLength = 2712`（来自 PostgreSQL 限制）。MySQL 为 3072 字节，MongoDB 为 1024 字节。
3. **关系映射仅限 MySQL/PostgreSQL**：只有这两种数据库实现了 `DatabaseRelationalMapping` 接口，支持 Bean 字段到表列的映射。
