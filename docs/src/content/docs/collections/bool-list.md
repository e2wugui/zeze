---
title: "BoolList 位图列表"
sidebar:
  order: 6
---

`BoolList` 是 Zeze 提供的**持久化位图**集合，使用位运算高效存储大量布尔值。每个逻辑记录包含 512 个位（8 个 long，每个 long 64 位），支持按索引读写单个位。所有操作在事务中执行，数据自动持久化。

## 包路径

```
Zeze.Collections.BoolList
```

## 存储原理

BoolList 将索引空间按 **512 位（64 字节）** 分块，每个块存储为一条记录。块内使用 8 个 `long` 字段（`item0` ~ `item7`）表示 512 个布尔位。索引到存储位置的映射：

```
recordIndex = index / 512       // 确定记录
bitPosition = index % 512       // 确定块内位偏移
longIndex    = bitPosition / 64  // 确定 item0~item7
bitMask      = 1L << (bitPosition % 64)  // 位掩码
```

## 快速开始

```java
Zeze.Application zeze = new Zeze.Application(config);
BoolList.Module boolModule = new BoolList.Module(zeze);

// 打开 BoolList 实例
BoolList flags = boolModule.open("player_flags_123");
```

## API 参考

### Module 方法

| 方法 | 说明 |
|------|------|
| `open(String name)` | 打开 BoolList 实例 |

### BoolList 操作

以下 `get`/`set`/`clear` 操作必须在 `Procedure` 内调用。`clearAll` 在事务外调用。

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `get(int index)` | `boolean` | 读取指定位 |
| `set(int index)` | `void` | 设置指定位为 `true` |
| `clear(int index)` | `void` | 清除指定位为 `false` |
| `clearAll()` | `void` | 清除所有位（事务外调用，批量删除） |
| `getName()` | `String` | 获取名称 |

## 使用示例

### 功能开关

```java
BoolList flags = boolModule.open("player_flags_" + playerId);

zeze.newProcedure(() -> {
    // 设置第 0 位（如：已完成新手引导）
    flags.set(0);

    // 设置第 1 位（如：已领取首充奖励）
    flags.set(1);

    // 检查状态
    if (flags.get(0)) {
        System.out.println("新手引导已完成");
    }

    // 清除某个标记
    flags.clear(1);

    return 0;
}, "flag_ops").call();
```

### 清除所有标记

```java
// clearAll 在事务外调用，内部自行管理事务
flags.clearAll();
```

## 内部实现

| 存储表 | 键 | 值 | 用途 |
|--------|----|----|------|
| `_tBoolList` | BKey(name, recordIndex) | BValue(item0~item7) | 位图数据 |

`clearAll` 使用 `walkDatabaseKey` 逐批扫描并删除当前 name 的所有记录，每批 20 条，避免大事务。

## 注意事项

1. **索引范围** -- 索引必须 >= 0，无上限（按需自动扩展）
2. **名称限制** -- 名称不能包含 `@` 字符，不能为空
3. **clearAll 事务外** -- `clearAll()` 在事务外调用，内部自行管理事务
4. **密度考量** -- 如果布尔值密度很低，考虑用 [LinkedMap](./linked-map) 的 Set 模式替代
