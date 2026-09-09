package UnitTest.Zeze.Util;

import harness.Fast;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.Util.ConcurrentLruLike;
import Zeze.Util.Task;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Fast
public class TestConcurrentLruLike {
	@org.junit.jupiter.api.BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	@Test
	public void testRemoveIfValueMatches() throws Exception {
		// period 调大，避免测试期间 lruHot 轮换/clean 干扰反射检查
		var lru = new ConcurrentLruLike<String, Object>("testRemoveIfValueMatches", 100, null, 600_000, 600_000, 16);
		var v1 = new Object();
		Assertions.assertSame(v1, lru.getOrAdd("k", () -> v1));

		// 值不匹配：不删
		Assertions.assertFalse(lru.remove("k", new Object()));
		Assertions.assertSame(v1, lru.get("k"));

		// key 不存在：不删
		Assertions.assertFalse(lru.remove("absent", v1));

		// 值匹配：删除成功，dataMap 与 lru 记录同步清理
		Assertions.assertTrue(lru.remove("k", v1));
		Assertions.assertNull(lru.get("k"));
		Assertions.assertFalse(lruHotContains(lru, "k"));

		// 已删除后再条件删除返回 false
		Assertions.assertFalse(lru.remove("k", v1));

		// 竞态场景：删除后同 key 已重建新值，按旧值条件删除不得误删新映射
		var v2 = new Object();
		Assertions.assertSame(v2, lru.getOrAdd("k", () -> v2));
		Assertions.assertFalse(lru.remove("k", v1));
		Assertions.assertSame(v2, lru.get("k"));
		Assertions.assertTrue(lru.remove("k", v2));
		Assertions.assertNull(lru.get("k"));
		Assertions.assertFalse(lruHotContains(lru, "k"));

		// 普通 remove(key) 行为不变：删除并清 lru 记录
		var v3 = new Object();
		Assertions.assertSame(v3, lru.getOrAdd("k2", () -> v3));
		Assertions.assertSame(v3, lru.remove("k2"));
		Assertions.assertNull(lru.get("k2"));
		Assertions.assertFalse(lruHotContains(lru, "k2"));
	}

	@SuppressWarnings("unchecked")
	private static boolean lruHotContains(ConcurrentLruLike<?, ?> lru, Object key) throws Exception {
		var f = ConcurrentLruLike.class.getDeclaredField("lruHot");
		f.setAccessible(true);
		return ((Map<Object, ?>)f.get(lru)).containsKey(key);
	}

