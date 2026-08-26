## 目录

```
|- ZezeJava     // zeze.jar的代码
|- ZezeJavaTest // 测试代码
|- test         // 手工测试环境脚本（bat 等）
|- ZezexJava    //
|- ZokerManager 
|- scripts
   |- cue       // 监控配置

```

## 如何运行ZezeJavaTest

1. （可选）改了 solution.xml/demo2.xml 才需要：run ../gen_use_publish.bat
2. 在 ZezeJava 目录下运行 `gradlew.bat :ZezeJavaTest:test`（快速）/ `:ZezeJavaTest:integrationTest`（全量功能）/ `:ZezeJavaTest:bench`（吞吐基准）
3. 或用 IDEA 打开 ZezeJava 目录，右键 "ZezeJavaTest/src" → "Run 'All Tests'"

---

## 结论先行

- **不需要准备 MySQL/MongoDB/TiKV/RocketMQ**。所有依赖外部数据库的测试都有"主机名门控"或 `@Disabled`，在陌生机器上会自动跳过，不会失败（详见下文分类）。
- **不需要手工启动任何 bat**。测试已于 2026-08 从 JUnit 4/3 整体迁移到 **JUnit 6（Jupiter 6.1.3，要求 Java 17+）**，并挂在 `ZezeJavaTest` 的 **test 源集**，三车道：
  - `gradle test`：只跑 **@Fast 自包含测试**（45 个类，无任何外部依赖，开箱即绿）。**类级并行**（个别类按 `@Execution(CONCURRENT)` 方法级并行），墙钟约 8s——下限由 worker JVM 启动和 ~3s 级的 CPU 测试杆决定，属刻意设计；
  - `gradle integrationTest`：**全量功能测试**（266 个，不含基准），由 `harness.TestEnvLauncherListener` 在测试 JVM 内自动启动 ServiceManager(5001) 与 GlobalCacheManagerAsyncServer(5002)，会话结束自动关闭；端口被手工 bat 占用时直接复用；
  - `gradle bench`：**吞吐基准**（@Bench 标注的 Benchmark 包整体，9 类 24 个）。基准靠打印 M/s 观察、不设断言，不进功能车道；其中 A/B/C 事务场景依赖 SM/GCM（进程内 harness 自动提供）。
- 已知限制：**`--tests` 与 `includeTestsMatching` 模式过滤在本项目不工作**（标签过滤正常，三车道均基于标签实现；用 IDEA 跑单个类）。
- 唯一例外：`Onz/TestOnz` 额外依赖第二对服务 5011/5012（GCM 是进程内单例，起不了第二对），未启动时自动跳过；要跑它先运行 `test/service & global.another.bat`。
- 两个任务都配置了 `workingDir = ZezeJavaTest`、`-ea`、`maxHeapSize = 2g`（BenchSocket 等大缓冲区测试在默认 512m 堆下会 OOM）。

## 环境要求

| 项目 | 要求 | 说明 |
|---|---|---|
| JDK | 21 | `build.gradle` toolchain Java 21 |
| 快速/全量测试 | `gradlew :ZezeJavaTest:test` / `:ZezeJavaTest:integrationTest` | 无需外部进程，SM/GCM 进程内自启 |
| 代码生成 | 通常可跳过 | `ZezeJavaTest/Gen` 已提交在仓库里；只有改了 solution.xml/demo2.xml 才需跑根目录 `gen_use_publish.bat` |
| MySQL/PG/Mongo/SqlServer/TiKV/RocketMQ | **不需要** | 对应测试会自动跳过（见下） |
| `test/*.bat` | 仅手工调试场景 | 进程内启动取代了 `service & global.bat` 的"跑测试"职责；raft/sync 变体仍服务手工场景 |

## 测试分层约定（给新增测试的规则）

