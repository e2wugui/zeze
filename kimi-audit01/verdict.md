# kimi-audit01 复核拍板（verdict）

复核人：主机逐条亲核（引用代码逐字对账）。
分支：`fix/kimi-audit01`（基线 0624a00f7）。每条修复独立提交，红绿测试先行。

标记：
- **成立** = 机制确凿，已修复（提交号见下）或按既定取舍不修但确认缺陷存在。
- **不成立** = 复核后判定非缺陷。
- **存疑** = 机制部分成立但属设计边界/误用防护/影响极低，本轮不修。

---

## 高置信

### C-01：Timer this:: 引用注册，unregisterWatch 失效、stopEvents 无界膨胀 —— **成立，已修复**
- 亲核：`Timer.start/stop` 的 `registerWatch(this::tryRecordHotModule)`/`unregisterWatch(...)` 每次求值新实例，ConcurrentHashSet 按实例判等，注销永败；`tryRecordHotModule`/`tryRecordBeanHotModuleWhileCommit` 的 `stopEvents.add(this::onHotModuleStop)` 同构。`Game/Online.java` 已有字段固化范本（onHotModuleStopRef/tryRecordHotModuleRef 注释）。
- 同型顺带修复（超出清单范围）：`Arch/Online.java:392/:433`（清单点名）、`Collections/DepartmentTree.java`、`Collections/Queue.java`、`Collections/LinkedMap.java`（grep 发现的清单外同型点）。
- 修复：五处统一固化为实例字段只求值一次。测试 `TestTimerHotWatchRef`（stopEvents 幂等 + watch 登记注销对称，红→绿）。

### C-12：RedoQueue.deleteDoneTasks 未截断填充数组误删下一跳任务 —— **成立，已修复**
- 亲核：`WriteLong` 变长编码（v<0x40 仅 1 字节）；`RocksDatabase.Table.deleteRange(byte[],byte[])` 整数组透传；`add()` 落盘 key 按 WriteIndex 截断而 `deleteDoneTasks` 传 8 字节填充数组——`enc(N+1)` 是 `end.Bytes` 真前缀，字节序排前，任务 N+1 落入删除区间。重启有积压时 start():72 立即触发。
- 修复：边界按 WriteIndex 截断（copyOf），Allocate(8)→(9) 对齐。测试 `TestRedoQueueDeleteRangeNextTask`（单字节/双字节 key 两场景，红→绿）。

### T-01：Profiler.beginContext 丢 count=n+1 —— **成立，已修复**
- 亲核：`beginContext` 写槽位 n=count 后不推进；`toString/genInfo` 只迭代 [0,count)，count 仅由 onRedo 推进——begin 上下文全部不可见/互相覆写/被 REDO 覆写。
- 修复：恢复 `count = n + 1;`（88aeceec1 误删回归）。测试 `TestProfilerBeginContext`（红→绿）。

### T-20：DynamicBean 集合元素日志键碰撞 —— **成立，已修复**
- 亲核：日志键 `parent().objectId()+variableId()`；集合元素 parent=集合对象且 variableId 不参与唯一性（decode 工厂 `new DynamicBean(0,..)`、用户工厂恒为生成 varId）——同组元素键完全相同。`LogDynamic.encode` 里甚至已有"元素 varId==0 用集合 varId 补正"的逻辑（框架知情但事务键未补正）。碰撞点共 3 处：`DynamicBean.getBean/getTypeId/setBeanWithSpecialTypeId` 查找键、`Log.getLogKey()`（savepoint 存储）、`Changes.collect` 的 DynamicBean 分支查找。
- 修复：新增 `DynamicBean.dynamicLogKey()`（元素形态用自身 objectId；objectId 按 4096 步长自增低 12 位留 varId，与宿主键空间数学隔离），三处统一。History 线格式（encode 用 parent/varId 补正）不动，wire 兼容。测试 `TestDynamicBeanElementLogKey`（用户工厂元素+重启 decode 元素两形态，读污染+错写断言，红→绿）。

### T-23：PList2ReadOnly 泄漏可变 Bean —— **成立，已修复**
- 亲核：`copyTo(V[] array,...)` 与 `toArray()` 把受管活 Bean 直接交给调用方；家族对照 `PMap2ReadOnly.copyTo(Map.Entry<K,VReadOnly>[])`/`PSortedMap2ReadOnly` 同为 VReadOnly 且无 toArray。全仓无调用方，签名变更无破坏面。
- 修复：`copyTo` 签名改 `VReadOnly[]`（经只读迭代器填充），`toArray()`×2 移除（2 系 Map 家族无此方法）。测试 `TestPList2ReadOnlyCopyTo`（编译期契约锁定 + 填充一致性）。

