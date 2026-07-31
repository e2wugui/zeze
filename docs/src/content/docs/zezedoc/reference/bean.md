---
title: "Bean 数据模型"
description: "Zeze Bean 的类型系统、托管状态、版本兼容与 Data 快照速查"
category: reference
order: 2
---

本文是 Zeze **Bean** 的完整 API 参考——覆盖类型系统、字段 id 规则、托管状态、DynamicBean、Data 类和版本兼容，供写代码时随查随用。XML 声明语法见 [solution.xml 参考](./solution-xml.md)，概念讲解见 [定义数据](../manual/03-defining-data.md)。

## 什么是 Bean

Bean 是 Zeze 里**结构化数据的基本单元**，作用类似 ORM 中的实体（Entity）。它由若干 **variable**（变量）组成，**自动参与事务管理**：字段一旦被修改，框架会追踪这次修改、记录事务日志，事务提交时原子地持久化。

Bean 的核心特性：

| 特性 | 说明 |
|------|------|
| 自动事务追踪 | 字段修改自动记日志，提交时原子生效 |
| 版本兼容 | 按 variable id 而非字段名序列化，支持加删字段 |
| 树形结构 | variable 和容器构成树，不重复引用、不成环 |
| 多态 | `dynamic` 类型支持运行时多态 |

---

## 支持的类型

### 基本类型

| 类型 | Java 对应 | 说明 |
|------|-----------|------|
| `byte` | `byte` | 8 位有符号整数 |
| `short` | `short` | 16 位有符号整数 |
| `int` | `int` | 32 位有符号整数 |
| `long` | `long` | 64 位有符号整数 |
| `float` | `float` | 32 位浮点 |
| `double` | `double` | 64 位浮点 |
| `bool` | `boolean` | 布尔 |
| `string` | `String` | UTF-8 字符串 |
| `binary` | `Zeze.Net.Binary` | 字节序列 |
| `decimal` | `java.math.BigDecimal` | 高精度十进制（不可变类型） |

### 向量

| 类型 | 说明 |
|------|------|
| `vector2` | 两 float（8 字节） |
| `vector2int` | 两 int |
| `vector3` | 三 float |
| `vector3int` | 三 int |
| `vector4` | 四 float |
| `quaternion` | 四元数 |

### 容器

| 类型 | 说明 |
|------|------|
| `list` | 有序列表，**元素可以是 Bean** |
| `set` | 无序集合 |
| `map` | 键值映射 |

容器支持任意嵌套（map 的 value 可以是另一个 Bean，Bean 里又可以有 map）。

### 嵌套 Bean 与 dynamic

| 类型 | 说明 |
|------|------|
| Bean 名 | 把一个 Bean 作为另一个 Bean 的字段 |
| `dynamic` | 运行时多态字段，见下文专门章节 |

### 自动类型转换

反序列化时框架会在兼容类型之间**自动转换**，不会因类型微调让历史数据失效：

| 转换方向 | 说明 |
|----------|------|
| 数值 ↔ 数值 | `byte`/`short`/`int`/`long` 之间 |
| `bool` ↔ 数值 | |
| `binary` ↔ `string` | |
| `list` ↔ `set` | |

---

## variable id：序列化和版本兼容的真正「键」

每个 variable 在 Bean 内部有一个**唯一 id**（正整数，最大 4095）。这是 Zeze 数据模型里最重要的概念之一。

| 规则 | 说明 |
|------|------|
| id 用于序列化和版本兼容 | 框架按 id 定位字段，**不是按 Java 字段名** |
| id 在 Bean 内唯一 | 同一 Bean 内不能重复 |
| 新增分配新 id | |
| 删除后不可回收 | 否则旧数据会读到错字段，造成数据错乱 |
| 想复用须定义完全一致 | id、类型、含义全部相同才允许「重新启用」 |

```java
// 你可以自由重命名字段，只要 id 不变：
// XML: <variable id="1" name="level" type="int"/>
//      ↓ 改名，id 不变，数据兼容
// XML: <variable id="1" name="playerLevel" type="int"/>
```

> **为什么不可回收？** 假设字段 A（id=3）被删了，你又把新字段 B 分配了 id=3。线上的旧数据里 id=3 存的是 A 的值，反序列化时会被读进 B——类型还对的话更危险，悄无声息地错。

---

## 托管状态：被框架「接管」的 Bean

Bean 有两种状态，决定了修改是否被事务追踪：

| 状态 | 说明 | 修改是否记日志 |
|------|------|----------------|
| **非托管** | 你自己 `new` 出来的 Bean | 否 |
| **托管** | 被放进 Table，或加入已托管容器后 | 是 |

```java
Item item = new Item();        // 非托管，改字段不记日志
item.setCount(5);              // 不被追踪

Player player = table.getOrAdd(roleId);  // 托管
player.getItems().put(1, item);           // item 现在被托管
item.setCount(10);                        // 这次修改被追踪
```

### 关键规则

| 规则 | 说明 |
|------|------|
| 托管不可逆 | 一旦托管，即使从 Table 移除仍保持托管 |
| 重用须 copy() | 想把已托管 Bean「重置」回可重用状态，调用 `copy()` 得到非托管副本 |
| 检测方法 | `isManaged()` 返回是否托管 |

### 树形结构

从 Table 为根出发，Bean 的 variable 和容器构成**一棵树**：

