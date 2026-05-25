---
title: "事务中操作外部系统"
sidebar:
  order: 6
---

Zeze 事务可能因冲突而重做（参见 → transaction），而外部系统操作无法随事务自动回滚。本文说明如何在事务中安全地调度外部操作。

## 核心问题

Zeze 采用乐观锁，当检测到冲突时整个事务体会重新执行（redo）。这意味着：1）事务体重做时，其中的外部调用会被再次发起；2）如果事务重做后最终回滚，之前已执行的外部操作不会自动回滚。

```java
// 危险：直接在事务体中调用外部系统
long call() {
    bag.remove(itemId);          // Zeze 管理，自动回滚
    oss.putObject(data);         // 外部系统，无法回滚
    return 0;                    // 如果事务重做，putObject 会执行多次
}
```

## WhileCommit / WhileRollback

**`Transaction.whileCommit()`** 注册一个回调，仅在事务最终提交成功后执行一次。**`Transaction.whileRollback()`** 注册的回调则在事务回滚时执行。两者都保证：无论事务重做多少次，回调只执行一次。

```java
public static void whileCommit(@NotNull Runnable action);
public static void whileRollback(@NotNull Runnable action);
```

`whileCommit` 的 action 在 `finalCommit` 阶段、数据写入完成后触发；`whileRollback` 的 action 在 `finalRollback` 阶段、日志清空后触发。如果事务已完成（`Completed` 状态），action 会立即执行。

## 常见场景

### 发送网络协议

框架的 `Online` 组件已封装 `sendWhileCommit` 方法，内部就是 `whileCommit` + `send` 的组合：

```java
// Game.Online 封装
public void sendWhileCommit(long roleId, Protocol<?> p) {
    Transaction.whileCommit(() -> send(roleId, p));
}

// 使用示例
long call() {
    bag.add(reward);
    Transaction.whileCommit(() -> online.send(roleId, new SNotify(reward)));
    return 0;
}
```

回滚时通知客户端（例如告知操作失败），可使用对应的 `sendWhileRollback` 方法。

### 注册 Timer

Timer 的注册和取消都应放在 `whileCommit` 中，确保只在事务成功时生效：

```java
long call() {
    role.setState(Waiting);
    Transaction.whileCommit(() ->
        timer.schedule(delay, roleId, "timeoutHandle")
    );
    return 0;
}
```

### 提交异步任务

向线程池提交任务等操作，也应放在 `whileCommit` 中：

```java
long call() {
    data.setProcessed(true);
    Transaction.whileCommit(() ->
        executor.submit(() -> postProcess(data.copy()))
    );
    return 0;
}
```

注意：传给异步任务的参数需要在注册回调时就确定值（或复制），事务提交后临时变量已不可访问。

### 操作自定义内存数据

非 Zeze 管理的内存变量（如本地缓存、统计计数器），需通过 `whileCommit` 保证一致性：

```java
long call() {
    player.setLevel(newLevel);
    Transaction.whileCommit(() -> localCache.update(playerId, newLevel));
    return 0;
}
```

## in/out/ref 参数的建议模式

事务回调中引用外部变量时，需要注意变量捕获的时机。对于需要"传入"事务的值，直接使用即可；对于需要"传出"事务的结果，应该通过 `whileCommit` 写回：

```java
// 错误：直接修改外部变量，事务重做会导致结果不确定
int result;
long call() {
    result = compute();  // 事务重做时 result 会被反复覆盖
    return 0;
}

// 正确：通过 whileCommit 传出结果
long call() {
    int computed = compute();
    Transaction.whileCommit(() -> outerResult.set(computed));
    return 0;
}
```

## 第三方系统的可靠性问题

如果外部系统本身可能失败（如网络超时），仅用 `whileCommit` 并不能保证最终一致性。

### 可靠性足够时：直接 WhileCommit

外部操作几乎不会失败时（如本地发协议、本地线程池提交），直接用 `whileCommit` 即可：

```java
Transaction.whileCommit(() -> channel.writeAndFlush(msg));
```

### 需要可靠投递时：事务队列

引入受 Zeze 管理的表作为队列，事务内写入队列记录，提交后由搬运线程消费并执行外部操作：

```java
long call() {
    bag.remove(itemId);
    // 将外部操作写入 Zeze 表，受事务保护
    queueTable.put(taskId, new QueueItem(ossPath, data));
    Transaction.whileCommit(() -> worker.wakeup());
    return 0;
}
```

### 调度方式选择

| 方式 | 适用场景 | 说明 |
|------|---------|------|
| **事务外调度** | 外部操作不依赖事务计算结果 | 先完成事务，再发起外部调用 |
| **事务内调用** | 外部操作参数来自事务内计算 | 外部调用需能安全处理重做 |
| **拆分事务** | 外部调用不可重做但需要事务内参数 | 拆成 proc1 + rpc + proc2 |

## 幂等性设计

当事务重做时，`whileCommit` 中的 action 只执行一次，但事务体中的代码会重复执行。如果必须在事务体内调用外部系统，需要确保操作具备**幂等性**：

- 使用唯一请求 ID（如 `AutoKey` 生成），外部系统通过 ID 去重。
- 查询类操作天然幂等，可安全地在事务体内执行。
- 写入类操作尽量避免在事务体内调用，改用 `whileCommit`。

```java
long call() {
    // 使用 AutoKey 生成幂等 ID，即使事务重做也能去重
    long requestId = autoKey.nextId();
    item.setData(newData);
    Transaction.whileCommit(() ->
        externalService.submit(requestId, newData)
    );
    return 0;
}
```
