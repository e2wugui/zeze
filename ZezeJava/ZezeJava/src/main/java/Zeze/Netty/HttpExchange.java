package Zeze.Netty;

import java.io.File;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.OpenOption;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Str;
import Zeze.Util.Task;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.AsciiString;
import io.netty.util.AttributeMap;
import io.netty.util.ReferenceCountUtil;

public class HttpExchange {
	static final int CHECK_IDLE_INTERVAL = 5; // 检查连接状态间隔(秒)
	static final int IDLE_TIMEOUT = 60; // 主动断开的超时时间(秒)

	static final int CLOSE_FINISH = 0; // 正常结束HttpExchange,不关闭连接
	static final int CLOSE_ON_FLUSH = 1; // 结束HttpExchange,发送完时关闭连接
	static final int CLOSE_FORCE = 2; // 结束HttpExchange,不等发送完强制关闭连接
	static final int CLOSE_TIMEOUT = 3; // 同上,只是因idle超时而关闭
	static final int CLOSE_PASSIVE = 4; // 同上,只是因远程主动关闭而关闭

	private static final Pattern rangePattern = Pattern.compile("[ =\\-/]");
	private static final OpenOption[] emptyOpenOptions = new OpenOption[0];

	private final HttpServer server;
	private final ChannelHandlerContext context;
	private final List<ByteBuf> contents = new LinkedList<>(); // 只用于非流模式
	private HttpRequest request;
	private HttpHandler handler;
	private long totalContentSize;
	private ByteBuf contentFull;
	private String path; // 保存path，优化！
	private int idleTime; // 当前统计的idle时间(秒)
	private int outBufHash;
	private boolean willCloseConnection;

	public HttpExchange(HttpServer server, ChannelHandlerContext context) {
		this.server = server;
		this.context = context;
	}

	// 通常不需要获取context,只给特殊需要时使用netty内部的方法
	public ChannelHandlerContext getContext() {
		return context;
	}

	public Channel channel() {
		return context.channel();
	}

	public AttributeMap attributes() {
		return context.channel();
	}

	public HttpRequest request() {
		return request;
	}

	public String path() {
		if (path == null) {
			var uri = request.uri();
			var i = uri.indexOf('?');
			path = i >= 0 ? uri.substring(0, i) : uri;
		}
		return path;
	}

	public String filePath() {
		var path = path();
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

	// 一般是会使用一次，不保存中间值
	public String query() {
		var uri = request.uri();
		var i = uri.indexOf('?');
		return i >= 0 ? uri.substring(i) : null;
	}

	public ByteBuf content() {
		return contentFull;
	}

	void checkTimeout() {
		// 这里为了减小开销, 前半段IDLE_TIMEOUT期间只判断读超时
		if ((idleTime += CHECK_IDLE_INTERVAL) < IDLE_TIMEOUT / 2)
			return;
		// 后半段IDLE_TIMEOUT期间每次再判断写buffer的状态是否有变化,有变化则重新idle计时
		var outBuf = context.channel().unsafe().outboundBuffer();
		if (outBuf != null) {
			int hash = System.identityHashCode(outBuf.current()) ^ Long.hashCode(outBuf.currentProgress());
			if (hash != outBufHash) {
				outBufHash = hash;
				idleTime = 0;
				return;
			}
		}
		// 到这里至少有IDLE_TIMEOUT的时间没有任何读写了,那就强制关闭吧
		if (idleTime >= IDLE_TIMEOUT)
			close(CLOSE_TIMEOUT, null);
	}

	private void fireFullRequestHandle() throws Exception {
		switch (contents.size()) {
		case 0:
			contentFull = Unpooled.EMPTY_BUFFER;
			break;
		case 1:
			contentFull = contents.get(0);
			contents.clear();
			break;
		default:
			contentFull = context.alloc().compositeBuffer().addComponents(contents);
			contents.clear();
			break;
		}

		if (server.zeze != null && handler.Level != TransactionLevel.None) {
			Task.run(server.zeze.NewProcedure(() -> {
				handler.EndStreamHandle.onEndStream(this);
				return Procedure.Success;
			}, "fireFullRequestHandle"), null, null, handler.Mode);
		} else
			handler.EndStreamHandle.onEndStream(this);
	}

	void channelRead(Object msg) throws Exception {
		idleTime = 0;
		boolean handled = false;

		if (msg instanceof HttpRequest) {
			request = ReferenceCountUtil.retain((HttpRequest)msg);
			if (!locateHandler()) {
				sendFile(filePath());
				return;
			}
			if (handler.isStreamMode())
				fireBeginStream();
			handled = true;
		}

		if (msg instanceof HttpContent) {
			if (request == null || handler == null) // 缺失上文的msg,有少数情况会出现,忽略吧
				return;
			//noinspection PatternVariableCanBeUsed
			var c = (HttpContent)msg;
			var n = c.content().readableBytes();
			totalContentSize += n;
			if (totalContentSize > handler.MaxContentLength) {
				Netty.logger.error("totalContentSize {} > {} from {}",
						totalContentSize, handler.MaxContentLength, context.channel().remoteAddress());
				closeConnectionNow();
			} else if (handler.isStreamMode()) {
				if (n > 0)
					fireStreamContentHandle(c);
				if (msg instanceof LastHttpContent)
					fireEndStreamHandle();
			} else {
				if (n > 0)
					contents.add(c.content().retain());
				if (msg instanceof LastHttpContent)
					fireFullRequestHandle();
			}
			return;
		}

		if (!handled) {
			Netty.logger.error("unknown message type={} from {}",
					(msg != null ? msg.getClass() : null), context.channel().remoteAddress());
			closeConnectionNow();
		}
	}

	void channelReadClosed() {
		willCloseConnection = true;
	}

	private void fireStreamContentHandle(HttpContent c) throws Exception {
		if (server.zeze != null && handler.Level != TransactionLevel.None) {
			c.retain();
			Task.run(server.zeze.NewProcedure(() -> {
				try {
					//noinspection ConstantConditions
					handler.StreamContentHandle.onStreamContent(this, c);
				} finally {
					c.release();
				}
				return Procedure.Success;
			}, "fireStreamContentHandle"), null, null, handler.Mode);
		} else {
			//noinspection ConstantConditions
			handler.StreamContentHandle.onStreamContent(this, c);
		}
	}

	private void fireEndStreamHandle() throws Exception {
		if (server.zeze != null && handler.Level != TransactionLevel.None) {
			Task.run(server.zeze.NewProcedure(() -> {
				handler.EndStreamHandle.onEndStream(this);
				return Procedure.Success;
			}, "fireEndStreamHandle"), null, null, handler.Mode);
		} else
			handler.EndStreamHandle.onEndStream(this);
	}

	// 下载请求/上传回复: range: bytes=[from]-[to] 范围是[from,to)
	// 上传请求/下载回复: content-range: bytes from-to/size 范围是[from,to]
	// 回复: [from, to, size]
	// 参考: https://www.jianshu.com/p/acca9656e250
	private long[] parseRange(AsciiString headerName) {
		long[] r = new long[]{-1, -1, -1};
		var range = request.headers().get(headerName);
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
		}
		return r;
	}

