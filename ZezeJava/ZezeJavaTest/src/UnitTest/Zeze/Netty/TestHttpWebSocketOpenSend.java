package UnitTest.Zeze.Netty;

import harness.Fast;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.Net.Service;
import Zeze.Net.Websocket;
import Zeze.Netty.HttpExchange;
import Zeze.Netty.HttpServer;
import Zeze.Netty.HttpWebSocketHandle;
import Zeze.Netty.Netty;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * FND3-24回归:onOpen在Netty握手完成前派发。修复前onOpen先于握手执行,101应答未写出、
 * HttpResponseEncoder未被替换为WebSocket帧编码器,onOpen内sendWebSocket的帧写入HTTP出站
 * 编码路径,写失败(unsupported message type);Websocket.Send丢弃写future恒返回true,
 * Direct模式(WebsocketHandle.start的默认注册方式,onOpen内联执行)下首条消息确定性静默丢失。
 * 修复后onOpen在HandshakeComplete用户事件之后派发,onOpen内发送的消息必须到达客户端;
 * Websocket.Send检查写回执,写失败必须返回false并关闭连接。
 */
@Fast
public class TestHttpWebSocketOpenSend {
	private static final long TIMEOUT_MS = 10_000;
	private static Netty netty;
	private static HttpServer server;
	private static Service service;
	private static int port;
	private static final AtomicReference<Boolean> openSendResult = new AtomicReference<>();
	private static final AtomicReference<HttpExchange> closedX = new AtomicReference<>();
	private static final AtomicReference<Websocket> closedWs = new AtomicReference<>();

	@BeforeAll
	public static void setUp() throws Exception {
		Task.tryInitThreadPool();
		netty = new Netty(1);
		server = new HttpServer(); // 无zeze,noProcedure
		service = new Service("test.ws.open.send");
		// Direct模式+onOpen内立即发送:正是WebsocketHandle.start的注册方式与
		// OnHandshakeDone"握手完成后立即发送握手协议"的用法,修复前必丢首条消息
		server.addHandler("/direct", 2048, TransactionLevel.None, DispatchMode.Direct, new HttpWebSocketHandle() {
			@Override
			public void onOpen(HttpExchange x) {
				var websocket = new Websocket(x, service);
				openSendResult.set(websocket.Send("hello-from-onopen".getBytes(StandardCharsets.UTF_8)));
			}
		});
		server.addHandler("/close", 2048, TransactionLevel.None, DispatchMode.Direct, new HttpWebSocketHandle() {
			@Override
			public void onOpen(HttpExchange x) {
				closedX.set(x);
				closedWs.set(new Websocket(x, service));
			}
		});
		var channel = server.start(netty, 0).sync().channel();
		port = ((InetSocketAddress)channel.localAddress()).getPort();
	}

	@AfterAll
	public static void tearDown() throws Exception {
		server.close();
		netty.close();
		service.Stop();
	}

	// 修复前:握手成功但onOpen内发送的帧永远不到达(写入HTTP出站管线失败被静默丢弃),本用例等满超时失败
	@Test
	public void testDirectModeSendInOnOpenArrives() throws Exception {
		var listener = new BinaryListener();
		var ws = HttpClient.newHttpClient().newWebSocketBuilder()
				.buildAsync(URI.create("ws://127.0.0.1:" + port + "/direct"), listener).join();
		try {
			Assertions.assertTrue(listener.binary.await(TIMEOUT_MS, TimeUnit.MILLISECONDS),
					"message sent in onOpen never arrived: frame lost in http outbound pipeline");
			Assertions.assertArrayEquals("hello-from-onopen".getBytes(StandardCharsets.UTF_8), listener.binaryRef.get());
			Assertions.assertEquals(Boolean.TRUE, openSendResult.get(), "Websocket.Send returned false in onOpen");
			ws.sendClose(WebSocket.NORMAL_CLOSURE, "").get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
		} finally {
			abort(ws);
		}
	}

	// FND3-24第二半:Send不能恒返回true。连接关闭后写future同步失败,Send必须返回false。
	// 在EventLoop上提交发送:写失败同步完成,结果确定。
	@Test
	public void testSendAfterCloseReturnsFalse() throws Exception {
		var ws = HttpClient.newHttpClient().newWebSocketBuilder()
				.buildAsync(URI.create("ws://127.0.0.1:" + port + "/close"), new BinaryListener()).join();
		try {
			var x = awaitRef(closedX);
			x.closeConnectionNow();
			Assertions.assertTrue(x.channel().closeFuture().await(TIMEOUT_MS, TimeUnit.MILLISECONDS),
					"channel not closed");
			var websocket = closedWs.get();
			Boolean r = x.channel().eventLoop().submit(() ->
					websocket.Send("after-close".getBytes(StandardCharsets.UTF_8))).get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
			Assertions.assertEquals(Boolean.FALSE, r, "Send must return false when write fails");
		} finally {
			abort(ws);
		}
	}

	private static <T> T awaitRef(AtomicReference<T> ref) throws InterruptedException {
		var deadline = System.currentTimeMillis() + TIMEOUT_MS;
		T v;
		while ((v = ref.get()) == null && System.currentTimeMillis() < deadline)
			//noinspection BusyWait
			Thread.sleep(10);
		Assertions.assertNotNull(v, "onOpen not called");
		return v;
	}

	// Websocket.Send(byte[])发送的是二进制帧,客户端走onBinary回调
	private static final class BinaryListener implements WebSocket.Listener {
		final CountDownLatch binary = new CountDownLatch(1);
		final AtomicReference<byte[]> binaryRef = new AtomicReference<>();

		@Override
		public CompletionStage<?> onBinary(WebSocket ws, ByteBuffer message, boolean last) {
			var b = new byte[message.remaining()];
			message.get(b);
			if (binaryRef.compareAndSet(null, b))
				binary.countDown();
			ws.request(1);
			return null;
		}
	}

	private static void abort(WebSocket ws) {
		try {
			ws.abort();
		} catch (Throwable ignored) {
		}
	}
}
