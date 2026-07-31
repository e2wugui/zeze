---
title: "消息队列集成"
description: "Zeze 内置 MQ 与第三方集成：生产者消费者模型、事务中发送的重做问题、RedoQueue 跨系统事务重试"
category: reference
order: 74
---

本文是 Zeze **消息队列（MQ）集成**的参考——说明 Zeze 内置 MQ 的定位与组件、典型用途、与事务配合时的重做问题，以及 `RedoQueue` 跨系统事务重试机制。事务中与第三方交互的细节见 [第三方交互与重做](./third-party-interactions.md)，事务模型见 [事务](./transaction.md)。

Zeze 内置一套消息队列实现，位于 `Zeze.MQ` 包，提供生产者/消费者模型；同时支持与第三方 MQ（如 RocketMQ）集成。MQ 的定位是**异步处理、跨系统通信、削峰填谷**。

---

## 内置 MQ 组件

`Zeze.MQ` 包的核心类：

| 类 | 职责 |
|----|------|
| `MQ` | 消息队列主体，提供创建/修改队列等管理能力（参考 Kafka，并结合 `TaskOneByOne` 实现） |
| `MQManager` | 管理多个 MQ 实例（`AbstractMQManager` 为抽象基类） |
| `MQProducer` | 消息生产者，发送消息（返回 `void`，不返回成功标志） |
| `MQConsumer` | 消息消费者，通过 `MQListener` 回调处理消息 |
| `MQListener` | 消费回调接口：`void onMessage(BPushMessage.Data pushMessage)`（**单参数**） |
| `MQPartition` | 分区，**分区数决定并发度** |
| `MQConfig` / `AbstractMQAgent` / `MQAgent` | 配置与 Agent |

**消息模型**：`BMessage`（Bean，位于 `Zeze.Builtin.MQ`）只有三个业务字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| `Timestamp` | `long` | 发送时间，**用户不用填写**（框架填充） |
| `Properties` | `Map<String, String>` | 用户自定义属性 |
| `Body` | `Zeze.Net.Binary` | 消息体，用户自定义 |

> ⚠️ **注意**：`BMessage` **没有 `topic` 和 `partitionIndex` 字段**——topic 归属生产者/消费者实例（由构造时传入），partition 由 key 哈希决定。并发由 `partitionCount` 决定。

### 生产者 API

`MQProducer` 的真实方法（`Zeze.MQ.MQProducer`）：

```java
public class MQProducer {
    public MQProducer(String topic) throws Exception;
    public void sendMessage(Object key, BMessage.Data message);  // 按 key 哈希选 partition
    public void sendMessage(int key, BMessage.Data message);
    public void sendMessage(long key, BMessage.Data message);
    public void sendMessage(BMessage.Data message);              // 不指定 key
    public String getTopic();                                     // 注意是 getTopic，不是 getName
    public BOptions.Data getOptions();
    public int getPartition();
    public void close();
}
```

> `sendMessage` 返回 **`void`**（不是 `boolean`）。**没有** `prepareMessage` / `commitMessage` / `getName` 方法——事务半消息机制在第三方集成层（如 RocketMQ），不是内置 `MQProducer` 的能力。

### 消费者 API

```java
public class MQConsumer {
    public MQConsumer(String topic, MQListener listener);
    public static Collection<MQConsumer> getConsumers();
    public long getSessionId();
    public String getTopic();                 // 订阅名，可能是队列/topic 名
    public BOptions.Data getOptions();
    public int getPartition();
    public @NotNull MQListener getListener();
    public void close();
}

public interface MQListener {
    void onMessage(BPushMessage.Data pushMessage);   // 单参数，接收推送消息
}
```

### 队列类型

`Options` 定义三种可靠性/部署模式：

| 类型 | 说明 |
|------|------|
| `Single` | 单一 MQ Server，无备份 |
| `DoubleWrite` | 主备双写：写入 Leader 后复制到 Follower 才算成功；Master 负责负载均衡与故障切换 |
| `Raft3` | 基于 Zeze-Raft 实现，多副本强一致 |

