# CsQueue 使用文档

## 概述

`CsQueue`（Concurrent Server Queue）是 Zeze 框架提供的**分布式服务器队列**实现。每个服务器拥有自己私有的队列，只能操作自己的队列。当服务器宕机时，其他服务器会自动接管它的队列数据，实现高可用性。

## 包路径
```
Zeze.Collections.CsQueue<V extends Bean>
```

## 核心特性

| 特性 | 说明 |
|------|------|
| 服务器隔离 | 每个服务器操作自己的私有队列 |
| 故障转移 | 服务器宕机时自动被其他服务器接管 |
| 高可用 | 队列数据不会因单点故障丢失 |
| 双模式 | 支持 FIFO（队列）和 LIFO（栈）两种模式 |
| 持久化 | 数据自动同步到数据库 |
| 事务安全 | 所有操作在事务中执行 |

---

## 快速开始

### 1. 创建 Queue.Module

```java
// 在应用启动时创建 Module
Zeze.Application zeze = new Zeze.Application(config);
Queue.Module queueModule = new Queue.Module(zeze);
```

### 2. 打开 CsQueue

```java
// 打开一个名为 "taskQueue" 的 CsQueue
// 自动使用当前服务器的 serverId
CsQueue<Task> taskQueue = queueModule.openCsQueue("taskQueue", Task.class);

// 指定节点大小
CsQueue<Task> taskQueue = queueModule.openCsQueue("taskQueue", Task.class, 50);
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

### Module 类方法（通过 Queue.Module）

| 方法 | 说明 |
|------|------|
| `openCsQueue(String name, Class<T> valueClass)` | 打开 CsQueue，默认节点大小30 |
| `openCsQueue(String name, Class<T> valueClass, int nodeSize)` | 打开 CsQueue，指定节点大小 |

### CsQueue 主要方法

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
| `getInnerName()` | `String` | 获取内部队列名称（包含 serverId） |
| `getLoadSerialNo()` | `long` | 获取加载序列号 |

#### 遍历与清理

| 方法 | 说明 |
|------|------|
| `walk(TableWalkHandle<BQueueNodeKey, V> func)` | 遍历所有元素 |
| `clear()` | 清空所有元素 |

#### 故障转移

| 方法 | 说明 |
|------|------|
| `splice(int serverId, long loadSerialNo)` | 接管指定服务器的队列数据 |

---

## 使用示例

### 示例1：分布式任务队列

```java
// 定义任务Bean
public class Task extends Bean {
    public String taskId;
    public String taskType;
    public long createTime;
    public Map<String, String> params;
}

// 使用 CsQueue 作为分布式任务队列
CsQueue<Task> taskQueue = queueModule.openCsQueue("distributed_tasks", Task.class);

// 生产者：添加任务
zeze.newProcedure(() -> {
    Task task = new Task();
    task.taskId = UUID.randomUUID().toString();
    task.taskType = "send_email";
    task.createTime = System.currentTimeMillis();
    taskQueue.add(task);
    return 0;
}, "add_task").call();

// 消费者：处理任务
zeze.newProcedure(() -> {
    Task task = taskQueue.poll();
    if (task != null) {
        processTask(task);
    }
    return 0;
}, "process_task").call();
```

### 示例2：邮件发送队列

```java
// 定义邮件Bean
public class EmailTask extends Bean {
    public String to;
    public String subject;
    public String content;
    public int retryCount;
}

// 使用 CsQueue 作为邮件发送队列
CsQueue<EmailTask> emailQueue = queueModule.openCsQueue("email_queue", EmailTask.class);

// 添加邮件任务
zeze.newProcedure(() -> {
    EmailTask email = new EmailTask();
    email.to = "user@example.com";
    email.subject = "Welcome";
    email.content = "Hello!";
    email.retryCount = 0;
    emailQueue.add(email);
    return 0;
}, "send_email").call();

// 批量处理邮件
zeze.newProcedure(() -> {
    BQueueNode node = emailQueue.pollNode();
    if (node != null) {
        for (var value : node.getValues()) {
            EmailTask email = (EmailTask)value.getValue().getBean();
            sendEmail(email);
        }
    }
    return 0;
}, "batch_send").call();
```

### 示例3：操作日志栈（LIFO）

```java
// 定义操作日志Bean
public class OperationLog extends Bean {
    public String operationType;
    public String targetId;
    public long timestamp;
    public String operatorId;
}

// 使用栈模式记录操作历史
CsQueue<OperationLog> logStack = queueModule.openCsQueue("operation_logs", OperationLog.class);

// 记录操作
zeze.newProcedure(() -> {
    OperationLog log = new OperationLog();
    log.operationType = "update_config";
    log.targetId = "config_001";
    log.timestamp = System.currentTimeMillis();
    log.operatorId = "admin";
    logStack.push(log);  // 压入栈顶
    return 0;
}, "log_operation").call();

