package Zeze.Transaction;

import org.jspecify.annotations.NullMarked;

import harness.FastServerIds;
import java.util.concurrent.atomic.AtomicInteger;

import Zeze.Application;
import Zeze.Config;
import Zeze.Util.TaskSpec;
import demo.Bean1;
import demo.Module1.tMemorySize;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 内存表 getCacheSize() 记账契约：插入 +1、覆盖不变、删除 -1（不得变负）。
 */
@Fast
@NullMarked
public class TestMemoryTableSize {
	// 独立serverId隔离本地zeze_cache目录与其他@Fast测试（T1组：100起）。
	private static final AtomicInteger nextServerId = new AtomicInteger(FastServerIds.TEST_MEMORY_TABLE_SIZE);

	private static Application newApp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(nextServerId.getAndIncrement());
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("memsize_" + conf.getServerId());
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		return new Application("TestMemoryTableSize@" + conf.getServerId(), conf);
	}

	private static Bean1 newBean(int v) {
		var b = new Bean1();
		b.setV1(v);
		return b;
	}

	@Test
	public void testTransactionalPutOverwriteAndRemoveCountSize() throws Exception {
		var app = newApp();
		var table = new tMemorySize();
		app.addTable("", table);
		app.start();
		try {
			Assertions.assertEquals(0, table.getCacheSize());

			Assertions.assertEquals(0L, TaskSpec.ofProcedure(app.newProcedure(() -> {
				table.put(1L, newBean(11));
				table.put(2L, newBean(22));
				return 0L;
			}, "TestMemoryTableSize.seed")).call());
			Assertions.assertEquals(2, table.getCacheSize());

			// 覆盖已有 key 不得重复递增。
			Assertions.assertEquals(0L, TaskSpec.ofProcedure(app.newProcedure(() -> {
				table.put(2L, newBean(222));
				return 0L;
			}, "TestMemoryTableSize.overwrite")).call());
			Assertions.assertEquals(2, table.getCacheSize());

			// 删除已有 key 正常递减，删除不存在 key 不得变负。
			Assertions.assertEquals(0L, TaskSpec.ofProcedure(app.newProcedure(() -> {
				table.remove(1L);
				table.remove(999L);
				return 0L;
			}, "TestMemoryTableSize.remove")).call());
			Assertions.assertEquals(1, table.getCacheSize());

			// 插入新 key 正常 +1。
			Assertions.assertEquals(0L, TaskSpec.ofProcedure(app.newProcedure(() -> {
				table.put(3L, newBean(33));
				return 0L;
			}, "TestMemoryTableSize.insert")).call());
			Assertions.assertEquals(2, table.getCacheSize());
		} finally {
			app.stop();
		}
	}
}
