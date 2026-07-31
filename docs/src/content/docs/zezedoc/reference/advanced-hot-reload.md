---
title: "热更新"
description: "Zeze 模块级热更新：基于 Java Agent 类重定义与自定义 ClassLoader，不停服升级业务逻辑、迁移状态并回滚"
category: reference
order: 70
---

本文是 Zeze **热更新（Hot Reload）** 的完整参考——阐述其模块级不停服更新的原理、核心组件、发布流程、状态迁移与回滚机制，以及工程上的注意事项，供设计热更方案、排查热更问题时查阅。事务与配置基础见 [事务](./transaction.md)、[配置参考](./configuration.md)，上线前的热更验收见 [上线清单](../manual/08-production-checklist.md)。

Zeze 提供完整的**模块级热更新**能力：不停服更新业务逻辑、基于 Java Agent 的类重定义能力和自定义 ClassLoader 支持模块独立升级、状态迁移与回滚。整套机制以「一个 jar 一个热更模块」为边界，模块之间相互隔离，同一模块的不同版本可在升级窗口内共存。

---

## 核心组件一览

| 组件 | 所在包 | 职责 |
|------|--------|------|
| `ClassReloader` | `Zeze.Util` | Java Agent 入口类，提供运行时类重定义的底层能力 |
| `HotModule` | `Zeze.Hot` | 自定义 `ClassLoader`，每个热更模块一个实例，加载该模块 jar 内的所有 class（接口除外） |
| `HotService` | `Zeze.Hot` | 热更模块生命周期接口：`start` / `stop` / `upgrade` |
| `HotManager` | `Zeze.Hot` | 热更管理器，协调整个加载、卸载、升级、回滚流程 |
| `HotAgent` | `Zeze.Hot` | 热更客户端，连接 `HotDistribute` 上传发布文件 |
| `HotDistribute` | `Zeze.Hot` | 热更发布控制台，管理发布状态机和文件传输 |

辅助组件：`HotModuleContext`（跨模块引用的版本化上下文）、`HotUpgrade` / `HotBeanFactory`（缓存刷新）、`HotGuard`（热更期间与 Raft 等操作的并发保护）、`HotTransaction`（安装过程的事务包装）。

---

## ClassReloader：类重定义底层能力

`Zeze.Util.ClassReloader` 使用 Java Instrumentation API 进行运行时类重定义。它支持两种加载方式：

**1. `-javaagent` 参数启动加载**

在 `MANIFEST.MF` 中声明（`Premain-Class` 可换成 `Agent-Class`），并以启动参数注入：

```java
// MANIFEST.MF
// Premain-Class: Zeze.Util.ClassReloader
// Can-Redefine-Classes: true
java -javaagent:zeze.jar ......
```

`premain(args, inst)` 将注入的 `Instrumentation` 保存到静态字段 `inst`。

**2. 运行时自动 attach**

调用 `ClassReloader.getInst()` 时若发现 `inst` 为 `null`，则自动创建一个临时 agent jar 并注入当前 JVM：

```java
public static Instrumentation getInst() {
    return inst != null ? inst : loadAgent();
}
```

`loadAgent()` 的关键步骤：写一个临时 `agent.jar`（包含 `Agent-Class`、`Premain-Class`、`Can-Redefine-Classes`、`Can-Retransform-Classes`），获取当前 JVM pid（`ManagementFactory.getRuntimeMXBean().getName()` 取 `@` 之前的部分），再用 `VirtualMachine.attach(pid).loadAgent(jarPath)` 注入：

```java
String pid = nameOfRunningVM.substring(0, nameOfRunningVM.indexOf('@'));
int r = Runtime.getRuntime().exec(new String[]{"java", "-cp", path, fullClassName, pid, path}).waitFor();
```

**类重定义 API**

| 方法 | 说明 |
|------|------|
| `reloadClass(byte[] classData, ClassLoader classLoader)` | 热更单个 class |
| `reloadClasses(Collection<byte[]> classDatas, ClassLoader classLoader)` | 批量热更多个 class |
| `reloadClasses(ZipFile zipFile)` | 从 zip/jar 批量加载，自动跳过同版本，避免不必要的重定义 |
| `getClassPathFromData(byte[] classData)` | 直接解析 class 二进制的常量池获取完整类名，不依赖 ClassLoader |

