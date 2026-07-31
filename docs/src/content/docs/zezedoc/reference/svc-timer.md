---
title: "Timer 定时器"
description: "Zeze 持久化分布式定时器：Auto/Named/Online/Offline 四类调度语义"
category: reference
order: 30
---

> 本文档说明 Zeze 的持久化分布式定时器组件，覆盖 Auto（简单/Cron）、Named（全局唯一命名）、Online（绑定在线用户）、Offline（绑定下线用户）四类调度的语义、生命周期与一致性保证，供业务调度参考。

## 概述

Zeze 的 Timer 是**持久化、分布式**的定时器组件，支持简单定时、周期定时和 Cron 表达式。底层调度统一走 `Task.scheduleUnsafe`（而非 JDK 的 `ScheduledThreadPoolExecutor`），这样定时任务的执行会进入 Zeze 的任务调度体系，受线程池容量与事务上下文管理。其分布式特性：定时器注册后，一般运行在注册所在的 Server 实例；若该 Server 非法宕机，调度会被转移到其他 Server 继续执行。

| 特性 | 说明 |
|------|------|
| 持久化 | ✅ Auto/Named/Offline 重启后继续调度 |
| 分布式 | ✅ 注册所在 Server 执行；宕机后调度到其他 Server |
| 调度底层 | `Task.scheduleUnsafe`（进入 Zeze 任务调度体系） |
| Cron | ✅ 支持 6 字段 cron 表达式 |

> ⚠️ **注意：`schedule` 必须传回调类型**。所有 `schedule` 重载都需要一个 `Class<? extends TimerHandle>`（回调类）和一个 `Bean customData`（自定义数据，不需要时传 `null`），返回 `String` 类型的 `timerId`，而**不是**简单的 `long`。如果照搬"传个 delay 就返回 id"的写法，代码无法编译。

## Auto Timer（自动命名）

以 `@` 开头自动命名（框架内部 `AutoKey` 分配），调用 `Timer.schedule(...)` 的重载。**所有重载返回 `String`（即 `timerId`，形如 `@xxxxx`）**：

| 方法（精简形式） | 语义 |
|------|------|
| `schedule(delay, handleClass, customData)` | 延迟 `delay` 后执行**一次** |
| `schedule(delay, period, handleClass, customData)` | 延迟 `delay` 后按 `period` 间隔**重复执行** |
| `schedule(delay, period, times, handleClass, customData)` | 重复 `times` 次 |
| `schedule(delay, period, times, endTime, handleClass, customData)` | 重复 `times` 次或到 `endTime` 为止 |
| `schedule(..., missfirePolicy, handleClass, customData)` | 带 missfire 策略 |
| `schedule(..., oneByOneKey, handleClass, customData)` | 带串行 key（保证单 key 顺序执行） |
| `schedule(cronExpression, handleClass, customData)` | 6 字段 cron 表达式，周期执行 |

此外提供按"时刻"配置的便捷重载（内部转为 cron）：

| 便捷方法 | 语义 |
|----------|------|
| `scheduleDay(hour, minute, second, handleClass, customData)` | 每天 `hour:minute:second` 执行 |
| `scheduleWeek(weekDay, hour, minute, second, handleClass, customData)` | 每周指定星期 |
| `scheduleMonth(monthDay, hour, minute, second, handleClass, customData)` | 每月指定日 |

```java
// 回调类：实现 TimerHandle
public class MyTimer implements TimerHandle {
    @Override
    public void onTimer() throws Exception {
        System.out.println("timer fired");
    }
}

// 延迟 5 秒执行一次（返回 timerId，String 类型）
String timerId = timer.schedule(5000, MyTimer.class, null);

// 延迟 1 秒后每 10 秒执行一次
String timerId2 = timer.schedule(1000, 10000, MyTimer.class, null);

// 每天 3:00:00 执行
String timerId3 = timer.scheduleDay(3, 0, 0, MyTimer.class, null);

// Cron 表达式（6 字段）：秒 分 时 日 月 周，每天 3:00 执行
String timerId4 = timer.schedule("0 0 3 * * ?", MyTimer.class, null);
```

> Cron 表达式为 6 字段：`秒 分 时 日 月 周`（`秒` 在最前），不是传统 crontab 的 5 字段。

特点：
- 自动命名，名称以 `@` 开头
- 持久化，重启继续调度
- 分布式，注册所在 Server 执行，宕机后调度到其他 Server

## Named Timer（命名定时器）

