package UnitTest.Zeze.Net;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import Zeze.Net.AsyncSocket;
import Zeze.Net.Service;
import Zeze.Net.Websocket;
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
 * N3-F2回归：Websocket.Send无发送堆积上限，OutputBufferMaxSize对websocket服务端连接失效
 * （checkOverflow唯一调用方是TcpSocket.Send）。修复：Send以channel在途字节+本帧为newSize
 * 调checkOverflow，超限返回false（与TcpSocket.Send同语义）。
 * 单帧即超过上限的确定性路径：OutputBufferMaxSize=4096时，5000字节Send必须被拒。
 */
@Fast
public class TestWebsocketSendOverflowLimit {

	private static final int MaxOutput = 4096;

	public static final class CaptureService extends Service {
		public volatile @Nullable Websocket lastHandshaked;

		CaptureService(String name) {
			super(name);
		}

		@Override
		public void OnHandshakeDone(@NotNull AsyncSocket so) throws Exception {
			if (so instanceof Websocket ws)
				lastHandshaked = ws;
			super.OnHandshakeDone(so);
		}
	}

	@Test
	public void testSendOverOutputBufferMaxSizeReturnsFalse() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new HttpServer();
		try {
			var service = new CaptureService("test.wssendoverflow");
			service.getSocketOptions().setOutputBufferMaxSize(MaxOutput);
			service.start();
			var handle = new WebsocketHandle("/wssend", server);
			handle.setService(service);
			handle.start();
			var port = ((InetSocketAddress)server.start(netty, 0).sync().channel().localAddress()).getPort();

			var opened = new CountDownLatch(1);
			var ws = HttpClient.newHttpClient().newWebSocketBuilder().buildAsync(
					URI.create("ws://127.0.0.1:" + port + "/wssend"), new WebSocket.Listener() {
						@Override
						public void onOpen(WebSocket webSocket) {
							opened.countDown();
							webSocket.request(1);
						}

						@Override
						public CompletionStage<?> onBinary(WebSocket webSocket, java.nio.ByteBuffer data, boolean last) {
							webSocket.request(1);
							return null;
						}
					}).join();
			try {
				Assertions.assertTrue(opened.await(10, TimeUnit.SECONDS));
				var serverSide = service.lastHandshaked;
				Assertions.assertNotNull(serverSide, "服务端Websocket必须建立");
				Assertions.assertFalse(serverSide.isClosed());

				// 单帧5000 > 4096：必须被checkOverflow拒绝（修复前恒接受，无任何上限）
				Assertions.assertFalse(serverSide.Send(new byte[MaxOutput + 1000], 0, MaxOutput + 1000),
						"超过OutputBufferMaxSize的发送必须返回false");
				// 上限内发送不受影响（连接仍可用）
				Assertions.assertTrue(serverSide.Send(new byte[100], 0, 100),
						"上限内发送必须照常接受");
				Assertions.assertFalse(serverSide.isClosed());
			} finally {
				try {
					ws.abort();
				} catch (Throwable ignored) {
				}
				service.stop();
			}
		} finally {
			server.close();
			netty.close();
		}
	}
}
