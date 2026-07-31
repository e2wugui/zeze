---
title: "定义数据"
description: "用 Bean、Table 和 solution.xml 三件套搭建 Zeze 应用的数据模型"
category: manual
order: 3
---

读完这篇，你能在 `solution.xml` 里声明自己的 Bean 和 Table，并理解字段 id、托管状态、版本兼容等机制如何保证数据随业务安全演进。

## 为什么用 XML 定义数据

传统 ORM 的思路是先写 Java 实体类，再想办法把它映射到数据库表。Zeze 把这个顺序倒了过来：你先在一个叫 `solution.xml` 的文件里**描述**数据长什么样，框架再据此生成访问代码、序列化逻辑和持久化支持。

这么做有三个直接好处：

1. **访问代码自动生成。** 你不需要手写 getter/setter、序列化方法、数据库映射。声明一个 `<bean>`，代码生成器就给你产出一个完整的 Java 类，字段、容器、嵌套结构一应俱全。
2. **序列化是「免费的」。** 因为数据的结构由框架掌握，框架知道每个字段的 id 和类型，二进制序列化、版本兼容、跨进程传输都不需要你操心。
3. **持久化与事务透明。** 你只要把 Bean 放进 Table，对字段的每一次修改都会被框架追踪、记入事务日志，提交时原子落库——不需要手动 `save()`。

理解了这三点，你就理解了为什么 Zeze 的数据定义是「声明在先，代码在后」。下面依次看三个主角：Bean、Table、solution.xml。

## Bean：结构化数据的基本单元

Bean 是 Zeze 里结构化数据的基本单位，作用类似 ORM 中的实体（Entity），但它**自动参与事务管理**：字段一旦被修改，框架会追踪这次修改、记录事务日志，事务提交时原子地持久化。它由若干 **variable**（变量）组成，在 `solution.xml` 里声明，由代码生成器产出 Java 类。

### 支持的类型

一个 variable 可以是以下类型：

- **整数**：`byte` / `short` / `int` / `long`
- **浮点**：`float` / `double`
- **布尔**：`bool`
- **字符串**：`string`（UTF-8 编码）
- **字节序列**：`binary`
- **向量**：`vector2` / `vector3` / `vector4`
- **持久化集合**：`list` / `set` / `map`，元素本身可以是 Bean
- **嵌套 Bean**：把一个 Bean 作为另一个 Bean 的字段
- **dynamic**：运行时多态，下文专门讲

值得注意的一点是，反序列化时框架会在兼容类型之间**自动转换**：数值类型之间、`bool` 与数值、`binary` 与 `string`、`list` 与 `set` 都能平滑互转。这意味着你不会因为把一个字段从 `int` 调整成 `long`，或从 `list` 换成 `set` 而让历史数据失效。

### 字段 id：序列化和版本兼容的真正「键」

每个 variable 在 Bean 内部有一个**唯一 id**（正整数，最大 4095）。这个 id 非常关键，必须理解几条规则：

- **id 用于序列化和版本兼容，不是字段名。** 框架在序列化时按 id 定位字段，而不是按 Java 字段名。这意味着你可以自由重命名字段，只要 id 不变。
- **新增 variable 时分配一个全新的 id。**
- **删除 variable 后，它的 id 永远不可回收。** 否则旧数据反序列化时会把本应被忽略的字段读到新字段里，造成数据错乱。

> 为什么不可回收？想象字段 A（id=3）被删了，你又把新字段 B 分配了 id=3。线上的旧数据里 id=3 位置存的是 A 的值，反序列化时会被读进 B——类型还对的话更危险，悄无声息地错。

与此相关的是 **BeautifulVariableId**：开发阶段可以用它把字段的 id 重排成连续值，让生成的代码更整洁。但**发布上线后绝不能再用**，因为它会改变既有字段的 id，是一次不兼容修改。

### 托管状态：被框架「接管」的 Bean

Bean 有两种状态：

- **非托管**：你自己 `new` 出来的 Bean，改它的字段不会记事务日志。
- **托管**：当一个 Bean 被放进 Table，或被加入一个已托管的容器（list/set/map/嵌套位置）后，它就进入托管状态。

托管状态一旦设置就**不可逆**——即使之后把它从 Table 里移除，它依然保持托管。如果你想把一个已托管的 Bean「重置」回可重用状态，需要调用 `copy()` 得到一份新的非托管副本。

从 Table 为根出发，Bean 的 variable 和容器构成**一棵树**：不会重复引用同一个节点，也不会形成环。父子的关联由 `parent()` 和 `variableId()` 维护。这种树形约束是事务追踪得以简单可靠的基础。

