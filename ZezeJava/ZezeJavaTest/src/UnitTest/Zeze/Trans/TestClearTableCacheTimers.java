package UnitTest.Zeze.Trans;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

import Zeze.Application;
import Zeze.Config;
import Zeze.Transaction.TableCache;
import Zeze.Util.TaskSpec;
import demo.Module1.Table3;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * CARRY-TABLECACHE-CLEARTABLE 回归：TableX.__ClearTableCacheUnsafe__ 重建
 * cache 时必须关闭旧 TableCache 的周期定时器。
 * 修复前：直接 new TableCache 覆盖，旧 cache 的 newLruHot/cleanNow 两个
 * 永不自取消的周期任务继续调度（定时任务闭包强引用旧 cache 的 dataMap，
 * 整表缓存无法GC），Queue/LinkedMap/Timer/DepartmentTree/Online 的
 * clearTableCache 每调一次泄漏 2 个调度任务。与 FND-T2-4（disable/open
 * 同款泄漏，26bd6b139）同模式。
 * 表用demo的持久表Table3：Application构造会自动注册Builtin组件表，addTable
 * 同名表会报duplicate table id。
 */
@Fast
public class TestClearTableCacheTimers {
	// 独立serverId隔离本地zeze_cache目录与其他@Fast测试（T2组：300/400/500/600起；本类700起）。
	private static final AtomicInteger nextServerId = new AtomicInteger(700);

	private static Application newApp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(nextServerId.getAndIncrement());
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("l4_cleartablecache_test_" + conf.getServerId());
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		return new Application("TestClearTableCacheTimers@" + conf.getServerId(), conf);
	}

	private static Object getField(Object obj, String name) throws ReflectiveOperationException {
		for (Class<?> c = obj.getClass(); c != null; c = c.getSuperclass()) {
			try {
				Field f = c.getDeclaredField(name);
				f.setAccessible(true);
				return f.get(obj);
			} catch (NoSuchFieldException ignored) {
			}
		}
		throw new NoSuchFieldException(name);
	}

	/** close() 会把两个定时器句柄置null；非null表示定时器仍在调度。 */
	private static boolean timersClosed(TableCache<?, ?> cache) throws ReflectiveOperationException {
		return getField(cache, "timerClean") == null && getField(cache, "timerNewHot") == null;
	}

	@Test
	public void testClearTableCacheClosesOldTimers() throws Exception {
		var app = newApp();
		var table = new Table3();
		app.addTable("", table);
		app.start();
		try {
			var oldCache = table.getCache();
			Assertions.assertNotNull(oldCache);
			Assertions.assertFalse(timersClosed(oldCache)); // 打开状态：定时器在调度

			// 真实语义入口：Queue/LinkedMap/Timer/DepartmentTree/Online 的
			// clearTableCache 最终都是调它（测试无事务、非checkpoint后，满足安全前提）。
			table.__ClearTableCacheUnsafe__();

			Assertions.assertTrue(timersClosed(oldCache), "__ClearTableCacheUnsafe__必须取消旧cache的周期定时器");
			var newCache = table.getCache();
			Assertions.assertNotNull(newCache);
			Assertions.assertNotSame(oldCache, newCache);
			Assertions.assertFalse(timersClosed(newCache)); // 新cache的定时器在调度

			// 重建cache后表仍可正常读写。
			var key = 31L;
			var rc = TaskSpec.ofProcedure(app.newProcedure(() -> {
				table.getOrAdd(key).setInt_1(43);
				return 0L;
			}, "TestClearTableCacheTimers.put")).call();
			Assertions.assertEquals(0L, rc);
			app.checkpointRun();
			Assertions.assertEquals(43, table.selectFromDatabase(key).getInt_1());
		} finally {
			app.stop();
		}
	}

	@Test
	public void testClearTableCacheRepeatedNoLeak() throws Exception {
		var app = newApp();
		var table = new Table3();
		app.addTable("", table);
		app.start();
		try {
			// 连续重建：每一代旧cache的定时器都必须被关闭，不随调用次数累积。
			TableCache<?, ?> prev = table.getCache();
			for (int i = 0; i < 5; i++) {
				table.__ClearTableCacheUnsafe__();
				Assertions.assertTrue(timersClosed(prev), "第" + i + "次重建后旧cache定时器必须全部取消");
				prev = table.getCache();
			}
		} finally {
			app.stop();
		}
	}
}
