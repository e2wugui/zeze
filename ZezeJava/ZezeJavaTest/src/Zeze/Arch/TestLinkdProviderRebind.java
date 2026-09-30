package Zeze.Arch;

import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.Application;
import Zeze.Builtin.LinkdBase.BReportError;
import Zeze.Builtin.LinkdBase.ReportError;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Net.Service;
import Zeze.Util.IntHashMap;
import Zeze.Util.LongHashSet;
import Zeze.Util.TimeThrottle;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
			var closeFailure = new AtomicReference<Throwable>();
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
		public boolean Send(@NotNull Protocol<?> protocol) {
			if (protocol instanceof ReportError report && report.Argument.getCode() == BReportError.CodeProviderBroken)
				providerBrokenReports.incrementAndGet();
			return !isClosed();
		}

		@Override
		public boolean Send(byte @NotNull [] bytes, int offset, int length) {
			return !isClosed();
		}

		@Override
		protected void doClose(@Nullable Throwable ex, boolean gracefully) {
			try {
				getService().OnSocketClose(this, ex);
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
