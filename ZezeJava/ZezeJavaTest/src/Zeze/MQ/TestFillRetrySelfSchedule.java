package Zeze.MQ;

import harness.Extra;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import Zeze.Builtin.MQ.BMessage;
import Zeze.Builtin.MQ.BSendMessage;
import Zeze.Util.Action0;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND20 GB-D03 回归：回填失败的自驱动重试（拍板 A：失败点排期，替代纯事件驱动）。
 * <p>
 * 修复前：FND-G2-2 修复给 fill 失败做"复位 future+重算 highLoad"但刻意不重启 fill，依赖后续
 * sendMessage/ack 事件驱动重试。回收竞态（GB-D02 段回收 × fill 相交，瞬时 messageIndexNotFound）
 * 制造"队列空+无 ack 在途+无新消息"的无事件源窗口时——积压场景常态（正是没有新消息才有积压）
 * ——分区在消费者健康在线时无限期停摆，失败只有一条 error，无周期提醒。
 * <p>
 * 修复后不变式：任何一次 fill 失败，无论外部是否再有事件，最终在有界延迟内必有下一次尝试
 * ——pullMessage 的 catch 在复位完成后按指数退避（复用 retryBackoffMs 公式）排期一次
 * tryStartBackgroundFill；构造路径豁免（创建失败保持响亮上抛，不为僵尸分区排期）；单槽句柄
 * cancel+replace；close 取消句柄并置 closed 短路迟到触发。
 * <p>
 * 复用 TestMQSingleFillStall 的 FlakyFile 注入（fillMessage 可注入失败）+ fillRetryScheduler
 * 注入缝捕获延迟序列与排期动作（手动驱动=确定性时钟）。fillRetryScheduler 经反射访问：测试
 * 需双车道复用（orig 基线缺失即判红），形态对齐 TestFnd20GBC02 反射缝先例。
 */
@Fast
@Extra
public class TestFillRetrySelfSchedule {

	/** 一次捕获的自排期（延迟+动作+返回的句柄，测试手动驱动=确定性时钟）。 */
	private static final class CapturedSchedule {
		final long delayMs;
		final Action0 action;
		final CompletableFuture<Void> handle = new CompletableFuture<>();

		CapturedSchedule(long delayMs, Action0 action) {
			this.delayMs = delayMs;
			this.action = action;
		}
	}

	/** 反射注入 fillRetryScheduler（捕获不执行）；旧基线无此字段判红（修复不存在）。 */
	private static List<CapturedSchedule> injectFillRetryScheduler(MQSingle single) throws Exception {
		var captured = new ArrayList<CapturedSchedule>();
		try {
			Field f = MQSingle.class.getDeclaredField("fillRetryScheduler");
			f.setAccessible(true);
			f.set(single, (MQSingle.RetryScheduler)(delayMs, action) -> {
				var schedule = new CapturedSchedule(delayMs, action);
				captured.add(schedule);
				return schedule.handle;
			});
		} catch (NoSuchFieldException e) {
			throw new AssertionError("fill 失败自排期机制缺失（FND20 GB-D03 修复不存在）", e);
		}
		return captured;
	}

	private static Object getField(Object obj, String name) throws Exception {
		var f = obj.getClass().getDeclaredField(name);
		f.setAccessible(true);
		return f.get(obj);
	}

	private static void setField(Object obj, String name, Object value) throws Exception {
		var f = obj.getClass().getDeclaredField(name);
		f.setAccessible(true);
		f.set(obj, value);
	}

