---
title: "消息队列"
sidebar:
  order: 3
---

Zeze 内置了一套轻量级**消息队列**（MQ）系统，支持分区（Partition）、持久化存储和推模式消费。此外还提供了与 **RocketMQ** 的集成适配，可在 Zeze 事务中发送事务消息。

## 整体架构

MQ 系统由以下核心角色组成：

- **MQManager**：队列管理器，负责 Topic/Partition 的创建、消息收发和负载上报。
- **MQ（客户端）**：面向生产者和消费者的入口，管理到各个 MQManager 的网络连接。
- **MQProducer / MQConsumer**：生产者与消费者 API 封装。
- **MQPartition / MQSingle**：服务端分区实现，每个分区对应一个 `MQSingle` 实例。

### 消息流转

```
Producer -> MQ.sendMessage(hash, message)
         -> MQManager -> MQSingle -> MQFileWithIndex (持久化)
                                  -> PushMessage -> Consumer
```

## 生产者 API

**MQProducer** 提供多种 `sendMessage` 重载，支持按键哈希分区或随机分区发送：

```java
// 创建生产者（打开已存在的 Topic）
var producer = new MQProducer("myTopic");

// 按键哈希路由到固定分区
producer.sendMessage(userId, message);

// 随机分区发送
producer.sendMessage(message);

// 关闭
producer.close();
```

内部通过 `Integer.remainderUnsigned(hash, partitionCount)` 将消息路由到对应分区的 MQManager 节点。

## 消费者 API

**MQConsumer** 采用推模式，服务端主动将消息推送到订阅者。`MQListener` 是函数式接口：

```java
var consumer = new MQConsumer("myTopic", pushMessage -> {
    // 处理消息
});
consumer.close();
```

## 分区机制

**MQPartition** 管理一个 Topic 下的所有分区实例（`MQSingle`），并负责消费者的负载均衡：

- 新消费者订阅或取消订阅时，触发 `arrangeConsumer()` 重新分配分区。
- 分配策略基于 `partitionIndex % subscriberCount` 取模，确保每个分区恰好绑定一个消费者。

```java
// MQPartition 核心方法
public void subscribe(AsyncSocket sender, long sessionId);
public void unsubscribe(AsyncSocket sender, long sessionId);
```

## 文件存储

**MQFileWithIndex** 实现了基于文件 + RocksDB 索引的持久化方案：

- 文件路径格式：`{home}/{topic}/{partitionId}.{nextMessageId}`
- 索引存储在 RocksDB 中，表名格式为 `{topic}.{partitionId}.{nextMessageId}`
- 每条消息写入文件时附带 `[messageId(8字节)][size(4字节)][body]` 的头部
- 每隔 `makeIndexPeriod`（默认 100）条消息建立一次索引
- 单文件超过 `trunkFileSize`（默认 100MB）时自动滚动创建新文件

```java
// 追加消息（在锁内执行）
fileWithIndex.appendMessage(message);

// 从文件装载消息填充内存队列
fileWithIndex.fillMessage(messageQueue, headMessageId, endMessageId);
```

## RocketMQ 集成

`Zeze.Services.RocketMQ.Producer` 封装了 `TransactionMQProducer`，支持将消息发送与 Zeze 事务绑定：

```java
var producer = new Producer(zeze, "producerGroup", clientConfig);
producer.start();

// 事务消息：仅当 procedureAction 执行成功时消息才生效
producer.sendMessageWithTransaction(msg, () -> {
    // Zeze 事务逻辑
    return 0;
});
```

核心机制：`executeLocalTransaction` 检查 `_tSent` 表中的事务状态；`checkLocalTransaction` 在 RocketMQ 回查时查询事务结果；事务状态通过 Zeze 表持久化，确保消息与数据的一致性。

Consumer 端使用 `Zeze.Services.RocketMQ.Consumer`，封装了 `DefaultMQPushConsumer`，支持标准的订阅和监听器注册。

## 配置

**MQConfig** 通过 XML 自定义配置 `<MQConfig RpcTimeout="20000"/>`，默认 RPC 超时 20 秒。

## 相关组件

- MQ 的 Master 协调依赖 [服务发现](../arch/service-manager.md) 进行节点注册与发现。
- 消息持久化使用 RocksDB，与 Zeze 的存储体系保持一致。
- 对于需要跨服事务保证的场景，可结合 [Onz 分布式事务编排](./onz.md) 使用。
