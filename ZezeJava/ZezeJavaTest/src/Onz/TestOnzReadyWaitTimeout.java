package Onz;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import Zeze.Config;
import Zeze.Onz.OnzProcedure;
import Zeze.Onz.OnzServer;
import Zeze.Onz.OnzTransaction;
import demo.App;
import demo.Module1.BKuafu;
import demo.Module1.BKuafuResult;
import harness.TestEnv;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * FND5-45 回归：参与方FuncProcedure执行成功后，在Transaction.perform提交路径调
 * sendReadyAndWait：先markReadyProcedure登记再等协调者的Commit/Rollback，而协调者
 * 的决策持久化saveCommitPoint排在txn.perform()之后——窗口覆盖整个业务执行期。
 * 协调者在perform期间崩溃（commitIndex尚无记录，重启redoTimer不会重发Rollback）
 * 时，commitFuture.await()无超时：参与方事务线程永久阻塞，已过lockAndCheck持有
 * 行锁，波及后续访问同记录的事务，readyProcedures条目永久滞留。
 * 对照：flush路径同类无超时await已被显式修复（OnzProcedure.sendFlushReady注释），
 * ready路径漏网。
 * 场景：参与方过程执行完毕进入ready等待后，协调者perform永久挂起（模拟崩溃，决策
 * 未持久化），flushTimeout=2s。断言：参与方超时自愈回滚——对同一行键的新事务必须
 * 在限期内获得锁执行成功（修复前：锁被死等事务持有，探测事务无法完成）。
 */
public class TestOnzReadyWaitTimeout {
	// 过程名必须全 JVM 唯一：demo.App 单例的 Onz 注册表跨测试类持久。
	// App.Instance 是单例，类内多个@Test共用一次注册；zeze2 每测试新实例，每次注册。
	private static final java.util.concurrent.atomic.AtomicBoolean registeredOnAppInstance = new java.util.concurrent.atomic.AtomicBoolean();
	private static final String ProcName = "kuafuReadyWait";

	private final App zeze2 = new App();
	private OnzServer onzServer;

	@BeforeEach
	public void before() throws Exception {
		// 第二对服务 SM(5011)/Global(5012) 由 TestEnvLauncherListener 在进程内自动启动。
		Assumptions.assumeTrue(TestEnv.portReachable("127.0.0.1", 5011) && TestEnv.portReachable("127.0.0.1", 5012),
				"第二对服务(5011/5012)不可用：zeze.test.env=off 时 TestEnvLauncherListener 不在进程内自动启动");

		var myConfig = Config.load("zeze.xml");
		var dbHome = "CommitOnzServer" + myConfig.getServerId();
		deleteRecursively(Path.of(dbHome));

		App.Instance.Start();
		var config2 = Config.load("./zeze_cluster_2.xml");
		zeze2.Start(config2);

		Infinite.App.clearDbTable(zeze2.demo_Module1.getKuafu());
		Infinite.App.clearDbTable(App.Instance.demo_Module1.getKuafu());

		if (registeredOnAppInstance.compareAndSet(false, true))
			App.Instance.Zeze.getOnz().register(ProcName, TestOnzReadyWaitTimeout::kuaFu, BKuafu.class, BKuafuResult.class);
		zeze2.Zeze.getOnz().register(ProcName, TestOnzReadyWaitTimeout::kuaFu, BKuafu.class, BKuafuResult.class);

		onzServer = new OnzServer("zeze1=zeze.xml;zeze2=zeze_cluster_2.xml", myConfig);
		onzServer.start();
	}

	@AfterEach
	public void after() throws Exception {
		// before() 被 Assumption 跳过时 onzServer 尚未创建；stop幂等
		if (onzServer != null)
			onzServer.stop();
		zeze2.Stop();
	}

	private static long kuaFu(OnzProcedure onzProcedure, BKuafu argument, BKuafuResult result) {
		var app = (App)onzProcedure.getStub().getOnz().getZeze().getAppBase();
		var account = app.demo_Module1.getKuafu().getOrAdd(argument.getAccount());
		account.setMoney(account.getMoney() + argument.getMoney());
		result.setMoney(account.getMoney());
		return 0;
	}

	/** 协调者在perform阶段"崩溃"：参与方执行完毕进入ready等待后，perform永不推进。 */
	private static class CrashCoordinatorTransaction extends OnzTransaction<BKuafu.Data, BKuafuResult.Data> {
		final CountDownLatch participantReady = new CountDownLatch(1);
		final CountDownLatch release = new CountDownLatch(1);

