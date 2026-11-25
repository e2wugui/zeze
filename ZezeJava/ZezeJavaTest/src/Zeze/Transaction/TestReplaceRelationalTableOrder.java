package Zeze.Transaction;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import Zeze.Application;
import Zeze.Config;
import Zeze.Schemas;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.SQLStatement;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Application.replaceTable 必须先将新表注册进 Application 与 Database，再执行 table.open(...)：
 * 关系映射表在 open -> new Storage -> openRelationalTable 中会立刻调用 db.getTable(name) 读取新表的
 * RelationalTable 元数据；若 open 失败则回滚全部索引与表缓存，不残留半注册状态。
 */
@Fast
public class TestReplaceRelationalTableOrder {
	private static final class RelBean extends Bean {
		long val;

		@Override
		public void encode(ByteBuffer bb) {
			bb.WriteLong(val);
		}

		@Override
		public void decode(IByteBuffer bb) {
			val = bb.ReadLong();
		}

		@Override
		public void encodeSQLStatement(ArrayList<String> parents, SQLStatement st) {
			st.appendLong("val", val);
		}

		@Override
		public RelBean copy() {
			var c = new RelBean();
			c.val = val;
			return c;
		}
	}

	private static final class RelTable extends TableX<Long, RelBean> {
		RelTable(int id, String name) {
			super(id, name);
		}

		@Override
		public boolean isRelationalMapping() {
			return true;
		}

		@Override
		public Class<Long> getKeyClass() {
			return Long.class;
		}

		@Override
		public Class<RelBean> getValueClass() {
			return RelBean.class;
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
		public void encodeKeySQLStatement(SQLStatement st, Long key) {
			st.appendLong("__key", key);
		}

		@Override
		public RelBean newValue() {
			return new RelBean();
		}
	}

	private static final class RecordingRelationalDatabase extends Database implements DatabaseRelationalMapping {
		final List<Zeze.Transaction.Table> seenDuringOpen = new ArrayList<>();
		boolean failNextOpen;

		RecordingRelationalDatabase(Config.DatabaseConf conf) {
			super(null, conf);
		}

		@Override
		public Map<String, String> getSqlTypeMap() {
			return Map.of("long", "BIGINT");
		}

		@Override
		public Database.Table openTable(String name, int id) {
			throw new UnsupportedOperationException("expected relational table");
		}

		@Override
		public Database.Table openRelationalTable(String name) {
			// 与 DatabaseMySql.TableMysqlRelational / DatabasePostgreSQL.TablePostgreSQLRelational 保持一致：
			// 构造期间必须能通过 getTable(name) 找到新表并生成建表 SQL。
			var table = getTable(name);
			if (table == null)
				throw new IllegalStateException("not found table: " + name);
			var rel = table.getRelationalTable();
			if (rel == null)
				throw new IllegalStateException("relationalTable is null: " + name);
			Assertions.assertFalse(rel.createTableSql().isBlank());
			seenDuringOpen.add(table);
			if (failNextOpen) {
				failNextOpen = false;
				throw new IllegalStateException("simulated relational open failure: " + name);
			}
			return (Database.Table)Proxy.newProxyInstance(Database.Table.class.getClassLoader(),
					new Class<?>[]{Database.Table.class}, (proxy, method, args) -> switch (method.getName()) {
						case "getDatabase" -> this;
						case "close" -> null;
						case "isNew" -> true;
						default -> throw new UnsupportedOperationException(method.getName());
					});
		}

		@Override
		public Database.Transaction beginTransaction() {
			return new Database.Transaction() {
				@Override
				public void commit() {
				}

				@Override
				public void rollback() {
				}

				@Override
				public void close() {
				}
			};
		}
	}

	private static Schemas buildSchemas(String... tableNames) {
		var schemas = new Schemas();
		var bean = new Schemas.Bean("RelBean", false);
		var v = new Schemas.Variable();
		v.id = 1;
		v.name = "val";
		v.typeName = "long";
		bean.getVariables().put(v.id, v);
		schemas.addBean(bean);
		for (var name : tableNames)
			schemas.addTable(new Schemas.Table(name, "long", "RelBean"));
		schemas.compile();
		return schemas;
	}

	@SuppressWarnings("unchecked")
	private static RecordingRelationalDatabase attachRelationalDb(Application app, String dbName) throws Exception {
		var relConf = new Config.DatabaseConf();
		relConf.setName(dbName);
		relConf.setDatabaseUrl("replace_rel_db_" + app.getConfig().getServerId());
		var relDb = new RecordingRelationalDatabase(relConf);
		var field = Application.class.getDeclaredField("databases");
		field.setAccessible(true);
		((Map<String, Database>)field.get(app)).put(dbName, relDb);
		return relDb;
	}

