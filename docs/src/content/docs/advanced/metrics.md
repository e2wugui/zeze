---
title: "Prometheus 监控集成"
sidebar:
  order: 6
---

Zeze 通过 **PrometheusCounter** 实现与 Prometheus 监控系统的集成，基于 `prometheus-metrics-core` 库提供丰富的运行时指标。通过 HTTP 端点暴露标准 metrics 格式，可直接接入 Prometheus Server 或 Grafana Dashboard。

## 启用方式

### 添加依赖

在 `pom.xml` 中引入 `prometheus-metrics-core` 和 `prometheus-metrics-exporter-httpserver`（Zeze 已声明为 `provided` scope）。

### 注册 HTTP Handler

通过 `PrometheusCounter.addHttpHandler` 将 metrics 端点注册到 Zeze 的 HTTP 服务：

```java
var httpServer = new HttpServer();
httpServer.start(netty, 9090);
PrometheusCounter.addHttpHandler(httpServer);
```

这将注册两个端点：

| 路径 | 说明 |
|------|------|
| `/metrics` | Prometheus 指标抓取端点 |
| `/healthy` | 健康检查端点，返回 `Exporter is healthy.` |

### 激活 Counter

通过 JVM 系统属性指定 Counter 实现类（默认已启用 `PrometheusCounter`）：

```
-DZezeCounter=Zeze.Util.PrometheusCounter
```

设为空或 `null` 可完全禁用指标收集。

## ZezeCounter 内置指标

**ZezeCounter** 是 Zeze 的指标抽象接口，`PrometheusCounter` 是其 Prometheus 实现。以下指标自动收集：

### 事务指标

| 指标名 | 类型 | 标签 | 说明 |
|--------|------|------|------|
| `procedure_started` | Counter | procedure | 事务启动次数 |
| `procedure_completed` | Counter | procedure, result_code | 事务完成次数（含结果码） |
| `procedure_duration_seconds` | Histogram | procedure, result_code | 事务执行耗时分布 |
| `procedure_redo` | Counter | procedure | 事务重做次数 |
| `procedure_redo_and_release_lock` | Counter | procedure | 重做并释放锁次数 |
| `procedure_many_locks` | Histogram | procedure | 事务锁定数量分布 |

### 协议指标

| 指标名 | 类型 | 标签 | 说明 |
|--------|------|------|------|
| `protocol_recv_bytes` | Counter | protocol | 接收字节数 |
| `protocol_duration_seconds` | Histogram | protocol | 协议处理耗时 |
| `protocol_send` | Counter | protocol | 发送次数 |
| `protocol_send_bytes` | Counter | protocol | 发送字节数 |

### 数据库表指标

| 指标名 | 类型 | 标签 | 说明 |
|--------|------|------|------|
| `database_table_operation` | Counter | table, operation | 表操作计数（readLock, writeLock, storageGet, acquireShare, acquireModify 等） |

### 服务指标

| 指标名 | 类型 | 标签 | 说明 |
|--------|------|------|------|
| `service_output_buffer_bytes` | Histogram | service | 输出缓冲区大小分布 |
| `service_recv` | Counter | service | 服务接收计数 |
| `service_recv_bytes` | Counter | service | 服务接收字节数 |
| `service_send` | Counter | service | 服务发送计数 |
| `service_send_bytes` | Counter | service | 服务发送字节数 |
| `service_send_raw_bytes` | Counter | service | 服务发送原始字节数 |

## Dashboard 配置建议

### Prometheus 配置

```yaml
scrape_configs:
  - job_name: 'zeze'
    scrape_interval: 15s
    static_configs:
      - targets: ['localhost:9090']
```

### 关键监控项

1. **事务延迟**：关注 `procedure_duration_seconds` 的 P99 分位数，发现慢事务。
2. **重做率**：`procedure_redo` / `procedure_started` 的比率反映锁冲突程度。
3. **协议吞吐**：`protocol_recv_bytes` 和 `protocol_send_bytes` 监控网络负载。
4. **缓冲区压力**：`service_output_buffer_bytes` 异常增长可能表示网络瓶颈。
5. **缓存同步**：`database_table_operation` 中的 `acquireShare/acquireModify/acquireInvalid` 反映 GlobalCacheManager 的负载。

## 自定义指标

通过 `ZezeCounter` 接口可创建自定义指标：

```java
var counter = ZezeCounter.instance.allocCounter("my_custom_counter");
counter.inc();

var labeledCounter = ZezeCounter.instance.allocLabeledCounterCreator(
    "my_labeled_counter", "label1", "label2");
labeledCounter.labelValues("value1", "value2").inc();
```

## 相关章节

- [版本发布与滚动更新](../devops/release-and-update.md)：运维发布流程。
- [配置说明](../devops/configuration.md)：服务配置详情。
