---
title: "DelayRemove 延迟删除"
sidebar:
  order: 6
---

DelayRemove 是 Zeze 框架提供的**延迟清理**组件。它用于在数据安全过期后批量删除表中的记录，典型场景是清理已下线玩家的在线数据。DelayRemove 使用持久化队列保证删除操作的可靠性，即使服务器中途重启，未完成的清理任务也不会丢失。

## 使用场景

在游戏服务器中，玩家的在线数据（如临时状态、会话信息）在玩家下线后不应立即删除，可能需要保留一段时间以处理重连、数据查询等需求。DelayRemove 提供了延迟清理机制，在数据过期后自动删除。

## 基本用法

### remove -- 延迟删除表记录

```java
// 将记录加入延迟删除队列
DelayRemove delayRemove = zeze.getDelayRemove();
delayRemove.remove(myTable, key);
```

`remove` 方法将表名和键编码为 `BTableKey` 并加入持久化队列（基于 `Zeze.Collections.Queue`）。队列按 `serverId` 隔离，每个服务器有独立的 GC 队列。

### 定时清理

DelayRemove 内部使用定时器在每天的配置时间段内执行清理。清理时按时间顺序检查队列节点：

- 如果最老的记录尚未过期（保留天数未到），停止本轮清理
- 如果已过期，删除整个节点中的所有记录

```java
// 配置清理时间窗口（XML 配置）
<DelayRemoveHourStart>2</DelayRemoveHourStart>  <!-- 凌晨 2 点开始 -->
<DelayRemoveHourEnd>6</DelayRemoveHourEnd>       <!-- 早上 6 点前结束 -->
<DelayRemoveDays>7</DelayRemoveDays>             <!-- 保留 7 天 -->
```

`DelayRemoveDays` 最小值为 7 天，确保数据不会被过早删除。

## Job 机制

除了表记录删除，DelayRemove 还支持自定义 Job，可以在延迟清理时执行任意逻辑。

### register -- 注册 Job 处理器

```java
delayRemove.register("MyCleanupJob", (dr, jobId, jobState) -> {
    // 执行自定义清理逻辑
    // jobState 是创建 Job 时保存的状态数据
});
```

### addJob -- 添加延迟 Job

```java
// 在事务内添加 Job
MyJobState state = new MyJobState();
state.setData(...);
delayRemove.addJob("MyCleanupJob", state);
```

Job 的状态通过 `_tJobs` 表持久化，服务器重启后通过 `continueJobs` 恢复执行。

### continueJobs -- 恢复未完成的 Job

```java
// 在所有模块启动完成后调用
delayRemove.continueJobs();
```

此方法遍历当前 serverId 下的所有 Job 记录，重新启动未完成的 Job。必须在所有模块（特别是 Job 处理器）都启动之后调用。

## 与 Timer 的关系

DelayRemove 的定时清理机制基于 `Task.scheduleUnsafe`，在配置的时间窗口内以 24 小时为周期执行。首次执行时间在配置的时间范围内随机选取，避免多台服务器同时执行清理操作。

DelayRemove 还使用 [AutoKey](./autokey.md) 生成 Job 的唯一 ID。

## 内部实现

DelayRemove 的队列基于 `Zeze.Collections.Queue`，每个 serverId 对应一个独立的持久化队列 `__GCTableQueue#<serverId>`。队列中的每个节点包含一批 `BTableKey` 记录，记录了表名、编码后的键和入队时间。

清理时按节点为单位处理：
1. 从队列头部取出一个节点
2. 检查节点中最早入队的记录是否已过期
3. 如果已过期，遍历节点中的所有记录，调用 `table.removeEncodedKey` 删除
4. 如果未过期，将节点放回队列，停止本轮清理

这种按节点批量处理的策略简化了实现，但可能删除一些尚未完全过期的记录。这是一个不精确但高效的清理方式。

## 配置参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `DelayRemoveHourStart` | - | 每天开始清理的小时 |
| `DelayRemoveHourEnd` | - | 每天结束清理的小时 |
| `DelayRemoveDays` | 7 | 数据最小保留天数（最小值为 7） |
