package UnitTest.Zeze.Trans;

import java.util.concurrent.atomic.AtomicInteger;

import Zeze.Application;
import Zeze.Config;
import Zeze.Services.GlobalCacheManagerConst;
import Zeze.Util.TaskSpec;
import demo.Bean1;
import demo.Module1.tMemorySize;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND7-01 回归：内存表热更搬运 __direct_put_cache__ 的 sizeCounter 记账。
 * 热更为新表建全新 TableCache（sizeCounter 归零）后经 __direct_put_cache__ 直写搬运，
 * 不补计数则升级后 getCacheSize() 恒 0；且搬运记录 softValue 非null，后续事务删除时
 * Record1.commit 按 strongRef!=null 走 decrement，计数变负。
 */
@Fast
public class TestFnd701MemoryTableSize {
	// 独立serverId隔离本地zeze_cache目录与其他@Fast测试（T1组：100起）。
	private static final AtomicInteger nextServerId = new AtomicInteger(100);

	private static Application newApp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(nextServerId.getAndIncrement());
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("t1_fnd701_memsize_" + conf.getServerId());
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		return new Application("TestFnd701MemoryTableSize@" + conf.getServerId(), conf);
	}

	private static Bean1 newBean(int v) {
		var b = new Bean1();
		b.setV1(v);
		return b;
	}

	@Test
	public void testDirectPutCacheCountsSize() throws Exception {
		var app = newApp();
		var table = new tMemorySize();
		app.addTable("", table);
		app.start();
		try {
			Assertions.assertEquals(0, table.getCacheSize());

			// 模拟热更搬运：两条直写。修复前 size 恒 0。
			table.__direct_put_cache__(1L, newBean(11), GlobalCacheManagerConst.StateModify);
			table.__direct_put_cache__(2L, newBean(22), GlobalCacheManagerConst.StateModify);
			Assertions.assertEquals(2, table.getCacheSize(), "热更搬运后size必须=搬运条数");

			// 同key重复直写（重复升级/回滚场景）不得重复计数。
			table.__direct_put_cache__(2L, newBean(222), GlobalCacheManagerConst.StateModify);
			Assertions.assertEquals(2, table.getCacheSize(), "同key重复直写不得重复计数");

			// 升级后事务删除搬运记录：修复前 0-1=-1，必须正常回到1。
			Assertions.assertEquals(0L, TaskSpec.ofProcedure(app.newProcedure(() -> {
				table.remove(1L);
				return 0L;
			}, "TestFnd701MemoryTableSize.remove")).call());
			Assertions.assertEquals(1, table.getCacheSize(), "删除搬运记录后不得变负");

			// 升级后事务put已有搬运值的key：strongRef非null，commit不得重复increment。
			Assertions.assertEquals(0L, TaskSpec.ofProcedure(app.newProcedure(() -> {
				table.put(2L, newBean(2222));
				return 0L;
			}, "TestFnd701MemoryTableSize.put")).call());
			Assertions.assertEquals(1, table.getCacheSize(), "事务覆盖搬运记录不得重复计数");

			// 新key走事务put：正常 +1。
			Assertions.assertEquals(0L, TaskSpec.ofProcedure(app.newProcedure(() -> {
				table.put(3L, newBean(33));
				return 0L;
			}, "TestFnd701MemoryTableSize.insert")).call());
			Assertions.assertEquals(2, table.getCacheSize());
		} finally {
			app.stop();
		}
	}
}
