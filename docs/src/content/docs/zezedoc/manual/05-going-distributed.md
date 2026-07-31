---
title: "走向分布式"
description: "理解 Provider-Linkd 架构图景，把单机拼成集群，承载全球同服"
category: manual
order: 5
---

读完这篇，你将能说清 Zeze 是怎样把一台单机逐步拼成一个可水平扩展、全球同服的集群的——谁负责什么、怎么找到彼此、数据怎么保持一致、什么时候该启动什么。

## 从一台机器说起

一个游戏服务端能在一台机器上跑起来，是因为所有的东西都在一个进程里：客户端连进来、业务逻辑处理、数据读写。这套东西很直观，但有一个绕不开的天花板——**一台机器的算力、内存、连接数都是有限的**。当玩家越来越多，单机迟早扛不住。

Zeze 的答案是 **Provider-Linkd 架构**：把"管理客户端连接"和"跑业务逻辑"这两件事拆开，各自独立扩展，再用一个服务注册中心把它们粘合起来。理解了这套骨架，你才算真正读懂了 Zeze 的分布式。

## 四个角色，各司其职

先认识参与这场协奏的四个角色：

| 角色 | 职责 | 是否用数据库 |
|---|---|---|
| **Linkd** | 前端网关。管理客户端连接、按 Module 分发请求、维护绑定关系、负责广播和踢人 | 一般不用，专注连接管理与协议转发 |
| **Provider**（Server / GameServer） | 后端业务承载者。通过事务操作数据库、处理转发的客户端请求、向 Linkd 报告自身负载 | 用 |
| **ServiceManager** | 服务注册中心。通告服务上下线变更、承载负载上报 | — |
| **ProviderDirect** | Provider 之间的直连通道，承载 Redirect 跨服调用 | — |

它们之间的交互全景大致是这样：

```
                         [Provider 2]
                             ^  |
              Provider协议   |  | ProviderDirect
                             |  v
[Client] --TCP--> [Linkd] --Provider协议--> [Provider 1]
                    |  ^                       |  ^
                    v  |                       v  |
             [ServiceManager]           [ProviderDirect]
              (服务注册/发现)            (Server间直连)
```

一句话概括数据流：**客户端的请求先到 Linkd，Linkd 根据模块配置把它转发到合适的 Provider；Provider 之间需要协作时走 ProviderDirect 直连。** Linkd 本身尽量"轻"，不碰业务数据，只做连接和路由。

## Module：最小的逻辑划分单位

要理解 Linkd 怎么分发请求，先得理解 **Module（模块）**。Module 类似微服务架构里的"服务"，是 Zeze 中最小的逻辑划分单位，有几个关键规则：

- 每个 Module 有一个全局唯一的 `moduleId`。
- 同一个 Module 的协议，在 Linkd 侧会被路由到同一组 Provider 处理。
- **一个 Provider 进程可以实现多个 Module**（把相关的业务逻辑放在一起）。
- **同一个 Module 也可以部署到多个 Provider 上**（水平扩展，分担压力）。
- 但 **一个 Module 只能在一个代码工程里实现**——不能两套代码各实现一半。

这样设计的好处是：你可以按业务边界拆分 Module（比如背包是一个 Module、战斗是另一个 Module），再把不同 Module 部署到不同的机器上，灵活组合。

## 绑定：让会话"粘"在正确的 Provider 上

当一个客户端连进来后，它的某些请求需要被固定发往同一台 Provider——比如同一个角色的数据操作必须落在同一台机器上，否则跨进程访问会拖慢性能、还可能破坏缓存一致性。这就涉及到**绑定（Bind）**。Zeze 提供两种绑定方式：

### 静态绑定

Provider 启动时通过 Bind 协议向 Linkd 注册自己实现了哪些静态模块。当 Linkd 收到某个模块的**首个请求**时，会自动选择一台 Provider，并把那台 Provider 上**所有静态模块**一次性绑定到这个客户端会话上。这样做的好处是**绑定亲缘性**：一旦绑定，这个客户端后续相关的请求都会落到同一台 Provider，避免反复跨进程访问。

### 动态绑定

Provider 在运行过程中主动调用 Bind，指定要绑定的 `linkSids`（客户端会话标识）。动态绑定需要 Provider **主动 UnBind 来解绑**，否则会一直保持。它适合那些需要按需建立、按需释放的关联场景。

## ChoiceType：Linkd 怎么挑选 Provider

当一个模块有多台 Provider 可选时，Linkd 用 **ChoiceType** 决定派给谁。常见的几种策略：

