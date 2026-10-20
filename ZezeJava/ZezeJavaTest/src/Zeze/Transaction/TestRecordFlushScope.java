package Zeze.Transaction;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import Zeze.Application;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.SQLStatement;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.Test;
import org.jspecify.annotations.Nullable;

import static org.junit.jupiter.api.Assertions.*;

/** 同一条缓存记录的编码结果归本次 flush 持有，失败保留脏值供下次重新编码。 */
@Fast
public class TestRecordFlushScope {
	static final class Value extends Bean {
		long value;
		boolean failEncoding;
		@Nullable Runnable beforeEncode;

		Value(long value) { this.value = value; }
		@Override public void encode(ByteBuffer bb) {
			var action = beforeEncode;
			if (action != null) {
				beforeEncode = null;
				action.run();
			}
			if (failEncoding)
				throw new IllegalStateException("encoding");
			bb.WriteLong(value);
		}
		@Override public void encodeSQLStatement(ArrayList<String> parents, SQLStatement st) { st.appendLong("value", value); }
		@Override public void decode(IByteBuffer bb) { value = bb.ReadLong(); }
		@Override public Value copy() { return new Value(value); }
	}

	static final class Values extends TableX<Long, Value> {
		private final boolean memory;
		boolean relational;
		Values() { this(false); }
		Values(boolean memory) {
			super(991801, "TestRecordFlushScope_Values");
			this.memory = memory;
		}
		@Override public boolean isMemory() { return memory; }
		@Override public boolean isRelationalMapping() { return relational; }
		@Override public Class<Long> getKeyClass() { return Long.class; }
		@Override public Class<Value> getValueClass() { return Value.class; }
		@Override public Long decodeKey(ByteBuffer bb) { return bb.ReadLong(); }
		@Override public ByteBuffer encodeKey(Long key) {
			var bb = ByteBuffer.Allocate();
			bb.WriteLong(key);
			return bb;
		}
		@Override public Long decodeKeyResultSet(java.sql.ResultSet rs) throws java.sql.SQLException {
			return rs.getLong("__key");
		}
		@Override public void encodeKeySQLStatement(SQLStatement st, Long key) { st.appendLong("__key", key); }
		@Override public Value newValue() { return new Value(0); }
	}

	/** 校验关系表参数并按事务提交到内存；镜像和旧表仍使用真实的二进制后端。 */
	private static final class RelationalDatabase extends Database implements DatabaseRelationalMapping {
		final Map<String, String> rows = new HashMap<>();
		private final Database.Table table;

		RelationalDatabase(Config.DatabaseConf conf) {
			super(null, conf);
			table = (Database.Table)Proxy.newProxyInstance(Database.Table.class.getClassLoader(),
					new Class<?>[]{Database.Table.class}, (proxy, method, args) -> {
						switch (method.getName()) {
						case "getDatabase":
							return this;
						case "replace":
						case "remove":
							var parameters = Objects.requireNonNull(args);
							var transaction = assertInstanceOf(SqlTransaction.class, parameters[0]);
							assertSame(this, transaction.database);
							var key = assertInstanceOf(SQLStatement.class, parameters[1]).getSql().toString();
							var value = method.getName().equals("replace")
									? assertInstanceOf(SQLStatement.class, parameters[2]).getSql().toString() : null;
							transaction.writes.put(key, value);
							return null;
						case "close":
							return null;
						default:
							throw new UnsupportedOperationException(method.getName());
						}
					});
		}

		@Override public Database.Table openTable(String name, int id) { throw new UnsupportedOperationException(); }
		@Override public Database.Table openRelationalTable(String name) { return table; }
		@Override public Map<String, String> getSqlTypeMap() { throw new UnsupportedOperationException(); }
		@Override public Database.Transaction beginTransaction() { return new SqlTransaction(this); }

		private static final class SqlTransaction implements Database.Transaction {
			final RelationalDatabase database;
			final Map<String, @Nullable String> writes = new HashMap<>();

			SqlTransaction(RelationalDatabase database) { this.database = database; }
			@Override public void commit() {
				writes.forEach((key, value) -> {
					if (value != null)
						database.rows.put(key, value);
					else
						database.rows.remove(key);
				});
			}
			@Override public void rollback() { writes.clear(); }
			@Override public void close() { writes.clear(); }
		}
	}

	private static RelationalDatabase installRelationalStorage(Values table) throws Exception {
		var conf = new Config.DatabaseConf();
		conf.setDatabaseUrl("record_flush_scope_sql_" + table.getZeze().getConfig().getServerId());
		var database = new RelationalDatabase(conf);
		table.relational = true;
		table.setDatabase(database);
		// 保留已打开的记录缓存、Rocks 镜像及旧表，只替换主库的关系表适配器。
		var field = TableX.class.getDeclaredField("storage");
		field.setAccessible(true);
		field.set(table, new Storage<>(table, database, table.getName()));
		return database;
	}

