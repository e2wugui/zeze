package UnitTest.Zeze.Trans;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import Zeze.Application;
import Zeze.Config;
import Zeze.Services.GlobalCacheManagerConst;
import Zeze.Transaction.Record1;
import Zeze.Util.TaskSpec;
import demo.Module1.BValue;
import demo.Module1.Table3;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND-T2-2 回归：StateInvalid 装载路径不得覆盖未 flush 的脏数据。
 * GCM reduce 的 flush 失败或 Releaser 降级窗口会留下 Invalid+dirty 的记录，
 * 此时后台库中还是旧值：load/selectDirty 的 Invalid 分支从库装载 setSoftValue
 * 会把内存中已提交的修改（strongDirtyValue）覆盖丢失。
 * 表用demo的持久表Table3：Application构造会自动注册Builtin组件表
 * （tQueues等），addTable同名表会报duplicate table id。
 */
@Fast
public class TestInvalidDirtyLoad {
	// 独立serverId隔离本地zeze_cache目录与其他@Fast测试（T2-1组从300起）。
	private static final AtomicInteger nextServerId = new AtomicInteger(400);

	private static Application newApp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(nextServerId.getAndIncrement());
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("t2_invaliddirty_test_" + conf.getServerId());
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		return new Application("TestInvalidDirtyLoad@" + conf.getServerId(), conf);
	}

	@SuppressWarnings("unchecked")
	private static Record1<Long, BValue> getRecord(Table3 table, long key) throws ReflectiveOperationException {
		var cache = table.getCache();
		for (Method m : cache.getClass().getDeclaredMethods()) {
			if (m.getName().equals("get") && m.getParameterCount() == 1) {
				m.setAccessible(true);
				return (Record1<Long, BValue>)m.invoke(cache, key);
			}
		}
		throw new NoSuchMethodException("TableCache.get");
	}

	/** 模拟GCM reduce/Releaser窗口：本地置Invalid（flush失败或降级先行的遗留状态）。 */
	private static void setStateInvalid(Record1<Long, BValue> r) throws ReflectiveOperationException {
		for (Class<?> c = r.getClass(); c != null; c = c.getSuperclass()) {
			for (Method m : c.getDeclaredMethods()) {
				if (m.getName().equals("setState") && m.getParameterCount() == 1) {
					m.setAccessible(true);
					m.invoke(r, GlobalCacheManagerConst.StateInvalid);
					return;
				}
			}
		}
		throw new NoSuchMethodException("Record.setState");
	}

	private static void put(Application app, Table3 table, long key, int v1) {
		var rc = TaskSpec.ofProcedure(app.newProcedure(() -> {
			table.getOrAdd(key).setInt_1(v1);
			return 0L;
		}, "TestInvalidDirtyLoad.put@" + v1)).call();
		Assertions.assertEquals(0L, rc);
	}

	private static int getV1(Application app, Table3 table, long key) {
		int[] out = {Integer.MIN_VALUE};
		var rc = TaskSpec.ofProcedure(app.newProcedure(() -> {
			var v = table.get(key);
			out[0] = v != null ? v.getInt_1() : Integer.MIN_VALUE;
			return 0L;
		}, "TestInvalidDirtyLoad.get")).call();
		Assertions.assertEquals(0L, rc);
		return out[0];
	}

	/** 事务load路径：Invalid+dirty 时不得从后台库装载旧值覆盖脏数据。 */
	@Test
	public void testLoadNotOverwriteDirty() throws Exception {
		var app = newApp();
		var table = new Table3();
		app.addTable("", table);
		app.start();
		try {
			var key = 11L;
			put(app, table, key, 100);
			app.checkpointRun(); // 后台库=100，dirty清除
			Assertions.assertEquals(100, getV1(app, table, key)); // sanity

			put(app, table, key, 200); // 脏值v2=200，后台库仍是100
			var r = getRecord(table, key);
			setStateInvalid(r); // 构造 Invalid+dirty 窗口

			// 核心：事务装载不能被库中旧值100覆盖
			Assertions.assertEquals(200, getV1(app, table, key));

			// 脏数据最终正常flush，不丢失
			app.checkpointRun();
			Assertions.assertEquals(200, table.selectFromDatabase(key).getInt_1());
			Assertions.assertEquals(200, getV1(app, table, key));
		} finally {
			app.stop();
		}
	}

	/** 事务外selectDirty路径：Invalid+dirty 时不得从后台库装载旧值（也不污染本地Rocks镜像）。 */
	@Test
	public void testSelectDirtyNotOverwriteDirty() throws Exception {
		var app = newApp();
		var table = new Table3();
		app.addTable("", table);
		app.start();
		try {
			var key = 12L;
			put(app, table, key, 100);
			app.checkpointRun(); // 后台库=100

			put(app, table, key, 200); // 脏值v2=200，后台库仍是100
			var r = getRecord(table, key);
			setStateInvalid(r); // Invalid+dirty；commit后timestamp为正值，TTL判断必然进入装载分支

			// 核心：selectDirty 不能返回库中旧值100
			var v = table.selectDirty(key);
			Assertions.assertNotNull(v);
			Assertions.assertEquals(200, v.getInt_1());

			// 脏数据最终正常flush，不丢失
			app.checkpointRun();
			Assertions.assertEquals(200, table.selectFromDatabase(key).getInt_1());
			Assertions.assertEquals(200, getV1(app, table, key));
		} finally {
			app.stop();
		}
	}
}
