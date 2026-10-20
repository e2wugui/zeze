# 10 Dbh2 redo/deliver 重复投递误报 divergence

- **严重级**：P2（数据幂等安全，纯可观测性缺陷：良性竞态被记为"人工对账"级 error）
- **类别**：Dbh2 复制协议/redo 兜底误报
- **子系统**：Dbh2
- **位置**：`ZezeJava/ZezeJava/src/main/java/Zeze/Dbh2/CommitRocks.java:91-92`（redo 无闸）、`:51`（60s 周期）；`Commit.java:59-63`（deliver 异步化）；`Dbh2StateMachine.java:609-612`（桶侧 error 分级）
- **状态**：⚔️ 对抗验证 CONFIRMED → ✅ **已修复·未提交（2026-10-20，owner 指示暂缓提交）**：桶侧 commitBatch 的 not-found 分支由 error 降为 warn，文案对齐 Onz 的 "duplicate delivery after commit -- normal idempotent redo" 分类（重复投递列为第一成因）与本状态机重复 undoBatch 的 warn 口径；true divergence 子类由上方 undonePending 复活分支（:605）单独 error 留痕不丢失。修法排序采对抗验证结论：桶侧判 warn 为完备解（redoTimer 年龄闸压不住 redo×redo 形态）。javac 零告警。红队强化证据保留备查

## 缺陷

`redoTimer` 对 eCommitting 无任何在途/年龄闸（ePreparing 有 inFlightTids 检查，:97），与异步
`commitDeliver` 竞态重复投递：deliver 在池内逐桶顺序 await（单桶预算 rpcTimeout+5s，多桶总时
长轻易超 60s redo 周期），任一桶投递超一个 redo 周期（如目标桶 raft 换主 30-60s 收敛中）时，
redoTimer 对**全部桶**重发 commitBatch——健康桶的第二个 LogCommitBatch apply 时
`transactions.remove(tid)` 为 null、`undonePending` 亦 null，落入 error 分支输出
"divergence-class event, manual check recommended"。数据无损坏（replace 幂等，redo 随后
removeTransactionRecord 正常收敛），但触发不必要的"人工对账"。

对照同库先例：Onz 参与方对同构竞态（Onz.ProcessCommitRequest 的 null 分支）已把重复 Commit
显式分类为 "normal idempotent redo" 的 warn——Dbh2 桶侧 error 文案只列了两个成因，漏掉最常见
的良性成因（redo 与在途 deliver 的重复投递），分级不对称。

## 代码证据

```java
// CommitRocks.java:91-92 —— eCommitting 无在途/年龄检查，直接对全部桶重发
case Commit.eCommitting:
    redo(it.key(), Dbh2Agent::commitBatch);
    break;

// Commit.java:57-63 —— 应答与投递解绑，deliver 异步
r.SendResult();
var pool = Zeze.Util.Task.getThreadPool();
if (null != pool)
    pool.execute(() -> Objects.requireNonNull(rocks).commitDeliver(decision));
```

## 触发场景

decide 已持久化 eCommitting 并应答成功；commitDeliver 逐桶 await 中，某桶超 60s（raft 换主
收敛）→ redoTimer 全桶重发 → 健康桶第二个 LogCommitBatch 落 error 分支。

## 修法方向

对齐 Onz 口径：桶侧 commitBatch 的 not-found 分支把"疑似 redo/deliver 重复投递"显式归类为
warn（可用协调者侧重发计数或投递时间窗辅助判别）；或 redoTimer 对 eCommitting 记录最近投递
时间、加最小年龄闸。
