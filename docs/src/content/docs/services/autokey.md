---
title: "AutoKey 自增 ID"
sidebar:
  order: 5
---

AutoKey 是 Zeze 框架提供的**分布式自增 ID** 生成器。它在事务内分配全局唯一的递增 ID，支持多服务器并发分配而不冲突。AutoKey 通过步长预分配机制减少网络开销，在保证 ID 唯一性的同时提供高性能。

## 原理

AutoKey 的核心思路是**本地预分配**。每台服务器不是每次分配一个 ID 都访问全局存储，而是一次性申请一个范围的 ID（称为 Range），然后在本地从这个范围中快速分配。

```java
// Range 分配范围: [start+1, end]
private static final class Range extends AtomicLong {
    private final long max;

    public long tryNextId() {
        var nextId = incrementAndGet();
        return nextId <= max ? nextId : 0; // 0 表示范围用尽
    }
}
```

当本地 Range 用尽时，会在事务内向全局存储（`tAutoKeys` 表）申请下一个 Range。全局存储为每个 `(serverId, name)` 组合维护一个递增的 `nextId` 值。

### ID 结构

生成的 ID 由 `serverId` 和 `seed` 两部分编码组成：

```
[serverId (变长)] [seed (变长)]
```

- `serverId` 由配置的 `ServerId` 决定，不同服务器生成的 ID 天然不会冲突
- `seed` 是单调递增的种子值

当 `serverId = 0` 时，只编码 seed，生成的 ID 更短。

## API

### 获取 AutoKey 实例

```java
AutoKey.Module autoKeyModule = zeze.getAutoKey();
AutoKey myKey = autoKeyModule.getOrAdd("myCounter");
```

### nextId -- 获取 long 类型 ID

```java
long id = myKey.nextId();
```

返回一个 long 值，由 serverId 和 seed 编码而成。由于使用大端编码，返回值始终为正数。

### nextString -- 获取 Base64 字符串 ID

```java
String id = myKey.nextString();
```

返回 Base64 编码的字节数组表示，常用于生成 timerId 等字符串标识。

### nextBinary / nextBytes -- 获取二进制 ID

```java
Binary id = myKey.nextBinary();
byte[] id = myKey.nextBytes();
```

### setMinId -- 设置最小 ID

```java
myKey.setMinId(10000); // 使下次 nextId() 不小于 10000
```

### getSeed / setSeed -- 直接操作种子

```java
long currentSeed = myKey.getSeed();
myKey.setSeed(1000);  // 新种子必须大于当前值
```

## 步长配置

步长通过 `TimeAdaptedFund` 的 `AutoKeyLocalStep` 配置控制。这个参数决定了每次本地 Range 用尽时向全局存储申请的 ID 数量。

较大的步长减少全局存储的访问频率但增加 ID 的浪费（服务器重启后 Range 中未用完的 ID 丢失）。

```xml
<AutoKeyLocalStep>100</AutoKeyLocalStep>
```

## 使用限制

- `serverId` 不能为负数，否则 `nextId()` 返回负值
- `serverId` 限制了 ID 的总量空间，因为 ID 中需要编码 serverId
- 步长预分配意味着 ID 不是严格连续的（服务器重启后会有间隔）
- 所有 ID 操作需要在事务环境内执行（全局 Range 申请需要事务支持）
