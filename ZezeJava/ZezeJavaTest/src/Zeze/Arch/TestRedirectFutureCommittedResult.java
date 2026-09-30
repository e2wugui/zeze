package Zeze.Arch;

import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.Application;
import Zeze.Config;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Transaction.TransactionLevel;
import demo.Module1.Table3;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestRedirectFutureCommittedResult {
	private static final AtomicInteger NEXT_SERVER_ID =
			new AtomicInteger(FastServerIds.TEST_REDIRECT_FUTURE_COMMITTED_RESULT);
	private static final long KEY = 1L;
	private final AtomicInteger attempts = new AtomicInteger();
	private Application app;
	private Table3 table;

	@BeforeEach
	public void startApp() throws Exception {
		var config = new Config();
		config.setServiceManager("disable");
		config.setTakeoverMode("off");
		config.setServerId(NEXT_SERVER_ID.getAndIncrement());
		config.setDefaultTableConf(new Config.TableConf());
		var database = new Config.DatabaseConf();
		database.setDatabaseUrl("redirect_committed_result_" + config.getServerId());
		config.getDatabaseConfMap().put("", database);
		app = new Application("RedirectFutureCommittedResult@" + config.getServerId(), config);
		app.setSchemas(new demo.Schemas());
		table = new Table3();
		app.addTable("", table);
		new ProviderApp(app); // runFuture只需真实Application；服务不启动，也无需外部SM。
		app.start();
		assertEquals(Procedure.Success, app.newProcedure(() -> {
			table.getOrAdd(KEY).setInt_1(0);
			return Procedure.Success;
		}, "RedirectFutureCommittedResult.prepare").call());
	}

	@AfterEach
	public void stopApp() throws Exception {
		if (app != null)
			app.stop();
	}

	@Test
	public void redoReturnsTheCommittedAttemptsValue() throws Exception {
		var future = runWithConcurrentUpdate(false);
		assertEquals(2, attempts.get(), "必须发生真实事务冲突并重做");
		assertEquals(1, future.get(5, TimeUnit.SECONDS), "返回最终提交轮次的值，不能返回已回滚首轮读到的0");
	}

	@Test
	public void failureAfterRedoCannotBeReportedAsAnEarlierSuccess() throws Exception {
		var future = runWithConcurrentUpdate(true);
		assertEquals(2, attempts.get(), "必须发生真实事务冲突并重做");
		var failure = assertThrows(CompletionException.class, () -> future.get(5, TimeUnit.SECONDS));
		assertTrue(future.isCompletedExceptionally(), "最终轮次失败必须覆盖已回滚轮次的候选成功值");
		assertInstanceOf(RedirectException.class, failure.getCause());
		assertEquals("committed attempt failed", failure.getCause().getCause().getMessage());
	}

	private RedirectFuture<Integer> runWithConcurrentUpdate(boolean failAfterRedo) throws Exception {
		var firstRead = new CountDownLatch(1);
		var updateCommitted = new CountDownLatch(1);
		var transactionFinished = new CountDownLatch(1);
		var updateFailure = new AtomicReference<Throwable>();
		var updater = new Thread(() -> {
			try {
				assertTrue(firstRead.await(5, TimeUnit.SECONDS), "redirect首轮必须读到记录");
				assertEquals(Procedure.Success, app.newProcedure(() -> {
					table.getOrAdd(KEY).setInt_1(1);
					return Procedure.Success;
				}, "RedirectFutureCommittedResult.concurrentUpdate").call());
			} catch (Throwable e) {
				updateFailure.set(e);
			} finally {
				updateCommitted.countDown();
			}
		}, "redirect-conflicting-update");
		updater.start();
		try {
			var future = app.redirect.runFuture(TransactionLevel.Serializable, () -> {
				int attempt = attempts.incrementAndGet();
				// runNow异步执行；只join冲突写者不能证明redirect事务已完成重做。
				// 重做不会执行这些终局回调，最终提交或回滚才发出完成信号。
				Transaction.whileCommit(transactionFinished::countDown);
				Transaction.whileRollback(transactionFinished::countDown);
				int value = table.getOrAdd(KEY).getInt_1();
				if (attempt == 1) {
					firstRead.countDown();
					assertTrue(updateCommitted.await(5, TimeUnit.SECONDS), "冲突事务必须先提交再结束首轮");
				} else if (failAfterRedo)
					throw new IllegalStateException("committed attempt failed");
				// 首轮返回已完成future后，真正的lockAndCheck才会发现读快照失效。
				return RedirectFuture.finish(value);
			}, "RedirectFutureCommittedResult.loopBack");
			updater.join(5_000);
			assertFalse(updater.isAlive(), "冲突写线程必须收尾");
			assertNull(updateFailure.get(), "冲突事务必须成功");
			assertTrue(transactionFinished.await(5, TimeUnit.SECONDS), "必须等待redirect事务最终提交或回滚");
			return future;
		} finally {
			firstRead.countDown();
			updater.interrupt();
			updater.join(5_000);
			assertFalse(updater.isAlive(), "测试结束不得留下冲突写线程");
		}
	}
}
