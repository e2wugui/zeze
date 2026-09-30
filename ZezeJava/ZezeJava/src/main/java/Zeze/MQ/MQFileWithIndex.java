package Zeze.MQ;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Builtin.MQ.BMessage;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.OutLong;
import Zeze.Util.OutObject;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.RocksDBException;

// 单分区消息存储：段文件顺序追加 + rocksdb 索引（按消息 id 定位）与 meta（位点持久化）。
// 文件路径: {ManagerHome}/{topic}/{partitionId}.{nextMessageId}
// meta表名: {topic}.{partitionId}
// index表名: {topic}.{partitionId}.{nextMessageId}
public class MQFileWithIndex {
	private static final Logger logger = LogManager.getLogger();
	private final ReentrantLock lock = new ReentrantLock();
	// ConcurrentSkipListMap：fillMessage在MQSingle锁外（本类lock外）读floorEntry，
	// appendMessage滚段时在lock内put——TreeMap并发读写是未定义行为（可能CME/读到旋转中间态），
	// 一次竞态异常就会杀死fill后台任务使分区投递永久假死。
	private final ConcurrentSkipListMap<Long, RocksDatabase.Table> indexes = new ConcurrentSkipListMap<>(); // key:nextMessageId, value.key:Long8BE(messageId), value.value:Long8BE(offset)
	private final RocksDatabase.Table meta;
	private final String home;
	private final RocksDatabase database;
	private final String topic;
	private final int partitionId;
	private File lastFile;
	private FileOutputStream lastFileOutputStream;

	private static final byte[] nextMessageIdName = "nextMessageId".getBytes(StandardCharsets.UTF_8);
	private long nextMessageId;

	private static final byte[] firstMessageIdName = "firstMessageId".getBytes(StandardCharsets.UTF_8);
	private long firstMessageId;

	// fillMessage在飞计数（mq-02 终结原语的排空依据）：fill持段索引迭代器与文件句柄期间，
	// dropTable/destroyColumnFamily/close是native use-after-free（RocksDatabase.close同型契约）。
	// 计数经 inFlightFillCheck() 注册给终结原语，段回收与分区删除共用同一排空（不设两套语义）。
	private final AtomicInteger activeFills = new AtomicInteger();

	// 包内可见：在飞 fill 计数（RocksDatabase.destroyColumnFamily 的排空依据）：分区删除链
	// （removePartition→deletePartitionStorage）传入——close 的有界排空超预算逃逸的 fill 世代
	// 持段索引迭代器，drop 前由原语等待归零（mq-02）。
	RocksDatabase.InFlight inFlightFillCheck() {
		return activeFills::get;
	}
	// 软删除窗口状态（lock内）：当前候选最老已确认段的段基 + 首次观察到的时间。
	private long recycleCandidateBase = -1;
	private long recycleCandidateSince;
	// 撕裂写悬挂状态（lock内）：上次 appendMessage 因 write 失败（POSIX 短写语义：出错前记录
	// 前缀已持久化——磁盘满/IO错）或因"文件写成功但未到提交点 meta.put"（索引/meta put 抛
	// RocksDBException 等，段内已残留未提交记录）按 torn 记账，且回滚截断未证实成功时保持置位，
	// 并记录"上次成功结尾"（回滚目标位）。悬挂未解除前不得再追加：追加流是 O_APPEND，写入恒落
	// 物理尾，孤儿字节不物理除掉，后续记录必然接错位（段内布局错位的根源）。
	private boolean tornWritePending;
	private long tornRollbackOffset;
	// 分区关闭标志（lock内）：close 在自身锁内置位（流关闭一并移入锁内，锁序
	// MQSingle→fileWithIndex 既有方向不变，无新交叠），tryRecycle 入口锁内复查即返回。闭合回收
	// 定时器（loadMonitorTimer 120s 周期驱动，原通路无 stopped/closed/managementLock 任何闸）与
	// 分区删除路径（removePartition→close→deletePartitionStorage 的 dropTable 从 tableMap 除名并
	// destroyColumnFamilyHandle 毁 meta/index 列族句柄）的相交：deleteStorage 严格在 close() 返回
	// 之后执行，tryRecycle 要么在置位前完整跑完（close 阻塞于本锁等其退出临界区），要么在置位后
	// 被本标志拒绝——两种时序下 metaConsistent 的无锁 meta.get 都不再与句柄销毁竞速（native
	// use-after-free，RocksDatabase.close/dropTable 契约明示形态）。
	private boolean closed;

	public static int trunkFileSize = 100 * 1024 * 1024;
	public static int makeIndexPeriod = 100;

	public File getLastFile() {
		return lastFile;
	}

	public long getNextMessageId() {
		return nextMessageId;
	}

	public long getFirstMessageId() {
		return firstMessageId;
	}

