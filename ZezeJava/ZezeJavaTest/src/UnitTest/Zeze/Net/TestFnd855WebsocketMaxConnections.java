package UnitTest.Zeze.Net;

import harness.Fast;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Net.Service;
import Zeze.Net.ServiceConf;
import Zeze.Net.WebsocketHandle;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND8-55回归：WebsocketHandle.onOpen直接addSocket+OnHandshakeDone——websocket接受路径
 * 完全不受maxConnections约束（对纯websocket形态的Service是100%死配置），且addSocket
 * 撞号false被忽略仍回调OnHandshakeDone（违反addSocket契约）。
 * 修复后：Service.tryAccept单一入口（限流→haProxy→注册→握手完成），TCP与websocket
 * 两路统一；超限显式关闭；孪生1-4（WebsocketClient.onOpen、Token两快速路径、
 * Handshake家族）addSocket返回值全部按契约短路。
 */
@Fast
public class TestFnd855WebsocketMaxConnections {

	static final class HandshakeCountService extends Service {
		final AtomicInteger handshakeDone = new AtomicInteger();

		HandshakeCountService(String name) {
			super(name);
		}

		@Override
		public void OnHandshakeDone(@NotNull AsyncSocket so) {
			handshakeDone.incrementAndGet();
		}
	}

	private static void setMaxConnections(Service service, int limit) throws Exception {
		// ServiceConf未提供setter，测试经反射设置（与XML配置等价）
		Field f = ServiceConf.class.getDeclaredField("maxConnections");
		f.setAccessible(true);
		f.setInt(service.getConfig(), limit);
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

	private static WebSocket.Listener silentListener(CountDownLatch closed) {
		return new WebSocket.Listener() {
			@Override
			public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
				webSocket.request(1);
				return null;
			}

			@Override
			public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
				if (closed != null)
					closed.countDown();
				return null;
			}

			@Override
			public void onError(WebSocket webSocket, Throwable error) {
				if (closed != null)
					closed.countDown();
			}
		};
	}

	// 服务端：maxConnections=1下第2条websocket升级连接必须被拒——不入socketMap、
	// 不回调OnHandshakeDone、连接被显式关闭（修复前全部照常接受）
	@Test
	public void testServerWebsocketOverLimitRejected() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new HttpServer();
		try {
			var wsService = new HandshakeCountService("test.fnd855.server");
			setMaxConnections(wsService, 1);
			var handle = new WebsocketHandle("/ws", server);
			handle.setService(wsService);
			handle.start();
			var port = ((InetSocketAddress)server.start(netty, 0).sync().channel().localAddress()).getPort();

			var ws1 = HttpClient.newHttpClient().newWebSocketBuilder().buildAsync(
					URI.create("ws://127.0.0.1:" + port + "/ws"), silentListener(null)).get(10, TimeUnit.SECONDS);
			try {
				await("first websocket accepted", 10_000,
						() -> wsService.getSocketCount() == 1 && wsService.handshakeDone.get() == 1);

				var closed2 = new CountDownLatch(1);
				var ws2 = HttpClient.newHttpClient().newWebSocketBuilder().buildAsync(
						URI.create("ws://127.0.0.1:" + port + "/ws"), silentListener(closed2))
						.get(10, TimeUnit.SECONDS);
				try {
					Assertions.assertTrue(closed2.await(10, TimeUnit.SECONDS),
							"超限websocket必须被服务端显式关闭");
					Assertions.assertEquals(1, wsService.getSocketCount(),
							"超限连接不得进入socketMap（修复前突破maxConnections）");
					Assertions.assertEquals(1, wsService.handshakeDone.get(),
							"超限连接不得回调OnHandshakeDone");
				} finally {
					try {
						ws2.abort();
					} catch (Throwable ignored) {
					}
				}
			} finally {
				try {
					ws1.abort();
				} catch (Throwable ignored) {
				}
				wsService.stop();
			}
		} finally {
			server.close();
			netty.close();
		}
	}

	// 回归红线：TCP接受路径（OnSocketAccept→tryAccept）限流语义不变
	@Test
	public void testTcpAcceptLimitStillEnforced() throws Exception {
		Task.tryInitThreadPool();
		var server = new HandshakeCountService("test.fnd855.tcp");
		setMaxConnections(server, 1);
		var listen = (Zeze.Net.TcpSocket)server.newServerSocket("127.0.0.1", 0, null);
		var local = listen.getLocalInet();
		Assertions.assertNotNull(local, "listen socket local address");
		var port = local.getPort();

		var client = new Service("test.fnd855.tcpclient");
		try {
			// 顺序确定化：8a22f09b5建连异步化后两条newClientSocket的TCP完成序不再随调用序
			//（resolver线程竞速，20轮压测18/20批实测so2可先完成被接受占位、so1反遭拒绝——
			// "second rejected & closed"等的其实是永不关闭的幸存者so2）。先等so1真正占位
			//（socketCount==1即握手完成入表）再发起so2，so2必为超限的第二条。
			var so1 = client.newClientSocket("127.0.0.1", port, null, null);
			Assertions.assertNotNull(so1);
			await("first accepted", 10_000, () -> server.getSocketCount() == 1);
			var so2 = client.newClientSocket("127.0.0.1", port, null, null);
			Assertions.assertNotNull(so2);
			// 第2条超限：服务端accept流程抛ISE关闭——客户端侧观察到连接被关
			await("second rejected & closed", 10_000, so2::isClosed);
			Assertions.assertEquals(1, server.getSocketCount(), "TCP路径限流不得回归");
			Assertions.assertEquals(1, server.handshakeDone.get());
			so1.close(null);
		} finally {
			client.stop();
			server.stop();
		}
	}

	// 孪生#1：客户端WebsocketClient.onOpen入口同经tryAccept限流
	@Test
	public void testClientWebsocketOverLimitRejected() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new HttpServer();
		try {
			var serverService = new Service("test.fnd855.wsserver");
			var handle = new WebsocketHandle("/ws", server);
			handle.setService(serverService);
			handle.start();
			var port = ((InetSocketAddress)server.start(netty, 0).sync().channel().localAddress()).getPort();

			var clientService = new HandshakeCountService("test.fnd855.wsclient");
			setMaxConnections(clientService, 1);
			var c1 = new Connector(true, "ws://127.0.0.1:" + port + "/ws");
			c1.SetService(clientService);
			var c2 = new Connector(true, "ws://127.0.0.1:" + port + "/ws");
			c2.SetService(clientService);
			try {
				c1.start();
				await("first client websocket registered", 10_000,
						() -> clientService.getSocketCount() == 1 && clientService.handshakeDone.get() == 1);

				c2.start();
				// 超限：onOpen显式关闭，futureSocket以异常完成——WaitReady抛CompletionException
				Assertions.assertThrows(Exception.class, c2::WaitReady,
						"超限的客户端websocket必须被拒绝（修复前照常接受）");
				Assertions.assertEquals(1, clientService.getSocketCount(),
						"客户端socketMap不得突破maxConnections");
				Assertions.assertEquals(1, clientService.handshakeDone.get());
			} finally {
				c1.stop();
				c2.stop();
				clientService.stop();
				serverService.stop();
			}
		} finally {
			server.close();
			netty.close();
		}
	}
}
