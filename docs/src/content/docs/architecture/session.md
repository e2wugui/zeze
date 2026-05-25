---
title: "Session 与 UserState"
sidebar:
  order: 5
---

在 Zeze 的 Provider-Linkd 架构中，用户会话信息通过 `UserState` 机制在连接、协议、事务之间传递。

## UserState 传递链

```
AsyncSocket.UserState
    |
    v
Protocol.setUserState(session)   // 框架在派发协议时设置
    |
    v
ProtocolHandle.handle(p)         // 业务处理器中通过 p.getUserState() 获取
```

| 类 | 字段 | 类型 | 用途 |
|----|------|------|------|
| `AsyncSocket` | `userState` | `Object` | 连接级别的会话对象 |
| `Protocol` | `userState` | `Object` | 协议处理时的会话上下文 |
| `Service.ManualContext` | `userState` | `Object` | 手动管理的异步上下文 |

在 `ProviderImplement.processDispatch` 中，框架创建 `ProviderUserSession` 并设置到协议上：

```java
var session = newSession(p);
p3.setUserState(session);
```

## ProviderUserSession — Provider 侧会话

封装了 Provider 处理用户请求时的完整会话信息。

### 核心属性

```java
ProviderUserSession session = ProviderUserSession.get(protocol);
session.getAccount();         // 账号名
session.getRoleId();          // 角色 ID（未登录返回 null）
session.getRoleIdNotNull();   // 角色 ID（未登录抛异常）
session.getLinkName();        // 所在 Linkd 名称
session.getLinkSid();         // Linkd 上的连接 sessionId
session.isLogin();            // 是否已登录
```

### 发送协议给客户端

协议自动封装为 `Send` 经 Linkd 转发：

```java
session.sendResponse(protocol);                        // 发送普通协议
session.sendResponse(typeId, fullEncodedProtocol);     // 发送已编码协议
session.sendResponseWhileCommit(protocol);             // 事务提交后发送（推荐）
session.sendResponseWhileRollback(protocol);           // 事务回滚后发送
```

`sendResponseWhileCommit` 保证只有事务成功提交后才发送响应，避免数据不一致。

### 踢出用户与获取会话

```java
session.kick(code, "reason");  // 踢出用户

// 业务处理器中获取会话，未认证时抛 IllegalStateException
ProviderUserSession session = ProviderUserSession.get(p);
```

## LinkdUserSession — Linkd 侧会话

Linkd 维护的客户端会话，记录连接状态和模块绑定关系。

### 核心属性与模块绑定

```java
session.getAccount();            // 账号名
session.getRoleId();             // 角色 ID
session.getSessionId();          // 客户端连接 sessionId
session.isAuthed();              // 是否已认证

// 模块路由：moduleId -> providerSessionId
session.bind(service, link, moduleIds, providerSocket);
session.unbind(service, link, moduleId, providerSocket);
Long providerSid = session.tryGetProvider(moduleId);
```

### 连接关闭

客户端断开时 `onClose()` 遍历所有绑定，向对应 Provider 发送 `LinkBroken` 协议通知清理在线状态。

## 最佳实践

**检查登录状态**：始终通过 `getRoleId()` 检查，为 null 表示未选择角色。

**事务中发送响应**：优先使用 `sendResponseWhileCommit` 保证一致性。

**自定义 UserState**：可在 `OnSocketAccept` 中通过 `so.setUserState()` 绑定连接级状态。
