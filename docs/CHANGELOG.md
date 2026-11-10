# 变更日志（Changelog）

覆盖范围：ZezeJava v1.6.3 → v2.0.6，含未发布 2.0.7-SNAPSHOT。条目末尾括号内为提交短哈希。

按约定，热更（HotManager）、History 增量日志、Raft 领域的兼容性适配改动以及已移除组件的收尾改动不在本文件收录范围；这些领域内影响使用者的真实功能/行为变化仅保留简要条目。升级操作请配合 [MIGRATION.md](MIGRATION.md)。

## [未发布] 2.0.7-SNAPSHOT

### 修复

- transaction: 非托管有序映射拒绝批量写入后丢失增量（`e4b010eec`）
- 存储: MongoDB 后端并发 checkpoint flush 遭遇 WriteConflict（瞬时事务冲突）不再 halt 进程——flush 重构为 prepare / 落库两段，落库段可整批重试；新增 `Database.isTransientWriteConflict` 钩子（默认 false，仅 MongoDB 覆写），其余后端行为不变（`39de1cc21`）

### 测试

- 测试后端重定向新增 RocksDb 与 MongoDb 目标（`-PzezeTestDatabase=RocksDb|MongoDb`），多 App 场景新增 `keepOriginal` 豁免（`70ae3826c`、`39de1cc21`）

## [2.0.6] — 2026-11-06

**主线**：TimeoutManager 重写为时间轮并接管 Rpc 超时；dbh2 一致性收口；API 空值契约体系化（getter 非空 / getXxxOrNull 探测）。

### 新功能与优化

- TimeoutManager 全面重写：低精度时间轮 + 分片 TimeoutSet，schedule 吞吐近 2000 万 ops/s；分片落点改 `threadId()&15` 拆除共享缓存行（静机实测 4-16 线程 +50~74%）；新增 `size()` 观测点（`88cf9e0e1`、`934ffc408`、`4d2ec2e59`）
- Rpc 超时列装到时间轮：修复高流量下 Rpc.schedule 积累 OOM（应答即 cancel + 取消即移除策略）；修复登记期间丢失取消的竞态（`e8346a189`、`0e1eb9d72`）
- Application / Task 统一 getter 契约：常规 getter 非空（未初始化即抛），探测场景用新增的 `getXxxOrNull()`（`ec58e0bec`）
- dbh2：身份与路由发布前同步元数据 WAL；拒绝旧目标的迟到分裂/迁移结算；修复压测批次失败后键占用未释放；事务暂存撤防御拷贝（性能）（`29717e0e0`、`830053cdc`、`621ec74ea`、`681190f1e`）
- registry（服务发现）：修复订阅快照导出非法空 upstream、Raft 应答/重放按确认状态发布、注册替换保留旧版本注销通知（`f0a42cd26`、`e250a01f9`、`2d605a49e`）
- json：裸键紧贴注释解析错误、十六进制末尾溢出丢位、整数解析浮点丢负零符号等修复（`33c88b960`、`a7be535ee`、`dd392ab8f`）
- timer：每周表达式同时指定日期与星期、在线取消转发遗漏离线定时器、冷在线调度入口校验调用者等修复（`445ea49d1`、`42e2c772e`、`f2d128ef8`）
- net/netty：multipart 空终结帧、停服后懒启动重建心跳、Zstd 源缓冲分配失败遗留原生流、静态文件服务保留文件系统根路径等修复（`76cf4ba47`、`048561e2c`、`480d5320c`、`7f7ff14b3`）
- gcm/daemon/autokey：降级等待统计真实应答、可靠应答校验对端、水位等待中断后号段恢复（`f0f558418`、`dfaf89960`、`caa506f45`）
- transaction/storage：内存遍历丢失结束回调、RocksDB 读取失败误当迭代结束、版本化保存布尔契约等修复（`c87e4dfb1`、`050824041`、`337daffc2`）

### 破坏性变更

- `ThreadDiagnosable` 被 `TimeoutDiagnosable` 取代；Timeout 派发接口新增 `getDispatchMode()` 参数（`85ad4176d`、`5ebd6de50`）
- Task 删除部分 `tryInitThreadPool` 重载（`6d61c836b`）
- 常规 getter 非空化，探测改用 `getXxxOrNull()`（`ec58e0bec`，承接 2.0.5 的 `fe2bd99e8`）
- TimeoutManager 时基改 `System.nanoTime()` 单调钟；schedule 增加一年上界，超界拒绝（`821fb7698`）
- dbh2 事务暂存与 `IApplyTable` 所有权契约：key/value 传入即放弃所有权，框架直接引用缓冲不再拷贝，传入后不得再修改（`681190f1e`、`2a99442ab`）
- Onz 协调决策与补偿意图默认 sync 写 WAL（防断电丢失恢复记录），可显式关闭（`440f9359f`）
- `Rank.getRankTotal` 命中缓存返回共享只读快照，返回值不得修改（`c0ad66d98`）
- 移除生产代码中的测试钩子与调试入口（dbh2 prepare、SM 致命退出豁免等）（`6b6b62f48`、`76fc62ba9`）

