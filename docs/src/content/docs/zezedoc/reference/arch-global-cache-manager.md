---
title: "GlobalCacheManager 缓存同步"
description: "Zeze 分布式缓存一致性协议核心组件的权限模型与协调机制"
category: reference
order: 15
---

> 本文档描述 Zeze 缓存一致性协议核心组件 `GlobalCacheManager`（GCM）的三种权限状态、Acquire/Reduce 协议、运行模式与性能特征，供分布式缓存协调开发检索参考。

## 定位

GCM 是缓存一致性协议的核心组件。分布式事务中多服务器缓存同一份数据时，GCM 通过权限模型协调，保证无死锁乐观锁事务。

## 三种权限状态

业务视角的三种逻辑状态：

| 状态 | 值 | 说明 |
|------|----|------|
| `Invalid` | `0` | 无权限 |
| `Share` | `1` | 共享读，多台可持有但不能改 |
| `Modify` | `2` | 排他写，同时只有一台持有，可直接修改无需通知 |

此外还有两个内部过渡状态：`StateRemoving = 3`（正在回收）与 `StateRemoved = 10`（最终态），业务一般不直接接触。

## Acquire 协议（申请权限）

服务器事务执行中通过 `Acquire` 向 GCM 申请权限。申请 `Modify` 时，GCM 需先向当前持有者发 `Reduce` 降级。

```java
// GlobalAgent 本身没有 send 方法；等待式调用用 Rpc 的 SendForWait(...).get()
Acquire acquire = new Acquire(gkey, state);
acquire.SendForWait(
    agents[getGlobalCacheManagerHashIndex(gkey)].getSocket(),
    acquireTimeout
).get();
```

## Reduce 协议（降级权限）

GCM 降级某台权限时发 `Reduce`，服务器本地降级，降级结果返回 GCM 再授予申请者。

| Reduce 类型 | 说明 |
|-------------|------|
| `ReduceInvalid` | 完全释放 |
| `ReduceShare` | Modify 降为 Share |

## 权限转换流程（典型 Modify 申请）

```
1. A 向 GCM 发 Acquire(Modify)
2. GCM 发现 B 持有 Modify
3. GCM 向 B 发 Reduce(Invalid)
4. B 本地降级，返回成功
5. GCM 授予 A Modify
```

## 运行模式

### 单机同步：GlobalCacheManagerServer

同步锁 `ReentrantLock` + `Condition`，降级时阻塞等待。

```bash
java -cp Zeze.Services.GlobalCacheManagerServer -port 5002
```

### 异步：GlobalCacheManagerAsyncServer

`AsyncLock` 不阻塞线程，回调驱动，`DispatchMode.Direct`（IO 线程直接执行）。

### Raft：GlobalCacheManagerWithRaft

状态持久化（Raft + RocksDB），悲观锁 `RocksMode.Pessimism`，`Reduce` 用 `reduceWaitLater` 异步处理。

## 客户端：GlobalAgent

管理多个 GCM 连接，按 `globalKey` 哈希路由。

| 接口 | 说明 |
|------|------|
| `getGlobalCacheManagerHashIndex(gkey)` | `Integer.remainderUnsigned(gkey.hashCode(), agents.length)` |
| 收到 `Reduce` | 找本地 Table，调 `reduceInvalid` / `reduceShare` |

## AchillesHeel 守护

定期检测服务器连接活跃度，超 `globalDaemonTimeout` 则踢掉并释放权限，防止异常退出导致权限永久无法释放。

## 性能影响

| 操作 | 网络往返次数 |
|------|-------------|
| `Share` 申请 | 通常 1 次 |
| `Modify` 申请 | 至少 2 次（向 GCM 申请 + GCM 向持有者 Reduce） |
| `Reduce` 超时 | 申请失败，触发事务重试 |

## XML 配置

```xml
<GlobalCacheManager InitialCapacity="..."
                    MaxNetPing="..."
                    ServerProcessTime="..."
                    ServerReleaseTimeout="..."/>
```

## 组件关系

| 关系 | 说明 |
|------|------|
| 与 ServiceManager | GCM 可独立运行，不依赖 ServiceManager |
| 与 Raft | Raft 模式下实现高可用与持久化 |

## 相关文档

- 工作原理：[../manual/02-how-zeze-works.md](../manual/02-how-zeze-works.md)
- 分布式入门：[../manual/05-going-distributed.md](../manual/05-going-distributed.md)
- 服务发现：[./arch-service-manager.md](./arch-service-manager.md)
- Raft 服务：[./svc-raft.md](./svc-raft.md)
- 表与存储：[./table.md](./table.md)
