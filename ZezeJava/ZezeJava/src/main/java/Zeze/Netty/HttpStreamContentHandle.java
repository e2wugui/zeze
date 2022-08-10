package Zeze.Netty;

import io.netty.handler.codec.http.HttpContent;

@FunctionalInterface
public interface HttpStreamContentHandle {
	void onStreamContent(HttpExchange x, HttpContent c) throws Exception;
}
