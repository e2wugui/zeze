---
title: "编写业务逻辑"
description: "在存储过程里写业务，借助自动事务提交、回滚与 whileCommit 安全处理副作用"
category: manual
order: 4
---

读完这篇，你能把业务逻辑写进 Zeze 的存储过程里，理解「返回 Success 自动提交、抛异常自动回滚」的工作机制，并知道为什么事务可能重做、如何用 `whileCommit` 安全地发送协议、注册定时器、操作外部系统。

## 存储过程：业务逻辑的执行单位

在 Zeze 里，业务逻辑写在**存储过程（Procedure）**里。存储过程是事务的执行单位——每个协议处理函数默认就跑在一个独立的存储过程里。

它的生命周期是这样的：

```
执行业务  →  提交时加锁、检查冲突  →  成功则提交 / 冲突则整体重做
```

也就是说，你写的逻辑会被框架包在一个事务里跑，跑完后框架帮你做提交和冲突检测。这就是 Zeze 业务编程的心智模型：**你只管写「做什么」，提交、加锁、冲突重试都由框架兜底。**

### 提交与回滚的魔法

存储过程返回一个 `long`。这个返回值就是事务的「指令」：

- **返回 `Procedure.Success`（也就是 0）**：事务成功，框架自动提交。
- **抛出异常**：事务失败，框架自动回滚。

你**不需要**手动调用 `commit()` 或 `rollback()`——返回值和异常就是信号。这让业务代码非常干净，核心就是「算完返回结果」：

```java
public class BuyItem extends AbstractBuyItem {

    @Override
    protected long doProcess() {
        // 框架已开启事务，Table 的修改会被自动追踪
        Player player = module.getTablePlayer().getOrAdd(roleId);
        if (player.getCoin() < price) {
            return errorCode(module, BuyItem.ERR_COIN_NOT_ENOUGH); // 用户错误码，事务回滚
        }

        player.setCoin(player.getCoin() - price);
        addItem(player, configId, count);

        return Procedure.Success; // 成功，自动提交
    }
}
```

这段代码里，对 `player.setCoin(...)` 的修改会被事务记录。如果走到 `return errorCode(...)`，之前对 `player` 的修改全部回滚；如果走到 `return Procedure.Success`，所有修改原子落库。

### 返回值的含义

存储过程返回值（`long`）分三段：

| 返回值 | 含义 |
|--------|------|
| `= 0` | 成功（`Procedure.Success`） |
| `< 0` | Zeze 内部错误码，共 20 个（`-1` ~ `-20`，详见 [事务参考](../reference/transaction.md)） |
| `> 0` | 用户错误码 |

用户错误码用 `(moduleId << 32) | (protocolId & 0xffffffffL)` 的方式编码（注意是**左移 32 位**，不是 16 位）。你不需要手算这个位运算——用 `errorCode(1)` 这样的构造方式生成，解码时用 `IModule.getModuleId(returnCode)` 取模块 id、`IModule.getErrorCode(returnCode)` 取错误码。结合 [定义数据](./03-defining-data.md) 里讲的 `<enum>`，你可以把错误码集中声明，代码里直接引用常量。

## 事务隔离级别

并非所有逻辑都需要同样严格的事务保证。Zeze 提供 **TransactionLevel** 来调节：

- **`None`**：不需要事务。
- **`Serializable`**（默认）：可串行化。事务访问的所有记录在提交时若**未被他人改动**才算成功，否则重做。
- **`AllowDirtyWhenAllRead`**：当一个事务**只读不写**时，跳过冲突检查以提升性能。

配置的优先级**从低到高**为：

```
程序默认  <  Module.DefaultTransactionLevel  <  Protocol.TransactionLevel  <  @TransactionLevel 注解
```

也就是说，越靠近代码的配置优先级越高。多数时候你用默认的 `Serializable` 即可；对于纯统计、纯展示的接口，可以考虑用 `AllowDirtyWhenAllRead` 换取性能。

## 嵌套存储过程

复杂业务里，你可能想把一段逻辑封装成独立的子事务。用 `Application.newProcedure` 创建并调用：

