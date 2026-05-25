---
title: "网络层"
sidebar:
  order: 4
---

Zeze 包含一套轻量级的异步网络实现，基于 Netty 构建。发送操作不会阻塞，接收数据由底层解码为协议对象后派发到线程池执行。

## Service — 网络服务基类

`Zeze.Net.Service` 是所有网络服务的抽象基类，承担以下职责：

- **连接管理**：维护 `socketMap`（`LongConcurrentHashMap<AsyncSocket>`），按 sessionId 索引所有活跃连接
- **协议注册与派发**：通过 `ProtocolFactoryHandle` 注册协议工厂和处理器，收到数据后根据 typeId 查找并派发
- **配置管理**：持有 `SocketOptions` 和 `ServiceConf`，统一管理连接参数
- **生命周期**：`start()` / `stop()` 控制服务的启动和关闭

应用通过继承 `Service` 的方式使用网络模块。重载事件方法时，通常需要调用基类的实现。

```java
public class MyService extends Service {
    public MyService(String name, Application app) {
        super(name, app);
    }

    @Override
    public void OnSocketAccept(AsyncSocket so) throws Exception {
        super.OnSocketAccept(so); // 必须调用基类
        // 自定义处理...
    }
}
```

### 关键事件方法

| 方法 | 触发时机 | 说明 |
|------|----------|------|
| `OnSocketAccept` | 服务器接受新连接 | 可检查最大连接数 |
| `OnSocketConnected` | 客户端连接成功 | 加入 socketMap |
| `OnSocketClose` | 连接关闭 | 从 socketMap 移除 |
| `OnHandshakeDone` | 握手完成 | 通知 Connector 连接就绪 |
| `OnSocketProcessInputBuffer` | 接收到数据 | 可自定义协议解析 |

## Protocol — 协议基类

`Zeze.Net.Protocol<TArgument>` 是所有协议的基类，包含一个 `Argument` 参数（Bean 类型）。

### 编码格式

```
Header = ModuleId[4] + ProtocolId[4] + Size[4]
Payload = FamilyClass[1] + [ResultCode[8]] + EncodedArgument
```

- `TypeId = (long)moduleId << 32 | protocolId`，全局唯一标识一种协议
- `ResultCode`：非 0 时才编码写入（通过 `BitResultCode` 标志位区分）
- 协议注册通过 `Protocol.register(typeId, class)` 静态方法完成

### CriticalLevel — 协议优先级

```java
public static final int eCriticalPlus = 0;  // 最高优先级
public static final int eCritical = 1;
public static final int eNormal = 2;        // 默认
public static final int eSheddable = 3;     // 可丢弃
```

在负载过高时，框架可根据优先级丢弃低级别协议。

### 发送协议

```java
MyProtocol p = new MyProtocol();
p.Argument.setXxx(...);
p.Send(asyncSocket);    // 发送到指定连接
p.Send(service);        // 发送到 service 的第一个连接
```

### 协议序列化性能

由于为了在协议层和数据库存储层共享结构（Bean），协议序列化在服务器端也是按支持事务的模式实现的。支持事务的容器性能会差一些。比如 Provider 发送给 Linkd 的 Send 协议非常频繁，其参数 BSend 有个容器变量，历史上容器类型是 Set，序列化性能太差，后来改成了 List。当遇到协议序列化性能问题时，可以考虑类似的优化手段。

如果需要进一步优化，可以选择手动方式：

1. 实现自己的协议参数 `MyBean`
2. 新建 `MyProtocol extends Protocol<MyBean>`
3. 在框架默认协议注册完成后，删除旧的 `ProtocolFactoryHandle`
4. 注册自己的 `MyProtocol`

## Rpc — 请求-响应模式

`Zeze.Net.Rpc<TArgument, TResult>` 继承自 `Protocol`，增加了 `Result` 参数和 `sessionId` 字段，实现请求-响应关联。

### 编码格式

```
Header = ModuleId[4] + ProtocolId[4] + Size[4]
Payload = FamilyClass[1] + [ResultCode[8]] + SessionId[8] + ArgumentOrResult
```

