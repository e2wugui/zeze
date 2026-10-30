package Zeze.Util;

import java.io.File;
import java.net.URLClassLoader;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.FileSystemException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import com.sun.nio.file.ExtendedOpenOption;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

	@Test
	@EnabledOnOs(OS.WINDOWS)
	public void metadataReadersDoNotMasqueradeAsDatabaseLockLeaks(@TempDir Path dir) throws Exception {
		runChild(dir, "cache-current-reader");
	}

	@Test
	public void lockProbeDistinguishesLiveAndClosedDatabase(@TempDir Path dir) throws Exception {
		runChild(dir, "lock-control");
	}

	private static void runChild(Path dir, String scenario, String... arguments) throws Exception {
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
		var output = dir.resolve("child-" + scenario + String.join("-", arguments) + ".log");
		var command = new ArrayList<String>();
		Collections.addAll(command, ProcessHandle.current().info().command().orElseThrow(), "-ea",
				"-cp", String.join(File.pathSeparator, entries),
				TestConstructorResourceRollback.class.getName(), dir.toString(), scenario);
		Collections.addAll(command, arguments);
		var child = new ProcessBuilder(command)
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
		@Nullable Path currentToRead;
		@Nullable FileChannel currentReader;

		RejectingScheduler() {
			super(1);
		}

		@Override
		public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay,
				long delay, TimeUnit unit) {
			if (remaining-- == 0) {
				if (currentToRead != null) {
					try {
						currentReader = FileChannel.open(currentToRead, StandardOpenOption.READ,
								ExtendedOpenOption.NOSHARE_DELETE);
					} catch (java.io.IOException e) {
						throw new AssertionError(e);
					}
				}
				throw new RejectedExecutionException("cleanup registration rejected");
			}
			var future = super.scheduleWithFixedDelay(command, initialDelay, delay, unit);
			registered.add(future);
			return future;
		}
	}

	public static void main(String[] args) throws Exception {
		var dir = Path.of(args[0]);
		var dbName = dir.resolve("db").toString();
		if (args[1].equals("database-lock")) {
			// Must run in another process: POSIX record locks are owned by PID, not by Java channel.
			var lockFile = Path.of(dbName, "LOCK");
			boolean available = Boolean.parseBoolean(args[2]);
			try (var channel = FileChannel.open(lockFile, StandardOpenOption.READ);
				 var lock = channel.tryLock(0, Long.MAX_VALUE, true)) {
				if (available)
					assertNotNull(lock, "构造失败后原生数据库锁必须已释放");
				else
					assertNull(lock, "探针必须识别另一个进程仍持有的原生数据库锁");
			} catch (FileSystemException denied) {
				// Windows RocksDB holds LOCK without read sharing, so contention can reject open itself.
				// The released-state assertion must still open and lock successfully; it never accepts I/O failure.
				if (File.separatorChar != '\\' || available)
					throw denied;
				assertTrue(Files.isRegularFile(lockFile), () -> "锁文件必须存在: " + denied);
			}
			return;
		}
		if (args[1].equals("lock-control")) {
			RocksDB.loadLibrary();
			try (var db = RocksDB.open(dbName)) {
				runChild(dir, "database-lock", "false");
			}
			runChild(dir, "database-lock", "true");
			return;
		}
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
				if (args[1].equals("cache-current-reader"))
					scheduler.currentToRead = Path.of(dbName, "CURRENT");
				assertThrows(RejectedExecutionException.class,
						() -> new Cache(dbName, 10, id -> null, (id, bb) -> null));
				assertEquals(expectedTimers, scheduler.registered.size());
				if (scheduler.currentReader != null) {
					// This unrelated reader reproduces the old oracle's failure despite successful cleanup.
					var failure = assertThrows(RocksDBException.class, () -> RocksDB.open(dbName));
					assertTrue(failure.getMessage().contains("Failed to rename:"));
					assertTrue(failure.getMessage().contains("CURRENT"));
				}
				runChild(dir, "database-lock", "true");
			}
			for (var future : scheduler.registered)
				assertTrue(future.isCancelled(), "构造失败必须取消已经注册的任务");
		} finally {
			if (scheduler.currentReader != null)
				scheduler.currentReader.close();
			Task.shutdownNow(5000);
		}
	}
}
