package Zeze.Netty;

import static java.util.Objects.requireNonNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import harness.Fast;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.InterfaceHttpPostRequestDecoder;
import io.netty.util.AttributeKey;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * 验证流式上传显式中止回调（{@link HttpAbortStreamHandle}）的契约：
 * 正常完成与异常中止严格互斥、旧三参数流式 handler 保持回退兼容、
 * {@link HttpMultipartHandle}/{@link HttpFileUploadHandle} 自动解耦并释放资源、
 * 以及异常中止路径下终态 request 释放不泄漏。
 */
@Fast
public class TestHttpStreamAbortCallback {
	private static final AttributeKey<@Nullable CompositeByteBuf> bufKey =
			AttributeKey.valueOf("TestHttpStreamAbortCallbackBuf");

	private static Socket connectWithRetry(int port) throws IOException {
		var target = new InetSocketAddress("127.0.0.1", port);
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

	private static String sendRawUntilClose(int port, String raw) throws IOException {
		try (var sock = connectWithRetry(port)) {
			sock.setSoTimeout(15_000);
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

	private static void await(String what, int timeoutMillis, java.util.function.BooleanSupplier cond)
			throws InterruptedException {
		long deadline = System.currentTimeMillis() + timeoutMillis;
		while (!cond.getAsBoolean()) {
			Assertions.assertTrue(System.currentTimeMillis() < deadline, "timeout waiting: " + what);
			//noinspection BusyWait
			Thread.sleep(5);
		}
	}

	@ParameterizedTest
	@EnumSource(value = DispatchMode.class, names = {"Direct", "Normal"})
	public void testExplicitAbortHandleDecouplesNormalCompletionAndAbort(DispatchMode mode) throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var lastExchange = new AtomicReference<@Nullable HttpExchange>();
		var server = new HttpServer() {
			@Override
			public HttpExchange createHttpExchange(ChannelHandlerContext context) {
				var x = new HttpExchange(this, context);
				lastExchange.set(x);
				return x;
			}
		};
		server.setMaxUploadSize(1024);

		var beginCount = new AtomicInteger();
		var endCount = new AtomicInteger();
		var abortCount = new AtomicInteger();
		var endSawAborted = new AtomicReference<@Nullable Boolean>();
		var abortSawAborted = new AtomicReference<@Nullable Boolean>();
		var abortSawRequest = new AtomicReference<@Nullable Boolean>();
		var releasedInEnd = new AtomicInteger();
		var releasedInAbort = new AtomicInteger();
		var beganLatch = new AtomicReference<>(new CountDownLatch(1));
		var doneLatch = new AtomicReference<>(new CountDownLatch(1));

		server.addHandler("/stream", TransactionLevel.None, mode,
				(x, from, to, size) -> {
					beginCount.incrementAndGet();
					x.attributes().attr(bufKey).set(x.channel().alloc().compositeBuffer());
					beganLatch.get().countDown();
				},
				(x, c) -> {
					var b = x.attributes().attr(bufKey).get();
					if (b != null)
						b.addComponent(true, c.content().retain());
				},
				x -> {
					endCount.incrementAndGet();
					endSawAborted.set(x.isStreamAborted());
					var b = x.attributes().attr(bufKey).getAndSet(null);
					String text = "";
					if (b != null) {
						try {
							text = b.toString(StandardCharsets.UTF_8);
						} finally {
							b.release();
							releasedInEnd.incrementAndGet();
						}
					}
					x.closeConnectionOnFlush(x.sendPlainText(HttpResponseStatus.OK, text));
					doneLatch.get().countDown();
				},
				x -> {
					abortCount.incrementAndGet();
					abortSawAborted.set(x.isStreamAborted());
					abortSawRequest.set(x.request() != null);
					var b = x.attributes().attr(bufKey).getAndSet(null);
					if (b != null) {
						b.release();
						releasedInAbort.incrementAndGet();
					}
					doneLatch.get().countDown();
				});

		try {
			int port = ((InetSocketAddress)server.start(netty, 0).sync().channel().localAddress()).getPort();

			// 1. 正常完成：仅调用 onEndStream（isStreamAborted==false），不调用 onAbortStream
			var resOk = sendRawUntilClose(port,
					"POST /stream HTTP/1.1\r\nHost: a\r\nContent-Length: 5\r\n\r\nhello");
			Assertions.assertTrue(resOk.startsWith("HTTP/1.1 200") && resOk.endsWith("hello"), resOk);
			Assertions.assertTrue(doneLatch.get().await(10, TimeUnit.SECONDS));
			Assertions.assertEquals(1, endCount.get());
			Assertions.assertEquals(0, abortCount.get());
			Assertions.assertEquals(Boolean.FALSE, endSawAborted.get());
			Assertions.assertEquals(1, releasedInEnd.get());

			// 2. 413 超限中止：仅调用 onAbortStream（isStreamAborted==true），不调用 onEndStream
			beganLatch.set(new CountDownLatch(1));
			doneLatch.set(new CountDownLatch(1));
			var overBody = "x".repeat(1025);
			var res413 = sendRawUntilClose(port,
					"POST /stream HTTP/1.1\r\nHost: a\r\nContent-Length: 2048\r\n\r\n" + overBody);
			Assertions.assertTrue(res413.startsWith("HTTP/1.1 413"), res413);
			Assertions.assertTrue(doneLatch.get().await(10, TimeUnit.SECONDS));
			Assertions.assertEquals(1, endCount.get(), "413中止不得再次调用onEndStream");
			Assertions.assertEquals(1, abortCount.get(), "413中止须调用一次onAbortStream");
			Assertions.assertEquals(Boolean.TRUE, abortSawAborted.get());
			Assertions.assertEquals(Boolean.TRUE, abortSawRequest.get(), "onAbortStream执行期间request不得提前释放");
			Assertions.assertEquals(1, releasedInAbort.get());
			var abortedEx = requireNonNull(lastExchange.get());
			await("request released after abort callback", 10_000, () -> abortedEx.request() == null);

			// 3. 客户端中途断连中止：仅调用 onAbortStream，不调用 onEndStream
			beganLatch.set(new CountDownLatch(1));
			doneLatch.set(new CountDownLatch(1));
			try (var sock = connectWithRetry(port)) {
				sock.setSoLinger(true, 0);
				var os = sock.getOutputStream();
				os.write(("POST /stream HTTP/1.1\r\nHost: a\r\nContent-Length: 500\r\n\r\npartial")
						.getBytes(StandardCharsets.ISO_8859_1));
				os.flush();
				Assertions.assertTrue(beganLatch.get().await(10, TimeUnit.SECONDS));
			}
			Assertions.assertTrue(doneLatch.get().await(10, TimeUnit.SECONDS));
			Assertions.assertEquals(1, endCount.get(), "断连中止不得调用onEndStream");
			Assertions.assertEquals(2, abortCount.get(), "断连中止须调用一次onAbortStream");
			Assertions.assertEquals(2, releasedInAbort.get());

			// 4. 畸形 chunk 解码失败（400）：仅调用 onAbortStream，不调用 onEndStream
			beganLatch.set(new CountDownLatch(1));
			doneLatch.set(new CountDownLatch(1));
			var res400 = sendRawUntilClose(port,
					"POST /stream HTTP/1.1\r\nHost: a\r\nTransfer-Encoding: chunked\r\n\r\n5\r\nhello\r\nZZZ\r\n");
			Assertions.assertTrue(res400.startsWith("HTTP/1.1 400"), res400);
			Assertions.assertTrue(doneLatch.get().await(10, TimeUnit.SECONDS));
			Assertions.assertEquals(1, endCount.get(), "畸形chunk中止不得调用onEndStream");
			Assertions.assertEquals(3, abortCount.get(), "畸形chunk中止须调用一次onAbortStream");
			Assertions.assertEquals(3, releasedInAbort.get());
			Assertions.assertEquals(4, beginCount.get());
		} finally {
			server.close();
			netty.close();
		}
	}

	@Test
	public void testLegacyThreeCallbackHandlerFallsBackToEndStreamWithAbortedFlag() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new HttpServer();
		server.setMaxUploadSize(512);

		var endCount = new AtomicInteger();
		var sawAborted = new AtomicReference<@Nullable Boolean>();
		var cleaned = new AtomicInteger();
		var done = new CountDownLatch(1);

		// 旧三参数流式注册（不传 HttpAbortStreamHandle）
		server.addHandler("/legacy", TransactionLevel.None, DispatchMode.Normal,
				(x, from, to, size) -> x.attributes().attr(bufKey).set(x.channel().alloc().compositeBuffer()),
				(x, c) -> {
					var b = x.attributes().attr(bufKey).get();
					if (b != null)
						b.addComponent(true, c.content().retain());
				},
				x -> {
					endCount.incrementAndGet();
					sawAborted.set(x.isStreamAborted());
					var b = x.attributes().attr(bufKey).getAndSet(null);
					if (b != null) {
						b.release();
						cleaned.incrementAndGet();
					}
					done.countDown();
				});

		try {
			int port = ((InetSocketAddress)server.start(netty, 0).sync().channel().localAddress()).getPort();
			var res = sendRawUntilClose(port,
					"POST /legacy HTTP/1.1\r\nHost: a\r\nContent-Length: 2048\r\n\r\n" + "a".repeat(513));
			Assertions.assertTrue(res.startsWith("HTTP/1.1 413"), res);
			Assertions.assertTrue(done.await(10, TimeUnit.SECONDS), "旧三参数handler中止须回退调用onEndStream");
			Assertions.assertEquals(1, endCount.get());
			Assertions.assertEquals(Boolean.TRUE, sawAborted.get());
			Assertions.assertEquals(1, cleaned.get());
		} finally {
			server.close();
			netty.close();
		}
	}

	@Test
	public void testDirectModeMultipartFailureOnLastContentIsMutuallyExclusive() {
		var endStreamCount = new AtomicInteger();
		var endRequestCount = new AtomicInteger();
		var abortStreamCount = new AtomicInteger();
		var abortRequestCount = new AtomicInteger();
		try (var server = new HttpServer()) {
			server.addHandler("/mp-direct", TransactionLevel.None, DispatchMode.Direct, new HttpMultipartHandle() {
				@Override
				public void onAttribute(HttpExchange x, Attribute attr) {
					throw new RuntimeException("simulated failure in Direct mode");
				}

				@Override
				public void onEndStream(HttpExchange x) throws Exception {
					endStreamCount.incrementAndGet();
					HttpMultipartHandle.super.onEndStream(x);
				}

				@Override
				public void onEndRequest(HttpExchange x, InterfaceHttpPostRequestDecoder decoder) {
					endRequestCount.incrementAndGet();
				}

				@Override
				public void onAbortStream(HttpExchange x) throws Exception {
					abortStreamCount.incrementAndGet();
					HttpMultipartHandle.super.onAbortStream(x);
				}

				@Override
				public void onAbortRequest(HttpExchange x) {
					abortRequestCount.incrementAndGet();
				}
			});

			var channel = new EmbeddedChannel(server);
			try {
				var req = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/mp-direct");
				req.headers().set(HttpHeaderNames.CONTENT_TYPE, "multipart/form-data; boundary=B");
				channel.writeInbound(req);
				var body = Unpooled.copiedBuffer(
						"--B\r\nContent-Disposition: form-data; name=\"k\"\r\n\r\nv\r\n--B--\r\n",
						StandardCharsets.US_ASCII);
				channel.writeInbound(new DefaultLastHttpContent(body));

				Assertions.assertEquals(0, endStreamCount.get(), "Direct模式onStreamContent关闭后不得再调onEndStream");
				Assertions.assertEquals(0, endRequestCount.get(), "Direct模式失败后不得调用onEndRequest");
				Assertions.assertEquals(1, abortStreamCount.get(), "Direct模式提前关闭须调用一次onAbortStream");
				Assertions.assertEquals(1, abortRequestCount.get(), "Direct模式提前关闭须调用一次onAbortRequest");
				Assertions.assertNull(channel.attr(HttpMultipartHandle.decoderKey).get(), "decoder须已清理");
			} finally {
				channel.finishAndReleaseAll();
			}
		}
	}

	@Test
	public void testMultipartHandlerConstructorAndLegacyThreeArgWiring() {
		var endStreamConvenience = new AtomicInteger();
		var endRequestConvenience = new AtomicInteger();
		var abortRequestConvenience = new AtomicInteger();
		var mpConvenience = new HttpMultipartHandle() {
			@Override
			public void onEndStream(HttpExchange x) throws Exception {
				endStreamConvenience.incrementAndGet();
				HttpMultipartHandle.super.onEndStream(x);
			}

			@Override
			public void onEndRequest(HttpExchange x, InterfaceHttpPostRequestDecoder decoder) {
				endRequestConvenience.incrementAndGet();
			}

			@Override
			public void onAbortRequest(HttpExchange x) {
				abortRequestConvenience.incrementAndGet();
			}
		};

		var endRequestLegacy = new AtomicInteger();
		var abortRequestLegacy = new AtomicInteger();
		var mpLegacy = new HttpMultipartHandle() {
			@Override
			public void onEndRequest(HttpExchange x, InterfaceHttpPostRequestDecoder decoder) {
				endRequestLegacy.incrementAndGet();
			}

			@Override
			public void onAbortRequest(HttpExchange x) {
				abortRequestLegacy.incrementAndGet();
			}
		};

		try (var server = new HttpServer()) {
			server.addHandler("/convenience", new HttpHandler(TransactionLevel.None, DispatchMode.Direct, mpConvenience));
			server.addHandler("/legacy3", new HttpHandler(TransactionLevel.None, DispatchMode.Direct,
					mpLegacy, mpLegacy, mpLegacy));

			var ch1 = new EmbeddedChannel(server);
			try {
				var req = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/convenience");
				req.headers().set(HttpHeaderNames.CONTENT_TYPE, "multipart/form-data; boundary=B");
				ch1.writeInbound(req);
				ch1.writeInbound(new DefaultHttpContent(Unpooled.copiedBuffer("--B\r\n", StandardCharsets.US_ASCII)));
				ch1.close(); // 触发 channelInactive 中止
				Assertions.assertEquals(0, endStreamConvenience.get());
				Assertions.assertEquals(0, endRequestConvenience.get());
				Assertions.assertEquals(1, abortRequestConvenience.get());
				Assertions.assertNull(ch1.attr(HttpMultipartHandle.decoderKey).get());
			} finally {
				ch1.finishAndReleaseAll();
			}

			var ch2 = new EmbeddedChannel(server);
			try {
				var req = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/legacy3");
				req.headers().set(HttpHeaderNames.CONTENT_TYPE, "multipart/form-data; boundary=B");
				ch2.writeInbound(req);
				ch2.writeInbound(new DefaultHttpContent(Unpooled.copiedBuffer("--B\r\n", StandardCharsets.US_ASCII)));
				ch2.close(); // 旧三参数构造器回退到 onEndStream(isStreamAborted==true) -> 转调 onAbortStream
				Assertions.assertEquals(0, endRequestLegacy.get());
				Assertions.assertEquals(1, abortRequestLegacy.get());
				Assertions.assertNull(ch2.attr(HttpMultipartHandle.decoderKey).get());
			} finally {
				ch2.finishAndReleaseAll();
			}

			var rawCompleted = new AtomicInteger();
			var rawEndRequest = new AtomicInteger();
			var rawAbortRequest = new AtomicInteger();
			var rawUpload = new HttpFileUploadHandle() {
				@Override
				public void onFileCompleted(HttpExchange x, FileUpload fileUpload) {
					rawCompleted.incrementAndGet();
				}

				@Override
				public void onEndRequest(HttpExchange x, InterfaceHttpPostRequestDecoder decoder) {
					rawEndRequest.incrementAndGet();
				}

				@Override
				public void onAbortRequest(HttpExchange x) {
					rawAbortRequest.incrementAndGet();
				}
			};
			server.addHandler("/raw-legacy3", new HttpHandler(TransactionLevel.None, DispatchMode.Direct,
					rawUpload, rawUpload, rawUpload));
			server.addHandler("/raw-normal", TransactionLevel.None, DispatchMode.Direct, rawUpload);

			var ch3 = new EmbeddedChannel(server);
			try {
				var req = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/raw-legacy3?filename=a.bin");
				ch3.writeInbound(req);
				ch3.writeInbound(new DefaultHttpContent(Unpooled.copiedBuffer("part", StandardCharsets.US_ASCII)));
				ch3.close();
				Assertions.assertEquals(0, rawCompleted.get());
				Assertions.assertEquals(0, rawEndRequest.get());
				Assertions.assertEquals(1, rawAbortRequest.get());
				Assertions.assertNull(ch3.attr(HttpFileUploadHandle.fileUploadKey).get());
			} finally {
				ch3.finishAndReleaseAll();
			}

			var ch4 = new EmbeddedChannel(server);
			try {
				var req = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/raw-normal?filename=b.bin");
				ch4.writeInbound(req);
				ch4.writeInbound(new DefaultLastHttpContent(Unpooled.copiedBuffer("full", StandardCharsets.US_ASCII)));
				Assertions.assertEquals(1, rawCompleted.get());
				Assertions.assertEquals(1, rawEndRequest.get());
				Assertions.assertEquals(1, rawAbortRequest.get(), "正常完成不得触发onAbortRequest");
				Assertions.assertNull(ch4.attr(HttpFileUploadHandle.fileUploadKey).get());
			} finally {
				ch4.finishAndReleaseAll();
			}
		}
	}

	@ParameterizedTest
	@EnumSource(value = DispatchMode.class, names = {"Direct", "Normal"})
	public void testAbortCallbackExceptionStillReleasesRequest(DispatchMode mode) throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var lastExchange = new AtomicReference<@Nullable HttpExchange>();
		var server = new HttpServer() {
			@Override
			public HttpExchange createHttpExchange(ChannelHandlerContext context) {
				var x = new HttpExchange(this, context);
				lastExchange.set(x);
				return x;
			}
		};
		server.setMaxUploadSize(256);
		var abortCalled = new CountDownLatch(1);
		server.addHandler("/abort-throws", TransactionLevel.None, mode,
				(x, from, to, size) -> { },
				(x, c) -> { },
				x -> Assertions.fail("onEndStream must not run on abort"),
				x -> {
					abortCalled.countDown();
					throw new RuntimeException("simulated abort callback exception");
				});
		try {
			int port = ((InetSocketAddress)server.start(netty, 0).sync().channel().localAddress()).getPort();
			var res = sendRawUntilClose(port,
					"POST /abort-throws HTTP/1.1\r\nHost: a\r\nContent-Length: 1024\r\n\r\n" + "z".repeat(257));
			Assertions.assertTrue(res.startsWith("HTTP/1.1 413"), res);
			Assertions.assertTrue(abortCalled.await(10, TimeUnit.SECONDS));
			var ex = requireNonNull(lastExchange.get());
			await("exchange closed and request released after abort exception", 10_000,
					() -> ex.isClosed() && ex.request() == null);
		} finally {
			server.close();
			netty.close();
		}
	}
}
