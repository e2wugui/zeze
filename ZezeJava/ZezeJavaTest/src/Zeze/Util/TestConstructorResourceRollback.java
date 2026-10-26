package Zeze.Util;

import java.io.File;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.rocksdb.RocksDB;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Fast
public class TestConstructorResourceRollback {
	@Test
	public void rejectedLruCleanupCancelsRegisteredRotation(@TempDir Path dir) throws Exception {
		runChild(dir, "lru");
	}

	@Test
	public void rejectedCacheLruReleasesDatabase(@TempDir Path dir) throws Exception {
		runChild(dir, "cache-lru");
	}

	@Test
	public void rejectedCacheCleanupReleasesLruAndDatabase(@TempDir Path dir) throws Exception {
		runChild(dir, "cache-cleanup");
	}

	private static void runChild(Path dir, String scenario) throws Exception {
		// 子进程拥有自己的 Task 池，避免修改并行快速测试的静态调度器。
		var entries = new LinkedHashSet<String>();
		for (var loader = TestConstructorResourceRollback.class.getClassLoader(); loader != null;
				loader = loader.getParent()) {
			if (loader instanceof URLClassLoader urls) {
				for (var url : urls.getURLs())
					entries.add(Path.of(url.toURI()).toString());
			}
		}
		entries.addAll(java.util.List.of(System.getProperty("java.class.path").split(File.pathSeparator)));
		var output = dir.resolve("child.log");
		var child = new ProcessBuilder(ProcessHandle.current().info().command().orElseThrow(), "-ea",
				"-cp", String.join(File.pathSeparator, entries),
				TestConstructorResourceRollback.class.getName(), dir.toString(), scenario)
				.redirectErrorStream(true).redirectOutput(output.toFile()).start();
		try {
			assertTrue(child.waitFor(20, TimeUnit.SECONDS), "构造失败的回收验证应及时结束");
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

	private static final class RejectingScheduler extends ScheduledThreadPoolExecutor {
		final ArrayList<ScheduledFuture<?>> registered = new ArrayList<>();
		int remaining = Integer.MAX_VALUE;

		RejectingScheduler() {
			super(1);
		}

		@Override
		public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay,
				long delay, TimeUnit unit) {
			if (remaining-- == 0)
				throw new RejectedExecutionException("cleanup registration rejected");
			var future = super.scheduleWithFixedDelay(command, initialDelay, delay, unit);
			registered.add(future);
			return future;
		}
	}

	public static void main(String[] args) throws Exception {
		var scheduler = new RejectingScheduler();
		Task.initThreadPool(Executors.newSingleThreadExecutor(), scheduler);
		try {
			scheduler.registered.clear();
			if (args[1].equals("lru")) {
				scheduler.remaining = 1;
				assertThrows(RejectedExecutionException.class,
						() -> new ConcurrentLruLike<>("failed-constructor", 10, null, 30_000, 30_000));
				assertEquals(1, scheduler.registered.size());
			} else {
				RocksDB.loadLibrary();
				scheduler.remaining = args[1].equals("cache-lru") ? 0 : 2;
				var expectedTimers = scheduler.remaining;
				var dbName = Path.of(args[0], "db").toString();
				assertThrows(RejectedExecutionException.class,
						() -> new Cache(dbName, 10, id -> null, (id, bb) -> null));
				assertEquals(expectedTimers, scheduler.registered.size());
				try (var reopened = RocksDB.open(dbName)) {
					assertTrue(reopened.isOwningHandle(), "构造失败后数据库锁必须已释放");
				}
			}
			for (var future : scheduler.registered)
				assertTrue(future.isCancelled(), "构造失败必须取消已经注册的任务");
		} finally {
			Task.shutdownNow(5000);
		}
	}
}
