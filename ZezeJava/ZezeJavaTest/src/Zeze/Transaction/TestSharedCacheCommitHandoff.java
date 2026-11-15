package Zeze.Transaction;

import java.net.InetAddress;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.Application;
import Zeze.Config;
import Zeze.Net.TcpSocket;
import Zeze.Services.GlobalCacheManagerAsyncServer;
import Zeze.Util.Task;
import demo.Module1.BValue;
import demo.Module1.tflush;
import harness.Fast;
import harness.FastServerIds;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** formal/integration：真实事务、Reduce、共享后端及重新加载的组合对照。 */
@Fast
@Timeout(90)
public class TestSharedCacheCommitHandoff {

	private static final AtomicInteger nextId = new AtomicInteger(FastServerIds.TEST_FORMAL_INTEGRATION);

	private static final class Node implements AutoCloseable {
		final Application app;
		final tflush table = new tflush();

		Node(int id, int port, String databaseUrl) throws Exception {
			var config = new Config();
			config.setServiceManager("disable");
			config.setServerId(id);
			config.setDefaultTableConf(new Config.TableConf());
			config.setGlobalCacheManagerHostNameOrAddress("127.0.0.1");
			config.setGlobalCacheManagerPort(port);
			config.setCheckpointMode(CheckpointMode.Table);
			config.setCheckpointPeriod(3_600_000);
			var database = new Config.DatabaseConf();
			database.setDatabaseType(Config.DbType.Memory);
			database.setDatabaseUrl(databaseUrl);
			config.getDatabaseConfMap().put("", database);
			app = new Application("SharedCacheHandoff" + id, config);
			app.setSchemas(new demo.Schemas());
			app.addTable("", table);
			app.start();
		}

		void write(int... values) {
			Assertions.assertEquals(Procedure.Success, app.newProcedure(() -> {
				for (int i = 0; i < values.length; ++i)
					table.getOrAdd((long)i + 1).setInt_1(values[i]);
				return Procedure.Success;
			}, "SharedCacheHandoff.Write").call());
		}

		int read(long key) {
			var answer = new AtomicInteger(-1);
			Assertions.assertEquals(Procedure.Success, app.newProcedure(() -> {
				var value = table.get(key);
				int snapshot = value != null ? value.getInt_1() : -1;
				Transaction.whileCommit(() -> answer.set(snapshot));
				return Procedure.Success;
			}, "SharedCacheHandoff.Read").call());
			return answer.get();
		}

		@Nullable BValue stored(long key) {
			TableX<Long, BValue> base = table;
			return Objects.requireNonNull(base.getStorage()).getDatabaseTable().find(table, key);
		}

		@Override
		public void close() throws Exception {
			app.stop();
		}
	}

	private static final class Deployment implements AutoCloseable {
		final GlobalCacheManagerAsyncServer gcm = new GlobalCacheManagerAsyncServer();
		final Node a;
		final Node b;

		Deployment() throws Exception {
			Task.tryInitThreadPool();
			gcm.start(InetAddress.getLoopbackAddress(), 0, new Config());
			try {
				var field = GlobalCacheManagerAsyncServer.class.getDeclaredField("serverSocket");
				field.setAccessible(true);
				int port = Objects.requireNonNull(((TcpSocket)field.get(gcm)).getLocalInet()).getPort();
				int id = nextId.getAndAdd(2);
				String databaseUrl = "shared_cache_handoff_" + id;
				a = new Node(id, port, databaseUrl);
				try {
					b = new Node(id + 1, port, databaseUrl);
				} catch (Throwable e) {
					a.close();
					throw e;
				}
			} catch (Throwable e) {
				gcm.stop();
				throw e;
			}
		}

		@Override
		public void close() throws Exception {
			try { b.close(); }
			finally {
				try { a.close(); }
				finally { gcm.stop(); }
			}
		}
	}

	@Test
	public void handoffFlushesBeforeLoadingAndCannotReplayOldDirtyValue() throws Exception {
		try (var d = new Deployment()) {
			d.a.write(11);
			Assertions.assertNull(d.a.stored(1L), "异步提交此时只在 A 内存中");
			Assertions.assertEquals(11, d.b.read(1L), "B 获权前必须刷出 A 的已提交值");
			Assertions.assertEquals(11, Objects.requireNonNull(d.a.stored(1L)).getInt_1());
			d.b.write(22);
			Assertions.assertEquals(11, Objects.requireNonNull(d.a.stored(1L)).getInt_1(),
					"B 的新值尚未被后台刷出");
			Objects.requireNonNull(d.a.app.getCheckpoint()).runOnce();
			Assertions.assertEquals(11, Objects.requireNonNull(d.a.stored(1L)).getInt_1(),
					"A 旧脏集合不能再次回写");
			Assertions.assertEquals(22, d.a.read(1L), "A 旧缓存必须失效后重新加载 B 的值");
			Assertions.assertEquals(22, Objects.requireNonNull(d.b.stored(1L)).getInt_1());
		}
	}

	@Test
	public void remoteCommitBetweenReadsInvalidatesTheWholeAttempt() throws Exception {
		try (var d = new Deployment(); var worker = Executors.newSingleThreadExecutor()) {
			d.a.write(0, 0);
			var firstRead = new CountDownLatch(1);
			var writerDone = new CountDownLatch(1);
			var attempts = new AtomicInteger();
			var committed = new AtomicReference<int[]>();
			var reader = worker.submit(() -> d.a.app.newProcedure(() -> {
				int round = attempts.incrementAndGet();
				int first = Objects.requireNonNull(d.a.table.get(1L)).getInt_1();
				if (round == 1) {
					firstRead.countDown();
					Assertions.assertTrue(writerDone.await(15, TimeUnit.SECONDS));
				}
				int second = Objects.requireNonNull(d.a.table.get(2L)).getInt_1();
				Transaction.whileCommit(() -> committed.set(new int[]{first, second}));
				return Procedure.Success;
			}, "SharedCacheHandoff.Reader").call());
			try {
				Assertions.assertTrue(firstRead.await(15, TimeUnit.SECONDS));
				d.b.write(1, 1);
			} finally { writerDone.countDown(); }
			Assertions.assertEquals(Procedure.Success, reader.get(30, TimeUnit.SECONDS));
			Assertions.assertTrue(attempts.get() >= 2);
			Assertions.assertArrayEquals(new int[]{1, 1}, committed.get());
		}
	}
}
