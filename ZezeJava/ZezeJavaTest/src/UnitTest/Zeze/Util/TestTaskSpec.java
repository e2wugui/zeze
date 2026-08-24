package UnitTest.Zeze.Util;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.Net.ProtocolDispatch;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Util.OutObject;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import Zeze.Util.TaskOneByOneByKey;
import Zeze.Util.TaskOneByOneByKey2;
import Zeze.Util.TaskOneByOneByKeyLru;
import Zeze.Util.TaskSpec;
import demo.App;
import org.junit.Assert;
import org.junit.Test;

public class TestTaskSpec {
	@org.junit.Before
	public void before() {
		Task.tryInitThreadPool();
	}

	@Test
	public void testOfActionCall() {
		var count = new AtomicInteger();
		TaskSpec.ofAction(count::incrementAndGet).name("testOfActionCall").call();
		Assert.assertEquals(1, count.get());
		// call 吞掉异常
		TaskSpec.ofAction(() -> {
			throw new IllegalStateException("testOfActionCall");
		}).call();
		Assert.assertEquals(1, count.get());
	}

	@Test
	public void testOfActionSubmitNowDirect() throws Exception {
		var threadId = new AtomicLong();
		var future = TaskSpec.ofAction(() -> threadId.set(Thread.currentThread().getId()))
				.dispatchMode(DispatchMode.Direct).submitNow();
		Assert.assertTrue(future.isDone());
		Assert.assertEquals(Thread.currentThread().getId(), threadId.get());

		// Direct 分支异常通过 Future 传播（TaskCompletionSource.get 抛 CompletionException）
		var futureEx = TaskSpec.ofAction(() -> {
			throw new IllegalStateException("direct");
		}).dispatchMode(DispatchMode.Direct).submitNow();
		try {
			futureEx.get(1, TimeUnit.SECONDS);
			Assert.fail();
		} catch (java.util.concurrent.CompletionException e) {
			Assert.assertTrue(e.getCause() instanceof IllegalStateException);
		}
	}

	@Test
	public void testOfActionRunNowDirect() {
		var count = new AtomicInteger();
		TaskSpec.ofAction(count::incrementAndGet).dispatchMode(DispatchMode.Direct).runNow();
		Assert.assertEquals(1, count.get()); // Direct 立即在当前线程执行
	}

	@Test
	public void testOfActionSubmitNowCritical() throws Exception {
		var threadName = new AtomicReference<String>();
		var future = TaskSpec.ofAction(() -> threadName.set(Thread.currentThread().getName()))
				.name("testOfActionSubmitNowCritical").dispatchMode(DispatchMode.Critical).submitNow();
		future.get(10, TimeUnit.SECONDS);
		Assert.assertNotNull(threadName.get());
		Assert.assertTrue(threadName.get(), threadName.get().startsWith("ZezeCriticalPool"));
	}

	@Test
	public void testOfActionRunNow() throws Exception {
		var done = new TaskCompletionSource<Long>();
		TaskSpec.ofAction(() -> done.setResult(Thread.currentThread().getId()))
				.name("testOfActionRunNow").runNow();
		long poolThreadId = done.get(10, TimeUnit.SECONDS);
		Assert.assertNotEquals(Thread.currentThread().getId(), poolThreadId);
	}

	@Test
	public void testOfActionScheduleNow() throws Exception {
		var done = new TaskCompletionSource<Boolean>();
		ScheduledFuture<?> future = TaskSpec.ofAction(() -> done.setResult(true))
				.name("testOfActionScheduleNow")
				.scheduleNow(50);
		Assert.assertFalse(future.isDone());
		Assert.assertTrue(done.get(10, TimeUnit.SECONDS));
	}

	@Test
	public void testOfActionSchedule() throws Exception {
		// 事务外：与 scheduleNow 等价
		var done = new TaskCompletionSource<Boolean>();
		TaskSpec.ofAction(() -> done.setResult(true)).name("testOfActionSchedule").schedule(10);
		Assert.assertTrue(done.get(10, TimeUnit.SECONDS));
	}

