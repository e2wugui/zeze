# ZezeJava 核心基准档案（BENCHMARK）

> 单一真相源：跑法、判定协议、四时点档案与结论都在本文件。docs 站点《性能调优》仅留摘要链接。
> 绝对数与机器强相关：引用必须带本文件脚注，对比证据只认同机同配置。

## 跑法

```bat
:: ZezeJava 目录下
gradlew.bat :ZezeJavaTest:benchCore [-PbenchTag=名]           :: 核心宏观基准（多轮+JSON 留档）
gradlew.bat :ZezeJavaTest:benchCorePlatform [-PbenchTag=名]   :: F 调度场景平台线程池对照
gradlew.bat :ZezeJavaTest:benchCore -PbenchHeap=4g ...        :: 跨版本对比公平堆（默认 2g，对齐历史存档真实口径）
gradlew.bat :ZezeJavaTest:benchCore -PbenchJvmExtra="-XX:StartFlightRecording=..." :: JFR/系统属性等诊断透传
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

微基准（src-jmh）：Serialize / Collections / Conflict / ReadOnly 四族。

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

## 跨代档案（v1.6.3 → 2.x）

机器脚注：16C/JDK21/Windows 同机；v1.6.3 列 4g 堆（回植分支显式设定），2026-10-11 列
实为 2g——`-PbenchHeap` 当时被 build.gradle 的 withType(Test) 兜底覆盖而失效（当日已修复，
默认对齐 2g），"两列均 4g"的旧表述失真；两列堆不一致为已知口径瑕疵，跨代结论按量级引用，
严格 4g/4g 复测待补。v1.6.3=回植分支 `bench/backport-v1.6.3`
（TaskSpec→Task.runUnsafe 等价改写，I 场景排行榜 psortedmap→map15 同型承载），
2026-10-11=当日 master 实测（含 RRS 非公平锁优化 07171e5a7）。单位 ops/s（中位数），
括号内为相对 v1.6.3 倍数。

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

当日多轮数字离散度偏大（长会话热态），倍数看量级不看小数；
2026-09-29 final 提交（5a92cc111）与本轮 HEAD 的同日 A/B 终裁：**592 个审计修复提交无性能回归**。

## 跨版本结论（v1.6.3 → 2.x）

- **B 强冲突提升约 20×**（1.26万→27万/s，512m 口径）：v1.6.3 `Transaction.java:339` 每次重试
  无条件 `logger.info`（×10 主因，2.x 改 trace+限频）+ v2.0 时代周边改进（×2，主循环本身零提交）。
- **H 只读分两档，脏读记账是两代同构的结构成本**：读"已提交未冲刷"的脏记录（H·脏档）比读
  已冲刷的干净记录（H·干净档）慢（公平锁时代 3.7~4.1×）。机制（JFR 定位）：每个读脏记录的
  事务提交时走 RelativeRecordSet 公平锁合并 + checkpoint 编组 map 重登记，并发锁车队；
  干净记录走孤立 RRS 快路。单线程微基准无此成本，纯并发现象。生产启示：全服批量事务后的
  并发读（如批发邮件后的背包读）会踩此罚金；缓解=批后 checkpointRun 冲散编组、缩短
  CheckpointPeriod。**第六轮已落 RRS 非公平锁（07171e5a7，公平锁早于 2022-03 初始导入、
  引入无论证，2024-01 仅为字段内联重构）：脏读 +91%、B +35%、全线赢或平**；纯读事务
  免编组合并/免重登记为剩余设计级候选。**第七轮补强（同日）**：非公平的代价是 flusher
  等待从确定性上界降为统计性安全，配套把观测兜底补齐——RRS 锁等待埋点
  （RRS.commitLockWait/flushLockWait 聚合 + RrsWaitLock 慢事务时间线 + FlushSet 取锁告警
  zeze.rrsFlushLockWaitWarnMs）。K 洪水探针（提交洪水与并发 flush 争锁，含生产形态无界
  VT）：flusher 数千迭代持续推进、flushLockWait 均值 143µs（固定 VT 21µs）、单次 flush
  最大 200ms、告警零触发——饿死残余风险=统计性安全+观测兜底+实测无迹象。低并发补测
  （ConflictBench t4/t8 同日 A/B，JMH 置信区间大重叠）：非公平与公平无显著差异。埋点
  开销抽查（B/H脏/C 同日 A/B）：B 持平（+0.4%）；H/C 正向超噪声线但机械上不可能，判
  长会话热漂移伪影（冷却纪律），严格裁决待冷却后复测。
- 网络 Rpc 回环（J）与 MMO 混合画像（I）两代持平——net 层与混合事务负载无跨代退化。
- E2 哨兵 v1.6.3 快 2.5×——不是回归：2.x 起（`95d511254`）主库 RocksDB 写统一改为 sync
  （每批 fsync 等 WAL 落盘），以兑现 `CheckpointWhenCommit` 表"提交即落库、掉电不丢"的
  承诺；v1.6.3 是异步写（进 OS 缓冲即返回）。0.38× 即 fsync 代价，属用吞吐买正确性的
  自觉交换（普通表也陪着付 sync——按表分流为潜在优化候选）。
- G 缓存压力 HEAD 较 final +24%（快照 CopyIf 收编收益在当日环境放大）。

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
