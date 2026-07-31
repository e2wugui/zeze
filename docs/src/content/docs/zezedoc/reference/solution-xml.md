---
title: "solution.xml 参考"
description: "Zeze 数据建模文件 solution.xml 的完整 XML 语法与元素属性速查"
category: reference
order: 1
---

本文是 `solution.xml` 的完整语法参考——列出所有元素及其属性、类型系统、跨语言映射，供写代码时随查随用。概念讲解见 [定义数据](../manual/03-defining-data.md)，生成的 Bean 与 Table 细节见 [Bean 数据模型](./bean.md) 和 [Table 存储接口](./table.md)。

## 文档总览

`solution.xml` 描述了应用的数据模型（Bean）、存储（Table）、网络协议（Protocol/Rpc）和代码生成目标（Project/Service）。顶层元素结构如下：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<solution name="..." ModuleIdAllowRanges="...">
    <import file="..."/>            <!-- 引入其他 solution 文件 -->
    <module name="..." id="...">    <!-- 逻辑模块，可嵌套 -->
        <bean name="..."> ... </bean>
        <beankey name="..."> ... </beankey>
        <table name="..." key="..." value="..."/>
        <rpc name="..." .../>
        <protocol name="..." .../>
        <enum name="..." value="..."/>
    </module>
    <external bean="..."/>           <!-- 引用手写 Bean -->
    <externalkey beankey="..."/>
    <project name="..." ...>         <!-- 代码生成目标 -->
        <service name="..." ...> <module ref="..."/> </service>
        <ModuleStartOrder> ... </ModuleStartOrder>
    </project>
</solution>
```

---

## `<solution>` 根元素

| 属性 | 必填 | 说明 |
|------|------|------|
| `name` | 是 | 顶层命名空间。生成的 Java 类会落在 `Zeze.<name>.<module>` 下 |
| `ModuleIdAllowRanges` | 是 | 模块 id 的合法范围。支持区间（`1-1000`）、离散值（`100`）、逗号分隔混合（`1-1000,2000`）。多个 solution 文件合并后范围不可重叠 |

```xml
<solution name="Game" ModuleIdAllowRanges="1-1000">
```

## `<import>` 引入其他文件

| 属性 | 说明 |
|------|------|
| `file` | 要引入的 solution 文件路径 |

- 可以相互 import，框架负责解析循环依赖。
- 适合把大型数据模型拆分到多个文件，按模块/团队管理。

```xml
<import file="role.xml"/>
<import file="../common/types.xml"/>
```

---

## `<module>` 模块

模块是逻辑分组，`id` 全局唯一。可嵌套，内部可包含 bean/table/rpc/protocol/enum。

| 属性 | 必填 | 说明 |
|------|------|------|
| `name` | 是 | 模块名，用作生成代码的命名空间 |
| `id` | 是 | 全局唯一模块 id，必须在 `ModuleIdAllowRanges` 范围内 |
| `hot` | 否 | 热更新相关标记 |
| `DefaultTransactionLevel` | 否 | 该模块默认事务级别（覆盖程序默认） |
| `UseData` | 否 | 控制是否生成 Data 类 |

```xml
<module name="role" id="1">
    <module name="bag" id="2">   <!-- 嵌套模块 -->
        ...
    </module>
</module>
```

---

## `<bean>` 数据结构

Bean 是结构化数据的基本单元，自动参与事务。详见 [Bean 数据模型](./bean.md)。

| 属性 | 必填 | 说明 |
|------|------|------|
| `name` | 是 | Bean 名 |
| `version` | 否 | 版本标记字段名（类型 long，修改时自动递增） |
| `equals` | 否 | 为 `true` 时生成 `equals` / `hashCode` |
| `interface` | 否 | 指定生成的 Bean 实现的接口 |
| `UseData` | 否 | `"true"` 生成 Data 类；`"only"` 只生成 Data 类 |
| `MappingClass` | 否 | 生成关系映射类（用于关系型数据库映射） |
| `kind` | 否 | 特殊类型，如 `"rocks"` |
| `comment` | 否 | 注释，生成到代码注释里 |

```xml
<bean name="Player" version="ver" equals="true">
    <variable id="1" name="name"  type="string"/>
    <variable id="2" name="level" type="int"/>
</bean>
```

### `<beankey>` 复合键

语法与 `<bean>` 相同，但用作 Table 的**复合主键**。可以为空（空 key 作占位）：

```xml
<beankey name="RoleIdServerKey">
    <variable id="1" name="roleId"   type="long"/>
    <variable id="2" name="serverId" type="int"/>
