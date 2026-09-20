package UnitTest.Zeze.Transaction;

import java.util.List;
import java.util.Set;

import Zeze.Transaction.Collections.PSet1;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND7-08：PSet1/LogSet1的addAll/removeAll用批量plusAll/minusAll的same-instance
 * 判定"是否变化"，但pcollections的MapPSet批量操作恒新建包装——无变化也恒返回true、
 * 恒把c全量记入added/removed日志，违反Collection契约"发生了变化才返回true"。
 * 修复：逐项plus/minus判定（单项无变化返回同一实例），无变化时不记账不返回true。
 */
@Fast
public class TestFnd708SetBulkChangeReturn {

	@Test
	public void testUnmanagedAddAllNoChangeReturnsFalse() {
		var set = new PSet1<Integer>(Integer.class);
		Assertions.assertTrue(set.addAll(List.of(1, 2, 3)));
		Assertions.assertFalse(set.addAll(List.of(1, 2, 3)), "全已存在必须返回false（JDK契约）");
		Assertions.assertFalse(set.addAll(List.of(2)), "部分已存在（子集）同样无变化");
		Assertions.assertEquals(Set.of(1, 2, 3), set.getSet());

		Assertions.assertTrue(set.addAll(List.of(3, 4)), "含新元素返回true");
		Assertions.assertEquals(Set.of(1, 2, 3, 4), set.getSet());
	}

	@Test
	public void testUnmanagedRemoveAllNoChangeReturnsFalse() {
		var set = new PSet1<Integer>(Integer.class);
		set.addAll(List.of(1, 2, 3));
		Assertions.assertFalse(set.removeAll(List.of(7, 8)), "无交集必须返回false（JDK契约）");
		Assertions.assertEquals(Set.of(1, 2, 3), set.getSet());

		Assertions.assertTrue(set.removeAll(List.of(3, 9)), "含命中元素返回true");
		Assertions.assertEquals(Set.of(1, 2), set.getSet());
	}

	@Test
	public void testManagedAddAllNoChangeReturnsFalse() throws Exception {
		// 托管路径（LogSet1）：无变化时返回false且added/removed日志不膨胀。
		var config = new Zeze.Config();
		config.setServerId(7080); // 缓存目录zeze_cache_<serverId>按serverId命名：默认0会与同JVM其他默认App互撞（start先删后开，LOCK被持即删失败）
		config.setServiceManager("disable");
		config.setDefaultTableConf(new Zeze.Config.TableConf());
		var dbConf = new Zeze.Config.DatabaseConf();
		dbConf.setDatabaseType(Zeze.Config.DbType.RocksDb);
		var dbDir = java.nio.file.Files.createTempDirectory("fnd708a");
		dbConf.setDatabaseUrl(dbDir.toString());
		config.getDatabaseConfMap().put("", dbConf);
		var app = new Zeze.Application("TestFnd708a", config);
		var table = new demo.Module1.tflush();
		app.addTable("", table);
		app.start();
		try {
			var result = app.newProcedure(() -> {
				var v = table.getOrAdd(7081L);
				var set = v.getSet10();
				set.clear();
				Assertions.assertTrue(set.addAll(List.of(1, 2, 3)));
				Assertions.assertFalse(set.addAll(List.of(1, 2, 3)), "托管路径全已存在必须返回false");
				Assertions.assertFalse(set.removeAll(List.of(7, 8)), "托管路径无交集必须返回false");
				Assertions.assertEquals(Set.of(1, 2, 3), set.getSet());
				// 有变化路径的返回值与最终内容不受影响
				Assertions.assertTrue(set.addAll(List.of(3, 4)));
				Assertions.assertTrue(set.removeAll(List.of(1, 9)));
				Assertions.assertEquals(Set.of(2, 3, 4), set.getSet());
				return 0L;
			}, "TestFnd708Managed").call();
			Assertions.assertEquals(Zeze.Transaction.Procedure.Success, result, "事务必须成功");
		} finally {
			app.stop();
			Zeze.Raft.LogSequence.deleteDirectory(dbDir.toFile()); // 2026-09-20审核：临时rocksdb目录不删
		}
	}
}
