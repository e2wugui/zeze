---
title: "事务系统"
sidebar:
  order: 1
---

Zeze 的事务系统以**存储过程**为执行单位，采用乐观锁实现无死锁并发控制，开发者只需关注业务逻辑，框架自动处理冲突检测、重做和持久化。

## 存储过程

**存储过程**（Procedure）是 Zeze 中事务的执行单位。每个协议处理函数默认运行在一个独立的存储过程内。框架会在调用时自动创建 `Procedure` 对象，开发者通常不需要手动创建。

存储过程的核心生命周期：执行业务代码 → 提交时加锁并检查冲突 → 成功则提交，冲突则重做。

## TransactionLevel 配置

`TransactionLevel` 决定事务的隔离语义：

```java
public enum TransactionLevel {
    None,                  // 不需要事务
    Serializable,          // 可串行化，所有访问的记录未修改时事务才成功。【默认】
    AllowDirtyWhenAllRead, // 当事务没有写操作时，允许脏读，不判断所读记录是否发生变化
}
```

配置优先级从低到高，高优先级会覆盖低优先级：

1. **程序默认** —— `Serializable`，优先级最低。
2. **Module.DefaultTransactionLevel** —— 配置模块内协议的默认事务级别，仅应用于当前模块，不包括子模块。建议在模块规划阶段确定，设定后不再修改。
3. **Protocol.TransactionLevel** —— 在协议定义中配置该协议处理时的事务级别。
4. **@TransactionLevelAnnotation** —— 在协议处理函数上加注解，优先级最高。

```java
@Zeze.Util.TransactionLevelAnnotation(Level = Zeze.Transaction.TransactionLevel.None)
protected long ProcessMyProtocol(MyProtocol p) {
    // 此处理函数不使用事务
}
```

**建议配置方式**：如果模块内大部分协议需要事务，保持默认 `Serializable`，个别协议用注解覆盖为 `None`；如果大部分不需要事务，设置 `Module.DefaultTransactionLevel` 为 `None`，个别需要的用注解覆盖。

### Serializable vs AllowDirtyWhenAllRead

以转账统计为例：两个账户初始为 0，系统并发随机转账（允许结果为负数），一个统计事务把两个账户加起来得到 `sum`。

- **Serializable**：`sum` 总是 0。事务提交时检查所有读取的记录是否被修改，只要存在冲突就重做，保证一致性。
- **AllowDirtyWhenAllRead**：`sum` 可能不为 0。当事务只有读操作没有写操作时，不检查所读记录是否发生变化，允许读到不一致的快照。适用于统计、查询等只读场景，可减少不必要的重做，提高性能。

## 嵌套存储过程

当业务需要忽略部分失败并继续执行事务时，使用**嵌套存储过程**。通过 `Application.newProcedure` 创建并调用：

```java
protected long ProcessMainTransaction(SomeProtocol p) {
    // 一些处理
    long result = App.Zeze.newProcedure(this::myNestedProcedure, "MyNestedProcedure").call();
    if (result != 0) {
        // 嵌套存储过程失败，其修改已全部回滚，此处可继续执行其他逻辑
    }
    // 继续处理
    return 0;
}

private long myNestedProcedure() {
    if (someCondition)
        return 0;  // 成功
    return errorCode(1);  // 失败，仅回滚此嵌套过程
}
```

嵌套存储过程在同一事务内执行。当嵌套过程返回非零值时，仅回滚该层修改，外层事务不受影响。嵌套过程使用 **Savepoint** 机制实现局部回滚，详见下文。

## Savepoint —— 部分回滚

**Savepoint** 是事务内的保存点机制，也是嵌套存储过程的底层实现。`Transaction.begin()` 创建一个新的 Savepoint，`commit()` 将当前 Savepoint 的日志合并到上一层，`rollback()` 则回滚当前 Savepoint 的修改并通知上一层。

每次 `Procedure.call()` 执行嵌套过程时，会自动调用 `begin()` / `commit()` / `rollback()`。开发者一般不需要直接操作 Savepoint，而是通过嵌套存储过程间接使用。

Savepoint 内部维护了日志映射和提交/回滚动作列表。当嵌套过程失败时，其日志被丢弃，注册的 `whileCommit` 动作被转为 `NESTED_ROLLBACK` 类型传递给上一层，确保副作用得到正确处理。

## WhileCommit / WhileRollback

事务可能因冲突而重做，所有事务内代码都可能被重复执行。`Transaction.whileCommit` 和 `Transaction.whileRollback` 用于控制副作用仅在确定的时机执行：

- **whileCommit** —— 事务最终成功提交时执行。
- **whileRollback** —— 事务最终失败回滚时执行。

```java
Transaction.whileCommit(() -> {
    // 仅在事务成功提交后执行
    logger.info("事务提交成功，执行后续操作");
});
```

**典型场景**——在只读事务中验证数据一致性：

