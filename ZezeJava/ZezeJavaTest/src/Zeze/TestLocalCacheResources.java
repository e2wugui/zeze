package Zeze;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Transaction.DatabaseRocksDb;
import Zeze.Util.FileMutex;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 本地缓存的部分获取、发布和失败清理契约；故障注入只替换当前实例的文件/数据库操作。 */
@Fast
public class TestLocalCacheResources {
	private static final AtomicInteger nextServerId = new AtomicInteger(FastServerIds.TEST_LOCAL_CACHE_RESOURCES);
	private Application newApp(LocalCacheResources.Operations operations) throws Exception {
		var conf = new Config();
		conf.setServerId(nextServerId.getAndIncrement());
		conf.setServiceManager("disable");
		conf.setDefaultTableConf(new Config.TableConf());
		var db = new Config.DatabaseConf();
		db.setDatabaseUrl("local_cache_resources_" + conf.getServerId());
		conf.getDatabaseConfMap().put("", db);
		var app = new Application("LocalCacheResources-" + conf.getServerId(), conf);
		var cacheField = Application.class.getDeclaredField("localCache");
		cacheField.setAccessible(true);
		cacheField.set(app, new LocalCacheResources(operations));
		return app;
	}

	private static File directory(Application app) {
		return new File("zeze_cache_" + app.getConfig().getServerId());
	}

	private static void assertLocked(Application app) {
		assertThrows(IllegalStateException.class,
				() -> FileMutex.acquire(directory(app) + ".lock", "test"));
	}

	private static void assertReleased(Application app) {
		try (var ignored = FileMutex.acquire(directory(app) + ".lock", "test")) {
			assertTrue(new File(directory(app) + ".lock").exists());
		}
	}

	@Test
	void deleteFailureKeepsLockUntilStopWithoutDeletingAgain() throws Exception {
		var failure = new IOException("initial delete");
		var app = newApp(new LocalCacheResources.Operations() {
			@Override
			void deleteCacheDirectory(File directory) throws IOException {
				throw failure;
			}
		});
		Files.createDirectories(directory(app).toPath());
		var marker = Files.createFile(directory(app).toPath().resolve("keep"));
		try {
			assertSame(failure, assertThrows(IOException.class, app::start));
			assertEquals(Application.StartState.eStarting, app.getStartState());
			assertNull(app.getLocalRocksCacheDb());
			assertLocked(app);
		} finally {
			app.stop();
		}
		assertReleased(app);
		assertTrue(Files.exists(marker));
		LocalCacheResources.deleteDirectory(directory(app));
	}

	@Test
	void constructorFailureKeepsLockAndResidualDirectoryUntilStop() throws Exception {
		var failure = new IllegalArgumentException("database constructor");
		var app = newApp(new LocalCacheResources.Operations() {
			@Override
			DatabaseRocksDb createDatabase(Application application, Config.DatabaseConf conf) {
				assertTrue(new File(conf.getDatabaseUrl()).mkdirs());
				throw failure;
			}
		});
		try {
			assertSame(failure, assertThrows(IllegalArgumentException.class, app::start));
			assertLocked(app);
			assertNull(app.getLocalRocksCacheDb());
		} finally {
			app.stop();
		}
		assertReleased(app);
		assertTrue(directory(app).isDirectory());
		LocalCacheResources.deleteDirectory(directory(app));
	}

	@Test
	void openFailurePublishesDatabaseAndDefersCleanupUntilStop() throws Exception {
		var failure = new IllegalStateException("open tables");
		var app = newApp(new LocalCacheResources.Operations() {
			@Override
			DatabaseRocksDb createDatabase(Application application, Config.DatabaseConf conf) {
				return new DatabaseRocksDb(application, conf, true) {
					@Override
					public Zeze.Transaction.Database.Table[] openTables(String[] names, int[] ids) {
						assertSame(this, application.getLocalRocksCacheDb());
						throw failure;
					}
				};
			}
		});
		try {
			assertSame(failure, assertThrows(IllegalStateException.class, app::start));
			assertNotNull(app.getLocalRocksCacheDb());
			assertLocked(app);
		} finally {
			app.stop();
		}
		assertNull(app.getLocalRocksCacheDb());
		assertFalse(directory(app).exists());
		assertReleased(app);
		app.stop();
	}

	@Test
	void closeFailureSkipsDeletionAndStillReleasesLock() throws Exception {
		var app = newApp(new LocalCacheResources.Operations() {
			@Override
			DatabaseRocksDb createDatabase(Application application, Config.DatabaseConf conf) {
				return new DatabaseRocksDb(application, conf, true) {
					@Override
					public void close() {
						assertSame(this, application.getLocalRocksCacheDb());
						super.close(); // 真实 native 句柄先释放，模拟关闭后报告失败。
						throw new IllegalStateException("close");
					}
				};
			}
		});
		try {
			app.start();
		} finally {
			app.stop();
		}
		assertEquals(Application.StartState.eStopped, app.getStartState());
		assertNull(app.getLocalRocksCacheDb());
		assertTrue(directory(app).exists());
		assertReleased(app);
		LocalCacheResources.deleteDirectory(directory(app));
	}

	@Test
	void stopDeleteFailureReleasesLock() throws Exception {
		stopDeleteFailureReleasesLockAndPreservesInterrupt(false);
	}

	@Test
	void stopDeleteInterruptionRestoresFlagAndReleasesLock() throws Exception {
		stopDeleteFailureReleasesLockAndPreservesInterrupt(true);
	}

	private void stopDeleteFailureReleasesLockAndPreservesInterrupt(boolean interrupted) throws Exception {
		var app = newApp(new LocalCacheResources.Operations() {
			private boolean first = true;

			@Override
			void deleteCacheDirectory(File directory) throws IOException, InterruptedException {
				if (!first) {
					if (interrupted)
						throw new InterruptedException("stop delete");
					throw new IOException("stop delete");
				}
				first = false;
				super.deleteCacheDirectory(directory);
			}
		});
		try {
			app.start();
		} finally {
			app.stop();
		}
		try {
			assertEquals(interrupted, Thread.currentThread().isInterrupted());
			assertNull(app.getLocalRocksCacheDb());
			assertEquals(Application.StartState.eStopped, app.getStartState());
		} finally {
			Thread.interrupted();
		}
		assertReleased(app);
		LocalCacheResources.deleteDirectory(directory(app));
	}
}
