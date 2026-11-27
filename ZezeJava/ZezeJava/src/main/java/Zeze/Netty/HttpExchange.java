package Zeze.Netty;

import java.util.Objects;
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
import Zeze.Transaction.EmptyBean;
import Zeze.Transaction.TransactionLog;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Action0;
import Zeze.Util.Str;
import Zeze.Util.Task;
import Zeze.Util.TaskSpec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
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
import io.netty.handler.stream.ChunkedInput;
import io.netty.util.AttributeKey;
import io.netty.util.AttributeMap;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.collection.LongObjectHashMap;
import org.jspecify.annotations.Nullable;
import org.thymeleaf.context.Context;
import Zeze.Netty.HttpSession.CookieSession;

/**
 * 单次 HTTP 请求的交换上下文：承接请求接收（缓冲/流式/WebSocket）、处理器派发、
 * 响应按序写出（HTTP/1.1 pipelining 序化）与资源回收。
 * handler 返回且未 {@link #detach()} 时自动关闭；detach 后由调用方在任意线程补发响应并关闭。
 */
public class HttpExchange {
	protected static final int CLOSE_FINISH = 0; // 正常结束HttpExchange,不关闭连接
	protected static final int CLOSE_ON_FLUSH = 1; // 结束HttpExchange,发送完时关闭连接
	protected static final int CLOSE_FORCE = 2; // 结束HttpExchange,不等发送完强制关闭连接
	protected static final int CLOSE_TIMEOUT = 3; // 同上,只是因idle超时而关闭
	protected static final int CLOSE_PASSIVE = 4; // 同上,只是因远程主动关闭而关闭

	protected static final VarHandle detachedHandle;
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

	protected final HttpServer server; // 所属的HttpServer对象,每个对象管理监听端口的所有连接
	protected final ChannelHandlerContext context; // netty的连接上下文,每个连接可能会依次绑定到多个HttpExchange对象
	protected volatile @Nullable HttpRequest request; // 收到完整HTTP header部分会赋值
	protected @Nullable HttpHandler handler; // 收到完整HTTP header部分会查找对应handler并赋值
	protected volatile ByteBuf content = Unpooled.EMPTY_BUFFER; // 当前收集的HTTP body部分, 只用于非流模式
	protected @Nullable Object userState;
	protected @Nullable ArrayList<Object> resHeaders; // key,value,key,value,...
	private @Nullable EmptyBean responseHeadersBean; // 仅事务内修改header时分配独立objectId，this守护

	private final class HeadersLog implements TransactionLog {
		private final EmptyBean owner;

		@Override
		public EmptyBean getBelong() {
			return owner;
		}

		@Override
		public int getVariableId() {
			return 0;
		}

		final ArrayList<Object> headers;
		@Nullable String sessionCookie;
		@Nullable DeferredResponse response;
		@Nullable Action0 deferredAction;
		@Nullable Integer closeMethod;
		@Nullable ChannelFuture closeFuture;

		HeadersLog(ArrayList<Object> headers, @Nullable String sessionCookie) {
			owner = Objects.requireNonNull(responseHeadersBean);
			this.headers = headers;
			this.sessionCookie = sessionCookie;
		}

		@Override
		public TransactionLog beginSavepoint() {
			var copy = new HeadersLog(new ArrayList<>(headers), sessionCookie);
			copy.response = response;
			copy.deferredAction = deferredAction;
			copy.closeMethod = closeMethod;
			copy.closeFuture = closeFuture;
			return copy;
		}

		@Override
		public void commit() {
			synchronized (HttpExchange.this) {
				resHeaders = new ArrayList<>(headers);
				sessionCookieHeader = sessionCookie;
				committedHeadersLog = this;
			}
		}
	}

	// 调用方持有this。header的增加、会话cookie替换和删除共用同一个保存点视图。
	private HeadersLog headersLog(Transaction t) {
		if (responseHeadersBean == null)
			responseHeadersBean = new EmptyBean();
		var log = (HeadersLog)t.getLog(responseHeadersBean.objectId());
		if (log == null) {
			log = new HeadersLog(resHeaders != null ? new ArrayList<>(resHeaders) : new ArrayList<>(), sessionCookieHeader);
			t.putLog(log);
			// 只登记一次。保存点commit发布最终Log，回调统一先写最终响应、再兑现关闭意图。
			t.runWhileCommit(this::publishTransactionalOutput);
		}
		return log;
	}

	private record DeferredResponse(HttpVersion version, HttpResponseStatus status, byte[] body,
			HttpHeaders headers, HttpHeaders trailers, boolean flush, ChannelPromise promise) {
	}

	private @Nullable HeadersLog committedHeadersLog; // this守护，仅Log.commit发布
	private final ArrayList<ChannelPromise> deferredResponsePromises = new ArrayList<>(); // 包含redo丢弃的尝试

	private void publishTransactionalOutput() {
		HeadersLog log;
		ArrayList<ChannelPromise> abandoned;
		synchronized (this) {
			log = committedHeadersLog;
			committedHeadersLog = null;
			if (log == null)
				return;
			abandoned = new ArrayList<>(deferredResponsePromises);
			deferredResponsePromises.clear();
			if (log.response != null)
				abandoned.remove(log.response.promise);
		}
		for (var promise : abandoned)
			promise.tryFailure(new IllegalStateException("HTTP response superseded by committed transaction"));
		ChannelFuture future = log.closeFuture;
		var response = log.response;
		var deferredAction = log.deferredAction;
		try {
			if (deferredAction != null) {
				future = null;
				deferredAction.run();
			} else if (response != null)
				future = writeResponse(new DefaultFullHttpResponse(response.version, response.status,
						Unpooled.wrappedBuffer(response.body), response.headers, response.trailers), response.flush, response.promise);
		} catch (Exception e) {
			throw Task.forceThrow(e);
		} finally {
			if (log.closeMethod != null)
				close(log.closeMethod, future);
		}
	}
	protected @Nullable List<Cookie> cookies;
	protected volatile @Nullable CookieSession cookieSession;
	protected @Nullable String path;
	protected volatile @SuppressWarnings("unused") int detached; // 0:not detached; 1:detached; 2:detached and closed
	// end-stream 任务在途期间禁止 close 抢先释放 request/content，由任务 finally 或 onCancel 接管终态释放。
	protected volatile boolean endStreamTaskPending;
	// 在途的流式 begin/content 任务计数（this 守护），归零前禁止终态释放。
	private int streamTasks;
	protected boolean willCloseConnection; // true表示close时会关闭连接
	protected boolean inStreamMode; // 是否在流/WebSocket模式过程中
	protected volatile boolean streamAborted; // 流模式在收到LastHttpContent前被中止（超限/断连/超时/畸形帧）
	protected boolean isWebSocketTextContent;
	protected long streamContentTotal; // 流模式累计收到的请求body字节数,server.maxUploadSize总量上限检查用
	// WebSocket 分片消息累计字节数（按 channel.id 串行访问，首帧重置、续帧累加、终帧清零）。
	protected long webSocketContentTotal;

