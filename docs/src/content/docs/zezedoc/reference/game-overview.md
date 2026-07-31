---
title: "游戏模块总览"
description: "Zeze.Game 游戏服务器开箱即用基础设施——在线管理、背包、排行榜等通用系统"
category: reference
order: 50
---

> 本文档总览 Zeze Game 模块：`ProviderWithOnline` 启动入口、`Online`/`Bag`/`Rank` 通用子系统、`ProviderLoadWithOnline` 负载报告与 `LinkBroken` 处理机制，供游戏服务器开发检索参考。

## 模块定位

Zeze Game 模块为游戏服务器开发提供**开箱即用**的基础设施，基于 **Provider-Linkd 架构**封装了在线管理、背包、排行榜等通用系统，**所有操作天然事务安全**。

| 模块 | 包路径 | 职责 |
|------|--------|------|
| `ProviderWithOnline` | `Zeze.Game` | 游戏服务器核心启动入口，整合 Online 在线管理与负载报告 |
| `Online` | `Zeze.Game.Online` | 登录/登出生命周期、可靠消息、跨服转发、事件、本机数据 |
| `Bag` | `Zeze.Game.Bag` | 物品增删移、自动堆叠/拆分、移动交换、排序 |
| `Rank` | `Zeze.Game.Rank` | 高性能分布式排行榜，并发分区 + 多路归并聚合查询 |
| `ProviderLoadWithOnline` | `Zeze.Game` | 以 Online 本地在线人数/登录次数作负载报告 |

---

## ProviderWithOnline：核心启动入口

`ProviderWithOnline` 继承 `ProviderImplement`，整合 **Online 在线管理**和 **ProviderLoadWithOnline 负载报告**。它是游戏 Provider（业务后端）的核心启动入口。

### 获取实例

```java
ProviderWithOnline provider = (ProviderWithOnline)zeze.redirect.providerApp.providerImplement;
```

### 创建默认 Online 及命名 OnlineSet

```java
// 在 App.Start 中调用：创建默认 Online 及命名 OnlineSet
provider.create(this, "chat", "friend");
provider.start();
```

`create(app, "chat", "friend")` 完成以下工作：

| 步骤 | 说明 |
|------|------|
| 1. 创建默认 Online | key 为空串 `""`，调用 `Initialize` 注册协议数据表 |
| 2. 按 names 创建额外 OnlineSet | 每个 OnlineSet 拥有**独立的在线数据和本地存储** |
| 3. 创建负载报告实例 | `ProviderLoadWithOnline`，注册线程池过载检测 |

> 默认 Online 与命名 OnlineSet 互相独立。`load` 报告仅定义在默认 Online 实例中，OnlineSet 不单独报告 load。

### start 完成事项

`provider.start()` 启动：

| 事项 | 说明 |
|------|------|
| 负载报告 | `load.start()` 启动定时负载上报 |
| Online 本地检查 | `online.start()` 启动 `verifyLocal` 定时器，清理过期本机数据 |

### 其他方法

| 方法 | 说明 |
|------|------|
| `getOnline()` | 默认 Online 实例 |
| `getOnline(name)` | 按名字取 OnlineSet（`null`/空串返回默认） |
| `foreachOnline(consumer)` | 遍历所有 OnlineSet |
| `getLoad()` | 负载报告实例 |
| `stop()` | 停止所有 Online 与 load |

---

## Online 在线管理

详见 [Online 在线管理](./arch-online.md)，此处仅列概览。

| 能力 | 说明 |
|------|------|
| **生命周期** | `Login` / `ReLogin` / `Logout` 协议，版本号（`LoginVersion`）机制判断数据新旧 |
| **可靠消息** | `sendReliableNotify` 持久化到队列，断线重连不丢 |
| **跨服转发** | `transmit` 将查询请求转发到目标角色所在 Server |
| **事件** | `loginEvents` / `reloginEvents` / `logoutEvents` / `linkBrokenEvents` / `localRemoveEvents` |
| **本机数据** | `setLocalBean` / `getLocalBean` / `getOrAddLocalBean` / `removeLocalBean` |

```java
// 注册登录事件
online.getLoginEvents().run((sender, arg) -> {
    // arg.roleId, arg.account
    return 0; // 0 成功
});
```

---

## Bag 背包系统

详见 [Bag 背包系统](./game-bag.md)。

| 能力 | 说明 |
|------|------|
| 物品添加 | 自动堆叠与拆分，溢出返回剩余数量 |
| 物品移除 | 跨格子累加，不足返回 `false` |
| 移动/交换/拆分 | `move` 一接口完成移动、交换、叠加、拆分 |
| 排序 | 自定义 `Comparator` |
| 属性扩展 | `BeanFactory` + `DynamicBean` 支持任意扩展 |

---

## Rank 排行榜

详见 [Rank 排行榜](./game-rank.md)。

| 能力 | 说明 |
|------|------|
| 高性能分布式 | `ConcurrentLevel` 并发分区，多服务器并行更新 |
| 一致性路由 | `RedirectHash` 按 hash 路由到一台 Server |
| 全局查询 | 多路归并聚合所有分组 |
| 适用场景 | 即时排行榜，角色数值变化马上更新 |

