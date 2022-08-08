package Zeze.Netty;

import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.LastHttpContent;

public class HttpExchange {
	private HttpServer server;
	private ChannelHandlerContext context;

	public HttpExchange(HttpServer server, ChannelHandlerContext context) {
		this.server = server;
		this.context = context;
	}

	private HttpRequest request;
	private List<HttpContent> contents = new ArrayList<>();
	private int totelContents = 0;

	public String method() {
		return request.method();
	}

	public URI uri() {
		return request.uri();
	}

	public HttpHeaders headers() {
		return request.headers();
	}

	public ByteBuffer body() {
		return null;
	}

	void channelRead(Object msg) {
		if (msg instanceof FullHttpRequest) {
			request = (HttpRequest)msg;
			fireFullHandler();
		} else if (msg instanceof HttpRequest) {
			request = (HttpRequest)msg;
		} else if (msg instanceof HttpContent) {
			var c = (HttpContent)msg;
			totelContents += c.content().readableBytes();
			// todo if totelContents >
			contents.add(c);
			if (msg instanceof LastHttpContent)
				fireFullHandler();
		}
	}

	private void fireFullHandler() {
		var lower = server.handlers.headMap(request.uri().getPath());
		// todo ???
		var last = lower.lastKey();
		server.handlers.get(last).onFullRequest(this);
	}

	void close() {

	}

}
