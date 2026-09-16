package UnitTest.Zeze.Trans;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.DatabaseMemory;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND7-03 回归：DatabaseMemory 静态共享 dataWithVersions 用实例级锁（Database extends
 * ReentrantLock 的 lock()）守卫。同 JVM 多个 Memory 库实例（同 URL）并发执行 Operates
 * 时互不互斥，非互斥并发写可损坏 HashMap；与静态 clear() 的静态写锁也不互斥。
 * 修复后统一用静态读写锁（对齐 databaseTables 的守卫约定）。
 */
@Fast
public class TestFnd703MemoryDbStaticLock {
	private static final String url = "t1_fnd703_memdb_static_lock";

	private static DatabaseMemory newDb() {
		var conf = new Config.DatabaseConf();
		conf.setDatabaseUrl(url);
		return new DatabaseMemory(null, conf); // 默认 DisableOperates=false，Operates 即库自身
	}

	private static ByteBuffer newBb(String s) {
		var bb = ByteBuffer.Allocate();
		bb.WriteString(s);
		return bb;
	}

	@Test
	public void testOperatesGuardedByStaticLock() throws Exception {
		var dbA = newDb();
		var dbB = newDb(); // 同URL第二实例
		try {
			// sanity：同URL共享分区，A保存B可见。
			var key = newBb("zeze.Schemas.V4.0");
			Assertions.assertTrue(dbA.saveDataWithSameVersion(key, newBb("a"), 0).getValue());
			Assertions.assertNotNull(dbB.getDataWithVersion(key));

			Field lockField = DatabaseMemory.class.getDeclaredField("lock");
			lockField.setAccessible(true);
			var staticLock = (ReentrantReadWriteLock)lockField.get(null);

			// 1) 持静态写锁期间，另一实例的save必须阻塞（修复前用实例锁，立即完成不阻塞）。
			var wl = staticLock.writeLock();
			wl.lock();
			final var saved = new CountDownLatch(1);
			var ts = new Thread(() -> {
				dbB.saveDataWithSameVersion(newBb("fnd703-s"), newBb("x"), 0);
				saved.countDown();
			});
			ts.start();
			Assertions.assertFalse(saved.await(300, TimeUnit.MILLISECONDS),
					"saveDataWithSameVersion必须受静态写锁守卫（跨实例互斥、与clear()互斥）");
			wl.unlock();
			Assertions.assertTrue(saved.await(5, TimeUnit.SECONDS), "释放静态锁后save必须完成");
			ts.join();

			// 2) 持静态写锁期间，getDataWithVersion（读方法）同样必须阻塞。
			wl.lock();
			final var loaded = new CountDownLatch(1);
			var tg = new Thread(() -> {
				dbA.getDataWithVersion(key);
				loaded.countDown();
			});
			tg.start();
			Assertions.assertFalse(loaded.await(300, TimeUnit.MILLISECONDS),
					"getDataWithVersion必须受静态读写锁守卫");
			wl.unlock();
			Assertions.assertTrue(loaded.await(5, TimeUnit.SECONDS), "释放静态锁后get必须完成");
			tg.join();
		} finally {
			dbA.close();
			dbB.close();
			// 不调用DatabaseMemory.clear()（R2-T复审）：clear是JVM级全局静态清空，@Fast套件8路
			// 类级并行下会波及同JVM其他正在使用Memory库的测试（先例fd334d7f8/a7450912c）。
			// 本测试独占URL分区"t1_fnd703_memdb_static_lock"，两方法的键空间互不相交
			// （zeze.Schemas.V4.0/fnd703-s vs fnd703-k0..31），残留互不可见，无需全局清空。
		}
	}

	@Test
	public void testConcurrentVersionCasExactlyOneWinner() throws Exception {
		var dbA = newDb();
		var dbB = newDb();
		final int keys = 32, rounds = 200;
		try {
			// 预建条目（version=0），此后每轮两实例并发以当前版本CAS，恰好一个赢家。
			var keyList = new ArrayList<ByteBuffer>();
			for (var k = 0; k < keys; k++)
				keyList.add(newBb("fnd703-k" + k));
			for (var key : keyList)
				dbA.saveDataWithSameVersion(key, newBb("init"), 0);

			for (var roundIdx = 0; roundIdx < rounds; roundIdx++) {
				final var round = roundIdx;
				final long version = round; // 每轮恰好一次成功保存，版本恰好+1。
				final var winners = new AtomicInteger();
				final var start = new CountDownLatch(1);
				var threads = new ArrayList<Thread>();
				for (var t = 0; t < 2; t++) {
					final var db = t == 0 ? dbA : dbB;
					var th = new Thread(() -> {
						try {
							start.await();
						} catch (InterruptedException e) {
							return;
						}
						for (var key : keyList) {
							var r = db.saveDataWithSameVersion(key, newBb("r" + round), version);
							if (r.getValue())
								winners.incrementAndGet();
						}
					});
					th.start();
					threads.add(th);
				}
				start.countDown();
				for (var th : threads)
					th.join();
				Assertions.assertEquals(keys, winners.get(),
						"第" + round + "轮每key必须恰好一个CAS赢家（实例间必须互斥）");
			}
		} finally {
			dbA.close();
			dbB.close();
			// 同上：不清全局静态桶（R2-T复审，并行测试隔离）。
		}
	}
}
