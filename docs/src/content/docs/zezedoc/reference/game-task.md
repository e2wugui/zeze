---
title: "Task 任务系统"
description: "Zeze 任务系统——基于 ConditionEvent/Condition 模型与持久化队列的任务触发与完成"
category: reference
order: 54
---

> 本文档描述 Zeze 任务系统的设计模型：`ConditionEvent` 事件触发、`Condition` 条件匹配、任务接受/完成/放弃流程与基于持久化集合的存储，供游戏任务业务开发检索参考。

## 模块定位

> ⚠️ **重要定位说明**：任务系统**不是 Zeze 框架的内置模块**。`Zeze.Builtin.Game/` 只内置了 Bag / Online / Rank 三个模块，**没有 Task**。本文描述的是 **MetaGame 示例项目**（`projects/MetaGame/`，包名 `metagame.Task`）中的任务系统——它是一个完整的**参考实现**，演示了如何基于 Zeze 的持久化集合与事务构建任务模型。业务可参考它，也可完全自研。

MetaGame 任务系统提供**条件-事件驱动**的任务模型：玩家行为产生 `ConditionEvent`，分派给正在进行的任务，匹配 `Condition` 条件，全部满足则任务完成。

| 核心概念 | 说明 |
|----------|------|
| **ConditionEvent** | 事件（`metagame.Task.ConditionEvent`），玩家行为触发（如杀怪、拾取），带 `name` 用于匹配 |
| **Condition** | 条件（`metagame.Task.Condition`），匹配事件并判断是否完成（`accept` / `isDone`） |
| **Task** | 任务，含若干 Condition，全部满足即完成 |
| **Phase** | 任务阶段，多阶段任务按顺序推进 |

---

## ConditionEvent：事件模型

任务系统使用的是 **`metagame.Task.ConditionEvent`**（**不是** `Zeze.Game.ConditionEvent`，两者类型不兼容，详见下方提示）。它通过 `name` 与条件匹配：

```java
package metagame.Task;

public class ConditionEvent {
    private final String name;
    private TaskModule taskModule;  // 分派上下文，由 TaskImpl.dispatch 设置

    public ConditionEvent(String name) { this.name = name; }
    public String getName() { return name; }
    public void setTaskModule(TaskModule taskModule) { this.taskModule = taskModule; }
    public TaskModule getTaskModule() { return taskModule; }
}
```

| 字段 | 说明 |
|------|------|
| `name` | 事件名，用于和 `Condition.getName()` 按名字相等匹配（也可为空，走 `instanceof` 识别） |

> ⚠️ **易混淆**：框架里另有一个 `Zeze.Game.ConditionEvent`（抽象类，带 `breakIfAccepted` 字段），但**任务系统并没有使用它**。MetaGame 用的是上面这个 `metagame.Task.ConditionEvent`（具体类，带 `name`）。两者不能互换。

### 自定义事件

业务自定义事件，可携带参数：

```java
// 击杀怪物事件
public class KillMonsterEvent extends ConditionEvent {
    private final int monsterId;
    private final int count;
    public KillMonsterEvent(String name, int monsterId, int count) {
        super(name);
        this.monsterId = monsterId;
        this.count = count;
    }
}
```

---

## Condition：条件接口

`Condition`（`metagame.Task.Condition`）定义条件契约，需实现 `Serializable` 以支持持久化：

```java
public interface Condition extends Serializable {
    String getName();
    boolean accept(ConditionEvent event);   // 是否匹配事件
    boolean isDone();                        // 是否完成

    // 任务完成时再次确认（如背包物品数量可能变动）
    default boolean finish(TaskModule module) { return true; }

    String getDescription();                 // 客户端显示描述
}
```

| 方法 | 说明 |
|------|------|
| `getName()` | 条件名，按名字匹配事件（可空） |
| `accept(event)` | 判断事件是否匹配，匹配则更新内部状态 |
| `isDone()` | 当前条件是否已满足 |
| `finish(module)` | 完成时确认（如扣减背包物品），失败返回 `false` |
| `getDescription()` | UI 显示文案（JSON） |

