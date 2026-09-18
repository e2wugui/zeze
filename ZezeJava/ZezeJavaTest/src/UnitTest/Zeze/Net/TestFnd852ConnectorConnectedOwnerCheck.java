package UnitTest.Zeze.Net;

import harness.Fast;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Net.Service;
import Zeze.Net.WebsocketHandle;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Util.TimeThrottle;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND8-52回归：Connector.OnSocketConnected无属主校验（对照OnSocketHandshakeDone:236、
 * OnSocketClose:195）——被stop()废弃的在途连接一旦连上，isConnected被误置true且再无人
 * 纠正（其OnSocketClose因socket==closed不匹配必然跳过stop()）。孪生#1：
 * WebsocketClient.onOpen入口同型（同一方法，同 patched）。
 * 修复后：OnSocketConnected与OnSocketHandshakeDone同一属主判定
 * （socket==so || (socket==null && connecting && !abortConnect)），stale回调不改写状态。
 */
@Fast
public class TestFnd852ConnectorConnectedOwnerCheck {

	/** 最小AsyncSocket桩：OnSocketConnected只读连接器状态，不触达传输。 */
	private static final class StubSocket extends AsyncSocket {
		StubSocket(Service service) {
			super(service);
		}

		@Override
		public Type getType() {
			return Type.eClient;
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
			return false;
		}

		@Override
		public boolean close(@Nullable Throwable ex, boolean gracefully) {
			return true;
		}

		@Override
		public boolean Send(byte @NotNull [] bytes, int offset, int length) {
			return true;
		}
	}

	/** 构造入口设门的Service：把connector保持在connecting窗口内（模拟在途DNS/连接）。 */
	private static final class GatedWsService extends Service {
		final CountDownLatch entered = new CountDownLatch(1);
		final CountDownLatch release = new CountDownLatch(1);

		GatedWsService(String name) {
			super(name);
		}

		@Override
		public @NotNull AsyncSocket newWebsocketClient(@NotNull String url, @Nullable Object userState,
													   @Nullable Connector connector) {
			entered.countDown();
			try {
				release.await();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			return super.newWebsocketClient(url, userState, connector);
		}
	}

	private static void await(String what, int timeoutMillis, java.util.function.BooleanSupplier cond)
			throws InterruptedException {
		long deadline = System.currentTimeMillis() + timeoutMillis;
		while (!cond.getAsBoolean()) {
			Assertions.assertTrue(System.currentTimeMillis() < deadline, "timeout waiting: " + what);
			//noinspection BusyWait
			Thread.sleep(5);
		}
	}

	// T1-T5触发链：stop废弃在途连接（abortConnect）后，stale的OnSocketConnected不得置isConnected
	@Test
	public void testStaleConnectedAfterStopRejected() throws Exception {
		Task.tryInitThreadPool();
		var client = new GatedWsService("test.fnd852.a");
		var connector = new Connector(true, "ws://127.0.0.1:1/dead");
		connector.SetService(client);
		try {
			var t1 = new Thread(connector::start, "fnd852-start");
			t1.start();
			Assertions.assertTrue(client.entered.await(10, TimeUnit.SECONDS), "connecting window not entered");
			Assertions.assertTrue(connector.isConnected() == false);

			connector.stop(); // T2: 废弃在途构造（abortConnect=true、isConnected=false）
			// T3: 该废弃连接实际连上 → 框架回调OnSocketConnected（公开回调，直调模拟同一契约）
			connector.OnSocketConnected(new StubSocket(client));
			Assertions.assertFalse(connector.isConnected(),
					"被stop废弃的stale连接不得置isConnected=true（误置后无人纠正）");

			client.release.countDown();
			t1.join(15_000);
			Assertions.assertFalse(t1.isAlive(), "in-flight start must return");
			Assertions.assertFalse(connector.isConnected(), "丢弃尾段后isConnected仍须为false");
		} finally {
			client.release.countDown();
			connector.stop();
			client.Stop();
		}
	}

	// 合法时序(ii)：connecting窗口内（未被stop，abortConnect=false）的连接回调必须放行
	@Test
	public void testConnectingWindowConnectedAccepted() throws Exception {
		Task.tryInitThreadPool();
		var client = new GatedWsService("test.fnd852.b");
		var connector = new Connector(true, "ws://127.0.0.1:1/dead");
		connector.SetService(client);
		try {
			var t1 = new Thread(connector::start, "fnd852-start-b");
			t1.start();
			Assertions.assertTrue(client.entered.await(10, TimeUnit.SECONDS), "connecting window not entered");

			// 构造内立即连上/OP_CONNECT先于第二锁段：socket==null && connecting && !abortConnect
			connector.OnSocketConnected(new StubSocket(client));
			Assertions.assertTrue(connector.isConnected(), "connecting窗口内的合法连接回调必须放行");

			client.release.countDown();
			t1.join(15_000);
		} finally {
			client.release.countDown();
			connector.stop();
			client.Stop();
		}
	}

	// 合法时序(i)：真实连接（url型Connector的WebsocketClient.onOpen路径，即孪生#1入口）
	// 发布后socket==so必须放行；closedInWindow（socket==null且connecting==false）必须拒绝
	@Test
	public void testOwnerSocketAcceptedAndIdleRejected() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new HttpServer();
		try {
			var wsService = new Service("test.fnd852.server");
			var handle = new WebsocketHandle("/ws", server);
			handle.setService(wsService);
			handle.start();
			var port = ((InetSocketAddress)server.start(netty, 0).sync().channel().localAddress()).getPort();

			var client = new Service("test.fnd852.client");
			var connector = new Connector(true, "ws://127.0.0.1:" + port + "/ws");
			connector.SetService(client);
			try {
				connector.start();
				AsyncSocket so = null;
				long deadline = System.currentTimeMillis() + 15_000;
				while (so == null && System.currentTimeMillis() < deadline) {
					so = connector.TryGetReadySocket();
					if (so == null)
						//noinspection BusyWait
						Thread.sleep(5);
				}
				Assertions.assertNotNull(so, "websocket连接未就绪");

				// 已发布的owner连接回调（重连成功后OnSocketConnected时socket==so）：放行
				connector.OnSocketConnected(so);
				Assertions.assertTrue(connector.isConnected(), "owner连接的回调必须放行");

				// closedInWindow窗口（socket==null且connecting==false）：迟到的Connected必属
				// 已死连接，拒绝置位
				connector.stop();
				Assertions.assertFalse(connector.isConnected());
				connector.OnSocketConnected(so); // 已被stop弃置的连接
				Assertions.assertFalse(connector.isConnected(), "非owner的stale回调不得置位");
			} finally {
				connector.stop();
				client.Stop();
				wsService.Stop();
			}
		} finally {
			server.close();
			netty.close();
		}
	}
}
