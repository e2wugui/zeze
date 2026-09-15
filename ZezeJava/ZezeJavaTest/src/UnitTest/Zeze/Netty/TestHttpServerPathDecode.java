package UnitTest.Zeze.Netty;

import harness.Fast;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * pathDecode（FND6-18）回归：path段'+'是普通字面量（不做form解码），仅百分号编码按UTF-8
 * 解码；handler匹配的是解码后路径（HttpExchange.channelRead先path()再server.getHandler）。
 * 畸形转义（GET /%zz → 500+关连接）已由TestHttpServerCleanup#testMalformedUriCleanup覆盖，
 * 此处不重复。
 */
@Fast
public class TestHttpServerPathDecode {
	private static Netty netty;
	private static HttpServer server;
	private static int port;

	@BeforeAll
	public static void setUp() throws Exception {
		Task.tryInitThreadPool();
		netty = new Netty(1);
		server = new HttpServer();
		// handler回显解码后的path():一次断言同时锁定"匹配命中"与"解码结果"两个环节。
		server.addHandler("/a+b", 8192, TransactionLevel.Serializable, DispatchMode.Direct,
				x -> x.sendPlainText(HttpResponseStatus.OK, x.path()));
		server.addHandler("/\u4E2D", 8192, TransactionLevel.Serializable, DispatchMode.Direct,
				x -> x.sendPlainText(HttpResponseStatus.OK, x.path()));
		var channel = server.start(netty, 0).sync().channel();
		port = ((InetSocketAddress)channel.localAddress()).getPort();
	}

	@AfterAll
	public static void tearDown() {
		server.close();
		netty.close();
	}

	// 发送原始请求行并读取一个完整响应。正常响应走keep-alive不关连接,不能像TestHttpServerCleanup
	// 那样读到EOF,改为按Content-Length定界(send()必带Content-Length):头部收齐后再等body收满。
	private static @NotNull String sendRawReadOne(@NotNull String rawPath) throws IOException {
		var raw = "GET " + rawPath + " HTTP/1.1\r\nHost: a\r\n\r\n";
		try (var sock = new Socket("127.0.0.1", port)) {
			sock.setSoTimeout(15000);
			var os = sock.getOutputStream();
			os.write(raw.getBytes(StandardCharsets.ISO_8859_1));
			os.flush();
			var out = new ByteArrayOutputStream();
			var buf = new byte[4096];
			var in = sock.getInputStream();
			int n;
			while ((n = in.read(buf)) >= 0) {
				out.write(buf, 0, n);
				var res = out.toString(StandardCharsets.ISO_8859_1);
				int headEnd = res.indexOf("\r\n\r\n");
				if (headEnd >= 0) {
					int bodyLen = contentLengthOf(res.substring(0, headEnd));
					if (bodyLen >= 0 && res.length() - headEnd - 4 >= bodyLen)
						return res;
				}
			}
			return out.toString(StandardCharsets.ISO_8859_1); // 服务器关连接(不应发生):返回已收到的让断言报错
		}
	}

	// 从响应头部解析Content-Length,缺失或非法返回-1(body收满判定永不成立,最终等soTimeout失败)
	private static int contentLengthOf(@NotNull String headers) {
		for (var line : headers.split("\r\n")) {
			var i = line.indexOf(':');
			if (i > 0 && line.substring(0, i).trim().equalsIgnoreCase("Content-Length"))
				try {
					return Integer.parseInt(line.substring(i + 1).trim());
				} catch (NumberFormatException ignored) {
					return -1;
				}
		}
		return -1;
	}

	// rawPath为请求行里的原始(未解码)路径;status为期望状态码;body为期望的响应body(=解码后path,可null不校验)。
	// 响应按ISO_8859_1读取,body是UTF-8字节流,期望值先做同构转换(ASCII时为恒等)。
	private static void assertStatus(@NotNull String rawPath, int status, @Nullable String body) throws IOException {
		var res = sendRawReadOne(rawPath);
		Assertions.assertTrue(res.startsWith("HTTP/1.1 " + status + " "), rawPath + " -> " + res);
		if (body != null) {
			var expected = new String(body.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
			Assertions.assertTrue(res.endsWith(expected), rawPath + " body: " + res);
		}
	}

	// 核心契约:'+'在path段是字面量,原样匹配已注册的"/a+b"(不被解码成空格)
	@Test
	public void testPlusLiteral() throws Exception {
		assertStatus("/a+b", 200, "/a+b");
	}

	// %2B解码为'+'后命中同一handler:转义形式与字面形式等价
	@Test
	public void testPercent2BDecodesToPlus() throws Exception {
		assertStatus("/a%2Bb", 200, "/a+b");
	}

	// %20解码为空格,未注册"/a b",不得命中"/a+b":空格与'+'在path段不同权
	@Test
	public void testPercent20SpaceNotMatch() throws Exception {
		assertStatus("/a%20b", 404, null);
	}

	// 多字节路径:注册的是解码后形式"/\u4E2D",请求以UTF-8转义/%E4%B8%AD到达,解码后命中
	@Test
	public void testMultibyteEscapedPathDecodedMatch() throws Exception {
		assertStatus("/%E4%B8%AD", 200, "/\u4E2D");
	}
}