## [2.0.5] — 2026-10-29

**主线**：事务集合持久化默认后端切换为自移植内核 myktt；API 空值契约与陈旧 API 清理；Online 两阶段登录版本约束；数据库迁移健壮性。

### 新功能与优化

- 从 kotlinx.collections.immutable 0.5.2 移植 PersistentList/HashMap/HashSet 入库，新增 myktt 档并定为默认后端（`zeze.collections.backend` 可切 pcollections/kt/ktt/myktt，磁盘序列化格式不变）（`60bc37a76`、`b32857fe4`）
- Online 两阶段登录以 LoginVersion 约束：事务重试不再接管新一代会话；新增 `ResultCodeLoginSuperseded` 结果码；Kick 延迟到补登出提交后发送；拒绝在已有事务上下文中调用登录入口（`89ddb7d5c`）
- 数据库与表结构迁移修复：MySql walkStreamed 中断关闭失败时废弃连接（堵流式结果集带毒归池）；tryAlter 空差分按物理对账自愈补列（根治 meta 领先物理的升级死区）；PostgreSQL 按实际 relation 查询迁移元数据；立即模式迁移写回旧库缺记录（`a0364edc0`、`cc1472056`、`6c37c0fb5`、`2687b4a3f`）
- Application / SM Raft 启动健壮性：动态开表失败撤销登记、构造失败回收已取得资源、Login 提交后再交接会话、依赖表就绪后再开放网络（`4dfc108b0`、`53a3d2ea8`、`2235d846c`、`b37ed9509`）
- Onz Saga：投递步骤前强制持久保存参与方意图、本地提交完成后才应答步骤成功；RedoQueue 持久完成后发布水位并保留失败重试（`1a0464034`、`ef738fd7d`、`5f0f4e97a`）
- Timer：装载异步补触发前固定本轮身份、装载事务失败中止启动并保留游标、离线取消校验 onlineSet（`07e5c2110`、`e8297f4f5`、`257d76e76`）
- net/serialize 审计加固：握手拒绝退化/越界 DH 公开值、Rpc 校验与消费绑定同一上下文、CodecInputStream 区间校验、http2 按流可写性推进文件响应等（`00f0c891a`、`e701276a6`、`925edabf5`、`5cbf15e10`）
- util：统一任务计时单位并使用全局时钟、AtomicOutputFile 关闭隐含提交、PersistentAtomicLong 跨进程重叠号段修复等（`2887d9fc1`、`f13b42ad6`、`7d0ed10a6`）
- 测试数据库体系重构：测试后端统一重定向开关（默认 Memory 可切 MySql/PostgreSQL）（`8616a05ab`、`1514b3936`）
- 构建脚本迁移：根目录 `PublishGen.bat` → `Gen/PublishGen.bat`；`component.confcs.client.xml` → `confcs/`；gen_use 脚本不再生成 zoker（`cda68600e`、`b7fffd1ff`）

### 破坏性变更

- 删除事务编号 API：`Transaction.getTransactionId()`、tid、专用 TransactionIdAutoKey（`cf2f46769`）
- Application 的 getTimer/getSafeBatch/getQueueModule/getDelayRemove/getTakeover 非空化（`fe2bd99e8`）
- 集合持久化默认后端 ktt → myktt（内存实现变化，磁盘格式兼容；显式配置 `zeze.collections.backend` 的部署不受影响）（`60bc37a76`）
- Task 移除 JUnit 探测，`useUnlimitedVirtualThread` 统一默认 true——需要有限线程池必须显式传参（`4f74b3e11`）
- `IByteBuffer.SkipUnknownField` 签名增参（ThreadLocal 隐式深度改显式传递），自定义实现需同步（`ccfb614ae`）
- Online 登录行为与结果码变化（LoginVersion 约束、`ResultCodeLoginSuperseded`、事务内调用登录入口抛错）（`89ddb7d5c`）
- 删除 Cube 六件死代码（MetaGame 迁移原型残留）（`7ed613b49`）

## [2.0.4] — 2026-10-22

**主线**：Netty 4.1 → 4.2（io_uring 默认启用）；事务集合持久化后端可切换；2026-10-20 全树审计修复落地；生成代码引入 JSpecify 空值契约。

### 新功能与优化