	private static BSendMessage.Data sendMessageOf(long id) {
		var message = new BMessage.Data();
		message.setTimestamp(id);
		var send = new BSendMessage.Data();
		send.setMessage(message);
		return send;
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
	 * 核心不变式：fill 失败后无任何外部事件（无 sendMessage/ack/bind），自排期驱动重试，
	 * 故障恢复后盘上积压完整装载（分区内按 id 有序）。旧基线：无 fillRetryScheduler（判红）；
	 * 若无自排期，重试只能等外部事件——本测试不再发事件，装载永不发生。
	 */
	@Test
	public void testFillFailureSelfSchedulesRecovery(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var home = tempDir.resolve("db").toString();
		var database = new RocksDatabase(home);
		var file = new TestMQSingleFillStall.FlakyFile(home, database);
		try {
			var single = new MQSingle(new MQPartition(null), "topic", 0, file);
			var captured = injectFillRetryScheduler(single); // 旧基线：NoSuchFieldException 判红

			// 直接写盘制造积压 ids 0..4（"盘上有、队列没有"的积压状态，TestMQSingleFillStall 同款）。
			for (long id = 0; id < 5; ++id)
				file.appendMessage(sendMessageOf(id).getMessage());

			// sendMessage(id5)：不直入 → highLoad++ → 后台 fill 提交 → 注入失败。
			file.failFill = true;
			single.sendMessage(sendMessageOf(5));

			await("fill失败后复位future、重算highLoad并自排期", () -> {
				try {
					return file.failedCount.get() >= 1 && captured.size() == 1
							&& null == getField(single, "messageFillFuture")
							&& 6 == (Long)getField(single, "highLoad");
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
			// 首次失败退避 = PushRetryBackoffBaseMs(默认500)<<1（DEFAULT_CONFIG，与 GB-D06 公式同源）。
			Assertions.assertEquals(1000, captured.get(0).delayMs, "失败点自排期按指数退避（base<<1）");

			// 故障恢复后无任何外部事件：唯一驱动源=自排期动作（退避到期形态）。
			file.failFill = false;
			captured.get(0).action.run();

			await("自排期驱动重试装载全部积压", () -> {
				try {
					return file.queueRef != null && file.queueRef.size() == 6
							&& null == getField(single, "messageFillFuture");
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
			Assertions.assertEquals(List.of("0", "1", "2", "3", "4", "5"), queueIds(file.queueRef),
					"自排期重试装载全部积压，按 id 有序（无事件源窗口不再停摆）");
			Assertions.assertEquals(0, file.getFirstMessageId(), "无 ack 发生，位点不得推进");
			Assertions.assertEquals(6, file.getNextMessageId());
			single.close(); // 顺带关闭文件流（file.close 同一目标）
		} finally {
			database.close();
		}
	}

	/**
	 * 退避形态与句柄生命周期：连续失败指数退避（base<<n）；单槽句柄 cancel+replace；
	 * close 取消句柄并短路迟到触发（closed 检查，分区删除/停机后无僵尸重试）。
	 * 顺带固化 FND-G2-2 的复位语义不回归（future 复位）。
	 */
	@Test
	public void testBackoffGrowthSingleSlotAndCloseCancel(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var home = tempDir.resolve("db2").toString();
		var database = new RocksDatabase(home);
		var file = new TestMQSingleFillStall.FlakyFile(home, database);
		try {
			var single = new MQSingle(new MQPartition(null), "topic", 0, file); // 空分区构造成功
			var captured = injectFillRetryScheduler(single); // 旧基线：NoSuchFieldException 判红
			setField(single, "messageFillFuture", CompletableFuture.completedFuture(null)); // 模拟在飞fill

			file.failFill = true;
			Assertions.assertThrows(RuntimeException.class, single::pullMessage);
			Assertions.assertThrows(RuntimeException.class, single::pullMessage);

			Assertions.assertEquals(List.of(1000L, 2000L),
					captured.stream().map(c -> c.delayMs).toList(),
					"连续失败指数退避（base<<1, base<<2；确定性损坏下重试频率有界=退避封顶）");
			Assertions.assertTrue(captured.get(0).handle.isCancelled(),
					"单槽句柄：新失败 cancel 旧排期（pending 数自限）");
			Assertions.assertFalse(captured.get(1).handle.isCancelled());
			Assertions.assertNull(getField(single, "messageFillFuture"),
					"FND-G2-2 复位语义不回归：fill异常后messageFillFuture必须复位");

			single.close(); // null-manager close：closed 置位 + 句柄取消（文件流一并关闭）
			Assertions.assertTrue(captured.get(1).handle.isCancelled(),
					"close 取消 fill 自排期句柄（对齐 retryFuture 的取消形态）");
			captured.get(1).action.run(); // 迟到触发（close 后到达的排期动作）
			Assertions.assertNull(getField(single, "messageFillFuture"),
					"close 后迟到的自排期触发不得提交新 fill（closed 检查短路，无僵尸重试）");
		} finally {
			database.close();
		}
	}

	/**
	 * 构造路径豁免：构造直调 pullMessage 失败保持"创建失败"响亮上抛，且不为僵尸分区排期
	 * 自重试（构造抛出后实例不发布，自排期只会重试无人引用的半成品分区）。
	 */
	@Test
	public void testConstructorFailureLoudNoZombieRetry(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var home = tempDir.resolve("db3").toString();
		var database = new RocksDatabase(home);
		var file = new TestMQSingleFillStall.FlakyFile(home, database);
		try {
			for (long id = 0; id < 3; ++id)
				file.appendMessage(sendMessageOf(id).getMessage()); // 构造期即有盘上积压
			file.failFill = true;

			Assertions.assertThrows(RuntimeException.class,
					() -> new MQSingle(new MQPartition(null), "topic", 0, file),
					"构造装载失败保持'创建失败'响亮语义上抛");

			Thread.sleep(1_500); // 若误为僵尸分区排期，默认/注入退避 ≤1.5s 内必有第二次尝试
			Assertions.assertEquals(1, file.failedCount.get(),
					"构造失败不得为僵尸分区排期自重试（FND20 GB-D03 构造路径豁免）");
			file.close();
		} finally {
			database.close();
		}
	}
}
