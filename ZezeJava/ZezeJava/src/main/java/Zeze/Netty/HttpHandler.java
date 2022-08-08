package Zeze.Netty;

import io.netty.handler.codec.http.HttpContent;

public abstract class HttpHandler {
	// 普通请求，如果需要则会自动聚合
	public abstract void onFullRequest(HttpExchange x);

	// 上行流
	public abstract void onBeginStream(HttpExchange x, int from, int to);
	public abstract void onStreamContent(HttpExchange x, HttpContent c);
	public abstract void onEndStream(HttpExchange x);
}
