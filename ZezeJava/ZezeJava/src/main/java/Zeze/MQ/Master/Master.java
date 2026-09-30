package Zeze.MQ.Master;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Builtin.MQ.Master.BMQServers;
import Zeze.Builtin.MQ.Master.BMQServer;
import Zeze.Builtin.MQ.Master.BMQServerReadOnly;
import Zeze.Builtin.MQ.Master.BReportPartitions;
import Zeze.Builtin.MQ.Master.CreateMQ;
import Zeze.Builtin.MQ.Master.CreatePartition;
import Zeze.Builtin.MQ.Master.DeletePartition;
import Zeze.Builtin.MQ.Master.Register;
import Zeze.Builtin.MQ.Master.ReportLoad;
import Zeze.Builtin.MQ.Master.ReportPartitions;
import Zeze.Builtin.MQ.Master.Subscribe;
import Zeze.Builtin.MQ.BOptions;
import Zeze.Config;
import Zeze.IModule;
import Zeze.MQ.MQConfig;
import Zeze.Net.AsyncSocket;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Procedure;
import Zeze.Util.RocksDatabase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.rocksdb.RocksDBException;

/**
 * MQ Master：维护 mqTable 路由（topic→分区→Manager），管理 Manager 注册与负载，
 * 并以磁盘真相对账裁决孤儿分区。
 */
public class Master extends AbstractMaster {
    private static final Logger logger = LogManager.getLogger();
    public static final String MasterDbName = "__mq_master__";
    // 错误码8：topic名为空串（会作为Manager home根目录本身写入，重启loadMQ只扫子目录，
    // 该topic全部数据成死数据且需人工清目录）。定义在手写子类，不改生成的AbstractMaster。
    public static final int eTopicEmpty = 8;
    // 错误码9：BOptions 传入了未实现的队列类型（DoubleWrite/Raft3 及其他非 Single 值）。
    // 定义在手写子类，不改生成的AbstractMaster（与eTopicEmpty同法）。
    public static final int eOptionsNotImplemented = 9;
    // 错误码10：topic 名与既有 topic 文件系统命名空间折叠冲突（仅大小写/尾随空白不同）。
    // 定义在手写子类，不改生成的AbstractMaster（与eTopicEmpty同法）。
    public static final int eTopicFsAlias = 10;
    // CreateMQ分区数上界：每分区在Manager侧对应一个MQSingle（列族+文件流），Master侧对应
    // 一条servers条目；协议不鉴权（MQPartition自述），无上界时单个请求（int上限）可先在
    // 循环内构造2^31-1条copy打挂Master（OOM/长CPU），侥幸下发后Manager对等膨胀连锁耗尽。
    // 常量上限挡失控/恶意请求即可（超限ePartition）；真实需要更多分区属部署扩容议题，
    // 上调常量重发版。1024对单topic分区已远超常规部署（默认单Manager部署形态）。
    public static final int MaxPartitionsPerTopic = 1024;
    private final String home;
    private final RocksDatabase masterDb;
    private final RocksDatabase.Table mqTable; // key:utf8(topic), value:encode(BMQServers)
    private final Config zezeConfig;
    // 发号基线取当前时间（<<8 留出单次运行内的递增位）：sessionId 不持久化，而 Manager 端订阅跨
    // Master 重启存活，重启后从 0 重发会与旧 id 重叠，重叠订阅会顶掉旧消费者仍活着的条目
    // （旧消费者永久收不到消息）。基线随时间前进，重叠仅在旧进程发号平均
    // 速率超过 256/ms 时才可能（发号点仅 openMQ/createMQ，远达不到）。
    private final AtomicLong sessionIdGen = new AtomicLong(System.currentTimeMillis() << 8);
    // Master 侧配置（仅 OrphanGracePeriodMs 参与；Manager 侧字段无 Master 语义）。
    private final MQConfig mqConfig = new MQConfig();
    // 孤儿候选状态（内存态）：key = {managerKey}|{topic}|{partition} → 候选首见时间。
    // 覆盖判定见 notCoveredPartitions；候选在册化/从上报中消失时除名（CreateMQ 部分成功窗口自愈）。
    final ConcurrentHashMap<String, Long> orphanFirstSeen = new ConcurrentHashMap<>();
    // 整 Manager 面积闸标记（key 同 orphanFirstSeen）：本轮候选覆盖上报者全部上报分区（全量灭失
    // 签名）时不下发删除，宽限期放大一轮（只放大一次）；生命期与 orphanFirstSeen 的各除名点同步。
    final java.util.Set<String> orphanAreaGated = ConcurrentHashMap.newKeySet();
    // 包内可见：孤儿删除下发钩子（默认真实 rpc；测试注入捕获断言"对哪些条目下发了删除"）。
    @FunctionalInterface
    interface DeletePartitionIssuer {
        void issue(BMQServer.Data managerInfo, AsyncSocket socket, String topic, HashSet<Integer> partitionIndexes)
                throws Exception;
    }
    DeletePartitionIssuer deleteIssuer = this::sendDeletePartition;
    // Master 侧停机静默标志（对齐 MQManager.stopped 的同型形态）：
    // Main.stop 最前置位。Service.stop 只置停机屏障、关 socket、停 keepalive，不清 worker 池
    // 已派发的任务：stop 时刻在飞的触库 handler 与 masterDb.close 并发是 native use-after-free
    // （RocksDatabase.close 契约：管理锁只串行化管理操作，不保护 native 句柄生命周期）。置位后
    // 到达/在飞的触库 handler 在入口或模块锁内复查拒绝，不再触碰 mqTable。
    private volatile boolean stopped;