- 自包含测试（不依赖外部 SM/GCM 进程和外部数据库）标注 **`@Fast`**（`harness.Fast` 组合注解，等价 `@Tag("fast")`），由 `gradle test` 执行。
- **吞吐基准标注 `@Bench`**（`harness.Bench`，等价 `@Tag("bench")`，Benchmark 包整体），由 `gradle bench` 执行，`integrationTest` 按标签排除。识别特征：无断言、靠 `Benchmark()/report()` 打印 M/s 或耗时——自包含 ≠ 该进快速车道。
- **不打 tag 的新测试默认归入 `integrationTest`**（环境更全的桶）——忘打 tag 不会打破 "`gradle test` 开箱即绿"。
- **`gradle test` 已开启类级并行**（仅此任务）：因此 `@Fast` 准入除"自包含"外还要求彼此互不干扰——固定端口独占、本地目录独占、无静态状态竞争（现有 fast 类固定端口：TestRpc=5000、TestToken=5003）。
- **`integrationTest` / `bench` 不要开并行**：`demo.App` 是 JVM 级单例（`Start()` 幂等、测试里 `Stop()` 被注释，整个 JVM 只起一次）、所有测试共享 `workingDir` 下的 `dbhome/`（RocksDB LOCK）、`App.Start()` 固定绑定 HttpServer 10000 端口、大量测试用固定 key——同 JVM 并行（`junit.jupiter.execution.parallel`）和跨 fork 并行（`maxParallelForks` 共享 workingDir）都会互相干扰。

## 测试分类盘点（`ZezeJavaTest/src`，共 105 个带 @Test 的类）

### A. 依赖 SM(5001) + Global(5002) — 约 47 个（主力，由 integrationTest 的进程内 harness 自动服务）

绝大多数核心测试属于此类：`@BeforeEach` 里调 `demo.App.getInstance().Start()`（`src/demo/App.java`）或自建 `Application` 加载 `./zeze.xml`，SM Agent 连 `127.0.0.1:5001`、GCM 连 `5002`。

- 代表：`UnitTest/Zeze/Trans/*`（TestTable、TestProcedure、TestCheckpoint、TestConflict、TestBegin、TestLock、TestTableKey 等）、`Collections/*`、`Component/*`、`Game/TestBag`、`Game/TestRank`（用 `demo.SimpleApp` 连外部 GCM）、`Serialize/TestRawBean`
- **注意：`Dbh2/Dbh2Test`、`Dbh2/Dbh2FullTest`、`TestLog4jQuery/TestLogService` 也属于此类**（自建 Application 但加载默认 zeze.xml 连外部 SM/GCM；此前文档误归为自包含）
- 进程内组网但仍连外部 SM/Global：`Zezex/TestOnlineSpec`、`TestGameTimer`、`TestRoleTimer`、`Infinite/Simulate`、`UnitTest/Zeze/Trans/TestConcurrentStartServer`、`Net/TestRpc`（进程内 127.0.0.1:5000 组网）
- `UnitTest/Zeze/Util/TestTaskSpec` 也用 `App.Instance.Start()`（不随 Util 其余测试进 fast）

### B. 额外依赖第二对 SM(5011) + Global(5012) — 仅 1 个

- `Onz/TestOnz.java`：加载 `zeze_cluster_2.xml`。`@BeforeEach` 有 5011 可达性 Assumption，未启动时跳过并提示运行 `test/service & global.another.bat`。

### C. 依赖外部数据库/中间件/外网 — 约 7 个，**全部自动跳过，无需准备**