	public MQFileWithIndex(String home, RocksDatabase database, String topic, int partitionId)
			throws RocksDBException, FileNotFoundException {
		this.home = home;
		this.database = database;
		this.topic = topic;
		this.partitionId = partitionId;

		this.meta = database.getOrAddTable(topic + "." + partitionId);

		var nextMessageIdValue = this.meta.get(nextMessageIdName);
		nextMessageId = null != nextMessageIdValue ? ByteBuffer.ToLongBE(nextMessageIdValue, 0) : 0;
		var firstMessageIdValue = this.meta.get(firstMessageIdName);
		firstMessageId = null != firstMessageIdValue ? ByteBuffer.ToLongBE(firstMessageIdValue, 0) : 0;

		var topicDir = new File(home, topic);
		topicDir.mkdirs();
		if (!topicDir.isDirectory())
			// 防御纵深（CreateMQ 入口已拒非法名字，此处自保存量/迁移形态）：先行失败并带
			// topic 名与路径，替代下方 FileOutputStream 指向不明的 FileNotFoundException。
			throw new FileNotFoundException("mq topic directory unavailable (illegal topic name on this filesystem?)"
					+ " topic=" + topic + " dir=" + topicDir);
		var files = topicDir.listFiles();
		var ghostSegments = new ArrayList<Long>();
		if (null != files) {
			for (var file : files) {
				var partIndex = file.getName().split("\\.");
				// 恰好两段，与 MQManager.loadMQ 的分区发现规则一致：接受更多段会把 "0.500.tmp" 一类
				// 杂散文件注册成幽灵段——lastEntry 被抬高后 lastFile 指向不存在的 "0.500"，
				// recoverTornTail 的 nextMessageId<segBase 检查 fatal 且报错指向"meta 丢失"这一错误方向。
				if (partIndex.length != 2)
					continue;
				try {
					var pid = Integer.parseInt(partIndex[0]);
					if (pid != partitionId)
						continue;
					var index = Long.parseLong(partIndex[1]);
					var tableName = topic + "." + partitionId + "." + index;
					// mq-02 幽灵段登记：getTable 不创建列族（对照 getOrAddTable），tableMap 在库打开时
					// 按 listColumnFamilies 全量装载——返回 null 即该列族在 rocksdb 中不存在而文件在，
					// 也就是「dropTable 成功 + file.delete 失败」的残留；下面 getOrAddTable 会为它重建出
					// 【空】索引列族，装载期须按位点形态恢复（见下方删除残留收尾与 rebuildSegmentIndex）。
					if (null == database.getTable(tableName))
						ghostSegments.add(index);
					indexes.put(index, database.getOrAddTable(tableName));
				} catch (NumberFormatException ex) {
						// 忽略无法解析为"分区号.消息号"的杂散文件名。
				}
			}
		}
		var lastEntry = indexes.lastEntry();
		if (lastEntry == null) {
			lastFile = new File(topicDir, partitionId + ".0");
			indexes.put(0L, database.getOrAddTable(topic + "." + partitionId + ".0"));
		} else if (nextMessageId == 0 && firstMessageId == 0 && lastEntry.getKey() > 0
				&& ghostSegments.size() == indexes.size()) {
			// mq-02 删除残留收尾：位点全零（meta 无 next/first 键=本代际零提交，写路径 next 恒 ≥1）
			// + 高位段文件在 + 全部段列族都是重建的空表。正常生命周期不可达（滚段前必先 meta.put），
			// 唯一系统可达路径=deletePartitionStorage 的「索引/meta 列族 dropTable 全部成功 +
			// file.delete 失败」（meta 能 drop 必然全部段列族已 drop，见其执行顺序）。原死结：
			// recoverTornTail 的 next<segBase 检查 fatal，而磁盘真相上报/对账自愈链全部依赖进程
			// 启动——Manager 反复起不来。收尾=完成被中断的删除并重置为全新分区。
			completeInterruptedDeletion(topicDir, ghostSegments);
			lastFile = new File(topicDir, partitionId + ".0");
			indexes.put(0L, database.getOrAddTable(topic + "." + partitionId + ".0"));
		} else {
			lastFile = new File(topicDir, partitionId + "." + lastEntry.getKey());
		}
		// 追加流打开前先恢复撕裂尾：一旦放任孤儿字节，之后的 appendMessage 会把
		// 新消息接在垃圾后面，错位被固化进文件，fillMessage 的按 id 跳扫从此确定性失败。
		recoverTornTail();
		// mq-02 幽灵段索引重建：必须在 recoverTornTail 之后（末段先截掉未提交尾巴，重建只面对
		// 已提交内容）；删除残留路径的 ghostSegments 已清空，此处只覆盖 meta 位点完好的形态。
		for (var base : ghostSegments)
			rebuildSegmentIndex(base, topicDir);
		lastFileOutputStream = new FileOutputStream(lastFile, true);
	}

	// mq-02（分区删除残留收尾，构造器调用）：完成 deletePartitionStorage 被中断的清理并重置分区。
	// 数据不丢论证：触发条件即位点全零 ⟺ meta 未承诺任何已提交消息；残留文件内容是删除裁决
	// 已放弃的数据（dropTable 成功=删除的提交点，与 recycleSegment 同口径），重建出的空列族
	// 无任何索引项。删文件再失败不阻塞装载：残留文件继续被磁盘真相上报（buildPartitionReport
	// 扫目录）→ Master 对账按孤儿重发 DeletePartition → deletePartitionStorage 重试，自愈链闭合
	// 且不再以「进程起不来」为代价。rocksdb 双故障（丢 meta 键且丢全部索引列族而文件健在）
	// 与本形态签名不可区分，同样收尾：无位点无索引的数据本已系统不可达，恢复服务优先于留档。
	private void completeInterruptedDeletion(File topicDir, ArrayList<Long> ghostSegments) {
		logger.warn("mq partition deletion residue detected, completing interrupted deletion and reset to fresh."
				+ " topic={} partition={} segments={}", topic, partitionId, ghostSegments);
		for (var base : ghostSegments) {
			try {
				database.dropTable(topic + "." + partitionId + "." + base);
			} catch (RocksDBException e) {
				logger.error("mq partition deletion residue: drop recreated index table failed, orphan column family kept."
						+ " topic={} partition={} segment={}", topic, partitionId, base, e);
			}
			var file = new File(topicDir, partitionId + "." + base);
			if (!file.delete())
				logger.warn("mq partition deletion residue: delete file failed, retry via master reconciliation."
						+ " topic={} partition={} file={}", topic, partitionId, file);
		}
		indexes.clear();
		ghostSegments.clear();
	}

