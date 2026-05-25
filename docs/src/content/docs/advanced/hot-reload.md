---
title: "热更新"
sidebar:
  order: 1
---

Zeze 提供了一套完整的模块级热更新机制，可以在不停止服务的情况下更新业务逻辑代码。热更新基于 Java Agent 的类重定义能力和自定义 ClassLoader 实现，支持模块独立升级、状态迁移和回滚。

## 整体架构

Zeze 的热更新系统由以下核心组件构成：

| 组件 | 职责 |
|------|------|
| **ClassReloader** | Java Agent 入口，提供类重定义的底层能力 |
| **HotModule** | 自定义 ClassLoader，每个热更模块一个实例，负责加载模块的 class 文件 |
| **HotService** | 热更模块的生命周期接口，定义 start/stop/upgrade 方法 |
| **HotManager** | 热更管理器，协调整个热更流程 |
| **HotAgent** | 热更客户端，用于连接到 HotDistribute 服务上传更新文件 |
| **HotDistribute** | 热更发布控制台，管理发布的状态机和文件传输 |

## ClassReloader 原理

**ClassReloader**（`Zeze.Util.ClassReloader`）是热更新的基础设施，使用 Java Instrumentation API 实现运行时类重定义。

### Java Agent 模式

ClassReloader 支持两种加载方式：

```bash
# 方式1：启动时通过 -javaagent 参数加载
java -javaagent:zeze.jar -jar myapp.jar

# 方式2：运行时自动附加到当前 JVM
# ClassReloader.getInst() 会自动创建临时 agent jar 并 attach
```

当调用 `getInst()` 发现 `inst` 为 null 时，会自动执行以下流程：

1. 创建包含 `ClassReloader` 自身的临时 agent jar。
2. 获取当前 JVM 的进程 ID。
3. 通过 `VirtualMachine.attach(pid).loadAgent(jarPath)` 将 agent 注入当前进程。

### 类重定义 API

```java
// 重定义单个类
ClassReloader.reloadClass(classData, classLoader);

// 批量重定义
ClassReloader.reloadClasses(classDatas, classLoader);

// 从 zip/jar 文件重定义
int count = ClassReloader.reloadClasses(new ZipFile("update.jar"));
```

`reloadClasses` 从 zip/jar 文件批量加载 class 数据，会自动跳过与当前已加载版本相同的 class（避免不必要的重定义）。

### 类名解析

`getClassPathFromData` 方法直接解析 class 文件的二进制格式（常量池）来获取完整类名，不依赖 ClassLoader，确保在任何环境下都能工作。

## HotModule -- 热更模块加载器

**HotModule**（`Zeze.Hot.HotModule`）继承自 `ClassLoader`，每个热更模块对应一个实例。它的核心设计原则：

### 隔离机制

每个 HotModule 实例以 jar 文件为边界，加载其中的所有 class（接口除外）。模块之间相互隔离，同一模块的不同版本可以共存。

```java
// 模块的入口类命名规则：{namespace}.Module{lastPart}
// 例如 namespace="Game.Rank" 时，入口类为 Game.Rank.ModuleRank
```

### 生命周期

```
创建 HotModule -> 注册到 HotManager -> start() -> startLast() -> 运行中
                                                         |
             upgrade(newModule) <- stopBefore() <- stop()
```

- **start/startLast**：初始化模块，注册协议和数据表。
- **stopBefore**：停机前调用，此时应用环境仍然完整。
- **stop**：执行 UnRegister 和清理。
- **upgrade**：从旧版本迁移状态到新版本。`contexts` 会从旧模块转移到新模块。

### 版本化 Context

HotModule 通过 `HotModuleContext<T>` 管理模块的外部引用。升级时 context 自动迁移到新模块，stop 时设置为 null（disable），防止外部模块持有过期引用。

```java
// 获取模块的上下文
HotModuleContext<MyService> ctx = hotModule.getContext(MyService.class);
```

## HotService -- 模块生命周期接口

**HotService**（`Zeze.Hot.HotService`）定义了热更模块必须实现的接口：

```java
public interface HotService {
    void start() throws Exception;
    default void startLast() throws Exception {}
    default void stopBefore() throws Exception {}
    void stop() throws Exception;
    void upgrade(HotService old) throws Exception;
}
```

- **start**：初始化资源、注册协议和数据表。
- **stop**：释放资源。如果服务有状态，stop 时需要保留状态供 upgrade 读取。
- **upgrade**：从旧服务实例迁移状态。开发者在此方法中读取 `old` 的数据并初始化新实例。

## 热更新流程

### 完整发布流程

```
1. 准备：HotAgent 连接到 HotDistribute
2. PrepareDistribute：进入准备状态，锁定发布通道
3. 文件传输：openFile -> appendFile(多次) -> closeFile(MD5校验)
4. TryDistribute：HotManager 执行 tryDistribute，触发模块升级
5. Commit/Commit2：确认升级成功
6. 如果失败：TryRollback 回滚
```

### 模块升级步骤

当 HotManager 执行 tryDistribute 时，内部流程如下：

1. **扫描发布目录**（`HotDistributeDir`）中的新 jar 文件。
2. 为每个 jar 创建新的 **HotModule** 实例，加载 class 并查找入口类。
3. 调用旧模块的 **stopBefore** -> **stop**。
4. 调用新模块的 **start** -> **startLast**。
5. 调用新模块的 **upgrade(oldModule)** 迁移状态。
6. 通知所有注册的 **HotUpgrade** 和 **HotBeanFactory** 刷新缓存数据。

## 配置

在 `zeze.xml` 中启用热更新：

```xml
<zeze ...
    HotWorkingDir="/path/to/working/dir"
    HotDistributeDir="distributes"
    ...>
</zeze>
```

- **HotWorkingDir**：热更新模块的工作目录。默认为当前目录。
- **HotDistributeDir**：发布文件存放的子目录名。默认为 `distributes`。

## 注意事项

1. **接口不能修改**：热更模块中对外暴露的接口（interface）不应修改签名。接口由父 ClassLoader 加载，不会被热更替换。

2. **数据兼容性**：`upgrade` 方法中必须处理新旧数据格式的兼容。如果 Bean 结构变化，需要实现 `HotUpgrade.upgrade` 方法中的 `retreatFunc` 逻辑。

3. **BeanFactory 注册**：模块中使用的自定义 Bean 类型必须通过 `BeanFactory.register` 注册，持久化时会保存类名，升级后通过类名反序列化。

4. **stop 事件**：HotModule 的 `stopEvents` 集合用于通知依赖方（如 Online）清理对旧模块的引用。注册了本地数据（`setLocalBean`）的模块会自动注册到 stop 事件中。

5. **不建议频繁热更**：虽然热更机制支持多次升级，但每次升级都会创建新的 ClassLoader 和 JarFile 句柄，频繁热更可能导致元空间（Metaspace）泄漏。

6. **Raft 线程安全**：热更过程中涉及的模块停止和启动操作在 `hotGuard` 保护下执行，确保与正在运行的 Raft 操作不冲突。
