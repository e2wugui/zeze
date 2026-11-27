package Zeze.Util;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestDaemonTimerRoundLifecycle {
	@Test
	public void bodyExceptionStillRenewsChain() throws Exception {
		runIsolated("body-failure");
	}

	@Test
	public void timeoutInitializationFailureStillSettlesRound() throws Exception {
		runIsolated("timeout-failure");
	}

	private static void runIsolated(String scenario) throws Exception {
		// Task 的执行器是进程全局状态；子进程隔离，支持 @Fast 类级并行。
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
				case "body-failure" -> verifyBodyFailure();
				case "timeout-failure" -> verifyTimeoutFailure();
				default -> throw new AssertionError(args[0]);
			}
		} finally {
			Task.shutdownNow(5_000);
		}
	}

	private static void verifyBodyFailure() throws Exception {
		var first = new AtomicBoolean(true);
		var renewed = new CountDownLatch(1);
		var daemon = new DaemonTimer("round.body.failure", 10, () -> {
			if (first.getAndSet(false))
				throw new IllegalStateException("expected first round body failure");
			renewed.countDown();
		});
		try {
			daemon.start();
			assertTrue(renewed.await(5, TimeUnit.SECONDS), "failed round must settle inFlight and renew the chain");
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

	private static void await(BooleanSupplier condition) throws InterruptedException {
		long start = System.nanoTime();
		while (!condition.getAsBoolean()) {
			assertTrue(System.nanoTime() - start < TimeUnit.SECONDS.toNanos(5), "condition did not become true");
			Thread.sleep(1);
		}
	}
}
