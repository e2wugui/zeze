package Zeze.MQ;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.MQ.BMessage;
import Zeze.Builtin.MQ.BSendMessage;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;

/**
 * FND21 GB-C05 回归：MQManager.replayDeadLetter 的 rocksdb 触点（dlq.get 前置读、dlq.delete
 * 后置删）无 stopped 闸——javadoc 承诺的"sendMessage 的停机拒绝语义透传"实际不覆盖首尾两步，
 * 与 stop 的 rocksDatabase.close 相交是对已关库/已毁句柄的 native 调用（窗口A/B）。
 * <p>
 * 修复形态（案卷处置建议）：入口 {@code if (stopped) throw IllegalStateException}（兑现 javadoc
 * 语义，对齐仓内"全触库入口有闸"口径）+ sendMessage 之后、dlq.delete 之前复查一次 stopped
 *（窗口B：跳过 delete，死信键保留可再重放——重放重复=at-least-once 既定语义，优于 native 面）。
 * <p>
 * 判别：
 * ① 好路径（双绿守卫）：重放=追加+消费死信键，修复不得破坏；
 * ② 入口闸（窗口A的时序终点=停机全量完成后调用）：修复代码 IllegalStateException（未触库），
 * 旧代码 getTable 返回 null 后抛 IllegalArgumentException（判红：异常类型错位——
 * "死信不存在"掩盖"Manager 已停止"的真语义）；
 * ③ 窗口B（重放消息已追加、delete 前停机置位——替身分区在 sendMessage 返回后置 stopped，
 * 只置标志不关库：聚焦闸语义，避免旧车道踩真 native 面）：修复代码拒绝且键保留（判绿），
 * 旧代码静默消费键正常返回（判红）。
 * <p>
 * 注：文件放 src/MQ/ 但声明 package Zeze.MQ（dlqKey 包内缝），与 TestFnd20GB* 先例一致。
 */
@ResourceLock("mq-file-statics") // 旁观者READ：与改写trunkFileSize/makeIndexPeriod的类互斥——静态被并行改小期间本类append会滚出无索引段，fillMessage seekForPrev落空即messageIndexNotFound假红（2026-10-04 test40批r5实证）；旁观者彼此READ可并行
@Fast
public class TestGBC05ReplayDeadLetterStopGate {

	/** 死信值编码（与 MQSingle.tryDeadLetter 同构：BMessage 编码 + 8 字节 BE 时间戳尾缀）。 */
	private static byte[] dlqValue(BMessage.Data message) {
		var bb = ByteBuffer.Allocate();
		message.encode(bb);
		var stamped = java.util.Arrays.copyOfRange(bb.Bytes, bb.ReadIndex, bb.ReadIndex + bb.size() + 8);
		ByteBuffer.longBeHandler.set(stamped, stamped.length - 8, System.currentTimeMillis());
		return stamped;
	}

	private static void putDeadLetter(MQManager manager, String topic, int partitionIndex, long messageId)
			throws Exception {
		var message = Fnd19MqTestSupport.messageOf(messageId);
		var key = MQManager.dlqKey(topic, partitionIndex, messageId);
		var value = dlqValue(message);
		manager.getDlqTable().put(key, 0, key.length, value, 0, value.length);
	}

	private static void setStopped(MQManager manager, boolean value) throws Exception {
		var f = MQManager.class.getDeclaredField("stopped");
		f.setAccessible(true);
		f.setBoolean(manager, value);
	}

	@SuppressWarnings("unchecked")
	private static ConcurrentHashMap<Integer, MQSingle> partitionsOf(MQPartition queue) throws Exception {
		var f = MQPartition.class.getDeclaredField("partitions");
		f.setAccessible(true);
		return (ConcurrentHashMap<Integer, MQSingle>)f.get(queue);
	}

	/**
	 * ① 好路径（双绿守卫）：死信存在 + 分区存活 → 重放把消息追加到分区尾（新 messageId）、
	 * 消费死信键。修复只加停机闸，不得改变好路径行为。
	 */
	@Test
	public void testReplayHappyPathAppendsAndConsumesKey(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var manager = new MQManager(tempDir.resolve("manager").toString(), new Config());
		try {
			manager.createPartition("t", new HashSet<>(java.util.List.of(0)));
			var single = manager.getQueueForTest("t").get(0);
			single.sendMessage(Fnd19MqTestSupport.sendMessageOf(0)); // 原分区已有 id0
			putDeadLetter(manager, "t", 0, 7);

			manager.replayDeadLetter("t", 0, 7);

			Assertions.assertNull(manager.getDlqTable().get(MQManager.dlqKey("t", 0, 7)),
					"重放成功即消费死信键");
			Assertions.assertEquals(2, single.getFileForTest().getNextMessageId(),
					"死信消息追加到分区尾（新 messageId=1）");
			Assertions.assertEquals(0, single.getFileForTest().getFirstMessageId(), "位点不动（追加面）");
		} finally {
			manager.stop();
		}
	}