	@Test
	public void testOfActionScheduleNowPeriodCancel() throws Exception {
		var count = new AtomicInteger();
		var future = TaskSpec.ofAction(count::incrementAndGet)
				.name("testOfActionScheduleNowPeriodCancel")
				.scheduleNow(10, 50);
		try {
			long begin = System.currentTimeMillis();
			while (count.get() < 3 && System.currentTimeMillis() - begin < 10_000)
				//noinspection BusyWait
				Thread.sleep(50);
			Assert.assertTrue(count.get() >= 3);
		} finally {
			future.cancel(false);
		}
		Assert.assertTrue(future.isCancelled());
		var countAfterCancel = count.get();
		Thread.sleep(200);
		Assert.assertEquals(countAfterCancel, count.get()); // cancel 之后不再执行
	}

	@Test
	public void testOfActionScheduleAtNow() {
		var now = java.util.Calendar.getInstance();
		var future = TaskSpec.ofAction(Assert::fail)
				.name("testOfActionScheduleAtNow")
				.scheduleAtNow(now.get(java.util.Calendar.HOUR_OF_DAY),
						(now.get(java.util.Calendar.MINUTE) + 1) % 60, 60_000);
		Assert.assertFalse(future.isDone());
		Assert.assertTrue(future.cancel(false));
		Assert.assertTrue(future.isCancelled());
	}

	@Test
	public void testOfFuncCall() {
		Assert.assertEquals(123L, (long)TaskSpec.ofFunc(() -> 123L).name("testOfFuncCall").call());
		// 异常返回 Procedure.Exception
		Assert.assertEquals(Procedure.Exception, (long)TaskSpec.ofFunc(() -> {
			throw new IllegalStateException("testOfFuncCall");
		}).call());
	}

	@Test
	public void testOfFuncErrorHandle() {
		var handled = new AtomicLong(-1);
		// p == null => isRequestSaved，结果非0时回调 errorHandle
		long r = ProtocolDispatch.ofFunc(() -> 1L, null).onError((p, code) -> handled.set(code)).call();
		Assert.assertEquals(1L, r);
		Assert.assertEquals(1L, handled.get());
		// 结果为 0 不回调
		handled.set(-1);
		Assert.assertEquals(0L, (long)ProtocolDispatch.ofFunc(() -> 0L, null).onError((p, code) -> handled.set(code)).call());
		Assert.assertEquals(-1L, handled.get());
	}

	@Test
	public void testOfFuncSubmitNowDirect() throws Exception {
		var future = TaskSpec.ofFunc(() -> 456L).dispatchMode(DispatchMode.Direct).submitNow();
		Assert.assertEquals(456L, (long)future.get(1, TimeUnit.SECONDS));
	}

	@Test
	public void testOfFuncRunNow() throws Exception {
		var done = new TaskCompletionSource<Boolean>();
		TaskSpec.ofFunc(() -> {
			done.setResult(true);
			return 0L;
		}).name("testOfFuncRunNow").runNow();
		Assert.assertTrue(done.get(10, TimeUnit.SECONDS));
	}

	@Test
	public void testOfFunc0ScheduleNow() throws Exception {
		Future<String> future = TaskSpec.ofFunc0(() -> "ok").scheduleNow(10);
		Assert.assertEquals("ok", future.get(10, TimeUnit.SECONDS));

		// 异常经 Future 传播
		Future<Long> futureEx = TaskSpec.<Long>ofFunc0(() -> {
			throw new IllegalStateException("ofFunc0");
		}).scheduleNow(10);
		try {
			futureEx.get(10, TimeUnit.SECONDS);
			Assert.fail();
		} catch (ExecutionException e) {
			Assert.assertTrue(e.getCause() instanceof IllegalStateException);
		}
	}

	@Test
	public void testOfFunc0SubmitNow() throws Exception {
		// ofFunc0 获得池调度动词（新增能力）：返回值与异常经 Future 传播
		Future<String> future = TaskSpec.ofFunc0(() -> "ok").submitNow();
		Assert.assertEquals("ok", future.get(10, TimeUnit.SECONDS));

		Future<Long> futureEx = TaskSpec.<Long>ofFunc0(() -> {
			throw new IllegalStateException("ofFunc0.submitNow");
		}).submitNow();
		try {
			futureEx.get(10, TimeUnit.SECONDS);
			Assert.fail();
		} catch (ExecutionException e) {
			Assert.assertTrue(e.getCause() instanceof IllegalStateException);
		}
	}

