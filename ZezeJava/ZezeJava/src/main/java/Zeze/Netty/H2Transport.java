package Zeze.Netty;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
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
}
