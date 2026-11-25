package Zeze.Transaction;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import Zeze.Application;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Logs.LogLong;
import harness.Fast;
import harness.FastServerIds;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Bean.version 递增必须在 Record1.commit 持 fairLock 临界区内且早于 setTimestamp 发布，
 * 保证并发 Serializable 读事务拿到新 timestamp 时必然可见新 version，不出现“新字段 + 旧 version”撕裂。
 */
@Fast
public class TestRecordVersionVisibility {
	private static final class VersionedBean extends Bean {
		private static final VarHandle VH_VALUE;

		static {
			try {
				VH_VALUE = MethodHandles.lookup().findVarHandle(VersionedBean.class, "_value", long.class);
			} catch (ReflectiveOperationException e) {
				throw new ExceptionInInitializerError(e);
			}
		}

		private long _value;
		private long _version;
		private @Nullable Runnable onVersionUpdate;

		public long getValue() {
			if (!isManaged())
				return _value;
			var t = Transaction.getCurrentVerifyRead(this);
			if (t == null)
				return _value;
			var log = t.getLog(objectId() + 1);
			return log != null ? ((LogLong)log).value : _value;
		}

		public void setValue(long v) {
			if (!isManaged()) {
				_value = v;
				return;
			}
			var t = Transaction.getCurrentVerifyWrite(this);
			t.putLog(new LogLong(this, 1, VH_VALUE, v));
		}

		@Override
		public long version() {
			return _version;
		}

		@Override
		public String versionVarName() {
			return "version";
		}

		@Override
		protected void version(long newVersion) {
			_version = newVersion;
			var hook = onVersionUpdate;
			if (hook != null)
				hook.run();
		}

		@Override
		public void encode(ByteBuffer bb) {
			bb.WriteLong(_value);
			bb.WriteLong(_version);
		}

		@Override
		public void decode(IByteBuffer bb) {
			_value = bb.ReadLong();
			_version = bb.ReadLong();
		}

		@Override
		public VersionedBean copy() {
			var c = new VersionedBean();
			c._value = _value;
			c._version = _version;
			return c;
		}
	}

	private static final class VersionedTable extends TableX<Long, VersionedBean> {
		VersionedTable() {
			super(991904, "UnitTest_TestRecordVersionVisibility");
		}

		@Override
		public Class<Long> getKeyClass() {
			return Long.class;
		}

		@Override
		public Class<VersionedBean> getValueClass() {
			return VersionedBean.class;
		}

		@Override
		public Long decodeKey(ByteBuffer bb) {
			return bb.ReadLong();
		}

		@Override
		public ByteBuffer encodeKey(Long key) {
			var bb = ByteBuffer.Allocate(8);
			bb.WriteLong(key);
			return bb;
		}

		@Override
		public Long decodeKeyResultSet(java.sql.ResultSet rs) throws java.sql.SQLException {
			return rs.getLong("__key");
		}

		@Override
		public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement st, Long key) {
			st.appendLong("__key", key);
		}

