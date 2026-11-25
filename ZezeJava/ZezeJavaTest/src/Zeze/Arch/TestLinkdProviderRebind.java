package Zeze.Arch;

import static java.util.Objects.requireNonNull;

import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.Application;
import Zeze.Builtin.LinkdBase.BReportError;
import Zeze.Builtin.LinkdBase.ReportError;
import Zeze.Builtin.Provider.AnnounceProviderInfo;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Net.Service;
import Zeze.Util.IntHashMap;
import Zeze.Util.LongHashSet;
import Zeze.Util.Task;
import Zeze.Util.TimeThrottle;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestLinkdProviderRebind {

	@Test
	public void replacementRemovesTheLinkFromTheFormerProvidersReverseIndex() throws Exception {
		try (var fixture = new Fixture(false)) {
			fixture.user.bind(fixture.providers, fixture.link, List.of(Online.ModuleId), fixture.oldProvider);
			assertTrue(contains(fixture.oldSession, Online.ModuleId, fixture.link.getSessionId()));

			fixture.user.bind(fixture.providers, fixture.link, List.of(Online.ModuleId), fixture.newProvider);

			assertEquals(fixture.newProvider.getSessionId(), fixture.user.tryGetProvider(Online.ModuleId));
			assertFalse(contains(fixture.oldSession, Online.ModuleId, fixture.link.getSessionId()),
					"换绑后旧provider不能继续保留客户端反向索引");
			assertTrue(contains(fixture.newSession, Online.ModuleId, fixture.link.getSessionId()));
		}
	}

	@Test
	public void anOldCloseSnapshotDoesNotReportTheReplacementProviderBroken() throws Exception {
		try (var fixture = new Fixture(true)) {
			int moduleId = Zeze.Game.Online.ModuleId;
			fixture.user.bind(fixture.providers, fixture.link, List.of(moduleId), fixture.oldProvider);
			var oldSession = (PausingSession)fixture.oldSession;
			var closeFailure = new AtomicReference<@Nullable Throwable>();
			var closeThread = new Thread(() -> {
				try {
					fixture.providerModule.onProviderClose(fixture.oldProvider);
				} catch (Throwable e) {
					closeFailure.set(e);
				}
			}, "linkd-old-provider-close");
			closeThread.start();
			try {
				assertTrue(oldSession.snapshotTaken.await(5, TimeUnit.SECONDS), "关闭路径必须先取出真实反向索引快照");
				// A关闭处理已拿到旧快照；B成功换绑后，A的remove无法再修改该快照。
				fixture.user.bind(fixture.providers, fixture.link, List.of(moduleId), fixture.newProvider);
				oldSession.resumeClose.countDown();
				closeThread.join(5_000);
				assertFalse(closeThread.isAlive(), "关闭处理必须收尾");
				assertNull(closeFailure.get());

				assertEquals(fixture.newProvider.getSessionId(), fixture.user.tryGetProvider(moduleId),
						"旧provider关闭不能解除现任provider的绑定");
				assertTrue(contains(fixture.newSession, moduleId, fixture.link.getSessionId()));
				assertEquals(0, fixture.link.providerBrokenReports.get(),
						"现任provider健康时，旧关闭快照不能向客户端误报CodeProviderBroken");
			} finally {
				oldSession.resumeClose.countDown();
				closeThread.interrupt();
				closeThread.join(5_000);
				assertFalse(closeThread.isAlive(), "测试结束不得留下关闭线程");
			}
		}
	}

	@Test
	public void closingTheCurrentOnlineProviderStillUnbindsAndReportsBroken() throws Exception {
		try (var fixture = new Fixture(false)) {
			fixture.user.bind(fixture.providers, fixture.link, List.of(Online.ModuleId), fixture.oldProvider);
			fixture.providerModule.onProviderClose(fixture.oldProvider);
			assertNull(fixture.user.tryGetProvider(Online.ModuleId));
			assertEquals(1, fixture.link.providerBrokenReports.get(), "实际断开的现任provider仍应报告失败");
		}
	}

	@Test
	public void closedProviderRejectsLateBindingWithoutReplacingHealthyOwner() throws Exception {
		try (var fixture = new Fixture(false)) {
			int moduleId = Online.ModuleId;
			fixture.user.bind(fixture.providers, fixture.link, List.of(moduleId), fixture.newProvider);
			fixture.providerModule.onProviderClose(fixture.oldProvider);
			fixture.user.bind(fixture.providers, fixture.link, List.of(moduleId), fixture.oldProvider);
			assertEquals(fixture.newProvider.getSessionId(), fixture.user.tryGetProvider(moduleId));
			assertTrue(contains(fixture.newSession, moduleId, fixture.link.getSessionId()));
			assertFalse(contains(fixture.oldSession, moduleId, fixture.link.getSessionId()));
			assertFalse(fixture.oldSession.tryAddLinkSession(moduleId, fixture.link.getSessionId()));
			fixture.oldSession.addLinkSession(moduleId, fixture.link.getSessionId());
			assertFalse(contains(fixture.oldSession, moduleId, fixture.link.getSessionId()));
			assertEquals(0, fixture.link.providerBrokenReports.get());
		}
	}

	@Test
	public void aBindAfterTheCloseSnapshotCannotEscapeCleanup() throws Exception {
		try (var fixture = new Fixture(true)) {
			int existingModule = Zeze.Game.Online.ModuleId;
			int lateModule = Online.ModuleId;
			fixture.user.bind(fixture.providers, fixture.link, List.of(existingModule), fixture.oldProvider);
			var session = (PausingSession)fixture.oldSession;
			var failure = new AtomicReference<@Nullable Throwable>();
			var close = new Thread(() -> {
				try {
					fixture.providerModule.onProviderClose(fixture.oldProvider);
				} catch (Throwable e) {
					failure.set(e);
				}
			}, "linkd-provider-close-admission");
			close.start();
			try {
				assertTrue(session.snapshotTaken.await(5, TimeUnit.SECONDS));
				fixture.user.bind(fixture.providers, fixture.link, List.of(lateModule), fixture.oldProvider);
				assertNull(fixture.user.tryGetProvider(lateModule));
				assertFalse(contains(session, lateModule, fixture.link.getSessionId()));
				session.resumeClose.countDown();
				close.join(5_000);
				assertFalse(close.isAlive());
				assertNull(failure.get());
				assertNull(fixture.user.tryGetProvider(existingModule));
				assertEquals(1, fixture.link.providerBrokenReports.get());
			} finally {
				session.resumeClose.countDown();
				close.interrupt();
				close.join(5_000);
				assertFalse(close.isAlive());
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static java.util.concurrent.ConcurrentHashMap<Integer, Zeze.Net.AsyncSocket> serverId2ProviderSocketOf(
			LinkdProvider provider) throws Exception {
		var f = LinkdProvider.class.getDeclaredField("serverId2ProviderSocket");
		f.setAccessible(true);
		return (java.util.concurrent.ConcurrentHashMap<Integer, Zeze.Net.AsyncSocket>)f.get(provider);
	}

	@Test
	public void closedProviderAnnounceDoesNotLeakRoutesOrKickActiveReplacement() throws Exception {
		Task.tryInitThreadPool();
		try (var fixture = new Fixture(false)) {
			var serverIdMap = serverId2ProviderSocketOf(fixture.providerModule);
			var announceNew = new AnnounceProviderInfo();
			announceNew.setSender(fixture.newProvider);
			announceNew.Argument.setServiceNamePrefix("Server#");
			announceNew.Argument.setServiceIdentity("7");
			announceNew.Argument.setProviderDirectIp("127.0.0.1");
			announceNew.Argument.setProviderDirectPort(20002);
			fixture.providerModule.ProcessAnnounceProviderInfo(announceNew);

			assertSame(fixture.newProvider, serverIdMap.get(7));
			assertSame(fixture.newSession, fixture.providers.providerSessions.get("127.0.0.1_20002"));

			// 旧连接先关闭，随后迟到的 AnnounceProviderInfo 不得写入路由表，更不得覆盖或踢掉活跃新连接。
			fixture.oldProvider.close();
			assertTrue(fixture.oldProvider.isClosed());

			var announceOldSameServer = new AnnounceProviderInfo();
			announceOldSameServer.setSender(fixture.oldProvider);
			announceOldSameServer.Argument.setServiceNamePrefix("Server#");
			announceOldSameServer.Argument.setServiceIdentity("7");
			announceOldSameServer.Argument.setProviderDirectIp("127.0.0.1");
			announceOldSameServer.Argument.setProviderDirectPort(20001);
			fixture.providerModule.ProcessAnnounceProviderInfo(announceOldSameServer);

			assertSame(fixture.newProvider, serverIdMap.get(7),
					"closed provider announce must not overwrite active serverId route");
			assertNull(fixture.providers.providerSessions.get("127.0.0.1_20001"),
					"closed provider announce must not leak into providerSessions");
			assertSame(fixture.newSession, fixture.providers.providerSessions.get("127.0.0.1_20002"));
			assertFalse(fixture.newProvider.isClosed(),
					"closed provider announce must not kick the active replacement connection");

			var announceOldFreshServer = new AnnounceProviderInfo();
			announceOldFreshServer.setSender(fixture.oldProvider);
			announceOldFreshServer.Argument.setServiceNamePrefix("Server#");
			announceOldFreshServer.Argument.setServiceIdentity("8");
			announceOldFreshServer.Argument.setProviderDirectIp("127.0.0.1");
			announceOldFreshServer.Argument.setProviderDirectPort(20003);
			fixture.providerModule.ProcessAnnounceProviderInfo(announceOldFreshServer);

			assertNull(serverIdMap.get(8),
					"closed provider announce must not register a dead socket");
			assertNull(fixture.providers.providerSessions.get("127.0.0.1_20003"),
					"closed provider announce must not register a dead session");

			// 活跃新连接后续正常关闭时，路由表必须按所有权清理干净。
			fixture.newProvider.close();
			assertNull(serverIdMap.get(7));
			assertNull(fixture.providers.providerSessions.get("127.0.0.1_20002"));
		}
	}

	private static boolean contains(LinkdProviderSession session, int moduleId, long linkId) {
		session.linkSessionIdsLock.lock();
		try {
			var links = session.linkSessionIds.get(moduleId);
			return links != null && links.contains(linkId);
		} finally {
			session.linkSessionIdsLock.unlock();
		}
	}

	private static final class Fixture implements AutoCloseable {
		final Application app;
		final ProviderService providers;
		final ClientService clients;
		final LinkdProvider providerModule = new LinkdProvider();
		final LinkdApp linkdApp;
		final LocalSocket oldProvider;
		final LocalSocket newProvider;
		final LocalSocket link;
		final LinkdProviderSession oldSession;
		final LinkdProviderSession newSession;
		final LinkdUserSession user;

		Fixture(boolean pauseOldClose) throws Exception {
			var config = new Config();
			config.setServiceManager("disable");
			config.setNoDatabase(true);
			app = ProviderDirectTestSupport.newAppWithFakeAgent("LinkdProviderRebind", config);
			providers = new ProviderService(app);
			clients = new ClientService(app);
			// 真实服务及模块装配；服务均不启动，只在本进程登记socket。
			linkdApp = new LinkdApp("LinkdProviderRebind", app, providerModule, providers, clients, new LoadConfig());
			oldProvider = new LocalSocket(providers);
			newProvider = new LocalSocket(providers);
			oldSession = pauseOldClose ? new PausingSession(oldProvider.getSessionId())
					: new LinkdProviderSession(oldProvider.getSessionId());
			newSession = new LinkdProviderSession(newProvider.getSessionId());
			oldProvider.setUserState(oldSession);
			newProvider.setUserState(newSession);
			assertTrue(providers.register(oldProvider));
			assertTrue(providers.register(newProvider));
			link = new LocalSocket(clients);
			user = new LinkdUserSession(link.getSessionId());
			user.setAuthed();
			link.setUserState(user);
			assertTrue(clients.register(link));
		}

		@Override
		public void close() throws Exception {
			oldSession.timeCounter.close();
			newSession.timeCounter.close();
			clients.stop();
			providers.stop();
			linkdApp.commandConsoleService.stop();
			app.stop();
		}
	}

	private static final class PausingSession extends LinkdProviderSession {
		final CountDownLatch snapshotTaken = new CountDownLatch(1);
		final CountDownLatch resumeClose = new CountDownLatch(1);

		PausingSession(long sessionId) {
			super(sessionId);
		}

		@Override
		public IntHashMap<LongHashSet> swapLinkSessionIds() {
			var snapshot = super.swapLinkSessionIds();
			snapshotTaken.countDown();
			try {
				assertTrue(resumeClose.await(5, TimeUnit.SECONDS), "快照取出后必须等待换绑结束");
			} catch (InterruptedException e) {
				throw new AssertionError(e);
			}
			return snapshot;
		}
	}

	private static final class ProviderService extends LinkdProviderService {
		ProviderService(Application app) {
			super("LinkdProviderRebind.providers", app);
		}

		boolean register(AsyncSocket socket) {
			return addSocket(socket);
		}
	}

	private static final class ClientService extends LinkdService {
		ClientService(Application app) {
			super("LinkdProviderRebind.clients", app);
		}

		boolean register(AsyncSocket socket) {
			return addSocket(socket);
		}
	}

	// 仅替代网络传输，捕获真实发送的协议；绑定和关闭处理均调用生产实现。
	private static final class LocalSocket extends AsyncSocket {
		final AtomicInteger providerBrokenReports = new AtomicInteger();

		LocalSocket(Service service) {
			super(service);
		}

		@Override
		public Type getType() {
			return Type.eServer;
		}

		@Override
		public boolean Send(Protocol<?> protocol) {
			if (protocol instanceof ReportError report && report.Argument.getCode() == BReportError.CodeProviderBroken)
				providerBrokenReports.incrementAndGet();
			return !isClosed();
		}

		@Override
		public boolean Send(byte [] bytes, int offset, int length) {
			return !isClosed();
		}

		@Override
		protected void doClose(@Nullable Throwable ex, boolean gracefully) {
			try {
				requireNonNull(getService()).OnSocketClose(this, ex);
			} catch (Exception e) {
				throw new AssertionError(e);
			}
		}

		@Override
		public @Nullable TimeThrottle getTimeThrottle() {
			return null;
		}

		@Override
		public @Nullable SocketAddress getRemoteAddress() {
			return null;
		}
	}
}
