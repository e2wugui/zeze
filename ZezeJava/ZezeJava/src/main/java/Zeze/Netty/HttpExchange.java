package Zeze.Netty;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.function.BiConsumer;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.OutInt;
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
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.util.AsciiString;
import io.netty.util.AttributeMap;

public class HttpExchange {
	private boolean sending = false;

	private final HttpServer server;
	private final ChannelHandlerContext context;

	public HttpExchange(HttpServer server, ChannelHandlerContext context) {
		this.server = server;
		this.context = context;
	}

	private HttpRequest request;
	private HttpHandler handler;
	private final List<HttpContent> contents = new ArrayList<>();
	private int totalContentSize;
	private ByteBuffer contentFull;

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

	// 保存path，优化！
	private String path;
	public String path() {
		if (path == null) {
			var uri = uri();
			var i = uri.indexOf('?');
			path = i >= 0 ? uri.substring(0, i) : uri;
		}
		return path;
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

	// 对称的话，这里应该返回Netty.ByteBuf。不熟悉，先用这个。
	public ByteBuffer content() {
		if (null == contentFull) {
			switch (contents.size()) {
			case 0:
				contentFull = ByteBuffer.allocate(0);
				break;
			case 1:
				var c0 = contents.get(0).content();
				contentFull = ByteBuffer.wrap(c0.array(), c0.arrayOffset(), c0.readableBytes());
				break;
			default:
				contentFull = ByteBuffer.allocate(totalContentSize);
				for (var ci : contents) {
					var cc = ci.content();
					contentFull.put(cc.array(), cc.arrayOffset(), cc.readableBytes());
				}
				break;
			}
		}
		return contentFull;
	}

	private void fireFullRequestHandle() throws Exception {
		if (server.Zeze != null && handler.Level != TransactionLevel.None) {
			Task.run(server.Zeze.NewProcedure(
					() -> {
						handler.FullRequestHandle.onFullRequest(this);
						return Procedure.Success;
					}, "fireFullRequestHandle"), null, null, handler.Mode);
		} else {
			handler.FullRequestHandle.onFullRequest(this);
		}
	}

	void channelRead(Object msg) throws Exception {
		if (msg instanceof FullHttpRequest full) {
			request = full;
			contents.add(full);
			if (locateHandler()) {
				fireFullRequestHandle();
				tryClose();
			} else {
				trySendFile();
			}
			return; // done
		}

		if (msg instanceof HttpRequest) {
			request = (HttpRequest)msg;
			if (!locateHandler()) {
				trySendFile();
			} else if (handler.isStreamMode()) {
				fireBeginStream();
			}

			return; // done
		}

		if (msg instanceof HttpContent c) {
			// 此时 request,handler 已经设置好。
			if (handler.isStreamMode()) {
				fireStreamContentHandle(c);
				if (msg instanceof LastHttpContent) {
					fireEndStreamHandle();
					tryClose();
				}

				return; // done
			}

			totalContentSize += c.content().readableBytes();
			if (totalContentSize > handler.MaxContentLength) {
				send500("content-length too big! allow max=" + handler.MaxContentLength);
				tryClose();

				return; // done
			}

			contents.add(c);
			if (msg instanceof LastHttpContent) {
				// 在content()方法里面处理合并。这里直接触发即可。
				fireFullRequestHandle();
				tryClose();
			}

			return; // done
		}

		send500("internal error. unknown message!");
	}

	private static long parse(String r) {
		if (r.isEmpty() || r.equals("*"))
			return -1;
		return Long.parseLong(r);
	}

	private void fireStreamContentHandle(HttpContent c) throws Exception {
		if (server.Zeze != null && handler.Level != TransactionLevel.None) {
			Task.run(server.Zeze.NewProcedure(
					() -> {
						handler.StreamContentHandle.onStreamContent(this, c);
						return Procedure.Success;
					}, "fireFullRequestHandle"), null, null, handler.Mode);
		} else {
			handler.StreamContentHandle.onStreamContent(this, c);
		}
	}

	private void fireEndStreamHandle() throws Exception {
		if (server.Zeze != null && handler.Level != TransactionLevel.None) {
			Task.run(server.Zeze.NewProcedure(
					() -> {
						handler.EndStreamHandle.onEndStream(this);
						return Procedure.Success;
					}, "fireFullRequestHandle"), null, null, handler.Mode);
		} else {
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
					//如果 asize[0] == ”*“；下面的代码能处理这种情况，不用特别判断。
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
		if (server.Zeze != null && handler.Level != TransactionLevel.None) {
			Task.run(server.Zeze.NewProcedure(
					() -> {
						handler.BeginStreamHandle.onBeginStream(this, from.Value, to.Value, size.Value);
						return Procedure.Success;
					}, "fireFullRequestHandle"), null, null, handler.Mode);
		} else {
			handler.BeginStreamHandle.onBeginStream(this, from.Value, to.Value, size.Value);
		}
	}

	private boolean locateHandler() {
		handler = server.handlers.get(path());
		return null != handler;
	}

	public void close() {
		sending = false;
		tryClose();
	}

	private void tryClose() {
		if (sending)
			return;

		if (null != server.exchanges.remove(context)) {
			context.flush();
			context.close();
		}
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// send response
	public void send(HttpResponseStatus status, String contentType, ByteBuf content) {
		var res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, content, false);
		res.headers().add(HttpHeaderNames.CONTENT_TYPE, contentType);
		context.write(res);
	}

	public void send(HttpResponseStatus status, String contentType, String content) {
		send(status, contentType, Unpooled.wrappedBuffer(content.getBytes(StandardCharsets.UTF_8)));
	}

	public void sendPlainText(HttpResponseStatus status, String text) {
		send(status, "text/plain; charset=utf-8", text);
	}

	public void sendHtml(HttpResponseStatus status, String html) {
		send(status, "text/html; charset=utf-8", html);
	}

	public void sendXml(HttpResponseStatus status, String html) {
		send(status, "text/xml; charset=utf-8", html);
	}

	public void sendGif(HttpResponseStatus status, ByteBuf gif) {
		send(status, "image/gif", gif);
	}

	public void sendJpeg(HttpResponseStatus status, ByteBuf jpeg) {
		send(status, "image/jpeg", jpeg);
	}

	public void sendPng(HttpResponseStatus status, ByteBuf png) {
		send(status, "image/png", png);
	}

	public static final String HTTP_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

	public void trySendFile() throws Exception {
		if (null == server.FileHome) {
			send404();
			tryClose();
			return; // done
		}

		var file = Path.of(server.FileHome, path()).toFile();
		if (!file.exists() || !file.isHidden() || file.isFile()) {
			send404();
			tryClose();
			return; // done
		}

		// 检查 IF_MODIFIED_SINCE
		String ifModifiedSince = headers().get(HttpHeaderNames.IF_MODIFIED_SINCE);
		SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT);
		if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
			var ifModifiedSinceDate = dateFormatter.parse(ifModifiedSince).getTime();
			if (file.lastModified() == ifModifiedSinceDate) {
				// 文件未改变。
				var res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_MODIFIED);
				var time = Calendar.getInstance();
				res.headers().set(HttpHeaderNames.DATE, dateFormatter.format(time.getTime()));
				context.write(res);
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

		var response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
		response.headers().set(HttpHeaderNames.CONTENT_TYPE, Mimes.fromFileName(file.getName()));
		response.headers().set(HttpHeaderNames.CONTENT_LENGTH, downloadLength);
		response.headers().set(HttpHeaderNames.CONTENT_RANGE, "bytes " + from.Value + "-" + to.Value + "/" + raf.length());
		// 设置时间头。
		Calendar time = Calendar.getInstance();
		response.headers().set(HttpHeaderNames.DATE, dateFormatter.format(time.getTime()));
		// 设置 cache 控制相关头。
		time.add(Calendar.SECOND, server.FileCacheSeconds);
		response.headers().set(HttpHeaderNames.EXPIRES, dateFormatter.format(time.getTime()));
		response.headers().set(HttpHeaderNames.CACHE_CONTROL, "private, max-age=" + server.FileCacheSeconds);
		response.headers().set(HttpHeaderNames.LAST_MODIFIED, dateFormatter.format(file.lastModified()));

		// send headers
		context.write(response);

		if (!HttpMethod.HEAD.equals(method())) {
			// send file content
			context.write(new DefaultFileRegion(raf.getChannel(), from.Value, downloadLength)).addListener(ChannelFutureListener.CLOSE);
			// 发文件任务全部交给Netty，并且发送完毕时关闭。
			return; // done
		}

		tryClose(); // 没有进行中的流发送任务，直接关闭。关闭会flush然后close。
	}

	public void send404() {
		sendPlainText(HttpResponseStatus.NOT_FOUND, "404");
	}

	public void send500(Throwable ex) {
		sendPlainText(HttpResponseStatus.INTERNAL_SERVER_ERROR, Str.stacktrace(ex));
	}

	public void send500(String text) {
		sendPlainText(HttpResponseStatus.INTERNAL_SERVER_ERROR, text);
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////
	// 流接口功能最大化，不做任何校验：状态校验，不正确的流起始Response（headers）等。
	public void beginStream(HttpResponseStatus status, HttpHeaders headers) {
		sending = true;
		var res = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status, headers);
		headers.set(HttpHeaderNames.TRANSFER_ENCODING, "chunked");
		headers.remove(HttpHeaderNames.CONTENT_LENGTH);
		context.write(res);
	}

	public void sendSteam(byte[] data, BiConsumer<HttpExchange, ChannelFuture> callback) {
		var buf = ByteBufAllocator.DEFAULT.ioBuffer(data.length);
		buf.writeBytes(data);
		var future = context.write(new DefaultHttpContent(buf), context.newPromise());
		future.addListener((ChannelFutureListener)future1 -> callback.accept(this, future1));
	}

	public void endStream() {
		context.write(new DefaultHttpContent(ByteBufAllocator.DEFAULT.ioBuffer(0)));
		sending = false;
		tryClose();
	}
}
