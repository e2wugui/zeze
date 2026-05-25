---
title: "DepartmentTree 部门树"
sidebar:
  order: 4
---

`DepartmentTree` 是 Zeze 提供的**持久化树形结构**，专为组织架构、权限管理等层级场景设计。它支持多泛型参数，允许自定义管理员数据、成员数据、部门成员数据、组数据和部门数据。所有操作在事务中执行，数据自动持久化。

## 包路径

```
Zeze.Collections.DepartmentTree<
    TManager extends Bean,
    TMember extends Bean,
    TDepartmentMember extends Bean,
    TGroupData extends Bean,
    TDepartmentData extends Bean>
```

## 快速开始

```java
Zeze.Application zeze = new Zeze.Application(config);
var linkedMapModule = new LinkedMap.Module(zeze);
var deptModule = new DepartmentTree.Module(zeze, linkedMapModule);

// 打开部门树实例
DepartmentTree<ManagerData, MemberData, DeptMemberData, GroupData, DeptData> tree =
    deptModule.open("corp_org",
        ManagerData.class, MemberData.class,
        DeptMemberData.class, GroupData.class, DeptData.class);
```

## API 参考

### 生命周期

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `create()` | `BDepartmentRoot` | 创建根节点 |
| `destroy()` | `void` | 删除整个部门树 |

### 根节点管理

| 方法 | 说明 |
|------|------|
| `getRoot()` | 获取根节点 |
| `changeRoot(String oldRoot, String newRoot)` | 变更 root 拥有者 |
| `getGroupData(BDepartmentRoot root)` | 获取组级别自定义数据 |

### 部门操作

| 方法 | 说明 |
|------|------|
| `createDepartment(long parentId, String name, int childrenLimit, OutLong outId)` | 创建子部门 |
| `deleteDepartment(long departmentId, boolean recursive)` | 删除部门 |
| `moveDepartment(long departmentId)` | 移动部门到根级 |
| `moveDepartment(long departmentId, long newParentId)` | 移动部门到指定父级 |
| `isRecursiveChild(long departmentId, long child)` | 判断是否为子孙部门 |
| `getDepartmentTreeNode(long departmentId)` | 获取部门节点 |

### 管理员操作

| 方法 | 说明 |
|------|------|
| `getOrAddManager(long departmentId, String name)` | 获取或添加管理员 |
| `deleteManager(long departmentId, String name)` | 删除管理员 |
| `getManagerData(BDepartmentRoot/Node, String account)` | 获取管理员数据 |

### 权限检查

| 方法 | 说明 |
|------|------|
| `checkManagePermission(String account, long departmentId)` | 检查当前部门管理权限 |
| `checkParentManagePermission(String account, long departmentId)` | 检查父部门管理权限 |

权限规则：如果部门设有管理员则仅管理员有权限；如无管理员则递归向上检查父部门，直到根节点（root 账号始终有权限）。

### 成员管理

| 方法 | 说明 |
|------|------|
| `getGroupMembers()` | 获取组级成员 [LinkedMap](./linked-map) |
| `getDepartmentMembers(long departmentId)` | 获取部门级成员 LinkedMap |

## 使用示例

```java
zeze.newProcedure(() -> {
    // 初始化
    var root = tree.create();
    root.setRoot("admin");

    // 创建部门
    OutLong deptId = new OutLong();
    tree.createDepartment(0, "Engineering", 100, deptId);
    long engId = deptId.value;

    // 创建子部门
    OutLong subId = new OutLong();
    tree.createDepartment(engId, "Backend", 50, subId);

    // 添加部门成员
    LinkedMap<DeptMemberData> members = tree.getDepartmentMembers(engId);
    DeptMemberData member = new DeptMemberData();
    member.name = "Alice";
    members.put("alice", member);

    // 移动部门
    tree.moveDepartment(subId, 0); // 移到根级

    // 权限检查
    long result = tree.checkManagePermission("admin", engId);
    if (result == 0) { /* 有权限 */ }

    return 0;
}, "dept_ops").call();
```

## 内部实现

| 存储表 | 键 | 值 | 用途 |
|--------|----|----|------|
| `_tDepartment` | name | BDepartmentRoot | 根节点 + 子部门 ID 映射 |
| `_tDepartmentTree` | name + departmentId | BDepartmentTreeNode | 部门节点（父子关系、管理员、自定义数据） |

DepartmentTree 依赖 [LinkedMap](./linked-map) 管理成员列表，成员数据使用 `"{departmentId}@{name}"` 格式的 LinkedMap 名称存储。

## 错误码

| 错误码 | 常量 | 说明 |
|--------|------|------|
| 1 | `ErrorChangeRootNotOwner` | 非 root 拥有者尝试变更 |
| 2 | `ErrorDepartmentDuplicate` | 部门名称重复 |
| 3 | `ErrorDepartmentNotExist` | 部门不存在 |
| 4 | `ErrorDeleteDepartmentRemainChildren` | 非递归删除仍有子部门 |
| 5 | `ErrorDepartmentSameParent` | 目标父级与当前相同 |
| 6 | `ErrorCanNotMoveToChildren` | 不能移动到自己的子孙部门 |
| 7 | `ErrorDepartmentParentNotExist` | 目标父部门不存在 |
| 8 | `ErrorManagePermission` | 无管理权限 |
| 9 | `ErrorTooManyChildren` | 子部门数量超限 |

## 注意事项

1. **依赖 LinkedMap** -- 初始化时需要传入 `LinkedMap.Module` 实例
2. **ID 自增** -- 部门 ID 从 1 开始自增，由根节点的 `nextDepartmentId` 管理
3. **递归删除** -- `deleteDepartment(id, true)` 会递归删除所有子部门及其成员
4. **名称限制** -- 集合名称不能包含 `@`，不能为空
