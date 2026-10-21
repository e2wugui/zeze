package Zeze.Util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestAsyncLockHandoff {
	@Test
	void oldCallbackFinallyCannotReleaseNewOwner() throws Exception {
		try (var pool = Executors.newFixedThreadPool(2)) {
			var lock = new AsyncLock(pool);
			var nextEntered = new CountDownLatch(1);
			var releaseNext = new CountDownLatch(1);
			var returnOld = new CountDownLatch(1);
			var tailRan = new CountDownLatch(1);
			var old = new Thread(() -> lock.enter(() -> {
				lock.enter(() -> {
					nextEntered.countDown();
					releaseNext.await();
				});
				lock.leave();
				returnOld.await();
			}), "asynclock-old-owner");
			old.setDaemon(true);
			old.start();
			try {
				assertTrue(nextEntered.await(5, TimeUnit.SECONDS));
				lock.enter(tailRan::countDown);
				returnOld.countDown();
				old.join(5_000); // 旧回调的 finally 在新回调仍持锁时执行。
				assertFalse(old.isAlive());
				assertTrue(lock.isLocked());
				assertEquals(1, tailRan.getCount(), "旧 finally 不得派发新持锁者之后的任务");
				releaseNext.countDown();
				assertTrue(tailRan.await(5, TimeUnit.SECONDS));
				awaitUnlocked(lock);
			} finally {
				returnOld.countDown();
				releaseNext.countDown();
				old.join(5_000);
			}
		}
	}

	@Test
	void workerStartedBeforeExecuteReturnsStillHandsOffOnLeave() throws Exception {
		try (var pool = Executors.newFixedThreadPool(2)) {
			var submissions = new AtomicInteger();
			var returnExecute = new CountDownLatch(1);
			var secondRan = new CountDownLatch(1);
			var handedOff = new AtomicBoolean();
			var submissionTimedOut = new AtomicBoolean();
			var lock = new AsyncLock(task -> {
				int submission = submissions.incrementAndGet();
				pool.execute(task);
				if (submission == 1) {
					try {
						submissionTimedOut.set(!returnExecute.await(10, TimeUnit.SECONDS));
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						submissionTimedOut.set(true);
					}
				}
			});
			var submitter = new Thread(() -> lock.enter(() -> {
				lock.enter(() -> {
					lock.leave();
					handedOff.set(secondRan.await(5, TimeUnit.SECONDS));
				});
				lock.enter(secondRan::countDown);
			}), "asynclock-submitter");
			submitter.setDaemon(true);
			submitter.start();
			try {
				assertTrue(secondRan.await(5, TimeUnit.SECONDS));
				// 第一轮 execute 此刻仍未返回，但工作线程必须走正常异步交接，不能误判为内联。
				assertEquals(2, submissions.get());
			} finally {
				returnExecute.countDown();
				submitter.join(5_000);
			}
			assertFalse(submitter.isAlive());
			pool.shutdown();
			assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
			assertFalse(submissionTimedOut.get());
			assertTrue(handedOff.get());
			assertFalse(lock.isLocked());
		}
	}

	@Test
	void concurrentBurstsAcrossInlineRejectedAndPooledDispatchStayExclusive() throws Exception {
		try (var pool = Executors.newFixedThreadPool(2);
			 var producers = Executors.newFixedThreadPool(2)) {
			var submissions = new AtomicInteger();
			var lock = new AsyncLock(task -> {
				switch (submissions.getAndIncrement() % 3) {
				case 0 -> pool.execute(task);
				case 1 -> task.run();
				default -> throw new RejectedExecutionException("test rejection");
				}
			});
			int rounds = 256;
			var seen = new AtomicIntegerArray(rounds * 4);
			var active = new AtomicInteger();
			var overlap = new AtomicBoolean();
			for (int round = 0; round < rounds; round++) {
				var start = new CountDownLatch(1);
				var finished = new CountDownLatch(4);
				for (int p = 0; p < 2; p++) {
					int id = round * 4 + p * 2;
					producers.submit(() -> {
						start.await();
						lock.enter(() -> {
							if (active.incrementAndGet() != 1)
								overlap.set(true);
							seen.incrementAndGet(id);
							lock.enter(() -> {
								if (active.incrementAndGet() != 1)
									overlap.set(true);
								seen.incrementAndGet(id + 1);
								Thread.yield();
								active.decrementAndGet();
								finished.countDown();
							});
							Thread.yield();
							active.decrementAndGet();
							finished.countDown();
						});
						return null;
					});
				}
				start.countDown();
				assertTrue(finished.await(5, TimeUnit.SECONDS), "round " + round);
				awaitUnlocked(lock); // 反复经过空队列交接，覆盖旧内联驱动收尾与新驱动接管。
			}
			assertTrue(submissions.get() >= 3, "必须覆盖线程池、直接执行、拒绝兜底三条路径");
			assertFalse(overlap.get());
			for (int i = 0; i < seen.length(); i++)
				assertEquals(1, seen.get(i), "callback " + i);
		}
	}

	private static void awaitUnlocked(AbstractAsyncLock lock) throws InterruptedException {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while (lock.isLocked() && System.nanoTime() < deadline)
			Thread.sleep(1);
		assertFalse(lock.isLocked());
	}
}
