package Zeze.Transaction;

import static java.util.Objects.requireNonNull;

import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import Zeze.Application;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Services.GlobalCacheManagerConst;
import Zeze.Transaction.Bean;
import Zeze.Transaction.CheckpointMode;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TableX;
import Zeze.Util.FuncLong;

/**
 * oldTable 倒库在所有 CheckpointMode 下统一由 TableX.load 通过 migrateOldRecord 同步迁入新主库与
 * 本地镜像，不再对孤立记录标脏（避免 selectCopy/回滚/GCM reduce 时残留未注册脏标记导致跨进程丢更新或
 * 缓存永久无法驱逐）；同时验证 RelativeRecordSet.flushWhenReduce 对孤立脏记录的防御兜底刷盘。
 */
@Fast
public class TestRrsReadFastPathOldTableMigration {
	private static final long KEY_OLD = 1L;

	@TempDir
	Path tempDir;

	private @Nullable Application app;
	private @Nullable tMigrate table;

	private void startAppWithOldTable() throws Exception {
		var config = new Config();
		config.setServiceManager("disable"); // 无GCM；readFastPath 全形态默认开（逃生门才需显式配置）
		config.setCheckpointMode(CheckpointMode.Table); // 读路径标脏的目标分支
		config.setServerId(FastServerIds.TEST_RRS_READ_FAST_PATH_OLD_TABLE);
		var tableConf = new Config.TableConf();
		tableConf.setDatabaseOldMode(1);
		tableConf.setDatabaseOldName("old");
		config.setDefaultTableConf(tableConf);
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseType(Config.DbType.RocksDb);
		dbConf.setDatabaseUrl(tempDir.resolve("dbhome").toString());
		config.getDatabaseConfMap().put("", dbConf);
		var oldDbConf = new Config.DatabaseConf();
		oldDbConf.setName("old"); // createDatabase按DatabaseConf.name注册，非map的key
		oldDbConf.setDatabaseType(Config.DbType.RocksDb);
		oldDbConf.setDatabaseUrl(tempDir.resolve("dbold").toString());
		config.getDatabaseConfMap().put("old", oldDbConf);

		app = new Application("TestRrsReadFastPathOldTableMigration", config);
		table = new tMigrate();
		app.addTable("", table);
		app.start();
	}

	// 种子数据只写旧库，模拟迁移模式下尚未倒库的存量记录（openTable为getOrAdd语义，重复打开安全）。
	private void seedOldDatabase(long key, long value) throws Exception {
		var oldDb = requireNonNull(app).getDatabase("old");
		var oldDbTable = oldDb.openTable(requireNonNull(table).getName(), table.getId());
		var seed = new MigrateBean();
		seed.value = value;
		try (var t = oldDb.beginTransaction()) {
			oldDbTable.replace(t, table.encodeKey(key), ByteBuffer.encode(seed));
			t.commit();
		}
	}

	// 纯读（无任何写）：装载路径触发倒库分支的 setDirty，提交时 hasDirtyWrite=false——
	// 修复前后谓词在此分叉（修复前走快路、修复后 hasUnregisteredDirty 留在原路径）。
	private long read(long key, final long[] out) {
		return requireNonNull(app).newProcedure((FuncLong)() -> {
			var v = requireNonNull(table).get(key);
			if (v != null)
				out[0] = v.value;
			return 0L;
		}, "TestRrsReadFastPathOldTableMigration.read").call();
	}

	@Test
	public void testPureReadMigratesOldTableRecord() throws Exception {
		startAppWithOldTable();
		try {
			seedOldDatabase(KEY_OLD, 100);
			Assertions.assertNull(requireNonNull(table).selectFromDatabase(KEY_OLD), "前置：倒库记录仅存旧库");

			// 纯读装载：新库 miss → oldTable 命中 → 统一在读路径同步迁移至新主库与本地镜像，不标脏。
			var out = new long[1];
			Assertions.assertEquals(Procedure.Success, read(KEY_OLD, out), "纯读装载必须成功");
			Assertions.assertEquals(100, out[0], "读值来自旧库");

			var migratedImmediate = table.selectFromDatabase(KEY_OLD);
			Assertions.assertNotNull(migratedImmediate, "纯读装载后新库必须立即可见已迁移记录");
			Assertions.assertEquals(100, migratedImmediate.value, "立即迁移落库的值等于旧库值");
			var cached = requireNonNull(requireNonNull(table.getCache()).get(KEY_OLD));
			Assertions.assertFalse(cached.getDirty(), "统一同步迁移后缓存记录不得残留脏标记");

			requireNonNull(app).checkpointRun();
			var migrated = table.selectFromDatabase(KEY_OLD);
			Assertions.assertNotNull(migrated, "checkpointRun 后新库记录保持完整");
			Assertions.assertEquals(100, migrated.value, "迁移落库的值等于旧库值");
		} finally {
			requireNonNull(app).stop();
		}
	}

