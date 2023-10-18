package Zeze.Netty;

import java.io.Closeable;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLException;
import Zeze.Net.Service;
import Zeze.Util.ConcurrentHashSet;
import Zeze.Util.Factory;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.ChannelInputShutdownEvent;
import io.netty.channel.socket.ChannelInputShutdownReadComplete;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import Zeze.Net.Protocol;

/**
 * TlsServer
 * 1. 可以自定义pipeline
 */
public class TlsServer extends ChannelInitializer<SocketChannel> implements Closeable {
	protected static final AttributeKey<Integer> idleTimeKey = AttributeKey.valueOf("ZezeTlsServerIdleTime");
	// 用于判断输出buffer是否有变化
	protected static final AttributeKey<Integer> outBufHashKey = AttributeKey.valueOf("ZezeTlsServerOutBufHash");

	protected @Nullable Future<?> scheduler;
	protected final ConcurrentHashSet<Channel> channels = new ConcurrentHashSet<>();
	protected final SslContext sslCtx;
	protected int writePendingLimit = 64 * 1024; // 写缓冲区的限制大小(字节),超过会立即断开连接,写大量内容需要考虑分片
	protected @Nullable ChannelFuture channelFuture;
	protected int checkIdleInterval = 5; // 检查超时的间隔(秒),只有以下两个超时时间都满足才会触发超时关闭,start之后修改无效
	protected int readIdleTimeout = 30; // 服务端无接收的超时时间(秒)
	protected int writeIdleTimeout = 60; // 服务端无发送的超时时间(秒)
	protected final List<Factory<ChannelHandler>> pipeline; // todo 改成 MethodHandle
	protected final Class<ChannelHandler> lastHandlerClass;

	@SuppressWarnings("unchecked")
	public TlsServer(List<Factory<ChannelHandler>> pipeline,
					 @NotNull PrivateKey priKey, @Nullable String keyPassword,
					 @Nullable X509Certificate... keyCertChain) throws SSLException {
		this.pipeline = pipeline;
		lastHandlerClass = (Class<ChannelHandler>)pipeline.get(pipeline.size() - 1).create().getClass();
		sslCtx = SslContextBuilder.forServer(priKey, keyPassword, keyCertChain).build();
	}

	public synchronized @NotNull ChannelFuture start(@NotNull Netty netty, int port) {
		if (scheduler != null)
			throw new IllegalStateException("already started");
		scheduler = netty.getEventLoopGroup().scheduleWithFixedDelay(
				() -> channels.keySet().forEach(this::checkTimeout),
				checkIdleInterval, checkIdleInterval, TimeUnit.SECONDS);
		return channelFuture = netty.startServer(this, port);
	}

	protected void checkTimeout(@NotNull Channel channel) {
		var idleTimeAttr = channel.attr(idleTimeKey);
		var idleTimeObj = idleTimeAttr.get();
		int idleTime = idleTimeObj != null ? idleTimeObj : 0;
		// 这里为了减小开销, 先只判断读超时
		if ((idleTime += checkIdleInterval) < readIdleTimeout) {
			idleTimeAttr.set(idleTime);
			return;
		}
		// 判断写超时前判断写buffer的状态是否有变化,有变化则重新idle计时
		var outBuf = channel.unsafe().outboundBuffer();
		if (outBuf != null) {
			int hash = System.identityHashCode(outBuf.current()) ^ Long.hashCode(outBuf.currentProgress());
			var outBufHashAttr = channel.attr(outBufHashKey);
			var outBufHash = outBufHashAttr.get();
			if (outBufHash == null || outBufHash != hash) {
				outBufHashAttr.set(hash);
				idleTimeAttr.set(0);
				return;
			}
		}
		idleTimeAttr.set(idleTime);
		// 读写都超时了,那就主动关闭吧
		if (idleTime >= writeIdleTimeout) {
			channel.close();
		}
	}
	@Override
	protected void initChannel(@NotNull SocketChannel ch) throws Exception {
		if (ch.pipeline().get(lastHandlerClass) != null)
			return;
		Netty.logger.info("accept: {}", ch.remoteAddress());
		var p = ch.pipeline();
		if (sslCtx != null)
			p.addLast(sslCtx.newHandler(ch.alloc()));
		for (var line : pipeline)
			p.addLast(line.create());
		ch.config().setWriteBufferHighWaterMark(writePendingLimit);
		channels.add(ch);
	}

	@Override
	public void close() throws IOException {
		if (scheduler == null)
			return;
		Netty.logger.info("close {}", getClass().getName());
		scheduler.cancel(true);
		scheduler = null;
		channels.forEach((ch) -> ch.close());
		channels.clear();
		if (channelFuture != null) {
			var ch = channelFuture.channel();
			channelFuture = null;
			if (ch != null)
				ch.close();
		}
	}

	@Override
	public void channelInactive(@NotNull ChannelHandlerContext ctx) throws Exception {
		var ch = ctx.channel();
		Netty.logger.info("closed: {}", ch.remoteAddress());
		channels.remove(ch);
		super.channelInactive(ctx);
	}

	@Override
	public void channelRead(@NotNull ChannelHandlerContext ctx, Object msg) throws Exception {
		try {
			// todo 这里需要与service有比较强的关系，去掉自定义pipeline？
			var p = (Protocol<?>)msg;
			var service = new Service("null");
			service.dispatchProtocol(p);
		} finally {
			ReferenceCountUtil.release(msg);
		}
	}

	@Override
	public void userEventTriggered(@NotNull ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt == ChannelInputShutdownEvent.INSTANCE) {
			Netty.logger.info("disconnect: {}", ctx.channel().remoteAddress());
			ctx.close();
		} else if (evt == ChannelInputShutdownReadComplete.INSTANCE && !ctx.channel().closeFuture().isDone()) {
			Netty.logger.info("inputClose: {}", ctx.channel().remoteAddress());
			ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
		}
		super.userEventTriggered(ctx, evt);
	}

	@Override
	public void channelWritabilityChanged(@NotNull ChannelHandlerContext ctx) throws Exception {
		var ch = ctx.channel();
		Netty.logger.error("write buffer overflow {} > {} from {}",
				ch.unsafe().outboundBuffer().totalPendingWriteBytes(),
				ch.config().getWriteBufferHighWaterMark(), ch.remoteAddress());
		ctx.flush().close();
		super.channelWritabilityChanged(ctx);
	}

	@Override
	public void exceptionCaught(@NotNull ChannelHandlerContext ctx, @NotNull Throwable cause) {
		try {
			Netty.logger.error("exceptionCaught from {}", ctx.channel().remoteAddress(), cause);
		} finally {
			ctx.flush().close();
		}
	}
}