	@Test
	public void testOfProcedure() throws Exception {
		App.Instance.Start();
		Assert.assertEquals(0L, (long)TaskSpec.ofProcedure(
				App.Instance.Zeze.newProcedure(() -> 0L, "TestTaskSpec.ofProcedure.call")).call());

		var future = TaskSpec.ofProcedure(
				App.Instance.Zeze.newProcedure(() -> 0L, "TestTaskSpec.ofProcedure.submitNow")).submitNow();
		Assert.assertEquals(0L, (long)future.get(10, TimeUnit.SECONDS));

		// outProtocol 分支（value 未被过程设置时 from 为 null）
		var out = new OutObject<Zeze.Net.Protocol<?>>();
		Assert.assertEquals(0L, (long)ProtocolDispatch.ofProcedure(
				App.Instance.Zeze.newProcedure(() -> 0L, "TestTaskSpec.ofProcedure.outProtocol"))
				.outProtocol(out).call());
		Assert.assertNull(out.value);
	}

	@Test
	public void testRunDeferInTransaction() throws Exception {
		App.Instance.Start();
		var order = new ArrayList<String>();
		var txnThread = new AtomicLong();
		var deferredThread = new AtomicLong();
		var result = App.Instance.Zeze.newProcedure(() -> {
			// 运行中的事务内 run：应延迟到事务提交后异步执行
			txnThread.set(Thread.currentThread().getId());
			TaskSpec.ofAction(() -> {
				deferredThread.set(Thread.currentThread().getId());
				synchronized (order) {
					order.add("deferred");
				}
			}).name("testRunDeferInTransaction.deferred").run();
			Transaction.getCurrent().verifyRunning();
			synchronized (order) {
				order.add("inTxn");
			}
			return 0L;
		}, "testRunDeferInTransaction").call();
		Assert.assertEquals(0L, result);
		// 提交后 deferred 被异步执行
		long begin = System.currentTimeMillis();
		while (deferredThread.get() == 0 && System.currentTimeMillis() - begin < 10_000)
			//noinspection BusyWait
			Thread.sleep(20);
		synchronized (order) {
			Assert.assertEquals(java.util.List.of("inTxn", "deferred"), order);
		}
		Assert.assertNotEquals(txnThread.get(), deferredThread.get());
	}

	@Test
	public void testOfProcedureRunDeferInTransaction() throws Exception {
		App.Instance.Start();
		var called = new TaskCompletionSource<Boolean>();
		var result = App.Instance.Zeze.newProcedure(() -> {
			// 运行中的事务内 ofProcedure().run()：延迟到提交后执行
			TaskSpec.ofProcedure(App.Instance.Zeze.newProcedure(() -> {
				called.setResult(true);
				return 0L;
			}, "TestTaskSpec.deferredProc")).run();
			Assert.assertFalse(called.isDone());
			return 0L;
		}, "testOfProcedureRunDeferInTransaction").call();
		Assert.assertEquals(0L, result);
		Assert.assertTrue(called.get(10, TimeUnit.SECONDS));
	}

	// ========== fail-fast 校验 ==========

	@Test
	public void testConsumedSingleUse() {
		var spec = TaskSpec.ofAction(() -> {
		});
		spec.call();
		try {
			spec.name("x");
			Assert.fail();
		} catch (IllegalStateException ignored) {
		}
		try {
			spec.run();
			Assert.fail();
		} catch (IllegalStateException ignored) {
		}
	}

	@Test
	public void testCallRejectsAsyncOptions() {
		try {
			TaskSpec.ofAction(() -> {
			}).dispatchMode(DispatchMode.Critical).call();
			Assert.fail();
		} catch (IllegalArgumentException ignored) {
		}
		try {
			TaskSpec.ofAction(() -> {
			}).timeout(1000).call();
			Assert.fail();
		} catch (IllegalArgumentException ignored) {
		}
		try {
			TaskSpec.ofAction(() -> {
			}).onCancel(() -> {
			}).call();
			Assert.fail();
		} catch (IllegalArgumentException ignored) {
		}
	}

