# Queue 使用文档

## 概述

`Queue` 是 Zeze 框架提供的**持久化队列**实现，支持 **FIFO（先进先出）** 和 **LIFO（后进先出/栈）** 两种操作模式。所有操作都在事务中执行，数据会自动持久化到配置的数据库。

## 包路径
```
Zeze.Collections.Queue<V extends Bean>
```

## 核心特性

| 特性 | 说明 |
|------|------|
| 双模式 | 支持 Queue（FIFO）和 Stack（LIFO）两种使用方式 |
| 持久化 | 数据自动同步到数据库 |
| 事务安全 | 所有操作在事务中执行，乐观锁保证并发安全 |
| 热更新支持 | 支持值类型的动态重载 |
| 节点分块 | 大数据量时分多个节点存储，提高性能 |
| 时间戳 | 每个元素自动记录添加时间 |

---

## 快速开始

### 1. 创建 Module

```java
// 在应用启动时创建 Module
Zeze.Application zeze = new Zeze.Application(config);
Queue.Module queueModule = new Queue.Module(zeze);
```

### 2. 打开 Queue

```java
// 打开一个名为 "taskQueue" 的队列，值类型为 Task
Queue<Task> taskQueue = queueModule.open("taskQueue", Task.class);

// 指定节点大小（每个节点存储的元素数量，默认30）
Queue<Task> taskQueue = queueModule.open("taskQueue", Task.class, 50);
```

### 3. 基本操作

```java
// 开启事务
zeze.newProcedure(() -> {
    // ===== FIFO 队列模式 =====
    // 添加元素到队尾
    taskQueue.add(new Task());

    // 查看队首元素（不删除）
    Task head = taskQueue.peek();

    // 取出队首元素（删除）
    Task task = taskQueue.poll();

    // ===== LIFO 栈模式 =====
    // 压入元素到栈顶
    taskQueue.push(new Task());

    // 弹出栈顶元素（删除）
    Task top = taskQueue.pop();

    // ===== 通用操作 =====
    // 获取元素数量
    long count = taskQueue.size();

    // 判断是否为空
    boolean empty = taskQueue.isEmpty();

    // 清空队列
    taskQueue.clear();

    return 0;
}, "example").call();
```

---

## API 参考

### Module 类方法

| 方法 | 说明 |
|------|------|
| `open(String name, Class<T> valueClass)` | 打开 Queue，默认节点大小30 |
| `open(String name, Class<T> valueClass, int nodeSize)` | 打开 Queue，指定节点大小 |
| `openCsQueue(String name, Class<T> valueClass)` | 打开跨服务器队列 CsQueue |
| `openCsQueue(String name, Class<T> valueClass, int nodeSize)` | 打开 CsQueue，指定节点大小 |

### Queue 主要方法

#### FIFO 队列操作

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `add(V value)` | `void` | 添加元素到队尾 |
| `poll()` | `V` | 取出并返回队首元素，队列为空返回 null |
| `peek()` | `V` | 查看队首元素（不删除），队列为空返回 null |

#### LIFO 栈操作

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `push(V value)` | `void` | 压入元素到栈顶（队首） |
| `pop()` | `V` | 弹出栈顶元素，栈为空返回 null |

> **注意**：`pop()` 内部实现就是 `poll()`，两种模式可以混合使用。

#### 节点操作

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `pollNode()` | `BQueueNode` | 删除并返回整个头节点 |
| `peekNode()` | `BQueueNode` | 查看头节点（不删除） |

#### 查询操作

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `size()` | `long` | 获取元素总数 |
| `isEmpty()` | `boolean` | 判断是否为空 |
| `getName()` | `String` | 获取队列名称 |

#### 遍历与清理

| 方法 | 说明 |
|------|------|
| `walk(TableWalkHandle<BQueueNodeKey, V> func)` | 遍历所有元素 |
| `clear()` | 清空所有元素 |

---

## 使用示例

### 示例1：消息队列（FIFO）

```java
// 定义消息Bean
public class Message extends Bean {
    public String content;
    public long senderId;
    // ...
}

// 使用
Queue<Message> msgQueue = queueModule.open("global_messages", Message.class);

// 生产者：发送消息
zeze.newProcedure(() -> {
    Message msg = new Message();
    msg.content = "Hello World";
    msg.senderId = 1001;
    msgQueue.add(msg);
    return 0;
}, "send_message").call();

// 消费者：接收消息
zeze.newProcedure(() -> {
    Message msg = msgQueue.poll();
    if (msg != null) {
        System.out.println("Received: " + msg.content);
    }
    return 0;
}, "receive_message").call();
```

