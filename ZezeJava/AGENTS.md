# ZezeJava 开发指南

## 运行 ZezeJavaTest

测试已在 `ZezeJavaTest` 的 **test 源集**（JUnit 6 / Jupiter，`src` 与 `Gen` 一起挂在 test 源集，二者互相引用不能拆分）。

```bat
:: 在 ZezeJava 目录下，只需 JDK 21，不需要任何手工启动的服务：
gradlew.bat :ZezeJavaTest:test             :: 快速自包含测试（@Fast 标注的类，无外部依赖）
gradlew.bat :ZezeJavaTest:integrationTest  :: 全量功能测试（自动在进程内启动 SM/GCM，不含fast和bench）
gradlew.bat :ZezeJavaTest:bench            :: 吞吐基准（@Bench 标注的类）
```

## 单类/单方法验证（--tests）

**用通配符（或简单类名）形式**，三个测试任务通用：

```bat
gradlew.bat :ZezeJavaTest:test --tests "*TestToken"              :: 单类（类需 @Fast）
gradlew.bat :ZezeJavaTest:test --tests "*TestToken.testToken"    :: 单方法
gradlew.bat :ZezeJavaTest:integrationTest --tests "*TestCsQueue" :: 单类（类不带 @Fast/@Bench）
gradlew.bat :ZezeJavaTest:bench --tests "*DiffLockAndNoLock"     :: 单类（类需 @Bench）
```

坑：**完整包名+类名、且不带通配符**的形式（如 `--tests "Zeze.Component.TestToken"`
或 `...TestToken.testToken`）会误报 `No tests found for given includes`，即使类存在、标签正确。
简单类名 `TestToken`、通配符 `*TestToken`、`*pkg.*ClassName` 均正常。

另外类的标签必须匹配任务的标签过滤，否则通配符形式同样报 No tests found：
test 只跑 @Fast；integrationTest 只跑不带 fast/bench 标签的；bench 只跑 @Bench。

## @Fast 准入（并行安全）

test 任务同 JVM 类级并行（fixed 8），@Fast 类必须彼此互不干扰。约束由代码承载：
`harness/FastServerIds` 号段桌是 serverId/url 全景的唯一真相，
`UnitTest/Zeze/TestFastAdmissionGuard` 自检区间两两不重叠——本文件不再登记号段。

需要 serverId 的 @Fast 测试三选一：

- `TakeoverTestEnv.newConf` 动态发号（桌已预留 100-199 段）。
- 固定号：在 FastServerIds 加一行 `seg(持有类, 基点, growth)`，站点引用桌常量。
  growth=该类一次全量运行的最大发号数（≈@Test 数×发号调用点）；growth 低估时
  `Application.start` 的 FileMutex fail-fast（先锁后删 `zeze_cache_<serverId>`，
  同段并发必炸）是运行时兜底。
- `setNoDatabase(true)`（含负 serverId）不建缓存目录，免登记。

DatabaseMemory 同 JVM 按 url 静态分桶，同名 url 的库实例静默共享存储串数据：
固定字面量 url 须登记为 FastServerIds 的 `memUrl(...)` 常量（guard 查重）；
`"前缀_" + serverId` 派生式随号段唯一，免登记；裸 `new Config.DatabaseConf()`
落默认空串共享桶，禁止。

净层组件（无 Application：Agent/OnzServer/Daemon/GCM/SM 等）不建缓存目录，
serverId 不入桌；固定端口与静态状态用 `@ResourceLock`/`@Isolated` 串行隔离。
绕过桌私写 serverId/url 字面量无静态拦截，防线=FileMutex 运行时兜底+评审时
本地字面量是 grep 异常（迁移后全树仅桌内出现号段数字）。

判例：2026-09 两轮人工选号/迁号仍留 10 处同段互撞（含迁移批自身回撞、计数器
基点落进动态池预留段），故收口为机械守门。

## GCM 与后端同库约定

**体系约束：一个部署里 GCM（全局缓存管理器）是同一个，且所有 Application 与 GCM
的后端必须是同一个数据库**——跨 app 的全局锁/缓存一致性由同库事务保证，这是 zeze
多 app 协作（Simulate 式跨 app 场景、History 回放、gid 序）的地基。

**测试现状违反此约定**：测试普遍 `new Config()`（默认库）或各自
`setDatabaseUrl("test_xxx")` 的 DatabaseMemory——每个 app 一个独立内存库，GCM 状态与
app 表不在同一个库里。**后果：持久化语义与跨 app 语义在当前测试配置下不验证真实拓扑**
（重启恢复、跨 app 锁一致性、Simulate 回放等的结论只对"每 app 独立内存库"成立）。

写涉及多 app/GCM/持久化的测试时必须显式意识到这一局限；需要真实验证时：让全部
app 与 GCM 显式配置**同一个 DatabaseConf**（同 url 同类型），或在测试注释里明确声明
"本用例仅在独立内存库语义下成立"。存量测试迁移到同库配置是系统性技术债，逐步偿还。

## 空安全注解

用 jetbrains 的 `@NotNull` / `@Nullable`（`org.jetbrains.annotations`），
不要用 jspecify 的 `@NonNull` / `@Nullable`（`org.jspecify.annotations`）。

## 持久化写入规约（I1，RFD1-03）

任何"重启后必须还在"的文件只能经 `Zeze.Util.AtomicFileWriter`
（`openOutput` 流式 / `replace` 全量小文件；分块接收等按路径写的候选用
`AtomicFileWriter.fsync` + 原子改名收口）落盘。截断式写（`new FileOutputStream`、
不带 APPEND 的 `Files.write/newOutputStream` 等）在白名单外没有合法场景；
白名单由 `TestAtomicWriteSourceGuard` 固化，调整须在提交信息里说明理由，
迁移完成一个调用点即收缩一项。分块接收（`.installing`）除外。
`AtomicOutputFile.close()` 内 force-先于-move 的顺序是安全前提，改动须逐字评审。

## 修复提交的信息格式

一个 bug 一个提交。格式：

```
<类别>：<符号> <缺陷本质>，<后果>

问题：
- 证据：位置、触发路径、线程模型。

修复：
- 关键修法。

验证：
- 真实证据：测试命令+结果 / 编译门禁 / 具体核查；没跑过的不写。
```

- 类别用模块名（transaction/util/raft/net/dbh2/game…），不带编号；主题不写修法。
- bullet `- ` 结尾带"。"，续行缩进两空格，约 64 列换行。
- 保持简洁易读，不要很长一段
