package Zeze.Transaction;

import static java.util.Objects.requireNonNull;

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
 * GTable 的陈旧行视图与托管值复用约束。
 * 陈旧 Row 写入必须失败；重复挂接已托管 Bean 必须中止整笔事务，不得留下持久化记录。
 * 2026-10-20起（277e6e501）语义升级：HasManagedException 经 Transaction.throwAbort
 * 中止整个事务（GoBackZeze cause，业务catch-and-continue不再可能）——本用例断言对齐
 * Abort 契约：返回 AbortException(-8)，整体回滚后行2无幻影、行1亦不残留（比旧契约更强）。
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

	/** T-27：内层put对已受管bean必须Abort整个事务（277e6e501起throwAbort语义），
	 * 全回滚后行2无幻影空行、行1亦不残留——无幻影保证比旧catch-and-continue契约更强。 */
	@Test
	public void testManagedValueReuseAbortsWithoutPersistingRows() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(NextId.incrementAndGet());
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("gtable_stale_phantom_" + conf.getServerId());
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		var app = new Application("TestT27PhantomRow" + conf.getServerId(), conf);
		app.setSchemas(new demo.Schemas());
		app.addTable(requireNonNull(conf.getTableConf("demo_ModuleGTable_tGTable2")).getDatabaseName(), new tGTable2());
		app.start();
		var table = (tGTable2)app.getTable("demo_ModuleGTable_tGTable2");
		try {
			// 单事务：建行1后同一bean再入行2——HasManagedException经throwAbort(GoBackZeze)整体中止
			var result = app.newProcedure(() -> {
				var g = requireNonNull(table).getOrAdd(1L).getGTable();
				var shared = new Bean1();
				g.put(1, 1, shared); // shared进入行1（受管）
				g.put(2, 1, shared); // 同一bean再入新行2：必须中止事务（不可catch-and-continue）
				Assertions.fail("已受管bean再入容器必须中止事务");
				return Procedure.Success;
			}, "T27.PhantomRow").call();
			Assertions.assertEquals(Procedure.AbortException, result, "throwAbort语义：返回AbortException(-8)");

			// 验证事务本身不得创建记录（getOrAdd会建空记录落库，污染"无残留"判定）：
			// 非创建get断言内存无记录，checkpointRun后断言数据库亦无——中止的事务不落库。
			Assertions.assertEquals(Procedure.Success, app.newProcedure(() -> {
				Assertions.assertNull(requireNonNull(table).get(1L), "整体Abort后记录1不得存在于内存");
				return Procedure.Success;
			}, "GTable.VerifyAbort").call());
			app.checkpointRun();
			Assertions.assertNull(requireNonNull(table).selectFromDatabase(1L), "被中止的新记录不得落库");
		} finally {
			app.stop();
		}
	}
}
