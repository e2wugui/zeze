---
title: "Zeze WebSocket / WSS 支持"
sidebar:
  order: 5
---


Zeze 通过 `HttpServer`（基于 Netty）提供 WebSocket 和 WSS（WebSocket Secure）服务端支持，同时通过 `WebsocketClient` 提供 WebSocket 客户端连接能力。WebSocket 连接在建立后会被封装为 `AsyncSocket`，与普通 TCP 连接使用相同的协议派发机制，无需额外学习成本。

## 核心类

| 类 | 包 | 说明 |
|---|---|---|
| `HttpServer` | `Zeze.Netty` | 基于 Netty 的 HTTP/WebSocket 服务端，支持 SSL/TLS |
| `HttpWebSocketHandle` | `Zeze.Netty` | WebSocket 事件处理接口（onOpen/onClose/onContent/onBinary/onText） |
| `Websocket` | `Zeze.Net` | 服务端 WebSocket 连接，继承 `AsyncSocket`，封装 `HttpExchange` |
| `WebsocketHandle` | `Zeze.Net` | 服务端 WebSocket 管理器，桥接 `HttpWebSocketHandle` 和 `Service` |
| `WebsocketClient` | `Zeze.Net` | WebSocket 客户端连接，继承 `AsyncSocket`，使用 JDK HttpClient |
| `Connector` | `Zeze.Net` | 连接器，支持 WebSocket URL，自动重连 |
| `Cert` | `Zeze.Util` | 证书工具类，加载 KeyStore、私钥、证书等 |

## 架构概览

```
客户端 (ws/wss)
    │
    ▼
HttpServer (Netty, 可选SSL)
    │  收到 WebSocket 升级请求
    ▼
WebsocketHandle (路径匹配)
    │  onOpen → 创建 Websocket(AsyncSocket)
    ▼
Service (协议派发)
    │  与普通 TCP 连接完全一致的协议处理
    ▼
业务模块 (Module)
```

## 一、服务端使用

### 1.1 XML 配置方式

在 `ServiceConf` 中添加 `Websocket` 节点：

```xml
<zeze>
    <!-- LinkdService 同时监听 TCP 和 WebSocket -->
    <ServiceConf Name="LinkdService" InputBufferMaxProtocolSize="2M">
        <Acceptor Port="10000"/>
        <Websocket Path="/websocket"/>
    </ServiceConf>
</zeze>
```

`Websocket` 节点属性：
- **Path**（必填）：WebSocket 的 HTTP 路径，客户端需通过此路径发起升级请求。

启动时会自动注册到 `HttpServer`，通过 `ServiceConf.start()` → `WebsocketHandle.start()` 完成。

### 1.2 代码配置方式

```java
// 创建 WebsocketHandle 时必须指定 HttpServer
var wsHandle = new WebsocketHandle("/ws", httpServer);
serviceConfig.addWebsocket(wsHandle);
```

> **注意**：如果不指定 `HttpServer`（如通过 XML 配置），`WebsocketHandle.start()` 会自动通过 `service.getZeze().getAppBase().getHttpServer()` 获取。因此应用需要重载 `AppBase.getHttpServer()` 返回实际实例。

### 1.3 WebSocket 生命周期

`WebsocketHandle` 自动管理 WebSocket 连接的整个生命周期：

1. **onOpen**：客户端发起 WebSocket 握手成功后，创建 `Websocket` 对象（继承 `AsyncSocket`），注册到 `Service`。
2. **onContent**：收到 WebSocket 帧数据，自动拼接后交给 `Service.OnSocketProcessInputBuffer` 处理（与 TCP 一致的协议解码）。
3. **onClose**：客户端关闭连接时，自动清理并触发 `Service.OnSocketClose`。

WebSocket 连接建立后，上层协议处理与 TCP 完全一致——所有已注册的 `Protocol` 都能正常工作。

## 二、客户端使用

### 2.1 通过 Connector 自动连接（推荐）

```java
// 使用 wss:// 前缀即启用 TLS
var url = "wss://example.com:10000/websocket";
var connector = new Connector(true, url);  // true = 自动重连
clientService.getConfig().addConnector(connector);
connector.start();
connector.WaitReady();  // 阻塞等待连接就绪
```

### 2.2 通过 Service 直接创建

```java
// Service.newWebsocketClient 方法
AsyncSocket socket = service.newWebsocketClient("ws://127.0.0.1:10000/websocket", null, connector);
```

### 2.3 实际示例（来自 Zezex ClientGame）

