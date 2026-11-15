package Zeze.Transaction;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Application;
import Zeze.Config;
import demo.Module1.BValue;
import demo.Module1.tflush;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** 与 formal/serializability 中的反例和重试路径对应的真实事务时序。 */
@Fast
public class TestSerializableCommitHistories {
	private static final AtomicInteger nextId =
			new AtomicInteger(FastServerIds.TEST_SERIALIZABLE_COMMIT_HISTORIES);

	private static final class Env implements AutoCloseable {
		final Application app;
		final tflush table = new tflush();

		Env() throws Exception {
			var config = new Config();
			config.setServiceManager("disable");
			config.setServerId(nextId.getAndIncrement());
			config.setDefaultTableConf(new Config.TableConf());
			var database = new Config.DatabaseConf();
			database.setDatabaseUrl("serializable_histories_" + config.getServerId());
			config.getDatabaseConfMap().put("", database);
			app = new Application("SerializableHistories" + config.getServerId(), config);
			app.setSchemas(new demo.Schemas());
			app.addTable("", table);
			app.start();
		}

		void initialize(int... values) {
			Assertions.assertEquals(Procedure.Success, app.newProcedure(() -> {
				for (int i = 0; i < values.length; ++i)
					table.getOrAdd((long)i + 1).setInt_1(values[i]);
				return Procedure.Success;
			}, "SerializableHistories.Initialize").call());
		}

		int read(long key) {
			var result = new AtomicInteger(-1);
			Assertions.assertEquals(Procedure.Success, app.newProcedure(() -> {
				var bean = table.get(key);
				int value = bean != null ? bean.getInt_1() : -1;
				Transaction.whileCommit(() -> result.set(value));
				return Procedure.Success;
			}, "SerializableHistories.Read").call());
			return result.get();
		}

		@Override
		public void close() throws Exception {
			app.stop();
		}
	}

	private static void await(CountDownLatch latch) throws InterruptedException {
		Assertions.assertTrue(latch.await(10, TimeUnit.SECONDS), "预定事务时序未到达");
	}

	@Test
	public void testConflictingIncrementsRetryWithoutLosingUpdates() throws Exception {
		try (var env = new Env(); var workers = Executors.newFixedThreadPool(2)) {
			env.initialize(0);
			var ready = new CountDownLatch(2);
			var attempts = new AtomicInteger();
			var results = new java.util.ArrayList<java.util.concurrent.Future<Long>>();
			for (int i = 0; i < 2; ++i) {
				var round = new AtomicInteger();
				results.add(workers.submit(() -> env.app.newProcedure(() -> {
					attempts.incrementAndGet();
					var bean = env.table.getOrAdd(1L);
					int value = bean.getInt_1();
					if (round.incrementAndGet() == 1) {
						ready.countDown();
						await(ready);
					}
					bean.setInt_1(value + 1);
					return Procedure.Success;
				}, "SerializableHistories.Increment").call()));
			}
			for (var result : results)
				Assertions.assertEquals(Procedure.Success, result.get(15, TimeUnit.SECONDS));
			Assertions.assertEquals(2, env.read(1L));
			Assertions.assertTrue(attempts.get() >= 3, "冲突的首轮尝试必须至少重试一笔");
		}
	}

	@Test
	public void testWriteSkewRevalidatesReadOnlyRecords() throws Exception {
		try (var env = new Env(); var workers = Executors.newFixedThreadPool(2)) {
			env.initialize(1, 1);
			var ready = new CountDownLatch(2);
			var results = new java.util.ArrayList<java.util.concurrent.Future<Long>>();
			for (long key = 1; key <= 2; ++key) {
				long ownKey = key;
				var round = new AtomicInteger();
				results.add(workers.submit(() -> env.app.newProcedure(() -> {
					int other = env.table.getOrAdd(3 - ownKey).getInt_1();
					if (round.incrementAndGet() == 1) {
						ready.countDown();
						await(ready);
					}
					if (other == 1)
						env.table.getOrAdd(ownKey).setInt_1(0);
					return Procedure.Success;
				}, "SerializableHistories.WriteSkew").call()));
			}
			for (var result : results)
				Assertions.assertEquals(Procedure.Success, result.get(15, TimeUnit.SECONDS));
			Assertions.assertEquals(1, env.read(1L) + env.read(2L));
		}
	}