	@Test
	public void testScheduleRejectsDispatchMode() {
		try {
			TaskSpec.ofAction(() -> {
			}).dispatchMode(DispatchMode.Critical).scheduleNow(10);
			Assert.fail();
		} catch (IllegalArgumentException ignored) {
		}
		try {
			TaskSpec.ofAction(() -> {
			}).onCancel(() -> {
			}).schedule(10);
			Assert.fail();
		} catch (IllegalArgumentException ignored) {
		}
	}

	@Test
	public void testScheduleOfFuncAndProcedure() throws Exception {
		// FuncLong 载荷：单次触发的结果码经 Future 传播
		Assert.assertEquals(7L, (long)TaskSpec.ofFunc(() -> 7L).name("testScheduleFunc").scheduleNow(10).get());
		// 周期形态结果丢弃，仅验证不抛异常、确实触发
		var count = new AtomicLong();
		var period = TaskSpec.ofFunc(() -> {
			count.incrementAndGet();
			return 0L;
		}).scheduleNow(10, 30);
		try {
			for (int i = 0; i < 100 && count.get() < 2; i++)
				Thread.sleep(20);
			Assert.assertTrue(count.get() >= 2);
		} finally {
			period.cancel(true);
		}
		// Procedure 载荷
		App.Instance.Start();
		var procCount = new AtomicLong();
		TaskSpec.ofProcedure(App.Instance.Zeze.newProcedure(procCount::incrementAndGet,
				"TestTaskSpec.scheduleProc")).scheduleNow(10).get();
		Assert.assertEquals(1L, procCount.get());
	}

	@Test
	public void testOneByOneValidation() {
		var oo = new TaskOneByOneByKey();
		try {
			TaskSpec.ofAction(() -> {
			}).timeout(1000).executeOneByOne(1, oo);
			Assert.fail();
		} catch (IllegalArgumentException ignored) {
		}
	}

	// ========== executeOneByOne（原 TestOneByOneSpec） ==========

	/**
	 * 3 种 key 型的 ofAction 在 ByKey（base 家族）上提交并全部执行完。
	 */
	@Test
	public void testOneByOne3KeyTypes() throws Exception {
		var oo = new TaskOneByOneByKey();
		var counter = new AtomicInteger();
		for (int i = 0; i < 100; i++) {
			TaskSpec.ofAction(counter::incrementAndGet).executeOneByOne("objectKey", oo);
			TaskSpec.ofAction(counter::incrementAndGet).executeOneByOne(1, oo);
			TaskSpec.ofAction(counter::incrementAndGet).executeOneByOne(2L, oo);
		}
		awaitCounter(counter, 300);
	}

	/**
	 * 同 key 严格串行：非原子 int 自增不丢失。
	 */
	@Test
	public void testOneByOneSerialBySameKey() throws Exception {
		var oo = new TaskOneByOneByKey();
		var count = new int[1];
		var done = new TaskCompletionSource<Boolean>();
		for (int i = 0; i < 1000; i++)
			TaskSpec.ofAction(() -> count[0]++).executeOneByOne(1, oo);
		TaskSpec.ofAction(() -> done.setResult(true)).executeOneByOne(1, oo);
		Assert.assertTrue(done.get(10, TimeUnit.SECONDS));
		Assert.assertEquals(1000, count[0]);
	}

	/**
	 * Lru 实例（base 家族，Object key 装箱路径），多个 key 队列最终全部执行完。
	 */
	@Test
	public void testOneByOneByLru() throws Exception {
		var lru = new TaskOneByOneByKeyLru();
		var counter = new AtomicInteger();
		for (int i = 0; i < 100; i++)
			TaskSpec.ofAction(counter::incrementAndGet).executeOneByOne(Integer.toString(i % 7), lru);
		awaitCounter(counter, 100);
	}

