---
title: "Redirect 跨服调用"
sidebar:
  order: 3
---

在分布式架构中，不同用户可能登录在不同的 Server 实例上。当用户 A 需要访问用户 B 所在 Server 上的数据时，如果直接从本机读取，会导致 Cache 失效、命中率下降。**Redirect** 机制将请求转发到目标数据所在的 Server 执行，从而保证修改和查询都在同一台服务器完成，Cache 命中率极高。整体架构参见 → [arch](./arch)。

## 三种 Redirect 注解

Zeze 提供了三种 Redirect 注解，覆盖不同的跨服调用场景：

| 注解 | 路由方式 | 典型场景 |
|---|---|---|
| `@RedirectHash` | 按 hash 值选择 Server | 排行榜更新、帮派操作 |
| `@RedirectAll` | 广播到所有分组并收集结果 | 排行榜全局查询 |
| `@RedirectToServer` | 直接指定 ServerId | 定时器取消、Rpc 调用 |

## @RedirectHash

按 hash 值将请求路由到某一台 Server 执行。被标记方法的第一个参数必须是 `int hash`（或通过 `@RedirectKey` 标记的参数），框架据此计算目标 Server。

```java
@RedirectHash(ConcurrentLevelSource = "getConcurrentLevel(keyHint.getRankType())")
public RedirectFuture<Long> updateRank(int hash, BConcurrentKey keyHint, long roleId, Bean value) {
    // 实际在目标 Server 上执行
    return RedirectFuture.finish(0L);
}
```

### 注解参数

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RedirectHash {
    boolean oneByOne()           default true;
    String  ConcurrentLevelSource() default "";
    int     timeout()            default 30_000;
    int     version()            default 0;
}
```

- **oneByOne**：是否串行执行。默认 `true`，同一 hash 的请求排队依次执行，避免并发冲突。
- **ConcurrentLevelSource**：以字符串形式指定方法调用表达式，返回分组总数（int）。框架据此将数据分散到多台 Server。例如 `"getConcurrentLevel(keyHint.getRankType())"` 会以第二个参数 `keyHint` 为输入调用当前对象的 `getConcurrentLevel` 方法。该参数决定了一致性哈希环的分组粒度。
- **timeout**：超时时间（毫秒），默认 30 秒。
- **version**：方法版本号，用于兼容性升级。

### 参数约定

- 首个参数必须是 `int hash`，框架自动将其作为路由依据。也可用 `@RedirectKey` 标记其他参数代替：
  ```java
  @RedirectHash(timeout = 2000)
  public RedirectFuture<GenericResult<BBeanResult>> TestHashGenericResult(
          int serverId, @RedirectKey Long arg) {
      return RedirectFuture.finish(new GenericResult<>());
  }
  ```
- hash 可能路由到本机（local loop-back），此时方法直接在当前进程内执行。

### 返回类型

- 有返回值：`RedirectFuture<T>`，其中 `T` 可以是自定义结果 Bean、`Long`（resultCode）、`String`、`Binary` 等。
- 无返回值：`void`，即 fire-and-forget 模式。

## @RedirectAll

向所有分组广播请求，并收集每个分组的处理结果。这是一种 **MapReduce** 风格的调用：每个 Server 处理自己负责的分组，发起方汇总所有结果。

```java
@RedirectAll(version = 3)
public RedirectAllFuture<TestToAllResult> TestToAll(int hash, int in) throws Exception {
    // hash 在发起方表示分组总数，在处理方表示当前分组编号
    var result = new TestToAllResult();
    result.out = in;
    return RedirectAllFuture.result(result); // 同步返回结果
}
```

### 注解参数

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RedirectAll {
    int timeout()  default 30_000;
    int version()  default 0;
}
```

- **timeout**：超时时间（毫秒），默认 30 秒。超时后尚未返回的分组结果将被丢弃。
- **version**：方法版本号。

### 参数约定

- 首个参数 `int hash` 的含义与 `@RedirectHash` 不同：**在发起方表示分组总数，在处理方表示当前分组的编号**。
- 返回类型为 `RedirectAllFuture<R>`（R 继承 `RedirectResult`），或 `void`（不需要收集结果）。

### RedirectAllFuture 用法

`RedirectAllFuture<R>` 支持同步和异步两种创建方式：

```java
// 同步结果：直接返回已知结果
return RedirectAllFuture.result(new MyResult());

// 异步结果：稍后通过 future.asyncResult() 设置
var future = RedirectAllFuture.<MyResult>async();
Task.run(() -> { future.asyncResult(new MyResult()); return Procedure.Success; });
return future;
```

