package Zeze.MQ;

import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
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
 * FND24 mq-01 回归：段文件出现同 id 双记录（appendMessage 文件写成功后索引/meta put 失败
 * 残留的"完整孤儿记录"+生产者重试重写形态）时，fillMessage 第二循环只丢弃记录头 id 不校验，
 * 孤儿体被当 id=N 投递、真实体被当 id=N+1——id 与内容系统性错位，无任何日志（静默错投）。
 * 修复：第二循环逐头校验记录 id==期望递增序列，不符响亮抛错进 pullMessage 既有失败-重试路径。
 * <p>
 * 注入在实例打开态完成：close 后重开会被 recoverTornTail 的"中间损坏 fatal"拦截（另一层
 * 防御，不在本用例），本用例只测 fillMessage 装载路径的校验本身。tail/中部两种注入形态
 * 分别覆盖"孤儿在真实记录之后/之前"两个方向。
 */
@Fast
public class TestMQFileWithIndexDuplicateRecordId {

	/** fillMessage 外层 catch(Exception) 会包一层 RuntimeException，取最深层消息做断言。 */
	private static String deepestMessage(Throwable e) {
		var msg = String.valueOf(e.getMessage());
		for (var cause = e.getCause(); cause != null; cause = cause.getCause())
			if (cause.getMessage() != null)
				msg = cause.getMessage();
		return msg;
	}

	/** 12 字节记录头 = Long8(id) + Int4(size)，解析序与 fillMessage/recoverTornTail 一致。 */
	private static int headSize(byte[] head) {
		var bb = ByteBuffer.Wrap(head);
		bb.ReadLong8(); // skip id
		return bb.ReadInt4();
	}

	/** 记录边界走查：返回每条记录的起始偏移（完整序列断言由调用方做）。 */
	private static long[] recordStarts(byte[] bytes) {
		var starts = new long[8];
		var count = 0;
		var pos = 0;
		while (pos + 12 <= bytes.length) {
			if (count == starts.length)
				starts = Arrays.copyOf(starts, count * 2);
			starts[count++] = pos;
			pos += 12L + headSize(Arrays.copyOfRange(bytes, pos, pos + 12));
		}
		Assertions.assertEquals(bytes.length, pos, "段文件必须是完整记录序列（测试前置）");
		return Arrays.copyOf(starts, count);
	}

	/** 打开实例的活跃段尾部追加"最后一条记录的字节副本"（id 重复、体相同）。 */
	private static void appendDuplicateOfLastRecord(Path segmentFile) throws Exception {
		var bytes = Files.readAllBytes(segmentFile);
		var starts = recordStarts(bytes);
		var lastStart = starts[starts.length - 1];
		try (var raf = new RandomAccessFile(segmentFile.toFile(), "rw")) {
			raf.seek(raf.length());
			raf.write(bytes, (int)lastStart, bytes.length - (int)lastStart);
		}
	}

	/** 打开实例的活跃段中部（第 index 条记录之后）插入该记录的字节副本（孤儿在真实记录之前）。 */
	private static void insertDuplicateOfRecord(Path segmentFile, int index) throws Exception {
		var bytes = Files.readAllBytes(segmentFile);
		var starts = recordStarts(bytes);
		Assertions.assertTrue(index < starts.length, "段内必须已有该记录");
		var from = (int)starts[index];
		var to = index + 1 < starts.length ? (int)starts[index + 1] : bytes.length;
		try (var raf = new RandomAccessFile(segmentFile.toFile(), "rw")) {
			raf.seek(0);
			raf.write(bytes, 0, to);                 // 前段（含该记录本身）
			raf.write(bytes, from, to - from);       // 副本紧随其后（真正的中部插入）
			raf.write(bytes, to, bytes.length - to); // 后段
		}
	}

	/** 尾随重复（孤儿在真实记录之后）：meta.next=3、装载 [0,4)——读到重复 id=2 时期望 id=3，
	 * 必须抛错；修复前静默把孤儿体当 id=3 投递（队列=[0,1,2,2]，id3↔体2 错位）。 */
	@Test
	public void testTrailingDuplicateRecordRejected(@TempDir Path tempDir) throws Exception {
		var home = tempDir.resolve("db").toString();
		try (var database = new RocksDatabase(home)) {
			var file = new MQFileWithIndex(home, database, "topic", 0);
			try {
				for (long id = 0; id < 3; ++id)
					file.appendMessage(MqTestSupport.messageOf(id));
				appendDuplicateOfLastRecord(file.getLastFile().toPath());

				Queue<BMessage.Data> queue = new ConcurrentLinkedQueue<>();
				var e = Assertions.assertThrows(RuntimeException.class, () -> file.fillMessage(queue, 0, 4),
						"记录 id 与期望序列不符必须响亮抛错（修复前静默错投）");
				Assertions.assertTrue(deepestMessage(e).contains("read message id mismatch"),
						"message=" + deepestMessage(e));
			} finally {
				file.close();
			}
		}
	}

	/** 中部重复（孤儿在真实记录之前，即原缺陷形态）：第 1 条记录后插入其副本，
	 * 装载 [0,3)——读到重复 id=1 时期望 id=2，必须抛错；修复前队列=[0,1,1]（id2↔体1 错位）。 */
	@Test
	public void testMidFileDuplicateRecordRejected(@TempDir Path tempDir) throws Exception {
		var home = tempDir.resolve("db").toString();
		try (var database = new RocksDatabase(home)) {
			var file = new MQFileWithIndex(home, database, "topic", 0);
			try {
				for (long id = 0; id < 3; ++id)
					file.appendMessage(MqTestSupport.messageOf(id));
				insertDuplicateOfRecord(file.getLastFile().toPath(), 1);

				Queue<BMessage.Data> queue = new ConcurrentLinkedQueue<>();
				var e = Assertions.assertThrows(RuntimeException.class, () -> file.fillMessage(queue, 0, 3),
						"记录 id 与期望序列不符必须响亮抛错（修复前静默错投）");
				Assertions.assertTrue(deepestMessage(e).contains("read message id mismatch"),
						"message=" + deepestMessage(e));
			} finally {
				file.close();
			}
		}
	}
}
