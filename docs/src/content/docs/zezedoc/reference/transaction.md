---
title: "事务系统"
description: "Zeze 存储过程、事务级别、嵌套 Savepoint、whileCommit 与错误码编码速查"
category: reference
order: 3
---

本文是 Zeze **事务系统**的完整 API 参考——覆盖存储过程生命周期、事务隔离级别、嵌套存储过程与 Savepoint、whileCommit/whileRollback、自定义日志、错误码编码和调度模式，供写代码时随查随用。概念讲解见 [Zeze 如何工作](../manual/02-how-zeze-works.md) 和 [编写业务逻辑](../manual/04-writing-logic.md)，副作用安全处理见 [事务中操作外部系统](./third-party-interactions.md)。

## 存储过程（Procedure）

**Procedure（存储过程）** 是事务的执行单位。每个协议处理函数默认就跑在一个独立的存储过程里，框架自动创建，通常不用手动建。

### 生命周期

```
执行业务代码  →  提交时加锁、检查冲突  →  成功则提交 / 冲突则整体重做
```

| 阶段 | 说明 |
|------|------|
| 执行 | 代码读写数据；写操作以**日志**形式暂存，原始数据不变 |
| 提交检查 | 返回成功后，对访问过的所有记录按固定顺序加锁，比对时间戳 |
| 成功提交 | 无冲突 → 应用日志、递增版本号、释放锁 |
| 冲突重做 | 某记录时间戳已变 → 丢弃日志和访问记录 → 从头重新执行 |

> 关键：执行阶段**完全不加锁**，提交时才检查冲突。这是 Zeze 原理上无死锁的基础。

## 事务隔离级别（TransactionLevel）

| 级别 | 说明 | 适用场景 |
|------|------|----------|
| `None` | 不需要事务 | |
| `Serializable`（默认） | 可串行化。访问的所有记录在提交时若**未被他人改动**才算成功，否则重做 | 默认，大多数写操作 |
| `AllowDirtyWhenAllRead` | 当事务**只读不写**时，跳过冲突检查以提升性能 | 纯统计、纯查询、只读接口 |

### Serializable vs AllowDirtyWhenAllRead（转账 vs 统计）

以「A 给 B 转 100，统计 A+B 总额」为例：

| 场景 | Serializable | AllowDirtyWhenAllRead |
|------|--------------|----------------------|
| 转账（有写） | sum 恒为 0，正确 | 不适用（有写不能跳过检查） |
| 统计总额（纯读） | 正确但慢 | sum 可能不为 0（读到中间态），适用于**统计查询**容忍轻微不一致 |

### 配置优先级（从低到高）

```
程序默认  <  Module.DefaultTransactionLevel  <  Protocol.TransactionLevel  <  @TransactionLevel 注解
```

越靠近代码的配置优先级越高。

```java
@TransactionLevel(TransactionLevel.AllowDirtyWhenAllRead)
protected long doProcess() { ... }
```

## 返回值

存储过程返回 `long`，分三段：

| 返回值 | 含义 |
|--------|------|
| `= 0` | 成功（`Procedure.Success`），框架自动提交 |
| `< 0` | Zeze 内部错误码，共 20 个（`-1` ~ `-20`），事务回滚 |
| `> 0` | 用户错误码，事务回滚 |

负数内部错误码完整列表（`Procedure.java` 常量）：

| 值 | 常量 | 说明 |
|----|------|------|
| `-1` | `Exception` | 抛出异常 |
| `-2` | `TooManyTry` | 重做次数过多 |
| `-3` | `NotImplement` | 未实现 |
| `-4` | `Unknown` | 未知错误 |
| `-5` | `ErrorSavepoint` | Savepoint 错误 |
| `-6` | `LogicError` | 逻辑错误 |
| `-7` | `RedoAndRelease` | 重做并释放 |
| `-8` | `AbortException` | 中止异常 |
| `-9` | `ProviderNotExist` | Provider 不存在 |
| `-10` | `Timeout` | 超时 |
| `-11` | `CancelException` | 取消异常 |
| `-12` | `DuplicateRequest` | 重复请求 |
| `-13` | `ErrorRequestId` | 请求 ID 错误 |
| `-14` | `ErrorSendFail` | 发送失败 |
| `-15` | `RaftRetry` | Raft 重试 |
| `-16` | `RaftApplied` | Raft 已应用 |
| `-17` | `RaftExpired` | Raft 过期 |
| `-18` | `Closed` | 已关闭 |
| `-19` | `Busy` | 繁忙 |
| `-20` | `AuthFail` | 鉴权失败 |

