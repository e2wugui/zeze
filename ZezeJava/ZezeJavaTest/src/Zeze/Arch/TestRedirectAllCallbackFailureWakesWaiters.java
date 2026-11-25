package Zeze.Arch;

import static java.util.Objects.requireNonNull;

import org.jspecify.annotations.Nullable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.Builtin.ProviderDirect.BModuleRedirectAllHash;
import Zeze.Builtin.ProviderDirect.ModuleRedirectAllResult;
import Zeze.Config;
import Zeze.Net.Binary;
import Zeze.Net.Service;
import Zeze.Util.Task;
import Zeze.Transaction.Procedure;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestRedirectAllCallbackFailureWakesWaiters {

	@Test
	public void allDoneFailureStillReleasesAnExistingWaiter() throws Exception {
		Task.tryInitThreadPool();
		// 不启动Service、无Application：复现无数据库部署的真实回调分支。
		var service = new Service("RedirectAllCallbackFailureWakesWaiters", (Zeze.Application)null, new Config());
		var context = new RedirectAllContext<RedirectResult>(1, binary -> new RedirectResult());
		long sessionId = service.addManualContextWithTimeout(context, 30_000);
		var callbackCalls = new AtomicInteger();
		requireNonNull(context.getFuture()).OnAllDone(done -> {
			callbackCalls.incrementAndGet();
			throw new IllegalStateException("completion callback failed");
		});

		var returned = new CountDownLatch(1);
		var waiterFailure = new AtomicReference<@Nullable Throwable>();
		var waiter = new Thread(() -> {
			try {
				requireNonNull(context.getFuture()).await();
				returned.countDown();
			} catch (Throwable e) {
				waiterFailure.set(e);
			}
		}, "redirect-all-await");
		waiter.start();
		try {
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
			while (waiter.getState() != Thread.State.WAITING && System.nanoTime() < deadline)
				Thread.sleep(1);
			assertEquals(Thread.State.WAITING, waiter.getState(), "await必须先进入等待，避免完成后才调用的假通过");

			var response = new ModuleRedirectAllResult();
			response.Argument.getHashes().put(0, new BModuleRedirectAllHash.Data(Procedure.Success, Binary.Empty));
			context.processResult(response); // 收齐结果，真实移除上下文并触发onAllDone。

			assertEquals(1, callbackCalls.get());
			assertNull(service.tryGetManualContext(sessionId), "已完成的上下文必须移除");
			assertTrue(returned.await(2, TimeUnit.SECONDS), "完成回调抛异常后，已有await等待者仍必须被唤醒");
			assertNull(waiterFailure.get(), "await正常完成，不应以中断或回调异常收尾");
		} finally {
			service.tryRemoveManualContext(sessionId);
			waiter.interrupt(); // 红测试也必须清理等待线程。
			waiter.join(5_000);
			assertFalse(waiter.isAlive(), "测试结束不得留下等待线程");
		}
	}

	@Test
	public void awaitWaitsUntilOnResultAndOnAllDoneFinish() throws Exception {
		Task.tryInitThreadPool();
		var service = new Service("RedirectAllAwaitWaitsForCallbacks", (Zeze.Application)null, new Config());
		var context = new RedirectAllContext<RedirectResult>(1, binary -> new RedirectResult());
		long sessionId = service.addManualContextWithTimeout(context, 30_000);
		var onResultEntered = new CountDownLatch(1);
		var releaseOnResult = new CountDownLatch(1);
		var onResultFinished = new AtomicBoolean();
		var onAllDoneEntered = new CountDownLatch(1);
		var releaseOnAllDone = new CountDownLatch(1);
		var onAllDoneFinished = new AtomicBoolean();

		var future = requireNonNull(context.getFuture());
		future.OnResult(r -> {
			onResultEntered.countDown();
			assertTrue(releaseOnResult.await(5, TimeUnit.SECONDS));
			onResultFinished.set(true);
		});
		future.OnAllDone(c -> {
			onAllDoneEntered.countDown();
			assertTrue(releaseOnAllDone.await(5, TimeUnit.SECONDS));
			onAllDoneFinished.set(true);
		});

		var producerFailure = new AtomicReference<@Nullable Throwable>();
		var producer = new Thread(() -> {
			try {
				var response = new ModuleRedirectAllResult();
				response.Argument.getHashes().put(0, new BModuleRedirectAllHash.Data(Procedure.Success, Binary.Empty));
				context.processResult(response);
			} catch (Throwable e) {
				producerFailure.set(e);
			}
		}, "redirect-all-producer");

		var waiterReturned = new CountDownLatch(1);
		var waiterFailure = new AtomicReference<@Nullable Throwable>();
		var waiter = new Thread(() -> {
			try {
				future.await();
				waiterReturned.countDown();
			} catch (Throwable e) {
				waiterFailure.set(e);
			}
		}, "redirect-all-await-callbacks");

		producer.start();
		try {
			assertTrue(onResultEntered.await(5, TimeUnit.SECONDS));
			assertTrue(context.isCompleted(), "hashResults already reached concurrentLevel before callbacks finished");

			waiter.start();
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
			while (waiter.getState() != Thread.State.WAITING && System.nanoTime() < deadline)
				Thread.sleep(1);
			assertEquals(Thread.State.WAITING, waiter.getState(),
					"await() must block while onResult is still executing even though isCompleted() is true");
			assertEquals(1, waiterReturned.getCount());

			releaseOnResult.countDown();
			assertTrue(onAllDoneEntered.await(5, TimeUnit.SECONDS));
			assertTrue(onResultFinished.get());
			assertEquals(Thread.State.WAITING, waiter.getState(),
					"await() must still block while onAllDone is executing");
			assertEquals(1, waiterReturned.getCount());

			releaseOnAllDone.countDown();
			assertTrue(waiterReturned.await(5, TimeUnit.SECONDS), "await() must return once onAllDone finishes");
			producer.join(5_000);
			assertFalse(producer.isAlive());
			assertNull(producerFailure.get());
			assertNull(waiterFailure.get());
			assertTrue(onResultFinished.get());
			assertTrue(onAllDoneFinished.get());
		} finally {
			releaseOnResult.countDown();
			releaseOnAllDone.countDown();
			service.tryRemoveManualContext(sessionId);
			waiter.interrupt();
			producer.join(5_000);
			waiter.join(5_000);
			assertFalse(producer.isAlive());
			assertFalse(waiter.isAlive());
		}
	}
}