`FamilyClass` 区分 `Request(1)` 和 `Response(0)`。

### 发送方式

**异步回调**：

```java
MyRpc rpc = new MyRpc();
rpc.Argument.setXxx(...);
rpc.Send(socket, (response) -> {
    // 处理响应
    return 0;
}, 5000); // 超时 5 秒
```

**同步等待**：

```java
MyRpc rpc = new MyRpc();
var future = rpc.SendForWait(socket, 5000);
future.await(); // 阻塞等待结果
if (rpc.getResultCode() != 0) {
    // 错误处理
}
```

**发送响应**：

```java
// 在服务端处理 Rpc 请求时
rpc.SendResult();                    // 发送空结果
rpc.SendResultCode(0);               // 发送带 ResultCode 的结果
rpc.setResultCode(0);
rpc.Result.setYyy(...);
rpc.SendResult();                    // 发送带数据的结果
```

### 超时机制

Rpc 内部维护 `rpcContexts` 映射表，发送请求时注册上下文并启动定时器。超时后回调会设置 `isTimeout = true` 和 `resultCode = Procedure.Timeout`。

## 连接管理

### Acceptor — 服务器监听

`Zeze.Net.Acceptor` 封装了 `bind` + `listen` 的 ServerSocket。支持 IP 配置：

- 具体地址（如 `127.0.0.1`）
- `@internal` 自动选择内网 IP
- `@external` 自动选择外网 IP

```xml
<Acceptor Ip="127.0.0.1" Port="5555"/>
```

### Connector — 客户端连接器

`Zeze.Net.Connector` 管理一个客户端连接，支持：

- **自动重连**：`isAutoReconnect = true` 时，连接断开后指数退避重连（初始 1 秒，最大 `maxReconnectDelay`）
- **握手就绪**：`GetReadySocket()` / `TryGetReadySocket()` 等待握手完成
- **自定义子类**：可通过配置 `class="FullClassName"` 或动态创建来扩展连接逻辑

```java
Connector conn = new Connector("127.0.0.1", 5555, true);
conn.setMaxReconnectDelay(8000);
```

Connector 可同时支持 TCP 和 WebSocket 连接。

## AsyncSocket — 异步套接字

`Zeze.Net.AsyncSocket` 是网络连接的抽象基类，提供：

- **发送接口**：`Send(Protocol)`、`Send(ByteBuffer)`、`Send(Binary)`、`Send(byte[])` 等
- **UserState**：可附加任意用户状态对象，用于关联会话信息
- **统计信息**：收发计数和字节数（`recvCount`、`sendSize` 等）
- **活跃时间**：`activeRecvTime`、`activeSendTime`，用于 KeepAlive 检测
- **握手状态**：`isHandshakeDone` 标记握手是否完成

### 类型

```java
public enum Type {
    eServer,       // 服务端接受的连接
    eClient,       // 客户端主动连接
    eServerSocket, // 监听套接字
}
```

## 压缩与加密

Zeze 通过 `HandshakeBase` 及其子类（`HandshakeServer`、`HandshakeClient`）实现连接建立时的加密和压缩协商。

### 加密类型

| 常量 | 值 | 说明 |
|------|----|------|
| `eEncryptTypeDisable` | 0 | 不加密 |
| `eEncryptTypeAes` | 1 | AES + DH 密钥交换（依赖 IP） |
| `eEncryptTypeAesNoSecureIp` | 2 | AES + DH 密钥交换（不依赖 IP） |
| `eEncryptTypeRsaAes` | 3 | RSA + AES 密钥交换 |

### 压缩类型

| 常量 | 值 | 说明 |
|------|----|------|
| `eCompressTypeDisable` | 0 | 不压缩 |
| `eCompressTypeMppc` | 1 | MPPC 压缩 |
| `eCompressTypeZstd` | 2 | Zstd 压缩 |

### 握手流程

