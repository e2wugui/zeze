# Timer 使用文档

## 概述

`Timer` 是 Zeze 框架提供的**持久化分布式定时器**组件，支持简单定时器和 Cron 表达式定时器。所有定时器数据都会持久化到数据库，支持服务宕机后的自动迁移和恢复。

## 包路径
```
Zeze.Component.Timer
```

## 核心特性

| 特性 | 说明 |
|------|------|
| 持久化 | 定时器数据自动同步到数据库 |
| 分布式 | 支持多服务器部署，宕机自动迁移 |
| 事务安全 | 回调在事务中执行，乐观锁保证并发安全 |
| 多种类型 | 支持简单定时器、周期定时器、Cron表达式定时器 |
| 命名定时器 | 支持全局唯一命名的定时器 |
| 在线/离线定时器 | 支持与用户在线状态绑定的定时器 |
| 热更新支持 | 支持 TimerHandle 的热更新 |

---

## 快速开始

### 1. 创建和启动 Timer

```java
// Timer 通过 Zeze.Application 创建
Zeze.Application zeze = new Zeze.Application(config);
Timer timer = Timer.create(app);

// 启动 Timer 服务（非事务环境调用）
timer.start();
```

### 2. 定义 TimerHandle

```java
// 实现定时器回调接口
public class MyTimerHandle implements TimerHandle {
    @Override
    public void onTimer(TimerContext context) throws Exception {
        // 在事务内运行，如果抛出异常，会自动取消该 timer
        System.out.println("Timer triggered: " + context.timerId);
        System.out.println("Trigger times: " + context.happenTimes);

        // 获取自定义数据
        MyCustomData data = (MyCustomData)context.customData;

        // 可以修改下次触发时间（仅 SimpleTimer 有效）
        // context.nextExpectedTimeMills = System.currentTimeMillis() + 5000;
    }

    // 可选：定时器被取消时的回调
    @Override
    public void onTimerCancel(BTimer timer) throws Exception {
        // 在事务内运行，时机在取消后
        // 允许在这里"复活"该 timerId 的定时器
    }
}
```

### 3. 调度定时器

```java
// 在事务内调度
zeze.newProcedure(() -> {
    // 一次性定时器（延迟 5 秒后执行）
    String timerId1 = timer.schedule(5000, MyTimerHandle.class, null);

    // 周期定时器（延迟 1 秒后开始，每 5 秒执行一次）
    String timerId2 = timer.schedule(1000, 5000, MyTimerHandle.class, null);

    // 带自定义数据
    MyCustomData customData = new MyCustomData();
    customData.playerId = 12345;
    String timerId3 = timer.schedule(5000, MyTimerHandle.class, customData);

    return 0;
}, "schedule_timer").call();
```

---

## 定时器类型

### 1. 自动命名定时器 (Auto Named Timer)

每次调用都会创建新的定时器实例，内部自动生成唯一 ID（以 `@` 开头）。

```java
// 一次性定时器
String timerId = timer.schedule(long delay, TimerHandle.class, Bean customData);

// 周期定时器
String timerId = timer.schedule(long delay, long period, TimerHandle.class, Bean customData);

// 限制次数的周期定时器
String timerId = timer.schedule(long delay, long period, long times, TimerHandle.class, Bean customData);

// 完整参数
String timerId = timer.schedule(
    long delay,           // 首次触发延迟(毫秒)，不能小于0
    long period,          // 触发周期(毫秒)，只有大于0才会周期触发
    long times,           // 限制触发次数，-1表示不限次数
    long endTime,         // 限制触发的最后时间(unix毫秒时间戳)，<=0不限制
    int missfirePolicy,   // 错过触发时间的处理策略
    TimerHandle.class,    // 回调处理类
    Bean customData,      // 自定义数据（可为null）
    String oneByOneKey    // 串行执行key（可选）
);
```

### 2. 命名定时器 (Named Timer)

