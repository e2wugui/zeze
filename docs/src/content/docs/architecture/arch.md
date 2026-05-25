---
title: "Provider-Linkd 架构"
sidebar:
  order: 1
---

Provider-Linkd 是 Zeze 的分布式服务骨架：**Linkd** 在前端管理客户端连接与负载分发，**Provider**（又称 Server 或 GameServer）在后端承载业务逻辑。二者通过 **ServiceManager** 互相发现，协作完成从接入到计算的完整链路。

## 角色与交互总览

```
[Client] --TCP--> [Linkd] --Provider协议--> [Provider]
                    |  ^                       |  ^
                    v  |                       v  |
             [ServiceManager]           [ProviderDirect]
              (服务注册/发现)            (Server间直连)
```

| 角色 | 职责 |
|------|------|
| **Linkd** | 接受客户端 TCP 连接；按 Module 配置将请求**分发**到合适的 Provider；管理绑定关系；广播、踢人等 |
| **Provider** | 实现业务逻辑；通过事务操作数据库；向 Linkd 报告负载；处理 Dispatch 转发的客户端请求 |
| **ServiceManager** | 服务注册中心；通告服务上下线变更；承载负载上报（→ service-manager） |
| **ProviderDirect** | Provider 之间的直连通道，承载 Redirect 调用（→ redirect） |

核心网络服务共四个：

| 服务 | 方向 | 基类 |
|------|------|------|
| `LinkdService` | Linkd <-> Client | `HandshakeServer` |
| `LinkdProviderService` | Linkd <-> Provider | `HandshakeServer` |
| `ProviderService` | Provider -> Linkd | `HandshakeClient` |
| `ProviderDirectService` | Provider <-> Provider | `HandshakeServer`/`HandshakeClient` |

## Provider 与 Module

### Module 是什么

**Module**（模块）是 Zeze 中最小的逻辑划分单位，类似于微服务。每个 Module 包含 Bean、协议、数据表，拥有一个**全局唯一编号** `moduleId`。同一 Module 的所有协议在 Linkd 侧会被路由到同一组 Provider 实例。

一个 Provider 进程可以实现多个 Module，同一 Module 也可以部署到多个 Provider 进程（水平扩展），但一个 Module 只能在一个代码工程中实现。

### 静态绑定与动态绑定

Linkd 为客户端选择 Provider 时，会同时建立**绑定亲缘性**：静态模块全部一起绑定到同一 Provider，后续同一客户端的所有静态模块请求都路由到同一实例，避免跨进程访问。

| 类型 | 说明 |
|------|------|
| **静态绑定** | Provider 启动时通过 `Bind` 协议注册；Linkd 收到该模块的首个请求时自动选择 Provider 并把该 Provider 的**所有**静态模块绑定到客户端会话 |
| **动态绑定** | Provider 通过 `Bind` 协议指定 `linkSids`，绑定到特定客户端；需要主动 `UnBind` 解绑；每次请求前 Linkd 不缓存绑定关系 |

### 绑定配置（provider.module.binds.xml）

```xml
<ProviderModuleBinds>
  <!-- 默认模块配置：未显式配置的模块使用此规则 -->
  <defaultModule ChoiceType="ChoiceTypeHashAccount"/>

  <!-- 静态模块：仅 providers 列表中的 ServerId 注册 -->
  <module name="Demo.ModuleA" ChoiceType="ChoiceTypeHashAccount" providers="0,1"/>

  <!-- 动态模块：dynamic="true" 表示动态绑定 -->
  <module name="Demo.ModuleB" ChoiceType="ChoiceTypeRequest" dynamic="true"/>

  <!-- 指定 Provider 不注册默认模块 -->
  <ProviderNoDefaultModule providers="2,3"/>
</ProviderModuleBinds>
```

`ChoiceType` 决定 Linkd 如何选择 Provider：

| ChoiceType | 路由依据 |
|------------|---------|
| `ChoiceTypeHashAccount` | hash(account) |
| `ChoiceTypeHashRoleId` | hash(roleId) |
| `ChoiceTypeHashSourceAddress` | hash(客户端 IP) |
| `ChoiceTypeFeedFullOneByOne` | 轮询喂饱一台再下一台 |
| `ChoiceTypeLoad` | 按负载权重随机选择 |
| `ChoiceTypeRequest` | 按请求计数反权重选择（请求少的优先） |

