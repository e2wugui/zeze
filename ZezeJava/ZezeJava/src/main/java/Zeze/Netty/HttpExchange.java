package Zeze.Netty;

import java.io.Closeable;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.BiConsumer;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.OutLong;
import Zeze.Util.Str;
import Zeze.Util.Task;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.FullHttpRequest;
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
import io.netty.util.ReferenceCounted;

public class HttpExchange implements Closeable {
	static final int CHECK_IDLE_INTERVAL = 5; // 检查连接间隔(秒)
	static final int IDLE_TIMEOUT = 120; // 超时 检查连接间隔(秒)

	private final HttpServer server;
	private final ChannelHandlerContext context;
	private HttpRequest request;
	private HttpHandler handler;
	private final List<HttpContent> contents = new ArrayList<>(); // 只用于非流模式
	private long totalContentSize;
	private ByteBuf contentFull;
	private String path; // 保存path，优化！
	private int idleTime; // 当前统计的idle时间(秒)
	private int outBufHash;
	private boolean sending = false;

	public HttpExchange(HttpServer server, ChannelHandlerContext context) {
		this.server = server;
		this.context = context;
	}

	public HttpRequest request() {
		return request;
	}

	public Channel channel() {
		return context.channel();
	}

	public AttributeMap attributes() {
		return context.channel();
	}

	public HttpMethod method() {
		return request.method();
	}

	public String uri() {
		return request.uri();
	}