- Netty 4.1.137 → 4.2.18：`Netty` 工厂改用 `MultiThreadIoEventLoopGroup` + IoHandlerFactory；Linux 传输三级回落 io_uring > epoll > nio（内核 5.1+ 自动启用，Docker seccomp 阻断时安全回落）；同步升级 log4j/postgresql/mongodb-driver/zstd-jni/thymeleaf 等（`888937927`、`69a2b188a`、`645204e85`）
- 事务集合持久化底层换可切后端，默认由 pcollections 切换为 ktt（kotlinx 内核 + builder 批量）：GameSim 对照驻留 -27%、批量写 p50 -64%、冷装载 -45%；新增配置 `zeze.collections.backend=pcollections|kt|ktt`（`0b8b64f70`、`3eaa6355f`）
- Checkpoint 刷盘重构：Record 只保留缓存状态，独占 FlushBatch 统一编码写入清脏（`f7e4ea50a`）
- 全树审计（~240k LOC）修复落地：JsonReader 九个扫描点补 EOF 守卫；JDBC 三后端 `saveDataWithSameVersion` 补返回码 4 映射（并发首插竞态败者不再中断启动）；Selector 主循环单任务异常隔离；集合解码预分配 OOM 防御（`efbc361eb`、`10e07207a`、`19538809b`、`a09751b2a`）
- Gen 生成包引入 JSpecify 空值契约：生成包 `@NullMarked`、可空处 `@Nullable`；主库同步补空值声明（`a53b2f2b9`、`331708f9f`）
- dbh2：修复闲置代理回收与恢复关闭两处反向锁序死锁；MasterDatabase 不再误毁已发布桶；redo 重发的重复批次由 error 降为 warn（`7aed099e3`、`ad68faa9d`、`5c73c117f`）
- Takeover 新增 version 梯队通知：同 version 优先争抢、失败超时按升序逐档、低于死者 version 永不参与、TakeoverDone 回执即停（`c1237d74c`）
- services/globalcache：SM 客户端退订时序、旧连接归属同名新会话误删、GCM 旧释放跨重启删新权限等修复（`4c410e79c`、`31645d0d1`、`39fbd1409`）
- net：HttpFileService 支持 TLS 文件消息、DecompressMppcZstd 混合输入、DbWeb 五项修复（大整数舍入、首表加载、查询参数编码等）（`a6d1fa764`、`de8f6d8a4`、`19eeaf051`）
- AsyncLock 重构：拆分 AbstractAsyncLock / AsyncLock(Executor) / InlineAsyncLock，解耦全局线程池与 Task 停机补偿（`de70cbaac`）
- 测试基建：test_soak.sh 四车道浸泡驱动；GameSim 补 PList 负载（`b27762d8b`、`35eb0eff3`）

### 破坏性变更

- **Netty 4.1 → 4.2**：二进制不兼容（EventLoop 创建模型变更）；下游自带 netty 依赖须同步升 4.2.x，混搭在 Linux 上会 AbstractMethodError（`888937927`、`645204e85`）
- **`HasManagedException` 语义收敛为 Abort**：向事务挂接已受管 bean 统一经 `Transaction.throwAbort` 中止整体事务（perform 返回 -8），批量入口旧旁路预检删除——catch 后继续提交的写法从此失效（`277e6e501`、`09335ed64`）
- **删除 Zeze.MQ 模块**（`Zeze/MQ/`、Builtin MQ、solution 定义及测试；moduleId 11039/11040 空缺不回收；RocketMQ 模块 11023 不受影响）（`64f6babc8`）
- **删除 `Config.serverVersion`**：SM Identify 上报、Takeover 梯队判据改用 `appVersion >>> 48`（应用主版本），须确保 appVersion 高 16 位正确填写（`7d331f62e`）
- **SM Identify 协议（BIdentify）新增 version 字段，wire 不兼容**：SM 与 GS 必须同版本发布，不支持跨版本滚动混跑（`c1237d74c`）
- 集合持久化默认后端切换 pcollections → ktt（磁盘格式不变；显式配置可回退）（`0b8b64f70`）
- 生成代码新增 JSpecify 注解，jspecify 成为传递依赖；启用空值检查的下游会看到新告警，需用新 Gen.exe 重新生成（`a53b2f2b9`）
- AsyncLock API 拆分，删除全局登记表与停机自动补偿（`de70cbaac`）

## [2.0.3] — 2026-10-15

**主线**：Zoker/LogService 日志发布栈迁出为独立项目；事务引擎性能优化（纯读免编组/非公平锁/提交许可证）；Dbh2 提交围栏正确性攻坚；MQ/RocketMQ 能力补齐。

### 新功能与优化

