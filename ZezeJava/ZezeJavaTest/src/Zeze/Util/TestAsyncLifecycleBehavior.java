package Zeze.Util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.*;

@Fast
@Timeout(10)
public class TestAsyncLifecycleBehavior {
	private static Object waitHead(TaskCompletionSource<?> source) throws Exception {
		var field = TaskCompletionSource.class.getDeclaredField("waitHead");
		field.setAccessible(true);
		return field.get(source);
	}

	private static void awaitWaiter(TaskCompletionSource<?> source) throws Exception {
		var started = System.nanoTime();
		while (waitHead(source) == null) {
			if (System.nanoTime() - started >= TimeUnit.SECONDS.toNanos(5))
				fail("waiter was not registered");
			Thread.yield();
		}
	}

	@Test
	public void testTimeoutAndInterruptReleaseWaitingReferences() throws Exception {
		var source = new TaskCompletionSource<Integer>();
		for (var i = 0; i < 100; ++i) {
			assertThrows(TimeoutException.class, () -> source.get(0, TimeUnit.NANOSECONDS));
			assertThrows(TimeoutException.class, () -> source.get(1, TimeUnit.NANOSECONDS));
		}
		assertNull(waitHead(source), "长期 pending Future 不得保留历史轮询者");
		var exit = new AtomicReference<Throwable>();
		var waiter = Thread.ofPlatform().daemon().start(() -> {
			try {
				source.get();
			} catch (Throwable e) {
				exit.set(e);
			}
		});
		try {
			awaitWaiter(source);
			waiter.interrupt();
			waiter.join(5_000);
			assertFalse(waiter.isAlive());
			assertInstanceOf(InterruptedException.class, exit.get());
			assertNull(waitHead(source));
		} finally {
			source.setResult(1);
			waiter.join(5_000);
		}
	}

	@Test
	public void testExpiredWaiterDoesNotLoseAnotherWaitersWakeup() throws Exception {
		var source = new TaskCompletionSource<Integer>();
		var value = new AtomicReference<Integer>();
		var waiter = Thread.ofPlatform().daemon().start(() -> value.set(source.get()));
		try {
			awaitWaiter(source);
			for (var i = 0; i < 100; ++i)
				assertThrows(TimeoutException.class, () -> source.get(1, TimeUnit.NANOSECONDS));
			source.setResult(42);
			waiter.join(5_000);
			assertFalse(waiter.isAlive());
			assertEquals(42, value.get());
			assertNull(waitHead(source));
		} finally {
			source.setResult(42);
			waiter.join(5_000);
		}
	}



	@Test
	public void testCancelTrueInterruptsBeforeJoiningBody() throws Exception {
		var scheduler = Executors.newSingleThreadScheduledExecutor();
		var timer = new TimerFuture<Void>("interrupt-and-join", 60_000);
		var started = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		var interrupted = new CountDownLatch(1);
		var exited = new CountDownLatch(1);
		var canceled = new CountDownLatch(1);
		Thread canceller = null;
		try {
			timer.setFuture(scheduler.scheduleWithFixedDelay(() -> {
				timer.lock();
				try {
					if (timer.isCancelled())
						return;
					started.countDown();
					try {
						release.await();
					} catch (InterruptedException e) {
						interrupted.countDown();
					}
				} finally {
					exited.countDown();
					timer.unlock();
				}
			}, 0, 60_000, TimeUnit.MILLISECONDS));
			assertTrue(started.await(5, TimeUnit.SECONDS));
			canceller = Thread.ofPlatform().daemon().start(() -> {
				timer.cancel(true);
				canceled.countDown();
			});
			assertTrue(interrupted.await(2, TimeUnit.SECONDS), "cancel(true) 必须发送在飞中断");
			assertTrue(canceled.await(2, TimeUnit.SECONDS));
			assertEquals(0, exited.getCount(), "cancel 返回前必须 join 当前 body");
		} finally {
			release.countDown();
			if (canceller != null)
				canceller.join(5_000);
			timer.cancel(false);
			scheduler.shutdownNow();
			assertTrue(scheduler.awaitTermination(5, TimeUnit.SECONDS));
		}
	}

	@Test
	public void testCancellationBeforeFuturePublicationReachesScheduledFuture() throws Exception {
		var scheduler = Executors.newSingleThreadScheduledExecutor();
		try {
			var timer = new TimerFuture<Void>("cancel-before-publication", 60_000);
			assertTrue(timer.cancel(true));
			var scheduled = scheduler.schedule(() -> fail("canceled task ran"), 1, TimeUnit.DAYS);
			timer.setFuture(scheduled);
			assertTrue(scheduled.isCancelled());
			assertTrue(timer.isCancelled());
		} finally {
			scheduler.shutdownNow();
			assertTrue(scheduler.awaitTermination(5, TimeUnit.SECONDS));
		}
	}
}