```
Server                                Client
  |  --- SHandshake0(支持的加密压缩) -->  |
  |  <-- CHandshake(选择的加密压缩) ---   |
  |  --- SHandshake(加密参数) --------->  |
  |  <-- CHandshakeDone ---------------   |
  |          握手完成，开始加密通信          |
```

握手协议在 IO 线程中同步处理（`DispatchMode.Direct`），不经过线程池。

## WebSocket 支持

Zeze 通过 `Zeze.Net.Websocket` 和 `Zeze.Net.WebsocketClient` 提供 WebSocket 支持。Connector 可配置 URL 方式建立 WebSocket 连接：

```java
Connector wsConn = new Connector(true, "ws://127.0.0.1:8080/ws");
```

WebSocket 连接与 TCP 连接对上层完全透明，共享相同的 `Service` 事件处理和协议派发机制。

## SSL 握手（KeyExchange）

`Zeze.Services.Handshake.KeyExchange` 提供了独立的 RSA + AES 密钥交换 RPC，支持双向认证：

```java
// 客户端
var keyExchange = new KeyExchange(serverPubKey, clientPubKey);
keyExchange.send(socket, clientPrivateKey);

// 服务端
KeyExchange.addHandler(service, serverPrivateKey);
```

流程：客户端生成随机 AES 密钥用服务器 RSA 公钥加密发送；服务器解密后生成自己的密钥，用客户端公钥加密（或 XOR 方式）回传。之后双向使用各自的 AES 密钥加密通信。

## SocketOptions — 连接参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `inputBufferMaxProtocolSize` | 2 MB | 单个协议包最大大小 |
| `outputBufferMaxSize` | 2 MB | 发送缓冲区最大堆积 |
| `noDelay` | 系统默认 | TCP_NODELAY |
| `sendBuffer` / `receiveBuffer` | 系统默认 | Socket 缓冲区大小 |
| `closeWhenMissHandle` | false | 未注册处理器时是否关闭连接 |

## 协议接收处理流程

```
AsyncSocket 接收数据
  |
  v
解密解压（可选）
  |
  v
Service.OnSocketProcessInputBuffer   [可重载]
  |
  v
Protocol.decode (按 moduleId+protocolId+size 分包)
  |
  +---> Service.dispatchUnknownProtocol   [未注册协议]
  |
  +---> Service.dispatchProtocol          [已注册协议]
          |
          +---> 握手协议: IO 线程同步执行
          +---> 事务协议: 创建 Procedure 提交线程池
          +---> 非事务协议: 直接提交线程池
```

标记 `*` 的步骤可通过重载 Service 方法自定义。

## 协议日志

协议日志通过加 JVM 参数来开启，如：`-DprotocolLog=DEBUG`。可通过加 JVM 参数来排除不需要输出日志的协议类型（TypeId），如：`-DprotocolLogExcept=3504939016,42955910777365`。以上参考 `Zeze.Net.AsyncSocket` 的定义。

以下是所有协议日志的格式：

### 通过 Zeze.Net.AsyncSocket.Send(Protocol) 直接发协议

```
SEND:连接sessionId RPC类名:RPC的sessionId 请求Bean内容         // 发送RPC请求
SEND:连接sessionId RPC类名:RPC的sessionId>resultCode 回复Bean内容 // 发送RPC回复
SEND:连接sessionId 协议类名 协议Bean内容                        // 发送协议
SEND:连接sessionId 协议类名>resultCode 协议Bean内容              // 发送带resultCode的协议
```

### 通过 Zeze.Net.Protocol.decode 接收协议

```
RECV:连接sessionId RPC类名:RPC的sessionId 请求Bean内容         // 接收RPC请求
RECV:连接sessionId RPC类名:RPC的sessionId>resultCode 回复Bean内容 // 接收RPC回复
RECV:连接sessionId 协议类名 协议Bean内容                        // 接收协议
RECV:连接sessionId 协议类名>resultCode 协议Bean内容              // 接收带resultCode的协议
```

