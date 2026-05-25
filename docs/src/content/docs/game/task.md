---
title: 任务系统
sidebar:
  order: 4
---

Zeze 提供了基于 **ConditionEvent** 条件事件的任务系统框架。任务系统通过事件驱动模型，在游戏逻辑中触发条件更新，由任务系统检查完成状态。由于任务系统关联广泛，应用时需要进一步开发适配。

## 整体架构

```
任务系统 (Task)
  ├─ 奖励系统 (Reward)
  ├─ 任务配置 (TaskGraphics)
  ├─ 任务条件 (Condition)
  └─ 任务情节 (Episode)

关联系统：
  地图系统 (World) → 区域事件、NPC 视野
  背包系统 (Bag)  → 物品收集条件
```

## ConditionEvent 条件事件

`ConditionEvent` 是所有任务条件的抽象基类：

```java
public abstract class ConditionEvent {
    private final boolean breakIfAccepted;

    public ConditionEvent(boolean breakIfAccepted) {
        this.breakIfAccepted = breakIfAccepted;
    }

    public final boolean isBreakIfAccepted() {
        return breakIfAccepted;
    }
}
```

**breakIfAccepted** 控制条件链的执行行为：`false`（默认）继续检查后续条件；`true` 在当前条件满足时中断后续检查，用于实现"完成任意一个即算完成"的 Or 逻辑。

## Condition 条件接口

```java
public interface Condition extends Serializable {
    String getName();
    boolean accept(ConditionEvent event);
    boolean isDone();

    // 任务完成时调用，某些条件需要再次确认 isDone
    default boolean finish() { return true; }

    // 描述信息，JSON 格式，如 { "done": "false", "des": "description" }
    String getDescription();
}
```

任务拥有多个条件，所有条件满足时任务完成。条件和事件的匹配有两种模式：按名字匹配，或用 `instanceof` 匹配事件类型。

### 内置条件实现

| 条件 | 说明 |
|------|------|
| `ConditionKillMonster` | 杀死指定怪物达到数量 |
| `ConditionPickItem` | 拾取指定物品达到数量 |
| `ConditionBag` | 背包内指定物品达到数量 |
| `ConditionNamedCount` | 命名计数条件，收到同名事件即计数 |
| `ConditionCompositeOr` | 多条件组合，任一完成即完成 |

## 自定义条件事件

```java
// 击杀怪物事件
public class KillMonsterEvent extends ConditionEvent {
    private final int monsterId;
    private final int count;

    public KillMonsterEvent(int monsterId, int count) {
        this.monsterId = monsterId;
        this.count = count;
    }
}

// 在游戏逻辑中触发
public void onMonsterKilled(Player player, int monsterId) {
    player.getTaskModule().fireEvent(new KillMonsterEvent(monsterId, 1));
}
```

## Reward 奖励系统

```java
public interface Reward {
    int getRewardId();
    int getRewardType();
    Binary getRewardParam(long roleId);
    void reward(long roleId);
}
```

实现各种奖励方式并注册到 `RewardConfig` 中，包括固定物品、权重随机、职业相关、Buf、启动剧情等。

```java
taskModule.getRewardConfig().put(new MyReward());
```

## 配置与扩展

- **TaskGraphics**：任务配置后端存储，应用可自行决定编辑器，导入数据到 TaskGraphics
- **World 地图系统**：需支持区域进入/退出事件订阅、NPC 视野管理
- **Bag 背包系统**：需支持物品数量查询和删除操作

## 关联文档

- [背包系统](./bag/)：物品收集类任务依赖背包模块
- [排行榜](./rank/)：排名类任务需要查询排行数据
- [事务](../core/transaction/)：使用 Procedure 包装任务操作保证数据一致性
