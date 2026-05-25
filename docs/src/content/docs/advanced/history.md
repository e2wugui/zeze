---
title: "History 数据溯源"
sidebar:
  order: 5
---

**History** 模块为 Zeze 提供数据变更审计与溯源能力。它记录每次事务对持久化表的修改日志，并支持将历史变更回放到独立的 **Apply 数据库**中，用于数据校验、审计分析和问题排查。

## 核心组件

| 组件 | 职责 |
|------|------|
| **History** | 变更日志容器，在事务执行期间收集 `BLogChanges`，编码后持久化 |
| **HistoryModule** | 模块入口，注册历史表、管理 Apply 流程、提供 HTTP 接口 |
| **ApplyHelper** | Apply 执行引擎，批量读取历史记录并应用到 Apply 数据库 |
| **ApplyTable** | 单表的 Apply 实现，支持 Put/Remove/Edit 三种变更操作 |
| **IApplyDatabase** | Apply 端数据库抽象接口 |

## 工作原理

### 日志收集

每次事务提交时，Zeze 通过 `History.buildLogChanges` 收集变更到 `BLogChanges.Data` 中。每条日志包含 **GlobalSerialId**（全局唯一 ID）、**Changes**（按 TableKey 组织的变更集合）、**Timestamp** 和可选的 Protocol 信息。内存表的变更不会被记录。

### 日志编码与持久化

`History` 使用两级结构：**logChanges**（事务期间动态收集的原始日志）和 **encoded**（编码后的二进制数据）。Checkpoint 时通过 `flush` 写入 `tHistory` 表。多个 History 实例可通过 `History.merge` 合并。

## Apply 机制

**ApplyHelper** 从 `tHistory` 表中批量读取历史记录，并将变更应用到 **IApplyDatabase** 中：

```java
var applyHelper = new ApplyHelper(zeze, historyTable, dbApplied, 20_000);

// 每次应用一条历史记录
Map<ApplyTable<?, ?>, Set<Object>> affected = applyHelper.apply(1);
```

### ApplyTable 变更处理

`ApplyTable` 支持三种变更类型：

| 类型 | 处理方式 |
|------|----------|
| **Record.Remove** | 从 Apply 库中删除对应记录 |
| **Record.Put** | 写入完整的新记录 |
| **Record.Edit** | 读取现有记录，通过 `followerApply` 应用增量日志后写回 |

`ApplyTable` 内部使用 **LRU 缓存**（默认 4096 条）减少对 Apply 数据库的读取次数。

## IApplyDatabase 接口

`IApplyDatabase` 仅包含一个 `open(String tableName)` 方法。Zeze 提供内存实现（`ApplyDatabaseMemory`）和持久化实现（`ApplyDatabaseZeze`）。

## Verify 校验

`ApplyTable.verifyAndClear()` 用于校验 Apply 数据库与原始表的一致性：

```java
applyTable.verifyAndClear();
```

校验逻辑：

1. 遍历原始表所有记录，逐条与 Apply 数据库对比。
2. 发现缺失或不一致的记录时，抛出异常并输出 `diff` 结果。
3. 校验通过后清除 Apply 数据库中的所有记录。

`diff` 方法会跳过包含指定字符串（如 `version=`）的行，避免版本号差异干扰校验。

## HTTP 接口

HistoryModule 内置 HTTP Servlet，用于手动触发 Apply 操作：

```
GET /walkPage?count=1
```

参数 `count` 指定本次 Apply 的历史记录数量。通过 `startHttpServer` 方法启动：

```java
historyModule.startHttpServer(netty, 8080);
```

## 使用场景

- **数据审计**：追踪任意数据记录的完整变更历史。
- **问题排查**：通过 Apply + Verify 还现并定位数据异常。
- **合规要求**：满足数据变更留痕的监管需求。

## 相关章节

- [事务系统](../core/transaction.md)：Zeze 事务模型基础。
- [Onz 分布式事务编排](./onz.md)：跨集群事务协调。