| 测试 | 目标 | 跳过机制 |
|---|---|---|
| `UnitTest/Zeze/Trans/TestDatabaseMySql` | `jdbc:mysql://localhost:3306/devtest` | 仅主机名 `doudouwang`/`DESKTOP-VVU42V2` 运行 |
| `UnitTest/Zeze/Trans/TestDatabasePostgreSQL` | `jdbc:postgresql://localhost:5432/devtest` | 仅 `doudouwang` |
| `UnitTest/Zeze/Trans/TestDatabaseMongoDb` | `mongodb://127.0.0.1:27017/?replicaSet=rs0` | 仅 `doudouwang` |
| `UnitTest/Zeze/Trans/TestDatabaseSqlServer` | `jdbc:sqlserver://localhost` | 无驱动即 skip，代码自述"先不管了" |
| `UnitTest/Zeze/Trans/TestDatabaseTikv` | PD `10.12.7.140:5379` | 整个类 `@Disabled` |
| `UnitTest/Zeze/Misc/TestRocketMQ` | namesrv `127.0.0.1:9876` | 整个类 `@Disabled` |
| `UnitTest/Zeze/Net/TestAsyncSocket` | 外网 `www.163.com:80` | `@Disabled`（依赖外网，不适合自动化） |

另：`UnitTest/Zeze/Trans/TestGlobal.test2App` 挂 `@Disabled`（两个 app 对同一 key 的 GCM 并发协调 `FutureTask.get()` 永久等待，疑似死锁，待排查）；`TestDatabaseRocksDB` 的 test1/test2 有主机名门控（`DESKTOP-48A4UQ1` 因 CPU 指令集跳过），其余机器正常执行。

### D. 完全自包含（@Fast，`gradle test` 执行）— 45 个

不需要任何外部进程，只要求工作目录正确：

- 进程内自建服务/存储：`MQ/TestMQ`、`MQ/TestFileWithIndexed`、`Dbh2/TestLocateBucket`、`Dbh2/TestRocksDb`、`TestLog4jQuery/TestLog4jQ`、`TestLog4jQuery/TestMmap`、`UnitTest/Zeze/Netty/TestNettyHttpServer`、`UnitTest/Zeze/Net/TestDatagram`、`UnitTest/Zeze/Net/TestRpc`（进程内 127.0.0.1:5000 组网）、`UnitTest/Zeze/Arch/TestArchOnlineSpec`、`UnitTest/Zeze/Collections/TestBeanFactory`、`UnitTest/Zeze/Component/TestToken`、`UnitTest/Zeze/Misc/TestTreeMap`、`UnitTest/Zeze/Trans/TestDatabaseRocksDB`（本地嵌入式 RocksDB）、`RelationalMapping/TestRelationalTableDiff`、`Temp/TestBigInt`
- 纯逻辑：`UnitTest/Zeze/Util/*` 中的 21 个（除 TestTaskSpec）、`UnitTest/Zeze/Serialize/TestByteBuffer`、`TestDynamic`、`TestRawBean.testBasic`（同类的 testTransaction 依赖 demo.App，类级不打 @Fast）、`UnitTest/Zeze/Trans/{TestBegin,TestLock,TestTableKey,TestConcurrentDictionary}`、`UnitTest/Zeze/Net/{TestCodec,TestOutputBuffer}`

### E. 吞吐基准（@Bench，`gradle bench` 执行）— Benchmark 包 9 类 24 个

`BenchSocket`（9，含少量正确性断言）、`BenchTaskOneByOne`（5）、`CheckpointFlush`（4）、`ABasic/BBasic/CBasicSimpleAdd*`（3，事务并发场景依赖 demo.App，bench 任务里由进程内 harness 伺候）、`BenchToData`、`DiffLockAndNoLock`、`PMapLogTypeIdHash32Cache`（各 1）。均以打印 M/s/耗时观察为主。原名 TestTaskOneByOne/TestToData 已改名 BenchTaskOneByOne/BenchToData（它们无断言、纯计速，Test 前缀名不副实）。

### 2026-08 补标：JUnit 4/3 迁移漏标的死测试已恢复

迁移时 14 个类 26 个 `public void testXxx()` 方法漏了 `@Test` 注解（JUnit 3 靠命名约定自动运行，Jupiter 下等于死代码，其中 5 个 TestDatabase\* 连维护者机器上都不会跑）。已全部补标并验证：