		@Override
		protected long perform() throws Exception {
			var arg = new BKuafu.Data();
			arg.setAccount(200);
			arg.setMoney(10);
			super.callProcedureAsync("zeze1", ProcName, arg, new BKuafuResult.Data()).get();
			participantReady.countDown(); // 参与方已执行完毕进入ready等待
			release.await(); // 模拟崩溃：决策未持久化（saveCommitPoint在其后）
			return 0;
		}
	}

	@Test
	@Timeout(120)
	public void testParticipantSelfHealsOnCoordinatorCrash() throws Exception {
		waitOnzReady();
		var txn = new CrashCoordinatorTransaction();
		txn.setOnzServer(onzServer);
		txn.setFlushTimeout(2_000);
		var performRc = new long[1];
		var coordinator = new Thread(() -> performRc[0] = onzServer.perform(txn));
		coordinator.setDaemon(true);
		coordinator.start();
		try {
			Assertions.assertTrue(txn.participantReady.await(10, TimeUnit.SECONDS), "参与方必须先就绪");

			// 探测：对同一行键的新事务必须能获得锁（参与方超时自愈、行锁释放后）。
			var probeDone = new CountDownLatch(1);
			var probeError = new AtomicReference<Throwable>();
			var probe = new Thread(() -> {
				try {
					App.Instance.Zeze.newProcedure(() -> {
						var account = App.Instance.demo_Module1.getKuafu().getOrAdd(200L);
						account.setMoney(account.getMoney() + 1);
						return 0;
					}, "TestOnzReadyWaitTimeout.probe").call();
				} catch (Throwable e) {
					probeError.set(e);
				} finally {
					probeDone.countDown();
				}
			});
			probe.setDaemon(true);
			probe.start();
			Assertions.assertTrue(probeDone.await(15, TimeUnit.SECONDS),
					"参与方ready等待必须超时自愈并释放行锁（FND5-45）");
			Assertions.assertNull(probeError.get(), "探测事务必须成功执行");

			// 自愈=按Rollback处理：参与方写入未落（金额=探测的+1，而非参与方的+10）。
			waitMoney(1, 10_000, "参与方超时自愈必须回滚本地写入");
		} finally {
			// 收尾（断言失败也执行）：释放"崩溃"的协调者——perform走完，其Commit命中
			// 超时回滚登记：假应答成功但真实不一致由error日志暴露（FND5-44/45联动）。
			txn.release.countDown();
			coordinator.join(30_000);
		}
		Assertions.assertEquals(0, performRc[0], "收尾：协调者perform完成");
		waitMoney(1, 10_000, "收尾后金额保持：参与方不复活已回滚的写入");
	}

	private static long getMoney() throws Exception {
		var holder = new long[1];
		App.Instance.Zeze.newProcedure(() -> {
			holder[0] = App.Instance.demo_Module1.getKuafu().getOrAdd(200L).getMoney();
			return 0;
		}, "TestOnzReadyWaitTimeout.getMoney").call();
		return holder[0];
	}

	private static void waitMoney(long expected, long timeoutMs, String message) throws Exception {
		long deadline = System.currentTimeMillis() + timeoutMs;
		while (getMoney() != expected) {
			Assertions.assertTrue(System.currentTimeMillis() < deadline, message
					+ "（期望=" + expected + " 实际=" + getMoney() + "）");
			//noinspection BusyWait
			Thread.sleep(50);
		}
	}

	// 同 TestOnz.waitOnzReady：等订阅发现两侧集群并建连（getZezeInstance成功即perform就绪）。
	private void waitOnzReady() throws InterruptedException {
		var deadline = System.currentTimeMillis() + 60_000;
		for (;;) {
			try {
				onzServer.getZezeInstance("zeze1");
				onzServer.getZezeInstance("zeze2");
				return;
			} catch (RuntimeException e) {
				if (System.currentTimeMillis() > deadline)
					throw e;
				Thread.sleep(100);
			}
		}
	}

	private static void deleteRecursively(Path root) throws Exception {
		if (!Files.exists(root))
			return;
		try (var walk = Files.walk(root)) {
			walk.sorted(Comparator.reverseOrder()).forEach(p -> {
				try {
					Files.delete(p);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
		}
	}
}
