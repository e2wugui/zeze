package UnitTest.Zeze.Util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import Zeze.Util.RocksDatabase;
import harness.Fast;

/**
 * RocksDatabase close-safe 契约（FND19-22 复盘：契约由"调用方先静默"下沉到资源层）：
 * 1. close 后迟到的数据通路调用抛 IllegalStateException（可捕获，非JNI崩溃）；
 * 2. close 与在飞调用并发：有界排空等待（迭代器未关闭会阻挡排空，关闭后放行）；
 * 3. 并发 put 线程与 close 同跑不崩溃（旧契约下此场景即 GC-C01 活体复刻的
 *    EXCEPTION_ACCESS_VIOLATION——本测试绿即新契约成立的端到端证据，红证据见
 *    audit 案卷留档的 hs_err，不在本 JVM 复现以免击杀测试进程）。
 */
@Fast
public class TestRocksDatabaseCloseSafe {

	@Test
	public void testLateOpsAfterCloseRejected(@TempDir Path tempDir) throws Exception {
		var db = new RocksDatabase(tempDir.resolve("db1").toString());
		var table = db.getOrAddTable("t");
		table.put(new byte[]{1}, new byte[]{1});
		db.close();
		assertTrue(db.isClosed());
		assertThrows(IllegalStateException.class, () -> table.put(new byte[]{2}, new byte[]{2}),
				"迟到put必须以IllegalStateException拒绝");
		assertThrows(IllegalStateException.class, () -> table.get(new byte[]{1}),
				"迟到get必须以IllegalStateException拒绝");
		assertThrows(IllegalStateException.class, table::iterator,
				"迟到迭代器创建必须以IllegalStateException拒绝");
	}

	@Test
	public void testConcurrentPutVsCloseNoCrash(@TempDir Path tempDir) throws Exception {
		var db = new RocksDatabase(tempDir.resolve("db2").toString());
		var table = db.getOrAddTable("t");
		table.put(new byte[]{1}, new byte[]{1});
		var rejected = new CountDownLatch(1);
		var writer = new Thread(() -> {
			var i = 0;
			try {
				//noinspection InfiniteLoopStatement
				while (true)
					table.put(new byte[]{(byte)(i++ & 0xff)}, new byte[]{1});
			} catch (RuntimeException expected) {
				// close-safe 门拒绝（旧契约下此处是JNI崩溃杀死进程）
				rejected.countDown();
			} catch (Exception rocksOrInterrupted) {
				// RocksDBException等checked异常同样以Java异常退出，符合契约
				rejected.countDown();
			}
		}, "close-safe-writer");
		writer.setDaemon(true);
		writer.start();
		db.close(); // 与写入线程并发
		assertTrue(db.isClosed());
		assertTrue(rejected.await(5, TimeUnit.SECONDS), "写入线程必须以可捕获异常退出而非崩溃");
		writer.join(5_000);
	}

	@Test
	public void testOpenIteratorBlocksDrainUntilClosed(@TempDir Path tempDir) throws Exception {
		var db = new RocksDatabase(tempDir.resolve("db3").toString());
		var table = db.getOrAddTable("t");
		table.put(new byte[]{1}, new byte[]{1});
		var it = table.iterator(); // 登记：openIterators 非空
		it.seekToFirst();
		assertTrue(it.isValid());

		var closer = new Thread(db::close, "close-safe-closer");
		closer.setDaemon(true);
		closer.start();
		Thread.sleep(300);
		assertFalse(db.isClosed(), "登记在册的未关闭迭代器必须阻挡排空");
		assertTrue(db.isClosing(), "closing已置位（迟到调用此刻已被拒绝）");

		it.close(); // 迭代器关闭 → 排空放行
		closer.join(10_000);
		assertTrue(db.isClosed(), "迭代器关闭后close必须完成");
	}

	@Test
	public void testNormalLifecycleUnchanged(@TempDir Path tempDir) throws Exception {
		// 正常序列（无并发）：行为零变化——写读删关全链路照旧。
		var db = new RocksDatabase(tempDir.resolve("db4").toString());
		var table = db.getOrAddTable("t");
		table.put(new byte[]{1}, new byte[]{9});
		assertEquals(9, table.get(new byte[]{1})[0]);
		try (var it2 = table.iterator()) {
			it2.seekToFirst();
			assertTrue(it2.isValid());
		}
		table.delete(new byte[]{1});
		db.close();
		assertTrue(db.isClosed());
	}
}
