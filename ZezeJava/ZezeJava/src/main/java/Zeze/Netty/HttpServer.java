package Zeze.Netty;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
import java.util.concurrent.RejectedExecutionException;
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
import Zeze.Util.FewModifySortedMap;
import Zeze.Util.GlobalTimer;
import Zeze.Util.PropertiesHelper;
import Zeze.Util.Reflect;
import Zeze.Util.TaskOneByOneByKey;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPromise;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.IoEventLoopGroup;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.unix.UnixChannelOption;
import io.netty.channel.uring.IoUring;
import io.netty.channel.uring.IoUringIoHandler;
import io.netty.channel.uring.IoUringServerSocketChannel;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.socket.ChannelInputShutdownEvent;
import io.netty.channel.socket.ChannelInputShutdownReadComplete;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.unix.Errors;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpDecoderConfig;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import org.jspecify.annotations.Nullable;

/**
 * 基于 Netty 的 HTTP 服务器（h1/h2c）：handler 注册与查找、请求派发（事务/线程模型）、
 * 连接与在途 exchange 的生命周期管理。
 */
@Sharable
public class HttpServer extends ChannelInboundHandlerAdapter implements Closeable {
	public static final Charset defaultCharset = StandardCharsets.UTF_8;

	protected static final int sendStackTrace = PropertiesHelper.getInt("HttpServer.sendStackTrace", 1);
	protected static final AttributeKey<@Nullable Integer> idleTimeKey = AttributeKey.valueOf("ZezeIdleTime");
	protected static final AttributeKey<@Nullable Integer> outBufHashKey = AttributeKey.valueOf("ZezeOutBufHash"); // 用于判断输出buffer是否有变化
	protected static final ZoneId zoneId = ZoneId.of("GMT");
	protected static final HttpDecoderConfig decCfg = new HttpDecoderConfig()
		.setMaxInitialLineLength(4096)
		.setMaxHeaderSize(8192)
		.setMaxChunkSize(8192)
		.setChunkedSupported(true)
		.setValidateHeaders(false);
	protected static long lastSecond;
	protected static @Nullable String lastDateStr;
	protected final @Nullable Application zeze; // 只用于通过事务处理HTTP请求
	protected final FewModifyMap<String, HttpHandler> handlers = new FewModifyMap<>();
	protected final FewModifySortedMap<String, HttpHandler> prefixHandlers = new FewModifySortedMap<>();
	protected final ConcurrentHashSet<Channel> channels = new ConcurrentHashSet<>();
	protected final ConcurrentHashMap<ChannelId, HttpExchange> exchanges = new ConcurrentHashMap<>();
	protected volatile TaskOneByOneByKey task11Executor = new TaskOneByOneByKey(); // close 时关闭，重启时按需重建
	protected boolean task11ExecutorDown; // 仅在 thisLock 内读写
	protected volatile boolean shutdown; // 停机标志：置位后新请求直接返回 503 并关闭连接
	protected int writePendingLimit = 64 * 1024; // 写缓冲高水位(字节)，越过触发背压日志，持续拥塞由写空闲超时关闭
	protected int maxUploadSize = 256 * 1024 * 1024; // 流模式上传的请求 body 总量限制(字节)，超过返回 413 并断开连接
	protected int checkIdleInterval = 5; // 检查超时的间隔(秒),只有以下两个超时时间都满足才会触发超时关闭,start之后修改无效
	protected int readIdleTimeout = 30; // 服务端无接收的超时时间(秒)
	protected int writeIdleTimeout = 60; // 服务端无发送的超时时间(秒)
	protected @Nullable SslContext sslCtx;
	protected @Nullable Future<?> scheduler;
	protected @Nullable ChannelFuture channelFuture;
	protected final ReentrantLock thisLock = new ReentrantLock();
	protected @Nullable HttpSession httpSession;
	protected final boolean noProcedure;

