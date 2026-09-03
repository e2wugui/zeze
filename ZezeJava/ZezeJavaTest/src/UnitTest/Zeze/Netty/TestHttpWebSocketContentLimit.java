package UnitTest.Zeze.Netty;

import harness.Fast;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
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
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * FND-N2-4回归:WebSocket分片消息(首帧isFinal=false+无限Continuation帧)默认在HttpExchange.content
 * 无上限累积,MaxContentLength只作为单帧上限(maxFramePayloadLength),单条连接即可耗尽堆内存。
 * 修复后分片消息的累积总量同样以MaxContentLength为上限,超限回CloseWebSocketFrame(1009)并关闭连接。
 */
@Fast
public class TestHttpWebSocketContentLimit {
	private static final int MaxFrame = 2048;
	private static Netty netty;
	private static HttpServer server;
	private static int port;

	@BeforeAll
	public static void setUp() throws Exception {
		Task.tryInitThreadPool();
		netty = new Netty(1);
		server = new HttpServer(); // 无zeze, noProcedure, Direct派发inline执行
		// maxFrameLength=2048:既是单帧上限(maxFramePayloadLength)也是分片消息累积上限(修复后)
		server.addHandler("/ws", MaxFrame, TransactionLevel.None, DispatchMode.Direct, new HttpWebSocketHandle() {
			@Override
			public void onText(HttpExchange x, @NotNull String text) {
				x.sendWebSocket(text); // 回显完整消息
			}
		});
		var channel = server.start(netty, 0).sync().channel();
		port = ((InetSocketAddress)channel.localAddress()).getPort();
	}

	@AfterAll
	public static void tearDown() {
		server.close();
		netty.close();
	}

	private static @NotNull String repeat(char c, int n) {
		return String.valueOf(c).repeat(n);
	}

	private static final class Listener implements WebSocket.Listener {
		final StringBuilder text = new StringBuilder();
		final CountDownLatch done = new CountDownLatch(1);
		final AtomicReference<Integer> closeStatus = new AtomicReference<>();

		@Override
		public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
			text.append(data);
			if (last)
				done.countDown();
			ws.request(1);
			return null;
		}

		@Override
		public CompletionStage<?> onClose(WebSocket ws, int statusCode, @NotNull String reason) {
			closeStatus.set(statusCode);
			done.countDown();
			return null;
		}

		@Override
		public void onError(WebSocket ws, @NotNull Throwable ex) {
			done.countDown();
		}
	}

	// 正常路径:分片消息总量在上限内,完整回显
	@Test
	public void testFragmentedWithinLimit() throws Exception {
		var listener = new Listener();
		var ws = HttpClient.newHttpClient().newWebSocketBuilder().buildAsync(
				URI.create("ws://127.0.0.1:" + port + "/ws"), listener).join();
		try {
			var part = repeat('a', 1000);
			ws.sendText(part, false).get(10, TimeUnit.SECONDS);
			ws.sendText(part, false).get(10, TimeUnit.SECONDS);
			ws.sendText("end", true).get(10, TimeUnit.SECONDS); // 总量2003 <= 2048
			Assertions.assertTrue(listener.done.await(10, TimeUnit.SECONDS), "echo not received");
			Assertions.assertEquals(part + part + "end", listener.text.toString());
			ws.sendClose(WebSocket.NORMAL_CLOSURE, "").get(10, TimeUnit.SECONDS);
		} finally {
			abort(ws);
		}
	}

	// 超限路径:分片累积总量超过MaxContentLength,服务器必须回1009关闭连接而不是无限累积
	@Test
	public void testFragmentedOverLimit() throws Exception {
		var listener = new Listener();
		var ws = HttpClient.newHttpClient().newWebSocketBuilder().buildAsync(
				URI.create("ws://127.0.0.1:" + port + "/ws"), listener).join();
		try {
			var part = repeat('b', 1000);
			ws.sendText(part, false).get(10, TimeUnit.SECONDS);
			ws.sendText(part, false).get(10, TimeUnit.SECONDS); // 2000
			// 第3片使累积达3000 > 2048:修复前继续累积(单帧均不超限),修复后服务器回1009并关闭
			// 注:若close帧先于本调用被客户端处理,sendText会同步抛IllegalStateException,属预期,吞掉
			try {
				ws.sendText(part, false).handle((v, e) -> null);
			} catch (IllegalStateException ignored) {
			}
			Assertions.assertTrue(listener.done.await(10, TimeUnit.SECONDS), "connection must be closed by server");
			// 客户端应观察到关闭:close帧(1009)或因服务器关闭导致的错误,二者必居其一
			Assertions.assertTrue(listener.closeStatus.get() != null || ws.isInputClosed(),
					"closeStatus=" + listener.closeStatus.get() + ", inputClosed=" + ws.isInputClosed());
			if (listener.closeStatus.get() != null)
				Assertions.assertEquals(1009, listener.closeStatus.get(), "expect MESSAGE_TOO_BIG");
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