| 约束 | 说明 |
|------|------|
| 不重复引用 | 同一个节点不会被引用两次 |
| 不成环 | 不会形成引用环 |
| 父子关联 | `parent()` 取父节点，`variableId()` 取所属字段 id |

这种树形约束是事务追踪得以简单可靠的基础。

---

## DynamicBean：运行时多态

用一个字段在不同时刻持有不同类型的 Bean。用 `type="dynamic"` 声明，再用 `<value>` 列出可能类型。

```java
// 取当前实际持有的 Bean
Pet pet = role.getPartner().getBean();
// 取 typeId（标识当前是哪种类型）
int typeId = role.getPartner().getTypeId();
// 设置 Bean（必须是用 <value> 列出过的类型）
role.getPartner().setBean(new Mount());
// 是否未设置
boolean empty = role.getPartner().isEmpty();
```

| 方法 | 说明 |
|------|------|
| `getBean()` | 取当前持有的 Bean（返回值需自行强转） |
| `setBean(bean)` | 设置 Bean，必须声明过 |
| `getTypeId()` | 取当前 typeId，标识持有哪种类型 |
| `isEmpty()` | 是否未设置 |

| 约束 | 说明 |
|------|------|
| 未设置时 | 内部用 `EmptyBean`（typeId=0）表示 |
| 不支持嵌套 | dynamic 字段里再套 dynamic 不允许 |

---

## Data 类：纯数据快照

每个 Bean 都对应一个 **Data 类**，是一份纯数据快照，**不参与事务**。专门用于跨线程传递和 RPC 序列化。

| 方法 | 说明 |
|------|------|
| `toData()` | 把当前托管 Bean 导出成一份快照 |
| `assign(data)` | 用一份快照的数据覆盖当前 Bean |
| `copy()` | 复制一份非托管的 Bean |
| `reset()` | 清空数据（恢复默认值） |

```java
// 跨线程传递：先 toData 取快照
PlayerData snapshot = player.toData();
executor.submit(() -> process(snapshot));  // 传不可变快照

// 从快照恢复
player.assign(snapshot);
```

### 集合转换

Data 与 Bean 的集合相互转换：

| 方法 | 说明 |
|------|------|
| `Bean.toDataList(beanList)` | List<Bean> → List<Data> |
| `Bean.toBeanList(dataList)` | List<Data> → List<Bean>（新建非托管） |
| `Bean.toDataMap(beanMap)` | Map<K,Bean> → Map<K,Data> |
| `Bean.toBeanMap(dataMap)` | Map<K,Data> → Map<K,Bean>（新建非托管） |

---

## 版本兼容规则

数据结构会随业务演进，Zeze 的兼容性规则让你在不停服前提下加字段、删字段：

| 变更 | 处理方式 |
|------|----------|
| **新增 variable** | 旧数据反序列化时该字段取默认值 |
| **删除 variable** | 旧数据里对应的 id 被忽略 |
| **类型变更** | 仅限兼容类型间（数值之间、bool↔数值、binary↔string、list↔set） |

### 核心原则

> **兼容性只看 variable 的 id 和类型，与类名、typeId 无关。** 只要 id 和类型对得上，数据就能正确读出来。

---

## BeautifulVariableId

| 用途 | 说明 |
|------|------|
| 开发期 | 重置字段的 id 为连续值，让生成的代码更整洁 |
| 发布后 | **绝不能用**，会改变既有字段 id，是一次不兼容修改 |

---

## version 字段

Bean 可声明一个类型为 `long` 的 `version` 字段（在 XML 里用 `<bean name="Player" version="ver">`，其中 `version` 属性的值 **`ver` 是本 Bean 内一个已声明的 long 变量的名字**，框架会校验该变量存在且为 long 类型）：

| 规则 | 说明 |
|------|------|
| 定义后 | 对 Bean 的修改让 `version` **自动递增** |
| 仅对 Table.Value 有效 | 作为表值的那个 Bean |
| 非 Table.Value | `version()` 返回 0 |
| 删除重建 | version 从 0 重新开始 |
| 用途 | 乐观锁、变更追踪 |

```java
Player player = table.getOrAdd(roleId);
long oldVer = player.version();          // 方法名是 version()，不是 getVersion()
player.setLevel(player.getLevel() + 1);
long newVer = player.version();          // 自动 +1
```

---

## 短代码示例

一个典型的 Bean 定义与使用：

```java
// XML:
// <bean name="Player" version="ver">
//     <variable id="1" name="name"  type="string"/>
//     <variable id="2" name="level" type="int"/>
//     <variable id="3" name="coins" type="long"/>
//     <variable id="4" name="items" type="map[int,Item]"/>
// </bean>

// 使用（在事务内）
Player player = tableTPlayer.getOrAdd(roleId);
player.setLevel(player.getLevel() + 1);          // 被追踪
player.getCoins();                                // 读取
player.getItems().put(100, new Item(100, 1));    // 嵌套 Bean 托管
//   注：框架生成的 Bean 只有无参构造；这里的带参构造需业务自行实现

// 导出快照（跨线程/RPC）
PlayerData data = player.toData();

// 复制
Player copy = player.copy();   // 非托管副本
```

## 相关文档

- [solution.xml 参考](./solution-xml.md) — XML 声明语法、所有元素属性
- [序列化协议](./serialize.md) — variable id 如何编码、类型自动转换细节
- [Table 存储接口](./table.md) — Bean 作为 Table value 的 CRUD
- [定义数据](../manual/03-defining-data.md) — 概念讲解