如果"协议类名/RPC类名"未知，会用 `moduleId:protocolId` 代替，同时 Bean 内容会用 `header[bean大小]` 代替。

### Linkd 通过 Zeze.Arch.LinkdProvider.ProcessSendRequest 处理的 Send 协议并转发里面的协议给客户端

```
Send:连接sessionId RPC类名:RPC的sessionId 请求Bean内容         // 发送RPC请求
Send:连接sessionId RPC类名:RPC的sessionId>resultCode 回复Bean内容 // 发送RPC回复
Send:连接sessionId 协议类名 协议Bean内容                        // 发送协议
Send:连接sessionId 协议类名>resultCode 协议Bean内容              // 发送带resultCode的协议
```

多组"连接sessionId"会以 `sessionId,sessionId,...` 方式输出，多于 10 个 sessionId 会以 `[sessionId数量]` 方式输出。

### Linkd 通过 Zeze.Arch.LinkdProvider.ProcessBroadcast 处理的 Broadcast 协议并广播里面的协议给客户端

```
Broc:客户端连接数 RPC类名:RPC的sessionId 请求Bean内容         // 发送RPC请求
Broc:客户端连接数 RPC类名:RPC的sessionId>resultCode 回复Bean内容 // 发送RPC回复
Broc:客户端连接数 协议类名 协议Bean内容                        // 发送协议
Broc:客户端连接数 协议类名>resultCode 协议Bean内容              // 发送带resultCode的协议
```

### Provider 通过 Zeze.Arch.ProviderImplement.ProcessDispatch 接收封装成 Dispatch 的协议

```
Recv:roleId RPC类名:RPC的sessionId 请求Bean内容         // 接收RPC请求
Recv:roleId RPC类名:RPC的sessionId>resultCode 回复Bean内容 // 接收RPC回复
Recv:roleId 协议类名 协议Bean内容                        // 接收协议
Recv:roleId 协议类名>resultCode 协议Bean内容              // 接收带resultCode的协议
```

如果当前 roleId 无效，则用负的 linkSid 代替。

### Provider 通过 Zeze.Arch.ProviderUserSession.sendResponse(Protocol) 发封装成 Send 的协议

```
Send:roleId RPC类名:RPC的sessionId 请求Bean内容         // 发送RPC请求
Send:roleId RPC类名:RPC的sessionId>resultCode 回复Bean内容 // 发送RPC回复
Send:roleId 协议类名 协议Bean内容                        // 发送协议
Send:roleId 协议类名>resultCode 协议Bean内容              // 发送带resultCode的协议
```

如果当前 roleId 无效，则用负的 linkSid 代替。

### Provider 通过 Zeze.Game.Online.send 发封装成 Send 的协议

```
Send:roleId RPC类名:RPC的sessionId 请求Bean内容         // 发送RPC请求
Send:roleId RPC类名:RPC的sessionId>resultCode 回复Bean内容 // 发送RPC回复
Send:roleId 协议类名 协议Bean内容                        // 发送协议
Send:roleId 协议类名>resultCode 协议Bean内容              // 发送带resultCode的协议
```

多组"roleId"会以 `roleId,roleId,...` 方式输出。

### Provider 通过 Zeze.Game.Online.sendReliableNotify 发封装成 SReliableNotify 的协议

```
Send:roleId:listenerName RPC类名:RPC的sessionId 请求Bean内容         // 发送RPC请求
Send:roleId:listenerName RPC类名:RPC的sessionId>resultCode 回复Bean内容 // 发送RPC回复
Send:roleId:listenerName 协议类名 协议Bean内容                        // 发送协议
Send:roleId:listenerName 协议类名>resultCode 协议Bean内容              // 发送带resultCode的协议
```

### Provider 通过 Zeze.Arch/Game.Online.broadcast(Protocol) 发封装成 Broadcast 的协议

```
Broc:link连接数 RPC类名:RPC的sessionId 请求Bean内容         // 发送RPC请求
Broc:link连接数 RPC类名:RPC的sessionId>resultCode 回复Bean内容 // 发送RPC回复
Broc:link连接数 协议类名 协议Bean内容                        // 发送协议
Broc:link连接数 协议类名>resultCode 协议Bean内容              // 发送带resultCode的协议
```

