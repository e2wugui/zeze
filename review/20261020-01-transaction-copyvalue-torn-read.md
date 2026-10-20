# 01 copyValue 撕裂读窗口 —— 【已撤回：误报】

- **状态**：❌ 误报，2026-10-20 复核后被用户质疑撤回（初审：Transaction 并发镜头分片；
  一审定谳时未验证关键前提；用户指出"写操作在 lockey.writeLock 内"后二审定谳撤回）
- **位置**：`Record1.java:65-83`（copyValue）、`TableX.java:1192+`（cacheCopy）

## 撤回理由（正确的机制，留档防再查）

原判前提"call() 阶段 setter 经 VarHandle 立即应用、无锁"**不成立**。实际机制：

1. **普通字段（LogInt 族）**：生成 setter 只 `putLog(new LogInt(this, varId, vh, value))`
   （BItem.setNumber，`Builtin/Game/Bag/BItem.java:84-91`），LogInt 构造器不写字段，真实写
   在 `LogInt.commit()` 的 `vh.set()`（`Transaction/Logs/LogInt.java:39-42`）；read-your-writes
   靠读侧查事务 log（`getNumber` 先 `_t_.getLog(objectId()+2)`，`BItem.java:74-82`）。
2. **容器（LogMap 族）**：put/remove 只更新 Log 的影子 value——`LogMap.setValue` 仅写
   `this.value`（`Transaction/Collections/LogMap.java:27-29`），真实写在 `commit()` 的
   `((PMap)getThis()).map = value`（同文件 :38-40）。
3. **commit() 唯一调用链**：`Savepoint.commit():118`（逐 log 调 commit）← `Transaction.java:675`
   （finalCommit 内）。lockAndCheck 在 call() 返回后先行为全部脏记录取 lockey.writeLock
   （`Transaction.java:379` call → `:396` lockAndCheck 的次序确实如此，但这不构成漏洞——
   因为 call() 期间对记录数据**零实际写**）。

因此 `copyValue` 的 `lockey.tryEnterReadLock(0)` 守卫覆盖的窗口（finalCommit 逐 log 应用、
持写锁）恰好等于全部真实写窗口，**守卫完备，无撕裂读**。call() 期间仅有的急写是容器管理
元数据（`mapKey`/`initRootInfoWithRedo`，即 20261020-04 号发现的急写面），属框架元数据、
非业务数据，拷贝副本中的混合元数据无消费方。

## 教训（两则）

- `TableX.cacheCopy` 注释"事务修改的字段setter通过VarHandle立即应用（仅savepoint存undo）"
  描述的是 commit 期的 log 应用，措辞歧义直接误导了本次初审（可考虑改写为"提交期经
  Savepoint.commit 应用"）。审计读注释的威胁模型时必须区分它描述的是哪个阶段。
- 复审定谳必须验证**机制前提**本身（setter 是否真的立即写），不能只验证时序与守卫语义。

## 原始发现（仅存档）

原判称：tryEnterReadLock 只挡 lockAndCheck→finalCommit 窗口，call() 阶段并发
walkMemory/selectCopy 可拷出"字段X新/字段Y旧"撕裂态。——前提错误，见上。