### T-25：LogSet1/LogMap1/LogSortedMap1 幻影增量 —— **成立，已修复**
- 亲核：`LogSet1.addAll/removeAll` 只要一项变化就把 c 全量记入 added/removed；`LogMap1.putAll`/`LogSortedMap1.putAll` 同构全量记 replaced（清单主报 LogSet1，姊妹形态一并修）。commit 写回与 followerApply 幂等不受影响，幻影只污染 Changes 增量通知链。
- 修复：记账循环只记真实变化（addAll 按 `!old.contains(v)`、removeAll 按 `old.contains(v)`、putAll 按 `!Objects.equals(old.get(k), v)`）。测试 `TestSetMapPartialChangePhantomDelta`（set/map/sortedmap 三路径，红→绿）。

---

## 中置信

### C-02：loadTimer 失败节点跳过不重试 —— **成立，不修（显式设计取舍）**
- 复核（子代理初核+主机抽查）：外层循环失败分支只 error+sleep 后按 whileRollback 已推进的游标继续，`// skip error` 注释表明跳过是显式设计意图；无补偿机制（afterTransfer 只装死服链）。后果：一次瞬态失败该节点定时器本进程生命周期静默停摆。
- 不修理由：skip-error 是作者显式选择；改为重试需拍板重试次数/上限后行为（卡死 start vs 停摆），属产品决策。修复方向：失败节点有界重试+最终 FATAL 告警。

### C-03：重登录僵尸行拒同名 —— **存疑（主触发前提不成立）**
- 复核：fireOnline 版本不符只 cancelFuture 不删行、isNamedTimerIdOccupied 只查存储——两处机制属实；但现行 Game/Arch 重登录为两遍式，pass1 无条件 removeLocalAndTrigger 会清掉全部在线定时器行，审计的"重登录后 Local 保留"主前提不成立。残余窗口：跨服迁移清理丢失（约 600-1200s 自愈）、stale local 重绑。
- **补充发现（超出清单）**：_tlocal 是内存表而 _tRoleTimers 持久——进程崩溃时归属追踪（eOnlineTimers）丢失，孤儿行无任何 sweep，非 Memory 部署下命名空间被永久占用。建议单独立项。

### C-06：超时补偿"同连接串行"前提不成立 —— **成立，不修（协议级重设计）**
- 复核：锁协议 DispatchMode.Normal 池化派发，offer 顺序可与网络序颠倒（ThreadingServer 自己在 KeepAlive 改 Direct 的注释里承认"Normal 经共享线程池派发既不保证同连接顺序"）。触发需原请求 handler 滞留 ≥5s（池积压/长 GC），概率低；一旦发生锁/许可在客户端存活期内悬挂。
- 不修理由：修复需协议改造（序列号确认/授权回验），且既有补偿测试锁定的语义与之纠缠。修复方向：服务端授权带请求序号，客户端验证后采纳。

### C-07：ProcessKeepAlive 非原子 check-then-act —— **成立，已修复**
- 复核：Direct 在 IO 线程执行，多 selector 线程部署下不同连接并发；读-比-释放-写无原子保护，同 serverId 双实例交错双 release 误放合法新 owner 的锁。默认 1 selector 不可达，但 Selectors.add 扩容是框架既有用法。
- 修复：serial 判据段纳入模块锁（可重入，release 同锁先例；KeepAlive 10s 周期无争用顾虑）。竞态交错本身不可确定性构造红（既有测试 javadoc 自认"天然竞态档"），靠机制推演+全套件回归。

### C-08：SemaphoreRelease 不钳制超量释放 —— **成立，已修复**
- 复核：动作内无条件 semaphore.release(permits)（仅校验>0），持有 1 时 release(100) 真实容量膨胀（JDK 信号量对象终身不复替换），账面负值删条目。Mutex/RWLock 两侧有所有权防护，唯 Semaphore 打穿。
- 修复：钳制 min(permits, 入账持有量)。测试 TestThreadingSemaphoreOverReleaseClamped（真实服务器，容量恢复断言，红→绿）。

