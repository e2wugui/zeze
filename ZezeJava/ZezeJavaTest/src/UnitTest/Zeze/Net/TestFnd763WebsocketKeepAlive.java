package UnitTest.Zeze.Net;

import harness.Fast;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Net.Service;
import Zeze.Net.WebsocketClient;
import Zeze.Net.WebsocketHandle;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Util.GlobalTimer;
import Zeze.Util.Task;
import Zeze.Util.TimeThrottle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND7-63回归：Websocket/WebsocketClient游离于KeepAlive回收体系。
 * <ul>
 * <li>收发路径必须维护活跃时间（processInput/onBinary→setActiveRecvTime，Send→setActiveSendTime），
 * 连接建立（构造/握手完成）时reset，成为checkKeepAlive可管理的连接；</li>
 * <li>checkKeepAlive的覆盖判据从instanceof TcpSocket放宽为"活跃时间曾被更新"，
 * 静默死链（对端断电、无FIN/RST）可被回收，Connector重连钩子才能触发；</li>
 * <li>从不更新活跃时间的连接类型保持豁免，不得因activeRecvTime==0被当作超时立即误杀。</li>
 * </ul>
 */
@Fast
public class TestFnd763WebsocketKeepAlive {
	private static void awaitSecondBoundary(long baseSeconds) throws InterruptedException {
		var deadline = System.currentTimeMillis() + 5_000;
		while (GlobalTimer.getCurrentSeconds() <= baseSeconds) {
			Assertions.assertTrue(System.currentTimeMillis() < deadline, "timeout waiting second boundary");
			//noinspection BusyWait
			Thread.sleep(10);
		}
	}

	// 未知协议不杀连接：服务端回显一个size=0的空协议头（驱动客户端onBinary路径），
	// 客户端吞掉回显（避免回声循环）。
	public static final class EchoWsService extends Service {
		public final AtomicInteger closeCount = new AtomicInteger();

		public EchoWsService(String name) {
			super(name);
		}

		@Override
		public void OnSocketClose(@NotNull AsyncSocket so, @Nullable Throwable e) throws Exception {
			closeCount.incrementAndGet();
			super.OnSocketClose(so, e);
		}

		@Override
		public void dispatchUnknownProtocol(@NotNull AsyncSocket so, int moduleId, int protocolId,
		                                    @NotNull Zeze.Serialize.ByteBuffer data) {
			if (so.getType() == AsyncSocket.Type.eServer)
				so.Send(new byte[12], 0, 12);
		}
	}

