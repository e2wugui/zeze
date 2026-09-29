package Zeze.Transaction;

import java.util.concurrent.atomic.AtomicInteger;

import Zeze.Application;
import Zeze.Config;
import demo.Module1.BSimple;
import demo.Module1.tflush;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * kimi-audit01 T-24回归：2系容器copy()浅拷贝共享受管活Bean，经ReadOnly.copy()成为
 * 可变后门。PMap2.copy()仅复制容器引用，值Bean是原记录树内同一批受管实例：
 * copy().get(k).setField(...)改的是原记录（事务内完全绕过只读约定）。
 * 修复：copy()深拷贝值Bean（对齐CollOne.copy()/生成代码assign的逐元素copy语义）。
 * 测试：副本元素必须是独立实例，修改副本不得影响原记录（修复前：同一实例，改写直达原记录）。
 */
@Fast
public class TestPMap2CopyDeep {
	private static final AtomicInteger NextId = new AtomicInteger(FastServerIds.TEST_PMAP2_COPY_DEEP);

	@Test
	public void testReadOnlyCopyElementsAreIndependent() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(NextId.incrementAndGet());
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("t24_copy_deep_" + conf.getServerId());
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		var app = new Application("TestT24CopyDeep" + conf.getServerId(), conf);
		app.setSchemas(new demo.Schemas());
		app.addTable(conf.getTableConf("demo_Module1_tflush").getDatabaseName(), new tflush());
		app.start();
		var table = (tflush)app.getTable("demo_Module1_tflush");
		try {
			Assertions.assertEquals(Procedure.Success, app.newProcedure(() -> {
				var bs = new BSimple();
				bs.setInt_1(5);
				table.getOrAdd(1L).getMap41().put(1L, bs);
				return Procedure.Success;
			}, "T24.Prepare").call());

			Assertions.assertEquals(Procedure.Success, app.newProcedure(() -> {
				var v = table.getOrAdd(1L);
				var copy = v.getMap41ReadOnly().copy();
				var copied = (BSimple)copy.get(1L);
				// 核心（红）：副本元素不得是原记录内的受管活Bean（同一实例即后门）
				Assertions.assertNotSame(v.getMap41().get(1L), copied, "copy()必须深拷贝值Bean");
				// 修改副本不得影响原记录（红：同一实例，setInt_1经受管路径直达原记录）
				copied.setInt_1(99);
				Assertions.assertEquals(5, v.getMap41().get(1L).getInt_1(), "修改副本不得改写原记录");
				return Procedure.Success;
			}, "T24.CopyAndModify").call());

			Assertions.assertEquals(Procedure.Success, app.newProcedure(() -> {
				Assertions.assertEquals(5, table.getOrAdd(1L).getMap41().get(1L).getInt_1(),
						"提交后原记录必须保持原值（红：副本修改经共享实例持久化）");
				return Procedure.Success;
			}, "T24.Verify").call());
		} finally {
			app.stop();
		}
	}
}
