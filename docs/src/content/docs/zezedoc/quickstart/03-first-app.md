---
title: "第一个应用"
description: "五分钟搭一个能跑的 Zeze 应用：XML 定义 → 生成代码 → 写业务逻辑"
category: quickstart
order: 3
---

# 第一个应用

> 读完这篇，你会拥有一个完整可跑的 Zeze 应用：定义角色数据、处理「增加经验并升级」的业务逻辑，并理解事务如何自动保护你的数据。

Zeze 的核心工作流只有三步：**在 XML 里定义数据 → 跑代码生成器 → 在生成的骨架里填业务逻辑**。本篇用一个「角色经验 + 背包」的完整例子带你走完全程。

## 第一步：定义数据结构（solution.xml）

`solution.xml` 是 Zeze 的核心建模文件，它描述所有的数据结构（**Bean**）、持久化存储表（**Table**）和网络协议（**RPC**）。框架据此生成 Java 代码，你只管业务本身。

下面这份 `solution.xml` 可以直接用：

```xml
<?xml version="1.0" encoding="utf-8"?>
<solution name="QuickStart" ModuleIdAllowRanges="1-100">
  <!-- 引入 Zeze 框架基础定义，路径按你的工程结构调整 -->
  <import file="../ZezeJava/solution.zeze.xml"/>

  <module name="Role" id="1">
    <!-- 角色数据结构 -->
    <bean name="BRole">
      <variable id="1" name="Level" type="int"/>
      <variable id="2" name="Experience" type="long"/>
    </bean>

    <!-- 背包数据结构 -->
    <bean name="BBag">
      <variable id="1" name="Items" type="list[int]"/>
    </bean>

    <!-- 持久化存储表：key 是 roleId -->
    <table name="tRole" key="long" value="BRole"/>
    <table name="tBag" key="long" value="BBag"/>

    <!-- 增加经验的 RPC 参数 -->
    <bean name="BAddExperience">
      <variable id="1" name="Experience" type="long"/>
    </bean>

    <!-- 增加经验的 RPC，handle="server" 表示由服务端处理 -->
    <rpc name="AddExperience" argument="BAddExperience"
         TransactionLevel="Serializable" handle="server"/>
  </module>

  <!-- 项目定义 -->
  <project name="GameServer" GenDir="GameServer/Gen"
           SrcDir="GameServer/src" platform="java">
    <service name="Server" handle="server">
      <module ref="Role"/>
    </service>
  </project>
</solution>
```

几个要点：

- **`<solution>`**：根元素，`name` 既是解决方案名，也是生成代码的根包名（如 `QuickStart.Role.BRole`）。
- **`<module>`**：逻辑模块，`id` 在整个解决方案里必须唯一。
- **`<bean>`**：数据结构，类似 C 的 struct。详见 [Bean 参考](../reference/bean.md)。
- **`<table>`**：持久化表，Zeze 自动管内存与数据库的同步。
- **`<rpc>`**：远程调用，`handle="server"` 表示请求由服务端处理，生成器会自动产出服务端处理函数骨架。
- **`<project>`**：定义代码输出目录和平台。

> XML 各标签的完整语法见 [solution.xml 参考](../reference/solution-xml.md)。

## 第二步：代码生成

生成器是上一篇装好的 `Gen.exe`：

```bash
# 1. 编译 Gen 工程（需要 .NET 10 SDK，只需一次）
dotnet build Gen

# 2. 在 solution.xml 所在目录执行生成
Gen/bin/Debug/net10.0/Gen.exe solution.xml
```

生成后的目录长这样：

```
solution.xml 所在目录/
└── GameServer/                  # <project name="GameServer">
    ├── Gen/                     # 自动生成，每次重新覆盖，【不要手改】
    │   └── QuickStart/
    │       └── Role/
    │           ├── BRole.java       # Bean 序列化代码
    │           ├── BBag.java
    │           ├── BAddExperience.java
    │           ├── tRole.java       # Table 访问接口
    │           ├── tBag.java
    │           └── AbstractModule.java  # 模块抽象基类
    └── src/                     # 业务代码目录，你在这里写
        └── QuickStart/
            └── Role/
                └── ModuleRole.java  # 模块实现（首次生成后不再覆盖）
```

**两条铁律**：

1. `Gen/` 下的代码每次生成都会被**完全覆盖**，不要手动改。
2. `src/` 下生成的是业务骨架（如 `ModuleRole.java`），只在文件不存在时创建，之后归你管。文件里 `ZEZE_FILE_CHUNK` 标记之间的代码由生成器维护，标记之外归你。

