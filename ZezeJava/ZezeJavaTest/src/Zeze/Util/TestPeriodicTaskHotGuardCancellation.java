package Zeze.Util;

import java.io.File;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BooleanSupplier;
import Zeze.AppBase;
import Zeze.Application;
import Zeze.Config;
import Zeze.Hot.HotGuard;
import Zeze.Hot.HotManager;
import harness.Fast;
import harness.FastServerIds;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Parent tests never change Task.hotGuard or Task pools. Each child owns those process globals.
 * All children are local and disposable. The manager-cycle child may contain a real old-code lock
 * cycle; only that child is killed/reaped on timeout. No install/start/tryDistribute/halt is called.
 */
@Fast
@Timeout(45)
public class TestPeriodicTaskHotGuardCancellation {
	@Test
	void cancelDoesNotJoinAnUnadmittedGuardWait(@TempDir(cleanup = CleanupMode.ON_SUCCESS) Path dir) throws Exception {
		runChild(dir, "guard-wait");
	}

	@Test
	void hotManagerWriterCanCancelAQueuedPeriodicRound(@TempDir(cleanup = CleanupMode.ON_SUCCESS) Path dir) throws Exception {
		runChild(dir, "manager-cycle");
	}

	@Test
	void cancelStillInterruptsAndDrainsAnAdmittedBody(@TempDir(cleanup = CleanupMode.ON_SUCCESS) Path dir) throws Exception {
		runChild(dir, "body");
	}

	@Test
	void cancelStillDrainsAnAdmittedGuardClose(@TempDir(cleanup = CleanupMode.ON_SUCCESS) Path dir) throws Exception {
		runChild(dir, "guard-close");
	}

	@Test
	void factoryFailureDoesNotKeepTheFutureLocked(@TempDir(cleanup = CleanupMode.ON_SUCCESS) Path dir) throws Exception {
		runChild(dir, "factory-failure");
	}

