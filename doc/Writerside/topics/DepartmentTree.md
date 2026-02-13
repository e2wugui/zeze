# DepartmentTree 使用文档

## 概述

`DepartmentTree` 是 Zeze 框架提供的**持久化部门树**实现，用于构建组织架构、部门层级等树形结构。支持管理员权限管理、部门成员管理、部门层级移动等功能。所有操作都在事务中执行，数据会自动持久化到配置的数据库。

## 包路径
```
Zeze.Collections.DepartmentTree<
    TManager extends Bean,
    TMember extends Bean,
    TDepartmentMember extends Bean,
    TGroupData extends Bean,
    TDepartmentData extends Bean>
```

## 核心特性

| 特性 | 说明 |
|------|------|
| 树形结构 | 支持多级部门层级 |
| 权限管理 | 支持管理员角色和递归权限检查 |
| 成员管理 | 每个部门可管理自己的成员列表 |
| 持久化 | 数据自动同步到数据库 |
| 事务安全 | 所有操作在事务中执行 |
| 热更新支持 | 支持所有 Bean 类型的动态重载 |
| 自定义数据 | 5种泛型参数支持自定义数据结构 |

---

## 泛型参数说明

| 参数 | 说明 |
|------|------|
| `TManager` | 管理员数据类型，存储管理员额外信息 |
| `TMember` | 组成员数据类型，根级别的成员 |
| `TDepartmentMember` | 部门成员数据类型，各部门的成员 |
| `TGroupData` | 组数据类型，根节点的自定义数据 |
| `TDepartmentData` | 部门数据类型，各部门的自定义数据 |

---

## 快速开始

### 1. 创建 Module

```java
// 在应用启动时创建 Module
Zeze.Application zeze = new Zeze.Application(config);
LinkedMap.Module linkedMapModule = new LinkedMap.Module(zeze);
DepartmentTree.Module deptTreeModule = new DepartmentTree.Module(zeze, linkedMapModule);
```

### 2. 定义 Bean 类型

```java
// 管理员数据
public class ManagerData extends Bean {
    public String title;      // 职位
    public long assignedTime; // 任命时间
}

// 组成员数据
public class MemberData extends Bean {
    public String nickname;
    public long joinTime;
}

// 部门成员数据
public class DepartmentMemberData extends Bean {
    public String position;   // 部门内职位
    public long joinTime;
}

// 组数据
public class GroupData extends Bean {
    public String groupName;
    public String description;
}

// 部门数据
public class DepartmentData extends Bean {
    public String location;   // 部门位置
    public String description;
}
```

### 3. 打开 DepartmentTree

```java
DepartmentTree<ManagerData, MemberData, DepartmentMemberData, GroupData, DepartmentData> orgTree =
    deptTreeModule.open(
        "company_org",
        ManagerData.class,
        MemberData.class,
        DepartmentMemberData.class,
        GroupData.class,
        DepartmentData.class
    );
```

### 4. 基本操作

```java
zeze.newProcedure(() -> {
    // 创建组织
    BDepartmentRoot root = orgTree.create();
    root.setRoot("admin_account");  // 设置超级管理员

    // 设置组数据
    GroupData groupData = orgTree.getGroupData(root);
    groupData.groupName = "My Company";
    groupData.description = "A great company";

    // 创建部门
    OutLong deptId = new OutLong();
    long result = orgTree.createDepartment(0, "技术部", 100, deptId);

    // 添加部门管理员
    ManagerData manager = orgTree.getOrAddManager(deptId.value, "manager_account");
    manager.title = "技术总监";

    // 添加部门成员
    LinkedMap<DepartmentMemberData> members = orgTree.getDepartmentMembers(deptId.value);
    DepartmentMemberData member = members.getOrAdd("employee_001");
    member.position = "高级工程师";

    return 0;
}, "init_org").call();
```

---

## API 参考

### Module 类方法

| 方法 | 说明 |
|------|------|
| `open(name, managerClass, memberClass, deptMemberClass, groupDataClass, deptDataClass)` | 打开 DepartmentTree |

### DepartmentTree 主要方法

