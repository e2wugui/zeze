package UnitTest.Zeze.Trans;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import Zeze.Application;
import Zeze.Config;
import Zeze.Transaction.Record1;
import Zeze.Util.TaskSpec;
import demo.Bean1;
import demo.Module1.tMemorySize;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND-T2-6 回归：内存表热更回滚不得清除本地Rocks镜像。
 * 回滚重新打开旧表时保留旧cache（dataMap），但open(exist,app)无条件
 * clear() localRocksCacheTable：软引用被GC回收后loadValue的恢复源为空，
 * 记录对get/getOrAdd永久丢失；受限容量walkMemory（走localRocks）也为空。
 * 同时本地Rocks是升级迁移（walkMemoryAny）的读取源，升级方向的open同样不得清除。
 */
@Fast
public class TestHotRollbackMemoryTable {
	// 独立serverId隔离本地zeze_cache目录与其他@Fast测试（T2组：300/400/500/600/700起）。
	private static final AtomicInteger nextServerId = new AtomicInteger(800);

	private static Application newApp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(nextServerId.getAndIncrement());
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("t2_memrollback_test_" + conf.getServerId());
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		return new Application("TestHotRollbackMemoryTable@" + conf.getServerId(), conf);
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

	private static void put(Application app, tMemorySize table, long key, int v1) {
		var rc = TaskSpec.ofProcedure(app.newProcedure(() -> {
			var v = new Bean1();
			v.setV1(v1);
			table.put(key, v);
			return 0L;
		}, "TestHotRollbackMemoryTable.put@" + key)).call();
		Assertions.assertEquals(0L, rc);
	}

	private static int getV1(Application app, tMemorySize table, long key) {
		var got = new int[] {-1};
		var rc = TaskSpec.ofProcedure(app.newProcedure(() -> {
			var v = table.get(key);
			got[0] = v != null ? v.getV1() : -1;
			return 0L;
		}, "TestHotRollbackMemoryTable.get@" + key)).call();
		Assertions.assertEquals(0L, rc);
		return got[0];
	}

	@Test
	public void testRollbackKeepsLocalRocks() throws Exception {
		var app = newApp();
		var oldTable = new tMemorySize();
		app.addTable("", oldTable);
		app.start();
		try {
			var key1 = 11L;
			var key2 = 12L;
			put(app, oldTable, key1, 111);
			put(app, oldTable, key2, 222);
			app.checkpointRun(); // 脏数据flush进本地Rocks（内存表唯一持久副本）
			Assertions.assertEquals(111, getV1(app, oldTable, key1)); // sanity

			// 模拟热更安装：新表open(exist)继承（修复前这里clear了本地Rocks）
			var newTable = new tMemorySize();
			app.replaceTable("", newTable);

			// 升级迁移的读取源必须保留：旧表walkMemoryAny（受限容量走localRocks）仍可见
			var migrated = new int[] {0};
			oldTable.walkMemoryAny((k, v) -> {
				migrated[0]++;
				return true;
			});
			Assertions.assertTrue(migrated[0] >= 2, "升级迁移的读取源（本地Rocks）不得被清除");

			// 模拟热更失败回滚：旧表重新注册（保留旧cache重新open）
			app.replaceTable("", oldTable);
			Assertions.assertSame(oldTable, app.getTable(oldTable.getId()));

			// 核心1：本地Rocks镜像数据完好（软引用回收后的唯一恢复源）
			var fromLocalRocks = oldTable.getLocalRocksCacheTable().find(oldTable, key1);
			Assertions.assertNotNull(fromLocalRocks, "回滚后本地Rocks不得为空");
			Assertions.assertEquals(111, fromLocalRocks.getV1());

			// 核心2：模拟软引用被GC回收（setSoftValue(null)），get必须能从本地Rocks恢复
			var r = getRecord(oldTable, key2);
			Assertions.assertNotNull(r);
			invokeOn(r, "setSoftValue", (Object)null);
			Assertions.assertEquals(222, getV1(app, oldTable, key2), "软引用回收后必须能从本地Rocks恢复，否则记录永久丢失");

			// 核心3：受限容量walkMemory（走localRocks）回滚后可见
			Assertions.assertTrue(oldTable.getTableConf().getRealCacheCapacity() >= 0); // sanity:默认受限
			var found = new int[] {0};
			oldTable.walkMemory((k, v) -> {
				if (v.getV1() == 111 || v.getV1() == 222)
					found[0]++;
				return true;
			});
			Assertions.assertEquals(2, found[0]);
		} finally {
			app.stop();
		}
	}
}
