package UnitTest.Zeze.Netty;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import Zeze.Netty.HttpExchange;
import Zeze.Netty.HttpServer;
import Zeze.Netty.HttpWebSocketHandle;
import Zeze.Netty.Netty;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import io.netty.buffer.CompositeByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.AttributeKey;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestNettyHttpServer {
	private static Netty netty;
	private static HttpServer server;
	private static int port;

	@BeforeClass
	public static void setUp() throws Exception {
		netty = new Netty();
		server = new HttpServer();

		server.addHandler("/testFull", 1024, TransactionLevel.Serializable, DispatchMode.Direct,
				x -> x.sendPlainText(HttpResponseStatus.OK, "fullBody"));

		var ck = AttributeKey.<CompositeByteBuf>valueOf("c");
		server.addHandler("/testStream", TransactionLevel.Serializable, DispatchMode.Direct,
				(x, from, to, size) -> x.attributes().attr(ck).set(x.channel().alloc().compositeBuffer()),
				(x, c) -> x.attributes().attr(ck).get().addComponent(true, c.content().retain()),
				x -> {
					var b = x.attributes().attr(ck).getAndSet(null);
					x.sendPlainText(HttpResponseStatus.OK, b.toString(StandardCharsets.UTF_8));
					b.release();
				});

		server.addHandler("/testWebSocket", TransactionLevel.Serializable, DispatchMode.Direct,
				new HttpWebSocketHandle() {
					@Override
					public void onText(HttpExchange x, String text) {
						x.sendWebSocket(text);
					}

					@Override
					public void onClose(HttpExchange x, int status, String reason) {
						System.out.println("server onClose: " + status + ", " + reason);
					}
				});
		var channel = server.start(netty, 0).sync().channel();
		port = ((InetSocketAddress)channel.localAddress()).getPort();
		System.out.println("netty bind port " + port);
	}

	@AfterClass
	public static void tearDown() {
		server.close();
		netty.close();
	}

	public static final class HttpResponseStringBody implements HttpResponse.BodySubscriber<String> {
		private final CompletableFuture<String> cf = new CompletableFuture<>();
		private final ByteBuffer buffer = ByteBuffer.Allocate();

		@Override
		public CompletionStage<String> getBody() {
			return cf;
		}

		@Override
		public void onSubscribe(Flow.Subscription subs) {
			subs.request(1024); // onNext最多处理的item数量
		}

		@Override
		public void onNext(List<java.nio.ByteBuffer> list) {
			for (var bb : list) {
				var size = bb.remaining();
				buffer.EnsureWrite(size);
				bb.get(buffer.Bytes, buffer.WriteIndex, size);
				buffer.WriteIndex += size;
			}
		}

		@Override
		public void onComplete() {
			cf.complete(new String(buffer.Bytes, buffer.ReadIndex, buffer.Size(), StandardCharsets.UTF_8));
		}

		@Override
		public void onError(Throwable ex) {
			ex.printStackTrace();
		}
	}

	public static final class HttpRequestStringBody implements HttpRequest.BodyPublisher {
		private final byte[] body;

		public HttpRequestStringBody(String body) {
			this.body = body.getBytes(StandardCharsets.UTF_8);
		}

		@Override
		public void subscribe(Flow.Subscriber<? super java.nio.ByteBuffer> subscriber) {
			subscriber.onSubscribe(new Flow.Subscription() {
				private boolean canceled;
				private boolean finished;

				@Override
				public void request(long n) {
					if (canceled)
						return;
					if (n <= 0)
						subscriber.onError(new IllegalArgumentException());
					else if (!finished) {
						finished = true;
						subscriber.onNext(java.nio.ByteBuffer.wrap(body));
					} else
						subscriber.onComplete();
				}

				@Override
				public void cancel() {
					canceled = true;
				}
			});
		}

		@Override
		public long contentLength() {
			return -1; // for chunked
		}
	}

	@Test
	public void testFullHttp() throws IOException, InterruptedException {
		var res = HttpClient.newHttpClient().send(HttpRequest.newBuilder()
				.uri(URI.create("http://127.0.0.1:" + port + "/testFull")).GET().build(), h -> {
			Assert.assertEquals(200, h.statusCode());
			return new HttpResponseStringBody();
		});
		Assert.assertEquals("fullBody", res.body());
	}

	@Test
	public void testStreamHttp() throws IOException, InterruptedException {
		var res = HttpClient.newHttpClient().send(HttpRequest.newBuilder()
				.uri(URI.create("http://127.0.0.1:" + port + "/testStream"))
				.POST(new HttpRequestStringBody("streamBody")).build(), h -> {
			Assert.assertEquals(200, h.statusCode());
			return new HttpResponseStringBody();
		});
		Assert.assertEquals("streamBody", res.body());
	}

	@Test
	public void testWebSocket() throws ExecutionException, InterruptedException {
		var checked = new AtomicBoolean();
		var ws = HttpClient.newHttpClient().newWebSocketBuilder().buildAsync(
				URI.create("ws://127.0.0.1:" + port + "/testWebSocket"), new WebSocket.Listener() {
					private final StringBuilder sb = new StringBuilder();

					@Override
					public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
						sb.append(data);
						if (last) {
							Assert.assertEquals("webSocketText", sb.toString());
							checked.set(true);
							sb.setLength(0);
							ws.sendClose(WebSocket.NORMAL_CLOSURE, "");
						}
						ws.request(1);
						return null;
					}

					@Override
					public CompletionStage<?> onClose(WebSocket webSocket, int status, String reason) {
						System.out.println("client onClose: " + status + ", " + reason);
						return null;
					}

					@Override
					public void onError(WebSocket ws, Throwable ex) {
						ex.printStackTrace();
					}
				}).get();
		ws.sendText("webSocketText", true).get();
		while (!ws.isInputClosed()) {
			//noinspection BusyWait
			Thread.sleep(100);
		}
		Assert.assertTrue(checked.get());
	}
}
