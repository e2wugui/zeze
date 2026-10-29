# 测试数据库选择

默认使用测试 XML 中的 Memory 配置。需要让共享测试应用改用 JDBC 数据库时，在 `ZezeJava` 目录运行，例如：

```powershell
.\gradlew.bat :ZezeJavaTest:integrationTest --tests "*TestOnline" -PzezeTestDatabase=MySql '-PzezeTestDatabaseUrl=jdbc:mysql://localhost:3306/devtest?user=dev&password=...'
.\gradlew.bat :ZezeJavaTest:integrationTest --tests "*TestOnline" -PzezeTestDatabase=PostgreSQL '-PzezeTestDatabaseUrl=jdbc:postgresql://localhost:5432/devtest?user=dev&password=...'
```

`build.gradle` 将两个参数传入测试 JVM 的 `zeze.test.database` 和 `zeze.test.database.url`。IDEA 或直接运行测试程序时，也可以用同名 `-D` 参数。

后端选择由测试工程的 `harness.TestDatabaseConfig.apply(config)` 完成，必须在创建 `Application` 之前调用。`demo.App.Start`、`demo.SimpleApp` 和 `ZezexTestEnv` 已接入，使用这些入口的集成测试和基准测试沿用同一开关。`ZezexTestEnv` 向 `Game.App.Start(Config, ...)`、`Zezex.App.Start(Config, ...)` 传入准备好的配置；这些启动重载接受普通配置，不读取测试属性。

助手仅切换空 URL 的 Memory 配置，保留数据库名、分布式事务和 `DisableOperates` 等原配置，同时设置 JDBC 驱动和测试连接池参数。独立 Memory 桶及其他后端保持原配置。选择了 JDBC 后端但未提供 URL 时立即报错。

Memory 语义测试直接创建 `Config`，不调用该助手；使用共享测试应用但需要独立 Memory 存储时，应在启动前设置独立的 Memory URL。同一部署的多个应用须使用一致的后端和 URL。

生产 `Config.createDatabase` 始终按传入配置创建数据库，不读取测试系统属性。`setDatabaseType` 仅设置类型，不记录调用历史。

## JDBC 适配器集成测试

`TestDatabaseMySql`、`TestDatabasePostgreSQL`、`TestDatabaseSqlServer` 使用同一组真实数据库契约，
在独立的 `jdbcTest` 任务中显式运行，不进入默认 `integrationTest`，也不启动 SM/GCM：

```powershell
.\gradlew.bat :ZezeJavaTest:jdbcTest -PzezeTestDatabase=MySql '-PzezeTestDatabaseUrl=jdbc:mysql://localhost:3306/devtest?user=...&password=...&connectTimeout=5000&socketTimeout=10000'
.\gradlew.bat :ZezeJavaTest:jdbcTest -PzezeTestDatabase=PostgreSQL '-PzezeTestDatabaseUrl=jdbc:postgresql://localhost:5432/devtest?user=...&password=...&connectTimeout=5&socketTimeout=10'
.\gradlew.bat :ZezeJavaTest:jdbcTest -PzezeTestDatabase=SqlServer '-PzezeTestDatabaseUrl=jdbc:sqlserver://localhost:1433;databaseName=devtest;user=...;password=...;encrypt=true;loginTimeout=5;socketTimeout=10000'
```

共享测试应用的数据库选择也支持 `SqlServer`。三个驱动由测试运行时依赖提供；SQL Server 本地自签名证书
环境可在测试连接串中显式设置 `trustServerCertificate=true`。连接串通过命令行或 IDE 的同名系统属性传入，
不再根据开发者主机名选择地址，也没有内置账号密码。

未配置后端或未选中该后端时，用例显示为跳过；选择了后端但缺少 URL、URL 类型不匹配、驱动缺失、
连接失败时均报失败，不能通过静默 return 假装通过。只提供 URL 而不指定后端同样报配置错误。

每个用例创建独立的 `zeze_jdbc_<随机值>` 表，验证提交后查询及遍历内容、覆盖已有值、回滚新增/更新/删除、
提交删除和超长键拒绝。结束时删除本用例的表并关闭连接池；运行账号需要测试库中的建表、读写及删表权限。
测试不创建 Application，禁用共享的 `_Zeze*` 元数据初始化，只验证 KV 适配器契约。
SQL Server 的存储过程调用占位符检查保留为 `@Fast` 测试，由默认 `test` 任务执行。

## RocketMQ 联调