	private static Application newStartedApp(Schemas schemas) throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(FastServerIds.TEST_REPLACE_RELATIONAL_TABLE_ORDER);
		conf.setCheckpointMode(CheckpointMode.Table);
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("replace_rel_main_" + conf.getServerId());
		conf.getDatabaseConfMap().put("", dbConf);
		var app = new Application("TestReplaceRelationalTableOrder", conf);
		app.setSchemas(schemas);
		app.start();
		return app;
	}

	@Test
	public void testReplaceTableRegistersBeforeOpenForNewAndExistingRelationalTables() throws Exception {
		var tableName = "UnitTest_ReplaceRel_T1";
		int tableId = 9919071;
		var app = newStartedApp(buildSchemas(tableName));
		try {
			var relDb = attachRelationalDb(app, "rel");

			// 1. 热更期间新增关系映射表：openRelationalTable 必须从 db.getTable(name) 拿到新表
			var t1 = new RelTable(tableId, tableName);
			app.replaceTable("rel", t1);
			Assertions.assertEquals(1, relDb.seenDuringOpen.size());
			Assertions.assertSame(t1, relDb.seenDuringOpen.get(0),
					"新增关系映射表打开时 db.getTable(name) 必须已指向新表");
			Assertions.assertSame(t1, app.getTable(tableId));
			Assertions.assertSame(t1, app.getTable(tableName));
			Assertions.assertSame(t1, relDb.getTable(tableName));

			// 2. 热更替换既有关系映射表：openRelationalTable 必须拿到新表 t2 而非旧表 t1
			var t2 = new RelTable(tableId, tableName);
			app.replaceTable("rel", t2);
			Assertions.assertEquals(2, relDb.seenDuringOpen.size());
			Assertions.assertSame(t2, relDb.seenDuringOpen.get(1),
					"替换关系映射表打开时 db.getTable(name) 必须已切换为新表而非旧表");
			Assertions.assertSame(t2, app.getTable(tableId));
			Assertions.assertSame(t2, app.getTable(tableName));
			Assertions.assertSame(t2, relDb.getTable(tableName));
			Assertions.assertNull(t1.getCache(), "旧表替换成功后必须被禁用关闭缓存");
			Assertions.assertNotNull(t2.getCache(), "新表缓存必须已就绪");
		} finally {
			app.stop();
		}
	}

	@Test
	public void testReplaceTableRollsBackIndicesWhenOpenFails() throws Exception {
		var existingName = "UnitTest_ReplaceRel_Existing";
		int existingId = 9919072;
		var freshName = "UnitTest_ReplaceRel_Fresh";
		int freshId = 9919073;
		var app = newStartedApp(buildSchemas(existingName, freshName));
		try {
			var relDb = attachRelationalDb(app, "rel");
			var recentField = Application.class.getDeclaredField("replaceTableRecent");
			recentField.setAccessible(true);
			var recent = (List<?>)recentField.get(app);

			// 1. 新增表 open 失败：所有索引与 replaceTableRecent 必须完全回滚，失败表缓存关闭
			var failingFresh = new RelTable(freshId, freshName);
			relDb.failNextOpen = true;
			Assertions.assertThrows(IllegalStateException.class, () -> app.replaceTable("rel", failingFresh));
			Assertions.assertNull(app.getTable(freshId));
			Assertions.assertNull(app.getTable(freshName));
			Assertions.assertNull(relDb.getTable(freshName));
			Assertions.assertNull(TableKey.tables.get(freshId));
			Assertions.assertFalse(recent.contains(failingFresh), "失败表不得留在 replaceTableRecent");
			Assertions.assertNull(failingFresh.getCache(), "失败表已建缓存必须关闭释放定时器");

			// 2. 先成功打开 existing，再模拟替换 existing 时 open 失败：旧表索引与活性必须完整恢复
			var existing = new RelTable(existingId, existingName);
			app.replaceTable("rel", existing);
			int recentSizeBefore = recent.size();

			var failingReplacement = new RelTable(existingId, existingName);
			relDb.failNextOpen = true;
			Assertions.assertThrows(IllegalStateException.class, () -> app.replaceTable("rel", failingReplacement));
			Assertions.assertSame(existing, app.getTable(existingId), "替换失败后 tables 必须恢复旧表");
			Assertions.assertSame(existing, app.getTable(existingName), "替换失败后 tableNameMap 必须恢复旧表");
			Assertions.assertSame(existing, relDb.getTable(existingName), "替换失败后 db.tables 必须恢复旧表");
			Assertions.assertEquals(existingName, TableKey.tables.get(existingId));
			Assertions.assertEquals(recentSizeBefore, recent.size(), "替换失败后 replaceTableRecent 必须回滚");
			Assertions.assertNotNull(existing.getCache(), "替换失败时旧表不得被禁用");
			Assertions.assertNull(failingReplacement.getCache(), "失败的新表缓存必须关闭释放定时器");
		} finally {
			app.stop();
		}
	}
}
