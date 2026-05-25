---
title: "Online 在线管理"
sidebar:
  order: 2
---

**Online** 是 Zeze 提供的基于账号的在线状态管理模块（`Zeze.Arch.Online`），负责跟踪用户登录/登出生命周期、维护本机数据、发送在线消息以及跨服查询转发。在游戏场景下使用 `Zeze.Game.Online`，它以 roleId 为键并增加了 `LinkBrokenEvents`、`OnlineSet` 等扩展。两者 API 风格一致，本文以 `Zeze.Arch.Online` 为主进行说明。

## 在线状态

每个用户登录由 `(account, clientId)` 唯一标识。clientId 允许同一账号多端同时在线。Online 维护三张内部表：

- **\_tonline** — 全局在线数据（`BOnlines`），记录每个账号所有登录的 LinkName、LinkSid、状态及 LoginVersion，在所有 Server 间通过 cache-sync 共享。
- **\_tlocal** — 本机数据（`BLocals`），仅当前 Server 可见的内存表，记录本地登录的私有数据。
- **ReliableNotifyQueue** — 可靠消息队列，持久化到数据库，用于断线补发。

状态常量：`eOffline`(0)、`eLinkBroken`(1)、`eLogined`(2)。

```java
Online online = providerApp.online; // 通过 ProviderApp 获取

boolean on = online.isOnline(account);
boolean on2 = online.isOnline(account, clientId);
int state = online.getState(account, clientId); // eOffline / eLinkBroken / eLogined
```

## 本机数据

账号登录在某台 Server 后，可为该登录保存仅本机可见的 **本机数据**（LocalBean）。本机数据存储在 `_tlocal` 内存表中，随登录创建、登出删除。

```java
// 写入：key 为自定义字符串标识
online.setLocalBean(account, clientId, "myState", myBean);

// 读取：泛型推断返回类型
MyBean state = online.getLocalBean(account, clientId, "myState");

// 获取或新增：如果 key 不存在则使用 defaultHint 初始化
MyBean state = online.getOrAddLocalBean(account, clientId, "myState", new MyBean());

// 删除
online.removeLocalBean(account, clientId, "myState");

// 遍历所有本机数据（必须在事务外执行）
online.walkLocal((account, locals) -> {
    // 处理每个账号的本地数据
    return true; // 返回 false 中断遍历
});
```

> `setLocalBean` 和 `getOrAddLocalBean` 必须在事务内调用。`walkLocal` 遍历内存表，不在事务中执行。

## 在线事件

Online 通过 `EventDispatcher` 暴露四种事件，使用 `triggerEmbed` → `triggerProcedure` → `triggerThread` 三阶段触发，支持在事务内或事务外注册处理函数。

| 事件 | 获取方法 | 参数类型 | 触发时机 |
|------|---------|---------|---------|
| **LoginEvents** | `getLoginEvents()` | `LoginArgument(account, clientId)` | 用户首次登录 |
| **ReloginEvents** | `getReloginEvents()` | `LoginArgument(account, clientId)` | 断线重连 |
| **LogoutEvents** | `getLogoutEvents()` | `LogoutEventArgument(account, clientId)` | 登出（含补发） |
| **LocalRemoveEvents** | `getLocalRemoveEvents()` | `LocalRemoveEventArgument(account, clientId, local)` | 本机数据被删除 |

```java
// 注册登录事件
online.getLoginEvents().run((sender, arg) -> {
    logger.info("用户登录: account={}, clientId={}", arg.account, arg.clientId);
    return 0; // 返回 0 表示成功
});
```

### 登出事件丢失与补发

服务器异常关闭时无法正常触发 Logout 事件。Online 的处理策略是：**下一次 Login 发生时，若发现上一个 Login 没有 Logout，先补发一个 Logout 事件，再执行 Login 流程**。Login 处理内部使用 `done` 标志循环重试来实现这一点。

这意味着 Logout 事件可能与对应的 Login 在时间上相隔很远。如果业务需要精确的时间统计（如在线时长），不应完全依赖 Login/Logout 事件对，需自行处理。

## ReliableNotify（可靠消息）

**可靠消息**是一种需要客户端确认的在线通知机制。消息先持久化到队列中再发送，客户端通过 `ReliableNotifyConfirm` 协议确认已收到的消息序号。在 Relogin 时，未确认的消息会同步给客户端，确保不丢失。

使用步骤：

1. 在数据装载时调用 `addReliableNotifyMark` 启用某个 listenerName 的可靠消息通道。
2. 通过 `sendReliableNotify` 系列方法发送消息。
3. 不再需要时调用 `removeReliableNotifyMark` 关闭通道。

```java
// 启用可靠消息通道（通常在 Login 事件中）
online.addReliableNotifyMark(account, clientId, "mailNotify");

// 发送可靠消息（事务内）
online.sendReliableNotify(account, clientId, "mailNotify", notifyProtocol);

// 事务成功时发送
online.sendReliableNotifyWhileCommit(account, clientId, "mailNotify", p);

// 事务回滚时发送
online.sendReliableNotifyWhileRollback(account, clientId, "mailNotify", p);

// 关闭通道
online.removeReliableNotifyMark(account, clientId, "mailNotify");
```

`sendReliableNotify` 的 `listenerName` 参数使用 `runTaskOneByOneByKey` 串行化，保证同一通道的消息按序发送。

## SendToLogin（给某个登录发送）

给指定的 `(account, clientId)` 发送协议，只有该登录能收到。如果目标不在线，发送会被静默跳过。