// 撤销最近的操作
zeze.newProcedure(() -> {
    OperationLog lastLog = logStack.pop();  // 弹出最近的操作
    if (lastLog != null) {
        undoOperation(lastLog);
    }
    return 0;
}, "undo_operation").call();
```

### 示例4：遍历队列

```java
// 遍历当前服务器的队列
long processed = taskQueue.walk((nodeKey, value) -> {
    System.out.println("NodeId: " + nodeKey.getNodeId() + ", Task: " + value.taskId);
    return true; // 返回true继续遍历，false停止
});
System.out.println("Total processed: " + processed);
```

---

## 故障转移机制

### 工作原理

```
Server A (serverId=1)          Server B (serverId=2)
┌─────────────────────┐        ┌─────────────────────┐
│  CsQueue "tasks@1"  │        │  CsQueue "tasks@2"  │
│  [task1][task2]...  │        │  [task5][task6]...  │
└─────────────────────┘        └─────────────────────┘
           │                              │
           │ Server A 宕机                │
           ▼                              │
    离线通知 ─────────────────────────────▶│
                                          │ splice(1, loadSerialNo)
                                          ▼
                            ┌─────────────────────┐
                            │  CsQueue "tasks@2"  │
                            │  [task1][task2]...  │ ← 接管的数据
                            │  [task5][task6]...  │
                            └─────────────────────┘
```

### 关键组件

1. **loadSerialNo（加载序列号）**
   - 用于标识队列的版本
   - 每次创建 CsQueue 时递增
   - 用于判断是否需要接管

2. **OfflineNotify（离线通知）**
   - 通过 ServiceManager 注册
   - 服务器离线时触发
   - 自动调用 splice 方法

3. **splice（拼接）**
   - 将离线服务器的队列数据拼接到当前服务器队列头部
   - 保证数据不丢失

### 故障转移流程

```java
// 1. 创建 CsQueue 时自动注册离线通知
var offlineNotify = new BOfflineNotify();
offlineNotify.serverId = module.zeze.getConfig().getServerId();
offlineNotify.notifySerialId = loadSerialNo;
offlineNotify.notifyId = "Zeze.Collections.CsQueue.OfflineNotify";
module.zeze.getServiceManager().offlineRegister(offlineNotify,
    (notify) -> splice(notify.serverId, notify.notifySerialId));

// 2. 服务器离线时，其他服务器收到通知并调用 splice
// 3. splice 将离线服务器的队列数据拼接到自己的队列
```

---

## 内部实现

### 数据隔离

```
CsQueue (name="tasks")
├── Server 1 → Queue "tasks@1" (私有)
├── Server 2 → Queue "tasks@2" (私有)
├── Server 3 → Queue "tasks@3" (私有)
└── ...

每个服务器只能操作自己的队列（tasks@当前serverId）
```

### 命名规则

- **外部名称**：`tasks`（用户指定的名称）
- **内部名称**：`tasks@1`（名称@serverId）

### 存储结构

```
_tQueues 表
├── "tasks@1" → BQueue (Server 1 的队列根节点)
├── "tasks@2" → BQueue (Server 2 的队列根节点)
└── ...

_tQueueNodes 表
├── ("tasks@1", nodeId) → BQueueNode
├── ("tasks@2", nodeId) → BQueueNode
└── ...
```

---

## CsQueue vs Queue 对比

| 特性 | CsQueue | Queue |
|------|---------|-------|
| 服务器隔离 | 是（每个服务器私有） | 否（共享队列） |
| 故障转移 | 支持 | 不支持 |
| 适用场景 | 分布式任务处理 | 单服务器队列 |
| 并发安全 | 是 | 是 |
| 数据隔离 | 按 serverId 隔离 | 共享数据 |

---

## 使用场景

### 适合使用 CsQueue

- 分布式任务队列（每台服务器处理自己的任务）
- 邮件/消息发送队列
- 需要高可用的队列场景
- 多服务器协同消费的场景

### 不适合使用 CsQueue

- 需要全局共享的队列
- 简单的单机应用
- 不需要故障转移的场景

---

## 注意事项

1. **事务要求**：所有操作必须在 `Procedure` 中执行
2. **服务器标识**：依赖 `zeze.getConfig().getServerId()` 获取当前服务器 ID
3. **ServiceManager 依赖**：需要正确配置 ServiceManager 才能实现故障转移
4. **名称限制**：名称不能包含 `@` 字符（保留用于 serverId 分隔）
5. **值类型**：值类型必须继承自 `Bean`
6. **splice 调用**：一般由系统自动调用，手动调用需谨慎

---

## 源码位置

`ZezeJava/ZezeJava/src/main/java/Zeze/Collections/CsQueue.java`
