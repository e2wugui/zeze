---
title: "全球同服"
description: "Zeze 通过分组拆分单点瓶颈并提升并发度的架构方案"
category: reference
order: 17
---

> 本文档描述 Zeze「全球同服」架构中单点模块瓶颈、分组拆分策略、负载分配及共享模块优化方案，供大型并发场景架构设计检索参考。

## 问题：单点模块瓶颈

全局单点模块无法提供足够并发。例如即时排行榜：角色数值变化马上更新，所有更新请求排队互斥，在线角色多、更新多，容易成为并发瓶颈。

## 分组解法

全局单点数据可按规则分开存储，需要时汇总。

| 策略 | 说明 |
|------|------|
| 分组存储 | 数据按 hash 分散到多组，每组独立排名并保存足够数量 |
| 全局汇总 | 需要全局排名时，整合所有分组 |
| 并发提升 | 并发量随分组数线性增长（如分 128 组，并发增 128 倍） |

### ConcurrentLevelSource（分组数量来源）

`ConcurrentLevelSource` **不是独立的 API**，而是 `@RedirectHash` 注解的一个**字符串字段**，用于在运行时动态指定分组数量。它指向一个方法调用（字符串形式），框架据此求值：

```java
// 注解定义（字段，非独立接口）
// public @interface RedirectHash {
//     String ConcurrentLevelSource() default "";
//     ...
// }

// 实际用法（来自 Rank 示例）
@RedirectHash(ConcurrentLevelSource = "getConcurrentLevel(keyHint.getRankType())")
public RedirectFuture<Long> updateRank(int hash, BConcurrentKey keyHint, long roleId, Bean value) {
    // hash 决定路由到哪台服务器的哪个分组
}
```

| 要点 | 说明 |
|------|------|
| 决定最大并发度 | 由 `ConcurrentLevelSource` 求值结果决定 |
| 留有余地 | 一般设足够大（如 128） |
| 不可随意改 | 排行榜改分组参数会导致分组数据全部失效 |

## 负载分配

| 原则 | 说明 |
|------|------|
| 同分组同服务器 | 访问同一分组的请求转发到同一台服务器，提高 Cache 命中 |
| 分组数固定 | 服务器数开始一般小于分组数 |
| 按 hash 分配 | `@RedirectHash` 按 hash 分配负载，每台可能处理多分组 |
| 分组数决定最大服务器数 | 分组数即为最大可扩展服务器数 |

## 大量共享模块优化

帮派 / 群拥有一定量成员，登录多台 Server，成员访问成员列表时缓存到本 Server，修改时作废所有 Server 缓存。

| 方案 | 性能特征 |
|------|----------|
| 缓存作废 | 性能突发，但成熟帮派每天修改次数少，总体不严重 |
| 定向单 Server（精益求精） | 把群所有操作发同一台 Server 处理，避免共享问题 |

## 实现：群操作定向

群操作定向到同一台 Server，由 `Arch.Linkd` 完成。Linkd 拦截群操作，按群编号 hash 定向。

### 工作流程

```
群操作请求到达 Linkd
        │
        ▼
LinkdService.dispatchUnknownProtocol 拦截
        │  switch (moduleId) { case 群操作: ... }
        ▼
ChoiceHashSend(DecodeGroupIdHash(data), moduleId, dispatch)
        │
        ▼
linkdApp.linkdProvider.choiceHashWithoutBind(moduleId, clientVersion, hash, provider)
        │
        ▼
GetSocket → Send（定向到选定的 Provider）
```

### DecodeGroupIdHash 规则

| 规则 | 说明 |
|------|------|
| 解析公共参数 | 解析协议中的 `GroupId` |
| 参数约定 | 所有群操作参数第一变量必须是 `Group`，且 `variable.id = 1` |
| 优化 | Linkd 只偷这部分，不解析完整协议 |
| `GroupId` | 只支持 `decode` + `hashCode`，不支持 `encode` |

### ChoiceHashSend 核心

```java
// 1. 偷解析 GroupId 的 hash
int hash = DecodeGroupIdHash(data);

// 2. 按 hash 选 Provider（不绑定），结果通过 OutLong 带出（4 参数）
OutLong provider = new OutLong();
linkdApp.linkdProvider.choiceHashWithoutBind(moduleId, clientVersion, hash, provider);

// 3. 发送到选定的 Provider
provider.value.GetSocket().Send(dispatch);
```

## 相关文档

- Redirect 跨服调用：[./arch-redirect.md](./arch-redirect.md)
- Provider-Linkd 架构：[./arch-provider-linkd.md](./arch-provider-linkd.md)
- 分布式入门：[../manual/05-going-distributed.md](../manual/05-going-distributed.md)
