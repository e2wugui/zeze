package Zeze.Services;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Application;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Connector;
import Zeze.Net.Service;
import Zeze.Net.TcpSocket;
import Zeze.Services.GlobalCacheManager.Acquire;
import Zeze.Services.GlobalCacheManager.KeepAlive;
import Zeze.Services.GlobalCacheManager.Login;
import Zeze.Services.GlobalCacheManager.ReLogin;
import Zeze.Services.GlobalCacheManager.Reduce;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.GlobalAgent;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.Task;
import demo.Module1.tflush;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static Zeze.Services.GlobalCacheManagerConst.*;

/** Real transport/transaction counterparts of formal/cache-coherence; no external services. */
@Fast
@Timeout(60)
public class TestFormalCacheCoherence {

	private static final AtomicInteger nextId = new AtomicInteger(FastServerIds.TEST_FORMAL_CACHE_COHERENCE);

	private static Object field(Object obj, String name) throws Exception {
		Field f = obj.getClass().getDeclaredField(name);
		f.setAccessible(true);
		return f.get(obj);
	}

	private static void set(Object obj, String name, Object value) throws Exception {
		Field f = obj.getClass().getDeclaredField(name);
		f.setAccessible(true);
		f.set(obj, value);
	}

	private static final class Cluster implements AutoCloseable {
		final GlobalCacheManagerAsyncServer gcm = new GlobalCacheManagerAsyncServer();
		final int id = nextId.getAndIncrement();
		final int port;
		final List<Application> apps = new ArrayList<>();
		final List<RawClient> clients = new ArrayList<>();

		Cluster() throws Exception {
			this(400);
		}

		Cluster(int reduceBudget) throws Exception {
			Task.tryInitThreadPool();
			set(gcm.getGcmConfig(), "maxNetPing", 100);
			set(gcm.getGcmConfig(), "serverProcessTime", reduceBudget - 100);
			gcm.start(InetAddress.getLoopbackAddress(), 0, new Config());
			port = ((TcpSocket)field(gcm, "serverSocket")).getLocalInet().getPort();
		}

		App app() throws Exception {
			var conf = new Config();
			conf.setServerId(nextId.getAndIncrement());
			conf.setServiceManager("disable");
			conf.setDefaultTableConf(new Config.TableConf());
			conf.setCheckpointPeriod(1_000_000);
			conf.setGlobalCacheManagerHostNameOrAddress("127.0.0.1");
			conf.setGlobalCacheManagerPort(port);
			var db = new Config.DatabaseConf();
			db.setDatabaseUrl("formal_cache_coherence_" + id); // ALL apps in this deployment share this backend.
			conf.getDatabaseConfMap().put("", db);
			var app = new Application("FormalCacheCoherence" + conf.getServerId(), conf);
			app.setSchemas(new demo.Schemas());
			var table = new tflush();
			app.addTable("", table);
			apps.add(app);
			app.start();
			return new App(app, table);
		}

		RawClient raw(int serverId) throws Exception {
			var client = new RawClient(port, serverId);
			clients.add(client);
			return client;
		}

		Object holder(int id) throws Exception {
			return ((LongConcurrentHashMap<?>)field(gcm, "sessions")).get(id);
		}

		@Override
		public void close() throws Exception {
			for (var app : apps)
				app.stop();
			for (var client : clients)
				client.stop();
			gcm.stop();
		}
	}

	private record App(Application app, tflush table) {
		void write(int value) {
			Assertions.assertEquals(Procedure.Success, app.newProcedure(() -> {
				table.getOrAdd(1L).setInt_1(value);
				return Procedure.Success;
			}, "FormalCacheCoherence.Write").call());
		}

		int read() {
			var result = new AtomicInteger(-1);
			Assertions.assertEquals(Procedure.Success, app.newProcedure(() -> {
				var bean = table.get(1L);
				result.set(bean == null ? -1 : bean.getInt_1());
				return Procedure.Success;
			}, "FormalCacheCoherence.Read").call());
			return result.get();
		}
	}

	private static final class RawClient extends Service {
		final BlockingQueue<Reduce> reduces = new LinkedBlockingQueue<>();
		final AsyncSocket socket;

