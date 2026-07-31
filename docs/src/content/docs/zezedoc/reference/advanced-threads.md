---
title: "线程模型与调度"
description: "Zeze 线程模型：Task 调度核心、Checkpoint/Net/Raft 专用线程、线程池分类与 @DispatchMode 协议调度"
category: reference
order: 71
---

本文是 Zeze **线程模型与任务调度**的参考——说明各类专用线程的职责、线程池的分类与容量、`@DispatchMode` 如何控制协议在哪个线程执行，以及 Java 21 虚拟线程对同步代码的加持。事务并发模型见 [事务](./transaction.md)，性能调优见 [性能调优指南](./advanced-performance.md)，并发编程心智模型见 [无惧并发](../manual/07-multithreading-without-fear.md)，完整配置见 [配置参考](./configuration.md)。

Zeze 采用**多类专用线程 + 受控任务调度**的模型：业务逻辑、IO、持久化、共识各跑在独立的线程（池）上，互不阻塞，并由注解显式声明协议的调度方式。

---

## 线程职责总览

| 线程 | 核心类 | 职责 |
|------|--------|------|
| 任务调度核心 | `Zeze.Util.Task` | 业务任务调度，提供 `run` / `schedule` 等提交入口，背后是普通线程池与调度线程池 |
| Checkpoint 线程 | `Zeze.Transaction.Checkpoint` | 定期把脏数据刷盘（持久化） |
| Net Selector（IO 线程） | `Zeze.Net.Selector` | 网络收发，**不阻塞**，基于 JDK NIO 自研（`Selector extends Thread`） |
| Raft 线程 | `Component.AbstractThreading` | 共识相关操作 |

设计原则：**IO 线程只负责收发，绝不在 IO 线程跑业务**。业务逻辑被分发到工作线程池执行。

---

## 线程池分类

| 线程池 | 默认容量 | 用途 |
|--------|----------|------|
| 普通线程池 `WorkerThreads` | `max(配置值, availableProcessors * 30)` | 默认业务逻辑执行池 |
| 调度线程池 `ScheduledThreads` | `max(配置值, availableProcessors)` | 定时/周期任务，如 Checkpoint 周期触发、热更监视 |
| Checkpoint 线程 | 单独的 daemon 线程 | 定期刷脏；优先级 `NORM_PRIORITY + 2` |
| Raft 线程 | 由 Raft 组件管理 | 共识 |
| IO 线程 | 自研 NIO（`Zeze.Net.Selector`） | 收发，不阻塞 |

容量含义：当配置值小于按 CPU 核数推导的下限时，取推导值。即框架会保证一个「按机器规模自适应」的最低并发度。

> Checkpoint 线程名形如 `Checkpoint-{serverId}`，是守护线程（`setDaemon(true)`），并在构造时设了更高的优先级，以保证持久化不被业务饿死。

---

## @DispatchMode：协议调度方式

协议（Protocol）/RPC 响应进入业务层时，由 `@DispatchModeAnnotation` 决定在哪种线程上执行：

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DispatchModeAnnotation {
    DispatchMode mode() default DispatchMode.Normal;
}
```

`Zeze.Transaction.DispatchMode` 枚举：

| 取值 | 说明 |
|------|------|
| `Normal` | 在**普通线程池**中执行（默认） |
| `Critical` | 在**重要线程池**中执行 |
| `Direct` | 在**调用者线程**直接执行（通常为 IO 线程，需保证不阻塞） |

**覆盖默认**：可在 `Service` 子类中重载 `DispatchProtocol` / `DispatchRpcResponse` 来覆盖注解的默认优先级——重载的结果**高于注解**。这用于全局层面强制某些协议走特定线程池。

---

## Checkpoint 线程驱动持久化

`Checkpoint` 是脏数据持久化的驱动器，按周期工作：

- **周期**：`CheckpointPeriod` 间隔（默认 `60000` 毫秒）。线程在锁/条件变量上 `cond.await(period, MILLISECONDS)` 等待下一次触发。
- **模式**：`CheckpointMode`
  - `Table`：按表批量刷，**分布式必须用**。
  - `Immediately`：每事务立即刷，性能差，不建议。
- **刷盘方式**：`CheckpointFlushMode`
  - `MultiThreadMerge`（推荐）：多线程合并刷，性能最优。
  - 另有 `SingleThread` / `MultiThread` / `SingleThreadMerge`。

相关合并参数：`CheckpointModeTableFlushSetCount`（合并的事务数，默认 `50`）、`CheckpointTransactionPeriod`（默认 `300000`）。

> Checkpoint 的最终一次刷盘发生在停止阶段（`isRunning=false` 退出循环后），保证停机前数据落盘。

---

## 任务调度 API

`Zeze.Util.Task` 是任务调度核心，常用入口：

| 方式 | 说明 |
|------|------|
| `Task.run(...)` | 向工作线程池提交任务 |
| `Task.schedule(...)` / `Task.scheduleUnsafe(...)` | 向调度线程池提交定时/周期任务 |

> 提交到线程池的任务，凡涉及事务一致性、需要等事务真正提交后再执行的副作用，一般要用 `whileCommit` 包装（事务提交成功后才回调），避免回滚后仍触发外部副作用。详见 [第三方交互与重做](./third-party-interactions.md)。

---

## Java 21 虚拟线程加持

在 Java 21+ 上启用虚拟线程后，lock 等待和同步 IO **不再占用操作系统载波线程**：

- **性能**：带来跟异步（async/await）同样的性能——线程再多也不耗 OS 线程。
- **代码风格**：又能保持**同步写法**，不必把业务拆成回调链或 Future 组合。

这意味着在 Zeze 上可以用「同步代码 + 虚拟线程」同时获得可读性与高并发吞吐，无需为每个阻塞点手写异步化。

---

## 延伸阅读

- [事务](./transaction.md) — 事务与线程、Checkpoint、锁的关系
- [性能调优指南](./advanced-performance.md) — 线程池容量与并发度的性能影响
- [无惧并发](../manual/07-multithreading-without-fear.md) — Zeze 并发编程心智模型
- [配置参考](./configuration.md) — `WorkerThreads` / `ScheduledThreads` / `CheckpointPeriod` 等
