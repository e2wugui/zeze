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
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
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
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Sharable
public class HttpServer extends ChannelInboundHandlerAdapter implements Closeable {
	public static final @NotNull Charset defaultCharset = StandardCharsets.UTF_8;

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
	protected final Application zeze; // 只用于通过事务处理HTTP请求
	protected final FewModifyMap<String, HttpHandler> handlers = new FewModifyMap<>();
	protected final FewModifySortedMap<String, HttpHandler> prefixHandlers = new FewModifySortedMap<>();
	protected final ConcurrentHashSet<Channel> channels = new ConcurrentHashSet<>();
	protected final ConcurrentHashMap<ChannelId, HttpExchange> exchanges = new ConcurrentHashMap<>();
	// close()的shutdown(true)不可逆:之后的submit被队列静默丢弃(不抛不记日志),非Direct请求全部无响应黑洞。
	// start()检测task11ExecutorDown则重建,支持close→start重启。派发线程读取引用,须volatile。
	protected volatile TaskOneByOneByKey task11Executor = new TaskOneByOneByKey();
	protected boolean task11ExecutorDown; // close()置位,start()重建后复位;仅thisLock(start/close)内读写
	// 停机拒绝标志:close()最前置位、start()重启时复位;channelRead在EventLoop线程上读,须volatile。
	// 置位后已accept连接上到达的新HttpRequest回503并关连接(明确拒绝),不再进exchanges/派发handler。
	protected volatile boolean shutdown;
	protected int writePendingLimit = 64 * 1024; // 写缓冲高水位(字节)：越过触发writability事件（背压信号，见channelWritabilityChanged），持续拥塞由写空闲超时兜底
	protected int maxUploadSize = 256 * 1024 * 1024; // 流模式上传(如multipart/raw文件上传)的请求body总量限制(字节),超过返回413并断开连接
	protected int checkIdleInterval = 5; // 检查超时的间隔(秒),只有以下两个超时时间都满足才会触发超时关闭,start之后修改无效
	protected int readIdleTimeout = 30; // 服务端无接收的超时时间(秒)
	protected int writeIdleTimeout = 60; // 服务端无发送的超时时间(秒)
	protected @Nullable SslContext sslCtx;
	protected @Nullable Future<?> scheduler;
	protected ChannelFuture channelFuture;
	protected final ReentrantLock thisLock = new ReentrantLock();
	protected @Nullable HttpSession httpSession;
	protected final boolean noProcedure;

	// 各EventLoop/池线程并发调用：两个静态字段的检查-更新无同步时，读者可观察到
	// 新lastSecond配旧lastDateStr（两写之间无happens-before），返回错位的Date串。
	// 竞争窗口每秒一次、临界区为一次缓存比较，synchronized开销可忽略。
	public static synchronized @NotNull String getDate() {
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
		try {
			return LocalDateTime.parse(dateStr, DateTimeFormatter.RFC_1123_DATE_TIME).toEpochSecond(ZoneOffset.UTC);
		} catch (Exception ignored) { // 无法解析的日期按RFC7232忽略，返回-1使调用方比较永不命中
			return -1;
		}
	}

	public static long getLastDateSecond() {
		return lastSecond;
	}