- Zoker/LogService/Log4jQuery 从主库迁出为独立项目 `projects/ZokerManager`（composite build 消费 zeze-java，ModuleId/TypeId 保持不变）（`f826aa252`、`6d488af90`）
- **纯读事务免编组**：无脏写提交免 rrs 锁/合并/重注册，单进程自动启用——L 场景容量 12.2万 → ≥30万 txn/s（2.2-2.6×），H 脏读 +110%（`7fe2d0dd2`）
- RelativeRecordSet 改非公平锁并加锁等待观测：B 场景 +35%、脏读 +91%；提交许可证 monitor 热路径税根治（`cb77d2c0b`、`327783514`）
- key 长度预算分档执法：写按 900，读/删/游标按存量档 2712 放行——修复调小预算后 901..2712 的旧 key 不可访问（`d927ef715`）
- dbh2：undo/commit 围栏改单调钟（步进免疫），已确认提交的事务不再灭失；新增远程提交模式 `CommitServerAddress` 配置；分桶生命周期闭合（DestroyBucket RPC）；walk 陈旧视图检测（VerifyBucketMeta 等 additive 参数）（`cbe01a2dc`、`103efc4a3`、`718932895`、`e8dce1081`）
- MQ：消息物理回收落地（段文件+索引列族，`MQConfig.SegmentRecycleEnabled` 门控）；毒消息重投上限 + 死信；客户端生命周期闭合；rocketmq-client 4.9.8 → 5.5.1 与主工程统一（`e581831d4`、`c41f511d6`、`93fd6bf9a`、`007c73ad9`）
- RPC 新增 `sendCallbackBrokenTimeout`（连接断/超时才回调）与 `sendCallbackAlways`（false 也回调），Game/Arch.Online 通知路径换用（`1cb2a177c`、`b5118ee8c`）
- DbWeb 六个数据端点强制 token 鉴权（X-Zeze-Token，失配 403；无参构造自动生成随机 token）（`96121919c`）
- net/services：Connector.Ready 等待超时 5s→30s（修复 STW/慢 DNS 假超时）；RPC 应答会合补 typeId/连接校验；HttpExchange 事务/redo 语义修复五连；GCM release 世代守卫；ServiceManager 入口校验与上限（`55f8b283a`、`5bde20dd8`、`0d44ff868`、`3feed1782`、`88ed31631`）
- Timer 修复簇：接管丢链版本致停摆、同名误取消、装载坏行不阻塞其他定时器（`dc267e1c4`、`189cdfc57`、`e28328bfb`）
- Json 引擎大修：keyHash 碰撞防护、无引号非 ASCII 慢路径、三处慢路径合并、拆除 JDK8/JDK9-17 兼容垫片（`3fe55117a`、`81bee757c`、`fa83d0a05`、`b6d601493`）
- AsyncLock 正确性三连（深队列 SOE、拒绝兜底交接破坏互斥等）；RocksDatabase close-safe 资源契约下沉（`de761e76a`、`628a87569`、`b96d659ed`）
- Collections：2 系容器 copy() 从浅拷贝改深拷贝（关闭经 ReadOnly 的可变后门）；PSortedMap.clear 丢比较器修复（`f386f1550`、`ad6b48668`）
- 核心基准新增只读/MMO 画像/网络回环三场景与四时点档案；文档站性能页（`docs/src/content/docs/advanced/performance.md`）基准章节重写（`2a7d85110`、`6eff5e9ab`）

### 破坏性变更

- Zoker/LogService/Log4jQuery 迁出主库：依赖方改为依赖 `projects/ZokerManager` 工程（`f826aa252`）
- raft 版 ServiceManager 的 AllocateId128 RPC 与 tId128 表删除（官方路径为 UDP Id128，仓内无 TCP 发送方）（`0cedd9e07`）
- `ZezeCounter.addTaskRunTime` 移除，统一 `getRunTimeObserver(key)` 句柄；Prometheus 指标改 `task_duration_seconds{task=...}` 标签聚合（`569eb22d8`）
- `Game.Rank.RankTotal.setTableValue` 参数收窄为 `BRankListReadOnly`（`f18251c10`）
- util 原子写 API 收口：`replace` 删除，一次性内容改用 `writeAtomically(Path, byte[])`（`886933def`）
- `PList2ReadOnly.copyTo` 签名收窄、`toArray` 移除（`3d361c103`）
- PMap/PSet/PSortedMap 及 GTable 行视图 `equals` 收窄到私有家族（与 JDK 容器 equals 恒为 false）（`6d0657b2b`、`4cc37761d`）
- Json 删除兼容垫片与 public `Json.javaVersion` 字段（`b6d601493`）
- rocketmq-client 4.9.8 → 5.5.1：4.x/5.x 混用会 NoSuchMethodError（`007c73ad9`）
- Rpc 回调 API：`FailCallback` 改名 `FailSend`（`b2c18b45a`）
- Dbh2 `Dbh2LocalCommit=false` 时必须配置 `CommitServerAddress`，否则构造期 fail-fast（`103efc4a3`）
- Onz BFlushReady 协议新增 Participant 字段（additive；协调者与参与方建议同批升级以获得按身份去重）（`3f004c282`）

