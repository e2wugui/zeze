package Zeze.Arch;

import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Util.TimeThrottle;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestRedirectServerReconnect {
	private static final int REMOTE_SERVER_ID = 99;

	@Test
	public void staleRegistrationDoesNotReportADeadConnectionReady() throws Exception {
		var config = new Config();
		config.setServiceManager("disable");
		config.setNoDatabase(true);
		var app = ProviderDirectTestSupport.newAppWithFakeAgent("RedirectServerReady", config);
		var service = new DirectService(app);
		service.providerApp = new ProviderApp(app);
		var oldSession = service.newSession(REMOTE_SERVER_ID);
		service.providerByServerId.put(REMOTE_SERVER_ID, oldSession); // socket已消失，OnSocketClose清理尚未执行。
		var readyCalls = new AtomicInteger();
		var liveSocket = new LocalSocket(service);
		var newSession = service.newSession(REMOTE_SERVER_ID);
		newSession.sessionId = liveSocket.getSessionId();
		liveSocket.setUserState(newSession);
		try {
			service.waitDirectServerReady(REMOTE_SERVER_ID, readyCalls::incrementAndGet);
			assertEquals(0, readyCalls.get(), "旧会话登记存在，但socket不存在，不能误判就绪");
			assertTrue(service.register(liveSocket));
			service.setRelativeServiceReady(newSession, "127.0.0.1", 1);
			assertEquals(1, readyCalls.get(), "活连接重新登记后才应通知就绪");
		} finally {
			liveSocket.close();
			oldSession.timeCounter.close();
			newSession.timeCounter.close();
			app.stop();
		}
	}

	@Test
	public void choiceServerUsesTheReplacementSessionAfterWaiting() throws Exception {
		var config = new Config();
		config.setServiceManager("disable");
		config.setNoDatabase(true);
		var app = ProviderDirectTestSupport.newAppWithFakeAgent("RedirectServerReconnect", config);
		var providerApp = new ProviderApp(app);
		var service = new DirectService(app);
		service.providerApp = providerApp;
		setField(providerApp, "providerDirectService", service);
		setField(providerApp, "startLast", true);
		var oldSession = service.newSession(REMOTE_SERVER_ID);
		service.providerByServerId.put(REMOTE_SERVER_ID, oldSession);
		var liveSocket = new LocalSocket(service);
		var newSession = service.newSession(REMOTE_SERVER_ID);
		newSession.sessionId = liveSocket.getSessionId();
		liveSocket.setUserState(newSession);
		var chosenSocket = new AtomicReference<AsyncSocket>();
		var routingFailure = new AtomicReference<Throwable>();
		var chooser = new Thread(() -> {
			try {
				chosenSocket.set(app.redirect.choiceServer(new ProviderDirect(), REMOTE_SERVER_ID));
			} catch (Throwable e) {
				routingFailure.set(e);
			}
		}, "redirect-await-reconnect");
		chooser.start();
		try {
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
			while (chooser.isAlive() && chooser.getState() != Thread.State.WAITING
					&& chooser.getState() != Thread.State.TIMED_WAITING && System.nanoTime() < deadline)
				Thread.sleep(1);
			assertTrue(chooser.isAlive(), "旧socket消失后应等待重连，不应立即报SERVER_NOT_FOUND");
			assertTrue(service.register(liveSocket));
			service.setRelativeServiceReady(newSession, "127.0.0.1", 1);
			chooser.join(5_000);
			assertFalse(chooser.isAlive(), "新会话就绪后必须结束等待");
			assertNull(routingFailure.get(), "重连成功后不能继续使用旧会话查socket并误报失败");
			assertSame(liveSocket, chosenSocket.get());
		} finally {
			chooser.interrupt();
			chooser.join(5_000);
			assertFalse(chooser.isAlive(), "测试结束不得留下路由等待线程");
			liveSocket.close();
			oldSession.timeCounter.close();
			newSession.timeCounter.close();
			app.stop();
		}
	}

	private static void setField(Object owner, String name, Object value) throws Exception {
		var field = ProviderApp.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(owner, value);
	}

	private static final class DirectService extends ProviderDirectService {
		DirectService(Zeze.Application app) {
			super("RedirectServerReconnect.direct", app);
		}

		boolean register(AsyncSocket socket) {
			return addSocket(socket);
		}
	}

	// 仅替代网络传输；会话登记、就绪通知和路由查找均调用真实实现。
	private static final class LocalSocket extends AsyncSocket {
		LocalSocket(DirectService service) {
			super(service);
		}

		@Override
		public Type getType() {
			return Type.eClient;
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
		public boolean Send(byte @NotNull [] bytes, int offset, int length) {
			return !isClosed();
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
