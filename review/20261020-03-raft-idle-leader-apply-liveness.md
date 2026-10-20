# 03 Raft 空闲 leader apply 活性竞态

- **严重级**：P2（微秒级窗口、下一条写入自愈、无数据损坏；但"已提交被客户端判失败"超出纯性能）
- **类别**：apply 触发竞态/leader 活性
- **子系统**：Raft
- **位置**：`ZezeJava/ZezeJava/src/main/java/Zeze/Raft/LogSequence.java:1005`（置空不持锁）、`:992`（持锁判空）、`:1162`（心跳成功分支无动作）；对照 follower 侧 `:1859-1869`、AE 早退分支 `:957-963`
- **状态**：已复核存活（Raft 分片初审，主审读码定谳）

## 缺陷

后台 apply 任务收尾的 `applyFuture = null` 在池线程 finally 里执行、**不持 raft 锁**，与
`tryStartApplyTask` 的持锁判空不成对。`backgroundApply` 末次迭代解锁（:1021）到置空（:1005）
之间存在窗口：此刻 AE 应答进 `tryCommit` 推进 commitIndex 并调 `tryStartApplyTask`——观察到
`applyFuture != null` 跳过启动，而旧任务已死。若该条目是当时最后一条日志（写负载恰好停止），
leader 侧无任何后续触发：心跳成功应答什么都不做（:1162 仅失败分支驱动复制），而
`tryStartApplyTask` 的 leader 侧调用点全在 AE 应答路径。集群完全静止时，已提交条目停在
lastApplied 之上直到下一条写入（新 AE 应答→tryCommit 早退分支恢复 apply）才自愈。

## 代码证据

```java
// :1000-1007 —— 置空在池线程 finally，不持 raft 锁
applyFuture = new TaskCompletionSource<>();
Raft.executeImportantTask(() -> {
    try {
        Objects.requireNonNull(applyFuture).setResult(...call() == 0);
    } finally {
        applyFuture = null; // 允许再次启动，不需要等待了。 ← 与 :992 持锁判空不成对
    }
});

// :1157-1175 —— 心跳成功分支：无 tryCommit/tryStartApplyTask（仅失败分支行动）
if (!heartbeat.Result.getSuccess() && connector.getPending() == null) { ... }
```

follower 侧每次 AppendEntries/心跳都重试（:1859 `commitIndex > lastApplied` 分支）；leader
的 AE 早退分支（:961-962，注释"空闲 leader 的复制应答是仅剩的周期触发点"）本可自愈，但静止
集群不再产生 AE 应答。

## 触发场景

bulk 写入使 `commitIndex - lastApplied ≥ backgroundApplyCount` 启动后台任务；任务最后一次
unlock 后、finally 置 null 前，follower AE 应答推进 commitIndex 并 tryStartApplyTask 被跳过；
此后集群无新写入。后果链：业务线程 `appendLog` 的 `future.await(2*AE+1000)` 超时 →
RaftRetryException → 重发命中未应用存根得 DuplicateRequest → Agent 按可重试码空转至 rpc
判死，客户端超时——数据实际已提交。

## 修法方向

任一：(a) 任务收尾的置空挪入 raft 锁内与判空配对；(b) 心跳成功回调补
`commitIndex > lastApplied` 的 apply 重试（对齐 follower 侧 :1859）。建议 (b)（同时兜住
其他未知丢触发形态）。