抛出异常也会回滚。

### 错误码编码

用户错误码用 `(moduleId << 32) | (errorCode & 0xffffffffL)` 编码（注意是**左移 32 位**，不是 16 位）：

```java
// 构造：errorCode(1) 表示该模块的错误码 1
return errorCode(1);   // = (getModuleId() << 32) | 1

// 解码
int moduleId  = IModule.getModuleId(returnCode);   // 取出模块 id
int errorCode = IModule.getErrorCode(returnCode);  // 取出错误码
```

模块级错误码可在 XML 里用 `<enum>` 集中声明：

```xml
<module name="role" id="1">
    <enum name="ERR_COIN_NOT_ENOUGH" value="1"/>
</module>
```

```java
return errorCode(role.ERR_COIN_NOT_ENOUGH);
```

### 自动发送 Rpc 错误结果

在 Arch 框架下，Rpc 的非零返回会被**自动转成错误结果**发给调用方；异常也返回错误码。正常流程显式设置结果并调用 `rpc.SendResult()`，出错直接返回错误码。

```java
public long doProcess() {
    if (invalidParam) {
        return errorCode(1);          // 自动发送错误结果
    }
    rpc.Result.setCode(0);
    rpc.SendResult();                  // 正常发送结果
    return Procedure.Success;
}
```

## 嵌套存储过程

用 `Application.newProcedure` 创建并调用子存储过程：

```java
public long doProcess() {
    long rc = Application.newProcedure(this::transferCoin, "TransferCoin").call();
    if (rc != Procedure.Success) {
        return rc;   // 内层失败，内层已回滚，外层干净，直接返回
    }
    return Procedure.Success;   // 外层继续
}

private long transferCoin() {
    return Procedure.Success;
}
```

基于 **Savepoint** 实现：**内层失败只回滚内层，外层不受影响、可继续**。

## Savepoint

Zeze **没有独立的 `createSavepoint()` 方法**，嵌套事务（Savepoint）就是通过 `Transaction` 自身的 `begin()` / `commit()` / `rollback()` 实现：内层 `begin()` 压入新的 Savepoint，`commit()` 把本层日志合并到上层，`rollback()` 只丢弃本层日志、保留上层。

| 方法 | 说明 |
|------|------|
| `Transaction.begin()` | 创建 Savepoint（进入新一层；首层为顶层事务，嵌套层为 Savepoint） |
| `Transaction.commit()` | 合并当前层日志到上层 |
| `Transaction.rollback()` | 回滚当前层日志，保留上层 |

```java
// 嵌套 Savepoint（在已有事务内开一层）
Transaction current = Transaction.getCurrent();
current.begin();          // 进入新一层 Savepoint
try {
    // 子操作
    current.commit();     // 成功：把本层日志合并到上层
} catch (Exception e) {
    current.rollback();   // 失败：只丢弃本层，上层不受影响
}
```

## whileCommit / whileRollback

> **Zeze 业务编程里最重要的一课。** 事务可能因冲突重做，事务体里的代码**可能执行多次**。直接在事务体里做副作用（发协议、注册 Timer、提交线程池）会重做多次，必须用 whileCommit 包装。

| 方法 | 说明 |
|------|------|
| `Transaction.whileCommit(action)` | 最终提交成功后执行一次 |
| `Transaction.whileRollback(action)` | 回滚时执行一次 |

无论事务因冲突重做了多少次，注册的回调**只执行一次**。

### 触发时机

| 回调 | 触发阶段 |
|------|----------|
| whileCommit | `finalCommit` 阶段，数据写入后触发 |
| whileRollback | `finalRollback` 阶段，日志清空后触发 |

事务进入 `Completed` 状态时**立即执行**。

### 典型场景

