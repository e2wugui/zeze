package Zeze.Transaction;

import java.lang.invoke.VarHandle;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.Application;
import Zeze.Config;
import Zeze.Services.GlobalCacheManagerConst;
import Zeze.Transaction.Logs.LogInt;
import Zeze.Transaction.Logs.LogLong;
import demo.Module1.BValue;
import demo.Module1.Table3;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** formal/rollback 与 record-lifecycle 的真实实现对照；每个应用使用独立后端。 */
@Fast
public class TestFormalRollback {
	private static final AtomicInteger nextId = new AtomicInteger(FastServerIds.TEST_FORMAL_ROLLBACK);

	private static final class Env implements AutoCloseable {
		final Application app;
		final Table3 table = new Table3();

		Env() throws Exception {
			var config = new Config();
			config.setServiceManager("disable");
			config.setServerId(nextId.getAndIncrement());
			config.setDefaultTableConf(new Config.TableConf());
			var database = new Config.DatabaseConf();
			database.setDatabaseUrl("formal_rollback_" + config.getServerId());
			config.getDatabaseConfMap().put("", database);
			app = new Application("FormalRollback" + config.getServerId(), config);
			app.setSchemas(new demo.Schemas());
			app.addTable("", table);
			app.start();
			ok(() -> {
				var b = table.getOrAdd(1L);
				b.setInt_1(0);
				b.setLong2(0);
				b.getMap15().put(1L, 0L);
				return Procedure.Success;
			});
		}

		void ok(Zeze.Util.FuncLong body) {
			Assertions.assertEquals(Procedure.Success, app.newProcedure(body, "FormalRollback").call());
		}

		void expect(int first, long second, long map) {
			ok(() -> {
				var b = table.getOrAdd(1L);
				Assertions.assertEquals(first, b.getInt_1());
				Assertions.assertEquals(second, b.getLong2());
				Assertions.assertEquals(map, b.getMap15().get(1L));
				return Procedure.Success;
			});
		}

		@Override
		public void close() throws Exception {
			app.stop();
		}
	}

	private static void await(CountDownLatch latch) throws InterruptedException {
		Assertions.assertTrue(latch.await(15, TimeUnit.SECONDS), "预定时序未到达");
	}

	@Test
	public void testTopLevelExceptionDiscardsScalarAndMapLogs() throws Exception {
		try (var env = new Env()) {
			Assertions.assertEquals(Procedure.Exception, env.app.newProcedure(() -> {
				var b = env.table.getOrAdd(1L);
				b.setInt_1(9);
				b.setLong2(9);
				b.getMap15().put(1L, 9L);
				throw new IllegalArgumentException("expected pre-publication failure");
			}, "FormalRollback.ExpectedException").call());
			env.expect(0, 0, 0);
		}
	}

	@Test
	public void testNestedRollbackRestoresParentScalarAndPersistentMap() throws Exception {
		try (var env = new Env()) {
			env.ok(() -> {
				var b = env.table.getOrAdd(1L);
				b.setInt_1(1);
				b.getMap15().put(1L, 1L);
				Assertions.assertEquals(Procedure.LogicError, env.app.newProcedure(() -> {
					b.setInt_1(2);
					b.setLong2(2);
					b.getMap15().put(1L, 2L);
					b.getMap15().put(2L, 2L);
					return Procedure.LogicError;
				}, "FormalRollback.NestedUndo").call());
				Assertions.assertEquals(1, b.getInt_1());
				Assertions.assertEquals(0, b.getLong2());
				Assertions.assertEquals(1L, b.getMap15().get(1L));
				Assertions.assertNull(b.getMap15().get(2L));
				return Procedure.Success;
			});
			env.expect(1, 0, 1);
		}
	}

	@Test
	public void testNestedCommitMergesBeforeOuterRollback() throws Exception {
		try (var env = new Env()) {
			Assertions.assertEquals(Procedure.LogicError, env.app.newProcedure(() -> {
				env.ok(() -> {
					var b = env.table.getOrAdd(1L);
					b.setInt_1(2);
					b.setLong2(2);
					b.getMap15().remove(1L);
					return Procedure.Success;
				});
				Assertions.assertEquals(2, env.table.getOrAdd(1L).getInt_1());
				return Procedure.LogicError;
			}, "FormalRollback.OuterUndo").call());
			env.expect(0, 0, 0);
		}
	}

