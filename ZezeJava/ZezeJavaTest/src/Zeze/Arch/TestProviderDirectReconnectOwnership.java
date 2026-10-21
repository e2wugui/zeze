package Zeze.Arch;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import Zeze.Application;
import Zeze.Builtin.Provider.BModule;
import Zeze.Builtin.ProviderDirect.AnnounceProviderInfo;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Net.Service;
import Zeze.Net.TcpSocket;
import Zeze.Services.HandshakeServer;
import Zeze.Services.ServiceManager.Agent;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Services.ServiceManager.BSubscribeInfo;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import Zeze.Util.TimeCounter;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

/** Real TCP/handshake/reconnect; the fake SM supplies only a local subscription snapshot. */
@Fast
@Isolated
public class TestProviderDirectReconnectOwnership {

	@Test
	void oldCloseKeepsTheReconnectedRoutes() throws Exception {
		try (var env = new Environment(true)) {
			var first = env.connect();
			env.client.delayClose = first;
			var close = Thread.ofPlatform().name("direct-delayed-close").start(first::close);
			try {
				assertTrue(env.client.closeEntered.await(5, TimeUnit.SECONDS));
				var replacement = env.nextHandshake(); // Normal Connector retry, not a manually replaced socket.
				assertNotSame(first, replacement);
				assertSame(replacement, env.connector().TryGetReadySocket());
				var replacementSession = (ProviderSession)requireNonNull(replacement.getUserState());
				assertSame(replacementSession, env.client.providerByServerId.get(Environment.REMOTE_ID));
				env.client.releaseClose.countDown();
				close.join(5_000);
				assertFalse(close.isAlive());
				assertAll(
						() -> assertFalse(replacement.isClosed(), "new TCP connection is still live"),
						() -> assertSame(replacementSession,
								env.client.providerByServerId.get(Environment.REMOTE_ID), "old close must retain server route"),
						() -> assertSame(replacementSession,
								env.client.providerByLoadName.get("127.0.0.1_" + env.port), "old close must retain address route"),
						() -> assertEquals(replacement.getSessionId(), assertInstanceOf(ProviderModuleState.class,
								env.subscription.getLocalStates().get(Environment.REMOTE_ID_STRING)).sessionId),
						() -> assertFalse(counterCancelled(replacementSession), "new session counter remains active"));
			} finally {
				env.client.releaseClose.countDown();
				close.join(5_000);
				assertFalse(close.isAlive(), "no close thread may survive the test");
			}
		}
	}

	@Test
	void normalReconnectGetsItsOwnSessionAndCounter() throws Exception {
		try (var env = new Environment(true)) {
			var first = env.connect();
			var firstSession = (ProviderSession)requireNonNull(first.getUserState());
			first.close();
			var replacement = env.nextHandshake();
			var nextSession = (ProviderSession)requireNonNull(replacement.getUserState());
			var template = (ProviderSession)requireNonNull(env.connector().userState);
			assertAll(
					() -> assertNotSame(firstSession, nextSession, "connection identity must not be a reusable template"),
					() -> assertTrue(counterCancelled(firstSession)),
					() -> assertFalse(counterCancelled(nextSession)),
					() -> assertEquals(Environment.REMOTE_ID, nextSession.getServerId()),
					() -> assertEquals(replacement.getSessionId(), nextSession.getSessionId()),
					() -> assertEquals(ControlledService.CONFIGURED_VERSION, nextSession.appVersion),
					() -> assertTrue(nextSession.isDisableChoice(), "per-connection factory initialization is retained"),
					() -> assertTrue(counterCancelled(template),
							"Connector retains an inactive ProviderSession template"));
		}
	}

