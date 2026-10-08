package Zeze.MQ.Master;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.Builtin.MQ.Master.CreateMQ;
import Zeze.Builtin.MQ.Master.OpenMQ;
import Zeze.Builtin.MQ.Master.Register;
import Zeze.Builtin.MQ.Master.ReportPartitions;
import Zeze.Builtin.MQ.Master.Subscribe;
import Zeze.Config;
import Zeze.Transaction.Procedure;
import Zeze.Util.RocksDatabase;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND20 GB-C01 回归：Master 侧停机静默（FND19 GB-C02 为 MQManager 建立的 stopped 闸形态的
 * Master 对称缺口）。Main.stop 旧顺序为 service.stop() 后立即 masterDb.close()，而
 * Service.stop 只关 socket 不清 worker 池已派发任务：stop 时刻在飞的触库 handler
 * （CreateMQ/Register→rewriteRoutes/对账链/OpenMQ/Subscribe 的 mqTable get/put/迭代）与
 * masterDb.close 并发是 native use-after-free（RocksDatabase.close 契约）。
 * <p>
 * 关机竞态无法确定性复现，本测试固化可确定性的契约面（形态对齐 TestMQManagerStopRejects）：
 * ① close 持模块锁有界排空——模块锁被占（=在飞触库 handler 持锁执行）时不得先关库；
 * ② stopped 置位后到达的触库请求在入口拒绝（Procedure.Closed，不触碰 mqTable）；
 * ③ 过入口闸、停在模块锁上的晚到 handler 在锁内复查拒绝。
 * <p>
 * 注：文件放 src/MQ/ 但声明 package Zeze.MQ.Master（handler 为包内可见的 protected 缝，
 * 布局约定见 MqTestSupport）。stopped/masterDb 经反射置读（前者修复引入，旧基线缺失即判红）。
 */
@Fast
public class TestMasterStopGate {

