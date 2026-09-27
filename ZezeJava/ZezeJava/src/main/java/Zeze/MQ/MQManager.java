package Zeze.MQ;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Builtin.MQ.BMessage;
import Zeze.Builtin.MQ.BSendMessage;
import Zeze.Builtin.MQ.Master.BReportPartitions;
import Zeze.Builtin.MQ.Master.BTopicPartitions;
import Zeze.Builtin.MQ.Master.CreatePartition;
import Zeze.Builtin.MQ.Master.DeletePartition;
import Zeze.Config;
import Zeze.MQ.Master.MasterAgent;
import Zeze.Net.AsyncSocket;
import Zeze.Raft.ProxyServer;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Procedure;
import Zeze.Util.AtomicFileWriter;
import Zeze.Util.DaemonTimer;
import Zeze.Util.KV;
import Zeze.Util.RocksDatabase;
import Zeze.Util.ShutdownHook;
import Zeze.Util.TaskSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rocksdb.RocksDBException;
import static Zeze.MQ.Master.AbstractMaster.ePartition;
import static Zeze.MQ.Master.AbstractMaster.eTopicNotExist;

public class MQManager extends AbstractMQManager {
	private static final Logger logger = LogManager.getLogger();

	// 【GB-D06】死信表：key=binary(topic,partitionIndex,messageId)（WriteString+WriteInt4+WriteLong8），
	// value=BMessage 编码 + 8 字节 BE 死信时间戳。
	public static final String DlqTableName = "dlq";

	private final Service masterService;
	private final MasterAgent masterAgent;
	private final ProxyServer proxyServer;
	private final String home;
	// 【GB-D05】Manager 稳定身份：路由表按它关联而非 host:port（换地址重注册→Master 联动重写路由）。
	// 首启自铸并持久化于 home/.managerId，此后跨重启/迁移（同 home）不变。
	private final long managerId;
	private final MQConfig mqConfig = new MQConfig();
	// 周期守护：body(reportLoad阻塞RPC)进worker池，不占调度线程；stop有界等待在飞一轮
	private final DaemonTimer loadMonitorTimer = new DaemonTimer("MQManager.loadMonitor", 120_000, this::loadMonitor);
	private final RocksDatabase rocksDatabase;
	// 数据面静默标志：stop 最前置位，之后到达的提交（SendMessage/push应答回调/回填重排）在入口
	// 被拒绝，保证 rocksDatabase.close 前所有数据通路已静默（其契约：并发 get/put/delete/迭代器
	// 是 native use-after-free）。
	private volatile boolean stopped;
	// 【FND20 GB-C02】管理面排空锁：Create/DeletePartition handler 过入口闸后的长路径全程持本锁。
	// 入口一次性闸只拦"stop 之后到达"的任务，拦不住已过闸正在执行的 handler——delete 的
	// removePartition 有界排空最长 RpcTimeout+5s（被删分区先从活集合摘除，stop 的 queue.close
	// 迭代等不到它），create 的 MQSingle 构造装载秒级（computeIfAbsent 构造完成前不入 map，
	// 同样排空不到），两者恢复后触库（dropTable/getOrAddTable/迭代器）与已完成的
	// rocksDatabase.close 相交即违约。stop 在关库前有界获取同一把锁等在飞管理 handler 出锁；
	// 过闸晚到任务在锁内复查 stopped 拒绝（同构 MQSingle.sendMessage 与 close 的锁内收口形态）。
	// 锁序：handler 路径 managementLock→MQSingle 锁；stop 路径先完成全部 queue.close（MQSingle
	// 锁已随迭代释放）再取 managementLock——两侧无 hold-and-wait 交叠，无 AB-BA。
	private final ReentrantLock managementLock = new ReentrantLock();

	public boolean isStopped() {
		return stopped;
	}

	public long getManagerId() {
		return managerId;
	}

	public RocksDatabase getRocksDatabase() {
		return rocksDatabase;
	}

	// 【GB-D06】死信表句柄（懒建；getOrAddTable 幂等，map 命中即返回，无需缓存）。
	public RocksDatabase.Table getDlqTable() throws RocksDBException {
		return rocksDatabase.getOrAddTable(DlqTableName);
	}

