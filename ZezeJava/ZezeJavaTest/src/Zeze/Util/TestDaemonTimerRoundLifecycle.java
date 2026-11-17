package Zeze.Util;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import Zeze.Hot.HotGuard;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestDaemonTimerRoundLifecycle {
	@Test
	public void initializationFailureStillRenewsRound() throws Exception {
		runIsolated("guard-failure");
	}

	@Test
	public void timeoutInitializationFailureStillSettlesRound() throws Exception {
		runIsolated("timeout-failure");
	}

	@Test
	public void stopWaitsForGuardClose() throws Exception {
		runIsolated("guard-close");
	}

	private static void runIsolated(String scenario) throws Exception {
		// Task 的执行器/guard 是进程全局状态；子进程隔离，支持 @Fast 类级并行。
		var child = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
				"-DZezeCounter=null", "-Dzeze.test.env=off", "-cp", System.getProperty("java.class.path"),
				TestDaemonTimerRoundLifecycle.class.getName(), scenario).inheritIO().start();
		try {
			assertTrue(child.waitFor(20, TimeUnit.SECONDS), "child did not finish: " + scenario);
			assertEquals(0, child.exitValue(), scenario);
		} finally {
			child.destroyForcibly();
		}
	}

	public static void main(String[] args) throws Exception {
		Task.initThreadPool(Executors.newSingleThreadExecutor(r -> new Thread(r, "round-worker")),
				Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "round-scheduler")));
		try {
			switch (args[0]) {
				case "guard-failure" -> verifyGuardFailure();
				case "timeout-failure" -> verifyTimeoutFailure();
				case "guard-close" -> verifyGuardClose();
				default -> throw new AssertionError(args[0]);
			}
		} finally {
			Task.hotGuard = () -> HotGuard.None;
			Task.shutdownNow(5_000);
		}
	}

	private static void verifyGuardFailure() throws Exception {
		var fail = new AtomicBoolean(true);
		var entered = new CountDownLatch(1);
		Task.hotGuard = () -> {
			if (Thread.currentThread().getName().equals("round-worker") && fail.getAndSet(false))
				throw new IllegalStateException("expected guard initialization failure");
			return HotGuard.None;
		};
		var daemon = new DaemonTimer("round.guard.failure", 10, 100, entered::countDown);
		try {
			daemon.start();
			assertTrue(entered.await(5, TimeUnit.SECONDS), "failed round must renew the chain");
		} finally {
			daemon.stop();
		}
	}

	private static void verifyTimeoutFailure() throws Exception {
		var first = new AtomicBoolean(true);
		var daemon = new DaemonTimer("round.timeout.failure", () -> first.getAndSet(false) ? 10 : 0,
				366L * 24 * 3600 * 1000, () -> fail("invalid timeout must not enter body"));
		daemon.start();
		await(daemon::isShutdown);
		assertFalse(daemon.isBusy(), "initialization failure must settle inFlight");
		daemon.stop();
	}

	private static void verifyGuardClose() throws Exception {
		var closing = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		Task.hotGuard = () -> Thread.currentThread().getName().equals("round-worker")
				? new HotGuard(new java.util.concurrent.locks.ReentrantLock()) {
			@Override
			public void close() {
				closing.countDown();
				try {
					assertTrue(release.await(5, TimeUnit.SECONDS));
				} catch (InterruptedException e) {
					throw new AssertionError(e);
				} finally {
					super.close();
				}
			}
		} : HotGuard.None;
		var daemon = new DaemonTimer("round.guard.close", 10, () -> {});
		var stopper = Executors.newSingleThreadExecutor();
		try {
			daemon.start();
			assertTrue(closing.await(5, TimeUnit.SECONDS));
			var stopped = stopper.submit(daemon::stop);
			assertThrows(java.util.concurrent.TimeoutException.class, () -> stopped.get(100, TimeUnit.MILLISECONDS));
			release.countDown();
			stopped.get(5, TimeUnit.SECONDS);
		} finally {
			release.countDown();
			daemon.stop();
			stopper.shutdownNow();
		}
	}

	private static void await(BooleanSupplier condition) throws InterruptedException {
		long start = System.nanoTime();
		while (!condition.getAsBoolean()) {
			assertTrue(System.nanoTime() - start < TimeUnit.SECONDS.toNanos(5), "condition did not become true");
			Thread.sleep(1);
		}
	}
}