	/** 反射置 Master.stopped；旧基线无此字段返回 false（修复缺失形态）。 */
	private static boolean trySetStopped(Master master, boolean value) {
		try {
			var f = Master.class.getDeclaredField("stopped");
			f.setAccessible(true);
			f.setBoolean(master, value);
			return true;
		} catch (NoSuchFieldException e) {
			return false;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	/** 反射读 Master.masterDb（新旧基线皆有，观察关库时序）。 */
	private static RocksDatabase masterDbOf(Master master) throws Exception {
		var f = Master.class.getDeclaredField("masterDb");
		f.setAccessible(true);
		return (RocksDatabase)f.get(master);
	}

	/** 等待线程停靠（锁等待态）；超时静默继续（旧基线不停靠由断言判红）。 */
	private static void awaitParked(Thread t) throws InterruptedException {
		var deadline = System.currentTimeMillis() + 5_000;
		while (t.isAlive() && System.currentTimeMillis() < deadline
				&& t.getState() != Thread.State.WAITING && t.getState() != Thread.State.TIMED_WAITING)
			Thread.sleep(10);
	}

	/**
	 * ① close 的有界排空契约：模块锁（全部触库 handler 的入闸同步点）被占时，close 不得先关库
	 * ——必须等持锁 handler 出锁（预算 RpcTimeout+5s）。旧代码 close 直接 masterDb.close()，
	 * 在飞 handler 与关库并发即 use-after-free。
	 */
	@Test
	public void testCloseDrainsModuleLockHolders(@TempDir Path tempDir) throws Exception {
		var master = new Master(tempDir.resolve("master").toString(), new Config());
		var db = masterDbOf(master);
		master.getLock().lock(); // 模拟在飞触库 handler 持模块锁执行
		var failure = new AtomicReference<Throwable>();
		var closer = new Thread(() -> {
			try {
				master.close();
			} catch (Throwable e) {
				failure.set(e);
			}
		}, "master-stop-closer");
		closer.start();
		Thread.sleep(500); // 旧代码毫秒级完成关库；修复形态停靠在 tryLock（预算25s）
		Assertions.assertFalse(db.isClosed(),
				"模块锁被占（在飞触库 handler）时 close 不得先关 masterDb（旧代码直接关库，FND20 GB-C01）");
		master.getLock().unlock();
		closer.join(30_000);
		Assertions.assertFalse(closer.isAlive(), "排空完成后 close 必须返回（预算 RpcTimeout+5s）");
		Assertions.assertNull(failure.get(), "close 不得抛出");
		Assertions.assertTrue(db.isClosed(), "排空后关库完成");
	}

	/**
	 * ② 入口闸契约：stopped 置位后到达的全部触库 handler 返回 Procedure.Closed，不触碰 mqTable。
	 */
	@Test
	public void testHandlersRejectAfterStopMarked(@TempDir Path tempDir) throws Exception {
		var master = new Master(tempDir.resolve("master").toString(), new Config());
		try {
			Assertions.assertTrue(trySetStopped(master, true),
					"Master.stopped 停机闸缺失（FND20 GB-C01 修复不存在）");

			var create = new CreateMQ();
			create.Argument.setTopic("t");
			create.Argument.setPartition(1);
			Assertions.assertEquals(Procedure.Closed, master.ProcessCreateMQRequest(create),
					"stopped 后 CreateMQ 入口拒绝");

			var open = new OpenMQ();
			open.Argument.setTopic("t");
			Assertions.assertEquals(Procedure.Closed, master.ProcessOpenMQRequest(open),
					"stopped 后 OpenMQ 入口拒绝");

			Assertions.assertEquals(Procedure.Closed, master.ProcessRegisterRequest(new Register()),
					"stopped 后 Register（rewriteRoutes 全表触库）入口拒绝");

			Assertions.assertEquals(Procedure.Closed, master.ProcessReportPartitionsRequest(new ReportPartitions()),
					"stopped 后 ReportPartitions（对账链触库）入口拒绝");

			var subscribe = new Subscribe();
			subscribe.Argument.setTopic("t");
			Assertions.assertEquals(Procedure.Closed, master.ProcessSubscribeRequest(subscribe),
					"stopped 后 Subscribe 入口拒绝");
		} finally {
			trySetStopped(master, false); // 测试内复位，走真实 close（无竞争）
			master.close();
		}
	}

	/**
	 * ③ 锁内复查契约：过入口闸（stopped=false）后停在模块锁上的晚到 handler，等锁期间 stop
	 * 置位——取锁后必须复查拒绝（Procedure.Closed），不得继续触库应答。旧代码 OpenMQ 不持锁
	 * 且无闸，直接走 mqTable.get→SendResult（结果非 Closed，判红）。
	 */
	@Test
	public void testInLockRecheckAbortsPastGateHandler(@TempDir Path tempDir) throws Exception {
		var master = new Master(tempDir.resolve("master").toString(), new Config());
		try {
			var db = masterDbOf(master);
			// 在册 topic：旧基线（无闸无锁）会走完 get→decode→SendResult 全程
			var servers = new Zeze.Builtin.MQ.Master.BMQServers();
			servers.getInfo().setTopic("t");
			servers.getInfo().setPartition(1);
			servers.getServers().add(new Zeze.Builtin.MQ.Master.BMQServer("h", 1, 0, "t", 7L));
			master.putMqServers("t", servers);

			var open = new OpenMQ();
			open.Argument.setTopic("t");

			master.getLock().lock(); // 晚到 handler 将停靠的入闸同步点
			var result = new AtomicLong(Long.MIN_VALUE);
			var failure = new AtomicReference<Throwable>();
			var handler = new Thread(() -> {
				try {
					result.set(master.ProcessOpenMQRequest(open)); // 入口闸通过（此刻未停机）
				} catch (Throwable e) {
					failure.set(e);
				}
			}, "master-stop-handler");
			handler.start();
			awaitParked(handler);
			var gated = trySetStopped(master, true); // stop 最前置位（此刻 handler 在锁上等）
			master.getLock().unlock();
			handler.join(10_000);
			Assertions.assertFalse(handler.isAlive());
			Assertions.assertNull(failure.get(), "handler 不得以异常逃逸");
			Assertions.assertTrue(gated, "Master.stopped 停机闸缺失（FND20 GB-C01 修复不存在）");
			Assertions.assertEquals(Procedure.Closed, result.get(),
					"过闸晚到 handler 须在锁内复查拒绝，不得触库应答（FND20 GB-C01）");
			Assertions.assertFalse(db.isClosed());
		} finally {
			trySetStopped(master, false);
			master.close();
		}
	}
}
