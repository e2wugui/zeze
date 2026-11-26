package Zeze.Services;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;

/**
 * 验证 Token 存储读写锁并发语义、并发冷回载一次性核销互斥以及 RocksDB 分批清理游标续扫与停机响应。
 */
@Fast
@ResourceLock("token.rocksdb")
public class TestTokenStoreConcurrencyAndBatchClean {
	private static int freePort() throws Exception {
		try (var s = new ServerSocket()) {
			s.bind(new InetSocketAddress("127.0.0.1", 0));
			return s.getLocalPort();
		}
	}

	private static byte[] encodeTokenState(Token token, String key, long createTime, long endTime) throws Exception {
		Class<?> stateClass = Class.forName("Zeze.Services.Token$TokenState");
		Constructor<?> ctor = stateClass.getDeclaredConstructor(
				Token.class, String.class, SocketAddress.class, Binary.class, long.class, long.class);
		ctor.setAccessible(true);
		Object state = ctor.newInstance(token, key, null, new Binary("ctx-" + key), createTime, endTime);
		Method encode = stateClass.getDeclaredMethod("encode", ByteBuffer.class);
		encode.setAccessible(true);
		ByteBuffer bb = (ByteBuffer)encode.invoke(state, (Object)null);
		return bb.Copy();
	}

	private static RocksDatabase.Table getTokenMapTable(Token token) throws Exception {
		Field f = Token.class.getDeclaredField("tokenMapTable");
		f.setAccessible(true);
		var table = (RocksDatabase.Table)f.get(token);
		Assertions.assertNotNull(table);
		return table;
	}

	private static ReentrantReadWriteLock getTokenStoreLock(Token token) throws Exception {
		Field f = Token.class.getDeclaredField("tokenStoreLock");
		f.setAccessible(true);
		return (ReentrantReadWriteLock)f.get(token);
	}

	private static Set<String> readAllDbKeys(RocksDatabase.Table table) {
		var keys = new HashSet<String>();
		try (var it = table.iterator()) {
			for (it.seekToFirst(); RocksDatabase.isValidOrThrow(it); it.next())
				keys.add(new String(it.key(), StandardCharsets.UTF_8));
		}
		return keys;
	}

	@Test
	@Timeout(30)
	public void testBatchCleanCursorHandlesAllBoundaries(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		System.setProperty("token.rocksdb", tempDir.resolve("token_db").toString());
		var token = new Token();
		try {
			token.start(null, "127.0.0.1", freePort());
			var table = getTokenMapTable(token);
			long now = System.currentTimeMillis();

			// 覆盖 batchSize=1、batchSize=3（整除/非整除）及跨越默认 1000 批大小：
			// 包含批尾删除、批尾保留、交替过期/未过期等全部续扫边界。
			var expectedLive = new HashSet<String>();
			for (int i = 0; i < 25; i++) {
				String key = String.format("small-%04d", i);
				boolean expired = (i % 3 != 1); // 0(删),1(留),2(删),3(删),4(留)...
				long endTime = expired ? now - 60_000 : now + 600_000;
				table.put(key.getBytes(StandardCharsets.UTF_8), encodeTokenState(token, key, now - 120_000, endTime));
				if (!expired)
					expectedLive.add(key);
			}
			// batchSize=1 逐条推进，验证每条删除/保留后的 resumeKey 续扫都不漏不死循环
			token.cleanTokenMapTableOnce(1);
			Assertions.assertEquals(expectedLive, readAllDbKeys(table), "batchSize=1 清理结果必须精确");

			// 写入恰好 9 条（整除 batchSize=3）再验证整批边界不漏不重
			for (int i = 0; i < 9; i++) {
				String key = String.format("exact-%04d", i);
				boolean expired = (i % 2 == 0);
				long endTime = expired ? now - 60_000 : now + 600_000;
				table.put(key.getBytes(StandardCharsets.UTF_8), encodeTokenState(token, key, now - 120_000, endTime));
				if (!expired)
					expectedLive.add(key);
			}
			token.cleanTokenMapTableOnce(3);
			Assertions.assertEquals(expectedLive, readAllDbKeys(table), "batchSize=3 整批清理结果必须精确");

			// 写入跨越默认 CLEAN_TABLE_BATCH_SIZE(1000) 的 2305 条记录，验证默认多批清理
			for (int i = 0; i < 2305; i++) {
				String key = String.format("bulk-%05d", i);
				boolean expired = (i % 5 != 0);
				long endTime = expired ? now - 60_000 : now + 600_000;
				table.put(key.getBytes(StandardCharsets.UTF_8), encodeTokenState(token, key, now - 120_000, endTime));
				if (!expired)
					expectedLive.add(key);
			}
			token.cleanTokenMapTableOnce(Token.CLEAN_TABLE_BATCH_SIZE);
			Assertions.assertEquals(expectedLive, readAllDbKeys(table), "默认1000批大小多批清理必须精确");
		} finally {
			try {
				token.stop();
			} catch (Exception ignored) {
			}
			token.closeDb();
			System.clearProperty("token.rocksdb");
		}
	}