	// 【FND20 GB-D02】死信键编码（写入端 MQSingle.tryDeadLetter 的唯一编码点）：
	// binary(topic,partitionIndex,messageId)=WriteString+WriteInt4+WriteLong8。
	static byte[] dlqKey(String topic, int partitionIndex, long messageId) {
		var bb = ByteBuffer.Allocate();
		bb.WriteString(topic);
		bb.WriteInt4(partitionIndex);
		bb.WriteLong8(messageId);
		return Arrays.copyOfRange(bb.Bytes, bb.ReadIndex, bb.ReadIndex + bb.size());
	}

	// 【FND20 GB-D02】死信分区前缀（不含 messageId）：WriteString 的变长长度前缀使该前缀在键空间
	// 唯一圈定 (topic,partitionIndex) 的全部死信键（不同 topic 的编码从首字节即分叉，无跨分区
	// 误删面）。
	static byte[] dlqPartitionPrefix(String topic, int partitionIndex) {
		var bb = ByteBuffer.Allocate();
		bb.WriteString(topic);
		bb.WriteInt4(partitionIndex);
		return Arrays.copyOfRange(bb.Bytes, bb.ReadIndex, bb.ReadIndex + bb.size());
	}

	// 【FND20 GB-D02】前缀开区间上界：右起首个非0xFF字节+1并在其后截断（截断结果=严格大于全部
	// 前缀键的最小键，deleteRange 不含上界故恰好圈定前缀全体）。前缀含 WriteInt4(partitionIndex)，
	// 非负 int 的高字节恒<0x80，全0xFF不可达；null 防御性返回（调用方跳过清理，与 dropTable
	// 对不存在表的空操作口径一致）。
	private static byte @Nullable [] keyPrefixSuccessor(byte[] prefix) {
		for (int i = prefix.length - 1; i >= 0; --i) {
			if (0 != (byte)(prefix[i] + 1)) {
				var successor = Arrays.copyOf(prefix, i + 1);
				successor[i] = (byte)(prefix[i] + 1);
				return successor;
			}
		}
		return null;
	}

	// 本manager的所有队列实现。
	// { topic -> { partitionIndex -> MQFile } }
	private final ConcurrentHashMap<String, MQPartition> queues = new ConcurrentHashMap<>();

	public MQManager(String home, Config config) throws RocksDBException {
		this.home = home;
		this.rocksDatabase = new RocksDatabase(this.home);
		this.managerId = loadOrMintManagerId(home); // rocksDatabase 构造已确保 home 目录存在
		config.parseCustomize(this.mqConfig);
		// 消费者连接落在proxyServer上（见onSocketClose注释），关闭清理钩子挂这里。
		proxyServer = new ProxyServer(config, mqConfig.getRpcTimeout()) {
			@Override
			public void OnSocketClose(@NotNull AsyncSocket so, Throwable e) throws Exception {
				super.OnSocketClose(so, e);
				MQManager.this.onSocketClose(so);
			}
		};
		masterService = new Service(config, proxyServer);
		masterService.setManager(this);
		masterAgent = new MasterAgent(config, masterService, this::createPartition, this::deletePartition);
		RegisterProtocols(proxyServer);
	}

	public String getHome() {
		return home;
	}

	public MQConfig getMqConfig() {
		return mqConfig;
	}

	public MasterAgent getMasterAgent() {
		return masterAgent;
	}

	public int queueCount() {
		int count = 0;
		for (var queue : queues.values())
			count += queue.size();
		return count;
	}

	// 【GB-D01/GB-D06】包内可见：按 topic 取活队列（测试驱动删除/毒消息路径的入口；MQPartition.get 公有）。
	MQPartition getQueueForTest(String topic) {
		return queues.get(topic);
	}

	public void start() throws Exception {
		ShutdownHook.add(this, this::stop);
		logger.info("start MQManager from '{}' managerId={}", home, managerId);
		loadMQ();
		masterAgent.startAndWaitConnectionReady();
		var acceptorAddress = masterService.getAcceptorAddress();
		masterAgent.register(acceptorAddress.getKey(), acceptorAddress.getValue(), queueCount(), managerId);
		proxyServer.start();

		loadMonitorTimer.start();
	}