```java
// 1. 发送网络协议（用 Online 封装，内部是 whileCommit + send）
online.sendWhileCommit(roleId, new SCoinChanged(player.getCoin()));
online.sendResponseWhileCommit(roleId, rpc);

// 2. 注册 Timer（框架内置 Timer 已自动嵌入事务，通常不用手包）
//    自定义调度机制需要 whileCommit 包装
Transaction.whileCommit(() -> scheduleTask(delay, this::onTimeout));

// 3. 提交线程池任务
Transaction.whileCommit(() -> executor.submit(() -> doWork(player.toData())));

// 4. 操作自定义内存数据（读-算-whileCommit 改 模式）
int result = computeSomething();
Transaction.whileCommit(() -> myCache.update(result));
```

### SendWhileCommit / SendWhileRollback

Online 组件封装的高层方法：

| 方法 | 实现 |
|------|------|
| `sendWhileCommit(...)` | 内部 `whileCommit` + `send` |
| `sendResponseWhileCommit(...)` | 内部 `whileCommit` + `sendResponse` |

## 事务重做导致的问题与对策

| 问题 | 对策 |
|------|------|
| 发送协议重发 | 用 `sendWhileCommit` |
| 注册 Timer 重复 | 用 `whileCommit`（内置 Timer 已自动嵌入事务） |
| 提交线程池重复 | 用 `whileCommit` |
| 操作自定义内存数据被覆盖 | 用「读-算-whileCommit 改」模式 |

### in / out / ref 参数

| 参数模式 | 安全用法 |
|----------|----------|
| `in`（只读） | 直接用，天生安全 |
| `out`（集合返回） | 集合能 `clear` 就开头 clear 一次再填；不能 clear 就先收集到局部变量，whileCommit 里合并 |
| `ref`（引用返回） | 结果在 whileCommit 里设置；集合先局部变量，whileCommit 合并 |

```java
// out 参数：集合先 clear 再填
public void compute(OutObject<Integer> out) {
    // 错误：直接改外部变量，事务重做结果不确定
    // 正确：先局部变量
    int result = calculate();
    Transaction.whileCommit(() -> out.setValue(result));
}
```

## 自定义日志

自定义事务日志用于追踪非 Bean 数据的修改。继承 `Zeze.Transaction.Log`：

| 方法 | 说明 |
|------|------|
| `commit()` | 提交时调用，**必须成功**否则 halt |
| `category` | 日志类别，返回 `eUser` / `eSpecial` |

通过 `Transaction.getLog` / `putLog` 注册。

```java
class MyLog extends Zeze.Transaction.Log {
    @Override
    public void commit() {
        // 必须成功，失败会 halt
    }
    @Override
    public int category() { return eUser; }
}
```

## 调度模式（@DispatchMode）

决定存储过程在哪个线程上执行：

| 模式 | 说明 |
|------|------|
| `Normal`（默认） | 普通线程池 |
| `Critical` | 重要线程池，不能被普通任务阻塞的关键路径 |
| `Direct` | 直接在调用者线程执行，省一次线程切换 |

```java
@DispatchMode(DispatchMode.Critical)
protected long doProcess() { ... }
```

可在 Service 子类重载 `DispatchProtocol` / `DispatchRpcResponse` 自定义调度逻辑。

## 典型场景速查

| 需求 | 做法 |
|------|------|
| 验证数据一致性 | 事务内 `whileCommit` 断言 |
| 发协议 | `sendWhileCommit` |
| 注册 Timer | `whileCommit`（内置 Timer 已嵌入） |
| 提交线程池 | `whileCommit` |
| 改自定义内存数据 | 「读-算-whileCommit 改」 |

## 相关文档

- [Table 存储接口](./table.md) — Table 操作必须在事务内
- [ChangeListener 数据变更监听](./listener.md) — 监听器在 finalCommit 阶段触发
- [事务中操作外部系统](./third-party-interactions.md) — whileCommit 与可靠投递
- [Zeze 如何工作](../manual/02-how-zeze-works.md) — 乐观锁与重做原理
- [编写业务逻辑](../manual/04-writing-logic.md) — 存储过程用法讲解
