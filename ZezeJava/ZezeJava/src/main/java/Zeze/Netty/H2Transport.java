package Zeze.Netty;

import java.nio.charset.StandardCharsets;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http2.Http2FrameCodec;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.handler.codec.http2.Http2StreamFrameToHttpObjectCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.jetbrains.annotations.NotNull;

/**
 * HTTP/2传输栈工厂（明文h2c prior-knowledge/Upgrade与ALPN共用）。
 *
 * <p>架构：协议翻译全部下沉到管线层，交换层（HttpServer/HttpExchange）零改动复用——
 * {@code Http2FrameCodec}+{@code Http2MultiplexHandler}把每个stream呈现为独立子channel，
 * 子channel上{@link Http2StreamFrameToHttpObjectCodec}双向翻译：入站Http2HeadersFrame/DataFrame
 * →HttpRequest/HttpContent（现有channelRead状态机原样消费），出站HttpResponse/HttpContent
 * →:status头帧/DATA帧。每stream恒单在途请求：HttpExchange的响应序化器/出站tripwire在子channel上
 * 天然no-op（{@code responseOrderKey}无登记→{@code seq==null}直写分支）；h1专属语义
 * （Connection头/100-continue/WebSocket升级→501）由{@code HttpExchange.isH2()}分支跳过。
 *
 * <p>子channel装配顺序（头→尾）：帧翻译codec → {@link ChunkedWriteHandler} → HttpServer。
 * ChunkedWriteHandler供HttpFileService的h2分块路径消费HttpChunkedInput（EventLoop上非阻塞、
 * 按可写性背压）：出站从HttpServer流向头侧，先被ChunkedWriteHandler展开为逐个HttpContent，
 * 再经帧codec转DATA帧。
 */
public final class H2Transport {
	private H2Transport() {
	}

	// 显式保守默认（无配置开关=默认值即安全姿态，对齐h1侧口径）：
	// maxConcurrentStreams对齐h1的MaxResponseOrderDepth=128（深度pipelining界）；
	// 帧与头列表上限防单stream/单连接内存驻留。
	public static final long MaxConcurrentStreams = 128;
	public static final int MaxFrameSize = 1 << 16; // 64KB
	public static final long MaxHeaderListSize = 1 << 20; // 1MB

	// 连接级h2栈：帧codec+多路复用（每stream子channel走createStreamInitializer装配）
	public static @NotNull ChannelHandler createMultiplex(@NotNull HttpServer server) {
		return new Http2MultiplexHandler(createStreamInitializer(server));
	}

	public static @NotNull Http2FrameCodec createFrameCodec() {
		var settings = new Http2Settings()
				.maxConcurrentStreams(MaxConcurrentStreams)
				.maxFrameSize(MaxFrameSize)
				.maxHeaderListSize(MaxHeaderListSize);
		return Http2FrameCodecBuilder.forServer()
				.initialSettings(settings)
				.build();
	}

	// 每stream子channel装配：帧翻译→ChunkedWriteHandler→HttpServer(@Sharable复用)→h2标志。
	// h2StreamKey置位后HttpExchange.isH2()为真，驱动各h1专属分支跳过。
	private static @NotNull ChannelHandler createStreamInitializer(@NotNull HttpServer server) {
		return new ChannelInitializer<Http2StreamChannel>() {
			@Override
			protected void initChannel(@NotNull Http2StreamChannel ch) {
				ch.pipeline().addLast(new Http2StreamFrameToHttpObjectCodec(true));
				ch.pipeline().addLast(new ChunkedWriteHandler());
				ch.pipeline().addLast(server);
				ch.attr(HttpExchange.h2StreamKey).set(Boolean.TRUE);
			}
		};
	}

	/**
	 * 明文h2c prior-knowledge探测器（管线首位）："PRI * HTTP/2.0"是RFC专门保留给h2预言的
	 * method，任何h1请求不可能以它开头——首3字节即无歧义判定。h2：换栈（帧codec+多路复用；
	 * h1件与HttpServer本身移除——HttpServer由子channel的stream初始化器重新装配）；
	 * h1：自移除透传（缓冲字节经ByteToMessageDecoder移除时的重放机制喂给后续h1解码器）。
	 *
	 * <p>范围：prior-knowledge（curl --http2-prior-knowledge/定制客户端）；h2c Upgrade（101
	 * 升级舞步）与TLS+ALPN协商为后续扩展（CleartextHttp2ServerUpgradeHandler要求HttpServerCodec
	 * 形态的源编解码器，与本服务的自定义encoder/decoder管线不合，需要适配层）。
	 */
	static final class PrefaceDetector extends ByteToMessageDecoder {
		private final @NotNull HttpServer server;
		private boolean resolved;

		// 完整24字节魔数：裸"PRI"前缀会误判自定义PRI前缀方法（RFC 9110扩展方法token）；
		// 字节不足继续累积，不匹配自移除回退h1。
		private static final byte[] H2_PREFACE = "PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n".getBytes(StandardCharsets.US_ASCII);

		PrefaceDetector(@NotNull HttpServer server) {
			this.server = server;
		}

		@Override
		protected void decode(@NotNull ChannelHandlerContext ctx, @NotNull ByteBuf in,
							  @NotNull List<Object> out) {
			if (resolved || in.readableBytes() < H2_PREFACE.length)
				return; // 字节不足判定，继续累积；已决议则等待移除（decodeLast重放兜底）
			var pipeline = ctx.pipeline();
			resolved = true;
			var readerIndex = in.readerIndex();
			var matched = true;
			for (int i = 0; i < H2_PREFACE.length; i++) {
				if (in.getByte(readerIndex + i) != H2_PREFACE[i]) {
					matched = false;
					break;
				}
			}
			if (matched) {
				// 顺序关键：必须先拆h1件再装h2栈——frameCodec的handlerAdded会同步写出服务器
				// SETTINGS（附CLOSE_ON_FAILURE），若写路径上还残留h1件（HttpResponseEncoder等），
				// 该原始ByteBuf写会同步失败，CLOSE_ON_FAILURE级联关闭整条连接（prior-knowledge
				// 握手必死，复现器实证）。先拆后装后写路径干净[marker→head]，握手正常。
				pipeline.remove(HttpResponseEncoder.class); // 匿名子类按类型匹配
				pipeline.remove(HttpRequestDecoder.class);
				pipeline.remove(server);
				// HttpServer移出父管线后channelInactive不可达：挂closeFuture从channels收口。
				ctx.channel().closeFuture().addListener(f -> server.channels.remove(ctx.channel()));
				pipeline.addLast(new H2ReadActivityMarker());
				pipeline.addLast(createFrameCodec());
				pipeline.addLast(new Http2MultiplexHandler(createStreamInitializer(server)));
			}
			pipeline.remove(this);
		}
	}

	// h2父连接的读活跃清零：h1下由HttpExchange.channelRead清idleTimeKey，h2换栈后父管线无
	// HttpServer——上载方向活跃（服务端只收不发）会累计idle被checkTimeout误判CLOSE_TIMEOUT。
	// 写方向的活跃检测不经此（checkTimeout0直读channel.unsafe().outboundBuffer的进度哈希，
	// 帧写出同样改变它，天然覆盖h2）。
	private static final class H2ReadActivityMarker extends ChannelInboundHandlerAdapter {
		@Override
		public void channelRead(@NotNull ChannelHandlerContext ctx, @NotNull Object msg) throws Exception {
			ctx.channel().attr(HttpServer.idleTimeKey).set(null);
			super.channelRead(ctx, msg);
		}
	}
}