	private static Application newApp(Values table) throws Exception {
		return newApp(table, false);
	}

	private static Application newApp(Values table, boolean oldTable) throws Exception {
		var conf = new Config();
		conf.setServerId(FastServerIds.TEST_RECORD_FLUSH_SCOPE);
		conf.setServiceManager("disable");
		conf.setCheckpointMode(CheckpointMode.Table);
		conf.setCheckpointPeriod(3_600_000);
		var tableConf = new Config.TableConf();
		if (oldTable) {
			tableConf.setDatabaseOldName("old");
			tableConf.setDatabaseOldMode(1);
			var old = new Config.DatabaseConf();
			old.setName("old");
			old.setDatabaseUrl("record_flush_scope_old_" + conf.getServerId());
			conf.getDatabaseConfMap().put("old", old);
		}
		conf.setDefaultTableConf(tableConf);
		var db = new Config.DatabaseConf();
		db.setDatabaseUrl("record_flush_scope_" + conf.getServerId());
		conf.getDatabaseConfMap().put("", db);
		var app = new Application("TestRecordFlushScope", conf);
		app.addTable("", table);
		app.start();
		return app;
	}

	private static Record1<Long, Value> put(Application app, Values table, long key, long value) {
		assertEquals(0L, app.newProcedure(() -> {
			table.put(key, new Value(value));
			return 0L;
		}, "TestRecordFlushScope.put").call());
		return Objects.requireNonNull(Objects.requireNonNull(table.getCache()).get(key));
	}

	@Test
	void returnedBuffersDoNotRetainRecordsTransactionsOrEncodingResults() throws Exception {
		var table = new Values();
		var app = newApp(table);
		try {
			var first = put(app, table, 1, 31);
			var second = put(app, table, 2, 32);
			var checkpoint = Objects.requireNonNull(app.getCheckpoint());
			checkpoint.flush(List.of(first, second), null, null);
			assertReturnedBuffersAreEmpty(checkpoint);
			put(app, table, 1, 41);
			var value = (Value)Objects.requireNonNull(put(app, table, 2, 42).getDirtyValue());
			value.failEncoding = true;
			assertThrows(RuntimeException.class, () -> checkpoint.flush(List.of(first, second), null, null));
			assertReturnedBuffersAreEmpty(checkpoint);
			value.failEncoding = false;
			checkpoint.flush(List.of(first, second), null, null);
			assertEquals(41, Objects.requireNonNull(table.selectFromDatabase(1L)).value);
			assertEquals(42, Objects.requireNonNull(table.selectFromDatabase(2L)).value);
		} finally {
			app.stop();
		}
	}

	private static void assertReturnedBuffersAreEmpty(Checkpoint checkpoint) throws Exception {
		var field = Checkpoint.class.getDeclaredField("flushBatches");
		field.setAccessible(true);
		var buffers = (Iterable<?>)field.get(checkpoint);
		boolean found = false;
		for (var buffer : buffers) {
			found = true;
			var records = buffer.getClass().getDeclaredField("records");
			records.setAccessible(true);
			assertTrue(((List<?>)records.get(buffer)).isEmpty());
			for (var name : List.of("keys", "values", "sqlKeys", "sqlValues")) {
				var array = buffer.getClass().getDeclaredField(name);
				array.setAccessible(true);
				for (var reference : (Object[])array.get(buffer))
					assertNull(reference, "returned batch retained a reference in " + name);
			}
		}
		assertTrue(found);
	}

	@Test
	void growingAndReusingABufferKeepsMixedSnapshotsSeparate() throws Exception {
		var table = new Values();
		var app = newApp(table);
		try {
			var checkpoint = Objects.requireNonNull(app.getCheckpoint());
			var first = put(app, table, 1, 61);
			checkpoint.flush(List.of(first), null, null); // 先归还仅一条记录容量的缓冲区。
			var records = new ArrayList<Record>();
			records.add(first); // 干净记录不编码。
			for (long key = 2; key <= 17; key++)
				records.add(put(app, table, key, 60 + key));
			assertEquals(0L, app.newProcedure(() -> {
				table.remove(2L);
				return 0L;
			}, "TestRecordFlushScope.remove").call());
			// 非 Collection 输入，并在复用的小缓冲区上连续扩容。
			checkpoint.flush((Iterable<Record>)records::iterator, null, null);
			assertEquals(61, Objects.requireNonNull(table.selectFromDatabase(1L)).value);
			assertNull(table.selectFromDatabase(2L));
			for (long key = 3; key <= 17; key++)
				assertEquals(60 + key, Objects.requireNonNull(table.selectFromDatabase(key)).value);
			for (var record : records)
				assertFalse(record.getDirty());
			assertReturnedBuffersAreEmpty(checkpoint);
			first = put(app, table, 1, 81);
			checkpoint.flush(List.of(first), null, null); // 大缓冲区再用于小批次。
			assertEquals(81, Objects.requireNonNull(table.selectFromDatabase(1L)).value);
			assertReturnedBuffersAreEmpty(checkpoint);
		} finally {
			app.stop();
		}
	}

