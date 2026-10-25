# ZezeJava 性能记录

每项只记录提交内容和性能结论。完整实验过程见 Git 历史，原始结果保存在本机
`ZezeJavaTest/bench-results/`（gitignored）；下列数字仅适用于对应实验配置。

## 跑法与判定

在 ZezeJava 目录使用 JDK 21：

```bat
gradlew.bat :ZezeJavaTest:benchCore -PbenchTag=名称
gradlew.bat :ZezeJavaTest:benchGame -PbenchTag=名称 -PgamePlayers=5000
gradlew.bat :ZezeJavaTest:jmh -PjmhFilter=正则 -PjmhExtraArgs="-prof gc"
gradlew.bat :ZezeJavaTest:test :ZezeJavaTest:integrationTest
```

同机同日交替运行 A/B，统一 JDK、堆大小和断言开关；堆大小可用 `-PbenchHeap=4g` 指定。
宏观变化需超过 3×合成标准差，JMH 结合置信区间与独立复测判断；噪声内不宣称加速，
也不把“未检出回退”解释为严格等价。对比工具为 `harness.BenchCompare`。

带参数的 JMH 使用 `ZezeJavaTest/scripts/compare_jmh.py before.json after.json --output report.json`，
按 benchmark 和完整 params 配对，并检查 JVM/JMH 配置与 GC 分配指标；Map 提交的默认 32 行矩阵另加
`--expect-map-grid`。不要用旧 BenchCompare 的仅名称 JMH 配对比较参数矩阵。
工具退出成功不代表等价：同方向的独立负向复测、逐 fork 档位和尾延迟仍须人工审核，不能跨行抵消。

游戏报告按完整 `L_MmoGameSim*.json` 文件名配对，输出所有活跃操作的 count、tps、mean、p50、p95、
p99、max；缺失会提示，尾指标仅作信息输出。还须核对实际运行配置与 XML 最终断言，单有 JSON 文件
不足以证明守恒检查通过。正式结论记录最终代码版本，诊断时加 JFR 等 profiler 的结果不得混入验收。
每轮保留原始 JSON、运行日志与最终 XML；有疑点先独立重复，再对改动后的最终版本重新验证。

