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
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Builtin.MQ.BMessage;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.OutLong;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.RocksDBException;

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
		var files = topicDir.listFiles();
		if (null != files) {
			for (var file : files) {
				var partIndex = file.getName().split("\\.");
				if (partIndex.length < 2)
					continue;
				try {
					var pid = Integer.parseInt(partIndex[0]);
					if (pid != partitionId)
						continue;
					var index = Long.parseLong(partIndex[1]);
					indexes.put(index, database.getOrAddTable(
							topic + "." + partitionId + "." + index));
				} catch (NumberFormatException ex) {
					// continue; 忽略无法解析为"分区号.消息号"的杂散文件名。
				}
			}
		}
		var lastEntry = indexes.lastEntry();
		if (lastEntry == null) {
			lastFile = new File(topicDir, partitionId + ".0");
			indexes.put(0L, database.getOrAddTable(topic + "." + partitionId + ".0"));
		} else {
			lastFile = new File(topicDir, partitionId + "." + lastEntry.getKey());
		}
		// 【FND2-G2-1】追加流打开前先恢复撕裂尾：一旦放任孤儿字节，之后的 appendMessage 会把
		// 新消息接在垃圾后面，错位被固化进文件，fillMessage 的按 id 跳扫从此确定性失败。
		recoverTornTail();
		lastFileOutputStream = new FileOutputStream(lastFile, true); // todo 没有buffer是不是很慢？
	}

	// 【FND2-G2-1】撕裂尾恢复（类 WAL recovery，仅构造时执行一次，在打开追加流之前）。
	// appendMessage 先写文件后写 meta：崩溃/掉电/磁盘满会把"半条记录"留在文件尾（掉电丢页缓存时
	// 甚至连已提交记录都会缺尾）；无恢复时下一条消息接在孤儿字节之后，fillMessage 按 12 字节头
	// 跳扫从错位处步步读歪——回填确定性永久失败，分区投递停摆（70a4f65cd 的失败-复位-重试
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
			var anchorId = segBase;
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
					if (messageSize < 0 || messageSize > remaining - 12)
						break; // 记录体越过文件尾（或负长度）：写了一半的尾巴，头字段同样不可信
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
	 * 从文件中装载消息填充到队列中。
	 * 注意：参数未经验证，需要外部确保正确（请使用calculateFill得到参数）。
	 * @param messageQueue 队列
	 * @param headMessageId 开始Id。
	 * @param endMessageId 结束Id。
	 */
	public void fillMessage(Queue<BMessage.Data> messageQueue, long headMessageId, long endMessageId) {
		// 锁内计算需要读取的消息数量，并且推进firstMessageId。
		try {
			while (headMessageId < endMessageId) {
				var floor = indexes.floorEntry(headMessageId);
				if (null != floor) {
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
								// locate headMessageId
								while (true) {
									filePosition += messageHead.length;
									if (filePosition > fileSize)
										throw new RuntimeException("locate message eof.");
									fileInput.read(messageHead);
									var bbHead = ByteBuffer.Wrap(messageHead);
									messageId = bbHead.ReadLong8();
									messageSize = bbHead.ReadInt4();
									if (messageId == headMessageId)
										break; // message found.
									if (fileInput.skipBytes(messageSize) < messageSize)
										throw new RuntimeException("message not found"); // 忽略的长度不够，表示数据文件被截断了。
									filePosition += messageSize;
								}

								// fill now
								while (true) {
									var messageBuffer = new byte[messageSize];
									filePosition += messageBuffer.length;
									if (filePosition > fileSize)
										throw new RuntimeException("read message body eof.");
									fileInput.read(messageBuffer);
									var message = new BMessage.Data();
									message.decode(ByteBuffer.Wrap(messageBuffer));
									messageQueue.add(message);

									headMessageId++;
									if (filePosition >= fileSize || headMessageId >= endMessageId)
										break; // eof or enough

									filePosition += messageHead.length;
									if (filePosition > fileSize)
										throw new RuntimeException("read message head eof.");
									fileInput.read(messageHead);
									var bbHead = ByteBuffer.Wrap(messageHead);
									bbHead.ReadLong8(); // skip result
									messageSize = bbHead.ReadInt4();
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
		}
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

			var fileOffset = lastFileOutputStream.getChannel().size();
			lastFileOutputStream.write(bb.Bytes, bb.ReadIndex, bb.size());
			if (nextMessageId % makeIndexPeriod == 0) {
				var bytesMessageId = new byte[8];
				ByteBuffer.longBeHandler.set(bytesMessageId, 0, nextMessageId);
				var bytesFileOffset = new byte[8];
				ByteBuffer.longBeHandler.set(bytesFileOffset, 0, fileOffset);
				indexes.lastEntry().getValue().put(bytesMessageId, bytesFileOffset);
			}

			// 递增消息编号，准备下一次使用，并且马上写入meta。
			++nextMessageId;
			var bbNextMessageId = new byte[8];
			ByteBuffer.longBeHandler.set(bbNextMessageId, 0, nextMessageId);
			meta.put(nextMessageIdName, bbNextMessageId);

			// 文件大小超过100M，就新建文件和索引表。
			// 除了文件大小，还需额外判断下一个消息Id也是makeIndexPeriod整除，这样新文件的第一个消息肯定会被建立索引，
			// 新文件第一个消息必须建立索引，否则开头的消息定位不到。
			if (fileOffset + bb.size() >= trunkFileSize && nextMessageId % makeIndexPeriod == 0) {
				var topicDir = new File(home, topic);
				lastFile = new File(topicDir, partitionId + "." + nextMessageId);
				indexes.put(nextMessageId, database.getOrAddTable(
						topic + "." + partitionId + "." + nextMessageId));
				lastFileOutputStream.close();
				lastFileOutputStream = new FileOutputStream(lastFile, true); // todo 没有buffer是不是很慢？
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			lock.unlock();
		}
	}

	public void close() throws IOException {
		lastFileOutputStream.close();
	}
}
