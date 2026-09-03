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
 * FND-T2-4 回归：热更替换/禁用表时必须关闭旧 TableCache 的周期定时器。
 * TableCache 构造注册 newLruHot/cleanNow 两个永不自取消的周期任务；
 * 旧表 disable/open 替换不关闭它们时，定时任务闭包强引用旧 cache 的 dataMap，
 * 整表缓存无法GC，且旧 cleanNow 持续对旧记录执行无效操作，每次热更泄漏累积。
 * 表用demo的持久表Table3：Application构造会自动注册Builtin组件表
 * （tQueues等），addTable同名表会报duplicate table id。
 */
@Fast
public class TestHotReplaceTableCacheClose {
	// 独立serverId隔离本地zeze_cache目录与其他@Fast测试（T2组：300/400/500起）。
	private static final AtomicInteger nextServerId = new AtomicInteger(600);

	private static Application newApp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(nextServerId.getAndIncrement());
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("t2_hotreplace_test_" + conf.getServerId());
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		return new Application("TestHotReplaceTableCacheClose@" + conf.getServerId(), conf);
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
	public void testDisableClosesCacheTimers() throws Exception {
		var app = newApp();
		var table = new Table3();
		app.addTable("", table);
		app.start();
		try {
			var cache = table.getCache();
			Assertions.assertNotNull(cache);
			Assertions.assertFalse(timersClosed(cache)); // 打开状态：定时器在调度

			table.disable();

			Assertions.assertTrue(timersClosed(cache), "disable必须取消旧cache的周期定时器");
			Assertions.assertNull(table.getCache());
		} finally {
			app.stop();
		}
	}

	@Test
	public void testReplaceTableClosesOldCacheTimers() throws Exception {
		var app = newApp();
		var oldTable = new Table3();
		app.addTable("", oldTable);
		app.start();
		try {
			var oldCache = oldTable.getCache();
			Assertions.assertFalse(timersClosed(oldCache));
			Assertions.assertSame(oldTable, app.getTable(oldTable.getId()));

			// 模拟热更替换（Application.replaceTable -> 新表open(exist) -> 旧表disable）
			var newTable = new Table3();
			app.replaceTable("", newTable);

			Assertions.assertTrue(timersClosed(oldCache), "热更替换后旧cache的周期定时器必须被取消");
			Assertions.assertSame(newTable, app.getTable(newTable.getId()));

			// 新表正常工作
			var key = 31L;
			var rc = TaskSpec.ofProcedure(app.newProcedure(() -> {
				newTable.getOrAdd(key).setInt_1(42);
				return 0L;
			}, "TestHotReplaceTableCacheClose.put")).call();
			Assertions.assertEquals(0L, rc);
			app.checkpointRun();
			Assertions.assertEquals(42, newTable.selectFromDatabase(key).getInt_1());
		} finally {
			app.stop();
		}
	}
}
