# ZezeJava 核心基准档案（BENCHMARK）

> 单一真相源：跑法、判定协议、四时点档案与结论都在本文件。docs 站点《性能调优》仅留摘要链接。
> 绝对数与机器强相关：引用必须带本文件脚注，对比证据只认同机同配置。

## 跑法

```bat
:: ZezeJava 目录下
gradlew.bat :ZezeJavaTest:benchCore [-PbenchTag=名]           :: 核心宏观基准（多轮+JSON 留档）
gradlew.bat :ZezeJavaTest:benchCorePlatform [-PbenchTag=名]   :: F 调度场景平台线程池对照
gradlew.bat :ZezeJavaTest:benchGame [-PbenchTag=名]           :: L 游戏模拟基准（独立车道）
gradlew.bat :ZezeJavaTest:benchCore -PbenchHeap=4g ...        :: 跨版本对比公平堆（默认 2g，对齐历史存档真实口径）
gradlew.bat :ZezeJavaTest:benchCore -PbenchJvmExtra="-XX:StartFlightRecording=..." :: JFR/系统属性等诊断透传
gradlew.bat :ZezeJavaTest:benchGame -PgamePlayers=5000 -PgameK=20 -PgameMinutes=10 :: L 旋钮（在线数/时间缩放/窗口游戏分钟）
gradlew.bat :ZezeJavaTest:jmh -PjmhFilter=正则 [-PjmhExtraArgs="-prof gc"]          :: JMH 微基准
gradlew.bat :ZezeJavaTest:test / integrationTest              :: 每轮门禁
```

- 宏观结果 JSON 留档 `ZezeJavaTest/bench-results/<benchTag>/`（gitignored，绝对数不进库）。
- 对比工具：`java -cp ZezeJavaTest/build/classes/java/test harness.BenchCompare <目录A> <目录B>`。

## 场景清单（@Bench + @Tag("core")，ZezeJavaTest/src/Benchmark/）

| 场景 | 类 | 测什么 |
|---|---|---|
| A 单线程简单事务 | ABasicSimpleAddOneThread | 最简事务路径地板 |
| B 强冲突 | BBasicSimpleAddConcurrentWithConflict | 480 线程互踩 1 key，冲突重做路径 |
| C 一般并发 | CBasicSimpleAddConcurrent | 5000 key 派发写，并发无冲突 |
| D 容器事务 | DBasicContainerTransaction | 嵌套持久化 map 增删 |
| E checkpoint 稳态 | ECheckpointSteady | flush（SingleThread / MultiThreadMerge） |
| E2 RocksDB 哨兵 | E2CheckpointRocksDBSentinel | 真实落盘路径防"Memory 口径优化变差" |
| F 调度形态 | FDispatchPoolThroughput | 虚拟/平台线程池派发吞吐（+ofAction 变体） |
| G 缓存压力 | GCacheThrashing | miss/装载/驱逐（工作集远超容量） |
| H 只读并发 | HReadOnlyConcurrent | 两档：脏记录读（近期写入热数据）/ 干净记录读（已冲刷） |
| I MMO-lite 画像 | IMmoLiteWorkload | 背包/房间/排行/邮件/会话五件套混合（30/20/20/20/10） |
| J 网络 Rpc 回环 | JSocketRpcLoopback | 纯 Zeze.Net 层 256B 定长往返，64 深度流水线 |
| K 洪水下 checkpoint 竞争 | KCheckpointUnderCommitFlood | 派发池提交洪水与并发 flush 争 RRS 锁（flusher 饿死探针；观测 RRS.flushLockWait/FlushSet 告警） |
| L MMO-lite 游戏模拟 | GameSim.LMmoGameSim | 真实游戏逻辑全模拟（@Tag("game") 独立车道 benchGame，见下节） |

微基准（src-jmh）：Serialize / Collections / Conflict / ReadOnly 四族。

## L 场景设计（LMmoGameSim，ZezeJavaTest/src/GameSim/）