```java
public long doProcess() {
    // 内层是一个独立的子存储过程
    long rc = Application.newProcedure(this::transferCoin, "TransferCoin").call();
    if (rc != Procedure.Success) {
        return rc; // 内层失败，直接返回（内层已回滚，外层干净）
    }
    // 外层继续
    return Procedure.Success;
}

private long transferCoin() {
    // 子事务逻辑
    return Procedure.Success;
}
```

嵌套存储过程基于 **Savepoint** 实现：**内层失败只回滚内层，外层不受影响、可以继续执行**。这让你能把可复用的业务片段安全地组合起来。

## 事务会重做：副作用必须用 whileCommit

这是 Zeze 业务编程里**最容易踩坑、也最重要**的一课。

回顾存储过程的生命周期：「冲突则整体重做」。这意味着事务体里的代码**可能被执行不止一次**。如果你在事务体里直接做有副作用的操作（发网络消息、注册定时器、提交线程池任务），重做时这些操作会**被执行多次**，后果往往是灾难性的——比如玩家收到三封相同的邮件。

框架给出的解决办法是 **`whileCommit` / `whileRollback`**：

- `Transaction.whileCommit(action)`：注册一个回调，**仅在事务最终提交成功后执行一次**。
- `whileRollback(action)`：在事务最终回滚时执行一次。

无论事务因为冲突重做了多少次，`whileCommit` 注册的回调**只会执行一次**。这正是它名字的含义——「while（在）事务最终 commit 时」。

下面看几个最常见的场景。

### 场景一：发送网络协议

事务内直接 `send` 是危险的（重做会重发）。正确做法是用 Online 组件封装的 **`sendWhileCommit` / `sendResponseWhileCommit`**——它们内部就是 `whileCommit` + `send`：

```java
public long doProcess() {
    Player player = module.getTablePlayer().getOrAdd(roleId);
    player.setCoin(player.getCoin() + reward);

    // 协议在事务真正提交后才发出，且只发一次
    online.sendWhileCommit(roleId, new SCoinChanged(player.getCoin()));
    return Procedure.Success;
}
```

### 场景二：注册定时器

注册 Timer 要用 `whileCommit` 包装，确保只在事务提交成功后才真正注册。需要注意，**框架内置的 Timer 已经自动嵌入了事务**，所以用框架 API 注册时通常不用你再手动包一层——但如果你用的是自定义的调度机制，记得用 `whileCommit`。

```java
public long doProcess() {
    // 内置 Timer 已自动嵌入事务，可直接注册
    scheduleTask(delay, this::onTimeout);
    return Procedure.Success;
}
```

### 场景三：提交线程池任务

往自定义线程池提交任务同样要用 `whileCommit` 包装，避免重做导致任务被提交多次：

```java
public long doProcess() {
    Player player = module.getTablePlayer().getOrAdd(roleId);

    // 提交到自定义线程池：用 whileCommit 确保只提交一次
    Transaction.whileCommit(() -> myExecutor.submit(() -> doHeavyWork(player.toData())));
    return Procedure.Success;
}
```

注意这里把 `player.toData()` 传入线程池——因为跨线程传递托管 Bean 是不安全的，应该传它的 Data 快照（见 [定义数据](./03-defining-data.md) 里的 Data 类）。

### 场景四：操作自定义内存数据

有时候你会维护一些不属于 Table 的自定义内存数据。推荐的统一模式是 **「随意读 → 算 → 在 whileCommit 里改」**：

```java
public long doProcess() {
    int result = computeSomething();   // 读 + 计算，可以放在事务体里随便做

    // 修改自定义内存数据：放到 whileCommit 里，确保只在提交后生效一次
    Transaction.whileCommit(() -> myCache.update(result));
    return Procedure.Success;
}
```

这个「读-算-whileCommit 改」的模式适用于几乎所有自定义内存数据，记住它就够了。

### in / out / ref 参数

调用方法时参数的副作用也要按事务安全的方式处理：