	@Test
	@Timeout(30)
	public void testReadLockAllowsConcurrentNewAndGetDuringClean(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		System.setProperty("token.rocksdb", tempDir.resolve("token_db").toString());
		int port = freePort();
		var token = new Token();
		try {
			token.start(null, "127.0.0.1", port);
			var client = new Token.TokenClient(null).start("127.0.0.1", port);
			try {
				client.waitReady();
				var readLock = getTokenStoreLock(token).readLock();

				// 模拟清理线程正持有读锁：IO 线程上的 NewToken 与 GetToken 必须能并发完成而非冻结
				readLock.lock();
				try {
					String issued = client.newToken(new Binary("rw-concurrent"), 60_000)
							.get(5, TimeUnit.SECONDS).getToken();
					Assertions.assertEquals(24, issued.length());

					var res = client.getToken(issued, 1).get(5, TimeUnit.SECONDS);
					Assertions.assertTrue(res.getTime() >= 0);
					Assertions.assertEquals(1, res.getCount());
					Assertions.assertEquals("rw-concurrent", res.getContext().toString(StandardCharsets.UTF_8));
				} finally {
					readLock.unlock();
				}

				// 真实并发交错：后台多批清理大量过期行，前台同步高频签发与核销（含冷回载有效行）
				var table = getTokenMapTable(token);
				long now = System.currentTimeMillis();
				var coldLiveTokens = new ArrayList<String>();
				for (int i = 0; i < 1500; i++) {
					String key = String.format("mix-exp-%05d", i);
					table.put(key.getBytes(StandardCharsets.UTF_8),
							encodeTokenState(token, key, now - 120_000, now - 60_000));
				}
				for (int i = 0; i < 50; i++) {
					String key = String.format("mix-live-%04d", i);
					table.put(key.getBytes(StandardCharsets.UTF_8),
							encodeTokenState(token, key, now - 10_000, now + 600_000));
					coldLiveTokens.add(key);
				}

				var cleanFailed = new AtomicBoolean();
				var cleaner = new Thread(() -> {
					try {
						token.cleanTokenMapTableOnce(16);
					} catch (Throwable t) {
						cleanFailed.set(true);
					}
				}, "test-token-concurrent-cleaner");
				cleaner.start();

				for (int i = 0; i < 100; i++) {
					String issued = client.newToken(new Binary("hot-" + i), 60_000)
							.get(5, TimeUnit.SECONDS).getToken();
					var res = client.getToken(issued, 1).get(5, TimeUnit.SECONDS);
					Assertions.assertTrue(res.getTime() >= 0);
					Assertions.assertEquals("hot-" + i, res.getContext().toString(StandardCharsets.UTF_8));
				}
				for (String coldKey : coldLiveTokens) {
					var res = client.getToken(coldKey, 1).get(5, TimeUnit.SECONDS);
					Assertions.assertTrue(res.getTime() >= 0, "有效冷 token 不得被并发清理误删: " + coldKey);
					Assertions.assertEquals("ctx-" + coldKey, res.getContext().toString(StandardCharsets.UTF_8));
				}

				cleaner.join(15_000);
				Assertions.assertFalse(cleaner.isAlive(), "后台分批清理必须按时完成");
				Assertions.assertFalse(cleanFailed.get(), "后台分批清理不得抛异常");
				Assertions.assertTrue(readAllDbKeys(table).isEmpty(),
						"全部过期行应已清理且已核销冷行应已删除");
			} finally {
				client.stop();
			}
		} finally {
			try {
				token.stop();
			} catch (Exception ignored) {
			}
			token.closeDb();
			System.clearProperty("token.rocksdb");
		}
	}

