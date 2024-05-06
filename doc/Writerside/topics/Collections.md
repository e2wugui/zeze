# Collections

Collections是Zeze的内建模块。

## LinkedMap
一个容量巨大的Map。一个有顺序的Map，注意，不是排序的Map。
所有数据操作支持事务。遍历操作是事务外的。
```
初始化
MyApp.LinkedMapModule = new LinkedMap.Module(zeze);
使用例子
Var friends = LinkedMapModule.open(“Friends”, BFreind.clsss);
```
| 方法           | 说明                 |
|--------------|--------------------|
| getRoot      | 得到LinkedMap的根节点    |
| getNode      | 查询数据节点             |
| getFristNode | 查询第一个数据节点          |
| isEmpty      | 是否空                |
| size         | 项数量                |
| moveAhead    | 把指定项移到列表开头         |
| moveTail     | 把指定项移到列表尾巴         |
| getOrAdd     | 根据key查询项，没有就创建一个   |
| put          | 插入或者替换一个key-value项 |
| get          | 查询项                |
| remove       | 移除项                |
| removeNode   | 移除节点               |
| clear        | 清楚整个容器             |
| walk         | 遍历容器               |

## Queue
一个容量巨大的单向链表队列。所有数据操作支持事务。遍历操作是事务外的。
不需要初始化，通过Zeze.Application.Queues创建和大概队列。

| 方法       | 说明                     |
|----------|------------------------|
| isEmpty  | 队列是否为空                 |
| pollNode | 删除并返回整个第一个节点，不存在返回null |
| peekNode | 返回第一个节点，不存在返回null      |
| clear    | 清除队列                   |
| poll     | 从队头提取项，没有则返回null       |
| peek     | 查询队头，队列为空返回null        |
| size     | 项的数量                   |
| add      | 在队尾添加项                 |
| push     | Stack                  |
| pop      | Stack                  |
| walk     | 事务外遍历整个队列              |

## DepartmentTree
这是一棵树。
```
初始化
MyApp.DepartmentTree = new DepartmentTree.Module(zeze, LinkedMapModule);
```

| 方法                       | 说明                 |
|--------------------------|--------------------|
| getRoot                  | 返回管理树的根节点          | 
| getDepartmentTreeNode    | 返回部门节点             |
| getDepartmentMembers     | 返回部门成员的LinkedMap   |
| getGroupMembers          | 返回树的所有成员的LinkedMap |
| selectRoot               | 事务外，返回树的根管理节点      |
| selectDepartmentTreeNode | 事务外，返回部门节点         |
| create                   | 创建树                |
| changeRoot               | 改变树的总管理员           |
| getOrAddRootManager      | 树的根节点管理员           |
| getOrAddManager          | 部门几点的管理员           |
| createDepartment         | 创建部门               |
| deleteDepartment         | 删除部门               |
| isRecursiveChild         | 是否某个部门的子（孙）部门      |
| moveDepartment           | 移动部门               |

## CsQueue
Concurrent Server Queue。每个server拥有自己私有的队列，只能操作自己的队列。server
宕机的时候，其他server会接管它的队列数据，保证数据最终能得到处理。它的接口实际
上和Queue一样，内部也是用Queue实现的。

| 方法       | 说明                     |
|----------|------------------------|
| isEmpty  | 队列是否为空                 |
| pollNode | 删除并返回整个第一个节点，不存在返回null |
| peekNode | 返回第一个节点，不存在返回null      |
| clear    | 清除队列                   |
| poll     | 从队头提取项，没有则返回null       |
| peek     | 查询队头，队列为空返回null        |
| size     | 项的数量                   |
| add      | 在队尾添加项                 |
| push     | Stack                  |
| pop      | Stack                  |
| walk     | 事务外遍历整个队列              |

## CHashMap
具有多个桶的大容量持久化HashMap实现。C=Concurrent。
主要数据结构：LinkedMap<V>[] buckets；
```
Hash选择桶：var index = ByteBuffer.calc_hashnr(key) % buckets.length;
var bucket = buckets[index];
```

| 方法       | 说明         |
|----------|------------|
| get      | 根据key查询映射项 |
| getOrAdd | 查询或创建      |
| put      | 加入映射项，覆盖方式 |
| remove   | 删除映射项      |
| size     | Map.size   |
| isEmpty  | Map是否为空    |

