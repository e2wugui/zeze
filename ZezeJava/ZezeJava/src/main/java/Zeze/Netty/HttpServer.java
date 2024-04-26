package Zeze.Netty;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import javax.net.ssl.SSLException;
import Zeze.Application;
import Zeze.Net.Helper;
import Zeze.Services.ServiceManager.AbstractAgent;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.ConcurrentHashSet;
import Zeze.Util.FewModifyMap;
import Zeze.Util.GlobalTimer;
import Zeze.Util.PropertiesHelper;
import Zeze.Util.Reflect;
import Zeze.Util.TaskOneByOneByKey;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.ChannelInputShutdownEvent;
import io.netty.channel.socket.ChannelInputShutdownReadComplete;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.unix.Errors;
import io.netty.handler.codec.http.HttpDecoderConfig;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("VulnerableCodeUsages")
@Sharable
public class HttpServer extends ChannelInitializer<SocketChannel> implements Closeable {
	protected static final int sendStackTrace = PropertiesHelper.getInt("HttpServer.sendStackTrace", 1);
	protected static final AttributeKey<Integer> idleTimeKey = AttributeKey.valueOf("ZezeIdleTime");
	protected static final AttributeKey<Integer> outBufHashKey = AttributeKey.valueOf("ZezeOutBufHash"); // 用于判断输出buffer是否有变化
	protected static final @NotNull ZoneId zoneId = ZoneId.of("GMT");
	protected static final HttpDecoderConfig decCfg = new HttpDecoderConfig()
			.setMaxInitialLineLength(4096)
			.setMaxHeaderSize(8192)
			.setMaxChunkSize(8192)
			.setChunkedSupported(true)
			.setValidateHeaders(false);
	protected static long lastSecond;
	protected static String lastDateStr;
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
	protected ChannelFuture channelFuture;
	protected final ReentrantLock thisLock = new ReentrantLock();
	protected @Nullable HttpSession httpSession;