	private static BValue replacement(int value) {
		var b = new BValue();
		b.setInt_1(value);
		b.setLong2(value);
		b.getMap15().put(1L, (long)value);
		return b;
	}

	@Test
	public void testTopLevelReplacementExceptionKeepsOriginalBean() throws Exception {
		try (var env = new Env()) {
			var original = new AtomicReference<BValue>();
			env.ok(() -> {
				original.set(env.table.getOrAdd(1L));
				return Procedure.Success;
			});
			Assertions.assertEquals(Procedure.Exception, env.app.newProcedure(() -> {
				env.table.put(1L, replacement(9));
				env.table.getOrAdd(1L).setLong2(10);
				throw new IllegalArgumentException("expected replacement rollback");
			}, "FormalRollback.ReplaceException").call());
			env.ok(() -> {
				Assertions.assertSame(original.get(), env.table.getOrAdd(1L));
				return Procedure.Success;
			});
			env.expect(0, 0, 0);
		}
	}

	@Test
	public void testNestedMapCommitMergesIntoExistingParentLog() throws Exception {
		try (var env = new Env()) {
			env.ok(() -> {
				var b = env.table.getOrAdd(1L);
				b.getMap15().put(1L, 1L);
				env.ok(() -> {
					b.getMap15().remove(1L);
					b.getMap15().put(2L, 2L);
					return Procedure.Success;
				});
				Assertions.assertNull(b.getMap15().get(1L));
				Assertions.assertEquals(2L, b.getMap15().get(2L));
				return Procedure.Success;
			});
			env.ok(() -> {
				var b = env.table.getOrAdd(1L);
				Assertions.assertNull(b.getMap15().get(1L));
				Assertions.assertEquals(2L, b.getMap15().get(2L));
				return Procedure.Success;
			});
		}
	}

	@Test
	public void testNestedWholeValueReplacementRollbackAndMerge() throws Exception {
		try (var env = new Env()) {
			env.ok(() -> {
				var original = env.table.getOrAdd(1L);
				original.setInt_1(1);
				Assertions.assertEquals(Procedure.LogicError, env.app.newProcedure(() -> {
					env.table.put(1L, replacement(2));
					env.table.getOrAdd(1L).setLong2(3);
					return Procedure.LogicError;
				}, "FormalRollback.ReplaceUndo").call());
				Assertions.assertSame(original, env.table.getOrAdd(1L));
				Assertions.assertEquals(1, original.getInt_1());
				env.ok(() -> {
					env.table.put(1L, replacement(4));
					env.table.getOrAdd(1L).setLong2(5);
					return Procedure.Success;
				});
				Assertions.assertNotSame(original, env.table.getOrAdd(1L));
				return Procedure.Success;
			});
			env.expect(4, 5, 4);
		}
	}

	@Test
	public void testConflictRetryDropsPreviousAttemptLogsAndCallbacks() throws Exception {
		try (var env = new Env(); var workers = Executors.newSingleThreadExecutor()) {
			var ready = new CountDownLatch(1);
			var changed = new CountDownLatch(1);
			var rounds = new AtomicInteger();
			var callbacks = new CopyOnWriteArrayList<Integer>();
			var tx = workers.submit(() -> env.app.newProcedure(() -> {
				int round = rounds.incrementAndGet();
				var b = env.table.getOrAdd(1L);
				if (round == 1) {
					b.setInt_1(99);
					b.getMap15().put(1L, 99L);
					ready.countDown();
					await(changed);
				} else
					b.setLong2(8);
				Transaction.whileCommit(() -> callbacks.add(round));
				return Procedure.Success;
			}, "FormalRollback.ConflictingAttempt").call());
			try {
				await(ready);
				env.ok(() -> {
					env.table.getOrAdd(1L).setLong2(7);
					return Procedure.Success;
				});
			} finally {
				changed.countDown();
			}
			Assertions.assertEquals(Procedure.Success, tx.get(20, TimeUnit.SECONDS));
			Assertions.assertEquals(2, rounds.get());
			Assertions.assertEquals(List.of(2), callbacks);
			env.expect(0, 8, 0);
		}
	}

