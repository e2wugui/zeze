package Zeze.Component;

import static java.util.Objects.requireNonNull;

import harness.Fast;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Application;
import Zeze.Netty.FreeMarker;
import Zeze.Netty.HttpExchange;
import Zeze.Netty.HttpHandler;
import Zeze.Netty.HttpSession;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Netty.Thymeleaf;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelFuture;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.ReferenceCountUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.thymeleaf.context.Context;

/** HTTP事务redo/终局失败与detach所有权的组合回归，使用真实Memory事务和Netty出站队列。 */
@Fast
public class TestHttpTransactionBoundary {
	private static final class Env implements AutoCloseable {
		final Application app;
		final HttpSession session;
		final HttpServer server;

		Env() throws Exception {
			Task.tryInitThreadPool();
			var conf = TakeoverTestEnv.newConf("dryrun", 600_000, 600_000);
			app = new Application("HttpTransactionBoundary." + conf.getServerId(), conf);
			app.initialize(new TakeoverTestEnv.TestAppBase(app));
			session = new HttpSession(app);
			session.RegisterZezeTables(app);
			app.start();
			server = new HttpServer(app) {
				@Override
				public HttpSession getHttpSession() {
					return session;
				}
			};
		}

		@Override
		public void close() throws Exception {
			server.close();
			app.stop();
		}
	}

	private static final class Exchange extends HttpExchange {
		Exchange(HttpServer server, EmbeddedChannel channel) {
			super(server, channel.pipeline().firstContext());
			request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/transaction");
		}

		void fire(HttpHandler h) {
			handler = h;
			fireEndStreamHandle();
		}

		boolean pending() {
			return endStreamTaskPending;
		}

		ArrayList<String> cookieHeaders() {
			var values = new ArrayList<String>();
			if (resHeaders != null)
				for (int i = 0; i < resHeaders.size(); i += 2)
					if (HttpHeaderNames.SET_COOKIE.contentEqualsIgnoreCase((CharSequence)resHeaders.get(i)))
						values.add(String.valueOf(resHeaders.get(i + 1)));
			return values;
		}
	}

	private static final class ChannelScope implements AutoCloseable {
		final EmbeddedChannel channel = new EmbeddedChannel(new ChannelInboundHandlerAdapter());

		@Override
		public void close() {
			channel.finishAndReleaseAll();
		}
	}

	@Test
	public void testRedoSendsOnlyCommittedResponseAndSessionCookie() throws Exception {
		try (var env = new Env(); var scope = new ChannelScope()) {
			var channel = scope.channel;
			var x = new Exchange(env.server, channel);
			x.addHeader(HttpHeaderNames.SET_COOKIE, "OTHER=keep");
			var attempts = new AtomicInteger();
			var bodies = new ArrayList<ByteBuf>();
			var sessions = new ArrayList<HttpSession.CookieSession>();
			long rc = env.app.newProcedure(() -> {
				int attempt = attempts.incrementAndGet();
				var cs = x.getCookieSession();
				Assertions.assertNotNull(cs);
				sessions.add(cs);
				cs.setProperty("value", "attempt-" + attempt);
				x.setCookie("ATTEMPT", "attempt-" + attempt, null, null, -1);
				var body = Unpooled.copiedBuffer("attempt-" + attempt, StandardCharsets.UTF_8);
				bodies.add(body);
				x.send(HttpResponseStatus.OK, "text/plain", body);
				Assertions.assertNull(channel.readOutbound(), "提交前不可有网络响应");
				if (attempt == 1)
					requireNonNull(Transaction.getCurrent()).throwRedo(
							requireNonNull(env.app.getTable("Zeze_Builtin_HttpSession_tSession")).getId(), "forced HTTP redo");
				return Procedure.Success;
			}, "HttpTransactionBoundary.redo").call();
			Assertions.assertEquals(Procedure.Success, rc);
			Assertions.assertEquals(2, attempts.get());
			Assertions.assertNotSame(sessions.get(0), sessions.get(1));
			Assertions.assertEquals("attempt-2", requireNonNull(x.getCookieSession()).getProperty("value"));
			Assertions.assertEquals(3, x.cookieHeaders().size(), "保留无关cookie及提交轮的session/普通cookie");
			Assertions.assertTrue(x.cookieHeaders().contains("ATTEMPT=attempt-2"));
			Assertions.assertFalse(x.cookieHeaders().contains("ATTEMPT=attempt-1"));
			bodies.forEach(body -> Assertions.assertEquals(0, body.refCnt()));
			channel.runPendingTasks();
			FullHttpResponse response = channel.readOutbound();
			Assertions.assertNotNull(response);
			try {
				Assertions.assertEquals("attempt-2", response.content().toString(StandardCharsets.UTF_8));
				Assertions.assertEquals(3, response.headers().getAll(HttpHeaderNames.SET_COOKIE).size());
			} finally {
				response.release();
			}
			Assertions.assertNull(channel.readOutbound(), "redo首轮响应不可出现");
			x.closeConnectionNow();
		}
	}

