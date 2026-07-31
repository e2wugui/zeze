---
title: "Redirect 跨服调用"
description: "Zeze 通过一致性哈希将数据访问路由到目标服务器执行的跨服调用机制"
category: reference
order: 12
---

> 本文档描述 Zeze Redirect 跨服调用机制的三种注解模式、Future 用法、错误码及服务就绪注意事项，供跨服业务开发检索参考。

## 工作原理

不同用户登录不同 Server，A 访问 B 的数据时直接读会导致 Cache 失效。Redirect 把请求转发到目标 Server 执行，保证修改与查询在同一台机器，Cache 命中率高。

## 三种注解模式

| 注解 | 说明 | 典型场景 |
|------|------|----------|
| `@RedirectHash` | 按 hash 选 Server | 排行榜、帮派 |
| `@RedirectAll` | 广播所有分组，收集结果 | 排行榜全局查询 |
| `@RedirectToServer` | 直接指定 ServerId | 定时器、取消、Rpc |

## @RedirectHash

首个参数为 `int hash`（或用 `@RedirectKey` 标记），框架据此选 Server。

### 注解参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `oneByOne` | `true` | 同 hash 排队串行，避免并发冲突 |
| `ConcurrentLevelSource` | `""` | 字符串方法调用表达式，返回分组总数（如 `getConcurrentLevel(keyHint.getRankType())`，以 `keyHint` 为输入决定取模分桶的分组粒度） |
| `timeout` | `30000` | 超时（毫秒） |
| `version` | - | 版本 |

### 参数约定

- 首个 `int hash` 可用 `@RedirectKey` 标记。
- 其他参数的 hash 可能路由到本机（loop-back）。

### 返回值

`RedirectFuture<T>`（`T` 为自定义 Bean / Long / resultCode / String / Binary）或 `void`。

```java
@RedirectHash
public RedirectFuture<BQueryResult> queryRank(int hash, long roleId) {
    // 框架据此选 Server 执行
}
```

## @RedirectAll

MapReduce 风格：每个 Server 处理自己分组，发起方汇总。

### 注解参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `timeout` | `30000` | 超时（毫秒） |
| `version` | `0` | 版本 |

### 参数约定

- 首个 `int hash`：发起方表示分组总数；处理方表示当前分组编号。

### 返回值

`RedirectAllFuture<R>`（`R` 继承 `RedirectResult`）或 `void`。

### RedirectAllFuture 用法

| 方式 | 接口 | 说明 |
|------|------|------|
| 同步 | `RedirectAllFuture.result` | 直接获取结果 |
| 异步 | `RedirectAllFuture.async` + `asyncResult` | 异步获取 |
| 链式 | `onResult(...)` | 每分组返回触发（不并发，安全访问共享） |
| 链式 | `onAllDone(ctx -> getAllResults IntHashMap)` | 全部完成 |
| 等待 | `await()` | 阻塞等待 |

```java
@RedirectAll
public RedirectAllFuture<BRankResult> queryGlobalRank(int hash) {
    // 发起方：hash=分组总数；处理方：hash=当前分组编号
}
```

## @RedirectToServer

直接指定 `serverId`，便利 RPC。

### 注解参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `oneByOne` | `true` | 同 ServerId 排队串行 |
| `orOtherServer` | `false` | 目标不可达时自动选其他（`false` 则不可达抛 `RedirectException`） |
| `timeout` | `30000` | 超时（毫秒） |
| `version` | `0` | 版本 |

### 参数约定

- 首个 `int serverId` 等于本机则 loop-back，支持 `@RedirectKey`。
- 目标不存在抛 `RedirectException(code SERVER_NOT_FOUND)`，除非 `orOtherServer`。

## RedirectFuture 用法

| 接口 | 说明 |
|------|------|
| `RedirectFuture.finish(result)` | 创建并完成 |
| `new RedirectFuture` + `setResult(...)` | 异步创建 |
| `onSuccess(...)` | 成功回调 |
| `onFail(RedirectException)` | 失败回调 |
| `then(...)` | 链式回调 |
| `await()` | 阻塞等待 |

## RedirectException 错误码

| 错误码 | 值 | 含义 |
|--------|----|------|
| `GENERIC` | 0 | 未知错误 |
| `SERVER_NOT_FOUND` | 1 | 未连接、未注册 |
| `SERVER_TIMEOUT` | 2 | 远程超时，不确定是否执行 |
| `LOCAL_EXECUTION` | 3 | 本地异常（`getCause`） |
| `REMOTE_EXECUTION` | 4 | 远程严重错误 |

## Redirect 服务就绪问题

| 注意点 | 说明 |
|--------|------|
| 模块注册时机 | 发现依赖模块注册在 `startLast` 后才注册，推荐实现在 `startLast` 前准备好 |
| 不可靠服务 | 推荐按「不可靠服务」使用 |
| 自定义注册中心 | `RedirectToServer` 可自定义注册中心 |
| 自行判断拒绝 | `startLast` 后仍有初始化的，需收到请求自行判断拒绝 |

## 相关文档

- Provider-Linkd 架构：[./arch-provider-linkd.md](./arch-provider-linkd.md)
- 在线管理：[./arch-online.md](./arch-online.md)
- 分布式入门：[../manual/05-going-distributed.md](../manual/05-going-distributed.md)