## [2.0.2] — 2026-09-24

**主线**：HTTP 体系大重构并新增 h2/h2c；组件删除与 API 签名显式化；FND11/wt6 审核修复批（GTable typeId 根治、JSON 词法等）。

### 新功能与优化

- 新增 **h2c prior-knowledge** 支持：PrefaceDetector 识别 h2 前导码换栈；HttpExchange 体系协议翻译下沉管线层；完整 24 字节魔数比对（`f373fd0d4`、`a2e1bbf7a`、`a1c7acf03`）
- HttpExchange 响应序化器重构为 EventLoop 单写者 + 整数游标（对齐 Armeria）；静态文件服务拆出 HttpFileService；写背压越水位改挂起不断连，暴露 `isWritable()`（`5c2795b30`、`002ef7474`、`129ab1458`）
- Connector 异步建连改造：socket 构造非阻塞、DNS 移专用 resolver 线程；重连意图代数——start/stop 作废已派发任务（`8a22f09b5`、`9a86f3b08`）
- GTable 外层 logTypeId 根治：typeId 由 (row,col,val) 完整身份参与，同 kind 不同列/值类型的表不再共享 typeId（`1cbf2fdd1`）
- Meta1/Meta2 拆 sealed 基类 + 九家族子类，跨家族 meta 编译期不可表示；线上 typeId 零漂移（`0b7aa7329`、`74e5153ed`）
- 新增 DaemonTimer 组件：周期任务重 body 迁出调度池线程，六处站点统一迁移（`5d0ab0163`）
- 安全修复批：ReloadClass/RunClass 端点强制 token 鉴权（X-Zeze-Token，默认自动生成随机 token）；HttpSession 会话固定修复；HttpFileService CRLF 响应头注入；KeyExchange 双向认证修复；LoginQueue 令牌固定 IV（`1d2af7266`、`e5cd44df6`、`5da96375d`、`f5ff36f4e`、`2446395ee`）
- linkBroken/onSendError 重构为幂等标记 + 延迟登出宽限钟，`OnlineLogoutDelay` 真正生效（`86f458299`）
- SqlServer setInUse 补死锁重试；MySQL 关系表 string key 显式二进制排序规则；MySql/PG 租期自愈与 SqlServer 跨进程互斥（`4cf1f78f2`、`879ba0114`、`87e93fd08`）
- Timer：missfire 补触发移出 commit 回调（修复定时器永久停摆）；装载坏数据不阻塞同节点其他定时器（`0a9238811`、`423a99a34`）
- LoginQueue：Client 收 token/满员通知先置停机屏障再回调（修复二次排队）；Server 幽灵条目修复（`2d42206c7`、`fa76b8ba7`）
- 基础设施：AtomicFileWriter 原语统一持久化原子性；Application 锁文件抽到 FileMutex；close 幂等契约（`eaaf76a95`、`ec21dabe6`、`8871e7534`）
- Onz：saga 参与方持久化与 redo 按类型分流、FlushReady 聚集补发（`c5658a349`、`85b9b35b6`）

### 破坏性变更

- **删除 ReliableUdp 组件**（`Zeze.Net.ReliableUdp` 及测试，-1057 行）（`91b4abd73`）
- **`Service.Start()/Stop()`（大写）删除**；`Acceptor.Start/Stop` 改名 `start/stop`（`852306c6b`）
- **`Rpc.SendReturnVoid` 删除**；gen 模式的 createRedirectModules 不再 System.exit（`9cea5d113`）
- **Meta1/Meta2 拆 sealed，容器构造器只收家族子类型**：以基类 Meta 类型构造容器的代码编译失败；需用新版 Gen.exe 重新生成（`0b7aa7329`）
- `IGlobalAgent` 去 Closeable（自声明 `stop()`）；`AcquireResult` 改 record（`e0d61314b`）
- `clearInUseAndIAmSureAppStopped` 拆为 `clearInUse(map)` 与 `clearInUseAndIAmSureAppStopped(zeze)` 双 API（`85993918`）
- **CheckpointPeriod 下界校验 fail-fast**：`<=0` 启动即抛（此前为单核 100% 忙转）——升级前须检查各 zeze.xml（`31356d535`）
- `FastRedoWhenConflict` 缺省统一 false（此前有 zeze.xml 但未写属性时隐式 true）（`2f40e963a`）
- GTable logTypeId 身份化：跨版本混布不在支持范围，启用 gtable 的集群需整体切换（`1cbf2fdd1`）
- Character 键 JSON 输出改码点数值（写读对称；旧版导出本身不可逆）（`725b71df0`）
- netty-codec-http2 成为新增 compileOnly 依赖：启用 h2/h2c 的下游需自行声明（`f373fd0d4`）
- PostgreSQL string key >256 前置拒绝（此前 checkpoint flush 才报 22001 毒化整批）（`df550898b`）
- linkd 断链改延迟登出（宽限期内 isOnline 报 false）——依赖"推送失败即刻登出"的逻辑需知晓（`86f45c299`）
- BoolList.clearAll/DbWeb.clearTable 失败可见：非 0 即抛（`8c6844e24`）
- 删除 binlogger（`e6fd06d13`）

