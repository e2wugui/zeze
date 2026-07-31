---
title: "Provider-Linkd 架构"
description: "Zeze 分布式服务器架构的核心组件及其交互关系"
category: reference
order: 10
---

> 本文档描述 Zeze 分布式架构中 Linkd 前端、Provider 后端、ServiceManager 服务注册中心及 ProviderDirect 直连的角色职责、交互流程与配置方式，供架构理解与部署参考。

## 架构总览

Zeze 采用「前端连接管理 + 后端业务承载」的分布式拆分架构。核心组件职责如下：

| 组件 | 角色 | 主要职责 |
|------|------|----------|
| **Linkd** | 前端 | 管理客户端连接，按 Module 配置负载分发到合适 Provider，管理绑定、广播、踢人；**不使用数据库**，专注连接管理 |
| **Provider** | 后端 | 承载业务逻辑与事务，操作数据库，向 Linkd 报告负载，处理转发请求 |
| **ServiceManager** | 服务注册中心 | 通告服务上下线，承载负载上报 |
| **ProviderDirect** | Provider 间直连 | 承载 Redirect（跨服调用）|

### 交互总览

```
                        ┌───────────────────┐
                        │  ServiceManager   │  服务注册中心（通告上下线 / 负载上报）
                        └─────────┬─────────┘
              注册/订阅/负载上报    │
       ┌───────────────────────────┼───────────────────────────┐
       │                           │                           │
┌──────▼──────┐   Provider协议   ┌─▼──────────┐   Provider直连  ┌─────────────┐
│   Client    │───TCP──────────▶ │   Linkd    │◀──────────────▶│  Provider   │
│             │                  │  (前端)    │                │  (后端)     │
└─────────────┘                  └────────────┘                └──────┬──────┘
                                     │ 派发                         │
                                     └──────────────────────────────▶│
                                                                 数据库操作
```

```
[Client] ──TCP──▶ [Linkd] ──Provider协议──▶ [Provider] + ServiceManager + ProviderDirect
```

### 核心网络服务（四个）

| 服务类 | 方向 | 说明 |
|--------|------|------|
| `LinkdService` | Linkd ↔ Client | HandshakeServer，接收客户端连接并握手 |
| `LinkdProviderService` | Linkd ↔ Provider | HandshakeServer，接收 Provider 连接并握手 |
| `ProviderService` | Provider → Linkd | HandshakeClient，连接到 Linkd |
| `ProviderDirectService` | Provider ↔ Provider | Provider 之间直连，承载 Redirect |

## Module（模块）

Module 是最小的逻辑单位，类似微服务。

| 属性 | 说明 |
|------|------|
| 全局唯一 | 每个模块有全局唯一的 `moduleId` |
| 同一协议 | 同一 Module 使用同一套协议 |
| Linkd 路由 | Linkd 把请求路由到实现该 Module 的同一组 Provider |
| 多 Module 实现 | 一个 Provider 可实现多个 Module |
| 水平扩展 | 同一 Module 可部署多个 Provider |
| 单工程实现 | 一个 Module 只能由一个代码工程实现 |

## 绑定模型（静态绑定 vs 动态绑定）

| 维度 | 静态绑定（Static） | 动态绑定（Dynamic） |
|------|--------------------|---------------------|
| 触发时机 | Provider 启动 `Bind` 注册，Linkd 收首个请求时选 Provider | `Bind` 指定 `linkSids`，需 `UnBind` 解绑 |
| 绑定粒度 | 把所有静态模块绑定到客户端会话（亲缘性，避免跨进程） | 每次请求不缓存绑定 |
| 适用场景 | 常规登录后业务 | 临时性、一次性访问 |

## Module 绑定配置

`provider.module.binds.xml` 配置示例：

```xml
<xml>
    <defaultModule ChoiceType="HashAccount"/>
    <ChoiceType name="Load"/>
    <module name="Login" ChoiceType="HashAccount"/>
    <module name="Bag"   ChoiceType="HashRoleId"  providers="0,1"/>
    <module name="Chat"  ChoiceType="Load"        dynamic="true"/>
    <ProviderNoDefaultModule/>
</xml>
```

### ChoiceType 选择算法表

| ChoiceType | 说明 |
|------------|------|
| `HashAccount` | 对 account 做 Hash |
| `HashRoleId` | 对 roleId 做 Hash |
| `HashSourceAddress` | 对来源地址做 Hash |
| `FeedFullOneByOne` | 轮询「喂饱」（按容量逐个填满） |
| `Load` | 按负载权重随机 |
| `Request` | 按请求计数反权重 |
| `Default` | 默认策略 |