	private static void readAcrossCommit(TransactionLevel level, boolean expectRetry) throws Exception {
		try (var env = new Env(); var workers = Executors.newSingleThreadExecutor()) {
			env.initialize(0, 0);
			var firstRead = new CountDownLatch(1);
			var writerDone = new CountDownLatch(1);
			var rounds = new AtomicInteger();
			var committed = new CopyOnWriteArrayList<List<Integer>>();
			var reader = workers.submit(() -> new Procedure(env.app, () -> {
				int first = env.table.getOrAdd(1L).getInt_1();
				if (rounds.incrementAndGet() == 1) {
					firstRead.countDown();
					await(writerDone);
				}
				int second = env.table.getOrAdd(2L).getInt_1();
				Transaction.whileCommit(() -> committed.add(List.of(first, second)));
				return Procedure.Success;
			}, "SerializableHistories.ReadAcrossCommit", level).call());
			try {
				await(firstRead);
				Assertions.assertEquals(Procedure.Success, env.app.newProcedure(() -> {
					env.table.getOrAdd(1L).setInt_1(1);
					env.table.getOrAdd(2L).setInt_1(1);
					return Procedure.Success;
				}, "SerializableHistories.PublishPair").call());
			} finally {
				writerDone.countDown();
			}
			Assertions.assertEquals(Procedure.Success, reader.get(15, TimeUnit.SECONDS));
			Assertions.assertEquals(List.of(expectRetry ? List.of(1, 1) : List.of(0, 1)), committed);
			Assertions.assertEquals(expectRetry ? 2 : 1, rounds.get());
		}
	}

	@Test
	public void testSerializableReadRetriesMixedRecordVersions() throws Exception {
		readAcrossCommit(TransactionLevel.Serializable, true);
	}

	@Test
	public void testDirtyReadModeExposesMixedRecordVersions() throws Exception {
		// 负对照：相同时序在弱隔离模式下确实读出混合结果，避免测试只有表面并发。
		readAcrossCommit(TransactionLevel.AllowDirtyWhenAllRead, false);
	}

	@Test
	public void testAbsentReadIsValidatedAfterConcurrentInsert() throws Exception {
		try (var env = new Env(); var workers = Executors.newSingleThreadExecutor()) {
			var firstRead = new CountDownLatch(1);
			var writerDone = new CountDownLatch(1);
			var rounds = new AtomicInteger();
			var reader = workers.submit(() -> env.app.newProcedure(() -> {
				boolean absent = env.table.get(1L) == null;
				if (rounds.incrementAndGet() == 1) {
					firstRead.countDown();
					await(writerDone);
				}
				env.table.getOrAdd(2L).setInt_1(absent ? 1 : 0);
				return Procedure.Success;
			}, "SerializableHistories.AbsentRead").call());
			try {
				await(firstRead);
				Assertions.assertEquals(Procedure.Success, env.app.newProcedure(() -> {
					var value = new BValue();
					value.setInt_1(7);
					env.table.insert(1L, value);
					return Procedure.Success;
				}, "SerializableHistories.Insert").call());
			} finally {
				writerDone.countDown();
			}
			Assertions.assertEquals(Procedure.Success, reader.get(15, TimeUnit.SECONDS));
			Assertions.assertEquals(2, rounds.get());
			Assertions.assertEquals(0, env.read(2L));
		}
	}

	private static void retryWithEarlierWrite(boolean loadEarlierOnFirstAttempt) throws Exception {
		try (var env = new Env(); var workers = Executors.newSingleThreadExecutor()) {
			env.initialize(0, 0);
			var firstRead = new CountDownLatch(1);
			var writerDone = new CountDownLatch(1);
			var rounds = new AtomicInteger();
			var reader = workers.submit(() -> env.app.newProcedure(() -> {
				int round = rounds.incrementAndGet();
				if (round == 1) {
					if (loadEarlierOnFirstAttempt)
						env.table.getOrAdd(1L).getInt_1();
					env.table.getOrAdd(2L).getInt_1();
					firstRead.countDown();
					await(writerDone);
				} else {
					env.table.getOrAdd(1L).setInt_1(2);
					env.table.getOrAdd(2L).getInt_1();
				}
				return Procedure.Success;
			}, "SerializableHistories.RetainedLocks").call());
			try {
				await(firstRead);
				Assertions.assertEquals(Procedure.Success, env.app.newProcedure(() -> {
					env.table.getOrAdd(2L).setInt_1(1);
					return Procedure.Success;
				}, "SerializableHistories.InvalidateRead").call());
			} finally {
				writerDone.countDown();
			}
			Assertions.assertEquals(Procedure.Success, reader.get(15, TimeUnit.SECONDS));
			Assertions.assertEquals(2, rounds.get());
			Assertions.assertEquals(2, env.read(1L));
		}
	}

	@Test
	public void testRetainedReadLocksCanUpgradeOnRetry() throws Exception {
		retryWithEarlierWrite(true);
	}

	@Test
	public void testEarlierKeyCanBeAddedBeforeRetainedLocks() throws Exception {
		retryWithEarlierWrite(false);
	}
}