#### 根节点操作

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `create()` | `BDepartmentRoot` | 创建组织根节点 |
| `getRoot()` | `BDepartmentRoot` | 获取根节点 |
| `selectRoot()` | `BDepartmentRoot` | 获取根节点（只读） |
| `destroy()` | `void` | 销毁整个组织 |
| `changeRoot(oldRoot, newRoot)` | `long` | 更换超级管理员 |
| `getGroupData(root)` | `TGroupData` | 获取组数据 |
| `getGroupMembers()` | `LinkedMap<TMember>` | 获取组成员列表 |

#### 部门操作

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `createDepartment(parentId, name, childrenLimit, outId)` | `long` | 创建部门 |
| `deleteDepartment(departmentId, recursive)` | `long` | 删除部门 |
| `moveDepartment(departmentId)` | `long` | 移动部门到根级别 |
| `moveDepartment(departmentId, parentId)` | `long` | 移动部门到指定父部门 |
| `getDepartmentTreeNode(departmentId)` | `BDepartmentTreeNode` | 获取部门节点 |
| `selectDepartmentTreeNode(departmentId)` | `BDepartmentTreeNode` | 获取部门节点（只读） |
| `getDepartmentData(department)` | `TDepartmentData` | 获取部门数据 |
| `getDepartmentMembers(departmentId)` | `LinkedMap<TDepartmentMember>` | 获取部门成员列表 |
| `isRecursiveChild(departmentId, child)` | `boolean` | 检查是否为递归子部门 |

#### 管理员操作

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `getOrAddManager(departmentId, name)` | `TManager` | 获取或添加管理员 |
| `getManagerData(root/dept, account)` | `TManager` | 获取管理员数据 |
| `deleteManager(departmentId, name)` | `TManager` | 删除管理员 |

#### 权限检查

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `checkManagePermission(account, departmentId)` | `long` | 检查管理权限 |
| `checkParentManagePermission(account, departmentId)` | `long` | 检查父部门管理权限 |

---

## 权限规则详解

### 管理权限检查规则

```
1. 如果部门拥有管理员：
   - 仅管理员账户拥有该部门的管理权限

2. 如果部门没有管理员：
   - 递归检查父部门的管理员设置
   - 直到找到有管理员的部门或到达根节点

3. 根节点（Root）：
   - root 账户总是拥有权限
   - 根节点的管理员也拥有权限
```

### 权限检查示例

```java
// 检查用户是否有部门管理权限
long result = orgTree.checkManagePermission("user_account", departmentId);
if (result == 0) {
    // 有权限
} else {
    // 无权限
}

// 检查用户是否有父部门管理权限（用于管理下级部门）
long result = orgTree.checkParentManagePermission("user_account", departmentId);
```

---

## 错误码

| 错误码 | 说明 |
|--------|------|
| `ErrorManagePermission` | 无管理权限 |
| `ErrorDepartmentNotExist` | 部门不存在 |
| `ErrorChangeRootNotOwner` | 非所有者无法更换Root |
| `ErrorTooManyChildren` | 子部门数量超过限制 |
| `ErrorDepartmentDuplicate` | 部门名称重复 |
| `ErrorDeleteDepartmentRemainChildren` | 删除部门时存在子部门 |
| `ErrorDepartmentSameParent` | 目标父部门与当前相同 |
| `ErrorDepartmentParentNotExist` | 目标父部门不存在 |
| `ErrorCanNotMoveToChildren` | 不能移动到自己的子部门 |

---

## 使用示例

### 示例1：创建组织架构

```java
zeze.newProcedure(() -> {
    // 创建公司
    BDepartmentRoot root = orgTree.create();
    root.setRoot("ceo_account");

    // 创建一级部门
    OutLong techDeptId = new OutLong();
    orgTree.createDepartment(0, "技术部", 10, techDeptId);

    OutLong hrDeptId = new OutLong();
    orgTree.createDepartment(0, "人力资源部", 10, hrDeptId);

    // 创建二级部门
    OutLong backendTeamId = new OutLong();
    orgTree.createDepartment(techDeptId.value, "后端组", 20, backendTeamId);

    OutLong frontendTeamId = new OutLong();
    orgTree.createDepartment(techDeptId.value, "前端组", 20, frontendTeamId);

    return 0;
}, "create_org").call();
```

### 示例2：部门成员管理

