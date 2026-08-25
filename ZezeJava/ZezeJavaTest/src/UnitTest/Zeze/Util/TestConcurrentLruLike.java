package UnitTest.Zeze.Util;

import java.util.Map;
import Zeze.Util.ConcurrentLruLike;
import Zeze.Util.Task;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
}