---

## ProviderLoadWithOnline：负载报告

`ProviderLoadWithOnline` 继承 `LoadBase`，把 **Online 本地在线人数和登录次数**作为负载报告上报。

| 方法 | 说明 |
|------|------|
| `getOnlineLocalCount()` | Online 本地在线人数（`online.getLocalCount()`） |
| `getOnlineLoginTimes()` | Online 累计登录次数（`online.getLoginTimes()`） |
| `getOverload()` | 过载检测实例（`ProviderOverload`） |
| `getLoadConfig()` | 负载配置（`ProposeMaxOnline` / `MaxOnlineNew` 等） |

### 过载检测机制

`Overload` 通过 `register(Task.getThreadPool(), config)` 注册线程池，定时（随机 1~2 秒）探测任务队列延迟：

| 延迟区间 | 状态 | 含义 |
|----------|------|------|
| `< ProviderThreshold`（默认 2000ms） | `eWorkFine`（0） | 正常 |
| `< ProviderOverload`（默认 4000ms） | `eThreshold`（1） | 忙碌 |
| `≥ ProviderOverload`（4000ms） | `eOverload`（2） | 熔断 |

> 任一注册线程池达到 `eOverload`，整体负载即为 `eOverload`；任一达到 `eThreshold`，整体为 `eThreshold`。Linkd 据此不再向该 Provider 分配用户。

### 负载上报去向

| 去向 | 接口 | 说明 |
|------|------|------|
| ServiceManager | `setServerLoad` | 注册中心负载上报（默认保留） |
| LoginQueueServer | `loginQueueAgent.reportProviderLoad` | 启用登录队列时报告，用于队列分配 |

---

## LinkBroken 处理

`ProviderWithOnline.ProcessLinkBroken` 处理客户端链路断开通知。

### 处理流程

```java
@Override
protected long ProcessLinkBroken(LinkBroken p) throws Exception {
    // 1. 从 UserState.Context 取出 roleId
    var roleId = Long.parseLong(p.Argument.getUserState().getContext());
    // 2. 从 UserState.OnlineSetName 定位 Online 实例
    var onlineSet = online.getOnline(p.Argument.getUserState().getOnlineSetName());
    // 3. 执行 linkBroken
    onlineSet.linkBroken(account, roleId, linkName, linkSid);
    return Procedure.Success;
}
```

| 步骤 | 说明 |
|------|------|
| 定位 Online | `UserState.Context(roleId)` + `OnlineSetName` 定位 Online 实例 |
| 版本号检查 | `LoginVersion` 不匹配则清理过期本机数据 |
| 触发事件 | `linkBrokenEvents`（`LinkBrokenArgument(roleId)`） |
| 延迟登出 | 按 `Config.OnlineLogoutDelay` 延迟登出；期间 `ReLogin` 可取消 |

### 延迟登出（DelayLogout）

链路断开后状态置为 `eLinkBroken`，启动延迟登出定时器。`tryLogout` 再次校验 `LoginVersion`：若玩家已重新登录（版本号变化），则取消登出。

---

## 架构关系

```
                    ┌─────────────────────────────────┐
                    │       Zeze.Application           │  事务、数据表管理
                    └───────────────┬─────────────────┘
                                    │ 依赖
                    ┌───────────────▼─────────────────┐
  Client ──TCP──▶ Linkd ──Provider协议──▶ │ ProviderApp (ProviderWithOnline) │
                                    │   ├── Online (在线管理)              │
                                    │   ├── Bag (背包)                     │
                                    │   ├── Rank (排行榜)                  │
                                    │   └── ProviderLoadWithOnline (负载)   │
                    └───────────────┬─────────────────┘
                            ProviderDirect │ 服务器间直连（Redirect）
                                    ▼
                              数据库操作（Zeze 事务内）
```

| 关系 | 说明 |
|------|------|
| 运行位置 | Game 模块运行在 **ProviderApp** 之上 |
| 事务依赖 | 依赖 `Zeze.Application` 管理事务、数据表 |
| 请求来源 | Provider 通过 **Linkd** 接收客户端请求 |
| 服务器直连 | `ProviderDirectService` 承载 Redirect 跨服调用 |
| 事务安全 | 所有 Game 数据操作在 **Zeze 事务**内，自动乐观锁 |
| 热更新 | 模块支持热更新（Online 实现 `HotUpgrade` / `HotBeanFactory`） |

---

## 相关文档

- [Online 在线管理](./arch-online.md) — 详细的在线状态、事件、可靠消息、跨服转发接口
- [Bag 背包系统](./game-bag.md) — 物品增删移、堆叠拆分、排序
- [Rank 排行榜](./game-rank.md) — 并发分区与多路归并
- [LoginQueue 登录队列](./game-login-queue.md) — 高并发登录速率控制
- [Provider-Linkd 架构](./arch-provider-linkd.md) — 前后端拆分架构
- [Redirect 跨服调用](./arch-redirect.md) — 跨服调用三种注解
- [分布式入门](../manual/05-going-distributed.md) — 走向分布式的概念讲解
- [热更新](./advanced-hot-reload.md) — 在线模块热升级