配置通过 `ProviderModuleBinds.load()` 加载，在 `ProviderApp.startLast()` 中构建：

```java
providerApp.startLast(ProviderModuleBinds.load(), modules);
```

## Linkd 内部组成

| 类 | 职责 |
|----|------|
| `LinkdApp` | 组装 Linkd 内部各组件，初始化服务发现回调 |
| `LinkdProvider` | 处理来自 Provider 的协议（Bind/UnBind/Send/Broadcast 等），执行负载选择 |
| `LinkdProviderService` | 面向 Provider 的网络服务（`HandshakeServer`） |
| `LinkdService` | 面向客户端的网络服务（`HandshakeServer`），`dispatchUnknownProtocol` 是转发的总入口 |

应用可以重载 `dispatchUnknownProtocol` 方法实现自己特殊的转发规则。比如一个群组聊天的派发，根据 `hash(GroupId)` 固定转发到相应的服务器上。为了避免 Decode 完整协议，有个办法就是所有的群组协议开头都有固定的公共参数（如 GroupId），Linkd 这里可以偷偷的只偷出这一部分来进行处理。

Linkd 本身一般不使用数据库，专注于连接管理和协议转发。

## 服务发现流程

### 注册与订阅

**Linkd 启动**时调用 `registerService()`，以 `linkdServiceName`（如 `"Zege.Linkd"`）为名注册到 ServiceManager：

```java
linkdApp.registerService(null); // extra 可传入 BLinkInfo 附加信息
```

**Provider 启动**时在 `registerModulesAndSubscribeLinkd()` 中完成两件事：

1. **注册**：为每个 Module 注册一条服务记录，名称格式为 `serviceNamePrefix + moduleId`（如 `"Zege.Server.Module#1"`）
2. **订阅**：订阅 Linkd 服务发现其地址并主动连接；同时订阅其他 Module 服务用于 ProviderDirect

```java
// ProviderImplement.registerModulesAndSubscribeLinkd()
// 注册：每个模块一条 BServiceInfo
// 订阅：linkdServiceName + 所有模块服务名
```

### Provider 连接 Linkd

1. ServiceManager 广播 Linkd 上线事件
2. Provider 收到后通过 `ProviderService.applyPut()` 建立 TCP 连接
3. 握手完成后 Provider 发送 `AnnounceProviderInfo`（含 servicePrefix、serverId、directIp/Port、appVersion）
4. Provider 发送 `Bind`（静态模块）和 `Subscribe`（动态模块）
5. Linkd 收到 Bind 后订阅该模块的服务信息，后续即可为客户端选择该 Provider

### Provider 之间直连

Provider 启动时也会订阅其他 Module 的服务。当 ServiceManager 通告新 Provider 上线时，`ProviderDirectService` 自动建立直连通道，用于 Redirect 调用（→ redirect）。

## Provider 协议族

Provider 与 Linkd 之间通过一组内部协议协作：

### Linkd -> Provider（下行）

| 协议 | 说明 |
|------|------|
| **Dispatch** | 转发客户端请求。携带 `linkSid`、`account`、`protocolType`、`protocolData`、`userState` 等。Provider 在 `ProcessDispatch` 中解码并执行目标协议处理器 |
| **LinkBroken** | 通知 Provider 某个客户端连接已断开 |

### Provider -> Linkd（上行）

| 协议 | 说明 |
|------|------|
| **AnnounceProviderInfo** | Provider 握手后立即发送，报告 `serviceNamePrefix`、`serviceIdentity`、`providerDirectIp/Port`、`appVersion`、`disableChoice` |
| **Bind** | 静态绑定（不指定 `linkSids`）或动态绑定（指定 `linkSids`）模块到 Linkd |
| **UnBind** | 解除静态或动态绑定 |
| **Subscribe** | 通知 Linkd 订阅动态模块的服务信息 |
| **Send** | 向指定客户端（`linkSids` 列表）转发协议，携带完整协议数据 |
| **Broadcast** | 向 Linkd 内所有已认证客户端广播，支持 `onlySameVersion` 过滤版本 |
| **SetUserState** | 设置客户端会话的 UserState，后续 Dispatch 会原样带回，常用于实现 LoginSession |
| **Kick** | 踢掉指定客户端，可配置是否关闭连接（`eControlClose`） |
| **SetDisableChoice** | 控制该 Provider 是否接受新用户分配 |