</beankey>

<!-- 空 beankey -->
<beankey name="EmptyKey"/>
```

> 用作 Table 的 key 时，beankey 生成的类必须实现 `Comparable`。

### `<variable>` 字段

| 属性 | 必填 | 说明 |
|------|------|------|
| `id` | 是 | Bean 内唯一正整数（≤ 4095）。用于序列化和版本兼容，**删除后不可复用** |
| `name` | 是 | 字段名 |
| `type` | 是 | 字段类型，见下方类型表 |
| `key` | 否 | map/set 时声明键类型 |
| `value` | 否 | map/list/set/dynamic 时声明值类型或 bean 名 |
| `default` | 否 | 默认值 |
| `AllowNegative` | 否 | 允许负数 |
| `transient` | 否 | `true` 时不持久化 |
| `javaType` | 否 | 特化集合实现类型 |

```xml
<variable id="1" name="level" type="int" default="1"/>
<variable id="2" name="friends" type="set" value="long"/>
<variable id="3" name="scores" type="map" key="int" value="float"/>
<!-- 方括号简写 -->
<variable id="4" name="names" type="list[string]"/>
```

---

## variable `type` 类型表

### 基本类型与向量

| 类型 | Java 对应 | 说明 |
|------|-----------|------|
| `bool` | `boolean` | 布尔 |
| `byte` | `byte` | 8 位有符号整数 |
| `short` | `short` | 16 位有符号整数 |
| `int` | `int` | 32 位有符号整数 |
| `long` | `long` | 64 位有符号整数 |
| `float` | `float` | 32 位浮点 |
| `double` | `double` | 64 位浮点 |
| `string` | `String` | UTF-8 字符串 |
| `binary` | `Zeze.Net.Binary` | 字节序列（Java/C# 均映射为 `Zeze.Net.Binary`） |
| `decimal` | `BigDecimal` | 高精度十进制 |
| `vector2` | `Vector2` | 两 float |
| `vector2int` | `Vector2Int` | 两 int |
| `vector3` | `Vector3` | 三 float |
| `vector3int` | `Vector3Int` | 三 int |
| `vector4` | `Vector4` | 四 float |
| `quaternion` | `Quaternion` | 四元数 |

### 集合类型

| 类型 | 说明 | 方括号简写 |
|------|------|-----------|
| `list` | 有序列表，元素可 Bean | `list[long]`、`list[Item]` |
| `set` | 无序集合 | `set[long]` |
| `map` | 键值映射 | `map[int,float]`、`map[int,Item]` |
| `array` | 数组 | |
| `gtable` | 表 | |

集合的元素本身可以是 Bean，支持任意嵌套。

### 引用类型

| 类型 | 说明 |
|------|------|
| `Bean名` | 嵌套引用其他 Bean |
| `dynamic` | 动态 Bean，运行时多态，见下节 |
| `beankey名` | 引用复合键 |

### `javaType` 特化集合

省去装箱开销，生成更高效的具体实现：

| javaType | 用途 |
|----------|------|
| `IntList` / `LongList` / `FloatList` | 基本类型 List（**没有 `DoubleList`**） |
| `Vector2List` / `Vector3List` / `Vector4List` | float 向量 List |
| `Vector2IntList` / `Vector3IntList` | int 向量 List |
| `IntHashSet` / `LongHashSet` | 基本 Set |
| `IntHashMap<V>` / `LongHashMap<V>` | 基本类型 Map |

```xml
<variable id="1" name="ids" type="list" value="int" javaType="IntList"/>
```

---

## `type="dynamic"` 动态 Bean

用一个 variable 持有多种 Bean 类型，运行时通过 `typeId()` 区分。用 `<value>` 列出所有可能类型：

```xml
<bean name="Pet"/>
<bean name="Mount"/>

<bean name="Role">
    <variable id="1" name="partner" type="dynamic">
        <value bean="Pet"/>
        <value bean="Mount"/>
    </variable>
