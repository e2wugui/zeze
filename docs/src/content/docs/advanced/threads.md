---
title: "线程模型"
sidebar:
  order: 2
---

Zeze 使用精心设计的多线程模型来保证高性能和正确性。框架管理三类线程池（普通、重要、调度），配合网络 IO 线程、Checkpoint 持久化线程和 Raft 共识线程，形成完整的并发体系。理解线程模型对正确使用 Zeze API 和排查性能问题至关重要。

## 线程池分类

### threadPoolDefault -- 普通线程池

用于处理绝大多数事务和任务。线程名命名规则为 `ZezeTaskPool-X`（序号从 1 开始），所有者类路径为 `Zeze.Util.Task.threadPoolDefault`，任务队列为 `LinkedBlockingQueue`。线程数量计算公式：

```
max(WorkerThreads, availableProcessors * 30)
```

默认约 240 个线程（8核 CPU）。GlobalCacheManager 服务器和 ServiceManager 服务器默认 240。支持虚拟线程（Java 21+），当 `useVirtualThread=true` 且 `useUnlimitedVirtualThread=true` 时，会使用无限制的虚拟线程池替代固定线程池。

```java
Task.getThreadPool();  // 获取普通线程池
```

### threadPoolCritical -- 重要线程池

用于执行框架内部的关键任务，与普通线程池隔离，防止业务任务饥饿影响系统功能。使用 `newCachedThreadPool` 实现，按需创建线程，优先级为 `NORM_PRIORITY + 2`。线程名命名规则为 `ZezeCriticalPool-X`（序号从 1 开始），所有者类路径为 `Zeze.Util.Task.threadPoolCritical`，任务队列为 `SynchronousQueue`。

CriticalPool 专门用于以下场景：

1. GlobalCacheServer 的客户端（`Zeze.Transaction.GlobalClient`）处理协议
2. ServiceManager 的客户端（`Zeze.Services.ServiceManager.AgentClient`）处理协议
3. Raft 客户端（`Zeze.Raft.Agent.NetClient`）处理 LeaderIs 协议和 RPC 回复
4. 其它不能排队导致饥饿的关键任务

```java
Task.getCriticalThreadPool();  // 获取重要线程池
```

### threadPoolScheduled -- 调度线程池

用于定时任务和延迟任务。线程名命名规则为 `ZezeScheduledPool-X`（序号从 1 开始），所有者类路径为 `Zeze.Util.Task.threadPoolScheduled`，任务队列为 `LinkedBlockingQueue`。线程数量计算公式：

```
max(ScheduledThreads, availableProcessors)
```

默认约 CPU 核数 * 15。GlobalCacheManager 服务器和 ServiceManager 服务器默认 120。

```java
Task.getScheduledThreadPool();  // 获取调度线程池
```

## Task API

**Task**（`Zeze.Util.Task`）是 Zeze 的任务调度核心类，封装了事务执行和线程池管理。

### 执行事务

```java
// 同步执行事务（阻塞等待结果）
long result = Task.call(procedure, "MyProcedure");

// 异步执行事务（提交到线程池）
Task.run(procedure, "MyProcedure");

// 指定调度模式异步执行
Task.run(procedure, "MyProcedure", null, DispatchMode.Normal);
```

### 定时任务

```java
// 延迟执行
TimerFuture<?> future = Task.schedule(5000, () -> doSomething());

// 周期执行
TimerFuture<?> future = Task.scheduleUnsafe(1000, 5000, () -> periodicTask());
```

`scheduleUnsafe` 不会捕获异常，适合内部使用。`schedule` 会在异常时记录日志。

### 按键串行执行

```java
// 同一个 roleId 的任务串行执行
Task.runTaskOneByOneByKey(roleId, "processLogin", () -> {
    return loginProcedure.call();
});
```

`TaskOneByOneByKey` 保证相同 key 的任务按提交顺序依次执行，不同 key 的任务并行执行。这是实现角色级操作串行化的核心机制。

### 线程池初始化