	public String path() {
		if (path == null) {
			var uri = uri();
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
		var uri = uri();
		var i = uri.indexOf('?');
		if (i >= 0)
			return uri.substring(i);
		return null;
	}

	public HttpHeaders headers() {
		return request.headers();
	}

	public ByteBuf content() {
		return contentFull;
	}

	boolean addReadIdleTimeAndCheckTimeout() {
		if ((idleTime += CHECK_IDLE_INTERVAL) < IDLE_TIMEOUT / 2) // 前半段IDLE_TIMEOUT期间只判断读超时
			return false;

		var outBuf = context.channel().unsafe().outboundBuffer();
		if (outBuf != null) {
			int hash = System.identityHashCode(outBuf.current()) ^ Long.hashCode(outBuf.currentProgress());
			if (hash != outBufHash) { // 后半段IDLE_TIMEOUT期间每次都判断写buffer的状态是否有变化,有变化则重新idle计时
				outBufHash = hash;
				idleTime = 0;
				return false;
			}
		}
		return idleTime >= IDLE_TIMEOUT; // 至少有IDLE_TIMEOUT的时间没有任何读写了,就返回true
	}

	private void fireFullRequestHandle() throws Exception {
		switch (contents.size()) {
		case 0:
			contentFull = Unpooled.EMPTY_BUFFER;
			break;
		case 1:
			contentFull = contents.get(0).content();
			break;
		default:
			var cbuf = context.alloc().compositeBuffer();
			for (var ci : contents)
				cbuf.addComponent(ci.content());
			contentFull = cbuf;
			break;
		}

		if (server.zeze != null && handler.Level != TransactionLevel.None) {
			Task.run(server.zeze.NewProcedure(() -> {
				//noinspection ConstantConditions
				handler.FullRequestHandle.onFullRequest(this);
				return Procedure.Success;
			}, "fireFullRequestHandle"), null, null, handler.Mode);
		} else {
			//noinspection ConstantConditions
			handler.FullRequestHandle.onFullRequest(this);
		}
	}

	void channelRead(Object msg) throws Exception {
		idleTime = 0;

		if (msg instanceof FullHttpRequest) {
			var full = ((FullHttpRequest)msg).retain();
			request = full;
			if (!locateHandler())
				trySendFile();
			else if (handler.isStreamMode()) {
				fireBeginStream();
				fireStreamContentHandle(full);
				fireEndStreamHandle();
				tryClose();
			} else {
				contents.add(full.retain());
				fireFullRequestHandle();
				tryClose();
			}
			return; // done
		}

		if (msg instanceof HttpRequest) {
			request = (HttpRequest)msg;
			if (request instanceof ReferenceCounted)
				((ReferenceCounted)request).retain();
			if (!locateHandler())
				trySendFile();
			else if (handler.isStreamMode())
				fireBeginStream();
			return; // done
		}

		if (msg instanceof HttpContent) {
			// 此时 request,handler 已经设置好。
			//noinspection PatternVariableCanBeUsed
			var c = (HttpContent)msg;
			totalContentSize += c.content().readableBytes();
			if (totalContentSize > handler.MaxContentLength) {
				send500("content-length too big! allow max=" + handler.MaxContentLength);
				tryClose();
				return; // done
			}

			if (handler.isStreamMode()) {
				fireStreamContentHandle(c);
				if (msg instanceof LastHttpContent) {
					fireEndStreamHandle();
					tryClose();
				}
				return; // done
			}

			contents.add(c.retain());
			if (msg instanceof LastHttpContent) {
				// 在content()方法里面处理合并。这里直接触发即可。
				fireFullRequestHandle();
				tryClose();
			}
			return; // done
		}

		send500("internal error. unknown message=" + (msg != null ? msg.getClass() : null));
		tryClose();
	}

	void channelReadClosed() {

	}

	private static long parse(String r) {
		if (r.isEmpty() || r.equals("*"))
			return -1;
		return Long.parseLong(r);
	}

	private void fireStreamContentHandle(HttpContent c) throws Exception {
		if (server.zeze != null && handler.Level != TransactionLevel.None) {
			Task.run(server.zeze.NewProcedure(() -> {
				//noinspection ConstantConditions
				handler.StreamContentHandle.onStreamContent(this, c);
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
				//noinspection ConstantConditions
				handler.EndStreamHandle.onEndStream(this);
				return Procedure.Success;
			}, "fireEndStreamHandle"), null, null, handler.Mode);
		} else {
			//noinspection ConstantConditions
			handler.EndStreamHandle.onEndStream(this);
		}
	}

	private void parseRange(AsciiString headerName, String firstSplit, OutLong from, OutLong to, OutLong size) {
		from.Value = -1;
		to.Value = -1;
		size.Value = -1;
		var range = headers().get(headerName);
		if (null != range) {
			var aunit = range.trim().split(firstSplit);
			if (aunit.length > 1) {
				var asize = aunit[1].split("/");
				if (asize.length > 0) {
					//如果 asize[0] == "*"；下面的代码能处理这种情况，不用特别判断。
					var arange = asize[0].split("-");
					if (arange.length > 0)
						from.Value = parse(arange[0]);
					if (arange.length > 1)
						to.Value = parse(arange[1]);
				}
				if (asize.length > 1) {
					size.Value = parse(asize[1]);
				}
			}
		}
	}

	private void fireBeginStream() throws Exception {
		var from = new OutLong();
		var to = new OutLong();
		var size = new OutLong();
		parseRange(HttpHeaderNames.CONTENT_RANGE, "=", from, to, size);
		if (server.zeze != null && handler.Level != TransactionLevel.None) {
			Task.run(server.zeze.NewProcedure(() -> {
				//noinspection ConstantConditions
				handler.BeginStreamHandle.onBeginStream(this, from.Value, to.Value, size.Value);
				return Procedure.Success;
			}, "fireBeginStream"), null, null, handler.Mode);
		} else {
			//noinspection ConstantConditions
			handler.BeginStreamHandle.onBeginStream(this, from.Value, to.Value, size.Value);
		}
	}

	private boolean locateHandler() {
		return (handler = server.handlers.get(path())) != null;
	}

	@Override
	public void close() {
		sending = false;
		tryClose();
		context.channel().eventLoop().execute(() -> {
			if (!contents.isEmpty()) {
				for (var c : contents)
					c.release();
				contents.clear();
			}
			contentFull.release();
		});
		var r = request;
		if (r != null) {
			request = null;
			if (r instanceof ReferenceCounted)
				((ReferenceCounted)r).release();
		}
	}

	private void tryClose() {
		if (sending)
			return;

		if (null != server.exchanges.remove(context))
			context.flush().close();
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// send response
	public ChannelFuture send(HttpResponseStatus status, String contentType, ByteBuf content) {
		var res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, content, false);
		res.headers()
				.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
				.set(HttpHeaderNames.CONTENT_TYPE, contentType);
		return context.writeAndFlush(res);
	}

	public ChannelFuture send(HttpResponseStatus status, String contentType, String content) {
		return send(status, contentType, Unpooled.wrappedBuffer(content.getBytes(StandardCharsets.UTF_8)));
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

	public void trySendFile() throws Exception {
		var file = new File(server.fileHome, filePath());
		if (!file.isFile() || file.isHidden()) {
			send404();
			tryClose();
			return; // done
		}

		// 检查 IF_MODIFIED_SINCE
		String ifModifiedSince = headers().get(HttpHeaderNames.IF_MODIFIED_SINCE);
		if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
			var ifModifiedSinceDate = Netty.parseDate(ifModifiedSince).getTime();
			if (file.lastModified() == ifModifiedSinceDate) {
				// 文件未改变。
				var res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_MODIFIED);
				res.headers()
						.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
						.set(HttpHeaderNames.DATE, Netty.getDate());
				context.writeAndFlush(res);
				tryClose();
				return; // done
			}
		}

		var raf = new RandomAccessFile(file, "r");
		var from = new OutLong();
		var to = new OutLong();
		var size = new OutLong(); // not used
		parseRange(HttpHeaderNames.RANGE, " ", from, to, size);
		if (from.Value == -1)
			from.Value = 0;
		if (to.Value == -1)
			to.Value = raf.length();
		var downloadLength = to.Value - from.Value;
		if (downloadLength < 0) {
			send500("error download range. length < 0.");
			tryClose();
			return; // done
		}

		var res = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
		res.headers()
				.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
				.set(HttpHeaderNames.CONTENT_TYPE, Mimes.fromFileName(file.getName()))
				.set(HttpHeaderNames.CONTENT_LENGTH, downloadLength)
				.set(HttpHeaderNames.CONTENT_RANGE, "bytes " + from.Value + "-" + to.Value + "/" + raf.length())
				.set(HttpHeaderNames.DATE, Netty.getDate()) // 设置时间头。
				.set(HttpHeaderNames.EXPIRES, Netty.getDate(new Date((Netty.getLastDateSecond() + server.fileCacheSeconds) * 1000L)))
				.set(HttpHeaderNames.CACHE_CONTROL, "private, max-age=" + server.fileCacheSeconds)
				.set(HttpHeaderNames.LAST_MODIFIED, Netty.getDate(new Date(file.lastModified())));

		// send headers
		context.writeAndFlush(res);

		if (!HttpMethod.HEAD.equals(method())) {
			// send file content
			context.writeAndFlush(new DefaultFileRegion(raf.getChannel(), from.Value, downloadLength)).addListener(ChannelFutureListener.CLOSE);
			// 发文件任务全部交给Netty，并且发送完毕时关闭。
			return; // done
		}

		tryClose(); // 没有进行中的流发送任务，直接关闭。关闭会flush然后close。
	}