	// Master重启丢失managers注册表后由连接建立钩子重发Register恢复；失败仅记日志，等下次重连再试。
	// 首连时与start()里的register各发一次，Master端按managerId（存量host:port兜底）幂等去重，
	// 并按id联动重写mqTable路由（【GB-D05】换地址重注册→路由自愈）。
	void reRegister() {
		try {
			var acceptorAddress = masterService.getAcceptorAddress();
			masterAgent.register(acceptorAddress.getKey(), acceptorAddress.getValue(), queueCount(), managerId);
		} catch (Exception e) {
			logger.error("re-register to master failed, wait for next reconnect", e);
		}
	}

	public void stop() throws Exception {
		stopped = true; // 数据面静默最前置位：晚到的提交在入口拒绝（不发成功应答），对齐 Application 停机拒绝语义
		loadMonitorTimer.stop(); // 有界等待在飞一轮（预算=timeoutMs+5s），不再interrupt池线程
		ShutdownHook.remove(this);
		proxyServer.stop();
		masterAgent.stop();
		// RocksDatabase.close 契约要求先静默全部数据通路（worker 池在飞协议任务、后台回填、push
		// 应答回调）。先关队列：MQSingle.close 有界排空在飞回填、持分区锁关文件流（与在飞
		// appendMessage 串行，此后晚到任务在锁内复查 stopped 拒绝）；rocksDatabase.close 最后。
		for (var queue : queues.values())
			queue.close();
		// 【FND20 GB-C02】管理面排空：过闸在飞的 Create/DeletePartition handler 全程持
		// managementLock（其内部 removePartition 排空最长 RpcTimeout+5s），取同一把锁等它们出锁
		// 后再关库——此后过闸晚到任务只会在锁内复查拒绝，不再触库。超预算仅告警继续（口径同
		// MQSingle.close 的有界排空，不引入无限等待）。取锁成功即释放：与关库之间的窗口内
		// 晚到任务仍有锁内复查兜底（stopped 已置位）。
		var drainBudgetMs = mqConfig.getRpcTimeout() + 5_000L;
		if (!managementLock.tryLock(drainBudgetMs, TimeUnit.MILLISECONDS))
			logger.warn("mq management handler not drained in {}ms, continue close rocksdb", drainBudgetMs);
		else
			managementLock.unlock();
		rocksDatabase.close();
	}

	private void loadMonitor() {
		var loadManager = 0.0;
		for (var queue : queues.values()) {
			loadManager += queue.load();
		}
		// 【FND20 GB-D01】回收前置（依赖方向修正）：不变式——到达段回收步骤之前不执行任何远端
		// 调用。两个上报"失败即抛"（Master 不可达/重启丢注册表的 eManagerNotFound 窗口），旧顺序
		// 把纯本地的回收排在最后，DaemonTimer 对 body 异常"记日志链继续"——Master 降级的整个
		// 期间每轮回收整体跳过（120s一轮），GB-D02 的"磁盘占用收敛于未消费积压+固定余量"承诺在
		// 降级期失守，而降级期恰是积压最大、最需要回收的时候。三块无数据依赖可自由重排：
		// reportLoad 只消费 loadCounter 差分；buildPartitionReport 只扫分区文件（回收只删段不删
		// 分区）；tryRecycle 纯本地且内部自兜异常。上报失败仍向上抛（损失≤一个周期，归因从异常
		// 消息读出），但本轮回收已完成。
		// 【GB-D02】段物理回收复用本timer周期触发（拍板：批量低频，不占ack热路径）；
		// 配置开关与软删除窗口见 MQConfig（SegmentRecycleEnabled/SegmentRecycleDelayMs）。
		if (mqConfig.isSegmentRecycleEnabled()) {
			for (var queue : queues.values())
				queue.tryRecycleSegments(mqConfig.getSegmentRecycleDelayMs());
		}
		masterAgent.reportLoad(loadManager);
		// 【GB-D01】磁盘真相上报（复用本timer周期，120s一轮）：Master 与 mqTable 对账，孤儿超宽限期
		// 下发 DeletePartition。失败语义与 reportLoad 相同（异常由 DaemonTimer 记录，本轮上报损失，
		// 下轮重试——段回收已在前面完成，不受影响）。
		masterAgent.reportPartitions(buildPartitionReport());
	}

	// 【GB-D01】组装分区上报：扫 home 下 topic 目录的分区文件（磁盘真相）。
	// 以磁盘为准而非 queues 活集合：活集合看不到"目录在而构造失败/构造中"的分区，孤儿对账会漏报。
	private BReportPartitions.Data buildPartitionReport() {
		var report = new BReportPartitions.Data();
		for (var e : scanDiskPartitions(home, false).entrySet()) {
			var tp = new BTopicPartitions.Data();
			tp.setTopic(e.getKey());
			tp.getPartitionIndexes().addAll(e.getValue());
			report.getTopics().add(tp);
		}
		return report;
	}

