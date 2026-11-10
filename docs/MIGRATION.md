# ZezeJava v1.6.3 → v2.0.x 迁移指南

面向使用 zeze-java 的应用工程，覆盖 v1.6.3 → v2.0.6（含 2.0.7-SNAPSHOT 已知事项）。逐版本变更明细见 [CHANGELOG.md](CHANGELOG.md)；本指南只讲"升级时你要做什么"。

按约定，热更（HotManager）、History 增量日志、Raft 领域的兼容性变化不在本指南范围。

## 1. 总览：断点在哪

v2.0.0 是主断点（JDK、依赖、API 家族、线格式），v2.0.x 各版本另有增量断点：

| 断点 | 引入版本 | 类型 | 章节 |
|---|---|---|---|
| 最低 JDK 21（Maven 口径） | 2.0.0 | 环境 | §2 |
| Netty 4.1 → 4.2（含 io_uring） | 2.0.4 | 依赖 | §2 |
| Redirect RPC 线格式不兼容 | 2.0.0 | 线格式 | §7 |
| Task/OneByOne/Timer/Online 旧 API 废弃 | 2.0.0 | API（编译告警） | §3.1 |
| 已删除 API / 模块一批 | 2.0.0–2.0.6 | API（编译失败） | §3.2 |
| KV key 上限收紧 2712→900 | 2.0.0/2.0.3 | 数据 | §7 |
| 内建 Bag（tbag）表键改造 | 2.0.1 | 数据迁移 | §7 |
| CheckpointPeriod 必须 >0 | 2.0.2 | 配置 | §4 |
| DbWeb / RunClass / ReloadClass 强制 token | 2.0.2/2.0.3 | 运维 | §5 |
| HasManagedException 改 Abort 语义 | 2.0.4 | 行为 | §6 |
| SM Identify wire 不兼容（SM 与 GS 须同版本） | 2.0.4 | 线格式 | §7 |
| Onz 协调决策默认 sync WAL | 2.0.6 | 行为 | §6 |

建议直接升级到 2.0.6，按下面清单一次性核对。

## 2. 环境与依赖

**JDK**：构建与运行最低 21。Maven 消费口径从 11 升到 21（`2f88f5871`）；Gradle 工具链 v1.6.3 起本就要求 21，Gradle 消费者无额外动作。

**Netty 4.1 → 4.2.18**（2.0.4，`888937927`、`645204e85`）：

- 4.2 与 4.1 二进制不兼容（EventLoop 创建模型变更）。工程自带 netty 依赖必须同步升 4.2.x，混搭在 Linux 上会 `AbstractMethodError`。
- 自建 EventLoopGroup 的代码：`EpollEventLoopGroup` / `NioEventLoopGroup`（4.2 已废弃）改 `MultiThreadIoEventLoopGroup(nThreads, factory)`。
- Linux 传输自动按 io_uring > epoll > nio 三级回落（内核 5.1+，推荐 5.10+；Docker seccomp 阻断时自动回落 epoll），无需配置。

**其他依赖**：

| 依赖 | 变化 | 版本 | 说明 |
|---|---|---|---|
| netty-codec-http2 | 新增 compileOnly | 2.0.2 | 启用 h2/h2c 的下游需自行声明 |
| rocketmq-client | 4.9.8 → 5.5.1 | 2.0.3 | 4.x/5.x 混用会 `NoSuchMethodError` |
| rocksdbjni | compileOnly/provided，不传递 | —（一直如此） | 直接使用 `org.rocksdb` 类型的下游需自行声明（10.10.1.1） |
| jspecify | 成为传递依赖 | 2.0.4 | 生成代码携带 `@NullMarked`/`@Nullable`，开启空值检查的工程会出现新告警 |
| fastjson2 | 2.0.62 → 2.0.65 | 2.0.0 | — |
| log4j/postgresql/mongodb-driver/zstd-jni/thymeleaf 等 | 升级 | 2.0.4 | 见 CHANGELOG |

## 3. 代码迁移

### 3.1 API 家族换代（2.0.0，旧 API 已 @Deprecated 未删除）

