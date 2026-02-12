# LinkedMap 使用文档

## 概述

`LinkedMap` 是 Zeze 框架提供的**持久化有序Map**实现，结合了 **HashMap 的快速查找** 和 **双向链表的顺序访问** 特性。所有操作都在事务中执行，数据会自动持久化到配置的数据库。

## 包路径
```
Zeze.Collections.LinkedMap<V extends Bean>
```

## 核心特性

| 特性 | 说明 |
|------|------|
| 有序性 | 元素按插入顺序排列，支持移动到头部/尾部 |
| 持久化 | 数据自动同步到数据库 |
| 事务安全 | 所有操作在事务中执行，乐观锁保证并发安全 |
| 热更新支持 | 支持值类型的动态重载 |
| 节点分块 | 大数据量时分多个节点存储，提高性能 |

---

## 快速开始

### 1. 创建 Module

```java
// 在应用启动时创建 Module
Zeze.Application zeze = new Zeze.Application(config);
LinkedMap.Module linkedMapModule = new LinkedMap.Module(zeze);
```

### 2. 打开 LinkedMap

```java
// 打开一个名为 "playerItems" 的 LinkedMap，值类型为 Item
LinkedMap<Item> itemMap = linkedMapModule.open("playerItems", Item.class);

// 指定节点大小（每个节点存储的元素数量，默认30）
LinkedMap<Item> itemMap = linkedMapModule.open("playerItems", Item.class, 50);
```

### 3. 基本操作

```java
// 开启事务
zeze.newProcedure(() -> {
    // 添加元素（默认添加到头部）
    itemMap.put("item1", new Item());

    // 添加到尾部
    itemMap.put("item2", new Item(), false);

    // 获取元素
    Item item = itemMap.get("item1");

    // 获取或创建
    Item item = itemMap.getOrAdd("item3");

    // 删除元素
    Item removed = itemMap.remove("item1");

    // 获取元素数量
    long count = itemMap.size();

    // 判断是否为空
    boolean empty = itemMap.isEmpty();

    return 0;
}, "example").call();
```

---

## API 参考

### Module 类方法

| 方法 | 说明 |
|------|------|
| `open(String name, Class<T> valueClass)` | 打开 LinkedMap，默认节点大小30 |
| `open(String name, Class<T> valueClass, int nodeSize)` | 打开 LinkedMap，指定节点大小 |
| `openConcurrent(String name, Class<T> valueClass)` | 打开并发版本 CHashMap |

### LinkedMap 主要方法

#### Map 操作

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `get(String id)` | `V` | 根据ID获取值 |
| `get(long id)` | `V` | 根据long类型ID获取值 |
| `put(String id, V value)` | `V` | 插入键值对（添加到头部），返回旧值 |
| `put(String id, V value, boolean ahead)` | `V` | 插入键值对，ahead=true添加到头部，false添加到尾部 |
| `getOrAdd(String id)` | `V` | 获取或创建值 |
| `remove(String id)` | `V` | 删除元素，返回被删除的值 |

#### 链表操作

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `moveAhead(String id)` | `long` | 将元素移动到链表头部 |
| `moveTail(String id)` | `long` | 将元素移动到链表尾部 |

#### 查询操作

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `size()` | `long` | 获取元素总数 |
| `isEmpty()` | `boolean` | 判断是否为空 |
| `getName()` | `String` | 获取 LinkedMap 名称 |
| `getRoot()` | `BLinkedMap` | 获取根节点信息 |
| `getNode(long nodeId)` | `BLinkedMapNode` | 根据节点ID获取节点 |
| `getFirstNode(OutLong nodeId)` | `BLinkedMapNode` | 获取第一个节点 |
| `getNodeById(String id)` | `BLinkedMapNode` | 根据元素ID获取所在节点 |
| `getNodeId(String id)` | `Long` | 获取元素所在的节点ID |

#### 遍历与清理

| 方法 | 说明 |
|------|------|
| `walk(TableWalkHandle<String, V> func)` | 遍历所有元素，返回处理的元素数量 |
| `clear()` | 清空所有元素（异步删除） |
| `removeNode(long nodeId)` | 删除整个节点及其所有元素 |

---

## 使用示例

### 示例1：玩家背包

```java
// 定义物品Bean
public class Item extends Bean {
    public int itemId;
    public int count;
    // ...
}

// 使用
LinkedMap<Item> bag = linkedMapModule.open("player_bag_123", Item.class);

zeze.newProcedure(() -> {
    // 添加物品到背包头部
    Item sword = new Item();
    sword.itemId = 1001;
    sword.count = 1;
    bag.put("slot_1", sword);

    // 获取物品
    Item item = bag.get("slot_1");

    // 移动物品到背包末尾
    bag.moveTail("slot_1");

    return 0;
}, "bag_op").call();
```

### 示例2：遍历所有元素

```java
long processed = linkedMap.walk((id, value) -> {
    System.out.println("ID: " + id + ", Value: " + value);
    return true; // 返回true继续遍历，false停止
});
```

### 示例3：使用 CHashMap（高并发场景）

```java
// CHashMap 内部使用多个 LinkedMap 分片，提高并发性能
CHashMap<Item> concurrentMap = linkedMapModule.openConcurrent("shared_items", Item.class);

zeze.newProcedure(() -> {
    concurrentMap.put("item_001", new Item());
    return 0;
}, "concurrent_op").call();
```

---

## 内部实现

### 数据结构

```
BLinkedMap (根节点)
├── headNodeId  // 头节点ID
├── tailNodeId  // 尾节点ID
├── lastNodeId  // 最后分配的节点ID
└── count       // 元素总数

BLinkedMapNode (数据节点) - 双向链表
├── prevNodeId  // 前驱节点
├── nextNodeId  // 后继节点
└── values[]    // 元素列表（每个节点最多 nodeSize 个元素）

BLinkedMapKey (索引) - 快速定位
└── name + id → nodeId
```

### 存储表

| 表名 | 键 | 值 | 用途 |
|------|----|----|------|
| `_tLinkedMaps` | name | BLinkedMap | 存储根节点 |
| `_tLinkedMapNodes` | name + nodeId | BLinkedMapNode | 存储数据节点 |
| `_tValueIdToNodeId` | name + id | nodeId | 值ID到节点ID的映射 |

---

## 注意事项

1. **事务要求**：所有操作必须在 `Procedure` 中执行
2. **名称限制**：名称不能包含 `@` 字符（保留用于内部）
3. **值类型**：值类型必须继承自 `Bean`
4. **clear 操作**：`clear()` 使用延迟删除，不会立即删除所有节点
5. **节点大小**：`nodeSize` 只影响新创建的节点，已有节点大小不变

---

## 源码位置

`ZezeJava/ZezeJava/src/main/java/Zeze/Collections/LinkedMap.java`