	// 【GB-D01】磁盘真相扫描：{ topic -> 分区索引集合 }，发现规则与 loadMQ 一致（"分区号.消息号"两段名）。
	// warnUnrecognized 仅在启动路径开启（周期上报每轮重复告警刷屏，磁盘残留靠对账收敛）。
	private static HashMap<String, HashSet<Integer>> scanDiskPartitions(String home, boolean warnUnrecognized) {
		var result = new HashMap<String, HashSet<Integer>>();
		var topics = new File(home).listFiles();
		if (null == topics)
			return result;
		for (var topic : topics) {
			if (!topic.isDirectory())
				continue;
			var partitions = topic.listFiles();
			if (null == partitions)
				continue;
			var partitionIndexes = new HashSet<Integer>();
			for (var partition : partitions) {
				if (!partition.isFile())
					continue;
				// 相同分区的文件可能有多个，这里使用HashSet会去重。
				var pa = partition.getName().split("\\.");
				if (pa.length == 2) {
					try {
						partitionIndexes.add(Integer.parseInt(pa[0]));
					} catch (NumberFormatException e) {
						// 忽略目录下混入了非"分区号.消息号"命名的杂散文件。
						if (warnUnrecognized)
							logger.warn("scanDiskPartitions skip unrecognized partition file: {}", partition.getName());
					}
				}
			}
			result.put(topic.getName(), partitionIndexes);
		}
		return result;
	}

	private void loadMQ() {
		for (var e : scanDiskPartitions(home, true).entrySet())
			createPartition(e.getKey(), e.getValue());
	}

	// 【GB-D01】包内可见（测试直构分区入口）：创建 topic 的分区集合。
	void createPartition(String topic, HashSet<Integer> partitionIndexes) {
		var cp = queues.computeIfAbsent(topic, (key) -> new MQPartition(this));
		var topicDir = new File(home, topic);
		//noinspection ResultOfMethodCallIgnored
		topicDir.mkdirs();
		cp.createPartitions(topic, partitionIndexes);
	}

	protected long createPartition(CreatePartition r) {
		// 与ProcessSendMessageRequest同款停机闸（增量审R1-02）：stop的queues快照之后执行时，
		// new MQSingle构造即触rocksdb，与rocksDatabase.close并发（use-after-free契约）。
		if (stopped)
			return Procedure.Closed;
		// 【FND20 GB-C02】入口闸只拦"stop 之后到达"的任务；已过闸正在执行的长路径（MQSingle
		// 构造装载秒级，computeIfAbsent 不入 map 使 stop 的 queue.close 排空不到它）须与 stop
		// 关库互斥：全程持 managementLock（见字段注释），锁内复查收口"等锁期间 stop 已完成"
		// 的竞态（同构 MQSingle.sendMessage 的锁内复查）。
		managementLock.lock();
		try {
			if (stopped)
				return Procedure.Closed;
			createPartition(r.Argument.getTopic(), r.Argument.getPartitionIndexes());
			r.SendResult();
			return 0;
		} finally {
			managementLock.unlock();
		}
	}

	// 【GB-D01】Master 对账裁决孤儿后下发的删除：活分区先 close+从 queues 摘除，段文件/索引列族/meta
	// 全清（无视水位线强制回收——GB-D02 回收三步形态作用于该分区全部段），topic 目录空则一并删除
	//（loadMQ 只扫子目录，空壳目录残留无数据但碍重启扫描）。
	protected long deletePartition(DeletePartition r) throws Exception {
		// 与ProcessSendMessageRequest同款停机闸：删除触 rocksdb dropTable/删文件，不得与 close 并发。
		if (stopped)
			return Procedure.Closed;
		// 【FND20 GB-C02】delete 的长路径最宽：removePartition 有界排空最长 RpcTimeout+5s，期间
		// stop 可已完成 rocksDatabase.close，恢复后的 deletePartitionStorage（dropTable）是对已关
		// 库的 native 调用；且被删分区已从活集合摘除，stop 的 queue.close 迭代等不到它。全程持
		// managementLock 与 stop 关库前的有界获取互斥，锁内复查收口"等锁期间 stop 已完成"的
		// 竞态（同构 MQSingle.sendMessage 的锁内复查）。停止期间中断的清理残留由磁盘真相上报
		// 重新候选、Master 宽限期后重发删除自愈（GB-D01 对账链）。
		managementLock.lock();
		try {
			if (stopped)
				return Procedure.Closed;
			deletePartition(r.Argument.getTopic(), r.Argument.getPartitionIndexes());
			logger.warn("mq partitions deleted by master reconciliation. topic={} partitions={} managerId={}",
					r.Argument.getTopic(), r.Argument.getPartitionIndexes(), managerId); // 动作审计（对账删除不可静默）
			r.SendResult();
			return 0;
		} finally {
			managementLock.unlock();
		}
	}

