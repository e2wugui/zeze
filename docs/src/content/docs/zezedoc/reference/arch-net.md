---
title: "网络层"
description: "Zeze 基于 JDK NIO 自研的轻量异步网络通信框架及协议编解码机制（WebSocket 子模块用 Netty）"
category: reference
order: 13
---

> 本文档描述 Zeze 基于 **JDK NIO 自研**的轻量异步网络层（仅 WebSocket 子模块使用 Netty），包括 Service 抽象、Protocol 编码格式、Rpc 模型、连接管理、握手加密压缩、WebSocket 支持与协议派发流程，供网络通信开发检索参考。

## 总览

Zeze 的核心 TCP 网络层**并非基于 Netty**，而是用 JDK 原生 `java.nio.channels.Selector` 自研的事件驱动 IO 线程（`Zeze.Net.Selector` 继承自 `Thread`）：发送不阻塞，接收解码为协议对象后派发到线程池。**只有 WebSocket 子模块**（`Websocket` / `WebsocketHandle`）引入了 Netty。

## Service（网络服务抽象基类）

`Zeze.Net.Service` 是所有网络服务的抽象基类。

| 组成 | 说明 |
|------|------|
| 连接管理 | `socketMap`（`LongConcurrentHashMap<AsyncSocket>`），按 `sessionId` 索引 |
| 协议注册派发 | `ProtocolFactoryHandle` 注册工厂与处理器，收到数据按 `typeId` 查找派发 |
| 配置管理 | `SocketOptions` + `ServiceConf` |
| 生命周期 | `start()` / `stop()` |

### 继承 Service 使用

重载事件方法时**必须调用基类实现**。

| 事件方法 | 说明 |
|----------|------|
| `OnSocketAccept` | 接受新连接（可检查最大连接） |
| `OnSocketConnected` | 客户端连接成功，加入 `socketMap` |
| `OnSocketClose` | 关闭，移除 |
| `OnHandshakeDone` | 握手完成，通知 `Connector` |
| `OnSocketProcessInputBuffer` | 接收数据，可自定义协议解析 |

## Protocol（协议基类）

`Zeze.Net.Protocol<TArgument>` 是所有协议的基类，包含 `Argument`（Bean）。

### 编码格式

```
Header = ModuleId[4] + ProtocolId[4] + Size[4]
Payload = FamilyClass[1] + [ResultCode[8]] + EncodedArgument
```

| 概念 | 公式 / 说明 |
|------|-------------|
| TypeId | `(long)moduleId << 32 \| protocolId`，全局唯一 |
| ResultCode | 非 0 才编码（`BitResultCode` 标志位） |
| 注册 | `Protocol.register(typeId, class)` |

### CriticalLevel（协议优先级）

| 级别 | 值 | 说明 |
|------|----|------|
| `eCriticalPlus` | 0 | 最高 |
| `eCritical` | 1 | 高 |
| `eNormal` | 2 | 默认 |
| `eSheddable` | 3 | 可丢弃 |

负载高时按优先级丢低级别。

### 发送协议

```java
p.Send(asyncSocket);   // 指定连接
p.Send(service);       // 使用第一个连接
```

### 协议序列化性能优化

协议层和数据库共享的 Bean 按事务模式实现，容器性能差（如 `BSend` 历史上 `Set` 改 `List` 优化）。可手动优化：

```java
// 自定义 Bean
public class MyBean extends Bean { /* ... */ }

// 自定义协议
public class MyProtocol extends Protocol<MyBean> { /* ... */ }

// 框架注册后，删除旧的 ProtocolFactoryHandle，注册自己的
```

## Rpc

`Zeze.Net.Rpc<TArgument, TResult>` 继承 `Protocol`，增加 `Result` 和 `sessionId`。

### 编码格式

```
Header
Payload = FamilyClass[1] + [ResultCode[8]] + SessionId[8] + ArgumentOrResult
```

| FamilyClass | 值 | 说明 |
|-------------|----|------|
| Request | 1 | 请求 |
| Response | 0 | 响应 |

### 发送方式

| 方式 | 接口 | 说明 |
|------|------|------|
| 异步回调 | `rpc.Send(socket, (response) -> {...}, 5000)` | 超时 5000ms |
| 同步等待 | `rpc.SendForWait(socket, 5000)` + `future.await()` + `rpc.getResultCode()` | 阻塞等待 |
| 发送响应 | `rpc.SendResult()` / `SendResultCode(0)` | 成功 |
| 带结果响应 | `setResultCode(0)` + `Result.setYyy` + `SendResult` | 设置结果后发送 |

### 超时机制

`rpcContexts` 映射：发请求时注册上下文并启动定时器，超时 `isTimeout = true`、`resultCode = Procedure.Timeout`。

## 连接管理

### Acceptor（服务端）