```java
loginQueueClient.setLoginToken((loginToken) -> {
    // Linkd 的 WebSocket 端口 = TCP 端口 + 10000
    var url = "ws://" + loginToken.getLinkIp()
            + ":" + (loginToken.getLinkPort() + 10000) + "/websocket";
    Connector = new Connector(true, url);
    ClientService.getConfig().addConnector(Connector);
    Connector.start();
    Connector.WaitReady();
});
```

## 三、启用 WSS（WebSocket Secure）

WSS 通过在 `HttpServer` 上启用 SSL/TLS 实现。启用 SSL 后，该 `HttpServer` 上的所有连接（包括 HTTP 和 WebSocket）都将使用加密传输。

### 3.1 准备证书

使用 `keytool` 生成 PKCS12 格式的密钥库：

```bash
keytool -genkeypair -keyalg RSA -keysize 2048 \
    -keystore server.ks -storetype pkcs12 -storepass 123456 \
    -alias server -validity 365 \
    -dname "cn=myserver.com, ou=Dev, o=MyOrg, c=CN"
```

### 3.2 在 HttpServer 上启用 SSL

```java
import Zeze.Util.Cert;

// 加载 KeyStore
var ks = Cert.loadKeyStore(
    Files.newInputStream(Path.of("server.ks")), "123456");

// 获取私钥和证书链
var priKey = Cert.getPrivateKey(ks, "123456", "server");
var cert = Cert.getCertificate(ks, "server");
var certChain = new X509Certificate[]{ (X509Certificate)cert };

// 在 HttpServer 上启用 SSL
httpServer.setSsl(priKey, null, certChain);
```

`HttpServer.setSsl` 内部使用 Netty 的 `SslContextBuilder` 创建服务端 SSL 上下文。调用 `setSsl` 后，`HttpServer.initChannel` 会自动在 pipeline 中添加 SSL handler：

```java
// HttpServer.initChannel 内部实现：
if (sslCtx != null)
    p.addLast(sslCtx.newHandler(ch.alloc()));
```

### 3.3 客户端使用 WSS

客户端只需将 URL 前缀改为 `wss://` 即可，`WebsocketClient` 使用 JDK 标准的 `HttpClient`，自动处理 TLS 握手：

```java
var url = "wss://myserver.com:10000/websocket";
var connector = new Connector(true, url);
service.getConfig().addConnector(connector);
connector.start();
```

## 四、自定义 WebSocket 处理

如需直接处理 WebSocket 的文本/二进制帧（不使用 Zeze 协议派发），可直接实现 `HttpWebSocketHandle` 接口并注册到 `HttpServer`：

```java
// 注册自定义 WebSocket 处理器到 HttpServer
httpServer.addHandler("/my-ws-path", TransactionLevel.None, DispatchMode.Direct,
    new HttpWebSocketHandle() {
        @Override
        public void onOpen(HttpExchange x) {
            // WebSocket 连接建立
        }

        @Override
        public void onClose(HttpExchange x, int status, String reason) {
            // WebSocket 连接关闭
        }

        @Override
        public void onBinary(HttpExchange x, ByteBuf content) {
            // 收到完整二进制帧
            // 注意：content 遵循 Netty 引用管理，带出方法外需要 retain
        }

        @Override
        public void onText(HttpExchange x, String text) {
            // 收到完整文本帧
        }
    });
```

`HttpWebSocketHandle` 接口方法：
- `onOpen`：连接建立
- `onClose`：连接关闭（status 为 `WebSocketCloseStatus` 编码，`ABNORMAL_CLOSURE` 表示强制关闭）
- `onContent`：收到帧片段（自动拼接）
- `onBinary`：收到完整二进制帧
- `onText`：收到完整文本帧
- `onPing / onPong`：心跳（默认 ping 自动回复 pong）

发送数据（通过 `HttpExchange`）：
```java
x.sendWebSocket("hello");                    // 发送文本帧
x.sendWebSocket(binaryData);                 // 发送二进制帧
x.sendWebSocket(binaryData, offset, len);    // 发送二进制帧（指定范围）
```

## 五、关键设计要点

1. **协议透明**：`Websocket` 和 `WebsocketClient` 都继承自 `AsyncSocket`，上层业务代码不感知底层是 TCP 还是 WebSocket。
2. **复用 Service**：WebSocket 连接与 TCP 连接共用同一个 `Service`，共用协议工厂和派发机制。
3. **自动重连**：`Connector` 支持 WebSocket URL，提供指数退避的自动重连（最大延迟 `maxReconnectDelay` 默认 8 秒）。
4. **SSL 可选**：SSL 在 `HttpServer` 级别配置，启用后同时保护 HTTP 和 WebSocket。
5. **帧拼接**：`HttpWebSocketHandle.onContent` 自动处理 WebSocket 帧的分片拼接，业务只需实现 `onBinary`/`onText` 即可处理完整帧。