`reloadClasses(ZipFile, classLoader, log)` 重载会逐项与 `classLoader.getResourceAsStream(name)` 的现有字节比对，**一致则跳过**（`buf0.equals(buf1)`），从而避免不必要的重定义。`getClassPathFromData` 自行解析 `.class` 常量池（`CONSTANT_Class`、`CONSTANT_Utf8` 等）得到类名，因此重定义前不需要先通过 ClassLoader 找到类。

---

## HotModule：模块级 ClassLoader

`HotModule extends ClassLoader implements Closeable`。**每个热更模块一个实例**，以一个 jar 文件为边界，加载其中所有 class（接口除外，接口由父 `ClassLoader` 加载）。

- **模块隔离**：模块间相互隔离；同一模块不同版本可在升级窗口内共存。
- **入口类命名**：`{namespace}.Module{lastPart}`，其中 `last(namespace)` 取 namespace 最后一段。例如 namespace 为 `MySolution.MyName` 时，入口类为 `MySolution.MyName.ModuleMyName`：

```java
var moduleClassName = namespace + ".Module" + last(namespace);
this.moduleClass = loadClass(moduleClassName);
```

- **加载规则**：重写 `findClass` / `loadClass`，从该模块的 `JarFile` 读取 `class` 字节后 `defineClass`。接口（`.interface.jar`）由 `HotManager` 这一层负责装载，不会被 `HotModule` 替换。

### 生命周期

```
创建 HotModule → 注册 HotManager → start() → startLast() → 运行中
                                                            │
                                              upgrade(newModule)
                                                            ▲
                                              stopBefore() ─┘
                                              stop()
```

| 阶段 | 做什么 |
|------|--------|
| `start()` | 初始化资源、注册协议数据表；重新为 `contexts` 设置当前模块引用 |
| `startLast()` | 依赖就绪后的二次初始化（在所有模块 `start` 之后再统一调用） |
| `stopBefore()` | 停机前调用，此时应用环境完整，可做依赖性的预清理 |
| `stop()` | 执行 `UnRegister`（注销协议）、释放资源、关闭 `JarFile`。**不清除本地进程状态**——有状态需保留供 `upgrade` 读取 |
| `upgrade(HotModule old)` | 把旧模块的 `contexts` 迁移到新模块，并调用 `service.upgrade(old.service)` 迁移状态 |

### 版本化 Context

`HotModuleContext<T extends HotService>` 用于管理外部模块对本模块服务的引用。**升级时 context 自动迁移到新模块**：

```java
public void upgrade(HotModule old) throws Exception {
    contexts.putAll(old.contexts);          // 继承旧模块的全部 context
    for (var context : contexts.values())
        context.setModule(this);            // 把 context 指向新模块
    service.upgrade(old.service);           // 业务状态迁移
}
```

`stop` 时（实为 `disable()`）会把每个 context 的 `module` 置为 `null`，**防止外部模块持有过期引用**：

```java
void disable() {
    for (var context : contexts.values())
        context.setModule(null);
}
```

获取引用：`HotModule.getContext(Class<T>)`，内部用 `ConcurrentHashMap` 懒初始化。

---

## HotService：生命周期接口

```java
public interface HotService {
    void start() throws Exception;
    default void startLast() throws Exception {}
    default void stopBefore() throws Exception {}
    void stop() throws Exception;
    void upgrade(HotService old) throws Exception;
}
```

| 方法 | 语义要点 |
|------|----------|
| `start()` | 初始化资源、注册协议数据表 |
| `startLast()` | 默认空实现；`start` 全部完成后的二次初始化 |
| `stopBefore()` | 默认空实现；停机前、应用环境完整时调用 |
| `stop()` | **必须保留有状态的数据**，后面 `upgrade` 时由新实例读取。负责 `UnRegister` 与释放资源 |
| `upgrade(HotService old)` | 从旧实例迁移状态到新实例 |

