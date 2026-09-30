package Zeze.Component;

import harness.Fast;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Application;
import Zeze.Netty.HttpExchange;
import Zeze.Netty.HttpHandler;
import Zeze.Netty.HttpSession;
import Zeze.Netty.HttpServer;
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
					Transaction.getCurrent().throwRedo(
							env.app.getTable("Zeze_Builtin_HttpSession_tSession").getId(), "forced header redo");
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






}
