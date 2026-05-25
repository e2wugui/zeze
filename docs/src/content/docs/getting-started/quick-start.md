---
title: "快速上手"
sidebar:
  order: 5
---

Zeze 的核心能力围绕**事务**展开。只需三步就能启动一个完整的 Zeze 应用：在 XML 中定义数据结构，运行代码生成器，然后在生成的骨架中填写业务逻辑。本页用一个「角色经验+背包」的完整示例带你走完全程。

## 第一步：定义数据结构（solution.xml）

Zeze 通过 `solution.xml` 文件描述所有的数据结构、存储表和网络协议。框架会据此生成 Java 代码，开发者只需关注业务逻辑本身。XML 各标签的完整语法参见 [→ solution-xml](../core/solution-xml)。

下面是一份可直接使用的 `solution.xml`，定义了三个核心要素：角色数据结构（**Bean**）、持久化存储表（**Table**）和远程调用协议（**RPC**）。

```xml
<?xml version="1.0" encoding="utf-8"?>
<solution name="QuickStart" ModuleIdAllowRanges="1-100">
  <!-- 引入 Zeze 框架基础定义，路径根据你的工程结构调整 -->
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

要点说明：

- **`<solution>`**：根元素，`name` 属性既是解决方案名，也作为生成代码的根包名（如 `QuickStart.Role.BRole`）。
- **`<module>`**：逻辑模块，`id` 在整个解决方案中必须唯一。一个模块可以包含多个 Bean、Table 和 RPC。
- **`<bean>`**：数据结构定义（类似 C 的 struct）。[→ bean](../core/bean)
- **`<table>`**：持久化存储表，Zeze 自动管理内存与数据库之间的同步。
- **`<rpc>`**：远程过程调用，`handle="server"` 表示请求由服务端处理，代码生成器会自动生成对应的服务端处理函数骨架。
- **`<project>`**：定义项目输出目录和平台。`GenDir` 指定生成代码目录，`SrcDir` 指定业务代码目录。

## 第二步：代码生成

Zeze 的代码生成器是一个 .NET 工具（`Gen.exe`），需要先编译框架根目录下的 `Gen` 工程。

```bash
# 1. 编译 Gen 工程（需要 .NET 8 SDK）
cd zeze
dotnet build Gen

# 2. 在 solution.xml 所在目录执行代码生成
Gen/bin/Debug/net8.0/Gen.exe solution.xml
```

生成完成后，目录结构如下：

```
solution.xml 所在目录/
└── GameServer/                  # <project name="GameServer">
    ├── Gen/                     # 自动生成，每次重新覆盖
    │   └── QuickStart/
    │       └── Role/
    │           ├── BRole.java       # Bean 序列化代码
    │           ├── BBag.java        # Bean 序列化代码
    │           ├── BAddExperience.java
    │           ├── tRole.java       # Table 访问接口
    │           ├── tBag.java        # Table 访问接口
    │           └── AbstractModule.java  # 模块抽象基类
    └── src/                     # 业务代码目录，开发者在此编写
        └── QuickStart/
            └── Role/
                └── ModuleRole.java  # 模块实现（首次生成后不再覆盖）
```

**关键规则**：

- `Gen/` 目录下的代码在每次运行生成器时都会被完全覆盖，**不要手动修改**。
- `src/` 目录下生成的是业务骨架代码（如 `ModuleRole.java`），代码生成器只在文件不存在时创建。其中 `ZEZE_FILE_CHUNK` 标记之间的代码由生成器维护，标记之外的代码属于开发者。

生成的 `ModuleRole.java` 骨架如下：

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

注意 `ProcessAddExperienceRequest` 方法返回 `Procedure.NotImplement`，这是生成器给出的占位符，表示「尚未实现」。你需要用实际逻辑替换它。

## 第三步：编写业务逻辑

下面实现 `ProcessAddExperienceRequest`，演示 Zeze 事务的核心用法：

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

代码中的 `_tRole` 和 `_tBag` 是生成器在 `AbstractModule` 中自动创建的表访问对象（类型为 `Table`），直接通过 `.` 操作即可使用。

## 事务自动回滚

Zeze 采用**乐观锁**实现事务。所有在 `ProcessXxxRequest` 方法中对 Table 数据的修改，都在方法返回 `Procedure.Success` 时自动提交。

如果方法中途抛出异常（如上例的 `throw new RuntimeException("BagIsFull")`），整个事务会自动回滚，之前对 `_tRole` 和 `_tBag` 的所有修改都会被撤销，就像什么都没发生过一样。这意味着你不需要手写任何回滚逻辑。

更详细的事务机制（隔离级别、存储过程、嵌套事务等）参见 [→ transaction](../core/transaction)。

## 开发环境搭建

### 前置条件

1. **JDK 11+**（框架目标版本为 Java 11）
   - 推荐: [Adoptium Temurin](https://adoptium.net/)
2. **IntelliJ IDEA**（社区版即可）
   - 下载: [JetBrains IDEA](https://www.jetbrains.com/idea/download/)
3. **.NET 8 SDK**（仅用于运行代码生成器 `Gen.exe`）
   - 下载: [.NET 8 SDK](https://dotnet.microsoft.com/download/dotnet/8.0)
4. **Maven**（可选，IDEA 内置即可）

### 快速搭建步骤

```bash
# 1. 克隆仓库
git clone https://gitee.com/e2wugui/zeze
cd zeze

# 2. 编译代码生成器
dotnet build Gen

# 3. 用 IDEA 打开 ZezeJava 目录
#    IDEA 会自动识别 4 个 Maven 模块：
#    - ZezeJava      (框架核心)
#    - ZezeJavaTest  (单元测试)
#    - ZezexJava     (示例项目)

# 4. 生成示例项目代码
cd ZezeJava/ZezexJava
../../Gen/bin/Debug/net8.0/Gen.exe solution.xml
../../Gen/bin/Debug/net8.0/Gen.exe solution.linkd.xml

# 5. 回到 IDEA 执行 Build All
```

## 接下来

- 了解 XML 定义的完整语法：[→ solution-xml](../core/solution-xml)
- 深入理解 Bean 的序列化与数据类型：[→ bean](../core/bean)
- 掌握事务机制与隔离级别：[→ transaction](../core/transaction)
- 搭建分布式架构（Linkd + Provider）：[→ arch](../architecture/arch)
