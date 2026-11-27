package Zeze.Netty;

/**
 * 流式上传中止回调：请求流在收到 {@code LastHttpContent} 前中止时触发，用于释放资源；
 * 与 {@link HttpEndStreamHandle#onEndStream(HttpExchange)} 互斥，触发时连接可能已关闭。
 */
@FunctionalInterface
public interface HttpAbortStreamHandle {
	void onAbortStream(HttpExchange x) throws Exception;
}
