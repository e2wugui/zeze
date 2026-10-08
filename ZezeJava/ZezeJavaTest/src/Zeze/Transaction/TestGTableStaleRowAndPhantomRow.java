package Zeze.Transaction;

import java.util.concurrent.atomic.AtomicInteger;

import Zeze.Application;
import Zeze.Config;
import Zeze.Transaction.GTable.GTable1;
import demo.ModuleGTable.Bean1;
import demo.ModuleGTable.tGTable2;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * kimi-audit01 T-26/T-27回归（GTable）。
 * T-26：Row视图向已从表中摘除的行写数据——rowMap().remove(r)/clear()整体摘行后，Row缓存的
 * backingRowMap仍非空，Row.put直接写已脱离backingMap的旧行（托管路径还记挂在脱树bean上的
 * 幻影redo），写入静默丢失（Guava原版同构缺陷，Zeze下丢的是持久化数据）。
 * 修复：陈旧视图守卫（backingMap不含该行即抛IllegalStateException，响亮失败）。
 * T-27：put先建行后写值——getOrCreate先把空行put进外层容器（托管路径当场记LogMap2）后
 * 内层put抛HasManagedException（值bean已受管），catch后继续提交会残留并持久化幻影空行。
 * 修复：先写内层再挂外层（对齐PMap2.putAll先全量校验后入日志惯例）。
 */
@Fast
public class TestGTableStaleRowAndPhantomRow {
	private static final AtomicInteger NextId = new AtomicInteger(FastServerIds.TEST_GTABLE_STALE_PHANTOM_ROW);

	/** T-26：行被整体摘除后，陈旧Row视图put必须响亮失败而非静默丢失。 */
	@Test
	public void testStaleRowViewPutThrows() {
		var t = new GTable1<Long, Long, Integer>(Long.class, Long.class, Integer.class);
		t.put(1L, 1L, 11);
		var row = t.row(1L);
		// get()经updateBackingRowMapField填充视图缓存——这是陈旧缓存形成的前提
		Assertions.assertEquals(11, row.get(1L));
		Assertions.assertEquals(11, row.put(1L, 12)); // 正常路径：原值11（缓存非空直写）

		t.clear(); // 行整体摘除，row视图缓存的backingRowMap仍非空且指向已脱离的旧行
		Assertions.assertThrows(IllegalStateException.class, () -> row.put(2L, 22),
				"陈旧行视图put必须响亮失败（修复前静默写入丢失的旧行）");

		// rowMap().remove同构
		t.put(2L, 1L, 21);
		var row2 = t.row(2L);
		Assertions.assertEquals(21, row2.get(1L)); // 填充缓存
		t.rowMap().remove(2L);
		Assertions.assertThrows(IllegalStateException.class, () -> row2.put(2L, 22));
	}

	/** T-27：内层put抛异常（值bean已受管）时不得在外层残留幻影空行并持久化。 */
	@Test
	public void testInnerPutFailLeavesNoPhantomRow() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(NextId.incrementAndGet());
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("gtable_stale_phantom_" + conf.getServerId());
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		var app = new Application("TestT27PhantomRow" + conf.getServerId(), conf);
		app.setSchemas(new demo.Schemas());
		app.addTable(conf.getTableConf("demo_ModuleGTable_tGTable2").getDatabaseName(), new tGTable2());
		app.start();
		var table = (tGTable2)app.getTable("demo_ModuleGTable_tGTable2");
		try {
			var result = app.newProcedure(() -> {
				var g = table.getOrAdd(1L).getGTable();
				var shared = new Bean1();
				g.put(1, 1, shared); // shared进入行1（受管）
				try {
					g.put(2, 1, shared); // 同一bean再入新行2：内层put必须抛HasManagedException
					Assertions.fail("已受管bean再入容器必须抛HasManagedException");
				} catch (Zeze.Transaction.HasManagedException expected) {
					// 业务catch后继续：外层不得残留行2的幻影空行
				}
				return Procedure.Success;
			}, "T27.PhantomRow").call();
			Assertions.assertEquals(Procedure.Success, result);

			Assertions.assertEquals(Procedure.Success, app.newProcedure(() -> {
				var g = table.getOrAdd(1L).getGTable();
				// 核心（红）：异常行2不得作为空行残留并提交持久化
				Assertions.assertFalse(g.containsRow(2), "内层失败的行不得以幻影空行残留");
				Assertions.assertTrue(g.containsRow(1));
				return Procedure.Success;
			}, "T27.Verify").call());
		} finally {
			app.stop();
		}
	}
}
