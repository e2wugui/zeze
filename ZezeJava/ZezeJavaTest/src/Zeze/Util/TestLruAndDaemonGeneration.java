package Zeze.Util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestLruAndDaemonGeneration {
	private static void invoke(Object object, String name) throws Exception {
		var method = object.getClass().getDeclaredMethod(name);
		method.setAccessible(true);
		method.invoke(object);
	}

	@Test
	@Timeout(20)
	public void insertionSurvivesHotNodeRetirementAndRemainsEvictable() throws Exception {
		Task.tryInitThreadPool();
		var lru = new ConcurrentLruLike<Integer, Integer>("retirement", 1, null, 60_000, 60_000);
		var factoryEntered = new CountDownLatch(1);
		var resume = new CountDownLatch(1);
		try (var pool = Executors.newSingleThreadExecutor()) {
			lru.getOrAdd(1, () -> 1);
			lru.getOrAdd(2, () -> 2);
			// 用工厂内的屏障控制暂停时序，模拟读取热点后发生的线程调度暂停。
			var insert = pool.submit(() -> lru.getOrAdd(42, () -> {
				factoryEntered.countDown();
				try {
					if (!resume.await(5, TimeUnit.SECONDS))
						throw new IllegalStateException("test barrier timed out");
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new IllegalStateException(e);
				}
				return 42;
			}));
			assertTrue(factoryEntered.await(5, TimeUnit.SECONDS));
			invoke(lru, "newLruHot");
			invoke(lru, "cleanNow");
			resume.countDown();
			assertEquals(42, insert.get(5, TimeUnit.SECONDS));
			lru.getOrAdd(43, () -> 43);
			invoke(lru, "newLruHot");
			invoke(lru, "cleanNow");
			assertNull(lru.get(42, false), "登记完成的新条目必须仍受容量清理管理");
		} finally {
			resume.countDown();
			lru.close();
		}
	}


}