## [2.0.1] — 2026-09-18

**主线**：FND3~FND7 大规模审计修复（事务、网络、服务组件、并发工具）；安全修复批；PerfCounter 监控计量体系重构。

### 新功能与优化

- 事务容器完整性：PMap/PList/PSet/PSortedMap 全家族拒绝 Bean key/Bean 值（原位修改不记账静默丢失、主从分歧）（`42a0d4034`、`046851476`、`724ef94d6`、`0762cc6e8`）
- 事务收尾健壮化：finalCommit/finalRollback 的 logActions 逐项异常隔离；Application.stop 拆卸步骤异常隔离不再跳过 db.close；停机窗口内拒绝提交（`9a3612b3c`、`f3469d909`、`a516fcfdc`）
- sessionId 发号从全局静态改按 App 隔离（修复多 App 同 JVM 撞号误判会话）；HttpExchange 响应写统一走序化器（修复 pipelining 下响应错序）（`1098affcd`、`9cf7ae9a9`）
- 安全链修复：握手加密输入侧门禁、KeyExchange 异常路径断连、LoginQueue 令牌 AES 密钥/IV 改 SecureRandom、Thymeleaf 模板名穿越防护、上传文件名净化（`976f476f3`、`f6241ab19`、`b3e1df169`、`27a708f17`、`1c3d4b041`）
- 守护进程：Daemon 单个坏 UDP 报文 halt 看门狗修复、GlobalOn 无对端校验循环杀子进程修复、SIGTERM 处理（`fc0c29ac2`、`f080b52ce`、`b56057375`）
- 锁与并发容器契约修复：FastRWLock 读重入与写者楔死、Semaphore.tryAcquire 超时补偿误释放、LongHashMap null 条目语义、IntHashMap 扩容回绕、FloatList equals/compareTo 违约（`261bfb8f3`、`72b0c0305`、`f44bf7b7c`、`ec6afa3de`、`37d859ea9`）
- TaskOneByOne 家族三处桶永久卡死修复；Timer 命名查重互通、cancelOffline 归属校验（`66840b5a2`、`b457fe4c3`、`426309676`）
- game/arch：Rank 周榜跨年键碰撞/季榜错位、Online 收敛链自愈、redirect 生成代码丢弃 Send 布尔返回修复（`a44eae09d`、`ccf00bf95`、`6a472ff56`）
- PerfCounter 体系重构：ZezeCounter 空对象化消灭全库判空、Snapshot 快照契约、TableCounter 收敛为 TableMetric 枚举单方法、新增 dispatch 等待/JVM 指标/checkpoint 时长等指标（`c7d8967e5`、`e2d46c103`、`b349754f9`、`dfcc26a02`）
- 性能：生成代码集合 meta 常量化、bean 构造热路径去工厂缓存探测、Id128/tidCache 分配改锁内非阻塞探测（消除 5 秒锁占用）（`b880d1af6`、`57008d559`、`6cd742e18`）
- AutoKey 合服重号窗口根治：失效发布先于提交、水位编码预算与耗尽治理（`2e05acef1`）

### 破坏性变更

- **内建 Bag 表（tbag）键结构改造**：`key="string"`（bagName）改为 `key="BBagKey"`（RoleId + BagName beankey），同时修复 bagName 归属无校验的越权漏洞，新增 ResultCodeNotLogin=7——存量 tbag 数据需迁移（`abe342ca7`）
- **Application 生命周期状态机重设计，实例不可复用**：stop 后再 start 抛 IllegalStateException；重启须新建实例（`89cc52462`）
- 删除 `Protocol.encodeShared` 与 `TcpSocket.SendShared`（`3eff241f2`）
- AutoKey：`invalidateRange` API 删除；`raiseWatermark` 越界 fail-fast（`2e05acef1`）
- 提交失败路径也执行已注册回滚动作（runWhileRollback/savepoint）——依赖"提交失败不跑回滚回调"的代码需复查（`29c13fc42`）
- 监控计量 API 收敛：TableCounter 访问器、PerfCounter 六个无调用方访问器等删除，自定义实现需适配（`b349754f9`、`87a60611d`）
- LinkedMap `getFirstNode/getNode` 返回值改 ReadOnly 并重新公开（`a70107e3b`）
- 全仓 jspecify `@NonNull` 注解替换为 jetbrains `@NotNull`（纯注解，无行为变化）（`cc78fe935`）
- Rpc 同实例重发契约收紧：二次 Send 入口即抛（`71026dfd6`）