> ⚠️ **XML 配置时取值要带 `ChoiceType` 前缀**：写成 `ChoiceTypeHashAccount` / `ChoiceTypeHashRoleId` / `ChoiceTypeLoad` / `ChoiceTypeDefault` 等。上表省略前缀仅为阅读方便，直接写 `HashAccount`、`Load`、`Request` 是无法通过字符串解析的。

绑定加载：`ProviderModuleBinds.load()`。

## Linkd 内部组成

| 组件 | 职责 |
|------|------|
| `LinkdApp` | 组装、初始化、服务发现回调 |
| `LinkdProvider` | 处理 Provider 协议：Bind / UnBind / Send / Broadcast，负载选择 |
| `LinkdProviderService` | Linkd 端与 Provider 的握手服务 |
| `LinkdService` | `dispatchUnknownProtocol` 转发总入口，可重载实现特殊转发（如群组按 `hash(GroupId)` 固定转发，可偷 Decode 部分公共参数） |

## 服务发现流程

```
1. Linkd 启动      → registerService()，以 linkdServiceName 注册
2. Provider 启动   → registerModulesAndSubscribeLinkd()
                     · 每 Module 一条 serviceName = Prefix + moduleId
                     · 订阅 Linkd
3. ServiceManager  → 广播 Linkd 上线
4. Provider 收到   → applyPut 建立 TCP 握手
                     发 AnnounceProviderInfo（携带 ServiceIdentity、directIpPort、appVersion、DisableChoice 等字段）
                     发 Bind（静态）+ Subscribe（动态）
5. Provider 间直连 → 订阅其他 Module 服务
```

## Provider 协议族

### Linkd → Provider

| 协议 | 说明 |
|------|------|
| `Dispatch` | 转发客户端请求，携带 `linkSid / account / protocolType / protocolData / userState`，Provider 通过 `ProcessDispatch` 解码执行 |
| `LinkBroken` | 通知链路断开 |

### Provider → Linkd

| 协议 | 说明 |
|------|------|
| `AnnounceProviderInfo` | 声明 Provider 信息 |
| `Bind` | 静态绑定模块 |
| `UnBind` | 解绑 |
| `Subscribe` | 订阅动态模块 |
| `Send` | 指定 `linkSids` 转发给客户端 |
| `Broadcast` | 广播（支持 `onlySameVersion`） |
| `SetUserState` | 设置用户状态 |
| `Kick` | 踢人（`eControlClose`） |
| `SetDisableChoice` | 设置是否禁止选择 |

## Transmit（跨服数据查询）

当用户分多台 Server、查询量大且改动频繁时，异机查询会导致 Cache 失效。`Transmit` 查找目标用户所在 Server，转去执行，结果直接发 Sender 不返回；Sender 所在服务器修改/查询都在 Target 所在服务器完成，Cache 命中率高，仅多一个 Rpc 转发。

| 方法 | 说明 |
|------|------|
| `Transmit(account, clientId, actionName, target, parameter)` | 基础转发 |
| `TransmitWhileCommit` | 事务提交时转发 |
| `TransmitWhileRollback` | 事务回滚时转发 |

## 负载均衡

### ProviderDistribute 选择流程

`choiceProviderAndBind()` 流程：

```
1. 选择版本分发器
2. 查询模块的 Provider 列表
3. 按 ChoiceType 选择
4. 检查版本、过载、disableChoice
5. 不满足则遍历剩余 Provider
6. 绑定静态模块
```

### 负载上报（LoadBase）

`LoadBase` 采集并上报 `BLoad`：

| 字段 | 说明 |
|------|------|
| `online` | 当前在线数 |
| `onlineNew` | 新增在线 |
| `proposeMaxOnline` | 建议最大在线 |
| `maxOnlineNew` | 最大新增 |
| `overload` | 过载状态：`eWorkFine` / `eThreshold` / `eOverload` |

上报频率自适应：过载加速，空闲降速，由 `LoadConfig` 配置。

### Provider 过载检测（ProviderOverload）

| 指标 | 默认值 | 行为 |
|------|--------|------|
| `< providerThreshold` | 2000 | 正常 |
| `< providerOverload` | 4000 | 阈值告警，丢 `eSheddable` |
| `>= providerOverload` | 4000 | 过载，仅留 `eCriticalPlus`，Linkd 停止派发；`ProcessDispatch` 为 RPC 时自动回 `Procedure.Busy` |

### Linkd 带宽保护（discard）

| 输出占比 | 行为 |
|----------|------|
| `< 70%` | 不丢 |
| `70% ~ 100%` | 自定义 `DiscardAction` |
| `> 100%` | 熔断，丢所有非关键 |

## 影响选择的开关