	private static VarHandle fieldHandle(String name) throws ReflectiveOperationException {
		var field = BValue.class.getDeclaredField(name);
		field.setAccessible(true);
		return (VarHandle)field.get(null);
	}

	// Commit must never throw: a failed scheduling condition is checked after publication.
	private static void pauseFirst(AtomicInteger count, CountDownLatch published,
			CountDownLatch release, AtomicInteger timingFailures) {
		if (count.incrementAndGet() == 1) {
			published.countDown();
			try {
				if (!release.await(15, TimeUnit.SECONDS))
					timingFailures.incrementAndGet();
			} catch (InterruptedException e) {
				timingFailures.incrementAndGet();
				Thread.currentThread().interrupt();
			}
		}
	}

	@Test
	public void testReaderRetriesBetweenIndividualFieldLogPublications() throws Exception {
		try (var env = new Env(); var workers = Executors.newFixedThreadPool(2)) {
			var first = new CountDownLatch(1);
			var release = new CountDownLatch(1);
			var read = new CountDownLatch(1);
			var count = new AtomicInteger();
			var timingFailures = new AtomicInteger();
			var observed = new CopyOnWriteArrayList<List<Long>>();
			var committed = new CopyOnWriteArrayList<List<Long>>();
			var writer = workers.submit(() -> env.app.newProcedure(() -> {
				var b = env.table.getOrAdd(1L);
				var tx = Transaction.getCurrent();
				tx.putLog(new LogInt(b, 1, fieldHandle("vh_int_1"), 1) {
					@Override
					public void commit() {
						super.commit();
						pauseFirst(count, first, release, timingFailures);
					}
				});
				tx.putLog(new LogLong(b, 2, fieldHandle("vh_long2"), 1) {
					@Override
					public void commit() {
						super.commit();
						pauseFirst(count, first, release, timingFailures);
					}
				});
				return Procedure.Success;
			}, "FormalRollback.PublishFields").call());
			await(first);
			var reader = workers.submit(() -> env.app.newProcedure(() -> {
				var b = env.table.getOrAdd(1L);
				var pair = List.of((long)b.getInt_1(), b.getLong2());
				observed.add(pair);
				Transaction.whileCommit(() -> committed.add(pair));
				read.countDown();
				return Procedure.Success;
			}, "FormalRollback.ReadPartialPublish").call());
			try {
				await(read);
			} finally {
				release.countDown();
			}
			Assertions.assertEquals(Procedure.Success, writer.get(20, TimeUnit.SECONDS));
			Assertions.assertEquals(Procedure.Success, reader.get(20, TimeUnit.SECONDS));
			Assertions.assertEquals(0, timingFailures.get());
			Assertions.assertEquals(2, observed.size());
			Assertions.assertNotEquals(observed.getFirst().get(0), observed.getFirst().get(1));
			Assertions.assertEquals(List.of(List.of(1L, 1L)), committed);
		}
	}

	private static Record1<Long, BValue> record(Env env) {
		return env.table.getCache().get(1L);
	}

	@Test
	public void testEvictionRecreationRejectsOldAttemptAndOldBeanWrite() throws Exception {
		try (var env = new Env(); var workers = Executors.newSingleThreadExecutor()) {
			env.app.checkpointRun();
			var oldRecord = record(env);
			var oldBean = new AtomicReference<BValue>();
			var ready = new CountDownLatch(1);
			var changed = new CountDownLatch(1);
			var rounds = new AtomicInteger();
			var tx = workers.submit(() -> env.app.newProcedure(() -> {
				var b = env.table.getOrAdd(1L);
				int value = b.getInt_1();
				if (rounds.incrementAndGet() == 1) {
					oldBean.set(b);
					ready.countDown();
					await(changed);
				}
				env.table.getOrAdd(2L).setInt_1(value);
				return Procedure.Success;
			}, "FormalRollback.ReadEvictedRecord").call());
			try {
				await(ready);
				var cache = env.table.getCache();
				var evict = cache.getClass().getDeclaredMethod("tryRemoveRecord", Map.Entry.class);
				evict.setAccessible(true);
				Assertions.assertEquals(true, evict.invoke(cache, Map.entry(1L, oldRecord)));
				Assertions.assertEquals(GlobalCacheManagerConst.StateRemoved, oldRecord.getState());
				env.ok(() -> {
					var b = env.table.getOrAdd(1L);
					Assertions.assertNotSame(oldBean.get(), b);
					b.setInt_1(7);
					return Procedure.Success;
				});
				Assertions.assertNotSame(oldRecord, record(env));
			} finally {
				changed.countDown();
			}
			Assertions.assertEquals(Procedure.Success, tx.get(20, TimeUnit.SECONDS));
			Assertions.assertEquals(2, rounds.get());
			env.ok(() -> {
				Assertions.assertEquals(7, env.table.getOrAdd(2L).getInt_1());
				return Procedure.Success;
			});
			// Explicit guard probe, outside the supported retain-a-Bean application pattern.
			var probeRounds = new AtomicInteger();
			env.ok(() -> {
				env.table.getOrAdd(1L);
				if (probeRounds.incrementAndGet() == 1)
					oldBean.get().setInt_1(99); // StateRemoved forces redo; new record cannot validate it.
				return Procedure.Success;
			});
			Assertions.assertEquals(2, probeRounds.get());
			env.expect(7, 0, 0);
		}
	}

