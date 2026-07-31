---
title: "Dbh2 分布式数据库"
description: "Zeze 自研分布式数据库 Dbh2：分桶、Raft 复制与多数据中心同步"
category: reference
order: 24
---

> 本文档说明 Zeze 自研的分布式数据库抽象层 Dbh2（`Zeze.Dbh2`），包括配置方式、核心组件（Dbh2 / Dbh2Manager / Commit / Bucket）、分桶与 Raft 复制机制，供需要 Zeze 原生分布式存储或多数据中心同步的场景参考。

## 概述

Dbh2 是 Zeze **自研的分布式数据库抽象层**，内置**分桶（Bucket）**和**复制**机制，支持多数据中心同步。它对外提供与 `Database` 抽象一致的接口，业务代码无感知。

| 特性 | 说明 |
|------|------|
| 来源 | Zeze 自研（`Zeze.Dbh2` 包） |
| 架构 | 分布式，内置分桶与复制 |
| 复制 | 通过 Raft 实现高可用 |
| 同步 | 支持多数据中心 |

## 配置

```xml
<DatabaseConf Name="default"
              DatabaseType="Dbh2"
              DatabaseUrl="dbh2://127.0.0.1:10999/dbh2_unittest"/>
```

| 属性 | 说明 |
|------|------|
| `DatabaseUrl` | Dbh2 协议地址，格式 `dbh2://<host>:<port>/<database_name>` |

## 核心组件

| 组件 | 职责 |
|------|------|
| `Dbh2` | 数据库实例，对外提供 `Database` 接口 |
| `Dbh2Manager` | 管理**桶（Bucket）**的生命周期与分布（管桶，不是管多个 Dbh2 实例） |
| `Master` | 桶的主节点，负责读写与 Raft 复制 |
| `Commit` | 提交事务，`Dbh2LocalCommit` 用于本地提交配置 |
| `Bucket` | 分桶，按 key 顺序范围分段的数据分片 |

## 分桶（Bucket）机制

数据按**桶（Bucket）**分布到多个节点。每个桶负责**一段连续的 key 范围**（按 key 顺序分段，而非按 key 哈希），桶可以 Split（分裂）和 Move（迁移）以实现负载均衡。

| 维度 | 说明 |
|------|------|
| 数据分布 | 按 key 顺序**范围分段**分配到不同桶（非哈希） |
| 桶操作 | 支持 Split（分裂）、Move（迁移到其他节点） |
| 桶数量 | 可水平扩展，桶分布在多节点 |
| 扩容 | 通过桶分裂与迁移实现 |

## Raft 集成

每个桶通过 **Raft** 实现高可用复制：

| 维度 | 说明 |
|------|------|
| 复制方式 | 每个桶是一个 Raft 复制组 |
| 高可用 | Leader 故障时从节点自动接管 |
| 一致性 | Raft 保证桶内数据强一致 |

Raft 实现细节参见 [./svc-raft.md](./svc-raft.md)。

## Dbh2LocalCommit 配置

`Dbh2LocalCommit` 用于本地提交事务的配置，确保本地与远程数据的一致性。

## 适用场景

| 场景 | 选择 |
|------|------|
| 多数据中心同步 | ✅ Dbh2 原生支持 |
| 需要 Zeze 原生分布式存储 | ✅ 无需引入第三方依赖 |
| 不想依赖 TiKV 等外部分布式 KV | ✅ 自研，栈内可控 |

## 相关文档

- 数据库总览：[./db-overview.md](./db-overview.md)
- Raft 共识实现：[./svc-raft.md](./svc-raft.md)