MMO-lite 游戏模拟世界：真实游戏逻辑承载访问模式，核心机制（编组/脏读记账/分片冲突/
大事务/纯读提交簿记）在真实业务形态下暴露。

- **架构铁律**：房间=内存态战斗状态机（tick/伤害公式/掷骰/respawn 全不经 Zeze），只向大厅
  投递结果事务；一切落库事务发生在大厅。单写者亲和：本角色记录由本人虚拟线程写（交易例外：
  受要约方一笔事务写双方）。心跳超时踢线是唯一 DaemonTimer 用法（内存扫描，踢人才落事务）。
- **七系统**：角色生命周期（login/心跳 15s/logout）、背包物品（loot/用药/穿脱/卖店）、PVE
  战斗、P2P 交易（双边原子换）、双排行（**内置 Zeze.Game.Rank 模块** 128 分片——勿手搓
  单记录榜，人为制造热 key 伪靶）、邮件（系统 fan-out 大事务/P2P/附件领取）、好友在线查询
  （**纯读事务**）。砍：聊天/公会/商城。
- **驱动与节奏**：每玩家一虚拟线程（默认 5000 在线，roleId 即种子）；per-player RNG 只在
  决策点（事务外）推进，loot 参数由房间掷骰定死后投递事件。真人交互频率 ×K 缩放：
  **K=20 节奏档**（延迟画像，offered 负载即天花板）、**K≥80 容量档**（压力/平台）。
- **指标**：总事务吞吐中位数（同日 A/B、3×噪声协议照旧）+ 每 op 延迟分位（JSON flat key
  `op.<name>.p50/p95/p99_us`，BenchCompare 信息性对照）。
- **窗口制**：世界连续运行，窗口=测量区间（默认 10 游戏分钟，预热 1+测量 3 取中位）；操作
  归属以发起时窗口为准，翻转后 2s 宽限收割；窗口内偏移 5 游戏分钟触发系统邮件 fan-out。
- **守恒验证**：停机后单只读事务对账——货币/物品守恒、会话无泄漏、榜单与重算一致、邮件
  inserted−claimed=存量，外加逐角色货币对账网；影子账本只记系统注入/销毁，**外部副作用一律
  whileCommit 登记在变更点**（提交点执行一次，重试安全）。测量轮内断言=事务零错误。
- **配置口径**：裸 Config 程序化构造（Config.load 补默认值会点亮 Onz 服务，disable SM 下
  NPE）；Memory 库、单进程本地锁、缓存 10 万、checkpoint 30s 墙钟。
- **已知口径瑕疵**：窗口计数含 ~2s 宽限渗漏（系统性高估 ~1/15，恒定偏差不碍同日 A/B）。

### L 基线与容量档案（2026-10-12）

**节奏档（K=20，5000 人/10 游戏分钟×3）**：master 36,660 txn/s 中位（cv ~1.4%）；
per-op 最大分位：mailFanout p95≈66ms（5000 行大事务）、loot p95=602µs/p99=2.6ms、
heartbeat p99 ~150-190ms（不随负载缩放，30s 周期 checkpoint 停顿嫌疑，未裁决）。

**容量档（K 梯度，同日顺序跑）**：K=20→40 吞吐 1.98× 纯线性且 p50 仍 µs 级（两档均空闲），
膝点在 K=40~80 之间：

| K | master 中位 | 心跳达成 | loot p50 | v1.6.3 中位 | v1.6.3 心跳 |
|---:|---:|---:|---:|---:|---:|
| 20 | 36,589 | 95.9% | 27µs | 36,988 | 96.1% |
| 40 | 72,310 | 99.5% | 45µs | 73,336 | 101.2% |
| 80 | 119,221 | 90.9% | 24.9ms | 86,088 | **65.3%** |
| 160 | **122,487** | **61.5%** | 50ms | — | — |