## [2.0.0] — 2026-09-10

**主线**：大版本。Task/OneByOne/Timer/Online 四大 API 家族换代为 Spec 风格；离线通知换轨 Takeover 租约；sortedmap 与 SafeBatch 新能力；JUnit 6 测试体系；数百项深度缺陷修复。

### 环境与依赖

- **最低 JDK 21**（Maven 发布口径 source/target 11 → 21；Gradle 工具链 v1.6.3 起已要求 21）（`2f88f5871`）
- netty 4.1.135 → 4.1.137；fastjson2 2.0.62 → 2.0.65；Gradle wrapper 8.14.4 → 9.7.1（`e7f1945f7`、`7ad23eb69`、`9140ade1a`）
- Gen.exe 改 NativeAOT 发布；生成端多线程优化（全量基线 2.73s → ~0.95s，约 2.9x）；新增 luaclient 生成目标（`c4f785023`、`fa2b80aab`）

### 新功能

- **TaskSpec**：sealed interface + ofAction/ofFunc/ofProcedure/ofFunc0 工厂 + 链式 setter，替代 Task 家族 88 个旧重载；周期调度拆名 schedulePeriod(Now)/scheduleAtPeriod(Now)，周期版返回 TimerFuture；新增 `Task.setSystemOneByOneConcurrency(n)`（`87943761c`、`c2d928458`、`07198ef2c`）
- **OneByOne 换代**：串行执行收敛为 TaskSpec 终结方法 `executeOneByOne(key[, queue])`（Object/int/long key 零装箱，开发过程中曾为独立 OneByOneSpec 家族，2.0.0 内合并）；新增全局队列 `Task.getOneByOne()`（`6ee1b0518`、`f46824064`、`e4548fbdb`）
- **TimerSpec / Timer 门面**：`timer.schedule(TimerSpec.ofDelay(delay).period(period), handleClass, customData)` 与 `timer.roles(roleId).online().schedule(...)` 推荐用法；cron 构造时校验；cancel 返回是否真实取消；missfire 修复（OldNext 只补触发一次并对齐未来定点）（`4589496cf`、`f8f340581`、`ae7eca8b6`）
- **OnlineSpec**：Login/Role/Logins/Account/Accounts/AllOnline/ReliableNotify/Transmit 系列，发送自适应事务；新增立即语义动词 `sendNow/transmitNow`（`b758d2547`、`3957970ee`）
- **sortedmap 容器类型**：Gen 支持 `sortedmap[K,V]`，PSortedMap/1/2 实现（pollFirstEntry/pollLastEntry、ReadOnly 包装、Comparable key 约束）（`907951d82`、`1452ccf78`）
- **内置模块 SafeBatch**（ModuleId=11044）：安全分批遍历 table/SortedMap/List，进度持久化、断点续跑（`8b9400df5`）
- **离线通知换轨 Takeover 租约**：新表 tTakeoverLease（claim 抢占 epoch+1、TTL/3 续约、release 宽限），SM 降级为报信员，无 SM 也能接管；正常关闭（缩容）也接管；CsQueue/Timer 写路径 checkFence（`f7a220e33`、`dd5259e91`、`784a1bf0f`）
- LinkedMap.clear 改 O(1)（SerialNo 代际号，旧数据零迁移）；`External.Depends` 依赖自动处理；simplecs 生成模式（只生成 protocol/rpc 及依赖 bean）（`a27191d64`、`ddeaf9dda`、`d7d20b989`）
- `TableX.selectFromDatabase` 直接查库接口；`Database.dropTable`（mysql/pg）（`1f5e41b0e`、`e4d73d3b5`）
- 新增配置 `Config.EnableGlobalFastFail`（默认 false）（`e83e0bc3a`）

### 重要修复

