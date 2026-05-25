---
title: "Dbh2 分布式数据库"
sidebar:
  order: 5
---

## 概述

Dbh2 是 Zeze 专有的分布式 KV 数据库，基于 RocksDB 和 Raft 共识算法构建。它不支持冲突记录的并发写（由 Zeze 事务框架保护），支持并发读取。其设计目标是提供低成本、高可靠、高分布的存储系统。

## 主要特性

- 一个 Dbh2 集群支持多个 Database，每个 Database 包含多张表
- 每张表为 KV 表，按 Key 的顺序自动分段到多个桶（Bucket）内
- 每个桶由 Raft 节点组成，提供高可用性
- 支持自动分桶（Split）和整桶迁移（Move）
- 具有动态扩容能力，负载监控自动触发再平衡

## 架构组件

### Master

Master 知道所有数据库、所有表、所有桶的分布情况。提供表创建和桶信息查询功能。

### Dbh2Manager

桶运行容器，每个 Manager 包含多个桶（Bucket），是主要的数据负载实现点。Manager 启动时从本地 `raft.xml` 文件加载已有的桶，并向 Master 注册自身信息。

### Dbh2Agent

嵌入到 Zeze 应用中，作为 Dbh2 的客户端执行数据库操作。

### CommitServer

可选的事务提交服务，将 Dbh2Agent 的事务提交功能移到独立进程处理，减轻应用端负担。

## Bucket 分桶机制

Bucket 是 Dbh2 的核心数据单元，每个桶管理一张表在一个 Key 范围内的记录。

### 内部结构

- **data**: 主数据表，存储 KV 记录
- **trans**: 事务表，存储进行中的事务信息
- **meta**: 元数据表，存储桶的元信息（`BBucketMeta`）、分桶状态和事务 ID 分配器
- **batch**: 批量写入对象，复用以提高性能

### 桶的范围管理

每个桶维护一个 Key 范围 `[keyFirst, keyLast)`，通过 `inBucket(key)` 判断记录是否属于当前桶。`keyLast` 为空时表示范围无上界。

### 自动分桶流程

1. `loadMonitor` 定时检查各桶负载
2. 负载超过 `splitLoad` 阈值时触发 `tryStartSplit(false)`（分桶）
3. 未达分桶条件但总负载较高时触发 `tryStartSplit(true)`（迁移）
4. 分桶过程：定位中间 Key -> 创建新桶 -> 数据复制 -> 原子切换元数据
5. 分桶期间新事务写入会同步到目标桶（`onCommitBatch`）

## Raft 集成

### Dbh2StateMachine

`Dbh2StateMachine` 继承自 `Zeze.Raft.StateMachine`，是 Dbh2 的 Raft 状态机实现。主要职责：

- **日志应用**: 处理 `LogPrepareBatch`、`LogCommitBatch`、`LogUndoBatch` 等日志类型
- **分桶日志**: 处理 `LogEndSplit`、`LogSetSplittingMeta`、`LogSplitPut`、`LogEndMove`
- **快照管理**: 通过 RocksDB Checkpoint 实现快照的创建与恢复
- **事务管理**: 维护进行中的事务集合，超时未提交的事务自动回滚
- **负载统计**: 实时统计 Get/Put/Delete 等操作的 QPS，供负载监控使用

### 请求调度

- **查询请求**（Get、Walk、WalkKey）: 允许并发执行
- **Prepare 请求**: 分桶期间放入队列顺序执行，避免数据不一致
- **其他请求**: 在 Raft UserThread 中串行执行

## 配置方式

### 客户端配置

在 `zeze.xml` 中配置 Dbh2 数据库连接：

```xml
<DatabaseConf Name=""
    DatabaseType="Dbh2"
    DatabaseUrl="dbh2://127.0.0.1:10999/dbh2_database"/>
```

- **DatabaseUrl 格式**: `dbh2://<master_host>:<master_port>/<database_name>`
- `master_host:master_port`: Master 服务的地址和端口
- `database_name`: 数据库名称

