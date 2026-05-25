---
title: 登录队列
sidebar:
  order: 3
---

当大量玩家同时登录时，直接涌入会导致服务器过载。Zeze 提供了 **LoginQueue** 登录队列服务，通过排队机制控制登录速率，保障服务器稳定运行。

## 架构概览

```
客户端 ──> LoginQueue ──排队分配──> 客户端拿到 Token ──> Linkd ──> Provider
               │
               v
         LoginQueueServer <──上报负载── LoginQueueAgent (部署在 Linkd/Provider 上)
```

- **LoginQueue**：独立进程，接受客户端连接，根据负载排队分配登录许可
- **LoginQueueServer**：内嵌在 LoginQueue 中，收集 Linkd 和 Provider 的负载信息
- **LoginQueueAgent**：部署在每个 Linkd 或 Provider 中，向 LoginQueueServer 上报负载

## LoginQueue 配置

```java
var loginQueue = new LoginQueue(100, false);  // maxOnlineNew, choiceLinkOnly
loginQueue.start();
```

`loginQueue.xml` 配置文件：

```xml
<?xml version="1.0" encoding="utf-8"?>
<zeze>
    <ServiceConf Name="LoginQueue">
        <Acceptor Ip="127.0.0.1" Port="5020"/>
    </ServiceConf>
    <ServiceConf Name="LoginQueueServer">
        <Acceptor Ip="127.0.0.1" Port="5021"/>
    </ServiceConf>
</zeze>
```

## 排队策略

LoginQueue 使用 **TimeThrottle** 进行速率控制：

1. 客户端连接时，如果队列为空且配额允许，直接分配服务器
2. 否则加入等待队列，每秒触发一次分配
3. 分配时根据 Provider 数量计算配额，随机分配一半以上
4. 定时向排队中的客户端广播队列位置（最多 10000 个）
5. 分配成功后通过 `PutLoginToken` 发送加密 **Token**

Token 使用 AES-CBC-PKCS5 加密，包含 `serverId`、`linkServerId`、`expireTime`（默认 30 分钟）和 `serialId`。

## 负载选择算法

LoginQueueServer 使用加权随机策略选择服务器：

- 跳过过载（`eOverload`）的服务器
- 权重 = `proposeMaxOnline - online`，权重越大被选中概率越高
- 没有可用服务器时返回 null，客户端继续排队

## LoginQueueAgent 配置

在 linkd.xml 和 gs.xml 中增加：

```xml
<ServiceConf Name="LoginQueueAgent">
    <Connector HostNameOrAddress="127.0.0.1" Port="5021"/>
</ServiceConf>
```

```java
var agent = new LoginQueueAgent(config, serverId, "192.168.1.10", 8080);
agent.start();
agent.reportLinkLoad(load);      // 上报 Linkd 负载
agent.reportProviderLoad(load);  // 或上报 Provider 负载（二选一）
```

## LoginQueueClient 客户端

```java
var client = new LoginQueueClient();
client.setQueuePosition(pos -> System.out.println("排队位置: " + pos.getQueuePosition()));
client.setLoginToken(token -> { /* 使用 token 连接 Linkd */ });
client.setQueueFull(() -> System.out.println("队列已满"));
client.connect("login.example.com", 5020);
```

| 协议 | 说明 |
|------|------|
| `PutQueuePosition` | 排队位置更新 |
| `PutLoginToken` | 登录许可，包含 Linkd 地址和加密 Token |
| `PutQueueFull` | 队列已满，连接关闭 |

### Lua 客户端

```
ConnectLoginQueue(ip, 5020)
-- 实现 OnQueueFull、OnQueuePosition、OnLoginToken 三个回调
-- OnLoginToken 中保存 token，连接 Linkd 后在 Auth 中带上 loginQueueToken
```

## Linkd 侧处理

Linkd 收到 Auth 协议时调用：

```java
App.LinkdProvider.choiceProvider(rpc.getSender(), rpc.Argument.getLoginQueueToken());
```

## 启动

```bash
java -cp zezex.jar Zeze.Services.LoginQueue -maxOnlineNew 100 -choiceLinkOnly false
```

## 相关文档

- [架构设计](../architecture/arch/)：Provider-Linkd 架构
- [运维配置](../devops/configuration/)：XML 配置说明