## Online 与 Transmit

### Transmit

在分布式架构中，用户被分到了多台 Server 实例中。当用户 A 需要查询用户 B 的数据，发送给用户 X（实际上通常 X 就是 A）。虽然每个 Server 实例都能直接看到所有用户的数据，但是如果这个数据查询量大，并且改动非常频繁，那么此时从异机查询会导致 Cache 失效，命中率下降。Transmit 会查找目标用户在哪台 Server 实例中，然后把请求转到他所在的 server 中执行。执行的结果直接发回给 Sender，不会返回 Sender 所在服务器。这样，修改和查询都发生在 Target 用户所在的服务器，Cache 命中率极高。而且整个操作代价不大，仅仅多一个 Rpc 转发。

```java
Transmit(String account, String clientId, String actionName, String target,
         Serializable parameter = null)
// Sender: Account,ClientId 请求发起者，数据结果发送给他。
// actionName：具体的查询操作，需要注册。
// Target：查询目标用户。
// Parameter：查询参数，可选。
```

相关方法：
- `Transmit` - 直接转发
- `TransmitWhileCommit` - 事务提交时转发
- `TransmitWhileRollback` - 事务回滚时转发

## 负载均衡与过载保护

### 负载选择算法

`ProviderDistribute` 实现了多种选择算法，根据 Module 的 `ChoiceType` 使用不同策略。核心流程在 `LinkdProvider.choiceProviderAndBind()` 中：

1. 根据 `clientAppVersion` 选择对应的版本分发器
2. 查找模块对应的 Provider 列表
3. 按 `ChoiceType` 选择目标 Provider
4. 检查选中 Provider 的版本、过载状态、disableChoice 标记
5. 若不满足条件，遍历剩余 Provider 寻找合适的
6. 绑定静态模块到客户端会话

### 负载上报

`LoadBase` 定时采集并上报负载信息（`BLoad`），包含：

- `online`：当前在线数
- `onlineNew`：每秒新增登录数
- `proposeMaxOnline`：建议最大在线
- `maxOnlineNew`：每秒最大新增
- `overload`：过载状态（`eWorkFine` / `eThreshold` / `eOverload`）

上报频率自适应：过载或新增过快时加速上报（快至 `digestionDelayExSeconds`），空闲时降速（最慢 `reportDelaySeconds`）。

```java
LoadConfig loadConfig = new LoadConfig();
loadConfig.setProposeMaxOnline(30000);  // 建议最大在线
loadConfig.setMaxOnlineNew(100);        // 每秒最大新增
loadConfig.setReportDelaySeconds(2);    // 正常上报间隔
loadConfig.setDigestionDelayExSeconds(1); // 过载消化间隔
loadConfig.setApproximatelyLinkdCount(100); // Linkd数量估算
```

### Provider 过载检测

`ProviderOverload` 通过向线程池提交探测任务测量排队延迟：

- 延迟 < `Config.providerThreshold`（默认 3000ms）：正常
- 延迟 < `Config.providerOverload`（默认 5000ms）：阈值告警（`eThreshold`），开始丢弃可丢弃协议（`eSheddable`）
- 延迟 >= `Config.providerOverload`：过载（`eOverload`），仅保留 `eCriticalPlus` 协议，Linkd 停止派发新用户

Provider 过载时，`ProcessDispatch` 会为 RPC 请求自动回复 `Procedure.Busy`，丢弃非关键协议。

### Linkd 带宽保护

`LinkdService.discard()` 实现输出带宽保护：

- 输出速率 < 70% 带宽上限：不丢弃
- 70% ~ 100%：调用应用自定义 `DiscardAction` 决定是否丢弃
- 超过熔断率（100%）：丢弃所有非关键协议

```java
linkdApp.discardAction = (sender, moduleId, protocolId, size, rate) -> {
    // 自定义丢弃策略，返回 true 表示丢弃
    return false;
};
```

## 影响选择的开关

### setDisableChoiceFromLinks

控制 Linkd 是否将新用户分配到此 Provider。常用于优雅停服：

