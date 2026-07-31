---
title: "LoginQueue 登录队列"
description: "Zeze.Services.LoginQueue 高并发登录排队——控制登录速率防止服务器过载"
category: reference
order: 53
---

> 本文档描述 `Zeze.Services.LoginQueue` 登录排队机制：队列管理、按负载分配登录令牌、Token 加密下发，供高并发开服排队场景参考。

## 模块定位

`Zeze.Services.LoginQueue` 用于**高并发时控制登录速率**，防止服务器过载。它在客户端与游戏服务器之间设置一道排队闸门，按顺序处理登录请求。

| 类 | 包路径 | 角色 |
|----|--------|------|
| `LoginQueue` | `Zeze.Services` | 队列主逻辑，接受客户端连接、维护队列、定时分配 |
| `LoginQueueServer` | `Zeze.Services` | 接收 Provider / Linkd 的负载上报，按负载选择服务器；**内嵌于 `LoginQueue`**（非独立进程） |
| `LoginQueueClient` | `Zeze.Services` | 客户端 SDK，接收队列位置/登录令牌通知 |
| `LoginQueueAgent` | `Zeze.Services` | Provider/Linkd 端，向 LoginQueueServer 上报负载 |

> ⚠️ **注意**：`LoginQueueServer` **不是独立进程**，而是**内嵌于 `LoginQueue`**（在 `LoginQueue` 内部实例化）。整个登录队列服务随 `LoginQueue` 进程启动。

---

## 工作原理

### 整体流程

```
Client ──连接──▶ LoginQueue ──排队──▶ 分配 LoginToken ──▶ Client 拿 Token 连 Linkd
                      ▲
                      │ 负载上报
           ┌──────────┴──────────┐
      Provider                Linkd
   (LoginQueueAgent)      (LoginQueueAgent)
```

| 步骤 | 说明 |
|------|------|
| 1. 客户端连接 | 客户端连接 `LoginQueueService`，触发 `tryOnAccept` |
| 2. 入队或直通 | 队列空且限流通过时，直接分配；否则入队等待 |
| 3. 定时分配 | `allocateTimer`（每秒）按服务器负载分配登录令牌 |
| 4. 广播队列位置 | 每若干次定时触发，给排队中的客户端广播队列位置 |
| 5. 下发 Token | 分配成功后，加密的 `LoginToken` 下发给客户端 |
| 6. 客户端登录 | 客户端拿 Token 连接 Linkd 正式登录 |

### 入队判定（tryOnAccept）

| 条件 | 行为 |
|------|------|
| 队列已满（≥ `MaxConnections`） | 发送 `PutQueueFull`，关闭连接 |
| 队列空 + 限流通过 | 直接尝试分配，成功则跳过排队 |
| 其他 | 入队等待 |

> 限流通过 `TimeThrottle` 控制：`maxOnlineNew`（单服务器每秒最大新增在线）乘以 Provider 数量。

### 分配逻辑（allocateTimer）

每秒触发一次，按负载选择服务器：

| 步骤 | 说明 |
|------|------|
| 计算配额 | `providerSize * maxOnlineNew`，随机取一半以上 |
| 遍历队列 | 按配额逐个分配，失败则停止 |
| 广播位置 | 每 3 次触发广播一次队列位置（最多前 10000 个） |

---

## 按负载选择服务器

`LoginQueueServer.choiceServer` 根据负载加权随机选择 Provider / Linkd：

| 过滤条件 | 说明 |
|----------|------|
| 跳过过载 | `load.Overload == eOverload` 的不选 |
| 跳过超限 | `OnlineNew > MaxOnlineNew` 的不选 |
| 计算权重 | `weight = ProposeMaxOnline - Online`，权重 ≤ 0 不选 |

| 选择方式 | 说明 |
|----------|------|
| 加权随机 | 按剩余容量权重随机选一台 |
| 失败返回 `null` | 无可用服务器 |

> Provider 和 Linkd 分别选择：`choiceProvider()` / `choiceLink()`。

---

## LoginToken 加密下发

分配成功后，服务器信息编码加密下发给客户端，客户端再转给 Linkd 使用。

| 字段 | 说明 |
|------|------|
| `ServerId` | 目标 Provider 的 ServerId（`-1` 表示仅选 Link） |
| `LinkServerId` | 目标 Linkd 的 ServerId |
| `SerialId` | 序列号（单调递增） |
| `ExpireTime` | 过期时间（默认 30 分钟） |

### 加密方式