	// mq-02（幽灵段索引重建，构造器调用、recoverTornTail 之后）：meta 位点完好而段索引列族丢失
	// 时，firstMessageId 落入该段区间会使 fillMessage 的 seekForPrev 在空表上定位失败——
	// messageIndexNotFound → MQSingle 构造失败 → loadMQ 抛 → Manager 启动死结（水位推进依赖
	// 投递、投递依赖 fill，无自愈）。段文件自描述（记录=12字节头+体，id 自段基连续——滚段
	// 不变式保证首条 id==文件名基），顺序扫描即可重建定位能力；建索引规则与 appendMessage
	// 一致（id%makeIndexPeriod 对齐处落项，段基必对齐→段首可达，floorEntry+seekForPrev 完整恢复）。
	// 记录错位/负长度/越界=真损坏（撕裂写只能产生前缀，产生不了中间错位），保守停在有效前缀：
	// 不静默截断，也不覆盖 fillMessage 对确定性损坏的既有响亮报错——未覆盖区间仍会失败报错。
	private void rebuildSegmentIndex(long base, File topicDir) {
		var indexTable = indexes.get(base);
		var file = new File(topicDir, partitionId + "." + base);
		if (null == indexTable || file.length() == 0)
			return; // 空文件（滚段后未追加即中断，或已恢复截断）：无索引可建
		try {
			try (var input = new RandomAccessFile(file, "r")) {
				var fileSize = input.getChannel().size();
				var messageHead = new byte[12]; // Long8(messageId) + Int4(messageSize)，读写序与fillMessage一致
				var pos = 0L;
				var expectId = base;
				var indexed = 0;
				while (fileSize - pos >= 12) {
					input.seek(pos); // pos含记录体长度（12+size）：readFully只前进头长，须按pos重定位到下条记录头
					input.readFully(messageHead);
					var bbHead = ByteBuffer.Wrap(messageHead);
					var messageId = bbHead.ReadLong8();
					var messageSize = bbHead.ReadInt4();
					if (messageId != expectId || messageSize < 0 || messageSize > fileSize - pos - 12)
						break; // 真损坏形态：有效前缀止步，交由既有响亮语义处置（下方 warn 留痕）
					if (messageId % makeIndexPeriod == 0) {
						var bytesMessageId = new byte[8];
						ByteBuffer.longBeHandler.set(bytesMessageId, 0, messageId);
						var bytesFileOffset = new byte[8];
						ByteBuffer.longBeHandler.set(bytesFileOffset, 0, pos);
						indexTable.put(bytesMessageId, bytesFileOffset);
						++indexed;
					}
					pos += 12L + messageSize;
					++expectId;
				}
				if (pos < fileSize)
					logger.warn("mq segment index rebuild stopped at corrupted record, uncovered tail will fail loudly"
							+ " if filled. topic={} partition={} segment={} position={} fileSize={}",
							topic, partitionId, file.getName(), pos, fileSize);
				logger.warn("mq segment index rebuilt from file. topic={} partition={} segment={} fileBytes={} indexed={}",
						topic, partitionId, file.getName(), fileSize, indexed);
			}
		} catch (Exception e) {
			// 与 recoverTornTail 同口径：装载期恢复失败=分区不可用，宁可响亮不可静默。
			throw Task.forceThrow(e);
		}
	}

	// 撕裂尾恢复（类 WAL recovery，仅构造时执行一次，在打开追加流之前）。
	// appendMessage 先写文件后写 meta：崩溃/掉电/磁盘满会把"半条记录"留在文件尾（掉电丢页缓存时
	// 甚至连已提交记录都会缺尾）；无恢复时下一条消息接在孤儿字节之后，fillMessage 按 12 字节头
	// 跳扫从错位处步步读歪——回填确定性永久失败，分区投递停摆（失败-复位-重试路径
	// 对确定性损坏无能为力，每条新消息触发一次失败）。
	// 策略：从最近已提交索引项（无则段首）顺序校验记录头连续性，按 meta 的 next 截断未提交
	// 尾巴（含撕裂字节与未提交的完好孤儿记录）并回拨位点与索引；只处理"尾部撕裂"——
	// 中间损坏（记录完整存在但 id 错位，其后可能还有完好数据）fatal 抛出，防自动截断静默丢中间消息。
	private void recoverTornTail() {
		try {
			var lastEntry = indexes.lastEntry(); // 构造器保证非 null
			var segBase = lastEntry.getKey();
			if (nextMessageId < segBase)
				// 写序（滚段发生在 meta.put 之后）下不可达；到达即 meta/文件状态损坏（如 rocksdb
				// 丢失而段文件残留），此时段内 id 空间已不可信，不自愈，响亮报错。
				throw new IllegalStateException("mq file inconsistent: nextMessageId(" + nextMessageId
						+ ") < segment base(" + segBase + "), meta lost while segment files kept?"
						+ " topic=" + topic + " partition=" + partitionId + " file=" + lastFile);

			// 锚点=最后段索引表中已提交（id<nextMessageId）的最大索引项：索引项在整条记录写完
			// 之后才落盘，可信指向一条完好记录；没有则退到段首——每段第一条消息必被索引（滚段
			// 条件保证），取不到只可能是段刚滚出还没有提交记录（nextMessageId==segBase，下面循环不进入）。
			long anchorId = segBase;
			var anchorOffset = 0L;
			var seekKey = new byte[8];
			ByteBuffer.longBeHandler.set(seekKey, 0, nextMessageId - 1);
			try (var it = lastEntry.getValue().iterator()) {
				it.seekForPrev(seekKey);
				if (it.isValid()) {
					var id = ByteBuffer.ToLongBE(it.key(), 0);
					if (id < nextMessageId) { // nextMessageId==0 时 seekForPrev(-1) 可命中孤儿条目，须排除
						anchorId = id;
						anchorOffset = ByteBuffer.ToLongBE(it.value(), 0);
					}
				}
			}

			try (var file = new RandomAccessFile(lastFile, "rw")) {
				var fileSize = file.getChannel().size();
				var messageHead = new byte[12]; // Long8(messageId) + Int4(messageSize)，读写序与fillMessage一致
				var pos = anchorOffset;
				var expectId = anchorId;
				while (expectId < nextMessageId) {
					var remaining = fileSize - pos;
					if (remaining < 12)
						break; // 尾巴连头都不完整（含文件短缺/锚点悬垂）：撕裂尾形态
					file.seek(pos);
					file.readFully(messageHead);
					var bbHead = ByteBuffer.Wrap(messageHead);
					var messageId = bbHead.ReadLong8();
					var messageSize = bbHead.ReadInt4();
					if (messageSize < 0 || messageSize > remaining - 12) {
						// size 损坏发生在提交区中间（其后还有已提交记录）时与 id 错位同型：撕裂写只能
						// 产生记录的前缀字节，产生不了"中间记录 size 越界"的形态，自动截断会静默丢掉
						// 其后的已提交消息（与本函数声明的"中间损坏 fatal"策略矛盾），必须响亮报错。
						if (expectId < nextMessageId - 1)
							throw new IllegalStateException("mq file corrupted in middle (bad record size). topic=" + topic
									+ " partition=" + partitionId + " file=" + lastFile + " position=" + pos
									+ " expectMessageId=" + expectId + " actualMessageId=" + messageId
									+ " messageSize=" + messageSize + " fileSize=" + fileSize);
						break; // 最后一条已提交记录的体越过文件尾（或负长度）：写了一半的尾巴，头字段同样不可信
					}
					if (messageId != expectId)
						// 记录完整落在文件内但 id 错位：撕裂写只能产生记录的"前缀"字节，产生不了这种
						// 形态——这是中间损坏或索引错指，其后可能还有完好数据，自动截断等于静默丢中间消息。
						throw new IllegalStateException("mq file corrupted in middle. topic=" + topic
								+ " partition=" + partitionId + " file=" + lastFile + " position=" + pos
								+ " expectMessageId=" + expectId + " actualMessageId=" + messageId
								+ " messageSize=" + messageSize + " fileSize=" + fileSize);
					pos += 12L + messageSize;
					++expectId;
				}
				// 此处 [anchorId, expectId) 完好，pos==最后一条完好记录的结尾（即截断点）。
				if (expectId == nextMessageId && pos == fileSize)
					return; // 干净：提交区完好且无未提交字节，不动文件。

				// 提交区内有缺失（掉电丢页缓存可达）：回拨 next；first 可能已越过回拨点（直入快路径
				// 的消息不等 fill 即被 ack 推进 first），一并夹回，保持 first<=next。持久化先行。
				var committedLost = nextMessageId - expectId;
				if (committedLost > 0) {
					var bbNext = new byte[8];
					ByteBuffer.longBeHandler.set(bbNext, 0, expectId);
					meta.put(nextMessageIdName, bbNext);
					if (firstMessageId > expectId) {
						var bbFirst = new byte[8];
						ByteBuffer.longBeHandler.set(bbFirst, 0, expectId);
						meta.put(firstMessageIdName, bbFirst);
						firstMessageId = expectId;
					}
					nextMessageId = expectId;
				}
				// 索引回拨：撕裂窗口内索引项可能先于 meta 落盘（appendMessage 内索引 put 在
				// meta.put 之前），截断后悬垂指向不存在的偏移，fillMessage 经它定位必失败。
				deleteIndexFrom(lastEntry.getValue(), expectId);
				if (pos < fileSize)
					file.getChannel().truncate(pos);
				logger.warn("mq torn tail recovered. topic={} partition={} file={} truncateBytes={}"
								+ " nextMessageId={}->{} committedLost={} firstMessageId={}",
						topic, partitionId, lastFile.getName(), fileSize - pos,
						nextMessageId + committedLost, nextMessageId, committedLost, firstMessageId);
			}
		} catch (Exception e) {
			// 构造失败=分区不可用：向上传播（Manager 启动失败），宁可响亮不可静默。
			throw Task.forceThrow(e);
		}
	}

