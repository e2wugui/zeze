package Zeze.Transaction;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import Zeze.Application;
import Zeze.Config;
import demo.Module1.tflush;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** formal/locks 的关键时序：真实事务、真实锁、有限等待，不向生产路径注入钩子。 */
@Fast
public class TestFormalLockProtocol {
	private static final AtomicInteger nextId = new AtomicInteger(FastServerIds.TEST_FORMAL_LOCK_PROTOCOL);

	private static final class Env implements AutoCloseable {
		final Application app;
		final tflush table = new tflush();

		Env() throws Exception {
			var config = new Config();
			config.setServiceManager("disable");
			config.setServerId(nextId.getAndIncrement());
			config.setDefaultTableConf(new Config.TableConf());
			var database = new Config.DatabaseConf();
			database.setDatabaseUrl("formal_locks_" + config.getServerId());
			config.getDatabaseConfMap().put("", database);
			app = new Application("FormalLocks" + config.getServerId(), config);
			app.setSchemas(new demo.Schemas());
			app.addTable("", table);
			app.start();
			Assertions.assertEquals(Procedure.Success, app.newProcedure(() -> {
				table.getOrAdd(1L).setInt_1(0);
				table.getOrAdd(2L).setInt_1(0);
				return Procedure.Success;
			}, "FormalLocks.Initialize").call());
		}

		Lockey key(long key) {
			return app.getLocks().get(new TableKey(table.getId(), key));
		}

		void assertReleased() {
			for (long k = 1; k <= 2; k++) {
				var lock = key(k);
				Assertions.assertTrue(lock.tryEnterWriteLock(1_000), "终局后仍被其他线程持锁: " + k);
				lock.exitWriteLock();
			}
		}

		@Override
		public void close() throws Exception {
			app.stop();
		}
	}

	private static void await(CountDownLatch latch) throws InterruptedException {
		Assertions.assertTrue(latch.await(10, TimeUnit.SECONDS), "预定锁时序未到达");
	}

	private static ReentrantReadWriteLock rw(Lockey lock) throws ReflectiveOperationException {
		Field field = Lockey.class.getDeclaredField("rwLock");
		field.setAccessible(true);
		return (ReentrantReadWriteLock)field.get(lock);
	}

	@Test
	public void testEqualTableKeysShareOneLiveLock() throws Exception {
		var locks = new Locks();
		var canonical = locks.get(new TableKey(10, 1L));
		try (var workers = Executors.newFixedThreadPool(4)) {
			var tasks = new java.util.ArrayList<java.util.concurrent.Future<Lockey>>();
			for (int i = 0; i < 32; i++)
				tasks.add(workers.submit(() -> locks.get(new TableKey(10, 1L))));
			for (var task : tasks)
				Assertions.assertSame(canonical, task.get(10, TimeUnit.SECONDS));
		}
		Assertions.assertFalse(rw(canonical).isFair());
		Assertions.assertTrue(new TableKey(9, 100L).compareTo(new TableKey(10, 1L)) < 0);
		Assertions.assertTrue(new TableKey(10, 1L).compareTo(new TableKey(10, 2L)) < 0);
	}

	@Test
	public void testReadHolderMustReleaseBeforeWriteAcquisition() {
		var lock = new Locks().get(new TableKey(10, 3L));
		lock.enterReadLock();
		try {
			Assertions.assertFalse(lock.tryEnterWriteLock(0), "ReentrantReadWriteLock 不支持持读升级");
		} finally {
			lock.exitReadLock();
		}
		Assertions.assertTrue(lock.tryEnterWriteLock(0));
		lock.exitWriteLock();
	}