| 开关 / 机制 | 说明 |
|-------------|------|
| `setDisableChoiceFromLinks` | 优雅停服：`providerService.setDisableChoiceFromLinks(true)`，等处理完安全关闭；与之配合，`ProviderApp` 在 `startLast()` 中调 `setUserDisableChoice(false)` 开启选择（详见下方 Java 示例） |
| `maxAppVersion` | 版本过滤：主版本号一致才派发，`(serverAppVersion >>> 48) == (clientAppVersion >>> 48)`，用于滚动更新 |
| 启动时控制服务可见 | `initDisableChoice = true`，在 `startService` 前设置 |

## Redirect 服务就绪说明

- 推荐按「不可靠服务」使用。
- `startLast` 才开启模块注册，推荐 Redirect 实现在 `startLast` 前准备好。

## 跨服务协议引用

```xml
<protocolref ref="Linkd.Auth"/>
```

## Linkd-GameServer 内部信息共享

通过引入共享模块（`ProviderService`）实现互相调用。

## 全系统启动 / 停止顺序

**启动顺序：**
```
ServiceManager → GlobalCacheManager → 任意顺序 Linkd 和 Provider
```

**停止顺序：**
```
ServiceManager 保持最后
  → Linkd 关闭 Acceptor
    → Provider 广播用户下线
      → Provider 等待关闭
        → Linkd 关闭
          → GCM 关闭
```

## XML 配置示例

`linkd.xml`：

```xml
<ServiceConf>
    <Acceptor Ip="@internal" Port="5555"/>
    <Acceptor Ip="@external" Port="5556"/>
</ServiceConf>
```

`server.xml`：

```xml
<ServiceConf>
    <Connector Ip="127.0.0.1" Port="5555" AutoReconnect="true"/>
</ServiceConf>
```

## Java 初始化示例

### ProviderApp

> ⚠️ **重要**：`ProviderApp` **不是** `extends Application`，而是 `extends ReentrantLock`（它本身不是 Zeze Application，持有的是 `Application zeze` 字段）。构造方法有 **8 个参数**，没有 `(int serverId, String servicePrefix)` 这样的简化构造。模块注册与服务订阅发生在 `startLast()` 内部调用的 `ProviderImplement.registerModulesAndSubscribeLinkd()`，最终通过 `ServiceManager.editService(BEditService)` 批量注册，而**不是** `providerService.start()`。

```java
// ProviderApp 真实构造签名（8 参数）
public class ProviderApp extends ReentrantLock {
    public ProviderApp(
            @NotNull Application zeze,
            @NotNull ProviderImplement server,
            @NotNull ProviderService toLinkdService,
            @NotNull String providerModulePrefixNameOnServiceManager,
            @NotNull ProviderDirect direct,
            @NotNull ProviderDirectService toOtherProviderService,
            @NotNull String linkdNameOnServiceManager,
            @NotNull LoadConfig loadConfig) {
        // ...
    }

    @Override
    public void startLast() throws Exception {
        // 模块注册与订阅 Linkd 在这里完成：
        //   providerImplement.registerModulesAndSubscribeLinkd()
        //     内部用 sm.editService(BEditService) 批量注册服务
        // 同时 setUserDisableChoice(false) 开启选择
    }
}
```

### LinkdApp

`LinkdApp` 负责前端连接管理，服务注册通过 `registerService(@Nullable BLinkInfo.Data extra)` 完成（内部用 `editService` + `BServiceInfo`）：

```java
public class LinkdApp extends Application {
    // Linkd 的服务发现注册走 registerService(BLinkInfo.Data)
    // LinkdApp 和 LinkdProvider 上都没有 setUserDisableChoice 方法
}
```

### `setUserDisableChoice` 归属

`setUserDisableChoice(boolean)` 是 **`ProviderApp`** 的方法（包级可见），**不在 `LinkdApp`/`LinkdProvider` 上**。它在 `startLast()` 中被调用（`setUserDisableChoice(false)` 开启选择），也会被 `ProviderService` 内部使用。不要在 Linkd 端调用它。

| 开关 | 归属 | 说明 |
|------|------|------|
| `setUserDisableChoice(false)` | `ProviderApp` | `startLast()` 内调用，开启选择 |
| `setDisableChoiceFromLinks(true)` | `ProviderService` | 优雅停服：拒绝 Linkd 派发新用户，等存量处理完再关 |
| `initDisableChoice = true` | `ProviderApp` | 启动时控制服务可见性，在 `startService` 前设置 |

## 相关文档

- 分布式入门：[../manual/05-going-distributed.md](../manual/05-going-distributed.md)
- 在线管理：[./arch-online.md](./arch-online.md)
- Redirect 跨服调用：[./arch-redirect.md](./arch-redirect.md)
- 网络层：[./arch-net.md](./arch-net.md)
- 服务发现：[./arch-service-manager.md](./arch-service-manager.md)
- 缓存同步：[./arch-global-cache-manager.md](./arch-global-cache-manager.md)
- 配置参考：[./configuration.md](./configuration.md)
