---
title: "NoSQL 数据库"
sidebar:
  order: 3
---

Zeze 除了关系型数据库外，还支持多种 NoSQL 数据库作为持久化后端。所有数据库适配器均实现统一的 `Database` 接口，可在配置文件中灵活切换。

## MongoDB

MongoDB 是一种文档型 NoSQL 数据库，Zeze 通过官方 Java 驱动 `mongodb-driver-sync` 进行连接。

### 配置方式

```xml
<DatabaseConf Name=""
    DatabaseType="MongoDB"
    DatabaseUrl="mongodb://user:password@127.0.0.1:27017/?replicaSet=rs0"
    DatabaseName="zeze_db"/>
```

- **DatabaseUrl**: MongoDB 连接字符串，需包含 `replicaSet` 参数以启用事务支持。
- **DatabaseName**: 指定数据库名称，每张 Zeze 表对应 MongoDB 中的一个 Collection。

### 内部实现

- 每张表 (`openTable`) 映射为 MongoDB 的一个 Collection，使用 `_id` 字段存储二进制 Key，`value` 字段存储二进制 Value。
- 事务通过 `ClientSession` 实现，依赖 MongoDB 副本集的分布式事务机制。
- 支持正序/逆序遍历（`walk`/`walkDesc`）以及分页遍历（带 `exclusiveStartKey` 和 `proposeLimit` 参数）。
- 集合在首次打开时自动创建；若已存在则复用。

## Redis

Redis 是一种高性能的内存 KV 存储，Zeze 基于 Jedis 客户端实现。兼容 Redis 2.8+、Kvrocks 2.02+、Pika 3.5.2+。

### 配置方式

```xml
<DatabaseConf Name=""
    DatabaseType="Redis"
    DatabaseUrl="redis://127.0.0.1:6379/"/>
```

- **DatabaseUrl**: 标准 Redis URI 格式，支持 `redis://user:password@host:port/db`。

### 内部实现

- 使用 JedisPool 连接池，默认最大并发连接数 1024，空闲连接上限 8，等待超时 10 秒。
- 每张表使用 Redis Hash 结构存储，表名作为 Hash Key。
- 事务通过 Redis `MULTI`/`EXEC` 命令实现（`RedisTransaction`）。
- 使用 `HSCAN` 进行遍历，**不支持逆序遍历**（`walkDesc`/`walkKeyDesc` 会抛出 `UnsupportedOperationException`）。
- Schema 版本管理通过全局锁（`SETNX`/`DEL`）实现互斥访问。

## TiKV

TiKV 是一种分布式 KV 数据库，Zeze 通过 TiKV Java Client 实现，支持 RawKV 和分布式事务两种模式。

### 配置方式

```xml
<DatabaseConf Name=""
    DatabaseType="Tikv"
    DatabaseUrl="172.21.15.68:2379"/>
```

- **DatabaseUrl**: TiKV PD（Placement Driver）地址，格式为 `host:port`。

### 内部实现

- **RawKV 模式**（默认）: 使用 `RawKVClient` 进行简单 KV 操作，通过 `batchPut`/`batchDelete` 批量提交。
- **分布式事务模式**: 通过配置 `DistTxn` 启用，使用 `TwoPhaseCommitter` 实现两阶段提交。两种模式的数据**不能互通**。
- 表名作为 Key 前缀，避免不同表的数据冲突。
- 支持正序遍历（`walk`/`walkKey`），**不支持逆序遍历**和部分分页遍历。
- `isNew()` 始终返回 `false`，不直接支持删表操作。

## DynamoDB

DynamoDB 是 AWS 提供的全托管 NoSQL 数据库服务。

### 配置方式

```xml
<DatabaseConf Name=""
    DatabaseType="DynamoDb"
    DatabaseUrl="">
    <DynamoConf region="CN_NORTH_1"/>
</DatabaseConf>
```

- **DynamoConf region**: AWS 区域，如 `CN_NORTH_1`、`US_EAST_1` 等。
- AWS 凭证通过标准凭证链（环境变量、配置文件、实例角色等）自动获取。

### 内部实现

- 使用 AWS SDK `AmazonDynamoDBClientBuilder` 创建客户端，启用端点发现。
- 每张表映射为 DynamoDB 的一张表，Key 为 `B`（Binary）类型分区键。
- 写操作通过 `TransactWriteItems` 实现事务语义。
- 遍历使用 `Scan` 操作，支持分页，**不支持逆序遍历**。
- 新表自动创建，默认预置吞吐量为读写各 10 个单位。

## 特性与限制对比

| 特性 | MongoDB | Redis | TiKV | DynamoDB |
|------|---------|-------|------|----------|
| 事务支持 | 副本集事务 | MULTI/EXEC | RawKV/两阶段提交 | TransactWriteItems |
| 正序遍历 | 支持 | 支持 (HSCAN) | 支持 | 支持 (Scan) |
| 逆序遍历 | 支持 | 不支持 | 不支持 | 不支持 |
| 分页遍历 | 支持 | 不支持 | 不支持 | 支持 |
| 自动建表 | 自动建集合 | 无需建表 | 无需建表 | 自动建表 |
| Schema 版本管理 | 支持 | 支持 (全局锁) | 支持 | 支持 |
| 部署依赖 | 副本集 | 单机/集群 | PD + TiKV 集群 | AWS 托管服务 |
| 适用场景 | 文档存储、事务 | 高速缓存、简单KV | 大规模分布式 | 云原生无运维 |

> **注意**: 所有 NoSQL 数据库适配器的 `DatabaseUrl` 和连接参数均通过 XML 配置文件中的 `<DatabaseConf>` 节点传递，运行时不可更改。
