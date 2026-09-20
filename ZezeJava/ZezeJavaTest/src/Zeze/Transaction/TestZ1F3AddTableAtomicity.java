package Zeze.Transaction;

import Zeze.Application;
import Zeze.Config;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Z1-F3回归：addTable原先先登记后查重（TableKey.tables.put与tables.putIfAbsent
 * 先于两道唯一性校验），表名冲突抛异常后留下半注册幻影表，重复id时还会先污染
 * 既有表的id→name映射。修复：两道唯一性校验全部通过后再统一登记，异常路径原子化。
 * 测试位于Zeze.Transaction包：Table存在包级抽象方法，桩必须同包才能实现。
 */
@Fast
public class TestZ1F3AddTableAtomicity {
	private Application app;

	/** 最小Table桩：本测试只触达id/name登记路径，不触达存储。 */
	static class StubTable extends Table {
		StubTable(int id, String name) {
			super(id, name);
		}

		@Override
		public @Nullable Storage<?, ?> open(@NotNull Application a, @NotNull Database database,
											@Nullable DatabaseRocksDb.Table localTable) {
			throw new UnsupportedOperationException();
		}

		@Override
		void close() {
		}

		@Override
		public @Nullable Storage<?, ?> getStorage() {
			return null;
		}

		@Override
		public @Nullable Database.Table getOldTable() {
			return null;
		}

		@Override
		public boolean isNew() {
			return false;
		}

		@Override
		public @NotNull Bean newValue() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void reduceShare(@NotNull Zeze.Services.GlobalCacheManager.Reduce rpc,
								@NotNull Zeze.Serialize.ByteBuffer bbKey) {
		}

		@Override
		public void reduceInvalid(@NotNull Zeze.Services.GlobalCacheManager.Reduce rpc,
								  @NotNull Zeze.Serialize.ByteBuffer bbKey) {
		}

		@Override
		void reduceInvalidAllLocalOnly(int globalCacheManagerHashIndex) {
		}

		@Override
		public void removeEncodedKey(@NotNull Zeze.Net.Binary encodedKey) {
		}

		@Override
		public void tryAlter() {
		}

		@Override
		public Zeze.Schemas.RelationalTable getRelationalTable() {
			return null;
		}

		@Override
		public void open(@NotNull Table exist, @NotNull Application a) {
		}

		@Override
		public void disable() {
		}

		@Override
		public DatabaseRocksDb.Table getLocalRocksCacheTable() {
			return null;
		}

		@Override
		public long walkMemoryAny(@NotNull TableWalkHandle<Object, Bean> handle) {
			return 0;
		}

		@Override
		public void __direct_put_cache__(@NotNull Object key, @NotNull Bean value, int state) {
		}

		@Override
		public @NotNull Zeze.Serialize.ByteBuffer encodeKey(@NotNull Object key) {
			throw new UnsupportedOperationException();
		}

		@Override
		public @NotNull Object decodeKey(@NotNull Zeze.Serialize.ByteBuffer bb) {
			throw new UnsupportedOperationException();
		}

		@Override
		public @NotNull Class<? extends Comparable<?>> getKeyClass() {
			throw new UnsupportedOperationException();
		}

		@Override
		public @NotNull Class<? extends Bean> getValueClass() {
			throw new UnsupportedOperationException();
		}

		@Override
		public @NotNull Zeze.History.ApplyTable<?, ?> createApplyTable(@NotNull Zeze.History.IApplyDatabase applyDb) {
			throw new UnsupportedOperationException();
		}
	}

	@BeforeEach
	public void setUp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(12823);
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseType(Config.DbType.Memory);
		dbConf.setDatabaseUrl("z1f3_memory");
		conf.getDatabaseConfMap().put("", dbConf);
		app = new Application("TestZ1F3AddTableAtomicity", conf);
		app.start();
	}

	@AfterEach
	public void tearDown() throws Exception {
		if (app.getStartState() != Application.StartState.eStopped)
			app.stop();
	}

	/** 表名冲突：抛异常后不得留下半注册幻影表（tables无新id条目、TableKey未被改写）。 */
	@Test
	public void testDuplicateNameLeavesNoPhantomEntry() {
		var exist = new StubTable(201, "Z1F3Exist");
		app.addTable("", exist);

		// 新id + 既有表名：必须在两道唯一性校验处失败。
		var phantom = new StubTable(202, "Z1F3Exist");
		var ex = assertThrows(IllegalStateException.class, () -> app.addTable("", phantom));
		assertTrue(ex.getMessage().contains("duplicate table name"), "必须因表名冲突失败: " + ex.getMessage());

		// 修复前：tables中残留id=202的幻影表（从未open），与tableNameMap视图分叉。
		assertNull(app.getTable(202), "表名冲突失败后不得留下半注册幻影表（tables原子回退）");
		assertSame(exist, app.getTable("Z1F3Exist"), "既有表的name视图不变");
		assertSame(exist, app.getTable(201), "既有表的id视图不变");
		assertNull(TableKey.tables.get(202), "TableKey不得登记幻影表的id");
	}

	/** 重复id：失败后既有表的id→name映射不得被污染（TableKey.tables先写后查的老顺序）。 */
	@Test
	public void testDuplicateIdDoesNotPolluteExistingMapping() {
		var exist = new StubTable(301, "Z1F3First");
		app.addTable("", exist);

		var second = new StubTable(301, "Z1F3Second"); // 重复id + 新名字
		var ex = assertThrows(IllegalStateException.class, () -> app.addTable("", second));
		assertTrue(ex.getMessage().contains("duplicate table id"), "必须因重复id失败: " + ex.getMessage());

		// 修复前：TableKey.tables.put(301,"Z1F3Second")先执行，既有表的展示映射被污染。
		Assertions.assertEquals("Z1F3First", TableKey.tables.get(301),
				"重复id失败不得污染既有表的id→name映射");
		assertSame(exist, app.getTable("Z1F3First"), "既有表仍在");
		assertNull(app.getTable("Z1F3Second"), "失败表不得登记");
	}

	/** 护栏：全新id+名字正常登记（openDynamicTable运行期路径不回归）。 */
	@Test
	public void testNormalAddStillWorks() {
		var ok = new StubTable(401, "Z1F3Ok");
		var db = app.addTable("", ok);
		assertSame(ok, app.getTable(401));
		assertSame(ok, app.getTable("Z1F3Ok"));
		Assertions.assertEquals("Z1F3Ok", TableKey.tables.get(401));
		Assertions.assertSame(app.getDatabase(""), db);
	}
}
