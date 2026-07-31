---
title: "ServiceManager 服务发现"
description: "Zeze 集群动态服务注册与发现中心及其通知机制"
category: reference
order: 16
---

> 本文档描述 Zeze 服务注册与发现中心 `ServiceManager` 的核心概念、API、订阅模式、运行模式与 KeepAlive 机制，供集群服务管理开发检索参考。

## 定位

`ServiceManager` 是服务注册与发现中心，管理集群动态服务注册信息，通知订阅者服务列表变更，是 Provider-Linkd 架构的基础设施。

## 核心概念

| 概念 | 说明 |
|------|------|
| 动态服务 | 启用 cache-sync 的逻辑服务器（如 `gs`），运行时注册 |
| 订阅者 | 需要服务列表的组件（如 Linkd） |
| 服务状态 | `serviceName` / `serviceIdentity` / `passiveIp` / `passivePort` / `version` |

## API

> ServiceManager 的客户端调用都通过 `AbstractAgent`（`Zeze.Services.ServiceManager.AbstractAgent`）进行。下面给出几个最常用方法的真实签名。

### registerService（注册单个服务）

```java
// AbstractAgent.registerService(BServiceInfo info)
agent.registerService(
    new BServiceInfo(
        "GameServer",        // serviceName
        "1",                 // serviceIdentity
        100,                 // version
        "192.168.1.10",      // ip
        5555,                // port
        Binary.Empty         // extraInfo，类型是 Zeze.Net.Binary（不是 String）
    )
);
```

> ⚠️ **注意**：`BServiceInfo` 的最后一个参数 `extraInfo` 是 **`Zeze.Net.Binary`** 类型，传 `""`（String）无法编译。不需要时传 `Binary.Empty`。

### editService（批量注册 / 增删）

批量增加、移除服务时用 `editService(BEditService)`，它内部维护 add / remove 列表，支持幂等，断线重连可重复：

```java
BEditService edit = new BEditService();
edit.getAdd().add(new BServiceInfo("GameServer", "1", 100, "192.168.1.10", 5555, Binary.Empty));
agent.editService(edit);
```

### subscribeService（订阅服务）

订阅指定服务变更，可指定 version（`0` = 所有），返回当前快照。参数是 **`BSubscribeInfo`**，不是 `Subscribe` 协议对象：

```java
// AbstractAgent.subscribeService(BSubscribeInfo info) → SubscribeState
BSubscribeInfo info = new BSubscribeInfo("GameServer", 0);
SubscribeState state = agent.subscribeService(info);

// 批量订阅另有 subscribeServices(BSubscribeArgument) / subscribeServicesAsync(...)
```

### OfflineRegister / OfflineNotify（离线通知）

| 接口 | 说明 |
|------|------|
| `OfflineRegister` | 服务异常断连后延迟（默认 **600 秒**，即 10 分钟，源码 `eOfflineNotifyDelay = 600 * 1000`）通知注册同一 `notifyId` 的服务 |
| `OfflineNotify` | 离线触发通知 |
| `AnnounceServers` | 声明监视哪些服务器，被监视者离线触发通知 |

> 原始官方文档写「600 秒」是正确的；此处之前误写为「60000 秒」，已修正为源码默认值 **600 秒（10 分钟）**。

## 订阅模式

### Simple 模式

订阅者注册后，服务注册/注销变更会发 `EditService` 通知，包含新增和移除列表。

### 版本过滤

订阅时指定 version，只收该版本的服务变更；version 为 `0` 表示收所有版本。

## 运行模式

### 单机：ServiceManagerServer

独立进程，TCP 默认 5001，内存保存，AutoKey 用本地 RocksDB。

```bash
java -cp Zeze.Services.ServiceManagerServer -port 5001
```

| 启动参数 | 默认值 | 说明 |
|----------|--------|------|
| `-ip` | - | 监听 IP |
| `-port` | 5001 | 监听端口 |
| `-autokeys` | - | AutoKey 配置 |

### Raft：ServiceManagerWithRaft

状态持久化（Raft + RocksDB），高可用，主故障时从节点接管。

```bash
java -cp Zeze.Services.ServiceManagerServer \
    -raft 127.0.0.1:6556 \
    -raftConf servicemanager.raft.xml
```

或 `-raft RunAllNodes` 单进程运行所有节点。客户端使用 `ServiceManagerAgentWithRaft`，内部 Raft Agent 连接，自动处理 Leader 切换，请求自动重发。

## XML 配置

```xml
<Zeze.Services.ServiceManager
    KeepAlivePeriod="30000"
    RetryNotifyDelayWhenNotAllReady="..."
    DbHome="..."
    ThreadingReleaseTimeout="..."/>
```

| 参数 | 说明 |
|------|------|
| `KeepAlivePeriod` | 心跳周期（毫秒）。**默认 `-1`（禁用）**；启用时建议设为如 `30000`（30 秒）。仅当 `> 0` 时才定时发 `KeepAlive` |
| `RetryNotifyDelayWhenNotAllReady` | 未全部就绪时重试通知延迟 |
| `DbHome` | 数据库目录 |
| `ThreadingReleaseTimeout` | 线程释放超时 |

## KeepAlive 机制

`KeepAlivePeriod > 0` 时，定期发送 `KeepAlive`。失败则关闭连接，触发会话清理，注销该连接上所有注册与订阅。

## 负载广播

服务注册订阅后自动建立负载观察，`SetServerLoad` 上报后转发给观察者。

## Arch 如何使用 ServiceManager

| 维度 | 说明 |
|------|------|
| 模块即服务 | Server 内每个模块是一个服务，Linkd 以模块为单位派发 |
| 模块服务名编码 | **直接拼接** `serviceNamePrefix + moduleId`（**没有分隔符**），如 `gs1` + 模块号 → `gs13` |
| `ServerServiceNamePrefix` | 应用标识，构造 `ProviderApp` 时传入；多个 ProviderApp 必须能区分 |
| Server 服务 ServiceIdentity | 编码 `String.valueOf(ServerId)` |
| Linkd 服务 ServiceIdentity | 编码 `"@" + ProviderIp + "_" + ProviderPort`（`@` 开头，IP 与端口用下划线 `_` 连接，`@` 前缀是 Zeze.Arch 保留） |

## 组件关系

| 关系 | 说明 |
|------|------|
| 与 GCM | 为 GCM 提供服务发现 |
| 与 Raft | Raft 实现高可用 |

## 相关文档

- Provider-Linkd 架构：[./arch-provider-linkd.md](./arch-provider-linkd.md)
- 缓存同步：[./arch-global-cache-manager.md](./arch-global-cache-manager.md)
- Raft 服务：[./svc-raft.md](./svc-raft.md)
