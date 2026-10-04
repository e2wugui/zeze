package Zeze.MQ;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import Zeze.Builtin.MQ.BMessage;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.RocksDatabase;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND2-G2-1 回归：MQFileWithIndex 打开时的撕裂尾恢复（类 WAL recovery）。
 * <p>
 * appendMessage 先写文件后写 meta：崩溃/掉电/磁盘满会把"半条记录"留在文件尾（掉电丢页缓存时
 * 甚至连已提交记录都会缺尾）。旧代码打开时无任何校验，下一条 appendMessage 把新消息接在孤儿
 * 字节之后，fillMessage 按 12 字节头跳扫从错位处步步读歪——回填确定性永久失败，分区投递停摆
 * （70a4f65cd 的失败-复位-重试对确定性损坏无能为力）。
 * <p>
 * 修复：构造时从最近已提交索引项顺序校验记录头连续性，按 meta 的 next 截断未提交尾巴并回拨
 * 位点/索引；只处理"尾部撕裂"，中间损坏 fatal 抛出（防自动截断静默丢中间数据）。
 * <p>
 * 注：文件放 src/MQ/ 但声明 package Zeze.MQ，与 TestMQFileWithIndexFillGuards 先例一致。
 */
@Fast
public class TestMQFileWithIndexTornTail {

	/** 构造含递增 Timestamp 的消息，回填后按 Timestamp 断言装载顺序。 */
	private static BMessage.Data messageOf(long id) {
		var message = new BMessage.Data();
		message.setTimestamp(id);
		return message;
	}

	/** 直写分区 meta（复刻实现内的表名与 key 编码），用于模拟掉电后 meta 与文件内容持久化乱序。 */
	private static void putMetaLong(RocksDatabase database, String key, long value) throws Exception {
		var bb = new byte[8];
		ByteBuffer.longBeHandler.set(bb, 0, value);
		database.getOrAddTable("topic.0").put(key.getBytes(StandardCharsets.UTF_8), bb);
	}

	/**
	 * 顺序扫描段文件前 count 条记录并返回其结尾偏移（记录头布局与实现一致：
	 * Long8(messageId) + Int4(messageSize)，读写均小端）。同时断言 id 连续性。
	 */
	private static long offsetAfter(File file, int count) throws Exception {
		try (var raf = new RandomAccessFile(file, "r")) {
			var fileSize = raf.length();
			var head = new byte[12];
			var pos = 0L;
			for (int i = 0; i < count; i++) {
				Assertions.assertTrue(fileSize - pos >= 12, "段文件记录数不足 " + count);
				raf.seek(pos);
				raf.readFully(head);
				var bb = ByteBuffer.Wrap(head);
				Assertions.assertEquals(i, bb.ReadLong8(), "记录 id 连续性");
				pos += 12 + bb.ReadInt4();
			}
			return pos;
		}
	}

	/** 断言回填结果恰为 [begin, end) 的按序 Timestamp。 */
	private static void assertFillInOrder(Queue<BMessage.Data> queue, long begin, long end) {
		Assertions.assertEquals(end - begin, queue.size(), "回填数量");
		var expect = begin;
		for (var message : queue)
			Assertions.assertEquals(expect++, message.getTimestamp(), "回填必须按 id 有序");
	}

	/** 取最深层异常消息（构造路径经 Task.forceThrow 可能不包一层，统一取最深即可）。 */
	private static String deepestMessage(Throwable e) {
		var msg = String.valueOf(e.getMessage());
		for (var cause = e.getCause(); cause != null; cause = cause.getCause())
			if (cause.getMessage() != null)
				msg = cause.getMessage();
		return msg;
	}

