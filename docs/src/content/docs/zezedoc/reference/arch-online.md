---
title: "Online 在线管理"
description: "Zeze 基于账号/角色的在线状态管理与跨服消息转发机制"
category: reference
order: 11
---

> 本文档描述 Zeze 在线状态管理模块 `Online` 的表结构、状态常量、事件机制、可靠消息、消息发送与跨服转发接口，供在线业务开发检索参考。

## 模块定位

| 模块 | 包路径 | 说明 |
|------|--------|------|
| `Online` | `Zeze.Arch.Online` | 基于账号的在线状态管理，跟踪登录登出、维护本机数据、发送在线消息、跨服查询转发 |
| `Online` | `Zeze.Game.Online` | 游戏场景，以 `roleId` 为键，增加 `LinkBrokenEvents` / `OnlineSet` 扩展 |

在线状态以 `(account, clientId)` 唯一标识，`clientId` 允许多端。

## 内部表结构

| 表 | 作用 | 范围 |
|----|------|------|
| `_tonline` | 全局 `BOnlines`，记录 `LinkName` / `LinkSid` / 状态 / `LoginVersion`，所有 Server cache-sync 共享 | 全局共享 |
| `_tlocal` | 本机 `BLocals`，仅当前 Server 内存表 | 本机内存 |
| `ReliableNotifyQueue` | 可靠消息队列，持久化断线补发 | 本机 |

## 状态常量

| 常量 | 值 | 含义 |
|------|----|------|
| `eOffline` | 0 | 离线 |
| `eLinkBroken` | 1 | 链路断开 |
| `eLogined` | 2 | 已登录 |

## 在线查询

```java
online.isOnline(account);             // 账号是否在线
online.isOnline(account, clientId);   // 指定端是否在线
online.getState();                    // 获取状态
```

## 本机数据管理

| 接口 | 事务要求 | 说明 |
|------|----------|------|
| `setLocalBean(account, clientId, key, bean)` | 必须事务内 | 设置本机数据（带 clientId、key） |
| `getLocalBean(account, clientId, key)` | - | 获取本机数据 |
| `getOrAddLocalBean(account, clientId, key, supplier)` | 必须事务内 | 获取或添加（带 clientId、key 与默认值构造器） |
| `removeLocalBean(account, clientId, key)` | - | 移除本机数据 |
| `walkLocal((k, v) -> {...})` | **事务外** | 遍历本机数据，返回 `false` 中断 |

> 注意：`setLocalBean` 和 `getOrAddLocalBean` 必须在事务内；`walkLocal` 不在事务中执行。

## 在线事件（EventDispatcher）

四种事件类型：

| 事件 | 触发时机 |
|------|----------|
| `LoginEvents` | 首次登录 |
| `ReloginEvents` | 断线重连 |
| `LogoutEvents` | 登出（含补发） |
| `LocalRemoveEvents` | 本机数据删除 |

事件分三阶段触发：`triggerEmbed` → `triggerProcedure` → `triggerThread`。

```java
online.getLoginEvents().run((sender, arg) -> {
    // 返回 0 成功
    return 0;
});
```

## 登出事件丢失与补发

服务器异常关闭时无法触发 `Logout`。下一次 `Login` 发现上一个没 `Logout`，会先补发 `Logout` 再 `Login`（用 `done` 标志循环重试）。`Logout` 可能与 `Login` 时间相隔很远，精确时间统计不应完全依赖该机制。

## ReliableNotify（可靠消息）

可靠消息需客户端确认，先持久化到队列再发送。

| 接口 | 说明 |
|------|------|
| `addReliableNotifyMark(account, clientId, listenerName)` | 启用通道（带 account、clientId、listenerName） |
| `sendReliableNotify(p)` | 发送可靠消息 |
| `sendReliableNotifyWhileCommit(p)` | 事务提交时发送 |
| `sendReliableNotifyWhileRollback(p)` | 事务回滚时发送 |
| `removeReliableNotifyMark(account, clientId, listenerName)` | 关闭通道 |

`Relogin` 时未确认消息同步；`runTaskOneByOneByKey` 串行化，保证按序。

## SendToLogin（发送给登录端）

| 接口 | 说明 |
|------|------|
| `send(account, clientId, p)` | 发送（三参数：account、clientId、协议） |
| `sendWhileCommit` | 事务提交时发送 |
| `sendWhileRollback` | 事务回滚时发送 |
| `sendResponse` | 发送响应 |
| `sendResponseWhileCommit` | 事务提交时发送响应 |

目标不在线时静默跳过。底层 `sendDirect` 查表找 `LinkName` / `LinkSid`，经 Linkd 转发，失败触发 `sendError`。

## SendToAccount（发送给账号所有登录端）

| 接口 | 说明 |
|------|------|
| `online.sendAccount(p)` | 发给账号所有登录端 |
| `online.sendAccounts(p)` | 按账号列表发送，按 `LinkName` 分组批量 |

## Transmit（跨服数据查询）

| 接口 | 说明 |
|------|------|
| `transmit(senderAccount, senderClientId, actionName, targetAccount, targetClientId, parameter)` | 跨服转发 |

注册 action：

```java
online.getTransmitActions().put("queryBag", (sender, senderClientId, targetAccount, targetClientId, parameter) -> {
    // 处理查询
    return 0;
});
```

`transmitWhileCommit` / `transmitWhileRollback` 对应事务时机。未启用 cache-sync 时，单机直接本地执行。

## Broadcast（广播）

向当前 Provider 所有 Linkd 广播，每个 Linkd 转发其客户端。

| 接口 | 说明 |
|------|------|
| `broadcast(p)` | 广播 |
| `broadcast(p, time)` | 去重窗口（默认 60 秒） |
| `broadcast(p, true)` | 仅同版本 |
| `broadcast(p, time, onlySameVersion)` | 去重 + 同版本 |

## 其他接口

| 接口 | 说明 |
|------|------|
| `getOnline` / `getOrAddOnline` | 获取/新增在线记录 |
| `getLogin` | 获取登录信息 |
| `getAccountLoginCount` | 账号登录端数量 |
| `isAccountLogin` | 账号是否登录 |
| `getLoginVersion` / `getLogoutVersion` / `getLocalLoginVersion` | LoginVersion 单调递增，判断登录数据新旧 |
| `bindDynamic(account, clientId, moduleId1, moduleId2)` | 动态模块绑定 |

## 链路断开处理

`linkBroken` 设为 `eLinkBroken`，启动延迟登出定时器（`Config.OnlineLogoutDelay`）。期间 `Relogin` 可取消登出；超时则正式 `Logout`。

## Game.Online 扩展

| 扩展 | 说明 |
|------|------|
| `OnlineSet` | `createOnlineSet(name)` 创建多个独立在线集合 |
| `LinkBrokenEvents` | 链路断开事件（`LinkBrokenArgument(roleId)`） |
| `UserData` | `setUserData` / `getUserData` 挂载自定义 Bean |
| `TimerRole` | 角色定时器 |
| `sendAllOnlines` | 向所有 OnlineSet 尝试发送 |

## 相关文档

- Provider-Linkd 架构：[./arch-provider-linkd.md](./arch-provider-linkd.md)
- 定时器服务：[./svc-timer.md](./svc-timer.md)
- 分布式入门：[../manual/05-going-distributed.md](../manual/05-going-distributed.md)
