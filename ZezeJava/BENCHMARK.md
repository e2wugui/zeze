# ZezeJava 核心基准档案（BENCHMARK）

> 单一真相源：跑法、判定协议、跨代档案与结论都在本文件。docs 站点《性能调优》仅留摘要链接。
> 绝对数与机器强相关：引用必须带本文件脚注，对比证据只认同机同配置。

## 跑法

```bat
:: ZezeJava 目录下
gradlew.bat :ZezeJavaTest:benchCore [-PbenchTag=名]           :: 核心宏观基准（多轮+JSON 留档）
gradlew.bat :ZezeJavaTest:benchCorePlatform [-PbenchTag=名]   :: F 调度场景平台线程池对照
gradlew.bat :ZezeJavaTest:benchGame [-PbenchTag=名]           :: L 游戏模拟基准（独立车道）
gradlew.bat :ZezeJavaTest:benchCore -PbenchHeap=4g ...        :: 跨版本对比公平堆（默认 2g，对齐历史存档真实口径）
gradlew.bat :ZezeJavaTest:benchCore -PbenchJvmExtra="-XX:StartFlightRecording=..." :: JFR/系统属性等诊断透传
gradlew.bat :ZezeJavaTest:benchGame -PgamePlayers=5000 -PgameMinutes=10 :: L 旋钮（负载=在线数；gameK 默认4封顶一般不动）
gradlew.bat :ZezeJavaTest:benchGame -PbenchJvmExtra="-Dgame.slowLogMs=5" :: L 诊断旋钮（慢op时间线；反事实 checkpointPeriodMs/fanoutOff；默认关=基线）
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
| K 洪水下 checkpoint 竞争 | KCheckpointUnderCommitFlood | 派发池提交洪水与并发 flush 争 RRS 锁（flusher 饿死探针） |
| L MMO-lite 游戏模拟 | GameSim.LMmoGameSim | 真实游戏逻辑全模拟（@Tag("game") 独立车道 benchGame） |

微基准（src-jmh）：Serialize / Collections / Conflict / ReadOnly 四族。

## History 关闭路径（2026-10-15）

同机同日 A/B，Windows 11 / Corretto 21.0.5 / 2g 堆 / 单线程，复用
`Benchmark.CollectionsBench.containerPutEntry`，每 fork 预热 3×1s、测量 5×1s，
共 2 fork，`-prof gc`。两侧均使用默认关闭的 History、无 ChangeListener、Memory 库。

```bat
gradlew.bat :ZezeJavaTest:jmh "-PjmhFilter=Benchmark.CollectionsBench.containerPutEntry" "-PjmhFork=2" "-PjmhWi=3" "-PjmhI=5" "-PjmhW=1s" "-PjmhR=1s" "-PjmhExtraArgs=-prof gc" "-PbenchTag=history-off-before"
:: 修改后相同命令，仅 benchTag 改为 history-off-after
```

| 改动 | 修改前 → 修改后 | 判定 |
|---|---|---|
| 无变更消费者时跳过 Changes 创建与后续收集遍历 | 3009.04 → 2912.65 B/op（-96.39 B/op，-3.20%） | 保留；分配降幅超过两侧最大样本标准差的 3 倍（最大约 16.85 B/op） |
| 同一组的吞吐观测 | 905,930 → 1,236,981 ops/s | 修改前波动较大，未通过 3×stdev 判据，不认定吞吐增益 |

分配量为整个容器事务的成本，不能解释为关闭 History 仍额外消耗约 2913 B/op。
这次同时使 HistoryModule / tHistory 按配置初始化、PendingGidLedger 首次使用才创建，
消除了默认关闭时对应的固定资源；上述热路径基准不量化启动耗时或固定内存节省。
原始结果（gitignored）：`ZezeJavaTest/bench-results/jmh-history-off-before.json` 与
`ZezeJavaTest/bench-results/jmh-history-off-after.json`。

后续拆分监听器与 History 收集：关闭 History 时不再创建 `HistoryChangesCollector`，
有监听器时直接执行共享变更采集。相同命令同日再次 A/B（tag 为
`history-collector-before` / `history-collector-after`）：2912.31 → 2880.72 B/op，
再减少 31.59 B/op（1.08%），超过两侧最大样本标准差的 3 倍（最大约 1.94 B/op），保留。
本组吞吐仍波动较大，不认定吞吐增益。原始 JSON 同样保存于
`ZezeJavaTest/bench-results/jmh-<tag>.json`。

完成 IHistory 接口拆分及重构后，使用相同参数再次验证关闭路径（tag：
`history-plugin-refactor-after`）：2880.72 → 2880.83 B/op，差异约 0.11 B/op，
落在样本波动内，未观察到本轮接口拆分增加事务分配。该数值仍是整个容器事务的
分配量，不是关闭 History 的额外成本；本次不据此认定吞吐变化。

## L 场景设计（LMmoGameSim，ZezeJavaTest/src/GameSim/）

MMO-lite 游戏模拟世界：真实游戏逻辑承载访问模式，核心机制（编组/脏读记账/分片冲突/
大事务/纯读提交簿记）在真实业务形态下暴露。

- **架构铁律**：房间=内存态战斗状态机（tick/伤害公式/掷骰/respawn 全不经 Zeze），只向大厅
  投递结果事务；单写者亲和（本角色记录由本人虚拟线程写，交易例外：受要约方一笔写双方）；
  心跳超时踢线是唯一 DaemonTimer 用法。
- **七系统**：角色生命周期（login/心跳 15s/logout）、背包物品、PVE 战斗、P2P 交易（双边
  原子换）、双排行（**内置 Zeze.Game.Rank 128 分片**——勿手搓单记录榜制造热 key）、邮件
  （系统 fan-out 大事务）、好友在线查询（**纯读事务**）。砍：聊天/公会/商城。
- **驱动与档位**：每玩家一虚拟线程（事务串行：同步 call 返回才进下一步，单用户不存在
  自我并发）；per-player RNG 只在决策点（事务外）推进。**K=10（2026-10-14 终版）**：
  当日两段演进 K20→4（废弃"单用户高频→redo 放大"假设的梯度容量档）→4→10（假设实证
  解除：K=14@p20k 人均 5.09 txn/s、loot p95 48µs 反降——redo 由全局密度×共享行提交率
  决定，主源 rank 分片行已 512 化缓解，p40000 +15% 即其证据）。容量仍以在线数为主
  旋钮（-PgamePlayers），K 调单玩家操作强度。
- **指标**：总吞吐中位（同日 A/B、3×噪声协议）+ 每 op 延迟分位（JSON `op.<name>.p50/p95/p99_us`）。
- **窗口制**：世界连续运行，窗口=10 游戏分钟（预热 1+测量 3 取中位）；操作归属以发起时
  窗口为准，翻转后 2s 宽限收割。已知瑕疵：宽限渗漏系统性高估 ~1/15（恒定，不碍同日 A/B）。
- **守恒验证**：停机后单只读事务对账——货币/物品守恒、会话无泄漏、榜单与重算一致、邮件
  inserted−claimed=存量；外部副作用一律 whileCommit 登记在变更点。
- **配置口径**：裸 Config 程序化构造（Config.load 会点亮 Onz）；Memory 库、单进程本地锁、
  缓存 10 万、checkpoint 30s 墙钟。

## L 档案（2026-10-12）

### 容量基线（pre-P2 口径；**2026-10-14 起本节 K 梯度方法已废弃**——K 只调单玩家节奏且
默认封顶 4，容量/平台改用大在线数，下表留作历史档案与跨代对比参考）

节奏档（K=20，5000 人）：master 36,660 txn/s 中位（cv ~1.4%）；mailFanout p95≈66ms
（5000 行大事务）、loot p95=602µs/p99=2.6ms、心跳尾延迟归因周期 checkpoint 轮（见下节）。

容量档（K 梯度，同日顺序跑）：K=20→40 线性 1.98×（两档均空闲），膝点 K=40~80：

| K | master 中位 | 心跳达成 | loot p50 | v1.6.3 中位 | v1.6.3 心跳 |
|---:|---:|---:|---:|---:|---:|
| 20 | 36,589 | 95.9% | 27µs | 36,988 | 96.1% |
| 40 | 72,310 | 99.5% | 45µs | 73,336 | 101.2% |
| 80 | 119,221 | 90.9% | 24.9ms | 86,088 | **65.3%** |
| 160 | **122,487** | **61.5%** | 50ms | — | — |

master 平台 ≈12.2 万 txn/s；v1.6.3 平台 ≈9 万（容量代差 ≈1.3×、K=80 同点交付 1.39×）。
K=20 的 1.00× 是"同 offered 双方足额交付"口径：延迟对比有效、容量对比无效，容量口径须 K≥80。

### 容量阶梯（players 口径，2026-10-14，K=4 固定节奏）

同日顺序跑（-PbenchHeap=4g 统一口径，bench-results/p*/ 留档）：

| players | 中位 txn/s | 相对上轮 | 人均 txn/s | cv | loot p50/p95 | 守恒 |
|---:|---:|---:|---:|---:|---|---|
| 5,000 | 7,165 | — | 1.433 | 0.28% | 33µs/~120µs | 绿 |
| 10,000 | 14,327 | 2.00× | 1.433 | 0.25% | 51µs/~150µs | 绿 |
| 20,000 | 28,500 | 1.99× | 1.425 | 0.18% | 60µs/~178µs | 绿 |
| 40,000 | 48,175 | 1.69× | 1.204 | ~5% | 102µs/269ms(r3) | **红**（见下） |
| 80,000 | 38,810 | **0.81×** | 0.485 | 23.5% | 132µs/25.2s(r3) | 绿 |

- **线性区 ≤20k**（人均 1.43 恒定）；**膝点 20k~40k**；**饱和交付 ≈48-49k txn/s**（p40000 峰值轮
  49,209 与 p80000 首轮 49,331 一致）；**p80000 过载崩塌**：轮内逐轮 -21%/-30%、loot p95 指数
  爆炸（128ms→2s→25.2s）——脏集膨胀×checkpoint 巨轮的恶性循环，压得越多交付越少。
- **额定建议**：单机（8 核/Memory 库）额定 ≤20,000 在线，峰值 30-40k。
- **p40000 守恒失败一次（立案）**：`货币守恒 ledger=249,807,748 role+mail=499,615,496`——
  **精确 2 倍**、逐角色网 #0 全过（每角色 DB==roleMoneyOf）、p80000 反而全绿 → 非确定性
  口径 bug 排除，**过载区竞态一次**（形态与 2026-10-12 前"全局差 100 罕见竞态"不同族）。
  嫌疑面：注入流（建号补贴/fanout whileCommit）在过载 redo 下漏记 moneyNet 但双侧 DB 完整，
  或对账读侧异常。复现抓手：p40000 重跑 + ledger/mail 侧拆解打点（moneyNet 分流计数）。

### redo 两源消除：rank 512 分片 + friendQuery 脏读（2026-10-14）

针对上表的 redo 主源定向修复：**①伤害榜分片 128→512**（`GameWorld` 构造
`rank.setFuncConcurrentLevel(→512)`；loot 每笔必写榜，players/128 在数万在线时同分片
数百人共写一行；concurrentLevel 变更=旧榜失效需清重建，GameSim 空库起跑无迁移面）；
**②friendQuery 好友 session 改 `selectDirty`**（显示性数据容忍稍旧值，脏读不注册读集，
消除"20 好友×心跳 3.75s"的常态冲突面；先例 Zeze.Game.Online 在线判定同款）。

| players | 前 median | 后 median | Δ | loot p50 | loot p95 | fq p95 | 守恒 |
|---:|---:|---:|---:|---|---|---|---|
| 20,000 | 28,500 | 28,578 | +0.3% | 60→34µs | 178→108µs | 91→**41µs** | 绿 |
| 40,000 | 48,175(红) | **55,395** | **+15%** | 102→34µs | 269ms→111µs | →35µs | **绿** |
| 80,000 | 38,810(崩塌) | 49,274(崩塌) | 首轮 49.3k→61.1k | 132→54µs | 崩塌同形态 | →63µs | fastfail |

- **线性区 20k→40k**（p40000 扩展比 1.69×→**1.94×**），**稳定平台 48-49k→55k+**
  （p40000 cv3.5% 稳定交付；p80000 首轮峰值 61,137 后逐轮崩塌——过载区死法两形态：
  守恒红（前）与 fastfail 543543（后），均已表征，不再深挖）。
- p40000 上批的守恒 2× 竞态本轮未复现（redo 风暴面缩小的方向一致；单轮证据，立案不销）。
- 留档 bench-results/p20000-r512、p40000-r512、p80000-r512。

### mailFanout 事务拆分（2026-10-14）

单大事务（一事务插全服邮件行）违反事务划分原则：持有全表读写集数百 ms，与全服
mailClaim/mailSend 互撞，且是 checkpoint 巨型 RRS 的物质基础（flush 解剖：tMail 24.7k
行占脏记录 80%+）。改为**每玩家一个微事务**（插自己的邮件行+自己的账本/通知同事务，
`whileCommit` 逐事务等价，部分失败只计 errors 总账）。

- p20000 实测：吞吐 28,578→28,345（-0.8%，噪声内）、守恒绿、**fanout 全程 355ms→263ms
  （-26%：2 万次微事务提交总开销 < 单大事务的全表读写集校验+RRS 编组）**。
- **统计口径同步修正**：record 移入循环每微事务记一次——mailFanout 计数=真实事务数
  （20000/窗，吞吐序列自动含微事务 ~133 txn/s@p20k），延迟分位与其他 op 统一为单事务
  口径（p50 9µs/p95 22µs/max 2.2ms）；全程耗时由 TimelineLog 相位打点承载。
  **历史档案的 mailFanout 全程口径（66ms/355ms）与新版不可直比。**
- 留档 bench-results/p20000-fanout-split（拆分）、p20000-fanout-precise（统计修正）。

### checkpoint 周期反事实：单峰曲线，高密度（p40k@K10）最优点=60s（2026-10-15）

假设"loot×rank 传递闭包使 RRS 膨胀，缩短 checkpoint 周期可切碎连通、缓解 RRS 大小"——
**实验否定**（-PgameCheckpointPeriodMs 五档同日顺序，bench-results/p40000-cp*/）：

| cp 周期 | median txn/s | 相对 | loot p50/p95 | cv |
|---:|---:|---:|---|---:|
| 5s | 99,003 | -16.8% | 36/**19-110ms**（p95 爆炸） | 0.4% |
| 10s | 103,993 | -12.6% | 35/139-274µs | 1.9% |
| 30s（默认） | 118,998 | -8.5% | 32/74-80µs | 2.4% |
| **60s** | **129,296** | **峰** | **31/62-67µs** | 3.6% |
| 120s | 117,941 | -8.8% | 34/71-87µs（hb p95 r1 尖峰 47µs） | 0.9% |

机制（单峰两侧）：**短侧**——flush 锁窗口按频率收费（FlushSet 一组一锁横跨存储事务、提交侧
读写事务全量锁），且 K10 密度下传递闭包稀释跟不上（5s 内每分片仍被写数百次），RRS 缩小收益
< 锁频率代价，5s 档 p95 19-110ms 是"高频中等停顿铺满时间轴"形态；**长侧**——RRS 单轮变大
（两周期脏集翻倍），flush 编组时间变长开始可感（120s 档 hb p95 出现 47µs 尖峰、吞吐掉回
30s 水平）。60s 档 cv 3.6% 系窗口墙钟与 checkpoint 同拍的相位交替（含 1~2 轮 flush 的
窗口轮替）。**最优周期随密度左移**：K4 低密度时代 10min 档 p99 大好，p40k@K10 峰在 60s——
密度越高 RRS 增长越快、平衡点越短。RRS 膨胀的正解仍是减少共写面（分片化/拆事务），不是调
flush 勤惰；高密度档建议 -PgameCheckpointPeriodMs=60000（+8.7% vs 默认 30s）。

**副产物（K=10 口径阶梯第二数据点）**：p40000@K10 默认 30s 口径 118,998（+17% vs K14@20k
的 101,891），人均 3.58→2.97（-17%）——**K10 口径膝点在 20k~40k 之间**，CPU 型饱和逼近
（~6.5/8 核）。p80k@K10 待跑（预期过载形态）。

### checkpoint 停顿裁决

hb 尾延迟元凶=**周期 checkpoint 轮（30s 墙钟）**，mailFanout 无罪。双证：

- **时间线证**：84% 慢 op（>5ms）落在轮起点 ±2s；轮近场 hb p50=123ms vs 远场 11ms。
- **反事实证**（周期拉到 10 分钟）：hb p99 34.5→0.76ms、max 294→18.5ms、慢 op 22.6k→2.4k；
  fanout 对齐率仅 175/60k（无罪）。
- **附带发现**：免冲刷下登录潮脏集无人收割、慢 op 反增 10×——checkpoint 是必要周期成本，
  优化方向是"降轮的代价"；leftoverRrs 逐轮走高（13→40）→ 首轮优化靶=flush 路径。
- **工具**：GameSim 休眠旋钮（slowLogMs / checkpointPeriodMs / fanoutOff，默认全关=基线）；
  核心侧 trace 已随 P1 回退。

### flush 路径解剖（P1/P2 前置诊断）

巨型编组是停顿的物质基础：每轮 topSizes≈[35k, 5000, …]——一个 ~3.5 万记录的 RRS
（tMail 24.7k + tRole/tSession 各 5k + trank 256；rank 分片与 role 同事务共写，传递闭包
吞并全世界），占脏记录 80%+ 且逐轮增长。FlushSet 一组一锁横跨存储事务：锁内 encode0
占 60-65%（23-153ms）+ 写主库/本地 Rocks 镜像 42-67ms（串行）。提交侧读写事务都全量锁
（"读写都需要收集"）——friendQuery（40% 操作）纯读也在轮内排队；Memory 库部署仍无条件
写本地 Rocks 镜像。

### 容量档 A/A 噪声带（同日背靠背 master×2）

- **K=80 主判档**：run-to-run 1.4%、轮内 cv 3.5-4.3% → 显著性门槛=**13%**。
- **K=160 信息档**：轮内 cv 21-31%、跨日漂移 +45%——**永不做门禁**。

### P2 纯读事务免编组（readFastPath）

**改动**：无脏写、无未注册脏、无 onz 的提交不再触碰 rrs 锁/合并/重注册
（`zeze.rrs.readFastPath` **默认开、全形态含 GCM**；=false 逃生门）。

- 安全性：读者无落库分组需求（读值稳定性由记录锁保证、脏集注册由写者完成、读者重注册
  是冗余）；GCM 的先落库后降级/整组刷原子性/Invalid+dirty 恢复均由写者注册的组独立满足。
- 唯一例外：oldTable 倒库在读路径标脏且孤立未注册，其落库注册只在老路径提交——
  `hasUnregisteredDirty`（脏&&孤立组签名）封洞，回归测试 TestRrsReadFastPathOldTableMigration。

**证据（同日同二进制切属性）**：

- L K=80 ABBA 四跑 **+17.6%**（11.9万→14.0万，过 13% 门禁）；hb p99 211/162→17.8/10.5ms、
  friendQuery p99 44.5/55.3→0.15/0.82ms（读离场）、loot p50 →0.03ms——K=80 去饱和。
- H 脏读档 **+110%**（47.0万→98.7万），脏读≈干净读——3.7-4.1× 结构代差抹平。
- K=20 护栏持平（offered 限制）、hb p99 2.21→0.02ms。

**容量（默认生效后）**：膝点 (40,80]→**(160,320]**，平台 12.2万→**≥30 万（2.2-2.6×）**，
K=320 轮内峰值 40.2 万。

**机制**：读边（friendQuery，40% 操作）是巨型编组的耦合生长引擎，读者重注册让脏集永驻
flush 地图；豁免后读离场（排队人口 -40%）、编组停止经读边生长。

### C3 容量收口（深夜轮）

- K=320 稳态（3 预热窗）中位 **35.4 万**、loot p50=0.03ms、hb p99=0.02ms——负载态延迟干净，
  不饱和；早前 K=320 的"饱和"（loot p50 55-61ms）系预热瞬态。
- **模拟器自限**：窗口 op 计数随 K 升反降（K80 103.6万→K320 66.4万）——玩家循环与房间
  tick 的固有时间成为限速器，继续加压需改模拟器承载（=重立基线）。
- 15k 玩家探针（K=160，诊断非基线）：交付 ~29.7 万不随玩家数线性；loot p99 2152ms 系 2g
  堆 GC 尾（4g→122ms，benchHeap 教训）；残余 122ms=checkpoint 轮写者停顿——session 与
  role 预热后解耦，hb 免疫、仅 loot（role+rank 分片）承担，只损延迟不损容量。
- **结论：容量平台 12.2万 → ≥35万（≈2.9×），引擎退出可测瓶颈位。**

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
11. Windows 下 String.format 的 %n 是回车换行，会破坏按字符数裁剪的手拼 JSON（用显式 \n）；
    轮询 CompletableFuture.get 的 TimeoutException 是设计内节奏，勿计错误。

## 跨代档案（v1.6.3 → 2.x）

- 机器脚注：16C/JDK21/Windows 同机；两列堆口径不一致为已知瑕疵（v1.6.3=回植分支显式 4g，
  2026-10-11 列实为 2g——`-PbenchHeap` 当时被 withType(Test) 兜底覆盖，当日已修复）——严格
  4g/4g 复测见台式机轮。倍数看量级不看小数。
- v1.6.3=回植分支 `bench/backport-v1.6.3`（TaskSpec→Task.runUnsafe 等价改写）；2026-10-11=
  当日 master（含 RRS 非公平锁）。单位 ops/s（中位数），括号内为相对 v1.6.3 倍数。
- L 行为 K=80 容量档同点交付（P2 前口径；P2 后 master 默认容量 ≥35 万，详见 L 档案）。
- 2026-09-29 final（5a92cc111）与本轮 HEAD 的同日 A/B 终裁：**592 个审计修复提交无性能回归**。

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
| H 只读并发·脏记录档 | 37.4万 | 57.9万（1.55×） |
| H 只读并发·干净记录档 | 151.9万 | 96.8万（0.64×） |
| I MMO-lite 混合画像 | 31.6万 | 34.0万（1.08×） |
| J 网络 Rpc 回环 256B | 20.3万 | 20.5万（1.01×） |
| L MMO-lite 游戏模拟（容量档） | 8.6万 | 11.9万（1.39×） |

### 台式机复测轮（严格 4g/4g，2026-10-12）

机器：i7-12700（8P+4E、20 逻辑核）/64GB/Windows；v1.6.3=回植 71d13076c、master=1a9d6d29d
（含 P2 readFastPath）。M→V→M→V 抵消顺序、core 双侧双跑（r1/r2）；**本机 run-to-run
±10~25%**（大小核调度敏感）——单场景中位变化 <25% 一律按噪声看。「笔记本」列=上表倍数。

| 场景 | v1.6.3 (r1/r2) | master (r1/r2) | 台式机倍数 | 笔记本倍数 |
|---|---:|---:|---:|---:|
| A 单线程简单事务 | 256.7/195.5 | 252.2/229.6 | 0.89~1.29 | 1.30× |
| B 强冲突（480线程踩1key） | 3.2/2.8 | 61.2/46.4 | **14.7~21.8×** | 23.5× |
| C 一般并发（5000key 派发） | 157.3/164.5 | 140.6/122.3 | 0.74~0.89 | 1.01× |
| D 容器事务 | 87.5/80.1 | 72.6/87.0 | 0.83~1.09 | 1.25× |
| E flush MultiThreadMerge | 85.1/89.3 | 74.4/83.0 | 0.83~0.98 | 1.25× |
| E flush SingleThread | 13.6/12.3 | 10.3/11.5 | 0.76~0.94 | 1.10× |
| E2 RocksDB 哨兵 | 58.0/57.0 | 9.6/9.6 | **0.17×** | 0.38× |
| F 派发池（虚拟线程） | 172.3/157.2 | 131.0/112.7 | 0.65~0.83 | 0.92× |
| F 派发池（平台线程） | 59.5（单跑） | 74.3（单跑） | 1.25×（不门禁） | 0.83× |
| G 缓存压力（miss/装载/驱逐） | 50.2/41.4 | 42.1/44.8 | 0.84~1.08 | 1.37× |
| H 只读并发·脏记录档 | 57.0/58.1 | 95.1/98.2 | **1.64~1.72×** | 1.55× |
| H 只读并发·干净记录档 | 205.6/198.8 | 134.5/141.1 | **0.65~0.71×** | 0.64× |
| I MMO-lite 混合画像 | 38.7/39.2 | 30.7/38.0 | 0.78~0.98 | 1.08× |
| J 网络 Rpc 回环 256B | 44.9/47.6 | 29.1/22.8 | **0.48~0.65**（四跑一致） | 1.01× |
| K 洪水下 checkpoint 竞争 | —（未回植） | 125.4/121.3 | — | — |
| L MMO-lite 游戏模拟（K=80） | 15.1 | 16.3 | 1.08× | 1.39× |

**跨机复现结论**（两机倍数量级一致，跨代事实稳固）：B 强冲突代差（重试日志限频主因）、
H 脏读 1.6~1.7×、H 干净读反向 0.65×（v2 干净读路径相对变慢的结构性事实）、L 容量代差
（P2 后 ≥3×）。

**大小核形态归因**（同日三臂对照：不钉/P钉16/APC16，明细略）：主因=**JVM 按含 E 核的
20 逻辑处理器自动 sizing**（E 核放置本身几乎无责）——大小核机跑 2.x 显式
`-XX:ActiveProcessorCount=<P核线程数>` 即复原，无需钉核。残留例外：C/F 派发形态对核数
反向敏感（16 视角更差，独立于 E 核），F 场景 JMH+线程剖析立案；J 回环低倍数同因（核数
sizing）；E2 0.17× 为本机盘速放大的主库 sync 写代价，非回归。

**L 容量（台式机档）**：K=80 同载荷 1.08×（15.1万 vs 16.3万）但 loot p50 6.2ms vs 21µs
——v1.6.3 已吃力（笔记本同点 94ms 全饱和），master 无感。v1.6.3 膝点 (80,160]、平台
≈15-16万；master K=320 交付 53.1万 且 loot p50 24µs 仍不饱和（模拟器自限形态复现）——
**台式机容量代差 ≥3.3×**（master 地板被模拟器自限遮蔽，未探到）。

## 跨版本结论（v1.6.3 → 2.x）

- **B 强冲突 +20×**：v1.6.3 每次重试逐笔 `logger.info`（×10 主因）+ v2.0 周边改进（×2）。
- **H 只读两档，脏读记账是结构性成本**：读"已提交未冲刷"的脏记录比读干净记录慢 3.7~4.1×
  （改非公平锁前口径；P2 后代差抹平）——读事务提交走 RRS 锁合并+编组重登记，并发锁车队。
  生产缓解：批量事务后 checkpointRun 冲散编组、缩短 CheckpointPeriod。
- 网络 Rpc 回环（J）与 MMO 画像（I）两代持平——net 层与混合事务负载无跨代退化。
- **L 游戏模拟**：手搓单记录榜时代的 1.80× 是口径伪影（单 key 31k puts/s 放大 v1.6.3
  串行化弱点，K=20 即饱和）；双方改用内置 Zeze.Game.Rank（128 分片）后 K=20 同载荷持平
  （36,660 vs 36,699，节奏档口径）。per-op 分位（K=20）：loot p95 602µs vs 4.2ms、好友
  查询 p95 565µs vs 1.7ms——重试路径改善在分片冲突形态下仍有代差但不再灾难化。
- E2 RocksDB 哨兵 0.38× 非回归：主库 sync 写（`95d511254`）兑现 CheckpointWhenCommit
  掉电承诺，fsync 代价的自觉交换；按表分流暂缓（RocksDB 后端非必选、checkpoint 非热路径）。
- G 缓存压力 +24%：MemTrans 快照 CopyIf 收编。

## 修改记录

- **重试日志限频**：perform 每次重试的逐笔 INFO 改 trace+限频——B ×10 主因。
- **Changes 容器惰性化**：无监听者事务免空容器分配，每事务 -33%（1.4KB→~1KB）。
- **MemTrans 快照 CopyIf 收编**：驱逐冲刷路径双重拷贝消除（G +24%）。
- **RRS 非公平锁**（`07171e5a7`）：公平锁引入无论证；改后脏读 +91%、B +35%、全线赢或平。
  flusher 饿死风险=统计性安全+观测兜底（锁等待埋点、FlushSet 告警
  `zeze.rrsFlushLockWaitWarnMs`）；K 洪水探针实测无饿死迹象（均值 143µs、最大 200ms、
  告警零触发）。
- **编组收集 TreeMap→ArrayList：尝试后回滚**：小事务 A/B +18%~+47% 为假收益（A/B 未
  交替+空事务放大）；实际负载编组数双峰，保序插入乱序到达 O(n²)；大编组场景 TreeMap
  近免费（键=RRS id，10万记录 2 编组=2 Entry）。**TreeMap 是唯一无坏形态的版本，保留。**
- **锁内并行 encode（P1）：尝试后回退**：≥1024 记录的组锁内并行 encode0——巨组 encode
  90ms→4ms，但 K=80 ABBA 吞吐 -3.3%、hb p99 恶化 2-3×（饱和区 commonPool CPU 突发抢占
  提交线程）；锁窗缩短≠停顿减轻。教训：锁窗时长与 CPU 集中度是两个独立的停顿源，
  饱和态必须同时治——指向 Fresh 队列（提交者根本不进入等待）。
- **纯读事务免编组（P2）**：无脏写、无未注册脏、无 onz 的提交免 rrs 锁/合并/重注册；
  `zeze.rrs.readFastPath` 默认开全形态（含 GCM，=false 逃生门），oldTable 读路径脏以
  脏&&孤立组签名排除（回归测试 TestRrsReadFastPathOldTableMigration）。L K=80 +17.6%、
  平台 12.2万→≥35万（≈2.9×）、膝点 (40,80]→(160,320]；H 脏读 +110%（脏≈干净代差抹平）；
  K=20 hb p99 2.21→0.02ms。机制：读边是编组耦合的生长引擎+读者重注册让脏集永驻 flush 地图。

### 挂起候选

- **Fresh 队列/追加不等待**：余靶=15k 玩家形态 loot p99 122ms（写者编组轮停顿；5k 形态
  无容量收益、延迟亦干净），须写密集载荷才能裁决。
- **F/C 派发路径跨机缺口**：台式机上 master 派发吞吐相对 v1.6.3 仅 0.5~0.9×（笔记本
  0.92~1.01×），与 E 核无关且核数反向敏感；F 场景 JMH+线程剖析立案。
- **GCM 多进程性能验证**：readFastPath 语义已论证（GCM 对 rrs 的依赖均由写者注册的组
  满足），flush 单元粒度变化未测。
- 有界 worker 并行 encode（低优先）。

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
