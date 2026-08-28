package UnitTest.Zeze.Util;

import harness.Fast;
import java.util.Map;
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
}
