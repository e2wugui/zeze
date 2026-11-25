package Zeze.Util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import harness.Fast;
import org.junit.jupiter.api.Test;

@Fast
public class TestThreadHelperShutdown {
	@Test
	public void shutdownFromWorkerReturnsAndStops() throws Exception {
		var completed = new CountDownLatch(1);
		var failure = new AtomicReference<Throwable>();
		var worker = new ThreadHelper("self-shutdown") {
			@Override
			public void run() {
				try {
					shutdown();
					assertFalse(isRunning());
				} catch (Throwable e) {
					failure.set(e);
				} finally {
					completed.countDown();
				}
			}
		};
		worker.start();
		assertTrue(completed.await(3, TimeUnit.SECONDS), "worker shutdown waited for itself");
		worker.join(3000);
		assertFalse(worker.isAlive());
		assertNull(failure.get());
	}

	@Test
	public void externalShutdownWaitsForWorkerExitAndWakesIdle() throws Exception {
		var started = new CountDownLatch(1);
		var cleanupStarted = new CountDownLatch(1);
		var finishCleanup = new CountDownLatch(1);
		var exited = new CountDownLatch(1);
		var shutdownReturned = new CountDownLatch(1);
		var worker = new ThreadHelper("external-shutdown") {
			@Override
			public void run() {
				started.countDown();
				while (isRunning())
					sleepIdle(TimeUnit.MINUTES.toMillis(1));
				cleanupStarted.countDown();
				try {
					finishCleanup.await();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				exited.countDown();
			}
		};
		worker.start();
		assertTrue(started.await(3, TimeUnit.SECONDS));
		var stopper = new Thread(() -> {
			worker.shutdown();
			shutdownReturned.countDown();
		}, "external-stopper");
		stopper.setDaemon(true);
		stopper.start();
		try {
			assertTrue(cleanupStarted.await(3, TimeUnit.SECONDS), "shutdown did not wake idle worker");
			assertFalse(shutdownReturned.await(100, TimeUnit.MILLISECONDS), "shutdown skipped worker cleanup");
		} finally {
			finishCleanup.countDown();
		}
		stopper.join(3000);
		assertFalse(stopper.isAlive());
		assertEquals(0, shutdownReturned.getCount());
		assertEquals(0, exited.getCount());
		assertFalse(worker.isAlive(), "external shutdown returned before worker exit");
	}

	@Test
	public void requestShutdownReturnsBeforeWorkerCleanupFinishes() throws Exception {
		var started = new CountDownLatch(1);
		var finish = new CountDownLatch(1);
		var worker = new ThreadHelper("requested-shutdown") {
			@Override
			public void run() {
				started.countDown();
				try {
					finish.await();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		};
		worker.start();
		try {
			assertTrue(started.await(3, TimeUnit.SECONDS));
			worker.requestShutdown();
			assertFalse(worker.isRunning());
			assertTrue(worker.isAlive());
		} finally {
			finish.countDown();
			worker.join(3000);
		}
		assertFalse(worker.isAlive());
	}

	@Test
	public void directSelfJoinIsRejected() throws Exception {
		var completed = new CountDownLatch(1);
		var failure = new AtomicReference<Throwable>();
		var worker = new ThreadHelper("self-join") {
			@Override
			public void run() {
				try {
					assertThrows(IllegalStateException.class, this::joinAssuring);
				} catch (Throwable e) {
					failure.set(e);
				} finally {
					completed.countDown();
				}
			}
		};
		worker.start();
		assertTrue(completed.await(3, TimeUnit.SECONDS), "direct self join waited for itself");
		worker.join(3000);
		assertFalse(worker.isAlive());
		assertNull(failure.get());
	}

	@Test
	public void externalInterruptDoesNotSpinSleepIdleLoop() throws Exception {
		var firstIterationStarted = new CountDownLatch(1);
		var secondIterationReached = new CountDownLatch(1);
		var thirdIterationReached = new CountDownLatch(1);
		var iterations = new AtomicInteger();
		var worker = new ThreadHelper("interrupt-sleep-idle") {
			@Override
			public void run() {
				while (isRunning()) {
					int n = iterations.incrementAndGet();
					if (n == 1)
						firstIterationStarted.countDown();
					else if (n == 2)
						secondIterationReached.countDown();
					else if (n >= 3)
						thirdIterationReached.countDown();
					sleepIdle(TimeUnit.MINUTES.toMillis(1));
				}
			}
		};
		worker.start();
		try {
			assertTrue(firstIterationStarted.await(3, TimeUnit.SECONDS));
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
			while (worker.getState() != Thread.State.TIMED_WAITING && System.nanoTime() < deadline)
				Thread.onSpinWait();
			assertEquals(Thread.State.TIMED_WAITING, worker.getState());

			worker.interrupt();
			assertTrue(secondIterationReached.await(3, TimeUnit.SECONDS), "interrupt should wake current sleepIdle");
			assertFalse(thirdIterationReached.await(200, TimeUnit.MILLISECONDS),
					"sleepIdle must consume interrupt flag and sleep again instead of spinning");
			assertEquals(2, iterations.get());
		} finally {
			worker.shutdown();
		}
		assertFalse(worker.isAlive());
	}
}