	public HttpExchange(HttpServer server, ChannelHandlerContext context) {
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

	// 延迟到业务线程首次访问时初始化，避免在 EventLoop 阻塞 DB 事务；未启用 httpSession 返回 null。
	public @Nullable CookieSession getCookieSession() {
		var cs = cookieSession;
		var t = Transaction.getCurrent();
		if (cs != null && (t == null || !t.isRunning()))
			return cs;
		var httpSession = server.getHttpSession();
		if (httpSession == null)
			return null;
		try {
			synchronized (this) {
				// 事务 redo 时需按当前事务视图复核会话行是否存在。
				//noinspection DataFlowIssue
				if (cookieSession == null || t != null && t.isRunning() && !cookieSession.existsInCurrentTransaction()) {
					cookieSession = httpSession.getCookieSession(this);
					if (t != null && t.isRunning()) {
						var initialized = cookieSession;
						t.runWhileRollback(() -> {
							synchronized (this) {
								if (cookieSession == initialized)
									cookieSession = null;
							}
						});
					}
				}
			}
			return cookieSession;
		} catch (Exception e) {
			throw Task.forceThrow(e);
		}
	}

	private @Nullable String sessionCookieHeader;

	void setSessionCookieHeader(String value) {
		synchronized (this) {
			var t = Transaction.getCurrent();
			if (t != null && t.isRunning()) {
				var log = headersLog(t);
				removeSessionCookieHeader(log.sessionCookie);
				log.headers.add(HttpHeaderNames.SET_COOKIE);
				log.headers.add(value);
				log.sessionCookie = value;
			} else {
				removeSessionCookieHeader(sessionCookieHeader);
				addHeader(HttpHeaderNames.SET_COOKIE, value);
				sessionCookieHeader = value;
			}
		}
	}

	private synchronized void removeSessionCookieHeader(@Nullable String expected) {
		if (expected == null)
			return;
		var t = Transaction.getCurrent();
		ArrayList<Object> headers;
		if (t != null && t.isRunning()) {
			var log = headersLog(t);
			if (!Objects.equals(log.sessionCookie, expected))
				return;
			headers = log.headers;
			log.sessionCookie = null;
		} else {
			if (!Objects.equals(sessionCookieHeader, expected))
				return;
			headers = resHeaders;
			sessionCookieHeader = null;
		}
		if (headers != null) {
			for (int i = headers.size() - 2; i >= 0; i -= 2) {
				if (headers.get(i + 1) == expected
						&& HttpHeaderNames.SET_COOKIE.contentEqualsIgnoreCase((CharSequence)headers.get(i))) {
					headers.remove(i + 1);
					headers.remove(i);
					break;
				}
			}
		}
	}

	/**
	 * @param key 建议从Netty的HttpHeaderNames类里取字符串常量
	 */
	public synchronized void addHeader(CharSequence key, Object value) {
		var t = Transaction.getCurrent();
		ArrayList<Object> headers;
		if (t != null && t.isRunning()) {
			headers = headersLog(t).headers;
		} else {
			headers = resHeaders;
			if (headers == null)
				resHeaders = headers = new ArrayList<>();
		}
		headers.add(key);
		headers.add(value);
	}

	private synchronized ArrayList<Object> responseHeaders() {
		var t = Transaction.getCurrent();
		if (t != null && t.isRunning() && responseHeadersBean != null) {
			var log = (HeadersLog)t.getLog(responseHeadersBean.objectId());
			if (log != null)
				return new ArrayList<>(log.headers);
		}
		return resHeaders != null ? new ArrayList<>(resHeaders) : new ArrayList<>();
	}

	void appendResponseHeaders(HttpHeaders headers) {
		var extraHeaders = responseHeaders();
		for (int i = 0, n = extraHeaders.size(); i < n; i += 2)
			headers.add((CharSequence)extraHeaders.get(i), extraHeaders.get(i + 1));
	}

	public void addCookie(String name, String value) {
		setCookie(name, value, null, null, -1);
	}

	public void removeCookie(String name) {
		setCookie(name, "", null, null, 0);
	}

	/**
	 * @param domain 域名. 可以是全的,也可以是跨域的,如".example.com". 默认是当前请求的域名(HOST)
	 * @param path   路径. 如"/","/path/". 默认是当前请求路径到最后一个"/"的部分
	 * @param maxAge 有效期. 小于0表示浏览器(内存)生命期(默认);0表示删除;大于0表示有效时长(秒)
	 */
	public void setCookie(String name, String value,
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
	public List<Cookie> getCookieList() {
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

	public @Nullable String getCookie(String name) {
		var cookies = getCookieList();
		if (!cookies.isEmpty()) {
			for (var cookie : cookies) {
				if (cookie.name().equals(name))
					return cookie.value();
			}
		}
		return null;
	}

	public boolean hasRequest() {
		return request != null;
	}

	public boolean isClosed() {
		return detached == 2;
	}

	/** 返回流式请求是否在收到 {@code LastHttpContent} 前被中止。 */
	public boolean isStreamAborted() {
		return streamAborted;
	}

	/**
	 * 获取底层 Netty 上下文（仅限特殊底层需求；绕过响应序化器直写响应头会被出站检查拒绝）。
	 */
	public ChannelHandlerContext context() {
		return context;
	}

	public Channel channel() {
		return context.channel();
	}

	/** 返回出站缓冲是否低于写水位，流式大响应可据此做背压控制。 */
	public boolean isWritable() {
		return context.channel().isWritable();
	}

	public AttributeMap attributes() {
		return context.channel();
	}

	public @Nullable HttpRequest request() {
		return request;
	}

	public ByteBuf content() {
		return content;
	}

	public String contentString() {
		return content.toString(HttpServer.defaultCharset);
	}

	public byte [] contentBytes() {
		var c = content;
		var size = c.readableBytes();
		if (size <= 0)
			return ByteBuffer.Empty;
		// 仅当底层数组与有效内容完全重合时直返，避免暴露池化缓冲尾部残留字节。
		if (c.readerIndex() == 0 && c.hasArray() && c.arrayOffset() == 0 && size == c.array().length)
			return c.array();
		var buf = new byte[size];
		c.getBytes(c.readerIndex(), buf);
		return buf;
	}

	// 用于 close 前提前释放 content；锁内原子摘取防与终态释放双释放，release 在锁外。
	public void releaseContent() {
		ByteBuf c;
		synchronized (this) {
			c = content;
			if (c == Unpooled.EMPTY_BUFFER)
				return;
			content = Unpooled.EMPTY_BUFFER;
		}
		c.release();
	}

	public HttpPostMultipartRequestDecoder contentMultipart() {
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

	public static String getMultipartString(HttpPostMultipartRequestDecoder multipart,
													 String key) {
		var httpData = multipart.getBodyHttpData(key);
		return httpData instanceof MemoryAttribute ? ((MemoryAttribute)httpData).getValue() : "";
	}

	public static byte [] getMultipartBytes(HttpPostMultipartRequestDecoder multipart,
													 String key) {
		var httpData = multipart.getBodyHttpData(key);
		return httpData instanceof MemoryAttribute ? ((MemoryAttribute)httpData).get() : ByteBuffer.Empty;
	}

	public static byte [] getMultipartFile(HttpPostMultipartRequestDecoder multipart,
													String key) {
		var httpData = multipart.getBodyHttpData(key);
		return httpData instanceof MemoryFileUpload ? ((MemoryFileUpload)httpData).get() : ByteBuffer.Empty;
	}

	public static @Nullable FileUpload getMultipartFileUpload(HttpPostMultipartRequestDecoder multipart,
															  String key) {
		var httpData = multipart.getBodyHttpData(key);
		return httpData instanceof MemoryFileUpload ? (MemoryFileUpload)httpData : null;
	}

	public static String urlDecode(String s) {
		for (int i = 0, n = s.length(); i < n; i++) {
			var c = s.charAt(i);
			if (c == '%' || c == '+')
				return URLDecoder.decode(s, HttpServer.defaultCharset);
		}
		return s;
	}

	// RFC 3986：path 段仅做 %XX 解码且保留字面量 '+'，十六进制严格限定 ASCII，非法抛 IllegalArgumentException。
	private static String pathDecode(String s) {
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

	public String path() {
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

	public static Map<String, String> parseQuery(@Nullable String s) {
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

	public Map<String, String> queryMap() {
		return parseQuery(query());
	}

	public Map<String, String> contentQueryMap() {
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
			if (handler.isWebSocketMode()) {
				if (isH2()) { // WebSocket over h2 暂不支持
					closeConnectionOnFlush(writeResponse(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
							HttpResponseStatus.NOT_IMPLEMENTED, Unpooled.EMPTY_BUFFER,
							headersFactory, trailersFactory), true, null));
					return;
				}
				if (context.pipeline().<@Nullable WebSocketServerProtocolHandler>get(WebSocketServerProtocolHandler.class) == null) {
					context.pipeline().addLast(new WebSocketServerProtocolHandler(WebSocketServerProtocolConfig.newBuilder()
							.websocketPath(path).decoderConfig(WebSocketDecoderConfig.newBuilder().withUTF8Validator(false)
									.maxFramePayloadLength(handler.MaxContentLength).build()).build()));
					// 在管线末尾监听 HandshakeComplete 再派发 onOpen，确保 101 已写出、帧编码器已就位且先于客户端帧。
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
			}
			if (handler.isStreamMode()) {
				fireBeginStream(req);
				if (isClosed())
					return;
				inStreamMode = true;
			}
			if (!(msg instanceof HttpContent)) {
				if (!isH2() && HttpUtil.is100ContinueExpected(req)) { // h2 不使用 100-continue
					if (!handler.isStreamMode() && HttpUtil.getContentLength(req, 0) > handler.MaxContentLength) {
						closeConnectionOnFlush(writeResponse(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
								HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE, Unpooled.EMPTY_BUFFER,
								headersFactory, trailersFactory), true, null));
						return;
					}
					writeResponse(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE,
							Unpooled.EMPTY_BUFFER, headersFactory, trailersFactory), true, context.voidPromise());
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
		} else if (request == null || handler == null) // 缺失上文的msg,可能很罕见,忽略
			return;

		var c = (HttpContent)msg;
		var b = c.content();
		var n = b.readableBytes();
		if (n > 0) {
			if (handler.isStreamMode()) {
				// 流模式用 server.maxUploadSize 兜底请求体总量，超限回 413 并断连。
				streamContentTotal += n;
				var maxUploadSize = server.getMaxUploadSize();
				if (streamContentTotal > maxUploadSize) {
					Netty.logger.error("upload size = {} > {} from {}", streamContentTotal, maxUploadSize,
							channel.remoteAddress());
					closeConnectionOnFlush(writeResponse(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
							HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE, Unpooled.EMPTY_BUFFER,
							headersFactory, trailersFactory), true, null));
					return;
				}
				fireStreamContentHandle(c);
				if (isClosed())
					return;
			} else {
				if (content.readableBytes() + n > handler.MaxContentLength) {
					Netty.logger.error("content size = {} + {} > {} from {}", content.readableBytes(), n,
							handler.MaxContentLength, channel.remoteAddress());
					closeConnectionOnFlush(writeResponse(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
							HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE, Unpooled.EMPTY_BUFFER,
							headersFactory, trailersFactory), true, null));
					return;
				}
				addContent(b.retain());
			}
		}
		// 空终结帧仍携带流结束语义（multipart decoder据此允许读取完整字段列表）。
		if (n == 0 && c instanceof LastHttpContent && handler.isStreamMode()) {
			fireStreamContentHandle(c);
			if (isClosed())
				return;
		}
		if (c instanceof LastHttpContent) {
			inStreamMode = false;
			fireEndStreamHandle(); // 流模式和非流模式通用
		}
	}

	// newContent会被转移所有权,调用者需要继续用的话,应该retain一次再传. 返回当前累积的content
	public ByteBuf addContent(ByteBuf newContent) {
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
	protected void fireBeginStream(HttpRequest req) {
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
			else {
				retainStreamTask();
				TaskSpec.ofFunc(() -> {
					try {
						return p.call();
					} finally {
						releaseStreamTask();
					}
				}).name(p.getActionName()).onCancel(this::releaseStreamTask)
						.dispatchMode(handler.Mode).executeOneByOne(context.channel().id(), server.task11Executor);
			}
		} else if (handler.Mode == DispatchMode.Direct) {
			TaskSpec.ofAction(() -> handler.BeginStreamHandle.onBeginStream(this, r[0], r[1], r[2]))
					.name("fireBeginStream").call();
		} else {
			retainStreamTask();
			TaskSpec.ofAction(() -> {
				try {
					handler.BeginStreamHandle.onBeginStream(this, r[0], r[1], r[2]);
				} finally {
					releaseStreamTask();
				}
			}).name("fireBeginStream").dispatchMode(handler.Mode).onCancel(this::releaseStreamTask)
					.executeOneByOne(context.channel().id(), server.task11Executor);
		}
	}

	protected void fireStreamContentHandle(HttpContent c) {
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
				// shutdown 丢弃未运行任务时由 onCancel 补偿释放 retain 的池化 content。
				c.retain();
				retainStreamTask();
				TaskSpec.ofFunc(() -> {
					try {
						return p.call();
					} finally {
						c.release();
						releaseStreamTask();
					}
				}).name(p.getActionName()).dispatchMode(handler.Mode).onCancel(() -> {
					c.release();
					releaseStreamTask();
				})
						.executeOneByOne(context.channel().id(), server.task11Executor);
			}
		} else if (handler.Mode == DispatchMode.Direct) {
			TaskSpec.ofAction(() -> handle.onStreamContent(this, c)).name("fireStreamContentHandle").call();
		} else {
			c.retain();
			retainStreamTask();
			TaskSpec.ofAction(() -> {
				try {
					handle.onStreamContent(this, c);
				} finally {
					c.release();
					releaseStreamTask();
				}
			}).name("fireStreamContentHandle").dispatchMode(handler.Mode).onCancel(() -> {
				c.release();
				releaseStreamTask();
			})
					.executeOneByOne(context.channel().id(), server.task11Executor);
		}
	}

	private synchronized void retainStreamTask() {
		streamTasks++;
	}

	private void releaseStreamTask() {
		synchronized (this) {
			if (--streamTasks != 0)
				return;
		}
		if (detached == 2)
			releaseTerminalFromAnyThread();
	}

	/**
	 * 声明本 exchange 生命周期由调用方接管：handler 返回后框架不再自动关闭，由调用方自行发送响应并 close。
	 */
	public HttpExchange detach() {
		detachedHandle.compareAndSet(this, 0, 1);
		return this;
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// HTTP/1.1 pipelining 响应序化（仅 channel EventLoop 单写者访问）：
	// 按请求到达序分配递增 orderId；当前持笔（orderId == writingOrderId）直写，后序请求挂起，
	// 持笔方 close/endStream 后冲刷挂起并链式推进到下一个存活条目；迟到写直接失败并释放。
	static final AttributeKey<@Nullable ResponseSequencer> responseOrderKey = AttributeKey.valueOf("ZezeHttpResponseOrder");
	private static final int MaxPendingResponseWrites = 256; // 单 exchange 挂起写上限
	private static final int MaxResponseOrderDepth = 128; // 每 channel 在途请求序深度上限

	static final class ResponseSequencer {
		long nextOrderId = 1;
		long writingOrderId = 1;
		final LongObjectHashMap<@Nullable OrderEntry> entries = new LongObjectHashMap<>();
	}

	static final class OrderEntry {
		final HttpExchange x;
	@Nullable ArrayDeque<DeferredWrite> pending;
		boolean finished;
		boolean started; // 出站序化检查依据：标记本条目已开始按序写出

		OrderEntry(HttpExchange x) {
			this.x = x;
		}
	}

	/**
	 * @param promise 调用方给定的promise（可为voidPromise），提交时原样传递
	 */
	record DeferredWrite(Object msg, ChannelPromise promise) {
	}

	// 响应序化状态（仅 EventLoop 线程访问）
	@Nullable ResponseSequencer responseSequencer;
	long responseOrderId;
	private @Nullable OrderEntry responseEntry;

	// 框架直写豁免标志（如 rejectAndClose 写 400/503 后随即关连接）。
	static final AttributeKey<@Nullable Boolean> responseOrderBypassKey = AttributeKey.valueOf("ZezeHttpResponseOrderBypass");

	// h2 子 channel 标志（每 stream 独立子 channel，跳过 h1 专属分支）。
	static final AttributeKey<@Nullable Boolean> h2StreamKey = AttributeKey.valueOf("ZezeHttpH2Stream");

	public boolean isH2() {
		return context.channel().attr(h2StreamKey).get() != null;
	}

	// 出站序化检查（由 HttpServer encoder write 钩子调用）：拒绝绕过 writeResponse 直写 ctx 的响应头。
	static void checkResponseOrder(Channel ch, Object msg) {
		if (!(msg instanceof HttpResponse))
			return;
		var seq = ch.attr(responseOrderKey).get();
		if (seq == null)
			return;
		if (ch.pipeline().<@Nullable WebSocketServerProtocolHandler>get(WebSocketServerProtocolHandler.class) != null)
			return;
		if (ch.attr(responseOrderBypassKey).get() != null)
			return;
		var e = seq.entries.get(seq.writingOrderId);
		if (e != null && !e.started)
			throw new IllegalStateException("http response written out of order from " + ch.remoteAddress()
				+ ": sequencer head(order=" + seq.writingOrderId + ") not started, bypass writeResponse?");
	}

	// 登记新请求到达序（仅 EventLoop 调用）；深度超限时关闭连接并返回 false，调用方不得再处理本请求。
	boolean registerResponseOrder() {
		var ch = context.channel();
		var seq = ch.attr(responseOrderKey).get();
		if (seq == null) {
			var created = new ResponseSequencer();
			var prev = ch.attr(responseOrderKey).setIfAbsent(created);
			seq = prev != null ? prev : created;
		}
		if (seq.entries.size() >= MaxResponseOrderDepth) {
			Netty.logger.error("too many in-flight pipelined exchanges: {} from {}, close connection",
					MaxResponseOrderDepth, ch.remoteAddress());
			closeConnectionNow();
			return false;
		}
		responseOrderId = seq.nextOrderId++;
			responseSequencer = seq;
		responseEntry = new OrderEntry(this);
		seq.entries.put(responseOrderId, responseEntry);
		return true;
	}

	// 关闭 channel 上全部在途 exchange（含 pipelining 下已被新请求覆盖出 exchanges 表的前序请求）。
	static boolean closeInFlightExchanges(Channel ch, int method) {
		var seq = ch.attr(responseOrderKey).get();
		if (seq == null)
			return false;
		var loop = ch.eventLoop();
		if (loop.inEventLoop())
			return closeInFlightExchanges0(seq, method);
		try {
			loop.execute(() -> closeInFlightExchanges0(seq, method));
		} catch (RejectedExecutionException ignored) {
			loop.terminationFuture().addListener(__ -> cleanupTerminatedSequencer(seq));
		}
		return false;
	}

	// 仅 EventLoop 调用：先快照再逐个 close（close 释放会修改 entries）。
	private static boolean closeInFlightExchanges0(ResponseSequencer seq, int method) {
		if (seq.entries.isEmpty())
			return false;
		var inFlight = seq.entries.values().toArray(new OrderEntry[0]);
		for (var e : inFlight)
			e.x.close(method, null);
		return true;
	}

	// 响应写统一入口（经 EventLoop 单写者按序写出；事务内全量响应延迟至提交时发送）。
	ChannelFuture writeResponse(Object msg, boolean flush, @Nullable ChannelPromise promise) {
		if (promise == null)
			promise = context.newPromise();
		var t = Transaction.getCurrent();
		if (t != null && t.isRunning()) {
			if (!(msg instanceof FullHttpResponse response)) {
				releaseResponseMessage(msg);
				throw new IllegalStateException("HTTP streaming/file responses require TransactionLevel.None");
			}
			return deferFullResponse(t, response, flush, promise);
		}
		var finalPromise = promise;
		//noinspection resource
		var loop = context.channel().eventLoop();
		if (loop.inEventLoop())
			writeResponse0(msg, flush, finalPromise);
		else {
			try {
				loop.execute(() -> writeResponse0(msg, flush, finalPromise));
			} catch (RejectedExecutionException e) {
				finalPromise.tryFailure(e);
				releaseResponseMessage(msg);
			}
		}
		return finalPromise;
	}

	private ChannelFuture deferFullResponse(Transaction t, FullHttpResponse response,
			boolean flush, ChannelPromise promise) {
		// 拷贝堆快照并释放池化 ByteBuf，避免事务 redo/回滚时泄漏池化内存。
		byte[] body;
		HttpHeaders headers;
		HttpHeaders trailers;
		var version = response.protocolVersion();
		var status = response.status();
		try {
			body = ByteBufUtil.getBytes(response.content());
			headers = response.headers().copy();
			trailers = response.trailingHeaders().copy();
		} finally {
			ReferenceCountUtil.release(response);
		}
		synchronized (this) {
			var log = headersLog(t);
			log.response = new DeferredResponse(version, status, body, headers, trailers, flush, promise);
			log.deferredAction = null;
			deferredResponsePromises.add(promise);
		}
		t.runWhileRollback(() -> {
			synchronized (this) {
				deferredResponsePromises.remove(promise);
			}
			promise.tryFailure(new IllegalStateException("HTTP handler transaction rolled back"));
		});
		return promise;
	}

	static void requireNonTransactionalStreaming() {
		var t = Transaction.getCurrent();
		if (t != null && t.isRunning())
			throw new IllegalStateException("HTTP streaming/file responses require TransactionLevel.None");
	}

	// 仅 EventLoop 调用：按到达序直写、挂起或拒绝迟到写。
	@SuppressWarnings("ConstantConditions")
	private void writeResponse0(Object msg, boolean flush, ChannelPromise promise) {
		var seq = responseSequencer;
		if (seq == null) {
			submit(msg, promise, flush);
			return;
		}
		if (responseOrderId < seq.writingOrderId) {
				Netty.logger.warn("late response write after exchange finished: {}", context.channel().remoteAddress());
			promise.tryFailure(new ClosedChannelException());
			releaseResponseMessage(msg);
			return;
			}
		if (responseOrderId > seq.writingOrderId) {
			var e = responseEntry;
			if (e.finished) {
				Netty.logger.warn("late response write after exchange finished: {}", context.channel().remoteAddress());
				promise.tryFailure(new ClosedChannelException());
				releaseResponseMessage(msg);
				return;
			}
			var pending = e.pending != null ? e.pending : (e.pending = new ArrayDeque<>());
			pending.addLast(new DeferredWrite(msg, promise));
			if (pending.size() >= MaxPendingResponseWrites) {
				Netty.logger.error("too many pending response writes: {} from {}, close connection",
						pending.size(), context.channel().remoteAddress());
				closeConnectionNow();
			}
			return;
		}
		responseEntry.started = true; // 先于 submit 置位，供编码器出站检查确认
		submit(msg, promise, flush);
	}

	// 仅 EventLoop 调用：写出消息，同步异常时失败 promise 并释放消息。
	private ChannelFuture submit(Object msg, ChannelPromise promise, boolean flush) {
		try {
			return flush ? context.writeAndFlush(msg, promise) : context.write(msg, promise);
		} catch (Throwable e) {
			promise.tryFailure(e);
			releaseResponseMessage(msg);
			return promise;
		}
	}

	// 标记本条目完成并推进序化队列（dropPending=true 时丢弃挂起写，用于强制关闭）。
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
			cleanupAfterEventLoopTermination();
		}
	}

	// EventLoop 拒绝提交时，等 EventLoop 完全终止后再清理其单写者状态与挂起消息。
	private void cleanupAfterEventLoopTermination() {
		context.channel().eventLoop().terminationFuture().addListener(__ -> {
			cleanupTerminatedExchange();
			var seq = responseSequencer;
			if (seq != null)
				cleanupTerminatedSequencer(seq);
		});
	}

	private static void cleanupTerminatedSequencer(ResponseSequencer seq) {
		var entries = seq.entries.values().toArray(new OrderEntry[0]);
		seq.entries.clear();
		for (var entry : entries) {
			entry.x.responseEntry = null;
			entry.x.cleanupTerminatedExchange();
		}
		for (var entry : entries)
			dropPending(entry);
	}

	private void cleanupTerminatedExchange() {
		detachedHandle.setVolatile(this, 2);
		server.exchanges.remove(context.channel().id(), this);
		abandonDeferredResponses();
		// 停机兜底线程不执行用户回调，仅回收框架资源。
		inStreamMode = false;
		if (!endStreamTaskPending)
			releaseTerminal();
	}

	private static void dropPending(OrderEntry entry) {
		var pending = entry.pending;
		entry.pending = null;
		if (pending == null)
			return;
		while (!pending.isEmpty()) {
			var write = pending.removeFirst();
			releaseResponseMessage(write.msg());
			write.promise().tryFailure(new ClosedChannelException());
		}
	}

	private static void releaseResponseMessage(Object message) {
		if (message instanceof ChunkedInput<?> input) {
			try {
				input.close();
			} catch (Exception e) {
				Netty.logger.warn("close pending HTTP input failed", e);
			}
		} else
			ReferenceCountUtil.release(message);
	}

	// 仅EventLoop线程调用
	private void releaseResponseOrder0(boolean dropPending) {
		var seq = responseSequencer;
		var e = responseEntry;
		if (seq == null || e == null)
			return;
		if (dropPending)
			dropPending(e);
		e.finished = true;
		if (responseOrderId == seq.writingOrderId)
			advance(seq);
	}

	// 仅 EventLoop 调用：冲刷已完成的头部条目并链式推进到下一个存活条目。
	private static void advance(ResponseSequencer seq) {
		for (; ; ) {
			var e = seq.entries.get(seq.writingOrderId);
			if (e == null)
				return;
			drainPending(e);
			if (!e.finished)
				return;
			seq.entries.remove(seq.writingOrderId);
			e.x.responseEntry = null;
			seq.writingOrderId++;
		}
	}

	// 仅 EventLoop 调用：冲刷条目挂起写，末尾合并单次 flush。
	private static void drainPending(OrderEntry e) {
		var pending = e.pending;
		if (pending == null || pending.isEmpty())
			return;
		e.pending = null;
		e.started = true;
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
			finishTerminalTask();
		}
	}