	// 删除索引表中 id>=fromId 的全部条目（撕裂尾恢复的索引回拨，仅作用于最后段）。
	private void deleteIndexFrom(RocksDatabase.Table indexTable, long fromId) throws RocksDBException {
		var keysToDelete = new ArrayList<byte[]>(); // 迭代器是快照，先收集再删，语义清晰
		var seekKey = new byte[8];
		ByteBuffer.longBeHandler.set(seekKey, 0, fromId);
		try (var it = indexTable.iterator()) {
			it.seek(seekKey); // 定位到 >=fromId 的第一个条目
			while (it.isValid()) {
				keysToDelete.add(it.key());
				it.next();
			}
		}
		for (var key : keysToDelete)
			indexTable.delete(key);
	}

	// 需要在MQSingle锁内，首先在外部加锁。执行这个函数需要两把锁。
	public long calculateFill(Queue<BMessage.Data> messageQueue, OutLong first, OutLong last, long maxLength) {
		lock.lock();
		try {
			var remain = messageQueue.size();
			var fillCount = Math.min(this.nextMessageId - this.firstMessageId - remain, maxLength - remain);
			first.value = this.firstMessageId + remain;
			last.value = first.value + fillCount;
			return fillCount;
		} finally {
			lock.unlock();
		}
	}

	// 索引定位失败必须响亮报错，不能静默跳过：fillMessage 外层 while 的推进只发生在成功定位之后，
	// 跳过会使 headMessageId 永不前进——回填任务在后台线程里不持锁、无 IO、无 sleep 地单核自旋，
	// 且没有任何日志或异常（触发态：索引 column family 损坏/误删后 getOrAddTable 重建出空表，
	// 或 topic 目录数据文件被误删导致 indexes 仅含高位键）。抛出后由 MQSingle.pullMessage 的
	// catch 复位 messageFillFuture 并重算 highLoad，转入 sendMessage/ack 事件驱动的失败-重试路径。
	private RuntimeException messageIndexNotFound(long headMessageId) {
		return new RuntimeException("message index not found. topic=" + topic
				+ " partition=" + partitionId + " headMessageId=" + headMessageId);
	}

	/**
	 * 装载预算（准入与入账同点）：通过即视为本条入账（调用方实现检查+记账，见 MQSingle）。
	 * queueEmpty=true 为队头活性放行：无条件入账并返回 true（预算小于单条也必须装队头）。
	 * <p>
	 * 调用时序契约：admit 在记录体已成功读取并 decode 之后调用，返回 true 后装载环立即
	 * 入队（两步之间无失败点）——入账不会产生"已入账未入队"的残留，实现方不得在 admit
	 * 内抛出非拒绝型异常。
	 */
	@FunctionalInterface
	public interface FillBudget {
		boolean admit(long messageBytes, boolean queueEmpty);
	}

	// 无预算形态（兼容重载/测试直驱）：恒放行不记账。
	private static final FillBudget AdmitAllFillBudget = (messageBytes, queueEmpty) -> true;

	/**
	 * 从文件中装载消息填充到队列中（无预算，恒放行）。
	 * 注意：参数未经验证，需要外部确保正确（请使用calculateFill得到参数）。
	 * @param messageQueue 队列
	 * @param headMessageId 开始Id。
	 * @param endMessageId 结束Id。
	 * @return 实际装载终点（== endMessageId 即完整装载）。
	 */
	public long fillMessage(Queue<BMessage.Data> messageQueue, long headMessageId, long endMessageId) {
		return fillMessage(messageQueue, headMessageId, endMessageId, AdmitAllFillBudget);
	}

