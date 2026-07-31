---
title: "Bag 背包系统"
description: "Zeze.Game.Bag 物品管理——自动堆叠拆分、移动交换、排序与扩展属性"
category: reference
order: 51
---

> 本文档描述 `Zeze.Game.Bag` / `AbstractBag` 的物品增删移、自动堆叠拆分、移动交换、排序、容量管理与扩展机制，供游戏背包业务开发检索参考。

## 模块定位

`Zeze.Game.Bag` 提供完整的物品管理能力，所有操作在**事务内**执行，自动持久化。

| 能力 | 说明 |
|------|------|
| 物品添加 | 自动堆叠（pile）与拆分，溢出返回剩余数量 |
| 物品移除 | 跨格子累加删除，不足返回 `false` |
| 移动/交换/拆分 | 单个 `move` 接口完成四种操作 |
| 排序 | 自定义 `Comparator` |
| 属性扩展 | `BeanFactory` + `DynamicBean` 支持任意扩展属性 |
| 同步客户端 | 基于 `ChangeListener` 同步数据 |

## 存储结构

背包由一张表 `tbag` 存储，key 为背包名（`bagName`），value 为 `BBag`：

| 字段 | 类型 | 说明 |
|------|------|------|
| `BBag.Capacity` | `int` | 容量（格子数） |
| `BBag.Items` | `map<int, BItem>` | 格子位置 → 物品（key 是 position） |

每个 `BItem` 由三个字段组成：

| 字段 | variable id | 类型 | 说明 |
|------|-------------|------|------|
| `Id` | 1 | `int` | 物品类型 id |
| `Number` | 2 | `int` | 数量 |
| `Item` | 3 | `dynamic`（`DynamicBean`） | 任意扩展属性 |

> `Item` 是 `DynamicBean`，通过 `BeanFactory` 注册后可承载任意自定义 Bean，实现「每个物品带自己的扩展属性」。

## 打开背包

```java
// 1. 创建 Module（提供物品堆叠上限查询函数）
var bagModule = new Bag.Module(providerApp, itemId -> pileMaxTable.getOrDefault(itemId, 99));

// 2. 在事务内打开具名背包
Bag bag = bagModule.open("inventory");
```

| 方法 | 说明 |
|------|------|
| `module.open(bagName)` | 打开（或创建）具名背包，需在事务内 |
| `module.getItemPileMax(itemId)` | 查询物品堆叠上限，未配置返回 `1`（不可堆叠） |
| `module.getTable()` | 取底层 `tbag` 表 |

### 堆叠上限

通过构造函数传入 `IntUnaryOperator funcItemPileMax`：

```java
new Bag.Module(providerApp, itemId -> {
    // 返回该物品可堆叠的最大数量；返回 1 表示不可堆叠
    return switch (itemId) {
        case 1001 -> 99;   // 普通材料可堆叠
        case 2001 -> 1;    // 装备不可堆叠
        default -> 999;
    };
});
```

---

## 物品添加

### add(id, number) —— 简单物品

```java
// 加入只有 id 和 number 的简单物品，自动堆叠与拆分
bag.add(1001, 30);
```

### add(positionHint, item) —— 完整物品

```java
BItem item = new BItem();
item.setId(1001);
item.setNumber(30);
item.getItem().setBean(myExtendBean);  // 可选：附加扩展属性

int remain = bag.add(-1, item);  // positionHint=-1 表示不指定格子
// remain > 0 表示背包满，剩余未放入的数量
```

| 返回值 | 含义 |
|--------|------|
| `0` | 全部放入成功 |
| `> 0` | 背包满，返回剩余未放入数量 |

### 自动堆叠与拆分逻辑

| 步骤 | 行为 |
|------|------|
| 1. 优先提示格子 | 若 `positionHint` 指定的格子是同 id 物品，先堆叠到上限 |
| 2. 遍历同 id 格子 | 继续堆叠到其他同 id 格子，直到达到 `pileMax` |
| 3. 拆分到空格子 | 剩余部分按 `pileMax` 拆分到空格子 |
| 4. 放不下 | 返回剩余 number |

> **失败处理**：若调用者回滚事务，所有添加被回滚；若不回滚，完成部分添加，返回剩余 number（可转邮件等系统）。想回滚全部但不回滚整个事务，用**嵌套事务**。

---

## 物品移除

### remove(id, number)

```java
// 跨格子删除 number 数量的指定 id 物品
boolean ok = bag.remove(1001, 5);
// false 表示物品不够
```

### remove(positionHint, id, number)

```java
// 优先删除 positionHint 指定格子的物品（右键使用场景友好）
boolean ok = bag.remove(3, 1001, 5);
```

| 返回值 | 含义 |
|--------|------|
| `true` | 删除成功 |
| `false` | 物品不够 |

> **警告**：返回 `false` 表示物品不足，此时应**回滚事务**，否则会部分删除。

