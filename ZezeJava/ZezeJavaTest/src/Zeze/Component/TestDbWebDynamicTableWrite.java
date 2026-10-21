package Zeze.Component;

import static java.util.Objects.requireNonNull;

import java.nio.charset.StandardCharsets;
import Zeze.Application;
import Zeze.Netty.HttpExchange;
import Zeze.Netty.HttpServer;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TableDynamic;
import Zeze.Transaction.TableX;
import Zeze.Util.Task;
import demo.Module1.BValue;
import demo.Module1.tCoverHistory;
import harness.Fast;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** 真实 PutRecord servlet、Memory 事务和 Netty 响应；每个 Application 独立发号及库 URL。 */
@Fast
public class TestDbWebDynamicTableWrite {
	private static final String TOKEN = "dynamic-table-write-token";

	private static final class Env implements AutoCloseable {
		final Application app;
		final DbWeb web = new DbWeb(TOKEN);
		final tCoverHistory generated = new tCoverHistory();
		final HttpServer server;

		Env() throws Exception {
			Task.tryInitThreadPool();
			var conf = TakeoverTestEnv.newConf("off", 600_000, 600_000);
			app = new Application("DbWebDynamicTableWrite." + conf.getServerId(), conf);
			app.addTable("", generated);
			app.start();
			web.Initialize(new TakeoverTestEnv.TestAppBase(app));
			server = new HttpServer(app);
		}

		@Override
		public void close() throws Exception {
			server.close();
			app.stop();
		}
	}

	private static final class Exchange extends HttpExchange {
		Exchange(HttpServer server, EmbeddedChannel channel, String tableName) {
			super(server, channel.pipeline().firstContext());
			var req = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST,
					"/Zeze/Builtin/DbWeb/PutRecord?t=" + tableName + "&k=17");
			req.headers().set("X-Zeze-Token", TOKEN);
			request = req;
			addContent(Unpooled.copiedBuffer("{int_1:42,string3:\"from-web\"}", StandardCharsets.UTF_8));
		}
	}

	private static void assertPutRecord(Env env, TableX<Long, BValue> table) {
		var channel = new EmbeddedChannel(new ChannelInboundHandlerAdapter());
		var exchange = new Exchange(env.server, channel, table.getName());
		try {
			// 与生成 servlet 的 TransactionLevel.Serializable 分发使用相同的真实事务边界。
			Assertions.assertEquals(Procedure.Success, env.app.newProcedure(() -> {
				env.web.OnServletPutRecord(exchange);
				return Procedure.Success;
			}, "DbWebDynamicTableWrite.put").call());
			channel.runPendingTasks();
			FullHttpResponse response = channel.readOutbound();
			Assertions.assertNotNull(response);
			try {
				Assertions.assertEquals(HttpResponseStatus.OK, response.status(),
						response.content().toString(StandardCharsets.UTF_8));
				Assertions.assertEquals("PutRecord done!", response.content().toString(StandardCharsets.UTF_8));
			} finally {
				response.release();
			}
			// HTTP 成功必须对应已提交记录，不能仅检查解析类型或响应状态。
			Assertions.assertEquals(Procedure.Success, env.app.newProcedure(() -> {
				var value = requireNonNull(table.get(17L));
				Assertions.assertEquals(42, value.getInt_1());
				Assertions.assertEquals("from-web", value.getString3());
				return Procedure.Success;
			}, "DbWebDynamicTableWrite.read").call());
		} finally {
			exchange.closeConnectionNow();
			channel.finishAndReleaseAll();
		}
	}

	@Test
	public void testWritesDynamicTableThroughServlet() throws Exception {
		try (var env = new Env()) {
			var dynamic = new TableDynamic<>(env.app, "DbWebDynamicTableWrite_dynamic", env.generated);
			assertPutRecord(env, dynamic);
		}
	}

	@Test
	public void testWritesGeneratedTableThroughServlet() throws Exception {
		try (var env = new Env()) {
			assertPutRecord(env, env.generated);
		}
	}
}