### 条件匹配模式

| 模式 | 说明 |
|------|------|
| 按名字相等 | 事件 `name` 与条件 `name` 相等即匹配 |
| 按类型识别 | 条件识别专门的事件类型（`instanceof`），此时名字为空 |

### 内置条件示例

| 条件 | 说明 |
|------|------|
| `ConditionKillMonster` | 击杀指定怪物 N 个 |
| `ConditionPickItem` | 拾取指定物品 N 个 |
| `ConditionBag` | 背包拥有指定物品 N 个（完成时扣减） |
| `ConditionNamedCount` | 命名计数条件 |
| `ConditionCompositeOr` | 组合条件（或） |

---

## 任务数据结构

任务数据持久化在 `tRoleTasks` 表中（`BRoleTasks`），每个角色一条记录：

| 结构 | 说明 |
|------|------|
| `BRoleTasks` | 角色任务集合（`roleId → BRoleTasks`） |
| `BTask` | 单个任务，含 `Phases` / `Conditions` / `IndexSet` / `TaskState` |
| `BCondition` | 条件 Bean，含 `ClassName` + `Parameter`（序列化的 Condition） |
| `BTaskConfig` | 任务静态配置 |

### 任务状态

| 常量 | 值 | 含义 |
|------|----|------|
| `eTaskAccepted` | 0 | 已接受（初始） |
| `eTaskDone` | 1 | 已完成（未发奖励） |

### 已完成任务追踪

```java
// 用 LinkedMap 记录已完成任务（持久化有序集合）
LinkedMap<EmptyBean> completed = linkedMapModule.open("Zeze.Game.Task.Completed." + roleId, EmptyBean.class);
```

---

## 任务流程

### 接受任务（Accept）

```java
protected long ProcessAcceptRequest(Accept r) throws Exception {
    var session = ProviderUserSession.get(r);
    var roleId = session.getRoleIdNotNull();
    return TaskImpl.acceptTask(this, roleId, r);
}
```

| 校验 | 说明 |
|------|------|
| 任务是否存在 | `TaskConfig` 查询 |
| 是否已接受 | 防重复接受 |
| 接受数量上限 | `maxAcceptedTaskCount`（默认 50） |
| 前置条件 | `checkTaskAcceptCondition` |

### 完成任务（Finish）

```java
protected long ProcessFinishRequest(Finish r) throws Exception {
    var session = ProviderUserSession.get(r);
    var roleId = session.getRoleIdNotNull();
    return TaskImpl.finishTask(this, roleId, r);
}
```

| 步骤 | 说明 |
|------|------|
| 校验状态 | 任务须为 `eTaskDone` |
| 确认条件 | 调用每个 Condition 的 `finish`（如扣减物品） |
| 发放奖励 | 按 `RewardConfig` 发放 |
| 标记完成 | 记入已完成 `LinkedMap` |

> `finish` 失败（如背包物品不足）时，任务状态回退为未完成。

### 放弃任务（Abandon）

```java
protected long ProcessAbandonRequest(Abandon r) throws Exception {
    var roleTasks = getRoleTasks(roleId);
    TaskImpl.abandonTask(this, roleId, roleTasks, r.Argument.getTaskId());
    r.SendResult();
    return 0;
}
```

### 自动完成

任务配置 `autoFinish` 时，条件满足后自动在新事务中完成：

```java
if (task.isAutoFinish()) {
    Task.run(zeze.newProcedure(
        () -> finishTask(module, roleId, task.getTaskId()), "autoFinishTask"));
}
```

> 自动完成在新事务中执行，因可能发生背包满等情况导致失败。

---

## 事件分派（dispatch）

服务器内部接口，玩家行为触发后调用：

