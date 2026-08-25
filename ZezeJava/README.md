## 目录

```
|- ZezeJava     // zeze.jar的代码
|- ZezeJavaTest // 测试代码
|- test         // 测试代码启动需要的环境
|- ZezexJava    //
|- ZokerManager 
|- scripts
   |- cue       // 监控配置

```

## 如何运行ZezeJavaTest

1. run ../gen_use_publish.bat
2. run test/build.bat
3. run test/service & global.bat
4. run test/service & global.another.bat
5. launch idea and open ZezeJava (this path)
6. in idea, right click "ZezeJavaTest/src" and click "Run 'All Tests'"

---

## 结论先行

- **不需要准备 MySQL/MongoDB/TiKV/RocketMQ**。所有依赖外部数据库的测试都有"主机名门控"或 `@Ignore`，在陌生机器上会自动跳过，不会失败（详见下文分类）。
- 真正必须的准备只有三件事：**JDK 21**、**`gradlew build copyJar`**、**启动两对 ServiceManager/GlobalCacheManager 进程**。
- 测试已于 2026-08 从 JUnit 4/3 **整体迁移到 JUnit 6（Jupiter 6.1.3，要求 Java 17+）**，但仍挂在 `ZezeJavaTest` 的 **main 源集**（`ZezeJavaTest/build.gradle` 的 `sourceSets.main.srcDir "src"/"Gen"`），所以 **`gradle test` 依然发现不了任何测试**，目前只能靠 IDEA "Run 'All Tests'"（需 2025.2+ 版本的 IDEA）或 `ZezeJavaTest/test_all.bat`（已改用 JUnit Platform ConsoleLauncher 的 `execute` 子命令）跑。

## 环境要求

| 项目 | 要求 | 说明 |
|---|---|---|
| JDK | 21 | `ZezeJava/build.gradle` 配置了 toolchain Java 21 |
| 构建 | `gradlew build copyJar` | 编译全部模块并把依赖拷到 `ZezeJava/lib`（service & global.bat 的 classpath 依赖它） |
| 代码生成 | 通常可跳过 | `ZezeJavaTest/Gen` 已提交在仓库里；只有改了 solution.xml/demo2.xml 才需跑根目录 `gen_use_publish.bat` |
| MySQL/PG/Mongo/SqlServer/TiKV/RocketMQ | **不需要** | 对应测试会自动跳过（见下） |

## 最小测试流程（Windows）

```bat
:: 1. 编译（在 ZezeJava 目录下）
gradlew.bat build copyJar
:: 等价于 test\build.bat

:: 2. 启动第一对服务：ServiceManager(5001) + GlobalCacheManager(5002)
test\"service & global.bat"

:: 3. 启动第二对服务：ServiceManager(5011) + GlobalCacheManager(5012)
::    仅 Onz.TestOnz 需要，但建议总是启动
test\"service & global.another.bat"

:: 4a. IDEA：打开 ZezeJava 目录，右键 ZezeJavaTest/src → Run 'All Tests'
:: 4b. 或命令行跑固定清单（约 100 个测试类，JUnitCore 方式）：
ZezeJavaTest\test_all.bat
```

注意：测试的**工作目录必须是 `ZezeJava/ZezeJavaTest`**。测试用相对路径加载 `./zeze.xml`（指向 SM 5001 / GCM 5002），并读写 `dbhome/`、`autokeys/`、`web/`、`log/` 等目录。IDEA 右键运行时工作目录默认即模块目录，无需额外设置。

## 测试分类盘点

`ZezeJavaTest/src` 下约 78 个可运行测试类（已全部为 JUnit 6 Jupiter 写法；迁移前为 72 个 JUnit4 注解式 + 33 个 JUnit3 风格，其中 JUnit3 的 `extends TestCase`/`testXxx()` 约定已转为 `@Test` 注解），按外部环境依赖分四类：

### A. 依赖外部 SM(5001) + Global(5002) 进程 — 约 45 个（主力）

绝大多数核心测试属于此类：`@Before` 里调 `demo.App.getInstance().Start()`（`src/demo/App.java`），加载 `./zeze.xml`，SM Agent 连 `127.0.0.1:5001`、GCM 连 `5002`。**没有任何 JUnit 测试在进程内自建 ServiceManager**——这就是必须先跑 bat 的根本原因。

- 代表：`UnitTest/Zeze/Trans/*`（TestTable、TestProcedure、TestCheckpoint、TestConflict 等约 20 个）、`Collections/*`、`Component/*`、`Game/TestBag`
- 进程内组网但仍连外部 SM/Global：`Zezex/TestOnlineSpec`、`TestGameTimer`、`TestRoleTimer`（进程内起 linkd+server+client，但 linkd.xml/server.xml 指向 5001/5002）、`Infinite/Simulate`、`UnitTest/Zeze/Trans/TestConcurrentStartServer`
- 不启动 bat 的症状：`Connection refused: 127.0.0.1:5001 / 5002`

### B. 额外依赖第二对 SM(5011) + Global(5012) — 仅 1 个

- `Onz/TestOnz.java`：加载 `zeze_cluster_2.xml`，需要 `service & global.another.bat`

### C. 依赖外部数据库/中间件 — 约 6 个，**全部自动跳过，无需准备**

