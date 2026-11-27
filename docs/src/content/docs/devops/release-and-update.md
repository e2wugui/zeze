---
title: "版本发布与滚动更新"
sidebar:
  order: 3
---

Zeze 支持多种版本发布与更新策略，从完全重启到逐台滚动更新与紧急类修复。本文重点描述**滚动更新**流程中的优雅停止机制与 **Schemas** 版本兼容检查。

## 更新策略概览

### 完全重启

停止全部服务器，全部更新，全部重启。更新频率不会太高，重启够快，这个方案最简单，虽然之后会出现一定的登录高峰，但一般都扛得住。系统规模越来越大，更新频率可能也会越来越高，会导致大量重启，此时需要考虑其他方式。**这个方案总是可用**，在下面的方案无法使用时，总是可以选择它。

### 逐台更新服务器

服务器本身是多台的，可以一台一台更新并重启。重启服务器上的用户重新登录，只要把重新登录处理得对用户比较无感，就能实现接近无缝的升级。整个过程可能需要一定时间，此时新旧版本同时存在，需要应用具有新旧同时存在的兼容性。

### Instrumentation 热更

Java 自带的 Instrumentation 提供了任意 class 热更新机制，可以在不重启服务器的基础上把新的 class 转载进去，保留所有服务器状态，完全无感无缝。这种热更有一定限制，一般用于紧急 BUG 修复、临时禁用某些功能、增加日志进行调试等。具体使用限制请查阅 Instrumentation 的文档。涉及更新多台服务器时，一般也是一台一台独立进行，很短的时间内存在新旧版本同时运行的时刻，需注意新旧版本兼容性。当然一般 Instrumentation 式热更新，新旧版本问题一般不大。

| 策略 | 停机时间 | 适用场景 |
|------|----------|----------|
| 完全重启 | 全部 | 任何场景，最简方案 |
| 逐台更新 | 接近零 | 日常版本发布 |
| Instrumentation 热更 | 零 | 紧急 BUG 修复 |

## 滚动更新流程

### 1. 标记下线

调用 `setDisableChoiceFromLinks(true)` 通知所有 Linkd 不再向该 Provider 分发新连接。该方法向所有 Linkd 发送 `SetDisableChoice` 协议，已在线用户不受影响。

```java
providerService.setDisableChoiceFromLinks(true);
```

### 2. 等待存量处理完成

等待当前服务器上的在线用户逐步减少，可配合超时踢出或引导重新登录。

### 3. 停止服务

确认存量请求基本处理完毕后，发送 SIGTERM 或调用 stop 方法优雅停止。

### 4. 更新并重启

部署新版本后启动服务。Provider 在 `OnHandshakeDone` 中发送 `AnnounceProviderInfo` 携带当前 `appVersion`，自动向 Linkd 注册并恢复流量分配。

## Schemas 版本兼容检查

**Schemas** 是 Zeze 的数据结构定义元数据。每次启动时，框架会比较当前代码的 Schemas 与数据库中保存的旧版 Schemas，确保兼容性。

### 兼容性规则

启动时 `Schemas.checkCompatible` 自动对比当前代码与数据库中的旧版 Schemas。主要规则：

| 规则 | 说明 |
|------|------|
| Variable.Id 类型不可变 | 同一 ID 的变量类型必须与旧版兼容 |
| 禁止复用已删除的 Variable.Id | 除非类型完全一致（允许"反悔"） |
| BeanKey 变量只增不减 | 被用作 Key 的 Bean 不允许删除变量 |
| Dynamic Bean 映射不可变更 | typeId 到 className 的映射不可修改或删除 |

类型兼容性表（部分）：

```
bool <-> byte <-> short <-> int <-> long <-> float <-> double
string <-> binary
vector2 <-> vector2int (允许互转)
```

### 处理不兼容

检测到不兼容变更时启动会抛出 `IllegalStateException`。可修正定义，或通过 `allowSchemasReuseVariableIdWithSameType` 配置放宽部分限制，极端情况下清除数据库。

## 相关章节

- [Prometheus 监控集成](../advanced/metrics.md)：通过监控指标评估更新影响。
- [Maven 中央仓库发布](./maven-deploy.md)：版本发布的前置构建流程。
