package UnitTest.Zeze.Net;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Net.Service;
import Zeze.Net.Websocket;
import Zeze.Net.WebsocketClient;
import Zeze.Net.WebsocketHandle;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Util.Task;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * N3-F3回归：WebsocketClient.Send的sendChain无界排队（对端零窗口时链不前进，排队节点钉住
 * 调用方byte[]在途无界驻留）。修复：在途字节（入队加/链节点完成减）对OutputBufferMaxSize
 * 设限，超限返回false。N3-F4回归：WebsocketClient全流量计数器恒0，Service统计漏掉websocket
 * 客户端流量——onBinary补recv计数、Send补send计数。
 */
@Fast
public class TestWebsocketClientSendLimit {

	private static final int MaxOutput = 4096;

	/** sendBinary首次调用返回可手工完成的future（模拟对端零窗口/慢速在途），此后立即完成 */
	private static final class PendingStubWebSocket implements WebSocket {
		volatile @Nullable CompletableFuture<WebSocket> pending;

		@Override
		public CompletableFuture<WebSocket> sendText(CharSequence data, boolean last) {
			return failed();
		}

		@Override
		public CompletableFuture<WebSocket> sendBinary(ByteBuffer data, boolean last) {
			var p = pending;
			if (p == null)
				pending = p = new CompletableFuture<>();
			else
				return CompletableFuture.completedFuture(this); // 后续调用立即完成：链排空时不再挂起
			return p;
		}

		static CompletableFuture<WebSocket> failed() {
			return CompletableFuture.failedFuture(new java.io.IOException("stub"));
		}

		@Override
		public CompletableFuture<WebSocket> sendPing(ByteBuffer message) {
			return failed();
		}

		@Override
		public CompletableFuture<WebSocket> sendPong(ByteBuffer message) {
			return failed();
		}

		@Override
		public CompletableFuture<WebSocket> sendClose(int statusCode, String reason) {
			return failed();
		}

		@Override
		public void request(long n) {
		}

		@Override
		public String getSubprotocol() {
			return "";
		}

		@Override
		public boolean isOutputClosed() {
			return false;
		}

		@Override
		public boolean isInputClosed() {
			return false;
		}

		@Override
		public void abort() {
		}
	}

	private static void injectWebSocket(@NotNull WebsocketClient so, @NotNull WebSocket stub) throws Exception {
		Field field = WebsocketClient.class.getDeclaredField("webSocket");
		field.setAccessible(true);
		field.set(so, stub);
	}

	// N3-F3：在途字节超过OutputBufferMaxSize后排队Send必须返回false；链排空后配额恢复
	@Test
	public void testQueuedSendOverInflightLimitReturnsFalse() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new HttpServer();
		try {
			server.addHandler("/ws", Zeze.Transaction.TransactionLevel.Serializable,
					Zeze.Transaction.DispatchMode.Direct, new Zeze.Netty.HttpWebSocketHandle() {
					});
			var port = ((InetSocketAddress)server.start(netty, 0).sync().channel().localAddress()).getPort();

			var service = new Service("test.wsc.sendlimit");
			service.getSocketOptions().setOutputBufferMaxSize(MaxOutput);
			var connector = new Connector(true, "ws://127.0.0.1:" + port + "/ws");
			connector.SetService(service);
			try {
				connector.start();
				var so = (WebsocketClient)connector.WaitReady();
				Assertions.assertNotNull(so);
				var stub = new PendingStubWebSocket();
				injectWebSocket(so, stub);

				// 第1次Send走直发路径，sendBinary挂起（模拟在途未完成）
				Assertions.assertTrue(so.Send(new byte[1024], 0, 1024));
				// 后续Send进入排队路径：在途字节累计，未超限前接受
				Assertions.assertTrue(so.Send(new byte[1024], 0, 1024), "在途2048<=4096，应接受");
				Assertions.assertTrue(so.Send(new byte[1024], 0, 1024), "在途3072<=4096，应接受");
				// 恰达边界（4096==4096，<=语义与TcpSocket一致）同样接受
				Assertions.assertTrue(so.Send(new byte[1024], 0, 1024), "在途恰达4096==上限，应接受");
				// 第5次：在途将达5120>4096，必须拒绝（修复前恒接受，无界排队）
				Assertions.assertFalse(so.Send(new byte[1024], 0, 1024), "在途超OutputBufferMaxSize的排队Send必须返回false");

				// 链排空（future完成，handle释放配额）后恢复
				var p = stub.pending;
				Assertions.assertNotNull(p);
				p.complete(stub);
				awaitCond(10_000, () -> so.Send(new byte[1024], 0, 1024), "配额释放后Send必须恢复接受");
			} finally {
				connector.stop();
				service.stop();
			}
		} finally {
			server.close();
			netty.close();
		}
	}

	// N3-F4：客户端收发计数不再恒0（onBinary补recv、Send补send）
	@Test
	public void testTrafficCounters() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new HttpServer();
		try {
			var service = new CaptureService("test.wsc.counters");
			var handle = new WebsocketHandle("/wscount", server);
			handle.setService(service);
			service.start();
			handle.start();
			var port = ((InetSocketAddress)server.start(netty, 0).sync().channel().localAddress()).getPort();

			var clientService = new Service("test.wsc.counters.client");
			var connector = new Connector(true, "ws://127.0.0.1:" + port + "/wscount");
			connector.SetService(clientService);
			try {
				connector.start();
				var so = (WebsocketClient)connector.WaitReady();
				Assertions.assertNotNull(so);

				// send侧：Send后计数必须可见（<HEADER_SIZE的推送不触发服务端协议解码）
				Assertions.assertEquals(0, so.getSendCount());
				Assertions.assertTrue(so.Send(new byte[8], 0, 8));
				awaitCond(10_000, () -> so.getSendCount() == 1 && so.getSendSize() == 8,
						"Send必须计入sendCount/sendSize");

				// recv侧：服务端下发后onBinary必须计入recv（<HEADER_SIZE的推送不触发协议解码）
				var serverSide = service.lastHandshaked;
				Assertions.assertNotNull(serverSide);
				Assertions.assertTrue(serverSide.Send(new byte[8], 0, 8));
				awaitCond(10_000, () -> so.getRecvCount() >= 1 && so.getRecvSize() >= 8,
						"onBinary必须计入recvCount/recvSize");
			} finally {
				connector.stop();
				clientService.stop();
				service.stop();
			}
		} finally {
			server.close();
			netty.close();
		}
	}

	public static final class CaptureService extends Service {
		public volatile @Nullable Websocket lastHandshaked;

		public CaptureService(String name) {
			super(name);
		}

		@Override
		public void OnHandshakeDone(@NotNull AsyncSocket so) throws Exception {
			if (so instanceof Websocket ws)
				lastHandshaked = ws;
			super.OnHandshakeDone(so);
		}
	}

	private static void awaitCond(int timeoutMillis, java.util.function.BooleanSupplier cond, String what)
			throws InterruptedException {
		long deadline = System.currentTimeMillis() + timeoutMillis;
		while (!cond.getAsBoolean()) {
			Assertions.assertTrue(System.currentTimeMillis() < deadline, "timeout waiting: " + what);
			//noinspection BusyWait
			Thread.sleep(5);
		}
	}
}