	@Test
	public void testOneByOneOfFunc() throws Exception {
		var oo = new TaskOneByOneByKey();
		var counter = new AtomicInteger();
		for (int i = 0; i < 100; i++) {
			TaskSpec.ofFunc(() -> {
				counter.incrementAndGet();
				return 0L;
			}).executeOneByOne(1L, oo);
			TaskSpec.<Object>ofFunc0(() -> {
				counter.incrementAndGet();
				return null;
			}).executeOneByOne(2, oo);
		}
		TaskSpec.ofFunc(() -> {
			counter.incrementAndGet();
			return 0L;
		}).executeOneByOne("objectKey", oo);
		awaitCounter(counter, 201);
	}

	/**
	 * dispatchMode(Critical) 生效：任务在 critical 线程池执行。
	 */
	@Test
	public void testOneByOneModeCritical() throws Exception {
		var oo = new TaskOneByOneByKey();
		var threadName = new TaskCompletionSource<String>();
		TaskSpec.ofAction(() -> threadName.setResult(Thread.currentThread().getName()))
				.name("testOneByOneModeCritical").dispatchMode(DispatchMode.Critical)
				.executeOneByOne("criticalKey", oo);
		var name = threadName.get(10, TimeUnit.SECONDS);
		Assert.assertTrue(name, name.startsWith("ZezeCriticalPool"));
	}

	/**
	 * 任务名：默认 lambda 类名；显式 name 覆盖；Procedure 固定 getActionName。
	 * 用 no-op executor 的 ByKey 让任务停留在队列里，通过 toString 检查。
	 */
	@Test
	public void testOneByOneNameDefaultExplicitAndProcedure() throws Exception {
		App.Instance.Start();
		var oo = new TaskOneByOneByKey(2048, r -> {
		}); // no-op executor：任务入队后不执行
		var counter = new AtomicInteger();
		TaskSpec.ofAction(counter::incrementAndGet).executeOneByOne("k1", oo);
		TaskSpec.ofAction(counter::incrementAndGet).name("TestTaskSpec.OneByOneNamedAction").executeOneByOne("k2", oo);
		TaskSpec.ofFunc(() -> 0L).name("TestTaskSpec.OneByOneNamedFunc").executeOneByOne(3, oo);
		TaskSpec.ofProcedure(App.Instance.Zeze.newProcedure(() -> 0L, "TestTaskSpec.OneByOneProcActionName"))
				.executeOneByOne("k4", oo);
		var dump = oo.toString();
		Assert.assertTrue(dump, dump.contains("$Lambda")); // 默认任务名 = lambda 类名
		Assert.assertTrue(dump, dump.contains("TestTaskSpec.OneByOneNamedAction"));
		Assert.assertTrue(dump, dump.contains("TestTaskSpec.OneByOneNamedFunc"));
		Assert.assertTrue(dump, dump.contains("TestTaskSpec.OneByOneProcActionName")); // Procedure 固定 getActionName
	}

	/**
	 * onCancel + shutdown(true)：shutdown 之后提交的任务不再执行，onCancel 被同步回调。
	 */
	@Test
	public void testOneByOneOnCancelAndShutdown() {
		var oo = new TaskOneByOneByKey();
		oo.shutdown(true); // 空队列 shutdown，立即完成
		var ran = new AtomicInteger();
		var canceled = new AtomicInteger();
		TaskSpec.ofAction(ran::incrementAndGet)
				.onCancel(canceled::incrementAndGet)
				.executeOneByOne("k", oo); // shutdown 后提交：onCancel 由当前线程同步执行
		Assert.assertEquals(1, canceled.get());
		Assert.assertEquals(0, ran.get());
	}

