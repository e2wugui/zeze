---
title: "上线清单"
description: "生产环境部署前要逐项确认的配置、容量、过载保护、停服与更新事项。"
category: manual
order: 8
---

读完这篇，你手上就有一份可以直接对照执行的生产上线 checklist，从配置到停服更新逐一覆盖。

# 上线清单

这是 Manual 部分的最后一篇，定位偏运维。我们把生产环境部署前需要确认的事项整理成一份逐项 checklist，你可以在每次上线时对照勾选。每项只列关键要点，想深入了解再去看对应的参考文档。

## ☐ 配置（zeze.xml）

配置文件是上线第一道关。同一服务的所有实例通常共享一份 `zeze.xml`，只有 `ServerId` 各不相同。

- **ServerId**：分布式部署的必需项，每个实例必须唯一编号。`Server` 用正整数（0、1、2……），`Linkd` 可以用负数。
- **CheckpointPeriod**：持久化的定时间隔，单位毫秒，默认 `60000`（1 分钟）。
- **CheckpointMode**：持久化方式。`Table`（按表批量）是分布式部署的必选项；`Immediately`（每事务立即持久化）性能很差，不建议用于生产。
- **CheckpointFlushMode**：刷盘方式。`MultiThreadMerge`（多线程合并持久化）性能最优，推荐生产使用；其余还有 `SingleThreadMerge`、`MultiThread`、`SingleThread`。
- **NoDatabase**：像 `Linkd` 这种不使用数据库的服务，设为 `true`。
- **GlobalCacheManagerHostNameOrAddress**：分布式缓存同步地址。单台填 `"ip"`，多台填 `"ip1:port1;ip2:port2"`，Raft 版填 `"GlobalCacheManagersConf"`，留空字符串表示不启用分布式缓存同步。
- **ServiceManager**：服务发现与管理的模式。`""` 为默认的单点版，`"raft"` 为 Raft 版，`"disable"` 为禁用。
- **OnlineLogoutDelay**：客户端断线后延迟自动登出的时间，单位毫秒，默认 `60000`（1 分钟）。这段时间内客户端可以重连而不丢会话。
- **ProviderThreshold**（默认 `2000ms`）/ **ProviderOverload**（默认 `4000ms`）：负载控制的两个阈值，详见下方"过载保护"。
- **AppVersion**：应用版本号，用于灰度发布和版本控制，详见下方"滚动更新"。

## ☐ 缓存容量（最重要的容量项）

缓存容量直接关系到在线人数承载能力。

- **TableConf.CacheCapacity**：建议配置为预期的在线人数。
- 实际容量 = `CacheCapacity × CacheFactor`（`CacheFactor` 默认 `5.0`）。
- 因为数据使用 `SoftReference` 并且本地有 RocksDB 兜底，**实际可缓存的容量会远超 `CacheCapacity` 这个数字**，所以默认值基本上不需要手动去调。除非你观察到明显的缓存抖动，否则保持默认即可。

## ☐ 数据库

数据库通过 `<DatabaseConf Name DatabaseType DatabaseUrl>` 配置。

- **Memory**：零配置，仅供开发调试。
- **MySql / PostgreSQL**：生产环境使用，填标准 JDBC URL。
- **RocksDB**：仅适用于单机部署。
- **多库混用**：通过 `TableConf.DatabaseName` 把不同的表分配到不同的数据库。
- ⚠️ **重要约束**：RocksDB 一旦配置了 `GlobalCacheManager` 就会抛异常，二者不能并用。
- **连接池**：使用 Druid，注意配置 `MaxActive`、`MaxWait` 等参数，按实际并发量和数据库承载能力调整。

## ☐ 过载保护

过载保护是高并发场景下保命的手段，分 Provider 侧和 Linkd 侧两层。

**Provider 侧**（通过探测任务测量排队延迟）：

- 排队延迟超过 **ProviderThreshold**（默认 2000ms）：标记为忙碌，丢弃可丢弃的协议（`eSheddable`）。
- 排队延迟超过 **ProviderOverload**（默认 4000ms）：进入过载状态，只保留 `eCriticalPlus` 级别的协议，Linkd 停止向该 Provider 派发。

