package Zeze.Arch;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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
		context.getFuture().OnAllDone(done -> {
			callbackCalls.incrementAndGet();
			throw new IllegalStateException("completion callback failed");
		});

		var returned = new CountDownLatch(1);
		var waiterFailure = new AtomicReference<Throwable>();
		var waiter = new Thread(() -> {
			try {
				context.getFuture().await();
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
}