> `stop` 与 `upgrade` 是配合使用的：`stop` 只断开外部接入并保留状态，`upgrade` 把保留的状态搬过来。实现 `HotService` 时务必遵守这一约定。

---

## HotManager：安装与升级编排

`HotManager extends ClassLoader`，全局一般一个实例，负责装载所有模块接口、监视发布目录、协调升级与回滚。关键字段：

```java
private final String workingDir;                                 // 工作目录
private final String distributeDir;                              // 发布文件存放子目录
private final FewModifySortedMap<String, HotModule> modules;     // namespace -> HotModule
private final ReentrantReadWriteLock hotLock;                    // 热更读写锁
private final ConcurrentHashSet<HotUpgrade> hotUpgrades;         // 缓存刷新
private final ConcurrentHashSet<HotBeanFactory> hotBeanFactories;
private final DistributeManager distributeManager;
private final HotDistribute hotDistribute;
```

**发布目录监视**：`start()` 后用调度线程周期性执行 `tryDistribute(false)`：

```java
Task.getScheduledThreadPool().scheduleAtFixedRate(
        () -> tryDistribute(false), 10000, 10000, TimeUnit.MILLISECONDS);
Task.hotGuard = this::enterReadLock;   // 热更期间保护运行中的操作
```

`tryDistribute` 检查 `{distributeDir}/ready` 标记文件是否存在；存在则读取其中的模块列表并触发 `installReadies(atomicAll)`。

### 模块升级步骤（install 流程）

`install(namespaces, atomicAll)` 是核心，步骤如下：

1. **锁外执行 `stopBefore`**：对每个待升级的现有模块调用 `stopBefore()`。
2. **进入写锁**：执行 `checkpointRun()` 先持久化现有数据；从 `modules` 中移除待升级 namespace。
3. **逆序 `stop`**：从后往前对旧模块调用 `stop()`（`UnRegister`、释放资源、保留状态）。若中途异常，调用 `recoverModules` 恢复。
4. **加载 Schemas**：`loadSchemas()` 从 `__hot_schemas__{SolutionName}.jar` 加载并切换 schemas（旧 schemas 保存用于回滚）。
5. **安装新模块**：为每个 namespace 从 `{distributeDir}/{namespace}.jar` 与 `{namespace}.interface.jar` 创建新 `HotModule` 并 `put` 进 `modules`；接口 jar 备份旧文件以便回滚。
6. **`createModuleInstance`**：批量装载 redirect 模块，创建 `IModule` 实例。
7. **`__install_alter__()`**：在切换后的 schemas 下执行结构变更。
8. **`upgrade(old)`**：对已存在的旧模块，调用 `module.upgrade(exist)` 把状态迁移到新模块（事务外运行）。
9. **`stopInternal` 旧模块**：内部停止、不可恢复——清理 `contexts`、触发 `stopEvents`。
10. **内部 `upgrade`**：对 `hotUpgrades`（缓存了其他模块数据）调用 `HotUpgrade.upgrade(retreatFunc)`；对 `HotBeanFactory` 刷新 Bean 类型注册；对 `__get_upgrade_memory_table__()` 执行内存表升级。
11. **`start()`**：按序启动新模块，启动失败的模块会被停止并注销。
12. **`sendCommitResultAndWaitCommit2`**：原子发布时通知发布控制台最终提交。
13. **`startLast()`**：最后统一调用 `startLast()`（忽略错误）。

> 关键设计：步骤 8 之后进入「不能出错阶段」——任何异常将 `Runtime.getRuntime().halt(111222)` 强制停机，因为此时数据结构变更已部分落地，无法回滚。这也是为什么热更要充分测试。

### 回滚

`install` 用 `HotTransaction` 包装，`whileRollback` 注册 `MainRollbackAction`：

- 恢复旧 schemas：`zeze.__upgrade_schemas__(oldSchemas)`
- 恢复被停止的模块：`recoverModules(exists, -1)`（重新 `Register`、`start`、加回 `modules`）
- 重新 alter：`zeze.__install_alter__()`
- 清理内存表升级记录：`zeze.__get_upgrade_memory_table__().clear()`

