# 单 JVM 事务可串行化验证

本目录保留模型和重现入口，维护者可以直接运行检查。

## 验证对象

验证默认 `TransactionLevel.Serializable` 的顶层事务核心：按主键访问、记录时间戳、事务内写日志、逐记录读写加锁与验证、逐记录发布值与时间戳、冲突重试，以及重试保留锁后的升级和锁序修复。

一条记录抽象为一个标量，`-1` 表示记录不存在，`-2` 仅用于表示没有暂存写入。每笔事务在每轮尝试执行一个有限的读写程序。程序可以有不同的读写集；锁升级和插入更早主键的配置还会在重试时改变程序。

模型把首次取得时间戳、后续读取值、加锁、版本检查、发布值、更新时间戳拆成独立动作。不同记录的发布与时间戳更新允许任意交错，只要求该记录的值先于其时间戳发布。它没有把整个提交规定成一个原子动作。

`Load` 只保存时间戳，后续共享值读取允许读取当前值。这比实际 `AtomicTupleRecord` 保存 Bean 引用更宽松；旧 Bean 引用可能保持旧值，而模型可以看到当前值。这个抽象适用于本轮记录身份稳定、标量读写的论证，不覆盖 Bean 别名、集合和记录重载。

## 检查标准

- `CommitOrderSerializable`：按逻辑完成提交的顺序，把每笔成功事务实际观察到的读取和暂存写入连续回放，每一次读取都必须等于串行存储中的值。回放包含事务自身之前的写入。锁与时间戳不参与回放判定。
- `QuiescentDataMatches`：没有正在发布的事务时，实际存储等于上述串行回放结果。
- `TypeOK`：阶段、锁、版本、提交身份等基本状态约束。

按提交顺序成立是可串行化的充分条件。模型不强迫执行按这个顺序发生；这个顺序只用于检查结果。它没有记录客户端调用与应答时刻，因此本轮不声称验证了严格可串行化。

失败的尝试不进入成功历史。模型允许它们临时读到混合状态；不检查 opacity，也不保证外部副作用能随重试回滚。

`CHECK_DEADLOCK FALSE` 是有意选择：本轮仅检查安全性，有限程序结束、重试耗尽都可能没有后继动作。可达性见证证明成功提交、保留/释放锁、升级和补锁的部分执行确实存在，不代表活性或无死锁证明。

## 工具与运行

使用 Python 3 标准库和 Java 11+；仓库 Java 测试仍要求 JDK 21。固定 TLC 为官方 TLA+ `v1.7.4`：

- [官方下载](https://github.com/tlaplus/tlaplus/releases/tag/v1.7.4)

在本目录准备工具：

```powershell
New-Item -ItemType Directory -Force build/tools | Out-Null
Invoke-WebRequest -UseBasicParsing -Uri 'https://github.com/tlaplus/tlaplus/releases/download/v1.7.4/tla2tools.jar' -OutFile build/tools/tla2tools.jar
python -B check.py
```

检查器每次验证 JAR 的 SHA-256，不在检查期间联网。JAR 和 TLC 状态文件都在忽略的 `build/` 下，不提交二进制。

指定 Java、只跑一个配置或限制单次检查时间：

```powershell
python -B check.py --java 'C:/Program Files/Eclipse Adoptium/jdk-21.0.13+9/bin/java.exe'
python -B check.py --case generic-programs --timeout 300
```

每个配置使用一个 TLC worker，固定 seed 和指纹多项式。正常配置只有 TLC 返回 0 且明确报告完整检查成功才通过；反例配置必须返回 12 且违反指定不变量，语法错误、其他错误或超时都不能算通过。

运行输出：

- `build/results.json`：完整运行结果、状态数、耗时与配置哈希。
- `build/results-metadata.json`：代码基线、模型与检查脚本哈希、JDK 和 TLC 身份。
- `build/<配置>/model.cfg`、`tlc.log`：实际配置、原始日志及反例时序。
- 筛选运行写入 `selected-results*.json` 和 `build/selected/<配置>/`，不会覆盖完整结果及其日志。

## Java 对照测试

在 `ZezeJava` 目录、JDK 21 下运行：

```powershell
.\gradlew.bat :ZezeJavaTest:test --tests '*TestSerializableCommitHistories' --tests '*TestFastAdmissionGuard'
.\gradlew.bat :ZezeJavaTest:integrationTest --tests '*TestTransactionLevelSerializable'
```

新增测试不启动 GCM/SM，使用独立 serverId 和 Memory URL。时序由 latch 控制，不靠 sleep 碰撞。所有 latch 与 Future 都有等待时限。

## 扩展顺序

多字段、嵌套保存点及异常/冲突回滚见 [rollback](../rollback/README.md)；Record身份变化见 [record-lifecycle](../record-lifecycle/README.md)。

更一般的业务程序、更大配置与 TLAPS 证明，围绕各阶段已经稳定的模型按需扩展。
