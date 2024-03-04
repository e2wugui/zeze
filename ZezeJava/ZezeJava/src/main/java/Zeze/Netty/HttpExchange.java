package Zeze.Netty;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.net.URLDecoder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Str;
import Zeze.Util.Task;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultFileRegion;
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
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostMultipartRequestDecoder;
import io.netty.handler.codec.http.multipart.MemoryAttribute;
import io.netty.handler.codec.http.multipart.MemoryFileUpload;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketCloseStatus;
import io.netty.handler.codec.http.websocketx.WebSocketDecoderConfig;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolConfig;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.util.AsciiString;
import io.netty.util.AttributeMap;
import io.netty.util.ReferenceCountUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("VulnerableCodeUsages")
public class HttpExchange {
	protected static final int CLOSE_FINISH = 0; // 正常结束HttpExchange,不关闭连接
	protected static final int CLOSE_ON_FLUSH = 1; // 结束HttpExchange,发送完时关闭连接
	protected static final int CLOSE_FORCE = 2; // 结束HttpExchange,不等发送完强制关闭连接
	protected static final int CLOSE_TIMEOUT = 3; // 同上,只是因idle超时而关闭
	protected static final int CLOSE_PASSIVE = 4; // 同上,只是因远程主动关闭而关闭

	protected static final @NotNull Pattern rangePattern = Pattern.compile("[ =\\-/]");
	protected static final OpenOption[] readOnlyOpenOptions = new OpenOption[]{StandardOpenOption.READ};
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
	protected boolean willCloseConnection; // true表示close时会关闭连接
	protected boolean inStreamMode; // 是否在流/WebSocket模式过程中
	protected @Nullable Object userState;
	protected volatile @SuppressWarnings("unused") int detached; // 0:not detached; 1:detached; 2:detached and closed

	public HttpExchange(@NotNull HttpServer server, @NotNull ChannelHandlerContext context) {
		this.server = server;
		this.context = context;
	}

	public @Nullable Object getUserState() {
		return userState;
	}

	public void setUserState(@Nullable Object userState) {
		this.userState = userState;
	}

	public boolean isActive() {
		return request != null;
	}

	public boolean isClosed() {
		return detached == 2;
	}

	// 通常不需要获取context,只给特殊需要时使用netty内部的方法
	public ChannelHandlerContext context() {
		return context;
	}

	public @NotNull Channel channel() {
		return context.channel();
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
		return content.toString(StandardCharsets.UTF_8);
	}

