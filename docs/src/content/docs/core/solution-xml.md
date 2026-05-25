---
title: "Solution.xml 参考"
sidebar:
  order: 3
---

`solution.xml` 是 Zeze 框架的核心建模文件，用于定义数据结构（**Bean**）、存储表（**Table**）、网络协议（**Protocol**）、远程调用（**Rpc**）以及代码生成目标（**Project**）。框架的代码生成器读取此文件后，自动产出 Java / C++ / TypeScript 等语言的类型定义、序列化和协议处理框架代码。

## solution 根元素

```xml
<solution name="Game" ModuleIdAllowRanges="1-1000">
  ...
</solution>
```

| 属性 | 必填 | 说明 |
|------|------|------|
| **name** | 是 | 解决方案名称，作为生成代码的顶层命名空间。例如 `name="Game"` 将在 Java 中生成到 `Game.*` 包下 |
| **ModuleIdAllowRanges** | 是 | 模块 ID 的合法范围。支持区间（`1-1000`）和离散值（`100`），支持逗号分隔多段（`1-5,100`）。当系统包含多个 solution 时，各范围不可重叠 |

## import 引入其他文件

通过 `import` 将另一个 solution 文件引入当前文件，引入后可直接使用被引入文件中定义的 Bean。两个 solution 文件可以相互 import。

```xml
<import file="solution.linkd.xml"/>
<import file="../ZezeJava/solution.zeze.xml"/>
```

| 属性 | 必填 | 说明 |
|------|------|------|
| **file** | 是 | 相对当前文件的路径 |

## module 模块

**模块（Module）** 是 Zeze 组织 Bean、Table、Protocol、Rpc 的逻辑单元。模块在生成代码时提供命名空间。模块可以嵌套定义子模块。

```xml
<module name="Login" id="1" hot="true">
  <bean name="BRole">...</bean>
  <table name="trole" key="long" value="BRole"/>
  <rpc name="CreateRole" argument="BCreateRole" result="BRole" handle="server"/>
</module>
```

| 属性 | 必填 | 说明 |
|------|------|------|
| **name** | 是 | 模块名称，作为生成代码中的类名或命名空间的一部分 |
| **id** | 是 | 模块 ID，在整个系统中必须唯一，且必须在 `ModuleIdAllowRanges` 范围内 |
| **hot** | 否 | 设为 `"true"` 表示该模块支持热更新 |
| **DefaultTransactionLevel** | 否 | 该模块内协议/方法的默认事务级别，可选值：`None`、`Serializable` 等 |
| **UseData** | 否 | 设为 `"true"` 表示使用 Data 层（区分只读和读写数据视图） |

模块内可定义 `bean`、`beankey`、`table`、`protocol`、`rpc`、`enum` 等子元素。

## bean 数据结构

**Bean** 是 Zeze 的核心数据结构定义，对应生成代码中的类。Bean 内通过 `variable` 子元素声明字段。

```xml
<bean name="BRole">
  <variable id="1" name="Id" type="long"/>
  <variable id="2" name="Name" type="string"/>
</bean>
```

### bean 属性

| 属性 | 必填 | 说明 |
|------|------|------|
| **name** | 是 | Bean 名称。建议以 `B` 前缀命名，方便在编辑器中通过前缀提示查找 |
| **version** | 否 | 版本标记字段名，框架将自动维护该字段的版本号 |
| **equals** | 否 | 设为 `"true"` 后生成代码会自动生成 `equals()` 和 `hashCode()` 方法 |
| **interface** | 否 | 指定 Bean 实现的 Java 接口 |
| **UseData** | 否 | `"true"` 启用 Data 层；`"only"` 仅生成 Data 类（不生成可变 Bean） |
| **MappingClass** | 否 | 设为 `"true"` 后生成关系映射类 |
| **kind** | 否 | 特殊存储类型，如 `"rocks"` 表示使用 RocksDB 直接存储 |
| **comment** | 否 | Bean 的注释说明 |