		RawClient(int port, int id) throws Exception {
			this(port, id, false);
		}

		RawClient(int port, int id, boolean relogin) throws Exception {
			super("FormalCacheRaw" + id, new Config());
			AddFactoryHandle(Login.TypeId_, new ProtocolFactoryHandle<>(Login::new, null, TransactionLevel.None, DispatchMode.Direct));
			AddFactoryHandle(ReLogin.TypeId_, new ProtocolFactoryHandle<>(ReLogin::new, null, TransactionLevel.None, DispatchMode.Direct));
			AddFactoryHandle(Acquire.TypeId_, new ProtocolFactoryHandle<>(Acquire::new, null, TransactionLevel.None, DispatchMode.Direct));
			AddFactoryHandle(KeepAlive.TypeId_, new ProtocolFactoryHandle<>(KeepAlive::new, null, TransactionLevel.None, DispatchMode.Direct));
			AddFactoryHandle(Reduce.TypeId_, new ProtocolFactoryHandle<>(Reduce::new, rpc -> {
				reduces.add(rpc);
				return 0;
			}, TransactionLevel.None, DispatchMode.Direct));
			var connector = new Connector("127.0.0.1", port, false);
			getConfig().addConnector(connector);
			start();
			socket = connector.WaitReady();
			if (relogin) {
				var login = new ReLogin();
				login.Argument.serverId = id;
				login.SendForWait(socket, 5000).get();
				Assertions.assertEquals(0, login.getResultCode());
			} else {
				var login = new Login();
				login.Argument.serverId = id;
				login.SendForWait(socket, 5000).get();
				Assertions.assertEquals(0, login.getResultCode());
			}
		}

		Acquire acquire(Binary key, int state) throws Exception {
			var rpc = new Acquire(key, state);
			rpc.SendForWait(socket, 5000).get();
			return rpc;
		}

		Reduce reduce() throws Exception {
			return java.util.Objects.requireNonNull(reduces.poll(5, TimeUnit.SECONDS), "Reduce not delivered");
		}
	}

	private static boolean answer(Reduce reduce, int state, long code) {
		reduce.Result.globalKey = reduce.Argument.globalKey;
		reduce.Result.state = state;
		return reduce.trySendResultCode(code);
	}

	@Test
	public void testModifyHandoffFlushesBeforeOtherApplicationLoads() throws Exception {
		try (var cluster = new Cluster()) {
			var a = cluster.app();
			var b = cluster.app();
			a.write(11);
			Assertions.assertEquals(11, b.read());
			b.write(22);
			Assertions.assertEquals(22, a.read());
			a.write(33);
			Assertions.assertEquals(33, b.read());
		}
	}

	@Test
	public void testErrorReduceCannotAuthorizeConflictingWriter() throws Exception {
		try (var cluster = new Cluster()) {
			var a = cluster.raw(cluster.id);
			var b = cluster.raw(cluster.id + 100_000); // session-only id; no Application/cache directory.
			var key = new Binary(new byte[]{11});
			Assertions.assertEquals(0, a.acquire(key, StateModify).getResultCode());
			var request = new Acquire(key, StateModify);
			var future = request.SendForWait(b.socket, 5000);
			answer(a.reduce(), StateInvalid, ReduceErrorState);
			future.get();
			Assertions.assertEquals(AcquireModifyFailed, request.getResultCode());
			Assertions.assertEquals(StateInvalid, request.Result.state);
		}
	}

	@Test
	public void testReduceTimeoutAndLateDuplicateResponseDoNotGrant() throws Exception {
		try (var cluster = new Cluster()) {
			var a = cluster.raw(cluster.id);
			var b = cluster.raw(cluster.id + 100_000);
			var key = new Binary(new byte[]{12});
			Assertions.assertEquals(0, a.acquire(key, StateModify).getResultCode());
			var request = new Acquire(key, StateShare);
			var future = request.SendForWait(b.socket, 5000);
			var reduce = a.reduce();
			future.get(); // true server Reduce timeout; A has not relinquished anything.
			Assertions.assertEquals(AcquireShareFailed, request.getResultCode());
			Assertions.assertTrue(answer(reduce, StateInvalid, 0));
			Assertions.assertFalse(answer(reduce, StateInvalid, 0), "RPC CAS prevents sending the same result twice");
			new KeepAlive().SendForWait(a.socket, 5000).get(); // TCP round trip drains the stale reply.
			Assertions.assertEquals(AcquireModifyFailed, b.acquire(key, StateModify).getResultCode());
		}
	}