	@Test
	public void testInterruptedLockWaitReturnsOnlyAfterOwnerReleases() throws Exception {
		var lock = new Locks().get(new TableKey(10, 2L));
		var returned = new CountDownLatch(1);
		var interrupted = new java.util.concurrent.atomic.AtomicBoolean();
		lock.enterWriteLock();
		var waiter = Thread.ofPlatform().start(() -> {
			lock.enterWriteLock();
			try {
				interrupted.set(Thread.currentThread().isInterrupted());
			} finally {
				lock.exitWriteLock();
				returned.countDown();
			}
		});
		try {
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
			while (!rw(lock).hasQueuedThread(waiter) && System.nanoTime() < deadline)
				java.util.concurrent.locks.LockSupport.parkNanos(100_000);
			Assertions.assertTrue(rw(lock).hasQueuedThread(waiter));
			waiter.interrupt();
			Assertions.assertFalse(returned.await(100, TimeUnit.MILLISECONDS), "lock() 不会因中断返回");
		} finally {
			lock.exitWriteLock();
		}
		await(returned);
		waiter.join(1_000);
		Assertions.assertTrue(interrupted.get());
	}

	private static void retainedSuffixIsReleased(boolean upgrade) throws Exception {
		try (var env = new Env(); var workers = Executors.newFixedThreadPool(2)) {
			var firstRead = new CountDownLatch(1);
			var changed = new CountDownLatch(1);
			var secondBody = new CountDownLatch(1);
			var proceed = new CountDownLatch(1);
			var rounds = new AtomicInteger();
			var transaction = workers.submit(() -> env.app.newProcedure(() -> {
				int round = rounds.incrementAndGet();
				if (round == 1) {
					if (upgrade)
						env.table.getOrAdd(1L).getInt_1();
					env.table.getOrAdd(2L).getInt_1();
					firstRead.countDown();
					await(changed);
				} else {
					if (round == 2) {
						Assertions.assertEquals(1, rw(env.key(2)).getReadHoldCount(), "冲突必须保留后方读锁");
						if (upgrade)
							Assertions.assertEquals(1, rw(env.key(1)).getReadHoldCount());
					}
					env.table.getOrAdd(1L).setInt_1(10);
					env.table.getOrAdd(2L).getInt_1();
					if (round == 2) {
						secondBody.countDown();
						await(proceed);
					}
				}
				return Procedure.Success;
			}, "FormalLocks.RepairSuffix").call());
			try {
				await(firstRead);
				Assertions.assertEquals(Procedure.Success, env.app.newProcedure(() -> {
					env.table.getOrAdd(2L).setInt_1(1);
					return Procedure.Success;
				}, "FormalLocks.Invalidate").call());
			} finally {
				changed.countDown();
			}
			await(secondBody);
			var earlier = env.key(1);
			if (upgrade)
				earlier.enterReadLock();
			else
				earlier.enterWriteLock();
			try {
				proceed.countDown();
				// 本线程持 key1 阻止对方获取；key2 若未先释放，这笔写将无法完成。
				var laterWriter = workers.submit(() -> env.app.newProcedure(() -> {
					env.table.getOrAdd(2L).setInt_1(2);
					return Procedure.Success;
				}, "FormalLocks.WriteSuffix").call());
				Assertions.assertEquals(Procedure.Success, laterWriter.get(10, TimeUnit.SECONDS));
			} finally {
				earlier.exitLock();
				proceed.countDown();
			}
			Assertions.assertEquals(Procedure.Success, transaction.get(10, TimeUnit.SECONDS));
			Assertions.assertTrue(rounds.get() >= 2);
			env.assertReleased();
		}
	}

	@Test
	public void testReadUpgradeReleasesLaterRetainedLocksBeforeWaiting() throws Exception {
		retainedSuffixIsReleased(true);
	}

	@Test
	public void testEarlierInsertionReleasesLaterRetainedLocksBeforeWaiting() throws Exception {
		retainedSuffixIsReleased(false);
	}

