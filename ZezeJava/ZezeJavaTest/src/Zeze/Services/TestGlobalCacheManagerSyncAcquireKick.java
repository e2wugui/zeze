package Zeze.Services;

import static java.util.Objects.requireNonNull;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Connector;
import Zeze.Net.Service;
import Zeze.Services.GlobalCacheManager.Acquire;
import Zeze.Services.GlobalCacheManager.Login;
import Zeze.Services.GlobalCacheManager.Reduce;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.IdentityHashSet;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.Task;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

/**
 * 同步版 GlobalCacheManagerServer 在 processAcquireRequest 捕获 sender 之后、进入
 * acquireShare/acquireModify 之前发生并发 kick/unbind（清空 socket.userState）时，
 * 必须复用入口单次捕获的 sender，不得重读 getUserState() 触发 NPE 撕裂 CacheState 不变式
 * （modify != null && !share.isEmpty() 或 modify 与 acquired 失配）。
 */
@Fast
@ResourceLock(value = "GlobalCacheManagerServer.instance", mode = ResourceAccessMode.READ_WRITE)
public class TestGlobalCacheManagerSyncAcquireKick {
	private static int port;
	private static @Nullable GlobalCacheManagerServer gcm;

	private static final class RawClient extends Service {
		private final BlockingQueue<Reduce> reduces = new LinkedBlockingQueue<>();

		RawClient(String name) {
			super(name, new Zeze.Config());
			AddFactoryHandle(Login.TypeId_, new ProtocolFactoryHandle<>(
					Login::new, null, TransactionLevel.None, DispatchMode.Direct));
			AddFactoryHandle(Acquire.TypeId_, new ProtocolFactoryHandle<>(
					Acquire::new, null, TransactionLevel.None, DispatchMode.Direct));
			AddFactoryHandle(Reduce.TypeId_, new ProtocolFactoryHandle<>(
					Reduce::new, this::processReduce, TransactionLevel.None, DispatchMode.Direct));
		}

		private long processReduce(Reduce rpc) {
			reduces.add(rpc);
			return 0;
		}

		AsyncSocket connect() throws Exception {
			var connector = new Connector("127.0.0.1", port, false);
			getConfig().addConnector(connector);
			start();
			return connector.WaitReady();
		}
	}

	@BeforeAll
	public static void setUp() throws Exception {
		Task.tryInitThreadPool();
		try (var s = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
			port = s.getLocalPort();
		}
		gcm = GlobalCacheManagerServer.getInstance();
		gcm.start(InetAddress.getLoopbackAddress(), port, null);
	}

	@AfterAll
	public static void tearDown() throws Exception {
		if (gcm != null) {
			gcm.stop();
			gcm = null;
		}
	}

	private static void login(AsyncSocket socket, int serverId) throws Exception {
		var login = new Login();
		login.Argument.serverId = serverId;
		login.Argument.globalCacheManagerHashIndex = 0;
		Assertions.assertTrue(login.SendForWait(socket, 10_000).await(10_000), "login await");
		Assertions.assertFalse(login.isTimeout(), "login timeout");
		Assertions.assertEquals(0, login.getResultCode(), "login resultCode");
	}

	private static Object sessionOf(int serverId) throws Exception {
		var sessionsField = GlobalCacheManagerServer.class.getDeclaredField("sessions");
		sessionsField.setAccessible(true);
		var sessions = (LongConcurrentHashMap<?>)sessionsField.get(gcm);
		var holder = requireNonNull(sessions).get(serverId);
		Assertions.assertNotNull(holder, "session must exist, serverId=" + serverId);
		return holder;
	}

	private static AsyncSocket serverSocketOf(int serverId) throws Exception {
		var holder = sessionOf(serverId);
		var sessionIdField = holder.getClass().getDeclaredField("sessionId");
		sessionIdField.setAccessible(true);
		long sessionId = sessionIdField.getLong(holder);
		var serverField = GlobalCacheManagerServer.class.getDeclaredField("server");
		serverField.setAccessible(true);
		var server = (Service)requireNonNull(serverField.get(gcm));
		var socket = server.GetSocket(sessionId);
		Assertions.assertNotNull(socket, "server socket must exist, serverId=" + serverId);
		return socket;
	}

	private static @Nullable Object cacheStateOf(Binary key) throws Exception {
		var field = GlobalCacheManagerServer.class.getDeclaredField("global");
		field.setAccessible(true);
		@SuppressWarnings("unchecked")
		var global = (ConcurrentHashMap<Binary, Object>)requireNonNull(field.get(gcm));
		return global.get(key);
	}