    // 包内可见（测试直驱停机竞态的契约面）。
    boolean isStopped() {
        return stopped;
    }

    // 置位停机静默标记（Main.stop 最前调用，先于 service.stop：晚到的已派发
    // 任务在入口即拒，无需等锁）。
    void markStopped() {
        stopped = true;
    }

    public static class Manager {
        private final AsyncSocket socket;
        private final BMQServer.Data info;
        private double load;

        public Manager(AsyncSocket socket, BMQServer.Data data) {
            this.socket = socket;
            this.info = data;
        }
    }

    private final ArrayList<Manager> managers = new ArrayList<>();

    public Master(String home, Config zezeConfig) throws RocksDBException {
        this.home = home;
        this.zezeConfig = zezeConfig;
        zezeConfig.parseCustomize(mqConfig);
        masterDb = new RocksDatabase(Path.of(home, MasterDbName).toString(),
                RocksDatabase.DbType.eOptimisticTransactionDb);
        mqTable = masterDb.getOrAddTable("mq");
    }

    // 包内可见：配置（测试收缩宽限期）。
    MQConfig getMqConfig() {
        return mqConfig;
    }

    public void close() {
        // 关库前有界排空在飞触库 handler：全部触库 handler 持模块锁执行（入口闸
        // +锁内复查），close 先取同一把锁即等它们出锁再关库——同构 MQSingle.close 持分区锁关
        // 文件流后、晚到任务锁内复查拒绝的收口形态。锁上最长等待者=CreateMQ 的 CreatePartition
        // rpc（5s×N）与对账链的阻塞 DeletePartition rpc（RpcTimeout 量级），预算对齐
        // MQSingle.close 的排空口径（RpcTimeout+5s）；超预算仅告警继续（不引入无限等待，
        // 对齐 Application 停机的有界等待口径）。取锁成功即释放：此后晚到任务过入口闸的
        // 概率窗口内仍有锁内复查兜底（stopped 已置位）。
        var drainBudgetMs = mqConfig.getRpcTimeout() + 5_000L;
        var drained = false;
        try {
            drained = getLock().tryLock(drainBudgetMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (!drained)
            logger.warn("mq master handler not drained in {}ms, continue close masterDb", drainBudgetMs);
        try {
            masterDb.close();
        } finally {
            if (drained)
                getLock().unlock();
        }
    }

    public String getHome() {
        return home;
    }

    public Config getZezeConfig() {
        return zezeConfig;
    }

    public void tryRemoveManager(AsyncSocket manager) {
        lock();
        try {
            for (int i = 0; i < managers.size(); ++i) {
                var e = managers.get(i);
                if (e.socket == manager) {
                    managers.remove(i);
                    // 断连终态：该 manager 的孤儿候选键不再被任何上报收敛（收敛只清在册上报者
                    // 自己的前缀），同步清理——legacy "sock"+hash 键跨 socket 世代永不复用，
                    // 不清则永久驻留。同 id 重连的旧条目已在 Register 替换路径处理。
                    removeOrphanCandidates(e);
                    break;
                }
            }
        } finally {
            unlock();
        }
    }

    private Manager[] choiceManager(int hint) {
        lock();
        try {
            managers.sort(Comparator.comparingDouble(o -> o.load));
            var size = Math.min(hint, managers.size());
            return managers.toArray(new Manager[size]);
        } finally {
            unlock();
        }
    }

    // topic 名的文件系统命名空间折叠键：topic 直接用作 Manager home 子目录名与 rocksdb
    // 列族名，大小写不敏感文件系统上仅大小写或尾随空白/点不同的名字解析到同一物理目录。
    // 折叠口径=尾随空白/点归一（Win32 CreateDirectory 剥离规则）+大小写不敏感（ROOT）；
    // 跨平台一致执行（防创建后迁移到大小写不敏感介质），存量名恒等于自身折叠，不自拒。
    public static String fsNamespaceKey(String topic) {
        return topic.replaceAll("[\\s.]+$", "").toLowerCase(java.util.Locale.ROOT);
    }

    // 新建 topic 的结构性危险名校验（仅 CreateMQ 入口生效，存量放行）：返回 null=合法，
    // 非 null=拒绝原因（日志用）。覆盖折叠检测抓不到的形态：控制字符（目录名非法/终端
    // 注入）与 Windows 保留设备名（CON/PRN/AUX/NUL/COM1-9/LPT1-9，mkdirs 失败仅留列族
    // 孤儿）。其余字符（含非ASCII）合法——白名单收紧会破坏存量命名习惯，不做。
    public static @Nullable String validateNewTopicName(String topic) {
        for (var i = 0; i < topic.length(); ++i) {
            var c = topic.charAt(i);
            if (c < 0x20 || c == 0x7F)
                return "control char U+" + Integer.toHexString(c) + " at " + i;
        }
        var base = fsNamespaceKey(topic); // 保留名匹配用同款折叠（大小写不敏感+尾空白归一）
        if (WindowsReservedNames.contains(base))
            return "windows reserved device name: " + base;
        return null;
    }

    private static final java.util.Set<String> WindowsReservedNames = java.util.Set.of(
            "con", "prn", "aux", "nul",
            "com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8", "com9",
            "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9");

    @Override
    protected long ProcessCreateMQRequest(CreateMQ r) throws Exception {
        // 停机闸（入口快路径）：stopped 置位后到达的请求直接回 Closed，
        // 不触碰 mqTable（masterDb.close 的 native use-after-free 契约）。
        if (stopped)
            return Procedure.Closed;
        // CreateMQ的handler运行在无事务模式（MasterService.setNoProcedure(true)），mqTable的
        // 存在性检查（get）与登记（put）之间无任何串行化：并发同名请求可双双通过检查，
        // 双双下发CreatePartition（Manager磁盘上出现两请求分区号并集的残留），put后写覆盖先写。
        // 模块锁把整个检查-创建-登记流程串行化（管理操作低频，持锁期间CreatePartition rpc
        // 最长5秒超时，仅推迟并发的Register/ReportLoad，无死锁）。
        lock();
        try {
            // 锁内复查：close 持本锁关 masterDb，过闸晚到任务在此拒绝
            //（同构 MQSingle.sendMessage 的锁内复查）。
            if (stopped)
                return Procedure.Closed;
            if (r.Argument.getTopic().isEmpty())
                return errorCode(eTopicEmpty);
            if (r.Argument.getTopic().contains("."))
                return errorCode(eTopicHasReserveChar);
            if (r.Argument.getTopic().contains("/"))
                return errorCode(eTopicHasReserveChar);
            if (r.Argument.getTopic().contains("\\"))
                return errorCode(eTopicHasReserveChar);
            // 结构性危险名校验（仅新建，存量放行）：控制字符与Windows保留设备名——
            // 折叠检测抓不到全新名字（无碰撞对象），这两类仍不可用。
            var nameReject = validateNewTopicName(r.Argument.getTopic());
            if (null != nameReject) {
                logger.error("createMQ topic name rejected: {}. topic={}", nameReject, r.Argument.getTopic());
                return errorCode(eTopicHasReserveChar);
            }
            var topicBytes = r.Argument.getTopic().getBytes(StandardCharsets.UTF_8);
            var mq = mqTable.get(topicBytes);
            if (null != mq)
                return errorCode(eTopicExist);
            // 折叠冲突检测：别名 topic 会与既有 topic 解析到同一物理目录，分区构造把共享
            // 段文件按"幽灵段+未提交尾巴"truncate(0)——既有 topic 的全部盘上积压被一次
            // 创建操作摧毁。对 mqTable 已有条目按折叠键查重，命中即拒。
            var foldKey = fsNamespaceKey(r.Argument.getTopic());
            try (var it = mqTable.iterator()) {
                for (it.seekToFirst(); it.isValid(); it.next()) {
                    if (fsNamespaceKey(new String(it.key(), StandardCharsets.UTF_8)).equals(foldKey)) {
                        logger.error("createMQ topic name rejected: filesystem namespace alias of existing topic."
                                        + " new={} existing={}",
                                r.Argument.getTopic(), new String(it.key(), StandardCharsets.UTF_8));
                        return errorCode(eTopicFsAlias);
                    }
                }
            }

            var servers = r.Result;

            // create
            servers.getInfo().setTopic(r.Argument.getTopic());
            servers.getInfo().setPartition(r.Argument.getPartition());
            // BOptions 校验（fail-fast）：实现只有 MQSingle 一种队列，接受 DoubleWrite/Raft3 会在
            // 协议层回显成功后静默按 Single 跑（单机无副本），可靠性承诺成纸上协议——明确报错拒绝。
            // 0=未指定（编码省略，等价不传），按 Single 默认，兼容既有 null 调用形态。
            var optionsValue = r.Argument.getOptions().getOptions();
            if (optionsValue != BOptions.Single && optionsValue != 0) {
                logger.error("createMQ options={} 未实现：当前仅实现 Single({})，DoubleWrite/Raft3 及其他值拒绝创建，不再静默按 Single 降级",
                        optionsValue, BOptions.Single);
                return errorCode(eOptionsNotImplemented);
            }
            servers.getInfo().setOptions(r.Argument.getOptions());
            if (r.Argument.getPartition() < 1 || r.Argument.getPartition() > MaxPartitionsPerTopic)
                return errorCode(ePartition);

            servers.setSessionId(sessionIdGen.incrementAndGet());

            // 分配manager
            var managers = choiceManager(r.Argument.getPartition());
            if (managers.length == 0)
                return errorCode(eManagerNotFound); // 无manager注册；否则下面 i % managers.length 除零。错误码在生成的AbstractMaster中定义，复用manager缺失语义，不改生成文件

            // 分区号必须按请求局部累积：挂在Manager上跨请求累积不清理，会把旧topic的分区号
            // 下发给新topic，manager上创建出多余分区并随目录持久化残留。
            var managerPartitionIndexes = new HashMap<Manager, HashSet<Integer>>();
            for (var i = 0; i < r.Argument.getPartition(); ++i) {
                var manager = managers[i % managers.length];
                // manager.info 即 Register 上报的 BMQServer（含稳定 ManagerId）：
                // copy 后按本 topic 改写分区号/主题名，ManagerId 随 copy 写入 servers 条目——
                // 路由表按 id 关联，Manager 换地址重注册时 Register 联动重写可命中。
                var info = manager.info.copy();
                info.setPartitionIndex(i);
                info.setTopic(r.Argument.getTopic());
                servers.getServers().add(info);
                managerPartitionIndexes.computeIfAbsent(manager, __ -> new HashSet<>()).add(i);
            }
            // 部分失败残留指明：两阶段创建（先各Manager建分区，全部成功后才登记mqTable）
            // 在中间失败时，已成功下发的分区在Manager磁盘上持久残留（重启loadMQ照常加载），而Master
            // 登记未落——openMQ永远eTopicNotExist，残留分区无主可寻。error必须逐项列出
            // "哪些Manager上已成功创建哪些分区"，供运维立即定位回收（对账收敛的完整形态见孤儿对账）。
            var createdSoFar = new StringBuilder();
            for (var manager : managers) {
                var indexes = managerPartitionIndexes.get(manager);
                if (indexes == null)
                    continue;
                var cp = new CreatePartition();
                cp.Argument.setTopic(r.Argument.getTopic());
                cp.Argument.setPartitionIndexes(indexes);
                cp.SendForWait(manager.socket).await();
                if (cp.getResultCode() != 0) {
                    var failed = manager.info.getHost() + ":" + manager.info.getPort();
                    logger.error("createMQ partial failure: partitions created successfully so far {}"
                                    + " (these persist on managers but the topic is NOT registered in master;"
                                    + " failed manager={} error={} manual cleanup required)",
                            createdSoFar, failed, IModule.getErrorCode(cp.getResultCode()));
                    return errorCode(eCreatePartition);
                }
                createdSoFar.append(manager.info.getHost()).append(':').append(manager.info.getPort())
                        .append("->partitions ").append(indexes).append("; ");
            }
            // save mq info
            var value = ByteBuffer.Allocate();
            servers.encode(value);
            mqTable.put(topicBytes, 0, topicBytes.length, value.Bytes, value.ReadIndex, value.size());

            r.SendResult();
            return 0;
        } finally {
            unlock();
        }
    }

    @Override
    protected long ProcessOpenMQRequest(Zeze.Builtin.MQ.Master.OpenMQ r) throws Exception {
        // 停机闸 + 模块锁内复查：mqTable.get 是 native 触点，须与 close 的
        // 关库互斥（无锁读的入口闸窗口由锁内复查收口）。
        if (stopped)
            return Procedure.Closed;
        lock();
        try {
            if (stopped)
                return Procedure.Closed;
            var topicBytes = r.Argument.getTopic().getBytes(StandardCharsets.UTF_8);
            var mq = mqTable.get(topicBytes);
            if (null == mq)
                return errorCode(eTopicNotExist);
            var servers = r.Result;
            servers.decode(ByteBuffer.Wrap(mq));
            servers.setSessionId(sessionIdGen.incrementAndGet()); // 生成id，必须在decode之后设置。
            r.SendResult();
            return 0;
        } finally {
            unlock();
        }
    }

    @Override
    protected long ProcessRegisterRequest(Register r) throws Exception {
        // 停机闸（入口快路径）。
        if (stopped)
            return Procedure.Closed;
        lock();
        try {
            // 锁内复查：rewriteRoutes 的全表迭代+put 是长触库路径。
            if (stopped)
                return Procedure.Closed;
            // 幂等：重连/重注册会重复到达（首连时start()注册与连接建立钩子各一次；Master重启后
            // 重连重注册），同socket重复append会堆积；且新连接的Register可能先于旧socket的
            // OnSocketClose到达，须替换旧条目，避免死连接滞留managers被choiceManager选中。
            // 匹配优先级：稳定 ManagerId（新路径，地址全换也能关联）> socket > host:port
            // （存量兜底）。host:port 匹配保留：ManagerId==0 视为未知身份（老版本Manager/存量注册表）。
            Manager replaced = null;
            for (int i = 0; i < managers.size(); ++i) {
                var e = managers.get(i);
                var sameManagerId = e.info.getManagerId() != 0 && e.info.getManagerId() == r.Argument.getManagerId();
                if (e.socket == r.getSender() || sameManagerId
                        || (e.info.getHost().equals(r.Argument.getHost()) && e.info.getPort() == r.Argument.getPort())) {
                    replaced = e;
                    managers.remove(i);
                    break;
                }
            }
            var fresh = new Manager(r.getSender(), r.Argument);
            managers.add(fresh);
            // 替换终态且键已变更（重铸 managerId / legacy 换代 / legacy→铸 id 等同址换身份形态）
            // 才清理旧键：旧键不再被任何上报命中，且被替换条目此后不会走 tryRemoveManager
            // （旧 socket 断连时已查不到条目），只能在此收尾。同 id 重连键不变则保留候选，
            // 不重置宽限期（保持收敛语义）。
            if (null != replaced && !orphanManagerKey(replaced).equals(orphanManagerKey(fresh)))
                removeOrphanCandidates(replaced);
            r.SendResult();
            // 联动重写路由：mqTable 中该 manager 承载的 servers 条目改写为新地址（换地址
            // 重注册→路由自愈）。注册本身已成功，重写失败仅记日志等下次重注册重试（Register 每次重连
            // 都会到达，收敛有保障）。
            try {
                rewriteRoutes(r.Argument, null != replaced ? replaced.info : null);
            } catch (Exception e) {
                logger.error("mq route rewrite on register failed, wait next re-register. managerId={} manager={}:{}",
                        r.Argument.getManagerId(), r.Argument.getHost(), r.Argument.getPort(), e);
            }
            return 0;
        } finally {
            unlock();
        }
    }

    /**
     * 包内可见：Register 联动重写 mqTable 路由（测试直驱存量兼容路径）。
     * <p>
     * 对全表每个 topic 的 servers 条目，两路匹配：
     * <ul>
     * <li>ManagerId 匹配（主路径）：注册携带稳定身份，条目按 id 关联——Manager 换地址重注册即可自愈，
     * 与存量地址无关；</li>
     * <li>兼容兜底（存量 mqTable 无 ManagerId，decode 缺省 0=未知身份）：按地址匹配——条目地址等于
     * 注册地址（同址重注册）或被替换旧注册条目的地址（同 socket 换地址）即视为同一 manager，
     * 命中后回填 ManagerId（0 视为未知身份而非"无 manager"，不参与 id 匹配）。</li>
     * </ul>
     * 老数据在首次重注册时被回填，之后走主路径。全表扫描：Register 低频（重连时），规模=topic 数。
     * 并发安全：per-topic 单次原子 put，OpenMQ/Subscribe 无锁读不会看到半态。
     */
    void rewriteRoutes(BMQServer.Data register, @Nullable BMQServer.Data replacedOldInfo) throws RocksDBException {
        var newId = register.getManagerId();
        // 先收集后写回（迭代中不写同表，对齐 MQFileWithIndex.deleteIndexFrom 的快照语义）。
        var pending = new HashMap<byte[], byte[]>();
        try (var it = mqTable.iterator()) {
            it.seekToFirst();
            while (it.isValid()) {
                var topicKey = it.key();
                var topic = new String(topicKey, StandardCharsets.UTF_8);
                var servers = new BMQServers();
                servers.decode(ByteBuffer.Wrap(it.value()));
                var changed = false;
                for (var server : servers.getServers()) {
                    var idMatch = newId != 0 && server.getManagerId() == newId;
                    var legacyMatch = server.getManagerId() == 0
                            && (addressEquals(server, register.getHost(), register.getPort())
                            || null != replacedOldInfo
                            && addressEquals(server, replacedOldInfo.getHost(), replacedOldInfo.getPort()));
                    if (idMatch || legacyMatch) {
                        if (!addressEquals(server, register.getHost(), register.getPort()) || server.getManagerId() != newId) {
                            server.setHost(register.getHost());
                            server.setPort(register.getPort());
                            server.setManagerId(newId); // 兼容路径回填（newId==0 时为空写，保持未知身份）
                            changed = true;
                        }
                    }
                }
                if (changed) {
                    var bb = ByteBuffer.Allocate();
                    servers.encode(bb);
                    pending.put(topicKey, java.util.Arrays.copyOfRange(bb.Bytes, bb.ReadIndex, bb.ReadIndex + bb.size()));
                    logger.info("mq route rewritten on register. topic={} managerId={} -> {}:{}",
                            topic, newId, register.getHost(), register.getPort());
                }
                it.next();
            }
        }
        for (var e : pending.entrySet())
            mqTable.put(e.getKey(), 0, e.getKey().length, e.getValue(), 0, e.getValue().length);
    }

    private static boolean addressEquals(BMQServerReadOnly server, String host, int port) {
        return server.getHost().equals(host) && server.getPort() == port;
    }

    /** 包内可见：读回 mqTable 条目（对账覆盖判定共用；测试断言路由内容）。 */
    @Nullable BMQServers getServers(String topic) throws RocksDBException {
        var mq = mqTable.get(topic.getBytes(StandardCharsets.UTF_8));
        if (null == mq)
            return null;
        var servers = new BMQServers();
        servers.decode(ByteBuffer.Wrap(mq));
        return servers;
    }

    /** 包内可见：mqTable 播种（测试直构对账判定的前置，与 ProcessCreateMQRequest 落表同构）。 */
    void putMqServers(String topic, BMQServers servers) throws RocksDBException {
        var topicBytes = topic.getBytes(StandardCharsets.UTF_8);
        var bb = ByteBuffer.Allocate();
        servers.encode(bb);
        mqTable.put(topicBytes, 0, topicBytes.length, bb.Bytes, bb.ReadIndex, bb.size());
    }

    // 包内可见：managers 注册表播种（测试直构对账判定的存活属主扫描输入；与 Register 落表同构）。
    void putManager(Manager manager) {
        managers.add(manager);
    }

    @Override
    protected long ProcessReportPartitionsRequest(ReportPartitions r) throws Exception {
        // 停机闸（入口快路径）。
        if (stopped)
            return Procedure.Closed;
        lock();
        try {
            // 锁内复查：对账链（notCoveredPartitions 的 mqTable.get + 宽限期后的
            // 阻塞 DeletePartition rpc）是长触库路径。
            if (stopped)
                return Procedure.Closed;
            var manager = findManager(r.getSender());
            if (null == manager)
                return errorCode(eManagerNotFound);
            // 先回应答再对账：对账含阻塞 DeletePartition rpc，不应占用 Manager 的上报应答
            //（对账触发点=收到上报时顺带，无独立定时器）。
            r.SendResult();
            reconcileOrphanReport(manager, r.Argument);
            return 0;
        } finally {
            unlock();
        }
    }

    // 孤儿候选键的 manager 维度（构造单点：对账收敛与终态清理共用）：稳定 ManagerId（非0）
    // 跨重连不变；legacy（未铸 id，Register 带 0）按 socket 身份临时命名——socket 世代更替即
    // 换键，旧键只能靠终态清理收尾。
    private static String orphanManagerKey(Manager manager) {
        return manager.info.getManagerId() != 0
                ? Long.toString(manager.info.getManagerId()) : "sock" + System.identityHashCode(manager.socket);
    }

    // 终态清理（模块锁内调用）：移除该 manager 前缀下的全部孤儿候选键。Manager 条目离开
    // managers（断连摘除/被替换）即终态——legacy "sock"+hash 键随 socket 世代死亡永不复用；
    // 重铸 managerId（home 损坏重造）的旧 id 键同样不会再被任何上报命中（reconcileOrphanReport
    // 的收敛循环只清当前上报者自己的前缀），不清理则永久驻留 orphanFirstSeen（确定性泄漏）。
    private void removeOrphanCandidates(Manager manager) {
        var prefix = orphanManagerKey(manager) + "|";
        for (var it = orphanFirstSeen.keySet().iterator(); it.hasNext(); ) {
            var key = it.next();
            if (key.startsWith(prefix))
                it.remove();
        }
        orphanAreaGated.removeIf(key -> key.startsWith(prefix));
    }

    /**
     * 孤儿对账（包内可见，测试直构判定）：上报条目在 mqTable 无对应 topic、或该 topic 的
     * servers 不含此 manager 承载该分区 → 孤儿候选；候选连续存活超宽限期（OrphanGracePeriodMs，
     * 覆盖 CreateMQ 部分成功/创建中的正常窗口）才下发 DeletePartition，动作记 warn（审计）。
     * <p>
     * 候选除名时机：在册化（CreateMQ 最终登记完成→覆盖命中）或从上报中消失（已删/重建中）；
     * 下发后也除名——删除失败的残留下轮上报重新候选、重新起算宽限期（=宽限期间隔的自动重试）。
     * <p>
     * 孤儿删除是破坏性裁决，依据须是正面遗弃证据而非"无匹配登记"：属主不在场（非零 id
     * 无存活连接，或存量 id==0 条目地址无存活注册=地址级死属主）时上报者的报告即数据
     * 延续证据，判覆盖并证据化转移路由（notCoveredPartitions 第三路 +
     * transferOrphanEvidence）；整 Manager 面积（候选覆盖上报者全部上报分区）的候选满龄
     * 也压一轮再删（面积闸，error 审计）。
     */
    void reconcileOrphanReport(Manager manager, BReportPartitions.Data report) throws Exception {
        var now = System.currentTimeMillis();
        var managerKey = orphanManagerKey(manager);
        var prefix = managerKey + "|";
        // 存活属主扫描（模块锁内，managers 稳定）：id 在 managers 有条目且 socket 未关 = live。
        // tryRemoveManager 异步摘除的窗口内条目仍在但 socket 已关，按死判（连接确已终止）。
        var liveManagerIds = new HashSet<Long>();
        for (var e : managers) {
            var id = e.info.getManagerId();
            if (id != 0 && null != e.socket && !e.socket.isClosed())
                liveManagerIds.add(id);
        }
        // 地址级存活快照（存量 id==0 条目的死属主判据）：有未关闭连接的注册条目即占着
        // 该地址（含 legacy id==0——旧版 Manager 同样在役）。条目地址不在快照中=旧址已
        // 无任何存活注册=地址级死属主（换地址迁移形态）。
        var liveAddresses = new HashSet<String>();
        for (var e : managers) {
            if (null != e.socket && !e.socket.isClosed())
                liveAddresses.add(e.info.getHost() + ":" + e.info.getPort());
        }
        // 本轮候选收集（宽限期从首见起算，putIfAbsent 保持原值）+ 证据化转移候选收集
        var seenKeys = new HashSet<String>();
        var candidates = new HashMap<String, HashSet<Integer>>();
        var transfers = new HashMap<String, HashMap<Integer, Long>>(); // topic -> partition -> 死属主 id
        var reportedTotal = 0;
        for (var tp : report.getTopics()) {
            reportedTotal += tp.getPartitionIndexes().size();
            var notCovered = notCoveredPartitions(tp.getTopic(), tp.getPartitionIndexes(), manager,
                    liveManagerIds, liveAddresses, transfers);
            if (notCovered.isEmpty())
                continue;
            candidates.put(tp.getTopic(), notCovered);
            for (var p : notCovered)
                seenKeys.add(prefix + tp.getTopic() + "|" + p);
        }
        // 证据化转移（锁内批量改写，collect-then-put 对齐 rewriteRoutes 的快照模式）：
        // 先于候选记账执行——转移命中的分区本轮已判覆盖，不产生候选。
        transferOrphanEvidence(transfers, manager);
        for (var key : seenKeys)
            orphanFirstSeen.putIfAbsent(key, now);
        // 收敛：本 manager 的候选中已不在本轮上报/已覆盖的除名（其他 manager 的键不受影响）
        for (var it = orphanFirstSeen.keySet().iterator(); it.hasNext(); ) {
            var key = it.next();
            if (key.startsWith(prefix) && !seenKeys.contains(key))
                it.remove();
        }
        orphanAreaGated.removeIf(key -> key.startsWith(prefix) && !seenKeys.contains(key));
        // 宽限期满 → 下发删除（按 topic 聚合一次 rpc）
        var grace = mqConfig.getOrphanGracePeriodMs();
        // 整 Manager 面积闸（全量灭失签名）：候选覆盖该上报者全部上报分区时升级 error 并把
        // 有效宽限翻倍（只放大一次——标记幂等，已放大的候选满 2×宽限后照常下发），全量孤儿
        // 仍能收敛、多等一个宽限间隔。
        var candidateTotal = 0;
        for (var e : candidates.entrySet())
            candidateTotal += e.getValue().size();
        var wholeManagerArea = reportedTotal > 0 && candidateTotal == reportedTotal;
        if (wholeManagerArea) {
            logger.error("mq orphan candidates cover ALL reported partitions of manager, grace widened one round"
                            + " before any delete: managerId={} manager={}:{} reported={} candidates={} topics={}"
                            + " (whole-manager area signature, manual check advised)",
                    manager.info.getManagerId(), manager.info.getHost(), manager.info.getPort(),
                    reportedTotal, candidateTotal, candidates.keySet());
            for (var e : candidates.entrySet())
                for (var p : e.getValue())
                    orphanAreaGated.add(prefix + e.getKey() + "|" + p);
        }
        var toDelete = new HashMap<String, HashSet<Integer>>();
        var orphanAges = new HashMap<String, Long>(); // 审计用：key=topic，value=最老候选年龄
        for (var e : candidates.entrySet()) {
            for (var p : e.getValue()) {
                var key = prefix + e.getKey() + "|" + p;
                var firstSeen = orphanFirstSeen.get(key);
                var effectiveGrace = orphanAreaGated.contains(key) ? grace * 2 : grace;
                if (null != firstSeen && now - firstSeen >= effectiveGrace) {
                    toDelete.computeIfAbsent(e.getKey(), __ -> new HashSet<>()).add(p);
                    orphanAges.merge(e.getKey(), now - firstSeen, Math::min);
                }
            }
        }
        for (var e : toDelete.entrySet()) {
            for (var p : e.getValue()) {
                var key = prefix + e.getKey() + "|" + p;
                orphanFirstSeen.remove(key);
                orphanAreaGated.remove(key);
            }
            // 动作审计：删除了什么、为什么删（对账裁决不可静默）
            logger.warn("mq orphan partitions delete issued: managerId={} manager={}:{} topic={} partitions={}"
                            + " orphanAgeMs={} (reported by manager but not registered in mqTable, grace {}ms exceeded)",
                    manager.info.getManagerId(), manager.info.getHost(), manager.info.getPort(),
                    e.getKey(), e.getValue(), orphanAges.get(e.getKey()), grace);
            deleteIssuer.issue(manager.info, manager.socket, e.getKey(), e.getValue());
        }
    }

    // 证据化转移（模块锁内调用）：把"属主不在场"（非零 id 无存活连接；存量 id==0 且地址
    // 无存活注册——deadOwnerId==0 即此形态）条目的 id/地址改写为上报者（报告即数据延续
    // 证据，路由跟随数据真相）。只改写收集时认定的死属主条目（同分区的 live 属主/在役
    // 地址条目不动）；上报者为 legacy（id==0）时与 rewriteRoutes 同口径：改写地址、id
    // 落 0（保持未知身份，按地址兜底匹配）。
    private void transferOrphanEvidence(HashMap<String, HashMap<Integer, Long>> transfers, Manager reporter)
            throws RocksDBException {
        for (var e : transfers.entrySet()) {
            var servers = getServers(e.getKey());
            if (null == servers)
                continue;
            var changed = false;
            for (var server : servers.getServers()) {
                var deadOwnerId = e.getValue().get(server.getPartitionIndex());
                if (null == deadOwnerId || server.getManagerId() != deadOwnerId)
                    continue;
                server.setHost(reporter.info.getHost());
                server.setPort(reporter.info.getPort());
                server.setManagerId(reporter.info.getManagerId());
                changed = true;
                logger.warn("mq orphan-evidence transfer: route entry rewritten from dead managerId={} to"
                                + " reporter managerId={} {}:{} topic={} partition={}"
                                + " (reporter's disk report is data-continuity evidence, data kept)",
                        deadOwnerId, reporter.info.getManagerId(), reporter.info.getHost(),
                        reporter.info.getPort(), e.getKey(), server.getPartitionIndex());
            }
            if (changed)
                putMqServers(e.getKey(), servers);
        }
    }

    /**
     * 覆盖判定：reported 分区中被 mqTable 登记给该 manager 的部分之外（= 未覆盖）的子集。
     * 匹配三路，前两路与 Register 联动重写一致：ManagerId 为主，存量条目（ManagerId==0）按注册
     * 地址兜底；第三路为证据化转移——属主不在场时上报者的报告即数据延续证据，判覆盖并收集
     * 转移候选（reconcileOrphanReport 批量改写路由），不判孤儿。属主不在场的两种形态统一
     * 表达：非零 id 无存活连接（managerId 重铸/换代），或存量 id==0 条目的地址无任何存活
     * 注册（换地址迁移——旧址已无在役 Manager）。属主 live 在场（id 命中或地址仍在役）时不走
     * 第三路——另一上报是竞争者而非延续证据，维持 fail-fast 孤儿裁决，防抢路由。
     */
    private HashSet<Integer> notCoveredPartitions(String topic, java.util.Set<Integer> reported, Manager manager,
                                                  HashSet<Long> liveManagerIds, HashSet<String> liveAddresses,
                                                  HashMap<String, HashMap<Integer, Long>> transfers)
            throws RocksDBException {
        var servers = getServers(topic);
        var notCovered = new HashSet<Integer>();
        if (null == servers) {
            notCovered.addAll(reported); // topic 整体不在册：全部条目为孤儿候选
            return notCovered;
        }
        var mid = manager.info.getManagerId();
        for (var p : reported) {
            var covered = false;
            for (var server : servers.getServers()) {
                if (server.getPartitionIndex() != p)
                    continue;
                if (mid != 0 && server.getManagerId() == mid) {
                    covered = true;
                    break;
                }
                // 兼容：存量条目 ManagerId==0，按当前注册地址匹配
                if (server.getManagerId() == 0 && addressEquals(server, manager.info.getHost(), manager.info.getPort())) {
                    covered = true;
                    break;
                }
            }
            if (!covered) {
                // 第三路（证据化转移，属主不在场的统一表达）：条目存在且属主不在场——非零 id
                // 无存活连接（managerId 重铸/换代形态），或存量 id==0 条目地址无任何存活注册
                // （换地址迁移形态，地址级死属主）。上报者的报告即数据延续证据，判覆盖并收集
                // 转移候选；属主在场（id live 或地址在役）不走此路——另一上报是竞争者而非
                // 延续证据，维持 fail-fast 孤儿裁决，防克隆数据目录抢路由。
                for (var server : servers.getServers()) {
                    if (server.getPartitionIndex() != p)
                        continue;
                    var ownerId = server.getManagerId();
                    var ownerGone = ownerId != 0
                            ? ownerId != mid && !liveManagerIds.contains(ownerId)
                            : !liveAddresses.contains(server.getHost() + ":" + server.getPort());
                    if (ownerGone) {
                        transfers.computeIfAbsent(topic, __ -> new HashMap<>()).putIfAbsent(p, ownerId);
                        covered = true;
                        break;
                    }
                }
            }
            if (!covered)
                notCovered.add(p);
        }
        return notCovered;
    }

    // 默认删除下发：失败不抛（记日志）——残留下轮上报重新候选，宽限期后自动重试。
    private void sendDeletePartition(BMQServer.Data managerInfo, AsyncSocket socket, String topic,
                                     HashSet<Integer> partitionIndexes) {
        try {
            var dp = new DeletePartition();
            dp.Argument.setTopic(topic);
            dp.Argument.getPartitionIndexes().addAll(partitionIndexes);
            dp.SendForWait(socket).await();
            if (dp.getResultCode() != 0)
                logger.warn("mq orphan delete rpc failed, will retry after next grace period. manager={}:{}"
                                + " topic={} partitions={} error={}",
                        managerInfo.getHost(), managerInfo.getPort(), topic, partitionIndexes,
                        IModule.getErrorCode(dp.getResultCode()));
        } catch (Exception e) {
            logger.error("mq orphan delete rpc exception, will retry after next grace period. manager={}:{}"
                    + " topic={} partitions={}", managerInfo.getHost(), managerInfo.getPort(), topic,
                    partitionIndexes, e);
        }
    }

    private Manager findManager(AsyncSocket sender) {
        for (var manager : managers)
            if (manager.socket == sender)
                return manager;
        return null;
    }

    @Override
    protected long ProcessReportLoadRequest(ReportLoad r) {
        lock();
        try {
            var manager = findManager(r.getSender());
            if (null == manager)
                return errorCode(eManagerNotFound);
            manager.load = r.Argument.getLoad();
            r.SendResult();
            return 0;
        } finally {
            unlock();
        }
    }

    @Override
    protected long ProcessSubscribeRequest(Subscribe r) throws Exception {
        // 停机闸 + 模块锁内复查（与 ProcessOpenMQRequest 同构：mqTable.get 是
        // native 触点，无锁读的入口闸窗口由锁内复查收口）。
        if (stopped)
            return Procedure.Closed;
        lock();
        try {
            if (stopped)
                return Procedure.Closed;
            var topicBytes = r.Argument.getTopic().getBytes(StandardCharsets.UTF_8);
            var mq = mqTable.get(topicBytes);
            var servers = r.Result;
            if (null == mq)
                return errorCode(eTopicNotExist);
            servers.decode(ByteBuffer.Wrap(mq));
            r.SendResult();
            return 0;
        } finally {
            unlock();
        }
    }
}