全局唯一，每个名字只能有一个定时器实例。

```java
// 如果同名定时器已存在，返回 false
boolean success = timer.scheduleNamed(
    String timerId,       // 自定义名字，不能以 '@' 开头
    long delay,
    long period,
    long times,
    long endTime,
    int missfirePolicy,
    TimerHandle.class,
    Bean customData
);
```

### 3. Cron 定时器

使用 Cron 表达式定义触发时间。

```java
// 每天 8:30:00 执行
String timerId = timer.scheduleDay(8, 30, 0, MyTimerHandle.class, null);

// 每周一 9:00:00 执行
String timerId = timer.scheduleWeek(1, 9, 0, 0, MyTimerHandle.class, null);

// 每月 1 号 0:00:00 执行
String timerId = timer.scheduleMonth(1, 0, 0, 0, MyTimerHandle.class, null);

// 自定义 Cron 表达式
String timerId = timer.schedule("0 30 8 * * ?", MyTimerHandle.class, null);
```

**Cron 表达式格式**：`秒 分 时 日 月 周`

```
"0 0 12 * * ?"        // 每天中午12点
"0 15 10 ? * *"       // 每天上午10:15
"0 15 10 * * ?"       // 每天上午10:15
"0 15 10 * * ? *"     // 每天上午10:15
"0 15 10 * * ? 2024"  // 2024年每天上午10:15
"0 * 14 * * ?"        // 每天下午2点到2:59的每分钟
"0 0/5 14 * * ?"      // 每天下午2点到2:55每5分钟
"0 0/5 14,18 * * ?"   // 下午2点到2:55和6点到6:55每5分钟
"0 0-5 14 * * ?"      // 下午2点到2:05每分钟
"0 10,44 14 ? 3 WED"  // 3月每周三下午2:10和2:44
"0 15 10 ? * MON-FRI" // 周一到周五上午10:15
"0 15 10 15 * ?"      // 每月15号上午10:15
```

### 4. 在线定时器 (Online Timer)

与用户在线状态绑定，用户下线时自动取消。

```java
// 通过 TimerRole (roleId)
TimerRole roleTimer = timer.getRoleTimer();

// 调度在线定时器（仅在线时有效）
String timerId = roleTimer.scheduleOnline(
    long roleId,
    long delay,
    long period,
    long times,
    long endTime,
    TimerHandle.class,
    Bean customData
);

// 命名在线定时器
boolean success = roleTimer.scheduleOnlineNamed(
    long roleId,
    String timerId,
    long delay,
    ...
);
```

**在线定时器特性**：
- 仅在在线时允许注册
- 非持久化，存储在内存中
- 用户下线时自动失效
- 支持跨服务器迁移

### 5. 离线定时器 (Offline Timer)

与用户离线状态绑定，用户上线时自动取消。

```java
// 调度离线定时器（仅离线时有效，持久化）
String timerId = roleTimer.scheduleOffline(
    long roleId,
    long delay,
    long period,
    long times,
    long endTime,
    int missfirePolicy,
    TimerHandle.class,
    Bean customData
);
```

**离线定时器特性**：
- 仅在离线时允许注册
- 持久化存储
- 数量有限制（由配置决定）
- 用户上线时自动取消

---

## Missfire 策略

当定时器错过了预定的触发时间（如服务器宕机重启后），可以选择不同的处理策略：

| 策略常量 | 值 | 说明 |
|----------|-----|------|
| `eMissfirePolicyNothing` | 0 | 不处理错过的触发，按原计划继续调度 |
| `eMissfirePolicyRunOnce` | 1 | 立即执行一次，然后以当前时间重新计算下次触发 |
| `eMissfirePolicyRunOnceOldNext` | 2 | 立即执行一次，保持原来的下次触发时间 |

---

## API 参考

### Timer 主要方法