	private void finishTerminalTask() {
		endStreamTaskPending = false;
		if (detached == 2)
			releaseTerminalFromAnyThread();
	}

	// shutdown 丢弃未运行终结任务时的资源补偿：关闭/释放 exchange 与 channel attr 上的上传解码器及临时文件（不触发用户回调）。
	private void onTerminalTaskCancel() {
		endStreamTaskPending = false;
		if (detached == 0)
			close(null);
		else if (detached == 2)
			releaseTerminalFromAnyThread();
		HttpMultipartHandle.destroyChannelDecoder(this);
		HttpFileUploadHandle.releaseChannelFileUpload(this);
	}

	@SuppressWarnings("ConstantConditions")
	protected void fireEndStreamHandle() {
		if (!server.noProcedure && handler.Level != TransactionLevel.None) {
			var p = server.zeze.newProcedure(() -> {
				var handle = handler.EndStreamHandle;
				if (handle != null)
					handle.onEndStream(this);
				return Procedure.Success;
			}, "fireEndStreamHandle");
			if (handler.Mode == DispatchMode.Direct) {
				try {
					TaskSpec.ofFunc(() -> callEndStreamProcedure(p)).name(p.getActionName()).call();
				} finally {
					if (detached == 0)
						close(null);
				}
			} else {
				endStreamTaskPending = true;
				TaskSpec.ofFunc(() -> {
					try {
						return callEndStreamProcedure(p);
					} finally {
						if (detached == 0)
							close(null);
						finishTerminalTask();
					}
				}).name(p.getActionName()).dispatchMode(handler.Mode).onCancel(this::onTerminalTaskCancel)
						.executeOneByOne(context.channel().id(), server.task11Executor);
			}
		} else if (handler.Mode == DispatchMode.Direct) {
			TaskSpec.ofAction(this::invokeEndStream).name("fireEndStreamHandle").call();
		} else {
			endStreamTaskPending = true;
			TaskSpec.ofAction(this::invokeEndStream)
					.name("fireEndStreamHandle").dispatchMode(handler.Mode).onCancel(this::onTerminalTaskCancel)
					.executeOneByOne(context.channel().id(), server.task11Executor);
		}
	}

