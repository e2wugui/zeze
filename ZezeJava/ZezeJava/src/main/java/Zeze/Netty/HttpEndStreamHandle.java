package Zeze.Netty;


/**
 * 请求流结束（或完整请求就绪）的处理回调。
 */
@FunctionalInterface
public interface HttpEndStreamHandle {
	void onEndStream(HttpExchange x) throws Exception;
}
