package UnitTest.Zeze.Net;

import harness.Fast;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Net.Rpc;
import Zeze.Net.RpcSocketDisposedException;
import Zeze.Net.Service;
import Zeze.Net.Websocket;
import Zeze.Net.WebsocketHandle;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Transaction.Procedure;
import Zeze.Util.Task;
import demo.Module1.BValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND8-44回归：Websocket/WebsocketClient关闭路径从不触发Service.OnSocketDisposed——
 * 在飞Rpc只能等超时（语义误判为RpcTimeoutException），覆写OnSocketDisposed做每连接清理的
 * Service子类对websocket连接永远收不到回调。
 */
@Fast
public class TestFnd844WebsocketDisposeRpc {
	private static final long TIMEOUT_MS = 10_000;

	public static final class DisposeCountService extends Service {
		public final AtomicInteger disposedCount = new AtomicInteger();
		public volatile @Nullable AsyncSocket lastHandshaked;

		public DisposeCountService(String name) {
			super(name);
		}

		@Override
		public void OnHandshakeDone(@NotNull AsyncSocket so) throws Exception {
			lastHandshaked = so;
			super.OnHandshakeDone(so);
		}

		@Override
		public void OnSocketDisposed(@NotNull AsyncSocket so) throws Exception {
			disposedCount.incrementAndGet();
			super.OnSocketDisposed(so);
		}
	}

	public static class NoReplyRpc extends Rpc<BValue, BValue> {
		public NoReplyRpc() {
			Argument = new BValue();
			Result = new BValue();
		}

		@Override
		public int getModuleId() {
			return 91;
		}

		@Override
		public int getProtocolId() {
			return 44;
		}
	}

	// 客户端：在飞Rpc随WebsocketClient.close立即以RpcSocketDisposedException失败（而非等超时）
	@Test
	public void testClientCloseDisposesInflightRpc() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new HttpServer();
		try {
			var serverService = new DisposeCountService("test.fnd844.server");
			var rpc = new NoReplyRpc();
			serverService.AddFactoryHandle(rpc.getTypeId(), new Service.ProtocolFactoryHandle<>(
					NoReplyRpc::new, r -> {
						// 永不应答：请求到达后保持上下文在飞
						return Procedure.Success;
					}));
			serverService.start();
			var handle = new WebsocketHandle("/ws", server);
			handle.setService(serverService);
			handle.start();
			var port = ((InetSocketAddress)server.start(netty, 0).sync().channel().localAddress()).getPort();

			var clientService = new DisposeCountService("test.fnd844.client");
			var connector = new Connector(true, "ws://127.0.0.1:" + port + "/ws");
			connector.SetService(clientService);
			try {
				connector.start();
				var so = connector.WaitReady();
				Assertions.assertNotNull(so);

				// 60s超时确保失败只能来自dispose而非定时器到点
				var future = new NoReplyRpc().SendForWait(so, 60_000);
				so.close(new IOException("test dispose"));
				var deadline = System.currentTimeMillis() + TIMEOUT_MS;
				while (!future.isDone() && System.currentTimeMillis() < deadline) {
					//noinspection BusyWait
					Thread.sleep(10);
				}
				Assertions.assertTrue(future.isDone(), "close后Rpc必须立即失败，不得等超时");
				Assertions.assertTrue(future.isCompletedExceptionally(), "close后Rpc必须异常完成");
				Throwable cause = null;
				try {
					future.join();
				} catch (Throwable e) {
					cause = e.getCause() != null ? e.getCause() : e;
				}
				Assertions.assertTrue(cause instanceof RpcSocketDisposedException,
						"期望RpcSocketDisposedException，实际=" + cause);
				Assertions.assertTrue(clientService.disposedCount.get() >= 1,
						"WebsocketClient.close必须回调OnSocketDisposed");
			} finally {
				connector.stop();
				clientService.Stop();
				serverService.Stop();
			}
		} finally {
			server.close();
			netty.close();
		}
	}

	// 服务端：Websocket.close（服务停机/主动关闭同一路径）必须回调OnSocketDisposed
	@Test
	public void testServerCloseCallsOnSocketDisposed() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new HttpServer();
		try {
			var wsService = new DisposeCountService("test.fnd844.wsserver");
			var handle = new WebsocketHandle("/ws", server);
			handle.setService(wsService);
			handle.start();
			var port = ((InetSocketAddress)server.start(netty, 0).sync().channel().localAddress()).getPort();

			var ws = HttpClient.newHttpClient().newWebSocketBuilder().buildAsync(
					URI.create("ws://127.0.0.1:" + port + "/ws"), new WebSocket.Listener() {
						@Override
						public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
							webSocket.request(1);
							return null;
						}
					}).get(10, TimeUnit.SECONDS);
			try {
				var deadline = System.currentTimeMillis() + TIMEOUT_MS;
				while (wsService.lastHandshaked == null && System.currentTimeMillis() < deadline) {
					//noinspection BusyWait
					Thread.sleep(10);
				}
				var serverSo = wsService.lastHandshaked;
				Assertions.assertNotNull(serverSo, "服务端Websocket未完成握手注册");
				Assertions.assertTrue(serverSo instanceof Websocket, "必须是服务端Websocket");
				serverSo.close(null); // 服务端主动关闭（Service.stop清扫同一路径）
				deadline = System.currentTimeMillis() + TIMEOUT_MS;
				while (wsService.disposedCount.get() == 0 && System.currentTimeMillis() < deadline) {
					//noinspection BusyWait
					Thread.sleep(10);
				}
				Assertions.assertTrue(wsService.disposedCount.get() >= 1,
						"Websocket.close必须回调OnSocketDisposed（修复前从不触发）");
			} finally {
				try {
					ws.abort();
				} catch (Throwable ignored) {
				}
				wsService.Stop();
			}
		} finally {
			server.close();
			netty.close();
		}
	}
}
