package Zeze.Netty;


/**
 * 请求流结束（或完整请求就绪）的处理回调。
 * 流模式下若已注册 {@link HttpAbortStreamHandle} 则仅在正常结束时触发，二者互斥；
 * 未注册时流中止也会回退触发本回调，此时 {@link HttpExchange#isStreamAborted()} 为 {@code true}。
 */
@FunctionalInterface
public interface HttpEndStreamHandle {
	void onEndStream(HttpExchange x) throws Exception;
}