	@SuppressWarnings("ConstantConditions")
	protected void fireAbortStreamHandle() {
		if (handler == null)
			return;
		var handle = handler.AbortStreamHandle;
		if (handle == null) {
			// 未注册独立中止回调时回退调用 onEndStream（此时 isStreamAborted 为 true）以便释放资源。
			fireEndStreamHandle();
			return;
		}
		if (!server.noProcedure && handler.Level != TransactionLevel.None) {
			var p = server.zeze.newProcedure(() -> {
				handle.onAbortStream(this);
				return Procedure.Success;
			}, "fireAbortStreamHandle");
			if (handler.Mode == DispatchMode.Direct) {
				TaskSpec.ofProcedure(p).call();
			} else {
				endStreamTaskPending = true;
				TaskSpec.ofFunc(() -> {
					try {
						return p.call();
					} finally {
						finishTerminalTask();
					}
				}).name(p.getActionName()).dispatchMode(handler.Mode).onCancel(this::onTerminalTaskCancel)
						.executeOneByOne(context.channel().id(), server.task11Executor);
			}
		} else if (handler.Mode == DispatchMode.Direct) {
			TaskSpec.ofAction(() -> handle.onAbortStream(this)).name("fireAbortStreamHandle").call();
		} else {
			endStreamTaskPending = true;
			TaskSpec.ofAction(() -> {
				try {
					handle.onAbortStream(this);
				} finally {
					finishTerminalTask();
				}
			}).name("fireAbortStreamHandle").dispatchMode(handler.Mode).onCancel(this::onTerminalTaskCancel)
					.executeOneByOne(context.channel().id(), server.task11Executor);
		}
	}

