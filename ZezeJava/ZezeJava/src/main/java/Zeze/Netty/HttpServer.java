package Zeze.Netty;

import java.io.Closeable;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLException;
import Zeze.Application;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.FewModifyMap;
import Zeze.Util.TaskOneByOneByKey;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.ChannelInputShutdownEvent;
import io.netty.channel.socket.ChannelInputShutdownReadComplete;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.ReferenceCountUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Sharable
public class HttpServer extends ChannelInitializer<SocketChannel> implements Closeable {
	private static final int WRITE_PENDING_LIMIT = 64 * 1024; // 写缓冲区的限制大小,超过会立即断开连接,写大量内容需要考虑分片

	final Application zeze;
	final String fileHome;
	final int fileCacheSeconds;
	final FewModifyMap<String, HttpHandler> handlers = new FewModifyMap<>();
	final ConcurrentHashMap<ChannelId, HttpExchange> exchanges = new ConcurrentHashMap<>();
	final TaskOneByOneByKey task11Executor = new TaskOneByOneByKey();
	private SslContext sslCtx;
	private Future<?> scheduler;
	private ChannelFuture channelFuture;

	public HttpServer() {
		this(null, null, 10 * 60);
	}

	public HttpServer(Application zeze, String fileHome, int fileCacheSeconds) {
		this.zeze = zeze;
		this.fileHome = fileHome;
		this.fileCacheSeconds = fileCacheSeconds;
	}

	public void setSsl(@NotNull PrivateKey priKey, @Nullable String keyPassword,
					   @Nullable X509Certificate... keyCertChain) throws SSLException {
		sslCtx = SslContextBuilder.forServer(priKey, keyPassword, keyCertChain).build();
	}

	public synchronized ChannelFuture start(Netty netty, int port) {
		if (scheduler != null)
			throw new IllegalStateException("already started");
		scheduler = netty.getEventLoopGroup().scheduleWithFixedDelay(
				() -> exchanges.values().forEach(HttpExchange::checkTimeout),
				HttpExchange.CHECK_IDLE_INTERVAL, HttpExchange.CHECK_IDLE_INTERVAL, TimeUnit.SECONDS);
		return channelFuture = netty.startServer(this, port);
	}

	@Override
	public synchronized void close() {
		task11Executor.shutdown(true);
		if (scheduler == null)
			return;
		Netty.logger.info("close {}", getClass().getName());
		scheduler.cancel(true);
		scheduler = null;
		exchanges.values().forEach(HttpExchange::closeConnectionNow);
		exchanges.clear();
		if (channelFuture != null) {
			var channel = channelFuture.channel();
			channelFuture = null;
			if (channel != null)
				channel.close();
		}
	}

	public void addHandler(String path, int maxContentLength, TransactionLevel level, DispatchMode mode,
						   HttpEndStreamHandle fullHandle) {
		addHandler(path, new HttpHandler(maxContentLength, level, mode, fullHandle));
	}

	public void addHandler(String path, TransactionLevel level, DispatchMode mode, HttpBeginStreamHandle beginStream,
						   HttpStreamContentHandle streamContent, HttpEndStreamHandle endStream) {
		addHandler(path, new HttpHandler(level, mode, beginStream, streamContent, endStream));
	}

	public void addHandler(String path, TransactionLevel level, DispatchMode mode, HttpWebSocketHandle webSocketHandle) {
		addHandler(path, new HttpHandler(level, mode, webSocketHandle));
	}

	public void addHandler(String path, HttpHandler handler) {
		if (handlers.putIfAbsent(path, handler) != null)
			throw new IllegalStateException("add handler: duplicate path=" + path);
	}

	// 允许扩展HttpExchange类
	public HttpExchange createHttpExchange(ChannelHandlerContext context) {
		return new HttpExchange(this, context);
	}

	@SuppressWarnings("RedundantThrows")
	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		if (ch.pipeline().get(HttpResponseEncoder.class) != null)
			return;
		Netty.logger.info("accept {}", ch.remoteAddress());
		var p = ch.pipeline();
		if (sslCtx != null)
			p.addLast(sslCtx.newHandler(ch.alloc()));
		p.addLast(new HttpResponseEncoder());
		p.addLast(new HttpRequestDecoder(4096, 8192, 8192, false));
		p.addLast(this);
		ch.config().setWriteBufferHighWaterMark(WRITE_PENDING_LIMIT);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		try {
			var channelId = ctx.channel().id();
			HttpExchange x;
			if (msg instanceof HttpRequest)
				exchanges.put(channelId, x = createHttpExchange(ctx));
			else if ((x = exchanges.get(channelId)) == null)
				return;
			x.channelRead(msg);
		} finally {
			ReferenceCountUtil.release(msg);
		}
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt == ChannelInputShutdownEvent.INSTANCE) {
			var x = exchanges.get(ctx.channel().id());
			if (x != null)
				x.close(HttpExchange.CLOSE_PASSIVE, null);
			else if (!ctx.channel().closeFuture().isDone()) {
				Netty.logger.info("disconnect: {}", ctx.channel().remoteAddress());
				ctx.close();
			}
		} else if (evt == ChannelInputShutdownReadComplete.INSTANCE && !ctx.channel().closeFuture().isDone()) {
			Netty.logger.info("inputClose: {}", ctx.channel().remoteAddress());
			var x = exchanges.get(ctx.channel().id());
			if (x != null)
				x.channelReadClosed();
			else
				ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
		}
		super.userEventTriggered(ctx, evt);
	}

	@Override
	public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
		var channel = ctx.channel();
		Netty.logger.error("write buffer overflow {} > {} from {}",
				channel.unsafe().outboundBuffer().totalPendingWriteBytes(),
				channel.config().getWriteBufferHighWaterMark(), channel.remoteAddress());
		ctx.flush().close();
		super.channelWritabilityChanged(ctx);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		try {
			Netty.logger.error("exceptionCaught from {}", ctx.channel().remoteAddress(), cause);
			var x = exchanges.get(ctx.channel().id());
			if (x != null)
				x.send500(cause); // 需要可配置，或者根据Debug|Release选择。
		} finally {
			ctx.flush().close();
		}
	}
}
