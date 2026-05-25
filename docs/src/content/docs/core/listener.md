---
title: "ChangeListener 数据变更监听"
sidebar:
  order: 5
---

**ChangeListener** 是 Zeze 提供的 Table 级别数据变更回调机制。当事务提交后表中记录发生插入、修改或删除时，已注册的监听器会收到通知，携带变更的键和详细日志。不管修改操作如何变化，只要数据定义不变，监听逻辑就无需调整。此功能最常见的用途是同步数据给客户端。

## 接口与变更类型

ChangeListener 是一个函数式接口：

```java
@FunctionalInterface
public interface ChangeListener {
    void OnChanged(Object key, Changes.Record r);
}
```

回调参数中 `key` 为记录的主键，`Changes.Record` 包含变更细节，通过 `getState()` 获取变更类型：

| 状态常量 | 值 | 含义 |
|---|---|---|
| `Changes.Record.Put` | 1 | 记录被插入或整个替换，`getValue()` 返回新值 |
| `Changes.Record.Edit` | 2 | 记录被增量修改，日志描述了哪些字段发生了变化 |
| `Changes.Record.Remove` | 0 | 记录被删除 |

## 注册与移除

每张表内置一个 **ChangeListenerMap**，用于管理该表的监听器集合：

```java
// 注册监听器
table.getChangeListenerMap().addListener(listener);

// 移除监听器
table.getChangeListenerMap().removeListener(listener);

// 查询是否已注册
table.getChangeListenerMap().hasListener();
```

ChangeListenerMap 内部使用读写分离策略：写操作加锁，读操作访问不可变快照，保证在事务收集日志阶段和通知阶段使用同一份监听器快照，避免因注册变动造成不一致。

## 与事务的关系

ChangeListener 的触发时机在事务 **finalCommit** 阶段——事务已提交成功、状态置为 Completed 之后，在 `whileCommit` 回调的同一阶段执行。关键特性：

- **事务内触发**：监听器回调发生在事务提交成功之后，属于事务生命周期的一部分。回调中不应再对同一张表执行写操作。
- **非事务上下文**：在事务外直接修改数据时，不会触发 ChangeListener。ChangeListener 严格绑定于事务提交流程。

如果需要在监听器中执行后续逻辑（如网络推送），可以在 `OnChanged` 中直接进行，框架会捕获回调中的异常并记录日志，不会影响其他监听器或事务状态。

## 变更日志结构

当变更类型为 `Edit` 时，`Changes.Record` 携带增量日志，可通过 `getVariableLog(variableId)` 获取指定变量的变更详情。各类型的日志结构如下：

| 日志类型 | 说明 |
|---|---|
| `LogBean` | Bean 的修改日志，包含 `Variables` 映射（variableId -> Log） |
| `LogList1` / `LogList2` | 列表操作日志，List2 还包含列表项的 Bean 变更 |
| `LogMap1` | Map 的替换和删除日志：`Replaced`、`Removed` |
| `LogMap2` | Map 的替换、删除和项内变更：`Replaced`、`Removed`、`Changed` |
| `LogSet1` | Set 的新增和删除：`Added`、`Removed` |
| 简单类型日志 | 如 int、long 等，包含新值 `Value` |

## 完整示例

以下示例展示监听角色背包变更，并将增量数据打包发送给客户端：

```java
public class ItemsChangeListener implements ChangeListener {
    @Override
    public void OnChanged(Object key, Changes.Record r) {
        long roleId = (Long) key;
        switch (r.getState()) {
        case Changes.Record.Put:
            // 整条记录被替换，发送全量数据
            sendFullToClient(roleId, r.getValue());
            break;
        case Changes.Record.Edit:
            // 增量变化，仅发送修改的部分
            var noteMap = (LogMap2<Integer, BItem>) r.getVariableLog(BRole.VAR_Items);
            if (noteMap != null) {
                // 将 Changed 合并到 Replaced，简化客户端处理
                noteMap.MergeChangedToReplaced();
                sendDeltaToClient(roleId, noteMap.getReplaced(), noteMap.getRemoved());
            }
            break;
        case Changes.Record.Remove:
            // 记录被删除
            sendRemoveToClient(roleId);
            break;
        }
    }
}

// 注册到背包表
_roleTable.getChangeListenerMap().addListener(new ItemsChangeListener());
```

## 客户端处理

客户端收到服务端推送的变更协议后，按变更类型处理本地缓存：

```java
switch (notify.getChangeTag()) {
case Put:
    localMap.clear();
    localMap.putAll(notify.getReplaced());
    break;
case Edit:
    localMap.putAll(notify.getReplaced());
    localMap.removeAll(notify.getRemoved());
    break;
case Remove:
    localMap = null;
    break;
}
```

## 数据同步模式

ChangeListener 最常用于客户端数据同步。以下是几种典型模式：

### 全量推送

服务端检测到变更后，将整条记录打包 Push 给客户端。结构清晰、实现简单，推荐优先采用。

### 增量推送

客户端先 Get 一次完整数据，之后服务端仅推送增量变更日志。需要注意两个问题：

1. **Get 与增量的原子性**：Get 和后续增量推送之间可能存在变更丢失。
2. **消息丢失**：网络不可靠可能导致增量消息丢失。

推荐方案：在数据中维护一个版本号，每次修改递增；推送时携带版本号；客户端发现版本不连续时重新 Get。

### Relogin 差异同步

玩家断线重连时，可以利用 ChangeListener 记录离线期间的变更日志。客户端重连后只需同步差异部分，避免全量数据传输。

## 分布式注意事项

分布式环境下每台 Server 实例各自注册 ChangeListener，只有实际执行修改的那台实例会触发回调。因此监听器适合同步**个人数据**（如角色背包），不太适合同步**共享数据**（如群成员列表）。如果确实需要同步共享数据，可考虑配合广播机制使用。

## 使用限制建议

**除非逻辑接近事件这种模型，否则不要用监听器实现逻辑。** Listener 适合同步个人数据，不适合更新共享数据（如群成员列表变化——虽然可以发 notify 广播，但有点浪费）。

## 数据同步模式推荐

- **Get 模式**（大大的推荐！）——客户端主动拉取完整数据
- **Push 完整数据**（大大的推荐！）——服务端推送完整 Bean 数据
- **Push 不完备增量更新**（限制使用！迫不得已时采用）——推送部分变更，不保证完备
- **Push 完备增量更新**（暂时不推荐使用）——完备增量同步

## 客户端数据获取时机

典型时序：Auth 验证 → Login 登录 → Map.EnterWorld 进入地图场景 → UI 显示时 → 其他需要数据的时候。对于客户端数据存储管理，建议模块自己管理（当模块需要热拔插时尤其需要这种）。
