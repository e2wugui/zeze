package UnitTest.Zeze.Netty;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import harness.Fast;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * h2c prior-knowledge冒烟（PR2管线装配的入门验证，深度测试族见后续PR3）：
 * netty自带h2客户端栈（Http2FrameCodec+Http2MultiplexHandler+client模式帧翻译）以
 * "PRI * HTTP/2.0"预言前缀直连，服务端H2Transport.PrefaceDetector换栈，帧翻译codec把请求
 * 转为HttpRequest进现有HttpServer/HttpExchange状态机、响应转回帧——交换层零改动复用的
 * 端到端实证。同端口h1客户端共存（探测器h1路径自移除透传）一并覆盖。
 * （本机两套curl的libcurl均无HTTP2支持，故客户端用netty自栈。）
 */
@Fast
public class TestHttp2PriorKnowledge {

	@Test
	public void testPriorKnowledgeAndH1Coexist() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new HttpServer();
		server.addHandler("/echo", 8192, TransactionLevel.None, DispatchMode.Normal,
				x -> x.sendPlainText(HttpResponseStatus.OK, "h2-body"));
		var clientGroup = new NioEventLoopGroup(1);
		try {
			var port = ((InetSocketAddress)server.start(netty, 0).sync().channel().localAddress()).getPort();

			// h2：两个stream（顺序各开各的）都拿到完整响应体
			Assertions.assertEquals("h2-body", h2Get(clientGroup, port, "/echo"), "h2 stream#1响应体");
			Assertions.assertEquals("h2-body", h2Get(clientGroup, port, "/echo"), "h2 stream#2响应体");

			// 同端口h1共存：探测器对h1连接自移除透传，h1路径零变化
			// （java.net.http.HttpResponse与netty的HttpResponse同名，此处全限定）
			var h1 = java.net.http.HttpClient.newBuilder().version(java.net.http.HttpClient.Version.HTTP_1_1).build();
			var body = h1.send(java.net.http.HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/echo")).GET().build(),
					java.net.http.HttpResponse.BodyHandlers.ofString()).body();
			Assertions.assertEquals("h2-body", body, "h1与h2同端口共存，h1行为不变");
		} finally {
			clientGroup.shutdownGracefully().sync();
			server.close();
			netty.close();
		}
	}

	// prior-knowledge单stream GET：连接级[h2 frame codec + 多路复用]，stream级[client帧翻译+收集器]
	private static String h2Get(NioEventLoopGroup group, int port, String path) throws Exception {
		Channel connection = null;
		try {
			var bootstrap = new Bootstrap().group(group)
					.channel(NioSocketChannel.class)
					.handler(new ChannelInitializer<SocketChannel>() {
						@Override
						protected void initChannel(@NotNull SocketChannel ch) {
							ch.pipeline().addLast(Http2FrameCodecBuilder.forClient().build());
							ch.pipeline().addLast(new Http2MultiplexHandler(new ChannelInitializer<Http2StreamChannel>() {
								@Override
								protected void initChannel(@NotNull Http2StreamChannel s) {
									// 客户端侧stream不自动装配：由open()后按需添加
								}
							}));
						}
					});
			connection = bootstrap.connect("127.0.0.1", port).sync().channel();
			var stream = new Http2StreamChannelBootstrap(connection).open().sync().getNow();
			var collector = new ResponseCollector();
			stream.pipeline().addLast(new Http2StreamFrameToHttpObjectCodec(false)); // client模式
			stream.pipeline().addLast(collector);
			FullHttpRequest req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, path);
			req.headers().set(HttpHeaderNames.HOST, "127.0.0.1:" + port); // codec取Host转:authority
			stream.writeAndFlush(req);
			return collector.done.get(10, TimeUnit.SECONDS);
		} finally {
			if (connection != null)
				connection.close().sync();
		}
	}

	private static final class ResponseCollector extends SimpleChannelInboundHandler<HttpObject> {
		final CompletableFuture<String> done = new CompletableFuture<>();
		private final StringBuilder body = new StringBuilder();

		@Override
		protected void channelRead0(@NotNull ChannelHandlerContext ctx, @NotNull HttpObject msg) {
			if (msg instanceof HttpResponse response)
				Assertions.assertEquals(HttpResponseStatus.OK, response.status(), "h2响应状态");
			if (msg instanceof HttpContent content) {
				body.append(content.content().toString(StandardCharsets.UTF_8));
				if (msg instanceof LastHttpContent)
					done.complete(body.toString());
			}
		}

		@Override
		public void exceptionCaught(@NotNull ChannelHandlerContext ctx, @NotNull Throwable cause) {
			done.completeExceptionally(cause);
		}
	}
}
