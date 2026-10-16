# ZezeJava 开发指南

## 运行 ZezeJavaTest

测试已在 `ZezeJavaTest` 的 **test 源集**（JUnit 6 / Jupiter，`src` 与 `Gen` 一起挂在 test 源集，二者互相引用不能拆分）。

```bat
:: 在 ZezeJava 目录下，只需 JDK 21，不需要任何手工启动的服务：
gradlew.bat :ZezeJavaTest:test             :: 快速自包含测试（@Fast 标注的类，无外部依赖）
gradlew.bat :ZezeJavaTest:integrationTest  :: 全量功能测试（自动在进程内启动 SM/GCM，不含fast和bench）
gradlew.bat :ZezeJavaTest:bench            :: 吞吐基准（@Bench 标注的类）
```

核心基准的跑法（benchCore/jmh/benchHeap/benchJvmExtra）、判定协议（3×stdev、同日 A/B）、
四时点档案与结论：[BENCHMARK.md](BENCHMARK.md)（单一真相源）。

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

test 任务同 JVM 类级并行（fixed 8），@Fast 类必须互不干扰。
serverId 号段与固定内存库 url 统一登记在 `harness/FastServerIds`，
由 `Zeze/TestFastAdmissionGuard` 检查冲突，不在本文件登记。

- serverId：用 `TakeoverTestEnv.newConf` 动态发号；固定号登记
  `seg(持有类, 基点, growth)` 并引用常量，growth 覆盖该类全量运行的最大发号数。
  `Application.start` 的 FileMutex 负责运行时冲突兜底。
- DatabaseMemory 按 url 共享存储：固定 url 用 `memUrl(...)` 常量；
  `"前缀_" + serverId` 随唯一号段派生，免登记。禁止裸 `new Config.DatabaseConf()`
  使用默认空串共享桶，也不要绕过登记私写 serverId/url 字面量。
- `setNoDatabase(true)`（含负 serverId）及无 Application 的组件不建缓存目录，
  serverId 免登记；固定端口、静态状态用 `@ResourceLock`/`@Isolated` 隔离。

## 测试类命名

类名用描述性行为名（常见词），不带 bug/审计/轮次编号（FndNNN、A2F1、Z1F2 之类）——
编号映射不入 git，对读者零信息；缺陷来龙去脉归提交信息。重名时加区分词，不靠序号。

## GCM 与后端同库约定

一个部署共用一个 GCM，所有 Application 与 GCM 必须使用同一个后端数据库，
以同库事务保证跨 app 的全局锁与缓存一致性。

存量测试常用各 app 独立的 DatabaseMemory，不能据此验证真实部署的重启恢复、
跨 app 锁一致性或回放语义。涉及多 app/GCM/持久化的测试，应为全部 app 与 GCM
显式配置同一个 DatabaseConf（同 url、同类型）；若仅验证独立内存库语义，
须在测试注释中声明。存量测试逐步迁移到同库配置。

## 空安全注解（JSpecify）

统一使用 JSpecify（`org.jspecify:jspecify`，ZezeJava 的 `implementation` 依赖）。
不要新增 JetBrains `@NotNull`/`@Nullable`；保留其 `compileOnly` 依赖及
仍在使用的 `@Contract`、`@UnmodifiableView`。

- 默认非空：包级 `package-info.java` 用 `@NullMarked`；默认包及不能共享
  package-info 的独立源集用类级标记，包括手写的 `Redirect_*.java`。
  可空字段、参数、返回值及泛型实参用 `org.jspecify.annotations.Nullable`。
- 保留实际空值语义：合法返回 null 的 getter 直接返回 `@Nullable` 原值，
  不为消除告警改成抛异常。只在解引用、拆箱等需要非空值的使用点检查，
  向接受 null 的方法透传时不提前检查。
- 书写优先 import 类型后用简单名，如 `private @Nullable Table logs;`。
  限定名的注解贴最后一段，如 `RocksDatabase.@Nullable Table`；
  数组引用可空写作 `ByteBuffer @Nullable [] buf`，前缀注解作用于元素。
- 生成代码遵循相同规则；工厂兜底及嵌套 Data 撞名时保留限定名最后段写法。
  修改 Gen 后运行根目录 `PublishGen.bat`、`gen_use_publish.bat`，不要手改生成文件。
- 验收：手写与生成代码自身 nullity 警告清零，容忍未标注第三方库产生的 unknown 警告。
  生成文件头不压制 `NullableProblems`，保留 `ConstantConditions`/`ConstantValue` 压制。


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