	@Test
	public void testFailedHandlersSend500AndReleaseProvisionalBodies() throws Exception {
		try (var env = new Env()) {
			for (var mode : new DispatchMode[]{DispatchMode.Direct, DispatchMode.Normal}) {
				try (var scope = new ChannelScope()) {
					var channel = scope.channel;
					var x = new Exchange(env.server, channel);
					var body = Unpooled.copiedBuffer("provisional 200", StandardCharsets.UTF_8);
					var entered = new CountDownLatch(1);
					x.fire(new HttpHandler(1024, TransactionLevel.Serializable, mode, exchange -> {
						entered.countDown();
						exchange.send(HttpResponseStatus.OK, "text/plain", body);
						throw new IllegalStateException("forced terminal rollback");
					}));
					Assertions.assertTrue(entered.await(5, TimeUnit.SECONDS));
					long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
					while (x.pending() && System.nanoTime() < deadline)
						Thread.sleep(1);
					Assertions.assertFalse(x.pending(), "handler任务须已完成");
					channel.runPendingTasks();
					FullHttpResponse response = channel.readOutbound();
					Assertions.assertNotNull(response, "失败须返回状态，不可只flush空buffer挂起客户端");
					try {
						Assertions.assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, response.status());
					} finally {
						response.release();
					}
					Assertions.assertEquals(0, body.refCnt());
					Object next;
					while ((next = channel.readOutbound()) != null) {
						Assertions.assertFalse(next instanceof FullHttpResponse, "不得追加旧200响应");
						ReferenceCountUtil.release(next);
					}
				}
			}
		}
	}

	@Test
	public void testRollbackClearsSessionCacheAndStreamingGuardPrecedesMutation() throws Exception {
		try (var env = new Env(); var scope = new ChannelScope()) {
			var channel = scope.channel;
			var x = new Exchange(env.server, channel);
			var old = new HttpSession.CookieSession[1];
			x.addHeader(HttpHeaderNames.SET_COOKIE, "OTHER=keep");
			long rc = env.app.newProcedure(() -> {
				old[0] = x.getCookieSession();
				requireNonNull(old[0]).setProperty("value", "rolled back");
				throw new IllegalStateException("forced terminal rollback");
			}, "HttpTransactionBoundary.rollback").call();
			Assertions.assertNotEquals(Procedure.Success, rc);
			Assertions.assertEquals(java.util.List.of("OTHER=keep"), x.cookieHeaders());
			var fresh = x.getCookieSession();
			Assertions.assertNotSame(old[0], fresh);
			requireNonNull(fresh).setProperty("value", "fresh");
			Assertions.assertEquals("fresh", fresh.getProperty("value"));
			var headers = new DefaultHttpHeaders();
			Assertions.assertEquals(Procedure.Success, env.app.newProcedure(() -> {
				var error = Assertions.assertThrows(IllegalStateException.class,
						() -> x.beginStream(HttpResponseStatus.OK, headers));
				Assertions.assertTrue(error.getMessage().contains("TransactionLevel.None"));
				Assertions.assertFalse(headers.contains(HttpHeaderNames.TRANSFER_ENCODING));
				Assertions.assertNull(channel.readOutbound());
				return Procedure.Success;
			}, "HttpTransactionBoundary.streaming").call());
			x.closeConnectionNow();
		}
	}

	@Test
	public void testNestedRollbackAndRedoPreserveOnlyCommittedHeaders() throws Exception {
		try (var env = new Env(); var scope = new ChannelScope()) {
			var channel = scope.channel;
			var x = new Exchange(env.server, channel);
			x.setCookie("BASE", "keep", null, null, -1);
			var attempts = new AtomicInteger();
			Assertions.assertEquals(Procedure.Success, env.app.newProcedure(() -> {
				int attempt = attempts.incrementAndGet();
				x.setCookie("OUTER", "round-" + attempt, null, null, -1);
				Assertions.assertEquals(Procedure.Unknown, env.app.newProcedure(() -> {
					x.setCookie("INNER", "discard", null, null, -1);
					return Procedure.Unknown;
				}, "HttpTransactionBoundary.nestedRollback").call());
				if (attempt == 1)
					requireNonNull(Transaction.getCurrent()).throwRedo(
							requireNonNull(env.app.getTable("Zeze_Builtin_HttpSession_tSession")).getId(), "forced header redo");
				x.sendPlainText(HttpResponseStatus.OK, "committed");
				return Procedure.Success;
			}, "HttpTransactionBoundary.headers").call());
			Assertions.assertEquals(java.util.List.of("BASE=keep", "OUTER=round-2"), x.cookieHeaders());
			channel.runPendingTasks();
			FullHttpResponse response = channel.readOutbound();
			Assertions.assertNotNull(response);
			try {
				Assertions.assertEquals(x.cookieHeaders(), response.headers().getAll(HttpHeaderNames.SET_COOKIE));
			} finally {
				response.release();
			}
			x.closeConnectionNow();
		}
	}

	@Test
	public void testNestedResponseAndCloseSelectTheCommittedSavepoint() throws Exception {
		try (var env = new Env()) {
			for (boolean commitNested : new boolean[]{false, true}) {
				try (var scope = new ChannelScope()) {
					var channel = scope.channel;
					var x = new Exchange(env.server, channel);
					var futures = new ChannelFuture[2];
					Assertions.assertEquals(Procedure.Success, env.app.newProcedure(() -> {
						futures[0] = x.sendPlainText(HttpResponseStatus.OK, "outer");
						x.close(futures[0]);
						var nestedResult = env.app.newProcedure(() -> {
							futures[1] = x.sendPlainText(HttpResponseStatus.OK, "inner");
							return commitNested ? Procedure.Success : Procedure.Unknown;
						}, "HttpTransactionBoundary.nestedResponse").call();
						Assertions.assertEquals(commitNested ? Procedure.Success : Procedure.Unknown, nestedResult);
						Assertions.assertNull(channel.readOutbound());
						return Procedure.Success;
					}, "HttpTransactionBoundary.responseAndClose").call());
					channel.runPendingTasks();
					FullHttpResponse response = channel.readOutbound();
					Assertions.assertNotNull(response, "nested分支不能抹掉最终有效响应");
					try {
						Assertions.assertEquals(commitNested ? "inner" : "outer",
								response.content().toString(StandardCharsets.UTF_8));
					} finally {
						response.release();
					}
					Assertions.assertTrue(futures[commitNested ? 1 : 0].isSuccess());
					Assertions.assertTrue(futures[commitNested ? 0 : 1].isDone());
					Assertions.assertFalse(futures[commitNested ? 0 : 1].isSuccess());
					Assertions.assertFalse(x.hasRequest(), "close意图须在最终响应完成后兑现");
					Assertions.assertNull(channel.readOutbound(), "只能发出一个完整响应");
				}
			}
		}
	}

	@Test
	public void testWebSocketRedoAndRollbackDoNotPublishProvisionalFrames() throws Exception {
		try (var env = new Env(); var scope = new ChannelScope()) {
			var channel = scope.channel;
			var x = new Exchange(env.server, channel);
			var attempts = new AtomicInteger();
			var transferred = new ArrayList<ByteBuf>();
			Assertions.assertEquals(Procedure.Success, env.app.newProcedure(() -> {
				int attempt = attempts.incrementAndGet();
				x.sendWebSocket("text-" + attempt);
				var data = Unpooled.copiedBuffer("binary-" + attempt, StandardCharsets.UTF_8);
				transferred.add(data);
				x.sendWebSocket(new BinaryWebSocketFrame(false, 0, data));
				Assertions.assertEquals(0, data.refCnt());
				Assertions.assertNull(channel.readOutbound());
				if (attempt == 1)
					requireNonNull(Transaction.getCurrent()).throwRedo(
							requireNonNull(env.app.getTable("Zeze_Builtin_HttpSession_tSession")).getId(), "forced websocket redo");
				return Procedure.Success;
			}, "HttpTransactionBoundary.websocket").call());
			channel.runPendingTasks();
			TextWebSocketFrame text = channel.readOutbound();
			BinaryWebSocketFrame binary = channel.readOutbound();
			Assertions.assertNotNull(text);
			Assertions.assertNotNull(binary);
			try {
				Assertions.assertEquals("text-2", text.text());
				Assertions.assertEquals("binary-2", binary.content().toString(StandardCharsets.UTF_8));
				Assertions.assertFalse(binary.isFinalFragment());
			} finally {
				text.release();
				binary.release();
			}
			Assertions.assertNull(channel.readOutbound());
			Assertions.assertEquals(Procedure.Unknown, env.app.newProcedure(() -> {
				x.sendWebSocket(new TextWebSocketFrame("discard"));
				return Procedure.Unknown;
			}, "HttpTransactionBoundary.websocketRollback").call());
			channel.runPendingTasks();
			Assertions.assertNull(channel.readOutbound());
			transferred.forEach(body -> Assertions.assertEquals(0, body.refCnt()));
			x.closeConnectionNow();
		}
	}

	@Test
	public void testDetachedHandlerKeepsRequestBodyUntilOwnerCloses() throws Exception {
		try (var server = new HttpServer(); var scope = new ChannelScope()) {
			var channel = scope.channel;
			var x = new Exchange(server, channel);
			var body = Unpooled.copiedBuffer("owned body", StandardCharsets.UTF_8);
			x.addContent(body);
			x.fire(new HttpHandler(1024, TransactionLevel.None, DispatchMode.Direct, HttpExchange::detach));
			Assertions.assertEquals(1, body.refCnt());
			Assertions.assertEquals("owned body", body.toString(StandardCharsets.UTF_8));
			x.closeConnectionNow();
			channel.runPendingTasks();
			Assertions.assertEquals(0, body.refCnt());
		}
	}

	@Test
	public void testTransactionalTemplateRenderingWithHeadersAndClose(@TempDir Path tempDir) throws Exception {
		Files.writeString(tempDir.resolve("fm.ftlh"), "<p>fm:${msg}</p>");
		Files.writeString(tempDir.resolve("thyme.html"), "<p th:text=\"${msg}\"></p>");
		var freeMarker = new FreeMarker(tempDir.toFile());
		var thymeleaf = new Thymeleaf(tempDir.toString());

		try (var env = new Env()) {
			var netty = new Netty(1);
			var httpServer = new HttpServer(env.app) {
				@Override
				public HttpSession getHttpSession() {
					return env.session;
				}

				@Override
				public FreeMarker getFreeMarker() {
					return freeMarker;
				}

				@Override
				public Thymeleaf getThymeleaf() {
					return thymeleaf;
				}
			};
			var fmAttempts = new AtomicInteger();
			var thymeCalls = new AtomicInteger();
			httpServer.addHandler("/fm", 8192, TransactionLevel.Serializable, DispatchMode.Normal, x -> {
				int attempt = fmAttempts.incrementAndGet();
				x.addCookie("fm_attempt", Integer.toString(attempt));
				var cs = x.getCookieSession();
				Assertions.assertNotNull(cs);
				cs.setProperty("attempt", Integer.toString(attempt));
				x.sendFreeMarker(Map.of("msg", "try-" + attempt));
				x.close(null);
				if (attempt == 1)
					requireNonNull(Transaction.getCurrent()).throwRedo(
							requireNonNull(env.app.getTable("Zeze_Builtin_HttpSession_tSession")).getId(), "forced fm redo");
			});
			httpServer.addHandler("/thyme", 8192, TransactionLevel.Serializable, DispatchMode.Normal, x -> {
				int n = thymeCalls.incrementAndGet();
				x.addCookie("th_req", "r" + n);
				var ctx = new Context();
				ctx.setVariable("msg", "thyme-" + n);
				x.sendThymeleaf(ctx);
				x.close(null);
			});

			try {
				int port = ((InetSocketAddress)httpServer.start(netty, 0).sync().channel().localAddress()).getPort();
				var client = HttpClient.newHttpClient();

				// 首次 FreeMarker（HttpExchangeStreamWriter + 事务 redo）
				var fm1 = client.send(HttpRequest.newBuilder()
						.uri(URI.create("http://127.0.0.1:" + port + "/fm")).GET().build(),
						HttpResponse.BodyHandlers.ofString());
				Assertions.assertEquals(200, fm1.statusCode());
				Assertions.assertEquals("<p>fm:try-2</p>", fm1.body());
				Assertions.assertEquals("text/html; charset=utf-8", fm1.headers().firstValue("content-type").orElse(""));
				var fm1Cookies = fm1.headers().allValues("set-cookie");
				Assertions.assertTrue(fm1Cookies.contains("fm_attempt=2"), "应含提交轮cookie: " + fm1Cookies);
				Assertions.assertFalse(fm1Cookies.contains("fm_attempt=1"), "不得含redo轮cookie: " + fm1Cookies);
				Assertions.assertTrue(fm1Cookies.stream().anyMatch(c -> c.startsWith("ZEZESESSIONID=")),
						"首次流式模板渲染应带会话Set-Cookie: " + fm1Cookies);

				// 第二次 FreeMarker（晋升为 HttpExchangeContentLengthWriter + close(null)）
				var fm2 = client.send(HttpRequest.newBuilder()
						.uri(URI.create("http://127.0.0.1:" + port + "/fm")).GET().build(),
						HttpResponse.BodyHandlers.ofString());
				Assertions.assertEquals(200, fm2.statusCode());
				Assertions.assertEquals("<p>fm:try-3</p>", fm2.body());
				Assertions.assertEquals("text/html; charset=utf-8", fm2.headers().firstValue("content-type").orElse(""));
				Assertions.assertTrue(fm2.headers().allValues("set-cookie").contains("fm_attempt=3"));

				// 首次与第二次 Thymeleaf（覆盖 StreamWriter 与 ContentLengthWriter 两条路径）
				for (int i = 1; i <= 2; i++) {
					var th = client.send(HttpRequest.newBuilder()
							.uri(URI.create("http://127.0.0.1:" + port + "/thyme")).GET().build(),
							HttpResponse.BodyHandlers.ofString());
					Assertions.assertEquals(200, th.statusCode());
					Assertions.assertEquals("<p>thyme-" + i + "</p>", th.body());
					Assertions.assertEquals("text/html; charset=utf-8", th.headers().firstValue("content-type").orElse(""));
					Assertions.assertTrue(th.headers().allValues("set-cookie").contains("th_req=r" + i));
				}
			} finally {
				httpServer.close();
				netty.close();
			}
		}
	}
}
