package Zeze.Util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.TimeUnit;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Fast
public class TestCounterInitializationOrder {
	@Test
	public void noopMayInitializeBeforeTheCounterInterface(@TempDir Path dir) throws Exception {
		runChild(dir, "noop-first", "null");
	}

	@Test
	public void emptySettingSupportsNoopInitializationFirst(@TempDir Path dir) throws Exception {
		runChild(dir, "noop-first", "");
	}

	@Test
	public void noopDoesNotSelectTheGlobalImplementation(@TempDir Path dir) throws Exception {
		// 全局配置故意无效；独立空实现仍应能够初始化和使用。
		runChild(dir, "noop-only", "unavailable.CounterImplementation");
	}

	@Test
	public void interfaceMayInitializeFirst(@TempDir Path dir) throws Exception {
		runChild(dir, "interface-first", "null");
	}

	@Test
	public void implementationAndInterfaceMayInitializeConcurrently(@TempDir Path dir) throws Exception {
		runChild(dir, "concurrent", "null");
	}

	private static void runChild(Path dir, String order, String setting) throws Exception {
		var entries = new LinkedHashSet<String>();
		for (var type : new Class<?>[]{TestCounterInitializationOrder.class, NoopCounter.class,
				org.apache.logging.log4j.LogManager.class})
			entries.add(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toString());
		var output = dir.resolve("child.log");
		var child = new ProcessBuilder(ProcessHandle.current().info().command().orElseThrow(),
				"-DZezeCounter=" + setting, "-cp", String.join(File.pathSeparator, entries),
				TestCounterInitializationOrder.class.getName(), order)
				.redirectErrorStream(true).redirectOutput(output.toFile()).start();
		try {
			assertTrue(child.waitFor(20, TimeUnit.SECONDS), "统计器冷初始化应在期限内结束");
			assertEquals(0, child.exitValue(), () -> {
				try {
					return Files.readString(output);
				} catch (java.io.IOException e) {
					return e.toString();
				}
			});
		} finally {
			if (child.isAlive()) {
				child.destroyForcibly();
				assertTrue(child.waitFor(5, TimeUnit.SECONDS));
			}
		}
	}

	public static void main(String[] args) throws Exception {
		if (args[0].equals("concurrent")) {
			var start = new CountDownLatch(1);
			var failure = new AtomicReference<@Nullable Throwable>();
			var threads = new Thread[2];
			var names = new String[]{"Zeze.Util.NoopCounter", "Zeze.Util.ZezeCounter"};
			for (int i = 0; i < threads.length; i++) {
				var name = names[i];
				threads[i] = Thread.ofPlatform().daemon().start(() -> {
					try {
						start.await();
						Class.forName(name);
					} catch (Throwable e) {
						failure.compareAndSet(null, e);
					}
				});
			}
			start.countDown();
			for (var thread : threads) {
				thread.join(5000);
				if (thread.isAlive())
					throw new AssertionError("counter class initialization deadlocked");
			}
			if (failure.get() != null)
				throw new AssertionError("counter class initialization failed", failure.get());
		} else if (args[0].equals("noop-first") || args[0].equals("noop-only")) {
			checkNoopCounter(new NoopCounter());
			if (args[0].equals("noop-only"))
				return;
		}
		if (ZezeCounter.instance == null)
			throw new AssertionError("ZezeCounter.instance is null");
		if (ZezeCounter.ENABLE)
			throw new AssertionError("disabled counter enabled statistics");
		if (!(ZezeCounter.instance instanceof NoopCounter))
			throw new AssertionError("disabled counter is not a no-op implementation");
		checkNoopCounter(ZezeCounter.instance);
	}

	private static void checkNoopCounter(ZezeCounter counter) {
		if (counter.allocCounter("probe") == null)
			throw new AssertionError("no-op counter handle is null");
		counter.init();
		if (counter.snapshotAndReset() != ZezeCounter.Snapshot.EMPTY)
			throw new AssertionError("invalid collected snapshot");
		if (counter.getLastSnapshot() != ZezeCounter.Snapshot.EMPTY)
			throw new AssertionError("invalid initial snapshot");
	}
}
