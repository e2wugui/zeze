package Zeze.MQ;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND21 GB-C02 回归：MQSingle.close 的排空只观察"锁内读到的单代 messageFillFuture"——
 * 排空等待期间 fill 完成路径自提交下一代（pullMessage 尾部 messageFillFuture=null 后紧跟
 * tryStartBackgroundFill，此刻 closed 尚未置位；分区删除路径 Manager 存活、无 stopped 兜底，
 * handlePushResult 成功路径与 sendMessage 同样在锁内提交），置闸段不重读不取消
 * messageFillFuture——逃逸代立即开跑的 fillMessage（索引迭代器+段文件读）与其后
 * deletePartitionStorage 的 dropTable 并发是 native use-after-free。
 * <p>
 * 修复形态：置 closed 后循环重读 messageFillFuture 并锁外有界排空（closed 先行使
 * tryStartBackgroundFill 恒拒绝，循环必收敛；等待必须锁外——fill 任务体收尾的
 * tryStartBackgroundFill/tryPushMessage 需要本锁，持锁等待是必然超时的自阻）。
 * <p>
 * 手工 future 注入模拟世代链（每步与真实 pullMessage 完成路径的状态迁移一一对应，确定性无竞态）：
 * F1 在飞（close 第一段排空停靠 f1.get）→ 等待期间完成路径提交 F2（覆盖 messageFillFuture）
 * → F1 完成（唤醒 close）→ 置闸 → 世代排空段停靠 f2.get → F2 完成并自清字段 → close 返回。
 * <p>
 * 判别点：逃逸代在飞时 close 不得返回——修复代码停靠在世代排空（预算 RpcTimeout+5s）；
 * 旧代码置闸后直接返回（逃逸代存活到 close 之后，判红：closer 线程已终结）。
 */
@Fast
public class TestCloseFillGenerationDrain {

	private static Object getField(MQSingle single, String name) throws Exception {
		var f = MQSingle.class.getDeclaredField(name);
		f.setAccessible(true);
		return f.get(single);
	}

	private static void setField(MQSingle single, String name, Object value) throws Exception {
		var f = MQSingle.class.getDeclaredField(name);
		f.setAccessible(true);
		f.set(single, value);
	}

	private static void awaitParked(Thread t) throws InterruptedException {
		var deadline = System.currentTimeMillis() + 5_000;
		while (t.isAlive() && System.currentTimeMillis() < deadline
				&& t.getState() != Thread.State.WAITING && t.getState() != Thread.State.TIMED_WAITING)
			Thread.sleep(10);
	}

	/**
	 * 核心契约：close 返回 ⟺ 逃逸代已排空（messageFillFuture 终态 null）。
	 * 旧代码只等锁内读到的一代（F1），其等待期间繁衍的 F2 存活到 close 之后。
	 */
	@Test
	public void testCloseDrainsEscapeeFillGeneration(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var home = tempDir.resolve("db").toString();
		var database = new RocksDatabase(home);
		var file = new TestMQSingleFillStall.FlakyFile(home, database);
		try {
			var single = new MQSingle(new MQPartition(null), "topic", 0, file); // 空分区构造成功

			var f1 = new CompletableFuture<Void>();
			var f2 = new CompletableFuture<Void>();
			setField(single, "messageFillFuture", f1); // F1 在飞（close 第一段排空的目标）

			var failure = new AtomicReference<Throwable>();
			var closer = new Thread(() -> {
				try {
					single.close();
				} catch (Throwable e) {
					failure.set(e);
				}
			}, "fnd21-gbc02-closer");
			closer.start();
			awaitParked(closer); // closer 停靠在第一段的 f1.get（TIMED_WAITING）⟹ 锁内读已看到 F1

			// F1 完成路径的世代繁衍（closed 未置位窗口内的真实迁移序）：
			// 提交 F2（tryStartBackgroundFill 赋回 messageFillFuture）→ F1 完成（唤醒 close 的第一段等待）。
			setField(single, "messageFillFuture", f2);
			f1.complete(null);

			Thread.sleep(500); // 置闸段毫秒级完成：修复代码停靠在世代排空的 f2.get；旧代码已返回
			Assertions.assertTrue(closer.isAlive(),
					"逃逸代（F2）在飞时 close 不得返回——必须循环排空后才算关闭完成（FND21 GB-C02："
							+ "逃逸代存活到 close 之后，其 fillMessage 与 deletePartitionStorage 的 dropTable 并发"
							+ "是 native use-after-free）");

			// F2 完成（任务体自清 messageFillFuture）→ 世代排空收敛，close 返回。
			f2.complete(null);
			setField(single, "messageFillFuture", null);
			closer.join(30_000);
			Assertions.assertFalse(closer.isAlive(), "逃逸代完成后 close 必须返回（预算 RpcTimeout+5s）");
			Assertions.assertNull(failure.get(), "close 不得抛出");
			Assertions.assertTrue((Boolean)getField(single, "closed"), "closed 闸已置位");
			Assertions.assertNull(getField(single, "messageFillFuture"),
					"close 返回时 messageFillFuture 必须为 null（closed 后 tryStartBackgroundFill 恒拒绝，"
							+ "读到 null 即终态）");
		} finally {
			database.close();
			file.close();
		}
	}
}