- **`in` 参数**：只读，天生安全，直接用。
- **`out` 参数**（通过集合返回多个值）：如果集合能 `clear`，就在方法开头 `clear` 一次再填充；如果不能 `clear`（比如调用方已经持有引用），就先把结果收集到局部变量，再在 `whileCommit` 里合并进去。
- **`ref` 参数**（通过引用返回结果）：在 `whileCommit` 里设置最终结果。

## 调度模式：@DispatchMode

存储过程执行在哪个线程上，由 `@DispatchMode` 决定：

- **`Normal`**（默认）：普通线程池。
- **`Critical`**：重要线程池，适合不能被普通任务阻塞的关键路径。
- **`Direct`**：直接在调用者线程上执行。

大多数情况用默认即可。`Direct` 偶尔用于希望省去一次线程切换、且任务极轻量的场景。

## 与外部系统打交道

这是另一个需要特别小心的领域。核心问题是：**外部系统的操作无法随事务自动回滚。**

比如你在事务里调用了一个第三方 HTTP 接口。如果事务随后因为冲突重做，这段调用逻辑会再跑一次——你就给第三方发了两次请求。

根据不同情况，有三种处理策略：

### 策略一：whileCommit 包装「几乎不会失败」的操作

对于本地发协议、本地线程池提交这类「不会失败、也不依赖外部可用性」的操作，用 `whileCommit` 包装就够了：

```java
Transaction.whileCommit(() -> localExecutor.submit(() -> ...));
```

### 策略二：事务队列实现可靠投递

当操作必须可靠送达时，不要在事务里直接调外部系统，而是**在事务内写一张 Zeze 表当作队列**，提交后由一个独立的搬运线程消费这个队列、去真正执行外部调用：

```
事务内：写入 queue 表  →  提交成功  →  搬运线程读取并执行外部调用  →  成功后删除队列项
```

这样即使系统重启，未消费的队列项也不会丢——它已经持久化在 Zeze 表里了。

### 策略三：用 AutoKey 保证幂等

有些外部接口需要你去重。可以用 **AutoKey** 在事务内生成一个**唯一的请求 id**，随请求带给外部系统让其去重。即使事务重做导致请求被发送多次，外部系统也能凭这个 id 识别并丢弃重复请求。

### 何时在事务内、何时在事务外调用外部系统

调度方式可以这样判断：

| 情况 | 调度方式 |
|------|----------|
| 外部操作**不依赖**事务结果 | 事务外调度，最简单 |
| 依赖事务结果，且**可以安全重做** | 事务内调用（用 whileCommit 包装） |
| **不可重做**，但又需要事务里的参数 | 拆成 `proc1 + rpc + proc2`：先在 proc1 里算好并提交，再发起 rpc 调外部，最后 proc2 收尾 |

记住总原则：**凡是写在事务体里、且不可重做的操作，都是 bug。** 要么用 `whileCommit` 推迟到提交后，要么用事务队列，要么拆事务。

## 小结

把这一章浓缩成几条：

1. **业务逻辑写进存储过程**，返回 `Success` 自动提交，抛异常自动回滚——不用手动管事务。
2. **返回值 `> 0` 是用户错误码**，用 `errorCode(1)` 构造、按 `(moduleId<<32)|errorCode` 编码。
3. **事务会因为冲突重做**，所以事务体里的代码可能跑多次。
4. **一切副作用都用 `whileCommit` 包装**：发协议用 `sendWhileCommit`，注册 Timer、提交线程池、改自定义内存数据都用 `whileCommit`。
5. **外部系统调用要额外小心**：能重做的用 `whileCommit`，要可靠的用事务队列，要去重的用 AutoKey 幂等。

更底层的细节——事务的完整生命周期、隔离级别的精确语义——见 [transaction 参考](../reference/transaction.md)；外部系统交互的更多模式见 [third-party interactions 参考](../reference/third-party-interactions.md)。

至此你已经能在单机视角下写出完整的业务逻辑了。但 Zeze 的真正威力在于分布式——下一章 [走向分布式](./05-going-distributed.md) 会带你把这套模型扩展到多进程、多服务器。