### beankey

**beankey** 与 `bean` 类似，但语义上用于作为表或映射的复合键。它生成的代码支持作为 `key` 使用。

```xml
<beankey name="BFighterId">
  <variable id="1" name="Type" type="int"/>
  <variable id="2" name="InstanceId" type="long"/>
</beankey>
```

`beankey` 的属性与 `bean` 基本一致。`beankey` 可以为空（如 `<beankey name="EmptyKey"/>`），此时用作占位键类型。

### variable 字段定义

`variable` 是 `bean` / `beankey` 的子元素，定义一个字段。

```xml
<variable id="1" name="Name" type="string"/>
<variable id="2" name="Extra" type="dynamic">
  <value bean="BSkillAttackExtra"/>
</variable>
<variable id="3" name="Items" type="map" key="int" value="BItem"/>
<variable id="4" name="Tags" type="set[long]"/>
```

| 属性 | 必填 | 说明 |
|------|------|------|
| **id** | 是 | 字段编号，在 Bean 内唯一。删除字段后 id 不可复用（除非新字段类型与被删字段兼容） |
| **name** | 是 | 字段名称 |
| **type** | 是 | 字段类型（见下方类型一览） |
| **key** | 条件必填 | 当 type 为 `map` 或 `set` 时需要，指定键类型 |
| **value** | 条件必填 | 当 type 为 `map`、`list`、`set`、`dynamic` 时需要，指定值类型 |
| **default** | 否 | 字段默认值 |
| **AllowNegative** | 否 | 设为 `"true"` 允许该字段存储负数（默认整型不允许负数） |
| **transient** | 否 | 设为 `"true"` 表示该字段不参与持久化存储 |
| **javaType** | 否 | Java 平台特化的集合类型名，如 `IntList`、`LongHashMap` 等（详见下方 javaType 一节） |

### dynamic 动态 Bean

`type="dynamic"` 声明一个**动态 Bean** 字段，运行时可持有不同的 Bean 实例。需要通过 `<value>` 子元素声明所有可能的 Bean 类型，并可以指定 typeId 编号。

```xml
<variable id="3" name="Extra" type="dynamic">
  <value bean="BSkillAttackExtra"/>
  <value bean="Game.Item.BHorseExtra"/>
  <value bean="Game.Equip.BEquipExtra"/>
</variable>
```

`<value bean="..."/>` 中可以使用完全限定名（`模块名.Bean名`）跨模块引用。也可以显式指定 typeId：

```xml
<value bean="demo.Bean1:1"/>
<value bean="BSimple:2"/>
```

dynamic 还支持简写语法和自定义工厂方法：

```xml
<!-- 简写：用方括号直接指定 key/value -->
<variable id="25" name="map25" type="map[Key,BSimple]"/>
<variable id="27" name="dynamic27" type="dynamic:BSimple"/>

<!-- 自定义 typeId 工厂 -->
<variable id="23" name="dynamic23" type="dynamic">
  <GetSpecialTypeIdFromBean value="demo.Module1.ModuleModule1::getSpecialTypeIdFromBean"/>
  <CreateBeanFromSpecialTypeId value="demo.Module1.ModuleModule1::createBeanFromSpecialTypeId"/>
  <CreateDataFromSpecialTypeId value="demo.Module1.ModuleModule1::createDataFromSpecialTypeId"/>
</variable>
```

## 支持的类型一览

variable 的 `type` 属性支持以下类型：

### 基本类型

| type | Java 类型 | C# 类型 | Lua 类型 | TypeScript 类型 |
|------|-----------|---------|----------|----------------|
| `bool` | `boolean` | `bool` | `boolean` | `boolean` |
| `byte` | `byte` | `byte` | `number(int64)` | `number` |
| `short` | `short` | `short` | `number(int64)` | `number` |
| `int` | `int` | `int` | `number(int64)` | `number` |
| `long` | `long` | `long` | `number(int64)` | `bigint` |
| `float` | `float` | `float` | `number(double)` | `number` |
| `double` | `double` | `double` | `number(double)` | `number` |
| `string` | `String` | `string` | `string` | `string` |
| `binary` | `Zeze.Net.Binary` | `Zeze.Net.Binary` | `string` | `Uint8Array` |