### Master 配置

```xml
<?xml version="1.0" encoding="utf-8"?>
<zeze
    GlobalCacheManagerHostNameOrAddress=""
    GlobalCacheManagerPort="5002"
    CheckpointPeriod="60000" ServerId="0"
    CheckpointMode="Table" CheckpointFlushMode="SingleThread"
    CheckpointModeTableFlushConcurrent="4"
    ServiceManager="">
    <DatabaseConf Name="" DatabaseType="Memory" DatabaseUrl=""/>
    <ServiceConf Name="Zeze.Services.ServiceManager.Agent">
        <Connector HostNameOrAddress="127.0.0.1" Port="5001"/>
    </ServiceConf>
    <ServiceConf Name="Zeze.Dbh2.Master">
        <Acceptor Ip="127.0.0.1" Port="10999"/>
    </ServiceConf>
    <ServiceConf Name="Zeze.Dbh2.Master.Agent">
        <Connector HostNameOrAddress="127.0.0.1" Port="10999"/>
    </ServiceConf>
    <ServiceConf Name="Zeze.Raft.ProxyServer">
        <Acceptor Ip="127.0.0.1" Port="0"/>
    </ServiceConf>
    <ServiceConf Name="Zeze.Dbh2.Commit">
        <Acceptor Ip="127.0.0.1" Port="7788"/>
    </ServiceConf>
</zeze>
```

### Manager 配置

```xml
<?xml version="1.0" encoding="utf-8"?>
<zeze
    GlobalCacheManagerHostNameOrAddress=""
    GlobalCacheManagerPort="5002"
    CheckpointPeriod="60000" ServerId="0"
    CheckpointMode="Table" CheckpointFlushMode="SingleThread"
    CheckpointModeTableFlushConcurrent="4"
    ServiceManager="disable">
    <DatabaseConf Name="" DatabaseType="Memory" DatabaseUrl=""/>
    <ServiceConf Name="Zeze.Dbh2.Master">
        <Acceptor Ip="127.0.0.1" Port="10999"/>
    </ServiceConf>
    <ServiceConf Name="Zeze.Dbh2.Master.Agent">
        <Connector HostNameOrAddress="127.0.0.1" Port="10999"/>
    </ServiceConf>
    <ServiceConf Name="Zeze.Raft.ProxyServer">
        <Acceptor Ip="127.0.0.1" Port="7780"/>
    </ServiceConf>
    <ServiceConf Name="Zeze.Dbh2.Commit">
        <Acceptor Ip="127.0.0.1" Port="7788"/>
    </ServiceConf>
</zeze>
```

## 启动脚本

```bash
# 启动 Master
nohup java -Dlogname=master -Xmx4g -cp .:lib/* Zeze.Dbh2.Master.Main zeze.xml&

sleep 2

# 启动 Manager（需要 3 个组成 Raft 集群）
nohup java -Dlogname=manager0 -Xmx4g -cp .:lib/* Zeze.Dbh2.Dbh2Manager manager0 zeze0.xml &
nohup java -Dlogname=manager1 -Xmx4g -cp .:lib/* Zeze.Dbh2.Dbh2Manager manager1 zeze1.xml &
nohup java -Dlogname=manager2 -Xmx4g -cp .:lib/* Zeze.Dbh2.Dbh2Manager manager2 zeze2.xml &
```

Manager 启动参数：`Dbh2Manager <home> <configXml> [-selector <n>]`

- **home**: Manager 数据存储根目录
- **configXml**: 配置文件路径
- **-selector**: Netty Selector 线程数（默认 1）

## Commit 服务

Commit 服务是可选组件，负责处理分布式事务的提交确认。`Commit` 类内部使用 `CommitRocks`（基于 RocksDB）持久化事务状态，通过 `CommitService` 提供网络服务。

- `ProcessCommitRequest`: 提交事务
- `ProcessQueryRequest`: 查询事务状态（`Critical` 调度模式，保证查询时序）
