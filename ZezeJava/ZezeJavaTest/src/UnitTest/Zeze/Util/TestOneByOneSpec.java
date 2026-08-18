package UnitTest.Zeze.Util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Transaction.DispatchMode;
import Zeze.Util.OneByOneSpec;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import Zeze.Util.TaskOneByOneByKey;
import Zeze.Util.TaskOneByOneByKey2;
import Zeze.Util.TaskOneByOneByKeyLru;
import demo.App;
import org.junit.Assert;
import org.junit.Test;

public class TestOneByOneSpec {
	@org.junit.Before
	public void before() {
		Task.tryInitThreadPool();
	}

	/**
	 * 3 种 key 型的 ofAction 在 ByKey（base 家族）上提交并全部执行完。
	 */
	@Test
	public void testOfActionByBase3KeyTypes() throws Exception {
		var oo = new TaskOneByOneByKey();
		var counter = new AtomicInteger();
		for (int i = 0; i < 100; i++) {
			OneByOneSpec.ofAction("objectKey", counter::incrementAndGet).execute(oo);
			OneByOneSpec.ofAction(1, counter::incrementAndGet).execute(oo);
			OneByOneSpec.ofAction(2L, counter::incrementAndGet).execute(oo);
		}
		awaitCounter(counter, 300);
	}

	/**
	 * 同 key 严格串行：非原子 int 自增不丢失。
	 */
	@Test
	public void testOfActionSerialBySameKey() throws Exception {
		var oo = new TaskOneByOneByKey();
		var count = new int[1];
		var done = new TaskCompletionSource<Boolean>();
		for (int i = 0; i < 1000; i++)
			OneByOneSpec.ofAction(1, () -> count[0]++).execute(oo);
		OneByOneSpec.ofAction(1, () -> done.setResult(true)).execute(oo);
		Assert.assertTrue(done.get(10, TimeUnit.SECONDS));
		Assert.assertEquals(1000, count[0]);
	}

	/**
	 * Lru 实例（base 家族，Object key 装箱路径），多个 key 队列最终全部执行完。
	 */
	@Test
	public void testOfActionByLru() throws Exception {
		var lru = new TaskOneByOneByKeyLru();
		var counter = new AtomicInteger();
		for (int i = 0; i < 100; i++)
			OneByOneSpec.ofAction(Integer.toString(i % 7), counter::incrementAndGet).execute(lru);
		awaitCounter(counter, 100);
	}

	@Test
	public void testOfFunc() throws Exception {
		var oo = new TaskOneByOneByKey();
		var counter = new AtomicInteger();
		for (int i = 0; i < 100; i++)
			OneByOneSpec.ofFunc(1L, () -> {
				counter.incrementAndGet();
				return 0L;
			}).execute(oo);
		OneByOneSpec.ofFunc("objectKey", () -> {
			counter.incrementAndGet();
			return 0L;
		}).execute(oo);
		awaitCounter(counter, 101);
	}

	/**
	 * mode(Critical) 生效：任务在 critical 线程池执行。
	 */
	@Test
	public void testModeCritical() throws Exception {
		var oo = new TaskOneByOneByKey();
		var threadName = new TaskCompletionSource<String>();
		OneByOneSpec.ofAction("criticalKey", () -> threadName.setResult(Thread.currentThread().getName()))
				.name("testModeCritical").mode(DispatchMode.Critical).execute(oo);
		var name = threadName.get(10, TimeUnit.SECONDS);
		Assert.assertTrue(name, name.startsWith("ZezeCriticalPool"));
	}

	/**
	 * 任务名：默认 lambda 类名；显式 name 覆盖；Procedure 固定 getActionName。
	 * 用 no-op executor 的 ByKey 让任务停留在队列里，通过 toString 检查。
	 */
	@Test
	public void testNameDefaultExplicitAndProcedure() throws Exception {
		App.Instance.Start();
		var oo = new TaskOneByOneByKey(2048, r -> {
		}); // no-op executor：任务入队后不执行
		var counter = new AtomicInteger();
		OneByOneSpec.ofAction("k1", counter::incrementAndGet).execute(oo);
		OneByOneSpec.ofAction("k2", counter::incrementAndGet).name("TestOneByOneSpec.NamedAction").execute(oo);
		OneByOneSpec.ofFunc(3, () -> 0L).name("TestOneByOneSpec.NamedFunc").execute(oo);
		OneByOneSpec.ofProcedure("k4",
				App.Instance.Zeze.newProcedure(() -> 0L, "TestOneByOneSpec.ProcActionName")).execute(oo);
		var dump = oo.toString();
		Assert.assertTrue(dump, dump.contains("$Lambda")); // 默认任务名 = lambda 类名
		Assert.assertTrue(dump, dump.contains("TestOneByOneSpec.NamedAction"));
		Assert.assertTrue(dump, dump.contains("TestOneByOneSpec.NamedFunc"));
		Assert.assertTrue(dump, dump.contains("TestOneByOneSpec.ProcActionName")); // Procedure 固定 getActionName
	}