模块文件层面的回滚：安装前把旧 `module.jar` / `interface.jar` 重命名为 `.backup`，`whileRollback` 时还原，`whileCommit` 时删除备份。

---

## 发布流程（HotAgent ⇄ HotDistribute）

完整发布是一个带状态机的文件传输过程：

```
1. HotAgent 连接 HotDistribute
2. PrepareDistribute        进入准备，锁定发布通道
3. 文件传输                  openFile → appendFile（多次）→ closeFile（MD5 校验）
4. TryDistribute            HotManager 执行 tryDistribute，触发模块升级
5. Commit / Commit2         确认成功；失败则 TryRollback 回滚
```

- **准备阶段**：`PrepareDistribute` 锁定发布通道，防止并发发布冲突。
- **文件传输**：`openFile` 打开目标文件，`appendFile` 分块上传（多次），`closeFile` 做 **MD5 校验**确认完整。
- **触发升级**：`HotManager.tryDistribute(true)` 在原子模式下执行 `installReadies(atomicAll)`，并在关键节点通过 `hotDistribute.sendTryDistributeResultAndWaitCommit` / `sendCommitResultAndWaitCommit2` 与发布控制台同步状态。
- **提交/回滚**：成功走 `Commit`/`Commit2`；失败走 `TryRollback`，`HotManager` 把 `distributes` 目录里的文件 `renameDistributes` 到 `backup/{时间戳}` 子目录。

模块识别规则：`loadExistDistributes` 扫描 `distributeDir` 下的 `.jar`，要求成对出现 `{namespace}.jar` 与 `{namespace}.interface.jar` 才视为 ready；可选的 `start.order.txt` 指定加载顺序。

---

## 配置

热更新相关配置位于 `<zeze>` 根元素：

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `HotWorkingDir` | `""`（空串，即当前目录） | 工作目录，运行态的 `modules/` 与 `interfaces/` 存放于此 |
| `HotDistributeDir` | `distributes` | 发布文件存放子目录名 |

```xml
<zeze HotWorkingDir="./hot"
      HotDistributeDir="distributes"
      ...>
</zeze>
```

`HotManager` 构造时会校验目录关系：`distributeDir` 不能是 `workingDir/interfaces/`、`workingDir/modules/` 的子目录，二者也不能互相包含。

---

## 注意事项

1. **接口不能修改**。接口由父 `ClassLoader`（`HotManager`）加载，热更不会替换接口 class。新增接口方法会破坏兼容。
2. **`upgrade` 处理新旧数据兼容**。Bean 结构变化时需实现 `HotUpgrade.upgrade` 的 `retreatFunc`——当缓存中仍持有旧模块创建的 Bean 时，通过序列化/反序列化把它「撤退」为新模块的类（`HotManager.retreat` 会用新模块的 `ClassLoader` 重新构造一个同结构的 Bean）。
3. **BeanFactory 注册**。模块自定义 Bean 类型须 `BeanFactory.register` 注册；由于持久化保存的是**类名**，升级后类名变化会影响反序列化，热更时框架会通过 `BeanFactory.resetHot` 重建注册表。
4. **`stop` 事件**。`HotModule.stopEvents` 用于通知依赖方（如 `Online`）清理旧模块引用。注册本地数据 `setLocalBean` 会自动注册对应的 stop 事件。
5. **不建议频繁热更**。每次热更都会创建新的 `ClassLoader` 和 `JarFile` 句柄，频繁热更可能导致 **Metaspace 泄漏**（旧模块类未及时卸载）。建议低峰期批量热更。
6. **Raft 线程安全**。热更过程通过 `hotGuard` 保护（`Task.hotGuard = this::enterReadLock`，`install` 持写锁），确保与运行中的 Raft 等操作不冲突。

---

## 延伸阅读

- [事务](./transaction.md) — 热更与 Checkpoint、事务边界的关系
- [配置参考](./configuration.md) — `HotWorkingDir` / `HotDistributeDir` 等完整配置
- [上线清单](../manual/08-production-checklist.md) — 热更上线前应验收的检查项