	/** 场景①：半条记录的头 7 字节（不足 12 字节头）落在完好记录之后，meta 未推进。 */
	@Test
	public void testTornTailTruncated(@TempDir Path tempDir) throws Exception {
		var home = tempDir.resolve("db").toString();
		long goodLength;
		var database = new RocksDatabase(home);
		var file = new MQFileWithIndex(home, database, "topic", 0);
		var dataFile = file.getLastFile();
		try {
			for (long id = 0; id < 3; id++)
				file.appendMessage(messageOf(id));
			goodLength = dataFile.length();
			Assertions.assertTrue(goodLength > 0);
			file.close();
		} finally {
			database.close();
		}

		// 崩溃模拟：appendMessage 写记录 3 的前 7 字节后进程消失（写文件半途，meta 未推进）。
		var bb = ByteBuffer.Allocate();
		bb.WriteLong8(3);
		bb.WriteInt4(123);
		Files.write(dataFile.toPath(), Arrays.copyOf(bb.Bytes, 7), StandardOpenOption.APPEND);

		try (var database2 = new RocksDatabase(home)) {
			var file2 = new MQFileWithIndex(home, database2, "topic", 0);
			try {
				// 打开即恢复：撕裂字节被截断，位点不动（meta 本来就停在 3）。
				Assertions.assertEquals(goodLength, dataFile.length(), "撕裂尾应被自动截断");
				Assertions.assertEquals(3, file2.getNextMessageId());
				Assertions.assertEquals(0, file2.getFirstMessageId());
				// 追加新消息成功且回填可定位（旧代码：跳扫从撕裂处读歪，回填确定性失败、投递停摆）。
				file2.appendMessage(messageOf(3));
				Assertions.assertEquals(4, file2.getNextMessageId());
				Queue<BMessage.Data> queue = new ConcurrentLinkedQueue<>();
				file2.fillMessage(queue, 0, 4);
				assertFillInOrder(queue, 0, 4);
			} finally {
				file2.close();
			}
		}
	}

	/**
	 * 场景②：掉电乱序持久化——记录 3 写了一半（头完整、体 5/100），meta 却已到 next=4、first=4
	 * （直入消息不等 fill 即被 ack 推进 first）。恢复须截断 + 回拨 next/first，防 first>next 倒挂。
	 */
	@Test
	public void testTornCommittedRecordRollsBack(@TempDir Path tempDir) throws Exception {
		var home = tempDir.resolve("db").toString();
		long goodLength;
		var database = new RocksDatabase(home);
		var file = new MQFileWithIndex(home, database, "topic", 0);
		var dataFile = file.getLastFile();
		try {
			for (long id = 0; id < 3; id++)
				file.appendMessage(messageOf(id));
			goodLength = dataFile.length();
			file.close();
		} finally {
			database.close();
		}

		var bb = ByteBuffer.Allocate();
		bb.WriteLong8(3);
		bb.WriteInt4(100);
		Files.write(dataFile.toPath(), Arrays.copyOf(bb.Bytes, 12 + 5), StandardOpenOption.APPEND);
		try (var dbMeta = new RocksDatabase(home)) {
			putMetaLong(dbMeta, "nextMessageId", 4);
			putMetaLong(dbMeta, "firstMessageId", 4);
		}

		try (var database2 = new RocksDatabase(home)) {
			var file2 = new MQFileWithIndex(home, database2, "topic", 0);
			try {
				Assertions.assertEquals(goodLength, dataFile.length(), "撕裂尾应被自动截断");
				Assertions.assertEquals(3, file2.getNextMessageId(), "已提交但缺尾的 next 应回拨");
				Assertions.assertEquals(3, file2.getFirstMessageId(), "first 应回拨到 next（防倒挂）");
				file2.appendMessage(messageOf(3));
				Queue<BMessage.Data> queue = new ConcurrentLinkedQueue<>();
				file2.fillMessage(queue, 0, 4);
				assertFillInOrder(queue, 0, 4);
			} finally {
				file2.close();
			}
		}
	}

	/** 场景③：完好文件不受恢复影响（长度不变、位点不动），追加与回填照常。 */
	@Test
	public void testIntactFileUntouched(@TempDir Path tempDir) throws Exception {
		var home = tempDir.resolve("db").toString();
		long goodLength;
		var database = new RocksDatabase(home);
		var file = new MQFileWithIndex(home, database, "topic", 0);
		var dataFile = file.getLastFile();
		try {
			for (long id = 0; id < 3; id++)
				file.appendMessage(messageOf(id));
			goodLength = dataFile.length();
			file.close();
		} finally {
			database.close();
		}

		try (var database2 = new RocksDatabase(home)) {
			var file2 = new MQFileWithIndex(home, database2, "topic", 0);
			try {
				Assertions.assertEquals(goodLength, dataFile.length(), "完好文件不得被截断");
				Assertions.assertEquals(3, file2.getNextMessageId());
				Assertions.assertEquals(0, file2.getFirstMessageId());
				file2.appendMessage(messageOf(3));
				Queue<BMessage.Data> queue = new ConcurrentLinkedQueue<>();
				file2.fillMessage(queue, 0, 4);
				assertFillInOrder(queue, 0, 4);
			} finally {
				file2.close();
			}
		}
	}