	// 【GB-D01】包内可见（测试直驱删除路径）：活分区先摘 + 存储全清。
	void deletePartition(String topic, Set<Integer> partitionIndexes) throws Exception {
		var queue = queues.get(topic);
		if (null != queue) {
			for (var index : partitionIndexes)
				queue.removePartition(index);
		}
		for (var index : partitionIndexes)
			deletePartitionStorage(topic, index);
	}

	// 单分区存储清理（锁序对齐 GB-D02 recycleSegment：先摘引用（queues/partitions map 已移除）→
	// dropTable 索引列族 → 删段文件；meta 列族最后 drop）。对不在活集合的分区同样有效（按目录扫描）。
	// dropTable 对不存在的表是空操作（RocksDatabase 契约），杂散文件名（非"num.num"）直接跳过。
	private void deletePartitionStorage(String topic, int index) throws RocksDBException {
		var topicDir = new File(home, topic);
		var files = topicDir.listFiles();
		if (null != files) {
			for (var file : files) {
				var pa = file.getName().split("\\.");
				if (pa.length != 2 || !pa[0].equals(String.valueOf(index)))
					continue;
				try {
					rocksDatabase.dropTable(topic + "." + index + "." + Long.parseLong(pa[1]));
				} catch (NumberFormatException e) {
					// 段基非数字的杂散文件：不触碰列族，仅删文件。
				}
				//noinspection ResultOfMethodCallIgnored
				file.delete();
			}
		}
		rocksDatabase.dropTable(topic + "." + index); // meta
		// 【FND20 GB-D02】死信生命周期与分区绑定：分区存储删除时联动清 dlq 中该 (topic,partition)
		// 前缀的死信键——否则对账删除的分区（含其上已转死信的消息）在 dlq 成为永无人认领的死数据；
		// 且分区重建后位点从 0 重计（meta 列族被 drop 后重建），旧死信键与新代际同 id 消息的键空间
		// 重叠，代际无从分辨。getTable 非懒建（null 即跳过）：getOrAddTable 会在每次分区删除时
		// 凭空造出 dlq 表——清理动作反向制造存储。
		var dlq = rocksDatabase.getTable(DlqTableName);
		if (null != dlq) {
			var begin = dlqPartitionPrefix(topic, index);
			var end = keyPrefixSuccessor(begin);
			if (null != end)
				dlq.deleteRange(begin, end);
		}
		var left = topicDir.listFiles();
		if (null != left && 0 == left.length)
			//noinspection ResultOfMethodCallIgnored
			topicDir.delete();
	}

	// 【GB-D05】铸Manager稳定身份（首启生成，home/.managerId 持久化，此后恒定）：
	// 生成式=时间基线<<16 | SecureRandom低16位——单调时间基线跨进程基本不撞，随机低位防同毫秒
	// 多Manager同铸；恒正（BMQServer.negativeCheck 约束 ManagerId>=0）。写入带 fsync：mint 后
	// 崩溃丢文件会使下次启动铸新身份，旧路由按旧 id 永不再被匹配（按孤儿对账口径还会误删其分区）。
	private static long loadOrMintManagerId(String home) {
		try {
			var file = new File(home, ".managerId");
			if (file.isFile()) {
				try {
					var text = Files.readString(file.toPath(), StandardCharsets.UTF_8).trim();
					if (!text.isEmpty()) {
						var id = Long.parseLong(text);
						if (id > 0)
							return id;
					}
					logger.warn("managerId file corrupted (home={}), re-mint", home); // 损坏内容按未铸处理，覆盖重铸
				} catch (NumberFormatException e) {
					// 非数字损坏同样按未铸处理（最常见损坏形态，不能让构造失败杀启动）：
					// 重铸新身份的代价是旧路由按旧id永不再匹配+孤儿对账回收，属可接受的降级。
					logger.warn("managerId file corrupted (home={}), re-mint", home);
				}
			}
			var id = (System.currentTimeMillis() << 16) | (new SecureRandom().nextInt() & 0xFFFFL);
			// AtomicFileWriter（I1规约）：fsync+原子rename换版，mint中途崩溃不留半文件。
			AtomicFileWriter.replace(file.toPath(), Long.toString(id).getBytes(StandardCharsets.UTF_8));
			return id;
		} catch (Exception e) {
			throw new RuntimeException("load or mint managerId failed. home=" + home, e);
		}
	}

