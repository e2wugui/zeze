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
gradlew.bat :ZezeJavaTest:benchGame -PbenchJvmExtra="-Dgame.slowLogMs=5" :: L 诊断旋钮（慢op时间线；反事实: -Dgame.checkpointPeriodMs/-Dgame.fanoutOff。默认全关=基线口径。核心侧 checkpoint 轮 trace 系一次性诊断已回退，复活方案见裁决节）
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
heartbeat 尾延迟**已裁决为周期 checkpoint 轮所致（双证，见下节）**；所有 op 类型 max 同落
200-294ms 一档（同一全局停顿）。节奏档吞吐不付这个代价（offered 固定），代价全在尾延迟。

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

### checkpoint 停顿裁决（2026-10-12，双证收口）

hb 尾延迟元凶=**周期 checkpoint 轮（30s 墙钟）**，mailFanout 无罪。诊断工具=休眠旋钮
（慢 op 时间线 `game.slowLogMs`+checkpoint 轮 trace `zeze.checkpointTrace`，与
ZezeJavaTest/log/zeze.log 毫秒时间戳同流相位相关；反事实旋钮 `game.checkpointPeriodMs`/
`game.fanoutOff`，默认全关=基线）。裁决收口后**核心侧 trace 已随 P1 一并回退**
（复活方案：Checkpoint.run 轮起止打点 + flushInternal 阶段计时，均在 `zeze.checkpointTrace`
属性门后）；GameSim 侧旋钮保留（后续循环复用）。

- **时间线证**：84% 慢 op（>5ms）落在轮起点 ±2s 内；轮近场 hb p50=123ms vs 远场 11ms
  （10×）；轮时长 101-210ms 与尾深同量级。
- **反事实证**（周期拉到 10 分钟，测量段零周期轮）：hb p99 34.5→0.76ms、max 294→18.5ms、
  测量窗慢 op 22.6k→2.4k；fanout 对齐率仅 175/60k（无罪释放，反事实 B 不需要）。
- **附带发现**：①免冲刷下登录潮窗脏集无人收割，慢 op 反增 10×（57.5k 全在预热窗）——
  checkpoint 是必要周期成本，优化方向是"降轮的代价"不是"废轮"；②周期轮 leftoverRrs
  逐轮走高（13→40），flush 追不上再脏化——**首轮优化靶=flush 路径（E/E2/K 隔离环境
  优化，L K=80 同日 A/B 主判）**。

**flush 路径解剖（2026-10-12，P1 前置诊断）**：巨型编组是停顿的物质基础——每轮 topSizes
≈[35k, 5000, …]：一个 ~3.5 万记录的 RRS（tMail 24.7k + tRole/tSession 各 5k + trank 256；
全库连通分量：rank 分片记录与 role 同事务共写，传递闭包吞并全世界），占脏记录 80%+ 且逐轮
增长。FlushSet 一组一锁横跨整个存储事务：锁内 encode0 23-153ms（**占 60-65%**）+ 写主库/
本地 Rocks 镜像 42-67ms（串行，镜像事务非线程安全）；MultiThreadMerge 的组间并行对单编组
无效。提交侧 `tryUpdateAndCheckpoint` **读写事务都全量锁**（"读写都需要收集"）——
friendQuery（40% 操作）纯读也在轮内排队。Memory 库部署仍无条件写本地 Rocks 镜像
（Application 有数据库即开 `zeze_cache_<serverId>`）。诊断工具=checkpoint trace 扩展
（`zeze.checkpointTrace`：轮起点 sets/records/topSizes/largestTables + 组级 encodeMs/writeMs，
一次性使用后随 P1 回退撤除）。

### 容量档 A/A 噪声带（2026-10-12，同日背靠背 master×2）

- **K=80 主判档**：run-to-run 差 1.4%、轮内 cv 3.5-4.3% → 显著性门槛=3× 轮内噪声 ≈**13%**。
- **K=160 信息档**：轮内 cv 21-31%（rounds 86k~191k）、首窗口系统性偏低（过载踢线风暴
  自适应），run-to-run 5.2%——超 5% 红线，**永不做门禁**；当日中位 172-182k 较 10-12
  档案（122,487）跨日漂移 +45%，"平台"量级在深度过载区跨日不可比。
- 当日 K=80 中位 125-127k（较档案 119,221 跨日 +5%）；hb p99 99-139ms——checkpoint
  尾在容量档同样在场。

### P2 纯读事务免编组（2026-10-12 落地，readExempt）

**改动**：无脏写、无 onz、无 CheckpointWhenCommit 的提交不再触碰 rrs 锁/合并/重注册
（`zeze.rrs.readExempt`；**单进程形态（SM=disable）自动启用**，多进程保守走原路径，
属性可显式强开）。协议论证：读者无落库分组需求——读值稳定性由记录锁保证（flush encode
走 volatile 快照）；被读合并的集合从未被同一事务共写，独立 flush 不破坏写原子性；脏集
注册由写者完成，读者重注册是幂等冗余；空事务先例（不取提交许可证）。全量功能测试
双态（开/关）绿。

