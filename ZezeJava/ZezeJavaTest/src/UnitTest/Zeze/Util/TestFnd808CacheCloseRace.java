package UnitTest.Zeze.Util;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.Util.Cache;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.rocksdb.RocksDBException;

/**
 * FND8-08回归：Cache.get入口校验通过后进入用户loader（任意时长），期间close()完成
 * ——loader返回后dbSave直读字段null即NPE（应为干净的closed错误）；appendToday在
 * close后todayFile为null时写流NPE、跨天分支关后重开新流泄漏句柄。修复：close在
 * todayLock临界区内置closed终态标志，appendToday锁内首查closed；dbSave快照判空。
 */
@Fast
public class TestFnd808CacheCloseRace {

	@FunctionalInterface
	private interface CacheOpener {
		Cache open() throws Exception;
	}

	// Windows下全新目录的RocksDB.open也可能在000005.dbtmp→CURRENT重命名时被拒（杀软/索引器
	// 与重命名抢句柄，环境性偶发），重试兜底，重试穷尽后原样抛出。
	private static Cache openCache(CacheOpener opener) throws Exception {
		for (int attempt = 0; ; attempt++) {
			try {
				return opener.open();
			} catch (RocksDBException e) {
				if (attempt >= 4)
					throw e;
				Thread.sleep(500);
			}
		}
	}

	/** loader执行期间close完成：dbSave必须抛干净的closed错误而非NPE，且不写库不写清单。 */
	@Test
	public void testCloseDuringLoaderThrowsCleanClosed(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var dir = tempDir.resolve("a1_fnd808_cache").toString();
		var loaderEntered = new CountDownLatch(1);
		var releaseLoader = new CountDownLatch(1);
		var cache = openCache(() -> new Cache(dir, 16,
				id -> {
					loaderEntered.countDown();
					try {
						releaseLoader.await(); // 模拟长时间loader，期间close()完成
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					return new TestCacheAppendTodayResume.Obj(id, "v1");
				},
				(id, bb) -> {
					var o = new TestCacheAppendTodayResume.Obj();
					o.decode(bb);
					return o;
				}));

		var getThrown = new AtomicReference<Throwable>();
		var getter = new Thread(() -> {
			try {
				cache.get("k1");
			} catch (Throwable e) {
				getThrown.compareAndSet(null, e);
			}
		}, "fnd808-getter");
		getter.setDaemon(true);
		getter.start();
		Assertions.assertTrue(loaderEntered.await(5, TimeUnit.SECONDS), "loader必须先执行");

		cache.close(); // loader仍在执行：close完整跑完
		releaseLoader.countDown();
		getter.join(5000);
		Assertions.assertFalse(getter.isAlive());

		// 修复前红：dbSave读null字段NPE
		var ex = getThrown.get();
		Assertions.assertNotNull(ex, "close期间loader返回后get必须明确失败");
		Assertions.assertInstanceOf(IllegalStateException.class, ex, "必须抛closed错误而非NPE");
		Assertions.assertTrue(ex.getMessage().contains("cache is closed"), ex.getMessage());
	}

	/** close后appendToday必须抛closed错误：不写已关流（B1的NPE）、不重开新流（B2的句柄泄漏）。 */
	@Test
	public void testAppendTodayAfterCloseThrows(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var dir = tempDir.resolve("a1_fnd808_cache2").toString();
		var cache = openCache(() -> new Cache(dir, 16,
				id -> new TestCacheAppendTodayResume.Obj(id, "v1"),
				(id, bb) -> {
					var o = new TestCacheAppendTodayResume.Obj();
					o.decode(bb);
					return o;
				}));
		Assertions.assertNotNull(cache.get("k1")); // 当天清单流已建立
		cache.close();

		// appendToday是当天清单唯一写入口（私有）：反射直调钉住close后的密闭检查
		var method = Cache.class.getDeclaredMethod("appendToday", String.class);
		method.setAccessible(true);
		var ex = Assertions.assertThrows(InvocationTargetException.class, () -> method.invoke(cache, "k2"));
		// 修复前红：todayFile为null直接NPE（同日），或跨天分支关后重开新流泄漏
		Assertions.assertInstanceOf(IllegalStateException.class, ex.getCause());
		Assertions.assertTrue(ex.getCause().getMessage().contains("cache is closed"), ex.getCause().getMessage());
	}
}