</bean>
```

| 属性 / 用法 | 说明 |
|-------------|------|
| `<value bean="..."/>` | 列出可能持有的 Bean |
| 完全限定名 | 可跨模块引用，如 `<value bean="Game.Item.BHorseExtra"/>` |
| 显式 typeId | `<value bean="demo.Bean1:1"/>` 指定 typeId 为 1 |
| 简写 | `type="dynamic:BSimple"` 等价于只列一个 BSimple |
| 自定义工厂 | 实现 `GetSpecialTypeIdFromBean` / `CreateBeanFromSpecialTypeId` / `CreateDataFromSpecialTypeId` |

> dynamic 不支持嵌套（dynamic 字段里再套 dynamic 不允许）。未设置时用 `EmptyBean`（typeId=0）表示。详见 [Bean 数据模型](./bean.md)。

---

## 跨语言类型映射表

同一份 XML 生成的多语言客户端，类型对应关系如下：

### 基本类型映射

| XML 类型 | Java | C# | Lua | TypeScript |
|----------|------|----|-----|------------|
| `bool` | `boolean` | `bool` | `boolean` | `boolean` |
| `byte` | `byte` | `sbyte` | `number` | `number` |
| `short` | `short` | `short` | `number` | `number` |
| `int` | `int` | `int` | `number` | `number` |
| `long` | `long` | `long` | `number` | `number` / `bigint` |
| `float` | `float` | `float` | `number` | `number` |
| `double` | `double` | `double` | `number` | `number` |
| `string` | `String` | `string` | `string` | `string` |
| `binary` | `Zeze.Net.Binary` | `Zeze.Net.Binary` | `string` | `Uint8Array` |

### 集合映射

| XML 类型 | Java | C# | Lua | TypeScript |
|----------|------|----|-----|------------|
| `map` | `HashMap` | `Dictionary` | `table` | `Map`（`CollMap2`/`PMap2`） |
| `list` | `ArrayList` | `List` | `table` | `Array`（`CollList2`/`PList2`） |
| `set` | `HashSet` | `HashSet` | `table` | `Set`（`CollSet2`/`PSet2`） |
| `dynamic` | `DynamicBean` | `DynamicBean` | `table` | `DynamicBean` |

---

## `<enum>` 常量 / 错误码

可定义在 bean / rpc / module 内，生成代码里得到对应常量。常用于声明错误码。

| 属性 | 必填 | 说明 |
|------|------|------|
| `name` | 是 | 常量名 |
| `value` | 是 | 值 |
| `comment` | 否 | 注释 |

```xml
<module name="role" id="1">
    <enum name="ERR_COIN_NOT_ENOUGH" value="1" comment="金币不足"/>
    <enum name="ERR_NOT_FOUND"       value="2" comment="玩家不存在"/>
</module>
```

模块级错误码编码为 `(moduleId << 32) | errorCode`，详见 [事务系统](./transaction.md)。

---

## `<table>` 存储

声明后生成 `TableXxx<K, V>` 子类，语义等价 `Map<K,V>`。详见 [Table 存储接口](./table.md)。

| 属性 | 必填 | 说明 |
|------|------|------|
| `name` | 是 | 表名，生成类名前缀 |
| `key` | 是 | 基本类型或 beankey（必须 `Comparable`） |
| `value` | 是 | 必须是 bean |
| `memory` | 否 | `true` 纯内存表，不持久化 |
| `autokey` | 否 | `"true"` 自动键；`"random"` 随机键 |
| `RelationalMapping` | 否 | 关系映射 |
| `suffix` | 否 | 表名后缀模板，如 `_@AppMainVersion`、`_@ServerId` |
| `gen` | 否 | 指定生成的 project |
| `kind` | 否 | 特殊类型 |
| `noSchema` | 否 | `true` 时不使用 Schema 校验 |
| `comment` | 否 | 注释 |

```xml
<table name="tPlayer" key="long" value="Player"/>
<table name="tMail"   key="long" value="Mail" suffix="_@ServerId"/>
<table name="tCounter" key="long" value="Counter" memory="true"/>
```

---

## `<rpc>` 远程过程调用

| 属性 | 必填 | 说明 |
|------|------|------|
| `name` | 是 | Rpc 名 |
| `argument` | 是 | 参数 Bean |
| `result` | 是 | 结果 Bean。若 result 含 `resultCode`（long）字段会被特殊处理，**0 为正常** |
| `handle` | 否 | 处理方，见下方 handle 表 |
| `base` | 否 | 基类 |
| `TransactionLevel` | 否 | 事务级别 |
| `NoProcedure` | 否 | 不在存储过程中执行 |
| `CriticalLevel` | 否 | 重要级别 |
| `UseData` | 否 | 控制是否生成 Data 类 |
| `comment` | 否 | 注释 |

```xml
<rpc name="Login" argument="LoginArg" result="LoginRes" handle="server"/>
```

## `<protocol>` 单向协议

| 属性 | 必填 | 说明 |
|------|------|------|
| `name` | 是 | 协议名。建议加 `C`（客户端发服务端）/ `S`（服务端发客户端）前缀 |
| `argument` | 是 | 参数 Bean |
| `handle` | 否 | 处理方 |
| `TransactionLevel` | 否 | 事务级别 |
| `NoProcedure` | 否 | 不在存储过程中执行 |
| `CriticalLevel` | 否 | 重要级别 |
| `UseData` | 否 | 控制 Data 类生成 |
| `comment` | 否 | 注释 |

```xml
<protocol name="CHeartbeat" argument="EmptyBean" handle="server"/>
<protocol name="SCoinChanged" argument="CoinChanged" handle="client"/>
```

### `handle` 标签

决定由谁处理该协议，可逗号组合：

| 值 | 说明 |
|----|------|
| `server` | 服务端处理 |
| `client` | 客户端处理 |
| `serverscript` | 服务端脚本处理 |
| `clientscript` | 客户端脚本处理 |
| `servlet` | HTTP 服务端点处理（用于 `<servlet>`） |

```xml
<rpc name="Echo" argument="EchoArg" result="EchoRes" handle="server,client"/>
```

---

## `<project>` 代码生成目标

一个 `<project>` 对应一个进程（生成目标），定义代码生成参数。

| 属性 | 必填 | 说明 |
|------|------|------|
| `name` | 是 | project 名 |
| `GenDir` | 是 | 生成代码输出目录 |
| `SrcDir` | 是 | 手写源码目录 |
| `platform` | 是 | 目标平台（见下方取值表） |
| `hot` | 否 | 热更新相关 |
| `MappingClass` | 否 | 生成关系映射类 |
| `ClientScript` | 否 | 客户端脚本配置 |
| `GenTables` | 否 | 指定生成哪些表 |

```xml
<project name="GameServer" GenDir="gen" SrcDir="src" platform="java">
    ...