	/**
	 * Key2：ofAction/ofFunc/ofFunc0/ofProcedure 提交执行、onCancel 拒绝、任务名（no-op executor 入队后 toString 检查）。
	 */
	@Test
	public void testOneByOneKey2() throws Exception {
		var oo = new TaskOneByOneByKey2();
		var counter = new AtomicInteger();
		for (int i = 0; i < 100; i++) {
			TaskSpec.ofAction(counter::incrementAndGet).executeOneByOne(1, oo);
			TaskSpec.<Object>ofFunc0(() -> {
				counter.incrementAndGet();
				return null;
			}).executeOneByOne(2L, oo);
			TaskSpec.ofAction(counter::incrementAndGet).executeOneByOne("k3", oo);
			TaskSpec.ofFunc(() -> {
				counter.incrementAndGet();
				return 0L;
			}).executeOneByOne(4, oo);
		}
		awaitCounter(counter, 400);

		// Key2 不支持 onCancel
		try {
			TaskSpec.ofAction(() -> {
			}).onCancel(() -> {
			}).executeOneByOne(1, oo);
			Assert.fail();
		} catch (IllegalArgumentException ignored) {
		}

		App.Instance.Start();
		var noop = new TaskOneByOneByKey2(1024, r -> {
		}); // no-op executor：任务入队后不执行
		TaskSpec.ofAction(counter::incrementAndGet).name("TestTaskSpec.Key2NamedAction").executeOneByOne("k1", noop);
		TaskSpec.ofFunc0(() -> 0).name("TestTaskSpec.Key2NamedFunc0").executeOneByOne(7, noop);
		TaskSpec.ofProcedure(App.Instance.Zeze.newProcedure(() -> 0L, "TestTaskSpec.Key2ProcActionName"))
				.executeOneByOne("k3", noop);
		var dump = noop.toString();
		Assert.assertTrue(dump, dump.contains("TestTaskSpec.Key2NamedAction"));
		Assert.assertTrue(dump, dump.contains("TestTaskSpec.Key2NamedFunc0"));
		Assert.assertTrue(dump, dump.contains("TestTaskSpec.Key2ProcActionName"));
	}

	/**
	 * 省略 queue：提交到全局静态 Task.getOneByOne()。
	 */
	@Test
	public void testOneByOneGlobal() throws Exception {
		var counter = new AtomicInteger();
		for (int i = 0; i < 100; i++)
			TaskSpec.ofAction(counter::incrementAndGet).executeOneByOne("globalKey");
		TaskSpec.ofFunc(() -> {
			counter.incrementAndGet();
			return 0L;
		}).executeOneByOne(1);
		awaitCounter(counter, 101);
	}

	/**
	 * int key 与 long key 走不装箱路径（含负数）。
	 */
	@Test
	public void testOneByOneIntLongKeyNegative() throws Exception {
		var oo = new TaskOneByOneByKey();
		var counter = new AtomicInteger();
		for (int i = 0; i < 100; i++) {
			TaskSpec.ofAction(counter::incrementAndGet).executeOneByOne(-1, oo);
			TaskSpec.ofAction(counter::incrementAndGet).executeOneByOne(0x1234_5678_9abcL, oo);
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
		var test = new TestTaskSpec();
		test.testOfActionCall();
		test.testOfActionSubmitNowDirect();
		test.testOfActionRunNowDirect();
		test.testOfActionSubmitNowCritical();
		test.testOfActionRunNow();
		test.testOfActionScheduleNow();
		test.testOfActionSchedule();
		test.testOfActionScheduleNowPeriodCancel();
		test.testOfActionScheduleAtNow();
		test.testOfFuncCall();
		test.testOfFuncErrorHandle();
		test.testOfFuncSubmitNowDirect();
		test.testOfFuncRunNow();
		test.testOfFunc0ScheduleNow();
		test.testOfFunc0SubmitNow();
		test.testOfProcedure();
		test.testRunDeferInTransaction();
		test.testOfProcedureRunDeferInTransaction();
		test.testConsumedSingleUse();
		test.testCallRejectsAsyncOptions();
		test.testScheduleRejectsDispatchMode();
		test.testOneByOneValidation();
		test.testOneByOne3KeyTypes();
		test.testOneByOneSerialBySameKey();
		test.testOneByOneByLru();
		test.testOneByOneOfFunc();
		test.testOneByOneModeCritical();
		test.testOneByOneNameDefaultExplicitAndProcedure();
		test.testOneByOneOnCancelAndShutdown();
		test.testOneByOneKey2();
		test.testOneByOneGlobal();
		test.testOneByOneIntLongKeyNegative();
		demo.App.Instance.Stop(); // App.Start 创建非守护线程，需要显式停止进程才能退出
		System.out.println("TestTaskSpec OK");
	}
}
