---
title: "DepartmentTree 部门树"
description: "Zeze.Collections.DepartmentTree 树形结构的初始化、依赖与适用场景"
category: reference
order: 44
---

> 本文档说明 `Zeze.Collections.DepartmentTree` 树形结构的定位、对 LinkedMap 的依赖、初始化与适用场景。

## 模块定位

`Zeze.Collections.DepartmentTree` 是持久化的**部门树形结构**，适用于组织架构、权限管理等层级数据。

| 特性 | 说明 |
|------|------|
| **树形结构** | 节点具有父子层级关系 |
| **增删查** | 提供节点的增加、删除、查询能力 |
| **依赖 LinkedMap** | 内部基于 LinkedMap 维护节点列表，需复用一个已注册的 LinkedMap Module |
| **事务安全** | 基于内部 Table，享受乐观锁与自动持久化 |

## 适用场景

- 组织架构（公司 → 部门 → 小组 → 成员）
- 权限管理（角色继承、权限树）
- 任意层级关系数据的持久化存储

## 初始化

`DepartmentTree` 构造时**必须传入一个已注册的 LinkedMap Module**，它复用该 Module 来维护子节点列表等有序数据。`open` 一共需要**5 个 Bean 类型 + 5 个对应的 Class**——部门树由 5 类数据组成：管理员、成员、部门成员、组数据、部门数据。

```java
// 1. 先注册依赖的 LinkedMap Module
var linkedMapModule = new LinkedMap.Module(zeze);

// 2. 注册 DepartmentTree Module，传入 linkedMapModule
var departmentTreeModule = new DepartmentTree.Module(zeze, linkedMapModule);

// 3. 打用具名实例：5 个泛型 + 5 个 Class（名称不能包含 @）
DepartmentTree<TManager, TMember, TDepartmentMember, TGroupData, TDepartmentData> tree =
    departmentTreeModule.open(
        "orgTree",
        TManager.class,          // 部门管理员
        TMember.class,           // 部门成员
        TDepartmentMember.class, // 部门成员（带部门归属信息）
        TGroupData.class,        // 组数据
        TDepartmentData.class);  // 部门数据
```

> ⚠️ **注意**：`open` 不是单参数。漏传任何 `Class` 都会编译失败。5 个值类型都必须继承 `Zeze.Transaction.Bean` 并经 `BeanFactory` 注册。

## 共同约束

| 约束 | 说明 |
|------|------|
| **必须在 Procedure 中操作** | 所有读写必须在事务内 |
| **名称不能含 `@`** | `@` 为内部保留分隔符 |
| **值必须继承 Bean** | 并经 `BeanFactory` 注册 |
| **支持值类型热重载** | 通过 `HotBeanFactory` 接口 |

## 相关文档

- 持久化集合总览：[./coll-overview.md](./coll-overview.md)
- LinkedMap 有序映射：[./coll-linked-map.md](./coll-linked-map.md)