	private long callEndStreamProcedure(Procedure procedure) {
		long rc = procedure.call();
		if (rc != Procedure.Success && detached != 2)
			close(sendPlainText(HttpResponseStatus.INTERNAL_SERVER_ERROR, "HTTP handler transaction failed"));
		return rc;
	}

	protected void fireWebSocket(WebSocketFrame frame) {
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
				// shutdown 丢弃未运行任务时由 onCancel 补偿释放 retain 的池化 frame。
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

	// WebSocket 的 onOpen/onClose 按 handler.Mode 与 onContent 同队列（channel.id）串行派发。
	@SuppressWarnings("ConstantConditions")
	protected void fireWebSocketNotify(String name, Action0 notify) {
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

	// 检查 WebSocket 分片消息累计大小是否超过 MaxContentLength；超限回 1009 并关连接，返回 false。
	protected boolean checkWebSocketContentSize(WebSocketFrame frame) {
		var t = Transaction.getCurrent();
		if (t != null && t.isRunning())
			return Objects.requireNonNull(t.resolveOnce(frame, 0, __ -> checkWebSocketContentSizeOnce(frame)));
		return checkWebSocketContentSizeOnce(frame);
	}

	private boolean checkWebSocketContentSizeOnce(WebSocketFrame frame) {
		if (!(frame instanceof ContinuationWebSocketFrame))
			webSocketContentTotal = 0;
		webSocketContentTotal += frame.content().readableBytes();
		//noinspection DataFlowIssue
		if (webSocketContentTotal > handler.MaxContentLength) {
			Netty.logger.error("websocket message size = {} > {} from {}",
					webSocketContentTotal, handler.MaxContentLength, context.channel().remoteAddress());
			webSocketContentTotal = 0;
			closeConnectionOnFlush(context.writeAndFlush(
					new CloseWebSocketFrame(WebSocketCloseStatus.MESSAGE_TOO_BIG, "message too big")));
			return false;
		}
		if (frame.isFinalFragment())
			webSocketContentTotal = 0;
		return true;
	}

	@SuppressWarnings("ConstantConditions")
	protected void fireWebSocket0(WebSocketFrame frame) throws Exception {
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
					//noinspection ConstantConditions
					fireWebSocketNotify("fireWebSocketClose", () -> handler.WebSocketHandle.onClose(
							this, WebSocketCloseStatus.ABNORMAL_CLOSURE.code(), ""));
				} else {
					streamAborted = true;
					fireAbortStreamHandle();
				}
			} catch (Exception e) {
				throw Task.forceThrow(e);
			}
		}
		// end-stream 任务在途时由任务 finally/onCancel 释放，避免排队中的回调读到已释放的 request/content。
		if (!endStreamTaskPending)
			releaseTerminal();
		if (willCloseConnection)
			context.close();
	}

	// 先在锁内原子摘取 request/content，再在锁外释放，防与停机收尾或 worker finally 并发双释放。
	private void releaseTerminal() {
		HttpRequest req;
		synchronized (this) {
			if (streamTasks != 0 || endStreamTaskPending)
				return;
			req = request;
			request = null;
		}
		releaseContent();
		if (req != null) {
			ReferenceCountUtil.release(req);
		}
	}

	// worker 线程的终态释放入口：投递到 EventLoop 执行；被拒时等 EventLoop 终止后兜底清理。
	private void releaseTerminalFromAnyThread() {
		//noinspection resource
		var eventLoop = context.channel().eventLoop();
		if (eventLoop.inEventLoop()) {
			releaseTerminal();
			return;
		}
		try {
			eventLoop.execute(this::releaseTerminal);
		} catch (RejectedExecutionException e) {
			cleanupAfterEventLoopTermination();
		}
	}

	protected void close(int method, @Nullable ChannelFuture cf) {
		var t = Transaction.getCurrent();
		if (t != null && t.isRunning()) {
			synchronized (this) {
				var log = headersLog(t);
				log.closeMethod = method;
				log.closeFuture = cf;
			}
			return;
		}
		abandonDeferredResponses();
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
				// 空写同样经序化器，避免先于挂起响应触发 closeInEventLoop 提前关连接。
				cf = writeResponse(Unpooled.EMPTY_BUFFER, true, null);
			cf.addListener(__ -> closeInEventLoop());
		} else {
			var eventLoop = ch.eventLoop();
			if (eventLoop.inEventLoop()) {
				context.flush();
				closeInEventLoop();
			} else {
				try {
					eventLoop.execute(() -> {
						context.flush();
						closeInEventLoop();
					});
				} catch (RejectedExecutionException ignored) {
					cleanupAfterEventLoopTermination();
				}
			}
		}
		// 须在 cf 提交后释放序化占位：FORCE 族丢弃挂起写，FINISH 族保留按序冲刷。
		releaseResponseOrder(method >= CLOSE_FORCE);
	}

	private void abandonDeferredResponses() {
		ArrayList<ChannelPromise> abandoned;
		synchronized (this) {
			abandoned = new ArrayList<>(deferredResponsePromises);
			deferredResponsePromises.clear();
		}
		for (var promise : abandoned)
			promise.tryFailure(new IllegalStateException("HTTP transaction completed without committing response"));
	}

	/** 正常结束本 exchange（保持连接），在 future 写出完成后执行收尾；幂等。 */
	public void close(@Nullable ChannelFuture future) {
		close(CLOSE_FINISH, future);
	}

	/** 结束本 exchange，发送完成后关闭连接；幂等。 */
	public void closeConnectionOnFlush(@Nullable ChannelFuture future) {
		close(CLOSE_ON_FLUSH, future);
	}

	/** 结束本 exchange 并立即关闭连接；幂等。 */
	public void closeConnectionNow() {
		close(CLOSE_FORCE, null);
	}

	/// //////////////////////////////////////////////////////////////////////////////////////////////////////////
	// send response
	public ChannelFuture send(HttpResponseStatus status, @Nullable String contentType,
									   @Nullable ByteBuf content) { // content所有权会被转移
		if (content == null)
			content = Unpooled.EMPTY_BUFFER;
		var res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, content, headersFactory, trailersFactory);
		var headers = HttpServer.setDate(res.headers())
				.set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
		if (!isH2()) // h2 禁止 Connection 头
			headers.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
		if (contentType != null)
			headers.set(HttpHeaderNames.CONTENT_TYPE, contentType);
		appendResponseHeaders(headers);
		return writeResponse(res, true, null);
	}

	public ChannelFuture send(HttpResponseStatus status, @Nullable String contentType,
									   @Nullable String content) {
		return send(status, contentType, content == null || content.isEmpty() ? Unpooled.EMPTY_BUFFER
				: Unpooled.wrappedBuffer(content.getBytes(HttpServer.defaultCharset)));
	}

	public ChannelFuture sendPlainText(HttpResponseStatus status, @Nullable String text) {
		return send(status, "text/plain; charset=utf-8", text);
	}

	public ChannelFuture sendPlainText(HttpResponseStatus status, byte @Nullable [] text) {
		return send(status, "text/plain; charset=utf-8", text == null || text.length == 0 ? Unpooled.EMPTY_BUFFER
				: Unpooled.wrappedBuffer(text));
	}

	public ChannelFuture sendHtml(HttpResponseStatus status, @Nullable String html) {
		return send(status, "text/html; charset=utf-8", html);
	}

	public ChannelFuture sendJson(HttpResponseStatus status, @Nullable String json) {
		return send(status, "application/json; charset=utf-8", json);
	}

	public ChannelFuture sendJson(HttpResponseStatus status, byte @Nullable [] json) {
		return send(status, "application/json; charset=utf-8", json == null || json.length == 0 ? Unpooled.EMPTY_BUFFER
				: Unpooled.wrappedBuffer(json));
	}

	public ChannelFuture sendXml(HttpResponseStatus status, @Nullable String xml) {
		return send(status, "text/xml; charset=utf-8", xml);
	}

	public void sendFreeMarker(@Nullable Object model) throws Exception {
		var freeMarker = server.getFreeMarker();
		if (freeMarker == null)
			throw new IllegalStateException("FreeMarker not available");
		var t = Transaction.getCurrent();
		if (t != null && t.isRunning()) {
			synchronized (this) {
				var log = headersLog(t);
				log.response = null;
				log.deferredAction = () -> freeMarker.sendResponse(this, model);
			}
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
			synchronized (this) {
				var log = headersLog(t);
				log.response = null;
				log.deferredAction = () -> thymeleaf.sendResponse(this, context);
			}
		} else
			thymeleaf.sendResponse(this, context);
	}

	public void sendFile(File file) throws Exception {
		requireNonTransactionalStreaming();
		HttpFileService.sendFile(this, file, 10 * 60);
	}

	/**
	 * 发送文件（支持 If-Modified-Since、单段 Range、缓存头与零拷贝）。
	 *
	 * @param fileCacheSeconds 客户端缓存秒数（Expires 与 max-age）
	 */
	public void sendFile(File file, int fileCacheSeconds) throws Exception {
		requireNonTransactionalStreaming();
		HttpFileService.sendFile(this, file, fileCacheSeconds);
		}

	public void sendPath(File file) {
		requireNonTransactionalStreaming();
		HttpFileService.sendPath(this, file);
		}

	public ChannelFuture send404() {
		return sendPlainText(HttpResponseStatus.NOT_FOUND, (byte[])null);
	}

	public ChannelFuture send500(Throwable ex) {
		return sendPlainText(HttpResponseStatus.INTERNAL_SERVER_ERROR, Str.stacktrace(ex));
	}

	public ChannelFuture send500(@Nullable String text) {
		return sendPlainText(HttpResponseStatus.INTERNAL_SERVER_ERROR, text);
	}

	public ChannelFuture sendWebSocket(WebSocketFrame frame) { // frame所有权会被转移
		var t = Transaction.getCurrent();
		if (t == null || !t.isRunning())
			return context.writeAndFlush(frame);
		// 只持有堆快照：redo不执行回调，池化frame不能留在提交闭包里等待release。
		byte[] body;
		int kind;
		boolean finalFragment = frame.isFinalFragment();
		int rsv = frame.rsv();
		try {
			kind = switch (frame) {
			case TextWebSocketFrame ignored -> 0;
			case BinaryWebSocketFrame ignored -> 1;
			case ContinuationWebSocketFrame ignored -> 2;
			case CloseWebSocketFrame ignored -> 3;
			case PingWebSocketFrame ignored -> 4;
			case PongWebSocketFrame ignored -> 5;
			default -> throw new IllegalArgumentException("unsupported transactional WebSocket frame: " + frame.getClass());
			};
			body = ByteBufUtil.getBytes(frame.content());
		} finally {
			frame.release();
		}
		var promise = context.newPromise();
		t.runWhileCommit(() -> {
			var data = Unpooled.wrappedBuffer(body);
			WebSocketFrame committed = switch (kind) {
			case 0 -> new TextWebSocketFrame(finalFragment, rsv, data);
			case 1 -> new BinaryWebSocketFrame(finalFragment, rsv, data);
			case 2 -> new ContinuationWebSocketFrame(finalFragment, rsv, data);
			case 3 -> new CloseWebSocketFrame(finalFragment, rsv, data);
			case 4 -> new PingWebSocketFrame(finalFragment, rsv, data);
			default -> new PongWebSocketFrame(finalFragment, rsv, data);
			};
			try {
				context.writeAndFlush(committed, promise);
			} catch (Throwable e) {
				committed.release();
				promise.tryFailure(e);
			}
		});
		t.runWhileRollback(() -> promise.tryFailure(new IllegalStateException("WebSocket handler transaction rolled back")));
		return promise;
	}

	public ChannelFuture sendWebSocket(String text) {
		return sendWebSocket(new TextWebSocketFrame(text));
	}

	public ChannelFuture sendWebSocket(byte [] data) {
		return sendWebSocket(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(data, 0, data.length)));
	}

	public ChannelFuture sendWebSocket(byte [] data, int offset, int count) {
		return sendWebSocket(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(data, offset, count)));
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * 流式响应开始：写出状态行与 headers（未设 Content-Length 则自动启用 chunked），不做状态校验；
	 * 后续用 {@link #sendStream} 发块、{@link #endStream} 收尾，大流量建议结合 {@link #isWritable()} 做背压。
	 */
	public ChannelFuture beginStream(HttpResponseStatus status, HttpHeaders headers) {
		requireNonTransactionalStreaming();
		if (!headers.contains(HttpHeaderNames.CONTENT_LENGTH))
			headers.set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
		return writeResponse(new DefaultHttpResponse(HttpVersion.HTTP_1_1, status, headers), true, null);
	}

	// 发送后data内容在回调前不能修改（所有权转移）
	public ChannelFuture sendStream(ByteBuf data) {
		return writeResponse(new DefaultHttpContent(data), true, null);
	}

	// 发送后data内容在回调前不能修改
	public ChannelFuture sendStream(byte [] data) {
		return writeResponse(new DefaultHttpContent(Unpooled.wrappedBuffer(data, 0, data.length)), true, null);
	}

	// 发送后data内容在回调前不能修改
	public ChannelFuture sendStream(byte [] data, int offset, int count) {
		return writeResponse(new DefaultHttpContent(Unpooled.wrappedBuffer(data, offset, count)), true, null);
	}

	// 发送后data内容在回调前不能修改
	public ChannelFuture sendStream(Binary b) {
		return writeResponse(new DefaultHttpContent(Unpooled.wrappedBuffer(b.bytesUnsafe(), b.getOffset(), b.size())),
				true, null);
	}

	// 发送后data内容在回调前不能修改
	public ChannelFuture sendStream(ByteBuffer bb) {
		return writeResponse(new DefaultHttpContent(Unpooled.wrappedBuffer(bb.Bytes, bb.ReadIndex, bb.size())),
				true, null);
	}

	// 发送后data内容在回调前不能修改
	public ChannelFuture sendStream(java.nio.ByteBuffer bb) {
		return writeResponse(new DefaultHttpContent(Unpooled.wrappedBuffer(bb)), true, null);
	}

	/** 写出流式响应终结符并结束本 exchange（幂等）。 */
	public void endStream() {
		requireNonTransactionalStreaming();
		if ((int)detachedHandle.getAndSet(this, 2) == 2)
			return;
		server.exchanges.remove(context.channel().id(), this);
		writeResponse(LastHttpContent.EMPTY_LAST_CONTENT, true, null).addListener(__ -> closeInEventLoop());
		releaseResponseOrder(false);
	}
}
