package UnitTest.Zeze.Net;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Net.Service;
import Zeze.Net.WebsocketClient;
import Zeze.Netty.HttpServer;
import Zeze.Netty.HttpWebSocketHandle;
import Zeze.Netty.Netty;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND4-34：WebsocketClient.Send 不能恒返回 true。
 * <ul>
 * <li>close() 后 Send 必须返回 false（close 需将 webSocket 置 null，帧不得静默丢弃）；</li>
 * <li>sendBinary 回执异常完成时必须触发 close 并返回 false（对齐服务端 Websocket.Send 的
 * FND3-24 修复形态），Rpc.Send 才能感知发送失败而非等 5 秒超时兜底。</li>
 * </ul>
 * 回执失败路径用注入 stub WebSocket 构造确定性红：真实断连的失败时机受 TCP 栈影响不可稳定复现。
 */
@Fast
public class TestWebsocketClientSend {
	/** sendBinary 恒以 IOException 异常完成的 stub；其余方法不可达（Send 路径只用 sendBinary）。 */
	private static final class FailingStubWebSocket implements WebSocket {
		@Override
		public CompletableFuture<WebSocket> sendText(CharSequence data, boolean last) {
			return CompletableFuture.failedFuture(new IOException("stub"));
		}

		@Override
		public CompletableFuture<WebSocket> sendBinary(ByteBuffer data, boolean last) {
			return CompletableFuture.failedFuture(new IOException("stub send fail"));
		}

		@Override
		public CompletableFuture<WebSocket> sendPing(ByteBuffer message) {
			return CompletableFuture.failedFuture(new IOException("stub"));
		}

		@Override
		public CompletableFuture<WebSocket> sendPong(ByteBuffer message) {
			return CompletableFuture.failedFuture(new IOException("stub"));
		}

		@Override
		public CompletableFuture<WebSocket> sendClose(int statusCode, String reason) {
			return CompletableFuture.failedFuture(new IOException("stub"));
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
			return true;
		}

		@Override
		public boolean isInputClosed() {
			return true;
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

	@Test
	public void testSendAfterCloseReturnsFalse() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new HttpServer();
		try {
			server.addHandler("/ws", TransactionLevel.Serializable, DispatchMode.Direct,
					new HttpWebSocketHandle() {
					});
			var port = ((InetSocketAddress)server.start(netty, 0).sync().channel().localAddress()).getPort();

			var service = new Service("test.ws.send.close");
			var connector = new Connector(true, "ws://127.0.0.1:" + port + "/ws");
			connector.SetService(service);
			try {
				connector.start();
				var so = connector.WaitReady();
				Assertions.assertNotNull(so);
				so.close(new IOException("test close"));
				Assertions.assertFalse(so.Send(new byte[1], 0, 1), "close 后 Send 必须返回 false");
			} finally {
				connector.stop();
				service.Stop();
			}
		} finally {
			server.close();
			netty.close();
		}
	}

	@Test
	public void testSendBinaryFailureClosesAndReturnsFalse() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new HttpServer();
		try {
			server.addHandler("/ws", TransactionLevel.Serializable, DispatchMode.Direct,
					new HttpWebSocketHandle() {
					});
			var port = ((InetSocketAddress)server.start(netty, 0).sync().channel().localAddress()).getPort();

			var service = new Service("test.ws.send.fail");
			var connector = new Connector(true, "ws://127.0.0.1:" + port + "/ws");
			connector.SetService(service);
			try {
				connector.start();
				var so = (WebsocketClient)connector.WaitReady();
				Assertions.assertNotNull(so);
				injectWebSocket(so, new FailingStubWebSocket());
				Assertions.assertFalse(so.Send(new byte[1], 0, 1), "sendBinary 异常完成时 Send 必须返回 false");
				Assertions.assertTrue(so.isClosed(), "发送失败必须触发 close（OnSocketClose 恰好一次）");
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