	@Test
	void memoryTableFlushWritesAndDeletesTheLocalMirrorWithoutMainStorage() throws Exception {
		var table = new Values(true);
		var app = newApp(table);
		try {
			var checkpoint = Objects.requireNonNull(app.getCheckpoint());
			var record = put(app, table, 1, 91);
			assertNull(table.getStorage());
			checkpoint.flush(List.of(record), null, null);
			assertFalse(record.getDirty());
			assertEquals(91, Objects.requireNonNull(table.getLocalRocksCacheTable().find(table, 1L)).value);
			assertEquals(0L, app.newProcedure(() -> {
				table.remove(1L);
				return 0L;
			}, "TestRecordFlushScope.removeMemory").call());
			checkpoint.flush(List.of(record), null, null);
			assertFalse(record.getDirty());
			assertNull(table.getLocalRocksCacheTable().find(table, 1L));
			assertReturnedBuffersAreEmpty(checkpoint);
		} finally {
			app.stop();
		}
	}

	@Test
	void relationalEncodingKeepsTheLocalMirrorBinaryForUpdatesAndDeletes() throws Exception {
		var table = new Values(true);
		var app = newApp(table);
		try {
			// 只切换编码格式；保留真实的本地 Rocks 镜像，不创建外部 SQL 服务。
			table.relational = true;
			var conf = new Config.DatabaseConf();
			conf.setDatabaseUrl("record_flush_scope_relational_" + app.getConfig().getServerId());
			table.setDatabase(new RelationalDatabase(conf));
			var checkpoint = Objects.requireNonNull(app.getCheckpoint());
			var record = put(app, table, 1, 93);
			assertEquals("__key=1", table.encodeSqlKey(1L).getSql().toString());
			var sqlValue = TableX.encodeSqlValue(record.getDirtyValue());
			assertNotNull(sqlValue);
			assertEquals("value=93", sqlValue.getSql().toString());
			checkpoint.flush(List.of(record), null, null);
			assertFalse(record.getDirty());
			assertEquals(93, Objects.requireNonNull(table.getLocalRocksCacheTable().find(table, 1L)).value);
			assertEquals(0L, app.newProcedure(() -> {
				table.remove(1L);
				return 0L;
			}, "TestRecordFlushScope.removeRelational").call());
			assertNull(TableX.encodeSqlValue(record.getDirtyValue()));
			checkpoint.flush(List.of(record), null, null);
			assertFalse(record.getDirty());
			assertNull(table.getLocalRocksCacheTable().find(table, 1L));
			assertReturnedBuffersAreEmpty(checkpoint);
		} finally {
			app.stop();
		}
	}

	@Test
	void relationalMainStorageUsesSqlWhileMirrorAndOldTableUseBinary() throws Exception {
		var table = new Values();
		var app = newApp(table, true);
		try {
			var record = put(app, table, 1, 93);
			var old = Objects.requireNonNull(table.getOldTable());
			var key = table.encodeKey(1L);
			try (var tx = old.getDatabase().beginTransaction()) {
				old.replace(tx, key, ByteBuffer.encode(new Value(53)));
				tx.commit();
			}
			var database = installRelationalStorage(table);
			var checkpoint = Objects.requireNonNull(app.getCheckpoint());
			checkpoint.flush(List.of(record), null, null);
			assertEquals("value=93", database.rows.get("__key=1"));
			assertEquals(93, Objects.requireNonNull(table.getLocalRocksCacheTable().find(table, 1L)).value);
			assertNotNull(((Database.AbstractKVTable)old).find(key));
			assertFalse(record.getDirty());
			assertReturnedBuffersAreEmpty(checkpoint);

			assertSame(record, put(app, table, 1, 94));
			checkpoint.flush(List.of(record), null, null);
			assertEquals("value=94", database.rows.get("__key=1"));
			assertEquals(94, Objects.requireNonNull(table.getLocalRocksCacheTable().find(table, 1L)).value);
			assertEquals(0L, app.newProcedure(() -> {
				table.remove(1L);
				return 0L;
			}, "TestRecordFlushScope.removeSql").call());
			checkpoint.flush(List.of(record), null, null);
			assertTrue(database.rows.isEmpty());
			assertNull(table.getLocalRocksCacheTable().find(table, 1L));
			assertNull(((Database.AbstractKVTable)old).find(key));
			assertFalse(record.getDirty());
			assertReturnedBuffersAreEmpty(checkpoint);
		} finally {
			app.stop();
		}
	}