	@Test
	public void testSelectCopyAndRollbackMigrateWithoutDirtyLeak() throws Exception {
		startAppWithOldTable();
		try {
			long keyCopy = 2L;
			long keyRollback = 3L;
			seedOldDatabase(keyCopy, 200);
			seedOldDatabase(keyRollback, 300);

			// 1. selectCopy 触发旧表装载：同步迁入新库且不留脏标记
			var copied = requireNonNull(table).selectCopy(keyCopy);
			Assertions.assertNotNull(copied);
			Assertions.assertEquals(200, copied.value);
			var migratedCopy = table.selectFromDatabase(keyCopy);
			Assertions.assertNotNull(migratedCopy, "selectCopy 命中旧表后新库必须立即可见");
			Assertions.assertEquals(200, migratedCopy.value);
			var copyRecord = requireNonNull(requireNonNull(table.getCache()).get(keyCopy));
			Assertions.assertFalse(copyRecord.getDirty(), "selectCopy 不得留下孤立脏记录");

			// 2. 事务读旧表后回滚：同步迁入新库且不留脏标记
			var rc = requireNonNull(app).newProcedure((FuncLong)() -> {
				var v = table.get(keyRollback);
				Assertions.assertNotNull(v);
				Assertions.assertEquals(300, v.value);
				return Procedure.LogicError; // 故意回滚
			}, "TestOldTableMigration.rollback").call();
			Assertions.assertEquals(Procedure.LogicError, rc);
			var migratedRollback = table.selectFromDatabase(keyRollback);
			Assertions.assertNotNull(migratedRollback, "回滚事务触发旧表装载后新库仍应完成迁移");
			Assertions.assertEquals(300, migratedRollback.value);
			var rollbackRecord = requireNonNull(requireNonNull(table.getCache()).get(keyRollback));
			Assertions.assertFalse(rollbackRecord.getDirty(), "回滚事务不得留下孤立脏记录");
		} finally {
			requireNonNull(app).stop();
		}
	}

	@Test
	public void testReduceInvalidAfterOldTableLoadReadsRemoteUpdate() throws Exception {
		startAppWithOldTable();
		try {
			long key = 4L;
			seedOldDatabase(key, 100);

			// S1 通过 selectCopy 触发旧表装载
			var copied = requireNonNull(table).selectCopy(key);
			Assertions.assertNotNull(copied);
			Assertions.assertEquals(100, copied.value);
			var record = requireNonNull(requireNonNull(table.getCache()).get(key));
			Assertions.assertFalse(record.getDirty());

			// 模拟 GCM 降级为 StateInvalid 且远端节点 S2 将新库更新为 200
			record.setState(GlobalCacheManagerConst.StateInvalid);
			var storage = requireNonNull(table.getStorage());
			var remoteValue = new MigrateBean();
			remoteValue.value = 200;
			try (var t = storage.getDatabaseTable().getDatabase().beginTransaction()) {
				storage.getDatabaseTable().replace(t, table.encodeKey(key), ByteBuffer.encode(remoteValue));
				t.commit();
			}

			// S1 再次读取必须从新库读到 S2 的 200，不能被残留脏值遮蔽成旧值 100
			var out = new long[1];
			Assertions.assertEquals(Procedure.Success, read(key, out));
			Assertions.assertEquals(200, out[0], "降级后重读必须看到远端最新值");
		} finally {
			requireNonNull(app).stop();
		}
	}

	@Test
	public void testFlushWhenReduceDefensivelyFlushesIsolatedDirtyRecord() throws Exception {
		startAppWithOldTable();
		try {
			long key = 5L;
			var out = new long[1];
			Assertions.assertEquals(Procedure.Success, read(key, out)); // 缓存中建立孤立记录（rrs.recordSet == null）
			var record = requireNonNull(requireNonNull(table).getCache()).get(key);
			Assertions.assertNotNull(record);

			var bean = new MigrateBean();
			bean.value = 555;
			record.setSoftValue(bean);
			record.setDirty();
			Assertions.assertTrue(record.getDirty());

			RelativeRecordSet.flushWhenReduce(record, requireNonNull(requireNonNull(app).getCheckpoint()));

			Assertions.assertFalse(record.getDirty(), "flushWhenReduce 必须刷盘并清除孤立脏记录的脏标记");
			var persisted = table.selectFromDatabase(key);
			Assertions.assertNotNull(persisted, "孤立脏记录降级前必须落库");
			Assertions.assertEquals(555, persisted.value);
		} finally {
			requireNonNull(app).stop();
		}
	}

	// ---------------------------------------------------------------
	// 测试用表和值类型
	// ---------------------------------------------------------------

	public static final class MigrateBean extends Bean {
		public long value;

		@Override
		public void encode(ByteBuffer bb) {
			bb.WriteLong(value);
		}

		@Override
		public void decode(IByteBuffer bb) {
			value = bb.ReadLong();
		}

		@Override
		public Bean copy() {
			var c = new MigrateBean();
			c.value = value;
			return c;
		}
	}

	/** 最小 TableX 实现（Long key → MigrateBean），非内存表（RocksDb存储）。 */
	public static final class tMigrate extends TableX<Long, MigrateBean> {
		public tMigrate() {
			super(731302, "UnitTest_TestRrsReadFastPathOldTableMigration_tMigrate");
		}

		@Override
		public Class<Long> getKeyClass() {
			return Long.class;
		}

		@Override
		public Class<MigrateBean> getValueClass() {
			return MigrateBean.class;
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
		public MigrateBean newValue() {
			return new MigrateBean();
		}
	}
}