旧重载仍可编译，建议随升级一次迁移完。四大家族的推荐写法：

**Task → TaskSpec**

```java
// v1.6.3
Task.call(this::doSomething, null);          // Action0 版；FuncLong 版返回 long

// v2.0.x
TaskSpec.ofAction(this::doSomething).call(); // ofFunc/ofProcedure/ofFunc0 同构
TaskSpec.ofAction(this::doSomething).run();  // 终结方法按需选择：call/run/runNow/schedule*
```

工厂：`ofAction / ofFunc / ofProcedure / ofFunc0`，可选参数链式 setter（name、mode(Critical)、timeout 等），终结方法 `call/run/runUnsafe/schedule*/...`。周期调度拆名为 `schedulePeriod(Now)` / `scheduleAtPeriod(Now)`，`period<=0` 显式抛错，周期版返回 `TimerFuture`。

**OneByOne 串行执行 → TaskSpec 终结方法**

```java
// v1.6.3
Application.getInstance().getTaskOneByOneByKey(key).execute(() -> doSomething(), null);

// v2.0.x —— 串行执行收敛为 TaskSpec 的终结方法（Object/int/long key 零装箱）
TaskSpec.ofAction(this::doSomething).executeOneByOne(key);                  // 全局队列
TaskSpec.ofAction(this::doSomething).executeOneByOne(key, Task.getOneByOne()); // 指定队列
```

`Application.getTaskOneByOneByKey` 已废弃，改 `Task.getOneByOne()`。

**Timer → TimerSpec / 门面**

```java
// v1.6.3
timer.schedule(delay, period, MyHandle.class, customData);

// v2.0.x
timer.schedule(TimerSpec.ofDelay(delay).period(period), MyHandle.class, customData);
// 角色在线定时器用门面（timer 为 application.getTimer()）：
timer.roles(roleId).online().schedule(TimerSpec.ofDelay(1000), MyHandle.class, custom);
```

行为收紧：`times(0)` 抛 `IllegalArgumentException`（原为触发一次即停）；`period<=0` 拒绝；`cancel` 返回是否真实取消；`loadTimer` 删除 serverId 参数。

**Online 发送 → OnlineSpec**

```java
// v1.6.3（Game.Online）
Game.Online.defaultInstance.send(roleId, new BSomeProtocol());

// v2.0.x —— online 实例从所属 App/Provider 获取（取代 defaultInstance）
OnlineSpec.ofRole(online, roleId).send(p);          // 自适应事务：事务内等 commit、事务外立即
OnlineSpec.ofRole(online, roleId).sendNow(p);       // 立即语义
OnlineSpec.ofRoles(online, List.of(id1, id2)).send(p); // 批量
```

注意：`sendRpcForWait` 在运行中的事务内改抛 `IllegalStateException`；`trying/trySend` 更名 `quietWhenAbsent`；`sendError` 改名 `onSendError`。

### 3.2 已删除 / 改名 API 一览（编译期发现）

