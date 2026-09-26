package Zeze.MQ;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import Zeze.Builtin.MQ.BMessage;
import Zeze.Util.RocksDatabase;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
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
 * <p>
 * 注：文件放 src/MQ/ 但声明 package Zeze.MQ，与 TestMQFileWithIndexTornTail 先例一致。
 */
@Fast
public class TestFnd19MQFileWithIndexGhostSegment {

	private static BMessage.Data messageOf(long id) {
		var message = new BMessage.Data();
		message.setTimestamp(id);
		return message;
	}

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
				file.appendMessage(messageOf(id));
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
				file2.appendMessage(messageOf(2));
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
			file.appendMessage(messageOf(0));
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
}