### DynamicBean：运行时多态字段

有时你希望一个字段在不同时刻持有不同类型的 Bean。`dynamic` 类型就是为此而生：用 `type="dynamic"` 声明一个 variable，再用 `<value>` 列出它可能持有的 Bean 类型。运行时框架通过 `typeId()` 标识当前实际持有的是哪一种。

几个细节：未设置时内部用 `EmptyBean`（`typeId=0`）表示；**不支持嵌套 DynamicBean**（dynamic 字段里再套一个 dynamic 是不行的）。

### Data 类：纯数据快照

每个 Bean 都对应一个 **Data 类**。它是一份纯数据快照，**不参与事务**，专门用于跨线程传递和 RPC 序列化。相关的转换方法：

- `toData()`：把当前托管 Bean 导出成一份快照。
- `assign(data)`：用一份快照的数据覆盖当前 Bean。
- `copy()`：复制一份非托管的 Bean。
- `reset()`：清空数据。

当你需要把数据交给另一个线程或通过网络发出去时，先 `toData()` 取快照，再传递——这样接收方拿到的是不可变的数据，不会受事务回滚影响。

### 版本兼容的规则

数据结构会随业务演进，Zeze 的兼容性规则让你在不停服的前提下加字段、删字段：

- **新增 variable**：旧数据反序列化时该字段取默认值。
- **删除 variable**：旧数据里对应的 id 被忽略。
- **类型变更**：仅限前文提到的兼容类型之间（数值之间、bool↔数值、binary↔string、list↔set）。

核心原则是：**兼容性只看 variable 的 id 和类型，与类名、typeId 无关。** 只要 id 和类型对得上，数据就能正确读出来。

### version 字段

Bean 还可以定义一个类型为 `long` 的 `version` 字段。在 XML 里写成 `<bean name="Player" version="ver">`——其中 `version` 属性的值（这里是 `ver`）是本 Bean 内**一个已声明的 long 变量的名字**，框架据此识别哪个变量充当版本号。注意它只对 **Table.Value**（作为表值的那个 Bean）有效。

#### 递增时机：每次脏事务提交时 +1

version 在**事务提交时**（`Transaction.finalCommit`）递增，确切说是：**每次"脏事务"提交 +1，纯读事务不增**。Checkpoint 落地次数不影响它。

要准确理解，需要看清 Zeze 的两层时间线：

- **提交时间线（Procedure 级）**：Procedure 执行成功后，`finalCommit` 把字段修改写入内存里的 Bean 对象，标脏，并**在这一步让 version +1**（`Transaction.java:486`）。一个 Procedure 改了字段就算一次"脏提交"。
- **落库时间线（Checkpoint 级）**：后台 Checkpoint 线程周期性地把标脏记录序列化写进后端数据库。这一步只是"拍照存盘"——把内存里已经累加到某个值的 version 原样写出去，**本身不再 +1**。

所以 version 衡量的是**这条记录被提交修改了几次**，而不是被落地了几次。两条规则：

- **只有改了字段的提交才 +1**。纯读事务（不产生修改日志）标不了脏，不会进入自增分支。删除记录时也不递增——`getSoftValue()` 返回 `null` 会跳过自增。
- **如果记录被删除后再次插入，version 从 0 重新开始**。

> **一个直观的例子**：一条记录 version 初始为 0，连续提交 100 次修改、期间 Checkpoint 落地 20 次。最终 version = **100**。那 20 次落地只是把累加过程中的值陆续写进数据库，最终值取决于提交次数，不取决于落地次数。

#### API

生成器会在子类里重写基类 `Bean` 的三个方法（基类定义在 `Zeze.Transaction.Bean`）：

- `long version()`：读取当前版本号。
- `String versionVarName()`：返回充当版本号的变量名（框架内部用来在 diff、History 等场景定位该字段）。
- `protected void version(long v)`：设置版本号，由框架在提交时调用，业务代码一般不直接用。

> ⚠️ 方法名是 `version()`，**不是** `getVersion()`。

#### 使用示例

```xml
<module name="role" id="1">
  <!-- version 属性指向 Bean 内一个 long 变量的名字 -->
  <bean name="BPlayer" version="ver">
    <variable id="1" name="ver"    type="long"/>    <!-- 名字必须等于 version 属性的值 -->
    <variable id="2" name="name"   type="string"/>
    <variable id="3" name="level"  type="int"/>
  </bean>

  <table name="tPlayer" key="long" value="BPlayer"/>
</module>
```

