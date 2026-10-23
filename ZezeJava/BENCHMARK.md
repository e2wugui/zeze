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

## 2026-10-23 · 事务日志职责拆分与可选增量（未提交）

改动：Transaction/Savepoint 保存 TransactionLog；Log 表达消费载荷和编码，LogRegistry
通过工厂返回完整解码值。Map1/2、SortedMap1/2、List1/2、Set1 将本地事务状态与消费载荷分开；
无 History、无监听时选择 NoDelta，仅维护最终持久容器。GTable 复用 Map 收集路径。
载荷从明确的最终 Savepoint 创建；消费者中途出现时，在不可逆提交及 Onz Ready 前重做并补齐增量。
Dynamic/One 适配新接口，本轮未完整拆分其事务对象与载荷。

标量使用 LogInt extends LogIntValue implements TransactionLog 的结构，17 种字段日志统一调整；
值父类不持有提交目标或 VarHandle，保留原 typeId、协议名称和可变值访问方式。
生成 setter 构造方式保持；BeanKey 回放改读 LogBeanKeyValue，已运行 PublishGen.bat 和
gen_use_publish.bat。Gen.exe 为发布产物，不纳入提交。Transaction.getLog/putLog 的字节码
描述符发生变化，冷、热模块须一起重新编译；测试构建已增加热模块重新打包依赖。

功能：完整 Fast 1931 项/跳过 2，集成 219 项/跳过 16，History 扩展 44 项，失败和错误均为 0。
最后删除标量构造器重复非空检查后，补跑相关 13 项定向测试及 JMH 编译，均通过；未再次全量运行。
当前 Corretto21.0.5 下 Instrumentation.getObjectSize 实测：17 种本地字段日志均保持 32 字节，
独立解码值均为 24 字节，原解码日志为 32 字节。此结果只说明浅对象大小，不能代替吞吐验收。

**性能验收仍未通过，未证明 GameSim 提速。** 本轮开始状态为 556f93406 加原有未提交清理，
以独立工作树和相同 workload/harness 对比；以下 GameSim 样本均早于最后的标量值父类调整。
配置为 Corretto21.0.5、2g、ktt、虚拟线程、-ea、5000 玩家、K=10，每窗口 10 游戏分钟，
2 预热窗口、3 测量窗口、checkpoint 30000ms；配套最终 XML 断言通过。

| 配对 | 改前 TPS 中位数 | 改后 TPS 中位数 | 变化 | mailSend p99（微秒） |
|---|---:|---:|---:|---:|
| B1 → C1 | 26583.65 | 26546.42 | -0.14%，3σ 波动内 | 661 → 1455 |
| B2 → C2 | 26629.76 | 26641.73 | +0.04%，3σ 波动内 | 711 → 1146 |

TPS 未显示稳定改善；两次 mailSend 尾延迟均出现不利结果，尚未归因，也未对最后代码完成重测。
早期 List/Set 单 fork 筛查中，NONE 的分配下降；LIST1/BOTH、SET1/BOTH 出现分配增长，
之后已调整操作列表容量及 Set 记账集合的延迟分配和容量。最新代码的 3 fork、wi10/i10、
每迭代 1 秒复测已完成（entries=16、REPLACE、BOTH，-da、2g、ktt）：

| 容器 | 分配改前 → 改后（B/事务） | 变化 | 吞吐均值变化 |
|---|---:|---:|---:|
| LIST1 | 2624.16 → 2696.06 | +71.90，+2.74%，CI 不重叠 | +4.86% |
| SET1 | 3069.47 → 3005.44 | -64.03，-2.09%，CI 不重叠 | -3.58% |

吞吐均值变化需结合各 fork 与 CI，不能单独判定稳定提速或回退；LIST1 分配增长尚未解决，
不能用吞吐均值增长或 SET1 的收益抵消。原始文件为
`jmh-log-redesign-b-list-long-final.json`、`jmh-log-redesign-c-list-long-values.json`。
当前尚无完整最终矩阵结论。
Map/SortedMap/GTable、List/Set 各消费模式、较大增量及最终 GameSim 性能验收均未完成。
不能把某行分配下降或标量对象变小写成整体无性能回退。

原始日志、JSON、验证 XML 及对象大小诊断保存在 gitignored 的
`ZezeJavaTest/bench-results/transaction-log-redesign/` 和 `log-redesign-*` 结果目录；
以下上一轮 Map 阶段的历史结论仍独立保留，不由本轮局部结果覆盖。