#### 调度方法

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `schedule(delay, handleClass, customData)` | `String` | 调度一次性定时器 |
| `schedule(delay, period, handleClass, customData)` | `String` | 调度周期定时器 |
| `schedule(delay, period, times, endTime, missfirePolicy, handleClass, customData, oneByOneKey)` | `String` | 完整参数调度 |
| `schedule(cronExpression, handleClass, customData)` | `String` | Cron 表达式定时器 |
| `scheduleDay(hour, minute, second, handleClass, customData)` | `String` | 每天定时器 |
| `scheduleWeek(weekDay, hour, minute, second, handleClass, customData)` | `String` | 每周定时器 |
| `scheduleMonth(monthDay, hour, minute, second, handleClass, customData)` | `String` | 每月定时器 |

#### 命名定时器方法

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `scheduleNamed(timerId, delay, ...)` | `boolean` | 调度命名定时器，已存在返回 false |

#### 管理方法

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `cancel(timerId)` | `void` | 取消定时器 |
| `getTimer(timerId)` | `BTimer` | 获取定时器信息 |
| `getTimerIndex(timerId)` | `BIndex` | 获取定时器索引 |
| `start()` | `void` | 启动 Timer 服务 |
| `stop()` | `void` | 停止 Timer 服务 |

### TimerContext 属性

| 属性 | 类型 | 说明 |
|------|------|------|
| `timer` | `Timer` | 所属的 Timer 模块 |
| `timerId` | `String` | 定时器 ID |
| `timerName` | `String` | 处理类完整类名 |
| `customData` | `Bean` | 创建时指定的自定义数据 |
| `happenTimes` | `long` | 已触发次数（首次触发时是 1） |
| `expectedTimeMills` | `long` | 本次应该触发的时间（unix 毫秒） |
| `nextExpectedTimeMills` | `long` | 下次计划触发时间（可修改） |
| `roleId` | `long` | 所属角色 ID（仅 TimerRole） |
| `account` | `String` | 所属账号（仅 TimerAccount） |
| `clientId` | `String` | 所属客户端 ID（仅 TimerAccount） |

### TimerHandle 接口

```java
public interface TimerHandle {
    // 必须实现：定时器触发时的回调
    void onTimer(TimerContext context) throws Exception;

    // 可选：定时器被取消时的回调
    default void onTimerCancel(BTimer timer) throws Exception {}
}
```

---

## 使用示例

### 示例1：简单定时任务

```java
public class SimpleTaskHandle implements TimerHandle {
    @Override
    public void onTimer(TimerContext context) throws Exception {
        System.out.println("Task executed at: " + new Date());
    }
}

// 在事务中调度
zeze.newProcedure(() -> {
    timer.schedule(5000, SimpleTaskHandle.class, null);
    return 0;
}, "schedule").call();
```

### 示例2：玩家 BUFF 过期

```java
public class BuffExpireHandle implements TimerHandle {
    @Override
    public void onTimer(TimerContext context) throws Exception {
        BuffData data = (BuffData)context.customData;
        // 移除玩家 BUFF
        removeBuff(data.playerId, data.buffId);
    }
}

public class BuffData extends Bean {
    public long playerId;
    public int buffId;
}

// 添加 BUFF 时调度过期定时器
public void addBuff(long playerId, int buffId, int durationMs) {
    var data = new BuffData();
    data.playerId = playerId;
    data.buffId = buffId;

    String timerId = "buff_" + playerId + "_" + buffId;
    timer.scheduleNamed(timerId, durationMs, BuffExpireHandle.class, data);
}
```

### 示例3：每日重置任务

```java
public class DailyResetHandle implements TimerHandle {
    @Override
    public void onTimer(TimerContext context) throws Exception {
        // 每天凌晨 4 点执行重置
        resetDailyTasks();
    }
}

// 应用启动时注册
zeze.newProcedure(() -> {
    timer.scheduleDay(4, 0, 0, DailyResetHandle.class, null);
    return 0;
}, "init").call();
```

### 示例4：在线心跳检测