| ChoiceType | 含义 |
|---|---|
| `ChoiceTypeHashAccount` | 对账号做 hash 路由，保证同一账号总落到同一台 |
| `ChoiceTypeHashRoleId` | 对角色 ID 做 hash 路由 |
| `ChoiceTypeHashSourceAddress` | 对来源地址做 hash |
| `ChoiceTypeFeedFullOneByOne` | 轮询"喂饱"一台再喂下一台 |
| `ChoiceTypeLoad` | 按负载权重分配 |
| `ChoiceTypeRequest` | 按请求计数做反权重（请求多的少派） |

选哪种，取决于你的业务希望"同一类用户尽量聚在一起"还是"尽量打散均衡"。

## 服务发现：它们是怎么找到彼此的

整个集群能跑起来，前提是各角色能互相发现。流程是这样的：

1. **Linkd 启动**，把自己注册到 ServiceManager。
2. **Provider 启动**，为它实现的每个 Module 注册一条服务记录（记录名格式是 `serviceNamePrefix + moduleId`），同时订阅 Linkd 的服务，以便发现 Linkd 的地址并主动连接。
3. **ServiceManager 广播** Linkd 上线的消息。
4. Provider 收到通知后，与 Linkd **建立 TCP 连接**。
5. 握手完成后，Provider 先发 `AnnounceProviderInfo` 通报自身信息，再发 `Bind`（静态绑定）和 `Subscribe`（动态订阅）。

整个过程是自动化的，你配置好 Module 和服务名前缀，剩下的交给框架。

## 负载均衡与过载保护

集群跑起来后，最怕两件事：**有的机器闲死、有的机器忙死**，以及**流量洪峰把单台机器压垮**。Zeze 用一套上报与探测机制来应对。

### 负载上报

`LoadBase` 会定时采集并上报负载指标，包括：在线数 `online`、每秒新增 `onlineNew`、建议最大在线数 `proposeMaxOnline`、过载状态 `overload`。上报频率是**自适应**的——变化大时报得勤，平稳时报得少，兼顾实时性和开销。

### 过载保护

当请求排队变长，`ProviderOverload` 会通过探测任务测量排队延迟，分三档处理：

- **延迟 < ProviderThreshold（默认 2000ms）**：正常派发。
- **延迟 < ProviderOverload（4000ms）**：进入阈值告警，开始丢弃可丢弃的协议。
- **延迟 >= ProviderOverload**：真正过载，只保留 `eCriticalPlus` 级别的关键协议，Linkd 停止向它派发新请求。

此外 Linkd 自身还有**输出带宽保护**，防止它把下游 Provider"灌爆"。

### 优雅停服与版本灰度

两个很实用的开关：

- `setDisableChoiceFromLinks`：控制是否接受新用户登录。停服时先打开它挡住新登录，等存量玩家自然退出，实现优雅停服。
- `maxAppVersion`：版本过滤。只有主版本号一致的客户端才允许派发新登录，配合它就能做**滚动更新**——新版逐台上线，老版不受影响。

## Redirect：跨服调用的标准姿势

玩家分散在不同 Server 上。当 A 所在的 Server 想访问属于 B 所在 Server 的数据时，如果直接跨进程去读，会导致 **cache 失效**——本地缓存的数据被别人改了自己不知道。Zeze 的解法是 **Redirect**：**不把数据搬过来，而是把请求送过去**。请求被转发到目标 Server 上执行，修改和查询都在那一台完成，cache 命中率自然就高。

Redirect 有三种注解，对应三种调用模式：

| 注解 | 模式 | 说明 |
|---|---|---|
| `@RedirectHash` | 按 hash 路由 | 根据参数 hash 选定目标 Server，默认串行执行（`oneByOne`） |
| `@RedirectAll` | 广播 + 收集 | 把请求广播到所有分组，收集每台的结果汇总返回，MapReduce 风格 |
| `@RedirectToServer` | 指定 serverId | 直接点名要送到哪台 Server |

调用后会返回 `RedirectFuture`（单点）或 `RedirectAllFuture`（广播），可以异步拿结果。

还有一个相关的机制叫 **Transmit**：它的作用是"查找目标用户当前在哪台 Server，把请求转到他所在的那台执行，结果直接发回发送者"。Transmit 关心的是"人在哪"，Redirect 关心的是"数据该在哪处理"。

## 全球同服：用分组破解单点并发瓶颈

"全球同服"听起来很美好——所有玩家在同一个世界里。但它有一个硬骨头：**全局单点模块**。以"即时排行榜"为例，所有玩家的排名更新请求都要排队、互斥地修改同一份数据。在线角色一多，这个单点就成了严重的并发瓶颈。