```java
// 在 Application 启动时自动调用
Task.initThreadPool(threadPoolDefault, threadPoolScheduled);
// 同时创建 threadPoolCritical
```

## Selector 网络 IO

**Selector**（`Zeze.Net.Selector`）是 Zeze 网络层的 IO 线程。每个 Selector 实例运行在独立的守护线程中，基于 Java NIO 实现。

### 线程模型

多个 Selector 实例由 **Selectors** 管理器协调。新建连接时通过轮询（round-robin）分配到不同的 Selector 线程上，实现网络 IO 的多核并行处理。

```java
// Selector 核心配置
DEFAULT_BUFFER_SIZE = 32 * 1024;              // 单个 buffer 容量
DEFAULT_BBPOOL_LOCAL_CAPACITY = 1000;          // 本地池最大保留
DEFAULT_BBPOOL_GLOBAL_CAPACITY = 100 * 1000;   // 全局池最大容量
```

### Buffer 池化

Selector 实现了 `ByteBufferAllocator` 接口，使用三级 Buffer 池：

1. **本地池**（`bbPool`）：每个 Selector 线程私有，无锁访问。
2. **全局池**（`bbGlobalPool`）：所有 Selector 共享，需要加锁。
3. **堆外分配**：池中无可用 buffer 时，优先分配 DirectByteBuffer，失败降级为堆内 buffer。

### 任务队列

每个 Selector 维护一个 `ConcurrentLinkedQueue<Runnable>` 任务队列，用于在 IO 线程上执行非 IO 操作（如连接建立后的初始化逻辑）。

```java
selector.addTask(() -> doSomethingInIoThread());
```

## Checkpoint 线程

**Checkpoint**（`Zeze.Transaction.Checkpoint`）运行在独立的守护线程中，优先级为 `NORM_PRIORITY + 2`，负责将内存中的脏数据持久化到后端数据库。

### 持久化模式

| 模式 | 说明 |
|------|------|
| **Table** | 按事务关联集合为单位持久化，分布式模式必须使用 |
| **Immediately** | 每个事务立即持久化，性能最差，仅特殊场景使用 |

### 工作流程（Table 模式）

```
循环等待(period毫秒)
    -> RelativeRecordSet.flushWhenCheckpoint()
        -> 收集所有脏记录集合
        -> 对每个集合执行 flush()
            -> 编码记录数据
            -> 为每个数据库创建事务
            -> 写入数据并提交
            -> 清理编码状态
```

`CheckpointPeriod`（默认 60 秒）控制持久化间隔。应用停机时会执行最后一次完整的 checkpoint。

## Raft 线程

Zeze 的 Raft 共识实现使用独立的线程处理选举、日志复制和状态机应用。Raft 操作与业务事务通过队列和锁机制隔离，确保共识过程不被业务逻辑阻塞。

## Threading 分布式同步

**Threading** 模块（`Zeze.Component.Threading`）提供跨进程的同步原语，支持分布式环境下的互斥和协调：

- **Mutex**：分布式互斥锁，通过 `MutexTryLock`/`MutexUnlock` 协议实现。
- **Semaphore**：分布式信号量，支持 `SemaphoreCreate`、`SemaphoreTryAcquire`、`SemaphoreRelease`。
- **ReadWriteLock**：分布式读写锁，通过 `ReadWriteLockOperate` 协议操作。

这些原语的事务级别均为 `None`，不参与 Zeze 事务，仅用于跨进程同步。

## 虚拟线程支持

Zeze 从 Java 21 开始支持虚拟线程（Virtual Thread），通过系统属性控制：

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `useVirtualThread` | `true` | 是否启用虚拟线程 |
| `useUnlimitedVirtualThread` | 非 JUnit 测试时为 `true` | 是否使用无限制虚拟线程池 |

启用虚拟线程后，普通线程池和重要线程池都会切换到 `Executors.newVirtualThreadPerTaskExecutor()`，理论上可以支持更高的并发量。但虚拟线程在事务冲突率高的场景下可能导致频繁 redo，需要根据实际场景评估。
