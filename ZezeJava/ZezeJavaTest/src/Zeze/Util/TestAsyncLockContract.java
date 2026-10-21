package Zeze.Util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestAsyncLockContract {
	@Test
	void asyncNotificationReplaysWaitingCallbacksInExistingOrder() throws Exception {
		verifyNotification(false);
	}

	@Test
	void inlineNotificationReplaysWaitingCallbacksInExistingOrder() throws Exception {
		verifyNotification(true);
	}

	private static void verifyNotification(boolean inline) throws Exception {
		try (var pool = Executors.newFixedThreadPool(2)) {
			AbstractAsyncLock lock = inline ? new InlineAsyncLock() : new AsyncLock(pool);
			var notified = new AtomicBoolean();
			var attempts = new AtomicInteger();
			var finished = new CountDownLatch(2);
			var order = new ArrayList<Integer>();
			lock.enter(lock::notifyAllWait); // 尚无等待队列。
			for (int i = 0; i < 2; i++) {
				int id = i;
				lock.enter(() -> {
					attempts.incrementAndGet();
					if (!notified.get()) {
						lock.leaveAndWaitNotify();
						return;
					}
					order.add(id);
					finished.countDown();
				});
			}
			assertEquals(2, attempts.get());
			assertFalse(lock.isLocked(), "等待通知不占派发权");
			lock.enter(() -> {
				notified.set(true);
				lock.notifyAllWait();
				lock.notifyAllWait(); // 重复通知不能重复执行。
			});
			assertTrue(finished.await(5, TimeUnit.SECONDS));
			awaitUnlocked(lock);
			assertEquals(4, attempts.get());
			assertEquals(List.of(1, 0), order, "保持原有等待者后进先出语义");
		}
	}

	@Test
	void asyncConcurrentProducersExecuteExactlyOnceWithMutualExclusion() throws Exception {
		verifyConcurrentProducers(false);
	}

	@Test
	void inlineConcurrentProducersExecuteExactlyOnceWithMutualExclusion() throws Exception {
		verifyConcurrentProducers(true);
	}

	private static void verifyConcurrentProducers(boolean inline) throws Exception {
		try (var pool = Executors.newFixedThreadPool(4);
			 var producers = Executors.newFixedThreadPool(4)) {
			AbstractAsyncLock lock = inline ? new InlineAsyncLock() : new AsyncLock(pool);
			int perProducer = 2_000;
			var seen = new AtomicIntegerArray(4 * perProducer);
			var active = new AtomicInteger();
			var overlap = new AtomicBoolean();
			var start = new CountDownLatch(1);
			var finished = new CountDownLatch(seen.length());
			for (int p = 0; p < 4; p++) {
				int base = p * perProducer;
				producers.submit(() -> {
					start.await();
					for (int i = 0; i < perProducer; i++) {
						int id = base + i;
						lock.enter(() -> {
							if (active.incrementAndGet() != 1)
								overlap.set(true);
							seen.incrementAndGet(id);
							Thread.yield();
							active.decrementAndGet();
							finished.countDown();
						});
					}
					return null;
				});
			}
			start.countDown();
			assertTrue(finished.await(10, TimeUnit.SECONDS));
			awaitUnlocked(lock);
			assertFalse(overlap.get());
			for (int i = 0; i < seen.length(); i++)
				assertEquals(1, seen.get(i), "callback " + i);
		}
	}

	@Test
	void asyncCallbackExceptionDoesNotPreventNextCallback() throws Exception {
		verifyCallbackException(false);
	}

	@Test
	void inlineCallbackExceptionDoesNotPreventNextCallback() throws Exception {
		verifyCallbackException(true);
	}

	private static void verifyCallbackException(boolean inline) throws Exception {
		try (var pool = Executors.newSingleThreadExecutor()) {
			AbstractAsyncLock lock = inline ? new InlineAsyncLock() : new AsyncLock(pool);
			var finished = new CountDownLatch(1);
			lock.enter(() -> {
				lock.enter(() -> {
					throw new IllegalStateException("test callback failure");
				});
				lock.enter(finished::countDown);
			});
			assertTrue(finished.await(5, TimeUnit.SECONDS));
			awaitUnlocked(lock);
		}
	}

	@Test
	void delayedExecutionOnSubmittingThreadStillDispatchesOneAtATime() {
		var tasks = new ArrayDeque<Runnable>();
		var lock = new AsyncLock(tasks::addLast);
		var order = new ArrayList<Integer>();
		lock.enter(() -> {
			lock.enter(() -> order.add(1));
			lock.enter(() -> order.add(2));
		});
		assertEquals(1, tasks.size());
		tasks.removeFirst().run();
		assertEquals(List.of(1), order, "execute 返回后的同线程执行不能误判为直接执行器");
		assertEquals(1, tasks.size());
		tasks.removeFirst().run();
		assertEquals(List.of(1, 2), order);
		assertFalse(lock.isLocked());
	}

	@Test
	void explicitLeaveHandsOffBeforeCallbackReturns() throws Exception {
		try (var pool = Executors.newSingleThreadExecutor()) {
			var lock = new AsyncLock(pool);
			var next = new CountDownLatch(1);
			var handedOff = new AtomicBoolean();
			lock.enter(() -> {
				lock.enter(next::countDown);
				lock.leave();
				handedOff.set(next.await(5, TimeUnit.SECONDS));
			});
			assertTrue(handedOff.get(), "正常异步路径的显式 leave 必须立即交接");
			awaitUnlocked(lock);
		}
	}

	private static void awaitUnlocked(AbstractAsyncLock lock) throws InterruptedException {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while (lock.isLocked() && System.nanoTime() < deadline)
			Thread.sleep(1); // 回调内的完成通知早于 finally 中的释放。
		assertFalse(lock.isLocked());
	}
}
