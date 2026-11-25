package Zeze.Collections;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Application;
import Zeze.BMyBean;
import Zeze.Config;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Util.FuncLong;
import Zeze.Util.TaskSpec;
import harness.Fast;
import harness.FastServerIds;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.Test;

/**
 * 验证 CHashMap 在运行中事务内捕获分片计数快照，提交回调不再访问 TableX：
 * 1. 对空桶/非空桶执行未命中 remove 不触发 State Is Not Running: Completed 异常日志；
 * 2. 同事务 clear + put 与 put + clear 后分片 size 精确一致；
 * 3. 嵌套 savepoint 回滚丢弃内层计数回调，不影响已提交分片计数。
 */
@Fast
public class TestCHashMapCommitSizeSnapshot {

	private static final AtomicInteger NEXT_SERVER_ID =
			new AtomicInteger(FastServerIds.TEST_CHASHMAP_COMMIT_SIZE_SNAPSHOT);

	private record Env(Application app, LinkedMap.Module linkedMapModule) implements AutoCloseable {
		static Env open() throws Exception {
			var conf = new Config();
			conf.setServiceManager("disable");
			conf.setServerId(NEXT_SERVER_ID.getAndIncrement());
			conf.setTakeoverMode("off");
			conf.setDefaultTableConf(new Config.TableConf());
			var dbConf = new Config.DatabaseConf();
			dbConf.setDatabaseUrl("chashmap_commit_size_" + conf.getServerId());
			conf.getDatabaseConfMap().putIfAbsent("", dbConf);
			var app = new Application("TestCHashMapCommitSizeSnapshot@" + conf.getServerId(), conf);
			var module = new LinkedMap.Module(app);
			app.start();
			return new Env(app, module);
		}

		void run(String name, FuncLong action) {
			assertEquals(Procedure.Success, TaskSpec.ofProcedure(app.newProcedure(action, name)).call());
		}

		@Override
		public void close() throws Exception {
			app.stop();
		}
	}

	private static final class RecordingAppender extends AbstractAppender {
		final List<LogEvent> events = new CopyOnWriteArrayList<>();

		RecordingAppender() {
			super("chashmap-txn-recorder", null, null, true, Property.EMPTY_ARRAY);
		}

		@Override
		public void append(LogEvent event) {
			events.add(event.toImmutable());
		}
	}

	@Test
	public void testMissingKeyRemoveDoesNotLogPostCommitTableAccessError() throws Exception {
		try (var env = Env.open()) {
			var map = env.linkedMapModule.openConcurrent("missingRemoveMap", BMyBean.class);
			var txnLogger = (Logger)LogManager.getLogger(Transaction.class);
			var appender = new RecordingAppender();
			appender.start();
			txnLogger.addAppender(appender);
			try {
				// 空桶未命中 remove：不读 _tLinkedMaps，提交回调不得访问表
				env.run("removeOnEmptyBucket", () -> {
					assertNull(map.remove("absent-empty-bucket"));
					return Procedure.Success;
				});
				assertEquals(0L, map.size());
				assertTrue(map.isEmpty());

				// 先写入一个 key，再对不同桶及同桶执行未命中 remove
				env.run("seedOne", () -> {
					var b = new BMyBean();
					b.setI(7);
					map.put("existing-key", b);
					return Procedure.Success;
				});
				assertEquals(1L, map.size());

				env.run("removeAbsentAcrossBuckets", () -> {
					for (int i = 0; i < 32; i++)
						assertNull(map.remove("absent-key-" + i));
					return Procedure.Success;
				});
				assertEquals(1L, map.size());

				// 命中删除：计数准确归零且无提交回调异常
				env.run("removeExisting", () -> {
					var removed = map.remove("existing-key");
					assertNotNull(removed);
					assertEquals(7, removed.getI());
					return Procedure.Success;
				});
				assertEquals(0L, map.size());
				assertTrue(map.isEmpty());
			} finally {
				txnLogger.removeAppender(appender);
				appender.stop();
			}

			var badEvents = appender.events.stream()
					.filter(e -> e.getLevel().isMoreSpecificThan(Level.ERROR)
							|| (e.getThrown() != null && String.valueOf(e.getThrown().getMessage())
									.contains("State Is Not Running")))
					.toList();
			assertTrue(badEvents.isEmpty(), "whileCommit 不得因提交后访问表抛出异常: " + badEvents);
		}
	}

	@Test
	public void testClearAndPutInSameTransactionKeepsExactSize() throws Exception {
		try (var env = Env.open()) {
			var map = env.linkedMapModule.openConcurrent("clearPutSameTxnMap", BMyBean.class);
			env.run("seed", () -> {
				for (int i = 0; i < 8; i++) {
					var b = new BMyBean();
					b.setI(i);
					map.put("k-" + i, b);
				}
				return Procedure.Success;
			});
			assertEquals(8L, map.size());

			// 同事务先 clear 再 put：最终 size 必须为 1
			env.run("clearThenPut", () -> {
				map.clear();
				var b = new BMyBean();
				b.setI(99);
				map.put("after-clear", b);
				return Procedure.Success;
			});
			assertEquals(1L, map.size());
			assertFalse(map.isEmpty());
			env.run("verifyAfterClearThenPut", () -> {
				assertEquals(99, requireNonNull(map.get("after-clear")).getI());
				assertNull(map.get("k-0"));
				return Procedure.Success;
			});

			// 同事务先 put 再 clear：最终 size 必须归零
			env.run("putThenClear", () -> {
				var b = new BMyBean();
				b.setI(100);
				map.put("before-clear", b);
				map.clear();
				return Procedure.Success;
			});
			assertEquals(0L, map.size());
			assertTrue(map.isEmpty());
		}
	}

	@Test
	public void testNestedSavepointRollbackDoesNotCorruptSize() throws Exception {
		try (var env = Env.open()) {
			var map = env.linkedMapModule.openConcurrent("savepointRollbackMap", BMyBean.class);
			env.run("seed", () -> {
				var b1 = new BMyBean();
				b1.setI(1);
				map.put("k1", b1);
				var b2 = new BMyBean();
				b2.setI(2);
				map.put("k2", b2);
				return Procedure.Success;
			});
			assertEquals(2L, map.size());

			env.run("outerWithRolledBackInner", () -> {
				var innerRc = env.app.newProcedure(() -> {
					var b3 = new BMyBean();
					b3.setI(3);
					map.put("k3", b3);
					map.remove("k1");
					map.clear();
					return Procedure.LogicError; // 回滚内层 savepoint 及其 whileCommit
				}, "innerRollback").call();
				assertEquals(Procedure.LogicError, innerRc);

				map.getOrAdd("k4").setI(4);
				return Procedure.Success;
			});

			assertEquals(3L, map.size());
			env.run("verifyAfterSavepointRollback", () -> {
				assertEquals(1, requireNonNull(map.get("k1")).getI());
				assertEquals(2, requireNonNull(map.get("k2")).getI());
				assertNull(map.get("k3"));
				assertEquals(4, requireNonNull(map.get("k4")).getI());
				return Procedure.Success;
			});
		}
	}
}
