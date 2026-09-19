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

坑：**完整包名+类名、且不带通配符**的形式（如 `--tests "UnitTest.Zeze.Component.TestToken"`
或 `...TestToken.testToken`）会误报 `No tests found for given includes`，即使类存在、标签正确。
简单类名 `TestToken`、通配符 `*TestToken`、`*pkg.*ClassName` 均正常。

另外类的标签必须匹配任务的标签过滤，否则通配符形式同样报 No tests found：
test 只跑 @Fast；integrationTest 只跑不带 fast/bench 标签的；bench 只跑 @Bench。

## @Fast 准入（并行安全）

test 任务类级并行（同 JVM），@Fast 类必须彼此互不干扰：

- **有库 App 的 serverId 必须唯一**。本地缓存目录 `zeze_cache_<serverId>` 每号一份，
  `Application.start` 对它先删后开——同号并发即 `delete failed: ...zeze_cache_N\LOCK`
  （Windows 下被打开的文件删不掉，重试 10s 后炸 start）。三选一：
  `TakeoverTestEnv.newConf` 式动态发号；固定空闲段字面量（查全景再选号）；
  `setNoDatabase(true)`（无库不建目录）。
- 选号两条铁律（2026-09-19 两处撞段实证）：**固定字面量不得落在他类计数器基点的
  增长范围内**（7353 撞 RankCacheEvict 第 4 实例、7360 撞 RankCountNeedKey）；
  **每类自带计数器若不共享，基点即撞点**（6 类各自从 1 起号互撞）。计数器基点
  间隔须 ≥ 该类 @Test 数。当前 7xxx 段：7150/7160/7250/7350/7360(固定)/7371(固定)/
  7410-7460/7470(固定)/7480/8790。
- dbhome、固定端口同理独占；只有 `Application.start` 且非 NoDatabase 才建缓存目录，
  净层组件（Service/Agent/MQManager/Dbh2 Master/RocksRaft/ServiceManagerWithRaft）不建。
- gradle 三池分治（fast 并行 / integration 串行 / bench）下默认 0 可能长期不撞纯属时序；
  IDEA"跑全部测试"是单 JVM 混跑并行，默认 0 的有库 App 必撞（2026-09-17
  testManagedPathFailFast / testManagedAddAllNoChangeReturnsFalse 假红即此，已迁 7070/7080）。

## 空安全注解

用 jetbrains 的 `@NotNull` / `@Nullable`（`org.jetbrains.annotations`），
不要用 jspecify 的 `@NonNull` / `@Nullable`（`org.jspecify.annotations`）。

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
