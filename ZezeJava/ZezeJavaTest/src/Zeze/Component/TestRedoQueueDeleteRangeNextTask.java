package Zeze.Component;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import Zeze.Builtin.RedoQueue.BTaskId;
import Zeze.Component.RedoQueue;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.RocksDatabase;
import harness.Fast;
import org.junit.jupiter.api.Test;

/**
 * kimi-audit01 C-12 回归：deleteDoneTasks把"未截断的填充数组"传给deleteRange，
 * 误删下一个待发送任务。
 * 任务key是WriteLong变长编码（首字节带长度前缀，1字节的id只有1字节），add()落盘按
 * WriteIndex截断；而deleteDoneTasks传的是Allocate(8)的整个后备数组：enc(N+1)恰是
 * end.Bytes的真前缀，字节序下排在前⇒任务N+1落入[enc(0)填充, enc(N+1)填充)被一并删除。
 * 触发面：重启时队列有积压，start()恢复水位后立即deleteDoneTasks()——积压的第一个
 * 待发任务被抹掉，泵读key(N+1)命中hole fatal停摆。
 * 测试：构造"水位已持久化但任务行未删（崩溃窗口残留）+积压"的落盘状态，重启触发
 * deleteDoneTasks，断言水位之下的行被清、水位的下一跳任务仍在（修复前：下一跳被误删）。
 */
@Fast
public class TestRedoQueueDeleteRangeNextTask {

	/** 单字节key（taskId&lt;0x40）：1字节的enc(2)是end.Bytes=[0x02,0x00…]的真前缀。 */
	@Test
	public void testRestartBacklogKeepsNextPendingTaskSingleByteKey() throws Exception {
		var queueName = "redo_delrange_sb_" + System.nanoTime(); // 唯一rocksdb目录
		var config = new Config();
		config.setServiceManager("disable");
		var queue = new RedoQueue(queueName, config);
		try {
			queue.start();
			queue.add(1, new BTaskId()); // taskId=1落盘（无socket，泵发送前返回）
			queue.add(1, new BTaskId()); // taskId=2落盘

			// 崩溃窗口残留：任务1已完成（水位=1已持久化）但deleteDoneTasks未执行，行1、行2俱在
			writeWatermark(queue, 1L);

			queue.stop();
			queue.start(); // 重启：恢复水位1→deleteDoneTasks删[enc(0),enc(2))

			// 核心断言（红）：下一跳任务2不得被连带删除——修复前enc(2)是end.Bytes真前缀被删
			assertFalse(hasTaskRow(queue, 1L), "已完成任务1的行应被水位清理");
			assertTrue(hasTaskRow(queue, 2L), "下一跳待发任务2不得被deleteRange误删");
		} finally {
			queue.stop();
			deleteRecursively(Path.of(queueName));
		}
	}

	/** 双字节key（0x40≤taskId&lt;0x2000）：多字节变长编码同构（enc=[0x40,0x65]两字节）。 */
	@Test
	public void testRestartBacklogKeepsNextPendingTaskTwoByteKey() throws Exception {
		var queueName = "redo_delrange_tb_" + System.nanoTime();
		var config = new Config();
		config.setServiceManager("disable");
		var queue = new RedoQueue(queueName, config);
		try {
			queue.start();
			// 填到101：add的taskId分配从1连续推进，101落在双字节编码区间
			for (int i = 0; i < 101; i++)
				queue.add(1, new BTaskId());
			writeWatermark(queue, 100L); // 崩溃窗口残留：行1..101俱在，水位=100

			queue.stop();
			queue.start(); // 重启触发deleteDoneTasks删[enc(0),enc(101))

			assertFalse(hasTaskRow(queue, 100L), "已完成任务100的行应被水位清理");
			assertTrue(hasTaskRow(queue, 101L), "下一跳待发任务101不得被deleteRange误删");
		} finally {
			queue.stop();
			deleteRecursively(Path.of(queueName));
		}
	}

	/** 写水位表：模拟processRunTaskResult成功路径的tableLastDoneTaskId.put。 */
	private static void writeWatermark(RedoQueue queue, long taskId) throws Exception {
		var tableField = RedoQueue.class.getDeclaredField("tableLastDoneTaskId");
		tableField.setAccessible(true);
		var table = (RocksDatabase.Table)tableField.get(queue);
		var keyField = RedoQueue.class.getDeclaredField("lastDoneTaskIdKey");
		keyField.setAccessible(true);
		var key = (byte[])keyField.get(queue);
		var value = ByteBuffer.Allocate(9);
		value.WriteLong(taskId);
		table.put(key, 0, key.length, value.Bytes, 0, value.WriteIndex);
	}

	/** 按生产路径的截断key读行，判定任务行是否存在。 */
	private static boolean hasTaskRow(RedoQueue queue, long taskId) throws Exception {
		var tableField = RedoQueue.class.getDeclaredField("tableTaskQueue");
		tableField.setAccessible(true);
		var table = (RocksDatabase.Table)tableField.get(queue);
		var key = ByteBuffer.Allocate(9);
		key.WriteLong(taskId);
		return null != table.get(key.Bytes, 0, key.WriteIndex);
	}

	private static void deleteRecursively(Path root) throws Exception {
		if (!Files.exists(root))
			return;
		try (var walk = Files.walk(root)) {
			walk.sorted(Comparator.reverseOrder()).forEach(p -> {
				try {
					Files.delete(p);
				} catch (Exception ignored) {
				}
			});
		}
	}
}
