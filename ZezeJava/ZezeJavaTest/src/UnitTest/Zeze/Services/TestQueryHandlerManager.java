package UnitTest.Zeze.Services;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import Zeze.Services.Log4jQuery.handler.QueryHandlerManager;
import harness.Fast;

/**
 * S-6：QueryHandlerManager 并发首查。
 * <p>
 * 原实现为非同步懒初始化（非volatile initFinish + 并发 put 同一 HashMap），
 * 修复为静态块初始化（类初始化锁保证恰好一次且安全发布）。
 * 本测试为并发回归守卫：多个线程同时首次触达，全部得到完整一致的注册表。
 * 说明：原缺陷为竞态（少量条目时难以确定性复现红），此用例防回归并固化修复语义。
 */
@Fast
public class TestQueryHandlerManager {
	@Test
	public void testConcurrentFirstInvoke() throws Exception {
		final int threads = 16;
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		try {
			var tasks = new ArrayList<Callable<String>>();
			for (int i = 0; i < threads; ++i)
				tasks.add(() -> QueryHandlerManager.invokeHandler("{\"cmd\":\"cmd_list\"}"));
			var results = new ArrayList<Future<String>>();
			for (var task : tasks)
				results.add(pool.submit(task));

			String first = null;
			for (var future : results) {
				var json = future.get();
				Assertions.assertNotNull(json);
				Assertions.assertFalse(json.isEmpty(), "cmd_list handler must be registered and return list");
				Assertions.assertTrue(json.contains("cmd_list"), "result must contain cmd_list itself");
				if (first == null)
					first = json;
				else
					Assertions.assertEquals(first, json, "all threads must see the same registry snapshot");
			}
		} finally {
			pool.shutdownNow();
		}
	}

	@Test
	public void testUnknownCmdReturnsEmpty() throws Exception {
		Assertions.assertEquals("", QueryHandlerManager.invokeHandler("{\"cmd\":\"__no_such_cmd__\"}"));
	}
}
