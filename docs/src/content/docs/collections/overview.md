---
title: "持久化集合总览"
sidebar:
  order: 1
---

Zeze Collections 模块提供了一系列**持久化集合**数据结构，所有操作都在事务中执行，数据自动同步到配置的数据库后端。这些集合基于 Zeze 的 **Table** 抽象层构建，底层支持 RocksDB、MySQL、PostgreSQL、MongoDB 等多种存储引擎。

## 统一初始化方式

所有集合都遵循相同的初始化模式：通过各自的 **`Module`** 内部类注册到 `Zeze.Application`，然后使用 `open` 方法打开具名实例。

```java
// 通用初始化模式
Zeze.Application zeze = new Zeze.Application(config);

var queueModule = new Queue.Module(zeze);
var linkedMapModule = new LinkedMap.Module(zeze);
var boolListModule = new BoolList.Module(zeze);
var dagModule = new DAG.Module(zeze);
var deptTreeModule = new DepartmentTree.Module(zeze, linkedMapModule);

// 打开具名集合实例
Queue<Task> q = queueModule.open("myQueue", Task.class);
LinkedMap<Item> lm = linkedMapModule.open("myMap", Item.class);
```

每个集合的值类型必须继承自 `Zeze.Transaction.Bean`，并通过 [**BeanFactory**](#beanfactory-注册机制) 完成类型注册。

## BeanFactory 注册机制

**`BeanFactory`** 是集合模块的统一类型注册中心，负责将 `typeId`（long）映射到具体的 Bean 构造器。集合内部使用 `DynamicBean` 存储值，在反序列化时通过 `createBeanFromSpecialTypeId` 动态创建正确的 Bean 实例。

核心方法：

| 方法 | 说明 |
|------|------|
| `register(Class<? extends Serializable> cls)` | 注册 Bean 类型，返回构造器 |
| `createBeanFromSpecialTypeId(long typeId)` | 根据 typeId 创建 Bean 实例 |
| `typeId(Class<? extends Bean> beanClass)` | 获取 Bean 的 typeId |
| `findClass(long typeId)` | 根据 typeId 查找 Bean 类 |

## 与 Table 的关系

每个集合的 `Module` 在构造时会调用 `RegisterZezeTables(zeze)` 将内部 Table 注册到框架。这些 Table 使用 Zeze 的标准 `Table` 接口，享受自动内存-数据库同步、乐观锁并发控制等特性。集合本身是对 Table 的高层封装，管理链表节点、哈希分桶等数据结构。

## 集合类型一览

| 集合 | 说明 | 适用场景 |
|------|------|----------|
| [Queue](./queue) | FIFO 队列 / LIFO 栈 | 消息队列、任务调度、操作历史 |
| [LinkedMap](./linked-map) | 有序双向链表映射 | 背包、排行榜、有序列表 |
| [CHashMap](./chashmap) | 一致性哈希并发 Map | 高并发随机读写 |
| [DepartmentTree](./department-tree) | 部门树形结构 | 组织架构、权限管理 |
| [BoolList](./bool-list) | 位图列表 | 标记位、开关集合 |
| [DAG](./dag) | 有向无环图 | 依赖管理、任务编排 |

## 共同约束

1. **事务要求** -- 所有读写操作必须在 `Procedure` 中执行
2. **名称限制** -- 集合名称不能包含 `@` 字符（`@` 为内部保留分隔符）
3. **值类型** -- 值必须继承 `Zeze.Transaction.Bean`
4. **热更新** -- 所有集合均通过 `HotBeanFactory` 接口支持值类型的热重载