	private static void retainedLocksOnExit(String exit) throws Exception {
		try (var env = new Env(); var workers = Executors.newSingleThreadExecutor()) {
			var read = new CountDownLatch(1);
			var changed = new CountDownLatch(1);
			var rounds = new AtomicInteger();
			var result = workers.submit(() -> env.app.newProcedure(() -> {
				int round = rounds.incrementAndGet();
				if (round == 1) {
					env.table.getOrAdd(1L).getInt_1();
					env.table.getOrAdd(2L).getInt_1();
					read.countDown();
					await(changed);
					return Procedure.Success;
				}
				int expectedHolds = exit.equals("release") && round == 3 ? 0 : 1;
				Assertions.assertEquals(expectedHolds, rw(env.key(1)).getReadHoldCount());
				Assertions.assertEquals(expectedHolds, rw(env.key(2)).getReadHoldCount());
				var txn = Transaction.getCurrent();
				switch (exit) {
				case "exception": throw new IllegalStateException("FormalLocks.ExpectedBusinessFailure");
				case "abort": txn.throwAbort("FormalLocks.ExpectedAbort", null); break;
				case "exhaust": txn.throwRedo(env.table.getId(), "FormalLocks.ExpectedRetry"); break;
				case "release":
					if (round == 2)
						txn.throwRedoAndReleaseLock(env.table.getId(), "FormalLocks.ExpectedRelease", null);
					break;
				default: return -123;
				}
				return Procedure.Success;
			}, "FormalLocks.Exit." + exit).call());
			try {
				await(read);
				Assertions.assertEquals(Procedure.Success, env.app.newProcedure(() -> {
					env.table.getOrAdd(2L).setInt_1(1);
					return Procedure.Success;
				}, "FormalLocks.InvalidateExit").call());
			} finally {
				changed.countDown();
			}
			long expected = switch (exit) {
				case "exception" -> Procedure.Exception;
				case "abort" -> Procedure.AbortException;
				case "exhaust" -> Procedure.TooManyTry;
				case "release" -> Procedure.Success;
				default -> -123;
			};
			Assertions.assertEquals(expected, result.get(15, TimeUnit.SECONDS));
			Assertions.assertEquals(exit.equals("exhaust") ? 256 : exit.equals("release") ? 3 : 2, rounds.get());
			env.assertReleased();
		}
	}

	@Test
	public void testBusinessExceptionReleasesRetainedLocks() throws Exception {
		retainedLocksOnExit("exception");
	}

	@Test
	public void testAbortReleasesRetainedLocks() throws Exception {
		retainedLocksOnExit("abort");
	}

	@Test
	public void testFailureResultReleasesRetainedLocks() throws Exception {
		retainedLocksOnExit("result");
	}

	@Test
	public void testExactly256AttemptsThenFailureReleasesRetainedLocks() throws Exception {
		retainedLocksOnExit("exhaust");
	}

	@Test
	public void testExplicitReleaseRetryStartsWithoutRetainedLocks() throws Exception {
		retainedLocksOnExit("release");
	}

	@Test
	public void testCopyUnderRecordLockFallsBackWhileTransactionWaits() throws Exception {
		try (var env = new Env(); var workers = Executors.newSingleThreadExecutor()) {
			var ref = new java.util.concurrent.atomic.AtomicReference<Record1<?, ?>>();
			Assertions.assertEquals(Procedure.Success, env.app.newProcedure(() -> {
				var bean = env.table.getOrAdd(1L);
				ref.set((Record1<?, ?>)Transaction.getCurrent()
						.getRecordAccessed(bean.tableKey()).atomicTupleRecord.record());
				return Procedure.Success;
			}, "FormalLocks.RecordReference").call());
			var record = ref.get();
			var keyAcquired = new CountDownLatch(1);
			record.enterFairLock();
			var waiter = workers.submit(() -> {
				var key = env.key(1);
				key.enterWriteLock();
				try {
					keyAcquired.countDown();
					record.enterFairLock();
					record.exitFairLock();
				} finally {
					key.exitWriteLock();
				}
			});
			try {
				await(keyAcquired);
				long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
				while (!record.hasQueuedThreads() && System.nanoTime() < deadline)
					java.util.concurrent.locks.LockSupport.parkNanos(100_000);
				Assertions.assertTrue(record.hasQueuedThreads(), "写者应已持 Lockey 等待 Record");
				// fairLock -> Lockey 的真实 copyValue 必须即时失败并回退，才能释放 fairLock。
				Assertions.assertTimeout(java.time.Duration.ofSeconds(3), record::copyValue);
			} finally {
				record.exitFairLock();
			}
			waiter.get(10, TimeUnit.SECONDS);
			env.assertReleased();
		}
	}
}
