---
title: "Timer 定时器"
sidebar:
  order: 4
---

Zeze 的 **Timer** 组件提供持久化的定时调度能力。Timer 的定义和状态保存在数据库中，服务器重启后能自动恢复所有未完成的定时器。Timer 与事务系统深度集成，支持 Server 级别、Account 级别和 Role 级别三种定时器作用域。

## 基本用法

Timer 的核心接口是 `Zeze.Component.Timer`，通过 `Application.getTimer()` 获取实例。所有调度操作必须在事务内执行。

### schedule -- 延时/周期定时器

```java
// 在事务内调度一个 5 秒后触发、周期 10 秒的定时器
String timerId = zeze.getTimer().schedule(
    5000,                    // delay: 首次触发延迟（毫秒）
    10000,                   // period: 周期（毫秒），-1 表示仅触发一次
    MyTimerHandle.class,     // 处理类
    myCustomData             // 自定义数据（可为 null）
);
```

`schedule` 返回自动生成的 timerId（以 `@` 开头的 Base64 字符串）。

### scheduleNamed -- 命名定时器

```java
// 调度一个命名定时器，同名 timer 只能存在一个
boolean created = zeze.getTimer().scheduleNamed(
    "myUniqueTimer",          // timerId，不能以 @ 开头
    5000,                     // delay
    10000,                    // period
    MyTimerHandle.class,
    myCustomData
);
```

如果同名定时器已存在于当前服务器，先取消旧的再创建新的；如果存在于其他服务器，返回 `false`。

### Cron 定时器

```java
// 使用 Cron 表达式调度定时器
String timerId = zeze.getTimer().schedule(
    "0 30 8 * * ?",           // 每天 8:30:00 触发
    MyTimerHandle.class,
    null
);

// 便捷方法：每天固定时刻
zeze.getTimer().scheduleDay(8, 30, 0, MyTimerHandle.class, null);

// 每周固定时刻
zeze.getTimer().scheduleWeek(1, 8, 30, 0, MyTimerHandle.class, null);

// 每月固定时刻
zeze.getTimer().scheduleMonth(1, 8, 30, 0, MyTimerHandle.class, null);
```

Cron 表达式格式为：`秒 分 时 日 月 周`。

### TimerHandle 回调

定时器触发时，框架在事务内调用 `TimerHandle.onTimer`：

```java
public class MyTimerHandle implements TimerHandle {
    @Override
    public void onTimer(TimerContext context) throws Exception {
        // context.timerId    - 定时器 ID
        // context.customData - 创建时的自定义数据
        // context.happenTimes - 已触发次数（首次为 1）
        // context.expectedTimeMills - 计划触发时间
        // context.nextExpectedTimeMills - 下次计划触发时间（SimpleTimer 可修改）

        // 如果 onTimer 抛出异常，该定时器会被自动取消
    }

    @Override
    public void onTimerCancel(BTimer timer) throws Exception {
        // 定时器被取消后触发（可选实现）
        // 可用于复活该 timerId
    }
}
```

### cancel -- 取消定时器

```java
zeze.getTimer().cancel(timerId);
```

取消操作也在事务内执行。如果定时器属于其他服务器，会通过 Redirect 机制转发取消请求。

### TimerContext

`TimerContext` 提供定时器触发时的上下文信息：

| 字段 | 类型 | 说明 |
|------|------|------|
| `timerId` | String | 定时器唯一标识 |
| `timerName` | String | TimerHandle 完整类名 |
| `customData` | Bean | 自定义数据 |
| `happenTimes` | long | 已触发次数 |
| `expectedTimeMills` | long | 计划触发时间 |
| `nextExpectedTimeMills` | long | 下次计划触发时间（可修改） |

## 高级特性

### 触发次数和结束时间

```java
// 最多触发 5 次
zeze.getTimer().schedule(1000, 5000, 5, MyHandle.class, null);

// 在指定时间之前触发
zeze.getTimer().schedule(1000, 5000, -1, endTimeMillis, MyHandle.class, null);
```

### Missfire 策略

当服务器重启后发现定时器的计划触发时间已过（missfire），可以选择不同的处理策略：

```java
// 什么都不做，重新调度下一个周期
Timer.eMissfirePolicyNothing   // 默认

// 立即触发一次，然后以当前时间为基准重新计算周期
Timer.eMissfirePolicyRunOnce

// 立即触发一次，但保持原来的调度周期
Timer.eMissfirePolicyRunOnceOldNext
```

### OneByOneKey

为定时器指定串行执行 key，同一 key 的定时器不会并发执行：

```java
zeze.getTimer().schedule(1000, 5000, -1, -1,
    Timer.eMissfirePolicyNothing,
    MyHandle.class, null,
    "player:12345"             // oneByOneKey
);
```

## Account 级别 Timer

`TimerAccount` 提供基于 Account 的在线定时器管理，定时器的生命周期与账号的登录会话绑定。

### 在线定时器

```java
// 账号在线时注册定时器
timerAccount.scheduleOnline(account, clientId, 5000, 10000,
    -1, -1, MyHandle.class, customData);
```

在线定时器的特点：

- **登录版本校验**：触发时检查登录版本是否匹配，不匹配则自动取消
- **自动转发**：如果账号登录在其他服务器，定时器会通过 Transmit 机制转发过去
- **自动清理**：账号下线时自动取消所有在线定时器

### 离线定时器

```java
// 账号离线后注册定时器
timerAccount.scheduleOffline(account, clientId, 60000, 3600000,
    -1, -1, Timer.eMissfirePolicyNothing, MyHandle.class, customData);
```

离线定时器在账号重新登录时自动取消。

## Role 级别 Timer

`TimerRole` 提供基于角色（Role）的定时器管理，行为与 TimerAccount 类似，但以 roleId 为标识：

```java
// 在线角色定时器
timerRole.scheduleOnline(roleId, 5000, 10000, -1, -1,
    MyHandle.class, customData);

// 离线角色定时器
timerRole.scheduleOffline(roleId, 60000, 3600000,
    Timer.eMissfirePolicyNothing, MyHandle.class, customData);
```

## Online 集成

通过 `initializeOnlineTimer` 初始化 Timer 与 Online 系统的集成：

```java
timer.initializeOnlineTimer(providerApp);
```

此方法检测 `ProviderImplement` 的类型，自动选择使用 `TimerAccount` 或 `TimerRole`。

## 事务集成

Timer 的所有调度和取消操作都在 Zeze 事务内执行。如果事务回滚，定时器的变更也会自动回滚：

- 调度成功后，实际的 `Future` 注册通过 `Transaction.whileCommit` 延迟到事务提交后执行
- 取消操作通过 `Transaction.whileCommit` 延迟取消 `Future`

```java
// Timer 内部实现
Transaction.whileCommit(() -> {
    timerFutures.put(timerId, Task.scheduleUnsafe(delay, () -> fireSimple(...)));
});
```

定时器触发时，回调在独立事务中执行。如果回调抛出异常，该定时器会被自动取消。

## Hot Reload 支持

Timer 支持热更新：通过 `scheduleOnlineHot` 系列方法注册的定时器，在热模块重新加载后会使用新的 Handle 类实例。非 Hot 版本的方法在热更新后仍使用旧的 Handle 实例。

## 与其他组件的关系

- **AutoKey**（[自增 ID](./autokey.md)）：Timer 使用 AutoKey 生成 timerId、nodeId 和 serialId。
- **DelayRemove**（[延迟删除](./delay-remove.md)）：Timer 使用 DelayRemove 清理空的 Node 数据。