	@Test
	void failedRelationalEncodingClearsEveryArrayBeforeRetryAndSmallerBatchReuse() throws Exception {
		var table = new Values();
		var app = newApp(table);
		try {
			var first = put(app, table, 1, 101);
			var second = put(app, table, 2, 102);
			var database = installRelationalStorage(table);
			var checkpoint = Objects.requireNonNull(app.getCheckpoint());
			var value = (Value)Objects.requireNonNull(second.getDirtyValue());
			value.failEncoding = true; // SQL 已编码，第二条的二进制镜像编码失败。
			assertThrows(RuntimeException.class, () -> checkpoint.flush(List.of(first, second), null, null));
			assertTrue(first.getDirty());
			assertTrue(second.getDirty());
			assertTrue(database.rows.isEmpty());
			assertNull(table.getLocalRocksCacheTable().find(table, 1L));
			assertNull(table.getLocalRocksCacheTable().find(table, 2L));
			assertReturnedBuffersAreEmpty(checkpoint);

			assertSame(first, put(app, table, 1, 111));
			value.failEncoding = false;
			checkpoint.flush(List.of(first, second), null, null);
			assertEquals("value=111", database.rows.get("__key=1"));
			assertEquals("value=102", database.rows.get("__key=2"));
			assertEquals(111, Objects.requireNonNull(table.getLocalRocksCacheTable().find(table, 1L)).value);
			assertEquals(102, Objects.requireNonNull(table.getLocalRocksCacheTable().find(table, 2L)).value);
			assertFalse(first.getDirty());
			assertFalse(second.getDirty());
			assertReturnedBuffersAreEmpty(checkpoint);

			assertEquals(0L, app.newProcedure(() -> {
				table.remove(1L);
				return 0L;
			}, "TestRecordFlushScope.removeRetriedSql").call());
			checkpoint.flush(List.of(first), null, null);
			assertEquals(Map.of("__key=2", "value=102"), database.rows);
			assertNull(table.getLocalRocksCacheTable().find(table, 1L));
			assertEquals(102, Objects.requireNonNull(table.getLocalRocksCacheTable().find(table, 2L)).value);
			assertReturnedBuffersAreEmpty(checkpoint);
		} finally {
			app.stop();
		}
	}

	@Test
	void reentrantFlushUsesAnIndependentBuffer() throws Exception {
		var table = new Values();
		var app = newApp(table);
		try {
			var outer = put(app, table, 1, 51);
			var inner = put(app, table, 2, 52);
			var checkpoint = Objects.requireNonNull(app.getCheckpoint());
			((Value)Objects.requireNonNull(outer.getDirtyValue())).beforeEncode =
					() -> checkpoint.flush(List.of(inner), null, null);
			checkpoint.flush(List.of(outer), null, null);
			assertFalse(outer.getDirty());
			assertFalse(inner.getDirty());
			assertEquals(51, Objects.requireNonNull(table.selectFromDatabase(1L)).value);
			assertEquals(52, Objects.requireNonNull(table.selectFromDatabase(2L)).value);
			assertReturnedBuffersAreEmpty(checkpoint);
		} finally {
			app.stop();
		}
	}

	@Test
	void reentrantFlushOfTheSameRecordDoesNotOverwriteTheNewerPersistedValue() throws Exception {
		var table = new Values();
		var app = newApp(table);
		try {
			var record = put(app, table, 1, 101);
			var checkpoint = Objects.requireNonNull(app.getCheckpoint());
			((Value)Objects.requireNonNull(record.getDirtyValue())).beforeEncode = () -> {
				assertSame(record, put(app, table, 1, 102));
				checkpoint.flush(List.of(record), null, null);
			};
			checkpoint.flush(List.of(record), null, null);
			assertFalse(record.getDirty());
			assertEquals(102, Objects.requireNonNull(table.selectFromDatabase(1L)).value);
			assertReturnedBuffersAreEmpty(checkpoint);
		} finally {
			app.stop();
		}
	}