### 集合类型（跨语言映射）

| type | Java | C# | Lua | TypeScript |
|------|------|----|-----|------------|
| `map` | `CollMap2<Bean>`, `CollMap1<Integer>` | `PMap2<Bean>`, `PMap1<int>` | `table` | `Map` |
| `list` | `CollList2<Bean>`, `CollList1<Integer>` | `PList2<Bean>`, `PList1<int>` | `table` | `Array` |
| `set` | `CollSet1<Integer>` | `PSet1<int>` | `table` | `Set` |
| `dynamic` | `DynamicBean` | `DynamicBean` | `table` | `DynamicBean` |

### 数学向量类型

| type | Java 类型 | 说明 |
|------|-----------|------|
| `vector2` | `Zeze.Serialize.Vector2` | 二维浮点向量 (x, y) |
| `vector2int` | `Zeze.Serialize.Vector2Int` | 二维整数向量 (x, y) |
| `vector3` | `Zeze.Serialize.Vector3` | 三维浮点向量 (x, y, z) |
| `vector3int` | `Zeze.Serialize.Vector3Int` | 三维整数向量 (x, y, z) |
| `vector4` | `Zeze.Serialize.Vector4` | 四维浮点向量 (x, y, z, w) |
| `quaternion` | `Zeze.Serialize.Quaternion` | 四元数 (x, y, z, w) |

### 集合类型

| type | Java 类型 | 说明 |
|------|-----------|------|
| `list` | `CollList1` / `CollList2` | 有序列表。使用 `value` 属性指定元素类型 |
| `map` | `CollMap1` / `CollMap2` | 键值映射。使用 `key` 和 `value` 属性 |
| `set` | `CollSet1` | 无序集合。使用 `value` 属性指定元素类型 |
| `array` | 原生数组 | 固定类型数组。如 `type="array[float]"` |
| `gtable` | `GTable` | 通用表格（稀疏二维表）。如 `type="gtable[int, int, Bean1]"` |

集合类型支持方括号简写语法：

```xml
<variable id="1" name="Roles" type="set[long]"/>
<variable id="2" name="Attrs" type="map[int,float]"/>
<variable id="3" name="Names" type="list[string]"/>
```

### 引用类型