### 示例2：操作历史栈（LIFO）

```java
// 定义操作Bean
public class Action extends Bean {
    public String actionType;
    public String targetId;
    // ...
}

// 使用栈实现撤销功能
Queue<Action> actionStack = queueModule.open("player_actions_123", Action.class);

// 执行操作并记录
zeze.newProcedure(() -> {
    Action action = new Action();
    action.actionType = "move";
    action.targetId = "item_001";
    actionStack.push(action);  // 压入栈
    return 0;
}, "do_action").call();

// 撤销最近的操作
zeze.newProcedure(() -> {
    Action lastAction = actionStack.pop();  // 弹出栈顶
    if (lastAction != null) {
        // 执行撤销逻辑
        undoAction(lastAction);
    }
    return 0;
}, "undo_action").call();
```

### 示例3：批量处理节点

```java
// 批量取出整个节点的数据
zeze.newProcedure(() -> {
    BQueueNode node = taskQueue.pollNode();
    if (node != null) {
        // 一次性处理节点中的所有值
        for (var value : node.getValues()) {
            Task task = (Task)value.getValue().getBean();
            processTask(task);
        }
    }
    return 0;
}, "batch_process").call();
```

### 示例4：遍历队列

```java
// 遍历所有元素（不删除）
long processed = taskQueue.walk((nodeKey, value) -> {
    System.out.println("NodeId: " + nodeKey.getNodeId() + ", Value: " + value);
    return true; // 返回true继续遍历，false停止
});
System.out.println("Total processed: " + processed);
```

### 示例5：跨服务器队列 CsQueue

```java
// CsQueue 支持多服务器协同消费
CsQueue<Task> csQueue = queueModule.openCsQueue("distributed_tasks", Task.class);

// 不同服务器可以协同处理队列中的任务
zeze.newProcedure(() -> {
    Task task = csQueue.poll();
    if (task != null) {
        // 处理任务，自动记录处理者服务器
        processTask(task);
    }
    return 0;
}, "process_distributed").call();
```

---

## 内部实现

### 数据结构

```
BQueue (根节点)
├── headNodeKey  // 头节点键（队首）
├── tailNodeKey  // 尾节点键（队尾）
├── lastNodeId   // 最后分配的节点ID
└── count        // 元素总数

BQueueNode (数据节点) - 单向链表
├── nextNodeKey  // 下一个节点
└── values[]     // 元素列表（每个节点最多 nodeSize 个元素）

BQueueNodeValue (元素值)
├── timestamp    // 添加时间戳
└── value        // 实际数据（Bean）
```

### 存储表

| 表名 | 键 | 值 | 用途 |
|------|----|----|------|
| `_tQueues` | name | BQueue | 存储根节点 |
| `_tQueueNodes` | name + nodeId | BQueueNode | 存储数据节点 |

### 链表结构示意

```
HeadNode → Node1 → Node2 → ... → TailNode
   ↓         ↓        ↓              ↓
[value1]  [value2]  [value3]      [valueN]
[value4]  [value5]  [value6]
  ...
```

---

## Queue vs LinkedMap 对比

| 特性 | Queue | LinkedMap |
|------|-------|-----------|
| 访问方式 | 只能访问队首/队尾 | 可通过 key 随机访问 |
| 元素顺序 | FIFO 或 LIFO | 插入顺序，可移动 |
| 删除操作 | 只能删除队首 | 可删除任意元素 |
| 适用场景 | 消息队列、任务队列、操作栈 | 有序Map、背包、排行榜 |

---

## 注意事项

1. **事务要求**：所有操作必须在 `Procedure` 中执行
2. **名称限制**：名称不能包含 `@` 字符（保留用于内部）
3. **值类型**：值类型必须继承自 `Bean`
4. **节点大小**：`nodeSize` 只影响新创建的节点，建议根据业务调整
5. **混合使用**：`add/poll` 和 `push/pop` 可以混合使用，但需注意业务逻辑
6. **时间戳**：每个元素添加时会自动记录时间戳，可用于超时判断

---

## 源码位置

`ZezeJava/ZezeJava/src/main/java/Zeze/Collections/Queue.java`
