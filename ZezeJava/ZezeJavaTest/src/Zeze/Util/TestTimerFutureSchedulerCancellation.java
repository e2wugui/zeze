package Zeze.Util;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Fast
public class TestTimerFutureSchedulerCancellation {
	@Test
	public void schedulerShutdownIsVisibleAsCancellation() {
		try (var scheduler = new ScheduledThreadPoolExecutor(1)) {
			var future = new TimerFuture<Long>("periodic", 1000);
			future.setFuture(scheduler.scheduleWithFixedDelay(() -> { }, 1, 1, TimeUnit.DAYS));
			scheduler.shutdown();
			assertTrue(future.isDone());
			assertThrows(CancellationException.class, future::get);
			assertTrue(future.isCancelled(), "底层调度被取消后，包装Future也必须报告取消");
		}
	}

	@Test
	@Timeout(15)
	public void cancelInterruptsAndDrainsAdmittedBody() throws Exception {
		Task.tryInitThreadPool();
		var entered = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		var interrupted = new CountDownLatch(1);
		var exited = new CountDownLatch(1);
		var canceled = new CountDownLatch(1);
		var failure = new AtomicReference<@Nullable Throwable>();
		var timer = TaskSpec.ofAction(() -> {
			entered.countDown();
			boolean restore = false;
			try {
				for (;;) {
					try {
						release.await();
						break;
					} catch (InterruptedException e) {
						restore = true;
						interrupted.countDown();
					}
				}
			} finally {
				if (restore)
					Thread.currentThread().interrupt();
				exited.countDown();
			}
		}).name("UnitTest.TimerFuture.cancelDrain").schedulePeriodNow(0, 60_000);

		Thread canceller = null;
		try {
			assertTrue(entered.await(5, TimeUnit.SECONDS), "周期任务体必须启动");
			canceller = Thread.ofPlatform().daemon().name("UnitTest.TimerFuture.canceller").start(() -> {
				try {
					timer.cancel(true);
				} catch (Throwable e) {
					failure.set(e);
				} finally {
					canceled.countDown();
				}
			});
			assertTrue(interrupted.await(5, TimeUnit.SECONDS), "cancel(true)必须在等待排空前先中断在飞任务体");
			var observedCanceller = canceller;
			awaitCondition(() -> canceled.getCount() == 0 || timer.hasQueuedThread(observedCanceller),
					"cancel线程必须进入TimerFuture锁排队等待在飞任务体");
			assertEquals(1, canceled.getCount(), "任务体退出前cancel不得提前返回");
			release.countDown();
			assertTrue(canceled.await(5, TimeUnit.SECONDS), "任务体释放后cancel必须返回");
			assertEquals(0, exited.getCount(), "cancel返回必须在在飞任务体退出之后");
		} finally {
			release.countDown();
			if (canceller == null)
				timer.cancel(true);
			else {
				canceller.join(5_000);
				assertTrue(!canceller.isAlive(), "cancel线程不得残留");
			}
			assertNull(failure.get(), "cancel不得抛异常");
		}
	}

	private static void awaitCondition(BooleanSupplier condition, String message) throws InterruptedException {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while (!condition.getAsBoolean()) {
			assertTrue(System.nanoTime() < deadline, message);
			Thread.sleep(1);
		}
	}
}
