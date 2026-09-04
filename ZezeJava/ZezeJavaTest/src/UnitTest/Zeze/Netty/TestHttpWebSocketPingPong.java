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
 * FND2-N2-7回归:onPing默认实现write不flush。Zeze管线里WebSocketFrame在HttpServer.channelRead
 * 终止(不fireChannelRead),尾部的WebSocketServerProtocolHandler收不到帧、自动pong不生效,
 * onPing是唯一pong路径;空闲连接上无业务发送触发flush,pong无限期滞留outbound队列,
 * 依赖ping/pong保活的客户端被误断。修复后默认实现writeAndFlush。
 * 同时锁定FND2-N2-1的onOpen派发:Normal模式下onOpen必须经task11Executor派发到池线程执行,
 * 不得内联在连接的EventLoop上(修复前channelRead里内联调用)。
 */
@Fast
public class TestHttpWebSocketPingPong {
	private static Netty netty;
	private static HttpServer server;
	private static int port;
	private static final AtomicReference<Thread> openThread = new AtomicReference<>();
	private static final AtomicReference<io.netty.channel.EventLoop> openEventLoop = new AtomicReference<>();

	@BeforeAll
	public static void setUp() throws Exception {
		Task.tryInitThreadPool();
		netty = new Netty(1);
		server = new HttpServer(); // 无zeze,noProcedure
		// Normal模式:onContent/onPing经task11Executor派发到池线程(正是派发契约路径)
		server.addHandler("/ws", 2048, TransactionLevel.None, DispatchMode.Normal, new HttpWebSocketHandle() {
			@Override
			public void onOpen(HttpExchange x) {
				openThread.set(Thread.currentThread());
				openEventLoop.set(x.channel().eventLoop());
			}
			// onPing用默认实现:修复的正是默认onPing的writeAndFlush
		});
		var channel = server.start(netty, 0).sync().channel();
		port = ((InetSocketAddress)channel.localAddress()).getPort();
	}

	@AfterAll
	public static void tearDown() {
		server.close();
		netty.close();
	}

	private static final class Listener implements WebSocket.Listener {
		final CountDownLatch pong = new CountDownLatch(1);
		final AtomicReference<byte[]> pongPayload = new AtomicReference<>();

		@Override
		public CompletionStage<?> onPong(WebSocket ws, ByteBuffer message) {
			var b = new byte[message.remaining()];
			message.get(b);
			pongPayload.set(b);
			pong.countDown();
			ws.request(1);
			return null;
		}

		@Override
		public void onError(WebSocket ws, Throwable ex) {
			pong.countDown();
		}
	}

	// 空闲连接上客户端ping,必须收到回显payload的pong:修复前pong滞留outbound队列永不发出,本用例等满超时失败
	@Test
	public void testDefaultOnPingFlushed() throws Exception {
		var listener = new Listener();
		var ws = HttpClient.newHttpClient().newWebSocketBuilder().buildAsync(
				URI.create("ws://127.0.0.1:" + port + "/ws"), listener).join();
		try {
			// 先等握手完成再ping,保证连接空闲(无业务发送顺带flush),只依赖onPing自身的flush
			ws.sendPing(ByteBuffer.wrap("hb".getBytes(StandardCharsets.UTF_8))).get(10, TimeUnit.SECONDS);
			Assertions.assertTrue(listener.pong.await(10, TimeUnit.SECONDS),
					"pong not received: default onPing must writeAndFlush");
			Assertions.assertArrayEquals("hb".getBytes(StandardCharsets.UTF_8), listener.pongPayload.get());
			ws.sendClose(WebSocket.NORMAL_CLOSURE, "").get(10, TimeUnit.SECONDS);
		} finally {
			abort(ws);
		}
	}

	// FND2-N2-1:Normal模式下onOpen不得内联在连接的EventLoop线程上执行。
	// pong已收到⟹同key(channel.id)队列FIFO⟹先提交的onOpen已执行完毕,断言无竞态。
	// 修复前onOpen在channelRead里内联执行,openThread就是EventLoop线程,本用例失败。
	@Test
	public void testOnOpenDispatchedOffEventLoop() throws Exception {
		var listener = new Listener();
		var ws = HttpClient.newHttpClient().newWebSocketBuilder().buildAsync(
				URI.create("ws://127.0.0.1:" + port + "/ws"), listener).join();
		try {
			ws.sendPing(ByteBuffer.wrap("hb".getBytes(StandardCharsets.UTF_8))).get(10, TimeUnit.SECONDS);
			Assertions.assertTrue(listener.pong.await(10, TimeUnit.SECONDS), "pong not received");
			var t = openThread.get();
			Assertions.assertNotNull(t, "onOpen not called");
			Assertions.assertFalse(openEventLoop.get().inEventLoop(t),
					"onOpen must not run inline on the channel EventLoop, thread=" + t);
			ws.sendClose(WebSocket.NORMAL_CLOSURE, "").get(10, TimeUnit.SECONDS);
		} finally {
			abort(ws);
		}
	}

	private static void abort(WebSocket ws) {
		try {
			ws.abort();
		} catch (Throwable ignored) {
		}
	}
}