| 删除项 | 版本 | 替代 |
|---|---|---|
| `Game.Online.defaultInstance` / `Arch.Online.instance` | 2.0.0 | `Application.getAppInstance(projectName)` 后从所属 App/Provider 取 Online 实例 |
| `AsyncSocket` 的 Closeable（try-with-resources） | 2.0.0 | 显式 `close(ex, gracefully)` / `closeGracefully()` |
| `Task.scheduleAtDelay` | 2.0.0 | `delayUntilNextDaily` |
| 内建模块 AccountOnline（11041）、DAG | 2.0.0 | 移除 solution 引用 |
| OfflineNotify/OfflineRegister/AnnounceServers/NormalClose | 2.0.0 | Takeover 租约（tTakeoverLease，自动） |
| `Protocol.encodeShared` / `TcpSocket.SendShared` | 2.0.1 | 常规 Send |
| `Application` stop→start 复用 | 2.0.1 | 五态状态机，重启须新建实例 |
| `AutoKey.invalidateRange` | 2.0.1 | `raiseWatermark`（越界 fail-fast） |
| ReliableUdp 组件 | 2.0.2 | TCP 或自建 |
| `Service.Start()/Stop()`（大写）、`Acceptor.Start/Stop` | 2.0.2 | 小写 `start()/stop()` |
| `Rpc.SendReturnVoid` | 2.0.2 | — |
| `IGlobalAgent` 的 Closeable / `AcquireResult` 类形态 | 2.0.2 | `stop()` / record 访问器 |
| `clearInUseAndIAmSureAppStopped` 单 API 双语义 | 2.0.2 | `clearInUse(map)` / `clearInUseAndIAmSureAppStopped(zeze)` |
| binlogger | 2.0.2 | — |
| Meta1/Meta2 以基类构造容器 | 2.0.2 | sealed 家族子类型；用新 Gen.exe 重新生成 |
| Zoker / LogService / Log4jQuery（主库内） | 2.0.3 | `projects/ZokerManager` 独立工程 |
| `ZezeCounter.addTaskRunTime` | 2.0.3 | `getRunTimeObserver(key)` |
| `RankTotal.setTableValue(BRankList)` | 2.0.3 | 传 `BRankListReadOnly` |
| util 原子写 `replace` | 2.0.3 | `writeAtomically(Path, byte[])` |
| `PList2ReadOnly.copyTo`/`toArray` 签名 | 2.0.3 | 返回 `VReadOnly[]` |
| Rpc 回调 `FailCallback` | 2.0.3 | `FailSend`；新增 `sendCallbackBrokenTimeout/Always` |
| Zeze.MQ 模块（11039/11040） | 2.0.4 | RocketMQ 组件（11023）或移除引用 |
| `Config.serverVersion` | 2.0.4 | `appVersion` 高 16 位（`getAppMainVersion()`） |
| `Transaction.getTransactionId()` / tid / TransactionIdAutoKey | 2.0.5 | 自有 id 体系（如全局 AutoKey 表） |
| `IByteBuffer.SkipUnknownField` 签名 | 2.0.5 | 增加显式深度参数 |
| Cube 六件 | 2.0.5 | — |
| `ThreadDiagnosable` | 2.0.6 | `TimeoutDiagnosable`（+`getDispatchMode()`） |
| Task 部分 `tryInitThreadPool` 重载 | 2.0.6 | 保留签名 |

## 4. 配置迁移（zeze.xml）

升级前必查：

- **`CheckpointPeriod` 必须 >0**（2.0.2，`31356d535`）：`<=0` 启动即抛 `IllegalStateException`（此前单核 100% 忙转）。各环境 zeze.xml 逐份检查。
- **`FastRedoWhenConflict` 缺省统一 false**（2.0.2，`2f40e963a`）：此前"有 zeze.xml 但未写该属性"隐式为 true；依赖旧行为的部署需显式写 `FastRedoWhenConflict="true"`。
- **`ServerVersion` 属性删除**（2.0.4）：SM Identify 上报、Takeover 梯队判据改用 `appVersion` 高 16 位，确保正确填写应用主版本。
- **Dbh2：`Dbh2LocalCommit="false"` 时必须配 `CommitServerAddress`**（2.0.3），否则构造期 fail-fast。

新增可选项（按需）：`Config.EnableGlobalFastFail`（2.0.0，默认 false）、`zeze.collections.backend=pcollections|kt|ktt|myktt`（2.0.4 起，默认 2.0.5 起为 myktt；启动时定型）、Onz 协调决策 sync WAL 开关（2.0.6，默认开）。

gen 脚本路径变化（2.0.5，`cda68600e`、`b7fffd1ff`）：根目录 `PublishGen.bat` → `Gen/PublishGen.bat`；`component.confcs.client.xml` → `confcs/`；`gen_use_*.bat` 不再生成 zoker。CI 中的旧路径引用需更新。

## 5. 安全默认值变化（运维必读）

