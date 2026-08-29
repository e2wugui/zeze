package UnitTest.Zeze.Net;

import harness.Fast;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Net.Service;
import Zeze.Netty.HttpServer;
import Zeze.Netty.HttpWebSocketHandle;
import Zeze.Netty.Netty;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Fast
public class TestWebsocketClient {
	private static final long TIMEOUT_MS = 10_000;

	public static final class TestWsService extends Service {
		public final AtomicInteger closeCount = new AtomicInteger();

		public TestWsService() {
			super("test.ws.client.service");
		}

		@Override
		public void OnSocketClose(@NotNull AsyncSocket so, @Nullable Throwable e) throws Exception {
			closeCount.incrementAndGet();
			super.OnSocketClose(so, e);
		}
	}

	// N-1缺陷①: 握手失败(死端口)时buildAsync的future被丢弃,close()永不被调用,
	// Connector.socket永远非null导致TryReconnect直接return,重连循环永远不启动。
	// 修复后: 每次握手失败都走close() -> OnSocketClose,重连按1s/2s退避持续推进。
	@Test
	public void testHandshakeFailureReconnect() throws Exception {
		Task.tryInitThreadPool();
		var service = new TestWsService();
		var connector = new Connector(true, "ws://127.0.0.1:1/deadPort"); // 立即连接拒绝
		connector.SetService(service);
		try {
			connector.start();
			long deadline = System.currentTimeMillis() + TIMEOUT_MS;
			while (service.closeCount.get() < 2 && System.currentTimeMillis() < deadline) {
				//noinspection BusyWait
				Thread.sleep(10);
			}
			Assertions.assertTrue(service.closeCount.get() >= 2,
					"closeCount=" + service.closeCount.get() + ", handshake failure not closed/reconnected");
		} finally {
			connector.stop();
			service.Stop();
		}
	}

	// N-1缺陷②: close()只通知Service不通知Connector(对照TcpSocket两者都通知),
	// 即使握手成功后断线,Connector.OnSocketClose不被调用,futureSocket永不刷新,断线后永不重连。
	// 修复后: close()通知Connector -> stop+TryReconnect -> 1s后重连成功,新socket握手完成。
	@Test
	public void testDisconnectReconnect() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new HttpServer();
		try {
			server.addHandler("/ws", TransactionLevel.Serializable, DispatchMode.Direct,
					new HttpWebSocketHandle() {
					});
			var port = ((InetSocketAddress)server.start(netty, 0).sync().channel().localAddress()).getPort();

			var service = new TestWsService();
			var connector = new Connector(true, "ws://127.0.0.1:" + port + "/ws");
			connector.SetService(service);
			try {
				connector.start();
				var so = connector.WaitReady(); // 连接并握手成功
				Assertions.assertNotNull(so);
				so.close(new IOException("test disconnect")); // 模拟断线
				long deadline = System.currentTimeMillis() + TIMEOUT_MS;
				AsyncSocket reconnected = null;
				while (System.currentTimeMillis() < deadline) {
					var so2 = connector.TryGetReadySocket();
					if (so2 != null && so2 != so) {
						reconnected = so2;
						break;
					}
					//noinspection BusyWait
					Thread.sleep(10);
				}
				Assertions.assertNotNull(reconnected, "no reconnect after disconnect");
			} finally {
				connector.stop();
				service.Stop();
			}
		} finally {
			server.close();
			netty.close();
		}
	}
}