生成的 `ModuleRole.java` 骨架：

```java
package QuickStart.Role;

import QuickStart.App;
import Zeze.Transaction.Procedure;

public final class ModuleRole extends AbstractModule {

    public void Start(App app) {
    }

    public void Stop(App app) {
    }

    @Override
    protected long ProcessAddExperienceRequest(AddExperience rpc) {
        // 在这里编写业务逻辑
        return Procedure.NotImplement;
    }

    // ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleRole(QuickStart.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
```

`ProcessAddExperienceRequest` 返回 `Procedure.NotImplement` 是生成器给的占位符，意思是「还没实现」。我们把它换成真正的逻辑。

## 第三步：编写业务逻辑

下面实现「增加经验 → 经验满了就升级 → 每 10 级发奖励到背包」：

```java
package QuickStart.Role;

import QuickStart.App;
import Zeze.Arch.ProviderUserSession;
import Zeze.Transaction.Procedure;

public final class ModuleRole extends AbstractModule {

    public void Start(App app) {
    }

    public void Stop(App app) {
    }

    @Override
    protected long ProcessAddExperienceRequest(AddExperience rpc) {
        var session = ProviderUserSession.get(rpc);
        long roleId = session.getRoleId();
        long newExperience = rpc.Argument.getExperience();

        // 从表中读取角色数据，不存在则自动创建
        var role = _tRole.getOrAdd(roleId);

        // 累加经验
        role.setExperience(role.getExperience() + newExperience);

        // 经验溢出时升级，每 10 级发放等级奖励到背包
        while (role.getExperience() >= getLevelUpExp(role.getLevel())) {
            role.setExperience(role.getExperience() - getLevelUpExp(role.getLevel()));
            role.setLevel(role.getLevel() + 1);

            if (role.getLevel() % 10 == 0) {
                addItemToBag(roleId, getLevelReward(role.getLevel()));
            }
        }

        session.sendResponseWhileCommit(rpc);
        return Procedure.Success;
    }

    /**
     * 向背包添加物品，背包满时抛出异常触发事务回滚。
     */
    private void addItemToBag(long roleId, int itemId) {
        var bag = _tBag.getOrAdd(roleId);
        if (bag.getItems().size() >= 100) {
            throw new RuntimeException("BagIsFull");
        }
        bag.getItems().add(itemId);
    }

    // 假设从配置表读取，此处简化
    private long getLevelUpExp(int level) {
        return 1000L * level;
    }

    private int getLevelReward(int level) {
        return level * 100;
    }

    // ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleRole(QuickStart.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
```

`_tRole` 和 `_tBag` 是生成器在 `AbstractModule` 里自动创建的表访问对象，直接用即可。

## 见证事务的魔法

这段代码最值得品味的是 `addItemToBag` 里那行 `throw new RuntimeException("BagIsFull")`。

假设玩家升到第 10 级，要往背包发奖励，但背包正好满了。这时候抛异常会发生什么？

**整个事务自动回滚**——之前对 `_tRole`（角色经验、等级）和 `_tBag`（背包）的所有修改全部撤销，就像这次「增加经验」从来没发生过。你**不需要手写任何回滚逻辑**。

这就是 Zeze 事务的核心承诺：`ProcessXxxRequest` 方法里对 Table 数据的所有修改，在方法返回 `Procedure.Success` 时整体提交；中途抛异常就整体回滚。代码可以完全按业务逻辑的自然顺序写，不用为「出错怎么办」操心。

> 为什么能这样？因为 Zeze 用乐观锁——执行时所有修改先记成日志，不动原始数据；成功了才把日志应用上去，失败就丢弃日志。详见 [事务参考](../reference/transaction.md) 和 [工作原理](../manual/02-how-zeze-works.md)。

## 跑起来

完整启动一个服务还涉及 `App`、`zeze.xml` 配置、网络服务等环节。最快的方式是直接用 [zezeboot 模板](https://gitee.com/dwing/zezeboot)，它已经把这些串好了。本篇聚焦「定义数据 → 写逻辑」这条主线，分布式部署的全貌见 [走向分布式](../manual/05-going-distributed.md)。

## 接下来

恭喜，你已经掌握了 Zeze 的核心工作流。建议接着读：

- 想理解背后原理 → [Zeze 如何工作](../manual/02-how-zeze-works.md)
- 想系统学怎么用好 → [Manual 指南](../manual/01-the-pain.md)
- 想查 XML 全部语法 → [solution.xml 参考](../reference/solution-xml.md)
