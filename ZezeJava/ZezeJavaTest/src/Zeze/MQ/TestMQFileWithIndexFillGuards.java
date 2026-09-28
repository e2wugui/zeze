package Zeze.MQ;

import java.nio.file.Path;
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
 * FND2-G2-3 / FND2-G2-4 回归：MQFileWithIndex.fillMessage 的两处静默失效。
 * <p>
 * G2-3：索引定位失败（索引 column family 损坏/误删后 getOrAddTable 重建出空表）时
 * {@code if (floorIt.isValid())} 整体被跳过，外层 while 的 headMessageId 永不推进——
 * 回填任务在后台线程里不持锁、无 IO、无 sleep 地单核自旋，无异常无日志（只能靠 jstack）。
 * 修复：响亮抛错，落入 MQSingle.pullMessage 的失败-复位-事件重试路径。
 * <p>
 * G2-4：filePosition 用 int 累加，索引 offset 越过 2GB（ProxyServer 放行 100MB 协议，段
 * 文件可达 2.2GB+）时复合赋值截断回绕为负，{@code filePosition > fileSize} 的 eof 边界
 * 判断恒不成立——头/体读全错位（异常或伪消息）。修复：改 long。
 * <p>
 * 注：文件放 src/MQ/ 但声明 package Zeze.MQ，与 TestMQSingle* 先例一致。
 */
@Fast
public class TestMQFileWithIndexFillGuards {

	/** 旧代码下 G2-3 场景是无限循环：放进守护线程限时执行，超时即失败而不是吊死整个测试。 */
	private static Throwable timedCall(long timeoutMs, Runnable call) throws InterruptedException {
		var ref = new Throwable[1];
		var t = new Thread(() -> {
			try {
				call.run();
			} catch (Throwable e) {
				ref[0] = e;
			}
		});
		t.setDaemon(true);
		t.start();
		t.join(timeoutMs);
		Assertions.assertFalse(t.isAlive(), "fillMessage 超时：疑似无进展自旋（索引定位失败未抛错）");
		return ref[0];
	}

	/** fillMessage 外层 catch(Exception) 会包一层 RuntimeException，取最深层消息做断言。 */
	private static String deepestMessage(Throwable e) {
		var msg = String.valueOf(e.getMessage());
		for (var cause = e.getCause(); cause != null; cause = cause.getCause())
			if (cause.getMessage() != null)
				msg = cause.getMessage();
		return msg;
	}

	/**
	 * G2-3：索引表为空（seekForPrev 定位失败）时必须抛错，不得静默自旋。
	 * 场景等价于 finding T2：数据文件完好、meta.next=N>0，但段索引所在 column family 内容
	 * 丢失后 getOrAddTable 重建出空表——floorEntry 非 null，迭代器上 seekForPrev 无效。
	 */
	@Test
	public void testEmptyIndexThrowsInsteadOfSpin(@TempDir Path tempDir) throws Exception {
		var home = tempDir.resolve("db").toString();
		var database = new RocksDatabase(home);
		var file = new MQFileWithIndex(home, database, "topic", 0);
		try {
			// 不写任何消息：索引表 topic.0.0 为空而 fill 范围 [0,1) 非空，即"索引内容丢失"状态。
			Queue<BMessage.Data> queue = new ConcurrentLinkedQueue<>();
			var e = timedCall(10_000, () -> file.fillMessage(queue, 0, 1));
			Assertions.assertNotNull(e, "索引定位失败必须抛错（旧代码静默自旋单核打满）");
			Assertions.assertTrue(deepestMessage(e).contains("message index not found"),
					"message=" + deepestMessage(e));
			Assertions.assertTrue(queue.isEmpty(), "定位失败时不得装入任何消息");
		} finally {
			database.close();
			file.close();
		}
	}

	/**
	 * G2-4：索引 offset 越过 Integer.MAX_VALUE 时 filePosition 不得回绕为负。
	 * 修复后：long 累加 2^31+100+12 > fileSize=0，在读头之前即被 "locate message eof." 拦截；
	 * 修复前（int）：回绕为负使边界判断恒 false，seek 到 EOF 读出全零伪头——messageId=0 恰好
	 * 等于 headMessageId，"假成功"装入全零伪消息（或错位扫描近乎无限自旋）。
	 */
	@Test
	public void testHugeIndexOffsetNotTruncated(@TempDir Path tempDir) throws Exception {
		var home = tempDir.resolve("db").toString();
		var database = new RocksDatabase(home);
		var file = new MQFileWithIndex(home, database, "topic", 0);
		try {
			// 伪造索引项：messageId=0 → offset=2^31+100（段文件越过 2GB 时的正常量级；
			// 文件本身保持空——修复后必须在读头之前就被 eof 边界拦下，无需真实大文件）。
			var indexTable = database.getOrAddTable("topic.0.0");
			var key = new byte[8];
			ByteBuffer.longBeHandler.set(key, 0, 0L);
			var value = new byte[8];
			ByteBuffer.longBeHandler.set(value, 0, (long)Integer.MAX_VALUE + 100);
			indexTable.put(key, value);

			Queue<BMessage.Data> queue = new ConcurrentLinkedQueue<>();
			var e = timedCall(10_000, () -> file.fillMessage(queue, 0, 1));
			Assertions.assertNotNull(e, "大 offset 必须被 eof 边界拦截（旧代码 int 回绕后边界恒不触发）");
			Assertions.assertTrue(deepestMessage(e).contains("locate message eof"),
					"message=" + deepestMessage(e));
			Assertions.assertTrue(queue.isEmpty(), "不得装入伪消息");
		} finally {
			database.close();
			file.close();
		}
	}
}