```java
public void verifyAccountSum() {
    var account1 = tableAccount.get("tom");
    var account2 = tableAccount.get("jack");
    var sum = account1.getValue() + account2.getValue();
    Transaction.whileCommit(() -> assert sum == 0);
    // 即使 TransactionLevel 为 Serializable，不用 whileCommit 断言也可能失败。
    // 因为乐观锁执行过程中不加锁，只有在最终提交时才检测冲突。
}
```

## SendWhileCommit / SendWhileRollback

在事务中发送协议是最常见的副作用之一。如果直接在事务内发送，重做会导致协议重复发送。Arch 框架的 `Online` 组件提供了事务安全的发送方法：

```java
// 事务提交后发送
online.sendWhileCommit(account, clientId, protocol);

// 事务回滚后发送
online.sendWhileRollback(account, clientId, protocol);

// 事务提交后发送 Rpc 响应
online.sendResponseWhileCommit(account, clientId, rpc);
```

这些方法内部使用 `Transaction.whileCommit` / `Transaction.whileRollback` 包装实际的发送操作。对于第三方交互的更多细节，参见 → third-party-interactions。

## 自定义日志

通过 `Transaction.getLog` / `putLog` 实现自定义日志，用于在事务中记录特殊操作：

```java
// 获取日志，不存在则通过工厂创建
Log log = Transaction.getCurrent().logGetOrAdd(logKey, MyLog::new);

// 放入日志
Transaction.getCurrent().putLog(myLog);
```

自定义日志需要继承 `Zeze.Transaction.Log` 并实现 `commit()` 方法。**注意**：`commit()` 必须成功，否则程序会终止运行（`halt`）。自定义日志的 `category()` 应返回 `Log.Category.eUser` 或 `Log.Category.eSpecial`。

## 存储过程返回值

`Procedure.call()` 返回 `long` 类型的结果码：

| 范围 | 含义 | 示例 |
|------|------|------|
| `= 0` | 成功 | `Procedure.Success` |
| `< 0` | Zeze 内部错误 | `Procedure.Exception`(-1), `Procedure.TooManyTry`(-2) |
| `> 0` | 用户自定义错误码 | `(moduleId << 16) \| errorCode` |

### 错误码编码

用户错误码按 `(Module.Id << 16) | ModuleErrorCode` 编码。`IModule` 基类提供辅助方法：

```java
// 在模块内构造错误码
return errorCode(1);  // 等价于 (getId() << 16) | 1

// 解码
int moduleId = IModule.getModuleId(result);   // 提取模块ID
int errCode = IModule.getErrorCode(result);    // 提取错误码
```

模块级错误码可以在模块定义（solution.xml）中用枚举（enum）声明。

### 自动发送 Rpc 错误结果

Arch 框架在处理 Rpc 的存储过程时，如果得到非零返回值，会自动发送 Rpc 的错误结果。异常时也会返回错误码。因此，正常流程只需设置 Rpc 结果参数并调用 `rpc.SendResult()`，错误时直接返回错误码即可。

## 事务重做导致的问题

事务执行中如果发生冲突，会自动重做（最多 256 次）。应用开发必须注意此特性：

- **发送协议** —— 除非不在乎重复发送，否则应使用 `sendWhileCommit` 等事务安全版本。
- **注册 Timer** —— 使用 `Transaction.whileCommit` 包装注册操作。Zeze 内置的 Timer 组件会自动嵌入事务，无需额外处理。
- **提交任务给线程池** —— 一般需要使用 `whileCommit` / `whileRollback` 包装。
- **操作自定义数据** —— 所有非 Zeze 管理的数据都属于自定义数据，推荐统一模式：随意读取 → 计算值存入局部变量 → `whileCommit` 中修改自定义数据。读取修改自定义数据需要自行控制并发。参见 → third-party-interactions。

## in / out / ref 参数注意事项

当向存储过程传递参数时，事务重做可能导致数据不一致：

- **in 参数（只读）** —— 安全，无需特殊处理。
- **out 参数（仅返回结果）**
  - 赋值单个引用给 out 参数的成员变量是安全的。
  - 通过集合返回多个值时，有两种处理方式：如果集合可以 `clear()`，在存储过程开始时调用一次，防止重做收集多余结果；如果集合不能清空（需要归并），先收集到局部变量，再通过 `whileCommit` 合并到 out 集合中。
- **ref 参数（读写兼返回）** —— 返回结果需通过 `whileCommit` 设置到 ref 变量中。集合类型结果先收集到局部变量。

## @DispatchMode

`@DispatchModeAnnotation` 是协议处理函数的注解，用于控制协议的调度方式：

```java
public enum DispatchMode {
    Normal,    // 在普通线程池中执行。【默认】
    Critical,  // 在重要线程池中执行。
    Direct,    // 在调用者线程中执行。
}
```

使用示例：

```java
@Zeze.Util.DispatchModeAnnotation(mode = Zeze.Transaction.DispatchMode.Critical)
protected long ProcessImportantProtocol(ImportantProtocol p) {
    // 在重要线程池中执行，适用于高优先级协议
}
```

协议的线程调度还可以在 `Zeze.Net.Service` 子类中重载 `DispatchProtocol`、`DispatchRpcResponse` 等方法控制。重载会覆盖默认实现，优先级高于注解。