	@Test
	void concurrentFlushesKeepTheirSnapshotsSeparateWhenBuffersAreReused() throws Exception {
		var table = new Values();
		var app = newApp(table);
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			var checkpoint = Objects.requireNonNull(app.getCheckpoint());
			var barrier = new CyclicBarrier(2);
			Runnable await = () -> {
				try {
					barrier.await(10, TimeUnit.SECONDS);
				} catch (Exception e) {
					throw new IllegalStateException(e);
				}
			};
			for (int round = 1; round <= 20; round++) {
				var first = put(app, table, 1, round);
				var second = put(app, table, 2, 100 + round);
				((Value)Objects.requireNonNull(first.getDirtyValue())).beforeEncode = await;
				((Value)Objects.requireNonNull(second.getDirtyValue())).beforeEncode = await;
				var one = executor.submit(() -> checkpoint.flush(List.of(first), null, null));
				var two = executor.submit(() -> checkpoint.flush(List.of(second), null, null));
				one.get(20, TimeUnit.SECONDS);
				two.get(20, TimeUnit.SECONDS);
				assertEquals(round, Objects.requireNonNull(table.selectFromDatabase(1L)).value);
				assertEquals(100 + round, Objects.requireNonNull(table.selectFromDatabase(2L)).value);
				assertReturnedBuffersAreEmpty(checkpoint);
			}
		} finally {
			app.stop();
		}
	}

	@Test
	void recordDirtiedAfterItsEncodingTurnFailsWithoutLosingTheUpdate() throws Exception {
		var table = new Values();
		var app = newApp(table);
		try {
			var checkpoint = Objects.requireNonNull(app.getCheckpoint());
			var first = put(app, table, 1, 11);
			checkpoint.flush(List.of(first), null, null);
			var second = put(app, table, 2, 22);
			// first 编码时仍干净；second 的回调使它变脏，但本批没有 first 的快照。
			((Value)Objects.requireNonNull(second.getDirtyValue())).beforeEncode =
					() -> assertSame(first, put(app, table, 1, 33));
			assertThrows(RuntimeException.class, () -> checkpoint.flush(List.of(first, second), null, null));
			assertTrue(first.getDirty());
			assertTrue(second.getDirty());
			assertEquals(11, Objects.requireNonNull(table.selectFromDatabase(1L)).value);
			assertReturnedBuffersAreEmpty(checkpoint);
			checkpoint.flush(List.of(first, second), null, null);
			assertEquals(33, Objects.requireNonNull(table.selectFromDatabase(1L)).value);
			assertEquals(22, Objects.requireNonNull(table.selectFromDatabase(2L)).value);
			assertFalse(first.getDirty());
			assertFalse(second.getDirty());
		} finally {
			app.stop();
		}
	}

	@Test
	void deletionRemovesOldTableWithTheCurrentFlushTransaction() throws Exception {
		var table = new Values();
		var app = newApp(table, true);
		try {
			var old = Objects.requireNonNull(table.getOldTable());
			var key = table.encodeKey(3L);
			try (var tx = old.getDatabase().beginTransaction()) {
				old.replace(tx, key, ByteBuffer.encode(new Value(53)));
				tx.commit();
			}
			assertEquals(0L, app.newProcedure(() -> {
				assertEquals(53, Objects.requireNonNull(table.get(3L)).value);
				table.remove(3L);
				return 0L;
			}, "TestRecordFlushScope.remove").call());
			app.checkpointRun();
			assertNull(table.selectFromDatabase(3L));
			assertNull(((Database.AbstractKVTable)old).find(key));
		} finally {
			app.stop();
		}
	}

	@Test
	void failedEncodingCanRetryWithTheLatestDirtyValue() throws Exception {
		var table = new Values();
		var app = newApp(table);
		try {
			var first = put(app, table, 1, 31);
			var second = put(app, table, 2, 32);
			var value = (Value)Objects.requireNonNull(second.getDirtyValue());
			value.failEncoding = true;
			var checkpoint = Objects.requireNonNull(app.getCheckpoint());
			assertThrows(RuntimeException.class, () -> checkpoint.flush(List.of(first, second), null, null));
			assertTrue(first.getDirty());
			assertTrue(second.getDirty());
			assertSame(first, put(app, table, 1, 41));
			value.failEncoding = false;
			checkpoint.flush(List.of(first, second), null, null);
			assertFalse(first.getDirty());
			assertFalse(second.getDirty());
			assertEquals(41, Objects.requireNonNull(table.selectFromDatabase(1L)).value);
			assertEquals(32, Objects.requireNonNull(table.selectFromDatabase(2L)).value);
		} finally {
			app.stop();
		}
	}
}