	// 修复后：Send/收包（echo回显）后客户端的活跃时间必须前进（秒级粒度，先跨秒边界再收发）。
	@Test
	public void testActiveTimesAdvanceOnRecvAndSend() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new HttpServer();
		try {
			var wsService = new EchoWsService("test.fnd763.echo");
			var handle = new WebsocketHandle("/ws", server);
			handle.setService(wsService);
			handle.start();
			var port = ((InetSocketAddress)server.start(netty, 0).sync().channel().localAddress()).getPort();

			var clientService = new EchoWsService("test.fnd763.client");
			var connector = new Connector(true, "ws://127.0.0.1:" + port + "/ws");
			connector.SetService(clientService);
			try {
				connector.start();
				var so = (WebsocketClient)connector.WaitReady();
				Assertions.assertNotNull(so);

				long base = Math.max(so.getActiveRecvTime(), so.getActiveSendTime());
				awaitSecondBoundary(base); // 时间戳秒级粒度，先跨秒保证后续可观测"前进"

				Assertions.assertTrue(so.Send(new byte[12], 0, 12), "send must be accepted");
				Assertions.assertTrue(so.getActiveSendTime() > base,
						"Send路径必须setActiveSendTime，Got=" + so.getActiveSendTime() + " base=" + base);

				var deadline = System.currentTimeMillis() + 10_000; // 等回显驱动onBinary
				while (so.getActiveRecvTime() <= base && System.currentTimeMillis() < deadline) {
					//noinspection BusyWait
					Thread.sleep(10);
				}
				Assertions.assertTrue(so.getActiveRecvTime() > base,
						"onBinary路径必须setActiveRecvTime，Got=" + so.getActiveRecvTime() + " base=" + base);
			} finally {
				connector.stop();
				clientService.Stop();
			}
		} finally {
			server.close();
			netty.close();
		}
	}

	// 端到端：静默的Websocket服务端连接（对端连上后不发任何数据）必须在KeepRecvTimeout后被
	// checkKeepAlive回收。修复前instanceof TcpSocket过滤把它排除在外，死链永不回收。
	@Test
	public void testIdleServerWebsocketRecycled() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new HttpServer();
		server.setCheckIdleInterval(3600); // 排除HttpServer自身idle检查的干扰
		server.setReadIdleTimeout(3600);
		server.setWriteIdleTimeout(3600);
		try {
			var wsService = new EchoWsService("test.fnd763.idle");
			wsService.getConfig().getHandshakeOptions().setKeepCheckPeriod(1);
			wsService.getConfig().getHandshakeOptions().setKeepRecvTimeout(2);
			wsService.start();
			var handle = new WebsocketHandle("/ws", server);
			handle.setService(wsService);
			handle.start();
			var port = ((InetSocketAddress)server.start(netty, 0).sync().channel().localAddress()).getPort();

			// 原生JDK客户端连上后保持静默（模拟对端静默死亡前最后状态：链路在但无任何数据）
			var ws = HttpClient.newHttpClient().newWebSocketBuilder().buildAsync(
					URI.create("ws://127.0.0.1:" + port + "/ws"), new WebSocket.Listener() {
						@Override
						public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
							webSocket.request(1);
							return null;
						}
					}).get(10, TimeUnit.SECONDS);
			try {
				var deadline = System.currentTimeMillis() + 8_000;
				while (wsService.closeCount.get() == 0 && System.currentTimeMillis() < deadline) {
					//noinspection BusyWait
					Thread.sleep(20);
				}
				Assertions.assertTrue(wsService.closeCount.get() >= 1,
						"静默Websocket必须在KeepRecvTimeout后被checkKeepAlive回收（连接泄漏+死链永不回收）");
			} finally {
				try {
					ws.abort();
				} catch (Throwable ignored) {
				}
				wsService.Stop();
			}
		} finally {
			server.close();
			netty.close();
		}
	}

	// 过滤判据：活跃时间曾被更新（被管理）且过期的连接必须回收；从不更新活跃时间的
	// 连接类型（activeRecvTime==activeSendTime==0）必须豁免，不得被立即误杀。
	private static final class StubSocket extends AsyncSocket {
		final AtomicInteger closedCount = new AtomicInteger();
		private final boolean managed;

		StubSocket(Service service, boolean managed) {
			super(service);
			this.managed = managed;
		}

		@Override
		public Type getType() {
			return Type.eServer;
		}

		@Override
		public @Nullable SocketAddress getRemoteAddress() {
			return null;
		}

		@Override
		public @Nullable TimeThrottle getTimeThrottle() {
			return null;
		}

		@Override
		public boolean isClosed() {
			return closedCount.get() > 0;
		}

		@Override
		public boolean close(@Nullable Throwable ex, boolean gracefully) {
			closedCount.incrementAndGet();
			return true;
		}

		@Override
		public boolean Send(byte @NotNull [] bytes, int offset, int length) {
			return true;
		}

		@Override
		public long getActiveRecvTime() {
			return managed ? 1 : 0; // 被管理的恒旧（必超时）；未管理的恒0（应豁免）
		}

		@Override
		public long getActiveSendTime() {
			return managed ? 1 : 0;
		}
	}

	public static final class ExposeAddSocketService extends Service {
		public ExposeAddSocketService(String name) {
			super(name);
		}

		public void register(AsyncSocket so) {
			addSocket(so);
		}
	}

	@Test
	public void testCheckKeepAliveCoverageAndExemption() throws Exception {
		Task.tryInitThreadPool();
		var service = new ExposeAddSocketService("test.fnd763.filter");
		service.getConfig().getHandshakeOptions().setKeepCheckPeriod(1);
		service.getConfig().getHandshakeOptions().setKeepRecvTimeout(1);
		var managed = new StubSocket(service, true);
		var unmanaged = new StubSocket(service, false);
		service.register(managed);
		service.register(unmanaged);
		service.start();
		try {
			var deadline = System.currentTimeMillis() + 5_000;
			while (managed.closedCount.get() == 0 && System.currentTimeMillis() < deadline) {
				//noinspection BusyWait
				Thread.sleep(20);
			}
			Assertions.assertTrue(managed.closedCount.get() >= 1,
					"活跃时间被管理且已过期的连接必须被回收（非TcpSocket类型不得被排除在回收之外）");

			//noinspection BusyWait
			Thread.sleep(2_500); // 未管理连接的观察窗口：跨越多个检查周期
			Assertions.assertEquals(0, unmanaged.closedCount.get(),
					"从不更新活跃时间的连接类型必须豁免，不得被立即误杀");
		} finally {
			service.Stop();
		}
	}
}