	@Test
	@Timeout(60)
	public void testConcurrentColdLoadConsumeDoesNotReplay(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		System.setProperty("token.rocksdb", tempDir.resolve("token_db").toString());
		int port = freePort();
		var tokenServer = new Token();
		final int singleUseCount = 30;
		final int doubleUseCount = 15;
		var singleTokens = new ArrayList<String>(singleUseCount);
		var doubleTokens = new ArrayList<String>(doubleUseCount);

		// 第一阶段：签发 token 后 stop() 落库并清空内存 tokenMap
		tokenServer.start(null, "127.0.0.1", port);
		try {
			var client = new Token.TokenClient(null).start("127.0.0.1", port);
			try {
				client.waitReady();
				for (int i = 0; i < singleUseCount; i++)
					singleTokens.add(client.newToken(new Binary("one-" + i), 600_000).get(5, TimeUnit.SECONDS).getToken());
				for (int i = 0; i < doubleUseCount; i++)
					doubleTokens.add(client.newToken(new Binary("two-" + i), 600_000).get(5, TimeUnit.SECONDS).getToken());
			} finally {
				client.stop();
			}
		} finally {
			tokenServer.stop();
			tokenServer.closeDb();
		}

		// 第二阶段：重启后所有 token 仅存于 RocksDB（冷态），多客户端并发抢同一冷 token 核销
		final int concurrency = 6;
		tokenServer.start(null, "127.0.0.1", port);
		var clients = new ArrayList<Token.TokenClient>(concurrency);
		ExecutorService pool = Executors.newFixedThreadPool(concurrency);
		try {
			for (int i = 0; i < concurrency; i++) {
				var c = new Token.TokenClient(null).start("127.0.0.1", port);
				c.waitReady();
				clients.add(c);
			}

			for (String t : singleTokens) {
				var barrier = new CyclicBarrier(concurrency);
				List<Future<Long>> futures = new ArrayList<>(concurrency);
				for (int i = 0; i < concurrency; i++) {
					var c = clients.get(i);
					futures.add(pool.submit(() -> {
						barrier.await(5, TimeUnit.SECONDS);
						var res = c.getToken(t, 1).get(10, TimeUnit.SECONDS);
						return res.getTime() >= 0 ? res.getCount() : -1L;
					}));
				}
				var successCounts = new ArrayList<Long>();
				for (var f : futures) {
					long cnt = f.get(15, TimeUnit.SECONDS);
					if (cnt > 0)
						successCounts.add(cnt);
				}
				Assertions.assertEquals(List.of(1L), successCounts,
						"并发冷回载一次性 token 必须恰核销一次，禁止重放: " + t);
			}

			for (String t : doubleTokens) {
				var barrier = new CyclicBarrier(concurrency);
				List<Future<Long>> futures = new ArrayList<>(concurrency);
				for (int i = 0; i < concurrency; i++) {
					var c = clients.get(i);
					futures.add(pool.submit(() -> {
						barrier.await(5, TimeUnit.SECONDS);
						var res = c.getToken(t, 2).get(10, TimeUnit.SECONDS);
						return res.getTime() >= 0 ? res.getCount() : -1L;
					}));
				}
				var successCounts = new ArrayList<Long>();
				for (var f : futures) {
					long cnt = f.get(15, TimeUnit.SECONDS);
					if (cnt > 0)
						successCounts.add(cnt);
				}
				Collections.sort(successCounts);
				Assertions.assertEquals(List.of(1L, 2L), successCounts,
						"并发冷回载 maxCount=2 token 必须恰核销两次且计数为 [1, 2]: " + t);
			}
		} finally {
			pool.shutdownNow();
			for (var c : clients) {
				try {
					c.stop();
				} catch (Exception ignored) {
				}
			}
			try {
				tokenServer.stop();
			} catch (Exception ignored) {
			}
			tokenServer.closeDb();
			System.clearProperty("token.rocksdb");
		}
	}

	@Test
	@Timeout(30)
	public void testBatchCleanYieldsToStopAndPreservesInterrupt(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		System.setProperty("token.rocksdb", tempDir.resolve("token_db").toString());
		var token = new Token();
		try {
			token.start(null, "127.0.0.1", freePort());
			var table = getTokenMapTable(token);
			long now = System.currentTimeMillis();
			for (int i = 0; i < 4000; i++) {
				String key = String.format("stop-%05d", i);
				table.put(key.getBytes(StandardCharsets.UTF_8),
						encodeTokenState(token, key, now - 120_000, now - 60_000));
			}

			// 1. 已中断线程调用清理应立即停止且保留中断标志
			Thread.currentThread().interrupt();
			try {
				token.cleanTokenMapTableOnce(10);
				Assertions.assertTrue(Thread.currentThread().isInterrupted(), "清理不得吞掉线程中断标志");
			} finally {
				Thread.interrupted(); // 复位中断标志以免影响后续清理
			}
			Assertions.assertEquals(4000, readAllDbKeys(table).size(), "已中断时应跳过清理");

			// 2. 清理运行中并发 stop() 应能快速收敛不阻塞
			var started = new CountDownLatch(1);
			var cleanFailed = new AtomicBoolean();
			var cleaner = new Thread(() -> {
				started.countDown();
				try {
					token.cleanTokenMapTableOnce(10);
				} catch (Throwable t) {
					cleanFailed.set(true);
				}
			}, "test-token-batch-cleaner");
			cleaner.start();
			Assertions.assertTrue(started.await(5, TimeUnit.SECONDS));
			token.stop();
			cleaner.join(10_000);
			Assertions.assertFalse(cleaner.isAlive(), "stop() 后分批清理线程必须在当前批边界快速退出");
			Assertions.assertFalse(cleanFailed.get(), "停机期间分批清理不得抛未捕获异常");
		} finally {
			try {
				token.stop();
			} catch (Exception ignored) {
			}
			token.closeDb();
			System.clearProperty("token.rocksdb");
		}
	}
}
