package UnitTest.Zeze.Netty;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.handler.codec.http2.Http2StreamChannelBootstrap;
import io.netty.handler.codec.http2.Http2StreamFrameToHttpObjectCodec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * h2测试客户端（仅测试用）：netty自client栈prior-knowledge连接，共享一条连接按需开stream发请求。
 * 本机两套curl的libcurl均无HTTP2支持，测试族统一经此客户端。
 */
final class H2TestClient implements AutoCloseable {
	private final NioEventLoopGroup group = new NioEventLoopGroup(1);
	private Channel connection;
	private io.netty.handler.codec.http2.Http2FrameCodec frameCodec;

	record Result(HttpResponseStatus status, byte @NotNull [] body) {
		@NotNull String bodyText() {
			return new String(body, java.nio.charset.StandardCharsets.ISO_8859_1);
		}
	}

	interface ResponseFuture {
		@NotNull CompletableFuture<Result> done();

		default Result await() throws Exception {
			return done().get(15, TimeUnit.SECONDS);
		}
	}

	void connect(int port) throws Exception {
		var bootstrap = new Bootstrap().group(group)
				.channel(NioSocketChannel.class)
				.handler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(@NotNull SocketChannel ch) {
						frameCodec = Http2FrameCodecBuilder.forClient()
								.initialSettings(new io.netty.handler.codec.http2.Http2Settings()
										.initialWindowSize(16 * 1024 * 1024)) // 流级接收窗
								.build();
						ch.pipeline().addLast(frameCodec);
						ch.pipeline().addLast(new Http2MultiplexHandler(new ChannelInitializer<Http2StreamChannel>() {
							@Override
							protected void initChannel(@NotNull Http2StreamChannel s) {
								// 客户端发起的stream由request()开stream后按需装配
							}
						}));
					}
				});
		connection = bootstrap.connect("127.0.0.1", port).sync().channel();
	}

	// 共享连接上开一个新stream发请求（并发调用=多stream并发）；body为null即无体GET/HEAD
	ResponseFuture request(@NotNull HttpMethod method, @NotNull String path, byte @Nullable [] body) throws Exception {
		var stream = new Http2StreamChannelBootstrap(connection).open().sync().getNow();
		var collector = new Collector(frameCodec);
		stream.pipeline().addLast(new Http2StreamFrameToHttpObjectCodec(false)); // client模式
		stream.pipeline().addLast(collector);
		ByteBuf content = body == null ? Unpooled.EMPTY_BUFFER : Unpooled.wrappedBuffer(body);
		var req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, path, content);
		req.headers().set(HttpHeaderNames.HOST, "127.0.0.1"); // codec取Host转:authority
		if (body != null)
			req.headers().set(HttpHeaderNames.CONTENT_LENGTH, body.length);
		stream.writeAndFlush(req);
		return collector;
	}

	@Override
	public void close() throws Exception {
		if (connection != null)
			connection.close().sync();
		group.shutdownGracefully().sync();
	}

	private static final class Collector extends SimpleChannelInboundHandler<HttpObject> implements ResponseFuture {
		private final CompletableFuture<Result> done = new CompletableFuture<>();
		private final io.netty.handler.codec.http2.Http2FrameCodec frameCodec;
		private HttpResponseStatus status;
		private final java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();

		Collector(io.netty.handler.codec.http2.Http2FrameCodec frameCodec) {
			this.frameCodec = frameCodec;
		}

		@Override
		public @NotNull CompletableFuture<Result> done() {
			return done;
		}

		@Override
		protected void channelRead0(@NotNull ChannelHandlerContext ctx, @NotNull HttpObject msg) {
			if (msg instanceof HttpResponse response)
				status = response.status();
			if (msg instanceof HttpContent content) {
				int n = content.content().readableBytes();
				var b = content.content();
				var bytes = new byte[b.readableBytes()];
				b.readBytes(bytes);
				out.write(bytes, 0, bytes.length);
				if (msg instanceof LastHttpContent)
					done.complete(new Result(status, out.toByteArray()));
			}
		}

		@Override
		public void exceptionCaught(@NotNull ChannelHandlerContext ctx, @NotNull Throwable cause) {
			done.completeExceptionally(cause);
		}
	}
}
