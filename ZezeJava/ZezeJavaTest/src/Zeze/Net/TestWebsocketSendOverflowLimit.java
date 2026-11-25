package Zeze.Net;

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
import org.jspecify.annotations.Nullable;
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
		public final CountDownLatch handshaked = new CountDownLatch(1);

		CaptureService(String name) {
			super(name);
		}

		@Override
		public void OnHandshakeDone(AsyncSocket so) throws Exception {
			if (so instanceof Websocket ws) {
				lastHandshaked = ws;
				handshaked.countDown();
			}
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
						public @Nullable CompletionStage<?> onBinary(WebSocket webSocket, java.nio.ByteBuffer data, boolean last) {
							webSocket.request(1);
							return null;
						}
					}).join();
			try {
				Assertions.assertTrue(opened.await(10, TimeUnit.SECONDS));
				// 客户端onOpen（收到101）不保证服务端OnHandshakeDone已派发完成：Netty写101与
				// Zeze握手注册链（onOpen→tryAccept）跨线程，负载下窗口放大（soak-20261116-1508
				// R1实锤null）。等待服务端握手同步点再取会话。
				Assertions.assertTrue(service.handshaked.await(10, TimeUnit.SECONDS), "服务端Websocket必须建立");
				var serverSide = service.lastHandshaked;
				Assertions.assertNotNull(serverSide, "服务端Websocket必须建立");
				Assertions.assertFalse(serverSide.isClosed());

				// 单帧5000 > 4096：必须被checkOverflow拒绝（修复前恒接受，无任何上限），且不虚增发送计数
				long sendCountBefore = serverSide.getSendCount();
				long sendSizeBefore = serverSide.getSendSize();
				Assertions.assertFalse(serverSide.Send(new byte[MaxOutput + 1000], 0, MaxOutput + 1000),
						"超过OutputBufferMaxSize的发送必须返回false");
				Assertions.assertEquals(sendCountBefore, serverSide.getSendCount(), "超限拒绝不得增加sendCount");
				Assertions.assertEquals(sendSizeBefore, serverSide.getSendSize(), "超限拒绝不得增加sendSize");
				// 上限内发送不受影响（连接仍可用）
				Assertions.assertTrue(serverSide.Send(new byte[100], 0, 100),
						"上限内发送必须照常接受");
				Assertions.assertEquals(sendCountBefore + 1, serverSide.getSendCount());
				Assertions.assertEquals(sendSizeBefore + 100, serverSide.getSendSize());
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

	@Test
	public void testSendAfterCloseReturnsFalse() throws Exception {
		Task.tryInitThreadPool();
		for (boolean gracefully : new boolean[]{false, true}) {
			var netty = new Netty(1);
			var server = new HttpServer();
			try {
				var service = new CaptureService("test.wssendclosed." + gracefully);
				service.start();
				var handle = new WebsocketHandle("/wsclose", server);
				handle.setService(service);
				handle.start();
				var port = ((InetSocketAddress)server.start(netty, 0).sync().channel().localAddress()).getPort();

				var opened = new CountDownLatch(1);
				var ws = HttpClient.newHttpClient().newWebSocketBuilder().buildAsync(
						URI.create("ws://127.0.0.1:" + port + "/wsclose"), new WebSocket.Listener() {
							@Override
							public void onOpen(WebSocket webSocket) {
								opened.countDown();
								webSocket.request(1);
							}
						}).join();
				try {
					Assertions.assertTrue(opened.await(10, TimeUnit.SECONDS));
					Assertions.assertTrue(service.handshaked.await(10, TimeUnit.SECONDS), "服务端Websocket必须建立");
					var serverSide = service.lastHandshaked;
					Assertions.assertNotNull(serverSide);

					Assertions.assertThrows(IllegalArgumentException.class,
							() -> serverSide.Send(new byte[4], 2, 5), "越界参数须抛IllegalArgumentException");

					long countBefore = serverSide.getSendCount();
					long sizeBefore = serverSide.getSendSize();
					if (gracefully)
						serverSide.closeGracefully();
					else
						serverSide.close(null);

					Assertions.assertTrue(serverSide.isClosed());
					Assertions.assertFalse(serverSide.Send(new byte[]{1, 2, 3}, 0, 3),
							"已关闭的Websocket.Send必须返回false (gracefully=" + gracefully + ")");
					Assertions.assertEquals(countBefore, serverSide.getSendCount(), "已关闭连接不得增加sendCount");
					Assertions.assertEquals(sizeBefore, serverSide.getSendSize(), "已关闭连接不得增加sendSize");
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
}
