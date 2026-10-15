---
title: "History 数据溯源"
sidebar:
  order: 5
---

**History** 模块为 Zeze 提供数据变更审计与溯源能力。它记录每次事务对持久化表的修改日志，并支持将历史变更回放到独立的 **Apply 数据库**中，用于数据校验、审计分析和问题排查。

## 默认关闭与启用时机

`Config.history` 默认为空字符串，`isHistory()` 返回 `false`。XML 中省略 `History`
属性或使用 `History=""` 都不启用历史采集。需要采集时，在构造 `Application` 前配置
非空的发号名：

```java
var config = Config.load("zeze.xml");
config.setHistory("MyHistory");
var zeze = new Application("MyApp", config);
```

关闭时不创建 `HistoryModule`、不注册或打开 `tHistory` 表，也不分配历史 gid、
编码或写入历史记录。`PendingGidLedger` 在首次使用时才分配，其周期对账在首次
登记 gid 时才启动；普通事务和 `Application.stop()` 不会因访问它而触发创建。

History 关闭且脏记录没有 ChangeListener 时，事务提交跳过 `Changes` 对象创建及
后续的日志树、记录变更收集遍历。事务本身的日志应用、锁检查和业务数据刷盘仍照常执行。
有 ChangeListener 时仍收集变更并按提交前的监听者快照通知。

监听器和 History 共用核心的 `Changes` 采集。History 关闭时，即使有 ChangeListener，
也不会创建历史提交上下文或历史缓冲；两者都不需要时连 `Changes` 也不创建。
History 启用时，纯读事务同样不创建 `Changes`、不取号、不创建历史提交上下文。

本轮插件化仍在 ZezeJava 工程及同一 JAR 内，通过接口划分核心和实现的职责。
`Application.getHistory()` 只返回可选的 `IHistory`，未启用或停止后返回 `null`。
装配发生在构造期，`HistoryModule` 同时固定发号名；运行中修改 `Config.history`
不会启用、关闭或更换发号名，需要调整时应新建 Application。没有运行期卸载或服务发现机制。

回放工具、历史表和账本通过实现侧入口访问。该入口只取已有实现，未启用时抛出异常，
不会隐式构造模块：

```java
var history = HistoryModule.get(zeze);
var table = history.getHistoryTable();
var affected = history.apply(1);
history.startHttpServer(netty, 8080);
```

默认内存回放库与 `ApplyHelper` 在首次 `apply`（包括首个 HTTP 回放请求）时创建。
关闭模块后不能通过这些工具入口重新创建资源。

## 核心组件

| 组件 | 职责 |
|------|------|
| **IHistory** | 核心保留的可选能力接口，编排注册、启动校验、提交准备及关闭 |
| **PreparedCommit** | 单笔脏写的提交上下文，在数据应用前取号，应用后生成完整历史 |
| **PendingHistory** | 已固定内容的待刷历史，绑定所属表和账本，支持合并、快照及失败重试 |
| **History** | 变更日志容器，在事务执行期间收集 `BLogChanges`，编码后持久化 |
| **HistoryModule** | 模块入口，注册历史表、管理 Apply 流程、提供 HTTP 接口 |
| **ApplyHelper** | Apply 执行引擎，批量读取历史记录并应用到 Apply 数据库 |
| **ApplyTable** | 单表的 Apply 实现，支持 Put/Remove/Edit 三种变更操作 |
| **IApplyDatabase** | Apply 端数据库抽象接口 |

## 工作原理

### 日志收集

脏写事务持有记录锁、关联集合锁及检查点提交使用权后，先调用
`IHistory.prepareCommit(procedure)`。实现完成 gid 分配和账本登记，失败通过
`CommitRejectedException` 令本次事务在应用数据前回滚并返回 `Procedure.Closed`。

