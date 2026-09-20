package UnitTest.Zeze.Net;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Net.Service;
import Zeze.Net.WebsocketHandle;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Util.Task;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * N3-F1回归：Websocket.processInput解码异常（未知协议等）后连接不关闭且输入缓冲永不Compact，
 * 远程可触发无界内存增长。修复：解码异常catch后close，对齐TcpSocket/WebsocketClient断连语义。
 * 客户端发送指向未注册协议的二进制帧，服务端必须关闭连接（客户端观察到onError/onClose）
 * 并回调OnSocketClose（不泄漏socketMap条目）。
 */
@Fast
public class TestWebsocketDecodeFailureCloses {

	public static final class CountingService extends Service {
		public final AtomicInteger closeCount = new AtomicInteger();

		public CountingService(String name) {
			super(name);
		}

		@Override
		public void OnSocketClose(@NotNull AsyncSocket so, java.lang.Throwable e) {
			closeCount.incrementAndGet();
			try {
				super.OnSocketClose(so, e);
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
	}

	// 未知协议帧：moduleId=99, protocolId=99, size=0（帧头完整、类型未注册）
	private static byte[] unknownProtocolFrame() {
		var bb = Zeze.Serialize.ByteBuffer.Allocate(Protocol.HEADER_SIZE);
		bb.WriteInt4(99);
		bb.WriteInt4(99);
		bb.WriteInt4(0);
		return Arrays.copyOfRange(bb.Bytes, bb.ReadIndex, bb.WriteIndex);
	}

	@Test
	public void testUnknownProtocolClosesConnection() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new HttpServer();
		try {
			var serverService = new CountingService("test.wsdecodefail");
			serverService.start();
			var handle = new WebsocketHandle("/wsbad", server);
			handle.setService(serverService);
			handle.start();
			var port = ((InetSocketAddress)server.start(netty, 0).sync().channel().localAddress()).getPort();

			var closed = new CountDownLatch(1);
			var ws = HttpClient.newHttpClient().newWebSocketBuilder().buildAsync(
					URI.create("ws://127.0.0.1:" + port + "/wsbad"), new WebSocket.Listener() {
						@Override
						public void onOpen(WebSocket webSocket) {
							webSocket.request(1);
						}

						@Override
						public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
							webSocket.request(1);
							return null;
						}

						@Override
						public void onError(WebSocket webSocket, Throwable error) {
							closed.countDown();
						}

						@Override
						public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
							closed.countDown();
							return null;
						}
					}).join();
			try {
				ws.sendBinary(ByteBuffer.wrap(unknownProtocolFrame()), true).get(10, TimeUnit.SECONDS);
				// 修复前：解码异常上抛无人处置，连接保持打开——此处超时失败；
				// 修复后：processInput catch后close，客户端观察到断连。
				Assertions.assertTrue(closed.await(10, TimeUnit.SECONDS), "解码失败必须断连（客户端可感知）");
				Assertions.assertTrue(serverService.closeCount.get() >= 1, "服务端必须回调OnSocketClose");
				Assertions.assertEquals(0, serverService.getSocketCount(), "socketMap不得残留条目");
			} finally {
				try {
					ws.abort();
				} catch (Throwable ignored) {
				}
				serverService.stop();
			}
		} finally {
			server.close();
			netty.close();
		}
	}
}
