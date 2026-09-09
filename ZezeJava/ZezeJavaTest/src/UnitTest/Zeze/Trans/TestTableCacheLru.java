package UnitTest.Zeze.Trans;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
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

	/**
	 * FND3-09 回归：getOrAdd 与 remove 的 check-then-act 竞态。
	 * 迟到的热点迁移（携带并发Remove完成后的过期引用，仅剩陈旧的lruNode字段）
	 * 不得把死记录登记进热点块——死记录将永久滞留块内，
	 * cleanNow每轮对它加锁尝试删除且节点永不为空。
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void testMigrateNotRegisterDeadRecord() throws Exception {
		var app = newApp();
		var table = new Table3();
		app.addTable("", table);
		app.start();
		try {
			var cache = table.getCache();
			var key = 104L;
			var dataMap = (ConcurrentHashMap<Long, Record1<Long, BValue>>)get(cache, "dataMap");
			var hot0 = (ConcurrentHashMap<Long, Record1<Long, BValue>>)get(cache, "lruHot");

			var r = new Record1<>(table, key, null);
			dataMap.put(key, r);
			hot0.put(key, r);
			set(r, "lruNode", hot0);

			invoke(cache, "newLruHot");
			var hot1 = (ConcurrentHashMap<Long, Record1<Long, BValue>>)get(cache, "lruHot");

			// 模拟并发Remove完整完成后的状态：dataMap删除+块内摘除，仅剩r.lruNode陈旧字段。
			// getOrAdd已在Remove前读到该记录，随后执行的就是这个"迟到"的迁移。
			dataMap.remove(key);
			hot0.remove(key, r);

			invoke(cache, "adjustLru", key, r, hot1);

			Assertions.assertNull(hot1.get(key), "死记录不得登记进热点块");
		} finally {
			app.stop();
		}
	}

	/**
	 * FND3-09 对偶分支回归：迁移撞上热点块内同key的过期占坑登记时，
	 * 必须摘除占坑者并完成登记；直接放弃会让活记录永久脱离所有块（lruNode停留null），
	 * 容量驱逐对它永久失效。
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void testMigrateEvictStaleOccupant() throws Exception {
		var app = newApp();
		var table = new Table3();
		app.addTable("", table);
		app.start();
		try {
			var cache = table.getCache();
			var key = 105L;
			var dataMap = (ConcurrentHashMap<Long, Record1<Long, BValue>>)get(cache, "dataMap");
			var hot0 = (ConcurrentHashMap<Long, Record1<Long, BValue>>)get(cache, "lruHot");

			var rLive = new Record1<>(table, key, null);
			dataMap.put(key, rLive);
			hot0.put(key, rLive);
			set(rLive, "lruNode", hot0);

			invoke(cache, "newLruHot");
			var hot1 = (ConcurrentHashMap<Long, Record1<Long, BValue>>)get(cache, "lruHot");

			// 占坑的过期登记（dataMap无此映射）
			var rStale = new Record1<>(table, key, null);
			hot1.put(key, rStale);

			invoke(cache, "adjustLru", key, rLive, hot1);

			Assertions.assertSame(rLive, hot1.get(key), "活记录必须完成登记（否则永久脱离LRU）");
			Assertions.assertSame(hot1, get(rLive, "lruNode"));
		} finally {
			app.stop();
		}
	}

	/**
	 * FND3-09 加固回归：adjustLru 存活检查（循环条件）与 putIfAbsent 之间发生 Remove+重建时，
	 * 占坑的 prev 是并发重建后【无条件 put 登记】进本热点块的活记录（getOrAdd.computeIfAbsent MUST replace），
	 * 不得盲摘——误摘会让活记录脱管（lruNode 指向热点块但登记被摘，容量驱逐与 cleanNow 都看不见，兜底无法收敛）。
	 * 用 rigged 热点块在 putIfAbsent 调用点精确注入该交错。
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void testMigrateNotEvictLiveOccupant() throws Exception {
		var app = newApp();
		var table = new Table3();
		app.addTable("", table);
		app.start();
		try {
			var cache = table.getCache();
			var key = 106L;
			var dataMap = (ConcurrentHashMap<Long, Record1<Long, BValue>>)get(cache, "dataMap");
			var hot0 = (ConcurrentHashMap<Long, Record1<Long, BValue>>)get(cache, "lruHot");

			var rOld = new Record1<>(table, key, null); // 迁移方：验活时仍活的旧记录
			dataMap.put(key, rOld);
			hot0.put(key, rOld);
			set(rOld, "lruNode", hot0);

			invoke(cache, "newLruHot");

			var rigged = new InterleavingNode();
			rigged.interleave = () -> {
				try {
					// B：Remove 完成（dataMap 删除；摘除因读到 null 的 lruNode 跳过——迁移方已取走）
					dataMap.remove(key, rOld);
					// C：getOrAdd 重建：computeIfAbsent 创建 + 热点块无条件 put 登记（MUST replace）
					var rNewC = new Record1<>(table, key, null);
					set(rNewC, "lruNode", rigged);
					dataMap.put(key, rNewC);
					rigged.put(key, rNewC);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			};
			invoke(cache, "adjustLru", key, rOld, rigged);

			Assertions.assertNull(hot0.get(key), "旧块条目已被迁移方摘除");
			Assertions.assertSame(dataMap.get(key), rigged.get(key), "占坑的活记录登记不得被误摘（否则活记录脱管）");
			Assertions.assertNotSame(rOld, rigged.get(key), "死记录不得完成登记");
		} finally {
			app.stop();
		}
	}

	/**
	 * FND3-09 加固回归：shrink 迁移的占坑协议与 adjustLru 一致。
	 * 当前结构下重建只登记进当前热点、必新于 head，活占坑按证不可达；
	 * 本用例直接构造该状态钉住防御分支：占坑的活记录登记不得被盲摘。
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void testShrinkNotEvictLiveOccupant() throws Exception {
		var app = newApp();
		var table = new Table3();
		app.addTable("", table);
		app.start();
		var cache = table.getCache();
		try {
			var key = 107L;
			var dataMap = (ConcurrentHashMap<Long, Record1<Long, BValue>>)get(cache, "dataMap");
			var queue = (ConcurrentLinkedQueue<ConcurrentHashMap<Long, Record1<Long, BValue>>>)
					(ConcurrentLinkedQueue<?>)get(cache, "lruQueue");
			queue.clear();

			// 迁移方 r：活映射，登记在被 poll 的块里
			var pollNode = new ConcurrentHashMap<Long, Record1<Long, BValue>>();
			var r = new Record1<>(table, key, null);
			set(r, "lruNode", pollNode);
			pollNode.put(key, r);
			dataMap.put(key, r);

			var rigged = new InterleavingNode();
			rigged.interleave = () -> {
				try {
					dataMap.remove(key, r);
					var rNewC = new Record1<>(table, key, null);
					set(rNewC, "lruNode", rigged);
					dataMap.put(key, rNewC);
					rigged.put(key, rNewC);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			};

			// 队列构型（MAX_NODE_COUNT=8640, SHRINK_NODE_COUNT=8000）：
			// pollNode + 640 空块将被 poll；rigged 位于 poll 边界（poll 后成为 peek），再补足 7999 个占位
			queue.add(pollNode);
			for (var i = 0; i < 640; i++)
				queue.add(new ConcurrentHashMap<Long, Record1<Long, BValue>>());
			queue.add((ConcurrentHashMap<Long, Record1<Long, BValue>>)(ConcurrentHashMap<?, ?>)rigged);
			for (var i = 0; i < 7999; i++)
				queue.add(new ConcurrentHashMap<Long, Record1<Long, BValue>>());

			invoke(cache, "tryPollLruQueue");

			Assertions.assertSame(rigged, (ConcurrentHashMap<?, ?>)queue.peek());
			Assertions.assertSame(dataMap.get(key), rigged.get(key), "占坑的活记录登记不得被误摘");
			Assertions.assertNotSame(r, rigged.get(key), "被取代的记录不得迁移进队头");
		} finally {
			// 恢复队列结构（清掉 8000+ 占位块，lruHot 重新入队），避免影响 stop 流程
			var queue = (ConcurrentLinkedQueue<ConcurrentHashMap<Long, Record1<Long, BValue>>>)
					(ConcurrentLinkedQueue<?>)get(cache, "lruQueue");
			queue.clear();
			invoke(cache, "newLruHot");
			app.stop();
		}
	}

	/** rigged 热点块：首次 putIfAbsent 前执行 interleave，在挂起点注入并发交错。 */
	@SuppressWarnings("serial")
	private static final class InterleavingNode extends ConcurrentHashMap<Object, Object> {
		private static final long serialVersionUID = 1L;
		private volatile boolean armed = true;
		private volatile Runnable interleave;

		@Override
		public Object putIfAbsent(Object key, Object value) {
			if (armed) {
				armed = false;
				interleave.run();
			}
			return super.putIfAbsent(key, value);
		}
	}
}
