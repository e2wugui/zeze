---
title: "版本发布与滚动更新"
sidebar:
  order: 3
---

Zeze 支持多种版本发布与更新策略，从完全重启到基于 ClassLoader 的模块热更新。本文重点描述**滚动更新**流程中的优雅停止机制、**Schemas** 版本兼容检查以及热更新的配合方案。

## 更新策略概览

### 完全重启

停止全部服务器，全部更新，全部重启。更新频率不会太高，重启够快，这个方案最简单，虽然之后会出现一定的登录高峰，但一般都扛得住。系统规模越来越大，更新频率可能也会越来越高，会导致大量重启，此时需要考虑其他方式。**这个方案总是可用**，在下面的方案无法使用时，总是可以选择它。

### 逐台更新服务器

服务器本身是多台的，可以一台一台更新并重启。重启服务器上的用户重新登录，只要把重新登录处理得对用户比较无感，就能实现接近无缝的升级。整个过程可能需要一定时间，此时新旧版本同时存在，需要应用具有新旧同时存在的兼容性。

### Instrumentation 热更

Java 自带的 Instrumentation 提供了任意 class 热更新机制，可以在不重启服务器的基础上把新的 class 转载进去，保留所有服务器状态，完全无感无缝。这种热更有一定限制，一般用于紧急 BUG 修复、临时禁用某些功能、增加日志进行调试等。具体使用限制请查阅 Instrumentation 的文档。涉及更新多台服务器时，一般也是一台一台独立进行，很短的时间内存在新旧版本同时运行的时刻，需注意新旧版本兼容性。当然一般 Instrumentation 式热更新，新旧版本问题一般不大。

### 模块热更新

基于 ClassLoader，以模块为单位热更，不重启服务器。一次更新可包含多个模块。存在新旧版本服务器同时运行，需要具有兼容性，也可以提供原子的更新所有服务器来解决。这个热更新方式对开发模式有要求，不能随意使用。服务启用了模块热更新后，除了模块，服务中还有一部分类是不支持热更新的。模块热更新也不是所有模块都支持，可以配置选择哪些支持。模块开发中，新版必须兼容旧版，兼容性是基本需求。

| 策略 | 停机时间 | 适用场景 |
|------|----------|----------|
| 完全重启 | 全部 | 任何场景，最简方案 |
| 逐台更新 | 接近零 | 日常版本发布 |
| Instrumentation 热更 | 零 | 紧急 BUG 修复 |
| 模块热更新 | 零 | 按模块粒度迭代 |

## 滚动更新流程

### 1. 标记下线

调用 `setDisableChoiceFromLinks(true)` 通知所有 Linkd 不再向该 Provider 分发新连接。该方法向所有 Linkd 发送 `SetDisableChoice` 协议，已在线用户不受影响。

```java
providerService.setDisableChoiceFromLinks(true);
```

### 2. 等待存量处理完成

等待当前服务器上的在线用户逐步减少，可配合超时踢出或引导重新登录。

### 3. 停止服务

确认存量请求基本处理完毕后，发送 SIGTERM 或调用 stop 方法优雅停止。

### 4. 更新并重启

部署新版本后启动服务。Provider 在 `OnHandshakeDone` 中发送 `AnnounceProviderInfo` 携带当前 `appVersion`，自动向 Linkd 注册并恢复流量分配。

## Schemas 版本兼容检查

**Schemas** 是 Zeze 的数据结构定义元数据。每次启动时，框架会比较当前代码的 Schemas 与数据库中保存的旧版 Schemas，确保兼容性。

### 兼容性规则

启动时 `Schemas.checkCompatible` 自动对比当前代码与数据库中的旧版 Schemas。主要规则：

| 规则 | 说明 |
|------|------|
| Variable.Id 类型不可变 | 同一 ID 的变量类型必须与旧版兼容 |
| 禁止复用已删除的 Variable.Id | 除非类型完全一致（允许"反悔"） |
| BeanKey 变量只增不减 | 被用作 Key 的 Bean 不允许删除变量 |
| Dynamic Bean 映射不可变更 | typeId 到 className 的映射不可修改或删除 |

类型兼容性表（部分）：

```
bool <-> byte <-> short <-> int <-> long <-> float <-> double
string <-> binary
vector2 <-> vector2int (允许互转)
```

### 处理不兼容

检测到不兼容变更时启动会抛出 `IllegalStateException`。可修正定义，或通过 `allowSchemasReuseVariableIdWithSameType` 配置放宽部分限制，极端情况下清除数据库。

## 模块热更新开发规范

模块热更新基于 ClassLoader，以模块为单位热更，不重启服务器。一次更新可包含多个模块。启用后，除了模块，服务中还有一部分类不支持热更新。也不是所有模块都支持，需配置选择。

### 接口化

模块提供的所有服务必须都是接口。模块实现的接口是主接口，如果存在其他接口，必须由主接口得到。接口方法可以使用非模块内定义的类型：Java 基本类型、JDK 容器、第三方库、Zeze 类型等。模块内定义的类如果需要公开也必须接口化。Bean 也是模块内自定义的类型，不能用于接口方法的参数。

### 接口引用保存规则

原则上不保存任何模块服务的接口引用。模块主接口特殊处理，引用其他模块主接口时，可按下面方式保存上下文：

```java
IModuleSome getSomeService() {
    if (this.moduleSome == null)
        this.moduleSome = HotManager.get("MySol.ModuleSome", IModuleSome.class);
    return moduleSome.getService();
}
```

### 新版接口兼容性

新版接口必须兼容旧版接口，旧版本接口一旦发布就不能再修改。例：`IModuleSome2 extends IModuleSome`。这点很重要，再重复一次。

### Start/Stop 实现要求

