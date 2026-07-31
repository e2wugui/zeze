---
title: "事务中操作外部系统"
description: "Zeze 事务与外部系统交互的可靠性问题、whileCommit 包装、事务队列与幂等设计速查"
category: reference
order: 7
---

本文是 Zeze **事务中操作外部系统**的完整参考——覆盖外部系统无法随事务回滚的核心问题、whileCommit/whileRollback、常见场景、in/out/ref 参数模式、第三方可靠性和调度方式选择，供写代码时随查随用。事务基础见 [事务系统](./transaction.md)，概念讲解见 [编写业务逻辑](../manual/04-writing-logic.md)。

## 核心问题

Zeze 事务可能因冲突**重做**，而外部系统的操作**无法随事务自动回滚**。这带来两个风险：

| 风险 | 说明 | 危险例子 |
|------|------|----------|
| 事务体重做时外部调用再次发起 | 重做会让外部调用执行多次 | 事务体重做 → putObject 调多次 |
| 事务重做后最终回滚 | 已执行的外部操作不会自动撤销 | `bag.remove` + `oss.putObject`，事务重做后 putObject 已执行多次且不回滚 |

> **总原则：凡是写在事务体里、且不可重做的操作，都是 bug。**

---

## whileCommit / whileRollback

框架给出的核心解决办法：

| 方法 | 说明 |
|------|------|
| `Transaction.whileCommit(action)` | 最终提交成功后执行一次 |
| `Transaction.whileRollback(action)` | 回滚时执行一次 |

### 关键保证

| 保证 | 说明 |
|------|------|
| 只执行一次 | 无论事务因冲突重做了多少次 |
| whileCommit 触发阶段 | `finalCommit` 阶段，**数据写入后**触发 |
| whileRollback 触发阶段 | `finalRollback` 阶段，**日志清空后**触发 |
| 立即执行 | 事务进入 `Completed` 状态时立即执行 |

```java
public long doProcess() {
    Player player = table.getOrAdd(roleId);
    player.setCoin(player.getCoin() + reward);

    // 正确：协议在事务提交后才发出，且只发一次
    Transaction.whileCommit(() -> externalApi.notify(roleId));
    return Procedure.Success;
}
```

---

## 常见场景

### 1. 发送网络协议

事务内直接 `send` 危险（重做会重发）。用 Online 组件封装：

| 方法 | 实现 |
|------|------|
| `online.sendWhileCommit(roleId, protocol)` | `whileCommit` + `send` |
| `online.sendResponseWhileCommit(roleId, rpc)` | `whileCommit` + `sendResponse` |

```java
online.sendWhileCommit(roleId, new SCoinChanged(player.getCoin()));
```

### 2. 注册 Timer

用 `whileCommit` 包装，确保只在提交成功后注册。

| 说明 | |
|------|----|
| 框架内置 Timer | **已自动嵌入事务**，用框架 API 注册时通常不用手包 |
| 自定义调度机制 | 需要 `whileCommit` 包装 |

```java
// 内置 Timer 已自动嵌入事务，可直接注册
scheduleTask(delay, this::onTimeout);
```

### 3. 提交异步任务

往自定义线程池提交任务用 `whileCommit` 包装，避免重做导致多次提交：

```java
Transaction.whileCommit(() -> myExecutor.submit(() -> doHeavyWork(player.toData())));
```

> 跨线程传递托管 Bean 不安全，应传 Data 快照（`toData()`），见 [Bean 数据模型](./bean.md)。

### 4. 操作自定义内存数据

统一模式：**「读-算-whileCommit 改」**。

```java
public long doProcess() {
    int result = computeSomething();   // 读 + 计算，事务体内随便做
    Transaction.whileCommit(() -> myCache.update(result));   // 提交后才改
    return Procedure.Success;
}
```

---

## in / out / ref 参数模式

调用方法时参数的副作用也要按事务安全方式处理：

| 参数模式 | 安全用法 |
|----------|----------|
| `in`（只读） | 直接用，天生安全 |
| `out`（集合返回多个值） | 通过 `whileCommit` 写回 |
| `ref`（引用返回） | 结果在 `whileCommit` 里设置；集合先局部变量，whileCommit 合并 |

### out 参数：错误 vs 正确

