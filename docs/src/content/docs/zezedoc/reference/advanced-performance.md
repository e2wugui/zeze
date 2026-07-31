---
title: "性能调优指南"
description: "Zeze 性能调优：缓存命中率、Checkpoint 策略、全局并发度、RocksDB 调优与基准数据"
category: reference
order: 72
---

本文是 Zeze **性能调优**的参考——围绕缓存命中率、Checkpoint 策略、全局并发度、RocksDB 调优四大关键因素，给出指标目标、配置项与调优建议，并附基准量级与过载保护配置。表与缓存机制见 [表](./table.md)、线程模型见 [线程模型与调度](./advanced-threads.md)、全局缓存协调见 [全局缓存管理器](./arch-global-cache-manager.md)、嵌入式存储见 [RocksDB](./db-rocksdb.md)、完整配置见 [配置参考](./configuration.md)、上线调优见 [上线清单](../manual/08-production-checklist.md)。

Zeze 的性能由四个关键因素决定：**缓存命中率、Checkpoint 策略、全局并发度、RocksDB 调优**。下面逐项展开。

---

## 1. 缓存命中率（最重要的指标）

缓存命中率是 Zeze 性能**最重要**的指标，目标应保持在 **>99%**。

- `TableCache` 的命中率直接决定是否回源数据库——命中即在内存完成，未命中则回源，开销骤增。
- 配置 `CacheCapacity`：建议按**预期在线人数 × CacheFactor(默认 5.0)** 设置。`getRealCacheCapacity()` 实际生效容量为 `floor(cacheCapacity * cacheFactor)`。

```xml
<TableConf CacheCapacity="200000" CacheFactor="5.0"/>
```

- **SoftReference + 本地 RocksDB**：缓存 value 用 `SoftReference` 持有，并在本地保留一份 RocksDB 副本。这样**内存容量与 value 大小无关**——冷数据被 GC 回收后可从本地 RocksDB 恢复，而无需回源远端数据库。默认配置下基本不用专门调。

> `selectDirty` 适合统计类查询（直接读脏数据，不走完整事务）；`whileCommit` 可用于容忍短暂不一致的场景。

---

## 2. Checkpoint 策略

Checkpoint 决定脏数据多久落盘一次，影响持久化频率与吞吐。

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `CheckpointPeriod` | `60000`（1 分钟） | 定时刷盘间隔 |
| `CheckpointMode` | `Table` | `Table`（分布式必须用）/ `Immediately`（每事务立即，性能差，不建议） |
| `CheckpointFlushMode` | `MultiThreadMerge` | **推荐**，多线程合并刷，性能最优 |
| `CheckpointModeTableFlushSetCount` | `50` | 合并的事务数 |
| `CheckpointTransactionPeriod` | `300000` | |

- **`Table` + `MultiThreadMerge`** 是分布式部署的推荐组合：周期性把一批事务的脏数据合并后多线程刷盘，摊薄了单次 IO 成本。
- `Immediately` 模式每事务立即刷，性能极差，仅用于特殊场景。

---

## 3. 全局并发度（GlobalCacheManager）

启用 `GlobalCacheManager` 后，跨实例的数据共享会引入额外的网络往返，是分布式部署下主要的性能变量。详见 [全局缓存管理器](./arch-global-cache-manager.md)。

| 操作 | 网络往返 | 说明 |
|------|----------|------|
| `Share`（读共享） | 约 **1 次** | 申请共享读 |
| `Modify`（写） | **至少 2 次** | 申请 + Reduce |
| `Reduce` 超时 | — | 触发事务重试 |

**优化方向**：
- **热点分散**：把集中在单一记录的写压力分散到多条记录/多个分片。
- **减少跨实例 Modify**：尽量让同一份数据的写操作落在同一实例，避免频繁的跨实例申请。

---

## 4. RocksDB 调优

`DatabaseRocksDb` 是嵌入式存储，特点：

- **无网络开销**：数据在本地进程内，单机高性能。
- **不支持 GlobalCacheManager**：源码在创建 RocksDb 时会显式校验——若同时配置了 `GlobalCacheManagerHostNameOrAddress` 会直接抛异常：