**Linkd 侧**（输出带宽保护）：

- 带宽 70% 以下：正常，不丢包。
- 带宽 70%–100%：执行自定义的 `DiscardAction` 策略。
- 超过熔断率：丢弃所有非关键协议。

**LoadConfig**（负载与流量控制）：

- `proposeMaxOnline`：建议的最大在线人数。
- `maxOnlineNew`：每秒允许的最大新增在线数。
- `reportDelaySeconds`：正常情况下的负载上报间隔。
- `digestionDelayExSeconds`：过载状态下的消化间隔。

## ☐ 优雅停服

需要停服时，要让现有用户安全退出，而不是粗暴断连。

```java
// 1. 禁止新用户被分配到本 Provider
providerService.setDisableChoiceFromLinks(true);

// 2. 等待现有用户处理完毕后，再安全关闭
```

启动时的反向操作是自动的：`ProviderApp.startLast()` 内部会在 Online 就绪之后自动调用 `setUserDisableChoice(false)`，重新开启用户分配。所以你只需在停服时手动调用禁用即可。

## ☐ 滚动更新

逐台滚动更新时，希望新登录不要被分配到旧版本服务器。

- 应用通过 `Config.setAppVersion()` 设置版本号，Provider 会把它报告给 Linkd。
- Linkd 只把新登录派发到**主版本号一致**的 Provider，判定规则为：`(serverAppVersion >>> 48) == (clientAppVersion >>> 48)`。
- 这样逐台滚动更新时，新登录自然只流向新版本服务器，旧版本实例可以安心下线。

关于服务调用时的容错：

- **Redirect 推荐按"不可靠服务"来使用**——即调用者要能容忍失败、做好降级或重试。不要假设 Redirect 一定成功。

## ☐ 热更新

热更新允许你**不停服地更新业务逻辑**，它基于 Java Agent 的类重定义和一个自定义 ClassLoader。

核心组件：

- **ClassReloader**：Java Agent 入口。
- **HotModule**：每个热更模块对应一个独立的 ClassLoader 实例。
- **HotService**：生命周期接口，提供 `start` / `stop` / `upgrade`。
- 此外还有 `HotManager`、`HotAgent`、`HotDistribute`，配合 `HotWorkingDir`、`HotDistributeDir` 等目录配置。

使用注意：

- **接口不能修改**：接口由父 ClassLoader 加载，不会被热更替换，所以接口签名一旦定下来就不要动。
- **`upgrade` 方法要处理新旧数据兼容**：热更时可能存在旧版本留下的数据结构，需要做兼容转换。
- **不建议频繁热更**：每次热更会产生新的 ClassLoader 实例，频繁热更可能导致 Metaspace 泄漏。

热更新适合作为紧急修复手段，常规迭代仍建议走正常的滚动发布。详见 [advanced-hot-reload](../reference/advanced-hot-reload.md)。

## ☐ 启动与停止顺序

多进程协同部署时，启停顺序很重要。

**启动顺序**：

1. 先启动 **ServiceManager**。
2. 再启动 **GlobalCacheManager**（如果需要分布式缓存同步）。
3. **Linkd 和 Provider** 可以按任意顺序启动。

**停止顺序**：

1. **ServiceManager** 保持运行到最后。
2. **Linkd** 先关闭客户端接入端口，阻止新用户进入。
3. **Provider** 广播通知在线用户下线。
4. **Provider** 等待处理完毕后关闭。
5. **Linkd** 关闭。
6. **GlobalCacheManager** 关闭。

---

到这里，从写代码到上线的完整链路就串起来了。更深一层的细节可以查阅：

- 配置项全表：[configuration](../reference/configuration.md)
- Provider 与 Linkd 架构：[arch-provider-linkd](../reference/arch-provider-linkd.md)
- 热更新机制：[advanced-hot-reload](../reference/advanced-hot-reload.md)
- 性能调优：[advanced-performance](../reference/advanced-performance.md)

或者回到 [文档首页](../index.md) 看看还有什么感兴趣的。