	@Test
	public void testAcquireTimeoutDiscardsLateSuccess() throws Exception {
		// Timer wheel granularity can coalesce 40ms and 400ms timeouts; keep the
		// server Reduce deadline well beyond the client timeout, so this is a late SUCCESS.
		try (var cluster = new Cluster(10_000)) {
			var a = cluster.raw(cluster.id);
			var b = cluster.raw(cluster.id + 100_000);
			var key = new Binary(new byte[]{13});
			a.acquire(key, StateModify);
			var rpc = new Acquire(key, StateModify);
			var callbacks = new AtomicInteger();
			var done = new java.util.concurrent.CountDownLatch(1);
			rpc.Send(b.socket, response -> {
				callbacks.incrementAndGet();
				done.countDown();
				return 0;
			}, 40);
			var reduce = a.reduce();
			Assertions.assertTrue(done.await(5, TimeUnit.SECONDS));
			Assertions.assertTrue(rpc.isTimeout());
			answer(reduce, StateInvalid, 0);
			// A new Acquire is serialized behind the old request; its response is a transport barrier.
			Assertions.assertEquals(AcquireModifyAlreadyIsModify, b.acquire(key, StateModify).getResultCode(),
					"the timed-out request must really have succeeded in the GCM ledger");
			Assertions.assertTrue(rpc.isTimeout(), "late success must not revive removed RPC context");
			Assertions.assertEquals(1, callbacks.get());
		}
	}

	@Test
	public void testDelayedOldCleanupDoesNotRevokeReLoginPermission() throws Exception {
		try (var cluster = new Cluster()) {
			var a = cluster.raw(cluster.id);
			var key = new Binary(new byte[]{14});
			a.acquire(key, StateModify);
			var holder = cluster.holder(cluster.id);
			long generation = (long)field(holder, "generation");
			a.socket.close();
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
			while ((long)field(holder, "sessionId") != 0 && System.nanoTime() < deadline)
				Thread.sleep(5);
			Assertions.assertEquals(0L, field(holder, "sessionId"));
			var a2 = new RawClient(cluster.port, cluster.id, true);
			cluster.clients.add(a2);
			var completed = new java.util.concurrent.CountDownLatch(1);
			var future = new GlobalCacheManagerAsyncServer.CountDownFuture();
			future.createOne();
			future.then(ignored -> completed.countDown());
			var release = GlobalCacheManagerAsyncServer.class.getDeclaredMethod("releaseAsync",
					holder.getClass(), Binary.class, GlobalCacheManagerAsyncServer.CountDownFuture.class, long.class);
			release.setAccessible(true);
			// Delay the real cleanup entry with its original captured token until after real ReLogin.
			release.invoke(cluster.gcm, holder, key, future, generation);
			Assertions.assertTrue(completed.await(5, TimeUnit.SECONDS));
			Assertions.assertTrue(((java.util.Map<?, ?>)field(holder, "acquired")).containsKey(key));
			Assertions.assertEquals(AcquireModifyAlreadyIsModify, a2.acquire(key, StateModify).getResultCode());
			var b = cluster.raw(cluster.id + 100_000);
			var acquire = new Acquire(key, StateModify);
			var result = acquire.SendForWait(b.socket, 5000);
			answer(a2.reduce(), StateInvalid, ReduceErrorState);
			result.get();
			Assertions.assertEquals(AcquireModifyFailed, acquire.getResultCode());
		}
	}

	@Test
	public void testReLoginPreservesLiveCachedPermission() throws Exception {
		try (var cluster = new Cluster()) {
			var a = cluster.app();
			a.write(41);
			var agent = ((GlobalAgent)a.app.getGlobalAgent()).getAgents()[0];
			long logins = agent.getLoginTimes().get();
			agent.connect().close();
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
			while (agent.getLoginTimes().get() == logins && System.nanoTime() < deadline)
				Thread.sleep(10);
			Assertions.assertTrue(agent.getLoginTimes().get() > logins);
			Assertions.assertEquals(41, a.read());
			Assertions.assertEquals(41, cluster.app().read());
		}
	}