链式回调：

```java
future
    .onResult(result -> {
        // 每个分组返回时触发，不并发，安全访问共享数据
    })
    .onAllDone(ctx -> {
        // 所有分组完成后触发一次
        var allResults = ctx.getAllResults(); // IntHashMap<R>
    })
    .await(); // 也可同步阻塞等待全部完成
```

- **onResult**：每收到一个分组的结果触发一次回调。同一个 future 内不会与 `onAllDone` 并发。
- **onAllDone**：所有分组结果收集完毕后触发一次，参数为 `RedirectAllContext<R>`，通过其 `getAllResults()` 获取完整的 `IntHashMap<R>` 结果集。

## @RedirectToServer

直接指定目标 ServerId 进行调用，相当于一种便利的 **RPC** 机制。

```java
@RedirectToServer(version = 1)
public RedirectFuture<TestToServerResult> TestToServer(int serverId, int in) {
    var result = new TestToServerResult();
    result.setOut(in);
    result.setServerId(App.Zeze.getConfig().getServerId());
    return RedirectFuture.finish(result);
}
```

### 注解参数

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RedirectToServer {
    boolean oneByOne()           default true;
    boolean orOtherServer()      default false;
    int     timeout()            default 30_000;
    int     version()            default 0;
}
```

- **oneByOne**：是否串行执行，默认 `true`。
- **orOtherServer**：当目标 Server 不可达时，是否自动选择其他可用的 Server。默认 `false`，不可达时抛出 `RedirectException`。
- **timeout**：超时时间（毫秒），默认 30 秒。
- **version**：方法版本号。

### 参数约定

- 首个参数必须是 `int serverId`，指定目标 Server 的 ID。
- 如果 `serverId` 等于当前进程的 ServerId，方法在本地执行（loop-back）。
- 同样支持 `@RedirectKey` 标记其他参数作为路由键。
- 目标 Server 不存在或未连接时，抛出 `RedirectException`（code = `SERVER_NOT_FOUND`），除非设置了 `orOtherServer = true`。

## RedirectFuture 异步结果

`RedirectFuture<R>` 是 `@RedirectHash` 和 `@RedirectToServer` 的返回类型，用于异步获取跨服调用结果。

### 创建方式

```java
// 同步完成：结果已知，直接返回
return RedirectFuture.finish(result);

// 异步完成：稍后在其他地方调用 setResult 设置结果
var future = new RedirectFuture<MyResult>();
Task.run(App.Zeze.newProcedure(() -> {
    future.setResult(computedResult);
    return Procedure.Success;
}), "MyRedirect");
return future;
```

### 回调与等待

```java
future
    .onSuccess(result -> { /* 调用成功 */ })
    .onFail(ex -> { /* 调用失败，ex.getCode() 判断错误类型 */ })
    .await(); // 同步阻塞等待结果
```

- **onSuccess**：成功回调，结果就绪时触发。
- **onFail**：失败回调，参数为 `RedirectException`。
- **then**：无论成功失败都执行（成功传入结果，失败传入 null）。
- **await**：阻塞当前线程直到结果就绪，返回 future 自身。

### RedirectException 错误码

| code | 常量 | 含义 |
|---|---|---|
| 0 | `GENERIC` | 未知通用错误 |
| 1 | `SERVER_NOT_FOUND` | 目标 Server 未连接或未注册 |
| 2 | `SERVER_TIMEOUT` | 远程执行超时，不确定是否已执行 |
| 3 | `LOCAL_EXECUTION` | 本地执行异常，`getCause()` 获取原异常 |
| 4 | `REMOTE_EXECUTION` | 远程执行出现严重错误 |

## Redirect 服务就绪问题

Redirect 是 Server 之间直连的服务，其发现机制依赖模块注册。在 `startLast` 之后模块才注册到 ServiceManager，其他 Server 才能感知并建立连接。最佳实践：

1. **Redirect 实现应在 `startLast` 之前准备好**，模块注册时服务即可用。
2. **推荐按不可靠服务使用 Redirect**，服务不存在或失败时调用者应能容忍错误（降级或重试）。
3. **对于 `@RedirectToServer`**，可通过自定义注册中心控制：Server 完全准备好后才注册，避免过早暴露。
4. **`startLast` 之后仍有初始化的服务**，需在收到请求后自行判断并拒绝。
Server 启动顺序和就绪控制机制的完整说明，参见 → [arch](./arch)。