| 配置 | 说明 |
|------|------|
| 具体地址 | 指定 IP/Port |
| `@internal` | 内网 |
| `@external` | 外网 |

```xml
<Acceptor Ip="@internal" Port="5555"/>
```

### Connector（客户端）

| 特性 | 说明 |
|------|------|
| 自动重连 | `isAutoReconnect`，指数退避，初始 1 秒，最大 `maxReconnectDelay` |
| 握手就绪 | `GetReadySocket` / `TryGetReadySocket` |
| 自定义子类 | `class` 配置 |

```java
new Connector("127.0.0.1", 5555, true);  // 自动重连
connector.setMaxReconnectDelay(30_000);
```

支持 TCP 和 WebSocket。

## AsyncSocket

抽象基类。

| 接口 / 属性 | 说明 |
|-------------|------|
| `Send(Protocol)` / `Send(ByteBuffer)` / `Send(Binary)` / `Send(byte[])` | 发送 |
| `UserState` | 附加任意状态 |
| `recvCount` / `sendSize` | 统计 |
| `activeRecvTime` / `activeSendTime` | 活跃时间，KeepAlive 检测 |
| `isHandshakeDone` | 握手是否完成 |
| `Type` 枚举 | `eServer` / `eClient` / `eServerSocket` |

## 压缩与加密握手

`HandshakeBase` 及子类 `HandshakeServer` / `HandshakeClient`。

### 加密类型表

| `eEncryptType` | 值 | 说明 |
|----------------|----|------|
| `Disable` | 0 | 不加密 |
| `Aes` | 1 | AES + DH（依赖 IP） |
| `AesNoSecureIp` | 2 | AES（不依赖 IP） |
| `RsaAes` | 3 | RSA + AES |

### 压缩类型表

| `eCompressType` | 值 | 说明 |
|-----------------|----|------|
| `Disable` | 0 | 不压缩 |
| `Mppc` | 1 | Mppc |
| `Zstd` | 2 | Zstd |

### 握手流程

```
SHandshake0 (支持加密压缩)
    │
    ▼
CHandshake  (选择)
    │
    ▼
SHandshake  (加密参数)
    │
    ▼
CHandshakeDone
```

握手协议在 IO 线程同步执行，`DispatchMode.Direct`。

## WebSocket

`Zeze.Net.Websocket` / `WebsocketClient`。

```java
new Connector(true, "ws://127.0.0.1:8080/ws");
```

与 TCP 透明，共享 Service 事件。

## SSL KeyExchange

`Zeze.Services.Handshake.KeyExchange` 提供独立的 RSA + AES 密钥交换 RPC，支持双向认证。

```java
keyExchange.send(socket, clientPrivateKey);
KeyExchange.addHandler(service, serverPrivateKey);
```

## SocketOptions

| 选项 | 默认值 | 说明 |
|------|--------|------|
| `inputBufferMaxProtocolSize` | 2M | 输入缓冲最大协议大小 |
| `outputBufferMaxSize` | 2M | 输出缓冲最大大小 |
| `noDelay` | - | TCP NoDelay |
| `sendBuffer` | - | 发送缓冲 |
| `receiveBuffer` | - | 接收缓冲 |
| `closeWhenMissHandle` | false | 找不到处理器时是否关闭连接 |

## 协议接收处理流程

```
AsyncSocket 接收
    │
    ▼
解密解压
    │
    ▼
Service.OnSocketProcessInputBuffer
    │
    ▼
Protocol.decode
    │
    ▼
dispatchUnknownProtocol / dispatchProtocol
    ├─ 握手：IO 线程同步执行
    ├─ 事务：创建 Procedure，提交线程池
    └─ 非事务：直接提交线程池
```

## 协议日志

| JVM 参数 | 作用 |
|----------|------|
| `-DprotocolLog=DEBUG` | 开启协议日志 |
| `-DprotocolLogExcept=TypeId` | 排除指定 TypeId |

日志格式：`SEND` / `RECV` / `Send` / `Broc` / `Recv` + sessionId + 协议名。

> Linkd 建议排除 `Dispatch` 和 `Send` 的 TypeId，只输出包装的协议。

## 配置示例

```xml
<ServiceConf>
    <Acceptor Ip="@internal" Port="5555"/>
    <Acceptor Ip="@external" Port="5556"/>
    <Connector Ip="127.0.0.1" Port="5001" AutoReconnect="true"/>
</ServiceConf>
```

Acceptor 和 Connector 可在同一 ServiceConf 中共存。

## 相关文档

- Provider-Linkd 架构：[./arch-provider-linkd.md](./arch-provider-linkd.md)
- Session 与 UserState：[./arch-session.md](./arch-session.md)
- 序列化：[./serialize.md](./serialize.md)
- 分布式入门：[../manual/05-going-distributed.md](../manual/05-going-distributed.md)