### T-02：ProcedureStatistics.Watcher 周期快照差值 —— **成立，不修（死代码+需新增累计源）**
- 复核：getLast() 是每 perfPeriod(100s) 清零的窗口计数，差值算法语义错误（稳态差≈0 永不触发）；仓库内无任何 watch 调用方。
- 不修理由：无调用方；正确实现需要未清零的累计计数源（现无），修法非无争议。

### T-04：停机窗口 setActiveTime NPE —— **成立，已修复**
- 复核：Application.stop 先 daemon=null(:828)、后 globalAgent.stop(:852)，窗口内 acquire 成功路径（业务线程）与 Login/KeepAlive 回调（IO线程）都会调 setActiveTime 直接解引用 NPE；业务线程路径无兜底，本可提交的事务假性失败。
- 修复：setActiveTime 判空跳过（daemon 已停时刷新无意义）。测试 TestGlobalAgentSetActiveTimeNoDaemon（红→绿）。

### T-07：NormalClose 先于终检点 —— **成立，已修复（收敛主窗口）**
- 复核：globalAgent.stop(:852) → NormalClose → GCM 立即释放全部锁 → 他进程 Acquire+读旧值+提交 → 本进程终检点(:870+)用旧脏值覆盖。TableX.reduceInvalid 注释自证 durability-before-downgrade 不变式，停机主路径违反。
- 修复：组件全停后、globalAgent.stop 前插入一轮 checkpointRun（先冲刷后释放）。残余窗口收敛为"冲刷与 NormalClose 之间的微窗内迟到提交"（由停机拒绝转 Closed 显式失败兜底），完整收口需在飞提交计数（T-11 同款），记档。无确定性红（停机时序交错），靠 TestSvc01 族回归+推演。

### T-08：Releaser 活命路径跑公共 commonPool —— **成立，不修（本轮范围外）**
- 复核：四层嵌套 parallelStream 全落 ForkJoinPool.commonPool（含 DB IO 与无超时 enterWriteLock）；超 serverReleaseTimeout(60s) 即 halt(123123) 无条件杀进程；周期检查点刷盘（MultiThreadMerge）同池——拥塞连坐误杀机制完整。
- 不修理由：改专用池涉及停机/异常路径线程模型重构（与 checkpointRun 同池问题纠缠），风险大于本轮授权；已有 Task.getCriticalThreadPool 设施可用，作为后续独立修复项记档。

### T-10：DynamoDb 建表不等 ACTIVE —— **成立，已修复**
- 复核：TableDynamoDb 构造只 createTable（AWS 异步），waitReady 默认空实现未覆写；schemasCompatible 首个 GetItem 即抛（不重试）→ 启动崩溃；Immediately 模式首批提交 halt。
- 修复：覆写 waitReady() 轮询 describeTable 至 ACTIVE/UPDATING（60s 上限）。无 DynamoDB 本地环境，不附测试（机制与 JDBC 后端的同步 DDL 语义对齐）。

### T-11：终检点 break-on-empty 晚注册丢写 —— **成立，不修（需新同步机制）**
- 复核：重查(:241)与 add(:246) 之间无原子性、无在飞提交计数；终检点 isEmpty 即 break 且轮间无等待——迟到 add 进入永不再 flush 的 map，perform 返回 Success，静默丢。窗口毫秒级（需 GC/调度恰命中）。
- 不修理由：真正收口需要"在飞提交计数在终检点前排干"（审计自认），新增停机协议机制超出本轮；补轮注释自认该缺口。

### T-16：驱逐镜像删除×并发装载镜像写入 —— **成立，不修（需 key 级互斥设计）**
- 复核：互斥按 Record 对象（fairLock）建立，镜像按 key 操作——驱逐的 rocksCacheRemove 迟到删除并发 load 的 rocksCachePut，活记录镜像丢失→读成"不存在"→getOrAdd 空值覆盖。窗口为驱逐线程几条指令被抢占，极窄高危。FND6-01 测试不覆盖此交错。
- 不修理由：修复需 key 级版本/身份校验（镜像删除带版本探测），属缓存协议改造；记档为后续项。