```java
case RocksDb:
    if (!zeze.getConfig().getGlobalCacheManagerHostNameOrAddress().isBlank())
        throw new IllegalStateException("RocksDb Can Not Work With GlobalCacheManager.");
```

- **本地缓存 value**：配合 `SoftReference`，GC 回收后从本地 RocksDB 恢复。详见 [RocksDB](./db-rocksdb.md)。

> 因此单机/嵌入式场景选 RocksDB（高性能、零网络），多实例共享场景才用关系库/TiKV + GlobalCacheManager。

---

## 事务重做（Redo）

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `FastRedoWhenConflict` | `false` | 冲突时**立即重做**；开启可能增加 CPU 开销 |

- 乐观锁机制下，**冲突少时吞吐极高**。
- 冲突多时事务会被重做。`FastRedoWhenConflict=false` 时按一定节奏重做；设为 `true` 则立即重做，减少延迟但可能放大 CPU 压力。

---

## 基准量级（benchmark）

以下为 Zeze 内存路径相对于传统 SQL 的近似量级，供建立直觉（非精确承诺）：

| 操作 | 相对量级 |
|------|----------|
| 内存 map 查询 vs SQL 查询 | 快约 **1000 倍** |
| 内存 map 更新 vs SQL update | 快约 **1000 倍** |
| 默认每事务 flush 时，map 更新 vs SQL update | 快约 **5000 倍** |

> 量级差异主要来自：内存操作 vs 网络往返 + 磁盘 IO；以及事务 flush 时批量提交摊薄了 IO。

---

## 监控与统计报告

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `ProcedureStatisticsReportPeriod` | `60000` | 过程（Procedure）统计报告周期 |
| `TableStatisticsReportPeriod` | `60000` | 表统计报告周期 |

报告内容覆盖过程执行次数、冲突重做次数、平均耗时、表缓存命中率、记录数、脏记录等。结合 Prometheus 可做长期监控，详见 [Prometheus 监控集成](./advanced-metrics.md)。

---

## 过载保护

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `ProviderThreshold` | `2000` | 阈值，达到后开始限制 |
| `ProviderOverload` | `4000` | 过载，达到后更强力地拒绝/降级 |

> 上面的 `2000` / `4000` 即源码默认值（`Config.java`：`providerThreshold = 2000`、`providerOverload = 4000`）。建议生产环境显式配置，避免不同版本默认值调整带来意外。

---

## 死锁检测与锁等待监控

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `DeadLockBreakerPeriod` | `60000` | 死锁检测周期（`Zeze.Util.DeadlockBreaker`） |
| `ProcedureLockWatcherMin` | `50` | 锁等待监控阈值（分钟级，超过则告警） |

死锁检测会主动打破成环的锁等待；锁等待监控用于发现长时间持锁/等锁的过程，定位性能瓶颈。

---

## 调优清单速查

1. **缓存命中率 >99%**：调大 `CacheCapacity`，依赖 SoftReference + 本地 RocksDB。
2. **Checkpoint 用 `Table` + `MultiThreadMerge`**：分布式必选，性能最优。
3. **减少跨实例 Modify**：分散热点，让写落在本地。
4. **单机选 RocksDB**：零网络开销；但不可与 GlobalCacheManager 共存。
5. **冲突低时吞吐最高**：必要时开 `FastRedoWhenConflict` 牺牲一点 CPU 换延迟。
6. **配好过载保护与死锁检测**：`ProviderThreshold`/`ProviderOverload`/`DeadLockBreakerPeriod`。

---

## 延伸阅读

- [表](./table.md) — `TableConf` 与缓存、脏记录机制
- [线程模型与调度](./advanced-threads.md) — 线程池容量与调度
- [全局缓存管理器](./arch-global-cache-manager.md) — Share/Modify/Reduce 的细节
- [RocksDB](./db-rocksdb.md) — 嵌入式存储配置
- [配置参考](./configuration.md) — 上述所有配置项的完整定义
- [上线清单](../manual/08-production-checklist.md) — 上线前的性能验收
