package Onz;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.CountDownLatch;

import Zeze.Builtin.Onz.BSavedCommits;
import Zeze.Config;
import Zeze.Onz.AbstractOnz;
import Zeze.Onz.OnzProcedure;
import Zeze.Onz.OnzServer;
import Zeze.Onz.OnzTransaction;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.RocksDatabase;
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
 * FND5-44 回归：OnzServer.perform 从 saveCommitPoint(ePreparing) 到 txn.commit
 * 覆盖为 eCommitting 之间存在 waitPendingAsync 等待窗口（业务异步放大，时长不定），
 * redoTimer 每60s持dbLock遍历commitIndex（RocksDB iterator为创建时快照），对落在
 * 窗口内的进行中ePreparing直接redo(rollback)——参与方回滚后对协调者迟到的Commit
 * 假应答成功（readyProcedures.remove为null直接SendResult(0)），协调者perform返回0，
 * 实际静默部分提交，无任何日志线索。
 * 场景（核心，修复前红）：参与方事务挂住协调者的pendingAsync → ePreparing落盘后
 * 手动驱动redoTimer → 释放协调者 → perform返回0但参与方必须真正提交。
 * FND6-36新契约（2759bfd，登记所有权门控取代年龄闸作为存活判据）：
 * - 存活登记的ePreparing（perform在途，addTransaction→finally removeTransaction）：
 *   即使值被改写为旧格式（仅state无时戳，升级遗留）或超龄时戳，redo也跳过不回滚，
 *   协调者正常提交、金额保持业务值——年龄不再是存活事务的判据；
 * - 真孤儿（无存活登记的崩溃残留，不经perform直接落库）：redo仍回滚清理（旧格式/
 *   超龄记录的恢复路径不被门控废掉），索引与commitPoint两表一起删除。
 */
public class TestOnzRedoPreparingWindow {
	// 过程名必须全 JVM 唯一：demo.App 单例的 Onz 注册表跨测试类持久（"kuafu"归 TestOnz）。
	// App.Instance 是单例，类内多个@Test共用一次注册；zeze2 每测试新实例，每次注册。
	private static final java.util.concurrent.atomic.AtomicBoolean registeredOnAppInstance = new java.util.concurrent.atomic.AtomicBoolean();
	private static final String ProcName = "kuafuRedoWin";

	private final App zeze2 = new App();
	private OnzServer onzServer;
	private String dbHome;