	/**
	 * 从文件中装载消息填充到队列中（字节预算准入）。
	 * 每条消息读体解码后查预算：队列空恒放行（队头活性——预算小于单条也必须装队头，否则
	 * 分区死锁）；不达标即截断返回（调用方据此重算 highLoad）。
	 * @return 实际装载终点（== endMessageId 即完整装载；&lt; endMessageId 即预算截断点）。
	 */
	public long fillMessage(Queue<BMessage.Data> messageQueue, long headMessageId, long endMessageId,
							FillBudget budget) {
		// 在飞计数先于首个floorEntry与租约检查（mq-02）：终结原语（destroyColumnFamily，段回收/
		// 分区删除都走它）以"先置标记后读计数"与本处"先计数后查标记"互为双检——计数覆盖整个
		// 调用（含迭代器 try-with-resources 的 close），原语排空读到 0 即无任何迭代器存活。
		activeFills.incrementAndGet();
		// 锁内计算需要读取的消息数量，并且推进firstMessageId。
		try {
			while (headMessageId < endMessageId) {
				var floor = indexes.floorEntry(headMessageId);
				if (null != floor) {
					// mq-02 迭代器租约：每轮批量迭代前检查——终结原语已置毁标记即放弃本轮，
					// 不得再获取该列族迭代器（其后原语的 drop 与迭代器并发是 native
					// use-after-free）。抛出走 pullMessage 既有的失败-复位-重试路径
					//（不静默返回：partial 装载会留下 highLoad 已扣而积压未装的停摆窗口）。
					if (floor.getValue().isDestroyPending())
						throw new IllegalStateException("segment index destroy pending, abort fill. topic=" + topic
								+ " partition=" + partitionId + " segment=" + floor.getKey()
								+ " headMessageId=" + headMessageId);
					var headMessageIdValue = new byte[8];
					ByteBuffer.longBeHandler.set(headMessageIdValue, 0, headMessageId);
					try (var floorIt = floor.getValue().iterator()) {
						floorIt.seekForPrev(headMessageIdValue);
						if (floorIt.isValid()) {
							var topicDir = new File(home, topic);
							var file = new File(topicDir, partitionId + "." + floor.getKey());
							try (var fileInput = new RandomAccessFile(file, "r")) {
								var fileSize = fileInput.getChannel().size();
								// 必须是 long：段文件可越过 2GB（ProxyServer 放行 100MB 协议，滚段还需
								// 等下一个 100 整除 id），int 累加回绕为负后与 fileSize 的 eof 边界
								// 判断恒不成立，头/体读全错位。
								var filePosition = 0L;
								var offset = ByteBuffer.ToLongBE(floorIt.value(), 0);
								fileInput.seek(offset);
								filePosition += offset;
								long messageId;
								int messageSize;
								var messageHead = new byte[12];
								while (true) {
									filePosition += messageHead.length;
									if (filePosition > fileSize)
										throw new RuntimeException("locate message eof.");
									fileInput.readFully(messageHead);
									var bbHead = ByteBuffer.Wrap(messageHead);
									messageId = bbHead.ReadLong8();
									messageSize = bbHead.ReadInt4();
									if (messageId == headMessageId)
										break; // message found.
									// 负长度防御：skipBytes(负数)按规范返回 0，下面的越界检查被绕过
									// （0<负数恒假），filePosition 镜像每轮净减使 eof 检查永不成立——
									// 无 IO、无异常、无日志的单核自旋（与 messageIndexNotFound 同型失效）。
									if (messageSize < 0)
										throw new RuntimeException("locate message size corrupted (negative). topic=" + topic
												+ " partition=" + partitionId + " headMessageId=" + headMessageId
												+ " messageId=" + messageId + " messageSize=" + messageSize);
									if (fileInput.skipBytes(messageSize) < messageSize)
										throw new RuntimeException("message not found"); // 忽略的长度不够，表示数据文件被截断了。
									filePosition += messageSize;
								}

								// 连续装载校验：headMessageId 即期望 id——首条由上面的定位循环以
								// messageId == headMessageId 判定命中，此后每装载一条 ++（见循环尾），
								// 后续每个记录头的 id 必须精确等于期望值；不符（同 id 双孤儿记录等
								// 布局错位形态）在此响亮抛出，进入 pullMessage 既有的失败-复位-重试
								// 路径，而不是把错位字节当消息静默装载投递。
								while (true) {
								// 先读体解码、后准入入账：admit=检查+入账同点，入账与入队
								// 之间不留失败点——体长越界/readFully IO错/decode失败都
								// 发生在入账之前，异常路径零残留记账。
								var messageBuffer = new byte[messageSize];
									var bodyEnd = filePosition + messageBuffer.length;
									if (bodyEnd > fileSize)
										throw new RuntimeException("read message body eof.");
									fileInput.readFully(messageBuffer);
									var message = new BMessage.Data();
									message.decode(ByteBuffer.Wrap(messageBuffer));
									if (!budget.admit(messageSize, messageQueue.isEmpty()))
										return headMessageId;
									messageQueue.add(message);
									filePosition = bodyEnd;

									headMessageId++;
									if (filePosition >= fileSize || headMessageId >= endMessageId)
										break; // eof or enough

									filePosition += messageHead.length;
									if (filePosition > fileSize)
										throw new RuntimeException("read message head eof.");
									fileInput.readFully(messageHead);
									var bbHead = ByteBuffer.Wrap(messageHead);
									messageId = bbHead.ReadLong8(); // 不再跳过：校验记录 id
									messageSize = bbHead.ReadInt4();
									if (messageId != headMessageId)
										throw new RuntimeException("read message id mismatch. topic=" + topic
												+ " partition=" + partitionId
												+ " expectMessageId=" + headMessageId
												+ " actualMessageId=" + messageId);
								}
							}
						} else {
							// seekForPrev 在空索引表上定位失败：整个循环体被跳过即无进展自旋。
							throw messageIndexNotFound(headMessageId);
						}
					}
				} else {
					// 索引段缺失（floorEntry 为 null）：与上面索引项缺失同型的无进展自旋，一并报错。
					throw messageIndexNotFound(headMessageId);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			// 计数与读路径同生共死：任何退出路径（含异常）都必须归零，否则回收从此永久跳过。
			activeFills.decrementAndGet();
		}
		return headMessageId;
	}

	/**
	 * 水位线整段回收（loadMonitorTimer 周期触发，批量低频不占热路径）。
	 * <p>
	 * 条件：firstMessageId 越过段尾（即该段全部消息已确认）才整段回收——at-least-once 契约不破；
	 * 水位在段中间不触发（只整段回收，天然防御历史损坏形态"first 回拨到段中间"）。
	 * 末段（活跃追加目标）永不回收。
	 * <p>
	 * 软删除窗口：候选段自首次被观察到"完全确认"起保留 delayMs 再回收（误判水位的最后防线，
	 * MQConfig.SegmentRecycleDelayMs，窗口粒度受 loadMonitorTimer 周期约束）。
	 * <p>
	 * 与 fillMessage 读路径互斥：入口与锁内双检跳过 + 终结原语排空（在飞 fill
	 * 持段索引迭代器与文件句柄，dropTable/删文件与其并发是 native use-after-free）。
	 * 非零时本轮跳过（不持锁等待 fill 批次，appendMessage 不被回收阻塞），下轮再试；
	 * 双检后与 remove 之间插入的逃逸 fill 由原语的置标记→排空→drop 三段承接（正确性
	 * 论证见 recycleSegment 注释）。残余竞态（fill 任务已按旧水位计算出区间、尚未开始执行）
	 * 在 fill 侧表现为 messageIndexNotFound / destroy-pending 的瞬时失败，由 pullMessage
	 * 既有的失败-复位-重试路径自愈，无数据损坏。
	 * <p>
	 * 与删除路径互斥：close 在本类锁内置 closed，tryRecycle 入口锁内复查即返回
	 * ——回收定时器（loadMonitorTimer 周期驱动，无 stopped/managementLock 闸）与
	 * deletePartitionStorage（removePartition→close 之后 dropTable 毁 meta/index 句柄）不再相交。
	 */
	public void tryRecycle(long delayMs) {
		if (activeFills.get() != 0)
			return; // 在飞fill排空（有界：fill装载maxFillMessageCount条即归零），本轮跳过
		lock.lock();
		try {
			if (closed)
				return; // 分区已 close（删除/停机路径先行，见字段注释）：closed 在
					// close 的本锁内置位，此处锁内读即精确——置位后回收通路（metaConsistent 的
					// meta.get、recycleSegment 的 dropTable）不再触碰可能已被 deletePartitionStorage
					// 毁掉的句柄。停机方向的 belt-and-braces 闸见 MQSingle.tryRecycleSegments。
			while (indexes.size() > 1) { // 只剩末段时无候选
				var keyIt = indexes.keySet().iterator();
				var oldest = keyIt.next();
				var second = keyIt.next();
				if (firstMessageId < second)
					break; // 最老段未完全确认：水位线未越过段尾
				// drop前一致性校验：内存位点与meta持久化值不一致（meta损坏/写丢失读出偏大值）
				// 时拒绝回收——dropTable 不可逆，宁可磁盘泄漏不可误删未消费段。
				if (!metaConsistent()) {
					logger.error("mq segment recycle skipped: meta inconsistent. topic={} partition={}"
									+ " first={} next={}", topic, partitionId, firstMessageId, nextMessageId);
					recycleCandidateBase = -1;
					return;
				}
				var now = System.currentTimeMillis();
				if (recycleCandidateBase != oldest) {
					// 软删除窗口起点：该段首次被观察到"完全确认"
					recycleCandidateBase = oldest;
					recycleCandidateSince = now;
				}
				if (now - recycleCandidateSince < delayMs)
					break; // 窗口内保留
				if (activeFills.get() != 0)
					return; // 锁内复查：入口检查后有新fill进入（跳过而非持锁等待，下轮再试）
				recycleSegment(oldest);
				recycleCandidateBase = -1; // 下一个候选重新起算窗口
			}
			if (indexes.size() <= 1)
				recycleCandidateBase = -1; // 无候选可守
		} finally {
			lock.unlock();
		}
	}

	// 一致性判据：meta持久化的next/first与内存值相等、且first<=next；meta读失败视同不一致
	// （拒绝回收，宁可磁盘泄漏不可误删——dropTable不可逆）。
	private boolean metaConsistent() {
		try {
			var nextInDb = meta.get(nextMessageIdName);
			var firstInDb = meta.get(firstMessageIdName);
			return null != nextInDb && null != firstInDb
					&& ByteBuffer.ToLongBE(nextInDb, 0) == nextMessageId
					&& ByteBuffer.ToLongBE(firstInDb, 0) == firstMessageId
					&& firstMessageId <= nextMessageId;
		} catch (RocksDBException e) {
			return false;
		}
	}

	// 销毁走 RocksDatabase 单一终结原语（mq-02：原"终检-放回"语义并入原语，不设两套）：
	// indexes 移除（锁外 fill 即不可再定位本段，防悬垂定位）→ destroyColumnFamily
	//（①除名+置毁标记 ②等在飞 fill 计数归零 ③drop）→ 删数据文件。
	// remove 与置标记之间插入的逃逸 fill（已取 floorEntry、尚未过租约检查点）由原语双检序
	// 承接（hb 论证）：fill 的 increment 严格先于其租约检查点（程序序）；检查点先于标记则其
	// increment 先于原语的计数读（AtomicInteger volatile 传递）——排空必见并等其退出（检查点
	// 在标记后则该轮放弃，同样退出）；排空读到 0 后进入的 fill 只能定位后继段（本段已出 map）。
	// 等待期间本类锁仍持有（tryRecycle 的 lock 内）：fill 的退出路径不取本锁（fillMessage 无锁）
	// 无自阻；常态（入口+锁内双检读0）等待为零，仅双检后的逃逸者会阻塞 appendMessage 至多一个
	// fill 批次。
	// drop/删文件失败仅记日志不重试：重启后loadMQ按文件扫描重注册列族，下轮回收重新收敛。
	private void recycleSegment(long base) {
		var indexTable = indexes.remove(base); // ConcurrentSkipListMap.remove原子，锁外fill立即可见
		if (null == indexTable)
			return;
		try {
			database.destroyColumnFamily(topic + "." + partitionId + "." + base, inFlightFillCheck());
		} catch (RocksDBException e) {
			logger.error("mq segment recycle dropTable failed, keep data file for restart rescan."
					+ " topic={} partition={} segment={}", topic, partitionId, base, e);
			return; // 不删文件：残留供重启重扫（段已出indexes，不再被读路径定位）
		}
		var file = new File(new File(home, topic), partitionId + "." + base);
		var bytes = file.length();
		if (file.delete())
			logger.info("mq segment recycled. topic={} partition={} segment={} bytes={}",
					topic, partitionId, file.getName(), bytes);
		else
			logger.warn("mq segment recycle delete file failed. topic={} partition={} file={}",
					topic, partitionId, file);
	}

	public void increaseFirstMessageId() {
		lock.lock();
		try {
			if (firstMessageId < nextMessageId) {
				var newFirstMessageId = firstMessageId + 1;
				var bbFirstMessageId = new byte[8];
				ByteBuffer.longBeHandler.set(bbFirstMessageId, 0, newFirstMessageId);
				meta.put(firstMessageIdName, bbFirstMessageId);
				// 持久化成功后才推进内存位点：meta.put 抛异常时保持"未推进"（否则内存位点越过
				// 未持久化的值，调用方重推后再次推进会跳过一条消息），调用方（推送ack回调）才能
				// 以消息留队首+位点未动重推同一条。
				firstMessageId = newFirstMessageId;
			}
		} catch (RocksDBException e) {
			throw new RuntimeException(e);
		} finally {
			lock.unlock();
		}
	}

	public void appendMessage(BMessage.Data message) {
		lock.lock();
		try {
			var bb = ByteBuffer.Allocate();
			bb.WriteLong8(nextMessageId);
			var sizeOffset = bb.WriteIndex;
			bb.WriteInt4(0);
			message.encode(bb);
			ByteBuffer.intLeHandler.set(bb.Bytes, sizeOffset, bb.WriteIndex - sizeOffset - 4);

			// 上次短写的回滚未证实成功：先补齐回滚（O_APPEND 恒接物理尾，
			// 孤儿前缀不物理除掉，本次追加必错位）。补齐仍失败则拒绝追加——fail-safe 优于
			// 错位追加（调用方回 rpc 错误，等磁盘恢复后的下次重试）。
			if (tornWritePending) {
				rollbackTornTail();
				if (tornWritePending)
					throw new IllegalStateException("mq append rejected: torn tail rollback still failing."
							+ " topic=" + topic + " partition=" + partitionId + " file=" + lastFile);
			}
			var fileOffset = lastFileOutputStream.getChannel().size();
			try {
				lastFileOutputStream.write(bb.Bytes, bb.ReadIndex, bb.size());
			} catch (IOException e) {
				// 短写回滚：write 出错前可能已持久化记录的前缀字节（磁盘满/IO错，
				// POSIX 短写语义），且进程继续运行（磁盘腾空后生产者重发是运维常态）。不回滚的话，
				// O_APPEND 使下一次 append 接在孤儿前缀之后——完整记录接在自己的撕裂前缀后面，
				// 段内物理布局错位：fillMessage 定位环按孤儿头（id/size 与真记录相同）命中后跨界
				// 读体，混合字节被 decode 成"成功"的消息静默投递；recoverTornTail 也被同 id 垃圾头
				// 欺骗（pos += 12+size 落进真记录体内、expectId 到顶退出），truncate 反向截掉真记录
				// 尾部，损坏被固化为"已提交记录体短缺"。截回上次成功结尾使"meta.next 是唯一提交
				// 点"的既有恢复语义重新成立（nextMessageId 未推进=该记录未提交，孤儿前缀物理消失，
				// 重启恢复无异）。磁盘满下截断释放空间通常立即成功；失败则悬挂到下次 append 前补滚。
				tornWritePending = true;
				tornRollbackOffset = fileOffset;
				rollbackTornTail();
				throw e; // 上抛：索引/meta 未写，调用方（SendMessage handler）回错误，分区继续运行
			}
			// 提交语义收口：文件 write 成功不等于提交，meta.put 成功才算（与 recoverTornTail 的
			// "meta.next 是唯一提交点"恢复语义对齐）。文件写成功后至 meta.put 成功前的任何异常
			//（(A) 索引 put、(B) meta.put 的 RocksDBException 等）都按 torn 记账：物理截断回
			// fileOffset，不留"完整孤儿记录"——否则 rocksdb 错误消除后生产者重试以同 id 再写，
			// 段内双同 id 完整记录（孤儿在前），fillMessage 顺序装载时错位投递。
			var idIncremented = false;
			try {
				if (nextMessageId % makeIndexPeriod == 0) {
					var bytesMessageId = new byte[8];
					ByteBuffer.longBeHandler.set(bytesMessageId, 0, nextMessageId);
					var bytesFileOffset = new byte[8];
					ByteBuffer.longBeHandler.set(bytesFileOffset, 0, fileOffset);
					indexes.lastEntry().getValue().put(bytesMessageId, bytesFileOffset);
				}

				// 递增消息编号，准备下一次使用，并且马上写入meta。
				++nextMessageId;
				idIncremented = true;
				var bbNextMessageId = new byte[8];
				ByteBuffer.longBeHandler.set(bbNextMessageId, 0, nextMessageId);
				meta.put(nextMessageIdName, bbNextMessageId);
			} catch (Exception e) {
				// 提交点未到，与上面 IOException 路径同构的 torn 记账：截断成功则孤儿记录物理
				// 消失（重启无异）；失败则悬挂到下次 append 前补滚（上面的 torn 检查段语义不变）。
				// (A) put 异常但条目已可见（rocksdb 非事务写"报错但已写"的窄形态）时无碍：其
				// id==回退后的 nextMessageId，不在装载区间 [firstMessageId,nextMessageId) 内，
				// 重启 recoverTornTail 的 deleteIndexFrom 亦会清除。
				if (idIncremented)
					--nextMessageId; // (B) 失败发生在 ++ 之后：内存位点回退，恢复"该记录未提交"
				tornWritePending = true;
				tornRollbackOffset = fileOffset;
				rollbackTornTail();
				throw e; // 上抛（外层包 RuntimeException）：索引未写/meta 未提交，调用方回错误，分区继续运行
			}

			// 文件大小超过100M，就新建文件和索引表。
			// 除了文件大小，还需额外判断下一个消息Id也是makeIndexPeriod整除，这样新文件的第一个消息肯定会被建立索引，
			// 新文件第一个消息必须建立索引，否则开头的消息定位不到。
			if (fileOffset + bb.size() >= trunkFileSize && nextMessageId % makeIndexPeriod == 0) {
				// 先开后关+资源就绪才发布：close→new 顺序下构造失败（EMFILE/
				// ENOSPC/目录项冲突等）使字段停留在已关闭的旧流上——此后每次 append 在
				// getChannel().size() 恒抛 ClosedChannelException，分区追加能力到重启前永久丧失
				//（本条消息已提交，无数据损坏，纯运行期可用性损失）。三要点：
				// ① getOrAddTable 幂等先行（文件面失败无外渗：open 失败原子不留文件，文件扫描发现规则
				// 不注册无文件段——重启无幽灵 lastEntry；列族面的外渗由失败回滚闭合，
				// 见下方 catch——"失败无外渗"对列族维度不成立）；
				// ② new 新流先于关旧流：构造失败则旧流仍开、字段未动，滚段留待条件重合自然重试
				//（size 条件持续成立，modulo 条件在下一个 makeIndexPeriod 整除点重合，旧段有限
				// 超限 <makeIndexPeriod 条）；
				// ③ 字段替换先于关旧流：oldStream.close 极难失败，失败仅遗留待 GC finalize 的旧 fd，
				// 字段已一致（追加面不受影响；本次 append 已提交，上抛错误由生产者重试=at-least-once）。
				// indexes.put 同样后移到新流构造之后：失败不在内存留无数据文件的幽灵段
				//（tryRecycle 会去 drop 一个空表）。
				var topicDir = new File(home, topic);
				var nextFile = new File(topicDir, partitionId + "." + nextMessageId);
				var nextTableName = topic + "." + partitionId + "." + nextMessageId;
				// isNew 判"本调用新建"：createColumnFamily 是即刻持久化的外部副作用，
				// open 失败时列族已建已注册而无数据文件——不在 indexes、重启扫描不注册（无文件）、
				// deletePartitionStorage 按文件名反推列族名也清不到（无文件则永不 drop），成为
				// rocksdb 元数据里的永久孤儿；且 EMFILE 持续期间每次失败尝试的 base 名不同
				//（nextMessageId 递增，滚段点每 makeIndexPeriod 整除点重合一次），每 100 条消息
				// 泄漏一个空列族，无上界。失败回滚：仅对新建者 dropTable 后原样上抛——重试路径
				// base 已换新名（幂等先行不受影响）；非本调用新建（前次回滚失败残留等）不是本次
				// 的泄漏，不 drop，留给其属主语义处置。
				var isNew = new OutObject<Boolean>();
				var nextTable = database.getOrAddTable(nextTableName, isNew);
				FileOutputStream next;
				try {
					next = new FileOutputStream(nextFile, true);
				} catch (IOException e) {
					if (Boolean.TRUE.equals(isNew.value))
						try {
							database.dropTable(nextTableName); // 回滚本次新建的列族（文件面本就未发布）
						} catch (RocksDBException dropEx) {
							// 回滚失败如实告警不掩盖原始 open 异常：孤儿列族残留（无文件、磁盘真相
							// 上报/删除清理均不可见），仅 rocksdb 自身错误时可发生，概率极低。
							logger.error("mq segment roll rollback dropTable failed, orphan column family kept."
									+ " topic={} partition={} segment={}", topic, partitionId, nextMessageId, dropEx);
						}
					throw e; // 上抛（外层包 RuntimeException）：字段未动，旧流可用语义不变
				}
				var oldStream = lastFileOutputStream;
				lastFile = nextFile;
				lastFileOutputStream = next;
				indexes.put(nextMessageId, nextTable);
				oldStream.close();
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			lock.unlock();
		}
	}

	// 撕裂写回滚（lock内调用）：截回 tornRollbackOffset（上次成功结尾）。
	// 成功即清悬挂——此后 O_APPEND 的写入位置与"已提交结尾"重新对齐；失败保持悬挂
	//（下次 append 前重试，期间 fillMessage 只读已提交区 [firstMessageId,nextMessageId)，
	// 不受孤儿字节影响；若进程就此退出，重启时 recoverTornTail 亦按未提交尾巴正确截断——
	// 但 O_APPEND 语义下运行期不除孤儿必错位，故悬挂未解除不得继续追加）。
	private void rollbackTornTail() {
		try {
			lastFileOutputStream.getChannel().truncate(tornRollbackOffset);
			tornWritePending = false;
			// 措辞覆盖两种 torn 形态：write 失败的"部分写前缀"与提交点未到（索引/meta put 失败）的
			// "完整未提交记录"——后者同样在此截除，不能只叫 partial-write 误导排查方向。
			logger.warn("mq append torn tail rolled back. topic={} partition={} file={} truncateTo={}",
					topic, partitionId, lastFile.getName(), tornRollbackOffset);
		} catch (IOException e) {
			logger.error("mq append torn tail rollback failed, keep pending for retry before next append."
					+ " topic={} partition={} file={}", topic, partitionId, lastFile.getName(), e);
		}
	}

	public void close() throws IOException {
		// 置位与关流均在自身锁内：close 返回即不变式"此后 tryRecycle 恒被 closed
		// 拒绝"成立——与 tryRecycle 临界区（锁内 meta.get → dropTable → 删文件）互斥，其后的
		// deletePartitionStorage（dropTable meta/index 列族并销毁句柄）与回收通路不再相交。关流
		// 一并移入锁内：锁序 MQSingle→fileWithIndex（MQSingle.close 持分区锁调用本方法）与
		// calculateFill/appendMessage 既有方向一致，本类锁从不反向取 MQSingle 锁，无新交叠。
		lock.lock();
		try {
			closed = true;
			lastFileOutputStream.close();
		} finally {
			lock.unlock();
		}
	}
}