	private static long parse(String s) {
		if (s.isEmpty())
			return -1;
		try {
			return Long.parseLong(s);
		} catch (Exception ignored) {
			return -1;
		}
	}

	private void fireBeginStream() throws Exception {
		var r = parseRange(HttpHeaderNames.CONTENT_RANGE);
		if (server.zeze != null && handler.Level != TransactionLevel.None) {
			Task.run(server.zeze.NewProcedure(() -> {
				//noinspection ConstantConditions
				handler.BeginStreamHandle.onBeginStream(this, r[0], r[1], r[2]);
				return Procedure.Success;
			}, "fireBeginStream"), null, null, handler.Mode);
		} else {
			//noinspection ConstantConditions
			handler.BeginStreamHandle.onBeginStream(this, r[0], r[1], r[2]);
		}
	}

	private boolean locateHandler() {
		return (handler = server.handlers.get(path())) != null;
	}

	private void closeInEventLoop() {
		if (!contents.isEmpty()) {
			for (var c : contents)
				c.release();
			contents.clear();
		}
		if (contentFull != null) {
			contentFull.release();
			contentFull = null;
		}
		if (request != null) {
			ReferenceCountUtil.release(request);
			request = null;
		}
		if (willCloseConnection)
			context.close();
	}

	void close(int method, ChannelFuture cf) {
		var removed = server.exchanges.remove(context, this);
		if (method != CLOSE_FINISH) {
			willCloseConnection = true;
			Netty.logger.info("close({}): {}", method, context.channel().remoteAddress());
		}
		if (method >= CLOSE_FORCE) { // or CLOSE_TIMEOUT or CLOSE_PASSIVE
			var eventLoop = context.channel().eventLoop();
			if (eventLoop.inEventLoop()) {
				context.flush();
				closeInEventLoop();
			} else {
				eventLoop.execute(() -> {
					context.flush();
					closeInEventLoop();
				});
			}
		} else if (removed)
			(cf != null ? cf : context.writeAndFlush(Unpooled.EMPTY_BUFFER)).addListener(__ -> closeInEventLoop());
		else if (method == CLOSE_ON_FLUSH)
			(cf != null ? cf : context.writeAndFlush(Unpooled.EMPTY_BUFFER)).addListener(__ -> context.close());
	}

	// 正常结束HttpExchange,不关闭连接
	public void close(ChannelFuture future) {
		close(CLOSE_FINISH, future);
	}

	// 结束HttpExchange,发送完时关闭连接
	public void closeConnectionOnFlush(ChannelFuture future) {
		close(CLOSE_ON_FLUSH, future);
	}