```java
public class HeartbeatHandle implements TimerHandle {
    @Override
    public void onTimer(TimerContext context) throws Exception {
        long roleId = context.roleId;
        // 检查玩家是否仍然在线
        if (isOnline(roleId)) {
            sendHeartbeat(roleId);
        }
        // 如果不需要继续，设置下次时间为 0
        // context.nextExpectedTimeMills = 0;
    }
}

// 在玩家登录事务中
public void onLogin(long roleId) {
    var roleTimer = timer.getRoleTimer();
    // 每 30 秒发送心跳
    roleTimer.scheduleOnline(roleId, 30000, 30000, -1, -1, HeartbeatHandle.class, null);
}
```

### 示例5：离线奖励通知

```java
public class OfflineRewardHandle implements TimerHandle {
    @Override
    public void onTimer(TimerContext context) throws Exception {
        RewardData data = (RewardData)context.customData;
        // 发送离线奖励邮件
        sendMail(data.roleId, "离线奖励", data.rewardItems);
    }
}

// 在玩家登出事务中
public void onLogout(long roleId) {
    var roleTimer = timer.getRoleTimer();
    var data = new RewardData();
    data.roleId = roleId;
    data.rewardItems = calculateOfflineReward(roleId);

    // 1 小时后发放离线奖励
    roleTimer.scheduleOffline(roleId, 3600000, -1, -1, -1,
        Timer.eMissfirePolicyNothing, OfflineRewardHandle.class, data);
}
```

---

## 内部实现

### 存储结构

```
BNodeRoot (每个 ServerId 一个)
├── headNodeId   // 头节点 ID
├── tailNodeId   // 尾节点 ID
├── version      // 应用版本
└── loadSerialNo // 加载序列号

BNode (双向循环链表节点)
├── prevNodeId   // 前驱节点
├── nextNodeId   // 后继节点
└── timers[]     // 定时器映射表（每个节点最多 CountPerNode 个）

BTimer (定时器数据)
├── timerName    // 定时器 ID
├── handleName   // 处理类名
├── timerObj     // BSimpleTimer 或 BCronTimer
└── customData   // 自定义数据

BIndex (索引表)
├── serverId     // 所属服务器
├── nodeId       // 所在节点 ID
├── serialId     // 序列号
└── version      // 应用版本
```

### 分布式故障转移

1. 每个 Server 启动时，通过 `ServiceManager` 注册
2. 当检测到其他 Server 宕机时，触发 `spliceLoadTimer`
3. 将宕机 Server 的定时器链表"拼接"到当前 Server
4. 重新调度接管的定时器

---

## 注意事项

1. **事务要求**：所有调度和取消操作必须在 `Procedure` 中执行
2. **TimerId 限制**：命名定时器的 ID 不能以 `@` 开头（保留给自动命名）
3. **Handle 类要求**：`TimerHandle` 实现类必须有无参构造函数，不能有状态（仅持久化类名）
4. **异常处理**：`onTimer` 抛出异常会自动取消定时器
5. **热更新**：使用带 `Hot` 后缀的方法（如 `scheduleOnlineHot`）支持处理类的热更新
6. **在线定时器**：非持久化，仅存在内存中，服务器重启后丢失
7. **离线定时器**：有数量限制，超出限制会记录错误日志但仍会创建
8. **并发安全**：使用 `concurrentSerialNo` 保证同一定时器在多 Server 竞争时只触发一次

---

## 源码位置

- `ZezeJava/ZezeJava/src/main/java/Zeze/Component/Timer.java`
- `ZezeJava/ZezeJava/src/main/java/Zeze/Component/TimerHandle.java`
- `ZezeJava/ZezeJava/src/main/java/Zeze/Component/TimerContext.java`
- `ZezeJava/ZezeJava/src/main/java/Zeze/Component/TimerRole.java`
- `ZezeJava/ZezeJava/src/main/java/Zeze/Component/TimerAccount.java`
