package Zeze.Netty;

import io.netty.handler.codec.http.HttpContent;

@FunctionalInterface
public interface HttpStreamContentHandle {
	// 注意参数c需要遵循netty的引用管理(ReferenceCounted),带出方法外需要retain
	void onStreamContent(HttpExchange x, HttpContent c) throws Exception;
}