Zeze 的破解之道是**分组**：把全局数据按规则切成若干分组（典型是 128 个），角色按 hash 分散到不同分组，**每个分组独立排名**。需要全局排名时，再把所有分组的结果整合起来。这样一来，原来串行的一把锁，变成了 128 把各自独立的锁，并发度直接翻上百倍。分组数由 `ConcurrentLevelSource` 决定，它定义了最大并发度。

> ⚠️ 分组数一旦确定一般不要改，因为改了会导致已分布的分组数据失效，需要重新迁移。

访问时，**落在同一分组的请求会被转发到同一台 Server**——这正是 `@RedirectHash` 的工作：按 hash 把请求稳定地分配到对应 Server。

对于大量共享模块（比如帮派、群聊），还有一个优化思路：**把所有群操作定向到同一台 Server 处理**，由 Linkd 在入口处按群号 hash 拦截转发，避免群数据在多台机器间来回漂移。

## GlobalCacheManager：缓存一致性的守护者

分组解决了"同一份数据被高频写入"的问题，但跨 Server 的数据访问仍然需要一套**缓存一致性协议**来保证正确性。这就是 **GlobalCacheManager（GCM）** 的职责。

GCM 为每份数据维护三种权限状态：

| 状态 | 含义 |
|---|---|
| **Modify** | 排他写。同一时刻只有一台 Server 能持有 |
| **Share** | 共享读。多台 Server 可以同时持有，但不能修改 |
| **Invalid** | 无权限，需要时再去申请 |

权限通过两个协议流转：`Acquire` 申请权限、`Reduce` 降级权限。一个典型的 Modify 申请过程是这样的：

```
Server A 想写数据，向 GCM 申请 Modify
        |
        v
GCM 发现 Server B 正持有 Modify
        |
        v
GCM 向 B 发 Reduce(Invalid)，要求它降级
        |
        v
B 完成降级并返回
        |
        v
GCM 正式把 Modify 授予 A
```

GCM 有三种运行模式：**单机同步**（开发测试用）、**异步**、以及 **Raft 模式**（状态持久化到 Raft 集群，保证自身高可用，生产推荐）。

为了防止某台 Server 卡死后仍霸占着权限，GCM 有一个叫 **AchillesHeel** 的守护机制，定期检测服务器连接的活跃度，超时就把这台服务器踢掉、释放它持有的权限。

关于性能，有个直觉上的认知：申请 **Share 大约需要一次往返**，而申请 **Modify 至少需要两次往返**（因为要先让别人降级）。所以在设计热点数据访问时，能读就别写。

## 启动与停止：顺序很重要

分布式系统里，启动和停止的顺序错了，可能引发一堆怪问题。Zeze 推荐的顺序是：

**启动顺序：**

1. 先启动 **ServiceManager**（注册中心，大家都要靠它发现彼此）。
2. 再启动 **GlobalCacheManager**（如果用到）。
3. 然后**以任意顺序**启动 Linkd 和 Provider。

**停止顺序：**

1. **ServiceManager 保持到最后**才关闭。
2. Linkd 先关闭客户端接入端口（挡住新连接）。
3. Provider 广播通知用户下线。
4. Provider 等待处理完手头的事后关闭。
5. Linkd 关闭。
6. GCM 关闭。

记住一个原则：**先关"用户入口"，再关"业务承载"，最后关"基础设施"**，让系统能优雅地把存量处理完。

## 小结

把这一篇串起来看，Provider-Linkd 架构其实回答了几个根本问题：

- **连接和业务怎么拆**——Linkd 管连接、Provider 管业务，靠 ServiceManager 发现彼此。
- **请求怎么落到对的机器**——靠 Module 划分 + 绑定 + ChoiceType 路由。
- **机器忙不过来怎么办**——靠负载上报、过载三档保护、优雅停服与版本灰度。
- **跨服数据怎么不出错**——Redirect 把请求送到数据所在处，GCM 用权限模型保证一致性。
- **全球同服的并发瓶颈怎么破**——分组，把单点锁拆成多把独立锁。

理解了这套图景，你就能开始规划自己的集群部署了。当你的业务需要选定一个具体的存储后端时，请继续阅读下一篇 [选配数据库](./06-choosing-database.md)。需要查阅更底层的细节时，可以参考 [Provider-Linkd 架构](../reference/arch-provider-linkd.md)、[Redirect 架构](../reference/arch-redirect.md) 和 [GlobalCacheManager](../reference/arch-global-cache-manager.md)。
