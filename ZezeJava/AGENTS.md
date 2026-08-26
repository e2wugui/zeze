# ZezeJava 开发指南

## 运行 ZezeJavaTest

测试已在 `ZezeJavaTest` 的 **test 源集**（JUnit 6 / Jupiter，`src` 与 `Gen` 一起挂在 test 源集，二者互相引用不能拆分）。

```bat
:: 在 ZezeJava 目录下，只需 JDK 21，不需要任何手工启动的服务：
gradlew.bat :ZezeJavaTest:test             :: 快速自包含测试（@Fast 标注的 45 个类，无外部依赖）
gradlew.bat :ZezeJavaTest:integrationTest  :: 全量功能测试（自动在进程内启动 SM:5001/GCM:5002，不含基准）
gradlew.bat :ZezeJavaTest:bench            :: 吞吐基准（Benchmark 包整体）
```

- `integrationTest` 通过 `harness.TestEnvLauncherListener`（JUnit LauncherSessionListener，ServiceLoader 注册）在测试 JVM 内启动 ServiceManager(5001) 与 GlobalCacheManagerAsyncServer(5002)，会话结束自动关闭；若端口已被手工 bat 占用则直接复用。
- 例外：`Onz.TestOnz` 额外需要第二对服务 5011/5012（GCM 是进程内单例，无法自动启动第二对），未启动时该测试自动跳过。需要跑它时先运行 `test/service & global.another.bat`。
- IDEA：打开 `ZezeJava` 目录，右键 `ZezeJavaTest/src` → "Run 'All Tests'"（launcher listener 同样生效，无需手工 bat）。
- 测试工作目录必须是 `ZezeJava/ZezeJavaTest`（gradle 任务已配置 `workingDir`；测试用相对路径加载 `./zeze.xml`，并读写 `dbhome/`、`autokeys/` 等目录）。
- （可选）改了 solution.xml/demo2.xml 需重新生成代码时，运行仓库根目录 `gen_use_publish.bat`。

### 测试分层约定

- 自包含测试（不依赖外部进程/数据库）标注 `@Fast`（`harness.Fast`，即 `@Tag("fast")`），由 `gradle test` 执行。
- **`gradle test` 是类级并行的**（`junit.jupiter.execution.parallel`，个别类 `@Execution(CONCURRENT)` 方法级并行，仅 test 任务开启）。因此 `@Fast` 准入除"自包含"外还要求**彼此互不干扰**：固定端口独占（现有 fast 类端口：TestRpc=5000、TestToken=5003；`TestTokenKeepAlive` 也用 5003 但在 integrationTest，串行不冲突）、本地目录独占、无静态状态竞争（`Task.tryInitThreadPool` 有锁幂等是安全先例）、**@Test 方法必须非 static**（Jupiter 会静默忽略 static @Test，覆盖悄悄丢失）。违反时失败是间歇性的，很难查。
- **吞吐基准标注 `@Bench`**（`harness.Bench`，等价 `@Tag("bench")`，Benchmark 包整体），由 `gradle bench` 执行，`integrationTest` 按标签排除。识别特征：无断言、靠 `Benchmark()/report()` 打印 M/s 或耗时——自包含 ≠ 该进快速车道。
- **新测试不打 tag 默认归入 `integrationTest`**（环境更全的桶）——忘打 tag 不会打破 `gradle test` 开箱即绿。
- 外部 DB 测试（MySQL/PG/Mongo/SqlServer/TiKV）靠主机名门控自动跳过，见 `ZezeJava/README.md` 测试分类盘点。
- `integrationTest` / `bench` 不要开并行：`demo.App` 是 JVM 级单例、测试共享 `dbhome/` 与 RocksDB LOCK、`App.Start()` 绑定 10000 端口，同 JVM 并行或 fork 并行都会互相干扰。
