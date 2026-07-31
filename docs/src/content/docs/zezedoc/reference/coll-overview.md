---
title: "持久化集合总览"
description: "Zeze Collections 模块提供的持久化集合类型、统一初始化方式与共同约束"
category: reference
order: 40
---

> 本文档总览 Zeze Collections 模块提供的全部持久化集合类型、统一初始化方式、值类型注册机制与共同约束，作为各集合文档的索引入口。

## 模块定位

Zeze **Collections** 模块提供一系列**持久化集合**。所有操作在**事务**中执行，数据自动同步到配置的数据库。集合基于 `Table` 抽象层，因此可以透明地运行在 **RocksDB / MySQL / PostgreSQL / MongoDB** 等后端之上。

集合对 `Table` 做了高层封装：内部管理链表节点、哈希分桶等结构，把数据落到内部 Table，对外暴露队列、有序映射、位图、图等业务友好的 API。

## 统一初始化

每个集合类型都有一个内部 `Module` 类，负责把内部 Table 注册到 `Zeze.Application`。通过 `open` 方法打开一个**具名实例**。

```java
// 1. 注册各集合 Module 到 Application
var queueModule        = new Queue.Module(zeze);
var linkedMapModule    = new LinkedMap.Module(zeze);
var boolListModule     = new BoolList.Module(zeze);
var dagModule          = new DAG.Module(zeze);
// DepartmentTree 依赖一个已注册的 LinkedMap Module
var departmentTreeModule = new DepartmentTree.Module(zeze, linkedMapModule);

// 2. 打开具名实例（名称即数据库 Key）
var taskQueue = queueModule.open("myQueue", Task.class);
```

## 值类型与 BeanFactory

存入集合的值类型必须继承 `Zeze.Transaction.Bean`，并通过 **`BeanFactory`** 注册。

`BeanFactory` 是统一的类型注册中心，建立 `typeId(long)` 与 `Bean` 构造器的映射。集合内部用 `DynamicBean` 保存值，反序列化时通过 `createBeanFromSpecialTypeId` 动态创建出正确的具体类型。

| BeanFactory 方法 | 说明 |
|------------------|------|
| `register(Class)` | 注册一个 Bean 类型，分配/记录 typeId |
| `createBeanFromSpecialTypeId(typeId)` | 根据 typeId 动态创建 Bean 实例 |
| `typeId(Class)` | 查询类型对应的 typeId |
| `findClass(typeId)` | 根据 typeId 反查类型 |

```java
public class Task extends Zeze.Transaction.Bean {
    // 业务字段 ...
    static {
        BeanFactory.register(Task.class);
    }
}
```

> 所有集合通过 **`HotBeanFactory`** 接口支持值类型的热重载，无需重启即可更新 Bean 结构。

## 与 Table 的关系

每个集合 `Module` 在构造时调用 `RegisterZezeTables(zeze)` 注册其内部 Table。集合本身不绕过事务，而是使用标准 `Table` 接口，因此天然享受：

- 数据自动同步到数据库
- 乐观锁（多版本并发控制）
- 事务原子性与隔离

换言之，集合是 `Table` 之上的高层封装。详见 [./table.md](./table.md)。

## 集合类型一览

| 类型 | 形态 | 典型场景 |
|------|------|----------|
| [**Queue**](./coll-queue.md) | FIFO 队列 / LIFO 栈 | 消息队列、任务调度、操作历史 |
| [**LinkedMap**](./coll-linked-map.md) | 有序双向链表映射 | 背包、排行榜、有序列表 |
| [**CHashMap**](./coll-chashmap.md) | 取模分桶并发 Map（依赖 LinkedMap） | 高并发随机读写 |
| [**DepartmentTree**](./coll-department-tree.md) | 部门树 | 组织架构、权限管理 |
| [**BoolList**](./coll-bool-list.md) | 位图列表 | 标记位、开关集合 |
| [**DAG**](./coll-dag.md) | 有向无环图 | 依赖管理、任务编排 |

## 共同约束

使用任何持久化集合都必须遵守以下约束：

| 约束 | 说明 |
|------|------|
| **必须在 Procedure 中读写** | 所有集合的读写操作都必须在事务（Procedure）内执行 |
| **名称不能包含 `@`** | `@` 是 Zeze 内部保留的分隔符，实例名中出现会导致冲突 |
| **值必须继承 Bean** | 存入集合的值类型必须继承 `Zeze.Transaction.Bean` 并经 `BeanFactory` 注册 |
| **支持值类型热重载** | 所有集合通过 `HotBeanFactory` 接口支持 Bean 结构的热更新 |

事务机制详见 [./transaction.md](./transaction.md)。

## 相关文档

- Queue 持久队列：[./coll-queue.md](./coll-queue.md)
- LinkedMap 有序映射：[./coll-linked-map.md](./coll-linked-map.md)
- CHashMap 一致性哈希：[./coll-chashmap.md](./coll-chashmap.md)
- DepartmentTree 部门树：[./coll-department-tree.md](./coll-department-tree.md)
- BoolList 位图列表：[./coll-bool-list.md](./coll-bool-list.md)
- DAG 有向无环图：[./coll-dag.md](./coll-dag.md)
- Table 抽象层：[./table.md](./table.md)
- 事务机制：[./transaction.md](./transaction.md)