</project>
```

`platform` 取值（生成器实际支持的活跃值）：

| platform | 说明 |
|----------|------|
| `java` | 服务端 Java 代码 |
| `conf+cs` | C# 客户端配置 + 代码（含 Table 等） |
| `conf+cs+net` | C# 客户端配置 + 代码 + 网络层（联网需要） |
| `cxx` | C++ 客户端 |
| `ts` | TypeScript 客户端 |
| `cxx+ts` | 同时生成 C++ 与 TypeScript |
| `luaclient` | Lua 客户端脚本（宿主嵌入 C++/C#） |
| `python` | Python 客户端 |

> ⚠️ **注意**：单独的 `cs`、`cs+luaclient`、`cs+ts` 这三个取值**已被废弃**（源码中已注释掉）。需要 C# 客户端请用 `conf+cs` 或 `conf+cs+net`。

### `<service>` 服务

project 内定义网络服务，用 `<module ref>` 引用模块。

| 属性 | 必填 | 说明 |
|------|------|------|
| `name` | 是 | 服务名 |
| `handle` | 否 | 处理方 |
| `base` | 否 | 基服务类（如 `main`） |

```xml
<service name="GameServer" handle="server" base="main">
    <module ref="role"/>
    <module ref="role.bag"/>
</service>
```

### `<ModuleStartOrder>` 模块启动顺序

控制 project 内模块的启动顺序：

```xml
<project name="GameServer" ...>
    <ModuleStartOrder>
        <module ref="role"/>
        <module ref="bag"/>
    </ModuleStartOrder>
</project>
```

---

## `<external>` / `<externalkey>` 手写 Bean

引用由 Java 代码直接编写（非生成）的 Bean 或 beankey，用完全限定名：

```xml
<external bean="Game.Item.BHandwrittenExtra"/>
<externalkey beankey="Game.Role.HandKey"/>
```

---

## `<servlet>` HTTP 服务端点

声明 HTTP 服务端点，`name` 同时作为 URL 路径。

| 属性 | 必填 | 说明 |
|------|------|------|
| `name` | 是 | 名称，同时作 URL 路径 |
| `TransactionLevel` | 否 | 事务级别 |

```xml
<servlet name="/api/health" TransactionLevel="None"/>
```

---

## 跨模块引用

在一个模块里引用另一个模块（甚至另一个 solution 文件）的 Bean / beankey / Table 时，使用**完全限定名**：

| 格式 | 说明 |
|------|------|
| `解决方案名.模块名.Bean名` | 跨 solution + 模块引用 |
| `解决方案名.Bean名` | Bean 在 solution 直接子节点时 |

```xml
<!-- 在 role 模块引用 Game.Item 模块的 Bean -->
<variable id="1" name="horse" type="dynamic">
    <value bean="Game.Item.BHorseExtra"/>
