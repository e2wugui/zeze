package UnitTest.Zeze.Util;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Util.Cache;
import Zeze.Util.ConcurrentLruLike;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND7-36 回归：ConcurrentLruLike 构造器无条件注册两个常驻周期任务（热点轮转+cleanNow），
 * 句柄曾直接丢弃且全类无取消途径——实例被重建（Raft restore/reset 重开 Table）或
 * Cache.close() 后，旧实例的任务仍永续执行并强引用 dataMap 缓存对象图，任务与内存泄漏。
 * 修复：构造器保存两个 TimerFuture，close() 取消；Cache.close() 级联关闭内部 LRU。
 */
@Fast
public class TestFnd736ConcurrentLruLikeClose {

	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	@Test
	public void testCloseStopsPeriodicTasks() throws Exception {
		// lruInitialCapacity = 31/5 = 6，lruHot 超过 3 个条目即触发轮转任务建新节点。
		var lru = new ConcurrentLruLike<String, Object>("fnd736-lru", 100, null, 50, 50, 31);
		for (var i = 0; i < 6; i++) {
			var v = new Object();
			Assertions.assertSame(v, lru.getOrAdd("k" + i, () -> v));
		}

		// 前置证明：热点轮转任务在跑（lruQueue 随周期增长）。
		// 轮转只在当前热点超过半满时发生，持续访问把条目迁入每代新热点以驱动连续轮转。
		var queue = lruQueueOf(lru);
		var deadline = System.currentTimeMillis() + 5000;
		while (queue.size() < 4 && System.currentTimeMillis() < deadline) {
			for (var i = 0; i < 6; i++)
				Assertions.assertNotNull(lru.get("k" + i));
			Thread.sleep(20);
		}
		Assertions.assertTrue(queue.size() >= 4, "周期任务必须在close前驱动热点轮转，实际节点数: " + queue.size());

		lru.close();
		Assertions.assertTrue(timerCancelled(lru, "newLruHotTimer"), "close必须取消热点轮转任务");
		Assertions.assertTrue(timerCancelled(lru, "cleanTimer"), "close必须取消cleanNow任务");

		// close 后任务不得再驱动：节点数不再增长。
		var sizeAfterClose = queue.size();
		Thread.sleep(300);
		Assertions.assertEquals(sizeAfterClose, queue.size(), "close后周期任务不得再执行");
	}

	/** Cache.close 必须级联关闭内部 LRU 的两个周期任务（此前只清自己的 cleanTimer）。 */
	@Test
	public void testCacheCloseCascadesToLru(@TempDir Path tempDir) throws Exception {
		var dir = tempDir.resolve("fnd736-cache").toString();
		var cache = new Cache(dir, 16, id -> new UnitTest.Zeze.Util.TestCacheAppendTodayResume.Obj(id, "v1"),
				(id, bb) -> {
					var o = new UnitTest.Zeze.Util.TestCacheAppendTodayResume.Obj();
					o.decode(bb);
					return o;
				});
		// 装载足量条目（lruInitialCapacity=6，>3 触发 200ms 热点轮转）。
		for (var i = 0; i < 8; i++)
			Assertions.assertNotNull(cache.get("key" + i));

		var lru = lruFieldOf(cache);
		var queue = lruQueueOf(lru);
		var deadline = System.currentTimeMillis() + 5000;
		while (queue.size() < 3 && System.currentTimeMillis() < deadline) {
			for (var i = 0; i < 8; i++)
				Assertions.assertNotNull(cache.get("key" + i));
			Thread.sleep(20);
		}
		Assertions.assertTrue(queue.size() >= 3, "Cache内部LRU的轮转任务必须在运行，实际节点数: " + queue.size());

		cache.close();
		Assertions.assertTrue(timerCancelled(lru, "newLruHotTimer"), "Cache.close必须级联取消LRU轮转任务");
		Assertions.assertTrue(timerCancelled(lru, "cleanTimer"), "Cache.close必须级联取消LRU清理任务");

		var sizeAfterClose = queue.size();
		Thread.sleep(500);
		Assertions.assertEquals(sizeAfterClose, queue.size(), "Cache.close后内部LRU周期任务不得再执行");
	}

	private static boolean timerCancelled(ConcurrentLruLike<?, ?> lru, String field) throws Exception {
		var f = ConcurrentLruLike.class.getDeclaredField(field);
		f.setAccessible(true);
		return ((java.util.concurrent.Future<?>)f.get(lru)).isCancelled();
	}

	private static ConcurrentLruLike<String, ?> lruFieldOf(Cache cache) throws Exception {
		var f = Cache.class.getDeclaredField("lru");
		f.setAccessible(true);
		@SuppressWarnings("unchecked")
		var lru = (ConcurrentLruLike<String, ?>)f.get(cache);
		return lru;
	}

	@SuppressWarnings("unchecked")
	private static ConcurrentLinkedQueue<ConcurrentHashMap<String, Object>> lruQueueOf(
			ConcurrentLruLike<?, ?> lru) throws Exception {
		var f = ConcurrentLruLike.class.getDeclaredField("lruQueue");
		f.setAccessible(true);
		return (ConcurrentLinkedQueue<ConcurrentHashMap<String, Object>>)(Object)f.get(lru);
	}
}
