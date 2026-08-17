package UnitTest.Zeze.Util;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Util.OutObject;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
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
	public void testOfActionRunUnsafeDirect() throws Exception {
		var threadId = new AtomicLong();
		var future = TaskSpec.ofAction(() -> threadId.set(Thread.currentThread().getId()))
				.mode(DispatchMode.Direct).runUnsafe();
		Assert.assertTrue(future.isDone());
		Assert.assertEquals(Thread.currentThread().getId(), threadId.get());

		// Direct 分支异常通过 Future 传播（TaskCompletionSource.get 抛 CompletionException）
		var futureEx = TaskSpec.<Object>ofAction(() -> {
			throw new IllegalStateException("direct");
		}).mode(DispatchMode.Direct).runUnsafe();
		try {
			futureEx.get(1, TimeUnit.SECONDS);
			Assert.fail();
		} catch (java.util.concurrent.CompletionException e) {
			Assert.assertTrue(e.getCause() instanceof IllegalStateException);
		}
	}

	@Test
	public void testOfActionExecuteUnsafeDirect() {
		var count = new AtomicInteger();
		TaskSpec.ofAction(count::incrementAndGet).mode(DispatchMode.Direct).executeUnsafe();
		Assert.assertEquals(1, count.get()); // Direct 立即在当前线程执行
	}

	@Test
	public void testOfActionRunUnsafeCritical() throws Exception {
		var threadName = new AtomicReference<String>();
		var future = TaskSpec.ofAction(() -> threadName.set(Thread.currentThread().getName()))
				.name("testOfActionRunUnsafeCritical").mode(DispatchMode.Critical).runUnsafe();
		future.get(10, TimeUnit.SECONDS);
		Assert.assertNotNull(threadName.get());
		Assert.assertTrue(threadName.get(), threadName.get().startsWith("ZezeCriticalPool"));
	}

	@Test
	public void testOfActionExecuteUnsafe() throws Exception {
		var done = new TaskCompletionSource<Long>();
		TaskSpec.ofAction(() -> done.setResult(Thread.currentThread().getId()))
				.name("testOfActionExecuteUnsafe").executeUnsafe();
		long poolThreadId = done.get(10, TimeUnit.SECONDS);
		Assert.assertNotEquals(Thread.currentThread().getId(), poolThreadId);
	}

	@Test
	public void testOfActionScheduleUnsafe() throws Exception {
		var done = new TaskCompletionSource<Boolean>();
		ScheduledFuture<?> future = TaskSpec.ofAction(() -> done.setResult(true))
				.name("testOfActionScheduleUnsafe")
				.scheduleUnsafe(50);
		Assert.assertFalse(future.isDone());
		Assert.assertTrue(done.get(10, TimeUnit.SECONDS));
	}

	@Test
	public void testOfActionSchedule() throws Exception {
		// 事务外：与 scheduleUnsafe 等价
		var done = new TaskCompletionSource<Boolean>();
		TaskSpec.ofAction(() -> done.setResult(true)).name("testOfActionSchedule").schedule(10);
		Assert.assertTrue(done.get(10, TimeUnit.SECONDS));
	}

	@Test
	public void testOfActionScheduleWithPeriodUnsafeCancel() throws Exception {
		var count = new AtomicInteger();
		var future = TaskSpec.ofAction(count::incrementAndGet)
				.name("testOfActionScheduleWithPeriodUnsafeCancel")
				.scheduleWithPeriodUnsafe(10, 50);
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
	public void testOfActionScheduleAtUnsafe() throws Exception {
		var now = java.util.Calendar.getInstance();
		var future = TaskSpec.ofAction(Assert::fail)
				.name("testOfActionScheduleAtUnsafe")
				.period(60_000)
				.scheduleAtUnsafe(now.get(java.util.Calendar.HOUR_OF_DAY),
						(now.get(java.util.Calendar.MINUTE) + 1) % 60);
		Assert.assertFalse(future.isDone());
		Assert.assertTrue(future.cancel(false));
		Assert.assertTrue(future.isCancelled());
	}

	@Test
	public void testOfFuncCall() {
		Assert.assertEquals(123L, TaskSpec.ofFunc(() -> 123L).name("testOfFuncCall").call());
		// 异常返回 Procedure.Exception
		Assert.assertEquals(Procedure.Exception, TaskSpec.ofFunc(() -> {
			throw new IllegalStateException("testOfFuncCall");
		}).call());
	}

	@Test
	public void testOfFuncErrorHandle() throws Exception {
		var handled = new AtomicLong(-1);
		// p == null => isRequestSaved，结果非0时回调 errorHandle
		long r = TaskSpec.ofFunc(() -> 1L).errorHandle((p, code) -> handled.set(code)).call();
		Assert.assertEquals(1L, r);
		Assert.assertEquals(1L, handled.get());
		// 结果为 0 不回调
		handled.set(-1);
		Assert.assertEquals(0L, TaskSpec.ofFunc(() -> 0L).errorHandle((p, code) -> handled.set(code)).call());
		Assert.assertEquals(-1L, handled.get());
	}

	@Test
	public void testOfFuncRunUnsafeDirect() throws Exception {
		var future = TaskSpec.ofFunc(() -> 456L).mode(DispatchMode.Direct).runUnsafe();
		Assert.assertEquals(456L, (long)future.get(1, TimeUnit.SECONDS));
	}

	@Test
	public void testOfFuncExecuteUnsafe() throws Exception {
		var done = new TaskCompletionSource<Boolean>();
		TaskSpec.ofFunc(() -> {
			done.setResult(true);
			return 0L;
		}).name("testOfFuncExecuteUnsafe").executeUnsafe();
		Assert.assertTrue(done.get(10, TimeUnit.SECONDS));
	}

	@Test
	public void testOfFunc0ScheduleUnsafe() throws Exception {
		Future<String> future = TaskSpec.<String>ofFunc0(() -> "ok").scheduleUnsafe(10);
		Assert.assertEquals("ok", future.get(10, TimeUnit.SECONDS));

		// 异常经 Future 传播
		Future<Long> futureEx = TaskSpec.<Long>ofFunc0(() -> {
			throw new IllegalStateException("ofFunc0");
		}).scheduleUnsafe(10);
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
		Assert.assertEquals(0L, TaskSpec.ofProcedure(
				App.Instance.Zeze.newProcedure(() -> 0L, "TestTaskSpec.ofProcedure.call")).call());

		var future = TaskSpec.ofProcedure(
				App.Instance.Zeze.newProcedure(() -> 0L, "TestTaskSpec.ofProcedure.runUnsafe")).runUnsafe();
		Assert.assertEquals(0L, (long)future.get(10, TimeUnit.SECONDS));

		// outProtocol 分支（value 未被过程设置时 from 为 null）
		var out = new OutObject<Zeze.Net.Protocol<?>>();
		Assert.assertEquals(0L, TaskSpec.ofProcedure(
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

	public static void main(String[] args) throws Exception {
		Task.tryInitThreadPool();
		var test = new TestTaskSpec();
		test.testOfActionCall();
		test.testOfActionRunUnsafeDirect();
		test.testOfActionExecuteUnsafeDirect();
		test.testOfActionRunUnsafeCritical();
		test.testOfActionExecuteUnsafe();
		test.testOfActionScheduleUnsafe();
		test.testOfActionSchedule();
		test.testOfActionScheduleWithPeriodUnsafeCancel();
		test.testOfActionScheduleAtUnsafe();
		test.testOfFuncCall();
		test.testOfFuncErrorHandle();
		test.testOfFuncRunUnsafeDirect();
		test.testOfFuncExecuteUnsafe();
		test.testOfFunc0ScheduleUnsafe();
		test.testOfProcedure();
		test.testRunDeferInTransaction();
		test.testOfProcedureRunDeferInTransaction();
		System.out.println("TestTaskSpec OK");
	}
}