	@BeforeEach
	public void before() throws Exception {
		// 第二对服务 SM(5011)/Global(5012) 由 TestEnvLauncherListener 在进程内自动启动。
		Assumptions.assumeTrue(TestEnv.portReachable("127.0.0.1", 5011) && TestEnv.portReachable("127.0.0.1", 5012),
				"第二对服务(5011/5012)不可用：zeze.test.env=off 时 TestEnvLauncherListener 不在进程内自动启动");

		var myConfig = Config.load("zeze.xml");
		dbHome = "CommitOnzServer" + myConfig.getServerId();
		deleteRecursively(Path.of(dbHome));

		App.Instance.Start();
		var config2 = Config.load("./zeze_cluster_2.xml");
		zeze2.Start(config2);

		Infinite.App.clearDbTable(zeze2.demo_Module1.getKuafu());
		Infinite.App.clearDbTable(App.Instance.demo_Module1.getKuafu());

		if (registeredOnAppInstance.compareAndSet(false, true))
			App.Instance.Zeze.getOnz().register(ProcName, TestOnzRedoPreparingWindow::kuaFu, BKuafu.class, BKuafuResult.class);
		zeze2.Zeze.getOnz().register(ProcName, TestOnzRedoPreparingWindow::kuaFu, BKuafu.class, BKuafuResult.class);

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

	/** 参与方执行完毕进入ready等待后，把协调者挂在pendingAsync窗口（ePreparing已落盘、commit未覆盖）。 */
	private static class PendingBlockTransaction extends OnzTransaction<BKuafu.Data, BKuafuResult.Data> {
		final CountDownLatch release = new CountDownLatch(1);

		@Override
		protected long perform() throws Exception {
			var arg = new BKuafu.Data();
			arg.setAccount(100);
			arg.setMoney(10);
			super.callProcedureAsync("zeze1", ProcName, arg, new BKuafuResult.Data()).get();
			setPendingAsync(true);
			var finisher = new Thread(() -> {
				try {
					release.await();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				setPendingAsync(false);
			});
			finisher.setDaemon(true);
			finisher.start();
			return 0;
		}
	}

	@Test
	@Timeout(120)
	public void testRedoSkipsInflightPreparing() throws Exception {
		waitOnzReady();
		var txn = new PendingBlockTransaction();
		txn.setOnzServer(onzServer);
		txn.setFlushTimeout(60_000); // 参与方ready等待不在本场景窗口内自愈（FND5-45独立覆盖）
		var performRc = new long[1];
		var coordinator = new Thread(() -> performRc[0] = onzServer.perform(txn));
		coordinator.setDaemon(true);
		coordinator.start();

		var commitIndex = tableOf("commitIndex");
		waitUntil(() -> count(commitIndex) == 1, 10_000, "ePreparing未落盘");
		try {
			// FND5-44核心：窗口内驱动redoTimer，不得误伤进行中事务。
			invokeRedoTimer();
			Assertions.assertEquals(1, count(commitIndex), "进行中的ePreparing不得被redo清除");

			// FND6-36钉死新契约：窗口内即使把时戳改写为超龄，redoTimer也不得清除该条目
			// ——登记所有权门控优先于年龄闸，年龄不再是存活事务的判据。
			var agedKey = firstKey(commitIndex);
			var aged = ByteBuffer.Allocate();
			aged.WriteUInt(AbstractOnz.ePreparing);
			aged.WriteLong8BE(System.currentTimeMillis() - 121_000);
			commitIndex.put(agedKey, java.util.Arrays.copyOf(aged.Bytes, aged.WriteIndex));
			invokeRedoTimer();
			Assertions.assertEquals(1, count(commitIndex), "超龄改写不得清除存活登记的ePreparing（所有权门控优先于年龄闸）");

			txn.release.countDown();
			coordinator.join(30_000);
			Assertions.assertEquals(0, performRc[0], "perform必须成功");
			waitMoney(App.Instance, 100, 10, 10_000,
					"参与方必须真正提交（修复前：窗口内被Rollback误伤回滚，协调者仍报成功）");
			Assertions.assertEquals(0, count(commitIndex), "事务完成后索引清理");
		} finally {
			// 断言失败也释放协调者（未来回归红态时不泄漏挂起线程）。
			txn.release.countDown();
			coordinator.join(10_000);
		}
	}

	@Test
	@Timeout(120)
	public void testLegacyRewriteSkippedWhileRegistered() throws Exception {
		waitOnzReady();
		var txn = new PendingBlockTransaction();
		txn.setOnzServer(onzServer);
		txn.setFlushTimeout(60_000);
		var performRc = new long[1];
		var coordinator = new Thread(() -> performRc[0] = onzServer.perform(txn));
		coordinator.setDaemon(true);
		coordinator.start();

		var commitIndex = tableOf("commitIndex");
		waitUntil(() -> count(commitIndex) == 1, 10_000, "ePreparing未落盘");

		// 改写为旧格式值（仅state，无时戳）：FND5-44年龄闸视为年龄无穷立即redo；
		// FND6-36后存活登记优先——redo跳过，协调者正常提交。
		var key = firstKey(commitIndex);
		var legacy = ByteBuffer.Allocate();
		legacy.WriteUInt(AbstractOnz.ePreparing);
		commitIndex.put(key, java.util.Arrays.copyOf(legacy.Bytes, legacy.WriteIndex));

		try {
			invokeRedoTimer();
			Assertions.assertEquals(1, count(commitIndex), "旧格式改写也不得回滚存活登记的ePreparing（所有权门控优先于格式）");

			txn.release.countDown();
			coordinator.join(30_000);
			Assertions.assertEquals(0, performRc[0], "perform必须成功");
			waitMoney(App.Instance, 100, 10, 10_000,
					"redo跳过后参与方必须真正提交，金额保持业务值（旧契约在此断言0，已被2759bfd废除）");
			Assertions.assertEquals(0, count(commitIndex), "事务完成后索引清理");
		} finally {
			// 断言失败也释放协调者（未来回归红态时不泄漏挂起线程）。
			txn.release.countDown();
			coordinator.join(10_000);
		}
	}

	@Test
	@Timeout(120)
	public void testAgedRewriteSkippedWhileRegistered() throws Exception {
		waitOnzReady();
		var txn = new PendingBlockTransaction();
		txn.setOnzServer(onzServer);
		txn.setFlushTimeout(60_000);
		var performRc = new long[1];
		var coordinator = new Thread(() -> performRc[0] = onzServer.perform(txn));
		coordinator.setDaemon(true);
		coordinator.start();

		var commitIndex = tableOf("commitIndex");
		waitUntil(() -> count(commitIndex) == 1, 10_000, "ePreparing未落盘");

		// 改写时戳为超龄：FND5-44年龄闸下是崩溃残留的redo恢复路径；
		// FND6-36后存活登记优先——redo跳过，协调者正常提交。
		var key = firstKey(commitIndex);
		var aged = ByteBuffer.Allocate();
		aged.WriteUInt(AbstractOnz.ePreparing);
		aged.WriteLong8BE(System.currentTimeMillis() - 121_000);
		commitIndex.put(key, java.util.Arrays.copyOf(aged.Bytes, aged.WriteIndex));

		try {
			invokeRedoTimer();
			Assertions.assertEquals(1, count(commitIndex), "超龄改写也不得回滚存活登记的ePreparing（所有权门控优先于年龄闸）");

			txn.release.countDown();
			coordinator.join(30_000);
			Assertions.assertEquals(0, performRc[0], "perform必须成功");
			waitMoney(App.Instance, 100, 10, 10_000,
					"redo跳过后参与方必须真正提交，金额保持业务值（旧契约在此断言0，已被2759bfd废除）");
			Assertions.assertEquals(0, count(commitIndex), "事务完成后索引清理");
		} finally {
			// 断言失败也释放协调者（未来回归红态时不泄漏挂起线程）。
			txn.release.countDown();
			coordinator.join(10_000);
		}
	}

	@Test
	@Timeout(120)
	public void testOrphanPreparingStillRedone() throws Exception {
		waitOnzReady();
		var commitIndex = tableOf("commitIndex");
		var commitPoint = tableOf("commitPoint");

		// 真孤儿：不经perform直接向两表写入崩溃残留（协调者死亡重启后登记表为空）。
		// 参与方按集群名持久化（FND4-90起），超龄时戳模拟残留到崩溃重启后的下一轮redo。
		var orphanTid = 0x5CA1BEEF00000001L;
		var key = new byte[8];
		ByteBuffer.longBeHandler.set(key, 0, orphanTid);
		var saved = new BSavedCommits.Data();
		saved.getOnzs().add("zeze1");
		var bbState = ByteBuffer.Allocate();
		saved.encode(bbState);
		commitPoint.put(key, java.util.Arrays.copyOf(bbState.Bytes, bbState.WriteIndex));
		var aged = ByteBuffer.Allocate();
		aged.WriteUInt(AbstractOnz.ePreparing);
		aged.WriteLong8BE(System.currentTimeMillis() - 121_000);
		commitIndex.put(key, java.util.Arrays.copyOf(aged.Bytes, aged.WriteIndex));
		Assertions.assertEquals(1, count(commitIndex), "孤儿条目写入前置");

		// 无存活登记：所有权门控放行，redo必须回滚（向参与方重发Rollback并清理两表）。
		invokeRedoTimer();

		Assertions.assertEquals(0, count(commitIndex), "孤儿ePreparing必须被redo回滚清理（崩溃残留恢复路径保留）");
		Assertions.assertEquals(0, count(commitPoint), "孤儿commitPoint必须随索引一起清理（FND4-88两表同生命周期）");
	}

	private static long getMoney(App app, long account) throws Exception {
		var holder = new long[1];
		app.Zeze.newProcedure(() -> {
			holder[0] = app.demo_Module1.getKuafu().getOrAdd(account).getMoney();
			return 0;
		}, "TestOnzRedoPreparingWindow.getMoney").call();
		return holder[0];
	}

	/** 参与方本地提交/回滚与协调者perform返回并发，金额断言轮询到稳定期望值。 */
	private static void waitMoney(App app, long account, long expected,
								   long timeoutMs, String message) throws Exception {
		long deadline = System.currentTimeMillis() + timeoutMs;
		while (getMoney(app, account) != expected) {
			Assertions.assertTrue(System.currentTimeMillis() < deadline, message
					+ "（期望=" + expected + " 实际=" + getMoney(app, account) + "）");
			//noinspection BusyWait
			Thread.sleep(50);
		}
	}

	private void invokeRedoTimer() throws Exception {
		var m = OnzServer.class.getDeclaredMethod("redoTimer");
		m.setAccessible(true);
		m.invoke(onzServer);
	}

	@SuppressWarnings("unchecked")
	private RocksDatabase.Table tableOf(String fieldName) throws Exception {
		Field f = OnzServer.class.getDeclaredField(fieldName);
		f.setAccessible(true);
		return (RocksDatabase.Table)f.get(onzServer);
	}

	private static byte[] firstKey(RocksDatabase.Table table) throws Exception {
		try (var it = table.iterator()) {
			it.seekToFirst();
			Assertions.assertTrue(it.isValid(), "表必须非空");
			return it.key();
		}
	}

	private static long count(RocksDatabase.Table table) throws Exception {
		long n = 0;
		try (var it = table.iterator()) {
			for (it.seekToFirst(); it.isValid(); it.next())
				n++;
		}
		return n;
	}

	private interface Condition {
		boolean test() throws Exception;
	}

	private static void waitUntil(Condition condition, long timeoutMs, String message) throws Exception {
		long deadline = System.currentTimeMillis() + timeoutMs;
		while (!condition.test()) {
			Assertions.assertTrue(System.currentTimeMillis() < deadline, message);
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