```java
// 禁止新用户分配
providerService.setDisableChoiceFromLinks(true);
// 等待现有用户处理完毕后安全关闭
```

`ProviderApp.startLast()` 内部会在 Online 就绪后自动调用 `setUserDisableChoice(false)` 开启分配。

### maxAppVersion（版本过滤）

应用通过 `Config.setAppVersion()` 设置版本号，Provider 报告给 Linkd。Linkd 只会将新登录派发到**主版本号一致**的 Provider：

```java
// ProviderDistribute.checkAppVersion()
(serverAppVersion >>> 48) == (clientAppVersion >>> 48) // 主版本号必须一致
```

适用场景：逐台滚动更新时，新登录不会分配到旧版本服务器。

### 启动时控制服务可见

Provider 在 `startLast()` 之前不会被其他服务发现。如果需要更精细的控制：

```java
public void Start(String conf) throws Throwable {
    // ...
    providerService.initDisableChoice(true); // startService 之前设置
    StartService();
    providerApp.startLast(ProviderModuleBinds.load(), Modules);
    // ... startLast 之后的初始化
    providerService.setDisableChoiceFromLinks(false); // 完全准备好后开门
}
```

### Redirect 服务准备好的说明

Redirect 是 Server 之间直连的服务，虽然服务发现也遵循模块注册（`startLast`），但是 Server 之间不容易实现明确的禁止选择策略，所以 Redirect 也会碰到服务没有准备好的问题。对这个问题，暂时不提供解决方案，仅提供几条说明：

1. **Redirect 推荐按不可靠服务来使用**。就是服务不存在或者失败时，调用者应该可以忽略错误。按这个原则，启动过程中出错或者拒绝也是合理的了。
2. **对于按 ServerId 注册到自己的注册中心，然后 `RedirectToServer` 方式使用的服务**，定义为应用自行处理。比如地图服务器分配地图实例的 Redirect，服务启动完全准备好才注册到自己的分配中心，中途避免新的分配进来。这样可以绕过 Zeze 不提供 Redirect 手动控制客户端请求的问题。
3. **`startLast` 才会开启模块注册**，所以此后服务器（模块）才会被其他服务器知道。按这个规则，推荐 Redirect 的实现在 `startLast` 之前准备好。对于无法这样处理的 Redirect 服务，也就是说 `startLast` 之后的初始化对于 Redirect 来说，需要自己在收到请求后处理并拒绝。

## ProtocolRef（跨服务协议引用）

Linkd 通常没有数据库，其 `LinkdService` 中的少量协议（如认证 Auth）需要交给 GameServer 处理。通过 `<protocolref>` 将协议处理句柄定义到 GameServer 的模块中：

```xml
<!-- GameServer 的模块内 -->
<protocolref ref="Linkd.Auth"/>
```

Linkd 也可以引用 GameServer 的协议来实现拦截转发：

```xml
<!-- Linkd 的模块内 -->
<protocolref import="GameServer.SomeNeedIpReq"/>
```

这样 Linkd 可以在 `DispatchUnknownProtocol` 中拦截协议、填充只有 Linkd 知道的信息（如客户端 IP），再调用 `super.dispatchUnknownProtocol` 继续派发。

## Linkd-GameServer 内部信息服务

Linkd 和 GameServer 之间可以定义**共享模块**，将其引入 `ProviderService`，即可在二者之间互相提供服务调用。例如 Linkd 提供黑名单查询功能，GameServer 提供玩家状态查询等。这种方式复用了 Provider 协议通道，无需额外网络服务。

## 全系统启动与停止

### 启动顺序

1. 启动 **ServiceManager**
2. 启动 **GlobalCacheManager**（如需缓存同步）
3. 以**任意顺序**启动 Linkd 和 Provider

### 停止顺序

1. **ServiceManager** 必须保持运行到最后
2. Linkd 关闭客户端接入端口（`Acceptor.close()`），阻止新用户进入
3. Provider 广播通知用户下线
4. Provider 等待一定时间后关闭
5. Linkd 关闭
6. GlobalCacheManager 关闭

## XML 配置示例

