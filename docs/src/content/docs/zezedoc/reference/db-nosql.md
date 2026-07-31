---
title: "NoSQL 数据库（MongoDB/Redis/TiKV）"
description: "Zeze 支持的三种 NoSQL 后端：MongoDB、Redis、TiKV 的配置、依赖与选型建议"
category: reference
order: 22
---

> 本文档说明 Zeze 支持的 NoSQL 存储后端（MongoDB / Redis / TiKV）的配置方式、连接 URL 示例、依赖坐标与适用场景，供需要文档存储、高吞吐热点、分布式事务的场景选型参考。

## 支持的 NoSQL 后端

| 实现 | 类型 | 是否持久化 | 是否分布式 | 特性 |
|------|------|------------|------------|------|
| `DatabaseMongoDb` | 文档存储 | ✅ | 视部署 | 文档型，灵活模式 |
| `DatabaseRedis` | KV 内存 | ✅ | 视部署 | 高吞吐热点读写 |
| `DatabaseTikv` | 分布式 KV | ✅ | ✅ | 水平扩展，支持分布式事务 |

> DynamoDB 虽然在源码中有实现类（`DatabaseDynamoDb`）和 `DbType` 枚举值，但 `Config.createDatabase` 工厂**没有为它接线**（switch 里无对应 case），配置后会落入 default 分支抛 `UnsupportedOperationException("unknown database type.")`，因此**当前版本不可用**，本文不再将其作为可用后端介绍。

## MongoDB

`DatabaseMongoDb` 是文档存储后端。

```xml
<DatabaseConf Name="default"
              DatabaseType="MongoDB"
              DatabaseUrl="mongodb://localhost:27017"
              DatabaseName="zeze_mongodb"/>
```

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `DatabaseUrl` | — | MongoDB 连接串，如 `mongodb://localhost:27017` |
| `DatabaseName` | `zeze_mongodb` | 数据库名 |

依赖：

```xml
<dependency>
    <groupId>org.mongodb</groupId>
    <artifactId>mongodb-driver-sync</artifactId>
    <version>5.8.0</version>
</dependency>
```

## Redis

`DatabaseRedis` 适合高吞吐热点读写，KV 内存级访问。

```xml
<DatabaseConf Name="hotdata"
              DatabaseType="Redis"
              DatabaseUrl="redis://localhost:6379"/>
```

| 属性 | 说明 |
|------|------|
| `DatabaseUrl` | Redis 连接串，如 `redis://localhost:6379` |

依赖：

```xml
<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
    <version>5.2.0</version>
</dependency>
```

## TiKV

`DatabaseTikv` 是分布式事务 KV，支持水平扩展与分布式事务。

```xml
<DatabaseConf Name="default"
              DatabaseType="Tikv"
              DatabaseUrl="172.21.15.68:2379"
              distTxn="true"/>
```

| 属性 | 说明 |
|------|------|
| `DatabaseUrl` | PD 地址，如 `172.21.15.68:2379` |
| `distTxn` | 设为 `true` 启用分布式事务（**属性名全小写 `distTxn`**，写成 `DistTxn` 不会被解析；值大小写不敏感） |

依赖：

```xml
<dependency>
    <groupId>org.tikv</groupId>
    <artifactId>tikv-client-java</artifactId>
    <version>3.3.5</version>
</dependency>
```

## DynamoDB（当前版本不可用）

源码中存在 `DatabaseDynamoDb` 实现类与 `DbType.DynamoDb` 枚举值，但 `Config.createDatabase` 的工厂 switch **没有 `case DynamoDb`**，配置后会抛 `UnsupportedOperationException("unknown database type.")`。**当前版本下 DynamoDB 不可配置使用**，请勿在 `DatabaseType` 中使用 `DynamoDB`。后续版本若完成接线，本文会补充其配置说明。

## 依赖坐标汇总

| 依赖 | 版本 | 用于 |
|------|------|------|
| `org.mongodb:mongodb-driver-sync` | `5.8.0` | MongoDB |
| `redis.clients:jedis` | `5.2.0` | Redis |
| `org.tikv:tikv-client-java` | `3.3.5` | TiKV |

## 选型建议

| 场景 | 推荐 | 理由 |
|------|------|------|
| 高吞吐热点读写 | `Redis` | 内存级 KV，延迟极低 |
| 超大规模分布式，需分布式事务 | `Tikv` | 水平扩展，原生支持 `distTxn` |
| 文档型存储，灵活模式 | `MongoDB` | 文档存储，字段可变 |

## 相关文档

- 数据库总览：[./db-overview.md](./db-overview.md)
- 配置参考：[./configuration.md](./configuration.md)
