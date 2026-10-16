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
import Zeze.Transaction.Bean;
import Zeze.Transaction.CheckpointMode;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TableX;
import Zeze.Util.FuncLong;

/**
 * readFastPath 回归：oldTable 倒库在读路径标脏（TableX.load，全仓读路径标脏仅此一处），
 * 记录孤立未注册，老路径纯读提交是它迁移落库的唯一通道。原谓词（!hasDirtyWrite）会让它
 * 走快路致迁移搁浅（新库永远查不到）；hasUnregisteredDirty（脏&&孤立组签名）修复。
 * 断言：纯读装载 → 提交 → checkpointRun → 新库必须查得到（selectFromDatabase 只读新库）。
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

			// 纯读装载：新库miss→oldTable命中→Table模式读路径setDirty（孤立未注册）。
			var out = new long[1];
			Assertions.assertEquals(Procedure.Success, read(KEY_OLD, out), "纯读装载必须成功");
			Assertions.assertEquals(100, out[0], "读值来自旧库");
			Assertions.assertNull(table.selectFromDatabase(KEY_OLD), "提交后checkpoint前：尚未落新库");

			// 核心：注册通道必须在——修复前快路吞掉注册，checkpointRun 后新库仍为空。
			requireNonNull(app).checkpointRun();
			var migrated = table.selectFromDatabase(KEY_OLD);
			Assertions.assertNotNull(migrated, "纯读提交必须保留倒库记录的flush注册（readFastPath不得放过未注册脏）");
			Assertions.assertEquals(100, migrated.value, "迁移落库的值等于旧库值");
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