- **RunClass / ReloadClass 端点强制 token 鉴权**（2.0.2，`1d2af7266`）：请求须携带 `X-Zeze-Token` 头或 `token` 查询参数，失配 403。无参构造自动生成随机 token 并打日志——使用这两个端点的运维工具必须改造取 token。
- **DbWeb 六个数据端点强制 token**（2.0.3，`96121919c`）：ListTable/WalkTable/GetValue/PutRecord/DeleteRecord/ClearTable 失配 403 并断连；同样无参构造自动生成随机 token。已有自动化脚本需携带。
- LoginQueue 令牌加密密钥/IV 改 SecureRandom（2.0.1）：升级后旧签发的排队令牌失效（一次性，重新排队即可）。

## 6. 行为与语义变化

升级后行为不同但无需改代码的点，按影响排序：

- **`HasManagedException` → Abort**（2.0.4，`277e6e501`）：向事务重复挂接已受管 bean 从"可捕获继续"改为整体中止事务（perform 返回 -8）。catch 该异常后继续提交的写法失效——改为避免重复挂接或接受整体回滚。
- **RocksDb 主库 commit 改 WAL sync 写**（2.0.0，`95d511254`）：兑现掉电承诺，写吞吐可能下降。
- **Online 登录**（2.0.5，`89ddb7d5c`）：两阶段登录以 LoginVersion 约束，被取代方收到新结果码 `ResultCodeLoginSuperseded`；已有事务上下文中调用登录入口抛错。
- **linkd 断链登出**（2.0.2，`86f45c299`）：推送失败不再立即登出，改 eLinkBroken 标记 + `OnlineLogoutDelay` 宽限钟，期间 `isOnline` 报 false。
- **提交失败也执行已注册回滚动作**（2.0.1，`29c13fc42`）：runWhileRollback/savepoint 注册的回调在提交失败路径同样执行。
- **2 系容器 `copy()` 浅拷贝改深拷贝**（2.0.3，`f386f1550`）；受管容器与 JDK 集合 `equals` 恒为 false（2.0.3，`6d0657b2b`）。
- **Task 线程池默认统一**（2.0.5，`4f74b3e11`）：`useUnlimitedVirtualThread` 默认 true（无界虚拟线程）；需要有限池显式传参。
- **Rpc 重发契约收紧**（2.0.1，`71026dfd6`）：同实例二次 Send 入口即抛。
- **Connector.Ready 等待超时 5s→30s**（2.0.3，`55f8b283a`）；TimeoutManager 时基改 nanoTime 单调钟且 schedule 有一年上界（2.0.6，`821fb7698`）。
- **dbh2 事务暂存所有权**（2.0.6，`681190f1e`）：key/value 传入暂存接口后不得再修改缓冲（框架直接引用，不再拷贝）；自定义 `IApplyTable` 实现同契约。
- **`Rank.getRankTotal` 返回共享只读快照**（2.0.6，`c0ad66d98`）：需要可写副本自行拷贝。
- **`DatabaseMemory.clear()`** 清数据不删表实例（2.0.0，`1570ce649`）。
- **BoolList.clearAll / DbWeb.clearTable 失败抛异常**（2.0.2，`8c6844e24`），调用方按幂等重跑收敛处理。
- **PostgreSQL string key >256 前置拒绝**（2.0.2，`df550898b`）；**Character 键 JSON 输出改码点数值**（2.0.2，`725b71df0`）。

## 7. 数据与线格式兼容性

升级前必须逐项核对：

