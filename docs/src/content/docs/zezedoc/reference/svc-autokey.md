---
title: "AutoKey 自增 ID"
description: "Zeze 分布式唯一自增 ID 分配器 AutoKey 的用法"
category: reference
order: 31
---

> 本文档说明 Zeze 的 AutoKey 组件：用于分配系统内**唯一** Id（`long`），由 Zeze 初始化、分布式唯一，业务代码通过 `Application.getAutoKey(name)` 获取实例并调用 `nextId` 取号。

## 概述

AutoKey 用于分配系统内**唯一**的 Id（`long` 类型）。它保证唯一性；在**单台 Server 内部单调递增**，但不同 Server 之间返回值不在同一数值区间（高位字节编码了 serverId），因此**跨 Server 比较大小无意义**。

| 特性 | 说明 |
|------|------|
| 类型 | `long` |
| 唯一性 | ✅ 系统内唯一 |
| 单 Server 内 | ✅ 单调递增 |
| 跨 Server | ❌ 不保证全局递增 |
| 分布式 | ✅ 分布式唯一 |
| 初始化 | 由 Zeze 自动完成，**不需要手动初始化** |

## 用法

通过 `app.getAutoKey(name)` 得到 AutoKey 实例，调用 `nextId()` 得到下一个 Id：

```java
AutoKey autoKey = app.getAutoKey("demo_Module1_RoleId");
long newRoleId = autoKey.nextId();
```

> 💡 **性能提示**：建议保存 `getAutoKey` 的返回值（AutoKey 实例）重复使用，可稍微提高效率，避免每次取号都查找实例。

```java
// 推荐：缓存实例
private final AutoKey roleIdKey = app.getAutoKey("demo_Module1_RoleId");

public long newRoleId() {
    return roleIdKey.nextId();
}
```

## API 一览

| API | 说明 |
|-----|------|
| `Application.getAutoKey(name)` | 按名字获取或创建 AutoKey 实例（内部调 `autoKey.getOrAdd(name)`） |
| `AutoKey.nextId()` | 返回下一个唯一 Id（单 Server 内单调递增） |

## 相关文档

- 配置参考：[./configuration.md](./configuration.md)