---

## 典型用途

1. **异步处理**：把不必同步返回结果的工作（如发邮件、写日志、通知）丢到 MQ，主流程立即返回。
2. **跨系统通信**：不同服务/异构系统之间通过消息解耦，生产者不需感知消费者是否在线。
3. **削峰填谷**：突发流量先入队列缓冲，消费者按自身速率处理，保护下游。

---

## 与事务的关系（重要）

在 Zeze 事务中操作 MQ 时，必须注意**重做（Redo）问题**：Zeze 事务在冲突时会重做整个过程，若直接在事务体里 `sendMessage`，**重做会导致重复发送**。

正确做法二选一：

1. **`whileCommit` 包装**：把发送动作注册为「事务提交成功后」才执行的回调，保证只发一次：

   ```java
   Transaction.getCurrent().whileCommit(() -> producer.sendMessage(msg));
   ```

   这要求能容忍「提交成功但发送失败」的微小不一致（详见 [第三方交互与重做](./third-party-interactions.md)）。

2. **事务消息模式（仅第三方集成）**：若对接支持半消息的 MQ（如 RocketMQ），可用其事务消息 / 半消息机制，由第三方 MQ 配合本地事务 ID 保证「事务提交才真正投递」。注意：内置 `MQProducer` **没有** `prepareMessage` / `commitMessage`，半消息能力只存在于第三方集成层。

---

## RedoQueue：跨系统事务重试

对于「非 Zeze Application 的客户端」要参与 Zeze 事务语义的场景，Zeze 提供 `RedoQueue` 机制做**跨系统事务重试**：

- **`RedoQueue(Client)`**：给非 Zeze Application 使用。它把任务**持久化存入 RocksDB**，同时发送给 ZezeApplication 执行。这样即使客户端进程崩溃，重启后未完成的任务仍可继续。
- **`RedoQueueServer`**：运行在 ZezeApplication 一侧，真正执行任务。
- **注册处理器**：`register(queue, type, Predicate<Binary> task)` 注册任务处理器。`Predicate<Binary>` 返回 `true` 表示处理成功。
- **断点续传**：框架**记录已完成任务的编号**；若出现回档（数据回滚到旧版本），则从「当前未完成」处继续处理，避免遗漏或重复。

适用场景：外部系统需要在 Zeze 事务提交可靠执行某个副作用，又无法直接加入 Zeze 事务时。

> `RedoQueue` 的具体方法签名以 SDK 为准；其核心思想是「持久化待办 + 编号去重 + 回档续传」，与 [第三方交互与重做](./third-party-interactions.md) 中描述的「可靠副作用」模式一脉相承。

---

## 第三方集成（RocketMQ 等）

除内置 MQ 外，也可对接 RocketMQ 等成熟消息中间件。对接时同样要遵守**「事务中发送需防重做」**的约束——优先用 RocketMQ 的事务消息或 `whileCommit` 包装，避免在事务体内直接发送。

---

## 使用要点

1. **分区即并发**：`partitionCount` 决定消费并发度，按吞吐需求规划；只增不减，且增加时最好队列为空，否则 one-by-one 特性可能被破坏。
2. **事务内慎发**：事务体内直接 `sendMessage` 会在重做时重复发送，务必用 `whileCommit` 或事务消息。
3. **可靠副作用用 RedoQueue**：跨系统、需持久化重试的场景用 `RedoQueue` + `RedoQueueServer`，享受断点续传与回档保护。
4. **选型**：单机简单场景用内置 `Single`；要求高可用用 `DoubleWrite` 或 `Raft3`；已有 MQ 基础设施则直接集成第三方。

---

## 延伸阅读

- [第三方交互与重做](./third-party-interactions.md) — 事务中与外部系统交互的可靠模式与 `whileCommit`
- [事务](./transaction.md) — 事务重做（Redo）机制