	/**
	 * cancel + shutdown(true)：shutdown 之后提交的任务不再执行，cancel 被同步回调。
	 */
	@Test
	public void testCancelAndShutdown() throws Exception {
		var oo = new TaskOneByOneByKey();
		oo.shutdown(true); // 空队列 shutdown，立即完成
		var ran = new AtomicInteger();
		var canceled = new AtomicInteger();
		OneByOneSpec.ofAction("k", ran::incrementAndGet)
				.cancel(canceled::incrementAndGet)
				.execute(oo); // shutdown 后提交：cancel 由当前线程同步执行
		Assert.assertEquals(1, canceled.get());
		Assert.assertEquals(0, ran.get());
	}

	/**
	 * int key 与 long key 走不装箱路径且 (int)rawKey 取回无损（含负数）。
	 */
	/**
	 * Key2：ofAction/ofFunc0/ofProcedure 提交执行、cancel 拒绝、任务名（no-op executor 入队后 toString 检查）。
	 */
	@Test
	public void testKey2() throws Exception {
		var oo = new TaskOneByOneByKey2();
		var counter = new AtomicInteger();
		for (int i = 0; i < 100; i++) {
			OneByOneSpec.ofAction(1, counter::incrementAndGet).execute(oo);
			OneByOneSpec.<Object>ofFunc0(2L, () -> {
				counter.incrementAndGet();
				return null;
			}).execute(oo);
			OneByOneSpec.ofAction("k3", counter::incrementAndGet).execute(oo);
		}
		awaitCounter(counter, 300);

		// Key2 不支持 cancel
		try {
			OneByOneSpec.ofAction(1, () -> {
			}).cancel(() -> {
			}).execute(oo);
			Assert.fail();
		} catch (IllegalArgumentException ignored) {
		}

		App.Instance.Start();
		var noop = new TaskOneByOneByKey2(1024, r -> {
		}); // no-op executor：任务入队后不执行
		OneByOneSpec.ofAction("k1", counter::incrementAndGet).name("TestOneByOneSpec.Key2NamedAction").execute(noop);
		OneByOneSpec.ofFunc0(7, () -> 0).name("TestOneByOneSpec.Key2NamedFunc0").execute(noop);
		OneByOneSpec.ofProcedure("k3",
				App.Instance.Zeze.newProcedure(() -> 0L, "TestOneByOneSpec.Key2ProcActionName")).execute(noop);
		var dump = noop.toString();
		Assert.assertTrue(dump, dump.contains("TestOneByOneSpec.Key2NamedAction"));
		Assert.assertTrue(dump, dump.contains("TestOneByOneSpec.Key2NamedFunc0"));
		Assert.assertTrue(dump, dump.contains("TestOneByOneSpec.Key2ProcActionName"));
	}

	/**
	 * int key 与 long key 走不装箱路径且 (int)rawKey 取回无损（含负数）。
	 */
	@Test
	public void testIntLongKeyNegative() throws Exception {
		var oo = new TaskOneByOneByKey();
		var counter = new AtomicInteger();
		for (int i = 0; i < 100; i++) {
			OneByOneSpec.ofAction(-1, counter::incrementAndGet).execute(oo);
			OneByOneSpec.ofAction(0x1234_5678_9abcL, counter::incrementAndGet).execute(oo);
		}
		awaitCounter(counter, 200);
	}

	private static void awaitCounter(AtomicInteger counter, int expected) throws InterruptedException {
		long begin = System.currentTimeMillis();
		while (counter.get() < expected && System.currentTimeMillis() - begin < 10_000)
			//noinspection BusyWait
			Thread.sleep(10);
		Assert.assertEquals(expected, counter.get());
	}

	public static void main(String[] args) throws Exception {
		Task.tryInitThreadPool();
		var test = new TestOneByOneSpec();
		test.testOfActionByBase3KeyTypes();
		test.testOfActionSerialBySameKey();
		test.testOfActionByLru();
		test.testOfFunc();
		test.testModeCritical();
		test.testNameDefaultExplicitAndProcedure();
		test.testCancelAndShutdown();
		test.testKey2();
		test.testIntLongKeyNegative();
		demo.App.Instance.Stop(); // App.Start 创建非守护线程，需要显式停止进程才能退出
		System.out.println("TestOneByOneSpec OK");
	}
}
