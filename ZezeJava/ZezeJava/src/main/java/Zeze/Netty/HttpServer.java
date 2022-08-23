package Zeze.Netty;

import java.io.Closeable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.FewModifyMap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.ChannelInputShutdownEvent;
import io.netty.channel.socket.ChannelInputShutdownReadComplete;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.util.ReferenceCountUtil;

public class HttpServer extends ChannelInitializer<SocketChannel> implements Closeable {
	private static final int WRITE_PENDING_LIMIT = 64 * 1024; // 写缓冲区的限制大小,超过会立即断开连接,写大量内容需要考虑分片

	final Zeze.Application zeze;
	final String fileHome;
	final int fileCacheSeconds;
	final FewModifyMap<String, HttpHandler> handlers = new FewModifyMap<>();
	final ConcurrentHashMap<ChannelHandlerContext, HttpExchange> exchanges = new ConcurrentHashMap<>();
	private Future<?> scheduler;

	public HttpServer() {
		this(null, null, 10 * 60);
	}

	public HttpServer(Zeze.Application zeze, String fileHome, int fileCacheSeconds) {
		this.zeze = zeze;
		this.fileHome = fileHome;
		this.fileCacheSeconds = fileCacheSeconds;
	}

	public synchronized ChannelFuture start(Netty netty, int port) {
		if (scheduler != null)
			throw new IllegalStateException("already started");
		scheduler = netty.getEventLoopGroup().scheduleWithFixedDelay(
				() -> exchanges.values().forEach(HttpExchange::checkTimeout),
				HttpExchange.CHECK_IDLE_INTERVAL, HttpExchange.CHECK_IDLE_INTERVAL, TimeUnit.SECONDS);
		return netty.startServer(this, port);
	}

	@Override
	public synchronized void close() {
		if (scheduler == null)
			return;
		scheduler.cancel(true);
		scheduler = null;
		exchanges.values().forEach(HttpExchange::closeConnectionNow);
	}

	public void addHandler(String path, int maxContentLength, TransactionLevel level, DispatchMode mode,
						   HttpEndStreamHandle fullHandle) {
		addHandler(path, new HttpHandler(maxContentLength, level, mode, fullHandle));
	}

	public void addHandler(String path, int maxContentLength, TransactionLevel level, DispatchMode mode,
						   HttpBeginStreamHandle beginStream, HttpStreamContentHandle streamContent,
						   HttpEndStreamHandle endStream) {
		addHandler(path, new HttpHandler(maxContentLength, level, mode, beginStream, streamContent, endStream));
	}

	public void addHandler(String path, HttpHandler handler) {
		if (null != handlers.putIfAbsent(path, handler))
			throw new RuntimeException("add handler: duplicate path=" + path);
	}

	@Override
	protected void initChannel(SocketChannel ch) {
		Netty.logger.info("accept {}", ch.remoteAddress());
		ch.pipeline()
				.addLast("encoder", new HttpResponseEncoder())
				.addLast("decoder", new HttpRequestDecoder(4096, 8192, 8192, false))
				.addLast("handler", new Handler());
		ch.config().setWriteBufferHighWaterMark(WRITE_PENDING_LIMIT);
	}

	public class Handler extends ChannelInboundHandlerAdapter {
		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) {
			try {
				exchanges.computeIfAbsent(ctx, c -> new HttpExchange(HttpServer.this, c)).channelRead(msg);
			} catch (RuntimeException r) {
				throw r;
			} catch (Exception e) {
				throw new RuntimeException(e);
			} finally {
				ReferenceCountUtil.release(msg);
			}
		}

		@Override
		public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
			if (evt == ChannelInputShutdownEvent.INSTANCE) {
				var he = exchanges.get(ctx);
				if (he != null)
					he.close(HttpExchange.CLOSE_PASSIVE, null);
				else if (!ctx.channel().closeFuture().isDone()) {
					Netty.logger.info("disconnect: {}", ctx.channel().remoteAddress());
					ctx.close();
				}
			} else if (evt == ChannelInputShutdownReadComplete.INSTANCE && !ctx.channel().closeFuture().isDone()) {
				Netty.logger.info("inputClose: {}", ctx.channel().remoteAddress());
				var he = exchanges.get(ctx);
				if (he != null)
					he.channelReadClosed();
				else
					ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
			}
		}

		@Override
		public void channelWritabilityChanged(ChannelHandlerContext ctx) {
			var channel = ctx.channel();
			Netty.logger.error("write buffer overflow {} > {} from {}",
					channel.unsafe().outboundBuffer().totalPendingWriteBytes(),
					channel.config().getWriteBufferHighWaterMark(), channel.remoteAddress());
			ctx.flush().close();
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
			try {
				Netty.logger.error("exceptionCaught from {}", ctx.channel().remoteAddress(), cause);
				var x = exchanges.get(ctx);
				if (x != null)
					x.send500(cause); // 需要可配置，或者根据Debug|Release选择。
			} finally {
				ctx.flush().close();
			}
		}
	}
}