### Provider 通过 Zeze.Arch.Online.send 发封装成 Send 的协议

```
Send:account,clientId RPC类名:RPC的sessionId 请求Bean内容         // 发送RPC请求
Send:account,clientId RPC类名:RPC的sessionId>resultCode 回复Bean内容 // 发送RPC回复
Send:account,clientId 协议类名 协议Bean内容                        // 发送协议
Send:account,clientId 协议类名>resultCode 协议Bean内容              // 发送带resultCode的协议
```

多组 `account,clientId` 会以 `account,clientId;account,clientId;...` 方式输出。

### Provider 通过 Zeze.Arch.Online.sendAccount/sendAccounts 发封装成 Send 的协议

```
Send:account RPC类名:RPC的sessionId 请求Bean内容         // 发送RPC请求
Send:account RPC类名:RPC的sessionId>resultCode 回复Bean内容 // 发送RPC回复
Send:account 协议类名 协议Bean内容                        // 发送协议
Send:account 协议类名>resultCode 协议Bean内容              // 发送带resultCode的协议
```

多组"account"会以 `account,account,...` 方式输出。

### Provider 通过 Zeze.Arch.Online.sendReliableNotify 发封装成 SReliableNotify 的协议

```
Send:account,clientId:listenerName RPC类名:RPC的sessionId 请求Bean内容         // 发送RPC请求
Send:account,clientId:listenerName RPC类名:RPC的sessionId>resultCode 回复Bean内容 // 发送RPC回复
Send:account,clientId:listenerName 协议类名 协议Bean内容                        // 发送协议
Send:account,clientId:listenerName 协议类名>resultCode 协议Bean内容              // 发送带resultCode的协议
```

### 协议日志开启建议

开启协议日志需要加 JVM 参数，推荐的设置方法：

```
linkd: -DprotocolLog=DEBUG
gs: -DprotocolLog=DEBUG -DprotocolLogExcept=47280285301785,47281226998238
```

上面两组数字是需要排除的两个协议 ID 分别是 Dispatch 和 Send，因为只需要输出里面包装的协议日志就够了。

### Linkd 协议日志举例

```
AsyncSocket: RECV:115004 1:291877964:2 1[1]                        // 收到客户端发来的协议，网络sessionId是115004，moduleId=1，protocolId=291877964，RPC的sessionId=2
AsyncSocket: SEND:115003 Dispatch Zeze.Builtin.Provider.BDispatch: {...  // 包装这个协议成Dispatch发给gs（网络sessionId=115003）
AsyncSocket: RECV:115003 Send:135 Zeze.Builtin.Provider.BSend: {...      // 收到gs的Send协议（RPC的sessionId=135）
AsyncSocket: Send:115004 1:291877964:2 0[179]                        // 发给客户端Send里包装的协议，协议类型和sessionId跟客户端发来的一样
AsyncSocket: SEND:115003 Send:135>0 Zeze.Builtin.Provider.BSendResult: {...  // 给gs回复Send协议，resultCode=0
```

### Provider 协议日志举例

```
AsyncSocket: Recv:257 GetMapLiveList:26 ()
AsyncSocket: Send:257 GetMapLiveList:26>0 wm.Map.BLiveList: {...
```

上面的 257 是角色 ID，26 是 RPC 的 sessionId，`>0` 表示返回的 resultCode=0。

## 配置示例

```xml
<ServiceConf Name="MyService">
    <Acceptor Ip="127.0.0.1" Port="5555"/>
    <Connector HostNameOrAddress="127.0.0.1" Port="6666" IsAutoReconnect="true"
               MaxReconnectDelay="8"/>
</ServiceConf>
```

同一个 Service 中 Acceptor 和 Connector 可以共存。不管是来自 Connector 的连接还是来自 Acceptor 的连接，都享受一样的服务。
