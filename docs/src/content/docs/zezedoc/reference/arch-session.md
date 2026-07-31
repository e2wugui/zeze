---
title: "Session 与 UserState"
description: "Zeze 用户会话信息在连接、协议、事务间传递的 UserState 机制"
category: reference
order: 14
---

> 本文档描述 Zeze 用户会话信息通过 `UserState` 机制在连接、协议、事务间传递的流程，以及 `ProviderUserSession` 与 `LinkdUserSession` 的核心属性与接口，供会话管理开发检索参考。

## UserState 传递链

用户会话信息通过 `UserState` 机制在连接、协议、事务间传递。

```
AsyncSocket.UserState
        │  (框架派发时设置)
        ▼
Protocol.setUserState(session)
        │
        ▼
ProtocolHandle.handle(p)
        │  p.getUserState() 获取
        ▼
业务处理器
```

| 层级 | 接口 | 说明 |
|------|------|------|
| `AsyncSocket` | `userState(Object)` | 连接级附加状态 |
| `Protocol` | `userState(Object)` | 协议级附加状态 |
| `Service.ManualContext` | `userState(Object)` | 上下文级附加状态 |

### ProviderImplement 中创建 Session

`ProviderImplement.processDispatch` 创建 `ProviderUserSession` 并设置到协议：

```java
ProviderUserSession session = newSession(p);
p3.setUserState(session);
```

## ProviderUserSession

封装 Provider 处理用户请求的完整会话。

### 核心属性

| 属性 | 说明 |
|------|------|
| `getAccount()` | 获取账号 |
| `getRoleId()` | 获取角色 ID（未登录返回 `null`） |
| `getRoleIdNotNull()` | 获取角色 ID（未登录抛异常） |
| `getLinkName()` | 获取 Linkd 名称 |
| `getLinkSid()` | 获取链路 SessionId |
| `isLogin()` | 是否已登录 |

### 发送协议给客户端

发送协议自动封装 `Send`，经 Linkd 转发。

| 接口 | 说明 |
|------|------|
| `sendResponse(protocol)` | 发送协议 |
| `sendResponse(typeId, fullEncodedProtocol)` | 发送已编码协议 |
| `sendResponseWhileCommit(protocol)` | **推荐**，事务提交时发送 |
| `sendResponseWhileRollback(protocol)` | 事务回滚时发送 |

### 踢出与获取会话

| 接口 | 说明 |
|------|------|
| `kick(code, "reason")` | 按 code 踢出，附带原因 |
| `ProviderUserSession.get(p)` | 从协议获取会话（未认证抛 `IllegalStateException`） |

```java
public long processRequest(MyProtocol p) {
    ProviderUserSession session = ProviderUserSession.get(p);
    if (!session.isLogin()) {
        return Procedure.LogicError;
    }
    session.sendResponseWhileCommit(new MyResponse());
    return 0;
}
```

## LinkdUserSession

维护客户端会话。

### 核心属性与模块绑定

| 属性 / 接口 | 说明 |
|-------------|------|
| `getAccount()` | 获取账号 |
| `getRoleId()` | 获取角色 ID |
| `getSessionId()` | 获取会话 ID |
| `isAuthed()` | 是否已认证 |
| `bind(service, link, moduleIds, providerSocket)` | 绑定模块 |
| `unbind(...)` | 解绑 |
| `tryGetProvider(moduleId)` | 尝试获取模块对应的 Provider |

### 连接关闭处理

`onClose()` 遍历所有绑定，向 Provider 发送 `LinkBroken` 通知并清理。

## 最佳实践

| 实践 | 说明 |
|------|------|
| 检查登录状态 | 始终检查 `getRoleId()`，为 `null` 表示未选角色 |
| 事务中发响应 | 优先使用 `sendResponseWhileCommit` |
| 自定义 UserState | 在 `OnSocketAccept` 中调用 `so.setUserState(...)` 设置 |

```java
@Override
public void OnSocketAccept(AsyncSocket so) {
    super.OnSocketAccept(so);
    MySession session = new MySession(so.getSessionId());
    so.setUserState(session);
}
```

## 相关文档

- Provider-Linkd 架构：[./arch-provider-linkd.md](./arch-provider-linkd.md)
- 在线管理：[./arch-online.md](./arch-online.md)
- 网络层：[./arch-net.md](./arch-net.md)
