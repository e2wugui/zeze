package UnitTest.Zeze.Trans;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import Zeze.Application;
import Zeze.Config;
import Zeze.Services.GlobalCacheManagerConst;
import Zeze.Transaction.Record1;
import demo.Module1.BValue;
import demo.Module1.Table3;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND-T2-1 回归：TableCache LRU 块条目的精确删除语义。
 * getOrAdd 的热点迁移路径必须使用两参 remove，
 * 不能误删旧块中并发新建记录的条目（否则活记录脱离LRU，永不清理）；
 * remove 的 dataMap 删除失败分支必须摘除滞留在块内的过期条目
 * （否则块永不为空，cleanNow 在超容量时死循环）。
 */
@Fast
public class TestTableCacheLru {
	// Application并发需要不同serverId：本地zeze_cache_<serverId>目录每serverId一份，
	// @Fast类并行时共用会撞。从300起避开其他测试（TakeoverTestEnv从100起、伪造死者id 777+）。
	// 表用demo的持久表Table3：Application构造会自动注册Builtin组件表
	// （tQueues等），addTable同名表会报duplicate table id。
	private static final AtomicInteger nextServerId = new AtomicInteger(300);

	private static Application newApp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(nextServerId.getAndIncrement());
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("t2_lru_test_" + conf.getServerId());
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		return new Application("TestTableCacheLru@" + conf.getServerId(), conf);
	}

	private static Object get(Object obj, String name) throws ReflectiveOperationException {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.get(obj);
	}

	private static void set(Object obj, String name, Object value) throws ReflectiveOperationException {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		f.set(obj, value);
	}

	private static Field findField(Class<?> clazz, String name) throws ReflectiveOperationException {
		for (var c = clazz; c != null; c = c.getSuperclass()) {
			try {
				return c.getDeclaredField(name);
			} catch (NoSuchFieldException ignored) {
			}
		}
		throw new NoSuchFieldException(name);
	}

	private static Object invoke(Object obj, String name, Object... args) throws ReflectiveOperationException {
		for (var c = obj.getClass(); c != null; c = c.getSuperclass()) {
			for (var m : c.getDeclaredMethods()) {
				if (m.getName().equals(name) && m.getParameterCount() == args.length) {
					m.setAccessible(true);
					return m.invoke(obj, args);
				}
			}
		}
		throw new NoSuchMethodException(name);
	}

	/**
	 * getOrAdd 迁移路径拿到过期引用（并发删除后 dataMap.get 的旧值）时，
	 * 旧块中该 key 可能已经是并发新建的记录——迁移不得误删它的条目。
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void testMigrateNotRemoveConcurrentRecordEntry() throws Exception {
		var app = newApp();
		var table = new Table3();
		app.addTable("", table);
		app.start();
		try {
			var cache = table.getCache();
			var key = 101L;
			var dataMap = (ConcurrentHashMap<Long, Record1<Long, BValue>>)get(cache, "dataMap");
			var hot0 = (ConcurrentHashMap<Long, Record1<Long, BValue>>)get(cache, "lruHot");

			var rOld = new Record1<>(table, key, null); // getOrAdd 拿到的（过期）引用
			var rNew = new Record1<>(table, key, null); // 并发新建、已占据旧块同key槽位的记录
			dataMap.put(key, rOld);
			hot0.put(key, rNew);
			set(rOld, "lruNode", hot0); // 模拟并发删除后残留的 lruNode 字段引用

			invoke(cache, "newLruHot"); // 热块轮换：this.lruHot = hot1
			var hot1 = (ConcurrentHashMap<Long, Record1<Long, BValue>>)get(cache, "lruHot");
			Assertions.assertNotSame(hot0, hot1);

			var got = cache.getOrAdd(key, () -> new Record1<>(table, key, null));
			Assertions.assertSame(rOld, got);
			// 核心：旧块中并发新建记录的条目不能被迁移误删（活记录脱离LRU将永不清理）
			Assertions.assertSame(rNew, hot0.get(key));
			// 过期引用仍迁移进新块（保持原有语义）
			Assertions.assertSame(rOld, hot1.get(key));
		} finally {
			app.stop();
		}
	}

	/** 正常迁移回归：旧块条目被精确删除，记录进入当前热块。 */
	@Test
	@SuppressWarnings("unchecked")
	public void testMigrateNormal() throws Exception {
		var app = newApp();
		var table = new Table3();
		app.addTable("", table);
		app.start();
		try {
			var cache = table.getCache();
			var key = 102L;
			var dataMap = (ConcurrentHashMap<Long, Record1<Long, BValue>>)get(cache, "dataMap");
			var hot0 = (ConcurrentHashMap<Long, Record1<Long, BValue>>)get(cache, "lruHot");

			var r = new Record1<>(table, key, null);
			dataMap.put(key, r);
			hot0.put(key, r);
			set(r, "lruNode", hot0);

			invoke(cache, "newLruHot");
			var hot1 = (ConcurrentHashMap<Long, Record1<Long, BValue>>)get(cache, "lruHot");

			var got = cache.getOrAdd(key, () -> new Record1<>(table, key, null));
			Assertions.assertSame(r, got);
			Assertions.assertNull(hot0.get(key)); // 从旧块移除
			Assertions.assertSame(r, hot1.get(key)); // 进入新块
			Assertions.assertSame(hot1, get(r, "lruNode"));
		} finally {
			app.stop();
		}
	}

	/**
	 * remove 的 dataMap 删除失败分支（key 已映射到别的记录）：
	 * 滞留在 Lru 块内的过期条目必须被摘除，否则块永不为空，cleanNow 死循环。
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void testRemoveStaleEntryFromLruNode() throws Exception {
		var app = newApp();
		var table = new Table3();
		app.addTable("", table);
		app.start();
		try {
			var cache = table.getCache();
			var key = 103L;
			var dataMap = (ConcurrentHashMap<Long, Record1<Long, BValue>>)get(cache, "dataMap");
			var hot0 = (ConcurrentHashMap<Long, Record1<Long, BValue>>)get(cache, "lruHot");

			var rOther = new Record1<>(table, key, null); // dataMap 当前映射（活的）
			var rStale = new Record1<>(table, key, null); // 滞留在块内的过期条目
			dataMap.put(key, rOther);
			hot0.put(key, rStale);
			set(rStale, "lruNode", hot0);

			// dataMap.remove(key, rStale) 失败 -> else 分支
			invoke(cache, "remove", key, rStale, false);

			// 核心：块内过期条目被摘除，且不误删其他记录
			Assertions.assertNull(hot0.get(key));
			Assertions.assertSame(rOther, dataMap.get(key));
			Assertions.assertEquals(GlobalCacheManagerConst.StateRemoved,
					((Number)invoke(rStale, "getState")).intValue());
		} finally {
			app.stop();
		}
	}
}