| 项 | 说明 |
|----|------|
| 算法 | `AES/CBC/PKCS5Padding` |
| 密钥 | `LoginQueueServer` 启动时随机生成 16 字节 Key + 16 字节 IV |
| 下发 | Provider/Linkd 连接时通过 `AnnounceSecret` 下发密钥 |
| 编解码 | `encodeToken(secret, token)` / `decodeToken(secret, binary)` |

> Token 过期时间默认 30 分钟（`eLoginTokenExpireTime`），因为排队完成后客户端还要走登录流程，不能太短。

---

## 客户端协议（LoginQueueClient）

客户端通过 `LoginQueueClient` 接收队列通知：

| 协议 | 回调 | 说明 |
|------|------|------|
| `PutQueuePosition` | `queuePosition` | 队列位置（第 N 个） |
| `PutLoginToken` | `loginToken` | 登录令牌（含 Linkd 地址） |
| `PutQueueFull` | `queueFull` | 队列已满，稍后重试 |

```java
LoginQueueClient client = new LoginQueueClient();
client.connect(hostNameOrAddress, port);

client.setQueuePosition(pos -> {
    // 显示「你前面还有 N 人」
});
client.setLoginToken(token -> {
    // 拿到 Linkd 地址和 Token，正式登录
});
client.setQueueFull(() -> {
    // 队列满，提示稍后再试
});
```

---

## 负载上报集成

Provider / Linkd 通过 `LoginQueueAgent` 向 `LoginQueueServer` 上报负载：

```java
// 1. 创建 Agent（连接 LoginQueueServer）
var agent = new LoginQueueAgent(config, serverId, serviceIp, servicePort);
agent.start();

// 2. 挂到 LoadBase
loadBase.setLoginQueueAgent(agent);

// 3. LoadBase 定时上报时会调用 reportProviderLoad / reportLinkLoad
```

| 上报接口 | 适用 | 说明 |
|----------|------|------|
| `reportProviderLoad(load)` | Provider | 上报 Provider 负载（在线数、新增、过载状态） |
| `reportLinkLoad(load)` | Linkd | 上报 Linkd 负载 |

> Provider 和 Linkd 只能二选一上报。`LoadBase` 在启用 `LoginQueueAgent` 后，定时上报时会自动调用对应接口。

---

## 配置参数

> 以下为 `LoginQueue` 构造参数与配置项，**具体默认值以 SDK 实现为准**。

| 参数 | 说明 |
|------|------|
| `maxOnlineNew` | 每台服务器每秒最大新增在线数（构造参数，默认 `100`） |
| `choiceLinkOnly` | 是否仅选择 Linkd（不选 Provider，默认 `false`） |
| `loginQueue.xml` | 网络服务配置（`Config.load`） |
| `MaxConnections` | 最大连接数（队列上限） |

### 启动方式

```java
// 独立进程启动
var lq = new LoginQueue(maxOnlineNew, choiceLinkOnly);
lq.start();

// 或通过 main 入口
// java ... LoginQueue -maxOnlineNew 100 -choiceLinkOnly false
```

---

## 适用场景

| 场景 | 是否适用 | 说明 |
|------|----------|------|
| **开服/活动高峰** | ✅ 适用 | 瞬时大量登录，排队防过载 |
| 公测开服 | ✅ 适用 | 典型排队场景 |
| 跨服活动开始 | ✅ 适用 | 控制瞬时涌入 |
| 日常平稳期 | ⚠️ 可选 | 可不启用，依赖 Provider 负载报告即可 |

### 与 Provider 负载报告的关系

| 机制 | 作用 |
|------|------|
| Provider 负载报告 | Linkd 据此分配用户，过载不再分发 |
| LoginQueue | 在登录入口处排队，更前置、更可控 |

> 启用 `LoginQueue` 后，原 Provider 负载报告可保留（兼容），两者协同。

---

## 注意事项

| 注意点 | 说明 |
|--------|------|
| **部署形态** | `LoginQueueServer` 内嵌于 `LoginQueue`，整个登录队列随 `LoginQueue` 进程部署，避免与业务服务器相互影响 |
| **Token 过期** | 默认 30 分钟，排队后仍需走登录流程，不宜过短 |
| **密钥安全** | AES 密钥在连接时下发，注意网络环境安全 |
| **广播限量** | 队列位置广播最多前 10000 个，超出的显示「>10000」 |
| **负载上报二选一** | Provider 与 Linkd 只能选一种上报 |

> 本文档描述排队机制与原理。具体方法签名、协议字段以 SDK 实现为准。

---

## 相关文档

- [Online 在线管理](./arch-online.md) — 登录后的在线状态、生命周期
- [Provider-Linkd 架构](./arch-provider-linkd.md) — 前后端拆分与负载分发
- [游戏模块总览](./game-overview.md) — ProviderLoadWithOnline 负载报告
