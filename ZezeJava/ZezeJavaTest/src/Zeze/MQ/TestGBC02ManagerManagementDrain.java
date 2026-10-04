package Zeze.MQ;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Builtin.MQ.Master.CreatePartition;
import Zeze.Builtin.MQ.Master.DeletePartition;
import Zeze.Config;
import Zeze.Transaction.Procedure;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND20 GB-C02 回归：MQManager 停机闸对"已过闸正在执行"的 Create/DeletePartition handler 的
 * 覆盖缺口。入口一次性闸（增量审R1-02）只拦"stop 之后到达"的任务：delete 过闸后 removePartition
 * 有界排空最长 RpcTimeout+5s（被删分区已从活集合摘除，stop 的 queue.close 迭代等不到它），
 * create 过闸后 MQSingle 构造装载秒级（computeIfAbsent 不入 map，同样排空不到）——stop 可在
 * handler 的长路径内完成 rocksDatabase.close()，handler 恢复后触库（dropTable/getOrAddTable）
 * 即对已关库的 native 调用（RocksDatabase.close 契约）。
 * <p>
 * 修复形态（对齐 MQSingle.sendMessage 与 close 的锁内收口同构）：两 handler 全程持
 * managementLock + 锁内复查 stopped；stop 在 rocksDatabase.close 前有界获取同一把锁排空。
 * 本测试固化可确定性的契约面：
 * ① stop 的关库不得先于 managementLock 释放（在飞管理 handler 排空后才关库）；
 * ② 过闸停在锁上的晚到 handler 在锁内复查拒绝（Procedure.Closed，不触库不建/不删）。
 * <p>
 * 注：managementLock/stopped 经反射置读（前者修复引入，旧基线缺失即判红；形态对齐
 * TestMQManagerStopRejects）。包内缝 createPartition/getQueueForTest 见 Fnd19MqTestSupport。
 */
@Fast
public class TestGBC02ManagerManagementDrain {

	/** 反射读 MQManager.managementLock；旧基线无此字段抛出（修复缺失判红）。 */
	private static ReentrantLock managementLockOf(MQManager manager) throws Exception {
		var f = MQManager.class.getDeclaredField("managementLock");
		f.setAccessible(true);
		return (ReentrantLock)f.get(manager);
	}

	/** 反射置 MQManager.stopped（FND19 引入，新旧基线皆有）。 */
	private static void setStopped(MQManager manager, boolean value) throws Exception {
		var f = MQManager.class.getDeclaredField("stopped");
		f.setAccessible(true);
		f.setBoolean(manager, value);
	}

	private static void awaitParked(Thread t) throws InterruptedException {
		var deadline = System.currentTimeMillis() + 5_000;
		while (t.isAlive() && System.currentTimeMillis() < deadline
				&& t.getState() != Thread.State.WAITING && t.getState() != Thread.State.TIMED_WAITING)
			Thread.sleep(10);
	}

	/**
	 * ① stop 的管理面排空契约：managementLock 被占（=过闸管理 handler 在飞）时，stop 不得先关
	 * rocksDatabase——须取同一把锁等 handler 出锁（预算 RpcTimeout+5s）。旧代码 stop 不感知
	 * 管理 handler，25s 排空窗口内先关库（FND20 GB-C02）。
	 */
	@Test
	public void testStopDrainsManagementHandlersBeforeCloseRocksdb(@TempDir Path tempDir) throws Exception {
		var manager = new MQManager(tempDir.resolve("manager").toString(), new Config());
		manager.createPartition("t", new HashSet<>(java.util.List.of(0))); // 活分区（删除路径前置）
		var managementLock = managementLockOf(manager); // 旧基线：NoSuchFieldException 判红
		managementLock.lock(); // 模拟过闸管理 handler 持锁执行（delete 的 25s 排空窗口形态）
		var failure = new AtomicReference<Throwable>();
		var stopper = new Thread(() -> {
			try {
				manager.stop();
			} catch (Throwable e) {
				failure.set(e);
			}
		}, "fnd20-gbc02-stopper");
		stopper.start();
		Thread.sleep(500); // 修复形态停靠在 tryLock（预算25s）；旧代码毫秒级走完 queue.close+关库
		Assertions.assertFalse(manager.getRocksDatabase().isClosed(),
				"管理 handler 在飞（managementLock 被占）时 stop 不得先关 rocksDatabase（FND20 GB-C02）");
		managementLock.unlock();
		stopper.join(30_000);
		Assertions.assertFalse(stopper.isAlive(), "管理面排空完成后 stop 必须返回（预算 RpcTimeout+5s）");
		Assertions.assertNull(failure.get(), "stop 不得抛出");
		Assertions.assertTrue(manager.getRocksDatabase().isClosed(), "排空后关库完成");
	}

	/**
	 * ② 锁内复查契约：过入口闸（stopped=false）后停在 managementLock 上的晚到 CreatePartition，
	 * 等锁期间 stop 置位——取锁后必须复查拒绝（Procedure.Closed），不得构造 MQSingle 触库；
	 * 已停止后到达的 DeletePartition 走入口闸拒绝，不得摘除活分区。
	 */
	@Test
	public void testPastGateHandlersRejectedInLock(@TempDir Path tempDir) throws Exception {
		var manager = new MQManager(tempDir.resolve("manager").toString(), new Config());
		manager.createPartition("t", new HashSet<>(java.util.List.of(0)));
		try {
			var managementLock = managementLockOf(manager); // 旧基线：NoSuchFieldException 判红
			managementLock.lock();

			var create = new CreatePartition();
			create.Argument.setTopic("t2");
			create.Argument.getPartitionIndexes().add(0);
			var createResult = new AtomicLong(Long.MIN_VALUE);
			var createFailure = new AtomicReference<Throwable>();
			var creator = new Thread(() -> {
				try {
					createResult.set(manager.createPartition(create)); // 入口闸通过（此刻未停机）
				} catch (Throwable e) {
					createFailure.set(e);
				}
			}, "fnd20-gbc02-creator");
			creator.start();
			awaitParked(creator);
			setStopped(manager, true); // stop 最前置位（此刻 handler 在锁上等）
			managementLock.unlock();
			creator.join(10_000);
			Assertions.assertFalse(creator.isAlive());
			Assertions.assertNull(createFailure.get(), "handler 不得以异常逃逸");
			Assertions.assertEquals(Procedure.Closed, createResult.get(),
					"过闸晚到 CreatePartition 须在锁内复查拒绝，不得构造 MQSingle 触库（FND20 GB-C02）");
			Assertions.assertNull(manager.getQueueForTest("t2"), "被拒绝的创建不得落任何状态");

			// 已停止后到达的 DeletePartition：入口闸拒绝，活分区原样保留（残留由 Master 对账
			// 宽限期后重发自愈，GB-D01）。
			var delete = new DeletePartition();
			delete.Argument.setTopic("t");
			delete.Argument.getPartitionIndexes().add(0);
			Assertions.assertEquals(Procedure.Closed, manager.deletePartition(delete),
					"stopped 后 DeletePartition 入口拒绝");
			Assertions.assertNotNull(manager.getQueueForTest("t"), "入口拒绝的删除不得摘除活分区");
		} finally {
			// 未 start 的 stop() 安全（TestMQManagerStopRejects 同款依据）；stopped 已置位无妨。
			manager.stop();
		}
	}
}
