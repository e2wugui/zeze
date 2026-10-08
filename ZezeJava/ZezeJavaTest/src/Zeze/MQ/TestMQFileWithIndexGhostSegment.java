package Zeze.MQ;

import harness.Extra;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import Zeze.Builtin.MQ.BMessage;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.RocksDatabase;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND19 GB-C03 回归：段文件名过滤规则统一为"恰好两段"。
 * <p>
 * MQManager.loadMQ 按恰两段（"分区号.消息号"）发现分区，MQFileWithIndex 构造却接受 ≥2 段：
 * topic 目录混入杂散文件 "0.500.tmp"（编辑器/同步工具临时文件、.bak、拷贝残留）时会被注册成
 * 幽灵段 500——indexes.lastEntry() 被抬高后 lastFile 指向并不存在的 "0.500"，recoverTornTail
 * 的 nextMessageId(2) &lt; segBase(500) 检查 fatal 且报错指向"meta 丢失"这一错误方向，
 * Manager 启动失败需人工排障。
 * <p>
 * 修复：MQFileWithIndex 与 loadMQ 同规（partIndex.length != 2 跳过），杂散文件不再参与段注册。
 *（布局约定见 MqTestSupport。）
 * 原"类级 @ResourceLock(mq-file-statics)"（appendMessage/rebuildSegmentIndex 读公共静态
 * makeIndexPeriod，FND26 并行红）已随静态实例化移除：每实例读自己的周期，跨类漂移根除。
 */
@Fast
@Extra
public class TestMQFileWithIndexGhostSegment {

	private static void assertFillInOrder(Queue<BMessage.Data> queue, long begin, long end) {
		Assertions.assertEquals(end - begin, queue.size(), "回填数量");
		var expect = begin;
		for (var message : queue)
			Assertions.assertEquals(expect++, message.getTimestamp(), "回填必须按 id 有序");
	}

	@Test
	public void testStrayMultiDotFileIgnored(@TempDir Path tempDir) throws Exception {
		var home = tempDir.resolve("db").toString();
		var database = new RocksDatabase(home);
		var file = new MQFileWithIndex(home, database, "topic", 0);
		var dataFile = file.getLastFile();
		try {
			for (long id = 0; id < 2; ++id)
				file.appendMessage(MqTestSupport.messageOf(id));
			file.close();
		} finally {
			database.close();
		}

		// 杂散文件混入 topic 目录：三段名 "0.500.tmp"——旧代码接受 ≥2 段，解析 [1]="500"
		// 注册成幽灵段（getOrAddTable("topic.0.500")），把 lastEntry 抬高到 nextMessageId 之上。
		Files.write(Path.of(dataFile.getParent().toString(), "0.500.tmp"), new byte[]{1, 2, 3});

		try (var database2 = new RocksDatabase(home)) {
			// 旧代码：构造即抛 IllegalStateException("nextMessageId < segment base")，Manager 无法启动
			// 且报错指向"meta 丢失"这一错误根因方向。
			var file2 = new MQFileWithIndex(home, database2, "topic", 0);
			try {
				Assertions.assertEquals(2, file2.getNextMessageId(), "杂散文件不得影响位点");
				Assertions.assertEquals(0, file2.getFirstMessageId());
				Assertions.assertEquals("0.0", file2.getLastFile().getName(),
						"活跃段仍是真实段，不得指向按 key 重建的幽灵文件名");
				// 打开后的追加与回填照常（回填经索引定位不受幽灵段影响）。
				file2.appendMessage(MqTestSupport.messageOf(2));
				Queue<BMessage.Data> queue = new ConcurrentLinkedQueue<>();
				file2.fillMessage(queue, 0, 3);
				assertFillInOrder(queue, 0, 3);
			} finally {
				file2.close();
			}
		}
	}

	/** 两段但非数字段名的杂散文件（如 "0.abc"）本来就因 NumberFormatException 被忽略——固化该行为。 */
	@Test
	public void testTwoSegmentNonNumericStillIgnored(@TempDir Path tempDir) throws Exception {
		var home = tempDir.resolve("db").toString();
		var database = new RocksDatabase(home);
		var file = new MQFileWithIndex(home, database, "topic", 0);
		var dataFile = file.getLastFile();
		try {
			file.appendMessage(MqTestSupport.messageOf(0));
			file.close();
		} finally {
			database.close();
		}
		Files.write(Path.of(dataFile.getParent().toString(), "0.abc"), new byte[]{1});

		try (var database2 = new RocksDatabase(home)) {
			var file2 = new MQFileWithIndex(home, database2, "topic", 0);
			try {
				Assertions.assertEquals(1, file2.getNextMessageId());
			} finally {
				file2.close();
			}
		}
	}

	/**
	 * FND26 mq-02 形态 I 回归：「索引列族 dropTable 成功 + 段文件 file.delete 失败」的幽灵段
	 * （列族不在 rocksdb 而文件在、meta 位点完好）重启装载时按文件顺序扫描重建段索引——
	 * 重建必须覆盖段内全部对齐点位（id%makeIndexPeriod==0）。扫描循环若不按 pos 重定位到
	 * 下一条记录头（readFully 只前进头长，记录体未跳过），第二条起即把体字节当头错位止步：
	 * 只入段基一条索引、每次回填退化为从段首线性扫，且对完好文件误报 corrupted record warn。
	 */
	@Test
	public void testDroppedIndexColumnFamilyRebuiltWithAllAlignedPoints(@TempDir Path tempDir) throws Exception {
		var home = tempDir.resolve("db").toString();
		var database = new RocksDatabase(home);
		var file = new MQFileWithIndex(home, database, "topic", 0);
		try {
			for (long id = 0; id < 250; ++id)
				file.appendMessage(MqTestSupport.messageOf(id));
			file.close();
		} finally {
			database.close();
		}

		// 幽灵段构造：只 drop 索引列族（dropTable 成功），保留段文件与 meta 位点（file.delete 失败）。
		try (var database2 = new RocksDatabase(home)) {
			database2.dropTable("topic.0.0");
			var file2 = new MQFileWithIndex(home, database2, "topic", 0);
			try {
				Assertions.assertEquals(250, file2.getNextMessageId(), "meta 位点完好，形态 I 重建不失位");
				var indexTable = database2.getTable("topic.0.0");
				Assertions.assertNotNull(indexTable, "幽灵段装载重建索引列族");
				try (var it = indexTable.iterator()) {
					it.seekToFirst();
					var expectId = 0L;
					while (it.isValid()) {
						Assertions.assertEquals(expectId, ByteBuffer.ToLongBE(it.key(), 0),
								"对齐点位必须按序恢复");
						expectId += 100;
						it.next();
					}
					Assertions.assertEquals(300, expectId, "对齐点位 0,100,200 共 3 条必须全部重建"
							+ "（只重建段基一条=扫描未跳过记录体的错位形态）");
				}
				Queue<BMessage.Data> queue = new ConcurrentLinkedQueue<>();
				file2.fillMessage(queue, 150, 200);
				assertFillInOrder(queue, 150, 200);
			} finally {
				file2.close();
			}
		}
	}
}