	// 同步保护 lastSecond 与 lastDateStr 的成对更新
	public static synchronized String getDate() {
		var second = GlobalTimer.getCurrentMillis() / 1000;
		var dateStr = lastDateStr;
		if (second == lastSecond && dateStr != null)
			return dateStr;
		dateStr = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.of(
			LocalDateTime.ofEpochSecond(second, 0, ZoneOffset.UTC), zoneId));
		lastDateStr = dateStr;
		lastSecond = second;
		return dateStr;
	}

	public static String getDate(long epochSecond) {
		return DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.of(
			LocalDateTime.ofEpochSecond(epochSecond, 0, ZoneOffset.UTC), zoneId));
	}

	public static long parseDate(String dateStr) {
		try {
			return LocalDateTime.parse(dateStr, DateTimeFormatter.RFC_1123_DATE_TIME).toEpochSecond(ZoneOffset.UTC);
		} catch (Exception ignored) { // 无法解析的日期按RFC7232忽略，返回-1使调用方比较永不命中
			return -1;
		}
	}

	public static long getLastDateSecond() {
		return lastSecond;
	}

	public static HttpHeaders setDate(HttpHeaders headers) {
		headers.set(HttpHeaderNames.DATE, getDate());
		return headers;
	}

	public HttpServer() {
		this(null);
	}

	public HttpServer(@Nullable Application zeze) {
		this.zeze = zeze;
		noProcedure = zeze == null || zeze.isNoDatabase();
	}

	public void lock() {
		thisLock.lock();
	}

	public void unlock() {
		thisLock.unlock();
	}

	// 建议在 zeze.start() 之前调用；zeze 已启动时走动态建表。
	public void enableHttpSession() {
		if (zeze == null)
			throw new IllegalStateException("zeze is null");
		if (zeze.isNoDatabase())
			throw new IllegalStateException("zeze is noDatabase");
		if (zeze.getTimerOrNull() == null)
			throw new IllegalStateException("zeze.timer is null: http session requires ProviderApp "
				+ "(Application creates Timer only when ProviderApp exists)");
		if (httpSession != null)
			return;
		httpSession = new HttpSession(zeze);
		//noinspection DataFlowIssue
		var dbName = zeze.getConfig().getTableConf(httpSession.tSession().getName()).getDatabaseName();
		if (zeze.isStart())
			zeze.openDynamicTable(dbName, httpSession.tSession());
		else
			httpSession.RegisterZezeTables(zeze);
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

	public int getMaxUploadSize() {
		return maxUploadSize;
	}

	public void setMaxUploadSize(int maxUploadSize) {
		this.maxUploadSize = maxUploadSize;
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

	public void setSsl(PrivateKey priKey, @Nullable String keyPassword,
					   @Nullable X509Certificate... keyCertChain) throws SSLException {
		sslCtx = SslContextBuilder.forServer(priKey, keyPassword, keyCertChain).build();
	}

	/**
	 * 子类需要freemarker时，构造，并且重载这个方法。
	 */
	public @Nullable FreeMarker getFreeMarker() {
		return null;
	}

	public @Nullable Thymeleaf getThymeleaf() {
		return null;
	}

	public ChannelFuture start(Netty netty, int port) throws Exception {
		return start(netty, null, port);
	}

	public ChannelFuture start(Netty netty, @Nullable String host, int port) throws Exception {
		lock();

		try {
			if (httpSession != null)
				httpSession.start();

			if (scheduler != null)
				throw new IllegalStateException("already started");
			if (task11ExecutorDown) {
				task11Executor = new TaskOneByOneByKey();
				task11ExecutorDown = false;
			}
			shutdown = false;
			var eventLoopGroup = netty.getEventLoopGroup();
			var b = new ServerBootstrap();
			// 仅 Epoll/IoUring 传输层支持 SO_REUSEPORT
			if (eventLoopGroup instanceof IoEventLoopGroup ioGroup
					&& (ioGroup.isIoType(EpollIoHandler.class) || ioGroup.isIoType(IoUringIoHandler.class)))
				b = b.option(UnixChannelOption.SO_REUSEPORT, true);
			b = b.group(eventLoopGroup)
				.option(ChannelOption.SO_BACKLOG, 8192)
				.option(ChannelOption.SO_REUSEADDR, true)
				.childOption(ChannelOption.SO_REUSEADDR, true)
				.childOption(ChannelOption.SO_KEEPALIVE, true)
				.childOption(ChannelOption.ALLOW_HALF_CLOSURE, true)
				.channel(IoUring.isAvailable() ? IoUringServerSocketChannel.class
						: Epoll.isAvailable() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
				.childHandler(new ChannelInitializer<SocketChannel>() {
					@Override
					public void initChannel(SocketChannel ch) throws Exception {
						HttpServer.this.initChannel(ch);
					}
				});
			ChannelFuture future;
			if (host != null && !(host = host.trim()).isEmpty())
				future = b.bind(host, port);
			else {
				future = b.bind(port);
				host = "any";
			}
			// 绑定成功后再启动空闲检测，避免 bind 失败残留定时任务
			scheduler = eventLoopGroup.scheduleWithFixedDelay(() -> channels.keySet().forEach(this::checkTimeout),
				checkIdleInterval, checkIdleInterval, TimeUnit.SECONDS);
			channelFuture = future;
			Netty.logger.info("startServer {} on {}:{}", getClass().getName(), host, port);
			return future;
		} finally {
			unlock();
		}
	}

	public @Nullable ChannelFuture getChannelFuture() {
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
			Thread.currentThread().interrupt(); // 恢复中断标志，让调用方能感知
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
	public String getExportIp() {
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

	public void publishService(String serviceName) {
		if (zeze == null)
			throw new IllegalStateException("without zeze env. use another publishService method with your special agent");
		publishService(serviceName, 0, Objects.requireNonNull(zeze.getServiceManager()));
	}

	/**
	 * 发布HttpServer到指定agent。
	 *
	 * @param serviceName 服务名
	 * @param version     服务版本
	 */
	public void publishService(String serviceName, long version, AbstractAgent agent) {
		var ip = getExportIp();
		int port = getPort();
		agent.registerService(new BServiceInfo(serviceName, "@" + ip + ":" + port, version, ip, port));
	}

	@Override
	public void close() {
		lock();
		try {
			// 先置停机标志并关闭监听，阻止新请求与新连接进入
			shutdown = true;
			if (channelFuture != null) {
				var ch = channelFuture.channel();
				channelFuture = null;
				if (ch != null)
					ch.close();
			}
			// 先关闭所有在途 exchange（含 pipelining 与 HTTP/2 子流），再停派发队列以确保收尾任务入队
			channels.keySet().forEach(ch -> HttpExchange.closeInFlightExchanges(ch, HttpExchange.CLOSE_FORCE));
			exchanges.values().forEach(HttpExchange::closeConnectionNow);
			exchanges.clear();
			// 关闭剩余空闲 keep-alive 连接
			channels.keySet().forEach(Channel::close);
			task11Executor.shutdown(true);
			task11ExecutorDown = true;
			if (scheduler == null)
				return;
			Netty.logger.info("close {}", getClass().getName());
			scheduler.cancel(true);
			scheduler = null;
			if (httpSession != null)
				httpSession.stop();
		} finally {
			unlock();
		}
	}

	// 低开销空闲超时检测，实际超时时间可能略长于预设值
	protected void checkTimeout(Channel channel) {
		//noinspection resource
		var eventLoop = channel.eventLoop();
		if (eventLoop.inEventLoop()) {
			checkTimeout0(channel);
			return;
		}
		// 在 channel 所属 EventLoop 执行，避免与 channelRead 清零 idleTime 竞态
		try {
			eventLoop.execute(() -> checkTimeout0(channel));
		} catch (RejectedExecutionException ignored) {
		}
	}

	protected void checkTimeout0(Channel channel) {
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
		// 读写都超时了,主动关闭（janitor关全部在途exchange；无在途时直接关channel）
		if (idleTime >= writeIdleTimeout && !Reflect.inDebugMode) {
			if (!HttpExchange.closeInFlightExchanges(channel, HttpExchange.CLOSE_TIMEOUT))
				channel.close();
		}
	}

	protected static void onBeforeWrite(Channel channel) {
		var outBufHashAttr = channel.attr(outBufHashKey);
		if (outBufHashAttr.get() == null)
			outBufHashAttr.set(0);
	}

	// h2子channel判定（H2Transport的stream初始化器置h2StreamKey；h1连接/父channel恒false）
	static boolean isH2Channel(io.netty.channel.Channel ch) {
		return ch.attr(HttpExchange.h2StreamKey).get() != null;
	}

	public void addHandler(String path, int maxContentLength, @Nullable TransactionLevel level,
						   @Nullable DispatchMode mode, HttpEndStreamHandle fullHandle) {
		addHandler(path, new HttpHandler(maxContentLength, level, mode, fullHandle));
	}

	public void addHandler(String path, @Nullable TransactionLevel level, @Nullable DispatchMode mode,
						   HttpBeginStreamHandle beginStream, @Nullable HttpStreamContentHandle streamContent,
						   HttpEndStreamHandle endStream) {
		addHandler(path, new HttpHandler(level, mode, beginStream, streamContent, endStream));
	}

	/**
	 * 注册流式上传处理器，显式区分正常完成（{@code endStream}）与异常中止（{@code abortStream}）。
	 * 当 {@code abortStream != null} 时，正常完成仅调用 {@code endStream}，流提前中止仅调用 {@code abortStream}。
	 */
	public void addHandler(String path, @Nullable TransactionLevel level, @Nullable DispatchMode mode,
						   HttpBeginStreamHandle beginStream, @Nullable HttpStreamContentHandle streamContent,
						   HttpEndStreamHandle endStream, @Nullable HttpAbortStreamHandle abortStream) {
		addHandler(path, new HttpHandler(level, mode, beginStream, streamContent, endStream, abortStream));
	}

	/**
	 * 注册 multipart/文件上传流式处理器，自动接入 {@link HttpMultipartHandle#onAbortStream(HttpExchange)} 中止回调。
	 */
	public void addHandler(String path, @Nullable TransactionLevel level, @Nullable DispatchMode mode,
						   HttpMultipartHandle multipartHandle) {
		addHandler(path, new HttpHandler(level, mode, multipartHandle));
	}

	public void addHandler(String path, int maxFrameLength, @Nullable TransactionLevel level,
						   @Nullable DispatchMode mode, HttpWebSocketHandle webSocketHandle) {
		addHandler(path, new HttpHandler(maxFrameLength, level, mode, webSocketHandle));
	}

	public void addHandler(String path, @Nullable TransactionLevel level, @Nullable DispatchMode mode,
						   HttpWebSocketHandle webSocketHandle) {
		addHandler(path, new HttpHandler(level, mode, webSocketHandle));
	}

	public void addHandler(String path, HttpHandler handler) {
		if (handlers.putIfAbsent(path, handler) != null)
			throw new IllegalStateException("add handler: duplicate path=" + path);
		Netty.logger.debug("addHandler: {}", path);
	}

	/**
	 * @param pathPrefix 匹配路径前缀,优先匹配最长前缀
	 */
	public void addPrefixHandler(String pathPrefix, HttpHandler handler) {
		if (prefixHandlers.putIfAbsent(pathPrefix, handler) != null)
			throw new IllegalStateException("add handler: duplicate path=" + pathPrefix);
		Netty.logger.debug("addPrefixHandler: {}", pathPrefix);
	}

	public void addFileHandler(String pathPrefix, String fileRootPath) {
		addFileHandler(pathPrefix, fileRootPath, false, 10 * 60);
	}

	public void addFileHandler(String pathPrefix, String fileRootPath, boolean canListPath) {
		addFileHandler(pathPrefix, fileRootPath, canListPath, 10 * 60);
	}

	/**
	 * @param pathPrefix       URL的根路径,开头和结尾应该都是"/"
	 * @param fileRootPath     访问文件的根目录
	 * @param canListPath      是否提供文件目录的访问(展示文件列表)
	 * @param fileCacheSeconds 通知客户端文件下载的缓存时间(秒)
	 */
	public void addFileHandler(String pathPrefix, String fileRootPath, boolean canListPath,
							   int fileCacheSeconds) {
		var pathPrefixLen = pathPrefix.length();
		var rootPath = new File(fileRootPath);
		addPrefixHandler(pathPrefix, new HttpHandler(0, TransactionLevel.None, DispatchMode.Direct, x -> {
			var subPath = x.path();
			int i = pathPrefixLen;
			for (int e = subPath.length(); i < e; i++) {
				var c = subPath.charAt(i);
				if (c != '.' && c != '/' && c != '\\') // 过滤掉前面的特殊符号,避免访问非法路径
					break;
			}
			subPath = subPath.substring(i);
			if (subPath.contains("..") || subPath.indexOf(':') >= 0) // 不能含有".."或":",否则就成为漏洞读取到意外的文件,虽然一般的浏览器在发请求前会过滤掉带..的path
				x.close(x.sendPlainText(HttpResponseStatus.FORBIDDEN, ""));
			else {
				var file = new File(rootPath, subPath);
				if (file.isFile() && !file.isHidden())
					x.sendFile(file, fileCacheSeconds);
				else if (canListPath && file.isDirectory() && !file.isHidden())
					x.sendPath(file);
				else
					x.close(x.send404());
			}
		}));
	}

	public void removeHandler(String path) {
		if (handlers.remove(path) != null)
			Netty.logger.debug("removeHandler: {}", path);
	}

	/** 仅当精确路径仍绑定到指定 handler 时移除注册。 */
	public void removeHandler(String path, HttpHandler handler) {
		if (handlers.remove(path, handler))
			Netty.logger.debug("removeHandler: {}", path);
	}

	public void removePrefixHandler(String path) {
		if (prefixHandlers.remove(path) != null)
			Netty.logger.debug("removePrefixHandler: {}", path);
	}

	public @Nullable HttpHandler getHandler(String path) {
		var handler = handlers.get(path);
		if (handler == null) {
			// 从 floorEntry 向前回退，首个前缀匹配的键即最长匹配前缀
			var e = prefixHandlers.floorEntry(path);
			while (e != null) {
				if (path.startsWith(e.getKey()))
					return e.getValue();
				e = prefixHandlers.lowerEntry(e.getKey());
			}
		}
		return handler;
	}

	/** 扩展点：创建 HttpExchange；返回 null 时由框架回 503 并关闭连接（勿自行写响应）。 */
	public @Nullable HttpExchange createHttpExchange(ChannelHandlerContext context) {
		return new HttpExchange(this, context);
	}

	@SuppressWarnings("RedundantThrows")
	protected void initChannel(SocketChannel ch) throws Exception {
		Netty.logger.info("accept: {}", ch.remoteAddress());
		var p = ch.pipeline();
		if (sslCtx != null)
			p.addLast(sslCtx.newHandler(ch.alloc()));
		// 管线首位探测 h2c prior-knowledge，命中则切 HTTP/2 栈，否则自移除透传
		p.addLast(new H2Transport.PrefaceDetector(this));
		p.addLast(new HttpResponseEncoder() {
			@Override
			public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
				onBeforeWrite(ctx.channel());
				try {
					HttpExchange.checkResponseOrder(ctx.channel(), msg);
				} catch (IllegalStateException e) {
					// 出站同步异常在 Netty 中默认仅 fail promise，须显式释放并走异常关闭
					ReferenceCountUtil.release(msg);
					promise.tryFailure(e);
					ctx.fireExceptionCaught(e);
					return;
				}
				super.write(ctx, msg, promise);
			}
		});
		p.addLast(new HttpRequestDecoder(decCfg));
		if (sslCtx != null)
			p.addLast(new ChunkedWriteHandler());
		p.addLast(this);
		ch.config().setWriteBufferHighWaterMark(writePendingLimit);
		channels.add(ch);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		var ch = ctx.channel();
		Netty.logger.info("closed: {}", ch.remoteAddress());
		channels.remove(ch);
		closeInFlightIncludingH2(ch, HttpExchange.CLOSE_PASSIVE);
		super.channelInactive(ctx);
	}

	// HTTP/2 子流无响应序化器，按 channelId 补关残留 exchange
	private void closeInFlightIncludingH2(Channel ch, int method) {
		if (!HttpExchange.closeInFlightExchanges(ch, method)) {
			var x = exchanges.remove(ch.id());
			if (x != null)
				x.close(method, null);
		}
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, @Nullable Object msg) {
		try {
			var channelId = ctx.channel().id();
			// 拦截解码失败的 HttpObject（Netty 解码失败不抛异常，仅置 DecoderResult）
			if (msg instanceof HttpObject httpObj && httpObj.decoderResult().isFailure()) {
				onDecodeFailure(ctx, httpObj);
				return;
			}
			HttpExchange x;
			if (msg instanceof HttpRequest) {
				if (shutdown) {
					Netty.logger.info("reject request from {} while shutdown", ctx.channel().remoteAddress());
					rejectAndClose(ctx, HttpResponseStatus.SERVICE_UNAVAILABLE);
					return;
				}
				if ((x = createHttpExchange(ctx)) == null) {
					// 拒绝时须关闭连接，避免后续 body 帧串入前一个 pipelined exchange
					Netty.logger.info("reject request from {} by createHttpExchange policy", ctx.channel().remoteAddress());
					rejectAndClose(ctx, HttpResponseStatus.SERVICE_UNAVAILABLE);
					return;
				}
				exchanges.put(channelId, x);
				// HTTP/1 登记请求到达序用于响应序化；HTTP/2 每流单在途无需序化
				if (!isH2Channel(ctx.channel()) && !x.registerResponseOrder())
					return;
			} else if ((x = exchanges.get(channelId)) == null)
				return;
			x.channelRead(msg);
		} finally {
			ReferenceCountUtil.release(msg);
		}
	}

	// 回状态码并关连接；若头部在途响应已开始流式写出则仅关连接，避免响应错位
	private void rejectAndClose(ChannelHandlerContext ctx, HttpResponseStatus status) {
		var ch = ctx.channel();
		var seq = ch.attr(HttpExchange.responseOrderKey).get();
		var head = seq != null ? seq.entries.get(seq.writingOrderId) : null;
		if (head != null && head.started) {
			Netty.logger.warn("suppress {} rejection to {}: response already streaming on connection, close only",
					status, ch.remoteAddress());
			ctx.close();
			return;
		}
		ch.attr(HttpExchange.responseOrderBypassKey).set(Boolean.TRUE);
		var res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled.EMPTY_BUFFER,
			HttpExchange.headersFactory, HttpExchange.trailersFactory);
		res.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
		var cf = ctx.writeAndFlush(res).addListener(ChannelFutureListener.CLOSE);
		var prev = exchanges.remove(ch.id());
		if (prev != null)
			prev.close(HttpExchange.CLOSE_ON_FLUSH, cf);
	}

	protected void onDecodeFailure(ChannelHandlerContext ctx, HttpObject obj) {
		Netty.logger.error("http decode failure from {}: {}", ctx.channel().remoteAddress(), obj.decoderResult().cause());
		rejectAndClose(ctx, HttpResponseStatus.BAD_REQUEST);
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, @Nullable Object evt) throws Exception {
		if (evt == ChannelInputShutdownEvent.INSTANCE) {
			// 客户端半关闭：等待在途响应写完后再关连接
			closeAfterLastResponse(ctx);
		} else if (evt == ChannelInputShutdownReadComplete.INSTANCE && !ctx.channel().closeFuture().isDone()) {
			Netty.logger.info("inputClose: {}", ctx.channel().remoteAddress());
			closeAfterLastResponse(ctx);
		}
		super.userEventTriggered(ctx, evt);
	}

	// 找到序化队列中最后一个在途响应并标记写完后关闭连接
	private void closeAfterLastResponse(ChannelHandlerContext ctx) {
		var ch = ctx.channel();
		HttpExchange last = exchanges.get(ch.id());
		var seq = ch.attr(HttpExchange.responseOrderKey).get();
		if (seq != null) {
			for (var entry : seq.entries.values()) {
				if (last == null || entry.x.responseOrderId > last.responseOrderId)
					last = entry.x;
			}
		}
		if (last != null)
			last.willCloseConnection = true;
		else if (!ch.closeFuture().isDone())
			ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
	}

	@Override
	public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
		var ch = ctx.channel();
		// 越过写缓冲水位仅记背压日志，持续拥塞由写空闲超时关闭
		if (!ch.isWritable())
			// 使用 bytesBeforeWritable() 兼容 TCP 与 HTTP/2 子流
			Netty.logger.info("write buffer saturated ~{} > {} from {}",
					ch.bytesBeforeWritable(),
					ch.config().getWriteBufferHighWaterMark(), ch.remoteAddress());
		super.channelWritabilityChanged(ctx);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
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
			// 先关连接再清理在途 exchange，避免回调异常导致连接未关
			closeInFlightIncludingH2(ctx.channel(), HttpExchange.CLOSE_PASSIVE);
		}
	}
}