- RocksDb 主库 commit 改 sync 写（兑现 WhenCommit 掉电承诺）（`95d511254`）
- `renameTable` 由直接 drop 旧表（丢数据）改为安全路径（`9b98a408c`）；CheckpointMode.Immediately 此前不落库（重启丢已提交数据）修复（`1ef1d17f0`）
- TableX 装载旧值覆盖未 flush 脏数据、GCM 断连跨进程丢更新、TableCache LRU 死循环（`88a6c5165`、`fbc29442e`、`e9e9d83b3`）
- KV 表 key 长度上限统一收紧 2712→900（8 个 KV 后端全入口检查，超限抛含表名异常）（`e86e24c3c`）
- 持久化容器 decode 负长度从静默清空改抛 ISE（堵无异常丢数据路径）（`4227d73a6`）
- WebSocket 分片无上限 OOM、HttpServer 上传无总量上限、WebsocketClient 重连全场景失效、HaProxy 解析、MppcZstd 压缩流等网络修复系列（`d51388b9d`、`f13164284`、`3058fdfe2`、`513af0d39`）
- GCM 异步 acquire 异常路径申请位不复位致 key 永久冻结、SM-raft 提交前应答回滚收敛、AllocateId 号段重复发放（`040671990`、`c0bc79b87`、`2eee0da1d`）
- LoginQueue 冷启动首次分配延迟消除（token 等待 2.4s → 毫秒级）（`0eb9c4a27`）
- dbh2：分桶期间墓碑复活、walk 多桶分页丢失、分桶失败不回滚致永久卡死、Master AB-BA 死锁等（`c1d00d543`、`070ff8f10`、`a1b0ebe5e`、`d94af5a89`）
- MQ 投递正确性系列（ack 越位致重启后消息永久丢失、消费者崩溃分区永久积压等）；Onz saga end/cancel 从未生效、2PC 决策破坏（`9bdaa45a6`、`1db0853cb`、`0a8988104`、`7bb1577ac`）
- Game：Rank 空榜越界崩溃、rankCached 无界增长、AutoKey 合服重号（`8f093adb7`、`109e9dd8e`、`862397efc`）
- Json：静态入口线程安全、嵌套深度限制、Infinity/NaN 往返、转义 key 丢字段等十余处（`be11ed456`、`be09f68e6`）

### 破坏性变更

- **最低 JDK 21**（Maven 口径）（`2f88f5871`）
- **Redirect RPC 线格式不兼容**：`@Redirect` 方法参数/返回值正确支持 null（生成代码大改）——必须重新运行 Gen 生成，且调用双方同时升级（`7e75be5a3`）
- **KV 表 key 长度预算 2712 → 900**：存量 key>900 的数据升级后写入拒绝（读/删在 2.0.3 起按存量档 2712 放行）；新表 DDL 收紧为 VARBINARY(900)（`e86e24c3c`、`d927ef715`）
- `Game.Online.defaultInstance` / `Arch.Online.instance` 删除，改 `Application.getAppInstance(projectName)`（`972144cf5`、`dfbcc80cf`）
- `AsyncSocket`（及 TcpSocket）不再 implements Closeable：不能 try-with-resources，显式 `close(ex, gracefully)` / `closeGracefully()`（`81ee3dc5b`）
- `Task.scheduleAtDelay` 更名 `delayUntilNextDaily`（`c2d928458`）
- 删除内置模块 **AccountOnline（id=11041）** 与 **DAG 模块**；solution 引用需移除（`225410881`、`0d8cc6f09`）
- 删除 OfflineNotify/OfflineRegister/AnnounceServers/NormalClose 全家 API（含 5 个协议文件），换轨 Takeover 租约；fence 校验失败=进程致命退出（`f7a220e33`）
- Timer：`times(0)` 抛 IllegalArgumentException（原为触发一次即停）；period<=0 拒绝；`loadTimer` 删除 serverId 参数（`4589496cf`、`eb9b22f6a`、`b33e9040a`）
- OnlineSpec：`sendRpcForWait` 在运行中的事务内抛 ISE（原为立即发送，自适应事务下必死锁）；`sendError` 改名 `onSendError`；`trying/trySend` 更名 `quietWhenAbsent`（`f103fc85c`、`71ad6d503`、`b03fc9e8a`）
- Task/OneByOne/Timer/Online 旧 API 共 200+ 处标 @Deprecated（未删除、签名不变，可渐进迁移到 Spec 家族）（`bed97bbb0`、`f46824064`、`ed08d1615`、`3957970ee`）
- `DatabaseMemory.clear()` 改为清数据不删表实例；DatabaseRocksDb 本地缓存标志改构造参数显式传递（`1570ce649`、`26d9164cc`）
- ZezeJavaTest 从 JUnit3/4 迁移到 JUnit 6（Jupiter）；`gradlew test`（@Fast）/ `integrationTest` / `bench` 三车道；测试 JVM 内自动启动 SM/GCM，test_all.bat 删除（`37bc29d21`、`5d0fc6471`）
