---
title: "ServiceManager 服务发现"
sidebar:
  order: 1
---

ServiceManager 是 Zeze 框架的**服务注册与发现**中心。它管理集群中所有动态服务（如 GameServer）的注册信息，并通知订阅者（如 Linkd）服务列表的变更。服务发现是 Provider-Linkd 架构的基础设施，确保请求分发器始终持有最新的可用服务列表。

## 核心概念

- **动态服务**：启用 cache-sync 的逻辑服务器（如 gs），运行时向 ServiceManager 注册自身。
- **订阅者**：需要使用动态服务列表的组件（如 Linkd），订阅后会接收服务变更通知。
- **服务状态**：每个服务由 `serviceName`、`serviceIdentity`、`passiveIp`、`passivePort`、`version` 等字段描述。

## API 概述

### RegisterService (EditService)

服务启动时通过 `EditService` 协议向 ServiceManager **注册**自身信息，支持批量增加和移除。注册操作是幂等的，断线重连后可以安全重复注册。

```java
// 服务注册示例
var edit = new EditService();
edit.Argument.getAdd().add(new BServiceInfo(
    "GameServer",     // serviceName
    "server1",        // serviceIdentity
    1,                // version
    "192.168.1.10",   // passiveIp
    8080,             // passivePort
    ""                // extraInfo
));
```

### SubscribeService (Subscribe)

订阅者通过 `Subscribe` 协议向 ServiceManager **订阅**指定服务的变更通知。订阅时可以指定版本号（version=0 表示订阅所有版本），返回当前已注册的服务快照。

```java
var sub = new Subscribe();
sub.Argument.subs.add(new BSubscribeInfo("GameServer", 0)); // 0 = 订阅所有版本
```

### UnSubscribeService

订阅者不再需要服务列表时，通过 `UnSubscribe` 取消订阅。

### OfflineRegister / OfflineNotify

服务器可注册**离线通知**，当某个服务异常断连时，ServiceManager 会在延迟（默认 600 秒）后通知其他注册了同一 `notifyId` 的服务。

### AnnounceServers

服务器通过 `AnnounceServers` 向 ServiceManager 声明自己正在监视哪些其他服务器。被监视的服务器离线后，ServiceManager 会触发通知。

## 订阅模式

ServiceManager 支持 **Simple** 订阅模式。订阅者通过 `Subscribe` 请求注册后，当有服务注册或注销变更时，ServiceManager 向所有匹配的订阅者发送 `EditService` 通知，内容包含新增和移除的服务列表。

通知的版本过滤逻辑：如果订阅者指定了特定 `version`，则只收到该版本的服务变更；`version=0` 表示接收所有版本的变更。

## 运行模式

### 单机模式 (ServiceManagerServer)

单机模式下，ServiceManager 作为一个独立进程运行，通过 TCP 监听服务端口（默认 5001）。所有服务状态保存在内存中，AutoKey 分配使用本地 RocksDB 持久化。

```bash
java -cp ... Zeze.Services.ServiceManagerServer -port 5001
```

启动参数：

| 参数 | 说明 |
|------|------|
| `-ip` | 绑定 IP 地址（可选） |
| `-port` | 监听端口（默认 5001） |
| `-autokeys` | AutoKey 数据库目录（默认 "autokeys"） |

### Raft 模式 (ServiceManagerWithRaft)

Raft 模式使用 `ServiceManagerWithRaft` 实现，将服务状态持久化到 Raft 集群的 RocksDB 中，提供高可用性。主节点故障后，从节点自动接管。

```bash
# 启动单个 Raft 节点
java -cp ... Zeze.Services.ServiceManagerServer -raft 127.0.0.1:6556 -raftConf servicemanager.raft.xml

# 本地启动所有 Raft 节点（开发调试用）
java -cp ... Zeze.Services.ServiceManagerServer -raft RunAllNodes -raftConf servicemanager.raft.xml
```

Raft 模式下的客户端使用 `ServiceManagerAgentWithRaft`，它内部通过 Raft Agent 连接集群，自动处理 Leader 切换和请求重发。

## XML 配置

ServiceManager 的自定义配置通过 `Zeze.Services.ServiceManager` 节点加载：

```xml
<Zeze.Services.ServiceManager
    KeepAlivePeriod="30000"
    RetryNotifyDelayWhenNotAllReady="30000"
    DbHome="."
    ThreadingReleaseTimeout="1800000" />
```

| 属性 | 说明 |
|------|------|
| `KeepAlivePeriod` | 心跳检测周期（毫秒），-1 表示禁用 |
| `RetryNotifyDelayWhenNotAllReady` | 通知未全部 Ready 时重试延迟 |
| `DbHome` | AutoKey 数据库主目录 |
| `ThreadingReleaseTimeout` | 线程资源释放超时 |

## KeepAlive 机制

当配置了 `KeepAlivePeriod > 0` 时，ServiceManager 会定期向所有连接的客户端发送 `KeepAlive` 请求。如果发送失败，会关闭连接并触发会话清理（注销该连接上的所有注册和订阅）。

## 负载广播

ServiceManager 支持**服务器负载**的广播机制。服务注册或订阅时，自动建立负载观察关系。服务器通过 `SetServerLoad` 上报负载数据后，ServiceManager 将负载信息转发给所有观察者。

## 与其他组件的关系

- **GlobalCacheManager**（[缓存同步](./global-cache-manager.md)）：ServiceManager 为其提供服务发现能力。
- **Raft**（[Raft 共识](./raft.md)）：ServiceManagerWithRaft 依赖 Raft 实现高可用。
