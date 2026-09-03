package UnitTest.Zeze.Netty;

import harness.Fast;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import Zeze.Netty.HttpExchange;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import io.netty.channel.ChannelId;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * 携带件CARRY-HTTP-CHUNK回归:Netty 4.1.135对非法chunk size不抛异常,
 * 而是产出带失败DecoderResult的空LastHttpContent(HttpObjectDecoder.READ_CHUNK_SIZE的catch→invalidChunk)。
 * HttpServer管线原无人检查DecoderResult,声称chunked的POST中途出现非法chunk头时,
 * 已收到的截断body被当成完整请求交给handler处理(如"5\r\nhello\r\nZZZZ\r\n"只收到"hello"就触发onEndStream)。
 * 修复后管线拦截DecoderResult失败的消息:回400并关闭连接,半处理的exchange同时清理。
 */
@Fast
public class TestHttpServerMalformedChunk {
	private static Netty netty;
	private static TestServer server;
	private static int port;

	// 暴露基类protected的exchanges给断言用
	public static final class TestServer extends HttpServer {
		public @NotNull ConcurrentHashMap<ChannelId, HttpExchange> exchangesView() {
			return exchanges;
		}
	}

	@BeforeAll
	public static void setUp() throws Exception {
		Task.tryInitThreadPool();
		netty = new Netty(1);
		server = new TestServer();
		// 非流模式回显body:修复前畸形chunk会让handler收到截断的body并回200
		server.addHandler("/echo", 8192, TransactionLevel.Serializable, DispatchMode.Direct,
				x -> x.sendPlainText(HttpResponseStatus.OK, x.contentString()));
		var channel = server.start(netty, 0).sync().channel();
		port = ((InetSocketAddress)channel.localAddress()).getPort();
	}

	@AfterAll
	public static void tearDown() {
		server.close();
		netty.close();
	}

	private static @NotNull String sendRawUntilClose(@NotNull String raw) throws IOException {
		try (var sock = new Socket("127.0.0.1", port)) {
			sock.setSoTimeout(15000);
			var os = sock.getOutputStream();
			os.write(raw.getBytes(StandardCharsets.ISO_8859_1));
			os.flush();
			var out = new ByteArrayOutputStream();
			var buf = new byte[4096];
			var in = sock.getInputStream();
			int n;
			while ((n = in.read(buf)) >= 0)
				out.write(buf, 0, n);
			return out.toString(StandardCharsets.ISO_8859_1);
		}
	}

	private static void awaitExchangesEmpty() throws InterruptedException {
		var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (!server.exchangesView().isEmpty()) {
			Assertions.assertTrue(System.nanoTime() < deadline,
					"exchanges not cleaned up after connection closed: " + server.exchangesView().keySet());
			//noinspection BusyWait
			Thread.sleep(20);
		}
	}

	// 核心:声称chunked的POST,首块合法,第二块chunk size非法("ZZZZ"非十六进制)。
	// 修复前:解码器产出失败DecoderResult的LastHttpContent,handler把已收的"hello"当完整body回200;
	// 修复后:回400并关闭连接。
	@Test
	public void testMalformedChunkSize() throws Exception {
		var raw = "POST /echo HTTP/1.1\r\nHost: a\r\nTransfer-Encoding: chunked\r\n\r\n"
				+ "5\r\nhello\r\n"
				+ "ZZZZ\r\n"; // 非法chunk size,非法行后无更多数据,避免RST抹掉400响应
		var res = sendRawUntilClose(raw);
		Assertions.assertTrue(res.startsWith("HTTP/1.1 400"), res);
		awaitExchangesEmpty();
	}

	// 回归:合法chunked请求不受影响,完整body正常处理
	@Test
	public void testNormalChunked() throws Exception {
		// contentLength()=-1的publisher让java.net.http使用chunked编码(同TestNettyHttpServer.HttpRequestStringBody手法)
		var res = HttpClient.newHttpClient().send(HttpRequest.newBuilder()
				.uri(URI.create("http://127.0.0.1:" + port + "/echo"))
				.POST(new ChunkedStringBody("hello")).build(), HttpResponse.BodyHandlers.ofString());
		Assertions.assertEquals(200, res.statusCode());
		Assertions.assertEquals("hello", res.body());
		awaitExchangesEmpty();
	}

	// contentLength()返回-1触发java.net.http的chunked编码
	private static final class ChunkedStringBody implements HttpRequest.BodyPublisher {
		private final byte[] body;

		ChunkedStringBody(@NotNull String body) {
			this.body = body.getBytes(StandardCharsets.UTF_8);
		}

		@Override
		public long contentLength() {
			return -1;
		}

		@Override
		public void subscribe(java.util.concurrent.Flow.Subscriber<? super java.nio.ByteBuffer> subscriber) {
			subscriber.onSubscribe(new java.util.concurrent.Flow.Subscription() {
				private boolean finished;

				@Override
				public void request(long n) {
					if (!finished) {
						finished = true;
						subscriber.onNext(java.nio.ByteBuffer.wrap(body));
					} else
						subscriber.onComplete();
				}

				@Override
				public void cancel() {
				}
			});
		}
	}
}