	@Test
	@Timeout(60)
	public void testAcquireShareWithClearedUserStateKeepsCacheStateConsistent() throws Exception {
		var key = new Binary("UnitTest.SyncAcquireShareKick.Key".getBytes(StandardCharsets.UTF_8));
		var clientA = new RawClient("SyncAcquireShareKick.A");
		var clientB = new RawClient("SyncAcquireShareKick.B");
		var clientC = new RawClient("SyncAcquireShareKick.C");
		try {
			var socketA = clientA.connect();
			login(socketA, 9301);
			var acquireA = new Acquire(key, GlobalCacheManagerConst.StateModify);
			Assertions.assertTrue(acquireA.SendForWait(socketA, 10_000).await(10_000));
			Assertions.assertEquals(0, acquireA.getResultCode());

			var socketB = clientB.connect();
			login(socketB, 9302);
			var socketBOnServer = serverSocketOf(9302);
			var holderB = requireNonNull(socketBOnServer.getUserState());
			// 模拟 processAcquireRequest 捕获 sender 后、acquireShare 入口前连接被 kick 清空 userState
			socketBOnServer.setUserState(null);

			var rpcB = new Acquire(key, GlobalCacheManagerConst.StateShare);
			rpcB.Result.globalKey = key;
			rpcB.setSender(socketBOnServer);
			var acquireShare = GlobalCacheManagerServer.class.getDeclaredMethod(
					"acquireShare", Acquire.class, holderB.getClass());
			acquireShare.setAccessible(true);
			var task = new FutureTask<Void>(() -> {
				acquireShare.invoke(gcm, rpcB, holderB);
				return null;
			});
			var worker = new Thread(task, "test-sync-acquire-share-kick");
			worker.setDaemon(true);
			worker.start();
			try {
				var reduce = clientA.reduces.poll(10, TimeUnit.SECONDS);
				Assertions.assertNotNull(reduce, "A must receive Reduce");
				reduce.Result.globalKey = reduce.Argument.globalKey;
				reduce.Result.state = GlobalCacheManagerConst.StateShare;
				reduce.SendResult();

				task.get(10, TimeUnit.SECONDS);
			} finally {
				if (worker.isAlive())
					worker.interrupt();
				worker.join(5_000);
			}

			var cs = requireNonNull(cacheStateOf(key));
			var modifyField = cs.getClass().getDeclaredField("modify");
			modifyField.setAccessible(true);
			var shareField = cs.getClass().getDeclaredField("share");
			shareField.setAccessible(true);
			var pendingField = cs.getClass().getDeclaredField("acquireStatePending");
			pendingField.setAccessible(true);
			Assertions.assertNull(modifyField.get(cs), "modify must be cleared after downgrade to Share");
			Assertions.assertEquals(GlobalCacheManagerConst.StateInvalid, pendingField.getInt(cs));
			@SuppressWarnings("unchecked")
			var shareSet = (IdentityHashSet<Object>)requireNonNull(shareField.get(cs));
			Assertions.assertTrue(shareSet.contains(sessionOf(9301)));
			Assertions.assertTrue(shareSet.contains(holderB));

			// 第三方 C 仍可正常获取 Share（若 CacheState 撕裂，此处会永久抛出 CacheState state error）
			var socketC = clientC.connect();
			login(socketC, 9303);
			var acquireC = new Acquire(key, GlobalCacheManagerConst.StateShare);
			Assertions.assertTrue(acquireC.SendForWait(socketC, 10_000).await(10_000));
			Assertions.assertFalse(acquireC.isTimeout());
			Assertions.assertEquals(0, acquireC.getResultCode());
			Assertions.assertEquals(GlobalCacheManagerConst.StateShare, acquireC.Result.state);
		} finally {
			clientA.stop();
			clientB.stop();
			clientC.stop();
		}
	}

	@Test
	@Timeout(60)
	public void testAcquireModifyWithClearedUserStateKeepsOwnershipConsistent() throws Exception {
		var key = new Binary("UnitTest.SyncAcquireModifyKick.Key".getBytes(StandardCharsets.UTF_8));
		var clientA = new RawClient("SyncAcquireModifyKick.A");
		var clientB = new RawClient("SyncAcquireModifyKick.B");
		try {
			var socketA = clientA.connect();
			login(socketA, 9311);
			var acquireA = new Acquire(key, GlobalCacheManagerConst.StateModify);
			Assertions.assertTrue(acquireA.SendForWait(socketA, 10_000).await(10_000));
			Assertions.assertEquals(0, acquireA.getResultCode());

			var socketB = clientB.connect();
			login(socketB, 9312);
			var socketBOnServer = serverSocketOf(9312);
			var holderA = sessionOf(9311);
			var holderB = requireNonNull(socketBOnServer.getUserState());
			socketBOnServer.setUserState(null);

			var rpcB = new Acquire(key, GlobalCacheManagerConst.StateModify);
			rpcB.Result.globalKey = key;
			rpcB.setSender(socketBOnServer);
			var acquireModify = GlobalCacheManagerServer.class.getDeclaredMethod(
					"acquireModify", Acquire.class, holderB.getClass());
			acquireModify.setAccessible(true);
			var task = new FutureTask<Void>(() -> {
				acquireModify.invoke(gcm, rpcB, holderB);
				return null;
			});
			var worker = new Thread(task, "test-sync-acquire-modify-kick");
			worker.setDaemon(true);
			worker.start();
			try {
				var reduce = clientA.reduces.poll(10, TimeUnit.SECONDS);
				Assertions.assertNotNull(reduce, "A must receive Reduce");
				reduce.Result.globalKey = reduce.Argument.globalKey;
				reduce.Result.state = GlobalCacheManagerConst.StateInvalid;
				reduce.SendResult();

				task.get(10, TimeUnit.SECONDS);
			} finally {
				if (worker.isAlive())
					worker.interrupt();
				worker.join(5_000);
			}

			var cs = requireNonNull(cacheStateOf(key));
			var modifyField = cs.getClass().getDeclaredField("modify");
			modifyField.setAccessible(true);
			var acquiredField = holderA.getClass().getDeclaredField("acquired");
			acquiredField.setAccessible(true);
			@SuppressWarnings("unchecked")
			var acquiredA = (ConcurrentHashMap<Binary, Integer>)requireNonNull(acquiredField.get(holderA));
			@SuppressWarnings("unchecked")
			var acquiredB = (ConcurrentHashMap<Binary, Integer>)requireNonNull(acquiredField.get(holderB));
			Assertions.assertSame(holderB, modifyField.get(cs), "modify must transfer cleanly to B");
			Assertions.assertFalse(acquiredA.containsKey(key), "A must no longer track downgraded key");
			Assertions.assertEquals(GlobalCacheManagerConst.StateModify, acquiredB.get(key));
		} finally {
			clientA.stop();
			clientB.stop();
		}
	}
}
