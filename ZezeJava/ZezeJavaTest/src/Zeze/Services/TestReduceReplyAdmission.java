package Zeze.Services;

import java.net.InetAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Connector;
import Zeze.Net.Service;
import Zeze.Net.TcpSocket;
import Zeze.Services.GlobalCacheManager.Acquire;
import Zeze.Services.GlobalCacheManager.Login;
import Zeze.Services.GlobalCacheManager.Reduce;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.IdentityHashSet;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
@ResourceLock("GlobalCacheManagerServer.instance")
public class TestReduceReplyAdmission {
	private static final class Peer extends Service {
		final BlockingQueue<Reduce> reduces = new LinkedBlockingQueue<>();

		Peer(String name) {
			super(name, new Config());
			AddFactoryHandle(Login.TypeId_, new ProtocolFactoryHandle<>(
					Login::new, null, TransactionLevel.None, DispatchMode.Direct));
			AddFactoryHandle(Acquire.TypeId_, new ProtocolFactoryHandle<>(
					Acquire::new, null, TransactionLevel.None, DispatchMode.Direct));
			AddFactoryHandle(Reduce.TypeId_, new ProtocolFactoryHandle<>(
					Reduce::new, r -> { reduces.add(r); return 0; }, TransactionLevel.None, DispatchMode.Direct));
		}

		AsyncSocket connect(int port) throws Exception {
			var connector = new Connector("127.0.0.1", port, false);
			getConfig().addConnector(connector);
			start();
			return connector.WaitReady();
		}
	}

	private static Object field(Object target, String name) throws Exception {
		var field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return java.util.Objects.requireNonNull(field.get(target));
	}

	private static void login(AsyncSocket socket, int serverId) {
		var r = new Login();
		r.Argument.serverId = serverId;
		assertTrue(r.SendForWait(socket, 10_000).await(10_000));
		assertEquals(0, r.getResultCode());
	}

	@SuppressWarnings("unchecked")
	private static void exchange(boolean async, int ownerState, int requestedState, long replyCode) throws Exception {
		Task.tryInitThreadPool();
		var sync = GlobalCacheManagerServer.getInstance();
		var asynchronous = new GlobalCacheManagerAsyncServer();
		Object gcm = async ? asynchronous : sync;
		var config = async ? asynchronous.getGcmConfig() : sync.getGcmConfig();
		int initialCapacity = config.initialCapacity;
		config.initialCapacity = 31;
		var owner = new Peer("reduce-owner");
		var requester = new Peer("reduce-requester");
		try {
			if (async)
				asynchronous.start(InetAddress.getLoopbackAddress(), 0, new Config());
			else
				sync.start(InetAddress.getLoopbackAddress(), 0, new Config());
			var listener = (TcpSocket)field(gcm, "serverSocket");
			var local = listener.getLocalInet();
			assertNotNull(local);
			var ownerSocket = owner.connect(local.getPort());
			var requesterSocket = requester.connect(local.getPort());
			login(ownerSocket, 1);
			login(requesterSocket, 2);
			var key = new Binary("reduce-reply");
			var acquired = new Acquire(key, ownerState);
			assertTrue(acquired.SendForWait(ownerSocket, 10_000).await(10_000));
			assertEquals(0, acquired.getResultCode());
			var sessions = (LongConcurrentHashMap<?>)field(gcm, "sessions");
			var ownerHolder = sessions.get(1);
			assertNotNull(ownerHolder);

			var requested = new Acquire(key, requestedState);
			var result = requested.SendForWait(requesterSocket, 10_000);
			var reply = owner.reduces.poll(10, TimeUnit.SECONDS);
			assertNotNull(reply);
			reply.Result.globalKey = reply.Argument.globalKey;
			reply.Result.state = GlobalCacheManagerConst.StateInvalid;
			reply.SendResultCode(replyCode);
			assertTrue(result.await(10_000));
			assertFalse(requested.isTimeout());
			if (replyCode == Procedure.Exception) {
				assertEquals(requestedState == GlobalCacheManagerConst.StateShare
						? GlobalCacheManagerConst.AcquireShareFailed : GlobalCacheManagerConst.AcquireModifyFailed,
						requested.getResultCode(), "error response cannot prove the holder released its lock");
				var global = (ConcurrentHashMap<Binary, Object>)field(gcm, "global");
				var state = global.get(key);
				assertNotNull(state);
				if (ownerState == GlobalCacheManagerConst.StateModify)
					assertSame(ownerHolder, field(state, "modify"));
				else
					assertTrue(((IdentityHashSet<Object>)field(state, "share")).contains(ownerHolder));
			} else {
				assertEquals(0, requested.getResultCode(), "already-invalid is a valid release acknowledgement");
				assertEquals(requestedState, requested.Result.state);
			}
		} finally {
			try {
				owner.stop();
				requester.stop();
			} finally {
				if (async)
					asynchronous.stop();
				else
					sync.stop();
				config.initialCapacity = initialCapacity;
			}
		}
	}

	private static void checkTransfers(long replyCode) {
		assertAll("sync/async Modify-to-Share, Modify-to-Modify and Share-to-Modify",
				() -> exchange(false, 2, 1, replyCode),
				() -> exchange(false, 2, 2, replyCode),
				() -> exchange(false, 1, 2, replyCode),
				() -> exchange(true, 2, 1, replyCode),
				() -> exchange(true, 2, 2, replyCode),
				() -> exchange(true, 1, 2, replyCode));
	}

	@Test
	public void errorReplyKeepsExistingOwner() {
		checkTransfers(Procedure.Exception);
	}

	@Test
	public void alreadyInvalidReplyAllowsTransfer() {
		checkTransfers(GlobalCacheManagerConst.ReduceInvalidAlreadyIsInvalid);
	}
}