用**固定名字**注册，方法族是 `scheduleNamed(...)`，返回 `boolean` 表示**调度是否成功**。与 Auto 的区别在于：每个名字全局只有一份实例。

```java
// 用固定名字注册，保证全局唯一
boolean ok = timer.scheduleNamed("DailyReset", 0, 86400000, MyTimer.class, null);
if (!ok) {
    // 该名字已被其他 Server 占用
}
```

### "已存在返回 false" 的确切语义

`scheduleNamed` 返回 `false` **仅当名字已被其他 Server 占用**。本 Server 内如果已存在同名定时器，框架会**先取消再重建**（参数完全相同时则跳过重建直接返回 `true`），返回 `true`：

| 场景 | 行为 | 返回值 |
|------|------|--------|
| 名字无人占用 | 新建 | `true` |
| 名字被**其他 Server** 占用 | 不覆盖 | `false` |
| 名字被**本 Server** 占用，参数有变 | 取消旧的、重建 | `true` |
| 名字被**本 Server** 占用，参数完全相同 | 跳过（cron 参数一致时） | `true` |

> ⚠️ 命名定时器的名字**不允许**以 `@` 开头（`@` 是 Auto Timer 的保留前缀，传入会抛 `IllegalArgumentException`）。

特点：
- 全局唯一，每个名字一份实例
- 一般运行在注册所在 Server，但可能被调度到其他 Server

## Online / Offline Timer（绑定用户）

这两类定时器**不在 `Timer` 类上**，而在 **`TimerAccount`** 类（通过 `timer.getAccountTimer()` 获取）。方法名是小写的 `scheduleOnline` / `scheduleOffline`（以及 `*Named` / `*Hot` 变体）：

```java
TimerAccount accountTimer = timer.getAccountTimer();
accountTimer.scheduleOnline(...);   // 在线定时器
accountTimer.scheduleOffline(...);  // 下线定时器
```

### Online Timer（在线定时器）

与用户绑定（支持 account 或 roleId），**仅在线时生效**。

| 特性 | 说明 |
|------|------|
| 注册限制 | 仅**在线**允许注册，下线全部失效 |
| 持久化 | ❌ 非持久化，存于内存（语言自带调度器） |
| 生命期 | 跟随 `ModuleOnline.LocalData` |
| 数量 | 不限 |
| 存在位置 | 仅存在登录所在 Server |
| 一致性保证 | ServerA 未正常下线又登录 ServerB → B 向 A 发 Kick → A 的 online-timer 结束，最终保证 |

### Offline Timer（下线定时器）

与用户绑定，**仅下线时生效**，一般在登出事件注册。

| 特性 | 说明 |
|------|------|
| 注册限制 | 仅**下线**允许注册，上线全部失效；一般在登出事件注册 |
| 持久化 | ✅ 持久化 |
| 数量 | 有限，每用户一个 Offline Bean |
| 持续性 | ❌ 不能一直持续，需有**次数或时间限制** |
| Server 限制 | 只允许一台 Server 下线 |
| 生命期 | 与 `LocalData` 相反：Offline Bean 在**登录事件**中删除（内嵌登录事务保持一致） |

### 取消与一致性

Offline Timer 的取消通过 `@Redirect` 通知其他 Server，并需在 Offline Timer 中记录 `Login.Version`，触发定期回调检查版本号是否一致来判定是否需要取消对应的 ThreadPool 任务。

```
登录事件：删除 Offline Bean（内嵌登录事务）
         ↓
Offline Timer 记录 Login.Version
         ↓
定期回调检查版本号 → 一致则保持，不一致则取消 ThreadPool 任务
```

## 四类定时器对比

| 维度 | Auto | Named | Online | Offline |
|------|------|-------|--------|---------|
| 所在类 | `Timer` | `Timer` | `TimerAccount` | `TimerAccount` |
| 命名 | 自动（`@`开头） | 全局唯一名字（非 `@`） | 绑定用户 | 绑定用户 |
| 返回值 | `String`（timerId） | `boolean`（是否成功） | — | — |
| 持久化 | ✅ | ✅ | ❌ | ✅ |
| 生命期 | 自身 | 自身 | `LocalData` | 与 `LocalData` 相反 |
| 数量 | 不限 | 每名字一份 | 不限 | 每用户一个 Offline Bean |
| 触发条件 | 时间到达 | 时间到达 | 在线 | 下线 |

## 相关文档

- 在线管理：[./arch-online.md](./arch-online.md)
- 事务参考：[./transaction.md](./transaction.md)
- AutoKey（自动命名底层）：[./svc-autokey.md](./svc-autokey.md)
