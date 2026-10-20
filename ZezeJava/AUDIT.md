# 审计档案（AUDIT）
> 与 BENCHMARK.md 对称：缺陷审计的单一真相源。每轮审计一个日期节；逐条完整证据在 git 历史。
对 `ZezeJava/src`（约 240k LOC；Builtin 85k 生成代码仅抽查手写逻辑）的缺陷审计终局档案。
流程三道：9 分片子代理初审（Transaction 并发/逻辑双镜头、集合族、Util、Net/Netty/Serialize、
Raft、Services、Dbh2/Onz/Hot/History、Arch/Game/Component/Builtin）→ 主审逐条读码复核 →
红队对抗验证（逐条设法推翻，撤回/降级经主审反向亲验）→ owner 裁定。

**统计：初审 13 条 → 修复 7、对抗验证撤回 2、owner 定谳关闭 2、裁定不修 2。无 P0/P1。**
逐条完整档案（证据引用/触发场景/红队判决全文）在 git 历史 `review/` 路径
（46dc35c55 引入，至 662c71e0b 终态，14 文件 607 行）。

## 记分板

| # | 子系统 | 缺陷 | 终判 | 提交 |
|---|---|---|---|---|
| 01 | Transaction | copyValue 撕裂读 | 误报撤回 | — |
| 02 | Transaction | JDBC 三后端 SP 返回码 4 丢映射 | 修复 | 10e07207a |
| 03 | Raft | 空闲 leader apply 活性竞态 | 修复（方案b：心跳回调兜底） | 73023242a |
| 04 | Collections | putAll 批内重复引用原子性 | owner 不修 | — |
| 05 | Collections | LogSortedMap2 漏 decoded 守卫 | 修复 | 11274ca62 |
| 06 | Collections | remove(K,V) 空值 NPE 分叉 | 修复（防御对齐） | 7cbb7731e |
| 07 | Net | Selector 排空无防护+codec 泄漏 | 修复 | 19538809b |
| 08 | Serialize | GenericBean 递归无上限 | 记账不修 | — |
| 09 | Util | JsonReader EOF 越界（九扫描点） | 修复 | efbc361eb |
| 10 | Dbh2 | redo 误报 divergence | 修复（桶侧判 warn） | 5c73c117f |
| 11 | Services | LoginQueue 越限入队 | 误报撤回 | — |
| 12 | Arch | RedirectHash 静默改投 | owner 定谳关闭 | 662c71e0b（注释） |
| 13 | Arch/Game | processErrorSids indexOf 漏标 | owner 定谳关闭 | — |

## 设计语义裁定（owner 原意，防后人重查）

- **#12**：conc≤1 的 `@RedirectHash` 语义就是 orOtherServer——按 hash 选归属服务器，不可达时
  尽力而为改投环上任意活 provider 是定义内行为（`551241edb` 引入"尽可能返回一个有效的
  socket"，`ca7d43fda` 的 orOtherServer 经 `choiceHash(module, serverId, 1)` 复用该块为底座）。
  依赖服务器本地态（内存态/_tlocal）的方法应声明 conc>1（该路径显式抛不改投）。
  已在 RedirectBase.java 回退块注释钉死。
- **#13**：两 roleId 不可能共享 linkSid（换角=先 Logout 再 Login），分组 LinkSids 不会含
  重复 sid，除非客户端违约出错——不在服务端正确性契约内。
- **#04**：HasManagedException 的语义就是 Abort（throw 方式 Bean.java 已修订），设计本就
  允许日志/挂接状态变脏，命中即放弃整个事务；预检把 isManaged 提前、试图保持日志干净是
  历史补丁而非设计意图——保留现状，不为日志干净追加强化检查（如批内 IdentityHashMap 去重）。
- **#08**：ThreadLocal 深度计数方案（SkipUnknownField 同款）对 GenericBean 过于沉重，暂不
  防护。风险留档：public API，下游拿它解不可信字节即继承无界递归（每层 2 字节可构造
  数万层→SOE 穿透 catch(Exception)）；轻量替代=decode 入口字节数/节点数总预算闸。

## 误报撤回论证（防再立案）

- **#01**：call() 期对记录数据**零实际写**——LogInt/LogMap 全是影子日志（构造只存值），真实
  写在 `Savepoint.commit→Log.commit`、于 finalCommit 持 lockey.writeLock 执行；read-your-writes
  靠读侧查事务 log。`copyValue` 的 `tryEnterReadLock(0)` 守卫恰好覆盖全部真实写窗口，完备。
  教训：`TableX.cacheCopy` 注释"setter通过VarHandle立即应用"指 commit 期应用，措辞歧义
  误导审计——读注释的威胁模型必须区分它描述哪个阶段；定谳须验证机制前提本身。
- **#11**：accept 严格单线程——`Selectors` 单例池默认 1 线程（构造器 count=1，无任何配置
  旋钮可调）、每 listener 单 key 单 selector、`OnSocketAccept` 在 selector 线程同步回调、
  `tryOnAccept` 全库唯一调用方；并发方 drainQueue 对 queue 只删不加，方向上不可能致超限。

## 修复要点（证据链在各提交信息）

- **#02**：SP 显式设计 ret=4=并发首插认领失败，败者应回读胜者走 CAS 重试（Tikv/Mongo/Dynamo
  均返回 false），唯 JDBC 三后端落 default 抛 ISE 中断 OwnerCheck/publishSchemas 启动。
- **#03**：applyFuture 池线程 finally 置空不持锁 × 持锁判空不成对，空闲 leader 心跳成功分支
  此前无动作——已提交条目可停在未应用（客户端判败但数据已提交）；心跳回调补
  `commitIndex > lastApplied` 重试，对齐 follower 侧 :1859，收敛窗口=一个心跳周期。
- **#05**：decoded 拒绝契约由 3c96a10f8 在 LogMap2 确立（+专门测试），排序镜像漏接——补
  守卫+镜像回归测试。
- **#09**：九个扫描点（skipComment/end/skipNext/skipVar×4/parseKeyHash×5/parseKeyHashNoQuot×5/
  parseString/parseStringNoQuot）补 EOF 守卫，口径对齐 next() 的 NUL 哨兵宽容哲学。残余
  同类面：:1378 上方无引号原始串扫描器（语料外未展开）。
- **#10**：rpcTimeout 默认 60s≥redo 周期，单桶满超时即确定性重叠；redo×redo 使健康桶每轮
  吃一条假 error——not-found 分支对齐 Onz "normal idempotent redo" 判 warn；true divergence
  由 undonePending 复活分支单独 error 留痕。

## 已知排除单（审计前在案，未计入）

FlushSet onz addAll FND8-18 重试窗口；selectDirty 已提交未 checkpoint 不可见边界；
WebSocket acknowledgeReception JDK 21.0.11 bug（soak-20261020-0935 批 round20 唯一 1f 即此）；
Raft onTimer ABBA（已修）；RRS readFastPath oldTable（已封洞）。