**证据（同日，同二进制切属性）**：
- L K=80 ABBA 四跑：**118,952→139,873（+17.6%，过 13% 门禁）**，最差 on 跑仍高于最好
  off 跑 10.8%；hb p99 211/162ms→17.8/10.5ms、friendQuery p99 44.5/55.3ms→0.15/0.82ms
  （读彻底离场）、loot p50 7.0/17.8ms→0.03ms——**K=80 去饱和**。
- H 脏读档 46.98万→98.72万（**+110%**），脏读≈干净读（987k vs 980k）——**3.7-4.1×
  结构代差抹平**（H 场景 SM 启用，走属性强开；默认值不变）。
- K=20 护栏：吞吐 36,871→36,883 持平（offered 限制）、hb p99 2.21→0.02ms。

**新容量曲线（readExempt 默认生效后重立基线）**：K=20=36,970（cv 0.44%，尾部塌方：
hbP99 0.02/fqP99 0.07/lootP95 0.15ms）；K=80=138,501（不饱和）；K=160=168,858~193,550
（loot p50 0.05ms，不饱和）；**K=320=272,372~308,946（loot p50 55-61ms=饱和，轮内峰值
40.2 万）**。膝点从 (40,80] 移到 **(160,320]**，平台 **≥30 万 txn/s ≈ 旧平台 12.2 万的
2.2-2.6×**。残留：K=320 下 hb p99 17-19ms（写者仍停顿，Fresh 队列靶）、首窗口低垂
（登录潮形态不变）。

**机制**：旧协议里纯读事务全量锁+合并+重注册——读边（好友查询，40% 操作）是巨型编组
的耦合生长引擎，且读者重注册让脏集永不离开 flush 地图。豁免后：读离场（排队人口 -40%）、
编组停止经读边生长、H 的脏读记账结构成本消失。

### C3 容量狩猎收口（2026-10-12 深夜轮）：引擎退出瓶颈位

- **K=320 稳态**（3 预热窗）：中位 **35.4 万 txn/s，loot p50=0.03ms、hb p99=0.02ms——
  负载态延迟干净，不饱和**。早前 K=320 的"饱和"（loot p50 55-61ms）系预热瞬态。
- **模拟器自限实锤**：每窗口 op 计数随 K 升反降（K80=103.6万→K160=72.7万→K320=66.4万）
  ——玩家循环与房间 tick 的固有时间成为限速器，K 压缩不再线性加压。
- **15k 玩家探针（K=160，一次性诊断非基线）**：交付 ~29.7 万不随玩家数线性（模拟器
  瓶颈）；hb/fq 全干净；**loot p99=2152ms 系 2g 堆 GC 尾（4g→122ms，benchHeap 教训：
  15k 玩家须 4g）**；残余 loot p99 122ms=checkpoint 轮写者编组停顿——**session 与 role
  预热后解耦**（login 仅预热窗耦合，此后 hb 独占 session 集），hb 因此免疫轮停顿，
  仅 loot（role+rank 分片）承担——Fresh 队列的余靶，当前形态只损延迟不损容量。
- **结论：L 形态容量平台 12.2万 → ≥35万（≈2.9×），引擎退出可测瓶颈位**。继续加压需
  改模拟器承载（加玩家/改权重=重立基线），或转战其他场景。

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
- **锁内并行 encode（P1）：尝试后回退（2026-10-12，代码全撤含旋钮）**：≥1024 记录的组在
  锁内用 commonPool 并行 encode0——巨组 encode 90ms→4ms、锁窗 ~140→30ms。但 K=80 ABBA
  （同二进制切 `zeze.encodeParallelMin`，4 跑抵消顺序）：**吞吐 -3.3%、hb p99
  100/162ms→280/315ms（2-3× 恶化）**——饱和区 15 线程 CPU 突发抢占提交线程，锁窗口缩短
  ≠停顿减轻；K=20 护栏混合（p95/中段 p99 变好、极端尾变差）；E SingleThread 两序一致
  +11%、E MTM/E2/K 噪声内无回归。教训：锁窗时长与 CPU 集中度是两个独立的停顿源，
  饱和态必须同时治——继续缩窗收益有限，指向 Fresh 队列（提交者根本不进入等待）。
- **纯读事务免编组（P2，2026-10-12）**：无脏写提交不再触碰 rrs 锁/合并/重注册；单进程
  （SM=disable）自动启用。L K=80 **+17.6%**（ABBA 过门禁）、平台 12.2万→**≥30 万
  （2.2-2.6×）**、膝点 (40,80]→(160,320]；H 脏读 **+110%**（脏≈干净，代差抹平）；
  K=20 hb p99 2.21→0.02ms。机制：读边是编组耦合的生长引擎+读者重注册让脏集永驻
  flush 地图（证据链见 L 节 P2 小节）。
- 挂起候选（更新 2026-10-12 深夜轮）：**Fresh 队列/追加不等待**——余靶收窄为 15k 玩家
  形态的 loot p99（122ms，写者编组轮停顿；5k 形态无容量收益、延迟亦干净），须写密集
  载荷才能裁决；**readExempt 谓词细化**——SM 启用但实际单进程的形态（如 H harness）
  默认仍背脏读税，表级全局共享判定需 storage/GCM 层考古后论证；跨代表 L 行刷新
  （v1.6.3 无 readExempt，容量代差待回植分支复测）；有界 worker 并行 encode（低优先）。


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
