package Zeze.MQ;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Builtin.MQ.BMessage;
import Zeze.Builtin.MQ.BSendMessage;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND-G2-2 回归：MQSingle.pullMessage 任意异常后 messageFillFuture 永久非空，后台回填永久停摆。
 * <p>
 * pullMessage 的异常源包括 MQFileWithIndex 防御性抛错（"locate message eof."、"message not found"、
 * "read message head eof."，对应数据文件截断/掉电丢尾）、BMessage.Data.decode 失败、IO 错误等。
 * 旧代码中 {@code messageFillFuture = null} 在 fillMessage 之后顺序执行，异常时永远走不到：
 * 此后所有 tryStartBackgroundFill 都看到 future != null 而跳过，highLoad 永不回落，该分区
 * 消息投递永久停止（直至进程重启），且无告警。
 * <p>
 * 修复：fill 段以 try/catch 保护——异常时复位 messageFillFuture、按盘上真实积压重算 highLoad
 * （calculateFill 已按快照扣减但装载未完成）、打 error 日志后重抛；重试保持事件驱动
 * （sendMessage/ack），不立即重启 fill。
 * <p>
 * 注：文件放在 src/MQ/ 但声明 package Zeze.MQ——需要访问 MQSingle 的包内测试缝（注入
 * MQFileWithIndex 的构造器与 pullMessage）；与 TestMQSingleDirectEnqueue 先例一致。
 */
@Fast
public class TestMQSingleFillStall {

	/** fillMessage 可注入失败的 MQFileWithIndex：failFill=true 时装载抛 RuntimeException。 */
	static class FlakyFile extends MQFileWithIndex {
		volatile boolean failFill;
		final AtomicInteger failedCount = new AtomicInteger();
		// MQSingle 的内存队列引用（pullMessage 把队列作为参数传入 fillMessage 时捕获）。
		Queue<BMessage.Data> queueRef;

		FlakyFile(String home, RocksDatabase database) throws Exception {
			super(home, database, "topic", 0);
		}

		@Override
		public void fillMessage(Queue<BMessage.Data> messageQueue, long headMessageId, long endMessageId) {
			queueRef = messageQueue;
			if (failFill) {
				failedCount.incrementAndGet();
				throw new RuntimeException("injected fill failure");
			}
			super.fillMessage(messageQueue, headMessageId, endMessageId);
		}
	}

	private static BSendMessage.Data sendMessageOf(long id) {
		var message = new BMessage.Data();
		message.setTimestamp(id);
		var send = new BSendMessage.Data();
		send.setMessage(message);
		return send;
	}

	private static Object getField(Object obj, String name) throws Exception {
		return fieldOf(obj, name).get(obj);
	}

	private static void setField(Object obj, String name, Object value) throws Exception {
		fieldOf(obj, name).set(obj, value);
	}

	private static Field fieldOf(Object obj, String name) throws Exception {
		var f = obj.getClass().getDeclaredField(name);
		f.setAccessible(true);
		return f;
	}

	private static List<String> queueIds(Queue<BMessage.Data> messageQueue) {
		var ids = new ArrayList<String>();
		if (null != messageQueue)
			for (var message : messageQueue)
				ids.add(String.valueOf(message.getTimestamp()));
		return ids;
	}

	private static void await(String what, java.util.function.BooleanSupplier cond) throws InterruptedException {
		var deadline = System.currentTimeMillis() + 30_000;
		while (!cond.getAsBoolean()) {
			if (System.currentTimeMillis() > deadline)
				throw new AssertionError("timeout waiting: " + what);
			Thread.sleep(10);
		}
	}

	/**
	 * 机制：fill 异常后 messageFillFuture 必须复位为 null（旧代码保持非空，回填永久停摆）。
	 */
	@Test
	public void testFillExceptionResetsFillFuture(@TempDir Path tempDir) throws Exception {
		var home = tempDir.resolve("db").toString();
		var database = new RocksDatabase(home);
		var file = new FlakyFile(home, database);
		try {
			// 空分区构造成功（无积压，构造时 fillMessage(0,0) 不注入失败）。
			var single = new MQSingle(new MQPartition(null), "topic", 0, file);

			// 模拟"fill 任务进行中"：future 非 null（tryStartBackgroundFill 提交后的状态）。
			setField(single, "messageFillFuture", CompletableFuture.completedFuture(null));

			file.failFill = true;
			Assertions.assertThrows(RuntimeException.class, single::pullMessage);

			Assertions.assertNull(getField(single, "messageFillFuture"),
					"fill异常后messageFillFuture必须复位，否则tryStartBackgroundFill永远跳过，分区回填永久停摆");
			Assertions.assertTrue(file.failedCount.get() >= 1);
		} finally {
			database.close();
			file.close();
		}
	}

	/**
	 * 端到端：fill 失败复位后，后续 sendMessage 事件驱动自动重试并恢复装载（分区内仍按 id 有序）。
	 * 模拟场景：盘上已有积压（数据文件中途损坏导致第一次装载失败），故障恢复后重试成功。
	 */
	@Test
	public void testFillRetriedOnSendMessageAfterFailure(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var home = tempDir.resolve("db").toString();
		var database = new RocksDatabase(home);
		var file = new FlakyFile(home, database);
		try {
			var single = new MQSingle(new MQPartition(null), "topic", 0, file);

			// 直接写盘制造积压 ids 0..4（绕开内存队列，等价于"盘上有、队列没有"的积压状态）。
			for (long id = 0; id < 5; ++id)
				file.appendMessage(sendMessageOf(id).getMessage());

			// sendMessage(id5)：队列空 != 盘上积压 5，不直入 → highLoad++ → 尝试启动后台回填。
			file.failFill = true;
			single.sendMessage(sendMessageOf(5));

			// 后台 fill 任务装载失败：future 复位、highLoad 重算为真实积压（6 条：0..5 都未装载）。
			await("fill任务失败后复位future并重算highLoad", () -> {
				try {
					return null == getField(single, "messageFillFuture")
							&& 6 == (Long)getField(single, "highLoad");
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
			Assertions.assertTrue(file.failedCount.get() >= 1, "装载确实失败过");

			// 故障恢复：下一个 sendMessage 事件重新驱动回填（旧代码 future 永久非空，永远无法重试）。
			file.failFill = false;
			single.sendMessage(sendMessageOf(6));

			await("重试装载全部积压", () -> file.queueRef != null && file.queueRef.size() == 7);
			// 全部积压按 id 有序装载，无跳过、无重复；无 ack 发生，位点不得推进。
			Assertions.assertEquals(List.of("0", "1", "2", "3", "4", "5", "6"), queueIds(file.queueRef));
			Assertions.assertEquals(0, file.getFirstMessageId());
			Assertions.assertEquals(7, file.getNextMessageId());
			Assertions.assertNull(getField(single, "messageFillFuture"), "装载完成后future应回到null");
		} finally {
			database.close();
			file.close();
		}
	}
}