	/**
	 * 【FND20 GB-D02】死信重放（本地运维入口；FND19 GB-D06 拍板"工具手动重放即可"的落地，
	 * 日志宣称的"可重放"自此有仓内真实路径）。重放 = 从死信表按三元组取回消息 → 经
	 * {@link MQSingle#sendMessage} 重新追加到原分区尾部（新 messageId）→ 成功后消费死信键。
	 * 消费者未修复时会再次走满重投上限退回死信（有界，自兜），无自动重放调度（拍板推迟）。
	 * <p>
	 * 死信键值格式：key=binary(topic,partitionIndex,messageId)（WriteString+WriteInt4+WriteLong8），
	 * value=BMessage 编码 + 8 字节 BE 死信时间戳（写入端见 MQSingle.tryDeadLetter）。
	 *
	 * @throws IllegalArgumentException 死信不存在 / 分区不存在（显式报错，不静默丢弃也不误投别处）
	 * @throws IllegalStateException Manager 已停止（sendMessage 的停机拒绝语义透传）
	 * @throws RocksDBException dlq 读写失败
	 */
	public void replayDeadLetter(String topic, int partitionIndex, long messageId) throws RocksDBException {
		// 【FND21 GB-C05】兑现 javadoc 的停机语义：首尾两步（dlq.get/dlq.delete）在 sendMessage 的
		// 锁内复查之外——stop 的 rocksDatabase.close 与其相交是对已关库/已毁句柄的 native 调用
		//（close 契约），"sendMessage 透传"实际覆盖不了全路径。入口收口对齐仓内"全触库入口有闸"
		// 口径（停机维护窗口正是重放毒消息的常见时机，非架空场景）。
		if (stopped)
			throw new IllegalStateException("mq manager stopped, reject replayDeadLetter. topic=" + topic);
		// 【FND22 GB-C02】fix-the-fix（真正闭合 GB-C05 命名窗口）：上面的入口闸与本方法尾部复查
		// 都是无锁裸读，check-then-act 缝隙仍在——"复查 stopped==false 之后、dlq.get/dlq.delete
		// 执行前"两条语句间，stop 可完整走完（proxyServer.stop、N 个分区 close 排空、关库），
		// 其后的触库是对已关库的悬垂句柄 native 调用。对齐同文件同族管理面入口
		//（createPartition/deletePartition：managementLock 全程持有 + 锁内复查）：stopped 在 stop
		// 取 managementLock 之前置位，持锁后复查必见终态；stop 关库前的 tryLock(25s) 有界等待在飞
		// replay 出锁后再关库（超预算 ε 同既有口径，不引入无限等待）。锁序 managementLock→MQSingle
		// 锁（sendMessage 内部自有 MQSingle 锁内 stopped 复查）与 handler 路径一致；stop 先完成全部
		// queue.close（MQSingle 锁随迭代释放）再取 managementLock——两侧无 hold-and-wait 交叠，无 AB-BA。
		managementLock.lock();
		try {
			if (stopped) // 锁内复查：等锁期间 stop 已完成置位（甚至关库）的收口
				throw new IllegalStateException("mq manager stopped, reject replayDeadLetter. topic=" + topic);
			var dlq = rocksDatabase.getTable(DlqTableName); // 非懒建：无死信表即无该死信
			var key = dlqKey(topic, partitionIndex, messageId);
			var value = null != dlq ? dlq.get(key) : null;
			if (null == value)
				throw new IllegalArgumentException("dead letter not found. topic=" + topic
						+ " partition=" + partitionIndex + " messageId=" + messageId);
			var queue = queues.get(topic);
			var single = null != queue ? queue.get(partitionIndex) : null;
			if (null == single)
				throw new IllegalArgumentException("partition not exists. topic=" + topic
						+ " partition=" + partitionIndex + " messageId=" + messageId);
			var message = new BMessage.Data();
			message.decode(ByteBuffer.Wrap(Arrays.copyOfRange(value, 0, value.length - 8))); // 尾缀8字节BE时间戳剥除
			var send = new BSendMessage.Data();
			send.setMessage(message);
			single.sendMessage(send); // 追加到原分区尾（新 messageId；失败上抛，死信键保留可再重放）
			// 【FND21 GB-C05】delete 前复查（sendMessage 成功后 stop 完成关库的窗口）：跳过 delete，
			// 死信键保留可再重放——重放重复（at-least-once，MQ 语义既定）优于对已关库的 native 调用。
			// 【FND22 GB-C02】复查已在 managementLock 内：预算内 stop 尚在 tryLock 等待本方法出锁、
			// 关库必在其后，本闸命中只可能是超预算 ε 逃逸——保留作 belt-and-braces，缩窄残余窗口。
			if (stopped)
				throw new IllegalStateException("mq manager stopped during replay, dead letter key kept for"
						+ " re-replay (message already re-appended). topic=" + topic);
			dlq.delete(key);
		} finally {
			managementLock.unlock();
		}
	}