Start、Stop 需要允许反复执行，热更的启动停止不能破坏状态。Stop 之后再 Start，必须能继续正常执行。可用 `isHotUpgrading()` 区分第一次启动、最后一次关闭和中间热更的启停：

```java
private boolean isHotUpgrading() {
    var hotManager = App.Zeze.getHotManager();
    if (null == hotManager) return false;
    return hotManager.isUpgrading();
}
```

**Start 实现过程必须都是同步调用，不能等待其他线程。** 热更过程中 Start 处于写锁内，启用新线程会造成死锁。

### 无状态模块

模块除了使用 Zeze 的 table 等服务，没有任何自定义数据（状态）。除了没有定义自己的变量，还需要注意 `java.ScheduleThreadPool.schedule` 调用也是程序状态。对于 schedule，如果它在 stop 被停止、在 start 时重新启动，那么状态相当于被自动恢复，此时仍然可以看作"无状态"（算半无状态）。Zeze 的 Table 在模块更新时状态会被自动刷新。其他服务有时候也是有状态的，可能需要使用特别的版本（见后面 Zeze 服务限制）。Zeze 的服务在热更下都会提供解决方案，而除了 Zeze 服务，程序没有自己状态，就满足了"无状态"条件。这种情况下的热更，开发除了上面几点（如兼容性等要求外），不需要更多额外的支持，就可以轻松支持模块热更了。

### 有状态模块

模块具有状态时，更新时需要处理状态迁移和刷新问题。分为两种：

**模块状态是自己内部创建的变量**：热更时，系统调用 `HotService.upgrade(HotService old)` 通知模块进行数据迁移。由于 old 是接口，所以需要支持迁移的模块还需要在接口中定义访问旧状态的方法。完全无缝迁移对于某些复杂模块，或者当修改比较大时，是比较困难的。这时应用上可以采取让玩家重新登录的方式，避免热更新时迁移状态。举个例子：游戏的地图服务器，可选的保存一些状态（所在地图位置方向），然后把玩家踢下线，玩家重新登录时恢复这些状态。

**模块状态保存了来自其他模块得到的数据引用**：分析这个问题比较复杂，定义一个 refresh 接口远远不够。涉及两个基本问题：A) 使用了哪些模块？怎么通知？B) 数据引用一般随着逻辑功能执行的过程保存下来的，这个刷新看来是不可能完成的任务。推荐的规则是**禁止保存来自其他模块的数据**。这能确保没有这个问题，但对应用限制比较大。另一个办法是对 Bean 的发布和热更进行特殊处理，使得 Bean 可以在接口中使用，并且作为返回值时也可以保存下来。

### Timer 的使用模式

这里的 Timer 特指 `Zeze.Component.Timer`。如果是 Java 自带的线程池的 Timer，可以参考这里说的，自行判断处理方式。

**start/stop 模式**：热更模块在 start 时注册的 Timer，在 stop 时注销。这种情况下不需要对 timer 进行额外处理。

**继承模式**：当 Timer 的注册是跟随逻辑走的，或者想保持一次注册、以后延续固定节奏，那么就不要重新注册，而是在 upgrade 时从旧接口把已注册的 timerId 得到，保存到新模块实例内。继承模式在注册时需要判断模块是第一次启动还是处于热更中：

```java
private boolean isHotUpgrading() {
    var hotManager = App.Zeze.getHotManager();
    if (null == hotManager) return false;
    return hotManager.isUpgrading();
}

void start() {
    Zeze.newProcedure(() -> {
        if (!isHotUpgrading())
            timerInherit = Zeze.getTimer().schedule(…); // 第一次启动注册
        return 0;
    }, "register timer").call();
}

void stop() {
    Zeze.newProcedure(() -> {
        if (!isHotUpgrading())
            Zeze.getTimer().cancel(timerInherit); // 程序退出，不是热更中
        return 0;
    }, "unregister timer").call();
}

void upgrade(HotService old) {
    var iMyHotService = (IMyHotService) old;
    timerInherit = iMyHotService.getTimerInherit(); // 继承过来
}
```

### Zeze 服务限制

启用模块热更新后，部分 Zeze 服务有限制：

```
A) 只能由应用的非热更代码调用，Zeze 会阻止违规调用：
   - Zeze.Component.TimerAccount & TimerRole
     scheduleOnline(… TimerHandle handle …)
     scheduleOnlineNamed(… TimerHandle handle …)
   - Zeze.Util.EventDispatcher（用于 Online）
     add(mode, handle) 调用者必须来自非热更模块

B) 热更新模块必须使用接口版本：
   - TimerAccount & TimerRole
     scheduleOnlineHot(… Class<? extends TimerHandle> handleClass …)
   - EventDispatcher
     addHot(mode, handleClass) 热更模块必须使用这个接口注册事件
```

### 模块热更新启用配置

- `project hot="true"` — 需要启用模块热更新时首先配置，默认 false
- `module hot="true"` — 每个需要热更新的模块单独配置，默认 false

### Redirect 接口参数规范

Redirect 接口方法直接公开到接口中，参数和结果在 Bean 能作为参数之前不能使用 Bean。临时方案：当 Redirect 结果是一个 Bean 时，把这个 Bean.class 发布到非热更部分，不进行热更。结构修改时定义新 Bean，旧方法和 Bean 保留。

- `RedirectAllFuture<MyRedirectResult>` 在 XML 中定义特殊 Bean：`<bean name="MyRedirectResult" RedirectResult="true"/>`
- 其他参数或结果如果使用自定义结构，只能是 Bean、Data 或 BeanKey

## 相关章节

- [Prometheus 监控集成](../advanced/metrics.md)：通过监控指标评估更新影响。
- [Maven 中央仓库发布](./maven-deploy.md)：版本发布的前置构建流程。
