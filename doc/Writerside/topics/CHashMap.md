# CHashMap 使用文档

## 概述

`CHashMap` 是 Zeze 框架提供的**并发哈希Map**实现，内部使用多个 `LinkedMap` 分片来提高并发性能。不同分片可以同时被不同事务访问，从而减少锁竞争。所有操作都在事务中执行，数据会自动持久化到配置的数据库。

## 包路径
```
Zeze.Collections.CHashMap<V extends Bean>
```

## 核心特性

| 特性 | 说明 |
|------|------|
| 高并发 | 多分片设计，减少锁竞争 |
| 持久化 | 数据自动同步到数据库 |
| 事务安全 | 所有操作在事务中执行 |
| 快速查找 | O(1) 平均时间复杂度 |
| 哈希分片 | 根据键的哈希值自动分配到分片 |

---

## 快速开始

### 1. 创建 LinkedMap.Module

```java
// CHashMap 依赖 LinkedMap.Module
Zeze.Application zeze = new Zeze.Application(config);
LinkedMap.Module linkedMapModule = new LinkedMap.Module(zeze);
```

### 2. 打开 CHashMap

```java
// 打开一个名为 "playerCache" 的 CHashMap
// 使用默认配置：256个分片，每个分片节点大小30
CHashMap<PlayerData> playerCache = linkedMapModule.openConcurrent("playerCache", PlayerData.class);

// 指定分片数量和节点大小
CHashMap<PlayerData> playerCache = linkedMapModule.openConcurrent("playerCache", PlayerData.class, 128);
```

### 3. 基本操作

```java
// 开启事务
zeze.newProcedure(() -> {
    // 添加元素
    PlayerData player = new PlayerData();
    player.name = "Alice";
    player.level = 10;
    playerCache.put("player_001", player);

    // 获取元素
    PlayerData data = playerCache.get("player_001");

    // 获取或创建
    PlayerData data = playerCache.getOrAdd("player_002");

    // 删除元素
    PlayerData removed = playerCache.remove("player_001");

    // 获取元素数量
    long count = playerCache.size();

    // 判断是否为空
    boolean empty = playerCache.isEmpty();

    return 0;
}, "example").call();
```

---

## API 参考

### Module 类方法（通过 LinkedMap.Module）

| 方法 | 说明 |
|------|------|
| `openConcurrent(String name, Class<T> valueClass)` | 打开 CHashMap，默认256分片，节点大小30 |
| `openConcurrent(String name, Class<T> valueClass, int nodeSize)` | 打开 CHashMap，默认256分片，指定节点大小 |

### CHashMap 主要方法

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `get(String key)` | `V` | 根据 key 获取值，不存在返回 null |
| `getOrAdd(String key)` | `V` | 获取或创建值 |
| `put(String key, V value)` | `V` | 添加键值对，返回旧值 |
| `remove(String key)` | `V` | 删除元素，返回被删除的值 |
| `size()` | `long` | 获取元素总数 |
| `isEmpty()` | `boolean` | 判断是否为空 |
| `getName()` | `String` | 获取 CHashMap 名称 |

---

## 使用示例

### 示例1：玩家数据缓存

```java
// 定义玩家数据Bean
public class PlayerData extends Bean {
    public String name;
    public int level;
    public long exp;
    // ...
}

// 使用 CHashMap 缓存玩家数据
CHashMap<PlayerData> playerCache = linkedMapModule.openConcurrent("players", PlayerData.class);

// 更新玩家数据
zeze.newProcedure(() -> {
    PlayerData player = playerCache.getOrAdd("player_12345");
    player.name = "Bob";
    player.level = 20;
    player.exp = 15000;
    return 0;
}, "update_player").call();

// 读取玩家数据
zeze.newProcedure(() -> {
    PlayerData player = playerCache.get("player_12345");
    if (player != null) {
        System.out.println("Player: " + player.name + ", Level: " + player.level);
    }
    return 0;
}, "read_player").call();
```

### 示例2：会话管理

```java
// 定义会话Bean
public class Session extends Bean {
    public long userId;
    public long loginTime;
    public String deviceInfo;
}

// 使用 CHashMap 管理会话
CHashMap<Session> sessions = linkedMapModule.openConcurrent("sessions", Session.class);

// 创建会话
zeze.newProcedure(() -> {
    Session session = new Session();
    session.userId = 1001;
    session.loginTime = System.currentTimeMillis();
    session.deviceInfo = "Android";
    sessions.put("session_" + session.userId, session);
    return 0;
}, "create_session").call();

// 删除会话（登出）
zeze.newProcedure(() -> {
    sessions.remove("session_1001");
    return 0;
}, "logout").call();
```