| type | 说明 |
|------|------|
| Bean 名称 | 引用另一个 Bean，如 `type="BRole"` |
| `dynamic` | 动态 Bean，运行时多态。参见 [dynamic 动态 Bean](#dynamic-动态-bean) |
| beankey 名称 | 引用一个 beankey 作为字段类型，如 `type="BFighterId"` |

### JSON 类型

通过 `value` 属性标记 JSON 语义：

```xml
<variable id="44" name="jsonObject" type="string" value="JSON_OBJECT"/>
<variable id="45" name="jsonArray" type="string" value="JSON_ARRAY"/>
```

### javaType 集合特化

对于 Java 平台，可通过 `javaType` 属性指定更高效的集合实现：

```xml
<variable id="30" name="list30" type="list[int]"     javaType="IntList"/>
<variable id="38" name="set38"  type="set[int]"       javaType="IntHashSet"/>
<variable id="40" name="map40"  type="map[int,int]"   javaType="IntHashMap"/>
<variable id="41" name="map41"  type="map[long,BSimple]" javaType="LongHashMap"/>
```

常用 javaType 值包括：`IntList`、`LongList`、`FloatList`、`Vector2List`、`Vector3List`、`Vector4List`、`Vector2IntList`、`Vector3IntList`、`IntHashSet`、`LongHashSet`、`IntHashMap`、`LongHashMap`。

## enum 枚举

`enum` 可定义在 `bean`、`rpc`、`module` 内部，用于定义常量或错误码。生成代码会产生对应的常量字段。

```xml
<!-- 在 bean 内定义 -->
<bean name="BValue">
  <enum name="Enum1" value="4" comment="枚举的注释"/>
  <variable id="1" name="State" type="int"/>
</bean>

<!-- 在 rpc 内定义错误码 -->
<rpc name="Auth" argument="BAuth" handle="server">
  <enum name="Success" value="0"/>
  <enum name="Error" value="1"/>
</rpc>

<!-- 在 module 内定义独立错误码 -->
<module name="Equip" id="7">
  <enum name="ResultCodeCannotEquip" value="1"/>
  <enum name="ResultCodeItemNotFound" value="2"/>
  <enum name="ResultCodeBagIsFull" value="3"/>
</module>
```

| 属性 | 必填 | 说明 |
|------|------|------|
| **name** | 是 | 枚举常量名称 |
| **value** | 是 | 枚举值（整数） |
| **comment** | 否 | 注释说明 |

## table 存储表

**Table** 定义 Key-Value 持久化存储表，是 Zeze 事务操作的核心载体。详见 → [transaction](./transaction)。

```xml
<table name="trole" key="long" value="BRole"/>
<table name="taccount" key="string" value="BAccount"/>
<table name="tbufs" key="long" value="BBufs"/>
```

| 属性 | 必填 | 说明 |
|------|------|------|
| **name** | 是 | 表名。建议以 `t` 前缀命名 |
| **key** | 是 | 键类型。可以是基本类型（`long`、`int`、`string`、`binary`）或 beankey 名称 |
| **value** | 是 | 值类型。必须是一个 Bean 名称 |
| **memory** | 否 | 设为 `"true"` 表示纯内存表，不持久化到数据库。默认 `"false"` |
| **autokey** | 否 | 设为 `"true"` 启用自动键生成；设为 `"random"` 启用随机键生成 |
| **RelationalMapping** | 否 | 设为 `"true"` 启用关系映射，生成额外的映射类，支持按 Bean 字段建索引 |
| **suffix** | 否 | 表名后缀模板，支持运行时变量替换。如 `"@AppMainVersion"` 或 `"@ServerId"` |
| **gen** | 否 | 指定在哪个 project 中生成此表的代码（配合 project 的 `GenTables` 使用） |
| **kind** | 否 | 特殊存储类型，如 `"rocks"` |
| **noSchema** | 否 | 设为 `"true"` 表示不使用 Schema 校验 |
| **comment** | 否 | 注释说明 |

### suffix 示例

```xml
<!-- 表名在运行时展开为 tKuafu__{AppMainVersion} -->
<table name="tKuafu" suffix="@AppMainVersion" key="long" value="BKuafu"/>
<!-- 表名在运行时展开为 tTestSchemas__{ServerId} -->
<table name="tTestSchemas" suffix="@ServerId" key="long" value="BTestSchemas"/>
```

## rpc 远程调用

**Rpc** 定义请求-响应式的远程过程调用。详见 → [arch](/architecture/arch)。

```xml
<rpc name="CreateRole" argument="BCreateRole" result="BRole" handle="server"/>
<rpc name="AreYouFight" handle="client"/>
<rpc name="Acquire" argument="BAcquireParam" result="BReduceParam"
     base="Zeze.Raft.RaftRpc" handle="server"/>
```

| 属性 | 必填 | 说明 |
|------|------|------|
| **name** | 是 | Rpc 名称 |
| **argument** | 否 | 请求参数 Bean。可省略（无参数） |
| **result** | 否 | 响应结果 Bean。可省略（无返回值） |
| **handle** | 是 | 处理方。可选值见下方 handle 说明 |
| **base** | 否 | 指定基类，如 `"Zeze.Raft.RaftRpc"` |
| **TransactionLevel** | 否 | 覆盖模块默认的事务级别。如 `"None"`、`"Serializable"` |
| **NoProcedure** | 否 | 设为 `"true"` 表示此 Rpc 不自动包装事务，由实现代码自行控制 |
| **CriticalLevel** | 否 | 关键级别，用于过载保护。数值越大越关键（0-3） |
| **UseData** | 否 | 设为 `"true"` 使用 Data 层 |
| **comment** | 否 | 注释说明 |

Rpc 的 result Bean 中，如果包含名为 `resultCode` 且类型为 `long` 的字段，该字段会被框架特殊处理，用于表示远程执行结果（0 为正常）。

## protocol 协议

**Protocol** 定义单向的网络协议消息。

```xml
<protocol name="SChanged" argument="BBufChanged" handle="client"/>
<protocol name="CEnterWorld" handle="server"/>
<protocol name="CEnterWorldDone" argument="BEnterWorldDone" handle="server"/>
```

| 属性 | 必填 | 说明 |
|------|------|------|
| **name** | 是 | 协议名称。建议以 `C`（Client 发往 Server）或 `S`（Server 发往 Client）前缀命名 |
| **argument** | 否 | 协议参数 Bean。可省略（无参数体） |
| **handle** | 是 | 处理方。见下方 handle 说明 |
| **TransactionLevel** | 否 | 覆盖模块默认的事务级别 |
| **NoProcedure** | 否 | 设为 `"true"` 不自动包装事务 |
| **CriticalLevel** | 否 | 关键级别，用于过载保护 |
| **UseData** | 否 | 设为 `"true"` 使用 Data 层 |
| **comment** | 否 | 注释说明 |

### handle 处理方

`handle` 属性标识协议/Rpc 的处理方，支持以下标签，可逗号分隔组合使用：

| 标签 | 说明 |
|------|------|
| `server` | 服务端处理 |
| `client` | 客户端处理 |
| `serverscript` | 服务端脚本处理 |
| `clientscript` | 客户端脚本处理 |

```xml
<protocol name="Protocol4" argument="BValue" handle="server,clientscript" CriticalLevel="0"/>
```

## project 项目

**Project** 定义一个代码生成目标，通常对应一个进程。一个 solution 可以有多个 project。

```xml
<project name="server" hot="true" GenDir="server/Gen" SrcDir="server/src"
         platform="java" GenTables="">
  <service name="Server" handle="server" base="Zeze.Arch.ProviderService">
    <module ref="Login"/>
    <module ref="Equip"/>
  </service>
</project>
```

| 属性 | 必填 | 说明 |
|------|------|------|
| **name** | 是 | 项目名称 |
| **GenDir** | 否 | 生成代码的输出目录。默认为当前目录 |
| **SrcDir** | 否 | 源代码目录 |
| **platform** | 否 | 目标平台。可选值：`java`、`cs`、`cxx`、`ts` |
| **GenTables** | 否 | 指定生成哪些 Table。为空时生成未配置 `gen` 属性的表。逗号分隔多个表名 |
| **hot** | 否 | 设为 `"true"` 支持热更新 |
| **MappingClass** | 否 | 设为 `"true"` 生成关系映射类 |
| **ClientScript** | 否 | 设为 `"true"` 表示客户端脚本项目 |

典型项目划分：一个 `server` 项目负责服务端逻辑，一个 `client` 或 `cxx` / `ts` 项目负责客户端。

## service 服务

**Service** 定义网络服务，负责管理连接、注册协议、派发协议处理。详见 → [arch](/architecture/arch)。

```xml
<service name="Server" handle="server" base="Zeze.Arch.ProviderService">
  <module ref="Login"/>
  <module ref="Equip"/>
</service>

<service name="ServerDirect" handle="server,client"
         base="Zeze.Arch.ProviderDirectService">
</service>
```

| 属性 | 必填 | 说明 |
|------|------|------|
| **name** | 是 | 服务名称 |
| **handle** | 是 | 处理方标签，与 protocol/rpc 的 handle 对应。只有匹配的协议才会被注册到此服务 |
| **base** | 否 | 生成代码的基类全限定名 |

`service` 内通过 `<module ref="..."/>` 引用模块。被引用模块中 handle 匹配的 protocol 和 rpc 会自动注册到该服务中。

## external 外部 Bean 引用

当需要在 solution 中引用未在当前文件中定义的、由 Java 代码直接编写的 Bean 时，使用 `external` 声明。

```xml
<external bean="Zeze.Arch.Beans.BSend"/>
<external bean="Zeze.Services.ServiceManager.BServiceInfo"/>
<externalkey beankey="Zeze.Util.Id128"/>
```

| 元素 | 属性 | 说明 |
|------|------|------|
| `external` | `bean` | 声明一个外部 Bean 的全限定名 |
| `externalkey` | `beankey` | 声明一个外部 BeanKey 的全限定名 |

## servlet HTTP 服务

在模块内定义 HTTP 服务端点。

```xml
<module name="web" id="4">
  <servlet name="hellocount"/>
</module>
```

```xml
<servlet name="Index" TransactionLevel="None"/>
<servlet name="PutRecord" TransactionLevel="Serializable"/>
```

| 属性 | 必填 | 说明 |
|------|------|------|
| **name** | 是 | Servlet 名称，同时作为 URL 路径 |
| **TransactionLevel** | 否 | 事务级别 |

## ModuleStartOrder 模块启动顺序

在 `project` 内可通过 `ModuleStartOrder` 控制模块的启动顺序。

```xml
<project name="server" GenDir="server/Gen" platform="java">
  <ModuleStartOrder>
    <!-- 在此按顺序列出模块名，未列出的模块按默认顺序启动 -->
  </ModuleStartOrder>
  <service name="Server" handle="server">
    <module ref="Login"/>
  </service>
</project>
```

## 跨模块引用

Bean、Table 的 key/value 属性支持跨模块引用，使用完全限定名：

```xml
<!-- 引用其他模块的 Bean -->
<variable id="14" name="bean12" type="demo.Module2.BValue"/>
<variable id="11" name="map11" type="map" key="long" value="demo.Module2.BValue"/>

<!-- 引用顶级 Bean（不属于任何 module） -->
<variable id="9" name="list9" type="list" value="demo.Bean1"/>
```

格式为 `解决方案名.模块名.Bean名`（跨模块）或 `解决方案名.Bean名`（顶级 Bean）。同一模块内可直接使用 Bean 名称。

## 完整示例

以下展示一个典型 solution.xml 的骨架结构：

```xml
<?xml version="1.0" encoding="utf-8"?>
<solution name="Game" ModuleIdAllowRanges="1-1000">
  <!-- 引入其他 solution -->
  <import file="solution.linkd.xml"/>

  <!-- 模块定义 -->
  <module name="Login" id="1">
    <!-- 枚举 -->
    <enum name="ResultCodeDuplicateName" value="1"/>

    <!-- 数据结构 -->
    <bean name="BRole">
      <variable id="1" name="Id" type="long"/>
      <variable id="2" name="Name" type="string"/>
    </bean>

    <!-- 存储表 -->
    <table name="trole" key="long" value="BRole"/>

    <!-- 远程调用 -->
    <rpc name="CreateRole" argument="BCreateRole" result="BRole" handle="server"/>

    <!-- 协议 -->
    <protocol name="SRoleList" argument="BRoles" handle="client"/>
  </module>

  <!-- 项目与服务 -->
  <project name="server" GenDir="server/Gen" SrcDir="server/src" platform="java">
    <service name="Server" handle="server" base="Zeze.Arch.ProviderService">
      <module ref="Login"/>
    </service>
  </project>
</solution>
```