- **master 容量平台 ≈12.2 万 txn/s**（K=160 offered 再翻倍仅 +2.7%）；v1.6.3 膝点更早更陡
  （K=80 心跳 65%、loot p50 94ms=同点 master 4 倍），平台 ≈9 万量级——**容量代差 ≈1.3×、
  K=80 同点交付 1.39×**。K=20 的 1.00× 是"同 offered 双方足额交付"口径：延迟对比有效、
  容量对比无效，容量口径须 K≥80。

## 判定协议

- 显著性：宏观中位数变化 > 3× 合成噪声；JMH 置信区间不重叠。
- **裁决必须同日 A/B**：跨日/长会话宏观数字只作线索（曾实证同代码冷机单测 -18% 假回归，
  同日背靠背复测逆转为 +24%）。
- 冷却纪律：机器长会话热节流会把宏观噪声从 ±10% 爆到 ±40~90%；中段只信 JMH 短 fork 与分配率。
- 跨版本对比注意堆配置公平性（v1.6.3 回植分支固定 4g：B 场景在飞任务驻留量撑爆默认 512m）。

## 基准自身的坑（曾实锤踩过，勿再犯）

1. procedure 返回值非零 → defaultLogAction 逐次日志（曾造成 78× 假信号；基准必须返回 0）。
2. 裸 Application 的 addTable 必须在 start() 之前（cache 在 start 内建）。
3. 批次 drain 制造屏障波（B batch100=17万 vs 饱和 63万）。
4. 单大事务批量预热会造 RelativeRecordSet 大编组，污染后续读场景形态（+7% 偏置）。
5. 热路径逐 op 累计用 LongAdder（单 AtomicLong 缓存行争串行化）。
6. **A/B 对比必须交替或抵消顺序**：会话单调热漂移会把"基线后跑"误判为改动收益
   （+47% 假收益实证，事后击穿回滚）。
7. **空事务放大固定提交成本占比**：宏观百分比是提交簿记口径的上界，重业务负载按
   绝对 µs/笔 × 事务率折算，不能外推。
8. **受管 bean 不得跨容器搬移**（HasManagedException）——搬移一律取值重建新实例；
   PMap put 同键=整槽替换，入包必须走堆叠合并。
9. **事务外部副作用记账用 whileCommit**（变更点登记、提交点执行一次）：holder 跨重试轮
   残留 + 静默放弃轮（return 0 也算成功）组合会按幻影快照记账。
10. **Rank.create 返回 redirect 生成子类**：裸环境 @RedirectHash 被路由拦截、写不进本地表
    且静默返回 0——须 GameRankFactory.createLocal 基类直构；getOrAdd 只留给真正建点，
    建号不变量下的缺行让自然 NPE 进错误账本而非静默吞掉。
11. Windows 下 String.format %n=
 会破坏按字符数裁剪的手拼 JSON（用显式 
）；
    轮询 CompletableFuture.get 的 TimeoutException 是设计内节奏，勿计错误。

## 跨代档案（v1.6.3 → 2.x）

机器脚注：16C/JDK21/Windows 同机；v1.6.3 列 4g 堆（回植分支显式设定），2026-10-11 列
实为 2g——`-PbenchHeap` 当时被 build.gradle 的 withType(Test) 兜底覆盖而失效（当日已修复，
默认对齐 2g），"两列均 4g"的旧表述失真；两列堆不一致为已知口径瑕疵，跨代结论按量级引用，
严格 4g/4g 复测待补。v1.6.3=回植分支 `bench/backport-v1.6.3`
（TaskSpec→Task.runUnsafe 等价改写，I 场景排行榜 psortedmap→map15 同型承载），
2026-10-11=当日 master 实测（含 RRS 非公平锁优化 07171e5a7）。单位 ops/s（中位数），
括号内为相对 v1.6.3 倍数。L 行为 K=80 容量档同点交付（10-12 实测，K=20 同载荷两代 3.67万
持平=节奏档口径；平台 12.2万 vs ≈9万，详见 L 容量档案）。