```java
// ❌ 错误：直接修改外部变量，事务重做结果不确定
public void compute(OutObject<Integer> out) {
    out.setValue(calculate());   // 事务重做会反复覆盖，结果不确定
}

// ✅ 正确：先局部变量，whileCommit 写出
public void compute(OutObject<Integer> out) {
    int result = calculate();
    Transaction.whileCommit(() -> out.setValue(result));
}
```

### ref 参数（集合）

```java
// 先局部变量收集，whileCommit 合并
List<Item> local = new ArrayList<>();
local.addAll(newItems);
Transaction.whileCommit(() -> targetList.addAll(local));
```

---

## 第三方可靠性问题

根据外部系统的失败特性分两种情况：

### 情况一：外部几乎不失败

适用于**本地发协议、本地线程池**这类操作（不依赖外部可用性）：

| 做法 | 适用 |
|------|------|
| 直接 `whileCommit` 包装 | 本地发协议、本地线程池 |

```java
Transaction.whileCommit(() -> localExecutor.submit(() -> ...));
```

### 情况二：需要可靠投递

用**事务队列**模式：

```
事务内：写入 Zeze 表作队列  →  提交成功  →  搬运线程消费队列、执行外部调用  →  成功后删除队列项
```

| 优势 | 说明 |
|------|------|
| 可靠 | 队列项已持久化在 Zeze 表，重启不丢 |
| 解耦 | 外部调用的重试、失败处理由搬运线程负责 |

```java
// 事务内：写队列
public long doProcess() {
    QueueTask task = tableQueue.getOrAdd(autoKey.nextId());
    task.setPayload(payload);
    return Procedure.Success;
}
// 提交后由独立的搬运线程消费 tableQueue，执行真正的外部调用
```

---

## 调度方式选择

| 情况 | 调度方式 |
|------|----------|
| 外部操作**不依赖**事务结果 | 事务外调度（最简单） |
| 依赖事务结果，且**可安全重做** | 事务内调用（用 `whileCommit` 包装） |
| **不可重做**，但需事务参数 | 拆分事务：`proc1 + rpc + proc2` |

### 拆分事务（proc1 + rpc + proc2）

当操作不可重做又需要事务里的参数时，把一个事务拆成三段：

```
proc1（算好参数并提交）  →  rpc（调外部，不可重做）  →  proc2（收尾，提交）
```

```java
// proc1：在事务里算好参数，提交
long rc = Application.newProcedure(this::prepare, "Prepare").call();
if (rc != Procedure.Success) return rc;

// rpc：事务外调外部系统（不可重做，但此时已无事务）
externalApi.call(preparedParam);

// proc2：在事务里收尾，提交
rc = Application.newProcedure(this::finish, "Finish").call();
return rc;
```

---

## 幂等性设计

| 场景 | 做法 |
|------|------|
| whileCommit 包装的操作 | 只执行一次，无需额外幂等 |
| 事务体内调用外部 | **必须幂等**（事务体可能重复执行） |

### 幂等手段

| 手段 | 适用 |
|------|------|
| AutoKey 唯一请求 id | 生成唯一 id，外部系统去重 |
| 查询操作 | 天然幂等 |
| 写入操作 | 尽量改成 `whileCommit`，避免事务体内写 |

```java
// 用 AutoKey 生成唯一请求 id，外部去重
public long doProcess() {
    long requestId = autoKey.nextId();
    Transaction.whileCommit(() ->
        externalApi.callWithId(requestId, payload));   // 外部按 requestId 去重
    return Procedure.Success;
}
```

---

## 速查决策表

| 你的操作 | 推荐做法 |
|----------|----------|
| 发协议给客户端 | `sendWhileCommit` |
| 注册框架内置 Timer | 直接注册（已嵌入事务） |
| 注册自定义调度 | `whileCommit` 包装 |
| 提交本地线程池任务 | `whileCommit` 包装 |
| 改自定义内存数据 | 「读-算-whileCommit 改」 |
| 调本地不会失败的外部 | `whileCommit` 包装 |
| 调需要可靠投递的外部 | 事务队列模式 |
| 调不可重做且需事务参数的外部 | 拆分事务 `proc1 + rpc + proc2` |
| 事务体内调外部（必须） | 用 AutoKey 保证幂等 |

## 相关文档

- [事务系统](./transaction.md) — Procedure、whileCommit、Savepoint
- [编写业务逻辑](../manual/04-writing-logic.md) — 副作用处理概念讲解