	@Test
	public void testCheckpointBeforeHaltExceedsGlobalReclaimMargin() throws Exception {
		var config = new AchillesHeelConfig(100, 300, 60_000);
		long margin = config.globalDaemonTimeout - config.serverDaemonTimeout - config.serverReleaseTimeout;
		var deadlineField = Application.class.getDeclaredField("HALT_DEADLINE_MS");
		deadlineField.setAccessible(true);
		long deadline = deadlineField.getLong(null);
		Assertions.assertTrue(deadline > margin, "witness: current halt grace exceeds GCM safety margin");
		var entered = new java.util.concurrent.CountDownLatch(1);
		var release = new java.util.concurrent.CountDownLatch(1);
		var halted = new java.util.concurrent.CountDownLatch(1);
		var helper = Application.class.getDeclaredMethod("haltWithDeadline", long.class,
				Zeze.Util.Action0.class, Runnable.class);
		helper.setAccessible(true);
		try (var worker = java.util.concurrent.Executors.newSingleThreadExecutor()) {
			var future = worker.submit(() -> {
				try {
					helper.invoke(null, deadline, (Zeze.Util.Action0)() -> {
						entered.countDown();
						release.await(); // real helper, simulated stuck checkpoint, mock halt only.
					}, (Runnable)halted::countDown);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
			try {
				Assertions.assertTrue(entered.await(5, TimeUnit.SECONDS));
				Assertions.assertFalse(halted.await(margin + 200, TimeUnit.MILLISECONDS),
						"GCM's entire safety margin has elapsed while helper still permits old process to live");
			} finally {
				release.countDown();
			}
			future.get(5, TimeUnit.SECONDS);
		}
		Assertions.assertEquals(0, halted.getCount());
		System.out.println("S6_HALT_BUDGET_WITNESS margin=" + margin + " haltGrace=" + deadline);
	}

	@Test
	public void testExpiredWatchdogRecoveryHasNoAutomaticPermissionFence() throws Exception {
		// Fault-injection witness, NOT a normal-operation safety assertion: stop A's
		// watchdog to represent a process suspension beyond the global reclaim deadline.
		// Advance only GCM's observation via its timestamp, invoke the REAL daemon, then
		// let A resume/reconnect. This records the timing precondition the protocol needs.
		try (var cluster = new Cluster()) {
			var a = cluster.app();
			var b = cluster.app();
			a.write(51);
			a.app.getAchillesHeelDaemon().stopAndJoin();
			var holder = cluster.holder(a.app.getConfig().getServerId());
			set(holder, "activeTime", System.currentTimeMillis() - 1_000_000L);
			var daemon = GlobalCacheManagerAsyncServer.class.getDeclaredMethod("achillesHeelDaemon");
			daemon.setAccessible(true);
			daemon.invoke(cluster.gcm);
			// Wait for true asynchronous release to finish, then obtain B's real Modify.
			var acquired = (java.util.Map<?, ?>)field(holder, "acquired");
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
			while (!acquired.isEmpty() && System.nanoTime() < deadline)
				Thread.sleep(5);
			Assertions.assertTrue(acquired.isEmpty());
			b.write(52);
			// Wait until A's REAL GlobalClient completes ReLogin. No permission revalidation occurs.
			var agent = ((GlobalAgent)a.app.getGlobalAgent()).getAgents()[0];
			deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
			while (agent.getLoginTimes().get() < 2 && System.nanoTime() < deadline)
				Thread.sleep(10);
			Assertions.assertTrue(agent.getLoginTimes().get() >= 2);
			a.write(53); // succeeds using A's stale local Modify, even with a healthy connection.
			Assertions.assertEquals(53, a.read());
			Assertions.assertEquals(52, b.read(), "separate real caches now accept conflicting successful transactions");
			System.out.println("S6_PAUSE_WITNESS A=53 B=52, both successful, ReLogin=" + agent.getLoginTimes().get());
		}
	}
}