	public ChannelFuture send404() {
		return sendPlainText(HttpResponseStatus.NOT_FOUND, "404");
	}

	public ChannelFuture send500(Throwable ex) {
		return sendPlainText(HttpResponseStatus.INTERNAL_SERVER_ERROR, Str.stacktrace(ex));
	}

	public ChannelFuture send500(String text) {
		return sendPlainText(HttpResponseStatus.INTERNAL_SERVER_ERROR, text);
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////
	// 流接口功能最大化，不做任何校验：状态校验，不正确的流起始Response（headers）等。
	public void beginStream(HttpResponseStatus status, HttpHeaders headers) {
		sending = true;
		var res = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status, headers);
		headers.remove(HttpHeaderNames.CONTENT_LENGTH);
		headers.set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
		context.writeAndFlush(res);
	}

	public void sendStream(byte[] data, BiConsumer<HttpExchange, ChannelFuture> callback) {
		sendStream(data, 0, data.length, callback);
	}

	public void sendStream(byte[] data, int offset, int count, BiConsumer<HttpExchange, ChannelFuture> callback) {
		var buf = ByteBufAllocator.DEFAULT.ioBuffer(data.length);
		buf.writeBytes(data, offset, count);
		var future = context.writeAndFlush(new DefaultHttpContent(buf), context.newPromise());
		future.addListener((ChannelFutureListener)future1 -> callback.accept(this, future1));
	}

	public void endStream() {
		context.writeAndFlush(new DefaultLastHttpContent());
		sending = false;
		tryClose();
	}
}
