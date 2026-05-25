---
title: "LinkedMap 有序映射"
sidebar:
  order: 3
---

`LinkedMap` 是 Zeze 提供的**持久化有序映射**，结合了 Map 的随机访问能力和链表的顺序特性。它维护元素的双向链表顺序，支持按 key 查找、插入到头部或尾部、以及移动元素位置。所有操作在事务中执行，数据自动持久化。

## 包路径

```
Zeze.Collections.LinkedMap<V extends Bean>
```

## 快速开始

```java
Zeze.Application zeze = new Zeze.Application(config);
LinkedMap.Module lmModule = new LinkedMap.Module(zeze);

// 打开 LinkedMap，默认节点大小 30
LinkedMap<Item> backpack = lmModule.open("player_backpack", Item.class);

// 指定节点大小
LinkedMap<Item> backpack = lmModule.open("player_backpack", Item.class, 50);
```

## API 参考

### Module 方法

| 方法 | 说明 |
|------|------|
| `open(String name, Class<T> valueClass)` | 打开 LinkedMap，默认节点大小 30 |
| `open(String name, Class<T> valueClass, int nodeSize)` | 指定节点大小 |
| `openConcurrent(String name, Class<T> valueClass)` | 打开 [CHashMap](./chashmap)（基于 LinkedMap 分桶） |

### 基本操作

以下操作必须在 `Procedure` 内调用。

#### Map 操作

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `get(String id)` | `V / null` | 根据 key 获取值 |
| `get(long id)` | `V / null` | 以 long 为 key 获取值 |
| `put(String id, V value)` | `V / null` | 插入或更新，默认插入到头部 |
| `put(String id, V value, boolean ahead)` | `V / null` | `ahead=true` 插入头部，`false` 插入尾部 |
| `getOrAdd(String id)` | `V` | 获取或创建 |
| `remove(String id)` | `V / null` | 根据 key 删除并返回 |
| `getNodeId(String id)` | `Long / null` | 获取 key 所在节点 ID |
| `size()` | `long` | 元素总数 |
| `isEmpty()` | `boolean` | 是否为空 |

#### 顺序操作

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `moveAhead(String id)` | `long` | 将元素移动到链表头部 |
| `moveTail(String id)` | `long` | 将元素移动到链表尾部 |

### 节点与遍历

| 方法 | 说明 |
|------|------|
| `getRoot()` | 获取根节点 |
| `getNode(long nodeId)` | 根据 ID 获取节点 |
| `getFirstNode(OutLong nodeId)` | 获取第一个节点 |
| `walk(TableWalkHandle<String, V> func)` | 从头到尾遍历所有元素 |

### 清理

| 方法 | 说明 |
|------|------|
| `clear()` | 异步清空所有数据（使用 **DelayRemove** 延迟删除） |

`clear()` 不会立即删除所有节点，而是重置根节点的 head/tail 指针，然后通过 `DelayRemove` 组件在后台逐步清理，避免大事务阻塞。

## 使用示例

### 背包系统

```java
LinkedMap<Item> backpack = lmModule.open("backpack_" + playerId, Item.class);

zeze.newProcedure(() -> {
    // 添加物品
    Item sword = new Item();
    sword.name = "Iron Sword";
    backpack.put("sword_001", sword);

    // 获取物品
    Item item = backpack.get("sword_001");

    // 将物品移到最前面
    backpack.moveAhead("sword_001");

    // 移除物品
    backpack.remove("sword_001");

    return 0;
}, "backpack_ops").call();
```

### 遍历

```java
long count = backpack.walk((id, value) -> {
    System.out.println("id=" + id + ", value=" + value);
    return true; // true 继续，false 停止
});
```

## 内部实现

LinkedMap 使用双向链表组织节点，每个节点存储最多 `nodeSize` 个值。额外维护 `_tValueIdToNodeId` 表实现 O(1) 的 key 到 node 的映射。

| 存储表 | 键 | 值 | 用途 |
|--------|----|----|------|
| `_tLinkedMaps` | name | BLinkedMap | 根节点（head/tail/count） |
| `_tLinkedMapNodes` | name + nodeId | BLinkedMapNode | 双向链表节点 |
| `_tValueIdToNodeId` | name + valueId | BLinkedMapNodeId | key -> nodeId 索引 |

## 注意事项

1. **事务要求** -- 所有读写操作必须在 `Procedure` 中执行（`walk` 除外，使用 `selectDirty` 快照读取）
2. **名称限制** -- 名称不能包含 `@` 字符
3. **节点大小** -- `nodeSize` 只影响新节点，已有节点不受影响
4. **清理性能** -- `clear()` 使用延迟删除，适合大数据量场景
