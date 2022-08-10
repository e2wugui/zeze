package Zeze.Netty;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import Zeze.Util.Str;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
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

public class HttpExchange {
	private boolean sending = false;

	private HttpServer server;
	private ChannelHandlerContext context;

	public HttpExchange(HttpServer server, ChannelHandlerContext context) {
		this.server = server;
		this.context = context;
	}

	private HttpRequest request;
	private HttpHandler handler;
	private List<HttpContent> contents = new ArrayList<>();
	private int totalContentSize;
	private ByteBuffer contentFull;

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

	void channelRead(Object msg) {
		if (msg instanceof FullHttpRequest) {
			var full = (FullHttpRequest)msg;
			request = full;
			contents.add(full);
			if (locateHandler())
				handler.FullRequestHandle.onFullRequest(this);
			else
				send404();
			close();

			return; // done
		}

		if (msg instanceof HttpRequest) {
			request = (HttpRequest)msg;
			if (!locateHandler()) {
				send404();
				close();
			} else if (handler.isStreamMode()) {
				fireBeginStream();
			}

			return; // done
		}

		if (msg instanceof HttpContent) {
			// 此时 request,handler 已经设置好。
			var c = (HttpContent)msg;
			if (handler.isStreamMode()) {
				handler.StreamContentHandle.onStreamContent(this, c);
				if (msg instanceof LastHttpContent) {
					handler.EndStreamHandle.onEndStream(this);
					close();
				}

				return; // done
			}

			totalContentSize += c.content().readableBytes();
			if (totalContentSize > handler.MaxContentLength) {
				send500("content-length too big! allow max=" + handler.MaxContentLength);
				close();

				return; // done
			}

			contents.add(c);
			if (msg instanceof LastHttpContent) {
				// 在content()方法里面处理合并。这里直接触发即可。
				handler.FullRequestHandle.onFullRequest(this);
				close();
			}

			return; // done
		}

		send500("internal error. unknown message!");
	}

	private final int parse(String r) {
		if (r.isEmpty() || r.equals("*"))
			return -1;
		return Integer.parseInt(r);
	}

	private void fireBeginStream() {
		var from = -1;
		var to = -1;
		var size = -1;
		var range = request.headers().get(HttpHeaderNames.CONTENT_RANGE);
		if (null != range) {
			var aunit = range.trim().split(" ");
			if (aunit.length > 1) {
				var asize = aunit[1].split("/");
				if (asize.length > 0) {
					var arange = asize[0].split("-");
					if (arange.length > 0)
						from = parse(arange[0]);
					if (arange.length > 1)
						to = parse(arange[1]);
				}
				if (asize.length > 1) {
					size = parse(asize[1]);
				}
			}
		}
		handler.BeginStreamHandle.onBeginStream(this, from, to, size);
	}

	private boolean locateHandler() {
		handler = server.handlers.get(path());
		return null != handler;
	}

	void close() {
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
	public void beginTrunk(HttpResponseStatus status, HttpHeaders headers, Consumer<HttpExchange> callback) {
		sending = true;
		var res = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status, headers);
		headers.set(HttpHeaderNames.TRANSFER_ENCODING, "chunked");
		var future = context.write(res, context.newPromise());
		future.addListener((ChannelFutureListener)future1 -> callback.accept(this)); // todo remove need?
	}

	public void sendTrunk(byte[] data, Consumer<HttpExchange> callback) {
		var nsize = (data.length + "\r\n").getBytes(StandardCharsets.UTF_8);
		var nbuf = ByteBufAllocator.DEFAULT.buffer(data.length + nsize.length);
		nbuf.writeBytes(nsize);
		nbuf.writeBytes(data);
		var future = context.write(new DefaultHttpContent(nbuf), context.newPromise());
		future.addListener((ChannelFutureListener)future1 -> callback.accept(this)); // todo remove need?
	}

	public void endTrunk() {
		var trunk = ("0\r\n\r\n").getBytes(StandardCharsets.UTF_8);
		var buf = ByteBufAllocator.DEFAULT.buffer(trunk.length);
		buf.writeBytes(trunk);
		context.write(new DefaultHttpContent(buf));
		sending = false;
		close();
	}
}
