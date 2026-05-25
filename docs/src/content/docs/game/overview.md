---
title: "游戏模块总览"
sidebar:
  order: 0
---

Zeze Game 模块为游戏服务器开发提供了开箱即用的基础设施。它基于 [Provider-Linkd 架构](../../arch/) 构建，封装了在线管理、背包、排行榜等游戏通用系统，所有操作天然具备事务安全性。

## ProviderWithOnline 初始化

**ProviderWithOnline** 是游戏服务器的核心启动入口，继承自 `ProviderImplement`。它将 **Online** 在线管理模块和 **ProviderLoadWithOnline** 负载报告模块整合在一起，形成完整的游戏 Provider 生命周期。

```java
// 在 App.Start 流程中创建并初始化
public class MyApp extends AppBase {
    @Override
    public void Start() throws Exception {
        var provider = (ProviderWithOnline)zeze.redirect.providerApp.providerImplement;
        provider.create(this, "chat", "friend"); // 创建默认 Online 及命名 OnlineSet
        provider.start();
    }
}
```

`create` 方法完成以下工作：

1. 创建默认 **Online** 实例（key 为空字符串），并调用 `Initialize` 注册协议和数据表。
2. 根据传入的 `names` 参数创建额外的 **OnlineSet**（如 `"chat"`、`"friend"`），每个 OnlineSet 拥有独立的在线数据和本地存储。
3. 创建 **ProviderLoadWithOnline** 负载报告实例，注册到线程池进行过载检测。

`start` 方法启动负载报告和 Online 的本地检查定时器。`stop` 方法按相反顺序清理所有资源。

## 模块概览

### Online 在线管理

**Online** 模块（参见源码 `Zeze.Game.Online`）是游戏在线状态的核心管理器，负责：

- **登录/登出生命周期**：通过 `Login`、`ReLogin`、`Logout` 协议管理玩家连接状态，内置版本号机制防止状态混乱。
- **可靠消息推送**：`sendReliableNotify` 系列方法保证消息在断线重连后不丢失。
- **跨服转发（Transmit）**：`transmit` 方法将请求路由到目标玩家所在的服务器执行。
- **事件系统**：`loginEvents`、`logoutEvents`、`linkBrokenEvents` 等事件分发器支持业务模块注册回调。
- **本地数据存储**：`setLocalBean`/`getLocalBean` 在进程内为在线角色保存临时数据，角色下线自动清理。

### Bag 背包系统

**Bag** 模块提供完整的物品管理能力，详见 [背包系统](../bag/)。支持物品添加（自动堆叠和拆分）、移除、移动交换和排序，通过 `BeanFactory` 支持任意扩展属性。

### Rank 排行榜

**Rank** 模块提供高性能的分布式排行榜，详见 [排行榜](../rank/)。通过 **ConcurrentLevel** 并发分区和 **RedirectHash** 实现多服务器并行更新，使用多路归并算法聚合查询结果。

## ProviderLoadWithOnline 负载报告

**ProviderLoadWithOnline** 继承自 `LoadBase`，将 Online 的本地在线人数和登录次数作为负载指标报告给 Linkd。负载报告影响 Linkd 的任务派发策略：

- **在线人数**：`getOnlineLocalCount()` 返回本地在线角色数量。
- **登录次数**：`getOnlineLoginTimes()` 返回累计登录次数。
- **过载检测**：通过 `Overload` 类注册到 `Task.getThreadPool()`，监控任务队列延迟。

当任务延迟超过 `ProviderThreshold`（默认 2000ms）时标记为忙碌，超过 `ProviderOverload`（默认 4000ms）时触发熔断。相关配置参见 [配置参考](../../devops/configuration/)。

## LinkBroken 处理

当客户端连接异常断开时，**ProviderWithOnline.ProcessLinkBroken** 被调用。它通过 LinkBroken 协议中的 `UserState.Context`（即 roleId）和 `OnlineSetName` 定位对应的 Online 实例，执行 `linkBroken` 流程：

1. 检查版本号，清理过期的本地数据。
2. 触发 `linkBrokenEvents` 事件。
3. 根据配置的 `OnlineLogoutDelay` 延迟执行登出。

## 与其他模块的关系

Game 模块运行在 **ProviderApp** 之上，依赖 `Zeze.Application` 管理事务和数据表。Provider 通过 **Linkd** 接收客户端请求，通过 **ProviderDirectService** 进行服务器间直连通信。所有 Game 模块的数据操作都在 Zeze 事务内执行，自动获得乐观锁并发控制。模块支持热更新，详见 [热更新](../../advanced/hot-reload/)。
