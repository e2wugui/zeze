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

	/** 重复挂接已托管值会 Abort，先前写入的行也必须一并回滚。 */
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
			var result = app.newProcedure(() -> {
				var g = requireNonNull(table).getOrAdd(1L).getGTable();
				var shared = new Bean1();
				g.put(1, 1, shared); // shared进入行1（受管）
				g.put(2, 1, shared); // 重复挂接由事务框架转换为 Abort，不能按成功提交。
				Assertions.fail("复用已托管 Bean 必须中止事务");
				return Procedure.Success;
			}, "T27.PhantomRow").call();
			Assertions.assertEquals(Procedure.AbortException, result);

			Assertions.assertEquals(Procedure.Success, app.newProcedure(() -> {
				Assertions.assertNull(requireNonNull(table).get(1L), "新记录及两行都必须回滚");
				return Procedure.Success;
			}, "GTable.VerifyAbort").call());
			app.checkpointRun();
			Assertions.assertNull(requireNonNull(table).selectFromDatabase(1L), "被中止的新记录不得落库");
		} finally {
			app.stop();
		}
	}
}