每条 `tPlayer` 记录每次被提交修改时，它的 `ver` 字段自动 +1；业务代码随时可用 `player.version()` 读取当前版本号。常用于：数据变更追踪、增量同步、乐观锁。

#### 跨语言支持

| 平台 | 支持 version |
|------|:---:|
| Java | ✅ |
| C# | ✅ |
| C++ | ✅ |
| **TypeScript** | ❌ |
| **Python** | ❌（属性会被解析，但不生效） |

TypeScript 和 Python 平台即使声明了 `version="..."`，也不会有自动递增行为。

## Table：类型安全的 Map

如果说 Bean 是「一行数据」，Table 就是「这张表」。它的语义等价于一个**类型安全的 `Map<K, V>`**：key 必须实现 `Comparable`（可以是简单类型或复合的 `beankey`），value 必须是一个 Bean。在 `solution.xml` 里用 `<table>` 声明后，代码生成器会产出一个 `TableXxx<K, V>` 子类。

### CRUD 操作

基本读写都在事务内进行：

- `get(key)`：取值，不存在返回 `null`。
- `getOrAdd(key)`：取值，不存在则创建。可以传一个 `OutObject<Boolean>` 来判断这次是不是新建的。
- `put` / `insert` / `tryAdd`：写入（语义略有差异）。
- `remove(key)`：删除。
- `contains(key)`：判断是否存在。

> 重要：**所有 CRUD 操作都必须在事务内调用。** Table 的事务特性依赖于框架对每次访问的记录，脱离事务框架无法保证一致性。

### 事务外读取

有时候你不在事务里，但又想读数据。Table 提供两种事务外读取方式，各有适用场景：

- **`selectCopy`**：返回一份数据的**深拷贝**。事务内外都能用，语义最安全，但代价是要做一次拷贝。
- **`selectDirty`**：从本地缓存快速读取。默认有 3 秒有效期，**不经过 GCM（全局缓存管理）协商**，因此一致性较弱，适合用在 `whileCommit` 里的预读、统计聚合等「能容忍轻微陈旧」的场景。

### 缓存是透明的

Table 背后有一套 **TableCache**：主存储是一个 `ConcurrentHashMap`，配合分段 LRU 做淘汰。此外，本地还维护一个 **RocksDB 缓存表**，value 用 `SoftReference` 持有——当内存吃紧被 GC 回收后，还能从本地 RocksDB 恢复。

这套设计的一个动机是：让 `cache.capacity` 只影响 Record1 条目数，**与 value 本身的大小无关**。默认值 `20000 × 5.0 = 100000` 基本不需要调整，大多数场景直接用默认即可。

这里还有一条贯穿始终的**第一原则**：本地 RocksDB 始终与后端数据库保持一致——写入时同时写远程和本地；加载时从远程加载后回写本地；删除时同步删本地。这保证了即便缓存未命中，回源后的数据也是可信的。

`Storage` 是 Table 与数据库之间的桥梁。对于纯内存表（`isMemory()` 为 true），`Storage` 为 `null`，脏记录由 Checkpoint 统一刷写。

### 遍历

需要全表扫描时，Table 提供了几种事务外遍历方式：

- **`walk` / `walkDesc`**：遍历数据库并合并缓存，得到最完整的结果。回调返回 `false` 可以提前中断。
- **`walkDatabase`**：直接遍历数据库，**不经过缓存**。
- **`walkMemory`**：只遍历内存缓存里的数据。

根据你想要「准」还是「快」选择合适的方式。

### 可选属性

声明 `<table>` 时可以加几个有用的属性：

- **`suffix`**：表名后缀，支持 `@ServerId`、`@AppMainVersion` 等变量替换，常用来做多服数据隔离。
- **`memory`**：声明为纯内存表。
- **`autokey`**：自动键，取值 `"true"`（仅 long key）或 `"random"`（仅 binary key）。

## solution.xml：把它们串起来

`solution.xml` 是 Zeze 的核心建模文件，Bean、Table、Protocol、Rpc、Project 全都在这里定义。先看它的整体结构。

根元素是 `<solution>`：

```xml
<solution name="demo" ModuleIdAllowRanges="1-1000">
```

- **`name`**：顶层命名空间，生成的 Java 类会落在它下面。
- **`ModuleIdAllowRanges`**：模块 id 的合法范围，支持区间（`1-1000`）、离散值（`100`）、逗号分隔混合。超出范围的模块 id 会在生成时报错，防止不同服务之间撞 id。

### 引入其他文件

```xml
<import file="other.xml"/>
```

用 `<import>` 可以把数据模型拆分到多个文件里，便于大型项目分模块管理。

### 模块（module）

