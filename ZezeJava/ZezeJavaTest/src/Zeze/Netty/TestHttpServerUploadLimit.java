package Zeze.Netty;

import static java.util.Objects.requireNonNull;

import org.jspecify.annotations.Nullable;

import harness.Fast;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.Netty.HttpExchange;
import Zeze.Netty.HttpFileUploadHandle;
import Zeze.Netty.HttpMultipartHandle;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.InterfaceHttpPostRequestDecoder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * FND-N2-5回归:流模式上传(multipart/raw)不受handler.MaxContentLength约束,
 * multipart默认实现(DefaultHttpDataFactory全堆内,maxSize=Long.MAX_VALUE)可被无限大body打爆堆,
 * raw上传(MixedFileUpload definedSize=0,未setMaxSize)超16KB落盘后无限增长耗尽磁盘。
 * 修复后流模式请求body总量以server.maxUploadSize兜底,超限回413并断开连接。
 */
@Fast
public class TestHttpServerUploadLimit {
	private static final int MaxUpload = 8192;
	private static @Nullable Netty netty;
	private static @Nullable HttpServer server;
	private static int port;

	@BeforeAll
	public static void setUp() throws Exception {
		Task.tryInitThreadPool();
		netty = new Netty(1);
		server = new HttpServer(); // 无zeze, noProcedure, Direct派发inline执行
		server.setMaxUploadSize(MaxUpload);
		// 流模式上传handler:不缓冲content(边收边丢),结束时回200
		server.addHandler("/upload", TransactionLevel.None, DispatchMode.Direct,
				(x, from, to, size) -> { },
				(x, c) -> { },
				x -> x.sendPlainText(HttpResponseStatus.OK, "ok"));
		var channel = server.start(netty, 0).sync().channel();
		port = ((InetSocketAddress)channel.localAddress()).getPort();
	}

	@AfterAll
	public static void tearDown() {
		requireNonNull(server).close();
		requireNonNull(netty).close();
	}

	// 发送原始请求字节并读到服务器关闭连接(EOF)为止,返回收到的完整响应
	private static String sendRawUntilClose(String raw) throws IOException {
		return sendRawUntilClose(port, raw);
	}

