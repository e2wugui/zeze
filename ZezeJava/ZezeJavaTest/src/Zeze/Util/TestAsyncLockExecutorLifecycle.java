package Zeze.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 执行器属于调用方；正常关闭保留已接受任务，强制关闭不再由锁补跑任务。 */
@Fast
public class TestAsyncLockExecutorLifecycle {
	@Test
	void orderlyShutdownCompletesAcceptedHandoff() throws Exception {
		verifyOrderlyShutdown(false);
	}

	@Test
	void orderlyShutdownCompletesWrappedHandoff() throws Exception {
		verifyOrderlyShutdown(true);
	}

	private static void verifyOrderlyShutdown(boolean wrap) throws Exception {
		var pool = newPool(wrap);
		var busy = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		var lock = new AsyncLock(pool);
		var order = new ArrayList<Integer>();
		try {
			occupyWorker(pool, busy, release);
			assertTrue(busy.await(5, TimeUnit.SECONDS));
			lock.enter(() -> {
				lock.enter(() -> order.add(1));
				lock.enter(() -> order.add(2));
			});
			assertEquals(1, pool.getQueue().size());
			assertTrue(order.isEmpty());
			assertFalse(pool.isShutdown(), "锁不管理共享执行器生命周期");
			pool.shutdown(); // 已接收的首个交接继续执行，后续投递被拒时循环兜底。
			release.countDown();
			assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
			assertEquals(List.of(1, 2), order);
			assertFalse(lock.isLocked());
		} finally {
			release.countDown();
			pool.shutdownNow();
			assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
		}
	}

	@Test
	void forcedShutdownDoesNotReplayDiscardedTasks() throws Exception {
		verifyForcedShutdown(false);
	}

	@Test
	void forcedShutdownDoesNotReplayDiscardedWrappedTasks() throws Exception {
		verifyForcedShutdown(true);
	}

	private static void verifyForcedShutdown(boolean wrap) throws Exception {
		var pool = newPool(wrap);
		var busy = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		var lock = new AsyncLock(pool);
		var runs = new AtomicInteger();
		try {
			occupyWorker(pool, busy, release);
			assertTrue(busy.await(5, TimeUnit.SECONDS));
			lock.enter(() -> lock.enter(runs::incrementAndGet));
			assertEquals(1, pool.getQueue().size());
			assertEquals(1, pool.shutdownNow().size());
			release.countDown();
			assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
			assertEquals(0, runs.get(), "强制丢弃由调用方处理，锁不能偷偷补跑业务");
			// 被强制中止的锁随所属业务一起丢弃，不要求重建池后恢复旧锁。
		} finally {
			release.countDown();
			pool.shutdownNow();
			assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
		}
	}

	private static ThreadPoolExecutor newPool(boolean wrap) {
		return new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>()) {
			@Override
			public void execute(Runnable command) {
				super.execute(wrap ? () -> command.run() : command);
			}
		};
	}

	private static void occupyWorker(ThreadPoolExecutor pool, CountDownLatch busy, CountDownLatch release) {
		pool.execute(() -> {
			busy.countDown();
			try {
				release.await();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});
	}
}
