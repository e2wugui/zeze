# S2：锁协议与有条件推进

模型从真实源码抽象，不修改生产实现。Java对照类为 `ZezeJavaTest/src/Zeze/Transaction/TestFormalLockProtocol.java`。

## 复现

从仓库根目录运行（Python 3、JDK21）：

```powershell
python ZezeJava/formal/locks/check.py --java 'C:\Program Files\Eclipse Adoptium\jdk-21.0.13+9\bin\java.exe'
```

只跑一项可加 `--case continuous-scheduled-starvation`，可重复指定；每项默认300秒超时，超时算未完成。正常退出码0；预期不变量反例要求TLC退出12且命中指定性质；预期活性反例要求退出13且日志含 temporal violation。负对照出现预期反例才算验证设施通过，不能将其称为真实源码缺陷。

工具使用 [工具说明](../serializability/README.md#工具与运行) 中的共用固定版本。入口检查完整性，固定单worker、seed=1、fp=0，每项独立metadir；每项正常完整结束才计通过。运行日志与结果在 `build/` 本地生成，不纳入版本控制。选择性重跑不覆盖完整结果索引。

真实 Java 检查使用共享mutex入口，避免并行Gradle覆盖产物：

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.13+9'
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\ZezeJava\formal\locks\run-java.ps1
```

脚本仍只通过共用 `run-gradle.ps1` 构建；初始化脚本仅将本次XML/HTML报告写至本目录 `build/java-xml`、`build/java-html`，避免后续并行聊天覆盖共同测试报告。11个新增测试使用已预留的 `TEST_FORMAL_LOCK_PROTOCOL` 范围7950–7969（8个Application，3个纯锁测试），每个Application分配独立ID/Memory URL，无外部服务。这里测试的是单Application本地锁；多Application不能借同一个模型假定共享Locks实例。

## 模型分工

| 模块 | 内容 | 不在本模块中的事实 |
|---|---|---|
| `LockProtocol.tla` / `Models.tla` | 排序读写锁、冲突继续补锁、保留/释放重试、后缀释放、升级、异常/耗尽终局释放 | 版本值、串行历史、GCM通信、Record身份变化 |
| `RecordNesting.tla` | Lockey→Record，与 Record→即时tryLock/copy回退交错 | 本地镜像值正确性、数据库I/O |
| `CrossLayers.tla` | 提交 Lockey→RRS→Record，与降级 Lockey→Record→RRS，同/异主键与共享RRS组 | RRS动态合并、多组ID排序、跨JVM应答 |
| `Barging.tla` | 有限竞争与持续新请求，唤醒后竞争、插队、显式返回 | AQS内部队列逐字段精化、概率/性能上界 |

`CHECK_DEADLOCK FALSE` 只允许正常终态继续stutter。安全配置独立检查 `NoWaitCycle`/`NoCrossLockCycle`/`NoCrossLayerCycle`，并以 `NoBlockedWork` 判断“仍有未结束工作却没有任何可执行动作”；因此没有把关闭TLC默认deadlock检测当作无死锁证据。活性配置另用弱公平性和temporal property检查，并有阻断推进的负对照。

本地锁的批量释放被收缩成一个动作：中间步骤只有释放、不请求新资源，隐藏这些交错不会建立新的等待环；它不是Java机械精化证明。Record和RRS模型采用独立接口边界，也不能由各自通过直接推出全部组合已证明。

## 公平性来源

1. `FiniteSpec` 每个有限事务的 `WF(Step(t))`：线程持续可运行时最终执行下一步；业务、检查、回调、I/O和RPC本身需返回。获取锁不一直可用时不保证获取；有限竞争者结束并释放后，等待者可持续运行，才由此推进。它是明确的环境条件，不是JVM无条件承诺。
2. `RecordNesting.LiveSpec` 的 `WF(Txn)`、`WF(Copy)`：两个有限参与者的可运行步骤最终执行，copy回退/拷贝和临界区有限结束。没有加入强公平取锁。
3. `CrossLayers.LiveSpec` 的 `WF(Step(a))`：有限commit/reduce线程可运行时推进，RRS临界区/刷盘最终返回；这里不验证网络与数据库响应。
4. `Barging.LiveSpec` 的 `WF(Wake)`：锁持续空闲且有等待者时，唤醒最终送达；`WF(VictimTry)`：被唤醒线程最终运行，**仍可获取失败**；`WF(VictimReturn)`、`WF(Release)`：持有者执行有限工作后释放；`WF(Arrive)`、`WF(Barge)`：持续环境中的新请求及可运行竞争线程推进。条件只为刻画选定环境，并非声称Java调度规范提供形式公平性。

未假设SF(获取)、FIFO交接、“等待者最终总拿到锁”或“冲突最终一定消失”。连续竞争的活性反例在以上全部WF条件下仍成立；加强反例还要求 `[]<>(victimMiss)`，排除了等待者从未再次运行这一解释。
