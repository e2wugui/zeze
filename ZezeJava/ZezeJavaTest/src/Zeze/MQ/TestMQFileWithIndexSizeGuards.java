package Zeze.MQ;

import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.Builtin.MQ.BMessage;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.RocksDatabase;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND19 GB-C04 回归：messageSize 负长度/中段损坏的防御对称性。
 * <p>
 * A（fillMessage 定位环）：数据文件头部运行期损坏出负 size 时，RandomAccessFile.skipBytes(负数)
 * 按规范返回 0，越界检查（0 &lt; 负数恒假）被绕过，filePosition 镜像每轮净减使 eof 检查永不
 * 成立——worker 线程无 IO、无异常、无日志的单核自旋（与 FND2-G2-3 的 messageIndexNotFound
 * 同型失效，但那条修复未覆盖 size 负值路径）。修复：定位环读出头后遇负 size 响亮抛出。
 * <p>
 * B（recoverTornTail）：size 检查的 break 先于 id 检查的 fatal 执行——中段记录 size 损坏
 * （其后仍有已提交记录）走"撕裂尾"分支静默截断，已提交的后续消息被丢弃（仅一条 warn），
 * 与函数自声明的"中间损坏 fatal，防自动截断静默丢中间消息"策略矛盾。修复：size 损坏发生在
 * 提交区中间（expectId &lt; nextMessageId-1）时按 id 错位同款 fatal；仅最后一条已提交记录的
 * 撕裂尾保留回拨语义（testTornCommittedRecordRollsBack 已固化，本类不重复）。
 *（布局约定见 MqTestSupport。）
 */
@Fast
public class TestMQFileWithIndexSizeGuards {

	/** 顺序扫描段文件前 count 条记录并返回其结尾偏移（记录头布局与实现一致：Long8(id)+Int4(size)）。 */
	private static long offsetAfter(java.io.File file, int count) throws Exception {
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

	/**
	 * 把索引表裁剪到只含 id=0：默认 makeIndexPeriod=100 下少量消息本来就只有 id0 被索引，
	 * 这样 locate 环确定性地从段首扫描；且与 makeIndexPeriod 静态字段解耦（TestFileWithIndexed
	 * 并行运行时会改写该字段，见 TestMQFileWithIndexTornTail 的同款处理说明）。
	 */
	private static void scrubIndexToFirstEntryOnly(RocksDatabase database, String indexTable) throws Exception {
		var table = database.getOrAddTable(indexTable);
		var keysToDelete = new ArrayList<byte[]>();
		try (var it = table.iterator()) {
			it.seekToFirst();
			while (it.isValid()) {
				keysToDelete.add(it.key());
				it.next();
			}
		}
		for (var key : keysToDelete) {
			if (0 != ByteBuffer.ToLongBE(key, 0))
				table.delete(key);
		}
	}

	/** 取最深层异常消息（构造路径经 Task.forceThrow 包装，统一取最深）。 */
	private static String deepestMessage(Throwable e) {
		var msg = String.valueOf(e.getMessage());
		for (var cause = e.getCause(); cause != null; cause = cause.getCause())
			if (cause.getMessage() != null)
				msg = cause.getMessage();
		return msg;
	}

	/**
	 * A：定位环遇负 size 必须响亮失败。旧代码无 IO 无异常无日志地单核自旋——在独立线程执行
	 * 并限时等待完成：修复后线程以 RuntimeException 终结；旧代码线程永不结束，等待超时判失败
	 * （不挂死整个测试进程）。
	 * <p>
	 * 损坏注入在构造之后（运行期 bit rot / 外部工具触碰形态）：recoverTornTail 仅构造时执行
	 * 一次（其自身的 size 防御由 testMidSizeCorruptionFatal 覆盖），定位环是此形态的最后防线。
	 */
	@Test
	public void testLocateLoopNegativeSizeThrows(@TempDir Path tempDir) throws Exception {
		var home = tempDir.resolve("db").toString();
		var database = new RocksDatabase(home);
		var file = new MQFileWithIndex(home, database, "topic", 0);
		var dataFile = file.getLastFile();
		try {
			for (long id = 0; id < 4; ++id)
				file.appendMessage(MqTestSupport.messageOf(id));
			scrubIndexToFirstEntryOnly(database, "topic.0.0");
			file.close();
		} finally {
			database.close();
		}

		var record1Offset = offsetAfter(dataFile, 1);
		try (var database2 = new RocksDatabase(home)) {
			var file2 = new MQFileWithIndex(home, database2, "topic", 0); // 完好打开，恢复校验通过
			try {
				// 运行期损坏：记录 1（定位路径的中间记录）的 size 字段改写为 -1
				// （字节序无关：全 0xFF）。
				try (var raf = new RandomAccessFile(dataFile, "rw")) {
					raf.seek(record1Offset + 8); // Long8(id) 之后
					raf.write(new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF});
				}

				final var failure = new AtomicReference<Throwable>();
				var worker = new Thread(() -> {
					try {
						Queue<BMessage.Data> queue = new ConcurrentLinkedQueue<>();
						// headMessageId=2 > 损坏记录 id=1：定位环必须跳过记录 0、1 才能到达 2。
						file2.fillMessage(queue, 2, 4);
					} catch (Throwable e) {
						failure.set(e);
					}
				}, "testLocateLoopNegativeSizeThrows");
				worker.setDaemon(true);
				worker.start();
				worker.join(10_000);
				Assertions.assertFalse(worker.isAlive(),
						"定位环遇负 size 不得自旋（旧代码：skipBytes(负数)返回0绕过越界检查，无进展单核自旋）");
				Assertions.assertNotNull(failure.get(), "负 size 必须响亮抛出");
				var msg = deepestMessage(failure.get());
				Assertions.assertTrue(msg.contains("negative"), "异常须定位负长度损坏：message=" + msg);
			} finally {
				file2.close();
			}
		}
	}

