package UnitTest.Zeze.Util;

import harness.Fast;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Util.FastRWLock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FastRWLock 基本契约测试。
 * <p>
 * FND-U0-1：writeLock 的判断条件 (s &amp; LOCK_MASK) == 0 把 WRITE_LOCK_FLAG 的高位也掩掉，
 * 写锁持有期间第二个写者 CAS(expect==当前值) 平凡成功，写-写不互斥。
 * 修复后：写锁持有期间其他写者必须等待 writeUnlock 才能进入。
 */
@Fast
public class TestFastRWLock {

	/**
	 * 写-写互斥：T1 持有写锁期间，T2 的 writeLock 必须阻塞，直到 T1 writeUnlock。
	 * 修复前 T2 会立即返回（写锁态被掩码判断为"无写独占"），latch 提前计数，断言失败。
	 */
	@Test
	public void testWriteWriteMutex() throws InterruptedException {
		var lock = new FastRWLock();
		var t1Entered = new CountDownLatch(1);
		var t2Exited = new CountDownLatch(1);
		var t1Released = new CountDownLatch(1);
		var writer1 = new Thread(() -> {
			lock.writeLock();
			try {
				t1Entered.countDown();
				// 持有写锁直到主线程确认 T2 被阻塞
				try {
					Assertions.assertTrue(t1Released.await(10, TimeUnit.SECONDS));
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			} finally {
				lock.writeUnlock();
			}
		}, "testFastRWLockWriter1");
		var writer2 = new Thread(() -> {
			lock.writeLock();
			try {
				t2Exited.countDown();
			} finally {
				lock.writeUnlock();
			}
		}, "testFastRWLockWriter2");
		writer1.start();
		Assertions.assertTrue(t1Entered.await(10, TimeUnit.SECONDS));
		writer2.start();
		// 修复前：T2 平凡 CAS 成功立即进入，200ms 内 t2Exited 计数；修复后必须等到释放。
		Assertions.assertFalse(t2Exited.await(200, TimeUnit.MILLISECONDS),
				"writeLock must be exclusive with an existing writer");
		t1Released.countDown();
		Assertions.assertTrue(t2Exited.await(10, TimeUnit.SECONDS));
		writer1.join(10_000);
		writer2.join(10_000);
	}

	/**
	 * 写锁持有时读锁被阻止；写释放后读锁可用。
	 */
	@Test
	public void testWriteBlocksRead() throws InterruptedException {
		var lock = new FastRWLock();
		var writeEntered = new CountDownLatch(1);
		var released = new CountDownLatch(1);
		var readerExited = new CountDownLatch(1);
		var writer = new Thread(() -> {
			lock.writeLock();
			try {
				writeEntered.countDown();
				try {
					Assertions.assertTrue(released.await(10, TimeUnit.SECONDS));
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			} finally {
				lock.writeUnlock();
			}
		}, "testFastRWLockWriter");
		writer.start();
		Assertions.assertTrue(writeEntered.await(10, TimeUnit.SECONDS));
		Assertions.assertFalse(lock.tryReadLock());
		var reader = new Thread(() -> {
			lock.readLock();
			try {
				readerExited.countDown();
			} finally {
				lock.readUnlock();
			}
		}, "testFastRWLockReader");
		reader.start();
		Assertions.assertFalse(readerExited.await(200, TimeUnit.MILLISECONDS));
		released.countDown();
		Assertions.assertTrue(readerExited.await(10, TimeUnit.SECONDS));
		writer.join(10_000);
		reader.join(10_000);
	}

	/**
	 * 读者存在时写者等待；全部读者退出后写者获得写锁（写等待标记升级路径）。
	 */
	@Test
	public void testWriteWaitsReadersDrain() throws InterruptedException {
		var lock = new FastRWLock();
		Assertions.assertTrue(lock.tryReadLock());
		Assertions.assertTrue(lock.tryReadLock());
		var writeExited = new CountDownLatch(1);
		var allowWrite = new CountDownLatch(1);
		var writer = new Thread(() -> {
			lock.writeLock();
			try {
				writeExited.countDown();
				Assertions.assertTrue(allowWrite.await(10, TimeUnit.SECONDS));
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} finally {
				lock.writeUnlock();
			}
		}, "testFastRWLockDrainWriter");
		writer.start();
		Assertions.assertFalse(writeExited.await(200, TimeUnit.MILLISECONDS));
		lock.readUnlock();
		Assertions.assertFalse(writeExited.await(200, TimeUnit.MILLISECONDS));
		lock.readUnlock(); // 最后一个读者退出，写者应获得锁
		Assertions.assertTrue(writeExited.await(10, TimeUnit.SECONDS));
		allowWrite.countDown();
		writer.join(10_000);
		Assertions.assertEquals(0, lock.get());
	}

	/**
	 * 多读并发计数 + 多写互斥回归：读者可并行，写者串行修改共享值。
	 */
	@Test
	public void testConcurrentReadWrite() throws InterruptedException {
		var lock = new FastRWLock();
		final int threads = 8, loops = 2000;
		var value = new int[1];
		var maxWriters = new AtomicInteger();
		var writers = new AtomicInteger();
		var ts = new Thread[threads];
		for (int i = 0; i < threads; i++) {
			var isWriter = (i & 1) == 0;
			(ts[i] = new Thread(() -> {
				for (int j = 0; j < loops; j++) {
					if (isWriter) {
						lock.writeLock();
						try {
							var w = writers.incrementAndGet();
							maxWriters.accumulateAndGet(w, Math::max);
							var v = value[0];
							value[0] = v + 1;
							writers.decrementAndGet();
						} finally {
							lock.writeUnlock();
						}
					} else {
						lock.readLock();
						try {
							var ignored = value[0]; // 读不要求看到最新，只要求不破坏结构
						} finally {
							lock.readUnlock();
						}
					}
				}
			}, "testFastRWLockMixed-" + i)).start();
		}
		for (var t : ts)
			t.join(30_000);
		Assertions.assertEquals(threads / 2 * loops, value[0]);
		Assertions.assertEquals(1, maxWriters.get(), "writers must never overlap");
		Assertions.assertEquals(0, lock.get());
	}
}
