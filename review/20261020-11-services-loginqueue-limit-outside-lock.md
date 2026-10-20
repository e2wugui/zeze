# 11 LoginQueue 上限检查在 allocateLock 外（并发 accept 越限入队）

- **严重级**：P2（边界越限，有界超额，无正确性后果）
- **类别**：并发容器复合操作/排队边界
- **子系统**：Services
- **位置**：`ZezeJava/ZezeJava/src/main/java/Zeze/Services/LoginQueue.java:225-244`
- **状态**：已复核存活（Services 分片初审，主审读码定谳）

## 缺陷

`tryOnAccept` 的队列上限检查在 `allocateLock` **之外**：多个 selector 线程并发 accept 时，
队列长度恰为 `maxConnections-1` 的瞬间多个线程同时通过锁外检查、再各自在锁内 `queue.add`，
队列实际可超 `maxConnections`（并发 accept 线程数-1 个）。注释显示锁是为"accept 线程与
timer 线程串行化，防同一排队连接被并发分配两次"设计——边界检查留在了锁外。

## 代码证据

```java
// LoginQueue.java:225-244
boolean tryOnAccept(AsyncSocket so) throws Exception {
    if (queue.size() >= ...getMaxConnections()) {   // ← 锁外检查
        new PutQueueFull().Send(so);
        so.closeGracefully();
        return false;
    }
    // 与drainQueue同锁：accept线程与timer线程串行化，防同一排队连接被并发分配两次...
    allocateLock.lock();
    try {
        ...
        queue.add(so);                               // ← 锁内入队
```

## 触发场景

高并发登录洪峰，队列长度在 maxConnections-1 附近，多个 selector 线程同时 accept——本应
`PutQueueFull` 拒绝的连接被排队。

## 修法方向

把上限检查移入 `allocateLock`（drainQueue 同锁，检查-入队原子），一行级搬移。
