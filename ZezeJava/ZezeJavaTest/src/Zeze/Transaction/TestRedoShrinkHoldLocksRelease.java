package Zeze.Transaction;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import Zeze.Application;
import Zeze.Config;
import Zeze.Util.TaskSpec;
import demo.Module1.Table3;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Transaction.lockAndCheck 在 redo 后必须释放本轮不再访问的尾部 holdLocks，
 * 以及 AllowDirtyWhenAllRead 退化为纯读时上一轮残留的全部 holdLocks，
 * 避免多余写锁滞留覆盖 finalCommit / whileCommit 等阶段。
 */
@Fast
public class TestRedoShrinkHoldLocksRelease {
	private static Application newApp(Table3 table) throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(FastServerIds.TEST_REDO_SHRINK_HOLD_LOCKS_RELEASE);
		conf.setCheckpointMode(CheckpointMode.Table);
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("redo_shrink_holdlocks_" + conf.getServerId());
		conf.getDatabaseConfMap().put("", dbConf);
		var app = new Application("TestRedoShrinkHoldLocksRelease", conf);
		app.addTable("", table);
		app.start();
		return app;
	}

	private static boolean canOtherThreadAcquireWriteLock(java.util.concurrent.ExecutorService probePool,
														  Lockey lockey) throws Exception {
		return probePool.submit(() -> {
			if (lockey.tryEnterWriteLock(0)) {
				lockey.exitWriteLock();
				return true;
			}
			return false;
		}).get(5, TimeUnit.SECONDS);
	}

	@Test
	public void testTrailingHoldLocksReleasedWhenRedoShrinksAccessSet() throws Exception {
		var table = new Table3();
		var app = newApp(table);
		try (var probePool = Executors.newSingleThreadExecutor()) {
			long k1 = 1L;
			long k5 = 5L;
			Assertions.assertEquals(Procedure.Success, app.newProcedure(() -> {
				table.getOrAdd(k1).setInt_1(10);
				table.getOrAdd(k5).setInt_1(50);
				return Procedure.Success;
			}, "seed").call());

			var lockeyK1 = app.getLocks().get(new TableKey(table.getId(), k1));
			var lockeyK5 = app.getLocks().get(new TableKey(table.getId(), k5));

			var round = new AtomicInteger();
			var round1Loaded = new CountDownLatch(1);
			var conflictCommitted = new CountDownLatch(1);
			var k5HeldInWhileCommit = new AtomicBoolean(true);
			var k5AcquirableByOtherThread = new AtomicBoolean(false);
			var k1StillHeldInWhileCommit = new AtomicBoolean(false);

			Future<Long> procP = TaskSpec.ofProcedure(app.newProcedure(() -> {
				int r = round.incrementAndGet();
				var v1 = table.getOrAdd(k1);
				if (r == 1) {
					var v5 = table.getOrAdd(k5);
					round1Loaded.countDown();
					if (!conflictCommitted.await(15, TimeUnit.SECONDS))
						return Procedure.Exception;
					// 第 1 轮同时写 K1 与 K5；因 K1 冲突触发 Redo，保留 [K1, K5] 写锁进入第 2 轮
					v1.setInt_1(11);
					v5.setInt_1(51);
					return Procedure.Success;
				}
				// 第 2 轮只修改 K1，不再访问尾部键 K5
				v1.setInt_1(12);
				Transaction.whileCommit(() -> {
					k1StillHeldInWhileCommit.set(lockeyK1.isWriteLockHeld());
					k5HeldInWhileCommit.set(lockeyK5.isWriteLockHeld());
					try {
						k5AcquirableByOtherThread.set(canOtherThreadAcquireWriteLock(probePool, lockeyK5));
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				});
				return Procedure.Success;
			}, "shrinkAccessSet", TransactionLevel.Serializable)).submitNow();

			Assertions.assertTrue(round1Loaded.await(15, TimeUnit.SECONDS));
			Assertions.assertEquals(Procedure.Success, app.newProcedure(() -> {
				table.getOrAdd(k1).setInt_1(99);
				return Procedure.Success;
			}, "conflictK1").call());
			conflictCommitted.countDown();

			Assertions.assertEquals(Procedure.Success, (long)procP.get(15, TimeUnit.SECONDS));
			Assertions.assertEquals(2, round.get(), "应经历一次冲突重做并在第 2 轮提交");
			Assertions.assertTrue(k1StillHeldInWhileCommit.get(), "仍在访问集内的 K1 写锁应继续保留至事务结束");
			Assertions.assertFalse(k5HeldInWhileCommit.get(), "第 2 轮未访问的尾部键 K5 写锁必须在 lockAndCheck 阶段释放");
			Assertions.assertTrue(k5AcquirableByOtherThread.get(), "其他线程在 whileCommit 期间必须能立即获取 K5 写锁");
		} finally {
			app.stop();
		}
	}

	@Test
	public void testHoldLocksReleasedWhenAllowDirtyWhenAllReadDegradesToReadOnly() throws Exception {
		var table = new Table3();
		var app = newApp(table);
		try (var probePool = Executors.newSingleThreadExecutor()) {
			long k1 = 11L;
			long k5 = 15L;
			Assertions.assertEquals(Procedure.Success, app.newProcedure(() -> {
				table.getOrAdd(k1).setInt_1(100);
				table.getOrAdd(k5).setInt_1(500);
				return Procedure.Success;
			}, "seedAllowDirty").call());

			var lockeyK1 = app.getLocks().get(new TableKey(table.getId(), k1));
			var lockeyK5 = app.getLocks().get(new TableKey(table.getId(), k5));

			var round = new AtomicInteger();
			var round1Loaded = new CountDownLatch(1);
			var conflictCommitted = new CountDownLatch(1);
			var k1HeldInWhileCommit = new AtomicBoolean(true);
			var k5HeldInWhileCommit = new AtomicBoolean(true);
			var k1AcquirableByOther = new AtomicBoolean(false);
			var k5AcquirableByOther = new AtomicBoolean(false);

			Future<Long> procP = TaskSpec.ofProcedure(app.newProcedure(() -> {
				int r = round.incrementAndGet();
				var v1 = table.getOrAdd(k1);
				if (r == 1) {
					var v5 = table.getOrAdd(k5);
					round1Loaded.countDown();
					if (!conflictCommitted.await(15, TimeUnit.SECONDS))
						return Procedure.Exception;
					// 第 1 轮存在写入并触发 Redo，持 [K1, K5] 写锁进入第 2 轮
					v1.setInt_1(101);
					v5.setInt_1(501);
					return Procedure.Success;
				}
				// 第 2 轮退化为纯读，命中 AllowDirtyWhenAllRead 早退分支
				Assertions.assertEquals(999, v1.getInt_1());
				Transaction.whileCommit(() -> {
					k1HeldInWhileCommit.set(lockeyK1.isWriteLockHeld());
					k5HeldInWhileCommit.set(lockeyK5.isWriteLockHeld());
					try {
						k1AcquirableByOther.set(canOtherThreadAcquireWriteLock(probePool, lockeyK1));
						k5AcquirableByOther.set(canOtherThreadAcquireWriteLock(probePool, lockeyK5));
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				});
				return Procedure.Success;
			}, "degradeReadOnly", TransactionLevel.AllowDirtyWhenAllRead)).submitNow();

			Assertions.assertTrue(round1Loaded.await(15, TimeUnit.SECONDS));
			Assertions.assertEquals(Procedure.Success, app.newProcedure(() -> {
				table.getOrAdd(k1).setInt_1(999);
				return Procedure.Success;
			}, "conflictAllowDirty").call());
			conflictCommitted.countDown();

			Assertions.assertEquals(Procedure.Success, (long)procP.get(15, TimeUnit.SECONDS));
			Assertions.assertEquals(2, round.get(), "应经历一次冲突重做并在第 2 轮以纯读提交");
			Assertions.assertFalse(k1HeldInWhileCommit.get(), "AllowDirtyWhenAllRead 退化为纯读前必须释放残留 K1 写锁");
			Assertions.assertFalse(k5HeldInWhileCommit.get(), "AllowDirtyWhenAllRead 退化为纯读前必须释放残留 K5 写锁");
			Assertions.assertTrue(k1AcquirableByOther.get(), "其他线程在 whileCommit 期间必须能立即获取 K1 写锁");
			Assertions.assertTrue(k5AcquirableByOther.get(), "其他线程在 whileCommit 期间必须能立即获取 K5 写锁");
		} finally {
			app.stop();
		}
	}
}
