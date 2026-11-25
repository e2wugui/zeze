package Zeze.Transaction;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

import Zeze.Application;
import Zeze.Config;
import Zeze.Util.FastLock;
import demo.Module1.Table3;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Checkpoint.run 在 lock.lock() 之后、cond.await(period) 之前必须复检 isRunning，
 * 否则 stopAndJoin() 在检查点线程进入 lock 前发出的 signal 会丢失，导致停机阻塞整整一个 period。
 */
@Fast
public class TestCheckpointStopWakeup {
	@TempDir
	Path tempDir;

	@Test
	public void testStopDoesNotMissSignalBeforeAwait() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setNoDatabase(true);
		conf.setCheckpointMode(CheckpointMode.Table);
		var app = new Application("TestCheckpointStopWakeup.direct", conf);
		var checkpoint = new Checkpoint(app, CheckpointMode.Table, 0);

		var lockField = Checkpoint.class.getDeclaredField("lock");
		lockField.setAccessible(true);
		var lock = (FastLock)lockField.get(checkpoint);

		var condField = Checkpoint.class.getDeclaredField("cond");
		condField.setAccessible(true);
		var cond = (Condition)condField.get(checkpoint);

		var periodField = Checkpoint.class.getDeclaredField("period");
		periodField.setAccessible(true);

		var isRunningField = Checkpoint.class.getDeclaredField("isRunning");
		isRunningField.setAccessible(true);

		var threadField = Checkpoint.class.getDeclaredField("checkpointThread");
		threadField.setAccessible(true);
		var thread = (Thread)threadField.get(checkpoint);

		// 确定性复现丢信号窗口：主线程先持 lock，让检查点线程越过 while (isRunning) 后阻塞于 lock.lock()，
		// 再在锁内将 isRunning 置 false 并调用 cond.signal()（此时无等待者，信号丢失）。
		lock.lock();
		try {
			periodField.setInt(checkpoint, 3_600_000);
			isRunningField.setBoolean(checkpoint, true);
			thread.start();

			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
			while (!lock.hasQueuedThreads() && System.nanoTime() < deadline)
				Thread.yield();
			Assertions.assertTrue(lock.hasQueuedThreads(), "检查点线程应在 lock.lock() 处等待");

			isRunningField.setBoolean(checkpoint, false);
			cond.signal();
		} finally {
			lock.unlock();
		}

		thread.join(5_000);
		if (thread.isAlive()) {
			// 防止测试失败时残留后台线程阻塞 1 小时
			lock.lock();
			try {
				cond.signal();
			} finally {
				lock.unlock();
			}
			thread.join(5_000);
			Assertions.fail("Checkpoint.run 未在锁内复检 isRunning，错过停机信号进入 1 小时等待");
		}
	}

	@Test
	public void testApplicationStartStopWithLongPeriodFlushesDirtyRecord() throws Exception {
		var dbPath = tempDir.resolve("dbhome").toString();
		for (int round = 0; round < 5; round++) {
			var conf = new Config();
			conf.setServiceManager("disable");
			conf.setServerId(FastServerIds.TEST_CHECKPOINT_STOP_WAKEUP);
			conf.setCheckpointMode(CheckpointMode.Table);
			conf.setCheckpointPeriod(3_600_000);
			conf.setDefaultTableConf(new Config.TableConf());
			var dbConf = new Config.DatabaseConf();
			dbConf.setDatabaseType(Config.DbType.RocksDb);
			dbConf.setDatabaseUrl(dbPath);
			conf.getDatabaseConfMap().put("", dbConf);

			var app = new Application("TestCheckpointStopWakeup.app." + round, conf);
			var table = new Table3();
			app.addTable("", table);
			app.start();
			long key = 1000L + round;
			int expected = 500 + round;
			try {
				if (round > 0) {
					var prev = table.selectFromDatabase(key - 1);
					Assertions.assertNotNull(prev, "上一轮终检点必须将脏记录落库");
					Assertions.assertEquals(expected - 1, prev.getInt_1());
				}
				Assertions.assertEquals(Procedure.Success, app.newProcedure(() -> {
					table.getOrAdd(key).setInt_1(expected);
					return Procedure.Success;
				}, "writeDirty").call());
			} finally {
				long begin = System.nanoTime();
				app.stop();
				long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - begin);
				Assertions.assertTrue(elapsedMs < 5_000,
						"长周期 Checkpoint 下 stop 必须迅速完成，实际耗时 " + elapsedMs + "ms");
			}
		}

		// 最后一轮落库复核
		var verifyConf = new Config();
		verifyConf.setServiceManager("disable");
		verifyConf.setServerId(FastServerIds.TEST_CHECKPOINT_STOP_WAKEUP);
		verifyConf.setCheckpointMode(CheckpointMode.Table);
		verifyConf.setCheckpointPeriod(3_600_000);
		verifyConf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseType(Config.DbType.RocksDb);
		dbConf.setDatabaseUrl(dbPath);
		verifyConf.getDatabaseConfMap().put("", dbConf);
		var verifyApp = new Application("TestCheckpointStopWakeup.verify", verifyConf);
		var verifyTable = new Table3();
		verifyApp.addTable("", verifyTable);
		verifyApp.start();
		try {
			var last = verifyTable.selectFromDatabase(1004L);
			Assertions.assertNotNull(last, "最后一轮终检点必须将脏记录落库");
			Assertions.assertEquals(504, Objects.requireNonNull(last).getInt_1());
		} finally {
			verifyApp.stop();
		}
	}
}