	@Override
	protected long ProcessSendMessageRequest(Zeze.Builtin.MQ.SendMessage r) {
		// Service.stop 只关 socket 不清 worker 队列：已派发的本任务可能在 stopped 之后才执行，
		// 在入口显式拒绝（不发成功应答），不触碰文件与 rocksdb。
		if (stopped)
			return Procedure.Closed;
		var queue = queues.get(r.Argument.getTopic());
		if (queue == null)
			return errorCode(eTopicNotExist);
		var partition = queue.get(r.Argument.getPartitionIndex());
		if (partition == null)
			return errorCode(ePartition);
		partition.sendMessage(r.Argument);
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessSubscribeRequest(Zeze.Builtin.MQ.Subscribe r) {
		var queue = queues.get(r.Argument.getTopic());
		if (queue == null)
			return errorCode(eTopicNotExist);
		queue.subscribe(r.getSender(), r.Argument.getSessionId());
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessUnsubscribeRequest(Zeze.Builtin.MQ.Unsubscribe r) {
		var queue = queues.get(r.Argument.getTopic());
		if (queue == null)
			return errorCode(eTopicNotExist);
		queue.unsubscribe(r.getSender(), r.Argument.getSessionId());
		r.SendResult();
		return 0;
	}

	// 消费者连接关闭：清理所有topic上该socket的订阅并重排分区。
	// 客户端close()走显式Unsubscribe，这里只兜底非正常死亡（崩溃/断网）——否则死socket永久占槽，
	// 绑到它的分区消息永久积压（arrangeConsumer仅由订阅变更事件触发）。
	// 注意钩子必须挂在proxyServer上：消费者连的是proxyServer端口（getAcceptorAddress优先返回proxy地址，
	// MQAgent经manager.GetReadySocket直连），masterService是连向Master的纯连接器服务，永远见不到消费者socket。
	public void onSocketClose(Zeze.Net.AsyncSocket so) {
		for (var queue : queues.values())
			queue.onSocketClose(so);
	}

	public static class Service extends Zeze.MQ.Master.MasterAgent.Service {
		private final ProxyServer proxyServer;
		private volatile MQManager manager;

		public Service(Config config) {
			super(config);
			proxyServer = null;
		}

		public Service(Config config, ProxyServer proxyServer) {
			super(config);
			this.proxyServer = proxyServer;
		}

		public void setManager(MQManager manager) {
			this.manager = manager;
		}

		@Override
		protected void OnMasterConnected(@NotNull AsyncSocket so) {
			var m = manager;
			if (null == m)
				return;
			// IO线程回调，不得同步等待rpc，提交任务池异步重注册。
			TaskSpec.ofAction(m::reRegister).name("MQManager.reRegister").submitNow();
		}

		public KV<String, Integer> getAcceptorAddress() {
			// 优先查找代理配置，
			return null != proxyServer
					? proxyServer.getOneAcceptorAddress()
					: getOneAcceptorAddress();
		}
	}
}