```java
Protocol<?> p = new SMyNotify();
p.Argument.setXxx(...);

// 直接发送（可在事务外使用）
online.send(account, clientId, p);

// 事务提交时发送
online.sendWhileCommit(account, clientId, p);

// 事务回滚时发送
online.sendWhileRollback(account, clientId, p);

// 发送 Rpc 响应
online.sendResponse(account, clientId, rpc);
online.sendResponseWhileCommit(account, clientId, rpc);
```

底层通过 `sendDirect` 方法查表找到目标登录所在的 LinkName 和 LinkSid，然后通过 Linkd 转发给客户端。发送失败（如 Link 断开）会自动触发 `sendError` 处理链。

## SendToAccount（给某个账号发送）

给某个账号的 **所有登录** 发送消息。一个账号可能同时有多个 clientId 在线，此方法会给所有在线登录都发送一份。

```java
// 单个账号
online.sendAccount(account, p);
online.sendAccountWhileCommit(account, p);
online.sendAccountWhileRollback(account, p);

// 多个账号
online.sendAccounts(accounts, p);
online.sendAccountsWhileCommit(accounts, p);
online.sendAccountsWhileRollback(accounts, p);
```

底层通过 `sendAccountDirect` / `sendAccountsDirect` 实现，按 LinkName 分组批量发送，减少网络开销。

## Transmit（跨服数据查询）

在分布式架构中，用户被分散到不同 Server 实例。Transmit 用于将请求 **转发到目标用户所在的 Server** 上执行，以最大化缓存命中率。具体流程：

1. 调用 `transmit`，指定 sender、actionName 和 target 账号。
2. Online 通过 `groupByServer` 查找每个目标所在的 ServerId。
3. 在目标 Server 上通过 `transmitActions` 查找已注册的 `TransmitAction` 并执行。
4. 跨 Server 通过 `TransmitAccount` 协议（Rpc）转发。

```java
// 1. 注册 TransmitAction（启动时）
online.getTransmitActions().put("queryBag", (senderAccount, senderClientId,
        targetAccount, targetClientId, parameter) -> {
    // 在目标用户所在服务器上执行
    var result = bagModule.query(targetAccount);
    online.send(senderAccount, senderClientId, resultProtocol);
    return 0;
});

// 2. 发起 Transmit
online.transmit(senderAccount, senderClientId, "queryBag",
        targetAccount, targetClientId, null);

// 事务提交/回滚时执行
online.transmitWhileCommit(sender, clientId, "queryBag", target, targetClientId, param);
online.transmitWhileRollback(sender, clientId, "queryBag", target, targetClientId, param);
```

如果未启用 cache-sync（即单机模式），Transmit 直接在本地执行，不进行跨服转发。

## Broadcast（全系统广播）

向连接到当前 Provider 的 **所有 Linkd** 发送广播协议。每个 Linkd 再转发给它所服务的所有客户端。

```java
// 基本广播（默认 60 秒去重窗口）
int linkCount = online.broadcast(p);

// 指定去重窗口时间（毫秒）
online.broadcast(p, 30 * 1000);

// 仅广播给相同版本的客户端
online.broadcast(p, true);

// 同时指定时间和版本
online.broadcast(p, 30 * 1000, true);
```

`time` 参数为广播去重窗口，在此时间窗口内相同协议不会重复发送给同一客户端。`onlySameVersion` 控制是否只广播给与应用版本号一致的客户端。广播应谨慎使用，仅在真正需要全量推送的场景下使用。

## 其他接口

### 在线查询

```java
// 获取全局在线数据
BOnlines data = online.getOnline(account);
// 获取或新增
BOnlines data = online.getOrAddOnline(account);
// 获取指定登录
BOnline login = online.getLogin(account, clientId);
// 统计账号在线数
int count = online.getAccountLoginCount(account);
// 判断账号是否有任意登录
boolean any = online.isAccountLogin(account);
```

### 版本号

Online 使用 **LoginVersion**（单调递增）来判断登录数据的新旧，配合 **LogoutVersion** 实现登出事件补发机制。两者相等时表示已完成登出。

```java
Long loginVer = online.getLoginVersion(account, clientId);    // 在线时返回版本号，否则 null
Long logoutVer = online.getLogoutVersion(account, clientId);  // 离线时返回版本号，在线返回 null
Long localVer = online.getLocalLoginVersion(account, clientId); // 本机数据版本号
```

### 链路断开处理

当 Linkd 报告客户端连接断开时，Online 执行 `linkBroken` 流程：将状态设置为 `eLinkBroken`，然后启动一个 **延迟登出定时器**（由 `Config.OnlineLogoutDelay` 配置）。如果在延迟期间客户端 Relogin，则取消登出；超时后触发正式 Logout。

### 动态模块绑定

```java
// 将指定模块动态绑定到当前登录的 Linkd 会话上
online.bindDynamic(account, clientId, moduleId1, moduleId2);
```

## Game.Online 扩展

`Zeze.Game.Online`（→ game）在 Arch.Online 基础上以 roleId 替代 account + clientId 的组合，增加了以下能力：

- **OnlineSet**：支持通过 `createOnlineSet(name)` 创建多个独立的在线集合，各自维护独立的在线数据表和本机数据表。
- **LinkBrokenEvents**：新增断线事件（`getLinkBrokenEvents()`），参数为 `LinkBrokenArgument(roleId)`。
- **UserData**：通过 `setUserData` / `getUserData` 在在线数据上挂载自定义 Bean。
- **TimerRole**：集成角色级别的定时器管理。
- **sendAllOnlines**：向所有 OnlineSet 尝试发送，用于不确定角色在哪个集合中的场景。
