---
title: "ChangeListener 数据变更监听"
description: "Zeze Table 数据变更监听机制、Changes.Record 结构与数据同步模式速查"
category: reference
order: 6
---

本文是 Zeze **ChangeListener** 的完整 API 参考——覆盖监听接口、Changes.Record 变更结构、增量日志、注册移除、与事务的关系和数据同步模式，供写代码时随查随用。Table 接口见 [Table 存储接口](./table.md)，事务触发时机见 [事务系统](./transaction.md)。

## 什么是 ChangeListener

ChangeListener 是 **Table 级**的数据变更回调机制。事务提交后，表中记录发生插入、修改、删除时，已注册的监听器会收到通知，携带键和详细变更日志。**最常用于同步数据给客户端**。

### 接口定义

```java
@FunctionalInterface
public interface ChangeListener {
    void OnChanged(Object key, Changes.Record r);
}
```

| 参数 | 说明 |
|------|------|
| `key` | 主键 |
| `r` | `Changes.Record`，含变更细节 |

---

## Changes.Record 变更类型

通过 `r.getState()` 获取变更类型：

| 状态 | 值 | 说明 | 附加方法 |
|------|----|------|----------|
| `Put` | 1 | 插入或整个替换 | `getValue()` 取新值 |
| `Edit` | 2 | 增量修改，日志描述哪些字段变化 | `getVariableLog(id)` 等 |
| `Remove` | 0 | 删除 | |

```java
ChangeListener listener = (key, r) -> {
    switch (r.getState()) {
        case Changes.Record.Put:
            Object newValue = r.getValue();   // 整个新值
            break;
        case Changes.Record.Edit:
            // 增量日志，见下节
            break;
        case Changes.Record.Remove:
            // 记录被删除
            break;
    }
};
```

---

## 增量日志结构（Edit 时）

`Edit` 状态下 `Changes.Record` 携带增量日志，通过 `getVariableLog(variableId)` 取得：

| 日志类型 | 适用容器 | 说明 |
|----------|----------|------|
| `LogBean` | Bean | Bean 修改，Variables 映射 `variableId -> Log` |
| `LogList1` | List | List 增量 |
| `LogList2` | List | List 增量（含项内 Bean 变更） |
| `LogMap1` | Map | Map 替换（`Replaced`）/ 删除（`Removed`） |
| `LogMap2` | Map | Map 替换 / 删除（项内 `Changed`） |
| `LogSet1` | Set | Set 新增（`Added`）/ 删除（`Removed`） |
| 简单类型日志 | 基本类型字段 | 含新值（`Value`） |

```java
case Changes.Record.Edit:
    // 取某字段的增量日志
    LogBean log = (LogBean) r.getVariableLog(VARIABLE_ID_ITEMS);
    // 根据 log 类型处理...
    break;
```

---

## 注册与移除

| 方法 | 说明 |
|------|------|
| `table.getChangeListenerMap().addListener(listener)` | 注册监听器 |
| `table.getChangeListenerMap().removeListener(listener)` | 移除监听器 |
| `table.getChangeListenerMap().hasListener()` | 是否有监听器 |

### ChangeListenerMap 实现

| 特性 | 说明 |
|------|------|
| 读写分离 | 写加锁，读访问不可变快照 |
| 快照一致性 | 保证事务收集日志阶段和通知阶段用**同一份快照** |

```java
ChangeListener listener = (key, r) -> { ... };
table.getChangeListenerMap().addListener(listener);

// 不再需要时移除
table.getChangeListenerMap().removeListener(listener);
```

---

## 与事务的关系

| 维度 | 说明 |
|------|------|
| 触发时机 | 事务 `finalCommit` 阶段，**先于** `whileCommit` 回调触发（均在提交成功后，但 listener 先于 whileCommit 执行） |
| 事务内回调 | **不应**再对同表写操作 |
| 非事务上下文 | 直接修改**不触发**监听器 |

监听器**严格绑定事务提交**——只有经过事务提交的修改才会触发通知。

---

## 完整示例：监听角色背包变更

```java
ChangeListener bagListener = (key, r) -> {
    long roleId = (Long) key;
    switch (r.getState()) {
        case Changes.Record.Put:
            // 全量：插入或整体替换
            Bag bag = (Bag) r.getValue();
            online.sendWhileCommit(roleId, new SBagSync(bag.toData()));
            break;

        case Changes.Record.Edit:
            // 增量：可用 MergeChangedToReplaced 简化客户端处理
            // 或分别取 getReplaced / getRemoved
            break;

        case Changes.Record.Remove:
            // 背包记录被删除
            online.sendWhileCommit(roleId, new SBagRemoved());
            break;
    }
};
module.getTableBag().getChangeListenerMap().addListener(bagListener);
```

### 客户端处理

| 状态 | 客户端动作 |
|------|-----------|
| Put | `clear` + `putAll`（整体替换） |
| Edit | `putAll`（变更项） + `removeAll`（删除项） |
| Remove | 置 `null` |

---

## 数据同步模式

### 全量推送

| 特点 | 说明 |
|------|------|
| 结构清晰 | 整个 Bean 推送 |
| 推荐 | ✅ 推荐使用 |

### 增量推送

| 问题 | 说明 |
|------|------|
| 原子性 | Get 与增量需保证原子 |
| 丢消息 | 增量消息可能丢失 |
| 对策 | 维护版本号递增，客户端发现不连续时重新 Get |

### Relogin 差异同步

| 特点 | 说明 |
|------|------|
| 离线记录 | 记录玩家离线期间的变更 |
| 重连同步 | 重连时只同步差异部分 |

---

## 分布式注意事项

| 维度 | 说明 |
|------|------|
| 各自注册 | 每台 Server 各自注册监听器 |
| 触发实例 | 只有**实际执行修改的实例**触发回调 |
| 适用 | 同步**个人数据**（玩家自己的数据） |
| 不适用 | 同步**共享数据**（多实例共改的数据） |

---

## 使用建议

| 场景 | 是否推荐 |
|------|----------|
| 逻辑接近事件模型 | 可以用监听器实现逻辑 |
| 一般业务逻辑 | ❌ 不建议用监听器实现，直接在存储过程里写 |
| 同步个人数据给客户端 | ✅ 合适 |
| 更新共享数据 | ❌ 不合适 |

### 数据获取方式推荐

| 方式 | 推荐度 | 说明 |
|------|--------|------|
| **Get 模式**（客户端主动拉） | ⭐⭐⭐ 大大推荐 | 客户端按需拉取 |
| **Push 完整数据** | ⭐⭐⭐ 大大推荐 | 全量推送 |
| Push 不完备增量 | ⚠️ 限制使用 | |
| Push 完备增量 | ❌ 暂不推荐 | |

### 客户端数据获取时机

```
Auth  →  Login  →  Map.EnterWorld  →  UI 显示  →  其他
```

## 相关文档

- [Table 存储接口](./table.md) — 监听器挂在 Table 上
- [事务系统](./transaction.md) — finalCommit 阶段触发、whileCommit
- [Online 组件](./arch-online.md) — sendWhileCommit 等封装
