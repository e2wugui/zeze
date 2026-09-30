package Zeze.Component;

import java.lang.ref.SoftReference;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import Zeze.Application;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.SQLStatement;
import Zeze.Transaction.Bean;
import Zeze.Transaction.CheckpointFlushMode;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Record;
import Zeze.Transaction.TableX;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** 容量驱逐与同键重装载交错后，清除软引用仍能通过表接口读到已经提交的值。 */
@Fast
public class TestTableCacheReloadDuringEviction {
	private static final long KEY = 1L;

	@Test
	public void testReloadedValueSurvivesEvictionAndSoftReferenceCollection() throws Exception {
		var conf = TakeoverTestEnv.newConf("off", 60_000, 60_000);
		conf.setCheckpointPeriod(3_600_000);
		conf.setCheckpointFlushMode(CheckpointFlushMode.SingleThread);
		var tableConf = new Config.TableConf();
		tableConf.setCacheCapacity(0);
		tableConf.setCacheCleanPeriod(3_600_000); // 本测试显式驱动容量清理。
		tableConf.setCacheNewLruHotPeriod(10); // 由真实周期任务把种子记录转为冷记录。
		var table = new ReloadTable();
		conf.getTableConfMap().put(table.getName(), tableConf);
		var app = new Application("TestTableCacheReloadDuringEviction", conf);
		app.addTable("", table);
		var failure = new AtomicReference<Throwable>();
		var readerEntered = new CountDownLatch(1);
		var readerLoaded = new CountDownLatch(1);
		Thread cleaner = null;
		Thread reader = null;
		try {
			app.start();
			Assertions.assertEquals(Procedure.Success, app.newProcedure(() -> {
				var value = new Value();
				value.number = 42;
				table.put(KEY, value);
				return Procedure.Success;
			}, "CacheReload.seed").call());
			app.checkpointRun();
			var oldRecord = table.getCache().getOrAdd(KEY, () -> {
				throw new AssertionError("The seeded record must be resident");
			});
			cleaner = new Thread(() -> {
				try {
					long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
					while (table.beforeMirrorDelete.getCount() != 0 && System.nanoTime() < deadline) {
						table.getCache().cleanNow();
						Thread.sleep(10);
					}
				} catch (Throwable e) {
					failure.compareAndSet(null, e);
				}
			}, "CacheReload.cleaner");
			table.cleanerThread = cleaner;
			cleaner.start();
			Assertions.assertTrue(table.beforeMirrorDelete.await(5, TimeUnit.SECONDS),
					"Capacity eviction must reach mirror cleanup");

			reader = new Thread(() -> {
				try {
					Assertions.assertEquals(Procedure.Success, app.newProcedure(() -> {
						readerEntered.countDown();
						var value = table.get(KEY);
						Assertions.assertNotNull(value);
						Assertions.assertEquals(42, value.number);
						readerLoaded.countDown();
						return Procedure.Success;
					}, "CacheReload.concurrentRead").call());
				} catch (Throwable e) {
					failure.compareAndSet(null, e);
				}
			}, "CacheReload.reader");
			reader.start();
			Assertions.assertTrue(readerEntered.await(5, TimeUnit.SECONDS));
			// 允许两种安全实现：读者已经完成装载，或者等待旧记录的 fairLock。
			// 不要求修复后仍先移除 dataMap，避免门控强制维持有缺陷的执行顺序。
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
			while (readerLoaded.getCount() != 0 && !oldRecord.hasQueuedThread(reader)
					&& failure.get() == null && System.nanoTime() < deadline)
				LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
			Assertions.assertTrue(readerLoaded.getCount() == 0 || oldRecord.hasQueuedThread(reader),
					"The concurrent reader must reach the controlled eviction window");
			tableConf.setCacheCapacity(1_000); // 本轮驱逐后保持重装载记录驻留，以单独模拟软引用回收。
			table.resumeMirrorDelete.countDown();
			cleaner.join(5_000);
			reader.join(5_000);
			Assertions.assertFalse(cleaner.isAlive());
			Assertions.assertFalse(reader.isAlive());
			Assertions.assertNull(failure.get(), "Controlled eviction and concurrent read must complete");

			var record = table.getCache().getOrAdd(KEY, () -> {
				throw new AssertionError("The concurrent read must leave a resident record");
			});
			// 精确模拟 GC 清除 clean 记录的软引用，不删除或直接改写镜像。
			var softValue = Record.class.getDeclaredField("softValue");
			softValue.setAccessible(true);
			((SoftReference<?>)softValue.get(record)).clear();
			var observed = new AtomicReference<Value>();
			Assertions.assertEquals(Procedure.Success, app.newProcedure(() -> {
				observed.set(table.get(KEY));
				return Procedure.Success;
			}, "CacheReload.afterCollection").call());
			Assertions.assertNotNull(observed.get(), "Eviction must preserve the reloaded value's mirror");
			Assertions.assertEquals(42, observed.get().number);
		} finally {
			table.resumeMirrorDelete.countDown();
			if (cleaner != null)
				cleaner.join(5_000);
			if (reader != null)
				reader.join(5_000);
			app.stop();
		}
	}

	public static final class Value extends Bean {
		long number;
		@Override public void encode(ByteBuffer bb) { bb.WriteLong(number); }
		@Override public void decode(IByteBuffer bb) { number = bb.ReadLong(); }
		@Override public Value copy() { var value = new Value(); value.number = number; return value; }
	}

	public static final class ReloadTable extends TableX<Long, Value> {
		final CountDownLatch beforeMirrorDelete = new CountDownLatch(1);
		final CountDownLatch resumeMirrorDelete = new CountDownLatch(1);
		final AtomicBoolean pauseOnce = new AtomicBoolean(true);
		volatile Thread cleanerThread;

		ReloadTable() {
			super(990_932, "UnitTest_TableCacheReloadDuringEviction");
		}

		@Override public Class<Long> getKeyClass() { return Long.class; }
		@Override public Class<Value> getValueClass() { return Value.class; }
		@Override public Value newValue() { return new Value(); }
		@Override public Long decodeKey(ByteBuffer bb) { return bb.ReadLong(); }
		@Override public Long decodeKeyResultSet(ResultSet rs) throws SQLException { return rs.getLong("__key"); }
		@Override public void encodeKeySQLStatement(SQLStatement st, Long key) { st.appendLong("__key", key); }

		@Override
		public ByteBuffer encodeKey(Long key) {
			if (Thread.currentThread() == cleanerThread && pauseOnce.compareAndSet(true, false)) {
				beforeMirrorDelete.countDown();
				try {
					if (!resumeMirrorDelete.await(10, TimeUnit.SECONDS))
						throw new IllegalStateException("Timed out waiting for concurrent reload");
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new IllegalStateException(e);
				}
			}
			var bb = ByteBuffer.Allocate(8);
			bb.WriteLong(key);
			return bb;
		}
	}
}