	private static String sendRawUntilClose(int targetPort, String raw) throws IOException {
		try (var sock = connectWithRetry(targetPort)) {
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

	// Windows满负载下loopback connect偶发SYN无应答（首轮与第五轮60轮压测各1例：~21s OS级
	// ETIMEDOUT，同刻8+测试服务器并行启动的连接风暴，服务端日志闭环无责）：显式短超时+有界重试。
	// 两种超时形态都要接：SocketTimeoutException=java侧计时器先到；ConnectException(time out)=
	// OS级ETIMEDOUT先到（第五轮round 41形态，原只接SocketTimeoutException漏过直穿）。
	// connect失败后Socket不可复用，每次尝试用新Socket。
	private static Socket connectWithRetry(int targetPort) throws IOException {
		var target = new InetSocketAddress("127.0.0.1", targetPort);
		for (int attempt = 1; ; ++attempt) {
			var sock = new Socket();
			try {
				sock.connect(target, 5_000);
				return sock;
			} catch (SocketTimeoutException | ConnectException e) {
				sock.close();
				if (attempt >= 3)
					throw e;
			}
		}
	}

	// 只发count块、不带终止块:让服务器在读完全部已发数据后恰好触发超限关闭,
	// 避免接收缓冲残留未读数据导致close时内核发RST抹掉413响应
	private static String chunkedBodyNoEnd(int chunkSize, int chunkCount) {
		var sb = new StringBuilder();
		var chunk = new String(new char[chunkSize]).replace('\0', 'x');
		for (int i = 0; i < chunkCount; i++)
			sb.append(Integer.toHexString(chunkSize)).append("\r\n").append(chunk).append("\r\n");
		return sb.toString();
	}

	// 正常路径:总量恰等于上限(不超),完整处理返回200
	@Test
	public void testUploadWithinLimit() throws Exception {
		var body = new byte[MaxUpload];
		var res = HttpClient.newHttpClient().send(HttpRequest.newBuilder()
				.uri(URI.create("http://127.0.0.1:" + port + "/upload"))
				.POST(HttpRequest.BodyPublishers.ofByteArray(body)).build(),
				HttpResponse.BodyHandlers.ofString());
		Assertions.assertEquals(200, res.statusCode());
		Assertions.assertEquals("ok", res.body());
	}

	// chunked上传超限(无Content-Length,raw上传definedSize=0的场景):回413并关闭连接
	@Test
	public void testChunkedUploadOverLimit() throws Exception {
		// 3*4096=12288 > 8192,第3块触发超限
		var raw = "POST /upload HTTP/1.1\r\nHost: a\r\nTransfer-Encoding: chunked\r\n\r\n"
				+ chunkedBodyNoEnd(4096, 3);
		var res = sendRawUntilClose(raw);
		Assertions.assertTrue(res.startsWith("HTTP/1.1 413"), res);
	}

	// Content-Length声明大小同样受总量上限约束(chunked可谎报/省略声明,检查必须按实际接收字节累计)
	@Test
	public void testContentLengthUploadOverLimit() throws Exception {
		// 只发送恰超限的字节数(声明仍谎报2倍上限)：服务端读到最后一字节触发超限时接收缓冲
		// 已空，close为有序FIN——对齐chunkedBodyNoEnd的技巧(见其注释)。一次性发全2倍上限
		// 时满负载并行下在途未读数据残留，close时内核发RST抹掉已排队的413(Windows实测竞态)。
		var body = new String(new char[MaxUpload + 1]).replace('\0', 'y');
		var raw = "POST /upload HTTP/1.1\r\nHost: a\r\nContent-Length: " + MaxUpload * 2
				+ "\r\n\r\n" + body;
		var res = sendRawUntilClose(raw);
		Assertions.assertTrue(res.startsWith("HTTP/1.1 413"), res);
	}

	@Test
	public void testAbortedOverLimitFileUploadSkipsCompletionCallbacks() throws Exception {
		var localNetty = new Netty(1);
		var localServer = new HttpServer();
		localServer.setCheckIdleInterval(1);
		localServer.setReadIdleTimeout(1);
		localServer.setWriteIdleTimeout(2);
		localServer.setMaxUploadSize(4096);
		var began = new CountDownLatch(1);
		var aborted = new CountDownLatch(1);
		var channelRef = new AtomicReference<@Nullable Channel>();
		var endStreamCount = new AtomicInteger();
		var abortRequestCount = new AtomicInteger();
		var abortSawAbortedFlag = new AtomicReference<@Nullable Boolean>();
		var abortSawNullAttr = new AtomicReference<@Nullable Boolean>();
		var fileCompletedCount = new AtomicInteger();
		var endRequestCount = new AtomicInteger();
		localServer.addHandler("/file-limit", TransactionLevel.None, DispatchMode.Normal, new HttpFileUploadHandle() {
			@Override
			public void onBeginStream(HttpExchange x, long from, long to, long size) throws Exception {
				HttpFileUploadHandle.super.onBeginStream(x, from, to, size);
				channelRef.set(x.channel());
				began.countDown();
			}

			@Override
			public void onEndStream(HttpExchange x) throws Exception {
				endStreamCount.incrementAndGet();
				HttpFileUploadHandle.super.onEndStream(x);
			}

			@Override
			public void onAbortRequest(HttpExchange x) {
				abortSawAbortedFlag.set(x.isStreamAborted());
				abortSawNullAttr.set(x.channel().attr(HttpFileUploadHandle.fileUploadKey).get() == null);
				abortRequestCount.incrementAndGet();
				aborted.countDown();
			}

			@Override
			public void onFileCompleted(HttpExchange x, FileUpload fileUpload) {
				fileCompletedCount.incrementAndGet();
			}

			@Override
			public void onEndRequest(HttpExchange x, InterfaceHttpPostRequestDecoder decoder) {
				endRequestCount.incrementAndGet();
			}
		});
		try {
			int localPort = ((InetSocketAddress)localServer.start(localNetty, 0).sync().channel().localAddress()).getPort();
			var body = new String(new char[4097]).replace('\0', 'a');
			var raw = "POST /file-limit?filename=big.bin HTTP/1.1\r\nHost: a\r\nContent-Length: 8192\r\n\r\n" + body;
			var res = sendRawUntilClose(localPort, raw);
			Assertions.assertTrue(res.startsWith("HTTP/1.1 413"), res);
			Assertions.assertTrue(began.await(10, TimeUnit.SECONDS), "onBeginStream须已执行");
			Assertions.assertTrue(aborted.await(10, TimeUnit.SECONDS), "onAbortRequest资源清理须已执行");
			Assertions.assertEquals(0, endStreamCount.get(), "413中止上传不得触发onEndStream");
			Assertions.assertEquals(1, abortRequestCount.get(), "413中止上传须触发一次onAbortRequest");
			Assertions.assertEquals(Boolean.TRUE, abortSawAbortedFlag.get(), "onAbortRequest时isStreamAborted须为true");
			Assertions.assertEquals(Boolean.TRUE, abortSawNullAttr.get(), "onAbortRequest前须已释放MixedFileUpload");
			Assertions.assertEquals(0, fileCompletedCount.get(), "413中止上传不得触发onFileCompleted");
			Assertions.assertEquals(0, endRequestCount.get(), "413中止上传不得触发onEndRequest");
			var ch = requireNonNull(channelRef.get());
			Assertions.assertNull(ch.attr(HttpFileUploadHandle.fileUploadKey).get(), "channel上的MixedFileUpload须已释放");
		} finally {
			localServer.close();
			localNetty.close();
		}
	}

	@Test
	public void testClientDisconnectMidUploadSkipsCompletionCallbacks() throws Exception {
		var localNetty = new Netty(1);
		var localServer = new HttpServer();
		localServer.setCheckIdleInterval(1);
		localServer.setReadIdleTimeout(1);
		localServer.setWriteIdleTimeout(2);
		var rawBegan = new CountDownLatch(1);
		var rawAborted = new CountDownLatch(1);
		var rawChannel = new AtomicReference<@Nullable Channel>();
		var rawEndStream = new AtomicInteger();
		var rawAbortRequest = new AtomicInteger();
		var rawCompleted = new AtomicInteger();
		var rawEndRequest = new AtomicInteger();
		localServer.addHandler("/raw-cut", TransactionLevel.None, DispatchMode.Normal, new HttpFileUploadHandle() {
			@Override
			public void onBeginStream(HttpExchange x, long from, long to, long size) throws Exception {
				HttpFileUploadHandle.super.onBeginStream(x, from, to, size);
				rawChannel.set(x.channel());
				rawBegan.countDown();
			}

			@Override
			public void onEndStream(HttpExchange x) throws Exception {
				rawEndStream.incrementAndGet();
				HttpFileUploadHandle.super.onEndStream(x);
			}

			@Override
			public void onAbortRequest(HttpExchange x) {
				rawAbortRequest.incrementAndGet();
				rawAborted.countDown();
			}

			@Override
			public void onFileCompleted(HttpExchange x, FileUpload fileUpload) {
				rawCompleted.incrementAndGet();
			}

			@Override
			public void onEndRequest(HttpExchange x, InterfaceHttpPostRequestDecoder decoder) {
				rawEndRequest.incrementAndGet();
			}
		});

		var mpBegan = new CountDownLatch(1);
		var mpAborted = new CountDownLatch(1);
		var mpChannel = new AtomicReference<@Nullable Channel>();
		var mpEndStream = new AtomicInteger();
		var mpAbortRequest = new AtomicInteger();
		var mpCompleted = new AtomicInteger();
		var mpEndRequest = new AtomicInteger();
		localServer.addHandler("/mp-cut", TransactionLevel.None, DispatchMode.Normal, new HttpMultipartHandle() {
			@Override
			public void onBeginStream(HttpExchange x, long from, long to, long size) throws Exception {
				HttpMultipartHandle.super.onBeginStream(x, from, to, size);
				mpChannel.set(x.channel());
				mpBegan.countDown();
			}

			@Override
			public void onEndStream(HttpExchange x) throws Exception {
				mpEndStream.incrementAndGet();
				HttpMultipartHandle.super.onEndStream(x);
			}

			@Override
			public void onAbortRequest(HttpExchange x) {
				mpAbortRequest.incrementAndGet();
				mpAborted.countDown();
			}

			@Override
			public void onFileCompleted(HttpExchange x, FileUpload fileUpload) {
				mpCompleted.incrementAndGet();
			}

			@Override
			public void onEndRequest(HttpExchange x, InterfaceHttpPostRequestDecoder decoder) {
				mpEndRequest.incrementAndGet();
			}
		});

		try {
			int localPort = ((InetSocketAddress)localServer.start(localNetty, 0).sync().channel().localAddress()).getPort();
			try (var sock = connectWithRetry(localPort)) {
				sock.setSoLinger(true, 0); // 强制 RST 模拟客户端异常断连（立即触发 channelInactive）
				var os = sock.getOutputStream();
				var partial = new String(new char[1000]).replace('\0', 'r');
				os.write(("POST /raw-cut?filename=cut.bin HTTP/1.1\r\nHost: a\r\nContent-Length: 10000\r\n\r\n" + partial)
						.getBytes(StandardCharsets.ISO_8859_1));
				os.flush();
				Assertions.assertTrue(rawBegan.await(10, TimeUnit.SECONDS), "raw onBeginStream须已执行");
			}
			Assertions.assertTrue(rawAborted.await(10, TimeUnit.SECONDS), "raw断连中止后须触发onAbortRequest并清理资源");
			Assertions.assertEquals(0, rawEndStream.get(), "raw断连中止不得触发onEndStream");
			Assertions.assertEquals(1, rawAbortRequest.get(), "raw断连中止须触发一次onAbortRequest");
			Assertions.assertEquals(0, rawCompleted.get(), "raw断连中止不得触发onFileCompleted");
			Assertions.assertEquals(0, rawEndRequest.get(), "raw断连中止不得触发onEndRequest");
			Assertions.assertNull(requireNonNull(rawChannel.get()).attr(HttpFileUploadHandle.fileUploadKey).get(),
					"raw断连中止须清理fileUploadKey");

			try (var sock = connectWithRetry(localPort)) {
				var os = sock.getOutputStream();
				var partialMp = "--B\r\nContent-Disposition: form-data; name=\"f\"; filename=\"cut.bin\"\r\n"
						+ "Content-Type: application/octet-stream\r\n\r\n"
						+ new String(new char[500]).replace('\0', 'm');
				os.write(("POST /mp-cut HTTP/1.1\r\nHost: a\r\nContent-Type: multipart/form-data; boundary=B\r\n"
						+ "Content-Length: 10000\r\n\r\n" + partialMp).getBytes(StandardCharsets.ISO_8859_1));
				os.flush();
				Assertions.assertTrue(mpBegan.await(10, TimeUnit.SECONDS), "multipart onBeginStream须已执行");
			}
			Assertions.assertTrue(mpAborted.await(10, TimeUnit.SECONDS), "multipart断连中止后须触发onAbortRequest并清理资源");
			Assertions.assertEquals(0, mpEndStream.get(), "multipart断连中止不得触发onEndStream");
			Assertions.assertEquals(1, mpAbortRequest.get(), "multipart断连中止须触发一次onAbortRequest");
			Assertions.assertEquals(0, mpCompleted.get(), "multipart断连中止不得触发onFileCompleted");
			Assertions.assertEquals(0, mpEndRequest.get(), "multipart断连中止不得触发onEndRequest");
			Assertions.assertNull(requireNonNull(mpChannel.get()).attr(HttpMultipartHandle.decoderKey).get(),
					"multipart断连中止须清理decoderKey");
		} finally {
			localServer.close();
			localNetty.close();
		}
	}
}
