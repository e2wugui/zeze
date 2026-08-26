# ZezeJava

Zeze 的 Java 实现:基于一致性缓存的分布式事务应用框架。框架的概念与设计见[主仓库 README](../README.md)与[文档站](https://e2wugui.github.io/zeze/),本文只介绍 ZezeJava 的使用。

## 目录

| 目录 | 说明 |
|---|---|
| `ZezeJava` | 框架本体(artifact: `com.zezeno:zeze-java`) |
| `ZezeJavaTest` | 测试与使用示例(`src/demo` 是完整的 App 示例) |
| `ZezexJava` | client / linkd / server 三进程示例工程 |
| `ZokerManager` | 日志查询管理服务 |
| `test` | 手工测试场景脚本(bat) |
| `scripts/cue` | 监控配置 |

## 环境要求

- **JDK 21**(gradle toolchain,编译与测试均使用)
- Gradle 无需单独安装(用自带 `gradlew.bat`)
- 代码生成器 `Gen.exe` 仓库已带(根目录 `publish/Gen.exe`)

## 在自己的项目中使用

```xml
<!-- 版本以 https://mvnrepository.com/artifact/com.zezeno/zeze-java 为准 -->
<dependency>
    <groupId>com.zezeno</groupId>
    <artifactId>zeze-java</artifactId>
    <version>1.6.3</version>
</dependency>
```

新项目建议直接从官方模板开始:<https://gitee.com/dwing/zezeboot>

要使用自己编译的版本:在 `ZezeJava/`(框架模块,即 pom.xml 所在目录)运行 `mvn install -Dgpg.skip=true` 安装到本地 maven 仓库,然后依赖 `com.zezeno:zeze-java:1.7.0-SNAPSHOT`。

## 快速上手

使用 Zeze 的流程:**在 `solution.xml` 里定义数据 → `Gen.exe` 生成访问代码 → 在事务过程中读写数据**。内存与数据库的同步、并发控制、回滚重试都由框架完成。

**1. 定义数据**(`solution.xml`):

```xml
<solution name="demo" ModuleIdAllowRanges="1-100">
    <module name="Module1" id="1">
        <bean name="BValue">
            <variable id="1" name="Name"  type="string"/>
            <variable id="2" name="Money" type="long"/>
        </bean>
        <table name="tValue" key="long" value="BValue"/>
    </module>
</solution>
```

**2. 生成代码**:在该目录运行 `Gen.exe solution.xml`,生成 bean、table 访问与协议处理代码(建议挂到自己项目的构建流程里)。

**3. 启动并执行事务**:对 Zeze 数据的所有读写都必须放在事务过程中——异常自动回滚,冲突自动重试:

```java
var config = Zeze.Config.load("zeze.xml");
var zeze = new Zeze.Application("demo", config);
zeze.start();

long result = zeze.newProcedure(() -> {
    var v = demo_Module1.getTValue().getOrAdd(1L);
    v.setMoney(v.getMoney() + 100);
    return Zeze.Transaction.Procedure.Success; // 返回 0 表示成功,非 0 自动回滚
}, "AddMoney").call();
```

**4. 配置**(`zeze.xml`):数据库(`DatabaseType` 支持 Memory / RocksDB / MySql / PostgreSQL / SqlServer / MongoDB / Tikv)、GlobalCacheManager 地址、表缓存容量等;各项含义见 `ZezeJava/zeze.xml` 内的注释。

完整示例(含网络服务、模块绑定、多进程组网)见 `ZezeJavaTest/src/demo` 与 `ZezexJava`。

## 构建

在本目录(`ZezeJava/`)下:

```bat
gradlew.bat build              :: 构建所有模块
gradlew.bat :ZezeJava:copyJar  :: 把框架 jar 及依赖同步到 ZezeJava/lib
```

## 运行测试

测试在 `ZezeJavaTest`,三条车道,均在 `ZezeJava` 目录下运行:

```bat
gradlew.bat :ZezeJavaTest:test             :: 快速自包含测试(@Fast 标注),约 7s
gradlew.bat :ZezeJavaTest:integrationTest  :: 全量功能测试(自动进程内启动服务,无需手工准备)
gradlew.bat :ZezeJavaTest:bench            :: 吞吐基准(打印 M/s 观察,不含功能断言)
```

- **不需要准备 MySQL/MongoDB/TiKV/RocketMQ 等任何外部依赖**:相关测试会自动跳过。
- **不需要手工启动任何服务**:`integrationTest` 会在测试 JVM 内自动启动 ServiceManager(5001)与 GlobalCacheManagerAsyncServer(5002),以及第二对服务(5011/5012,仅 `Onz/TestOnz` 使用的独立集群),结束自动关闭。
- **Zezex 系测试需要先发布一次 hot 模块**(新机器/清理过 `ZezexJava/server/hot` 时):在 `ZezexJava` 目录运行 `distribute.bat`。该目录是 gitignore 的构建产物,缺失时 Game.Login 等模块不加载,相关测试在 GetRoleList 等 RPC 上超时。
- IDEA:打开 `ZezeJava` 目录,右键 `ZezeJavaTest/src` → "Run 'All Tests'"。
- `--tests` / `includeTestsMatching` 模式过滤在本项目不工作,按 JUnit 标签过滤正常;跑单个类请用 IDEA。

## 代码生成

- `ZezeJava/gen.bat`:改了框架自带的 `solution.zeze.xml` 后运行,重新生成框架内置结构(协议、表等)。
- `ZezeJava/genRedirect.bat`:改了代码中 `@Redirect` 相关注解后运行。
- 仓库根目录 `gen_use_publish.bat`:全量重新生成(confcs、框架、ZezeJavaTest、ZezexJava、python),改了 `solution.xml`/`demo2.xml` 后使用。
- 生成的代码都已提交 git,日常使用方不需要运行这些脚本。

## 内置服务

框架自带两个基础服务,分布式部署时需要先启动:

- **ServiceManagerServer**(默认端口 5001):服务发现与注册。
- **GlobalCacheManagerAsyncServer**(默认端口 5002):跨进程缓存一致性协调。

手工启动的方式(任选其一):

```bat
ZezeJava\global&service.bat                    :: Windows 脚本,一次启动两个服务
gradlew.bat :ZezeJava:startServiceManager      :: 或用 gradle 任务单独启动(跨平台)
gradlew.bat :ZezeJava:startGlobalCacheManagerAsync
```

`test/` 下还有 raft / sync 等变体启动脚本,服务于对应的手工测试场景。单机跑测试时不需要手工启动(见上文"运行测试")。

## 更多文档

文档站:<https://e2wugui.github.io/zeze/>(源码在仓库 `docs/` 目录)。常用章节:

- [快速开始](https://e2wugui.github.io/zeze/getting-started/quick-start/) / [solution.xml](https://e2wugui.github.io/zeze/core/solution-xml/) / [事务](https://e2wugui.github.io/zeze/core/transaction/) / [表](https://e2wugui.github.io/zeze/core/table/)
- 架构:Online / Provider / Redirect / 服务管理
- 进阶:热更新 / Raft / 数据库映射 / 性能与指标