</variable>
```

---

## 完整示例骨架

下面是一个覆盖主要元素的完整示例：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<solution name="Game" ModuleIdAllowRanges="1-1000">

    <!-- 引入公共类型 -->
    <import file="common/types.xml"/>

    <!-- 角色模块 -->
    <module name="role" id="1" DefaultTransactionLevel="Serializable">

        <!-- 玩家数据（Table 的 value） -->
        <bean name="Player" version="ver" equals="true">
            <variable id="1" name="name"   type="string" default=""/>
            <variable id="2" name="level"  type="int"    default="1"/>
            <variable id="3" name="coins"  type="long"/>
            <variable id="4" name="items"  type="map[int,Item]"/>
            <variable id="5" name="ids"    type="list" value="int" javaType="IntList"/>
            <variable id="6" name="tag"    type="set[long]"/>
        </bean>

        <!-- 道具：嵌套 Bean -->
        <bean name="Item">
            <variable id="1" name="configId" type="int"/>
            <variable id="2" name="count"    type="int"/>
        </bean>

        <!-- 玩家表：long 主键 -> Player -->
        <table name="tPlayer" key="long" value="Player"/>

        <!-- 按服隔离的邮件表 -->
        <table name="tMail" key="long" value="Mail" suffix="_@ServerId"/>

        <!-- 纯内存计数表 -->
        <table name="tCounter" key="long" value="Counter" memory="true"/>

        <!-- 复合主键示例 -->
        <beankey name="RoleServerKey">
            <variable id="1" name="roleId"   type="long"/>
            <variable id="2" name="serverId" type="int"/>
        </beankey>
        <table name="tRoleServer" key="RoleServerKey" value="RoleServer"/>

        <!-- 登录 Rpc -->
        <bean name="LoginArg">
            <variable id="1" name="account" type="string"/>
        </bean>
        <bean name="LoginRes">
            <variable id="1" name="resultCode" type="long"/>   <!-- 0 为正常 -->
            <variable id="2" name="roleId"     type="long"/>
        </bean>
        <rpc name="Login" argument="LoginArg" result="LoginRes" handle="server"/>

        <!-- 单向协议 -->
        <protocol name="CHeartbeat" argument="EmptyBean" handle="server"/>
        <protocol name="SCoinChanged" argument="CoinChanged" handle="client"/>

        <!-- 错误码 -->
        <enum name="ERR_COIN_NOT_ENOUGH" value="1" comment="金币不足"/>
        <enum name="ERR_NOT_FOUND"       value="2" comment="玩家不存在"/>

        <!-- 嵌套模块：背包 -->
        <module name="bag" id="2">
            <bean name="Bag">
                <variable id="1" name="slots" type="map[int,Item]"/>
            </bean>
            <!-- dynamic 演示 -->
            <bean name="BHorseExtra"/>
            <bean name="BWingExtra"/>
            <bean name="BagExtra">
                <variable id="1" name="ext" type="dynamic">
                    <value bean="BHorseExtra"/>
                    <value bean="BWingExtra:2"/>
                </variable>
            </bean>
        </module>
    </module>

    <!-- 引用手写 Bean -->
    <external bean="Game.Common.BHandwritten"/>

    <!-- 代码生成目标：游戏服 -->
    <project name="GameServer" GenDir="gen/GameServer" SrcDir="src/GameServer" platform="java">
        <ModuleStartOrder>
            <module ref="role"/>
            <module ref="role.bag"/>
        </ModuleStartOrder>
        <service name="GameServer" handle="server" base="main">
            <module ref="role"/>
            <module ref="role.bag"/>
        </service>
    </project>

    <!-- HTTP 端点 -->
    <servlet name="/api/health" TransactionLevel="None"/>

</solution>
```

运行代码生成器后，你会得到 `Player`、`Item`、`Bag` 等 Bean 类、`TableTPlayer`、`TableTMail` 等表类，以及 `Login` Rpc、`CHeartbeat`/`SCoinChanged` 协议的骨架代码。

## 相关文档

- [Bean 数据模型](./bean.md) — variable id、托管状态、版本兼容
- [Table 存储接口](./table.md) — CRUD、缓存、遍历
- [序列化协议](./serialize.md) — 二进制编码细节
- [事务系统](./transaction.md) — Procedure 与返回值编码
- [定义数据](../manual/03-defining-data.md) — 概念讲解
