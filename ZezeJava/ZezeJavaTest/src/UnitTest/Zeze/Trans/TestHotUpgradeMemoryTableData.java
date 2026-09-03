package UnitTest.Zeze.Trans;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import Zeze.Application;
import Zeze.Config;
import Zeze.Services.GlobalCacheManagerConst;
import Zeze.Transaction.Record1;
import Zeze.Util.TaskSpec;
import demo.Bean1;
import demo.Module1.tMemorySize;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND-T2-5 回归：内存表热更迁移 __direct_put_cache__ 的落盘语义。
 * 迁移记录不在任何rrs中，置脏后checkpoint永远flush不到它：数据永不落本地Rocks
 * （受限容量walkMemory为空），且cleanNow因脏记录清不掉而无限循环。
 * 迁移必须直接写入本地Rocks镜像且不置脏。
 */
@Fast
public class TestHotUpgradeMemoryTableData {
	// 独立serverId隔离本地zeze_cache目录与其他@Fast测试（T2组：300/400/500/600起）。
	private static final AtomicInteger nextServerId = new AtomicInteger(700);

	private static Application newApp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(nextServerId.getAndIncrement());
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("t2_memupgrade_test_" + conf.getServerId());
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		return new Application("TestHotUpgradeMemoryTableData@" + conf.getServerId(), conf);
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

	private static Object invokeOn(Object obj, String name, Object... args) throws ReflectiveOperationException {
		for (Class<?> c = obj.getClass(); c != null; c = c.getSuperclass()) {
			for (Method m : c.getDeclaredMethods()) {
				if (m.getName().equals(name) && m.getParameterCount() == args.length) {
					m.setAccessible(true);
					return m.invoke(obj, args);
				}
			}
		}
		throw new NoSuchMethodException(name);
	}

	@SuppressWarnings("unchecked")
	private static Record1<Long, Bean1> getRecord(tMemorySize table, long key) throws ReflectiveOperationException {
		return (Record1<Long, Bean1>)invokeOn(table.getCache(), "get", key);
	}

	@Test
	public void testDirectPutCacheWritesLocalRocks() throws Exception {
		var app = newApp();
		var table = new tMemorySize();
		app.addTable("", table);
		app.start();
		try {
			var key = 7777L;
			var value = new Bean1();
			value.setV1(321);
			table.__direct_put_cache__(key, value, GlobalCacheManagerConst.StateModify);

			// 1. 核心：本地Rocks镜像有数据（受限容量walkMemory的数据源、软引用回收后的恢复源）
			var fromLocalRocks = table.getLocalRocksCacheTable().find(table, key);
			Assertions.assertNotNull(fromLocalRocks, "__direct_put_cache__必须写入本地Rocks镜像");
			Assertions.assertEquals(321, fromLocalRocks.getV1());

			// 2. 记录不置脏（不在rrs中的脏记录cleanNow永远清不掉：无限循环）
			var r = getRecord(table, key);
			Assertions.assertFalse((Boolean)invokeOn(r, "getDirty"), "迁移记录不得置脏");

			// 3. 受限容量内存表的walkMemory（走localRocks）可见
			Assertions.assertTrue(table.getTableConf().getRealCacheCapacity() >= 0); // sanity:默认受限
			var found = new boolean[] {false};
			table.walkMemory((k, v) -> {
				if (k != null && k == key && v.getV1() == 321)
					found[0] = true;
				return true;
			});
			Assertions.assertTrue(found[0], "受限容量walkMemory必须能遍历到迁移数据");

			// 4. cleanNow能清出dataMap（修复前dirty记录卡住，块永不为空循环清理），
			//    且清出后事务重新装载仍能得到数据（localRocks恢复源生效）。
			var key2 = 7778L;
			var value2 = new Bean1();
			value2.setV1(654);
			table.__direct_put_cache__(key2, value2, GlobalCacheManagerConst.StateModify);
			var dataMap = (ConcurrentHashMap<Long, Record1<Long, Bean1>>)getField(table.getCache(), "dataMap");
			Assertions.assertEquals(2, dataMap.size());

			table.getTableConf().setCacheCapacity(1);
			table.getTableConf().setCacheFactor(1); // realCacheCapacity=1 < 2 触发清理
			invokeOn(table.getCache(), "newLruHot"); // 当前记录所在块退出热块
			table.getCache().cleanNow();

			Assertions.assertEquals(0, dataMap.size(), "非脏的迁移记录必须能被cleanNow清理");

			var got = new int[] {-1};
			var rc = TaskSpec.ofProcedure(app.newProcedure(() -> {
				var v = table.get(key);
				got[0] = v != null ? v.getV1() : -1;
				return 0L;
			}, "TestHotUpgradeMemoryTableData.reload")).call();
			Assertions.assertEquals(0L, rc);
			Assertions.assertEquals(321, got[0], "清出缓存后必须能从本地Rocks重新装载");
		} finally {
			app.stop();
		}
	}
}