1. **Redirect RPC 线格式不兼容**（2.0.0，`7e75be5a3`）：`@Redirect` 方法参数/返回值支持 null 导致生成代码大改。**必须用新版 Gen.exe 重新生成全部代码，且调用双方同时升级**，新旧版本互发 Redirect 会解析失败。
2. **SM Identify 协议（BIdentify）wire 不兼容**（2.0.4，`c1237d74c`）：ServiceManager 与 GS/Agent 必须同版本发布，不支持跨版本滚动混跑。集群升级用停机窗口或整体替换。
3. **KV 表 key 长度预算 2712→900**（2.0.0 `e86e24c3c`；2.0.3 `d927ef715` 分档）：写入 >900 拒绝（新表 DDL 收紧为 VARBINARY(900)）；存量 901..2712 的旧 key 自 2.0.3 起可读/删/游标（按存量档 2712），但**不可再写回**。存量超长 key 需迁移（读旧写新）或治理。
4. **内建 Bag（tbag）表键改造**（2.0.1，`abe342ca7`）：`tbag` 的 key 从 String（bagName）改为 `BBagKey`（RoleId + BagName），并修复越权漏洞、新增 ResultCodeNotLogin=7。**存量 tbag 数据需迁移**；直接以 String key 访问的代码改为构造 BBagKey。
5. **GTable 外层 logTypeId 身份化**（2.0.2，`1cbf2fdd1`）：typeId 由 (row,col,val) 完整身份参与；跨版本混布不在支持范围，启用 gtable 的集群整体切换。
6. **Meta sealed 家族**（2.0.2，`0b7aa7329`）：线上 typeId 零漂移，但以基类 Meta 构造容器的自定义代码需改；用新 Gen.exe 重新生成。
7. **LinkedMap SerialNo**（2.0.0，`a27191d64`）：旧数据零迁移兼容；但 clear 后跨事务持有的旧 NodeId 一律视为不存在——依赖旧 NodeId 长期有效性的逻辑需复查。
8. **持久化容器 decode 负长度抛 ISE**（2.0.0，`4227d73a6`）：损坏数据显式报错而非静默清空——升级后若启动报 ISE，说明此前已有静默损坏数据。
9. **集合持久化后端三代演进**（pcollections→ktt→myktt，2.0.4–2.0.5）：磁盘序列化格式不变，无需数据动作；`zeze.collections.backend` 可回退。
10. **Onz BFlushReady 新增 Participant 字段**（2.0.3，`3f004c282`）：additive；协调者与参与方同批升级才能获得按身份去重计数。
11. zeze.xml / solution.xml 格式无破坏性变化；solution 新能力（sortedmap、External.Depends、simplecs）为增量，旧定义兼容。

## 8. 推荐升级步骤

1. **环境**：确认 JDK 21；下游自带 netty 的同步升 4.2.18；用到 h2/rocketmq/rocksdb 的核对依赖声明（§2）。
2. **替换依赖**：zeze-java 升到 2.0.6。
3. **重新生成代码**：按仓库规则用 `Gen/PublishGen.bat` 构建 Gen.exe（如需），再跑根目录 `gen_use_publish.bat` 全量重生成。多个断点（Redirect 2.0.0、Meta sealed 2.0.2、JSpecify 2.0.4）都要求重生成，一次到位。
4. **编译修复**：按 §3.2 表逐项处理编译错误；随后按 §3.1 把 @Deprecated 调用迁到 Spec 家族（可分批）。
5. **配置检查**：按 §4 逐份检查各环境 zeze.xml（重点 CheckpointPeriod、FastRedoWhenConflict、ServerVersion、Dbh2 CommitServerAddress）。
6. **运维工具适配**：RunClass/ReloadClass/DbWeb 的 token（§5）。
7. **数据核对**：§7 第 3、4 项（超长 key、tbag 迁移）；如启用 gtable 按 §7 第 5 项整体切换。
8. **发布方式**：涉及 Redirect 双端与 SM Identify 的，同版本整体发布，不做跨版本滚动混跑（§7 第 1、2 项）。
9. **回归**：跑通业务全量测试；关注 §6 行为变化点（尤其 HasManagedException→Abort、Online 登录结果码、linkd 登出时序）。

## 9. 测试工程（参考 ZezeJavaTest 的项目）

- ZezeJavaTest 已从 JUnit3/4 迁移到 **JUnit 6（Jupiter）**：`@Before→@BeforeEach`、`Assert→Assertions`、JUnit3 命名约定的 `public void testXxx()` 在 Jupiter 下不会自动运行，需补 `@Test`。
- 测试三车道：`gradlew test`（仅 @Fast，类级并行）/ `integrationTest`（排除 fast/bench）/ `bench`（仅 @Bench）。
- 测试 JVM 内由 TestEnvLauncherListener 自动启动 ServiceManager/GlobalCacheManager，不再需要手工 bat；test_all.bat 已删除。
- 测试后端可用 `-PzezeTestDatabase=MySql|PostgreSQL|RocksDb|MongoDb` 重定向（默认 Memory）。