		@Override
		public VersionedBean newValue() {
			return new VersionedBean();
		}
	}

	private static Application newApp(VersionedTable table) throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(FastServerIds.TEST_RECORD_VERSION_VISIBILITY);
		conf.setCheckpointMode(CheckpointMode.Table);
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("record_version_visibility_" + conf.getServerId());
		conf.getDatabaseConfMap().put("", dbConf);
		var app = new Application("TestRecordVersionVisibility", conf);
		app.addTable("", table);
		app.start();
		return app;
	}

	@Test
	public void testVersionUpdatedInsideFairLockBeforeTimestampPublish() throws Exception {
		var table = new VersionedTable();
		var app = newApp(table);
		try {
			long key = 1L;
			var hookCalled = new AtomicBoolean();
			var heldFairLock = new AtomicBoolean();
			var tsBeforePublish = new AtomicLong();
			var tsAtVersionUpdate = new AtomicLong();

			// 1. 首次插入（put）：version 从 0 -> 1，在 fairLock 内且早于 setTimestamp
			Assertions.assertEquals(Procedure.Success, app.newProcedure(() -> {
				var b = new VersionedBean();
				b.setValue(1);
				table.put(key, b);
				var ra = Objects.requireNonNull(Transaction.getCurrent())
						.getRecordAccessed(new TableKey(table.getId(), key));
				var r = Objects.requireNonNull(ra).atomicTupleRecord.record();
				tsBeforePublish.set(r.getTimestamp());
				b.onVersionUpdate = () -> {
					hookCalled.set(true);
					heldFairLock.set(r.isHeldByCurrentThread());
					tsAtVersionUpdate.set(r.getTimestamp());
				};
				return Procedure.Success;
			}, "insert").call());
			Assertions.assertTrue(hookCalled.get(), "插入提交必须调用 version()");
			Assertions.assertTrue(heldFairLock.get(), "version() 必须在 record.fairLock 临界区内更新");
			Assertions.assertEquals(tsBeforePublish.get(), tsAtVersionUpdate.get(),
					"version() 必须在 setTimestamp() 递增前更新");
			var r = Objects.requireNonNull(Objects.requireNonNull(table.getCache()).get(key));
			Assertions.assertTrue(r.getTimestamp() > tsBeforePublish.get(), "提交后 timestamp 必须递增");
			Assertions.assertEquals(1L, Objects.requireNonNull(r.getSoftValue()).version());

			// 2. 就地字段修改（committedPutLog == null）：version 从 1 -> 2，同样在 fairLock 内且早于 setTimestamp
			hookCalled.set(false);
			heldFairLock.set(false);
			tsBeforePublish.set(r.getTimestamp());
			Assertions.assertEquals(Procedure.Success, app.newProcedure(() -> {
				var b = Objects.requireNonNull(table.get(key));
				b.setValue(2);
				return Procedure.Success;
			}, "modifyInPlace").call());
			Assertions.assertTrue(hookCalled.get(), "就地修改提交必须调用 version()");
			Assertions.assertTrue(heldFairLock.get(), "就地修改时 version() 必须在 record.fairLock 内更新");
			Assertions.assertEquals(tsBeforePublish.get(), tsAtVersionUpdate.get(),
					"就地修改时 version() 必须在 setTimestamp() 前更新");
			Assertions.assertEquals(2L, Objects.requireNonNull(r.getSoftValue()).version());

			// 3. 删除后重插：version 重置为 1
			Assertions.assertEquals(Procedure.Success, app.newProcedure(() -> {
				table.remove(key);
				return Procedure.Success;
			}, "remove").call());
			Assertions.assertEquals(Procedure.Success, app.newProcedure(() -> {
				var b = new VersionedBean();
				b.setValue(10);
				table.put(key, b);
				return Procedure.Success;
			}, "reinsert").call());
			Assertions.assertEquals(1L, Objects.requireNonNull(r.getSoftValue()).version());
		} finally {
			app.stop();
		}
	}

	@Test
	public void testSerializableReaderNeverSeesStaleVersionWithNewValue() throws Exception {
		var table = new VersionedTable();
		var app = newApp(table);
		try (var pool = Executors.newFixedThreadPool(2)) {
			long key = 2L;
			Assertions.assertEquals(Procedure.Success, app.newProcedure(() -> {
				var b = new VersionedBean();
				b.setValue(1);
				table.put(key, b);
				return Procedure.Success;
			}, "init").call());

			var done = new AtomicBoolean();
			var readersStarted = new java.util.concurrent.CountDownLatch(2);
			var failure = new AtomicReference<@Nullable Throwable>();
			var readCount = new AtomicLong();

			Runnable reader = () -> {
				readersStarted.countDown();
				try {
					while (!done.get() && failure.get() == null) {
						final long[] observed = new long[2];
						var rc = app.newProcedure(() -> {
							var b = table.get(key);
							if (b != null) {
								observed[0] = b.getValue();
								observed[1] = b.version();
							}
							return Procedure.Success;
						}, "reader", TransactionLevel.Serializable).call();
						if (rc != Procedure.Success) {
							failure.compareAndSet(null, new AssertionError("reader failed rc=" + rc));
							return;
						}
						if (observed[0] != observed[1]) {
							failure.compareAndSet(null, new AssertionError(
									"committed torn snapshot: value=" + observed[0] + ", version=" + observed[1]));
						}
						readCount.incrementAndGet();
					}
				} catch (Throwable t) {
					failure.compareAndSet(null, t);
				}
			};
			var f1 = pool.submit(reader);
			var f2 = pool.submit(reader);
			Assertions.assertTrue(readersStarted.await(5, TimeUnit.SECONDS), "读线程必须在规定时间内启动");

			try {
				for (int i = 2; i <= 2000 && failure.get() == null; i++) {
					final long next = i;
					var rc = app.newProcedure(() -> {
						var b = Objects.requireNonNull(table.get(key));
						b.setValue(next);
						return Procedure.Success;
					}, "writer", TransactionLevel.Serializable).call();
					Assertions.assertEquals(Procedure.Success, rc);
				}
				long waitDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
				while (readCount.get() == 0 && failure.get() == null && System.nanoTime() < waitDeadline)
					Thread.yield();
			} finally {
				done.set(true);
				f1.get(10, TimeUnit.SECONDS);
				f2.get(10, TimeUnit.SECONDS);
			}

			var err = failure.get();
			if (err != null)
				Assertions.fail("Serializable 读事务观测到 value 与 version 不一致", err);
			Assertions.assertTrue(readCount.get() > 0, "读线程必须执行了有效读取");
		} finally {
			app.stop();
		}
	}
}