	// 结束HttpExchange,不等发送完强制关闭连接
	public void closeConnectionNow() {
		close(CLOSE_FORCE, null);
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// send response
	public ChannelFuture send(HttpResponseStatus status, String contentType, ByteBuf content) {
		var res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, content, false);
		var len = content.readableBytes();
		var headers = Netty.setDate(res.headers())
				.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
				.set(HttpHeaderNames.CONTENT_LENGTH, len);
		if (len > 0)
			headers.set(HttpHeaderNames.CONTENT_TYPE, contentType);
		return context.writeAndFlush(res);
	}

	public ChannelFuture send(HttpResponseStatus status, String contentType, String content) {
		return send(status, contentType, content == null || content.isEmpty() ? Unpooled.EMPTY_BUFFER
				: Unpooled.wrappedBuffer(content.getBytes(StandardCharsets.UTF_8)));
	}

	public ChannelFuture sendPlainText(HttpResponseStatus status, String text) {
		return send(status, "text/plain; charset=utf-8", text);
	}

	public ChannelFuture sendHtml(HttpResponseStatus status, String html) {
		return send(status, "text/html; charset=utf-8", html);
	}

	public ChannelFuture sendXml(HttpResponseStatus status, String xml) {
		return send(status, "text/xml; charset=utf-8", xml);
	}

	public ChannelFuture sendGif(HttpResponseStatus status, ByteBuf gif) {
		return send(status, "image/gif", gif);
	}

	public ChannelFuture sendJpeg(HttpResponseStatus status, ByteBuf jpeg) {
		return send(status, "image/jpeg", jpeg);
	}

	public ChannelFuture sendPng(HttpResponseStatus status, ByteBuf png) {
		return send(status, "image/png", png);
	}

	public void sendFile(String filePath) throws Exception {
		var file = new File(server.fileHome, filePath);
		if (!file.isFile() || file.isHidden()) {
			close(send404());
			return;
		}

		// 检查 if-modified-since
		var lastModified = file.lastModified() / 1000;
		var ifModifiedSince = request.headers().get(HttpHeaderNames.IF_MODIFIED_SINCE);
		if (ifModifiedSince != null && !ifModifiedSince.isEmpty() && lastModified == Netty.parseDate(ifModifiedSince)) {
			var res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_MODIFIED, // 文件未改变
					Unpooled.EMPTY_BUFFER, false);
			Netty.setDate(res.headers())
					.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
					.set(HttpHeaderNames.CONTENT_LENGTH, 0);
			close(context.writeAndFlush(res));
			return;
		}

		var fc = FileChannel.open(file.toPath(), emptyOpenOptions);
		var fsize = fc.size();
		var r = parseRange(HttpHeaderNames.RANGE);
		var from = Math.max(r[0], 0);
		var to = r[1];
		var contentLen = Math.max((to >= 0 ? to : fsize) - from, 0L);

		var res = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, false);
		Netty.setDate(res.headers())
				.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
				.set(HttpHeaderNames.CONTENT_TYPE, Mimes.fromFileName(filePath))
				.set(HttpHeaderNames.CONTENT_LENGTH, contentLen)
				.set(HttpHeaderNames.CONTENT_RANGE, "bytes " + from + '-' + (from + contentLen - 1) + '/' + fsize)
				.set(HttpHeaderNames.EXPIRES, Netty.getDate(Netty.getLastDateSecond() + server.fileCacheSeconds))
				.set(HttpHeaderNames.CACHE_CONTROL, "private, max-age=" + server.fileCacheSeconds)
				.set(HttpHeaderNames.LAST_MODIFIED, Netty.getDate(lastModified));
		context.write(res, context.voidPromise());

		if (contentLen > 0 && !HttpMethod.HEAD.equals(request.method())) // 发文件任务全部交给Netty，并且发送完毕时关闭。
			context.write(new DefaultFileRegion(fc, from, contentLen), context.voidPromise());
		close(context.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT).addListener(__ -> fc.close()));
	}

	public ChannelFuture send404() {
		return sendPlainText(HttpResponseStatus.NOT_FOUND, null);
	}

	public ChannelFuture send500(Throwable ex) {
		return sendPlainText(HttpResponseStatus.INTERNAL_SERVER_ERROR, Str.stacktrace(ex));
	}

	public ChannelFuture send500(String text) {
		return sendPlainText(HttpResponseStatus.INTERNAL_SERVER_ERROR, text);
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////
	// 流接口功能最大化，不做任何校验：状态校验，不正确的流起始Response（headers）等。
	public ChannelFuture beginStream(HttpResponseStatus status, HttpHeaders headers) {
		var res = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status, headers);
		headers.remove(HttpHeaderNames.CONTENT_LENGTH);
		headers.set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
		return context.writeAndFlush(res);
	}

	// 发送后data内容在回调前不能修改
	public ChannelFuture sendStream(byte[] data) {
		return sendStream(data, 0, data.length);
	}

	// 发送后data内容在回调前不能修改
	public ChannelFuture sendStream(byte[] data, int offset, int count) {
		return context.writeAndFlush(new DefaultHttpContent(Unpooled.wrappedBuffer(data, offset, count)));
	}

	public void endStream() {
		close(context.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT));
	}
}