### 示例3：商品库存

```java
// 定义商品库存Bean
public class ProductStock extends Bean {
    public int productId;
    public int count;
    public long updateTime;
}

// 使用 CHashMap 管理库存
CHashMap<ProductStock> stockMap = linkedMapModule.openConcurrent("product_stock", ProductStock.class);

// 扣减库存
zeze.newProcedure(() -> {
    ProductStock stock = stockMap.get("product_001");
    if (stock != null && stock.count >= 10) {
        stock.count -= 10;
        stock.updateTime = System.currentTimeMillis();
        return 0; // 成功
    }
    return -1; // 库存不足
}, "deduct_stock").call();
```

### 示例4：多线程并发访问

```java
// CHashMap 的分片设计允许多个线程同时访问不同分片
// 下面的操作可以并发执行（假设访问不同的 key）

// 线程1：操作 player_aaa
zeze.newProcedure(() -> {
    playerCache.put("player_aaa", new PlayerData());
    return 0;
}, "thread1").call();

// 线程2：操作 player_bbb（与 player_aaa 可能在不同分片）
zeze.newProcedure(() -> {
    playerCache.put("player_bbb", new PlayerData());
    return 0;
}, "thread2").call();
```

---

## 内部实现

### 分片结构

```
CHashMap
├── buckets[0] → LinkedMap (name@0)
├── buckets[1] → LinkedMap (name@1)
├── buckets[2] → LinkedMap (name@2)
│   ...
└── buckets[n-1] → LinkedMap (name@n-1)
```

### 哈希分片算法

```java
// 计算 key 应该放入哪个分片
int index = Integer.remainderUnsigned(ByteBuffer.calc_hashnr(key), buckets.length);
```

### 并发优势

```
传统单锁Map：
Transaction1 ──锁──> [    Map    ] <──锁── Transaction2
                     (串行等待)

CHashMap分片：
Transaction1 ──> [Bucket 0]
Transaction2 ──> [Bucket 1]  (并行执行)
Transaction3 ──> [Bucket 2]
```

### Size 统计

- 每个 CHashMap 维护一个 `sizes[]` 数组
- 每个分片的大小缓存在对应位置
- 事务提交后通过 `Transaction.whileCommit` 更新
- `size()` 方法通过累加所有分片大小得到总数，避免锁住所有桶

---

## CHashMap vs LinkedMap 对比

| 特性 | CHashMap | LinkedMap |
|------|----------|-----------|
| 并发性能 | 高（多分片） | 一般（单分片） |
| 元素顺序 | 无序 | 有序（插入顺序） |
| 顺序操作 | 不支持 | 支持 moveAhead/moveTail |
| 遍历 | 不支持 | 支持 walk |
| 适用场景 | 高并发缓存、会话管理 | 需要顺序的场景、背包 |

---

## 与 Java ConcurrentHashMap 的区别

| 特性 | CHashMap | ConcurrentHashMap |
|------|----------|-------------------|
| 持久化 | 自动持久化到数据库 | 仅内存 |
| 事务 | 支持事务 | 不支持 |
| 值类型 | 必须是 Bean | 任意对象 |
| 分布式 | 支持多进程共享 | 单进程 |
| 性能 | 相对较低（事务开销） | 高 |

---

## 配置建议

### 分片数量选择

| 场景 | 建议分片数 | 说明 |
|------|-----------|------|
| 低并发（<10事务/秒） | 32-64 | 减少资源占用 |
| 中等并发（10-100事务/秒） | 128 | 平衡性能和资源 |
| 高并发（>100事务/秒） | 256+ | 最大化并发性能 |

### 节点大小选择

| 场景 | 建议节点大小 | 说明 |
|------|-------------|------|
| 频繁增删 | 10-20 | 减少节点内元素移动 |
| 稳定数据 | 30-50 | 减少节点数量 |
| 大数据量 | 50-100 | 减少存储开销 |

---

## 注意事项

1. **事务要求**：所有操作必须在 `Procedure` 中执行
2. **无序性**：CHashMap 不保证元素顺序，不提供遍历功能
3. **名称限制**：名称不能包含 `@` 字符（保留用于分片命名）
4. **值类型**：值类型必须继承自 `Bean`
5. **分片数量**：分片数量在创建时确定，之后不可修改
6. **Size 精度**：`size()` 返回的是近似值（基于缓存），不是实时精确值

---

## 源码位置

`ZezeJava/ZezeJava/src/main/java/Zeze/Collections/CHashMap.java`