	/** 场景④：中间损坏（记录完整在文件内但 id 错位，其后还有完好记录）不自动修，fatal 带定位。 */
	@Test
	public void testMiddleCorruptionFatal(@TempDir Path tempDir) throws Exception {
		var home = tempDir.resolve("db").toString();
		var database = new RocksDatabase(home);
		var file = new MQFileWithIndex(home, database, "topic", 0);
		var dataFile = file.getLastFile();
		try {
			for (long id = 0; id < 3; id++)
				file.appendMessage(messageOf(id));
			file.close();
		} finally {
			database.close();
		}

		// 磁盘腐蚀模拟：记录 1（中间）的 id 首字节被改坏，size 字段不动（记录仍完整落在文件内，
		// 其后的记录 2 完好）——自动截断会静默丢掉记录 1..2，必须 fatal 交人工处置。
		var record1Offset = offsetAfter(dataFile, 1);
		try (var raf = new RandomAccessFile(dataFile, "rw")) {
			raf.seek(record1Offset);
			raf.write(0x7f); // 原小端 id=1（01 00 ...）→ 0x7f 开头，id 错位
		}

		try (var database2 = new RocksDatabase(home)) {
			var ex = Assertions.assertThrows(RuntimeException.class,
					() -> new MQFileWithIndex(home, database2, "topic", 0));
			var msg = deepestMessage(ex);
			Assertions.assertTrue(msg.contains("corrupted in middle"), "message=" + msg);
			Assertions.assertTrue(msg.contains("position=" + record1Offset)
					&& msg.contains("expectMessageId=1"), "fatal 须带定位信息：message=" + msg);
		}
	}

	/**
	 * 场景⑤：文件与索引写到 2，meta 只持久化到 2 之前——未提交的完好孤儿记录连同悬垂索引项
	 * 一并按 meta.next 截断/回拨（孤儿不截断则 append 复用 id 后 dup-id 错位投递；悬垂索引
	 * 指向截断区，fillMessage 经它定位必失败）。
	 * <p>
	 * 孤儿索引项（id=100）手工伪造：与 testHugeIndexOffsetNotTruncated 的 Table.put 手法一致，
	 * 等价于"appendMessage 内索引 put 已落盘、meta.put 未落盘"的崩溃窗口，且不依赖
	 * makeIndexPeriod 的取值（TestFileWithIndexed 并行运行时会改写该静态字段）。
	 */
	@Test
	public void testOrphanTailAndIndexRolledBack(@TempDir Path tempDir) throws Exception {
		var home = tempDir.resolve("db").toString();
		long endOfRecord1;
		var database = new RocksDatabase(home);
		var file = new MQFileWithIndex(home, database, "topic", 0);
		var dataFile = file.getLastFile();
		try {
			for (long id = 0; id < 3; id++)
				file.appendMessage(messageOf(id));
			endOfRecord1 = offsetAfter(dataFile, 2); // 记录 1 的结尾 = 按 meta.next 的截断点
			file.close();
		} finally {
			database.close();
		}

		// 崩溃模拟：id=2 的记录与 id=100 的索引项已落盘（appendMessage 内索引 put 先于 meta.put），
		// 但 meta 只持久化到 next=2（id=2 的 meta.put 未完成）。
		try (var dbMeta = new RocksDatabase(home)) {
			var indexTable = dbMeta.getOrAddTable("topic.0.0");
			var key100 = new byte[8];
			ByteBuffer.longBeHandler.set(key100, 0, 100);
			var value100 = new byte[8];
			ByteBuffer.longBeHandler.set(value100, 0, dataFile.length());
			indexTable.put(key100, value100);
			putMetaLong(dbMeta, "nextMessageId", 2);
		}

		try (var database2 = new RocksDatabase(home)) {
			var file2 = new MQFileWithIndex(home, database2, "topic", 0);
			try {
				Assertions.assertEquals(2, file2.getNextMessageId(), "未提交尾巴应按 meta.next 回拨");
				Assertions.assertEquals(endOfRecord1, dataFile.length(), "未提交的完好孤儿记录应被截断");
				var key100 = new byte[8];
				ByteBuffer.longBeHandler.set(key100, 0, 100);
				Assertions.assertNull(database2.getOrAddTable("topic.0.0").get(key100),
						"指向截断区的悬垂索引项应被回拨");
				// 复用回拨后的 id 2 追加成功，回填 [0,3) 可定位。
				file2.appendMessage(messageOf(2));
				Queue<BMessage.Data> queue = new ConcurrentLinkedQueue<>();
				file2.fillMessage(queue, 0, 3);
				assertFillInOrder(queue, 0, 3);
			} finally {
				file2.close();
			}
		}
	}
}
