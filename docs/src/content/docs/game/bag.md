---
title: "背包系统"
sidebar:
  order: 1
---

Bag 模块（`Zeze.Game.Bag`）为游戏提供完整的背包物品管理功能。它支持物品的添加、移除、移动、销毁和排序，内置自动堆叠与拆分机制，所有操作均在 Zeze 事务中执行以保证数据一致性。

## 核心数据结构

背包数据存储在持久化表 `tbag` 中，以背包名称为主键。每个 **BBag** 包含：

- **capacity**：背包容量（格子总数），通过 `getCapacity()`/`setCapacity(int)` 读写。
- **items**：`Map<Integer, BItem>`，以格子位置为键，**BItem** 包含物品 ID、数量和扩展属性（通过 `DynamicBean` 支持任意自定义字段）。

## 打开背包

通过 `Bag.Module.open(bagName)` 在事务内获取或创建背包实例：

```java
Bag.Module bagModule = new Bag.Module(providerApp, itemId -> pileMax);
Bag bag = bagModule.open("role_" + roleId); // 在事务内调用
```

`open` 返回的 **Bag** 实例与事务绑定，不可跨事务持有。

## API 详解

### add -- 添加物品

```java
// 简单物品（仅 id 和数量）
int remain = bag.add(10001, 5);  // 添加 5 个 id=10001 的物品

// 指定格子和扩展属性
BItem item = new BItem();
item.setId(10001);
item.setNumber(10);
item.getItem().setBean(customExtData);
int remain = bag.add(positionHint, item);
```

**添加逻辑**：

1. **优先堆叠**：如果 `positionHint` 指定的格子已有相同 ID 的物品，先尝试堆叠至堆叠上限（`pileMax`）。
2. **遍历已有格子**：继续在已有同 ID 格子上堆叠。
3. **新格子分配**：剩余物品自动拆分到空格子中，每个格子不超过 `pileMax`。
4. **返回值**：返回未能添加的剩余数量。返回 0 表示全部添加成功。

堆叠上限通过 `Bag.Module` 构造函数的 `funcItemPileMax` 参数配置，默认为 1（不可堆叠）。

### remove -- 移除物品

```java
// 从任意格子移除
boolean ok = bag.remove(10001, 3);  // 移除 3 个

// 优先从指定格子移除
boolean ok = bag.remove(positionHint, 10001, 3);
```

移除失败（物品不足）时返回 `false`。**重要**：如果调用者在失败时需要回滚整个事务来避免部分删除，可以使用嵌套事务来尝试。

### move -- 移动物品

```java
int code = bag.move(from, to, number);
// number=-1 表示移动全部
```

`move` 方法统一处理以下场景：

- **移动**：源格子和目标格子都为空或同 ID 时，直接移动。
- **交换**：目标格子存在不同 ID 的物品时，交换两个格子。
- **叠加**：目标格子存在同 ID 物品时，叠加至 `pileMax`。
- **拆分**：`number` 小于源格子数量时，拆分一部分到目标格子。

返回值使用 `Module.ResultCode*` 常量表示错误原因：

| 常量 | 含义 |
|------|------|
| `ResultCodeFromInvalid` | 源格子越界 |
| `ResultCodeToInvalid` | 目标格子越界 |
| `ResultCodeFromNotExist` | 源格子无物品 |
| `ResultCodeTrySplitButTargetExistDifferenceItem` | 拆分时目标已有不同物品 |

### destroy -- 销毁物品

```java
bag.destroy(position);  // 直接删除指定格子的物品
```

### sort -- 排序

```java
bag.sort(null);  // 默认按物品 ID 排序
bag.sort(Comparator.comparingLong(x -> x.getValue().getId()));  // 自定义排序
```

排序会重新分配所有物品到从 0 开始的连续格子位置。

## 容量管理

背包容量通过 `getCapacity()`/`setCapacity(int)` 管理。添加物品时只在 `[0, capacity)` 范围内寻找空格子。扩大容量后新格子立即可用，缩小容量不会删除已有物品但可能导致后续操作异常，建议在事务中同步调整。

## 事务安全

所有 Bag 操作都在调用者的事务上下文中执行。Zeze 的乐观锁机制保证并发安全：

- 多个事务同时修改不同背包互不冲突。
- 同一背包的并发修改会在提交时检测冲突并自动重做。
- `add` 返回非零剩余数量时，如需回滚已添加的部分，应使用嵌套事务或直接回滚外层事务。

## 协议处理

Bag 模块内置了客户端协议处理：

- **Move** 协议：处理客户端的物品移动请求，事务级别为 `Serializable`。
- **Destroy** 协议：处理客户端的物品销毁请求。

客户端通过 `ProviderUserSession` 发送协议，服务端在事务中执行操作并在提交后回复响应。
