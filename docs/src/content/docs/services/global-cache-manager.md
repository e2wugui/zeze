---
title: "GlobalCacheManager 缓存同步"
sidebar:
  order: 2
---

GlobalCacheManager（GCM）是 Zeze **缓存一致性**协议的核心组件。在分布式事务中，多个服务器可能同时缓存同一份数据，GCM 通过权限模型（Modify/Share/Invalid）协调各服务器的缓存状态，保证事务的正确性。GCM 是 Zeze 实现无死锁乐观锁事务的关键基础设施。

## 缓存同步协议

GCM 使用三种权限状态管理缓存记录的访问控制：

- **Modify**：排他写权限。同一时刻只有一台服务器可以持有 Modify 权限。持有 Modify 的服务器可以直接修改数据而无需通知其他服务器。
- **Share**：共享读权限。多台服务器可以同时持有 Share 权限，但此时任何服务器都不能修改数据。
- **Invalid**：无权限。服务器不持有该记录的任何缓存。

### Acquire 协议

服务器在事务执行过程中通过 `Acquire` 请求向 GCM 申请权限。申请 Modify 时，GCM 需要先向当前持有者发送 **Reduce**（降级）请求，将其权限降低到 Share 或 Invalid。

```java
// 在 GlobalAgent.acquire 中，根据 globalKey 的哈希值选择对应的 GCM Agent
var agent = agents[getGlobalCacheManagerHashIndex(gkey)];
var rpc = new Acquire(gkey, state); // state 为 Share 或 Modify
rpc.SendForWait(socket, acquireTimeout).get();
```

### Reduce 协议

当 GCM 需要降级某台服务器的权限时，向该服务器发送 `Reduce` 请求。服务器收到后，在本地执行缓存降级操作：

- **ReduceInvalid**：完全释放缓存记录。
- **ReduceShare**：将 Modify 降级为 Share。

降级结果返回给 GCM 后，GCM 再将权限授予申请者。

### 权限转换流程

典型的 Modify 申请流程：

1. 服务器 A 向 GCM 发送 `Acquire(Modify)` 请求
2. GCM 发现服务器 B 当前持有 Modify 权限
3. GCM 向服务器 B 发送 `Reduce(Invalid)` 请求
4. 服务器 B 执行本地缓存降级，返回成功
5. GCM 将 Modify 权限授予服务器 A，返回成功

## 运行模式

### 单机同步模式 (GlobalCacheManagerServer)

单机同步模式是基础实现，所有 Acquire 和 Reduce 操作在同步锁（`ReentrantLock` + `Condition`）下执行。当发生权限降级时，GCM 线程会阻塞等待远程服务器的 Reduce 响应。

```bash
java -cp ... Zeze.Services.GlobalCacheManagerServer -port 5002
```

适合服务器数量较少、网络延迟稳定的场景。

### 异步模式 (GlobalCacheManagerAsyncServer)

异步模式使用 `AsyncLock` 替代传统的 `ReentrantLock`，在等待远程 Reduce 响应时不阻塞线程，而是通过回调机制继续处理。这使得 GCM 能够处理更高的并发量。

所有协议以 `DispatchMode.Direct` 方式在 IO 线程上直接执行，避免线程切换开销。

```bash
java -cp ... Zeze.Services.GlobalCacheManagerAsyncServer -port 5002
```

### Raft 模式 (GlobalCacheManagerWithRaft)

Raft 模式将 GCM 的状态（权限分配记录、服务器会话信息）持久化到 Raft 集群的 RocksDB 中。使用**悲观锁**（`RocksMode.Pessimism`）保证事务的原子性。

Raft 模式下 Reduce 操作使用 `reduceWaitLater` 异步发送并等待结果，不阻塞 Raft 事务线程。

```bash
# 单节点
java -cp ... Zeze.Services.GlobalCacheManagerServer -raft 127.0.0.1_5556 -raftConf global.raft.xml

# 本地启动所有节点
java -cp ... Zeze.Services.GlobalCacheManagerServer -raft RunAllNodes -raftConf global.raft.xml
```

## 客户端 (GlobalAgent)

`GlobalAgent` 是 GCM 的客户端代理，管理与一个或多个 GCM 实例的连接。它根据 `globalKey` 的哈希值将请求路由到对应的 GCM Agent，支持多 GCM 实例的水平扩展。

```java
// GlobalAgent 根据哈希选择 Agent
public int getGlobalCacheManagerHashIndex(Binary gkey) {
    return Integer.remainderUnsigned(gkey.hashCode(), agents.length);
}
```

GlobalAgent 负责处理 Reduce 请求：收到 Reduce 后，找到本地的 Table 并调用 `reduceInvalid` 或 `reduceShare` 执行本地缓存降级。

## AchillesHeel 守护

GCM 内置 **AchillesHeel** 守护机制，定期检测服务器连接活跃度。如果某台服务器超过 `globalDaemonTimeout` 未活跃，GCM 会主动踢掉该服务器的连接并释放其所有权限。这防止了服务器异常退出后权限永久无法释放的问题。

## 性能影响

缓存同步是分布式事务的主要开销来源：

- **Share 申请**：通常只需一次网络往返（与 GCM 通信）
- **Modify 申请**：至少需要两次网络往返（向 GCM 申请 + GCM 向持有者 Reduce）
- **Reduce 超时**：如果 Reduce 请求超时，申请会失败并触发事务重试

## XML 配置

```xml
<GlobalCacheManager
    InitialCapacity="10000000"
    MaxNetPing="3000"
    ServerProcessTime="14000"
    ServerReleaseTimeout="60000" />
```

| 属性 | 说明 |
|------|------|
| `InitialCapacity` | 全局缓存状态 HashMap 初始容量 |
| `MaxNetPing` | 最大网络延迟（毫秒） |
| `ServerProcessTime` | 服务器处理时间阈值 |
| `ServerReleaseTimeout` | 服务器释放超时时间 |

## 与其他组件的关系

- **ServiceManager**（[服务发现](./service-manager.md)）：GCM 可以独立运行，不依赖 ServiceManager。
- **Raft**（[Raft 共识](./raft.md)）：Raft 模式下 GCM 通过 Raft 实现高可用和数据持久化。