	@Test
	public void testInvalidReloadAdvancesTimestampAndRetriesOldAttempt() throws Exception {
		try (var env = new Env(); var workers = Executors.newSingleThreadExecutor()) {
			env.app.checkpointRun();
			var oldRecord = record(env);
			long stamp = oldRecord.getTimestamp();
			var ready = new CountDownLatch(1);
			var changed = new CountDownLatch(1);
			var rounds = new AtomicInteger();
			var tx = workers.submit(() -> env.app.newProcedure(() -> {
				int value = env.table.getOrAdd(1L).getInt_1();
				if (rounds.incrementAndGet() == 1) {
					ready.countDown();
					await(changed);
				}
				env.table.getOrAdd(2L).setInt_1(value);
				return Procedure.Success;
			}, "FormalRollback.ReadInvalidatedRecord").call());
			try {
				await(ready);
				var lockey = env.app.getLocks().get(new TableKey(env.table.getId(), 1L));
				lockey.enterWriteLock();
				try {
					oldRecord.enterFairLock();
					try {
						oldRecord.setState(GlobalCacheManagerConst.StateInvalid);
					} finally {
						oldRecord.exitFairLock();
					}
				} finally {
					lockey.exitWriteLock();
				}
				env.ok(() -> {
					Assertions.assertEquals(0, env.table.getOrAdd(1L).getInt_1());
					return Procedure.Success;
				});
				Assertions.assertSame(oldRecord, record(env));
				Assertions.assertTrue(oldRecord.getTimestamp() > stamp);
			} finally {
				changed.countDown();
			}
			Assertions.assertEquals(Procedure.Success, tx.get(20, TimeUnit.SECONDS));
			Assertions.assertEquals(2, rounds.get());
		}
	}

	@Test
	public void testDeleteReinsertSameKeyInvalidatesOldAttempt() throws Exception {
		try (var env = new Env(); var workers = Executors.newSingleThreadExecutor()) {
			var oldRecord = record(env);
			var ready = new CountDownLatch(1);
			var changed = new CountDownLatch(1);
			var rounds = new AtomicInteger();
			var committed = new CopyOnWriteArrayList<Integer>();
			var tx = workers.submit(() -> env.app.newProcedure(() -> {
				int value = env.table.getOrAdd(1L).getInt_1();
				if (rounds.incrementAndGet() == 1) {
					ready.countDown();
					await(changed);
				}
				Transaction.whileCommit(() -> committed.add(value));
				return Procedure.Success;
			}, "FormalRollback.ReadReinsertedKey").call());
			try {
				await(ready);
				env.ok(() -> { env.table.remove(1L); return Procedure.Success; });
				env.ok(() -> { env.table.insert(1L, replacement(6)); return Procedure.Success; });
				Assertions.assertSame(oldRecord, record(env));
			} finally {
				changed.countDown();
			}
			Assertions.assertEquals(Procedure.Success, tx.get(20, TimeUnit.SECONDS));
			Assertions.assertEquals(2, rounds.get());
			Assertions.assertEquals(List.of(6), committed);
			env.expect(6, 6, 6);
		}
	}
}
