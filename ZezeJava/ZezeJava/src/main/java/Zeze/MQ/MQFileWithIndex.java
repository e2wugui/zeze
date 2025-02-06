package Zeze.MQ;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Builtin.MQ.BMessage;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.RocksDatabase;
import org.rocksdb.RocksDBException;

public class MQFileWithIndex {
	private final ReentrantLock lock = new ReentrantLock();
	private final TreeMap<Long, RocksDatabase.Table> indexes = new TreeMap<>();
	private final RocksDatabase.Table meta;
	private final MQManager manager;
	private final String topic;
	private final int partitionId;
	private File lastFile;
	private FileOutputStream lastFileOutputStream;

	private static final byte[] nextMessageIdName = "nextMessageId".getBytes(StandardCharsets.UTF_8);
	private long nextMessageId;

	private static final byte[] firstMessageIdName = "firstMessageId".getBytes(StandardCharsets.UTF_8);
	private long firstMessageId;

	private static int trunkFileSize = 100 * 1024 * 1024;

	public File getLastFile() {
		return lastFile;
	}

	public long getNextMessageId() {
		return nextMessageId;
	}

	public long getFirstMessageId() {
		return firstMessageId;
	}

	public MQFileWithIndex(MQManager manager, String topic, int partitionId) throws RocksDBException, FileNotFoundException {
		this.manager = manager;
		this.topic = topic;
		this.partitionId = partitionId;

		this.meta = manager.getRocksDatabase().getOrAddTable(topic + "." + partitionId);

		var nextMessageIdValue = this.meta.get(nextMessageIdName);
		nextMessageId = null != nextMessageIdValue ? ByteBuffer.Wrap(nextMessageIdValue).ReadLong8() : 0;
		var firstMessageIdValue = this.meta.get(firstMessageIdName);
		firstMessageId = null != firstMessageIdValue ? ByteBuffer.Wrap(firstMessageIdValue).ReadLong8() : 0;

		var topicDir = new File(manager.getHome(), topic);
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
					indexes.put(index, manager.getRocksDatabase().getOrAddTable(
							topic + "." + partitionId + "." + index));
				} catch (Exception ex) {
					continue;
				}
			}
		}
		var lastEntry = indexes.lastEntry();
		if (lastEntry == null) {
			lastFile = new File(topicDir, partitionId + ".0");
			indexes.put(0L, manager.getRocksDatabase().getOrAddTable(topic + "." + partitionId + ".0"));
		} else {
			lastFile = new File(topicDir, partitionId + "." + lastEntry.getKey());
		}
		lastFileOutputStream = new FileOutputStream(lastFile, true); // todo 没有buffer是不是很慢？
	}

	public void fillMessage(Queue<BMessage.Data> out, int maxLength) {
		// 锁内计算需要读取的消息数量，并且推进firstMessageId。
		long headMessageId;
		long endMessageId;
		lock.lock();
		try {
			var fillCount = Math.min(this.nextMessageId - this.firstMessageId, maxLength);
			headMessageId = this.firstMessageId;
			endMessageId = this.firstMessageId + fillCount;
		} finally {
			lock.unlock();
		}
		try {
			while (headMessageId < endMessageId) {
				var floor = indexes.floorEntry(headMessageId);
				if (null != floor) {
					var headMessageIdValue = new byte[8];
					ByteBuffer.longLeHandler.set(headMessageIdValue, 0, headMessageId);
					var floorIt = floor.getValue().iterator();
					floorIt.seekForPrev(headMessageIdValue);
					if (floorIt.isValid()) {
						var topicDir = new File(manager.getHome(), topic);
						var file = new File(topicDir, partitionId + "." + floor.getKey());
						var fileInput = new RandomAccessFile(file, "r");
						try {
							fileInput.seek(ByteBuffer.Wrap(floorIt.value()).ReadLong8());
							long messageId;
							int messageSize;
							var messageHead = new byte[12];
							// locate headMessageId
							while (true) {
								fileInput.read(messageHead);
								var bbHead = ByteBuffer.Wrap(messageHead);
								messageId = bbHead.ReadLong8();
								messageSize = bbHead.ReadInt4();
								if (messageId == headMessageId)
									break; // message found.
								if (fileInput.skipBytes(messageSize) < messageSize)
									throw new RuntimeException("message not found"); // 忽略的长度不够，表示数据文件被阶段了。
							}

							// fill now
							var fileInputChannel = fileInput.getChannel();
							// 下面while判断的时候，headMessageId肯定小于endMessageId。
							while (fileInputChannel.position() < fileInputChannel.size()) {
								var messageBuffer = new byte[messageSize];
								fileInput.read(messageBuffer);
								var message = new BMessage.Data();
								message.decode(ByteBuffer.Wrap(messageBuffer));
								out.add(message);

								headMessageId++;
								if (fileInputChannel.position() >= fileInputChannel.size() || headMessageId >= endMessageId)
									break; // eof or enough

								fileInput.read(messageHead);
								var bbHead = ByteBuffer.Wrap(messageHead);
								bbHead.ReadLong8(); // skip result
								messageSize = bbHead.ReadInt4();
							}
						} finally {
							fileInput.close();
						}
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void increaseFirstMessageId() {
		lock.lock();
		try {
			firstMessageId++;
			var bbFirstMessageId = new byte[8];
			ByteBuffer.longLeHandler.set(bbFirstMessageId, 0, firstMessageId);
			meta.put(firstMessageIdName, bbFirstMessageId);
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
			ByteBuffer.intLeHandler.set(bb.Bytes,  sizeOffset, bb.WriteIndex - sizeOffset - 4);

			var fileOffset = lastFileOutputStream.getChannel().size();
			lastFileOutputStream.write(bb.Bytes, bb.ReadIndex, bb.size());
			if (nextMessageId % 500 == 0) {
				var bytesMessageId = new byte[8];
				ByteBuffer.longLeHandler.set(bytesMessageId, 0, nextMessageId);
				var bytesFileOffset = new byte[8];
				ByteBuffer.longLeHandler.set(bytesFileOffset, 0, fileOffset);
				indexes.lastEntry().getValue().put(bytesMessageId, bytesFileOffset);
			}

			// 递增消息编号，准备下一次使用，并且马上写入meta。
			++nextMessageId;
			var bbNextMessageId = new byte[8];
			ByteBuffer.longLeHandler.set(bbNextMessageId, 0, nextMessageId);
			meta.put(nextMessageIdName, bbNextMessageId);

			// 文件大小超过100M，就新建文件和索引表。
			// 除了文件大小，还需额外判断下一个消息Id也是500整除，这样新文件的第一个消息肯定会被建立索引，
			// 新文件第一个消息必须建立索引，否则开头的消息定位不到。
			if (fileOffset + bb.size() >= trunkFileSize && nextMessageId % 500 == 0) {
				var topicDir = new File(manager.getHome(), topic);
				lastFile = new File(topicDir, partitionId + "." + nextMessageId);
				indexes.put(nextMessageId, manager.getRocksDatabase().getOrAddTable(
						topic + "." + partitionId + "." + nextMessageId));
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