	public byte @NotNull [] contentBytes() {
		var c = content;
		var size = c.readableBytes();
		if (size <= 0)
			return ByteBuffer.Empty;
		int offset = c.readerIndex();
		if (offset == 0 && c.hasArray())
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

	public static @NotNull String urlDecode(@NotNull String s) {
		for (int i = 0, n = s.length(); i < n; i++) {
			var c = s.charAt(i);
			if (c == '%' || c == '+')
				return URLDecoder.decode(s, StandardCharsets.UTF_8);
		}
		return s;
	}

	public @NotNull String path() {
		var req = request;
		if (req == null)
			return "";
		var uri = req.uri();
		var i = uri.indexOf('?');
		return urlDecode(i >= 0 ? uri.substring(0, i) : uri);
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
		return parseQuery(content().toString(StandardCharsets.UTF_8));
	}

	public static @NotNull String filePath(@NotNull String path) {
		var i = path.lastIndexOf(':'); // 过滤掉盘符,避免访问非法路径
		if (i < 0)
			i = 0;
		for (int e = path.length(); i < e; i++) {
			var c = path.charAt(i);
			if (c != '.' && c != '/' && c != '\\') // 过滤掉前面的特殊符号,避免访问非法路径
				break;
		}
		return path.substring(i);
	}

	protected void channelRead(@Nullable Object msg) throws Exception {
		var channel = context.channel();
		channel.attr(HttpServer.idleTimeKey).set(null);
		if (msg instanceof HttpRequest) {
			var req = ReferenceCountUtil.retain((HttpRequest)msg);
			request = req;
			var path = path();
			handler = server.getHandler(path);
			if (handler == null) {
				sendFile(filePath(path));
				return;
			}
			if (handler.isWebSocketMode() && context.pipeline().get(WebSocketServerProtocolHandler.class) == null) {
				context.pipeline().addLast(new WebSocketServerProtocolHandler(WebSocketServerProtocolConfig.newBuilder()
						.websocketPath(path).decoderConfig(WebSocketDecoderConfig.newBuilder().withUTF8Validator(false)
								.build()).build()));
				context.pipeline().addFirst(new ChannelOutboundHandlerAdapter() {
					@Override
					public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
						HttpServer.onBeforeWrite(ctx.channel());
						super.write(ctx, msg, promise);
					}
				});
				inStreamMode = true;
				//noinspection ConstantConditions
				handler.WebSocketHandle.onOpen(this);
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
						closeConnectionOnFlush(context.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
								HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE, Unpooled.EMPTY_BUFFER,
								headersFactory, trailersFactory)));
						return;
					}
					context.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE,
							Unpooled.EMPTY_BUFFER, headersFactory, trailersFactory), context.voidPromise());
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
			if (handler.isStreamMode())
				fireStreamContentHandle(c);
			else {
				if (content.readableBytes() + n > handler.MaxContentLength) {
					Netty.logger.error("content size = {} + {} > {} from {}", content.readableBytes(), n,
							handler.MaxContentLength, channel.remoteAddress());
					closeConnectionOnFlush(context.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
							HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE, Unpooled.EMPTY_BUFFER,
							headersFactory, trailersFactory)));
					return;
				}
				b.retain();
				if (content == Unpooled.EMPTY_BUFFER)
					content = b;
				else if (content instanceof CompositeByteBuf)
					((CompositeByteBuf)content).addComponent(true, b);
				else
					content = context.alloc().compositeBuffer().addComponent(true, content).addComponent(true, b);
			}
		}
		if (c instanceof LastHttpContent) {
			inStreamMode = false;
			fireEndStreamHandle(); // 流模式和非流模式通用
		}
	}

	@SuppressWarnings("DataFlowIssue")
	protected void fireBeginStream(@NotNull HttpRequest req) {
		if (handler == null)
			return;
		var r = parseRange(req, HttpHeaderNames.CONTENT_RANGE);
		if (server.zeze != null && handler.Level != TransactionLevel.None) {
			var p = server.zeze.newProcedure(() -> {
				handler.BeginStreamHandle.onBeginStream(this, r[0], r[1], r[2]);
				return Procedure.Success;
			}, "fireBeginStream");
			if (handler.Mode == DispatchMode.Direct)
				Task.call(p);
			else
				server.task11Executor.Execute(context.channel().id(), p, null, handler.Mode);
		} else if (handler.Mode == DispatchMode.Direct) {
			Task.call(() -> handler.BeginStreamHandle.onBeginStream(this, r[0], r[1], r[2]), "fireBeginStream");
		} else {
			server.task11Executor.Execute(context.channel().id(),
					() -> handler.BeginStreamHandle.onBeginStream(this, r[0], r[1], r[2]),
					"fireBeginStream", handler.Mode);
		}
	}

	protected void fireStreamContentHandle(@NotNull HttpContent c) {
		var handle = handler != null ? handler.StreamContentHandle : null;
		if (handle == null)
			return;
		if (server.zeze != null && handler.Level != TransactionLevel.None) {
			var p = server.zeze.newProcedure(() -> {
				handle.onStreamContent(this, c);
				return Procedure.Success;
			}, "fireStreamContentHandle");
			if (handler.Mode == DispatchMode.Direct)
				Task.call(p);
			else {
				c.retain();
				server.task11Executor.Execute(context.channel().id(), () -> {
					try {
						return p.call();
					} finally {
						c.release();
					}
				}, p.getActionName(), handler.Mode);
			}
		} else if (handler.Mode == DispatchMode.Direct) {
			Task.call(() -> handle.onStreamContent(this, c), "fireStreamContentHandle");
		} else {
			c.retain();
			server.task11Executor.Execute(context.channel().id(), () -> {
				try {
					handle.onStreamContent(this, c);
				} finally {
					c.release();
				}
			}, "fireStreamContentHandle", handler.Mode);
		}
	}

	public @NotNull HttpExchange detach() {
		detachedHandle.compareAndSet(this, 0, 1);
		return this;
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
		if (server.zeze != null && handler.Level != TransactionLevel.None) {
			var p = server.zeze.newProcedure(() -> {
				var handle = handler.EndStreamHandle;
				if (handle != null)
					handle.onEndStream(this);
				return Procedure.Success;
			}, "fireEndStreamHandle");
			if (handler.Mode == DispatchMode.Direct) {
				try {
					Task.call(p);
				} finally {
					if (detached == 0)
						close(null);
				}
			} else {
				server.task11Executor.Execute(context.channel().id(), () -> {
					try {
						return p.call();
					} finally {
						if (detached == 0)
							close(null);
					}
				}, p.getActionName(), handler.Mode);
			}
		} else if (handler.Mode == DispatchMode.Direct) {
			Task.call(this::invokeEndStream, "fireEndStreamHandle");
		} else {
			server.task11Executor.Execute(context.channel().id(), this::invokeEndStream,
					"fireEndStreamHandle", handler.Mode);
		}
	}

	protected void fireWebSocket(@NotNull WebSocketFrame frame) {
		if (handler == null)
			return;
		if (server.zeze != null && handler.Level != TransactionLevel.None) {
			var p = server.zeze.newProcedure(() -> {
				fireWebSocket0(frame);
				return Procedure.Success;
			}, "fireWebSocket");
			if (handler.Mode == DispatchMode.Direct)
				Task.call(p);
			else {
				frame.retain();
				server.task11Executor.Execute(context.channel().id(), () -> {
					try {
						return p.call();
					} finally {
						frame.release();
					}
				}, p.getActionName(), handler.Mode);
			}
		} else if (handler.Mode == DispatchMode.Direct) {
			Task.call(() -> fireWebSocket0(frame), "fireWebSocket");
		} else {
			frame.retain();
			server.task11Executor.Execute(context.channel().id(), () -> {
				try {
					fireWebSocket0(frame);
				} finally {
					frame.release();
				}
			}, "fireWebSocket", handler.Mode);
		}
	}

	@SuppressWarnings("ConstantConditions")
	protected void fireWebSocket0(@NotNull WebSocketFrame frame) throws Exception {
		if (frame instanceof BinaryWebSocketFrame)
			handler.WebSocketHandle.onBinary(this, frame.content());
		else if (frame instanceof TextWebSocketFrame)
			handler.WebSocketHandle.onText(this, ((TextWebSocketFrame)frame).text());
		else if (frame instanceof CloseWebSocketFrame) {
			inStreamMode = false;
			//noinspection PatternVariableCanBeUsed
			var closeFrame = (CloseWebSocketFrame)frame;
			handler.WebSocketHandle.onClose(this, closeFrame.statusCode(), closeFrame.reasonText());
			closeConnectionOnFlush(null);
		} else if (frame instanceof PingWebSocketFrame)
			handler.WebSocketHandle.onPing(this, frame.content());
		else if (frame instanceof PongWebSocketFrame)
			handler.WebSocketHandle.onPong(this, frame.content());
		else {
			Netty.logger.error("unknown websocket message type = {} from {}",
					frame.getClass().getName(), context.channel().remoteAddress());
			closeConnectionNow();
		}
	}

	// 下载请求/上传回复: range: bytes=[from]-[to] 范围是[from,to)
	// 上传请求/下载回复: content-range: bytes from-to/size 范围是[from,to]
	// 参考: https://www.jianshu.com/p/acca9656e250
	// 返回: [from, to, size]
	protected static long @NotNull [] parseRange(@NotNull HttpRequest req, @NotNull AsciiString headerName) {
		var r = new long[]{-1, -1, -1};
		var headers = req.headers();
		var range = headers.get(headerName);
		if (range != null) {
			var p = range.indexOf(',');
			if (p >= 0)
				range = range.substring(0, p); // 暂不支持",",只下载第1段
			var ss = rangePattern.split(range);
			var sn = ss.length;
			if (sn > 1) {
				r[0] = parse(ss[1]);
				if (sn > 2) {
					r[1] = parse(ss[2]);
					if (sn > 3)
						r[2] = parse(ss[3]);
				}
			}
		} else if (headerName == HttpHeaderNames.CONTENT_RANGE) { // 如果没找到content-range可能只用content-length大小上传
			var len = headers.get(HttpHeaderNames.CONTENT_LENGTH);
			if (len != null) {
				var n = Integer.parseInt(len);
				r[0] = 0;
				r[1] = n - 1;
				r[2] = n;
			}
		}
		return r;
	}

	protected static long parse(@NotNull String s) {
		if (s.isEmpty())
			return -1;
		try {
			return Long.parseLong(s);
		} catch (Exception ignored) {
			return -1;
		}
	}

	protected void closeInEventLoop() {
		if (inStreamMode) {
			inStreamMode = false;
			try {
				if (handler != null && handler.isWebSocketMode()) {
					//noinspection ConstantConditions
					handler.WebSocketHandle.onClose(this, WebSocketCloseStatus.ABNORMAL_CLOSURE.code(), "");
				} else
					fireEndStreamHandle();
			} catch (Exception e) {
				Task.forceThrow(e);
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
				cf = context.writeAndFlush(Unpooled.EMPTY_BUFFER);
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
	}

	// 正常结束HttpExchange,不关闭连接
	public void close(@Nullable ChannelFuture future) {
		close(CLOSE_FINISH, future);
	}

	// 结束HttpExchange,发送完时关闭连接
	public void closeConnectionOnFlush(@Nullable ChannelFuture future) {
		close(CLOSE_ON_FLUSH, future);
	}

	// 结束HttpExchange,不等发送完强制关闭连接
	public void closeConnectionNow() {
		close(CLOSE_FORCE, null);
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////
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
		return context.writeAndFlush(res);
	}

	public @NotNull ChannelFuture send(@NotNull HttpResponseStatus status, @Nullable String contentType,
									   @Nullable String content) {
		return send(status, contentType, content == null || content.isEmpty() ? Unpooled.EMPTY_BUFFER
				: Unpooled.wrappedBuffer(content.getBytes(StandardCharsets.UTF_8)));
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

	public void sendFile(@NotNull String filePath) throws Exception {
		var fileHome = server.fileHome;
		if (fileHome == null)
			close(send404());
		else
			sendFile(new File(fileHome.isEmpty() ? "." : fileHome, filePath));
	}

	public void sendFile(@NotNull File file) throws Exception {
		if (!file.isFile() || file.isHidden()) {
			close(send404());
			return;
		}
		var req = request;
		if (req == null) {
			close(send500(""));
			return;
		}

		// 检查 if-modified-since
		var lastModified = file.lastModified() / 1000;
		var ifModifiedSince = req.headers().get(HttpHeaderNames.IF_MODIFIED_SINCE);
		if (ifModifiedSince != null && !ifModifiedSince.isEmpty() && lastModified == HttpServer.parseDate(ifModifiedSince)) {
			var res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_MODIFIED, // 文件未改变
					Unpooled.EMPTY_BUFFER, headersFactory, trailersFactory);
			HttpServer.setDate(res.headers())
					.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
					.set(HttpHeaderNames.CONTENT_LENGTH, 0);
			close(context.writeAndFlush(res));
			return;
		}

		var fn = file.getName();
		var fc = FileChannel.open(file.toPath(), readOnlyOpenOptions);
		var fsize = fc.size();
		var r = parseRange(req, HttpHeaderNames.RANGE);
		var from = Math.max(r[0], 0);
		var to = r[1];
		var contentLen = Math.max((to >= 0 ? to : fsize) - from, 0L);

		var res = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, headersFactory);
		HttpServer.setDate(res.headers())
				.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
				.set(HttpHeaderNames.CONTENT_DISPOSITION, "inline; filename=\"" + fn + '"')
				.set(HttpHeaderNames.CONTENT_TYPE, Mimes.fromFileName(fn))
				.set(HttpHeaderNames.CONTENT_LENGTH, contentLen)
				.set(HttpHeaderNames.CONTENT_RANGE, "bytes " + from + '-' + (from + contentLen - 1) + '/' + fsize)
				.set(HttpHeaderNames.EXPIRES, HttpServer.getDate(HttpServer.getLastDateSecond() + server.fileCacheSeconds))
				.set(HttpHeaderNames.CACHE_CONTROL, "private, max-age=" + server.fileCacheSeconds)
				.set(HttpHeaderNames.LAST_MODIFIED, HttpServer.getDate(lastModified));
		context.write(res, context.voidPromise());

		if (contentLen > 0 && !HttpMethod.HEAD.equals(req.method())) // 发文件任务全部交给Netty，并且发送完毕时关闭。
			context.write(new DefaultFileRegion(fc, from, contentLen), context.voidPromise());
		close(context.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT).addListener(__ -> fc.close()));
	}

	@SuppressWarnings("deprecation")
	public void sendPath(@NotNull File file) {
		if (!file.isDirectory() || file.isHidden()) {
			close(send404());
			return;
		}

		int fileLimit = 10000; // 限制最多列出多少目录+文件,避免开销太大
		var fn = file.getName();
		var sb = new StringBuilder("<html><head><title>Index of ").append(fn)
				.append("/</title></head><body><h1>Index of ").append(fn)
				.append("/</h1><hr><pre><a href=\"../\">../</a>\n");
		var fs = file.listFiles();
		if (fs != null) {
			for (var f : fs) {
				if (f.isDirectory() && !f.isHidden()) { // 先列目录
					if (--fileLimit < 0) {
						sb.append("......\n");
						break;
					}
					fn = f.getName();
					var date = new Date(f.lastModified());
					sb.append(String.format("%4d-%02d-%02d %02d:%02d:%02d %18s <a href=\"%s/\">%s/</a>\n",
							date.getYear() + 1900, date.getMonth() + 1, date.getDate(),
							date.getHours(), date.getMinutes(), date.getSeconds(),
							"", fn, fn));
				}
			}
			for (var f : fs) {
				if (!f.isDirectory() && !f.isHidden()) { // 再列文件
					if (--fileLimit < 0) {
						sb.append("......\n");
						break;
					}
					fn = f.getName();
					var date = new Date(f.lastModified());
					sb.append(String.format("%4d-%02d-%02d %02d:%02d:%02d %,18d <a href=\"%s\">%s</a>\n",
							date.getYear() + 1900, date.getMonth() + 1, date.getDate(),
							date.getHours(), date.getMinutes(), date.getSeconds(),
							f.length(), fn, fn));
				}
			}
		}
		close(sendHtml(HttpResponseStatus.OK, sb.append("</pre><hr></body></html>").toString()));
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

	/////////////////////////////////////////////////////////////////////////////////////////////////
	// 流接口功能最大化，不做任何校验：状态校验，不正确的流起始Response（headers）等。
	public @NotNull ChannelFuture beginStream(@NotNull HttpResponseStatus status, @NotNull HttpHeaders headers) {
		if (!headers.contains(HttpHeaderNames.CONTENT_LENGTH))
			headers.set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
		return context.writeAndFlush(new DefaultHttpResponse(HttpVersion.HTTP_1_1, status, headers));
	}

	// 发送后data内容在回调前不能修改
	public @NotNull ChannelFuture sendStream(byte @NotNull [] data) {
		return context.writeAndFlush(new DefaultHttpContent(Unpooled.wrappedBuffer(data, 0, data.length)));
	}

	// 发送后data内容在回调前不能修改
	public @NotNull ChannelFuture sendStream(byte @NotNull [] data, int offset, int count) {
		return context.writeAndFlush(new DefaultHttpContent(Unpooled.wrappedBuffer(data, offset, count)));
	}

	public void endStream() {
		close(context.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT));
	}
}