	private static String childClasspath() throws Exception {
		// Gradle 9 workers may expose only the worker bootstrap in java.class.path.
		// Follow TestDbh2IncrementalRecover's loader walk, then add the two code sources.
		var entries = new LinkedHashSet<Path>();
		for (var loader = TestPeriodicTaskHotGuardCancellation.class.getClassLoader(); loader != null;
				loader = loader.getParent()) {
			if (loader instanceof URLClassLoader urls) {
				for (var url : urls.getURLs()) {
					if (url.getProtocol().equals("file"))
						entries.add(Path.of(url.toURI()).toAbsolutePath().normalize());
				}
			}
		}
		for (var entry : System.getProperty("java.class.path").split(java.util.regex.Pattern.quote(File.pathSeparator)))
			entries.add(Path.of(entry).toAbsolutePath().normalize());
		for (var type : new Class<?>[]{TestPeriodicTaskHotGuardCancellation.class, Task.class})
			entries.add(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI())
					.toAbsolutePath().normalize());
		return String.join(File.pathSeparator, entries.stream().map(Path::toString).toList());
	}

	private static String quoteArgument(String argument) {
		return '"' + argument.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
	}

	private static void runChild(Path dir, String scenario) throws Exception {
		assertEquals(21, Runtime.version().feature(), "This test requires the repository's JDK 21 worker");
		var arguments = new ArrayList<>(List.of("-ea", "-DZezeCounter=null", "-Dzeze.test.env=off",
				"-DuseVirtualThread=false", "-DuseUnlimitedVirtualThread=false", "-cp", childClasspath(),
				TestPeriodicTaskHotGuardCancellation.class.getName(), scenario, dir.toAbsolutePath().toString()));
		var argumentFile = dir.resolve("java.args");
		// Launcher argfiles use native.encoding on Windows; keep non-ASCII cache/user paths intact.
		Files.write(argumentFile, arguments.stream().map(TestPeriodicTaskHotGuardCancellation::quoteArgument).toList(),
				Charset.forName(System.getProperty("native.encoding", "UTF-8")));
		var java = Path.of(System.getProperty("java.home"), "bin",
				System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java");
		var log = dir.resolve(scenario + ".log");
		var child = new ProcessBuilder(java.toString(), "@" + argumentFile.toAbsolutePath())
				.directory(dir.toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
		try {
			boolean completed = child.waitFor(30, TimeUnit.SECONDS);
			if (!completed)
				stopOwnChild(child, log);
			String transcript;
			try {
				transcript = Files.readString(log, StandardCharsets.UTF_8);
			} catch (java.io.IOException e) {
				throw new AssertionError("PROBE_OUTPUT_FAILURE " + scenario + ": " + log, e);
			}
			System.out.println("periodic child " + scenario + ":\n" + transcript);
			assertTrue(transcript.contains("READY " + scenario),
					"PROBE_STARTUP_FAILURE (not S005 red): " + transcript);
			assertTrue(!transcript.contains("CLEANUP_FAILURE"),
					"PROBE_CLEANUP_FAILURE (not S005 red): " + transcript);
			if (!completed) {
				boolean confirmed = scenario.equals("manager-cycle")
						&& transcript.contains("actual-manager-writer-calling-cancel")
						&& transcript.contains("confirmed-actual-lock-cycle");
				throw new AssertionError((confirmed ? "S005_CONFIRMED_MANAGER_CYCLE: "
						: "PROBE_TIMEOUT (not S005 red): ") + transcript);
			}
			assertEquals(0, child.exitValue(), "SCENARIO_FAILURE " + scenario + ":\n" + transcript);
			assertTrue(transcript.contains("PASS " + scenario), "PROBE_OUTPUT_FAILURE: " + transcript);
		} finally {
			stopOwnChild(child, log);
		}
	}

	private static void stopOwnChild(Process child, Path log) {
		if (!child.isAlive())
			return;
		child.destroyForcibly();
		boolean interrupted = false;
		long started = System.nanoTime();
		try {
			for (;;) {
				long remaining = TimeUnit.SECONDS.toNanos(5) - (System.nanoTime() - started);
				if (remaining <= 0)
					throw new AssertionError("PROBE_CLEANUP_FAILURE: own child not reaped: " + log);
				try {
					if (child.waitFor(remaining, TimeUnit.NANOSECONDS))
						return;
				} catch (InterruptedException e) {
					interrupted = true;
				}
			}
		} finally {
			if (interrupted)
				Thread.currentThread().interrupt();
		}
	}

	// Only adds entry/exit observation around the original Runnable supplied by Task;
	// does not copy or replace the production scheduling body/cancellation implementation.
	private static final class ProbeScheduler extends ScheduledThreadPoolExecutor {
		final AtomicReference<@Nullable Thread> runner = new AtomicReference<>();
		final CountDownLatch finished = new CountDownLatch(1);

		ProbeScheduler() {
			super(1, r -> {
				var thread = new Thread(r, "periodic-guard-child-scheduler");
				thread.setDaemon(true);
				return thread;
			});
		}

		@Override
		public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay,
				long delay, TimeUnit unit) {
			return super.scheduleWithFixedDelay(() -> {
				runner.set(Thread.currentThread());
				try {
					command.run();
				} finally {
					finished.countDown();
				}
			}, initialDelay, delay, unit);
		}
	}

	private static void check(boolean value, String message) {
		if (!value)
			throw new AssertionError(message);
	}

	private static void await(CountDownLatch latch, String message) throws InterruptedException {
		check(latch.await(5, TimeUnit.SECONDS), message);
	}

	private static void awaitCondition(BooleanSupplier condition, String message) throws InterruptedException {
		long started = System.nanoTime();
		while (!condition.getAsBoolean()) {
			check(System.nanoTime() - started < TimeUnit.SECONDS.toNanos(5), message);
			Thread.sleep(1); // bounded observation polling, not a chosen race window
		}
	}

	private static void awaitUninterruptibly(CountDownLatch gate, CountDownLatch interrupted) {
		boolean restore = false;
		for (;;) {
			try {
				gate.await();
				break;
			} catch (InterruptedException e) {
				restore = true;
				interrupted.countDown();
			}
		}
		if (restore)
			Thread.currentThread().interrupt();
	}

	private static Thread startCancel(TimerFuture<?> timer, boolean interrupt, CountDownLatch done,
			AtomicReference<@Nullable Throwable> failure) {
		return Thread.ofPlatform().daemon().name("periodic-guard-child-cancel").start(() -> {
			try {
				timer.cancel(interrupt);
			} catch (Throwable e) {
				failure.set(e);
			} finally {
				done.countDown();
			}
		});
	}

	private static void join(Thread thread, AtomicReference<@Nullable Throwable> failure)
			throws InterruptedException {
		thread.join(5_000);
		check(!thread.isAlive(), "CLEANUP_FAILURE: released probe must not retain a canceller");
		if (failure.get() != null)
			throw new AssertionError("cancel failed", failure.get());
	}

	private static final class GateLock extends ReentrantLock {
		final CountDownLatch attempted = new CountDownLatch(1);
		final CountDownLatch release = new CountDownLatch(1);
		final CountDownLatch interrupted = new CountDownLatch(1);
		final CountDownLatch closed = new CountDownLatch(1);

		@Override
		public void lock() {
			attempted.countDown();
			awaitUninterruptibly(release, interrupted);
			super.lock();
		}

		@Override
		public void unlock() {
			try {
				super.unlock();
			} finally {
				closed.countDown();
			}
		}
	}

	private static void guardWait(ProbeScheduler scheduler) throws Exception {
		var gate = new GateLock();
		var bodies = new AtomicInteger();
		var canceled = new CountDownLatch(1);
		var failure = new AtomicReference<@Nullable Throwable>();
		Task.hotGuard = () -> new HotGuard(gate);
		var timer = TaskSpec.ofAction(bodies::incrementAndGet).name("periodic-guard-wait")
				.schedulePeriodNow(0, 60_000);
		Thread canceller = null;
		try {
			await(gate.attempted, "original periodic wrapper must reach hot guard acquisition");
			canceller = startCancel(timer, true, canceled, failure);
			await(gate.interrupted, "underlying Future.cancel(true) must interrupt waiting wrapper");
			// Old: cancellation times out here while the gate stays closed; finally makes it drain.
			// Fixed: cancellation completes without admission; gate is still closed and body is zero.
			await(canceled, "S005_GUARD_WAIT_BLOCKED: cancel must not join an unadmitted guard acquisition");
			check(bodies.get() == 0, "unadmitted body must not execute");
			check(gate.release.getCount() == 1, "success must precede guard release");
		} finally {
			gate.release.countDown();
			if (canceller == null)
				canceller = startCancel(timer, true, canceled, failure);
			join(canceller, failure);
			await(scheduler.finished, "original wrapper must finish after releasing the gate");
			await(gate.closed, "late acquired guard must be closed");
			check(bodies.get() == 0, "late guard admission must recheck cancellation before body");
			System.out.println("guard-wait-cleaned: canceled=" + timer.isCancelled() + ", body=" + bodies.get());
		}
	}

	private static HotManager newManager(Path dir) throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setNoDatabase(true);
		conf.setServerId(FastServerIds.TEST_PERIODIC_TASK_HOT_GUARD_CANCELLATION);
		conf.setDefaultTableConf(new Config.TableConf());
		// Application always calls conf.createDatabase even in NoDatabase mode. Keep its map
		// empty: no memory bucket/default URL, native database or service. The app id is still registered.
		conf.getDatabaseConfMap().clear();
		var app = new Application("PeriodicTaskHotGuardChild", conf);
		check(app.getDatabases().isEmpty(), "manager child must not create a database");
		var working = dir.resolve("working");
		Files.createDirectories(working.resolve("interfaces"));
		Files.createDirectories(working.resolve("modules"));
		var staging = Files.createDirectories(dir.resolve("staging"));
		return new HotManager(new AppBase() {
			@Override
			public Application getZeze() {
				return app;
			}
		}, working.toString(), staging.toString());
	}

	private static void managerCycle(ProbeScheduler scheduler, Path dir) throws Exception {
		var manager = newManager(dir); // no start(), install(), tryDistribute(), database or service start
		var lockField = HotManager.class.getDeclaredField("hotLock");
		lockField.setAccessible(true);
		var hotLock = (ReentrantReadWriteLock)lockField.get(manager);
		var attempted = new CountDownLatch(1);
		var canceled = new CountDownLatch(1);
		var bodies = new AtomicInteger();
		var writerThread = Thread.currentThread();
		Task.hotGuard = () -> {
			attempted.countDown();
			return manager.enterReadLock(); // actual production read entry on the actual shared hotLock
		};
		Thread observer = null;
		TimerFuture<?> ownedTimer = null;
		try (var writer = manager.enterWriteLock()) {
			var timer = TaskSpec.ofAction(bodies::incrementAndGet).name("periodic-hot-manager-writer")
					.schedulePeriodNow(0, 60_000);
			ownedTimer = timer;
			await(attempted, "periodic wrapper must request manager read guard");
			awaitCondition(() -> hotLock.hasQueuedThread(Objects.requireNonNull(scheduler.runner.get())),
					"periodic wrapper must be queued on actual manager hot read lock");
			var periodicThread = Objects.requireNonNull(scheduler.runner.get());
			observer = Thread.ofPlatform().daemon().name("periodic-hot-manager-cycle-observer").start(() -> {
				try {
					while (!canceled.await(10, TimeUnit.MILLISECONDS)) {
						if (timer.hasQueuedThread(writerThread)
								&& hotLock.hasQueuedThread(Objects.requireNonNull(scheduler.runner.get()))) {
							System.out.println("actual-lock-cycle-queues: writer owns hotLock and waits TimerFuture; "
									+ "periodic worker owns TimerFuture and waits hotLock.readLock");
							for (var thread : new Thread[]{writerThread, periodicThread}) {
								System.out.println("thread=" + thread.getName());
								for (var frame : thread.getStackTrace())
									System.out.println("  " + frame);
							}
							System.out.println("confirmed-actual-lock-cycle");
							System.out.flush();
							return;
						}
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			});
			System.out.println("actual-manager-writer-calling-cancel");
			System.out.flush();
			timer.cancel(true); // Old: actual closed wait cycle. Parent alone kills/reaps this child.
			canceled.countDown();
			check(bodies.get() == 0, "writer-held interval cannot admit the body");
		} finally {
			// Green/ordinary failure: same writer thread releases its own lock via try-with-resources.
			// Old cycle cannot reach here; the parent owns the child process and forcibly reaps it.
			try {
				canceled.countDown();
				if (ownedTimer != null)
					ownedTimer.cancel(true); // writer resource already released, ordinary failure cleanup only
				if (observer != null) {
					observer.join(5_000);
					check(!observer.isAlive(), "CLEANUP_FAILURE: cycle observer must stop");
				}
			} finally {
				var executorField = HotManager.class.getDeclaredField("installExecutor");
				executorField.setAccessible(true);
				var executor = (ExecutorService)executorField.get(manager);
				executor.shutdownNow(); // lazy and never submitted to
				try {
					manager.getDistributeManager().closeAll();
				} finally {
					check(executor.awaitTermination(5, TimeUnit.SECONDS), "CLEANUP_FAILURE: manager executor");
				}
			}
		}
		await(scheduler.finished, "late periodic wrapper must exit after actual writer release");
		check(bodies.get() == 0, "canceled late round must not run body");
		System.out.println("actual-manager-writer-completed: body=0");
	}

	private static void admittedBody(ProbeScheduler scheduler) throws Exception {
		var entered = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		var interrupted = new CountDownLatch(1);
		var exited = new CountDownLatch(1);
		var canceled = new CountDownLatch(1);
		var failure = new AtomicReference<@Nullable Throwable>();
		var timer = TaskSpec.ofAction(() -> {
			entered.countDown();
			try {
				awaitUninterruptibly(release, interrupted);
			} finally {
				exited.countDown();
			}
		}).name("periodic-admitted-body").schedulePeriodNow(0, 60_000);
		Thread canceller = null;
		try {
			await(entered, "body must have been admitted by original Task wrapper");
			canceller = startCancel(timer, true, canceled, failure);
			await(interrupted, "cancel(true) must interrupt before joining body");
			var observedCanceller = canceller;
			awaitCondition(() -> canceled.getCount() == 0 || timer.hasQueuedThread(observedCanceller),
					"cancel must either return or queue on admitted body's future lock");
			check(canceled.getCount() == 1, "cancel must keep draining an admitted body");
			release.countDown();
			await(canceled, "cancel must return after body is released");
			check(exited.getCount() == 0, "cancel return must follow admitted body exit");
		} finally {
			release.countDown();
			if (canceller == null)
				canceller = startCancel(timer, true, canceled, failure);
			join(canceller, failure);
			await(scheduler.finished, "released body wrapper must finish");
		}
	}

	private static void admittedGuardClose(ProbeScheduler scheduler) throws Exception {
		var closing = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		var interrupted = new CountDownLatch(1);
		var closed = new CountDownLatch(1);
		var canceled = new CountDownLatch(1);
		var failure = new AtomicReference<@Nullable Throwable>();
		Task.hotGuard = () -> new HotGuard(new ReentrantLock()) {
			@Override
			public void close() {
				closing.countDown();
				try {
					awaitUninterruptibly(release, interrupted);
				} finally {
					super.close();
					closed.countDown();
				}
			}
		};
		var timer = TaskSpec.ofAction(() -> { }).name("periodic-admitted-guard-close")
				.schedulePeriodNow(0, 60_000);
		Thread canceller = null;
		try {
			await(closing, "body must finish and enter successful guard close");
			canceller = startCancel(timer, false, canceled, failure);
			var observedCanceller = canceller;
			awaitCondition(() -> canceled.getCount() == 0 || timer.hasQueuedThread(observedCanceller),
					"cancel must either return or queue on admitted guard close");
			check(canceled.getCount() == 1, "moving guard admission must preserve admitted close draining");
			release.countDown();
			await(canceled, "cancel(false) must return after guard closes");
			check(closed.getCount() == 0, "cancel return must follow admitted guard close");
		} finally {
			release.countDown();
			if (canceller == null)
				canceller = startCancel(timer, false, canceled, failure);
			join(canceller, failure);
			await(scheduler.finished, "admitted close wrapper must finish");
		}
	}

	private static void factoryFailure(ProbeScheduler scheduler) throws Exception {
		var attempts = new AtomicInteger();
		var secondBody = new CountDownLatch(1);
		Task.hotGuard = () -> {
			if (attempts.incrementAndGet() == 1)
				throw new IllegalStateException("controlled hot guard factory failure");
			return HotGuard.None;
		};
		var timer = TaskSpec.ofAction(secondBody::countDown).name("periodic-guard-factory-failure")
				.schedulePeriodNow(0, 1);
		var canceled = new CountDownLatch(1);
		var failure = new AtomicReference<@Nullable Throwable>();
		Thread canceller = null;
		try {
			await(secondBody, "factory failure must not strand future lock or stop later periodic round");
			check(attempts.get() >= 2, "later factory attempt must be reached");
			canceller = startCancel(timer, false, canceled, failure);
			await(canceled, "factory failure must not prevent later cancel");
		} finally {
			if (canceller == null)
				canceller = startCancel(timer, false, canceled, failure);
			join(canceller, failure);
			await(scheduler.finished, "factory scenario must finish a wrapper round");
		}
	}

	public static void main(String[] args) throws Exception {
		check(Runtime.version().feature() == 21, "probe requires JDK 21");
		var scheduler = new ProbeScheduler();
		var worker = Executors.newSingleThreadExecutor(r -> {
			var thread = new Thread(r, "periodic-guard-child-default");
			thread.setDaemon(true);
			return thread;
		});
		var originalGuard = Task.hotGuard;
		Throwable failure = null;
		try {
			Task.initThreadPool(worker, scheduler);
			System.out.println("READY " + args[0]);
			System.out.flush();
			switch (args[0]) {
			case "guard-wait" -> guardWait(scheduler);
			case "manager-cycle" -> managerCycle(scheduler, Path.of(args[1]));
			case "body" -> admittedBody(scheduler);
			case "guard-close" -> admittedGuardClose(scheduler);
			case "factory-failure" -> factoryFailure(scheduler);
			default -> throw new IllegalArgumentException(args[0]);
			}
		} catch (Throwable e) {
			failure = e;
		} finally {
			Task.hotGuard = originalGuard;
			try {
				Task.shutdownNow(5_000);
			} catch (Throwable e) {
				System.out.println("CLEANUP_FAILURE Task pools: " + e);
				if (failure == null)
					failure = e;
				else
					failure.addSuppressed(e);
			} finally {
				worker.shutdownNow();
				scheduler.shutdownNow();
				ThreadDiagnosable.stopDiagnose();
			}
		}
		if (failure != null)
			throw new AssertionError("SCENARIO_FAILURE " + args[0], failure);
		System.out.println("PASS " + args[0]);
		System.out.flush();
	}
}