	public static @NotNull HttpHeaders setDate(@NotNull HttpHeaders headers) {
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
		// FND7-76：timer只在ProviderApp形态创建（Application.initialize要求redirect!=null）。
		// 缺失时不在此fail-fast的话，HttpSession.start的scheduleNamed在null timer上NPE，
		// 被Procedure转成"enableHttpSessionExpiredTimer error=..."错误码，无指向。
		if (zeze.getTimer() == null)
			throw new IllegalStateException("zeze.timer is null: http session requires ProviderApp "
				+ "(Application creates Timer only when ProviderApp exists)");
		if (httpSession != null)
			return;
		httpSession = new HttpSession(zeze);
		// 注册会话表：不注册的话 _tSession 不属于任何Database，表访问必失败。
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

	public void setSsl(@NotNull PrivateKey priKey, @Nullable String keyPassword,
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

	public @NotNull ChannelFuture start(@NotNull Netty netty, int port) throws Exception {
		return start(netty, null, port);
	}

	public @NotNull ChannelFuture start(@NotNull Netty netty, @Nullable String host, int port) throws Exception {
		lock();

		try {
			// 必须在try内（FND4-36）：HttpSession.start 可抛（newProcedure失败抛RuntimeException+
			// ParseException），原位于lock()与try之间——抛异常时unlock永不执行，thisLock被当前线程
			// 永久持有，此后任何线程调close()/start()永久阻塞（同线程因可重入不易察觉）。
			if (httpSession != null)
				httpSession.start();

			if (scheduler != null)
				throw new IllegalStateException("already started");
			if (task11ExecutorDown) {
				// close→start重启:已shutdown的task11Executor无法复活,直接复用会让非Direct请求全部黑洞
				task11Executor = new TaskOneByOneByKey();
				task11ExecutorDown = false;
			}
			shutdown = false; // close→start重启配套:清除停机拒绝标志,重新接受新请求
			var eventLoopGroup = netty.getEventLoopGroup();
			var b = new ServerBootstrap();
			if (eventLoopGroup instanceof EpollEventLoopGroup)
				b = b.option(EpollChannelOption.SO_REUSEPORT, true);
			b = b.group(eventLoopGroup)
				.option(ChannelOption.SO_BACKLOG, 8192)
				.option(ChannelOption.SO_REUSEADDR, true)
				.childOption(ChannelOption.SO_REUSEADDR, true)
				.childOption(ChannelOption.SO_KEEPALIVE, true)
				.childOption(ChannelOption.ALLOW_HALF_CLOSURE, true)
				.channel(Epoll.isAvailable() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
				.childHandler(new ChannelInitializer<SocketChannel>() {
					@Override
					public void initChannel(@NotNull SocketChannel ch) throws Exception {
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
			// scheduler注册在bind之后（FND4-39）：bind同步失败（非法host/端口）时
			// 定时任务不残留——否则再次start抛"already started"进入永久半启动态，
			// 且每checkIdleInterval的checkTimeout对空channels永久空转。
			scheduler = eventLoopGroup.scheduleWithFixedDelay(() -> channels.keySet().forEach(this::checkTimeout),
				checkIdleInterval, checkIdleInterval, TimeUnit.SECONDS);
			channelFuture = future;
			Netty.logger.info("startServer {} on {}:{}", getClass().getName(), host, port);
			return future;
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

	public void publishService(String serviceName) {
		if (zeze == null)
			throw new IllegalStateException("without zeze env. use another publishService method with your special agent");
		publishService(serviceName, 0, zeze.getServiceManager());
	}

	/**
	 * 发布HttpServer到指定agent。
	 *
	 * @param serviceName 服务名
	 * @param version     服务版本
	 */
	public void publishService(@NotNull String serviceName, long version, @NotNull AbstractAgent agent) {
		var ip = getExportIp();
		int port = getPort();
		agent.registerService(new BServiceInfo(serviceName, "@" + ip + ":" + port, version, ip, port));
	}

	@Override
	public void close() {
		lock();
		try {
			// 最先置停机标志（在关监听channel/清扫exchanges之前）：已accept连接上随后到达的
			// 新HttpRequest立即走503拒绝（见channelRead），不再进exchanges/派发handler——
			// 否则这些请求要么提交到已shutdown(true)的派发队列被静默丢弃，要么落入清扫与
			// shutdown之间的竞态黑洞（同FND6-15背景）。
			shutdown = true;
			// 最先关监听channel（必须在下方exchanges清扫与shutdown之前）：shutdown(true)内部
			// waitComplete可阻塞秒级，若监听channel后关，阻塞窗口内新accept的连接加入channels时
			// 清扫已过，残留连接黑洞（同FND6-15）。channelFuture为null（未start绑定）时此块为空操作。
			if (channelFuture != null) {
				var ch = channelFuture.channel();
				channelFuture = null;
				if (ch != null)
					ch.close();
			}
			// 先关exchanges再shutdown派发队列：closeConnectionNow→closeInEventLoop→fireEndStreamHandle/
			// fireWebSocket产生的收尾任务（onEndStream、retain的content/frame的release、multipart decoder
			// destroy、上传临时文件清理）必须在队列置isShutdown之前进入队列，否则提交被丢弃/仅cancel补偿。
			// janitor按channel遍历在途列表（pipelining下被覆盖出exchanges表的前序也要关）。
			channels.keySet().forEach(ch -> HttpExchange.closeInFlightExchanges(ch, HttpExchange.CLOSE_FORCE));
			exchanges.clear();
			// FND6-15：空闲keep-alive连接（请求间隙，exchange已移除）也要关闭——否则停机后
			// 客户端在旧连接发新请求，Normal处理器提交到已shutdown(true)的派发队列被静默丢弃，
			// 请求无响应、连接不断，挂到客户端超时；scheduler取消后idle检测也已停。在途exchange
			// 的连接已由closeConnectionNow关闭，此处对已关连接的close幂等。
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

	// 这是一个低开销的检测空闲超时的方法,不准确但只会比预设的超时时间长,写超时可能会多出readIdleTimeout的时长
	protected void checkTimeout(@NotNull Channel channel) {
		//noinspection resource
		var eventLoop = channel.eventLoop();
		if (eventLoop.inEventLoop()) {
			checkTimeout0(channel);
			return;
		}
		// 检查主体必须在channel自己的EventLoop上执行（FND7-26）：原实现在调度线程上对
		// idleTime做get→+interval→set读改写，channelRead（EventLoop）的清零set(null)落在
		// get与set之间时被写回旧值——静默累计到超时边界的活跃连接（只收不发，如大上传）
		// 被误判空闲而CLOSE_TIMEOUT关闭，违反“超时只长不短”契约。与清零同队列串行后，
		// 检查必然观察到排队在它之前的所有读活动。EL已关停（整个Netty在关闭）时拒绝提交，
		// 此时channel必然已关闭，忽略即可。
		try {
			eventLoop.execute(() -> checkTimeout0(channel));
		} catch (RejectedExecutionException ignored) {
		}
	}

	protected void checkTimeout0(@NotNull Channel channel) {
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
		// 读写都超时了,那就主动关闭吧（janitor关全部在途exchange；无在途时直接关channel）
		if (idleTime >= writeIdleTimeout && !Reflect.inDebugMode) {
			if (!HttpExchange.closeInFlightExchanges(channel, HttpExchange.CLOSE_TIMEOUT))
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
						   @NotNull HttpMultipartHandle multipartHandle) {
		addHandler(path, new HttpHandler(level, mode, multipartHandle, multipartHandle, multipartHandle));
	}

	public void addHandler(@NotNull String path, int maxFrameLength, @Nullable TransactionLevel level,
						   @Nullable DispatchMode mode, @NotNull HttpWebSocketHandle webSocketHandle) {
		addHandler(path, new HttpHandler(maxFrameLength, level, mode, webSocketHandle));
	}

	public void addHandler(@NotNull String path, @Nullable TransactionLevel level, @Nullable DispatchMode mode,
						   @NotNull HttpWebSocketHandle webSocketHandle) {
		addHandler(path, new HttpHandler(64 * 1024, level, mode, webSocketHandle));
	}

	public void addHandler(@NotNull String path, @NotNull HttpHandler handler) {
		if (handlers.putIfAbsent(path, handler) != null)
			throw new IllegalStateException("add handler: duplicate path=" + path);
		Netty.logger.debug("addHandler: {}", path);
	}

	/**
	 * @param pathPrefix 匹配路径前缀,优先匹配最长前缀
	 */
	public void addPrefixHandler(@NotNull String pathPrefix, @NotNull HttpHandler handler) {
		if (prefixHandlers.putIfAbsent(pathPrefix, handler) != null)
			throw new IllegalStateException("add handler: duplicate path=" + pathPrefix);
		Netty.logger.debug("addPrefixHandler: {}", pathPrefix);
	}

	public void addFileHandler(@NotNull String pathPrefix, @NotNull String fileRootPath) {
		addFileHandler(pathPrefix, fileRootPath, false, 10 * 60);
	}

	public void addFileHandler(@NotNull String pathPrefix, @NotNull String fileRootPath, boolean canListPath) {
		addFileHandler(pathPrefix, fileRootPath, canListPath, 10 * 60);
	}

	/**
	 * @param pathPrefix       URL的根路径,开头和结尾应该都是"/"
	 * @param fileRootPath     访问文件的根目录
	 * @param canListPath      是否提供文件目录的访问(展示文件列表)
	 * @param fileCacheSeconds 通知客户端文件下载的缓存时间(秒)
	 */
	public void addFileHandler(@NotNull String pathPrefix, @NotNull String fileRootPath, boolean canListPath,
							   int fileCacheSeconds) {
		var pathPrefixLen = pathPrefix.length();
		//noinspection DynamicRegexReplaceableByCompiledPattern
		var rootPath = fileRootPath.replaceFirst("[/\\\\]+$", "");
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
					x.sendFile(file);
				else if (canListPath && file.isDirectory() && !file.isHidden())
					x.sendPath(file);
				else
					x.close(x.send404());
			}
		}));
	}

	public void removeHandler(@NotNull String path) {
		if (handlers.remove(path) != null)
			Netty.logger.debug("removeHandler: {}", path);
	}

	public void removePrefixHandler(@NotNull String path) {
		if (prefixHandlers.remove(path) != null)
			Netty.logger.debug("removePrefixHandler: {}", path);
	}

	public @Nullable HttpHandler getHandler(@NotNull String path) {
		var handler = handlers.get(path);
		if (handler == null) {
			// 从字典序最大的候选(floorEntry(path))向下回退,第一个是path前缀的键即最长匹配前缀。
			// 只查一次floorEntry不够:字典序落在真实前缀与请求路径之间的更长非前缀键会挡住匹配,
			// 如注册"/a"和"/abc"后请求"/abd",floorEntry是"/abc"(不是前缀),必须继续回退才能命中"/a"。
			// 回退不会错过更长的匹配:path的任意两个前缀键中,短的是长的真前缀,字典序必更小。
			var e = prefixHandlers.floorEntry(path);
			while (e != null) {
				if (path.startsWith(e.getKey()))
					return e.getValue();
				e = prefixHandlers.lowerEntry(e.getKey());
			}
		}
		return handler;
	}

	// 允许扩展HttpExchange类。返回null表示忽略处理：框架将代回503并关闭连接（含该连接上在途的
	// exchange）；返回null前请勿自行写响应。使用恰当策略提前忽略可以避免同时接收太多请求数据导致OOM。
	public @Nullable HttpExchange createHttpExchange(@NotNull ChannelHandlerContext context) {
		return new HttpExchange(this, context);
	}

	@SuppressWarnings("RedundantThrows")
	protected void initChannel(@NotNull SocketChannel ch) throws Exception {
		Netty.logger.info("accept: {}", ch.remoteAddress());
		var p = ch.pipeline();
		if (sslCtx != null)
			p.addLast(sslCtx.newHandler(ch.alloc()));
		p.addLast(new HttpResponseEncoder() {
			@Override
			public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
				onBeforeWrite(ctx.channel());
				try {
					HttpExchange.checkResponseOrder(ctx.channel(), msg); // 出站tripwire：直写响应当场拒绝
				} catch (IllegalStateException e) {
					// Netty对出站写的同步异常只fail promise（写方不听则静默），必须显式走异常处置关连接
					ReferenceCountUtil.release(msg);
					promise.tryFailure(e);
					ctx.fireExceptionCaught(e);
					return;
				}
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
		// 兜底清理:连接已失活时在途exchange不会再有人close(异常路径或连接被强制关闭)，主动结束
		// 并释放retain的request和累积的content,否则永久泄漏。close是幂等的,正常完成的早已自行移除。
		// janitor关闭全部在途（pipelining下被覆盖出exchanges表的前序也要关），不只表内最新一个。
		HttpExchange.closeInFlightExchanges(ch, HttpExchange.CLOSE_PASSIVE);
		super.channelInactive(ctx);
	}

	@Override
	public void channelRead(@NotNull ChannelHandlerContext ctx, @Nullable Object msg) {
		try {
			var channelId = ctx.channel().id();
			// 拦截解码失败的消息(如畸形chunk size):Netty对此类错误不抛异常,而是产出带失败DecoderResult的
			// 空LastHttpContent或invalid message,不拦截的话截断的body会被当成完整请求交给handler处理。
			if (msg instanceof HttpObject httpObj && httpObj.decoderResult().isFailure()) {
				onDecodeFailure(ctx, httpObj);
				return;
			}
			HttpExchange x;
			if (msg instanceof HttpRequest) {
				// 停机后到达的新请求：不创建exchange/不派发handler，按onDecodeFailure模式回
				// 503 Service Unavailable并关连接，把close()期间的"静默丢弃"变为明确拒绝。
				if (shutdown) {
					Netty.logger.info("reject request from {} while shutdown", ctx.channel().remoteAddress());
					rejectAndClose(ctx, HttpResponseStatus.SERVICE_UNAVAILABLE);
					return;
				}
				if ((x = createHttpExchange(ctx)) == null) {
					// FND8-56：被拒请求不得裸return——其后续HttpContent/LastHttpContent帧会按channelId
					// 路由进仍在表内的前一个pipelined exchange（body串包+二次派发onEndStream+重复响应）。
					// 照搬停机503分支同构处置：框架代回503+关连接，同步善后在途exchange。
					Netty.logger.info("reject request from {} by createHttpExchange policy", ctx.channel().remoteAddress());
					rejectAndClose(ctx, HttpResponseStatus.SERVICE_UNAVAILABLE);
					return;
				}
				exchanges.put(channelId, x);
				// N①：登记请求到达序（响应序化器的排队依据）。在此（EventLoop）先于任何响应写完成，
				// Direct内联与非Direct派发的handler执行都晚于本登记。
				x.registerResponseOrder();
			} else if ((x = exchanges.get(channelId)) == null)
				return;
			x.channelRead(msg);
		} finally {
			ReferenceCountUtil.release(msg);
		}
	}

	// 框架级拒绝：代回状态（400/503）并关闭连接，同步移除并善后该连接上在途的exchange
	//（先移除,后续消息不再派发）——停机/创建策略拒绝（FND8-56）与解码失败共用处置。
	private void rejectAndClose(@NotNull ChannelHandlerContext ctx, @NotNull HttpResponseStatus status) {
		ctx.channel().attr(HttpExchange.responseOrderBypassKey).set(Boolean.TRUE); // tripwire豁免：框架直写，连接将亡
		var res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled.EMPTY_BUFFER,
			HttpExchange.headersFactory, HttpExchange.trailersFactory);
		res.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
		var cf = ctx.writeAndFlush(res).addListener(ChannelFutureListener.CLOSE);
		var prev = exchanges.remove(ctx.channel().id());
		if (prev != null)
			prev.close(HttpExchange.CLOSE_ON_FLUSH, cf);
	}

	// 畸形http消息(非法chunk size/坏头等):解码器产出DecoderResult.failure的HttpObject而不是抛异常。
	// 统一记录日志,回400并关闭连接;同时清理可能已半处理的exchange(release retain的request和已累积的content)。
	protected void onDecodeFailure(@NotNull ChannelHandlerContext ctx, @NotNull HttpObject obj) {
		Netty.logger.error("http decode failure from {}: {}", ctx.channel().remoteAddress(), obj.decoderResult().cause());
		rejectAndClose(ctx, HttpResponseStatus.BAD_REQUEST);
	}

	@Override
	public void userEventTriggered(@NotNull ChannelHandlerContext ctx, @Nullable Object evt) throws Exception {
		if (evt == ChannelInputShutdownEvent.INSTANCE) {
			// 客户端半关闭（发完请求即FIN，合法的HTTP半双工用法，childOption已开ALLOW_HALF_CLOSURE）：
			// 只标记willCloseConnection，让在途handler的响应发送完再关连接（响应完成后closeInEventLoop
			// 会context.close()）；不再强制结束exchange丢弃响应。请求不完整、永远等不到剩余body的连接
			// 由checkTimeout的读写空闲超时（默认30/60秒）兜底关闭。与下面ChannelInputShutdownReadComplete
			// 分支的处置一致。
			var x = exchanges.get(ctx.channel().id());
			if (x != null)
				x.willCloseConnection = true;
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
		// 背压而非断连：越过水位（writePendingLimit）只记日志，由写方感知isWritable/等待积压排出
		// （HttpResponseWithBodyStream已内置阻塞等待）。原先这里flush().close()直接杀连接——
		// 慢客户端+大响应（文件/流式）必然越过水位，合法流量被误杀；持续拥塞由checkTimeout0的
		// 写空闲超时（outboundBuffer无进度）兜底关闭。
		if (!ch.isWritable())
			Netty.logger.info("write buffer saturated {} > {} from {}",
				ch.unsafe().outboundBuffer().totalPendingWriteBytes(),
				ch.config().getWriteBufferHighWaterMark(), ch.remoteAddress());
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
			// 异常路径的exchange不会再有正常的close时机(如畸形uri解码抛出后无人移除)，这里结束全部
			// 在途，释放retain的request和累积的content，避免池化内存泄漏（close幂等）。先关闭连接再清理:
			// 即使清理过程中用户回调抛出异常,连接也已被关闭,close开头的exchanges.remove保证条目已删。
			HttpExchange.closeInFlightExchanges(ctx.channel(), HttpExchange.CLOSE_PASSIVE);
		}
	}
}