| 测试 | 目标 | 跳过机制 |
|---|---|---|
| `UnitTest/Zeze/Trans/TestDatabaseMySql` | `jdbc:mysql://localhost:3306/devtest` | 仅主机名 `doudouwang`/`DESKTOP-VVU42V2` 运行 |
| `UnitTest/Zeze/Trans/TestDatabasePostgreSQL` | `jdbc:postgresql://localhost:5432/devtest` | 仅 `doudouwang` |
| `UnitTest/Zeze/Trans/TestDatabaseMongoDb` | `mongodb://127.0.0.1:27017/?replicaSet=rs0` | 仅 `doudouwang` |
| `UnitTest/Zeze/Trans/TestDatabaseSqlServer` | `jdbc:sqlserver://localhost` | 无驱动即 skip，代码自述"先不管了" |
| `UnitTest/Zeze/Trans/TestDatabaseTikv` | PD `10.12.7.140:5379` | 整个类 `@Ignore` |
| `UnitTest/Zeze/Misc/TestRocketMQ` | namesrv `127.0.0.1:9876` | 整个类 `@Ignore` |

### D. 完全自包含 — 约 20 个

不需要任何外部进程，只要求工作目录正确：

- 进程内自建服务：`MQ/TestMQ`（进程内 MQ master+3 manager）、`Dbh2/Dbh2Test`、`Dbh2FullTest`（进程内 3 节点 Raft + 本地 RocksDB）、`TestLog4jQuery/TestLogService`、`UnitTest/Zeze/Netty/TestNettyHttpServer`
- 本地嵌入存储：`UnitTest/Zeze/Trans/TestDatabaseRocksDB`
- 纯逻辑：`UnitTest/Zeze/Util/*`（17 个）、`Zeze/Arch/TestArchOnlineSpec`、`Benchmark/DiffLockAndNoLock`、`RelationalMapping/TestRelationalTableDiff` 等

## 常见问题

- **Connection refused 5001/5002** → 没跑 `test/service & global.bat`，或 `copyJar` 没做导致服务起不来。
- **找不到 zeze.xml / dbhome 报错** → 工作目录不对，必须在 `ZezeJava/ZezeJavaTest` 下运行。
- **`gradle test` 显示 "no tests found" 或直接成功** → 正常现象，测试不在 test 源集（见改进建议 1）。
- **test_all.bat 与 IDEA 全量不一致** → `test_all.bat` 是手工维护的固定类清单，新增测试类不会自动包含。

## 改进建议

### 1. 集成到 `gradle test`（建议做，但要分层）

现状：测试在 main 源集（JUnit 6 迁移已完成，`junit:junit` 依赖已移除，不再需要 vintage engine），根 `build.gradle` 的 `test { useJUnitPlatform() }` 还在根 project 作用域（根 project 无源码），形同虚设。建议：

- 在 `ZezeJavaTest/build.gradle` 增加 test 源集（把 `src`/`Gen` 一起从 main 移到 test，二者互相引用不能拆分），jupiter 依赖改为 `testImplementation`/`testRuntimeOnly`；
- `test` 任务配置 `workingDir = projectDir`（满足相对路径假设）、`jvmArgs '-ea'`；
- **关键是分层**：D 类自包含测试用 JUnit5 `@Tag("fast")` 或直接按包过滤纳入默认 `gradle test`；A/B 类注册成单独的 `gradle integrationTest` 任务。这样 `gradle test` 开箱即绿，全量测试单独跑。
- 不建议"直接把全部测试塞进 gradle test"：外部 DB 测试靠主机名门控跳过是脆弱的约定，CI 机器名一旦撞上就会误跑。

### 2. 消除对外部 SM/Global bat 的依赖（收益最大）

约 45 个测试依赖手工启动的两个 bat，这是新人跑测试最大的坑，也阻塞 CI。两个方向：

- 优先：写一个测试基类（目前已有事实上的共享入口 `demo/App`，可在其 `Start()` 里）在进程内启动 `Zeze.Services.ServiceManagerServer` 和 `GlobalCacheManagerAsyncServer`（二者都是普通 main 类，可编程启动），JVM 退出时关闭。`GlobalRaft/TestGlobalCacheMgrWithRaft.java` 已有进程内 `new ServiceManagerServer` 的先例可参考。端口固定 5001/5002，本地端口冲突时用 JUnit `Assumptions` 跳过而不是失败。
- 次选：gradle `integrationTest` 任务用 `doFirst` 以后台 JavaExec 拉起两个进程、`doLast` 杀掉——可行但进程管理脆弱，不如进程内启动干净。

### 3. 统一命令行入口

`test_all.bat` 手工维护约 100 个类名，必然腐化。集成 gradle 后用 `gradle test --tests '*'` 取代；过渡期至少让 test_all.bat 改为扫描 class 文件而不是硬编码清单。

### 4. 其他

- 把本指南的"最小流程"3 行命令同步进 `README.md` 和 `AGENTS.md`（目前两处只列步骤不解释为什么）；
- `ZezeJavaTest/` 根目录散落的 `.zeze.pal`、`CommitRocks*`、`manager*/` 等运行时产物建议加入 .gitignore 并约定跑测试前清理；
- Zezex 三个测试各自复制了 `prepareNewEnvironment/stopAll`，可抽成公共 harness；
- `service & global.sync.bat` / `*.raft.bat` 等变体目前没有对应自动化测试使用，建议在脚本内注释说明各自服务于哪些手工场景。
