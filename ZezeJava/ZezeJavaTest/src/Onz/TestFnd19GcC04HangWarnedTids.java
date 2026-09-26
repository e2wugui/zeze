package Onz;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.CountDownLatch;

import Zeze.Config;
import Zeze.Onz.AbstractOnz;
import Zeze.Onz.OnzServer;
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
 * FND19 GC-C04 回归：hangWarnedTids 只增不减——写入条件是"登记中（perform还活着，
 * 阻塞在waitPendingAsync）且ePreparing年龄≥240s"，慢而最终完成的合法长事务（可配大
 * flushTimeout的pendingAsync异步放大）tid被永久写入集合，与"集合有界于挂死perform数"
 * 的注释契约不符。修复：perform 的 finally 顺带回收tid，集合真正有界于挂死数。
 */
public class TestFnd19GcC04HangWarnedTids {
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

	@Test
	@Timeout(120)
	public void testHangWarnedTidsRemovedWhenPerformFinishes() throws Exception {
		waitOnzReady();

		var txn = new PendingBlockTransaction();
		txn.setOnzServer(onzServer);
		var performRc = new long[1];
		var coordinator = new Thread(() -> performRc[0] = onzServer.perform(txn));
		coordinator.setDaemon(true);
		coordinator.start();

		var commitIndex = tableOf("commitIndex");
		waitUntil(() -> count(commitIndex) == 1, 10_000, "ePreparing未落盘");

		// 改写时戳为超龄（≥2×RedoPreparingMinAgeMs=240s）：登记中+超龄 → hangWarnedTids.add+warn。
		var key = firstKey(commitIndex);
		var aged = ByteBuffer.Allocate();
		aged.WriteUInt(AbstractOnz.ePreparing);
		aged.WriteLong8BE(System.currentTimeMillis() - 300_000);
		commitIndex.put(key, java.util.Arrays.copyOf(aged.Bytes, aged.WriteIndex));

		invokeRedoTimer();
		Assertions.assertTrue(hangWarnedTids().contains(txn.getOnzTid()),
				"登记中超龄ePreparing必须触发过封锁告警（前置：确认触发路径，两种修复形态下都成立）");

		txn.release.countDown();
		coordinator.join(30_000);
		Assertions.assertEquals(0L, performRc[0], "perform必须成功");
		Assertions.assertFalse(hangWarnedTids().contains(txn.getOnzTid()),
				"完成的perform必须回收告警tid（修复前：只增不减，慢而最终完成的事务tid永久残留）");
		Assertions.assertEquals(0, count(commitIndex), "事务完成后索引清理");
	}

	/** 慢而最终完成的perform：无参与方，纯pendingAsync阻塞在waitPendingAsync（ePreparing已落盘）。 */
	private static class PendingBlockTransaction extends Zeze.Onz.OnzTransaction<BKuafu.Data, BKuafuResult.Data> {
		final CountDownLatch release = new CountDownLatch(1);

		@Override
		protected long perform() throws Exception {
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

	@SuppressWarnings("unchecked")
	private java.util.Set<Long> hangWarnedTids() throws Exception {
		var f = OnzServer.class.getDeclaredField("hangWarnedTids");
		f.setAccessible(true);
		return (java.util.Set<Long>)f.get(onzServer);
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