| 场景 | v1.6.3 | 2026-10-11 |
|---|---:|---:|
| A 单线程简单事务 | 122.5万 | 159.6万（1.30×） |
| B 强冲突（480线程踩1key） | **1.26万** | 29.5万（**23.5×**） |
| C 一般并发（5000key 派发） | 88.6万 | 89.9万（1.01×） |
| D 容器事务 | 54.4万 | 68.0万（1.25×） |
| E flush MultiThreadMerge | 62.4万 | 77.8万（1.25×） |
| E flush SingleThread | 7.96万 | 8.74万（1.10×） |
| E2 RocksDB 哨兵 | 47.2万 | 17.7万（0.38×） |
| F 派发池（虚拟线程） | 83.6万 | 77.0万（0.92×） |
| F 派发池（平台线程） | 62.2万 | 51.8万（0.83×） |
| G 缓存压力（miss/装载/驱逐） | 35.6万 | 48.9万（1.37×） |
| H 只读并发·脏记录档（2.x 新增） | 37.4万 | 57.9万（1.55×） |
| H 只读并发·干净记录档（2.x 新增） | 151.9万 | 96.8万（0.64×） |
| I MMO-lite 混合画像（2.x 新增） | 31.6万 | 34.0万（1.08×） |
| J 网络 Rpc 回环 256B（2.x 新增） | 20.3万 | 20.5万（1.01×） |
| L MMO-lite 游戏模拟（2.x 新增，容量档） | 8.6万 | 11.9万（1.39×） |

当日多轮数字离散度偏大（长会话热态），倍数看量级不看小数；
2026-09-29 final 提交（5a92cc111）与本轮 HEAD 的同日 A/B 终裁：**592 个审计修复提交无性能回归**。

## 跨版本结论（v1.6.3 → 2.x）

- **B 强冲突 +20×**：v1.6.3 每次重试逐笔 `logger.info`（×10 主因）+ v2.0 周边改进（×2）。
- **H 只读两档，脏读记账是结构性成本**：读"已提交未冲刷"的脏记录比读干净记录慢
  3.7~4.1×（改非公平锁前口径）——读事务提交走 RRS 锁合并+编组重登记，并发锁车队；
  干净记录走孤立快路，单线程无此成本。生产缓解：批量事务后 checkpointRun 冲散编组、
  缩短 CheckpointPeriod。
- 网络 Rpc 回环（J）与 MMO 画像（I）两代持平——net 层与混合事务负载无跨代退化。
- **L 游戏模拟：K=20 同载荷 1.00×，容量口径 ≈1.3×**：手搓单记录榜时代的 1.80×
  （46,092 vs 25,649）是口径伪影——单 key 31k puts/s 把 v1.6.3 的串行化弱点放大到
  loot p95=284ms 且 K=20 即饱和（hb 53%）。双方改用内置 Zeze.Game.Rank 模块（128 分片）
  后同日 A/B：K=20 **36,660 vs 36,699 txn/s 同载荷持平**（双方均空闲，节奏档口径）；
  **K 梯度容量档案见 L 节**——master 平台 ≈12.2 万 txn/s、v1.6.3 ≈9 万（容量代差 ≈1.3×，
  K=80 同点交付 1.39× 且 loot p50 差 4 倍）。per-op 分位（K=20）：loot p95 602µs vs 4.2ms
  ≈7×、好友查询 p95 565µs vs 1.7ms——重试路径改善在分片冲突形态下仍有代差但不再灾难化。
- E2 RocksDB 哨兵 0.38× 非回归：主库 sync 写（`95d511254`）兑现 CheckpointWhenCommit
  掉电承诺，fsync 代价的自觉交换；按表分流**暂缓**（RocksDB 后端非必选、checkpoint
  非热路径，收益场景太窄）。
- G 缓存压力 +24%：MemTrans 快照 CopyIf 收编。

## 修改记录

