---
title: "Bean 数据模型"
sidebar:
  order: 1
---

**Bean** 是 Zeze 中结构化数据的基本单元，类似传统 ORM 中的"实体"，但自动参与事务管理——对 Bean 字段的修改会被框架追踪、记录日志，并在事务提交时原子地持久化到数据库。Bean 在 solution.xml 中声明（→ solution-xml），由代码生成器产出 Java 类。

## 支持的类型

每个 Bean 由若干 **variable**（变量）组成，variable 支持以下类型：

| 分类 | 类型 | 说明 |
|------|------|------|
| 整数 | `byte`, `short`, `int`, `long` | 定长整数 |
| 浮点 | `float`, `double` | IEEE 754 浮点 |
| 布尔 | `bool` | true / false |
| 字符串 | `string` | UTF-8 编码 |
| 二进制 | `binary` | 原始字节序列 |
| 向量 | `vector2`, `vector3`, `vector4` | 2/3/4 维浮点向量 |
| 容器 | `list`, `set`, `map` | 持久化集合，元素可为 Bean |
| 嵌套 Bean | 任意 Bean 类型 | 结构体嵌套 |
| 动态 Bean | `dynamic` | 运行时多态，见下文 |

所有数值类型（byte ~ double）之间以及 bool 与数值之间在反序列化时自动兼容转换，规则与 Java 强转一致。`binary` 与 `string` 也互相兼容（UTF-8 编解码）。序列容器（list、set）之间互相兼容，容器内元素类型按上述规则处理。

## Variable Id

每个 variable 拥有在 Bean 内唯一的 **id**（正整数，最大值 4095）。Id 用于序列化时的字段标识和版本兼容，而不是字段名称。因此在 Bean 生命周期中：

- **新增** variable 时分配一个新的、从未使用过的 id
- **删除** variable 后其 id **不可回收**，否则反序列化旧数据时会读到错误的字段
- 如果确实需要复用 id，必须保证字段定义完全一致——这被视为"反悔操作"

```xml
<bean name="BRole">
  <variable id="1" name="Id" type="long"/>
  <variable id="2" name="Name" type="string"/>
  <variable id="3" name="Level" type="int"/>  <!-- 新增字段，分配新 id=3 -->
</bean>
```

## Bean 的生命周期：托管与非托管

Bean 有两种状态：

- **非托管**：刚通过 `new` 创建的 Bean 处于非托管状态，此时对其字段的修改不记录事务日志
- **托管（Managed）**：当 Bean 被放入 Table 或加入一个已托管的 Bean 容器后，进入托管状态

可以通过 `isManaged()` 方法检测当前状态：

```java
BRole role = new BRole();    // 非托管
assert !role.isManaged();

// 假设 trole 是一个 Table<long, BRole>
trole.insert(roleId, role);  // 进入托管状态
assert role.isManaged();
```

关键约束：**托管状态一旦设置就不可逆**。即使从 Table 或容器中移除该 Bean，它仍然保持托管状态。若要重新使用，需调用 `copy()` 创建一份新的副本。

## 树形结构

从 Table 为根出发，Bean 的 variable 和容器构成一棵 **树**。每个 Bean 实例不会被重复引用，也不会出现环。框架内部通过 `parent()` 和 `variableId()` 维护这棵树的父子关系，用于日志记录和变更通知（→ listener）。

```java
Bean parent = role.parent();      // 获取父 Bean
int varId  = role.variableId();   // 获取在父 Bean 中的 variable id
```

## DynamicBean：运行时多态

**DynamicBean** 允许一个 variable 在运行时持有不同类型的 Bean 实例。框架通过 `typeId()` 标识具体类型，序列化时先写入 typeId 再写入 Bean 数据。

在 solution.xml 中用 `type="dynamic"` 声明，并通过 `<value>` 列出所有可能的 Bean 类型：

```xml
<bean name="BItem">
  <variable id="3" name="Extra" type="dynamic">
    <value bean="BFoodExtra"/>
    <value bean="BEquipExtra"/>
  </variable>
</bean>
```

生成的代码提供 `setBean()` / `getBean()` 方法来读写动态 Bean：

```java
BItem item = new BItem();
item.getExtra().setBean(new BEquipExtra());  // 设置为装备扩展

Bean extra = item.getExtra().getBean();      // 读取当前 Bean
long  tid  = item.getExtra().getTypeId();    // 读取当前 typeId

if (item.getExtra().isEmpty()) { ... }       // 检查是否为空（EmptyBean）
```

DynamicBean 内部使用 `EmptyBean` 表示"未设置"状态，其 typeId 为 0。不支持嵌套 DynamicBean。

## Bean 与 Data

每个生成的 Bean 都对应一个 **Data** 类（`Zeze.Transaction.Data` 的子类）。Data 是 Bean 的纯数据快照，不参与事务管理，适合用于跨线程传递、RPC 序列化等场景。

| 方法 | 说明 |
|------|------|
| `bean.toData()` | 从 Bean 创建 Data 快照 |
| `bean.assign(data)` | 将 Data 的值赋给 Bean |
| `bean.copy()` | 深拷贝一个非托管的新 Bean |
| `bean.reset()` | 将所有字段重置为默认值 |

对于集合转换，Bean 类提供了静态工具方法：

```java
// Bean 列表 ↔ Data 列表
Bean.toDataList(beans, datas);
Bean.toBeanList(datas, beans);

// Bean 映射 ↔ Data 映射
Bean.toDataMap(beans, datas);
Bean.toBeanMap(datas, beans);
```

## 版本兼容规则

Zeze 的序列化以 variable id 为标识（→ serialize），具备向前/向后兼容能力：

1. **新增 variable**：旧数据中不存在的字段自动取默认值（0、false、空字符串、空容器、所有字段为默认值的 Bean、内容为 EmptyBean 的 DynamicBean）
2. **删除 variable**：旧数据中存在但当前 Bean 中不定义的 id 在反序列化时被忽略，再次序列化时自动丢弃
3. **类型变更**：仅限兼容类型之间（数值互换、binary/string 互换、序列容器互换、dynamic/Bean 互换），不兼容的类型变更会导致数据丢失

:::caution
Bean 的兼容性只看 **variable id** 和 **variable 类型**，与 Bean 的类名、typeId 无关。发布后切勿复用已删除的 variable id。
:::

## BeautifulVariableId

关于 Bean 增减 variable 的建议：按 variable.id 的顺序从 1 开始自增地分配和扩展；删除 variable 不要直接删除，可以修改 variable.name 或注释来表示"临时不再使用"的含义，方便保留数据库中已有数据不丢失，以备之后再恢复使用，也防止增加 variable 时重用该 variable.id 引发取出旧数据的混乱。

开发过程中由于实现方案修改，被删除的 variable 比较多，造成 id 不连续。可利用 `Gen.exe -BeautifulVariableId` 功能重置 id，使得从 1 开始连续编号。**这个操作是不兼容的修改，只能用于开发期删除所有数据库数据时一并处理。一旦程序发布，就不能再使用 BeautifulVariableId 功能。** 使用后需重新生成相关代码。

## Version

Table.Value 可以定义一个版本号，修改发生时自动递增：

```xml
<bean name="BVersionSample" version="VersionVarName">
    <variable id="1" name="VersionVarName" type="long"/>
</bean>
```

- 版本变量名可自定义，在 Bean 的 `version` 属性中声明
- 类型必须是 `long`
- 通过 `Bean.getVersion()` 获取当前版本号
- 如果 Bean 没有作为 Table.Value，`getVersion()` 总是返回 0
- 如果记录被删除后再次加入，版本号从 0 重新开始
