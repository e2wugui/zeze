---
title: "Raft 共识实现"
description: "Zeze.Raft 包实现的 Raft 共识算法，用于 GlobalCacheManager 与 ServiceManager 的高可用"
category: reference
order: 33
---

> 本文档说明 `Zeze.Raft` 包实现的 Raft 共识算法，包括核心组件、Leader 选举与日志复制、用于 GlobalCacheManager 和 ServiceManager 高可用的两种应用模式、启动命令与配置，供部署高可用状态服务参考。

## 概述

`Zeze.Raft` 包实现了 Raft 共识算法，用于 **GlobalCacheManager** 和 **ServiceManager** 的高可用。核心思想：通过 Leader 选举 + 日志复制 + 快照，保证集群状态强一致；主故障时从节点自动接管。

## 核心组件

| 组件 | 职责 |
|------|------|
| `Raft` | Raft 算法主体 |
| `Server` | Raft 服务端节点 |
| `Agent` | 客户端 Agent，与 Raft 集群交互 |
| `StateMachine` | 状态机**抽象类**（非接口），应用层继承实现，把日志应用到业务状态 |
| `Log` | 日志条目，承载状态变更 |
| `RaftConfig` | Raft 配置 |

## Raft 核心机制

| 机制 | 说明 |
|------|------|
| Leader 选举 | 集群选出唯一 Leader 处理写请求 |
| 日志复制 | Leader 把日志复制到多数从节点后提交 |
| 快照 | 定期对状态机做快照，压缩日志，加速恢复 |

## 应用模式

### 1. GlobalCacheManagerWithRaft

用于 GlobalCacheManager 的高可用：

| 维度 | 说明 |
|------|------|
| 用途 | 状态持久化，GlobalCacheManager 高可用 |
| 存储 | 状态持久化到 Raft |
| 锁 | RocksDB 悲观锁（`RocksMode.Pessimism`） |

### 2. ServiceManagerWithRaft

用于 ServiceManager 的高可用：

| 维度 | 说明 |
|------|------|
| 用途 | 服务状态持久化，高可用 |
| 故障切换 | 主故障时从节点自动接管 |
| 客户端 | `ServiceManagerAgentWithRaft` 自动 Leader 切换，请求重发 |

## 启动

> ⚠️ **注意**：下面的 `-raft` / `-raftConf` / `RunAllNodes` **不是通用 Raft 库的参数**，而是 `GlobalCacheManagerServer` / `ServiceManagerServer` 这两个**具体服务**的专属启动参数。Raft 库本身由使用方在代码中组装，不存在独立的「启动 Raft 节点」命令。

```bash
# GlobalCacheManager / ServiceManager 启动高可用模式（示例）
java -cp ... Zeze.Services.GlobalCacheManagerServer \
    -raft 127.0.0.1:5556 -raftConf raft.xml

# 开发/测试：单进程启动配置中的所有节点
java -cp ... Zeze.Services.GlobalCacheManagerServer -raft RunAllNodes
```

| 参数 | 归属 | 说明 |
|------|------|------|
| `-raft <ip>:<port>` | GCM / ServiceManager | Raft 监听地址 |
| `-raftConf <file>` | GCM / ServiceManager | Raft 配置文件 |
| `-raft RunAllNodes` | GCM / ServiceManager | 单进程启动配置中的所有节点（开发/测试用） |

## Raft 配置文件

Raft 配置用 `<node>` 元素，属性是 `Host` 和 `Port`（**不是** `<host name ip port/>`）：

```xml
<raft>
    <node Name="node1" Host="127.0.0.1" Port="5556"/>
    <node Name="node2" Host="127.0.0.1" Port="5557"/>
    <node Name="node3" Host="127.0.0.1" Port="5558"/>
</raft>
```

> 各节点的配置需要**包含相同的节点集合**（`sortedNames` 集合一致即可），但**不要求 XML 中书写的物理顺序完全一致**——框架按节点名排序后比较。只要集合相同，选举与日志复制即可正常工作。

## 线程安全与热更新

> `hotGuard` 是 Zeze 通用热更新机制的保护锁，**与 Raft 无直接关系**——它保护的是任意热更过程，而非 Raft 特有的并发。Raft 自身的线程安全由其内部实现保证。详见 [热更新](./advanced-hot-reload.md)。

## 相关文档

- GlobalCacheManager：[./arch-global-cache-manager.md](./arch-global-cache-manager.md)
- ServiceManager：[./arch-service-manager.md](./arch-service-manager.md)
- Dbh2 分布式数据库：[./db-dbh2.md](./db-dbh2.md)
