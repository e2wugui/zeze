---
title: "RocksDB 嵌入式引擎"
description: "Zeze 嵌入式 RocksDB 引擎：高性能本地存储、单机限制与本地缓存一致性"
category: reference
order: 23
---

> 本文档说明 Zeze 的 RocksDB 嵌入式存储引擎：配置方式、单机模式限制（不支持 GlobalCacheManager）、本地缓存层用途与「本地 RocksDB 一致性原则」，供单机高性能场景参考。

## 概述

`DatabaseRocksDb` 是嵌入式 KV 存储引擎，**性能极高、无网络开销、读写延迟极低**。数据直接读写本地磁盘上的 RocksDB 实例。

| 特性 | 说明 |
|------|------|
| 类型 | 嵌入式 KV 存储 |
| 网络 | 无，纯本地 |
| 延迟 | 极低 |
| 持久化 | ✅ 本地磁盘 |
| 分布式 | ❌ 单机 |

## 配置

```xml
<DatabaseConf Name="default"
              DatabaseType="RocksDB"
              DatabaseUrl="/data/zeze/rocksdb"/>
```

> ⚠️ **注意大小写**：`DatabaseType` 必须写成全大写的 **`RocksDB`**（解析字符串大小写敏感）。写成 `RocksDb` 会匹配不到分支，抛 `unknown database type`。

| 属性 | 说明 |
|------|------|
| `DatabaseType` | 固定写 **`RocksDB`**（全大写，大小写敏感） |
| `DatabaseUrl` | **本地目录路径**，RocksDB 数据文件存放位置，如 `/data/zeze/rocksdb` |

依赖：

```xml
<dependency>
    <groupId>org.rocksdb</groupId>
    <artifactId>rocksdbjni</artifactId>
    <version>10.10.1.1</version>
</dependency>
```

## 重要限制

> ⚠️ **RocksDB 不支持 `GlobalCacheManager`。**

RocksDB 是嵌入式存储，无法多实例共享同一份数据。配置 `GlobalCacheManagerHostNameOrAddress` 时使用 RocksDB 会**抛异常**，因为它无法参与多服务器之间的缓存同步。

| 限制 | 说明 |
|------|------|
| 多实例共享 | ❌ 不支持，嵌入式存储无法跨进程共享 |
| `GlobalCacheManager` | ❌ 不支持，配置后抛异常 |
| 适用规模 | 单机 |

## 本地缓存层

RocksDB 常用于「本地缓存层」场景：每张表维护一个 `localRocksCacheTable`，value 以 `SoftReference`（软引用）持有。当 GC 回收软引用后，数据从本地 RocksDB 恢复。

## 本地 RocksDB 一致性原则

本地 RocksDB 与后端数据库必须保持一致。原则是「与后端数据库一致」：

| 操作 | 一致性要求 |
|------|------------|
| 写入 | 同时写**远程**和**本地** |
| 加载 | 远程加载后写**本地** |
| 删除 | 同步删**本地** |
| 分布式失效 | 其他实例改记录后，本实例记录降级为 `Invalid`，下次访问从远程重载并覆盖本地 |

上述四条构成「恢复不变式」：无论 GC 回收软引用还是远程失效，本地数据最终都能从远程正确恢复。

## 适用场景

| 场景 | 选择 |
|------|------|
| 单机高性能服务 | ✅ 嵌入式，无网络开销 |
| 本地缓存层（远程数据库的本地副本） | ✅ SoftReference + 本地 RocksDB 恢复 |

## 相关文档

- 数据库总览：[./db-overview.md](./db-overview.md)
- Table 参考：[./table.md](./table.md)
- 选型指南：[../manual/06-choosing-database.md](../manual/06-choosing-database.md)