`<module>` 是逻辑模块，id **全局唯一**，可以嵌套，内部可以包含 bean、table、rpc、protocol、enum：

```xml
<module name="role" id="1">
  <!-- 这里的 bean/table 都属于 role 模块 -->
</module>
```

### Bean 与 variable

`<bean>` 内部用 `<variable>` 描述字段：

```xml
<bean name="Player">
  <variable id="1" name="level" type="int"/>
  <variable id="2" name="name" type="string"/>
</bean>
```

`<beankey>` 的写法类似，但它是用来做**复合键**的，可以为空（空表示无字段的占位键）。

variable 的 `type` 支持基本类型、集合、向量、引用、dynamic。集合支持**方括号简写**：

```xml
<variable id="3" name="friends" type="set[long]"/>
<variable id="4" name="scores" type="map[int,float]"/>
```

还可以用 `javaType` 把生成的集合**特化**成更高效的具体实现（比如 `IntList`、`LongHashMap`），省去装箱开销。

### Table

```xml
<table name="tRole" key="long" value="Player"/>
```

`key` 可以是基本类型或 `beankey`，`value` 必须是 bean。这就把前面定义的 `Player` 绑成了一张可持久化的表。

### Rpc 与 Protocol

```xml
<rpc name="login" argument="LoginArg" result="LoginRes" handle="server"/>
<protocol name="heartbeat" argument="EmptyBean" handle="server"/>
```

`handle` 指明由谁来处理，可以是 `server` / `client` / `serverscript` / `clientscript`，并且可以组合（比如 `server,client` 表示两边都生成处理代码）。这部分在 [编写业务逻辑](./04-writing-logic.md) 和分布式章节会更深入。

### Project 与 Service

```xml
<project name="GameServer" GenDir="gen" SrcDir="src" platform="java">
  <service name="GameServer" handle="server" base="main">
    <module ref="role"/>
  </service>
</project>
```

一个 `<project>` 对应一个进程，定义代码生成的目标目录（`GenDir` 生成目录、`SrcDir` 源码目录、`platform` 平台）。`<service>` 里用 `<module ref>` 引用前面定义的模块，决定这个进程加载哪些模块的逻辑。

### Enum

```xml
<enum name="ERROR_PARAM" value="1" comment="参数错误"/>
```

`<enum>` 可以定义在 bean、rpc 或 module 内，用来声明常量或错误码，生成代码里会得到对应的常量。

## 一个完整的例子

把上面这些拼起来，下面是一个最小但完整的数据定义示例：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<solution name="demo" ModuleIdAllowRanges="1-1000">

  <module name="role" id="1">

    <!-- 玩家数据：作为 Table 的 value -->
    <bean name="Player">
      <variable id="1" name="name"   type="string"/>
      <variable id="2" name="level"  type="int"/>
      <variable id="3" name="coins"  type="long"/>
      <variable id="4" name="items"  type="map[int,Item]"/>
    </bean>

    <!-- 道具：嵌套 Bean -->
    <bean name="Item">
      <variable id="1" name="configId" type="int"/>
      <variable id="2" name="count"    type="int"/>
    </bean>

    <!-- 玩家表：long 主键 -> Player -->
    <table name="tPlayer" key="long" value="Player"/>

    <!-- 多服隔离示例：按 ServerId 自动加后缀 -->
    <table name="tMail" key="long" value="Mail" suffix="_@ServerId"/>

    <enum name="ERR_COIN_NOT_ENOUGH" value="1" comment="金币不足"/>
  </module>

  <project name="GameServer" GenDir="gen" SrcDir="src" platform="java">
    <service name="GameServer" handle="server" base="main">
      <module ref="role"/>
    </service>
  </project>

</solution>
```

运行代码生成器后，你会得到 `Player`、`Item` 两个 Bean 类、`TableTPlayer`、`TableTMail` 两张表，以及对应的 Data 快照类。所有序列化、缓存、事务追踪都不需要你写一行。

## 小结

回顾一下三者的协同：**solution.xml 是蓝图**，描述数据结构；**Bean 是结构化数据的载体**，字段带 id、状态可托管、版本向前兼容；**Table 是类型安全的 Map**，把 Bean 接入事务和持久化，缓存对你透明。三者通过声明串联，框架据此生成全部访问代码。

想了解每个元素的完整属性列表，参阅 [solution.xml 参考](../reference/solution-xml.md)、[bean 参考](../reference/bean.md)、[table 参考](../reference/table.md)。

数据模型搭好了，下一步就是往里写业务逻辑——下一章 [编写业务逻辑](./04-writing-logic.md) 会讲透存储过程、事务回滚和副作用处理。
