package Zeze.Netty;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.net.URLDecoder;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Action0;
import Zeze.Util.Str;
import Zeze.Util.Task;
import Zeze.Util.TaskSpec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpHeadersFactory;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpHeadersFactory;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostMultipartRequestDecoder;
import io.netty.handler.codec.http.multipart.MemoryAttribute;
import io.netty.handler.codec.http.multipart.MemoryFileUpload;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketCloseStatus;
import io.netty.handler.codec.http.websocketx.WebSocketDecoderConfig;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolConfig;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.util.AttributeKey;
import io.netty.util.AttributeMap;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.collection.IntObjectHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.thymeleaf.context.Context;

/**
 * 一个HTTP请求从生到死的交换载体（对标JDK的com.sun.net.httpserver.HttpExchange）：
 * 每个HttpRequest由HttpServer.channelRead创建一个exchange，承接请求接收（body累积/流式转发）、
 * handler派发、响应写出与资源回收。响应写的唯一入口是内部的writeResponse——HTTP/1.1
 * pipelining下按请求到达序写出，绕过它直写ctx的响应头会被出站tripwire拒绝。
 *
 * <p>生命周期：handler（onEndStream，流式为onBeginStream+onStreamContent）按DispatchMode派发——
 * Direct内联（EventLoop线程）或任务派发且同连接回调串行（executeOneByOne(channel.id)）。
 * handler正常返回且未{@link #detach()}时框架自动close；detach后由调用方在任意线程、任意时机
 * 补发响应并close。close族幂等（detached CAS保证）；悬挂的exchange由idle超时与janitor
 * （断连/异常/停机清理点）兜底回收。
 */
public class HttpExchange {
	protected static final int CLOSE_FINISH = 0; // 正常结束HttpExchange,不关闭连接
	protected static final int CLOSE_ON_FLUSH = 1; // 结束HttpExchange,发送完时关闭连接
	protected static final int CLOSE_FORCE = 2; // 结束HttpExchange,不等发送完强制关闭连接
	protected static final int CLOSE_TIMEOUT = 3; // 同上,只是因idle超时而关闭
	protected static final int CLOSE_PASSIVE = 4; // 同上,只是因远程主动关闭而关闭

	protected static final @NotNull VarHandle detachedHandle;
	protected static final HttpDataFactory httpDataFactory = new DefaultHttpDataFactory(false);
	protected static final HttpHeadersFactory headersFactory = DefaultHttpHeadersFactory.headersFactory().withValidation(false);
	protected static final HttpHeadersFactory trailersFactory = DefaultHttpHeadersFactory.trailersFactory().withValidation(false);

