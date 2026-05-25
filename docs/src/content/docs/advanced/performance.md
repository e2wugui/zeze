---
title: "性能调优"
sidebar:
  order: 7
---

Zeze 框架在默认配置下已经针对大多数游戏场景做了优化。本文档介绍影响性能的关键配置参数和调优策略，帮助在面对特定性能瓶颈时进行针对性调整。

## 缓存命中率优化

Zeze 直接使用本进程内的 Cache。在 Cache 命中的情况下，没有任何远程访问，此时性能可以达到最高。**Zeze 的性能核心就是 Cache 命中率**。负载分配的一个原则就是需要提高 Cache 命中率。

Zeze 使用 LRU 缓存策略，缓存在内存中，通过 `SoftReference` 在内存紧张时自动释放。

### CacheCapacity 配置

```xml
<!-- 默认表配置 -->
<TableConf CacheCapacity="20000"/>

<!-- 单独为大表配置较小缓存 -->
<TableConf Name="demo_Module1_tBigBean" CacheCapacity="1000"/>
```

**经验法则**：将 `CacheCapacity` 配置为预期的在线人数。例如，单服 2 万在线就配 20000。Zeze 使用 `CacheFactor`（默认 5.0）放大实际容量，即 20000 的配置实际可以缓存约 100000 条记录。

### 关键参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `CacheCapacity` | 20000 | 缓存容量 |
| `CacheFactor` | 5.0 | 容量放大因子 |
| `CacheInitialCapacity` | 0 | HashMap 初始容量（最小 31） |
| `CacheCleanPeriod` | 10000ms | 缓存清理间隔 |
| `CacheCleanPeriodWhenExceedCapacity` | 1000ms | 超容量时加速清理 |

### 缓存清理策略

当缓存数量超过 `CacheCapacity * CacheFactor` 时，LRU 清理器开始工作。如果清理速度跟不上新增速度，框架会进入加速清理模式（使用 `CacheCleanPeriodWhenExceedCapacity` 间隔）。这可以有效防止内存溢出。

## Checkpoint 策略

Checkpoint 的性能直接影响事务的提交延迟和系统的吞吐量。

### 持久化模式选择

```xml
<zeze CheckpointPeriod="60000"
      CheckpointMode="Table"
      CheckpointFlushMode="MultiThreadMerge"
      CheckpointModeTableFlushSetCount="50">
</zeze>
```

| 参数 | 推荐值 | 说明 |
|------|--------|------|
| `CheckpointPeriod` | 30000-60000ms | 太小增加 IO 压力，太大增加数据丢失风险 |
| `CheckpointMode` | `Table` | 分布式模式必须使用 |
| `CheckpointFlushMode` | `MultiThreadMerge` | 多线程合并，性能最优 |
| `checkpointModeTableFlushSetCount` | 50 | 合并的事务数量，可适当增大 |

### CheckpointWhenCommit

对于关键的金融数据（如充值），启用立即持久化：

```xml
<TableConf Name="tCurrency" CheckpointWhenCommit="true"/>
```

这会使该表相关的事务在提交时同步等待持久化完成，牺牲吞吐量换取数据安全性。

## 全局并发度配置

Zeze 使用乐观锁机制，并发冲突通过事务重做解决。以下配置影响并发行为：

### FastRedoWhenConflict

```xml
<zeze FastRedoWhenConflict="false">
</zeze>
```

默认关闭。开启后事务执行过程中检测到冲突会立即重做，而不是等到最后检测。适用于冲突率低但单个事务执行时间长的场景。

### 线程池调优

```xml
<zeze WorkerThreads="240" ScheduledThreads="8">
</zeze>
```

- **WorkerThreads**：普通事务处理线程。默认 `availableProcessors * 30`，对于高并发游戏服务器通常不需要调整。
- **ScheduledThreads**：调度线程。默认 `availableProcessors`。

### Provider 负载控制

```xml
<zeze ProviderThreshold="2000" ProviderOverload="4000">
</zeze>
```

- **ProviderThreshold**：测试任务延迟超过此值（毫秒）标记为忙碌，通知 Linkd 减少派发。
- **ProviderOverload**：超过此值标记为过载，Linkd 停止派发新请求。

## ProcedureStatistics -- 存储过程统计

Zeze 内置了事务执行统计，用于监控和发现性能热点。

### 统计报告

```xml
<zeze ProcedureStatisticsReportPeriod="60000"
      TableStatisticsReportPeriod="60000">
</zeze>
```

框架会定期输出每个存储过程的执行次数、耗时分布和冲突率。

### 阈值监控

```java
// 监控某个过程的执行频率，超过阈值触发处理
ProcedureStatistics.watch("MyProcedure", 1000, () -> {
    logger.warn("MyProcedure 执行频率过高");
});
```

