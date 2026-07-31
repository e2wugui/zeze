---
title: "Prometheus 监控集成"
description: "Zeze 集成 Prometheus：通过 ZezeCounter/PrometheusCounter 暴露过程、表、线程池、Checkpoint、网络等指标"
category: reference
order: 73
---

本文是 Zeze **Prometheus 监控集成**的参考——说明监控的定位、指标采集原理（`ZezeCounter` 接口与 `PrometheusCounter` 实现）、暴露 `/metrics` 端点的方式，以及可观测的指标维度与统计报告配置。完整配置见 [配置参考](./configuration.md)，性能调优依据见 [性能调优指南](./advanced-performance.md)，线程监控见 [线程模型与调度](./advanced-threads.md)。

Zeze 集成了 Prometheus 监控，通过统一的计数器抽象把框架内部的运行指标暴露出来，供 Prometheus 抓取并在 Grafana 等 Dashboard 中可视化。定位是**运维可观测性**：回答「过程耗时多少、冲突多不多、缓存命中如何、网络是否健康」等问题。

---

## 采集原理：ZezeCounter 抽象

Zeze 把「统计」与「指标后端」解耦，核心是接口 `Zeze.Util.ZezeCounter`：

- 框架内部所有埋点都面向 `ZezeCounter` 编程（过程开始/结束、表操作、协议收发字节数等）。
- 具体后端通过系统属性 `ZezeCounter` 选择实现类，默认 `Zeze.Util.PerfCounter`；设为 Prometheus 时使用 `Zeze.Util.PrometheusCounter`：

```java
var className = System.getProperty("ZezeCounter", "Zeze.Util.PerfCounter");
```

- 设为空白或 `"null"` 则**关闭**统计（`ENABLE = false`），零开销。

`PrometheusCounter implements ZezeCounter`，内部用 `io.prometheus` 客户端库注册 Counter / Histogram / CounterWithCallback，把抽象方法映射为 Prometheus 指标。`Zeze.Component.AbstractStatistics`（注意包路径是 `Zeze.Component`，不是 `Zeze.Services`）负责周期性地汇总统计并输出报告。

> `ZezeCounter` 定义的抽象能力包括：分配累加器（`allocCounter`）、带标签的 Counter/Histogram 创建器、过程级埋点（`procedureStart/End/Redo`）、表级统计（`getOrAddTableInfo`）、协议收发大小与耗时（`addRecvSizeTime` / `addSendSize`）、服务启停（`serviceStart/Stop`）等。具体方法签名以所集成版本的 SDK 为准。

---

## 暴露 /metrics 端点

`PrometheusCounter.addHttpHandler(HttpServer)` 在内置 HTTP 服务器上注册两个端点：

| 路径 | 调度方式 | 作用 |
|------|----------|------|
| `/metrics` | `Normal`（普通线程池） | Prometheus 抓取入口，由 `PrometheusScrapeHandler` 输出文本格式指标 |
| `/healthy` | `Direct`（调用者线程） | Exporter 自身健康检查 |

```java
httpServer.addHandler("/metrics", 0,
        TransactionLevel.None, DispatchMode.Normal, new MetricHandler());
httpServer.addHandler("/healthy", 0,
        TransactionLevel.None, DispatchMode.Direct, new HealthyHandler());
```

部署时把 Prometheus 的 scrape 目标指向该 HTTP 端口即可。

---

## 可观测的指标维度

`PrometheusCounter` 注册的主要指标（指标名即 Prometheus metric name）：

### 过程（Procedure）
- `procedure_started{procedure}` — 启动次数
- `procedure_completed{procedure,result_code}` — 完成次数（按结果码）
- `procedure_duration_seconds{procedure,result_code}` — 耗时分布（Histogram）
- `procedure_redo{procedure}` — 冲突重做次数
- `procedure_redo_and_release_lock{procedure}` — 重做并释放锁次数
- `procedure_many_locks{procedure}` — 单过程加锁数量分布

### 表（Table / 数据库操作）
- `database_table_operation{table,operation}` — 按表、按操作类型计数。operation 包括：`readLock` / `writeLock` / `storageGet` / `tryReadLock` / `tryWriteLock` / `acquireShare` / `acquireModify` / `acquireInvalid` / `reduceInvalid` / `redo`
  - 其中 `storageGet` 反映**回源数据库**的次数，是判断缓存命中率的关键。
  - `acquireShare` / `acquireModify` / `acquireInvalid` / `reduceInvalid` 反映与 GlobalCacheManager 的交互量。

### 协议（Protocol）
- `protocol_recv_bytes{protocol}` — 收字节数
- `protocol_duration_seconds{protocol}` — 协议处理耗时分布
- `protocol_send{protocol}` — 发送次数
- `protocol_send_bytes{protocol}` — 发字节数

### 服务 / 网络
- `service_recv{service}` / `service_recv_bytes{service}` — 收包数 / 字节数（CounterWithCallback，实时读取）
- `service_send{service}` / `service_send_bytes{service}` / `service_send_raw_bytes{service}` — 发送相关
- `service_output_buffer_bytes{service}` — 输出缓冲区大小分布（定时采样，间隔由属性 `ServiceOutputObserveInterval` 控制，默认 60 秒）

### 任务
- `task_duration_seconds{task}` — 任务运行耗时分布

> 此外还有 `procedure` 与 `table` 的**周期性统计报告**（文本日志），由下面的配置项控制周期，便于不接 Prometheus 时也能从日志观测。

---

## 统计报告配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `ProcedureStatisticsReportPeriod` | `60000` | 过程统计报告周期（毫秒） |
| `TableStatisticsReportPeriod` | `60000` | 表统计报告周期（毫秒） |

报告覆盖过程执行次数、冲突重做次数、平均耗时，以及表缓存命中率、记录数、脏记录数等。

---

## Dashboard 配置建议

在 Grafana 中建议围绕以下看板组织：

1. **概览**：QPS（`procedure_completed`）、平均/P99 耗时（`procedure_duration_seconds`）、冲突率（`procedure_redo` / `procedure_started`）。
2. **缓存与存储**：命中率（`storageGet` 占比）、各表操作分布（`database_table_operation`）。
3. **全局缓存**：`acquireShare` / `acquireModify` / `acquireInvalid` / `reduceInvalid` 趋势，识别跨实例热点。
4. **网络**：收发字节与包数（`service_*` / `protocol_*`）、输出缓冲区（`service_output_buffer_bytes`）。
5. **线程/任务**：任务耗时（`task_duration_seconds`）、加锁分布（`procedure_many_locks`）。

调优依据参见 [性能调优指南](./advanced-performance.md)。

---

## 使用要点

1. **选择后端**：默认 `PerfCounter` 仅做框架内统计；接入 Prometheus 需通过 `-DZezeCounter=Zeze.Util.PrometheusCounter` 切换实现。
2. **关闭统计**：设 `-DZezeCounter=null` 可完全关闭，适用于压测基线或对开销敏感的场景。
3. **端点隔离**：`/metrics` 走普通线程池，避免抓取阻塞 IO 线程；`/healthy` 走 `Direct`，轻量快速。
4. **指标按需取用**：表级 `storageGet` 是缓存命中率的代理指标，过程级 `procedure_redo` 是冲突率的代理指标——这两个最值得优先上 Dashboard。

---

## 延伸阅读

- [配置参考](./configuration.md) — `ProcedureStatisticsReportPeriod` / `TableStatisticsReportPeriod` 等配置
- [性能调优指南](./advanced-performance.md) — 用监控指标驱动调优
- [线程模型与调度](./advanced-threads.md) — 线程池与任务耗时监控
