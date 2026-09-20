package UnitTest.Zeze;

import java.util.ArrayList;

import Zeze.Application;
import Zeze.Config;
import Zeze.Schemas;
import Zeze.Transaction.Database;
import org.jetbrains.annotations.NotNull;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Z2-F1回归：checkCompatible的renameTable副作用原先在首个循环内逐表执行，
 * 先于失败的兼容检查提交——兼容失败抛异常后rename已生效且重试不幂等
 * （原表已被改名），失败后重启永久失败。修复：先全量判定兼容，全部通过后
 * 再统一执行renameTable。用记录型Database观测rename调用时序。
 */
@Fast
public class TestZ2F1RenameTableAfterCompatCheck {
	private static final String T_UPGRADE = "Z2F1UpgradeTable"; // 版本升级（触发rename）
	private static final String T_BREAK = "Z2F1BreakTable"; // 不兼容（value int→string）

	private Application app;
	private RecordingDb db;

	/** 仅记录renameTable调用的最小Database实现（checkCompatible只触达renameTable）。 */
	static class RecordingDb extends Database {
		final ArrayList<String> renames = new ArrayList<>();

		RecordingDb(Application zeze, Config.DatabaseConf conf) {
			super(zeze, conf);
		}

		@Override
		public @NotNull Database.Table openTable(@NotNull String name, int id) {
			throw new UnsupportedOperationException("not used in this test");
		}

		@Override
		public @NotNull Database.Transaction beginTransaction() {
			throw new UnsupportedOperationException("not used in this test");
		}

		@Override
		public void renameTable(@NotNull String tableOldName, @NotNull String tableNewName) {
			renames.add(tableOldName + "->" + tableNewName);
		}
	}

	@BeforeEach
	public void setUp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(12822);
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseType(Config.DbType.Memory);
		dbConf.setDatabaseUrl("z2f1_memory");
		conf.getDatabaseConfMap().put("", dbConf);
		app = new Application("TestZ2F1RenameTableAfterCompatCheck", conf);
		app.start();
		// 替换默认库为记录型：checkCompatible按TableConf的databaseName("")取库。
		db = new RecordingDb(app, dbConf);
		app.getDatabases().put("", db);
	}

	@AfterEach
	public void tearDown() throws Exception {
		if (app.getStartState() != Application.StartState.eStopped)
			app.stop();
	}

	private static Schemas schemas(int upgradeVersion, String breakValueType) {
		var s = new Schemas();
		s.addTable(new Schemas.Table(T_UPGRADE, "long", "int", upgradeVersion));
		s.addTable(new Schemas.Table(T_BREAK, "long", breakValueType, 0));
		s.compile();
		return s;
	}

	/** 兼容检查失败时：异常抛出且renameTable一次都不能执行（副作用不得先于失败的检查提交）。 */
	@Test
	public void testIncompatibleThrowsBeforeAnyRename() {
		var previous = schemas(0, "int");
		var current = schemas(1, "string"); // T_BREAK不兼容 + T_UPGRADE版本升级

		var ex = assertThrows(IllegalStateException.class, () -> current.checkCompatible(previous, app));
		assertTrue(ex.getMessage().contains("Incompatible"), "必须因不兼容而失败: " + ex.getMessage());
		assertEquals(0, db.renames.size(),
				"兼容检查失败时renameTable不得执行（修复前T_UPGRADE的rename已在首个循环内先提交，"
						+ "重试不幂等，失败后重启永久失败）");
	}

	/** 全部兼容时：版本升级表统一rename，时序调整为检查全部通过之后（行为保留）。 */
	@Test
	public void testCompatibleRenamesAfterCheck() throws Exception {
		var previous = schemas(0, "int");
		var current = schemas(1, "int"); // 全兼容 + T_UPGRADE版本升级

		current.checkCompatible(previous, app);
		assertEquals(1, db.renames.size(), "版本升级表必须恰rename一次");
		assertTrue(db.renames.get(0).equals(T_UPGRADE + "->zeze_backup_" + T_UPGRADE + "_0"),
				"rename目标名保持既有格式: " + db.renames.get(0));
	}
}