	@Test
	void closeDuringSessionCreationClosesTheUnpublishedCounter() throws Exception {
		try (var env = new Environment(true)) {
			env.client.delaySessionCreation = true;
			env.startConnector();
			assertTrue(env.client.sessionCreated.await(5, TimeUnit.SECONDS));
			var socket = requireNonNull(env.connector().getSocket());
			env.connector().setAutoReconnect(false);
			env.connector().stop(); // close sees the template before the actual session is published.
			env.client.releaseSession.countDown();
			assertTrue(env.client.connected.await(5, TimeUnit.SECONDS));
			assertTrue(socket.isClosed());
			for (var session : env.client.sessions)
				assertTrue(counterCancelled(session), "close before publication must reclaim the new counter too");
			assertNull(env.client.providerByServerId.get(Environment.REMOTE_ID));
			assertTrue(env.client.handshakes.isEmpty());
		}
	}

	@Test
	void stopBeforeHandshakeClosesConnectionState() throws Exception {
		try (var env = new Environment(false)) {
			env.startConnector();
			assertTrue(env.client.connected.await(5, TimeUnit.SECONDS));
			var socket = requireNonNull(env.connector().getSocket());
			assertFalse(socket.isHandshakeDone(), "peer intentionally sends no handshake");
			var session = (ProviderSession)requireNonNull(socket.getUserState());
			env.connector().setAutoReconnect(false);
			env.connector().stop();
			assertTrue(socket.isClosed());
			assertTrue(counterCancelled(session));
			assertNull(env.client.providerByServerId.get(Environment.REMOTE_ID));
		}
	}

	@Test
	void stopDuringDnsDoesNotActivateAConnectionState() throws Exception {
		try (var env = new Environment(true)) {
			env.client.delayResolve = true;
			env.startConnector();
			assertTrue(env.client.resolveEntered.await(5, TimeUnit.SECONDS));
			var pending = requireNonNull(env.connector().getSocket());
			env.connector().setAutoReconnect(false);
			env.connector().stop();
			env.client.releaseResolve.countDown();
			assertTrue(env.client.resolveExited.await(5, TimeUnit.SECONDS));
			assertTrue(pending.isClosed());
			assertNull(env.connector().getSocket());
			assertTrue(env.client.handshakes.isEmpty());
			for (var session : env.client.sessions)
				assertTrue(counterCancelled(session), "unowned template/pending state must not retain a periodic task");
		}
	}

	private static boolean counterCancelled(ProviderSession session) throws Exception {
		var field = TimeCounter.class.getDeclaredField("discardTimer");
		field.setAccessible(true);
		return requireNonNull((Future<?>)field.get(session.timeCounter)).isCancelled();
	}

	private static final class Environment implements AutoCloseable {
		static final int REMOTE_ID = 99; // No database/app start; server IDs need no shared allocation.
		static final String REMOTE_ID_STRING = "99";
		static final String PREFIX = "DirectReconnect#";
		static final int MODULE = 42;
		final Application app;
		final ControlledService client;
		final ProviderService linkService;
		final Service peer;
		final int port;
		final Agent.SubscribeState subscription;
		private @Nullable Connector connector;

		Environment(boolean handshake) throws Exception {
			Task.tryInitThreadPool();
			var conf = new Config();
			conf.setNoDatabase(true);
			conf.setServiceManager("disable");
			conf.setServerId(1);
			app = ProviderDirectTestSupport.newAppWithFakeAgent("DirectReconnect", conf);
			client = new ControlledService(app);
			linkService = new ProviderService("DirectReconnect.links", app);
			var provider = new ProviderApp(app, new ProviderOnly(), linkService, PREFIX,
					new ProviderDirect(), client, "DirectReconnect.linkd", new LoadConfig());
			provider.modules.put(MODULE, new BModule.Data());
			peer = handshake ? new HandshakeServer("DirectReconnect.peer", (Config)null) : new Service("DirectReconnect.peer");
			peer.AddFactoryHandle(AnnounceProviderInfo.TypeId_, new Service.ProtocolFactoryHandle<>(
					AnnounceProviderInfo::new, rpc -> { rpc.SendResult(); return 0; },
					TransactionLevel.None, DispatchMode.Direct));
			var listener = (TcpSocket)peer.newServerSocket("127.0.0.1", 0, null);
			port = requireNonNull(listener.getLocalInet()).getPort();
			subscription = new Agent.SubscribeState(new BSubscribeInfo(PREFIX + MODULE, 0));
			subscription.onRegister(new BServiceInfo(PREFIX + MODULE, REMOTE_ID_STRING, 0, "127.0.0.1", port));
			requireNonNull(app.getServiceManager()).getSubscribeStates().put(PREFIX + MODULE, subscription);
		}