---

## 移动 / 交换 / 拆分

`move(from, to, number)` 单接口完成四种操作：

```java
int code = bag.move(from, to, number);
```

| `number` 取值 | 行为 |
|----------------|------|
| `-1` 或超出当前数量 | 移动全部 |
| `<` 当前数量 | 拆分 |

| from / to 格子内容 | 行为 |
|--------------------|------|
| to 为空 | **移动**（或拆分） |
| from、to 同 id | **叠加**（或拆分） |
| from、to 不同 id | **交换**（仅当移动全部时） |

### 结果码

| 常量 | 值 | 含义 |
|------|----|------|
| `0` | 0 | 成功 |
| `ResultCodeFromInvalid` | 1 | from 格子非法 |
| `ResultCodeToInvalid` | 2 | to 格子非法 |
| `ResultCodeFromNotExist` | 3 | from 格子无物品 |
| `ResultCodeTrySplitButTargetExistDifferenceItem` | 4 | 试图拆分但目标存在不同物品 |

```java
int code = bag.move(0, 5, 10);
if (code != 0) {
    return errorCode(code);  // 回滚事务
}
```

---

## 销毁与排序

### destroy(from)

```java
// 直接删除指定格子物品（不返回物品内容）
bag.destroy(3);  // 返回 0
```

### sort(comparator)

```java
// 按物品 id 升序（默认）
bag.sort(null);

// 自定义排序：按数量降序
bag.sort((a, b) -> Integer.compare(b.getValue().getNumber(), a.getValue().getNumber()));
```

排序会重建格子映射（先 `copy` 再 `putAll`），保证托管状态正确。

---

## 容量管理

```java
bag.getCapacity();      // 当前容量
bag.setCapacity(100);   // 设置容量（格子数）
```

> 所有 position 参数（`positionHint` / `from` / `to`）均校验 `0 <= pos < Capacity`。

---

## 扩展属性（BeanFactory）

`BItem.Item` 是 `DynamicBean`，可挂载任意自定义 Bean。加入物品时**自动注册**其类型：

```java
// 自定义扩展 Bean（XML 定义后生成）
public class EquipExtra extends Bean { ... }

BItem item = new BItem();
item.setId(2001);
item.setNumber(1);
item.getItem().setBean(new EquipExtra());  // 附加扩展属性

bag.add(-1, item);  // 自动调用 Module.register 注册类型
```

| 机制 | 说明 |
|------|------|
| 自动注册 | `add` 时调用 `Module.register(item.getItem().getBean())` |
| 持久化 | 注册的 Bean `ClassName` 被持久化，Module `start` 时自动装载 |
| 热更新 | 配合 `BeanFactory` 支持类型热重载 |

---

## 客户端协议

`AbstractBag` 内置两个客户端协议：

| 协议 | 处理方法 | 说明 |
|------|----------|------|
| `Move` | `ProcessMoveRequest` | 客户端移动/交换/拆分请求 |
| `Destroy` | `ProcessDestroyRequest` | 客户端销毁请求 |

两者均以 `Serializable` 事务级别处理，成功后 `sendResponseWhileCommit` 回复。

---

## 同步数据给客户端

背包变更推荐通过 `ChangeListener`（挂在 `tbag` 表上）同步给客户端：

```java
bagModule.getTable().getChangeListenerMap().addListener((key, r) -> {
    String bagName = (String) key;
    switch (r.getState()) {
        case Changes.Record.Put:
            // 全量：背包插入或整体替换
            BBag fullBag = (BBag) r.getValue();
            online.sendWhileCommit(roleId, new SBagSync(bagName, fullBag.toData()));
            break;
        case Changes.Record.Edit:
            // 增量：可用 getVariableLog 取 Items 的变更
            break;
        case Changes.Record.Remove:
            // 背包被删除
            break;
    }
});
```

> `ChangeListener` 严格绑定事务提交，只有经过事务的修改才触发。详见 [ChangeListener](./listener.md)。

---

## 注意事项

| 注意点 | 说明 |
|--------|------|
| **必须在事务内操作** | `open` / `add` / `remove` / `move` 等均需事务 |
| **堆叠上限** | 由 `funcItemPileMax` 决定，未配置默认 `1`（不可堆叠） |
| **部分添加** | `add` 失败返回剩余数量，不回滚则部分生效 |
| **物品不足** | `remove` 返回 `false` 时应回滚事务，否则部分删除 |
| **扩展类型注册** | `add` 时自动注册，无需手动调用（除非特殊场景） |
| **托管状态** | 排序时对已托管 Bean 先 `copy` 再放回，避免状态错乱 |

---

## 相关文档

- [游戏模块总览](./game-overview.md) — Game 模块整体架构
- [ChangeListener 数据变更监听](./listener.md) — 同步数据给客户端的机制
- [Bean 数据模型](./bean.md) — DynamicBean、托管状态、BeanFactory
