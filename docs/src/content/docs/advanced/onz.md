---
title: "Onz 分布式事务编排"
sidebar:
  order: 4
---

**Onz**（On Zeze）是 Zeze 提供的跨集群分布式事务方案。它支持**两段式提交**和 **Saga** 两种事务模式，适用于对数据一致性要求较高的跨服场景，如跨服转账。

## 需求与限制

Zeze 集群内的事务由框架自动管理，但在分服运维模式下，跨 Zeze 集群的操作需要额外的事务协调。Onz 专门解决这一问题。

Onz 事务适用于数据完整性要求高但并发量可控的场景。对性能要求极高的跨服功能建议采用缓存装载 + 定时同步的方式。

## 核心组件

| 组件 | 职责 |
|------|------|
| **Onz** | 嵌入 Zeze 集群的模块，注册并执行 OnzProcedure/OnzSaga |
| **OnzProcedure** | 两段式事务的执行单元，在 Zeze 端运行 |
| **OnzSaga** | Saga 模式的执行单元，支持补偿（Cancel） |
| **OnzTransaction** | OnzServer 端的事务编排基类，协调多个 Zeze 集群的调用 |
| **OnzServer** | 独立或嵌入的事务协调服务，管理事务生命周期和故障恢复 |

## 事务模式

### 两段式提交（Procedure 模式）

Zeze 端注册 `OnzProcedure`，协调方通过 `OnzTransaction.callProcedureAsync` 发起远程调用：

```java
// Zeze 端注册
onz.register("transfer", (procedure, argument, result) -> {
    // 事务逻辑
    procedure.sendReadyAndWait(); // 通知就绪并等待提交
    return 0;
}, ArgumentClass.class, ResultClass.class);
```

执行流程：远程调用 -> 执行逻辑 -> `sendReadyAndWait`（发送就绪信号）-> 等待 Commit/Rollback。

### Saga 模式

Saga 模式执行阶段不阻塞等待，由协调方在完成后统一处理：

```java
// Zeze 端注册 Saga（需提供补偿函数）
onz.registerSaga("transfer",
    (saga, argument, result) -> {
        // 正向逻辑，执行完自动返回
        return 0;
    },
    (cancelArgument) -> {
        // 补偿（Cancel）逻辑
        return 0;
    },
    ArgumentClass.class, ResultClass.class, CancelClass.class);
```

当事务需要回滚时，OnzServer 对已成功执行的 Saga 发送 `FuncSagaEnd(cancel=true)` 触发补偿。

## OnzTransaction 编排

`OnzTransaction` 是协调端的事务基类，开发者继承并实现 `perform()` 方法：

```java
public class MyTransaction extends OnzTransaction<ArgData, ResultData> {
    @Override
    protected long perform() throws Exception {
        // 并发调用多个 Zeze 集群的 OnzProcedure
        var future1 = callProcedureAsync("zeze1", "transfer", arg1, result1);
        var future2 = callProcedureAsync("zeze2", "transfer", arg2, result2);

        future1.await();
        future2.await();
        return 0;
    }
}
```

注意事项：
- `callProcedureAsync` 和 `callSagaAsync` 在同一事务内不能混用。
- 每个 Zeze 集群最多一个并发调用。

## OnzServer 部署

OnzServer 支持两种部署方式：

```java
// 方式一：每个 Zeze 集群独立 ServiceManager（推荐）
var server = new OnzServer("zeze1=zeze1.xml;zeze2=zeze2.xml", myConfig);

// 方式二：共享 ServiceManager
var server = new OnzServer("shared.xml", "zeze1;zeze2", myConfig);
```

执行事务：

```java
server.perform(transaction);
```

## 保存模式与 Flush 退化

Onz 支持三种保存模式：

- **FlushImmediately**：事务完成后立即两段式保存到后端数据库，安全性最高。
- **FlushAsync**：仅提交到缓存，各 Zeze 集群自行选择保存时机，性能最好。
- **FlushPeriod**：定时两段式保存（当前未完全支持）。

### FlushPeriod 的致命缺点

FlushPeriod 模式看起来不错（既降低延迟又有数据一致性保证），但有个致命缺点：如果某个 Zeze 服务宕机，会导致其他健在的 Zeze 服务也只能放弃所有的缓存数据，此时保存行为已经缺失数据，不可能一致了。最终导致所有 Zeze 集群不可控地被完全关联起来，谁都不允许出错。**所以这个等级在没有好的解决方案前不考虑支持。**

### Flush 退化策略

1. **Zeze 服务器方**：等待 FlushReady 超时，记录日志继续保存，相当于降级为 FlushAsync 模式。
2. **OnzServer 协调方**：收不到部分 Zeze 服务器的 FlushReady，先允许已 FlushReady 的继续保存，然后定时发起未收到 FlushReady 的 Zeze 的全服 Checkpoint，触发 Flush 流程，直到相关数据保存成功。

当 Flush 阶段协调失败时，系统退化处理：允许已就绪的节点继续保存，并对未就绪的节点定时触发 Checkpoint，尽量降低数据不一致的风险。

### 运行模式选择建议

以上两种运行模式的选择，建议参考跨服功能的稳定性。需求变动大的建议第一种（独立进程）。

## 故障恢复

OnzServer 使用 RocksDB 持久化事务状态（`commitPoint` 和 `commitIndex`），并在启动和定时任务中重放未完成的事务（`eCommitting` 重发 Commit，`ePreparing` 重发 Rollback），确保异常宕机后的事务最终一致性。

## 相关章节

- [消息队列](./mq.md)：Zeze 内置 MQ 系统。
- [History 数据溯源](./history.md)：数据变更审计。