		Connector connector() {
			return requireNonNull(connector);
		}

		void startConnector() {
			client.addServer(new BServiceInfo(PREFIX + MODULE, REMOTE_ID_STRING, 0, "127.0.0.1", port));
			connector = requireNonNull(client.getConfig().findConnector("127.0.0.1_" + port));
		}

		AsyncSocket connect() throws Exception {
			startConnector();
			return nextHandshake();
		}

		AsyncSocket nextHandshake() throws Exception {
			return requireNonNull(client.handshakes.poll(10, TimeUnit.SECONDS), "real reconnect handshake did not finish");
		}

		@Override
		public void close() throws Exception {
			client.releaseClose.countDown();
			client.releaseResolve.countDown();
			client.releaseSession.countDown();
			var c = connector;
			if (c != null) {
				c.setAutoReconnect(false);
				c.stop();
			}
			try {
				client.stop();
			} finally {
				try {
					peer.stop();
				} finally {
					linkService.stop();
					app.stop();
					for (var session : client.sessions)
						session.timeCounter.close();
				}
			}
		}
	}

	private static final class ControlledService extends ProviderDirectService {
		static final long CONFIGURED_VERSION = 1L << 48;
		final LinkedBlockingQueue<AsyncSocket> handshakes = new LinkedBlockingQueue<>();
		final List<ProviderSession> sessions = new CopyOnWriteArrayList<>();
		final CountDownLatch connected = new CountDownLatch(1);
		final CountDownLatch closeEntered = new CountDownLatch(1);
		final CountDownLatch releaseClose = new CountDownLatch(1);
		final CountDownLatch resolveEntered = new CountDownLatch(1);
		final CountDownLatch releaseResolve = new CountDownLatch(1);
		final CountDownLatch resolveExited = new CountDownLatch(1);
		final CountDownLatch sessionCreated = new CountDownLatch(1);
		final CountDownLatch releaseSession = new CountDownLatch(1);
		volatile @Nullable AsyncSocket delayClose;
		volatile boolean delayResolve;
		volatile boolean delaySessionCreation;

		ControlledService(Application app) {
			super("DirectReconnect.client", app);
		}

		@Override
		public ProviderSession newSession(int serverId) {
			var session = super.newSession(serverId);
			session.appVersion = CONFIGURED_VERSION;
			session.setDisableChoice(true);
			sessions.add(session);
			if (delaySessionCreation && sessions.size() > 1) {
				sessionCreated.countDown();
				try {
					assertTrue(releaseSession.await(10, TimeUnit.SECONDS), "session creation gate timed out");
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new AssertionError(e);
				}
			}
			return session;
		}

		@Override
		public void OnSocketConnected(AsyncSocket so) {
			super.OnSocketConnected(so);
			connected.countDown();
		}

		@Override
		public void OnHandshakeDone(AsyncSocket so) throws Exception {
			super.OnHandshakeDone(so);
			if (!so.isClosed())
				handshakes.add(so);
		}

		@Override
		public void OnSocketClose(AsyncSocket socket, @Nullable Throwable ex) throws Exception {
			if (socket == delayClose) {
				closeEntered.countDown();
				assertTrue(releaseClose.await(10, TimeUnit.SECONDS), "old close gate timed out");
			}
			super.OnSocketClose(socket, ex);
		}

		@Override
		protected InetAddress resolveAddress(@Nullable String host) throws IOException {
			if (!delayResolve)
				return super.resolveAddress(host);
			resolveEntered.countDown();
			try {
				if (!releaseResolve.await(10, TimeUnit.SECONDS))
					throw new IOException("DNS gate timed out");
				return super.resolveAddress(host);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IOException(e);
			} finally {
				resolveExited.countDown();
			}
		}
	}
}