```java
zeze.newProcedure(() -> {
    // 获取部门成员列表
    LinkedMap<DepartmentMemberData> members = orgTree.getDepartmentMembers(departmentId);

    // 添加成员
    DepartmentMemberData member = members.getOrAdd("employee_001");
    member.position = "高级工程师";
    member.joinTime = System.currentTimeMillis();

    // 移动成员到队首
    members.moveAhead("employee_001");

    // 移除成员
    members.remove("employee_002");

    // 遍历部门成员
    members.walk((id, data) -> {
        System.out.println("Employee: " + id + ", Position: " + data.position);
        return true;
    });

    return 0;
}, "manage_members").call();
```

### 示例3：部门移动

```java
zeze.newProcedure(() -> {
    // 将部门移动到根级别
    long result = orgTree.moveDepartment(departmentId);

    // 将部门移动到另一个父部门下
    result = orgTree.moveDepartment(departmentId, newParentId);

    // 检查是否可以移动（不能移动到自己的子部门）
    if (orgTree.isRecursiveChild(departmentId, targetParentId)) {
        // 不能移动
    }

    return 0;
}, "move_dept").call();
```

### 示例4：删除部门

```java
zeze.newProcedure(() -> {
    // 删除部门（必须没有子部门）
    long result = orgTree.deleteDepartment(departmentId, false);

    // 递归删除部门及其所有子部门
    result = orgTree.deleteDepartment(departmentId, true);

    return 0;
}, "delete_dept").call();
```

### 示例5：管理员管理

```java
zeze.newProcedure(() -> {
    // 添加部门管理员
    ManagerData manager = orgTree.getOrAddManager(departmentId, "manager_account");
    manager.title = "部门经理";
    manager.assignedTime = System.currentTimeMillis();

    // 获取管理员数据
    ManagerData data = orgTree.getManagerData(
        orgTree.getDepartmentTreeNode(departmentId),
        "manager_account"
    );

    // 删除管理员
    ManagerData removed = orgTree.deleteManager(departmentId, "manager_account");

    return 0;
}, "manage_manager").call();
```

---

## 内部实现

### 数据结构

```
BDepartmentRoot (根节点)
├── root              // 超级管理员账户
├── managers          // 管理员Map<account, DynamicBean>
├── data              // 组数据 (TGroupData)
├── children          // 子部门Map<name, departmentId>
└── nextDepartmentId  // 下一个部门ID

BDepartmentTreeNode (部门节点)
├── name              // 部门名称
├── parentDepartment  // 父部门ID (0表示根级别)
├── managers          // 管理员Map<account, DynamicBean>
├── data              // 部门数据 (TDepartmentData)
└── children          // 子部门Map<name, departmentId>
```

### 存储表

| 表名 | 键 | 值 | 用途 |
|------|----|----|------|
| `_tDepartment` | name | BDepartmentRoot | 存储根节点 |
| `_tDepartmentTree` | name + departmentId | BDepartmentTreeNode | 存储部门节点 |
| `_tLinkedMapNodes` | deptId@name + nodeId | LinkedMapNode | 存储部门成员 |

### 树形结构示意

```
BDepartmentRoot (root)
├── children: {"技术部": 1, "人力资源部": 2}
│
├── [1] 技术部
│   ├── parentDepartment: 0
│   ├── children: {"后端组": 3, "前端组": 4}
│   │
│   ├── [3] 后端组
│   │   └── parentDepartment: 1
│   │
│   └── [4] 前端组
│       └── parentDepartment: 1
│
└── [2] 人力资源部
    └── parentDepartment: 0
```

---

## 注意事项

1. **事务要求**：所有操作必须在 `Procedure` 中执行
2. **名称限制**：名称不能包含 `@` 字符（保留用于内部）
3. **依赖 LinkedMap**：需要先创建 `LinkedMap.Module`
4. **部门 ID 为 0**：表示根级别，不能通过 `getDepartmentTreeNode(0)` 访问
5. **删除部门**：非递归删除时，部门必须没有子部门
6. **移动部门**：不能将部门移动到自己的子部门下
7. **权限继承**：没有管理员的部门会继承父部门的权限设置

---

## 源码位置

`ZezeJava/ZezeJava/src/main/java/Zeze/Collections/DepartmentTree.java`