业务日志应用后，核心统一收集 `Changes`，再调用 `PreparedCommit.capture(changes)`。
`HistoryModule` 在此构造 `BLogChanges.Data` 并返回 `PendingHistory`。每条日志包含
**GlobalSerialId**、按 TableKey 组织的 **Changes**、**Timestamp** 和可选的 Protocol 信息。
内存表的变更不会被记录。

如果应用业务数据后采集失败，此时无法干净回滚，也没有完整历史可重试，必须进入框架
fatal 处理。Immediately 模式即使补刷业务成功，也不能将该事务报告为正常成功。

### 日志编码与持久化

`History` 实现 `IHistory.PendingHistory`，使用两级结构：**logChanges**（原始日志）
和 **encoded**（编码字节）。核心不依赖这两个容器或 `BLogChanges` 的具体类型。

- `getTable()` 返回创建时绑定的历史表，不能随配置或 Application 停机字段变化。
- `mergeFrom()` 只修改接收者；只允许同一实例、同一历史表的缓冲合并。
- `snapshot()` 创建独立容器，保留表及账本绑定。FlushSet 先建一个快照，再逐个合并成员，
  不修改成员缓冲；失败轮重分组不会混入其他组的历史或跨组核销 gid。
- `encode0()` 和 `writeOnly()` 可重复调用，失败时保留内容。历史写入复用核心已有的数据库
  事务映射，与同库业务数据使用同一个事务，实现不得自行提交、回滚或关闭这个事务。
- 全部相关数据库和本地缓存事务提交成功后，核心才调用 `onPersisted()` 核销 gid 并清理。
  此方法须幂等，只完成本地清理，不做额外 I/O。

多库仍按现有逻辑逐库提交，不提供跨库原子性。History 表归属校验及旧记录格式保持不变。

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

`IApplyDatabase` 提供开表、单条历史记录的原子应用事务及游标保存/恢复。
Zeze 提供内存实现（`ApplyDatabaseMemory`）和持久化实现（`ApplyDatabaseZeze`）。
HistoryModule 的默认分析入口仍使用内存后端；持久化回放可显式构造 `ApplyHelper`。

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
GET /Zeze/Builtin/HistoryModule/WalkPage?count=1
```

参数 `count` 指定本次 Apply 的历史记录数量。通过 `startHttpServer` 方法启动：

```java
historyModule.startHttpServer(netty, 8080);
```

## 使用场景

- **数据审计**：追踪任意数据记录的完整变更历史。
- **问题排查**：通过 Apply + Verify 还现并定位数据异常。
- **合规要求**：满足数据变更留痕的监管需求。

## 核心边界与调用迁移

核心仅保留 `Zeze.Transaction.IHistory` 及嵌套的两个提交接口。唯一的具体实现引用在
Application 构造期的默认装配点：配置非空时创建 `HistoryModule`。事务与检查点不再
引用 History 实现、内置日志 Bean、发号服务或账本。

| 原调用 | 当前调用 |
|--------|----------|
| `zeze.getHistoryModule()` | 检查能力用 `zeze.getHistory()`；使用工具用 `HistoryModule.get(zeze)` |
| `zeze.getPendingGidLedger()` | `HistoryModule.get(zeze).getPendingGidLedger()` |
| `table.createApplyTable(dbApplied)` | `ApplyTable.create(table, dbApplied)` |

`Changes` 的创建由核心根据脏记录、监听者及是否需要全量采集决定，不再读取 History 配置。
事务回滚必需的日志和 ChangeListener 仍属于核心。

停机先排空在飞提交及检查点，再调用 `IHistory.close()`，最后关闭数据库。实现须关闭
HTTP、停止已创建的账本守护并注销历史表；某项清理失败也要继续其他清理。
模块锁串行化回放和关闭，关闭 HTTP 时释放模块锁，避免等待中的请求与关闭互相阻塞。
关闭过程不创建账本或回放库，已创建资源可在部分启动失败后清理。

## 相关章节

- [事务系统](../core/transaction.md)：Zeze 事务模型基础。
- [Onz 分布式事务编排](./onz.md)：跨集群事务协调。
