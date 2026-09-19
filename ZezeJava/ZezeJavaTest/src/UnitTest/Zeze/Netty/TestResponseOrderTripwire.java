package UnitTest.Zeze.Net;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedQueue;
import Zeze.Netty.HttpExchange;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import harness.Fast;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 出站tripwire回归：绕过writeResponse直写ctx的响应（FND8-46同型）必须在编码器处被拒绝——
 * checkResponseOrder抛异常→exceptionCaught→关连接。修复前（无tripwire）该直写在pipelining下
 * 先于前序响应上线，客户端静默拿到错配响应；现在宁可断连不可错序。
 * 正常路径不误伤由既有测试组保证：tripwire随encoder常驻，PipeliningResponseOrder/FND846/
 * NettyHttpServer/WebSocket等全部用例都在tripwire开启下运行。
 */
@Fast
public class TestResponseOrderTripwire {

	public static final class TestServer extends HttpServer {
		public final ConcurrentLinkedQueue<HttpExchange> created = new ConcurrentLinkedQueue<>();

		@Override
		public @NotNull HttpExchange createHttpExchange(@NotNull ChannelHandlerContext context) {
			var x = new HttpExchange(this, context);
			created.add(x);
			return x;
		}
	}

	private static void await(String what, int timeoutMillis, java.util.function.BooleanSupplier cond)
			throws InterruptedException {
		long deadline = System.currentTimeMillis() + timeoutMillis;
		while (!cond.getAsBoolean()) {
			Assertions.assertTrue(System.currentTimeMillis() < deadline, "timeout waiting: " + what);
			//noinspection BusyWait
			Thread.sleep(10);
		}
	}

	// pipelining：/slow detach住不响应（序化器队头、未首写），/rogue的handler拿context()直写响应
	// ——tripwire必须当场拒绝：连接被关，客户端收不到rogue字节，两个exchange都被janitor清理。
	@Test
	public void testRogueDirectWriteRejected() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new TestServer();
		server.addHandler("/slow", 8192, TransactionLevel.None, DispatchMode.Normal, HttpExchange::detach);
		server.addHandler("/rogue", 8192, TransactionLevel.None, DispatchMode.Normal, x -> {
			// 模拟未来代码绕过"响应写唯一入口"直写ctx（FND8-46的304/416与/metrics同型）
			var res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
					Unpooled.copiedBuffer("rogue-body", StandardCharsets.UTF_8));
			res.headers().set(HttpHeaderNames.CONTENT_LENGTH, 10);
			x.context().writeAndFlush(res);
		});
		try {
			var port = ((InetSocketAddress)server.start(netty, 0).sync().channel().localAddress()).getPort();
			try (var sock = new Socket("127.0.0.1", port)) {
				sock.setSoTimeout(15_000);
				var os = sock.getOutputStream();
				os.write(("GET /slow HTTP/1.1\r\nHost: a\r\n\r\n"
						+ "GET /rogue HTTP/1.1\r\nHost: a\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1));
				os.flush();

				await("both exchanges created", 10_000, () -> server.created.size() >= 2);

				// tripwire拒绝→exceptionCaught→关连接：客户端读到EOF，且任何字节里不得有rogue-body
				InputStream is = sock.getInputStream();
				var buf = new java.io.ByteArrayOutputStream();
				var bytes = new byte[4096];
				int n;
				while ((n = is.read(bytes)) >= 0)
					buf.write(bytes, 0, n);
				Assertions.assertFalse(buf.toString(StandardCharsets.ISO_8859_1).contains("rogue-body"),
						"rogue response must not reach the client, got: " + buf);
			}
			// janitor随后清理全部在途（含detach住的slow）
			var slow = (HttpExchange)server.created.toArray()[0];
			var rogue = (HttpExchange)server.created.toArray()[1];
			await("slow exchange closed by janitor", 10_000, slow::isClosed);
			await("rogue exchange closed by janitor", 10_000, rogue::isClosed);
		} finally {
			server.close();
			netty.close();
		}
	}
}
