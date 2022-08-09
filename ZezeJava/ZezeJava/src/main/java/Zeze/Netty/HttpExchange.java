package Zeze.Netty;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.HttpHeaders;

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
			} else {
				contents.add(c);
				if (msg instanceof LastHttpContent) {
					// 合并content在content()方法里面处理。这里直接
					handler.FullRequestHandle.onFullRequest(this);
					close();
				}
			}

			return; // done
		}

		sendFullResponse404(); // todo error
	}

	private boolean locateHandler() {
		var lower = server.handlers.headMap(request.uri()); // path?
		// todo ???
		var last = lower.lastKey();
		handler = server.handlers.get(last);
		return true;
	}

	void close() {
		context.flush();
		context.close();
	}

	public void sendFullResponse404() {
		// todo
	}

	public void sendFullResponse500(Throwable ex) {

	}
}