Windows 可用 `ZezeJavaTest/scripts/run_map_phase_acceptance.ps1` 固定单轮配置：

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
.\ZezeJavaTest\scripts\run_map_phase_acceptance.ps1 -Root $PWD -Tag map-phase-g-matrix-1 -Stage Matrix
```

阶段为 Functional、Matrix（Map/GTable 的 40 行筛查）、Multi（64 项修改的 8 行）、
Collections（List1/List2/Set1 的替换、清空重填，共 48 行）、ListEdit（List2 原位字段修改的 8 行）、
Macro（D 与 L）。Matrix 使用明确的基准类过滤，不随新加入的 CommitBench 意外扩展范围。
每次使用新 Tag，串行运行两个版本；可用 GradleExecutable 指定本机缓存的 Gradle。
脚本只确认命令与最终 XML 已保存，不自动判性能通过；Matrix 的单 fork 不能替代独立 JVM 复测。

| 场景 | 基准类 | 内容 |
|---|---|---|
| A | ABasicSimpleAddOneThread | 单线程简单事务 |
| B | BBasicSimpleAddConcurrentWithConflict | 强冲突事务 |
| C | CBasicSimpleAddConcurrent | 一般并发事务 |
| D | DBasicContainerTransaction | 容器事务 |
| E | ECheckpointSteady | 四种刷盘模式 |
| E2 | E2CheckpointRocksDBSentinel | 真实 RocksDB 主库 |
| F | FDispatchPoolThroughput | 虚拟／平台线程派发 |
| G | GCacheThrashing | 缓存装载与驱逐 |
| H | HReadOnlyConcurrent | 脏／干净记录只读事务 |
| I | IMmoLiteWorkload | MMO 混合负载 |
| J | JSocketRpcLoopback | 网络 RPC 回环 |
| K | KCheckpointUnderCommitFlood | 提交洪水下的刷盘竞争 |
| L | GameSim.LMmoGameSim | 游戏模拟与容量 |

## 2026-10-19～20 · 刷盘接口收敛

改动：Record 保留缓存状态，Table 移除刷盘接口，TableX 提供具体编码能力，Checkpoint 持有批次和事务。
明确记录、表与批次的职责，没有新增逐条刷盘对象。

性能：1／1024 条、64／4096B 四档完整 flush 的稳态 Java 分配不增。
独立 JVM 曾出现偏慢结果，共享线程和 JNI 后反转版本位置未复现稳定回退；宏观波动较大，
结论限于覆盖场景未检出可复现退步。

## 2026-10-19 · 刷盘状态批次化

改动：以可复用批次缓冲替代逐条 EncodedFlush 对象，清理后缓存空数组，并支持并发、重入和失败重试。
随后删除单例绕行与业务调用方对槽位布局的依赖。

性能：相对最初记录持有快照的基线，Record1 浅大小由 96B 降至 72B，单条完整 flush 分配约少 64B。
逐条对象方案在 1024 条／64B 场景约多 44B/条（总分配 +7.98%）的回退已消除；
最终覆盖的耗时与吞吐对照未检出稳定回退。

## 2026-10-16 · 0b8b64f70 · 持久化容器默认使用 ktt

改动：事务集合可切换 pcollections、kt、ktt 后端，默认 ktt，批量更新使用 builder。
解码改为 JDK 容器暂存后一次构建，保留 pcollections 回退选项。

性能：5000 玩家、K=10 的同日两轮对照中，ktt 驻留堆约少 27%，TPS 变化约 ±0.2%，处于波动内。
256 条批量写 p50 由 120µs 降至 44µs，冷装载耗时约少 40%；PList 补测驻留堆少约 30%、冷装载耗时少约 51%。
归档：`collbackend1/`、`collbackend2/`、`plist1/`、`plist2/`。

## 2026-10-16 · 3eaa6355f · kotlinx 升级至 0.5.2

改动：升级持久化容器内核，采用其碰撞处理和短路修正，基准标签改为读取实际运行版本。
适配器与基准统一使用 0.5.x 新 API，不再兼容 0.3.7 回插编译。

性能：生产关闭断言时，0.5.2 与 0.3.7 的差异处于波动范围内；开启 `-ea` 后，恢复的内部断言使部分
容器热路径约慢一倍，分配量不变。跨版本比较必须使用相同断言开关。

## 2026-10-15～16 · de7f07246 等 · History 关闭路径

改动：无变更消费者时跳过 Changes 创建，拆分监听器与 History 收集，关闭 History 时惰性创建相关资源。
随后通过 IHistory 接口隔离模块职责。

性能：整个容器事务的分配先减少 96.39 B/op，再减少 31.59 B/op；接口拆分前后约为
2880.72／2880.83 B/op，差异在波动内。吞吐未检出显著增益，启动耗时和固定内存节省未量化。
归档：`jmh-history-off-*`、`jmh-history-collector-*`、`jmh-history-plugin-refactor-after.json`。

## 2026-10-14～15 · 游戏模拟的刷盘周期

改动：在 40000 玩家、K=10 负载下对比 5／10／30／60／120 秒刷盘周期，检查吞吐和操作尾延迟。

性能：本组 60 秒最好，中位吞吐 129296 txn/s，较默认 30 秒约高 8.7%；5 秒约低 16.8%，
且 loot p95 达 19～110ms。最优周期依赖负载密度，不能把更频繁刷盘当作通用优化。
归档：`p40000-cp*/`。

## 2026-10-14 · a6226af90 · 邮件广播拆分事务

改动：全服邮件广播由一个大事务改为每玩家一个事务，同时把计数和延迟统一为单事务口径。

性能：20000 玩家时，广播全程由 355ms 降至 263ms，吞吐变化约 -0.8%，处于噪声内，守恒校验通过。
旧版全程延迟与新版单事务分位不可直接比较。归档：`p20000-fanout-split/`、`p20000-fanout-precise/`。

## 2026-10-14 · eea4d944c、d9c5ffbf2 · 游戏负载与冲突面调整

改动：容量测试以在线人数为主变量，将排行榜改为 512 分片，好友显示查询使用脏读；后续固定操作强度 K=10。

性能：K=4 的修正前后对照中，40000 玩家吞吐约由 48175 增至 55395 txn/s（+15%），
loot p95 由 269ms 降至 111µs；80000 玩家仍过载。旧 K 梯度与新人数梯度的容量数字不可直接比较。
归档：`p20000-r512/`、`p40000-r512/`、`p80000-r512/`。

## 2026-10-12 · 7fe2d0dd2 · 纯读事务免编组

改动：无脏写、无未注册脏记录且无 Onz 的提交跳过 RRS 锁、合并和重注册；旧表倒库产生的脏记录仍需注册。

性能：同日 ABBA 对照中，L 的 K=80 吞吐提高 17.6%，H 脏记录只读吞吐提高约 110%。
历史容量由约 12.2 万提高到至少 35 万 txn/s，之后受模拟器限速，不能视为已测出引擎上限。

## 2026-10-11 · cb77d2c0b · RRS 非公平锁

改动：RRS 使用非公平锁，并增加刷盘锁等待观测和告警。

性能：对应对照中脏记录只读吞吐提高约 91%，强冲突场景提高约 35%；K 洪水探针未观察到刷盘饥饿，
锁等待均值约 143µs、最大约 200ms。

## 2026-09～10 · Changes 惰性化与快照收编

改动：`9fcd43afa` 延迟创建无监听者事务的 Changes 容器；`5a92cc111` 让 Memory 事务收编可安全共享的快照。

性能：对应历史对照中，普通无监听者事务分配由约 1.4KB 降至约 1KB，G 缓存压力吞吐提高约 24%。

## 2026-10-11～12 · v1.6.3 与 2.x 对照

改动：在回植基准上比较两代的事务、刷盘、派发和游戏负载；台式机复测统一使用 4g 堆并交替运行。

性能：两机均复现强冲突约 15～23 倍、脏记录只读约 1.6～1.7 倍的差异，干净记录只读则下降约三成。
RocksDB 主库同步写的额外成本来自持久化语义加强，
派发和网络结果还受处理器数量配置影响，不能归纳为所有场景统一加速。
