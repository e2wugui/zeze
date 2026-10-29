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

## RocketMQ 联调

`Zeze.Misc.TestRocketMQ` 默认 `@Disabled`，需要 RocketMQ broker 和持久数据库。手动启用该类后，可通过上面的数据库参数准备共享 demo 应用，再运行：

```powershell
.\gradlew.bat :ZezeJavaTest:integrationTest --tests "*TestRocketMQ" -PzezeTestDatabase=PostgreSQL '-PzezeTestDatabaseUrl=jdbc:postgresql://localhost:5432/devtest?user=dev&password=...'
```

也可在测试 XML 中直接配置持久后端。生产 `Producer.start()` 始终拒绝把事务回查表 `tSent` 放在 Memory 库；真实启动的测试须遵守同一约束。

不连接 broker 的 RocketMQ 回归通过 `extraTest --tests "*.RocketMQ.*"` 执行：仅构造包装器的测试继续使用隔离 Memory 桶；启动校验的正向用例由测试工程提供临时 RocksDB，并用 SDK 客户端替身验证校验通过后到达客户端启动边界。
