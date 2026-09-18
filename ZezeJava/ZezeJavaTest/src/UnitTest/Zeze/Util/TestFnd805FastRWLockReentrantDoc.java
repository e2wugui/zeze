package UnitTest.Zeze.Util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import Zeze.Util.FastRWLock;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND8-05回归（文档成文，实现不改）：FastRWLock类注释原称"只支持读锁与读锁的重入"，
 * 但readLock()对写等待标记置位（c&lt;0）一律无界等待——持读者重入恰与等待写者互相
 * 等待永久楔死。契约修正为"只支持无写等待时的读锁嵌套；需要嵌套读用tryReadLock()
 * 失败自处理"。本测试钉住文档指向的fail-fast原语：写等待置位后tryReadLock立即false，
 * 读者退光后写者照常获得写锁。
 */
@Fast
public class TestFnd805FastRWLockReentrantDoc {

	@Test
	public void testTryReadLockFailFastUnderWriteWait() throws Exception {
		var lock = new FastRWLock();
		lock.readLock(); // 主线程持读（state=1）

		var writerDone = new CountDownLatch(1);
		var writer = new Thread(() -> {
			lock.writeLock(); // 置写等待标记后忙等存量读者退光
			try {
				writerDone.countDown();
			} finally {
				lock.writeUnlock();
			}
		}, "fnd805-writer");
		writer.setDaemon(true);
		writer.start();

		// 轮询等待写等待标记置位（state转负），不用裸sleep赌时序
		var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while (lock.get() >= 0) {
			Assertions.assertTrue(System.nanoTime() < deadline, "写等待标记必须在5秒内置位");
			Thread.onSpinWait();
		}

		// 嵌套读的文档化出路：tryReadLock立即失败，不与写者互等楔死
		Assertions.assertFalse(lock.tryReadLock(), "写等待置位后tryReadLock必须fail-fast");

		// 读者退光后写者照常升级写独占并收尾
		lock.readUnlock();
		Assertions.assertTrue(writerDone.await(5, TimeUnit.SECONDS), "读者退光后写者必须获得写锁");
		writer.join(5000);
		Assertions.assertEquals(0, lock.get());

		// 锁回到可用状态
		lock.readLock();
		lock.readUnlock();
		Assertions.assertEquals(0, lock.get());
	}
}