### T-24：2系 copy() 浅拷贝可变后门 —— **成立，已修复**
- 复核：copy() 仅复制容器引用，值 Bean 是原记录树内同一批受管实例；ReadOnly.copy() 拿到的"副本"get(k) 即活 Bean，事务内 setField 直改原记录。生成 assign 走逐元素 copy 不受影响，容器级 copy() 唯一调用方就是 ReadOnly 暴露口（全仓 grep 确认），深拷贝化无破坏面。
- 修复：PMap2/PList2/PSortedMap2.copy() 逐元素深拷贝（对齐 CollOne/生成代码语义）。测试 TestPMap2CopyDeep（红→绿）。

### T-26：GTable Row 陈旧视图静默丢写 —— **成立，已修复**
- 复核：Row.put 命中非空 backingRowMap 即直写；行被 rowMap().remove/clear 整体摘除后缓存仍非空——写入脱树受管 BeanMap1（还记幻影 redo），静默丢失。Guava 原版同构缺陷，Zeze 下丢持久化数据。
- 修复：陈旧视图守卫（backingMap 不含该行即抛 IllegalStateException，响亮失败优于静默丢失）。测试 TestGTableStaleRowAndPhantomRow（红→绿）。

### T-27：StandardTable.put 幻影空行 —— **成立，已修复**
- 复核：getOrCreate 先把空行 put 进外层容器（托管当场记 LogMap2）再内层 put；内层 HasManagedException（值已受管，现实路径——Game/Rank 注释自证）后 catch 续行→空行持久化。PMap2.putAll 已有"先全量校验"对照。
- 修复：先写内层再挂外层。测试 TestGTableStaleRowAndPhantomRow（红→绿）。

## 低置信

### T-03：Completed 后反向注册同趟执行 —— **存疑（误用防护类）**
- 复核：机制属实（trigger 循环拾取不区分类型的新动作），但仅误用场景（finalRollback 回调里注册 whileCommit）触达；正常用法是设计目的。API 加防护属可用性改进，非缺陷修复。不修。

### T-05：stop 先杀 daemon 排干期缺 keepAlive —— **不修（报告自认设计权衡）**
- 需"排干超 21s 且无 Acquire"同时成立；反向选择（守护活到 globalAgent.stop 后）引入排干期误触发 startRelease。既定取舍。

### T-06：ProcessDaemon 构造失败泄漏 —— **存疑（极低严重度）**
- 启动失败路径句柄泄漏、deleteOnExit 兜底。修复需重排构造序，收益极低。不修。

### T-09：GlobalAgentBase.config 非 volatile —— **成立，已修复**
- 置 volatile（一行，闭合可见性；实际风险低——JVM 首个锁周期后几乎必然可见）。

### T-12：PG 存储过程吞死锁 —— **存疑（无环境验证）**
- 机制属实（EXCEPTION WHEN OTHERS THEN END 吞掉一切，Java 重试死代码）；但移除异常处理器改变存储过程错误语义（并发首启竞态的吞咽可能是有意的），且本环境无 PG 可验证。记档：对齐 MySQL 行为（无 handler）需 DBA 拍板。

### T-13：ret 4 硬失败不走重试环 —— **存疑（对称性修复需真库验证）**
- 机制属实；等价路径其他后端优雅重试。修复（ret 4 → 版本重读）简单但需关系库环境验证，本环境不具备。记档。

### T-14：MongoDb.close 无双段守卫 —— **成立，已修复**
- 对照 DynamoDb 已修同型：try/finally 包 super.close()。

### T-15：Tikv 构造失败泄漏 TiSession —— **成立，已修复**
- createRawClient/createKVClient 抛异常时静默关 session 再抛。

### T-17：replaceTable 先发布后 open 窗口 —— **不修（热更瞬间毛刺，自愈）**
- 微秒~毫秒级窗口，reduce 侧协议异常自愈。记档。

### T-18：walkMemory 漏已提交未检点记录 —— **成立，不修（需遍历源设计决策）**
- 机制属实：受限容量内存表遍历 localRocks，dirty 记录（未 flush）不在 key 集内——与不限容量路径（遍历 dataMap）语义不一致。修复需"两源合并去重"的遍历语义设计（tombstone/版本取舍），超本轮。影响面：Arch/Online walkMemory 用默认配置即命中。记档为后续项。

### T-19：TableCache 构造期 timerNewHot 泄漏 —— **成立，已修复**
- 第二个周期注册失败时 cancel 已注册的 timerNewHot 再抛。

