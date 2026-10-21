package Zeze.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import static org.junit.jupiter.api.Assertions.*;

@Fast
@Isolated
public class TestAsyncLockShutdownHandoff {
	@AfterEach
	void restorePools() throws InterruptedException {
		stopPools();
		Task.tryInitThreadPool();
	}

	private static void stopPools() throws InterruptedException {
		try {
			Task.shutdownNow(200);
		} catch (TimeoutException ignored) {
			// 其他用例遗留任务可能未及时终止；Task 的池引用已清空。
		}
	}

	@Test
	void acceptedHandoffSurvivesShutdownNow() throws Exception {
		verifyDiscardedHandoff(false);
	}

	@Test
	void wrappedHandoffSurvivesShutdownNow() throws Exception {
		verifyDiscardedHandoff(true);
	}

	private static void verifyDiscardedHandoff(boolean wrap) throws Exception {
		stopPools();
		var accepted = new ArrayList<Runnable>();
		var pool = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>()) {
			@Override
			public void execute(Runnable command) {
				accepted.add(command);
				super.execute(wrap ? () -> command.run() : command);
			}
		};
		Task.initThreadPool(pool, Executors.newSingleThreadScheduledExecutor());
		var busy = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		var lock = new AsyncLock();
		var order = new ArrayList<Integer>();
		try {
			pool.execute(() -> {
				busy.countDown();
				try {
					release.await();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			});
			assertTrue(busy.await(5, TimeUnit.SECONDS));
			lock.enter(() -> {
				lock.enter(() -> order.add(1));
				lock.enter(() -> order.add(2));
			});
			assertEquals(1, pool.getQueue().size(), "交接任务已接收且尚未执行");
			assertTrue(order.isEmpty());
			Task.shutdownNow(5_000);
			assertEquals(List.of(1, 2), order, "停机丢弃的交接必须延续已有内联兜底语义");
			assertFalse(lock.isLocked(), "不得保留无人负责的派发权");

			// 模拟执行器迟到调用旧驱动：停机补偿与真实执行只能有一方赢得认领。
			accepted.get(1).run();
			assertEquals(List.of(1, 2), order);
			Task.tryInitThreadPool();
			lock.enter(() -> order.add(3));
			assertEquals(List.of(1, 2, 3), order, "重建池后同一个锁可继续使用");
		} finally {
			release.countDown();
			pool.shutdownNow();
			assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
		}
	}

	@Test
	void runningHandoffIsNotReplayedByShutdown() throws Exception {
		stopPools();
		var pool = Executors.newSingleThreadExecutor();
		Task.initThreadPool(pool, Executors.newSingleThreadScheduledExecutor());
		var entered = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		var finished = new CountDownLatch(1);
		var runs = new AtomicInteger();
		var lock = new AsyncLock();
		try {
			lock.enter(() -> lock.enter(() -> {
				runs.incrementAndGet();
				entered.countDown();
				for (;;) {
					try {
						release.await();
						break;
					} catch (InterruptedException ignored) {
						// 控制停机期间回调仍在执行，不能将它当作未启动驱动补偿。
					}
				}
				lock.leave();
				finished.countDown();
			}));
			assertTrue(entered.await(5, TimeUnit.SECONDS));
			assertThrows(TimeoutException.class, () -> Task.shutdownNow(10));
			assertEquals(1, runs.get());
			assertTrue(lock.isLocked());
			release.countDown();
			assertTrue(finished.await(5, TimeUnit.SECONDS));
			assertFalse(lock.isLocked());
		} finally {
			release.countDown();
			pool.shutdownNow();
			assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
		}
	}
}
