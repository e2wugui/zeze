package Zeze.Netty;

import java.nio.charset.StandardCharsets;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPromise;
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

/**
 * HTTP/2 传输栈工厂：在管线层通过 {@link Http2FrameCodec}、{@link Http2MultiplexHandler} 与
 * {@link Http2StreamFrameToHttpObjectCodec} 将每个 stream 适配为独立子 channel，复用 HttpServer/HttpExchange。
 */
public final class H2Transport {
	private H2Transport() {
	}

	public static final long MaxConcurrentStreams = 128;
	public static final int MaxFrameSize = 1 << 16; // 64KB
	public static final long MaxHeaderListSize = 1 << 20; // 1MB

	public static ChannelHandler createMultiplex(HttpServer server) {
		return new Http2MultiplexHandler(createStreamInitializer(server));
	}

	public static Http2FrameCodec createFrameCodec() {
		var settings = new Http2Settings()
				.maxConcurrentStreams(MaxConcurrentStreams)
				.maxFrameSize(MaxFrameSize)
				.maxHeaderListSize(MaxHeaderListSize);
		return Http2FrameCodecBuilder.forServer()
				.initialSettings(settings)
				.build();
	}

	// 子 channel 装配顺序（头→尾）：帧翻译 → ChunkedWriteHandler（供 h2 文件分块） → HttpServer。
	private static ChannelHandler createStreamInitializer(HttpServer server) {
		return new ChannelInitializer<Http2StreamChannel>() {
			@Override
			protected void initChannel(Http2StreamChannel ch) {
				ch.pipeline().addLast(new Http2StreamFrameToHttpObjectCodec(true));
				ch.pipeline().addLast(new ChunkedWriteHandler());
				ch.pipeline().addLast(server);
				ch.attr(HttpExchange.h2StreamKey).set(Boolean.TRUE);
			}
		};
	}

	/**
	 * 明文 h2c prior-knowledge 探测器：匹配 24 字节 HTTP/2 前言时切换为 h2 管线，否则自移除透传给 h1。
	 */
	static final class PrefaceDetector extends ByteToMessageDecoder {
		private final HttpServer server;
		private boolean resolved;

		private static final byte[] H2_PREFACE = "PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n".getBytes(StandardCharsets.US_ASCII);

		PrefaceDetector(HttpServer server) {
			this.server = server;
		}

		@Override
		protected void decode(ChannelHandlerContext ctx, ByteBuf in,
							  List<Object> out) {
			if (resolved)
				return;
			var pipeline = ctx.pipeline();
			var readerIndex = in.readerIndex();
			var readable = Math.min(in.readableBytes(), H2_PREFACE.length);
			// 逐字节前缀比对，不匹配即刻回退 h1，避免短 HTTP 请求被阻留。
			var matched = true;
			for (int i = 0; i < readable; i++) {
				if (in.getByte(readerIndex + i) != H2_PREFACE[i]) {
					matched = false;
					break;
				}
			}
			if (matched && readable < H2_PREFACE.length)
				return;
			resolved = true;
			if (matched) {
				// 必须先拆 h1 编解码器再装 h2 栈，否则 frameCodec.handlerAdded 同步写 SETTINGS 会被 h1 编码器拒绝。
				pipeline.remove(HttpResponseEncoder.class);
				pipeline.remove(HttpRequestDecoder.class);
				pipeline.remove(server);
				// HttpServer 移出父管线后 channelInactive 不再触发，经 closeFuture 清理 channels。
				ctx.channel().closeFuture().addListener(f -> server.channels.remove(ctx.channel()));
				pipeline.addLast(new H2ActivityMarker());
				pipeline.addLast(createFrameCodec());
				pipeline.addLast(new Http2MultiplexHandler(createStreamInitializer(server)));
			}
			pipeline.remove(this);
		}
	}

	// 位于 Http2FrameCodec 之前，截获父连接入站读与出站写帧以维护空闲超时活跃状态。
	private static final class H2ActivityMarker extends ChannelDuplexHandler {
		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			ctx.channel().attr(HttpServer.idleTimeKey).set(null);
			super.channelRead(ctx, msg);
		}

		@Override
		public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
			HttpServer.onBeforeWrite(ctx.channel());
			super.write(ctx, msg, promise);
		}
	}
}