## RocksDB 调优

单机模式下使用 RocksDB 作为存储引擎时，可以针对具体场景调优。

### 注意事项

- **RocksDB 不支持分布式**：启用了 `GlobalCacheManager` 时不能使用 RocksDB。
- **数据目录配置**：`DatabaseUrl` 指向 RocksDB 数据目录，建议放在 SSD 上。
- **内存使用**：RocksDB 会使用堆外内存，需要关注系统的内存限制。

## 网络调优

### SocketOptions

```xml
<ServiceConf Name="GameServer"
    NoDelay="true"
    SendBuffer="1M"
    ReceiveBuffer="1M"
    InputBufferMaxProtocolSize="2M"
    OutputBufferMaxSize="2M">
</ServiceConf>
```

### 流量控制

对于网关服务（Linkd），建议配置流量限制：

```xml
<ServiceConf Name="LinkdService"
    TimeThrottle="queue"
    TimeThrottleSeconds="3"
    TimeThrottleLimit="1000"
    TimeThrottleBandwidth="10M"
    OverBandwidth="100M">
    <Acceptor Ip="@external" Port="8888"/>
</ServiceConf>
```

### 连接数

```xml
<ServiceConf Name="LinkdService" maxConnections="50000">
</ServiceConf>
```

Linkd 的默认 `maxConnections` 为 1024，游戏服务器通常需要调大。

## 全局模块的并发策略

全球同服的系统里，有些模块可能是全局的，所有请求都访问同一份数据，互斥排队执行。最高性能就是单线程全速运行的事务数，这是有上限的。需要采取一些方案提高数据的并发度。

### 记录大小

Zeze 使用后台 key-value 数据库保存数据，记录读取和写入是作为整体保存的。如果记录太大，只修改少量数据也需要整个记录一起保存。一般来说按模块划分数据即可，模块太大时分成子模块或多条小记录。记录可以包含容器，一般需要设定合适上限。数据需要很大时，应用可能需要自己在 key-value 记录的基础上实现 list（多个记录保存数据）。

### 数据分块方案

常见方式是按某种规则把数据分成多个部分，参考 ConcurrentHashMap 的实现：

1. **记录数据大**：直接分成小块，提高并发。
2. **多份数据**：比如公司账号有大量并发转账请求，可以建多个子账号。转入操作根据转入者 Id 的 Hash 选择某个子账号，转入就并发了。转出按规则找到开始子账号，不够时继续扣后面的。多数情况只需访问一个子账号。读取可以分别显示或统计，用定时更新的 cache 减少实际数据访问量。
3. **RedirectHash**：Arch 部分提到的 `@RedirectHash` 就是一种把数据分组的规则，乐观估计可以解决相当一部分全局单点模块并发问题。

### 按需优化

如果可以预见请求量，并且代价不大，可以一开始就优化并发性能。否则可以等到请求量大到快无法支撑了再来优化。一开始实现一个支持任意请求量是没有必要的——计算机都是在有限资源有限时间内解决问题。

## 性能监控建议

1. **关注 ProcedureStatistics 输出**：定期检查高冲突率的存储过程，优化事务范围。
2. **监控 Checkpoint 耗时**：如果 checkpoint 周期性耗时过长，考虑增大 `CheckpointPeriod` 或使用 `MultiThreadMerge` 模式。
3. **观察缓存命中率**：命中率低于 90% 时考虑增大 `CacheCapacity`。
4. **DeadLockBreakerPeriod**（默认 60 秒）：死锁检测器周期，通常不需要调整。
5. **ProcedureLockWatcherMin**（默认 50）：存储过程锁等待监控阈值。

## Benchmark

以下测试数据来自 `ZezeJavaTest` 中的基准测试，供参考：

* **单线程顺序事务**

tasks/s=1495740.77 time=6.69s cpu=8.27s concurrent=1.24
`ZezeJavaTest::ABasicSimpleAddOneThread.java` — 循环执行存储过程估计被 Java 强烈优化，数值偏高。

* **多线程并发强冲突事务**

tasks/s=252613.37 time=3.96s cpu=9.84s concurrent=2.49
`ZezeJavaTest::BBasicSimpleAddConcurrentWithConflict.java` — 强烈冲突意味着事务几乎总是重做，由于乐观锁重做时保持锁定状态，只会重做一次，concurrent=2.49 符合预期。

* **多线程并发一般冲突事务**

tasks/s=1140652.44 time=4.38s cpu=15.58s concurrent=3.55
`ZezeJavaTest::CBasicSimpleAddConcurrent.java`

* **GlobalAsync** > 50w/s
* **Global 虚拟线程** > 15w/s（当前锁不匹配虚拟线程，否则应该接近 Async）
* **GlobalWithRaft 虚拟线程** 5w/s