## 2026-10-22～23 · Map2 变更阶段拆分

改动：普通 Transaction 的 Map2/SortedMap2 将保存点、结构写和 commit 留在内部 Delta，
原名 LogMap2/LogSortedMap2 保存最终消费载荷，监听通过独立投影取完整 Bean。
GTable1/2 复用同一收集路径；随后单独将单格 get 缩短为两层安全 Map 查询，避免临时 Row 视图。
生成器、生成 Bean、日志 typeId 和 wire 格式保持。Map 阶段完成后仅单独整理 GTable 查询；
Map 分组与最终 GTable/宏观对照的原始证据分别保留。

功能：最终 Fast 1920 项/跳过 2，标准集成 219 项/跳过 16，失败和错误均为 0；
JMH 编译通过。实际 History capture/编码、生成 follower 回放、保存点、陈旧引用与有序键均有覆盖。

性能：Corretto21.0.5、2g、ktt、同机相邻串行；最终 GTABLE2/NONE 两规模 f5/wi15/i10 对照，
1024 项分配约少 39.61 B/事务（-4.67%）、吞吐 +5.65%；16 项分配少 58.38 B（-6.74%）、
吞吐 +1.95%。分配均有利区间分离，改后各 fork 均低于基线，吞吐仍存在漂移，不能扩大成普遍提速。

**性能整体验收未通过。** GTABLE1/NONE/1024 的单 fork 矩阵分配 +39.99 B，独立长 f3 后仍有
均值 +12.40 B（+0.76%，CI 重叠）的档位残余；未完成版本归因，未证明每事务固定额外对象，仍按事前门槛保留。
最终基线→改后宏观 D/L 中位吞吐为 -0.32%/+0.33%，按 3σ 均在波动内；mailSend p99 +11.00%，
loot/trade/equip p99 也有不利样本，steady 驻留堆 +2.99%。不能用总 TPS、其他行收益或剖析运行抵消。

验收使用独立基线与共同 harness；正式/失败样本和诊断原始文件保留于 gitignored 的
`ZezeJavaTest/bench-results/`；基线证据已复制至其中的 `map-phases-baseline-archive/`，
83 个原始文件共 145668748 字节，逐文件 SHA256 核验一致，清单为该目录的 `_evidence-manifest.json`。

## 2026-10-20 · Checkpoint 批次重构

改动：FlushBatch 组合复用记录列表和四个类型编码数组，集中编码、主库／镜像／旧表写入及清脏。
有界 ArrayDeque 负责批次借还，并发和重入各自独占，归还前清引用；删除一次性 A/C 脚本。

性能：JDK 21 压缩引用下，容量足够时容器复用为 0 B/轮；组合较继承每个新建批次多 16B。
前序五槽／多列及写入收敛的 Memory 主库＋Rocks 镜像对照未检出稳定的整体回退，
最终组合改动只复测容器分配，不据此认定吞吐提升、尾延迟等价或 SQL 性能。
归档：`flush-batch-refactor/`、`flush-snapshot-invariants/`、`flush-batch-ownership/`、`flush-batch-composition/`。

## 2026-10-19～20 · 刷盘接口收敛

改动：Record 保留缓存状态，Table 移除刷盘接口，TableX 提供具体编码能力，Checkpoint 持有批次和事务。
明确记录、表与批次的职责，没有新增逐条刷盘对象。

性能：1／1024 条、64／4096B 四档完整 flush 的稳态 Java 分配不增。
独立 JVM 曾出现偏慢结果，共享线程和 JNI 后反转版本位置未复现稳定回退；宏观波动较大，
结论限于覆盖场景未检出可复现退步。归档：`record-flush-ac/`。

## 2026-10-19 · 刷盘状态批次化

改动：以可复用批次缓冲替代逐条 EncodedFlush 对象，清理后缓存空数组，并支持并发、重入和失败重试。
随后删除单例绕行与业务调用方对槽位布局的依赖。

性能：相对最初记录持有快照的基线，Record1 浅大小由 96B 降至 72B，单条完整 flush 分配约少 64B。
逐条对象方案在 1024 条／64B 场景约多 44B/条（总分配 +7.98%）的回退已消除；
最终覆盖的耗时与吞吐对照未检出稳定回退。归档：`record-flush-ac/`。

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