	public static @NotNull String getDate() {
		var second = GlobalTimer.getCurrentMillis() / 1000;
		if (second == lastSecond)
			return lastDateStr;
		var dateStr = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.of(
				LocalDateTime.ofEpochSecond(second, 0, ZoneOffset.UTC), zoneId));
		lastDateStr = dateStr;
		lastSecond = second;
		return dateStr;
	}

	public static @NotNull String getDate(long epochSecond) {
		return DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.of(
				LocalDateTime.ofEpochSecond(epochSecond, 0, ZoneOffset.UTC), zoneId));
	}

	public static long parseDate(@NotNull String dateStr) {
		return LocalDateTime.parse(dateStr, DateTimeFormatter.RFC_1123_DATE_TIME).toEpochSecond(ZoneOffset.UTC);
	}

	public static long getLastDateSecond() {
		return lastSecond;
	}

	public static @NotNull HttpHeaders setDate(@NotNull HttpHeaders headers) {
		headers.set(HttpHeaderNames.DATE, getDate());
		return headers;
	}

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

	public void lock() {
		thisLock.lock();
	}

	public void unlock() {
		thisLock.unlock();
	}

	// before start
	public void enableHttpSession() {
		if (zeze == null)
			throw new IllegalStateException("zeze is null.");
		httpSession = new HttpSession(zeze);
	}

	public @Nullable HttpSession getHttpSession() {
		return httpSession;
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

	public @NotNull ChannelFuture start(@NotNull Netty netty, int port) throws Exception {
		return start(netty, null, port);
	}

	public @NotNull ChannelFuture start(@NotNull Netty netty, @Nullable String host, int port) throws Exception {
		lock();
		if (httpSession != null)
			httpSession.start();

		try {
			if (scheduler != null)
				throw new IllegalStateException("already started");
			scheduler = netty.getEventLoopGroup().scheduleWithFixedDelay(
					() -> channels.keySet().forEach(this::checkTimeout),
					checkIdleInterval, checkIdleInterval, TimeUnit.SECONDS);
			return channelFuture = netty.startServer(this, host, port);
		} finally {
			unlock();
		}
	}

	public ChannelFuture getChannelFuture() {
		return channelFuture;
	}

	/**
	 * 需要端口已在监听状态才能获取到, 可能会同步等待监听的启动
	 *
	 * @return 无法获取时返回null
	 */
	public @Nullable InetSocketAddress getLocalAddress() {
		var cf = channelFuture;
		if (cf == null)
			return null;
		try {
			cf.sync();
		} catch (InterruptedException e) {
			return null;
		}
		var addr = cf.channel().localAddress();
		return addr instanceof InetSocketAddress ? (InetSocketAddress)addr : null;
	}

	/**
	 * 获取实际监听的IP地址, 其他机器可以通过这个连接过来. 可能会同步等待监听的启动
	 *
	 * @throws IllegalStateException 无法获取时会抛出
	 */
	public @NotNull String getExportIp() {
		var addr = getLocalAddress();
		if (addr == null)
			throw new IllegalStateException();
		return addr.getAddress().isAnyLocalAddress()
				? Helper.selectOneIpAddress(false)
				: addr.getAddress().getHostAddress();
	}

	/**
	 * 获取实际监听的端口. 可能会同步等待监听的启动
	 *
	 * @throws IllegalStateException 无法获取时会抛出
	 */
	public int getPort() {
		var addr = getLocalAddress();
		if (addr == null)
			throw new IllegalStateException();
		return addr.getPort();
	}

	public void publishService(String serviceName) throws InterruptedException {
		if (null == zeze)
			throw new IllegalStateException("without zeze env. use another publishService method with your special agent");
		publishService(serviceName, 0, zeze.getServiceManager());
	}

	/**
	 * 发布HttpServer到指定agent。
	 *
	 * @param serviceName 服务名
	 * @param version     服务版本
	 */
	public void publishService(@NotNull String serviceName, long version, @NotNull AbstractAgent agent)
			throws InterruptedException {
		var ip = getExportIp();
		int port = getPort();
		agent.registerService(new BServiceInfo(serviceName, "@" + ip + ":" + port, version, ip, port));
	}

	@Override
	public void close() {
		lock();
		try {
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
			if (httpSession != null)
				httpSession.stop();
		} finally {
			unlock();
		}
	}

	// 这是一个低开销的检测空闲超时的方法,不准确但只会比预设的超时时间长,写超时可能会多出readIdleTimeout的时长
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
			var msg = outBuf.current();
			var hash = msg != null ? System.identityHashCode(msg) ^ Long.hashCode(outBuf.currentProgress()) : null;
			var outBufHashAttr = channel.attr(outBufHashKey);
			if (!Objects.equals(outBufHashAttr.get(), hash)) {
				outBufHashAttr.set(hash);
				idleTimeAttr.set(0);
				return;
			}
		}
		idleTimeAttr.set(idleTime);
		// 读写都超时了,那就主动关闭吧
		if (idleTime >= writeIdleTimeout && !Reflect.inDebugMode) {
			var x = exchanges.get(channel.id());
			if (x != null)
				x.close(HttpExchange.CLOSE_TIMEOUT, null);
			else
				channel.close();
		}
	}

	protected static void onBeforeWrite(@NotNull Channel channel) {
		var outBufHashAttr = channel.attr(outBufHashKey);
		if (outBufHashAttr.get() == null)
			outBufHashAttr.set(0);
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
		Netty.logger.debug("addHandler: {}", path);
	}

	public void removeHandler(@NotNull String path) {
		if (handlers.remove(path) != null)
			Netty.logger.debug("removeHandler: {}", path);
	}

	public @Nullable HttpHandler getHandler(@NotNull String path) {
		return handlers.get(path);
	}

	// 允许扩展HttpExchange类,返回null表示忽略处理(通常要回复状态并关闭连接). 使用恰当策略提前忽略可以避免同时接收太多请求数据导致OOM
	public @Nullable HttpExchange createHttpExchange(@NotNull ChannelHandlerContext context) {
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
		p.addLast(new HttpResponseEncoder() {
			@Override
			public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
				onBeforeWrite(ctx.channel());
				super.write(ctx, msg, promise);
			}
		});
		p.addLast(new HttpRequestDecoder(decCfg));
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
	public void channelRead(@NotNull ChannelHandlerContext ctx, @Nullable Object msg) throws Exception {
		try {
			var channelId = ctx.channel().id();
			HttpExchange x;
			if (msg instanceof HttpRequest) {
				if ((x = createHttpExchange(ctx)) == null)
					return;
				exchanges.put(channelId, x);
			} else if ((x = exchanges.get(channelId)) == null)
				return;
			x.channelRead(msg);
		} finally {
			ReferenceCountUtil.release(msg);
		}
	}

	@Override
	public void userEventTriggered(@NotNull ChannelHandlerContext ctx, @Nullable Object evt) throws Exception {
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
				x.willCloseConnection = true;
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
			var ch = ctx.channel();
			var addr = ch.remoteAddress();
			if (cause instanceof IOException)
				Netty.logger.info("exceptionCaught: {} {}", addr, cause);
			else
				Netty.logger.error("exceptionCaught: {} exception:", addr, cause);
			if (!(cause instanceof Errors.NativeIoException) && !(cause instanceof SocketException)) { // Connection reset by peer
				var x = exchanges.get(ch.id());
				if (x != null && ch.isActive()) {
					if (sendStackTrace > 0)
						x.send500(cause);
					else if (sendStackTrace == 0)
						x.send500(cause.toString());
					else
						x.send500((String)null);
				}
			}
		} finally {
			ctx.flush().close();
		}
	}
}
