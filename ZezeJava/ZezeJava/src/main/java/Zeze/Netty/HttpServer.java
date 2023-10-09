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
import Zeze.Util.ConcurrentHashSet;
import Zeze.Util.FewModifyMap;
import Zeze.Util.TaskOneByOneByKey;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
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
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Sharable
public class HttpServer extends ChannelInitializer<SocketChannel> implements Closeable {
	protected static final AttributeKey<Integer> idleTimeKey = AttributeKey.valueOf("ZezeIdleTime");
	protected static final AttributeKey<Integer> outBufHashKey = AttributeKey.valueOf("ZezeOutBufHash"); // 用于判断输出buffer是否有变化
	protected final @Nullable Application zeze; // 只用于通过事务处理HTTP请求
	protected final @Nullable String fileHome; // 客户端可下载的文件根目录
	protected final int fileCacheSeconds; // 通知客户端文件下载的缓存时间(秒)
	protected final FewModifyMap<String, HttpHandler> handlers = new FewModifyMap<>();
	protected final ConcurrentHashSet<Channel> channels = new ConcurrentHashSet<>();
	protected final ConcurrentHashMap<ChannelId, HttpExchange> exchanges = new ConcurrentHashMap<>();
	protected final TaskOneByOneByKey task11Executor = new TaskOneByOneByKey();
	protected int writePendingLimit = 64 * 1024; // 写缓冲区的限制大小(字节),超过会立即断开连接,写大量内容需要考虑分片
	protected int checkIdleInterval = 5; // 检查超时的间隔(秒),只有以下两个超时时间都满足才会触发超时关闭,start之后修改无效
	protected int readIdleTimeout = 30; // 服务端无接收的超时时间(秒)
	protected int writeIdleTimeout = 60; // 服务端无发送的超时时间(秒)
	protected @Nullable SslContext sslCtx;
	protected @Nullable Future<?> scheduler;
	protected @Nullable ChannelFuture channelFuture;

	public HttpServer() {
		this(null, null, 10 * 60);
	}

	public HttpServer(@Nullable String fileHome, int fileCacheSeconds) {
		this(null, fileHome, fileCacheSeconds);
	}

	public HttpServer(@Nullable Application zeze, @Nullable String fileHome, int fileCacheSeconds) {
		this.zeze = zeze;
		this.fileHome = fileHome;
		this.fileCacheSeconds = fileCacheSeconds;
	}

	public int getWritePendingLimit() {
		return writePendingLimit;
	}

	public void setWritePendingLimit(int writePendingLimit) {
		this.writePendingLimit = writePendingLimit;
	}

	public int getCheckIdleInterval() {
		return checkIdleInterval;
	}

	public void setCheckIdleInterval(int checkIdleInterval) {
		this.checkIdleInterval = checkIdleInterval;
	}

	public int getReadIdleTimeout() {
		return readIdleTimeout;
	}

	public void setReadIdleTimeout(int readIdleTimeout) {
		this.readIdleTimeout = readIdleTimeout;
	}

	public int getWriteIdleTimeout() {
		return writeIdleTimeout;
	}

	public void setWriteIdleTimeout(int writeIdleTimeout) {
		this.writeIdleTimeout = writeIdleTimeout;
	}

	public void setSsl(@NotNull PrivateKey priKey, @Nullable String keyPassword,
					   @Nullable X509Certificate... keyCertChain) throws SSLException {
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
			var ch = channelFuture.channel();
			channelFuture = null;
			if (ch != null)
				ch.close();
		}
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
			var x = exchanges.get(channel.id());
			if (x != null)
				x.close(HttpExchange.CLOSE_TIMEOUT, null);
			else
				channel.close();
		}
	}

	public void addHandler(@NotNull String path, int maxContentLength, @Nullable TransactionLevel level,
						   @Nullable DispatchMode mode, @NotNull HttpEndStreamHandle fullHandle) {
		addHandler(path, new HttpHandler(maxContentLength, level, mode, fullHandle));
	}

	public void addHandler(@NotNull String path, @Nullable TransactionLevel level, @Nullable DispatchMode mode,
						   @NotNull HttpBeginStreamHandle beginStream, @Nullable HttpStreamContentHandle streamContent,
						   @NotNull HttpEndStreamHandle endStream) {
		addHandler(path, new HttpHandler(level, mode, beginStream, streamContent, endStream));
	}

	public void addHandler(@NotNull String path, @Nullable TransactionLevel level, @Nullable DispatchMode mode,
						   @NotNull HttpWebSocketHandle webSocketHandle) {
		addHandler(path, new HttpHandler(level, mode, webSocketHandle));
	}

	public void addHandler(@NotNull String path, @NotNull HttpHandler handler) {
		if (handlers.putIfAbsent(path, handler) != null)
			throw new IllegalStateException("add handler: duplicate path=" + path);
	}

	public void removeHandler(@NotNull String path) {
		handlers.remove(path);
	}

	// 允许扩展HttpExchange类
	public @NotNull HttpExchange createHttpExchange(@NotNull ChannelHandlerContext context) {
		return new HttpExchange(this, context);
	}

	@SuppressWarnings("RedundantThrows")
	@Override
	protected void initChannel(@NotNull SocketChannel ch) throws Exception {
		if (ch.pipeline().get(HttpResponseEncoder.class) != null)
			return;
		Netty.logger.info("accept: {}", ch.remoteAddress());
		var p = ch.pipeline();
		if (sslCtx != null)
			p.addLast(sslCtx.newHandler(ch.alloc()));
		p.addLast(new HttpResponseEncoder());
		p.addLast(new HttpRequestDecoder(4096, 8192, 8192, false));
		p.addLast(this);
		ch.config().setWriteBufferHighWaterMark(writePendingLimit);
		channels.add(ch);
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
	public void userEventTriggered(@NotNull ChannelHandlerContext ctx, Object evt) throws Exception {
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
			var x = exchanges.get(ctx.channel().id());
			if (x != null)
				x.send500(cause); // 需要可配置，或者根据Debug|Release选择。
		} finally {
			ctx.flush().close();
		}
	}
}