### linkd.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<zeze GlobalCacheManagerPort="5002" CheckpointPeriod="0" ServerId="-1">
  <DatabaseConf Name="" DatabaseType="Memory" DatabaseUrl=""/>

  <!-- 面向客户端 -->
  <ServiceConf Name="LinkdService"
               InputBufferMaxProtocolSize="2097152"
               SocketLogLevel="Trace">
    <Acceptor Port="5100"/>
  </ServiceConf>

  <!-- 面向 Provider -->
  <ServiceConf Name="ProviderService"
               InputBufferMaxProtocolSize="2097152"
               SocketLogLevel="Trace">
    <Acceptor Ip="" Port="5101"/>
  </ServiceConf>

  <!-- ServiceManager 连接 -->
  <ServiceConf Name="Zeze.Services.ServiceManager.Agent">
    <Connector HostNameOrAddress="127.0.0.1" Port="5001"/>
  </ServiceConf>
</zeze>
```

### server.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<zeze
    GlobalCacheManagerHostNameOrAddress="127.0.0.1"
    GlobalCacheManagerPort="5002"
    CheckpointPeriod="60000"
    ServerId="0">
  <DatabaseConf Name="" DatabaseType="Memory" DatabaseUrl=""/>

  <!-- Provider -> Linkd 的连接（无 Acceptor，作为 Client） -->
  <ServiceConf Name="Server"
               InputBufferMaxProtocolSize="2097152"
               SocketLogLevel="Trace">
  </ServiceConf>

  <!-- ServiceManager 连接 -->
  <ServiceConf Name="Zeze.Services.ServiceManager.Agent">
    <Connector HostNameOrAddress="127.0.0.1" Port="5001"/>
  </ServiceConf>

  <!-- Provider 之间直连 -->
  <ServiceConf Name="ServerDirect"
               InputBufferMaxProtocolSize="2097152"
               SocketLogLevel="Trace">
    <Acceptor Ip="" Port="5102"/>
  </ServiceConf>
</zeze>
```

## Java 初始化代码示例

### LinkdApp 初始化

```java
public void Start(String conf) throws Throwable {
    var config = Config.Load(conf);
    CreateZeze(config);
    CreateService();

    // Arch 模块初始化
    LinkdProvider = new Zeze.Arch.LinkdProvider();
    LinkdApp = new Zeze.Arch.LinkdApp(
        "Zege.Linkd",           // linkdServiceName
        Zeze,                    // Application
        LinkdProvider,           // LinkdProvider
        ProviderService,         // LinkdProviderService
        LinkdService,            // LinkdService
        LoadConfig()             // LoadConfig
    );

    CreateModules();
    Zeze.Start();
    StartModules();

    // SessionId 生成器
    AsyncSocket.setSessionIdGenFunc(
        PersistentAtomicLong.getOrAdd(LinkdApp.getName())::next);

    StartService();
    // 向 ServiceManager 注册，开始接受 Provider 连接
    LinkdApp.registerService(null);
}
```

### ProviderApp 初始化

```java
public void Start(String conf) throws Throwable {
    var config = Config.Load(conf);
    CreateZeze(config);
    CreateService();

    // Arch 模块初始化
    Provider = new Zeze.Arch.ProviderWithOnline();
    ProviderDirect = new Zeze.Arch.ProviderDirect();
    ProviderApp = new Zeze.Arch.ProviderApp(
        Zeze,                          // Application
        Provider,                      // ProviderImplement
        Server,                        // ProviderService
        "Zege.Server.Module#",         // serviceNamePrefix
        ProviderDirect,                // ProviderDirect
        ServerDirect,                  // ProviderDirectService
        "Zege.Linkd",                  // linkdServiceName
        LoadConfig()                   // LoadConfig
    );

    // 可选模块初始化
    Provider.Online = GenModule.Instance.ReplaceModuleInstance(
        this, new Online(this));

    CreateModules();
    Zeze.Start();
    StartModules();

    // Online 初始化（→ online）
    Provider.Online.Start();

    // SessionId 生成器
    PersistentAtomicLong socketSessionIdGen = PersistentAtomicLong.getOrAdd(
        "Zege.Server." + Zeze.getConfig().getServerId());
    AsyncSocket.setSessionIdGenFunc(socketSessionIdGen::next);

    StartService();
    // 最后一步：注册模块、订阅 Linkd、开始接受请求
    ProviderApp.startLast(ProviderModuleBinds.load(), Modules);
}
```
