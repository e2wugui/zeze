package Zeze.Netty;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import Zeze.Util.Str;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
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

	public String path() {
		var uri = uri();
		var i = uri.indexOf('?');
		if (i >= 0)
			return uri.substring(0, i);
		return uri;
	}

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
				sendFullResponse404();
			close(); // todo 生命期管理

			return; // done
		}

		if (msg instanceof HttpRequest) {
			request = (HttpRequest)msg;
			if (!locateHandler()) {
				sendFullResponse404();
				close();
			} else if (handler.isStreamMode()) {
				handler.BeginStreamHandle.onBeginStream(this, 0, 0); // todo from to
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
				sendFullResponse404(); // todo error
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

		sendFullResponse404(); // todo error
	}

	private boolean locateHandler() {
		handler = server.handlers.get(request.uri());
		handler = server.handlers.entrySet().iterator().next().getValue();
		return null != handler;
	}

	void close() {
		context.flush();
		context.close();
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// send response
	public void sendFullResponse(HttpResponseStatus status, String contentType, ByteBuf content) {
		var res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, content, false);
		res.headers().add(HttpHeaderNames.CONTENT_TYPE, contentType);
		context.write(res);
	}

	public void sendFullResponse(HttpResponseStatus status, String contentType, String content) {
		sendFullResponse(status, contentType, Unpooled.wrappedBuffer(content.getBytes(StandardCharsets.UTF_8)));
	}

	public void sendFullResponsePlainText(HttpResponseStatus status, String text) {
		sendFullResponse(status, "text/plain; charset=utf-8", text);
	}

	public void sendFullResponseHtml(HttpResponseStatus status, String html) {
		sendFullResponse(status, "text/html; charset=utf-8", html);
	}

	public void sendFullResponseXml(HttpResponseStatus status, String html) {
		sendFullResponse(status, "text/xml; charset=utf-8", html);
	}

	public void sendFullResponseGif(HttpResponseStatus status, ByteBuf gif) {
		sendFullResponse(status, "image/gif", gif);
	}

	public void sendFullResponseJpeg(HttpResponseStatus status, ByteBuf jpeg) {
		sendFullResponse(status, "image/jpeg", jpeg);
	}

	public void sendFullResponsePng(HttpResponseStatus status, ByteBuf png) {
		sendFullResponse(status, "image/png", png);
	}

	public void sendFullResponse404() {
		sendFullResponsePlainText(HttpResponseStatus.NOT_FOUND, "404");
	}

	public void sendFullResponse500(Throwable ex) {
		sendFullResponsePlainText(HttpResponseStatus.OK, Str.stacktrace(ex));
	}
}