	/**
	 * ② 入口闸（窗口A 的时序终点）：停机全量完成（stopped 置位 + rocksDatabase.close 完成）
	 * 后调用重放——修复代码入口拒绝 IllegalStateException（javadoc 承诺的语义）；旧代码
	 * dlq.get 触达已清空的 tableMap，以 IllegalArgumentException("dead letter not found")
	 * 掩盖真语义（判红：异常类型错位）。
	 */
	@Test
	public void testStoppedManagerRejectsReplayBeforeAnyRocksdbTouch(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var manager = new MQManager(tempDir.resolve("manager").toString(), new Config());
		manager.createPartition("t", new HashSet<>(java.util.List.of(0)));
		manager.getQueueForTest("t").get(0).sendMessage(Fnd19MqTestSupport.sendMessageOf(0));
		putDeadLetter(manager, "t", 0, 7);
		manager.stop(); // 全量停机完成（含 rocksDatabase.close）
		var ex = Assertions.assertThrows(IllegalStateException.class,
				() -> manager.replayDeadLetter("t", 0, 7),
				"停机后的重放必须入口拒绝（FND21 GB-C05：首步 dlq.get 不得触达已关库）");
		Assertions.assertTrue(ex.getMessage().contains("stopped"), "拒绝语义可辨识");
	}

	/** 替身分区：sendMessage（重放的追加步）返回后置 stopped——模拟窗口B 的停机插入点。 */
	private static final class StopAfterSendSingle extends MQSingle {
		private final MQManager manager;

		StopAfterSendSingle(MQPartition partition, String topic, int partitionId,
				MQFileWithIndex fileWithIndex, MQManager manager) {
			super(partition, topic, partitionId, fileWithIndex);
			this.manager = manager;
		}

		@Override
		public void sendMessage(BSendMessage.Data message) {
			super.sendMessage(message); // 重放消息已真实追加到分区尾
			try {
				setStopped(manager, true); // stop 在 append 与 dlq.delete 之间完成置位（只置标志不关库）
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * ③ 窗口B：重放消息已追加、dlq.delete 之前停机置位——修复代码 delete 前复查拒绝
	 * （IllegalStateException、死信键保留可再重放）；旧代码静默消费键正常返回（判红：
	 * 无异常+键消失=对停机语义的违约）。
	 */
	@Test
	public void testStopDuringReplayKeepsDeadLetterKey(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var manager = new MQManager(tempDir.resolve("manager").toString(), new Config());
		try {
			manager.createPartition("t", new HashSet<>(java.util.List.of(0)));
			var queue = manager.getQueueForTest("t");
			var real = queue.get(0);
			real.sendMessage(Fnd19MqTestSupport.sendMessageOf(0)); // id0 已提交
			putDeadLetter(manager, "t", 0, 7);

			// 替身接管分区 0（独立 MQFileWithIndex：真分区的流关闭后替身仍可追加）。
			var stubFile = new MQFileWithIndex(manager.getHome(), manager.getRocksDatabase(), "t", 0);
			var stub = new StopAfterSendSingle(queue, "t", 0, stubFile, manager);
			real.close(); // 摘除真分区（其文件流关闭；替身持有独立流）
			partitionsOf(queue).put(0, stub);

			var ex = Assertions.assertThrows(IllegalStateException.class,
					() -> manager.replayDeadLetter("t", 0, 7),
					"重放追加完成后停机的重放必须在 delete 前拒绝（FND21 GB-C05 窗口B）");
			Assertions.assertTrue(ex.getMessage().contains("stopped"), "拒绝语义可辨识");
			Assertions.assertNotNull(manager.getDlqTable().get(MQManager.dlqKey("t", 0, 7)),
					"死信键保留（可再重放；重放重复=at-least-once 既定语义，优于对已关库的 native 调用）");
			Assertions.assertEquals(2, stub.getFileForTest().getNextMessageId(),
					"重放消息确实已追加到分区尾（拒绝的是 delete，不是 append）");
		} finally {
			// stopped 已被替身置位，仍须走完整 stop 关闭 rocksdb 与替身文件流（Windows 临时目录清理）
			manager.stop();
		}
	}
}