```java
public void dispatch(long roleId, ConditionEvent event) throws Exception {
    TaskImpl.dispatch(this, roleId, event);
}
```

### 分派逻辑

| 步骤 | 说明 |
|------|------|
| 1. 遍历角色任务 | 逐个检查正在进行的任务 |
| 2. 阶段条件优先 | 多阶段任务先派发给当前阶段 |
| 3. 条件匹配 | `condition.accept(event)`，匹配则更新状态并持久化 |
| 4. 阶段完成 | 当前阶段所有条件满足（`IndexSet` 清空），推进下一阶段 |
| 5. 任务完成 | 所有阶段 + 条件完成，置 `eTaskDone`，自动完成则发奖励 |
| 6. 通知客户端 | `notifyTaskChanged` 推送任务变更 |

```java
// 业务触发：玩家击杀怪物
taskModule.dispatch(roleId, new KillMonsterEvent(monsterId, 1));
```

> 当前实现中，`TaskImpl.dispatch` 在派发完成后**固定 `return`**，事件只派发给当前角色的任务集合一次就结束（并非由 `ConditionEvent.breakIfAccepted` 控制——任务系统的 `ConditionEvent` 也没有这个字段）。

---

## 基于持久化集合

任务系统底层依赖 Zeze 持久化集合：

| 集合 | 用途 |
|------|------|
| `tRoleTasks`（Table） | 角色任务主存储 |
| `LinkedMap` | 已完成任务记录 |
| `Queue`（任务队列模式） | 待处理任务、奖励发放队列 |

> 所有操作在事务内，自动乐观锁与持久化。详见 [Queue 持久队列](./coll-queue.md)。

---

## 客户端协议

| 协议 | 说明 |
|------|------|
| `Accept` | 接受任务请求 |
| `Finish` | 完成任务请求 |
| `Abandon` | 放弃任务请求 |
| `GetRoleTasks` | 查询角色所有任务（返回描述列表） |
| `TaskChanged` | 任务变更通知（服务器推送） |
| `TaskRemoved` | 任务移除通知 |

### 任务描述（getDescription）

`Condition.getDescription()` 返回 JSON，供客户端 UI 渲染：

```java
@Override
public String getDescription() {
    // 最简结构：{ done: "true|false", des: "description" }
    return "pick " + itemId + " " + count + "/" + expected;
}
```

---

## 适用场景

| 场景 | 是否适用 | 说明 |
|------|----------|------|
| **主线/支线任务** | ✅ 适用 | 多阶段、多条件任务 |
| 日常任务 | ✅ 适用 | 击杀、拾取、计数条件 |
| 成就系统 | ✅ 适用 | 用 `LinkedMap` 记录已完成 |
| 活动任务 | ✅ 适用 | 动态配置 + 条件组合 |
| 复杂前置条件 | ✅ 适用 | `ConditionCompositeOr` 组合 |

---

## 注意事项

| 注意点 | 说明 |
|--------|------|
| **条件须可序列化** | `Condition` 实现 `Serializable`，状态持久化在 `BCondition.Parameter` |
| **`finish` 可能失败** | 背包物品可能变动，失败时任务状态回退 |
| **事件不再派发** | `TaskImpl.dispatch` 写死 `return`，事件只派发一次（非由 breakIfAccepted 控制） |
| **阶段条件限制** | 阶段条件不允许出现需 `finish` 的条件（如 `ConditionBag`） |
| **完成上限** | `maxAcceptedTaskCount` 默认 50，可配置 |
| **自动完成独立事务** | 避免背包满影响主流程 |

> 本文档描述任务系统的设计模型与参考实现（MetaGame 示例）。具体类名、协议字段以实际项目 SDK 为准。

---

## 相关文档

- [Queue 持久队列](./coll-queue.md) — 任务队列的持久化基础
- [游戏模块总览](./game-overview.md) — Game 模块整体架构
- [LinkedMap 有序映射](./coll-linked-map.md) — 已完成任务记录