	@Test
	public void testCleanNowNotStuckOnBusyOldestNode() throws Exception {
		// 最老node的回调恒失败（模拟TaskOneByOneByKeyLru的队列忙/残留队列），新node的key可删。
		// 修复前：cleanNow只盯最老node，warn+sleep忙等无限循环，后面的可删条目永远轮不到，且调度线程被占死。
		var ref = new AtomicReference<ConcurrentLruLike<String, Object>>();
		// period调大，避免测试期间后台newLruHot轮换/clean干扰；多代node由反射显式创建。
		var lru = new ConcurrentLruLike<String, Object>("testCleanNowNotStuck", 2,
				(k, v) -> !k.startsWith("busy") && ref.get().remove(k) != null,
				600_000, 600_000, 16);
		ref.set(lru);

		// 老node：全部删不掉
		lru.getOrAdd("busy1", Object::new);
		lru.getOrAdd("busy2", Object::new);
		invokeNewLruHot(lru);
		// 新node：可删条目，总量5 > capacity 2
		lru.getOrAdd("e1", Object::new);
		lru.getOrAdd("e2", Object::new);
		lru.getOrAdd("e3", Object::new);
		invokeNewLruHot(lru); // 再换一代hot，保证最老的busy node不是hot

		var cleaner = new Thread(null, () -> {
			try {
				invokeCleanNow(lru);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}, "testCleanNowNotStuck-cleaner", 100_000);
		cleaner.setDaemon(true);
		cleaner.start();
		cleaner.join(3000);
		Assertions.assertFalse(cleaner.isAlive(), "cleanNow必须在有限时间内返回：不得对删不掉的最老node忙等");

		// busy条目保留（回调失败），可删条目被驱逐，总量降回capacity以内
		Assertions.assertNotNull(lru.get("busy1"));
		Assertions.assertNotNull(lru.get("busy2"));
		Assertions.assertNull(lru.get("e1"));
		Assertions.assertNull(lru.get("e2"));
		Assertions.assertNull(lru.get("e3"));
	}

	private static void invokeNewLruHot(ConcurrentLruLike<?, ?> lru) throws Exception {
		var m = ConcurrentLruLike.class.getDeclaredMethod("newLruHot");
		m.setAccessible(true);
		m.invoke(lru);
	}

	private static void invokeCleanNow(ConcurrentLruLike<?, ?> lru) throws Exception {
		var m = ConcurrentLruLike.class.getDeclaredMethod("cleanNow");
		m.setAccessible(true);
		m.invoke(lru);
	}

	/**
	 * FND3-09 回归：get/getOrAdd 与 remove 的 check-then-act 竞态。
	 * 迟到的 adjustLru（携带并发remove完成后的过期引用，仅剩陈旧的lruNode字段）
	 * 不得把死条目登记进热点节点——否则死条目永久滞留节点，节点永不为空，
	 * cleanNow 对它反复回调/warn。
	 */
	@Test
	public void testAdjustLruNotRegisterDeadItem() throws Exception {
		// period 调大，避免测试期间 lruHot 轮换/clean 干扰
		var lru = new ConcurrentLruLike<String, Object>("testAdjustLruDead", 100, null, 600_000, 600_000, 16);
		var v = new Object();
		Assertions.assertSame(v, lru.getOrAdd("k", () -> v));
		var hot0 = lruHotOf(lru);
		invokeNewLruHot(lru);
		var hot1 = lruHotOf(lru);

		// 模拟并发remove完整完成后的状态：dataMap删除+节点摘除，仅剩item.lruNode陈旧字段。
		// get已在remove前读到该条目，随后执行的就是下面这个"迟到"的adjustLru。
		var item = dataMapOf(lru).remove("k");
		hot0.remove("k", item);
		Assertions.assertNull(lru.get("k"));
		invokeAdjustLru(lru, "k", item, hot1);

		Assertions.assertNull(hot1.get("k"), "死条目不得登记进热点节点");
	}

	/**
	 * FND3-09 对偶分支回归：迁移撞上热点内同key的过期占坑登记时，
	 * 必须摘除占坑者并完成登记；直接放弃会让活条目永久脱离所有节点（lruNode停留null），
	 * 容量驱逐对它永久失效。
	 */
	@Test
	public void testAdjustLruEvictStaleOccupant() throws Exception {
		var lru = new ConcurrentLruLike<String, Object>("testAdjustLruStaleOccupant", 100, null, 600_000, 600_000, 16);
		var v = new Object();
		Assertions.assertSame(v, lru.getOrAdd("k", () -> v));
		var hot0 = lruHotOf(lru);
		invokeNewLruHot(lru);
		var hot1 = lruHotOf(lru);

		// 占坑的过期条目（dataMap无此映射）
		var stale = newLruItem(new Object(), hot1);
		hot1.put("k", stale);

		var item = dataMapOf(lru).get("k");
		invokeAdjustLru(lru, "k", item, hot1);

		Assertions.assertSame(item, hot1.get("k"), "活条目必须完成登记（否则永久脱离LRU）");
	}

	/** FND3-09：cleanNow（无回调路径）必须摘除节点内的过期登记，否则节点永不为空。 */
	@Test
	public void testCleanNowRemovesStaleEntry() throws Exception {
		var lru = new ConcurrentLruLike<String, Object>("testCleanStale", 2, null, 600_000, 600_000, 16);
		var hot0 = lruHotOf(lru);
		// 过期登记滞留在最老节点（dataMap无此映射）
		hot0.put("ghost", newLruItem(new Object(), hot0));
		invokeNewLruHot(lru); // hot1
		// 活条目超容量（2>capacity），驱动cleanNow处理老节点
		lru.getOrAdd("a", Object::new);
		lru.getOrAdd("b", Object::new);
		lru.getOrAdd("c", Object::new);
		invokeCleanNow(lru);

		Assertions.assertFalse(hot0.containsKey("ghost"), "过期登记必须被摘除");
		// 活条目都在当前热点，不应被驱逐
		Assertions.assertNotNull(lru.get("a"));
		Assertions.assertNotNull(lru.get("b"));
		Assertions.assertNotNull(lru.get("c"));
	}

	/** FND3-09：cleanNow（回调路径）对过期登记不得执行回调，直接摘除。 */
	@Test
	public void testCleanNowNotCallbackOnStaleEntry() throws Exception {
		var ref = new AtomicReference<ConcurrentLruLike<String, Object>>();
		var calledKeys = ConcurrentHashMap.newKeySet();
		var lru = new ConcurrentLruLike<String, Object>("testCleanStaleCb", 2, (k, v) -> {
			calledKeys.add(k);
			return ref.get().remove(k) != null;
		}, 600_000, 600_000, 16);
		ref.set(lru);
		var hot0 = lruHotOf(lru);
		hot0.put("ghost", newLruItem(new Object(), hot0));
		invokeNewLruHot(lru); // hot1
		lru.getOrAdd("a", Object::new);
		lru.getOrAdd("b", Object::new);
		lru.getOrAdd("c", Object::new);
		invokeNewLruHot(lru); // hot2：hot1成为可清理的普通节点

		invokeCleanNow(lru);

		Assertions.assertFalse(calledKeys.contains("ghost"), "不得对死值执行回调");
		Assertions.assertFalse(hot0.containsKey("ghost"), "过期登记必须被摘除");
		Assertions.assertTrue(calledKeys.contains("a"), "活条目正常走回调驱逐");
	}

	/** FND3-09：shrink迁移不得把死条目搬进队头节点（应随被poll的节点一起废弃）。 */
	@Test
	public void testTryPollLruQueueNotMigrateDeadEntry() throws Exception {
		var lru = new ConcurrentLruLike<String, Object>("testShrinkNotMigrateDead", 100, null, 600_000, 600_000, 16);
		var liveV = new Object();
		Assertions.assertSame(liveV, lru.getOrAdd("live", () -> liveV));
		var hot0 = lruHotOf(lru);
		// 死条目滞留在hot0（dataMap无此映射），lruNode字段指向hot0
		hot0.put("dead", newLruItem(new Object(), hot0));

		// 节点数超过MAX_NODE_COUNT(8640)触发shrink
		while (lruQueueOf(lru).size() <= 8640)
			invokeNewLruHot(lru);
		invokeTryPollLruQueue(lru);

		var head = lruQueueOf(lru).peek();
		Assertions.assertSame(dataMapOf(lru).get("live"), head.get("live"), "活条目应迁移到队头");
		Assertions.assertNull(head.get("dead"), "死条目不得迁移进队头");
	}

	/**
	 * FND3-09 并发压力回归：多线程 get/getOrAdd/remove（含按值删除）+ 超容量驱逐
	 * （cleanNow自身的remove就会与并发get对撞，是幽灵条目的常规制造者）。
	 * 结束后清空全部活条目，所有节点必须完全排空：残留即竞态漏掉的死条目。
	 */
	@Test
	public void testConcurrentGetRemoveNoResidue() throws Exception {
		var lru = new ConcurrentLruLike<String, Object>("testStressNoResidue", 8, null, 50, 100, 16);
		var keys = new String[64];
		for (var i = 0; i < keys.length; i++)
			keys[i] = "k" + i;
		var threads = new Thread[8];
		var stopAt = System.currentTimeMillis() + 1500;
		for (var t = 0; t < threads.length; t++) {
			threads[t] = new Thread(null, () -> {
				var rnd = ThreadLocalRandom.current();
				while (System.currentTimeMillis() < stopAt) {
					var k = keys[rnd.nextInt(keys.length)];
					switch (rnd.nextInt(4)) {
					case 0 -> lru.get(k);
					case 1 -> lru.getOrAdd(k, Object::new);
					case 2 -> lru.remove(k);
					default -> {
						var v = lru.get(k);
						if (v != null)
							lru.remove(k, v);
					}
					}
				}
			}, "testStressNoResidue-" + t, 100_000);
			threads[t].setDaemon(true);
			threads[t].start();
		}
		for (var thread : threads)
			thread.join();

		// 清空所有活条目后，节点内不得有任何残留登记（允许短暂等待消化瞬态）
		for (var k : keys)
			lru.remove(k);
		var deadline = System.currentTimeMillis() + 5000;
		int residue;
		do {
			Thread.sleep(100);
			residue = 0;
			for (var node : lruQueueOf(lru))
				residue += node.size();
		} while (residue > 0 && System.currentTimeMillis() < deadline);
		Assertions.assertEquals(0, residue, "并发get/remove后节点不得有残留登记（幽灵条目）");
	}

	/**
	 * FND3-09 加固回归：adjustLru 存活检查（循环条件）与 putIfAbsent 之间发生 remove+重建时，
	 * 占坑的 prev 是并发重建后【无条件 put 登记】进本热点的活条目（getOrAdd.computeIfAbsent MUST replace），
	 * 不得盲摘——误摘会让活条目脱管（lruNode 指向热点但登记被摘，容量驱逐与 cleanNow 都看不见，兜底无法收敛）。
	 * 用 rigged 热点在 putIfAbsent 调用点精确注入该交错。
	 */
	@Test
	public void testAdjustLruNotEvictLiveOccupant() throws Exception {
		// period 调大，避免测试期间 lruHot 轮换/clean 干扰
		var lru = new ConcurrentLruLike<String, Object>("testAdjNotEvictLive", 100, null, 600_000, 600_000, 16);
		var v1 = new Object();
		Assertions.assertSame(v1, lru.getOrAdd("k", () -> v1));
		var hot0 = lruHotOf(lru);
		invokeNewLruHot(lru);

		var dataMap = dataMapOf(lru);
		var itemA = dataMap.get("k"); // A：验活时仍活的旧条目，随即"挂起"在 putIfAbsent 前
		var rigged = new InterleavingHot();
		rigged.interleave = () -> {
			try {
				// B：remove 完成（dataMap 删除；摘除因读到 null 的 lruNode 跳过——A 已取走，无需模拟）
				dataMap.remove("k", itemA);
				// C：getOrAdd 重建：computeIfAbsent 创建 + 热点无条件 put 登记（MUST replace）
				var itemC = newLruItem(new Object(), rigged);
				dataMap.put("k", itemC);
				rigged.put("k", itemC);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		};
		invokeAdjustLru(lru, "k", itemA, rigged);

		Assertions.assertNull(hot0.get("k"), "旧节点条目已被迁移方摘除");
		Assertions.assertSame(dataMap.get("k"), rigged.get("k"), "占坑的活条目登记不得被误摘（否则活条目脱管）");
		Assertions.assertNotSame(itemA, rigged.get("k"), "死条目不得完成登记");
	}

	/**
	 * FND3-09 加固回归：shrink 迁移的占坑协议与 adjustLru 一致。
	 * 当前结构下重建只登记进当前热点、必新于 head，活占坑按证不可达；
	 * 本用例直接构造该状态钉住防御分支：占坑的活条目登记不得被盲摘。
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void testTryPollLruQueueNotEvictLiveOccupant() throws Exception {
		var lru = new ConcurrentLruLike<String, Object>("testShrinkNotEvictLive", 100, null, 600_000, 600_000, 16);
		var dataMap = dataMapOf(lru);
		var queue = lruQueueOf(lru);
		queue.clear();

		// 迁移方 r：活映射，登记在被 poll 的节点里
		var pollNode = new ConcurrentHashMap<String, Object>();
		var r = newLruItem(new Object(), pollNode);
		pollNode.put("k", r);
		dataMap.put("k", r);

		var rigged = new InterleavingHot();
		rigged.interleave = () -> {
			try {
				dataMap.remove("k", r);
				var itemC = newLruItem(new Object(), rigged);
				dataMap.put("k", itemC);
				rigged.put("k", itemC);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		};

		// 队列构型（MAX_NODE_COUNT=8640, SHRINK_NODE_COUNT=8000）：
		// pollNode + 640 空节点将被 poll；rigged 位于 poll 边界（poll 后成为 peek），再补足 7999 个占位
		queue.add(pollNode);
		for (var i = 0; i < 640; i++)
			queue.add(new ConcurrentHashMap<String, Object>());
		queue.add((ConcurrentHashMap<String, Object>)(ConcurrentHashMap<?, ?>)rigged);
		for (var i = 0; i < 7999; i++)
			queue.add(new ConcurrentHashMap<String, Object>());

		invokeTryPollLruQueue(lru);

		Assertions.assertSame(rigged, (ConcurrentHashMap<?, ?>)queue.peek());
		Assertions.assertSame(dataMap.get("k"), rigged.get("k"), "占坑的活条目登记不得被误摘");
		Assertions.assertNotSame(r, rigged.get("k"), "被取代的条目不得迁移进队头");
	}

	/** rigged 热点节点：首次 putIfAbsent 前执行 interleave，在挂起点注入并发交错。 */
	@SuppressWarnings("serial")
	private static final class InterleavingHot extends ConcurrentHashMap<Object, Object> {
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

	@SuppressWarnings("unchecked")
	private static ConcurrentHashMap<String, Object> dataMapOf(ConcurrentLruLike<?, ?> lru) throws Exception {
		var f = ConcurrentLruLike.class.getDeclaredField("dataMap");
		f.setAccessible(true);
		return (ConcurrentHashMap<String, Object>)f.get(lru);
	}

	@SuppressWarnings("unchecked")
	static ConcurrentHashMap<String, Object> lruHotOf(ConcurrentLruLike<?, ?> lru) throws Exception {
		var f = ConcurrentLruLike.class.getDeclaredField("lruHot");
		f.setAccessible(true);
		return (ConcurrentHashMap<String, Object>)f.get(lru);
	}

	@SuppressWarnings("unchecked")
	private static ConcurrentLinkedQueue<ConcurrentHashMap<String, Object>> lruQueueOf(ConcurrentLruLike<?, ?> lru)
			throws Exception {
		var f = ConcurrentLruLike.class.getDeclaredField("lruQueue");
		f.setAccessible(true);
		return (ConcurrentLinkedQueue<ConcurrentHashMap<String, Object>>)f.get(lru);
	}

	private static Object newLruItem(Object value, Object node) throws Exception {
		var c = Class.forName("Zeze.Util.ConcurrentLruLike$LruItem");
		var ctor = c.getDeclaredConstructors()[0];
		ctor.setAccessible(true);
		return ctor.newInstance(value, node);
	}

	private static void invokeAdjustLru(ConcurrentLruLike<?, ?> lru, Object key, Object item, Object hot)
			throws Exception {
		var m = ConcurrentLruLike.class.getDeclaredMethod("adjustLru", Object.class,
				Class.forName("Zeze.Util.ConcurrentLruLike$LruItem"), ConcurrentHashMap.class);
		m.setAccessible(true);
		m.invoke(lru, key, item, hot);
	}

	private static void invokeTryPollLruQueue(ConcurrentLruLike<?, ?> lru) throws Exception {
		var m = ConcurrentLruLike.class.getDeclaredMethod("tryPollLruQueue");
		m.setAccessible(true);
		m.invoke(lru);
	}
}