### T-21：setBeanWithSpecialTypeId 先挂接后校验 —— **成立，已修复**
- 校验前置：verifyWrite 抛 IllegalStateException 时不再留下已受管却未入日志的脏归属。

### T-22：LogBinary/LogString DECIMAL128 截断 —— **成立，已修复**
- 改精确字符串构造（对齐 LogDecimal 已修惯例）；main 源内无消费方，纯潜伏不一致闭合。

### T-28：GTable1/GTable2/BeanMap2 自赋值清空 —— **成立，已修复**
- clear 后遍历 _o_（==this 已空）零次迭代，整表静默清空并提交。自赋值守卫（BeanMap1 经 PMap1.assign 先快照的安全语义对照）。测试 TestGTableSelfAssignKeepsData（红→绿）。

### C-04：transferAll 死者链头尾行缺失死循环 —— **不修（需数据损坏前提）**
- 需外部删行/数据损坏前提；重试语义（TxFailed 无告警去重）改进属监控设计。记档。

### C-05：exceptCounter 静态 map —— **不修（正常有界）**
- 上限=出现过的坏 typeId 种数，数据污染时缓慢增长，可忽略。

### C-09：appSerialId 依赖水位文件 —— **不修（部署前提）**
- 容器重部署/清 CWD 前提；属运维契约（.zeze.pal 需随实例持久化），记档到部署文档层面。

### C-10：timeoutMs 无上限钉死 SimulateThread —— **存疑（误用/恶意输入面）**
- 服务端钳制上限值的选择（多长合理）是产品决策。记档。

### C-11：锁表只增不减 —— **不修（类注释自述"暂不考虑"）**
- WeakRef 回收方案作者已知并明确搁置。记档。

### C-13：ErrorRequestId 反向改写本地水位 —— **成立，已修复**
- 钳制单向推进：服务端回包水位小于本地时保持本地并 warn（换库/清数据/同名接新实例场景防 deleteDoneTasks 删错区间+hole 停摆）。

### C-14：SafeBatch 热更窗口误 cancel —— **存疑（需产品拍板）**
- 机制属实（findHandle 失败被当用户异常 cancel 批处理）；但"重试等热更完成"还是"显式终止"是语义决策。记档。

## 汇总

| 档 | 条目 |
|---|---|
| 成立已修复 | C-01, C-07, C-08, C-12, C-13, T-01, T-04, T-07(主窗口), T-09, T-10, T-14, T-15, T-19, T-20, T-21, T-22, T-23, T-24, T-25, T-26, T-27, T-28 |
| 成立不修（设计取舍/需新机制，已记档） | C-02, C-06, T-02, T-08, T-11, T-16, T-18 |
| 存疑（前提弱/误用类/需环境或产品拍板） | C-03, C-04*, C-09*, C-10, C-11*, C-14, T-03, T-05, T-06, T-12, T-13, T-17（带*者报告自认既定取舍/前提） |
| 不成立 | （无——42条中无复核后彻底推翻者；C-03主前提不成立但机制残余，归存疑） |

## 修复记录

分支 `fix/kimi-audit01`，16 个提交覆盖 22 条"成立已修复"项；红绿测试 11 个新测试类（TestRedoQueueDeleteRangeNextTask、TestDynamicBeanElementLogKey、TestTimerHotWatchRef、TestProfilerBeginContext、TestPList2ReadOnlyCopyTo、TestSetMapPartialChangePhantomDelta、TestGlobalAgentSetActiveTimeNoDaemon、TestThreadingSemaphoreOverReleaseClamped、TestGTableSelfAssignKeepsData、TestGTableStaleRowAndPhantomRow、TestPMap2CopyDeep）。T-23 为编译期契约锁定；T-10 无本地 DynamoDB 环境不附测试；C-07/T-07 竞态/停机时序无确定性红，靠机制推演+全量回归（ZezeJavaTest:test 1753 通过）。

另：复核中发现的清单外事项——(1) DepartmentTree/Queue/LinkedMap 的 this:: stopEvents 同型（已随 C-01 修复）；(2) _tlocal 内存表崩溃孤儿 _tRoleTimers 行无 sweep（C-03 补充发现，未修，建议立项）；(3) Onz 族 @Fast 测试固定端口存在并行车道互撞隐患（TestGcC02 的 51893-51896 与 Fnd21 支撑段 51890-51894 重叠），本轮观察到 3 次时序性失败，未修。