`Zeze.Misc.TestRocketMQ` 默认 `@Disabled`，需要 RocketMQ broker 和持久数据库。手动启用该类后，可通过上面的数据库参数准备共享 demo 应用，再运行：

```powershell
.\gradlew.bat :ZezeJavaTest:integrationTest --tests "*TestRocketMQ" -PzezeTestDatabase=PostgreSQL '-PzezeTestDatabaseUrl=jdbc:postgresql://localhost:5432/devtest?user=dev&password=...'
```

也可在测试 XML 中直接配置持久后端。生产 `Producer.start()` 始终拒绝把事务回查表 `tSent` 放在 Memory 库；真实启动的测试须遵守同一约束。

不连接 broker 的 RocketMQ 回归通过 `extraTest --tests "*.RocketMQ.*"` 执行：仅构造包装器的测试继续使用隔离 Memory 桶；启动校验的正向用例由测试工程提供临时 RocksDB，并用 SDK 客户端替身验证校验通过后到达客户端启动边界。

## Takeover 致命退出

```powershell
.\gradlew.bat :ZezeJavaTest:test --tests "*TestTakeover*" --tests "*TestTimerTakeoverVersion"
.\gradlew.bat :ZezeJavaTest:integrationTest --tests "*TestTakeoverTimerDemoApp"
```

`TestTakeoverFence` 将 Queue、Timer 写路径 fencing、注册 scope 丢失租约和续租丢失租约分别放到独立子 JVM，调用生产代码固定的 `System.exit(-1)`。父测试同时校验准备完成标记、目标分支的致命日志、退出码和停机完成标记，子进程超时会被终止并回收，启动失败不能算作致命退出通过。

每个子进程独占临时工作目录、本地缓存和 Memory 桶，不需要外部数据库。测试专用的停机回调在应用关库前调用真实 `Takeover.release()` 并核对租约 epoch 和到期时间均未改变；Queue、Timer 场景还保留同 epoch 的租约，确保确实验证 `fenceFatal` 保护。此回调只观察停机，不替换退出动作。

正常 release/start 的重新登记和宽限期刷新由 `TestTakeoverLifecycle` 单独验证。墓碑复活和零 epoch 自愈也在子 JVM 中验证正常完成，以免回归导致整个 JUnit worker 退出。生产代码没有测试退出开关。

## Raft 致命退出与 SM/GCM 测试

```powershell
.\gradlew.bat :ZezeJavaTest:test --tests "*TestEndReceiveInstallSnapshot*" --tests "*TestLeaderApplyFatalKill" --tests "*TestInstallSnapshotDelayedOwner"
.\gradlew.bat :ZezeJavaTest:test --tests "*TestServiceManagerWithRaft*" --tests "*TestGlobalCacheManagerRaft*" --tests "*TestRaftUniqueNamesLimit" --tests "*TestSmRaftCleanupNotifyAfterCommit" --tests "*TestServiceManagerRaftSessionOwnership" --tests "*TestServiceManagerUnsubscribeOrdering"
.\gradlew.bat :ZezeJavaTest:extraTest --tests "*TestRecoveredAppliedPrefixProtection"
```

原先替换 Raft 致命动作的测试现使用测试工程的 `@ForkedJvm`。父 JUnit 只负责启动、检查结果和回收子进程；每个测试方法及其原有 `BeforeAll/BeforeEach/AfterEach/AfterAll` 在独立子 JVM 中运行。工作目录、测试 XML 副本、数据库和临时文件位于 `build/forked-tests/` 下，成功后清理，失败时保留日志和数据。正常场景要求目标方法和完整生命周期通过且退出码为 0，任何意外 halt 都会使父测试失败。

`@ForkedJvm.ExpectHalt` 用于快照装载失败、leader apply 分歧和已提交日志冲突三个场景。测试在真实 `addAtFatalKill()` 回调中检查关键状态，父测试要求目标致命日志、回调断言完成标记和 `halt(-1)` 对应的退出码，同时检查致命调用没有继续返回、JVM shutdown hook 没有执行。已提交前缀冲突场景在 halt 后再启动验证子 JVM，重开同一临时持久库，核对业务数据、应用水位与日志前缀仍然完整。

SM/GCM 用例直接构造真实服务，不再通过反射替换退出动作。生产 `Raft.fatalKill()` 始终执行原有 fatal 回调、日志库关闭、日志系统关闭及 `Runtime.halt(-1)`；`addAtFatalKill()` 保留正常生产用途。子进程身份只由测试启动器内部状态识别，生产代码没有测试开关。