	/**
	 * B：中段记录 size 损坏（其后还有已提交记录）必须 fatal，不得按撕裂尾静默截断丢弃
	 * 已提交消息。旧代码 break 走回拨分支：nextMessageId 3→1，记录 1、2 被静默丢弃（仅 warn）。
	 */
	@Test
	public void testMidSizeCorruptionFatal(@TempDir Path tempDir) throws Exception {
		var home = tempDir.resolve("db").toString();
		var database = new RocksDatabase(home);
		var file = new MQFileWithIndex(home, database, "topic", 0);
		var dataFile = file.getLastFile();
		try {
			for (long id = 0; id < 3; ++id)
				file.appendMessage(MqTestSupport.messageOf(id));
			scrubIndexToFirstEntryOnly(database, "topic.0.0");
			file.close();
		} finally {
			database.close();
		}

		// 磁盘腐蚀模拟：记录 1（中间）的 size 改写为大正值（越过文件尾，1_000_000_000 小端），
		// id 字段不动——与 testMiddleCorruptionFatal 的 id 错位形态相对：同等损伤的 size 表现形式。
		var record1Offset = offsetAfter(dataFile, 1);
		try (var raf = new RandomAccessFile(dataFile, "rw")) {
			raf.seek(record1Offset + 8);
			raf.write(new byte[]{0x00, (byte)0xCA, (byte)0x9A, 0x3B}); // 1_000_000_000 little-endian
		}

		try (var database2 = new RocksDatabase(home)) {
			var ex = Assertions.assertThrows(RuntimeException.class,
					() -> new MQFileWithIndex(home, database2, "topic", 0),
					"中段 size 损坏必须 fatal（旧代码静默截断丢弃已提交的记录 1、2）");
			var msg = deepestMessage(ex);
			Assertions.assertTrue(msg.contains("corrupted in middle"), "message=" + msg);
			Assertions.assertTrue(msg.contains("bad record size"), "须标明 size 损坏形态：message=" + msg);
			Assertions.assertTrue(msg.contains("position=" + record1Offset)
					&& msg.contains("expectMessageId=1"), "fatal 须带定位信息：message=" + msg);
		}
	}
}