- **24 个直接启用并通过**：`TestBegin`(4)、`TestLock`(5，含裸名 `test()`)、`TestTableKey`、`TestConcurrentDictionary`、`TestCodec`(3)、`TestOutputBuffer`、`TestRpc`、`TestRawBean.testTransaction`、`TestGlobal.testNone`、`TestDatabase{MySql,PostgreSQL,MongoDb,SqlServer}.test1`、`TestDatabaseRocksDB`(2)
- **2 个有意 `@Disabled`**：`TestAsyncSocket.testConnect`（连外网 www.163.com）、`TestGlobal.test2App`（GCM 并发协调 hang，待排查——这可能是暴露真实问题的线索）
- 其中的 `@Fast` 候选（TestBegin/TestLock/TestTableKey/TestConcurrentDictionary/TestCodec/TestOutputBuffer 等纯逻辑类）暂留在 integrationTest，后续可评估移入 fast
- 仍有 3 个文件无 @Test 但属"main 方式手工运行"而非漏标：`TestLog4jQuery/TestWatch`、`SimpleRaft/RaftTest`（`testSimple` 由自身 main 调用）、`Benchmark/BenchStackWalker`（`testAll` 由 main 调用）

## 进程内环境 harness（`src/harness/`）

- `TestEnvLauncherListener`：JUnit Platform `LauncherSessionListener`，经 `src/test/resources/META-INF/services/` ServiceLoader 注册，IDEA / gradle / ConsoleLauncher 三个入口统一生效。会话开始时探测 5001/5002，空闲则进程内启动（`new ServiceManagerServer(null, 5001, Config.load())` + `GlobalCacheManagerAsyncServer.getInstance().start(null, 5002, null)`，语义与 bat 相同），结束只关闭自己启动的。`gradle test` 通过 `-Dzeze.test.env=off` 关闭它。
- 副作用：SM 的 autokeys 目录从 `test/autokeys` 变为 `ZezeJavaTest/autokeys`（已被 gitignore 覆盖）。
- 先例参考：`GlobalRaft/TestGlobalCacheMgrWithRaft.java`。

## 常见问题

- **Connection refused 5001/5002** → 用了旧的运行方式（手工 bat + IDEA）但没启动 bat；新方式 `gradle integrationTest` / IDEA All Tests 会自动进程内启动。
- **找不到 zeze.xml / dbhome 报错** → 工作目录不对，必须在 `ZezeJava/ZezeJavaTest` 下运行（gradle 任务已自动配置；单独用 ConsoleLauncher 时需自己 cd）。
- **test_all.bat** → **已废弃**，由 `gradle integrationTest` 取代（它引用旧的 main 源集输出路径且类清单是手工维护的）。
- **gradle test 只跑部分测试** → 正常，它按 `@Fast` 标签过滤；全量功能用 `gradle integrationTest`，基准用 `gradle bench`。三车道均基于 JUnit 标签（模式过滤 `--tests`/`includeTestsMatching` 在本项目不工作，见"结论先行"）。
- **log/ 目录积累大量日志会拖慢 gradle test** → `TestLog4jQuery/TestLog4jQ` 线性扫描最近一天的日志文件，机器上有日志洪水时单个类可占 60s+，清理 `ZezeJavaTest/log/` 即恢复（都是运行产物）。

## 遗留改进（2026-08 分层改造后仍有效的）

- Zezex 三个测试各自复制了 `prepareNewEnvironment/stopAll`，可抽成公共 harness；
- `service & global.sync.bat` / `*.raft.bat` 等变体没有对应自动化测试使用，建议在脚本内注释说明各自服务于哪些手工场景；
- C 类外部 DB 测试的主机名门控是脆弱约定（CI 机器撞名会误跑），后续可改成 `@Tag("external-db")` + 显式开关；
- `TestGlobal.test2App` 的 GCM 并发协调 hang 值得排查（补标时发现，已 `@Disabled` 保留现场）；
- `--tests` 过滤失效问题（见"结论先行"）待排查。
