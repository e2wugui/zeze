package UnitTest.Zeze.Component;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
 * CP1-F1 回归（P0）：排空队列重启后水位回绕。
 * 队列排空时tableTaskQueue已无条目，重启后lastTaskId从空表恢复为0，而lastDoneTaskId=N
 * （水位表独立持久化）——泵条件lastDoneTaskId&lt;lastTaskId永假，新增任务被静默丢弃；
 * 且新任务从id=1重新分配，落入水位之下的已删区间（下一次水位推进会连带误删）。
 * 修复：start()以水位为下界钳制lastTaskId。
 * 测试：构造"任务1已完成、队列已排空"的落盘状态（水位=1且任务行已删），重启后
 * 断言lastTaskId钳制到1，新增任务分配id=2（修复前：lastTaskId=0、新任务id=1）。
 */
@Fast
public class TestRedoQueueRestartWatermark {

	@Test
	public void testDrainedQueueRestartClampsLastTaskIdToWatermark() throws Exception {
		var queueName = "redo_restart_wm_" + System.nanoTime(); // 唯一rocksdb目录
		var config = new Config();
		config.setServiceManager("disable");
		var queue = new RedoQueue(queueName, config);
		try {
			queue.start();
			queue.add(1, new BTaskId()); // taskId=1落盘（无socket，泵发送前返回，任务留存）

			// 构造排空态：任务1已完成（水位推进到1）且deleteDoneTasks已清任务行。
			writeWatermark(queue, 1L);
			deleteTaskKey(queue, 1L);

			queue.stop();
			queue.start(); // 重启恢复（排空队列场景）

			// 核心（红断言）：lastTaskId必须钳制到水位——修复前从空表恢复为0，水位"回绕"
			assertEquals(1L, fieldOf(queue, "lastTaskId"), "重启后lastTaskId不得回绕到水位之下");
			assertEquals(1L, fieldOf(queue, "lastDoneTaskId"));

			// 新增任务必须从id=2继续：修复前id=1落回水位之下的已删区间（会被下一次
			// deleteDoneTasks连带误删），且泵条件(1<0)永假、任务永不发送
			queue.add(1, new BTaskId());
			assertEquals(2L, maxTaskKey(queue), "排空重启后新增任务必须分配到水位之上的新id");
			assertEquals(2L, fieldOf(queue, "lastTaskId"));
		} finally {
			queue.stop();
			deleteRecursively(Path.of(queueName));
		}
	}

	private static long fieldOf(RedoQueue queue, String name) throws Exception {
		var field = RedoQueue.class.getDeclaredField(name);
		field.setAccessible(true);
		return field.getLong(queue);
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

	private static void deleteTaskKey(RedoQueue queue, long taskId) throws Exception {
		var tableField = RedoQueue.class.getDeclaredField("tableTaskQueue");
		tableField.setAccessible(true);
		var table = (RocksDatabase.Table)tableField.get(queue);
		var key = ByteBuffer.Allocate(9);
		key.WriteLong(taskId);
		table.delete(key.Bytes, 0, key.WriteIndex);
	}

	private static long maxTaskKey(RedoQueue queue) throws Exception {
		var tableField = RedoQueue.class.getDeclaredField("tableTaskQueue");
		tableField.setAccessible(true);
		var table = (RocksDatabase.Table)tableField.get(queue);
		try (var it = table.iterator()) {
			it.seekToLast();
			if (!it.isValid())
				return 0;
			return ByteBuffer.Wrap(it.key()).ReadLong();
		}
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