	static {
		try {
			detachedHandle = MethodHandles.lookup().findVarHandle(HttpExchange.class, "detached", int.class);
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	protected final @NotNull HttpServer server; // 所属的HttpServer对象,每个对象管理监听端口的所有连接
	protected final @NotNull ChannelHandlerContext context; // netty的连接上下文,每个连接可能会依次绑定到多个HttpExchange对象
	protected @Nullable HttpRequest request; // 收到完整HTTP header部分会赋值
	protected @Nullable HttpHandler handler; // 收到完整HTTP header部分会查找对应handler并赋值
	protected @NotNull ByteBuf content = Unpooled.EMPTY_BUFFER; // 当前收集的HTTP body部分, 只用于非流模式
	protected @Nullable Object userState;
	protected @Nullable ArrayList<Object> resHeaders; // key,value,key,value,...
	protected @Nullable List<Cookie> cookies;
	protected @Nullable HttpSession.CookieSession cookieSession;
	protected @Nullable String path;
	protected volatile int detached; // 0:not detached; 1:detached; 2:detached and closed
	// close路径在任意线程写、closeInEventLoop（EL）读，volatile保证可见（终值兜底：context.close幂等）
	protected volatile boolean willCloseConnection;
	protected boolean inStreamMode; // 是否在流/WebSocket模式过程中
	protected boolean isWebSocketTextContent;
	protected long streamContentTotal; // 流模式累计收到的请求body字节数,server.maxUploadSize总量上限检查用

	public HttpExchange(@NotNull HttpServer server, @NotNull ChannelHandlerContext context) {
		this.server = server;
		this.context = context;
	}

	public HttpServer getHttpServer() {
		return server;
	}

	public @Nullable Object getUserState() {
		return userState;
	}

	public void setUserState(@Nullable Object userState) {
		this.userState = userState;
	}

	// lazy初始化:首次调用时在调用方线程完成(通常已运行在用户handler的事务内,同事务访问session表)。
	// 不再由channelRead在EventLoop上同步执行DB事务:那会让DB延迟/乐观锁redo直接阻塞IO线程,
	// DB抖动期间该EventLoop上所有连接的读写/握手/心跳全部停摆。未启用httpSession时返回null。
	public @Nullable HttpSession.CookieSession getCookieSession() {
		var cs = cookieSession;
		if (cs != null)
			return cs;
		var httpSession = server.getHttpSession();
		if (httpSession == null)
			return null;
		try {
			return cookieSession = httpSession.getCookieSession(this);
		} catch (Exception e) {
			throw Task.forceThrow(e);
		}
	}

	/**
	 * @param key 建议从Netty的HttpHeaderNames类里取字符串常量
	 */
	public void addHeader(@NotNull CharSequence key, @NotNull Object value) {
		var headers = resHeaders;
		if (headers == null)
			resHeaders = headers = new ArrayList<>();
		headers.add(key);
		headers.add(value);
	}

	public void addCookie(@NotNull String name, @NotNull String value) {
		setCookie(name, value, null, null, -1);
	}

	public void removeCookie(@NotNull String name) {
		setCookie(name, "", null, null, 0);
	}

	/**
	 * @param domain 域名. 可以是全的,也可以是跨域的,如".example.com". 默认是当前请求的域名(HOST)
	 * @param path   路径. 如"/","/path/". 默认是当前请求路径到最后一个"/"的部分
	 * @param maxAge 有效期. 小于0表示浏览器(内存)生命期(默认);0表示删除;大于0表示有效时长(秒)
	 */
	public void setCookie(@NotNull String name, @NotNull String value,
						  @Nullable String domain, @Nullable String path, long maxAge) {
		var cookie = new DefaultCookie(name, value);
		if (domain != null)
			cookie.setDomain(domain);
		if (path != null)
			cookie.setPath(path);
		if (maxAge >= 0)
			cookie.setMaxAge(maxAge);
		addHeader(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.LAX.encode(cookie));
	}

	/**
	 * @return 不可修改的Cookie容器
	 */
	public @NotNull List<Cookie> getCookieList() {
		var cs = cookies;
		if (cs != null)
			return cs;
		var r = request;
		if (r == null)
			return List.of();
		var cookieStr = r.headers().get(HttpHeaderNames.COOKIE);
		if (cookieStr == null || cookieStr.isBlank())
			return List.of();
		cookies = cs = ServerCookieDecoder.LAX.decodeAll(cookieStr);
		return cs;
	}

	public @Nullable String getCookie(@NotNull String name) {
		var cookies = getCookieList();
		if (!cookies.isEmpty()) {
			for (var cookie : cookies) {
				if (cookie.name().equals(name))
					return cookie.value();
			}
		}
		return null;
	}

	// 已收到请求头且未释放（勿与channel.isActive()的"连接活跃"语义混淆）
	public boolean hasRequest() {
		return request != null;
	}

	public boolean isClosed() {
		return detached == 2;
	}

	/**
	 * 通常不需要获取context，只给特殊需要时使用netty内部的方法。
	 * 注意：绕过响应写唯一入口直接经context写响应头会被出站tripwire拒绝（宁可断连不可错序）。
	 */
	public @NotNull ChannelHandlerContext context() {
		return context;
	}

	public @NotNull Channel channel() {
		return context.channel();
	}

	// 流式大响应的背压信号：false表示outbound缓冲已越过水位（writePendingLimit），继续无界发送会积压内存。
	// 高吞吐流式发送建议分块并检查；HttpResponseWithBodyStream的OutputStream已内置越水位阻塞等待。
	public boolean isWritable() {
		return context.channel().isWritable();
	}

	public @NotNull AttributeMap attributes() {
		return context.channel();
	}

	public @Nullable HttpRequest request() {
		return request;
	}

	public @NotNull ByteBuf content() {
		return content;
	}

	public @NotNull String contentString() {
		return content.toString(HttpServer.defaultCharset);
	}

	public byte @NotNull [] contentBytes() {
		var c = content;
		var size = c.readableBytes();
		if (size <= 0)
			return ByteBuffer.Empty;
		// 快路径仅在底层数组与内容严格重合时直接返回数组:池化/预分配缓冲capacity常大于writerIndex,
		// 直接返回c.array()会把尾随的陈旧字节(可能是上一请求残留)带给调用方,造成解析错乱/信息泄露。
		if (c.readerIndex() == 0 && c.hasArray() && c.arrayOffset() == 0 && c.writerIndex() == c.capacity())
			return c.array();
		var buf = new byte[size];
		c.getBytes(c.readerIndex(), buf);
		return buf;
	}

	// 用于close前提前释放content数据,如果不再需要用的话
	public void releaseContent() {
		if (content != Unpooled.EMPTY_BUFFER) {
			content.release();
			content = Unpooled.EMPTY_BUFFER;
		}
	}

	public @NotNull HttpPostMultipartRequestDecoder contentMultipart() {
		var req = request;
		if (req == null)
			throw new IllegalStateException();
		var multipart = new HttpPostMultipartRequestDecoder(httpDataFactory, req);
		var c = content;
		if (req instanceof HttpContent) {
			int s = ((HttpContent)req).content().readableBytes();
			c = c.slice(s, c.readableBytes() - s);
		}
		if (c.readableBytes() > 0)
			multipart.offer(new DefaultLastHttpContent(c));
		return multipart;
	}

	public static @NotNull String getMultipartString(@NotNull HttpPostMultipartRequestDecoder multipart,
													 @NotNull String key) {
		var httpData = multipart.getBodyHttpData(key);
		return httpData instanceof MemoryAttribute ? ((MemoryAttribute)httpData).getValue() : "";
	}

	public static byte @NotNull [] getMultipartBytes(@NotNull HttpPostMultipartRequestDecoder multipart,
													 @NotNull String key) {
		var httpData = multipart.getBodyHttpData(key);
		return httpData instanceof MemoryAttribute ? ((MemoryAttribute)httpData).get() : ByteBuffer.Empty;
	}

	public static byte @NotNull [] getMultipartFile(@NotNull HttpPostMultipartRequestDecoder multipart,
													@NotNull String key) {
		var httpData = multipart.getBodyHttpData(key);
		return httpData instanceof MemoryFileUpload ? ((MemoryFileUpload)httpData).get() : ByteBuffer.Empty;
	}

	public static @Nullable FileUpload getMultipartFileUpload(@NotNull HttpPostMultipartRequestDecoder multipart,
															  @NotNull String key) {
		var httpData = multipart.getBodyHttpData(key);
		return httpData instanceof MemoryFileUpload ? (MemoryFileUpload)httpData : null;
	}

	public static @NotNull String urlDecode(@NotNull String s) {
		for (int i = 0, n = s.length(); i < n; i++) {
			var c = s.charAt(i);
			if (c == '%' || c == '+')
				return URLDecoder.decode(s, HttpServer.defaultCharset);
		}
		return s;
	}

	// FND6-18：RFC 3986中'+'仅在query的form编码里代表空格，path段是普通字面量。
	// path解码仅处理百分号编码、'+'保真：%XX连续段按charset解码；十六进制严格限定ASCII
	// ——HexFormat.fromHexDigit（JDK17+，非法抛NumberFormatException转报IAE），不用
	// Character.digit(char,16)（会额外接受Unicode Nd数字如'٢'，比URLDecoder语义更宽）。
	// 畸形/不完整的%序列保持抛IllegalArgumentException（消息沿用URLDecoder风格，异常路径
	// 500+关连接的既有契约不变，FND-N2-1回归依赖）；不含'+'的路径行为与原实现一致。
	private static @NotNull String pathDecode(@NotNull String s) {
		if (s.indexOf('%') < 0)
			return s;
		var sb = new StringBuilder(s.length());
		var bytes = new byte[s.length()];
		int k = 0;
		for (int i = 0, n = s.length(); i < n; ) {
			char c = s.charAt(i);
			if (c == '%') {
				if (i + 2 >= n)
					throw new IllegalArgumentException("Incomplete trailing escape (%) pattern: " + s);
				int hi, lo;
				try {
					hi = HexFormat.fromHexDigit(s.charAt(i + 1));
					lo = HexFormat.fromHexDigit(s.charAt(i + 2));
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("Illegal hexadecimal characters in escape (%) pattern: " + s);
				}
				bytes[k++] = (byte)((hi << 4) | lo);
				i += 3;
				continue;
			}
			if (k > 0) {
				sb.append(new String(bytes, 0, k, HttpServer.defaultCharset));
				k = 0;
			}
			sb.append(c);
			i++;
		}
		if (k > 0)
			sb.append(new String(bytes, 0, k, HttpServer.defaultCharset));
		return sb.toString();
	}

	public @NotNull String path() {
		if (path != null)
			return path;
		var req = request;
		if (req == null)
			return "";
		var uri = req.uri();
		var i = uri.indexOf('?');
		return path = pathDecode(i >= 0 ? uri.substring(0, i) : uri);
	}

	public @Nullable String query() {
		var req = request;
		if (req == null)
			return "";
		var uri = req.uri();
		var i = uri.indexOf('?');
		return i >= 0 ? uri.substring(i + 1) : null;
	}

	public static @NotNull Map<String, String> parseQuery(@Nullable String s) {
		if (s == null)
			return Map.of();
		var m = new LinkedHashMap<String, String>();
		for (int i = 0, b = 0, e = -1, n = s.length(); ; i++) {
			int c = i < n ? s.charAt(i) : '&';
			if (c == '&') {
				m.put(urlDecode(s.substring(b, e >= 0 ? e : i)), e >= 0 ? urlDecode(s.substring(e + 1, i)) : "");
				if (i >= n)
					return m;
				b = i + 1;
				e = -1;
			} else if (c == '=' && e < 0)
				e = i;
		}
	}

	public @NotNull Map<String, String> queryMap() {
		return parseQuery(query());
	}

	public @NotNull Map<String, String> contentQueryMap() {
		return parseQuery(content().toString(HttpServer.defaultCharset));
	}

	protected void channelRead(@Nullable Object msg) {
		var channel = context.channel();
		channel.attr(HttpServer.idleTimeKey).set(null);
		if (msg instanceof HttpRequest) {
			var req = ReferenceCountUtil.retain((HttpRequest)msg);
			request = req;
			var path = path();
			handler = server.getHandler(path);
			if (handler == null) {
				close(send404());
				return;
			}
			if (handler.isWebSocketMode() && context.pipeline().get(WebSocketServerProtocolHandler.class) == null) {
				context.pipeline().addLast(new WebSocketServerProtocolHandler(WebSocketServerProtocolConfig.newBuilder()
					.websocketPath(path).decoderConfig(WebSocketDecoderConfig.newBuilder().withUTF8Validator(false)
						.maxFramePayloadLength(handler.MaxContentLength).build()).build()));
				// onOpen不能在握手启动前派发:此刻101应答未写出、HttpResponseEncoder未替换为WebSocket帧
				// 编码器,onOpen内sendWebSocket的帧写入HTTP出站编码路径,写失败(unsupported message
				// type),Direct模式下onOpen内联执行时首条消息确定性静默丢失。改在握手完成后的
				// HandshakeComplete用户事件时派发:该事件由Netty握手处理器在本handler之后向tail方向触发,
				// 位于前面的HttpServer收不到,需在管线末尾追加观察者接住;事件触发时101已写出、编码器已替换,
				// 且必然先于客户端任何帧被读到,保证onOpen派发先于onContent。
				context.pipeline().addLast(new ChannelInboundHandlerAdapter() {
					@Override
					public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
						if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
							//noinspection ConstantConditions
							fireWebSocketNotify("fireWebSocketOpen",
								() -> handler.WebSocketHandle.onOpen(HttpExchange.this));
						}
						super.userEventTriggered(ctx, evt);
					}
				});
				context.pipeline().addFirst(new ChannelOutboundHandlerAdapter() {
					@Override
					public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
						HttpServer.onBeforeWrite(ctx.channel());
						super.write(ctx, msg, promise);
					}
				});
				inStreamMode = true;
				context.fireChannelRead(msg);
				return;
			}
			if (handler.isStreamMode()) {
				fireBeginStream(req);
				inStreamMode = true;
			}
			if (!(msg instanceof HttpContent)) {
				if (HttpUtil.is100ContinueExpected(req)) {
					if (!handler.isStreamMode() && HttpUtil.getContentLength(req, 0) > handler.MaxContentLength) {
						closeConnectionOnFlush(writeResponse(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
							HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE, Unpooled.EMPTY_BUFFER,
							headersFactory, trailersFactory), true, null)); // N①
						return;
					}
					writeResponse(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE,
						Unpooled.EMPTY_BUFFER, headersFactory, trailersFactory), true, context.voidPromise()); // N①
				}
				return;
			}
		} else if (msg instanceof WebSocketFrame) {
			fireWebSocket((WebSocketFrame)msg);
			return;
		} else if (!(msg instanceof HttpContent)) {
			Netty.logger.error("unknown message type = {} from {}",
				(msg != null ? msg.getClass() : null), channel.remoteAddress());
			closeConnectionNow();
			return;
		} else if (request == null || handler == null) // 缺失上文的msg,可能很罕见,忽略吧
			return;

		var c = (HttpContent)msg;
		var b = c.content();
		var n = b.readableBytes();
		if (n > 0) {
			if (handler.isStreamMode()) {
				// 流模式不受handler.MaxContentLength约束(全量缓冲才有意义,流式处理本应边收边处理),
				// 但multipart/raw等默认实现在服务端全量缓冲(堆或临时文件),无总量限制会被单连接打爆堆/磁盘,
				// 这里以server.maxUploadSize兜底请求body的总量,超限回413并断开连接。
				streamContentTotal += n;
				var maxUploadSize = server.getMaxUploadSize();
				if (streamContentTotal > maxUploadSize) {
					Netty.logger.error("upload size = {} > {} from {}", streamContentTotal, maxUploadSize,
						channel.remoteAddress());
					closeConnectionOnFlush(writeResponse(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
						HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE, Unpooled.EMPTY_BUFFER,
						headersFactory, trailersFactory), true, null)); // N①
					return;
				}
				fireStreamContentHandle(c);
			} else {
				if (content.readableBytes() + n > handler.MaxContentLength) {
					Netty.logger.error("content size = {} + {} > {} from {}", content.readableBytes(), n,
						handler.MaxContentLength, channel.remoteAddress());
					closeConnectionOnFlush(writeResponse(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
						HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE, Unpooled.EMPTY_BUFFER,
						headersFactory, trailersFactory), true, null)); // N①
					return;
				}
				addContent(b.retain());
			}
		}
		if (c instanceof LastHttpContent) {
			inStreamMode = false;
			fireEndStreamHandle(); // 流模式和非流模式通用
		}
	}

	// newContent会被转移所有权,调用者需要继续用的话,应该retain一次再传. 返回当前累积的content
	public @NotNull ByteBuf addContent(@NotNull ByteBuf newContent) {
		var c = content;
		if (c == Unpooled.EMPTY_BUFFER)
			content = c = newContent;
		else if (c instanceof CompositeByteBuf)
			((CompositeByteBuf)c).addComponent(true, newContent);
		else
			content = c = c.alloc().compositeBuffer().addComponent(true, c).addComponent(true, newContent);
		return c;
	}

	@SuppressWarnings("DataFlowIssue")
	protected void fireBeginStream(@NotNull HttpRequest req) {
		if (handler == null)
			return;
		var r = HttpFileService.parseRange(req, HttpHeaderNames.CONTENT_RANGE);
		if (!server.noProcedure && handler.Level != TransactionLevel.None) {
			var p = server.zeze.newProcedure(() -> {
				handler.BeginStreamHandle.onBeginStream(this, r[0], r[1], r[2]);
				return Procedure.Success;
			}, "fireBeginStream");
			if (handler.Mode == DispatchMode.Direct)
				TaskSpec.ofProcedure(p).call();
			else
				TaskSpec.ofProcedure(p)
					.dispatchMode(handler.Mode).executeOneByOne(context.channel().id(), server.task11Executor);
		} else if (handler.Mode == DispatchMode.Direct) {
			TaskSpec.ofAction(() -> handler.BeginStreamHandle.onBeginStream(this, r[0], r[1], r[2]))
				.name("fireBeginStream").call();
		} else {
			TaskSpec.ofAction(() -> handler.BeginStreamHandle.onBeginStream(this, r[0], r[1], r[2]))
				.name("fireBeginStream").dispatchMode(handler.Mode)
				.executeOneByOne(context.channel().id(), server.task11Executor);
		}
	}

	protected void fireStreamContentHandle(@NotNull HttpContent c) {
		var handle = handler != null ? handler.StreamContentHandle : null;
		if (handle == null)
			return;
		if (!server.noProcedure && handler.Level != TransactionLevel.None) {
			//noinspection DataFlowIssue
			var p = server.zeze.newProcedure(() -> {
				handle.onStreamContent(this, c);
				return Procedure.Success;
			}, "fireStreamContentHandle");
			if (handler.Mode == DispatchMode.Direct)
				TaskSpec.ofProcedure(p).call();
			else {
				// onCancel:HttpServer.close()的shutdown(true)清扫丢弃未运行任务(或提交命中已shutdown队列)时
				// 补偿release,否则retain的池化content在关停窗口静默泄漏;cancel与执行路径互斥,不会双释放。
				c.retain();
				TaskSpec.ofFunc(() -> {
						try {
							return p.call();
						} finally {
							c.release();
						}
					}).name(p.getActionName()).dispatchMode(handler.Mode).onCancel(c::release)
					.executeOneByOne(context.channel().id(), server.task11Executor);
			}
		} else if (handler.Mode == DispatchMode.Direct) {
			TaskSpec.ofAction(() -> handle.onStreamContent(this, c)).name("fireStreamContentHandle").call();
		} else {
			c.retain();
			TaskSpec.ofAction(() -> {
					try {
						handle.onStreamContent(this, c);
					} finally {
						c.release();
					}
				}).name("fireStreamContentHandle").dispatchMode(handler.Mode).onCancel(c::release)
				.executeOneByOne(context.channel().id(), server.task11Executor);
		}
	}

	/**
	 * 声明本exchange的生命周期由调用方接管：handler返回后框架不再自动close，
	 * 可在任意线程、任意时机补发响应（sendXxx/beginStream/sendFile）并close。
	 * 不detach也不在handler内close视为悬挂，由idle超时/janitor兜底回收。
	 */
	public @NotNull HttpExchange detach() {
		detachedHandle.compareAndSet(this, 0, 1);
		return this;
	}

	/// //////////////////////////////////////////////////////////////////////////////////////////////////////////
	// pipelining响应序化：HTTP/1.1要求响应按请求到达序写出，而响应写可能从任意线程、任意时机发出
	//（异步完成、Direct内联、用户线程）——后到请求的响应先上线，对pipelining客户端是整条连接的
	// 静默数据错乱。
	// 单写者：序化状态只由channel的EventLoop读写，非EL调用任务跳转（Netty的写本也要跳EL，成本
	// 持平；同线程提交序即任务序，顺序不变）。游标：登记时（channelRead，EL，先于任何响应写）分配
	// 递增orderId，writingOrderId即"笔"——orderId等于它直写；大于它挂起（promise桥接，真实写出时
	// 兑现）；小于它是已出队的迟到写：失败并释放（终结符已写出，放行会污染后继响应的帧）。
	// close/endStream标记entry完成；持笔者冲刷挂起（末尾单次flush）并链式出队到下一个存活entry，
	// 其后续写直写。FINISH族挂起写按序送出（endStream终结符也在其中），FORCE族（连接将亡）丢弃。
	// 非pipelining（单在途请求）时exchange即持笔者，写直达；直接构造、未经channelRead登记的
	// exchange不参与序化。
	// 已知豁免（不经序化器）：WebSocket升级101与帧（升级请求不会被pipelining）、HttpServer自身的
	// 400/503直写（随即关闭连接，responseOrderBypassKey声明豁免）、HttpResponseWithBodyStream的
	// 异常中止路径（连接将亡）。豁免之外的响应头直写由出站tripwire（checkResponseOrder，
	// HttpServer的encoder write钩子）当场拒绝。
	static final AttributeKey<ResponseSequencer> responseOrderKey = AttributeKey.valueOf("ZezeHttpResponseOrder");
	// 单exchange挂起响应写上限：防pipelining滥用驻留内存（每挂起写持有一个响应消息）。
	private static final int MaxPendingResponseWrites = 256;
	// 每channel在途（已登记未出队）请求序深度上限：防深度pipelining滥用，超出按滥用关闭连接。
	private static final int MaxResponseOrderDepth = 128;

	// 序化器per-channel（channel属性）。全部字段仅channel的EventLoop线程访问（单写者）。
	static final class ResponseSequencer {
		int nextOrderId = 1; // 下一个到达序（registerResponseOrder分配）
		int writingOrderId = 1; // 当前持笔orderId：此前的已全部写出出队
		final IntObjectHashMap<OrderEntry> entries = new IntObjectHashMap<>(); // 在途entry
	}

	// 在途exchange的序化条目（仅EventLoop线程访问）。
	static final class OrderEntry {
		final HttpExchange x;
		ArrayDeque<DeferredWrite> pending; // 未轮到时的挂起响应写，懒建
		boolean finished; // exchange已close/endStream（FINISH族挂起写仍按序送出）
		boolean started; // 本entry的按序写已开始（出站tripwire的判定依据）

		OrderEntry(HttpExchange x) {
			this.x = x;
		}
	}

	/**
	 * @param promise 调用方给定的promise（可为voidPromise），提交时原样传递
	 */
	record DeferredWrite(@NotNull Object msg, @NotNull ChannelPromise promise, boolean flush) {
	}

	// 响应序化状态：仅EventLoop线程访问，登记（先于任何响应写）时赋值。
	@Nullable ResponseSequencer responseSequencer;
	int responseOrderId; // 登记分配的到达序；未登记（直接构造）为0且responseSequencer为null
	private @Nullable OrderEntry responseEntry; // 本exchange的序化条目（出队时清引用）

	// 框架直写豁免标志（rejectAndClose设置）：声明该channel的后续直写响应合法（随即关连接），免清理。
	static final AttributeKey<Boolean> responseOrderBypassKey = AttributeKey.valueOf("ZezeHttpResponseOrderBypass");

	// 出站tripwire（HttpServer的encoder write钩子调用）：任何响应头(HttpResponse)写出时，序化器持笔
	// entry必须已开始写——绕过writeResponse直写ctx的响应（FND8-46类）当场抛异常（→exceptionCaught
	// →关连接），把"pipelining客户端静默拿到错配响应"变成测试期响亮失败。宁可断连不可错序。
	// 豁免：WebSocket升级（管线含协议处理器时整条连接跳过，升级后已无HTTP响应语义）、框架400/503
	// 拒绝（responseOrderBypassKey声明）。限制：在途全部出队后的迟到直写无法归因，不检查。
	static void checkResponseOrder(@NotNull Channel ch, @NotNull Object msg) {
		if (!(msg instanceof HttpResponse))
			return;
		var seq = ch.attr(responseOrderKey).get();
		if (seq == null) // 无登记（首请求即被拒/直接构造）：无序化语义可违反
			return;
		if (ch.pipeline().get(WebSocketServerProtocolHandler.class) != null)
			return;
		if (ch.attr(responseOrderBypassKey).get() != null)
			return;
		var e = seq.entries.get(seq.writingOrderId);
		if (e != null && !e.started)
			throw new IllegalStateException("http response written out of order from " + ch.remoteAddress()
				+ ": sequencer head(order=" + seq.writingOrderId + ") not started, bypass writeResponse?");
	}

	// HttpServer.channelRead收到HttpRequest创建本exchange后登记（EventLoop线程，先于任何响应写）。
	void registerResponseOrder() {
		var ch = context.channel();
		var seq = ch.attr(responseOrderKey).get();
		if (seq == null) {
			var created = new ResponseSequencer();
			var prev = ch.attr(responseOrderKey).setIfAbsent(created);
			seq = prev != null ? prev : created;
		}
		if (seq.entries.size() >= MaxResponseOrderDepth) { // 深度pipelining滥用：不登记（后续写不序化直发），关连接
			Netty.logger.error("too many in-flight pipelined exchanges: {} from {}, close connection",
				MaxResponseOrderDepth, ch.remoteAddress());
			closeConnectionNow();
			return;
		}
		responseOrderId = seq.nextOrderId++;
		responseSequencer = seq;
		responseEntry = new OrderEntry(this);
		seq.entries.put(responseOrderId, responseEntry);
	}

	// janitor：关闭channel上全部在途exchange（entries即权威在途列表）。四个清理点原先只关exchanges
	// 表内最新一个，pipelining下被后续请求覆盖出表的前序exchange无人close（泄漏retain的request、
	// 累积content、挂起响应写）。entries为EL状态：非EL调用跳EL执行。返回是否有exchange被关闭
	//（仅EL内联调用有意义，异步路径恒false）。
	static boolean closeInFlightExchanges(@NotNull Channel ch, int method) {
		var seq = ch.attr(responseOrderKey).get();
		if (seq == null)
			return false;
		var loop = ch.eventLoop();
		if (loop.inEventLoop())
			return closeInFlightExchanges0(seq, method);
		try {
			loop.execute(() -> closeInFlightExchanges0(seq, method));
		} catch (RejectedExecutionException ignored) {
			// EventLoop已关停（Netty整体关闭中）：channel必已关闭，无需清理
		}
		return false;
	}

	// 仅channel EventLoop线程调用。先快照再逐个close：close的release会修改entries。
	private static boolean closeInFlightExchanges0(@NotNull ResponseSequencer seq, int method) {
		if (seq.entries.isEmpty())
			return false;
		var inFlight = seq.entries.values().toArray(new OrderEntry[0]);
		for (var e : inFlight)
			e.x.close(method, null);
		return true;
	}

	// 响应写唯一入口（send/beginStream/sendStream/endStream/sendFile/100-continue/413/close(null)空写
	// 共用；包内可见——唯一协作方为HttpFileService（包内静态文件服务），出站tripwire在wire层守门）：
	// 决策与写出都在EventLoop上（单写者）。promise为null时新建（桥接挂起写，调用方listener/close(future)
	// 语义在真实写出时兑现）。
	ChannelFuture writeResponse(@NotNull Object msg, boolean flush, @Nullable ChannelPromise promise) {
		if (promise == null)
			promise = context.newPromise();
		var finalPromise = promise;
		//noinspection resource
		var loop = context.channel().eventLoop();
		if (loop.inEventLoop())
			writeResponse0(msg, flush, finalPromise);
		else {
			try {
				loop.execute(() -> writeResponse0(msg, flush, finalPromise));
			} catch (RejectedExecutionException e) { // EventLoop已关停：连接必已失效
				finalPromise.tryFailure(e);
				ReferenceCountUtil.release(msg);
			}
		}
		return finalPromise;
	}

	// 仅EventLoop线程调用：按orderId与writingOrderId的关系直写/挂起/拒绝迟到写。
	@SuppressWarnings("ConstantConditions")
	private void writeResponse0(@NotNull Object msg, boolean flush, @NotNull ChannelPromise promise) {
		var seq = responseSequencer;
		if (seq == null) { // 未登记（直接构造的exchange）：不序化，保持原行为
			submit(msg, promise, flush);
			return;
		}
		if (responseOrderId < seq.writingOrderId) { // 已出队的迟到写：失败并释放。放行的字节会插进
			// 后继响应的帧中间；合法调用不会到达（close/endStream的CAS都先于各自最后的写）
			Netty.logger.warn("late response write after exchange finished: {}", context.channel().remoteAddress());
			promise.tryFailure(new ClosedChannelException());
			ReferenceCountUtil.release(msg);
			return;
		}
		if (responseOrderId > seq.writingOrderId) { // 未轮到：挂起（close后未出队的写也在此列，轮到时冲刷）
			var e = responseEntry;
			var pending = e.pending != null ? e.pending : (e.pending = new ArrayDeque<>());
			pending.addLast(new DeferredWrite(msg, promise, flush));
			if (pending.size() >= MaxPendingResponseWrites) { // 挂起上限：按滥用关闭连接
				Netty.logger.error("too many pending response writes: {} from {}, close connection",
					pending.size(), context.channel().remoteAddress());
				closeConnectionNow(); // FORCE族丢弃全部挂起写（含刚加入的这条，promise随之失败）
			}
			return;
		}
		responseEntry.started = true; // 先于submit：编码器的出站tripwire据此判定持笔已开始
		submit(msg, promise, flush); // 持笔：直达，同exchange内部顺序由（同线程）提交序保证
	}

	// 仅channel EventLoop线程调用：写出（或提交）一条消息。同步异常补失败并释放，防挂起promise与泄漏。
	private ChannelFuture submit(@NotNull Object msg, @NotNull ChannelPromise promise, boolean flush) {
		try {
			return flush ? context.writeAndFlush(msg, promise) : context.write(msg, promise);
		} catch (Throwable e) {
			promise.tryFailure(e);
			ReferenceCountUtil.release(msg);
			return promise;
		}
	}

	// close（CAS成功方）或endStream时调用：标记完成；持笔者冲刷挂起并链式让位。FINISH族保留挂起写
	// 按序冲刷，FORCE族丢弃。非EL调用跳EL（排在本exchange先前的写任务之后，挂起写先于让位被看到）。
	private void releaseResponseOrder(boolean dropPending) {
		//noinspection resource
		var loop = context.channel().eventLoop();
		if (loop.inEventLoop()) {
			releaseResponseOrder0(dropPending);
			return;
		}
		try {
			loop.execute(() -> releaseResponseOrder0(dropPending));
		} catch (RejectedExecutionException ignored) {
			// EventLoop已关停：连接必已关闭，挂起写随channel终结
		}
	}

	// 仅EventLoop线程调用
	private void releaseResponseOrder0(boolean dropPending) {
		var seq = responseSequencer;
		var e = responseEntry;
		if (seq == null || e == null)
			return;
		if (dropPending) {
			var pending = e.pending;
			if (pending != null) {
				e.pending = null;
				while (!pending.isEmpty()) {
					var w = pending.pollFirst();
					w.promise().tryFailure(new ClosedChannelException());
					ReferenceCountUtil.release(w.msg());
				}
			}
		}
		e.finished = true;
		if (responseOrderId == seq.writingOrderId) // 持笔让位：冲刷挂起并链式推进
			advance(seq); // 非持笔者无需推进，由持笔方的release按序推进到它
	}

	// 仅EventLoop线程、持笔者让位时调用：冲刷头部挂起写（末尾单次flush合并），已完成的entry出队
	// 并链式推进，遇存活entry停（其后续写直写）。
	private static void advance(@NotNull ResponseSequencer seq) {
		for (; ; ) {
			var e = seq.entries.get(seq.writingOrderId);
			if (e == null)
				return; // 在途全部出队
			drainPending(e);
			if (!e.finished)
				return; // 存活的头部entry接笔：等它后续写/让位
			seq.entries.remove(seq.writingOrderId);
			e.x.responseEntry = null;
			seq.writingOrderId++;
		}
	}

	// 仅EventLoop线程调用：冲刷entry全部挂起写，末尾单次flush。
	private static void drainPending(@NotNull OrderEntry e) {
		var pending = e.pending;
		if (pending == null || pending.isEmpty())
			return;
		e.pending = null;
		e.started = true; // 本entry的按序写开始（出站tripwire依据），先于submit
		var x = e.x;
		while (!pending.isEmpty()) {
			var w = pending.pollFirst();
			x.submit(w.msg(), w.promise(), false);
		}
		x.context.flush();
	}

	protected void invokeEndStream() throws Exception {
		try {
			var handle = handler != null ? handler.EndStreamHandle : null;
			if (handle != null)
				handle.onEndStream(this);
		} finally {
			if (detached == 0)
				close(null);
		}
	}

	@SuppressWarnings("ConstantConditions")
	protected void fireEndStreamHandle() {
		// onCancel:HttpServer.close()的shutdown(true)清扫丢弃未运行任务（或提交命中已shutdown队列）时
		// 补偿close（与任务finally一致），释放retain的request与累积的content（池化内存）——
		// pipelining下本exchange可能已被channelRead的exchanges.put覆盖逐出，停机清扫扫不到它，
		// 不补偿则永久泄漏。cancel与执行路径互斥（TaskOneByOneQueue保证），close幂等，不会双释放。
		// 对照:fireStreamContentHandle/fireWebSocket的onCancel同因。
		// FND8-58：close只释放exchange自身资源，不触碰channel attr上的流式上传状态——multipart
		// 解码器/上传缓冲（堆数据、临时文件、未关fileChannel）永久驻留static工厂的
		// requestFileDeleteMap，跨close→start重启累积无上界。这里做资源级销毁（不跑用户回调，
		// 对齐fireStreamContentHandle的补偿哲学）；幂等由getAndSet(null)的"取走即负责"语义
		// 保证（Netty destroy()非幂等，不能裸调两次）。attr是channel级：停机时所有exchange一并
		// 终结，跨请求"取走"垂死请求的decoder无害（对方自己的cancel只会拿到null）。
		var cancel = (Action0)() -> {
			if (detached == 0)
				close(null);
			var decoder = context.channel().attr(HttpMultipartHandle.decoderKey).getAndSet(null);
			if (decoder != null) {
				try {
					decoder.destroy();
				} catch (Throwable e) {
					Netty.logger.error("multipart decoder destroy on cancel", e);
				}
			}
			var fileUpload = context.channel().attr(HttpFileUploadHandle.fileUploadKey).getAndSet(null);
			if (fileUpload != null) {
				try {
					fileUpload.release();
				} catch (Throwable e) {
					Netty.logger.error("file upload release on cancel", e);
				}
			}
		};
		if (!server.noProcedure && handler.Level != TransactionLevel.None) {
			var p = server.zeze.newProcedure(() -> {
				var handle = handler.EndStreamHandle;
				if (handle != null)
					handle.onEndStream(this);
				return Procedure.Success;
			}, "fireEndStreamHandle");
			if (handler.Mode == DispatchMode.Direct) {
				try {
					TaskSpec.ofProcedure(p).call();
				} finally {
					if (detached == 0)
						close(null);
				}
			} else {
				TaskSpec.ofFunc(() -> {
						try {
							return p.call();
						} finally {
							if (detached == 0)
								close(null);
						}
					}).name(p.getActionName()).dispatchMode(handler.Mode).onCancel(cancel)
					.executeOneByOne(context.channel().id(), server.task11Executor);
			}
		} else if (handler.Mode == DispatchMode.Direct) {
			TaskSpec.ofAction(this::invokeEndStream).name("fireEndStreamHandle").call();
		} else {
			TaskSpec.ofAction(this::invokeEndStream)
				.name("fireEndStreamHandle").dispatchMode(handler.Mode).onCancel(cancel)
				.executeOneByOne(context.channel().id(), server.task11Executor);
		}
	}

	protected void fireWebSocket(@NotNull WebSocketFrame frame) {
		if (handler == null)
			return;
		if (!server.noProcedure && handler.Level != TransactionLevel.None) {
			//noinspection DataFlowIssue
			var p = server.zeze.newProcedure(() -> {
				fireWebSocket0(frame);
				return Procedure.Success;
			}, "fireWebSocket");
			if (handler.Mode == DispatchMode.Direct)
				TaskSpec.ofProcedure(p).call();
			else {
				// onCancel:同fireStreamContentHandle,shutdown清扫丢弃时补偿release retain的池化frame。
				frame.retain();
				TaskSpec.ofFunc(() -> {
						try {
							return p.call();
						} finally {
							frame.release();
						}
					}).name(p.getActionName()).dispatchMode(handler.Mode).onCancel(frame::release)
					.executeOneByOne(context.channel().id(), server.task11Executor);
			}
		} else if (handler.Mode == DispatchMode.Direct) {
			TaskSpec.ofAction(() -> fireWebSocket0(frame)).name("fireWebSocket").call();
		} else {
			frame.retain();
			TaskSpec.ofAction(() -> {
					try {
						fireWebSocket0(frame);
					} finally {
						frame.release();
					}
				}).name("fireWebSocket").dispatchMode(handler.Mode).onCancel(frame::release)
				.executeOneByOne(context.channel().id(), server.task11Executor);
		}
	}

	// WebSocket的onOpen/onClose通知走与fireWebSocket相同的Mode派发(Direct内联/否则executeOneByOne,
	// key=channel.id与onContent同队列串行)。修复前channelRead的onOpen与closeInEventLoop的异常关闭onClose
	// 都在EventLoop上内联执行:用户回调阻塞IO线程,且与在途的onContent派发任务并发访问用户状态,
	// 绕过了"同连接回调串行"的派发契约(纯非Direct配置也触发)。
	@SuppressWarnings("ConstantConditions")
	protected void fireWebSocketNotify(@NotNull String name, @NotNull Action0 notify) {
		if (!server.noProcedure && handler.Level != TransactionLevel.None) {
			var p = server.zeze.newProcedure(() -> {
				notify.run();
				return Procedure.Success;
			}, name);
			if (handler.Mode == DispatchMode.Direct)
				TaskSpec.ofProcedure(p).call();
			else
				TaskSpec.ofProcedure(p)
					.dispatchMode(handler.Mode).executeOneByOne(context.channel().id(), server.task11Executor);
		} else if (handler.Mode == DispatchMode.Direct) {
			TaskSpec.ofAction(notify).name(name).call();
		} else {
			TaskSpec.ofAction(notify).name(name).dispatchMode(handler.Mode)
				.executeOneByOne(context.channel().id(), server.task11Executor);
		}
	}

	// WebSocket分片消息(首帧isFinal=false+后续Continuation帧)会在content无上限累积,单连接即可耗尽堆内存。
	// 以handler.MaxContentLength(即maxFramePayloadLength的同一配置)作为分片消息的总大小上限:
	// 单帧本身已受maxFramePayloadLength限制,这里挡的是分片累积总量。超限按RFC6455回1009(Message Too Big)并关闭连接。
	// 返回false表示已超限并关闭连接,调用方不应再继续分发本帧。
	protected boolean checkWebSocketContentSize(@NotNull WebSocketFrame frame) {
		var n = frame.content().readableBytes();
		//noinspection DataFlowIssue
		if (content.readableBytes() + n > handler.MaxContentLength) {
			Netty.logger.error("websocket content size = {} + {} > {} from {}",
				content.readableBytes(), n, handler.MaxContentLength, context.channel().remoteAddress());
			closeConnectionOnFlush(context.writeAndFlush(
				new CloseWebSocketFrame(WebSocketCloseStatus.MESSAGE_TOO_BIG, "message too big")));
			return false;
		}
		return true;
	}

	@SuppressWarnings("ConstantConditions")
	protected void fireWebSocket0(@NotNull WebSocketFrame frame) throws Exception {
		switch (frame) {
		case BinaryWebSocketFrame ignored -> {
			isWebSocketTextContent = false;
			if (checkWebSocketContentSize(frame))
				handler.WebSocketHandle.onContent(this, frame.content(), false, frame.isFinalFragment());
		}
		case TextWebSocketFrame ignored -> {
			isWebSocketTextContent = true;
			if (checkWebSocketContentSize(frame))
				handler.WebSocketHandle.onContent(this, frame.content(), true, frame.isFinalFragment());
		}
		case ContinuationWebSocketFrame ignored -> {
			if (checkWebSocketContentSize(frame))
				handler.WebSocketHandle.onContent(this, frame.content(), isWebSocketTextContent, frame.isFinalFragment());
		}
		case CloseWebSocketFrame closeFrame -> {
			inStreamMode = false;
			handler.WebSocketHandle.onClose(this, closeFrame.statusCode(), closeFrame.reasonText());
			closeConnectionOnFlush(null);
		}
		case PingWebSocketFrame ignored -> handler.WebSocketHandle.onPing(this, frame.content());
		case PongWebSocketFrame ignored -> handler.WebSocketHandle.onPong(this, frame.content());
		default -> {
			Netty.logger.error("unknown websocket message type = {} from {}",
				frame.getClass().getName(), context.channel().remoteAddress());
			closeConnectionNow();
		}
		}
	}

	protected void closeInEventLoop() {
		if (inStreamMode) {
			inStreamMode = false;
			try {
				if (handler != null && handler.isWebSocketMode()) {
					// 只在EventLoop上执行(close的listener/execute分支保证)。onClose不能内联在EL执行,
					// 改走与fireWebSocket相同的Mode派发,与在途onContent派发任务按channel.id串行。
					//noinspection ConstantConditions
					fireWebSocketNotify("fireWebSocketClose", () -> handler.WebSocketHandle.onClose(
						this, WebSocketCloseStatus.ABNORMAL_CLOSURE.code(), ""));
				} else
					fireEndStreamHandle();
			} catch (Exception e) {
				throw Task.forceThrow(e);
			}
		}
		releaseContent();
		var req = request;
		if (req != null) {
			ReferenceCountUtil.release(req);
			request = null;
		}
		if (willCloseConnection)
			context.close();
	}

	protected void close(int method, @Nullable ChannelFuture cf) {
		if ((int)detachedHandle.getAndSet(this, 2) == 2)
			return;
		var ch = context.channel();
		server.exchanges.remove(ch.id(), this); // 尝试删除,避免继续接收当前请求的消息
		if (method != CLOSE_FINISH) {
			willCloseConnection = true;
			if (method == CLOSE_ON_FLUSH)
				Netty.logger.info("closeOnFlush: {}", ch.remoteAddress());
		}
		if (method <= CLOSE_ON_FLUSH) { // CLOSE_FINISH | CLOSE_ON_FLUSH
			if (cf == null)
				// 经序化器：未持笔时挂起，避免空写flush先于挂起响应触发closeInEventLoop（ON_FLUSH族会提前关连接）
				cf = writeResponse(Unpooled.EMPTY_BUFFER, true, null);
			cf.addListener(__ -> closeInEventLoop());
		} else {
			var eventLoop = ch.eventLoop();
			if (eventLoop.inEventLoop()) {
				context.flush();
				closeInEventLoop();
			} else {
				eventLoop.execute(() -> {
					context.flush();
					closeInEventLoop();
				});
			}
		}
		// 释放响应序化占位（N①）：在cf写出提交之后——挂起写（含endStream终结符）先于后续
		// exchange的响应冲刷。FORCE族丢弃挂起写，FINISH族保留按序送出。
		releaseResponseOrder(method >= CLOSE_FORCE);
	}

	/**
	 * 正常结束本exchange，不关闭连接（keep-alive）。future为本次响应的写出future
	 * （惯用法{@code x.close(x.send(...))}），真实写出完成后才执行收尾；null时框架补一个空写。
	 * 幂等：已结束时二次调用无效果。
	 */
	public void close(@Nullable ChannelFuture future) {
		close(CLOSE_FINISH, future);
	}

	/**
	 * 结束本exchange，发送完成后关闭连接。幂等。
	 */
	public void closeConnectionOnFlush(@Nullable ChannelFuture future) {
		close(CLOSE_ON_FLUSH, future);
	}

	/**
	 * 结束本exchange，不等发送完成立即关闭连接。幂等。
	 */
	public void closeConnectionNow() {
		close(CLOSE_FORCE, null);
	}

	/// //////////////////////////////////////////////////////////////////////////////////////////////////////////
	// send response
	public @NotNull ChannelFuture send(@NotNull HttpResponseStatus status, @Nullable String contentType,
									   @Nullable ByteBuf content) { // content所有权会被转移
		if (content == null)
			content = Unpooled.EMPTY_BUFFER;
		var res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, content, headersFactory, trailersFactory);
		var headers = HttpServer.setDate(res.headers())
			.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
			.set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
		if (contentType != null)
			headers.set(HttpHeaderNames.CONTENT_TYPE, contentType);
		if (resHeaders != null) {
			for (int i = 0, n = resHeaders.size(); i < n; i += 2)
				headers.add((CharSequence)resHeaders.get(i), resHeaders.get(i + 1));
		}
		return writeResponse(res, true, null); // N①：响应经per-channel序化器，pipelining按请求序写出
	}

	public @NotNull ChannelFuture send(@NotNull HttpResponseStatus status, @Nullable String contentType,
									   @Nullable String content) {
		return send(status, contentType, content == null || content.isEmpty() ? Unpooled.EMPTY_BUFFER
			: Unpooled.wrappedBuffer(content.getBytes(HttpServer.defaultCharset)));
	}

	public @NotNull ChannelFuture sendPlainText(@NotNull HttpResponseStatus status, @Nullable String text) {
		return send(status, "text/plain; charset=utf-8", text);
	}

	public @NotNull ChannelFuture sendPlainText(@NotNull HttpResponseStatus status, byte @Nullable [] text) {
		return send(status, "text/plain; charset=utf-8", text == null || text.length == 0 ? Unpooled.EMPTY_BUFFER
			: Unpooled.wrappedBuffer(text));
	}

	public @NotNull ChannelFuture sendHtml(@NotNull HttpResponseStatus status, @Nullable String html) {
		return send(status, "text/html; charset=utf-8", html);
	}

	public @NotNull ChannelFuture sendJson(@NotNull HttpResponseStatus status, @Nullable String json) {
		return send(status, "application/json; charset=utf-8", json);
	}

	public @NotNull ChannelFuture sendJson(@NotNull HttpResponseStatus status, byte @Nullable [] json) {
		return send(status, "application/json; charset=utf-8", json == null || json.length == 0 ? Unpooled.EMPTY_BUFFER
			: Unpooled.wrappedBuffer(json));
	}

	public @NotNull ChannelFuture sendXml(@NotNull HttpResponseStatus status, @Nullable String xml) {
		return send(status, "text/xml; charset=utf-8", xml);
	}

	public void sendFreeMarker(@Nullable Object model) throws Exception {
		var freeMarker = server.getFreeMarker();
		if (freeMarker == null)
			throw new IllegalStateException("FreeMarker not available");
		var t = Transaction.getCurrent();
		if (t != null && t.isRunning()) {
			t.runWhileCommit(() -> {
				try {
					freeMarker.sendResponse(this, model);
				} catch (Exception e) {
					throw Task.forceThrow(e);
				}
			});
		} else
			freeMarker.sendResponse(this, model);
	}

	public void sendThymeleaf(@Nullable Object model) throws Exception {
		Context context;
		if (model instanceof Context)
			context = (Context)model;
		else {
			context = new Context();
			if (model != null)
				context.setVariable("bean", model); // 单个对象时怎么取名？
		}

		var thymeleaf = server.getThymeleaf();
		if (thymeleaf == null)
			throw new IllegalStateException("Thymeleaf not available");
		var t = Transaction.getCurrent();
		if (t != null && t.isRunning()) {
			t.runWhileCommit(() -> {
				try {
					thymeleaf.sendResponse(this, context);
				} catch (Exception e) {
					throw Task.forceThrow(e);
				}
			});
		} else
			thymeleaf.sendResponse(this, context);
	}

	public void sendFile(@NotNull File file) throws Exception {
		HttpFileService.sendFile(this, file, 10 * 60);
	}

	/**
	 * 发送文件：If-Modified-Since命中回304、Range不可满足回416、单段Range回206，
	 * 其余200全量；含缓存头（Expires/Cache-Control/Last-Modified）与FileRegion零拷贝。
	 *
	 * @param fileCacheSeconds 客户端缓存秒数（Expires与max-age）
	 */
	public void sendFile(@NotNull File file, int fileCacheSeconds) throws Exception {
		HttpFileService.sendFile(this, file, fileCacheSeconds);
	}

	public void sendPath(@NotNull File file) {
		HttpFileService.sendPath(this, file);
	}

	public @NotNull ChannelFuture send404() {
		return sendPlainText(HttpResponseStatus.NOT_FOUND, (byte[])null);
	}

	public @NotNull ChannelFuture send500(@NotNull Throwable ex) {
		return sendPlainText(HttpResponseStatus.INTERNAL_SERVER_ERROR, Str.stacktrace(ex));
	}

	public @NotNull ChannelFuture send500(@Nullable String text) {
		return sendPlainText(HttpResponseStatus.INTERNAL_SERVER_ERROR, text);
	}

	public @NotNull ChannelFuture sendWebSocket(@NotNull WebSocketFrame frame) { // frame所有权会被转移
		return context.writeAndFlush(frame);
	}

	public @NotNull ChannelFuture sendWebSocket(@NotNull String text) {
		return context.writeAndFlush(new TextWebSocketFrame(text));
	}

	public @NotNull ChannelFuture sendWebSocket(byte @NotNull [] data) {
		return context.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(data, 0, data.length)));
	}

	public @NotNull ChannelFuture sendWebSocket(byte @NotNull [] data, int offset, int count) {
		return context.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(data, offset, count)));
	}

	/// //////////////////////////////////////////////////////////////////////////////////////////////
	// 流接口功能最大化，不做任何校验：状态校验，不正确的流起始Response（headers）等。
	// 高吞吐大流量流式发送建议按块检查isWritable()（背压信号，越过水位时等待积压排出再继续）。

	/**
	 * 流式响应开始：写出状态行与headers（未设Content-Length则自动Transfer-Encoding: chunked）。
	 * 本族接口功能最大化、不做校验（状态/头部正确性自负）。后续用{@link #sendStream}发块、
	 * {@link #endStream}收尾；高吞吐大流量建议按块检查{@link #isWritable()}（背压）。
	 */
	public @NotNull ChannelFuture beginStream(@NotNull HttpResponseStatus status, @NotNull HttpHeaders headers) {
		if (!headers.contains(HttpHeaderNames.CONTENT_LENGTH))
			headers.set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
		return writeResponse(new DefaultHttpResponse(HttpVersion.HTTP_1_1, status, headers), true, null); // N①
	}

	// 发送后data内容在回调前不能修改（所有权转移）
	public @NotNull ChannelFuture sendStream(@NotNull ByteBuf data) {
		return writeResponse(new DefaultHttpContent(data), true, null); // N①
	}

	// 发送后data内容在回调前不能修改
	public @NotNull ChannelFuture sendStream(byte @NotNull [] data) {
		return writeResponse(new DefaultHttpContent(Unpooled.wrappedBuffer(data, 0, data.length)), true, null); // N①
	}

	// 发送后data内容在回调前不能修改
	public @NotNull ChannelFuture sendStream(byte @NotNull [] data, int offset, int count) {
		return writeResponse(new DefaultHttpContent(Unpooled.wrappedBuffer(data, offset, count)), true, null); // N①
	}

	// 发送后data内容在回调前不能修改
	public @NotNull ChannelFuture sendStream(@NotNull Binary b) {
		return writeResponse(new DefaultHttpContent(Unpooled.wrappedBuffer(b.bytesUnsafe(), b.getOffset(), b.size())),
			true, null); // N①
	}

	// 发送后data内容在回调前不能修改
	public @NotNull ChannelFuture sendStream(@NotNull ByteBuffer bb) {
		return writeResponse(new DefaultHttpContent(Unpooled.wrappedBuffer(bb.Bytes, bb.ReadIndex, bb.size())),
			true, null); // N①
	}

	// 发送后data内容在回调前不能修改
	public @NotNull ChannelFuture sendStream(@NotNull java.nio.ByteBuffer bb) {
		return writeResponse(new DefaultHttpContent(Unpooled.wrappedBuffer(bb)), true, null); // N①
	}

	/**
	 * 流式响应终结符（chunked的0\r\n\r\n / 固定长度的空结尾）并结束exchange。
	 * 幂等：已结束时二次调用no-op（FND7-25——二次终结符会被客户端当作下一响应的前缀垃圾）。
	 * 终结符经序化器按请求到达序写出。
	 */
	public void endStream() {
		if ((int)detachedHandle.getAndSet(this, 2) == 2)
			return;
		server.exchanges.remove(context.channel().id(), this);
		// 终结符经序化器——本exchange未持笔（前面请求的响应未完成）时挂起，release按请求序冲刷；
		// closeInEventLoop挂在桥接promise上，真实写出完成时兑现（非pipelining时同步直达，行为不变）。
		writeResponse(LastHttpContent.EMPTY_LAST_CONTENT, true, null).addListener(__ -> closeInEventLoop());
		releaseResponseOrder(false);
	}
}
