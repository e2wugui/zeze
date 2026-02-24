# BoolList 使用文档

## 概述

`BoolList` 是 Zeze 框架提供的**持久化布尔值列表**实现，采用**位压缩**技术存储，支持海量布尔值的高效存储和访问。所有操作都在事务中执行，数据会自动持久化到配置的数据库。

## 包路径
```
Zeze.Collections.BoolList
```

## 核心特性

| 特性 | 说明 |
|------|------|
| 位压缩存储 | 每个布尔值仅占用 1 bit，极大节省存储空间 |
| 持久化 | 数据自动同步到数据库 |
| 事务安全 | 所有操作在事务中执行，乐观锁保证并发安全 |
| 分块存储 | 每 512 个布尔值存储在一个记录中，支持稀疏访问 |
| 随机访问 | 支持任意索引位置的读写操作 |

---

## 快速开始

### 1. 创建 Module

```java
// 在应用启动时创建 Module
Zeze.Application zeze = new Zeze.Application(config);
BoolList.Module boolListModule = new BoolList.Module(zeze);
```

### 2. 打开 BoolList

```java
// 打开一个名为 "playerFlags" 的 BoolList
BoolList flags = boolListModule.open("playerFlags");
```

### 3. 基本操作

```java
// 开启事务
zeze.newProcedure(() -> {
    // 设置索引位置的值为 true
    flags.set(100);      // 将索引 100 设为 true

    // 获取索引位置的值
    boolean value = flags.get(100);  // 返回 true

    // 清除索引位置的值（设为 false）
    flags.clear(100);    // 将索引 100 设为 false

    // 获取未设置的索引
    boolean unset = flags.get(999999);  // 返回 false（默认值）

    return 0;
}, "example").call();

// 清除所有数据（事务外调用）
flags.clearAll();
```

---

## API 参考

### Module 类方法

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `Module(Zeze.Application zeze)` | - | 构造函数，初始化模块并注册表 |
| `open(String name)` | `BoolList` | 打开或创建指定名称的 BoolList |
| `UnRegister()` | `void` | 注销模块，释放资源 |

### BoolList 主要方法

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `get(int index)` | `boolean` | 获取指定索引位置的布尔值 |
| `set(int index)` | `void` | 将指定索引位置设为 true |
| `clear(int index)` | `void` | 将指定索引位置设为 false |
| `clearAll()` | `void` | 清除所有数据（事务外调用） |
| `getModule()` | `Module` | 获取所属模块 |
| `getName()` | `String` | 获取 BoolList 名称 |

---

## 使用示例

### 示例1：玩家成就系统

```java
// 打开玩家的成就列表
BoolList achievements = boolListModule.open("player_12345_achievements");

zeze.newProcedure(() -> {
    // 解锁成就 100
    achievements.set(100);

    // 检查成就是否已解锁
    if (achievements.get(100)) {
        System.out.println("成就 100 已解锁");
    }

    // 重置成就（如需要）
    achievements.clear(100);

    return 0;
}, "achievement_op").call();
```

### 示例2：功能开关/权限位图

```java
// 定义功能常量
public class FeatureFlags {
    public static final int FEATURE_A = 0;
    public static final int FEATURE_B = 1;
    public static final int FEATURE_C = 2;
    public static final int FEATURE_D = 3;
    // ... 最多支持约 21 亿个标志位
}

BoolList userFeatures = boolListModule.open("user_features");

zeze.newProcedure(() -> {
    // 启用功能
    userFeatures.set(FeatureFlags.FEATURE_A);
    userFeatures.set(FeatureFlags.FEATURE_C);

    // 检查功能是否启用
    if (userFeatures.get(FeatureFlags.FEATURE_A)) {
        // 功能 A 已启用
    }

    return 0;
}, "feature_op").call();
```

### 示例3：每日签到记录

```java
// 使用索引表示天数（0-365）
BoolList dailySignIn = boolListModule.open("player_12345_signin_2024");

zeze.newProcedure(() -> {
    int dayOfYear = LocalDate.now().getDayOfYear();

    // 签到
    dailySignIn.set(dayOfYear);

    // 检查今天是否已签到
    if (dailySignIn.get(dayOfYear)) {
        System.out.println("今天已签到");
    }

    // 检查连续签到
    boolean yesterdaySigned = dailySignIn.get(dayOfYear - 1);

    return 0;
}, "signin_op").call();
```

---

## 内部实现

### 位压缩原理

BoolList 使用 `long` 类型（64位）存储布尔值，每个 `long` 可存储 64 个布尔值：

```
每条记录存储结构：
├── item0 (long)  // 位 0-63
├── item1 (long)  // 位 64-127
├── item2 (long)  // 位 128-191
├── item3 (long)  // 位 192-255
├── item4 (long)  // 位 256-319
├── item5 (long)  // 位 320-383
├── item6 (long)  // 位 384-447
└── item7 (long)  // 位 448-511

总计：8 × 64 = 512 个布尔值 / 每条记录
```

### 索引计算

```
给定索引 index：
- 记录ID = index / 512         (确定存储在哪条记录)
- 记录内偏移 = index % 512      (确定在记录内的位置)
- long索引 = 偏移 / 64          (确定使用哪个 long)
- 位偏移 = 偏移 % 64            (确定 long 中的具体位)
```

### 存储表

| 表名 | 键 | 值 | 用途 |
|------|----|----|------|
| `_tBoolList` | name + recordId | BValue | 存储 512 个布尔值的位图 |

---

## 性能特点

### 存储效率

| 指标 | 数值 |
|------|------|
| 每个布尔值存储空间 | 1 bit |
| 每条记录存储数量 | 512 个布尔值 |
| 每条记录存储空间 | 64 字节（8个long） |
| 100万个布尔值占用 | ~125KB |

### 访问性能

- **时间复杂度**：O(1) 随机访问
- **空间局部性**：相邻索引共享同一记录，批量访问效率高
- **稀疏友好**：未设置的索引不占用存储空间（记录按需创建）

---

## 注意事项

1. **事务要求**：`get`、`set`、`clear` 操作必须在 `Procedure` 中执行
2. **clearAll 限制**：`clearAll()` 必须在**事务外**调用
3. **名称限制**：名称不能包含 `@` 字符（保留用于内部），不能为空
4. **索引范围**：索引必须 >= 0，最大支持约 21 亿（int 范围）
5. **默认值**：未设置的索引返回 `false`
6. **模块注册**：使用前必须创建 `BoolList.Module` 并注册到 `Zeze.Application`

---

## 源码位置

`ZezeJava/ZezeJava/src/main/java/Zeze/Collections/BoolList.java`