- **重试日志限频**：perform 每次重试的逐笔 INFO 改 trace+限频——B ×10 主因。
- **Changes 容器惰性化**：无监听者事务免空容器分配，每事务 -33%（1.4KB→~1KB）。
- **MemTrans 快照 CopyIf 收编**：驱逐冲刷路径双重拷贝消除（G +24%）。
- **RRS 非公平锁**（`07171e5a7`）：公平锁引入无论证；改后脏读 +91%、B +35%、全线赢或平。
  flusher 饿死风险=统计性安全+观测兜底：锁等待埋点（commitLockWait/flushLockWait 聚合、
  RrsWaitLock 慢事务时间线、FlushSet 告警 `zeze.rrsFlushLockWaitWarnMs`）；K 洪水探针
  实测无饿死迹象（flushLockWait 均值 143µs、单次最大 200ms、告警零触发）；低并发与
  公平无显著差异。
- **编组收集 TreeMap→ArrayList：尝试后回滚**：小事务 A/B +18%~+47% 为假收益（A/B 未
  交替顺序+空事务放大）；实际负载编组数双峰，保序插入乱序到达 O(n²)；大编组重复场景
  TreeMap 近免费（键=RRS id 而非记录，10万记录 2 编组=2 Entry）。**TreeMap 是唯一无
  坏形态的版本，保留。**
- 挂起候选：纯读事务免编组合并/免重登记（协议语义需论证）；Fresh 队列（等业务同 key
  冲突量级）。

- **GameSim 决策快照 holder 每轮重置**：重试轮走放弃路径时上轮残留幻影记账
  （probe-k20 全局恰差武器价 100、v1.6.3 回植差护甲 80/武器 100，逐角色对账网两代同源实锤）。
- **GameSim 三项口径修正**（用户三连指正）：getOrAdd 收敛建点（九处 get+守卫，心跳缺行
  不再静默重建掩盖踢线）；排行改内置 Zeze.Game.Rank 模块（手搓单记录榜的 loot p95 13.7ms
  证伪为伪靶，分片后 602µs；Rank.create 是 redirect 子类裸环境写不进表——同包基类直构）；
  冒险权重 30→10 降 loot 频率（怪血×2 伪杠杆闭环自平衡仅 -37%；权重正杠杆 -49%，占比
  68.8%→44.3%）。跨代结论随之改写为 1.00×。

- **GameSim 外部副作用记账改 whileCommit**（用户指正，替代 holder 首句重置过渡方案）：
  影子账本/计数/邮箱通知/镜像更新登记在事务体变更点，提交点执行——框架原生保证最终轮语义。
  验证：46,228 txn/s 中位与基线同噪声带，五守恒律+逐角色对账全绿。

## 相对结论（2026-09 起，比值非绝对值）

- 强冲突（全部事务互踩同一记录）吞吐约为单线程的 1/6~1/5：乐观锁重做经记录锁串行化，
  park/unpark 交接是主要成本；重试路径日志已限频（曾因逐笔 info 日志损失一个数量级）。
- checkpoint flush：`MultiThreadMerge` 约为 `SingleThread` 的 8~10 倍（rec/s）；Memory 库 flush
  与 RocksDB flush 相比约 4~5 倍。
- 线程池形态：**JDK21 固定虚拟线程池的过程派发吞吐不低于平台线程池**（同负载 +20~30%）；
  早期"虚拟线程 15w/s 远低于 Async"不复现。
- 空过程与单字段读改写事务成本约 1:1.5~2.5；容器 bean 事务约为单字段事务的 2~3 倍
  （delta-log 与提交快照维护）。
- 无监听者普通事务每笔分配约 1KB 量级（曾 ~1.4KB，Changes 惰性化后 -33%）。

## 历史参考（参数与当前基准文件已漂移，仅存档）

单线程 149w/s；强冲突 25w/s；一般并发 114w/s；GlobalAsync >50w/s；Global 虚拟线程 >15w/s；
GlobalWithRaft 虚拟线程 5w/s（口传数字，未落成基准）。
